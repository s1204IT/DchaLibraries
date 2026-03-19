package com.android.settingslib.wifi;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Looper;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settingslib.R;

public class AccessPointPreference extends Preference {
    private AccessPoint mAccessPoint;
    private Drawable mBadge;
    private final UserBadgeCache mBadgeCache;
    private final int mBadgePadding;
    private CharSequence mContentDescription;
    private int mDefaultIconResId;
    private boolean mForSavedNetworks;
    private final StateListDrawable mFrictionSld;
    private final IconInjector mIconInjector;
    private int mLevel;
    private final Runnable mNotifyChanged;
    private boolean mShowDivider;
    private TextView mTitleView;
    private int mWifiSpeed;
    private static final int[] STATE_SECURED = {R.attr.state_encrypted};
    private static final int[] STATE_METERED = {R.attr.state_metered};
    private static final int[] FRICTION_ATTRS = {R.attr.wifi_friction};
    private static final int[] WIFI_CONNECTION_STRENGTH = {R.string.accessibility_no_wifi, R.string.accessibility_wifi_one_bar, R.string.accessibility_wifi_two_bars, R.string.accessibility_wifi_three_bars, R.string.accessibility_wifi_signal_full};

    public static class UserBadgeCache {
    }

    public AccessPointPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mForSavedNetworks = false;
        this.mWifiSpeed = 0;
        this.mNotifyChanged = new Runnable() {
            @Override
            public void run() {
                AccessPointPreference.this.notifyChanged();
            }
        };
        this.mFrictionSld = null;
        this.mBadgePadding = 0;
        this.mBadgeCache = null;
        this.mIconInjector = new IconInjector(context);
    }

    AccessPointPreference(AccessPoint accessPoint, Context context, UserBadgeCache userBadgeCache, int i, boolean z, StateListDrawable stateListDrawable, int i2, IconInjector iconInjector) {
        super(context);
        this.mForSavedNetworks = false;
        this.mWifiSpeed = 0;
        this.mNotifyChanged = new Runnable() {
            @Override
            public void run() {
                AccessPointPreference.this.notifyChanged();
            }
        };
        setLayoutResource(R.layout.preference_access_point);
        setWidgetLayoutResource(getWidgetLayoutResourceId());
        this.mBadgeCache = userBadgeCache;
        this.mAccessPoint = accessPoint;
        this.mForSavedNetworks = z;
        this.mAccessPoint.setTag(this);
        this.mLevel = i2;
        this.mDefaultIconResId = i;
        this.mFrictionSld = stateListDrawable;
        this.mIconInjector = iconInjector;
        this.mBadgePadding = context.getResources().getDimensionPixelSize(R.dimen.wifi_preference_badge_padding);
    }

    protected int getWidgetLayoutResourceId() {
        return R.layout.access_point_friction_widget;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        if (this.mAccessPoint == null) {
            return;
        }
        Drawable icon = getIcon();
        if (icon != null) {
            icon.setLevel(this.mLevel);
        }
        this.mTitleView = (TextView) preferenceViewHolder.findViewById(android.R.id.title);
        if (this.mTitleView != null) {
            this.mTitleView.setCompoundDrawablesRelativeWithIntrinsicBounds((Drawable) null, (Drawable) null, this.mBadge, (Drawable) null);
            this.mTitleView.setCompoundDrawablePadding(this.mBadgePadding);
        }
        preferenceViewHolder.itemView.setContentDescription(this.mContentDescription);
        bindFrictionImage((ImageView) preferenceViewHolder.findViewById(R.id.friction_icon));
        preferenceViewHolder.findViewById(R.id.two_target_divider).setVisibility(shouldShowDivider() ? 0 : 4);
    }

    public boolean shouldShowDivider() {
        return this.mShowDivider;
    }

    private void bindFrictionImage(ImageView imageView) {
        if (imageView == null || this.mFrictionSld == null) {
            return;
        }
        if (this.mAccessPoint.getSecurity() != 0) {
            this.mFrictionSld.setState(STATE_SECURED);
        } else if (this.mAccessPoint.isMetered()) {
            this.mFrictionSld.setState(STATE_METERED);
        }
        imageView.setImageDrawable(this.mFrictionSld.getCurrent());
    }

    @Override
    protected void notifyChanged() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            postNotifyChanged();
        } else {
            super.notifyChanged();
        }
    }

    static void setTitle(AccessPointPreference accessPointPreference, AccessPoint accessPoint, boolean z) {
        if (z) {
            accessPointPreference.setTitle(accessPoint.getConfigName());
        } else {
            accessPointPreference.setTitle(accessPoint.getSsidStr());
        }
    }

    static CharSequence buildContentDescription(Context context, Preference preference, AccessPoint accessPoint) {
        String string;
        CharSequence title = preference.getTitle();
        CharSequence summary = preference.getSummary();
        if (!TextUtils.isEmpty(summary)) {
            title = TextUtils.concat(title, ",", summary);
        }
        int level = accessPoint.getLevel();
        if (level >= 0 && level < WIFI_CONNECTION_STRENGTH.length) {
            title = TextUtils.concat(title, ",", context.getString(WIFI_CONNECTION_STRENGTH[level]));
        }
        CharSequence[] charSequenceArr = new CharSequence[3];
        charSequenceArr[0] = title;
        charSequenceArr[1] = ",";
        if (accessPoint.getSecurity() == 0) {
            string = context.getString(R.string.accessibility_wifi_security_type_none);
        } else {
            string = context.getString(R.string.accessibility_wifi_security_type_secured);
        }
        charSequenceArr[2] = string;
        return TextUtils.concat(charSequenceArr);
    }

    private void postNotifyChanged() {
        if (this.mTitleView != null) {
            this.mTitleView.post(this.mNotifyChanged);
        }
    }

    static class IconInjector {
        private final Context mContext;

        public IconInjector(Context context) {
            this.mContext = context;
        }
    }
}
