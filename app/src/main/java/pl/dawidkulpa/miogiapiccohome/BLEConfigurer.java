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

import java.util.Timer;
import java.util.TimerTask;

import pl.dawidkulpa.miogiapiccohome.API.Device;
import pl.dawidkulpa.miogiapiccohome.activities.NewDeviceActivity;

public class BLEConfigurer {
    /** Constants */
    public static final String MG_SSID_PREFIX = "MioGiapicco";

    public static final int ACTION_TIMEOUT_GATT_CONNECTION              = 15000;
    public static final int ACTION_TIMEOUT_REGISTER_DEVICE              = 10000;
    public static final int ACTION_TIMEOUT_WRITE_CHARACTERISTICS        = 15000;
    public static final int ACTION_TIMEOUT_DEVICE_SEARCH                = 120000;

    /** State machines */
    public enum ConfigurerState {CheckingPermissions, WaitingForPermissionsUserResponse,
        WaitingForBluetooth, SearchingDevice, ConnectingWithDevice, ReadingCharacteristics, EnablingWiFiScanNotifications,
        EnablingSetFlagNotifications,
        WaitingForUserInput, RegisteringDevice, WritingCharacteristics,
        NotifyingCharacteristicsReady, DeviceConfigured, ConnectionFailed, APICommunicationFailed}

    public interface BLEConfigurerCallbacks {
        void deviceSearchStarted();
        void onTimeout(ConfigurerState state);
        void onDeviceBond(boolean success);
        void onDeviceConfigured(boolean success);
        void onWiFiListRefreshed(String wifis);
    }

    private ConfigurerState state;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothLeService bluetoothService;
    private String bleAddress;
    private boolean scanning;
    private final Context c;
    private final BLEConfigurerCallbacks callbacks;
    // Action timeout method handler
    Timer timeoutWatchdogTimer;

    // Device scan callback.
    private final ScanCallback leScanCallback =
            new ScanCallback() {

                @SuppressLint("MissingPermission")
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);

