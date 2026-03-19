package android.telephony.ims.compat.feature;

import android.app.PendingIntent;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.stub.ImsEcbmImplBase;
import android.telephony.ims.stub.ImsMultiEndpointImplBase;
import android.telephony.ims.stub.ImsUtImplBase;
import com.android.ims.internal.IImsCallSession;
import com.android.ims.internal.IImsCallSessionListener;
import com.android.ims.internal.IImsConfig;
import com.android.ims.internal.IImsEcbm;
import com.android.ims.internal.IImsMMTelFeature;
import com.android.ims.internal.IImsMultiEndpoint;
import com.android.ims.internal.IImsRegistrationListener;
import com.android.ims.internal.IImsUt;

public class MMTelFeature extends ImsFeature {
    private final Object mLock = new Object();
    private final IImsMMTelFeature mImsMMTelBinder = new IImsMMTelFeature.Stub() {
        @Override
        public int startSession(PendingIntent pendingIntent, IImsRegistrationListener iImsRegistrationListener) throws RemoteException {
            int iStartSession;
            synchronized (MMTelFeature.this.mLock) {
                iStartSession = MMTelFeature.this.startSession(pendingIntent, iImsRegistrationListener);
            }
            return iStartSession;
        }

        @Override
        public void endSession(int i) throws RemoteException {
            synchronized (MMTelFeature.this.mLock) {
                MMTelFeature.this.endSession(i);
            }
        }

        @Override
        public boolean isConnected(int i, int i2) throws RemoteException {
            boolean zIsConnected;
            synchronized (MMTelFeature.this.mLock) {
                zIsConnected = MMTelFeature.this.isConnected(i, i2);
            }
            return zIsConnected;
        }

        @Override
        public boolean isOpened() throws RemoteException {
            boolean zIsOpened;
            synchronized (MMTelFeature.this.mLock) {
                zIsOpened = MMTelFeature.this.isOpened();
            }
            return zIsOpened;
        }

        @Override
        public int getFeatureStatus() throws RemoteException {
            int featureState;
            synchronized (MMTelFeature.this.mLock) {
                featureState = MMTelFeature.this.getFeatureState();
            }
            return featureState;
        }

        @Override
        public void addRegistrationListener(IImsRegistrationListener iImsRegistrationListener) throws RemoteException {
            synchronized (MMTelFeature.this.mLock) {
                MMTelFeature.this.addRegistrationListener(iImsRegistrationListener);
            }
        }

        @Override
        public void removeRegistrationListener(IImsRegistrationListener iImsRegistrationListener) throws RemoteException {
            synchronized (MMTelFeature.this.mLock) {
                MMTelFeature.this.removeRegistrationListener(iImsRegistrationListener);
            }
        }

        @Override
        public ImsCallProfile createCallProfile(int i, int i2, int i3) throws RemoteException {
            ImsCallProfile imsCallProfileCreateCallProfile;
            synchronized (MMTelFeature.this.mLock) {
                imsCallProfileCreateCallProfile = MMTelFeature.this.createCallProfile(i, i2, i3);
            }
            return imsCallProfileCreateCallProfile;
        }

        @Override
        public IImsCallSession createCallSession(int i, ImsCallProfile imsCallProfile) throws RemoteException {
            IImsCallSession iImsCallSessionCreateCallSession;
            synchronized (MMTelFeature.this.mLock) {
                iImsCallSessionCreateCallSession = MMTelFeature.this.createCallSession(i, imsCallProfile, null);
            }
            return iImsCallSessionCreateCallSession;
        }

        @Override
        public IImsCallSession getPendingCallSession(int i, String str) throws RemoteException {
            IImsCallSession pendingCallSession;
            synchronized (MMTelFeature.this.mLock) {
                pendingCallSession = MMTelFeature.this.getPendingCallSession(i, str);
            }
            return pendingCallSession;
        }

        @Override
        public IImsUt getUtInterface() throws RemoteException {
            IImsUt iImsUt;
            synchronized (MMTelFeature.this.mLock) {
                ImsUtImplBase utInterface = MMTelFeature.this.getUtInterface();
                iImsUt = utInterface != null ? utInterface.getInterface() : null;
            }
            return iImsUt;
        }

        @Override
        public IImsConfig getConfigInterface() throws RemoteException {
            IImsConfig configInterface;
            synchronized (MMTelFeature.this.mLock) {
                configInterface = MMTelFeature.this.getConfigInterface();
            }
            return configInterface;
        }

        @Override
        public void turnOnIms() throws RemoteException {
            synchronized (MMTelFeature.this.mLock) {
                MMTelFeature.this.turnOnIms();
            }
        }

        @Override
        public void turnOffIms() throws RemoteException {
            synchronized (MMTelFeature.this.mLock) {
                MMTelFeature.this.turnOffIms();
            }
        }

        @Override
        public IImsEcbm getEcbmInterface() throws RemoteException {
            IImsEcbm imsEcbm;
            synchronized (MMTelFeature.this.mLock) {
                ImsEcbmImplBase ecbmInterface = MMTelFeature.this.getEcbmInterface();
                imsEcbm = ecbmInterface != null ? ecbmInterface.getImsEcbm() : null;
            }
            return imsEcbm;
        }

        @Override
        public void setUiTTYMode(int i, Message message) throws RemoteException {
            synchronized (MMTelFeature.this.mLock) {
                MMTelFeature.this.setUiTTYMode(i, message);
            }
        }

        @Override
        public IImsMultiEndpoint getMultiEndpointInterface() throws RemoteException {
            IImsMultiEndpoint iImsMultiEndpoint;
            synchronized (MMTelFeature.this.mLock) {
                ImsMultiEndpointImplBase multiEndpointInterface = MMTelFeature.this.getMultiEndpointInterface();
                iImsMultiEndpoint = multiEndpointInterface != null ? multiEndpointInterface.getIImsMultiEndpoint() : null;
            }
            return iImsMultiEndpoint;
        }
    };

    @Override
    public final IImsMMTelFeature getBinder() {
        return this.mImsMMTelBinder;
    }

    public int startSession(PendingIntent pendingIntent, IImsRegistrationListener iImsRegistrationListener) {
        return 0;
    }

    public void endSession(int i) {
    }

    public boolean isConnected(int i, int i2) {
        return false;
    }

    public boolean isOpened() {
        return false;
    }

    public void addRegistrationListener(IImsRegistrationListener iImsRegistrationListener) {
    }

    public void removeRegistrationListener(IImsRegistrationListener iImsRegistrationListener) {
    }

    public ImsCallProfile createCallProfile(int i, int i2, int i3) {
        return null;
    }

    public IImsCallSession createCallSession(int i, ImsCallProfile imsCallProfile, IImsCallSessionListener iImsCallSessionListener) {
        return null;
    }

    public IImsCallSession getPendingCallSession(int i, String str) {
        return null;
    }

    public ImsUtImplBase getUtInterface() {
        return null;
    }

    public IImsConfig getConfigInterface() {
        return null;
    }

    public void turnOnIms() {
    }

    public void turnOffIms() {
    }

    public ImsEcbmImplBase getEcbmInterface() {
        return null;
    }

    public void setUiTTYMode(int i, Message message) {
    }

    public ImsMultiEndpointImplBase getMultiEndpointInterface() {
        return null;
    }

    @Override
    public void onFeatureReady() {
    }

    @Override
    public void onFeatureRemoved() {
    }
}
