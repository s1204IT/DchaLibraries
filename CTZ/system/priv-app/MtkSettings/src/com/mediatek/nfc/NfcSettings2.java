package com.mediatek.nfc;

import android.R;
import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.TextView;

/* compiled from: NfcSettings.java */
/* renamed from: com.mediatek.nfc.NfcDescriptionPreference, reason: use source file name */
/* loaded from: classes.dex */
class NfcSettings2 extends Preference {
    public NfcSettings2(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public NfcSettings2(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override // android.support.v7.preference.Preference
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        TextView textView = (TextView) preferenceViewHolder.findViewById(R.id.title);
        if (textView != null) {
            textView.setSingleLine(false);
            textView.setMaxLines(3);
        }
    }
}
