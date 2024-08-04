package pl.dawidkulpa.miogiapiccohome.API;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

public class LightDevice extends Device{
    private static final String JSON_TAG_NAME="n";
    private static final String JSON_TAG_DLI="dli";
    private static final String JSON_TAG_DS="ds";
    private static final String JSON_TAG_SRD="srd";
    private static final String JSON_TAG_DE="de";
    private static final String JSON_TAG_SSD="ssd";

    public static final int FIELD_DS_ID=1;
    public static final int FIELD_DE_ID=2;
    public static final int FIELD_SSS_ID=3;
    public static final int FIELD_SRE_ID=4;
    public static final int FIELD_SRD_ID=5;
    public static final int FIELD_SSD_ID=6;

    private int sectorParentId;

    private String name;

    private int dli;
    private int dayStartAt;
    private int sunriseEndAt;
    private int sunsetStartAt;
    private int dayEndAt;

    public int getDli() {
        return dli/10;
    }

    public int getDs() {return dayStartAt;}
    public int getDe() {return dayEndAt;}
    public int getSre() {return sunriseEndAt;}
    public int getSss() {return sunsetStartAt;}
    public int getSrd() {return sunriseEndAt-dayStartAt;}
    public int getSsd() {return dayEndAt-sunsetStartAt;}

    public LightDevice(JSONObject jobj, int plantId) throws JSONException, ParseException {
        super(jobj, Type.Light);

        sectorParentId= plantId;
        name= jobj.getString(JSON_TAG_NAME);

        dli = jobj.getInt(JSON_TAG_DLI);

        dayStartAt = jobj.getInt(JSON_TAG_DS);
        sunriseEndAt = dayStartAt + jobj.getInt(JSON_TAG_SRD);
        dayEndAt = jobj.getInt(JSON_TAG_DE);
        sunsetStartAt = dayEndAt - jobj.getInt(JSON_TAG_SSD);
    }

    public String getStringDs(){
        int h= dayStartAt/60;
        int min= dayStartAt%60;

        String textTime=h+":";
        if(min<10)
            textTime+="0";
        textTime+=min;

        return textTime;
    }

    public String getStringDe(){
        int h= dayEndAt/60;
        int min= dayEndAt%60;

        String textTime=h+":";
        if(min<10)
            textTime+="0";
        textTime+=min;

        return textTime;
    }

    public int getSectorParentId(){
        return sectorParentId;
    }

    public String getStringSss() {
        int h= sunsetStartAt/60;
        int min= sunsetStartAt%60;

        String textTime=h+":";
        if(min<10)
            textTime+="0";
        textTime+=min;

        return textTime;
    }

    public String getStringSre() {
        int h= sunriseEndAt/60;
        int min= sunriseEndAt%60;

        String textTime=h+":";
        if(min<10)
            textTime+="0";
        textTime+=min;

        return textTime;
    }

    public void setDli(int dli) {
        this.dli= Math.min(dli, 100)*10;
    }

    public void setTimeOf(int field, int val){
        switch (field){
            case FIELD_DS_ID:
                if(val<=sunriseEndAt)
                    dayStartAt= val;
                break;
            case FIELD_DE_ID:
                if(val>=sunsetStartAt)
                    dayEndAt= val;
                break;
            case FIELD_SSS_ID:
                if(val>sunriseEndAt && val<=dayEndAt)
                    sunsetStartAt= val;
                break;
            case FIELD_SRE_ID:
                if(val>=dayStartAt && val<sunsetStartAt)
                    sunriseEndAt= val;
                break;
        }
    }

    public int getTimeOf(int field){
        switch (field){
            case FIELD_DS_ID:
                return dayStartAt;
            case FIELD_DE_ID:
                return dayEndAt;
            case FIELD_SSS_ID:
                return sunsetStartAt;
            case FIELD_SRE_ID:
                return sunriseEndAt;
            case FIELD_SSD_ID:
                return sunriseEndAt-dayStartAt;
            case FIELD_SRD_ID:
                return dayEndAt-sunsetStartAt;
            default:
                return 0;
        }
    }

    public void setName(String newName){
        name= newName;
    }
    public String getName(){
        return name;
    }

    public void unbind(){
        sectorParentId= -1;
    }
}
