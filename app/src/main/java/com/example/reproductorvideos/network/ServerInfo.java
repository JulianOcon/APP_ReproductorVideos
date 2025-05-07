package com.example.reproductorvideos.network;

import com.google.gson.annotations.SerializedName;

public class ServerInfo {
    @SerializedName("ip_publica")
    private String ipPublica;

    @SerializedName("url_publica")
    private String urlPublica;

    public String getIpPublica() {
        return ipPublica;
    }

    public String getUrlPublica() {
        return urlPublica;
    }
}
