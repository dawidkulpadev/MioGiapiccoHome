package pl.dawidkulpa.miogiapiccohome.API;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Objects;

import pl.dawidkulpa.miogiapiccohome.activities.MainActivity;
import pl.dawidkulpa.miogiapiccohome.R;
import pl.dawidkulpa.miogiapiccohome.activities.SignInActivity;

public class BackgroundWatcher extends BroadcastReceiver {
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        final PendingResult pendingResult = goAsync();

        this.context = context;
        Log.e("APISyncer", "Start...");
    }


    public void onGetResponse(JSONArray jarr) {
        Log.d("StateWatcher", "onGetResponse");
        try {
            if(jarr.length()!=0){
                StringBuilder submsg= new StringBuilder();

                for(int i=0; i<jarr.length(); i++){
                    submsg.append(jarr.getJSONObject(i).getString("n"));
                    if(i<jarr.length()-1){
                        submsg.append(", ");
                    }
                }

                showNotification(context.getString(R.string.malfunction_detected),
                        submsg.toString());
            }
        } catch (JSONException e) {
            Log.e("APISyncer", "Cannot show notification :(");
        }
    }

    public void showNotification(String name, String descr) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
                .setSmallIcon(R.drawable.logo_notif)
                .setContentTitle(name)
                .setContentText(descr)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(1, builder.build());
        }

    }
}
