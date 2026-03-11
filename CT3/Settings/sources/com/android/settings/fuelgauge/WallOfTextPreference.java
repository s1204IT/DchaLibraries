package com.android.settings.fuelgauge;

import android.R;
import android.content.Context;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.settings.DividerPreference;

public class WallOfTextPreference extends DividerPreference {
    public WallOfTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        TextView summary = (TextView) view.findViewById(R.id.summary);
        summary.setMaxLines(20);
    }
}
