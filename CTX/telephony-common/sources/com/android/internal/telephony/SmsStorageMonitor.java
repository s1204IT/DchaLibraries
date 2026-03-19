package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;

public class SmsStorageMonitor extends Handler {
    private static final int EVENT_ICC_FULL = 1;
    protected static final int EVENT_RADIO_ON = 3;
    protected static final int EVENT_REPORT_MEMORY_STATUS_DONE = 2;
    private static final String TAG = "SmsStorageMonitor";
    protected static final int WAKE_LOCK_TIMEOUT = 5000;
    protected final CommandsInterface mCi;
    protected final Context mContext;
    protected Phone mPhone;
    private boolean mReportMemoryStatusPending;
    protected PowerManager.WakeLock mWakeLock;
    protected boolean mStorageAvailable = true;
    private final BroadcastReceiver mResultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.DEVICE_STORAGE_FULL")) {
                SmsStorageMonitor.this.mStorageAvailable = false;
                SmsStorageMonitor.this.mCi.reportSmsMemoryStatus(false, SmsStorageMonitor.this.obtainMessage(2));
            } else if (intent.getAction().equals("android.intent.action.DEVICE_STORAGE_NOT_FULL")) {
                SmsStorageMonitor.this.mStorageAvailable = true;
                SmsStorageMonitor.this.mCi.reportSmsMemoryStatus(true, SmsStorageMonitor.this.obtainMessage(2));
            }
        }
    };

    public SmsStorageMonitor(Phone phone) {
        this.mPhone = phone;
        this.mContext = phone.getContext();
        this.mCi = phone.mCi;
        createWakelock();
        this.mCi.setOnIccSmsFull(this, 1, null);
        this.mCi.registerForOn(this, 3, null);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.DEVICE_STORAGE_FULL");
        intentFilter.addAction("android.intent.action.DEVICE_STORAGE_NOT_FULL");
        this.mContext.registerReceiver(this.mResultReceiver, intentFilter);
    }

    public void dispose() {
        this.mCi.unSetOnIccSmsFull(this);
        this.mCi.unregisterForOn(this);
        this.mContext.unregisterReceiver(this.mResultReceiver);
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case 1:
                handleIccFull();
                break;
            case 2:
                if (((AsyncResult) message.obj).exception != null) {
                    this.mReportMemoryStatusPending = true;
                    Rlog.v(TAG, "Memory status report to modem pending : mStorageAvailable = " + this.mStorageAvailable);
                } else {
                    this.mReportMemoryStatusPending = false;
                }
                break;
            case 3:
                if (this.mReportMemoryStatusPending) {
                    Rlog.v(TAG, "Sending pending memory status report : mStorageAvailable = " + this.mStorageAvailable);
                    this.mCi.reportSmsMemoryStatus(this.mStorageAvailable, obtainMessage(2));
                }
                break;
        }
    }

    private void createWakelock() {
        this.mWakeLock = ((PowerManager) this.mContext.getSystemService("power")).newWakeLock(1, TAG);
        this.mWakeLock.setReferenceCounted(true);
    }

    public void handleIccFull() {
        Intent intent = new Intent("android.provider.Telephony.SIM_FULL");
        intent.setComponent(SmsApplication.getDefaultSimFullApplication(this.mContext, false));
        this.mWakeLock.acquire(5000L);
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhone.getPhoneId());
        this.mContext.sendBroadcast(intent, "android.permission.RECEIVE_SMS");
    }

    public boolean isStorageAvailable() {
        return this.mStorageAvailable;
    }
}
