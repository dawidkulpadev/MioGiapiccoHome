package pl.dawidkulpa.miogiapiccohome.API;

import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

public class Plant {
    private static final String JSON_TAG_ID="id";
    private static final String JSON_TAG_NAME="n";
    private static final String JSON_TAG_SOIL_DEVICE="sd";

    private final int id;
    private String name;

    private SoilDevice soilDevice =null;

    private boolean showingDetails= false;

    public Plant(JSONObject jobj) throws JSONException, ParseException {
        this.id= jobj.getInt(JSON_TAG_ID);
        this.name= jobj.getString(JSON_TAG_NAME);

        if(!jobj.isNull(JSON_TAG_SOIL_DEVICE)){
            Log.e("Plant "+id, "Parsing Soil device "+jobj.getJSONObject(JSON_TAG_SOIL_DEVICE).toString());
            soilDevice = new SoilDevice(this, jobj.getJSONObject(JSON_TAG_SOIL_DEVICE));
        }
    }

    public void toggleShowingDetails() {
        showingDetails = !showingDetails;
    }

    public boolean isShowingDetails() {
        return showingDetails;
    }

    public int getId(){return id;}
    public void setName(String n){this.name= n;}
    public String getName(){
        return name;
    }

    public SoilDevice getSoilDevice(){
        return soilDevice;
    }

    public boolean hasAnyDevice(){
        return (soilDevice !=null);
    }


    @Override
    public int hashCode() {
        return this.id;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj instanceof Plant)
            return this.id==((Plant)obj).id;
        else
            return false;
    }
}
