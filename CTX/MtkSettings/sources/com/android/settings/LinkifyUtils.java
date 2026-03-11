package com.android.settings;

import android.text.Spannable;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

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
        ((Spannable) textView.getText()).setSpan(new ClickableSpan() {
            @Override
            public void onClick(View view) {
                onClickListener.onClick();
            }

            @Override
            public void updateDrawState(TextPaint textPaint) {
                super.updateDrawState(textPaint);
                textPaint.setUnderlineText(false);
            }
        }, iIndexOf, iIndexOf2, 33);
        return true;
    }
}
