package com.android.browser.preferences;

import android.content.Context;
import android.preference.SeekBarPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import com.android.browser.R;

public class SeekBarSummaryPreference extends SeekBarPreference {
    CharSequence mSummary;
    TextView mSummaryView;

    public SeekBarSummaryPreference(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init();
    }

    public SeekBarSummaryPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    public SeekBarSummaryPreference(Context context) {
        super(context);
        init();
    }

    void init() {
        setWidgetLayoutResource(R.layout.font_size_widget);
    }

    public void setSummary(CharSequence charSequence) {
        this.mSummary = charSequence;
        if (this.mSummaryView != null) {
            this.mSummaryView.setText(this.mSummary);
        }
    }

    public CharSequence getSummary() {
        return null;
    }

    protected void onBindView(View view) {
        super.onBindView(view);
        this.mSummaryView = (TextView) view.findViewById(R.id.text);
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
}
