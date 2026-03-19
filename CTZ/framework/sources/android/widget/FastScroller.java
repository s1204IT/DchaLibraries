package android.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.IntProperty;
import android.util.MathUtils;
import android.util.Property;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;
import android.widget.ImageView;
import com.android.internal.R;

class FastScroller {
    private static final int DURATION_CROSS_FADE = 50;
    private static final int DURATION_FADE_IN = 150;
    private static final int DURATION_FADE_OUT = 300;
    private static final int DURATION_RESIZE = 100;
    private static final long FADE_TIMEOUT = 1500;
    private static final int MIN_PAGES = 4;
    private static final int OVERLAY_ABOVE_THUMB = 2;
    private static final int OVERLAY_AT_THUMB = 1;
    private static final int OVERLAY_FLOATING = 0;
    private static final int PREVIEW_LEFT = 0;
    private static final int PREVIEW_RIGHT = 1;
    private static final int STATE_DRAGGING = 2;
    private static final int STATE_NONE = 0;
    private static final int STATE_VISIBLE = 1;
    private static final int THUMB_POSITION_INSIDE = 1;
    private static final int THUMB_POSITION_MIDPOINT = 0;
    private boolean mAlwaysShow;
    private AnimatorSet mDecorAnimation;
    private boolean mEnabled;
    private int mFirstVisibleItem;
    private int mHeaderCount;
    private float mInitialTouchY;
    private boolean mLayoutFromRight;
    private final AbsListView mList;
    private Adapter mListAdapter;
    private boolean mLongList;
    private boolean mMatchDragPosition;
    private final int mMinimumTouchTarget;
    private int mOldChildCount;
    private int mOldItemCount;
    private final ViewGroupOverlay mOverlay;
    private int mOverlayPosition;
    private AnimatorSet mPreviewAnimation;
    private final View mPreviewImage;
    private int mPreviewMinHeight;
    private int mPreviewMinWidth;
    private int mPreviewPadding;
    private final TextView mPrimaryText;
    private int mScaledTouchSlop;
    private int mScrollBarStyle;
    private boolean mScrollCompleted;
    private final TextView mSecondaryText;
    private SectionIndexer mSectionIndexer;
    private Object[] mSections;
    private boolean mShowingPreview;
    private boolean mShowingPrimary;
    private int mState;
    private int mTextAppearance;
    private ColorStateList mTextColor;
    private float mTextSize;
    private Drawable mThumbDrawable;
    private final ImageView mThumbImage;
    private int mThumbMinHeight;
    private int mThumbMinWidth;
    private float mThumbOffset;
    private int mThumbPosition;
    private float mThumbRange;
    private Drawable mTrackDrawable;
    private final ImageView mTrackImage;
    private boolean mUpdatingLayout;
    private int mWidth;
    private static final long TAP_TIMEOUT = ViewConfiguration.getTapTimeout();
    private static Property<View, Integer> LEFT = new IntProperty<View>("left") {
        @Override
        public void setValue(View view, int i) {
            view.setLeft(i);
        }

        @Override
        public Integer get(View view) {
            return Integer.valueOf(view.getLeft());
        }
    };
    private static Property<View, Integer> TOP = new IntProperty<View>("top") {
        @Override
        public void setValue(View view, int i) {
            view.setTop(i);
        }

        @Override
        public Integer get(View view) {
            return Integer.valueOf(view.getTop());
        }
    };
    private static Property<View, Integer> RIGHT = new IntProperty<View>("right") {
        @Override
        public void setValue(View view, int i) {
            view.setRight(i);
        }

        @Override
        public Integer get(View view) {
            return Integer.valueOf(view.getRight());
        }
    };
    private static Property<View, Integer> BOTTOM = new IntProperty<View>("bottom") {
        @Override
        public void setValue(View view, int i) {
            view.setBottom(i);
        }

        @Override
        public Integer get(View view) {
            return Integer.valueOf(view.getBottom());
        }
    };
    private final Rect mTempBounds = new Rect();
    private final Rect mTempMargins = new Rect();
    private final Rect mContainerRect = new Rect();
    private final int[] mPreviewResId = new int[2];
    private int mCurrentSection = -1;
    private int mScrollbarPosition = -1;
    private long mPendingDrag = -1;
    private final Runnable mDeferHide = new Runnable() {
        @Override
        public void run() {
            FastScroller.this.setState(0);
        }
    };
    private final Animator.AnimatorListener mSwitchPrimaryListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animator) {
            FastScroller.this.mShowingPrimary = !FastScroller.this.mShowingPrimary;
        }
    };

    public FastScroller(AbsListView absListView, int i) {
        this.mList = absListView;
        this.mOldItemCount = absListView.getCount();
        this.mOldChildCount = absListView.getChildCount();
        Context context = absListView.getContext();
        this.mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        this.mScrollBarStyle = absListView.getScrollBarStyle();
        this.mScrollCompleted = true;
        this.mState = 1;
        this.mMatchDragPosition = context.getApplicationInfo().targetSdkVersion >= 11;
        this.mTrackImage = new ImageView(context);
        this.mTrackImage.setScaleType(ImageView.ScaleType.FIT_XY);
        this.mThumbImage = new ImageView(context);
        this.mThumbImage.setScaleType(ImageView.ScaleType.FIT_XY);
        this.mPreviewImage = new View(context);
        this.mPreviewImage.setAlpha(0.0f);
        this.mPrimaryText = createPreviewTextView(context);
        this.mSecondaryText = createPreviewTextView(context);
        this.mMinimumTouchTarget = absListView.getResources().getDimensionPixelSize(R.dimen.fast_scroller_minimum_touch_target);
        setStyle(i);
        ViewGroupOverlay overlay = absListView.getOverlay();
        this.mOverlay = overlay;
        overlay.add(this.mTrackImage);
        overlay.add(this.mThumbImage);
        overlay.add(this.mPreviewImage);
        overlay.add(this.mPrimaryText);
        overlay.add(this.mSecondaryText);
        getSectionsFromIndexer();
        updateLongList(this.mOldChildCount, this.mOldItemCount);
        setScrollbarPosition(absListView.getVerticalScrollbarPosition());
        postAutoHide();
    }

    private void updateAppearance() {
        int iMax;
        this.mTrackImage.setImageDrawable(this.mTrackDrawable);
        if (this.mTrackDrawable != null) {
            iMax = Math.max(0, this.mTrackDrawable.getIntrinsicWidth());
        } else {
            iMax = 0;
        }
        this.mThumbImage.setImageDrawable(this.mThumbDrawable);
        this.mThumbImage.setMinimumWidth(this.mThumbMinWidth);
        this.mThumbImage.setMinimumHeight(this.mThumbMinHeight);
        if (this.mThumbDrawable != null) {
            iMax = Math.max(iMax, this.mThumbDrawable.getIntrinsicWidth());
        }
        this.mWidth = Math.max(iMax, this.mThumbMinWidth);
        if (this.mTextAppearance != 0) {
            this.mPrimaryText.setTextAppearance(this.mTextAppearance);
            this.mSecondaryText.setTextAppearance(this.mTextAppearance);
        }
        if (this.mTextColor != null) {
            this.mPrimaryText.setTextColor(this.mTextColor);
            this.mSecondaryText.setTextColor(this.mTextColor);
        }
        if (this.mTextSize > 0.0f) {
            this.mPrimaryText.setTextSize(0, this.mTextSize);
            this.mSecondaryText.setTextSize(0, this.mTextSize);
        }
        int i = this.mPreviewPadding;
        this.mPrimaryText.setIncludeFontPadding(false);
        this.mPrimaryText.setPadding(i, i, i, i);
        this.mSecondaryText.setIncludeFontPadding(false);
        this.mSecondaryText.setPadding(i, i, i, i);
        refreshDrawablePressedState();
    }

    public void setStyle(int i) {
        TypedArray typedArrayObtainStyledAttributes = this.mList.getContext().obtainStyledAttributes(null, R.styleable.FastScroll, 16843767, i);
        int indexCount = typedArrayObtainStyledAttributes.getIndexCount();
        for (int i2 = 0; i2 < indexCount; i2++) {
            int index = typedArrayObtainStyledAttributes.getIndex(i2);
            switch (index) {
                case 0:
                    this.mTextAppearance = typedArrayObtainStyledAttributes.getResourceId(index, 0);
                    break;
                case 1:
                    this.mTextSize = typedArrayObtainStyledAttributes.getDimensionPixelSize(index, 0);
                    break;
                case 2:
                    this.mTextColor = typedArrayObtainStyledAttributes.getColorStateList(index);
                    break;
                case 3:
                    this.mPreviewPadding = typedArrayObtainStyledAttributes.getDimensionPixelSize(index, 0);
                    break;
                case 4:
                    this.mPreviewMinWidth = typedArrayObtainStyledAttributes.getDimensionPixelSize(index, 0);
                    break;
                case 5:
                    this.mPreviewMinHeight = typedArrayObtainStyledAttributes.getDimensionPixelSize(index, 0);
                    break;
                case 6:
                    this.mThumbPosition = typedArrayObtainStyledAttributes.getInt(index, 0);
                    break;
                case 7:
                    this.mPreviewResId[0] = typedArrayObtainStyledAttributes.getResourceId(index, 0);
                    break;
                case 8:
                    this.mPreviewResId[1] = typedArrayObtainStyledAttributes.getResourceId(index, 0);
                    break;
                case 9:
                    this.mOverlayPosition = typedArrayObtainStyledAttributes.getInt(index, 0);
                    break;
                case 10:
                    this.mThumbDrawable = typedArrayObtainStyledAttributes.getDrawable(index);
                    break;
                case 11:
                    this.mThumbMinHeight = typedArrayObtainStyledAttributes.getDimensionPixelSize(index, 0);
                    break;
                case 12:
                    this.mThumbMinWidth = typedArrayObtainStyledAttributes.getDimensionPixelSize(index, 0);
                    break;
                case 13:
                    this.mTrackDrawable = typedArrayObtainStyledAttributes.getDrawable(index);
                    break;
            }
        }
        typedArrayObtainStyledAttributes.recycle();
        updateAppearance();
    }

    public void remove() {
        this.mOverlay.remove(this.mTrackImage);
        this.mOverlay.remove(this.mThumbImage);
        this.mOverlay.remove(this.mPreviewImage);
        this.mOverlay.remove(this.mPrimaryText);
        this.mOverlay.remove(this.mSecondaryText);
    }

    public void setEnabled(boolean z) {
        if (this.mEnabled != z) {
            this.mEnabled = z;
            onStateDependencyChanged(true);
        }
    }

    public boolean isEnabled() {
        return this.mEnabled && (this.mLongList || this.mAlwaysShow);
    }

    public void setAlwaysShow(boolean z) {
        if (this.mAlwaysShow != z) {
            this.mAlwaysShow = z;
            onStateDependencyChanged(false);
        }
    }

    public boolean isAlwaysShowEnabled() {
        return this.mAlwaysShow;
    }

    private void onStateDependencyChanged(boolean z) {
        if (isEnabled()) {
            if (isAlwaysShowEnabled()) {
                setState(1);
            } else if (this.mState == 1) {
                postAutoHide();
            } else if (z) {
                setState(1);
                postAutoHide();
            }
        } else {
            stop();
        }
        this.mList.resolvePadding();
    }

    public void setScrollBarStyle(int i) {
        if (this.mScrollBarStyle != i) {
            this.mScrollBarStyle = i;
            updateLayout();
        }
    }

    public void stop() {
        setState(0);
    }

    public void setScrollbarPosition(int i) {
        boolean z = true;
        if (i == 0) {
            if (!this.mList.isLayoutRtl()) {
                i = 2;
            } else {
                i = 1;
            }
        }
        if (this.mScrollbarPosition != i) {
            this.mScrollbarPosition = i;
            if (i == 1) {
                z = false;
            }
            this.mLayoutFromRight = z;
            this.mPreviewImage.setBackgroundResource(this.mPreviewResId[this.mLayoutFromRight ? 1 : 0]);
            int iMax = Math.max(0, (this.mPreviewMinWidth - this.mPreviewImage.getPaddingLeft()) - this.mPreviewImage.getPaddingRight());
            this.mPrimaryText.setMinimumWidth(iMax);
            this.mSecondaryText.setMinimumWidth(iMax);
            int iMax2 = Math.max(0, (this.mPreviewMinHeight - this.mPreviewImage.getPaddingTop()) - this.mPreviewImage.getPaddingBottom());
            this.mPrimaryText.setMinimumHeight(iMax2);
            this.mSecondaryText.setMinimumHeight(iMax2);
            updateLayout();
        }
    }

    public int getWidth() {
        return this.mWidth;
    }

    public void onSizeChanged(int i, int i2, int i3, int i4) {
        updateLayout();
    }

    public void onItemCountChanged(int i, int i2) {
        if (this.mOldItemCount != i2 || this.mOldChildCount != i) {
            this.mOldItemCount = i2;
            this.mOldChildCount = i;
            if ((i2 - i > 0) && this.mState != 2) {
                setThumbPos(getPosFromItemCount(this.mList.getFirstVisiblePosition(), i, i2));
            }
            updateLongList(i, i2);
        }
    }

    private void updateLongList(int i, int i2) {
        boolean z = i > 0 && i2 / i >= 4;
        if (this.mLongList != z) {
            this.mLongList = z;
            onStateDependencyChanged(false);
        }
    }

    private TextView createPreviewTextView(Context context) {
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(-2, -2);
        TextView textView = new TextView(context);
        textView.setLayoutParams(layoutParams);
        textView.setSingleLine(true);
        textView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        textView.setGravity(17);
        textView.setAlpha(0.0f);
        textView.setLayoutDirection(this.mList.getLayoutDirection());
        return textView;
    }

    public void updateLayout() {
        if (this.mUpdatingLayout) {
            return;
        }
        this.mUpdatingLayout = true;
        updateContainerRect();
        layoutThumb();
        layoutTrack();
        updateOffsetAndRange();
        Rect rect = this.mTempBounds;
        measurePreview(this.mPrimaryText, rect);
        applyLayout(this.mPrimaryText, rect);
        measurePreview(this.mSecondaryText, rect);
        applyLayout(this.mSecondaryText, rect);
        if (this.mPreviewImage != null) {
            rect.left -= this.mPreviewImage.getPaddingLeft();
            rect.top -= this.mPreviewImage.getPaddingTop();
            rect.right += this.mPreviewImage.getPaddingRight();
            rect.bottom += this.mPreviewImage.getPaddingBottom();
            applyLayout(this.mPreviewImage, rect);
        }
        this.mUpdatingLayout = false;
    }

    private void applyLayout(View view, Rect rect) {
        view.layout(rect.left, rect.top, rect.right, rect.bottom);
        view.setPivotX(this.mLayoutFromRight ? rect.right - rect.left : 0.0f);
    }

    private void measurePreview(View view, Rect rect) {
        Rect rect2 = this.mTempMargins;
        rect2.left = this.mPreviewImage.getPaddingLeft();
        rect2.top = this.mPreviewImage.getPaddingTop();
        rect2.right = this.mPreviewImage.getPaddingRight();
        rect2.bottom = this.mPreviewImage.getPaddingBottom();
        if (this.mOverlayPosition == 0) {
            measureFloating(view, rect2, rect);
        } else {
            measureViewToSide(view, this.mThumbImage, rect2, rect);
        }
    }

    private void measureViewToSide(View view, View view2, Rect rect, Rect rect2) {
        int i;
        int i2;
        int i3;
        int right;
        int left;
        if (rect != null) {
            i = rect.left;
            i2 = rect.top;
            i3 = rect.right;
        } else {
            i3 = 0;
            i = 0;
            i2 = 0;
        }
        Rect rect3 = this.mContainerRect;
        int iWidth = rect3.width();
        if (view2 != null) {
            if (this.mLayoutFromRight) {
                iWidth = view2.getLeft();
            } else {
                iWidth -= view2.getRight();
            }
        }
        int iMax = Math.max(0, rect3.height());
        int iMax2 = Math.max(0, (iWidth - i) - i3);
        view.measure(View.MeasureSpec.makeMeasureSpec(iMax2, Integer.MIN_VALUE), View.MeasureSpec.makeSafeMeasureSpec(iMax, 0));
        int iMin = Math.min(iMax2, view.getMeasuredWidth());
        if (this.mLayoutFromRight) {
            left = (view2 == null ? rect3.right : view2.getLeft()) - i3;
            right = left - iMin;
        } else {
            right = (view2 == null ? rect3.left : view2.getRight()) + i;
            left = right + iMin;
        }
        rect2.set(right, i2, left, view.getMeasuredHeight() + i2);
    }

    private void measureFloating(View view, Rect rect, Rect rect2) {
        int i;
        int i2;
        int i3;
        if (rect != null) {
            i = rect.left;
            i2 = rect.top;
            i3 = rect.right;
        } else {
            i3 = 0;
            i = 0;
            i2 = 0;
        }
        Rect rect3 = this.mContainerRect;
        int iWidth = rect3.width();
        view.measure(View.MeasureSpec.makeMeasureSpec(Math.max(0, (iWidth - i) - i3), Integer.MIN_VALUE), View.MeasureSpec.makeSafeMeasureSpec(Math.max(0, rect3.height()), 0));
        int iHeight = rect3.height();
        int measuredWidth = view.getMeasuredWidth();
        int i4 = (iHeight / 10) + i2 + rect3.top;
        int measuredHeight = view.getMeasuredHeight() + i4;
        int i5 = ((iWidth - measuredWidth) / 2) + rect3.left;
        rect2.set(i5, i4, measuredWidth + i5, measuredHeight);
    }

    private void updateContainerRect() {
        AbsListView absListView = this.mList;
        absListView.resolvePadding();
        Rect rect = this.mContainerRect;
        rect.left = 0;
        rect.top = 0;
        rect.right = absListView.getWidth();
        rect.bottom = absListView.getHeight();
        int i = this.mScrollBarStyle;
        if (i == 16777216 || i == 0) {
            rect.left += absListView.getPaddingLeft();
            rect.top += absListView.getPaddingTop();
            rect.right -= absListView.getPaddingRight();
            rect.bottom -= absListView.getPaddingBottom();
            if (i == 16777216) {
                int width = getWidth();
                if (this.mScrollbarPosition == 2) {
                    rect.right += width;
                } else {
                    rect.left -= width;
                }
            }
        }
    }

    private void layoutThumb() {
        Rect rect = this.mTempBounds;
        measureViewToSide(this.mThumbImage, null, null, rect);
        applyLayout(this.mThumbImage, rect);
    }

    private void layoutTrack() {
        int i;
        int i2;
        ImageView imageView = this.mTrackImage;
        ImageView imageView2 = this.mThumbImage;
        Rect rect = this.mContainerRect;
        imageView.measure(View.MeasureSpec.makeMeasureSpec(Math.max(0, rect.width()), Integer.MIN_VALUE), View.MeasureSpec.makeSafeMeasureSpec(Math.max(0, rect.height()), 0));
        if (this.mThumbPosition == 1) {
            i2 = rect.top;
            i = rect.bottom;
        } else {
            int height = imageView2.getHeight() / 2;
            int i3 = rect.top + height;
            i = rect.bottom - height;
            i2 = i3;
        }
        int measuredWidth = imageView.getMeasuredWidth();
        int left = imageView2.getLeft() + ((imageView2.getWidth() - measuredWidth) / 2);
        imageView.layout(left, i2, measuredWidth + left, i);
    }

    private void updateOffsetAndRange() {
        float top;
        float bottom;
        ImageView imageView = this.mTrackImage;
        ImageView imageView2 = this.mThumbImage;
        if (this.mThumbPosition == 1) {
            float height = imageView2.getHeight() / 2.0f;
            top = imageView.getTop() + height;
            bottom = imageView.getBottom() - height;
        } else {
            top = imageView.getTop();
            bottom = imageView.getBottom();
        }
        this.mThumbOffset = top;
        this.mThumbRange = bottom - top;
    }

    private void setState(int i) {
        this.mList.removeCallbacks(this.mDeferHide);
        if (this.mAlwaysShow && i == 0) {
            i = 1;
        }
        if (i == this.mState) {
            return;
        }
        switch (i) {
            case 0:
                transitionToHidden();
                break;
            case 1:
                transitionToVisible();
                break;
            case 2:
                if (transitionPreviewLayout(this.mCurrentSection)) {
                    transitionToDragging();
                } else {
                    transitionToVisible();
                }
                break;
        }
        this.mState = i;
        refreshDrawablePressedState();
    }

    private void refreshDrawablePressedState() {
        boolean z = this.mState == 2;
        this.mThumbImage.setPressed(z);
        this.mTrackImage.setPressed(z);
    }

    private void transitionToHidden() {
        if (this.mDecorAnimation != null) {
            this.mDecorAnimation.cancel();
        }
        Animator duration = groupAnimatorOfFloat(View.ALPHA, 0.0f, this.mThumbImage, this.mTrackImage, this.mPreviewImage, this.mPrimaryText, this.mSecondaryText).setDuration(300L);
        Animator duration2 = groupAnimatorOfFloat(View.TRANSLATION_X, this.mLayoutFromRight ? this.mThumbImage.getWidth() : -this.mThumbImage.getWidth(), this.mThumbImage, this.mTrackImage).setDuration(300L);
        this.mDecorAnimation = new AnimatorSet();
        this.mDecorAnimation.playTogether(duration, duration2);
        this.mDecorAnimation.start();
        this.mShowingPreview = false;
    }

    private void transitionToVisible() {
        if (this.mDecorAnimation != null) {
            this.mDecorAnimation.cancel();
        }
        Animator duration = groupAnimatorOfFloat(View.ALPHA, 1.0f, this.mThumbImage, this.mTrackImage).setDuration(150L);
        Animator duration2 = groupAnimatorOfFloat(View.ALPHA, 0.0f, this.mPreviewImage, this.mPrimaryText, this.mSecondaryText).setDuration(300L);
        Animator duration3 = groupAnimatorOfFloat(View.TRANSLATION_X, 0.0f, this.mThumbImage, this.mTrackImage).setDuration(150L);
        this.mDecorAnimation = new AnimatorSet();
        this.mDecorAnimation.playTogether(duration, duration2, duration3);
        this.mDecorAnimation.start();
        this.mShowingPreview = false;
    }

    private void transitionToDragging() {
        if (this.mDecorAnimation != null) {
            this.mDecorAnimation.cancel();
        }
        Animator duration = groupAnimatorOfFloat(View.ALPHA, 1.0f, this.mThumbImage, this.mTrackImage, this.mPreviewImage).setDuration(150L);
        Animator duration2 = groupAnimatorOfFloat(View.TRANSLATION_X, 0.0f, this.mThumbImage, this.mTrackImage).setDuration(150L);
        this.mDecorAnimation = new AnimatorSet();
        this.mDecorAnimation.playTogether(duration, duration2);
        this.mDecorAnimation.start();
        this.mShowingPreview = true;
    }

    private void postAutoHide() {
        this.mList.removeCallbacks(this.mDeferHide);
        this.mList.postDelayed(this.mDeferHide, FADE_TIMEOUT);
    }

    public void onScroll(int i, int i2, int i3) {
        if (!isEnabled()) {
            setState(0);
            return;
        }
        if ((i3 - i2 > 0) && this.mState != 2) {
            setThumbPos(getPosFromItemCount(i, i2, i3));
        }
        this.mScrollCompleted = true;
        if (this.mFirstVisibleItem != i) {
            this.mFirstVisibleItem = i;
            if (this.mState != 2) {
                setState(1);
                postAutoHide();
            }
        }
    }

    private void getSectionsFromIndexer() {
        this.mSectionIndexer = null;
        ListAdapter adapter = this.mList.getAdapter();
        if (adapter instanceof HeaderViewListAdapter) {
            HeaderViewListAdapter headerViewListAdapter = (HeaderViewListAdapter) adapter;
            this.mHeaderCount = headerViewListAdapter.getHeadersCount();
            adapter = headerViewListAdapter.getWrappedAdapter();
        }
        if (adapter instanceof ExpandableListConnector) {
            ExpandableListAdapter adapter2 = ((ExpandableListConnector) adapter).getAdapter();
            if (adapter2 instanceof SectionIndexer) {
                this.mSectionIndexer = (SectionIndexer) adapter2;
                this.mListAdapter = adapter;
                this.mSections = this.mSectionIndexer.getSections();
                return;
            }
            return;
        }
        if (adapter instanceof SectionIndexer) {
            this.mListAdapter = adapter;
            this.mSectionIndexer = (SectionIndexer) adapter;
            this.mSections = this.mSectionIndexer.getSections();
        } else {
            this.mListAdapter = adapter;
            this.mSections = null;
        }
    }

    public void onSectionsChanged() {
        this.mListAdapter = null;
    }

    private void scrollTo(float f) {
        int length;
        int i;
        int positionForSection;
        int i2;
        int positionForSection2;
        int i3;
        float f2;
        float f3;
        this.mScrollCompleted = false;
        int count = this.mList.getCount();
        Object[] objArr = this.mSections;
        if (objArr != null) {
            length = objArr.length;
        } else {
            length = 0;
        }
        if (objArr == null || length <= 1) {
            int iConstrain = MathUtils.constrain((int) (f * count), 0, count - 1);
            if (this.mList instanceof ExpandableListView) {
                ExpandableListView expandableListView = (ExpandableListView) this.mList;
                expandableListView.setSelectionFromTop(expandableListView.getFlatListPosition(ExpandableListView.getPackedPositionForGroup(iConstrain + this.mHeaderCount)), 0);
            } else if (this.mList instanceof ListView) {
                ((ListView) this.mList).setSelectionFromTop(iConstrain + this.mHeaderCount, 0);
            } else {
                this.mList.setSelection(iConstrain + this.mHeaderCount);
            }
            i = -1;
        } else {
            float f4 = length;
            int i4 = length - 1;
            int iConstrain2 = MathUtils.constrain((int) (f * f4), 0, i4);
            int positionForSection3 = this.mSectionIndexer.getPositionForSection(iConstrain2);
            int i5 = iConstrain2 + 1;
            if (iConstrain2 < i4) {
                positionForSection = this.mSectionIndexer.getPositionForSection(i5);
            } else {
                positionForSection = count;
            }
            if (positionForSection == positionForSection3) {
                i2 = iConstrain2;
                positionForSection2 = positionForSection3;
                while (true) {
                    if (i2 <= 0) {
                        i = iConstrain2;
                        i2 = i;
                        break;
                    }
                    i2--;
                    positionForSection2 = this.mSectionIndexer.getPositionForSection(i2);
                    if (positionForSection2 != positionForSection3) {
                        break;
                    }
                    if (i2 == 0) {
                        i = 0;
                        i2 = iConstrain2;
                        break;
                    }
                }
                i3 = i5 + 1;
                while (i3 < length && this.mSectionIndexer.getPositionForSection(i3) == positionForSection) {
                    i3++;
                    i5++;
                }
                f2 = i2 / f4;
                f3 = i5 / f4;
                float f5 = count != 0 ? Float.MAX_VALUE : 0.125f / count;
                if (i2 == iConstrain2 || f - f2 >= f5) {
                    positionForSection2 += (int) (((positionForSection - positionForSection2) * (f - f2)) / (f3 - f2));
                }
                int iConstrain3 = MathUtils.constrain(positionForSection2, 0, count - 1);
                if (!(this.mList instanceof ExpandableListView)) {
                    ExpandableListView expandableListView2 = (ExpandableListView) this.mList;
                    expandableListView2.setSelectionFromTop(expandableListView2.getFlatListPosition(ExpandableListView.getPackedPositionForGroup(iConstrain3 + this.mHeaderCount)), 0);
                } else if (this.mList instanceof ListView) {
                    ((ListView) this.mList).setSelectionFromTop(iConstrain3 + this.mHeaderCount, 0);
                } else {
                    this.mList.setSelection(iConstrain3 + this.mHeaderCount);
                }
            } else {
                i2 = iConstrain2;
                positionForSection2 = positionForSection3;
            }
            i = i2;
            i3 = i5 + 1;
            while (i3 < length) {
                i3++;
                i5++;
            }
            f2 = i2 / f4;
            f3 = i5 / f4;
            if (count != 0) {
            }
            if (i2 == iConstrain2) {
                positionForSection2 += (int) (((positionForSection - positionForSection2) * (f - f2)) / (f3 - f2));
                int iConstrain32 = MathUtils.constrain(positionForSection2, 0, count - 1);
                if (!(this.mList instanceof ExpandableListView)) {
                }
            }
        }
        if (this.mCurrentSection != i) {
            this.mCurrentSection = i;
            boolean zTransitionPreviewLayout = transitionPreviewLayout(i);
            if (!this.mShowingPreview && zTransitionPreviewLayout) {
                transitionToDragging();
            } else if (this.mShowingPreview && !zTransitionPreviewLayout) {
                transitionToVisible();
            }
        }
    }

    private boolean transitionPreviewLayout(int i) {
        String string;
        TextView textView;
        TextView textView2;
        Object obj;
        Object[] objArr = this.mSections;
        if (objArr != null && i >= 0 && i < objArr.length && (obj = objArr[i]) != null) {
            string = obj.toString();
        } else {
            string = null;
        }
        Rect rect = this.mTempBounds;
        View view = this.mPreviewImage;
        if (this.mShowingPrimary) {
            textView = this.mPrimaryText;
            textView2 = this.mSecondaryText;
        } else {
            textView = this.mSecondaryText;
            textView2 = this.mPrimaryText;
        }
        textView2.setText(string);
        measurePreview(textView2, rect);
        applyLayout(textView2, rect);
        if (this.mPreviewAnimation != null) {
            this.mPreviewAnimation.cancel();
        }
        Animator duration = animateAlpha(textView2, 1.0f).setDuration(50L);
        Animator duration2 = animateAlpha(textView, 0.0f).setDuration(50L);
        duration2.addListener(this.mSwitchPrimaryListener);
        rect.left -= view.getPaddingLeft();
        rect.top -= view.getPaddingTop();
        rect.right += view.getPaddingRight();
        rect.bottom += view.getPaddingBottom();
        Animator animatorAnimateBounds = animateBounds(view, rect);
        animatorAnimateBounds.setDuration(100L);
        this.mPreviewAnimation = new AnimatorSet();
        AnimatorSet.Builder builderWith = this.mPreviewAnimation.play(duration2).with(duration);
        builderWith.with(animatorAnimateBounds);
        int width = (view.getWidth() - view.getPaddingLeft()) - view.getPaddingRight();
        int width2 = textView2.getWidth();
        if (width2 > width) {
            textView2.setScaleX(width / width2);
            builderWith.with(animateScaleX(textView2, 1.0f).setDuration(100L));
        } else {
            textView2.setScaleX(1.0f);
        }
        int width3 = textView.getWidth();
        if (width3 > width2) {
            builderWith.with(animateScaleX(textView, width2 / width3).setDuration(100L));
        }
        this.mPreviewAnimation.start();
        return !TextUtils.isEmpty(string);
    }

    private void setThumbPos(float f) {
        float f2 = (f * this.mThumbRange) + this.mThumbOffset;
        this.mThumbImage.setTranslationY(f2 - (this.mThumbImage.getHeight() / 2.0f));
        View view = this.mPreviewImage;
        float height = view.getHeight() / 2.0f;
        switch (this.mOverlayPosition) {
            case 1:
                break;
            case 2:
                f2 -= height;
                break;
            default:
                f2 = 0.0f;
                break;
        }
        Rect rect = this.mContainerRect;
        float fConstrain = MathUtils.constrain(f2, rect.top + height, rect.bottom - height) - height;
        view.setTranslationY(fConstrain);
        this.mPrimaryText.setTranslationY(fConstrain);
        this.mSecondaryText.setTranslationY(fConstrain);
    }

    private float getPosFromMotionEvent(float f) {
        if (this.mThumbRange <= 0.0f) {
            return 0.0f;
        }
        return MathUtils.constrain((f - this.mThumbOffset) / this.mThumbRange, 0.0f, 1.0f);
    }

    private float getPosFromItemCount(int i, int i2, int i3) {
        float paddingTop;
        int i4;
        int height;
        int height2;
        int positionForSection;
        SectionIndexer sectionIndexer = this.mSectionIndexer;
        if (sectionIndexer == null || this.mListAdapter == null) {
            getSectionsFromIndexer();
        }
        float f = 0.0f;
        if (i2 == 0 || i3 == 0) {
            return 0.0f;
        }
        if (!((sectionIndexer == null || this.mSections == null || this.mSections.length <= 0) ? false : true) || !this.mMatchDragPosition) {
            if (i2 == i3) {
                return 0.0f;
            }
            return i / (i3 - i2);
        }
        int i5 = i - this.mHeaderCount;
        if (i5 < 0) {
            return 0.0f;
        }
        int i6 = i3 - this.mHeaderCount;
        View childAt = this.mList.getChildAt(0);
        if (childAt != null && childAt.getHeight() != 0) {
            paddingTop = (this.mList.getPaddingTop() - childAt.getTop()) / childAt.getHeight();
        } else {
            paddingTop = 0.0f;
        }
        int sectionForPosition = sectionIndexer.getSectionForPosition(i5);
        int positionForSection2 = sectionIndexer.getPositionForSection(sectionForPosition);
        int length = this.mSections.length;
        if (sectionForPosition < length - 1) {
            int i7 = sectionForPosition + 1;
            if (i7 < length) {
                positionForSection = sectionIndexer.getPositionForSection(i7);
            } else {
                positionForSection = i6 - 1;
            }
            i4 = positionForSection - positionForSection2;
        } else {
            i4 = i6 - positionForSection2;
        }
        if (i4 != 0) {
            f = ((i5 + paddingTop) - positionForSection2) / i4;
        }
        float f2 = (sectionForPosition + f) / length;
        if (i5 > 0 && i5 + i2 == i6) {
            View childAt2 = this.mList.getChildAt(i2 - 1);
            int paddingBottom = this.mList.getPaddingBottom();
            if (this.mList.getClipToPadding()) {
                height = childAt2.getHeight();
                height2 = (this.mList.getHeight() - paddingBottom) - childAt2.getTop();
            } else {
                height = childAt2.getHeight() + paddingBottom;
                height2 = this.mList.getHeight() - childAt2.getTop();
            }
            if (height2 > 0 && height > 0) {
                return f2 + ((1.0f - f2) * (height2 / height));
            }
            return f2;
        }
        return f2;
    }

    private void cancelFling() {
        MotionEvent motionEventObtain = MotionEvent.obtain(0L, 0L, 3, 0.0f, 0.0f, 0);
        this.mList.onTouchEvent(motionEventObtain);
        motionEventObtain.recycle();
    }

    private void cancelPendingDrag() {
        this.mPendingDrag = -1L;
    }

    private void startPendingDrag() {
        this.mPendingDrag = SystemClock.uptimeMillis() + TAP_TIMEOUT;
    }

    private void beginDrag() {
        this.mPendingDrag = -1L;
        setState(2);
        if (this.mListAdapter == null && this.mList != null) {
            getSectionsFromIndexer();
        }
        if (this.mList != null) {
            this.mList.requestDisallowInterceptTouchEvent(true);
            this.mList.reportScrollStateChange(1);
        }
        cancelFling();
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (!isEnabled()) {
            return false;
        }
        switch (motionEvent.getActionMasked()) {
            case 0:
                if (isPointInside(motionEvent.getX(), motionEvent.getY())) {
                    if (!this.mList.isInScrollingContainer()) {
                        return true;
                    }
                    this.mInitialTouchY = motionEvent.getY();
                    startPendingDrag();
                }
                return false;
            case 1:
            case 3:
                cancelPendingDrag();
                return false;
            case 2:
                if (!isPointInside(motionEvent.getX(), motionEvent.getY())) {
                    cancelPendingDrag();
                } else if (this.mPendingDrag >= 0 && this.mPendingDrag <= SystemClock.uptimeMillis()) {
                    beginDrag();
                    scrollTo(getPosFromMotionEvent(this.mInitialTouchY));
                    return onTouchEvent(motionEvent);
                }
                return false;
            default:
                return false;
        }
    }

    public boolean onInterceptHoverEvent(MotionEvent motionEvent) {
        if (!isEnabled()) {
            return false;
        }
        int actionMasked = motionEvent.getActionMasked();
        if ((actionMasked == 9 || actionMasked == 7) && this.mState == 0 && isPointInside(motionEvent.getX(), motionEvent.getY())) {
            setState(1);
            postAutoHide();
        }
        return false;
    }

    public PointerIcon onResolvePointerIcon(MotionEvent motionEvent, int i) {
        if (this.mState == 2 || isPointInside(motionEvent.getX(), motionEvent.getY())) {
            return PointerIcon.getSystemIcon(this.mList.getContext(), 1000);
        }
        return null;
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!isEnabled()) {
            return false;
        }
        switch (motionEvent.getActionMasked()) {
            case 0:
                if (isPointInside(motionEvent.getX(), motionEvent.getY()) && !this.mList.isInScrollingContainer()) {
                    beginDrag();
                    return true;
                }
                return false;
            case 1:
                if (this.mPendingDrag >= 0) {
                    beginDrag();
                    float posFromMotionEvent = getPosFromMotionEvent(motionEvent.getY());
                    setThumbPos(posFromMotionEvent);
                    scrollTo(posFromMotionEvent);
                }
                if (this.mState == 2) {
                    if (this.mList != null) {
                        this.mList.requestDisallowInterceptTouchEvent(false);
                        this.mList.reportScrollStateChange(0);
                    }
                    setState(1);
                    postAutoHide();
                    return true;
                }
                return false;
            case 2:
                if (this.mPendingDrag >= 0 && Math.abs(motionEvent.getY() - this.mInitialTouchY) > this.mScaledTouchSlop) {
                    beginDrag();
                }
                if (this.mState == 2) {
                    float posFromMotionEvent2 = getPosFromMotionEvent(motionEvent.getY());
                    setThumbPos(posFromMotionEvent2);
                    if (this.mScrollCompleted) {
                        scrollTo(posFromMotionEvent2);
                    }
                    return true;
                }
                return false;
            case 3:
                cancelPendingDrag();
                return false;
            default:
                return false;
        }
    }

    private boolean isPointInside(float f, float f2) {
        return isPointInsideX(f) && (this.mTrackDrawable != null || isPointInsideY(f2));
    }

    private boolean isPointInsideX(float f) {
        float translationX = this.mThumbImage.getTranslationX();
        float right = this.mMinimumTouchTarget - ((this.mThumbImage.getRight() + translationX) - (this.mThumbImage.getLeft() + translationX));
        if (right <= 0.0f) {
            right = 0.0f;
        }
        return this.mLayoutFromRight ? f >= ((float) this.mThumbImage.getLeft()) - right : f <= ((float) this.mThumbImage.getRight()) + right;
    }

    private boolean isPointInsideY(float f) {
        float translationY = this.mThumbImage.getTranslationY();
        float top = this.mThumbImage.getTop() + translationY;
        float bottom = this.mThumbImage.getBottom() + translationY;
        float f2 = this.mMinimumTouchTarget - (bottom - top);
        float f3 = f2 > 0.0f ? f2 / 2.0f : 0.0f;
        return f >= top - f3 && f <= bottom + f3;
    }

    private static Animator groupAnimatorOfFloat(Property<View, Float> property, float f, View... viewArr) {
        AnimatorSet animatorSet = new AnimatorSet();
        AnimatorSet.Builder builderPlay = null;
        for (int length = viewArr.length - 1; length >= 0; length--) {
            ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(viewArr[length], property, f);
            if (builderPlay == null) {
                builderPlay = animatorSet.play(objectAnimatorOfFloat);
            } else {
                builderPlay.with(objectAnimatorOfFloat);
            }
        }
        return animatorSet;
    }

    private static Animator animateScaleX(View view, float f) {
        return ObjectAnimator.ofFloat(view, View.SCALE_X, f);
    }

    private static Animator animateAlpha(View view, float f) {
        return ObjectAnimator.ofFloat(view, View.ALPHA, f);
    }

    private static Animator animateBounds(View view, Rect rect) {
        return ObjectAnimator.ofPropertyValuesHolder(view, PropertyValuesHolder.ofInt(LEFT, rect.left), PropertyValuesHolder.ofInt(TOP, rect.top), PropertyValuesHolder.ofInt(RIGHT, rect.right), PropertyValuesHolder.ofInt(BOTTOM, rect.bottom));
    }
}
