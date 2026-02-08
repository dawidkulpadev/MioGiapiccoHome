package pl.dawidkulpa.miogiapiccohome.API.data;

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;

public class Sector {
    public static final String JSON_TAG_ID="id";
    public static final String JSON_TAG_NAME="n";
    private static final String JSON_TAG_PLANTS="ps";
    private static final String JSON_TAG_LIGHT_DEVICES="lds";
    private static final String JSON_TAG_AIR_DEVICES="ads";

    private int id;
    private String name;
    private ArrayList<Plant> plants= new ArrayList<>();
    private ArrayList<LightDevice> lightDevices= new ArrayList<>();
    private AirDevice airDevice = null;
    private int parentRoomId;

    public Sector(JsonObject jobj, int roomId) throws ParseException{
        id= jobj.get(JSON_TAG_ID).getAsInt();
        name= jobj.get(JSON_TAG_NAME).getAsString();
        parentRoomId= roomId;

        JsonArray jplants= jobj.getAsJsonArray(JSON_TAG_PLANTS);
        JsonArray jlights= jobj.getAsJsonArray(JSON_TAG_LIGHT_DEVICES);
        JsonArray jairs= jobj.getAsJsonArray(JSON_TAG_AIR_DEVICES);

        for(int i=0; i<jplants.size(); i++){
            Log.d("Sector "+id, "Parsing Plant "+i+" "+jplants.get(i).toString());
            plants.add(new Plant(jplants.get(i).getAsJsonObject()));
        }

        for(int i=0; i<jlights.size(); i++){
            Log.d("Sector "+id, "Parsing Light device "+i+" "+jlights.get(i).toString());
            lightDevices.add(new LightDevice(jlights.get(i).getAsJsonObject(), parentRoomId, id));
        }

        for(int i=0; i<jairs.size(); i++){
            Log.d("Sector "+id, "Parsing Air device "+i+" "+jairs.get(i).toString());
            airDevice= new AirDevice(jairs.get(i).getAsJsonObject(), Device.DEVICE_NO_PARENT_ID, id);
        }
    }

    public int getId(){
        return id;
    }
    public int getParentRoomId() {return parentRoomId;}

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
