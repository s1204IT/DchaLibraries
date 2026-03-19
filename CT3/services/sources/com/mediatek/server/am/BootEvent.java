package com.mediatek.server.am;

import android.util.Slog;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class BootEvent {
    private static final String TAG = "BootEvent";
    private static final boolean sDebug = false;
    private static boolean sEnabled = true;

    public static void addBootEvent(String bootevent) {
        if (!sEnabled) {
            return;
        }
        try {
            FileOutputStream fbp = new FileOutputStream("/proc/bootprof");
            fbp.write(bootevent.getBytes());
            fbp.flush();
            fbp.close();
        } catch (FileNotFoundException e) {
            Slog.e(TAG, "Failure open /proc/bootprof, not found!", e);
        } catch (IOException e2) {
            Slog.e(TAG, "Failure open /proc/bootprof entry", e2);
        }
    }

    public static void setEnabled(boolean enabled) {
        Slog.d(TAG, "setEnabled " + enabled);
        sEnabled = enabled;
    }
}
