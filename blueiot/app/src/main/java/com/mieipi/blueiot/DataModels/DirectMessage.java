package com.mieipi.blueiot.DataModels;

import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * Created by Miguel on 12/06/16.
 */
public class DirectMessage extends RealmObject {
    //private String userId;                  //Endereco MAC do dispositivo com quem se comunica
    private String userName;                //Nome do bluetooth do dispositivo com quem se comunica
    private RealmList<Mensagem> mensagens;  //Conjunto de mensagens trocadas entre o dispositivo



    /*public String getUserId() {
        return this.userId;
    }*/

    /*public void setUserId(String s) {
        this.userId = s;
    }*/

    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String s) {
        this.userName = s;
    }

    public RealmList<Mensagem> getMensagens() {
        return this.mensagens;
    }

    public void setMensagens(RealmList<Mensagem> mensagens) {
        this.mensagens = mensagens;
    }
}
