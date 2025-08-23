package pl.dawidkulpa.miogiapiccohome.activities;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
import android.icu.util.TimeZone;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import pl.dawidkulpa.miogiapiccohome.API.Device;
import pl.dawidkulpa.miogiapiccohome.API.Plant;
import pl.dawidkulpa.miogiapiccohome.API.Room;
import pl.dawidkulpa.miogiapiccohome.API.Sector;
import pl.dawidkulpa.miogiapiccohome.API.User;
import pl.dawidkulpa.miogiapiccohome.API.UserData;
import pl.dawidkulpa.miogiapiccohome.BLEConfigurer;
import pl.dawidkulpa.miogiapiccohome.BLEConfigurerGattCallbacks;
import pl.dawidkulpa.miogiapiccohome.BluetoothLeService;
import pl.dawidkulpa.miogiapiccohome.R;

public class NewDeviceActivity extends AppCompatActivity {
    public enum UIState {PrepareBluetooth, SearchForDevice, SetupWiFi, DeviceConfigured, Failed}

    private UIState uiState;
    boolean scanning = false;

    /** Private variables */
    // Config holders

    private int configRoomIdx=-1;
    private int configSectorIdx=-1;
    private int configPlantIdx=-1;

    private User user;
    private enum UserDataReceiveState {WaitingForResponse, Success, Failed}
    private UserDataReceiveState userDataReceiveState;
    private final ArrayList<String> timezones= new ArrayList<>();
    private String[] timezoneCodes;



    /** UI Elements */
    TextView step1Label;
    TextView step2Label;
    TextView step3Label;

    EditText wifiPskEdit;
    EditText devicesNameEdit;
    TextInputLayout roomsMenu;
    TextInputLayout sectorsMenu;
    TextInputLayout plantsMenu;
    TextInputLayout timezonesMenu;
    TextInputLayout wifiSSIDsMenu;
    MaterialAutoCompleteTextView roomsAutoComplete;
    MaterialAutoCompleteTextView sectorsAutoComplete;
    MaterialAutoCompleteTextView plantsAutoComplete;
    MaterialAutoCompleteTextView timezonesAutoComplete;
    MaterialAutoCompleteTextView wifiSSIDsAutoComplete;

    ProgressBar progressBar;
    Button nextButton;


