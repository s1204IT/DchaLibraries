package android.telephony.ims.feature;

import android.annotation.SystemApi;
import android.content.Context;
import android.content.Intent;
import android.os.IInterface;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.telephony.ims.aidl.IImsCapabilityCallback;
import android.util.Log;
import com.android.ims.internal.IImsFeatureStatusCallback;
import com.android.internal.annotations.VisibleForTesting;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;

@SystemApi
public abstract class ImsFeature {
    public static final String ACTION_IMS_SERVICE_DOWN = "com.android.ims.IMS_SERVICE_DOWN";
    public static final String ACTION_IMS_SERVICE_UP = "com.android.ims.IMS_SERVICE_UP";
    public static final int CAPABILITY_ERROR_GENERIC = -1;
    public static final int CAPABILITY_SUCCESS = 0;
    public static final String EXTRA_PHONE_ID = "android:phone_id";
    public static final int FEATURE_EMERGENCY_MMTEL = 0;
    public static final int FEATURE_INVALID = -1;
    public static final int FEATURE_MAX = 3;
    public static final int FEATURE_MMTEL = 1;
    public static final int FEATURE_RCS = 2;
    private static final String LOG_TAG = "ImsFeature";
    public static final int STATE_INITIALIZING = 1;
    public static final int STATE_READY = 2;
    public static final int STATE_UNAVAILABLE = 0;
    protected Context mContext;
    private final Set<IImsFeatureStatusCallback> mStatusCallbacks = Collections.newSetFromMap(new WeakHashMap());
    private int mState = 0;
    private int mSlotId = -1;
    private final Object mLock = new Object();
    private final RemoteCallbackList<IImsCapabilityCallback> mCapabilityCallbacks = new RemoteCallbackList<>();
    private Capabilities mCapabilityStatus = new Capabilities();

    @Retention(RetentionPolicy.SOURCE)
    public @interface FeatureType {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ImsCapabilityError {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ImsState {
    }

    public abstract void changeEnabledCapabilities(CapabilityChangeRequest capabilityChangeRequest, CapabilityCallbackProxy capabilityCallbackProxy);

    protected abstract IInterface getBinder();

    public abstract void onFeatureReady();

    public abstract void onFeatureRemoved();

    public static class CapabilityCallback extends IImsCapabilityCallback.Stub {
        @Override
        public final void onCapabilitiesStatusChanged(int i) throws RemoteException {
            onCapabilitiesStatusChanged(new Capabilities(i));
        }

        @Override
        public void onQueryCapabilityConfiguration(int i, int i2, boolean z) {
        }

        @Override
        public void onChangeCapabilityConfigurationError(int i, int i2, int i3) {
        }

        public void onCapabilitiesStatusChanged(Capabilities capabilities) {
        }
    }

    protected static class CapabilityCallbackProxy {
        private final IImsCapabilityCallback mCallback;

        public CapabilityCallbackProxy(IImsCapabilityCallback iImsCapabilityCallback) {
            this.mCallback = iImsCapabilityCallback;
        }

        public void onChangeCapabilityConfigurationError(int i, int i2, int i3) {
            if (this.mCallback == null) {
                return;
            }
            try {
                this.mCallback.onChangeCapabilityConfigurationError(i, i2, i3);
            } catch (RemoteException e) {
                Log.e(ImsFeature.LOG_TAG, "onChangeCapabilityConfigurationError called on dead binder.");
            }
        }
    }

    public static class Capabilities {
        protected int mCapabilities;

        public Capabilities() {
            this.mCapabilities = 0;
        }

        protected Capabilities(int i) {
            this.mCapabilities = 0;
            this.mCapabilities = i;
        }

        public void addCapabilities(int i) {
            this.mCapabilities = i | this.mCapabilities;
        }

        public void removeCapabilities(int i) {
            this.mCapabilities = (~i) & this.mCapabilities;
        }

        public boolean isCapable(int i) {
            return (this.mCapabilities & i) == i;
        }

        public Capabilities copy() {
            return new Capabilities(this.mCapabilities);
        }

        public int getMask() {
            return this.mCapabilities;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            return (obj instanceof Capabilities) && this.mCapabilities == ((Capabilities) obj).mCapabilities;
        }

        public int hashCode() {
            return this.mCapabilities;
        }

        public String toString() {
            return "Capabilities: " + Integer.toBinaryString(this.mCapabilities);
        }
    }

