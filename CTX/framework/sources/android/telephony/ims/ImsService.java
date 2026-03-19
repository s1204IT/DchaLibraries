package android.telephony.ims;

import android.annotation.SystemApi;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.ims.aidl.IImsConfig;
import android.telephony.ims.aidl.IImsMmTelFeature;
import android.telephony.ims.aidl.IImsRcsFeature;
import android.telephony.ims.aidl.IImsRegistration;
import android.telephony.ims.aidl.IImsServiceController;
import android.telephony.ims.aidl.IImsServiceControllerListener;
import android.telephony.ims.feature.ImsFeature;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.feature.RcsFeature;
import android.telephony.ims.stub.ImsConfigImplBase;
import android.telephony.ims.stub.ImsFeatureConfiguration;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.util.Log;
import android.util.SparseArray;
import com.android.ims.internal.IImsFeatureStatusCallback;
import com.android.internal.annotations.VisibleForTesting;

@SystemApi
public class ImsService extends Service {
    private static final String LOG_TAG = "ImsService";
    public static final String SERVICE_INTERFACE = "android.telephony.ims.ImsService";
    private final SparseArray<SparseArray<ImsFeature>> mFeaturesBySlot = new SparseArray<>();
    protected final IBinder mImsServiceController = new IImsServiceController.Stub() {
        @Override
        public void setListener(IImsServiceControllerListener iImsServiceControllerListener) {
            ImsService.this.mListener = iImsServiceControllerListener;
        }

        @Override
        public IImsMmTelFeature createMmTelFeature(int i, IImsFeatureStatusCallback iImsFeatureStatusCallback) {
            return ImsService.this.createMmTelFeatureInternal(i, iImsFeatureStatusCallback);
        }

        @Override
        public IImsRcsFeature createRcsFeature(int i, IImsFeatureStatusCallback iImsFeatureStatusCallback) {
            return ImsService.this.createRcsFeatureInternal(i, iImsFeatureStatusCallback);
        }

        @Override
        public void removeImsFeature(int i, int i2, IImsFeatureStatusCallback iImsFeatureStatusCallback) {
            ImsService.this.removeImsFeature(i, i2, iImsFeatureStatusCallback);
        }

        @Override
        public ImsFeatureConfiguration querySupportedImsFeatures() {
            return ImsService.this.querySupportedImsFeatures();
        }

        @Override
        public void notifyImsServiceReadyForFeatureCreation() {
            ImsService.this.readyForFeatureCreation();
        }

        @Override
        public IImsConfig getConfig(int i) {
            ImsConfigImplBase config = ImsService.this.getConfig(i);
            if (config != null) {
                return config.getIImsConfig();
            }
            return null;
        }

        @Override
        public IImsRegistration getRegistration(int i) {
            ImsRegistrationImplBase registration = ImsService.this.getRegistration(i);
            if (registration != null) {
                return registration.getBinder();
            }
            return null;
        }

        @Override
        public void enableIms(int i) {
            ImsService.this.enableIms(i);
        }

        @Override
        public void disableIms(int i) {
            ImsService.this.disableIms(i);
        }
    };
    private IImsServiceControllerListener mListener;

    public static class Listener extends IImsServiceControllerListener.Stub {
        @Override
        public void onUpdateSupportedImsFeatures(ImsFeatureConfiguration imsFeatureConfiguration) {
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (SERVICE_INTERFACE.equals(intent.getAction())) {
            Log.i(LOG_TAG, "ImsService Bound.");
            return this.mImsServiceController;
        }
        return null;
    }

    @VisibleForTesting
    public SparseArray<ImsFeature> getFeatures(int i) {
        return this.mFeaturesBySlot.get(i);
    }

    private IImsMmTelFeature createMmTelFeatureInternal(int i, IImsFeatureStatusCallback iImsFeatureStatusCallback) {
        MmTelFeature mmTelFeatureCreateMmTelFeature = createMmTelFeature(i);
        if (mmTelFeatureCreateMmTelFeature != null) {
            setupFeature(mmTelFeatureCreateMmTelFeature, i, 1, iImsFeatureStatusCallback);
            return mmTelFeatureCreateMmTelFeature.getBinder();
        }
        Log.e(LOG_TAG, "createMmTelFeatureInternal: null feature returned.");
        return null;
    }

    private IImsRcsFeature createRcsFeatureInternal(int i, IImsFeatureStatusCallback iImsFeatureStatusCallback) {
        RcsFeature rcsFeatureCreateRcsFeature = createRcsFeature(i);
        if (rcsFeatureCreateRcsFeature != null) {
            setupFeature(rcsFeatureCreateRcsFeature, i, 2, iImsFeatureStatusCallback);
            return rcsFeatureCreateRcsFeature.getBinder();
        }
        Log.e(LOG_TAG, "createRcsFeatureInternal: null feature returned.");
        return null;
    }

    private void setupFeature(ImsFeature imsFeature, int i, int i2, IImsFeatureStatusCallback iImsFeatureStatusCallback) {
        imsFeature.addImsFeatureStatusCallback(iImsFeatureStatusCallback);
        imsFeature.initialize(this, i);
        addImsFeature(i, i2, imsFeature);
    }

    private void addImsFeature(int i, int i2, ImsFeature imsFeature) {
        synchronized (this.mFeaturesBySlot) {
            SparseArray<ImsFeature> sparseArray = this.mFeaturesBySlot.get(i);
            if (sparseArray == null) {
                sparseArray = new SparseArray<>();
                this.mFeaturesBySlot.put(i, sparseArray);
            }
            sparseArray.put(i2, imsFeature);
        }
    }

    private void removeImsFeature(int i, int i2, IImsFeatureStatusCallback iImsFeatureStatusCallback) {
        synchronized (this.mFeaturesBySlot) {
            SparseArray<ImsFeature> sparseArray = this.mFeaturesBySlot.get(i);
            if (sparseArray == null) {
                Log.w(LOG_TAG, "Can not remove ImsFeature. No ImsFeatures exist on slot " + i);
                return;
            }
            ImsFeature imsFeature = sparseArray.get(i2);
            if (imsFeature == null) {
                Log.w(LOG_TAG, "Can not remove ImsFeature. No feature with type " + i2 + " exists on slot " + i);
                return;
            }
            imsFeature.removeImsFeatureStatusCallback(iImsFeatureStatusCallback);
            imsFeature.onFeatureRemoved();
            sparseArray.remove(i2);
        }
    }

    public ImsFeatureConfiguration querySupportedImsFeatures() {
        return new ImsFeatureConfiguration();
    }

    public final void onUpdateSupportedImsFeatures(ImsFeatureConfiguration imsFeatureConfiguration) throws RemoteException {
        if (this.mListener == null) {
            throw new IllegalStateException("Framework is not ready");
        }
        this.mListener.onUpdateSupportedImsFeatures(imsFeatureConfiguration);
    }

    public void readyForFeatureCreation() {
    }

    public void enableIms(int i) {
    }

    public void disableIms(int i) {
    }

    public MmTelFeature createMmTelFeature(int i) {
        return null;
    }

    public RcsFeature createRcsFeature(int i) {
        return null;
    }

    public ImsConfigImplBase getConfig(int i) {
        return new ImsConfigImplBase();
    }

    public ImsRegistrationImplBase getRegistration(int i) {
        return new ImsRegistrationImplBase();
    }
}
