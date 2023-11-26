package pl.dawidkulpa.miogiapiccohome.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import pl.dawidkulpa.miogiapiccohome.API.Device;
import pl.dawidkulpa.miogiapiccohome.API.Plant;
import pl.dawidkulpa.miogiapiccohome.API.Room;
import pl.dawidkulpa.miogiapiccohome.API.Sector;
import pl.dawidkulpa.miogiapiccohome.API.User;
import pl.dawidkulpa.miogiapiccohome.API.UserData;
import pl.dawidkulpa.miogiapiccohome.BluetoothLeService;
import pl.dawidkulpa.miogiapiccohome.R;

public class NewDeviceActivity extends AppCompatActivity {

    /** Constants */
    public static final String MG_SSID_PREFIX = "MioGiapicco";

    public static final int ACTION_TIMEOUT_GATT_CONNECTION              = 15000;
    public static final int ACTION_TIMEOUT_READ_CHARACTERISTICS         = 5000;
    public static final int ACTION_TIMEOUT_ENABLE_NOTIFICATIONS         = 4000;
    public static final int ACTION_TIMEOUT_REGISTER_DEVICE              = 10000;
    public static final int ACTION_TIMEOUT_WRITE_CHARACTERISTICS        = 6000;
    public static final int ACTION_TIMEOUT_DEVICE_CONFIGURED_RESPONSE   = 4000;
    public static final int ACTION_TIMEOUT_DEVICE_SEARCH                = 120000;

    public static final String SERVICE_UUID                  = "952cb13b-57fa-4885-a445-57d1f17328fd";
    public static final String CHARACTERISTIC_UUID_WIFI_SSID = "345ac506-c96e-45c6-a418-56a2ef2d6072";
    public static final String CHARACTERISTIC_UUID_WIFI_PSK  = "b675ddff-679e-458d-9960-939d8bb03572";
    public static final String CHARACTERISTIC_UUID_UID       = "566f9eb0-a95e-4c18-bc45-79bd396389af";
    public static final String CHARACTERISTIC_UUID_PICKLOCK  = "f6ffba4e-eea1-4728-8b1a-7789f9a22da8";
    public static final String CHARACTERISTIC_UUID_MAC       = "c0cd497d-6987-41fa-9b6d-ef2e2a94e04a";
    public static final String CHARACTERISTIC_UUID_SET_FLAG  = "e34fc92f-7565-403b-9528-35b4650596fc";
    public static final String CHARACTERISTIC_UUID_TIMEZONE  = "e00758dd-7c07-42fd-8699-423b73fcb4ce";

    /** State machines */
    public enum SystemState {CheckingPermissions, WaitingForPermissionsUserResponse,
        WaitingForBluetooth, SearchingDevice, ConnectingWithDevice, ReadingCharacteristics, EnablingNotifications,
        WaitingForUserInput, RegisteringDevice, WritingCharacteristics,
        NotifyingCharacteristicsReady, DeviceConfigured, ConnectionFailed, APICommunicationFailed}

    public enum UIState {PrepareBluetooth, SearchForDevice, SetupWiFi, DeviceConfigured, Failed}

    private SystemState systemState;
    private UIState uiState;
    boolean scanning = false;

    /** Private variables */
    // Config holders
    private String configWifiSSID;
    private String configWifiPSK;
    private String configPicklock;
    private int configRoomIdx;
    private int configSectorIdx;
    private int configPlantIdx;
    private String configUID;
    private String configMAC;
    private String configTimezone;

    // Config written flags
    private boolean configWifiSSIDWritten= false;
    private boolean configWifiPSKWritten= false;
    private boolean configPicklockWritten= false;
    private boolean configUIDWritten= false;
    private boolean configTimezoneWritten= false;

    private User user;
    private enum UserDataReceiveState {WaitingForResponse, Success, Failed}
    private UserDataReceiveState userDataReceiveState;
    private final ArrayList<String> timezones= new ArrayList<>();
    private String[] timezoneCodes;

    private Device.Type connectedDevType= Device.Type.Unknown;

    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothLeService bluetoothService;
    private String bleAddress;
    private String bleName;

