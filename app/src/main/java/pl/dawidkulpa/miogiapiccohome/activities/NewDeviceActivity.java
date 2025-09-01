package pl.dawidkulpa.miogiapiccohome.activities;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.emoji2.widget.EmojiTextView;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.icu.util.TimeZone;
import android.os.Build;
import android.os.Bundle;
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
import pl.dawidkulpa.miogiapiccohome.ble.BLEConfigurer;
import pl.dawidkulpa.miogiapiccohome.R;

// TODO: Make Scan shorter and auto restart scan
// TODO: Put messages in search -> "still working", "just a minute" etc

public class NewDeviceActivity extends AppCompatActivity {
    public enum UIState {PrepareBluetooth, SearchForDevice, UserInputing, DeviceConfigured, UnexpectedDisconnect, Failed}

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

    String inputWifiSSID;
    String inputWifiPSK;
    String inputTimezone;


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

        prepareNextStep(UIState.PrepareBluetooth);

        userDataReceiveState= UserDataReceiveState.WaitingForResponse;
        user.downloadData(this::onDownloadUserDataResult);

        bleConfigurer= new BLEConfigurer(this, new BLEConfigurer.BLEConfigurerCallbacks() {
            @Override
            public void deviceSearchStarted() {
                prepareNextStep(NewDeviceActivity.UIState.SearchForDevice);
            }

            @Override
            public void onError(BLEConfigurer.ErrorCode errorCode) {
                onBLEError(errorCode);
            }

            @Override
            public void onDeviceFound(String name) {
                findViewById(R.id.step2_found_message).setVisibility(View.VISIBLE);
            }

            @Override
            public void onDeviceConnected() {

            }

            @Override
            public void onDeviceReady() {
                if (userDataReceiveState != UserDataReceiveState.Success) {
                    Log.d("NewDeviceActivity", "Failed reading user data!");
                    onBLEError(BLEConfigurer.ErrorCode.ConnectFailed);
                    prepareNextStep(NewDeviceActivity.UIState.Failed);
                } else {
                    Log.d("NewDeviceActivity", "System state: WaitingForUserInput");
                    prepareTimezoneListAdapter();
                    prepareRoomsListAdapter();
                    prepareNextStep(NewDeviceActivity.UIState.UserInputing);
                }
            }

            @Override
            public void onDeviceConfigured() {
                prepareNextStep(NewDeviceActivity.UIState.DeviceConfigured);
            }

            @Override
            public void onWiFiListRefreshed(String wifis) {
                refreshAvailableWiFiSSIDs(wifis);
            }
        });

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
            findViewById(R.id.new_room_input_layout).setVisibility(View.GONE);
        } else {
            findViewById(R.id.new_room_input_layout).setVisibility(View.VISIBLE);
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
            ((View)findViewById(R.id.new_sector_edit).getParent()).setVisibility(View.VISIBLE);
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
            ((View)findViewById(R.id.new_plant_edit).getParent()).setVisibility(View.VISIBLE);
        }
    }

    private void prepareTimezoneListAdapter(){
        timezonesAutoComplete.setSimpleItems(timezones.toArray(new String[0]));
        timezonesAutoComplete.setOnItemClickListener((parent, view, position, id) ->
                inputTimezone= timezoneCodes[position]);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String tzName= TimeZone.getDefault().getID();
            int tzIdx= timezones.indexOf(tzName);
            if(tzIdx>=0){
                timezonesAutoComplete.setText(tzName, false);
                inputTimezone= timezoneCodes[tzIdx];
                Log.d("ASD", "tzIdx>=0");
            }
        }
    }

    public void onBLEError(BLEConfigurer.ErrorCode errorCode){
        if(errorCode== BLEConfigurer.ErrorCode.UnexpectedDisconnect && uiState==UIState.UserInputing){
            prepareNextStep(UIState.UnexpectedDisconnect);
        } else {
            prepareNextStep(UIState.Failed);
        }
        Log.e("NewDeviceActivity", "BLE Error: "+ errorCode.toString());
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
            case UserInputing:
                onWriteConfigClick();
                break;
            case DeviceConfigured:
                exitActivity();
                break;
            case UnexpectedDisconnect:
            case Failed:
                retryConnect();
                break;
        }
    }

    private void retryConnect(){
        bleConfigurer.restart();
        prepareNextStep(UIState.PrepareBluetooth);
        prepareNextStep(UIState.SearchForDevice);
        bleConfigurer.startConnectingSystem();
    }

    private void onWriteConfigClick(){
        String deviceName= devicesNameEdit.getText().toString();
        inputWifiSSID= wifiSSIDsAutoComplete.getText().toString();
        inputWifiPSK= wifiPskEdit.getText().toString();

        // Remove trailing and leading spaces
        deviceName= deviceName.trim();

        if(inputWifiPSK.isEmpty() || inputWifiSSID.isEmpty() || configRoomIdx==-1
                || (bleConfigurer.getConnectedDevType()== Device.Type.Light && configSectorIdx==-1)
                || (bleConfigurer.getConnectedDevType()== Device.Type.Light && deviceName.isEmpty())
                || (bleConfigurer.getConnectedDevType()== Device.Type.Soil && configPlantIdx==-1)
                || (bleConfigurer.getConnectedDevType()== Device.Type.Air && configSectorIdx==-1)
                || inputTimezone.isEmpty()){

            Log.d("SSID IsEmpty", String.valueOf(inputWifiSSID.isEmpty()));
            Log.d("PSK IsEmpty", String.valueOf(inputWifiPSK.isEmpty()));
            Log.d("roomIdx IsEmpty", String.valueOf(configRoomIdx==-1));
            Log.d("light and sectorIdx IsEmpty", String.valueOf((bleConfigurer.getConnectedDevType()== Device.Type.Light && configSectorIdx==-1)));
            Log.d("light and devName IsEmpty", String.valueOf((bleConfigurer.getConnectedDevType()== Device.Type.Light && deviceName.isEmpty())));
            Log.d("soil and plantIdx IsEmpty", String.valueOf((bleConfigurer.getConnectedDevType()== Device.Type.Soil && configPlantIdx==-1)));
            Log.d("timezone IsEmpty", String.valueOf(inputTimezone.isEmpty()));

            Snackbar.make(wifiSSIDsMenu, "Set your WiFi SSID, PSK and name your device", BaseTransientBottomBar.LENGTH_SHORT).show();
        } else {
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
            Log.d("NewDeviceActivity", "New version");
            Log.d("NewDeviceActivity", "System state: WritingCharacteristics");
            Log.d("NewDeviceActivity", "Populate device in database success");
            bleConfigurer.writeDeviceConfig(inputWifiSSID, inputWifiPSK, String.valueOf(user.getUid()), user.getPicklock(), inputTimezone);

        } else {
            Log.e("NewDeviceActivity", "System state: ConnectionFailed");
            Log.e("NewDeviceActivity", "Populate device in database failed");
            onBLEError(BLEConfigurer.ErrorCode.ConfigWriteFailed);
        }
    }

    private void exitActivity(){
        bleConfigurer.finish(this);
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

    private void hideUserInputs(boolean hide, Device.Type devType){
        int state= hide?View.GONE:View.VISIBLE;

        findViewById(R.id.found_device_text).setVisibility(state);

        if(devType== Device.Type.Light)
            ((View)devicesNameEdit.getParent()).setVisibility(state);
        else
            ((View)devicesNameEdit.getParent()).setVisibility(View.GONE);

        findViewById(R.id.wifi_config_label).setVisibility(state);
        wifiSSIDsMenu.setVisibility(state);
        ((View)wifiPskEdit.getParent()).setVisibility(state);

        findViewById(R.id.placement_config_label).setVisibility(state);
        roomsMenu.setVisibility(state);
        findViewById(R.id.new_room_input_layout).setVisibility(View.GONE);
        sectorsMenu.setVisibility(View.GONE);
        findViewById(R.id.new_sector_input_layout).setVisibility(View.GONE);
        plantsMenu.setVisibility(View.GONE);
        findViewById(R.id.new_plant_input_layout).setVisibility(View.GONE);

        findViewById(R.id.timezone_config_label).setVisibility(state);
        timezonesMenu.setVisibility(state);
    }

    private void prepareNextStep(@NonNull UIState next){
        switch (next){
            case PrepareBluetooth:
                // Labels
                step1Label.setEnabled(true);
                step1Label.setVisibility(View.VISIBLE);
                step1Label.setTypeface(null, Typeface.BOLD);
                step1Label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                step2Label.setVisibility(View.GONE);
                step3Label.setVisibility(View.GONE);

                // Step 2 messages
                findViewById(R.id.step2_found_message).setVisibility(View.GONE);

                // User input form
                hideUserInputs(true, Device.Type.Unknown);

                // Step 3 messages
                findViewById(R.id.action_done_text).setVisibility(View.GONE);
                findViewById(R.id.action_failed_text).setVisibility(View.GONE);

                // Others
                progressBar.setVisibility(View.GONE);
                nextButton.setVisibility(View.INVISIBLE);
                break;
            case SearchForDevice:
                step1Label.setEnabled(false);
                step1Label.setTypeface(null, Typeface.NORMAL);
                step1Label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                step2Label.setVisibility(View.VISIBLE);
                step2Label.setEnabled(true);
                step2Label.setTypeface(null, Typeface.BOLD);
                step2Label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);

                // Step 2 messages
                findViewById(R.id.step2_found_message).setVisibility(View.GONE);

                // User input form
                hideUserInputs(true, Device.Type.Unknown);

                // Step 3 messages
                findViewById(R.id.action_done_text).setVisibility(View.GONE);
                findViewById(R.id.action_failed_text).setVisibility(View.GONE);

                // Others
                progressBar.setVisibility(View.VISIBLE);
                nextButton.setVisibility(View.INVISIBLE);

                break;
            case UserInputing:
                step2Label.setEnabled(false);
                step2Label.setTypeface(null, Typeface.NORMAL);
                step2Label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                step3Label.setVisibility(View.VISIBLE);
                step3Label.setEnabled(true);
                step3Label.setTypeface(null, Typeface.BOLD);
                step3Label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);

                // Step 2 messages
                findViewById(R.id.step2_found_message).setVisibility(View.GONE);

                // User input form
                hideUserInputs(false, bleConfigurer.getConnectedDevType());
                ((TextView)findViewById(R.id.found_device_text)).setText(bleConfigurer.getFoundDeviceName());

                // Step 3 messages
                findViewById(R.id.action_done_text).setVisibility(View.GONE);
                findViewById(R.id.action_failed_text).setVisibility(View.GONE);

                // Others
                progressBar.setVisibility(View.GONE);
                nextButton.setVisibility(View.VISIBLE);
                nextButton.setText(R.string.button_config_device);
                break;
            case DeviceConfigured:
                step1Label.setVisibility(View.VISIBLE);
                step1Label.setEnabled(false);
                step1Label.setTypeface(null, Typeface.NORMAL);
                step2Label.setVisibility(View.VISIBLE);
                step2Label.setEnabled(false);
                step2Label.setTypeface(null, Typeface.NORMAL);
                step3Label.setVisibility(View.VISIBLE);
                step3Label.setEnabled(false);
                step3Label.setTypeface(null, Typeface.NORMAL);
                step3Label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);

                // Step 2 messages
                findViewById(R.id.step2_found_message).setVisibility(View.GONE);

                // User input form
                hideUserInputs(true, Device.Type.Unknown);

                // Step 3 messages
                findViewById(R.id.action_done_text).setVisibility(View.VISIBLE);
                findViewById(R.id.action_failed_text).setVisibility(View.GONE);

                // Others
                progressBar.setVisibility(View.GONE);
                nextButton.setVisibility(View.VISIBLE);
                nextButton.setText(R.string.button_finish);
                break;
            case Failed:
                step1Label.setVisibility(View.VISIBLE);
                step1Label.setEnabled(false);
                step1Label.setTypeface(null, Typeface.NORMAL);
                step2Label.setVisibility(View.VISIBLE);
                step2Label.setEnabled(false);
                step2Label.setTypeface(null, Typeface.NORMAL);
                step2Label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                step3Label.setVisibility(View.VISIBLE);
                step3Label.setEnabled(false);
                step3Label.setTypeface(null, Typeface.NORMAL);
                step3Label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);

                // Step 2 messages
                findViewById(R.id.step2_found_message).setVisibility(View.GONE);

                // User input form
                hideUserInputs(true, Device.Type.Unknown);

                // Step 3 messages
                findViewById(R.id.action_done_text).setVisibility(View.GONE);
                ((EmojiTextView)findViewById(R.id.action_failed_text)).setText(R.string.message_device_connect_failed);
                findViewById(R.id.action_failed_text).setVisibility(View.VISIBLE);

                // Others
                progressBar.setVisibility(View.GONE);
                nextButton.setVisibility(View.VISIBLE);
                nextButton.setText(R.string.button_try_again);
                break;
            case UnexpectedDisconnect:
                step1Label.setVisibility(View.VISIBLE);
                step1Label.setEnabled(false);
                step1Label.setTypeface(null, Typeface.NORMAL);
                step2Label.setVisibility(View.VISIBLE);
                step2Label.setEnabled(false);
                step2Label.setTypeface(null, Typeface.NORMAL);
                step2Label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                step3Label.setVisibility(View.VISIBLE);
                step3Label.setEnabled(false);
                step3Label.setTypeface(null, Typeface.NORMAL);
                step3Label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);

                // Step 2 messages
                findViewById(R.id.step2_found_message).setVisibility(View.GONE);

                // User input form
                hideUserInputs(true, Device.Type.Unknown);

                // Step 3 messages
                findViewById(R.id.action_done_text).setVisibility(View.GONE);
                ((EmojiTextView)findViewById(R.id.action_failed_text)).setText(R.string.message_device_unexpected_disconnect);
                findViewById(R.id.action_failed_text).setVisibility(View.VISIBLE);

                // Others
                progressBar.setVisibility(View.GONE);
                nextButton.setVisibility(View.VISIBLE);
                nextButton.setText(R.string.button_try_again);
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
                // TODO: action on timer timeout
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