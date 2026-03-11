package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import com.android.systemui.R;

public class IconMerger extends LinearLayout {
    private int mIconHPadding;
    private int mIconSize;
    private View mMoreView;

    public IconMerger(Context context, AttributeSet attrs) {
        super(context, attrs);
        reloadDimens();
    }

    private void reloadDimens() {
        Resources res = this.mContext.getResources();
        this.mIconSize = res.getDimensionPixelSize(R.dimen.status_bar_icon_size);
        this.mIconHPadding = res.getDimensionPixelSize(R.dimen.status_bar_icon_padding);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        reloadDimens();
    }

    public void setOverflowIndicator(View v) {
        this.mMoreView = v;
    }

    private int getFullIconWidth() {
        return this.mIconSize + (this.mIconHPadding * 2);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        setMeasuredDimension(width - (width % getFullIconWidth()), getMeasuredHeight());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        checkOverflow(r - l);
    }

    private void checkOverflow(int width) {
        if (this.mMoreView == null) {
            return;
        }
        int N = getChildCount();
        int visibleChildren = 0;
        for (int i = 0; i < N; i++) {
            if (getChildAt(i).getVisibility() != 8) {
                visibleChildren++;
            }
        }
        boolean overflowShown = this.mMoreView.getVisibility() == 0;
        if (overflowShown) {
            visibleChildren--;
        }
        final boolean moreRequired = getFullIconWidth() * visibleChildren > width;
        if (moreRequired == overflowShown) {
            return;
        }
        post(new Runnable() {
            @Override
            public void run() {
                IconMerger.this.mMoreView.setVisibility(moreRequired ? 0 : 8);
            }
        });
    }
}
