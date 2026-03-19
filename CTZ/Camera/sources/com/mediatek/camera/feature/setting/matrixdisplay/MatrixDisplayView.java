package com.mediatek.camera.feature.setting.matrixdisplay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.StateSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EdgeEffect;
import android.widget.LinearLayout;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import java.util.LinkedList;

public class MatrixDisplayView extends ViewGroup {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(MatrixDisplayView.class.getSimpleName());
    private BaseAdapter mAdapter;
    private int mBottomPosition;
    private int mColumnCount;
    private int mColumnHeight;
    private int mColumnWidth;
    private float mDensity;
    private int mDispalyHeight;
    private int mDisplayWidth;
    private int mDownPointX;
    private int mDownPointY;
    private int mDownRow;
    private long mDownTime;
    private EdgeEffect mEdgeGlowBottom;
    private EdgeEffect mEdgeGlowTop;
    private int mEventState;
    private Handler mHandler;
    private OnItemClickListener mItemClickListener;
    private int mLastMoveDistance;
    private int mLastTopRow;
    private int mMotionY;
    private long mMoveTime;
    private boolean mNeedScrollOut;
    private boolean mNeedUpdateView;
    private OnScrollListener mOnScrollListener;
    private int mPressX;
    private int mPressY;
    private View mPressedView;
    private LinkedList<View> mRecycleView;
    private int mSelectedPosition;
    private View mSelectedView;
    private int mSelectionBottomPadding;
    private int mSelectionLeftPadding;
    private int mSelectionRightPadding;
    private int mSelectionTopPadding;
    private Drawable mSelectorDrawable;
    private Rect mSelectorRect;
    private int mTop;
    private int mTopPosition;
    private boolean mTouchFocused;
    private int mUpPointX;
    private int mUpPointY;
    private long mUpTime;

    public interface OnItemClickListener {
        boolean onItemClick(View view, int i);
    }

    public interface OnScrollListener {
        void onScrollDone(MatrixDisplayView matrixDisplayView, int i, int i2);

        void onScrollOut(MatrixDisplayView matrixDisplayView, int i);
    }

