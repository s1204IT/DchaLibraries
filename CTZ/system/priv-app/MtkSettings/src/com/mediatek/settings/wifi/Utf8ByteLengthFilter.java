package com.mediatek.settings.wifi;

import android.text.InputFilter;
import android.text.Spanned;

/* loaded from: classes.dex */
public class Utf8ByteLengthFilter implements InputFilter {
    private final int mMaxBytes;

    public Utf8ByteLengthFilter(int i) {
        this.mMaxBytes = i;
    }

    @Override // android.text.InputFilter
    public CharSequence filter(CharSequence charSequence, int i, int i2, Spanned spanned, int i3, int i4) {
        int i5;
        int i6;
        int i7 = i;
        int i8 = 0;
        while (true) {
            int i9 = 1;
            if (i7 >= i2) {
                break;
            }
            char cCharAt = charSequence.charAt(i7);
            if (cCharAt >= 128) {
                i9 = cCharAt < 2048 ? 2 : 3;
            }
            i8 += i9;
            i7++;
        }
        int length = spanned.length();
        int i10 = 0;
        for (int i11 = 0; i11 < length; i11++) {
            if (i11 < i3 || i11 >= i4) {
                char cCharAt2 = spanned.charAt(i11);
                if (cCharAt2 < 128) {
                    i6 = 1;
                } else {
                    i6 = cCharAt2 < 2048 ? 2 : 3;
                }
                i10 += i6;
            }
        }
        int i12 = this.mMaxBytes - i10;
        if (i12 <= 0) {
            return "";
        }
        if (i12 >= i8) {
            return null;
        }
        int i13 = i12;
        for (int i14 = i; i14 < i2; i14++) {
            char cCharAt3 = charSequence.charAt(i14);
            if (cCharAt3 < 128) {
                i5 = 1;
            } else {
                i5 = cCharAt3 < 2048 ? 2 : 3;
            }
            i13 -= i5;
            if (i13 < 0) {
                return charSequence.subSequence(i, i14);
            }
        }
        return null;
    }
}
