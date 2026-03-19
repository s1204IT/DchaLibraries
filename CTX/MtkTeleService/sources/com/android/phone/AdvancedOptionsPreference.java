package com.android.phone;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class AdvancedOptionsPreference extends Preference {
    public AdvancedOptionsPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        setIcon(R.drawable.ic_expand_more);
        setTitle(R.string.advanced_options_title);
        ((TextView) view.findViewById(android.R.id.summary)).setMaxLines(1);
    }
}
