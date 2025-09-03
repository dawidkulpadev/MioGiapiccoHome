package pl.dawidkulpa.miogiapiccohome.API;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class Device {
    public enum Type {Unknown, Light, Soil, Air};

    public static final int DEVICE_NO_PARENT_ID=-1;

    private static final String JSON_TAG_ID= "id";
    private static final String JSON_TAG_LAST_SEEN= "ls";
    private static final String JSON_TAG_FIRMWARE_VERSION="fwv";
    private static final String JSON_TAG_UPDATE_ALLOWED="ua";
    private static final String JSON_TAG_UPDATE_AVAILABLE="uav";

    public static SimpleDateFormat sqlSDF= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    private final String id;
    private final Calendar lastSeen;
    private final String sv;            // Software version
    private final String hv;            // Hardware version
    private final boolean updateAllowed;
    private final boolean updateAvailable;
    private final Type type;

    public Device(JSONObject jobj, Type t) throws JSONException, ParseException {
        id= jobj.getString(JSON_TAG_ID);
        type= t;

        if(jobj.has(JSON_TAG_FIRMWARE_VERSION)){
            /*
             * Firmware version number - 32 bit number
             * (16-bit)hw_id, (16-bit, 15-0 bits)sw_version
             * Hardware id: (6-bit) hw type, (10-bit) hw type version
             * Software version: (5-bit) sw epoch, (7-bit) sw epoch version, (4-bit) sw epoch version fix
             */
            int fwv= jobj.getInt(JSON_TAG_FIRMWARE_VERSION);

            if(fwv!=0) {
                int hv_code = (fwv & 0xffff0000) >> 16;
                int hw_type = (hv_code & 0x0000fc00) >> 10;
                int hw_version = hv_code & 0x000003ff;

                int sv_code = fwv & 0x0000ffff;
                int sw_epoch = (sv_code & 0x0000f800) >> 11;
                int sw_epoch_version = (sv_code & 0x000007f0) >> 4;
                int sw_epoch_version_fix = sv_code & 0x0000000f;

                hv = hw_type + "." + hw_version;
                sv = sw_epoch + "." + sw_epoch_version + "." + sw_epoch_version_fix;
            } else {
                hv= "";
                sv= "";
            }
        } else {
            hv= "";
            sv= "";
        }

        if(jobj.has(JSON_TAG_UPDATE_ALLOWED)){
            updateAllowed= jobj.getInt(JSON_TAG_UPDATE_ALLOWED)==1;
        } else {
            updateAllowed= false;
        }

        String strLS= jobj.getString(JSON_TAG_LAST_SEEN);
        lastSeen= Calendar.getInstance();
        lastSeen.setTime(Objects.requireNonNull(sqlSDF.parse(strLS)));
        //updateAvailable= (jobj.getInt(JSON_TAG_UPDATE_AVAILABLE)==1);
        updateAvailable= false;
    }

    public Type getType() {
        return type;
    }

    public int getLastSeen() {
        Calendar c= Calendar.getInstance();
        long diff= c.getTimeInMillis()-lastSeen.getTimeInMillis();
        return (int)(diff/60000);
    }

    public String getId() {
        return id;
    }

    public String getSoftwareVersion(){
        return sv;
    }

    public String getHardwareVersion(){
        return hv;
    }

    public boolean isUpdateAllowed(){
        return updateAllowed;
    }

    public boolean isUpdateAvailable(){
        return updateAvailable;
    }
}
