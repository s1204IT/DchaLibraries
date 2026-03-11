package com.android.launcher3;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.InsettableFrameLayout;
import com.android.launcher3.accessibility.LauncherAccessibilityDelegate;
import com.android.launcher3.compat.PackageInstallerCompat;
import com.mediatek.launcher3.LauncherLog;
import java.util.ArrayList;

public class DragLayer extends InsettableFrameLayout {
    View mAnchorView;
    int mAnchorViewInitialScrollX;
    private float mBackgroundAlpha;
    private int mChildCountOnLastUpdate;
    private final TimeInterpolator mCubicEaseOutInterpolator;
    private AppWidgetResizeFrame mCurrentResizeFrame;
    DragController mDragController;
    private ValueAnimator mDropAnim;
    DragView mDropView;
    private final Rect mHitRect;
    private boolean mHoverPointClosesFolder;
    private boolean mInScrollArea;
    private final boolean mIsRtl;
    private Launcher mLauncher;
    private Drawable mLeftHoverDrawable;
    private Drawable mLeftHoverDrawableActive;
    private View mOverlayView;
    private final ArrayList<AppWidgetResizeFrame> mResizeFrames;
    private Drawable mRightHoverDrawable;
    private Drawable mRightHoverDrawableActive;
    private final Rect mScrollChildPosition;
    private boolean mShowPageHints;
    private final int[] mTmpXY;
    private int mTopViewIndex;
    private TouchCompleteListener mTouchCompleteListener;
    private int mXDown;
    private int mYDown;

    public interface TouchCompleteListener {
        void onTouchComplete();
    }

