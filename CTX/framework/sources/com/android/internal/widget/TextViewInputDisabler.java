package com.android.internal.widget;

import android.text.InputFilter;
import android.text.Spanned;
import android.widget.TextView;

public class TextViewInputDisabler {
    private InputFilter[] mDefaultFilters;
    private InputFilter[] mNoInputFilters = {new InputFilter() {
        @Override
        public CharSequence filter(CharSequence charSequence, int i, int i2, Spanned spanned, int i3, int i4) {
            return "";
        }
    }};
    private TextView mTextView;

    public TextViewInputDisabler(TextView textView) {
        this.mTextView = textView;
        this.mDefaultFilters = this.mTextView.getFilters();
    }

    public void setInputEnabled(boolean z) {
        this.mTextView.setFilters(z ? this.mDefaultFilters : this.mNoInputFilters);
    }
}
