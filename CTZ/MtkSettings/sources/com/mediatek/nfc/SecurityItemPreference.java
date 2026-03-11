package com.mediatek.nfc;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;
import com.android.settings.R;

class SecurityItemPreference extends Preference implements View.OnClickListener {
    private boolean mChecked;
    private RadioButton mPreferenceButton;
    private TextView mPreferenceTitle;
    private CharSequence mTitleValue;

    public SecurityItemPreference(Context context) {
        super(context);
        this.mPreferenceTitle = null;
        this.mPreferenceButton = null;
        this.mTitleValue = "";
        this.mChecked = false;
        setLayoutResource(R.layout.card_emulation_item);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        this.mPreferenceTitle = (TextView) preferenceViewHolder.findViewById(R.id.preference_title);
        this.mPreferenceTitle.setText(this.mTitleValue);
        this.mPreferenceButton = (RadioButton) preferenceViewHolder.findViewById(R.id.preference_radiobutton);
        this.mPreferenceButton.setOnClickListener(this);
        this.mPreferenceButton.setChecked(this.mChecked);
    }

    @Override
    public void setTitle(CharSequence charSequence) {
        if (this.mPreferenceTitle == null) {
            this.mTitleValue = charSequence;
        }
        if (!charSequence.equals(this.mTitleValue)) {
            this.mTitleValue = charSequence;
            this.mPreferenceTitle.setText(this.mTitleValue);
        }
    }

    @Override
    public void onClick(View view) {
        boolean z = !isChecked();
        if (!z) {
            Log.d("@M_SecurityItemPreference", "button.onClick return");
        } else if (setChecked(z)) {
            callChangeListener(Boolean.valueOf(z));
            Log.d("@M_SecurityItemPreference", "button.onClick");
        }
    }

    public boolean isChecked() {
        return this.mChecked;
    }

    public boolean setChecked(boolean z) {
        if (this.mPreferenceButton == null) {
            Log.d("@M_SecurityItemPreference", "setChecked return");
            this.mChecked = z;
            return false;
        }
        if (this.mChecked == z) {
            return false;
        }
        this.mPreferenceButton.setChecked(z);
        this.mChecked = z;
        return true;
    }
}
