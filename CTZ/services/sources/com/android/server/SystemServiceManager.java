package com.android.server;

import android.content.Context;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Slog;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class SystemServiceManager {
    private static final int SERVICE_CALL_WARN_TIME_MS = 50;
    private static final String TAG = "SystemServiceManager";
    private final Context mContext;
    private boolean mRuntimeRestarted;
    private long mRuntimeStartElapsedTime;
    private long mRuntimeStartUptime;
    private boolean mSafeMode;
    private final ArrayList<SystemService> mServices = new ArrayList<>();
    private int mCurrentPhase = -1;

    SystemServiceManager(Context context) {
        this.mContext = context;
    }

    public SystemService startService(String str) {
        try {
            return startService(Class.forName(str));
        } catch (ClassNotFoundException e) {
            Slog.i(TAG, "Starting " + str);
            throw new RuntimeException("Failed to create service " + str + ": service class not found, usually indicates that the caller should have called PackageManager.hasSystemFeature() to check whether the feature is available on this device before trying to start the services that implement it", e);
        }
    }

    public <T extends SystemService> T startService(Class<T> cls) {
        try {
            String name = cls.getName();
            Slog.i(TAG, "Starting " + name);
            Trace.traceBegin(524288L, "StartService " + name);
            if (!SystemService.class.isAssignableFrom(cls)) {
                throw new RuntimeException("Failed to create " + name + ": service must extend " + SystemService.class.getName());
            }
            try {
                try {
                    try {
                        T tNewInstance = cls.getConstructor(Context.class).newInstance(this.mContext);
                        startService(tNewInstance);
                        return tNewInstance;
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException("Failed to create service " + name + ": service must have a public constructor with a Context argument", e);
                    }
                } catch (InvocationTargetException e2) {
                    throw new RuntimeException("Failed to create service " + name + ": service constructor threw an exception", e2);
                }
            } catch (IllegalAccessException e3) {
                throw new RuntimeException("Failed to create service " + name + ": service must have a public constructor with a Context argument", e3);
            } catch (InstantiationException e4) {
                throw new RuntimeException("Failed to create service " + name + ": service could not be instantiated", e4);
            }
        } finally {
            Trace.traceEnd(524288L);
        }
    }

    public void startService(SystemService systemService) {
        this.mServices.add(systemService);
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        try {
            systemService.onStart();
            warnIfTooLong(SystemClock.elapsedRealtime() - jElapsedRealtime, systemService, "onStart");
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to start service " + systemService.getClass().getName() + ": onStart threw an exception", e);
        }
    }

    public void startBootPhase(int i) {
        if (i <= this.mCurrentPhase) {
            throw new IllegalArgumentException("Next phase must be larger than previous");
        }
        this.mCurrentPhase = i;
        Slog.i(TAG, "Starting phase " + this.mCurrentPhase);
        try {
            Trace.traceBegin(524288L, "OnBootPhase " + i);
            int size = this.mServices.size();
            for (int i2 = 0; i2 < size; i2++) {
                SystemService systemService = this.mServices.get(i2);
                long jElapsedRealtime = SystemClock.elapsedRealtime();
                Trace.traceBegin(524288L, systemService.getClass().getName());
                try {
                    systemService.onBootPhase(this.mCurrentPhase);
                    warnIfTooLong(SystemClock.elapsedRealtime() - jElapsedRealtime, systemService, "onBootPhase");
                    Trace.traceEnd(524288L);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to boot service " + systemService.getClass().getName() + ": onBootPhase threw an exception during phase " + this.mCurrentPhase, e);
                }
            }
        } finally {
            Trace.traceEnd(524288L);
        }
    }

    public boolean isBootCompleted() {
        return this.mCurrentPhase >= 1000;
    }

    public void startUser(int i) {
        Slog.i(TAG, "Calling onStartUser u" + i);
        int size = this.mServices.size();
        for (int i2 = 0; i2 < size; i2++) {
            SystemService systemService = this.mServices.get(i2);
            Trace.traceBegin(524288L, "onStartUser " + systemService.getClass().getName());
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            try {
                systemService.onStartUser(i);
            } catch (Exception e) {
                Slog.wtf(TAG, "Failure reporting start of user " + i + " to service " + systemService.getClass().getName(), e);
            }
            warnIfTooLong(SystemClock.elapsedRealtime() - jElapsedRealtime, systemService, "onStartUser ");
            Trace.traceEnd(524288L);
        }
    }

    public void unlockUser(int i) {
        Slog.i(TAG, "Calling onUnlockUser u" + i);
        int size = this.mServices.size();
        for (int i2 = 0; i2 < size; i2++) {
            SystemService systemService = this.mServices.get(i2);
            Trace.traceBegin(524288L, "onUnlockUser " + systemService.getClass().getName());
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            try {
                systemService.onUnlockUser(i);
            } catch (Exception e) {
                Slog.wtf(TAG, "Failure reporting unlock of user " + i + " to service " + systemService.getClass().getName(), e);
            }
            warnIfTooLong(SystemClock.elapsedRealtime() - jElapsedRealtime, systemService, "onUnlockUser ");
            Trace.traceEnd(524288L);
        }
    }

    public void switchUser(int i) {
        Slog.i(TAG, "Calling switchUser u" + i);
        int size = this.mServices.size();
        for (int i2 = 0; i2 < size; i2++) {
            SystemService systemService = this.mServices.get(i2);
            Trace.traceBegin(524288L, "onSwitchUser " + systemService.getClass().getName());
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            try {
                systemService.onSwitchUser(i);
            } catch (Exception e) {
                Slog.wtf(TAG, "Failure reporting switch of user " + i + " to service " + systemService.getClass().getName(), e);
            }
            warnIfTooLong(SystemClock.elapsedRealtime() - jElapsedRealtime, systemService, "onSwitchUser");
            Trace.traceEnd(524288L);
        }
    }

    public void stopUser(int i) {
        Slog.i(TAG, "Calling onStopUser u" + i);
        int size = this.mServices.size();
        for (int i2 = 0; i2 < size; i2++) {
            SystemService systemService = this.mServices.get(i2);
            Trace.traceBegin(524288L, "onStopUser " + systemService.getClass().getName());
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            try {
                systemService.onStopUser(i);
            } catch (Exception e) {
                Slog.wtf(TAG, "Failure reporting stop of user " + i + " to service " + systemService.getClass().getName(), e);
            }
            warnIfTooLong(SystemClock.elapsedRealtime() - jElapsedRealtime, systemService, "onStopUser");
            Trace.traceEnd(524288L);
        }
    }

    public void cleanupUser(int i) {
        Slog.i(TAG, "Calling onCleanupUser u" + i);
        int size = this.mServices.size();
        for (int i2 = 0; i2 < size; i2++) {
            SystemService systemService = this.mServices.get(i2);
            Trace.traceBegin(524288L, "onCleanupUser " + systemService.getClass().getName());
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            try {
                systemService.onCleanupUser(i);
            } catch (Exception e) {
                Slog.wtf(TAG, "Failure reporting cleanup of user " + i + " to service " + systemService.getClass().getName(), e);
            }
            warnIfTooLong(SystemClock.elapsedRealtime() - jElapsedRealtime, systemService, "onCleanupUser");
            Trace.traceEnd(524288L);
        }
    }

    void setSafeMode(boolean z) {
        this.mSafeMode = z;
    }

    public boolean isSafeMode() {
        return this.mSafeMode;
    }

    public boolean isRuntimeRestarted() {
        return this.mRuntimeRestarted;
    }

    public long getRuntimeStartElapsedTime() {
        return this.mRuntimeStartElapsedTime;
    }

    public long getRuntimeStartUptime() {
        return this.mRuntimeStartUptime;
    }

    void setStartInfo(boolean z, long j, long j2) {
        this.mRuntimeRestarted = z;
        this.mRuntimeStartElapsedTime = j;
        this.mRuntimeStartUptime = j2;
    }

    private void warnIfTooLong(long j, SystemService systemService, String str) {
        if (j > 50) {
            Slog.w(TAG, "Service " + systemService.getClass().getName() + " took " + j + " ms in " + str);
        }
    }

    public void dump() {
        StringBuilder sb = new StringBuilder();
        sb.append("Current phase: ");
        sb.append(this.mCurrentPhase);
        sb.append("\n");
        sb.append("Services:\n");
        int size = this.mServices.size();
        for (int i = 0; i < size; i++) {
            SystemService systemService = this.mServices.get(i);
            sb.append("\t");
            sb.append(systemService.getClass().getSimpleName());
            sb.append("\n");
        }
        Slog.e(TAG, sb.toString());
    }
}
