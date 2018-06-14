package com.mieipi.blueiot.DatabaseConverter;

import java.io.Serializable;

/**
 * Created by Miguel on 11/05/16.
 */
public class Ponto implements Serializable {
    private double latitude; //Latitude
    private double longitude; //Longitude
    private String endereco; //Endereço correspondente às coordenadas latitude/longitude. Não necessita de ser sempre utilizado.
    private String descricao; //Descricao acerca do ponto


    public Ponto(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Ponto(double latitude, double longitude, String descricao) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.descricao = descricao;
    }

    public Ponto(double latitude, double longitude, String endereco, String descricao) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.endereco = endereco;
        this.descricao = descricao;
    }

    public String getDescricao() { return this.descricao; }

    public double getLatitude() {
        return this.latitude;
    }

    public double getLongitude() {
        return this.longitude;
    }

    public String getEndereco() {
        return this.endereco;
    }
}
