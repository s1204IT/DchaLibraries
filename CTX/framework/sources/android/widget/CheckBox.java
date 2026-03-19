package android.widget;

import android.content.Context;
import android.util.AttributeSet;

public class CheckBox extends CompoundButton {
    public CheckBox(Context context) {
        this(context, null);
    }

    public CheckBox(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 16842860);
    }

    public CheckBox(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public CheckBox(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return CheckBox.class.getName();
    }
}
