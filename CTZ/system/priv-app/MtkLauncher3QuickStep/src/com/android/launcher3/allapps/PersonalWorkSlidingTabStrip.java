package com.android.launcher3.allapps;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.pageindicators.PageIndicator;
import com.android.launcher3.util.Themes;

/* loaded from: classes.dex */
public class PersonalWorkSlidingTabStrip extends LinearLayout implements PageIndicator {
    private static final String KEY_SHOWED_PEEK_WORK_TAB = "showed_peek_work_tab";
    private static final int POSITION_PERSONAL = 0;
    private static final int POSITION_WORK = 1;
    private AllAppsContainerView mContainerView;
    private final Paint mDividerPaint;
    private int mIndicatorLeft;
    private int mIndicatorRight;
    private boolean mIsRtl;
    private int mLastActivePage;
    private float mScrollOffset;
    private int mSelectedIndicatorHeight;
    private final Paint mSelectedIndicatorPaint;
    private int mSelectedPosition;
    private final SharedPreferences mSharedPreferences;

    public PersonalWorkSlidingTabStrip(@NonNull Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mIndicatorLeft = -1;
        this.mIndicatorRight = -1;
        this.mSelectedPosition = 0;
        this.mLastActivePage = 0;
        setOrientation(0);
        setWillNotDraw(false);
        this.mSelectedIndicatorHeight = getResources().getDimensionPixelSize(R.dimen.all_apps_tabs_indicator_height);
        this.mSelectedIndicatorPaint = new Paint();
        this.mSelectedIndicatorPaint.setColor(Themes.getAttrColor(context, android.R.attr.colorAccent));
        this.mDividerPaint = new Paint();
        this.mDividerPaint.setColor(Themes.getAttrColor(context, android.R.attr.colorControlHighlight));
        this.mDividerPaint.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.all_apps_divider_height));
        this.mSharedPreferences = Launcher.getLauncher(getContext()).getSharedPrefs();
        this.mIsRtl = Utilities.isRtl(getResources());
    }

    private void updateIndicatorPosition(float f) {
        this.mScrollOffset = f;
        updateIndicatorPosition();
    }

    private void updateTabTextColor(int i) {
        this.mSelectedPosition = i;
        int i2 = 0;
        while (i2 < getChildCount()) {
            ((Button) getChildAt(i2)).setSelected(i2 == i);
            i2++;
        }
    }

    @Override // android.widget.LinearLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        updateTabTextColor(this.mSelectedPosition);
        updateIndicatorPosition(this.mScrollOffset);
    }

    private void updateIndicatorPosition() {
        int width;
        View leftTab = getLeftTab();
        int left = -1;
        if (leftTab != null) {
            left = (int) (leftTab.getLeft() + (leftTab.getWidth() * this.mScrollOffset));
            width = leftTab.getWidth() + left;
        } else {
            width = -1;
        }
        setIndicatorPosition(left, width);
    }

    private View getLeftTab() {
        return getChildAt(this.mIsRtl ? 1 : 0);
    }

    private void setIndicatorPosition(int i, int i2) {
        if (i != this.mIndicatorLeft || i2 != this.mIndicatorRight) {
            this.mIndicatorLeft = i;
            this.mIndicatorRight = i2;
            invalidate();
        }
    }

    @Override // android.widget.LinearLayout, android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float height = getHeight() - this.mDividerPaint.getStrokeWidth();
        canvas.drawLine(getPaddingLeft(), height, getWidth() - getPaddingRight(), height, this.mDividerPaint);
        canvas.drawRect(this.mIndicatorLeft, getHeight() - this.mSelectedIndicatorHeight, this.mIndicatorRight, getHeight(), this.mSelectedIndicatorPaint);
    }

    public void highlightWorkTabIfNecessary() {
        if (this.mSharedPreferences.getBoolean(KEY_SHOWED_PEEK_WORK_TAB, false) || this.mLastActivePage != 0) {
            return;
        }
        highlightWorkTab();
        this.mSharedPreferences.edit().putBoolean(KEY_SHOWED_PEEK_WORK_TAB, true).apply();
    }

    private void highlightWorkTab() {
        final View childAt = getChildAt(1);
        childAt.post(new Runnable() { // from class: com.android.launcher3.allapps.-$$Lambda$PersonalWorkSlidingTabStrip$J-TkCQjnY8hDIMkKUDDYAg9supI
            @Override // java.lang.Runnable
            public final void run() {
                PersonalWorkSlidingTabStrip.lambda$highlightWorkTab$0(childAt);
            }
        });
    }

    static /* synthetic */ void lambda$highlightWorkTab$0(View view) {
        view.setPressed(true);
        view.setPressed(false);
    }

    @Override // com.android.launcher3.pageindicators.PageIndicator
    public void setScroll(int i, int i2) {
        updateIndicatorPosition(i / i2);
    }

    @Override // com.android.launcher3.pageindicators.PageIndicator
    public void setActiveMarker(int i) {
        updateTabTextColor(i);
        if (this.mContainerView != null && this.mLastActivePage != i) {
            this.mContainerView.onTabChanged(i);
        }
        this.mLastActivePage = i;
    }

    public void setContainerView(AllAppsContainerView allAppsContainerView) {
        this.mContainerView = allAppsContainerView;
    }

    @Override // com.android.launcher3.pageindicators.PageIndicator
    public void setMarkersCount(int i) {
    }

    @Override // android.view.View
    public boolean hasOverlappingRendering() {
        return false;
    }
}
