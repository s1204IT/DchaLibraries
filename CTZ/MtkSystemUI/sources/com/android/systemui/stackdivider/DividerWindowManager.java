package com.android.systemui.stackdivider;

import android.content.Context;
import android.os.Binder;
import android.view.View;
import android.view.WindowManager;

public class DividerWindowManager {
    private WindowManager.LayoutParams mLp;
    private View mView;
    private final WindowManager mWindowManager;

    public DividerWindowManager(Context context) {
        this.mWindowManager = (WindowManager) context.getSystemService(WindowManager.class);
    }

    public void add(View view, int i, int i2) {
        this.mLp = new WindowManager.LayoutParams(i, i2, 2034, 545521704, -3);
        this.mLp.token = new Binder();
        this.mLp.setTitle("DockedStackDivider");
        this.mLp.privateFlags |= 64;
        this.mLp.layoutInDisplayCutoutMode = 1;
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

    public void setSlippery(boolean z) {
        boolean z2 = true;
        if (z && (this.mLp.flags & 536870912) == 0) {
            WindowManager.LayoutParams layoutParams = this.mLp;
            layoutParams.flags = 536870912 | layoutParams.flags;
        } else if (!z && (this.mLp.flags & 536870912) != 0) {
            this.mLp.flags &= -536870913;
        } else {
            z2 = false;
        }
        if (z2) {
            this.mWindowManager.updateViewLayout(this.mView, this.mLp);
        }
    }

    public void setTouchable(boolean z) {
        boolean z2 = true;
        if (!z && (this.mLp.flags & 16) == 0) {
            this.mLp.flags |= 16;
        } else if (z && (this.mLp.flags & 16) != 0) {
            this.mLp.flags &= -17;
        } else {
            z2 = false;
        }
        if (z2) {
            this.mWindowManager.updateViewLayout(this.mView, this.mLp);
        }
    }
}
