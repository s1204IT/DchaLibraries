package com.android.systemui.stackdivider;

import android.content.Context;
import android.view.View;
import android.view.WindowManager;

public class DividerWindowManager {
    private WindowManager.LayoutParams mLp;
    private View mView;
    private final WindowManager mWindowManager;

    public DividerWindowManager(Context ctx) {
        this.mWindowManager = (WindowManager) ctx.getSystemService(WindowManager.class);
    }

    public void add(View view, int width, int height) {
        this.mLp = new WindowManager.LayoutParams(width, height, 2034, 545521704, -3);
        this.mLp.setTitle("DockedStackDivider");
        this.mLp.privateFlags |= 64;
        view.setSystemUiVisibility(1792);
        this.mWindowManager.addView(view, this.mLp);
        this.mView = view;
    }

    public void remove() {
        if (this.mView != null) {
            this.mWindowManager.removeView(this.mView);
        }
        this.mView = null;
    }

    public void setSlippery(boolean slippery) {
        boolean changed = false;
        if (slippery && (this.mLp.flags & 536870912) == 0) {
            this.mLp.flags |= 536870912;
            changed = true;
        } else if (!slippery && (this.mLp.flags & 536870912) != 0) {
            this.mLp.flags &= -536870913;
            changed = true;
        }
        if (!changed) {
            return;
        }
        this.mWindowManager.updateViewLayout(this.mView, this.mLp);
    }

    public void setTouchable(boolean touchable) {
        boolean changed = false;
        if (!touchable && (this.mLp.flags & 16) == 0) {
            this.mLp.flags |= 16;
            changed = true;
        } else if (touchable && (this.mLp.flags & 16) != 0) {
            this.mLp.flags &= -17;
            changed = true;
        }
        if (!changed) {
            return;
        }
        this.mWindowManager.updateViewLayout(this.mView, this.mLp);
    }
}
