package com.example.reproductorvideos.model;

public class LoginRequest {
    private String usuario;
    private String contrasena;
    private String dispositivo_hash; // ✅ NUEVO

    public LoginRequest(String usuario, String contrasena, String dispositivo_hash) {
        this.usuario = usuario;
        this.contrasena = contrasena;
        this.dispositivo_hash = dispositivo_hash;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getContrasena() {
        return contrasena;
    }

    public String getDispositivo_hash() {
        return dispositivo_hash;
    }
}
