package pl.dawidkulpa.miogiapiccohome.API;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;

public class UserData {
    private ArrayList<Room> rooms= new ArrayList<>();
    private ArrayList<Device> looseDevices= new ArrayList<>();

    public boolean parse(JSONObject jobj){
        // TODO: Dump data update! Make smart data update

        rooms.clear();

        try {
            JSONArray jarr= jobj.getJSONArray("rs");

            for (int i = 0; i < jarr.length(); i++) {
                JSONObject jRoomObj= jarr.getJSONObject(i);
                Log.d("UserData", "Parsing room "+i+" "+jRoomObj.toString());
                rooms.add(new Room(jRoomObj));
            }

            return true;
        } catch (JSONException | ParseException e){
            Log.e("UserData", "parse: "+e.getMessage());
            return false;
        }
    }

    public ArrayList<Room> getRooms(){
        return rooms;
    }

    public ArrayList<Sector> getSectors(int roomId){
        return rooms.get(roomId).getSectors();
    }

    public ArrayList<Plant> getPlants(int roomId, int sectorId){
        return rooms.get(roomId).getSectors().get(sectorId).getPlants();
    }

    public ArrayList<Device> getLooseDevices(){
        return looseDevices;
    }
}
