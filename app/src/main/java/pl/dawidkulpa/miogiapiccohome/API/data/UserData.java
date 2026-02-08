package pl.dawidkulpa.miogiapiccohome.API.data;

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.text.ParseException;
import java.util.ArrayList;

public class UserData {
    private ArrayList<Room> rooms= new ArrayList<>();
    private ArrayList<Device> looseDevices= new ArrayList<>();

    public boolean parse(JsonObject jobj){
        // TODO: Dump data update! Make smart data update
        rooms.clear();

        JsonArray jarr= jobj.getAsJsonArray("rs");

        for (int i = 0; i < jarr.size(); i++) {
            JsonObject jRoomObj= jarr.get(i).getAsJsonObject();
            Log.d("UserData", "Parsing room "+i+" "+jRoomObj.toString());
            try {
                rooms.add(new Room(jRoomObj));
            } catch (ParseException e){
                Log.e("UserData", "Failed parsing room: "+i+" with message: "+ e.getMessage());
            }
        }

        return true;
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
