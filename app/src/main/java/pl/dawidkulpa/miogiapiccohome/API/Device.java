package pl.dawidkulpa.miogiapiccohome.API;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class Device {
    public enum Type {Unknown, Light, Soil, Air};

    public static final int DEVICE_NO_PARENT_ID=-1;

    private static final String JSON_TAG_ID= "id";
    private static final String JSON_TAG_LAST_SEEN= "ls";

    public static SimpleDateFormat sqlSDF= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    private final String id;
    private final Calendar lastSeen;
    private final boolean updateAllowed;

    public Device(JSONObject jobj) throws JSONException, ParseException {
        Log.d("Device", "Parsing "+jobj.toString());
        id= jobj.getString(JSON_TAG_ID);

        String strLS= jobj.getString(JSON_TAG_LAST_SEEN);
        lastSeen= Calendar.getInstance();
        lastSeen.setTime(Objects.requireNonNull(sqlSDF.parse(strLS)));
        updateAllowed= false;
    }


    public int getLastSeen() {
        Calendar c= Calendar.getInstance();
        long diff= c.getTimeInMillis()-lastSeen.getTimeInMillis();
        return (int)(diff/60000);
    }

    public String getId() {
        return id;
    }
}
