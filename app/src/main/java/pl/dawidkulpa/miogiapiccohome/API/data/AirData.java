package pl.dawidkulpa.miogiapiccohome.API.data;

import com.google.gson.JsonObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class AirData {
    private static final String JSON_TAG_AIR_HUMIDITY= "ah";
    private static final String JSON_TAG_AIR_TEMPERATURE= "at";
    private static final String JSON_TAG_TIMESTAMP= "ts";

    public static SimpleDateFormat sqlSDF= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
    public static SimpleDateFormat hmTimeSDF= new SimpleDateFormat("HH:mm", Locale.getDefault());

    private final float temp;
    private final float hum;
    private final Calendar timestamp;

    public AirData(JsonObject jobj) throws ParseException {
        temp= (float) jobj.get(JSON_TAG_AIR_TEMPERATURE).getAsDouble();
        hum= (float) jobj.get(JSON_TAG_AIR_HUMIDITY).getAsDouble();

        String strLS= jobj.get(JSON_TAG_TIMESTAMP).getAsString();
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
