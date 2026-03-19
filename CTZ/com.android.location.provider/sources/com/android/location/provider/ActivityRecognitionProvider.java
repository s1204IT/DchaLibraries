package com.android.location.provider;

import android.hardware.location.IActivityRecognitionHardware;
import android.hardware.location.IActivityRecognitionHardwareSink;
import android.os.RemoteException;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public final class ActivityRecognitionProvider {
    public static final String ACTIVITY_IN_VEHICLE = "android.activity_recognition.in_vehicle";
    public static final String ACTIVITY_ON_BICYCLE = "android.activity_recognition.on_bicycle";
    public static final String ACTIVITY_RUNNING = "android.activity_recognition.running";
    public static final String ACTIVITY_STILL = "android.activity_recognition.still";
    public static final String ACTIVITY_TILTING = "android.activity_recognition.tilting";
    public static final String ACTIVITY_WALKING = "android.activity_recognition.walking";
    public static final int EVENT_TYPE_ENTER = 1;
    public static final int EVENT_TYPE_EXIT = 2;
    public static final int EVENT_TYPE_FLUSH_COMPLETE = 0;
    private final IActivityRecognitionHardware mService;
    private final HashSet<Sink> mSinkSet = new HashSet<>();

    public interface Sink {
        void onActivityChanged(ActivityChangedEvent activityChangedEvent);
    }

    public ActivityRecognitionProvider(IActivityRecognitionHardware iActivityRecognitionHardware) throws RemoteException {
        Preconditions.checkNotNull(iActivityRecognitionHardware);
        this.mService = iActivityRecognitionHardware;
        this.mService.registerSink(new SinkTransport());
    }

    public String[] getSupportedActivities() throws RemoteException {
        return this.mService.getSupportedActivities();
    }

    public boolean isActivitySupported(String str) throws RemoteException {
        return this.mService.isActivitySupported(str);
    }

    public void registerSink(Sink sink) {
        Preconditions.checkNotNull(sink);
        synchronized (this.mSinkSet) {
            this.mSinkSet.add(sink);
        }
    }

    public void unregisterSink(Sink sink) {
        Preconditions.checkNotNull(sink);
        synchronized (this.mSinkSet) {
            this.mSinkSet.remove(sink);
        }
    }

    public boolean enableActivityEvent(String str, int i, long j) throws RemoteException {
        return this.mService.enableActivityEvent(str, i, j);
    }

    public boolean disableActivityEvent(String str, int i) throws RemoteException {
        return this.mService.disableActivityEvent(str, i);
    }

    public boolean flush() throws RemoteException {
        return this.mService.flush();
    }

    private final class SinkTransport extends IActivityRecognitionHardwareSink.Stub {
        private SinkTransport() {
        }

        public void onActivityChanged(android.hardware.location.ActivityChangedEvent activityChangedEvent) {
            synchronized (ActivityRecognitionProvider.this.mSinkSet) {
                if (ActivityRecognitionProvider.this.mSinkSet.isEmpty()) {
                    return;
                }
                ArrayList arrayList = new ArrayList(ActivityRecognitionProvider.this.mSinkSet);
                ArrayList arrayList2 = new ArrayList();
                for (android.hardware.location.ActivityRecognitionEvent activityRecognitionEvent : activityChangedEvent.getActivityRecognitionEvents()) {
                    arrayList2.add(new ActivityRecognitionEvent(activityRecognitionEvent.getActivity(), activityRecognitionEvent.getEventType(), activityRecognitionEvent.getTimestampNs()));
                }
                ActivityChangedEvent activityChangedEvent2 = new ActivityChangedEvent(arrayList2);
                Iterator it = arrayList.iterator();
                while (it.hasNext()) {
                    ((Sink) it.next()).onActivityChanged(activityChangedEvent2);
                }
            }
        }
    }
}
