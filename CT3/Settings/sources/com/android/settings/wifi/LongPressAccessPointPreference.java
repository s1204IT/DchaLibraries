package com.android.settings.wifi;

import android.app.Fragment;
import android.content.Context;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.AccessPointPreference;

public class LongPressAccessPointPreference extends AccessPointPreference {
    private final Fragment mFragment;

    public LongPressAccessPointPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mFragment = null;
    }

    public LongPressAccessPointPreference(AccessPoint accessPoint, Context context, AccessPointPreference.UserBadgeCache cache, boolean forSavedNetworks, Fragment fragment) {
        super(accessPoint, context, cache, forSavedNetworks);
        this.mFragment = fragment;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        if (this.mFragment == null) {
            return;
        }
        view.itemView.setOnCreateContextMenuListener(this.mFragment);
        view.itemView.setTag(this);
        view.itemView.setLongClickable(true);
    }
}
