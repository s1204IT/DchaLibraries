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

    @Deprecated
    public interface OnBreadCrumbClickListener {
        boolean onBreadCrumbClick(FragmentManager.BackStackEntry backStackEntry, int i);
    }

    public FragmentBreadCrumbs(Context context) {
        this(context, null);
    }

    public FragmentBreadCrumbs(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R.attr.fragmentBreadCrumbsStyle);
    }

    public FragmentBreadCrumbs(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public FragmentBreadCrumbs(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mMaxVisible = -1;
        this.mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view.getTag() instanceof FragmentManager.BackStackEntry) {
                    FragmentManager.BackStackEntry backStackEntry = (FragmentManager.BackStackEntry) view.getTag();
                    if (backStackEntry == FragmentBreadCrumbs.this.mParentEntry) {
                        if (FragmentBreadCrumbs.this.mParentClickListener != null) {
                            FragmentBreadCrumbs.this.mParentClickListener.onClick(view);
                            return;
                        }
                        return;
                    }
                    if (FragmentBreadCrumbs.this.mOnBreadCrumbClickListener != null) {
                        if (FragmentBreadCrumbs.this.mOnBreadCrumbClickListener.onBreadCrumbClick(backStackEntry == FragmentBreadCrumbs.this.mTopEntry ? null : backStackEntry, 0)) {
                            return;
                        }
                    }
                    if (backStackEntry == FragmentBreadCrumbs.this.mTopEntry) {
                        FragmentBreadCrumbs.this.mActivity.getFragmentManager().popBackStack();
                    } else {
                        FragmentBreadCrumbs.this.mActivity.getFragmentManager().popBackStack(backStackEntry.getId(), 0);
                    }
                }
            }
        };
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.FragmentBreadCrumbs, i, i2);
        this.mGravity = typedArrayObtainStyledAttributes.getInt(0, DEFAULT_GRAVITY);
        this.mLayoutResId = typedArrayObtainStyledAttributes.getResourceId(2, R.layout.fragment_bread_crumb_item);
        this.mTextColor = typedArrayObtainStyledAttributes.getColor(1, 0);
        typedArrayObtainStyledAttributes.recycle();
    }

    public void setActivity(Activity activity) {
        this.mActivity = activity;
        this.mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mContainer = (LinearLayout) this.mInflater.inflate(R.layout.fragment_bread_crumbs, (ViewGroup) this, false);
        addView(this.mContainer);
        activity.getFragmentManager().addOnBackStackChangedListener(this);
        updateCrumbs();
        setLayoutTransition(new LayoutTransition());
    }

    public void setMaxVisible(int i) {
        if (i < 1) {
            throw new IllegalArgumentException("visibleCrumbs must be greater than zero");
        }
        this.mMaxVisible = i;
    }

    public void setParentTitle(CharSequence charSequence, CharSequence charSequence2, View.OnClickListener onClickListener) {
        this.mParentEntry = createBackStackEntry(charSequence, charSequence2);
        this.mParentClickListener = onClickListener;
        updateCrumbs();
    }

    public void setOnBreadCrumbClickListener(OnBreadCrumbClickListener onBreadCrumbClickListener) {
        this.mOnBreadCrumbClickListener = onBreadCrumbClickListener;
    }

    private BackStackRecord createBackStackEntry(CharSequence charSequence, CharSequence charSequence2) {
        if (charSequence == null) {
            return null;
        }
        BackStackRecord backStackRecord = new BackStackRecord((FragmentManagerImpl) this.mActivity.getFragmentManager());
        backStackRecord.setBreadCrumbTitle(charSequence);
        backStackRecord.setBreadCrumbShortTitle(charSequence2);
        return backStackRecord;
    }

    public void setTitle(CharSequence charSequence, CharSequence charSequence2) {
        this.mTopEntry = createBackStackEntry(charSequence, charSequence2);
        updateCrumbs();
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int measuredWidth;
        int measuredWidth2;
        if (getChildCount() == 0) {
            return;
        }
        View childAt = getChildAt(0);
        int i5 = this.mPaddingTop;
        int measuredHeight = (this.mPaddingTop + childAt.getMeasuredHeight()) - this.mPaddingBottom;
        int absoluteGravity = Gravity.getAbsoluteGravity(this.mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK, getLayoutDirection());
        if (absoluteGravity == 1) {
            measuredWidth = this.mPaddingLeft + (((this.mRight - this.mLeft) - childAt.getMeasuredWidth()) / 2);
            measuredWidth2 = childAt.getMeasuredWidth() + measuredWidth;
        } else if (absoluteGravity == 5) {
            measuredWidth2 = (this.mRight - this.mLeft) - this.mPaddingRight;
            measuredWidth = measuredWidth2 - childAt.getMeasuredWidth();
        } else {
            measuredWidth = this.mPaddingLeft;
            measuredWidth2 = childAt.getMeasuredWidth() + measuredWidth;
        }
        if (measuredWidth < this.mPaddingLeft) {
            measuredWidth = this.mPaddingLeft;
        }
        if (measuredWidth2 > (this.mRight - this.mLeft) - this.mPaddingRight) {
            measuredWidth2 = (this.mRight - this.mLeft) - this.mPaddingRight;
        }
        childAt.layout(measuredWidth, i5, measuredWidth2, measuredHeight);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int childCount = getChildCount();
        int iMax = 0;
        int iMax2 = 0;
        int iCombineMeasuredStates = 0;
        for (int i3 = 0; i3 < childCount; i3++) {
            View childAt = getChildAt(i3);
            if (childAt.getVisibility() != 8) {
                measureChild(childAt, i, i2);
                iMax = Math.max(iMax, childAt.getMeasuredWidth());
                iMax2 = Math.max(iMax2, childAt.getMeasuredHeight());
                iCombineMeasuredStates = combineMeasuredStates(iCombineMeasuredStates, childAt.getMeasuredState());
            }
        }
        setMeasuredDimension(resolveSizeAndState(Math.max(iMax + this.mPaddingLeft + this.mPaddingRight, getSuggestedMinimumWidth()), i, iCombineMeasuredStates), resolveSizeAndState(Math.max(iMax2 + this.mPaddingTop + this.mPaddingBottom, getSuggestedMinimumHeight()), i2, iCombineMeasuredStates << 16));
    }

    @Override
    public void onBackStackChanged() {
        updateCrumbs();
    }

    private int getPreEntryCount() {
        return (this.mTopEntry != null ? 1 : 0) + (this.mParentEntry != null ? 1 : 0);
    }

    private FragmentManager.BackStackEntry getPreEntry(int i) {
        if (this.mParentEntry != null) {
            return i == 0 ? this.mParentEntry : this.mTopEntry;
        }
        return this.mTopEntry;
    }

    void updateCrumbs() {
        int i;
        int i2;
        FragmentManager.BackStackEntry backStackEntryAt;
        FragmentManager fragmentManager = this.mActivity.getFragmentManager();
        int backStackEntryCount = fragmentManager.getBackStackEntryCount();
        int preEntryCount = getPreEntryCount();
        int childCount = this.mContainer.getChildCount();
        int i3 = 0;
        while (true) {
            i = backStackEntryCount + preEntryCount;
            if (i3 >= i) {
                break;
            }
            if (i3 < preEntryCount) {
                backStackEntryAt = getPreEntry(i3);
            } else {
                backStackEntryAt = fragmentManager.getBackStackEntryAt(i3 - preEntryCount);
            }
            if (i3 < childCount && this.mContainer.getChildAt(i3).getTag() != backStackEntryAt) {
                for (int i4 = i3; i4 < childCount; i4++) {
                    this.mContainer.removeViewAt(i3);
                }
                childCount = i3;
            }
            if (i3 >= childCount) {
                View viewInflate = this.mInflater.inflate(this.mLayoutResId, (ViewGroup) this, false);
                TextView textView = (TextView) viewInflate.findViewById(16908310);
                textView.setText(backStackEntryAt.getBreadCrumbTitle());
                textView.setTag(backStackEntryAt);
                textView.setTextColor(this.mTextColor);
                if (i3 == 0) {
                    viewInflate.findViewById(R.id.left_icon).setVisibility(8);
                }
                this.mContainer.addView(viewInflate);
                textView.setOnClickListener(this.mOnClickListener);
            }
            i3++;
        }
        int childCount2 = this.mContainer.getChildCount();
        while (childCount2 > i) {
            this.mContainer.removeViewAt(childCount2 - 1);
            childCount2--;
        }
        int i5 = 0;
        while (i5 < childCount2) {
            View childAt = this.mContainer.getChildAt(i5);
            childAt.findViewById(16908310).setEnabled(i5 < childCount2 + (-1));
            if (this.mMaxVisible > 0) {
                childAt.setVisibility(i5 < childCount2 - this.mMaxVisible ? 8 : 0);
                View viewFindViewById = childAt.findViewById(R.id.left_icon);
                if (i5 > childCount2 - this.mMaxVisible && i5 != 0) {
                    i2 = 0;
                } else {
                    i2 = 8;
                }
                viewFindViewById.setVisibility(i2);
            }
            i5++;
        }
    }
}
