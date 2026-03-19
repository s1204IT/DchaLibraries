package android.app;

import android.annotation.SystemApi;
import android.app.IAlarmListener;
import android.content.Context;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.WorkSource;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.proto.ProtoOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
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
    public static final int PRE_SCHEDULE_POWER_OFF_ALARM = 7;
    public static final int RTC = 1;
    public static final int RTC_WAKEUP = 0;
    private static final String TAG = "AlarmManager";
    public static final long WINDOW_EXACT = 0;
    public static final long WINDOW_HEURISTIC = -1;
    private static ArrayMap<OnAlarmListener, ListenerWrapper> sWrappers;
    private final boolean mAlwaysExact;
    private final Context mContext;
    private final Handler mMainThreadHandler;
    private final String mPackageName;
    private final IAlarmManager mService;
    private final int mTargetSdkVersion;

    @Retention(RetentionPolicy.SOURCE)
    public @interface AlarmType {
    }

    public interface OnAlarmListener {
        void onAlarm();
    }

    final class ListenerWrapper extends IAlarmListener.Stub implements Runnable {
        IAlarmCompleteListener mCompletion;
        Handler mHandler;
        final OnAlarmListener mListener;

        public ListenerWrapper(OnAlarmListener onAlarmListener) {
            this.mListener = onAlarmListener;
        }

        public void setHandler(Handler handler) {
            this.mHandler = handler;
        }

        public void cancel() {
            try {
                AlarmManager.this.mService.remove(null, this);
                synchronized (AlarmManager.class) {
                    if (AlarmManager.sWrappers != null) {
                        AlarmManager.sWrappers.remove(this.mListener);
                    }
                }
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        @Override
        public void doAlarm(IAlarmCompleteListener iAlarmCompleteListener) {
            this.mCompletion = iAlarmCompleteListener;
            synchronized (AlarmManager.class) {
                if (AlarmManager.sWrappers != null) {
                    AlarmManager.sWrappers.remove(this.mListener);
                }
            }
            this.mHandler.post(this);
        }

        @Override
        public void run() {
            try {
                this.mListener.onAlarm();
                try {
                    this.mCompletion.alarmComplete(this);
                } catch (Exception e) {
                    Log.e(AlarmManager.TAG, "Unable to report completion to Alarm Manager!", e);
                }
            } catch (Throwable th) {
                try {
                    this.mCompletion.alarmComplete(this);
                } catch (Exception e2) {
                    Log.e(AlarmManager.TAG, "Unable to report completion to Alarm Manager!", e2);
                }
                throw th;
            }
        }
    }

    AlarmManager(IAlarmManager iAlarmManager, Context context) {
        this.mService = iAlarmManager;
        this.mContext = context;
        this.mPackageName = context.getPackageName();
        this.mTargetSdkVersion = context.getApplicationInfo().targetSdkVersion;
        this.mAlwaysExact = this.mTargetSdkVersion < 19;
        this.mMainThreadHandler = new Handler(context.getMainLooper());
    }

    private long legacyExactLength() {
        return this.mAlwaysExact ? 0L : -1L;
    }

    public void set(int i, long j, PendingIntent pendingIntent) {
        setImpl(i, j, legacyExactLength(), 0L, 0, pendingIntent, null, null, null, null, null);
    }

    public void set(int i, long j, String str, OnAlarmListener onAlarmListener, Handler handler) {
        setImpl(i, j, legacyExactLength(), 0L, 0, null, onAlarmListener, str, handler, null, null);
    }

    public void setRepeating(int i, long j, long j2, PendingIntent pendingIntent) {
        setImpl(i, j, legacyExactLength(), j2, 0, pendingIntent, null, null, null, null, null);
    }

    public void setWindow(int i, long j, long j2, PendingIntent pendingIntent) {
        setImpl(i, j, j2, 0L, 0, pendingIntent, null, null, null, null, null);
    }

    public void setWindow(int i, long j, long j2, String str, OnAlarmListener onAlarmListener, Handler handler) {
        setImpl(i, j, j2, 0L, 0, null, onAlarmListener, str, handler, null, null);
    }

    public void setExact(int i, long j, PendingIntent pendingIntent) {
        setImpl(i, j, 0L, 0L, 0, pendingIntent, null, null, null, null, null);
    }

    public void setExact(int i, long j, String str, OnAlarmListener onAlarmListener, Handler handler) {
        setImpl(i, j, 0L, 0L, 0, null, onAlarmListener, str, handler, null, null);
    }

    public void setIdleUntil(int i, long j, String str, OnAlarmListener onAlarmListener, Handler handler) {
        setImpl(i, j, 0L, 0L, 16, null, onAlarmListener, str, handler, null, null);
    }

    public void setAlarmClock(AlarmClockInfo alarmClockInfo, PendingIntent pendingIntent) {
        setImpl(0, alarmClockInfo.getTriggerTime(), 0L, 0L, 0, pendingIntent, null, null, null, null, alarmClockInfo);
    }

    @SystemApi
    public void set(int i, long j, long j2, long j3, PendingIntent pendingIntent, WorkSource workSource) {
        setImpl(i, j, j2, j3, 0, pendingIntent, null, null, null, workSource, null);
    }

    public void set(int i, long j, long j2, long j3, String str, OnAlarmListener onAlarmListener, Handler handler, WorkSource workSource) {
        setImpl(i, j, j2, j3, 0, null, onAlarmListener, str, handler, workSource, null);
    }

    @SystemApi
    public void set(int i, long j, long j2, long j3, OnAlarmListener onAlarmListener, Handler handler, WorkSource workSource) {
        setImpl(i, j, j2, j3, 0, null, onAlarmListener, null, handler, workSource, null);
    }

    private void setImpl(int i, long j, long j2, long j3, int i2, PendingIntent pendingIntent, OnAlarmListener onAlarmListener, String str, Handler handler, WorkSource workSource, AlarmClockInfo alarmClockInfo) {
        Handler handler2;
        long j4 = j < 0 ? 0L : j;
        ListenerWrapper listenerWrapper = null;
        if (onAlarmListener != null) {
            synchronized (AlarmManager.class) {
                if (sWrappers == null) {
                    sWrappers = new ArrayMap<>();
                }
                listenerWrapper = sWrappers.get(onAlarmListener);
                if (listenerWrapper == null) {
                    listenerWrapper = new ListenerWrapper(onAlarmListener);
                    sWrappers.put(onAlarmListener, listenerWrapper);
                }
            }
            if (handler == null) {
                handler2 = this.mMainThreadHandler;
            } else {
                handler2 = handler;
            }
            listenerWrapper.setHandler(handler2);
        }
        try {
            this.mService.set(this.mPackageName, i, j4, j2, j3, i2, pendingIntent, listenerWrapper, str, workSource, alarmClockInfo);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setInexactRepeating(int i, long j, long j2, PendingIntent pendingIntent) {
        setImpl(i, j, -1L, j2, 0, pendingIntent, null, null, null, null, null);
    }

    public void setAndAllowWhileIdle(int i, long j, PendingIntent pendingIntent) {
        setImpl(i, j, -1L, 0L, 4, pendingIntent, null, null, null, null, null);
    }

    public void setExactAndAllowWhileIdle(int i, long j, PendingIntent pendingIntent) {
        setImpl(i, j, 0L, 0L, 4, pendingIntent, null, null, null, null, null);
    }

    public void cancel(PendingIntent pendingIntent) {
        if (pendingIntent == null) {
            if (this.mTargetSdkVersion >= 24) {
                throw new NullPointerException("cancel() called with a null PendingIntent");
            }
            Log.e(TAG, "cancel() called with a null PendingIntent");
        } else {
            try {
                this.mService.remove(pendingIntent, null);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void cancel(OnAlarmListener onAlarmListener) {
        if (onAlarmListener == null) {
            throw new NullPointerException("cancel() called with a null OnAlarmListener");
        }
        ListenerWrapper listenerWrapper = null;
        synchronized (AlarmManager.class) {
            if (sWrappers != null) {
                listenerWrapper = sWrappers.get(onAlarmListener);
            }
        }
        if (listenerWrapper == null) {
            Log.w(TAG, "Unrecognized alarm listener " + onAlarmListener);
            return;
        }
        listenerWrapper.cancel();
    }

    public void setTime(long j) {
        try {
            this.mService.setTime(j);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setTimeZone(String str) {
        if (TextUtils.isEmpty(str)) {
            return;
        }
        if (this.mTargetSdkVersion >= 23) {
            boolean zHasTimeZone = false;
            try {
                zHasTimeZone = ZoneInfoDB.getInstance().hasTimeZone(str);
            } catch (IOException e) {
            }
            if (!zHasTimeZone) {
                throw new IllegalArgumentException("Timezone: " + str + " is not an Olson ID");
            }
        }
        try {
            this.mService.setTimeZone(str);
        } catch (RemoteException e2) {
            throw e2.rethrowFromSystemServer();
        }
    }

    public void cancelPoweroffAlarm(String str) {
        try {
            this.mService.cancelPoweroffAlarm(str);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to cancel power off Alarm Manager!", e);
        }
    }

    public long getNextWakeFromIdleTime() {
        try {
            return this.mService.getNextWakeFromIdleTime();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public AlarmClockInfo getNextAlarmClock() {
        return getNextAlarmClock(this.mContext.getUserId());
    }

    public AlarmClockInfo getNextAlarmClock(int i) {
        try {
            return this.mService.getNextAlarmClock(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static final class AlarmClockInfo implements Parcelable {
        public static final Parcelable.Creator<AlarmClockInfo> CREATOR = new Parcelable.Creator<AlarmClockInfo>() {
            @Override
            public AlarmClockInfo createFromParcel(Parcel parcel) {
                return new AlarmClockInfo(parcel);
            }

            @Override
            public AlarmClockInfo[] newArray(int i) {
                return new AlarmClockInfo[i];
            }
        };
        private final PendingIntent mShowIntent;
        private final long mTriggerTime;

        public AlarmClockInfo(long j, PendingIntent pendingIntent) {
            this.mTriggerTime = j;
            this.mShowIntent = pendingIntent;
        }

        AlarmClockInfo(Parcel parcel) {
            this.mTriggerTime = parcel.readLong();
            this.mShowIntent = (PendingIntent) parcel.readParcelable(PendingIntent.class.getClassLoader());
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
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeLong(this.mTriggerTime);
            parcel.writeParcelable(this.mShowIntent, i);
        }

        public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
            long jStart = protoOutputStream.start(j);
            protoOutputStream.write(1112396529665L, this.mTriggerTime);
            this.mShowIntent.writeToProto(protoOutputStream, 1146756268034L);
            protoOutputStream.end(jStart);
        }
    }
}
