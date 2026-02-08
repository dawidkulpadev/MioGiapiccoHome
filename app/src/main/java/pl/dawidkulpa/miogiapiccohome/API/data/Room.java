package pl.dawidkulpa.miogiapiccohome.API.data;

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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

    public Room(JsonObject jObj) throws ParseException, NumberFormatException {
        id= jObj.get(JSON_TAG_ID).getAsInt();
        name= jObj.get(JSON_TAG_NAME).getAsString();

        humidityTarget= jObj.get(JSON_TAG_HUMIDITY_TARGET).getAsInt();

        JsonArray jSectors= jObj.getAsJsonArray(JSON_TAG_SECTORS);

        for(int i=0; i<jSectors.size(); i++){
            Log.d("Room "+id, "Parsing sector "+i+" "+jSectors.get(i).toString());
            sectors.add(new Sector(jSectors.get(i).getAsJsonObject(), id));
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
