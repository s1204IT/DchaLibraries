package com.android.settings.wifi;

import android.app.Fragment;
import android.content.Context;
import android.support.v7.preference.PreferenceViewHolder;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.AccessPointPreference;

/* loaded from: classes.dex */
public class LongPressAccessPointPreference extends AccessPointPreference {
    private final Fragment mFragment;

    public LongPressAccessPointPreference(AccessPoint accessPoint, Context context, AccessPointPreference.UserBadgeCache userBadgeCache, boolean z, Fragment fragment) {
        super(accessPoint, context, userBadgeCache, z);
        this.mFragment = fragment;
    }

    public LongPressAccessPointPreference(AccessPoint accessPoint, Context context, AccessPointPreference.UserBadgeCache userBadgeCache, boolean z, int i, Fragment fragment) {
        super(accessPoint, context, userBadgeCache, i, z);
        this.mFragment = fragment;
    }

    @Override // com.android.settingslib.wifi.AccessPointPreference, android.support.v7.preference.Preference
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        if (this.mFragment != null) {
            preferenceViewHolder.itemView.setOnCreateContextMenuListener(this.mFragment);
            preferenceViewHolder.itemView.setTag(this);
            preferenceViewHolder.itemView.setLongClickable(true);
        }
    }
}