    // Bluetooth characteristics (null before discovered)
    private BluetoothGattCharacteristic wifiSSIDCharacteristic=null;
    private BluetoothGattCharacteristic wifiPSKCharacteristic=null;
    private BluetoothGattCharacteristic uidCharacteristic=null;
    private BluetoothGattCharacteristic picklockCharacteristic=null;
    private BluetoothGattCharacteristic macCharacteristic=null;
    private BluetoothGattCharacteristic timezoneCharacteristic=null;
    private BluetoothGattCharacteristic setFlagCharacteristic=null;

    /** UI Elements */
    TextView step1Label;
    TextView step2Label;
    TextView step3Label;

    TextView foundDeviceName;

    EditText wifiSSIDEdit;
    EditText wifiPskEdit;
    Spinner roomsSpinner;
    Spinner sectorsSpinner;
    Spinner plantSpinner;
    Spinner timezonesSpinner;

    ProgressBar progressBar;
    Button nextButton;


    /** Callbacks */
    // Device scan callback.
    private final ScanCallback leScanCallback =
            new ScanCallback() {

                @SuppressLint("MissingPermission")
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);

                    if(systemState==SystemState.SearchingDevice) {
                        if (result.getDevice().getName() != null &&
                                result.getDevice().getName().contains(MG_SSID_PREFIX)) {
                            scanning = false;
                            scanStopHandler.removeCallbacksAndMessages(null);
                            bluetoothLeScanner.stopScan(leScanCallback);
                            systemState=SystemState.ConnectingWithDevice;
                            Log.d("NewDeviceActivity", "System state: ConnectingWithDevice");
                            bleName= result.getDevice().getName();
                            connect(result.getDevice().getAddress());
                        }
                    }
                }
            };

    // Stop bluetooth method handler
    Handler scanStopHandler= new Handler();

    // Action timeout method handler
    Timer timeoutWatchdogTimer;

    // GATT manager callbacks
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(BluetoothLeService.ACTION_GATT_DISCONNECTED)){
                onConnectionFailed();
                return;
            }

            if(systemState==SystemState.ConnectingWithDevice){
                if(action.equals(BluetoothLeService.ACTION_GATT_CONNECTED)){
                    configWifiSSID = null;
                    configWifiPSK = null;
                    configUID= null;
                    configPicklock= null;
                    configMAC= null;
                    configTimezone= null;
                    Log.d("NewDeviceActivity", "System state: WaitingForServices");
                } else if(action.equals(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)){
                    stopTimeoutWatchdog();
                    if(loadCharacteristics()){
                        systemState=SystemState.ReadingCharacteristics;
                        Log.d("NewDeviceActivity", "System state: ReadingCharacteristics");
                        readAllConfigCharacteristic();
                        startTimeoutWatchdog(ACTION_TIMEOUT_READ_CHARACTERISTICS);
                    } else {
                        systemState= SystemState.ConnectionFailed;
                        onConnectionFailed();
                        Log.d("NewDeviceActivity", "System state: ConnectionFailed (Characteristics not found)");
                    }
                }
            } else if(systemState==SystemState.ReadingCharacteristics){
                if(action.equals(BluetoothLeService.ACTION_DATA_AVAILABLE)){
                    String uuid= intent.getStringExtra("uuid");
                    String data= intent.getStringExtra("data");
                    if(data==null)
                        data="";

                    switch (uuid) {
                        case CHARACTERISTIC_UUID_PICKLOCK:
                            Log.e("NewDeviceActivity", "Picklock characteristic data received!");
                            configPicklock = data;
                            break;
                        case CHARACTERISTIC_UUID_WIFI_SSID:
                            Log.e("NewDeviceActivity", "WiFi SSID characteristic data received!");
                            configWifiSSID = data;
                            break;
                        case CHARACTERISTIC_UUID_WIFI_PSK:
                            Log.e("NewDeviceActivity", "WiFi PSK characteristic data received!");
                            configWifiPSK = data;
                            break;
                        case CHARACTERISTIC_UUID_UID:
                            Log.e("NewDeviceActivity", "UID characteristic data received!");
                            configUID = data;
                            break;
                        case CHARACTERISTIC_UUID_MAC:
                            Log.e("NewDeviceActivity", "MAC characteristic data received!");
                            configMAC = data;
                            break;
                        case CHARACTERISTIC_UUID_TIMEZONE:
                            Log.e("NewDeviceActivity", "Timezone characteristic data received!");
                            configTimezone = data;
                            break;
                    }

                    if( configWifiSSID !=null && configWifiPSK !=null &&
                            configUID!=null && configPicklock!=null &&
                            configMAC!=null && configTimezone!=null){
                        stopTimeoutWatchdog();
                        systemState= SystemState.EnablingNotifications;
                        bluetoothService.allowNotificationsFor(setFlagCharacteristic);
                        startTimeoutWatchdog(ACTION_TIMEOUT_ENABLE_NOTIFICATIONS);
                        Log.d("NewDeviceActivity", "System state: EnablingNotifications");
                    }
                }
            } else if(systemState==SystemState.EnablingNotifications){
                if(action.equals(BluetoothLeService.ACTION_DESCR_WRITE_COMPLETE)) {
                    stopTimeoutWatchdog();
                    if(userDataReceiveState!= UserDataReceiveState.Success){
                        systemState= SystemState.APICommunicationFailed;
                        finishBLE();
                        prepareNextStep(UIState.Failed);
                    } else {
                        systemState = SystemState.WaitingForUserInput;
                        Log.d("NewDeviceActivity", "System state: WaitingForUserInput");

                        if(bleName.contains("Light"))
                            connectedDevType= Device.Type.Light;
                        else if(bleName.contains("Soil")){
                            connectedDevType= Device.Type.Soil;
                        } else if (bleName.contains("Air"))
                            connectedDevType= Device.Type.Air;

                        prepareRoomsListAdapter();
                        prepareNextStep(UIState.SetupWiFi);
                    }
                }
            } else if(systemState==SystemState.WritingCharacteristics){
                if(action.equals(BluetoothLeService.ACTION_DATA_WRITE_COMPLETE)){
                    String uuid= intent.getStringExtra("uuid");

                    switch (uuid) {
                        case CHARACTERISTIC_UUID_PICKLOCK:
                            configPicklockWritten= true;
                            Log.e("NewDeviceActivity", "Picklock characteristic written!");
                            break;
                        case CHARACTERISTIC_UUID_WIFI_SSID:
                            configWifiSSIDWritten= true;
                            Log.e("NewDeviceActivity", "WiFi SSID characteristic written!");
                            break;
                        case CHARACTERISTIC_UUID_WIFI_PSK:
                            configWifiPSKWritten = true;
                            Log.e("NewDeviceActivity", "WiFi PSK characteristic written!");
                            break;
                        case CHARACTERISTIC_UUID_UID:
                            configUIDWritten = true;
                            Log.e("NewDeviceActivity", "UID characteristic written!");
                            break;
                        case CHARACTERISTIC_UUID_TIMEZONE:
                            configTimezoneWritten= true;
                            Log.e("NewDeviceActivity", "Timezone characteristic written!");

                    }

                    if( configWifiSSIDWritten && configWifiPSKWritten &&
                    configUIDWritten && configPicklockWritten && configTimezoneWritten){
                        stopTimeoutWatchdog();
                        systemState= SystemState.NotifyingCharacteristicsReady;
                        Log.d("NewDeviceActivity", "System state: NotifyingCharacteristicsReady");
                        setFlagCharacteristic.setValue(new byte[]{0x01});
                        bluetoothService.writeCharacteristic(setFlagCharacteristic);
                        startTimeoutWatchdog(ACTION_TIMEOUT_DEVICE_CONFIGURED_RESPONSE);
                    }
                }
            } else if(systemState==SystemState.NotifyingCharacteristicsReady){
                if(action.equals(BluetoothLeService.ACTION_CHARACTERISTIC_CHANGED)){
                    String uuid= intent.getStringExtra("uuid");
                    String data= intent.getStringExtra("data");

                    if(uuid.equals(CHARACTERISTIC_UUID_SET_FLAG) && data.equals("0")) {
                        stopTimeoutWatchdog();
                        systemState = SystemState.DeviceConfigured;

                        Log.d("NewDeviceActivity", "System state: DeviceConfigured");
                        prepareNextStep(UIState.DeviceConfigured);
                    }
                }
            }
        }
    };

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
            if(systemState==SystemState.WaitingForBluetooth){
                if(mBluetoothAdapter!=null && mBluetoothAdapter.isEnabled()){
                    systemState= SystemState.SearchingDevice;
                    Log.d("NewDeviceActivity", "System state: SearchingForDevice");
                    bluetoothLeScanner= BluetoothAdapter.
                            getDefaultAdapter().
                            getBluetoothLeScanner();
                    prepareNextStep(UIState.SearchForDevice);
                    startScan();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_device);

        user= getIntent().getParcelableExtra("UserAPI");

        step1Label = findViewById(R.id.step1_label);
        step2Label= findViewById(R.id.step2_label);
        step3Label= findViewById(R.id.step3_label);

        foundDeviceName= findViewById(R.id.found_name);

        wifiSSIDEdit= findViewById(R.id.wifi_ssid_edit);
        wifiPskEdit= findViewById(R.id.wifi_psk_edit);
        nextButton= findViewById(R.id.next_step_button);
        progressBar= findViewById(R.id.progressbar);
        roomsSpinner= findViewById(R.id.rooms_list_spinner);
        sectorsSpinner= findViewById(R.id.sectors_list_spinner);
        plantSpinner= findViewById(R.id.plants_list_spinner);
        timezonesSpinner= findViewById(R.id.timezone_list_spinner);

        String[] tzs= getResources().getStringArray(R.array.timezones_names);
        timezoneCodes= getResources().getStringArray(R.array.timezones_codes);
        timezones.addAll(Arrays.asList(tzs));
        prepareTimezoneListAdapter();

        prepareNextStep(UIState.PrepareBluetooth);

        userDataReceiveState= UserDataReceiveState.WaitingForResponse;
        user.downloadData(this::onDownloadUserDataResult);

        systemState=SystemState.CheckingPermissions;
        Log.d("NewDeviceActivity", "System state: CheckingPermissions");
        checkAndRequestPermissions();
    }

    private void onDownloadUserDataResult(boolean success, UserData userData){
        if(success) {
            userDataReceiveState = UserDataReceiveState.Success;
        } else {
            userDataReceiveState = UserDataReceiveState.Failed;
        }
    }

    private void prepareRoomsListAdapter(){
        ArrayList<String> roomNames= new ArrayList<>();

        ArrayList<Room> userRooms= user.getDataHandler().getRooms();
        for(Room r: userRooms){
            roomNames.add(r.getName());
        }
        roomNames.add(getString(R.string.label_add_new_plant));

        ArrayAdapter<String> roomsListAdapter= new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, roomNames);
        roomsListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        roomsSpinner.setAdapter(roomsListAdapter);
        roomsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                onRoomItemSelected(pos);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                configPlantIdx= -1;
            }
        });
    }

    private void onRoomItemSelected(int pos){
        if(pos < user.getDataHandler().getRooms().size()) {
            configRoomIdx= pos;
            prepareSectorsListAdapter(pos);
        }
    }

    private void prepareSectorsListAdapter(int roomId){
        ArrayList<String> sectorNames= new ArrayList<>();
        ArrayList<Sector> userSectors= user.getDataHandler().getSectors(roomId);
        for(Sector s: userSectors){
            sectorNames.add(s.getName());
        }
        sectorNames.add(getString(R.string.label_add_new_plant));

        ArrayAdapter<String> sectorsListAdapter= new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, sectorNames);
        sectorsListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        sectorsSpinner.setAdapter(sectorsListAdapter);
        sectorsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                onSectorItemSelected(pos);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                configPlantIdx= -1;
            }
        });
    }

    private void onSectorItemSelected(int pos){
        if(pos < user.getDataHandler().getRooms().get(configRoomIdx).getSectors().size()){
            configSectorIdx= pos;
            if(connectedDevType== Device.Type.Soil)
                preparePlantsListAdapter(configRoomIdx, configSectorIdx);
        }
    }

    public void preparePlantsListAdapter(int roomId, int sectorId){
        ArrayList<String> plantNames= new ArrayList<>();
        ArrayList<Plant> userPlants= user.getDataHandler().getPlants(roomId, sectorId);
        for(Plant p: userPlants){
            plantNames.add(p.getName());
        }
        plantNames.add(getString(R.string.label_add_new_plant));

        ArrayAdapter<String> plantsListAdapter= new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, plantNames);
        plantsListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        plantSpinner.setAdapter(plantsListAdapter);
        plantSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                onPlantsItemSelected(pos);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                configPlantIdx= -1;
            }
        });
    }

    private void onPlantsItemSelected(int pos){
        if(pos < user.getDataHandler().getRooms().get(configRoomIdx).getSectors()
                .get(configSectorIdx).getPlants().size()){
            configPlantIdx= pos;
        }
    }

    private void prepareTimezoneListAdapter(){
        ArrayAdapter<String> timezoneListAdapter= new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, timezones);
        timezoneListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        timezonesSpinner.setAdapter(timezoneListAdapter);
        timezonesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                configTimezone= timezoneCodes[pos];
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                configTimezone= "";
            }
        });
    }

    public void startTimeoutWatchdog(long ms){
        if(timeoutWatchdogTimer !=null){
            timeoutWatchdogTimer.cancel();
        }

        timeoutWatchdogTimer = new Timer();
        timeoutWatchdogTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                onActionTimeout();
            }
        }, ms);
    }

    public void stopTimeoutWatchdog(){
        if(timeoutWatchdogTimer !=null) {
            timeoutWatchdogTimer.cancel();
            timeoutWatchdogTimer = null;
        }
    }

    public void onActionTimeout(){
        SystemState onFailState= systemState;
        onConnectionFailed();
        Log.e("NewDeviceActivity", "Timeout at "+onFailState.toString());
    }

    public void onConnectionFailed(){
        systemState= SystemState.ConnectionFailed;
        finishBLE();
        prepareNextStep(UIState.Failed);
    }

    public void finishBLE(){
        if(bluetoothService!=null)
            bluetoothService.close();
        try {
            unregisterReceiver(gattUpdateReceiver);
        } catch (IllegalArgumentException e){
            Log.w("NewDeviceActivity", "Gatt update receiver not registered");
        }

        try{
            unbindService(serviceConnection);
        } catch (IllegalArgumentException e){
            Log.w("NewDeviceActivity", "BLE Service not bound");
        }
    }

    public void checkAndRequestPermissions(){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            boolean bluetoothConnectPermited=
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
            boolean bluetoothScanPermited=
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
            boolean fineLocationPermited=
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

            if(!bluetoothConnectPermited || !bluetoothScanPermited || !fineLocationPermited){
                systemState= SystemState.WaitingForPermissionsUserResponse;
                Log.d("NewDeviceActivity", "System state: WaitingForPermissionsUserResponse");
                requestPermissions(
                        new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION},
                        1);
            } else {
                startConnectingSystem();
            }
        } else {
            startConnectingSystem();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            int granted = 0;

            for (int g : grantResults) {
                if (g == PackageManager.PERMISSION_GRANTED)
                    granted++;
            }

            if (granted == grantResults.length)
                startConnectingSystem();
        }
    }

    public void startConnectingSystem(){
        systemState= SystemState.WaitingForBluetooth;
        Log.d("NewDeviceActivity", "System state: WaitingForBluetooth");
        checkBluetoothHandler.post(checkBluetoothRunnable);
    }

    @SuppressLint("MissingPermission")
    public void startScan(){
        if (!scanning) {
            Log.d("NewDeviceActivity", "Scan start!");
            // Stops scanning after a predefined scan period.
            scanStopHandler.postDelayed(() -> {
                scanning = false;
                bluetoothLeScanner.stopScan(leScanCallback);
                onActionTimeout();
            }, ACTION_TIMEOUT_DEVICE_SEARCH);

            scanning = true;
            bluetoothLeScanner.startScan(leScanCallback);
        } else {
            Log.e("NewDeviceActivity", "Scan already running!");
        }
    }

    public void connect(String address){
        ContextCompat.registerReceiver(this, gattUpdateReceiver, makeGattUpdateIntentFilter(), ContextCompat.RECEIVER_EXPORTED);

        bleAddress= address;
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        startTimeoutWatchdog(ACTION_TIMEOUT_GATT_CONNECTION);
    }

    private boolean loadCharacteristics(){
        wifiSSIDCharacteristic= null;
        wifiPSKCharacteristic= null;
        uidCharacteristic= null;
        picklockCharacteristic= null;
        macCharacteristic= null;
        setFlagCharacteristic= null;
        timezoneCharacteristic= null;

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
                        case CHARACTERISTIC_UUID_WIFI_SSID:
                            wifiSSIDCharacteristic= gattCharacteristic;
                            break;
                        case CHARACTERISTIC_UUID_WIFI_PSK:
                            wifiPSKCharacteristic= gattCharacteristic;
                            break;
                        case CHARACTERISTIC_UUID_UID:
                            uidCharacteristic= gattCharacteristic;
                            break;
                        case CHARACTERISTIC_UUID_PICKLOCK:
                            picklockCharacteristic= gattCharacteristic;
                            break;
                        case CHARACTERISTIC_UUID_MAC:
                            macCharacteristic= gattCharacteristic;
                            break;
                        case CHARACTERISTIC_UUID_SET_FLAG:
                            setFlagCharacteristic= gattCharacteristic;
                            break;
                        case CHARACTERISTIC_UUID_TIMEZONE:
                            timezoneCharacteristic= gattCharacteristic;
                    }
                }

                break;
            }
        }

        return (picklockCharacteristic!=null && macCharacteristic!=null &&
                uidCharacteristic!=null && wifiSSIDCharacteristic!=null &&
                wifiPSKCharacteristic!=null && setFlagCharacteristic!=null &&
                timezoneCharacteristic!=null);
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

    public void onNextClick(View v){
        Log.e("UIState", uiState.name());
        switch (uiState){
            case SetupWiFi:
                onWriteConfigClick();
                break;
            case DeviceConfigured:
                onFinishClick();
                break;
        }
    }

    private void readAllConfigCharacteristic(){
        bluetoothService.readCharacteristic(wifiSSIDCharacteristic);
        bluetoothService.readCharacteristic(wifiPSKCharacteristic);
        bluetoothService.readCharacteristic(uidCharacteristic);
        bluetoothService.readCharacteristic(macCharacteristic);
        bluetoothService.readCharacteristic(picklockCharacteristic);
        bluetoothService.readCharacteristic(timezoneCharacteristic);
    }

    private void onWriteConfigClick(){
        configWifiSSID = wifiSSIDEdit.getText().toString();
        configWifiPSK = wifiPskEdit.getText().toString();

        if(configWifiPSK.isEmpty() || configWifiSSID.isEmpty() || configRoomIdx==-1
                || (connectedDevType== Device.Type.Light && configSectorIdx==-1)
                || (connectedDevType== Device.Type.Soil && configPlantIdx==-1)
                || configTimezone.isEmpty()){
            Snackbar.make(wifiSSIDEdit, "Set your WiFi SSID, PSK and name your device", BaseTransientBottomBar.LENGTH_SHORT).show();
        } else {
            systemState=SystemState.RegisteringDevice;
            Log.d("NewDeviceActivity", "System state: RegisteringDevice");
            progressBar.setVisibility(View.VISIBLE);
            startTimeoutWatchdog(ACTION_TIMEOUT_REGISTER_DEVICE);

            int roomId= user.getDataHandler().getRooms().get(configRoomIdx).getId();;
            int sectorId= -1;
            int plantId= -1;

            if(configSectorIdx!=-1){
                sectorId= user.getDataHandler().getRooms().get(configRoomIdx).getSectors()
                        .get(configSectorIdx).getId();
            }

            if(configPlantIdx!=-1){
                plantId= user.getDataHandler().getRooms().get(configRoomIdx).getSectors()
                        .get(configSectorIdx).getPlants().get(configPlantIdx).getId();
            }

            user.registerDevice(configMAC, roomId, sectorId, plantId, connectedDevType,
                    this::onDeviceRegisterResult);
        }
    }

    private void writeCharacteristics(){
        wifiSSIDCharacteristic.setValue(configWifiSSID);
        wifiPSKCharacteristic.setValue(configWifiPSK);
        uidCharacteristic.setValue(String.valueOf(user.getUid()));
        picklockCharacteristic.setValue(user.getPicklock());
        timezoneCharacteristic.setValue(configTimezone);

        bluetoothService.writeCharacteristic(timezoneCharacteristic);
        bluetoothService.writeCharacteristic(wifiSSIDCharacteristic);
        bluetoothService.writeCharacteristic(wifiPSKCharacteristic);
        bluetoothService.writeCharacteristic(uidCharacteristic);
        bluetoothService.writeCharacteristic(picklockCharacteristic);
    }

    private void onDeviceRegisterResult(boolean success){
        if(success){
            stopTimeoutWatchdog();
            systemState=SystemState.WritingCharacteristics;
            Log.d("NewDeviceActivity", "System state: WritingCharacteristics");
            Log.d("NewDeviceActivity", "Populate device in database success");
            writeCharacteristics();
            startTimeoutWatchdog(ACTION_TIMEOUT_WRITE_CHARACTERISTICS);
        } else {
            stopTimeoutWatchdog();
            systemState=SystemState.ConnectionFailed;
            Log.e("NewDeviceActivity", "System state: ConnectionFailed");
            Log.e("NewDeviceActivity", "Populate device in database failed");
            onConnectionFailed();
        }
    }

    private void onFinishClick(){
        finishBLE();
        Intent intent= new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.putExtra("UserAPI", user);
        startActivity(intent);
    }

    private void prepareNextStep(@NonNull UIState next){
        switch (next){
            case PrepareBluetooth:
                step1Label.setVisibility(View.VISIBLE);
                step1Label.setTextColor(getColor(R.color.textPrimary));
                step1Label.setTypeface(null, Typeface.BOLD);
                step2Label.setVisibility(View.GONE);
                step3Label.setVisibility(View.GONE);

                foundDeviceName.setVisibility(View.GONE);

                ((View)wifiSSIDEdit.getParent()).setVisibility(View.GONE);
                ((View)wifiPskEdit.getParent()).setVisibility(View.GONE);
                plantSpinner.setVisibility(View.GONE);

                progressBar.setVisibility(View.GONE);

                foundDeviceName.setText("");
                nextButton.setVisibility(View.GONE);
                break;
            case SearchForDevice:
                step1Label.setVisibility(View.VISIBLE);
                step1Label.setTextColor(getColor(R.color.textDisabled));
                step1Label.setTypeface(null, Typeface.NORMAL);
                step2Label.setVisibility(View.VISIBLE);
                step2Label.setTextColor(getColor(R.color.textPrimary));
                step2Label.setTypeface(null, Typeface.BOLD);
                step3Label.setVisibility(View.GONE);

                foundDeviceName.setVisibility(View.VISIBLE);

                ((View)wifiSSIDEdit.getParent()).setVisibility(View.GONE);
                ((View)wifiPskEdit.getParent()).setVisibility(View.GONE);
                plantSpinner.setVisibility(View.GONE);

                progressBar.setVisibility(View.VISIBLE);

                foundDeviceName.setText(R.string.label_addnewdevice_searching);
                nextButton.setVisibility(View.GONE);
                break;
            case SetupWiFi:
                step1Label.setVisibility(View.VISIBLE);
                step1Label.setTextColor(getColor(R.color.textDisabled));
                step1Label.setTypeface(null, Typeface.NORMAL);
                step2Label.setVisibility(View.VISIBLE);
                step2Label.setTextColor(getColor(R.color.textDisabled));
                step2Label.setTypeface(null, Typeface.NORMAL);
                step3Label.setVisibility(View.VISIBLE);
                step3Label.setTextColor(getColor(R.color.textPrimary));
                step3Label.setTypeface(null, Typeface.BOLD);

                foundDeviceName.setVisibility(View.GONE);

                ((View)wifiSSIDEdit.getParent()).setVisibility(View.VISIBLE);
                ((View)wifiPskEdit.getParent()).setVisibility(View.VISIBLE);
                plantSpinner.setVisibility(View.VISIBLE);

                progressBar.setVisibility(View.GONE);

                nextButton.setVisibility(View.VISIBLE);
                nextButton.setText(R.string.button_config_device);
                break;
            case DeviceConfigured:
                step1Label.setVisibility(View.VISIBLE);
                step1Label.setTextColor(getColor(R.color.textDisabled));
                step1Label.setTypeface(null, Typeface.NORMAL);
                step2Label.setVisibility(View.VISIBLE);
                step2Label.setTextColor(getColor(R.color.textDisabled));
                step2Label.setTypeface(null, Typeface.NORMAL);
                step3Label.setVisibility(View.VISIBLE);
                step3Label.setTextColor(getColor(R.color.textDisabled));
                step3Label.setTypeface(null, Typeface.NORMAL);

                foundDeviceName.setVisibility(View.GONE);

                ((View)wifiSSIDEdit.getParent()).setVisibility(View.GONE);
                ((View)wifiPskEdit.getParent()).setVisibility(View.GONE);
                plantSpinner.setVisibility(View.GONE);

                progressBar.setVisibility(View.GONE);

                nextButton.setVisibility(View.VISIBLE);
                nextButton.setText(R.string.button_finish);
                break;
            case Failed:
                break;
        }
        uiState = next;
    }
}