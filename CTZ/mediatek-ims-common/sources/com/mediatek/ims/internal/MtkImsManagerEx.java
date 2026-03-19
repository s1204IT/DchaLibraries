package com.mediatek.ims.internal;

import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.Rlog;
import com.android.ims.ImsException;
import com.mediatek.ims.internal.IMtkImsService;

public class MtkImsManagerEx {
    private static final boolean DBG = true;
    public static final String MTK_IMS_SERVICE = "mtkIms";
    private static final String TAG = "MtkImsManagerEx";
    private static MtkImsManagerEx sInstance = new MtkImsManagerEx();
    private IMtkImsService mMtkImsService = null;
    private MtkImsServiceDeathRecipient mMtkDeathRecipient = new MtkImsServiceDeathRecipient();

    private MtkImsManagerEx() {
        bindMtkImsService(DBG);
    }

    public static MtkImsManagerEx getInstance() {
        return sInstance;
    }

    private void checkAndThrowExceptionIfServiceUnavailable() throws ImsException {
        if (this.mMtkImsService == null) {
            bindMtkImsService(DBG);
            if (this.mMtkImsService == null) {
                throw new ImsException("MtkImsService is unavailable", 106);
            }
        }
    }

    private static String getMtkImsServiceName() {
        return "mtkIms";
    }

    private void bindMtkImsService(boolean z) {
        if (z && ServiceManager.checkService(getMtkImsServiceName()) == null) {
            loge("bindMtkImsService binder is null");
            return;
        }
        IBinder service = ServiceManager.getService(getMtkImsServiceName());
        if (service != null) {
            try {
                service.linkToDeath(this.mMtkDeathRecipient, 0);
            } catch (RemoteException e) {
            }
        }
        this.mMtkImsService = IMtkImsService.Stub.asInterface(service);
        log("mMtkImsService = " + this.mMtkImsService);
    }

    private static void log(String str) {
        Rlog.d(TAG, str);
    }

    private static void logw(String str) {
        Rlog.w(TAG, str);
    }

    private static void loge(String str) {
        Rlog.e(TAG, str);
    }

    private static void loge(String str, Throwable th) {
        Rlog.e(TAG, str, th);
    }

    private class MtkImsServiceDeathRecipient implements IBinder.DeathRecipient {
        private MtkImsServiceDeathRecipient() {
        }

        @Override
        public void binderDied() {
            MtkImsManagerEx.this.mMtkImsService = null;
        }
    }

    public int getImsState(int i) throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            int imsState = this.mMtkImsService.getImsState(i);
            log("getImsState=" + imsState + " for phoneId=" + i);
            return imsState;
        } catch (RemoteException e) {
            throw new ImsException("getImsState()", e, 106);
        }
    }

    public int getModemMultiImsCount() throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            int modemMultiImsCount = this.mMtkImsService.getModemMultiImsCount();
            log("getModemMultiImsCount=" + modemMultiImsCount);
            return modemMultiImsCount;
        } catch (RemoteException e) {
            throw new ImsException("getModemMultiImsCount()", e, 106);
        }
    }

    public int getCurrentCallCount(int i) throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            int currentCallCount = this.mMtkImsService.getCurrentCallCount(i);
            log("getCurrentCallCount, phoneId: " + i + " callCount: " + currentCallCount);
            return currentCallCount;
        } catch (RemoteException e) {
            throw new ImsException("getCurrentCallCount()", e, 106);
        }
    }
}
