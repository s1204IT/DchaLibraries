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

    private ActivityRecognitionProxy(Context context, Handler handler, boolean z, ActivityRecognitionHardware activityRecognitionHardware, int i, int i2, int i3) {
        this.mIsSupported = z;
        this.mInstance = activityRecognitionHardware;
        this.mServiceWatcher = new ServiceWatcher(context, TAG, "com.android.location.service.ActivityRecognitionProvider", i, i2, i3, new Runnable() {
            @Override
            public void run() {
                ActivityRecognitionProxy.this.bindProvider();
            }
        }, handler);
    }

    public static ActivityRecognitionProxy createAndBind(Context context, Handler handler, boolean z, ActivityRecognitionHardware activityRecognitionHardware, int i, int i2, int i3) {
        ActivityRecognitionProxy activityRecognitionProxy = new ActivityRecognitionProxy(context, handler, z, activityRecognitionHardware, i, i2, i3);
        if (!activityRecognitionProxy.mServiceWatcher.start()) {
            Log.e(TAG, "ServiceWatcher could not start.");
            return null;
        }
        return activityRecognitionProxy;
    }

    private void bindProvider() {
        if (!this.mServiceWatcher.runOnBinder(new ServiceWatcher.BinderRunner() {
            @Override
            public void run(IBinder iBinder) {
                try {
                    String interfaceDescriptor = iBinder.getInterfaceDescriptor();
                    if (IActivityRecognitionHardwareWatcher.class.getCanonicalName().equals(interfaceDescriptor)) {
                        IActivityRecognitionHardwareWatcher iActivityRecognitionHardwareWatcherAsInterface = IActivityRecognitionHardwareWatcher.Stub.asInterface(iBinder);
                        if (iActivityRecognitionHardwareWatcherAsInterface != null) {
                            if (ActivityRecognitionProxy.this.mInstance != null) {
                                try {
                                    iActivityRecognitionHardwareWatcherAsInterface.onInstanceChanged(ActivityRecognitionProxy.this.mInstance);
                                    return;
                                } catch (RemoteException e) {
                                    Log.e(ActivityRecognitionProxy.TAG, "Error delivering hardware interface to watcher.", e);
                                    return;
                                }
                            }
                            Log.d(ActivityRecognitionProxy.TAG, "AR HW instance not available, binding will be a no-op.");
                            return;
                        }
                        Log.e(ActivityRecognitionProxy.TAG, "No watcher found on connection.");
                        return;
                    }
                    if (IActivityRecognitionHardwareClient.class.getCanonicalName().equals(interfaceDescriptor)) {
                        IActivityRecognitionHardwareClient iActivityRecognitionHardwareClientAsInterface = IActivityRecognitionHardwareClient.Stub.asInterface(iBinder);
                        if (iActivityRecognitionHardwareClientAsInterface != null) {
                            try {
                                iActivityRecognitionHardwareClientAsInterface.onAvailabilityChanged(ActivityRecognitionProxy.this.mIsSupported, ActivityRecognitionProxy.this.mInstance);
                                return;
                            } catch (RemoteException e2) {
                                Log.e(ActivityRecognitionProxy.TAG, "Error delivering hardware interface to client.", e2);
                                return;
                            }
                        }
                        Log.e(ActivityRecognitionProxy.TAG, "No client found on connection.");
                        return;
                    }
                    Log.e(ActivityRecognitionProxy.TAG, "Invalid descriptor found on connection: " + interfaceDescriptor);
                } catch (RemoteException e3) {
                    Log.e(ActivityRecognitionProxy.TAG, "Unable to get interface descriptor.", e3);
                }
            }
        })) {
            Log.e(TAG, "Null binder found on connection.");
        }
    }
}
