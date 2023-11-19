package pl.dawidkulpa.miogiapiccohome.API;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;

public class UserData {
    private ArrayList<Room> rooms= new ArrayList<>();
    private ArrayList<Device> looseDevices;

    boolean parse(JSONObject jobj){
        // TODO: Dump data update! Make smart data update

        rooms.clear();

        try {
            JSONArray jarr= jobj.getJSONArray("rooms");

            for (int i = 0; i < jarr.length(); i++) {
                JSONObject jRoomObj= jarr.getJSONObject(i);
                rooms.add(new Room(jRoomObj));
            }

            return true;
        } catch (JSONException | ParseException e){
            return false;
        }
    }
}
