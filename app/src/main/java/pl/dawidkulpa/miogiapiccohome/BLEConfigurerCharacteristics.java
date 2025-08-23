package pl.dawidkulpa.miogiapiccohome;

import android.bluetooth.BluetoothGattService;

abstract public class BLEConfigurerCharacteristics {
    protected String configWifiSSID;
    protected String configWifiPSK;
    protected String configPicklock;
    protected String configUID;
    protected String configMAC;
    protected String configTimezone;
    protected String wifiSSIDsCSV;

    public interface WiFiListListener {
        void onRefresh(String wifis);
    }

    BluetoothLeService bleService;
    WiFiListListener wiFiListListener;

    BLEConfigurerCharacteristics(BluetoothLeService bleService, WiFiListListener listener){
        this.bleService= bleService;
        wiFiListListener= listener;
    }

    abstract boolean discoverCharacteristics(BluetoothGattService gattService);
    abstract void startWrite();
    abstract void startRead();
    abstract boolean preparedAndReady();


    abstract void onPreparingDataAvailable(String uuid, String data);
    abstract void onPreparingDescriptorUpdate();

    abstract void onReadyCharacteristicChanged(String uuid);
    abstract void onReadyDataAvailable(String uuid, String data);

    abstract void onWritingWriteComplete(String uuid);
    abstract void onWritingCharacteristicChanged(String uuid, String data);
    abstract boolean writingComplete();

    public String getWiFiSSID(){
        return configWifiSSID;
    }

    public String getWiFiPSK(){
        return configWifiPSK;
    }

    public String getPicklock(){
        return configPicklock;
    }

    public String getMAC(){
        return configMAC;
    }

    public String getTimezone(){
        return configTimezone;
    }

    public void setConfigWifiSSID(String configWifiSSID) {
        this.configWifiSSID = configWifiSSID;
    }

    public void setConfigWifiPSK(String configWifiPSK) {
        this.configWifiPSK = configWifiPSK;
    }

    public void setConfigPicklock(String configPicklock) {
        this.configPicklock = configPicklock;
    }

    public void setConfigUID(String configUID) {
        this.configUID = configUID;
    }

    public void setConfigTimezone(String configTimezone) {
        this.configTimezone = configTimezone;
    }
}
