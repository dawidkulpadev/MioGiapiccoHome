package pl.dawidkulpa.miogiapiccohome.API;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;

import pl.dawidkulpa.scm.Query;
import pl.dawidkulpa.scm.ServerRequest;

public class User implements Parcelable {
    public interface SingInUpListener {
        void onFinished(User user, boolean success);
    }

    public interface RegisterDeviceListener {
        void onFinished(boolean success);
    }

    public interface GetPlantsListener {
        void onResult(boolean success, ArrayList<Plant> plants);
    }

    public interface UpdateDeviceListener {
        void onFinished(boolean success);
    }

    public interface CreatePlantListener {
        void onFinished(boolean success);
    }

    private int uid;
    final private String login;
    final private String pass;
    private String picklock;
    private SingInUpListener singInUpListener;
    private RegisterDeviceListener registerDeviceListener;
    private GetPlantsListener getPlantsListener;
    private UpdateDeviceListener updateDeviceListener;
    private CreatePlantListener createPlantListener;

    private ProgressBar progressBar;

    public User(String login, String pass){
        this.login= login;
        this.pass= pass;
        progressBar= null;
    }

    public void signIn(){
        ServerRequest sr= new ServerRequest(Query.FormatType.Pairs,
                ServerRequest.METHOD_POST,
                ServerRequest.RESPONSE_TYPE_JSON,
                ServerRequest.TIMEOUT_DEFAULT,
                this::onSignInFinished);

        sr.addRequestDataPair("login", login);
        sr.addRequestDataPair("pass", pass);

        if(progressBar!=null){
            progressBar.setVisibility(View.VISIBLE);
        }
        sr.start("https://dawidkulpa.pl/apis/miogiapicco/user/signin.php");
    }

    public void onSignInFinished(int respCode, JSONObject jObject){
        if(singInUpListener !=null){
            if(respCode==200) {
                try {
                    uid = jObject.getInt("uid");
                    picklock= jObject.getString("picklock");
                    singInUpListener.onFinished(this,true);
                } catch (JSONException e){
                    singInUpListener.onFinished(this,false);
                }
            } else
                singInUpListener.onFinished(this,false);
        }

        if(progressBar!=null){
            progressBar.setVisibility(View.GONE);
        }
    }

    public void signUp(){
        ServerRequest sr= new ServerRequest(Query.FormatType.Pairs,
                ServerRequest.METHOD_POST,
                ServerRequest.RESPONSE_TYPE_JSON,
                ServerRequest.TIMEOUT_DEFAULT,
                this::onSignInFinished);

        sr.addRequestDataPair("login", login);
        sr.addRequestDataPair("pass", pass);

        if(progressBar!=null){
            progressBar.setVisibility(View.VISIBLE);
        }
        sr.start("https://dawidkulpa.pl/apis/miogiapicco/user/signup.php");
    }

    public void setProgressBar(ProgressBar progressBar){
        this.progressBar= progressBar;
    }

    public void setRegisterDeviceListener(RegisterDeviceListener registerDeviceListener) {
        this.registerDeviceListener = registerDeviceListener;
    }

    public void setSingInUpListener(SingInUpListener singInUpListener) {
        this.singInUpListener = singInUpListener;
    }

    public void setGetPlantsListener(GetPlantsListener getDevicesListener) {
        this.getPlantsListener = getDevicesListener;
    }

    public void setUpdateDeviceListener(UpdateDeviceListener updateDeviceListener) {
        this.updateDeviceListener = updateDeviceListener;
    }

    public void createPlant(String name, CreatePlantListener createPlantListener){
        ServerRequest sr= new ServerRequest(Query.FormatType.Pairs,
                ServerRequest.METHOD_POST,
                ServerRequest.RESPONSE_TYPE_JSON,
                ServerRequest.TIMEOUT_DEFAULT,
                (respCode, jObject) -> {
                    if(createPlantListener !=null){
                        Log.e("CODE", String.valueOf(respCode));
                        createPlantListener.onFinished(respCode == 200);
                    }
                    if(progressBar!=null){
                        progressBar.setVisibility(View.GONE);
                    }
                });

        sr.addRequestDataPair("login", login);
        sr.addRequestDataPair("pass", pass);
        sr.addRequestDataPair("name", name);

        if(progressBar!=null){
            progressBar.setVisibility(View.VISIBLE);
        }
        sr.start("https://dawidkulpa.pl/apis/miogiapicco/user/createplant.php");
    }

    public void registerDevice(String id, int plantId, String bleName){
        ServerRequest sr= new ServerRequest(Query.FormatType.Pairs,
                ServerRequest.METHOD_POST,
                ServerRequest.RESPONSE_TYPE_JSON,
                ServerRequest.TIMEOUT_DEFAULT,
                (respCode, jObject) -> {
                    if(registerDeviceListener !=null){
                        registerDeviceListener.onFinished(respCode == 200);
                    }
                    if(progressBar!=null){
                        progressBar.setVisibility(View.GONE);
                    }
                });

        sr.addRequestDataPair("f", "register");
        if(bleName.contains("Sensor"))
            sr.addRequestDataPair("t", "sensor");
        else if(bleName.contains("Light"))
            sr.addRequestDataPair("t", "light");
        sr.addRequestDataPair("login", login);
        sr.addRequestDataPair("pass", pass);
        sr.addRequestDataPair("id", id);
        sr.addRequestDataPair("plantId", plantId);

        if(progressBar!=null){
            progressBar.setVisibility(View.VISIBLE);
        }
        sr.start("https://dawidkulpa.pl/apis/miogiapicco/user/registerdevice.php");
    }

