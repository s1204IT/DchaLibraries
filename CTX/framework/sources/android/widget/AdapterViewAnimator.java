package android.widget;

import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.RemotableViewMethod;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsAdapter;
import com.android.internal.R;
import java.util.ArrayList;
import java.util.HashMap;

public abstract class AdapterViewAnimator extends AdapterView<Adapter> implements RemoteViewsAdapter.RemoteAdapterConnectionCallback, Advanceable {
    private static final int DEFAULT_ANIMATION_DURATION = 200;
    private static final String TAG = "RemoteViewAnimator";
    static final int TOUCH_MODE_DOWN_IN_CURRENT_VIEW = 1;
    static final int TOUCH_MODE_HANDLED = 2;
    static final int TOUCH_MODE_NONE = 0;
    int mActiveOffset;
    Adapter mAdapter;
    boolean mAnimateFirstTime;
    int mCurrentWindowEnd;
    int mCurrentWindowStart;
    int mCurrentWindowStartUnbounded;
    AdapterView<Adapter>.AdapterDataSetObserver mDataSetObserver;
    boolean mDeferNotifyDataSetChanged;
    boolean mFirstTime;
    ObjectAnimator mInAnimation;
    boolean mLoopViews;
    int mMaxNumActiveViews;
    ObjectAnimator mOutAnimation;
    private Runnable mPendingCheckForTap;
    ArrayList<Integer> mPreviousViews;
    int mReferenceChildHeight;
    int mReferenceChildWidth;
    RemoteViewsAdapter mRemoteViewsAdapter;
    private int mRestoreWhichChild;
    private int mTouchMode;
    HashMap<Integer, ViewAndMetaData> mViewsMap;
    int mWhichChild;

    public AdapterViewAnimator(Context context) {
        this(context, null);
    }

