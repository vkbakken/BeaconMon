package com.fastbakken.beaconmonitor.db;

import com.raizlabs.android.dbflow.annotation.Database;


@Database(name = DbInstance.NAME, version = DbInstance.VERSION)
public class DbInstance {
    public static final String NAME = "LocalDatabase";
    public static final int VERSION = 1;
}