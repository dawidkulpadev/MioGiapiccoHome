package pl.dawidkulpa.miogiapiccohome.API;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String BASE_URL = "https://dawidkulpa.pl/miogiapicco/api/";
    private static Retrofit retrofit = null;

    public static MioGiapiccoApi getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(MioGiapiccoApi.class);
    }
}