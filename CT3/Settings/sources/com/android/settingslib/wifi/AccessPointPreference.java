package com.android.settingslib.wifi;

import android.R;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.wifi.WifiConfiguration;
import android.os.Looper;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.widget.TextView;
import com.android.settingslib.R$attr;
import com.android.settingslib.R$dimen;
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

    public AccessPointPreference(AccessPoint accessPoint, Context context, UserBadgeCache cache, boolean forSavedNetworks) {
        super(context);
        this.mForSavedNetworks = false;
        this.mNotifyChanged = new Runnable() {
            @Override
            public void run() {
                AccessPointPreference.this.notifyChanged();
            }
        };
        this.mBadgeCache = cache;
        this.mAccessPoint = accessPoint;
        this.mForSavedNetworks = forSavedNetworks;
        this.mAccessPoint.setTag(this);
        this.mLevel = -1;
        this.mWifiSld = (StateListDrawable) context.getTheme().obtainStyledAttributes(wifi_signal_attributes).getDrawable(0);
        this.mBadgePadding = context.getResources().getDimensionPixelSize(R$dimen.wifi_preference_badge_padding);
        refresh();
    }

    public AccessPoint getAccessPoint() {
        return this.mAccessPoint;
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

    protected void updateIcon(int level, Context context) {
        int[] iArr;
        if (level == -1) {
            setIcon((Drawable) null);
            return;
        }
        if (getIcon() != null || this.mWifiSld == null) {
            return;
        }
        StateListDrawable stateListDrawable = this.mWifiSld;
        if (this.mAccessPoint.getSecurity() != 0) {
            iArr = STATE_SECURED;
        } else {
            iArr = STATE_NONE;
        }
        stateListDrawable.setState(iArr);
        Drawable drawable = this.mWifiSld.getCurrent();
        if (!this.mForSavedNetworks) {
            setIcon(drawable);
        } else {
            setIcon((Drawable) null);
        }
    }

    protected void updateBadge(Context context) {
        WifiConfiguration config = this.mAccessPoint.getConfig();
        if (config == null) {
            return;
        }
        this.mBadge = this.mBadgeCache.getUserBadge(config.creatorUid);
    }

    public void refresh() {
        if (this.mForSavedNetworks) {
            setTitle(this.mAccessPoint.getConfigName());
        } else {
            setTitle(this.mAccessPoint.getSsid());
        }
        Context context = getContext();
        int level = this.mAccessPoint.getLevel();
        if (level != this.mLevel) {
            this.mLevel = level;
            updateIcon(this.mLevel, context);
            notifyChanged();
        }
        updateBadge(context);
        setSummary(this.mForSavedNetworks ? this.mAccessPoint.getSavedNetworkSummary() : this.mAccessPoint.getSettingsSummary());
        this.mContentDescription = getTitle();
        if (getSummary() != null) {
            this.mContentDescription = TextUtils.concat(this.mContentDescription, ",", getSummary());
        }
        if (level < 0 || level >= WIFI_CONNECTION_STRENGTH.length) {
            return;
        }
        this.mContentDescription = TextUtils.concat(this.mContentDescription, ",", getContext().getString(WIFI_CONNECTION_STRENGTH[level]));
    }

    @Override
    protected void notifyChanged() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            postNotifyChanged();
        } else {
            super.notifyChanged();
        }
    }

    public void onLevelChanged() {
        postNotifyChanged();
    }

    private void postNotifyChanged() {
        if (this.mTitleView == null) {
            return;
        }
        this.mTitleView.post(this.mNotifyChanged);
    }

    public static class UserBadgeCache {
        private final SparseArray<Drawable> mBadges = new SparseArray<>();
        private final PackageManager mPm;

        public UserBadgeCache(PackageManager pm) {
            this.mPm = pm;
        }

        public Drawable getUserBadge(int userId) {
            int index = this.mBadges.indexOfKey(userId);
            if (index < 0) {
                Drawable badge = this.mPm.getUserBadgeForDensity(new UserHandle(userId), 0);
                this.mBadges.put(userId, badge);
                return badge;
            }
            return this.mBadges.valueAt(index);
        }
    }
}
