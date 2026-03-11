package com.android.settings.notification;

import android.content.Context;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.settings.R;

public class NotificationFooterPreference extends Preference {
    public NotificationFooterPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet, TypedArrayUtils.getAttr(context, R.attr.footerPreferenceStyle, android.R.attr.preferenceStyle));
        init();
    }

    public NotificationFooterPreference(Context context) {
        this(context, null);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        TextView textView = (TextView) preferenceViewHolder.itemView.findViewById(android.R.id.title);
        textView.setMovementMethod(new LinkMovementMethod());
        textView.setClickable(false);
        textView.setLongClickable(false);
    }

    private void init() {
        setIcon(R.drawable.ic_info_outline_24dp);
        setSelectable(false);
    }
}
