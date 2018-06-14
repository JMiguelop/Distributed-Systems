package com.mieipi.blueiot.views;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mieipi.blueiot.DataModels.DirectMessage;
import com.mieipi.blueiot.DataModels.Mensagem;
import com.mieipi.blueiot.DatabaseConverter.MensagemDireta;
import com.mieipi.blueiot.Main;
import com.mieipi.blueiot.R;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmBaseAdapter;
import io.realm.RealmList;
import io.realm.RealmResults;

public class Mensagens_Comunicacao_Direta extends AppCompatActivity {
    private String userDestino;                         //Nome do utilizador (nome do bluetooth do dispositivo) para quem a mensagem é enviada
    private BluetoothDevice userDestinoDevice = null;   //Bluetooth device do utilizador destino
    private boolean userEstado;                         //Estado do utilizador. True -> valido, False -> nao valido
    private String myDeviceName = null;                 //Nome do bluetooth do dispositivo (nome do utilizador)
    private ListView mensagensListView;
    private Realm realm;



    private final BroadcastReceiver exit_from_service = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
            System.exit(0);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerReceiver(exit_from_service, new IntentFilter("exit_from_service"));
        setContentView(R.layout.activity_mensagens__comunicacao__direta);

        /* Obtem o conteudo passado no intent para esta actividade */
        Intent intent = getIntent();
        if(intent.getStringExtra("estadoUtilizador").equals("true")) this.userEstado = true;
        else this.userEstado = false;
        this.userDestino = intent.getStringExtra("nomeUtilizador");
        Toast.makeText(this, "Recebi no intent: " + this.userDestino + " - " + this.userEstado, Toast.LENGTH_SHORT).show();

        /* Nome do meu dispositivo */
        this.myDeviceName = Main.myService.getBluetoothDeviceName();

        /* BluetoothDevice do utilizador destino */
        ArrayList<BluetoothDevice> dispositivos = Main.myService.getDevices();
        for(BluetoothDevice bd : dispositivos) {
            if((bd.getName() != null) && (bd.getName().equals(this.userDestino))) {
                this.userDestinoDevice = bd;
                break;
            }
        }

        /* Inicia o Realm */
        realm = Realm.getDefaultInstance();

        /* Inicia a apresentação das mensagens */
        getMensagens();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
        unregisterReceiver(exit_from_service);
    }

    public void getMensagens() {
        RealmList<Mensagem> mensagens;
        RealmResults<DirectMessage> mensagensResultado = realm.where(DirectMessage.class)
                .equalTo("userName", this.userDestino)
                .findAll();

        mensagens = mensagensResultado.get(0).getMensagens();

        this.mensagensListView = (ListView) findViewById(R.id.mensagensListView); //Associa a lista ao id da parte da vista para mostrar a lista

        //Adapter que permite atualizar e apresentar automaticamente a lista das mensagens sempre que uma nova mensagem é recebida/enviada
        final RealmBaseAdapter<Mensagem> adapter = new RealmBaseAdapter<Mensagem>(this.getBaseContext(), mensagens.where().findAll(), true) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = convertView;

                LayoutInflater li;
                li = LayoutInflater.from(parent.getContext());
                view = li.inflate(android.R.layout.simple_list_item_1, null);

                Mensagem mensagem = getItem(position);

                if (mensagem != null) {
                    TextView text = (TextView) view.findViewById(android.R.id.text1);

                    if (text != null) {
                        //Se fui eu que enviei a mensagem
                        if (mensagem.getSender().equals(myDeviceName)) {
                            text.setText(mensagem.getMensagem());
                            text.setGravity(Gravity.CENTER | Gravity.RIGHT);
                            text.setBackgroundColor(getResources().getColor(R.color.mensagemEnviada));
                            text.setTextColor(getResources().getColor(R.color.mensagemEnviadaTexto));
                        } else { //Se foi outro utilizador que enviou a mensagem
                            text.setText(mensagem.getMensagem());
                            text.setBackgroundColor(getResources().getColor(R.color.mensagemRecebida));
                            text.setTextColor(getResources().getColor(R.color.mensagemRecebidaTexto));
                        }
                    }
                } else
                    Toast.makeText(getBaseContext(), "Null !!!", Toast.LENGTH_SHORT).show();

                return view;
            }
        };
        this.mensagensListView.setAdapter(adapter);
    }

    public void enviar_mensagem(View view) {
        EditText editText = (EditText) findViewById(R.id.edit_message);
        String textoMensagem = editText.getText().toString();

        if(this.userEstado == true) { //Caso o utilizador destino esteja como válido então vai buscar o texto da mensagem e envia-a para o destino
            if((this.userDestinoDevice != null) && (this.myDeviceName != null)) {
                //Crio a mensagem a enviar
                MensagemDireta m = new MensagemDireta(this.myDeviceName, textoMensagem);

                /* Acrescento a mensagem à conversa na base de dados */
                RealmResults<DirectMessage> mensagensResultado = realm.where(DirectMessage.class)
                        .equalTo("userName", this.userDestino)
                        .findAll();

                if(mensagensResultado.isEmpty()) {
                    realm.beginTransaction();

                    DirectMessage dm = realm.createObject(DirectMessage.class);
                    dm.setUserName(this.userDestino);

                    Mensagem mensagem = realm.createObject(Mensagem.class);
                    mensagem.setSender(this.myDeviceName);
                    mensagem.setMensagem(textoMensagem);

                    dm.getMensagens().add(mensagem);

                    realm.commitTransaction();

                    /* Envia a mensagem ao utilizador destino */
                    Main.myService.sendDirectMessage(this.userDestinoDevice, m);
                } else {
                    DirectMessage dm = mensagensResultado.get(0);

                    realm.beginTransaction();

                    Mensagem mensagem = realm.createObject(Mensagem.class);
                    mensagem.setSender(this.myDeviceName);
                    mensagem.setMensagem(textoMensagem);

                    dm.getMensagens().add(mensagem);

                    realm.commitTransaction();

                    /* Envia a mensagem ao utilizador destino */
                    Main.myService.sendDirectMessage(this.userDestinoDevice, m);
                }
            } else Toast.makeText(this, "Impossível enviar mensagem: algo correu mal", Toast.LENGTH_SHORT).show();

            //Apaga a mensagem da caixa de inserir a mensagem
            editText.getText().clear();

        } else { //Caso o utilizador destino não esteja disponível não deixa enviar a mensagem
            Toast.makeText(this, "Impossível enviar mensagem: utilizador destino não está disponível.", Toast.LENGTH_LONG).show();
            editText.getText().clear(); //Apaga a mensagem da caixa de inserir a mensagem
        }
    }
}