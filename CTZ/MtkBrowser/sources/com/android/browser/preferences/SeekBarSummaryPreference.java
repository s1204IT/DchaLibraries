package com.android.browser.preferences;

import android.content.Context;
import android.preference.SeekBarPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

public class SeekBarSummaryPreference extends SeekBarPreference {
    CharSequence mSummary;
    TextView mSummaryView;

    public SeekBarSummaryPreference(Context context) {
        super(context);
        init();
    }

    public SeekBarSummaryPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    public SeekBarSummaryPreference(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init();
    }

    public CharSequence getSummary() {
        return null;
    }

    void init() {
        setWidgetLayoutResource(2130968600);
    }

    protected void onBindView(View view) {
        super.onBindView(view);
        this.mSummaryView = (TextView) view.findViewById(2131558479);
        if (TextUtils.isEmpty(this.mSummary)) {
            this.mSummaryView.setVisibility(8);
        } else {
            this.mSummaryView.setVisibility(0);
            this.mSummaryView.setText(this.mSummary);
        }
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    public void setSummary(CharSequence charSequence) {
        this.mSummary = charSequence;
        if (this.mSummaryView != null) {
            this.mSummaryView.setText(this.mSummary);
        }
    }
}
