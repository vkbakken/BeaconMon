package com.fastbakken.beaconmonitor.db;


import android.content.Context;

import com.fastbakken.beaconmonitor.R;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.data.Blob;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;


@Table(database = DbInstance.class)
public class EddyStoneDevice extends BaseModel {
    @Column
    @PrimaryKey
    public String id;
    @Column
    public String url;
    @Column
    public int rssi;
    public double[] data;
    @Column
    public String name;
    @Column
    public double temperature;
    @Column
    public double humidity;
    @Column
    public double pressure;
    @Column
    public Blob rawDataBlob;
    public byte[] rawData;
    @Column
    public double voltage;
    @Column
    public Date updateAt;
    @Column
    public double txPower;
    @Column
    public int measurementSequenceNumber;


    public EddyStoneDevice preserveData(EddyStoneDevice tag) {
        tag.name = this.name;
        tag.updateAt = new Date();
        return tag;
    }

    private double getFahrenheit() {
        return celciusToFahrenheit(this.temperature);
    }

    public static String getTemperatureUnit(Context context) {
        //return new Preferences(context).getTemperatureUnit();
        return "C";
    }

    public String getTemperatureString(Context context) {
        String temperatureUnit = EddyStoneDevice.getTemperatureUnit(context);
        return String.format(context.getString(R.string.temperature_reading), this.temperature) + temperatureUnit;
    }

    public String getDispayName() {
        return (this.name != null && !this.name.isEmpty()) ? this.name : this.id;
    }

    public static List<EddyStoneDevice> getAll() {
        return SQLite.select()
                .from(EddyStoneDevice.class)
                .queryList();
    }

    public static EddyStoneDevice get(String id) {
        return SQLite.select()
                .from(EddyStoneDevice.class)
                .where(EddyStoneDevice_Table.id.eq(id))
                .querySingle();
    }

    public void deleteTagAndRelatives() {
        SQLite.delete()
                .from(EddyStoneReading.class)
                .where(EddyStoneReading_Table.eddystoneId.eq(this.id))
                .execute();

        this.delete();
    }


    public double celciusToFahrenheit(double celcius) {
        return round(celcius * 1.8 + 32.0, 2);
    }

    public double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