    public final void initialize(Context context, int i) {
        this.mContext = context;
        this.mSlotId = i;
    }

    public int getFeatureState() {
        int i;
        synchronized (this.mLock) {
            i = this.mState;
        }
        return i;
    }

    public final void setFeatureState(int i) {
        synchronized (this.mLock) {
            if (this.mState != i) {
                this.mState = i;
                notifyFeatureState(i);
            }
        }
    }

    @VisibleForTesting
    public void addImsFeatureStatusCallback(IImsFeatureStatusCallback iImsFeatureStatusCallback) {
        try {
            iImsFeatureStatusCallback.notifyImsFeatureStatus(getFeatureState());
            synchronized (this.mLock) {
                this.mStatusCallbacks.add(iImsFeatureStatusCallback);
            }
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "Couldn't notify feature state: " + e.getMessage());
        }
    }

    @VisibleForTesting
    public void removeImsFeatureStatusCallback(IImsFeatureStatusCallback iImsFeatureStatusCallback) {
        synchronized (this.mLock) {
            this.mStatusCallbacks.remove(iImsFeatureStatusCallback);
        }
    }

    private void notifyFeatureState(int i) {
        synchronized (this.mLock) {
            Iterator<IImsFeatureStatusCallback> it = this.mStatusCallbacks.iterator();
            while (it.hasNext()) {
                IImsFeatureStatusCallback next = it.next();
                try {
                    Log.i(LOG_TAG, "notifying ImsFeatureState=" + i);
                    next.notifyImsFeatureStatus(i);
                } catch (RemoteException e) {
                    it.remove();
                    Log.w(LOG_TAG, "Couldn't notify feature state: " + e.getMessage());
                }
            }
        }
        sendImsServiceIntent(i);
    }

    private void sendImsServiceIntent(int i) {
        Intent intent;
        if (this.mContext == null || this.mSlotId == -1) {
            return;
        }
        switch (i) {
            case 0:
            case 1:
                intent = new Intent("com.android.ims.IMS_SERVICE_DOWN");
                break;
            case 2:
                intent = new Intent("com.android.ims.IMS_SERVICE_UP");
                break;
            default:
                intent = new Intent("com.android.ims.IMS_SERVICE_DOWN");
                break;
        }
        intent.putExtra("android:phone_id", this.mSlotId);
        this.mContext.sendBroadcast(intent);
    }

    public final void addCapabilityCallback(IImsCapabilityCallback iImsCapabilityCallback) {
        this.mCapabilityCallbacks.register(iImsCapabilityCallback);
    }

    public final void removeCapabilityCallback(IImsCapabilityCallback iImsCapabilityCallback) {
        this.mCapabilityCallbacks.unregister(iImsCapabilityCallback);
    }

    @VisibleForTesting
    public Capabilities queryCapabilityStatus() {
        Capabilities capabilitiesCopy;
        synchronized (this.mLock) {
            capabilitiesCopy = this.mCapabilityStatus.copy();
        }
        return capabilitiesCopy;
    }

    @VisibleForTesting
    public final void requestChangeEnabledCapabilities(CapabilityChangeRequest capabilityChangeRequest, IImsCapabilityCallback iImsCapabilityCallback) {
        if (capabilityChangeRequest == null) {
            throw new IllegalArgumentException("ImsFeature#requestChangeEnabledCapabilities called with invalid params.");
        }
        changeEnabledCapabilities(capabilityChangeRequest, new CapabilityCallbackProxy(iImsCapabilityCallback));
    }

    protected final void notifyCapabilitiesStatusChanged(Capabilities capabilities) {
        synchronized (this.mLock) {
            this.mCapabilityStatus = capabilities.copy();
        }
        int iBeginBroadcast = this.mCapabilityCallbacks.beginBroadcast();
        for (int i = 0; i < iBeginBroadcast; i++) {
            try {
                try {
                    ((IImsCapabilityCallback) this.mCapabilityCallbacks.getBroadcastItem(i)).onCapabilitiesStatusChanged(capabilities.mCapabilities);
                } catch (RemoteException e) {
                    Log.w(LOG_TAG, e + " notifyCapabilitiesStatusChanged() - Skipping callback.");
                }
            } finally {
                this.mCapabilityCallbacks.finishBroadcast();
            }
        }
    }
}
