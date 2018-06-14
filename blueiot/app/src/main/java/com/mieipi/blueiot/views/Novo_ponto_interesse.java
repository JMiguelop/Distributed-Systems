package com.mieipi.blueiot.views;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.mieipi.blueiot.DataModels.InterestPoint;
import com.mieipi.blueiot.DataModels.Point;
import com.mieipi.blueiot.LocationFinder;
import com.mieipi.blueiot.R;
import java.util.ArrayList;
import io.realm.Realm;
import io.realm.RealmResults;


public class Novo_ponto_interesse extends AppCompatActivity {
    private ImageView imageView1;
    private ImageView imageView2;
    private ImageView imageView3;
    private ImageView imageView4;
    private RoundImage roundedImage;
    private Bitmap bm;
    private String descricao;
    private Realm realm;

    /* LocationFinder */
    private LocationFinder locationFinder;

    private final BroadcastReceiver exit_from_service = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
            System.exit(0);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
        unregisterReceiver(exit_from_service);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerReceiver(exit_from_service, new IntentFilter("exit_from_service"));

        setContentView(R.layout.activity_novo_ponto_interesse);

        /* Transforma as imagens dos menus (butoes) em circulos */
        setRoudImages();

        realm = Realm.getDefaultInstance();

        /* Inicia o LocationFinder para determinar localização */
        this.locationFinder = new LocationFinder(this);
    }

    private void setRoudImages() {
        this.imageView1 = (ImageView) findViewById(R.id.imageView1);
        this.bm = BitmapFactory.decodeResource(getResources(), R.drawable.image_lazer);
        this.roundedImage = new RoundImage(this.bm);
        this.imageView1.setImageDrawable(this.roundedImage);

        this.imageView2 = (ImageView) findViewById(R.id.imageView2);
        this.bm = BitmapFactory.decodeResource(getResources(), R.drawable.image_work);
        this.roundedImage = new RoundImage(this.bm);
        this.imageView2.setImageDrawable(this.roundedImage);

        this.imageView3 = (ImageView) findViewById(R.id.imageView3);
        this.bm = BitmapFactory.decodeResource(getResources(), R.drawable.image_desporto);
        this.roundedImage = new RoundImage(this.bm);
        this.imageView3.setImageDrawable(this.roundedImage);

        this.imageView4 = (ImageView) findViewById(R.id.imageView4);
        this.bm = BitmapFactory.decodeResource(getResources(), R.drawable.image_alimentacao);
        this.roundedImage = new RoundImage(this.bm);
        this.imageView4.setImageDrawable(this.roundedImage);
    }

    /* Guarda na base de dados a localização de um novo ponto de interesse. Recebe uma string com o tipo de ponto de interesse
     * que se pretende guardar: lazer, work, desporto, alimentacao */
    public void guardar_novo_interesse(ArrayList<String> myLocationAndAddress, String tipoPonto, String descricao) {
        if(myLocationAndAddress != null) {
            double latitude = Double.parseDouble(myLocationAndAddress.get(0));
            double longitude = Double.parseDouble(myLocationAndAddress.get(1));
            String endereco = myLocationAndAddress.get(2);
            Toast.makeText(this, "Latitude: " + latitude + "\nLongitude: " + longitude + "\nEndereço: " + endereco + "\nDescricao: " + descricao, Toast.LENGTH_SHORT).show();

            //Realm realm = Realm.getDefaultInstance();

            RealmResults<InterestPoint> interestPoints = realm.where(InterestPoint.class)
                    .equalTo("category", tipoPonto)
                    .findAll();

            //Caso ainda não exista na base de dados pontos de interesse do tipo pretendido então cria e depois tenta associar endereços
            if(interestPoints.isEmpty()) {
                realm.beginTransaction();

                InterestPoint interestPoint = realm.createObject(InterestPoint.class);
                interestPoint.setCategory(tipoPonto);

                realm.commitTransaction();
            }

            realm.beginTransaction();

            //Procura todos os pontos de interesse do tipo especificado e dado o resultado disso
            //procura todos os pontos com o endereço que se quer inserir.
            RealmResults<InterestPoint> interestPointsWithAddress = realm.where(InterestPoint.class)
                    .equalTo("category", tipoPonto)
                    .findAll()
                    .where()
                    .equalTo("points.endereco", endereco)
                    .findAll();

            //Caso a querie acima dê algum resultado quer dizer que já existe um ponto de interesse do tipo pretendido naquele
            //endereço e como tal não vai voltar a inserir (parte do else). Caso seja vazio então o endereço ainda não tem um ponto de interesse
            //do tipo especificado e então vai inserir na base de dados (parte do if).
            if(interestPointsWithAddress.isEmpty()) {
                Point point = realm.createObject(Point.class);
                point.setLatitude(latitude);
                point.setLongitude(longitude);
                point.setEndereco(endereco);
                point.setDescricao(descricao);
                interestPoints.get(0).getPoints().add(point);

                realm.commitTransaction();
                Toast.makeText(this, "New point of interest " + tipoPonto + " set in your location.", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(this, R.string.point_of_interest_in_address_already_exists, Toast.LENGTH_SHORT).show();
                Toast.makeText(this, "Quantidade de endereços nesta localização: " + interestPointsWithAddress.size(), Toast.LENGTH_SHORT).show();
                realm.cancelTransaction();
            }

            //realm.close();
        }
        else Toast.makeText(this, R.string.no_address, Toast.LENGTH_SHORT).show();
    }

    private void promptDescricao(final String tipoPonto) {
        final ArrayList<String> myLocationAndAddress = this.locationFinder.getMyLocationAndAddress();

        if(myLocationAndAddress != null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Insira uma descrição!");

            // Set up the input
            final EditText input = new EditText(this);

            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
            input.setRawInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            builder.setView(input);
            builder.setCancelable(true);

            // Set up the buttons
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    descricao = input.getText().toString();
                    guardar_novo_interesse(myLocationAndAddress,tipoPonto,descricao);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();
        } else  {
            Toast.makeText(this, R.string.no_address, Toast.LENGTH_SHORT).show();
        }
    }

    public void novo_ponto_interesse_lazer(View view) {
        promptDescricao("Lazer");
    }

    public void novo_ponto_interesse_work(View view) {
        promptDescricao("Trabalho");
    }

    public void novo_ponto_interesse_desporto(View view) {
        promptDescricao("Desporto");
    }

    public void novo_ponto_interesse_alimentacao(View view) {
        promptDescricao("Alimentação");
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.locationFinder.connectGoogleApi();
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.locationFinder.disconnectGoogleApi();
    }
}
