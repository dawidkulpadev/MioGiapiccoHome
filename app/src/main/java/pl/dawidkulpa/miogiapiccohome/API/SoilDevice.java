package pl.dawidkulpa.miogiapiccohome.API;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

public class SoilDevice extends Device {
    private Plant parent;
    int airHumidity;
    int airTemperature;
    int soilHumidity;
    int batteryLevel;

    public SoilDevice(Plant parent, JSONObject jobj) throws JSONException, ParseException {
        super(jobj);
        this.parent= parent;
        airHumidity= jobj.getInt("AH");
        airTemperature= jobj.getInt("AT");

        soilHumidity= jobj.getInt("SH");

        batteryLevel= jobj.getInt("BTRY");
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
