package com.mieipi.blueiot.DataModels;

import io.realm.RealmObject;

/**
 * Created by Miguel on 17/05/16.
 */
public class CommunicationCounter extends RealmObject {
    private String userId; //Endereco MAC do dispositivo
    private String userName; //Nome do bluetooth do dispositivo
    private int communicationCounter; //Numero de comunicacoes efetuadas com o dispositivo



    public String getUserId() {
        return this.userId;
    }

    public void setUserId(String s) {
        this.userId = s;
    }

    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String s) {
        this.userName = s;
    }

    public int getCommunicationCounter() {
        return this.communicationCounter;
    }

    public void setCommunicationCounter(int cc) {
        this.communicationCounter = cc;
    }
}
