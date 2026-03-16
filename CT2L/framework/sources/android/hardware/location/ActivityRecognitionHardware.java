package android.hardware.location;

import android.content.Context;
import android.hardware.location.IActivityRecognitionHardware;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

public class ActivityRecognitionHardware extends IActivityRecognitionHardware.Stub {
    private static final String HARDWARE_PERMISSION = "android.permission.LOCATION_HARDWARE";
    private static final int INVALID_ACTIVITY_TYPE = -1;
    private static final int NATIVE_SUCCESS_RESULT = 0;
    private static final String TAG = "ActivityRecognitionHardware";
    private static ActivityRecognitionHardware sSingletonInstance = null;
    private static final Object sSingletonInstanceLock = new Object();
    private final Context mContext;
    private final RemoteCallbackList<IActivityRecognitionHardwareSink> mSinks = new RemoteCallbackList<>();
    private final String[] mSupportedActivities;

    private static native void nativeClassInit();

    private native int nativeDisableActivityEvent(int i, int i2);

    private native int nativeEnableActivityEvent(int i, int i2, long j);

    private native int nativeFlush();

    private native String[] nativeGetSupportedActivities();

    private native void nativeInitialize();

    private static native boolean nativeIsSupported();

    private native void nativeRelease();

    static {
        nativeClassInit();
    }

    private static class Event {
        public int activity;
        public long timestamp;
        public int type;

        private Event() {
        }
    }

    private ActivityRecognitionHardware(Context context) {
        nativeInitialize();
        this.mContext = context;
        this.mSupportedActivities = fetchSupportedActivities();
    }

    public static ActivityRecognitionHardware getInstance(Context context) {
        ActivityRecognitionHardware activityRecognitionHardware;
        synchronized (sSingletonInstanceLock) {
            if (sSingletonInstance == null) {
                sSingletonInstance = new ActivityRecognitionHardware(context);
            }
            activityRecognitionHardware = sSingletonInstance;
        }
        return activityRecognitionHardware;
    }

    public static boolean isSupported() {
        return nativeIsSupported();
    }

    @Override
    public String[] getSupportedActivities() {
        checkPermissions();
        return this.mSupportedActivities;
    }

    @Override
    public boolean isActivitySupported(String activity) {
        checkPermissions();
        int activityType = getActivityType(activity);
        return activityType != -1;
    }

    @Override
    public boolean registerSink(IActivityRecognitionHardwareSink sink) {
        checkPermissions();
        return this.mSinks.register(sink);
    }

    @Override
    public boolean unregisterSink(IActivityRecognitionHardwareSink sink) {
        checkPermissions();
        return this.mSinks.unregister(sink);
    }

    @Override
    public boolean enableActivityEvent(String activity, int eventType, long reportLatencyNs) {
        checkPermissions();
        int activityType = getActivityType(activity);
        if (activityType == -1) {
            return false;
        }
        int result = nativeEnableActivityEvent(activityType, eventType, reportLatencyNs);
        return result == 0;
    }

    @Override
    public boolean disableActivityEvent(String activity, int eventType) {
        checkPermissions();
        int activityType = getActivityType(activity);
        if (activityType == -1) {
            return false;
        }
        int result = nativeDisableActivityEvent(activityType, eventType);
        return result == 0;
    }

    @Override
    public boolean flush() {
        checkPermissions();
        int result = nativeFlush();
        return result == 0;
    }

    private void onActivityChanged(Event[] events) {
        if (events == null || events.length == 0) {
            Log.d(TAG, "No events to broadcast for onActivityChanged.");
            return;
        }
        int eventsLength = events.length;
        ActivityRecognitionEvent[] activityRecognitionEventArray = new ActivityRecognitionEvent[eventsLength];
        for (int i = 0; i < eventsLength; i++) {
            Event event = events[i];
            String activityName = getActivityName(event.activity);
            activityRecognitionEventArray[i] = new ActivityRecognitionEvent(activityName, event.type, event.timestamp);
        }
        ActivityChangedEvent activityChangedEvent = new ActivityChangedEvent(activityRecognitionEventArray);
        int size = this.mSinks.beginBroadcast();
        for (int i2 = 0; i2 < size; i2++) {
            IActivityRecognitionHardwareSink sink = (IActivityRecognitionHardwareSink) this.mSinks.getBroadcastItem(i2);
            try {
                sink.onActivityChanged(activityChangedEvent);
            } catch (RemoteException e) {
                Log.e(TAG, "Error delivering activity changed event.", e);
            }
        }
        this.mSinks.finishBroadcast();
    }

    private String getActivityName(int activityType) {
        if (activityType >= 0 && activityType < this.mSupportedActivities.length) {
            return this.mSupportedActivities[activityType];
        }
        String message = String.format("Invalid ActivityType: %d, SupportedActivities: %d", Integer.valueOf(activityType), Integer.valueOf(this.mSupportedActivities.length));
        Log.e(TAG, message);
        return null;
    }

    private int getActivityType(String activity) {
        if (TextUtils.isEmpty(activity)) {
            return -1;
        }
        int supportedActivitiesLength = this.mSupportedActivities.length;
        for (int i = 0; i < supportedActivitiesLength; i++) {
            if (activity.equals(this.mSupportedActivities[i])) {
                return i;
            }
        }
        return -1;
    }

    private void checkPermissions() {
        String message = String.format("Permission '%s' not granted to access ActivityRecognitionHardware", "android.permission.LOCATION_HARDWARE");
        this.mContext.enforceCallingPermission("android.permission.LOCATION_HARDWARE", message);
    }

    private String[] fetchSupportedActivities() {
        String[] supportedActivities = nativeGetSupportedActivities();
        return supportedActivities != null ? supportedActivities : new String[0];
    }
}
