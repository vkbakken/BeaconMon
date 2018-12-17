package com.fastbakken.beaconmonitor.activity;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.ImageViewCompat;
import android.support.v7.widget.AppCompatImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.fastbakken.beaconmonitor.R;
import com.fastbakken.beaconmonitor.db.EddyStoneDevice;


import java.util.List;


public class DeviceAdapter extends ArrayAdapter<EddyStoneDevice> {

    public DeviceAdapter(@NonNull Context context, List<EddyStoneDevice> tags) {
        super(context, 0, tags);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final EddyStoneDevice tag = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
        }

        TextView txtId = convertView.findViewById(R.id.id);
        TextView lastseen = convertView.findViewById(R.id.lastseen);
        TextView temp = convertView.findViewById(R.id.row_main_temperature);
        TextView humid = convertView.findViewById(R.id.row_main_humidity);
        TextView pres = convertView.findViewById(R.id.row_main_pressure);
        TextView signal = convertView.findViewById(R.id.row_main_signal);

        txtId.setText(tag.getDispayName());

        int ballColorRes = (position % 2 == 0) ? R.color.main : R.color.mainLight;

        ((ImageView)convertView.findViewById(R.id.row_main_letter))
                .setImageBitmap(Helper.createBall((int)getContext().getResources().getDimension(R.dimen.letter_ball_radius),
                        getContext().getResources().getColor(ballColorRes),
                        Color.WHITE,
                        txtId.getText().charAt(0) + ""));

        convertView.findViewById(R.id.row_main_root).setTag(tag);

        String updatedAt = getContext().getResources().getString(R.string.updated) + " " + Helper.strDescribingTimeSince(tag.updateAt);

        lastseen.setText(updatedAt);
        AppCompatImageView bell = convertView.findViewById(R.id.bell);
        int status = 1; //AlarmChecker.getStatus(tag);
        switch (status) {
            case -1:
                bell.setVisibility(View.VISIBLE);
                bell.setImageResource(R.drawable.ic_notifications_off_24px);
                break;
            case 0:
                bell.setVisibility(View.VISIBLE);
                bell.setImageResource(R.drawable.ic_notifications_on_24px);
                break;
            case 1:
                bell.setImageResource(R.drawable.ic_notifications_active_24px);
                bell.setVisibility(bell.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
                break;
        }
        ImageViewCompat.setImageTintList(bell, ColorStateList.valueOf(getContext().getResources().getColor(R.color.main)));

        temp.setText(tag.getTemperatureString(getContext()));
        humid.setText(String.format(getContext().getString(R.string.humidity_reading), tag.humidity));
        pres.setText(String.format(getContext().getString(R.string.pressure_reading), tag.pressure));
        signal.setText(String.format(getContext().getString(R.string.signal_reading), tag.rssi));

        return convertView;
    }
}
