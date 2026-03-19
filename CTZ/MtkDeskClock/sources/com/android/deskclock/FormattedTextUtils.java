package com.android.deskclock;

import android.text.SpannableString;

public class FormattedTextUtils {
    private FormattedTextUtils() {
    }

    public static CharSequence formatText(CharSequence charSequence, Object obj) {
        if (charSequence == null) {
            return null;
        }
        SpannableString spannableStringValueOf = SpannableString.valueOf(charSequence);
        spannableStringValueOf.setSpan(obj, 0, spannableStringValueOf.length(), 33);
        return spannableStringValueOf;
    }
}
