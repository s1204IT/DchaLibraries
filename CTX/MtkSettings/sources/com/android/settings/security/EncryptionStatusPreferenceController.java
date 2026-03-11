package com.android.settings.security;

import android.content.Context;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class EncryptionStatusPreferenceController extends BasePreferenceController {
    static final String PREF_KEY_ENCRYPTION_DETAIL_PAGE = "encryption_and_credentials_encryption_status";
    static final String PREF_KEY_ENCRYPTION_SECURITY_PAGE = "encryption_and_credential";
    private final UserManager mUserManager;

    public EncryptionStatusPreferenceController(Context context, String str) {
        super(context, str);
        this.mUserManager = (UserManager) context.getSystemService("user");
    }

    @Override
    public int getAvailabilityStatus() {
        if (!TextUtils.equals(getPreferenceKey(), PREF_KEY_ENCRYPTION_DETAIL_PAGE) || this.mContext.getResources().getBoolean(R.bool.config_show_encryption_and_credentials_encryption_status)) {
            return this.mUserManager.isAdminUser() ? 0 : 3;
        }
        return 2;
    }

    @Override
    public void updateState(Preference preference) {
        if (LockPatternUtils.isDeviceEncryptionEnabled()) {
            if (TextUtils.equals(getPreferenceKey(), PREF_KEY_ENCRYPTION_DETAIL_PAGE)) {
                preference.setFragment(null);
            }
            preference.setSummary(R.string.crypt_keeper_encrypted_summary);
        } else {
            if (TextUtils.equals(getPreferenceKey(), PREF_KEY_ENCRYPTION_DETAIL_PAGE)) {
                preference.setFragment(CryptKeeperSettings.class.getName());
            }
            preference.setSummary(R.string.decryption_settings_summary);
        }
    }
}
