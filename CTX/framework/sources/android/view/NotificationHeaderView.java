package android.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RemoteViews;
import com.android.internal.R;
import com.android.internal.widget.CachingIconView;
import java.util.ArrayList;

@RemoteViews.RemoteView
public class NotificationHeaderView extends ViewGroup {
    public static final int NO_COLOR = 1;
    private boolean mAcceptAllTouches;
    private View mAppName;
    private View mAppOps;
    private View.OnClickListener mAppOpsListener;
    private Drawable mBackground;
    private View mCameraIcon;
    private final int mChildMinWidth;
    private final int mContentEndMargin;
    private boolean mEntireHeaderClickable;
    private ImageView mExpandButton;
    private View.OnClickListener mExpandClickListener;
    private boolean mExpandOnlyOnButton;
    private boolean mExpanded;
    private final int mGravity;
    private View mHeaderText;
    private CachingIconView mIcon;
    private int mIconColor;
    private View mMicIcon;
    private int mOriginalNotificationColor;
    private View mOverlayIcon;
    private View mProfileBadge;
    ViewOutlineProvider mProvider;
    private View mSecondaryHeaderText;
    private boolean mShowExpandButtonAtEnd;
    private boolean mShowWorkBadgeAtEnd;
    private int mTotalWidth;
    private HeaderTouchListener mTouchListener;

    public NotificationHeaderView(Context context) {
        this(context, null);
    }

