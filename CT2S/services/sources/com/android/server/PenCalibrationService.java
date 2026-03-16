package com.android.server;

import android.hardware.input.IPenCalibrationService;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

public class PenCalibrationService extends IPenCalibrationService.Stub {
    static final String PATH = "/sys/class/wacom_class/wacom_emr/calibration";
    static final String TAG = "PenCalibrationService";

    PenCalibrationService() {
    }

    public void writeBytes(byte[] data) {
        try {
            BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(PATH));
            writer.write(data);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            Log.w(TAG, "failed to writeBytes.", e);
        }
    }

    public void writeChar(char command) {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(PATH)));
            writer.write(command);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            Log.w(TAG, "failed to writeChar.", e);
        }
    }

    public void read(byte[] buffer) {
        try {
            BufferedInputStream reader = new BufferedInputStream(new FileInputStream(PATH));
            reader.read(buffer);
            reader.close();
        } catch (Exception e) {
            Log.w(TAG, "failed to read.", e);
        }
    }
}
