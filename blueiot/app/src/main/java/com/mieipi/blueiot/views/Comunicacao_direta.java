package com.mieipi.blueiot.views;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mieipi.blueiot.DataModels.CommunicationCounter;
import com.mieipi.blueiot.DataModels.DirectMessage;
import com.mieipi.blueiot.Main;
import com.mieipi.blueiot.R;
import java.util.ArrayList;
import io.realm.Realm;
import io.realm.RealmResults;



public class Comunicacao_direta extends AppCompatActivity {
    private ListView availableDevicesListView;
    private ArrayList<BluetoothDevice> dispositivos;
    private Realm realm;
    private int numeroConecoes;



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
        setContentView(R.layout.activity_comunicacao_direta);

        if(Main.myService != null) dispositivos = Main.myService.getDevices();
        realm = Realm.getDefaultInstance();
        numeroConecoes = 1;

        //procurar_dispositivos();
        getDispositivos();
        setClickListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
        unregisterReceiver(exit_from_service);
    }

    public void getDispositivos() {
        //Vai buscar os dispositivos que tenham comunicações iguais ou superiores ao numero de comunicações especificado
        RealmResults<CommunicationCounter> dispositivosComNComunicacoes = realm.where(CommunicationCounter.class)
                .greaterThanOrEqualTo("communicationCounter", numeroConecoes)
                .findAll();

        ArrayList<String> nomeDispositivo = new ArrayList<>();

        for(CommunicationCounter cc : dispositivosComNComunicacoes) {
            nomeDispositivo.add(cc.getUserName());
        }

        this.availableDevicesListView = (ListView) findViewById(R.id.listView); //Associa a lista ao id da parte da vista para mostrar a lista

        //Se foram encontrados dispositivos
        if(!nomeDispositivo.isEmpty()) {
            final ArrayAdapter<String> arrayAdapterAvailableDevices = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, nomeDispositivo) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    TextView text = (TextView) view.findViewById(android.R.id.text1);

                    //Determina o estado (cor) de cada dispositivo encontrado
                    if(estadoDispositivo(getItem(position)) == true) text.setBackgroundColor(getResources().getColor(R.color.comDiretaBack2));
                    else text.setBackgroundColor(getResources().getColor(R.color.comDiretaBack1));

                    return view;
                }
            };
            this.availableDevicesListView.setAdapter(arrayAdapterAvailableDevices);
        }
        else { //Se nao foram encontrados dispositivos
            ArrayAdapter<String> arrayAdapterAvailableDevices = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, 0);
            arrayAdapterAvailableDevices.add("NO DEVICES AVAILABLE TO CONNECT");
            this.availableDevicesListView.setAdapter(arrayAdapterAvailableDevices);
        }
    }

    //Verifica se um dado dispositivo esta valido (pertence a connectableDevices).
    //Retorna TRUE se estiver valido e FALSE caso contrario.
    public boolean estadoDispositivo(String nomeDispositivo) {
        for(BluetoothDevice bd : dispositivos) {
            if((bd.getName() != null) && (bd.getName().equals(nomeDispositivo))) return true;
        }
        return false;
    }

    public void setClickListener() {
        this.availableDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(!parent.getItemAtPosition(position).equals("NO DEVICES AVAILABLE TO CONNECT")) {
                    RealmResults<DirectMessage> mensagensResultado = realm.where(DirectMessage.class)
                            .equalTo("userName", (String)parent.getItemAtPosition(position))
                            .findAll();

                    if(mensagensResultado.isEmpty()) {
                        realm.beginTransaction();

                        DirectMessage dm = realm.createObject(DirectMessage.class);
                        dm.setUserName((String)parent.getItemAtPosition(position));

                        realm.commitTransaction();
                    }

                    Intent intent = new Intent(Comunicacao_direta.this, Mensagens_Comunicacao_Direta.class);
                    if(estadoDispositivo((String)parent.getItemAtPosition(position)) == true) intent.putExtra("estadoUtilizador", "true");
                    else intent.putExtra("estadoUtilizador", "false");
                    intent.putExtra("nomeUtilizador",(String)parent.getItemAtPosition(position));
                    startActivity(intent);
                }
            }
        });
    }
}

