package com.mediatek.ims;

import android.os.Message;
import android.os.RemoteException;
import android.telephony.ims.ImsReasonInfo;
import com.android.ims.ImsException;
import com.android.ims.ImsUt;
import com.android.ims.internal.IImsUt;
import com.mediatek.ims.internal.IMtkImsUt;
import com.mediatek.ims.internal.IMtkImsUtListener;
import java.util.Arrays;

public class MtkImsUt extends ImsUt {
    private static final String TAG = "MtkImsUt";
    private final IMtkImsUt miMtkUt;

    public MtkImsUt(IImsUt iImsUt, IMtkImsUt iMtkImsUt) {
        super(iImsUt);
        this.miMtkUt = iMtkImsUt;
        if (this.miMtkUt != null) {
            try {
                this.miMtkUt.setListener(new IMtkImsUtListenerProxy());
            } catch (RemoteException e) {
            }
        }
    }

    public void close() {
        super.close();
    }

    private class IMtkImsUtListenerProxy extends IMtkImsUtListener.Stub {
        private IMtkImsUtListenerProxy() {
        }

        public void utConfigurationCallForwardInTimeSlotQueried(IMtkImsUt iMtkImsUt, int i, MtkImsCallForwardInfo[] mtkImsCallForwardInfoArr) {
            Integer numValueOf = Integer.valueOf(i);
            synchronized (MtkImsUt.this.mLockObj) {
                MtkImsUt.this.sendSuccessReport((Message) MtkImsUt.this.mPendingCmds.get(numValueOf), mtkImsCallForwardInfoArr);
                MtkImsUt.this.mPendingCmds.remove(numValueOf);
            }
        }
    }

    public String getUtIMPUFromNetwork() throws ImsException {
        String utIMPUFromNetwork;
        log("getUtIMPUFromNetwork :: Ut = " + this.miMtkUt);
        synchronized (this.mLockObj) {
            try {
                try {
                    utIMPUFromNetwork = this.miMtkUt.getUtIMPUFromNetwork();
                } catch (RemoteException e) {
                    throw new ImsException("getUtIMPUFromNetwork()", e, 802);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return utIMPUFromNetwork;
    }

    public void setupXcapUserAgentString(String str) throws ImsException {
        log("setupXcapUserAgentString :: Ut = " + this.miMtkUt);
        synchronized (this.mLockObj) {
            try {
                try {
                    this.miMtkUt.setupXcapUserAgentString(str);
                } catch (RemoteException e) {
                    throw new ImsException("setupXcapUserAgentString()", e, 802);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public void queryCallForwardInTimeSlot(int i, Message message) {
        int iQueryCallForwardInTimeSlot;
        log("queryCallForwardInTimeSlot :: Ut = " + this.miMtkUt + ", condition = " + i);
        synchronized (this.mLockObj) {
            try {
                iQueryCallForwardInTimeSlot = this.miMtkUt.queryCallForwardInTimeSlot(i);
            } catch (RemoteException e) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            }
            if (iQueryCallForwardInTimeSlot < 0) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(iQueryCallForwardInTimeSlot), message);
            }
        }
    }

    public void updateCallForwardInTimeSlot(int i, int i2, String str, int i3, long[] jArr, Message message) {
        int iUpdateCallForwardInTimeSlot;
        log("updateCallForwardInTimeSlot :: Ut = " + this.miMtkUt + ", action = " + i + ", condition = " + i2 + ", number = " + str + ", timeSeconds = " + i3 + ", timeSlot = " + Arrays.toString(jArr));
        synchronized (this.mLockObj) {
            try {
                iUpdateCallForwardInTimeSlot = this.miMtkUt.updateCallForwardInTimeSlot(i, i2, str, i3, jArr);
            } catch (RemoteException e) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            }
            if (iUpdateCallForwardInTimeSlot < 0) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(iUpdateCallForwardInTimeSlot), message);
            }
        }
    }

    public void updateCallBarring(String str, int i, int i2, Message message, String[] strArr, int i3) {
        int iUpdateCallBarringForServiceClass;
        if (strArr != null) {
            String str2 = new String();
            for (String str3 : strArr) {
                str2.concat(str3 + " ");
            }
            log("updateCallBarring :: Ut=" + this.miMtkUt + ", cbType=" + i + ", action=" + i2 + ", serviceClass=" + i3 + ", barrList=" + str2);
        } else {
            log("updateCallBarring :: Ut=" + this.miMtkUt + ", cbType=" + i + ", action=" + i2 + ", serviceClass=" + i3);
        }
        synchronized (this.mLockObj) {
            try {
                iUpdateCallBarringForServiceClass = this.miMtkUt.updateCallBarringForServiceClass(str, i, i2, strArr, i3);
            } catch (RemoteException e) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            }
            if (iUpdateCallBarringForServiceClass < 0) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(iUpdateCallBarringForServiceClass), message);
            }
        }
    }

    public String getXcapConflictErrorMessage() throws ImsException {
        String xcapConflictErrorMessage;
        log("getXcapConflictErrorMessage :: Ut = " + this.miMtkUt);
        synchronized (this.mLockObj) {
            try {
                try {
                    xcapConflictErrorMessage = this.miMtkUt.getXcapConflictErrorMessage();
                } catch (RemoteException e) {
                    throw new ImsException("getXcapConflictErrorMessage()", e, 802);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return xcapConflictErrorMessage;
    }
}
