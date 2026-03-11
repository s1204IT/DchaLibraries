package com.android.settings.wifi;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.settings.R;

public class WifiDetailPreference extends Preference {
    private String mDetailText;

    public WifiDetailPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setWidgetLayoutResource(R.layout.preference_widget_summary);
    }

    public void setDetailText(String str) {
        if (TextUtils.equals(this.mDetailText, str)) {
            return;
        }
        this.mDetailText = str;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        TextView textView = (TextView) preferenceViewHolder.findViewById(R.id.widget_summary);
        textView.setText(this.mDetailText);
        textView.setPadding(0, 0, 10, 0);
    }
}
