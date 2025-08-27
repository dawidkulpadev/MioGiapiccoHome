package pl.dawidkulpa.miogiapiccohome;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.content.ContextCompat;

import pl.dawidkulpa.miogiapiccohome.API.Device;

public class BLEConfigurer {
    /** Constants */
    public static final String MG_SSID_PREFIX = "MioGiapicco";

    public static final int ACTION_TIMEOUT_GATT_CONNECTION              = 3000;
    public static final int ACTION_TIMEOUT_REGISTER_DEVICE              = 20000;
    public static final int ACTION_TIMEOUT_DEVICE_SEARCH                = 60000;

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
    private BLEGattService bluetoothService;
    private String bleAddress;
    private boolean scanning;
    private final Context c;
    private final BLEConfigurerCallbacks callbacks;
    // Action timeout method handler
    TimeoutWatchdog timeoutWatchdog= new TimeoutWatchdog();

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
                            bleConfigurerGatt.setBleName(result.getDevice().getName());
                            bindGattService(result.getDevice().getAddress());
                        }
                    }
                }
            };

    // Stop bluetooth method handler
    Handler scanStopHandler= new Handler();

    // Service connecting callback
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e("ServiceConnection", "service connected");
            bluetoothService = ((BLEGattService.LocalBinder) service).getService();
            if (bluetoothService != null) {
                if (bluetoothService.initialize()) {
                    if(bleAddress!=null && !bleAddress.isEmpty()) {
                        bleConfigurerGatt.startConnectAndReceiving(bluetoothService, bleAddress);
                    } else {
                        Log.e("ServiceConnection", "Address empty");
                        callbacks.onError(ErrorCode.ConnectFailed);
                    }
                } else {
                    Log.e("ServiceConnection", "BLEService init failed");
                    callbacks.onError(ErrorCode.ConnectFailed);
                }
            } else {
                Log.e("ServiceConnection", "BLEService null");
                callbacks.onError(ErrorCode.ConnectFailed);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e("ServiceConnection", "service disconnected");
            bluetoothService = null;
        }
    };

    // Check bluetooth enabled callbacks
    Handler checkBluetoothHandler= new Handler();
    Runnable checkBluetoothRunnable= new Runnable() {
        @Override
        public void run() {
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if(state==BLEConfigurer.ConfigurerState.WaitingForBluetooth){
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

        bleConfigurerGatt = new BLEConfigurerGatt(this, new BLEConfigurerGatt.ConfigurerGattListener() {
            @Override
            public void onError(BLEConfigurerGatt.ErrorCode ec) {
                if(ec== BLEConfigurerGatt.ErrorCode.DiscoverFailed || ec== BLEConfigurerGatt.ErrorCode.SyncFailed)
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
                timeoutWatchdog.stop();
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

    public void bindGattService(String address){
        ContextCompat.registerReceiver(c, bleConfigurerGatt, makeGattUpdateIntentFilter(), ContextCompat.RECEIVER_EXPORTED);
        bleAddress= address;
        Intent gattServiceIntent = new Intent(c, BLEGattService.class);

        // Call actual connect when gattService bind finished -> serviceConnection.onServiceConnected callback
        c.bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        timeoutWatchdog.start(ACTION_TIMEOUT_GATT_CONNECTION, ()->{
            onTimeout(ErrorCode.ConnectFailed);
        });
    }

    @SuppressLint("MissingPermission")
    private void startScan(){
        if (!scanning) {
            Log.d("NewDeviceActivity", "Scan start!");
            // Stops scanning after a predefined scan period.
            scanStopHandler.postDelayed(() -> {
                scanning = false;
                bluetoothLeScanner.stopScan(leScanCallback);
                state= ConfigurerState.ConnectionFailed;
                callbacks.onError(ErrorCode.NoDeviceFound);
            }, ACTION_TIMEOUT_DEVICE_SEARCH);

            scanning = true;
            bluetoothLeScanner.startScan(leScanCallback);
        } else {
            Log.e("NewDeviceActivity", "Scan already running!");
        }
    }

    public void finishBLE(){
        if(bluetoothService!=null)
            bluetoothService.close();
        try {
            c.unregisterReceiver(bleConfigurerGatt);
        } catch (IllegalArgumentException e){
            Log.w("NewDeviceActivity", "Gatt update receiver not registered");
        }

        try{
            c.unbindService(serviceConnection);
        } catch (IllegalArgumentException e){
            Log.w("NewDeviceActivity", "BLE Service not bound");
        }
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

    public void onTimeout(ErrorCode ec){
        state= ConfigurerState.ConnectionFailed;
        callbacks.onError(ec);
    }
}
