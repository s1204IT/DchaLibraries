package com.mediatek.internal.telephony.util;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Binder;
import android.os.Build;
import android.telephony.Rlog;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.cdma.SmsMessage;
import com.mediatek.internal.telephony.gsm.MtkSmsMessage;
import java.util.List;

public final class MtkSMSDispatcherUtil {
    private static final boolean ENG = "eng".equals(Build.TYPE);
    static final String TAG = "MtkSMSDispatcherUtil";

    private MtkSMSDispatcherUtil() {
    }

    public static SmsMessageBase.SubmitPduBase getSubmitPdu(boolean z, String str, String str2, String str3, boolean z2, SmsHeader smsHeader) {
        if (z) {
            return getSubmitPduCdma(str, str2, str3, z2, smsHeader);
        }
        return getSubmitPduGsm(str, str2, str3, z2);
    }

    public static SmsMessageBase.SubmitPduBase getSubmitPdu(boolean z, String str, String str2, String str3, boolean z2, SmsHeader smsHeader, int i, int i2) {
        if (z) {
            return getSubmitPduCdma(str, str2, str3, z2, smsHeader, i);
        }
        return getSubmitPduGsm(str, str2, str3, z2, i2);
    }

    public static SmsMessageBase.SubmitPduBase getSubmitPduGsm(String str, String str2, String str3, boolean z) {
        return MtkSmsMessage.getSubmitPdu(str, str2, str3, z);
    }

    public static SmsMessageBase.SubmitPduBase getSubmitPduGsm(String str, String str2, String str3, boolean z, int i) {
        return MtkSmsMessage.getSubmitPdu(str, str2, str3, z, i);
    }

    public static SmsMessageBase.SubmitPduBase getSubmitPduCdma(String str, String str2, String str3, boolean z, SmsHeader smsHeader) {
        return SmsMessage.getSubmitPdu(str, str2, str3, z, smsHeader);
    }

    public static SmsMessageBase.SubmitPduBase getSubmitPduCdma(String str, String str2, String str3, boolean z, SmsHeader smsHeader, int i) {
        return SmsMessage.getSubmitPdu(str, str2, str3, z, smsHeader, i);
    }

    public static SmsMessageBase.SubmitPduBase getSubmitPdu(boolean z, String str, String str2, int i, byte[] bArr, boolean z2) {
        if (z) {
            return getSubmitPduCdma(str, str2, i, bArr, z2);
        }
        return getSubmitPduGsm(str, str2, i, bArr, z2);
    }

    public static SmsMessageBase.SubmitPduBase getSubmitPduCdma(String str, String str2, int i, byte[] bArr, boolean z) {
        return SmsMessage.getSubmitPdu(str, str2, i, bArr, z);
    }

    public static SmsMessageBase.SubmitPduBase getSubmitPduGsm(String str, String str2, int i, byte[] bArr, boolean z) {
        return MtkSmsMessage.getSubmitPdu(str, str2, i, bArr, z);
    }

    public static GsmAlphabet.TextEncodingDetails calculateLength(boolean z, CharSequence charSequence, boolean z2) {
        if (z) {
            return calculateLengthCdma(charSequence, z2);
        }
        return calculateLengthGsm(charSequence, z2);
    }

    public static GsmAlphabet.TextEncodingDetails calculateLengthGsm(CharSequence charSequence, boolean z) {
        return MtkSmsMessage.calculateLength(charSequence, z);
    }

    public static GsmAlphabet.TextEncodingDetails calculateLengthCdma(CharSequence charSequence, boolean z) {
        return SmsMessage.calculateLength(charSequence, z, false);
    }

    public static SmsMessageBase.SubmitPduBase getSubmitPdu(boolean z, String str, String str2, int i, int i2, byte[] bArr, boolean z2) {
        if (z) {
            return null;
        }
        return getSubmitPduGsm(str, str2, i, i2, bArr, z2);
    }

    public static SmsMessageBase.SubmitPduBase getSubmitPduGsm(String str, String str2, int i, int i2, byte[] bArr, boolean z) {
        return MtkSmsMessage.getSubmitPdu(str, str2, i, i2, bArr, z);
    }

    public static String getPackageNameViaProcessId(Context context, String[] strArr) {
        String str;
        String str2 = null;
        if (strArr.length == 1) {
            str = strArr[0];
        } else if (strArr.length > 1) {
            int callingPid = Binder.getCallingPid();
            List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = ((ActivityManager) context.getSystemService("activity")).getRunningAppProcesses();
            if (runningAppProcesses != null) {
                for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : runningAppProcesses) {
                    if (callingPid == runningAppProcessInfo.pid) {
                        String[] strArr2 = runningAppProcessInfo.pkgList;
                        String str3 = null;
                        for (String str4 : strArr2) {
                            int length = strArr.length;
                            int i = 0;
                            while (true) {
                                if (i >= length) {
                                    break;
                                }
                                String str5 = strArr[i];
                                if (!str5.equals(str4)) {
                                    i++;
                                } else {
                                    str3 = str5;
                                    break;
                                }
                            }
                            if (str3 != null) {
                                break;
                            }
                        }
                        str = str3;
                    }
                }
                str = null;
            } else {
                str = null;
            }
        }
        if (str == null) {
            if (strArr != null && strArr.length > 0) {
                str2 = strArr[0];
            }
        } else {
            str2 = str;
        }
        if (ENG) {
            Rlog.d(TAG, "getPackageNameViaProcessId: " + str2);
        }
        return str2;
    }

    public static SmsMessageBase.SubmitPduBase getSubmitPdu(boolean z, String str, String str2, byte[] bArr, byte[] bArr2, boolean z2) {
        if (!z) {
            return MtkSmsMessage.getSubmitPdu(str, str2, bArr, bArr2, z2);
        }
        return null;
    }

    public static SmsMessageBase.SubmitPduBase getSubmitPduGsm(String str, String str2, byte[] bArr, byte[] bArr2, boolean z) {
        return MtkSmsMessage.getSubmitPdu(str, str2, bArr, bArr2, z);
    }

    public static SmsMessageBase.SubmitPduBase getSubmitPdu(boolean z, String str, String str2, String str3, boolean z2, byte[] bArr, int i, int i2, int i3, int i4) {
        if (!z) {
            return getSubmitPduGsm(str, str2, str3, z2, bArr, i, i2, i3, i4);
        }
        return null;
    }

    public static SmsMessageBase.SubmitPduBase getSubmitPduGsm(String str, String str2, String str3, boolean z, byte[] bArr, int i, int i2, int i3, int i4) {
        return MtkSmsMessage.getSubmitPdu(str, str2, str3, z, bArr, i, i2, i3, i4);
    }
}
