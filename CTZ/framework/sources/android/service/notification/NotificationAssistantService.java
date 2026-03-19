package android.service.notification;

import android.annotation.SystemApi;
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

@SystemApi
public abstract class NotificationAssistantService extends NotificationListenerService {
    public static final String SERVICE_INTERFACE = "android.service.notification.NotificationAssistantService";
    private static final String TAG = "NotificationAssistants";
    protected Handler mHandler;

    public abstract Adjustment onNotificationEnqueued(StatusBarNotification statusBarNotification);

    public abstract void onNotificationSnoozedUntilContext(StatusBarNotification statusBarNotification, String str);

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        this.mHandler = new MyHandler(getContext().getMainLooper());
    }

    @Override
    public final IBinder onBind(Intent intent) {
        if (this.mWrapper == null) {
            this.mWrapper = new NotificationAssistantServiceWrapper();
        }
        return this.mWrapper;
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification statusBarNotification, NotificationListenerService.RankingMap rankingMap, NotificationStats notificationStats, int i) {
        onNotificationRemoved(statusBarNotification, rankingMap, i);
    }

    public final void adjustNotification(Adjustment adjustment) {
        if (isBound()) {
            try {
                getNotificationInterface().applyAdjustmentFromAssistant(this.mWrapper, adjustment);
            } catch (RemoteException e) {
                Log.v(TAG, "Unable to contact notification manager", e);
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public final void adjustNotifications(List<Adjustment> list) {
        if (isBound()) {
            try {
                getNotificationInterface().applyAdjustmentsFromAssistant(this.mWrapper, list);
            } catch (RemoteException e) {
                Log.v(TAG, "Unable to contact notification manager", e);
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public final void unsnoozeNotification(String str) {
        if (isBound()) {
            try {
                getNotificationInterface().unsnoozeNotificationFromAssistant(this.mWrapper, str);
            } catch (RemoteException e) {
                Log.v(TAG, "Unable to contact notification manager", e);
            }
        }
    }

    private class NotificationAssistantServiceWrapper extends NotificationListenerService.NotificationListenerWrapper {
        private NotificationAssistantServiceWrapper() {
            super();
        }

        @Override
        public void onNotificationEnqueued(IStatusBarNotificationHolder iStatusBarNotificationHolder) {
            try {
                StatusBarNotification statusBarNotification = iStatusBarNotificationHolder.get();
                SomeArgs someArgsObtain = SomeArgs.obtain();
                someArgsObtain.arg1 = statusBarNotification;
                NotificationAssistantService.this.mHandler.obtainMessage(1, someArgsObtain).sendToTarget();
            } catch (RemoteException e) {
                Log.w(NotificationAssistantService.TAG, "onNotificationEnqueued: Error receiving StatusBarNotification", e);
            }
        }

        @Override
        public void onNotificationSnoozedUntilContext(IStatusBarNotificationHolder iStatusBarNotificationHolder, String str) throws RemoteException {
            try {
                StatusBarNotification statusBarNotification = iStatusBarNotificationHolder.get();
                SomeArgs someArgsObtain = SomeArgs.obtain();
                someArgsObtain.arg1 = statusBarNotification;
                someArgsObtain.arg2 = str;
                NotificationAssistantService.this.mHandler.obtainMessage(2, someArgsObtain).sendToTarget();
            } catch (RemoteException e) {
                Log.w(NotificationAssistantService.TAG, "onNotificationSnoozed: Error receiving StatusBarNotification", e);
            }
        }
    }

    private final class MyHandler extends Handler {
        public static final int MSG_ON_NOTIFICATION_ENQUEUED = 1;
        public static final int MSG_ON_NOTIFICATION_SNOOZED = 2;

        public MyHandler(Looper looper) {
            super(looper, null, false);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    SomeArgs someArgs = (SomeArgs) message.obj;
                    StatusBarNotification statusBarNotification = (StatusBarNotification) someArgs.arg1;
                    someArgs.recycle();
                    Adjustment adjustmentOnNotificationEnqueued = NotificationAssistantService.this.onNotificationEnqueued(statusBarNotification);
                    if (adjustmentOnNotificationEnqueued == null || !NotificationAssistantService.this.isBound()) {
                        return;
                    }
                    try {
                        NotificationAssistantService.this.getNotificationInterface().applyEnqueuedAdjustmentFromAssistant(NotificationAssistantService.this.mWrapper, adjustmentOnNotificationEnqueued);
                        return;
                    } catch (RemoteException e) {
                        Log.v(NotificationAssistantService.TAG, "Unable to contact notification manager", e);
                        throw e.rethrowFromSystemServer();
                    }
                case 2:
                    SomeArgs someArgs2 = (SomeArgs) message.obj;
                    StatusBarNotification statusBarNotification2 = (StatusBarNotification) someArgs2.arg1;
                    String str = (String) someArgs2.arg2;
                    someArgs2.recycle();
                    NotificationAssistantService.this.onNotificationSnoozedUntilContext(statusBarNotification2, str);
                    return;
                default:
                    return;
            }
        }
    }
}
