package com.android.internal.telephony.gsm;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Message;
import android.provider.Telephony;
import android.telephony.Rlog;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.ImsSMSDispatcher;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.SMSDispatcher;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsUsageMonitor;
import com.android.internal.telephony.VzwSmsFilter;
import com.android.internal.telephony.gsm.SmsMessage;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class GsmSMSDispatcher extends SMSDispatcher {
    public static final int EVENT_NEW_SMS_STATUS_REPORT = 100;
    private static final String TAG = "GsmSMSDispatcher";
    private static final boolean VDBG = false;
    private GsmInboundSmsHandler mGsmInboundSmsHandler;
    private AtomicReference<IccRecords> mIccRecords;
    private ImsSMSDispatcher mImsDispatcher;
    private AtomicReference<UiccCardApplication> mUiccApplication;
    protected UiccController mUiccController;

    public GsmSMSDispatcher(PhoneBase phone, SmsUsageMonitor usageMonitor, ImsSMSDispatcher imsSMSDispatcher, GsmInboundSmsHandler gsmInboundSmsHandler) {
        super(phone, usageMonitor, imsSMSDispatcher);
        this.mUiccController = null;
        this.mIccRecords = new AtomicReference<>();
        this.mUiccApplication = new AtomicReference<>();
        this.mImsDispatcher = imsSMSDispatcher;
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
    protected String getFormat() {
        return android.telephony.SmsMessage.FORMAT_3GPP;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 14:
                this.mGsmInboundSmsHandler.sendMessage(1, msg.obj);
                break;
            case 15:
                onUpdateIccAvailability();
                break;
            case 100:
                handleStatusReport((AsyncResult) msg.obj, false);
                break;
            case 1001:
                handleNewSmsOverIms((AsyncResult) msg.obj);
                break;
            case 1002:
                handleStatusReport((AsyncResult) msg.obj, true);
                break;
            default:
                super.handleMessage(msg);
                break;
        }
    }

    private void handleStatusReport(AsyncResult ar, boolean fromIms) {
        String pduString = (String) ar.result;
        SmsMessage sms = SmsMessage.newFromCDS(pduString);
        if (sms != null) {
            int tpStatus = sms.getStatus();
            int messageRef = sms.mMessageRef;
            int i = 0;
            int count = this.deliveryPendingList.size();
            while (true) {
                if (i >= count) {
                    break;
                }
                SMSDispatcher.SmsTracker tracker = this.deliveryPendingList.get(i);
                if (tracker.mMessageRef != messageRef) {
                    i++;
                } else {
                    if (tpStatus >= 64 || tpStatus < 32) {
                        this.deliveryPendingList.remove(i);
                        tracker.updateSentMessageStatus(this.mContext, tpStatus);
                    }
                    PendingIntent intent = tracker.mDeliveryIntent;
                    Intent fillIn = new Intent();
                    fillIn.putExtra("pdu", IccUtils.hexStringToBytes(pduString));
                    fillIn.putExtra(Telephony.CellBroadcasts.MESSAGE_FORMAT, getFormat());
                    try {
                        intent.send(this.mContext, -1, fillIn);
                    } catch (PendingIntent.CanceledException e) {
                    }
                }
            }
        }
        if (fromIms) {
            try {
                this.mImsDispatcher.getImsSms().acknowledgeLastIncomingGsmSms(true, 1, (Message) null);
            } catch (Exception e2) {
            }
        } else {
            this.mCi.acknowledgeLastIncomingGsmSms(true, 1, null);
        }
    }

    private void handleNewSmsOverIms(AsyncResult ar) {
        int result;
        String[] a = new String[2];
        a[1] = (String) ar.result;
        android.telephony.SmsMessage sms = android.telephony.SmsMessage.newFromCMT(a);
        if (sms != null) {
            VzwSmsFilter filter = new VzwSmsFilter(this.mGsmInboundSmsHandler, this.mContext, sms, android.telephony.SmsMessage.FORMAT_3GPP);
            if (filter.filter()) {
                if (this.mImsDispatcher.getImsSms() != null) {
                    this.mImsDispatcher.getImsSms().acknowledgeLastIncomingGsmSms(true, 1, (Message) null);
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
                } else {
                    result = this.mGsmInboundSmsHandler.dispatchMessage(sms.mWrappedSmsMessage);
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
                    this.mImsDispatcher.getImsSms().acknowledgeLastIncomingGsmSms(true, 1, (Message) null);
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
        if (pdu != null) {
            SMSDispatcher.SmsTracker tracker = getSmsTracker(getSmsTrackerMap(destAddr, scAddr, destPort, data, pdu), sentIntent, deliveryIntent, getFormat(), null, false, null, false);
            String carrierPackage = getCarrierAppPackageName();
            if (carrierPackage != null) {
                Rlog.d(TAG, "Found carrier package.");
                SMSDispatcher.DataSmsSender smsSender = new SMSDispatcher.DataSmsSender(tracker);
                smsSender.sendSmsByCarrierApp(carrierPackage, new SMSDispatcher.SmsSenderCallback(smsSender));
                return;
            } else {
                Rlog.v(TAG, "No carrier package.");
                sendRawPdu(tracker);
                return;
            }
        }
        Rlog.e(TAG, "GsmSMSDispatcher.sendData(): getSubmitPdu() returned null");
    }

    @Override
    protected void sendText(String destAddr, String scAddr, String text, PendingIntent sentIntent, PendingIntent deliveryIntent, Uri messageUri, String callingPkg) {
        SmsMessage.SubmitPdu pdu;
        if (this.mImsDispatcher.isIms()) {
            int encoding = this.mImsDispatcher.getMoSmsEncodingFromDataBase(android.telephony.SmsMessage.FORMAT_3GPP);
            pdu = SmsMessage.getSubmitPdu(scAddr, destAddr, text, deliveryIntent != null, null, encoding, 0, 0);
        } else {
            pdu = SmsMessage.getSubmitPdu(scAddr, destAddr, text, deliveryIntent != null);
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
                sendRawPdu(tracker);
                return;
            }
        }
        Rlog.e(TAG, "GsmSMSDispatcher.sendText(): getSubmitPdu() returned null");
    }

    @Override
    protected void injectSmsPdu(byte[] pdu, String format, PendingIntent receivedIntent) {
        throw new IllegalStateException("This method must be called only on ImsSMSDispatcher");
    }

    @Override
    protected GsmAlphabet.TextEncodingDetails calculateLength(CharSequence messageBody, boolean use7bitOnly) {
        return SmsMessage.calculateLength(messageBody, use7bitOnly);
    }

    @Override
    protected SMSDispatcher.SmsTracker getNewSubmitPduTracker(String destinationAddress, String scAddress, String message, SmsHeader smsHeader, int encoding, PendingIntent sentIntent, PendingIntent deliveryIntent, boolean lastPart, AtomicInteger unsentPartCount, AtomicBoolean anyPartFailed, Uri messageUri, String fullMessageText) {
        SmsMessage.SubmitPdu pdu = SmsMessage.getSubmitPdu(scAddress, destinationAddress, message, deliveryIntent != null, SmsHeader.toByteArray(smsHeader), encoding, smsHeader.languageTable, smsHeader.languageShiftTable);
        if (pdu != null) {
            return getSmsTracker(getSmsTrackerMap(destinationAddress, scAddress, message, pdu), sentIntent, deliveryIntent, getFormat(), unsentPartCount, anyPartFailed, messageUri, smsHeader, !lastPart, fullMessageText, true);
        }
        Rlog.e(TAG, "GsmSMSDispatcher.sendNewSubmitPdu(): getSubmitPdu() returned null");
        return null;
    }

    @Override
    protected void sendSubmitPdu(SMSDispatcher.SmsTracker tracker) {
        sendRawPdu(tracker);
    }

    @Override
    protected void sendSms(SMSDispatcher.SmsTracker tracker) {
        HashMap<String, Object> map = tracker.mData;
        byte[] pdu = (byte[]) map.get("pdu");
        if (tracker.mRetryCount > 0) {
            Rlog.d(TAG, "sendSms:  mRetryCount=" + tracker.mRetryCount + " mMessageRef=" + tracker.mMessageRef + " SS=" + this.mPhone.getServiceState().getState());
            if ((pdu[0] & 1) == 1) {
                pdu[0] = (byte) (pdu[0] | 4);
                pdu[1] = (byte) tracker.mMessageRef;
            }
        }
        Rlog.d(TAG, "sendSms:  isIms()=" + isIms() + " mRetryCount=" + tracker.mRetryCount + " mImsRetry=" + tracker.mImsRetry + " mMessageRef=" + tracker.mMessageRef + " SS=" + this.mPhone.getServiceState().getState());
        sendSmsByPstn(tracker);
    }

    @Override
    protected void sendSmsByPstn(SMSDispatcher.SmsTracker tracker) {
        int ss = this.mPhone.getServiceState().getState();
        if (!isIms() && ss != 0) {
            tracker.onFailed(this.mContext, getNotInServiceError(ss), 0);
            return;
        }
        HashMap<String, Object> map = tracker.mData;
        byte[] smsc = (byte[]) map.get("smsc");
        byte[] pdu = (byte[]) map.get("pdu");
        Message reply = obtainMessage(2, tracker);
        if (tracker.mImsRetry == 0 && !isIms()) {
            if (tracker.mRetryCount > 0 && (pdu[0] & 1) == 1) {
                pdu[0] = (byte) (pdu[0] | 4);
                pdu[1] = (byte) tracker.mMessageRef;
            }
            if (tracker.mRetryCount == 0 && tracker.mExpectMore) {
                this.mCi.sendSMSExpectMore(IccUtils.bytesToHexString(smsc), IccUtils.bytesToHexString(pdu), reply);
                return;
            } else {
                this.mCi.sendSMS(IccUtils.bytesToHexString(smsc), IccUtils.bytesToHexString(pdu), reply);
                return;
            }
        }
        try {
            if (this.mImsDispatcher.getImsSms() != null) {
                this.mImsDispatcher.getImsSms().sendImsGsmSms(IccUtils.bytesToHexString(smsc), IccUtils.bytesToHexString(pdu), tracker.mImsRetry, tracker.mMessageRef, reply);
                tracker.mImsRetry++;
            } else {
                Rlog.e(TAG, "sms over ims, mImsSms can't be null");
            }
        } catch (Exception e) {
            Rlog.e(TAG, "sendImsGsmSms over Ims error");
        }
    }

    protected UiccCardApplication getUiccCardApplication() {
        Rlog.d(TAG, "GsmSMSDispatcher: subId = " + this.mPhone.getSubId() + " slotId = " + this.mPhone.getPhoneId());
        return this.mUiccController.getUiccCardApplication(this.mPhone.getPhoneId(), 1);
    }

    private void onUpdateIccAvailability() {
        UiccCardApplication newUiccApplication;
        UiccCardApplication app;
        if (this.mUiccController != null && (app = this.mUiccApplication.get()) != (newUiccApplication = getUiccCardApplication())) {
            if (app != null) {
                Rlog.d(TAG, "Removing stale icc objects.");
                if (this.mIccRecords.get() != null) {
                    this.mIccRecords.get().unregisterForNewSms(this);
                }
                this.mIccRecords.set(null);
                this.mUiccApplication.set(null);
            }
            if (newUiccApplication != null) {
                Rlog.d(TAG, "New Uicc application found");
                this.mUiccApplication.set(newUiccApplication);
                this.mIccRecords.set(newUiccApplication.getIccRecords());
                if (this.mIccRecords.get() != null) {
                    this.mIccRecords.get().registerForNewSms(this, 14, null);
                }
            }
        }
    }
}
