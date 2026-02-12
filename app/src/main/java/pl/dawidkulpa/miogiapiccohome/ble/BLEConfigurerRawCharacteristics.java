package pl.dawidkulpa.miogiapiccohome.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import pl.dawidkulpa.miogiapiccohome.R;

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
    private boolean wifiScanResCharRead = false;

    // Config written flags
    private boolean configWifiSSIDWritten= false;
    private boolean configWifiPSKWritten= false;
    private boolean configPicklockWritten= false;
    private boolean configUIDWritten= false;
    private boolean configTimezoneWritten= false;

    private enum State {Init, ReadingCharacteristics, EnablingSetFlagNotifications, EnablingWiFiScanNotifications, Ready, ConfigWritten}
    private State state;

    BLEConfigurerRawCharacteristics(BLEGattService bleService, ActionsListener listener) {
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

        actionsListener.onSyncProgress(20, R.string.message_connect_step_gen2_reading_config);

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

        bleService.writeCharacteristic(timezoneChar, configTimezone.getBytes());
        bleService.writeCharacteristic(wifiSSIDChar, configWifiSSID.getBytes());
        bleService.writeCharacteristic(wifiPSKChar, configWifiPSK.getBytes());
        bleService.writeCharacteristic(uidChar, configUID.getBytes());
        bleService.writeCharacteristic(picklockChar, configPicklock.getBytes());
    }

    @Override
    void startSync() {
        wifiSSIDCharRead = false;
        wifiPSKCharRead = false;
        uidCharRead = false;
        picklockCharRead = false;
        macCharRead = false;
        timezoneCharRead = false;
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
    void preparingStateOnValueReceived(String uuid, byte[] data) {
        String dataStr= new String(data, StandardCharsets.UTF_8);

        switch (Objects.requireNonNull(uuid)) {
            case BLE_CHAR_UUID_PICKLOCK:
                Log.e("NewDeviceActivity", "Picklock characteristic data received!: "+dataStr);
                configPicklock = dataStr;
                picklockCharRead = true;
                actionsListener.onSyncProgress(30, -1);
                break;
            case BLE_CHAR_UUID_WIFI_SSID:
                Log.e("NewDeviceActivity", "WiFi SSID characteristic data received!: "+dataStr);
                configWifiSSID = dataStr;
                wifiSSIDCharRead = true;
                actionsListener.onSyncProgress(40, -1);
                break;
            case BLE_CHAR_UUID_WIFI_PSK:
                Log.e("NewDeviceActivity", "WiFi PSK characteristic data received!: "+dataStr);
                configWifiPSK = dataStr;
                wifiPSKCharRead = true;
                actionsListener.onSyncProgress(50, -1);
                break;
            case BLE_CHAR_UUID_UID:
                Log.e("NewDeviceActivity", "UID characteristic data received!: "+dataStr);
                configUID = dataStr;
                uidCharRead = true;
                actionsListener.onSyncProgress(60, -1);
                break;
            case BLE_CHAR_UUID_MAC:
                Log.e("NewDeviceActivity", "MAC characteristic data received!:= "+dataStr);
                configMAC = dataStr;
                macCharRead = true;
                actionsListener.onSyncProgress(70, -1);
                break;
            case BLE_CHAR_UUID_TIMEZONE:
                Log.e("NewDeviceActivity", "Timezone characteristic data received!: "+dataStr);
                configTimezone = dataStr;
                timezoneCharRead = true;
                actionsListener.onSyncProgress(80, -1);
                break;
            case BLE_CHAR_UUID_WIFI_SCAN_RES:
                wifiSSIDsCSV= dataStr;
                wifiScanResCharRead = true;
                Log.e("NewDeviceActivity", "WiFi scan result characteristic data received!: "+dataStr);
                break;
        }

        if(allCharsRead()){
            actionsListener.onSyncProgress(90, -1);
            state= State.EnablingSetFlagNotifications;
            bleService.allowNotificationsFor(setFlagChar);
        }
    }

    @Override
    void preparingStateOnDescriptorUpdate(String uuid) {
        if(state==State.EnablingSetFlagNotifications){
            state= State.EnablingWiFiScanNotifications;
            bleService.allowNotificationsFor(wifiScanResChar);
            actionsListener.onSyncProgress(95, -1);
        } else if(state==State.EnablingWiFiScanNotifications){
            actionsListener.onSyncProgress(100, -1);
            state= State.Ready;
        }
    }

    @Override
    void preparingStateOnNotify(String uuid, byte[] data) {

    }

    @Override
    void readyStateOnNotify(String uuid, byte[] data) {
        if(Objects.equals(uuid, BLE_CHAR_UUID_WIFI_SCAN_RES)) {
            bleService.readCharacteristic(wifiScanResChar);
        }
    }

    @Override
    void readyStateOnValueReceived(String uuid, byte[] data) {
        String dataStr= new String(data, StandardCharsets.UTF_8);

        if(Objects.equals(uuid, BLE_CHAR_UUID_WIFI_SCAN_RES) && data!=null){
            actionsListener.onRefresh(dataStr);
        }
    }

    private boolean allCharWriten(){
        return configWifiSSIDWritten && configWifiPSKWritten && configPicklockWritten &&
                configUIDWritten && configTimezoneWritten;
    }

    @Override
    void writingStateOnWriteComplete(String uuid) {
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
            bleService.writeCharacteristic(setFlagChar, new byte[]{0x01});
        }
    }

    @Override
    void writingStateOnNotify(String uuid, byte[] data) {
        String dataStr= new String(data, StandardCharsets.UTF_8);

        if(uuid!=null && uuid.equals(BLE_CHAR_UUID_SET_FLAG) && dataStr.equals("0")) {
            state= State.ConfigWritten;
        }
    }

    @Override
    boolean writingComplete() {
        return state==State.ConfigWritten;
    }

    @Override
    String getDevicesPubKey() {
        return "";
    }

    @Override
    void restart() {
        finish();
        state= State.Init;
    }

    @Override
    void finish() {
        timeoutWatchdog.stop();

        wifiSSIDChar =null;
        wifiPSKChar =null;
        uidChar =null;
        picklockChar =null;
        macChar =null;
        timezoneChar =null;
        setFlagChar =null;
        wifiScanResChar= null;

        wifiSSIDCharRead = false;
        wifiPSKCharRead = false;
        uidCharRead = false;
        picklockCharRead = false;
        macCharRead = false;
        timezoneCharRead = false;
        wifiScanResCharRead = false;

        // Config written flags
        configWifiSSIDWritten= false;
        configWifiPSKWritten= false;
        configPicklockWritten= false;
        configUIDWritten= false;
        configTimezoneWritten= false;
    }
}
