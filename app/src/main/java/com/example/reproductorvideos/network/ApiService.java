package com.example.reproductorvideos.network;
import com.example.reproductorvideos.model.Video;


import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import java.util.List;

// Aquí defines tu interface ApiService
public interface ApiService {
    @GET("videos/{categoria}")
    Call<List<Video>> getVideosByCategory(@Path("categoria") String categoria);
}
