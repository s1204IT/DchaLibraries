package com.android.internal.telephony;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telephony.Rlog;
import android.telephony.SmsMessage;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.SMSDispatcher;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.cdma.CdmaInboundSmsHandler;
import com.android.internal.telephony.cdma.CdmaSMSDispatcher;
import com.android.internal.telephony.cdma.SmsMessage;
import com.android.internal.telephony.gsm.GsmInboundSmsHandler;
import java.util.ArrayList;
import java.util.HashMap;

public class SmsDispatchersController extends Handler {
    private static final int EVENT_IMS_STATE_CHANGED = 12;
    private static final int EVENT_IMS_STATE_DONE = 13;
    private static final int EVENT_RADIO_ON = 11;
    private static final String TAG = "SmsDispatchersController";
    protected SMSDispatcher mCdmaDispatcher;
    protected CdmaInboundSmsHandler mCdmaInboundSmsHandler;
    protected final CommandsInterface mCi;
    protected final Context mContext;
    protected SMSDispatcher mGsmDispatcher;
    protected GsmInboundSmsHandler mGsmInboundSmsHandler;
    protected ImsSmsDispatcher mImsSmsDispatcher;
    protected Phone mPhone;
    private final SmsUsageMonitor mUsageMonitor;
    private boolean mIms = false;
    private String mImsSmsFormat = "unknown";

    public interface SmsInjectionCallback {
        void onSmsInjectedResult(int i);
    }

    public SmsDispatchersController(Phone phone, SmsStorageMonitor smsStorageMonitor, SmsUsageMonitor smsUsageMonitor) {
        Rlog.d(TAG, "SmsDispatchersController created");
        this.mContext = phone.getContext();
        this.mUsageMonitor = smsUsageMonitor;
        this.mCi = phone.mCi;
        this.mPhone = phone;
        this.mImsSmsDispatcher = TelephonyComponentFactory.getInstance().makeImsSmsDispatcher(phone, this);
        this.mCdmaDispatcher = TelephonyComponentFactory.getInstance().makeCdmaSMSDispatcher(phone, this);
        this.mGsmInboundSmsHandler = TelephonyComponentFactory.getInstance().makeGsmInboundSmsHandler(phone.getContext(), smsStorageMonitor, phone);
        this.mCdmaInboundSmsHandler = CdmaInboundSmsHandler.makeInboundSmsHandler(phone.getContext(), smsStorageMonitor, phone, (CdmaSMSDispatcher) this.mCdmaDispatcher);
        this.mGsmDispatcher = TelephonyComponentFactory.getInstance().makeGsmSMSDispatcher(phone, this, this.mGsmInboundSmsHandler);
        TelephonyComponentFactory.getInstance().makeSmsBroadcastUndelivered(phone.getContext(), this.mGsmInboundSmsHandler, this.mCdmaInboundSmsHandler);
        InboundSmsHandler.registerNewMessageNotificationActionHandler(phone.getContext());
        this.mCi.registerForOn(this, 11, null);
        this.mCi.registerForImsNetworkStateChanged(this, 12, null);
    }

    protected void updatePhoneObject(Phone phone) {
        Rlog.d(TAG, "In IMS updatePhoneObject ");
        this.mCdmaDispatcher.updatePhoneObject(phone);
        this.mGsmDispatcher.updatePhoneObject(phone);
        this.mGsmInboundSmsHandler.updatePhoneObject(phone);
        this.mCdmaInboundSmsHandler.updatePhoneObject(phone);
    }

