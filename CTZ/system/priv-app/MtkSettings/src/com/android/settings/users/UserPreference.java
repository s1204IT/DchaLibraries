package com.android.settings.users;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.wifi.AccessPoint;
import java.util.Comparator;

/* loaded from: classes.dex */
public class UserPreference extends RestrictedPreference {
    public static final Comparator<UserPreference> SERIAL_NUMBER_COMPARATOR = new Comparator<UserPreference>() { // from class: com.android.settings.users.UserPreference.1
        /* JADX DEBUG: Method merged with bridge method: compare(Ljava/lang/Object;Ljava/lang/Object;)I */
        @Override // java.util.Comparator
        public int compare(UserPreference userPreference, UserPreference userPreference2) {
            int serialNumber = userPreference.getSerialNumber();
            int serialNumber2 = userPreference2.getSerialNumber();
            if (serialNumber < serialNumber2) {
                return -1;
            }
            if (serialNumber > serialNumber2) {
                return 1;
            }
            return 0;
        }
    };
    private View.OnClickListener mDeleteClickListener;
    private int mSerialNumber;
    private View.OnClickListener mSettingsClickListener;
    private int mUserId;

    public UserPreference(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, -10, null, null);
    }

    UserPreference(Context context, AttributeSet attributeSet, int i, View.OnClickListener onClickListener, View.OnClickListener onClickListener2) {
        super(context, attributeSet);
        this.mSerialNumber = -1;
        this.mUserId = -10;
        if (onClickListener2 != null || onClickListener != null) {
            setWidgetLayoutResource(R.layout.restricted_preference_user_delete_widget);
        }
        this.mDeleteClickListener = onClickListener2;
        this.mSettingsClickListener = onClickListener;
        this.mUserId = i;
        useAdminDisabledSummary(true);
    }

    private void dimIcon(boolean z) {
        Drawable icon = getIcon();
        if (icon != null) {
            icon.mutate().setAlpha(z ? 102 : 255);
            setIcon(icon);
        }
    }

    @Override // com.android.settingslib.RestrictedPreference, com.android.settingslib.TwoTargetPreference
    protected boolean shouldHideSecondTarget() {
        if (isDisabledByAdmin()) {
            return true;
        }
        return !canDeleteUser() && this.mSettingsClickListener == null;
    }

    @Override // com.android.settingslib.RestrictedPreference, com.android.settingslib.TwoTargetPreference, android.support.v7.preference.Preference
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        boolean zIsDisabledByAdmin = isDisabledByAdmin();
        dimIcon(zIsDisabledByAdmin);
        View viewFindViewById = preferenceViewHolder.findViewById(R.id.user_delete_widget);
        int i = 0;
        if (viewFindViewById != null) {
            viewFindViewById.setVisibility(zIsDisabledByAdmin ? 8 : 0);
        }
        if (!zIsDisabledByAdmin) {
            View viewFindViewById2 = preferenceViewHolder.findViewById(R.id.divider_delete);
            View viewFindViewById3 = preferenceViewHolder.findViewById(R.id.divider_manage);
            View viewFindViewById4 = preferenceViewHolder.findViewById(R.id.trash_user);
            if (viewFindViewById4 != null) {
                if (canDeleteUser()) {
                    viewFindViewById4.setVisibility(0);
                    viewFindViewById2.setVisibility(0);
                    viewFindViewById4.setOnClickListener(this.mDeleteClickListener);
                    viewFindViewById4.setTag(this);
                } else {
                    viewFindViewById4.setVisibility(8);
                    viewFindViewById2.setVisibility(8);
                }
            }
            ImageView imageView = (ImageView) preferenceViewHolder.findViewById(R.id.manage_user);
            if (imageView != null) {
                if (this.mSettingsClickListener != null) {
                    imageView.setVisibility(0);
                    if (this.mDeleteClickListener != null) {
                        i = 8;
                    }
                    viewFindViewById3.setVisibility(i);
                    imageView.setOnClickListener(this.mSettingsClickListener);
                    imageView.setTag(this);
                    return;
                }
                imageView.setVisibility(8);
                viewFindViewById3.setVisibility(8);
            }
        }
    }

    private boolean canDeleteUser() {
        return (this.mDeleteClickListener == null || RestrictedLockUtils.hasBaseUserRestriction(getContext(), "no_remove_user", UserHandle.myUserId())) ? false : true;
    }

    private int getSerialNumber() {
        if (this.mUserId == UserHandle.myUserId()) {
            return AccessPoint.UNREACHABLE_RSSI;
        }
        if (this.mSerialNumber < 0) {
            if (this.mUserId == -10) {
                return Preference.DEFAULT_ORDER;
            }
            if (this.mUserId == -11) {
                return 2147483646;
            }
            this.mSerialNumber = ((UserManager) getContext().getSystemService("user")).getUserSerialNumber(this.mUserId);
            if (this.mSerialNumber < 0) {
                return this.mUserId;
            }
        }
        return this.mSerialNumber;
    }

    public int getUserId() {
        return this.mUserId;
    }
}
