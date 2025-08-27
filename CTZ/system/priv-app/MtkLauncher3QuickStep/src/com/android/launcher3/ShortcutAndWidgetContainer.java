package com.android.launcher3;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import com.android.launcher3.CellLayout;
import com.android.launcher3.widget.LauncherAppWidgetHostView;

/* loaded from: classes.dex */
public class ShortcutAndWidgetContainer extends ViewGroup {
    static final String TAG = "ShortcutAndWidgetContainer";
    private int mCellHeight;
    private int mCellWidth;
    private final int mContainerType;
    private int mCountX;
    private boolean mInvertIfRtl;
    private Launcher mLauncher;
    private final int[] mTmpCellXY;
    private final WallpaperManager mWallpaperManager;

    public ShortcutAndWidgetContainer(Context context, int i) {
        super(context);
        this.mTmpCellXY = new int[2];
        this.mInvertIfRtl = false;
        this.mLauncher = Launcher.getLauncher(context);
        this.mWallpaperManager = WallpaperManager.getInstance(context);
        this.mContainerType = i;
    }

    public void setCellDimensions(int i, int i2, int i3, int i4) {
        this.mCellWidth = i;
        this.mCellHeight = i2;
        this.mCountX = i3;
    }

    public View getChildAt(int i, int i2) {
        int childCount = getChildCount();
        for (int i3 = 0; i3 < childCount; i3++) {
            View childAt = getChildAt(i3);
            CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) childAt.getLayoutParams();
            if (layoutParams.cellX <= i && i < layoutParams.cellX + layoutParams.cellHSpan && layoutParams.cellY <= i2 && i2 < layoutParams.cellY + layoutParams.cellVSpan) {
                return childAt;
            }
        }
        return null;
    }

    @Override // android.view.View
    protected void onMeasure(int i, int i2) {
        int childCount = getChildCount();
        setMeasuredDimension(View.MeasureSpec.getSize(i), View.MeasureSpec.getSize(i2));
        for (int i3 = 0; i3 < childCount; i3++) {
            View childAt = getChildAt(i3);
            if (childAt.getVisibility() != 8) {
                measureChild(childAt);
            }
        }
    }

    public void setupLp(View view) {
        CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) view.getLayoutParams();
        if (view instanceof LauncherAppWidgetHostView) {
            DeviceProfile deviceProfile = this.mLauncher.getDeviceProfile();
            layoutParams.setup(this.mCellWidth, this.mCellHeight, invertLayoutHorizontally(), this.mCountX, deviceProfile.appWidgetScale.x, deviceProfile.appWidgetScale.y);
        } else {
            layoutParams.setup(this.mCellWidth, this.mCellHeight, invertLayoutHorizontally(), this.mCountX);
        }
    }

    public void setInvertIfRtl(boolean z) {
        this.mInvertIfRtl = z;
    }

    public int getCellContentHeight() {
        return Math.min(getMeasuredHeight(), this.mLauncher.getDeviceProfile().getCellHeight(this.mContainerType));
    }

    public void measureChild(View view) {
        int i;
        CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) view.getLayoutParams();
        DeviceProfile deviceProfile = this.mLauncher.getDeviceProfile();
        if (view instanceof LauncherAppWidgetHostView) {
            layoutParams.setup(this.mCellWidth, this.mCellHeight, invertLayoutHorizontally(), this.mCountX, deviceProfile.appWidgetScale.x, deviceProfile.appWidgetScale.y);
        } else {
            layoutParams.setup(this.mCellWidth, this.mCellHeight, invertLayoutHorizontally(), this.mCountX);
            int iMax = (int) Math.max(0.0f, (layoutParams.height - getCellContentHeight()) / 2.0f);
            if (this.mContainerType == 0) {
                i = deviceProfile.workspaceCellPaddingXPx;
            } else {
                i = (int) (deviceProfile.edgeMarginPx / 2.0f);
            }
            view.setPadding(i, iMax, i, 0);
        }
        view.measure(View.MeasureSpec.makeMeasureSpec(layoutParams.width, 1073741824), View.MeasureSpec.makeMeasureSpec(layoutParams.height, 1073741824));
    }

    public boolean invertLayoutHorizontally() {
        return this.mInvertIfRtl && Utilities.isRtl(getResources());
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int childCount = getChildCount();
        for (int i5 = 0; i5 < childCount; i5++) {
            View childAt = getChildAt(i5);
            if (childAt.getVisibility() != 8) {
                CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) childAt.getLayoutParams();
                if (childAt instanceof LauncherAppWidgetHostView) {
                    LauncherAppWidgetHostView launcherAppWidgetHostView = (LauncherAppWidgetHostView) childAt;
                    DeviceProfile deviceProfile = this.mLauncher.getDeviceProfile();
                    float f = deviceProfile.appWidgetScale.x;
                    float f2 = deviceProfile.appWidgetScale.y;
                    launcherAppWidgetHostView.setScaleToFit(Math.min(f, f2));
                    launcherAppWidgetHostView.setTranslationForCentering((-(layoutParams.width - (layoutParams.width * f))) / 2.0f, (-(layoutParams.height - (layoutParams.height * f2))) / 2.0f);
                }
                int i6 = layoutParams.x;
                int i7 = layoutParams.y;
                childAt.layout(i6, i7, layoutParams.width + i6, layoutParams.height + i7);
                if (layoutParams.dropped) {
                    layoutParams.dropped = false;
                    int[] iArr = this.mTmpCellXY;
                    getLocationOnScreen(iArr);
                    this.mWallpaperManager.sendWallpaperCommand(getWindowToken(), "android.home.drop", iArr[0] + i6 + (layoutParams.width / 2), iArr[1] + i7 + (layoutParams.height / 2), 0, null);
                }
            }
        }
    }

    @Override // android.view.ViewGroup
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == 0 && getAlpha() == 0.0f) {
            return true;
        }
        return super.onInterceptTouchEvent(motionEvent);
    }

    @Override // android.view.ViewGroup
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override // android.view.ViewGroup, android.view.ViewParent
    public void requestChildFocus(View view, View view2) {
        super.requestChildFocus(view, view2);
        if (view != null) {
            Rect rect = new Rect();
            view.getDrawingRect(rect);
            requestRectangleOnScreen(rect);
        }
    }

    @Override // android.view.View
    public void cancelLongPress() {
        super.cancelLongPress();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            getChildAt(i).cancelLongPress();
        }
    }
}
