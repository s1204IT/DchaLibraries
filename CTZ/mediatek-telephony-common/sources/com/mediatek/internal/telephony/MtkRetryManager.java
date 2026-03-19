package com.mediatek.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.os.ServiceManager;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.RetryManager;
import com.android.internal.telephony.TelephonyDevController;
import com.android.internal.telephony.dataconnection.ApnSetting;
import com.mediatek.ims.internal.IMtkImsService;
import com.mediatek.internal.telephony.dataconnection.DcFailCauseManager;

public final class MtkRetryManager extends RetryManager {
    public static final String LOG_TAG = "MtkRetryManager";
    private static IMtkImsService mMtkImsService = null;
    private boolean mBcastRegistered;
    private BroadcastReceiver mBroadcastReceiver;
    private Context mContext;
    private DcFailCauseManager mDcFcMgr;
    private int mPhoneNum;
    private TelephonyDevController mTelDevController;

    static {
        MAX_SAME_APN_RETRY = 100;
    }

    public MtkRetryManager(Phone phone, String str) {
        super(phone, str);
        this.mTelDevController = TelephonyDevController.getInstance();
        this.mBcastRegistered = false;
        this.mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Rlog.d(MtkRetryManager.LOG_TAG, "mBroadcastReceiver: action " + intent.getAction() + ", mSameApnRetryCount:" + MtkRetryManager.this.mSameApnRetryCount + ", mModemSuggestedDelay:" + MtkRetryManager.this.mModemSuggestedDelay + ", mCurrentApnIndex:" + MtkRetryManager.this.mCurrentApnIndex);
                if (intent.getAction().equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                    int i = MtkRetryManager.this.mSameApnRetryCount;
                    long j = MtkRetryManager.this.mModemSuggestedDelay;
                    int i2 = MtkRetryManager.this.mCurrentApnIndex;
                    MtkRetryManager.this.configureRetryOnly();
                    MtkRetryManager.this.mSameApnRetryCount = i;
                    MtkRetryManager.this.mModemSuggestedDelay = j;
                    MtkRetryManager.this.mCurrentApnIndex = i2;
                }
            }
        };
        this.mPhoneNum = TelephonyManager.getDefault().getPhoneCount();
        this.mDcFcMgr = DcFailCauseManager.getInstance(this.mPhone);
        this.mContext = this.mPhone.getContext();
        if (!this.mBcastRegistered) {
            this.mContext.registerReceiver(this.mBroadcastReceiver, new IntentFilter("android.telephony.action.CARRIER_CONFIG_CHANGED"));
            this.mBcastRegistered = true;
        }
        if (mMtkImsService == null) {
            checkAndBindImsService();
        }
    }

    protected void finalize() {
        Rlog.d(LOG_TAG, "RetryManager finalized");
        this.mContext.unregisterReceiver(this.mBroadcastReceiver);
    }

    protected boolean configure(String str) {
        super.configure(str);
        return true;
    }

    protected void configureRetry() {
        super.configureRetry();
        if (this.mWaitingApns != null && this.mWaitingApns.size() != 0) {
            int i = this.mCurrentApnIndex;
            if (i < 0 || i >= this.mWaitingApns.size()) {
                i = 0;
            }
            Rlog.d(LOG_TAG, "configureRetry: mCurrentApnIndex: " + this.mCurrentApnIndex + ", reset MD data count for apn: " + ((ApnSetting) this.mWaitingApns.get(i)).apn);
            this.mPhone.mCi.resetMdDataRetryCount(((ApnSetting) this.mWaitingApns.get(i)).apn, null);
        } else {
            Rlog.e(LOG_TAG, "configureRetry: mWaitingApns is null or empty");
        }
        if (this.mTelDevController != null && this.mTelDevController.getModem(0) != null && ((MtkHardwareConfig) this.mTelDevController.getModem(0)).hasMdAutoSetupImsCapability()) {
            if (TextUtils.equals("ims", this.mApnType) || TextUtils.equals("emergency", this.mApnType)) {
                int[] iArr = new int[this.mPhoneNum];
                int[] iArr2 = new int[this.mPhoneNum];
                try {
                    int[] imsNetworkState = mMtkImsService.getImsNetworkState(4);
                    int[] imsNetworkState2 = mMtkImsService.getImsNetworkState(10);
                    if ((TextUtils.equals("ims", this.mApnType) && imsNetworkState[this.mPhone.getPhoneId()] == NetworkInfo.State.DISCONNECTED.ordinal()) || (TextUtils.equals("emergency", this.mApnType) && imsNetworkState2[this.mPhone.getPhoneId()] == NetworkInfo.State.DISCONNECTED.ordinal())) {
                        Rlog.d(LOG_TAG, "configureRetry: IMS/EIMS and disconnected, no retry by mobile.");
                        configure("max_retries=0, -1, -1, -1");
                    }
                } catch (Exception e) {
                    Rlog.d(LOG_TAG, "getImsNetworkState failed.");
                }
            }
        }
    }

    private void configureRetryOnly() {
        super.configureRetry();
    }

    public long getDelayForNextApn(boolean z) {
        long retryTimer;
        if (this.mWaitingApns == null || this.mWaitingApns.size() == 0) {
            Rlog.d(LOG_TAG, "Waiting APN list is null or empty.");
            return -1L;
        }
        if (this.mModemSuggestedDelay == -1) {
            Rlog.d(LOG_TAG, "Modem suggested not retrying.");
            return -1L;
        }
        if (this.mModemSuggestedDelay != -2 && this.mSameApnRetryCount < MAX_SAME_APN_RETRY) {
            if (this.mModemSuggestedDelay == DcFailCauseManager.retryConfigForCC33.retryTime.getValue() && this.mDcFcMgr != null && this.mDcFcMgr.isNetworkOperatorForCC33() && this.mSameApnRetryCount >= DcFailCauseManager.retryConfigForCC33.maxRetryCount.getValue()) {
                return -1L;
            }
            Rlog.d(LOG_TAG, "Modem suggested retry in " + this.mModemSuggestedDelay + " ms.");
            return this.mModemSuggestedDelay;
        }
        int i = this.mCurrentApnIndex;
        do {
            i++;
            if (i >= this.mWaitingApns.size()) {
                i = 0;
            }
            if (!((ApnSetting) this.mWaitingApns.get(i)).permanentFailed) {
                if (i <= this.mCurrentApnIndex) {
                    if (!this.mRetryForever && this.mRetryCount + 1 > this.mMaxRetryCount) {
                        Rlog.d(LOG_TAG, "Reached maximum retry count " + this.mMaxRetryCount + ".");
                        return -1L;
                    }
                    retryTimer = getRetryTimer();
                    this.mRetryCount++;
                } else {
                    retryTimer = this.mInterApnDelay;
                }
                if (z && retryTimer > this.mFailFastInterApnDelay) {
                    return this.mFailFastInterApnDelay;
                }
                return retryTimer;
            }
        } while (i != this.mCurrentApnIndex);
        Rlog.d(LOG_TAG, "All APNs have permanently failed.");
        return -1L;
    }

    private void checkAndBindImsService() {
        IBinder service = ServiceManager.getService("mtkIms");
        if (service == null) {
            return;
        }
        mMtkImsService = IMtkImsService.Stub.asInterface(service);
        if (mMtkImsService == null) {
            return;
        }
        Rlog.d(LOG_TAG, "checkAndBindImsService: mMtkImsService = " + mMtkImsService);
    }
}