    BLEConfigurer bleConfigurer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_device);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                exitActivity();
            }
        });

        toolbar.setNavigationOnClickListener(v -> exitActivity());

        user= getIntent().getParcelableExtra("UserAPI");

        step1Label = findViewById(R.id.step1_label);
        step2Label= findViewById(R.id.step2_label);
        step3Label= findViewById(R.id.step3_label);

        wifiPskEdit= findViewById(R.id.wifi_psk_edit);
        devicesNameEdit= findViewById(R.id.device_name_edit);
        nextButton= findViewById(R.id.next_step_button);
        progressBar= findViewById(R.id.progressbar);
        roomsMenu= findViewById(R.id.room_select_menu);
        roomsAutoComplete= findViewById(R.id.room_select_autocomplete);
        sectorsMenu= findViewById(R.id.sector_select_menu);
        sectorsAutoComplete= findViewById(R.id.sector_select_autocomplete);
        plantsMenu= findViewById(R.id.plant_select_menu);
        plantsAutoComplete= findViewById(R.id.plant_select_autocomplete);
        timezonesMenu= findViewById(R.id.timezone_select_menu);
        timezonesAutoComplete= findViewById(R.id.timezone_select_autocomplete);
        wifiSSIDsMenu= findViewById(R.id.wifi_ssid_menu);
        wifiSSIDsAutoComplete= findViewById(R.id.wifi_ssid_autocomplete);

        String[] tzs= getResources().getStringArray(R.array.timezones_names);
        timezoneCodes= getResources().getStringArray(R.array.timezones_codes);
        timezones.addAll(Arrays.asList(tzs));
        prepareTimezoneListAdapter();

        prepareNextStep(UIState.PrepareBluetooth);

        userDataReceiveState= UserDataReceiveState.WaitingForResponse;
        user.downloadData(this::onDownloadUserDataResult);

        bleConfigurer= new BLEConfigurer(this, new BLEConfigurer.BLEConfigurerCallbacks() {
            @Override
            public void deviceSearchStarted() {
                prepareNextStep(NewDeviceActivity.UIState.SearchForDevice);
            }

            @Override
            public void onTimeout(BLEConfigurer.ConfigurerState state) {
                onActionTimeout(state);
            }

            @Override
            public void onDeviceBond(boolean success) {
                if (userDataReceiveState != UserDataReceiveState.Success) {
                    Log.d("NewDeviceActivity", "Failed reading user data!");
                    onConnectionFailed();
                    prepareNextStep(NewDeviceActivity.UIState.Failed);
                } else {
                    Log.d("NewDeviceActivity", "System state: WaitingForUserInput");

                    prepareRoomsListAdapter();
                    prepareNextStep(NewDeviceActivity.UIState.SetupWiFi);
                }
            }

            @Override
            public void onDeviceConfigured(boolean success) {
                prepareNextStep(NewDeviceActivity.UIState.DeviceConfigured);
            }

            @Override
            public void onWiFiListRefreshed(String wifis) {
                refreshAvailableWiFiSSIDs(wifis);
            }
        });
        bleConfigurer.setState(BLEConfigurer.ConfigurerState.CheckingPermissions);

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
        roomNames.add(getString(R.string.label_add_new_room));

        roomsAutoComplete.setSimpleItems(roomNames.toArray(new String[0]));
        roomsAutoComplete.setOnItemClickListener((parent, view, position, id) -> onRoomItemSelected(position));
    }

    private void onRoomItemSelected(int pos){
        Log.d("onRoomItemSelected", String.valueOf(pos));
        if(pos < user.getDataHandler().getRooms().size()) {
            configRoomIdx= pos;
            prepareSectorsListAdapter(pos);
        } else {
            Log.d("onRoomItemSelected", "Show new room edit");
            //TODO: ((View)findViewById(R.id.new_room_edit).getParent()).setVisibility(View.VISIBLE);
        }
    }

    private void prepareSectorsListAdapter(int roomId){
        sectorsMenu.setVisibility(View.VISIBLE);
        ArrayList<String> sectorNames= new ArrayList<>();
        ArrayList<Sector> userSectors= user.getDataHandler().getSectors(roomId);
        for(Sector s: userSectors){
            sectorNames.add(s.getName());
        }
        sectorNames.add(getString(R.string.label_add_new_sector));

        sectorsAutoComplete.setSimpleItems(sectorNames.toArray(new String[0]));
        sectorsAutoComplete.setOnItemClickListener((parent, view, position, id) -> onSectorItemSelected(position));
    }

    private void onSectorItemSelected(int pos){
        Log.d("onSectorItemSelected", String.valueOf(pos));
        if(pos < user.getDataHandler().getRooms().get(configRoomIdx).getSectors().size()){
            configSectorIdx= pos;
            if(bleConfigurer.getConnectedDevType()== Device.Type.Soil)
                preparePlantsListAdapter(configRoomIdx, configSectorIdx);
        } else {
            Log.d("onSectorItemSelected", "Show new sector edit");
            //TODO: ((View)findViewById(R.id.new_sector_edit).getParent()).setVisibility(View.VISIBLE);
        }
    }

    public void preparePlantsListAdapter(int roomId, int sectorId){
        ArrayList<String> plantNames= new ArrayList<>();
        ArrayList<Plant> userPlants= user.getDataHandler().getPlants(roomId, sectorId);
        for(Plant p: userPlants){
            plantNames.add(p.getName());
        }
        plantNames.add(getString(R.string.label_add_new_plant));

        plantsAutoComplete.setSimpleItems(plantNames.toArray(new String[0]));
        plantsAutoComplete.setOnItemClickListener((parent, view, position, id) -> onPlantsItemSelected(position));
    }


    private void onPlantsItemSelected(int pos){
        Log.d("onPlantsItemSelected", String.valueOf(pos));
        if(pos < user.getDataHandler().getRooms().get(configRoomIdx).getSectors()
                .get(configSectorIdx).getPlants().size()){
            configPlantIdx= pos;
        }else {
            Log.d("onPlantItemSelected", "Show new plant edit");
            //TODO: ((View)findViewById(R.id.new_plant_edit).getParent()).setVisibility(View.VISIBLE);
        }
    }

    private void prepareTimezoneListAdapter(){
        timezonesAutoComplete.setSimpleItems(timezones.toArray(new String[0]));
        timezonesAutoComplete.setOnItemClickListener((parent, view, position, id) ->
                bleConfigurer.setConfigTimezone(timezoneCodes[position]));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String tzName= TimeZone.getDefault().getID();
            int tzIdx= timezones.indexOf(tzName);
            if(tzIdx>=0){
                timezonesAutoComplete.setText(tzName, false);
                bleConfigurer.setConfigTimezone(timezoneCodes[tzIdx]);
                Log.d("ASD", "tzIdx>=0");
            }
        }
    }



    public void onActionTimeout(BLEConfigurer.ConfigurerState onState){
        BLEConfigurer.ConfigurerState onFailState= onState;
        onConnectionFailed();
        Log.e("NewDeviceActivity", "Timeout at "+onFailState.toString());
    }

    public void onConnectionFailed(){
        bleConfigurer.finishBLE();
        prepareNextStep(UIState.Failed);
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
                bleConfigurer.setState(BLEConfigurer.ConfigurerState.WaitingForPermissionsUserResponse);
                Log.d("NewDeviceActivity", "System state: WaitingForPermissionsUserResponse");
                requestPermissions(
                        new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION},
                        1);
            } else {
                bleConfigurer.startConnectingSystem();
            }
        } else {
            bleConfigurer.startConnectingSystem();
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
                bleConfigurer.startConnectingSystem();
        }
    }

    public void onNextClick(View v){
        Log.e("UIState", uiState.name());
        switch (uiState){
            case SetupWiFi:
                onWriteConfigClick();
                break;
            case DeviceConfigured:
                exitActivity();
                break;
        }
    }

    private void onWriteConfigClick(){
        String deviceName= devicesNameEdit.getText().toString();
        bleConfigurer.setConfigWifiSSID(wifiSSIDsAutoComplete.getText().toString());
        bleConfigurer.setConfigWifiPSK(wifiPskEdit.getText().toString());
        timezonesMenu.getEditText().getText().toString();

        // Remove trailing and leading spaces
        deviceName= deviceName.trim();

        if(bleConfigurer.getConfigWifiPSK().isEmpty() ||
                bleConfigurer.getConfigWifiSSID().isEmpty() || configRoomIdx==-1
                || (bleConfigurer.getConnectedDevType()== Device.Type.Light && configSectorIdx==-1)
                || (bleConfigurer.getConnectedDevType()== Device.Type.Light && deviceName.isEmpty())
                || (bleConfigurer.getConnectedDevType()== Device.Type.Soil && configPlantIdx==-1)
                || bleConfigurer.getConfigTimezone().isEmpty()){

            Log.d("IsEmpty", String.valueOf(bleConfigurer.getConfigWifiPSK().isEmpty()));
            Log.d("IsEmpty", String.valueOf(bleConfigurer.getConfigWifiSSID().isEmpty()));
            Log.d("IsEmpty", String.valueOf(configRoomIdx==-1));
            Log.d("IsEmpty", String.valueOf((bleConfigurer.getConnectedDevType()== Device.Type.Light && configSectorIdx==-1)));
            Log.d("IsEmpty", String.valueOf((bleConfigurer.getConnectedDevType()== Device.Type.Light && deviceName.isEmpty())));
            Log.d("IsEmpty", String.valueOf((bleConfigurer.getConnectedDevType()== Device.Type.Soil && configPlantIdx==-1)));
            Log.d("IsEmpty", String.valueOf(bleConfigurer.getConfigTimezone().isEmpty()));

            Snackbar.make(wifiSSIDsMenu, "Set your WiFi SSID, PSK and name your device", BaseTransientBottomBar.LENGTH_SHORT).show();
        } else {
            bleConfigurer.setState(BLEConfigurer.ConfigurerState.RegisteringDevice);
            Log.d("NewDeviceActivity", "System state: RegisteringDevice");
            progressBar.setVisibility(View.VISIBLE);
            startUITimeoutWatchdog(BLEConfigurer.ACTION_TIMEOUT_REGISTER_DEVICE);

            int roomId= user.getDataHandler().getRooms().get(configRoomIdx).getId();
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

            user.registerDevice(bleConfigurer.getConfigMAC(), roomId, sectorId, plantId,
                    deviceName, bleConfigurer.getConnectedDevType(),
                    this::onDeviceRegisterResult);
        }
    }



    private void onDeviceRegisterResult(boolean success){
        stopUITimeoutWatchdog();
        if(success){

            bleConfigurer.setState(BLEConfigurer.ConfigurerState.WritingCharacteristics);
            Log.d("NewDeviceActivity", "New version");
            Log.d("NewDeviceActivity", "System state: WritingCharacteristics");
            Log.d("NewDeviceActivity", "Populate device in database success");
            Log.d("NewDeviceActivity", "Sending picklock: "+user.getPicklock());
            bleConfigurer.writeCharacteristics(String.valueOf(user.getUid()), user.getPicklock());

        } else {
            bleConfigurer.setState(BLEConfigurer.ConfigurerState.ConnectionFailed);
            Log.e("NewDeviceActivity", "System state: ConnectionFailed");
            Log.e("NewDeviceActivity", "Populate device in database failed");
            onConnectionFailed();
        }
    }

    private void exitActivity(){
        bleConfigurer.finishBLE();
        finish();
    }

    private void refreshAvailableWiFiSSIDs(String wifisStr){
        String[] wifisDataArr= wifisStr.split(";");
        ArrayList<String> wifiSSIDsList= new ArrayList<>();

        for(String d: wifisDataArr){
            String[] parts= d.split("@");
            if(parts.length==2){
                wifiSSIDsList.add(parts[0]);
            }
        }

        wifiSSIDsAutoComplete.setSimpleItems(wifiSSIDsList.toArray(new String[0]));
    }

    private void prepareNextStep(@NonNull UIState next){
        switch (next){
            case PrepareBluetooth:
                step1Label.setVisibility(View.VISIBLE);
                step1Label.setTypeface(null, Typeface.BOLD);
                step2Label.setVisibility(View.GONE);
                step3Label.setVisibility(View.GONE);

                findViewById(R.id.found_device_text).setVisibility(View.GONE);
                findViewById(R.id.wifi_config_label).setVisibility(View.GONE);
                findViewById(R.id.placement_config_label).setVisibility(View.GONE);
                findViewById(R.id.timezone_config_label).setVisibility(View.GONE);
                wifiSSIDsMenu.setVisibility(View.GONE);
                ((View)wifiPskEdit.getParent()).setVisibility(View.GONE);
                ((View)devicesNameEdit.getParent()).setVisibility(View.GONE);
                roomsMenu.setVisibility(View.GONE);
                sectorsMenu.setVisibility(View.GONE);
                plantsMenu.setVisibility(View.GONE);
                timezonesMenu.setVisibility(View.GONE);

                progressBar.setVisibility(View.GONE);

                nextButton.setVisibility(View.GONE);
                break;
            case SearchForDevice:
                step1Label.setEnabled(false);
                step1Label.setTypeface(null, Typeface.NORMAL);
                step1Label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                step2Label.setVisibility(View.VISIBLE);
                step2Label.setTypeface(null, Typeface.BOLD);

                progressBar.setVisibility(View.VISIBLE);
                break;
            case SetupWiFi:
                step2Label.setEnabled(false);
                step2Label.setTypeface(null, Typeface.NORMAL);
                step2Label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                step3Label.setVisibility(View.VISIBLE);
                step3Label.setTypeface(null, Typeface.BOLD);


                wifiSSIDsMenu.setVisibility(View.VISIBLE);
                ((View)wifiPskEdit.getParent()).setVisibility(View.VISIBLE);
                findViewById(R.id.found_device_text).setVisibility(View.VISIBLE);
                ((TextView)findViewById(R.id.found_device_text)).setText(bleConfigurer.getFoundDeviceName());
                findViewById(R.id.wifi_config_label).setVisibility(View.VISIBLE);
                findViewById(R.id.placement_config_label).setVisibility(View.VISIBLE);
                findViewById(R.id.timezone_config_label).setVisibility(View.VISIBLE);
                roomsMenu.setVisibility(View.VISIBLE);
                timezonesMenu.setVisibility(View.VISIBLE);

                if(bleConfigurer.getConnectedDevType()== Device.Type.Light) {
                    ((View) devicesNameEdit.getParent()).setVisibility(View.VISIBLE);
                }

                if(bleConfigurer.getConnectedDevType()== Device.Type.Soil){
                    plantsMenu.setVisibility(View.VISIBLE);
                } else {
                    plantsMenu.setVisibility(View.GONE);
                }

                progressBar.setVisibility(View.GONE);

                nextButton.setVisibility(View.VISIBLE);
                nextButton.setText(R.string.button_config_device);
                break;
            case DeviceConfigured:
                step1Label.setVisibility(View.VISIBLE);
                step1Label.setTextColor(getColor(R.color.textDisabledLight));
                step1Label.setTypeface(null, Typeface.NORMAL);
                step2Label.setVisibility(View.VISIBLE);
                step2Label.setTextColor(getColor(R.color.textDisabledLight));
                step2Label.setTypeface(null, Typeface.NORMAL);
                step3Label.setVisibility(View.VISIBLE);
                step3Label.setTextColor(getColor(R.color.textDisabledLight));
                step3Label.setTypeface(null, Typeface.NORMAL);

                wifiSSIDsMenu.setVisibility(View.GONE);
                ((View)wifiPskEdit.getParent()).setVisibility(View.GONE);
                plantsMenu.setVisibility(View.GONE);

                progressBar.setVisibility(View.GONE);

                nextButton.setVisibility(View.VISIBLE);
                nextButton.setText(R.string.button_finish);
                break;
            case Failed:
                break;
        }
        uiState = next;
    }

    Timer timeoutWatchdogTimer;

    public void startUITimeoutWatchdog(long ms){
        if(timeoutWatchdogTimer !=null){
            timeoutWatchdogTimer.cancel();
        }

        timeoutWatchdogTimer = new Timer();
        timeoutWatchdogTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                // TODO: callbacks.onTimeout(state);
            }
        }, ms);
    }

    public void stopUITimeoutWatchdog(){
        if(timeoutWatchdogTimer !=null) {
            timeoutWatchdogTimer.cancel();
            timeoutWatchdogTimer = null;
        }
    }


}