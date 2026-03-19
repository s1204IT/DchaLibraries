package com.android.contacts.format;

import android.text.SpannableString;
import android.text.style.CharacterStyle;
import android.text.style.StyleSpan;
import android.widget.TextView;

public class TextHighlighter {
    private static final boolean DEBUG = false;
    private int mTextStyle;
    private final String TAG = TextHighlighter.class.getSimpleName();
    private CharacterStyle mTextStyleSpan = getStyleSpan();

    public TextHighlighter(int i) {
        this.mTextStyle = i;
    }

    public void setPrefixText(TextView textView, String str, String str2) {
        textView.setText(applyPrefixHighlight(str, str2));
    }

    private CharacterStyle getStyleSpan() {
        return new StyleSpan(this.mTextStyle);
    }

    public void applyMaskingHighlight(SpannableString spannableString, int i, int i2) {
        spannableString.setSpan(getStyleSpan(), i, i2, 0);
    }

    public CharSequence applyPrefixHighlight(CharSequence charSequence, String str) {
        if (str == null) {
            return charSequence;
        }
        int i = 0;
        while (i < str.length() && !Character.isLetterOrDigit(str.charAt(i))) {
            i++;
        }
        String strSubstring = str.substring(i);
        int iIndexOfWordPrefix = FormatUtils.indexOfWordPrefix(charSequence, strSubstring);
        if (iIndexOfWordPrefix != -1) {
            SpannableString spannableString = new SpannableString(charSequence);
            spannableString.setSpan(this.mTextStyleSpan, iIndexOfWordPrefix, strSubstring.length() + iIndexOfWordPrefix, 0);
            return spannableString;
        }
        return charSequence;
    }
}
