package pl.dawidkulpa.miogiapiccohome;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.util.List;
import java.util.Objects;

public class BLEConfigurerRawCharacteristics extends BLEConfigurerCharacteristics{
    public static final String BLE_CHAR_UUID_WIFI_SSID      = "345ac506-c96e-45c6-a418-56a2ef2d6072";
    public static final String BLE_CHAR_UUID_WIFI_PSK       = "b675ddff-679e-458d-9960-939d8bb03572";
    public static final String BLE_CHAR_UUID_UID            = "566f9eb0-a95e-4c18-bc45-79bd396389af";
    public static final String BLE_CHAR_UUID_PICKLOCK       = "f6ffba4e-eea1-4728-8b1a-7789f9a22da8";
    public static final String BLE_CHAR_UUID_MAC            = "c0cd497d-6987-41fa-9b6d-ef2e2a94e04a";
    public static final String BLE_CHAR_UUID_SET_FLAG       = "e34fc92f-7565-403b-9528-35b4650596fc";
    public static final String BLE_CHAR_UUID_TIMEZONE       = "e00758dd-7c07-42fd-8699-423b73fcb4ce";
    public static final String BLE_CHAR_UUID_WIFI_SCAN_RES  = "ef7cb0fc-53a4-4062-bb0e-25443e3a1f5d";

    // Bluetooth characteristics (null before discovered)
    private BluetoothGattCharacteristic wifiSSIDChar =null;
    private BluetoothGattCharacteristic wifiPSKChar =null;
    private BluetoothGattCharacteristic uidChar =null;
    private BluetoothGattCharacteristic picklockChar =null;
    private BluetoothGattCharacteristic macChar =null;
    private BluetoothGattCharacteristic timezoneChar =null;
    private BluetoothGattCharacteristic setFlagChar =null;
    private BluetoothGattCharacteristic wifiScanResChar= null;

    private boolean wifiSSIDCharRead = false;
    private boolean wifiPSKCharRead = false;
    private boolean uidCharRead = false;
    private boolean picklockCharRead = false;
    private boolean macCharRead = false;
    private boolean timezoneCharRead = false;
    private boolean setFlagCharRead = false;
    private boolean wifiScanResCharRead = false;

    // Config written flags
    private boolean configWifiSSIDWritten= false;
    private boolean configWifiPSKWritten= false;
    private boolean configPicklockWritten= false;
    private boolean configUIDWritten= false;
    private boolean configTimezoneWritten= false;

    private enum State {Init, ReadingCharacteristics, EnablingSetFlagNotifications, EnablingWiFiScanNotifications, Ready, ConfigWritten}
    private State state;

    BLEConfigurerRawCharacteristics(BluetoothLeService bleService, WiFiListListener listener) {
        super(bleService, listener);
        state= State.Init;
    }

