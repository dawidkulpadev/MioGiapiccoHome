package pl.dawidkulpa.miogiapiccohome.API;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.ParseException;

import pl.dawidkulpa.miogiapiccohome.activities.MainActivity;
import pl.dawidkulpa.miogiapiccohome.R;
import pl.dawidkulpa.scm.Query;
import pl.dawidkulpa.scm.ServerRequest;

public class StateWatcher extends BroadcastReceiver {
    public static final int ERROR_INTERVAL= 10 * 60 * 1000;

    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        final PendingResult pendingResult = goAsync();

        this.context= context;
        Log.e("APISyncer", "Start...");

        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this.context);
        String login= prefs.getString("login", "");
        String pass= prefs.getString("pass", "");
        if(!login.isEmpty() && !pass.isEmpty()) {
            ServerRequest sr= new ServerRequest(Query.FormatType.Pairs, ServerRequest.METHOD_POST,
                    ServerRequest.RESPONSE_TYPE_JSON, ServerRequest.TIMEOUT_DEFAULT,
                    (respCode, jObject) -> {
                        if(respCode==200) {
                            try {
                                onGetResponse(jObject.getJSONArray("array"));
                            } catch (JSONException je) {
                                Log.e("MainAcitivty", je.getMessage());
                            }
                        }

                        Log.e("APISyncer", "Stop...");
                        pendingResult.finish();
                    });

            sr.addRequestDataPair("login", login);
            sr.addRequestDataPair("pass", pass);

            sr.start("https://dawidkulpa.pl/apis/miogiapicco/user/getplants.php");
        }
    }

    public void onGetResponse(JSONArray jarr){
        try {
            for(int i=0; i<jarr.length(); i++){
                Plant p= new Plant(jarr.getJSONObject(i));

                if(p.getLastLastSeen()>ERROR_INTERVAL){
                    StringBuilder submessage= new StringBuilder();

                    String message = p.getName() + " " +
                            context.getString(R.string.malfunction_detected);

                    submessage.append(context.getString(R.string.last_seen));
                    submessage.append(": ");
                    if (p.getLastLastSeen() == 0) {
                        submessage.append(context.getString(R.string.info_seconds_ago));
                    } else if (p.getLastLastSeen() < 60) {
                        submessage.append(context.getString(R.string.info_min_ago, p.getLastLastSeen()));
                    } else if (p.getLastLastSeen() < 60*24){
                        submessage.append(context.getString(R.string.info_hours_ago, p.getLastLastSeen()/60));
                    } else {
                        submessage.append(context.getString(R.string.info_days_ago, p.getLastLastSeen()/(60*24)));
                    }

                    showNotification(message,
                            submessage.toString());
                }
            }
        } catch (JSONException | ParseException e){
            Log.e("APISyncer", "Cannot show notification :(");
        }
    }

    public void showNotification(String name, String descr){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
                .setSmallIcon(R.drawable.logo_notif)
                .setContentTitle(name)
                .setContentText(descr)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(1, builder.build());
    }
}
