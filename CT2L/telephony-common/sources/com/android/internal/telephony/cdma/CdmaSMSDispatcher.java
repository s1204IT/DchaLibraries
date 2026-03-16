package com.android.internal.telephony.cdma;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Message;
import android.os.Parcel;
import android.os.SystemProperties;
import android.provider.Telephony;
import android.telephony.Rlog;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.IccUtils;
import com.android.internal.telephony.ImsSMSDispatcher;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.SMSDispatcher;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsUsageMonitor;
import com.android.internal.telephony.VzwSmsFilter;
import com.android.internal.telephony.cdma.SmsMessage;
import com.android.internal.telephony.cdma.sms.CdmaSmsPlugin;
import com.android.internal.telephony.cdma.sms.UserData;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CdmaSMSDispatcher extends SMSDispatcher {
    private static final String TAG = "CdmaSMSDispatcher";
    private static final boolean VDBG = false;
    private ImsSMSDispatcher mImsDispatcher;

    public CdmaSMSDispatcher(PhoneBase phone, SmsUsageMonitor usageMonitor, ImsSMSDispatcher imsSMSDispatcher) {
        super(phone, usageMonitor, imsSMSDispatcher);
        this.mImsDispatcher = imsSMSDispatcher;
        CdmaSmsPlugin.createInstance(phone.getContext());
        Rlog.d(TAG, "CdmaSMSDispatcher created");
    }

    @Override
    protected String getFormat() {
        return android.telephony.SmsMessage.FORMAT_3GPP2;
    }

    void sendStatusReportMessage(SmsMessage sms) {
        sendMessage(obtainMessage(10, sms));
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1001:
                handleNewSmsOverIms((AsyncResult) msg.obj);
                break;
            default:
                super.handleMessage(msg);
                break;
        }
    }

    @Override
    protected void handleStatusReport(Object o) {
        if (o instanceof SmsMessage) {
            handleCdmaStatusReport((SmsMessage) o);
        } else {
            Rlog.e(TAG, "handleStatusReport() called for object type " + o.getClass().getName());
        }
    }

    void handleCdmaStatusReport(SmsMessage sms) {
        int count = this.deliveryPendingList.size();
        for (int i = 0; i < count; i++) {
            SMSDispatcher.SmsTracker tracker = this.deliveryPendingList.get(i);
            if (tracker.mMessageRef == sms.mMessageRef) {
                this.deliveryPendingList.remove(i);
                tracker.updateSentMessageStatus(this.mContext, 0);
                PendingIntent intent = tracker.mDeliveryIntent;
                Intent fillIn = new Intent();
                fillIn.putExtra("pdu", sms.getPdu());
                fillIn.putExtra(Telephony.CellBroadcasts.MESSAGE_FORMAT, getFormat());
                try {
                    intent.send(this.mContext, -1, fillIn);
                    return;
                } catch (PendingIntent.CanceledException e) {
                    return;
                }
            }
        }
    }

    private void handleNewSmsOverIms(AsyncResult ar) {
        int result;
        String ret = (String) ar.result;
        byte[] message = IccUtils.hexStringToBytes(ret);
        Parcel p = Parcel.obtain();
        p.unmarshall(message, 0, message.length);
        p.setDataPosition(0);
        android.telephony.SmsMessage sms = android.telephony.SmsMessage.newFromParcel(p);
        if (p != null) {
            p.recycle();
        }
        if (sms != null) {
            VzwSmsFilter filter = new VzwSmsFilter(this.mImsDispatcher.getCdmaInboundSmshandler(), this.mContext, sms, android.telephony.SmsMessage.FORMAT_3GPP2);
            if (filter.filter()) {
                if (this.mImsDispatcher.getImsSms() != null) {
                    this.mImsDispatcher.getImsSms().acknowledgeLastIncomingCdmaSms(true, 1, (Message) null);
                    return;
                }
                Rlog.e(TAG, "unknown exception happen");
                Intent intent = new Intent(Telephony.Sms.Intents.SMS_REJECTED_ACTION);
                intent.putExtra("result", 1);
                this.mContext.sendBroadcast(intent, "android.permission.RECEIVE_SMS");
                return;
            }
            try {
                if (sms.mWrappedSmsMessage == null) {
                    Rlog.e(TAG, "dispatchSmsMessage: message is null");
                    result = 2;
                } else if (this.mImsDispatcher.getCdmaInboundSmshandler() != null) {
                    result = this.mImsDispatcher.getCdmaInboundSmshandler().dispatchMessage(sms.mWrappedSmsMessage);
                } else {
                    Rlog.e(TAG, "getCdmaInboundSmshandler return null");
                    result = 2;
                }
            } catch (RuntimeException ex) {
                Rlog.e(TAG, "Exception dispatching message", ex);
                result = 2;
            }
            if (result != -1) {
                boolean handled = result == 1;
                if (!handled) {
                    Intent intent2 = new Intent(Telephony.Sms.Intents.SMS_REJECTED_ACTION);
                    intent2.putExtra("result", result);
                    this.mContext.sendBroadcast(intent2, "android.permission.RECEIVE_SMS");
                }
                if (this.mImsDispatcher.getImsSms() != null) {
                    this.mImsDispatcher.getImsSms().acknowledgeLastIncomingCdmaSms(true, 1, (Message) null);
                    return;
                }
                Rlog.e(TAG, "unknown exception happen");
                Intent intent3 = new Intent(Telephony.Sms.Intents.SMS_REJECTED_ACTION);
                intent3.putExtra("result", result);
                this.mContext.sendBroadcast(intent3, "android.permission.RECEIVE_SMS");
            }
        }
    }

    @Override
    protected void sendData(String destAddr, String scAddr, int destPort, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        SmsMessage.SubmitPdu pdu = SmsMessage.getSubmitPdu(scAddr, destAddr, destPort, data, deliveryIntent != null);
        SMSDispatcher.SmsTracker tracker = getSmsTracker(getSmsTrackerMap(destAddr, scAddr, destPort, data, pdu), sentIntent, deliveryIntent, getFormat(), null, false, null, false);
        String carrierPackage = getCarrierAppPackageName();
        if (carrierPackage != null) {
            Rlog.d(TAG, "Found carrier package.");
            SMSDispatcher.DataSmsSender smsSender = new SMSDispatcher.DataSmsSender(tracker);
            smsSender.sendSmsByCarrierApp(carrierPackage, new SMSDispatcher.SmsSenderCallback(smsSender));
        } else {
            Rlog.v(TAG, "No carrier package.");
            sendSubmitPdu(tracker);
        }
    }

    @Override
    protected void sendText(String destAddr, String scAddr, String text, PendingIntent sentIntent, PendingIntent deliveryIntent, Uri messageUri, String callingPkg) {
        SmsMessage.SubmitPdu pdu;
        if (this.mImsDispatcher.isIms()) {
            int encoding = this.mImsDispatcher.getMoSmsEncodingFromDataBase(android.telephony.SmsMessage.FORMAT_3GPP2);
            pdu = SmsMessage.getSubmitPdu(scAddr, destAddr, text, deliveryIntent != null, null, encoding);
        } else {
            pdu = SmsMessage.getSubmitPdu(scAddr, destAddr, text, deliveryIntent != null, (SmsHeader) null);
        }
        if (pdu != null) {
            SMSDispatcher.SmsTracker tracker = getSmsTracker(getSmsTrackerMap(destAddr, scAddr, text, pdu), sentIntent, deliveryIntent, getFormat(), messageUri, false, text, true);
            String carrierPackage = getCarrierAppPackageName();
            if (carrierPackage != null) {
                Rlog.d(TAG, "Found carrier package.");
                SMSDispatcher.TextSmsSender smsSender = new SMSDispatcher.TextSmsSender(tracker);
                smsSender.sendSmsByCarrierApp(carrierPackage, new SMSDispatcher.SmsSenderCallback(smsSender));
                return;
            } else {
                Rlog.v(TAG, "No carrier package.");
                sendSubmitPdu(tracker);
                return;
            }
        }
        Rlog.e(TAG, "CdmaSMSDispatcher.sendText(): getSubmitPdu() returned null");
    }

    @Override
    protected void injectSmsPdu(byte[] pdu, String format, PendingIntent receivedIntent) {
        throw new IllegalStateException("This method must be called only on ImsSMSDispatcher");
    }

    @Override
    protected GsmAlphabet.TextEncodingDetails calculateLength(CharSequence messageBody, boolean use7bitOnly) {
        return SmsMessage.calculateLength(messageBody, use7bitOnly, false);
    }

    @Override
    protected SMSDispatcher.SmsTracker getNewSubmitPduTracker(String destinationAddress, String scAddress, String message, SmsHeader smsHeader, int encoding, PendingIntent sentIntent, PendingIntent deliveryIntent, boolean lastPart, AtomicInteger unsentPartCount, AtomicBoolean anyPartFailed, Uri messageUri, String fullMessageText) {
        UserData uData = new UserData();
        uData.payloadStr = message;
        uData.userDataHeader = smsHeader;
        if (encoding == 1) {
            uData.msgEncoding = 9;
        } else {
            uData.msgEncoding = 4;
        }
        uData.msgEncodingSet = true;
        SmsMessage.SubmitPdu submitPdu = SmsMessage.getSubmitPdu(destinationAddress, uData, deliveryIntent != null && lastPart);
        return getSmsTracker(getSmsTrackerMap(destinationAddress, scAddress, message, submitPdu), sentIntent, deliveryIntent, getFormat(), unsentPartCount, anyPartFailed, messageUri, smsHeader, false, fullMessageText, true);
    }

    @Override
    protected void sendSubmitPdu(SMSDispatcher.SmsTracker tracker) {
        if (SystemProperties.getBoolean("ril.cdma.inecmmode", false)) {
            tracker.onFailed(this.mContext, 4, 0);
        } else {
            sendRawPdu(tracker);
        }
    }

    @Override
    protected void sendSms(SMSDispatcher.SmsTracker tracker) {
        HashMap<String, Object> map = tracker.mData;
        Rlog.d(TAG, "sendSms:  isIms()=" + isIms() + " mRetryCount=" + tracker.mRetryCount + " mImsRetry=" + tracker.mImsRetry + " mMessageRef=" + tracker.mMessageRef + " SS=" + this.mPhone.getServiceState().getState());
        sendSmsByPstn(tracker);
    }

    @Override
    protected void sendSmsByPstn(SMSDispatcher.SmsTracker tracker) {
        boolean imsSmsDisabled = false;
        int ss = this.mPhone.getServiceState().getState();
        if (!isIms() && ss != 0) {
            tracker.onFailed(this.mContext, getNotInServiceError(ss), 0);
            return;
        }
        Message reply = obtainMessage(2, tracker);
        byte[] pdu = (byte[]) tracker.mData.get("pdu");
        int currentDataNetwork = this.mPhone.getServiceState().getDataNetworkType();
        if ((currentDataNetwork == 14 || (currentDataNetwork == 13 && !this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed())) && this.mPhone.getServiceState().getVoiceNetworkType() == 7 && ((CDMAPhone) this.mPhone).mCT.mState != PhoneConstants.State.IDLE) {
            imsSmsDisabled = true;
        }
        if ((tracker.mImsRetry == 0 && !isIms()) || imsSmsDisabled) {
            this.mCi.sendCdmaSms(pdu, reply);
            return;
        }
        try {
            if (this.mImsDispatcher.getImsSms() != null) {
                int msgId = SystemProperties.getInt("persist.radio.cdma.msgid", 1);
                tracker.mMessageRef = msgId > 1 ? msgId - 1 : 65535;
                Rlog.d(TAG, "sendImsCdmaSms MessageRef =" + tracker.mMessageRef);
                this.mImsDispatcher.getImsSms().sendImsCdmaSms(pdu, tracker.mImsRetry, tracker.mMessageRef, reply);
                tracker.mImsRetry++;
                return;
            }
            Rlog.e(TAG, "sms over ims, mImsSms can't be null");
        } catch (Exception e) {
            Rlog.e(TAG, "sendImsCdmaSms over Ims error");
        }
    }
}
