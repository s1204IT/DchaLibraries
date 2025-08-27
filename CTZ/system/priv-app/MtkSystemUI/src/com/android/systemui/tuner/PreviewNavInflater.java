package com.android.systemui.tuner;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.phone.NavigationBarInflaterView;

/* loaded from: classes.dex */
public class PreviewNavInflater extends NavigationBarInflaterView {
    public PreviewNavInflater(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override // com.android.systemui.statusbar.phone.NavigationBarInflaterView, android.view.ViewGroup, android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ((TunerService) Dependency.get(TunerService.class)).removeTunable(this);
    }

    @Override // android.view.ViewGroup
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        return true;
    }

    @Override // com.android.systemui.statusbar.phone.NavigationBarInflaterView, com.android.systemui.tuner.TunerService.Tunable
    public void onTuningChanged(String str, String str2) throws NumberFormatException {
        if ("sysui_nav_bar".equals(str)) {
            if (isValidLayout(str2)) {
                super.onTuningChanged(str, str2);
                return;
            }
            return;
        }
        super.onTuningChanged(str, str2);
    }

    private boolean isValidLayout(String str) {
        if (str == null) {
            return true;
        }
        int i = 0;
        int i2 = 0;
        for (int i3 = 0; i3 < str.length(); i3++) {
            if (str.charAt(i3) == ";".charAt(0)) {
                if (i3 == 0 || i3 - i2 == 1) {
                    return false;
                }
                i++;
                i2 = i3;
            }
        }
        if (i == 2 && str.length() - i2 != 1) {
            return true;
        }
        return false;
    }
}
