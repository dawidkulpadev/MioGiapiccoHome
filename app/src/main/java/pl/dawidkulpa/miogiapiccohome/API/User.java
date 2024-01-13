package pl.dawidkulpa.miogiapiccohome.API;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;

import pl.dawidkulpa.scm.Query;
import pl.dawidkulpa.scm.ServerRequest;

public class User implements Parcelable {
    public interface SignInUpListener {
        void onFinished(User user, boolean success);
    }

    public interface ActionListener {
        void onFinished(boolean success);
    }

    public interface DownloadDataListener {
        void onResult(boolean success, UserData userData);
    }

    private int uid;
    final private String login;
    final private String pass;
    private String picklock;
    private UserData data;

    public User(String login, String pass){
        this.login= login;
        this.pass= pass;
        data= new UserData();
    }

    public UserData getDataHandler(){
        return data;
    }

    /** SIGN IN / UP */
    public void signIn(SignInUpListener signInUpListener){
        ServerRequest sr= new ServerRequest(Query.FormatType.Pairs,
                ServerRequest.METHOD_POST,
                ServerRequest.RESPONSE_TYPE_JSON,
                ServerRequest.TIMEOUT_DEFAULT,
                (respCode, jObject) -> onSignInUpFinished(respCode, jObject, signInUpListener));

        sr.addRequestDataPair("login", login);
        sr.addRequestDataPair("pass", pass);

        sr.start("https://dawidkulpa.pl/apis/miogiapicco/user/signin.php");
    }

    public void signUp(SignInUpListener signInUpListener){
        ServerRequest sr= new ServerRequest(Query.FormatType.Pairs,
                ServerRequest.METHOD_POST,
                ServerRequest.RESPONSE_TYPE_JSON,
                ServerRequest.TIMEOUT_DEFAULT,
                (respCode, jObject) -> onSignInUpFinished(respCode, jObject, signInUpListener));

        sr.addRequestDataPair("login", login);
        sr.addRequestDataPair("pass", pass);

        sr.start("https://dawidkulpa.pl/apis/miogiapicco/user/signup.php");
    }

    public void onSignInUpFinished(int respCode, JSONObject jObject, SignInUpListener signInUpListener){
        if(signInUpListener !=null){
            if(respCode==200) {
                try {
                    uid = jObject.getInt("uid");
                    picklock= jObject.getString("picklock");
                    signInUpListener.onFinished(this,true);
                } catch (JSONException e){
                    signInUpListener.onFinished(this,false);
                }
            } else
                signInUpListener.onFinished(this,false);
        }

    }


    public void createRoom(String name, ActionListener actionListener){
        ServerRequest sr= new ServerRequest(Query.FormatType.Pairs,
                ServerRequest.METHOD_POST,
                ServerRequest.RESPONSE_TYPE_JSON,
                ServerRequest.TIMEOUT_DEFAULT,
                (respCode, jObject) -> {
                    if(actionListener !=null){
                        Log.e("CODE", String.valueOf(respCode));
                        actionListener.onFinished(respCode == 200);
                    }
                });

        sr.addRequestDataPair("login", login);
        sr.addRequestDataPair("pass", pass);
        sr.addRequestDataPair("name", name);

        sr.start("https://dawidkulpa.pl/apis/miogiapicco/user/createroom.php");
    }
    public void createSector(String name, int roomId, ActionListener actionListener){
        ServerRequest sr= new ServerRequest(Query.FormatType.Pairs,
                ServerRequest.METHOD_POST,
                ServerRequest.RESPONSE_TYPE_JSON,
                ServerRequest.TIMEOUT_DEFAULT,
                (respCode, jObject) -> {
                    if(actionListener !=null){
                        Log.e("CODE", String.valueOf(respCode));
                        actionListener.onFinished(respCode == 200);
                    }
                });

        sr.addRequestDataPair("login", login);
        sr.addRequestDataPair("pass", pass);
        sr.addRequestDataPair("name", name);
        sr.addRequestDataPair("roomid", roomId);

        sr.start("https://dawidkulpa.pl/apis/miogiapicco/user/createsector.php");
    }

