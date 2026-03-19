package android.app;

import android.app.IAlarmListener;
import android.content.Context;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.WorkSource;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import java.io.IOException;
import libcore.util.ZoneInfoDB;

public class AlarmManager {
    public static final String ACTION_NEXT_ALARM_CLOCK_CHANGED = "android.app.action.NEXT_ALARM_CLOCK_CHANGED";
    public static final int ELAPSED_REALTIME = 3;
    public static final int ELAPSED_REALTIME_WAKEUP = 2;
    public static final int FLAG_ALLOW_WHILE_IDLE = 4;
    public static final int FLAG_ALLOW_WHILE_IDLE_UNRESTRICTED = 8;
    public static final int FLAG_IDLE_UNTIL = 16;
    public static final int FLAG_STANDALONE = 1;
    public static final int FLAG_WAKE_FROM_IDLE = 2;
    public static final long INTERVAL_DAY = 86400000;
    public static final long INTERVAL_FIFTEEN_MINUTES = 900000;
    public static final long INTERVAL_HALF_DAY = 43200000;
    public static final long INTERVAL_HALF_HOUR = 1800000;
    public static final long INTERVAL_HOUR = 3600000;
    public static final int RTC = 1;
    public static final int RTC_WAKEUP = 0;
    private static final String TAG = "AlarmManager";
    public static final long WINDOW_EXACT = 0;
    public static final long WINDOW_HEURISTIC = -1;
    private static ArrayMap<OnAlarmListener, ListenerWrapper> sWrappers;
    private final boolean mAlwaysExact;
    private final Handler mMainThreadHandler;
    private final String mPackageName;
    private final IAlarmManager mService;
    private final int mTargetSdkVersion;

    public interface OnAlarmListener {
        void onAlarm();
    }

    final class ListenerWrapper extends IAlarmListener.Stub implements Runnable {
        IAlarmCompleteListener mCompletion;
        Handler mHandler;
        final OnAlarmListener mListener;

        public ListenerWrapper(OnAlarmListener listener) {
            this.mListener = listener;
        }

        public void setHandler(Handler h) {
            this.mHandler = h;
        }

        public void cancel() {
            try {
                AlarmManager.this.mService.remove(null, this);
                synchronized (AlarmManager.class) {
                    if (AlarmManager.sWrappers != null) {
                        AlarmManager.sWrappers.remove(this.mListener);
                    }
                }
            } catch (RemoteException ex) {
                throw ex.rethrowFromSystemServer();
            }
        }

        @Override
        public void doAlarm(IAlarmCompleteListener alarmManager) {
            this.mCompletion = alarmManager;
            this.mHandler.post(this);
        }

        @Override
        public void run() {
            synchronized (AlarmManager.class) {
                if (AlarmManager.sWrappers != null) {
                    AlarmManager.sWrappers.remove(this.mListener);
                }
            }
            try {
                this.mListener.onAlarm();
            } finally {
                try {
                    this.mCompletion.alarmComplete(this);
                } catch (Exception e) {
                    Log.e(AlarmManager.TAG, "Unable to report completion to Alarm Manager!", e);
                }
            }
        }
    }

    AlarmManager(IAlarmManager service, Context ctx) {
        this.mService = service;
        this.mPackageName = ctx.getPackageName();
        this.mTargetSdkVersion = ctx.getApplicationInfo().targetSdkVersion;
        this.mAlwaysExact = this.mTargetSdkVersion < 19;
        this.mMainThreadHandler = new Handler(ctx.getMainLooper());
    }

    private long legacyExactLength() {
        return this.mAlwaysExact ? 0L : -1L;
    }

    public void set(int type, long triggerAtMillis, PendingIntent operation) throws Throwable {
        setImpl(type, triggerAtMillis, legacyExactLength(), 0L, 0, operation, null, null, null, null, null);
    }

    public void set(int type, long triggerAtMillis, String tag, OnAlarmListener listener, Handler targetHandler) throws Throwable {
        setImpl(type, triggerAtMillis, legacyExactLength(), 0L, 0, null, listener, tag, targetHandler, null, null);
    }