    public void getPlantsList(){
        ServerRequest sr= new ServerRequest(Query.FormatType.Pairs, ServerRequest.METHOD_POST,
                ServerRequest.RESPONSE_TYPE_JSON, ServerRequest.TIMEOUT_DEFAULT,
                (respCode, jObject) -> {
                    if(respCode==200) {
                        try {
                            onGetPlantsResponse(jObject.getJSONArray("array"));
                        } catch (JSONException je) {
                            Log.d("ASD", "ASD2");
                            if(getPlantsListener!=null){
                                getPlantsListener.onResult(false, null);
                            }
                        }
                    } else {
                        if(getPlantsListener!=null){
                            Log.d("ASD", "ASD1");
                            getPlantsListener.onResult(false, null);
                        }
                    }
                    if(progressBar!=null){
                        progressBar.setVisibility(View.GONE);
                    }
                });

        sr.addRequestDataPair("f", "getplants");
        sr.addRequestDataPair("login", login);
        sr.addRequestDataPair("pass", pass);

        if(progressBar!=null){
            progressBar.setVisibility(View.VISIBLE);
        }
        sr.start("https://dawidkulpa.pl/apis/miogiapicco/user/getplants.php");
    }

    public void onGetPlantsResponse(JSONArray jarr){
        ArrayList<Plant> plants= new ArrayList<>();

        try {
            for (int i = 0; i < jarr.length(); i++) {
                JSONObject jobj= jarr.getJSONObject(i);
                plants.add(new Plant(jobj));
            }
            if(getPlantsListener!=null){
                getPlantsListener.onResult(true, plants);
            }
        } catch (JSONException | ParseException je){
            if(getPlantsListener!=null){
                getPlantsListener.onResult(false, null);
            }
        }
    }

    public void updateLightDevice(LightDevice d){
        ServerRequest sr= new ServerRequest(Query.FormatType.Pairs,
                ServerRequest.METHOD_POST,
                ServerRequest.RESPONSE_TYPE_JSON,
                ServerRequest.TIMEOUT_DEFAULT,
                (respCode, jObject) -> {
                    if(respCode==200){
                        if(updateDeviceListener!=null){
                            updateDeviceListener.onFinished(true);
                        }
                    } else {
                        if(updateDeviceListener!=null){
                            updateDeviceListener.onFinished(false);
                        }
                    }

                    if(progressBar!=null){
                        progressBar.setVisibility(View.GONE);
                    }
                });

        sr.addRequestDataPair("id", d.getId());
        sr.addRequestDataPair("login", login);
        sr.addRequestDataPair("pass", pass);

        sr.addRequestDataPair("PlantId", d.getParent().getId());
        sr.addRequestDataPair("DLI", d.getDli()*10);
        sr.addRequestDataPair("DS", d.getDs());
        sr.addRequestDataPair("DE", d.getDe());
        sr.addRequestDataPair("SSD", d.getSsd());
        sr.addRequestDataPair("SRD", d.getSrd());

        if(progressBar!=null){
            progressBar.setVisibility(View.VISIBLE);
        }
        sr.start("https://dawidkulpa.pl/apis/miogiapicco/user/updatelightdevice.php");
    }

    public void updatePlantName(Plant p){
        ServerRequest sr= new ServerRequest(Query.FormatType.Pairs,
                ServerRequest.METHOD_POST,
                ServerRequest.RESPONSE_TYPE_JSON,
                ServerRequest.TIMEOUT_DEFAULT,
                (respCode, jObject) -> {
                    if(respCode==200){
                        if(updateDeviceListener!=null){
                            updateDeviceListener.onFinished(true);
                        }
                    } else {
                        if(updateDeviceListener!=null){
                            updateDeviceListener.onFinished(false);
                        }
                    }

                    if(progressBar!=null){
                        progressBar.setVisibility(View.GONE);
                    }
                });

        sr.addRequestDataPair("id", p.getId());
        sr.addRequestDataPair("login", login);
        sr.addRequestDataPair("pass", pass);

        sr.addRequestDataPair("Name", p.getName());

        if(progressBar!=null){
            progressBar.setVisibility(View.VISIBLE);
        }
        sr.start("https://dawidkulpa.pl/apis/miogiapicco/user/updateplant.php");
    }



    public int getUid() {
        return uid;
    }

    public String getPicklock() {
        return picklock;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return pass;
    }

    /** Parcelable implementation */
    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(uid);
        out.writeString(login);
        out.writeString(pass);
        out.writeString(picklock);
    }

    public static final Parcelable.Creator<User> CREATOR= new Parcelable.Creator<User>() {
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        public User[] newArray(int size) {
            return new User[size];
        }
    };

    private User(Parcel in) {
        uid= in.readInt();
        login= in.readString();
        pass= in.readString();
        picklock= in.readString();
        progressBar= null;
    }
}
