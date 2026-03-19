package com.android.internal.telephony.ims;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.ims.aidl.IImsServiceController;
import android.telephony.ims.stub.ImsFeatureConfiguration;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ImsServiceFeatureQueryManager {
    private final Context mContext;
    private final Listener mListener;
    private final Map<ComponentName, ImsServiceFeatureQuery> mActiveQueries = new HashMap();
    private final Object mLock = new Object();

    public interface Listener {
        void onComplete(ComponentName componentName, Set<ImsFeatureConfiguration.FeatureSlotPair> set);

        void onError(ComponentName componentName);
    }

    private final class ImsServiceFeatureQuery implements ServiceConnection {
        private static final String LOG_TAG = "ImsServiceFeatureQuery";
        private final String mIntentFilter;
        private final ComponentName mName;

        ImsServiceFeatureQuery(ComponentName componentName, String str) {
            this.mName = componentName;
            this.mIntentFilter = str;
        }

        public boolean start() {
            Log.d(LOG_TAG, "start: intent filter=" + this.mIntentFilter + ", name=" + this.mName);
            boolean zBindService = ImsServiceFeatureQueryManager.this.mContext.bindService(new Intent(this.mIntentFilter).setComponent(this.mName), this, 67108929);
            if (!zBindService) {
                cleanup();
            }
            return zBindService;
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.i(LOG_TAG, "onServiceConnected for component: " + componentName);
            if (iBinder != null) {
                queryImsFeatures(IImsServiceController.Stub.asInterface(iBinder));
                return;
            }
            Log.w(LOG_TAG, "onServiceConnected: " + componentName + " binder null, cleaning up.");
            cleanup();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.w(LOG_TAG, "onServiceDisconnected for component: " + componentName);
        }

        private void queryImsFeatures(IImsServiceController iImsServiceController) {
            try {
                Set<ImsFeatureConfiguration.FeatureSlotPair> serviceFeatures = iImsServiceController.querySupportedImsFeatures().getServiceFeatures();
                cleanup();
                ImsServiceFeatureQueryManager.this.mListener.onComplete(this.mName, serviceFeatures);
            } catch (RemoteException e) {
                Log.w(LOG_TAG, "queryImsFeatures - error: " + e);
                cleanup();
                ImsServiceFeatureQueryManager.this.mListener.onError(this.mName);
            }
        }

        private void cleanup() {
            ImsServiceFeatureQueryManager.this.mContext.unbindService(this);
            synchronized (ImsServiceFeatureQueryManager.this.mLock) {
                ImsServiceFeatureQueryManager.this.mActiveQueries.remove(this.mName);
            }
        }
    }

    public ImsServiceFeatureQueryManager(Context context, Listener listener) {
        this.mContext = context;
        this.mListener = listener;
    }

    public boolean startQuery(ComponentName componentName, String str) {
        synchronized (this.mLock) {
            if (this.mActiveQueries.containsKey(componentName)) {
                return true;
            }
            ImsServiceFeatureQuery imsServiceFeatureQuery = new ImsServiceFeatureQuery(componentName, str);
            this.mActiveQueries.put(componentName, imsServiceFeatureQuery);
            return imsServiceFeatureQuery.start();
        }
    }

    public boolean isQueryInProgress() {
        boolean z;
        synchronized (this.mLock) {
            z = !this.mActiveQueries.isEmpty();
        }
        return z;
    }
}
