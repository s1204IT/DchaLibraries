package com.android.server;

import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Slog;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class SystemServiceManager {
    private static final boolean IS_USER_BUILD = "user".equals(Build.TYPE);
    private static final String TAG = "SystemServiceManager";
    private final Context mContext;
    private boolean mSafeMode;
    private final ArrayList<SystemService> mServices = new ArrayList<>();
    private int mCurrentPhase = -1;

    public SystemServiceManager(Context context) {
        this.mContext = context;
    }

    public SystemService startService(String className) {
        try {
            return startService(Class.forName(className));
        } catch (ClassNotFoundException ex) {
            Slog.i(TAG, "Starting " + className);
            throw new RuntimeException("Failed to create service " + className + ": service class not found, usually indicates that the caller should have called PackageManager.hasSystemFeature() to check whether the feature is available on this device before trying to start the services that implement it", ex);
        }
    }

    public <T extends SystemService> T startService(Class<T> serviceClass) {
        try {
            String name = serviceClass.getName();
            Slog.i(TAG, "Starting " + name);
            Trace.traceBegin(2097152L, "StartService " + name);
            if (!SystemService.class.isAssignableFrom(serviceClass)) {
                throw new RuntimeException("Failed to create " + name + ": service must extend " + SystemService.class.getName());
            }
            try {
                try {
                    Constructor<T> constructor = serviceClass.getConstructor(Context.class);
                    T service = constructor.newInstance(this.mContext);
                    this.mServices.add(service);
                    try {
                        service.onStart();
                        return service;
                    } catch (RuntimeException ex) {
                        throw new RuntimeException("Failed to start service " + name + ": onStart threw an exception", ex);
                    }
                } catch (InstantiationException ex2) {
                    throw new RuntimeException("Failed to create service " + name + ": service could not be instantiated", ex2);
                } catch (NoSuchMethodException ex3) {
                    throw new RuntimeException("Failed to create service " + name + ": service must have a public constructor with a Context argument", ex3);
                }
            } catch (IllegalAccessException ex4) {
                throw new RuntimeException("Failed to create service " + name + ": service must have a public constructor with a Context argument", ex4);
            } catch (InvocationTargetException ex5) {
                throw new RuntimeException("Failed to create service " + name + ": service constructor threw an exception", ex5);
            }
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    public void startBootPhase(int phase) {
        if (phase <= this.mCurrentPhase) {
            throw new IllegalArgumentException("Next phase must be larger than previous");
        }
        this.mCurrentPhase = phase;
        Slog.i(TAG, "Starting phase " + this.mCurrentPhase);
        try {
            Trace.traceBegin(2097152L, "OnBootPhase " + phase);
            int serviceLen = this.mServices.size();
            for (int i = 0; i < serviceLen; i++) {
                SystemService service = this.mServices.get(i);
                try {
                    long startTime = IS_USER_BUILD ? 0L : SystemClock.elapsedRealtime();
                    service.onBootPhase(this.mCurrentPhase);
                    if (!IS_USER_BUILD) {
                        checkTime(startTime, "Phase " + this.mCurrentPhase, service.getClass().getName());
                    }
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to boot service " + service.getClass().getName() + ": onBootPhase threw an exception during phase " + this.mCurrentPhase, ex);
                }
            }
        } finally {
            Trace.traceEnd(2097152L);
        }
    }

    public void startUser(int userHandle) {
        int serviceLen = this.mServices.size();
        for (int i = 0; i < serviceLen; i++) {
            SystemService service = this.mServices.get(i);
            try {
                service.onStartUser(userHandle);
            } catch (Exception ex) {
                Slog.wtf(TAG, "Failure reporting start of user " + userHandle + " to service " + service.getClass().getName(), ex);
            }
        }
    }

    public void unlockUser(int userHandle) {
        int serviceLen = this.mServices.size();
        for (int i = 0; i < serviceLen; i++) {
            SystemService service = this.mServices.get(i);
            try {
                service.onUnlockUser(userHandle);
            } catch (Exception ex) {
                Slog.wtf(TAG, "Failure reporting unlock of user " + userHandle + " to service " + service.getClass().getName(), ex);
            }
        }
    }

    public void switchUser(int userHandle) {
        int serviceLen = this.mServices.size();
        for (int i = 0; i < serviceLen; i++) {
            SystemService service = this.mServices.get(i);
            try {
                service.onSwitchUser(userHandle);
            } catch (Exception ex) {
                Slog.wtf(TAG, "Failure reporting switch of user " + userHandle + " to service " + service.getClass().getName(), ex);
            }
        }
    }

    public void stopUser(int userHandle) {
        int serviceLen = this.mServices.size();
        for (int i = 0; i < serviceLen; i++) {
            SystemService service = this.mServices.get(i);
            try {
                service.onStopUser(userHandle);
            } catch (Exception ex) {
                Slog.wtf(TAG, "Failure reporting stop of user " + userHandle + " to service " + service.getClass().getName(), ex);
            }
        }
    }

    public void cleanupUser(int userHandle) {
        int serviceLen = this.mServices.size();
        for (int i = 0; i < serviceLen; i++) {
            SystemService service = this.mServices.get(i);
            try {
                service.onCleanupUser(userHandle);
            } catch (Exception ex) {
                Slog.wtf(TAG, "Failure reporting cleanup of user " + userHandle + " to service " + service.getClass().getName(), ex);
            }
        }
    }

    public void setSafeMode(boolean safeMode) {
        this.mSafeMode = safeMode;
    }

    public boolean isSafeMode() {
        return this.mSafeMode;
    }

    public void dump() {
        StringBuilder builder = new StringBuilder();
        builder.append("Current phase: ").append(this.mCurrentPhase).append("\n");
        builder.append("Services:\n");
        int startedLen = this.mServices.size();
        for (int i = 0; i < startedLen; i++) {
            SystemService service = this.mServices.get(i);
            builder.append("\t").append(service.getClass().getSimpleName()).append("\n");
        }
        Slog.e(TAG, builder.toString());
    }

    private void checkTime(long startTime, String op, String service) {
        long now = SystemClock.elapsedRealtime();
        if (now - startTime <= 500) {
            return;
        }
        Slog.w(TAG, "[" + op + "]" + service + " took " + (now - startTime) + "ms");
    }
}
