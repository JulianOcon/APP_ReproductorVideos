package com.example.reproductorvideos.model;

public class LoginRequest {
    private String usuario;
    private String contrasena;

    public LoginRequest(String usuario, String contrasena) {
        this.usuario = usuario;
        this.contrasena = contrasena;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getContrasena() {
        return contrasena;
    }
}