package com.android.launcher3;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.Stats;

public class Hotseat extends FrameLayout implements Stats.LaunchSourceProvider {
    private int mAllAppsButtonRank;
    private CellLayout mContent;
    private final boolean mHasVerticalHotseat;
    private Launcher mLauncher;

    public Hotseat(Context context) {
        this(context, null);
    }

    public Hotseat(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Hotseat(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mLauncher = (Launcher) context;
        this.mHasVerticalHotseat = this.mLauncher.getDeviceProfile().isVerticalBarLayout();
    }

    CellLayout getLayout() {
        return this.mContent;
    }

    @Override
    public void setOnLongClickListener(View.OnLongClickListener l) {
        this.mContent.setOnLongClickListener(l);
    }

    int getOrderInHotseat(int x, int y) {
        if (!this.mHasVerticalHotseat) {
            return x;
        }
        int x2 = (this.mContent.getCountY() - y) - 1;
        return x2;
    }

    int getCellXFromOrder(int rank) {
        if (this.mHasVerticalHotseat) {
            return 0;
        }
        return rank;
    }

    int getCellYFromOrder(int rank) {
        if (this.mHasVerticalHotseat) {
            return this.mContent.getCountY() - (rank + 1);
        }
        return 0;
    }

    public boolean isAllAppsButtonRank(int rank) {
        return rank == this.mAllAppsButtonRank;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        DeviceProfile grid = this.mLauncher.getDeviceProfile();
        this.mAllAppsButtonRank = grid.inv.hotseatAllAppsRank;
        this.mContent = (CellLayout) findViewById(R.id.layout);
        if (grid.isLandscape && !grid.isLargeTablet) {
            this.mContent.setGridSize(1, grid.inv.numHotseatIcons);
        } else {
            this.mContent.setGridSize(grid.inv.numHotseatIcons, 1);
        }
        this.mContent.setIsHotseat(true);
        resetLayout();
    }

    void resetLayout() {
        this.mContent.removeAllViewsInLayout();
        Context context = getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        TextView allAppsButton = (TextView) inflater.inflate(R.layout.all_apps_button, (ViewGroup) this.mContent, false);
        Drawable d = context.getResources().getDrawable(R.drawable.all_apps_button_icon);
        this.mLauncher.resizeIconDrawable(d);
        int scaleDownPx = getResources().getDimensionPixelSize(R.dimen.all_apps_button_scale_down);
        Rect bounds = d.getBounds();
        d.setBounds(bounds.left, bounds.top + (scaleDownPx / 2), bounds.right - scaleDownPx, bounds.bottom - (scaleDownPx / 2));
        allAppsButton.setCompoundDrawables(null, d, null, null);
        allAppsButton.setContentDescription(context.getString(R.string.all_apps_button_label));
        allAppsButton.setOnKeyListener(new HotseatIconKeyEventListener());
        if (this.mLauncher != null) {
            this.mLauncher.setAllAppsButton(allAppsButton);
            allAppsButton.setOnTouchListener(this.mLauncher.getHapticFeedbackTouchListener());
            allAppsButton.setOnClickListener(this.mLauncher);
            allAppsButton.setOnLongClickListener(this.mLauncher);
            allAppsButton.setOnFocusChangeListener(this.mLauncher.mFocusHandler);
        }
        int x = getCellXFromOrder(this.mAllAppsButtonRank);
        int y = getCellYFromOrder(this.mAllAppsButtonRank);
        CellLayout.LayoutParams lp = new CellLayout.LayoutParams(x, y, 1, 1);
        lp.canReorder = false;
        this.mContent.addViewToCellLayout(allAppsButton, -1, allAppsButton.getId(), lp, true);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (this.mLauncher.getWorkspace().workspaceInModalState()) {
            return true;
        }
        return false;
    }

    @Override
    public void fillInLaunchSourceData(View v, Bundle sourceData) {
        sourceData.putString("container", "hotseat");
    }
}
