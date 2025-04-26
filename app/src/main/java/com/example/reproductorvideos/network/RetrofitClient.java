package com.example.reproductorvideos.network;
import com.example.reproductorvideos.model.Video;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

import java.util.List;

public class RetrofitClient {

    private static final String BASE_URL = "http://localhost:3000/api/"; // Ojo: si usas emulador Android no puede ser localhost

    private static Retrofit retrofit = null;

    private static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static ApiService getApiService() {
        return getRetrofitInstance().create(ApiService.class);
    }

    public interface ApiService {
        @GET("videos/{categoria}")
        Call<List<Video>> getVideosByCategory(@Path("categoria") String categoria);
    }
}
