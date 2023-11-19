package pl.dawidkulpa.miogiapiccohome.API;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;

public class Sector {
    private static final String JSON_TAG_ID="id";
    private static final String JSON_TAG_NAME="n";
    private static final String JSON_TAG_PLANTS="ps";
    private static final String JSON_TAG_LIGHT_DEVICES="lds";
    private static final String JSON_TAG_AIR_DEVICES="ads";

    private int id;
    private String name;
    private ArrayList<Plant> plants;
    private ArrayList<LightDevice> lightDevices;
    private ArrayList<AirDevice> airDevices;

    public Sector(JSONObject jobj) throws JSONException, ParseException {
        id= jobj.getInt(JSON_TAG_ID);
        name= jobj.getString(JSON_TAG_NAME);

        JSONArray jplants= jobj.getJSONArray(JSON_TAG_PLANTS);
        JSONArray jlights= jobj.getJSONArray(JSON_TAG_LIGHT_DEVICES);
        JSONArray jairs= jobj.getJSONArray(JSON_TAG_AIR_DEVICES);

        for(int i=0; i<jplants.length(); i++){
            plants.add(new Plant(jobj));
        }

        for(int i=0; i<jlights.length(); i++){
            lightDevices.add(new LightDevice(jobj));
        }

        for(int i=0; i<jplants.length(); i++){
            airDevices.add(new AirDevice(jobj));
        }
    }
}
