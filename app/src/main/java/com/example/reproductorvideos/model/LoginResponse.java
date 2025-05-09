package com.example.reproductorvideos.model;

public class LoginResponse {
    private boolean success;
    private String mensaje;
    private String usuario;
    private String tipo_usuario;

    public boolean isSuccess() {
        return success;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getTipo_usuario() {
        return tipo_usuario;
    }
}
