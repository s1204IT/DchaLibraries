package com.android.contacts.common.format;

import android.text.SpannableString;
import android.text.style.CharacterStyle;
import android.text.style.StyleSpan;
import android.widget.TextView;

public class TextHighlighter {
    private int mTextStyle;
    private final String TAG = TextHighlighter.class.getSimpleName();
    private CharacterStyle mTextStyleSpan = getStyleSpan();

    public TextHighlighter(int textStyle) {
        this.mTextStyle = textStyle;
    }

    public void setPrefixText(TextView view, String text, String prefix) {
        view.setText(applyPrefixHighlight(text, prefix));
    }

    private CharacterStyle getStyleSpan() {
        return new StyleSpan(this.mTextStyle);
    }

    public void applyMaskingHighlight(SpannableString text, int start, int end) {
        text.setSpan(getStyleSpan(), start, end, 0);
    }

    public CharSequence applyPrefixHighlight(CharSequence text, String prefix) {
        if (prefix != null) {
            int prefixStart = 0;
            while (prefixStart < prefix.length() && !Character.isLetterOrDigit(prefix.charAt(prefixStart))) {
                prefixStart++;
            }
            String trimmedPrefix = prefix.substring(prefixStart);
            int index = FormatUtils.indexOfWordPrefix(text, trimmedPrefix);
            if (index != -1) {
                SpannableString result = new SpannableString(text);
                result.setSpan(this.mTextStyleSpan, index, trimmedPrefix.length() + index, 0);
                return result;
            }
            return text;
        }
        return text;
    }
}
