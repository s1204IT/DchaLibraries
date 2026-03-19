package com.android.ims;

import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.Rlog;
import com.android.ims.internal.IImsUt;
import com.android.ims.internal.IImsUtListener;
import java.util.Arrays;
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
    private static final boolean DBG = true;
    public static final String KEY_ACTION = "action";
    public static final String KEY_CATEGORY = "category";
    private static final String TAG = "ImsUt";
    private IImsUtListenerProxy mListener;
    private Object mLockObj = new Object();
    private HashMap<Integer, Message> mPendingCmds = new HashMap<>();
    private final IImsUt miUt;

    public ImsUt(IImsUt iUt) {
        this.mListener = null;
        this.miUt = iUt;
        this.mListener = new IImsUtListenerProxy(this, null);
        if (this.miUt == null) {
            return;
        }
        try {
            this.miUt.setListener(this.mListener);
        } catch (RemoteException e) {
        }
    }

    public void updateListener() {
        log("updateListener: miUt=" + this.miUt + ", mListener=" + this.mListener);
        if (this.miUt == null || this.mListener == null) {
            return;
        }
        try {
            this.miUt.setListener(this.mListener);
        } catch (RemoteException e) {
            log("updateListener failed, RemoteException");
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
                Map.Entry<Integer, Message>[] entries = (Map.Entry[]) this.mPendingCmds.entrySet().toArray(new Map.Entry[this.mPendingCmds.size()]);
                for (Map.Entry<Integer, Message> entry : entries) {
                    sendFailureReport(entry.getValue(), new ImsReasonInfo(802, 0));
                }
                this.mPendingCmds.clear();
            }
        }
    }

    @Override
    public void queryCallBarring(int cbType, Message result) {
        int id;
        log("queryCallBarring :: Ut=" + this.miUt + ", cbType=" + cbType);
        synchronized (this.mLockObj) {
            try {
                id = this.miUt.queryCallBarring(cbType);
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
            if (id < 0) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(id), result);
            }
        }
    }

    @Override
    public void queryCallForward(int condition, String number, Message result) {
        int id;
        log("queryCallForward :: Ut=" + this.miUt + ", condition=" + condition + ", number=" + number);
        synchronized (this.mLockObj) {
            try {
                id = this.miUt.queryCallForward(condition, number);
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
            if (id < 0) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(id), result);
            }
        }
    }

    @Override
    public void queryCallWaiting(Message result) {
        int id;
        log("queryCallWaiting :: Ut=" + this.miUt);
        synchronized (this.mLockObj) {
            try {
                id = this.miUt.queryCallWaiting();
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
            if (id < 0) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(id), result);
            }
        }
    }

    @Override
    public void queryCLIR(Message result) {
        int id;
        log("queryCLIR :: Ut=" + this.miUt);
        synchronized (this.mLockObj) {
            try {
                id = this.miUt.queryCLIR();
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
            if (id < 0) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(id), result);
            }
        }
    }

    @Override
    public void queryCLIP(Message result) {
        int id;
        log("queryCLIP :: Ut=" + this.miUt);
        synchronized (this.mLockObj) {
            try {
                id = this.miUt.queryCLIP();
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
            if (id < 0) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(id), result);
            }
        }
    }

    @Override
    public void queryCOLR(Message result) {
        int id;
        log("queryCOLR :: Ut=" + this.miUt);
        synchronized (this.mLockObj) {
            try {
                id = this.miUt.queryCOLR();
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
            if (id < 0) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(id), result);
            }
        }
    }

    @Override
    public void queryCOLP(Message result) {
        int id;
        log("queryCOLP :: Ut=" + this.miUt);
        synchronized (this.mLockObj) {
            try {
                id = this.miUt.queryCOLP();
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
            if (id < 0) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(id), result);
            }
        }
    }

    @Override
    public void updateCallBarring(int cbType, int action, Message result, String[] barrList) {
        int id;
        if (barrList != null) {
            String bList = new String();
            for (String str : barrList) {
                bList.concat(str + " ");
            }
            log("updateCallBarring :: Ut=" + this.miUt + ", cbType=" + cbType + ", action=" + action + ", barrList=" + bList);
        } else {
            log("updateCallBarring :: Ut=" + this.miUt + ", cbType=" + cbType + ", action=" + action);
        }
        synchronized (this.mLockObj) {
            try {
                id = this.miUt.updateCallBarring(cbType, action, barrList);
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
            if (id < 0) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(id), result);
            }
        }
    }

    @Override
    public void updateCallForward(int action, int condition, String number, int serviceClass, int timeSeconds, Message result) {
        int id;
        log("updateCallForward :: Ut=" + this.miUt + ", action=" + action + ", condition=" + condition + ", number=" + number + ", serviceClass=" + serviceClass + ", timeSeconds=" + timeSeconds);
        synchronized (this.mLockObj) {
            try {
                id = this.miUt.updateCallForward(action, condition, number, serviceClass, timeSeconds);
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
            if (id < 0) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(id), result);
            }
        }
    }

    @Override
    public void updateCallWaiting(boolean enable, int serviceClass, Message result) {
        int id;
        log("updateCallWaiting :: Ut=" + this.miUt + ", enable=" + enable + ",serviceClass=" + serviceClass);
        synchronized (this.mLockObj) {
            try {
                id = this.miUt.updateCallWaiting(enable, serviceClass);
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
            if (id < 0) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(id), result);
            }
        }
    }

    @Override
    public void updateCLIR(int clirMode, Message result) {
        int id;
        log("updateCLIR :: Ut=" + this.miUt + ", clirMode=" + clirMode);
        synchronized (this.mLockObj) {
            try {
                id = this.miUt.updateCLIR(clirMode);
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
            if (id < 0) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(id), result);
            }
        }
    }

    @Override
    public void updateCLIP(boolean enable, Message result) {
        int id;
        log("updateCLIP :: Ut=" + this.miUt + ", enable=" + enable);
        synchronized (this.mLockObj) {
            try {
                id = this.miUt.updateCLIP(enable);
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
            if (id < 0) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(id), result);
            }
        }
    }

    @Override
    public void updateCOLR(int presentation, Message result) {
        int id;
        log("updateCOLR :: Ut=" + this.miUt + ", presentation=" + presentation);
        synchronized (this.mLockObj) {
            try {
                id = this.miUt.updateCOLR(presentation);
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
            if (id < 0) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(id), result);
            }
        }
    }

    @Override
    public void updateCOLP(boolean enable, Message result) {
        int id;
        log("updateCallWaiting :: Ut=" + this.miUt + ", enable=" + enable);
        synchronized (this.mLockObj) {
            try {
                id = this.miUt.updateCOLP(enable);
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
            if (id < 0) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(id), result);
            }
        }
    }

    public void transact(Bundle ssInfo, Message result) {
        int id;
        log("transact :: Ut=" + this.miUt + ", ssInfo=" + ssInfo);
        synchronized (this.mLockObj) {
            try {
                id = this.miUt.transact(ssInfo);
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
            if (id < 0) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(id), result);
            }
        }
    }

    private void sendFailureReport(Message result, ImsReasonInfo error) {
        String errorString;
        if (result == null || error == null) {
            return;
        }
        if (error.mExtraMessage == null) {
            errorString = new String("IMS UT exception");
        } else {
            errorString = new String(error.mExtraMessage);
        }
        AsyncResult.forMessage(result, (Object) null, new ImsException(errorString, error.mCode));
        result.sendToTarget();
    }

    private void sendSuccessReport(Message result) {
        if (result == null) {
            return;
        }
        AsyncResult.forMessage(result, (Object) null, (Throwable) null);
        result.sendToTarget();
    }

    private void sendSuccessReport(Message result, Object ssInfo) {
        if (result == null) {
            return;
        }
        AsyncResult.forMessage(result, ssInfo, (Throwable) null);
        result.sendToTarget();
    }

    private void log(String s) {
        Rlog.d(TAG, s);
    }

    private void loge(String s) {
        Rlog.e(TAG, s);
    }

    private void loge(String s, Throwable t) {
        Rlog.e(TAG, s, t);
    }

    private class IImsUtListenerProxy extends IImsUtListener.Stub {
        IImsUtListenerProxy(ImsUt this$0, IImsUtListenerProxy iImsUtListenerProxy) {
            this();
        }

        private IImsUtListenerProxy() {
        }

        public void utConfigurationUpdated(IImsUt ut, int id) {
            Integer key = Integer.valueOf(id);
            synchronized (ImsUt.this.mLockObj) {
                ImsUt.this.sendSuccessReport((Message) ImsUt.this.mPendingCmds.get(key));
                ImsUt.this.mPendingCmds.remove(key);
            }
        }

        public void utConfigurationUpdateFailed(IImsUt ut, int id, ImsReasonInfo error) {
            Integer key = Integer.valueOf(id);
            synchronized (ImsUt.this.mLockObj) {
                ImsUt.this.sendFailureReport((Message) ImsUt.this.mPendingCmds.get(key), error);
                ImsUt.this.mPendingCmds.remove(key);
            }
        }

        public void utConfigurationQueried(IImsUt ut, int id, Bundle ssInfo) {
            Integer key = Integer.valueOf(id);
            synchronized (ImsUt.this.mLockObj) {
                ImsUt.this.sendSuccessReport((Message) ImsUt.this.mPendingCmds.get(key), ssInfo);
                ImsUt.this.mPendingCmds.remove(key);
            }
        }

        public void utConfigurationQueryFailed(IImsUt ut, int id, ImsReasonInfo error) {
            Integer key = Integer.valueOf(id);
            synchronized (ImsUt.this.mLockObj) {
                ImsUt.this.sendFailureReport((Message) ImsUt.this.mPendingCmds.get(key), error);
                ImsUt.this.mPendingCmds.remove(key);
            }
        }

        public void utConfigurationCallBarringQueried(IImsUt ut, int id, ImsSsInfo[] cbInfo) {
            Integer key = Integer.valueOf(id);
            synchronized (ImsUt.this.mLockObj) {
                ImsUt.this.sendSuccessReport((Message) ImsUt.this.mPendingCmds.get(key), cbInfo);
                ImsUt.this.mPendingCmds.remove(key);
            }
        }

        public void utConfigurationCallForwardQueried(IImsUt ut, int id, ImsCallForwardInfo[] cfInfo) {
            Integer key = Integer.valueOf(id);
            synchronized (ImsUt.this.mLockObj) {
                ImsUt.this.sendSuccessReport((Message) ImsUt.this.mPendingCmds.get(key), cfInfo);
                ImsUt.this.mPendingCmds.remove(key);
            }
        }

        public void utConfigurationCallWaitingQueried(IImsUt ut, int id, ImsSsInfo[] cwInfo) {
            Integer key = Integer.valueOf(id);
            synchronized (ImsUt.this.mLockObj) {
                ImsUt.this.sendSuccessReport((Message) ImsUt.this.mPendingCmds.get(key), cwInfo);
                ImsUt.this.mPendingCmds.remove(key);
            }
        }

        public void utConfigurationCallForwardInTimeSlotQueried(IImsUt ut, int id, ImsCallForwardInfoEx[] cfInfo) {
            Integer key = Integer.valueOf(id);
            synchronized (ImsUt.this.mLockObj) {
                ImsUt.this.sendSuccessReport((Message) ImsUt.this.mPendingCmds.get(key), cfInfo);
                ImsUt.this.mPendingCmds.remove(key);
            }
        }
    }

    @Override
    public void queryCallForwardInTimeSlot(int condition, Message result) {
        int id;
        log("queryCallForwardInTimeSlot :: Ut = " + this.miUt + ", condition = " + condition);
        synchronized (this.mLockObj) {
            try {
                id = this.miUt.queryCallForwardInTimeSlot(condition);
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
            if (id < 0) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(id), result);
            }
        }
    }

    @Override
    public void updateCallForwardInTimeSlot(int action, int condition, String number, int timeSeconds, long[] timeSlot, Message result) {
        int id;
        log("updateCallForwardInTimeSlot :: Ut = " + this.miUt + ", action = " + action + ", condition = " + condition + ", number = " + number + ", timeSeconds = " + timeSeconds + ", timeSlot = " + Arrays.toString(timeSlot));
        synchronized (this.mLockObj) {
            try {
                id = this.miUt.updateCallForwardInTimeSlot(action, condition, number, timeSeconds, timeSlot);
            } catch (RemoteException e) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            }
            if (id < 0) {
                sendFailureReport(result, new ImsReasonInfo(802, 0));
            } else {
                this.mPendingCmds.put(Integer.valueOf(id), result);
            }
        }
    }
}
