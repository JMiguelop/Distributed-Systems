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

public class Pontos_interesse_intermedio extends AppCompatActivity {
    private ImageView imageView1;
    private ImageView imageView2;
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

        setContentView(R.layout.activity_pontos_interesse_intermedio);

        /* Transforma as imagens dos menus (butoes) em circulos */
        setRoudImages();
    }

    private void setRoudImages() {
        this.imageView1 = (ImageView) findViewById(R.id.imageView1);
        this.bm = BitmapFactory.decodeResource(getResources(), R.drawable.image_new_2);
        this.roundedImage = new RoundImage(this.bm);
        this.imageView1.setImageDrawable(this.roundedImage);

        this.imageView2 = (ImageView) findViewById(R.id.imageView2);
        this.bm = BitmapFactory.decodeResource(getResources(), R.drawable.image_interest_point_locations);
        this.roundedImage = new RoundImage(this.bm);
        this.imageView2.setImageDrawable(this.roundedImage);
    }

    public void start_novo_ponto_interesse(View view) {
        Intent intent = new Intent(Pontos_interesse_intermedio.this, Novo_ponto_interesse.class);
        startActivity(intent);
    }

    public void start_localizar_pontos_interesse(View view) {
        Intent intent = new Intent(Pontos_interesse_intermedio.this, Localizar_ponto_interesse.class);
        startActivity(intent);
    }
}
