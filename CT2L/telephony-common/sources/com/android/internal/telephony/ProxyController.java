package com.android.internal.telephony;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.PowerManager;
import android.telephony.RadioAccessFamily;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.dataconnection.DctController;
import com.android.internal.telephony.uicc.UiccController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class ProxyController {
    private static final int EVENT_APPLY_RC_RESPONSE = 3;
    private static final int EVENT_FINISH_RC_RESPONSE = 4;
    private static final int EVENT_NOTIFICATION_RC_CHANGED = 1;
    private static final int EVENT_START_RC_RESPONSE = 2;
    static final String LOG_TAG = "ProxyController";
    private static final int SET_RC_STATUS_APPLYING = 3;
    private static final int SET_RC_STATUS_FAIL = 5;
    private static final int SET_RC_STATUS_IDLE = 0;
    private static final int SET_RC_STATUS_STARTED = 2;
    private static final int SET_RC_STATUS_STARTING = 1;
    private static final int SET_RC_STATUS_SUCCESS = 4;
    private static final int SET_RC_TIMEOUT_WAITING_MSEC = 45000;
    private static ProxyController sProxyController;
    private CommandsInterface[] mCi;
    private Context mContext;
    private DctController mDctController;
    private String[] mLogicalModemIds;
    private int[] mNewRadioAccessFamily;
    private int[] mOldRadioAccessFamily;
    private PhoneSubInfoController mPhoneSubInfoController;
    private PhoneProxy[] mProxyPhones;
    private int mRadioAccessFamilyStatusCounter;
    private int mRadioCapabilitySessionId;
    private int[] mSetRadioAccessFamilyStatus;
    RadioCapabilityRunnable mSetRadioCapabilityRunnable;
    private UiccController mUiccController;
    private UiccPhoneBookController mUiccPhoneBookController;
    private UiccSmsController mUiccSmsController;
    PowerManager.WakeLock mWakeLock;
    private AtomicInteger mUniqueIdGenerator = new AtomicInteger(new Random().nextInt());
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            ProxyController.this.logd("handleMessage msg.what=" + msg.what);
            switch (msg.what) {
                case 1:
                    ProxyController.this.onNotificationRadioCapabilityChanged(msg);
                    break;
                case 2:
                    ProxyController.this.onStartRadioCapabilityResponse(msg);
                    break;
                case 3:
                    ProxyController.this.onApplyRadioCapabilityResponse(msg);
                    break;
                case 4:
                    ProxyController.this.onFinishRadioCapabilityResponse(msg);
                    break;
            }
        }
    };

    public static ProxyController getInstance(Context context, PhoneProxy[] phoneProxy, UiccController uiccController, CommandsInterface[] ci) {
        if (sProxyController == null) {
            sProxyController = new ProxyController(context, phoneProxy, uiccController, ci);
        }
        return sProxyController;
    }

    public static ProxyController getInstance() {
        return sProxyController;
    }

    private ProxyController(Context context, PhoneProxy[] phoneProxy, UiccController uiccController, CommandsInterface[] ci) {
        logd("Constructor - Enter");
        this.mContext = context;
        this.mProxyPhones = phoneProxy;
        this.mUiccController = uiccController;
        this.mCi = ci;
        this.mDctController = DctController.makeDctController(phoneProxy);
        this.mUiccPhoneBookController = new UiccPhoneBookController(this.mProxyPhones);
        this.mPhoneSubInfoController = new PhoneSubInfoController(this.mProxyPhones);
        this.mUiccSmsController = new UiccSmsController(this.mProxyPhones);
        this.mSetRadioAccessFamilyStatus = new int[this.mProxyPhones.length];
        this.mNewRadioAccessFamily = new int[this.mProxyPhones.length];
        this.mOldRadioAccessFamily = new int[this.mProxyPhones.length];
        this.mLogicalModemIds = new String[this.mProxyPhones.length];
        for (int i = 0; i < this.mProxyPhones.length; i++) {
            this.mLogicalModemIds[i] = Integer.toString(i);
        }
        this.mSetRadioCapabilityRunnable = new RadioCapabilityRunnable();
        PowerManager pm = (PowerManager) context.getSystemService("power");
        this.mWakeLock = pm.newWakeLock(1, LOG_TAG);
        this.mWakeLock.setReferenceCounted(false);
        clearTransaction();
        for (int i2 = 0; i2 < this.mProxyPhones.length; i2++) {
            this.mProxyPhones[i2].registerForRadioCapabilityChanged(this.mHandler, 1, null);
        }
        logd("Constructor - Exit");
    }

    public void updateDataConnectionTracker(int sub) {
        this.mProxyPhones[sub].updateDataConnectionTracker();
    }

    public void enableDataConnectivity(int sub) {
        this.mProxyPhones[sub].setInternalDataEnabled(true);
    }

    public void disableDataConnectivity(int sub, Message dataCleanedUpMsg) {
        this.mProxyPhones[sub].setInternalDataEnabled(false, dataCleanedUpMsg);
    }

    public void updateCurrentCarrierInProvider(int sub) {
        this.mProxyPhones[sub].updateCurrentCarrierInProvider();
    }

    public void registerForAllDataDisconnected(int subId, Handler h, int what, Object obj) {
        int phoneId = SubscriptionController.getInstance().getPhoneId(subId);
        if (phoneId >= 0 && phoneId < TelephonyManager.getDefault().getPhoneCount()) {
            this.mProxyPhones[phoneId].registerForAllDataDisconnected(h, what, obj);
        }
    }

    public void unregisterForAllDataDisconnected(int subId, Handler h) {
        int phoneId = SubscriptionController.getInstance().getPhoneId(subId);
        if (phoneId >= 0 && phoneId < TelephonyManager.getDefault().getPhoneCount()) {
            this.mProxyPhones[phoneId].unregisterForAllDataDisconnected(h);
        }
    }

    public boolean isDataDisconnected(int subId) {
        int phoneId = SubscriptionController.getInstance().getPhoneId(subId);
        if (phoneId < 0 || phoneId >= TelephonyManager.getDefault().getPhoneCount()) {
            return true;
        }
        Phone activePhone = this.mProxyPhones[phoneId].getActivePhone();
        return ((PhoneBase) activePhone).mDcTracker.isDisconnected();
    }

    public int getRadioAccessFamily(int phoneId) {
        if (phoneId >= this.mProxyPhones.length) {
            return 1;
        }
        return this.mProxyPhones[phoneId].getRadioAccessFamily();
    }

    public boolean setRadioCapability(RadioAccessFamily[] rafs) {
        if (rafs.length != this.mProxyPhones.length) {
            throw new RuntimeException("Length of input rafs must equal to total phone count");
        }
        synchronized (this.mSetRadioAccessFamilyStatus) {
            for (int i = 0; i < this.mProxyPhones.length; i++) {
                logd("setRadioCapability: mSetRadioAccessFamilyStatus[" + i + "]=" + this.mSetRadioAccessFamilyStatus[i]);
                if (this.mSetRadioAccessFamilyStatus[i] != 0) {
                    loge("setRadioCapability: Phone[" + i + "] is not idle. Rejecting request.");
                    return false;
                }
            }
            clearTransaction();
            this.mRadioCapabilitySessionId = this.mUniqueIdGenerator.getAndIncrement();
            this.mWakeLock.acquire();
            this.mSetRadioCapabilityRunnable.setTimeoutState(this.mRadioCapabilitySessionId);
            this.mHandler.postDelayed(this.mSetRadioCapabilityRunnable, 45000L);
            synchronized (this.mSetRadioAccessFamilyStatus) {
                logd("setRadioCapability: new request session id=" + this.mRadioCapabilitySessionId);
                this.mRadioAccessFamilyStatusCounter = rafs.length;
                for (int i2 = 0; i2 < rafs.length; i2++) {
                    int phoneId = rafs[i2].getPhoneId();
                    logd("setRadioCapability: phoneId=" + phoneId + " status=STARTING");
                    this.mSetRadioAccessFamilyStatus[phoneId] = 1;
                    this.mOldRadioAccessFamily[phoneId] = this.mProxyPhones[phoneId].getRadioAccessFamily();
                    int requestedRaf = rafs[i2].getRadioAccessFamily();
                    this.mNewRadioAccessFamily[phoneId] = requestedRaf;
                    logd("setRadioCapability: mOldRadioAccessFamily[" + phoneId + "]=" + this.mOldRadioAccessFamily[phoneId]);
                    logd("setRadioCapability: mNewRadioAccessFamily[" + phoneId + "]=" + this.mNewRadioAccessFamily[phoneId]);
                    sendRadioCapabilityRequest(phoneId, this.mRadioCapabilitySessionId, 1, this.mOldRadioAccessFamily[phoneId], this.mLogicalModemIds[phoneId], 0, 2);
                }
            }
            return true;
        }
    }

    private void onStartRadioCapabilityResponse(Message msg) {
        synchronized (this.mSetRadioAccessFamilyStatus) {
            RadioCapability rc = (RadioCapability) ((AsyncResult) msg.obj).result;
            if (rc == null || rc.getSession() != this.mRadioCapabilitySessionId) {
                logd("onStartRadioCapabilityResponse: Ignore session=" + this.mRadioCapabilitySessionId + " rc=" + rc);
                return;
            }
            this.mRadioAccessFamilyStatusCounter--;
            int id = rc.getPhoneId();
            if (((AsyncResult) msg.obj).exception != null) {
                logd("onStartRadioCapabilityResponse: Error response session=" + rc.getSession());
                logd("onStartRadioCapabilityResponse: phoneId=" + id + " status=FAIL");
                this.mSetRadioAccessFamilyStatus[id] = 5;
            } else {
                logd("onStartRadioCapabilityResponse: phoneId=" + id + " status=STARTED");
                this.mSetRadioAccessFamilyStatus[id] = 2;
            }
            if (this.mRadioAccessFamilyStatusCounter == 0) {
                resetRadioAccessFamilyStatusCounter();
                boolean success = checkAllRadioCapabilitySuccess();
                logd("onStartRadioCapabilityResponse: success=" + success);
                if (!success) {
                    issueFinish(2, this.mRadioCapabilitySessionId);
                } else {
                    for (int i = 0; i < this.mProxyPhones.length; i++) {
                        sendRadioCapabilityRequest(i, this.mRadioCapabilitySessionId, 2, this.mNewRadioAccessFamily[i], this.mLogicalModemIds[i], 0, 3);
                        logd("onStartRadioCapabilityResponse: phoneId=" + i + " status=APPLYING");
                        this.mSetRadioAccessFamilyStatus[i] = 3;
                    }
                }
            }
        }
    }

    private void onApplyRadioCapabilityResponse(Message msg) {
        RadioCapability rc = (RadioCapability) ((AsyncResult) msg.obj).result;
        if (rc == null || rc.getSession() != this.mRadioCapabilitySessionId) {
            logd("onApplyRadioCapabilityResponse: Ignore session=" + this.mRadioCapabilitySessionId + " rc=" + rc);
            return;
        }
        logd("onApplyRadioCapabilityResponse: rc=" + rc);
        if (((AsyncResult) msg.obj).exception != null) {
            synchronized (this.mSetRadioAccessFamilyStatus) {
                logd("onApplyRadioCapabilityResponse: Error response session=" + rc.getSession());
                int id = rc.getPhoneId();
                logd("onApplyRadioCapabilityResponse: phoneId=" + id + " status=FAIL");
                this.mSetRadioAccessFamilyStatus[id] = 5;
            }
            return;
        }
        logd("onApplyRadioCapabilityResponse: Valid start expecting notification rc=" + rc);
    }

    private void onNotificationRadioCapabilityChanged(Message msg) {
        int status;
        RadioCapability rc = (RadioCapability) ((AsyncResult) msg.obj).result;
        if (rc == null || rc.getSession() != this.mRadioCapabilitySessionId) {
            logd("onNotificationRadioCapabilityChanged: Ignore session=" + this.mRadioCapabilitySessionId + " rc=" + rc);
            return;
        }
        synchronized (this.mSetRadioAccessFamilyStatus) {
            logd("onNotificationRadioCapabilityChanged: rc=" + rc);
            if (rc.getSession() != this.mRadioCapabilitySessionId) {
                logd("onNotificationRadioCapabilityChanged: Ignore session=" + this.mRadioCapabilitySessionId + " rc=" + rc);
                return;
            }
            int id = rc.getPhoneId();
            if (((AsyncResult) msg.obj).exception != null || rc.getStatus() == 2) {
                logd("onNotificationRadioCapabilityChanged: phoneId=" + id + " status=FAIL");
                this.mSetRadioAccessFamilyStatus[id] = 5;
            } else {
                logd("onNotificationRadioCapabilityChanged: phoneId=" + id + " status=SUCCESS");
                this.mSetRadioAccessFamilyStatus[id] = 4;
            }
            this.mRadioAccessFamilyStatusCounter--;
            if (this.mRadioAccessFamilyStatusCounter == 0) {
                logd("onNotificationRadioCapabilityChanged: removing callback from handler");
                this.mHandler.removeCallbacks(this.mSetRadioCapabilityRunnable);
                resetRadioAccessFamilyStatusCounter();
                boolean success = checkAllRadioCapabilitySuccess();
                logd("onNotificationRadioCapabilityChanged: APPLY URC success=" + success);
                if (success) {
                    status = 1;
                } else {
                    status = 2;
                }
                issueFinish(status, this.mRadioCapabilitySessionId);
            }
        }
    }

    void onFinishRadioCapabilityResponse(Message msg) {
        RadioCapability rc = (RadioCapability) ((AsyncResult) msg.obj).result;
        if (rc == null || rc.getSession() != this.mRadioCapabilitySessionId) {
            logd("onFinishRadioCapabilityResponse: Ignore session=" + this.mRadioCapabilitySessionId + " rc=" + rc);
            return;
        }
        synchronized (this.mSetRadioAccessFamilyStatus) {
            logd(" onFinishRadioCapabilityResponse mRadioAccessFamilyStatusCounter=" + this.mRadioAccessFamilyStatusCounter);
            this.mRadioAccessFamilyStatusCounter--;
            if (this.mRadioAccessFamilyStatusCounter == 0) {
                completeRadioCapabilityTransaction();
            }
        }
    }

    private void issueFinish(int status, int sessionId) {
        synchronized (this.mSetRadioAccessFamilyStatus) {
            for (int i = 0; i < this.mProxyPhones.length; i++) {
                if (this.mSetRadioAccessFamilyStatus[i] != 5) {
                    logd("issueFinish: phoneId=" + i + " sessionId=" + sessionId + " status=" + status);
                    sendRadioCapabilityRequest(i, sessionId, 4, this.mOldRadioAccessFamily[i], this.mLogicalModemIds[i], status, 4);
                    if (status == 2) {
                        logd("issueFinish: phoneId: " + i + " status: FAIL");
                        this.mSetRadioAccessFamilyStatus[i] = 5;
                    }
                } else {
                    logd("issueFinish: Ignore already FAIL, Phone" + i + " sessionId=" + sessionId + " status=" + status);
                }
            }
        }
    }

    private void completeRadioCapabilityTransaction() {
        Intent intent;
        boolean success = checkAllRadioCapabilitySuccess();
        logd("onFinishRadioCapabilityResponse: success=" + success);
        if (success) {
            ArrayList<? extends Parcelable> arrayList = new ArrayList<>();
            for (int i = 0; i < this.mProxyPhones.length; i++) {
                int raf = this.mProxyPhones[i].getRadioAccessFamily();
                logd("radioAccessFamily[" + i + "]=" + raf);
                RadioAccessFamily phoneRC = new RadioAccessFamily(i, raf);
                arrayList.add(phoneRC);
            }
            intent = new Intent("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE");
            intent.putParcelableArrayListExtra("rafs", arrayList);
        } else {
            intent = new Intent("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED");
        }
        clearTransaction();
        this.mContext.sendBroadcast(intent);
    }

    private void clearTransaction() {
        logd("clearTransaction");
        synchronized (this.mSetRadioAccessFamilyStatus) {
            for (int i = 0; i < this.mProxyPhones.length; i++) {
                logd("clearTransaction: phoneId=" + i + " status=IDLE");
                this.mSetRadioAccessFamilyStatus[i] = 0;
                this.mOldRadioAccessFamily[i] = 0;
                this.mNewRadioAccessFamily[i] = 0;
            }
            if (this.mWakeLock.isHeld()) {
                this.mWakeLock.release();
            }
        }
    }

    private boolean checkAllRadioCapabilitySuccess() {
        boolean z;
        synchronized (this.mSetRadioAccessFamilyStatus) {
            int i = 0;
            while (true) {
                if (i < this.mProxyPhones.length) {
                    if (this.mSetRadioAccessFamilyStatus[i] != 5) {
                        i++;
                    } else {
                        z = false;
                        break;
                    }
                } else {
                    z = true;
                    break;
                }
            }
        }
        return z;
    }

    private void resetRadioAccessFamilyStatusCounter() {
        this.mRadioAccessFamilyStatusCounter = this.mProxyPhones.length;
    }

    private void sendRadioCapabilityRequest(int phoneId, int sessionId, int rcPhase, int radioFamily, String logicalModemId, int status, int eventId) {
        RadioCapability requestRC = new RadioCapability(phoneId, sessionId, rcPhase, radioFamily, logicalModemId, status);
        this.mProxyPhones[phoneId].setRadioCapability(requestRC, this.mHandler.obtainMessage(eventId));
    }

    private class RadioCapabilityRunnable implements Runnable {
        private int mSessionId;

        public RadioCapabilityRunnable() {
        }

        public void setTimeoutState(int sessionId) {
            this.mSessionId = sessionId;
        }

        @Override
        public void run() {
            if (this.mSessionId != ProxyController.this.mRadioCapabilitySessionId) {
                ProxyController.this.logd("RadioCapability timeout: Ignore mSessionId=" + this.mSessionId + "!= mRadioCapabilitySessionId=" + ProxyController.this.mRadioCapabilitySessionId);
                return;
            }
            synchronized (ProxyController.this.mSetRadioAccessFamilyStatus) {
                for (int i = 0; i < ProxyController.this.mProxyPhones.length; i++) {
                    ProxyController.this.logd("RadioCapability timeout: mSetRadioAccessFamilyStatus[" + i + "]=" + ProxyController.this.mSetRadioAccessFamilyStatus[i]);
                }
                int uniqueDifferentId = ProxyController.this.mUniqueIdGenerator.getAndIncrement();
                ProxyController.this.issueFinish(2, uniqueDifferentId);
                ProxyController.this.completeRadioCapabilityTransaction();
            }
        }
    }

    private void logd(String string) {
        Rlog.d(LOG_TAG, string);
    }

    private void loge(String string) {
        Rlog.e(LOG_TAG, string);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        try {
            this.mDctController.dump(fd, pw, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
