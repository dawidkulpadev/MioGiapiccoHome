package pl.dawidkulpa.miogiapiccohome.API;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import pl.dawidkulpa.scm.Query;
import pl.dawidkulpa.scm.ServerRequest;

public class User implements Parcelable {
    public static final int ACTIVATION_SUCCESS=0;
    public static final int ACTIVATION_CODE_EXPIRED =1;
    public static final int ACTIVATION_CODE_INCORRECT=2;
    public static final int ACTIVATION_USER_AUTH_ERROR=3;
    public static final int ACTIVATION_SERVER_ERROR=4;
    public static final int ACTIVATION_NO_CODE=5;

    public interface SignInUpListener {
        void onFinished(User user, boolean success);
    }

    public interface ActivationListener {
        void onFinished(int result);
    }

    public interface ActionListener {
        void onFinished(boolean success);
    }

    public interface DownloadDataListener {
        void onResult(boolean success, UserData userData);
    }

    private final String serverAddress= "https://dawidkulpa.pl/apis/miogiapicco-dev/";
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

        sr.start(serverAddress+"/user/signin.php");
    }

    public void signUp(SignInUpListener signInUpListener){
        ServerRequest sr= new ServerRequest(Query.FormatType.Pairs,
                ServerRequest.METHOD_POST,
                ServerRequest.RESPONSE_TYPE_JSON,
                ServerRequest.TIMEOUT_DEFAULT,
                (respCode, jObject) -> onSignInUpFinished(respCode, jObject, signInUpListener));

        sr.addRequestDataPair("login", login);
        sr.addRequestDataPair("pass", pass);

        sr.start(serverAddress+"/user/create/account.php");
    }

    public void activateAccount(String activationCode, ActivationListener activationListener){
        ServerRequest sr= new ServerRequest(Query.FormatType.Pairs,
                ServerRequest.METHOD_POST,
                ServerRequest.RESPONSE_TYPE_JSON,
                ServerRequest.TIMEOUT_DEFAULT,
                ((respCode, jObject) -> {
                    int res;

                    switch (respCode){
                        case 200:
                            res= ACTIVATION_SUCCESS;
                            break;
                        case 400:
                            res= ACTIVATION_CODE_EXPIRED;
                            break;
                        case 401:
                            res= ACTIVATION_USER_AUTH_ERROR;
                            break;
                        case 403:
                            res= ACTIVATION_CODE_INCORRECT;
                            break;
                        case 404:
                            res= ACTIVATION_NO_CODE;
                            break;
                        default:
                            res= ACTIVATION_SERVER_ERROR;
                    }

                    if(activationListener!=null)
                        activationListener.onFinished(res);
                }));
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
                        actionListener.onFinished(respCode == 200);
                    }
                });

        sr.addRequestDataPair("login", login);
        sr.addRequestDataPair("pass", pass);
        sr.addRequestDataPair("name", name);

        sr.start(serverAddress+"/user/create/room.php");
    }

    public void createSector(String name, int roomId, ActionListener actionListener){
        ServerRequest sr= new ServerRequest(Query.FormatType.Pairs,
                ServerRequest.METHOD_POST,
                ServerRequest.RESPONSE_TYPE_JSON,
                ServerRequest.TIMEOUT_DEFAULT,
                (respCode, jObject) -> {
                    if(actionListener !=null){
                        actionListener.onFinished(respCode == 200);
                    }
                });

        sr.addRequestDataPair("login", login);
        sr.addRequestDataPair("pass", pass);
        sr.addRequestDataPair("name", name);
        sr.addRequestDataPair("roomid", roomId);

        sr.start(serverAddress+"/user/create/sector.php");
    }

    public void createPlant(String name, int secId, ActionListener actionListener){
        ServerRequest sr= new ServerRequest(Query.FormatType.Pairs,
                ServerRequest.METHOD_POST,
                ServerRequest.RESPONSE_TYPE_JSON,
                ServerRequest.TIMEOUT_DEFAULT,
                (respCode, jObject) -> {
                    if(actionListener !=null){
                        actionListener.onFinished(respCode == 200);
                    }
                });

        sr.addRequestDataPair("login", login);
        sr.addRequestDataPair("pass", pass);
        sr.addRequestDataPair("name", name);
        sr.addRequestDataPair("secid", secId);

        sr.start(serverAddress+"/user/create/plant.php");
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

        sr.start(serverAddress+"/user/create/registerdevice.php");
    }

    public void downloadData(final DownloadDataListener ddl){
        ServerRequest sr= new ServerRequest(Query.FormatType.Pairs,
                ServerRequest.METHOD_POST, ServerRequest.RESPONSE_TYPE_JSON,
                ServerRequest.TIMEOUT_DEFAULT, (respCode, jObject) -> {
                    if(respCode==200) {
                        if (data.parse(jObject)){
                            ddl.onResult(true, data);
                        } else {
                            if(ddl!=null){
                                ddl.onResult(false, null);
                            }
                        }
                    } else {
                        if(ddl!=null){
                            ddl.onResult(false, null);
                        }
                    }
                });

        sr.addRequestDataPair("login", login);
        sr.addRequestDataPair("pass", pass);

        sr.start(serverAddress+"/user/getdata.php");
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

        sr.start(serverAddress+"/user/changedata/lightdevice.php");
    }

    public void markLightDeviceUpgradeAllowed(LightDevice d, ActionListener actionListener){
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

        sr.start(serverAddress+"/user/changedata/allowupdate.php");
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
