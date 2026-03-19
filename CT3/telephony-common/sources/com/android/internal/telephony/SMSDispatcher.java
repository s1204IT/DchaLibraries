package com.android.internal.telephony;

import android.R;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.Telephony;
import android.service.carrier.ICarrierMessagingCallback;
import android.service.carrier.ICarrierMessagingService;
import android.telephony.CarrierMessagingServiceManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.EventLog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccController;
import com.mediatek.internal.telephony.ppl.IPplSmsFilter;
import com.mediatek.internal.telephony.ppl.PplSmsFilterExtension;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class SMSDispatcher extends Handler {
    protected static final int EVENT_ACTIVATE_CB_COMPLETE = 101;
    private static final int EVENT_CONFIRM_SEND_TO_POSSIBLE_PREMIUM_SHORT_CODE = 8;
    private static final int EVENT_CONFIRM_SEND_TO_PREMIUM_SHORT_CODE = 9;
    protected static final int EVENT_COPY_TEXT_MESSAGE_DONE = 106;
    protected static final int EVENT_DELAY_SEND_MESSAGE_QUEUE = 107;
    protected static final int EVENT_GET_CB_CONFIG_COMPLETE = 102;
    protected static final int EVENT_HANDLE_STATUS_REPORT = 10;
    protected static final int EVENT_ICC_CHANGED = 15;
    protected static final int EVENT_IMS_STATE_CHANGED = 12;
    protected static final int EVENT_IMS_STATE_DONE = 13;
    protected static final int EVENT_NEW_ICC_SMS = 14;
    protected static final int EVENT_QUERY_CB_ACTIVATION_COMPLETE = 104;
    protected static final int EVENT_RADIO_ON = 11;
    static final int EVENT_SEND_CONFIRMED_SMS = 5;
    private static final int EVENT_SEND_LIMIT_REACHED_CONFIRMATION = 4;
    private static final int EVENT_SEND_RETRY = 3;
    protected static final int EVENT_SEND_SMS_COMPLETE = 2;
    protected static final int EVENT_SET_CB_CONFIG_COMPLETE = 103;
    protected static final int EVENT_SMS_READY = 105;
    static final int EVENT_STOP_SENDING = 7;
    private static final float MAX_LABEL_SIZE_PX = 500.0f;
    private static final int MAX_SEND_RETRIES = 3;
    private static final int MO_MSG_QUEUE_LIMIT = 5;
    private static final int PREMIUM_RULE_USE_BOTH = 3;
    private static final int PREMIUM_RULE_USE_NETWORK = 2;
    private static final int PREMIUM_RULE_USE_SIM = 1;
    private static final int RESULT_ERROR_RUIM_PLUG_OUT = 107;
    private static final String SEND_NEXT_MSG_EXTRA = "SendNextMsg";
    private static final int SEND_RETRY_DELAY = 2000;
    private static final int SINGLE_PART_SMS = 1;
    static final String TAG = "SMSDispatcher";
    protected static final int WAKE_LOCK_TIMEOUT = 500;
    protected final CommandsInterface mCi;
    protected final Context mContext;
    private ImsSMSDispatcher mImsSMSDispatcher;
    private int mPendingTrackerCount;
    protected Phone mPhone;
    protected final ContentResolver mResolver;
    private final SettingsObserver mSettingsObserver;
    protected boolean mSmsCapable;
    protected boolean mSmsSendDisabled;
    protected final TelephonyManager mTelephonyManager;
    private SmsUsageMonitor mUsageMonitor;
    protected PowerManager.WakeLock mWakeLock;
    private static final boolean ENG = "eng".equals(Build.TYPE);
    private static int sConcatenatedRef = new Random().nextInt(256);
    static final boolean DBG = false;
    protected static boolean isDmLock = DBG;
    protected static String PDU_SIZE = "pdu_size";
    protected static String MSG_REF_NUM = "msg_ref_num";
    private final AtomicInteger mPremiumSmsRule = new AtomicInteger(1);
    protected ArrayList<SmsTracker> mSTrackersQueue = new ArrayList<>(5);
    protected boolean mStorageAvailable = true;
    protected boolean mSmsReady = DBG;
    protected int messageCountNeedCopy = 0;
    protected Object mLock = new Object();
    protected boolean mSuccess = true;
    protected final ArrayList<SmsTracker> deliveryPendingList = new ArrayList<>();
    private BroadcastReceiver mDMLockReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Rlog.d(SMSDispatcher.TAG, "[DM-Lock receive lock/unlock intent");
            if (intent.getAction() == null) {
                return;
            }
            if (intent.getAction().equals("com.mediatek.dm.LAWMO_LOCK")) {
                Rlog.d(SMSDispatcher.TAG, "[DM-Lock DM is locked now");
                SMSDispatcher.isDmLock = true;
            } else {
                if (!intent.getAction().equals("com.mediatek.dm.LAWMO_UNLOCK")) {
                    return;
                }
                Rlog.d(SMSDispatcher.TAG, "[DM-Lock DM is unlocked now");
                SMSDispatcher.isDmLock = SMSDispatcher.DBG;
            }
        }
    };

    protected abstract GsmAlphabet.TextEncodingDetails calculateLength(CharSequence charSequence, boolean z);

    protected abstract String getFormat();

    protected abstract SmsTracker getNewSubmitPduTracker(String str, String str2, String str3, SmsHeader smsHeader, int i, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z, AtomicInteger atomicInteger, AtomicBoolean atomicBoolean, Uri uri, String str4);

    protected abstract void injectSmsPdu(byte[] bArr, String str, PendingIntent pendingIntent);

    protected abstract void sendData(String str, String str2, int i, byte[] bArr, PendingIntent pendingIntent, PendingIntent pendingIntent2);

    protected abstract void sendSms(SmsTracker smsTracker);

    protected abstract void sendSmsByPstn(SmsTracker smsTracker);

    protected abstract void sendSubmitPdu(SmsTracker smsTracker);

    protected abstract void sendText(String str, String str2, String str3, PendingIntent pendingIntent, PendingIntent pendingIntent2, Uri uri, String str4, boolean z);

    protected static int getNextConcatenatedRef() {
        sConcatenatedRef++;
        return sConcatenatedRef;
    }

    protected SMSDispatcher(Phone phone, SmsUsageMonitor usageMonitor, ImsSMSDispatcher imsSMSDispatcher) {
        this.mSmsCapable = true;
        this.mPhone = phone;
        this.mImsSMSDispatcher = imsSMSDispatcher;
        this.mContext = phone.getContext();
        this.mResolver = this.mContext.getContentResolver();
        this.mCi = phone.mCi;
        this.mUsageMonitor = usageMonitor;
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        this.mSettingsObserver = new SettingsObserver(this, this.mPremiumSmsRule, this.mContext);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("sms_short_code_rule"), DBG, this.mSettingsObserver);
        this.mSmsCapable = this.mContext.getResources().getBoolean(R.^attr-private.fromLeft);
        this.mSmsSendDisabled = !this.mTelephonyManager.getSmsSendCapableForPhone(this.mPhone.getPhoneId(), this.mSmsCapable);
        Rlog.d(TAG, "SMSDispatcher: ctor mSmsCapable=" + this.mSmsCapable + " format=" + getFormat() + " mSmsSendDisabled=" + this.mSmsSendDisabled);
        createWakelock();
        this.mCi.registerForSmsReady(this, 105, null);
        IntentFilter dmFilter = new IntentFilter();
        dmFilter.addAction("com.mediatek.dm.LAWMO_LOCK");
        dmFilter.addAction("com.mediatek.dm.LAWMO_UNLOCK");
        this.mContext.registerReceiver(this.mDMLockReceiver, dmFilter);
    }

    private static class SettingsObserver extends ContentObserver {
        private final Context mContext;
        private final AtomicInteger mPremiumSmsRule;

        SettingsObserver(Handler handler, AtomicInteger premiumSmsRule, Context context) {
            super(handler);
            this.mPremiumSmsRule = premiumSmsRule;
            this.mContext = context;
            onChange(SMSDispatcher.DBG);
        }

        @Override
        public void onChange(boolean selfChange) {
            this.mPremiumSmsRule.set(Settings.Global.getInt(this.mContext.getContentResolver(), "sms_short_code_rule", 1));
        }
    }

    protected void updatePhoneObject(Phone phone) {
        this.mPhone = phone;
        this.mUsageMonitor = phone.mSmsUsageMonitor;
        Rlog.d(TAG, "Active phone changed to " + this.mPhone.getPhoneName());
    }

    public void dispose() {
        this.mContext.getContentResolver().unregisterContentObserver(this.mSettingsObserver);
    }

    protected void handleStatusReport(Object o) {
        Rlog.d(TAG, "handleStatusReport() called with no subclass.");
    }

    protected void finalize() throws Throwable {
        super.finalize();
        Rlog.d(TAG, "SMSDispatcher finalized");
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 2:
                handleSendComplete((AsyncResult) msg.obj);
                return;
            case 3:
                Rlog.d(TAG, "SMS retry..");
                sendRetrySms((SmsTracker) msg.obj);
                return;
            case 4:
                handleReachSentLimit((SmsTracker) msg.obj);
                return;
            case 5:
                SmsTracker tracker = (SmsTracker) msg.obj;
                if (tracker.isMultipart()) {
                    sendMultipartSms(tracker);
                } else {
                    if (this.mPendingTrackerCount > 1) {
                        tracker.mExpectMore = true;
                    } else {
                        tracker.mExpectMore = DBG;
                    }
                    sendSms(tracker);
                }
                this.mPendingTrackerCount--;
                return;
            case 7:
                ((SmsTracker) msg.obj).onFailed(this.mContext, 5, 0);
                this.mPendingTrackerCount--;
                return;
            case 8:
                handleConfirmShortCode(DBG, (SmsTracker) msg.obj);
                return;
            case 9:
                handleConfirmShortCode(true, (SmsTracker) msg.obj);
                return;
            case 10:
                handleStatusReport(msg.obj);
                return;
            case 101:
            case 102:
            case EVENT_SET_CB_CONFIG_COMPLETE:
                AsyncResult ar = (AsyncResult) msg.obj;
                AsyncResult.forMessage((Message) ar.userObj, ar.result, ar.exception);
                ((Message) ar.userObj).sendToTarget();
                return;
            case 104:
                handleQueryCbActivation((AsyncResult) msg.obj);
                return;
            case 105:
                Rlog.d(TAG, "SMS is ready, Phone: " + this.mPhone.getPhoneId());
                this.mSmsReady = true;
                notifySmsReady(this.mSmsReady);
                return;
            case 106:
                AsyncResult ar2 = (AsyncResult) msg.obj;
                synchronized (this.mLock) {
                    this.mSuccess = ar2.exception == null;
                    if (this.mSuccess) {
                        Rlog.d(TAG, "[copyText success to copy one");
                        this.messageCountNeedCopy--;
                    } else {
                        Rlog.d(TAG, "[copyText fail to copy one");
                        this.messageCountNeedCopy = 0;
                    }
                    this.mLock.notifyAll();
                }
                return;
            case 107:
                Rlog.d(TAG, "EVENT_DELAY_SEND_MESSAGE_QUEUE: " + msg.obj);
                handleSendNextTracker((SmsTracker) msg.obj);
                return;
            default:
                Rlog.e(TAG, "handleMessage() ignoring message of unexpected type " + msg.what);
                return;
        }
    }

    protected abstract class SmsSender extends CarrierMessagingServiceManager {
        protected volatile SmsSenderCallback mSenderCallback;
        protected final SmsTracker mTracker;

        protected SmsSender(SmsTracker tracker) {
            this.mTracker = tracker;
        }

        public void sendSmsByCarrierApp(String carrierPackageName, SmsSenderCallback senderCallback) {
            this.mSenderCallback = senderCallback;
            if (!bindToCarrierMessagingService(SMSDispatcher.this.mContext, carrierPackageName)) {
                Rlog.e(SMSDispatcher.TAG, "bindService() for carrier messaging service failed");
                this.mSenderCallback.onSendSmsComplete(1, 0);
            } else {
                Rlog.d(SMSDispatcher.TAG, "bindService() for carrier messaging service succeeded");
            }
        }
    }

    private static int getSendSmsFlag(PendingIntent deliveryIntent) {
        if (deliveryIntent == null) {
            return 0;
        }
        return 1;
    }

    protected final class TextSmsSender extends SmsSender {
        public TextSmsSender(SmsTracker tracker) {
            super(tracker);
        }

        @Override
        protected void onServiceReady(ICarrierMessagingService carrierMessagingService) {
            HashMap<String, Object> map = this.mTracker.getData();
            String text = (String) map.get("text");
            if (text != null) {
                try {
                    carrierMessagingService.sendTextSms(text, SMSDispatcher.this.getSubId(), this.mTracker.mDestAddress, SMSDispatcher.getSendSmsFlag(this.mTracker.mDeliveryIntent), this.mSenderCallback);
                    return;
                } catch (RemoteException e) {
                    Rlog.e(SMSDispatcher.TAG, "Exception sending the SMS: " + e);
                    this.mSenderCallback.onSendSmsComplete(1, 0);
                    return;
                }
            }
            this.mSenderCallback.onSendSmsComplete(1, 0);
        }
    }

    protected final class DataSmsSender extends SmsSender {
        public DataSmsSender(SmsTracker tracker) {
            super(tracker);
        }

        @Override
        protected void onServiceReady(ICarrierMessagingService carrierMessagingService) {
            HashMap<String, Object> map = this.mTracker.getData();
            byte[] data = (byte[]) map.get("data");
            int destPort = ((Integer) map.get("destPort")).intValue();
            if (data != null) {
                try {
                    carrierMessagingService.sendDataSms(data, SMSDispatcher.this.getSubId(), this.mTracker.mDestAddress, destPort, SMSDispatcher.getSendSmsFlag(this.mTracker.mDeliveryIntent), this.mSenderCallback);
                    return;
                } catch (RemoteException e) {
                    Rlog.e(SMSDispatcher.TAG, "Exception sending the SMS: " + e);
                    this.mSenderCallback.onSendSmsComplete(1, 0);
                    return;
                }
            }
            this.mSenderCallback.onSendSmsComplete(1, 0);
        }
    }

    protected final class SmsSenderCallback extends ICarrierMessagingCallback.Stub {
        private final SmsSender mSmsSender;

        public SmsSenderCallback(SmsSender smsSender) {
            this.mSmsSender = smsSender;
        }

        public void onSendSmsComplete(int result, int messageRef) {
            SMSDispatcher.this.checkCallerIsPhoneOrCarrierApp();
            long identity = Binder.clearCallingIdentity();
            try {
                this.mSmsSender.disposeConnection(SMSDispatcher.this.mContext);
                SMSDispatcher.this.processSendSmsResponse(this.mSmsSender.mTracker, result, messageRef);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void onSendMultipartSmsComplete(int result, int[] messageRefs) {
            Rlog.e(SMSDispatcher.TAG, "Unexpected onSendMultipartSmsComplete call with result: " + result);
        }

        public void onFilterComplete(int result) {
            Rlog.e(SMSDispatcher.TAG, "Unexpected onFilterComplete call with result: " + result);
        }

        public void onSendMmsComplete(int result, byte[] sendConfPdu) {
            Rlog.e(SMSDispatcher.TAG, "Unexpected onSendMmsComplete call with result: " + result);
        }

        public void onDownloadMmsComplete(int result) {
            Rlog.e(SMSDispatcher.TAG, "Unexpected onDownloadMmsComplete call with result: " + result);
        }
    }

    private void processSendSmsResponse(SmsTracker tracker, int result, int messageRef) {
        if (tracker == null) {
            Rlog.e(TAG, "processSendSmsResponse: null tracker");
        }
        SmsResponse smsResponse = new SmsResponse(messageRef, null, -1);
        switch (result) {
            case 0:
                Rlog.d(TAG, "Sending SMS by IP succeeded.");
                sendMessage(obtainMessage(2, new AsyncResult(tracker, smsResponse, (Throwable) null)));
                break;
            case 1:
                Rlog.d(TAG, "Sending SMS by IP failed. Retry on carrier network.");
                sendSubmitPdu(tracker);
                break;
            case 2:
                Rlog.d(TAG, "Sending SMS by IP failed.");
                sendMessage(obtainMessage(2, new AsyncResult(tracker, smsResponse, new CommandException(CommandException.Error.GENERIC_FAILURE))));
                break;
            default:
                Rlog.d(TAG, "Unknown result " + result + " Retry on carrier network.");
                sendSubmitPdu(tracker);
                break;
        }
    }

    protected final class MultipartSmsSender extends CarrierMessagingServiceManager {
        private final List<String> mParts;
        private volatile MultipartSmsSenderCallback mSenderCallback;
        public final SmsTracker[] mTrackers;

        public MultipartSmsSender(ArrayList<String> parts, SmsTracker[] trackers) {
            this.mParts = parts;
            this.mTrackers = trackers;
        }

        public void sendSmsByCarrierApp(String carrierPackageName, MultipartSmsSenderCallback senderCallback) {
            this.mSenderCallback = senderCallback;
            if (!bindToCarrierMessagingService(SMSDispatcher.this.mContext, carrierPackageName)) {
                Rlog.e(SMSDispatcher.TAG, "bindService() for carrier messaging service failed");
                this.mSenderCallback.onSendMultipartSmsComplete(1, null);
            } else {
                Rlog.d(SMSDispatcher.TAG, "bindService() for carrier messaging service succeeded");
            }
        }

        @Override
        protected void onServiceReady(ICarrierMessagingService carrierMessagingService) {
            try {
                carrierMessagingService.sendMultipartTextSms(this.mParts, SMSDispatcher.this.getSubId(), this.mTrackers[0].mDestAddress, SMSDispatcher.getSendSmsFlag(this.mTrackers[0].mDeliveryIntent), this.mSenderCallback);
            } catch (RemoteException e) {
                Rlog.e(SMSDispatcher.TAG, "Exception sending the SMS: " + e);
                this.mSenderCallback.onSendMultipartSmsComplete(1, null);
            }
        }
    }

    protected final class MultipartSmsSenderCallback extends ICarrierMessagingCallback.Stub {
        private final MultipartSmsSender mSmsSender;

        public MultipartSmsSenderCallback(MultipartSmsSender smsSender) {
            this.mSmsSender = smsSender;
        }

        public void onSendSmsComplete(int result, int messageRef) {
            Rlog.e(SMSDispatcher.TAG, "Unexpected onSendSmsComplete call with result: " + result);
        }

        public void onSendMultipartSmsComplete(int result, int[] messageRefs) {
            this.mSmsSender.disposeConnection(SMSDispatcher.this.mContext);
            if (this.mSmsSender.mTrackers == null) {
                Rlog.e(SMSDispatcher.TAG, "Unexpected onSendMultipartSmsComplete call with null trackers.");
                return;
            }
            SMSDispatcher.this.checkCallerIsPhoneOrCarrierApp();
            long identity = Binder.clearCallingIdentity();
            for (int i = 0; i < this.mSmsSender.mTrackers.length; i++) {
                try {
                    int messageRef = 0;
                    if (messageRefs != null && messageRefs.length > i) {
                        messageRef = messageRefs[i];
                    }
                    SMSDispatcher.this.processSendSmsResponse(this.mSmsSender.mTrackers[i], result, messageRef);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void onFilterComplete(int result) {
            Rlog.e(SMSDispatcher.TAG, "Unexpected onFilterComplete call with result: " + result);
        }

        public void onSendMmsComplete(int result, byte[] sendConfPdu) {
            Rlog.e(SMSDispatcher.TAG, "Unexpected onSendMmsComplete call with result: " + result);
        }

        public void onDownloadMmsComplete(int result) {
            Rlog.e(SMSDispatcher.TAG, "Unexpected onDownloadMmsComplete call with result: " + result);
        }
    }

    protected void handleSendComplete(AsyncResult ar) {
        int errorCode;
        SmsTracker tracker = (SmsTracker) ar.userObj;
        PendingIntent pendingIntent = tracker.mSentIntent;
        handleSendNextTracker(tracker);
        if (ar.result != null) {
            tracker.mMessageRef = ((SmsResponse) ar.result).mMessageRef;
        } else {
            Rlog.d(TAG, "SmsResponse was null");
        }
        if (ar.exception == null) {
            if (tracker.mDeliveryIntent != null) {
                this.deliveryPendingList.add(tracker);
            }
            tracker.onSent(this.mContext);
            return;
        }
        int ss = this.mPhone.getServiceState().getState();
        if (2 == this.mPhone.getPhoneType() && ar.result != null && (errorCode = ((SmsResponse) ar.result).mErrorCode) == 107) {
            Rlog.d(TAG, "RUIM card is plug out");
            tracker.onFailed(this.mContext, 1, errorCode);
            return;
        }
        if (tracker.mImsRetry > 0 && ss != 0) {
            tracker.mRetryCount = 3;
            Rlog.d(TAG, "handleSendComplete: Skipping retry:  isIms()=" + isIms() + " mRetryCount=" + tracker.mRetryCount + " mImsRetry=" + tracker.mImsRetry + " mMessageRef=" + tracker.mMessageRef + " SS= " + this.mPhone.getServiceState().getState());
        }
        if (!isIms() && ss != 0) {
            tracker.onFailed(this.mContext, getNotInServiceError(ss), 0);
            return;
        }
        int errorCode2 = ar.result != null ? ((SmsResponse) ar.result).mErrorCode : 0;
        int error = ((CommandException) ar.exception).getCommandError() == CommandException.Error.FDN_CHECK_FAILURE ? 6 : 1;
        tracker.onFailed(this.mContext, error, errorCode2);
    }

    protected static void handleNotInService(int ss, PendingIntent sentIntent) {
        if (sentIntent != null) {
            try {
                if (ss == 3) {
                    sentIntent.send(2);
                } else {
                    sentIntent.send(4);
                }
                return;
            } catch (PendingIntent.CanceledException e) {
                Rlog.d(TAG, "CanceledException happenedwhen send sms fail with sentIntent due to no service");
                return;
            }
        }
        Rlog.d(TAG, "Send sms fail without sentIntent due to no service");
    }

    protected static int getNotInServiceError(int ss) {
        if (ss == 3) {
            return 2;
        }
        return 4;
    }

    protected void sendMultipartText(String destAddr, String scAddr, ArrayList<String> parts, ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents, Uri messageUri, String callingPkg, boolean persistMessage) {
        String fullMessageText = getMultipartMessageText(parts);
        int refNumber = getNextConcatenatedRef() & 255;
        int msgCount = parts.size();
        int encoding = 0;
        GsmAlphabet.TextEncodingDetails[] encodingForParts = new GsmAlphabet.TextEncodingDetails[msgCount];
        for (int i = 0; i < msgCount; i++) {
            GsmAlphabet.TextEncodingDetails details = calculateLength(parts.get(i), DBG);
            if (encoding != details.codeUnitSize && (encoding == 0 || encoding == 1)) {
                encoding = details.codeUnitSize;
            }
            encodingForParts[i] = details;
        }
        SmsTracker[] trackers = new SmsTracker[msgCount];
        AtomicInteger unsentPartCount = new AtomicInteger(msgCount);
        AtomicBoolean anyPartFailed = new AtomicBoolean(DBG);
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
            trackers[i2] = getNewSubmitPduTracker(destAddr, scAddr, parts.get(i2), smsHeader, encoding, sentIntent, deliveryIntent, i2 == msgCount + (-1) ? true : DBG, unsentPartCount, anyPartFailed, messageUri, fullMessageText);
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
            MultipartSmsSender smsSender = new MultipartSmsSender(parts, trackers);
            smsSender.sendSmsByCarrierApp(carrierPackage, new MultipartSmsSenderCallback(smsSender));
            return;
        }
        Rlog.v(TAG, "No carrier package.");
        for (SmsTracker tracker : trackers) {
            if (tracker != null) {
                sendSubmitPdu(tracker);
            } else {
                Rlog.e(TAG, "Null tracker.");
            }
        }
    }

    protected SmsTracker getNewSubmitPduTracker(String destinationAddress, String scAddress, String message, SmsHeader smsHeader, int encoding, PendingIntent sentIntent, PendingIntent deliveryIntent, boolean lastPart, AtomicInteger unsentPartCount, AtomicBoolean anyPartFailed, Uri messageUri, String fullMessageText, int validityPeriod) {
        return null;
    }

    protected void sendRawPdu(SmsTracker tracker) {
        byte[] pdu = (byte[]) tracker.getData().get("pdu");
        if (this.mSmsSendDisabled) {
            Rlog.e(TAG, "Device does not support sending sms.");
            tracker.onFailed(this.mContext, 4, 0);
            return;
        }
        if (pdu == null) {
            Rlog.e(TAG, "Empty PDU");
            tracker.onFailed(this.mContext, 3, 0);
            return;
        }
        PackageManager pm = this.mContext.getPackageManager();
        String[] packageNames = pm.getPackagesForUid(Binder.getCallingUid());
        if (packageNames == null || packageNames.length == 0) {
            Rlog.e(TAG, "Can't get calling app package name: refusing to send SMS");
            tracker.onFailed(this.mContext, 1, 0);
            return;
        }
        String packageName = getPackageNameViaProcessId(packageNames);
        if (packageName != null) {
            packageNames[0] = packageName;
        }
        Rlog.d(TAG, "sendRawPdu and get the package name via process id: " + packageNames[0]);
        try {
            PackageInfo appInfo = pm.getPackageInfo(packageNames[0], 64);
            if (checkDestination(tracker)) {
                if (!this.mUsageMonitor.check(appInfo.packageName, 1)) {
                    sendMessage(obtainMessage(4, tracker));
                    return;
                }
                sendSms(tracker);
            }
            if (!PhoneNumberUtils.isLocalEmergencyNumber(this.mContext, tracker.mDestAddress)) {
                return;
            }
            new AsyncEmergencyContactNotifier(this.mContext).execute(new Void[0]);
        } catch (PackageManager.NameNotFoundException e) {
            Rlog.e(TAG, "Can't get calling app package info: refusing to send SMS");
            tracker.onFailed(this.mContext, 1, 0);
        }
    }

    boolean checkDestination(SmsTracker tracker) {
        int event;
        if (this.mContext.checkCallingOrSelfPermission("android.permission.SEND_SMS_NO_CONFIRMATION") == 0) {
            return true;
        }
        int rule = this.mPremiumSmsRule.get();
        int smsCategory = 0;
        if (rule == 1 || rule == 3) {
            String simCountryIso = this.mTelephonyManager.getSimCountryIso(getSubId());
            if (simCountryIso == null || simCountryIso.length() != 2) {
                Rlog.e(TAG, "Can't get SIM country Iso: trying network country Iso");
                simCountryIso = this.mTelephonyManager.getNetworkCountryIso(getSubId());
            }
            smsCategory = this.mUsageMonitor.checkDestination(tracker.mDestAddress, simCountryIso);
        }
        if (rule == 2 || rule == 3) {
            String networkCountryIso = this.mTelephonyManager.getNetworkCountryIso(getSubId());
            if (networkCountryIso == null || networkCountryIso.length() != 2) {
                Rlog.e(TAG, "Can't get Network country Iso: trying SIM country Iso");
                networkCountryIso = this.mTelephonyManager.getSimCountryIso(getSubId());
            }
            smsCategory = SmsUsageMonitor.mergeShortCodeCategories(smsCategory, this.mUsageMonitor.checkDestination(tracker.mDestAddress, networkCountryIso));
        }
        if (smsCategory == 0 || smsCategory == 1 || smsCategory == 2) {
            return true;
        }
        if (Settings.Global.getInt(this.mResolver, "device_provisioned", 0) == 0) {
            Rlog.e(TAG, "Can't send premium sms during Setup Wizard");
            return DBG;
        }
        int premiumSmsPermission = this.mUsageMonitor.getPremiumSmsPermission(tracker.mAppInfo.packageName);
        if (premiumSmsPermission == 0) {
            premiumSmsPermission = 1;
        }
        switch (premiumSmsPermission) {
            case 2:
                Rlog.w(TAG, "User denied this app from sending to premium SMS");
                sendMessage(obtainMessage(7, tracker));
                break;
            case 3:
                Rlog.d(TAG, "User approved this app to send to premium SMS");
                break;
            default:
                if (smsCategory == 3) {
                    event = 8;
                } else {
                    event = 9;
                }
                sendMessage(obtainMessage(event, tracker));
                break;
        }
        return true;
    }

    private boolean denyIfQueueLimitReached(SmsTracker tracker) {
        if (this.mPendingTrackerCount >= 5) {
            Rlog.e(TAG, "Denied because queue limit reached");
            tracker.onFailed(this.mContext, 5, 0);
            return true;
        }
        this.mPendingTrackerCount++;
        return DBG;
    }

    private CharSequence getAppLabel(String appPackage) {
        PackageManager pm = this.mContext.getPackageManager();
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(appPackage, 0);
            String label = appInfo.loadLabel(pm).toString();
            return convertSafeLabel(label, appPackage);
        } catch (PackageManager.NameNotFoundException e) {
            Rlog.e(TAG, "PackageManager Name Not Found for package " + appPackage);
            return appPackage;
        }
    }

    private CharSequence convertSafeLabel(String labelStr, String appPackage) {
        int labelLength = labelStr.length();
        int offset = 0;
        while (offset < labelLength) {
            int codePoint = labelStr.codePointAt(offset);
            int type = Character.getType(codePoint);
            if (type == 13 || type == 15 || type == 14) {
                labelStr = labelStr.substring(0, offset);
                break;
            }
            if (type == 12) {
                labelStr = labelStr.substring(0, offset) + " " + labelStr.substring(Character.charCount(codePoint) + offset);
            }
            offset += Character.charCount(codePoint);
        }
        String labelStr2 = labelStr.trim();
        if (labelStr2.isEmpty()) {
            return appPackage;
        }
        TextPaint paint = new TextPaint();
        paint.setTextSize(42.0f);
        return TextUtils.ellipsize(labelStr2, paint, MAX_LABEL_SIZE_PX, TextUtils.TruncateAt.END);
    }

    protected void handleReachSentLimit(SmsTracker tracker) {
        if (denyIfQueueLimitReached(tracker)) {
            return;
        }
        CharSequence appLabel = getAppLabel(tracker.mAppInfo.packageName);
        Resources r = Resources.getSystem();
        Spanned messageText = Html.fromHtml(r.getString(R.string.face_acquired_sensor_dirty, appLabel));
        ConfirmDialogListener listener = new ConfirmDialogListener(tracker, null);
        AlertDialog d = new AlertDialog.Builder(this.mContext).setTitle(R.string.face_acquired_roll_too_extreme).setIcon(R.drawable.stat_sys_warning).setMessage(messageText).setPositiveButton(r.getString(R.string.face_acquired_tilt_too_extreme), listener).setNegativeButton(r.getString(R.string.face_acquired_too_bright), listener).setOnCancelListener(listener).create();
        d.getWindow().setType(TelephonyEventLog.TAG_IMS_CALL_RECEIVE);
        d.show();
    }

    protected void handleConfirmShortCode(boolean isPremium, SmsTracker tracker) {
        int detailsId;
        if (denyIfQueueLimitReached(tracker)) {
            return;
        }
        if (isPremium) {
            detailsId = R.string.face_acquired_too_different;
        } else {
            detailsId = R.string.face_acquired_too_dark;
        }
        CharSequence appLabel = getAppLabel(tracker.mAppInfo.packageName);
        Resources r = Resources.getSystem();
        Spanned messageText = Html.fromHtml(r.getString(R.string.face_acquired_too_close, appLabel, tracker.mDestAddress));
        LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
        View layout = inflater.inflate(R.layout.notification_template_material_call, (ViewGroup) null);
        ConfirmDialogListener listener = new ConfirmDialogListener(tracker, (TextView) layout.findViewById(R.id.month_view));
        TextView messageView = (TextView) layout.findViewById(R.id.mode_normal);
        messageView.setText(messageText);
        ViewGroup detailsLayout = (ViewGroup) layout.findViewById(R.id.mode_out);
        TextView detailsView = (TextView) detailsLayout.findViewById(R.id.monospace);
        detailsView.setText(detailsId);
        CheckBox rememberChoice = (CheckBox) layout.findViewById(R.id.month);
        rememberChoice.setOnCheckedChangeListener(listener);
        AlertDialog d = new AlertDialog.Builder(this.mContext).setView(layout).setPositiveButton(r.getString(R.string.face_acquired_too_far), listener).setNegativeButton(r.getString(R.string.face_acquired_too_high), listener).setOnCancelListener(listener).create();
        d.getWindow().setType(TelephonyEventLog.TAG_IMS_CALL_RECEIVE);
        d.show();
        listener.setPositiveButton(d.getButton(-1));
        listener.setNegativeButton(d.getButton(-2));
    }

    public int getPremiumSmsPermission(String packageName) {
        return this.mUsageMonitor.getPremiumSmsPermission(packageName);
    }

    public void setPremiumSmsPermission(String packageName, int permission) {
        this.mUsageMonitor.setPremiumSmsPermission(packageName, permission);
    }

    protected static String getAppNameByIntent(PendingIntent intent) {
        Resources.getSystem();
        return intent != null ? intent.getTargetPackage() : "Resource unusable";
    }

    public void sendRetrySms(SmsTracker tracker) {
        if (this.mImsSMSDispatcher != null) {
            this.mImsSMSDispatcher.sendRetrySms(tracker);
        } else {
            Rlog.e(TAG, this.mImsSMSDispatcher + " is null. Retry failed");
        }
    }

    private void sendMultipartSms(SmsTracker tracker) {
        HashMap<String, Object> map = tracker.getData();
        String destinationAddress = (String) map.get("destination");
        String scAddress = (String) map.get("scaddress");
        ArrayList<String> parts = (ArrayList) map.get("parts");
        ArrayList<PendingIntent> sentIntents = (ArrayList) map.get("sentIntents");
        ArrayList<PendingIntent> deliveryIntents = (ArrayList) map.get("deliveryIntents");
        int ss = this.mPhone.getServiceState().getState();
        if (!isIms() && ss != 0 && !this.mTelephonyManager.isWifiCallingAvailable()) {
            int count = parts.size();
            for (int i = 0; i < count; i++) {
                PendingIntent sentIntent = null;
                if (sentIntents != null && sentIntents.size() > i) {
                    sentIntent = sentIntents.get(i);
                }
                handleNotInService(ss, sentIntent);
            }
            return;
        }
        sendMultipartText(destinationAddress, scAddress, parts, sentIntents, deliveryIntents, null, null, tracker.mPersistMessage);
    }

    public static class SmsTracker {
        private AtomicBoolean mAnyPartFailed;
        public final PackageInfo mAppInfo;
        private final HashMap<String, Object> mData;
        public final PendingIntent mDeliveryIntent;
        public final String mDestAddress;
        public boolean mExpectMore;
        String mFormat;
        private String mFullMessageText;
        public int mImsRetry;
        private boolean mIsText;
        public int mMessageRef;
        public Uri mMessageUri;
        public boolean mPersistMessage;
        private IPplSmsFilter mPplSmsFilter;
        public int mRetryCount;
        public final PendingIntent mSentIntent;
        public final SmsHeader mSmsHeader;
        private int mSubId;
        private long mTimestamp;
        private AtomicInteger mUnsentPartCount;

        SmsTracker(HashMap data, PendingIntent sentIntent, PendingIntent deliveryIntent, PackageInfo appInfo, String destAddr, String format, AtomicInteger unsentPartCount, AtomicBoolean anyPartFailed, Uri messageUri, SmsHeader smsHeader, boolean isExpectMore, String fullMessageText, int subId, boolean isText, boolean persistMessage, SmsTracker smsTracker) {
            this(data, sentIntent, deliveryIntent, appInfo, destAddr, format, unsentPartCount, anyPartFailed, messageUri, smsHeader, isExpectMore, fullMessageText, subId, isText, persistMessage);
        }

        private SmsTracker(HashMap<String, Object> data, PendingIntent sentIntent, PendingIntent deliveryIntent, PackageInfo appInfo, String destAddr, String format, AtomicInteger unsentPartCount, AtomicBoolean anyPartFailed, Uri messageUri, SmsHeader smsHeader, boolean isExpectMore, String fullMessageText, int subId, boolean isText, boolean persistMessage) {
            this.mTimestamp = System.currentTimeMillis();
            this.mPplSmsFilter = null;
            this.mData = data;
            this.mSentIntent = sentIntent;
            this.mDeliveryIntent = deliveryIntent;
            this.mRetryCount = 0;
            this.mAppInfo = appInfo;
            this.mDestAddress = destAddr;
            this.mFormat = format;
            this.mExpectMore = isExpectMore;
            this.mImsRetry = 0;
            this.mMessageRef = 0;
            this.mUnsentPartCount = unsentPartCount;
            this.mAnyPartFailed = anyPartFailed;
            this.mMessageUri = messageUri;
            this.mSmsHeader = smsHeader;
            this.mFullMessageText = fullMessageText;
            this.mSubId = subId;
            this.mIsText = isText;
            this.mPersistMessage = persistMessage;
        }

        boolean isMultipart() {
            return this.mData.containsKey("parts");
        }

        public HashMap<String, Object> getData() {
            return this.mData;
        }

        public void updateSentMessageStatus(Context context, int status) {
            if (this.mMessageUri == null) {
                return;
            }
            ContentValues values = new ContentValues(1);
            values.put("status", Integer.valueOf(status));
            SqliteWrapper.update(context, context.getContentResolver(), this.mMessageUri, values, (String) null, (String[]) null);
        }

        private void updateMessageState(Context context, int messageType, int errorCode) {
            if (this.mMessageUri == null) {
                return;
            }
            ContentValues values = new ContentValues(2);
            values.put("type", Integer.valueOf(messageType));
            values.put(Telephony.TextBasedSmsColumns.ERROR_CODE, Integer.valueOf(errorCode));
            long identity = Binder.clearCallingIdentity();
            try {
                if (SqliteWrapper.update(context, context.getContentResolver(), this.mMessageUri, values, (String) null, (String[]) null) != 1) {
                    Rlog.e(SMSDispatcher.TAG, "Failed to move message to " + messageType);
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        private Uri persistSentMessageIfRequired(Context context, int messageType, int errorCode) {
            if (!this.mIsText || !this.mPersistMessage || !SmsApplication.shouldWriteMessageForPackage(this.mAppInfo.packageName, context) || isFilterOutByPpl(context, this.mDestAddress, this.mFullMessageText)) {
                return null;
            }
            Rlog.d(SMSDispatcher.TAG, "Persist SMS into " + (messageType == 5 ? "FAILED" : "SENT"));
            ContentValues values = new ContentValues();
            values.put("sub_id", Integer.valueOf(this.mSubId));
            values.put("address", this.mDestAddress);
            values.put("body", this.mFullMessageText);
            values.put("date", Long.valueOf(System.currentTimeMillis()));
            values.put("seen", (Integer) 1);
            values.put("read", (Integer) 1);
            String creator = this.mAppInfo != null ? this.mAppInfo.packageName : null;
            if (!TextUtils.isEmpty(creator)) {
                values.put("creator", creator);
            }
            if (this.mDeliveryIntent != null) {
                values.put("status", (Integer) 32);
            }
            if (errorCode != 0) {
                values.put(Telephony.TextBasedSmsColumns.ERROR_CODE, Integer.valueOf(errorCode));
            }
            long identity = Binder.clearCallingIdentity();
            ContentResolver resolver = context.getContentResolver();
            try {
                Uri uri = resolver.insert(Telephony.Sms.Sent.CONTENT_URI, values);
                if (uri != null && messageType == 5) {
                    ContentValues updateValues = new ContentValues(1);
                    updateValues.put("type", (Integer) 5);
                    resolver.update(uri, updateValues, null, null);
                }
                return uri;
            } catch (Exception e) {
                Rlog.e(SMSDispatcher.TAG, "writeOutboxMessage: Failed to persist outbox message", e);
                return null;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        private void persistOrUpdateMessage(Context context, int messageType, int errorCode) {
            if (this.mMessageUri != null) {
                updateMessageState(context, messageType, errorCode);
            } else {
                this.mMessageUri = persistSentMessageIfRequired(context, messageType, errorCode);
            }
        }

        public void onFailed(Context context, int error, int errorCode) {
            if (this.mAnyPartFailed != null) {
                this.mAnyPartFailed.set(true);
            }
            boolean isSinglePartOrLastPart = true;
            if (this.mUnsentPartCount != null) {
                isSinglePartOrLastPart = this.mUnsentPartCount.decrementAndGet() == 0 ? true : SMSDispatcher.DBG;
            }
            if (isSinglePartOrLastPart) {
                persistOrUpdateMessage(context, 5, errorCode);
            }
            if (this.mSentIntent == null) {
                return;
            }
            try {
                Intent fillIn = new Intent();
                if (this.mMessageUri != null) {
                    fillIn.putExtra("uri", this.mMessageUri.toString());
                }
                if (errorCode != 0) {
                    fillIn.putExtra(TelephonyEventLog.DATA_KEY_SMS_ERROR_CODE, errorCode);
                }
                if (this.mUnsentPartCount != null && isSinglePartOrLastPart) {
                    fillIn.putExtra(SMSDispatcher.SEND_NEXT_MSG_EXTRA, true);
                }
                int szPdu = 0;
                int smscLength = 0;
                int pduLength = 0;
                if (this.mData != null) {
                    if (this.mData.get("smsc") != null) {
                        smscLength = ((byte[]) this.mData.get("smsc")).length;
                    }
                    if (this.mData.get("pdu") != null) {
                        pduLength = ((byte[]) this.mData.get("pdu")).length;
                    }
                    szPdu = smscLength + pduLength;
                }
                fillIn.putExtra(SMSDispatcher.PDU_SIZE, szPdu);
                this.mSentIntent.send(context, error, fillIn);
            } catch (PendingIntent.CanceledException e) {
                Rlog.e(SMSDispatcher.TAG, "Failed to send result");
            }
        }

        public void onSent(Context context) {
            boolean isSinglePartOrLastPart = true;
            if (this.mUnsentPartCount != null) {
                isSinglePartOrLastPart = this.mUnsentPartCount.decrementAndGet() == 0 ? true : SMSDispatcher.DBG;
            }
            if (isSinglePartOrLastPart) {
                int messageType = 2;
                if (this.mAnyPartFailed != null && this.mAnyPartFailed.get()) {
                    messageType = 5;
                }
                persistOrUpdateMessage(context, messageType, 0);
            }
            if (this.mSentIntent == null) {
                return;
            }
            try {
                Intent fillIn = new Intent();
                if (this.mMessageUri != null) {
                    fillIn.putExtra("uri", this.mMessageUri.toString());
                }
                if (this.mUnsentPartCount != null && isSinglePartOrLastPart) {
                    fillIn.putExtra(SMSDispatcher.SEND_NEXT_MSG_EXTRA, true);
                }
                int szPdu = 0;
                int smscLength = 0;
                int pduLength = 0;
                if (this.mData != null) {
                    if (this.mData.get("smsc") != null) {
                        smscLength = ((byte[]) this.mData.get("smsc")).length;
                    }
                    if (this.mData.get("pdu") != null) {
                        pduLength = ((byte[]) this.mData.get("pdu")).length;
                    }
                    szPdu = smscLength + pduLength;
                }
                fillIn.putExtra(SMSDispatcher.PDU_SIZE, szPdu);
                fillIn.putExtra(SMSDispatcher.MSG_REF_NUM, this.mMessageRef);
                Rlog.d(SMSDispatcher.TAG, "message reference number : " + this.mMessageRef);
                this.mSentIntent.send(context, -1, fillIn);
            } catch (PendingIntent.CanceledException e) {
                Rlog.e(SMSDispatcher.TAG, "Failed to send result");
            }
        }

        protected boolean isFilterOutByPpl(Context context, String destAddr, String text) {
            if (this.mPplSmsFilter == null) {
                this.mPplSmsFilter = new PplSmsFilterExtension(context);
            }
            if (!SmsConstants.isPrivacyLockSupport()) {
                return SMSDispatcher.DBG;
            }
            if (SMSDispatcher.ENG) {
                Rlog.d(SMSDispatcher.TAG, "[Moms] Phone privacy check start");
            }
            Bundle pplData = new Bundle();
            pplData.putString(IPplSmsFilter.KEY_MSG_CONTENT, text);
            pplData.putString(IPplSmsFilter.KEY_DST_ADDR, destAddr);
            pplData.putString("format", this.mFormat);
            pplData.putInt(IPplSmsFilter.KEY_SUB_ID, this.mSubId);
            pplData.putInt(IPplSmsFilter.KEY_SMS_TYPE, 1);
            boolean pplResult = this.mPplSmsFilter.pplFilter(pplData);
            if (SMSDispatcher.ENG) {
                Rlog.d(SMSDispatcher.TAG, "[Moms] Phone privacy check end, Need to filter(result) = " + pplResult);
            }
            return pplResult;
        }
    }

    protected SmsTracker getSmsTracker(HashMap<String, Object> data, PendingIntent sentIntent, PendingIntent deliveryIntent, String format, AtomicInteger unsentPartCount, AtomicBoolean anyPartFailed, Uri messageUri, SmsHeader smsHeader, boolean isExpectMore, String fullMessageText, boolean isText, boolean persistMessage) {
        PackageManager pm = this.mContext.getPackageManager();
        String[] packageNames = pm.getPackagesForUid(Binder.getCallingUid());
        PackageInfo appInfo = null;
        if (packageNames != null && packageNames.length > 0) {
            try {
                String packageName = getPackageNameViaProcessId(packageNames);
                if (packageName != null) {
                    packageNames[0] = packageName;
                }
                Rlog.d(TAG, "SmsTrackerFactory and get the package name via process id: " + packageNames[0]);
                appInfo = pm.getPackageInfo(packageNames[0], 64);
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        String destAddr = PhoneNumberUtils.extractNetworkPortion((String) data.get("destAddr"));
        return new SmsTracker(data, sentIntent, deliveryIntent, appInfo, destAddr, format, unsentPartCount, anyPartFailed, messageUri, smsHeader, isExpectMore, fullMessageText, getSubId(), isText, persistMessage, null);
    }

    protected SmsTracker getSmsTracker(HashMap<String, Object> data, PendingIntent sentIntent, PendingIntent deliveryIntent, String format, Uri messageUri, boolean isExpectMore, String fullMessageText, boolean isText, boolean persistMessage) {
        return getSmsTracker(data, sentIntent, deliveryIntent, format, null, null, messageUri, null, isExpectMore, fullMessageText, isText, persistMessage);
    }

    protected HashMap<String, Object> getSmsTrackerMap(String destAddr, String scAddr, String text, SmsMessageBase.SubmitPduBase pdu) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("destAddr", destAddr);
        map.put("scAddr", scAddr);
        map.put("text", text);
        map.put("smsc", pdu.encodedScAddress);
        map.put("pdu", pdu.encodedMessage);
        return map;
    }

    protected HashMap<String, Object> getSmsTrackerMap(String destAddr, String scAddr, int destPort, byte[] data, SmsMessageBase.SubmitPduBase pdu) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("destAddr", destAddr);
        map.put("scAddr", scAddr);
        map.put("destPort", Integer.valueOf(destPort));
        map.put("data", data);
        map.put("smsc", pdu.encodedScAddress);
        map.put("pdu", pdu.encodedMessage);
        return map;
    }

    private final class ConfirmDialogListener implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener, CompoundButton.OnCheckedChangeListener {
        private Button mNegativeButton;
        private Button mPositiveButton;
        private boolean mRememberChoice;
        private final TextView mRememberUndoInstruction;
        private final SmsTracker mTracker;

        ConfirmDialogListener(SmsTracker tracker, TextView textView) {
            this.mTracker = tracker;
            this.mRememberUndoInstruction = textView;
        }

        void setPositiveButton(Button button) {
            this.mPositiveButton = button;
        }

        void setNegativeButton(Button button) {
            this.mNegativeButton = button;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            int newSmsPermission = 1;
            if (which == -1) {
                Rlog.d(SMSDispatcher.TAG, "CONFIRM sending SMS");
                EventLog.writeEvent(EventLogTags.EXP_DET_SMS_SENT_BY_USER, this.mTracker.mAppInfo.applicationInfo != null ? this.mTracker.mAppInfo.applicationInfo.uid : -1);
                SMSDispatcher.this.sendMessage(SMSDispatcher.this.obtainMessage(5, this.mTracker));
                if (this.mRememberChoice) {
                    newSmsPermission = 3;
                }
            } else if (which == -2) {
                Rlog.d(SMSDispatcher.TAG, "DENY sending SMS");
                EventLog.writeEvent(EventLogTags.EXP_DET_SMS_DENIED_BY_USER, this.mTracker.mAppInfo.applicationInfo != null ? this.mTracker.mAppInfo.applicationInfo.uid : -1);
                SMSDispatcher.this.sendMessage(SMSDispatcher.this.obtainMessage(7, this.mTracker));
                if (this.mRememberChoice) {
                    newSmsPermission = 2;
                }
            }
            SMSDispatcher.this.setPremiumSmsPermission(this.mTracker.mAppInfo.packageName, newSmsPermission);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            Rlog.d(SMSDispatcher.TAG, "dialog dismissed: don't send SMS");
            SMSDispatcher.this.sendMessage(SMSDispatcher.this.obtainMessage(7, this.mTracker));
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Rlog.d(SMSDispatcher.TAG, "remember this choice: " + isChecked);
            this.mRememberChoice = isChecked;
            if (isChecked) {
                this.mPositiveButton.setText(R.string.face_acquired_too_much_motion);
                this.mNegativeButton.setText(R.string.face_acquired_too_right);
                if (this.mRememberUndoInstruction == null) {
                    return;
                }
                this.mRememberUndoInstruction.setText(R.string.face_acquired_too_low);
                this.mRememberUndoInstruction.setPadding(0, 0, 0, 32);
                return;
            }
            this.mPositiveButton.setText(R.string.face_acquired_too_far);
            this.mNegativeButton.setText(R.string.face_acquired_too_high);
            if (this.mRememberUndoInstruction == null) {
                return;
            }
            this.mRememberUndoInstruction.setText(UsimPBMemInfo.STRING_NOT_SET);
            this.mRememberUndoInstruction.setPadding(0, 0, 0, 0);
        }
    }

    public boolean isIms() {
        if (this.mImsSMSDispatcher != null) {
            return this.mImsSMSDispatcher.isIms();
        }
        Rlog.e(TAG, this.mImsSMSDispatcher + " is null");
        return DBG;
    }

    public String getImsSmsFormat() {
        if (this.mImsSMSDispatcher != null) {
            return this.mImsSMSDispatcher.getImsSmsFormat();
        }
        Rlog.e(TAG, this.mImsSMSDispatcher + " is null");
        return null;
    }

    protected String getMultipartMessageText(ArrayList<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part != null) {
                sb.append(part);
            }
        }
        return sb.toString();
    }

    protected String getCarrierAppPackageName() {
        List<String> carrierPackages;
        UiccCard card = UiccController.getInstance().getUiccCard(this.mPhone.getPhoneId());
        if (card == null || (carrierPackages = card.getCarrierPackageNamesForIntent(this.mContext.getPackageManager(), new Intent("android.service.carrier.CarrierMessagingService"))) == null || carrierPackages.size() != 1) {
            return null;
        }
        return carrierPackages.get(0);
    }

    protected int getSubId() {
        return SubscriptionController.getInstance().getSubIdUsingPhoneId(this.mPhone.getPhoneId());
    }

    private void checkCallerIsPhoneOrCarrierApp() {
        int uid = Binder.getCallingUid();
        int appId = UserHandle.getAppId(uid);
        if (appId == 1001 || uid == 0) {
            return;
        }
        try {
            PackageManager pm = this.mContext.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(getCarrierAppPackageName(), 0);
            if (UserHandle.isSameApp(ai.uid, Binder.getCallingUid())) {
            } else {
                throw new SecurityException("Caller is not phone or carrier app!");
            }
        } catch (PackageManager.NameNotFoundException e) {
            throw new SecurityException("Caller is not phone or carrier app!");
        }
    }

    private void createWakelock() {
        PowerManager pm = (PowerManager) this.mContext.getSystemService("power");
        this.mWakeLock = pm.newWakeLock(1, TAG);
        this.mWakeLock.setReferenceCounted(true);
    }

    protected void handleIccFull() {
    }

    protected void handleQueryCbActivation(AsyncResult ar) {
        Rlog.e(TAG, "didn't support cellBoradcast in the CDMA phone");
    }

    protected void sendData(String destAddr, String scAddr, int destPort, int originalPort, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) {
    }

    protected void sendMultipartData(String destAddr, String scAddr, int destPort, ArrayList<SmsRawData> data, ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents) {
    }

    public int copyTextMessageToIccCard(String scAddress, String address, List<String> text, int status, long timestamp) {
        return 0;
    }

    private void notifySmsReady(boolean isReady) {
        Intent intent = new Intent(Telephony.Sms.Intents.SMS_STATE_CHANGED_ACTION);
        intent.putExtra("ready", isReady);
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhone.getPhoneId());
        this.mWakeLock.acquire(500L);
        this.mContext.sendBroadcast(intent);
    }

    protected void setSmsMemoryStatus(boolean status) {
        if (status == this.mStorageAvailable) {
            return;
        }
        this.mStorageAvailable = status;
        this.mCi.reportSmsMemoryStatus(status, null);
    }

    protected boolean isSmsReady() {
        return this.mSmsReady;
    }

    protected void sendTextWithEncodingType(String destAddr, String scAddr, String text, int encodingType, PendingIntent sentIntent, PendingIntent deliveryIntent, Uri messageUri, String callingPkg, boolean persistMessage) {
    }

    protected void sendMultipartTextWithEncodingType(String destAddr, String scAddr, ArrayList<String> parts, int encodingType, ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents, Uri messageUri, String callingPkg, boolean persistMessage) {
    }

    public void sendTextWithExtraParams(String destAddr, String scAddr, String text, Bundle extraParams, PendingIntent sentIntent, PendingIntent deliveryIntent, Uri messageUri, String callingPkg, boolean persistMessage) {
    }

    public void sendMultipartTextWithExtraParams(String destAddr, String scAddr, ArrayList<String> parts, Bundle extraParams, ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents, Uri messageUri, String callingPkg, boolean persistMessage) {
    }

    private String getPackageNameViaProcessId(String[] packageNames) {
        String packageName = null;
        if (packageNames.length == 1) {
            String packageName2 = packageNames[0];
            return packageName2;
        }
        if (packageNames.length <= 1) {
            return null;
        }
        int callingPid = Binder.getCallingPid();
        ActivityManager am = (ActivityManager) this.mContext.getSystemService("activity");
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();
        if (runningAppProcesses == null) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo processInfo : runningAppProcesses) {
            if (callingPid == processInfo.pid) {
                for (String pkgInProcess : processInfo.pkgList) {
                    int i = 0;
                    int length = packageNames.length;
                    while (true) {
                        if (i >= length) {
                            break;
                        }
                        String pkg = packageNames[i];
                        if (!pkg.equals(pkgInProcess)) {
                            i++;
                        } else {
                            packageName = pkg;
                            break;
                        }
                    }
                    if (packageName != null) {
                        return packageName;
                    }
                }
                return packageName;
            }
        }
        return null;
    }

    protected void handleSendNextTracker(SmsTracker currentTracker) {
        int szPdu = 0;
        if (currentTracker != null) {
            HashMap map = currentTracker.mData;
            int smscLength = 0;
            int pduLength = 0;
            if (map != null) {
                if (map.get("smsc") != null) {
                    smscLength = ((byte[]) map.get("smsc")).length;
                }
                if (map.get("pdu") != null) {
                    pduLength = ((byte[]) map.get("pdu")).length;
                }
                szPdu = smscLength + pduLength;
            }
        } else {
            Rlog.d(TAG, "Current tracker is null");
        }
        SmsTracker nextTracker = null;
        synchronized (this.mSTrackersQueue) {
            Rlog.d(TAG, "Remove Tracker");
            SmsTracker smsTrackerRemove = !this.mSTrackersQueue.isEmpty() ? this.mSTrackersQueue.remove(0) : null;
            if (smsTrackerRemove != null && smsTrackerRemove.equals(currentTracker) && ENG) {
                Rlog.d(TAG, "[pdu size: " + szPdu);
            }
            if (!this.mSTrackersQueue.isEmpty()) {
                SmsTracker nextTracker2 = this.mSTrackersQueue.get(0);
                nextTracker = nextTracker2;
            }
        }
        if (nextTracker != null) {
            if (isFormatMatch(nextTracker, this.mPhone)) {
                sendSms(nextTracker);
                return;
            } else {
                nextTracker.onFailed(this.mContext, 1, 0);
                return;
            }
        }
        Rlog.d(TAG, "mSTrackersQueue is empty");
    }

    boolean isFormatMatch(SmsTracker tracker, Phone phone) {
        if (tracker.mFormat.equals(SmsMessage.FORMAT_3GPP2) && phone.getPhoneType() == 2) {
            return true;
        }
        if (tracker.mFormat.equals(SmsMessage.FORMAT_3GPP) && phone.getPhoneType() == 1) {
            return true;
        }
        return DBG;
    }
}
