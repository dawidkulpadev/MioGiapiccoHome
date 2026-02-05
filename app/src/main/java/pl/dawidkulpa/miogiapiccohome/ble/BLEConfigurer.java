package pl.dawidkulpa.miogiapiccohome.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import pl.dawidkulpa.miogiapiccohome.API.data.Device;

public class BLEConfigurer {
    /** Constants */
    public static final String MG_SSID_PREFIX = "MioGiapicco";


    public static final int ACTION_TIMEOUT_REGISTER_DEVICE              = 20000;
    public static final int ACTION_TIMEOUT_DEVICE_SEARCH                = 40000;

    public enum ErrorCode {NoDeviceFound, ConnectFailed, ConfigWriteFailed, UnexpectedDisconnect}

    /** State machines */
    private enum ConfigurerState {Init, WaitingForBluetooth, SearchingDevice, ConnectingWithDevice,
        WaitingForUserInput, WritingConfiguration, DeviceConfigured,
        ConnectionFailed}

    public interface BLEConfigurerCallbacks {
        void deviceSearchStarted();
        void onError(ErrorCode errorCode);
        void onDeviceFound(String name);
        void onDeviceConnected();
        void onDeviceReady();
        void onDeviceConfigured();
        void onWiFiListRefreshed(String wifis);
    }

    private ConfigurerState state;
    private BluetoothLeScanner bluetoothLeScanner;
    // GATT manager callbacks
    private final BLEConfigurerGatt bleConfigurerGatt;
    private boolean scanning;
    private final Context c;
    private final BLEConfigurerCallbacks callbacks;



    // Device scan callback.
    private final ScanCallback leScanCallback =
            new ScanCallback() {
                @SuppressLint("MissingPermission")
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);

                    if(state== BLEConfigurer.ConfigurerState.SearchingDevice) {
                        if (result.getDevice().getName() != null &&
                                result.getDevice().getName().contains(MG_SSID_PREFIX)) {
                            callbacks.onDeviceFound(result.getDevice().getName());
                            scanning = false;
                            scanStopHandler.removeCallbacksAndMessages(null);
                            bluetoothLeScanner.stopScan(leScanCallback);
                            state= ConfigurerState.ConnectingWithDevice;
                            Log.d("NewDeviceActivity", "System state: ConnectingWithDevice");
                            bleConfigurerGatt.startConnectAndReceiving(c,result.getDevice().getAddress(), result.getDevice().getName());
                        }
                    }
                }
            };

    // Stop bluetooth method handler
    @SuppressLint("MissingPermission")
    Runnable scanStopTask = new Runnable() {
        @Override
        public void run() {
            scanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
            state= ConfigurerState.ConnectionFailed;
            callbacks.onError(ErrorCode.NoDeviceFound);
        }
    };
    Handler scanStopHandler= new Handler();



    // Check bluetooth enabled callbacks
    Handler checkBluetoothHandler= new Handler();
    Runnable checkBluetoothRunnable= new Runnable() {
        @Override
        public void run() {
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if(state== BLEConfigurer.ConfigurerState.WaitingForBluetooth){
                if(mBluetoothAdapter!=null && mBluetoothAdapter.isEnabled()){
                    state= BLEConfigurer.ConfigurerState.SearchingDevice;
                    Log.d("NewDeviceActivity", "System state: SearchingForDevice");
                    bluetoothLeScanner= BluetoothAdapter.
                            getDefaultAdapter().
                            getBluetoothLeScanner();
                    callbacks.deviceSearchStarted();
                    startScan();
                } else {
                    checkBluetoothHandler.postDelayed(checkBluetoothRunnable, 2000);
                }
            }
        }
    };



    public BLEConfigurer(Context context, BLEConfigurerCallbacks bleConfigurerCallbacks){
        c= context;
        callbacks= bleConfigurerCallbacks;
        state= ConfigurerState.Init;

        bleConfigurerGatt = new BLEConfigurerGatt(new BLEConfigurerGatt.ConfigurerGattListener() {
            @Override
            public void onError(BLEConfigurerGatt.ErrorCode ec) {
                if(ec== BLEConfigurerGatt.ErrorCode.DiscoverFailed || ec== BLEConfigurerGatt.ErrorCode.SyncFailed || ec==BLEConfigurerGatt.ErrorCode.ConnectFailed)
                    callbacks.onError(ErrorCode.ConnectFailed);
                else if(ec==BLEConfigurerGatt.ErrorCode.ConfigWriteFailed)
                    callbacks.onError(ErrorCode.ConfigWriteFailed);
                else if(ec==BLEConfigurerGatt.ErrorCode.UnexpectedDisconnect){
                    callbacks.onError(ErrorCode.UnexpectedDisconnect);
                }

                // Disconnect device
                // BLE Cleanup
                state= ConfigurerState.ConnectionFailed;
            }

            @Override
            public void onDeviceConnected() {

                callbacks.onDeviceConnected();
            }

            @Override
            public void onDeviceReady() {
                state= ConfigurerState.WaitingForUserInput;
                callbacks.onDeviceReady();
            }

            @Override
            public void onWiFisRefresh(String wifis) {
                callbacks.onWiFiListRefreshed(wifis);
            }

            @Override
            public void onConfigFinished() {
                state= ConfigurerState.DeviceConfigured;
                callbacks.onDeviceConfigured();
            }
        });
    }

    public Context getContext(){
        return c;
    }

    @SuppressLint("MissingPermission")
    private void startScan(){
        if (!scanning) {
            Log.d("NewDeviceActivity", "Scan start!");
            // Stops scanning after a predefined scan period.
            scanStopHandler.postDelayed(scanStopTask, ACTION_TIMEOUT_DEVICE_SEARCH);

            scanning = true;
            bluetoothLeScanner.startScan(leScanCallback);
        } else {
            Log.e("NewDeviceActivity", "Scan already running!");
        }
    }

    public void restart(){
        state=ConfigurerState.Init;
        scanStopHandler.removeCallbacks(scanStopTask);
        bleConfigurerGatt.restart();
        scanning= false;
    }

    public void finish(Context c){
        scanStopHandler.removeCallbacks(scanStopTask);
        bleConfigurerGatt.finish(c);
    }

    public void startConnectingSystem(){
        state= ConfigurerState.WaitingForBluetooth;
        Log.d("NewDeviceActivity", "System state: WaitingForBluetooth");
        checkBluetoothHandler.post(checkBluetoothRunnable);
    }

    public String getConfigWifiSSID() {
        return bleConfigurerGatt.getConfigWifiSSID();
    }

    public String getConfigWifiPSK() {
        return bleConfigurerGatt.getConfigWifiPSK();
    }

    public Device.Type getConnectedDevType() {
        return bleConfigurerGatt.getConnectedDevType();
    }

    public String getConfigTimezone() {
        return bleConfigurerGatt.getConfigTimezone();
    }

    public String getConfigMAC() {
        return bleConfigurerGatt.getConfigMAC();
    }

    public String getFoundDeviceName() {
        return bleConfigurerGatt.getFoundDeviceName();
    }

    public void writeDeviceConfig(String wifiSSID, String wifiPSK, String uid, String picklock, String timezone){
        state= ConfigurerState.WritingConfiguration;
        bleConfigurerGatt.startConfigWrite(wifiSSID, wifiPSK, uid, picklock, timezone);
    }
}