    public DragLayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mTmpXY = new int[2];
        this.mResizeFrames = new ArrayList<>();
        this.mDropAnim = null;
        this.mCubicEaseOutInterpolator = new DecelerateInterpolator(1.5f);
        this.mDropView = null;
        this.mAnchorViewInitialScrollX = 0;
        this.mAnchorView = null;
        this.mHoverPointClosesFolder = false;
        this.mHitRect = new Rect();
        this.mChildCountOnLastUpdate = -1;
        this.mBackgroundAlpha = 0.0f;
        this.mScrollChildPosition = new Rect();
        setMotionEventSplittingEnabled(false);
        setChildrenDrawingOrderEnabled(true);
        Resources res = getResources();
        this.mLeftHoverDrawable = res.getDrawable(R.drawable.page_hover_left);
        this.mRightHoverDrawable = res.getDrawable(R.drawable.page_hover_right);
        this.mLeftHoverDrawableActive = res.getDrawable(R.drawable.page_hover_left_active);
        this.mRightHoverDrawableActive = res.getDrawable(R.drawable.page_hover_right_active);
        this.mIsRtl = Utilities.isRtl(res);
    }

    public void setup(Launcher launcher, DragController controller) {
        this.mLauncher = launcher;
        this.mDragController = controller;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean zDispatchKeyEvent = !this.mDragController.dispatchKeyEvent(event) ? super.dispatchKeyEvent(event) : true;
        if (LauncherLog.DEBUG_KEY) {
            LauncherLog.d("DragLayer", "dispatchKeyEvent: keycode = " + event.getKeyCode() + ", action = " + event.getAction() + ", handled = " + zDispatchKeyEvent);
        }
        return zDispatchKeyEvent;
    }

    public void showOverlayView(View overlayView) {
        LayoutParams lp = new LayoutParams(-1, -1);
        this.mOverlayView = overlayView;
        addView(overlayView, lp);
        this.mOverlayView.bringToFront();
    }

    private boolean isEventOverFolderTextRegion(Folder folder, MotionEvent ev) {
        getDescendantRectRelativeToSelf(folder.getEditTextRegion(), this.mHitRect);
        if (this.mHitRect.contains((int) ev.getX(), (int) ev.getY())) {
            return true;
        }
        return false;
    }

    private boolean isEventOverFolder(Folder folder, MotionEvent ev) {
        getDescendantRectRelativeToSelf(folder, this.mHitRect);
        if (this.mHitRect.contains((int) ev.getX(), (int) ev.getY())) {
            return true;
        }
        return false;
    }

    private boolean isEventOverDropTargetBar(MotionEvent ev) {
        getDescendantRectRelativeToSelf(this.mLauncher.getSearchDropTargetBar(), this.mHitRect);
        if (this.mHitRect.contains((int) ev.getX(), (int) ev.getY())) {
            return true;
        }
        return false;
    }

    private boolean handleTouchDown(MotionEvent ev, boolean intercept) {
        Rect hitRect = new Rect();
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        if (LauncherLog.DEBUG_MOTION) {
            LauncherLog.d("DragLayer", "handleTouchDown: x = " + x + ", y = " + y + ", intercept = " + intercept + ", mXDown = " + this.mXDown + ", mYDown = " + this.mYDown);
        }
        for (AppWidgetResizeFrame child : this.mResizeFrames) {
            child.getHitRect(hitRect);
            if (hitRect.contains(x, y) && child.beginResizeIfPointInRegion(x - child.getLeft(), y - child.getTop())) {
                this.mCurrentResizeFrame = child;
                this.mXDown = x;
                this.mYDown = y;
                requestDisallowInterceptTouchEvent(true);
                return true;
            }
        }
        Folder currentFolder = this.mLauncher.getWorkspace().getOpenFolder();
        if (currentFolder == null || !intercept) {
            return false;
        }
        if (currentFolder.isEditingName() && !isEventOverFolderTextRegion(currentFolder, ev)) {
            currentFolder.dismissEditingName();
            return true;
        }
        if (isEventOverFolder(currentFolder, ev)) {
            return false;
        }
        if (isInAccessibleDrag()) {
            return !isEventOverDropTargetBar(ev);
        }
        this.mLauncher.closeFolder();
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (LauncherLog.DEBUG_MOTION) {
            LauncherLog.d("DragLayer", "onInterceptTouchEvent: action = " + ev.getAction() + ", x = " + ev.getX() + ", y = " + ev.getY());
        }
        int action = ev.getAction();
        if (action == 0) {
            if (handleTouchDown(ev, true)) {
                return true;
            }
        } else if (action == 1 || action == 3) {
            if (this.mTouchCompleteListener != null) {
                this.mTouchCompleteListener.onTouchComplete();
            }
            this.mTouchCompleteListener = null;
        }
        clearAllResizeFrames();
        return this.mDragController.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onInterceptHoverEvent(MotionEvent ev) {
        Folder currentFolder;
        boolean isOverFolderOrSearchBar;
        boolean isOverFolderOrSearchBar2;
        if (this.mLauncher == null || this.mLauncher.getWorkspace() == null || (currentFolder = this.mLauncher.getWorkspace().getOpenFolder()) == null) {
            return false;
        }
        AccessibilityManager accessibilityManager = (AccessibilityManager) getContext().getSystemService("accessibility");
        if (accessibilityManager.isTouchExplorationEnabled()) {
            int action = ev.getAction();
            switch (action) {
                case 7:
                    if (isEventOverFolder(currentFolder, ev)) {
                        isOverFolderOrSearchBar = true;
                    } else {
                        isOverFolderOrSearchBar = isInAccessibleDrag() ? isEventOverDropTargetBar(ev) : false;
                    }
                    if (!isOverFolderOrSearchBar && !this.mHoverPointClosesFolder) {
                        sendTapOutsideFolderAccessibilityEvent(currentFolder.isEditingName());
                        this.mHoverPointClosesFolder = true;
                        return true;
                    }
                    if (!isOverFolderOrSearchBar) {
                        return true;
                    }
                    this.mHoverPointClosesFolder = false;
                    break;
                    break;
                case 9:
                    if (isEventOverFolder(currentFolder, ev)) {
                        isOverFolderOrSearchBar2 = true;
                    } else {
                        isOverFolderOrSearchBar2 = isInAccessibleDrag() ? isEventOverDropTargetBar(ev) : false;
                    }
                    if (!isOverFolderOrSearchBar2) {
                        sendTapOutsideFolderAccessibilityEvent(currentFolder.isEditingName());
                        this.mHoverPointClosesFolder = true;
                        return true;
                    }
                    this.mHoverPointClosesFolder = false;
                    break;
                    break;
            }
        }
        return false;
    }

    private void sendTapOutsideFolderAccessibilityEvent(boolean isEditingName) {
        AccessibilityManager accessibilityManager = (AccessibilityManager) getContext().getSystemService("accessibility");
        if (!accessibilityManager.isEnabled()) {
            return;
        }
        int stringId = isEditingName ? R.string.folder_tap_to_rename : R.string.folder_tap_to_close;
        AccessibilityEvent event = AccessibilityEvent.obtain(8);
        onInitializeAccessibilityEvent(event);
        event.getText().add(getContext().getString(stringId));
        accessibilityManager.sendAccessibilityEvent(event);
    }

    private boolean isInAccessibleDrag() {
        LauncherAccessibilityDelegate delegate = LauncherAppState.getInstance().getAccessibilityDelegate();
        if (delegate != null) {
            return delegate.isInAccessibleDrag();
        }
        return false;
    }

    @Override
    public boolean onRequestSendAccessibilityEvent(View child, AccessibilityEvent event) {
        Folder currentFolder = this.mLauncher.getWorkspace().getOpenFolder();
        if (currentFolder != null) {
            if (child == currentFolder) {
                return super.onRequestSendAccessibilityEvent(child, event);
            }
            if (isInAccessibleDrag() && (child instanceof SearchDropTargetBar)) {
                return super.onRequestSendAccessibilityEvent(child, event);
            }
            return false;
        }
        return super.onRequestSendAccessibilityEvent(child, event);
    }

    @Override
    public void addChildrenForAccessibility(ArrayList<View> childrenForAccessibility) {
        Folder currentFolder = this.mLauncher.getWorkspace().getOpenFolder();
        if (currentFolder != null) {
            childrenForAccessibility.add(currentFolder);
            if (!isInAccessibleDrag()) {
                return;
            }
            childrenForAccessibility.add(this.mLauncher.getSearchDropTargetBar());
            return;
        }
        super.addChildrenForAccessibility(childrenForAccessibility);
    }

    @Override
    public boolean onHoverEvent(MotionEvent ev) {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean handled = false;
        int action = ev.getAction();
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        if (action == 1) {
            LauncherLog.d("DragLayer", "[PerfTest --> drag widget] start process");
        }
        if (LauncherLog.DEBUG_MOTION) {
            LauncherLog.d("DragLayer", "onTouchEvent: action = " + action + ", x = " + x + ", y = " + y + ", mXDown = " + this.mXDown + ", mYDown = " + this.mYDown + ", mCurrentResizeFrame = " + this.mCurrentResizeFrame);
        }
        if (action == 0) {
            if (handleTouchDown(ev, false)) {
                return true;
            }
        } else if (action == 1 || action == 3) {
            if (this.mTouchCompleteListener != null) {
                this.mTouchCompleteListener.onTouchComplete();
            }
            this.mTouchCompleteListener = null;
        }
        if (this.mCurrentResizeFrame != null) {
            handled = true;
            switch (action) {
                case PackageInstallerCompat.STATUS_INSTALLING:
                case 3:
                    this.mCurrentResizeFrame.visualizeResizeForDelta(x - this.mXDown, y - this.mYDown);
                    this.mCurrentResizeFrame.onTouchUp();
                    this.mCurrentResizeFrame = null;
                    break;
                case PackageInstallerCompat.STATUS_FAILED:
                    this.mCurrentResizeFrame.visualizeResizeForDelta(x - this.mXDown, y - this.mYDown);
                    break;
            }
        }
        if (handled) {
            return true;
        }
        return this.mDragController.onTouchEvent(ev);
    }

    public float getDescendantRectRelativeToSelf(View descendant, Rect r) {
        this.mTmpXY[0] = 0;
        this.mTmpXY[1] = 0;
        float scale = getDescendantCoordRelativeToSelf(descendant, this.mTmpXY);
        r.set(this.mTmpXY[0], this.mTmpXY[1], (int) (this.mTmpXY[0] + (descendant.getMeasuredWidth() * scale)), (int) (this.mTmpXY[1] + (descendant.getMeasuredHeight() * scale)));
        return scale;
    }

    public float getLocationInDragLayer(View child, int[] loc) {
        loc[0] = 0;
        loc[1] = 0;
        return getDescendantCoordRelativeToSelf(child, loc);
    }

    public float getDescendantCoordRelativeToSelf(View descendant, int[] coord) {
        return getDescendantCoordRelativeToSelf(descendant, coord, false);
    }

    public float getDescendantCoordRelativeToSelf(View descendant, int[] coord, boolean includeRootScroll) {
        return Utilities.getDescendantCoordRelativeToParent(descendant, this, coord, includeRootScroll);
    }

    public float mapCoordInSelfToDescendent(View descendant, int[] coord) {
        return Utilities.mapCoordInSelfToDescendent(descendant, this, coord);
    }

    public void getViewRectRelativeToSelf(View v, Rect r) {
        int[] loc = new int[2];
        getLocationInWindow(loc);
        int x = loc[0];
        int y = loc[1];
        v.getLocationInWindow(loc);
        int vX = loc[0];
        int vY = loc[1];
        int left = vX - x;
        int top = vY - y;
        r.set(left, top, v.getMeasuredWidth() + left, v.getMeasuredHeight() + top);
    }

    @Override
    public boolean dispatchUnhandledMove(View focused, int direction) {
        if (LauncherLog.DEBUG_KEY) {
            LauncherLog.d("DragLayer", "dispatchUnhandledMove: focused = " + focused + ", direction = " + direction);
        }
        return this.mDragController.dispatchUnhandledMove(focused, direction);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    public LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-2, -2);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    public LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    public static class LayoutParams extends InsettableFrameLayout.LayoutParams {
        public boolean customPosition;
        public int x;
        public int y;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            this.customPosition = false;
        }

        public LayoutParams(int width, int height) {
            super(width, height);
            this.customPosition = false;
        }

        public LayoutParams(ViewGroup.LayoutParams lp) {
            super(lp);
            this.customPosition = false;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getWidth() {
            return this.width;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getHeight() {
            return this.height;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getX() {
            return this.x;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getY() {
            return this.y;
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) child.getLayoutParams();
            if (flp instanceof LayoutParams) {
                LayoutParams lp = (LayoutParams) flp;
                if (lp.customPosition) {
                    child.layout(lp.x, lp.y, lp.x + lp.width, lp.y + lp.height);
                }
            }
        }
    }

    public void clearAllResizeFrames() {
        if (this.mResizeFrames.size() <= 0) {
            return;
        }
        if (LauncherLog.DEBUG_DRAG) {
            LauncherLog.d("DragLayer", "clearAllResizeFrames: mResizeFrames size = " + this.mResizeFrames.size());
        }
        for (AppWidgetResizeFrame frame : this.mResizeFrames) {
            frame.commitResize();
            removeView(frame);
        }
        this.mResizeFrames.clear();
    }

    public void addResizeFrame(ItemInfo itemInfo, LauncherAppWidgetHostView widget, CellLayout cellLayout) {
        AppWidgetResizeFrame resizeFrame = new AppWidgetResizeFrame(getContext(), widget, cellLayout, this);
        if (LauncherLog.DEBUG) {
            LauncherLog.d("DragLayer", "addResizeFrame: itemInfo = " + itemInfo + ", widget = " + widget + ", resizeFrame = " + resizeFrame);
        }
        LayoutParams lp = new LayoutParams(-1, -1);
        lp.customPosition = true;
        addView(resizeFrame, lp);
        this.mResizeFrames.add(resizeFrame);
        resizeFrame.snapToWidget(false);
    }

    public void animateViewIntoPosition(DragView dragView, int[] pos, float alpha, float scaleX, float scaleY, int animationEndStyle, Runnable onFinishRunnable, int duration) {
        Rect r = new Rect();
        getViewRectRelativeToSelf(dragView, r);
        int fromX = r.left;
        int fromY = r.top;
        animateViewIntoPosition(dragView, fromX, fromY, pos[0], pos[1], alpha, 1.0f, 1.0f, scaleX, scaleY, onFinishRunnable, animationEndStyle, duration, null);
    }

    public void animateViewIntoPosition(DragView dragView, View child, Runnable onFinishAnimationRunnable, View anchorView) {
        animateViewIntoPosition(dragView, child, -1, onFinishAnimationRunnable, anchorView);
    }

    public void animateViewIntoPosition(DragView dragView, final View child, int duration, final Runnable onFinishAnimationRunnable, View anchorView) {
        int toY;
        int toX;
        ShortcutAndWidgetContainer parentChildren = (ShortcutAndWidgetContainer) child.getParent();
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
        parentChildren.measureChild(child);
        Rect r = new Rect();
        getViewRectRelativeToSelf(dragView, r);
        if (LauncherLog.DEBUG) {
            LauncherLog.d("DragLayer", "animateViewIntoPosition: dragView = " + dragView + ", r = " + r + ", lp.x = " + lp.x + ", lp.y = " + lp.y);
        }
        float childScale = child.getScaleX();
        int[] coord = {lp.x + ((int) ((child.getMeasuredWidth() * (1.0f - childScale)) / 2.0f)), lp.y + ((int) ((child.getMeasuredHeight() * (1.0f - childScale)) / 2.0f))};
        float scale = getDescendantCoordRelativeToSelf((View) child.getParent(), coord) * childScale;
        int toX2 = coord[0];
        int toY2 = coord[1];
        float toScale = scale;
        if (child instanceof TextView) {
            TextView tv = (TextView) child;
            toScale = scale / dragView.getIntrinsicIconScaleFactor();
            toY = (int) ((toY2 + Math.round(tv.getPaddingTop() * toScale)) - ((dragView.getMeasuredHeight() * (1.0f - toScale)) / 2.0f));
            if (dragView.getDragVisualizeOffset() != null) {
                toY -= Math.round(dragView.getDragVisualizeOffset().y * toScale);
            }
            toX = toX2 - ((dragView.getMeasuredWidth() - Math.round(child.getMeasuredWidth() * scale)) / 2);
        } else if (child instanceof FolderIcon) {
            toY = (int) (((int) ((toY2 + Math.round((child.getPaddingTop() - dragView.getDragRegionTop()) * scale)) - ((2.0f * scale) / 2.0f))) - (((1.0f - scale) * dragView.getMeasuredHeight()) / 2.0f));
            toX = toX2 - ((dragView.getMeasuredWidth() - Math.round(child.getMeasuredWidth() * scale)) / 2);
        } else {
            toY = toY2 - (Math.round((dragView.getHeight() - child.getMeasuredHeight()) * scale) / 2);
            toX = toX2 - (Math.round((dragView.getMeasuredWidth() - child.getMeasuredWidth()) * scale) / 2);
        }
        int fromX = r.left;
        int fromY = r.top;
        child.setVisibility(4);
        Runnable onCompleteRunnable = new Runnable() {
            @Override
            public void run() {
                child.setVisibility(0);
                if (onFinishAnimationRunnable == null) {
                    return;
                }
                onFinishAnimationRunnable.run();
            }
        };
        animateViewIntoPosition(dragView, fromX, fromY, toX, toY, 1.0f, 1.0f, 1.0f, toScale, toScale, onCompleteRunnable, 0, duration, anchorView);
    }

    public void animateViewIntoPosition(DragView view, int fromX, int fromY, int toX, int toY, float finalAlpha, float initScaleX, float initScaleY, float finalScaleX, float finalScaleY, Runnable onCompleteRunnable, int animationEndStyle, int duration, View anchorView) {
        Rect from = new Rect(fromX, fromY, view.getMeasuredWidth() + fromX, view.getMeasuredHeight() + fromY);
        Rect to = new Rect(toX, toY, view.getMeasuredWidth() + toX, view.getMeasuredHeight() + toY);
        animateView(view, from, to, finalAlpha, initScaleX, initScaleY, finalScaleX, finalScaleY, duration, null, null, onCompleteRunnable, animationEndStyle, anchorView);
    }

    public void animateView(final DragView view, final Rect from, final Rect to, final float finalAlpha, final float initScaleX, final float initScaleY, final float finalScaleX, final float finalScaleY, int duration, final Interpolator motionInterpolator, final Interpolator alphaInterpolator, Runnable onCompleteRunnable, int animationEndStyle, View anchorView) {
        float dist = (float) Math.hypot(to.left - from.left, to.top - from.top);
        Resources res = getResources();
        float maxDist = res.getInteger(R.integer.config_dropAnimMaxDist);
        if (duration < 0) {
            int duration2 = res.getInteger(R.integer.config_dropAnimMaxDuration);
            if (dist < maxDist) {
                duration2 = (int) (duration2 * this.mCubicEaseOutInterpolator.getInterpolation(dist / maxDist));
            }
            duration = Math.max(duration2, res.getInteger(R.integer.config_dropAnimMinDuration));
        }
        TimeInterpolator interpolator = null;
        if (alphaInterpolator == null || motionInterpolator == null) {
            interpolator = this.mCubicEaseOutInterpolator;
        }
        final float initAlpha = view.getAlpha();
        final float dropViewScale = view.getScaleX();
        ValueAnimator.AnimatorUpdateListener updateCb = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float percent = ((Float) animation.getAnimatedValue()).floatValue();
                int width = view.getMeasuredWidth();
                int height = view.getMeasuredHeight();
                float alphaPercent = alphaInterpolator == null ? percent : alphaInterpolator.getInterpolation(percent);
                float motionPercent = motionInterpolator == null ? percent : motionInterpolator.getInterpolation(percent);
                float initialScaleX = initScaleX * dropViewScale;
                float initialScaleY = initScaleY * dropViewScale;
                float scaleX = (finalScaleX * percent) + ((1.0f - percent) * initialScaleX);
                float scaleY = (finalScaleY * percent) + ((1.0f - percent) * initialScaleY);
                float alpha = (finalAlpha * alphaPercent) + (initAlpha * (1.0f - alphaPercent));
                float fromLeft = from.left + (((initialScaleX - 1.0f) * width) / 2.0f);
                float fromTop = from.top + (((initialScaleY - 1.0f) * height) / 2.0f);
                int x = (int) (Math.round((to.left - fromLeft) * motionPercent) + fromLeft);
                int y = (int) (Math.round((to.top - fromTop) * motionPercent) + fromTop);
                int anchorAdjust = DragLayer.this.mAnchorView == null ? 0 : (int) (DragLayer.this.mAnchorView.getScaleX() * (DragLayer.this.mAnchorViewInitialScrollX - DragLayer.this.mAnchorView.getScrollX()));
                int xPos = (x - DragLayer.this.mDropView.getScrollX()) + anchorAdjust;
                int yPos = y - DragLayer.this.mDropView.getScrollY();
                DragLayer.this.mDropView.setTranslationX(xPos);
                DragLayer.this.mDropView.setTranslationY(yPos);
                DragLayer.this.mDropView.setScaleX(scaleX);
                DragLayer.this.mDropView.setScaleY(scaleY);
                DragLayer.this.mDropView.setAlpha(alpha);
            }
        };
        animateView(view, updateCb, duration, interpolator, onCompleteRunnable, animationEndStyle, anchorView);
    }

    public void animateView(DragView view, ValueAnimator.AnimatorUpdateListener updateCb, int duration, TimeInterpolator interpolator, final Runnable onCompleteRunnable, final int animationEndStyle, View anchorView) {
        if (this.mDropAnim != null) {
            this.mDropAnim.cancel();
        }
        this.mDropView = view;
        this.mDropView.cancelAnimation();
        this.mDropView.resetLayoutParams();
        if (anchorView != null) {
            this.mAnchorViewInitialScrollX = anchorView.getScrollX();
        }
        this.mAnchorView = anchorView;
        this.mDropAnim = new ValueAnimator();
        this.mDropAnim.setInterpolator(interpolator);
        this.mDropAnim.setDuration(duration);
        this.mDropAnim.setFloatValues(0.0f, 1.0f);
        this.mDropAnim.addUpdateListener(updateCb);
        this.mDropAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (onCompleteRunnable != null) {
                    onCompleteRunnable.run();
                }
                switch (animationEndStyle) {
                    case PackageInstallerCompat.STATUS_INSTALLED:
                        DragLayer.this.clearAnimatedView();
                        break;
                }
            }
        });
        this.mDropAnim.start();
    }

    public void clearAnimatedView() {
        if (this.mDropAnim != null) {
            this.mDropAnim.cancel();
        }
        if (this.mDropView != null) {
            this.mDragController.onDeferredEndDrag(this.mDropView);
        }
        this.mDropView = null;
        invalidate();
    }

    public View getAnimatedView() {
        return this.mDropView;
    }

    @Override
    public void onChildViewAdded(View parent, View child) {
        super.onChildViewAdded(parent, child);
        if (this.mOverlayView != null) {
            this.mOverlayView.bringToFront();
        }
        updateChildIndices();
    }

    @Override
    public void onChildViewRemoved(View parent, View child) {
        updateChildIndices();
    }

    @Override
    public void bringChildToFront(View child) {
        super.bringChildToFront(child);
        if (child != this.mOverlayView && this.mOverlayView != null) {
            this.mOverlayView.bringToFront();
        }
        updateChildIndices();
    }

    private void updateChildIndices() {
        this.mTopViewIndex = -1;
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            if (getChildAt(i) instanceof DragView) {
                this.mTopViewIndex = i;
            }
        }
        this.mChildCountOnLastUpdate = childCount;
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        if (this.mChildCountOnLastUpdate != childCount) {
            updateChildIndices();
        }
        if (this.mTopViewIndex == -1) {
            return i;
        }
        if (i == childCount - 1) {
            return this.mTopViewIndex;
        }
        if (i < this.mTopViewIndex) {
            return i;
        }
        return i + 1;
    }

    void onEnterScrollArea(int direction) {
        this.mInScrollArea = true;
        invalidate();
    }

    void onExitScrollArea() {
        this.mInScrollArea = false;
        invalidate();
    }

    void showPageHints() {
        this.mShowPageHints = true;
        Workspace workspace = this.mLauncher.getWorkspace();
        getDescendantRectRelativeToSelf(workspace.getChildAt(workspace.numCustomPages()), this.mScrollChildPosition);
        invalidate();
    }

    void hidePageHints() {
        this.mShowPageHints = false;
        invalidate();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (this.mBackgroundAlpha > 0.0f) {
            int alpha = (int) (this.mBackgroundAlpha * 255.0f);
            canvas.drawColor((alpha << 24) | 0);
        }
        super.dispatchDraw(canvas);
    }

    private void drawPageHints(Canvas canvas) {
        if (!this.mShowPageHints) {
            return;
        }
        Workspace workspace = this.mLauncher.getWorkspace();
        int width = getMeasuredWidth();
        int page = workspace.getNextPage();
        CellLayout leftPage = (CellLayout) workspace.getChildAt(this.mIsRtl ? page + 1 : page - 1);
        CellLayout rightPage = (CellLayout) workspace.getChildAt(this.mIsRtl ? page - 1 : page + 1);
        if (leftPage != null && leftPage.isDragTarget()) {
            Drawable left = (this.mInScrollArea && leftPage.getIsDragOverlapping()) ? this.mLeftHoverDrawableActive : this.mLeftHoverDrawable;
            left.setBounds(0, this.mScrollChildPosition.top, left.getIntrinsicWidth(), this.mScrollChildPosition.bottom);
            left.draw(canvas);
        }
        if (rightPage == null || !rightPage.isDragTarget()) {
            return;
        }
        Drawable right = (this.mInScrollArea && rightPage.getIsDragOverlapping()) ? this.mRightHoverDrawableActive : this.mRightHoverDrawable;
        right.setBounds(width - right.getIntrinsicWidth(), this.mScrollChildPosition.top, width, this.mScrollChildPosition.bottom);
        right.draw(canvas);
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        boolean ret = super.drawChild(canvas, child, drawingTime);
        if (child instanceof Workspace) {
            drawPageHints(canvas);
        }
        return ret;
    }

    public void setBackgroundAlpha(float alpha) {
        if (alpha == this.mBackgroundAlpha) {
            return;
        }
        this.mBackgroundAlpha = alpha;
        invalidate();
    }

    public float getBackgroundAlpha() {
        return this.mBackgroundAlpha;
    }

    public void setTouchCompleteListener(TouchCompleteListener listener) {
        this.mTouchCompleteListener = listener;
    }
}
