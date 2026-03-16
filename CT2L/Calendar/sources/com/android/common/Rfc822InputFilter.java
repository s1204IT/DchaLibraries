package com.android.common;

import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

public class Rfc822InputFilter implements InputFilter {
    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        if (end - start != 1 || source.charAt(start) != ' ') {
            return null;
        }
        int scanBack = dstart;
        boolean dotFound = false;
        while (scanBack > 0) {
            scanBack--;
            char c = dest.charAt(scanBack);
            switch (c) {
                case ',':
                    return null;
                case '.':
                    dotFound = true;
                    break;
                case '@':
                    if (!dotFound) {
                        return null;
                    }
                    if (source instanceof Spanned) {
                        SpannableStringBuilder sb = new SpannableStringBuilder(",");
                        sb.append(source);
                        return sb;
                    }
                    return ", ";
            }
        }
        return null;
    }
}
