package com.mediatek.internal.telephony;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Message;
import android.os.PowerManager;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.SMSDispatcher;
import com.android.internal.telephony.SmsDispatchersController;
import com.android.internal.telephony.SmsRawData;
import com.android.internal.telephony.SmsStorageMonitor;
import com.android.internal.telephony.SmsUsageMonitor;
import java.util.ArrayList;
import java.util.List;

public class MtkSmsDispatchersController extends SmsDispatchersController {
    private static final boolean ENG = "eng".equals(Build.TYPE);
    protected static final int EVENT_SMS_READY = 0;
    private static final int FORMAT_CS_CDMA = 3;
    private static final int FORMAT_CS_GSM = 2;
    private static final int FORMAT_IMS = 1;
    private static final int FORMAT_NOT_MATCH = 0;
    public static final String SELECT_BY_REFERENCE = "address=? AND reference_number=? AND count=? AND deleted=0 AND sub_id=?";
    private static final String TAG = "MtkSmsDispatchersController";
    private static final int WAKE_LOCK_TIMEOUT = 500;
    private boolean mSmsReady;
    private PowerManager.WakeLock mWakeLock;

    public MtkSmsDispatchersController(Phone phone, SmsStorageMonitor smsStorageMonitor, SmsUsageMonitor smsUsageMonitor) {
        super(phone, smsStorageMonitor, smsUsageMonitor);
        this.mSmsReady = false;
        updatePhoneObject(phone);
        createWakelock();
        this.mCi.registerForSmsReady(this, 0, null);
        Rlog.d(TAG, "MtkSmsDispatchersController created");
    }

    protected void sendData(String str, String str2, int i, int i2, byte[] bArr, PendingIntent pendingIntent, PendingIntent pendingIntent2) {
        if (this.mImsSmsDispatcher.isAvailable()) {
            ((MtkImsSmsDispatcher) this.mImsSmsDispatcher).sendData(str, str2, i, i2, bArr, pendingIntent, pendingIntent2);
        } else if (isCdmaMo()) {
            this.mCdmaDispatcher.sendData(str, str2, i, i2, bArr, pendingIntent, pendingIntent2);
        } else {
            this.mGsmDispatcher.sendData(str, str2, i, i2, bArr, pendingIntent, pendingIntent2);
        }
    }

    protected void sendMultipartData(String str, String str2, int i, ArrayList<SmsRawData> arrayList, ArrayList<PendingIntent> arrayList2, ArrayList<PendingIntent> arrayList3) {
        if (this.mImsSmsDispatcher.isAvailable()) {
            ((MtkImsSmsDispatcher) this.mImsSmsDispatcher).sendMultipartData(str, str2, i, arrayList, arrayList2, arrayList3);
        } else if (!isCdmaMo()) {
            this.mGsmDispatcher.sendMultipartData(str, str2, i, arrayList, arrayList2, arrayList3);
        }
    }

    public int copyTextMessageToIccCard(String str, String str2, List<String> list, int i, long j) {
        if (isCdmaMo()) {
            return this.mCdmaDispatcher.copyTextMessageToIccCard(str, str2, list, i, j);
        }
        return this.mGsmDispatcher.copyTextMessageToIccCard(str, str2, list, i, j);
    }

    protected void sendTextWithEncodingType(String str, String str2, String str3, int i, PendingIntent pendingIntent, PendingIntent pendingIntent2, Uri uri, String str4, boolean z, int i2, boolean z2, int i3) {
        if (this.mImsSmsDispatcher.isAvailable()) {
            ((MtkImsSmsDispatcher) this.mImsSmsDispatcher).sendTextWithEncodingType(str, str2, str3, i, pendingIntent, pendingIntent2, uri, str4, z, i2, z2, i3);
        } else if (isCdmaMo()) {
            this.mCdmaDispatcher.sendTextWithEncodingType(str, str2, str3, i, pendingIntent, pendingIntent2, uri, str4, z, i2, z2, i3);
        } else {
            this.mGsmDispatcher.sendTextWithEncodingType(str, str2, str3, i, pendingIntent, pendingIntent2, uri, str4, z, i2, z2, i3);
        }
    }

    protected void sendMultipartTextWithEncodingType(String str, String str2, ArrayList<String> arrayList, int i, ArrayList<PendingIntent> arrayList2, ArrayList<PendingIntent> arrayList3, Uri uri, String str3, boolean z, int i2, boolean z2, int i3) {
        if (this.mImsSmsDispatcher.isAvailable()) {
            ((MtkImsSmsDispatcher) this.mImsSmsDispatcher).sendMultipartTextWithEncodingType(str, str2, arrayList, i, arrayList2, arrayList3, uri, str3, z, i2, z2, i3);
        } else if (isCdmaMo()) {
            this.mCdmaDispatcher.sendMultipartTextWithEncodingType(str, str2, arrayList, i, arrayList2, arrayList3, uri, str3, z, i2, z2, i3);
        } else {
            this.mGsmDispatcher.sendMultipartTextWithEncodingType(str, str2, arrayList, i, arrayList2, arrayList3, uri, str3, z, i2, z2, i3);
        }
    }

    protected void handleIccFull() {
        if (!isCdmaMo()) {
            this.mGsmDispatcher.handleIccFull();
        }
    }

    protected void setSmsMemoryStatus(boolean z) {
        if (!isCdmaMo()) {
            this.mGsmDispatcher.setSmsMemoryStatus(z);
        }
    }

    public boolean isSmsReady() {
        return this.mSmsReady;
    }

    public void handleMessage(Message message) {
        if (message.what == 0) {
            Rlog.d(TAG, "SMS is ready, Phone: " + this.mPhone.getPhoneId());
            this.mSmsReady = true;
            notifySmsReady(this.mSmsReady);
            return;
        }
        super.handleMessage(message);
    }

    private void createWakelock() {
        this.mWakeLock = ((PowerManager) this.mPhone.getContext().getSystemService("power")).newWakeLock(1, "SmsCommonEventHelp");
        this.mWakeLock.setReferenceCounted(true);
    }

    private void notifySmsReady(boolean z) {
        Intent intent = new Intent("android.provider.Telephony.SMS_STATE_CHANGED");
        intent.putExtra("ready", z);
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhone.getPhoneId());
        intent.addFlags(16777216);
        intent.setComponent(null);
        this.mWakeLock.acquire(500L);
        this.mPhone.getContext().sendBroadcast(intent);
    }

    int isFormatMatch(SMSDispatcher.SmsTracker smsTracker, Phone phone) {
        if (ENG) {
            Rlog.d(TAG, "isFormatMatch, isIms " + isIms() + ", ims sms format " + getImsSmsFormat() + ", tracker format " + smsTracker.mFormat + ", Phone type " + phone.getPhoneType());
        }
        if (this.mImsSmsDispatcher.isAvailable() && smsTracker.mFormat.equals(this.mImsSmsDispatcher.getFormat())) {
            return 1;
        }
        if (smsTracker.mFormat.equals("3gpp2") && phone.getPhoneType() == 2) {
            return 3;
        }
        return (smsTracker.mFormat.equals("3gpp") && phone.getPhoneType() == 1) ? 2 : 0;
    }

    public void addToGsmDeliverPendingList(SMSDispatcher.SmsTracker smsTracker) {
        this.mGsmDispatcher.addToGsmDeliverPendingList(smsTracker);
    }
}
