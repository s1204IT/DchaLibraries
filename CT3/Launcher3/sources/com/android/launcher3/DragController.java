package com.android.launcher3;

import android.content.ComponentName;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.InputMethodManager;
import com.android.launcher3.DropTarget;
import com.android.launcher3.accessibility.DragViewStateAnnouncer;
import com.android.launcher3.compat.PackageInstallerCompat;
import com.mediatek.launcher3.LauncherLog;
import java.util.ArrayList;
import java.util.HashSet;

public class DragController {
    private DropTarget.DragObject mDragObject;
    DragScroller mDragScroller;
    private boolean mDragging;
    private DropTarget mFlingToDeleteDropTarget;
    protected int mFlingToDeleteThresholdVelocity;
    private Handler mHandler;
    private InputMethodManager mInputMethodManager;
    private boolean mIsAccessibleDrag;
    private final boolean mIsRtl;
    private DropTarget mLastDropTarget;
    Launcher mLauncher;
    private int mMotionDownX;
    private int mMotionDownY;
    private View mMoveTarget;
    private View mScrollView;
    private int mScrollZone;
    private VelocityTracker mVelocityTracker;
    private IBinder mWindowToken;
    public static int DRAG_ACTION_MOVE = 0;
    public static int DRAG_ACTION_COPY = 1;
    private Rect mRectTemp = new Rect();
    private final int[] mCoordinatesTemp = new int[2];
    private ArrayList<DropTarget> mDropTargets = new ArrayList<>();
    private ArrayList<DragListener> mListeners = new ArrayList<>();
    int mScrollState = 0;
    private ScrollRunnable mScrollRunnable = new ScrollRunnable();
    int[] mLastTouch = new int[2];
    long mLastTouchUpTime = -1;
    int mDistanceSinceScroll = 0;
    private int[] mTmpPoint = new int[2];
    private Rect mDragLayerRect = new Rect();

    public interface DragListener {
        void onDragEnd();

        void onDragStart(DragSource dragSource, Object obj, int i);
    }

    public DragController(Launcher launcher) {
        Resources r = launcher.getResources();
        this.mLauncher = launcher;
        this.mHandler = new Handler();
        this.mScrollZone = r.getDimensionPixelSize(R.dimen.scroll_zone);
        this.mVelocityTracker = VelocityTracker.obtain();
        float density = r.getDisplayMetrics().density;
        this.mFlingToDeleteThresholdVelocity = (int) (r.getInteger(R.integer.config_flingToDeleteMinVelocity) * density);
        this.mIsRtl = Utilities.isRtl(r);
    }

    public void startDrag(View v, Bitmap bmp, DragSource source, Object dragInfo, Rect viewImageBounds, int dragAction, float initialDragViewScale) {
        int[] loc = this.mCoordinatesTemp;
        this.mLauncher.getDragLayer().getLocationInDragLayer(v, loc);
        int dragLayerX = loc[0] + viewImageBounds.left + ((int) (((bmp.getWidth() * initialDragViewScale) - bmp.getWidth()) / 2.0f));
        int dragLayerY = loc[1] + viewImageBounds.top + ((int) (((bmp.getHeight() * initialDragViewScale) - bmp.getHeight()) / 2.0f));
        startDrag(bmp, dragLayerX, dragLayerY, source, dragInfo, dragAction, null, null, initialDragViewScale, false);
        if (dragAction != DRAG_ACTION_MOVE) {
            return;
        }
        v.setVisibility(8);
    }

