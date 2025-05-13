package com.example.reproductorvideos.model;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("mensaje")
    private String mensaje;

    @SerializedName("usuario")
    private String usuario;

    @SerializedName("tipo_usuario")
    private String tipoUsuario;

    @SerializedName("token")
    private String token;

    public boolean isSuccess() {
        return success;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getTipoUsuario() {
        return tipoUsuario;
    }

    public String getToken() {
        return token;
    }
}
