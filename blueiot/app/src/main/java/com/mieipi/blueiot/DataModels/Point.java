package com.mieipi.blueiot.DataModels;

import io.realm.RealmObject;

/**
 * Created by Ricardo on 19/04/2016.
 */
public class Point extends RealmObject {
    private double latitude; //Latitude
    private double longitude; //Longitude
    private String endereco; //Endereço correspondente às coordenadas latitude/longitude. Não necessita de ser sempre utilizado.
    private String descricao; //Breve descrição do ponto de interesse


    public double getLatitude() {
        return this.latitude;
    }

    public void setLatitude(double lat) {
        this.latitude = lat;
    }

    public double getLongitude() {
        return this.longitude;
    }

    public void setLongitude(double longi) {
        this.longitude = longi;
    }

    public String getEndereco() {
        return this.endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}
