package pl.dawidkulpa.miogiapiccohome.API;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;

public class Room {
    public static final String JSON_TAG_ID="id";
    public static final String JSON_TAG_NAME="n";
    private static final String JSON_TAG_SECTORS="ss";
    public static final String JSON_TAG_HUMIDITY_TARGET="ht";

    private final int id;
    private final String name;
    private int humidityTarget;
    private final ArrayList<Sector> sectors= new ArrayList<>();

    public Room(JSONObject jObj) throws JSONException, ParseException {
        id= jObj.getInt(JSON_TAG_ID);
        name= jObj.getString(JSON_TAG_NAME);

        humidityTarget= jObj.getInt(JSON_TAG_HUMIDITY_TARGET);

        JSONArray jSectors= jObj.getJSONArray(JSON_TAG_SECTORS);

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

    public int getHumidityTarget(){
        return humidityTarget;
    }

    public void setHumidityTarget(int ht){
        humidityTarget= ht;
    }
}
