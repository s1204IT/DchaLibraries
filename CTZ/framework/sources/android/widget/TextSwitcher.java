package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class TextSwitcher extends ViewSwitcher {
    public TextSwitcher(Context context) {
        super(context);
    }

    public TextSwitcher(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public void addView(View view, int i, ViewGroup.LayoutParams layoutParams) {
        if (!(view instanceof TextView)) {
            throw new IllegalArgumentException("TextSwitcher children must be instances of TextView");
        }
        super.addView(view, i, layoutParams);
    }

    public void setText(CharSequence charSequence) {
        ((TextView) getNextView()).setText(charSequence);
        showNext();
    }

    public void setCurrentText(CharSequence charSequence) {
        ((TextView) getCurrentView()).setText(charSequence);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return TextSwitcher.class.getName();
    }
}
