package com.mieipi.blueiot.views;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.mieipi.blueiot.DataModels.InterestPoint;
import com.mieipi.blueiot.DataModels.Point;
import com.mieipi.blueiot.DatabaseConverter.DBToFromBytes;
import com.mieipi.blueiot.R;
import io.realm.Realm;
import io.realm.RealmResults;



public class Localizacao_pontos_interesse extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private String tipoPontoInteresse;
    private Realm realm;


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

        realm = Realm.getDefaultInstance();

        /* Mapa */
        setContentView(R.layout.activity_localizacao_pontos_interesse);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_localizacao_pontos_interesse);
        mapFragment.getMapAsync(this);

        /* Obtem o conteudo passado no intent para esta atividade */
        Intent intent = getIntent();
        this.tipoPontoInteresse = intent.getStringExtra("tipoPontoInteresse");
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        /* This part is to display multiline snippets */
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {

                Context context = getApplicationContext();

                LinearLayout info = new LinearLayout(context);
                info.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(context);
                title.setTextColor(Color.BLACK);
                title.setGravity(Gravity.CENTER);
                title.setTypeface(null, Typeface.BOLD);
                title.setText(marker.getTitle());

                TextView snippet = new TextView(context);
                snippet.setTextColor(Color.GRAY);
                snippet.setText(marker.getSnippet());

                info.addView(title);
                info.addView(snippet);

                return info;
            }
        });
        /**************************************************/


        /* Verifica se tem permissao para apresentar a localização actual do utilizador */
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
        else Toast.makeText(this, "Unable to get your current location", Toast.LENGTH_SHORT).show();

        //Realm realm = Realm.getDefaultInstance();
        RealmResults<InterestPoint> interestPoints = null;

        if(this.tipoPontoInteresse.equals("Todos"))
            interestPoints = realm.where(InterestPoint.class).findAll();
        else
            interestPoints = realm.where(InterestPoint.class).equalTo("category", this.tipoPontoInteresse).findAll();

        if(interestPoints.size() > 0) {
            int number_points = 0;
            LatLng position = null;
            for(InterestPoint ip : interestPoints) {
                for (Point p : ip.getPoints()) {
                    position = new LatLng(p.getLatitude(), p.getLongitude());
                    MarkerOptions marker = new MarkerOptions().position(position).title(ip.getCategory()).snippet(p.getDescricao());
                    switch (ip.getCategory()) {
                        case "Lazer":
                            marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_lazer));
                            break;
                        case "Desporto":
                            marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_sports));
                            break;
                        case "Alimentação":
                            marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_alimentacao));
                            break;
                        case "Trabalho":
                            marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_work));
                            break;
                    }
                    this.mMap.addMarker(marker);
                    number_points++;
                }
            }
            this.mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(41.533357, -8.410872),9),6000 ,null);
            Toast.makeText(this, "Dimensao dos pontos: " + number_points, Toast.LENGTH_SHORT).show();
        }

        //realm.close();
    }
}
