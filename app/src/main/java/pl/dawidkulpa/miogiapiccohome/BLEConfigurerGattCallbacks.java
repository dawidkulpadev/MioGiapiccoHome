package pl.dawidkulpa.miogiapiccohome;

import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.List;
import java.util.Objects;

import pl.dawidkulpa.miogiapiccohome.API.Device;

public class BLEConfigurerGattCallbacks extends BroadcastReceiver {
    public static final int ACTION_TIMEOUT_READ_CHARACTERISTICS         = 10000;
    public static final int ACTION_TIMEOUT_DEVICE_CONFIGURED_RESPONSE   = 15000;

    public static final String SERVICE_UUID                 = "952cb13b-57fa-4885-a445-57d1f17328fd";

    public interface ConfigurerGattListener {
        void onConnectionFailed();
        void onDeviceBond();
        void onWiFisRefresh(String wifis);
        void onConfigFinished(boolean success);
    }

    private Device.Type connectedDevType= Device.Type.Unknown;
    private String foundDeviceName;
    private String bleName;

    BluetoothLeService bluetoothService;
    ConfigurerGattListener listener;
    BLEConfigurerCharacteristics characteristicsManager;
    BLEConfigurer parent;


    public BLEConfigurerGattCallbacks(BLEConfigurer parentConfigurer, ConfigurerGattListener configurerGattListener){
        parent= parentConfigurer;
        listener= configurerGattListener;
    }

    void setBLEService(BluetoothLeService bleService){
        bluetoothService= bleService;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if(action!=null && action.equals(BluetoothLeService.ACTION_GATT_DISCONNECTED)){
            listener.onConnectionFailed();
            return;
        }

        if(parent.getState()== BLEConfigurer.ConfigurerState.ConnectingWithDevice){
            Log.d("IASD", intent.toString());
            if(action!=null && action.equals(BluetoothLeService.ACTION_GATT_CONNECTED)){
                if (bleName.contains("Light")) {
                    connectedDevType = Device.Type.Light;
                    foundDeviceName= context.getString(R.string.mg_device_name_light);
                } else if (bleName.contains("Soil")) {
                    connectedDevType = Device.Type.Soil;
                    foundDeviceName= context.getString(R.string.mg_device_name_soil);
                } else if (bleName.contains("Air")) {
                    connectedDevType = Device.Type.Air;
                    foundDeviceName = context.getString(R.string.mg_device_name_air);
                }

                if(bleName.contains("Gen2")){
                    characteristicsManager= new BLEConfigurerBLELNCharacteristics(bluetoothService, wifis -> listener.onWiFisRefresh(wifis));
                } else {
                    characteristicsManager= new BLEConfigurerRawCharacteristics(bluetoothService, wifis -> listener.onWiFisRefresh(wifis));
                }

                Log.d("NewDeviceActivity", "System state: WaitingForServices");
            } else if(action!=null && action.equals(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)){
                parent.stopTimeoutWatchdog();
                if(discoverService()){
                    parent.setState(BLEConfigurer.ConfigurerState.PreparingCharacteristicsManager);
                    Log.d("NewDeviceActivity", "System state: ReadingCharacteristics");
                    characteristicsManager.startRead();
                    parent.startTimeoutWatchdog(ACTION_TIMEOUT_READ_CHARACTERISTICS);
                } else {
                    parent.setState(BLEConfigurer.ConfigurerState.ConnectionFailed);
                    listener.onConnectionFailed();
                    Log.d("NewDeviceActivity", "System state: ConnectionFailed (Characteristics not found)");
                }
            }
        } else if(parent.getState()== BLEConfigurer.ConfigurerState.PreparingCharacteristicsManager){
            if(action!=null){
                if(action.equals(BluetoothLeService.ACTION_DATA_AVAILABLE)) {
                    String uuid = intent.getStringExtra("uuid");
                    String data = intent.getStringExtra("data");

                    if (data == null)
                        data = "";

                    characteristicsManager.onPreparingDataAvailable(uuid, data);
                } else if (action.equals(BluetoothLeService.ACTION_DESCR_WRITE_COMPLETE)){
                    characteristicsManager.onPreparingDescriptorUpdate();

                    if (characteristicsManager.preparedAndReady()) {
                        parent.stopTimeoutWatchdog();
                        parent.setState(BLEConfigurer.ConfigurerState.WaitingForUserInput);
                        listener.onDeviceBond();
                    }
                }
            }
        } else if(parent.getState()== BLEConfigurer.ConfigurerState.WaitingForUserInput){
            if(action!=null && action.equals(BluetoothLeService.ACTION_CHARACTERISTIC_CHANGED)){
                String uuid= intent.getStringExtra("uuid");
                Bundle extras = intent.getExtras();

                if (extras != null) {
                    for (String key : extras.keySet()) {
                        Object value = extras.get(key);
                        Log.d("IntentDebug", key + " = " + value + " (" + (value != null ? value.getClass().getName() : "null") + ")");
                    }
                }
                characteristicsManager.onReadyCharacteristicChanged(uuid);
            } else if(action!=null && action.equals(BluetoothLeService.ACTION_DATA_AVAILABLE)){
                String uuid= intent.getStringExtra("uuid");
                String data= intent.getStringExtra("data");
                characteristicsManager.onReadyDataAvailable(uuid, data);
            }
        } else if(parent.getState()== BLEConfigurer.ConfigurerState.WritingCharacteristics){
            if(action!=null){
                if(action.equals(BluetoothLeService.ACTION_DATA_WRITE_COMPLETE)) {
                    String uuid = intent.getStringExtra("uuid");
                    characteristicsManager.onWritingWriteComplete(uuid);
                } else if(action.equals(BluetoothLeService.ACTION_CHARACTERISTIC_CHANGED)){
                    String uuid= intent.getStringExtra("uuid");
                    String data= intent.getStringExtra("data");

                    Log.d("BLEConfigurerGattCallbacks", "Notify received - "+uuid+": "+data);
                    characteristicsManager.onWritingCharacteristicChanged(uuid, data);

                    if(characteristicsManager.writingComplete()){
                        parent.stopTimeoutWatchdog();
                        parent.setState(BLEConfigurer.ConfigurerState.DeviceConfigured);
                        Log.d("NewDeviceActivity", "System state: DeviceConfigured");
                        listener.onConfigFinished(true);
                    }
                }
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

    public void writeCharacteristics(String wifiSSID, String wifiPSK, String uid, String picklock, String timezone){
        characteristicsManager.setConfigWifiSSID(wifiSSID);
        characteristicsManager.setConfigWifiPSK(wifiPSK);
        characteristicsManager.setConfigUID(uid);
        characteristicsManager.setConfigPicklock(picklock);
        characteristicsManager.setConfigTimezone(timezone);

        characteristicsManager.startWrite();
    }
}
