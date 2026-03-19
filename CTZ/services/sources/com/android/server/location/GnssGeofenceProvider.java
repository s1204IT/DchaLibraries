package com.android.server.location;

import android.location.IGpsGeofenceHardware;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

class GnssGeofenceProvider extends IGpsGeofenceHardware.Stub {
    private final SparseArray<GeofenceEntry> mGeofenceEntries;
    private final Handler mHandler;
    private final GnssGeofenceProviderNative mNative;
    private static final String TAG = "GnssGeofenceProvider";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);

    private static native boolean native_add_geofence(int i, double d, double d2, double d3, int i2, int i3, int i4, int i5);

    private static native boolean native_is_geofence_supported();

    private static native boolean native_pause_geofence(int i);

    private static native boolean native_remove_geofence(int i);

    private static native boolean native_resume_geofence(int i, int i2);

    private static class GeofenceEntry {
        public int geofenceId;
        public int lastTransition;
        public double latitude;
        public double longitude;
        public int monitorTransitions;
        public int notificationResponsiveness;
        public boolean paused;
        public double radius;
        public int unknownTimer;

        private GeofenceEntry() {
        }
    }

    GnssGeofenceProvider(Looper looper) {
        this(looper, new GnssGeofenceProviderNative());
    }

    @VisibleForTesting
    GnssGeofenceProvider(Looper looper, GnssGeofenceProviderNative gnssGeofenceProviderNative) {
        this.mGeofenceEntries = new SparseArray<>();
        this.mHandler = new Handler(looper);
        this.mNative = gnssGeofenceProviderNative;
    }

    void resumeIfStarted() {
        if (DEBUG) {
            Log.d(TAG, "resumeIfStarted");
        }
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                GnssGeofenceProvider.lambda$resumeIfStarted$0(this.f$0);
            }
        });
    }

    public static void lambda$resumeIfStarted$0(GnssGeofenceProvider gnssGeofenceProvider) {
        for (int i = 0; i < gnssGeofenceProvider.mGeofenceEntries.size(); i++) {
            GeofenceEntry geofenceEntryValueAt = gnssGeofenceProvider.mGeofenceEntries.valueAt(i);
            if (gnssGeofenceProvider.mNative.addGeofence(geofenceEntryValueAt.geofenceId, geofenceEntryValueAt.latitude, geofenceEntryValueAt.longitude, geofenceEntryValueAt.radius, geofenceEntryValueAt.lastTransition, geofenceEntryValueAt.monitorTransitions, geofenceEntryValueAt.notificationResponsiveness, geofenceEntryValueAt.unknownTimer) && geofenceEntryValueAt.paused) {
                gnssGeofenceProvider.mNative.pauseGeofence(geofenceEntryValueAt.geofenceId);
            }
        }
    }

    private boolean runOnHandlerThread(Callable<Boolean> callable) {
        FutureTask futureTask = new FutureTask(callable);
        this.mHandler.post(futureTask);
        try {
            return ((Boolean) futureTask.get()).booleanValue();
        } catch (InterruptedException | ExecutionException e) {
            Log.e(TAG, "Failed running callable.", e);
            return false;
        }
    }

    public boolean isHardwareGeofenceSupported() {
        final GnssGeofenceProviderNative gnssGeofenceProviderNative = this.mNative;
        Objects.requireNonNull(gnssGeofenceProviderNative);
        return runOnHandlerThread(new Callable() {
            @Override
            public final Object call() {
                return Boolean.valueOf(gnssGeofenceProviderNative.isGeofenceSupported());
            }
        });
    }

    public boolean addCircularHardwareGeofence(final int i, final double d, final double d2, final double d3, final int i2, final int i3, final int i4, final int i5) {
        return runOnHandlerThread(new Callable() {
            @Override
            public final Object call() {
                return GnssGeofenceProvider.lambda$addCircularHardwareGeofence$1(this.f$0, i, d, d2, d3, i2, i3, i4, i5);
            }
        });
    }

    public static Boolean lambda$addCircularHardwareGeofence$1(GnssGeofenceProvider gnssGeofenceProvider, int i, double d, double d2, double d3, int i2, int i3, int i4, int i5) throws Exception {
        boolean zAddGeofence = gnssGeofenceProvider.mNative.addGeofence(i, d, d2, d3, i2, i3, i4, i5);
        if (zAddGeofence) {
            GeofenceEntry geofenceEntry = new GeofenceEntry();
            geofenceEntry.geofenceId = i;
            geofenceEntry.latitude = d;
            geofenceEntry.longitude = d2;
            geofenceEntry.radius = d3;
            geofenceEntry.lastTransition = i2;
            geofenceEntry.monitorTransitions = i3;
            geofenceEntry.notificationResponsiveness = i4;
            geofenceEntry.unknownTimer = i5;
            gnssGeofenceProvider.mGeofenceEntries.put(i, geofenceEntry);
        }
        return Boolean.valueOf(zAddGeofence);
    }

    public boolean removeHardwareGeofence(final int i) {
        return runOnHandlerThread(new Callable() {
            @Override
            public final Object call() {
                return GnssGeofenceProvider.lambda$removeHardwareGeofence$2(this.f$0, i);
            }
        });
    }

    public static Boolean lambda$removeHardwareGeofence$2(GnssGeofenceProvider gnssGeofenceProvider, int i) throws Exception {
        boolean zRemoveGeofence = gnssGeofenceProvider.mNative.removeGeofence(i);
        if (zRemoveGeofence) {
            gnssGeofenceProvider.mGeofenceEntries.remove(i);
        }
        return Boolean.valueOf(zRemoveGeofence);
    }

    public boolean pauseHardwareGeofence(final int i) {
        return runOnHandlerThread(new Callable() {
            @Override
            public final Object call() {
                return GnssGeofenceProvider.lambda$pauseHardwareGeofence$3(this.f$0, i);
            }
        });
    }

    public static Boolean lambda$pauseHardwareGeofence$3(GnssGeofenceProvider gnssGeofenceProvider, int i) throws Exception {
        GeofenceEntry geofenceEntry;
        boolean zPauseGeofence = gnssGeofenceProvider.mNative.pauseGeofence(i);
        if (zPauseGeofence && (geofenceEntry = gnssGeofenceProvider.mGeofenceEntries.get(i)) != null) {
            geofenceEntry.paused = true;
        }
        return Boolean.valueOf(zPauseGeofence);
    }

    public boolean resumeHardwareGeofence(final int i, final int i2) {
        return runOnHandlerThread(new Callable() {
            @Override
            public final Object call() {
                return GnssGeofenceProvider.lambda$resumeHardwareGeofence$4(this.f$0, i, i2);
            }
        });
    }

    public static Boolean lambda$resumeHardwareGeofence$4(GnssGeofenceProvider gnssGeofenceProvider, int i, int i2) throws Exception {
        GeofenceEntry geofenceEntry;
        boolean zResumeGeofence = gnssGeofenceProvider.mNative.resumeGeofence(i, i2);
        if (zResumeGeofence && (geofenceEntry = gnssGeofenceProvider.mGeofenceEntries.get(i)) != null) {
            geofenceEntry.paused = false;
            geofenceEntry.monitorTransitions = i2;
        }
        return Boolean.valueOf(zResumeGeofence);
    }

    @VisibleForTesting
    static class GnssGeofenceProviderNative {
        GnssGeofenceProviderNative() {
        }

        public boolean isGeofenceSupported() {
            return GnssGeofenceProvider.native_is_geofence_supported();
        }

        public boolean addGeofence(int i, double d, double d2, double d3, int i2, int i3, int i4, int i5) {
            return GnssGeofenceProvider.native_add_geofence(i, d, d2, d3, i2, i3, i4, i5);
        }

        public boolean removeGeofence(int i) {
            return GnssGeofenceProvider.native_remove_geofence(i);
        }

        public boolean resumeGeofence(int i, int i2) {
            return GnssGeofenceProvider.native_resume_geofence(i, i2);
        }

        public boolean pauseGeofence(int i) {
            return GnssGeofenceProvider.native_pause_geofence(i);
        }
    }
}
