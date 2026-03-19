package android.net;

import android.app.PendingIntent;
import android.net.ConnectivityMetricsEvent;
import android.net.IConnectivityMetricsLogger;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

public class ConnectivityMetricsLogger {
    public static final int COMPONENT_TAG_BLUETOOTH = 1;
    public static final int COMPONENT_TAG_CONNECTIVITY = 0;
    public static final int COMPONENT_TAG_TELECOM = 3;
    public static final int COMPONENT_TAG_TELEPHONY = 4;
    public static final int COMPONENT_TAG_WIFI = 2;
    public static final String CONNECTIVITY_METRICS_LOGGER_SERVICE = "connectivity_metrics_logger";
    public static final String DATA_KEY_EVENTS_COUNT = "count";
    private static final boolean DBG = true;
    public static final int NUMBER_OF_COMPONENTS = 5;
    private static String TAG = "ConnectivityMetricsLogger";
    public static final int TAG_SKIPPED_EVENTS = -1;
    private long mServiceUnblockedTimestampMillis = 0;
    private int mNumSkippedEvents = 0;
    private IConnectivityMetricsLogger mService = IConnectivityMetricsLogger.Stub.asInterface(ServiceManager.getService(CONNECTIVITY_METRICS_LOGGER_SERVICE));

    public void logEvent(long timestamp, int componentTag, int eventTag, Parcelable data) {
        long result;
        if (this.mService == null) {
            Log.d(TAG, "logEvent(" + componentTag + "," + eventTag + ") Service not ready");
            return;
        }
        if (this.mServiceUnblockedTimestampMillis > 0 && System.currentTimeMillis() < this.mServiceUnblockedTimestampMillis) {
            this.mNumSkippedEvents++;
            return;
        }
        ConnectivityMetricsEvent skippedEventsEvent = null;
        if (this.mNumSkippedEvents > 0) {
            Bundle b = new Bundle();
            b.putInt(DATA_KEY_EVENTS_COUNT, this.mNumSkippedEvents);
            skippedEventsEvent = new ConnectivityMetricsEvent(this.mServiceUnblockedTimestampMillis, componentTag, -1, b);
            this.mServiceUnblockedTimestampMillis = 0L;
        }
        ConnectivityMetricsEvent event = new ConnectivityMetricsEvent(timestamp, componentTag, eventTag, data);
        try {
            if (skippedEventsEvent == null) {
                result = this.mService.logEvent(event);
            } else {
                result = this.mService.logEvents(new ConnectivityMetricsEvent[]{skippedEventsEvent, event});
            }
            if (result == 0) {
                this.mNumSkippedEvents = 0;
                return;
            }
            this.mNumSkippedEvents++;
            if (result <= 0) {
                return;
            }
            this.mServiceUnblockedTimestampMillis = result;
        } catch (RemoteException e) {
            Log.e(TAG, "Error logging event " + e.getMessage());
        }
    }

    public ConnectivityMetricsEvent[] getEvents(ConnectivityMetricsEvent.Reference reference) {
        try {
            return this.mService.getEvents(reference);
        } catch (RemoteException ex) {
            Log.e(TAG, "IConnectivityMetricsLogger.getEvents: " + ex);
            return null;
        }
    }

    public boolean register(PendingIntent newEventsIntent) {
        try {
            return this.mService.register(newEventsIntent);
        } catch (RemoteException ex) {
            Log.e(TAG, "IConnectivityMetricsLogger.register: " + ex);
            return false;
        }
    }

    public boolean unregister(PendingIntent newEventsIntent) {
        try {
            this.mService.unregister(newEventsIntent);
            return true;
        } catch (RemoteException ex) {
            Log.e(TAG, "IConnectivityMetricsLogger.unregister: " + ex);
            return false;
        }
    }
}
