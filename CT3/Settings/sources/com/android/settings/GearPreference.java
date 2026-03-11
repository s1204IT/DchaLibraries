package com.android.settings;

import android.content.Context;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import com.android.settingslib.RestrictedPreference;

public class GearPreference extends RestrictedPreference implements View.OnClickListener {
    private OnGearClickListener mOnGearClickListener;

    public interface OnGearClickListener {
        void onGearClick(GearPreference gearPreference);
    }

    public GearPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.preference_widget_settings);
    }

    public void setOnGearClickListener(OnGearClickListener l) {
        this.mOnGearClickListener = l;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View gear = holder.findViewById(R.id.settings_button);
        gear.setOnClickListener(this);
        gear.setEnabled(true);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() != R.id.settings_button || this.mOnGearClickListener == null) {
            return;
        }
        this.mOnGearClickListener.onGearClick(this);
    }
}
