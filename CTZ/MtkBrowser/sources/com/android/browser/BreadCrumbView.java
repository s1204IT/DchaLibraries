package com.android.browser;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class BreadCrumbView extends LinearLayout implements View.OnClickListener {
    private ImageButton mBackButton;
    private Context mContext;
    private Controller mController;
    private int mCrumbPadding;
    private List<Crumb> mCrumbs;
    private float mDividerPadding;
    private int mMaxVisible;
    private Drawable mSeparatorDrawable;
    private boolean mUseBackButton;

    public interface Controller {
        void onTop(BreadCrumbView breadCrumbView, int i, Object obj);
    }

    public BreadCrumbView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mMaxVisible = -1;
        init(context);
    }

    public BreadCrumbView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mMaxVisible = -1;
        init(context);
    }

    public BreadCrumbView(Context context) {
        super(context);
        this.mMaxVisible = -1;
        init(context);
    }

    private void init(Context context) {
        this.mContext = context;
        setFocusable(true);
        this.mUseBackButton = false;
        this.mCrumbs = new ArrayList();
        TypedArray typedArrayObtainStyledAttributes = this.mContext.obtainStyledAttributes(com.android.internal.R.styleable.Theme);
        this.mSeparatorDrawable = typedArrayObtainStyledAttributes.getDrawable(155);
        typedArrayObtainStyledAttributes.recycle();
        float f = this.mContext.getResources().getDisplayMetrics().density;
        this.mDividerPadding = 12.0f * f;
        this.mCrumbPadding = (int) (8.0f * f);
        addBackButton();
    }

    public void setUseBackButton(boolean z) {
        this.mUseBackButton = z;
        updateVisible();
    }

    public void setController(Controller controller) {
        this.mController = controller;
    }

    public void setMaxVisible(int i) {
        this.mMaxVisible = i;
        updateVisible();
    }

    public Object getTopData() {
        Crumb topCrumb = getTopCrumb();
        if (topCrumb != null) {
            return topCrumb.data;
        }
        return null;
    }

    public int size() {
        return this.mCrumbs.size();
    }

    public void clear() {
        while (this.mCrumbs.size() > 1) {
            pop(false);
        }
        pop(true);
    }

    public void notifyController() {
        if (this.mController != null) {
            if (this.mCrumbs.size() > 0) {
                this.mController.onTop(this, this.mCrumbs.size(), getTopCrumb().data);
            } else {
                this.mController.onTop(this, 0, null);
            }
        }
    }

    public View pushView(String str, Object obj) {
        return pushView(str, true, obj);
    }

    public View pushView(String str, boolean z, Object obj) {
        Crumb crumb = new Crumb(str, z, obj);
        pushCrumb(crumb);
        return crumb.crumbView;
    }

    public void popView() {
        pop(true);
    }

    private void addBackButton() {
        this.mBackButton = new ImageButton(this.mContext);
        this.mBackButton.setImageResource(R.drawable.ic_back_hierarchy_holo_dark);
        TypedValue typedValue = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true);
        this.mBackButton.setBackgroundResource(typedValue.resourceId);
        this.mBackButton.setLayoutParams(new LinearLayout.LayoutParams(-2, -1));
        this.mBackButton.setOnClickListener(this);
        this.mBackButton.setVisibility(8);
        this.mBackButton.setContentDescription(this.mContext.getText(R.string.accessibility_button_bookmarks_folder_up));
        addView(this.mBackButton, 0);
    }

    private void pushCrumb(Crumb crumb) {
        if (this.mCrumbs.size() > 0) {
            addSeparator();
        }
        this.mCrumbs.add(crumb);
        addView(crumb.crumbView);
        updateVisible();
        crumb.crumbView.setOnClickListener(this);
    }

    private void addSeparator() {
        ImageView imageViewMakeDividerView = makeDividerView();
        imageViewMakeDividerView.setLayoutParams(makeDividerLayoutParams());
        addView(imageViewMakeDividerView);
    }

    private ImageView makeDividerView() {
        ImageView imageView = new ImageView(this.mContext);
        imageView.setImageDrawable(this.mSeparatorDrawable);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        return imageView;
    }

    private LinearLayout.LayoutParams makeDividerLayoutParams() {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -1);
        ((ViewGroup.MarginLayoutParams) layoutParams).topMargin = (int) this.mDividerPadding;
        ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin = (int) this.mDividerPadding;
        return layoutParams;
    }

    private void pop(boolean z) {
        int size = this.mCrumbs.size();
        if (size > 0) {
            removeLastView();
            if (!this.mUseBackButton || size > 1) {
                removeLastView();
            }
            this.mCrumbs.remove(size - 1);
            if (this.mUseBackButton) {
                Crumb topCrumb = getTopCrumb();
                if (topCrumb != null && topCrumb.canGoBack) {
                    this.mBackButton.setVisibility(0);
                } else {
                    this.mBackButton.setVisibility(8);
                }
            }
            updateVisible();
            if (z) {
                notifyController();
            }
        }
    }

    private void updateVisible() {
        int i = 1;
        if (this.mMaxVisible >= 0) {
            int size = size() - this.mMaxVisible;
            if (size > 0) {
                int i2 = 1;
                for (int i3 = 0; i3 < size; i3++) {
                    getChildAt(i2).setVisibility(8);
                    int i4 = i2 + 1;
                    if (getChildAt(i4) != null) {
                        getChildAt(i4).setVisibility(8);
                    }
                    i2 = i4 + 1;
                }
                i = i2;
            }
            int childCount = getChildCount();
            while (i < childCount) {
                getChildAt(i).setVisibility(0);
                i++;
            }
        } else {
            int childCount2 = getChildCount();
            while (i < childCount2) {
                getChildAt(i).setVisibility(0);
                i++;
            }
        }
        if (this.mUseBackButton) {
            this.mBackButton.setVisibility(getTopCrumb() != null ? getTopCrumb().canGoBack : false ? 0 : 8);
        } else {
            this.mBackButton.setVisibility(8);
        }
    }

    private void removeLastView() {
        int childCount = getChildCount();
        if (childCount > 0) {
            removeViewAt(childCount - 1);
        }
    }

    Crumb getTopCrumb() {
        if (this.mCrumbs.size() > 0) {
            return this.mCrumbs.get(this.mCrumbs.size() - 1);
        }
        return null;
    }

    @Override
    public void onClick(View view) {
        if (this.mBackButton == view) {
            popView();
            notifyController();
        } else {
            while (view != getTopCrumb().crumbView) {
                pop(false);
            }
            notifyController();
        }
    }

    @Override
    public int getBaseline() {
        int childCount = getChildCount();
        if (childCount > 0) {
            return getChildAt(childCount - 1).getBaseline();
        }
        return super.getBaseline();
    }

    @Override
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        int intrinsicHeight = this.mSeparatorDrawable.getIntrinsicHeight();
        if (getMeasuredHeight() < intrinsicHeight) {
            int mode = View.MeasureSpec.getMode(i2);
            if (mode != Integer.MIN_VALUE) {
                if (mode == 1073741824) {
                    return;
                }
            } else if (View.MeasureSpec.getSize(i2) < intrinsicHeight) {
                return;
            }
            setMeasuredDimension(getMeasuredWidth(), intrinsicHeight);
        }
    }

    class Crumb {
        public boolean canGoBack;
        public View crumbView;
        public Object data;

        public Crumb(String str, boolean z, Object obj) {
            init(makeCrumbView(str), z, obj);
        }

        private void init(View view, boolean z, Object obj) {
            this.canGoBack = z;
            this.crumbView = view;
            this.data = obj;
        }

        private TextView makeCrumbView(String str) {
            TextView textView = new TextView(BreadCrumbView.this.mContext);
            textView.setTextAppearance(BreadCrumbView.this.mContext, android.R.style.TextAppearance.Medium);
            textView.setPadding(BreadCrumbView.this.mCrumbPadding, 0, BreadCrumbView.this.mCrumbPadding, 0);
            textView.setGravity(16);
            textView.setText(str);
            textView.setLayoutParams(new LinearLayout.LayoutParams(-2, -1));
            textView.setSingleLine();
            textView.setEllipsize(TextUtils.TruncateAt.END);
            return textView;
        }
    }
}
