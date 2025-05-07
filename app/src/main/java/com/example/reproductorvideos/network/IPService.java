package com.example.reproductorvideos.network;

import retrofit2.Call;
import retrofit2.http.GET;

public interface IPService {
    @GET("ip")
    Call<ServerInfo> getServerInfo();
}
