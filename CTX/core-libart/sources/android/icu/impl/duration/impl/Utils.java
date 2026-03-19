package android.icu.impl.duration.impl;

import android.icu.impl.locale.BaseLocale;
import java.util.Locale;

public class Utils {
    public static final Locale localeFromString(String str) {
        String strSubstring = "";
        String strSubstring2 = "";
        int iIndexOf = str.indexOf(BaseLocale.SEP);
        if (iIndexOf != -1) {
            strSubstring = str.substring(iIndexOf + 1);
            str = str.substring(0, iIndexOf);
        }
        int iIndexOf2 = strSubstring.indexOf(BaseLocale.SEP);
        if (iIndexOf2 != -1) {
            strSubstring2 = strSubstring.substring(iIndexOf2 + 1);
            strSubstring = strSubstring.substring(0, iIndexOf2);
        }
        return new Locale(str, strSubstring, strSubstring2);
    }

    public static String chineseNumber(long j, ChineseDigits chineseDigits) {
        boolean z;
        boolean z2;
        int i;
        long j2 = j;
        if (j2 < 0) {
            j2 = -j2;
        }
        if (j2 <= 10) {
            return j2 == 2 ? String.valueOf(chineseDigits.liang) : String.valueOf(chineseDigits.digits[(int) j2]);
        }
        char[] cArr = new char[40];
        char[] charArray = String.valueOf(j2).toCharArray();
        int length = cArr.length;
        int length2 = charArray.length;
        int i2 = -1;
        int i3 = length;
        int i4 = -1;
        int i5 = -1;
        boolean z3 = true;
        boolean z4 = false;
        while (true) {
            length2 += i2;
            if (length2 < 0) {
                break;
            }
            if (i4 == i2) {
                if (i5 != i2) {
                    i3--;
                    cArr[i3] = chineseDigits.levels[i5];
                    z3 = true;
                    z4 = false;
                }
                i4++;
            } else {
                i3--;
                int i6 = i4 + 1;
                cArr[i3] = chineseDigits.units[i4];
                if (i6 == 3) {
                    i5++;
                    z2 = z3;
                    i4 = -1;
                    i = charArray[length2] - '0';
                    if (i != 0) {
                        if (i3 < cArr.length - 1 && i4 != 0) {
                            cArr[i3] = '*';
                        }
                        if (z2 || z4) {
                            i3--;
                            cArr[i3] = '*';
                            z3 = z2;
                        } else {
                            i3--;
                            cArr[i3] = chineseDigits.digits[0];
                            z4 = i4 == 1;
                            z3 = true;
                        }
                    } else {
                        i3--;
                        cArr[i3] = chineseDigits.digits[i];
                        z3 = false;
                    }
                    i2 = -1;
                } else {
                    i4 = i6;
                }
            }
            z2 = z3;
            i = charArray[length2] - '0';
            if (i != 0) {
            }
            i2 = -1;
        }
        if (j2 > 1000000) {
            int length3 = cArr.length - 3;
            boolean z5 = true;
            while (cArr[length3] != '0') {
                length3 -= 8;
                z5 = !z5;
                if (length3 <= i3) {
                    break;
                }
            }
            int length4 = cArr.length - 7;
            do {
                if (cArr[length4] == chineseDigits.digits[0] && !z5) {
                    cArr[length4] = '*';
                }
                length4 -= 8;
                z5 = !z5;
            } while (length4 > i3);
            if (j2 >= 100000000) {
                int length5 = cArr.length - 8;
                do {
                    int i7 = length5 - 1;
                    int iMax = Math.max(i3 - 1, length5 - 8);
                    while (true) {
                        if (i7 <= iMax) {
                            z = true;
                            break;
                        }
                        if (cArr[i7] != '*') {
                            z = false;
                            break;
                        }
                        i7--;
                    }
                    if (z) {
                        int i8 = length5 + 1;
                        if (cArr[i8] == '*' || cArr[i8] == chineseDigits.digits[0]) {
                            cArr[length5] = '*';
                        } else {
                            cArr[length5] = chineseDigits.digits[0];
                        }
                    }
                    length5 -= 8;
                } while (length5 > i3);
            }
        }
        for (int i9 = i3; i9 < cArr.length; i9++) {
            if (cArr[i9] == chineseDigits.digits[2] && (i9 >= cArr.length - 1 || cArr[i9 + 1] != chineseDigits.units[0])) {
                if (i9 > i3) {
                    int i10 = i9 - 1;
                    if (cArr[i10] != chineseDigits.units[0] && cArr[i10] != chineseDigits.digits[0] && cArr[i10] != '*') {
                        cArr[i9] = chineseDigits.liang;
                    }
                }
            }
        }
        if (cArr[i3] == chineseDigits.digits[1] && (chineseDigits.ko || cArr[i3 + 1] == chineseDigits.units[0])) {
            i3++;
        }
        int i11 = i3;
        int i12 = i11;
        while (i11 < cArr.length) {
            if (cArr[i11] != '*') {
                cArr[i12] = cArr[i11];
                i12++;
            }
            i11++;
        }
        return new String(cArr, i3, i12 - i3);
    }

    public static class ChineseDigits {
        final char[] digits;
        final boolean ko;
        final char[] levels;
        final char liang;
        final char[] units;
        public static final ChineseDigits DEBUG = new ChineseDigits("0123456789s", "sbq", "WYZ", 'L', false);
        public static final ChineseDigits TRADITIONAL = new ChineseDigits("零一二三四五六七八九十", "十百千", "萬億兆", 20841, false);
        public static final ChineseDigits SIMPLIFIED = new ChineseDigits("零一二三四五六七八九十", "十百千", "万亿兆", 20004, false);
        public static final ChineseDigits KOREAN = new ChineseDigits("영일이삼사오육칠팔구십", "십백천", "만억?", 51060, true);

        ChineseDigits(String str, String str2, String str3, char c, boolean z) {
            this.digits = str.toCharArray();
            this.units = str2.toCharArray();
            this.levels = str3.toCharArray();
            this.liang = c;
            this.ko = z;
        }
    }
}
