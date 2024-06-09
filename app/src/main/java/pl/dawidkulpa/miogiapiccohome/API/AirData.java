package pl.dawidkulpa.miogiapiccohome.API;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class AirData {
    private static final String JSON_TAG_AIR_HUMIDITY= "ah";
    private static final String JSON_TAG_AIR_TEMPERATURE= "at";
    private static final String JSON_TAG_TIMESTAMP= "ts";

    public static SimpleDateFormat sqlSDF= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    public static SimpleDateFormat hmTimeSDF= new SimpleDateFormat("HH:mm", Locale.getDefault());

    private float temp;
    private float hum;
    private Calendar timestamp;

    public AirData(JSONObject jobj) throws JSONException, ParseException {
        temp= (float) jobj.getDouble(JSON_TAG_AIR_TEMPERATURE);
        hum= (float) jobj.getDouble(JSON_TAG_AIR_HUMIDITY);

        String strLS= jobj.getString(JSON_TAG_TIMESTAMP);
        timestamp= Calendar.getInstance();
        timestamp.setTime(Objects.requireNonNull(sqlSDF.parse(strLS)));
    }

    public float getTemp() {
        return temp;
    }

    public float getHum() {
        return hum;
    }

    public Calendar getTimestamp() {
        return timestamp;
    }

    public String getStringTimestamp(){
        return sqlSDF.format(timestamp.getTime());
    }

    public String getStringTime(){
        return hmTimeSDF.format(timestamp.getTime());
    }
}
