package android.service.notification;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.service.notification.NotificationListenerService;
import android.util.Log;
import com.android.internal.os.SomeArgs;
import java.util.List;

public abstract class NotificationRankerService extends NotificationListenerService {
    public static final int REASON_APP_CANCEL = 8;
    public static final int REASON_APP_CANCEL_ALL = 9;
    public static final int REASON_DELEGATE_CANCEL = 2;
    public static final int REASON_DELEGATE_CANCEL_ALL = 3;
    public static final int REASON_DELEGATE_CLICK = 1;
    public static final int REASON_DELEGATE_ERROR = 4;
    public static final int REASON_GROUP_OPTIMIZATION = 13;
    public static final int REASON_GROUP_SUMMARY_CANCELED = 12;
    public static final int REASON_LISTENER_CANCEL = 10;
    public static final int REASON_LISTENER_CANCEL_ALL = 11;
    public static final int REASON_PACKAGE_BANNED = 7;
    public static final int REASON_PACKAGE_CHANGED = 5;
    public static final int REASON_PACKAGE_SUSPENDED = 14;
    public static final int REASON_PROFILE_TURNED_OFF = 15;
    public static final int REASON_UNAUTOBUNDLED = 16;
    public static final int REASON_USER_STOPPED = 6;
    public static final String SERVICE_INTERFACE = "android.service.notification.NotificationRankerService";
    private static final String TAG = "NotificationRankers";
    private Handler mHandler;

    public abstract Adjustment onNotificationEnqueued(StatusBarNotification statusBarNotification, int i, boolean z);

    @Override
    public void registerAsSystemService(Context context, ComponentName componentName, int currentUser) {
        throw new UnsupportedOperationException("the ranker lifecycle is managed by the system.");
    }

    @Override
    public void unregisterAsSystemService() {
        throw new UnsupportedOperationException("the ranker lifecycle is managed by the system.");
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        this.mHandler = new MyHandler(getContext().getMainLooper());
    }

    @Override
    public final IBinder onBind(Intent intent) {
        NotificationRankingServiceWrapper notificationRankingServiceWrapper = null;
        if (this.mWrapper == null) {
            this.mWrapper = new NotificationRankingServiceWrapper(this, notificationRankingServiceWrapper);
        }
        return this.mWrapper;
    }

    public void onNotificationVisibilityChanged(String key, long time, boolean visible) {
    }

    public void onNotificationClick(String key, long time) {
    }

    public void onNotificationActionClick(String key, long time, int actionIndex) {
    }

    public void onNotificationRemoved(String key, long time, int reason) {
    }

    public final void adjustNotification(Adjustment adjustment) {
        if (isBound()) {
            try {
                getNotificationInterface().applyAdjustmentFromRankerService(this.mWrapper, adjustment);
            } catch (RemoteException ex) {
                Log.v(TAG, "Unable to contact notification manager", ex);
            }
        }
    }

    public final void adjustNotifications(List<Adjustment> adjustments) {
        if (isBound()) {
            try {
                getNotificationInterface().applyAdjustmentsFromRankerService(this.mWrapper, adjustments);
            } catch (RemoteException ex) {
                Log.v(TAG, "Unable to contact notification manager", ex);
            }
        }
    }

    private class NotificationRankingServiceWrapper extends NotificationListenerService.NotificationListenerWrapper {
        NotificationRankingServiceWrapper(NotificationRankerService this$0, NotificationRankingServiceWrapper notificationRankingServiceWrapper) {
            this();
        }

        private NotificationRankingServiceWrapper() {
            super();
        }

        @Override
        public void onNotificationEnqueued(IStatusBarNotificationHolder sbnHolder, int importance, boolean user) {
            try {
                StatusBarNotification sbn = sbnHolder.get();
                SomeArgs args = SomeArgs.obtain();
                args.arg1 = sbn;
                args.argi1 = importance;
                args.argi2 = user ? 1 : 0;
                NotificationRankerService.this.mHandler.obtainMessage(1, args).sendToTarget();
            } catch (RemoteException e) {
                Log.w(NotificationRankerService.TAG, "onNotificationEnqueued: Error receiving StatusBarNotification", e);
            }
        }

        @Override
        public void onNotificationVisibilityChanged(String key, long time, boolean visible) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = key;
            args.arg2 = Long.valueOf(time);
            args.argi1 = visible ? 1 : 0;
            NotificationRankerService.this.mHandler.obtainMessage(2, args).sendToTarget();
        }

        @Override
        public void onNotificationClick(String key, long time) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = key;
            args.arg2 = Long.valueOf(time);
            NotificationRankerService.this.mHandler.obtainMessage(3, args).sendToTarget();
        }

        @Override
        public void onNotificationActionClick(String key, long time, int actionIndex) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = key;
            args.arg2 = Long.valueOf(time);
            args.argi1 = actionIndex;
            NotificationRankerService.this.mHandler.obtainMessage(4, args).sendToTarget();
        }

        @Override
        public void onNotificationRemovedReason(String key, long time, int reason) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = key;
            args.arg2 = Long.valueOf(time);
            args.argi1 = reason;
            NotificationRankerService.this.mHandler.obtainMessage(5, args).sendToTarget();
        }
    }

    private final class MyHandler extends Handler {
        public static final int MSG_ON_NOTIFICATION_ACTION_CLICK = 4;
        public static final int MSG_ON_NOTIFICATION_CLICK = 3;
        public static final int MSG_ON_NOTIFICATION_ENQUEUED = 1;
        public static final int MSG_ON_NOTIFICATION_REMOVED_REASON = 5;
        public static final int MSG_ON_NOTIFICATION_VISIBILITY_CHANGED = 2;

        public MyHandler(Looper looper) {
            super(looper, null, false);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    SomeArgs args = (SomeArgs) msg.obj;
                    StatusBarNotification sbn = (StatusBarNotification) args.arg1;
                    int importance = args.argi1;
                    boolean user = args.argi2 == 1;
                    args.recycle();
                    Adjustment adjustment = NotificationRankerService.this.onNotificationEnqueued(sbn, importance, user);
                    if (adjustment != null) {
                        NotificationRankerService.this.adjustNotification(adjustment);
                    }
                    break;
                case 2:
                    SomeArgs args2 = (SomeArgs) msg.obj;
                    String key = (String) args2.arg1;
                    long time = ((Long) args2.arg2).longValue();
                    boolean visible = args2.argi1 == 1;
                    args2.recycle();
                    NotificationRankerService.this.onNotificationVisibilityChanged(key, time, visible);
                    break;
                case 3:
                    SomeArgs args3 = (SomeArgs) msg.obj;
                    String key2 = (String) args3.arg1;
                    long time2 = ((Long) args3.arg2).longValue();
                    args3.recycle();
                    NotificationRankerService.this.onNotificationClick(key2, time2);
                    break;
                case 4:
                    SomeArgs args4 = (SomeArgs) msg.obj;
                    String key3 = (String) args4.arg1;
                    long time3 = ((Long) args4.arg2).longValue();
                    int actionIndex = args4.argi1;
                    args4.recycle();
                    NotificationRankerService.this.onNotificationActionClick(key3, time3, actionIndex);
                    break;
                case 5:
                    SomeArgs args5 = (SomeArgs) msg.obj;
                    String key4 = (String) args5.arg1;
                    long time4 = ((Long) args5.arg2).longValue();
                    int reason = args5.argi1;
                    args5.recycle();
                    NotificationRankerService.this.onNotificationRemoved(key4, time4, reason);
                    break;
            }
        }
    }
}
