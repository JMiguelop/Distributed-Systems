package com.mieipi.blueiot;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * Created by Miguel on 03/05/16.
 */

/* Permite obter a localização, mais atualizada e aproximada possível, do utilizador.
 *
 * Pode ser utilizada no resto do trabalho sempre que se queira saber a localização do utilizador.
 *
 * Para utilizar basta que na actividade que pretenda obter a localização se crie um "LocationFinder" passando o "Context" da actividade como parâmetro.
 *
 * Existem 3 possibilidades/funções de obter a localização:
 *      - getMyLocation(): retorna um "Location" que depois se pode retirar os valores de latitude/longitude
 *      - getMyAddress(): retorna uma string com o endereço de rua correspondente à localização
 *      - getMyLocationAndAddress(): retorna um ArrayList<String> com as posições: 0 -> lat, 1 -> long, 2 -> endereço
 *
 * Uma actividade que inicie uma classe deste tipo tem sempre de utilizar a função: disconnectGoogleApi() quando termina (quando entra
 * em pausa também se não for preciso obter a localização em background) caso contrário os pedidos de localização vão continuar mesmo
 * depois da actividade terminar.
 *
 * Ver um exemplo de utilização desta classe na classe: Novo_ponto_interesse.
 * */
public class LocationFinder implements com.google.android.gms.location.LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private Context mContext;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private Location myLocation;
    private String myAddress;
    private ArrayList<String> myLocationAndAddress; //Posições: 0 -> latitude, 1 -> longitude, 2 -> endereço

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000; //Código de pedido a ser enviado ao google play services para resolução de problemas



    /* Construtor */
    public LocationFinder(Context context) {
        this.mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        //Cria o pedido de obtenção da localização
        this.mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(300 * 1000)        // 300 segundos em milisegundos (5 MINUTOS !!!)
                .setFastestInterval(60 * 1000); // 60 segundos em milisegundos (1 MINUTO !!!)

        this.mContext = context;
        this.myLocation = null;
        this.myAddress = null;
    }


    /* Métodos */
    /* Faz a conecção à google API */
    public void connectGoogleApi() {
        this.mGoogleApiClient.connect(); //Ligo à google API
    }

    /* Desconecta da google API e pára os pedidos de obtenção de localização. A actividade que enicie um LocationFinder tem de executar esta função sempre que termine caso contrário os pedidos de localização continuam mesmo depois de a actividade terminar */
    public void disconnectGoogleApi() {
        if(this.mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(this.mGoogleApiClient, this); //Pára os pedidos de obtenção de localização
            this.mGoogleApiClient.disconnect(); //Desligo da google API
            Toast.makeText(this.mContext, R.string.location_services_disconnected, Toast.LENGTH_SHORT).show();
        }
    }

    /* Retorna um "Location" o mais atualizado possível */
    public Location getMyLocation() {
        if (ActivityCompat.checkSelfPermission(this.mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this.mContext, R.string.no_location_permission, Toast.LENGTH_SHORT).show();
            return null;
        }

        Location location = LocationServices.FusedLocationApi.getLastLocation(this.mGoogleApiClient);
        if(location != null) this.myLocation = location;

        return this.myLocation;
    }

    /* Retorna uma string com o valor do endereço que corresponde à localização (latitude e longitude) o mais actualizada possível. Requer uma conecção à internet */
    public String getMyAddress() {
        if (ActivityCompat.checkSelfPermission(this.mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this.mContext, R.string.no_location_permission, Toast.LENGTH_SHORT).show();
            return null;
        }

        List<Address> addresses = null;
        Geocoder gcd = new Geocoder(this.mContext, Locale.getDefault());

        Location location = LocationServices.FusedLocationApi.getLastLocation(this.mGoogleApiClient);
        if(location != null) this.myLocation = location;

        if(this.myLocation != null) {
            try {
                addresses = gcd.getFromLocation(this.myLocation.getLatitude(), this.myLocation.getLongitude(), 1);

                if(!addresses.isEmpty()) {
                    this.myAddress = addresses.get(0).getAddressLine(0) + ", " + addresses.get(0).getAddressLine(1) + ", " + addresses.get(0).getAddressLine(2);
                }
                else Toast.makeText(this.mContext, "Unable to get address.\nLat: " + this.myLocation.getLatitude() + "\nLong: " + this.myLocation.getLongitude() + "\nPlease try again.", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                //Apanhar problemas de network e de I/O
                Toast.makeText(this.mContext, R.string.network_problem, Toast.LENGTH_SHORT).show();
            } catch (IllegalArgumentException illegalArgumentException) {
                //Apanhar problema de valores de latitude e/ou longitude inválidos
                Toast.makeText(this.mContext, R.string.invalid_lat_long, Toast.LENGTH_SHORT).show();
            }
        }

        return this.myAddress;
    }

    /* Retorna um arraylist de string com o conjunto de informação da localização actual. Posição 0 -> latitude; Posição 1 -> longitude; Posição 2 -> endereço. Requer uma conecção à internet */
    public ArrayList<String> getMyLocationAndAddress() {
        if (ActivityCompat.checkSelfPermission(this.mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this.mContext, R.string.no_location_permission, Toast.LENGTH_SHORT).show();
            return null;
        }

        this.myLocationAndAddress = new ArrayList<>();
        List<Address> addresses = null;
        Geocoder gcd = new Geocoder(this.mContext, Locale.getDefault());

        Location location = LocationServices.FusedLocationApi.getLastLocation(this.mGoogleApiClient);
        if (location != null) this.myLocation = location;

        if (this.myLocation != null) {
            try {
                Location auxLocation = this.myLocation; //Salvaguarda as coordenadas da minha localização para evitar que updates da mesma levem a inconsistencia entre latitude/longitude e endereço.
                addresses = gcd.getFromLocation(auxLocation.getLatitude(), auxLocation.getLongitude(), 1);

                if (!addresses.isEmpty()) {
                    this.myAddress = addresses.get(0).getAddressLine(0) + ", " + addresses.get(0).getAddressLine(1) + ", " + addresses.get(0).getAddressLine(2);
                    this.myLocationAndAddress.add(String.valueOf(auxLocation.getLatitude()));
                    this.myLocationAndAddress.add(String.valueOf(auxLocation.getLongitude()));
                    this.myLocationAndAddress.add(this.myAddress);
                } else Toast.makeText(this.mContext, "Unable to get address.\nLat: " + this.myLocation.getLatitude() + "\nLong: " + this.myLocation.getLongitude() + "\nPlease try again.", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                //Apanhar problemas de network e de I/O
                Toast.makeText(this.mContext, R.string.network_problem, Toast.LENGTH_SHORT).show();
            } catch (IllegalArgumentException illegalArgumentException) {
                //Apanhar problema de valores de latitude e/ou longitude inválidos
                Toast.makeText(this.mContext, R.string.invalid_lat_long, Toast.LENGTH_SHORT).show();
            }
        }

        if(this.myLocationAndAddress.size() == 3) return this.myLocationAndAddress;
        else return null;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Toast.makeText(this.mContext, R.string.location_services_connected, Toast.LENGTH_SHORT).show();

        if (ActivityCompat.checkSelfPermission(this.mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this.mContext, R.string.no_location_permission, Toast.LENGTH_SHORT).show();
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(this.mGoogleApiClient, this.mLocationRequest, this); //Inicia os pedidos de obtenção de localização
    }

    @Override
    public void onLocationChanged(Location location) {
        this.myLocation = location;
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution() && this.mContext instanceof Activity) {
            try {
                Activity activity = (Activity) this.mContext;
                connectionResult.startResolutionForResult(activity, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this.mContext, "Location services connection failed with code " + connectionResult.getErrorMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
