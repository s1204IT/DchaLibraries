package com.android.internal.telephony;

import android.R;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.Telephony;
import android.service.carrier.ICarrierMessagingCallback;
import android.service.carrier.ICarrierMessagingService;
import android.telephony.CarrierMessagingServiceManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.EventLog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.cdma.sms.UserData;
import com.android.internal.telephony.gsm.SmsMessage;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class SMSDispatcher extends Handler {
    static final boolean DBG = false;
    protected static final int EVENT_CONFIRM_SEND_TO_POSSIBLE_PREMIUM_SHORT_CODE = 8;
    protected static final int EVENT_CONFIRM_SEND_TO_PREMIUM_SHORT_CODE = 9;
    protected static final int EVENT_GET_IMS_SERVICE = 16;
    protected static final int EVENT_HANDLE_STATUS_REPORT = 10;
    protected static final int EVENT_ICC_CHANGED = 15;
    protected static final int EVENT_NEW_ICC_SMS = 14;
    static final int EVENT_SEND_CONFIRMED_SMS = 5;
    protected static final int EVENT_SEND_LIMIT_REACHED_CONFIRMATION = 4;
    private static final int EVENT_SEND_RETRY = 3;
    protected static final int EVENT_SEND_SMS_COMPLETE = 2;
    protected static final int EVENT_STOP_SENDING = 7;
    protected static final String MAP_KEY_DATA = "data";
    protected static final String MAP_KEY_DEST_ADDR = "destAddr";
    protected static final String MAP_KEY_DEST_PORT = "destPort";
    protected static final String MAP_KEY_PDU = "pdu";
    protected static final String MAP_KEY_SC_ADDR = "scAddr";
    protected static final String MAP_KEY_SMSC = "smsc";
    protected static final String MAP_KEY_TEXT = "text";
    protected static final int MAX_SEND_RETRIES = 3;
    public static final int MO_MSG_QUEUE_LIMIT = 5;
    protected static final int PREMIUM_RULE_USE_BOTH = 3;
    protected static final int PREMIUM_RULE_USE_NETWORK = 2;
    protected static final int PREMIUM_RULE_USE_SIM = 1;
    private static final String SEND_NEXT_MSG_EXTRA = "SendNextMsg";
    private static final int SEND_RETRY_DELAY = 2000;
    protected static final int SINGLE_PART_SMS = 1;
    static final String TAG = "SMSDispatcher";
    private static int sConcatenatedRef = new Random().nextInt(256);
    protected final CommandsInterface mCi;
    protected final Context mContext;
    private int mPendingTrackerCount;
    protected Phone mPhone;
    protected final ContentResolver mResolver;
    private final SettingsObserver mSettingsObserver;
    protected boolean mSmsCapable;
    protected SmsDispatchersController mSmsDispatchersController;
    protected boolean mSmsSendDisabled;
    protected final TelephonyManager mTelephonyManager;
    protected final AtomicInteger mPremiumSmsRule = new AtomicInteger(1);
    protected final ArrayList<SmsTracker> deliveryPendingList = new ArrayList<>();

    protected abstract GsmAlphabet.TextEncodingDetails calculateLength(CharSequence charSequence, boolean z);

    protected abstract String getFormat();

    protected abstract SmsMessageBase.SubmitPduBase getSubmitPdu(String str, String str2, int i, byte[] bArr, boolean z);

    protected abstract SmsMessageBase.SubmitPduBase getSubmitPdu(String str, String str2, String str3, boolean z, SmsHeader smsHeader, int i, int i2);

    protected abstract void sendSms(SmsTracker smsTracker);

    protected abstract boolean shouldBlockSmsForEcbm();

    protected static int getNextConcatenatedRef() {
        sConcatenatedRef++;
        return sConcatenatedRef;
    }

    protected SMSDispatcher(Phone phone, SmsDispatchersController smsDispatchersController) {
        this.mSmsCapable = true;
        this.mPhone = phone;
        this.mSmsDispatchersController = smsDispatchersController;
        this.mContext = phone.getContext();
        this.mResolver = this.mContext.getContentResolver();
        this.mCi = phone.mCi;
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        this.mSettingsObserver = new SettingsObserver(this, this.mPremiumSmsRule, this.mContext);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("sms_short_code_rule"), false, this.mSettingsObserver);
        this.mSmsCapable = this.mContext.getResources().getBoolean(R.^attr-private.notificationHeaderIconSize);
        this.mSmsSendDisabled = !this.mTelephonyManager.getSmsSendCapableForPhone(this.mPhone.getPhoneId(), this.mSmsCapable);
        Rlog.d(TAG, "SMSDispatcher: ctor mSmsCapable=" + this.mSmsCapable + " format=" + getFormat() + " mSmsSendDisabled=" + this.mSmsSendDisabled);
    }

    private static class SettingsObserver extends ContentObserver {
        private final Context mContext;
        private final AtomicInteger mPremiumSmsRule;

        SettingsObserver(Handler handler, AtomicInteger atomicInteger, Context context) {
            super(handler);
            this.mPremiumSmsRule = atomicInteger;
            this.mContext = context;
            onChange(false);
        }

        @Override
        public void onChange(boolean z) {
            this.mPremiumSmsRule.set(Settings.Global.getInt(this.mContext.getContentResolver(), "sms_short_code_rule", 1));
        }
    }

    protected void updatePhoneObject(Phone phone) {
        this.mPhone = phone;
        Rlog.d(TAG, "Active phone changed to " + this.mPhone.getPhoneName());
    }

    public void dispose() {
        this.mContext.getContentResolver().unregisterContentObserver(this.mSettingsObserver);
    }

    protected void handleStatusReport(Object obj) {
        Rlog.d(TAG, "handleStatusReport() called with no subclass.");
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case 2:
                handleSendComplete((AsyncResult) message.obj);
                break;
            case 3:
                Rlog.d(TAG, "SMS retry..");
                sendRetrySms((SmsTracker) message.obj);
                break;
            case 4:
                handleReachSentLimit((SmsTracker) message.obj);
                break;
            case 5:
                SmsTracker smsTracker = (SmsTracker) message.obj;
                if (smsTracker.isMultipart()) {
                    sendMultipartSms(smsTracker);
                } else {
                    if (this.mPendingTrackerCount > 1) {
                        smsTracker.mExpectMore = true;
                    } else {
                        smsTracker.mExpectMore = false;
                    }
                    sendSms(smsTracker);
                }
                this.mPendingTrackerCount--;
                break;
            case 6:
            default:
                Rlog.e(TAG, "handleMessage() ignoring message of unexpected type " + message.what);
                break;
            case 7:
                SmsTracker smsTracker2 = (SmsTracker) message.obj;
                if (message.arg1 == 0) {
                    if (message.arg2 == 1) {
                        smsTracker2.onFailed(this.mContext, 8, 0);
                        Rlog.d(TAG, "SMSDispatcher: EVENT_STOP_SENDING - sending SHORT_CODE_NEVER_ALLOWED error code.");
                    } else {
                        smsTracker2.onFailed(this.mContext, 7, 0);
                        Rlog.d(TAG, "SMSDispatcher: EVENT_STOP_SENDING - sending SHORT_CODE_NOT_ALLOWED error code.");
                    }
                } else if (message.arg1 == 1) {
                    smsTracker2.onFailed(this.mContext, 5, 0);
                    Rlog.d(TAG, "SMSDispatcher: EVENT_STOP_SENDING - sending LIMIT_EXCEEDED error code.");
                } else {
                    Rlog.e(TAG, "SMSDispatcher: EVENT_STOP_SENDING - unexpected cases.");
                }
                this.mPendingTrackerCount--;
                break;
            case 8:
                handleConfirmShortCode(false, (SmsTracker) message.obj);
                break;
            case 9:
                handleConfirmShortCode(true, (SmsTracker) message.obj);
                break;
            case 10:
                handleStatusReport(message.obj);
                break;
        }
    }

    protected abstract class SmsSender extends CarrierMessagingServiceManager {
        protected volatile SmsSenderCallback mSenderCallback;
        protected final SmsTracker mTracker;

        protected SmsSender(SmsTracker smsTracker) {
            this.mTracker = smsTracker;
        }

        public void sendSmsByCarrierApp(String str, SmsSenderCallback smsSenderCallback) {
            this.mSenderCallback = smsSenderCallback;
            if (!bindToCarrierMessagingService(SMSDispatcher.this.mContext, str)) {
                Rlog.e(SMSDispatcher.TAG, "bindService() for carrier messaging service failed");
                this.mSenderCallback.onSendSmsComplete(1, 0);
            } else {
                Rlog.d(SMSDispatcher.TAG, "bindService() for carrier messaging service succeeded");
            }
        }
    }

    private static int getSendSmsFlag(PendingIntent pendingIntent) {
        if (pendingIntent == null) {
            return 0;
        }
        return 1;
    }

    protected final class TextSmsSender extends SmsSender {
        public TextSmsSender(SmsTracker smsTracker) {
            super(smsTracker);
        }

        @Override
        protected void onServiceReady(ICarrierMessagingService iCarrierMessagingService) {
            String str = (String) this.mTracker.getData().get(SMSDispatcher.MAP_KEY_TEXT);
            if (str == null) {
                this.mSenderCallback.onSendSmsComplete(1, 0);
                return;
            }
            try {
                iCarrierMessagingService.sendTextSms(str, SMSDispatcher.this.getSubId(), this.mTracker.mDestAddress, SMSDispatcher.getSendSmsFlag(this.mTracker.mDeliveryIntent), this.mSenderCallback);
            } catch (RemoteException e) {
                Rlog.e(SMSDispatcher.TAG, "Exception sending the SMS: " + e);
                this.mSenderCallback.onSendSmsComplete(1, 0);
            }
        }
    }

    protected final class DataSmsSender extends SmsSender {
        public DataSmsSender(SmsTracker smsTracker) {
            super(smsTracker);
        }

        @Override
        protected void onServiceReady(ICarrierMessagingService iCarrierMessagingService) {
            HashMap<String, Object> data = this.mTracker.getData();
            byte[] bArr = (byte[]) data.get(SMSDispatcher.MAP_KEY_DATA);
            int iIntValue = ((Integer) data.get(SMSDispatcher.MAP_KEY_DEST_PORT)).intValue();
            if (bArr == null) {
                this.mSenderCallback.onSendSmsComplete(1, 0);
                return;
            }
            try {
                iCarrierMessagingService.sendDataSms(bArr, SMSDispatcher.this.getSubId(), this.mTracker.mDestAddress, iIntValue, SMSDispatcher.getSendSmsFlag(this.mTracker.mDeliveryIntent), this.mSenderCallback);
            } catch (RemoteException e) {
                Rlog.e(SMSDispatcher.TAG, "Exception sending the SMS: " + e);
                this.mSenderCallback.onSendSmsComplete(1, 0);
            }
        }
    }

    protected final class SmsSenderCallback extends ICarrierMessagingCallback.Stub {
        private final SmsSender mSmsSender;

        public SmsSenderCallback(SmsSender smsSender) {
            this.mSmsSender = smsSender;
        }

        public void onSendSmsComplete(int i, int i2) {
            SMSDispatcher.this.checkCallerIsPhoneOrCarrierApp();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                this.mSmsSender.disposeConnection(SMSDispatcher.this.mContext);
                SMSDispatcher.this.processSendSmsResponse(this.mSmsSender.mTracker, i, i2);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void onSendMultipartSmsComplete(int i, int[] iArr) {
            Rlog.e(SMSDispatcher.TAG, "Unexpected onSendMultipartSmsComplete call with result: " + i);
        }

        public void onFilterComplete(int i) {
            Rlog.e(SMSDispatcher.TAG, "Unexpected onFilterComplete call with result: " + i);
        }

        public void onSendMmsComplete(int i, byte[] bArr) {
            Rlog.e(SMSDispatcher.TAG, "Unexpected onSendMmsComplete call with result: " + i);
        }

        public void onDownloadMmsComplete(int i) {
            Rlog.e(SMSDispatcher.TAG, "Unexpected onDownloadMmsComplete call with result: " + i);
        }
    }

    private void processSendSmsResponse(SmsTracker smsTracker, int i, int i2) {
        if (smsTracker == null) {
            Rlog.e(TAG, "processSendSmsResponse: null tracker");
        }
        SmsResponse smsResponse = new SmsResponse(i2, null, -1);
        switch (i) {
            case 0:
                Rlog.d(TAG, "Sending SMS by IP succeeded.");
                sendMessage(obtainMessage(2, new AsyncResult(smsTracker, smsResponse, (Throwable) null)));
                break;
            case 1:
                Rlog.d(TAG, "Sending SMS by IP failed. Retry on carrier network.");
                sendSubmitPdu(smsTracker);
                break;
            case 2:
                Rlog.d(TAG, "Sending SMS by IP failed.");
                sendMessage(obtainMessage(2, new AsyncResult(smsTracker, smsResponse, new CommandException(CommandException.Error.GENERIC_FAILURE))));
                break;
            default:
                Rlog.d(TAG, "Unknown result " + i + " Retry on carrier network.");
                sendSubmitPdu(smsTracker);
                break;
        }
    }

    protected final class MultipartSmsSender extends CarrierMessagingServiceManager {
        private final List<String> mParts;
        private volatile MultipartSmsSenderCallback mSenderCallback;
        public final SmsTracker[] mTrackers;

        public MultipartSmsSender(ArrayList<String> arrayList, SmsTracker[] smsTrackerArr) {
            this.mParts = arrayList;
            this.mTrackers = smsTrackerArr;
        }

        public void sendSmsByCarrierApp(String str, MultipartSmsSenderCallback multipartSmsSenderCallback) {
            this.mSenderCallback = multipartSmsSenderCallback;
            if (!bindToCarrierMessagingService(SMSDispatcher.this.mContext, str)) {
                Rlog.e(SMSDispatcher.TAG, "bindService() for carrier messaging service failed");
                this.mSenderCallback.onSendMultipartSmsComplete(1, null);
            } else {
                Rlog.d(SMSDispatcher.TAG, "bindService() for carrier messaging service succeeded");
            }
        }

        @Override
        protected void onServiceReady(ICarrierMessagingService iCarrierMessagingService) {
            try {
                iCarrierMessagingService.sendMultipartTextSms(this.mParts, SMSDispatcher.this.getSubId(), this.mTrackers[0].mDestAddress, SMSDispatcher.getSendSmsFlag(this.mTrackers[0].mDeliveryIntent), this.mSenderCallback);
            } catch (RemoteException e) {
                Rlog.e(SMSDispatcher.TAG, "Exception sending the SMS: " + e);
                this.mSenderCallback.onSendMultipartSmsComplete(1, null);
            }
        }
    }

    protected final class MultipartSmsSenderCallback extends ICarrierMessagingCallback.Stub {
        private final MultipartSmsSender mSmsSender;

        public MultipartSmsSenderCallback(MultipartSmsSender multipartSmsSender) {
            this.mSmsSender = multipartSmsSender;
        }

        public void onSendSmsComplete(int i, int i2) {
            Rlog.e(SMSDispatcher.TAG, "Unexpected onSendSmsComplete call with result: " + i);
        }

        public void onSendMultipartSmsComplete(int i, int[] iArr) {
            int i2;
            this.mSmsSender.disposeConnection(SMSDispatcher.this.mContext);
            if (this.mSmsSender.mTrackers != null) {
                SMSDispatcher.this.checkCallerIsPhoneOrCarrierApp();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                for (int i3 = 0; i3 < this.mSmsSender.mTrackers.length; i3++) {
                    try {
                        if (iArr != null && iArr.length > i3) {
                            i2 = iArr[i3];
                        } else {
                            i2 = 0;
                        }
                        SMSDispatcher.this.processSendSmsResponse(this.mSmsSender.mTrackers[i3], i, i2);
                    } finally {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                }
                return;
            }
            Rlog.e(SMSDispatcher.TAG, "Unexpected onSendMultipartSmsComplete call with null trackers.");
        }

        public void onFilterComplete(int i) {
            Rlog.e(SMSDispatcher.TAG, "Unexpected onFilterComplete call with result: " + i);
        }

        public void onSendMmsComplete(int i, byte[] bArr) {
            Rlog.e(SMSDispatcher.TAG, "Unexpected onSendMmsComplete call with result: " + i);
        }

        public void onDownloadMmsComplete(int i) {
            Rlog.e(SMSDispatcher.TAG, "Unexpected onDownloadMmsComplete call with result: " + i);
        }
    }

    protected void sendSubmitPdu(SmsTracker smsTracker) {
        if (shouldBlockSmsForEcbm()) {
            Rlog.d(TAG, "Block SMS in Emergency Callback mode");
            smsTracker.onFailed(this.mContext, 4, 0);
        } else {
            sendRawPdu(smsTracker);
        }
    }

    protected void handleSendComplete(AsyncResult asyncResult) {
        SmsTracker smsTracker = (SmsTracker) asyncResult.userObj;
        PendingIntent pendingIntent = smsTracker.mSentIntent;
        if (asyncResult.result != null) {
            smsTracker.mMessageRef = ((SmsResponse) asyncResult.result).mMessageRef;
        } else {
            Rlog.d(TAG, "SmsResponse was null");
        }
        if (asyncResult.exception == null) {
            if (smsTracker.mDeliveryIntent != null) {
                this.deliveryPendingList.add(smsTracker);
            }
            smsTracker.onSent(this.mContext);
            return;
        }
        int state = this.mPhone.getServiceState().getState();
        if (smsTracker.mImsRetry > 0 && state != 0) {
            smsTracker.mRetryCount = 3;
            Rlog.d(TAG, "handleSendComplete: Skipping retry:  isIms()=" + isIms() + " mRetryCount=" + smsTracker.mRetryCount + " mImsRetry=" + smsTracker.mImsRetry + " mMessageRef=" + smsTracker.mMessageRef + " SS= " + this.mPhone.getServiceState().getState());
        }
        if (!isIms() && state != 0) {
            smsTracker.onFailed(this.mContext, getNotInServiceError(state), 0);
            return;
        }
        if (((CommandException) asyncResult.exception).getCommandError() == CommandException.Error.SMS_FAIL_RETRY && smsTracker.mRetryCount < 3) {
            smsTracker.mRetryCount++;
            sendMessageDelayed(obtainMessage(3, smsTracker), 2000L);
        } else {
            smsTracker.onFailed(this.mContext, ((CommandException) asyncResult.exception).getCommandError() == CommandException.Error.FDN_CHECK_FAILURE ? 6 : 1, asyncResult.result != null ? ((SmsResponse) asyncResult.result).mErrorCode : 0);
        }
    }

    protected static void handleNotInService(int i, PendingIntent pendingIntent) {
        if (pendingIntent != null) {
            try {
                if (i == 3) {
                    pendingIntent.send(2);
                } else {
                    pendingIntent.send(4);
                }
            } catch (PendingIntent.CanceledException e) {
                Rlog.e(TAG, "Failed to send result");
            }
        }
    }

    protected static int getNotInServiceError(int i) {
        if (i == 3) {
            return 2;
        }
        return 4;
    }

    protected void sendData(String str, String str2, int i, byte[] bArr, PendingIntent pendingIntent, PendingIntent pendingIntent2) {
        SmsMessageBase.SubmitPduBase submitPduBaseOnSendData = onSendData(str, str2, i, bArr, pendingIntent, pendingIntent2);
        if (submitPduBaseOnSendData != null) {
            SmsTracker smsTracker = getSmsTracker(getSmsTrackerMap(str, str2, i, bArr, submitPduBaseOnSendData), pendingIntent, pendingIntent2, getFormat(), null, false, null, false, true);
            if (!sendSmsByCarrierApp(true, smsTracker)) {
                sendSubmitPdu(smsTracker);
                return;
            }
            return;
        }
        Rlog.e(TAG, "SMSDispatcher.sendData(): getSubmitPdu() returned null");
        triggerSentIntentForFailure(pendingIntent);
    }

    public void sendText(String str, String str2, String str3, PendingIntent pendingIntent, PendingIntent pendingIntent2, Uri uri, String str4, boolean z, int i, boolean z2, int i2) {
        Rlog.d(TAG, "sendText");
        SmsMessageBase.SubmitPduBase submitPduBaseOnSendText = onSendText(str, str2, str3, pendingIntent, pendingIntent2, uri, str4, z, i, z2, i2);
        if (submitPduBaseOnSendText != null) {
            SmsTracker smsTracker = getSmsTracker(getSmsTrackerMap(str, str2, str3, submitPduBaseOnSendText), pendingIntent, pendingIntent2, getFormat(), uri, z2, str3, true, z, i, i2);
            if (!sendSmsByCarrierApp(false, smsTracker)) {
                sendSubmitPdu(smsTracker);
                return;
            }
            return;
        }
        Rlog.e(TAG, "SmsDispatcher.sendText(): getSubmitPdu() returned null");
        triggerSentIntentForFailure(pendingIntent);
    }

    private void triggerSentIntentForFailure(PendingIntent pendingIntent) {
        if (pendingIntent != null) {
            try {
                pendingIntent.send(1);
            } catch (PendingIntent.CanceledException e) {
                Rlog.e(TAG, "Intent has been canceled!");
            }
        }
    }

    protected boolean sendSmsByCarrierApp(boolean z, SmsTracker smsTracker) {
        SmsSender textSmsSender;
        String carrierAppPackageName = getCarrierAppPackageName();
        if (carrierAppPackageName != null) {
            Rlog.d(TAG, "Found carrier package.");
            if (z) {
                textSmsSender = new DataSmsSender(smsTracker);
            } else {
                textSmsSender = new TextSmsSender(smsTracker);
            }
            textSmsSender.sendSmsByCarrierApp(carrierAppPackageName, new SmsSenderCallback(textSmsSender));
            return true;
        }
        return false;
    }

    protected void sendMultipartText(String str, String str2, ArrayList<String> arrayList, ArrayList<PendingIntent> arrayList2, ArrayList<PendingIntent> arrayList3, Uri uri, String str3, boolean z, int i, boolean z2, int i2) {
        PendingIntent pendingIntent;
        ArrayList<PendingIntent> arrayList4 = arrayList2;
        String multipartMessageText = getMultipartMessageText(arrayList);
        int nextConcatenatedRef = getNextConcatenatedRef() & 255;
        int size = arrayList.size();
        GsmAlphabet.TextEncodingDetails[] textEncodingDetailsArr = new GsmAlphabet.TextEncodingDetails[size];
        int i3 = size;
        int i4 = nextConcatenatedRef;
        int iOnSendMultipartText = onSendMultipartText(str, str2, arrayList, arrayList4, arrayList3, uri, str3, z, i, z2, i2, textEncodingDetailsArr);
        SmsTracker[] smsTrackerArr = new SmsTracker[i3];
        AtomicInteger atomicInteger = new AtomicInteger(i3);
        boolean z3 = false;
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        int i5 = 0;
        while (i5 < i3) {
            SmsHeader.ConcatRef concatRef = new SmsHeader.ConcatRef();
            concatRef.refNumber = i4;
            int i6 = i5 + 1;
            concatRef.seqNumber = i6;
            concatRef.msgCount = i3;
            concatRef.isEightBits = true;
            SmsHeader smsHeaderMakeSmsHeader = TelephonyComponentFactory.getInstance().makeSmsHeader();
            smsHeaderMakeSmsHeader.concatRef = concatRef;
            if (iOnSendMultipartText == 1) {
                smsHeaderMakeSmsHeader.languageTable = textEncodingDetailsArr[i5].languageTable;
                smsHeaderMakeSmsHeader.languageShiftTable = textEncodingDetailsArr[i5].languageShiftTable;
            }
            PendingIntent pendingIntent2 = null;
            if (arrayList4 == null || arrayList2.size() <= i5) {
                pendingIntent = null;
            } else {
                pendingIntent = arrayList4.get(i5);
            }
            if (arrayList3 != null && arrayList3.size() > i5) {
                pendingIntent2 = arrayList3.get(i5);
            }
            PendingIntent pendingIntent3 = pendingIntent2;
            int i7 = i4;
            String str4 = arrayList.get(i5);
            int i8 = i3;
            PendingIntent pendingIntent4 = pendingIntent;
            int i9 = i5;
            boolean z4 = z3;
            boolean z5 = i5 == i3 + (-1) ? true : z3;
            AtomicBoolean atomicBoolean2 = atomicBoolean;
            SmsTracker[] smsTrackerArr2 = smsTrackerArr;
            smsTrackerArr2[i9] = getNewSubmitPduTracker(str, str2, str4, smsHeaderMakeSmsHeader, iOnSendMultipartText, pendingIntent4, pendingIntent3, z5, atomicInteger, atomicBoolean2, uri, multipartMessageText, i, z2, i2);
            smsTrackerArr2[i9].mPersistMessage = z;
            arrayList4 = arrayList2;
            smsTrackerArr = smsTrackerArr2;
            i3 = i8;
            i4 = i7;
            z3 = z4;
            atomicBoolean = atomicBoolean2;
            i5 = i6;
            atomicInteger = atomicInteger;
            iOnSendMultipartText = iOnSendMultipartText;
        }
        boolean z6 = z3;
        SmsTracker[] smsTrackerArr3 = smsTrackerArr;
        if (arrayList == null || smsTrackerArr3.length == 0 || smsTrackerArr3[z6 ? 1 : 0] == null) {
            Rlog.e(TAG, "Cannot send multipart text. parts=" + arrayList + " trackers=" + smsTrackerArr3);
            return;
        }
        String carrierAppPackageName = getCarrierAppPackageName();
        if (carrierAppPackageName != null) {
            Rlog.d(TAG, "Found carrier package.");
            MultipartSmsSender multipartSmsSender = new MultipartSmsSender(arrayList, smsTrackerArr3);
            multipartSmsSender.sendSmsByCarrierApp(carrierAppPackageName, new MultipartSmsSenderCallback(multipartSmsSender));
            return;
        }
        Rlog.v(TAG, "No carrier package.");
        int length = smsTrackerArr3.length;
        for (int i10 = z6 ? 1 : 0; i10 < length; i10++) {
            SmsTracker smsTracker = smsTrackerArr3[i10];
            if (smsTracker != null) {
                sendSubmitPdu(smsTracker);
            } else {
                Rlog.e(TAG, "Null tracker.");
            }
        }
    }

    protected SmsTracker getNewSubmitPduTracker(String str, String str2, String str3, SmsHeader smsHeader, int i, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z, AtomicInteger atomicInteger, AtomicBoolean atomicBoolean, Uri uri, String str4, int i2, boolean z2, int i3) {
        if (isCdmaMo()) {
            return getSmsTracker(getSmsTrackerMap(str, str2, str3, onGetNewSubmitCdmaPduTracker(str, str2, str3, smsHeader, i, pendingIntent, pendingIntent2, z, atomicInteger, atomicBoolean, uri, str4, i2, z2, i3)), pendingIntent, pendingIntent2, getFormat(), atomicInteger, atomicBoolean, uri, smsHeader, !z || z2, str4, true, true, i2, i3);
        }
        SmsMessage.SubmitPdu submitPdu = SmsMessage.getSubmitPdu(str2, str, str3, pendingIntent2 != null, SmsHeader.toByteArray(smsHeader), i, smsHeader.languageTable, smsHeader.languageShiftTable, i3);
        if (submitPdu != null) {
            return getSmsTracker(getSmsTrackerMap(str, str2, str3, submitPdu), pendingIntent, pendingIntent2, getFormat(), atomicInteger, atomicBoolean, uri, smsHeader, !z || z2, str4, true, false, i2, i3);
        }
        Rlog.e(TAG, "GsmSMSDispatcher.sendNewSubmitPdu(): getSubmitPdu() returned null");
        return null;
    }

    @VisibleForTesting
    public void sendRawPdu(SmsTracker smsTracker) {
        byte[] bArr = (byte[]) smsTracker.getData().get(MAP_KEY_PDU);
        if (this.mSmsSendDisabled) {
            Rlog.e(TAG, "Device does not support sending sms.");
            smsTracker.onFailed(this.mContext, 4, 0);
            return;
        }
        if (bArr == null) {
            Rlog.e(TAG, "Empty PDU");
            smsTracker.onFailed(this.mContext, 3, 0);
            return;
        }
        PackageManager packageManager = this.mContext.getPackageManager();
        String[] packagesForUid = packageManager.getPackagesForUid(Binder.getCallingUid());
        if (packagesForUid == null || packagesForUid.length == 0) {
            Rlog.e(TAG, "Can't get calling app package name: refusing to send SMS");
            smsTracker.onFailed(this.mContext, 1, 0);
            return;
        }
        packagesForUid[0] = getPackageNameViaProcessId(packagesForUid);
        try {
            PackageInfo packageInfoAsUser = packageManager.getPackageInfoAsUser(packagesForUid[0], 64, smsTracker.mUserId);
            if (checkDestination(smsTracker)) {
                if (!this.mSmsDispatchersController.getUsageMonitor().check(packageInfoAsUser.packageName, 1)) {
                    sendMessage(obtainMessage(4, smsTracker));
                    return;
                }
                sendSms(smsTracker);
            }
            if (PhoneNumberUtils.isLocalEmergencyNumber(this.mContext, smsTracker.mDestAddress)) {
                new AsyncEmergencyContactNotifier(this.mContext).execute(new Void[0]);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Rlog.e(TAG, "Can't get calling app package info: refusing to send SMS");
            smsTracker.onFailed(this.mContext, 1, 0);
        }
    }

    protected boolean checkDestination(SmsTracker smsTracker) {
        int iCheckDestination;
        int i;
        if (this.mContext.checkCallingOrSelfPermission("android.permission.SEND_SMS_NO_CONFIRMATION") == 0) {
            return true;
        }
        int i2 = this.mPremiumSmsRule.get();
        if (i2 == 1 || i2 == 3) {
            String simCountryIso = this.mTelephonyManager.getSimCountryIso(getSubId());
            if (simCountryIso == null || simCountryIso.length() != 2) {
                Rlog.e(TAG, "Can't get SIM country Iso: trying network country Iso");
                simCountryIso = this.mTelephonyManager.getNetworkCountryIso(getSubId());
            }
            iCheckDestination = this.mSmsDispatchersController.getUsageMonitor().checkDestination(smsTracker.mDestAddress, simCountryIso);
        } else {
            iCheckDestination = 0;
        }
        if (i2 == 2 || i2 == 3) {
            String networkCountryIso = this.mTelephonyManager.getNetworkCountryIso(getSubId());
            if (networkCountryIso == null || networkCountryIso.length() != 2) {
                Rlog.e(TAG, "Can't get Network country Iso: trying SIM country Iso");
                networkCountryIso = this.mTelephonyManager.getSimCountryIso(getSubId());
            }
            iCheckDestination = SmsUsageMonitor.mergeShortCodeCategories(iCheckDestination, this.mSmsDispatchersController.getUsageMonitor().checkDestination(smsTracker.mDestAddress, networkCountryIso));
        }
        if (iCheckDestination == 0 || iCheckDestination == 1 || iCheckDestination == 2) {
            return true;
        }
        if (Settings.Global.getInt(this.mResolver, "device_provisioned", 0) == 0) {
            Rlog.e(TAG, "Can't send premium sms during Setup Wizard");
            return false;
        }
        int premiumSmsPermission = this.mSmsDispatchersController.getUsageMonitor().getPremiumSmsPermission(smsTracker.getAppPackageName());
        if (premiumSmsPermission == 0) {
            premiumSmsPermission = 1;
        }
        switch (premiumSmsPermission) {
            case 2:
                Rlog.w(TAG, "User denied this app from sending to premium SMS");
                Message messageObtainMessage = obtainMessage(7, smsTracker);
                messageObtainMessage.arg1 = 0;
                messageObtainMessage.arg2 = 1;
                sendMessage(messageObtainMessage);
                return false;
            case 3:
                Rlog.d(TAG, "User approved this app to send to premium SMS");
                return true;
            default:
                if (iCheckDestination == 3) {
                    i = 8;
                } else {
                    i = 9;
                }
                sendMessage(obtainMessage(i, smsTracker));
                return false;
        }
    }

    private boolean denyIfQueueLimitReached(SmsTracker smsTracker) {
        if (this.mPendingTrackerCount >= 5) {
            Rlog.e(TAG, "Denied because queue limit reached");
            smsTracker.onFailed(this.mContext, 5, 0);
            return true;
        }
        this.mPendingTrackerCount++;
        return false;
    }

    private CharSequence getAppLabel(String str, int i) {
        PackageManager packageManager = this.mContext.getPackageManager();
        try {
            return packageManager.getApplicationInfoAsUser(str, 0, i).loadSafeLabel(packageManager);
        } catch (PackageManager.NameNotFoundException e) {
            Rlog.e(TAG, "PackageManager Name Not Found for package " + str);
            return str;
        }
    }

    protected void handleReachSentLimit(SmsTracker smsTracker) {
        if (denyIfQueueLimitReached(smsTracker)) {
            return;
        }
        CharSequence appLabel = getAppLabel(smsTracker.getAppPackageName(), smsTracker.mUserId);
        Resources system = Resources.getSystem();
        Spanned spannedFromHtml = Html.fromHtml(system.getString(R.string.mediasize_chinese_prc_7, appLabel));
        ConfirmDialogListener confirmDialogListener = new ConfirmDialogListener(smsTracker, null, 1);
        AlertDialog alertDialogCreate = new AlertDialog.Builder(this.mContext).setTitle(R.string.mediasize_chinese_prc_9).setIcon(R.drawable.stat_sys_warning).setMessage(spannedFromHtml).setPositiveButton(system.getString(R.string.mediasize_chinese_roc_16k), confirmDialogListener).setNegativeButton(system.getString(R.string.mediasize_chinese_prc_8), confirmDialogListener).setOnCancelListener(confirmDialogListener).create();
        alertDialogCreate.getWindow().setType(2003);
        alertDialogCreate.show();
    }

    protected void handleConfirmShortCode(boolean z, SmsTracker smsTracker) {
        int i;
        if (denyIfQueueLimitReached(smsTracker)) {
            return;
        }
        if (z) {
            i = R.string.mediasize_iso_a0;
        } else {
            i = R.string.mediasize_iso_a5;
        }
        CharSequence appLabel = getAppLabel(smsTracker.getAppPackageName(), smsTracker.mUserId);
        Resources system = Resources.getSystem();
        Spanned spannedFromHtml = Html.fromHtml(system.getString(R.string.mediasize_iso_a3, appLabel, smsTracker.mDestAddress));
        View viewInflate = ((LayoutInflater) this.mContext.getSystemService("layout_inflater")).inflate(R.layout.notification_template_text_multiline, (ViewGroup) null);
        ConfirmDialogListener confirmDialogListener = new ConfirmDialogListener(smsTracker, (TextView) viewInflate.findViewById(R.id.music), 0);
        ((TextView) viewInflate.findViewById(R.id.multi-select)).setText(spannedFromHtml);
        ((TextView) ((ViewGroup) viewInflate.findViewById(R.id.multiple)).findViewById(R.id.multipleChoice)).setText(i);
        ((CheckBox) viewInflate.findViewById(R.id.multipleChoiceModal)).setOnCheckedChangeListener(confirmDialogListener);
        AlertDialog alertDialogCreate = new AlertDialog.Builder(this.mContext).setView(viewInflate).setPositiveButton(system.getString(R.string.mediasize_iso_a1), confirmDialogListener).setNegativeButton(system.getString(R.string.mediasize_iso_a2), confirmDialogListener).setOnCancelListener(confirmDialogListener).create();
        alertDialogCreate.getWindow().setType(2003);
        alertDialogCreate.show();
        confirmDialogListener.setPositiveButton(alertDialogCreate.getButton(-1));
        confirmDialogListener.setNegativeButton(alertDialogCreate.getButton(-2));
    }

    public void sendRetrySms(SmsTracker smsTracker) {
        if (this.mSmsDispatchersController != null) {
            this.mSmsDispatchersController.sendRetrySms(smsTracker);
            return;
        }
        Rlog.e(TAG, this.mSmsDispatchersController + " is null. Retry failed");
    }

    protected void sendMultipartSms(SmsTracker smsTracker) {
        HashMap<String, Object> data = smsTracker.getData();
        String str = (String) data.get("destination");
        String str2 = (String) data.get("scaddress");
        ArrayList<String> arrayList = (ArrayList) data.get("parts");
        ArrayList<PendingIntent> arrayList2 = (ArrayList) data.get("sentIntents");
        ArrayList<PendingIntent> arrayList3 = (ArrayList) data.get("deliveryIntents");
        int state = this.mPhone.getServiceState().getState();
        if (!isIms() && state != 0) {
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                PendingIntent pendingIntent = null;
                if (arrayList2 != null && arrayList2.size() > i) {
                    pendingIntent = arrayList2.get(i);
                }
                handleNotInService(state, pendingIntent);
            }
            return;
        }
        sendMultipartText(str, str2, arrayList, arrayList2, arrayList3, null, null, smsTracker.mPersistMessage, smsTracker.mPriority, smsTracker.mExpectMore, smsTracker.mValidityPeriod);
    }

    public static class SmsTracker {
        private AtomicBoolean mAnyPartFailed;
        public final PackageInfo mAppInfo;
        public final HashMap<String, Object> mData;
        public final PendingIntent mDeliveryIntent;
        public final String mDestAddress;
        public boolean mExpectMore;
        public String mFormat;
        public String mFullMessageText;
        private boolean mIsText;
        public Uri mMessageUri;
        public boolean mPersistMessage;
        public int mPriority;
        public final PendingIntent mSentIntent;
        public final SmsHeader mSmsHeader;
        public int mSubId;
        private AtomicInteger mUnsentPartCount;
        public final int mUserId;
        public int mValidityPeriod;
        protected static String PDU_SIZE = "pdu_size";
        protected static String MSG_REF_NUM = "msg_ref_num";
        private long mTimestamp = System.currentTimeMillis();
        public int mRetryCount = 0;
        public int mImsRetry = 0;
        public boolean mUsesImsServiceForIms = false;
        public int mMessageRef = 0;

        public SmsTracker(HashMap<String, Object> map, PendingIntent pendingIntent, PendingIntent pendingIntent2, PackageInfo packageInfo, String str, String str2, AtomicInteger atomicInteger, AtomicBoolean atomicBoolean, Uri uri, SmsHeader smsHeader, boolean z, String str3, int i, boolean z2, boolean z3, int i2, int i3, int i4) {
            this.mData = map;
            this.mSentIntent = pendingIntent;
            this.mDeliveryIntent = pendingIntent2;
            this.mAppInfo = packageInfo;
            this.mDestAddress = str;
            this.mFormat = str2;
            this.mExpectMore = z;
            this.mUnsentPartCount = atomicInteger;
            this.mAnyPartFailed = atomicBoolean;
            this.mMessageUri = uri;
            this.mSmsHeader = smsHeader;
            this.mFullMessageText = str3;
            this.mSubId = i;
            this.mIsText = z2;
            this.mPersistMessage = z3;
            this.mUserId = i2;
            this.mPriority = i3;
            this.mValidityPeriod = i4;
        }

        boolean isMultipart() {
            return this.mData.containsKey("parts");
        }

        public HashMap<String, Object> getData() {
            return this.mData;
        }

        public String getAppPackageName() {
            if (this.mAppInfo != null) {
                return this.mAppInfo.packageName;
            }
            return null;
        }

        public void updateSentMessageStatus(Context context, int i) {
            if (this.mMessageUri != null) {
                ContentValues contentValues = new ContentValues(1);
                contentValues.put("status", Integer.valueOf(i));
                SqliteWrapper.update(context, context.getContentResolver(), this.mMessageUri, contentValues, (String) null, (String[]) null);
            }
        }

        private void updateMessageState(Context context, int i, int i2) {
            if (this.mMessageUri == null) {
                return;
            }
            ContentValues contentValues = new ContentValues(2);
            contentValues.put("type", Integer.valueOf(i));
            contentValues.put("error_code", Integer.valueOf(i2));
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                if (SqliteWrapper.update(context, context.getContentResolver(), this.mMessageUri, contentValues, (String) null, (String[]) null) != 1) {
                    Rlog.e(SMSDispatcher.TAG, "Failed to move message to " + i);
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        private Uri persistSentMessageIfRequired(Context context, int i, int i2) {
            if (!this.mIsText || !this.mPersistMessage || !SmsApplication.shouldWriteMessageForPackage(this.mAppInfo.packageName, context)) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Persist SMS into ");
            sb.append(i == 5 ? "FAILED" : "SENT");
            Rlog.d(SMSDispatcher.TAG, sb.toString());
            ContentValues contentValues = new ContentValues();
            contentValues.put("sub_id", Integer.valueOf(this.mSubId));
            contentValues.put("address", this.mDestAddress);
            contentValues.put("body", this.mFullMessageText);
            contentValues.put("date", Long.valueOf(System.currentTimeMillis()));
            contentValues.put("seen", (Integer) 1);
            contentValues.put("read", (Integer) 1);
            String str = this.mAppInfo != null ? this.mAppInfo.packageName : null;
            if (!TextUtils.isEmpty(str)) {
                contentValues.put("creator", str);
            }
            if (this.mDeliveryIntent != null) {
                contentValues.put("status", (Integer) 32);
            }
            if (i2 != 0) {
                contentValues.put("error_code", Integer.valueOf(i2));
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            ContentResolver contentResolver = context.getContentResolver();
            try {
                Uri uriInsert = contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, contentValues);
                if (uriInsert != null && i == 5) {
                    ContentValues contentValues2 = new ContentValues(1);
                    contentValues2.put("type", (Integer) 5);
                    contentResolver.update(uriInsert, contentValues2, null, null);
                }
                return uriInsert;
            } catch (Exception e) {
                Rlog.e(SMSDispatcher.TAG, "writeOutboxMessage: Failed to persist outbox message", e);
                return null;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        private void persistOrUpdateMessage(Context context, int i, int i2) {
            if (this.mMessageUri != null) {
                updateMessageState(context, i, i2);
            } else {
                this.mMessageUri = persistSentMessageIfRequired(context, i, i2);
            }
        }

        public void onFailed(Context context, int i, int i2) {
            if (this.mAnyPartFailed != null) {
                this.mAnyPartFailed.set(true);
            }
            boolean z = this.mUnsentPartCount == null || this.mUnsentPartCount.decrementAndGet() == 0;
            if (z) {
                new AsyncPersistOrUpdateTask(context, 5, i2, i, true).execute(new Void[0]);
                return;
            }
            if (this.mSentIntent != null) {
                try {
                    Intent intent = new Intent();
                    if (this.mMessageUri != null) {
                        intent.putExtra("uri", this.mMessageUri.toString());
                    }
                    if (i2 != 0) {
                        intent.putExtra("errorCode", i2);
                    }
                    if (this.mUnsentPartCount != null && z) {
                        intent.putExtra(SMSDispatcher.SEND_NEXT_MSG_EXTRA, true);
                    }
                    putPduSize(intent);
                    this.mSentIntent.send(context, i, intent);
                } catch (PendingIntent.CanceledException e) {
                    Rlog.e(SMSDispatcher.TAG, "Failed to send result");
                }
            }
        }

        private void putPduSize(Intent intent) {
            int length;
            if (this.mData != null) {
                if (this.mData.get(SMSDispatcher.MAP_KEY_SMSC) != null) {
                    length = ((byte[]) this.mData.get(SMSDispatcher.MAP_KEY_SMSC)).length;
                } else {
                    length = 0;
                }
                length = (this.mData.get(SMSDispatcher.MAP_KEY_PDU) != null ? ((byte[]) this.mData.get(SMSDispatcher.MAP_KEY_PDU)).length : 0) + length;
            }
            intent.putExtra(PDU_SIZE, length);
        }

        public void onSent(Context context) {
            boolean z = this.mUnsentPartCount == null || this.mUnsentPartCount.decrementAndGet() == 0;
            if (z) {
                int i = 2;
                if (this.mAnyPartFailed != null && this.mAnyPartFailed.get()) {
                    i = 5;
                }
                new AsyncPersistOrUpdateTask(context, i, 0, 0, false).execute(new Void[0]);
                return;
            }
            if (this.mSentIntent != null) {
                try {
                    Intent intent = new Intent();
                    if (this.mMessageUri != null) {
                        intent.putExtra("uri", this.mMessageUri.toString());
                    }
                    if (this.mUnsentPartCount != null && z) {
                        intent.putExtra(SMSDispatcher.SEND_NEXT_MSG_EXTRA, true);
                    }
                    putPduSize(intent);
                    intent.putExtra(MSG_REF_NUM, this.mMessageRef);
                    Rlog.d(SMSDispatcher.TAG, "message reference number : " + this.mMessageRef);
                    this.mSentIntent.send(context, -1, intent);
                } catch (PendingIntent.CanceledException e) {
                    Rlog.e(SMSDispatcher.TAG, "Failed to send result");
                }
            }
        }

        class AsyncPersistOrUpdateTask extends AsyncTask<Void, Void, Void> {
            private final Context mContext;
            private int mError;
            private int mErrorCode;
            private boolean mFail;
            private int mMessageType;

            public AsyncPersistOrUpdateTask(Context context, int i, int i2, int i3, boolean z) {
                this.mContext = context;
                this.mMessageType = i;
                this.mErrorCode = i2;
                this.mError = i3;
                this.mFail = z;
            }

            @Override
            protected Void doInBackground(Void... voidArr) {
                SmsTracker.this.persistOrUpdateMessage(this.mContext, this.mMessageType, this.mErrorCode);
                return null;
            }

            @Override
            protected void onPostExecute(Void r5) {
                int length;
                if (SmsTracker.this.mSentIntent != null) {
                    try {
                        Intent intent = new Intent();
                        if (SmsTracker.this.mMessageUri != null) {
                            intent.putExtra("uri", SmsTracker.this.mMessageUri.toString());
                        }
                        if (this.mFail && this.mErrorCode != 0) {
                            intent.putExtra("errorCode", this.mErrorCode);
                        }
                        if (SmsTracker.this.mUnsentPartCount != null) {
                            intent.putExtra(SMSDispatcher.SEND_NEXT_MSG_EXTRA, true);
                        }
                        if (SmsTracker.this.mData != null) {
                            if (SmsTracker.this.mData.get(SMSDispatcher.MAP_KEY_SMSC) != null) {
                                length = ((byte[]) SmsTracker.this.mData.get(SMSDispatcher.MAP_KEY_SMSC)).length;
                            } else {
                                length = 0;
                            }
                            length = (SmsTracker.this.mData.get(SMSDispatcher.MAP_KEY_PDU) != null ? ((byte[]) SmsTracker.this.mData.get(SMSDispatcher.MAP_KEY_PDU)).length : 0) + length;
                        }
                        intent.putExtra(SmsTracker.PDU_SIZE, length);
                        if (!this.mFail) {
                            intent.putExtra(SmsTracker.MSG_REF_NUM, SmsTracker.this.mMessageRef);
                            Rlog.d(SMSDispatcher.TAG, "message reference number : " + SmsTracker.this.mMessageRef);
                            SmsTracker.this.mSentIntent.send(this.mContext, -1, intent);
                            return;
                        }
                        SmsTracker.this.mSentIntent.send(this.mContext, this.mError, intent);
                    } catch (PendingIntent.CanceledException e) {
                        Rlog.e(SMSDispatcher.TAG, "Failed to send result");
                    }
                }
            }
        }
    }

    protected SmsTracker getSmsTracker(HashMap<String, Object> map, PendingIntent pendingIntent, PendingIntent pendingIntent2, String str, AtomicInteger atomicInteger, AtomicBoolean atomicBoolean, Uri uri, SmsHeader smsHeader, boolean z, String str2, boolean z2, boolean z3, int i, int i2) {
        PackageInfo packageInfoAsUser;
        PackageManager packageManager = this.mContext.getPackageManager();
        String[] packagesForUid = packageManager.getPackagesForUid(Binder.getCallingUid());
        int callingUserId = UserHandle.getCallingUserId();
        if (packagesForUid != null && packagesForUid.length > 0) {
            try {
                packagesForUid[0] = getPackageNameViaProcessId(packagesForUid);
                packageInfoAsUser = packageManager.getPackageInfoAsUser(packagesForUid[0], 64, callingUserId);
            } catch (PackageManager.NameNotFoundException e) {
                packageInfoAsUser = null;
            }
        } else {
            packageInfoAsUser = null;
        }
        return new SmsTracker(map, pendingIntent, pendingIntent2, packageInfoAsUser, PhoneNumberUtils.extractNetworkPortion((String) map.get(MAP_KEY_DEST_ADDR)), str, atomicInteger, atomicBoolean, uri, smsHeader, z, str2, getSubId(), z2, z3, callingUserId, i, i2);
    }

    protected SmsTracker getSmsTracker(HashMap<String, Object> map, PendingIntent pendingIntent, PendingIntent pendingIntent2, String str, Uri uri, boolean z, String str2, boolean z2, boolean z3) {
        return getSmsTracker(map, pendingIntent, pendingIntent2, str, null, null, uri, null, z, str2, z2, z3, -1, -1);
    }

    protected SmsTracker getSmsTracker(HashMap<String, Object> map, PendingIntent pendingIntent, PendingIntent pendingIntent2, String str, Uri uri, boolean z, String str2, boolean z2, boolean z3, int i, int i2) {
        return getSmsTracker(map, pendingIntent, pendingIntent2, str, null, null, uri, null, z, str2, z2, z3, i, i2);
    }

    protected HashMap<String, Object> getSmsTrackerMap(String str, String str2, String str3, SmsMessageBase.SubmitPduBase submitPduBase) {
        HashMap<String, Object> map = new HashMap<>();
        map.put(MAP_KEY_DEST_ADDR, str);
        map.put(MAP_KEY_SC_ADDR, str2);
        map.put(MAP_KEY_TEXT, str3);
        map.put(MAP_KEY_SMSC, submitPduBase.encodedScAddress);
        map.put(MAP_KEY_PDU, submitPduBase.encodedMessage);
        return map;
    }

    protected HashMap<String, Object> getSmsTrackerMap(String str, String str2, int i, byte[] bArr, SmsMessageBase.SubmitPduBase submitPduBase) {
        HashMap<String, Object> map = new HashMap<>();
        map.put(MAP_KEY_DEST_ADDR, str);
        map.put(MAP_KEY_SC_ADDR, str2);
        map.put(MAP_KEY_DEST_PORT, Integer.valueOf(i));
        map.put(MAP_KEY_DATA, bArr);
        map.put(MAP_KEY_SMSC, submitPduBase.encodedScAddress);
        map.put(MAP_KEY_PDU, submitPduBase.encodedMessage);
        return map;
    }

    private final class ConfirmDialogListener implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener, CompoundButton.OnCheckedChangeListener {
        private static final int NEVER_ALLOW = 1;
        private static final int RATE_LIMIT = 1;
        private static final int SHORT_CODE_MSG = 0;
        private int mConfirmationType;
        private Button mNegativeButton;
        private Button mPositiveButton;
        private boolean mRememberChoice;
        private final TextView mRememberUndoInstruction;
        private final SmsTracker mTracker;

        ConfirmDialogListener(SmsTracker smsTracker, TextView textView, int i) {
            this.mTracker = smsTracker;
            this.mRememberUndoInstruction = textView;
            this.mConfirmationType = i;
        }

        void setPositiveButton(Button button) {
            this.mPositiveButton = button;
        }

        void setNegativeButton(Button button) {
            this.mNegativeButton = button;
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            int i2 = 1;
            if (i == -1) {
                Rlog.d(SMSDispatcher.TAG, "CONFIRM sending SMS");
                EventLog.writeEvent(EventLogTags.EXP_DET_SMS_SENT_BY_USER, this.mTracker.mAppInfo.applicationInfo != null ? this.mTracker.mAppInfo.applicationInfo.uid : -1);
                SMSDispatcher.this.sendMessage(SMSDispatcher.this.obtainMessage(5, this.mTracker));
                if (this.mRememberChoice) {
                    i2 = 3;
                }
            } else if (i == -2) {
                Rlog.d(SMSDispatcher.TAG, "DENY sending SMS");
                EventLog.writeEvent(EventLogTags.EXP_DET_SMS_DENIED_BY_USER, this.mTracker.mAppInfo.applicationInfo != null ? this.mTracker.mAppInfo.applicationInfo.uid : -1);
                Message messageObtainMessage = SMSDispatcher.this.obtainMessage(7, this.mTracker);
                messageObtainMessage.arg1 = this.mConfirmationType;
                if (this.mRememberChoice) {
                    messageObtainMessage.arg2 = 1;
                    i2 = 2;
                }
                SMSDispatcher.this.sendMessage(messageObtainMessage);
            }
            SMSDispatcher.this.mSmsDispatchersController.setPremiumSmsPermission(this.mTracker.getAppPackageName(), i2);
        }

        @Override
        public void onCancel(DialogInterface dialogInterface) {
            Rlog.d(SMSDispatcher.TAG, "dialog dismissed: don't send SMS");
            Message messageObtainMessage = SMSDispatcher.this.obtainMessage(7, this.mTracker);
            messageObtainMessage.arg1 = this.mConfirmationType;
            SMSDispatcher.this.sendMessage(messageObtainMessage);
        }

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
            Rlog.d(SMSDispatcher.TAG, "remember this choice: " + z);
            this.mRememberChoice = z;
            if (z) {
                this.mPositiveButton.setText(R.string.mediasize_iso_a10);
                this.mNegativeButton.setText(R.string.mediasize_iso_a4);
                if (this.mRememberUndoInstruction != null) {
                    this.mRememberUndoInstruction.setText(R.string.mediasize_iso_a7);
                    this.mRememberUndoInstruction.setPadding(0, 0, 0, 32);
                    return;
                }
                return;
            }
            this.mPositiveButton.setText(R.string.mediasize_iso_a1);
            this.mNegativeButton.setText(R.string.mediasize_iso_a2);
            if (this.mRememberUndoInstruction != null) {
                this.mRememberUndoInstruction.setText("");
                this.mRememberUndoInstruction.setPadding(0, 0, 0, 0);
            }
        }
    }

    public boolean isIms() {
        if (this.mSmsDispatchersController != null) {
            return this.mSmsDispatchersController.isIms();
        }
        Rlog.e(TAG, "mSmsDispatchersController  is null");
        return false;
    }

    protected String getMultipartMessageText(ArrayList<String> arrayList) {
        StringBuilder sb = new StringBuilder();
        for (String str : arrayList) {
            if (str != null) {
                sb.append(str);
            }
        }
        return sb.toString();
    }

    protected String getCarrierAppPackageName() {
        UiccCard uiccCard = UiccController.getInstance().getUiccCard(this.mPhone.getPhoneId());
        if (uiccCard == null) {
            return null;
        }
        List<String> carrierPackageNamesForIntent = uiccCard.getCarrierPackageNamesForIntent(this.mContext.getPackageManager(), new Intent("android.service.carrier.CarrierMessagingService"));
        if (carrierPackageNamesForIntent != null && carrierPackageNamesForIntent.size() == 1) {
            return carrierPackageNamesForIntent.get(0);
        }
        return CarrierSmsUtils.getCarrierImsPackageForIntent(this.mContext, this.mPhone, new Intent("android.service.carrier.CarrierMessagingService"));
    }

    protected int getSubId() {
        return SubscriptionController.getInstance().getSubIdUsingPhoneId(this.mPhone.getPhoneId());
    }

    private void checkCallerIsPhoneOrCarrierApp() {
        int callingUid = Binder.getCallingUid();
        if (UserHandle.getAppId(callingUid) == 1001 || callingUid == 0) {
            return;
        }
        try {
            if (!UserHandle.isSameApp(this.mContext.getPackageManager().getApplicationInfo(getCarrierAppPackageName(), 0).uid, Binder.getCallingUid())) {
                throw new SecurityException("Caller is not phone or carrier app!");
            }
        } catch (PackageManager.NameNotFoundException e) {
            throw new SecurityException("Caller is not phone or carrier app!");
        }
    }

    protected boolean isCdmaMo() {
        return this.mSmsDispatchersController.isCdmaMo();
    }

    protected String getPackageNameViaProcessId(String[] strArr) {
        if (strArr == null || strArr.length <= 0) {
            return null;
        }
        return strArr[0];
    }

    protected SmsMessageBase.SubmitPduBase onSendData(String str, String str2, int i, byte[] bArr, PendingIntent pendingIntent, PendingIntent pendingIntent2) {
        return getSubmitPdu(str2, str, i, bArr, pendingIntent2 != null);
    }

    protected SmsMessageBase.SubmitPduBase onSendText(String str, String str2, String str3, PendingIntent pendingIntent, PendingIntent pendingIntent2, Uri uri, String str4, boolean z, int i, boolean z2, int i2) {
        return getSubmitPdu(str2, str, str3, pendingIntent2 != null, null, i, i2);
    }

    protected int onSendMultipartText(String str, String str2, ArrayList<String> arrayList, ArrayList<PendingIntent> arrayList2, ArrayList<PendingIntent> arrayList3, Uri uri, String str3, boolean z, int i, boolean z2, int i2, GsmAlphabet.TextEncodingDetails[] textEncodingDetailsArr) {
        int size = arrayList.size();
        int i3 = 0;
        for (int i4 = 0; i4 < size; i4++) {
            GsmAlphabet.TextEncodingDetails textEncodingDetailsCalculateLength = calculateLength(arrayList.get(i4), false);
            if (i3 != textEncodingDetailsCalculateLength.codeUnitSize && (i3 == 0 || i3 == 1)) {
                i3 = textEncodingDetailsCalculateLength.codeUnitSize;
            }
            textEncodingDetailsArr[i4] = textEncodingDetailsCalculateLength;
        }
        return i3;
    }

    protected SmsMessageBase.SubmitPduBase onGetNewSubmitCdmaPduTracker(String str, String str2, String str3, SmsHeader smsHeader, int i, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z, AtomicInteger atomicInteger, AtomicBoolean atomicBoolean, Uri uri, String str4, int i2, boolean z2, int i3) {
        UserData userData = new UserData();
        userData.payloadStr = str3;
        userData.userDataHeader = smsHeader;
        if (i == 1) {
            userData.msgEncoding = 9;
        } else {
            userData.msgEncoding = 4;
        }
        userData.msgEncodingSet = true;
        return com.android.internal.telephony.cdma.SmsMessage.getSubmitPdu(str, userData, pendingIntent2 != null && z, i2);
    }
}
