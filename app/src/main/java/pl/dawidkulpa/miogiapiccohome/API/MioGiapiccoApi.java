package pl.dawidkulpa.miogiapiccohome.API;

import com.google.gson.JsonObject;

import pl.dawidkulpa.miogiapiccohome.API.requests.ActivationRequest;
import pl.dawidkulpa.miogiapiccohome.API.requests.LoginRequest;
import pl.dawidkulpa.miogiapiccohome.API.requests.RegisterDeviceRequest;
import pl.dawidkulpa.miogiapiccohome.API.requests.RoomCreateRequest;
import pl.dawidkulpa.miogiapiccohome.API.requests.RoomDeleteRequest;
import pl.dawidkulpa.miogiapiccohome.API.requests.SectorCreateRequest;
import pl.dawidkulpa.miogiapiccohome.API.requests.SectorDeleteRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface MioGiapiccoApi {
    @POST("user/login")
    Call<JsonObject> login(@Body LoginRequest body);

    @POST("user/register")
    Call<JsonObject> register(@Body LoginRequest body);

    @POST("user/activate")
    Call<JsonObject> activate(@Body ActivationRequest body);

    @GET("user/get-devices")
    Call<JsonObject> getDevices(@Header("authorization") String token);

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
}