package com.android.setupwizardlib.view;

import android.content.Context;
import android.text.Annotation;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.TextAppearanceSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;
import com.android.setupwizardlib.span.LinkSpan;
import com.android.setupwizardlib.span.SpanHelper;
import com.android.setupwizardlib.view.TouchableMovementMethod;

public class RichTextView extends TextView implements LinkSpan.OnLinkClickListener {
    private LinkSpan.OnLinkClickListener mOnLinkClickListener;

    public static CharSequence getRichText(Context context, CharSequence charSequence) {
        if (charSequence instanceof Spanned) {
            SpannableString spannableString = new SpannableString(charSequence);
            for (Annotation annotation : (Annotation[]) spannableString.getSpans(0, spannableString.length(), Annotation.class)) {
                String key = annotation.getKey();
                if ("textAppearance".equals(key)) {
                    int identifier = context.getResources().getIdentifier(annotation.getValue(), "style", context.getPackageName());
                    if (identifier == 0) {
                        Log.w("RichTextView", "Cannot find resource: " + identifier);
                    }
                    SpanHelper.replaceSpan(spannableString, annotation, new TextAppearanceSpan(context, identifier));
                } else if ("link".equals(key)) {
                    SpanHelper.replaceSpan(spannableString, annotation, new LinkSpan(annotation.getValue()));
                }
            }
            return spannableString;
        }
        return charSequence;
    }

    public RichTextView(Context context) {
        super(context);
    }

    public RichTextView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public void setText(CharSequence charSequence, TextView.BufferType bufferType) {
        CharSequence richText = getRichText(getContext(), charSequence);
        super.setText(richText, bufferType);
        boolean zHasLinks = hasLinks(richText);
        if (zHasLinks) {
            setMovementMethod(TouchableMovementMethod.TouchableLinkMovementMethod.getInstance());
        } else {
            setMovementMethod(null);
        }
        setFocusable(zHasLinks);
        setRevealOnFocusHint(false);
        setFocusableInTouchMode(zHasLinks);
    }

    private boolean hasLinks(CharSequence charSequence) {
        return (charSequence instanceof Spanned) && ((ClickableSpan[]) ((Spanned) charSequence).getSpans(0, charSequence.length(), ClickableSpan.class)).length > 0;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        boolean zOnTouchEvent = super.onTouchEvent(motionEvent);
        MovementMethod movementMethod = getMovementMethod();
        if (movementMethod instanceof TouchableMovementMethod) {
            TouchableMovementMethod touchableMovementMethod = (TouchableMovementMethod) movementMethod;
            if (touchableMovementMethod.getLastTouchEvent() == motionEvent) {
                return touchableMovementMethod.isLastTouchEventHandled();
            }
        }
        return zOnTouchEvent;
    }

    @Override
    public boolean onLinkClick(LinkSpan linkSpan) {
        if (this.mOnLinkClickListener != null) {
            return this.mOnLinkClickListener.onLinkClick(linkSpan);
        }
        return false;
    }
}