                    if(state== BLEConfigurer.ConfigurerState.SearchingDevice) {
                        Log.d("Scan new device", "Name: "+result.getDevice().getName());
                        if (result.getDevice().getName() != null &&
                                result.getDevice().getName().contains(MG_SSID_PREFIX)) {
                            scanning = false;
                            scanStopHandler.removeCallbacksAndMessages(null);
                            bluetoothLeScanner.stopScan(leScanCallback);
                            state= ConfigurerState.ConnectingWithDevice;
                            Log.d("NewDeviceActivity", "System state: ConnectingWithDevice");
                            gattUpdateReceiver.setBleName(result.getDevice().getName());
                            connect(result.getDevice().getAddress());
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
            bluetoothService = ((BluetoothLeService.LocalBinder) service).getService();
            if (bluetoothService != null) {
                if (bluetoothService.initialize()) {
                    if(bleAddress!=null && !bleAddress.isEmpty())
                        bluetoothService.connect(bleAddress);
                    else
                        Log.e("ASD", "Address empty");
                } else {
                    Log.e("ASD", "BLEService init failed");
                }
            } else {
                Log.e("ASD", "BLEService null");
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
                }
            }
        }
    };

    // GATT manager callbacks
    private final BLEConfigurerGattCallbacks gattUpdateReceiver;



    public BLEConfigurer(Context context, BLEConfigurerCallbacks bleConfigurerCallbacks){
        c= context;
        callbacks= bleConfigurerCallbacks;
        gattUpdateReceiver= new BLEConfigurerGattCallbacks(this, bluetoothService, new BLEConfigurerGattCallbacks.ConfigurerGattListener() {
            @Override
            public void onConnectionFailed() {
                ConfigurerState onFailState= state;
                state= ConfigurerState.ConnectionFailed;
                callbacks.onTimeout(state);
            }

            @Override
            public void onDeviceBond() {
                state= ConfigurerState.WaitingForUserInput;
                callbacks.onDeviceBond(true);

            }

            @Override
            public void onWiFisRefresh(String wifis) {
                callbacks.onWiFiListRefreshed(wifis);
            }

            @Override
            public void onConfigFinished(boolean success) {
                callbacks.onDeviceConfigured(success);
            }
        });
    }

    public Context getContext(){
        return c;
    }

    public void connect(String address){
        ContextCompat.registerReceiver(c, gattUpdateReceiver, makeGattUpdateIntentFilter(), ContextCompat.RECEIVER_EXPORTED);

        bleAddress= address;
        Intent gattServiceIntent = new Intent(c, BluetoothLeService.class);
        c.bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        startTimeoutWatchdog(ACTION_TIMEOUT_GATT_CONNECTION);
    }

    @SuppressLint("MissingPermission")
    public void startScan(){
        if (!scanning) {
            Log.d("NewDeviceActivity", "Scan start!");
            // Stops scanning after a predefined scan period.
            scanStopHandler.postDelayed(() -> {
                scanning = false;
                bluetoothLeScanner.stopScan(leScanCallback);
                ConfigurerState onFaileState= state;
                state= ConfigurerState.ConnectionFailed;
                callbacks.onTimeout(onFaileState);
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
            c.unregisterReceiver(gattUpdateReceiver);
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
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_WRITE_COMPLETE);
        intentFilter.addAction(BluetoothLeService.ACTION_DESCR_WRITE_COMPLETE);
        intentFilter.addAction(BluetoothLeService.ACTION_CHARACTERISTIC_CHANGED);
        return intentFilter;
    }

    public void startTimeoutWatchdog(long ms){
        if(timeoutWatchdogTimer !=null){
            timeoutWatchdogTimer.cancel();
        }

        timeoutWatchdogTimer = new Timer();
        timeoutWatchdogTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                ConfigurerState onFaileState= state;
                state= ConfigurerState.ConnectionFailed;
                callbacks.onTimeout(onFaileState);
            }
        }, ms);
    }

    public void stopTimeoutWatchdog(){
        if(timeoutWatchdogTimer !=null) {
            timeoutWatchdogTimer.cancel();
            timeoutWatchdogTimer = null;
        }
    }

    public void startConnectingSystem(){
        state= ConfigurerState.WaitingForBluetooth;
        Log.d("NewDeviceActivity", "System state: WaitingForBluetooth");
        checkBluetoothHandler.post(checkBluetoothRunnable);
    }

    public ConfigurerState getState() {
        return state;
    }

    public void setState(ConfigurerState state) {
        this.state = state;
    }

    public String getConfigWifiSSID() {
        return gattUpdateReceiver.getConfigWifiSSID();
    }

    public void setConfigWifiSSID(String configWifiSSID) {
        gattUpdateReceiver.setConfigWifiSSID(configWifiSSID.trim());
    }

    public String getConfigWifiPSK() {
        return gattUpdateReceiver.getConfigWifiPSK();
    }

    public void setConfigWifiPSK(String configWifiPSK) {
        gattUpdateReceiver.setConfigWifiPSK(configWifiPSK.trim());
    }

    public String getConfigPicklock() {
        return gattUpdateReceiver.getConfigPicklock();
    }

    public void setConfigPicklock(String configPicklock) {
        gattUpdateReceiver.setConfigPicklock(configPicklock);
    }

    public Device.Type getConnectedDevType() {
        return gattUpdateReceiver.getConnectedDevType();
    }

    public String getConfigTimezone() {
        return gattUpdateReceiver.getConfigTimezone();
    }

    public void setConfigTimezone(String configTimezone) {
        gattUpdateReceiver.setConfigTimezone(configTimezone);
    }

    public String getConfigMAC() {
        return gattUpdateReceiver.getConfigMAC();
    }

    public String getFoundDeviceName() {
        return gattUpdateReceiver.getFoundDeviceName();
    }

    public void writeCharacteristics(String uid, String picklock){
        startTimeoutWatchdog(ACTION_TIMEOUT_WRITE_CHARACTERISTICS);
        gattUpdateReceiver.writeCharacteristics(uid, picklock);
    }
}
