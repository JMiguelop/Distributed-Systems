package com.mieipi.blueiot.DataModels;

import io.realm.RealmObject;

/**
 * Created by Miguel on 12/06/16.
 */
public class Mensagem extends RealmObject {
    private String sender;      //Nome de quem enviou a mensagem (utilizar o nome do dispositivo bluetooth)
    private String mensagem;    //Corpo da mensagem



    public String getSender() {
        return this.sender;
    }

    public void setSender(String s) {
        this.sender = s;
    }

    public String getMensagem() {
        return this.mensagem;
    }

    public void setMensagem(String s) {
        this.mensagem = s;
    }
}
