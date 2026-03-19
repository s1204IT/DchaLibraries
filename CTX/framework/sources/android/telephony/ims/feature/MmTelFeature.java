package android.telephony.ims.feature;

import android.annotation.SystemApi;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.aidl.IImsCapabilityCallback;
import android.telephony.ims.aidl.IImsMmTelFeature;
import android.telephony.ims.aidl.IImsMmTelListener;
import android.telephony.ims.aidl.IImsSmsListener;
import android.telephony.ims.feature.ImsFeature;
import android.telephony.ims.stub.ImsCallSessionImplBase;
import android.telephony.ims.stub.ImsEcbmImplBase;
import android.telephony.ims.stub.ImsMultiEndpointImplBase;
import android.telephony.ims.stub.ImsSmsImplBase;
import android.telephony.ims.stub.ImsUtImplBase;
import android.util.Log;
import com.android.ims.internal.IImsCallSession;
import com.android.ims.internal.IImsEcbm;
import com.android.ims.internal.IImsMultiEndpoint;
import com.android.ims.internal.IImsUt;
import com.android.internal.annotations.VisibleForTesting;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@SystemApi
public class MmTelFeature extends ImsFeature {
    private static final String LOG_TAG = "MmTelFeature";
    public static final int PROCESS_CALL_CSFB = 1;
    public static final int PROCESS_CALL_IMS = 0;
    private IImsMmTelListener mListener;
    private final IImsMmTelFeature mImsMMTelBinder = new IImsMmTelFeature.Stub() {
        @Override
        public void setListener(IImsMmTelListener iImsMmTelListener) throws RemoteException {
            synchronized (MmTelFeature.this.mLock) {
                MmTelFeature.this.setListener(iImsMmTelListener);
            }
        }

        @Override
        public int getFeatureState() throws RemoteException {
            int featureState;
            synchronized (MmTelFeature.this.mLock) {
                try {
                    try {
                        featureState = MmTelFeature.this.getFeatureState();
                    } catch (Exception e) {
                        throw new RemoteException(e.getMessage());
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            return featureState;
        }

        @Override
        public ImsCallProfile createCallProfile(int i, int i2) throws RemoteException {
            ImsCallProfile imsCallProfileCreateCallProfile;
            synchronized (MmTelFeature.this.mLock) {
                try {
                    try {
                        imsCallProfileCreateCallProfile = MmTelFeature.this.createCallProfile(i, i2);
                    } catch (Exception e) {
                        throw new RemoteException(e.getMessage());
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            return imsCallProfileCreateCallProfile;
        }

        @Override
        public IImsCallSession createCallSession(ImsCallProfile imsCallProfile) throws RemoteException {
            IImsCallSession iImsCallSessionCreateCallSessionInterface;
            synchronized (MmTelFeature.this.mLock) {
                iImsCallSessionCreateCallSessionInterface = MmTelFeature.this.createCallSessionInterface(imsCallProfile);
            }
            return iImsCallSessionCreateCallSessionInterface;
        }

        @Override
        public int shouldProcessCall(String[] strArr) {
            int iShouldProcessCall;
            synchronized (MmTelFeature.this.mLock) {
                iShouldProcessCall = MmTelFeature.this.shouldProcessCall(strArr);
            }
            return iShouldProcessCall;
        }

        @Override
        public IImsUt getUtInterface() throws RemoteException {
            IImsUt utInterface;
            synchronized (MmTelFeature.this.mLock) {
                utInterface = MmTelFeature.this.getUtInterface();
            }
            return utInterface;
        }

        @Override
        public IImsEcbm getEcbmInterface() throws RemoteException {
            IImsEcbm ecbmInterface;
            synchronized (MmTelFeature.this.mLock) {
                ecbmInterface = MmTelFeature.this.getEcbmInterface();
            }
            return ecbmInterface;
        }

        @Override
        public void setUiTtyMode(int i, Message message) throws RemoteException {
            synchronized (MmTelFeature.this.mLock) {
                try {
                    try {
                        MmTelFeature.this.setUiTtyMode(i, message);
                    } catch (Exception e) {
                        throw new RemoteException(e.getMessage());
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }

        @Override
        public IImsMultiEndpoint getMultiEndpointInterface() throws RemoteException {
            IImsMultiEndpoint multiEndpointInterface;
            synchronized (MmTelFeature.this.mLock) {
                multiEndpointInterface = MmTelFeature.this.getMultiEndpointInterface();
            }
            return multiEndpointInterface;
        }

        @Override
        public int queryCapabilityStatus() throws RemoteException {
            int i;
            synchronized (MmTelFeature.this.mLock) {
                i = MmTelFeature.this.queryCapabilityStatus().mCapabilities;
            }
            return i;
        }

        @Override
        public void addCapabilityCallback(IImsCapabilityCallback iImsCapabilityCallback) {
            MmTelFeature.this.addCapabilityCallback(iImsCapabilityCallback);
        }

        @Override
        public void removeCapabilityCallback(IImsCapabilityCallback iImsCapabilityCallback) {
            MmTelFeature.this.removeCapabilityCallback(iImsCapabilityCallback);
        }

        @Override
        public void changeCapabilitiesConfiguration(CapabilityChangeRequest capabilityChangeRequest, IImsCapabilityCallback iImsCapabilityCallback) throws RemoteException {
            synchronized (MmTelFeature.this.mLock) {
                MmTelFeature.this.requestChangeEnabledCapabilities(capabilityChangeRequest, iImsCapabilityCallback);
            }
        }

        @Override
        public void queryCapabilityConfiguration(int i, int i2, IImsCapabilityCallback iImsCapabilityCallback) {
            synchronized (MmTelFeature.this.mLock) {
                MmTelFeature.this.queryCapabilityConfigurationInternal(i, i2, iImsCapabilityCallback);
            }
        }

        @Override
        public void setSmsListener(IImsSmsListener iImsSmsListener) throws RemoteException {
            synchronized (MmTelFeature.this.mLock) {
                MmTelFeature.this.setSmsListener(iImsSmsListener);
            }
        }

        @Override
        public void sendSms(int i, int i2, String str, String str2, boolean z, byte[] bArr) {
            synchronized (MmTelFeature.this.mLock) {
                MmTelFeature.this.sendSms(i, i2, str, str2, z, bArr);
            }
        }

        @Override
        public void acknowledgeSms(int i, int i2, int i3) {
            synchronized (MmTelFeature.this.mLock) {
                MmTelFeature.this.acknowledgeSms(i, i2, i3);
            }
        }

        @Override
        public void acknowledgeSmsReport(int i, int i2, int i3) {
            synchronized (MmTelFeature.this.mLock) {
                MmTelFeature.this.acknowledgeSmsReport(i, i2, i3);
            }
        }

        @Override
        public String getSmsFormat() {
            String smsFormat;
            synchronized (MmTelFeature.this.mLock) {
                smsFormat = MmTelFeature.this.getSmsFormat();
            }
            return smsFormat;
        }

        @Override
        public void onSmsReady() {
            synchronized (MmTelFeature.this.mLock) {
                MmTelFeature.this.onSmsReady();
            }
        }
    };
    private final Object mLock = new Object();

    @Retention(RetentionPolicy.SOURCE)
    public @interface ProcessCallResult {
    }

    public static class MmTelCapabilities extends ImsFeature.Capabilities {
        public static final int CAPABILITY_TYPE_SMS = 8;
        public static final int CAPABILITY_TYPE_UT = 4;
        public static final int CAPABILITY_TYPE_VIDEO = 2;
        public static final int CAPABILITY_TYPE_VOICE = 1;

        @Retention(RetentionPolicy.SOURCE)
        public @interface MmTelCapability {
        }

        @VisibleForTesting
        public MmTelCapabilities() {
        }

        public MmTelCapabilities(ImsFeature.Capabilities capabilities) {
            this.mCapabilities = capabilities.mCapabilities;
        }

        public MmTelCapabilities(int i) {
            this.mCapabilities = i;
        }

        @Override
        public final void addCapabilities(int i) {
            super.addCapabilities(i);
        }

        @Override
        public final void removeCapabilities(int i) {
            super.removeCapabilities(i);
        }

        @Override
        public final boolean isCapable(int i) {
            return super.isCapable(i);
        }

        @Override
        public String toString() {
            return "MmTel Capabilities - [Voice: " + isCapable(1) + " Video: " + isCapable(2) + " UT: " + isCapable(4) + " SMS: " + isCapable(8) + "]";
        }
    }

    public static class Listener extends IImsMmTelListener.Stub {
        @Override
        public void onIncomingCall(IImsCallSession iImsCallSession, Bundle bundle) {
        }

        @Override
        public void onVoiceMessageCountUpdate(int i) {
        }
    }

    private void setListener(IImsMmTelListener iImsMmTelListener) {
        synchronized (this.mLock) {
            this.mListener = iImsMmTelListener;
        }
        if (this.mListener != null) {
            onFeatureReady();
        }
    }

    private void queryCapabilityConfigurationInternal(int i, int i2, IImsCapabilityCallback iImsCapabilityCallback) {
        boolean zQueryCapabilityConfiguration = queryCapabilityConfiguration(i, i2);
        if (iImsCapabilityCallback != null) {
            try {
                iImsCapabilityCallback.onQueryCapabilityConfiguration(i, i2, zQueryCapabilityConfiguration);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "queryCapabilityConfigurationInternal called on dead binder!");
            }
        }
    }

    @Override
    public final MmTelCapabilities queryCapabilityStatus() {
        return new MmTelCapabilities(super.queryCapabilityStatus());
    }

    public final void notifyCapabilitiesStatusChanged(MmTelCapabilities mmTelCapabilities) {
        super.notifyCapabilitiesStatusChanged((ImsFeature.Capabilities) mmTelCapabilities);
    }

    public final void notifyIncomingCall(ImsCallSessionImplBase imsCallSessionImplBase, Bundle bundle) {
        synchronized (this.mLock) {
            if (this.mListener == null) {
                throw new IllegalStateException("Session is not available.");
            }
            try {
                this.mListener.onIncomingCall(imsCallSessionImplBase.getServiceImpl(), bundle);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public final void notifyIncomingCallSession(IImsCallSession iImsCallSession, Bundle bundle) {
        synchronized (this.mLock) {
            if (this.mListener == null) {
                throw new IllegalStateException("Session is not available.");
            }
            try {
                this.mListener.onIncomingCall(iImsCallSession, bundle);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public final void notifyVoiceMessageCountUpdate(int i) {
        synchronized (this.mLock) {
            if (this.mListener == null) {
                throw new IllegalStateException("Session is not available.");
            }
            try {
                this.mListener.onVoiceMessageCountUpdate(i);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public boolean queryCapabilityConfiguration(int i, int i2) {
        return false;
    }

    @Override
    public void changeEnabledCapabilities(CapabilityChangeRequest capabilityChangeRequest, ImsFeature.CapabilityCallbackProxy capabilityCallbackProxy) {
    }

    public ImsCallProfile createCallProfile(int i, int i2) {
        return null;
    }

    public IImsCallSession createCallSessionInterface(ImsCallProfile imsCallProfile) throws RemoteException {
        ImsCallSessionImplBase imsCallSessionImplBaseCreateCallSession = createCallSession(imsCallProfile);
        if (imsCallSessionImplBaseCreateCallSession != null) {
            return imsCallSessionImplBaseCreateCallSession.getServiceImpl();
        }
        return null;
    }

    public ImsCallSessionImplBase createCallSession(ImsCallProfile imsCallProfile) {
        return null;
    }

    public int shouldProcessCall(String[] strArr) {
        return 0;
    }

    protected IImsUt getUtInterface() throws RemoteException {
        ImsUtImplBase ut = getUt();
        if (ut != null) {
            return ut.getInterface();
        }
        return null;
    }

    protected IImsEcbm getEcbmInterface() throws RemoteException {
        ImsEcbmImplBase ecbm = getEcbm();
        if (ecbm != null) {
            return ecbm.getImsEcbm();
        }
        return null;
    }

    public IImsMultiEndpoint getMultiEndpointInterface() throws RemoteException {
        ImsMultiEndpointImplBase multiEndpoint = getMultiEndpoint();
        if (multiEndpoint != null) {
            return multiEndpoint.getIImsMultiEndpoint();
        }
        return null;
    }

    public ImsUtImplBase getUt() {
        return new ImsUtImplBase();
    }

    public ImsEcbmImplBase getEcbm() {
        return new ImsEcbmImplBase();
    }

    public ImsMultiEndpointImplBase getMultiEndpoint() {
        return new ImsMultiEndpointImplBase();
    }

    public void setUiTtyMode(int i, Message message) {
    }

    private void setSmsListener(IImsSmsListener iImsSmsListener) {
        getSmsImplementation().registerSmsListener(iImsSmsListener);
    }

    private void sendSms(int i, int i2, String str, String str2, boolean z, byte[] bArr) {
        getSmsImplementation().sendSms(i, i2, str, str2, z, bArr);
    }

    private void acknowledgeSms(int i, int i2, int i3) {
        getSmsImplementation().acknowledgeSms(i, i2, i3);
    }

    private void acknowledgeSmsReport(int i, int i2, int i3) {
        getSmsImplementation().acknowledgeSmsReport(i, i2, i3);
    }

    private void onSmsReady() {
        getSmsImplementation().onReady();
    }

    public ImsSmsImplBase getSmsImplementation() {
        return new ImsSmsImplBase();
    }

    private String getSmsFormat() {
        return getSmsImplementation().getSmsFormat();
    }

    @Override
    public void onFeatureRemoved() {
    }

    @Override
    public void onFeatureReady() {
    }

    @Override
    public final IImsMmTelFeature getBinder() {
        return this.mImsMMTelBinder;
    }
}
