package com.android.internal.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ActionMenuPresenter;
import android.widget.ActionMenuView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.internal.R;
import com.android.internal.view.menu.MenuBuilder;

public class ActionBarContextView extends AbsActionBarView {
    private static final String TAG = "ActionBarContextView";
    private View mClose;
    private int mCloseItemLayout;
    private View mCustomView;
    private Drawable mSplitBackground;
    private CharSequence mSubtitle;
    private int mSubtitleStyleRes;
    private TextView mSubtitleView;
    private CharSequence mTitle;
    private LinearLayout mTitleLayout;
    private boolean mTitleOptional;
    private int mTitleStyleRes;
    private TextView mTitleView;

    public ActionBarContextView(Context context) {
        this(context, null);
    }

    public ActionBarContextView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 16843668);
    }

    public ActionBarContextView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public ActionBarContextView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.ActionMode, i, i2);
        setBackground(typedArrayObtainStyledAttributes.getDrawable(0));
        this.mTitleStyleRes = typedArrayObtainStyledAttributes.getResourceId(2, 0);
        this.mSubtitleStyleRes = typedArrayObtainStyledAttributes.getResourceId(3, 0);
        this.mContentHeight = typedArrayObtainStyledAttributes.getLayoutDimension(1, 0);
        this.mSplitBackground = typedArrayObtainStyledAttributes.getDrawable(4);
        this.mCloseItemLayout = typedArrayObtainStyledAttributes.getResourceId(5, R.layout.action_mode_close_item);
        typedArrayObtainStyledAttributes.recycle();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mActionMenuPresenter != null) {
            this.mActionMenuPresenter.hideOverflowMenu();
            this.mActionMenuPresenter.hideSubMenus();
        }
    }

    @Override
    public void setSplitToolbar(boolean z) {
        if (this.mSplitActionBar != z) {
            if (this.mActionMenuPresenter != null) {
                ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(-2, -1);
                if (!z) {
                    this.mMenuView = (ActionMenuView) this.mActionMenuPresenter.getMenuView(this);
                    this.mMenuView.setBackground(null);
                    ViewGroup viewGroup = (ViewGroup) this.mMenuView.getParent();
                    if (viewGroup != null) {
                        viewGroup.removeView(this.mMenuView);
                    }
                    addView(this.mMenuView, layoutParams);
                } else {
                    this.mActionMenuPresenter.setWidthLimit(getContext().getResources().getDisplayMetrics().widthPixels, true);
                    this.mActionMenuPresenter.setItemLimit(Integer.MAX_VALUE);
                    layoutParams.width = -1;
                    layoutParams.height = this.mContentHeight;
                    this.mMenuView = (ActionMenuView) this.mActionMenuPresenter.getMenuView(this);
                    this.mMenuView.setBackground(this.mSplitBackground);
                    ViewGroup viewGroup2 = (ViewGroup) this.mMenuView.getParent();
                    if (viewGroup2 != null) {
                        viewGroup2.removeView(this.mMenuView);
                    }
                    this.mSplitView.addView(this.mMenuView, layoutParams);
                }
            }
            super.setSplitToolbar(z);
        }
    }

    @Override
    public void setContentHeight(int i) {
        this.mContentHeight = i;
    }

    public void setCustomView(View view) {
        if (this.mCustomView != null) {
            removeView(this.mCustomView);
        }
        this.mCustomView = view;
        if (view != null && this.mTitleLayout != null) {
            removeView(this.mTitleLayout);
            this.mTitleLayout = null;
        }
        if (view != null) {
            addView(view);
        }
        requestLayout();
    }

    public void setTitle(CharSequence charSequence) {
        this.mTitle = charSequence;
        initTitle();
    }

    public void setSubtitle(CharSequence charSequence) {
        this.mSubtitle = charSequence;
        initTitle();
    }

    public CharSequence getTitle() {
        return this.mTitle;
    }

    public CharSequence getSubtitle() {
        return this.mSubtitle;
    }

    private void initTitle() {
        if (this.mTitleLayout == null) {
            LayoutInflater.from(getContext()).inflate(R.layout.action_bar_title_item, this);
            this.mTitleLayout = (LinearLayout) getChildAt(getChildCount() - 1);
            this.mTitleView = (TextView) this.mTitleLayout.findViewById(R.id.action_bar_title);
            this.mSubtitleView = (TextView) this.mTitleLayout.findViewById(R.id.action_bar_subtitle);
            if (this.mTitleStyleRes != 0) {
                this.mTitleView.setTextAppearance(this.mTitleStyleRes);
            }
            if (this.mSubtitleStyleRes != 0) {
                this.mSubtitleView.setTextAppearance(this.mSubtitleStyleRes);
            }
        }
        this.mTitleView.setText(this.mTitle);
        this.mSubtitleView.setText(this.mSubtitle);
        boolean z = !TextUtils.isEmpty(this.mTitle);
        boolean z2 = !TextUtils.isEmpty(this.mSubtitle);
        this.mSubtitleView.setVisibility(z2 ? 0 : 8);
        this.mTitleLayout.setVisibility((z || z2) ? 0 : 8);
        if (this.mTitleLayout.getParent() == null) {
            addView(this.mTitleLayout);
        }
    }

    public void initForMode(final ActionMode actionMode) {
        if (this.mClose == null) {
            this.mClose = LayoutInflater.from(this.mContext).inflate(this.mCloseItemLayout, (ViewGroup) this, false);
            addView(this.mClose);
        } else if (this.mClose.getParent() == null) {
            addView(this.mClose);
        }
        this.mClose.findViewById(R.id.action_mode_close_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                actionMode.finish();
            }
        });
        MenuBuilder menuBuilder = (MenuBuilder) actionMode.getMenu();
        if (this.mActionMenuPresenter != null) {
            this.mActionMenuPresenter.dismissPopupMenus();
        }
        this.mActionMenuPresenter = new ActionMenuPresenter(this.mContext);
        this.mActionMenuPresenter.setReserveOverflow(true);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(-2, -1);
        if (!this.mSplitActionBar) {
            menuBuilder.addMenuPresenter(this.mActionMenuPresenter, this.mPopupContext);
            this.mMenuView = (ActionMenuView) this.mActionMenuPresenter.getMenuView(this);
            this.mMenuView.setBackground(null);
            addView(this.mMenuView, layoutParams);
            return;
        }
        this.mActionMenuPresenter.setWidthLimit(getContext().getResources().getDisplayMetrics().widthPixels, true);
        this.mActionMenuPresenter.setItemLimit(Integer.MAX_VALUE);
        layoutParams.width = -1;
        layoutParams.height = this.mContentHeight;
        menuBuilder.addMenuPresenter(this.mActionMenuPresenter, this.mPopupContext);
        this.mMenuView = (ActionMenuView) this.mActionMenuPresenter.getMenuView(this);
        this.mMenuView.setBackgroundDrawable(this.mSplitBackground);
        this.mSplitView.addView(this.mMenuView, layoutParams);
    }

    public void closeMode() {
        if (this.mClose == null) {
            killMode();
        }
    }

    public void killMode() {
        removeAllViews();
        if (this.mSplitView != null) {
            this.mSplitView.removeView(this.mMenuView);
        }
        this.mCustomView = null;
        this.mMenuView = null;
    }

    @Override
    public boolean showOverflowMenu() {
        if (this.mActionMenuPresenter != null) {
            return this.mActionMenuPresenter.showOverflowMenu();
        }
        return false;
    }

    @Override
    public boolean hideOverflowMenu() {
        if (this.mActionMenuPresenter != null) {
            return this.mActionMenuPresenter.hideOverflowMenu();
        }
        return false;
    }

    @Override
    public boolean isOverflowMenuShowing() {
        if (this.mActionMenuPresenter != null) {
            return this.mActionMenuPresenter.isOverflowMenuShowing();
        }
        return false;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new ViewGroup.MarginLayoutParams(-1, -2);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new ViewGroup.MarginLayoutParams(getContext(), attributeSet);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int i3 = 1073741824;
        if (View.MeasureSpec.getMode(i) != 1073741824) {
            throw new IllegalStateException(getClass().getSimpleName() + " can only be used with android:layout_width=\"match_parent\" (or fill_parent)");
        }
        if (View.MeasureSpec.getMode(i2) == 0) {
            throw new IllegalStateException(getClass().getSimpleName() + " can only be used with android:layout_height=\"wrap_content\"");
        }
        int size = View.MeasureSpec.getSize(i);
        int size2 = this.mContentHeight > 0 ? this.mContentHeight : View.MeasureSpec.getSize(i2);
        int paddingTop = getPaddingTop() + getPaddingBottom();
        int paddingLeft = (size - getPaddingLeft()) - getPaddingRight();
        int iMin = size2 - paddingTop;
        int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(iMin, Integer.MIN_VALUE);
        if (this.mClose != null) {
            int iMeasureChildView = measureChildView(this.mClose, paddingLeft, iMakeMeasureSpec, 0);
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) this.mClose.getLayoutParams();
            paddingLeft = iMeasureChildView - (marginLayoutParams.leftMargin + marginLayoutParams.rightMargin);
        }
        if (this.mMenuView != null && this.mMenuView.getParent() == this) {
            paddingLeft = measureChildView(this.mMenuView, paddingLeft, iMakeMeasureSpec, 0);
        }
        if (this.mTitleLayout != null && this.mCustomView == null) {
            if (this.mTitleOptional) {
                this.mTitleLayout.measure(View.MeasureSpec.makeSafeMeasureSpec(size, 0), iMakeMeasureSpec);
                int measuredWidth = this.mTitleLayout.getMeasuredWidth();
                boolean z = measuredWidth <= paddingLeft;
                if (z) {
                    paddingLeft -= measuredWidth;
                }
                this.mTitleLayout.setVisibility(z ? 0 : 8);
            } else {
                paddingLeft = measureChildView(this.mTitleLayout, paddingLeft, iMakeMeasureSpec, 0);
            }
        }
        if (this.mCustomView != null) {
            ViewGroup.LayoutParams layoutParams = this.mCustomView.getLayoutParams();
            int i4 = layoutParams.width != -2 ? 1073741824 : Integer.MIN_VALUE;
            if (layoutParams.width >= 0) {
                paddingLeft = Math.min(layoutParams.width, paddingLeft);
            }
            if (layoutParams.height == -2) {
                i3 = Integer.MIN_VALUE;
            }
            if (layoutParams.height >= 0) {
                iMin = Math.min(layoutParams.height, iMin);
            }
            this.mCustomView.measure(View.MeasureSpec.makeMeasureSpec(paddingLeft, i4), View.MeasureSpec.makeMeasureSpec(iMin, i3));
        }
        if (this.mContentHeight <= 0) {
            int childCount = getChildCount();
            int i5 = 0;
            for (int i6 = 0; i6 < childCount; i6++) {
                int measuredHeight = getChildAt(i6).getMeasuredHeight() + paddingTop;
                if (measuredHeight > i5) {
                    i5 = measuredHeight;
                }
            }
            setMeasuredDimension(size, i5);
            return;
        }
        setMeasuredDimension(size, size2);
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int iPositionChild;
        int paddingRight;
        boolean zIsLayoutRtl = isLayoutRtl();
        int paddingRight2 = zIsLayoutRtl ? (i3 - i) - getPaddingRight() : getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingTop2 = ((i4 - i2) - getPaddingTop()) - getPaddingBottom();
        if (this.mClose != null && this.mClose.getVisibility() != 8) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) this.mClose.getLayoutParams();
            int i5 = zIsLayoutRtl ? marginLayoutParams.rightMargin : marginLayoutParams.leftMargin;
            int i6 = zIsLayoutRtl ? marginLayoutParams.leftMargin : marginLayoutParams.rightMargin;
            int next = next(paddingRight2, i5, zIsLayoutRtl);
            iPositionChild = next(next + positionChild(this.mClose, next, paddingTop, paddingTop2, zIsLayoutRtl), i6, zIsLayoutRtl);
        } else {
            iPositionChild = paddingRight2;
        }
        if (this.mTitleLayout != null && this.mCustomView == null && this.mTitleLayout.getVisibility() != 8) {
            iPositionChild += positionChild(this.mTitleLayout, iPositionChild, paddingTop, paddingTop2, zIsLayoutRtl);
        }
        int i7 = iPositionChild;
        if (this.mCustomView != null) {
            positionChild(this.mCustomView, i7, paddingTop, paddingTop2, zIsLayoutRtl);
        }
        if (!zIsLayoutRtl) {
            paddingRight = (i3 - i) - getPaddingRight();
        } else {
            paddingRight = getPaddingLeft();
        }
        if (this.mMenuView != null) {
            positionChild(this.mMenuView, paddingRight, paddingTop, paddingTop2, !zIsLayoutRtl);
        }
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override
    public void onInitializeAccessibilityEventInternal(AccessibilityEvent accessibilityEvent) {
        if (accessibilityEvent.getEventType() == 32) {
            accessibilityEvent.setSource(this);
            accessibilityEvent.setClassName(getClass().getName());
            accessibilityEvent.setPackageName(getContext().getPackageName());
            accessibilityEvent.setContentDescription(this.mTitle);
            return;
        }
        super.onInitializeAccessibilityEventInternal(accessibilityEvent);
    }

    public void setTitleOptional(boolean z) {
        if (z != this.mTitleOptional) {
            requestLayout();
        }
        this.mTitleOptional = z;
    }

    public boolean isTitleOptional() {
        return this.mTitleOptional;
    }
}
