package com.fastbakken.beaconmonitor.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import com.fastbakken.beaconmonitor.db.EddyStoneDevice;
import com.fastbakken.beaconmonitor.db.EddyStoneReading;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class BtleScan extends Service {
    private final static String TAG = BtleScan.class.getSimpleName();
    private Handler mHandler = new Handler();
    private static final long scanPeriod = 30 * 1000; // scan period in milliseconds
    private volatile boolean mIsScanning = false;
    private ScanSettings settings = new ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build();
    private ScanFilter filter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB")).build();

    private ScanCallback sc = new ScanCallback() {
        @Override
        public void onScanFailed(int errorCode) {

            System.out.println("Something went wrong: " + errorCode);
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            System.out.println("Device seen " + result.getDevice().getAddress());

            EddyStoneDevice esd = EddyStoneDecoder.decode(result);

            if (esd != null) {
                esd.id = result.getDevice().getAddress();
                esd.updateAt = Calendar.getInstance().getTime();
                esd.rssi = result.getRssi();

                EddyStoneDevice dbDev = EddyStoneDevice.get(esd.id);
                if (dbDev != null) {
                    esd = dbDev.preserveData(esd);
                    esd.update();
                } else {
                    esd.updateAt = Calendar.getInstance().getTime();
                    esd.save();
                }

                EddyStoneReading reading = new EddyStoneReading(esd);
                reading.save();
            }
        }
    };

    public void start() {
        if (scanPeriod > 0) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mIsScanning) {
                        Log.d(TAG, "Scan timer expired. Restart scan");
                        stop();
                        start();
                    }
                }
            }, scanPeriod);
        }

        mIsScanning = true;
        Log.d(TAG, "start scanning");

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothLeScanner le = mBluetoothAdapter.getBluetoothLeScanner();

        List<ScanFilter> filt = new ArrayList<ScanFilter>();
        filt.add(filter);
        le.startScan(filt, settings, sc);
    }

    public void stop() {
        if (mIsScanning) {
            mHandler.removeCallbacksAndMessages(null);      // cancel pending calls to stop
            mIsScanning = false;
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothLeScanner le = mBluetoothAdapter.getBluetoothLeScanner();
            le.stopScan(sc);
            Log.d(TAG, "stop scanning");
        }
    }

    public boolean isScanning() {
        return mIsScanning;
    }

    public class LocalBinder extends Binder {
        BtleScan getService() {
            return BtleScan.this;
        }
    }


    @Override
    public void onCreate() {
        Toast.makeText(this, "Invoke background service onCreate method.", Toast.LENGTH_LONG).show();
        super.onCreate();
        start();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Invoke background service onStartCommand method.", Toast.LENGTH_LONG).show();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
        Toast.makeText(this, "Invoke background service onDestroy method.", Toast.LENGTH_LONG).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {

        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();
}
