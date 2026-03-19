package com.android.location.provider;

import android.hardware.location.IActivityRecognitionHardware;
import android.hardware.location.IActivityRecognitionHardwareClient;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public abstract class ActivityRecognitionProviderClient {
    private static final String TAG = "ArProviderClient";
    private IActivityRecognitionHardwareClient.Stub mClient = new IActivityRecognitionHardwareClient.Stub() {
        public void onAvailabilityChanged(boolean isSupported, IActivityRecognitionHardware instance) {
            ActivityRecognitionProvider activityRecognitionProvider;
            int callingUid = Binder.getCallingUid();
            if (callingUid != 1000) {
                Log.d(ActivityRecognitionProviderClient.TAG, "Ignoring calls from non-system server. Uid: " + callingUid);
                return;
            }
            if (!isSupported) {
                activityRecognitionProvider = null;
            } else {
                try {
                    activityRecognitionProvider = new ActivityRecognitionProvider(instance);
                } catch (RemoteException e) {
                    Log.e(ActivityRecognitionProviderClient.TAG, "Error creating Hardware Activity-Recognition Provider.", e);
                    return;
                }
            }
            ActivityRecognitionProviderClient.this.onProviderChanged(isSupported, activityRecognitionProvider);
        }
    };

    public abstract void onProviderChanged(boolean z, ActivityRecognitionProvider activityRecognitionProvider);

    protected ActivityRecognitionProviderClient() {
    }

    public IBinder getBinder() {
        return this.mClient;
    }
}
