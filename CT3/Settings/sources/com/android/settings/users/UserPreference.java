package com.android.settings.users;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;
import java.util.Comparator;

public class UserPreference extends RestrictedPreference {
    public static final Comparator<UserPreference> SERIAL_NUMBER_COMPARATOR = new Comparator<UserPreference>() {
        @Override
        public int compare(UserPreference p1, UserPreference p2) {
            int sn1 = p1.getSerialNumber();
            int sn2 = p2.getSerialNumber();
            if (sn1 < sn2) {
                return -1;
            }
            if (sn1 > sn2) {
                return 1;
            }
            return 0;
        }
    };
    private View.OnClickListener mDeleteClickListener;
    private int mSerialNumber;
    private View.OnClickListener mSettingsClickListener;
    private int mUserId;

    UserPreference(Context context, AttributeSet attrs, int userId, View.OnClickListener settingsListener, View.OnClickListener deleteListener) {
        super(context, attrs);
        this.mSerialNumber = -1;
        this.mUserId = -10;
        if (deleteListener != null || settingsListener != null) {
            setWidgetLayoutResource(R.layout.restricted_preference_user_delete_widget);
        }
        this.mDeleteClickListener = deleteListener;
        this.mSettingsClickListener = settingsListener;
        this.mUserId = userId;
        useAdminDisabledSummary(true);
    }

    private void dimIcon(boolean dimmed) {
        Drawable icon = getIcon();
        if (icon == null) {
            return;
        }
        icon.mutate().setAlpha(dimmed ? 102 : 255);
        setIcon(icon);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        boolean disabledByAdmin = isDisabledByAdmin();
        dimIcon(disabledByAdmin);
        View userDeleteWidget = view.findViewById(R.id.user_delete_widget);
        if (userDeleteWidget != null) {
            userDeleteWidget.setVisibility(disabledByAdmin ? 8 : 0);
        }
        if (disabledByAdmin) {
            return;
        }
        View deleteDividerView = view.findViewById(R.id.divider_delete);
        View manageDividerView = view.findViewById(R.id.divider_manage);
        View deleteView = view.findViewById(R.id.trash_user);
        if (deleteView != null) {
            if (this.mDeleteClickListener != null && !RestrictedLockUtils.hasBaseUserRestriction(getContext(), "no_remove_user", UserHandle.myUserId())) {
                deleteView.setVisibility(0);
                deleteDividerView.setVisibility(0);
                deleteView.setOnClickListener(this.mDeleteClickListener);
                deleteView.setTag(this);
            } else {
                deleteView.setVisibility(8);
                deleteDividerView.setVisibility(8);
            }
        }
        ImageView manageView = (ImageView) view.findViewById(R.id.manage_user);
        if (manageView == null) {
            return;
        }
        if (this.mSettingsClickListener != null) {
            manageView.setVisibility(0);
            manageDividerView.setVisibility(this.mDeleteClickListener != null ? 8 : 0);
            manageView.setOnClickListener(this.mSettingsClickListener);
            manageView.setTag(this);
            return;
        }
        manageView.setVisibility(8);
        manageDividerView.setVisibility(8);
    }

    public int getSerialNumber() {
        if (this.mUserId == UserHandle.myUserId()) {
            return Integer.MIN_VALUE;
        }
        if (this.mSerialNumber < 0) {
            if (this.mUserId == -10) {
                return Integer.MAX_VALUE;
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
