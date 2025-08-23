package pl.dawidkulpa.miogiapiccohome;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.List;
import java.util.Objects;

import pl.dawidkulpa.miogiapiccohome.API.Device;
import pl.dawidkulpa.miogiapiccohome.activities.NewDeviceActivity;

public class BLEConfigurerGattCallbacks extends BroadcastReceiver {
    public static final int ACTION_TIMEOUT_READ_CHARACTERISTICS         = 5000;
    public static final int ACTION_TIMEOUT_ENABLE_NOTIFICATIONS         = 15000;

    public static final int ACTION_TIMEOUT_DEVICE_CONFIGURED_RESPONSE   = 15000;

    public static final String SERVICE_UUID                 = "952cb13b-57fa-4885-a445-57d1f17328fd";
    public static final String BLE_CHAR_UUID_WIFI_SSID      = "345ac506-c96e-45c6-a418-56a2ef2d6072";
    public static final String BLE_CHAR_UUID_WIFI_PSK       = "b675ddff-679e-458d-9960-939d8bb03572";
    public static final String BLE_CHAR_UUID_UID            = "566f9eb0-a95e-4c18-bc45-79bd396389af";
    public static final String BLE_CHAR_UUID_PICKLOCK       = "f6ffba4e-eea1-4728-8b1a-7789f9a22da8";
    public static final String BLE_CHAR_UUID_MAC            = "c0cd497d-6987-41fa-9b6d-ef2e2a94e04a";
    public static final String BLE_CHAR_UUID_SET_FLAG       = "e34fc92f-7565-403b-9528-35b4650596fc";
    public static final String BLE_CHAR_UUID_TIMEZONE       = "e00758dd-7c07-42fd-8699-423b73fcb4ce";
    public static final String BLE_CHAR_UUID_WIFI_SCAN_RES  = "ef7cb0fc-53a4-4062-bb0e-25443e3a1f5d";



    public interface ConfigurerGattListener {
        void onConnectionFailed();
        void onDeviceBond();
        void onWiFisRefresh(String wifis);
        void onConfigFinished(boolean success);
    }

    private String configWifiSSID;
    private String configWifiPSK;
    private String configPicklock;
    private String configUID;
    private String configMAC;
    private String configTimezone;

    // Config written flags
    private boolean configWifiSSIDWritten= false;
    private boolean configWifiPSKWritten= false;
    private boolean configPicklockWritten= false;
    private boolean configUIDWritten= false;
    private boolean configTimezoneWritten= false;



    private Device.Type connectedDevType= Device.Type.Unknown;

    private String wifiSSIDsCSV;
    private String foundDeviceName;
    private String bleName;

    BluetoothLeService bluetoothService;
    ConfigurerGattListener listener;

    // Bluetooth characteristics (null before discovered)
    private BluetoothGattCharacteristic wifiSSIDChar =null;
    private BluetoothGattCharacteristic wifiPSKChar =null;
    private BluetoothGattCharacteristic uidChar =null;
    private BluetoothGattCharacteristic picklockChar =null;
    private BluetoothGattCharacteristic macChar =null;
    private BluetoothGattCharacteristic timezoneChar =null;
    private BluetoothGattCharacteristic setFlagChar =null;
    private BluetoothGattCharacteristic wifiScanResChar= null;


    BLEConfigurerCharacteristics characteristicsManager;

    BLEConfigurer parent;


