package com.example.reproductorvideos.model;

import com.google.gson.annotations.SerializedName;

public class Video {
    @SerializedName("titulo")
    private String title;  // Asegúrate de tener un campo 'title'
    private String url;    // Ejemplo de otro campo

    // Constructor
    public Video(String title, String url) {
        this.title = title;
        this.url = url;
    }

    // Getter para el título
    public String getTitle() {
        return title;
    }

    // Setter para el título (si lo necesitas)
    public void setTitle(String title) {
        this.title = title;
    }

    // Getter y Setter para otros campos si es necesario
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
