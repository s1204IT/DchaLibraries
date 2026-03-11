package com.android.settings.wifi;

import android.content.Context;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.View;
import com.android.settings.R;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.AccessPointPreference;

public class ConnectedAccessPointPreference extends AccessPointPreference implements View.OnClickListener {
    private boolean mIsCaptivePortal;
    private OnGearClickListener mOnGearClickListener;

    public interface OnGearClickListener {
        void onGearClick(ConnectedAccessPointPreference connectedAccessPointPreference);
    }

    public ConnectedAccessPointPreference(AccessPoint accessPoint, Context context, AccessPointPreference.UserBadgeCache userBadgeCache, int i, boolean z) {
        super(accessPoint, context, userBadgeCache, i, z);
    }

    @Override
    protected int getWidgetLayoutResourceId() {
        return R.layout.preference_widget_gear_optional_background;
    }

    @Override
    public void refresh() {
        super.refresh();
        setShowDivider(this.mIsCaptivePortal);
        if (this.mIsCaptivePortal) {
            setSummary(R.string.wifi_tap_to_sign_in);
        }
    }

    public void setOnGearClickListener(OnGearClickListener onGearClickListener) {
        this.mOnGearClickListener = onGearClickListener;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        View viewFindViewById = preferenceViewHolder.findViewById(R.id.settings_button);
        viewFindViewById.setOnClickListener(this);
        preferenceViewHolder.findViewById(R.id.settings_button_no_background).setVisibility(this.mIsCaptivePortal ? 4 : 0);
        viewFindViewById.setVisibility(this.mIsCaptivePortal ? 0 : 4);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.settings_button && this.mOnGearClickListener != null) {
            this.mOnGearClickListener.onGearClick(this);
        }
    }

    public void setCaptivePortal(boolean z) {
        if (this.mIsCaptivePortal != z) {
            this.mIsCaptivePortal = z;
            refresh();
        }
    }
}
