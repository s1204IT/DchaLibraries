package com.android.launcher3.dragndrop;

import android.content.ComponentName;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.IBinder;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import com.android.launcher3.DragSource;
import com.android.launcher3.DropTarget;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.accessibility.DragViewStateAnnouncer;
import com.android.launcher3.dragndrop.DragDriver;
import com.android.launcher3.util.ItemInfoMatcher;
import com.android.launcher3.util.TouchController;
import com.android.launcher3.util.UiThreadHelper;
import java.util.ArrayList;
import java.util.Iterator;

/* loaded from: classes.dex */
public class DragController implements DragDriver.EventListener, TouchController {
    private static final boolean PROFILE_DRAWING_DURING_DRAG = false;
    private DropTarget.DragObject mDragObject;
    private FlingToDeleteHelper mFlingToDeleteHelper;
    private boolean mIsInPreDrag;
    private DropTarget mLastDropTarget;
    Launcher mLauncher;
    private int mMotionDownX;
    private int mMotionDownY;
    private View mMoveTarget;
    private DragOptions mOptions;
    private IBinder mWindowToken;
    private Rect mRectTemp = new Rect();
    private final int[] mCoordinatesTemp = new int[2];
    private DragDriver mDragDriver = null;
    private ArrayList<DropTarget> mDropTargets = new ArrayList<>();
    private ArrayList<DragListener> mListeners = new ArrayList<>();
    int[] mLastTouch = new int[2];
    long mLastTouchUpTime = -1;
    int mDistanceSinceScroll = 0;
    private int[] mTmpPoint = new int[2];
    private Rect mDragLayerRect = new Rect();

    public interface DragListener {
        void onDragEnd();

        void onDragStart(DropTarget.DragObject dragObject, DragOptions dragOptions);
    }

    public DragController(Launcher launcher) {
        this.mLauncher = launcher;
        this.mFlingToDeleteHelper = new FlingToDeleteHelper(launcher);
    }

    public DragView startDrag(Bitmap bitmap, int i, int i2, DragSource dragSource, ItemInfo itemInfo, Point point, Rect rect, float f, float f2, DragOptions dragOptions) {
        int i3;
        int i4;
        UiThreadHelper.hideKeyboardAsync(this.mLauncher, this.mWindowToken);
        this.mOptions = dragOptions;
        if (this.mOptions.systemDndStartPoint != null) {
            this.mMotionDownX = this.mOptions.systemDndStartPoint.x;
            this.mMotionDownY = this.mOptions.systemDndStartPoint.y;
        }
        int i5 = this.mMotionDownX - i;
        int i6 = this.mMotionDownY - i2;
        if (rect != null) {
            i3 = rect.left;
        } else {
            i3 = 0;
        }
        if (rect != null) {
            i4 = rect.top;
        } else {
            i4 = 0;
        }
        this.mLastDropTarget = null;
        this.mDragObject = new DropTarget.DragObject();
        this.mIsInPreDrag = (this.mOptions.preDragCondition == null || this.mOptions.preDragCondition.shouldStartDrag(0.0d)) ? false : true;
        float dimensionPixelSize = this.mIsInPreDrag ? this.mLauncher.getResources().getDimensionPixelSize(R.dimen.pre_drag_view_scale) : 0.0f;
        DropTarget.DragObject dragObject = this.mDragObject;
        DragView dragView = new DragView(this.mLauncher, bitmap, i5, i6, f, f2, dimensionPixelSize);
        dragObject.dragView = dragView;
        dragView.setItemInfo(itemInfo);
        this.mDragObject.dragComplete = false;
        if (this.mOptions.isAccessibleDrag) {
            this.mDragObject.xOffset = bitmap.getWidth() / 2;
            this.mDragObject.yOffset = bitmap.getHeight() / 2;
            this.mDragObject.accessibleDrag = true;
        } else {
            this.mDragObject.xOffset = this.mMotionDownX - (i + i3);
            this.mDragObject.yOffset = this.mMotionDownY - (i2 + i4);
            this.mDragObject.stateAnnouncer = DragViewStateAnnouncer.createFor(dragView);
            this.mDragDriver = DragDriver.create(this.mLauncher, this, this.mDragObject, this.mOptions);
        }
        this.mDragObject.dragSource = dragSource;
        this.mDragObject.dragInfo = itemInfo;
        this.mDragObject.originalDragInfo = new ItemInfo();
        this.mDragObject.originalDragInfo.copyFrom(itemInfo);
        if (point != null) {
            dragView.setDragVisualizeOffset(new Point(point));
        }
        if (rect != null) {
            dragView.setDragRegion(new Rect(rect));
        }
        this.mLauncher.getDragLayer().performHapticFeedback(0);
        dragView.show(this.mMotionDownX, this.mMotionDownY);
        this.mDistanceSinceScroll = 0;
        if (!this.mIsInPreDrag) {
            callOnDragStart();
        } else if (this.mOptions.preDragCondition != null) {
            this.mOptions.preDragCondition.onPreDragStart(this.mDragObject);
        }
        this.mLastTouch[0] = this.mMotionDownX;
        this.mLastTouch[1] = this.mMotionDownY;
        handleMoveEvent(this.mMotionDownX, this.mMotionDownY);
        this.mLauncher.getUserEventDispatcher().resetActionDurationMillis();
        return dragView;
    }

