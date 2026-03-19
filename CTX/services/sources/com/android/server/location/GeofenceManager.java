package com.android.server.location;

import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.location.Geofence;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Slog;
import com.android.server.LocationManagerService;
import com.android.server.PendingIntentUtils;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class GeofenceManager implements LocationListener, PendingIntent.OnFinished {
    private static final boolean D = LocationManagerService.D;
    private static final long DEFAULT_MIN_INTERVAL_MS = 1800000;
    private static final long MAX_AGE_NANOS = 300000000000L;
    private static final long MAX_INTERVAL_MS = 7200000;
    private static final int MAX_SPEED_M_S = 100;
    private static final int MSG_UPDATE_FENCES = 1;
    private static final String TAG = "GeofenceManager";
    private final AppOpsManager mAppOps;
    private final LocationBlacklist mBlacklist;
    private final Context mContext;
    private long mEffectiveMinIntervalMs;
    private Location mLastLocationUpdate;
    private final LocationManager mLocationManager;
    private long mLocationUpdateInterval;
    private boolean mPendingUpdate;
    private boolean mReceivingLocationUpdates;
    private ContentResolver mResolver;
    private final PowerManager.WakeLock mWakeLock;
    private Object mLock = new Object();
    private List<GeofenceState> mFences = new LinkedList();
    private final GeofenceHandler mHandler = new GeofenceHandler();

    public GeofenceManager(Context context, LocationBlacklist locationBlacklist) {
        this.mContext = context;
        this.mLocationManager = (LocationManager) this.mContext.getSystemService("location");
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService("appops");
        this.mWakeLock = ((PowerManager) this.mContext.getSystemService("power")).newWakeLock(1, TAG);
        this.mBlacklist = locationBlacklist;
        this.mResolver = this.mContext.getContentResolver();
        updateMinInterval();
        this.mResolver.registerContentObserver(Settings.Global.getUriFor("location_background_throttle_proximity_alert_interval_ms"), true, new ContentObserver(this.mHandler) {
            @Override
            public void onChange(boolean z) {
                synchronized (GeofenceManager.this.mLock) {
                    GeofenceManager.this.updateMinInterval();
                }
            }
        }, -1);
    }

    private void updateMinInterval() {
        this.mEffectiveMinIntervalMs = Settings.Global.getLong(this.mResolver, "location_background_throttle_proximity_alert_interval_ms", 1800000L);
    }

    public void addFence(LocationRequest locationRequest, Geofence geofence, PendingIntent pendingIntent, int i, int i2, String str) {
        LocationRequest locationRequest2;
        int i3;
        String str2;
        if (D) {
            StringBuilder sb = new StringBuilder();
            sb.append("addFence: request=");
            locationRequest2 = locationRequest;
            sb.append(locationRequest2);
            sb.append(", geofence=");
            sb.append(geofence);
            sb.append(", intent=");
            sb.append(pendingIntent);
            sb.append(", uid=");
            i3 = i2;
            sb.append(i3);
            sb.append(", packageName=");
            str2 = str;
            sb.append(str2);
            Slog.d(TAG, sb.toString());
        } else {
            locationRequest2 = locationRequest;
            i3 = i2;
            str2 = str;
        }
        GeofenceState geofenceState = new GeofenceState(geofence, locationRequest2.getExpireAt(), i, i3, str2, pendingIntent);
        synchronized (this.mLock) {
            int size = this.mFences.size() - 1;
            while (true) {
                if (size < 0) {
                    break;
                }
                GeofenceState geofenceState2 = this.mFences.get(size);
                if (!geofence.equals(geofenceState2.mFence) || !pendingIntent.equals(geofenceState2.mIntent)) {
                    size--;
                } else {
                    this.mFences.remove(size);
                    break;
                }
            }
            this.mFences.add(geofenceState);
            scheduleUpdateFencesLocked();
        }
    }

    public void removeFence(Geofence geofence, PendingIntent pendingIntent) {
        if (D) {
            Slog.d(TAG, "removeFence: fence=" + geofence + ", intent=" + pendingIntent);
        }
        synchronized (this.mLock) {
            Iterator<GeofenceState> it = this.mFences.iterator();
            while (it.hasNext()) {
                GeofenceState next = it.next();
                if (next.mIntent.equals(pendingIntent)) {
                    if (geofence == null) {
                        it.remove();
                    } else if (geofence.equals(next.mFence)) {
                        it.remove();
                    }
                }
            }
            scheduleUpdateFencesLocked();
        }
    }

    public void removeFence(String str) {
        if (D) {
            Slog.d(TAG, "removeFence: packageName=" + str);
        }
        synchronized (this.mLock) {
            Iterator<GeofenceState> it = this.mFences.iterator();
            while (it.hasNext()) {
                if (it.next().mPackageName.equals(str)) {
                    it.remove();
                }
            }
            scheduleUpdateFencesLocked();
        }
    }

    private void removeExpiredFencesLocked() {
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        Iterator<GeofenceState> it = this.mFences.iterator();
        while (it.hasNext()) {
            if (it.next().mExpireAt < jElapsedRealtime) {
                it.remove();
            }
        }
    }

    private void scheduleUpdateFencesLocked() {
        if (!this.mPendingUpdate) {
            this.mPendingUpdate = true;
            this.mHandler.sendEmptyMessage(1);
        }
    }

    private Location getFreshLocationLocked() {
        Location lastLocation = this.mReceivingLocationUpdates ? this.mLastLocationUpdate : null;
        if (lastLocation == null && !this.mFences.isEmpty()) {
            lastLocation = this.mLocationManager.getLastLocation();
        }
        if (lastLocation != null && SystemClock.elapsedRealtimeNanos() - lastLocation.getElapsedRealtimeNanos() <= MAX_AGE_NANOS) {
            return lastLocation;
        }
        return null;
    }

    private void updateFences() {
        long jMin;
        LinkedList linkedList = new LinkedList();
        LinkedList linkedList2 = new LinkedList();
        synchronized (this.mLock) {
            this.mPendingUpdate = false;
            removeExpiredFencesLocked();
            Location freshLocationLocked = getFreshLocationLocked();
            boolean z = false;
            double d = Double.MAX_VALUE;
            for (GeofenceState geofenceState : this.mFences) {
                if (this.mBlacklist.isBlacklisted(geofenceState.mPackageName)) {
                    if (D) {
                        Slog.d(TAG, "skipping geofence processing for blacklisted app: " + geofenceState.mPackageName);
                    }
                } else if (LocationManagerService.resolutionLevelToOp(geofenceState.mAllowedResolutionLevel) >= 0 && this.mAppOps.noteOpNoThrow(1, geofenceState.mUid, geofenceState.mPackageName) != 0) {
                    if (D) {
                        Slog.d(TAG, "skipping geofence processing for no op app: " + geofenceState.mPackageName);
                    }
                } else {
                    if (freshLocationLocked != null) {
                        int iProcessLocation = geofenceState.processLocation(freshLocationLocked);
                        if ((iProcessLocation & 1) != 0) {
                            linkedList.add(geofenceState.mIntent);
                        }
                        if ((iProcessLocation & 2) != 0) {
                            linkedList2.add(geofenceState.mIntent);
                        }
                        double distanceToBoundary = geofenceState.getDistanceToBoundary();
                        if (distanceToBoundary < d) {
                            d = distanceToBoundary;
                        }
                    }
                    z = true;
                }
            }
            if (z) {
                if (freshLocationLocked != null && Double.compare(d, Double.MAX_VALUE) != 0) {
                    jMin = (long) Math.min(7200000.0d, Math.max(this.mEffectiveMinIntervalMs, (d * 1000.0d) / 100.0d));
                } else {
                    jMin = this.mEffectiveMinIntervalMs;
                }
                if (!this.mReceivingLocationUpdates || this.mLocationUpdateInterval != jMin) {
                    this.mReceivingLocationUpdates = true;
                    this.mLocationUpdateInterval = jMin;
                    this.mLastLocationUpdate = freshLocationLocked;
                    LocationRequest locationRequest = new LocationRequest();
                    locationRequest.setInterval(jMin).setFastestInterval(0L);
                    this.mLocationManager.requestLocationUpdates(locationRequest, this, this.mHandler.getLooper());
                }
            } else if (this.mReceivingLocationUpdates) {
                this.mReceivingLocationUpdates = false;
                this.mLocationUpdateInterval = 0L;
                this.mLastLocationUpdate = null;
                this.mLocationManager.removeUpdates(this);
            }
            if (D) {
                Slog.d(TAG, "updateFences: location=" + freshLocationLocked + ", mFences.size()=" + this.mFences.size() + ", mReceivingLocationUpdates=" + this.mReceivingLocationUpdates + ", mLocationUpdateInterval=" + this.mLocationUpdateInterval + ", mLastLocationUpdate=" + this.mLastLocationUpdate);
            }
        }
        Iterator it = linkedList2.iterator();
        while (it.hasNext()) {
            sendIntentExit((PendingIntent) it.next());
        }
        Iterator it2 = linkedList.iterator();
        while (it2.hasNext()) {
            sendIntentEnter((PendingIntent) it2.next());
        }
    }

    private void sendIntentEnter(PendingIntent pendingIntent) {
        if (D) {
            Slog.d(TAG, "sendIntentEnter: pendingIntent=" + pendingIntent);
        }
        Intent intent = new Intent();
        intent.putExtra("entering", true);
        sendIntent(pendingIntent, intent);
    }

    private void sendIntentExit(PendingIntent pendingIntent) {
        if (D) {
            Slog.d(TAG, "sendIntentExit: pendingIntent=" + pendingIntent);
        }
        Intent intent = new Intent();
        intent.putExtra("entering", false);
        sendIntent(pendingIntent, intent);
    }

    private void sendIntent(PendingIntent pendingIntent, Intent intent) {
        this.mWakeLock.acquire();
        try {
            pendingIntent.send(this.mContext, 0, intent, this, null, "android.permission.ACCESS_FINE_LOCATION", PendingIntentUtils.createDontSendToRestrictedAppsBundle(null));
        } catch (PendingIntent.CanceledException e) {
            removeFence(null, pendingIntent);
            this.mWakeLock.release();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        synchronized (this.mLock) {
            if (this.mReceivingLocationUpdates) {
                this.mLastLocationUpdate = location;
            }
            if (this.mPendingUpdate) {
                this.mHandler.removeMessages(1);
            } else {
                this.mPendingUpdate = true;
            }
        }
        updateFences();
    }

    @Override
    public void onStatusChanged(String str, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String str) {
    }

    @Override
    public void onProviderDisabled(String str) {
    }

    @Override
    public void onSendFinished(PendingIntent pendingIntent, Intent intent, int i, String str, Bundle bundle) {
        this.mWakeLock.release();
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println("  Geofences:");
        for (GeofenceState geofenceState : this.mFences) {
            printWriter.append("    ");
            printWriter.append((CharSequence) geofenceState.mPackageName);
            printWriter.append(" ");
            printWriter.append((CharSequence) geofenceState.mFence.toString());
            printWriter.append("\n");
        }
    }

    private final class GeofenceHandler extends Handler {
        public GeofenceHandler() {
            super(true);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 1) {
                GeofenceManager.this.updateFences();
            }
        }
    }
}