    public NotificationHeaderView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public NotificationHeaderView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public NotificationHeaderView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mTouchListener = new HeaderTouchListener();
        this.mProvider = new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                if (NotificationHeaderView.this.mBackground != null) {
                    outline.setRect(0, 0, NotificationHeaderView.this.getWidth(), NotificationHeaderView.this.getHeight());
                    outline.setAlpha(1.0f);
                }
            }
        };
        Resources resources = getResources();
        this.mChildMinWidth = resources.getDimensionPixelSize(R.dimen.notification_header_shrink_min_width);
        this.mContentEndMargin = resources.getDimensionPixelSize(R.dimen.notification_content_margin_end);
        this.mEntireHeaderClickable = resources.getBoolean(R.bool.config_notificationHeaderClickableForExpand);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, new int[]{16842927}, i, i2);
        this.mGravity = typedArrayObtainStyledAttributes.getInt(0, 0);
        typedArrayObtainStyledAttributes.recycle();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mAppName = findViewById(R.id.app_name_text);
        this.mHeaderText = findViewById(R.id.header_text);
        this.mSecondaryHeaderText = findViewById(R.id.header_text_secondary);
        this.mExpandButton = (ImageView) findViewById(R.id.expand_button);
        this.mIcon = (CachingIconView) findViewById(16908294);
        this.mProfileBadge = findViewById(R.id.profile_badge);
        this.mCameraIcon = findViewById(R.id.camera);
        this.mMicIcon = findViewById(R.id.mic);
        this.mOverlayIcon = findViewById(R.id.overlay);
        this.mAppOps = findViewById(R.id.app_ops);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int size = View.MeasureSpec.getSize(i);
        int size2 = View.MeasureSpec.getSize(i2);
        int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(size, Integer.MIN_VALUE);
        int iMakeMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(size2, Integer.MIN_VALUE);
        int paddingStart = getPaddingStart() + getPaddingEnd();
        for (int i3 = 0; i3 < getChildCount(); i3++) {
            View childAt = getChildAt(i3);
            if (childAt.getVisibility() != 8) {
                ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) childAt.getLayoutParams();
                childAt.measure(getChildMeasureSpec(iMakeMeasureSpec, marginLayoutParams.leftMargin + marginLayoutParams.rightMargin, marginLayoutParams.width), getChildMeasureSpec(iMakeMeasureSpec2, marginLayoutParams.topMargin + marginLayoutParams.bottomMargin, marginLayoutParams.height));
                paddingStart += marginLayoutParams.leftMargin + marginLayoutParams.rightMargin + childAt.getMeasuredWidth();
            }
        }
        if (paddingStart > size) {
            shrinkViewForOverflow(iMakeMeasureSpec2, shrinkViewForOverflow(iMakeMeasureSpec2, shrinkViewForOverflow(iMakeMeasureSpec2, paddingStart - size, this.mAppName, this.mChildMinWidth), this.mHeaderText, 0), this.mSecondaryHeaderText, 0);
        }
        this.mTotalWidth = Math.min(paddingStart, size);
        setMeasuredDimension(size, size2);
    }

    private int shrinkViewForOverflow(int i, int i2, View view, int i3) {
        int measuredWidth = view.getMeasuredWidth();
        if (i2 > 0 && view.getVisibility() != 8 && measuredWidth > i3) {
            int iMax = Math.max(i3, measuredWidth - i2);
            view.measure(View.MeasureSpec.makeMeasureSpec(iMax, Integer.MIN_VALUE), i);
            return i2 - (measuredWidth - iMax);
        }
        return i2;
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int width;
        int paddingStart = getPaddingStart();
        int measuredWidth = getMeasuredWidth();
        if ((this.mGravity & 1) != 0) {
            paddingStart += (getMeasuredWidth() / 2) - (this.mTotalWidth / 2);
        }
        int childCount = getChildCount();
        int measuredHeight = (getMeasuredHeight() - getPaddingTop()) - getPaddingBottom();
        for (int i5 = 0; i5 < childCount; i5++) {
            View childAt = getChildAt(i5);
            if (childAt.getVisibility() != 8) {
                int measuredHeight2 = childAt.getMeasuredHeight();
                ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) childAt.getLayoutParams();
                int marginStart = paddingStart + marginLayoutParams.getMarginStart();
                int measuredWidth2 = childAt.getMeasuredWidth() + marginStart;
                int paddingTop = (int) (getPaddingTop() + ((measuredHeight - measuredHeight2) / 2.0f));
                int i6 = measuredHeight2 + paddingTop;
                if (childAt == this.mExpandButton && this.mShowExpandButtonAtEnd) {
                    int i7 = measuredWidth - this.mContentEndMargin;
                    measuredWidth = i7 - childAt.getMeasuredWidth();
                    width = i7;
                    marginStart = measuredWidth;
                } else {
                    width = measuredWidth2;
                }
                if (childAt == this.mProfileBadge) {
                    int paddingEnd = getPaddingEnd();
                    if (this.mShowWorkBadgeAtEnd) {
                        paddingEnd = this.mContentEndMargin;
                    }
                    width = measuredWidth - paddingEnd;
                    measuredWidth = width - childAt.getMeasuredWidth();
                    marginStart = measuredWidth;
                }
                if (childAt == this.mAppOps) {
                    width = measuredWidth - this.mContentEndMargin;
                    measuredWidth = width - childAt.getMeasuredWidth();
                    marginStart = measuredWidth;
                }
                if (getLayoutDirection() == 1) {
                    int width2 = getWidth() - width;
                    width = getWidth() - marginStart;
                    marginStart = width2;
                }
                childAt.layout(marginStart, paddingTop, width, i6);
                paddingStart = measuredWidth2 + marginLayoutParams.getMarginEnd();
            }
        }
        updateTouchListener();
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new ViewGroup.MarginLayoutParams(getContext(), attributeSet);
    }

    public void setHeaderBackgroundDrawable(Drawable drawable) {
        if (drawable != null) {
            setWillNotDraw(false);
            this.mBackground = drawable;
            this.mBackground.setCallback(this);
            setOutlineProvider(this.mProvider);
        } else {
            setWillNotDraw(true);
            this.mBackground = null;
            setOutlineProvider(null);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (this.mBackground != null) {
            this.mBackground.setBounds(0, 0, getWidth(), getHeight());
            this.mBackground.draw(canvas);
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable drawable) {
        return super.verifyDrawable(drawable) || drawable == this.mBackground;
    }

    @Override
    protected void drawableStateChanged() {
        if (this.mBackground != null && this.mBackground.isStateful()) {
            this.mBackground.setState(getDrawableState());
        }
    }

    private void updateTouchListener() {
        if (this.mExpandClickListener == null && this.mAppOpsListener == null) {
            setOnTouchListener(null);
        } else {
            setOnTouchListener(this.mTouchListener);
            this.mTouchListener.bindTouchRects();
        }
    }

    public void setAppOpsOnClickListener(View.OnClickListener onClickListener) {
        this.mAppOpsListener = onClickListener;
        this.mAppOps.setOnClickListener(this.mAppOpsListener);
        this.mCameraIcon.setOnClickListener(this.mAppOpsListener);
        this.mMicIcon.setOnClickListener(this.mAppOpsListener);
        this.mOverlayIcon.setOnClickListener(this.mAppOpsListener);
        updateTouchListener();
    }

    @Override
    public void setOnClickListener(View.OnClickListener onClickListener) {
        this.mExpandClickListener = onClickListener;
        this.mExpandButton.setOnClickListener(this.mExpandClickListener);
        updateTouchListener();
    }

    @RemotableViewMethod
    public void setOriginalIconColor(int i) {
        this.mIconColor = i;
    }

    public int getOriginalIconColor() {
        return this.mIconColor;
    }

    @RemotableViewMethod
    public void setOriginalNotificationColor(int i) {
        this.mOriginalNotificationColor = i;
    }

    public int getOriginalNotificationColor() {
        return this.mOriginalNotificationColor;
    }

    @RemotableViewMethod
    public void setExpanded(boolean z) {
        this.mExpanded = z;
        updateExpandButton();
    }

    public void showAppOpsIcons(ArraySet<Integer> arraySet) {
        if (this.mOverlayIcon == null || this.mCameraIcon == null || this.mMicIcon == null || arraySet == null) {
            return;
        }
        this.mOverlayIcon.setVisibility(arraySet.contains(24) ? 0 : 8);
        this.mCameraIcon.setVisibility(arraySet.contains(26) ? 0 : 8);
        this.mMicIcon.setVisibility(arraySet.contains(27) ? 0 : 8);
    }

    private void updateExpandButton() {
        int i;
        int i2;
        if (this.mExpanded) {
            i = R.drawable.ic_collapse_notification;
            i2 = R.string.expand_button_content_description_expanded;
        } else {
            i = R.drawable.ic_expand_notification;
            i2 = R.string.expand_button_content_description_collapsed;
        }
        this.mExpandButton.setImageDrawable(getContext().getDrawable(i));
        this.mExpandButton.setColorFilter(this.mOriginalNotificationColor);
        this.mExpandButton.setContentDescription(this.mContext.getText(i2));
    }

    public void setShowWorkBadgeAtEnd(boolean z) {
        if (z != this.mShowWorkBadgeAtEnd) {
            setClipToPadding(!z);
            this.mShowWorkBadgeAtEnd = z;
        }
    }

    public void setShowExpandButtonAtEnd(boolean z) {
        if (z != this.mShowExpandButtonAtEnd) {
            setClipToPadding(!z);
            this.mShowExpandButtonAtEnd = z;
        }
    }

    public View getWorkProfileIcon() {
        return this.mProfileBadge;
    }

    public CachingIconView getIcon() {
        return this.mIcon;
    }

    public class HeaderTouchListener implements View.OnTouchListener {
        private Rect mAppOpsRect;
        private float mDownX;
        private float mDownY;
        private Rect mExpandButtonRect;
        private final ArrayList<Rect> mTouchRects = new ArrayList<>();
        private int mTouchSlop;
        private boolean mTrackGesture;

        public HeaderTouchListener() {
        }

        public void bindTouchRects() {
            this.mTouchRects.clear();
            addRectAroundView(NotificationHeaderView.this.mIcon);
            this.mExpandButtonRect = addRectAroundView(NotificationHeaderView.this.mExpandButton);
            this.mAppOpsRect = addRectAroundView(NotificationHeaderView.this.mAppOps);
            addWidthRect();
            this.mTouchSlop = ViewConfiguration.get(NotificationHeaderView.this.getContext()).getScaledTouchSlop();
        }

        private void addWidthRect() {
            Rect rect = new Rect();
            rect.top = 0;
            rect.bottom = (int) (32.0f * NotificationHeaderView.this.getResources().getDisplayMetrics().density);
            rect.left = 0;
            rect.right = NotificationHeaderView.this.getWidth();
            this.mTouchRects.add(rect);
        }

        private Rect addRectAroundView(View view) {
            Rect rectAroundView = getRectAroundView(view);
            this.mTouchRects.add(rectAroundView);
            return rectAroundView;
        }

        private Rect getRectAroundView(View view) {
            float f = 48.0f * NotificationHeaderView.this.getResources().getDisplayMetrics().density;
            float fMax = Math.max(f, view.getWidth());
            float fMax2 = Math.max(f, view.getHeight());
            Rect rect = new Rect();
            if (view.getVisibility() == 8) {
                view = NotificationHeaderView.this.getFirstChildNotGone();
                rect.left = (int) (view.getLeft() - (fMax / 2.0f));
            } else {
                rect.left = (int) (((view.getLeft() + view.getRight()) / 2.0f) - (fMax / 2.0f));
            }
            rect.top = (int) (((view.getTop() + view.getBottom()) / 2.0f) - (fMax2 / 2.0f));
            rect.bottom = (int) (rect.top + fMax2);
            rect.right = (int) (rect.left + fMax);
            return rect;
        }

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            float x = motionEvent.getX();
            float y = motionEvent.getY();
            switch (motionEvent.getActionMasked() & 255) {
                case 0:
                    this.mTrackGesture = false;
                    if (isInside(x, y)) {
                        this.mDownX = x;
                        this.mDownY = y;
                        this.mTrackGesture = true;
                        return true;
                    }
                    break;
                case 1:
                    if (this.mTrackGesture) {
                        if (!NotificationHeaderView.this.mAppOps.isVisibleToUser() || (!this.mAppOpsRect.contains((int) x, (int) y) && !this.mAppOpsRect.contains((int) this.mDownX, (int) this.mDownY))) {
                            NotificationHeaderView.this.mExpandButton.performClick();
                        } else {
                            NotificationHeaderView.this.mAppOps.performClick();
                            return true;
                        }
                    }
                    break;
                case 2:
                    if (this.mTrackGesture && (Math.abs(this.mDownX - x) > this.mTouchSlop || Math.abs(this.mDownY - y) > this.mTouchSlop)) {
                        this.mTrackGesture = false;
                    }
                    break;
            }
            return this.mTrackGesture;
        }

        private boolean isInside(float f, float f2) {
            if (NotificationHeaderView.this.mAcceptAllTouches) {
                return true;
            }
            if (NotificationHeaderView.this.mExpandOnlyOnButton) {
                return this.mExpandButtonRect.contains((int) f, (int) f2);
            }
            for (int i = 0; i < this.mTouchRects.size(); i++) {
                if (this.mTouchRects.get(i).contains((int) f, (int) f2)) {
                    return true;
                }
            }
            return false;
        }
    }

    private View getFirstChildNotGone() {
        for (int i = 0; i < getChildCount(); i++) {
            View childAt = getChildAt(i);
            if (childAt.getVisibility() != 8) {
                return childAt;
            }
        }
        return this;
    }

    public ImageView getExpandButton() {
        return this.mExpandButton;
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public boolean isInTouchRect(float f, float f2) {
        if (this.mExpandClickListener != null) {
            return this.mTouchListener.isInside(f, f2);
        }
        return false;
    }

    @RemotableViewMethod
    public void setAcceptAllTouches(boolean z) {
        this.mAcceptAllTouches = this.mEntireHeaderClickable || z;
    }

    @RemotableViewMethod
    public void setExpandOnlyOnButton(boolean z) {
        this.mExpandOnlyOnButton = z;
    }
}
