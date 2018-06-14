package com.mieipi.blueiot.DatabaseConverter;

import android.util.Log;

import com.mieipi.blueiot.DataModels.CommunicationPoint;
import com.mieipi.blueiot.DataModels.InterestPoint;
import com.mieipi.blueiot.DataModels.Mensagem;
import com.mieipi.blueiot.DataModels.Point;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by Miguel on 11/05/16.
 */
public class DBToFromBytes implements Serializable {
    public enum communicationType { POINTS, DIRECT_MESSAGE }

    private String TAG = "BlueIOT";
    private communicationType type; //Tipo de comunicação que vai ser utilizado
    private ArrayList<Ponto> communicationPoints; //Localização dos pontos de comunicação
    private HashMap<String, ArrayList<Ponto>> interestPoints; //Pontos de interesse: categoria e respetivos pontos (localizações)
    private MensagemDireta directMessage; //Mensagem de comunicação direta


    /* Construtores */
    /* Construtor para a comunicação automática */
    public DBToFromBytes() {
        this.type = communicationType.POINTS;
        this.communicationPoints = new ArrayList<>();
        this.interestPoints = new HashMap<>();
    }

    /* Construtor para a comunicação direta */
    public DBToFromBytes(MensagemDireta mensagem) {
        this.type = communicationType.DIRECT_MESSAGE;
        this.directMessage = mensagem;
    }



    /* Métodos */
    public void prepareDataBase() {
        Log.d(TAG, "1 - A preparar a base de dados para enviar...");
        prepareCommunicationPoints();
        prepareInterestPoints();
    }

    public communicationType getTipoComunicacao() {
        return this.type;
    }

    public MensagemDireta getDirectMessage() {
        return this.directMessage;
    }

    /* Prepara os pontos de comunicação para enviar */
    private void prepareCommunicationPoints() {
        Realm realm = Realm.getDefaultInstance();

        RealmResults<CommunicationPoint> pontosComunicação = realm.where(CommunicationPoint.class)
                .findAll();

        if(pontosComunicação.size() != 0) {
            Log.d(TAG, "2 - A preparar os pontos de comunicação...");
            for(CommunicationPoint cp : pontosComunicação) {
                Point point = cp.getPoint();
                Ponto ponto = new Ponto(point.getLatitude(), point.getLongitude());

                this.communicationPoints.add(ponto);
            }
        }
        else Log.d(TAG, "3 - Nao tem pontos de comunicação para enviar !!!");

        realm.close();
    }

    /* Prepara os pontos de interesse para enviar */
    private void prepareInterestPoints() {
        Realm realm = Realm.getDefaultInstance();

        RealmResults<InterestPoint> pontosInterese = realm.where(InterestPoint.class)
                .findAll();

        if(pontosInterese.size() != 0) {
            Log.d(TAG, "4 - A preparar os pontos de interesse...");
            for(InterestPoint ip : pontosInterese) {
                String category = ip.getCategory();

                ArrayList<Ponto> pontosAux = new ArrayList<>();

                if(this.interestPoints.containsKey(category)) {
                    for(Point p : ip.getPoints()) {
                        Ponto pp = new Ponto(p.getLatitude(), p.getLongitude(), p.getEndereco(), p.getDescricao());
                        this.interestPoints.get(category).add(pp);
                    }
                }
                else {
                    for(Point p : ip.getPoints()) {
                        Ponto pp = new Ponto(p.getLatitude(), p.getLongitude(), p.getEndereco(), p.getDescricao());
                        pontosAux.add(pp);
                    }
                    this.interestPoints.put(category, pontosAux);
                }
            }
        }
        else Log.d(TAG, "5 - Nao tem pontos de interesse para enviar !!!");

        realm.close();
    }

    /* ------------------------------------------------------------------------------------------------------------------------- */
    /* ------------------------------------------------------------------------------------------------------------------------- */

    public void insertIntoDataBase() {
        insertCommunicationPoints(); //Insere os pontos de comunicação na BD de acordo com os parãmetros definidos
        insertInterestPoints(); //Insere os pontos de interesse na BD de acordo com os parâmetros definidos
    }

