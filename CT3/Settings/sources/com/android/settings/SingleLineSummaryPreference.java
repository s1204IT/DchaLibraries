package com.android.settings;

import android.content.Context;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.settingslib.RestrictedPreference;

public class SingleLineSummaryPreference extends RestrictedPreference {
    public SingleLineSummaryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        TextView summaryView = (TextView) view.findViewById(android.R.id.summary);
        summaryView.setSingleLine();
        summaryView.setEllipsize(TextUtils.TruncateAt.END);
    }
}
