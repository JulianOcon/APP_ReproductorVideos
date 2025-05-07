package com.example.reproductorvideos.network;

import com.example.reproductorvideos.model.Video;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {
    // Traer todas las categorías
    @GET("categorias")
    Call<List<String>> getCategoriasDisponibles();

    // Traer TODOS los videos (sin categoría)
    @GET("videos")
    Call<List<Video>> getAllVideos();

    // Traer los videos de una categoría específica
    @GET("videos/{categoria}")
    Call<List<Video>> getVideosByCategory(@Path("categoria") String categoria);
    @GET("videos")
    Call<List<Video>> obtenerVideos();

}