    public void setRepeating(int type, long triggerAtMillis, long intervalMillis, PendingIntent operation) throws Throwable {
        setImpl(type, triggerAtMillis, legacyExactLength(), intervalMillis, 0, operation, null, null, null, null, null);
    }

    public void setWindow(int type, long windowStartMillis, long windowLengthMillis, PendingIntent operation) throws Throwable {
        setImpl(type, windowStartMillis, windowLengthMillis, 0L, 0, operation, null, null, null, null, null);
    }

    public void setWindow(int type, long windowStartMillis, long windowLengthMillis, String tag, OnAlarmListener listener, Handler targetHandler) throws Throwable {
        setImpl(type, windowStartMillis, windowLengthMillis, 0L, 0, null, listener, tag, targetHandler, null, null);
    }

    public void setExact(int type, long triggerAtMillis, PendingIntent operation) throws Throwable {
        setImpl(type, triggerAtMillis, 0L, 0L, 0, operation, null, null, null, null, null);
    }

    public void setExact(int type, long triggerAtMillis, String tag, OnAlarmListener listener, Handler targetHandler) throws Throwable {
        setImpl(type, triggerAtMillis, 0L, 0L, 0, null, listener, tag, targetHandler, null, null);
    }

    public void setIdleUntil(int type, long triggerAtMillis, String tag, OnAlarmListener listener, Handler targetHandler) throws Throwable {
        setImpl(type, triggerAtMillis, 0L, 0L, 16, null, listener, tag, targetHandler, null, null);
    }

    public void setAlarmClock(AlarmClockInfo info, PendingIntent operation) throws Throwable {
        setImpl(0, info.getTriggerTime(), 0L, 0L, 0, operation, null, null, null, null, info);
    }

    public void set(int type, long triggerAtMillis, long windowMillis, long intervalMillis, PendingIntent operation, WorkSource workSource) throws Throwable {
        setImpl(type, triggerAtMillis, windowMillis, intervalMillis, 0, operation, null, null, null, workSource, null);
    }

    public void set(int type, long triggerAtMillis, long windowMillis, long intervalMillis, String tag, OnAlarmListener listener, Handler targetHandler, WorkSource workSource) throws Throwable {
        setImpl(type, triggerAtMillis, windowMillis, intervalMillis, 0, null, listener, tag, targetHandler, workSource, null);
    }

    public void set(int type, long triggerAtMillis, long windowMillis, long intervalMillis, OnAlarmListener listener, Handler targetHandler, WorkSource workSource) throws Throwable {
        setImpl(type, triggerAtMillis, windowMillis, intervalMillis, 0, null, listener, null, targetHandler, workSource, null);
    }

