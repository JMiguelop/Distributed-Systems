package com.mieipi.blueiot;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.mieipi.blueiot.DataModels.CommunicationCounter;
import com.mieipi.blueiot.DataModels.CommunicationPoint;
import com.mieipi.blueiot.DataModels.DirectMessage;
import com.mieipi.blueiot.DataModels.Mensagem;
import com.mieipi.blueiot.DataModels.Point;
import com.mieipi.blueiot.DatabaseConverter.DBToFromBytes;
import com.mieipi.blueiot.DatabaseConverter.MensagemDireta;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.realm.Realm;
import io.realm.RealmResults;

import static android.support.v4.app.ActivityCompat.startActivityForResult;

/**
 * Created by nelson on 12-04-2016.
 */
public class blueIotService extends Service {
    private static final int NOTIFICATION_ID = 1;
    protected BluetoothAdapter mBluetoothAdapter;
    protected BroadcastReceiver mReceiver;
    protected IntentFilter filter;
    protected ArrayList<BluetoothDevice> discoveredDevices;  // all discovered devices
    protected ArrayList<BluetoothDevice> validDevices; // devices we connected sucessfully once, but maybe not on range
    protected ArrayList<BluetoothDevice> connectableDevices; // valid devices we now that are on range


    // Static variables
    protected static UUID DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Service variables
    private final IBinder mBinder = new LocalBinder();
    private String TAG = "BlueIOT";                       // tag for logs
    private BroadcastReceiver btReceiver;                 // to capture actions from notification

    private static final long SEARCH_INTERVAL = 30000;    // 5s
    private static final int MAX_TRIES = 5;               // number of attempts to connect to a device

    // Threads
    private AcceptThread acceptor;
    private SearchDevices searchThread;
    ExecutorService executor = Executors.newCachedThreadPool();

