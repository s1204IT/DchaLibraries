package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import com.android.internal.policy.DividerSnapAlgorithm;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.RecentsComponent;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.plugins.statusbar.phone.NavGesture;
import com.android.systemui.stackdivider.Divider;
import com.android.systemui.stackdivider.DividerView;
import com.android.systemui.tuner.TunerService;

/* loaded from: classes.dex */
public class NavigationBarGestureHelper implements NavGesture.GestureHelper, TunerService.Tunable {
    private Context mContext;
    private Divider mDivider;
    private boolean mDockWindowEnabled;
    private boolean mDockWindowTouchSlopExceeded;
    private boolean mDownOnRecents;
    private int mDragMode;
    private boolean mIsInScreenPinning;
    private boolean mIsVertical;
    private NavigationBarView mNavigationBarView;
    private boolean mNotificationsVisibleOnDown;
    private final QuickStepController mQuickStepController;
    private RecentsComponent mRecentsComponent;
    private final int mScrollTouchSlop;
    private final StatusBar mStatusBar;
    private int mTouchDownX;
    private int mTouchDownY;
    private VelocityTracker mVelocityTracker;

    public NavigationBarGestureHelper(Context context) {
        this.mContext = context;
        this.mStatusBar = (StatusBar) SysUiServiceProvider.getComponent(context, StatusBar.class);
        this.mScrollTouchSlop = context.getResources().getDimensionPixelSize(R.dimen.navigation_bar_min_swipe_distance);
        this.mQuickStepController = new QuickStepController(context);
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "overview_nav_bar_gesture");
    }

    @Override // com.android.systemui.plugins.statusbar.phone.NavGesture.GestureHelper
    public void destroy() {
        ((TunerService) Dependency.get(TunerService.class)).removeTunable(this);
    }

    public void setComponents(RecentsComponent recentsComponent, Divider divider, NavigationBarView navigationBarView) {
        this.mRecentsComponent = recentsComponent;
        this.mDivider = divider;
        this.mNavigationBarView = navigationBarView;
        this.mQuickStepController.setComponents(this.mNavigationBarView);
    }

    @Override // com.android.systemui.plugins.statusbar.phone.NavGesture.GestureHelper
    public void setBarState(boolean z, boolean z2) {
        this.mIsVertical = z;
        this.mQuickStepController.setBarState(z, z2);
    }

    @Override // com.android.systemui.plugins.statusbar.phone.NavGesture.GestureHelper
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getActionMasked() == 0) {
            this.mIsInScreenPinning = this.mNavigationBarView.inScreenPinning();
            this.mNotificationsVisibleOnDown = !this.mStatusBar.isPresenterFullyCollapsed();
        }
        if (!canHandleGestures()) {
            return false;
        }
        boolean zOnInterceptTouchEvent = this.mQuickStepController.onInterceptTouchEvent(motionEvent);
        if (this.mDockWindowEnabled) {
            return zOnInterceptTouchEvent | interceptDockWindowEvent(motionEvent);
        }
        return zOnInterceptTouchEvent;
    }

    @Override // com.android.systemui.plugins.statusbar.phone.NavGesture.GestureHelper
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!canHandleGestures()) {
            return false;
        }
        boolean zOnTouchEvent = this.mQuickStepController.onTouchEvent(motionEvent);
        if (this.mDockWindowEnabled) {
            return zOnTouchEvent | handleDockWindowEvent(motionEvent);
        }
        return zOnTouchEvent;
    }

    @Override // com.android.systemui.plugins.statusbar.phone.NavGesture.GestureHelper
    public void onDraw(Canvas canvas) {
        this.mQuickStepController.onDraw(canvas);
    }

    @Override // com.android.systemui.plugins.statusbar.phone.NavGesture.GestureHelper
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        this.mQuickStepController.onLayout(z, i, i2, i3, i4);
    }

    @Override // com.android.systemui.plugins.statusbar.phone.NavGesture.GestureHelper
    public void onDarkIntensityChange(float f) {
        this.mQuickStepController.onDarkIntensityChange(f);
    }

    @Override // com.android.systemui.plugins.statusbar.phone.NavGesture.GestureHelper
    public void onNavigationButtonLongPress(View view) {
        this.mQuickStepController.onNavigationButtonLongPress(view);
    }

    private boolean interceptDockWindowEvent(MotionEvent motionEvent) throws Resources.NotFoundException {
        switch (motionEvent.getActionMasked()) {
            case 0:
                handleDragActionDownEvent(motionEvent);
                return false;
            case 1:
            case 3:
                handleDragActionUpEvent(motionEvent);
                return false;
            case 2:
                return handleDragActionMoveEvent(motionEvent);
            default:
                return false;
        }
    }

    private boolean handleDockWindowEvent(MotionEvent motionEvent) throws Resources.NotFoundException {
        switch (motionEvent.getActionMasked()) {
            case 0:
                handleDragActionDownEvent(motionEvent);
                break;
            case 1:
            case 3:
                handleDragActionUpEvent(motionEvent);
                break;
            case 2:
                handleDragActionMoveEvent(motionEvent);
                break;
        }
        return true;
    }

    private void handleDragActionDownEvent(MotionEvent motionEvent) {
        this.mVelocityTracker = VelocityTracker.obtain();
        this.mVelocityTracker.addMovement(motionEvent);
        boolean z = false;
        this.mDockWindowTouchSlopExceeded = false;
        this.mTouchDownX = (int) motionEvent.getX();
        this.mTouchDownY = (int) motionEvent.getY();
        if (this.mNavigationBarView != null) {
            View currentView = this.mNavigationBarView.getRecentsButton().getCurrentView();
            if (currentView != null) {
                if (this.mTouchDownX >= currentView.getLeft() && this.mTouchDownX <= currentView.getRight() && this.mTouchDownY >= currentView.getTop() && this.mTouchDownY <= currentView.getBottom()) {
                    z = true;
                }
                this.mDownOnRecents = z;
                return;
            }
            this.mDownOnRecents = false;
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:45:0x00b2  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private boolean handleDragActionMoveEvent(MotionEvent motionEvent) {
        int i;
        int rawY;
        this.mVelocityTracker.addMovement(motionEvent);
        int x = (int) motionEvent.getX();
        int y = (int) motionEvent.getY();
        int iAbs = Math.abs(x - this.mTouchDownX);
        int iAbs2 = Math.abs(y - this.mTouchDownY);
        if (this.mDivider == null || this.mRecentsComponent == null) {
            return false;
        }
        if (!this.mDockWindowTouchSlopExceeded) {
            boolean z = this.mIsVertical ? !(iAbs <= this.mScrollTouchSlop || iAbs <= iAbs2) : !(iAbs2 <= this.mScrollTouchSlop || iAbs2 <= iAbs);
            if (this.mDownOnRecents && z && this.mDivider.getView().getWindowManagerProxy().getDockSide() == -1) {
                Rect rect = null;
                int iCalculateDragMode = calculateDragMode();
                int i2 = 2;
                if (iCalculateDragMode == 1) {
                    rect = new Rect();
                    DividerView view = this.mDivider.getView();
                    if (this.mIsVertical) {
                        rawY = (int) motionEvent.getRawX();
                    } else {
                        rawY = (int) motionEvent.getRawY();
                    }
                    if (!this.mDivider.getView().isHorizontalDivision()) {
                        i2 = 1;
                    }
                    view.calculateBoundsForPosition(rawY, i2, rect);
                } else {
                    if (iCalculateDragMode == 0 && this.mTouchDownX < this.mContext.getResources().getDisplayMetrics().widthPixels / 2) {
                        i = 1;
                    }
                    if (this.mRecentsComponent.splitPrimaryTask(iCalculateDragMode, i, rect, 272)) {
                        this.mDragMode = iCalculateDragMode;
                        if (this.mDragMode == 1) {
                            this.mDivider.getView().startDragging(false, true);
                        }
                        this.mDockWindowTouchSlopExceeded = true;
                        return true;
                    }
                }
                i = 0;
                if (this.mRecentsComponent.splitPrimaryTask(iCalculateDragMode, i, rect, 272)) {
                }
            }
        } else if (this.mDragMode == 1) {
            int rawY2 = (int) (!this.mIsVertical ? motionEvent.getRawY() : motionEvent.getRawX());
            DividerSnapAlgorithm.SnapTarget snapTargetCalculateSnapTarget = this.mDivider.getView().getSnapAlgorithm().calculateSnapTarget(rawY2, 0.0f, false);
            this.mDivider.getView().resizeStack(rawY2, snapTargetCalculateSnapTarget.position, snapTargetCalculateSnapTarget);
        } else if (this.mDragMode == 0) {
            this.mRecentsComponent.onDraggingInRecents(motionEvent.getRawY());
        }
        return false;
    }

    private void handleDragActionUpEvent(MotionEvent motionEvent) throws Resources.NotFoundException {
        int rawY;
        float yVelocity;
        this.mVelocityTracker.addMovement(motionEvent);
        this.mVelocityTracker.computeCurrentVelocity(1000);
        if (this.mDockWindowTouchSlopExceeded && this.mDivider != null && this.mRecentsComponent != null) {
            if (this.mDragMode == 1) {
                DividerView view = this.mDivider.getView();
                if (this.mIsVertical) {
                    rawY = (int) motionEvent.getRawX();
                } else {
                    rawY = (int) motionEvent.getRawY();
                }
                if (this.mIsVertical) {
                    yVelocity = this.mVelocityTracker.getXVelocity();
                } else {
                    yVelocity = this.mVelocityTracker.getYVelocity();
                }
                view.stopDragging(rawY, yVelocity, true, false);
            } else if (this.mDragMode == 0) {
                this.mRecentsComponent.onDraggingInRecentsEnded(this.mVelocityTracker.getYVelocity());
            }
        }
        this.mVelocityTracker.recycle();
        this.mVelocityTracker = null;
    }

    private boolean canHandleGestures() {
        return (this.mIsInScreenPinning || this.mStatusBar.isKeyguardShowing() || this.mNotificationsVisibleOnDown) ? false : true;
    }

    private int calculateDragMode() {
        if (!this.mIsVertical || this.mDivider.getView().isHorizontalDivision()) {
            return (this.mIsVertical || !this.mDivider.getView().isHorizontalDivision()) ? 0 : 1;
        }
        return 1;
    }

    @Override // com.android.systemui.tuner.TunerService.Tunable
    public void onTuningChanged(String str, String str2) {
        boolean z = false;
        if (((str.hashCode() == 572283195 && str.equals("overview_nav_bar_gesture")) ? (char) 0 : (char) 65535) == 0) {
            if (str2 != null && Integer.parseInt(str2) != 0) {
                z = true;
            }
            this.mDockWindowEnabled = z;
        }
    }
}
