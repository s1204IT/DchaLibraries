package com.android.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Slog;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class EntropyMixer extends Binder {
    private static final int ENTROPY_WHAT = 1;
    private static final int ENTROPY_WRITE_PERIOD = 10800000;
    private static final String TAG = "EntropyMixer";
    private final String entropyFile;
    private final String hwRandomDevice;
    private final BroadcastReceiver mBroadcastReceiver;
    private final Handler mHandler;
    private final String randomDevice;
    private static final long START_TIME = System.currentTimeMillis();
    private static final long START_NANOTIME = System.nanoTime();

    public EntropyMixer(Context context) {
        this(context, getSystemDir() + "/entropy.dat", "/dev/urandom", "/dev/hw_random");
    }

    public EntropyMixer(Context context, String str, String str2, String str3) throws Throwable {
        this.mHandler = new Handler(IoThread.getHandler().getLooper()) {
            @Override
            public void handleMessage(Message message) throws Throwable {
                if (message.what == 1) {
                    EntropyMixer.this.addHwRandomEntropy();
                    EntropyMixer.this.writeEntropy();
                    EntropyMixer.this.scheduleEntropyWriter();
                    return;
                }
                Slog.e(EntropyMixer.TAG, "Will not process invalid message");
            }
        };
        this.mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) throws Throwable {
                EntropyMixer.this.writeEntropy();
            }
        };
        if (str2 == null) {
            throw new NullPointerException("randomDevice");
        }
        if (str3 == null) {
            throw new NullPointerException("hwRandomDevice");
        }
        if (str == null) {
            throw new NullPointerException("entropyFile");
        }
        this.randomDevice = str2;
        this.hwRandomDevice = str3;
        this.entropyFile = str;
        loadInitialEntropy();
        addDeviceSpecificEntropy();
        addHwRandomEntropy();
        writeEntropy();
        scheduleEntropyWriter();
        IntentFilter intentFilter = new IntentFilter("android.intent.action.ACTION_SHUTDOWN");
        intentFilter.addAction("android.intent.action.ACTION_POWER_CONNECTED");
        intentFilter.addAction("android.intent.action.REBOOT");
        context.registerReceiver(this.mBroadcastReceiver, intentFilter, null, this.mHandler);
    }

    private void scheduleEntropyWriter() {
        this.mHandler.removeMessages(1);
        this.mHandler.sendEmptyMessageDelayed(1, 10800000L);
    }

    private void loadInitialEntropy() throws Throwable {
        try {
            RandomBlock.fromFile(this.entropyFile).toFile(this.randomDevice, false);
        } catch (FileNotFoundException e) {
            Slog.w(TAG, "No existing entropy file -- first boot?");
        } catch (IOException e2) {
            Slog.w(TAG, "Failure loading existing entropy file", e2);
        }
    }

    private void writeEntropy() throws Throwable {
        try {
            Slog.i(TAG, "Writing entropy...");
            RandomBlock.fromFile(this.randomDevice).toFile(this.entropyFile, true);
        } catch (IOException e) {
            Slog.w(TAG, "Unable to write entropy", e);
        }
    }

    private void addDeviceSpecificEntropy() throws Throwable {
        PrintWriter printWriter;
        Throwable th;
        IOException e;
        try {
            try {
                printWriter = new PrintWriter(new FileOutputStream(this.randomDevice));
                try {
                    printWriter.println("Copyright (C) 2009 The Android Open Source Project");
                    printWriter.println("All Your Randomness Are Belong To Us");
                    printWriter.println(START_TIME);
                    printWriter.println(START_NANOTIME);
                    printWriter.println(SystemProperties.get("ro.serialno"));
                    printWriter.println(SystemProperties.get("ro.bootmode"));
                    printWriter.println(SystemProperties.get("ro.baseband"));
                    printWriter.println(SystemProperties.get("ro.carrier"));
                    printWriter.println(SystemProperties.get("ro.bootloader"));
                    printWriter.println(SystemProperties.get("ro.hardware"));
                    printWriter.println(SystemProperties.get("ro.revision"));
                    printWriter.println(SystemProperties.get("ro.build.fingerprint"));
                    printWriter.println(new Object().hashCode());
                    printWriter.println(System.currentTimeMillis());
                    printWriter.println(System.nanoTime());
                } catch (IOException e2) {
                    e = e2;
                    Slog.w(TAG, "Unable to add device specific data to the entropy pool", e);
                    if (printWriter == null) {
                        return;
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                if (printWriter != null) {
                    printWriter.close();
                }
                throw th;
            }
        } catch (IOException e3) {
            printWriter = null;
            e = e3;
        } catch (Throwable th3) {
            printWriter = null;
            th = th3;
            if (printWriter != null) {
            }
            throw th;
        }
        printWriter.close();
    }

    private void addHwRandomEntropy() throws Throwable {
        if (!new File(this.hwRandomDevice).exists()) {
            return;
        }
        try {
            RandomBlock.fromFile(this.hwRandomDevice).toFile(this.randomDevice, false);
            Slog.i(TAG, "Added HW RNG output to entropy pool");
        } catch (IOException e) {
            Slog.w(TAG, "Failed to add HW RNG output to entropy pool", e);
        }
    }

    private static String getSystemDir() {
        File file = new File(Environment.getDataDirectory(), "system");
        file.mkdirs();
        return file.toString();
    }
}