    @Override
    boolean discoverCharacteristics(BluetoothGattService gattService) {
        state= State.ReadingCharacteristics;

        wifiSSIDChar = null;
        wifiPSKChar = null;
        uidChar = null;
        picklockChar = null;
        macChar = null;
        setFlagChar = null;
        timezoneChar = null;

        List<BluetoothGattCharacteristic> gattCharacteristics =
                gattService.getCharacteristics();

        for(BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics){
            String uuid = gattCharacteristic.getUuid().toString();

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

        // Check if every characteristic was loaded
        return (picklockChar !=null && macChar !=null &&
                uidChar !=null && wifiSSIDChar !=null &&
                wifiPSKChar !=null && setFlagChar !=null &&
                timezoneChar !=null && wifiScanResChar!=null);
    }

    @Override
    void startWrite() {
        configWifiSSIDWritten= false;
        configWifiPSKWritten= false;
        configPicklockWritten= false;
        configUIDWritten= false;
        configTimezoneWritten= false;

        wifiSSIDChar.setValue(configWifiSSID);
        wifiPSKChar.setValue(configWifiPSK);
        uidChar.setValue(configUID);
        picklockChar.setValue(configPicklock);
        timezoneChar.setValue(configTimezone);

        bleService.writeCharacteristic(timezoneChar);
        bleService.writeCharacteristic(wifiSSIDChar);
        bleService.writeCharacteristic(wifiPSKChar);
        bleService.writeCharacteristic(uidChar);
        bleService.writeCharacteristic(picklockChar);
    }

    @Override
    void startRead() {
        wifiSSIDCharRead = false;
        wifiPSKCharRead = false;
        uidCharRead = false;
        picklockCharRead = false;
        macCharRead = false;
        timezoneCharRead = false;
        setFlagCharRead = false;
        wifiScanResCharRead = false;

        bleService.readCharacteristic(wifiSSIDChar);
        bleService.readCharacteristic(wifiPSKChar);
        bleService.readCharacteristic(uidChar);
        bleService.readCharacteristic(macChar);
        bleService.readCharacteristic(picklockChar);
        bleService.readCharacteristic(timezoneChar);
        bleService.readCharacteristic(wifiScanResChar);
    }

    @Override
    boolean preparedAndReady() {
        return state==State.Ready;
    }

    private boolean allCharsRead(){
        return wifiSSIDCharRead && wifiPSKCharRead && uidCharRead && picklockCharRead &&
                macCharRead && timezoneCharRead  && wifiScanResCharRead;
    }

    @Override
    void onPreparingDataAvailable(String uuid, String data) {
        switch (Objects.requireNonNull(uuid)) {
            case BLE_CHAR_UUID_PICKLOCK:
                Log.e("NewDeviceActivity", "Picklock characteristic data received!: "+data);
                configPicklock = data;
                picklockCharRead = true;
                break;
            case BLE_CHAR_UUID_WIFI_SSID:
                Log.e("NewDeviceActivity", "WiFi SSID characteristic data received!: "+data);
                configWifiSSID = data;
                wifiSSIDCharRead = true;
                break;
            case BLE_CHAR_UUID_WIFI_PSK:
                Log.e("NewDeviceActivity", "WiFi PSK characteristic data received!: "+data);
                configWifiPSK = data;
                wifiPSKCharRead = true;
                break;
            case BLE_CHAR_UUID_UID:
                Log.e("NewDeviceActivity", "UID characteristic data received!: "+data);
                configUID = data;
                uidCharRead = true;
                break;
            case BLE_CHAR_UUID_MAC:
                Log.e("NewDeviceActivity", "MAC characteristic data received!:= "+data);
                configMAC = data;
                macCharRead = true;
                break;
            case BLE_CHAR_UUID_TIMEZONE:
                Log.e("NewDeviceActivity", "Timezone characteristic data received!: "+data);
                configTimezone = data;
                timezoneCharRead = true;
                break;
            case BLE_CHAR_UUID_WIFI_SCAN_RES:
                wifiSSIDsCSV= data;
                wifiScanResCharRead = true;
                Log.e("NewDeviceActivity", "WiFi scan result characteristic data received!: "+data);
                break;
        }

        if(allCharsRead()){
            state= State.EnablingSetFlagNotifications;
            bleService.allowNotificationsFor(setFlagChar);
        }
    }

    @Override
    void onPreparingDescriptorUpdate() {
        // TODO: Check which descriptor was updated
        if(state==State.EnablingSetFlagNotifications){
            state= State.EnablingWiFiScanNotifications;
            bleService.allowNotificationsFor(wifiScanResChar);
        } else if(state==State.EnablingWiFiScanNotifications){
            state= State.Ready;
        }
    }

    @Override
    void onReadyCharacteristicChanged(String uuid) {
        if(Objects.equals(uuid, BLE_CHAR_UUID_WIFI_SCAN_RES)) {
            bleService.readCharacteristic(wifiScanResChar);
        }
    }

    @Override
    void onReadyDataAvailable(String uuid, String data) {
        if(Objects.equals(uuid, BLE_CHAR_UUID_WIFI_SCAN_RES) && data!=null){
            wiFiListListener.onRefresh(data);
        }
    }

    private boolean allCharWriten(){
        return configWifiSSIDWritten && configWifiPSKWritten && configPicklockWritten &&
                configUIDWritten && configTimezoneWritten;
    }

    @Override
    void onWritingWriteComplete(String uuid) {
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

        if (allCharWriten()) {
            //parent.stopTimeoutWatchdog();
            //parent.setState(BLEConfigurer.ConfigurerState.NotifyingCharacteristicsReady);
            //Log.d("NewDeviceActivity", "System state: NotifyingCharacteristicsReady");
            setFlagChar.setValue(new byte[]{0x01});
            bleService.writeCharacteristic(setFlagChar);
            //parent.startTimeoutWatchdog(ACTION_TIMEOUT_DEVICE_CONFIGURED_RESPONSE);
        }
    }

    @Override
    void onWritingCharacteristicChanged(String uuid, String data) {
        if(uuid!=null && data!=null && uuid.equals(BLE_CHAR_UUID_SET_FLAG) && data.equals("0")) {
            state= State.ConfigWritten;
        }
    }

    @Override
    boolean writingComplete() {
        return state==State.ConfigWritten;
    }
}
