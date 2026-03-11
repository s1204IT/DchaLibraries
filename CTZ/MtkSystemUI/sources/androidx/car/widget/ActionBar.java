package androidx.car.widget;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Space;
import androidx.car.R;
import java.util.Locale;

public class ActionBar extends RelativeLayout {
    private ViewGroup mActionBarWrapper;
    private View mDefaultExpandCollapseView;
    private final SparseArray<View> mFixedViews;
    private boolean mIsExpanded;
    private int mNumColumns;
    private int mNumExtraRowsInUse;
    private int mNumRows;
    private ViewGroup mRowsContainer;
    private FrameLayout[] mSlots;

    public ActionBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mFixedViews = new SparseArray<>();
        init(context, attrs, 0, 0);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttrs, int defStyleRes) {
        inflate(context, R.layout.action_bar, this);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ActionBar, defStyleAttrs, defStyleRes);
        this.mNumColumns = Math.max(ta.getInteger(R.styleable.ActionBar_columns, 3), 3);
        ta.recycle();
        this.mActionBarWrapper = (ViewGroup) findViewById(R.id.action_bar_wrapper);
        this.mRowsContainer = (ViewGroup) findViewById(R.id.rows_container);
        this.mNumRows = this.mRowsContainer.getChildCount();
        this.mSlots = new FrameLayout[this.mNumColumns * this.mNumRows];
        for (int i = 0; i < this.mNumRows; i++) {
            ViewGroup mRow = (ViewGroup) this.mRowsContainer.getChildAt((this.mNumRows - i) - 1);
            Space space = new Space(context);
            mRow.addView(space);
            space.setLayoutParams(new LinearLayout.LayoutParams(0, -1, 0.5f));
            for (int j = 0; j < this.mNumColumns; j++) {
                int pos = (this.mNumColumns * i) + j;
                this.mSlots[pos] = (FrameLayout) inflate(context, R.layout.action_bar_slot, null);
                this.mSlots[pos].setLayoutParams(new LinearLayout.LayoutParams(0, -1, 1.0f));
                mRow.addView(this.mSlots[pos]);
            }
            Space space2 = new Space(context);
            mRow.addView(space2);
            space2.setLayoutParams(new LinearLayout.LayoutParams(0, -1, 0.5f));
        }
        this.mDefaultExpandCollapseView = createIconButton(context, R.drawable.ic_overflow);
        this.mDefaultExpandCollapseView.setContentDescription(context.getString(R.string.action_bar_expand_collapse_button));
        this.mDefaultExpandCollapseView.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                this.f$0.onExpandCollapse();
            }
        });
    }

    private int getSlotIndex(int slotPosition) {
        switch (slotPosition) {
            case 0:
                return this.mNumColumns / 2;
            case 1:
                if (this.mNumColumns < 3) {
                    return -1;
                }
                return (this.mNumColumns / 2) - 1;
            case 2:
                if (this.mNumColumns < 2) {
                    return -1;
                }
                return (this.mNumColumns / 2) + 1;
            case 3:
                return this.mNumColumns - 1;
            default:
                throw new IllegalArgumentException("Unknown position: " + slotPosition);
        }
    }

    private ImageButton createIconButton(Context context, int iconResId) {
        ImageButton button = (ImageButton) inflate(context, R.layout.action_bar_button, null);
        Drawable icon = context.getDrawable(iconResId);
        button.setImageDrawable(icon);
        return button;
    }

    public void onExpandCollapse() {
        this.mIsExpanded = !this.mIsExpanded;
        this.mSlots[getSlotIndex(3)].setActivated(this.mIsExpanded);
        int animationDuration = getContext().getResources().getInteger(this.mIsExpanded ? R.integer.car_action_bar_expand_anim_duration : R.integer.car_action_bar_collapse_anim_duration);
        TransitionSet set = new TransitionSet().addTransition(new ChangeBounds()).addTransition(new Fade()).setDuration(animationDuration).setInterpolator((TimeInterpolator) new FastOutSlowInInterpolator());
        TransitionManager.beginDelayedTransition(this.mActionBarWrapper, set);
        for (int i = 0; i < this.mNumExtraRowsInUse; i++) {
            this.mRowsContainer.getChildAt(i).setVisibility(this.mIsExpanded ? 0 : 8);
        }
    }

    View getViewAt(int rowIdx, int colIdx) {
        if (rowIdx < 0 || rowIdx > this.mRowsContainer.getChildCount()) {
            throw new IllegalArgumentException(String.format((Locale) null, "Row index out of range (requested: %d, max: %d)", Integer.valueOf(rowIdx), Integer.valueOf(this.mRowsContainer.getChildCount())));
        }
        if (colIdx < 0 || colIdx > this.mNumColumns) {
            throw new IllegalArgumentException(String.format((Locale) null, "Column index out of range (requested: %d, max: %d)", Integer.valueOf(colIdx), Integer.valueOf(this.mNumColumns)));
        }
        FrameLayout slot = (FrameLayout) ((LinearLayout) this.mRowsContainer.getChildAt(rowIdx)).getChildAt(colIdx + 1);
        if (slot.getChildCount() > 0) {
            return slot.getChildAt(0);
        }
        return null;
    }
}
