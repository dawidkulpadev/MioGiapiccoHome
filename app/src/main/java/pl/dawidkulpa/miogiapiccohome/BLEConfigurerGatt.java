package pl.dawidkulpa.miogiapiccohome;

import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.List;

import pl.dawidkulpa.miogiapiccohome.API.Device;

public class BLEConfigurerGatt extends BroadcastReceiver {
    private enum State {Idle, Connecting, Syncing, WaitingForUserInput, WritingConfig, DeviceConfigured, ConnectionFailed}
    private State state;

    public enum ErrorCode {DiscoverFailed, SyncFailed, ConfigWriteFailed, UnexpectedDisconnect}

    public static final String SERVICE_UUID                 = "952cb13b-57fa-4885-a445-57d1f17328fd";

    public interface ConfigurerGattListener {
        void onDeviceConnected();
        void onDeviceReady();
        void onWiFisRefresh(String wifis);
        void onConfigFinished();
        void onError(ErrorCode ec);
    }

    private Device.Type connectedDevType= Device.Type.Unknown;
    private String foundDeviceName;
    private String bleName;

    BLEGattService bluetoothService;
    ConfigurerGattListener listener;
    BLEConfigurerCharacteristics characteristicsManager;
    BLEConfigurer parent;


    public BLEConfigurerGatt(BLEConfigurer parentConfigurer, ConfigurerGattListener configurerGattListener){
        parent= parentConfigurer;
        listener= configurerGattListener;
        state= State.Idle;
    }

    void startConnectAndReceiving(BLEGattService bleService, String address){
        bluetoothService= bleService;
        bluetoothService.connect(address);
        state= State.Connecting;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if(action==null)
            return;

        if(state== State.Connecting){
            switch (action) {
                case BLEGattService.ACTION_GATT_CONNECTED:
                    if (bleName.contains("Light")) {
                        connectedDevType = Device.Type.Light;
                        foundDeviceName = context.getString(R.string.mg_device_name_light);
                    } else if (bleName.contains("Soil")) {
                        connectedDevType = Device.Type.Soil;
                        foundDeviceName = context.getString(R.string.mg_device_name_soil);
                    } else if (bleName.contains("Air")) {
                        connectedDevType = Device.Type.Air;
                        foundDeviceName = context.getString(R.string.mg_device_name_air);
                    }

                    BLEConfigurerCharacteristics.ActionsListener charListener =
                            new BLEConfigurerCharacteristics.ActionsListener() {
                                @Override
                                public void onRefresh(String wifis) {
                                    listener.onWiFisRefresh(wifis);
                                }

                                @Override
                                public void onError(BLEConfigurerCharacteristics.ErrorCode ec) {
                                    if(ec== BLEConfigurerCharacteristics.ErrorCode.SyncFailed)
                                        listener.onError(ErrorCode.SyncFailed);
                                    else if(ec== BLEConfigurerCharacteristics.ErrorCode.WriteFailed)
                                        listener.onError(ErrorCode.ConfigWriteFailed);
                                }
                            };

                    if (bleName.contains("Gen2")) {
                        characteristicsManager = new BLEConfigurerBLELNCharacteristics(bluetoothService,
                                charListener);
                    } else {
                        characteristicsManager = new BLEConfigurerRawCharacteristics(bluetoothService,
                                charListener);
                    }
                    break;
                case BLEGattService.ACTION_GATT_SERVICES_DISCOVERED:
                    if (discoverService()) {
                        listener.onDeviceConnected();
                        state = State.Syncing;
                        Log.d("BLEConfigurerGattCallbacks", "System state: Syncing");
                        characteristicsManager.startSync();
                    } else {
                        state = State.ConnectionFailed;
                        listener.onError(ErrorCode.DiscoverFailed);
                        Log.d("BLEConfigurerGattCallbacks", "System state: ConnectionFailed (Characteristics not found)");
                    }
                    break;
                case BLEGattService.ACTION_GATT_DISCONNECTED:
                    listener.onError(ErrorCode.UnexpectedDisconnect);
                    break;
            }
        } else if(state== State.Syncing){
            switch (action) {
                case BLEGattService.ACTION_DATA_AVAILABLE: {
                    String uuid = intent.getStringExtra("uuid");
                    byte[] data = intent.getByteArrayExtra("data");
                    characteristicsManager.onPreparingDataAvailable(uuid, data);
                    break;
                }
                case BLEGattService.ACTION_DESCR_WRITE_COMPLETE: {
                    String uuid = intent.getStringExtra("uuid");
                    characteristicsManager.onPreparingDescriptorUpdate(uuid);
                    break;
                }
                case BLEGattService.ACTION_CHARACTERISTIC_CHANGED: {
                    String uuid = intent.getStringExtra("uuid");
                    byte[] data = intent.getByteArrayExtra("data");
                    characteristicsManager.onPreparingNotify(uuid, data);
                    break;
                }
                case BLEGattService.ACTION_GATT_DISCONNECTED: {
                    listener.onError(ErrorCode.UnexpectedDisconnect);
                    break;
                }
            }

            if (characteristicsManager.preparedAndReady()) {
                state= State.WaitingForUserInput;
                listener.onDeviceReady();
            }
        } else if(state== State.WaitingForUserInput){
            switch (action) {
                case BLEGattService.ACTION_CHARACTERISTIC_CHANGED: {
                    String uuid = intent.getStringExtra("uuid");
                    byte[] data = intent.getByteArrayExtra("data");
                    characteristicsManager.onReadyNotify(uuid, data);
                    break;
                }
                case BLEGattService.ACTION_DATA_AVAILABLE: {
                    String uuid = intent.getStringExtra("uuid");
                    byte[] data = intent.getByteArrayExtra("data");
                    characteristicsManager.onReadyDataAvailable(uuid, data);
                    break;
                }
                case BLEGattService.ACTION_GATT_DISCONNECTED:
                    listener.onError(ErrorCode.UnexpectedDisconnect);
                    break;
            }
        } else if(state== State.WritingConfig){
            switch (action) {
                case BLEGattService.ACTION_DATA_WRITE_COMPLETE: {
                    String uuid = intent.getStringExtra("uuid");
                    characteristicsManager.onWritingWriteComplete(uuid);
                    break;
                }
                case BLEGattService.ACTION_CHARACTERISTIC_CHANGED: {
                    String uuid = intent.getStringExtra("uuid");
                    byte[] data = intent.getByteArrayExtra("data");

                    characteristicsManager.onWritingNotify(uuid, data);

                    if (characteristicsManager.writingComplete()) {
                        state = State.DeviceConfigured;
                        Log.d("BLEConfigurerGattCallbacks", "System state: DeviceConfigured");
                        listener.onConfigFinished();
                    }
                    break;
                }
                case BLEGattService.ACTION_GATT_DISCONNECTED:
                    listener.onError(ErrorCode.UnexpectedDisconnect);
                    break;
            }
        }
    }

