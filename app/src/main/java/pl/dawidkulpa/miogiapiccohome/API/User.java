package pl.dawidkulpa.miogiapiccohome.API;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

import pl.dawidkulpa.miogiapiccohome.API.data.AirDataHistory;
import pl.dawidkulpa.miogiapiccohome.API.data.AirDevice;
import pl.dawidkulpa.miogiapiccohome.API.data.Device;
import pl.dawidkulpa.miogiapiccohome.API.data.LightDevice;
import pl.dawidkulpa.miogiapiccohome.API.data.Room;
import pl.dawidkulpa.miogiapiccohome.API.data.Sector;
import pl.dawidkulpa.miogiapiccohome.API.data.UserData;
import pl.dawidkulpa.miogiapiccohome.API.requests.ActivationRequest;
import pl.dawidkulpa.miogiapiccohome.API.requests.AddPlantRequest;
import pl.dawidkulpa.miogiapiccohome.API.requests.AirDataHistoryRequest;
import pl.dawidkulpa.miogiapiccohome.API.requests.AppSignRequest;
import pl.dawidkulpa.miogiapiccohome.API.requests.LoginRequest;
import pl.dawidkulpa.miogiapiccohome.API.requests.RegisterDeviceRequest;
import pl.dawidkulpa.miogiapiccohome.API.requests.RoomCreateRequest;
import pl.dawidkulpa.miogiapiccohome.API.requests.RoomDeleteRequest;
import pl.dawidkulpa.miogiapiccohome.API.requests.SectorCreateRequest;
import pl.dawidkulpa.miogiapiccohome.API.requests.SectorDeleteRequest;
import pl.dawidkulpa.miogiapiccohome.API.requests.UnregisterDeviceRequest;
import pl.dawidkulpa.miogiapiccohome.ble.bleln_encryption.BLELNAuthSecrets;
import pl.dawidkulpa.miogiapiccohome.ble.bleln_encryption.BLELNCert;
import pl.dawidkulpa.miogiapiccohome.ble.bleln_encryption.BLELNEncryption;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class User implements Parcelable {
    private static final String TAG="User";


    public static final int ACTIVATION_SUCCESS=0;
    public static final int ACTIVATION_CODE_EXPIRED =1;
    public static final int ACTIVATION_CODE_INCORRECT=2;
    public static final int ACTIVATION_USER_AUTH_ERROR=3;
    public static final int ACTIVATION_SERVER_ERROR=4;
    public static final int ACTIVATION_NO_CODE=5;
    public static final int ACTIVATION_CONN_ERROR= 6;

    public static final int SIGN_IN_RESULT_SUCCESS=0;
    public static final int SIGN_IN_RESULT_NOT_ACTIVATED=1;
    public static final int SIGN_IN_RESULT_AUTH_ERROR=2;
    public static final int SIGN_IN_RESULT_SERVER_ERROR=3;
    public static final int SIGN_IN_RESULT_CONN_ERROR= 4;

    public static final int SIGN_UP_RESULT_SUCCESS=0;
    public static final int SIGN_UP_RESULT_SERVER_ERROR=1;
    public static final int SIGN_UP_RESULT_ACCOUNT_EXISTS =2;
    public static final int SIGN_UP_RESULT_CONN_ERROR= 3;

    private static final SimpleDateFormat isoDateFormatter;
    static {
        isoDateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        isoDateFormatter.setTimeZone(TimeZone.getDefault());
    }

    public interface SignInUpListener {
        void onFinished(User user, int result);
    }

    public interface ActivationListener {
        void onFinished(int result);
    }

    public interface ActionListener {
        void onFinished(boolean success, JsonObject data);
    }

    public interface DownloadDataListener {
        void onResult(boolean success, UserData userData);
    }

    public interface DownloadAirDataHistoryListener {
        void onResult(boolean success, AirDataHistory airDataHistory);
    }

    private int uid;
    final private String login;
    final private String pass;
    private String token;
    private String picklock;
    private final UserData data;
    private BLELNCert appCert;
    private BLELNAuthSecrets authSecrets;

    private final MioGiapiccoApi api = ApiClient.getClient();

    public User(String login, String pass){
        this.login= login;
        this.pass= pass;
        data= new UserData();
    }

    public UserData getDataHandler(){
        return data;
    }

    /** SIGN IN / UP */
    public void signIn(SignInUpListener listener){
        Call<JsonObject> call = api.login(new LoginRequest(login, pass));

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject body = response.body();
                    token = "Bearer " + body.get("token").getAsString();
                    Log.e("Token", token);
                    uid = body.get("uid").getAsInt();
                    if(listener != null) listener.onFinished(User.this, SIGN_IN_RESULT_SUCCESS);
                } else {
                    int code = response.code();
                    int result = (code == 401) ? SIGN_IN_RESULT_AUTH_ERROR : SIGN_IN_RESULT_SERVER_ERROR;
                    if(listener != null) listener.onFinished(User.this, result);
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                // Błąd połączenia (brak neta, timeout)
                if(listener != null) listener.onFinished(User.this, SIGN_IN_RESULT_CONN_ERROR);
            }
        });
    }

    public void signUp(SignInUpListener listener){
        Call<JsonObject> call = api.register(new LoginRequest(login, pass));

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body()!=null) {
                    JsonObject body= response.body();
                    uid = body.get("uid").getAsInt();
                    picklock= body.get("picklock").getAsString();
                    if(listener != null) listener.onFinished(User.this, SIGN_UP_RESULT_SUCCESS);
                } else {
                    // 409 Conflict - użytkownik istnieje
                    int result = (response.code() == 409) ? SIGN_UP_RESULT_ACCOUNT_EXISTS : SIGN_UP_RESULT_SERVER_ERROR;
                    if(listener != null) listener.onFinished(User.this, result);
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if(listener != null) listener.onFinished(User.this, SIGN_UP_RESULT_CONN_ERROR);
            }
        });
    }

    public void activateAccount(String activationCode, ActivationListener activationListener){
        Call<JsonObject> call = api.activate(new ActivationRequest(login, pass, activationCode));

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    if(activationListener != null)
                        activationListener.onFinished(ACTIVATION_SUCCESS);
                } else {
                    int res;
                    switch (response.code()){
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
                        case 500:
                            res= ACTIVATION_SERVER_ERROR;
                            break;
                        default:
                            res= ACTIVATION_CONN_ERROR;
                    }

                    if(activationListener!=null)
                        activationListener.onFinished(res);
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if(activationListener != null) activationListener.onFinished(ACTIVATION_SERVER_ERROR);
            }
        });
    }

    public void regenerateActivationCode(ActionListener actionListener){
        Call<JsonObject> call = api.regenerateActivationPin(new LoginRequest(login, pass));

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if(actionListener!=null)
                    actionListener.onFinished(response.isSuccessful(), response.body());
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if(actionListener!=null)
                    actionListener.onFinished(false, null);
            }
        });
    }


    public void createRoom(String name, ActionListener listener){
        Call<JsonObject> call = api.createRoom(token, new RoomCreateRequest(name));

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                boolean success = response.isSuccessful(); // Kod 200-299
                if(listener != null) listener.onFinished(success, response.body());
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if(listener != null) listener.onFinished(false, null);
            }
        });
    }

    public void createSector(String name, int roomId, ActionListener actionListener){
        Call<JsonObject> call = api.createSector(token, new SectorCreateRequest(name, roomId));

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if(actionListener !=null)
                    actionListener.onFinished(response.isSuccessful(), response.body());
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if(actionListener != null) actionListener.onFinished(false, null);
            }
        });
    }

    public void createPlant(String name, int secId, ActionListener actionListener){
        Call<JsonObject> call = api.createPlant(token, new AddPlantRequest(name, secId));

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if(actionListener!=null){
                    actionListener.onFinished(response.isSuccessful(), response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                actionListener.onFinished(false, null);
            }
        });
    }

    public void registerDevice(String id, int roomId, int sectorId, int plantId, String name,
                               String devPubKey, Device.Type devType,
                               ActionListener actionListener){
        Call<JsonObject> call = api.registerDevice(
                token, new RegisterDeviceRequest(
                        id,
                        devType.ordinal(),
                        roomId,
                        sectorId,
                        plantId,
                        name,
                        devPubKey));

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                logAPIResponse(response);
                if(actionListener!=null)
                    actionListener.onFinished(response.isSuccessful(), response.body());
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if(actionListener!=null)
                    actionListener.onFinished(false, null);
            }
        });
    }

    public void unregisterDevice(Device device, ActionListener actionListener){
        Call<JsonObject> call = api.unregisterDevice(token,
                new UnregisterDeviceRequest(device.getId(), device.getType().ordinal()));
                call.enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                        if(actionListener!=null)
                            actionListener.onFinished(response.isSuccessful(), response.body());
                    }

                    @Override
                    public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                        if(actionListener!=null)
                            actionListener.onFinished(false, null);
                    }
                }
            );
    }

    public boolean generateAppAuthSecrets() {
        authSecrets= BLELNAuthSecrets.generate();

        return authSecrets!=null;
    }

    public BLELNAuthSecrets getAppAuthSecrets(){
        return authSecrets;
    }

    public void signAppCert(final ActionListener actionListener){
        Random rand = new Random();
        byte[] macAddr = new byte[6];
        rand.nextBytes(macAddr);

        macAddr[0] = (byte)(macAddr[0] & (byte)254);
        macAddr[0] = (byte)(macAddr[0] | (byte)2);

        String macBase64= Base64.encodeToString(macAddr, Base64.NO_WRAP);
        String pubBase64= Base64.encodeToString(authSecrets.getDevPubKey(), Base64.NO_WRAP);

        Call<JsonObject> call = api.getAppSignature(token, macBase64, pubBase64);

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {

                if(response.isSuccessful() && response.body()!=null){
                    Log.e(TAG, response.body().toString());
                    String signBase64= response.body().get("signature").getAsString();
                    String macBase64= response.body().get("mac").getAsString();

                    byte[] certSign= Base64.decode(signBase64, Base64.NO_WRAP);
                    byte[] mac= Base64.decode(macBase64, Base64.NO_WRAP);

                    if(certSign.length!=64 || mac.length!=6 || authSecrets==null){
                        actionListener.onFinished(false, null);
                        return;
                    }

                    appCert= new BLELNCert(2, mac, authSecrets.getDevPubKey(), uid, certSign);
                    actionListener.onFinished(true, response.body());
                } else {
                    actionListener.onFinished(false, null);
                }

            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                actionListener.onFinished(false, null);
            }
        });
    }

    public BLELNCert getAppCert(){
        return appCert;
    }

    public void logAPIResponse(@NonNull Response<JsonObject> response){
        if(response.isSuccessful()) {
            if(response.body()!=null)
                Log.e(TAG, "API response: " + response.body().toString());
        } else {
            try {
                if(response.errorBody()!=null)
                    Log.e(TAG, "API response: "+response.errorBody().string());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void downloadData(final DownloadDataListener ddl){
        Call<JsonObject> call = api.getDevices(token);

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if(response.isSuccessful() && response.body() != null){
                    Log.e("DownloadData", response.body().toString());
                    data.parse(response.body());
                    ddl.onResult(true, data);
                } else {
                    if(ddl != null) ddl.onResult(false, null);
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if(ddl != null) ddl.onResult(false, null);
            }
        });
    }

    public void getAirDataHistory(AirDevice airDevice, @NonNull DownloadAirDataHistoryListener dadhl, Calendar start, Calendar end){
        String strStart = (start != null) ? isoDateFormatter.format(start.getTime()) : null;
        String strEnd = (end != null) ? isoDateFormatter.format(end.getTime()) : null;

        Call<JsonObject> call = api.getAirDataHistory(token, airDevice.getId(), strStart, strEnd);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if(response.isSuccessful() && response.body()!=null){

                    try {
                        AirDataHistory airData= new AirDataHistory(response.body());
                        dadhl.onResult(true, airData);
                    } catch (ParseException e) {
                        dadhl.onResult(false, null);
                    }

                } else {
                    dadhl.onResult(false, null);
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                dadhl.onResult(false, null);
            }
        });
    }

    public void updateRoom(Room r, String newName, ActionListener actionListener){
       /* ServerRequest sr= new ServerRequest(Query.FormatType.Pairs,
                ServerRequest.METHOD_POST,
                ServerRequest.RESPONSE_TYPE_JSON,
                ServerRequest.TIMEOUT_DEFAULT,
                (respCode, jObject) -> {
                    if(actionListener!=null){
                        actionListener.onFinished(respCode==200);
                    }
                });

        sr.addRequestDataPair(Room.JSON_TAG_ID, r.getId());
        sr.addRequestDataPair("login", login);
        sr.addRequestDataPair("pass", pass);

        sr.addRequestDataPair(Room.JSON_TAG_NAME, newName);
        sr.addRequestDataPair(Room.JSON_TAG_HUMIDITY_TARGET, r.getHumidityTarget());

        sr.start(serverAddress+"/user/changedata/room.php");*/
    }

    public void updateSector(Sector s, String newName, ActionListener actionListener){
       /* ServerRequest sr= new ServerRequest(Query.FormatType.Pairs,
                ServerRequest.METHOD_POST,
                ServerRequest.RESPONSE_TYPE_JSON,
                ServerRequest.TIMEOUT_DEFAULT,
                (respCode, jObject) -> {
                    if(actionListener!=null){
                        actionListener.onFinished(respCode==200);
                    }
                });

        sr.addRequestDataPair(Sector.JSON_TAG_ID, s.getId());
        sr.addRequestDataPair("login", login);
        sr.addRequestDataPair("pass", pass);

        sr.addRequestDataPair(Sector.JSON_TAG_NAME, newName);

        sr.start(serverAddress+"/user/changedata/sector.php");*/
    }

    public void deleteRoom(Room r, ActionListener actionListener){
        Call<JsonObject> call = api.deleteRoom(token, new RoomDeleteRequest(r.getId()));

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if(actionListener !=null)
                    actionListener.onFinished(response.isSuccessful(), response.body());
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if(actionListener != null) actionListener.onFinished(false, null);
            }
        });
    }

    public void deleteSector(Sector s, ActionListener actionListener){
        Call<JsonObject> call = api.deleteSector(token, new SectorDeleteRequest(s.getParentRoomId(), s.getId()));

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if(actionListener !=null)
                    actionListener.onFinished(response.isSuccessful(), response.body());
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if(actionListener != null) actionListener.onFinished(false, null);
            }
        });
    }

    public void updateLightDevice(LightDevice d, ActionListener actionListener){
    /*    ServerRequest sr= new ServerRequest(Query.FormatType.Pairs,
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

        sr.addRequestDataPair("si", d.getSectorId());
        sr.addRequestDataPair("n", d.getName());
        sr.addRequestDataPair("dli", d.getDli()*10);
        sr.addRequestDataPair("ds", d.getDs());
        sr.addRequestDataPair("de", d.getDe());
        sr.addRequestDataPair("ssd", d.getSsd());
        sr.addRequestDataPair("srd", d.getSrd());

        sr.start(serverAddress+"/user/changedata/lightdevice.php");*/
    }

    public void markLightDeviceUpgradeAllowed(Device d, ActionListener actionListener){
    /*    ServerRequest sr= new ServerRequest(Query.FormatType.Pairs,
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

        sr.start(serverAddress+"/user/changedata/allowupdate.php");*/
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
        out.writeString(token);
        out.writeInt(uid);
        out.writeString(login);
        out.writeString(pass);
        out.writeString(picklock);
    }

    public static final Creator<User> CREATOR= new Creator<User>() {
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        public User[] newArray(int size) {
            return new User[size];
        }
    };

    private User(Parcel in) {
        token= in.readString();
        uid= in.readInt();
        login= in.readString();
        pass= in.readString();
        picklock= in.readString();
        data= new UserData();
    }
}