    private void callOnDragStart() {
        if (this.mOptions.preDragCondition != null) {
            this.mOptions.preDragCondition.onPreDragEnd(this.mDragObject, true);
        }
        this.mIsInPreDrag = false;
        Iterator it = new ArrayList(this.mListeners).iterator();
        while (it.hasNext()) {
            ((DragListener) it.next()).onDragStart(this.mDragObject, this.mOptions);
        }
    }

    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        return this.mDragDriver != null;
    }

    public boolean isDragging() {
        return this.mDragDriver != null || (this.mOptions != null && this.mOptions.isAccessibleDrag);
    }

    public void cancelDrag() {
        if (isDragging()) {
            if (this.mLastDropTarget != null) {
                this.mLastDropTarget.onDragExit(this.mDragObject);
            }
            this.mDragObject.deferDragViewCleanupPostAnimation = false;
            this.mDragObject.cancelled = true;
            this.mDragObject.dragComplete = true;
            if (!this.mIsInPreDrag) {
                dispatchDropComplete(null, false);
            }
        }
        endDrag();
    }

    private void dispatchDropComplete(View view, boolean z) {
        if (!z) {
            this.mLauncher.getStateManager().goToState(LauncherState.NORMAL, 500L);
            this.mDragObject.deferDragViewCleanupPostAnimation = false;
        }
        this.mDragObject.dragSource.onDropCompleted(view, this.mDragObject, z);
    }

    public void onAppsRemoved(ItemInfoMatcher itemInfoMatcher) {
        ComponentName targetComponent;
        if (this.mDragObject != null) {
            ItemInfo itemInfo = this.mDragObject.dragInfo;
            if ((itemInfo instanceof ShortcutInfo) && (targetComponent = itemInfo.getTargetComponent()) != null && itemInfoMatcher.matches(itemInfo, targetComponent)) {
                cancelDrag();
            }
        }
    }

    private void endDrag() {
        if (isDragging()) {
            this.mDragDriver = null;
            boolean z = false;
            if (this.mDragObject.dragView != null) {
                z = this.mDragObject.deferDragViewCleanupPostAnimation;
                if (!z) {
                    this.mDragObject.dragView.remove();
                } else if (this.mIsInPreDrag) {
                    animateDragViewToOriginalPosition(null, null, -1);
                }
                this.mDragObject.dragView = null;
            }
            if (!z) {
                callOnDragEnd();
            }
        }
        this.mFlingToDeleteHelper.releaseVelocityTracker();
    }

    /* renamed from: com.android.launcher3.dragndrop.DragController$1 */
    class AnonymousClass1 implements Runnable {
        final /* synthetic */ Runnable val$onComplete;
        final /* synthetic */ View val$originalIcon;

        AnonymousClass1(View view, Runnable runnable) {
            view = view;
            runnable = runnable;
        }

        @Override // java.lang.Runnable
        public void run() {
            if (view != null) {
                view.setVisibility(0);
            }
            if (runnable != null) {
                runnable.run();
            }
        }
    }

    public void animateDragViewToOriginalPosition(Runnable runnable, View view, int i) {
        this.mDragObject.dragView.animateTo(this.mMotionDownX, this.mMotionDownY, new Runnable() { // from class: com.android.launcher3.dragndrop.DragController.1
            final /* synthetic */ Runnable val$onComplete;
            final /* synthetic */ View val$originalIcon;

            AnonymousClass1(View view2, Runnable runnable2) {
                view = view2;
                runnable = runnable2;
            }

            @Override // java.lang.Runnable
            public void run() {
                if (view != null) {
                    view.setVisibility(0);
                }
                if (runnable != null) {
                    runnable.run();
                }
            }
        }, i);
    }

    private void callOnDragEnd() {
        if (this.mIsInPreDrag && this.mOptions.preDragCondition != null) {
            this.mOptions.preDragCondition.onPreDragEnd(this.mDragObject, false);
        }
        this.mIsInPreDrag = false;
        this.mOptions = null;
        Iterator it = new ArrayList(this.mListeners).iterator();
        while (it.hasNext()) {
            ((DragListener) it.next()).onDragEnd();
        }
    }

    void onDeferredEndDrag(DragView dragView) {
        dragView.remove();
        if (this.mDragObject.deferDragViewCleanupPostAnimation) {
            callOnDragEnd();
        }
    }

    private int[] getClampedDragLayerPos(float f, float f2) {
        this.mLauncher.getDragLayer().getLocalVisibleRect(this.mDragLayerRect);
        this.mTmpPoint[0] = (int) Math.max(this.mDragLayerRect.left, Math.min(f, this.mDragLayerRect.right - 1));
        this.mTmpPoint[1] = (int) Math.max(this.mDragLayerRect.top, Math.min(f2, this.mDragLayerRect.bottom - 1));
        return this.mTmpPoint;
    }

    public long getLastGestureUpTime() {
        if (this.mDragDriver != null) {
            return System.currentTimeMillis();
        }
        return this.mLastTouchUpTime;
    }

    public void resetLastGestureUpTime() {
        this.mLastTouchUpTime = -1L;
    }

    @Override // com.android.launcher3.dragndrop.DragDriver.EventListener
    public void onDriverDragMove(float f, float f2) {
        int[] clampedDragLayerPos = getClampedDragLayerPos(f, f2);
        handleMoveEvent(clampedDragLayerPos[0], clampedDragLayerPos[1]);
    }

    @Override // com.android.launcher3.dragndrop.DragDriver.EventListener
    public void onDriverDragExitWindow() {
        if (this.mLastDropTarget != null) {
            this.mLastDropTarget.onDragExit(this.mDragObject);
            this.mLastDropTarget = null;
        }
    }

    @Override // com.android.launcher3.dragndrop.DragDriver.EventListener
    public void onDriverDragEnd(float f, float f2) {
        DropTarget dropTargetFindDropTarget;
        Runnable flingAnimation = this.mFlingToDeleteHelper.getFlingAnimation(this.mDragObject);
        if (flingAnimation != null) {
            dropTargetFindDropTarget = this.mFlingToDeleteHelper.getDropTarget();
        } else {
            dropTargetFindDropTarget = findDropTarget((int) f, (int) f2, this.mCoordinatesTemp);
        }
        drop(dropTargetFindDropTarget, flingAnimation);
        endDrag();
    }

    @Override // com.android.launcher3.dragndrop.DragDriver.EventListener
    public void onDriverDragCancel() {
        cancelDrag();
    }

    @Override // com.android.launcher3.util.TouchController
    public boolean onControllerInterceptTouchEvent(MotionEvent motionEvent) {
        if (this.mOptions != null && this.mOptions.isAccessibleDrag) {
            return false;
        }
        this.mFlingToDeleteHelper.recordMotionEvent(motionEvent);
        int action = motionEvent.getAction();
        int[] clampedDragLayerPos = getClampedDragLayerPos(motionEvent.getX(), motionEvent.getY());
        int i = clampedDragLayerPos[0];
        int i2 = clampedDragLayerPos[1];
        switch (action) {
            case 0:
                this.mMotionDownX = i;
                this.mMotionDownY = i2;
                break;
            case 1:
                this.mLastTouchUpTime = System.currentTimeMillis();
                break;
        }
        return this.mDragDriver != null && this.mDragDriver.onInterceptTouchEvent(motionEvent);
    }

    public boolean onDragEvent(long j, DragEvent dragEvent) {
        this.mFlingToDeleteHelper.recordDragEvent(j, dragEvent);
        return this.mDragDriver != null && this.mDragDriver.onDragEvent(dragEvent);
    }

    public void onDragViewAnimationEnd() {
        if (this.mDragDriver != null) {
            this.mDragDriver.onDragViewAnimationEnd();
        }
    }

    public void setMoveTarget(View view) {
        this.mMoveTarget = view;
    }

    public boolean dispatchUnhandledMove(View view, int i) {
        return this.mMoveTarget != null && this.mMoveTarget.dispatchUnhandledMove(view, i);
    }

    private void handleMoveEvent(int i, int i2) {
        this.mDragObject.dragView.move(i, i2);
        int[] iArr = this.mCoordinatesTemp;
        DropTarget dropTargetFindDropTarget = findDropTarget(i, i2, iArr);
        this.mDragObject.x = iArr[0];
        this.mDragObject.y = iArr[1];
        checkTouchMove(dropTargetFindDropTarget);
        this.mDistanceSinceScroll = (int) (this.mDistanceSinceScroll + Math.hypot(this.mLastTouch[0] - i, this.mLastTouch[1] - i2));
        this.mLastTouch[0] = i;
        this.mLastTouch[1] = i2;
        if (this.mIsInPreDrag && this.mOptions.preDragCondition != null && this.mOptions.preDragCondition.shouldStartDrag(this.mDistanceSinceScroll)) {
            callOnDragStart();
        }
    }

    public float getDistanceDragged() {
        return this.mDistanceSinceScroll;
    }

    public void forceTouchMove() {
        int[] iArr = this.mCoordinatesTemp;
        DropTarget dropTargetFindDropTarget = findDropTarget(this.mLastTouch[0], this.mLastTouch[1], iArr);
        this.mDragObject.x = iArr[0];
        this.mDragObject.y = iArr[1];
        checkTouchMove(dropTargetFindDropTarget);
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

    @Override // com.android.launcher3.util.TouchController
    public boolean onControllerTouchEvent(MotionEvent motionEvent) {
        if (this.mDragDriver == null || this.mOptions == null || this.mOptions.isAccessibleDrag) {
            return false;
        }
        this.mFlingToDeleteHelper.recordMotionEvent(motionEvent);
        int action = motionEvent.getAction();
        int[] clampedDragLayerPos = getClampedDragLayerPos(motionEvent.getX(), motionEvent.getY());
        int i = clampedDragLayerPos[0];
        int i2 = clampedDragLayerPos[1];
        if (action == 0) {
            this.mMotionDownX = i;
            this.mMotionDownY = i2;
        }
        return this.mDragDriver.onTouchEvent(motionEvent);
    }

    public void prepareAccessibleDrag(int i, int i2) {
        this.mMotionDownX = i;
        this.mMotionDownY = i2;
    }

    public void completeAccessibleDrag(int[] iArr) {
        int[] iArr2 = this.mCoordinatesTemp;
        DropTarget dropTargetFindDropTarget = findDropTarget(iArr[0], iArr[1], iArr2);
        this.mDragObject.x = iArr2[0];
        this.mDragObject.y = iArr2[1];
        checkTouchMove(dropTargetFindDropTarget);
        dropTargetFindDropTarget.prepareAccessibilityDrop();
        drop(dropTargetFindDropTarget, null);
        endDrag();
    }

    /* JADX DEBUG: Multi-variable search result rejected for r5v0, resolved type: com.android.launcher3.DropTarget */
    /* JADX WARN: Multi-variable type inference failed */
    private void drop(DropTarget dropTarget, Runnable runnable) {
        int[] iArr = this.mCoordinatesTemp;
        boolean z = false;
        this.mDragObject.x = iArr[0];
        this.mDragObject.y = iArr[1];
        if (dropTarget != this.mLastDropTarget) {
            if (this.mLastDropTarget != null) {
                this.mLastDropTarget.onDragExit(this.mDragObject);
            }
            this.mLastDropTarget = dropTarget;
            if (dropTarget != 0) {
                dropTarget.onDragEnter(this.mDragObject);
            }
        }
        this.mDragObject.dragComplete = true;
        if (this.mIsInPreDrag) {
            if (dropTarget != 0) {
                dropTarget.onDragExit(this.mDragObject);
                return;
            }
            return;
        }
        if (dropTarget != 0) {
            dropTarget.onDragExit(this.mDragObject);
            if (dropTarget.acceptDrop(this.mDragObject)) {
                if (runnable != null) {
                    runnable.run();
                } else {
                    dropTarget.onDrop(this.mDragObject, this.mOptions);
                }
                z = true;
            }
        }
        View view = dropTarget instanceof View ? (View) dropTarget : null;
        this.mLauncher.getUserEventDispatcher().logDragNDrop(this.mDragObject, view);
        dispatchDropComplete(view, z);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r5v1, resolved type: com.android.launcher3.DropTarget */
    /* JADX WARN: Multi-variable type inference failed */
    private DropTarget findDropTarget(int i, int i2, int[] iArr) {
        this.mDragObject.x = i;
        this.mDragObject.y = i2;
        Rect rect = this.mRectTemp;
        ArrayList<DropTarget> arrayList = this.mDropTargets;
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            DropTarget dropTarget = arrayList.get(size);
            if (dropTarget.isDropEnabled()) {
                dropTarget.getHitRectRelativeToDragLayer(rect);
                if (rect.contains(i, i2)) {
                    iArr[0] = i;
                    iArr[1] = i2;
                    this.mLauncher.getDragLayer().mapCoordInSelfToDescendant((View) dropTarget, iArr);
                    return dropTarget;
                }
            }
        }
        iArr[0] = i;
        iArr[1] = i2;
        this.mLauncher.getDragLayer().mapCoordInSelfToDescendant(this.mLauncher.getWorkspace(), iArr);
        return this.mLauncher.getWorkspace();
    }

    public void setWindowToken(IBinder iBinder) {
        this.mWindowToken = iBinder;
    }

    public void addDragListener(DragListener dragListener) {
        this.mListeners.add(dragListener);
    }

    public void removeDragListener(DragListener dragListener) {
        this.mListeners.remove(dragListener);
    }

    public void addDropTarget(DropTarget dropTarget) {
        this.mDropTargets.add(dropTarget);
    }

    public void removeDropTarget(DropTarget dropTarget) {
        this.mDropTargets.remove(dropTarget);
    }
}
