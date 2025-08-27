package com.android.settings;

import android.text.Spannable;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

/* loaded from: classes.dex */
public class LinkifyUtils {

    public interface OnClickListener {
        void onClick();
    }

    public static boolean linkify(TextView textView, StringBuilder sb, final OnClickListener onClickListener) {
        int iIndexOf = sb.indexOf("LINK_BEGIN");
        if (iIndexOf == -1) {
            textView.setText(sb);
            return false;
        }
        sb.delete(iIndexOf, "LINK_BEGIN".length() + iIndexOf);
        int iIndexOf2 = sb.indexOf("LINK_END");
        if (iIndexOf2 == -1) {
            textView.setText(sb);
            return false;
        }
        sb.delete(iIndexOf2, "LINK_END".length() + iIndexOf2);
        textView.setText(sb.toString(), TextView.BufferType.SPANNABLE);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        ((Spannable) textView.getText()).setSpan(new ClickableSpan() { // from class: com.android.settings.LinkifyUtils.1
            @Override // android.text.style.ClickableSpan
            public void onClick(View view) {
                onClickListener.onClick();
            }

            @Override // android.text.style.ClickableSpan, android.text.style.CharacterStyle
            public void updateDrawState(TextPaint textPaint) {
                super.updateDrawState(textPaint);
                textPaint.setUnderlineText(false);
            }
        }, iIndexOf, iIndexOf2, 33);
        return true;
    }
}
