package com.android.server.location;

import android.app.PendingIntent;
import android.location.Geofence;
import android.location.Location;
import com.android.server.backup.BackupManagerConstants;

public class GeofenceState {
    public static final int FLAG_ENTER = 1;
    public static final int FLAG_EXIT = 2;
    private static final int STATE_INSIDE = 1;
    private static final int STATE_OUTSIDE = 2;
    private static final int STATE_UNKNOWN = 0;
    public final int mAllowedResolutionLevel;
    public final long mExpireAt;
    public final Geofence mFence;
    public final PendingIntent mIntent;
    public final String mPackageName;
    public final int mUid;
    int mState = 0;
    double mDistanceToCenter = Double.MAX_VALUE;
    private final Location mLocation = new Location(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);

    public GeofenceState(Geofence geofence, long j, int i, int i2, String str, PendingIntent pendingIntent) {
        this.mFence = geofence;
        this.mExpireAt = j;
        this.mAllowedResolutionLevel = i;
        this.mUid = i2;
        this.mPackageName = str;
        this.mIntent = pendingIntent;
        this.mLocation.setLatitude(geofence.getLatitude());
        this.mLocation.setLongitude(geofence.getLongitude());
    }

    public int processLocation(Location location) {
        this.mDistanceToCenter = this.mLocation.distanceTo(location);
        int i = this.mState;
        if (this.mDistanceToCenter <= ((double) Math.max(this.mFence.getRadius(), location.getAccuracy()))) {
            this.mState = 1;
            if (i != 1) {
                return 1;
            }
        } else {
            this.mState = 2;
            if (i == 1) {
                return 2;
            }
        }
        return 0;
    }

    public double getDistanceToBoundary() {
        if (Double.compare(this.mDistanceToCenter, Double.MAX_VALUE) == 0) {
            return Double.MAX_VALUE;
        }
        return Math.abs(((double) this.mFence.getRadius()) - this.mDistanceToCenter);
    }

    public String toString() {
        String str;
        switch (this.mState) {
            case 1:
                str = "IN";
                break;
            case 2:
                str = "OUT";
                break;
            default:
                str = "?";
                break;
        }
        return String.format("%s d=%.0f %s", this.mFence.toString(), Double.valueOf(this.mDistanceToCenter), str);
    }
}
