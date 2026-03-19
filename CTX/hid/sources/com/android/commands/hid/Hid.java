package com.android.commands.hid;

import android.util.Log;
import android.util.SparseArray;
import com.android.commands.hid.Event;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import libcore.io.IoUtils;

public class Hid {
    private static final String TAG = "HID";
    private final SparseArray<Device> mDevices = new SparseArray<>();
    private final Event.Reader mReader;

    private static void usage() {
        error("Usage: hid [FILE]");
    }

    public static void main(String[] strArr) {
        InputStream fileInputStream;
        if (strArr.length != 1) {
            usage();
            System.exit(1);
        }
        InputStream inputStream = null;
        try {
            try {
                if (strArr[0].equals("-")) {
                    fileInputStream = System.in;
                } else {
                    fileInputStream = new FileInputStream(new File(strArr[0]));
                }
                inputStream = fileInputStream;
                new Hid(inputStream).run();
            } catch (Exception e) {
                error("HID injection failed.", e);
                System.exit(1);
            }
        } finally {
            IoUtils.closeQuietly(inputStream);
        }
    }

    private Hid(InputStream inputStream) {
        try {
            this.mReader = new Event.Reader(new InputStreamReader(inputStream, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private void run() {
        while (true) {
            try {
                Event nextEvent = this.mReader.getNextEvent();
                if (nextEvent == null) {
                    break;
                } else {
                    process(nextEvent);
                }
            } catch (IOException e) {
                error("Error reading in events.", e);
            }
        }
        for (int i = 0; i < this.mDevices.size(); i++) {
            this.mDevices.valueAt(i).close();
        }
    }

    private void process(Event event) {
        int iIndexOfKey = this.mDevices.indexOfKey(event.getId());
        if (iIndexOfKey < 0) {
            if (Event.COMMAND_REGISTER.equals(event.getCommand())) {
                registerDevice(event);
                return;
            } else {
                Log.e(TAG, "Unknown device id specified. Ignoring event.");
                return;
            }
        }
        Device deviceValueAt = this.mDevices.valueAt(iIndexOfKey);
        if (Event.COMMAND_DELAY.equals(event.getCommand())) {
            deviceValueAt.addDelay(event.getDuration());
            return;
        }
        if (Event.COMMAND_REPORT.equals(event.getCommand())) {
            deviceValueAt.sendReport(event.getReport());
            return;
        }
        if (Event.COMMAND_REGISTER.equals(event.getCommand())) {
            error("Device id=" + event.getId() + " is already registered. Ignoring event.");
            return;
        }
        error("Unknown command \"" + event.getCommand() + "\". Ignoring event.");
    }

    private void registerDevice(Event event) {
        if (!Event.COMMAND_REGISTER.equals(event.getCommand())) {
            throw new IllegalStateException("Tried to send command \"" + event.getCommand() + "\" to an unregistered device!");
        }
        int id = event.getId();
        this.mDevices.append(id, new Device(id, event.getName(), event.getVendorId(), event.getProductId(), event.getDescriptor(), event.getReport()));
    }

    private static void error(String str) {
        error(str, null);
    }

    private static void error(String str, Exception exc) {
        Log.e(TAG, str);
        if (exc != null) {
            Log.e(TAG, Log.getStackTraceString(exc));
        }
    }
}
