package pl.dawidkulpa.miogiapiccohome.API;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

public class AirDevice extends Device{
    private static final String JSON_TAG_NAME="n";
    private static final String JSON_TAG_AIR_HUM="ah";
    private static final String JSON_TAG_AIR_TEMP="at";
    private static final String JSON_TAG_HUMIDIFIER_WATER_LEVEL="hwl";
    private static final String JSON_TAG_CO2_PPM="c";

    private int roomParentId;
    private int sectorParentId;

    private String name;

    private double airHumidity;
    private double aitTemperature;
    private int humidWaterLvl;
    private int co2ppm;

    public AirDevice(JSONObject jobj, int roomId, int sectorId) throws JSONException, ParseException {
        super(jobj);

        roomParentId= roomId;
        sectorParentId= sectorId;

        name= jobj.getString(JSON_TAG_NAME);

        airHumidity= jobj.getDouble(JSON_TAG_AIR_HUM);
        aitTemperature= jobj.getDouble(JSON_TAG_AIR_TEMP);
        co2ppm= jobj.getInt(JSON_TAG_CO2_PPM);
        humidWaterLvl= jobj.getInt(JSON_TAG_HUMIDIFIER_WATER_LEVEL);
    }

    public double getAirHumidity() {
        return airHumidity;
    }

    public double getAitTemperature() {
        return aitTemperature;
    }

    public int getCo2ppm(){
        return co2ppm;
    }

    public int getHumidWaterLvl() {
        return humidWaterLvl;
    }

    public String getName(){
        return name;
    }
}
