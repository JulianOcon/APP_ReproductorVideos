package com.example.reproductorvideos.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Mp3File implements Serializable {

    @SerializedName("titulo")
    private String titulo;

    // JSON "url" → Java: url/ruta interna
    @SerializedName("url")
    private String url;

    // JSON "cover_url" → Java: coverUrl
    @SerializedName("cover")
    private String coverUrl;

    @SerializedName("artista")
    private String artista;

    // Constructor vacío para Gson/Retrofit
    public Mp3File() {}

    // Getter para el título
    public String getTitulo() {
        return titulo;
    }

    // Alias “oficial” para la URL del MP3
    public String getUrl() {
        return url;
    }

    // Alias para compatibilidad con tu Mp3Activity (sigue usando getRuta())
    public String getRuta() {
        return url;
    }

    // URL de la portada
    public String getCoverUrl() {
        return coverUrl;
    }

    // Artista (o “Desconocido” si viene null)
    public String getArtista() {
        return artista != null ? artista : "Desconocido";
    }
}
