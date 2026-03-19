package com.android.ims;

import android.app.PendingIntent;
import android.os.Message;
import android.telephony.ims.ImsCallProfile;
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
        private ImsServiceBinder() {
        }

        public int open(int i, int i2, PendingIntent pendingIntent, IImsRegistrationListener iImsRegistrationListener) {
            return ImsServiceBase.this.onOpen(i, i2, pendingIntent, iImsRegistrationListener);
        }

        public void close(int i) {
            ImsServiceBase.this.onClose(i);
        }

        public boolean isConnected(int i, int i2, int i3) {
            return ImsServiceBase.this.onIsConnected(i, i2, i3);
        }

        public boolean isOpened(int i) {
            return ImsServiceBase.this.onIsOpened(i);
        }

        public void setRegistrationListener(int i, IImsRegistrationListener iImsRegistrationListener) {
            ImsServiceBase.this.onSetRegistrationListener(i, iImsRegistrationListener);
        }

        public void addRegistrationListener(int i, int i2, IImsRegistrationListener iImsRegistrationListener) {
            ImsServiceBase.this.onAddRegistrationListener(i, i2, iImsRegistrationListener);
        }

        public ImsCallProfile createCallProfile(int i, int i2, int i3) {
            return ImsServiceBase.this.onCreateCallProfile(i, i2, i3);
        }

        public IImsCallSession createCallSession(int i, ImsCallProfile imsCallProfile, IImsCallSessionListener iImsCallSessionListener) {
            return ImsServiceBase.this.onCreateCallSession(i, imsCallProfile, iImsCallSessionListener);
        }

        public IImsCallSession getPendingCallSession(int i, String str) {
            return ImsServiceBase.this.onGetPendingCallSession(i, str);
        }

        public IImsUt getUtInterface(int i) {
            return ImsServiceBase.this.onGetUtInterface(i);
        }

        public IImsConfig getConfigInterface(int i) {
            return ImsServiceBase.this.onGetConfigInterface(i);
        }

        public void turnOnIms(int i) {
            ImsServiceBase.this.onTurnOnIms(i);
        }

        public void turnOffIms(int i) {
            ImsServiceBase.this.onTurnOffIms(i);
        }

        public IImsEcbm getEcbmInterface(int i) {
            return ImsServiceBase.this.onGetEcbmInterface(i);
        }

        public void setUiTTYMode(int i, int i2, Message message) {
            ImsServiceBase.this.onSetUiTTYMode(i, i2, message);
        }

        public IImsMultiEndpoint getMultiEndpointInterface(int i) {
            return ImsServiceBase.this.onGetMultiEndpointInterface(i);
        }
    }

    public ImsServiceBinder getBinder() {
        if (this.mBinder == null) {
            this.mBinder = new ImsServiceBinder();
        }
        return this.mBinder;
    }

    protected int onOpen(int i, int i2, PendingIntent pendingIntent, IImsRegistrationListener iImsRegistrationListener) {
        return 0;
    }

    protected void onClose(int i) {
    }

    protected boolean onIsConnected(int i, int i2, int i3) {
        return false;
    }

    protected boolean onIsOpened(int i) {
        return false;
    }

    protected void onSetRegistrationListener(int i, IImsRegistrationListener iImsRegistrationListener) {
    }

    protected void onAddRegistrationListener(int i, int i2, IImsRegistrationListener iImsRegistrationListener) {
    }

    protected ImsCallProfile onCreateCallProfile(int i, int i2, int i3) {
        return null;
    }

    protected IImsCallSession onCreateCallSession(int i, ImsCallProfile imsCallProfile, IImsCallSessionListener iImsCallSessionListener) {
        return null;
    }

    protected IImsCallSession onGetPendingCallSession(int i, String str) {
        return null;
    }

    protected IImsUt onGetUtInterface(int i) {
        return null;
    }

    protected IImsConfig onGetConfigInterface(int i) {
        return null;
    }

    protected void onTurnOnIms(int i) {
    }

    protected void onTurnOffIms(int i) {
    }

    protected IImsEcbm onGetEcbmInterface(int i) {
        return null;
    }

    protected void onSetUiTTYMode(int i, int i2, Message message) {
    }

    protected IImsMultiEndpoint onGetMultiEndpointInterface(int i) {
        return null;
    }
}
