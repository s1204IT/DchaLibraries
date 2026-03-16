package com.android.internal.telephony;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Telephony;
import android.telephony.Rlog;
import android.telephony.SmsMessage;
import com.android.ims.ImsContentObserver;
import com.android.ims.ImsManager;
import com.android.ims.ImsSms;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.SMSDispatcher;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.cdma.CdmaInboundSmsHandler;
import com.android.internal.telephony.cdma.CdmaSMSDispatcher;
import com.android.internal.telephony.gsm.GsmInboundSmsHandler;
import com.android.internal.telephony.gsm.GsmSMSDispatcher;
import com.android.internal.telephony.imsphone.ImsPhone;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class ImsSMSDispatcher extends SMSDispatcher {
    private static final int EVENT_IMS_SMS_READY = 1000;
    public static final int EVENT_NEW_IMS_SMS = 1001;
    public static final int EVENT_NEW_IMS_SMS_STATUS_REPORT = 1002;
    private static final String PERSIST_MASTER_SIM = "persist.sys.sim2.master.enable";
    private static final String TAG = "RIL_ImsSms";
    private SMSDispatcher mCdmaDispatcher;
    private CdmaInboundSmsHandler mCdmaInboundSmsHandler;
    private SMSDispatcher mGsmDispatcher;
    private GsmInboundSmsHandler mGsmInboundSmsHandler;
    private boolean mIms;
    private ImsContentObserver mImsContentObserver;
    private ImsManager mImsManager;
    private ImsSms mImsSms;
    private String mImsSmsFormat;
    private PhoneBase mPhone;

    public ImsSMSDispatcher(PhoneBase phone, SmsStorageMonitor storageMonitor, SmsUsageMonitor usageMonitor) {
        super(phone, usageMonitor, null);
        this.mImsSms = null;
        this.mImsManager = null;
        this.mIms = false;
        this.mImsSmsFormat = "unknown";
        this.mImsContentObserver = null;
        Rlog.d(TAG, "ImsSMSDispatcher created");
        this.mPhone = phone;
        this.mCdmaDispatcher = new CdmaSMSDispatcher(phone, usageMonitor, this);
        this.mGsmInboundSmsHandler = GsmInboundSmsHandler.makeInboundSmsHandler(phone.getContext(), storageMonitor, phone);
        this.mCdmaInboundSmsHandler = CdmaInboundSmsHandler.makeInboundSmsHandler(phone.getContext(), storageMonitor, phone, (CdmaSMSDispatcher) this.mCdmaDispatcher);
        this.mGsmDispatcher = new GsmSMSDispatcher(phone, usageMonitor, this, this.mGsmInboundSmsHandler);
        Thread broadcastThread = new Thread(new SmsBroadcastUndelivered(phone.getContext(), this.mGsmInboundSmsHandler, this.mCdmaInboundSmsHandler));
        broadcastThread.start();
        this.mCi.registerForOn(this, 11, null);
        int phoneId = this.mPhone.getPhoneId();
        if (phoneId == getMasterSimId()) {
            this.mImsManager = ImsManager.getInstance(this.mPhone.getContext(), phoneId);
            Rlog.d(TAG, "MasterSimId" + phoneId + " ImsManager" + this.mImsManager);
            if (this.mImsManager != null) {
                this.mImsManager.registerForImsSmsReady(this, 1000, (Object) null);
                return;
            } else {
                Rlog.e(TAG, "IMS Manager is null");
                return;
            }
        }
        Rlog.e(TAG, "only support IMS on master SIM, this ImsSMSDispatcher is for " + this.mPhone.getPhoneId());
    }

    private int getMasterSimId() {
        boolean isSim2Master = SystemProperties.getBoolean(PERSIST_MASTER_SIM, false);
        return isSim2Master ? 1 : 0;
    }

    @Override
    protected void updatePhoneObject(PhoneBase phone) {
        Rlog.d(TAG, "In IMS updatePhoneObject ");
        super.updatePhoneObject(phone);
        this.mCdmaDispatcher.updatePhoneObject(phone);
        this.mGsmDispatcher.updatePhoneObject(phone);
        this.mGsmInboundSmsHandler.updatePhoneObject(phone);
        this.mCdmaInboundSmsHandler.updatePhoneObject(phone);
    }

    @Override
    public void dispose() {
        this.mCi.unregisterForOn(this);
        Phone imsPhone = this.mPhone.getImsPhone();
        if (imsPhone != null && (imsPhone instanceof ImsPhone)) {
            ((ImsPhone) imsPhone).unregisterForImsStateChanged(this);
        }
        if (this.mImsManager != null) {
            this.mImsManager.unregisterForImsSmsReady(this);
        }
        if (this.mImsSms != null) {
            this.mImsSms.unSetOnImsSmsStatus(this.mGsmDispatcher);
            this.mImsSms.unSetOnNewImsGsmSms(this.mGsmDispatcher);
            this.mImsSms.unSetOnNewImsCdmaSms(this.mCdmaDispatcher);
        }
        this.mGsmDispatcher.dispose();
        this.mCdmaDispatcher.dispose();
        this.mGsmInboundSmsHandler.dispose();
        this.mCdmaInboundSmsHandler.dispose();
    }

    public CdmaInboundSmsHandler getCdmaInboundSmshandler() {
        return this.mCdmaInboundSmsHandler;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 11:
            case 12:
                updateImsRegistrationState();
                break;
            case 13:
                AsyncResult ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    updateImsInfo(ar);
                } else {
                    Rlog.e(TAG, "IMS State query failed with exp " + ar.exception);
                }
                break;
            case 1000:
                Phone imsPhone = this.mPhone.getImsPhone();
                if (imsPhone != null && (imsPhone instanceof ImsPhone)) {
                    updateImsRegistrationState();
                    ((ImsPhone) imsPhone).registerForImsStateChanged(this, 12, null);
                    break;
                }
                break;
            case 1010:
                String result = (String) msg.obj;
                Rlog.d(TAG, "MSG_SMS_FORMAT_UPDATE result= " + result);
                getMoSmsFormatFromDataBase();
                break;
            default:
                super.handleMessage(msg);
                break;
        }
    }

    private void setImsSmsFormat(int format) {
        switch (format) {
            case 1:
                this.mImsSmsFormat = SmsMessage.FORMAT_3GPP;
                break;
            case 2:
                this.mImsSmsFormat = SmsMessage.FORMAT_3GPP2;
                break;
            default:
                this.mImsSmsFormat = "unknown";
                break;
        }
    }

    private void updateImsInfo(AsyncResult ar) {
        int[] responseArray = (int[]) ar.result;
        this.mIms = false;
        if (responseArray[0] == 1) {
            Rlog.d(TAG, "IMS is registered!");
            this.mIms = true;
        } else {
            Rlog.d(TAG, "IMS is NOT registered!");
        }
        setImsSmsFormat(responseArray[1]);
        if ("unknown".equals(this.mImsSmsFormat)) {
            Rlog.e(TAG, "IMS format was unknown!");
            this.mIms = false;
        }
    }

    @Override
    protected void sendData(String destAddr, String scAddr, int destPort, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        if (isCdmaMo()) {
            this.mCdmaDispatcher.sendData(destAddr, scAddr, destPort, data, sentIntent, deliveryIntent);
        } else {
            this.mGsmDispatcher.sendData(destAddr, scAddr, destPort, data, sentIntent, deliveryIntent);
        }
    }

    @Override
    protected void sendMultipartText(String destAddr, String scAddr, ArrayList<String> parts, ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents, Uri messageUri, String callingPkg) {
        if (isCdmaMo()) {
            this.mCdmaDispatcher.sendMultipartText(destAddr, scAddr, parts, sentIntents, deliveryIntents, messageUri, callingPkg);
        } else {
            this.mGsmDispatcher.sendMultipartText(destAddr, scAddr, parts, sentIntents, deliveryIntents, messageUri, callingPkg);
        }
    }

    @Override
    protected void sendSms(SMSDispatcher.SmsTracker tracker) {
        Rlog.e(TAG, "sendSms should never be called from here!");
    }

    @Override
    protected void sendSmsByPstn(SMSDispatcher.SmsTracker tracker) {
        Rlog.e(TAG, "sendSmsByPstn should never be called from here!");
    }

    @Override
    protected void sendText(String destAddr, String scAddr, String text, PendingIntent sentIntent, PendingIntent deliveryIntent, Uri messageUri, String callingPkg) {
        Rlog.d(TAG, "sendText");
        if (isCdmaMo()) {
            this.mCdmaDispatcher.sendText(destAddr, scAddr, text, sentIntent, deliveryIntent, messageUri, callingPkg);
        } else {
            this.mGsmDispatcher.sendText(destAddr, scAddr, text, sentIntent, deliveryIntent, messageUri, callingPkg);
        }
    }

    @Override
    protected void injectSmsPdu(byte[] pdu, String format, PendingIntent receivedIntent) {
        Rlog.d(TAG, "ImsSMSDispatcher:injectSmsPdu");
        try {
            SmsMessage msg = SmsMessage.createFromPdu(pdu, format);
            if (msg.getMessageClass() != SmsMessage.MessageClass.CLASS_1) {
                if (receivedIntent != null) {
                    receivedIntent.send(2);
                }
            } else {
                AsyncResult ar = new AsyncResult(receivedIntent, msg, (Throwable) null);
                if (format.equals(SmsMessage.FORMAT_3GPP)) {
                    Rlog.i(TAG, "ImsSMSDispatcher:injectSmsText Sending msg=" + msg + ", format=" + format + "to mGsmInboundSmsHandler");
                    this.mGsmInboundSmsHandler.sendMessage(8, ar);
                } else if (format.equals(SmsMessage.FORMAT_3GPP2)) {
                    Rlog.i(TAG, "ImsSMSDispatcher:injectSmsText Sending msg=" + msg + ", format=" + format + "to mCdmaInboundSmsHandler");
                    this.mCdmaInboundSmsHandler.sendMessage(8, ar);
                } else {
                    Rlog.e(TAG, "Invalid pdu format: " + format);
                    if (receivedIntent != null) {
                        receivedIntent.send(2);
                    }
                }
            }
        } catch (Exception e) {
            Rlog.e(TAG, "injectSmsPdu failed: ", e);
            if (receivedIntent != null) {
                try {
                    receivedIntent.send(2);
                } catch (PendingIntent.CanceledException e2) {
                }
            }
        }
    }

    @Override
    public void sendRetrySms(SMSDispatcher.SmsTracker tracker) {
        String oldFormat = tracker.mFormat;
        String newFormat = 2 == this.mPhone.getPhoneType() ? this.mCdmaDispatcher.getFormat() : this.mGsmDispatcher.getFormat();
        if (oldFormat.equals(newFormat)) {
            if (isCdmaFormat(newFormat)) {
                Rlog.d(TAG, "old format matched new format (cdma)");
                this.mCdmaDispatcher.sendSms(tracker);
                return;
            } else {
                Rlog.d(TAG, "old format matched new format (gsm)");
                this.mGsmDispatcher.sendSms(tracker);
                return;
            }
        }
        HashMap<String, Object> map = tracker.mData;
        if (!map.containsKey("scAddr") || !map.containsKey("destAddr") || (!map.containsKey(Telephony.Mms.Part.TEXT) && (!map.containsKey("data") || !map.containsKey("destPort")))) {
            Rlog.e(TAG, "sendRetrySms failed to re-encode per missing fields!");
            tracker.onFailed(this.mContext, 1, 0);
            return;
        }
        String scAddr = (String) map.get("scAddr");
        String destAddr = (String) map.get("destAddr");
        SmsMessageBase.SubmitPduBase pdu = null;
        if (map.containsKey(Telephony.Mms.Part.TEXT)) {
            Rlog.d(TAG, "sms failed was text");
            String text = (String) map.get(Telephony.Mms.Part.TEXT);
            if (isCdmaFormat(newFormat)) {
                Rlog.d(TAG, "old format (gsm) ==> new format (cdma)over ims?" + isIms());
                if (isIms()) {
                    int encoding = getMoSmsEncodingFromDataBase(SmsMessage.FORMAT_3GPP2);
                    pdu = com.android.internal.telephony.cdma.SmsMessage.getSubmitPdu(scAddr, destAddr, text, tracker.mDeliveryIntent != null, null, encoding);
                } else {
                    pdu = com.android.internal.telephony.cdma.SmsMessage.getSubmitPdu(scAddr, destAddr, text, tracker.mDeliveryIntent != null, (SmsHeader) null);
                }
            } else {
                Rlog.d(TAG, "old format (cdma) ==> new format (gsm) over ims?" + isIms());
                if (isIms()) {
                    int encoding2 = getMoSmsEncodingFromDataBase(SmsMessage.FORMAT_3GPP);
                    pdu = com.android.internal.telephony.gsm.SmsMessage.getSubmitPdu(scAddr, destAddr, text, tracker.mDeliveryIntent != null, null, encoding2, 0, 0);
                } else {
                    pdu = com.android.internal.telephony.gsm.SmsMessage.getSubmitPdu(scAddr, destAddr, text, tracker.mDeliveryIntent != null, (byte[]) null);
                }
            }
        } else if (map.containsKey("data")) {
            Rlog.d(TAG, "sms failed was data");
            byte[] data = (byte[]) map.get("data");
            Integer destPort = (Integer) map.get("destPort");
            if (isCdmaFormat(newFormat)) {
                Rlog.d(TAG, "old format (gsm) ==> new format (cdma)");
                pdu = com.android.internal.telephony.cdma.SmsMessage.getSubmitPdu(scAddr, destAddr, destPort.intValue(), data, tracker.mDeliveryIntent != null);
            } else {
                Rlog.d(TAG, "old format (cdma) ==> new format (gsm)");
                pdu = com.android.internal.telephony.gsm.SmsMessage.getSubmitPdu(scAddr, destAddr, destPort.intValue(), data, tracker.mDeliveryIntent != null);
            }
        }
        map.put("smsc", pdu.encodedScAddress);
        map.put("pdu", pdu.encodedMessage);
        SMSDispatcher dispatcher = isCdmaFormat(newFormat) ? this.mCdmaDispatcher : this.mGsmDispatcher;
        tracker.mFormat = dispatcher.getFormat();
        dispatcher.sendSms(tracker);
    }

    @Override
    protected void sendSubmitPdu(SMSDispatcher.SmsTracker tracker) {
        sendRawPdu(tracker);
    }

    @Override
    protected String getFormat() {
        Rlog.e(TAG, "getFormat should never be called from here!");
        return "unknown";
    }

    @Override
    protected GsmAlphabet.TextEncodingDetails calculateLength(CharSequence messageBody, boolean use7bitOnly) {
        Rlog.e(TAG, "Error! Not implemented for IMS.");
        return null;
    }

    @Override
    protected SMSDispatcher.SmsTracker getNewSubmitPduTracker(String destinationAddress, String scAddress, String message, SmsHeader smsHeader, int format, PendingIntent sentIntent, PendingIntent deliveryIntent, boolean lastPart, AtomicInteger unsentPartCount, AtomicBoolean anyPartFailed, Uri messageUri, String fullMessageText) {
        Rlog.e(TAG, "Error! Not implemented for IMS.");
        return null;
    }

    @Override
    public boolean isIms() {
        boolean isSmsOverImsEnabled = true;
        if (this.mPhone.getDomainSelection() != null) {
            isSmsOverImsEnabled = this.mPhone.getDomainSelection().smsOverImsEnabled();
        }
        return this.mIms && isSmsOverImsEnabled;
    }

    @Override
    public String getImsSmsFormat() {
        return this.mImsSmsFormat;
    }

    private boolean isCdmaMo() {
        if (isIms()) {
            return isCdmaFormat(this.mImsSmsFormat);
        }
        return 2 == this.mPhone.getPhoneType();
    }

    private boolean isCdmaFormat(String format) {
        return this.mCdmaDispatcher.getFormat().equals(format);
    }

    private void getMoSmsFormatFromDataBase() {
        ContentResolver cr = this.mContext.getContentResolver();
        Cursor c = cr.query(ImsContentObserver.ims_uri, null, "key='KEY_MO_SMS_FORMAT'", null, null);
        if (c != null && c.getCount() > 0) {
            if (c.moveToFirst()) {
                this.mImsSmsFormat = c.getString(2);
            }
            c.close();
        } else {
            this.mImsSmsFormat = "unknown";
        }
        Rlog.d(TAG, "getMoSmsFormatFromDataBase mImsSmsFormat= " + this.mImsSmsFormat);
    }

    public int getMoSmsEncodingFromDataBase(String format) {
        String encoding = null;
        int ret = -1;
        ContentResolver cr = this.mContext.getContentResolver();
        Cursor c = cr.query(ImsContentObserver.ims_uri, null, "key='KEY_MO_SMS_ENCODING'", null, null);
        if (c != null && c.getCount() > 0) {
            if (c.moveToFirst()) {
                encoding = c.getString(2);
            }
            c.close();
        }
        if (format == SmsMessage.FORMAT_3GPP) {
            switch (encoding) {
                case "gsm7":
                    ret = 1;
                    break;
                case "ascii7":
                    ret = 0;
                    break;
                case "ucs2":
                    ret = 3;
                    break;
                default:
                    ret = 0;
                    break;
            }
        } else if (format == SmsMessage.FORMAT_3GPP2) {
            switch (encoding) {
                case "gsm7":
                    ret = 9;
                    break;
                case "ascii7":
                    ret = 2;
                    break;
                case "ucs2":
                    ret = 4;
                    break;
                default:
                    ret = -1;
                    break;
            }
        } else {
            Rlog.e(TAG, "getMoSmsEncodingFromDataBase format error= " + format);
        }
        Rlog.d(TAG, "getMoSmsEncodingFromDataBase format= " + format + " encoding=" + ret);
        return ret;
    }

    private void registerContentObservers() {
        if (this.mImsContentObserver == null) {
            this.mImsContentObserver = new ImsContentObserver(this.mContext, this);
            this.mContext.getContentResolver().registerContentObserver(ImsContentObserver.imsSms_uri, false, this.mImsContentObserver);
            Rlog.d(TAG, "registerContentObservers mImsContentObserver= " + this.mImsContentObserver);
            return;
        }
        Rlog.d(TAG, "mImsContentObserver= " + this.mImsContentObserver + "already exist");
    }

    private void unregisterContentObservers() {
        if (this.mImsContentObserver != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mImsContentObserver);
            this.mImsContentObserver = null;
        }
    }

    private void updateImsRegistrationState() {
        Rlog.d(TAG, "updateImsRegistrationState");
        if (this.mImsManager != null) {
            this.mImsSms = this.mImsManager.getSmsInterface();
        }
        if (this.mImsSms != null) {
            Phone imsPhone = this.mPhone.getImsPhone();
            if (imsPhone == null) {
                Rlog.e(TAG, "unknown exception happen, IMS phone crash");
                return;
            }
            if (imsPhone.getServiceState().getState() == 0) {
                this.mIms = true;
                getMoSmsFormatFromDataBase();
                this.mImsSms.setOnImsSmsStatus(this.mGsmDispatcher, 1002, (Object) null);
                this.mImsSms.setOnNewImsGsmSms(this.mGsmDispatcher, 1001, (Object) null);
                this.mImsSms.setOnNewImsCdmaSms(this.mCdmaDispatcher, 1001, (Object) null);
                registerContentObservers();
                return;
            }
            this.mIms = false;
            this.mImsSmsFormat = "unknown";
            unregisterContentObservers();
        }
    }

    public ImsSms getImsSms() {
        return this.mImsSms;
    }
}
