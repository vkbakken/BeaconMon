package com.fastbakken.beaconmonitor.activity;


import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fastbakken.beaconmonitor.R;
import com.fastbakken.beaconmonitor.db.EddyStoneReading;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.List;


public class GraphActivity extends AppCompatActivity {
    private static final int GRAPH_UI_UPDATE_FREQ = 5000;
    private Handler mHandler = new Handler();
    private LineChart temp1;
    private LineChart temp2;
    private String id;
    private float idx = (float)0.0;


    private Runnable updater = new Runnable() {
        @Override
        public void run() {
            updateUi();
            mHandler.postDelayed(updater, GRAPH_UI_UPDATE_FREQ);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        id = getIntent().getStringExtra("device_id");

        System.out.println("Intent data: " + id);

        setContentView(R.layout.activity_graph);

        // Inflate the layout for this fragment
        idx = (float)0.0;

        temp1 = findViewById(R.id.temp1Chart);
        temp1.getDescription().setEnabled(false);
        temp1.setTouchEnabled(true);
        temp1.setPinchZoom(true);
        temp1.setScaleEnabled(true);
        temp1.setDrawGridBackground(false);
        temp1.setBackgroundColor(Color.DKGRAY);

        setupAxes();

        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);
        temp1.setData(data);

        mHandler.post(updater);
    }

    private void updateUi() {
        LineData data = temp1.getData();

        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);

            List<EddyStoneReading> measurements = EddyStoneReading.getForTag(id);

            if (set != null) {
                data.removeDataSet(0);
            }

            set = createSet(id);

            int i = 0;
            for (EddyStoneReading r : measurements) {
                set.addEntry(new Entry(i++, (float)r.temperature));
            }

            data.addDataSet(set);
            data.notifyDataChanged();
            temp1.notifyDataSetChanged();

            // limit the number of visible entries
            temp1.setVisibleXRangeMaximum(10);
            // move to the latest entry
            temp1.moveViewToX(measurements.size());
        }
    }



    private LineDataSet createSet(String address) {
        LineDataSet set = new LineDataSet(null, address);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColors(ColorTemplate.VORDIPLOM_COLORS[0]);
        set.setCircleColor(Color.WHITE);
        set.setLineWidth(2f);
        set.setCircleRadius(4f);
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(10f);
        // To show values of each point
        set.setDrawValues(true);

        return set;
    }


    private void setupAxes() {
        XAxis xl = temp1.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = temp1.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMaximum(44.0f);
        leftAxis.setAxisMinimum(20f);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = temp1.getAxisRight();
        rightAxis.setEnabled(false);

        // Add a limit line
        LimitLine ll = new LimitLine(38.0f, "Febrile Core Temp");
        ll.setLineWidth(2f);
        ll.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        ll.setTextSize(10f);
        ll.setTextColor(Color.WHITE);
        // reset all limit lines to avoid overlapping lines
        leftAxis.removeAllLimitLines();
        leftAxis.addLimitLine(ll);
        // limit lines are drawn behind data (and not on top)
        leftAxis.setDrawLimitLinesBehindData(true);
    }
}