    public DragView startDrag(Bitmap b, int dragLayerX, int dragLayerY, DragSource source, Object dragInfo, int dragAction, Point dragOffset, Rect dragRegion, float initialDragViewScale, boolean accessible) {
        if (this.mInputMethodManager == null) {
            this.mInputMethodManager = (InputMethodManager) this.mLauncher.getSystemService("input_method");
        }
        this.mInputMethodManager.hideSoftInputFromWindow(this.mWindowToken, 0);
        for (DragListener listener : this.mListeners) {
            listener.onDragStart(source, dragInfo, dragAction);
        }
        int registrationX = this.mMotionDownX - dragLayerX;
        int registrationY = this.mMotionDownY - dragLayerY;
        if (LauncherLog.DEBUG_DRAG) {
            LauncherLog.d("Launcher.DragController", "startDrag: dragLayerX = " + dragLayerX + ", dragLayerY = " + dragLayerY + ", dragInfo = " + dragInfo + ", registrationX = " + registrationX + ", registrationY = " + registrationY + ", dragRegion = " + dragRegion);
        }
        int dragRegionLeft = dragRegion == null ? 0 : dragRegion.left;
        int dragRegionTop = dragRegion == null ? 0 : dragRegion.top;
        this.mDragging = true;
        this.mIsAccessibleDrag = accessible;
        this.mDragObject = new DropTarget.DragObject();
        DragView dragView = new DragView(this.mLauncher, b, registrationX, registrationY, 0, 0, b.getWidth(), b.getHeight(), initialDragViewScale);
        this.mDragObject.dragView = dragView;
        this.mDragObject.dragComplete = false;
        if (this.mIsAccessibleDrag) {
            this.mDragObject.xOffset = b.getWidth() / 2;
            this.mDragObject.yOffset = b.getHeight() / 2;
            this.mDragObject.accessibleDrag = true;
        } else {
            this.mDragObject.xOffset = this.mMotionDownX - (dragLayerX + dragRegionLeft);
            this.mDragObject.yOffset = this.mMotionDownY - (dragLayerY + dragRegionTop);
            this.mDragObject.stateAnnouncer = DragViewStateAnnouncer.createFor(dragView);
        }
        this.mDragObject.dragSource = source;
        this.mDragObject.dragInfo = dragInfo;
        if (dragOffset != null) {
            dragView.setDragVisualizeOffset(new Point(dragOffset));
        }
        if (dragRegion != null) {
            dragView.setDragRegion(new Rect(dragRegion));
        }
        this.mLauncher.getDragLayer().performHapticFeedback(0);
        dragView.show(this.mMotionDownX, this.mMotionDownY);
        handleMoveEvent(this.mMotionDownX, this.mMotionDownY);
        return dragView;
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (LauncherLog.DEBUG_KEY) {
            LauncherLog.d("Launcher.DragController", "dispatchKeyEvent: keycode = " + event.getKeyCode() + ", action = " + event.getAction() + ", mDragging = " + this.mDragging);
        }
        return this.mDragging;
    }

    public boolean isDragging() {
        return this.mDragging;
    }