    public void dispose() {
        this.mCi.unregisterForOn(this);
        this.mCi.unregisterForImsNetworkStateChanged(this);
        this.mGsmDispatcher.dispose();
        this.mCdmaDispatcher.dispose();
        this.mGsmInboundSmsHandler.dispose();
        this.mCdmaInboundSmsHandler.dispose();
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case 11:
            case 12:
                this.mCi.getImsRegistrationState(obtainMessage(13));
                break;
            case 13:
                AsyncResult asyncResult = (AsyncResult) message.obj;
                if (asyncResult.exception == null) {
                    updateImsInfo(asyncResult);
                } else {
                    Rlog.e(TAG, "IMS State query failed with exp " + asyncResult.exception);
                }
                break;
            default:
                if (isCdmaMo()) {
                    this.mCdmaDispatcher.handleMessage(message);
                } else {
                    this.mGsmDispatcher.handleMessage(message);
                }
                break;
        }
    }

    private void setImsSmsFormat(int i) {
        switch (i) {
            case 1:
                this.mImsSmsFormat = "3gpp";
                break;
            case 2:
                this.mImsSmsFormat = "3gpp2";
                break;
            default:
                this.mImsSmsFormat = "unknown";
                break;
        }
    }

    private void updateImsInfo(AsyncResult asyncResult) {
        int[] iArr = (int[]) asyncResult.result;
        boolean z = true;
        setImsSmsFormat(iArr[1]);
        if (iArr[0] != 1 || "unknown".equals(this.mImsSmsFormat)) {
            z = false;
        }
        this.mIms = z;
        Rlog.d(TAG, "IMS registration state: " + this.mIms + " format: " + this.mImsSmsFormat);
    }

    @VisibleForTesting
    public void injectSmsPdu(byte[] bArr, String str, SmsInjectionCallback smsInjectionCallback) {
        injectSmsPdu(SmsMessage.createFromPdu(bArr, str), str, smsInjectionCallback, false);
    }

    @VisibleForTesting
    public void injectSmsPdu(SmsMessage smsMessage, String str, SmsInjectionCallback smsInjectionCallback, boolean z) {
        Rlog.d(TAG, "SmsDispatchersController:injectSmsPdu");
        try {
            if (smsMessage == null) {
                Rlog.e(TAG, "injectSmsPdu: createFromPdu returned null");
                smsInjectionCallback.onSmsInjectedResult(2);
                return;
            }
            if (!z && smsMessage.getMessageClass() != SmsMessage.MessageClass.CLASS_1) {
                Rlog.e(TAG, "injectSmsPdu: not class 1");
                smsInjectionCallback.onSmsInjectedResult(2);
                return;
            }
            AsyncResult asyncResult = new AsyncResult(smsInjectionCallback, smsMessage, (Throwable) null);
            if (str.equals("3gpp")) {
                Rlog.i(TAG, "SmsDispatchersController:injectSmsText Sending msg=" + smsMessage + ", format=" + str + "to mGsmInboundSmsHandler");
                this.mGsmInboundSmsHandler.sendMessage(8, asyncResult);
                return;
            }
            if (str.equals("3gpp2")) {
                Rlog.i(TAG, "SmsDispatchersController:injectSmsText Sending msg=" + smsMessage + ", format=" + str + "to mCdmaInboundSmsHandler");
                this.mCdmaInboundSmsHandler.sendMessage(8, asyncResult);
                return;
            }
            Rlog.e(TAG, "Invalid pdu format: " + str);
            smsInjectionCallback.onSmsInjectedResult(2);
        } catch (Exception e) {
            Rlog.e(TAG, "injectSmsPdu failed: ", e);
            smsInjectionCallback.onSmsInjectedResult(2);
        }
    }

    public void sendRetrySms(SMSDispatcher.SmsTracker smsTracker) {
        SmsMessage.SubmitPdu submitPdu;
        String str = smsTracker.mFormat;
        String format = (2 == this.mPhone.getPhoneType() ? this.mCdmaDispatcher : this.mGsmDispatcher).getFormat();
        if (str.equals(format)) {
            if (isCdmaFormat(format)) {
                Rlog.d(TAG, "old format matched new format (cdma)");
                this.mCdmaDispatcher.sendSms(smsTracker);
                return;
            } else {
                Rlog.d(TAG, "old format matched new format (gsm)");
                this.mGsmDispatcher.sendSms(smsTracker);
                return;
            }
        }
        HashMap<String, Object> data = smsTracker.getData();
        if (!data.containsKey("scAddr") || !data.containsKey("destAddr") || (!data.containsKey("text") && (!data.containsKey("data") || !data.containsKey("destPort")))) {
            Rlog.e(TAG, "sendRetrySms failed to re-encode per missing fields!");
            smsTracker.onFailed(this.mContext, 1, 0);
            return;
        }
        String str2 = (String) data.get("scAddr");
        String str3 = (String) data.get("destAddr");
        SmsMessage.SubmitPdu submitPdu2 = null;
        if (data.containsKey("text")) {
            Rlog.d(TAG, "sms failed was text");
            String str4 = (String) data.get("text");
            if (isCdmaFormat(format)) {
                Rlog.d(TAG, "old format (gsm) ==> new format (cdma)");
                submitPdu = com.android.internal.telephony.cdma.SmsMessage.getSubmitPdu(str2, str3, str4, smsTracker.mDeliveryIntent != null, (SmsHeader) null);
            } else {
                Rlog.d(TAG, "old format (cdma) ==> new format (gsm)");
                submitPdu = com.android.internal.telephony.gsm.SmsMessage.getSubmitPdu(str2, str3, str4, smsTracker.mDeliveryIntent != null, (byte[]) null);
            }
            submitPdu2 = submitPdu;
        } else if (data.containsKey("data")) {
            Rlog.d(TAG, "sms failed was data");
            byte[] bArr = (byte[]) data.get("data");
            Integer num = (Integer) data.get("destPort");
            if (isCdmaFormat(format)) {
                Rlog.d(TAG, "old format (gsm) ==> new format (cdma)");
                submitPdu2 = com.android.internal.telephony.cdma.SmsMessage.getSubmitPdu(str2, str3, num.intValue(), bArr, smsTracker.mDeliveryIntent != null);
            } else {
                Rlog.d(TAG, "old format (cdma) ==> new format (gsm)");
                submitPdu2 = com.android.internal.telephony.gsm.SmsMessage.getSubmitPdu(str2, str3, num.intValue(), bArr, smsTracker.mDeliveryIntent != null);
            }
        }
        data.put("smsc", ((SmsMessageBase.SubmitPduBase) submitPdu2).encodedScAddress);
        data.put("pdu", ((SmsMessageBase.SubmitPduBase) submitPdu2).encodedMessage);
        SMSDispatcher sMSDispatcher = isCdmaFormat(format) ? this.mCdmaDispatcher : this.mGsmDispatcher;
        smsTracker.mFormat = sMSDispatcher.getFormat();
        sMSDispatcher.sendSms(smsTracker);
    }

    public boolean isIms() {
        return this.mIms;
    }

    public String getImsSmsFormat() {
        return this.mImsSmsFormat;
    }

    protected boolean isCdmaMo() {
        if (isIms()) {
            return isCdmaFormat(this.mImsSmsFormat);
        }
        return 2 == this.mPhone.getPhoneType();
    }

    public boolean isCdmaFormat(String str) {
        return this.mCdmaDispatcher.getFormat().equals(str);
    }

    protected void sendData(String str, String str2, int i, byte[] bArr, PendingIntent pendingIntent, PendingIntent pendingIntent2) {
        if (this.mImsSmsDispatcher.isAvailable()) {
            this.mImsSmsDispatcher.sendData(str, str2, i, bArr, pendingIntent, pendingIntent2);
        } else if (isCdmaMo()) {
            this.mCdmaDispatcher.sendData(str, str2, i, bArr, pendingIntent, pendingIntent2);
        } else {
            this.mGsmDispatcher.sendData(str, str2, i, bArr, pendingIntent, pendingIntent2);
        }
    }

    public void sendText(String str, String str2, String str3, PendingIntent pendingIntent, PendingIntent pendingIntent2, Uri uri, String str4, boolean z, int i, boolean z2, int i2) {
        if (this.mImsSmsDispatcher.isAvailable()) {
            this.mImsSmsDispatcher.sendText(str, str2, str3, pendingIntent, pendingIntent2, uri, str4, z, -1, false, -1);
        } else if (isCdmaMo()) {
            this.mCdmaDispatcher.sendText(str, str2, str3, pendingIntent, pendingIntent2, uri, str4, z, i, z2, i2);
        } else {
            this.mGsmDispatcher.sendText(str, str2, str3, pendingIntent, pendingIntent2, uri, str4, z, i, z2, i2);
        }
    }

    protected void sendMultipartText(String str, String str2, ArrayList<String> arrayList, ArrayList<PendingIntent> arrayList2, ArrayList<PendingIntent> arrayList3, Uri uri, String str3, boolean z, int i, boolean z2, int i2) {
        if (this.mImsSmsDispatcher.isAvailable()) {
            this.mImsSmsDispatcher.sendMultipartText(str, str2, arrayList, arrayList2, arrayList3, uri, str3, z, -1, false, -1);
        } else if (isCdmaMo()) {
            this.mCdmaDispatcher.sendMultipartText(str, str2, arrayList, arrayList2, arrayList3, uri, str3, z, i, z2, i2);
        } else {
            this.mGsmDispatcher.sendMultipartText(str, str2, arrayList, arrayList2, arrayList3, uri, str3, z, i, z2, i2);
        }
    }

    public int getPremiumSmsPermission(String str) {
        return this.mUsageMonitor.getPremiumSmsPermission(str);
    }

    public void setPremiumSmsPermission(String str, int i) {
        this.mUsageMonitor.setPremiumSmsPermission(str, i);
    }

    public SmsUsageMonitor getUsageMonitor() {
        return this.mUsageMonitor;
    }

    public Pair<Boolean, Boolean> handleSmsStatusReport(SMSDispatcher.SmsTracker smsTracker, String str, byte[] bArr) {
        if (isCdmaFormat(str)) {
            return handleCdmaStatusReport(smsTracker, str, bArr);
        }
        return handleGsmStatusReport(smsTracker, str, bArr);
    }

    private Pair<Boolean, Boolean> handleCdmaStatusReport(SMSDispatcher.SmsTracker smsTracker, String str, byte[] bArr) {
        smsTracker.updateSentMessageStatus(this.mContext, 0);
        return new Pair<>(Boolean.valueOf(triggerDeliveryIntent(smsTracker, str, bArr)), true);
    }

    private Pair<Boolean, Boolean> handleGsmStatusReport(SMSDispatcher.SmsTracker smsTracker, String str, byte[] bArr) {
        boolean zTriggerDeliveryIntent;
        com.android.internal.telephony.gsm.SmsMessage smsMessageNewFromCDS = com.android.internal.telephony.gsm.SmsMessage.newFromCDS(bArr);
        boolean z = false;
        if (smsMessageNewFromCDS != null) {
            int status = smsMessageNewFromCDS.getStatus();
            if (status >= 64 || status < 32) {
                smsTracker.updateSentMessageStatus(this.mContext, status);
                z = true;
            }
            zTriggerDeliveryIntent = triggerDeliveryIntent(smsTracker, str, bArr);
        } else {
            zTriggerDeliveryIntent = false;
        }
        return new Pair<>(Boolean.valueOf(zTriggerDeliveryIntent), Boolean.valueOf(z));
    }

    private boolean triggerDeliveryIntent(SMSDispatcher.SmsTracker smsTracker, String str, byte[] bArr) {
        PendingIntent pendingIntent = smsTracker.mDeliveryIntent;
        Intent intent = new Intent();
        intent.putExtra("pdu", bArr);
        intent.putExtra("format", str);
        try {
            pendingIntent.send(this.mContext, -1, intent);
            return true;
        } catch (PendingIntent.CanceledException e) {
            return false;
        }
    }
}
