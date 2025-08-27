package pl.dawidkulpa.miogiapiccohome;

import android.bluetooth.BluetoothGattService;

abstract public class BLEConfigurerCharacteristics {
    protected String configWifiSSID="";
    protected String configWifiPSK="";
    protected String configPicklock="";
    protected String configUID="";
    protected String configMAC="";
    protected String configTimezone="";
    protected String wifiSSIDsCSV="";

    public enum ErrorCode {SyncFailed, WriteFailed}

    final TimeoutWatchdog timeoutWatchdog= new TimeoutWatchdog();

    public interface ActionsListener {
        void onRefresh(String wifis);
        void onError(ErrorCode ec);
    }

    BLEGattService bleService;
    ActionsListener actionsListener;

    BLEConfigurerCharacteristics(BLEGattService bleService, ActionsListener listener){
        this.bleService= bleService;
        actionsListener = listener;
    }

    abstract boolean discoverCharacteristics(BluetoothGattService gattService);
    abstract void startWrite();
    abstract void startSync();
    abstract boolean preparedAndReady();


    abstract void onPreparingDataAvailable(String uuid, byte[] data);
    abstract void onPreparingDescriptorUpdate(String uuid);
    abstract void onPreparingNotify(String uuid, byte[] data);

    abstract void onReadyNotify(String uuid, byte[] data);
    abstract void onReadyDataAvailable(String uuid, byte[] data);

    abstract void onWritingWriteComplete(String uuid);
    abstract void onWritingNotify(String uuid, byte[] data);
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