    /* Insere os PONTOS DE COMUNICAÇÃO na base de dados */
    private void insertCommunicationPoints() {
        Realm realm = Realm.getDefaultInstance();

        RealmResults<CommunicationPoint> pontosComunicacao = realm.where(CommunicationPoint.class)
                .findAll();

        if(this.communicationPoints.size() != 0) { //Se foram passados pontos de comunicação
            if(pontosComunicacao.size() != 0) { //Se já tem pontos de comunicação na base de dados
                Log.d(TAG, "6 - Base de dados já possui pontos de comunicação");
                for(Ponto p : this.communicationPoints) {
                    RealmResults<CommunicationPoint> pontoComunicacao = realm.where(CommunicationPoint.class)
                            .equalTo("point.latitude", p.getLatitude())
                            .findAll()
                            .where()
                            .equalTo("point.longitude", p.getLongitude())
                            .findAll();

                    //Se já tiver o ponto na base de dados então não vai inserir.
                    //Se ainda não tiver o ponto então insere.
                    if(pontoComunicacao.size() == 0) {
                        Log.d(TAG, "7 - Ainda nao existe o ponto de comunicacao na base de dados");
                        realm.beginTransaction();

                        CommunicationPoint cp = realm.createObject(CommunicationPoint.class);

                        Point point = realm.createObject(Point.class);
                        point.setLatitude(p.getLatitude());
                        point.setLongitude(p.getLongitude());

                        cp.setPoint(point);

                        realm.commitTransaction();
                        Log.d(TAG, "8 - Novo ponto de comunicação inserido");
                    }
                    else Log.d(TAG, "9 - Ponto de comunicação já existe na base de dados");
                }
            }
            else { //Se ainda não tem pontos de comunicação na base de dados então posso inserir tudo o que recebi sem fazer verificações de conteudo duplicado
                Log.d(TAG, "10 - Base de dados ainda não possui pontos de comunicação");
                for(Ponto p : this.communicationPoints) {
                    realm.beginTransaction();

                    CommunicationPoint cp = realm.createObject(CommunicationPoint.class);

                    Point point = realm.createObject(Point.class);
                    point.setLatitude(p.getLatitude());
                    point.setLongitude(p.getLongitude());

                    cp.setPoint(point);

                    realm.commitTransaction();
                    Log.d(TAG, "11 - Novo ponto de comunicação inserido");
                }
            }
        }

        realm.close();
    }

