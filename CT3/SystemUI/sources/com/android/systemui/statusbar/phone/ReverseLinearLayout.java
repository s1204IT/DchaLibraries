package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import java.util.ArrayList;

public class ReverseLinearLayout extends LinearLayout {
    private boolean mIsLayoutRtl;

    public ReverseLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mIsLayoutRtl = getResources().getConfiguration().getLayoutDirection() == 1;
    }

    @Override
    public void addView(View child) {
        reversParams(child.getLayoutParams());
        if (this.mIsLayoutRtl) {
            super.addView(child);
        } else {
            super.addView(child, 0);
        }
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        reversParams(params);
        if (this.mIsLayoutRtl) {
            super.addView(child, params);
        } else {
            super.addView(child, 0, params);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateRTLOrder();
    }

    private void updateRTLOrder() {
        boolean isLayoutRtl = getResources().getConfiguration().getLayoutDirection() == 1;
        if (this.mIsLayoutRtl == isLayoutRtl) {
            return;
        }
        int childCount = getChildCount();
        ArrayList<View> childList = new ArrayList<>(childCount);
        for (int i = 0; i < childCount; i++) {
            childList.add(getChildAt(i));
        }
        removeAllViews();
        for (int i2 = childCount - 1; i2 >= 0; i2--) {
            super.addView(childList.get(i2));
        }
        this.mIsLayoutRtl = isLayoutRtl;
    }

    private void reversParams(ViewGroup.LayoutParams params) {
        if (params == null) {
            return;
        }
        int width = params.width;
        params.width = params.height;
        params.height = width;
    }
}