    private void setImpl(int type, long triggerAtMillis, long windowMillis, long intervalMillis, int flags, PendingIntent operation, OnAlarmListener listener, String listenerTag, Handler targetHandler, WorkSource workSource, AlarmClockInfo alarmClock) throws Throwable {
        if (triggerAtMillis < 0) {
            triggerAtMillis = 0;
        }
        ListenerWrapper recipientWrapper = null;
        if (listener != null) {
            synchronized (AlarmManager.class) {
                try {
                    if (sWrappers == null) {
                        sWrappers = new ArrayMap<>();
                    }
                    recipientWrapper = sWrappers.get(listener);
                    if (recipientWrapper == null) {
                        ListenerWrapper recipientWrapper2 = new ListenerWrapper(listener);
                        try {
                            sWrappers.put(listener, recipientWrapper2);
                            recipientWrapper = recipientWrapper2;
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    }
                    Handler handler = targetHandler != null ? targetHandler : this.mMainThreadHandler;
                    recipientWrapper.setHandler(handler);
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        }
        try {
            this.mService.set(this.mPackageName, type, triggerAtMillis, windowMillis, intervalMillis, flags, operation, recipientWrapper, listenerTag, workSource, alarmClock);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void setInexactRepeating(int type, long triggerAtMillis, long intervalMillis, PendingIntent operation) throws Throwable {
        setImpl(type, triggerAtMillis, -1L, intervalMillis, 0, operation, null, null, null, null, null);
    }

    public void setAndAllowWhileIdle(int type, long triggerAtMillis, PendingIntent operation) throws Throwable {
        setImpl(type, triggerAtMillis, -1L, 0L, 4, operation, null, null, null, null, null);
    }

    public void setExactAndAllowWhileIdle(int type, long triggerAtMillis, PendingIntent operation) throws Throwable {
        setImpl(type, triggerAtMillis, 0L, 0L, 4, operation, null, null, null, null, null);
    }

    public void cancel(PendingIntent operation) {
        if (operation == null) {
            if (this.mTargetSdkVersion >= 24) {
                throw new NullPointerException("cancel() called with a null PendingIntent");
            }
            Log.e(TAG, "cancel() called with a null PendingIntent");
        } else {
            try {
                this.mService.remove(operation, null);
            } catch (RemoteException ex) {
                throw ex.rethrowFromSystemServer();
            }
        }
    }

    public void cancel(OnAlarmListener listener) {
        if (listener == null) {
            throw new NullPointerException("cancel() called with a null OnAlarmListener");
        }
        ListenerWrapper wrapper = null;
        synchronized (AlarmManager.class) {
            if (sWrappers != null) {
                ListenerWrapper wrapper2 = sWrappers.get(listener);
                wrapper = wrapper2;
            }
        }
        if (wrapper == null) {
            Log.w(TAG, "Unrecognized alarm listener " + listener);
        } else {
            wrapper.cancel();
        }
    }

    public void setTime(long millis) {
        try {
            this.mService.setTime(millis);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void setTimeZone(String timeZone) {
        if (TextUtils.isEmpty(timeZone)) {
            return;
        }
        if (this.mTargetSdkVersion >= 23) {
            boolean hasTimeZone = false;
            try {
                hasTimeZone = ZoneInfoDB.getInstance().hasTimeZone(timeZone);
            } catch (IOException e) {
            }
            if (!hasTimeZone) {
                throw new IllegalArgumentException("Timezone: " + timeZone + " is not an Olson ID");
            }
        }
        try {
            this.mService.setTimeZone(timeZone);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public long getNextWakeFromIdleTime() {
        try {
            return this.mService.getNextWakeFromIdleTime();
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void cancelPoweroffAlarm(String name) {
        try {
            this.mService.cancelPoweroffAlarm(name);
        } catch (RemoteException e) {
        }
    }

    public AlarmClockInfo getNextAlarmClock() {
        return getNextAlarmClock(UserHandle.myUserId());
    }

    public AlarmClockInfo getNextAlarmClock(int userId) {
        try {
            return this.mService.getNextAlarmClock(userId);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public static final class AlarmClockInfo implements Parcelable {
        public static final Parcelable.Creator<AlarmClockInfo> CREATOR = new Parcelable.Creator<AlarmClockInfo>() {
            @Override
            public AlarmClockInfo createFromParcel(Parcel in) {
                return new AlarmClockInfo(in);
            }

            @Override
            public AlarmClockInfo[] newArray(int size) {
                return new AlarmClockInfo[size];
            }
        };
        private final PendingIntent mShowIntent;
        private final long mTriggerTime;

        public AlarmClockInfo(long triggerTime, PendingIntent showIntent) {
            this.mTriggerTime = triggerTime;
            this.mShowIntent = showIntent;
        }

        AlarmClockInfo(Parcel in) {
            this.mTriggerTime = in.readLong();
            this.mShowIntent = (PendingIntent) in.readParcelable(PendingIntent.class.getClassLoader());
        }

        public long getTriggerTime() {
            return this.mTriggerTime;
        }

        public PendingIntent getShowIntent() {
            return this.mShowIntent;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(this.mTriggerTime);
            dest.writeParcelable(this.mShowIntent, flags);
        }
    }
}
