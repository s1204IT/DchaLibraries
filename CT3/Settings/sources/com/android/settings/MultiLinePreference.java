package com.android.settings;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.TextView;

public class MultiLinePreference extends Preference {
    public MultiLinePreference(Context ctx, AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle);
    }

    public MultiLinePreference(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
    }

    public MultiLinePreference(Context ctx) {
        super(ctx);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        TextView textView = (TextView) view.findViewById(android.R.id.title);
        if (textView == null) {
            return;
        }
        textView.setSingleLine(false);
    }
}
