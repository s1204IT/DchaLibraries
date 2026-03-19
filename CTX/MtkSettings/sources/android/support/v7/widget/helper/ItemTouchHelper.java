package android.support.v7.widget.helper;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.recyclerview.R;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.view.animation.Interpolator;
import java.util.ArrayList;
import java.util.List;

public class ItemTouchHelper extends RecyclerView.ItemDecoration implements RecyclerView.OnChildAttachStateChangeListener {
    private Callback mCallback;
    private List<Integer> mDistances;
    private long mDragScrollStartTimeInMs;
    private float mDx;
    private float mDy;
    private GestureDetectorCompat mGestureDetector;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private ItemTouchHelperGestureListener mItemTouchHelperGestureListener;
    private float mMaxSwipeVelocity;
    private RecyclerView mRecyclerView;
    private int mSelectedFlags;
    private float mSelectedStartX;
    private float mSelectedStartY;
    private int mSlop;
    private List<RecyclerView.ViewHolder> mSwapTargets;
    private float mSwipeEscapeVelocity;
    private Rect mTmpRect;
    private VelocityTracker mVelocityTracker;
    final List<View> mPendingCleanup = new ArrayList();
    private final float[] mTmpPosition = new float[2];
    private RecyclerView.ViewHolder mSelected = null;
    private int mActivePointerId = -1;
    private int mActionState = 0;
    List<RecoverAnimation> mRecoverAnimations = new ArrayList();
    private final Runnable mScrollRunnable = new Runnable() {
        @Override
        public void run() {
            if (ItemTouchHelper.this.mSelected != null && ItemTouchHelper.this.scrollIfNecessary()) {
                if (ItemTouchHelper.this.mSelected != null) {
                    ItemTouchHelper.this.moveIfNecessary(ItemTouchHelper.this.mSelected);
                }
                ItemTouchHelper.this.mRecyclerView.removeCallbacks(ItemTouchHelper.this.mScrollRunnable);
                ViewCompat.postOnAnimation(ItemTouchHelper.this.mRecyclerView, this);
            }
        }
    };
    private RecyclerView.ChildDrawingOrderCallback mChildDrawingOrderCallback = null;
    private View mOverdrawChild = null;
    private int mOverdrawChildPosition = -1;
    private final RecyclerView.OnItemTouchListener mOnItemTouchListener = new RecyclerView.OnItemTouchListener() {
        @Override
        public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent event) {
            int index;
            RecoverAnimation animation;
            ItemTouchHelper.this.mGestureDetector.onTouchEvent(event);
            int action = event.getActionMasked();
            if (action == 0) {
                ItemTouchHelper.this.mActivePointerId = event.getPointerId(0);
                ItemTouchHelper.this.mInitialTouchX = event.getX();
                ItemTouchHelper.this.mInitialTouchY = event.getY();
                ItemTouchHelper.this.obtainVelocityTracker();
                if (ItemTouchHelper.this.mSelected == null && (animation = ItemTouchHelper.this.findAnimation(event)) != null) {
                    ItemTouchHelper.this.mInitialTouchX -= animation.mX;
                    ItemTouchHelper.this.mInitialTouchY -= animation.mY;
                    ItemTouchHelper.this.endRecoverAnimation(animation.mViewHolder, true);
                    if (ItemTouchHelper.this.mPendingCleanup.remove(animation.mViewHolder.itemView)) {
                        ItemTouchHelper.this.mCallback.clearView(ItemTouchHelper.this.mRecyclerView, animation.mViewHolder);
                    }
                    ItemTouchHelper.this.select(animation.mViewHolder, animation.mActionState);
                    ItemTouchHelper.this.updateDxDy(event, ItemTouchHelper.this.mSelectedFlags, 0);
                }
            } else if (action == 3 || action == 1) {
                ItemTouchHelper.this.mActivePointerId = -1;
                ItemTouchHelper.this.select(null, 0);
            } else if (ItemTouchHelper.this.mActivePointerId != -1 && (index = event.findPointerIndex(ItemTouchHelper.this.mActivePointerId)) >= 0) {
                ItemTouchHelper.this.checkSelectForSwipe(action, event, index);
            }
            if (ItemTouchHelper.this.mVelocityTracker != null) {
                ItemTouchHelper.this.mVelocityTracker.addMovement(event);
            }
            return ItemTouchHelper.this.mSelected != null;
        }

