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
        this.mStats.get("gps").available = this.mLocationManager.isProviderEnabled("gps");
        this.mStats.put("network", new ProviderStats());
        this.mStats.get("network").available = this.mLocationManager.isProviderEnabled("network");
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
        this.mEnabled = false;
        updateRequirements();
    }

    public void setRequest(ProviderRequestUnbundled request, WorkSource source) {
        this.mRequest = request;
        this.mEnabled = request.getReportLocation();
        updateRequirements();
    }

    private static class ProviderStats {
        public boolean available;
        public long minTime;
        public long requestTime;
        public boolean requested;

        private ProviderStats() {
        }

        public String toString() {
            StringBuilder s = new StringBuilder();
            s.append(this.available ? "AVAILABLE" : "UNAVAILABLE");
            s.append(this.requested ? " REQUESTED" : " ---");
            return s.toString();
        }
    }

    private void enableProvider(String name, long minTime) {
        ProviderStats stats = this.mStats.get(name);
        if (stats.requested) {
            if (stats.minTime != minTime) {
                stats.minTime = minTime;
                this.mLocationManager.requestLocationUpdates(name, minTime, 0.0f, this, this.mLooper);
                return;
            }
            return;
        }
        stats.requestTime = SystemClock.elapsedRealtime();
        stats.requested = true;
        stats.minTime = minTime;
        this.mLocationManager.requestLocationUpdates(name, minTime, 0.0f, this, this.mLooper);
    }

    private void disableProvider(String name) {
        ProviderStats stats = this.mStats.get(name);
        if (stats.requested) {
            stats.requested = false;
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
        long networkInterval = Long.MAX_VALUE;
        long gpsInterval = Long.MAX_VALUE;
        for (LocationRequestUnbundled request : this.mRequest.getLocationRequests()) {
            switch (request.getQuality()) {
                case 100:
                case 203:
                    if (request.getInterval() < gpsInterval) {
                        gpsInterval = request.getInterval();
                    }
                    if (request.getInterval() < networkInterval) {
                        networkInterval = request.getInterval();
                    }
                    break;
                case 102:
                case 104:
                case 201:
                    if (request.getInterval() < networkInterval) {
                        networkInterval = request.getInterval();
                    }
                    break;
            }
        }
        if (gpsInterval < Long.MAX_VALUE) {
            enableProvider("gps", gpsInterval);
        } else {
            disableProvider("gps");
        }
        if (networkInterval < Long.MAX_VALUE) {
            enableProvider("network", networkInterval);
        } else {
            disableProvider("network");
        }
    }

    private static boolean isBetterThan(Location locationA, Location locationB) {
        if (locationA == null) {
            return false;
        }
        if (locationB == null || locationA.getElapsedRealtimeNanos() > locationB.getElapsedRealtimeNanos() - 1884901888) {
            return true;
        }
        if (locationA.hasAccuracy()) {
            return !locationB.hasAccuracy() || locationA.getAccuracy() < locationB.getAccuracy();
        }
        return false;
    }

    private void updateFusedLocation() {
        Bundle srcExtras;
        if (isBetterThan(this.mGpsLocation, this.mNetworkLocation)) {
            this.mFusedLocation = new Location(this.mGpsLocation);
        } else {
            this.mFusedLocation = new Location(this.mNetworkLocation);
        }
        this.mFusedLocation.setProvider("fused");
        if (this.mNetworkLocation != null && (srcExtras = this.mNetworkLocation.getExtras()) != null) {
            Parcelable srcParcelable = srcExtras.getParcelable("noGPSLocation");
            if (srcParcelable instanceof Location) {
                Bundle dstExtras = this.mFusedLocation.getExtras();
                if (dstExtras == null) {
                    dstExtras = new Bundle();
                    this.mFusedLocation.setExtras(dstExtras);
                }
                dstExtras.putParcelable("noGPSLocation", (Location) srcParcelable);
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
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
        ProviderStats stats = this.mStats.get(provider);
        if (stats != null) {
            stats.available = true;
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        ProviderStats stats = this.mStats.get(provider);
        if (stats != null) {
            stats.available = false;
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        StringBuilder s = new StringBuilder();
        s.append("mEnabled=" + this.mEnabled).append(' ').append(this.mRequest).append('\n');
        s.append("fused=").append(this.mFusedLocation).append('\n');
        s.append(String.format("gps %s\n", this.mGpsLocation));
        s.append("    ").append(this.mStats.get("gps")).append('\n');
        s.append(String.format("net %s\n", this.mNetworkLocation));
        s.append("    ").append(this.mStats.get("network")).append('\n');
        pw.append((CharSequence) s);
    }

    public void switchUser() {
        this.mFusedLocation = null;
        this.mGpsLocation = null;
        this.mNetworkLocation = null;
    }
}
