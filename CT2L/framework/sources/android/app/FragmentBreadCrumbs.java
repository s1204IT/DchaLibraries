package android.app;

import android.animation.LayoutTransition;
import android.app.FragmentManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.internal.R;

@Deprecated
public class FragmentBreadCrumbs extends ViewGroup implements FragmentManager.OnBackStackChangedListener {
    private static final int DEFAULT_GRAVITY = 8388627;
    Activity mActivity;
    LinearLayout mContainer;
    private int mGravity;
    LayoutInflater mInflater;
    private int mLayoutResId;
    int mMaxVisible;
    private OnBreadCrumbClickListener mOnBreadCrumbClickListener;
    private View.OnClickListener mOnClickListener;
    private View.OnClickListener mParentClickListener;
    BackStackRecord mParentEntry;
    private int mTextColor;
    BackStackRecord mTopEntry;

    public interface OnBreadCrumbClickListener {
        boolean onBreadCrumbClick(FragmentManager.BackStackEntry backStackEntry, int i);
    }

    public FragmentBreadCrumbs(Context context) {
        this(context, null);
    }

    public FragmentBreadCrumbs(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.fragmentBreadCrumbsStyle);
    }

    public FragmentBreadCrumbs(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public FragmentBreadCrumbs(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mMaxVisible = -1;
        this.mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getTag() instanceof FragmentManager.BackStackEntry) {
                    FragmentManager.BackStackEntry bse = (FragmentManager.BackStackEntry) v.getTag();
                    if (bse == FragmentBreadCrumbs.this.mParentEntry) {
                        if (FragmentBreadCrumbs.this.mParentClickListener != null) {
                            FragmentBreadCrumbs.this.mParentClickListener.onClick(v);
                            return;
                        }
                        return;
                    }
                    if (FragmentBreadCrumbs.this.mOnBreadCrumbClickListener != null) {
                        if (FragmentBreadCrumbs.this.mOnBreadCrumbClickListener.onBreadCrumbClick(bse == FragmentBreadCrumbs.this.mTopEntry ? null : bse, 0)) {
                            return;
                        }
                    }
                    if (bse == FragmentBreadCrumbs.this.mTopEntry) {
                        FragmentBreadCrumbs.this.mActivity.getFragmentManager().popBackStack();
                    } else {
                        FragmentBreadCrumbs.this.mActivity.getFragmentManager().popBackStack(bse.getId(), 0);
                    }
                }
            }
        };
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FragmentBreadCrumbs, defStyleAttr, defStyleRes);
        this.mGravity = a.getInt(0, DEFAULT_GRAVITY);
        this.mLayoutResId = a.getResourceId(1, R.layout.fragment_bread_crumb_item);
        this.mTextColor = a.getColor(2, 0);
        a.recycle();
    }

    public void setActivity(Activity a) {
        this.mActivity = a;
        this.mInflater = (LayoutInflater) a.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mContainer = (LinearLayout) this.mInflater.inflate(R.layout.fragment_bread_crumbs, (ViewGroup) this, false);
        addView(this.mContainer);
        a.getFragmentManager().addOnBackStackChangedListener(this);
        updateCrumbs();
        setLayoutTransition(new LayoutTransition());
    }

    public void setMaxVisible(int visibleCrumbs) {
        if (visibleCrumbs < 1) {
            throw new IllegalArgumentException("visibleCrumbs must be greater than zero");
        }
        this.mMaxVisible = visibleCrumbs;
    }

    public void setParentTitle(CharSequence title, CharSequence shortTitle, View.OnClickListener listener) {
        this.mParentEntry = createBackStackEntry(title, shortTitle);
        this.mParentClickListener = listener;
        updateCrumbs();
    }

    public void setOnBreadCrumbClickListener(OnBreadCrumbClickListener listener) {
        this.mOnBreadCrumbClickListener = listener;
    }

    private BackStackRecord createBackStackEntry(CharSequence title, CharSequence shortTitle) {
        if (title == null) {
            return null;
        }
        BackStackRecord entry = new BackStackRecord((FragmentManagerImpl) this.mActivity.getFragmentManager());
        entry.setBreadCrumbTitle(title);
        entry.setBreadCrumbShortTitle(shortTitle);
        return entry;
    }

    public void setTitle(CharSequence title, CharSequence shortTitle) {
        this.mTopEntry = createBackStackEntry(title, shortTitle);
        updateCrumbs();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childLeft;
        int childRight;
        int childCount = getChildCount();
        if (childCount != 0) {
            View child = getChildAt(0);
            int childTop = this.mPaddingTop;
            int childBottom = (this.mPaddingTop + child.getMeasuredHeight()) - this.mPaddingBottom;
            int layoutDirection = getLayoutDirection();
            int horizontalGravity = this.mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
            switch (Gravity.getAbsoluteGravity(horizontalGravity, layoutDirection)) {
                case 1:
                    childLeft = this.mPaddingLeft + (((this.mRight - this.mLeft) - child.getMeasuredWidth()) / 2);
                    childRight = childLeft + child.getMeasuredWidth();
                    break;
                case 5:
                    childRight = (this.mRight - this.mLeft) - this.mPaddingRight;
                    childLeft = childRight - child.getMeasuredWidth();
                    break;
                default:
                    childLeft = this.mPaddingLeft;
                    childRight = childLeft + child.getMeasuredWidth();
                    break;
            }
            if (childLeft < this.mPaddingLeft) {
                childLeft = this.mPaddingLeft;
            }
            if (childRight > (this.mRight - this.mLeft) - this.mPaddingRight) {
                childRight = (this.mRight - this.mLeft) - this.mPaddingRight;
            }
            child.layout(childLeft, childTop, childRight, childBottom);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();
        int maxHeight = 0;
        int maxWidth = 0;
        int measuredChildState = 0;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                measureChild(child, widthMeasureSpec, heightMeasureSpec);
                maxWidth = Math.max(maxWidth, child.getMeasuredWidth());
                maxHeight = Math.max(maxHeight, child.getMeasuredHeight());
                measuredChildState = combineMeasuredStates(measuredChildState, child.getMeasuredState());
            }
        }
        setMeasuredDimension(resolveSizeAndState(Math.max(maxWidth + this.mPaddingLeft + this.mPaddingRight, getSuggestedMinimumWidth()), widthMeasureSpec, measuredChildState), resolveSizeAndState(Math.max(maxHeight + this.mPaddingTop + this.mPaddingBottom, getSuggestedMinimumHeight()), heightMeasureSpec, measuredChildState << 16));
    }

    @Override
    public void onBackStackChanged() {
        updateCrumbs();
    }

    private int getPreEntryCount() {
        return (this.mTopEntry != null ? 1 : 0) + (this.mParentEntry == null ? 0 : 1);
    }

    private FragmentManager.BackStackEntry getPreEntry(int index) {
        if (this.mParentEntry != null) {
            return index == 0 ? this.mParentEntry : this.mTopEntry;
        }
        return this.mTopEntry;
    }

    void updateCrumbs() {
        FragmentManager fm = this.mActivity.getFragmentManager();
        int numEntries = fm.getBackStackEntryCount();
        int numPreEntries = getPreEntryCount();
        int numViews = this.mContainer.getChildCount();
        int i = 0;
        while (i < numEntries + numPreEntries) {
            FragmentManager.BackStackEntry bse = i < numPreEntries ? getPreEntry(i) : fm.getBackStackEntryAt(i - numPreEntries);
            if (i < numViews) {
                View v = this.mContainer.getChildAt(i);
                Object tag = v.getTag();
                if (tag != bse) {
                    for (int j = i; j < numViews; j++) {
                        this.mContainer.removeViewAt(i);
                    }
                    numViews = i;
                }
            }
            if (i >= numViews) {
                View item = this.mInflater.inflate(this.mLayoutResId, (ViewGroup) this, false);
                TextView text = (TextView) item.findViewById(16908310);
                text.setText(bse.getBreadCrumbTitle());
                text.setTag(bse);
                text.setTextColor(this.mTextColor);
                if (i == 0) {
                    item.findViewById(R.id.left_icon).setVisibility(8);
                }
                this.mContainer.addView(item);
                text.setOnClickListener(this.mOnClickListener);
            }
            i++;
        }
        int viewI = numEntries + numPreEntries;
        int numViews2 = this.mContainer.getChildCount();
        while (numViews2 > viewI) {
            this.mContainer.removeViewAt(numViews2 - 1);
            numViews2--;
        }
        int i2 = 0;
        while (i2 < numViews2) {
            View child = this.mContainer.getChildAt(i2);
            child.findViewById(16908310).setEnabled(i2 < numViews2 + (-1));
            if (this.mMaxVisible > 0) {
                child.setVisibility(i2 < numViews2 - this.mMaxVisible ? 8 : 0);
                View leftIcon = child.findViewById(R.id.left_icon);
                leftIcon.setVisibility((i2 <= numViews2 - this.mMaxVisible || i2 == 0) ? 8 : 0);
            }
            i2++;
        }
    }
}
