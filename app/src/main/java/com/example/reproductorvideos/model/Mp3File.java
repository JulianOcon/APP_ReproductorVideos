package com.example.reproductorvideos.model;

public class Mp3File {
    private String titulo;
    private String url;
    private String cover_url; // Este campo debe coincidir exactamente con el JSON
    private String artista;

    public String getTitulo() {
        return titulo;
    }

    public String getUrl() {
        return url;
    }

    public String getCoverUrl() {
        return cover_url; // Este getter puede tener nombre diferente, pero debe devolver cover_url
    }

    public String getArtista() {
        return artista != null ? artista : "Desconocido";
    }
}
