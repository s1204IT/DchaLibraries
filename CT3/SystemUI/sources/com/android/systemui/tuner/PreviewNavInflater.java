package com.android.systemui.tuner;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import com.android.systemui.statusbar.phone.NavigationBarInflaterView;

public class PreviewNavInflater extends NavigationBarInflaterView {
    public PreviewNavInflater(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        TunerService.get(getContext()).removeTunable(this);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        if ("sysui_nav_bar".equals(key)) {
            if (!isValidLayout(newValue)) {
                return;
            }
            super.onTuningChanged(key, newValue);
            return;
        }
        super.onTuningChanged(key, newValue);
    }

    private boolean isValidLayout(String newValue) {
        if (newValue == null) {
            return true;
        }
        int separatorCount = 0;
        int lastGravitySeparator = 0;
        for (int i = 0; i < newValue.length(); i++) {
            if (newValue.charAt(i) == ";".charAt(0)) {
                if (i == 0 || i - lastGravitySeparator == 1) {
                    return false;
                }
                lastGravitySeparator = i;
                separatorCount++;
            }
        }
        return separatorCount == 2 && newValue.length() - lastGravitySeparator != 1;
    }
}
