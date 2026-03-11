package com.android.settings.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.text.Annotation;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.View;
import com.android.settings.utils.AnnotationSpan;

public class AnnotationSpan extends URLSpan {
    private final View.OnClickListener mClickListener;

    private AnnotationSpan(View.OnClickListener onClickListener) {
        super((String) null);
        this.mClickListener = onClickListener;
    }

    @Override
    public void onClick(View view) {
        if (this.mClickListener != null) {
            this.mClickListener.onClick(view);
        }
    }

    @Override
    public void updateDrawState(TextPaint textPaint) {
        super.updateDrawState(textPaint);
        textPaint.setUnderlineText(false);
    }

    public static CharSequence linkify(CharSequence charSequence, LinkInfo... linkInfoArr) {
        SpannableString spannableString = new SpannableString(charSequence);
        Annotation[] annotationArr = (Annotation[]) spannableString.getSpans(0, spannableString.length(), Annotation.class);
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(spannableString);
        for (Annotation annotation : annotationArr) {
            String value = annotation.getValue();
            int spanStart = spannableString.getSpanStart(annotation);
            int spanEnd = spannableString.getSpanEnd(annotation);
            AnnotationSpan annotationSpan = null;
            int length = linkInfoArr.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                LinkInfo linkInfo = linkInfoArr[i];
                if (!linkInfo.mAnnotation.equals(value)) {
                    i++;
                } else {
                    annotationSpan = new AnnotationSpan(linkInfo.mListener);
                    break;
                }
            }
            if (annotationSpan != null) {
                spannableStringBuilder.setSpan(annotationSpan, spanStart, spanEnd, spannableString.getSpanFlags(annotationSpan));
            }
        }
        return spannableStringBuilder;
    }

    public static class LinkInfo {
        private final Boolean mActionable;
        private final String mAnnotation;
        private final View.OnClickListener mListener;

        public LinkInfo(String str, View.OnClickListener onClickListener) {
            this.mAnnotation = str;
            this.mListener = onClickListener;
            this.mActionable = true;
        }

        public LinkInfo(Context context, String str, final Intent intent) {
            this.mAnnotation = str;
            if (intent != null) {
                this.mActionable = Boolean.valueOf(context.getPackageManager().resolveActivity(intent, 0) != null);
            } else {
                this.mActionable = false;
            }
            if (!this.mActionable.booleanValue()) {
                this.mListener = null;
            } else {
                this.mListener = new View.OnClickListener() {
                    @Override
                    public final void onClick(View view) {
                        AnnotationSpan.LinkInfo.lambda$new$0(intent, view);
                    }
                };
            }
        }

        static void lambda$new$0(Intent intent, View view) {
            try {
                view.startActivityForResult(intent, 0);
            } catch (ActivityNotFoundException e) {
                Log.w("AnnotationSpan.LinkInfo", "Activity was not found for intent, " + intent);
            }
        }

        public boolean isActionable() {
            return this.mActionable.booleanValue();
        }
    }
}
