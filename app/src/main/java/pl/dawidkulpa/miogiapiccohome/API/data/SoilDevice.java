package pl.dawidkulpa.miogiapiccohome.API.data;

import com.google.gson.JsonObject;

import java.text.ParseException;

public class SoilDevice extends Device {
    private Plant parent;
    int airHumidity;
    int airTemperature;
    int soilHumidity;
    int batteryLevel;

    public SoilDevice(Plant parent, JsonObject jobj) throws ParseException {
        super(jobj, Type.Soil);
        this.parent= parent;
        airHumidity= jobj.get("AH").getAsInt();
        airTemperature= jobj.get("AT").getAsInt();
        soilHumidity= jobj.get("SH").getAsInt();
        batteryLevel= jobj.get("BTRY").getAsInt();
    }

    public Plant getParent(){
        return parent;
    }

    public int getAirHumidity(){
        return airHumidity;
    }

    public int getAirTemperature(){
        return airTemperature;
    }

    public int getSoilHumidity(){
        return soilHumidity;
    }

    public int getBatteryLevel(){
        return batteryLevel;
    }
}
