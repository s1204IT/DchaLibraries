package jp.co.omronsoft.android.inputmethodservice;

import android.R;
import android.content.Context;
import android.inputmethodservice.ExtractEditText;
import android.text.Editable;
import android.text.ParcelableSpan;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.MetaKeyKeyListener;
import android.util.AttributeSet;
import android.view.inputmethod.ExtractedText;
import android.widget.TextView;

class EmojiExtractEditText extends ExtractEditText {
    public EmojiExtractEditText(Context context) {
        super(context, null);
    }

    public EmojiExtractEditText(Context context, AttributeSet attrs) {
        super(context, attrs, R.attr.editTextStyle);
    }

    public EmojiExtractEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setExtractedText(ExtractedText text) {
        try {
            startInternalChanges();
            setExtractedTextTextView(text);
        } finally {
            finishInternalChanges();
        }
    }

    private void setExtractedTextTextView(ExtractedText text) {
        Editable content = getEditableText();
        if (text.text != null) {
            if (content == null) {
                setText(text.text, TextView.BufferType.EDITABLE);
            } else if (text.partialStartOffset < 0) {
                removeParcelableSpans(content, 0, content.length());
                content.replace(0, content.length(), text.text);
            } else {
                int N = content.length();
                int start = text.partialStartOffset;
                if (start > N) {
                    start = N;
                }
                int end = text.partialEndOffset;
                if (end > N) {
                    end = N;
                }
                removeParcelableSpans(content, start, end);
                content.replace(start, end, text.text);
            }
        }
        Spannable sp = getText();
        int N2 = sp.length();
        int start2 = text.selectionStart;
        if (start2 < 0) {
            start2 = 0;
        } else if (start2 > N2) {
            start2 = N2;
        }
        int end2 = text.selectionEnd;
        if (end2 < 0) {
            end2 = 0;
        } else if (end2 > N2) {
            end2 = N2;
        }
        Selection.setSelection(sp, start2, end2);
        if ((text.flags & 2) != 0) {
            MetaKeyKeyListener.startSelecting(this, sp);
        } else {
            MetaKeyKeyListener.stopSelecting(this, sp);
        }
    }

    static void removeParcelableSpans(Spannable spannable, int start, int end) {
        if (start != end) {
            Object[] spans = spannable.getSpans(start, end, ParcelableSpan.class);
            int i = spans.length;
            while (i > 0) {
                i--;
                spannable.removeSpan(spans[i]);
            }
        }
    }
}
