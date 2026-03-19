package com.android.internal.telephony.gsm;

import android.os.AsyncResult;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.SMSDispatcher;
import com.android.internal.telephony.SmsDispatchersController;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.util.SMSDispatcherUtil;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

public class GsmSMSDispatcher extends SMSDispatcher {
    private static final int EVENT_NEW_SMS_STATUS_REPORT = 100;
    private static final String TAG = "GsmSMSDispatcher";
    protected GsmInboundSmsHandler mGsmInboundSmsHandler;
    protected AtomicReference<IccRecords> mIccRecords;
    protected AtomicReference<UiccCardApplication> mUiccApplication;
    protected UiccController mUiccController;

    public GsmSMSDispatcher(Phone phone, SmsDispatchersController smsDispatchersController, GsmInboundSmsHandler gsmInboundSmsHandler) {
        super(phone, smsDispatchersController);
        this.mUiccController = null;
        this.mIccRecords = new AtomicReference<>();
        this.mUiccApplication = new AtomicReference<>();
        this.mCi.setOnSmsStatus(this, 100, null);
        this.mGsmInboundSmsHandler = gsmInboundSmsHandler;
        this.mUiccController = UiccController.getInstance();
        this.mUiccController.registerForIccChanged(this, 15, null);
        Rlog.d(TAG, "GsmSMSDispatcher created");
    }

    @Override
    public void dispose() {
        super.dispose();
        this.mCi.unSetOnSmsStatus(this);
        this.mUiccController.unregisterForIccChanged(this);
    }

    @Override
    public String getFormat() {
        return "3gpp";
    }

    @Override
    public void handleMessage(Message message) {
        int i = message.what;
        if (i == 100) {
            handleStatusReport((AsyncResult) message.obj);
        }
        switch (i) {
            case 14:
                this.mGsmInboundSmsHandler.sendMessage(1, message.obj);
                break;
            case 15:
                onUpdateIccAvailability();
                break;
            default:
                super.handleMessage(message);
                break;
        }
    }

    @Override
    protected boolean shouldBlockSmsForEcbm() {
        return false;
    }

    @Override
    protected SmsMessageBase.SubmitPduBase getSubmitPdu(String str, String str2, String str3, boolean z, SmsHeader smsHeader, int i, int i2) {
        return SMSDispatcherUtil.getSubmitPduGsm(str, str2, str3, z, i2);
    }

    @Override
    protected SmsMessageBase.SubmitPduBase getSubmitPdu(String str, String str2, int i, byte[] bArr, boolean z) {
        return SMSDispatcherUtil.getSubmitPduGsm(str, str2, i, bArr, z);
    }

    @Override
    protected GsmAlphabet.TextEncodingDetails calculateLength(CharSequence charSequence, boolean z) {
        return SMSDispatcherUtil.calculateLengthGsm(charSequence, z);
    }

    private void handleStatusReport(AsyncResult asyncResult) {
        byte[] bArr = (byte[]) asyncResult.result;
        SmsMessage smsMessageNewFromCDS = SmsMessage.newFromCDS(bArr);
        if (smsMessageNewFromCDS != null) {
            int i = smsMessageNewFromCDS.mMessageRef;
            int i2 = 0;
            int size = this.deliveryPendingList.size();
            while (true) {
                if (i2 >= size) {
                    break;
                }
                SMSDispatcher.SmsTracker smsTracker = this.deliveryPendingList.get(i2);
                if (smsTracker.mMessageRef != i) {
                    i2++;
                } else if (((Boolean) this.mSmsDispatchersController.handleSmsStatusReport(smsTracker, getFormat(), bArr).second).booleanValue()) {
                    this.deliveryPendingList.remove(i2);
                }
            }
        }
        this.mCi.acknowledgeLastIncomingGsmSms(true, 1, null);
    }

    @Override
    protected void sendSms(SMSDispatcher.SmsTracker smsTracker) {
        HashMap<String, Object> data = smsTracker.getData();
        byte[] bArr = (byte[]) data.get("pdu");
        if (smsTracker.mRetryCount > 0) {
            Rlog.d(TAG, "sendSms:  mRetryCount=" + smsTracker.mRetryCount + " mMessageRef=" + smsTracker.mMessageRef + " SS=" + this.mPhone.getServiceState().getState());
            if ((bArr[0] & 1) == 1) {
                bArr[0] = (byte) (bArr[0] | 4);
                bArr[1] = (byte) smsTracker.mMessageRef;
            }
        }
        Rlog.d(TAG, "sendSms:  isIms()=" + isIms() + " mRetryCount=" + smsTracker.mRetryCount + " mImsRetry=" + smsTracker.mImsRetry + " mMessageRef=" + smsTracker.mMessageRef + " mUsesImsServiceForIms=" + smsTracker.mUsesImsServiceForIms + " SS=" + this.mPhone.getServiceState().getState());
        int state = this.mPhone.getServiceState().getState();
        if (!isIms() && state != 0) {
            smsTracker.onFailed(this.mContext, getNotInServiceError(state), 0);
            return;
        }
        byte[] bArr2 = (byte[]) data.get("smsc");
        Message messageObtainMessage = obtainMessage(2, smsTracker);
        if ((smsTracker.mImsRetry == 0 && !isIms()) || smsTracker.mUsesImsServiceForIms) {
            if (smsTracker.mRetryCount == 0 && smsTracker.mExpectMore) {
                this.mCi.sendSMSExpectMore(IccUtils.bytesToHexString(bArr2), IccUtils.bytesToHexString(bArr), messageObtainMessage);
                return;
            } else {
                this.mCi.sendSMS(IccUtils.bytesToHexString(bArr2), IccUtils.bytesToHexString(bArr), messageObtainMessage);
                return;
            }
        }
        this.mCi.sendImsGsmSms(IccUtils.bytesToHexString(bArr2), IccUtils.bytesToHexString(bArr), smsTracker.mImsRetry, smsTracker.mMessageRef, messageObtainMessage);
        smsTracker.mImsRetry++;
    }

    protected UiccCardApplication getUiccCardApplication() {
        Rlog.d(TAG, "GsmSMSDispatcher: subId = " + this.mPhone.getSubId() + " slotId = " + this.mPhone.getPhoneId());
        return this.mUiccController.getUiccCardApplication(this.mPhone.getPhoneId(), 1);
    }

    protected void onUpdateIccAvailability() {
        UiccCardApplication uiccCardApplication;
        UiccCardApplication uiccCardApplication2;
        if (this.mUiccController != null && (uiccCardApplication2 = this.mUiccApplication.get()) != (uiccCardApplication = getUiccCardApplication())) {
            if (uiccCardApplication2 != null) {
                Rlog.d(TAG, "Removing stale icc objects.");
                if (this.mIccRecords.get() != null) {
                    this.mIccRecords.get().unregisterForNewSms(this);
                }
                this.mIccRecords.set(null);
                this.mUiccApplication.set(null);
            }
            if (uiccCardApplication != null) {
                Rlog.d(TAG, "New Uicc application found");
                this.mUiccApplication.set(uiccCardApplication);
                this.mIccRecords.set(uiccCardApplication.getIccRecords());
                if (this.mIccRecords.get() != null) {
                    this.mIccRecords.get().registerForNewSms(this, 14, null);
                }
            }
        }
    }
}