    private boolean discoverService(){
        boolean discoverResult= false;

        List<BluetoothGattService> gattServices= bluetoothService.getSupportedGattServices();

        if (gattServices == null) return false;
        String uuid;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();

            if(uuid.equals(SERVICE_UUID)){
                discoverResult= characteristicsManager.discoverCharacteristics(gattService);
                break;
            }
        }

        // Check if every characteristic was discovered
        return discoverResult;
    }



    public String getConfigWifiSSID() {
        if(characteristicsManager!=null)
            return characteristicsManager.getWiFiSSID();
        else
            return "";
    }


    public String getConfigWifiPSK() {
        if(characteristicsManager!=null)
            return characteristicsManager.getWiFiPSK();
        else
            return "";
    }


    public String getConfigPicklock() {
        if(characteristicsManager!=null)
            return characteristicsManager.getPicklock();
        else
            return "";
    }

    public String getConfigMAC() {
        if(characteristicsManager!=null)
            return characteristicsManager.getMAC();
        else
            return "";
    }

    public String getConfigTimezone() {
        if(characteristicsManager!=null)
            return characteristicsManager.getTimezone();
        else
            return "";
    }

    public Device.Type getConnectedDevType() {
        return connectedDevType;
    }

    public void setBleName(String bleName) {
        this.bleName = bleName;
    }

    public String getFoundDeviceName() {
        return foundDeviceName;
    }

    public void startConfigWrite(String wifiSSID, String wifiPSK, String uid, String picklock, String timezone){
        state= State.WritingConfig;
        characteristicsManager.setConfigWifiSSID(wifiSSID);
        characteristicsManager.setConfigWifiPSK(wifiPSK);
        characteristicsManager.setConfigUID(uid);
        characteristicsManager.setConfigPicklock(picklock);
        characteristicsManager.setConfigTimezone(timezone);

        characteristicsManager.startWrite();
    }
}