        @Override
        public void onTouchEvent(RecyclerView recyclerView, MotionEvent event) {
            ItemTouchHelper.this.mGestureDetector.onTouchEvent(event);
            if (ItemTouchHelper.this.mVelocityTracker != null) {
                ItemTouchHelper.this.mVelocityTracker.addMovement(event);
            }
            if (ItemTouchHelper.this.mActivePointerId == -1) {
                return;
            }
            int action = event.getActionMasked();
            int activePointerIndex = event.findPointerIndex(ItemTouchHelper.this.mActivePointerId);
            if (activePointerIndex >= 0) {
                ItemTouchHelper.this.checkSelectForSwipe(action, event, activePointerIndex);
            }
            RecyclerView.ViewHolder viewHolder = ItemTouchHelper.this.mSelected;
            if (viewHolder == null) {
                return;
            }
            if (action != 6) {
                switch (action) {
                    case 1:
                        break;
                    case 2:
                        if (activePointerIndex >= 0) {
                            ItemTouchHelper.this.updateDxDy(event, ItemTouchHelper.this.mSelectedFlags, activePointerIndex);
                            ItemTouchHelper.this.moveIfNecessary(viewHolder);
                            ItemTouchHelper.this.mRecyclerView.removeCallbacks(ItemTouchHelper.this.mScrollRunnable);
                            ItemTouchHelper.this.mScrollRunnable.run();
                            ItemTouchHelper.this.mRecyclerView.invalidate();
                            return;
                        }
                        return;
                    case 3:
                        if (ItemTouchHelper.this.mVelocityTracker != null) {
                            ItemTouchHelper.this.mVelocityTracker.clear();
                        }
                        break;
                    default:
                        return;
                }
                ItemTouchHelper.this.select(null, 0);
                ItemTouchHelper.this.mActivePointerId = -1;
                return;
            }
            int pointerIndex = event.getActionIndex();
            int pointerId = event.getPointerId(pointerIndex);
            if (pointerId == ItemTouchHelper.this.mActivePointerId) {
                int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                ItemTouchHelper.this.mActivePointerId = event.getPointerId(newPointerIndex);
                ItemTouchHelper.this.updateDxDy(event, ItemTouchHelper.this.mSelectedFlags, pointerIndex);
            }
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            if (disallowIntercept) {
                ItemTouchHelper.this.select(null, 0);
            }
        }
    };

    public interface ViewDropHandler {
        void prepareForDrop(View view, View view2, int i, int i2);
    }

    public ItemTouchHelper(Callback callback) {
        this.mCallback = callback;
    }

    private static boolean hitTest(View child, float x, float y, float left, float top) {
        return x >= left && x <= ((float) child.getWidth()) + left && y >= top && y <= ((float) child.getHeight()) + top;
    }

    public void attachToRecyclerView(RecyclerView recyclerView) {
        if (this.mRecyclerView == recyclerView) {
            return;
        }
        if (this.mRecyclerView != null) {
            destroyCallbacks();
        }
        this.mRecyclerView = recyclerView;
        if (recyclerView != null) {
            Resources resources = recyclerView.getResources();
            this.mSwipeEscapeVelocity = resources.getDimension(R.dimen.item_touch_helper_swipe_escape_velocity);
            this.mMaxSwipeVelocity = resources.getDimension(R.dimen.item_touch_helper_swipe_escape_max_velocity);
            setupCallbacks();
        }
    }

    private void setupCallbacks() {
        ViewConfiguration vc = ViewConfiguration.get(this.mRecyclerView.getContext());
        this.mSlop = vc.getScaledTouchSlop();
        this.mRecyclerView.addItemDecoration(this);
        this.mRecyclerView.addOnItemTouchListener(this.mOnItemTouchListener);
        this.mRecyclerView.addOnChildAttachStateChangeListener(this);
        startGestureDetection();
    }

    private void destroyCallbacks() {
        this.mRecyclerView.removeItemDecoration(this);
        this.mRecyclerView.removeOnItemTouchListener(this.mOnItemTouchListener);
        this.mRecyclerView.removeOnChildAttachStateChangeListener(this);
        int recoverAnimSize = this.mRecoverAnimations.size();
        for (int i = recoverAnimSize - 1; i >= 0; i--) {
            RecoverAnimation recoverAnimation = this.mRecoverAnimations.get(0);
            this.mCallback.clearView(this.mRecyclerView, recoverAnimation.mViewHolder);
        }
        this.mRecoverAnimations.clear();
        this.mOverdrawChild = null;
        this.mOverdrawChildPosition = -1;
        releaseVelocityTracker();
        stopGestureDetection();
    }

    private void startGestureDetection() {
        this.mItemTouchHelperGestureListener = new ItemTouchHelperGestureListener();
        this.mGestureDetector = new GestureDetectorCompat(this.mRecyclerView.getContext(), this.mItemTouchHelperGestureListener);
    }

    private void stopGestureDetection() {
        if (this.mItemTouchHelperGestureListener != null) {
            this.mItemTouchHelperGestureListener.doNotReactToLongPress();
            this.mItemTouchHelperGestureListener = null;
        }
        if (this.mGestureDetector != null) {
            this.mGestureDetector = null;
        }
    }

    private void getSelectedDxDy(float[] outPosition) {
        if ((this.mSelectedFlags & 12) != 0) {
            outPosition[0] = (this.mSelectedStartX + this.mDx) - this.mSelected.itemView.getLeft();
        } else {
            outPosition[0] = this.mSelected.itemView.getTranslationX();
        }
        if ((this.mSelectedFlags & 3) != 0) {
            outPosition[1] = (this.mSelectedStartY + this.mDy) - this.mSelected.itemView.getTop();
        } else {
            outPosition[1] = this.mSelected.itemView.getTranslationY();
        }
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        float dx = 0.0f;
        float dy = 0.0f;
        if (this.mSelected != null) {
            getSelectedDxDy(this.mTmpPosition);
            dx = this.mTmpPosition[0];
            dy = this.mTmpPosition[1];
        }
        this.mCallback.onDrawOver(c, parent, this.mSelected, this.mRecoverAnimations, this.mActionState, dx, dy);
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        this.mOverdrawChildPosition = -1;
        float dx = 0.0f;
        float dy = 0.0f;
        if (this.mSelected != null) {
            getSelectedDxDy(this.mTmpPosition);
            dx = this.mTmpPosition[0];
            dy = this.mTmpPosition[1];
        }
        this.mCallback.onDraw(c, parent, this.mSelected, this.mRecoverAnimations, this.mActionState, dx, dy);
    }

    private void select(RecyclerView.ViewHolder selected, int actionState) {
        int prevActionState;
        boolean z;
        float targetTranslateY;
        float targetTranslateX;
        if (selected == this.mSelected && actionState == this.mActionState) {
            return;
        }
        this.mDragScrollStartTimeInMs = Long.MIN_VALUE;
        int prevActionState2 = this.mActionState;
        endRecoverAnimation(selected, true);
        this.mActionState = actionState;
        if (actionState == 2) {
            if (selected == null) {
                throw new IllegalArgumentException("Must pass a ViewHolder when dragging");
            }
            this.mOverdrawChild = selected.itemView;
            addChildDrawingOrderCallback();
        }
        int actionStateMask = (1 << ((8 * actionState) + 8)) - 1;
        boolean preventLayout = false;
        if (this.mSelected == null) {
            prevActionState = 2;
        } else {
            final RecyclerView.ViewHolder prevSelected = this.mSelected;
            if (prevSelected.itemView.getParent() == null) {
                prevActionState = 2;
                removeChildDrawingOrderCallbackIfNecessary(prevSelected.itemView);
                this.mCallback.clearView(this.mRecyclerView, prevSelected);
            } else {
                final int swipeDir = prevActionState2 == 2 ? 0 : swipeIfNecessary(prevSelected);
                releaseVelocityTracker();
                int animationType = 4;
                if (swipeDir == 4 || swipeDir == 8 || swipeDir == 16 || swipeDir == 32) {
                    targetTranslateY = 0.0f;
                    targetTranslateX = Math.signum(this.mDx) * this.mRecyclerView.getWidth();
                } else {
                    switch (swipeDir) {
                        case 1:
                        case 2:
                            targetTranslateX = 0.0f;
                            targetTranslateY = Math.signum(this.mDy) * this.mRecyclerView.getHeight();
                            break;
                        default:
                            targetTranslateX = 0.0f;
                            targetTranslateY = 0.0f;
                            break;
                    }
                }
                float targetTranslateX2 = targetTranslateX;
                float targetTranslateY2 = targetTranslateY;
                if (prevActionState2 == 2) {
                    animationType = 8;
                } else if (swipeDir > 0) {
                    animationType = 2;
                }
                int animationType2 = animationType;
                getSelectedDxDy(this.mTmpPosition);
                float currentTranslateX = this.mTmpPosition[0];
                float currentTranslateY = this.mTmpPosition[1];
                prevActionState = 2;
                RecoverAnimation rv = new RecoverAnimation(prevSelected, animationType2, prevActionState2, currentTranslateX, currentTranslateY, targetTranslateX2, targetTranslateY2) {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        if (this.mOverridden) {
                            return;
                        }
                        if (swipeDir <= 0) {
                            ItemTouchHelper.this.mCallback.clearView(ItemTouchHelper.this.mRecyclerView, prevSelected);
                        } else {
                            ItemTouchHelper.this.mPendingCleanup.add(prevSelected.itemView);
                            this.mIsPendingCleanup = true;
                            if (swipeDir > 0) {
                                ItemTouchHelper.this.postDispatchSwipe(this, swipeDir);
                            }
                        }
                        if (ItemTouchHelper.this.mOverdrawChild == prevSelected.itemView) {
                            ItemTouchHelper.this.removeChildDrawingOrderCallbackIfNecessary(prevSelected.itemView);
                        }
                    }
                };
                long duration = this.mCallback.getAnimationDuration(this.mRecyclerView, animationType2, targetTranslateX2 - currentTranslateX, targetTranslateY2 - currentTranslateY);
                rv.setDuration(duration);
                this.mRecoverAnimations.add(rv);
                rv.start();
                preventLayout = true;
            }
            this.mSelected = null;
        }
        if (selected != null) {
            this.mSelectedFlags = (this.mCallback.getAbsoluteMovementFlags(this.mRecyclerView, selected) & actionStateMask) >> (this.mActionState * 8);
            this.mSelectedStartX = selected.itemView.getLeft();
            this.mSelectedStartY = selected.itemView.getTop();
            this.mSelected = selected;
            if (actionState == prevActionState) {
                z = false;
                this.mSelected.itemView.performHapticFeedback(0);
            } else {
                z = false;
            }
        }
        ViewParent rvParent = this.mRecyclerView.getParent();
        if (rvParent != null) {
            if (this.mSelected != null) {
                z = true;
            }
            rvParent.requestDisallowInterceptTouchEvent(z);
        }
        if (!preventLayout) {
            this.mRecyclerView.getLayoutManager().requestSimpleAnimationsInNextLayout();
        }
        this.mCallback.onSelectedChanged(this.mSelected, this.mActionState);
        this.mRecyclerView.invalidate();
    }

    private void postDispatchSwipe(final RecoverAnimation anim, final int swipeDir) {
        this.mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                if (ItemTouchHelper.this.mRecyclerView != null && ItemTouchHelper.this.mRecyclerView.isAttachedToWindow() && !anim.mOverridden && anim.mViewHolder.getAdapterPosition() != -1) {
                    RecyclerView.ItemAnimator animator = ItemTouchHelper.this.mRecyclerView.getItemAnimator();
                    if ((animator == null || !animator.isRunning(null)) && !ItemTouchHelper.this.hasRunningRecoverAnim()) {
                        ItemTouchHelper.this.mCallback.onSwiped(anim.mViewHolder, swipeDir);
                    } else {
                        ItemTouchHelper.this.mRecyclerView.post(this);
                    }
                }
            }
        });
    }

    private boolean hasRunningRecoverAnim() {
        int size = this.mRecoverAnimations.size();
        for (int i = 0; i < size; i++) {
            if (!this.mRecoverAnimations.get(i).mEnded) {
                return true;
            }
        }
        return false;
    }

    private boolean scrollIfNecessary() {
        int bottomDiff;
        int rightDiff;
        if (this.mSelected == null) {
            this.mDragScrollStartTimeInMs = Long.MIN_VALUE;
            return false;
        }
        long now = System.currentTimeMillis();
        long scrollDuration = this.mDragScrollStartTimeInMs == Long.MIN_VALUE ? 0L : now - this.mDragScrollStartTimeInMs;
        RecyclerView.LayoutManager lm = this.mRecyclerView.getLayoutManager();
        if (this.mTmpRect == null) {
            this.mTmpRect = new Rect();
        }
        int scrollX = 0;
        int scrollY = 0;
        lm.calculateItemDecorationsForChild(this.mSelected.itemView, this.mTmpRect);
        if (lm.canScrollHorizontally()) {
            int curX = (int) (this.mSelectedStartX + this.mDx);
            int leftDiff = (curX - this.mTmpRect.left) - this.mRecyclerView.getPaddingLeft();
            if (this.mDx < 0.0f && leftDiff < 0) {
                scrollX = leftDiff;
            } else if (this.mDx > 0.0f && (rightDiff = ((this.mSelected.itemView.getWidth() + curX) + this.mTmpRect.right) - (this.mRecyclerView.getWidth() - this.mRecyclerView.getPaddingRight())) > 0) {
                scrollX = rightDiff;
            }
        }
        if (lm.canScrollVertically()) {
            int curY = (int) (this.mSelectedStartY + this.mDy);
            int topDiff = (curY - this.mTmpRect.top) - this.mRecyclerView.getPaddingTop();
            if (this.mDy < 0.0f && topDiff < 0) {
                scrollY = topDiff;
            } else if (this.mDy > 0.0f && (bottomDiff = ((this.mSelected.itemView.getHeight() + curY) + this.mTmpRect.bottom) - (this.mRecyclerView.getHeight() - this.mRecyclerView.getPaddingBottom())) > 0) {
                scrollY = bottomDiff;
            }
        }
        if (scrollX != 0) {
            scrollX = this.mCallback.interpolateOutOfBoundsScroll(this.mRecyclerView, this.mSelected.itemView.getWidth(), scrollX, this.mRecyclerView.getWidth(), scrollDuration);
        }
        if (scrollY != 0) {
            scrollY = this.mCallback.interpolateOutOfBoundsScroll(this.mRecyclerView, this.mSelected.itemView.getHeight(), scrollY, this.mRecyclerView.getHeight(), scrollDuration);
        }
        if (scrollX != 0 || scrollY != 0) {
            if (this.mDragScrollStartTimeInMs == Long.MIN_VALUE) {
                this.mDragScrollStartTimeInMs = now;
            }
            this.mRecyclerView.scrollBy(scrollX, scrollY);
            return true;
        }
        this.mDragScrollStartTimeInMs = Long.MIN_VALUE;
        return false;
    }

    private List<RecyclerView.ViewHolder> findSwapTargets(RecyclerView.ViewHolder viewHolder) {
        int margin;
        int left;
        RecyclerView.ViewHolder viewHolder2 = viewHolder;
        if (this.mSwapTargets == null) {
            this.mSwapTargets = new ArrayList();
            this.mDistances = new ArrayList();
        } else {
            this.mSwapTargets.clear();
            this.mDistances.clear();
        }
        int margin2 = this.mCallback.getBoundingBoxMargin();
        int left2 = Math.round(this.mSelectedStartX + this.mDx) - margin2;
        int top = Math.round(this.mSelectedStartY + this.mDy) - margin2;
        int right = viewHolder2.itemView.getWidth() + left2 + (2 * margin2);
        int bottom = viewHolder2.itemView.getHeight() + top + (2 * margin2);
        int centerX = (left2 + right) / 2;
        int centerY = (top + bottom) / 2;
        RecyclerView.LayoutManager lm = this.mRecyclerView.getLayoutManager();
        int childCount = lm.getChildCount();
        int i = 0;
        while (i < childCount) {
            View other = lm.getChildAt(i);
            if (other != viewHolder2.itemView) {
                if (other.getBottom() < top || other.getTop() > bottom || other.getRight() < left2) {
                    margin = margin2;
                    left = left2;
                } else if (other.getLeft() > right) {
                    margin = margin2;
                    left = left2;
                } else {
                    RecyclerView.ViewHolder otherVh = this.mRecyclerView.getChildViewHolder(other);
                    if (this.mCallback.canDropOver(this.mRecyclerView, this.mSelected, otherVh)) {
                        int dx = Math.abs(centerX - ((other.getLeft() + other.getRight()) / 2));
                        int dy = Math.abs(centerY - ((other.getTop() + other.getBottom()) / 2));
                        int dist = (dx * dx) + (dy * dy);
                        int cnt = this.mSwapTargets.size();
                        margin = margin2;
                        int margin3 = 0;
                        int pos = 0;
                        while (true) {
                            int j = pos;
                            left = left2;
                            if (j >= cnt) {
                                break;
                            }
                            int cnt2 = cnt;
                            if (dist <= this.mDistances.get(j).intValue()) {
                                break;
                            }
                            margin3++;
                            pos = j + 1;
                            left2 = left;
                            cnt = cnt2;
                        }
                        this.mSwapTargets.add(margin3, otherVh);
                        this.mDistances.add(margin3, Integer.valueOf(dist));
                    }
                }
            }
            i++;
            margin2 = margin;
            left2 = left;
            viewHolder2 = viewHolder;
        }
        return this.mSwapTargets;
    }

    private void moveIfNecessary(RecyclerView.ViewHolder viewHolder) {
        if (!this.mRecyclerView.isLayoutRequested() && this.mActionState == 2) {
            float threshold = this.mCallback.getMoveThreshold(viewHolder);
            int x = (int) (this.mSelectedStartX + this.mDx);
            int y = (int) (this.mSelectedStartY + this.mDy);
            if (Math.abs(y - viewHolder.itemView.getTop()) < viewHolder.itemView.getHeight() * threshold && Math.abs(x - viewHolder.itemView.getLeft()) < viewHolder.itemView.getWidth() * threshold) {
                return;
            }
            List<RecyclerView.ViewHolder> swapTargets = findSwapTargets(viewHolder);
            if (swapTargets.size() != 0) {
                RecyclerView.ViewHolder target = this.mCallback.chooseDropTarget(viewHolder, swapTargets, x, y);
                if (target == null) {
                    this.mSwapTargets.clear();
                    this.mDistances.clear();
                    return;
                }
                int toPosition = target.getAdapterPosition();
                int fromPosition = viewHolder.getAdapterPosition();
                if (this.mCallback.onMove(this.mRecyclerView, viewHolder, target)) {
                    this.mCallback.onMoved(this.mRecyclerView, viewHolder, fromPosition, target, toPosition, x, y);
                }
            }
        }
    }

    @Override
    public void onChildViewAttachedToWindow(View view) {
    }

    @Override
    public void onChildViewDetachedFromWindow(View view) {
        removeChildDrawingOrderCallbackIfNecessary(view);
        RecyclerView.ViewHolder holder = this.mRecyclerView.getChildViewHolder(view);
        if (holder == null) {
            return;
        }
        if (this.mSelected != null && holder == this.mSelected) {
            select(null, 0);
            return;
        }
        endRecoverAnimation(holder, false);
        if (this.mPendingCleanup.remove(holder.itemView)) {
            this.mCallback.clearView(this.mRecyclerView, holder);
        }
    }

    private void endRecoverAnimation(RecyclerView.ViewHolder viewHolder, boolean override) {
        int recoverAnimSize = this.mRecoverAnimations.size();
        for (int i = recoverAnimSize - 1; i >= 0; i--) {
            RecoverAnimation anim = this.mRecoverAnimations.get(i);
            if (anim.mViewHolder == viewHolder) {
                anim.mOverridden |= override;
                if (!anim.mEnded) {
                    anim.cancel();
                }
                this.mRecoverAnimations.remove(i);
                return;
            }
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.setEmpty();
    }

    private void obtainVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
        }
        this.mVelocityTracker = VelocityTracker.obtain();
    }

    private void releaseVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    private RecyclerView.ViewHolder findSwipedView(MotionEvent motionEvent) {
        View child;
        RecyclerView.LayoutManager lm = this.mRecyclerView.getLayoutManager();
        if (this.mActivePointerId == -1) {
            return null;
        }
        int pointerIndex = motionEvent.findPointerIndex(this.mActivePointerId);
        float dx = motionEvent.getX(pointerIndex) - this.mInitialTouchX;
        float dy = motionEvent.getY(pointerIndex) - this.mInitialTouchY;
        float absDx = Math.abs(dx);
        float absDy = Math.abs(dy);
        if (absDx < this.mSlop && absDy < this.mSlop) {
            return null;
        }
        if (absDx > absDy && lm.canScrollHorizontally()) {
            return null;
        }
        if ((absDy <= absDx || !lm.canScrollVertically()) && (child = findChildView(motionEvent)) != null) {
            return this.mRecyclerView.getChildViewHolder(child);
        }
        return null;
    }

    private void checkSelectForSwipe(int action, MotionEvent motionEvent, int pointerIndex) {
        RecyclerView.ViewHolder vh;
        if (this.mSelected != null || action != 2 || this.mActionState == 2 || !this.mCallback.isItemViewSwipeEnabled() || this.mRecyclerView.getScrollState() == 1 || (vh = findSwipedView(motionEvent)) == null) {
            return;
        }
        int movementFlags = this.mCallback.getAbsoluteMovementFlags(this.mRecyclerView, vh);
        int swipeFlags = (65280 & movementFlags) >> 8;
        if (swipeFlags == 0) {
            return;
        }
        float x = motionEvent.getX(pointerIndex);
        float y = motionEvent.getY(pointerIndex);
        float dx = x - this.mInitialTouchX;
        float dy = y - this.mInitialTouchY;
        float absDx = Math.abs(dx);
        float absDy = Math.abs(dy);
        if (absDx < this.mSlop && absDy < this.mSlop) {
            return;
        }
        if (absDx > absDy) {
            if (dx < 0.0f && (swipeFlags & 4) == 0) {
                return;
            }
            if (dx > 0.0f && (swipeFlags & 8) == 0) {
                return;
            }
        } else {
            if (dy < 0.0f && (swipeFlags & 1) == 0) {
                return;
            }
            if (dy > 0.0f && (swipeFlags & 2) == 0) {
                return;
            }
        }
        this.mDy = 0.0f;
        this.mDx = 0.0f;
        this.mActivePointerId = motionEvent.getPointerId(0);
        select(vh, 1);
    }

    private View findChildView(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        if (this.mSelected != null) {
            View selectedView = this.mSelected.itemView;
            if (hitTest(selectedView, x, y, this.mSelectedStartX + this.mDx, this.mSelectedStartY + this.mDy)) {
                return selectedView;
            }
        }
        for (int i = this.mRecoverAnimations.size() - 1; i >= 0; i--) {
            RecoverAnimation anim = this.mRecoverAnimations.get(i);
            View view = anim.mViewHolder.itemView;
            if (hitTest(view, x, y, anim.mX, anim.mY)) {
                return view;
            }
        }
        return this.mRecyclerView.findChildViewUnder(x, y);
    }

    public void startDrag(RecyclerView.ViewHolder viewHolder) {
        if (!this.mCallback.hasDragFlag(this.mRecyclerView, viewHolder)) {
            Log.e("ItemTouchHelper", "Start drag has been called but dragging is not enabled");
            return;
        }
        if (viewHolder.itemView.getParent() != this.mRecyclerView) {
            Log.e("ItemTouchHelper", "Start drag has been called with a view holder which is not a child of the RecyclerView which is controlled by this ItemTouchHelper.");
            return;
        }
        obtainVelocityTracker();
        this.mDy = 0.0f;
        this.mDx = 0.0f;
        select(viewHolder, 2);
    }

    private RecoverAnimation findAnimation(MotionEvent event) {
        if (this.mRecoverAnimations.isEmpty()) {
            return null;
        }
        View target = findChildView(event);
        for (int i = this.mRecoverAnimations.size() - 1; i >= 0; i--) {
            RecoverAnimation anim = this.mRecoverAnimations.get(i);
            if (anim.mViewHolder.itemView == target) {
                return anim;
            }
        }
        return null;
    }

    private void updateDxDy(MotionEvent ev, int directionFlags, int pointerIndex) {
        float x = ev.getX(pointerIndex);
        float y = ev.getY(pointerIndex);
        this.mDx = x - this.mInitialTouchX;
        this.mDy = y - this.mInitialTouchY;
        if ((directionFlags & 4) == 0) {
            this.mDx = Math.max(0.0f, this.mDx);
        }
        if ((directionFlags & 8) == 0) {
            this.mDx = Math.min(0.0f, this.mDx);
        }
        if ((directionFlags & 1) == 0) {
            this.mDy = Math.max(0.0f, this.mDy);
        }
        if ((directionFlags & 2) == 0) {
            this.mDy = Math.min(0.0f, this.mDy);
        }
    }

    private int swipeIfNecessary(RecyclerView.ViewHolder viewHolder) {
        if (this.mActionState == 2) {
            return 0;
        }
        int originalMovementFlags = this.mCallback.getMovementFlags(this.mRecyclerView, viewHolder);
        int absoluteMovementFlags = this.mCallback.convertToAbsoluteDirection(originalMovementFlags, ViewCompat.getLayoutDirection(this.mRecyclerView));
        int flags = (absoluteMovementFlags & 65280) >> 8;
        if (flags == 0) {
            return 0;
        }
        int originalFlags = (65280 & originalMovementFlags) >> 8;
        if (Math.abs(this.mDx) > Math.abs(this.mDy)) {
            int swipeDir = checkHorizontalSwipe(viewHolder, flags);
            if (swipeDir > 0) {
                return (originalFlags & swipeDir) == 0 ? Callback.convertToRelativeDirection(swipeDir, ViewCompat.getLayoutDirection(this.mRecyclerView)) : swipeDir;
            }
            int swipeDir2 = checkVerticalSwipe(viewHolder, flags);
            if (swipeDir2 > 0) {
                return swipeDir2;
            }
        } else {
            int swipeDir3 = checkVerticalSwipe(viewHolder, flags);
            if (swipeDir3 > 0) {
                return swipeDir3;
            }
            int swipeDir4 = checkHorizontalSwipe(viewHolder, flags);
            if (swipeDir4 > 0) {
                return (originalFlags & swipeDir4) == 0 ? Callback.convertToRelativeDirection(swipeDir4, ViewCompat.getLayoutDirection(this.mRecyclerView)) : swipeDir4;
            }
        }
        return 0;
    }

    private int checkHorizontalSwipe(RecyclerView.ViewHolder viewHolder, int flags) {
        if ((flags & 12) != 0) {
            int dirFlag = this.mDx > 0.0f ? 8 : 4;
            if (this.mVelocityTracker != null && this.mActivePointerId > -1) {
                this.mVelocityTracker.computeCurrentVelocity(1000, this.mCallback.getSwipeVelocityThreshold(this.mMaxSwipeVelocity));
                float xVelocity = this.mVelocityTracker.getXVelocity(this.mActivePointerId);
                float yVelocity = this.mVelocityTracker.getYVelocity(this.mActivePointerId);
                int velDirFlag = xVelocity > 0.0f ? 8 : 4;
                float absXVelocity = Math.abs(xVelocity);
                if ((velDirFlag & flags) != 0 && dirFlag == velDirFlag && absXVelocity >= this.mCallback.getSwipeEscapeVelocity(this.mSwipeEscapeVelocity) && absXVelocity > Math.abs(yVelocity)) {
                    return velDirFlag;
                }
            }
            float threshold = this.mRecyclerView.getWidth() * this.mCallback.getSwipeThreshold(viewHolder);
            if ((flags & dirFlag) != 0 && Math.abs(this.mDx) > threshold) {
                return dirFlag;
            }
            return 0;
        }
        return 0;
    }

    private int checkVerticalSwipe(RecyclerView.ViewHolder viewHolder, int flags) {
        if ((flags & 3) != 0) {
            int dirFlag = this.mDy > 0.0f ? 2 : 1;
            if (this.mVelocityTracker != null && this.mActivePointerId > -1) {
                this.mVelocityTracker.computeCurrentVelocity(1000, this.mCallback.getSwipeVelocityThreshold(this.mMaxSwipeVelocity));
                float xVelocity = this.mVelocityTracker.getXVelocity(this.mActivePointerId);
                float yVelocity = this.mVelocityTracker.getYVelocity(this.mActivePointerId);
                int velDirFlag = yVelocity > 0.0f ? 2 : 1;
                float absYVelocity = Math.abs(yVelocity);
                if ((velDirFlag & flags) != 0 && velDirFlag == dirFlag && absYVelocity >= this.mCallback.getSwipeEscapeVelocity(this.mSwipeEscapeVelocity) && absYVelocity > Math.abs(xVelocity)) {
                    return velDirFlag;
                }
            }
            float threshold = this.mRecyclerView.getHeight() * this.mCallback.getSwipeThreshold(viewHolder);
            if ((flags & dirFlag) != 0 && Math.abs(this.mDy) > threshold) {
                return dirFlag;
            }
            return 0;
        }
        return 0;
    }

    private void addChildDrawingOrderCallback() {
        if (Build.VERSION.SDK_INT >= 21) {
            return;
        }
        if (this.mChildDrawingOrderCallback == null) {
            this.mChildDrawingOrderCallback = new RecyclerView.ChildDrawingOrderCallback() {
                @Override
                public int onGetChildDrawingOrder(int childCount, int i) {
                    if (ItemTouchHelper.this.mOverdrawChild != null) {
                        int childPosition = ItemTouchHelper.this.mOverdrawChildPosition;
                        if (childPosition == -1) {
                            childPosition = ItemTouchHelper.this.mRecyclerView.indexOfChild(ItemTouchHelper.this.mOverdrawChild);
                            ItemTouchHelper.this.mOverdrawChildPosition = childPosition;
                        }
                        if (i == childCount - 1) {
                            return childPosition;
                        }
                        return i < childPosition ? i : i + 1;
                    }
                    return i;
                }
            };
        }
        this.mRecyclerView.setChildDrawingOrderCallback(this.mChildDrawingOrderCallback);
    }

    private void removeChildDrawingOrderCallbackIfNecessary(View view) {
        if (view == this.mOverdrawChild) {
            this.mOverdrawChild = null;
            if (this.mChildDrawingOrderCallback != null) {
                this.mRecyclerView.setChildDrawingOrderCallback(null);
            }
        }
    }

    public static abstract class Callback {
        private static final Interpolator sDragScrollInterpolator = new Interpolator() {
            @Override
            public float getInterpolation(float t) {
                return t * t * t * t * t;
            }
        };
        private static final Interpolator sDragViewScrollCapInterpolator = new Interpolator() {
            @Override
            public float getInterpolation(float t) {
                float t2 = t - 1.0f;
                return (t2 * t2 * t2 * t2 * t2) + 1.0f;
            }
        };
        private int mCachedMaxScrollSpeed = -1;

        public abstract int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder);

        public abstract boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder viewHolder2);

        public abstract void onSwiped(RecyclerView.ViewHolder viewHolder, int i);

        public static int convertToRelativeDirection(int flags, int layoutDirection) {
            int masked = flags & 789516;
            if (masked == 0) {
                return flags;
            }
            int flags2 = flags & (~masked);
            if (layoutDirection != 0) {
                return flags2 | ((masked << 1) & (-789517)) | ((789516 & (masked << 1)) << 2);
            }
            return flags2 | (masked << 2);
        }

        public static int makeMovementFlags(int dragFlags, int swipeFlags) {
            return makeFlag(0, swipeFlags | dragFlags) | makeFlag(1, swipeFlags) | makeFlag(2, dragFlags);
        }

        public static int makeFlag(int actionState, int directions) {
            return directions << (actionState * 8);
        }

        public int convertToAbsoluteDirection(int flags, int layoutDirection) {
            int masked = flags & 3158064;
            if (masked == 0) {
                return flags;
            }
            int flags2 = flags & (~masked);
            if (layoutDirection != 0) {
                return flags2 | ((masked >> 1) & (-3158065)) | ((3158064 & (masked >> 1)) >> 2);
            }
            return flags2 | (masked >> 2);
        }

        final int getAbsoluteMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            int flags = getMovementFlags(recyclerView, viewHolder);
            return convertToAbsoluteDirection(flags, ViewCompat.getLayoutDirection(recyclerView));
        }

        boolean hasDragFlag(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            int flags = getAbsoluteMovementFlags(recyclerView, viewHolder);
            return (16711680 & flags) != 0;
        }

        public boolean canDropOver(RecyclerView recyclerView, RecyclerView.ViewHolder current, RecyclerView.ViewHolder target) {
            return true;
        }

        public boolean isLongPressDragEnabled() {
            return true;
        }

        public boolean isItemViewSwipeEnabled() {
            return true;
        }

        public int getBoundingBoxMargin() {
            return 0;
        }

        public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
            return 0.5f;
        }

        public float getMoveThreshold(RecyclerView.ViewHolder viewHolder) {
            return 0.5f;
        }

        public float getSwipeEscapeVelocity(float defaultValue) {
            return defaultValue;
        }

        public float getSwipeVelocityThreshold(float defaultValue) {
            return defaultValue;
        }

        public RecyclerView.ViewHolder chooseDropTarget(RecyclerView.ViewHolder selected, List<RecyclerView.ViewHolder> dropTargets, int curX, int curY) {
            int diff;
            int score;
            int diff2;
            int score2;
            int diff3;
            int score3;
            int diff4;
            int score4;
            int right = curX + selected.itemView.getWidth();
            int bottom = curY + selected.itemView.getHeight();
            RecyclerView.ViewHolder winner = null;
            int winnerScore = -1;
            int dx = curX - selected.itemView.getLeft();
            int dy = curY - selected.itemView.getTop();
            int targetsSize = dropTargets.size();
            for (int i = 0; i < targetsSize; i++) {
                RecyclerView.ViewHolder target = dropTargets.get(i);
                if (dx > 0 && (diff4 = target.itemView.getRight() - right) < 0 && target.itemView.getRight() > selected.itemView.getRight() && (score4 = Math.abs(diff4)) > winnerScore) {
                    winnerScore = score4;
                    winner = target;
                }
                if (dx < 0 && (diff3 = target.itemView.getLeft() - curX) > 0 && target.itemView.getLeft() < selected.itemView.getLeft() && (score3 = Math.abs(diff3)) > winnerScore) {
                    winnerScore = score3;
                    winner = target;
                }
                if (dy < 0 && (diff2 = target.itemView.getTop() - curY) > 0 && target.itemView.getTop() < selected.itemView.getTop() && (score2 = Math.abs(diff2)) > winnerScore) {
                    winnerScore = score2;
                    winner = target;
                }
                if (dy > 0 && (diff = target.itemView.getBottom() - bottom) < 0 && target.itemView.getBottom() > selected.itemView.getBottom() && (score = Math.abs(diff)) > winnerScore) {
                    winnerScore = score;
                    winner = target;
                }
            }
            return winner;
        }

        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            if (viewHolder != null) {
                ItemTouchUIUtilImpl.INSTANCE.onSelected(viewHolder.itemView);
            }
        }

        private int getMaxDragScroll(RecyclerView recyclerView) {
            if (this.mCachedMaxScrollSpeed == -1) {
                this.mCachedMaxScrollSpeed = recyclerView.getResources().getDimensionPixelSize(R.dimen.item_touch_helper_max_drag_scroll_per_frame);
            }
            return this.mCachedMaxScrollSpeed;
        }

        public void onMoved(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, int fromPos, RecyclerView.ViewHolder target, int toPos, int x, int y) {
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            if (layoutManager instanceof ViewDropHandler) {
                ((ViewDropHandler) layoutManager).prepareForDrop(viewHolder.itemView, target.itemView, x, y);
                return;
            }
            if (layoutManager.canScrollHorizontally()) {
                int minLeft = layoutManager.getDecoratedLeft(target.itemView);
                if (minLeft <= recyclerView.getPaddingLeft()) {
                    recyclerView.scrollToPosition(toPos);
                }
                int maxRight = layoutManager.getDecoratedRight(target.itemView);
                if (maxRight >= recyclerView.getWidth() - recyclerView.getPaddingRight()) {
                    recyclerView.scrollToPosition(toPos);
                }
            }
            if (layoutManager.canScrollVertically()) {
                int minTop = layoutManager.getDecoratedTop(target.itemView);
                if (minTop <= recyclerView.getPaddingTop()) {
                    recyclerView.scrollToPosition(toPos);
                }
                int maxBottom = layoutManager.getDecoratedBottom(target.itemView);
                if (maxBottom >= recyclerView.getHeight() - recyclerView.getPaddingBottom()) {
                    recyclerView.scrollToPosition(toPos);
                }
            }
        }

        void onDraw(Canvas c, RecyclerView parent, RecyclerView.ViewHolder selected, List<RecoverAnimation> recoverAnimationList, int actionState, float dX, float dY) {
            int recoverAnimSize = recoverAnimationList.size();
            int i = 0;
            while (true) {
                int i2 = i;
                if (i2 >= recoverAnimSize) {
                    break;
                }
                RecoverAnimation anim = recoverAnimationList.get(i2);
                anim.update();
                int count = c.save();
                onChildDraw(c, parent, anim.mViewHolder, anim.mX, anim.mY, anim.mActionState, false);
                c.restoreToCount(count);
                i = i2 + 1;
            }
            if (selected != null) {
                int count2 = c.save();
                onChildDraw(c, parent, selected, dX, dY, actionState, true);
                c.restoreToCount(count2);
            }
        }

        void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.ViewHolder selected, List<RecoverAnimation> recoverAnimationList, int actionState, float dX, float dY) {
            int recoverAnimSize = recoverAnimationList.size();
            int i = 0;
            while (true) {
                int i2 = i;
                if (i2 >= recoverAnimSize) {
                    break;
                }
                RecoverAnimation anim = recoverAnimationList.get(i2);
                int count = c.save();
                onChildDrawOver(c, parent, anim.mViewHolder, anim.mX, anim.mY, anim.mActionState, false);
                c.restoreToCount(count);
                i = i2 + 1;
            }
            if (selected != null) {
                int count2 = c.save();
                onChildDrawOver(c, parent, selected, dX, dY, actionState, true);
                c.restoreToCount(count2);
            }
            boolean hasRunningAnimation = false;
            for (int i3 = recoverAnimSize - 1; i3 >= 0; i3--) {
                RecoverAnimation anim2 = recoverAnimationList.get(i3);
                if (anim2.mEnded && !anim2.mIsPendingCleanup) {
                    recoverAnimationList.remove(i3);
                } else if (!anim2.mEnded) {
                    hasRunningAnimation = true;
                }
            }
            if (hasRunningAnimation) {
                parent.invalidate();
            }
        }

        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            ItemTouchUIUtilImpl.INSTANCE.clearView(viewHolder.itemView);
        }

        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            ItemTouchUIUtilImpl.INSTANCE.onDraw(c, recyclerView, viewHolder.itemView, dX, dY, actionState, isCurrentlyActive);
        }

        public void onChildDrawOver(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            ItemTouchUIUtilImpl.INSTANCE.onDrawOver(c, recyclerView, viewHolder.itemView, dX, dY, actionState, isCurrentlyActive);
        }

        public long getAnimationDuration(RecyclerView recyclerView, int animationType, float animateDx, float animateDy) {
            RecyclerView.ItemAnimator itemAnimator = recyclerView.getItemAnimator();
            return itemAnimator == null ? animationType == 8 ? 200L : 250L : animationType == 8 ? itemAnimator.getMoveDuration() : itemAnimator.getRemoveDuration();
        }

        public int interpolateOutOfBoundsScroll(RecyclerView recyclerView, int viewSize, int viewSizeOutOfBounds, int totalSize, long msSinceStartScroll) {
            float timeRatio;
            int maxScroll = getMaxDragScroll(recyclerView);
            int absOutOfBounds = Math.abs(viewSizeOutOfBounds);
            int direction = (int) Math.signum(viewSizeOutOfBounds);
            float outOfBoundsRatio = Math.min(1.0f, (absOutOfBounds * 1.0f) / viewSize);
            int cappedScroll = (int) (direction * maxScroll * sDragViewScrollCapInterpolator.getInterpolation(outOfBoundsRatio));
            if (msSinceStartScroll > 2000) {
                timeRatio = 1.0f;
            } else {
                float timeRatio2 = msSinceStartScroll;
                timeRatio = timeRatio2 / 2000.0f;
            }
            int value = (int) (cappedScroll * sDragScrollInterpolator.getInterpolation(timeRatio));
            if (value == 0) {
                return viewSizeOutOfBounds > 0 ? 1 : -1;
            }
            return value;
        }
    }

    public static abstract class SimpleCallback extends Callback {
        private int mDefaultDragDirs;
        private int mDefaultSwipeDirs;

        public SimpleCallback(int dragDirs, int swipeDirs) {
            this.mDefaultSwipeDirs = swipeDirs;
            this.mDefaultDragDirs = dragDirs;
        }

        public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return this.mDefaultSwipeDirs;
        }

        public int getDragDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return this.mDefaultDragDirs;
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return makeMovementFlags(getDragDirs(recyclerView, viewHolder), getSwipeDirs(recyclerView, viewHolder));
        }
    }

    private class ItemTouchHelperGestureListener extends GestureDetector.SimpleOnGestureListener {
        private boolean mShouldReactToLongPress = true;

        ItemTouchHelperGestureListener() {
        }

        void doNotReactToLongPress() {
            this.mShouldReactToLongPress = false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            View child;
            RecyclerView.ViewHolder vh;
            if (!this.mShouldReactToLongPress || (child = ItemTouchHelper.this.findChildView(e)) == null || (vh = ItemTouchHelper.this.mRecyclerView.getChildViewHolder(child)) == null || !ItemTouchHelper.this.mCallback.hasDragFlag(ItemTouchHelper.this.mRecyclerView, vh)) {
                return;
            }
            int pointerId = e.getPointerId(0);
            if (pointerId == ItemTouchHelper.this.mActivePointerId) {
                int index = e.findPointerIndex(ItemTouchHelper.this.mActivePointerId);
                float x = e.getX(index);
                float y = e.getY(index);
                ItemTouchHelper.this.mInitialTouchX = x;
                ItemTouchHelper.this.mInitialTouchY = y;
                ItemTouchHelper.this.mDx = ItemTouchHelper.this.mDy = 0.0f;
                if (ItemTouchHelper.this.mCallback.isLongPressDragEnabled()) {
                    ItemTouchHelper.this.select(vh, 2);
                }
            }
        }
    }

    private static class RecoverAnimation implements Animator.AnimatorListener {
        final int mActionState;
        final int mAnimationType;
        private float mFraction;
        boolean mIsPendingCleanup;
        final float mStartDx;
        final float mStartDy;
        final float mTargetX;
        final float mTargetY;
        final RecyclerView.ViewHolder mViewHolder;
        float mX;
        float mY;
        boolean mOverridden = false;
        boolean mEnded = false;
        private final ValueAnimator mValueAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);

        RecoverAnimation(RecyclerView.ViewHolder viewHolder, int animationType, int actionState, float startDx, float startDy, float targetX, float targetY) {
            this.mActionState = actionState;
            this.mAnimationType = animationType;
            this.mViewHolder = viewHolder;
            this.mStartDx = startDx;
            this.mStartDy = startDy;
            this.mTargetX = targetX;
            this.mTargetY = targetY;
            this.mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    RecoverAnimation.this.setFraction(animation.getAnimatedFraction());
                }
            });
            this.mValueAnimator.setTarget(viewHolder.itemView);
            this.mValueAnimator.addListener(this);
            setFraction(0.0f);
        }

        public void setDuration(long duration) {
            this.mValueAnimator.setDuration(duration);
        }

        public void start() {
            this.mViewHolder.setIsRecyclable(false);
            this.mValueAnimator.start();
        }

        public void cancel() {
            this.mValueAnimator.cancel();
        }

        public void setFraction(float fraction) {
            this.mFraction = fraction;
        }

        public void update() {
            if (this.mStartDx == this.mTargetX) {
                this.mX = this.mViewHolder.itemView.getTranslationX();
            } else {
                this.mX = this.mStartDx + (this.mFraction * (this.mTargetX - this.mStartDx));
            }
            if (this.mStartDy == this.mTargetY) {
                this.mY = this.mViewHolder.itemView.getTranslationY();
            } else {
                this.mY = this.mStartDy + (this.mFraction * (this.mTargetY - this.mStartDy));
            }
        }

        @Override
        public void onAnimationStart(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (!this.mEnded) {
                this.mViewHolder.setIsRecyclable(true);
            }
            this.mEnded = true;
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            setFraction(1.0f);
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }
    }
}
