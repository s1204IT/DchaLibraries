package com.mediatek.internal.telephony.cdma;

import android.app.PendingIntent;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Message;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SMSDispatcher;
import com.android.internal.telephony.SmsDispatchersController;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.SmsResponse;
import com.android.internal.telephony.cdma.CdmaSMSDispatcher;
import com.android.internal.telephony.cdma.SmsMessage;
import com.android.internal.telephony.uicc.IccUtils;
import com.mediatek.internal.telephony.MtkPhoneNumberUtils;
import com.mediatek.internal.telephony.util.MtkSMSDispatcherUtil;
import com.mediatek.internal.telephony.util.MtkSmsCommonUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MtkCdmaSMSDispatcher extends CdmaSMSDispatcher {
    private static final boolean ENG = "eng".equals(Build.TYPE);
    private static final int EVENT_COPY_TEXT_MESSAGE_DONE = 106;
    private static final String MTK_KEY_CDMA_SMS_7_BIT_ENCODING_TYPE_INT = "mtk_cdma_sms_7bit_encoding_type_int";
    private static final int RESULT_ERROR_RUIM_PLUG_OUT = 107;
    private static final int RESULT_ERROR_SUCCESS = 0;
    private static final String TAG = "MtkCdmaMoSms";
    private static final boolean VDBG = false;
    private static final int WAKE_LOCK_TIMEOUT = 500;
    private boolean mCopied;
    private ThreadLocal<Integer> mEncodingType;
    protected Object mLock;
    private ThreadLocal<Integer> mOriginalPort;
    protected boolean mSuccess;

    public MtkCdmaSMSDispatcher(Phone phone, SmsDispatchersController smsDispatchersController) {
        super(phone, smsDispatchersController);
        this.mLock = new Object();
        this.mCopied = false;
        this.mSuccess = true;
        this.mOriginalPort = new ThreadLocal<Integer>() {
            @Override
            protected Integer initialValue() {
                return -1;
            }
        };
        this.mEncodingType = new ThreadLocal<Integer>() {
            @Override
            protected Integer initialValue() {
                return 0;
            }
        };
        Rlog.d(TAG, "MtkCdmaSMSDispatcher created");
    }

    public void sendData(String str, String str2, int i, byte[] bArr, PendingIntent pendingIntent, PendingIntent pendingIntent2) {
        MtkPhoneNumberUtils.cdmaCheckAndProcessPlusCodeForSms(str);
        super.sendData(str, str2, i, bArr, pendingIntent, pendingIntent2);
    }

    public void sendText(String str, String str2, String str3, PendingIntent pendingIntent, PendingIntent pendingIntent2, Uri uri, String str4, boolean z, int i, boolean z2, int i2) {
        super.sendText(MtkPhoneNumberUtils.cdmaCheckAndProcessPlusCodeForSms(str), str2, str3, pendingIntent, pendingIntent2, uri, str4, z, i, z2, i2);
    }

    protected SmsMessageBase.SubmitPduBase onGetNewSubmitCdmaPduTracker(String str, String str2, String str3, SmsHeader smsHeader, int i, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z, AtomicInteger atomicInteger, AtomicBoolean atomicBoolean, Uri uri, String str4, int i2, boolean z2, int i3) {
        return MtkSmsMessage.getSubmitPdu(str2, str, str3, pendingIntent2 != null && z, smsHeader, i, i3, i2, 2 == get7bitEncodingType());
    }

    public void sendSms(SMSDispatcher.SmsTracker smsTracker) {
        int state = this.mPhone.getServiceState().getState();
        if (!isIms() && state != 0) {
            if (isSimAbsent()) {
                smsTracker.onFailed(this.mContext, 1, 0);
                return;
            } else {
                smsTracker.onFailed(this.mContext, getNotInServiceError(state), 0);
                return;
            }
        }
        super.sendSms(smsTracker);
    }

    protected SMSDispatcher.SmsTracker getSmsTracker(HashMap<String, Object> map, PendingIntent pendingIntent, PendingIntent pendingIntent2, String str, AtomicInteger atomicInteger, AtomicBoolean atomicBoolean, Uri uri, SmsHeader smsHeader, boolean z, String str2, boolean z2, boolean z3, int i, int i2) {
        SMSDispatcher.SmsTracker smsTracker = super.getSmsTracker(map, pendingIntent, pendingIntent2, str, atomicInteger, atomicBoolean, uri, smsHeader, z, str2, z2, z3, i, i2);
        MtkSmsCommonUtil.filterOutByPpl(this.mContext, smsTracker);
        return smsTracker;
    }

    protected String getPackageNameViaProcessId(String[] strArr) {
        return MtkSMSDispatcherUtil.getPackageNameViaProcessId(this.mContext, strArr);
    }

    public void sendData(String str, String str2, int i, int i2, byte[] bArr, PendingIntent pendingIntent, PendingIntent pendingIntent2) {
        this.mOriginalPort.set(Integer.valueOf(i2));
        sendData(str, str2, i, bArr, pendingIntent, pendingIntent2);
        this.mOriginalPort.remove();
    }

    protected SmsMessageBase.SubmitPduBase onSendData(String str, String str2, int i, byte[] bArr, PendingIntent pendingIntent, PendingIntent pendingIntent2) {
        if (this.mOriginalPort.get().intValue() == -1) {
            return super.onSendData(str, str2, i, bArr, pendingIntent, pendingIntent2);
        }
        return MtkSmsMessage.getSubmitPdu(str2, str, i, this.mOriginalPort.get().intValue(), bArr, pendingIntent2 != null);
    }

    public int copyTextMessageToIccCard(String str, String str2, List<String> list, int i, long j) {
        SmsMessage.SubmitPdu submitPduCreateEfPdu;
        this.mSuccess = true;
        int size = list.size();
        Rlog.d(TAG, "copyTextMessageToIccCard status = " + i + ", msgCount = " + size);
        if ((i != 1 && i != 3 && i != 5 && i != 7) || size < 1) {
            return 1;
        }
        for (int i2 = 0; i2 < size; i2++) {
            if (!this.mSuccess || (submitPduCreateEfPdu = MtkSmsMessage.createEfPdu(str2, list.get(i2), j)) == null) {
                return 1;
            }
            this.mCi.writeSmsToRuim(i, IccUtils.bytesToHexString(submitPduCreateEfPdu.encodedMessage), obtainMessage(EVENT_COPY_TEXT_MESSAGE_DONE));
            synchronized (this.mLock) {
                this.mCopied = false;
                while (!this.mCopied) {
                    try {
                        this.mLock.wait();
                    } catch (InterruptedException e) {
                        return 1;
                    }
                }
            }
        }
        return 1 ^ (this.mSuccess ? 1 : 0);
    }

    public void sendTextWithEncodingType(String str, String str2, String str3, int i, PendingIntent pendingIntent, PendingIntent pendingIntent2, Uri uri, String str4, boolean z, int i2, boolean z2, int i3) {
        Rlog.d(TAG, "sendTextWithEncodingType encoding = " + i);
        this.mEncodingType.set(Integer.valueOf(i));
        sendText(str, str2, str3, pendingIntent, pendingIntent2, uri, str4, z, i2, z2, i3);
        this.mEncodingType.remove();
    }

    protected SmsMessageBase.SubmitPduBase onSendText(String str, String str2, String str3, PendingIntent pendingIntent, PendingIntent pendingIntent2, Uri uri, String str4, boolean z, int i, boolean z2, int i2) {
        int iIntValue = this.mEncodingType.get().intValue();
        if (iIntValue == 1 || iIntValue == 2 || iIntValue == 3) {
            return MtkSmsMessage.getSubmitPdu(str2, str, str3, pendingIntent2 != null, (SmsHeader) null, iIntValue, i2, i, true);
        }
        return super.onSendText(str, str2, str3, pendingIntent, pendingIntent2, uri, str4, z, i, z2, i2);
    }

    public void sendMultipartTextWithEncodingType(String str, String str2, ArrayList<String> arrayList, int i, ArrayList<PendingIntent> arrayList2, ArrayList<PendingIntent> arrayList3, Uri uri, String str3, boolean z, int i2, boolean z2, int i3) {
        Rlog.d(TAG, "sendMultipartTextWithEncodingType encoding = " + i);
        this.mEncodingType.set(Integer.valueOf(i));
        sendMultipartText(str, str2, arrayList, arrayList2, arrayList3, uri, str3, z, i2, z2, i3);
        this.mEncodingType.remove();
    }

    protected int onSendMultipartText(String str, String str2, ArrayList<String> arrayList, ArrayList<PendingIntent> arrayList2, ArrayList<PendingIntent> arrayList3, Uri uri, String str3, boolean z, int i, boolean z2, int i2, GsmAlphabet.TextEncodingDetails[] textEncodingDetailsArr) {
        int iIntValue = this.mEncodingType.get().intValue();
        if (iIntValue == 1 || iIntValue == 2 || iIntValue == 3) {
            int size = arrayList.size();
            for (int i3 = 0; i3 < size; i3++) {
                GsmAlphabet.TextEncodingDetails textEncodingDetailsCalculateLength = MtkSmsMessage.calculateLength(arrayList.get(i3), false, iIntValue);
                textEncodingDetailsCalculateLength.codeUnitSize = iIntValue;
                textEncodingDetailsArr[i3] = textEncodingDetailsCalculateLength;
            }
            return iIntValue;
        }
        return super.onSendMultipartText(str, str2, arrayList, arrayList2, arrayList3, uri, str3, z, i, z2, i2, textEncodingDetailsArr);
    }

    public void handleMessage(Message message) {
        if (message.what == EVENT_COPY_TEXT_MESSAGE_DONE) {
            AsyncResult asyncResult = (AsyncResult) message.obj;
            synchronized (this.mLock) {
                this.mSuccess = asyncResult.exception == null;
                this.mCopied = true;
                this.mLock.notifyAll();
            }
            return;
        }
        super.handleMessage(message);
    }

    protected void handleSendComplete(AsyncResult asyncResult) {
        SMSDispatcher.SmsTracker smsTracker = (SMSDispatcher.SmsTracker) asyncResult.userObj;
        if (asyncResult.exception != null && asyncResult.result != null) {
            int i = ((SmsResponse) asyncResult.result).mErrorCode;
            if (i == RESULT_ERROR_RUIM_PLUG_OUT) {
                Rlog.d(TAG, "RUIM card is plug out");
                smsTracker.onFailed(this.mContext, 1, i);
                return;
            }
            int state = this.mPhone.getServiceState().getState();
            if (!isIms() && state != 0 && isSimAbsent()) {
                smsTracker.onFailed(this.mContext, 1, i);
                return;
            }
        }
        super.handleSendComplete(asyncResult);
    }

    private boolean isSimAbsent() {
        IccCardConstants.State state;
        IccCard iccCard = PhoneFactory.getPhone(this.mPhone.getPhoneId()).getIccCard();
        if (iccCard == null) {
            state = IccCardConstants.State.UNKNOWN;
        } else {
            state = iccCard.getState();
        }
        boolean z = state == IccCardConstants.State.ABSENT || state == IccCardConstants.State.NOT_READY;
        Rlog.d(TAG, "isSimAbsent state = " + state + " ret=" + z);
        return z;
    }

    private int get7bitEncodingType() {
        PersistableBundle configForSubId = ((CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config")).getConfigForSubId(this.mPhone.getSubId());
        int i = configForSubId != null ? configForSubId.getInt(MTK_KEY_CDMA_SMS_7_BIT_ENCODING_TYPE_INT, 9) : 9;
        Rlog.d(TAG, "get7bitEncodingType = " + i);
        return i;
    }
}
