package pl.dawidkulpa.miogiapiccohome.API.data;

import com.google.gson.JsonObject;

import java.text.ParseException;

public class AirDevice extends Device {
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
    private final int batteryVoltage;

    public AirDevice(JsonObject jobj, int roomId, int sectorId) throws ParseException {
        super(jobj, Type.Air);

        roomParentId= roomId;
        sectorParentId= sectorId;

        name= jobj.get(JSON_TAG_NAME).getAsString();

        airHumidity= jobj.get(JSON_TAG_AIR_HUM).getAsDouble();
        aitTemperature= jobj.get(JSON_TAG_AIR_TEMP).getAsDouble();
        humidWaterLvl= jobj.get(JSON_TAG_HUMIDIFIER_WATER_LEVEL).getAsInt();
        batteryVoltage= jobj.get(JSON_TAG_BATTERY_VOLTAGE).getAsInt();
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

    public int getBatteryVoltage(){ return batteryVoltage;}

    public String getName(){
        return name;
    }
}