package android.service.notification;

import android.annotation.SystemApi;
import android.app.INotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.service.notification.IConditionProvider;
import android.util.Log;

public abstract class ConditionProviderService extends Service {
    public static final String EXTRA_RULE_ID = "android.service.notification.extra.RULE_ID";
    public static final String META_DATA_CONFIGURATION_ACTIVITY = "android.service.zen.automatic.configurationActivity";
    public static final String META_DATA_RULE_INSTANCE_LIMIT = "android.service.zen.automatic.ruleInstanceLimit";
    public static final String META_DATA_RULE_TYPE = "android.service.zen.automatic.ruleType";
    public static final String SERVICE_INTERFACE = "android.service.notification.ConditionProviderService";
    private final String TAG = ConditionProviderService.class.getSimpleName() + "[" + getClass().getSimpleName() + "]";
    private final H mHandler = new H();
    private INotificationManager mNoMan;
    private Provider mProvider;

    public abstract void onConnected();

    public abstract void onSubscribe(Uri uri);

    public abstract void onUnsubscribe(Uri uri);

    @SystemApi
    public void onRequestConditions(int i) {
    }

    private final INotificationManager getNotificationInterface() {
        if (this.mNoMan == null) {
            this.mNoMan = INotificationManager.Stub.asInterface(ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        }
        return this.mNoMan;
    }

    public static final void requestRebind(ComponentName componentName) {
        try {
            INotificationManager.Stub.asInterface(ServiceManager.getService(Context.NOTIFICATION_SERVICE)).requestBindProvider(componentName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public final void requestUnbind() {
        try {
            getNotificationInterface().requestUnbindProvider(this.mProvider);
            this.mProvider = null;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public final void notifyCondition(Condition condition) {
        if (condition == null) {
            return;
        }
        notifyConditions(condition);
    }

    public final void notifyConditions(Condition... conditionArr) {
        if (!isBound() || conditionArr == null) {
            return;
        }
        try {
            getNotificationInterface().notifyConditions(getPackageName(), this.mProvider, conditionArr);
        } catch (RemoteException e) {
            Log.v(this.TAG, "Unable to contact notification manager", e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (this.mProvider == null) {
            this.mProvider = new Provider();
        }
        return this.mProvider;
    }

    public boolean isBound() {
        if (this.mProvider == null) {
            Log.w(this.TAG, "Condition provider service not yet bound.");
            return false;
        }
        return true;
    }

    private final class Provider extends IConditionProvider.Stub {
        private Provider() {
        }

        @Override
        public void onConnected() {
            ConditionProviderService.this.mHandler.obtainMessage(1).sendToTarget();
        }

        @Override
        public void onSubscribe(Uri uri) {
            ConditionProviderService.this.mHandler.obtainMessage(3, uri).sendToTarget();
        }

        @Override
        public void onUnsubscribe(Uri uri) {
            ConditionProviderService.this.mHandler.obtainMessage(4, uri).sendToTarget();
        }
    }

    private final class H extends Handler {
        private static final int ON_CONNECTED = 1;
        private static final int ON_SUBSCRIBE = 3;
        private static final int ON_UNSUBSCRIBE = 4;

        private H() {
        }

        @Override
        public void handleMessage(Message message) {
            String str;
            if (!ConditionProviderService.this.isBound()) {
                return;
            }
            try {
                str = message.what;
                if (str == 1) {
                    try {
                        ConditionProviderService.this.onConnected();
                        return;
                    } catch (Throwable th) {
                        str = "onConnected";
                        th = th;
                    }
                } else {
                    try {
                        switch (str) {
                            case 3:
                                String str2 = "onSubscribe";
                                ConditionProviderService.this.onSubscribe((Uri) message.obj);
                                str = str2;
                                break;
                            case 4:
                                String str3 = "onUnsubscribe";
                                ConditionProviderService.this.onUnsubscribe((Uri) message.obj);
                                str = str3;
                                break;
                            default:
                                return;
                        }
                        return;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
            } catch (Throwable th3) {
                th = th3;
                str = 0;
            }
            Log.w(ConditionProviderService.this.TAG, "Error running " + str, th);
        }
    }
}
