package com.android.systemui.qs.car;

import android.R;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.LinearLayout;
import com.android.settingslib.Utils;
import com.android.systemui.BatteryMeterView;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;

/* loaded from: classes.dex */
public class CarStatusBarHeader extends LinearLayout {
    public CarStatusBarHeader(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        int colorAttr = Utils.getColorAttr(getContext(), R.attr.colorForeground);
        float f = colorAttr == -1 ? 0.0f : 1.0f;
        Rect rect = new Rect(0, 0, 0, 0);
        applyDarkness(com.android.systemui.R.id.battery, rect, f, colorAttr);
        applyDarkness(com.android.systemui.R.id.clock, rect, f, colorAttr);
        ((BatteryMeterView) findViewById(com.android.systemui.R.id.battery)).setForceShowPercent(true);
    }

    private void applyDarkness(int i, Rect rect, float f, int i2) {
        KeyEvent.Callback callbackFindViewById = findViewById(i);
        if (callbackFindViewById instanceof DarkIconDispatcher.DarkReceiver) {
            ((DarkIconDispatcher.DarkReceiver) callbackFindViewById).onDarkChanged(rect, f, i2);
        }
    }
}
