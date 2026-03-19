package com.mediatek.internal.telephony;

import android.app.PendingIntent;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.telephony.Rlog;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.ImsSmsDispatcher;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.SMSDispatcher;
import com.android.internal.telephony.SmsDispatchersController;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.SmsRawData;
import com.android.internal.telephony.util.SMSDispatcherUtil;
import com.mediatek.internal.telephony.ppl.PplSmsFilterExtension;
import com.mediatek.internal.telephony.util.MtkSMSDispatcherUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MtkImsSmsDispatcher extends ImsSmsDispatcher {
    private static final boolean ENG = "eng".equals(Build.TYPE);
    private static final String TAG = "MtkImsSmsDispacher";
    private ThreadLocal<Integer> mEncodingType;
    private PplSmsFilterExtension mPplSmsFilter;

    protected SmsMessageBase.SubmitPduBase getSubmitPdu(String str, String str2, int i, int i2, byte[] bArr, boolean z) {
        return MtkSMSDispatcherUtil.getSubmitPdu(isCdmaMo(), str, str2, i, i2, bArr, z);
    }

    protected SmsMessageBase.SubmitPduBase getSubmitPdu(String str, String str2, byte[] bArr, byte[] bArr2, boolean z) {
        return MtkSMSDispatcherUtil.getSubmitPdu(isCdmaMo(), str, str2, bArr, bArr2, z);
    }

    protected SmsMessageBase.SubmitPduBase getSubmitPdu(boolean z, String str, String str2, String str3, boolean z2, byte[] bArr, int i, int i2, int i3, int i4) {
        return MtkSMSDispatcherUtil.getSubmitPdu(isCdmaMo(), str, str2, str3, z2, bArr, i, i2, i3, i4);
    }

    public MtkImsSmsDispatcher(Phone phone, SmsDispatchersController smsDispatchersController) {
        super(phone, smsDispatchersController);
        this.mPplSmsFilter = null;
        this.mEncodingType = new ThreadLocal<Integer>() {
            @Override
            protected Integer initialValue() {
                return 0;
            }
        };
        Rlog.d(TAG, "Created!");
    }

    protected void sendData(String str, String str2, int i, int i2, byte[] bArr, PendingIntent pendingIntent, PendingIntent pendingIntent2) {
        if (!isCdmaMo()) {
            sendDataGsm(str, str2, i, i2, bArr, pendingIntent, pendingIntent2);
        } else {
            super.sendData(str, str2, i, bArr, pendingIntent, pendingIntent2);
        }
    }

    public void sendMultipartData(String str, String str2, int i, ArrayList<SmsRawData> arrayList, ArrayList<PendingIntent> arrayList2, ArrayList<PendingIntent> arrayList3) {
        if (!isCdmaMo()) {
            sendMultipartDataGsm(str, str2, i, arrayList, arrayList2, arrayList3);
        } else {
            Rlog.d(TAG, "Don't support sendMultipartData for CDMA");
        }
    }

    public void sendTextWithEncodingType(String str, String str2, String str3, int i, PendingIntent pendingIntent, PendingIntent pendingIntent2, Uri uri, String str4, boolean z, int i2, boolean z2, int i3) {
        if (!isCdmaMo()) {
            sendTextWithEncodingTypeGsm(str, str2, str3, i, pendingIntent, pendingIntent2, uri, str4, z, i2, z2, i3);
        } else {
            sendText(str, str2, str3, pendingIntent, pendingIntent2, uri, str4, z, i2, z2, i3);
        }
    }

    public void sendMultipartTextWithEncodingType(String str, String str2, ArrayList<String> arrayList, int i, ArrayList<PendingIntent> arrayList2, ArrayList<PendingIntent> arrayList3, Uri uri, String str3, boolean z, int i2, boolean z2, int i3) {
        if (!isCdmaMo()) {
            sendMultipartTextWithEncodingTypeGsm(str, str2, arrayList, i, arrayList2, arrayList3, uri, str3, z, i2, z2, i3);
        } else {
            sendMultipartText(str, str2, arrayList, arrayList2, arrayList3, uri, str3, z, i2, z2, i3);
        }
    }

    protected SMSDispatcher.SmsTracker getNewSubmitPduTracker(String str, String str2, String str3, SmsHeader smsHeader, int i, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z, AtomicInteger atomicInteger, AtomicBoolean atomicBoolean, Uri uri, String str4, int i2, boolean z2, int i3) {
        if (ENG) {
            Rlog.d(TAG, "getNewSubmitPduTracker w/ validity");
        }
        if (isCdmaMo()) {
            return super.getNewSubmitPduTracker(str, str2, str3, smsHeader, i, pendingIntent, pendingIntent2, z, atomicInteger, atomicBoolean, uri, str4, i2, z2, i3);
        }
        return getNewSubmitPduTrackerGsm(str, str2, str3, smsHeader, i, pendingIntent, pendingIntent2, z, atomicInteger, atomicBoolean, uri, str4, i2, z2, i3);
    }

    protected void sendDataGsm(String str, String str2, int i, int i2, byte[] bArr, PendingIntent pendingIntent, PendingIntent pendingIntent2) {
        Rlog.d(TAG, "sendData: enter");
        SmsMessageBase.SubmitPduBase submitPdu = getSubmitPdu(str2, str, i, i2, bArr, pendingIntent2 != null);
        if (submitPdu != null) {
            SMSDispatcher.SmsTracker smsTracker = getSmsTracker(getSmsTrackerMap(str, str2, i, bArr, submitPdu), pendingIntent, pendingIntent2, getFormat(), null, false, null, false, true);
            if (!sendSmsByCarrierApp(true, smsTracker)) {
                sendSubmitPdu(smsTracker);
                return;
            }
            return;
        }
        Rlog.e(TAG, "sendData(): getSubmitPdu() returned null");
    }

    public void sendMultipartDataGsm(String str, String str2, int i, ArrayList<SmsRawData> arrayList, ArrayList<PendingIntent> arrayList2, ArrayList<PendingIntent> arrayList3) {
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
        while (i2 < size) {
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
            SMSDispatcher.SmsTracker[] smsTrackerArr2 = smsTrackerArr;
            smsTrackerArr2[i2] = getSmsTracker(getSmsTrackerMap(str, str2, i, arrayList.get(i2).getBytes(), getSubmitPdu(str2, str, arrayList.get(i2).getBytes(), submitPduHeader, pendingIntent3 != null)), pendingIntent, pendingIntent3, getFormat(), null, false, null, false, true);
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

    public void sendTextWithEncodingTypeGsm(String str, String str2, String str3, int i, PendingIntent pendingIntent, PendingIntent pendingIntent2, Uri uri, String str4, boolean z, int i2, boolean z2, int i3) {
        Rlog.d(TAG, "sendTextWithEncodingTypeGsm encoding = " + i);
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
        return getSubmitPdu(false, str2, str, str3, pendingIntent2 != null, null, iIntValue, textEncodingDetailsCalculateLength.languageTable, textEncodingDetailsCalculateLength.languageShiftTable, i2);
    }

    public void sendMultipartTextWithEncodingTypeGsm(String str, String str2, ArrayList<String> arrayList, int i, ArrayList<PendingIntent> arrayList2, ArrayList<PendingIntent> arrayList3, Uri uri, String str3, boolean z, int i2, boolean z2, int i3) {
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

    protected SMSDispatcher.SmsTracker getSmsTracker(HashMap<String, Object> map, PendingIntent pendingIntent, PendingIntent pendingIntent2, String str, Uri uri, boolean z, String str2, boolean z2, boolean z3) {
        return getSmsTracker(map, pendingIntent, pendingIntent2, str, null, null, uri, null, z, str2, z2, z3, -1, -1);
    }

    protected SMSDispatcher.SmsTracker getSmsTracker(HashMap<String, Object> map, PendingIntent pendingIntent, PendingIntent pendingIntent2, String str, AtomicInteger atomicInteger, AtomicBoolean atomicBoolean, Uri uri, SmsHeader smsHeader, boolean z, String str2, boolean z2, boolean z3, int i, int i2) {
        SMSDispatcher.SmsTracker smsTracker = super.getSmsTracker(map, pendingIntent, pendingIntent2, str, atomicInteger, atomicBoolean, uri, smsHeader, z, str2, z2, z3, i, i2);
        FilterOutByPpl(this.mContext, smsTracker);
        return smsTracker;
    }

    private void FilterOutByPpl(Context context, SMSDispatcher.SmsTracker smsTracker) {
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

    private SMSDispatcher.SmsTracker getNewSubmitPduTrackerGsm(String str, String str2, String str3, SmsHeader smsHeader, int i, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z, AtomicInteger atomicInteger, AtomicBoolean atomicBoolean, Uri uri, String str4, int i2, boolean z2, int i3) {
        SmsMessageBase.SubmitPduBase submitPdu = getSubmitPdu(false, str2, str, str3, pendingIntent2 != null, MtkSmsHeader.toByteArray(smsHeader), i, smsHeader.languageTable, smsHeader.languageShiftTable, i3);
        if (submitPdu != null) {
            return getSmsTracker(getSmsTrackerMap(str, str2, str3, submitPdu), pendingIntent, pendingIntent2, getFormat(), atomicInteger, atomicBoolean, uri, smsHeader, !z || z2, str4, true, true, i2, i3);
        }
        Rlog.e(TAG, "getNewSubmitPduTrackerGsm: getSubmitPdu() returned null");
        return null;
    }

    protected String getPackageNameViaProcessId(String[] strArr) {
        return MtkSMSDispatcherUtil.getPackageNameViaProcessId(this.mContext, strArr);
    }
}
