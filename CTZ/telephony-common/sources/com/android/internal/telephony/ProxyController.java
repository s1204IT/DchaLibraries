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
import android.util.Log;
import com.android.internal.telephony.uicc.UiccController;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class ProxyController {
    protected static final int EVENT_APPLY_RC_RESPONSE = 3;
    protected static final int EVENT_FINISH_RC_RESPONSE = 4;
    protected static final int EVENT_NOTIFICATION_RC_CHANGED = 1;
    protected static final int EVENT_START_RC_RESPONSE = 2;
    protected static final int EVENT_TIMEOUT = 5;
    static final String LOG_TAG = "ProxyController";
    protected static final int SET_RC_STATUS_APPLYING = 3;
    protected static final int SET_RC_STATUS_FAIL = 5;
    protected static final int SET_RC_STATUS_IDLE = 0;
    protected static final int SET_RC_STATUS_STARTED = 2;
    protected static final int SET_RC_STATUS_STARTING = 1;
    protected static final int SET_RC_STATUS_SUCCESS = 4;
    protected static final int SET_RC_TIMEOUT_WAITING_MSEC = 45000;
    private static ProxyController sProxyController;
    protected CommandsInterface[] mCi;
    protected Context mContext;
    protected String[] mCurrentLogicalModemIds;
    protected String[] mNewLogicalModemIds;
    protected int[] mNewRadioAccessFamily;
    protected int[] mOldRadioAccessFamily;
    protected PhoneSubInfoController mPhoneSubInfoController;
    protected PhoneSwitcher mPhoneSwitcher;
    protected Phone[] mPhones;
    protected int mRadioAccessFamilyStatusCounter;
    protected int mRadioCapabilitySessionId;
    protected int[] mSetRadioAccessFamilyStatus;
    protected UiccController mUiccController;
    protected UiccPhoneBookController mUiccPhoneBookController;
    protected UiccSmsController mUiccSmsController;
    protected PowerManager.WakeLock mWakeLock;
    protected boolean mTransactionFailed = false;
    protected AtomicInteger mUniqueIdGenerator = new AtomicInteger(new Random().nextInt());
    protected Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            ProxyController.this.logd("handleMessage msg.what=" + message.what);
            switch (message.what) {
                case 1:
                    ProxyController.this.onNotificationRadioCapabilityChanged(message);
                    break;
                case 2:
                    ProxyController.this.onStartRadioCapabilityResponse(message);
                    break;
                case 3:
                    ProxyController.this.onApplyRadioCapabilityResponse(message);
                    break;
                case 4:
                    ProxyController.this.onFinishRadioCapabilityResponse(message);
                    break;
                case 5:
                    ProxyController.this.onTimeoutRadioCapability(message);
                    break;
            }
        }
    };

    public static ProxyController getInstance(Context context, Phone[] phoneArr, UiccController uiccController, CommandsInterface[] commandsInterfaceArr, PhoneSwitcher phoneSwitcher) {
        if (sProxyController == null) {
            sProxyController = TelephonyComponentFactory.getInstance().makeProxyController(context, phoneArr, uiccController, commandsInterfaceArr, phoneSwitcher);
        }
        return sProxyController;
    }

    public static ProxyController getInstance() {
        return sProxyController;
    }

    public ProxyController(Context context, Phone[] phoneArr, UiccController uiccController, CommandsInterface[] commandsInterfaceArr, PhoneSwitcher phoneSwitcher) {
        logd("Constructor - Enter");
        this.mContext = context;
        this.mPhones = phoneArr;
        this.mUiccController = uiccController;
        this.mCi = commandsInterfaceArr;
        this.mPhoneSwitcher = phoneSwitcher;
        this.mUiccPhoneBookController = new UiccPhoneBookController(this.mPhones);
        this.mPhoneSubInfoController = new PhoneSubInfoController(this.mContext, this.mPhones);
        this.mUiccSmsController = new UiccSmsController();
        this.mSetRadioAccessFamilyStatus = new int[this.mPhones.length];
        this.mNewRadioAccessFamily = new int[this.mPhones.length];
        this.mOldRadioAccessFamily = new int[this.mPhones.length];
        this.mCurrentLogicalModemIds = new String[this.mPhones.length];
        this.mNewLogicalModemIds = new String[this.mPhones.length];
        this.mWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, LOG_TAG);
        this.mWakeLock.setReferenceCounted(false);
        clearTransaction();
        for (int i = 0; i < this.mPhones.length; i++) {
            this.mPhones[i].registerForRadioCapabilityChanged(this.mHandler, 1, null);
        }
        logd("Constructor - Exit");
    }

    public void updateDataConnectionTracker(int i) {
        this.mPhones[i].updateDataConnectionTracker();
    }

    public void enableDataConnectivity(int i) {
        this.mPhones[i].setInternalDataEnabled(true, null);
    }

    public void disableDataConnectivity(int i, Message message) {
        this.mPhones[i].setInternalDataEnabled(false, message);
    }

    public void updateCurrentCarrierInProvider(int i) {
        this.mPhones[i].updateCurrentCarrierInProvider();
    }

    public void registerForAllDataDisconnected(int i, Handler handler, int i2, Object obj) {
        int phoneId = SubscriptionController.getInstance().getPhoneId(i);
        if (phoneId >= 0 && phoneId < TelephonyManager.getDefault().getPhoneCount()) {
            this.mPhones[phoneId].registerForAllDataDisconnected(handler, i2, obj);
        }
    }

    public void unregisterForAllDataDisconnected(int i, Handler handler) {
        int phoneId = SubscriptionController.getInstance().getPhoneId(i);
        if (phoneId >= 0 && phoneId < TelephonyManager.getDefault().getPhoneCount()) {
            this.mPhones[phoneId].unregisterForAllDataDisconnected(handler);
        }
    }

    public boolean isDataDisconnected(int i) {
        int phoneId = SubscriptionController.getInstance().getPhoneId(i);
        if (phoneId >= 0 && phoneId < TelephonyManager.getDefault().getPhoneCount()) {
            return this.mPhones[phoneId].mDcTracker.isDisconnected();
        }
        return true;
    }

    public int getRadioAccessFamily(int i) {
        if (i >= this.mPhones.length) {
            return 1;
        }
        return this.mPhones[i].getRadioAccessFamily();
    }

    public boolean setRadioCapability(RadioAccessFamily[] radioAccessFamilyArr) {
        if (radioAccessFamilyArr.length != this.mPhones.length) {
            throw new RuntimeException("Length of input rafs must equal to total phone count");
        }
        synchronized (this.mSetRadioAccessFamilyStatus) {
            for (int i = 0; i < this.mPhones.length; i++) {
                if (this.mSetRadioAccessFamilyStatus[i] != 0) {
                    loge("setRadioCapability: Phone[" + i + "] is not idle. Rejecting request.");
                    return false;
                }
            }
            boolean z = true;
            for (int i2 = 0; i2 < this.mPhones.length; i2++) {
                if (this.mPhones[i2].getRadioAccessFamily() != radioAccessFamilyArr[i2].getRadioAccessFamily()) {
                    z = false;
                }
            }
            if (z) {
                logd("setRadioCapability: Already in requested configuration, nothing to do.");
                return true;
            }
            clearTransaction();
            this.mWakeLock.acquire();
            return doSetRadioCapabilities(radioAccessFamilyArr);
        }
    }

    protected boolean doSetRadioCapabilities(RadioAccessFamily[] radioAccessFamilyArr) {
        this.mRadioCapabilitySessionId = this.mUniqueIdGenerator.getAndIncrement();
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(5, this.mRadioCapabilitySessionId, 0), 45000L);
        synchronized (this.mSetRadioAccessFamilyStatus) {
            logd("setRadioCapability: new request session id=" + this.mRadioCapabilitySessionId);
            resetRadioAccessFamilyStatusCounter();
            for (int i = 0; i < radioAccessFamilyArr.length; i++) {
                int phoneId = radioAccessFamilyArr[i].getPhoneId();
                logd("setRadioCapability: phoneId=" + phoneId + " status=STARTING");
                this.mSetRadioAccessFamilyStatus[phoneId] = 1;
                this.mOldRadioAccessFamily[phoneId] = this.mPhones[phoneId].getRadioAccessFamily();
                int radioAccessFamily = radioAccessFamilyArr[i].getRadioAccessFamily();
                this.mNewRadioAccessFamily[phoneId] = radioAccessFamily;
                this.mCurrentLogicalModemIds[phoneId] = this.mPhones[phoneId].getModemUuId();
                this.mNewLogicalModemIds[phoneId] = getLogicalModemIdFromRaf(radioAccessFamily);
                logd("setRadioCapability: mOldRadioAccessFamily[" + phoneId + "]=" + this.mOldRadioAccessFamily[phoneId]);
                logd("setRadioCapability: mNewRadioAccessFamily[" + phoneId + "]=" + this.mNewRadioAccessFamily[phoneId]);
                sendRadioCapabilityRequest(phoneId, this.mRadioCapabilitySessionId, 1, this.mOldRadioAccessFamily[phoneId], this.mCurrentLogicalModemIds[phoneId], 0, 2);
            }
        }
        return true;
    }

    protected void onStartRadioCapabilityResponse(Message message) {
        synchronized (this.mSetRadioAccessFamilyStatus) {
            AsyncResult asyncResult = (AsyncResult) message.obj;
            if (TelephonyManager.getDefault().getPhoneCount() == 1 && asyncResult.exception != null) {
                logd("onStartRadioCapabilityResponse got exception=" + asyncResult.exception);
                this.mRadioCapabilitySessionId = this.mUniqueIdGenerator.getAndIncrement();
                this.mContext.sendBroadcast(new Intent("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED"));
                clearTransaction();
                return;
            }
            RadioCapability radioCapability = (RadioCapability) ((AsyncResult) message.obj).result;
            if (radioCapability != null && radioCapability.getSession() == this.mRadioCapabilitySessionId) {
                this.mRadioAccessFamilyStatusCounter--;
                int phoneId = radioCapability.getPhoneId();
                if (((AsyncResult) message.obj).exception != null) {
                    logd("onStartRadioCapabilityResponse: Error response session=" + radioCapability.getSession());
                    logd("onStartRadioCapabilityResponse: phoneId=" + phoneId + " status=FAIL");
                    this.mSetRadioAccessFamilyStatus[phoneId] = 5;
                    this.mTransactionFailed = true;
                } else {
                    logd("onStartRadioCapabilityResponse: phoneId=" + phoneId + " status=STARTED");
                    this.mSetRadioAccessFamilyStatus[phoneId] = 2;
                }
                if (this.mRadioAccessFamilyStatusCounter == 0) {
                    HashSet hashSet = new HashSet(this.mNewLogicalModemIds.length);
                    for (String str : this.mNewLogicalModemIds) {
                        if (!hashSet.add(str)) {
                            this.mTransactionFailed = true;
                            Log.wtf(LOG_TAG, "ERROR: sending down the same id for different phones");
                        }
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("onStartRadioCapabilityResponse: success=");
                    sb.append(!this.mTransactionFailed);
                    logd(sb.toString());
                    if (this.mTransactionFailed) {
                        issueFinish(this.mRadioCapabilitySessionId);
                    } else {
                        resetRadioAccessFamilyStatusCounter();
                        for (int i = 0; i < this.mPhones.length; i++) {
                            sendRadioCapabilityRequest(i, this.mRadioCapabilitySessionId, 2, this.mNewRadioAccessFamily[i], this.mNewLogicalModemIds[i], 0, 3);
                            logd("onStartRadioCapabilityResponse: phoneId=" + i + " status=APPLYING");
                            this.mSetRadioAccessFamilyStatus[i] = 3;
                        }
                    }
                }
                return;
            }
            logd("onStartRadioCapabilityResponse: Ignore session=" + this.mRadioCapabilitySessionId + " rc=" + radioCapability);
        }
    }

    protected void onApplyRadioCapabilityResponse(Message message) {
        RadioCapability radioCapability = (RadioCapability) ((AsyncResult) message.obj).result;
        if (radioCapability == null || radioCapability.getSession() != this.mRadioCapabilitySessionId) {
            logd("onApplyRadioCapabilityResponse: Ignore session=" + this.mRadioCapabilitySessionId + " rc=" + radioCapability);
            onApplyRadioCapabilityErrorHandler(message);
            return;
        }
        logd("onApplyRadioCapabilityResponse: rc=" + radioCapability);
        if (((AsyncResult) message.obj).exception != null) {
            synchronized (this.mSetRadioAccessFamilyStatus) {
                logd("onApplyRadioCapabilityResponse: Error response session=" + radioCapability.getSession());
                int phoneId = radioCapability.getPhoneId();
                onApplyExceptionHandler(message);
                logd("onApplyRadioCapabilityResponse: phoneId=" + phoneId + " status=FAIL");
                this.mSetRadioAccessFamilyStatus[phoneId] = 5;
                this.mTransactionFailed = true;
            }
            return;
        }
        logd("onApplyRadioCapabilityResponse: Valid start expecting notification rc=" + radioCapability);
    }

    protected void onNotificationRadioCapabilityChanged(Message message) {
        RadioCapability radioCapability = (RadioCapability) ((AsyncResult) message.obj).result;
        if (radioCapability == null || radioCapability.getSession() != this.mRadioCapabilitySessionId) {
            logd("onNotificationRadioCapabilityChanged: Ignore session=" + this.mRadioCapabilitySessionId + " rc=" + radioCapability);
            return;
        }
        synchronized (this.mSetRadioAccessFamilyStatus) {
            logd("onNotificationRadioCapabilityChanged: rc=" + radioCapability);
            if (radioCapability.getSession() != this.mRadioCapabilitySessionId) {
                logd("onNotificationRadioCapabilityChanged: Ignore session=" + this.mRadioCapabilitySessionId + " rc=" + radioCapability);
                return;
            }
            int phoneId = radioCapability.getPhoneId();
            if (((AsyncResult) message.obj).exception != null || radioCapability.getStatus() == 2) {
                logd("onNotificationRadioCapabilityChanged: phoneId=" + phoneId + " status=FAIL");
                this.mSetRadioAccessFamilyStatus[phoneId] = 5;
                this.mTransactionFailed = true;
            } else {
                logd("onNotificationRadioCapabilityChanged: phoneId=" + phoneId + " status=SUCCESS");
                this.mSetRadioAccessFamilyStatus[phoneId] = 4;
                this.mPhoneSwitcher.resendDataAllowed(phoneId);
                this.mPhones[phoneId].radioCapabilityUpdated(radioCapability);
            }
            this.mRadioAccessFamilyStatusCounter--;
            if (this.mRadioAccessFamilyStatusCounter == 0) {
                logd("onNotificationRadioCapabilityChanged: APPLY URC success=" + this.mTransactionFailed);
                issueFinish(this.mRadioCapabilitySessionId);
            }
        }
    }

    protected void onFinishRadioCapabilityResponse(Message message) {
        RadioCapability radioCapability = (RadioCapability) ((AsyncResult) message.obj).result;
        if (radioCapability == null || radioCapability.getSession() != this.mRadioCapabilitySessionId) {
            logd("onFinishRadioCapabilityResponse: Ignore session=" + this.mRadioCapabilitySessionId + " rc=" + radioCapability);
            return;
        }
        synchronized (this.mSetRadioAccessFamilyStatus) {
            logd(" onFinishRadioCapabilityResponse mRadioAccessFamilyStatusCounter=" + this.mRadioAccessFamilyStatusCounter);
            this.mRadioAccessFamilyStatusCounter = this.mRadioAccessFamilyStatusCounter + (-1);
            if (this.mRadioAccessFamilyStatusCounter == 0) {
                completeRadioCapabilityTransaction();
            }
        }
    }

    protected void onTimeoutRadioCapability(Message message) {
        if (message.arg1 != this.mRadioCapabilitySessionId) {
            logd("RadioCapability timeout: Ignore msg.arg1=" + message.arg1 + "!= mRadioCapabilitySessionId=" + this.mRadioCapabilitySessionId);
            return;
        }
        synchronized (this.mSetRadioAccessFamilyStatus) {
            for (int i = 0; i < this.mPhones.length; i++) {
                logd("RadioCapability timeout: mSetRadioAccessFamilyStatus[" + i + "]=" + this.mSetRadioAccessFamilyStatus[i]);
            }
            this.mRadioCapabilitySessionId = this.mUniqueIdGenerator.getAndIncrement();
            this.mRadioAccessFamilyStatusCounter = 0;
            this.mTransactionFailed = true;
            issueFinish(this.mRadioCapabilitySessionId);
        }
    }

    protected void issueFinish(int i) {
        synchronized (this.mSetRadioAccessFamilyStatus) {
            for (int i2 = 0; i2 < this.mPhones.length; i2++) {
                logd("issueFinish: phoneId=" + i2 + " sessionId=" + i + " mTransactionFailed=" + this.mTransactionFailed);
                this.mRadioAccessFamilyStatusCounter = this.mRadioAccessFamilyStatusCounter + 1;
                sendRadioCapabilityRequest(i2, i, 4, this.mTransactionFailed ? this.mOldRadioAccessFamily[i2] : this.mNewRadioAccessFamily[i2], this.mTransactionFailed ? this.mCurrentLogicalModemIds[i2] : this.mNewLogicalModemIds[i2], this.mTransactionFailed ? 2 : 1, 4);
                if (this.mTransactionFailed) {
                    logd("issueFinish: phoneId: " + i2 + " status: FAIL");
                    this.mSetRadioAccessFamilyStatus[i2] = 5;
                }
            }
        }
    }

    protected void completeRadioCapabilityTransaction() {
        Intent intent;
        StringBuilder sb = new StringBuilder();
        sb.append("onFinishRadioCapabilityResponse: success=");
        sb.append(!this.mTransactionFailed);
        logd(sb.toString());
        int i = 0;
        if (!this.mTransactionFailed) {
            ArrayList<? extends Parcelable> arrayList = new ArrayList<>();
            while (i < this.mPhones.length) {
                int radioAccessFamily = this.mPhones[i].getRadioAccessFamily();
                logd("radioAccessFamily[" + i + "]=" + radioAccessFamily);
                arrayList.add(new RadioAccessFamily(i, radioAccessFamily));
                i++;
            }
            Intent intent2 = new Intent("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE");
            intent2.putParcelableArrayListExtra("rafs", arrayList);
            this.mRadioCapabilitySessionId = this.mUniqueIdGenerator.getAndIncrement();
            clearTransaction();
            intent = intent2;
        } else {
            intent = new Intent("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED");
            this.mTransactionFailed = false;
            RadioAccessFamily[] radioAccessFamilyArr = new RadioAccessFamily[this.mPhones.length];
            while (i < this.mPhones.length) {
                radioAccessFamilyArr[i] = new RadioAccessFamily(i, this.mOldRadioAccessFamily[i]);
                i++;
            }
            doSetRadioCapabilities(radioAccessFamilyArr);
        }
        this.mContext.sendBroadcast(intent, "android.permission.READ_PHONE_STATE");
    }

    protected void clearTransaction() {
        logd("clearTransaction");
        synchronized (this.mSetRadioAccessFamilyStatus) {
            for (int i = 0; i < this.mPhones.length; i++) {
                logd("clearTransaction: phoneId=" + i + " status=IDLE");
                this.mSetRadioAccessFamilyStatus[i] = 0;
                this.mOldRadioAccessFamily[i] = 0;
                this.mNewRadioAccessFamily[i] = 0;
                this.mTransactionFailed = false;
            }
            if (this.mWakeLock.isHeld()) {
                this.mWakeLock.release();
            }
        }
    }

    protected void resetRadioAccessFamilyStatusCounter() {
        this.mRadioAccessFamilyStatusCounter = this.mPhones.length;
    }

    protected void sendRadioCapabilityRequest(int i, int i2, int i3, int i4, String str, int i5, int i6) {
        this.mPhones[i].setRadioCapability(new RadioCapability(i, i2, i3, i4, str, i5), this.mHandler.obtainMessage(i6));
    }

    public int getMaxRafSupported() {
        int[] iArr = new int[this.mPhones.length];
        int radioAccessFamily = 1;
        int i = 0;
        for (int i2 = 0; i2 < this.mPhones.length; i2++) {
            iArr[i2] = Integer.bitCount(this.mPhones[i2].getRadioAccessFamily());
            if (i < iArr[i2]) {
                i = iArr[i2];
                radioAccessFamily = this.mPhones[i2].getRadioAccessFamily();
            }
        }
        return radioAccessFamily;
    }

    public int getMinRafSupported() {
        int[] iArr = new int[this.mPhones.length];
        int radioAccessFamily = 1;
        int i = 0;
        for (int i2 = 0; i2 < this.mPhones.length; i2++) {
            iArr[i2] = Integer.bitCount(this.mPhones[i2].getRadioAccessFamily());
            if (i == 0 || i > iArr[i2]) {
                i = iArr[i2];
                radioAccessFamily = this.mPhones[i2].getRadioAccessFamily();
            }
        }
        return radioAccessFamily;
    }

    protected String getLogicalModemIdFromRaf(int i) {
        for (int i2 = 0; i2 < this.mPhones.length; i2++) {
            if (this.mPhones[i2].getRadioAccessFamily() == i) {
                return this.mPhones[i2].getModemUuId();
            }
        }
        return null;
    }

    protected void logd(String str) {
        Rlog.d(LOG_TAG, str);
    }

    protected void loge(String str) {
        Rlog.e(LOG_TAG, str);
    }

    protected void onApplyRadioCapabilityErrorHandler(Message message) {
    }

    protected void onApplyExceptionHandler(Message message) {
    }
}
