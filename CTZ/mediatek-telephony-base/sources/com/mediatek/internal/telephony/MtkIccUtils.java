package com.mediatek.internal.telephony;

import android.telephony.Rlog;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.uicc.IccUtils;

public class MtkIccUtils extends IccUtils {
    static final String MTK_LOG_TAG = "MtkIccUtils";

    public static String parsePlmnToStringForEfOpl(byte[] bArr, int i, int i2) {
        StringBuilder sb = new StringBuilder(i2 * 2);
        int i3 = bArr[i] & 15;
        if (i3 >= 0 && i3 <= 9) {
            sb.append((char) (i3 + 48));
        } else {
            if (i3 == 13) {
                sb.append('d');
            }
            return sb.toString();
        }
        int i4 = (bArr[i] >> 4) & 15;
        if (i4 >= 0 && i4 <= 9) {
            sb.append((char) (i4 + 48));
        } else {
            if (i4 == 13) {
                sb.append('d');
            }
            return sb.toString();
        }
        int i5 = i + 1;
        int i6 = bArr[i5] & 15;
        if (i6 >= 0 && i6 <= 9) {
            sb.append((char) (i6 + 48));
        } else {
            if (i6 == 13) {
                sb.append('d');
            }
            return sb.toString();
        }
        int i7 = i + 2;
        int i8 = bArr[i7] & 15;
        if (i8 >= 0 && i8 <= 9) {
            sb.append((char) (i8 + 48));
        } else {
            if (i8 == 13) {
                sb.append('d');
            }
            return sb.toString();
        }
        int i9 = (bArr[i7] >> 4) & 15;
        if (i9 >= 0 && i9 <= 9) {
            sb.append((char) (i9 + 48));
        } else {
            if (i9 == 13) {
                sb.append('d');
            }
            return sb.toString();
        }
        int i10 = (bArr[i5] >> 4) & 15;
        if (i10 >= 0 && i10 <= 9) {
            sb.append((char) (48 + i10));
        } else if (i10 == 13) {
            sb.append('d');
        }
        return sb.toString();
    }

    public static String parseLanguageIndicator(byte[] bArr, int i, int i2) {
        if (bArr == null) {
            return null;
        }
        if (bArr.length < i + i2) {
            Rlog.e(MTK_LOG_TAG, "length is invalid");
            return null;
        }
        return GsmAlphabet.gsm8BitUnpackedToString(bArr, i, i2);
    }

    public static String parsePlmnToString(byte[] bArr, int i, int i2) {
        StringBuilder sb = new StringBuilder(i2 * 2);
        int i3 = bArr[i] & 15;
        if (i3 <= 9) {
            sb.append((char) (i3 + 48));
            int i4 = (bArr[i] >> 4) & 15;
            if (i4 <= 9) {
                sb.append((char) (i4 + 48));
                int i5 = i + 1;
                int i6 = bArr[i5] & 15;
                if (i6 <= 9) {
                    sb.append((char) (i6 + 48));
                    int i7 = i + 2;
                    int i8 = bArr[i7] & 15;
                    if (i8 <= 9) {
                        sb.append((char) (i8 + 48));
                        int i9 = (bArr[i7] >> 4) & 15;
                        if (i9 <= 9) {
                            sb.append((char) (i9 + 48));
                            int i10 = (bArr[i5] >> 4) & 15;
                            if (i10 <= 9) {
                                sb.append((char) (48 + i10));
                            }
                        }
                    }
                }
            }
        }
        return sb.toString();
    }
}