    private LocationFinder lf;


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "BlueIOT Service started");

        startSearching(startId);                    // starts searching for devices
        startAcceptor();                            // start listening for connections request

        return START_STICKY;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        this.lf = new LocationFinder(this);
        this.lf.connectGoogleApi();
        registerBtReceiver();            // register broadcastReceiver to capture notification actions
        showNotification();              // this must come after registerReceiver
    }

    private void startAcceptor() {
        acceptor = new AcceptThread();
        executor.submit(acceptor);
    }

    private void startSearching(int startId) {
        searchThread = new SearchDevices(startId);
        executor.submit(searchThread);

    }


    private void registerBtReceiver() {
        btReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                stop();
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("CLOSE_ACTION");
        registerReceiver(btReceiver, filter);
    }


    // TODO: make base notification to reduce duplicated code
    private void showNotification() {
        // stuff for opening app on click
        Intent intentForeground = new Intent(this, Main.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendIntent = PendingIntent.getActivity(getApplicationContext(), 0, intentForeground, 0);

        // stuff for close button
        Intent buttonIntent = new Intent("CLOSE_ACTION");
        PendingIntent btPendingIntent = PendingIntent.getBroadcast(this, 0, buttonIntent, 0);

        Notification.Builder builder = new Notification.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.bluetooth_icon_png)
                .setContentTitle("BlueIOT")
                .setContentText("Clique para abrir aplicação")
                .setContentIntent(pendIntent)
                .setDefaults(Notification.DEFAULT_ALL)
                .setOnlyAlertOnce(false) //vibrate
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Sair", btPendingIntent)
                .setOngoing(false);

        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_FOREGROUND_SERVICE;
        startForeground(NOTIFICATION_ID, notification);
    }

    public void updateNotification(String text, int notificationId) {
        Intent intentForeground = new Intent(this, Main.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendIntent = PendingIntent.getActivity(getApplicationContext(), 0, intentForeground, 0);

        // stuff for close button
        Intent buttonIntent = new Intent("CLOSE_ACTION");
        PendingIntent btPendingIntent = PendingIntent.getBroadcast(this, 0, buttonIntent, 0);

        Notification.Builder builder = new Notification.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.bluetooth_icon_png)
                .setContentTitle("BlueIOT")
                .setContentText("Toque para abrir aplicação")
                .setSubText(text)
                .setTicker(text)
                .setContentIntent(pendIntent)
                .setOnlyAlertOnce(false) //vibrate
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Sair", btPendingIntent)
                .setOngoing(false);

        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_FOREGROUND_SERVICE;

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, notification);
    }

    /**
     * Stops executor
     */
    public void stop() {
        // Tell activity to close itself
        sendBroadcast(new Intent("exit_from_service"));

        // uncomment this to prevent device from remaining discoverable
        //mBluetoothAdapter.disable();

        // close notification
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(NOTIFICATION_ID);

        // close all threads
        acceptor.close();
        searchThread.close();

        executor.shutdownNow(); //Hammer
        stopForeground(true);
        stopSelf();
    }

    public class LocalBinder extends Binder {
        blueIotService getService() {
            return blueIotService.this;
        }
    }

    public ArrayList<BluetoothDevice> getDevices() {
        return connectableDevices;
    }

    public String getBluetoothDeviceName() {
        return mBluetoothAdapter.getName();
    }


    /********** SearchDevices ********/
    /**
     * Thread that keeps searching for bt devices and updating array
     */
    private class SearchDevices extends Thread {
        int service_id;
        boolean flag;

        public SearchDevices(int service_id) {
            discoveredDevices = new ArrayList<>();
            validDevices = new ArrayList<>();
            connectableDevices = new ArrayList<>();

            service_id = service_id;
            flag = true;
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                Log.e("ERROR", "mBluetooth adapter is null");
            }
            init();
         }

        private void init() {
            filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();

                    if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        Log.d(TAG, "SearchDevices: Discovered - " + device.getName());
                        discoveredDevices.add(device);
                        if (validDevices.contains(device) && !connectableDevices.contains(device)) {
                            connectableDevices.add(device);     // we already know this device is connectable, so add it now
                            Log.d(TAG, "SearchDevices: Device - " + device.getName() + " was marked as valid, and now is connectable again");
                        }
                    } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                        discoveredDevices.clear();           // if we start, must clear array
                    } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                        Log.d(TAG, "SearchDevices: Discovery stopped");
                        if (!flag) return;  // onStop()

                        // after a new search has finished we must check if all connectable devices are still in our range
                        for (BluetoothDevice dev : connectableDevices) {
                            if (!discoveredDevices.contains(dev)) {
                                connectableDevices.remove(dev);
                                Log.d(TAG, "SearchDevices: Device - " + dev.getName() + " is not in range, so removing it from connectable");
                            }
                        }

                        /* Try to connect to some devices and send them data */
                        new Thread(new broadCaster()).start();

                        new Handler().postDelayed(new Runnable() { // Maybe remove this pause...
                            @Override
                            public void run() {
                                Log.d(TAG, "SearchDevices: Discovery restarted");
                                mBluetoothAdapter.startDiscovery();
                            }
                        }, SEARCH_INTERVAL);
                    } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                        if (mBluetoothAdapter.getState() == mBluetoothAdapter.STATE_OFF) {
                            Log.e(TAG, "SearchDevices: BT adapter is off");
                        }
                    }
                }
            };
            registerReceiver(mReceiver, filter);
            filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            registerReceiver(mReceiver, filter);
            filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(mReceiver, filter);
            filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mReceiver, filter);
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            startActivity(discoverableIntent);

        }

        @Override
        public void run() {
            mBluetoothAdapter.startDiscovery();
            Log.d(TAG, "SearchDevices: Discovery started");

        }

        private BluetoothDevice getRandomDevice(List<BluetoothDevice> tries) {
            ArrayList<BluetoothDevice> aux = new ArrayList<>();
            for (BluetoothDevice dev : discoveredDevices) {
                if (tries.contains(dev))
                    continue;                      //already tried this device, so skip
                //if (connectableDevices.contains(dev)) continue;   // enable this to connect only once per device
                aux.add(dev);
            }
            if (aux.size() == 0) return null;                      // tried all devices
            return aux.get(new Random().nextInt(aux.size()));   // return random device
        }

        public void close() {
            Log.d(TAG, "SearchDevices: Exiting...");
            flag = false;
            mBluetoothAdapter.cancelDiscovery();
        }

        private class broadCaster implements Runnable {
            List<BluetoothDevice> tries = new ArrayList<>();
            BluetoothSocket mmSocket = null;
            OutputStream mmOutStream;
            InputStream mmInputStream;

            @Override
            public void run() {
                for (int attempt = 0; ((attempt < MAX_TRIES) && flag); attempt++) {
                    BluetoothDevice device = getRandomDevice(tries);
                    if (device == null) {  // randomDevices returns null when there is no more devices
                        tries.clear();  // cleared so we can try again to same device TODO: is this good?
                        break;          // tried all devices or no devices available
                    }
                    try {
                        mmSocket = device.createInsecureRfcommSocketToServiceRecord(DEFAULT_UUID);
                        mmSocket.connect();

                        // if we reach here means connection was sucessfully
                        Log.d(TAG, "Broadcaster: Connected sucessfully to: " + device.getName());
                        mmOutStream = mmSocket.getOutputStream();
                        mmInputStream = mmSocket.getInputStream();

                        // TODO: Add send data code here
                        Log.d(TAG, "Broadcaster: Sending data");
                        DBToFromBytes dbtobytes = new DBToFromBytes();
                        dbtobytes.prepareDataBase();
                        mmOutStream.write(dbtobytes.databaseToBytes());
                        mmOutStream.flush();

                        //TODO: wait for reply on other thread and add timeout maybe a a worker thread that takes care of sending and receiving
                        Log.d(TAG, "Broacaster: Sent message, now waiting for reply");
                        //byte[] buffer = new byte[4096];                                     // buffering received message TODO: is 1024 enough?
                        ObjectInputStream objIn = new ObjectInputStream(mmInputStream);
                        DBToFromBytes dbfrombytes = (DBToFromBytes) objIn.readObject();
                        //int bytes = mmInputStream.read(buffer);
                        //DBToFromBytes dbfrombytes = DBToFromBytes.databaseFromBytes(buffer);
                        dbfrombytes.insertIntoDataBase();


                        //Guarda a localização onde acorreu a comunicação (posição do utilizador)
                        saveMyLocationToDBComunicationPoint();
                        //Actualiza o numero de comunicações que fez com o dispositivo. Recebe como parâmetro o endereço mac do dispositivo.
                        //updateCommunicationCounter(mmSocket.getRemoteDevice().getAddress(), mmSocket.getRemoteDevice().getName());


                        String msg = mmSocket.getRemoteDevice().getName() + " enviou a sua BD!";
                        Log.d(TAG, "Broacaster: Got reply - " + msg);
                        updateNotification(msg, NOTIFICATION_ID);

                        //Actualiza o numero de comunicações que fez com o dispositivo. Recebe como parâmetro o endereço mac do dispositivo.
                        updateCommunicationCounter(mmSocket.getRemoteDevice().getAddress(), mmSocket.getRemoteDevice().getName());

                        Log.d(TAG, "Broacaster: Added device to connectable");
                        if (!validDevices.contains(device))  // register device as a valid device since we can connect to it
                            validDevices.add(device);
                        if (!connectableDevices.contains(device))
                            connectableDevices.add(device);

                        break;          // since we connected sucessfully we exit to while loop

                    } catch (IOException connectException) {
                        Log.d(TAG, "Broadcaster: Connection request failed to: " + device.getName());
                        tries.add(device);   // add device so getRandomDevice dont select him again
                        // we once marked this device as valid, but since we couldn't connect to him anymore
                        // we must remove it from valid and connectable
                        if (validDevices.contains(device)) {
                            validDevices.remove(device);
                            connectableDevices.remove(device);
                            Log.d(TAG, "Broadcaster: Removing valid device, since connection failed: " + device.getName());
                        }
                        try {
                            mmSocket.close();
                        } catch (IOException closeException) {
                            Log.e(TAG, "Broadcaster: Error while closing socket");
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }


    /***** Acceptor ******/
    /**
     * Listens for connections
     */
    private class AcceptThread extends Thread {
        private BluetoothServerSocket mmServerSocket;
        private boolean flag;

        public AcceptThread() {
            Log.d(TAG, "Starting Acceptor");
            flag = true;
            try {
                mmServerSocket = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("BlueIot", DEFAULT_UUID);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }

        public void run() {
            BluetoothSocket socket;
            try {
                while (flag) {
                    Log.d(TAG, "Acceptor: Listening for connections");
                    socket = mmServerSocket.accept();

                    // If a connection was accepted
                    if (socket != null) {
                        Log.d(TAG, "Acceptor: Got connection, starting worker");

                        // TODO: Add here what to do with connection
                        new workerThread(socket).start();
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Acceptor (run) : Got Exception - " + e.getMessage());
            }
        }

        /**
         * Will cancel the listening socket, and cause the thread to finish
         */
        public void close() {
            Log.d(TAG, "Acceptor: Exiting...");
            flag = false;
        }
    }

    /******** Worker *********/
    /**
     * Thread to handle received connection
     */
    private class workerThread extends Thread {
        private final BluetoothSocket mmSocket;
        private InputStream mmInStream;
        private OutputStream mmOutStream;

        public workerThread(BluetoothSocket socket) {
            mmSocket = socket;
            try {
                mmInStream = socket.getInputStream();
                mmOutStream = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Worker: Got Exception - " + e.getMessage());
            }
        }

        /* RECEBER DADOS */
        public void run() {
            int bytes;                                      // number of bytes returned from read()
            //byte[] buffer = new byte[4096];                 // buffering received message TODO: is 1024 enough?
            try {
                Log.d(TAG, "Worker: Listening for data");
                ObjectInputStream objIn = new ObjectInputStream(mmInStream);
                //bytes = mmInStream.read(buffer);            // Read from the InputStream

                // TODO: Put here what to do with message ( deserialize, etc)
                DBToFromBytes dbfrombytes = (DBToFromBytes) objIn.readObject();
                //DBToFromBytes dbfrombytes = DBToFromBytes.databaseFromBytes(buffer);


                if(dbfrombytes.getTipoComunicacao() == DBToFromBytes.communicationType.POINTS) {
                    dbfrombytes.insertIntoDataBase();
                    String msg = mmSocket.getRemoteDevice().getName() + " enviou a sua BD!";
                    //String msg = mmSocket.getRemoteDevice().getName() + " : " + new String(buffer, 0, bytes);
                    //Log.d(TAG, "Worker: Got message - " + msg);
                    updateNotification(msg, NOTIFICATION_ID);

                    Log.d(TAG, "Worker: Sending response...");

                    DBToFromBytes dbtobytes = new DBToFromBytes();
                    dbtobytes.prepareDataBase();
                    mmOutStream.write(dbtobytes.databaseToBytes());
                    //mmOutStream.write("Hello world".getBytes());
                    mmOutStream.flush();

                    //Guarda a localização onde acorreu a comunicação (posição do utilizador)
                    saveMyLocationToDBComunicationPoint();
                    //Actualiza o numero de comunicações que fez com o dispositivo. Recebe como parâmetro o endereço mac do dispositivo.
                    updateCommunicationCounter(mmSocket.getRemoteDevice().getAddress(), mmSocket.getRemoteDevice().getName());

                    // we can now add remote device to connectable and devices
                    if (!connectableDevices.contains(mmSocket.getRemoteDevice()))
                        connectableDevices.add(mmSocket.getRemoteDevice());
                    if (!validDevices.contains(mmSocket.getRemoteDevice()))
                        validDevices.add(mmSocket.getRemoteDevice());

                } else if(dbfrombytes.getTipoComunicacao() == DBToFromBytes.communicationType.DIRECT_MESSAGE) { //Comunicação direta
                    MensagemDireta mensagemRecebida = dbfrombytes.getDirectMessage();

                    saveDirectMessage(mensagemRecebida);
                }

                mmSocket.close(); // one time comunication
            } catch (IOException e) {
                Log.e(TAG, "Worker: Error listening to data from: " + mmSocket.getRemoteDevice().getName() + " : " + e.getMessage());
                try {
                    if (validDevices.contains(mmSocket.getRemoteDevice())) {
                        Log.e(TAG, "Worker: Removing device from trust lists: " + mmSocket.getRemoteDevice().getName());
                        validDevices.remove(mmSocket.getRemoteDevice());
                        connectableDevices.remove(mmSocket.getRemoteDevice());
                    }
                    mmSocket.close();

                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /* Envia uma mensagem de Comunicação Direta para o utilizador destino */
    public void sendDirectMessage(BluetoothDevice device, MensagemDireta msg) {
        new sendDirectMessageThread(device, msg).start();
    }

    private class sendDirectMessageThread extends Thread {
        BluetoothSocket mmSocket = null;
        OutputStream mmOutStream = null;
        BluetoothDevice device;
        MensagemDireta msg;

        public sendDirectMessageThread(BluetoothDevice device, MensagemDireta msg) {
            this.device = device;
            this.msg = msg;
        }

        public void run() {
            try {
                mmSocket = device.createInsecureRfcommSocketToServiceRecord(DEFAULT_UUID);
                mmSocket.connect();

                mmOutStream = mmSocket.getOutputStream();

                DBToFromBytes dbtobytes = new DBToFromBytes(msg);
                mmOutStream.write(dbtobytes.databaseToBytes());
                mmOutStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "DirectCommunicationError - 1");
                try {
                    mmSocket.close();
                } catch (IOException e1) {
                    Log.e(TAG, "DirectCommunicationError - 2");
                }
            } finally {
                try {
                    mmSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "DirectCommunicationError - 2");
                }
            }
        }
    }

    /* Guarda uma mensagem de Comunicação Direta recebida */
    public void saveDirectMessage(MensagemDireta mensagemDireta) {
        Realm realm = Realm.getDefaultInstance();

        //Vai buscar a conversa com o dispositivo que enviou a mensagem
        RealmResults<DirectMessage> mensagensDiretasDB = realm.where(DirectMessage.class)
                .equalTo("userName", mensagemDireta.getSender())
                .findAll();

        if(mensagensDiretasDB.isEmpty()) {
            realm.beginTransaction();

            DirectMessage dm = realm.createObject(DirectMessage.class);
            dm.setUserName(mensagemDireta.getSender());

            Mensagem mensagem = realm.createObject(Mensagem.class);
            mensagem.setSender(mensagemDireta.getSender());
            mensagem.setMensagem(mensagemDireta.getMensagem());

            dm.getMensagens().add(mensagem);

            realm.commitTransaction();
        } else {
            DirectMessage dm = mensagensDiretasDB.get(0);

            realm.beginTransaction();

            Mensagem mensagem = realm.createObject(Mensagem.class);
            mensagem.setSender(mensagemDireta.getSender());
            mensagem.setMensagem(mensagemDireta.getMensagem());

            dm.getMensagens().add(mensagem);

            realm.commitTransaction();
        }

        realm.close();
    }

    public void saveMyLocationToDBComunicationPoint() {
        Location myLocation = this.lf.getMyLocation();

        if(myLocation != null) {
            Log.d(TAG, "LATITUDE: " + String.valueOf(myLocation.getLatitude()));
            Log.d(TAG, "LONGITUDE: " + String.valueOf(myLocation.getLongitude()));

            Realm realm = Realm.getDefaultInstance();

            //Verifica se as coordenadas ainda não existem na base de dados. Caso ainda não existam então coloca a localização na BD, caso contrário ignora.
            RealmResults<CommunicationPoint> communicationPoints = realm.where(CommunicationPoint.class)
                    .equalTo("point.latitude", myLocation.getLatitude())
                    .findAll()
                    .where()
                    .equalTo("point.longitude", myLocation.getLongitude())
                    .findAll();

            if(communicationPoints.isEmpty()) {
                realm.beginTransaction();

                CommunicationPoint cp = realm.createObject(CommunicationPoint.class);

                Point p = realm.createObject(Point.class);
                p.setLatitude(myLocation.getLatitude());
                p.setLongitude(myLocation.getLongitude());

                cp.setPoint(p);

                realm.commitTransaction();
            }
            else Log.d(TAG, "PONTO DE COMUNICAÇÃO JA EXISTE NA BASE DE DADOS !!!");

            realm.close();
        }
    }

    public void updateCommunicationCounter(String macDevice, final String deviceName) {
        Realm realm = Realm.getDefaultInstance();

        //Verifica se o dispositivo ainda não existe na base de dados. Caso ainda não exista então coloca-o e actualiza
        //o contador de comunicação. Caso já exista actualiza apenas o contador de comunicação.
        final RealmResults<CommunicationCounter> communicationCounter = realm.where(CommunicationCounter.class)
                .equalTo("userId", macDevice)
                .findAll();

        if(communicationCounter.isEmpty()) { //Ainda não existe o utilizador na BD
            realm.beginTransaction();

            CommunicationCounter cc = realm.createObject(CommunicationCounter.class);

            cc.setUserId(macDevice);
            cc.setUserName(deviceName);
            cc.setCommunicationCounter(1);

            realm.commitTransaction();
        }
        else { //Utilizador já existe na BD
            int aux = communicationCounter.get(0).getCommunicationCounter();

            realm.beginTransaction();

            communicationCounter.get(0).setCommunicationCounter(aux + 1);

            realm.commitTransaction();

            if((aux + 1) > 0) {
                updateComunicationNotification((aux + 1) + " comunicações com " + deviceName, 2);
            }
        }

        realm.close();
    }

    private void updateComunicationNotification(String s, int notificationId) {
        Notification.Builder mBuilder =
                new Notification.Builder(this)
                        .setSmallIcon(R.drawable.bluetooth_icon_png)
                        .setContentTitle("Nº Comunicações Automáticas")
                        .setContentText(s);

        Intent resultIntent = new Intent(this, Main.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(Main.class);
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);

        Notification notification = mBuilder.build();

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(notificationId, notification);
    }
}