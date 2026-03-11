package com.android.settingslib.wifi;

import android.R;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Looper;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.settingslib.R$attr;
import com.android.settingslib.R$string;

public class AccessPointPreference extends Preference {
    private AccessPoint mAccessPoint;
    private Drawable mBadge;
    private final UserBadgeCache mBadgeCache;
    private final int mBadgePadding;
    private CharSequence mContentDescription;
    private boolean mForSavedNetworks;
    private int mLevel;
    private final Runnable mNotifyChanged;
    private TextView mTitleView;
    private final StateListDrawable mWifiSld;
    private static final int[] STATE_SECURED = {R$attr.state_encrypted};
    private static final int[] STATE_NONE = new int[0];
    private static int[] wifi_signal_attributes = {R$attr.wifi_signal};
    static final int[] WIFI_CONNECTION_STRENGTH = {R$string.accessibility_wifi_one_bar, R$string.accessibility_wifi_two_bars, R$string.accessibility_wifi_three_bars, R$string.accessibility_wifi_signal_full};

    public static class UserBadgeCache {
    }

    public AccessPointPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mForSavedNetworks = false;
        this.mNotifyChanged = new Runnable() {
            @Override
            public void run() {
                AccessPointPreference.this.notifyChanged();
            }
        };
        this.mWifiSld = null;
        this.mBadgePadding = 0;
        this.mBadgeCache = null;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        if (this.mAccessPoint == null) {
            return;
        }
        Drawable drawable = getIcon();
        if (drawable != null) {
            drawable.setLevel(this.mLevel);
        }
        this.mTitleView = (TextView) view.findViewById(R.id.title);
        if (this.mTitleView != null) {
            this.mTitleView.setCompoundDrawablesRelativeWithIntrinsicBounds((Drawable) null, (Drawable) null, this.mBadge, (Drawable) null);
            this.mTitleView.setCompoundDrawablePadding(this.mBadgePadding);
        }
        view.itemView.setContentDescription(this.mContentDescription);
    }

    @Override
    protected void notifyChanged() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            postNotifyChanged();
        } else {
            super.notifyChanged();
        }
    }

    private void postNotifyChanged() {
        if (this.mTitleView == null) {
            return;
        }
        this.mTitleView.post(this.mNotifyChanged);
    }
}
