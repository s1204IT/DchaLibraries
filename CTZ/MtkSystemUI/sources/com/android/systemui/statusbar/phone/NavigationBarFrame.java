package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import com.android.systemui.statusbar.policy.DeadZone;

public class NavigationBarFrame extends FrameLayout {
    private DeadZone mDeadZone;

    public NavigationBarFrame(Context context) {
        super(context);
        this.mDeadZone = null;
    }

    public NavigationBarFrame(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mDeadZone = null;
    }

    public NavigationBarFrame(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mDeadZone = null;
    }

    public void setDeadZone(DeadZone deadZone) {
        this.mDeadZone = deadZone;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == 4 && this.mDeadZone != null) {
            return this.mDeadZone.onTouchEvent(motionEvent);
        }
        return super.dispatchTouchEvent(motionEvent);
    }
}
