package com.android.location.fused;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.os.SystemClock;
import android.os.WorkSource;
import android.util.Log;
import com.android.location.provider.LocationRequestUnbundled;
import com.android.location.provider.ProviderRequestUnbundled;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;

public class FusionEngine implements LocationListener {
    private Callback mCallback;
    private final Context mContext;
    private boolean mEnabled;
    private Location mFusedLocation;
    private Location mGpsLocation;
    private final LocationManager mLocationManager;
    private final Looper mLooper;
    private ProviderRequestUnbundled mRequest;
    private final HashMap<String, ProviderStats> mStats = new HashMap<>();
    private Location mNetworkLocation = new Location("");

    public interface Callback {
        void reportLocation(Location location);
    }

    public FusionEngine(Context context, Looper looper) {
        this.mContext = context;
        this.mLocationManager = (LocationManager) context.getSystemService("location");
        this.mNetworkLocation.setAccuracy(Float.MAX_VALUE);
        this.mGpsLocation = new Location("");
        this.mGpsLocation.setAccuracy(Float.MAX_VALUE);
        this.mLooper = looper;
        this.mStats.put("gps", new ProviderStats());
        this.mStats.put("network", new ProviderStats());
    }

    public void init(Callback callback) {
        Log.i("FusedLocation", "engine started (" + this.mContext.getPackageName() + ")");
        this.mCallback = callback;
    }

    public void deinit() {
        this.mRequest = null;
        disable();
        Log.i("FusedLocation", "engine stopped (" + this.mContext.getPackageName() + ")");
    }

    public void disable() {
        if (this.mEnabled) {
            this.mEnabled = false;
            updateRequirements();
        }
    }

    public void setRequest(ProviderRequestUnbundled providerRequestUnbundled, WorkSource workSource) {
        this.mRequest = providerRequestUnbundled;
        this.mEnabled = providerRequestUnbundled.getReportLocation();
        updateRequirements();
    }

    private static class ProviderStats {
        public long minTime;
        public long requestTime;
        public boolean requested;

        private ProviderStats() {
        }

        public String toString() {
            return this.requested ? " REQUESTED" : " ---";
        }
    }

    private void enableProvider(String str, long j) {
        ProviderStats providerStats = this.mStats.get(str);
        if (providerStats != null && this.mLocationManager.isProviderEnabled(str)) {
            if (providerStats.requested) {
                if (providerStats.minTime != j) {
                    providerStats.minTime = j;
                    this.mLocationManager.requestLocationUpdates(str, j, 0.0f, this, this.mLooper);
                    return;
                }
                return;
            }
            providerStats.requestTime = SystemClock.elapsedRealtime();
            providerStats.requested = true;
            providerStats.minTime = j;
            this.mLocationManager.requestLocationUpdates(str, j, 0.0f, this, this.mLooper);
        }
    }

    private void disableProvider(String str) {
        ProviderStats providerStats = this.mStats.get(str);
        if (providerStats != null && providerStats.requested) {
            providerStats.requested = false;
            this.mLocationManager.removeUpdates(this);
        }
    }

    private void updateRequirements() {
        if (!this.mEnabled || this.mRequest == null) {
            this.mRequest = null;
            disableProvider("network");
            disableProvider("gps");
            return;
        }
        long interval = Long.MAX_VALUE;
        long interval2 = Long.MAX_VALUE;
        for (LocationRequestUnbundled locationRequestUnbundled : this.mRequest.getLocationRequests()) {
            int quality = locationRequestUnbundled.getQuality();
            if (quality != 100) {
                if (quality == 102 || quality == 104 || quality == 201) {
                    if (locationRequestUnbundled.getInterval() < interval2) {
                        interval2 = locationRequestUnbundled.getInterval();
                    }
                } else if (quality != 203) {
                }
            }
            if (locationRequestUnbundled.getInterval() < interval) {
                interval = locationRequestUnbundled.getInterval();
            }
            if (locationRequestUnbundled.getInterval() < interval2) {
                interval2 = locationRequestUnbundled.getInterval();
            }
        }
        if (interval < Long.MAX_VALUE) {
            enableProvider("gps", interval);
        } else {
            disableProvider("gps");
        }
        if (interval2 < Long.MAX_VALUE) {
            enableProvider("network", interval2);
        } else {
            disableProvider("network");
        }
    }

    private static boolean isBetterThan(Location location, Location location2) {
        if (location == null) {
            return false;
        }
        if (location2 == null || location.getElapsedRealtimeNanos() > location2.getElapsedRealtimeNanos() + 11000000000L) {
            return true;
        }
        if (!location.hasAccuracy()) {
            return false;
        }
        if (location2.hasAccuracy() && location.getAccuracy() >= location2.getAccuracy()) {
            return false;
        }
        return true;
    }

    private void updateFusedLocation() {
        Bundle extras;
        if (isBetterThan(this.mGpsLocation, this.mNetworkLocation)) {
            this.mFusedLocation = new Location(this.mGpsLocation);
        } else {
            this.mFusedLocation = new Location(this.mNetworkLocation);
        }
        this.mFusedLocation.setProvider("fused");
        if (this.mNetworkLocation != null && (extras = this.mNetworkLocation.getExtras()) != null) {
            Parcelable parcelable = extras.getParcelable("noGPSLocation");
            if (parcelable instanceof Location) {
                Bundle extras2 = this.mFusedLocation.getExtras();
                if (extras2 == null) {
                    extras2 = new Bundle();
                    this.mFusedLocation.setExtras(extras2);
                }
                extras2.putParcelable("noGPSLocation", parcelable);
            }
        }
        if (this.mCallback != null) {
            this.mCallback.reportLocation(this.mFusedLocation);
        } else {
            Log.w("FusedLocation", "Location updates received while fusion engine not started");
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if ("gps".equals(location.getProvider())) {
            this.mGpsLocation = location;
            updateFusedLocation();
        } else if ("network".equals(location.getProvider())) {
            this.mNetworkLocation = location;
            updateFusedLocation();
        }
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

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        StringBuilder sb = new StringBuilder();
        sb.append("mEnabled=");
        sb.append(this.mEnabled);
        sb.append(' ');
        sb.append(this.mRequest);
        sb.append('\n');
        sb.append("fused=");
        sb.append(this.mFusedLocation);
        sb.append('\n');
        sb.append(String.format("gps %s\n", this.mGpsLocation));
        sb.append("    ");
        sb.append(this.mStats.get("gps"));
        sb.append('\n');
        sb.append(String.format("net %s\n", this.mNetworkLocation));
        sb.append("    ");
        sb.append(this.mStats.get("network"));
        sb.append('\n');
        printWriter.append((CharSequence) sb);
    }

    public void switchUser() {
        this.mFusedLocation = null;
        this.mGpsLocation = null;
        this.mNetworkLocation = null;
    }
}
