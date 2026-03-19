package com.android.internal.widget;

import android.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.text.Layout;
import android.util.AttributeSet;
import android.widget.TextView;

public class DialogTitle extends TextView {
    public DialogTitle(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    public DialogTitle(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public DialogTitle(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public DialogTitle(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int lineCount;
        super.onMeasure(i, i2);
        Layout layout = getLayout();
        if (layout != null && (lineCount = layout.getLineCount()) > 0 && layout.getEllipsisCount(lineCount - 1) > 0) {
            setSingleLine(false);
            setMaxLines(2);
            TypedArray typedArrayObtainStyledAttributes = this.mContext.obtainStyledAttributes(null, R.styleable.TextAppearance, 16842817, 16973892);
            int dimensionPixelSize = typedArrayObtainStyledAttributes.getDimensionPixelSize(0, 0);
            if (dimensionPixelSize != 0) {
                setTextSize(0, dimensionPixelSize);
            }
            typedArrayObtainStyledAttributes.recycle();
            super.onMeasure(i, i2);
        }
    }
}
