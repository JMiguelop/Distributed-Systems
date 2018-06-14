package com.mieipi.blueiot.views;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.mieipi.blueiot.DataModels.CommunicationPoint;
import com.mieipi.blueiot.R;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmResults;


public class Localizacao_comunicacoes extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private HeatmapTileProvider mProvider;
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

        setContentView(R.layout.activity_localizacao_comunicacoes);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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

        /* Verifica se tem permissao para apresentar a localização actual do utilizador */
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
        else Toast.makeText(this, "Unnable to get your current location", Toast.LENGTH_SHORT).show();

        //Realm realm = Realm.getDefaultInstance();

        RealmResults<CommunicationPoint> points = realm.where(CommunicationPoint.class)
                .findAll();

        ArrayList<LatLng> locations = new ArrayList<>();
        for (CommunicationPoint p : points) {
            LatLng marker = new LatLng(p.getPoint().getLatitude(), p.getPoint().getLongitude());
            locations.add(marker);
        }


        if( !locations.isEmpty() ){
            // Create the gradient.
            int[] colors = {
                    //Color.rgb(255, 0, 0),   // red
                    Color.rgb(173,255,47), //yellowgreen
                    //Color.rgb(102, 225, 0), // green
                    Color.rgb(102, 225, 0) // green
            };

            float[] startPoints = {
                    0.2f, 1f
            };

            Gradient gradient = new Gradient(colors, startPoints);

            mProvider = new HeatmapTileProvider.Builder().data( locations ).gradient(gradient).build();
            mProvider.setRadius( HeatmapTileProvider.DEFAULT_RADIUS * 4 );
            mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
        }

        //realm.close();
    }
}
