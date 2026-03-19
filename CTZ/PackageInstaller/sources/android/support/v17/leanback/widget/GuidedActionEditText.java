package android.support.v17.leanback.widget;

import android.R;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.widget.GuidedActionAutofillSupport;
import android.support.v17.leanback.widget.ImeKeyMonitor;
import android.support.v4.widget.TextViewCompat;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.autofill.AutofillValue;
import android.widget.EditText;
import android.widget.TextView;

public class GuidedActionEditText extends EditText implements GuidedActionAutofillSupport, ImeKeyMonitor {
    private GuidedActionAutofillSupport.OnAutofillListener mAutofillListener;
    private ImeKeyMonitor.ImeKeyListener mKeyListener;
    private final Drawable mNoPaddingDrawable;
    private final Drawable mSavedBackground;

    static final class NoPaddingDrawable extends Drawable {
        NoPaddingDrawable() {
        }

        @Override
        public boolean getPadding(Rect padding) {
            padding.set(0, 0, 0, 0);
            return true;
        }

        @Override
        public void draw(Canvas canvas) {
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return -2;
        }
    }

    public GuidedActionEditText(Context ctx) {
        this(ctx, null);
    }

    public GuidedActionEditText(Context ctx, AttributeSet attrs) {
        this(ctx, attrs, R.attr.editTextStyle);
    }

    public GuidedActionEditText(Context ctx, AttributeSet attrs, int defStyleAttr) {
        super(ctx, attrs, defStyleAttr);
        this.mSavedBackground = getBackground();
        this.mNoPaddingDrawable = new NoPaddingDrawable();
        setBackground(this.mNoPaddingDrawable);
    }

    @Override
    public void setImeKeyListener(ImeKeyMonitor.ImeKeyListener listener) {
        this.mKeyListener = listener;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        boolean result = false;
        if (this.mKeyListener != null) {
            result = this.mKeyListener.onKeyPreIme(this, keyCode, event);
        }
        if (!result) {
            boolean result2 = super.onKeyPreIme(keyCode, event);
            return result2;
        }
        return result;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName((isFocused() ? EditText.class : TextView.class).getName());
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (focused) {
            setBackground(this.mSavedBackground);
        } else {
            setBackground(this.mNoPaddingDrawable);
        }
        if (!focused) {
            setFocusable(false);
        }
    }

    @Override
    public int getAutofillType() {
        return 1;
    }

    @Override
    public void setOnAutofillListener(GuidedActionAutofillSupport.OnAutofillListener autofillListener) {
        this.mAutofillListener = autofillListener;
    }

    @Override
    public void autofill(AutofillValue values) {
        super.autofill(values);
        if (this.mAutofillListener != null) {
            this.mAutofillListener.onAutofill(this);
        }
    }

    @Override
    public void setCustomSelectionActionModeCallback(ActionMode.Callback actionModeCallback) {
        super.setCustomSelectionActionModeCallback(TextViewCompat.wrapCustomSelectionActionModeCallback(this, actionModeCallback));
    }
}
