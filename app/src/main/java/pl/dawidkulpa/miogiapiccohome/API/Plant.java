package pl.dawidkulpa.miogiapiccohome.API;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

public class Plant {
    private final int id;
    private String name;

    private LightDevice lightDevice=null;
    private SensorDevice sensorDevice=null;

    private boolean showingDetails= false;

    public Plant(JSONObject jobj) throws JSONException, ParseException {
        this.id= jobj.getInt("id");
        this.name= jobj.getString("name");

        if(!jobj.isNull("ld")){
            lightDevice= new LightDevice(this, jobj.getJSONObject("ld"));
        }

        if(!jobj.isNull("sd")){
            sensorDevice= new SensorDevice(this, jobj.getJSONObject("sd"));
        }
    }

    public void update(Plant other){
        if(other.id==this.id){
            this.name= other.name;
            this.lightDevice= other.lightDevice;
            this.sensorDevice= other.sensorDevice;
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

    public int getLastLastSeen() {
        int ldLastSeen=Integer.MAX_VALUE;
        int sdLastSeen=Integer.MAX_VALUE;

        if(lightDevice!=null)
            ldLastSeen= lightDevice.getLastSeen();

        if(sensorDevice!=null)
            sdLastSeen= sensorDevice.getLastSeen();

        if(lightDevice==null && sensorDevice==null)
            return -1;

        return Integer.min(ldLastSeen, sdLastSeen);
    }

    public LightDevice getLightDevice(){
        return lightDevice;
    }

    public SensorDevice getSensorDevice(){
        return sensorDevice;
    }

    public boolean hasAnyDevice(){
        return (lightDevice!=null) || (sensorDevice!=null);
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
