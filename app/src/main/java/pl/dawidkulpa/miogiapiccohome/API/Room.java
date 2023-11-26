package pl.dawidkulpa.miogiapiccohome.API;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;

public class Room {
    private static final String JSON_TAG_ID="id";
    private static final String JSON_TAG_NAME="n";
    private static final String JSON_TAG_AIR_DEVICES="ads";
    private static final String JSON_TAG_SECTORS="ss";

    private int id;
    private String name;
    private ArrayList<Sector> sectors= new ArrayList<>();
    private ArrayList<AirDevice> airDevices= new ArrayList<>();

    public Room(JSONObject jObj) throws JSONException, ParseException {
        id= jObj.getInt(JSON_TAG_ID);
        name= jObj.getString(JSON_TAG_NAME);

        JSONArray jAirDevs= jObj.getJSONArray(JSON_TAG_AIR_DEVICES);
        JSONArray jSectors= jObj.getJSONArray(JSON_TAG_SECTORS);

        for(int i=0; i<jAirDevs.length(); i++){
            Log.d("Room "+id, "Parsing Air device "+i+" "+jAirDevs.getJSONObject(i).toString());
            airDevices.add(new AirDevice(jAirDevs.getJSONObject(i), id, Device.DEVICE_NO_PARENT_ID));
        }

        for(int i=0; i<jSectors.length(); i++){
            Log.d("Room "+id, "Parsing sector "+i+" "+jSectors.getJSONObject(i).toString());
            sectors.add(new Sector(jSectors.getJSONObject(i)));
        }
    }

    public int getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public ArrayList<Sector> getSectors(){
        return sectors;
    }

    public ArrayList<AirDevice> getAirDevices(){
        return airDevices;
    }
}
