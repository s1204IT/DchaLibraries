package com.android.settings.graph;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import com.android.settings.R;
import com.android.settingslib.wifi.AccessPoint;

/* loaded from: classes.dex */
public class BottomLabelLayout extends LinearLayout {
    public BottomLabelLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override // android.widget.LinearLayout, android.view.View
    protected void onMeasure(int i, int i2) {
        boolean z;
        int iMakeMeasureSpec;
        int size = View.MeasureSpec.getSize(i);
        boolean zIsStacked = isStacked();
        boolean z2 = true;
        if (!zIsStacked && View.MeasureSpec.getMode(i) == 1073741824) {
            iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(size, AccessPoint.UNREACHABLE_RSSI);
            z = true;
        } else {
            z = false;
            iMakeMeasureSpec = i;
        }
        super.onMeasure(iMakeMeasureSpec, i2);
        if (!zIsStacked && (getMeasuredWidthAndState() & (-16777216)) == 16777216) {
            setStacked(true);
        } else {
            z2 = z;
        }
        if (z2) {
            super.onMeasure(i, i2);
        }
    }

    void setStacked(boolean z) {
        setOrientation(z ? 1 : 0);
        setGravity(z ? 8388611 : 80);
        View viewFindViewById = findViewById(R.id.spacer);
        if (viewFindViewById != null) {
            viewFindViewById.setVisibility(z ? 8 : 0);
        }
    }

    private boolean isStacked() {
        return getOrientation() == 1;
    }
}
