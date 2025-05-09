package com.example.reproductorvideos.network;

import com.example.reproductorvideos.model.Video;
import com.example.reproductorvideos.model.LoginRequest;
import com.example.reproductorvideos.model.LoginResponse;
import com.example.reproductorvideos.model.RegisterRequest;
import com.example.reproductorvideos.model.RegisterResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

    // 🔹 AUTENTICACIÓN
    @POST("login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("register")
    Call<RegisterResponse> register(@Body RegisterRequest request);

    // 🔹 VIDEOS Y CATEGORÍAS

    // Obtener todas las categorías
    @GET("categorias")
    Call<List<String>> getCategoriasDisponibles();


    // Obtener todos los videos
    Call<List<Video>> getVideosByCategory(@Path("categoria") String categoria);

    @GET("videos")
    Call<List<Video>> obtenerVideos();

    // Obtener videos por categoría
    @GET("videos/{categoria}")
    Call<List<Video>> obtenerVideosPorCategoria(@Path("categoria") String categoria);


}
