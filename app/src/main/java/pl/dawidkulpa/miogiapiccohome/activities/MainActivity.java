package pl.dawidkulpa.miogiapiccohome.activities;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

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
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.Objects;

import pl.dawidkulpa.miogiapiccohome.API.AirDevice;
import pl.dawidkulpa.miogiapiccohome.API.LightDevice;
import pl.dawidkulpa.miogiapiccohome.API.Plant;
import pl.dawidkulpa.miogiapiccohome.API.SoilDevice;
import pl.dawidkulpa.miogiapiccohome.API.StateWatcher;
import pl.dawidkulpa.miogiapiccohome.API.User;
import pl.dawidkulpa.miogiapiccohome.API.UserData;
import pl.dawidkulpa.miogiapiccohome.adapters.CreatePlantDialog;
import pl.dawidkulpa.miogiapiccohome.adapters.RoomsListAdapter;
import pl.dawidkulpa.miogiapiccohome.R;

public class MainActivity extends AppCompatActivity implements RoomsListAdapter.DataChangeListener {

    public static final String CHANNEL_ID= "dev_notifs";

    private RoomsListAdapter adapter;

    private User user;

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        checkAndRequestPermissions();

        user= getIntent().getParcelableExtra("UserAPI");
        if(user==null){
            Log.e("MainActivity", "User is null!");
            Intent intent= new Intent(this, SignInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(intent);
        }

        Objects.requireNonNull(getSupportActionBar()).setTitle(user.getLogin());

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> onFABClick());
        findViewById(R.id.fab_add_plant).setOnClickListener(view -> onAddPlantClick());
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
        adapter= new RoomsListAdapter(this, user.getDataHandler().getRooms(), this);
        rv.setAdapter(adapter);

        progressBar = findViewById(R.id.progressbar);

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
            startBackground();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean fabExpanded=false;
    public void onFABClick(){
        FloatingActionButton plantFAB = findViewById(R.id.fab_add_plant);
        FloatingActionButton deviceFAB = findViewById(R.id.fab_register_device);

        if(fabExpanded){
            ObjectAnimator plantFABAnimation = ObjectAnimator.ofFloat(plantFAB, "translationY", 0f);
            plantFABAnimation.setDuration(200);
            plantFABAnimation.start();

            ObjectAnimator deviceFABanimation = ObjectAnimator.ofFloat(deviceFAB, "translationY", 0f);
            deviceFABanimation.setDuration(200);
            deviceFABanimation.start();

            plantFAB.setVisibility(View.INVISIBLE);
            deviceFAB.setVisibility(View.INVISIBLE);

            fabExpanded= false;
        } else {
            int fabSize= plantFAB.getMeasuredHeight();
            int marginPxSize= getResources().getDimensionPixelSize(R.dimen.fab_margin);

            ObjectAnimator plantFABAnimation = ObjectAnimator.ofFloat(plantFAB, "translationY", -2*(fabSize+marginPxSize));
            plantFABAnimation.setDuration(200);
            plantFABAnimation.start();

            ObjectAnimator deviceFABanimation = ObjectAnimator.ofFloat(deviceFAB, "translationY", -(fabSize+marginPxSize));
            deviceFABanimation.setDuration(200);
            deviceFABanimation.start();

            plantFAB.setVisibility(View.VISIBLE);
            deviceFAB.setVisibility(View.VISIBLE);

            fabExpanded= true;
        }
    }

    private void onAddPlantClick(){
        CreatePlantDialog createPlantDialog= new CreatePlantDialog(this::onCreatePlantDialogPositiveClick);

        createPlantDialog.show(this);
    }

    private void onCreatePlantDialogPositiveClick(String v){
        progressBar.setVisibility(View.VISIBLE);
        user.createPlant(v, this::onCreatePlantResult);
    }

    private void onRegisterDeviceClick(){
        Intent intent= new Intent(this, NewDeviceActivity.class);
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

    private void onCreatePlantResult(boolean success){
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

    @Override
    public void onLightDeviceDataChanged(LightDevice d) {
        user.updateLightDevice(d, this::onUpdateLightDeviceResult);
    }

    @Override
    public void onSoilDeviceDataChanged(SoilDevice d) {

    }

    @Override
    public void onAirDeviceDataChanged(AirDevice d) {

    }
}
