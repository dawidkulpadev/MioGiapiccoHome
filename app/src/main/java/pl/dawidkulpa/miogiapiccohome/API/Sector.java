package pl.dawidkulpa.miogiapiccohome.API;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;

public class Sector {
    public static final String JSON_TAG_ID="id";
    private static final String JSON_TAG_NAME="n";
    private static final String JSON_TAG_PLANTS="ps";
    private static final String JSON_TAG_LIGHT_DEVICES="lds";
    private static final String JSON_TAG_AIR_DEVICES="ads";

    private int id;
    private String name;
    private ArrayList<Plant> plants= new ArrayList<>();
    private ArrayList<LightDevice> lightDevices= new ArrayList<>();
    private AirDevice airDevice = null;

    public Sector(JSONObject jobj) throws JSONException, ParseException {
        id= jobj.getInt(JSON_TAG_ID);
        name= jobj.getString(JSON_TAG_NAME);

        JSONArray jplants= jobj.getJSONArray(JSON_TAG_PLANTS);
        JSONArray jlights= jobj.getJSONArray(JSON_TAG_LIGHT_DEVICES);
        JSONArray jairs= jobj.getJSONArray(JSON_TAG_AIR_DEVICES);

        for(int i=0; i<jplants.length(); i++){
            Log.d("Sector "+id, "Parsing Plant "+i+" "+jplants.getJSONObject(i).toString());
            plants.add(new Plant(jplants.getJSONObject(i)));
        }

        for(int i=0; i<jlights.length(); i++){
            Log.d("Sector "+id, "Parsing Light device "+i+" "+jlights.getJSONObject(i).toString());
            lightDevices.add(new LightDevice(jlights.getJSONObject(i), id));
        }

        for(int i=0; i<jairs.length(); i++){
            Log.d("Sector "+id, "Parsing Air device "+i+" "+jairs.getJSONObject(i).toString());
            airDevice= new AirDevice(jairs.getJSONObject(i), Device.DEVICE_NO_PARENT_ID, id);
        }
    }

    public int getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public ArrayList<Plant> getPlants(){
        return plants;
    }
    public ArrayList<LightDevice> getLightDevices(){
        return lightDevices;
    }
    public AirDevice getAirDevice() {return airDevice;}
}
