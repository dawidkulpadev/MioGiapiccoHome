package pl.dawidkulpa.miogiapiccohome.activities;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import java.util.Calendar;
import java.util.Objects;

import pl.dawidkulpa.miogiapiccohome.API.AirDevice;
import pl.dawidkulpa.miogiapiccohome.API.Device;
import pl.dawidkulpa.miogiapiccohome.API.LightDevice;
import pl.dawidkulpa.miogiapiccohome.API.Plant;
import pl.dawidkulpa.miogiapiccohome.API.Room;
import pl.dawidkulpa.miogiapiccohome.API.Sector;
import pl.dawidkulpa.miogiapiccohome.API.SoilDevice;
import pl.dawidkulpa.miogiapiccohome.API.StateWatcher;
import pl.dawidkulpa.miogiapiccohome.API.User;
import pl.dawidkulpa.miogiapiccohome.API.UserData;
import pl.dawidkulpa.miogiapiccohome.EditTextWatcher;
import pl.dawidkulpa.miogiapiccohome.adapters.RoomsListAdapter;
import pl.dawidkulpa.miogiapiccohome.R;
import pl.dawidkulpa.miogiapiccohome.dialogs.AirDataPlotDialog;
import pl.dawidkulpa.miogiapiccohome.dialogs.NewSectorDialog;

