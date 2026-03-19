package com.mediatek.ims.internal;

import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsCallSession;
import android.telephony.ims.ImsReasonInfo;
import android.util.Log;
import com.android.ims.internal.IImsCallSession;
import com.mediatek.ims.internal.IMtkImsCallSessionListener;
import com.mediatek.ims.internal.op.OpImsCallSessionBase;
import com.mediatek.internal.telephony.MtkOpTelephonyCustomizationUtils;
import java.util.Objects;

public class MtkImsCallSession extends ImsCallSession {
    private static final String TAG = "MtkImsCallSession";
    private OpImsCallSessionBase mOpExt;
    private final IMtkImsCallSession miMtkSession;

    public MtkImsCallSession(IImsCallSession iImsCallSession, IMtkImsCallSession iMtkImsCallSession) {
        this.miMtkSession = iMtkImsCallSession;
        this.miSession = iImsCallSession;
        if (iMtkImsCallSession == null || iImsCallSession == null) {
            this.mClosed = true;
            return;
        }
        try {
            this.miMtkSession.setListener(new IMtkImsCallSessionListenerProxy());
        } catch (RemoteException e) {
        }
        try {
            this.miSession.setListener(new ImsCallSession.IImsCallSessionListenerProxy(this));
        } catch (RemoteException e2) {
        }
        this.mOpExt = MtkOpTelephonyCustomizationUtils.getOpCommonInstance().makeOpImsCallSession();
    }

    public synchronized void close() {
        if (this.mClosed) {
            return;
        }
        try {
            this.miMtkSession.close();
            this.mClosed = true;
        } catch (RemoteException e) {
        }
    }

    public void sendDtmf(char c, Message message) {
        if (this.mClosed) {
            return;
        }
        if (message != null) {
            try {
                if (message.getTarget() != null) {
                    this.miMtkSession.sendDtmfbyTarget(c, message, new Messenger(message.getTarget()));
                } else {
                    this.miSession.sendDtmf(c, message);
                }
            } catch (RemoteException e) {
            }
        }
    }

    public boolean isIncomingCallMultiparty() {
        if (this.mClosed) {
            return false;
        }
        try {
            return this.miMtkSession.isIncomingCallMultiparty();
        } catch (RemoteException e) {
            return false;
        }
    }

    public void explicitCallTransfer() {
        if (this.mClosed) {
            return;
        }
        try {
            this.miMtkSession.explicitCallTransfer();
        } catch (RemoteException e) {
            Log.e(TAG, "explicitCallTransfer: RemoteException!");
        }
    }

    public void unattendedCallTransfer(String str, int i) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miMtkSession.unattendedCallTransfer(str, i);
        } catch (RemoteException e) {
            Log.e(TAG, "explicitCallTransfer: RemoteException!");
        }
    }

    public void deviceSwitch(String str, String str2) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miMtkSession.deviceSwitch(str, str2);
        } catch (RemoteException e) {
            Log.e(TAG, "deviceSwitch: RemoteException!");
        }
    }

    public void cancelDeviceSwitch() {
        if (this.mClosed) {
            return;
        }
        try {
            this.miMtkSession.cancelDeviceSwitch();
        } catch (RemoteException e) {
            Log.e(TAG, "cancelDeviceSwitch: RemoteException!");
        }
    }

    public class IMtkImsCallSessionListenerProxy extends IMtkImsCallSessionListener.Stub {
        public IMtkImsCallSessionListenerProxy() {
        }

        @Override
        public void callSessionTransferred(IMtkImsCallSession iMtkImsCallSession) {
            MtkImsCallSession.this.mListener.callSessionTransferred(MtkImsCallSession.this);
        }

        @Override
        public void callSessionTransferFailed(IMtkImsCallSession iMtkImsCallSession, ImsReasonInfo imsReasonInfo) {
            MtkImsCallSession.this.mListener.callSessionTransferFailed(MtkImsCallSession.this, imsReasonInfo);
        }

        @Override
        public void callSessionTextCapabilityChanged(IMtkImsCallSession iMtkImsCallSession, int i, int i2, int i3, int i4) {
            MtkImsCallSession.this.mOpExt.callSessionTextCapabilityChanged(MtkImsCallSession.this.mListener, MtkImsCallSession.this, i, i2, i3, i4);
        }

        @Override
        public void callSessionRttEventReceived(IMtkImsCallSession iMtkImsCallSession, int i) {
            MtkImsCallSession.this.mOpExt.callSessionRttEventReceived(MtkImsCallSession.this.mListener, MtkImsCallSession.this, i);
        }

        @Override
        public void callSessionDeviceSwitched(IMtkImsCallSession iMtkImsCallSession) {
            MtkImsCallSession.this.mListener.callSessionDeviceSwitched(MtkImsCallSession.this);
        }

        @Override
        public void callSessionDeviceSwitchFailed(IMtkImsCallSession iMtkImsCallSession, ImsReasonInfo imsReasonInfo) {
            MtkImsCallSession.this.mListener.callSessionDeviceSwitchFailed(MtkImsCallSession.this, imsReasonInfo);
        }

        @Override
        public void callSessionMergeStarted(IMtkImsCallSession iMtkImsCallSession, IMtkImsCallSession iMtkImsCallSession2, ImsCallProfile imsCallProfile) {
            Log.d(MtkImsCallSession.TAG, "callSessionMergeStarted");
        }

        @Override
        public void callSessionMergeComplete(IMtkImsCallSession iMtkImsCallSession) {
            if (MtkImsCallSession.this.mListener != null) {
                if (iMtkImsCallSession == null) {
                    MtkImsCallSession.this.mListener.callSessionMergeComplete((ImsCallSession) null);
                    return;
                }
                MtkImsCallSession mtkImsCallSession = MtkImsCallSession.this;
                try {
                    if (!Objects.equals(MtkImsCallSession.this.miSession.getCallId(), iMtkImsCallSession.getCallId())) {
                        mtkImsCallSession = new MtkImsCallSession(iMtkImsCallSession.getIImsCallSession(), iMtkImsCallSession);
                    }
                } catch (RemoteException e) {
                    Log.e(MtkImsCallSession.TAG, "callSessionMergeComplete: exception for getCallId!");
                }
                MtkImsCallSession.this.mListener.callSessionMergeComplete(mtkImsCallSession);
            }
        }
    }
}
