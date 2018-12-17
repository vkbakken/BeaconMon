package com.fastbakken.beaconmonitor.service;

import android.bluetooth.le.ScanResult;
import android.os.ParcelUuid;

import com.fastbakken.beaconmonitor.db.EddyStoneDevice;
import java.nio.ByteBuffer;


public class EddyStoneDecoder {

    static final byte MIN_SERVICE_DATA_LEN = 14;

    // TLM frames only support version 0x00 for now.
    static final byte EXPECTED_VERSION = 0x00;

    // Minimum expected voltage value in beacon telemetry in millivolts.
    static final int MIN_EXPECTED_VOLTAGE = 0;

    // Maximum expected voltage value in beacon telemetry in millivolts.
    static final int MAX_EXPECTED_VOLTAGE = 10000;

    // Value indicating temperature not supported. temp[0] == 0x80, temp[1] == 0x00.
    static final float TEMPERATURE_NOT_SUPPORTED = -128.0f;

    // Minimum expected temperature value in beacon telemetry in degrees Celsius.
    static final float MIN_EXPECTED_TEMP = 0.0f;

    // Maximum expected temperature value in beacon telemetry in degrees Celsius.
    static final float MAX_EXPECTED_TEMP = 60.0f;

    // Maximum expected PDU count in beacon telemetry.
    // The fastest we'd expect to see a beacon transmitting would be about 10 Hz.
    // Given that and a lifetime of ~3 years, any value above this is suspicious.
    static final int MAX_EXPECTED_PDU_COUNT = 10 * 60 * 60 * 24 * 365 * 3;

    // Maximum expected time since boot in beacon telemetry.
    // Given that and a lifetime of ~3 years, any value above this is suspicious.
    static final int MAX_EXPECTED_SEC_COUNT = 10 * 60 * 60 * 24 * 365 * 3;

    // The service data for a TLM frame should vary with each broadcast, but depending on the
    // firmware implementation a couple of consecutive TLM frames may be broadcast. Store the
    // frame only if few seconds have passed since we last saw one.
    static final int STORE_NEXT_FRAME_DELTA_MS = 3000;

    private static final ParcelUuid EDDYSTONE_SERVICE_UUID = ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB");

    public static EddyStoneDevice decode(ScanResult sr) {
        byte[] serviceData = sr.getScanRecord().getServiceData(EDDYSTONE_SERVICE_UUID);
        EddyStoneDevice beacon = new EddyStoneDevice();

        if (serviceData.length < MIN_SERVICE_DATA_LEN) {
            String err = String.format("TLM frame too short, needs at least %d bytes, got %d",
                    MIN_SERVICE_DATA_LEN, serviceData.length);
            System.out.println(err);
            return null;
        }

        ByteBuffer buf = ByteBuffer.wrap(serviceData);
        buf.get();  // We already know the frame type byte is 0x20.

        // The version should be zero.
        byte version = buf.get();
        if (version != EXPECTED_VERSION) {
            String err = String.format("Bad TLM version, expected 0x%02X, got %02X",
                    EXPECTED_VERSION, version);
            System.out.println(err);
            return null;
        }

        // Battery voltage should be sane. Zero is fine if the device is externally powered, but
        // it shouldn't be negative or unreasonably high.
        short voltage = buf.getShort();
        beacon.voltage = (double)voltage;
        if (voltage != 0 && (voltage < MIN_EXPECTED_VOLTAGE || voltage > MAX_EXPECTED_VOLTAGE)) {
            String err = String.format("Expected TLM voltage to be between %d and %d, got %d",
                    MIN_EXPECTED_VOLTAGE, MAX_EXPECTED_VOLTAGE, voltage);
            System.out.println(err);
            return null;
        }

        // Temp varies a lot with the hardware and the margins appear to be very wide. USB beacons
        // in particular can report quite high temps. Let's at least check they're partially sane.
        byte tempIntegral = buf.get();
        int tempFractional = (buf.get() & 0xff);
        float temp = tempIntegral + (tempFractional / 256.0f);
        beacon.temperature = temp;
        if (temp != TEMPERATURE_NOT_SUPPORTED) {
            if (temp < MIN_EXPECTED_TEMP || temp > MAX_EXPECTED_TEMP) {
                String err = String.format("Expected TLM temperature to be between %.2f and %.2f, got %.2f",
                        MIN_EXPECTED_TEMP, MAX_EXPECTED_TEMP, temp);
                System.out.println(err);
                return null;
            }
        }

        // Check the PDU count is increasing from frame to frame and is neither too low or too high.
        int advCnt = buf.getInt();
        //beacon.movementCounter = advCnt;
        if (advCnt <= 0) {
            String err = "Expected TLM ADV count to be positive, got " + advCnt;
            System.out.println(err);
            return null;
        }

        if (advCnt > MAX_EXPECTED_PDU_COUNT) {
            String err = String.format("TLM ADV count %d is higher than expected max of %d",
                    advCnt, MAX_EXPECTED_PDU_COUNT);
            System.out.println(err);
            return null;
        }

        return beacon;
    }
}
