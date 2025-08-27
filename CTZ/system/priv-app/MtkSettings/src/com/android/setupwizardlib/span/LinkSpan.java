package com.android.setupwizardlib.span;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

/* loaded from: classes.dex */
public class LinkSpan extends ClickableSpan {
    private static final Typeface TYPEFACE_MEDIUM = Typeface.create("sans-serif-medium", 0);
    private final String mId;

    @Deprecated
    public interface OnClickListener {
        void onClick(LinkSpan linkSpan);
    }

    public interface OnLinkClickListener {
        boolean onLinkClick(LinkSpan linkSpan);
    }

    public LinkSpan(String str) {
        this.mId = str;
    }

    @Override // android.text.style.ClickableSpan
    public void onClick(View view) {
        if (dispatchClick(view)) {
            if (Build.VERSION.SDK_INT >= 19) {
                view.cancelPendingInputEvents();
            }
        } else {
            Log.w("LinkSpan", "Dropping click event. No listener attached.");
        }
        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            if (text instanceof Spannable) {
                Selection.setSelection((Spannable) text, 0);
            }
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: android.view.View */
    /* JADX WARN: Multi-variable type inference failed */
    private boolean dispatchClick(View view) {
        boolean zOnLinkClick;
        OnClickListener legacyListenerFromContext;
        if (view instanceof OnLinkClickListener) {
            zOnLinkClick = ((OnLinkClickListener) view).onLinkClick(this);
        } else {
            zOnLinkClick = false;
        }
        if (!zOnLinkClick && (legacyListenerFromContext = getLegacyListenerFromContext(view.getContext())) != null) {
            legacyListenerFromContext.onClick(this);
            return true;
        }
        return zOnLinkClick;
    }

    @Deprecated
    private OnClickListener getLegacyListenerFromContext(Context context) {
        Object baseContext = context;
        while (!(baseContext instanceof OnClickListener)) {
            if (baseContext instanceof ContextWrapper) {
                baseContext = ((ContextWrapper) baseContext).getBaseContext();
            } else {
                return null;
            }
        }
        return (OnClickListener) baseContext;
    }

    @Override // android.text.style.ClickableSpan, android.text.style.CharacterStyle
    public void updateDrawState(TextPaint textPaint) {
        super.updateDrawState(textPaint);
        textPaint.setUnderlineText(false);
        textPaint.setTypeface(TYPEFACE_MEDIUM);
    }

    public String getId() {
        return this.mId;
    }
}
