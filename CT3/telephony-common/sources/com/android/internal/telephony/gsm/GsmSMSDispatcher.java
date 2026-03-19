package com.android.internal.telephony.gsm;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Message;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.SmsManager;
import com.android.internal.telephony.CallFailCause;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.ImsSMSDispatcher;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.SMSDispatcher;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsRawData;
import com.android.internal.telephony.SmsUsageMonitor;
import com.android.internal.telephony.gsm.SmsMessage;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.google.android.mms.pdu.CharacterSets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class GsmSMSDispatcher extends SMSDispatcher {
    private static final int EVENT_NEW_SMS_STATUS_REPORT = 100;
    private static final String TAG = "GsmSMSDispatcher";
    private static final boolean VDBG = false;
    private GsmInboundSmsHandler mGsmInboundSmsHandler;
    private AtomicReference<IccRecords> mIccRecords;
    private AtomicReference<UiccCardApplication> mUiccApplication;
    protected UiccController mUiccController;

    public GsmSMSDispatcher(Phone phone, SmsUsageMonitor usageMonitor, ImsSMSDispatcher imsSMSDispatcher, GsmInboundSmsHandler gsmInboundSmsHandler) {
        super(phone, usageMonitor, imsSMSDispatcher);
        this.mUiccController = null;
        this.mIccRecords = new AtomicReference<>();
        this.mUiccApplication = new AtomicReference<>();
        this.mCi.setOnSmsStatus(this, 100, null);
        this.mGsmInboundSmsHandler = gsmInboundSmsHandler;
        this.mUiccController = UiccController.getInstance();
        Integer phoneId = new Integer(this.mPhone.getPhoneId());
        this.mUiccController.registerForIccChanged(this, 15, phoneId);
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
                Integer phoneId = getUiccControllerPhoneId(msg);
                if (phoneId.intValue() != this.mPhone.getPhoneId()) {
                    Rlog.d(TAG, "Wrong phone id event coming, PhoneId: " + phoneId);
                } else {
                    onUpdateIccAvailability();
                }
                break;
            case 100:
                handleStatusReport((AsyncResult) msg.obj);
                break;
            default:
                super.handleMessage(msg);
                break;
        }
    }

    private void handleStatusReport(AsyncResult ar) {
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
                    fillIn.putExtra("format", getFormat());
                    try {
                        intent.send(this.mContext, -1, fillIn);
                    } catch (PendingIntent.CanceledException e) {
                    }
                }
            }
        }
        this.mCi.acknowledgeLastIncomingGsmSms(true, 1, null);
    }

    @Override
    protected void sendData(String destAddr, String scAddr, int destPort, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        if (isDmLock) {
            Rlog.d(TAG, "DM status: lock-on");
            return;
        }
        SmsMessage.SubmitPdu pdu = SmsMessage.getSubmitPdu(scAddr, destAddr, destPort, data, deliveryIntent != null);
        if (pdu != null) {
            SMSDispatcher.SmsTracker tracker = getSmsTracker(getSmsTrackerMap(destAddr, scAddr, destPort, data, pdu), sentIntent, deliveryIntent, getFormat(), null, false, null, false, true);
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
    public void sendText(String destAddr, String scAddr, String text, PendingIntent sentIntent, PendingIntent deliveryIntent, Uri messageUri, String callingPkg, boolean persistMessage) {
        if (isDmLock) {
            Rlog.d(TAG, "DM status: lock-on");
            return;
        }
        SmsMessage.SubmitPdu pdu = SmsMessage.getSubmitPdu(scAddr, destAddr, text, deliveryIntent != null);
        if (pdu != null) {
            SMSDispatcher.SmsTracker tracker = getSmsTracker(getSmsTrackerMap(destAddr, scAddr, text, pdu), sentIntent, deliveryIntent, getFormat(), messageUri, false, text, true, persistMessage);
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
            return getSmsTracker(getSmsTrackerMap(destinationAddress, scAddress, message, pdu), sentIntent, deliveryIntent, getFormat(), unsentPartCount, anyPartFailed, messageUri, smsHeader, !lastPart, fullMessageText, true, true);
        }
        Rlog.e(TAG, "GsmSMSDispatcher.sendNewSubmitPdu(): getSubmitPdu() returned null");
        return null;
    }

    @Override
    protected void sendSubmitPdu(SMSDispatcher.SmsTracker tracker) {
        sendRawPdu(tracker);
    }

    @Override
    protected SMSDispatcher.SmsTracker getNewSubmitPduTracker(String destinationAddress, String scAddress, String message, SmsHeader smsHeader, int encoding, PendingIntent sentIntent, PendingIntent deliveryIntent, boolean lastPart, AtomicInteger unsentPartCount, AtomicBoolean anyPartFailed, Uri messageUri, String fullMessageText, int validityPeriod) {
        SmsMessage.SubmitPdu pdu = SmsMessage.getSubmitPdu(scAddress, destinationAddress, message, deliveryIntent != null, SmsHeader.toByteArray(smsHeader), encoding, smsHeader.languageTable, smsHeader.languageShiftTable, validityPeriod);
        if (pdu != null) {
            return getSmsTracker(getSmsTrackerMap(destinationAddress, scAddress, message, pdu), sentIntent, deliveryIntent, getFormat(), unsentPartCount, anyPartFailed, messageUri, smsHeader, !lastPart, fullMessageText, true, true);
        }
        Rlog.e(TAG, "GsmSMSDispatcher.getNewSubmitPduTracker(): getSubmitPdu() returned null");
        return null;
    }

    @Override
    protected void sendSms(SMSDispatcher.SmsTracker tracker) {
        boolean isReadySend;
        HashMap<String, Object> map = tracker.getData();
        byte[] pdu = (byte[]) map.get("pdu");
        synchronized (this.mSTrackersQueue) {
            if (this.mSTrackersQueue.isEmpty() || this.mSTrackersQueue.get(0) != tracker) {
                Rlog.d(TAG, "Add tracker into the list: " + tracker);
                this.mSTrackersQueue.add(tracker);
            }
            isReadySend = this.mSTrackersQueue.get(0) == tracker;
        }
        if (!isReadySend) {
            Rlog.d(TAG, "There is another tracker in-queue and is sending");
            return;
        }
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
        if (!isIms() && ss != 0 && !this.mTelephonyManager.isWifiCallingAvailable()) {
            tracker.onFailed(this.mContext, getNotInServiceError(ss), 0);
            Message delay = obtainMessage(107, tracker);
            sendMessageDelayed(delay, 10L);
            return;
        }
        HashMap<String, Object> map = tracker.getData();
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
        this.mCi.sendImsGsmSms(IccUtils.bytesToHexString(smsc), IccUtils.bytesToHexString(pdu), tracker.mImsRetry, tracker.mMessageRef, reply);
        tracker.mImsRetry++;
    }

    protected UiccCardApplication getUiccCardApplication() {
        Rlog.d(TAG, "GsmSMSDispatcher: subId = " + this.mPhone.getSubId() + " slotId = " + this.mPhone.getPhoneId());
        return this.mUiccController.getUiccCardApplication(this.mPhone.getPhoneId(), 1);
    }

    private void onUpdateIccAvailability() {
        UiccCardApplication newUiccApplication;
        UiccCardApplication app;
        if (this.mUiccController == null || (app = this.mUiccApplication.get()) == (newUiccApplication = getUiccCardApplication())) {
            return;
        }
        if (app != null) {
            Rlog.d(TAG, "Removing stale icc objects.");
            if (this.mIccRecords.get() != null) {
                this.mIccRecords.get().unregisterForNewSms(this);
            }
            this.mIccRecords.set(null);
            this.mUiccApplication.set(null);
        }
        if (newUiccApplication == null) {
            return;
        }
        Rlog.d(TAG, "New Uicc application found");
        this.mUiccApplication.set(newUiccApplication);
        this.mIccRecords.set(newUiccApplication.getIccRecords());
        if (this.mIccRecords.get() == null) {
            return;
        }
        this.mIccRecords.get().registerForNewSms(this, 14, null);
    }

    private Integer getUiccControllerPhoneId(Message msg) {
        Integer phoneId = new Integer(-1);
        AsyncResult ar = (AsyncResult) msg.obj;
        if (ar != null && (ar.result instanceof Integer)) {
            return (Integer) ar.result;
        }
        return phoneId;
    }

    @Override
    protected void sendData(String destAddr, String scAddr, int destPort, int originalPort, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        Rlog.d(TAG, "GsmSmsDispatcher.sendData: enter");
        if (isDmLock) {
            Rlog.d(TAG, "DM status: lock-on");
            return;
        }
        SmsMessage.SubmitPdu pdu = SmsMessage.getSubmitPdu(scAddr, destAddr, destPort, originalPort, data, deliveryIntent != null);
        if (pdu != null) {
            SMSDispatcher.SmsTracker tracker = getSmsTracker(getSmsTrackerMap(destAddr, scAddr, destPort, data, pdu), sentIntent, deliveryIntent, getFormat(), null, false, null, false, true);
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
    protected void sendMultipartData(String destAddr, String scAddr, int destPort, ArrayList<SmsRawData> data, ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents) {
        if (isDmLock) {
            Rlog.d(TAG, "DM status: lock-on");
            return;
        }
        int refNumber = getNextConcatenatedRef() & 255;
        int msgCount = data.size();
        SMSDispatcher.SmsTracker[] trackers = new SMSDispatcher.SmsTracker[msgCount];
        for (int i = 0; i < msgCount; i++) {
            byte[] smsHeader = SmsHeader.getSubmitPduHeader(destPort, refNumber, i + 1, msgCount);
            PendingIntent sentIntent = null;
            if (sentIntents != null && sentIntents.size() > i) {
                sentIntent = sentIntents.get(i);
            }
            PendingIntent deliveryIntent = null;
            if (deliveryIntents != null && deliveryIntents.size() > i) {
                deliveryIntent = deliveryIntents.get(i);
            }
            SmsMessage.SubmitPdu pdus = SmsMessage.getSubmitPdu(scAddr, destAddr, data.get(i).getBytes(), smsHeader, deliveryIntent != null);
            trackers[i] = getSmsTracker(getSmsTrackerMap(destAddr, scAddr, destPort, data.get(i).getBytes(), pdus), sentIntent, deliveryIntent, getFormat(), null, false, null, false, true);
        }
        if (data == null || trackers == null || trackers.length == 0 || trackers[0] == null) {
            Rlog.e(TAG, "Cannot send multipart data. parts=" + data + " trackers=" + trackers);
            return;
        }
        String carrierPackage = getCarrierAppPackageName();
        if (carrierPackage != null) {
            Rlog.d(TAG, "Found carrier package.");
            SMSDispatcher.DataSmsSender smsSender = new SMSDispatcher.DataSmsSender(trackers[0]);
            smsSender.sendSmsByCarrierApp(carrierPackage, new SMSDispatcher.SmsSenderCallback(smsSender));
            return;
        }
        Rlog.v(TAG, "No carrier package.");
        for (SMSDispatcher.SmsTracker tracker : trackers) {
            if (tracker != null) {
                sendRawPdu(tracker);
            } else {
                Rlog.e(TAG, "Null tracker.");
            }
        }
    }

    protected void activateCellBroadcastSms(int activate, Message response) {
        Message reply = obtainMessage(101, response);
        this.mCi.setGsmBroadcastActivation(activate == 0, reply);
    }

    protected void getCellBroadcastSmsConfig(Message response) {
        Message reply = obtainMessage(CallFailCause.RECOVERY_ON_TIMER_EXPIRY, response);
        this.mCi.getGsmBroadcastConfig(reply);
    }

    protected void setCellBroadcastConfig(int[] configValuesArray, Message response) {
        Rlog.e(TAG, "Error! The functionality cell broadcast sms is not implemented for GSM.");
        response.recycle();
    }

    protected void setCellBroadcastConfig(ArrayList<SmsBroadcastConfigInfo> chIdList, ArrayList<SmsBroadcastConfigInfo> langList, Message response) {
        Message reply = obtainMessage(103, response);
        chIdList.addAll(langList);
        this.mCi.setGsmBroadcastConfig((SmsBroadcastConfigInfo[]) chIdList.toArray(new SmsBroadcastConfigInfo[1]), reply);
    }

    protected void queryCellBroadcastActivation(Message response) {
        Message reply = obtainMessage(CharacterSets.ISO_2022_CN, response);
        this.mCi.getGsmBroadcastConfig(reply);
    }

    @Override
    public int copyTextMessageToIccCard(String scAddress, String address, List<String> text, int status, long timestamp) {
        boolean isDeliverPdu;
        Rlog.d(TAG, "GsmSMSDispatcher: copy text message to icc card");
        if (!checkPhoneNumber(scAddress)) {
            Rlog.d(TAG, "[copyText invalid sc address");
            scAddress = null;
        }
        if (!checkPhoneNumber(address)) {
            Rlog.d(TAG, "[copyText invalid dest address");
            return 8;
        }
        this.mSuccess = true;
        int msgCount = text.size();
        Rlog.d(TAG, "[copyText storage available");
        if (status == 1 || status == 3) {
            Rlog.d(TAG, "[copyText to encode deliver pdu");
            isDeliverPdu = true;
        } else if (status == 5 || status == 7) {
            isDeliverPdu = false;
            Rlog.d(TAG, "[copyText to encode submit pdu");
        } else {
            Rlog.d(TAG, "[copyText invalid status, default is deliver pdu");
            return 1;
        }
        Rlog.d(TAG, "[copyText msgCount " + msgCount);
        if (msgCount > 1) {
            Rlog.d(TAG, "[copyText multi-part message");
        } else if (msgCount == 1) {
            Rlog.d(TAG, "[copyText single-part message");
        } else {
            Rlog.d(TAG, "[copyText invalid message count");
            return 1;
        }
        int refNumber = getNextConcatenatedRef() & 255;
        int encoding = 0;
        GsmAlphabet.TextEncodingDetails[] details = new GsmAlphabet.TextEncodingDetails[msgCount];
        for (int i = 0; i < msgCount; i++) {
            details[i] = SmsMessage.calculateLength(text.get(i), false);
            if (encoding != details[i].codeUnitSize && (encoding == 0 || encoding == 1)) {
                encoding = details[i].codeUnitSize;
            }
        }
        for (int i2 = 0; i2 < msgCount; i2++) {
            if (!this.mSuccess) {
                Rlog.d(TAG, "[copyText Exception happened when copy message");
                return 1;
            }
            int singleShiftId = -1;
            int lockingShiftId = -1;
            int language = details[i2].shiftLangId;
            int encoding_method = encoding;
            if (encoding == 1) {
                Rlog.d(TAG, "Detail: " + i2 + " ted" + details[i2]);
                if (details[i2].useLockingShift && details[i2].useSingleShift) {
                    singleShiftId = language;
                    lockingShiftId = language;
                    encoding_method = 13;
                } else if (details[i2].useLockingShift) {
                    lockingShiftId = language;
                    encoding_method = 12;
                } else if (details[i2].useSingleShift) {
                    singleShiftId = language;
                    encoding_method = 11;
                }
            }
            byte[] smsHeader = null;
            if (msgCount > 1) {
                Rlog.d(TAG, "[copyText get pdu header for multi-part message");
                smsHeader = SmsHeader.getSubmitPduHeaderWithLang(-1, refNumber, i2 + 1, msgCount, singleShiftId, lockingShiftId);
            }
            if (isDeliverPdu) {
                SmsMessage.DeliverPdu pdu = SmsMessage.getDeliverPduWithLang(scAddress, address, text.get(i2), smsHeader, timestamp, encoding, language);
                if (pdu != null) {
                    Rlog.d(TAG, "[copyText write deliver pdu into SIM");
                    this.mCi.writeSmsToSim(status, IccUtils.bytesToHexString(pdu.encodedScAddress), IccUtils.bytesToHexString(pdu.encodedMessage), obtainMessage(106));
                }
            } else {
                SmsMessage.SubmitPdu pdu2 = SmsMessage.getSubmitPduWithLang(scAddress, address, text.get(i2), false, smsHeader, encoding_method, language);
                if (pdu2 != null) {
                    Rlog.d(TAG, "[copyText write submit pdu into SIM");
                    this.mCi.writeSmsToSim(status, IccUtils.bytesToHexString(pdu2.encodedScAddress), IccUtils.bytesToHexString(pdu2.encodedMessage), obtainMessage(106));
                }
            }
            synchronized (this.mLock) {
                try {
                    Rlog.d(TAG, "[copyText wait until the message be wrote in SIM");
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    Rlog.d(TAG, "[copyText interrupted while trying to copy text message into SIM");
                    return 1;
                }
            }
            Rlog.d(TAG, "[copyText thread is waked up");
        }
        if (this.mSuccess) {
            Rlog.d(TAG, "[copyText all messages have been copied into SIM");
            return 0;
        }
        Rlog.d(TAG, "[copyText copy failed");
        return 1;
    }

    private boolean isValidSmsAddress(String address) {
        String encodedAddress = PhoneNumberUtils.extractNetworkPortion(address);
        return encodedAddress == null || encodedAddress.length() == address.length();
    }

    private boolean checkPhoneNumber(char c) {
        return (c >= '0' && c <= '9') || c == '*' || c == '+' || c == '#' || c == 'N' || c == ' ' || c == '-';
    }

    private boolean checkPhoneNumber(String address) {
        if (address == null) {
            return true;
        }
        Rlog.d(TAG, "checkPhoneNumber: " + address);
        int n = address.length();
        for (int i = 0; i < n; i++) {
            if (!checkPhoneNumber(address.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void sendTextWithEncodingType(String destAddr, String scAddr, String text, int encodingType, PendingIntent sentIntent, PendingIntent deliveryIntent, Uri messageUri, String callingPkg, boolean persistMessage) {
        if (isDmLock) {
            Rlog.d(TAG, "DM status: lock-on");
            return;
        }
        GsmAlphabet.TextEncodingDetails details = SmsMessage.calculateLength(text, false);
        if (encodingType != details.codeUnitSize && (encodingType == 0 || encodingType == 1)) {
            Rlog.d(TAG, "[enc conflict between details[" + details.codeUnitSize + "] and encoding " + encodingType);
            details.codeUnitSize = encodingType;
        }
        SmsMessage.SubmitPdu pdu = SmsMessage.getSubmitPdu(scAddr, destAddr, text, deliveryIntent != null, null, encodingType, details.languageTable, details.languageShiftTable);
        if (pdu != null) {
            SMSDispatcher.SmsTracker tracker = getSmsTracker(getSmsTrackerMap(destAddr, scAddr, text, pdu), sentIntent, deliveryIntent, getFormat(), messageUri, false, text, true, persistMessage);
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
        Rlog.e(TAG, "GsmSMSDispatcher.sendTextWithEncodingType(): getSubmitPdu() returned null");
        if (sentIntent == null) {
            return;
        }
        try {
            sentIntent.send(3);
        } catch (PendingIntent.CanceledException e) {
            Rlog.e(TAG, "failed to send back RESULT_ERROR_NULL_PDU");
        }
    }

    @Override
    protected void sendMultipartTextWithEncodingType(String destAddr, String scAddr, ArrayList<String> parts, int encodingType, ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents, Uri messageUri, String callingPkg, boolean persistMessage) {
        if (isDmLock) {
            Rlog.d(TAG, "DM status: lock-on");
            return;
        }
        String fullMessageText = getMultipartMessageText(parts);
        int refNumber = getNextConcatenatedRef() & 255;
        int msgCount = parts.size();
        GsmAlphabet.TextEncodingDetails[] encodingForParts = new GsmAlphabet.TextEncodingDetails[msgCount];
        for (int i = 0; i < msgCount; i++) {
            GsmAlphabet.TextEncodingDetails details = SmsMessage.calculateLength(parts.get(i), false);
            if (encodingType != details.codeUnitSize && (encodingType == 0 || encodingType == 1)) {
                Rlog.d(TAG, "[enc conflict between details[" + details.codeUnitSize + "] and encoding " + encodingType);
                details.codeUnitSize = encodingType;
            }
            encodingForParts[i] = details;
        }
        SMSDispatcher.SmsTracker[] trackers = new SMSDispatcher.SmsTracker[msgCount];
        AtomicInteger unsentPartCount = new AtomicInteger(msgCount);
        AtomicBoolean anyPartFailed = new AtomicBoolean(false);
        int i2 = 0;
        while (i2 < msgCount) {
            SmsHeader.ConcatRef concatRef = new SmsHeader.ConcatRef();
            concatRef.refNumber = refNumber;
            concatRef.seqNumber = i2 + 1;
            concatRef.msgCount = msgCount;
            concatRef.isEightBits = true;
            SmsHeader smsHeader = new SmsHeader();
            smsHeader.concatRef = concatRef;
            if (encodingType == 1) {
                smsHeader.languageTable = encodingForParts[i2].languageTable;
                smsHeader.languageShiftTable = encodingForParts[i2].languageShiftTable;
            }
            PendingIntent sentIntent = null;
            if (sentIntents != null && sentIntents.size() > i2) {
                sentIntent = sentIntents.get(i2);
            }
            PendingIntent deliveryIntent = null;
            if (deliveryIntents != null && deliveryIntents.size() > i2) {
                deliveryIntent = deliveryIntents.get(i2);
            }
            trackers[i2] = getNewSubmitPduTracker(destAddr, scAddr, parts.get(i2), smsHeader, encodingType, sentIntent, deliveryIntent, i2 == msgCount + (-1), unsentPartCount, anyPartFailed, messageUri, fullMessageText);
            trackers[i2].mPersistMessage = persistMessage;
            i2++;
        }
        if (parts == null || trackers == null || trackers.length == 0 || trackers[0] == null) {
            Rlog.e(TAG, "Cannot send multipart text. parts=" + parts + " trackers=" + trackers);
            return;
        }
        String carrierPackage = getCarrierAppPackageName();
        if (carrierPackage != null) {
            Rlog.d(TAG, "Found carrier package.");
            SMSDispatcher.MultipartSmsSender smsSender = new SMSDispatcher.MultipartSmsSender(parts, trackers);
            smsSender.sendSmsByCarrierApp(carrierPackage, new SMSDispatcher.MultipartSmsSenderCallback(smsSender));
            return;
        }
        Rlog.v(TAG, "No carrier package.");
        for (SMSDispatcher.SmsTracker tracker : trackers) {
            if (tracker != null) {
                sendSubmitPdu(tracker);
            } else {
                Rlog.e(TAG, "Null tracker.");
            }
        }
    }

    @Override
    protected void handleIccFull() {
        this.mGsmInboundSmsHandler.mStorageMonitor.handleIccFull();
    }

    @Override
    protected void handleQueryCbActivation(AsyncResult ar) {
        Boolean result = null;
        if (ar.exception == null) {
            ArrayList<SmsBroadcastConfigInfo> list = (ArrayList) ar.result;
            if (list.size() == 0) {
                result = new Boolean(false);
            } else {
                SmsBroadcastConfigInfo cbConfig = list.get(0);
                Rlog.d(TAG, "cbConfig: " + cbConfig.toString());
                if (cbConfig.getFromCodeScheme() == -1 && cbConfig.getToCodeScheme() == -1 && cbConfig.getFromServiceId() == -1 && cbConfig.getToServiceId() == -1 && !cbConfig.isSelected()) {
                    result = new Boolean(false);
                } else {
                    result = new Boolean(true);
                }
            }
        }
        Rlog.d(TAG, "queryCbActivation: " + result);
        AsyncResult.forMessage((Message) ar.userObj, result, ar.exception);
        ((Message) ar.userObj).sendToTarget();
    }

    @Override
    public void sendTextWithExtraParams(String destAddr, String scAddr, String text, Bundle extraParams, PendingIntent sentIntent, PendingIntent deliveryIntent, Uri messageUri, String callingPkg, boolean persistMessage) {
        Rlog.d(TAG, "sendTextWithExtraParams");
        if (isDmLock) {
            Rlog.d(TAG, "DM status: lock-on");
            return;
        }
        int validityPeriod = extraParams.getInt(SmsManager.EXTRA_PARAMS_VALIDITY_PERIOD, -1);
        Rlog.d(TAG, "validityPeriod is " + validityPeriod);
        SmsMessage.SubmitPdu pdu = SmsMessage.getSubmitPdu(scAddr, destAddr, text, deliveryIntent != null, null, 0, 0, 0, validityPeriod);
        if (pdu != null) {
            SMSDispatcher.SmsTracker tracker = getSmsTracker(getSmsTrackerMap(destAddr, scAddr, text, pdu), sentIntent, deliveryIntent, getFormat(), messageUri, false, text, true, persistMessage);
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
        Rlog.e(TAG, "GsmSMSDispatcher.sendTextWithExtraParams(): getSubmitPdu() returned null");
        if (sentIntent == null) {
            return;
        }
        try {
            sentIntent.send(3);
        } catch (PendingIntent.CanceledException e) {
            Rlog.e(TAG, "failed to send back RESULT_ERROR_NULL_PDU");
        }
    }

    @Override
    public void sendMultipartTextWithExtraParams(String destAddr, String scAddr, ArrayList<String> parts, Bundle extraParams, ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents, Uri messageUri, String callingPkg, boolean persistMessage) {
        Rlog.d(TAG, "sendMultipartTextWithExtraParams");
        if (isDmLock) {
            Rlog.d(TAG, "DM status: lock-on");
            return;
        }
        int validityPeriod = extraParams.getInt(SmsManager.EXTRA_PARAMS_VALIDITY_PERIOD, -1);
        Rlog.d(TAG, "validityPeriod is " + validityPeriod);
        String fullMessageText = getMultipartMessageText(parts);
        int refNumber = getNextConcatenatedRef() & 255;
        int msgCount = parts.size();
        int encoding = 0;
        GsmAlphabet.TextEncodingDetails[] encodingForParts = new GsmAlphabet.TextEncodingDetails[msgCount];
        for (int i = 0; i < msgCount; i++) {
            GsmAlphabet.TextEncodingDetails details = calculateLength(parts.get(i), false);
            if (encoding != details.codeUnitSize && (encoding == 0 || encoding == 1)) {
                encoding = details.codeUnitSize;
            }
            encodingForParts[i] = details;
        }
        SMSDispatcher.SmsTracker[] trackers = new SMSDispatcher.SmsTracker[msgCount];
        AtomicInteger unsentPartCount = new AtomicInteger(msgCount);
        AtomicBoolean anyPartFailed = new AtomicBoolean(false);
        int i2 = 0;
        while (i2 < msgCount) {
            SmsHeader.ConcatRef concatRef = new SmsHeader.ConcatRef();
            concatRef.refNumber = refNumber;
            concatRef.seqNumber = i2 + 1;
            concatRef.msgCount = msgCount;
            concatRef.isEightBits = true;
            SmsHeader smsHeader = new SmsHeader();
            smsHeader.concatRef = concatRef;
            if (encoding == 1) {
                smsHeader.languageTable = encodingForParts[i2].languageTable;
                smsHeader.languageShiftTable = encodingForParts[i2].languageShiftTable;
            }
            PendingIntent sentIntent = null;
            if (sentIntents != null && sentIntents.size() > i2) {
                sentIntent = sentIntents.get(i2);
            }
            PendingIntent deliveryIntent = null;
            if (deliveryIntents != null && deliveryIntents.size() > i2) {
                deliveryIntent = deliveryIntents.get(i2);
            }
            trackers[i2] = getNewSubmitPduTracker(destAddr, scAddr, parts.get(i2), smsHeader, encoding, sentIntent, deliveryIntent, i2 == msgCount + (-1), unsentPartCount, anyPartFailed, messageUri, fullMessageText, validityPeriod);
            trackers[i2].mPersistMessage = persistMessage;
            i2++;
        }
        if (parts == null || trackers == null || trackers.length == 0 || trackers[0] == null) {
            Rlog.e(TAG, "Cannot send multipart text. parts=" + parts + " trackers=" + trackers);
            return;
        }
        String carrierPackage = getCarrierAppPackageName();
        if (carrierPackage != null) {
            Rlog.d(TAG, "Found carrier package.");
            SMSDispatcher.MultipartSmsSender smsSender = new SMSDispatcher.MultipartSmsSender(parts, trackers);
            smsSender.sendSmsByCarrierApp(carrierPackage, new SMSDispatcher.MultipartSmsSenderCallback(smsSender));
            return;
        }
        Rlog.v(TAG, "No carrier package.");
        for (SMSDispatcher.SmsTracker tracker : trackers) {
            if (tracker != null) {
                sendSubmitPdu(tracker);
            } else {
                Rlog.e(TAG, "Null tracker.");
            }
        }
    }
}
