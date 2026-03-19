package com.mediatek.internal.telephony.gsm;

import android.app.PendingIntent;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.SMSDispatcher;
import com.android.internal.telephony.SmsDispatchersController;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.SmsRawData;
import com.android.internal.telephony.gsm.GsmInboundSmsHandler;
import com.android.internal.telephony.gsm.GsmSMSDispatcher;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.android.internal.telephony.gsm.SmsMessage;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.util.SMSDispatcherUtil;
import com.mediatek.internal.telephony.MtkSmsDispatchersController;
import com.mediatek.internal.telephony.MtkSmsHeader;
import com.mediatek.internal.telephony.gsm.MtkSmsMessage;
import com.mediatek.internal.telephony.ppl.IPplSmsFilter;
import com.mediatek.internal.telephony.ppl.PplSmsFilterExtension;
import com.mediatek.internal.telephony.util.MtkSMSDispatcherUtil;
import com.mediatek.internal.telephony.util.MtkSmsCommonUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MtkGsmSMSDispatcher extends GsmSMSDispatcher {
    protected static final int EVENT_ADD_DELIVER_PENDING_LIST = 107;
    protected static final int EVENT_COPY_TEXT_MESSAGE_DONE = 106;
    private static final String TAG = "MtkGsmSMSDispatcher";
    private ThreadLocal<Integer> mEncodingType;
    protected Object mLock;
    private PplSmsFilterExtension mPplSmsFilter;
    private boolean mStorageAvailable;
    private boolean mSuccess;
    protected int messageCountNeedCopy;
    private static final boolean ENG = "eng".equals(Build.TYPE);
    protected static String PDU_SIZE = "pdu_size";
    protected static String MSG_REF_NUM = "msg_ref_num";

    public MtkGsmSMSDispatcher(Phone phone, SmsDispatchersController smsDispatchersController, GsmInboundSmsHandler gsmInboundSmsHandler) {
        super(phone, smsDispatchersController, gsmInboundSmsHandler);
        this.mStorageAvailable = true;
        this.mSuccess = true;
        this.messageCountNeedCopy = 0;
        this.mLock = new Object();
        this.mPplSmsFilter = null;
        this.mEncodingType = new ThreadLocal<Integer>() {
            @Override
            protected Integer initialValue() {
                return 0;
            }
        };
        this.mUiccController.unregisterForIccChanged(this);
        this.mUiccController.registerForIccChanged(this, 15, new Integer(this.mPhone.getPhoneId()));
        Rlog.d(TAG, "MtkGsmSMSDispatcher created");
    }

    public void dispose() {
        super.dispose();
        this.mCi.unSetOnSmsStatus(this);
        this.mUiccController.unregisterForIccChanged(this);
    }

    public String getFormat() {
        return "3gpp";
    }

    public void handleMessage(Message message) {
        int i = message.what;
        if (i == 15) {
            Integer uiccControllerPhoneId = getUiccControllerPhoneId(message);
            if (uiccControllerPhoneId.intValue() != this.mPhone.getPhoneId()) {
                Rlog.d(TAG, "Wrong phone id event coming, PhoneId: " + uiccControllerPhoneId);
                return;
            }
            Rlog.d(TAG, "EVENT_ICC_CHANGED, PhoneId: " + uiccControllerPhoneId + " match exactly.");
            onUpdateIccAvailability();
            return;
        }
        switch (i) {
            case EVENT_COPY_TEXT_MESSAGE_DONE:
                AsyncResult asyncResult = (AsyncResult) message.obj;
                synchronized (this.mLock) {
                    this.mSuccess = asyncResult.exception == null;
                    if (this.mSuccess) {
                        Rlog.d(TAG, "[copyText success to copy one");
                        this.messageCountNeedCopy--;
                    } else {
                        Rlog.d(TAG, "[copyText fail to copy one");
                        this.messageCountNeedCopy = 0;
                    }
                    this.mLock.notifyAll();
                    break;
                }
                return;
            case EVENT_ADD_DELIVER_PENDING_LIST:
                SMSDispatcher.SmsTracker smsTracker = (SMSDispatcher.SmsTracker) message.obj;
                Rlog.d(TAG, "EVENT_ADD_DELIVER_PENDING_LIST mMessageRef=" + smsTracker.mMessageRef);
                this.deliveryPendingList.add(smsTracker);
                return;
            default:
                super.handleMessage(message);
                return;
        }
    }

    protected SMSDispatcher.SmsTracker getNewSubmitPduTracker(String str, String str2, String str3, SmsHeader smsHeader, int i, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z, AtomicInteger atomicInteger, AtomicBoolean atomicBoolean, Uri uri, String str4, int i2, boolean z2, int i3) {
        if (ENG) {
            Rlog.d(TAG, "getNewSubmitPduTracker w/ validity");
        }
        SmsMessage.SubmitPdu submitPdu = MtkSmsMessage.getSubmitPdu(str2, str, str3, pendingIntent2 != null, MtkSmsHeader.toByteArray(smsHeader), i, smsHeader.languageTable, smsHeader.languageShiftTable, i3);
        if (submitPdu != null) {
            return getSmsTracker(getSmsTrackerMap(str, str2, str3, submitPdu), pendingIntent, pendingIntent2, getFormat(), atomicInteger, atomicBoolean, uri, smsHeader, !z || z2, str4, true, true, i2, i3);
        }
        Rlog.e(TAG, "GsmSMSDispatcher.getNewSubmitPduTracker(): getSubmitPdu() returned null");
        return null;
    }

    private Integer getUiccControllerPhoneId(Message message) {
        Integer num = new Integer(-1);
        AsyncResult asyncResult = (AsyncResult) message.obj;
        if (asyncResult != null && (asyncResult.result instanceof Integer)) {
            return (Integer) asyncResult.result;
        }
        return num;
    }

    public void sendData(String str, String str2, int i, int i2, byte[] bArr, PendingIntent pendingIntent, PendingIntent pendingIntent2) {
        Rlog.d(TAG, "MtkGsmSmsDispatcher.sendData: enter");
        SmsMessage.SubmitPdu submitPdu = MtkSmsMessage.getSubmitPdu(str2, str, i, i2, bArr, pendingIntent2 != null);
        if (submitPdu != null) {
            SMSDispatcher.SmsTracker smsTracker = getSmsTracker(getSmsTrackerMap(str, str2, i, bArr, submitPdu), pendingIntent, pendingIntent2, getFormat(), null, false, null, false, true);
            if (!sendSmsByCarrierApp(true, smsTracker)) {
                sendSubmitPdu(smsTracker);
                return;
            }
            return;
        }
        Rlog.e(TAG, "GsmSMSDispatcher.sendData(): getSubmitPdu() returned null");
    }

    public void sendMultipartData(String str, String str2, int i, ArrayList<SmsRawData> arrayList, ArrayList<PendingIntent> arrayList2, ArrayList<PendingIntent> arrayList3) {
        PendingIntent pendingIntent;
        ArrayList<PendingIntent> arrayList4 = arrayList2;
        if (arrayList == null) {
            Rlog.e(TAG, "Cannot send multipart data when data is null!");
            return;
        }
        int nextConcatenatedRef = getNextConcatenatedRef() & 255;
        int size = arrayList.size();
        SMSDispatcher.SmsTracker[] smsTrackerArr = new SMSDispatcher.SmsTracker[size];
        int i2 = 0;
        while (true) {
            boolean z = true;
            if (i2 >= size) {
                break;
            }
            int i3 = i2 + 1;
            byte[] submitPduHeader = MtkSmsHeader.getSubmitPduHeader(i, nextConcatenatedRef, i3, size);
            PendingIntent pendingIntent2 = null;
            if (arrayList4 == null || arrayList2.size() <= i2) {
                pendingIntent = null;
            } else {
                pendingIntent = arrayList4.get(i2);
            }
            if (arrayList3 != null && arrayList3.size() > i2) {
                pendingIntent2 = arrayList3.get(i2);
            }
            PendingIntent pendingIntent3 = pendingIntent2;
            byte[] bytes = arrayList.get(i2).getBytes();
            if (pendingIntent3 == null) {
                z = false;
            }
            SMSDispatcher.SmsTracker[] smsTrackerArr2 = smsTrackerArr;
            smsTrackerArr2[i2] = getSmsTracker(getSmsTrackerMap(str, str2, i, arrayList.get(i2).getBytes(), MtkSmsMessage.getSubmitPdu(str2, str, bytes, submitPduHeader, z)), pendingIntent, pendingIntent3, getFormat(), null, false, null, false, true);
            smsTrackerArr = smsTrackerArr2;
            i2 = i3;
            arrayList4 = arrayList2;
        }
        SMSDispatcher.SmsTracker[] smsTrackerArr3 = smsTrackerArr;
        if (smsTrackerArr3.length == 0 || smsTrackerArr3[0] == null) {
            Rlog.e(TAG, "Cannot send multipart data. parts=" + arrayList + " trackers length = " + smsTrackerArr3.length);
            return;
        }
        for (SMSDispatcher.SmsTracker smsTracker : smsTrackerArr3) {
            if (smsTracker != null) {
                if (!sendSmsByCarrierApp(true, smsTracker)) {
                    sendSubmitPdu(smsTracker);
                }
            } else {
                Rlog.e(TAG, "Null tracker.");
            }
        }
    }

    public int copyTextMessageToIccCard(String str, String str2, List<String> list, int i, long j) {
        String str3;
        boolean z;
        int i2;
        int i3;
        int i4;
        int i5;
        int i6;
        int i7;
        GsmAlphabet.TextEncodingDetails[] textEncodingDetailsArr;
        byte[] submitPduHeaderWithLang;
        int i8;
        int i9;
        Rlog.d(TAG, "GsmSMSDispatcher: copy text message to icc card");
        if (checkPhoneNumber(str)) {
            str3 = str;
        } else {
            Rlog.d(TAG, "[copyText invalid sc address");
            str3 = null;
        }
        if (!checkPhoneNumber(str2)) {
            Rlog.d(TAG, "[copyText invalid dest address");
            return 8;
        }
        int i10 = 1;
        this.mSuccess = true;
        int size = list.size();
        Rlog.d(TAG, "[copyText storage available");
        int i11 = 0;
        if (i == 1 || i == 3) {
            Rlog.d(TAG, "[copyText to encode deliver pdu");
            z = true;
        } else if (i == 5 || i == 7) {
            Rlog.d(TAG, "[copyText to encode submit pdu");
            z = false;
        } else {
            Rlog.d(TAG, "[copyText invalid status, default is deliver pdu");
            return 1;
        }
        Rlog.d(TAG, "[copyText msgCount " + size);
        if (size > 1) {
            Rlog.d(TAG, "[copyText multi-part message");
        } else {
            if (size != 1) {
                Rlog.d(TAG, "[copyText invalid message count");
                return 1;
            }
            Rlog.d(TAG, "[copyText single-part message");
        }
        int nextConcatenatedRef = getNextConcatenatedRef() & 255;
        GsmAlphabet.TextEncodingDetails[] textEncodingDetailsArr2 = new GsmAlphabet.TextEncodingDetails[size];
        int i12 = 0;
        for (int i13 = 0; i13 < size; i13++) {
            textEncodingDetailsArr2[i13] = MtkSmsMessage.calculateLength(list.get(i13), false);
            if (i12 != textEncodingDetailsArr2[i13].codeUnitSize && (i12 == 0 || i12 == 1)) {
                i12 = textEncodingDetailsArr2[i13].codeUnitSize;
            }
        }
        int i14 = 0;
        while (i14 < size) {
            if (!this.mSuccess) {
                Rlog.d(TAG, "[copyText Exception happened when copy message");
                return i10;
            }
            int i15 = textEncodingDetailsArr2[i14].shiftLangId;
            if (i12 == i10) {
                Rlog.d(TAG, "Detail: " + i14 + " ted" + textEncodingDetailsArr2[i14]);
                if (!textEncodingDetailsArr2[i14].useLockingShift || !textEncodingDetailsArr2[i14].useSingleShift) {
                    if (!textEncodingDetailsArr2[i14].useLockingShift) {
                        if (textEncodingDetailsArr2[i14].useSingleShift) {
                            i2 = 11;
                            i4 = i15;
                            i3 = -1;
                        }
                        i2 = i12;
                        i3 = -1;
                        i4 = -1;
                    } else {
                        i2 = 12;
                        i3 = i15;
                        i4 = -1;
                    }
                } else {
                    i2 = 13;
                    i3 = i15;
                    i4 = i3;
                }
            } else {
                i2 = i12;
                i3 = -1;
                i4 = -1;
            }
            if (size > i10) {
                Rlog.d(TAG, "[copyText get pdu header for multi-part message");
                i5 = i15;
                i6 = i14;
                i7 = i12;
                int i16 = i4;
                textEncodingDetailsArr = textEncodingDetailsArr2;
                submitPduHeaderWithLang = MtkSmsHeader.getSubmitPduHeaderWithLang(-1, nextConcatenatedRef, i14 + 1, size, i16, i3);
            } else {
                i5 = i15;
                i6 = i14;
                i7 = i12;
                textEncodingDetailsArr = textEncodingDetailsArr2;
                submitPduHeaderWithLang = null;
            }
            if (z) {
                i8 = nextConcatenatedRef;
                i9 = i11;
                MtkSmsMessage.DeliverPdu deliverPduWithLang = MtkSmsMessage.getDeliverPduWithLang(str3, str2, list.get(i6), submitPduHeaderWithLang, j, i7, i5);
                if (deliverPduWithLang != null) {
                    Rlog.d(TAG, "[copyText write deliver pdu into SIM");
                    this.mCi.writeSmsToSim(i, IccUtils.bytesToHexString(deliverPduWithLang.encodedScAddress), IccUtils.bytesToHexString(deliverPduWithLang.encodedMessage), obtainMessage(EVENT_COPY_TEXT_MESSAGE_DONE));
                    synchronized (this.mLock) {
                        try {
                            try {
                                Rlog.d(TAG, "[copyText wait until the message be wrote in SIM");
                                this.mLock.wait();
                            } catch (InterruptedException e) {
                                Rlog.d(TAG, "Fail to copy text message into SIM");
                                return 1;
                            }
                        } finally {
                        }
                    }
                } else {
                    continue;
                }
            } else {
                i8 = nextConcatenatedRef;
                i9 = i11;
                SmsMessage.SubmitPdu submitPduWithLang = MtkSmsMessage.getSubmitPduWithLang(str3, str2, list.get(i6), false, submitPduHeaderWithLang, i2, i5, -1);
                if (submitPduWithLang != null) {
                    Rlog.d(TAG, "[copyText write submit pdu into SIM");
                    this.mCi.writeSmsToSim(i, IccUtils.bytesToHexString(submitPduWithLang.encodedScAddress), IccUtils.bytesToHexString(submitPduWithLang.encodedMessage), obtainMessage(EVENT_COPY_TEXT_MESSAGE_DONE));
                    synchronized (this.mLock) {
                        try {
                            try {
                                Rlog.d(TAG, "[copyText wait until the message be wrote in SIM");
                                this.mLock.wait();
                            } catch (InterruptedException e2) {
                                Rlog.d(TAG, "fail to copy text message into SIM");
                                return 1;
                            }
                        } finally {
                        }
                    }
                } else {
                    continue;
                }
            }
            Rlog.d(TAG, "[copyText thread is waked up");
            i14 = i6 + 1;
            textEncodingDetailsArr2 = textEncodingDetailsArr;
            i12 = i7;
            nextConcatenatedRef = i8;
            i11 = i9;
            i10 = 1;
        }
        int i17 = i11;
        if (this.mSuccess) {
            Rlog.d(TAG, "[copyText all messages have been copied into SIM");
            return i17;
        }
        Rlog.d(TAG, "[copyText copy failed");
        return 1;
    }

    private boolean isValidSmsAddress(String str) {
        String strExtractNetworkPortion = PhoneNumberUtils.extractNetworkPortion(str);
        return strExtractNetworkPortion == null || strExtractNetworkPortion.length() == str.length();
    }

    private boolean checkPhoneNumber(char c) {
        return (c >= '0' && c <= '9') || c == '*' || c == '+' || c == '#' || c == 'N' || c == ' ' || c == '-';
    }

    private boolean checkPhoneNumber(String str) {
        if (str == null) {
            return true;
        }
        Rlog.d(TAG, "checkPhoneNumber: " + str);
        int length = str.length();
        for (int i = 0; i < length; i++) {
            if (!checkPhoneNumber(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public void sendTextWithEncodingType(String str, String str2, String str3, int i, PendingIntent pendingIntent, PendingIntent pendingIntent2, Uri uri, String str4, boolean z, int i2, boolean z2, int i3) {
        Rlog.d(TAG, "sendTextWithEncodingType encoding = " + i);
        this.mEncodingType.set(Integer.valueOf(i));
        sendText(str, str2, str3, pendingIntent, pendingIntent2, uri, str4, z, i2, z2, i3);
        this.mEncodingType.remove();
    }

    protected SmsMessageBase.SubmitPduBase onSendText(String str, String str2, String str3, PendingIntent pendingIntent, PendingIntent pendingIntent2, Uri uri, String str4, boolean z, int i, boolean z2, int i2) {
        int iIntValue = this.mEncodingType.get().intValue();
        if (iIntValue == 0) {
            return super.onSendText(str, str2, str3, pendingIntent, pendingIntent2, uri, str4, z, i, z2, i2);
        }
        GsmAlphabet.TextEncodingDetails textEncodingDetailsCalculateLength = SMSDispatcherUtil.calculateLength(false, str3, false);
        return SmsMessage.getSubmitPdu(str2, str, str3, pendingIntent2 != null, (byte[]) null, iIntValue, textEncodingDetailsCalculateLength.languageTable, textEncodingDetailsCalculateLength.languageShiftTable, i2);
    }

    public void sendMultipartTextWithEncodingType(String str, String str2, ArrayList<String> arrayList, int i, ArrayList<PendingIntent> arrayList2, ArrayList<PendingIntent> arrayList3, Uri uri, String str3, boolean z, int i2, boolean z2, int i3) {
        Rlog.d(TAG, "sendMultipartTextWithEncodingType encoding = " + i);
        this.mEncodingType.set(Integer.valueOf(i));
        sendMultipartText(str, str2, arrayList, arrayList2, arrayList3, uri, str3, z, i2, z2, i3);
        this.mEncodingType.remove();
    }

    protected int onSendMultipartText(String str, String str2, ArrayList<String> arrayList, ArrayList<PendingIntent> arrayList2, ArrayList<PendingIntent> arrayList3, Uri uri, String str3, boolean z, int i, boolean z2, int i2, GsmAlphabet.TextEncodingDetails[] textEncodingDetailsArr) {
        int iIntValue = this.mEncodingType.get().intValue();
        if (iIntValue == 0) {
            int iOnSendMultipartText = super.onSendMultipartText(str, str2, arrayList, arrayList2, arrayList3, uri, str3, z, i, z2, i2, textEncodingDetailsArr);
            Rlog.d(TAG, "onSendMultipartText encoding = " + iOnSendMultipartText);
            return iOnSendMultipartText;
        }
        int size = arrayList.size();
        for (int i3 = 0; i3 < size; i3++) {
            GsmAlphabet.TextEncodingDetails textEncodingDetailsCalculateLength = SMSDispatcherUtil.calculateLength(false, arrayList.get(i3), false);
            if (iIntValue != textEncodingDetailsCalculateLength.codeUnitSize && (iIntValue == 0 || iIntValue == 1)) {
                Rlog.d(TAG, "[enc conflict between details[" + textEncodingDetailsCalculateLength.codeUnitSize + "] and encoding " + iIntValue);
                textEncodingDetailsCalculateLength.codeUnitSize = iIntValue;
            }
            textEncodingDetailsArr[i3] = textEncodingDetailsCalculateLength;
        }
        return iIntValue;
    }

    public void handleIccFull() {
        this.mGsmInboundSmsHandler.mStorageMonitor.handleIccFull();
    }

    public void handleQueryCbActivation(AsyncResult asyncResult) {
        Boolean bool;
        if (asyncResult.exception == null) {
            ArrayList arrayList = (ArrayList) asyncResult.result;
            if (arrayList.size() == 0) {
                bool = new Boolean(false);
            } else {
                SmsBroadcastConfigInfo smsBroadcastConfigInfo = (SmsBroadcastConfigInfo) arrayList.get(0);
                Rlog.d(TAG, "cbConfig: " + smsBroadcastConfigInfo.toString());
                if (smsBroadcastConfigInfo.getFromCodeScheme() == -1 && smsBroadcastConfigInfo.getToCodeScheme() == -1 && smsBroadcastConfigInfo.getFromServiceId() == -1 && smsBroadcastConfigInfo.getToServiceId() == -1 && !smsBroadcastConfigInfo.isSelected()) {
                    bool = new Boolean(false);
                } else {
                    bool = new Boolean(true);
                }
            }
        } else {
            bool = null;
        }
        Rlog.d(TAG, "queryCbActivation: " + bool);
        AsyncResult.forMessage((Message) asyncResult.userObj, bool, asyncResult.exception);
        ((Message) asyncResult.userObj).sendToTarget();
    }

    public void setSmsMemoryStatus(boolean z) {
        if (z != this.mStorageAvailable) {
            this.mStorageAvailable = z;
            this.mCi.reportSmsMemoryStatus(z, (Message) null);
        }
    }

    public boolean isSmsReady() {
        return ((MtkSmsDispatchersController) this.mSmsDispatchersController).isSmsReady();
    }

    protected String getPackageNameViaProcessId(String[] strArr) {
        return MtkSMSDispatcherUtil.getPackageNameViaProcessId(this.mContext, strArr);
    }

    protected void sendMultipartSms(SMSDispatcher.SmsTracker smsTracker) {
        HashMap data = smsTracker.getData();
        ArrayList arrayList = (ArrayList) data.get("parts");
        ArrayList arrayList2 = (ArrayList) data.get("sentIntents");
        int state = this.mPhone.getServiceState().getState();
        if (!isIms() && state != 0 && !this.mTelephonyManager.isWifiCallingAvailable()) {
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                PendingIntent pendingIntent = null;
                if (arrayList2 != null && arrayList2.size() > i) {
                    pendingIntent = (PendingIntent) arrayList2.get(i);
                }
                handleNotInService(state, pendingIntent);
            }
            return;
        }
        super.sendMultipartSms(smsTracker);
    }

    private void FilterOutByPpl(Context context, SMSDispatcher.SmsTracker smsTracker) {
        if (this.mPplSmsFilter == null) {
            this.mPplSmsFilter = new PplSmsFilterExtension(context);
        }
        if (!MtkSmsCommonUtil.isPrivacyLockSupport()) {
            return;
        }
        if (ENG) {
            Rlog.d(TAG, "[PPL] Phone privacy check start");
        }
        Bundle bundle = new Bundle();
        PplSmsFilterExtension pplSmsFilterExtension = this.mPplSmsFilter;
        bundle.putString(IPplSmsFilter.KEY_MSG_CONTENT, smsTracker.mFullMessageText);
        PplSmsFilterExtension pplSmsFilterExtension2 = this.mPplSmsFilter;
        bundle.putString(IPplSmsFilter.KEY_DST_ADDR, smsTracker.mDestAddress);
        PplSmsFilterExtension pplSmsFilterExtension3 = this.mPplSmsFilter;
        bundle.putString(IPplSmsFilter.KEY_FORMAT, smsTracker.mFormat);
        PplSmsFilterExtension pplSmsFilterExtension4 = this.mPplSmsFilter;
        bundle.putInt(IPplSmsFilter.KEY_SUB_ID, smsTracker.mSubId);
        PplSmsFilterExtension pplSmsFilterExtension5 = this.mPplSmsFilter;
        bundle.putInt(IPplSmsFilter.KEY_SMS_TYPE, 1);
        boolean zPplFilter = this.mPplSmsFilter.pplFilter(bundle);
        if (zPplFilter) {
            smsTracker.mPersistMessage = false;
        }
        if (ENG) {
            Rlog.d(TAG, "[PPL] Phone privacy check end, Need to filter(result) = " + zPplFilter);
        }
    }

    protected SMSDispatcher.SmsTracker getSmsTracker(HashMap<String, Object> map, PendingIntent pendingIntent, PendingIntent pendingIntent2, String str, AtomicInteger atomicInteger, AtomicBoolean atomicBoolean, Uri uri, SmsHeader smsHeader, boolean z, String str2, boolean z2, boolean z3) {
        SMSDispatcher.SmsTracker smsTracker = super.getSmsTracker(map, pendingIntent, pendingIntent2, str, atomicInteger, atomicBoolean, uri, smsHeader, z, str2, z2, z3, -1, -1);
        FilterOutByPpl(this.mContext, smsTracker);
        return smsTracker;
    }

    public void addToGsmDeliverPendingList(SMSDispatcher.SmsTracker smsTracker) {
        if (smsTracker.mDeliveryIntent != null) {
            Rlog.d(TAG, "addToGsmDeliverPendingList sendMessage");
            sendMessage(obtainMessage(EVENT_ADD_DELIVER_PENDING_LIST, smsTracker));
        }
    }
}
