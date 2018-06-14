package com.mieipi.blueiot.DataModels;

import java.util.Date;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;


public class User extends RealmObject {
    @PrimaryKey
    private String Id;
    //private RealmList<Point> points;
    //private Date lastContact;
    private int nContacts;

}
