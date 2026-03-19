package com.android.contacts.datepicker;

public class ICU {
    public static char[] getDateFormatOrder(String str) {
        char[] cArr = new char[3];
        int i = 0;
        boolean z = false;
        int i2 = 0;
        boolean z2 = false;
        boolean z3 = false;
        while (i < str.length()) {
            char cCharAt = str.charAt(i);
            if (cCharAt == 'd' || cCharAt == 'L' || cCharAt == 'M' || cCharAt == 'y') {
                if (cCharAt == 'd' && !z) {
                    cArr[i2] = 'd';
                    i2++;
                    z = true;
                } else if ((cCharAt == 'L' || cCharAt == 'M') && !z2) {
                    cArr[i2] = 'M';
                    i2++;
                    z2 = true;
                } else if (cCharAt == 'y' && !z3) {
                    cArr[i2] = 'y';
                    i2++;
                    z3 = true;
                }
            } else if (cCharAt == 'G') {
                continue;
            } else {
                if ((cCharAt >= 'a' && cCharAt <= 'z') || (cCharAt >= 'A' && cCharAt <= 'Z')) {
                    throw new IllegalArgumentException("Bad pattern character '" + cCharAt + "' in " + str);
                }
                if (cCharAt != '\'') {
                    continue;
                } else if (i < str.length() - 1) {
                    int i3 = i + 1;
                    if (str.charAt(i3) == '\'') {
                        i = i3;
                    } else {
                        int iIndexOf = str.indexOf(39, i + 1);
                        if (iIndexOf == -1) {
                            throw new IllegalArgumentException("Bad quoting in " + str);
                        }
                        i = iIndexOf + 1;
                    }
                }
            }
            i++;
        }
        return cArr;
    }
}
