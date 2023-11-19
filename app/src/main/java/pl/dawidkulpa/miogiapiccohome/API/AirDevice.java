package pl.dawidkulpa.miogiapiccohome.API;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

public class AirDevice extends Device{
    private static final String JSON_TAG_AIR_HUM="ah";
    private static final String JSON_TAG_AIR_TEMP="at";
    private static final String JSON_TAG_HUMIDIFIER_WATER_LEVEL="hwl";

    private double airHumidity;
    private double aitTemperature;
    private int humidWaterLvl;

    public AirDevice(JSONObject jobj) throws JSONException, ParseException {
        super(jobj);

        airHumidity= jobj.getDouble(JSON_TAG_AIR_HUM);
        aitTemperature= jobj.getDouble(JSON_TAG_AIR_TEMP);
        humidWaterLvl= jobj.getInt(JSON_TAG_HUMIDIFIER_WATER_LEVEL);
    }

    public double getAirHumidity() {
        return airHumidity;
    }

    public double getAitTemperature() {
        return aitTemperature;
    }

    public int getHumidWaterLvl() {
        return humidWaterLvl;
    }
}
