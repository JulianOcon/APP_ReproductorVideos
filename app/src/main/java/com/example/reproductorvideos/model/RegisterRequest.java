package com.example.reproductorvideos.model;

public class RegisterRequest {
    private String nombre;
    private String apellidos;
    private String telefono;
    private String contrasena;
    private String tipo_usuario;

    public RegisterRequest(String nombre, String apellidos, String telefono, String contrasena, String tipo_usuario) {
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.telefono = telefono;
        this.contrasena = contrasena;
        this.tipo_usuario = tipo_usuario;
    }
}
