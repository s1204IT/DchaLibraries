package com.android.ims;

import android.R;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RemoteException;
import android.telephony.Rlog;
import android.telephony.ims.ImsCallForwardInfo;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsSsData;
import android.telephony.ims.ImsSsInfo;
import com.android.ims.internal.IImsUt;
import com.android.ims.internal.IImsUtListener;
import java.util.HashMap;
import java.util.Map;

public class ImsUt implements ImsUtInterface {
    public static final String CATEGORY_CB = "CB";
    public static final String CATEGORY_CDIV = "CDIV";
    public static final String CATEGORY_CONF = "CONF";
    public static final String CATEGORY_CW = "CW";
    public static final String CATEGORY_OIP = "OIP";
    public static final String CATEGORY_OIR = "OIR";
    public static final String CATEGORY_TIP = "TIP";
    public static final String CATEGORY_TIR = "TIR";
    protected static final boolean DBG = true;
    public static final String KEY_ACTION = "action";
    public static final String KEY_CATEGORY = "category";
    private static final int SERVICE_CLASS_NONE = 0;
    private static final int SERVICE_CLASS_VOICE = 1;
    private static final String TAG = "ImsUt";
    protected Object mLockObj = new Object();
    protected HashMap<Integer, Message> mPendingCmds = new HashMap<>();
    private Registrant mSsIndicationRegistrant;
    private final IImsUt miUt;

    public ImsUt(IImsUt iImsUt) {
        this.miUt = iImsUt;
        if (this.miUt != null) {
            try {
                this.miUt.setListener(new IImsUtListenerProxy());
            } catch (RemoteException e) {
            }
        }
    }

    public void close() {
        synchronized (this.mLockObj) {
            if (this.miUt != null) {
                try {
                    this.miUt.close();
                } catch (RemoteException e) {
                }
            }
            if (!this.mPendingCmds.isEmpty()) {
                for (Map.Entry entry : (Map.Entry[]) this.mPendingCmds.entrySet().toArray(new Map.Entry[this.mPendingCmds.size()])) {
                    sendFailureReport((Message) entry.getValue(), new ImsReasonInfo(802, 0));
                }
                this.mPendingCmds.clear();
            }
        }
    }

    public void registerForSuppServiceIndication(Handler handler, int i, Object obj) {
        this.mSsIndicationRegistrant = new Registrant(handler, i, obj);
    }

    public void unregisterForSuppServiceIndication(Handler handler) {
        this.mSsIndicationRegistrant.clear();
    }

    public void queryCallBarring(int i, Message message) {
        queryCallBarring(i, message, 0);
    }

