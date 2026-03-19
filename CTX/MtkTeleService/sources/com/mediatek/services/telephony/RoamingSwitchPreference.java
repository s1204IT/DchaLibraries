package com.mediatek.services.telephony;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class RoamingSwitchPreference extends SwitchPreference {
    public RoamingSwitchPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        adjustViews(view);
    }

    protected void adjustViews(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                adjustViews(viewGroup.getChildAt(i));
            }
            return;
        }
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            textView.setSingleLine(false);
            textView.setEllipsize(null);
        }
    }
}