    public MatrixDisplayView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mEventState = 2;
        this.mRecycleView = new LinkedList<>();
        this.mSelectorRect = new Rect();
        this.mLastTopRow = -1;
        this.mSelectionLeftPadding = 0;
        this.mSelectionTopPadding = 0;
        this.mSelectionRightPadding = 0;
        this.mSelectionBottomPadding = 0;
        this.mDensity = 0.0f;
        this.mNeedScrollOut = false;
        this.mNeedUpdateView = false;
        this.mTouchFocused = false;
        this.mDensity = context.getResources().getDisplayMetrics().density;
        this.mSelectorDrawable = getResources().getDrawable(R.drawable.bg_pressed);
        this.mSelectorDrawable.setCallback(this);
        setWillNotDraw(false);
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 101:
                        int[] iArr = (int[]) message.obj;
                        MatrixDisplayView.this.scrollViewByDistance(iArr[0], iArr[1], iArr[2]);
                        MatrixDisplayView.this.showSelectedBorder(MatrixDisplayView.this.mSelectedPosition);
                        break;
                    case 102:
                        MatrixDisplayView.this.onItemClick((View) message.obj);
                        break;
                    case 103:
                        MatrixDisplayView.this.scrollDone();
                        break;
                    default:
                        LogHelper.d(MatrixDisplayView.TAG, "[handleMessage]unrecognize message!");
                        break;
                }
            }
        };
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        LogHelper.d(TAG, "[onLayout], left:" + i + ", top:" + i2 + ", right:" + i3 + ", bottom:" + i4);
        int childCount = getChildCount();
        for (int i5 = 0; i5 < childCount; i5++) {
            View childAt = getChildAt(i5);
            childAt.layout(childAt.getLeft(), childAt.getTop(), childAt.getRight(), childAt.getBottom());
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        LogHelper.d(TAG, "action = " + motionEvent.getAction() + ", mTopPosition = " + this.mTopPosition + ", X = " + motionEvent.getX() + ", Y = " + motionEvent.getY());
        switch (motionEvent.getAction()) {
            case 0:
                return doActionDown(motionEvent);
            case Camera2Proxy.TEMPLATE_PREVIEW:
                return doActionUp(motionEvent);
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                return doActionMove(motionEvent);
            case Camera2Proxy.TEMPLATE_RECORD:
                this.mSelectorRect.setEmpty();
                releaseEdgeEffects();
                invalidate();
                return false;
            default:
                return false;
        }
    }

    @Override
    protected void onMeasure(int i, int i2) {
        super.onMeasure(View.MeasureSpec.makeMeasureSpec(this.mDisplayWidth, 1073741824), View.MeasureSpec.makeMeasureSpec(this.mDispalyHeight, 1073741824));
        LogHelper.d(TAG, "[onMeasure]");
        int childCount = getChildCount();
        for (int i3 = 0; i3 < childCount; i3++) {
            getChildAt(i3).measure(this.mColumnWidth, this.mColumnHeight);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        LogHelper.d(TAG, "[onDraw]");
        if (this.mNeedScrollOut) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    LogHelper.d(MatrixDisplayView.TAG, "onScrollOut");
                    if (MatrixDisplayView.this.mOnScrollListener != null) {
                        MatrixDisplayView.this.mOnScrollListener.onScrollOut(MatrixDisplayView.this, 1);
                    }
                }
            });
            this.mNeedScrollOut = false;
        }
    }

    public void setLayoutSize(int i, int i2) {
        if (this.mDisplayWidth != i || this.mDispalyHeight != i2) {
            this.mDisplayWidth = i;
            this.mDispalyHeight = i2;
            requestLayout();
        }
    }

    public void setGridCountInColumn(int i) {
        this.mColumnCount = i;
    }

    public void setGridWidth(int i) {
        this.mColumnWidth = i;
    }

    public void setGridHeight(int i) {
        this.mColumnHeight = i;
    }

    public void scrollToSelectedPosition(int i) {
        int i2 = i / 3;
        if (i2 <= 1) {
            if (i2 <= 1) {
                scrollViewByDistance(this.mTopPosition, 0, this.mColumnHeight / 2);
            }
        } else {
            scrollViewByDistance(this.mTopPosition, -((i2 - 1) * this.mColumnHeight), this.mColumnHeight / 2);
        }
    }

    public void setAdapter(BaseAdapter baseAdapter) {
        this.mAdapter = baseAdapter;
        this.mBottomPosition = this.mColumnHeight * (baseAdapter.getCount() / this.mColumnCount);
        if (baseAdapter.getCount() % this.mColumnCount != 0) {
            this.mBottomPosition += this.mColumnHeight;
        }
        bindView();
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mItemClickListener = onItemClickListener;
    }

    public void setOnScrollListener(OnScrollListener onScrollListener) {
        this.mOnScrollListener = onScrollListener;
    }

    public void setSelector(int i) {
        setSelector(getResources().getDrawable(i));
    }

    public void showSelectedBorder(int i) {
        ViewGroup viewGroup;
        View childAt;
        clearPressedState();
        this.mSelectedPosition = i;
        if (isSelectedViewInSight() && (viewGroup = (ViewGroup) getChildAt((i / 3) % 4)) != null && (childAt = viewGroup.getChildAt(i % 3)) != null) {
            this.mSelectedView = childAt;
            childAt.setBackgroundResource(R.drawable.selected_border);
        }
    }

    @Override
    public void setOverScrollMode(int i) {
        if (i != 2) {
            if (this.mEdgeGlowTop == null) {
                Context context = getContext();
                this.mEdgeGlowTop = new EdgeEffect(context);
                this.mEdgeGlowBottom = new EdgeEffect(context);
            }
        } else {
            this.mEdgeGlowTop = null;
            this.mEdgeGlowBottom = null;
        }
        super.setOverScrollMode(i);
    }

    private void setSelector(Drawable drawable) {
        if (this.mSelectorDrawable != null) {
            this.mSelectorDrawable.setCallback(null);
            unscheduleDrawable(this.mSelectorDrawable);
        }
        this.mSelectorDrawable = drawable;
        Rect rect = new Rect();
        drawable.getPadding(rect);
        this.mSelectionLeftPadding = rect.left;
        this.mSelectionTopPadding = rect.top;
        this.mSelectionRightPadding = rect.right;
        this.mSelectionBottomPadding = rect.bottom;
        drawable.setCallback(this);
        updateSelectorState();
    }

    private void scrollViewByDistance(int i, int i2, int i3) {
        boolean z;
        int i4;
        LogHelper.d(TAG, "scrollViewByDistance(), startPoint:" + i + ", endPoint:" + i2 + ",step:" + i3 + ", mEventState:" + this.mEventState);
        int i5 = i2 - i;
        if (i5 <= 0) {
            int i6 = i5 + i3;
            if (i6 > 0) {
                z = false;
            } else {
                z = true;
            }
            if (i6 <= 0) {
                i5 = -i3;
            }
            scrollViewByDistance(i5);
            i4 = i - i3;
        } else {
            int i7 = i5 - i3;
            if (i7 < 0) {
                z = false;
            } else {
                z = true;
            }
            if (i7 >= 0) {
                i5 = i3;
            }
            scrollViewByDistance(i5);
            i4 = i + i3;
        }
        int[] iArr = {i4, i2, i3};
        if (this.mEventState == 2) {
            if (z) {
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(101, iArr), 10L);
            } else {
                this.mHandler.sendEmptyMessage(103);
            }
        }
    }

    private void scrollViewByDistance(int i) {
        if ((i < 0 || this.mTopPosition > 0) && i < 0 && this.mBottomPosition >= this.mColumnHeight * 3 && this.mBottomPosition + i < this.mColumnHeight * 3) {
            this.mEdgeGlowBottom.onPull(i / getHeight());
            if (!this.mEdgeGlowTop.isFinished()) {
                this.mEdgeGlowTop.onRelease();
            }
            if (!this.mEdgeGlowTop.isFinished() || !this.mEdgeGlowBottom.isFinished()) {
                postInvalidateOnAnimation();
            }
            i = (this.mColumnHeight * 3) - this.mBottomPosition;
        }
        LogHelper.d(TAG, "mTopPosition = " + this.mTopPosition + "deltaY = " + i);
        if (this.mTopPosition < 0 && Math.abs(i) > this.mColumnHeight) {
            if (i > 0) {
                i -= this.mColumnHeight;
            } else {
                i += this.mColumnHeight;
            }
        }
        this.mTopPosition += i;
        this.mBottomPosition += i;
        scrollBy(0, -i);
        if (this.mTopPosition <= 0) {
            scrollView(this.mTopPosition);
        }
        if (this.mTopPosition >= this.mDispalyHeight) {
            this.mNeedScrollOut = true;
        }
    }

    private boolean isSelectedViewInSight() {
        int i = ((this.mSelectedPosition / 3) + 1) * this.mColumnHeight;
        return i >= (-this.mTopPosition) && i <= (-this.mTopPosition) + this.mDispalyHeight;
    }

    private void onItemClick(View view) {
        boolean zOnItemClick;
        if (this.mPressedView == null) {
            return;
        }
        int iPointToPosition = pointToPosition(this.mUpPointX, this.mUpPointY);
        if (iPointToPosition >= this.mAdapter.getCount()) {
            iPointToPosition = this.mAdapter.getCount() - 1;
        }
        if (this.mItemClickListener != null) {
            zOnItemClick = this.mItemClickListener.onItemClick(view, iPointToPosition);
        } else {
            zOnItemClick = false;
        }
        if (!zOnItemClick) {
            return;
        }
        this.mSelectorRect.setEmpty();
        setPressed(false);
        this.mPressedView.setPressed(false);
        clearPressedState();
        this.mSelectedView = view;
        view.setBackgroundResource(R.drawable.selected_border);
        this.mSelectedPosition = iPointToPosition;
    }

    private void clearPressedState() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            LinearLayout linearLayout = (LinearLayout) getChildAt(i);
            int childCount2 = linearLayout.getChildCount();
            for (int i2 = 0; i2 < childCount2; i2++) {
                linearLayout.getChildAt(i2).setBackgroundResource(R.drawable.unselected_border);
            }
        }
    }

    private int pointToPosition(int i, int i2) {
        int iComputeCurrentRow = computeCurrentRow(i2);
        return (iComputeCurrentRow * this.mColumnCount) + computeCurrentColumn(i);
    }

    private int computeCurrentRow(int i) {
        return (i - this.mTopPosition) / this.mColumnHeight;
    }

    private int computeCurrentColumn(int i) {
        return i / this.mColumnWidth;
    }

    private void scrollView(int i) {
        int i2 = -i;
        int i3 = i2 / this.mColumnHeight;
        if (this.mLastTopRow == i3) {
            scrollViewInCurPage(i3);
            return;
        }
        LogHelper.d(TAG, "topRow:" + i3 + ", mLastTopRow:" + this.mLastTopRow + ",topPosition:" + i);
        int i4 = i2 % this.mColumnHeight;
        if (this.mLastTopRow > i3) {
            scrollViewToPrvPage(i3, 0);
        } else {
            scrollViewToNextPage(i3, 0, i4);
        }
        this.mLastTopRow = i3;
    }

    private void scrollViewInCurPage(int i) {
        int count = this.mAdapter.getCount();
        int i2 = i + 3;
        final LinearLayout linearLayout = (LinearLayout) getChildAt(i2 % 4);
        if (this.mNeedUpdateView) {
            int i3 = i2 * 3;
            for (int i4 = i3; i4 < this.mColumnCount + i3 && i4 < count; i4++) {
                this.mAdapter.getView(i4, this.mRecycleView.get(i4 % 12), linearLayout);
            }
            this.mNeedUpdateView = false;
        }
        this.mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (linearLayout != null) {
                    linearLayout.setAlpha(1.0f);
                }
            }
        }, 150L);
    }

    private void scrollViewToPrvPage(int i, int i2) {
        final LinearLayout linearLayout = (LinearLayout) getChildAt(i);
        if (linearLayout != null) {
            linearLayout.layout(0, this.mColumnHeight * i, this.mColumnWidth * this.mColumnCount, (i + 1) * this.mColumnHeight);
            this.mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (linearLayout != null) {
                        linearLayout.setAlpha(1.0f);
                    }
                }
            }, 150L);
        }
        int i3 = i * 3;
        for (int i4 = i3; i4 < this.mColumnCount + i3; i4++) {
            this.mAdapter.getView(i4, this.mRecycleView.get(i4), linearLayout);
            this.mRecycleView.get(i4).setVisibility(0);
            this.mRecycleView.get(i4).setClickable(true);
        }
    }

    private void scrollViewToNextPage(int i, int i2, int i3) {
        int count = this.mAdapter.getCount();
        if (i3 >= 0) {
            if (i <= 0) {
                if (i == 0) {
                    LinearLayout linearLayout = (LinearLayout) getChildAt(3);
                    for (int i4 = 9; i4 < this.mColumnCount + 9 && i4 < count; i4++) {
                        this.mAdapter.getView(i4, this.mRecycleView.get(i4), linearLayout);
                    }
                    return;
                }
                return;
            }
            int i5 = i - 1;
            int i6 = i5 + 4;
            final LinearLayout linearLayout2 = (LinearLayout) getChildAt(i5);
            if (linearLayout2 != null) {
                linearLayout2.layout(0, this.mColumnHeight * i6, this.mColumnWidth * this.mColumnCount, (i6 + 1) * this.mColumnHeight);
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (linearLayout2 != null) {
                            linearLayout2.setAlpha(0.0f);
                        }
                    }
                });
            }
            int i7 = i5 - 1;
            if (i7 >= 0) {
                final LinearLayout linearLayout3 = (LinearLayout) getChildAt(i7);
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (linearLayout2 != null) {
                            linearLayout3.setAlpha(1.0f);
                        }
                    }
                });
            }
            int i8 = i6 * 3;
            int i9 = i5 * 3;
            for (int i10 = i8; i10 < this.mColumnCount + i8 && i10 < count; i10++) {
                this.mAdapter.getView(i10, this.mRecycleView.get(i9), linearLayout2);
                i9++;
            }
            int i11 = (i8 + this.mColumnCount) - count;
            for (int i12 = 0; i12 < i11; i12++) {
                View childAt = null;
                if (linearLayout2 != null) {
                    childAt = linearLayout2.getChildAt((this.mColumnCount - i12) - 1);
                }
                if (childAt != null) {
                    childAt.setVisibility(4);
                    childAt.setClickable(false);
                }
            }
        }
    }

    private void scrollDone() {
        if (this.mTopPosition <= 0) {
            int i = (-this.mTopPosition) / this.mColumnHeight;
            if (Math.abs(this.mTopPosition % this.mColumnHeight) >= this.mColumnHeight / 2) {
                i++;
            }
            int i2 = i * 3;
            int count = i2 + 9;
            if (count > this.mAdapter.getCount()) {
                count = this.mAdapter.getCount();
            }
            if (this.mOnScrollListener != null) {
                this.mOnScrollListener.onScrollDone(this, i2, count);
            }
            this.mNeedUpdateView = true;
        }
    }

    private void releaseEdgeEffects() {
        if (this.mEdgeGlowTop != null) {
            this.mEdgeGlowTop.onRelease();
            this.mEdgeGlowBottom.onRelease();
        }
    }

    private View getViewByPosition(int i, int i2) {
        int iComputeCurrentRow = computeCurrentRow(i2);
        int iComputeCurrentColumn = computeCurrentColumn(i);
        LinearLayout linearLayout = (LinearLayout) getChildAt(iComputeCurrentRow % 4);
        if (linearLayout != null) {
            return linearLayout.getChildAt(iComputeCurrentColumn);
        }
        return null;
    }

    private boolean isTouchFocus(int i, int i2, boolean z) {
        int iAbs = Math.abs(i - this.mPressX);
        int iAbs2 = Math.abs(i2 - this.mPressY);
        int i3 = (int) ((8.0f * this.mDensity) + 0.5f);
        if (z) {
            long j = this.mMoveTime - this.mDownTime;
            LogHelper.d(TAG, "Moving, distanceX:" + iAbs + ", distanceY:" + iAbs2 + ", max:" + i3 + ", timeInterval:" + j);
            return iAbs < i3 && iAbs2 < i3 && j > 100;
        }
        long j2 = this.mUpTime - this.mDownTime;
        LogHelper.d(TAG, "Up, distanceX:" + iAbs + ", distanceY:" + iAbs2 + ", max:" + i3 + ", timeInterval:" + j2);
        return iAbs < i3 && iAbs2 < i3;
    }

    private void positionSelector(int i, View view) {
        Rect rect = this.mSelectorRect;
        rect.set(view.getLeft(), this.mColumnHeight * i, view.getRight(), (i + 1) * this.mColumnHeight);
        positionSelector(rect.left, rect.top, rect.right, rect.bottom);
        refreshDrawableState();
    }

    private void positionSelector(int i, int i2, int i3, int i4) {
        this.mSelectorRect.set(i - this.mSelectionLeftPadding, i2 - this.mSelectionTopPadding, i3 + this.mSelectionRightPadding, i4 + this.mSelectionBottomPadding);
    }

    private void bindView() {
        removeAllViews();
        this.mRecycleView.clear();
        int count = this.mAdapter.getCount();
        int i = (this.mDispalyHeight / this.mColumnHeight) * this.mColumnCount;
        int iMin = Math.min(count, this.mColumnCount + i);
        LogHelper.d(TAG, "bindView(), maxCountsInScreen:" + i + ", count:" + iMin);
        View[] viewArr = null;
        int i2 = 0;
        for (int i3 = 0; i3 < iMin; i3++) {
            if (i2 % this.mColumnCount == 0) {
                viewArr = new View[this.mColumnCount];
            }
            View view = this.mAdapter.getView(i3, null, null);
            if (view != null) {
                this.mRecycleView.add(view);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view2) {
                        MatrixDisplayView.this.mHandler.sendMessage(MatrixDisplayView.this.mHandler.obtainMessage(102, view2));
                    }
                });
                viewArr[i2] = view;
                i2++;
                if (i2 == this.mColumnCount) {
                    LinearLayout linearLayout = new LinearLayout(getContext());
                    linearLayout.setMotionEventSplittingEnabled(false);
                    addLayout(linearLayout, viewArr);
                    i2 = 0;
                } else if (i3 >= iMin - 1 && i2 > 0) {
                    addLayout(new LinearLayout(getContext()), viewArr);
                }
            }
        }
    }

    private void addLayout(LinearLayout linearLayout, View[] viewArr) {
        linearLayout.setOrientation(0);
        linearLayout.setLayoutDirection(0);
        addCell(linearLayout, viewArr);
        addView(linearLayout);
        linearLayout.measure(this.mColumnWidth * this.mColumnCount, this.mColumnHeight);
        linearLayout.layout(0, this.mTop, this.mColumnWidth * this.mColumnCount, this.mTop + this.mColumnHeight);
        this.mTop += this.mColumnHeight;
    }

    private void addCell(LinearLayout linearLayout, View[] viewArr) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(this.mColumnWidth, this.mColumnHeight);
        for (View view : viewArr) {
            if (view != null) {
                linearLayout.addView(view, layoutParams);
            }
        }
    }

    private void updateSelectorState() {
        if (this.mSelectorDrawable != null) {
            if (shouldShowSelector()) {
                this.mSelectorDrawable.setState(getDrawableState());
            } else {
                this.mSelectorDrawable.setState(StateSet.NOTHING);
            }
        }
    }

    private boolean shouldShowSelector() {
        return !isInTouchMode() || isPressed();
    }

    private boolean doActionDown(MotionEvent motionEvent) {
        this.mDownTime = System.currentTimeMillis();
        this.mPressX = (int) motionEvent.getX();
        int y = (int) motionEvent.getY();
        this.mMotionY = y;
        this.mPressY = y;
        if (this.mTopPosition < this.mPressY) {
            this.mEventState = 0;
            if (this.mHandler != null) {
                this.mHandler.removeMessages(101);
            }
            releaseEdgeEffects();
            clearPressedState();
            this.mPressedView = getViewByPosition(this.mPressX, this.mPressY);
            this.mDownRow = computeCurrentRow(this.mPressY);
            this.mDownPointX = this.mPressX;
            this.mDownPointY = this.mMotionY;
            return false;
        }
        return true;
    }

    private boolean doActionMove(MotionEvent motionEvent) {
        this.mEventState = 1;
        this.mMoveTime = System.currentTimeMillis();
        int x = (int) motionEvent.getX();
        int y = (int) motionEvent.getY();
        if (isTouchFocus(x, y, true) && !this.mTouchFocused && this.mPressedView != null) {
            this.mSelectorRect.setEmpty();
            setPressed(true);
            this.mPressedView.setPressed(true);
            positionSelector(this.mDownRow, this.mPressedView);
            this.mTouchFocused = true;
        } else if (!isTouchFocus(x, y, true) && this.mTouchFocused && this.mPressedView != null) {
            this.mSelectorRect.setEmpty();
            setPressed(false);
            this.mPressedView.setPressed(false);
            this.mTouchFocused = false;
        }
        int iAbs = y - this.mMotionY;
        this.mLastMoveDistance = iAbs;
        if (Math.abs(iAbs) > this.mColumnHeight) {
            iAbs = (this.mColumnHeight * iAbs) / Math.abs(iAbs);
        }
        if (Math.abs(iAbs) > 0) {
            scrollViewByDistance(iAbs);
        }
        this.mMotionY = y;
        return false;
    }

    private boolean doActionUp(MotionEvent motionEvent) {
        this.mEventState = 2;
        this.mUpTime = System.currentTimeMillis();
        this.mUpPointX = (int) motionEvent.getX();
        this.mUpPointY = (int) motionEvent.getY();
        if (isTouchFocus(this.mUpPointX, this.mUpPointY, false) && !this.mTouchFocused && this.mPressedView != null) {
            this.mSelectorRect.setEmpty();
            setPressed(true);
            this.mPressedView.setPressed(true);
            positionSelector(this.mDownRow, this.mPressedView);
        }
        releaseEdgeEffects();
        if (this.mTopPosition <= 0 || this.mTopPosition > this.mDispalyHeight / 2) {
            if (this.mTopPosition > this.mDispalyHeight / 2) {
                scrollViewByDistance(this.mTopPosition, this.mDispalyHeight, 50);
            }
        } else {
            scrollViewByDistance(this.mTopPosition, 0, 50);
        }
        if (this.mTopPosition < 0) {
            int i = this.mTopPosition % this.mColumnHeight;
            if (Math.abs(i) >= this.mColumnHeight / 2) {
                scrollViewByDistance(this.mTopPosition, this.mTopPosition - (this.mColumnHeight - Math.abs(i)), 20);
            } else {
                scrollViewByDistance(this.mTopPosition, this.mTopPosition - i, 20);
            }
        }
        LogHelper.d(TAG, "mUpPointX:" + this.mUpPointX + ", mUpPointY:" + this.mUpPointY + ", distance:" + Math.abs(this.mUpPointY - this.mDownPointY));
        if (Math.abs(this.mUpPointY - this.mDownPointY) > (this.mDensity * 8.0f) + 0.5f || Math.abs(this.mUpPointX - this.mDownPointX) > (8.0f * this.mDensity) + 0.5f) {
            if (isSelectedViewInSight() && this.mSelectedView != null) {
                this.mSelectedView.setBackgroundResource(R.drawable.selected_border);
            }
            invalidate();
            return true;
        }
        this.mTouchFocused = false;
        return false;
    }
}
