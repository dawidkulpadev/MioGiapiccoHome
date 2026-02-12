package pl.dawidkulpa.miogiapiccohome.ble;

import android.bluetooth.BluetoothGattService;

import pl.dawidkulpa.miogiapiccohome.TimeoutWatchdog;

abstract public class BLEConfigurerCharacteristics {
    protected String configWifiSSID="";
    protected String configWifiPSK="";
    protected String configPicklock="";
    protected String configUID="";
    protected String configMAC="";
    protected String configTimezone="";
    protected String wifiSSIDsCSV="";
    protected int configRole=0;



    protected String configDevSignBase64="";

    public enum ErrorCode {SyncFailed, WriteFailed}

    final TimeoutWatchdog timeoutWatchdog= new TimeoutWatchdog();

    public interface ActionsListener {
        void onSyncProgress(int progress, int msgResId);
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


    abstract void preparingStateOnValueReceived(String uuid, byte[] data);
    abstract void preparingStateOnDescriptorUpdate(String uuid);
    abstract void preparingStateOnNotify(String uuid, byte[] data);

    abstract void readyStateOnNotify(String uuid, byte[] data);
    abstract void readyStateOnValueReceived(String uuid, byte[] data);

    abstract void writingStateOnWriteComplete(String uuid);
    abstract void writingStateOnNotify(String uuid, byte[] data);
    abstract boolean writingComplete();
    abstract void restart();
    abstract void finish();

    abstract String getDevicesPubKey();

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

    public int getRole(){ return configRole; }

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

    public void setConfigRole(int role){
        this.configRole= role;
    }

    public void setConfigDevSignBase64(String configDevSignBase64) {
        this.configDevSignBase64 = configDevSignBase64;
    }
}
