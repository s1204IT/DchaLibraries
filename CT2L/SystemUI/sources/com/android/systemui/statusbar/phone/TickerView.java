package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextSwitcher;

public class TickerView extends TextSwitcher {
    Ticker mTicker;

    public TickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (this.mTicker != null) {
            this.mTicker.reflowText();
        }
    }
}
