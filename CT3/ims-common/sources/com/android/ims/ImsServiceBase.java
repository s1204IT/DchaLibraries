package com.android.ims;

import android.app.PendingIntent;
import android.os.Message;
import com.android.ims.internal.IImsCallSession;
import com.android.ims.internal.IImsCallSessionListener;
import com.android.ims.internal.IImsConfig;
import com.android.ims.internal.IImsEcbm;
import com.android.ims.internal.IImsMultiEndpoint;
import com.android.ims.internal.IImsRegistrationListener;
import com.android.ims.internal.IImsService;
import com.android.ims.internal.IImsUt;

public abstract class ImsServiceBase {
    private ImsServiceBinder mBinder;

    private final class ImsServiceBinder extends IImsService.Stub {
        ImsServiceBinder(ImsServiceBase this$0, ImsServiceBinder imsServiceBinder) {
            this();
        }

        private ImsServiceBinder() {
        }

        public int open(int phoneId, int serviceClass, PendingIntent incomingCallIntent, IImsRegistrationListener listener) {
            return ImsServiceBase.this.onOpen(phoneId, serviceClass, incomingCallIntent, listener);
        }

        public void close(int serviceId) {
            ImsServiceBase.this.onClose(serviceId);
        }

        public boolean isConnected(int serviceId, int serviceType, int callType) {
            return ImsServiceBase.this.onIsConnected(serviceId, serviceType, callType);
        }

        public boolean isOpened(int serviceId) {
            return ImsServiceBase.this.onIsOpened(serviceId);
        }

        public void setRegistrationListener(int serviceId, IImsRegistrationListener listener) {
            ImsServiceBase.this.onSetRegistrationListener(serviceId, listener);
        }

        public ImsCallProfile createCallProfile(int serviceId, int serviceType, int callType) {
            return ImsServiceBase.this.onCreateCallProfile(serviceId, serviceType, callType);
        }

        public IImsCallSession createCallSession(int serviceId, ImsCallProfile profile, IImsCallSessionListener listener) {
            return ImsServiceBase.this.onCreateCallSession(serviceId, profile, listener);
        }

        public IImsCallSession getPendingCallSession(int serviceId, String callId) {
            return ImsServiceBase.this.onGetPendingCallSession(serviceId, callId);
        }

        public IImsUt getUtInterface(int serviceId) {
            return ImsServiceBase.this.onGetUtInterface(serviceId);
        }

        public IImsConfig getConfigInterface(int phoneId) {
            return ImsServiceBase.this.onGetConfigInterface(phoneId);
        }

        public void turnOnIms(int phoneId) {
            ImsServiceBase.this.onTurnOnIms(phoneId);
        }

        public void turnOffIms(int phoneId) {
            ImsServiceBase.this.onTurnOffIms(phoneId);
        }

        public IImsEcbm getEcbmInterface(int serviceId) {
            return ImsServiceBase.this.onGetEcbmInterface(serviceId);
        }

        public void setUiTTYMode(int serviceId, int uiTtyMode, Message onComplete) {
            ImsServiceBase.this.onSetUiTTYMode(serviceId, uiTtyMode, onComplete);
        }

        public IImsMultiEndpoint getMultiEndpointInterface(int serviceId) {
            return ImsServiceBase.this.onGetMultiEndpointInterface(serviceId);
        }

        public void setCallIndication(int serviceId, String callId, String callNum, int seqNum, boolean isAllow) {
            ImsServiceBase.this.onSetCallIndication(serviceId, callId, callNum, seqNum, isAllow);
        }

        public int getImsState(int phoneId) {
            return ImsServiceBase.this.onGetImsState(phoneId);
        }

        public boolean getImsRegInfo(int phoneId) {
            return ImsServiceBase.this.onGetImsRegInfo(phoneId);
        }

        public String getImsExtInfo(int phoneId) {
            return ImsServiceBase.this.onGetImsExtInfo(phoneId);
        }

        public void hangupAllCall() {
            ImsServiceBase.this.onHangupAllCall();
        }

        public int getRegistrationStatus() {
            return ImsServiceBase.this.onGetRegistrationStatus();
        }

        public void deregisterIms(int phoneId) {
            ImsServiceBase.this.onDeregisterIms(phoneId);
        }

        public void updateRadioState(int radioState, int phoneId) {
            ImsServiceBase.this.onUpdateRadioState(radioState, phoneId);
        }
    }

    public ImsServiceBinder getBinder() {
        ImsServiceBinder imsServiceBinder = null;
        if (this.mBinder == null) {
            this.mBinder = new ImsServiceBinder(this, imsServiceBinder);
        }
        return this.mBinder;
    }

    protected int onOpen(int phoneId, int serviceClass, PendingIntent incomingCallIntent, IImsRegistrationListener listener) {
        return 0;
    }

    protected void onClose(int serviceId) {
    }

    protected boolean onIsConnected(int serviceId, int serviceType, int callType) {
        return false;
    }

    protected boolean onIsOpened(int serviceId) {
        return false;
    }

    protected void onSetRegistrationListener(int serviceId, IImsRegistrationListener listener) {
    }

    protected ImsCallProfile onCreateCallProfile(int serviceId, int serviceType, int callType) {
        return null;
    }

    protected IImsCallSession onCreateCallSession(int serviceId, ImsCallProfile profile, IImsCallSessionListener listener) {
        return null;
    }

    protected IImsCallSession onGetPendingCallSession(int serviceId, String callId) {
        return null;
    }

    protected IImsUt onGetUtInterface(int serviceId) {
        return null;
    }

    protected IImsConfig onGetConfigInterface(int phoneId) {
        return null;
    }

    protected void onTurnOnIms(int phoneId) {
    }

    protected void onTurnOffIms(int phoneId) {
    }

    protected IImsEcbm onGetEcbmInterface(int serviceId) {
        return null;
    }

    protected void onSetUiTTYMode(int serviceId, int uiTtyMode, Message onComplete) {
    }

    protected IImsMultiEndpoint onGetMultiEndpointInterface(int serviceId) {
        return null;
    }

    protected void onSetCallIndication(int serviceId, String callId, String callNum, int seqNum, boolean isAllow) {
    }

    protected int onGetImsState(int phoneId) {
        return 0;
    }

    protected boolean onGetImsRegInfo(int phoneId) {
        return false;
    }

    protected String onGetImsExtInfo(int phoneId) {
        return "";
    }

    protected void onHangupAllCall() {
    }

    protected int onGetRegistrationStatus() {
        return 100;
    }

    protected void onDeregisterIms(int phoneId) {
    }

    protected void onUpdateRadioState(int radioState, int phoneId) {
    }
}
