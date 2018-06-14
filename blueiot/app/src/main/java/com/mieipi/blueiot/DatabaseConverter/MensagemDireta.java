package com.mieipi.blueiot.DatabaseConverter;

import java.io.Serializable;

/**
 * Created by Miguel on 14/06/16.
 */
public class MensagemDireta implements Serializable {
    private String sender;      //Nome de quem enviou a mensagem (utilizar o nome do dispositivo bluetooth)
    private String mensagem;    //Corpo da mensagem



    public MensagemDireta(String sender, String mensagem) {
        this.sender = sender;
        this.mensagem = mensagem;
    }

    public String getSender() {
        return this.sender;
    }

    public String getMensagem() {
        return this.mensagem;
    }
}
