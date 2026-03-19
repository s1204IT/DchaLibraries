package com.mediatek.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.SmsStorageMonitor;

public class MtkSmsStorageMonitor extends SmsStorageMonitor {
    private static final int EVENT_ME_FULL = 100;
    private static final String TAG = "MtkSmsStorageMonitor";
    private final BroadcastReceiver mMtkResultReceiver;
    private boolean mPendingIccFullNotify;

    public MtkSmsStorageMonitor(Phone phone) {
        super(phone);
        this.mPendingIccFullNotify = false;
        this.mMtkResultReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED") && MtkSmsStorageMonitor.this.mPendingIccFullNotify) {
                    MtkSmsStorageMonitor.this.handleIccFull();
                    MtkSmsStorageMonitor.this.mPendingIccFullNotify = false;
                }
            }
        };
        if (this.mCi != null && (this.mCi instanceof MtkRIL)) {
            this.mCi.setOnMeSmsFull(this, 100, null);
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.BOOT_COMPLETED");
        this.mContext.registerReceiver(this.mMtkResultReceiver, intentFilter);
    }

    public void dispose() {
        Rlog.d(TAG, "disposed...");
        if (this.mCi != null && (this.mCi instanceof MtkRIL)) {
            this.mCi.unSetOnMeSmsFull(this);
        }
        super.dispose();
    }

    public void handleIccFull() {
        if (!SystemProperties.get("sys.boot_completed").equals("1")) {
            this.mPendingIccFullNotify = true;
            Rlog.d(TAG, "too early, wait for boot complete to send broadcast");
        } else {
            super.handleIccFull();
        }
    }

    public void handleMessage(Message message) {
        int i = message.what;
        if (i != 3) {
            if (i == 100) {
                handleMeFull();
                return;
            } else {
                super.handleMessage(message);
                return;
            }
        }
        Rlog.v(TAG, "Sending pending memory status report : mStorageAvailable = " + this.mStorageAvailable);
        this.mCi.reportSmsMemoryStatus(this.mStorageAvailable, obtainMessage(2));
    }

    private void handleMeFull() {
        Intent intent = new Intent("android.provider.Telephony.SMS_REJECTED");
        intent.putExtra("result", 3);
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhone.getPhoneId());
        this.mWakeLock.acquire(5000L);
        this.mContext.sendBroadcast(intent, "android.permission.RECEIVE_SMS");
    }
}
