package com.android.settings.bluetooth;

import android.text.InputFilter;
import android.text.Spanned;

class Utf8ByteLengthFilter implements InputFilter {
    private final int mMaxBytes;

    Utf8ByteLengthFilter(int maxBytes) {
        this.mMaxBytes = maxBytes;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        int i;
        int i2;
        int i3;
        int srcByteCount = 0;
        for (int i4 = start; i4 < end; i4++) {
            char c = source.charAt(i4);
            if (c < 128) {
                i3 = 1;
            } else {
                i3 = c < 2048 ? 2 : 3;
            }
            srcByteCount += i3;
        }
        int destLen = dest.length();
        int destByteCount = 0;
        for (int i5 = 0; i5 < destLen; i5++) {
            if (i5 < dstart || i5 >= dend) {
                char c2 = dest.charAt(i5);
                if (c2 < 128) {
                    i2 = 1;
                } else {
                    i2 = c2 < 2048 ? 2 : 3;
                }
                destByteCount += i2;
            }
        }
        int keepBytes = this.mMaxBytes - destByteCount;
        if (keepBytes <= 0) {
            return "";
        }
        if (keepBytes >= srcByteCount) {
            return null;
        }
        for (int i6 = start; i6 < end; i6++) {
            char c3 = source.charAt(i6);
            if (c3 < 128) {
                i = 1;
            } else {
                i = c3 < 2048 ? 2 : 3;
            }
            keepBytes -= i;
            if (keepBytes < 0) {
                return source.subSequence(start, i6);
            }
        }
        return null;
    }
}