    public BLEConfigurerGattCallbacks(BLEConfigurer parent, BluetoothLeService bleService, ConfigurerGattListener configurerGattListener){
        bluetoothService= bleService;
        listener= configurerGattListener;
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
                configWifiSSID = null;
                configWifiPSK = null;
                configUID= null;
                configPicklock= null;
                configMAC= null;
                configTimezone= null;
                wifiSSIDsCSV= null;

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

                Log.d("NewDeviceActivity", "System state: WaitingForServices");
            } else if(action!=null && action.equals(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)){
                parent.stopTimeoutWatchdog();
                if(loadCharacteristics()){
                    parent.setState(BLEConfigurer.ConfigurerState.ReadingCharacteristics);
                    Log.d("NewDeviceActivity", "System state: ReadingCharacteristics");
                    readAllConfigCharacteristic();
                    parent.startTimeoutWatchdog(ACTION_TIMEOUT_READ_CHARACTERISTICS);
                } else {
                    parent.setState(BLEConfigurer.ConfigurerState.ConnectionFailed);
                    listener.onConnectionFailed();
                    Log.d("NewDeviceActivity", "System state: ConnectionFailed (Characteristics not found)");
                }
            }
        } else if(parent.getState()== BLEConfigurer.ConfigurerState.ReadingCharacteristics){
            if(action!=null && action.equals(BluetoothLeService.ACTION_DATA_AVAILABLE)){
                String uuid= intent.getStringExtra("uuid");
                String data= intent.getStringExtra("data");
                if(data==null)
                    data="";

                switch (Objects.requireNonNull(uuid)) {
                    case BLE_CHAR_UUID_PICKLOCK:
                        Log.e("NewDeviceActivity", "Picklock characteristic data received!");
                        configPicklock = data;
                        break;
                    case BLE_CHAR_UUID_WIFI_SSID:
                        Log.e("NewDeviceActivity", "WiFi SSID characteristic data received!");
                        configWifiSSID = data;
                        break;
                    case BLE_CHAR_UUID_WIFI_PSK:
                        Log.e("NewDeviceActivity", "WiFi PSK characteristic data received!");
                        configWifiPSK = data;
                        break;
                    case BLE_CHAR_UUID_UID:
                        Log.e("NewDeviceActivity", "UID characteristic data received!");
                        configUID = data;
                        break;
                    case BLE_CHAR_UUID_MAC:
                        Log.e("NewDeviceActivity", "MAC characteristic data received!");
                        configMAC = data;
                        break;
                    case BLE_CHAR_UUID_TIMEZONE:
                        Log.e("NewDeviceActivity", "Timezone characteristic data received!");
                        configTimezone = data;
                        break;
                    case BLE_CHAR_UUID_WIFI_SCAN_RES:
                        wifiSSIDsCSV= data;
                        Log.e("NewDeviceActivity", "WiFi scan result characteristic data received!");
                        break;
                }

                if( configWifiSSID !=null && configWifiPSK !=null &&
                        configUID!=null && configPicklock!=null &&
                        configMAC!=null && configTimezone!=null && wifiSSIDsCSV!=null){
                    parent.stopTimeoutWatchdog();
                    parent.setState(BLEConfigurer.ConfigurerState.EnablingSetFlagNotifications);
                    bluetoothService.allowNotificationsFor(setFlagChar);

                    parent.startTimeoutWatchdog(ACTION_TIMEOUT_ENABLE_NOTIFICATIONS);
                    Log.d("NewDeviceActivity", "System state: EnablingNotifications");
                }
            }
        } else if(parent.getState()== BLEConfigurer.ConfigurerState.EnablingSetFlagNotifications){
            if(action!=null && action.equals(BluetoothLeService.ACTION_DESCR_WRITE_COMPLETE)) {
                parent.setState(BLEConfigurer.ConfigurerState.EnablingWiFiScanNotifications);
                bluetoothService.allowNotificationsFor(wifiScanResChar);
            }
        }
        else if(parent.getState()== BLEConfigurer.ConfigurerState.EnablingWiFiScanNotifications){
            if(action!=null && action.equals(BluetoothLeService.ACTION_DESCR_WRITE_COMPLETE)) {
                parent.stopTimeoutWatchdog();
                listener.onDeviceBond();


            }
        } else if(parent.getState()== BLEConfigurer.ConfigurerState.WaitingForUserInput){
            if(action!=null && action.equals(BluetoothLeService.ACTION_CHARACTERISTIC_CHANGED)){
                String uuid= intent.getStringExtra("uuid");
                if(Objects.equals(uuid, BLE_CHAR_UUID_WIFI_SCAN_RES)) {
                    bluetoothService.readCharacteristic(wifiScanResChar);
                }
            } else if(action!=null && action.equals(BluetoothLeService.ACTION_DATA_AVAILABLE)){
                String uuid= intent.getStringExtra("uuid");
                String data= intent.getStringExtra("data");

                if(Objects.equals(uuid, BLE_CHAR_UUID_WIFI_SCAN_RES) && data!=null){
                    listener.onWiFisRefresh(data);
                }
            }
        } else if(parent.getState()== BLEConfigurer.ConfigurerState.WritingCharacteristics){
            if(action!=null && action.equals(BluetoothLeService.ACTION_DATA_WRITE_COMPLETE)){
                String uuid= intent.getStringExtra("uuid");

                switch (Objects.requireNonNull(uuid)) {
                    case BLE_CHAR_UUID_PICKLOCK:
                        configPicklockWritten= true;
                        Log.e("NewDeviceActivity", "Picklock characteristic written!");
                        break;
                    case BLE_CHAR_UUID_WIFI_SSID:
                        configWifiSSIDWritten= true;
                        Log.e("NewDeviceActivity", "WiFi SSID characteristic written!");
                        break;
                    case BLE_CHAR_UUID_WIFI_PSK:
                        configWifiPSKWritten = true;
                        Log.e("NewDeviceActivity", "WiFi PSK characteristic written!");
                        break;
                    case BLE_CHAR_UUID_UID:
                        configUIDWritten = true;
                        Log.e("NewDeviceActivity", "UID characteristic written!");
                        break;
                    case BLE_CHAR_UUID_TIMEZONE:
                        configTimezoneWritten= true;
                        Log.e("NewDeviceActivity", "Timezone characteristic written!");
                }

                if( configWifiSSIDWritten && configWifiPSKWritten &&
                        configUIDWritten && configPicklockWritten && configTimezoneWritten){
                    parent.stopTimeoutWatchdog();
                    parent.setState(BLEConfigurer.ConfigurerState.NotifyingCharacteristicsReady);
                    Log.d("NewDeviceActivity", "System state: NotifyingCharacteristicsReady");
                    setFlagChar.setValue(new byte[]{0x01});
                    bluetoothService.writeCharacteristic(setFlagChar);
                    parent.startTimeoutWatchdog(ACTION_TIMEOUT_DEVICE_CONFIGURED_RESPONSE);
                }
            }
        } else if(parent.getState()== BLEConfigurer.ConfigurerState.NotifyingCharacteristicsReady){
            if(action!=null && action.equals(BluetoothLeService.ACTION_CHARACTERISTIC_CHANGED)){
                String uuid= intent.getStringExtra("uuid");
                String data= intent.getStringExtra("data");

                if(uuid!=null && data!=null && uuid.equals(BLE_CHAR_UUID_SET_FLAG) && data.equals("0")) {
                    parent.stopTimeoutWatchdog();
                    parent.setState(BLEConfigurer.ConfigurerState.DeviceConfigured);
                    Log.d("NewDeviceActivity", "System state: DeviceConfigured");
                    listener.onConfigFinished(true);
                }
            }
        }
    }

    private void readAllConfigCharacteristic(){
        bluetoothService.readCharacteristic(wifiSSIDChar);
        bluetoothService.readCharacteristic(wifiPSKChar);
        bluetoothService.readCharacteristic(uidChar);
        bluetoothService.readCharacteristic(macChar);
        bluetoothService.readCharacteristic(picklockChar);
        bluetoothService.readCharacteristic(timezoneChar);
        bluetoothService.readCharacteristic(wifiScanResChar);
    }

    private boolean loadCharacteristics(){
        wifiSSIDChar = null;
        wifiPSKChar = null;
        uidChar = null;
        picklockChar = null;
        macChar = null;
        setFlagChar = null;
        timezoneChar = null;

        List<BluetoothGattService> gattServices= bluetoothService.getSupportedGattServices();

        if (gattServices == null) return false;
        String uuid;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();

            if(uuid.equals(SERVICE_UUID)){
                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();

                for(BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics){
                    uuid = gattCharacteristic.getUuid().toString();

                    switch (uuid){
                        case BLE_CHAR_UUID_WIFI_SSID:
                            wifiSSIDChar = gattCharacteristic;
                            break;
                        case BLE_CHAR_UUID_WIFI_PSK:
                            wifiPSKChar = gattCharacteristic;
                            break;
                        case BLE_CHAR_UUID_UID:
                            uidChar = gattCharacteristic;
                            break;
                        case BLE_CHAR_UUID_PICKLOCK:
                            picklockChar = gattCharacteristic;
                            break;
                        case BLE_CHAR_UUID_MAC:
                            macChar = gattCharacteristic;
                            break;
                        case BLE_CHAR_UUID_SET_FLAG:
                            setFlagChar = gattCharacteristic;
                            break;
                        case BLE_CHAR_UUID_TIMEZONE:
                            timezoneChar = gattCharacteristic;
                            break;
                        case BLE_CHAR_UUID_WIFI_SCAN_RES:
                            wifiScanResChar= gattCharacteristic;

                    }
                }

                break;
            }
        }

        // Check if every characteristic was loaded
        return (picklockChar !=null && macChar !=null &&
                uidChar !=null && wifiSSIDChar !=null &&
                wifiPSKChar !=null && setFlagChar !=null &&
                timezoneChar !=null && wifiScanResChar!=null);
    }



    public String getConfigWifiSSID() {
        return configWifiSSID;
    }

    public void setConfigWifiSSID(String configWifiSSID) {
        this.configWifiSSID = configWifiSSID.trim();
    }

    public String getConfigWifiPSK() {
        return configWifiPSK;
    }

    public void setConfigWifiPSK(String configWifiPSK) {
        this.configWifiPSK = configWifiPSK.trim();
    }

    public String getConfigPicklock() {
        return configPicklock;
    }

    public void setConfigPicklock(String configPicklock) {
        this.configPicklock = configPicklock;
    }

    public String getConfigUID() {
        return configUID;
    }

    public void setConfigUID(String configUID) {
        this.configUID = configUID;
    }

    public String getConfigMAC() {
        return configMAC;
    }

    public void setConfigMAC(String configMAC) {
        this.configMAC = configMAC;
    }

    public String getConfigTimezone() {
        return configTimezone;
    }

    public void setConfigTimezone(String configTimezone) {
        this.configTimezone = configTimezone;
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

    public void writeCharacteristics(String uid, String picklock){
        wifiSSIDChar.setValue(configWifiSSID);
        wifiPSKChar.setValue(configWifiPSK);
        uidChar.setValue(uid);
        picklockChar.setValue(picklock);
        timezoneChar.setValue(configTimezone);

        bluetoothService.writeCharacteristic(timezoneChar);
        bluetoothService.writeCharacteristic(wifiSSIDChar);
        bluetoothService.writeCharacteristic(wifiPSKChar);
        bluetoothService.writeCharacteristic(uidChar);
        bluetoothService.writeCharacteristic(picklockChar);
    }
}
