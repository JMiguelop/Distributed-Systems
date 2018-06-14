package com.mieipi.blueiot.DataModels;

import java.util.Date;
import io.realm.RealmObject;

public class CommunicationPoint extends RealmObject {
    private Point point;
    //private Date registeredDate;

    public Point getPoint() {
        return point;
    }

    public void setPoint(Point point) {
        this.point = point;
    }

    /*public Date getRegisteredDate() {
        return registeredDate;
    }*/

    /*public void setRegisteredDate(Date registeredDate) {
        this.registeredDate = registeredDate;
    }*/
}
