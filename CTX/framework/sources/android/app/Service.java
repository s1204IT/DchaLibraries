package android.app;

import android.content.ComponentCallbacks2;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public abstract class Service extends ContextWrapper implements ComponentCallbacks2 {
    public static final int START_CONTINUATION_MASK = 15;
    public static final int START_FLAG_REDELIVERY = 1;
    public static final int START_FLAG_RETRY = 2;
    public static final int START_NOT_STICKY = 2;
    public static final int START_REDELIVER_INTENT = 3;
    public static final int START_STICKY = 1;
    public static final int START_STICKY_COMPATIBILITY = 0;
    public static final int START_TASK_REMOVED_COMPLETE = 1000;
    public static final int STOP_FOREGROUND_DETACH = 2;
    public static final int STOP_FOREGROUND_REMOVE = 1;
    private static final String TAG = "Service";
    private IActivityManager mActivityManager;
    private Application mApplication;
    private String mClassName;
    private boolean mStartCompatibility;
    private ActivityThread mThread;
    private IBinder mToken;

    @Retention(RetentionPolicy.SOURCE)
    public @interface StartArgFlags {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface StartResult {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface StopForegroundFlags {
    }

    public abstract IBinder onBind(Intent intent);

    public Service() {
        super(null);
        this.mThread = null;
        this.mClassName = null;
        this.mToken = null;
        this.mApplication = null;
        this.mActivityManager = null;
        this.mStartCompatibility = false;
    }

    public final Application getApplication() {
        return this.mApplication;
    }

    public void onCreate() {
    }

    @Deprecated
    public void onStart(Intent intent, int i) {
    }

    public int onStartCommand(Intent intent, int i, int i2) {
        onStart(intent, i2);
        return !this.mStartCompatibility ? 1 : 0;
    }

    public void onDestroy() {
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
    }

    @Override
    public void onLowMemory() {
    }

    @Override
    public void onTrimMemory(int i) {
    }

    public boolean onUnbind(Intent intent) {
        return false;
    }

    public void onRebind(Intent intent) {
    }

    public void onTaskRemoved(Intent intent) {
    }

    public final void stopSelf() {
        stopSelf(-1);
    }

    public final void stopSelf(int i) {
        if (this.mActivityManager == null) {
            return;
        }
        try {
            this.mActivityManager.stopServiceToken(new ComponentName(this, this.mClassName), this.mToken, i);
        } catch (RemoteException e) {
        }
    }

    public final boolean stopSelfResult(int i) {
        if (this.mActivityManager == null) {
            return false;
        }
        try {
            return this.mActivityManager.stopServiceToken(new ComponentName(this, this.mClassName), this.mToken, i);
        } catch (RemoteException e) {
            return false;
        }
    }

    @Deprecated
    public final void setForeground(boolean z) {
        Log.w(TAG, "setForeground: ignoring old API call on " + getClass().getName());
    }

    public final void startForeground(int i, Notification notification) {
        try {
            this.mActivityManager.setServiceForeground(new ComponentName(this, this.mClassName), this.mToken, i, notification, 0);
        } catch (RemoteException e) {
        }
    }

    public final void stopForeground(boolean z) {
        stopForeground(z ? 1 : 0);
    }

    public final void stopForeground(int i) {
        try {
            this.mActivityManager.setServiceForeground(new ComponentName(this, this.mClassName), this.mToken, 0, null, i);
        } catch (RemoteException e) {
        }
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("nothing to dump");
    }

    public final void attach(Context context, ActivityThread activityThread, String str, IBinder iBinder, Application application, Object obj) {
        attachBaseContext(context);
        this.mThread = activityThread;
        this.mClassName = str;
        this.mToken = iBinder;
        this.mApplication = application;
        this.mActivityManager = (IActivityManager) obj;
        this.mStartCompatibility = getApplicationInfo().targetSdkVersion < 5;
    }

    public final void detachAndCleanUp() {
        this.mToken = null;
    }

    final String getClassName() {
        return this.mClassName;
    }
}