    public void queryCallBarring(int i, Message message, int i2) {
        int iQueryCallBarringForServiceClass;
        log("queryCallBarring :: Ut=" + this.miUt + ", cbType=" + i + ", serviceClass=" + i2);
        synchronized (this.mLockObj) {
            try {
                iQueryCallBarringForServiceClass = this.miUt.queryCallBarringForServiceClass(i, i2);
            } catch (RemoteException e) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            }
            if (iQueryCallBarringForServiceClass < 0) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(iQueryCallBarringForServiceClass), message);
            }
        }
    }

    public void queryCallForward(int i, String str, Message message) {
        int iQueryCallForward;
        log("queryCallForward :: Ut=" + this.miUt + ", condition=" + i + ", number=" + Rlog.pii(TAG, str));
        synchronized (this.mLockObj) {
            try {
                iQueryCallForward = this.miUt.queryCallForward(i, str);
            } catch (RemoteException e) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            }
            if (iQueryCallForward < 0) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(iQueryCallForward), message);
            }
        }
    }

    public void queryCallWaiting(Message message) {
        int iQueryCallWaiting;
        log("queryCallWaiting :: Ut=" + this.miUt);
        synchronized (this.mLockObj) {
            try {
                iQueryCallWaiting = this.miUt.queryCallWaiting();
            } catch (RemoteException e) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            }
            if (iQueryCallWaiting < 0) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(iQueryCallWaiting), message);
            }
        }
    }

    public void queryCLIR(Message message) {
        int iQueryCLIR;
        log("queryCLIR :: Ut=" + this.miUt);
        synchronized (this.mLockObj) {
            try {
                iQueryCLIR = this.miUt.queryCLIR();
            } catch (RemoteException e) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            }
            if (iQueryCLIR < 0) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(iQueryCLIR), message);
            }
        }
    }

    public void queryCLIP(Message message) {
        int iQueryCLIP;
        log("queryCLIP :: Ut=" + this.miUt);
        synchronized (this.mLockObj) {
            try {
                iQueryCLIP = this.miUt.queryCLIP();
            } catch (RemoteException e) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            }
            if (iQueryCLIP < 0) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(iQueryCLIP), message);
            }
        }
    }

    public void queryCOLR(Message message) {
        int iQueryCOLR;
        log("queryCOLR :: Ut=" + this.miUt);
        synchronized (this.mLockObj) {
            try {
                iQueryCOLR = this.miUt.queryCOLR();
            } catch (RemoteException e) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            }
            if (iQueryCOLR < 0) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(iQueryCOLR), message);
            }
        }
    }

    public void queryCOLP(Message message) {
        int iQueryCOLP;
        log("queryCOLP :: Ut=" + this.miUt);
        synchronized (this.mLockObj) {
            try {
                iQueryCOLP = this.miUt.queryCOLP();
            } catch (RemoteException e) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            }
            if (iQueryCOLP < 0) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(iQueryCOLP), message);
            }
        }
    }

    public void updateCallBarring(int i, int i2, Message message, String[] strArr) {
        updateCallBarring(i, i2, message, strArr, 0);
    }

    public void updateCallBarring(int i, int i2, Message message, String[] strArr, int i3) {
        int iUpdateCallBarringForServiceClass;
        if (strArr != null) {
            String str = new String();
            for (String str2 : strArr) {
                str.concat(str2 + " ");
            }
            log("updateCallBarring :: Ut=" + this.miUt + ", cbType=" + i + ", action=" + i2 + ", serviceClass=" + i3 + ", barrList=" + str);
        } else {
            log("updateCallBarring :: Ut=" + this.miUt + ", cbType=" + i + ", action=" + i2 + ", serviceClass=" + i3);
        }
        synchronized (this.mLockObj) {
            try {
                iUpdateCallBarringForServiceClass = this.miUt.updateCallBarringForServiceClass(i, i2, strArr, i3);
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

    public void updateCallForward(int i, int i2, String str, int i3, int i4, Message message) {
        int iUpdateCallForward;
        log("updateCallForward :: Ut=" + this.miUt + ", action=" + i + ", condition=" + i2 + ", number=" + Rlog.pii(TAG, str) + ", serviceClass=" + i3 + ", timeSeconds=" + i4);
        synchronized (this.mLockObj) {
            try {
                iUpdateCallForward = this.miUt.updateCallForward(i, i2, str, i3, i4);
            } catch (RemoteException e) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            }
            if (iUpdateCallForward < 0) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(iUpdateCallForward), message);
            }
        }
    }

    public void updateCallWaiting(boolean z, int i, Message message) {
        int iUpdateCallWaiting;
        log("updateCallWaiting :: Ut=" + this.miUt + ", enable=" + z + ",serviceClass=" + i);
        synchronized (this.mLockObj) {
            try {
                iUpdateCallWaiting = this.miUt.updateCallWaiting(z, i);
            } catch (RemoteException e) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            }
            if (iUpdateCallWaiting < 0) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(iUpdateCallWaiting), message);
            }
        }
    }

    public void updateCLIR(int i, Message message) {
        int iUpdateCLIR;
        log("updateCLIR :: Ut=" + this.miUt + ", clirMode=" + i);
        synchronized (this.mLockObj) {
            try {
                iUpdateCLIR = this.miUt.updateCLIR(i);
            } catch (RemoteException e) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            }
            if (iUpdateCLIR < 0) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(iUpdateCLIR), message);
            }
        }
    }

    public void updateCLIP(boolean z, Message message) {
        int iUpdateCLIP;
        log("updateCLIP :: Ut=" + this.miUt + ", enable=" + z);
        synchronized (this.mLockObj) {
            try {
                iUpdateCLIP = this.miUt.updateCLIP(z);
            } catch (RemoteException e) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            }
            if (iUpdateCLIP < 0) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(iUpdateCLIP), message);
            }
        }
    }

    public void updateCOLR(int i, Message message) {
        int iUpdateCOLR;
        log("updateCOLR :: Ut=" + this.miUt + ", presentation=" + i);
        synchronized (this.mLockObj) {
            try {
                iUpdateCOLR = this.miUt.updateCOLR(i);
            } catch (RemoteException e) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            }
            if (iUpdateCOLR < 0) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(iUpdateCOLR), message);
            }
        }
    }

    public void updateCOLP(boolean z, Message message) {
        int iUpdateCOLP;
        log("updateCallWaiting :: Ut=" + this.miUt + ", enable=" + z);
        synchronized (this.mLockObj) {
            try {
                iUpdateCOLP = this.miUt.updateCOLP(z);
            } catch (RemoteException e) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            }
            if (iUpdateCOLP < 0) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(iUpdateCOLP), message);
            }
        }
    }

    public boolean isBinderAlive() {
        return this.miUt.asBinder().isBinderAlive();
    }

    public void transact(Bundle bundle, Message message) {
        int iTransact;
        log("transact :: Ut=" + this.miUt + ", ssInfo=" + bundle);
        synchronized (this.mLockObj) {
            try {
                iTransact = this.miUt.transact(bundle);
            } catch (RemoteException e) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            }
            if (iTransact < 0) {
                sendFailureReport(message, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(iTransact), message);
            }
        }
    }

    protected void sendFailureReport(Message message, ImsReasonInfo imsReasonInfo) {
        String str;
        if (message == null || imsReasonInfo == null) {
            return;
        }
        if (imsReasonInfo.mExtraMessage == null) {
            str = Resources.getSystem().getString(R.string.ext_media_missing_message);
        } else {
            str = new String(imsReasonInfo.mExtraMessage);
        }
        AsyncResult.forMessage(message, (Object) null, new ImsException(str, imsReasonInfo.mCode));
        message.sendToTarget();
    }

    protected void sendSuccessReport(Message message) {
        if (message == null) {
            return;
        }
        AsyncResult.forMessage(message, (Object) null, (Throwable) null);
        message.sendToTarget();
    }

    protected void sendSuccessReport(Message message, Object obj) {
        if (message == null) {
            return;
        }
        AsyncResult.forMessage(message, obj, (Throwable) null);
        message.sendToTarget();
    }

    protected void log(String str) {
        Rlog.d(TAG, str);
    }

    protected void loge(String str) {
        Rlog.e(TAG, str);
    }

    protected void loge(String str, Throwable th) {
        Rlog.e(TAG, str, th);
    }

    private class IImsUtListenerProxy extends IImsUtListener.Stub {
        private IImsUtListenerProxy() {
        }

        public void utConfigurationUpdated(IImsUt iImsUt, int i) {
            Integer numValueOf = Integer.valueOf(i);
            synchronized (ImsUt.this.mLockObj) {
                ImsUt.this.sendSuccessReport(ImsUt.this.mPendingCmds.get(numValueOf));
                ImsUt.this.mPendingCmds.remove(numValueOf);
            }
        }

        public void utConfigurationUpdateFailed(IImsUt iImsUt, int i, ImsReasonInfo imsReasonInfo) {
            Integer numValueOf = Integer.valueOf(i);
            synchronized (ImsUt.this.mLockObj) {
                ImsUt.this.sendFailureReport(ImsUt.this.mPendingCmds.get(numValueOf), imsReasonInfo);
                ImsUt.this.mPendingCmds.remove(numValueOf);
            }
        }

        public void utConfigurationQueried(IImsUt iImsUt, int i, Bundle bundle) {
            Integer numValueOf = Integer.valueOf(i);
            synchronized (ImsUt.this.mLockObj) {
                ImsUt.this.sendSuccessReport(ImsUt.this.mPendingCmds.get(numValueOf), bundle);
                ImsUt.this.mPendingCmds.remove(numValueOf);
            }
        }

        public void utConfigurationQueryFailed(IImsUt iImsUt, int i, ImsReasonInfo imsReasonInfo) {
            Integer numValueOf = Integer.valueOf(i);
            synchronized (ImsUt.this.mLockObj) {
                ImsUt.this.sendFailureReport(ImsUt.this.mPendingCmds.get(numValueOf), imsReasonInfo);
                ImsUt.this.mPendingCmds.remove(numValueOf);
            }
        }

        public void utConfigurationCallBarringQueried(IImsUt iImsUt, int i, ImsSsInfo[] imsSsInfoArr) {
            Integer numValueOf = Integer.valueOf(i);
            synchronized (ImsUt.this.mLockObj) {
                ImsUt.this.sendSuccessReport(ImsUt.this.mPendingCmds.get(numValueOf), imsSsInfoArr);
                ImsUt.this.mPendingCmds.remove(numValueOf);
            }
        }

        public void utConfigurationCallForwardQueried(IImsUt iImsUt, int i, ImsCallForwardInfo[] imsCallForwardInfoArr) {
            Integer numValueOf = Integer.valueOf(i);
            synchronized (ImsUt.this.mLockObj) {
                ImsUt.this.sendSuccessReport(ImsUt.this.mPendingCmds.get(numValueOf), imsCallForwardInfoArr);
                ImsUt.this.mPendingCmds.remove(numValueOf);
            }
        }

        public void utConfigurationCallWaitingQueried(IImsUt iImsUt, int i, ImsSsInfo[] imsSsInfoArr) {
            Integer numValueOf = Integer.valueOf(i);
            synchronized (ImsUt.this.mLockObj) {
                ImsUt.this.sendSuccessReport(ImsUt.this.mPendingCmds.get(numValueOf), imsSsInfoArr);
                ImsUt.this.mPendingCmds.remove(numValueOf);
            }
        }

        public void onSupplementaryServiceIndication(ImsSsData imsSsData) {
            if (ImsUt.this.mSsIndicationRegistrant != null) {
                ImsUt.this.mSsIndicationRegistrant.notifyResult(imsSsData);
            }
        }
    }
}
