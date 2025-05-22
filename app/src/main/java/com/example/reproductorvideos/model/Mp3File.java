package com.example.reproductorvideos.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Mp3File implements Serializable {
    @SerializedName("titulo")
    private String titulo;
    @SerializedName("url")
    private String url;
    @SerializedName("cover")
    private String coverUrl;
    @SerializedName("artista")
    private String artista;

    /** Para cachear el artwork embebido y no volver a extraerlo cada vez */
    private transient byte[] artworkData;

    public Mp3File() {}

    public String getTitulo() {
        return titulo;
    }

    public String getUrl() {
        return url;
    }

    public String getRuta() {
        return url;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public String getArtista() {
        return artista != null ? artista : "Desconocido";
    }

    public void setArtworkData(byte[] data) {
        this.artworkData = data;
    }

    public byte[] getArtworkData() {
        return artworkData;
    }
}
