package com.android.settings.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import com.android.internal.R;

public class DisabledCheckBoxPreference extends CheckBoxPreference {
    private View mCheckBox;
    private boolean mEnabledCheckBox;
    private PreferenceViewHolder mViewHolder;

    public DisabledCheckBoxPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        setupDisabledCheckBoxPreference(context, attributeSet, i, i2);
    }

    public DisabledCheckBoxPreference(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        setupDisabledCheckBoxPreference(context, attributeSet, i, 0);
    }

    public DisabledCheckBoxPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setupDisabledCheckBoxPreference(context, attributeSet, 0, 0);
    }

    public DisabledCheckBoxPreference(Context context) {
        super(context);
        setupDisabledCheckBoxPreference(context, null, 0, 0);
    }

    private void setupDisabledCheckBoxPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.Preference, i, i2);
        for (int indexCount = typedArrayObtainStyledAttributes.getIndexCount() - 1; indexCount >= 0; indexCount--) {
            int index = typedArrayObtainStyledAttributes.getIndex(indexCount);
            if (index == 2) {
                this.mEnabledCheckBox = typedArrayObtainStyledAttributes.getBoolean(index, true);
            }
        }
        typedArrayObtainStyledAttributes.recycle();
        super.setEnabled(true);
        enableCheckbox(this.mEnabledCheckBox);
    }

    public void enableCheckbox(boolean z) {
        this.mEnabledCheckBox = z;
        if (this.mViewHolder != null && this.mCheckBox != null) {
            this.mCheckBox.setEnabled(this.mEnabledCheckBox);
            this.mViewHolder.itemView.setEnabled(this.mEnabledCheckBox);
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        this.mViewHolder = preferenceViewHolder;
        this.mCheckBox = preferenceViewHolder.findViewById(android.R.id.checkbox);
        enableCheckbox(this.mEnabledCheckBox);
    }

    @Override
    protected void performClick(View view) {
        if (this.mEnabledCheckBox) {
            super.performClick(view);
        }
    }
}
