package com.android.ims;

import android.os.RemoteException;
import android.telephony.Rlog;
import com.android.ims.internal.IImsEcbm;
import com.android.ims.internal.IImsEcbmListener;

public class ImsEcbm {
    private static final boolean DBG = true;
    private static final String TAG = "ImsEcbm";
    private final IImsEcbm miEcbm;

    public ImsEcbm(IImsEcbm iImsEcbm) {
        Rlog.d(TAG, "ImsEcbm created");
        this.miEcbm = iImsEcbm;
    }

    public void setEcbmStateListener(ImsEcbmStateListener imsEcbmStateListener) throws ImsException {
        try {
            this.miEcbm.setListener(new ImsEcbmListenerProxy(imsEcbmStateListener));
        } catch (RemoteException e) {
            throw new ImsException("setEcbmStateListener()", e, 106);
        }
    }

    public void exitEmergencyCallbackMode() throws ImsException {
        try {
            this.miEcbm.exitEmergencyCallbackMode();
        } catch (RemoteException e) {
            throw new ImsException("exitEmergencyCallbackMode()", e, 106);
        }
    }

    public boolean isBinderAlive() {
        return this.miEcbm.asBinder().isBinderAlive();
    }

    private class ImsEcbmListenerProxy extends IImsEcbmListener.Stub {
        private ImsEcbmStateListener mListener;

        public ImsEcbmListenerProxy(ImsEcbmStateListener imsEcbmStateListener) {
            this.mListener = imsEcbmStateListener;
        }

        public void enteredECBM() {
            Rlog.d(ImsEcbm.TAG, "enteredECBM ::");
            if (this.mListener != null) {
                this.mListener.onECBMEntered();
            }
        }

        public void exitedECBM() {
            Rlog.d(ImsEcbm.TAG, "exitedECBM ::");
            if (this.mListener != null) {
                this.mListener.onECBMExited();
            }
        }
    }
}
