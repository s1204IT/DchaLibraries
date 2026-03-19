package com.android.common;

import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

public class Rfc822InputFilter implements InputFilter {
    @Override
    public CharSequence filter(CharSequence charSequence, int i, int i2, Spanned spanned, int i3, int i4) {
        if (i2 - i != 1 || charSequence.charAt(i) != ' ') {
            return null;
        }
        boolean z = false;
        while (i3 > 0) {
            i3--;
            char cCharAt = spanned.charAt(i3);
            if (cCharAt == ',') {
                return null;
            }
            if (cCharAt == '.') {
                z = true;
            } else if (cCharAt == '@') {
                if (!z) {
                    return null;
                }
                if (charSequence instanceof Spanned) {
                    SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(",");
                    spannableStringBuilder.append(charSequence);
                    return spannableStringBuilder;
                }
                return ", ";
            }
        }
        return null;
    }
}
