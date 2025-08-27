package com.android.settings.password;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.UserHandle;
import com.android.settings.R;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class ChooseLockGenericController {
    private final Context mContext;
    private DevicePolicyManager mDpm;
    private ManagedLockPasswordProvider mManagedPasswordProvider;
    private final int mUserId;

    public ChooseLockGenericController(Context context, int i) {
        this(context, i, (DevicePolicyManager) context.getSystemService(DevicePolicyManager.class), ManagedLockPasswordProvider.get(context, i));
    }

    ChooseLockGenericController(Context context, int i, DevicePolicyManager devicePolicyManager, ManagedLockPasswordProvider managedLockPasswordProvider) {
        this.mContext = context;
        this.mUserId = i;
        this.mManagedPasswordProvider = managedLockPasswordProvider;
        this.mDpm = devicePolicyManager;
    }

    public int upgradeQuality(int i) {
        return Math.max(i, this.mDpm.getPasswordQuality(null, this.mUserId));
    }

    public boolean isScreenLockVisible(ScreenLockType screenLockType) {
        switch (screenLockType) {
            case NONE:
                return !this.mContext.getResources().getBoolean(R.bool.config_hide_none_security_option);
            case SWIPE:
                return !this.mContext.getResources().getBoolean(R.bool.config_hide_swipe_security_option) && this.mUserId == UserHandle.myUserId();
            case MANAGED:
                return this.mManagedPasswordProvider.isManagedPasswordChoosable();
            default:
                return true;
        }
    }

    public boolean isScreenLockEnabled(ScreenLockType screenLockType, int i) {
        return screenLockType.maxQuality >= i;
    }

    public boolean isScreenLockDisabledByAdmin(ScreenLockType screenLockType, int i) {
        boolean z = screenLockType.maxQuality < i;
        return screenLockType == ScreenLockType.MANAGED ? z || !this.mManagedPasswordProvider.isManagedPasswordChoosable() : z;
    }

    public CharSequence getTitle(ScreenLockType screenLockType) {
        switch (screenLockType) {
            case NONE:
                return this.mContext.getText(R.string.unlock_set_unlock_off_title);
            case SWIPE:
                return this.mContext.getText(R.string.unlock_set_unlock_none_title);
            case MANAGED:
                return this.mManagedPasswordProvider.getPickerOptionTitle(false);
            case PATTERN:
                return this.mContext.getText(R.string.unlock_set_unlock_pattern_title);
            case PIN:
                return this.mContext.getText(R.string.unlock_set_unlock_pin_title);
            case PASSWORD:
                return this.mContext.getText(R.string.unlock_set_unlock_password_title);
            default:
                return null;
        }
    }

    public List<ScreenLockType> getVisibleScreenLockTypes(int i, boolean z) {
        int iUpgradeQuality = upgradeQuality(i);
        ArrayList arrayList = new ArrayList();
        for (ScreenLockType screenLockType : ScreenLockType.values()) {
            if (isScreenLockVisible(screenLockType) && (z || isScreenLockEnabled(screenLockType, iUpgradeQuality))) {
                arrayList.add(screenLockType);
            }
        }
        return arrayList;
    }
}