    /* Insere os PONTOS DE INTERESSE na base de dados */
    private void insertInterestPoints() {
        Realm realm = Realm.getDefaultInstance();

        RealmResults<InterestPoint> pontosInterese = realm.where(InterestPoint.class)
                .findAll();

        if(this.interestPoints.size() != 0) { //Se foram passados pontos de interesse
            if(pontosInterese.size() != 0) { //Se já tem pontos de interesse na base de dados
                Log.d(TAG, "12 - Base de dados já possui pontos de interesse");
                for(String s : this.interestPoints.keySet()) {
                    RealmResults<InterestPoint> pontosInteresseCategoria = realm.where(InterestPoint.class)
                            .equalTo("category", s)
                            .findAll();

                    //Se já tem a categoria na base de dados então verifica se pode inserir os pontos no RealmList de points.
                    //Só pode inserir se aquele ponto ainda não existir naquele endereço para aquela categoria.
                    if(pontosInteresseCategoria.size() != 0) {
                        Log.d(TAG, "13 - Categoria ja existe na base de dados");
                        for(Ponto ponto : this.interestPoints.get(s)) {
                            RealmResults<InterestPoint> pontosInteresseCategoriaPonto = realm.where(InterestPoint.class)
                                    .equalTo("category", s)
                                    .findAll()
                                    .where()
                                    .equalTo("points.endereco", ponto.getEndereco())
                                    .findAll();

                            //Se estiver vazio então ainda não tem nenhum ponto de interesse, da categoria especificada (s), no endereço
                            //especificado (ponto.getEndereco()) logo vai inserir na base de dados.
                            //Caso o resultado não seja vazio é porque já existe um ponto de enteresse, da categoria especificada,
                            //no endereço especificado, logo, não vai voltar a inserir esse ponto de interesse.
                            if(pontosInteresseCategoriaPonto.isEmpty()) {
                                realm.beginTransaction();

                                Point point = realm.createObject(Point.class);
                                point.setLatitude(ponto.getLatitude());
                                point.setLongitude(ponto.getLongitude());
                                point.setEndereco(ponto.getEndereco());
                                if(ponto.getDescricao() == null) {
                                    point.setDescricao("");
                                }
                                else point.setDescricao(ponto.getDescricao());
                                //point.setDescricao(ponto.getDescricao());

                                pontosInteresseCategoria.get(0).getPoints().add(point);

                                realm.commitTransaction();
                                Log.d(TAG, "14 - Novo ponto de interesse inserido");
                            }
                            else Log.d(TAG, "15 - Ja existe um ponto de interesse na localizacao pretendida da mesma categoria");
                        }
                    }
                    else { //Se ainda não tiver a categoria na base de dados então insere a categoria e os respetivos points no RealmList. Que agora não é preciso verificar se já existem.
                        Log.d(TAG, "16 - Categoria ainda nao existe na base de dados");
                        realm.beginTransaction();

                        InterestPoint interestPoint = realm.createObject(InterestPoint.class);
                        interestPoint.setCategory(s);

                        realm.commitTransaction();

                        Log.d(TAG, "17 - Categoria inserida");

                        RealmResults<InterestPoint> pontosInteresseCategoria2 = realm.where(InterestPoint.class)
                                .equalTo("category", s)
                                .findAll();

                        if(pontosInteresseCategoria2.size() != 0) {
                            for(Ponto ponto : this.interestPoints.get(s)) {
                                realm.beginTransaction();

                                Point point = realm.createObject(Point.class);
                                point.setLatitude(ponto.getLatitude());
                                point.setLongitude(ponto.getLongitude());
                                point.setEndereco(ponto.getEndereco());
                                if(ponto.getDescricao() == null) {
                                    point.setDescricao("");
                                }
                                else point.setDescricao(ponto.getDescricao());
                                //point.setDescricao(ponto.getDescricao());

                                pontosInteresseCategoria2.get(0).getPoints().add(point);

                                realm.commitTransaction();
                                Log.d(TAG, "18 - Novo ponto de interesse inserido");
                            }
                        }
                    }
                }
            }
            else { //Se ainda não tem pontos de interesse na base de dados então posso inserir tudo o que recebi sem fazer verificações de conteudo duplicado
                Log.d(TAG, "19 - Base de dados ainda nao tem pontos de interesse");
                for(String s : this.interestPoints.keySet()) {
                    realm.beginTransaction();

                    InterestPoint interestPoint = realm.createObject(InterestPoint.class);
                    interestPoint.setCategory(s);

                    realm.commitTransaction();

                    Log.d(TAG, "20 - Categoria inserida");

                    RealmResults<InterestPoint> pontosInteresseCategoria3 = realm.where(InterestPoint.class)
                            .equalTo("category", s)
                            .findAll();

                    if(pontosInteresseCategoria3.size() != 0) {
                        for(Ponto ponto : this.interestPoints.get(s)) {
                            realm.beginTransaction();

                            Point point = realm.createObject(Point.class);
                            point.setLatitude(ponto.getLatitude());
                            point.setLongitude(ponto.getLongitude());
                            point.setEndereco(ponto.getEndereco());
                            if(ponto.getDescricao() == null) {
                                point.setDescricao("");
                            }
                            else point.setDescricao(ponto.getDescricao());
                            //point.setDescricao(ponto.getDescricao());

                            pontosInteresseCategoria3.get(0).getPoints().add(point);

                            realm.commitTransaction();
                            Log.d(TAG, "21 - Novo ponto de interesse inserido");
                        }
                    }
                }
            }
        }

        realm.close();
    }

    /* ------------------------------------------------------------------------------------------------------------------------- */
    /* ------------------------------------------------------------------------------------------------------------------------- */

    /* Transforma num array de bytes */
    public byte[] databaseToBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutput oo = new ObjectOutputStream(baos);
        oo.writeObject(this);
        return baos.toByteArray();
    }

    /* Reconstroi a partir de um array de bytes */
    public static DBToFromBytes databaseFromBytes(byte[] bytesMensagem) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytesMensagem);
        ObjectInput oi = new ObjectInputStream(bais);
        return (DBToFromBytes) oi.readObject();
    }
}
