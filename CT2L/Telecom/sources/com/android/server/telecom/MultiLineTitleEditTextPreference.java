package com.android.server.telecom;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class MultiLineTitleEditTextPreference extends EditTextPreference {
    public MultiLineTitleEditTextPreference(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public MultiLineTitleEditTextPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public MultiLineTitleEditTextPreference(Context context) {
        super(context);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        TextView textView = (TextView) view.findViewById(android.R.id.title);
        if (textView != null) {
            textView.setSingleLine(false);
        }
    }
}
