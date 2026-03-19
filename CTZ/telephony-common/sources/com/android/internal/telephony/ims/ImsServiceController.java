package com.android.internal.telephony.ims;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.IPackageManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.ims.ImsService;
import android.telephony.ims.aidl.IImsConfig;
import android.telephony.ims.aidl.IImsMmTelFeature;
import android.telephony.ims.aidl.IImsRcsFeature;
import android.telephony.ims.aidl.IImsRegistration;
import android.telephony.ims.aidl.IImsServiceController;
import android.telephony.ims.stub.ImsFeatureConfiguration;
import android.util.Log;
import com.android.ims.internal.IImsFeatureStatusCallback;
import com.android.ims.internal.IImsServiceFeatureCallback;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.ExponentialBackoff;
import com.android.internal.telephony.ims.ImsServiceController;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class ImsServiceController {
    private static final String LOG_TAG = "ImsServiceController";
    private static final int REBIND_MAXIMUM_DELAY_MS = 60000;
    private static final int REBIND_START_DELAY_MS = 2000;
    private ExponentialBackoff mBackoff;
    private ImsServiceControllerCallbacks mCallbacks;
    private final ComponentName mComponentName;
    protected final Context mContext;
    private ImsService.Listener mFeatureChangedListener;
    private Set<ImsFeatureStatusCallback> mFeatureStatusCallbacks;
    private final HandlerThread mHandlerThread;
    private IImsServiceController mIImsServiceController;
    private ImsDeathRecipient mImsDeathRecipient;
    private HashSet<ImsFeatureContainer> mImsFeatureBinders;
    private HashSet<ImsFeatureConfiguration.FeatureSlotPair> mImsFeatures;
    private ImsServiceConnection mImsServiceConnection;
    private IBinder mImsServiceControllerBinder;
    private Set<IImsServiceFeatureCallback> mImsStatusCallbacks;
    private boolean mIsBinding;
    private boolean mIsBound;
    protected final Object mLock;
    private final IPackageManager mPackageManager;
    private RebindRetry mRebindRetry;
    private Runnable mRestartImsServiceRunnable;

    public interface ImsServiceControllerCallbacks {
        void imsServiceFeatureCreated(int i, int i2, ImsServiceController imsServiceController);

        void imsServiceFeatureRemoved(int i, int i2, ImsServiceController imsServiceController);

        void imsServiceFeaturesChanged(ImsFeatureConfiguration imsFeatureConfiguration, ImsServiceController imsServiceController);
    }

    @VisibleForTesting
    public interface RebindRetry {
        long getMaximumDelay();

        long getStartDelay();
    }

    class ImsDeathRecipient implements IBinder.DeathRecipient {
        private ComponentName mComponentName;

        ImsDeathRecipient(ComponentName componentName) {
            this.mComponentName = componentName;
        }

        @Override
        public void binderDied() {
            Log.e(ImsServiceController.LOG_TAG, "ImsService(" + this.mComponentName + ") died. Restarting...");
            synchronized (ImsServiceController.this.mLock) {
                ImsServiceController.this.mIsBinding = false;
                ImsServiceController.this.mIsBound = false;
            }
            ImsServiceController.this.cleanupAllFeatures();
            ImsServiceController.this.cleanUpService();
            ImsServiceController.this.startDelayedRebindToService();
        }
    }

    public class ImsServiceConnection implements ServiceConnection {
        public ImsServiceConnection() {
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            ImsServiceController.this.mBackoff.stop();
            synchronized (ImsServiceController.this.mLock) {
                ImsServiceController.this.mIsBound = true;
                ImsServiceController.this.mIsBinding = false;
                Log.d(ImsServiceController.LOG_TAG, "ImsService(" + componentName + "): onServiceConnected with binder: " + iBinder);
                if (iBinder != null) {
                    ImsServiceController.this.mImsDeathRecipient = ImsServiceController.this.new ImsDeathRecipient(componentName);
                    try {
                        iBinder.linkToDeath(ImsServiceController.this.mImsDeathRecipient, 0);
                        ImsServiceController.this.mImsServiceControllerBinder = iBinder;
                        ImsServiceController.this.setServiceController(iBinder);
                        ImsServiceController.this.notifyImsServiceReady();
                        Iterator it = ImsServiceController.this.mImsFeatures.iterator();
                        while (it.hasNext()) {
                            ImsServiceController.this.addImsServiceFeature((ImsFeatureConfiguration.FeatureSlotPair) it.next());
                        }
                    } catch (RemoteException e) {
                        ImsServiceController.this.mIsBound = false;
                        ImsServiceController.this.mIsBinding = false;
                        if (ImsServiceController.this.mImsDeathRecipient != null) {
                            ImsServiceController.this.mImsDeathRecipient.binderDied();
                        }
                        Log.e(ImsServiceController.LOG_TAG, "ImsService(" + componentName + ") RemoteException:" + e.getMessage());
                    }
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            synchronized (ImsServiceController.this.mLock) {
                ImsServiceController.this.mIsBinding = false;
            }
            cleanupConnection();
            Log.w(ImsServiceController.LOG_TAG, "ImsService(" + componentName + "): onServiceDisconnected. Waiting...");
        }

        @Override
        public void onBindingDied(ComponentName componentName) {
            synchronized (ImsServiceController.this.mLock) {
                ImsServiceController.this.mIsBinding = false;
                ImsServiceController.this.mIsBound = false;
            }
            cleanupConnection();
            Log.w(ImsServiceController.LOG_TAG, "ImsService(" + componentName + "): onBindingDied. Starting rebind...");
            ImsServiceController.this.startDelayedRebindToService();
        }

        private void cleanupConnection() {
            if (ImsServiceController.this.isServiceControllerAvailable()) {
                ImsServiceController.this.mImsServiceControllerBinder.unlinkToDeath(ImsServiceController.this.mImsDeathRecipient, 0);
            }
            ImsServiceController.this.cleanupAllFeatures();
            ImsServiceController.this.cleanUpService();
        }
    }

    private class ImsFeatureContainer {
        public int featureType;
        private IInterface mBinder;
        public int slotId;

        ImsFeatureContainer(int i, int i2, IInterface iInterface) {
            this.slotId = i;
            this.featureType = i2;
            this.mBinder = iInterface;
        }

        public <T extends IInterface> T resolve(Class<T> cls) {
            return cls.cast(this.mBinder);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            ImsFeatureContainer imsFeatureContainer = (ImsFeatureContainer) obj;
            if (this.slotId != imsFeatureContainer.slotId || this.featureType != imsFeatureContainer.featureType) {
                return false;
            }
            if (this.mBinder != null) {
                return this.mBinder.equals(imsFeatureContainer.mBinder);
            }
            if (imsFeatureContainer.mBinder == null) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (31 * ((this.slotId * 31) + this.featureType)) + (this.mBinder != null ? this.mBinder.hashCode() : 0);
        }
    }

    private class ImsFeatureStatusCallback {
        private final IImsFeatureStatusCallback mCallback = new IImsFeatureStatusCallback.Stub() {
            public void notifyImsFeatureStatus(int i) throws RemoteException {
                Log.i(ImsServiceController.LOG_TAG, "notifyImsFeatureStatus: slot=" + ImsFeatureStatusCallback.this.mSlotId + ", feature=" + ImsFeatureStatusCallback.this.mFeatureType + ", status=" + i);
                ImsServiceController.this.sendImsFeatureStatusChanged(ImsFeatureStatusCallback.this.mSlotId, ImsFeatureStatusCallback.this.mFeatureType, i);
            }
        };
        private int mFeatureType;
        private int mSlotId;

        ImsFeatureStatusCallback(int i, int i2) {
            this.mSlotId = i;
            this.mFeatureType = i2;
        }

        public IImsFeatureStatusCallback getCallback() {
            return this.mCallback;
        }
    }

    public ImsServiceController(Context context, ComponentName componentName, ImsServiceControllerCallbacks imsServiceControllerCallbacks) {
        this.mFeatureChangedListener = new ImsService.Listener() {
            public void onUpdateSupportedImsFeatures(ImsFeatureConfiguration imsFeatureConfiguration) {
                if (ImsServiceController.this.mCallbacks != null) {
                    ImsServiceController.this.mCallbacks.imsServiceFeaturesChanged(imsFeatureConfiguration, ImsServiceController.this);
                }
            }
        };
        this.mHandlerThread = new HandlerThread("ImsServiceControllerHandler");
        this.mIsBound = false;
        this.mIsBinding = false;
        this.mImsFeatureBinders = new HashSet<>();
        this.mImsStatusCallbacks = ConcurrentHashMap.newKeySet();
        this.mFeatureStatusCallbacks = new HashSet();
        this.mLock = new Object();
        this.mRestartImsServiceRunnable = new Runnable() {
            @Override
            public void run() {
                synchronized (ImsServiceController.this.mLock) {
                    if (ImsServiceController.this.mIsBound) {
                        return;
                    }
                    ImsServiceController.this.bind(ImsServiceController.this.mImsFeatures);
                }
            }
        };
        this.mRebindRetry = new RebindRetry() {
            @Override
            public long getStartDelay() {
                return 2000L;
            }

            @Override
            public long getMaximumDelay() {
                return 60000L;
            }
        };
        this.mContext = context;
        this.mComponentName = componentName;
        this.mCallbacks = imsServiceControllerCallbacks;
        this.mHandlerThread.start();
        this.mBackoff = new ExponentialBackoff(this.mRebindRetry.getStartDelay(), this.mRebindRetry.getMaximumDelay(), 2, this.mHandlerThread.getLooper(), this.mRestartImsServiceRunnable);
        this.mPackageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
    }

    @VisibleForTesting
    public ImsServiceController(Context context, ComponentName componentName, ImsServiceControllerCallbacks imsServiceControllerCallbacks, Handler handler, RebindRetry rebindRetry) {
        this.mFeatureChangedListener = new ImsService.Listener() {
            public void onUpdateSupportedImsFeatures(ImsFeatureConfiguration imsFeatureConfiguration) {
                if (ImsServiceController.this.mCallbacks != null) {
                    ImsServiceController.this.mCallbacks.imsServiceFeaturesChanged(imsFeatureConfiguration, ImsServiceController.this);
                }
            }
        };
        this.mHandlerThread = new HandlerThread("ImsServiceControllerHandler");
        this.mIsBound = false;
        this.mIsBinding = false;
        this.mImsFeatureBinders = new HashSet<>();
        this.mImsStatusCallbacks = ConcurrentHashMap.newKeySet();
        this.mFeatureStatusCallbacks = new HashSet();
        this.mLock = new Object();
        this.mRestartImsServiceRunnable = new Runnable() {
            @Override
            public void run() {
                synchronized (ImsServiceController.this.mLock) {
                    if (ImsServiceController.this.mIsBound) {
                        return;
                    }
                    ImsServiceController.this.bind(ImsServiceController.this.mImsFeatures);
                }
            }
        };
        this.mRebindRetry = new RebindRetry() {
            @Override
            public long getStartDelay() {
                return 2000L;
            }

            @Override
            public long getMaximumDelay() {
                return 60000L;
            }
        };
        this.mContext = context;
        this.mComponentName = componentName;
        this.mCallbacks = imsServiceControllerCallbacks;
        this.mBackoff = new ExponentialBackoff(rebindRetry.getStartDelay(), rebindRetry.getMaximumDelay(), 2, handler, this.mRestartImsServiceRunnable);
        this.mPackageManager = null;
    }

    public boolean bind(HashSet<ImsFeatureConfiguration.FeatureSlotPair> hashSet) {
        synchronized (this.mLock) {
            if (this.mIsBound || this.mIsBinding) {
                return false;
            }
            this.mIsBinding = true;
            this.mImsFeatures = hashSet;
            grantPermissionsToService();
            Intent component = new Intent(getServiceInterface()).setComponent(this.mComponentName);
            this.mImsServiceConnection = new ImsServiceConnection();
            Log.i(LOG_TAG, "Binding ImsService:" + this.mComponentName);
            try {
                boolean zStartBindToService = startBindToService(component, this.mImsServiceConnection, 67108929);
                if (!zStartBindToService) {
                    this.mIsBinding = false;
                    this.mBackoff.notifyFailed();
                }
                return zStartBindToService;
            } catch (Exception e) {
                this.mBackoff.notifyFailed();
                Log.e(LOG_TAG, "Error binding (" + this.mComponentName + ") with exception: " + e.getMessage() + ", rebinding in " + this.mBackoff.getCurrentDelay() + " ms");
                return false;
            }
        }
    }

    protected boolean startBindToService(Intent intent, ImsServiceConnection imsServiceConnection, int i) {
        return this.mContext.bindService(intent, imsServiceConnection, i);
    }

    public void unbind() throws RemoteException {
        synchronized (this.mLock) {
            this.mBackoff.stop();
            if (this.mImsServiceConnection != null && this.mImsDeathRecipient != null) {
                changeImsServiceFeatures(new HashSet<>());
                removeImsServiceFeatureCallbacks();
                this.mImsServiceControllerBinder.unlinkToDeath(this.mImsDeathRecipient, 0);
                Log.i(LOG_TAG, "Unbinding ImsService: " + this.mComponentName);
                this.mContext.unbindService(this.mImsServiceConnection);
                cleanUpService();
            }
        }
    }

    public void changeImsServiceFeatures(HashSet<ImsFeatureConfiguration.FeatureSlotPair> hashSet) throws RemoteException {
        synchronized (this.mLock) {
            Log.i(LOG_TAG, "Features changed (" + this.mImsFeatures + "->" + hashSet + ") for ImsService: " + this.mComponentName);
            HashSet hashSet2 = new HashSet(this.mImsFeatures);
            this.mImsFeatures = hashSet;
            if (this.mIsBound) {
                HashSet hashSet3 = new HashSet(this.mImsFeatures);
                hashSet3.removeAll(hashSet2);
                Iterator it = hashSet3.iterator();
                while (it.hasNext()) {
                    addImsServiceFeature((ImsFeatureConfiguration.FeatureSlotPair) it.next());
                }
                HashSet hashSet4 = new HashSet(hashSet2);
                hashSet4.removeAll(this.mImsFeatures);
                Iterator it2 = hashSet4.iterator();
                while (it2.hasNext()) {
                    removeImsServiceFeature((ImsFeatureConfiguration.FeatureSlotPair) it2.next());
                }
            }
        }
    }

    @VisibleForTesting
    public IImsServiceController getImsServiceController() {
        return this.mIImsServiceController;
    }

    @VisibleForTesting
    public IBinder getImsServiceControllerBinder() {
        return this.mImsServiceControllerBinder;
    }

    @VisibleForTesting
    public long getRebindDelay() {
        return this.mBackoff.getCurrentDelay();
    }

    public ComponentName getComponentName() {
        return this.mComponentName;
    }

    public void addImsServiceFeatureCallback(IImsServiceFeatureCallback iImsServiceFeatureCallback) {
        this.mImsStatusCallbacks.add(iImsServiceFeatureCallback);
        synchronized (this.mLock) {
            if (this.mImsFeatures == null || this.mImsFeatures.isEmpty()) {
                return;
            }
            try {
                for (ImsFeatureConfiguration.FeatureSlotPair featureSlotPair : this.mImsFeatures) {
                    iImsServiceFeatureCallback.imsFeatureCreated(featureSlotPair.slotId, featureSlotPair.featureType);
                }
            } catch (RemoteException e) {
                Log.w(LOG_TAG, "addImsServiceFeatureCallback: exception notifying callback");
            }
        }
    }

    public void enableIms(int i) {
        try {
            synchronized (this.mLock) {
                if (isServiceControllerAvailable()) {
                    this.mIImsServiceController.enableIms(i);
                }
            }
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "Couldn't enable IMS: " + e.getMessage());
        }
    }

    public void disableIms(int i) {
        try {
            synchronized (this.mLock) {
                if (isServiceControllerAvailable()) {
                    this.mIImsServiceController.disableIms(i);
                }
            }
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "Couldn't disable IMS: " + e.getMessage());
        }
    }

    public IImsMmTelFeature getMmTelFeature(int i) {
        synchronized (this.mLock) {
            ImsFeatureContainer imsFeatureContainer = getImsFeatureContainer(i, 1);
            if (imsFeatureContainer == null) {
                Log.w(LOG_TAG, "Requested null MMTelFeature on slot " + i);
                return null;
            }
            return imsFeatureContainer.resolve(IImsMmTelFeature.class);
        }
    }

    public IImsRcsFeature getRcsFeature(int i) {
        synchronized (this.mLock) {
            ImsFeatureContainer imsFeatureContainer = getImsFeatureContainer(i, 2);
            if (imsFeatureContainer == null) {
                Log.w(LOG_TAG, "Requested null RcsFeature on slot " + i);
                return null;
            }
            return imsFeatureContainer.resolve(IImsRcsFeature.class);
        }
    }

    public IImsRegistration getRegistration(int i) throws RemoteException {
        IImsRegistration registration;
        synchronized (this.mLock) {
            registration = isServiceControllerAvailable() ? this.mIImsServiceController.getRegistration(i) : null;
        }
        return registration;
    }

    public IImsConfig getConfig(int i) throws RemoteException {
        IImsConfig config;
        synchronized (this.mLock) {
            config = isServiceControllerAvailable() ? this.mIImsServiceController.getConfig(i) : null;
        }
        return config;
    }

    protected void notifyImsServiceReady() throws RemoteException {
        synchronized (this.mLock) {
            if (isServiceControllerAvailable()) {
                Log.d(LOG_TAG, "notifyImsServiceReady");
                this.mIImsServiceController.setListener(this.mFeatureChangedListener);
                this.mIImsServiceController.notifyImsServiceReadyForFeatureCreation();
            }
        }
    }

    protected String getServiceInterface() {
        return "android.telephony.ims.ImsService";
    }

    protected void setServiceController(IBinder iBinder) {
        this.mIImsServiceController = IImsServiceController.Stub.asInterface(iBinder);
    }

    public boolean isBound() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mIsBound;
        }
        return z;
    }

    protected boolean isServiceControllerAvailable() {
        return this.mIImsServiceController != null;
    }

    @VisibleForTesting
    public void removeImsServiceFeatureCallbacks() {
        this.mImsStatusCallbacks.clear();
    }

    private void startDelayedRebindToService() {
        this.mBackoff.start();
    }

    private void grantPermissionsToService() {
        Log.i(LOG_TAG, "Granting Runtime permissions to:" + getComponentName());
        String[] strArr = {this.mComponentName.getPackageName()};
        try {
            if (this.mPackageManager != null) {
                this.mPackageManager.grantDefaultPermissionsToEnabledImsServices(strArr, this.mContext.getUserId());
            }
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "Unable to grant permissions, binder died.");
        }
    }

    private void sendImsFeatureCreatedCallback(int i, int i2) {
        Iterator<IImsServiceFeatureCallback> it = this.mImsStatusCallbacks.iterator();
        while (it.hasNext()) {
            try {
                it.next().imsFeatureCreated(i, i2);
            } catch (RemoteException e) {
                Log.w(LOG_TAG, "sendImsFeatureCreatedCallback: Binder died, removing callback. Exception:" + e.getMessage());
                it.remove();
            }
        }
    }

    private void sendImsFeatureRemovedCallback(int i, int i2) {
        Iterator<IImsServiceFeatureCallback> it = this.mImsStatusCallbacks.iterator();
        while (it.hasNext()) {
            try {
                it.next().imsFeatureRemoved(i, i2);
            } catch (RemoteException e) {
                Log.w(LOG_TAG, "sendImsFeatureRemovedCallback: Binder died, removing callback. Exception:" + e.getMessage());
                it.remove();
            }
        }
    }

    private void sendImsFeatureStatusChanged(int i, int i2, int i3) {
        Iterator<IImsServiceFeatureCallback> it = this.mImsStatusCallbacks.iterator();
        while (it.hasNext()) {
            try {
                it.next().imsStatusChanged(i, i2, i3);
            } catch (RemoteException e) {
                Log.w(LOG_TAG, "sendImsFeatureStatusChanged: Binder died, removing callback. Exception:" + e.getMessage());
                it.remove();
            }
        }
    }

    private void addImsServiceFeature(ImsFeatureConfiguration.FeatureSlotPair featureSlotPair) throws RemoteException {
        if (!isServiceControllerAvailable() || this.mCallbacks == null) {
            Log.w(LOG_TAG, "addImsServiceFeature called with null values.");
            return;
        }
        if (featureSlotPair.featureType != 0) {
            ImsFeatureStatusCallback imsFeatureStatusCallback = new ImsFeatureStatusCallback(featureSlotPair.slotId, featureSlotPair.featureType);
            this.mFeatureStatusCallbacks.add(imsFeatureStatusCallback);
            addImsFeatureBinder(featureSlotPair.slotId, featureSlotPair.featureType, createImsFeature(featureSlotPair.slotId, featureSlotPair.featureType, imsFeatureStatusCallback.getCallback()));
            this.mCallbacks.imsServiceFeatureCreated(featureSlotPair.slotId, featureSlotPair.featureType, this);
        } else {
            Log.i(LOG_TAG, "supports emergency calling on slot " + featureSlotPair.slotId);
        }
        sendImsFeatureCreatedCallback(featureSlotPair.slotId, featureSlotPair.featureType);
    }

    private void removeImsServiceFeature(final ImsFeatureConfiguration.FeatureSlotPair featureSlotPair) {
        if (!isServiceControllerAvailable() || this.mCallbacks == null) {
            Log.w(LOG_TAG, "removeImsServiceFeature called with null values.");
            return;
        }
        if (featureSlotPair.featureType != 0) {
            ImsFeatureStatusCallback imsFeatureStatusCallbackOrElse = this.mFeatureStatusCallbacks.stream().filter(new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return ImsServiceController.lambda$removeImsServiceFeature$0(featureSlotPair, (ImsServiceController.ImsFeatureStatusCallback) obj);
                }
            }).findFirst().orElse(null);
            if (imsFeatureStatusCallbackOrElse != null) {
                this.mFeatureStatusCallbacks.remove(imsFeatureStatusCallbackOrElse);
            }
            removeImsFeatureBinder(featureSlotPair.slotId, featureSlotPair.featureType);
            this.mCallbacks.imsServiceFeatureRemoved(featureSlotPair.slotId, featureSlotPair.featureType, this);
            try {
                removeImsFeature(featureSlotPair.slotId, featureSlotPair.featureType, imsFeatureStatusCallbackOrElse != null ? imsFeatureStatusCallbackOrElse.getCallback() : null);
            } catch (RemoteException e) {
                Log.i(LOG_TAG, "Couldn't remove feature {" + featureSlotPair.featureType + "}, connection is down: " + e.getMessage());
            }
        } else {
            Log.i(LOG_TAG, "doesn't support emergency calling on slot " + featureSlotPair.slotId);
        }
        sendImsFeatureRemovedCallback(featureSlotPair.slotId, featureSlotPair.featureType);
    }

    static boolean lambda$removeImsServiceFeature$0(ImsFeatureConfiguration.FeatureSlotPair featureSlotPair, ImsFeatureStatusCallback imsFeatureStatusCallback) {
        return imsFeatureStatusCallback.mSlotId == featureSlotPair.slotId && imsFeatureStatusCallback.mFeatureType == featureSlotPair.featureType;
    }

    protected IInterface createImsFeature(int i, int i2, IImsFeatureStatusCallback iImsFeatureStatusCallback) throws RemoteException {
        switch (i2) {
            case 1:
                return this.mIImsServiceController.createMmTelFeature(i, iImsFeatureStatusCallback);
            case 2:
                return this.mIImsServiceController.createRcsFeature(i, iImsFeatureStatusCallback);
            default:
                return null;
        }
    }

    protected void removeImsFeature(int i, int i2, IImsFeatureStatusCallback iImsFeatureStatusCallback) throws RemoteException {
        this.mIImsServiceController.removeImsFeature(i, i2, iImsFeatureStatusCallback);
    }

    private void addImsFeatureBinder(int i, int i2, IInterface iInterface) {
        this.mImsFeatureBinders.add(new ImsFeatureContainer(i, i2, iInterface));
    }

    private void removeImsFeatureBinder(final int i, final int i2) {
        ImsFeatureContainer imsFeatureContainer = (ImsFeatureContainer) this.mImsFeatureBinders.stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return ImsServiceController.lambda$removeImsFeatureBinder$1(i, i2, (ImsServiceController.ImsFeatureContainer) obj);
            }
        }).findFirst().orElse(null);
        if (imsFeatureContainer != null) {
            this.mImsFeatureBinders.remove(imsFeatureContainer);
        }
    }

    static boolean lambda$removeImsFeatureBinder$1(int i, int i2, ImsFeatureContainer imsFeatureContainer) {
        return imsFeatureContainer.slotId == i && imsFeatureContainer.featureType == i2;
    }

    private ImsFeatureContainer getImsFeatureContainer(final int i, final int i2) {
        return (ImsFeatureContainer) this.mImsFeatureBinders.stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return ImsServiceController.lambda$getImsFeatureContainer$2(i, i2, (ImsServiceController.ImsFeatureContainer) obj);
            }
        }).findFirst().orElse(null);
    }

    static boolean lambda$getImsFeatureContainer$2(int i, int i2, ImsFeatureContainer imsFeatureContainer) {
        return imsFeatureContainer.slotId == i && imsFeatureContainer.featureType == i2;
    }

    private void cleanupAllFeatures() {
        synchronized (this.mLock) {
            Iterator<ImsFeatureConfiguration.FeatureSlotPair> it = this.mImsFeatures.iterator();
            while (it.hasNext()) {
                removeImsServiceFeature(it.next());
            }
            removeImsServiceFeatureCallbacks();
        }
    }

    private void cleanUpService() {
        synchronized (this.mLock) {
            this.mImsDeathRecipient = null;
            this.mImsServiceConnection = null;
            this.mImsServiceControllerBinder = null;
            setServiceController(null);
        }
    }
}
