package pl.dawidkulpa.miogiapiccohome.activities;

import android.animation.ObjectAnimator;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.Objects;

import pl.dawidkulpa.miogiapiccohome.API.LightDevice;
import pl.dawidkulpa.miogiapiccohome.API.Plant;
import pl.dawidkulpa.miogiapiccohome.API.StateWatcher;
import pl.dawidkulpa.miogiapiccohome.API.User;
import pl.dawidkulpa.miogiapiccohome.adapters.CreatePlantDialog;
import pl.dawidkulpa.miogiapiccohome.adapters.PlantsListAdapter;
import pl.dawidkulpa.miogiapiccohome.R;

public class MainActivity extends AppCompatActivity {

    public static final String CHANNEL_ID= "dev_notifs";

    private PlantsListAdapter adapter;
    private ArrayList<Plant> plants;

    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        user= getIntent().getParcelableExtra("UserAPI");
        if(user==null){
            Intent intent= new Intent(this, SignInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(intent);
        }

        Objects.requireNonNull(getSupportActionBar()).setTitle(user.getLogin());

        plants= new ArrayList<>();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> onFABClick());
        findViewById(R.id.fab_add_plant).setOnClickListener(view -> onAddPlantClick());
        findViewById(R.id.fab_register_device).setOnClickListener(view -> onRegisterDeviceClick());

        final SwipeRefreshLayout srl= findViewById(R.id.swipe_refresh_layout);
        srl.setOnRefreshListener(() -> {
            user.getPlantsList();
            srl.setRefreshing(false);
        });

        RecyclerView.LayoutManager layoutManager;
        layoutManager = new LinearLayoutManager(this);
        RecyclerView rv= findViewById(R.id.dev_list);
        rv.setLayoutManager(layoutManager);
        rv.setHasFixedSize(true);
        adapter= new PlantsListAdapter(this, plants, new PlantsListAdapter.DataChangeListener() {
            @Override
            public void onPlantNameChanged(Plant p) {
                user.updatePlantName(p);
            }

            @Override
            public void onLightDataChanged(LightDevice d) {
                user.updateLightDevice(d);
            }
        });
        rv.setAdapter(adapter);

        ProgressBar progressBar = findViewById(R.id.progressbar);
        user.setProgressBar(progressBar);


        user.setGetPlantsListener((success, newPlantsList) -> {
            if(success) {
                processReceivedPlantData(newPlantsList);
            } else {
                Snackbar.make(findViewById(R.id.dev_list), "Server error :(", BaseTransientBottomBar.LENGTH_SHORT).show();
            }
        });
        user.setUpdateDeviceListener(success -> {
            if(success) {
                user.getPlantsList();
            } else {
                Snackbar.make(findViewById(R.id.dev_list), "Server error :(", BaseTransientBottomBar.LENGTH_SHORT).show();
            }
        });

        user.getPlantsList();
    }

    private void processReceivedPlantData(ArrayList<Plant> newPlantsList){
        int i=0;

        // Remove not received
        while(i<plants.size()){
            if(!newPlantsList.contains(plants.get(i))){
                plants.remove(i);
                adapter.notifyItemRemoved(i);
            } else {
                i++;
            }
        }

        // Update existing and add new
        for (i = 0; i < newPlantsList.size(); i++) {
            int inOld = plants.indexOf(newPlantsList.get(i));
            if (inOld >= 0) {
                plants.get(inOld).update(newPlantsList.get(i));
                adapter.notifyItemChanged(inOld);
            } else {
                plants.add(newPlantsList.get(i));
                adapter.notifyItemInserted(plants.size()-1);
            }
        }
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
        CreatePlantDialog createPlantDialog= new CreatePlantDialog(v -> user.createPlant(v, success -> {
            if(success) {
                user.getPlantsList();
            } else {
                Snackbar.make(findViewById(R.id.dev_list), "Server error :(", BaseTransientBottomBar.LENGTH_SHORT).show();
            }
        }));
        createPlantDialog.show(this);
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
}
