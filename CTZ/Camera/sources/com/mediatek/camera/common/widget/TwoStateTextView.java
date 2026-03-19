package com.mediatek.camera.common.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class TwoStateTextView extends TextView {
    private boolean mFilterEnabled;

    public TwoStateTextView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mFilterEnabled = true;
    }

    @Override
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
    }

    @Override
    public void setEnabled(boolean z) {
        super.setEnabled(z);
        if (this.mFilterEnabled) {
            if (z) {
                setAlpha(1.0f);
            } else {
                setAlpha(0.4f);
            }
        }
    }
}
