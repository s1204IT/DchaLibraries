package com.android.internal.telephony.cdma;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Message;
import android.telephony.Rlog;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.ImsSMSDispatcher;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SMSDispatcher;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsUsageMonitor;
import com.android.internal.telephony.cdma.SmsMessage;
import com.android.internal.telephony.cdma.sms.UserData;
import com.android.internal.telephony.uicc.IccUtils;
import com.mediatek.internal.telephony.cdma.CdmaOmhSmsUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CdmaSMSDispatcher extends SMSDispatcher {
    private static final int EVENT_CDMA_CARD_INITIAL_ESN_OR_MEID = 200;
    private static final String TAG = "CdmaMtSms";
    private static final boolean VDBG = false;

    public CdmaSMSDispatcher(Phone phone, SmsUsageMonitor usageMonitor, ImsSMSDispatcher imsSMSDispatcher) {
        super(phone, usageMonitor, imsSMSDispatcher);
        Rlog.d(TAG, "CdmaSMSDispatcher created");
        this.mCi.setCDMACardInitalEsnMeid(this, 200, null);
    }

    @Override
    public String getFormat() {
        return android.telephony.SmsMessage.FORMAT_3GPP2;
    }

    public void sendStatusReportMessage(SmsMessage sms) {
        sendMessage(obtainMessage(10, sms));
    }

    @Override
    protected void handleStatusReport(Object o) {
        if (o instanceof SmsMessage) {
            handleCdmaStatusReport((SmsMessage) o);
        } else {
            Rlog.e(TAG, "handleStatusReport() called for object type " + o.getClass().getName());
        }
    }

    private void handleCdmaStatusReport(SmsMessage sms) {
        int count = this.deliveryPendingList.size();
        for (int i = 0; i < count; i++) {
            SMSDispatcher.SmsTracker tracker = this.deliveryPendingList.get(i);
            if (tracker.mMessageRef == sms.mMessageRef) {
                this.deliveryPendingList.remove(i);
                tracker.updateSentMessageStatus(this.mContext, 0);
                PendingIntent intent = tracker.mDeliveryIntent;
                Intent fillIn = new Intent();
                fillIn.putExtra("pdu", sms.getPdu());
                fillIn.putExtra("format", getFormat());
                try {
                    intent.send(this.mContext, -1, fillIn);
                    return;
                } catch (PendingIntent.CanceledException e) {
                    return;
                }
            }
        }
    }

    @Override
    public void sendData(String destAddr, String scAddr, int destPort, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        if (CdmaOmhSmsUtils.isOmhCard(this.mPhone.getSubId())) {
            CdmaOmhSmsUtils.getNextMessageId(this.mPhone.getSubId());
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
                sendSubmitPdu(tracker);
                return;
            }
        }
        Rlog.e(TAG, "CdmaSMSDispatcher.sendData(): getSubmitPdu() returned null");
        if (sentIntent == null) {
            return;
        }
        try {
            sentIntent.send(1);
        } catch (PendingIntent.CanceledException e) {
            Rlog.e(TAG, "Intent has been canceled!");
        }
    }

    @Override
    public void sendText(String destAddr, String scAddr, String text, PendingIntent sentIntent, PendingIntent deliveryIntent, Uri messageUri, String callingPkg, boolean persistMessage) {
        if (CdmaOmhSmsUtils.isOmhCard(this.mPhone.getSubId())) {
            CdmaOmhSmsUtils.getNextMessageId(this.mPhone.getSubId());
        }
        SmsMessage.SubmitPdu pdu = SmsMessage.getSubmitPdu(scAddr, destAddr, text, deliveryIntent != null, (SmsHeader) null);
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
                sendSubmitPdu(tracker);
                return;
            }
        }
        Rlog.e(TAG, "CdmaSMSDispatcher.sendText(): getSubmitPdu() returned null");
        if (sentIntent == null) {
            return;
        }
        try {
            sentIntent.send(1);
        } catch (PendingIntent.CanceledException e) {
            Rlog.e(TAG, "Intent has been canceled!");
        }
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
            uData.msgEncoding = 2;
        } else {
            uData.msgEncoding = 4;
        }
        uData.msgEncodingSet = true;
        if (CdmaOmhSmsUtils.isOmhCard(this.mPhone.getSubId())) {
            CdmaOmhSmsUtils.getNextMessageId(this.mPhone.getSubId());
        }
        if (deliveryIntent == null) {
            lastPart = false;
        }
        SmsMessage.SubmitPdu submitPdu = SmsMessage.getSubmitPdu(destinationAddress, uData, lastPart);
        return getSmsTracker(getSmsTrackerMap(destinationAddress, scAddress, message, submitPdu), sentIntent, deliveryIntent, getFormat(), unsentPartCount, anyPartFailed, messageUri, smsHeader, false, fullMessageText, true, true);
    }

    @Override
    protected void sendSubmitPdu(SMSDispatcher.SmsTracker tracker) {
        String ecmModeStr = TelephonyManager.getTelephonyProperty(this.mPhone.getPhoneId(), "ril.cdma.inecmmode", "false");
        boolean ecmMode = Boolean.parseBoolean(ecmModeStr);
        if (ecmMode) {
            tracker.onFailed(this.mContext, 4, 0);
        } else {
            sendRawPdu(tracker);
        }
    }

    @Override
    public void sendSms(SMSDispatcher.SmsTracker tracker) {
        if (addToTrackerQueue(tracker)) {
            Rlog.d(TAG, "sendSms:  isIms()=" + isIms() + " mRetryCount=" + tracker.mRetryCount + " mImsRetry=" + tracker.mImsRetry + " mMessageRef=" + tracker.mMessageRef + " SS=" + this.mPhone.getServiceState().getState());
            sendSmsByPstn(tracker);
        }
    }

    @Override
    protected void sendSmsByPstn(SMSDispatcher.SmsTracker tracker) {
        int ss = this.mPhone.getServiceState().getState();
        if (!isIms() && ss != 0) {
            if (isSimAbsent()) {
                tracker.onFailed(this.mContext, 1, 0);
            } else {
                tracker.onFailed(this.mContext, getNotInServiceError(ss), 0);
            }
            Message delay = obtainMessage(107, tracker);
            sendMessageDelayed(delay, 10L);
            return;
        }
        Message reply = obtainMessage(2, tracker);
        byte[] pdu = (byte[]) tracker.getData().get("pdu");
        int currentDataNetwork = this.mPhone.getServiceState().getDataNetworkType();
        boolean imsSmsDisabled = (currentDataNetwork == 14 || (currentDataNetwork == 13 && !this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed())) && this.mPhone.getServiceState().getVoiceNetworkType() == 7 && ((GsmCdmaPhone) this.mPhone).mCT.mState != PhoneConstants.State.IDLE;
        if ((tracker.mImsRetry == 0 && !isIms()) || imsSmsDisabled) {
            this.mCi.sendCdmaSms(pdu, reply);
        } else {
            this.mCi.sendImsCdmaSms(pdu, tracker.mImsRetry, tracker.mMessageRef, reply);
            tracker.mImsRetry++;
        }
    }

    private boolean addToTrackerQueue(SMSDispatcher.SmsTracker tracker) {
        boolean isReadySend = false;
        synchronized (this.mSTrackersQueue) {
            if (this.mSTrackersQueue.isEmpty() || this.mSTrackersQueue.get(0) != tracker) {
                Rlog.d(TAG, "Add tracker into the list: " + tracker);
                this.mSTrackersQueue.add(tracker);
            }
            if (this.mSTrackersQueue.get(0) == tracker) {
                isReadySend = true;
            }
        }
        if (!isReadySend) {
            Rlog.d(TAG, "There is another tracker in-queue and is sending");
        }
        return isReadySend;
    }

    @Override
    protected void sendData(String destAddr, String scAddr, int destPort, int originalPort, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        Rlog.d(TAG, "CdmaSMSDispatcher, implemented for interfaces needed. sendData");
        if (CdmaOmhSmsUtils.isOmhCard(this.mPhone.getSubId())) {
            CdmaOmhSmsUtils.getNextMessageId(this.mPhone.getSubId());
        }
        SmsMessage.SubmitPdu pdu = SmsMessage.getSubmitPdu(scAddr, destAddr, destPort, originalPort, data, deliveryIntent != null);
        if (pdu == null) {
            Rlog.d(TAG, "sendData error: invalid paramters, pdu == null.");
            return;
        }
        SMSDispatcher.SmsTracker tracker = getSmsTracker(getSmsTrackerMap(destAddr, scAddr, destPort, data, pdu), sentIntent, deliveryIntent, getFormat(), null, false, null, false, true);
        String carrierPackage = getCarrierAppPackageName();
        if (carrierPackage != null) {
            Rlog.d(TAG, "Found carrier package. w/op");
            SMSDispatcher.DataSmsSender smsSender = new SMSDispatcher.DataSmsSender(tracker);
            smsSender.sendSmsByCarrierApp(carrierPackage, new SMSDispatcher.SmsSenderCallback(smsSender));
        } else {
            Rlog.v(TAG, "No carrier package. w/op");
            sendRawPdu(tracker);
        }
    }

    @Override
    public int copyTextMessageToIccCard(String scAddress, String address, List<String> text, int status, long timestamp) {
        Rlog.d(TAG, "CDMASMSDispatcher: copy text message to icc card");
        this.mSuccess = true;
        int msgCount = text.size();
        Rlog.d(TAG, "[copyText storage available");
        if (status == 1 || status == 3) {
            Rlog.d(TAG, "[copyText to encode deliver pdu");
        } else if (status == 5 || status == 7) {
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
        for (int i = 0; i < msgCount; i++) {
            if (!this.mSuccess) {
                Rlog.d(TAG, "[copyText Exception happened when copy message");
                return 1;
            }
            SmsMessage.SubmitPdu pdu = SmsMessage.createEfPdu(address, text.get(i), timestamp);
            if (pdu == null) {
                return 1;
            }
            Rlog.d(TAG, "[copyText write submit pdu into UIM");
            this.mCi.writeSmsToRuim(status, IccUtils.bytesToHexString(pdu.encodedMessage), obtainMessage(106));
            synchronized (this.mLock) {
                try {
                    Rlog.d(TAG, "[copyText wait until the message be wrote in UIM");
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    Rlog.d(TAG, "[copyText interrupted while trying to copy text message into UIM");
                    return 1;
                }
            }
            Rlog.d(TAG, "[copyText thread is waked up");
        }
        if (this.mSuccess) {
            Rlog.d(TAG, "[copyText all messages have been copied into UIM");
            return 0;
        }
        Rlog.d(TAG, "[copyText copy failed");
        return 1;
    }

    @Override
    protected void sendTextWithEncodingType(String destAddr, String scAddr, String text, int encodingType, PendingIntent sentIntent, PendingIntent deliveryIntent, Uri messageUri, String callingPkg, boolean persistMessage) {
        Rlog.d(TAG, "CdmaSMSDispatcher, implemented for interfaces needed. sendTextWithEncodingType");
        int encoding = encodingType;
        Rlog.d(TAG, "want to use encoding = " + encodingType);
        if (encodingType < 0 || encodingType > 10) {
            Rlog.w(TAG, "unavalid encoding = " + encodingType);
            Rlog.w(TAG, "to use the unkown default.");
            encoding = 0;
        }
        if (encoding == 0) {
            Rlog.d(TAG, "unkown encoding, to find one best.");
            GsmAlphabet.TextEncodingDetails details = SmsMessage.calculateLength((CharSequence) text, false, encoding);
            if (encoding != details.codeUnitSize && (encoding == 0 || encoding == 1)) {
                encoding = details.codeUnitSize;
            }
        }
        UserData uData = new UserData();
        uData.payloadStr = text;
        if (encoding == 1) {
            uData.msgEncoding = 2;
        } else if (encoding == 2) {
            uData.msgEncoding = 0;
        } else {
            uData.msgEncoding = 4;
        }
        uData.msgEncodingSet = true;
        if (CdmaOmhSmsUtils.isOmhCard(this.mPhone.getSubId())) {
            CdmaOmhSmsUtils.getNextMessageId(this.mPhone.getSubId());
        }
        SmsMessage.SubmitPdu submitPdu = SmsMessage.getSubmitPdu(destAddr, uData, deliveryIntent != null);
        if (submitPdu != null) {
            SMSDispatcher.SmsTracker tracker = getSmsTracker(getSmsTrackerMap(destAddr, scAddr, text, submitPdu), sentIntent, deliveryIntent, getFormat(), messageUri, false, text, true, true);
            String carrierPackage = getCarrierAppPackageName();
            if (carrierPackage != null) {
                Rlog.d(TAG, "sendTextWithEncodingType: Found carrier package.");
                SMSDispatcher.TextSmsSender smsSender = new SMSDispatcher.TextSmsSender(tracker);
                smsSender.sendSmsByCarrierApp(carrierPackage, new SMSDispatcher.SmsSenderCallback(smsSender));
                return;
            } else {
                Rlog.v(TAG, "sendTextWithEncodingType: No carrier package.");
                sendSubmitPdu(tracker);
                return;
            }
        }
        Rlog.d(TAG, "sendTextWithEncodingType: submitPdu is null");
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
        String fullMessageText = getMultipartMessageText(parts);
        Rlog.d(TAG, "CdmaSMSDispatcher, implemented by for interfaces needed. sendMultipartTextWithEncodingType");
        int refNumber = getNextConcatenatedRef() & 255;
        int msgCount = parts.size();
        int encoding = encodingType;
        Rlog.d(TAG, "want to use encoding = " + encodingType);
        if (encodingType < 0 || encodingType > 10) {
            Rlog.w(TAG, "unavalid encoding = " + encodingType);
            Rlog.w(TAG, "to use the unkown default.");
            encoding = 0;
        }
        GsmAlphabet.TextEncodingDetails[] encodingForParts = new GsmAlphabet.TextEncodingDetails[msgCount];
        if (encoding == 0) {
            Rlog.d(TAG, "unkown encoding, to find one best.");
            for (int i = 0; i < msgCount; i++) {
                GsmAlphabet.TextEncodingDetails details = calculateLength(parts.get(i), false);
                if (encoding != details.codeUnitSize && (encoding == 0 || encoding == 1)) {
                    encoding = details.codeUnitSize;
                }
                encodingForParts[i] = details;
            }
        } else {
            Rlog.d(TAG, "APP want use specified encoding type.");
            for (int i2 = 0; i2 < msgCount; i2++) {
                GsmAlphabet.TextEncodingDetails details2 = SmsMessage.calculateLength((CharSequence) parts.get(i2), false, encoding);
                details2.codeUnitSize = encoding;
                encodingForParts[i2] = details2;
            }
        }
        SMSDispatcher.SmsTracker[] trackers = new SMSDispatcher.SmsTracker[msgCount];
        AtomicInteger unsentPartCount = new AtomicInteger(msgCount);
        AtomicBoolean anyPartFailed = new AtomicBoolean(false);
        Rlog.d(TAG, "now to send one by one, msgCount = " + msgCount);
        int i3 = 0;
        while (i3 < msgCount) {
            SmsHeader.ConcatRef concatRef = new SmsHeader.ConcatRef();
            concatRef.refNumber = refNumber;
            concatRef.seqNumber = i3 + 1;
            concatRef.msgCount = msgCount;
            concatRef.isEightBits = true;
            SmsHeader smsHeader = new SmsHeader();
            smsHeader.concatRef = concatRef;
            PendingIntent sentIntent = null;
            if (sentIntents != null && sentIntents.size() > i3) {
                sentIntent = sentIntents.get(i3);
            }
            PendingIntent deliveryIntent = null;
            if (deliveryIntents != null && deliveryIntents.size() > i3) {
                deliveryIntent = deliveryIntents.get(i3);
            }
            Rlog.d(TAG, "to send the " + i3 + " part");
            trackers[i3] = getNewSubmitPduTracker(destAddr, scAddr, parts.get(i3), smsHeader, encodingForParts[i3].codeUnitSize, sentIntent, deliveryIntent, i3 == msgCount + (-1), unsentPartCount, anyPartFailed, messageUri, fullMessageText);
            i3++;
        }
        if (parts == null || trackers == null || trackers.length == 0 || trackers[0] == null) {
            Rlog.e(TAG, "sendMultipartTextWithEncodingType: Cannot send multipart text. parts=" + parts + " trackers=" + trackers);
            return;
        }
        String carrierPackage = getCarrierAppPackageName();
        if (carrierPackage != null) {
            Rlog.d(TAG, "sendMultipartTextWithEncodingType: Found carrier package.");
            SMSDispatcher.MultipartSmsSender smsSender = new SMSDispatcher.MultipartSmsSender(parts, trackers);
            smsSender.sendSmsByCarrierApp(carrierPackage, new SMSDispatcher.MultipartSmsSenderCallback(smsSender));
            return;
        }
        Rlog.v(TAG, "sendMultipartTextWithEncodingType: No carrier package.");
        for (SMSDispatcher.SmsTracker tracker : trackers) {
            if (tracker != null) {
                sendSubmitPdu(tracker);
            } else {
                Rlog.e(TAG, "sendMultipartTextWithEncodingType: Null tracker.");
            }
        }
    }

    @Override
    public void sendTextWithExtraParams(String destAddr, String scAddr, String text, Bundle extraParams, PendingIntent sentIntent, PendingIntent deliveryIntent, Uri messageUri, String callingPkg, boolean persistMessage) {
        int validityPeriod;
        int priority;
        int encoding;
        Rlog.d(TAG, "CdmaSMSDispatcher, implemented by for interfaces needed. sendTextWithExtraParams");
        if (extraParams == null) {
            Rlog.d(TAG, "extraParams == null, will encoding with no extra feature.");
            validityPeriod = -1;
            priority = -1;
            encoding = 0;
        } else {
            validityPeriod = extraParams.getInt(SmsManager.EXTRA_PARAMS_VALIDITY_PERIOD, -1);
            if (validityPeriod > 244 && validityPeriod <= 255) {
                validityPeriod = 244;
            }
            priority = extraParams.getInt("priority", -1);
            encoding = extraParams.getInt(SmsManager.EXTRA_PARAMS_ENCODING_TYPE, 0);
        }
        Rlog.d(TAG, "validityPeriod is " + validityPeriod);
        Rlog.d(TAG, "priority is " + priority);
        Rlog.d(TAG, "want to use encoding = " + encoding);
        if (encoding < 0 || encoding > 10) {
            Rlog.w(TAG, "unavalid encoding = " + encoding);
            Rlog.w(TAG, "to use the unkown default.");
            encoding = 0;
        }
        if (encoding == 0) {
            Rlog.d(TAG, "unkown encoding, to find one best.");
            GsmAlphabet.TextEncodingDetails details = calculateLength(text, false);
            if (encoding != details.codeUnitSize && (encoding == 0 || encoding == 1)) {
                encoding = details.codeUnitSize;
            }
        }
        if (CdmaOmhSmsUtils.isOmhCard(this.mPhone.getSubId())) {
            CdmaOmhSmsUtils.getNextMessageId(this.mPhone.getSubId());
        }
        SmsMessage.SubmitPdu submitPdu = SmsMessage.getSubmitPdu(scAddr, destAddr, text, deliveryIntent != null, null, encoding, validityPeriod, priority);
        if (submitPdu != null) {
            SMSDispatcher.SmsTracker tracker = getSmsTracker(getSmsTrackerMap(destAddr, scAddr, text, submitPdu), sentIntent, deliveryIntent, getFormat(), messageUri, false, text, true, true);
            String carrierPackage = getCarrierAppPackageName();
            if (carrierPackage != null) {
                Rlog.d(TAG, "sendTextWithExtraParams: Found carrier package.");
                SMSDispatcher.TextSmsSender smsSender = new SMSDispatcher.TextSmsSender(tracker);
                smsSender.sendSmsByCarrierApp(carrierPackage, new SMSDispatcher.SmsSenderCallback(smsSender));
                return;
            } else {
                Rlog.v(TAG, "sendTextWithExtraParams: No carrier package.");
                sendSubmitPdu(tracker);
                return;
            }
        }
        Rlog.d(TAG, "sendTextWithExtraParams: submitPdu is null");
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
        int validityPeriod;
        int priority;
        int encoding;
        String fullMessageText = getMultipartMessageText(parts);
        Rlog.d(TAG, "CdmaSMSDispatcher, implemented by for interfaces needed. sendMultipartTextWithExtraParams");
        if (extraParams == null) {
            Rlog.d(TAG, "extraParams == null, will encoding with no extra feature.");
            validityPeriod = -1;
            priority = -1;
            encoding = 0;
        } else {
            validityPeriod = extraParams.getInt(SmsManager.EXTRA_PARAMS_VALIDITY_PERIOD, -1);
            priority = extraParams.getInt("priority", -1);
            encoding = extraParams.getInt(SmsManager.EXTRA_PARAMS_ENCODING_TYPE, 0);
        }
        Rlog.d(TAG, "validityPeriod is " + validityPeriod);
        Rlog.d(TAG, "priority is " + priority);
        Rlog.d(TAG, "want to use encoding = " + encoding);
        int refNumber = getNextConcatenatedRef() & 255;
        int msgCount = parts.size();
        if (encoding < 0 || encoding > 10) {
            Rlog.w(TAG, "unavalid encoding = " + encoding);
            Rlog.w(TAG, "to use the unkown default.");
            encoding = 0;
        }
        GsmAlphabet.TextEncodingDetails[] encodingForParts = new GsmAlphabet.TextEncodingDetails[msgCount];
        if (encoding == 0) {
            Rlog.d(TAG, "unkown encoding, to find one best.");
            for (int i = 0; i < msgCount; i++) {
                GsmAlphabet.TextEncodingDetails details = calculateLength(parts.get(i), false);
                if (encoding != details.codeUnitSize && (encoding == 0 || encoding == 1)) {
                    encoding = details.codeUnitSize;
                }
                encodingForParts[i] = details;
            }
        } else {
            Rlog.d(TAG, "APP want use specified encoding type.");
            for (int i2 = 0; i2 < msgCount; i2++) {
                GsmAlphabet.TextEncodingDetails details2 = SmsMessage.calculateLength((CharSequence) parts.get(i2), false, encoding);
                details2.codeUnitSize = encoding;
                encodingForParts[i2] = details2;
            }
        }
        SMSDispatcher.SmsTracker[] trackers = new SMSDispatcher.SmsTracker[msgCount];
        AtomicInteger unsentPartCount = new AtomicInteger(msgCount);
        AtomicBoolean anyPartFailed = new AtomicBoolean(false);
        Rlog.d(TAG, "now to send one by one, msgCount = " + msgCount);
        int i3 = 0;
        while (i3 < msgCount) {
            SmsHeader.ConcatRef concatRef = new SmsHeader.ConcatRef();
            concatRef.refNumber = refNumber;
            concatRef.seqNumber = i3 + 1;
            concatRef.msgCount = msgCount;
            concatRef.isEightBits = true;
            SmsHeader smsHeader = new SmsHeader();
            smsHeader.concatRef = concatRef;
            PendingIntent sentIntent = null;
            if (sentIntents != null && sentIntents.size() > i3) {
                sentIntent = sentIntents.get(i3);
            }
            PendingIntent deliveryIntent = null;
            if (deliveryIntents != null && deliveryIntents.size() > i3) {
                deliveryIntent = deliveryIntents.get(i3);
            }
            trackers[i3] = getNewSubmitPduTracker(destAddr, scAddr, parts.get(i3), smsHeader, encodingForParts[i3].codeUnitSize, sentIntent, deliveryIntent, i3 == msgCount + (-1), unsentPartCount, anyPartFailed, messageUri, fullMessageText, validityPeriod, priority);
            i3++;
        }
        if (parts == null || trackers == null || trackers.length == 0 || trackers[0] == null) {
            Rlog.e(TAG, "sendMultipartTextWithExtraParams: Cannot send multipart text. parts=" + parts + " trackers=" + trackers);
            return;
        }
        String carrierPackage = getCarrierAppPackageName();
        if (carrierPackage != null) {
            Rlog.d(TAG, "sendMultipartTextWithExtraParams: Found carrier package.");
            SMSDispatcher.MultipartSmsSender smsSender = new SMSDispatcher.MultipartSmsSender(parts, trackers);
            smsSender.sendSmsByCarrierApp(carrierPackage, new SMSDispatcher.MultipartSmsSenderCallback(smsSender));
            return;
        }
        Rlog.v(TAG, "sendMultipartTextWithExtraParams: No carrier package.");
        for (SMSDispatcher.SmsTracker tracker : trackers) {
            if (tracker != null) {
                sendSubmitPdu(tracker);
            } else {
                Rlog.e(TAG, "sendMultipartTextWithExtraParams: Null tracker.");
            }
        }
    }

    protected SMSDispatcher.SmsTracker getNewSubmitPduTracker(String destinationAddress, String scAddress, String message, SmsHeader smsHeader, int encoding, PendingIntent sentIntent, PendingIntent deliveryIntent, boolean lastPart, AtomicInteger unsentPartCount, AtomicBoolean anyPartFailed, Uri messageUri, String fullMessageText, int validityPeriod, int priority) {
        if (CdmaOmhSmsUtils.isOmhCard(this.mPhone.getSubId())) {
            CdmaOmhSmsUtils.getNextMessageId(this.mPhone.getSubId());
        }
        SmsMessage.SubmitPdu submitPdu = SmsMessage.getSubmitPdu(scAddress, destinationAddress, message, deliveryIntent != null ? lastPart : false, smsHeader, encoding, validityPeriod, priority);
        if (submitPdu != null) {
            return getSmsTracker(getSmsTrackerMap(destinationAddress, scAddress, message, submitPdu), sentIntent, deliveryIntent, getFormat(), unsentPartCount, anyPartFailed, messageUri, smsHeader, false, fullMessageText, true, true);
        }
        Rlog.e(TAG, "CDMASMSDispatcher.getNewSubmitPduTracker(), returned null, B");
        return null;
    }

    boolean isSimAbsent() {
        IccCardConstants.State state;
        IccCard card = PhoneFactory.getPhone(this.mPhone.getPhoneId()).getIccCard();
        if (card == null) {
            state = IccCardConstants.State.UNKNOWN;
        } else {
            state = card.getState();
        }
        boolean ret = state == IccCardConstants.State.ABSENT || state == IccCardConstants.State.NOT_READY;
        Rlog.d(TAG, "isSimAbsent state = " + state + " ret=" + ret);
        return ret;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 200:
                AsyncResult ar = (AsyncResult) msg.obj;
                if (ar != null && ar.exception == null && ar.result != null) {
                    try {
                        String data = (String) ar.result;
                        Intent intent = new Intent("android.telephony.sms.CDMA_CARD_ESN_OR_MEID");
                        String[] temp = data.split(",");
                        if (temp.length >= 1) {
                            intent.putExtra("esn_or_meid", temp[0].trim());
                        }
                        if (temp.length >= 2) {
                            intent.putExtra("esn_or_meid2", temp[1].trim());
                        }
                        Rlog.d(TAG, "Broadcast ESN/MEID = " + data);
                        this.mContext.sendBroadcast(intent);
                    } catch (ClassCastException e) {
                        return;
                    }
                    break;
                }
                break;
            default:
                super.handleMessage(msg);
                break;
        }
    }
}
