package pl.dawidkulpa.miogiapiccohome.API;

import com.google.gson.JsonObject;

import pl.dawidkulpa.miogiapiccohome.API.requests.ActivationRequest;
import pl.dawidkulpa.miogiapiccohome.API.requests.AddPlantRequest;
import pl.dawidkulpa.miogiapiccohome.API.requests.AirDataHistoryRequest;
import pl.dawidkulpa.miogiapiccohome.API.requests.AppSignRequest;
import pl.dawidkulpa.miogiapiccohome.API.requests.DeletePlantRequest;
import pl.dawidkulpa.miogiapiccohome.API.requests.LoginRequest;
import pl.dawidkulpa.miogiapiccohome.API.requests.RegisterDeviceRequest;
import pl.dawidkulpa.miogiapiccohome.API.requests.RoomCreateRequest;
import pl.dawidkulpa.miogiapiccohome.API.requests.RoomDeleteRequest;
import pl.dawidkulpa.miogiapiccohome.API.requests.SectorCreateRequest;
import pl.dawidkulpa.miogiapiccohome.API.requests.SectorDeleteRequest;
import pl.dawidkulpa.miogiapiccohome.API.requests.UnregisterDeviceRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface MioGiapiccoApi {
    @POST("user/login")
    Call<JsonObject> login(@Body LoginRequest body);

    @POST("user/register")
    Call<JsonObject> register(@Body LoginRequest body);

    @POST("user/activate")
    Call<JsonObject> activate(@Body ActivationRequest body);

    @GET("user/get-devices")
    Call<JsonObject> getDevices(@Header("authorization") String token);

    @GET("user/get-app-signature")
    Call<JsonObject> getAppSignature(@Header("authorization") String token, @Query("mac") String mac, @Query("pub") String pubKey);

    @POST("room/create")
    Call<JsonObject> createRoom(@Header("authorization") String token, @Body RoomCreateRequest body);

    @POST("room/delete")
    Call<JsonObject> deleteRoom(@Header("authorization") String token, @Body RoomDeleteRequest body);

    @POST("sector/create")
    Call<JsonObject> createSector(@Header("authorization") String token, @Body SectorCreateRequest body);

    @POST("sector/delete")
    Call<JsonObject> deleteSector(@Header("authorization") String token, @Body SectorDeleteRequest body);

    @POST("user/regenerate-activation-pin")
    Call<JsonObject> regenerateActivationPin(@Body LoginRequest body);

    @POST("device/register")
    Call<JsonObject> registerDevice(@Header("authorization") String token, @Body RegisterDeviceRequest body);

    @POST("device/unregister")
    Call<JsonObject> unregisterDevice(@Header("authorization") String token, @Body UnregisterDeviceRequest body);

    @GET("device/air-history")
    Call<JsonObject> getAirDataHistory(@Header("authorization") String token, @Query("aid") String dev_id, @Query("hs") String hs, @Query("he") String he);

    @POST("plant/create")
    Call<JsonObject> createPlant(@Header("authorization") String token, @Body AddPlantRequest body);

    @POST("plant/delete")
    Call<JsonObject> deletePlant(@Header("authorization") String token, @Body DeletePlantRequest body);
}