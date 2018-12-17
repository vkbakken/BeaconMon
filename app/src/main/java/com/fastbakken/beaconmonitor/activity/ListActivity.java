package com.fastbakken.beaconmonitor.activity;

import android.Manifest;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.fastbakken.beaconmonitor.R;
import com.fastbakken.beaconmonitor.db.EddyStoneDevice;
import com.fastbakken.beaconmonitor.service.BtleScan;
import com.raizlabs.android.dbflow.config.FlowManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ListActivity extends AppCompatActivity {
    private static final String TAG = ListActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1337;
    private static final int REQUEST_ENABLE_BT = 1338;
    private static final int IGNORE_BATTERY_OPTIMIZATION_REQUEST = 1339;
    private static final int LIST_UI_UPDATE_FREQ = 1000;
    private Button ctrl;
    private Handler mHandler = new Handler();
    private List<EddyStoneDevice> devices;
    private DeviceAdapter adapter;


    private Runnable updater = new Runnable() {
        @Override
        public void run() {
            updateUi();
            mHandler.postDelayed(updater, LIST_UI_UPDATE_FREQ);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        FlowManager.init(getApplicationContext());
        askIgnoreOptimization();

        ctrl = findViewById(R.id.btleCtrl);
        if (!hasPermissions()) {
            ctrl.setBackgroundColor(Color.RED);
            ctrl.setText(R.string.permissions);
        } else {
            if (!btleIsEnabled()) {
                ctrl.setBackgroundColor(Color.RED);
                ctrl.setText(R.string.enable);
            } else {
                BluetoothAdapter.getDefaultAdapter().disable();
                ctrl.setText(R.string.scan);
                BluetoothAdapter.getDefaultAdapter().enable();
            }
        }

        ctrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasPermissions()) {
                    ctrl.setBackgroundColor(Color.GREEN);

                    if (btleIsEnabled()) {
                        System.out.println(ctrl.getText());
                        System.out.println(getString(R.string.scan));
                        if (ctrl.getText().equals(getString(R.string.scan))) {
                            ctrl.setText(R.string.stopScan);
                            startScan();
                        } else {
                            ctrl.setText(R.string.scan);
                            stopScan();
                        }

                    } else {
                        askToEnable();
                    }
                } else {
                    askPermission();
                }
            }
        });

        devices = new ArrayList();
        adapter = new DeviceAdapter(this, devices);

        ListView list = findViewById(R.id.device_list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), GraphActivity.class);
                intent.putExtra("device_id", adapter.getItem(position).id);
                startActivity(intent);
            }
        });
    }


    @Override
    protected void onDestroy () {
        super.onDestroy();
        stopScan();
    }

    private void updateUi() {
        devices.clear();
        devices.addAll(EddyStoneDevice.getAll());

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, -50);
        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).updateAt.getTime() < calendar.getTime().getTime()) {
                devices.remove(i);
                i--;
            }
        }
        if (devices.size() > 0) {
            Helper.sortTagsByRssi(devices);
        }

        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void startScan() {
        if (!isBtleServiceRunning(BtleScan.class)) {
            startService(new Intent(this, BtleScan.class));
            mHandler.post(updater);
        }
    }


    private void stopScan() {
        stopService(new Intent(this, BtleScan.class));
    }

    private boolean isBtleServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    private void askIgnoreOptimization() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, IGNORE_BATTERY_OPTIMIZATION_REQUEST);
        }
    }


    private void askToEnable() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }


    private boolean btleIsEnabled() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
           return false;
        }

        return mBluetoothAdapter.isEnabled();
    }


    private boolean hasPermissions() {
        int res1 = this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        int res2 = this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);

        return (res1 == PackageManager.PERMISSION_GRANTED) && (res1 == PackageManager.PERMISSION_GRANTED);
    }


    private void askPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ctrl.setText(R.string.enable);
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                ctrl.setText(R.string.scan);
            }
        }
    }
}
