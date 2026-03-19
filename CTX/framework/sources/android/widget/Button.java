package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.widget.RemoteViews;

@RemoteViews.RemoteView
public class Button extends TextView {
    public Button(Context context) {
        this(context, null);
    }

    public Button(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 16842824);
    }

    public Button(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public Button(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return Button.class.getName();
    }

    @Override
    public PointerIcon onResolvePointerIcon(MotionEvent motionEvent, int i) {
        if (getPointerIcon() == null && isClickable() && isEnabled()) {
            return PointerIcon.getSystemIcon(getContext(), 1002);
        }
        return super.onResolvePointerIcon(motionEvent, i);
    }
}