public class MainActivity extends AppCompatActivity
        implements RoomsListAdapter.DataChangeListener, NewSectorDialog.ClosedListener, AirDataPlotDialog.AirDataRequestListener {

    public static final String CHANNEL_ID= "dev_notifs";

    private RoomsListAdapter adapter;
    private User user;

    private ProgressBar progressBar;

    AirDataPlotDialog airDataPlotDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        checkAndRequestPermissions();

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });

        user= getIntent().getParcelableExtra("UserAPI");
        if(user==null){
            Log.e("MainActivity", "User is null!");
            Intent intent= new Intent(this, SignInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(intent);
        }

        Objects.requireNonNull(getSupportActionBar()).setTitle(user.getLogin());

        findViewById(R.id.fab_register_device).setOnClickListener(view -> onRegisterDeviceClick());

        final SwipeRefreshLayout srl= findViewById(R.id.swipe_refresh_layout);
        srl.setOnRefreshListener(() -> {
            startUserDataDownload();
            srl.setRefreshing(false);
        });

        RecyclerView.LayoutManager layoutManager;
        layoutManager = new LinearLayoutManager(this);
        RecyclerView rv= findViewById(R.id.dev_list);
        rv.setLayoutManager(layoutManager);
        rv.setHasFixedSize(true);
        adapter= new RoomsListAdapter(this, user.getDataHandler().getRooms(), this, this, this);
        rv.setAdapter(adapter);

        progressBar = findViewById(R.id.progressbar);

        findViewById(R.id.new_room_button).setOnClickListener(v -> onAddRoomClick());

        airDataPlotDialog= new AirDataPlotDialog();

        startUserDataDownload();
    }

    public void checkAndRequestPermissions(){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            boolean postNotifPermited=
                    ActivityCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED;

            if(!postNotifPermited){
                Log.d("MainActivity", "System state: WaitingForPermissionsUserResponse");
                requestPermissions(
                        new String[]{"android.permission.POST_NOTIFICATIONS"},
                        1);
            }
        }
    }

    public void startUserDataDownload(){
        progressBar.setVisibility(View.VISIBLE);
        user.downloadData(this::onDownloadDataResult);
    }



    private void processReceivedUserData(UserData userData){
        adapter.updateList(userData.getRooms());
        adapter.notifyDataSetChanged();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();



        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            // startBackground();
            return true;
        } else if(id == R.id.action_logout){
            SignInActivity.removeLastLoginData(this);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void onAddRoomClick(){
        MaterialAlertDialogBuilder madb= new MaterialAlertDialogBuilder(this);
        madb.setTitle(R.string.title_create_room_dialog).setIcon(R.drawable.icon_add)
                .setMessage(R.string.message_set_rooms_name)
                .setView(R.layout.dialog_new_room)
                .setNegativeButton(R.string.button_dismiss, (dialog, which) -> {

                })
                .setPositiveButton(R.string.button_create, null);

        AlertDialog dialog= madb.create();
        dialog.setOnShowListener(d -> {
            Button b= ((AlertDialog)d).getButton(AlertDialog.BUTTON_POSITIVE);
            b.setOnClickListener(v -> onCreateRoomDialogPositiveClick(((AlertDialog) d)));
            b.setEnabled(false);

            TextInputLayout til= ((AlertDialog)d).findViewById(R.id.text_input_layout);
            TextInputEditText tiet= ((AlertDialog)d).findViewById(R.id.text_input);
            if(til!=null && tiet!=null)
                tiet.addTextChangedListener(new EditTextWatcher(til, b, ((AlertDialog) d).getContext().getString(R.string.error_empty_name)));
        });

        dialog.show();
    }


    private void onCreateRoomDialogPositiveClick(AlertDialog d){
        TextInputEditText input= d.findViewById(R.id.text_input);

        if(input!=null){
            progressBar.setVisibility(View.VISIBLE);
            user.createRoom(Objects.requireNonNull(input.getText()).toString(), this::onCreateRoomResult);
            d.dismiss();
        }
    }

    private void onRegisterDeviceClick(){
        Intent intent= new Intent(this, NewDeviceActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.putExtra("UserAPI", user);
        startActivity(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void startBackground(){
        createNotificationChannel();
        AlarmManager alarmManager =
                (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

        Intent i = new Intent(this, StateWatcher.class);
        PendingIntent pi= PendingIntent.getBroadcast(this,0,i,PendingIntent.FLAG_IMMUTABLE);

        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_HALF_HOUR,
                AlarmManager.INTERVAL_HALF_HOUR, pi);
    }

    public void onDownloadDataResult(boolean success, UserData userData) {
        if(success)
            processReceivedUserData(userData);
        else
            Snackbar.make(findViewById(R.id.dev_list), "Server error :(", BaseTransientBottomBar.LENGTH_SHORT).show();
        progressBar.setVisibility(View.INVISIBLE);
    }

    private void onCreateRoomResult(boolean success){
        if(success) {
            startUserDataDownload();
        } else {
            Snackbar.make(findViewById(R.id.dev_list), "Server error :(", BaseTransientBottomBar.LENGTH_SHORT).show();
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    private void onUpdateLightDeviceResult(boolean success){
        if(success) {
            startUserDataDownload();
        } else {
            Snackbar.make(findViewById(R.id.dev_list), "Server error :(", BaseTransientBottomBar.LENGTH_SHORT).show();
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    private void onUpdateRoomResult(boolean success){
        if(success) {
            startUserDataDownload();
        } else {
            Snackbar.make(findViewById(R.id.dev_list), "Server error :(", BaseTransientBottomBar.LENGTH_SHORT).show();
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onLightDeviceDataChanged(LightDevice d) {
        user.updateLightDevice(d, this::onUpdateLightDeviceResult);
    }

    @Override
    public void onDeviceUpdateEnableClick(Device d) {
        user.markLightDeviceUpgradeAllowed(d, success -> startUserDataDownload());
    }

    @Override
    public void onDeviceDeleteClick(Device d) {
        user.unregisterDevice(d, this::onUpdateLightDeviceResult);
    }

    @Override
    public void onRoomDeleteClick(Room r) {
        user.deleteRoom(r, this::onUpdateRoomResult);
    }

    @Override
    public void onSoilDeviceDataChanged(SoilDevice d) {

    }

    @Override
    public void onAirDeviceDataChanged(AirDevice d) {

    }

    @Override
    public void onPlantDataChanged(Plant p) {

    }

    @Override
    public void onRoomNameChanged(Room r, String newName) {
        user.updateRoom(r, newName, this::onUpdateRoomResult);
    }

    @Override
    public void onSectorNameChanged(Sector s, String newName) {
        user.updateSector(s, newName, this::onUpdateRoomResult);
    }

    @Override
    public void onSectorDeleteClick(Sector s) {
        user.deleteSector(s, this::onUpdateRoomResult);
    }

    @Override
    public void onPositiveClick(int roomId, String name) {
        progressBar.setVisibility(View.VISIBLE);
        user.createSector(name, roomId, this::onCreateSectorResult);
    }

    private void onCreateSectorResult(boolean success){
        if(success) {
            startUserDataDownload();
        } else {
            Snackbar.make(findViewById(R.id.dev_list), "Server error :(", BaseTransientBottomBar.LENGTH_SHORT).show();
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void request(AirDevice ad, User.DownloadAirDataHistoryListener dadh, Calendar start, Calendar end) {
        user.getAirDataHistory(ad, dadh, start, end);
    }
}
