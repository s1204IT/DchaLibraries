package com.android.internal.telephony.cdma;

import android.os.Message;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.SMSDispatcher;
import com.android.internal.telephony.SmsDispatchersController;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.util.SMSDispatcherUtil;

public class CdmaSMSDispatcher extends SMSDispatcher {
    private static final String TAG = "CdmaSMSDispatcher";
    private static final boolean VDBG = false;

    public CdmaSMSDispatcher(Phone phone, SmsDispatchersController smsDispatchersController) {
        super(phone, smsDispatchersController);
        Rlog.d(TAG, "CdmaSMSDispatcher created");
    }

    @Override
    public String getFormat() {
        return "3gpp2";
    }

    public void sendStatusReportMessage(SmsMessage smsMessage) {
        sendMessage(obtainMessage(10, smsMessage));
    }

    @Override
    protected void handleStatusReport(Object obj) {
        if (obj instanceof SmsMessage) {
            handleCdmaStatusReport((SmsMessage) obj);
            return;
        }
        Rlog.e(TAG, "handleStatusReport() called for object type " + obj.getClass().getName());
    }

    @Override
    protected boolean shouldBlockSmsForEcbm() {
        return this.mPhone.isInEcm() && isCdmaMo() && !isIms();
    }

    @Override
    protected SmsMessageBase.SubmitPduBase getSubmitPdu(String str, String str2, String str3, boolean z, SmsHeader smsHeader, int i, int i2) {
        return SMSDispatcherUtil.getSubmitPduCdma(str, str2, str3, z, smsHeader, i);
    }

    @Override
    protected SmsMessageBase.SubmitPduBase getSubmitPdu(String str, String str2, int i, byte[] bArr, boolean z) {
        return SMSDispatcherUtil.getSubmitPduCdma(str, str2, i, bArr, z);
    }

    @Override
    protected GsmAlphabet.TextEncodingDetails calculateLength(CharSequence charSequence, boolean z) {
        return SMSDispatcherUtil.calculateLengthCdma(charSequence, z);
    }

    private void handleCdmaStatusReport(SmsMessage smsMessage) {
        int size = this.deliveryPendingList.size();
        for (int i = 0; i < size; i++) {
            SMSDispatcher.SmsTracker smsTracker = this.deliveryPendingList.get(i);
            if (smsTracker.mMessageRef == smsMessage.mMessageRef) {
                if (((Boolean) this.mSmsDispatchersController.handleSmsStatusReport(smsTracker, getFormat(), smsMessage.getPdu()).second).booleanValue()) {
                    this.deliveryPendingList.remove(i);
                    return;
                }
                return;
            }
        }
    }

    @Override
    public void sendSms(SMSDispatcher.SmsTracker smsTracker) {
        Rlog.d(TAG, "sendSms:  isIms()=" + isIms() + " mRetryCount=" + smsTracker.mRetryCount + " mImsRetry=" + smsTracker.mImsRetry + " mMessageRef=" + smsTracker.mMessageRef + " mUsesImsServiceForIms=" + smsTracker.mUsesImsServiceForIms + " SS=" + this.mPhone.getServiceState().getState());
        int state = this.mPhone.getServiceState().getState();
        boolean z = false;
        if (!isIms() && state != 0) {
            smsTracker.onFailed(this.mContext, getNotInServiceError(state), 0);
            return;
        }
        Message messageObtainMessage = obtainMessage(2, smsTracker);
        byte[] bArr = (byte[]) smsTracker.getData().get("pdu");
        int dataNetworkType = this.mPhone.getServiceState().getDataNetworkType();
        if ((dataNetworkType == 14 || (ServiceState.isLte(dataNetworkType) && !this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed())) && this.mPhone.getServiceState().getVoiceNetworkType() == 7 && ((GsmCdmaPhone) this.mPhone).mCT.mState != PhoneConstants.State.IDLE) {
            z = true;
        }
        if ((smsTracker.mImsRetry == 0 && !isIms()) || z || smsTracker.mUsesImsServiceForIms) {
            this.mCi.sendCdmaSms(bArr, messageObtainMessage);
        } else {
            this.mCi.sendImsCdmaSms(bArr, smsTracker.mImsRetry, smsTracker.mMessageRef, messageObtainMessage);
            smsTracker.mImsRetry++;
        }
    }
}