    public AdapterViewAnimator(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public AdapterViewAnimator(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public AdapterViewAnimator(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mWhichChild = 0;
        this.mRestoreWhichChild = -1;
        this.mAnimateFirstTime = true;
        this.mActiveOffset = 0;
        this.mMaxNumActiveViews = 1;
        this.mViewsMap = new HashMap<>();
        this.mCurrentWindowStart = 0;
        this.mCurrentWindowEnd = -1;
        this.mCurrentWindowStartUnbounded = 0;
        this.mDeferNotifyDataSetChanged = false;
        this.mFirstTime = true;
        this.mLoopViews = true;
        this.mReferenceChildWidth = -1;
        this.mReferenceChildHeight = -1;
        this.mTouchMode = 0;
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.AdapterViewAnimator, i, i2);
        int resourceId = typedArrayObtainStyledAttributes.getResourceId(0, 0);
        if (resourceId > 0) {
            setInAnimation(context, resourceId);
        } else {
            setInAnimation(getDefaultInAnimation());
        }
        int resourceId2 = typedArrayObtainStyledAttributes.getResourceId(1, 0);
        if (resourceId2 > 0) {
            setOutAnimation(context, resourceId2);
        } else {
            setOutAnimation(getDefaultOutAnimation());
        }
        setAnimateFirstView(typedArrayObtainStyledAttributes.getBoolean(2, true));
        this.mLoopViews = typedArrayObtainStyledAttributes.getBoolean(3, false);
        typedArrayObtainStyledAttributes.recycle();
        initViewAnimator();
    }

    private void initViewAnimator() {
        this.mPreviousViews = new ArrayList<>();
    }

    class ViewAndMetaData {
        int adapterPosition;
        long itemId;
        int relativeIndex;
        View view;

        ViewAndMetaData(View view, int i, int i2, long j) {
            this.view = view;
            this.relativeIndex = i;
            this.adapterPosition = i2;
            this.itemId = j;
        }
    }

    void configureViewAnimator(int i, int i2) {
        this.mMaxNumActiveViews = i;
        this.mActiveOffset = i2;
        this.mPreviousViews.clear();
        this.mViewsMap.clear();
        removeAllViewsInLayout();
        this.mCurrentWindowStart = 0;
        this.mCurrentWindowEnd = -1;
    }

    void transformViewForTransition(int i, int i2, View view, boolean z) {
        if (i == -1) {
            this.mInAnimation.setTarget(view);
            this.mInAnimation.start();
        } else if (i2 == -1) {
            this.mOutAnimation.setTarget(view);
            this.mOutAnimation.start();
        }
    }

    ObjectAnimator getDefaultInAnimation() {
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat((Object) null, "alpha", 0.0f, 1.0f);
        objectAnimatorOfFloat.setDuration(200L);
        return objectAnimatorOfFloat;
    }

    ObjectAnimator getDefaultOutAnimation() {
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat((Object) null, "alpha", 1.0f, 0.0f);
        objectAnimatorOfFloat.setDuration(200L);
        return objectAnimatorOfFloat;
    }

    @RemotableViewMethod
    public void setDisplayedChild(int i) {
        setDisplayedChild(i, true);
    }

    private void setDisplayedChild(int i, boolean z) {
        if (this.mAdapter != null) {
            this.mWhichChild = i;
            if (i >= getWindowSize()) {
                this.mWhichChild = this.mLoopViews ? 0 : getWindowSize() - 1;
            } else if (i < 0) {
                this.mWhichChild = this.mLoopViews ? getWindowSize() - 1 : 0;
            }
            boolean z2 = getFocusedChild() != null;
            showOnly(this.mWhichChild, z);
            if (z2) {
                requestFocus(2);
            }
        }
    }

    void applyTransformForChildAtIndex(View view, int i) {
    }

    public int getDisplayedChild() {
        return this.mWhichChild;
    }

    public void showNext() {
        setDisplayedChild(this.mWhichChild + 1);
    }

    public void showPrevious() {
        setDisplayedChild(this.mWhichChild - 1);
    }

    int modulo(int i, int i2) {
        if (i2 > 0) {
            return ((i % i2) + i2) % i2;
        }
        return 0;
    }

    View getViewAtRelativeIndex(int i) {
        if (i >= 0 && i <= getNumActiveViews() - 1 && this.mAdapter != null) {
            int iModulo = modulo(this.mCurrentWindowStartUnbounded + i, getWindowSize());
            if (this.mViewsMap.get(Integer.valueOf(iModulo)) != null) {
                return this.mViewsMap.get(Integer.valueOf(iModulo)).view;
            }
            return null;
        }
        return null;
    }

    int getNumActiveViews() {
        if (this.mAdapter != null) {
            return Math.min(getCount() + 1, this.mMaxNumActiveViews);
        }
        return this.mMaxNumActiveViews;
    }

    int getWindowSize() {
        if (this.mAdapter != null) {
            int count = getCount();
            if (count <= getNumActiveViews() && this.mLoopViews) {
                return count * this.mMaxNumActiveViews;
            }
            return count;
        }
        return 0;
    }

    private ViewAndMetaData getMetaDataForChild(View view) {
        for (ViewAndMetaData viewAndMetaData : this.mViewsMap.values()) {
            if (viewAndMetaData.view == view) {
                return viewAndMetaData;
            }
        }
        return null;
    }

    ViewGroup.LayoutParams createOrReuseLayoutParams(View view) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams != null) {
            return layoutParams;
        }
        return new ViewGroup.LayoutParams(0, 0);
    }

    void refreshChildren() {
        if (this.mAdapter == null) {
            return;
        }
        for (int i = this.mCurrentWindowStart; i <= this.mCurrentWindowEnd; i++) {
            int iModulo = modulo(i, getWindowSize());
            View view = this.mAdapter.getView(modulo(i, getCount()), null, this);
            if (view.getImportantForAccessibility() == 0) {
                view.setImportantForAccessibility(1);
            }
            if (this.mViewsMap.containsKey(Integer.valueOf(iModulo))) {
                FrameLayout frameLayout = (FrameLayout) this.mViewsMap.get(Integer.valueOf(iModulo)).view;
                if (view != null) {
                    frameLayout.removeAllViewsInLayout();
                    frameLayout.addView(view);
                }
            }
        }
    }

    FrameLayout getFrameForChild() {
        return new FrameLayout(this.mContext);
    }

    void showOnly(int i, boolean z) {
        int count;
        int i2;
        int i3;
        int i4;
        boolean z2;
        int i5;
        int i6;
        int i7;
        int i8;
        int i9;
        int i10;
        if (this.mAdapter == null || (count = getCount()) == 0) {
            return;
        }
        boolean z3 = false;
        int i11 = 0;
        while (true) {
            i2 = -1;
            if (i11 >= this.mPreviousViews.size()) {
                break;
            }
            View view = this.mViewsMap.get(this.mPreviousViews.get(i11)).view;
            this.mViewsMap.remove(this.mPreviousViews.get(i11));
            view.clearAnimation();
            if (view instanceof ViewGroup) {
                ((ViewGroup) view).removeAllViewsInLayout();
            }
            applyTransformForChildAtIndex(view, -1);
            removeViewInLayout(view);
            i11++;
        }
        this.mPreviousViews.clear();
        int i12 = i - this.mActiveOffset;
        boolean z4 = true;
        int numActiveViews = (getNumActiveViews() + i12) - 1;
        int iMax = Math.max(0, i12);
        int iMin = Math.min(count - 1, numActiveViews);
        if (this.mLoopViews) {
            i4 = numActiveViews;
            i3 = i12;
        } else {
            i3 = iMax;
            i4 = iMin;
        }
        int iModulo = modulo(i3, getWindowSize());
        int iModulo2 = modulo(i4, getWindowSize());
        if (iModulo <= iModulo2) {
            z2 = false;
        } else {
            z2 = true;
        }
        for (Integer num : this.mViewsMap.keySet()) {
            if (((z2 || (num.intValue() >= iModulo && num.intValue() <= iModulo2)) && (!z2 || num.intValue() <= iModulo2 || num.intValue() >= iModulo)) ? z3 : true) {
                View view2 = this.mViewsMap.get(num).view;
                int i13 = this.mViewsMap.get(num).relativeIndex;
                this.mPreviousViews.add(num);
                transformViewForTransition(i13, -1, view2, z);
            }
            z3 = false;
        }
        if (i3 != this.mCurrentWindowStart || i4 != this.mCurrentWindowEnd || i12 != this.mCurrentWindowStartUnbounded) {
            int i14 = i3;
            while (i14 <= i4) {
                int iModulo3 = modulo(i14, getWindowSize());
                if (this.mViewsMap.containsKey(Integer.valueOf(iModulo3))) {
                    i5 = this.mViewsMap.get(Integer.valueOf(iModulo3)).relativeIndex;
                } else {
                    i5 = i2;
                }
                int i15 = i14 - i12;
                if ((!this.mViewsMap.containsKey(Integer.valueOf(iModulo3)) || this.mPreviousViews.contains(Integer.valueOf(iModulo3))) ? false : z4) {
                    View view3 = this.mViewsMap.get(Integer.valueOf(iModulo3)).view;
                    this.mViewsMap.get(Integer.valueOf(iModulo3)).relativeIndex = i15;
                    applyTransformForChildAtIndex(view3, i15);
                    transformViewForTransition(i5, i15, view3, z);
                    i6 = count;
                    i10 = i2;
                    i7 = i12;
                    i8 = i4;
                    i9 = iModulo3;
                } else {
                    int iModulo4 = modulo(i14, count);
                    View view4 = this.mAdapter.getView(iModulo4, null, this);
                    long itemId = this.mAdapter.getItemId(iModulo4);
                    FrameLayout frameForChild = getFrameForChild();
                    if (view4 != null) {
                        frameForChild.addView(view4);
                    }
                    i6 = count;
                    i7 = i12;
                    i8 = i4;
                    i9 = iModulo3;
                    this.mViewsMap.put(Integer.valueOf(iModulo3), new ViewAndMetaData(frameForChild, i15, iModulo4, itemId));
                    addChild(frameForChild);
                    applyTransformForChildAtIndex(frameForChild, i15);
                    i10 = -1;
                    transformViewForTransition(-1, i15, frameForChild, z);
                }
                this.mViewsMap.get(Integer.valueOf(i9)).view.bringToFront();
                i14++;
                i2 = i10;
                count = i6;
                i12 = i7;
                i4 = i8;
                z4 = true;
            }
            int i16 = count;
            this.mCurrentWindowStart = i3;
            this.mCurrentWindowEnd = i4;
            this.mCurrentWindowStartUnbounded = i12;
            if (this.mRemoteViewsAdapter != null) {
                this.mRemoteViewsAdapter.setVisibleRangeHint(modulo(this.mCurrentWindowStart, i16), modulo(this.mCurrentWindowEnd, i16));
            }
        }
        requestLayout();
        invalidate();
    }

    private void addChild(View view) {
        addViewInLayout(view, -1, createOrReuseLayoutParams(view));
        if (this.mReferenceChildWidth == -1 || this.mReferenceChildHeight == -1) {
            int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
            view.measure(iMakeMeasureSpec, iMakeMeasureSpec);
            this.mReferenceChildWidth = view.getMeasuredWidth();
            this.mReferenceChildHeight = view.getMeasuredHeight();
        }
    }

    void showTapFeedback(View view) {
        view.setPressed(true);
    }

    void hideTapFeedback(View view) {
        view.setPressed(false);
    }

    void cancelHandleClick() {
        View currentView = getCurrentView();
        if (currentView != null) {
            hideTapFeedback(currentView);
        }
        this.mTouchMode = 0;
    }

    final class CheckForTap implements Runnable {
        CheckForTap() {
        }

        @Override
        public void run() {
            if (AdapterViewAnimator.this.mTouchMode == 1) {
                AdapterViewAnimator.this.showTapFeedback(AdapterViewAnimator.this.getCurrentView());
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (action == 6) {
            return false;
        }
        boolean z = true;
        switch (action) {
            case 0:
                View currentView = getCurrentView();
                if (currentView == null || !isTransformedTouchPointInView(motionEvent.getX(), motionEvent.getY(), currentView, null)) {
                    return false;
                }
                if (this.mPendingCheckForTap == null) {
                    this.mPendingCheckForTap = new CheckForTap();
                }
                this.mTouchMode = 1;
                postDelayed(this.mPendingCheckForTap, ViewConfiguration.getTapTimeout());
                return false;
            case 1:
                if (this.mTouchMode == 1) {
                    final View currentView2 = getCurrentView();
                    final ViewAndMetaData metaDataForChild = getMetaDataForChild(currentView2);
                    if (currentView2 != null && isTransformedTouchPointInView(motionEvent.getX(), motionEvent.getY(), currentView2, null)) {
                        Handler handler = getHandler();
                        if (handler != null) {
                            handler.removeCallbacks(this.mPendingCheckForTap);
                        }
                        showTapFeedback(currentView2);
                        postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                AdapterViewAnimator.this.hideTapFeedback(currentView2);
                                AdapterViewAnimator.this.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (metaDataForChild != null) {
                                            AdapterViewAnimator.this.performItemClick(currentView2, metaDataForChild.adapterPosition, metaDataForChild.itemId);
                                        } else {
                                            AdapterViewAnimator.this.performItemClick(currentView2, 0, 0L);
                                        }
                                    }
                                });
                            }
                        }, ViewConfiguration.getPressedStateDuration());
                    } else {
                        z = false;
                    }
                }
                this.mTouchMode = 0;
                return z;
            case 2:
            default:
                return false;
            case 3:
                View currentView3 = getCurrentView();
                if (currentView3 != null) {
                    hideTapFeedback(currentView3);
                }
                this.mTouchMode = 0;
                return false;
        }
    }

    private void measureChildren() {
        int childCount = getChildCount();
        int measuredWidth = (getMeasuredWidth() - this.mPaddingLeft) - this.mPaddingRight;
        int measuredHeight = (getMeasuredHeight() - this.mPaddingTop) - this.mPaddingBottom;
        for (int i = 0; i < childCount; i++) {
            getChildAt(i).measure(View.MeasureSpec.makeMeasureSpec(measuredWidth, 1073741824), View.MeasureSpec.makeMeasureSpec(measuredHeight, 1073741824));
        }
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int i3;
        int size = View.MeasureSpec.getSize(i);
        int size2 = View.MeasureSpec.getSize(i2);
        int mode = View.MeasureSpec.getMode(i);
        int mode2 = View.MeasureSpec.getMode(i2);
        boolean z = (this.mReferenceChildWidth == -1 || this.mReferenceChildHeight == -1) ? false : true;
        if (mode2 != 0) {
            if (mode2 == Integer.MIN_VALUE && z) {
                int i4 = this.mReferenceChildHeight + this.mPaddingTop + this.mPaddingBottom;
                size2 = i4 > size2 ? size2 | 16777216 : i4;
            }
        } else {
            size2 = z ? this.mReferenceChildHeight + this.mPaddingTop + this.mPaddingBottom : 0;
        }
        if (mode == 0) {
            if (z) {
                i3 = this.mReferenceChildWidth + this.mPaddingLeft + this.mPaddingRight;
            } else {
                size = 0;
            }
        } else if (mode2 == Integer.MIN_VALUE && z) {
            i3 = this.mReferenceChildWidth + this.mPaddingLeft + this.mPaddingRight;
            size = i3 > size ? size | 16777216 : i3;
        }
        setMeasuredDimension(size, size2);
        measureChildren();
    }

    void checkForAndHandleDataChanged() {
        if (this.mDataChanged) {
            post(new Runnable() {
                @Override
                public void run() {
                    AdapterViewAnimator.this.handleDataChanged();
                    if (AdapterViewAnimator.this.mWhichChild >= AdapterViewAnimator.this.getWindowSize()) {
                        AdapterViewAnimator.this.mWhichChild = 0;
                        AdapterViewAnimator.this.showOnly(AdapterViewAnimator.this.mWhichChild, false);
                    } else if (AdapterViewAnimator.this.mOldItemCount != AdapterViewAnimator.this.getCount()) {
                        AdapterViewAnimator.this.showOnly(AdapterViewAnimator.this.mWhichChild, false);
                    }
                    AdapterViewAnimator.this.refreshChildren();
                    AdapterViewAnimator.this.requestLayout();
                }
            });
        }
        this.mDataChanged = false;
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        checkForAndHandleDataChanged();
        int childCount = getChildCount();
        for (int i5 = 0; i5 < childCount; i5++) {
            View childAt = getChildAt(i5);
            childAt.layout(this.mPaddingLeft, this.mPaddingTop, this.mPaddingLeft + childAt.getMeasuredWidth(), this.mPaddingTop + childAt.getMeasuredHeight());
        }
    }

    static class SavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            @Override
            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        int whichChild;

        SavedState(Parcelable parcelable, int i) {
            super(parcelable);
            this.whichChild = i;
        }

        private SavedState(Parcel parcel) {
            super(parcel);
            this.whichChild = parcel.readInt();
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(this.whichChild);
        }

        public String toString() {
            return "AdapterViewAnimator.SavedState{ whichChild = " + this.whichChild + " }";
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable parcelableOnSaveInstanceState = super.onSaveInstanceState();
        if (this.mRemoteViewsAdapter != null) {
            this.mRemoteViewsAdapter.saveRemoteViewsCache();
        }
        return new SavedState(parcelableOnSaveInstanceState, this.mWhichChild);
    }

    @Override
    public void onRestoreInstanceState(Parcelable parcelable) {
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(savedState.getSuperState());
        this.mWhichChild = savedState.whichChild;
        if (this.mRemoteViewsAdapter != null && this.mAdapter == null) {
            this.mRestoreWhichChild = this.mWhichChild;
        } else {
            setDisplayedChild(this.mWhichChild, false);
        }
    }

    public View getCurrentView() {
        return getViewAtRelativeIndex(this.mActiveOffset);
    }

    public ObjectAnimator getInAnimation() {
        return this.mInAnimation;
    }

    public void setInAnimation(ObjectAnimator objectAnimator) {
        this.mInAnimation = objectAnimator;
    }

    public ObjectAnimator getOutAnimation() {
        return this.mOutAnimation;
    }

    public void setOutAnimation(ObjectAnimator objectAnimator) {
        this.mOutAnimation = objectAnimator;
    }

    public void setInAnimation(Context context, int i) {
        setInAnimation((ObjectAnimator) AnimatorInflater.loadAnimator(context, i));
    }

    public void setOutAnimation(Context context, int i) {
        setOutAnimation((ObjectAnimator) AnimatorInflater.loadAnimator(context, i));
    }

    public void setAnimateFirstView(boolean z) {
        this.mAnimateFirstTime = z;
    }

    @Override
    public int getBaseline() {
        return getCurrentView() != null ? getCurrentView().getBaseline() : super.getBaseline();
    }

    @Override
    public Adapter getAdapter() {
        return this.mAdapter;
    }

    @Override
    public void setAdapter(Adapter adapter) {
        if (this.mAdapter != null && this.mDataSetObserver != null) {
            this.mAdapter.unregisterDataSetObserver(this.mDataSetObserver);
        }
        this.mAdapter = adapter;
        checkFocus();
        if (this.mAdapter != null) {
            this.mDataSetObserver = new AdapterView.AdapterDataSetObserver();
            this.mAdapter.registerDataSetObserver(this.mDataSetObserver);
            this.mItemCount = this.mAdapter.getCount();
        }
        setFocusable(true);
        this.mWhichChild = 0;
        showOnly(this.mWhichChild, false);
    }

    @RemotableViewMethod(asyncImpl = "setRemoteViewsAdapterAsync")
    public void setRemoteViewsAdapter(Intent intent) {
        setRemoteViewsAdapter(intent, false);
    }

    public Runnable setRemoteViewsAdapterAsync(Intent intent) {
        return new RemoteViewsAdapter.AsyncRemoteAdapterAction(this, intent);
    }

    @Override
    public void setRemoteViewsAdapter(Intent intent, boolean z) {
        if (this.mRemoteViewsAdapter != null && new Intent.FilterComparison(intent).equals(new Intent.FilterComparison(this.mRemoteViewsAdapter.getRemoteViewsServiceIntent()))) {
            return;
        }
        this.mDeferNotifyDataSetChanged = false;
        this.mRemoteViewsAdapter = new RemoteViewsAdapter(getContext(), intent, this, z);
        if (this.mRemoteViewsAdapter.isDataReady()) {
            setAdapter(this.mRemoteViewsAdapter);
        }
    }

    public void setRemoteViewsOnClickHandler(RemoteViews.OnClickHandler onClickHandler) {
        if (this.mRemoteViewsAdapter != null) {
            this.mRemoteViewsAdapter.setRemoteViewsOnClickHandler(onClickHandler);
        }
    }

    @Override
    public void setSelection(int i) {
        setDisplayedChild(i);
    }

    @Override
    public View getSelectedView() {
        return getViewAtRelativeIndex(this.mActiveOffset);
    }

    @Override
    public void deferNotifyDataSetChanged() {
        this.mDeferNotifyDataSetChanged = true;
    }

    @Override
    public boolean onRemoteAdapterConnected() {
        if (this.mRemoteViewsAdapter != this.mAdapter) {
            setAdapter(this.mRemoteViewsAdapter);
            if (this.mDeferNotifyDataSetChanged) {
                this.mRemoteViewsAdapter.notifyDataSetChanged();
                this.mDeferNotifyDataSetChanged = false;
            }
            if (this.mRestoreWhichChild > -1) {
                setDisplayedChild(this.mRestoreWhichChild, false);
                this.mRestoreWhichChild = -1;
            }
            return false;
        }
        if (this.mRemoteViewsAdapter == null) {
            return false;
        }
        this.mRemoteViewsAdapter.superNotifyDataSetChanged();
        return true;
    }

    @Override
    public void onRemoteAdapterDisconnected() {
    }

    @Override
    public void advance() {
        showNext();
    }

    @Override
    public void fyiWillBeAdvancedByHostKThx() {
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return AdapterViewAnimator.class.getName();
    }
}
