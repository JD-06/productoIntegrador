package com.empresa.pos.modelo;

public class Usuario {
    private final String nombre;
    private final String rol; // "ADMIN", "CAJERO", "SUPERVISOR"

    public Usuario(String nombre, String rol) {
        this.nombre = nombre;
        this.rol = rol;
    }

    public String getNombre() { return nombre; }
    public String getRol()    { return rol; }
    public boolean esAdmin()  { return "ADMIN".equals(rol); }
}
