package pl.dawidkulpa.miogiapiccohome.ble;

import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.List;

import pl.dawidkulpa.miogiapiccohome.API.Device;
import pl.dawidkulpa.miogiapiccohome.R;
import pl.dawidkulpa.miogiapiccohome.TimeoutWatchdog;

public class BLEConfigurerGatt extends BroadcastReceiver {
    public static final int ACTION_TIMEOUT_GATT_CONNECTION              = 3000;

    private enum State {Idle, Connecting, Syncing, WaitingForUserInput, WritingConfig, DeviceConfigured, ConnectionFailed}
    private State state;

    public enum ErrorCode {ConnectFailed, DiscoverFailed, SyncFailed, ConfigWriteFailed, UnexpectedDisconnect}

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
    private String bleAddress;

    BLEGattService bluetoothService=null;
    ConfigurerGattListener listener;
    BLEConfigurerCharacteristics characteristicsManager= null;
    TimeoutWatchdog timeoutWatchdog= new TimeoutWatchdog();

    // Service connecting callback
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e("ServiceConnection", "service connected");
            bluetoothService = ((BLEGattService.LocalBinder) service).getService();
            if (bluetoothService != null) {
                if (bluetoothService.initialize()) {
                    if(bleAddress!=null && !bleAddress.isEmpty()) {
                        bluetoothService.connect(bleAddress);
                        state= State.Connecting;

                    } else {
                        Log.e("ServiceConnection", "Address empty");
                        listener.onError(ErrorCode.ConnectFailed);
                    }
                } else {
                    Log.e("ServiceConnection", "BLEService init failed");
                    listener.onError(ErrorCode.ConnectFailed);
                }
            } else {
                Log.e("ServiceConnection", "BLEService null");
                listener.onError(ErrorCode.ConnectFailed);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e("ServiceConnection", "service disconnected");
            bluetoothService = null;
        }
    };


    public BLEConfigurerGatt(ConfigurerGattListener configurerGattListener){
        listener= configurerGattListener;
        state= State.Idle;
    }

    void startConnectAndReceiving(Context c, String address, String name){
        bleName= name;
        bleAddress= address;
        bindGattService(c, address);
    }

    public void bindGattService(Context c, String address){
        ContextCompat.registerReceiver(c, this, makeGattUpdateIntentFilter(), ContextCompat.RECEIVER_EXPORTED);
        bleAddress= address;
        Intent gattServiceIntent = new Intent(c, BLEGattService.class);

        // Call actual connect when gattService bind finished -> serviceConnection.onServiceConnected callback
        c.bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        timeoutWatchdog.start(ACTION_TIMEOUT_GATT_CONNECTION, this::onConnctTimeout);
    }

    private void onConnctTimeout(){
        state = State.ConnectionFailed;
        listener.onError(ErrorCode.ConnectFailed);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEGattService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BLEGattService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BLEGattService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BLEGattService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BLEGattService.ACTION_DATA_WRITE_COMPLETE);
        intentFilter.addAction(BLEGattService.ACTION_DESCR_WRITE_COMPLETE);
        intentFilter.addAction(BLEGattService.ACTION_CHARACTERISTIC_CHANGED);
        return intentFilter;
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
                    timeoutWatchdog.stop();
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

    public void finish(Context c){
        if(characteristicsManager!=null)
            characteristicsManager.finish();

        if(bluetoothService!=null){
            bluetoothService.finish();
        }

        timeoutWatchdog.stop();

        try{
            c.unbindService(serviceConnection);
        } catch (IllegalArgumentException e){
            Log.w("NewDeviceActivity", "BLE Service not bound");
        }
    }

    public void restart(){
        connectedDevType= Device.Type.Unknown;
        foundDeviceName="";
        bleName="";
        state = State.Idle;
        timeoutWatchdog.stop();

        if(bluetoothService!=null)
            bluetoothService.restart();

        if(characteristicsManager!=null)
            characteristicsManager.restart();
    }
}
