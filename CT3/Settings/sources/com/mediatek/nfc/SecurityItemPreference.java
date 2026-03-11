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
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        this.mPreferenceTitle = (TextView) holder.findViewById(R.id.preference_title);
        this.mPreferenceTitle.setText(this.mTitleValue);
        this.mPreferenceButton = (RadioButton) holder.findViewById(R.id.preference_radiobutton);
        this.mPreferenceButton.setOnClickListener(this);
        this.mPreferenceButton.setChecked(this.mChecked);
    }

    @Override
    public void setTitle(CharSequence title) {
        if (this.mPreferenceTitle == null) {
            this.mTitleValue = title;
        }
        if (title.equals(this.mTitleValue)) {
            return;
        }
        this.mTitleValue = title;
        this.mPreferenceTitle.setText(this.mTitleValue);
    }

    @Override
    public void onClick(View v) {
        boolean newValue = !isChecked();
        if (!newValue) {
            Log.d("@M_SecurityItemPreference", "button.onClick return");
        } else {
            if (!setChecked(newValue)) {
                return;
            }
            callChangeListener(Boolean.valueOf(newValue));
            Log.d("@M_SecurityItemPreference", "button.onClick");
        }
    }

    public boolean isChecked() {
        return this.mChecked;
    }

    public boolean setChecked(boolean checked) {
        if (this.mPreferenceButton == null) {
            Log.d("@M_SecurityItemPreference", "setChecked return");
            this.mChecked = checked;
            return false;
        }
        if (this.mChecked == checked) {
            return false;
        }
        this.mPreferenceButton.setChecked(checked);
        this.mChecked = checked;
        return true;
    }
}
