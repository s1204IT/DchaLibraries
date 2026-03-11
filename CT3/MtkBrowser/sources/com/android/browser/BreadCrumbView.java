package com.android.browser;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
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

    public BreadCrumbView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mMaxVisible = -1;
        init(context);
    }

    public BreadCrumbView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mMaxVisible = -1;
        init(context);
    }

    public BreadCrumbView(Context context) {
        super(context);
        this.mMaxVisible = -1;
        init(context);
    }

    private void init(Context ctx) {
        this.mContext = ctx;
        setFocusable(true);
        this.mUseBackButton = false;
        this.mCrumbs = new ArrayList();
        TypedArray a = this.mContext.obtainStyledAttributes(com.android.internal.R.styleable.Theme);
        this.mSeparatorDrawable = a.getDrawable(155);
        a.recycle();
        float density = this.mContext.getResources().getDisplayMetrics().density;
        this.mDividerPadding = 12.0f * density;
        this.mCrumbPadding = (int) (8.0f * density);
        addBackButton();
    }

    public void setUseBackButton(boolean useflag) {
        this.mUseBackButton = useflag;
        updateVisible();
    }

    public void setController(Controller ctl) {
        this.mController = ctl;
    }

    public void setMaxVisible(int max) {
        this.mMaxVisible = max;
        updateVisible();
    }

    public Object getTopData() {
        Crumb c = getTopCrumb();
        if (c != null) {
            return c.data;
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
        if (this.mController == null) {
            return;
        }
        if (this.mCrumbs.size() > 0) {
            this.mController.onTop(this, this.mCrumbs.size(), getTopCrumb().data);
        } else {
            this.mController.onTop(this, 0, null);
        }
    }

    public View pushView(String name, Object data) {
        return pushView(name, true, data);
    }

    public View pushView(String name, boolean canGoBack, Object data) {
        Crumb crumb = new Crumb(name, canGoBack, data);
        pushCrumb(crumb);
        return crumb.crumbView;
    }

    public void popView() {
        pop(true);
    }

    private void addBackButton() {
        this.mBackButton = new ImageButton(this.mContext);
        this.mBackButton.setImageResource(R.drawable.ic_back_hierarchy_holo_dark);
        TypedValue outValue = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        int resid = outValue.resourceId;
        this.mBackButton.setBackgroundResource(resid);
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
        View sep = makeDividerView();
        sep.setLayoutParams(makeDividerLayoutParams());
        addView(sep);
    }

    private ImageView makeDividerView() {
        ImageView result = new ImageView(this.mContext);
        result.setImageDrawable(this.mSeparatorDrawable);
        result.setScaleType(ImageView.ScaleType.FIT_XY);
        return result;
    }

    private LinearLayout.LayoutParams makeDividerLayoutParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -1);
        params.topMargin = (int) this.mDividerPadding;
        params.bottomMargin = (int) this.mDividerPadding;
        return params;
    }

    private void pop(boolean notify) {
        int n = this.mCrumbs.size();
        if (n <= 0) {
            return;
        }
        removeLastView();
        if (!this.mUseBackButton || n > 1) {
            removeLastView();
        }
        this.mCrumbs.remove(n - 1);
        if (this.mUseBackButton) {
            Crumb top = getTopCrumb();
            if (top != null && top.canGoBack) {
                this.mBackButton.setVisibility(0);
            } else {
                this.mBackButton.setVisibility(8);
            }
        }
        updateVisible();
        if (!notify) {
            return;
        }
        notifyController();
    }

    private void updateVisible() {
        int childIndex = 1;
        if (this.mMaxVisible >= 0) {
            int invisibleCrumbs = size() - this.mMaxVisible;
            if (invisibleCrumbs > 0) {
                for (int crumbIndex = 0; crumbIndex < invisibleCrumbs; crumbIndex++) {
                    getChildAt(childIndex).setVisibility(8);
                    int childIndex2 = childIndex + 1;
                    if (getChildAt(childIndex2) != null) {
                        getChildAt(childIndex2).setVisibility(8);
                    }
                    childIndex = childIndex2 + 1;
                }
            }
            int childCount = getChildCount();
            while (childIndex < childCount) {
                getChildAt(childIndex).setVisibility(0);
                childIndex++;
            }
        } else {
            int count = getChildCount();
            for (int i = 1; i < count; i++) {
                getChildAt(i).setVisibility(0);
            }
        }
        if (this.mUseBackButton) {
            this.mBackButton.setVisibility(getTopCrumb() != null ? getTopCrumb().canGoBack : false ? 0 : 8);
        } else {
            this.mBackButton.setVisibility(8);
        }
    }

    private void removeLastView() {
        int ix = getChildCount();
        if (ix <= 0) {
            return;
        }
        removeViewAt(ix - 1);
    }

    Crumb getTopCrumb() {
        if (this.mCrumbs.size() <= 0) {
            return null;
        }
        Crumb crumb = this.mCrumbs.get(this.mCrumbs.size() - 1);
        return crumb;
    }

    @Override
    public void onClick(View v) {
        if (this.mBackButton == v) {
            popView();
            notifyController();
        } else {
            while (v != getTopCrumb().crumbView) {
                pop(false);
            }
            notifyController();
        }
    }

    @Override
    public int getBaseline() {
        int ix = getChildCount();
        if (ix > 0) {
            return getChildAt(ix - 1).getBaseline();
        }
        return super.getBaseline();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int height = this.mSeparatorDrawable.getIntrinsicHeight();
        if (getMeasuredHeight() >= height) {
            return;
        }
        int mode = View.MeasureSpec.getMode(heightMeasureSpec);
        switch (mode) {
            case Integer.MIN_VALUE:
                if (View.MeasureSpec.getSize(heightMeasureSpec) < height) {
                    return;
                }
                break;
            case 1073741824:
                return;
        }
        setMeasuredDimension(getMeasuredWidth(), height);
    }

    class Crumb {
        public boolean canGoBack;
        public View crumbView;
        public Object data;

        public Crumb(String title, boolean backEnabled, Object tag) {
            init(makeCrumbView(title), backEnabled, tag);
        }

        private void init(View view, boolean back, Object tag) {
            this.canGoBack = back;
            this.crumbView = view;
            this.data = tag;
        }

        private TextView makeCrumbView(String name) {
            TextView tv = new TextView(BreadCrumbView.this.mContext);
            tv.setTextAppearance(BreadCrumbView.this.mContext, android.R.style.TextAppearance.Medium);
            tv.setPadding(BreadCrumbView.this.mCrumbPadding, 0, BreadCrumbView.this.mCrumbPadding, 0);
            tv.setGravity(16);
            tv.setText(name);
            tv.setLayoutParams(new LinearLayout.LayoutParams(-2, -1));
            tv.setSingleLine();
            tv.setEllipsize(TextUtils.TruncateAt.END);
            return tv;
        }
    }
}
