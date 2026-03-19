package com.android.ims;

import android.content.Context;
import android.net.Uri;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.aidl.IImsConfig;
import android.telephony.ims.aidl.IImsMmTelFeature;
import android.telephony.ims.aidl.IImsMmTelListener;
import android.telephony.ims.aidl.IImsRegistration;
import android.telephony.ims.aidl.IImsRegistrationCallback;
import android.telephony.ims.aidl.IImsSmsListener;
import android.telephony.ims.feature.CapabilityChangeRequest;
import android.telephony.ims.feature.ImsFeature;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.util.Log;
import com.android.ims.internal.IImsCallSession;
import com.android.ims.internal.IImsEcbm;
import com.android.ims.internal.IImsMultiEndpoint;
import com.android.ims.internal.IImsServiceFeatureCallback;
import com.android.ims.internal.IImsUt;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class MmTelFeatureConnection {
    protected static final String TAG = "MmTelFeatureConnection";
    protected IBinder mBinder;
    private final CapabilityCallbackManager mCapabilityCallbackManager;
    private IImsConfig mConfigBinder;
    private Context mContext;
    private IImsRegistration mRegistrationBinder;
    private ImsRegistrationCallbackAdapter mRegistrationCallbackManager;
    protected final int mSlotId;
    private IFeatureUpdate mStatusCallback;
    private volatile boolean mIsAvailable = false;
    private Integer mFeatureStateCached = null;
    private final Object mLock = new Object();
    private boolean mSupportsEmergencyCalling = false;
    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public final void binderDied() {
            MmTelFeatureConnection.lambda$new$0(this.f$0);
        }
    };
    private final IImsServiceFeatureCallback mListenerBinder = new IImsServiceFeatureCallback.Stub() {
        public void imsFeatureCreated(int i, int i2) throws RemoteException {
            synchronized (MmTelFeatureConnection.this.mLock) {
                if (MmTelFeatureConnection.this.mSlotId != i) {
                    return;
                }
                switch (i2) {
                    case 0:
                        MmTelFeatureConnection.this.mSupportsEmergencyCalling = true;
                        Log.i(MmTelFeatureConnection.TAG, "Emergency calling enabled on slotId: " + i);
                        break;
                    case 1:
                        if (!MmTelFeatureConnection.this.mIsAvailable) {
                            Log.i(MmTelFeatureConnection.TAG, "MmTel enabled on slotId: " + i);
                            MmTelFeatureConnection.this.mIsAvailable = true;
                        }
                        break;
                }
            }
        }

        public void imsFeatureRemoved(int i, int i2) throws RemoteException {
            synchronized (MmTelFeatureConnection.this.mLock) {
                if (MmTelFeatureConnection.this.mSlotId != i) {
                    return;
                }
                switch (i2) {
                    case 0:
                        MmTelFeatureConnection.this.mSupportsEmergencyCalling = false;
                        Log.i(MmTelFeatureConnection.TAG, "Emergency calling disabled on slotId: " + i);
                        break;
                    case 1:
                        Log.i(MmTelFeatureConnection.TAG, "MmTel removed on slotId: " + i);
                        MmTelFeatureConnection.this.onRemovedOrDied();
                        break;
                }
            }
        }

        public void imsStatusChanged(int i, int i2, int i3) throws RemoteException {
            synchronized (MmTelFeatureConnection.this.mLock) {
                Log.i(MmTelFeatureConnection.TAG, "imsStatusChanged: slot: " + i + " feature: " + i2 + " status: " + i3);
                if (MmTelFeatureConnection.this.mSlotId == i && i2 == 1) {
                    MmTelFeatureConnection.this.mFeatureStateCached = Integer.valueOf(i3);
                    if (MmTelFeatureConnection.this.mStatusCallback != null) {
                        MmTelFeatureConnection.this.mStatusCallback.notifyStateChanged();
                    }
                }
            }
        }
    };

    public interface IFeatureUpdate {
        void notifyStateChanged();

        void notifyUnavailable();
    }

    public static void lambda$new$0(MmTelFeatureConnection mmTelFeatureConnection) {
        Log.w(TAG, "DeathRecipient triggered, binder died.");
        mmTelFeatureConnection.onRemovedOrDied();
    }

    private abstract class CallbackAdapterManager<T> {
        private static final String TAG = "CallbackAdapterManager";
        private boolean mHasConnected;
        protected final Set<T> mLocalCallbacks;

        abstract boolean createConnection() throws RemoteException;

        abstract void removeConnection();

        private CallbackAdapterManager() {
            this.mLocalCallbacks = Collections.newSetFromMap(new ConcurrentHashMap());
            this.mHasConnected = false;
        }

        public void addCallback(T t) throws RemoteException {
            synchronized (MmTelFeatureConnection.this.mLock) {
                if (!this.mHasConnected) {
                    if (createConnection()) {
                        this.mHasConnected = true;
                    } else {
                        throw new RemoteException("Can not create connection!");
                    }
                }
            }
            Log.i(TAG, "Local callback added: " + t);
            this.mLocalCallbacks.add(t);
        }

        public void removeCallback(T t) {
            Log.i(TAG, "Local callback removed: " + t);
            this.mLocalCallbacks.remove(t);
            synchronized (MmTelFeatureConnection.this.mLock) {
                if (this.mHasConnected && this.mLocalCallbacks.isEmpty()) {
                    removeConnection();
                    this.mHasConnected = false;
                }
            }
        }

        public void close() {
            synchronized (MmTelFeatureConnection.this.mLock) {
                if (this.mHasConnected) {
                    removeConnection();
                    this.mHasConnected = false;
                }
            }
            Log.i(TAG, "Closing connection and clearing callbacks");
            this.mLocalCallbacks.clear();
        }
    }

    private class ImsRegistrationCallbackAdapter extends CallbackAdapterManager<ImsRegistrationImplBase.Callback> {
        private final RegistrationCallbackAdapter mRegistrationCallbackAdapter;

        private ImsRegistrationCallbackAdapter() {
            super();
            this.mRegistrationCallbackAdapter = new RegistrationCallbackAdapter();
        }

        private class RegistrationCallbackAdapter extends IImsRegistrationCallback.Stub {
            private RegistrationCallbackAdapter() {
            }

            public void onRegistered(final int i) {
                Log.i(MmTelFeatureConnection.TAG, "onRegistered ::");
                ImsRegistrationCallbackAdapter.this.mLocalCallbacks.forEach(new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        ((ImsRegistrationImplBase.Callback) obj).onRegistered(i);
                    }
                });
            }

            public void onRegistering(final int i) {
                Log.i(MmTelFeatureConnection.TAG, "onRegistering ::");
                ImsRegistrationCallbackAdapter.this.mLocalCallbacks.forEach(new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        ((ImsRegistrationImplBase.Callback) obj).onRegistering(i);
                    }
                });
            }

            public void onDeregistered(final ImsReasonInfo imsReasonInfo) {
                Log.i(MmTelFeatureConnection.TAG, "onDeregistered ::");
                ImsRegistrationCallbackAdapter.this.mLocalCallbacks.forEach(new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        ((ImsRegistrationImplBase.Callback) obj).onDeregistered(imsReasonInfo);
                    }
                });
            }

            public void onTechnologyChangeFailed(final int i, final ImsReasonInfo imsReasonInfo) {
                Log.i(MmTelFeatureConnection.TAG, "onTechnologyChangeFailed :: targetAccessTech=" + i + ", imsReasonInfo=" + imsReasonInfo);
                ImsRegistrationCallbackAdapter.this.mLocalCallbacks.forEach(new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        ((ImsRegistrationImplBase.Callback) obj).onTechnologyChangeFailed(i, imsReasonInfo);
                    }
                });
            }

            public void onSubscriberAssociatedUriChanged(final Uri[] uriArr) {
                Log.i(MmTelFeatureConnection.TAG, "onSubscriberAssociatedUriChanged");
                ImsRegistrationCallbackAdapter.this.mLocalCallbacks.forEach(new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        ((ImsRegistrationImplBase.Callback) obj).onSubscriberAssociatedUriChanged(uriArr);
                    }
                });
            }
        }

        @Override
        boolean createConnection() throws RemoteException {
            if (MmTelFeatureConnection.this.getRegistration() != null) {
                MmTelFeatureConnection.this.getRegistration().addRegistrationCallback(this.mRegistrationCallbackAdapter);
                return true;
            }
            Log.e(MmTelFeatureConnection.TAG, "ImsRegistration is null");
            return false;
        }

        @Override
        void removeConnection() {
            if (MmTelFeatureConnection.this.getRegistration() != null) {
                try {
                    MmTelFeatureConnection.this.getRegistration().removeRegistrationCallback(this.mRegistrationCallbackAdapter);
                    return;
                } catch (RemoteException e) {
                    Log.w(MmTelFeatureConnection.TAG, "removeConnection: couldn't remove registration callback");
                    return;
                }
            }
            Log.e(MmTelFeatureConnection.TAG, "ImsRegistration is null");
        }
    }

    private class CapabilityCallbackManager extends CallbackAdapterManager<ImsFeature.CapabilityCallback> {
        private final CapabilityCallbackAdapter mCallbackAdapter;

        private CapabilityCallbackManager() {
            super();
            this.mCallbackAdapter = new CapabilityCallbackAdapter();
        }

        private class CapabilityCallbackAdapter extends ImsFeature.CapabilityCallback {
            private CapabilityCallbackAdapter() {
            }

            public void onCapabilitiesStatusChanged(final ImsFeature.Capabilities capabilities) {
                CapabilityCallbackManager.this.mLocalCallbacks.forEach(new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        ((ImsFeature.CapabilityCallback) obj).onCapabilitiesStatusChanged(capabilities);
                    }
                });
            }
        }

        @Override
        boolean createConnection() throws RemoteException {
            IImsMmTelFeature serviceInterface;
            synchronized (MmTelFeatureConnection.this.mLock) {
                MmTelFeatureConnection.this.checkServiceIsReady();
                serviceInterface = MmTelFeatureConnection.this.getServiceInterface(MmTelFeatureConnection.this.mBinder);
            }
            if (serviceInterface != null) {
                serviceInterface.addCapabilityCallback(this.mCallbackAdapter);
                return true;
            }
            Log.w(MmTelFeatureConnection.TAG, "create: Couldn't get IImsMmTelFeature binder");
            return false;
        }

        @Override
        void removeConnection() {
            IImsMmTelFeature serviceInterface;
            synchronized (MmTelFeatureConnection.this.mLock) {
                try {
                    MmTelFeatureConnection.this.checkServiceIsReady();
                    serviceInterface = MmTelFeatureConnection.this.getServiceInterface(MmTelFeatureConnection.this.mBinder);
                } catch (RemoteException e) {
                    serviceInterface = null;
                }
            }
            if (serviceInterface != null) {
                try {
                    serviceInterface.removeCapabilityCallback(this.mCallbackAdapter);
                    return;
                } catch (RemoteException e2) {
                    Log.w(MmTelFeatureConnection.TAG, "remove: IImsMmTelFeature binder is dead");
                    return;
                }
            }
            Log.w(MmTelFeatureConnection.TAG, "remove: Couldn't get IImsMmTelFeature binder");
        }
    }

    public static MmTelFeatureConnection create(Context context, int i) {
        MmTelFeatureConnection mmTelFeatureConnection = new MmTelFeatureConnection(context, i);
        TelephonyManager telephonyManager = getTelephonyManager(context);
        if (telephonyManager == null) {
            Rlog.w(TAG, "create: TelephonyManager is null!");
            return mmTelFeatureConnection;
        }
        IImsMmTelFeature imsMmTelFeatureAndListen = telephonyManager.getImsMmTelFeatureAndListen(i, mmTelFeatureConnection.getListener());
        if (imsMmTelFeatureAndListen != null) {
            mmTelFeatureConnection.setBinder(imsMmTelFeatureAndListen.asBinder());
            mmTelFeatureConnection.getFeatureState();
        } else {
            Rlog.w(TAG, "create: binder is null! Slot Id: " + i);
        }
        return mmTelFeatureConnection;
    }

    public static TelephonyManager getTelephonyManager(Context context) {
        return (TelephonyManager) context.getSystemService("phone");
    }

    public MmTelFeatureConnection(Context context, int i) {
        this.mRegistrationCallbackManager = new ImsRegistrationCallbackAdapter();
        this.mCapabilityCallbackManager = new CapabilityCallbackManager();
        this.mSlotId = i;
        this.mContext = context;
    }

    private void onRemovedOrDied() {
        synchronized (this.mLock) {
            if (this.mIsAvailable) {
                this.mIsAvailable = false;
                this.mRegistrationBinder = null;
                this.mConfigBinder = null;
                if (this.mBinder != null) {
                    this.mBinder.unlinkToDeath(this.mDeathRecipient, 0);
                }
                if (this.mStatusCallback != null) {
                    this.mStatusCallback.notifyUnavailable();
                }
            }
        }
    }

    private IImsRegistration getRegistration() {
        synchronized (this.mLock) {
            if (this.mRegistrationBinder != null) {
                return this.mRegistrationBinder;
            }
            TelephonyManager telephonyManager = getTelephonyManager(this.mContext);
            IImsRegistration imsRegistration = telephonyManager != null ? telephonyManager.getImsRegistration(this.mSlotId, 1) : null;
            synchronized (this.mLock) {
                if (this.mRegistrationBinder == null) {
                    this.mRegistrationBinder = imsRegistration;
                }
            }
            return this.mRegistrationBinder;
        }
    }

    private IImsConfig getConfig() {
        synchronized (this.mLock) {
            if (this.mConfigBinder != null) {
                return this.mConfigBinder;
            }
            TelephonyManager telephonyManager = getTelephonyManager(this.mContext);
            IImsConfig imsConfig = telephonyManager != null ? telephonyManager.getImsConfig(this.mSlotId, 1) : null;
            synchronized (this.mLock) {
                if (this.mConfigBinder == null) {
                    this.mConfigBinder = imsConfig;
                }
            }
            return this.mConfigBinder;
        }
    }

    public boolean isEmergencyMmTelAvailable() {
        return this.mSupportsEmergencyCalling;
    }

    public IImsServiceFeatureCallback getListener() {
        return this.mListenerBinder;
    }

    public void setBinder(IBinder iBinder) {
        synchronized (this.mLock) {
            this.mBinder = iBinder;
            try {
                if (this.mBinder != null) {
                    this.mBinder.linkToDeath(this.mDeathRecipient, 0);
                }
            } catch (RemoteException e) {
            }
        }
    }

    public void openConnection(MmTelFeature.Listener listener) throws RemoteException {
        synchronized (this.mLock) {
            checkServiceIsReady();
            getServiceInterface(this.mBinder).setListener(listener);
        }
    }

    public void closeConnection() {
        this.mRegistrationCallbackManager.close();
        this.mCapabilityCallbackManager.close();
        try {
            synchronized (this.mLock) {
                if (isBinderAlive()) {
                    getServiceInterface(this.mBinder).setListener((IImsMmTelListener) null);
                }
            }
        } catch (RemoteException e) {
            Log.w(TAG, "closeConnection: couldn't remove listener!");
        }
    }

    public void addRegistrationCallback(ImsRegistrationImplBase.Callback callback) throws RemoteException {
        this.mRegistrationCallbackManager.addCallback(callback);
    }

    public void removeRegistrationCallback(ImsRegistrationImplBase.Callback callback) throws RemoteException {
        this.mRegistrationCallbackManager.removeCallback(callback);
    }

    public void addCapabilityCallback(ImsFeature.CapabilityCallback capabilityCallback) throws RemoteException {
        this.mCapabilityCallbackManager.addCallback(capabilityCallback);
    }

    public void removeCapabilityCallback(ImsFeature.CapabilityCallback capabilityCallback) throws RemoteException {
        this.mCapabilityCallbackManager.removeCallback(capabilityCallback);
    }

    public void changeEnabledCapabilities(CapabilityChangeRequest capabilityChangeRequest, ImsFeature.CapabilityCallback capabilityCallback) throws RemoteException {
        synchronized (this.mLock) {
            checkServiceIsReady();
            getServiceInterface(this.mBinder).changeCapabilitiesConfiguration(capabilityChangeRequest, capabilityCallback);
        }
    }

    public void queryEnabledCapabilities(int i, int i2, ImsFeature.CapabilityCallback capabilityCallback) throws RemoteException {
        synchronized (this.mLock) {
            checkServiceIsReady();
            getServiceInterface(this.mBinder).queryCapabilityConfiguration(i, i2, capabilityCallback);
        }
    }

    public MmTelFeature.MmTelCapabilities queryCapabilityStatus() throws RemoteException {
        MmTelFeature.MmTelCapabilities mmTelCapabilities;
        synchronized (this.mLock) {
            checkServiceIsReady();
            mmTelCapabilities = new MmTelFeature.MmTelCapabilities(getServiceInterface(this.mBinder).queryCapabilityStatus());
        }
        return mmTelCapabilities;
    }

    public ImsCallProfile createCallProfile(int i, int i2) throws RemoteException {
        ImsCallProfile imsCallProfileCreateCallProfile;
        synchronized (this.mLock) {
            checkServiceIsReady();
            imsCallProfileCreateCallProfile = getServiceInterface(this.mBinder).createCallProfile(i, i2);
        }
        return imsCallProfileCreateCallProfile;
    }

    public IImsCallSession createCallSession(ImsCallProfile imsCallProfile) throws RemoteException {
        IImsCallSession iImsCallSessionCreateCallSession;
        synchronized (this.mLock) {
            checkServiceIsReady();
            iImsCallSessionCreateCallSession = getServiceInterface(this.mBinder).createCallSession(imsCallProfile);
        }
        return iImsCallSessionCreateCallSession;
    }

    public IImsUt getUtInterface() throws RemoteException {
        IImsUt utInterface;
        synchronized (this.mLock) {
            checkServiceIsReady();
            utInterface = getServiceInterface(this.mBinder).getUtInterface();
        }
        return utInterface;
    }

    public IImsConfig getConfigInterface() throws RemoteException {
        return getConfig();
    }

    public int getRegistrationTech() throws RemoteException {
        IImsRegistration registration = getRegistration();
        if (registration != null) {
            return registration.getRegistrationTechnology();
        }
        return -1;
    }

    public IImsEcbm getEcbmInterface() throws RemoteException {
        IImsEcbm ecbmInterface;
        synchronized (this.mLock) {
            checkServiceIsReady();
            ecbmInterface = getServiceInterface(this.mBinder).getEcbmInterface();
        }
        return ecbmInterface;
    }

    public void setUiTTYMode(int i, Message message) throws RemoteException {
        synchronized (this.mLock) {
            checkServiceIsReady();
            getServiceInterface(this.mBinder).setUiTtyMode(i, message);
        }
    }

    public IImsMultiEndpoint getMultiEndpointInterface() throws RemoteException {
        IImsMultiEndpoint multiEndpointInterface;
        synchronized (this.mLock) {
            checkServiceIsReady();
            multiEndpointInterface = getServiceInterface(this.mBinder).getMultiEndpointInterface();
        }
        return multiEndpointInterface;
    }

    public void sendSms(int i, int i2, String str, String str2, boolean z, byte[] bArr) throws RemoteException {
        synchronized (this.mLock) {
            checkServiceIsReady();
            getServiceInterface(this.mBinder).sendSms(i, i2, str, str2, z, bArr);
        }
    }

    public void acknowledgeSms(int i, int i2, int i3) throws RemoteException {
        synchronized (this.mLock) {
            checkServiceIsReady();
            getServiceInterface(this.mBinder).acknowledgeSms(i, i2, i3);
        }
    }

    public void acknowledgeSmsReport(int i, int i2, int i3) throws RemoteException {
        synchronized (this.mLock) {
            checkServiceIsReady();
            getServiceInterface(this.mBinder).acknowledgeSmsReport(i, i2, i3);
        }
    }

    public String getSmsFormat() throws RemoteException {
        String smsFormat;
        synchronized (this.mLock) {
            checkServiceIsReady();
            smsFormat = getServiceInterface(this.mBinder).getSmsFormat();
        }
        return smsFormat;
    }

    public void onSmsReady() throws RemoteException {
        synchronized (this.mLock) {
            checkServiceIsReady();
            getServiceInterface(this.mBinder).onSmsReady();
        }
    }

    public void setSmsListener(IImsSmsListener iImsSmsListener) throws RemoteException {
        synchronized (this.mLock) {
            checkServiceIsReady();
            getServiceInterface(this.mBinder).setSmsListener(iImsSmsListener);
        }
    }

    public int shouldProcessCall(boolean z, String[] strArr) throws RemoteException {
        int iShouldProcessCall;
        if (z && !isEmergencyMmTelAvailable()) {
            Log.i(TAG, "MmTel does not support emergency over IMS, fallback to CS.");
            return 1;
        }
        synchronized (this.mLock) {
            checkServiceIsReady();
            iShouldProcessCall = getServiceInterface(this.mBinder).shouldProcessCall(strArr);
        }
        return iShouldProcessCall;
    }

    public int getFeatureState() {
        synchronized (this.mLock) {
            if (isBinderAlive() && this.mFeatureStateCached != null) {
                return this.mFeatureStateCached.intValue();
            }
            Integer numRetrieveFeatureState = retrieveFeatureState();
            synchronized (this.mLock) {
                try {
                    if (numRetrieveFeatureState == null) {
                        return 0;
                    }
                    this.mFeatureStateCached = numRetrieveFeatureState;
                    Log.i(TAG, "getFeatureState - returning " + numRetrieveFeatureState);
                    return numRetrieveFeatureState.intValue();
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
    }

    private Integer retrieveFeatureState() {
        if (this.mBinder != null) {
            try {
                return Integer.valueOf(getServiceInterface(this.mBinder).getFeatureState());
            } catch (RemoteException e) {
                return null;
            }
        }
        return null;
    }

    public void setStatusCallback(IFeatureUpdate iFeatureUpdate) {
        this.mStatusCallback = iFeatureUpdate;
    }

    public boolean isBinderReady() {
        return isBinderAlive() && getFeatureState() == 2;
    }

    public boolean isBinderAlive() {
        return this.mIsAvailable && this.mBinder != null && this.mBinder.isBinderAlive();
    }

    protected void checkServiceIsReady() throws RemoteException {
        if (!isBinderReady()) {
            throw new RemoteException("ImsServiceProxy is not ready to accept commands.");
        }
    }

    private IImsMmTelFeature getServiceInterface(IBinder iBinder) {
        return IImsMmTelFeature.Stub.asInterface(iBinder);
    }

    protected void checkBinderConnection() throws RemoteException {
        if (!isBinderAlive()) {
            throw new RemoteException("ImsServiceProxy is not available for that feature.");
        }
    }
}
