package com.example.reproductorvideos.model;

import com.google.gson.annotations.SerializedName;

public class Video {
    @SerializedName("titulo")
    private String title;

    @SerializedName("url")
    private String url;

    @SerializedName("thumbnail")
    private String thumbnailUrl;

    @SerializedName("fecha")
    private long fecha;

    private boolean esFavorito = false;


    public Video(String title, String url, String thumbnailUrl, long fecha) {
        this.title = title;
        this.url = url;
        this.thumbnailUrl = thumbnailUrl;
        this.fecha = fecha;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public long getFecha() {
        return fecha;
    }

    public boolean isFavorito() {
        return esFavorito;
    }

    public void setFavorito(boolean favorito) {
        this.esFavorito = favorito;
    }

}
