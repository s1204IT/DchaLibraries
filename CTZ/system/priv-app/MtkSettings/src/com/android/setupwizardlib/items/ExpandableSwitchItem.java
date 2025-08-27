package com.android.setupwizardlib.items;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.android.setupwizardlib.R;
import com.android.setupwizardlib.view.CheckableLinearLayout;

/* loaded from: classes.dex */
public class ExpandableSwitchItem extends SwitchItem implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private CharSequence mCollapsedSummary;
    private CharSequence mExpandedSummary;
    private boolean mIsExpanded;

    public ExpandableSwitchItem() {
        this.mIsExpanded = false;
    }

    public ExpandableSwitchItem(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mIsExpanded = false;
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.SuwExpandableSwitchItem);
        this.mCollapsedSummary = typedArrayObtainStyledAttributes.getText(R.styleable.SuwExpandableSwitchItem_suwCollapsedSummary);
        this.mExpandedSummary = typedArrayObtainStyledAttributes.getText(R.styleable.SuwExpandableSwitchItem_suwExpandedSummary);
        typedArrayObtainStyledAttributes.recycle();
    }

    @Override // com.android.setupwizardlib.items.SwitchItem, com.android.setupwizardlib.items.Item
    protected int getDefaultLayoutResource() {
        return R.layout.suw_items_expandable_switch;
    }

    @Override // com.android.setupwizardlib.items.Item
    public CharSequence getSummary() {
        return this.mIsExpanded ? getExpandedSummary() : getCollapsedSummary();
    }

    public boolean isExpanded() {
        return this.mIsExpanded;
    }

    public void setExpanded(boolean z) {
        if (this.mIsExpanded == z) {
            return;
        }
        this.mIsExpanded = z;
        notifyItemChanged();
    }

    public CharSequence getCollapsedSummary() {
        return this.mCollapsedSummary;
    }

    public CharSequence getExpandedSummary() {
        return this.mExpandedSummary;
    }

    @Override // com.android.setupwizardlib.items.SwitchItem, com.android.setupwizardlib.items.Item, com.android.setupwizardlib.items.IItem
    public void onBindView(View view) {
        super.onBindView(view);
        View viewFindViewById = view.findViewById(R.id.suw_items_expandable_switch_content);
        viewFindViewById.setOnClickListener(this);
        if (viewFindViewById instanceof CheckableLinearLayout) {
            ((CheckableLinearLayout) viewFindViewById).setChecked(isExpanded());
        }
        tintCompoundDrawables(view);
        view.setFocusable(false);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        setExpanded(!isExpanded());
    }

    private void tintCompoundDrawables(View view) {
        TypedArray typedArrayObtainStyledAttributes = view.getContext().obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary});
        ColorStateList colorStateList = typedArrayObtainStyledAttributes.getColorStateList(0);
        typedArrayObtainStyledAttributes.recycle();
        if (colorStateList != null) {
            TextView textView = (TextView) view.findViewById(R.id.suw_items_title);
            for (Drawable drawable : textView.getCompoundDrawables()) {
                if (drawable != null) {
                    drawable.setColorFilter(colorStateList.getDefaultColor(), PorterDuff.Mode.SRC_IN);
                }
            }
            if (Build.VERSION.SDK_INT >= 17) {
                for (Drawable drawable2 : textView.getCompoundDrawablesRelative()) {
                    if (drawable2 != null) {
                        drawable2.setColorFilter(colorStateList.getDefaultColor(), PorterDuff.Mode.SRC_IN);
                    }
                }
            }
        }
    }
}
