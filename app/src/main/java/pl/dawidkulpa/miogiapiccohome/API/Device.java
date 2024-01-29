package pl.dawidkulpa.miogiapiccohome.API;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

import pl.dawidkulpa.miogiapiccohome.R;

public class Device {
    public enum Type {Unknown, Light, Soil, Air};

    public static final int DEVICE_NO_PARENT_ID=-1;

    private static final String JSON_TAG_ID= "id";
    private static final String JSON_TAG_LAST_SEEN= "ls";
    private static final String JSON_TAG_SOFTWARE_VERSION= "sv";
    private static final String JSON_TAG_HARDWARE_VERSION= "hv";
    private static final String JSON_TAG_UPDATE_ALLOWED="ua";
    private static final String JSON_TAG_UPDATE_AVAILABLE="uav";

    public static SimpleDateFormat sqlSDF= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    private final String id;
    private final Calendar lastSeen;
    private final String sv;            // Software version
    private final String hv;            // Hardware version
    private final boolean updateAllowed;
    private final boolean updateAvailable;

    public Device(JSONObject jobj) throws JSONException, ParseException {
        id= jobj.getString(JSON_TAG_ID);

        if(jobj.has(JSON_TAG_HARDWARE_VERSION)){
            hv= jobj.getString(JSON_TAG_HARDWARE_VERSION);
        } else {
            hv= "";
        }

        if(jobj.has(JSON_TAG_SOFTWARE_VERSION)){
            sv= jobj.getString(JSON_TAG_SOFTWARE_VERSION);
        } else {
            sv= "";
        }

        String strLS= jobj.getString(JSON_TAG_LAST_SEEN);
        lastSeen= Calendar.getInstance();
        lastSeen.setTime(Objects.requireNonNull(sqlSDF.parse(strLS)));
        //updateAllowed= (jobj.getInt(JSON_TAG_UPDATE_ALLOWED)==1);
        //updateAvailable= (jobj.getInt(JSON_TAG_UPDATE_AVAILABLE)==1);
        updateAvailable= false;
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

    public String getSoftwareVersion(){
        return sv;
    }

    public String getHardwareVersion(){
        return hv;
    }

    public boolean isUpdateAllowed(){
        return updateAllowed;
    }

    public boolean isUpdateAvailable(){
        return updateAvailable;
    }
}
