package pl.dawidkulpa.miogiapiccohome.API;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

public class AirDevice extends Device{
    private static final String JSON_TAG_NAME="n";
    private static final String JSON_TAG_AIR_HUM="ah";
    private static final String JSON_TAG_AIR_TEMP="at";
    private static final String JSON_TAG_HUMIDIFIER_WATER_LEVEL="hwl";
    private static final String JSON_TAG_BATTERY_VOLTAGE="bv";

    private int roomParentId;
    private int sectorParentId;

    private final String name;

    private final double airHumidity;
    private final double aitTemperature;
    private final int humidWaterLvl;
    private final double batteryVoltage;

    public AirDevice(JSONObject jobj, int roomId, int sectorId) throws JSONException, ParseException {
        super(jobj, Type.Air);

        roomParentId= roomId;
        sectorParentId= sectorId;

        name= jobj.getString(JSON_TAG_NAME);

        airHumidity= jobj.getDouble(JSON_TAG_AIR_HUM);
        aitTemperature= jobj.getDouble(JSON_TAG_AIR_TEMP);
        humidWaterLvl= jobj.getInt(JSON_TAG_HUMIDIFIER_WATER_LEVEL);
        batteryVoltage= jobj.getDouble(JSON_TAG_BATTERY_VOLTAGE);
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

    public double getBatteryVoltage(){ return batteryVoltage;}

    public String getName(){
        return name;
    }
}
