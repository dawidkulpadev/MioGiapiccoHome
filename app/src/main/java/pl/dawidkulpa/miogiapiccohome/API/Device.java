package pl.dawidkulpa.miogiapiccohome.API;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class Device {
    public static SimpleDateFormat sqlSDF= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    private final String id;
    private final Calendar lastSeen;

    public Device(JSONObject jobj) throws JSONException, ParseException {
        id= jobj.getString("id");

        String strLS= jobj.getString("LS");
        lastSeen= Calendar.getInstance();
        lastSeen.setTime(Objects.requireNonNull(sqlSDF.parse(strLS)));
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
