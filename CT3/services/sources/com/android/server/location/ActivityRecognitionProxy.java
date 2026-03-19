package com.android.server.location;

import android.content.Context;
import android.hardware.location.ActivityRecognitionHardware;
import android.hardware.location.IActivityRecognitionHardwareClient;
import android.hardware.location.IActivityRecognitionHardwareWatcher;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.android.server.ServiceWatcher;

public class ActivityRecognitionProxy {
    private static final String TAG = "ActivityRecognitionProxy";
    private final ActivityRecognitionHardware mInstance;
    private final boolean mIsSupported;
    private final ServiceWatcher mServiceWatcher;

    private ActivityRecognitionProxy(Context context, Handler handler, boolean activityRecognitionHardwareIsSupported, ActivityRecognitionHardware activityRecognitionHardware, int overlaySwitchResId, int defaultServicePackageNameResId, int initialPackageNameResId) {
        this.mIsSupported = activityRecognitionHardwareIsSupported;
        this.mInstance = activityRecognitionHardware;
        Runnable newServiceWork = new Runnable() {
            @Override
            public void run() {
                ActivityRecognitionProxy.this.bindProvider();
            }
        };
        this.mServiceWatcher = new ServiceWatcher(context, TAG, "com.android.location.service.ActivityRecognitionProvider", overlaySwitchResId, defaultServicePackageNameResId, initialPackageNameResId, newServiceWork, handler);
    }

    public static ActivityRecognitionProxy createAndBind(Context context, Handler handler, boolean activityRecognitionHardwareIsSupported, ActivityRecognitionHardware activityRecognitionHardware, int overlaySwitchResId, int defaultServicePackageNameResId, int initialPackageNameResId) {
        ActivityRecognitionProxy activityRecognitionProxy = new ActivityRecognitionProxy(context, handler, activityRecognitionHardwareIsSupported, activityRecognitionHardware, overlaySwitchResId, defaultServicePackageNameResId, initialPackageNameResId);
        if (!activityRecognitionProxy.mServiceWatcher.start()) {
            Log.e(TAG, "ServiceWatcher could not start.");
            return null;
        }
        return activityRecognitionProxy;
    }

    private void bindProvider() {
        IBinder binder = this.mServiceWatcher.getBinder();
        if (binder == null) {
            Log.e(TAG, "Null binder found on connection.");
            return;
        }
        try {
            String descriptor = binder.getInterfaceDescriptor();
            if (IActivityRecognitionHardwareWatcher.class.getCanonicalName().equals(descriptor)) {
                IActivityRecognitionHardwareWatcher watcher = IActivityRecognitionHardwareWatcher.Stub.asInterface(binder);
                if (watcher == null) {
                    Log.e(TAG, "No watcher found on connection.");
                    return;
                }
                if (this.mInstance == null) {
                    Log.d(TAG, "AR HW instance not available, binding will be a no-op.");
                    return;
                }
                try {
                    watcher.onInstanceChanged(this.mInstance);
                    return;
                } catch (RemoteException e) {
                    Log.e(TAG, "Error delivering hardware interface to watcher.", e);
                    return;
                }
            }
            if (IActivityRecognitionHardwareClient.class.getCanonicalName().equals(descriptor)) {
                IActivityRecognitionHardwareClient client = IActivityRecognitionHardwareClient.Stub.asInterface(binder);
                if (client == null) {
                    Log.e(TAG, "No client found on connection.");
                    return;
                }
                try {
                    client.onAvailabilityChanged(this.mIsSupported, this.mInstance);
                    return;
                } catch (RemoteException e2) {
                    Log.e(TAG, "Error delivering hardware interface to client.", e2);
                    return;
                }
            }
            Log.e(TAG, "Invalid descriptor found on connection: " + descriptor);
        } catch (RemoteException e3) {
            Log.e(TAG, "Unable to get interface descriptor.", e3);
        }
    }
}
