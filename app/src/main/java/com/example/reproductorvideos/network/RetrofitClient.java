package com.example.reproductorvideos.network;

import android.content.Context;
import android.content.SharedPreferences;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit = null;
    private static String BASE_URL = "http://192.168.1.12:3000/api/";

    /**
     * Permite cambiar la URL base en tiempo de ejecución
     */
    public static void setBaseUrl(String url) {
        if (url != null && !url.isEmpty()) {
            BASE_URL = url.endsWith("/") ? url : url + "/";
            retrofit = null; // Forzar recrear instancia
        }
    }

    /**
     * Crea o retorna la instancia de Retrofit inyectando el token JWT si existe
     */
    private static Retrofit getRetrofitInstance(Context ctx) {
        if (retrofit == null) {
            // Interceptor de logging
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                    .addInterceptor(logging);

            // Leer JWT de SharedPreferences
            SharedPreferences prefs = ctx.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE);
            String token = prefs.getString("jwt_token", null);
            if (token != null) {
                httpClient.addInterceptor(chain -> {
                    Request newRequest = chain.request().newBuilder()
                            .addHeader("Authorization", "Bearer " + token)
                            .build();
                    return chain.proceed(newRequest);
                });
            }

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(httpClient.build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    /**
     * Retorna el servicio de API con autenticación si hay token
     */
    public static ApiService getApiService(Context ctx) {
        return getRetrofitInstance(ctx).create(ApiService.class);
    }
}