    public void createPlant(String name, int secId, ActionListener actionListener){
        ServerRequest sr= new ServerRequest(Query.FormatType.Pairs,
                ServerRequest.METHOD_POST,
                ServerRequest.RESPONSE_TYPE_JSON,
                ServerRequest.TIMEOUT_DEFAULT,
                (respCode, jObject) -> {
                    if(actionListener !=null){
                        Log.e("CODE", String.valueOf(respCode));
                        actionListener.onFinished(respCode == 200);
                    }
                });

        sr.addRequestDataPair("login", login);
        sr.addRequestDataPair("pass", pass);
        sr.addRequestDataPair("name", name);
        sr.addRequestDataPair("secid", secId);

        sr.start("https://dawidkulpa.pl/apis/miogiapicco/user/createplant.php");
    }

    public void registerDevice(String id, int roomId, int sectorId, int plantId, String name,
                               Device.Type devType,
                               ActionListener actionListener){
        ServerRequest sr= new ServerRequest(Query.FormatType.Pairs,
                ServerRequest.METHOD_POST,
                ServerRequest.RESPONSE_TYPE_JSON,
                ServerRequest.TIMEOUT_DEFAULT,
                (respCode, jObject) -> {
                    if(actionListener !=null){
                        actionListener.onFinished(respCode == 200);
                    }
                });

        sr.addRequestDataPair("f", "register");
        if(devType== Device.Type.Soil)
            sr.addRequestDataPair("t", "soil");
        else if(devType==Device.Type.Light)
            sr.addRequestDataPair("t", "light");
        else if(devType==Device.Type.Air)
            sr.addRequestDataPair("t", "air");
        sr.addRequestDataPair("login", login);
        sr.addRequestDataPair("pass", pass);
        sr.addRequestDataPair("id", id);

        sr.addRequestDataPair("roomId", roomId);
        sr.addRequestDataPair("sectorId", sectorId);
        sr.addRequestDataPair("plantId", plantId);
        sr.addRequestDataPair("name", name);

        sr.start("https://dawidkulpa.pl/apis/miogiapicco/user/registerdevice.php");
    }

    public void downloadData(final DownloadDataListener ddl){
        ServerRequest sr= new ServerRequest(Query.FormatType.Pairs,
                ServerRequest.METHOD_POST, ServerRequest.RESPONSE_TYPE_JSON,
                ServerRequest.TIMEOUT_DEFAULT, (respCode, jObject) -> {
                    if(respCode==200) {
                        if (data.parse(jObject)){
                            ddl.onResult(true, data);
                        } else {
                            Log.d("ASD", "ASD2");
                            if(ddl!=null){
                                ddl.onResult(false, null);
                            }
                        }
                    } else {
                        if(ddl!=null){
                            Log.d("User", "downloadData: http response code= "+ respCode);
                            ddl.onResult(false, null);
                        }
                    }
                });

        sr.addRequestDataPair("login", login);
        sr.addRequestDataPair("pass", pass);

        sr.start("https://dawidkulpa.pl/apis/miogiapicco/user/getdata.php");
    }

    public void updateLightDevice(LightDevice d, ActionListener actionListener){
        ServerRequest sr= new ServerRequest(Query.FormatType.Pairs,
                ServerRequest.METHOD_POST,
                ServerRequest.RESPONSE_TYPE_JSON,
                ServerRequest.TIMEOUT_DEFAULT,
                (respCode, jObject) -> {
                    if(actionListener!=null){
                        actionListener.onFinished(respCode==200);
                    }
                });

        sr.addRequestDataPair("id", d.getId());
        sr.addRequestDataPair("login", login);
        sr.addRequestDataPair("pass", pass);

        sr.addRequestDataPair("si", d.getSectorParentId());
        sr.addRequestDataPair("n", d.getName());
        sr.addRequestDataPair("dli", d.getDli()*10);
        sr.addRequestDataPair("ds", d.getDs());
        sr.addRequestDataPair("de", d.getDe());
        sr.addRequestDataPair("ssd", d.getSsd());
        sr.addRequestDataPair("srd", d.getSrd());

        sr.start("https://dawidkulpa.pl/apis/miogiapicco/user/updatelightdevice.php");
    }

    public void updatePlantName(Plant p, ActionListener actionListener){
        ServerRequest sr= new ServerRequest(Query.FormatType.Pairs,
                ServerRequest.METHOD_POST,
                ServerRequest.RESPONSE_TYPE_JSON,
                ServerRequest.TIMEOUT_DEFAULT,
                (respCode, jObject) -> {
                    if(actionListener!=null){
                        actionListener.onFinished(respCode==200);
                    }
                });

        sr.addRequestDataPair("id", p.getId());
        sr.addRequestDataPair("login", login);
        sr.addRequestDataPair("pass", pass);

        sr.addRequestDataPair("Name", p.getName());

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
        data= new UserData();
    }
}
