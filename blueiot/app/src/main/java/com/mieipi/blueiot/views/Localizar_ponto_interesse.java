package com.mieipi.blueiot.views;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.mieipi.blueiot.R;

public class Localizar_ponto_interesse extends AppCompatActivity {
    private ImageView imageView1;
    private ImageView imageView2;
    private ImageView imageView3;
    private ImageView imageView4;
    private ImageView imageView_todos;
    private RoundImage roundedImage;
    private Bitmap bm;

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
        unregisterReceiver(exit_from_service);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerReceiver(exit_from_service, new IntentFilter("exit_from_service"));

        setContentView(R.layout.activity_localizar_ponto_interesse);

        /* Transforma as imagens dos menus (butoes) em circulos */
        setRoundImages();
    }

    private void setRoundImages() {
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

        this.imageView_todos = (ImageView) findViewById(R.id.imageView_todos);
        this.bm = BitmapFactory.decodeResource(getResources(), R.drawable.image_todos);
        this.roundedImage = new RoundImage(this.bm);
        this.imageView_todos.setImageDrawable(this.roundedImage);
    }

    public void localizar_ponto_interesse_lazer(View view) {
        Intent intent = new Intent(Localizar_ponto_interesse.this, Localizacao_pontos_interesse.class);
        intent.putExtra("tipoPontoInteresse", "Lazer");
        startActivity(intent);
    }

    public void localizar_ponto_interesse_work(View view) {
        Intent intent = new Intent(Localizar_ponto_interesse.this, Localizacao_pontos_interesse.class);
        intent.putExtra("tipoPontoInteresse", "Trabalho");
        startActivity(intent);
    }

    public void localizar_ponto_interesse_desporto(View view) {
        Intent intent = new Intent(Localizar_ponto_interesse.this, Localizacao_pontos_interesse.class);
        intent.putExtra("tipoPontoInteresse","Desporto");
        startActivity(intent);
    }

    public void localizar_ponto_interesse_alimentacao(View view) {
        Intent intent = new Intent(Localizar_ponto_interesse.this, Localizacao_pontos_interesse.class);
        intent.putExtra("tipoPontoInteresse", "Alimentação");
        startActivity(intent);
    }

    public void localizar_ponto_interesse_todos(View view) {
        Intent intent = new Intent(Localizar_ponto_interesse.this, Localizacao_pontos_interesse.class);
        intent.putExtra("tipoPontoInteresse", "Todos");
        startActivity(intent);
    }
}
