package com.fastbakken.beaconmonitor.db;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.util.Calendar;
import java.util.Date;
import java.util.List;


@Table(database = DbInstance.class)
public class EddyStoneReading extends BaseModel {
    @PrimaryKey(autoincrement = true)
    @Column
    public int id;
    @Column
    public String eddystoneId;
    @Column
    public Date createdAt;
    @Column
    public double temperature;
    @Column
    public double humidity;
    @Column
    public double pressure;
    @Column
    public int rssi;
    @Column
    public double accelX;
    @Column
    public double accelY;
    @Column
    public double accelZ;
    @Column
    public double voltage;
    @Column
    public int dataFormat;
    @Column
    public double txPower;
    @Column
    public int movementCounter;
    @Column
    public int measurementSequenceNumber;

    public EddyStoneReading() {

    }

    public EddyStoneReading(EddyStoneDevice tag) {
        this.eddystoneId = tag.id;
        this.temperature = tag.temperature;
        this.humidity = tag.humidity;
        this.pressure = tag.pressure;
        this.rssi = tag.rssi;
        this.voltage = tag.voltage;
        this.txPower = tag.txPower;
        //this.movementCounter = tag.movementCounter;
        this.measurementSequenceNumber = tag.measurementSequenceNumber;
        this.createdAt = new Date();
    }


    public static List<EddyStoneReading> getForTag(String id) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.HOUR, -24);
        return SQLite.select()
                .from(EddyStoneReading.class)
                .where(EddyStoneReading_Table.eddystoneId.eq(id))
                .and(EddyStoneReading_Table.createdAt.greaterThan(cal.getTime()))
                .queryList();
    }

    public static List<EddyStoneReading> getLatestForTag(String id, int limit) {
        return SQLite.select()
                .from(EddyStoneReading.class)
                .where(EddyStoneReading_Table.eddystoneId.eq(id))
                .orderBy(EddyStoneReading_Table.id, false)
                .limit(limit)
                .queryList();
    }

    public static void removeOlderThan(int hours) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.HOUR, -hours);
        SQLite.delete()
                .from(EddyStoneReading.class)
                .where(EddyStoneReading_Table.createdAt.lessThan(cal.getTime()))
                .async()
                .execute();
    }
}