    public void cancelDrag() {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher.DragController", "cancelDrag: mDragging = " + this.mDragging + ", mLastDropTarget = " + this.mLastDropTarget);
        }
        if (this.mDragging) {
            if (this.mLastDropTarget != null) {
                this.mLastDropTarget.onDragExit(this.mDragObject);
            }
            this.mDragObject.deferDragViewCleanupPostAnimation = false;
            this.mDragObject.cancelled = true;
            this.mDragObject.dragComplete = true;
            this.mDragObject.dragSource.onDropCompleted(null, this.mDragObject, false, false);
        }
        endDrag();
    }

    public void onAppsRemoved(HashSet<String> packageNames, HashSet<ComponentName> cns) {
        boolean isSameComponent;
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher.DragController", "onAppsRemoved: mDragging = " + this.mDragging + ", mDragObject = " + this.mDragObject);
        }
        if (this.mDragObject == null) {
            return;
        }
        Object rawDragInfo = this.mDragObject.dragInfo;
        if (!(rawDragInfo instanceof ShortcutInfo)) {
            return;
        }
        ShortcutInfo dragInfo = (ShortcutInfo) rawDragInfo;
        for (ComponentName componentName : cns) {
            if (dragInfo.intent != null) {
                ComponentName cn = dragInfo.intent.getComponent();
                if (cn == null) {
                    isSameComponent = false;
                } else if (cn.equals(componentName)) {
                    isSameComponent = true;
                } else {
                    isSameComponent = packageNames.contains(cn.getPackageName());
                }
                if (isSameComponent) {
                    cancelDrag();
                    return;
                }
            }
        }
    }

    private void endDrag() {
        if (LauncherLog.DEBUG_DRAG) {
            LauncherLog.d("Launcher.DragController", "endDrag: mDragging = " + this.mDragging + ", mDragObject = " + this.mDragObject);
        }
        if (this.mDragging) {
            this.mDragging = false;
            this.mIsAccessibleDrag = false;
            clearScrollRunnable();
            boolean isDeferred = false;
            if (this.mDragObject.dragView != null) {
                isDeferred = this.mDragObject.deferDragViewCleanupPostAnimation;
                if (!isDeferred) {
                    this.mDragObject.dragView.remove();
                }
                this.mDragObject.dragView = null;
            }
            if (!isDeferred) {
                for (DragListener listener : new ArrayList(this.mListeners)) {
                    listener.onDragEnd();
                }
            }
        }
        releaseVelocityTracker();
    }

    void onDeferredEndDrag(DragView dragView) {
        dragView.remove();
        if (!this.mDragObject.deferDragViewCleanupPostAnimation) {
            return;
        }
        for (DragListener listener : new ArrayList(this.mListeners)) {
            listener.onDragEnd();
        }
    }

    public void onDeferredEndFling(DropTarget.DragObject d) {
        d.dragSource.onFlingToDeleteCompleted();
    }

    private int[] getClampedDragLayerPos(float x, float y) {
        this.mLauncher.getDragLayer().getLocalVisibleRect(this.mDragLayerRect);
        this.mTmpPoint[0] = (int) Math.max(this.mDragLayerRect.left, Math.min(x, this.mDragLayerRect.right - 1));
        this.mTmpPoint[1] = (int) Math.max(this.mDragLayerRect.top, Math.min(y, this.mDragLayerRect.bottom - 1));
        return this.mTmpPoint;
    }

    long getLastGestureUpTime() {
        if (this.mDragging) {
            return System.currentTimeMillis();
        }
        return this.mLastTouchUpTime;
    }

    void resetLastGestureUpTime() {
        this.mLastTouchUpTime = -1L;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (this.mIsAccessibleDrag) {
            return false;
        }
        acquireVelocityTrackerAndAddMovement(ev);
        int action = ev.getAction();
        int[] dragLayerPos = getClampedDragLayerPos(ev.getX(), ev.getY());
        int dragLayerX = dragLayerPos[0];
        int dragLayerY = dragLayerPos[1];
        if (LauncherLog.DEBUG_MOTION) {
            LauncherLog.d("Launcher.DragController", "onInterceptTouchEvent: action = " + action + ", mDragging = " + this.mDragging + ", dragLayerX = " + dragLayerX + ", dragLayerY = " + dragLayerY);
        }
        switch (action) {
            case PackageInstallerCompat.STATUS_INSTALLED:
                this.mMotionDownX = dragLayerX;
                this.mMotionDownY = dragLayerY;
                this.mLastDropTarget = null;
                break;
            case PackageInstallerCompat.STATUS_INSTALLING:
                this.mLastTouchUpTime = System.currentTimeMillis();
                if (this.mDragging) {
                    PointF vec = isFlingingToDelete(this.mDragObject.dragSource);
                    if (!DeleteDropTarget.supportsDrop(this.mDragObject.dragInfo)) {
                        vec = null;
                    }
                    if (vec != null) {
                        dropOnFlingToDeleteTarget(dragLayerX, dragLayerY, vec);
                    } else {
                        drop(dragLayerX, dragLayerY);
                    }
                }
                endDrag();
                break;
            case 3:
                cancelDrag();
                break;
        }
        return this.mDragging;
    }

    void setMoveTarget(View view) {
        this.mMoveTarget = view;
    }

    public boolean dispatchUnhandledMove(View focused, int direction) {
        if (LauncherLog.DEBUG_KEY) {
            LauncherLog.d("Launcher.DragController", "dispatchUnhandledMove: focused = " + focused + ", direction = " + direction);
        }
        if (this.mMoveTarget != null) {
            return this.mMoveTarget.dispatchUnhandledMove(focused, direction);
        }
        return false;
    }

    private void clearScrollRunnable() {
        this.mHandler.removeCallbacks(this.mScrollRunnable);
        if (this.mScrollState != 1) {
            return;
        }
        this.mScrollState = 0;
        this.mScrollRunnable.setDirection(1);
        this.mDragScroller.onExitScrollArea();
        this.mLauncher.getDragLayer().onExitScrollArea();
    }

    private void handleMoveEvent(int x, int y) {
        this.mDragObject.dragView.move(x, y);
        int[] coordinates = this.mCoordinatesTemp;
        DropTarget dropTarget = findDropTarget(x, y, coordinates);
        this.mDragObject.x = coordinates[0];
        this.mDragObject.y = coordinates[1];
        if (LauncherLog.DEBUG_DRAG) {
            LauncherLog.d("Launcher.DragController", "handleMoveEvent: x = " + x + ", y = " + y + ", dragView = " + this.mDragObject.dragView + ", dragX = " + this.mDragObject.x + ", dragY = " + this.mDragObject.y);
        }
        checkTouchMove(dropTarget);
        this.mDistanceSinceScroll = (int) (((double) this.mDistanceSinceScroll) + Math.hypot(this.mLastTouch[0] - x, this.mLastTouch[1] - y));
        this.mLastTouch[0] = x;
        this.mLastTouch[1] = y;
        checkScrollState(x, y);
    }

    public void forceTouchMove() {
        int[] dummyCoordinates = this.mCoordinatesTemp;
        DropTarget dropTarget = findDropTarget(this.mLastTouch[0], this.mLastTouch[1], dummyCoordinates);
        this.mDragObject.x = dummyCoordinates[0];
        this.mDragObject.y = dummyCoordinates[1];
        checkTouchMove(dropTarget);
    }

    private void checkTouchMove(DropTarget dropTarget) {
        if (dropTarget != null) {
            if (this.mLastDropTarget != dropTarget) {
                if (this.mLastDropTarget != null) {
                    this.mLastDropTarget.onDragExit(this.mDragObject);
                }
                dropTarget.onDragEnter(this.mDragObject);
            }
            dropTarget.onDragOver(this.mDragObject);
        } else if (this.mLastDropTarget != null) {
            this.mLastDropTarget.onDragExit(this.mDragObject);
        }
        this.mLastDropTarget = dropTarget;
    }

    void checkScrollState(int x, int y) {
        int slop = ViewConfiguration.get(this.mLauncher).getScaledWindowTouchSlop();
        int delay = this.mDistanceSinceScroll < slop ? 900 : 500;
        DragLayer dragLayer = this.mLauncher.getDragLayer();
        int forwardDirection = this.mIsRtl ? 1 : 0;
        int backwardsDirection = this.mIsRtl ? 0 : 1;
        if (x < this.mScrollZone) {
            if (this.mScrollState != 0) {
                return;
            }
            this.mScrollState = 1;
            if (!this.mDragScroller.onEnterScrollArea(x, y, forwardDirection)) {
                return;
            }
            dragLayer.onEnterScrollArea(forwardDirection);
            this.mScrollRunnable.setDirection(forwardDirection);
            this.mHandler.postDelayed(this.mScrollRunnable, delay);
            return;
        }
        if (x > this.mScrollView.getWidth() - this.mScrollZone) {
            if (this.mScrollState != 0) {
                return;
            }
            this.mScrollState = 1;
            if (!this.mDragScroller.onEnterScrollArea(x, y, backwardsDirection)) {
                return;
            }
            dragLayer.onEnterScrollArea(backwardsDirection);
            this.mScrollRunnable.setDirection(backwardsDirection);
            this.mHandler.postDelayed(this.mScrollRunnable, delay);
            return;
        }
        clearScrollRunnable();
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (!this.mDragging || this.mIsAccessibleDrag) {
            return false;
        }
        acquireVelocityTrackerAndAddMovement(ev);
        int action = ev.getAction();
        int[] dragLayerPos = getClampedDragLayerPos(ev.getX(), ev.getY());
        int dragLayerX = dragLayerPos[0];
        int dragLayerY = dragLayerPos[1];
        if (LauncherLog.DEBUG_MOTION) {
            LauncherLog.d("Launcher.DragController", "onTouchEvent: action = " + action + ", dragLayerX = " + dragLayerX + ", dragLayerY = " + dragLayerY + ", mMotionDownX = " + this.mMotionDownX + ", mMotionDownY = " + this.mMotionDownY + ", mScrollState = " + this.mScrollState);
        }
        switch (action) {
            case PackageInstallerCompat.STATUS_INSTALLED:
                this.mMotionDownX = dragLayerX;
                this.mMotionDownY = dragLayerY;
                if (dragLayerX < this.mScrollZone || dragLayerX > this.mScrollView.getWidth() - this.mScrollZone) {
                    this.mScrollState = 1;
                    this.mHandler.postDelayed(this.mScrollRunnable, 500L);
                } else {
                    this.mScrollState = 0;
                }
                handleMoveEvent(dragLayerX, dragLayerY);
                return true;
            case PackageInstallerCompat.STATUS_INSTALLING:
                handleMoveEvent(dragLayerX, dragLayerY);
                this.mHandler.removeCallbacks(this.mScrollRunnable);
                if (this.mDragging) {
                    PointF vec = isFlingingToDelete(this.mDragObject.dragSource);
                    if (!DeleteDropTarget.supportsDrop(this.mDragObject.dragInfo)) {
                        vec = null;
                    }
                    if (vec != null) {
                        dropOnFlingToDeleteTarget(dragLayerX, dragLayerY, vec);
                    } else {
                        drop(dragLayerX, dragLayerY);
                    }
                }
                endDrag();
                return true;
            case PackageInstallerCompat.STATUS_FAILED:
                handleMoveEvent(dragLayerX, dragLayerY);
                return true;
            case 3:
                this.mHandler.removeCallbacks(this.mScrollRunnable);
                cancelDrag();
                return true;
            default:
                return true;
        }
    }

    public void prepareAccessibleDrag(int x, int y) {
        this.mMotionDownX = x;
        this.mMotionDownY = y;
        this.mLastDropTarget = null;
    }

    public void completeAccessibleDrag(int[] location) {
        int[] coordinates = this.mCoordinatesTemp;
        DropTarget dropTarget = findDropTarget(location[0], location[1], coordinates);
        this.mDragObject.x = coordinates[0];
        this.mDragObject.y = coordinates[1];
        checkTouchMove(dropTarget);
        dropTarget.prepareAccessibilityDrop();
        drop(location[0], location[1]);
        endDrag();
    }

    private PointF isFlingingToDelete(DragSource source) {
        if (this.mFlingToDeleteDropTarget == null || !source.supportsFlingToDelete()) {
            return null;
        }
        ViewConfiguration config = ViewConfiguration.get(this.mLauncher);
        this.mVelocityTracker.computeCurrentVelocity(1000, config.getScaledMaximumFlingVelocity());
        if (this.mVelocityTracker.getYVelocity() < this.mFlingToDeleteThresholdVelocity) {
            PointF vel = new PointF(this.mVelocityTracker.getXVelocity(), this.mVelocityTracker.getYVelocity());
            PointF upVec = new PointF(0.0f, -1.0f);
            float theta = (float) Math.acos(((vel.x * upVec.x) + (vel.y * upVec.y)) / (vel.length() * upVec.length()));
            if (theta <= Math.toRadians(35.0d)) {
                return vel;
            }
        }
        return null;
    }

    private void dropOnFlingToDeleteTarget(float x, float y, PointF vel) {
        int[] coordinates = this.mCoordinatesTemp;
        this.mDragObject.x = coordinates[0];
        this.mDragObject.y = coordinates[1];
        if (this.mLastDropTarget != null && this.mFlingToDeleteDropTarget != this.mLastDropTarget) {
            this.mLastDropTarget.onDragExit(this.mDragObject);
        }
        boolean accepted = false;
        this.mFlingToDeleteDropTarget.onDragEnter(this.mDragObject);
        this.mDragObject.dragComplete = true;
        this.mFlingToDeleteDropTarget.onDragExit(this.mDragObject);
        if (this.mFlingToDeleteDropTarget.acceptDrop(this.mDragObject)) {
            this.mFlingToDeleteDropTarget.onFlingToDelete(this.mDragObject, vel);
            accepted = true;
        }
        this.mDragObject.dragSource.onDropCompleted((View) this.mFlingToDeleteDropTarget, this.mDragObject, true, accepted);
    }

    private void drop(float x, float y) {
        int[] coordinates = this.mCoordinatesTemp;
        DropTarget dropTargetFindDropTarget = findDropTarget((int) x, (int) y, coordinates);
        this.mDragObject.x = coordinates[0];
        this.mDragObject.y = coordinates[1];
        if (LauncherLog.DEBUG_DRAG) {
            LauncherLog.d("Launcher.DragController", "drop: x = " + x + ", y = " + y + ", mDragObject.x = " + this.mDragObject.x + ", mDragObject.y = " + this.mDragObject.y + ", dropTarget = " + dropTargetFindDropTarget);
        }
        boolean accepted = false;
        if (dropTargetFindDropTarget != 0) {
            this.mDragObject.dragComplete = true;
            dropTargetFindDropTarget.onDragExit(this.mDragObject);
            if (dropTargetFindDropTarget.acceptDrop(this.mDragObject)) {
                dropTargetFindDropTarget.onDrop(this.mDragObject);
                accepted = true;
            }
        }
        this.mDragObject.dragSource.onDropCompleted((View) dropTargetFindDropTarget, this.mDragObject, false, accepted);
    }

    private DropTarget findDropTarget(int x, int y, int[] dropCoordinates) {
        Rect r = this.mRectTemp;
        ArrayList<DropTarget> dropTargets = this.mDropTargets;
        int count = dropTargets.size();
        for (int i = count - 1; i >= 0; i--) {
            DropTarget dropTarget = dropTargets.get(i);
            if (dropTarget.isDropEnabled()) {
                dropTarget.getHitRectRelativeToDragLayer(r);
                this.mDragObject.x = x;
                this.mDragObject.y = y;
                if (r.contains(x, y)) {
                    dropCoordinates[0] = x;
                    dropCoordinates[1] = y;
                    this.mLauncher.getDragLayer().mapCoordInSelfToDescendent((View) dropTarget, dropCoordinates);
                    return dropTarget;
                }
            }
        }
        return null;
    }

    public void setDragScoller(DragScroller scroller) {
        this.mDragScroller = scroller;
    }

    public void setWindowToken(IBinder token) {
        this.mWindowToken = token;
    }

    public void addDragListener(DragListener l) {
        this.mListeners.add(l);
    }

    public void removeDragListener(DragListener l) {
        this.mListeners.remove(l);
    }

    public void addDropTarget(DropTarget target) {
        this.mDropTargets.add(target);
    }

    public void removeDropTarget(DropTarget target) {
        this.mDropTargets.remove(target);
    }

    public void setFlingToDeleteDropTarget(DropTarget target) {
        this.mFlingToDeleteDropTarget = target;
    }

    private void acquireVelocityTrackerAndAddMovement(MotionEvent ev) {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
        this.mVelocityTracker.addMovement(ev);
    }

    private void releaseVelocityTracker() {
        if (this.mVelocityTracker == null) {
            return;
        }
        this.mVelocityTracker.recycle();
        this.mVelocityTracker = null;
    }

    public void setScrollView(View v) {
        this.mScrollView = v;
    }

    private class ScrollRunnable implements Runnable {
        private int mDirection;

        ScrollRunnable() {
        }

        @Override
        public void run() {
            if (DragController.this.mDragScroller == null) {
                return;
            }
            if (this.mDirection == 0) {
                DragController.this.mDragScroller.scrollLeft();
            } else {
                DragController.this.mDragScroller.scrollRight();
            }
            DragController.this.mScrollState = 0;
            DragController.this.mDistanceSinceScroll = 0;
            DragController.this.mDragScroller.onExitScrollArea();
            DragController.this.mLauncher.getDragLayer().onExitScrollArea();
            if (!DragController.this.isDragging()) {
                return;
            }
            DragController.this.checkScrollState(DragController.this.mLastTouch[0], DragController.this.mLastTouch[1]);
        }

        void setDirection(int direction) {
            this.mDirection = direction;
        }
    }
}
