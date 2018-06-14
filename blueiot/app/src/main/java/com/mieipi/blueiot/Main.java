package com.mieipi.blueiot;

import android.app.ActivityManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.mieipi.blueiot.DataModels.CommunicationPoint;
import com.mieipi.blueiot.DataModels.InterestPoint;
import com.mieipi.blueiot.DataModels.Point;
import com.mieipi.blueiot.views.Comunicacao_direta;
import com.mieipi.blueiot.views.Localizacao_comunicacoes;
import com.mieipi.blueiot.views.Pontos_interesse_intermedio;
import com.mieipi.blueiot.views.RoundImage;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class Main extends AppCompatActivity {
    private ImageView imageView1;
    private ImageView imageView2;
    private ImageView imageView3;
    private RoundImage roundedImage;
    private Bitmap bm;

    private String TAG="BlueIOT";
    public static blueIotService myService = null;

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

        setContentView(R.layout.activity_main);
        /* Transforma as imagens dos menus (butoes) em circulos */
        setRoundImages();

        /* Realm Init */
        RealmConfiguration config = new RealmConfiguration.Builder(this).build();
        Realm.setDefaultConfiguration(config);

        DBDefaultData();
    }










    /* Função de teste da base de dados */
    private void DBDefaultData() {

        Realm realm = Realm.getDefaultInstance();

        //PARA PONTOS DE INTERESSE !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        realm.beginTransaction();

        InterestPoint interestPoint = realm.createObject(InterestPoint.class);
        interestPoint.setCategory("Trabalho");

        Point point = realm.createObject(Point.class);
        point.setLatitude(40.807225);
        point.setLongitude(-73.962538);
        //point.setEndereco(endereco);

        interestPoint.getPoints().add(point);

        realm.commitTransaction();

        realm.beginTransaction();

        InterestPoint interestPoint1 = realm.createObject(InterestPoint.class);
        interestPoint1.setCategory("Lazer");

        Point point1 = realm.createObject(Point.class);
        point1.setLatitude(40.804024);
        point1.setLongitude(-73.962505);
        //point.setEndereco(endereco);

        interestPoint1.getPoints().add(point1);

        realm.commitTransaction();

        realm.beginTransaction();

        InterestPoint interestPoint2 = realm.createObject(InterestPoint.class);
        interestPoint2.setCategory("Desporto");

        Point point2 = realm.createObject(Point.class);
        point2.setLatitude(40.811196);
        point2.setLongitude(-73.965423);
        //point.setEndereco(endereco);

        interestPoint2.getPoints().add(point2);

        realm.commitTransaction();

        realm.beginTransaction();

        InterestPoint interestPoint3 = realm.createObject(InterestPoint.class);
        interestPoint3.setCategory("Alimentação");

        Point point3 = realm.createObject(Point.class);
        point3.setLatitude(40.807350);
        point3.setLongitude(-73.964341);
        //point.setEndereco(endereco);

        interestPoint3.getPoints().add(point3);

        realm.commitTransaction();

        realm.beginTransaction();

        InterestPoint interestPoint4 = realm.createObject(InterestPoint.class);
        interestPoint4.setCategory("Lazer");

        Point point4 = realm.createObject(Point.class);
        point4.setLatitude(40.808397);
        point4.setLongitude(-73.962910);
        //point.setEndereco(endereco);

        interestPoint4.getPoints().add(point4);

        realm.commitTransaction();

        realm.beginTransaction();

        InterestPoint interestPoint5 = realm.createObject(InterestPoint.class);
        interestPoint5.setCategory("Alimentação");

        Point point5 = realm.createObject(Point.class);
        point5.setLatitude(40.804409);
        point5.setLongitude(-73.963283);
        //point.setEndereco(endereco);

        interestPoint5.getPoints().add(point5);

        realm.commitTransaction();

        realm.close();
    }











    @Override
    protected void onResume() {
        super.onResume();
        /* Inicia o serviço em foreground */
        startService();
    }

    private void setRoundImages() {
        this.imageView1 = (ImageView) findViewById(R.id.imageView1);
        this.bm = BitmapFactory.decodeResource(getResources(), R.drawable.image_map);
        this.roundedImage = new RoundImage(this.bm);
        this.imageView1.setImageDrawable(this.roundedImage);

        this.imageView2 = (ImageView) findViewById(R.id.imageView2);
        this.bm = BitmapFactory.decodeResource(getResources(), R.drawable.image_interest_points);
        this.roundedImage = new RoundImage(this.bm);
        this.imageView2.setImageDrawable(this.roundedImage);

        this.imageView3 = (ImageView) findViewById(R.id.imageView3);
        this.bm = BitmapFactory.decodeResource(getResources(), R.drawable.image_direct_comunication);
        this.roundedImage = new RoundImage(this.bm);
        this.imageView3.setImageDrawable(this.roundedImage);
    }

    public void start_comunication_points_location(View view) {
        Intent intent = new Intent(Main.this, Localizacao_comunicacoes.class);
        startActivity(intent);
    }

    public void start_pontos_interesse_view(View view) {
        Intent intent = new Intent(Main.this, Pontos_interesse_intermedio.class);
        startActivity(intent);
    }

    public void start_comunicacao_directa_view(View view) {
        if(myService != null) {
            Intent intent = new Intent(Main.this, Comunicacao_direta.class);
            startActivity(intent);
        }
        else Toast.makeText(this, "Service is not running",Toast.LENGTH_SHORT).show();
    }

    public void getDevices() {
        if(myService==null) {  //TODO: Maybe check if executor is running
            Toast.makeText(this, "Service is not running",Toast.LENGTH_SHORT).show();
            return;
        }
        ArrayList<BluetoothDevice> devices = myService.getDevices();
        if(devices.size()==0) {
            Toast.makeText(this, "No devices found, try again later!",Toast.LENGTH_SHORT).show();
            return;
        }
        for (BluetoothDevice dev : devices) {
            Toast.makeText(this,dev.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Check if executor is already connected, if not connects and binds
     */
    private void startService() {
        if(myService!=null) {
            Log.d(TAG,"Main: Service is already connected");
            return;
        }
        if (isMyServiceRunning(blueIotService.class)) {
            Intent intent = new Intent(this, blueIotService.class);
            bindService(intent,mConnection, Context.BIND_AUTO_CREATE);
            Log.d(TAG,"Main: Service is already running");
        } else {
            Intent intent = new Intent(this, blueIotService.class);
            startService(intent);
            bindService(intent,mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            blueIotService.LocalBinder srv= (blueIotService.LocalBinder) service;
            myService = srv.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            myService = null;
        }
    };
}
