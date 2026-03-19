package com.android.settings.security;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.SwitchBar;
import java.util.Arrays;
import java.util.List;

public class ScreenPinningSettings extends SettingsPreferenceFragment implements Indexable, SwitchBar.OnSwitchChangeListener {
    private static final CharSequence KEY_USE_SCREEN_LOCK = "use_screen_lock";
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.screen_pinning_settings;
            return Arrays.asList(searchIndexableResource);
        }
    };
    private LockPatternUtils mLockPatternUtils;
    private SwitchBar mSwitchBar;
    private SwitchPreference mUseScreenLock;

    @Override
    public int getMetricsCategory() {
        return 86;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        SettingsActivity settingsActivity = (SettingsActivity) getActivity();
        settingsActivity.setTitle(R.string.screen_pinning_title);
        this.mLockPatternUtils = new LockPatternUtils(settingsActivity);
        this.mSwitchBar = settingsActivity.getSwitchBar();
        this.mSwitchBar.addOnSwitchChangeListener(this);
        this.mSwitchBar.show();
        this.mSwitchBar.setChecked(isLockToAppEnabled(getActivity()));
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_screen_pinning;
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        ViewGroup viewGroup = (ViewGroup) view.findViewById(android.R.id.list_container);
        View viewInflate = LayoutInflater.from(getContext()).inflate(R.layout.screen_pinning_instructions, viewGroup, false);
        viewGroup.addView(viewInflate);
        setEmptyView(viewInflate);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.mSwitchBar.removeOnSwitchChangeListener(this);
        this.mSwitchBar.hide();
    }

    private static boolean isLockToAppEnabled(Context context) {
        return Settings.System.getInt(context.getContentResolver(), "lock_to_app_enabled", 0) != 0;
    }

    private void setLockToAppEnabled(boolean z) {
        Settings.System.putInt(getContentResolver(), "lock_to_app_enabled", z ? 1 : 0);
        if (z) {
            setScreenLockUsedSetting(isScreenLockUsed());
        }
    }

    private boolean isScreenLockUsed() {
        return Settings.Secure.getInt(getContentResolver(), "lock_to_app_exit_locked", this.mLockPatternUtils.isSecure(UserHandle.myUserId()) ? 1 : 0) != 0;
    }

    private boolean setScreenLockUsed(boolean z) {
        if (z && new LockPatternUtils(getActivity()).getKeyguardStoredPasswordQuality(UserHandle.myUserId()) == 0) {
            Intent intent = new Intent("android.app.action.SET_NEW_PASSWORD");
            intent.putExtra("minimum_quality", 65536);
            startActivityForResult(intent, 43);
            return false;
        }
        setScreenLockUsedSetting(z);
        return true;
    }

    private void setScreenLockUsedSetting(boolean z) {
        Settings.Secure.putInt(getContentResolver(), "lock_to_app_exit_locked", z ? 1 : 0);
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i == 43) {
            boolean z = new LockPatternUtils(getActivity()).getKeyguardStoredPasswordQuality(UserHandle.myUserId()) != 0;
            setScreenLockUsed(z);
            this.mUseScreenLock.setChecked(z);
        }
    }

    private int getCurrentSecurityTitle() {
        int keyguardStoredPasswordQuality = this.mLockPatternUtils.getKeyguardStoredPasswordQuality(UserHandle.myUserId());
        if (keyguardStoredPasswordQuality == 65536) {
            if (this.mLockPatternUtils.isLockPatternEnabled(UserHandle.myUserId())) {
                return R.string.screen_pinning_unlock_pattern;
            }
            return R.string.screen_pinning_unlock_none;
        }
        if (keyguardStoredPasswordQuality == 131072 || keyguardStoredPasswordQuality == 196608) {
            return R.string.screen_pinning_unlock_pin;
        }
        if (keyguardStoredPasswordQuality == 262144 || keyguardStoredPasswordQuality == 327680 || keyguardStoredPasswordQuality == 393216 || keyguardStoredPasswordQuality == 524288) {
            return R.string.screen_pinning_unlock_password;
        }
        return R.string.screen_pinning_unlock_none;
    }

    @Override
    public void onSwitchChanged(Switch r1, boolean z) {
        setLockToAppEnabled(z);
        updateDisplay();
    }

    public void updateDisplay() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen != null) {
            preferenceScreen.removeAll();
        }
        if (isLockToAppEnabled(getActivity())) {
            addPreferencesFromResource(R.xml.screen_pinning_settings);
            this.mUseScreenLock = (SwitchPreference) getPreferenceScreen().findPreference(KEY_USE_SCREEN_LOCK);
            this.mUseScreenLock.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object obj) {
                    return ScreenPinningSettings.this.setScreenLockUsed(((Boolean) obj).booleanValue());
                }
            });
            this.mUseScreenLock.setChecked(isScreenLockUsed());
            this.mUseScreenLock.setTitle(getCurrentSecurityTitle());
        }
    }
}
