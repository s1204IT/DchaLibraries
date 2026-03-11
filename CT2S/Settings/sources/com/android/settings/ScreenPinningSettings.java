package com.android.settings;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.widget.SwitchBar;
import java.util.ArrayList;
import java.util.List;

public class ScreenPinningSettings extends SettingsPreferenceFragment implements Indexable, SwitchBar.OnSwitchChangeListener {
    private static final CharSequence KEY_USE_SCREEN_LOCK = "use_screen_lock";
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> result = new ArrayList<>();
            Resources res = context.getResources();
            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = res.getString(R.string.screen_pinning_title);
            data.screenTitle = res.getString(R.string.screen_pinning_title);
            result.add(data);
            if (ScreenPinningSettings.isLockToAppEnabled(context)) {
                SearchIndexableRaw data2 = new SearchIndexableRaw(context);
                data2.title = res.getString(R.string.screen_pinning_unlock_none);
                data2.screenTitle = res.getString(R.string.screen_pinning_title);
                result.add(data2);
            } else {
                SearchIndexableRaw data3 = new SearchIndexableRaw(context);
                data3.title = res.getString(R.string.screen_pinning_description);
                data3.screenTitle = res.getString(R.string.screen_pinning_title);
                result.add(data3);
            }
            return result;
        }
    };
    private LockPatternUtils mLockPatternUtils;
    private SwitchBar mSwitchBar;
    private SwitchPreference mUseScreenLock;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        SettingsActivity activity = (SettingsActivity) getActivity();
        this.mLockPatternUtils = new LockPatternUtils(activity);
        View emptyView = LayoutInflater.from(activity).inflate(R.layout.screen_pinning_instructions, (ViewGroup) null);
        ((ViewGroup) getListView().getParent()).addView(emptyView);
        getListView().setEmptyView(emptyView);
        this.mSwitchBar = activity.getSwitchBar();
        this.mSwitchBar.addOnSwitchChangeListener(this);
        this.mSwitchBar.show();
        this.mSwitchBar.setChecked(isLockToAppEnabled(getActivity()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.mSwitchBar.removeOnSwitchChangeListener(this);
        this.mSwitchBar.hide();
    }

    public static boolean isLockToAppEnabled(Context context) {
        return Settings.System.getInt(context.getContentResolver(), "lock_to_app_enabled", 0) != 0;
    }

    private void setLockToAppEnabled(boolean isEnabled) {
        Settings.System.putInt(getContentResolver(), "lock_to_app_enabled", isEnabled ? 1 : 0);
    }

    private boolean isScreenLockUsed() {
        return Settings.Secure.getInt(getContentResolver(), "lock_to_app_exit_locked", 0) != 0;
    }

    public boolean setScreenLockUsed(boolean isEnabled) {
        if (isEnabled) {
            LockPatternUtils lockPatternUtils = new LockPatternUtils(getActivity());
            if (lockPatternUtils.getKeyguardStoredPasswordQuality() == 0) {
                Intent chooseLockIntent = new Intent("android.app.action.SET_NEW_PASSWORD");
                chooseLockIntent.putExtra("minimum_quality", 65536);
                startActivityForResult(chooseLockIntent, 43);
                return false;
            }
        }
        Settings.Secure.putInt(getContentResolver(), "lock_to_app_exit_locked", isEnabled ? 1 : 0);
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 43) {
            LockPatternUtils lockPatternUtils = new LockPatternUtils(getActivity());
            boolean validPassQuality = lockPatternUtils.getKeyguardStoredPasswordQuality() != 0;
            setScreenLockUsed(validPassQuality);
            this.mUseScreenLock.setChecked(validPassQuality);
        }
    }

    private int getCurrentSecurityTitle() {
        int quality = this.mLockPatternUtils.getKeyguardStoredPasswordQuality();
        switch (quality) {
            case 65536:
                if (this.mLockPatternUtils.isLockPatternEnabled()) {
                    return R.string.screen_pinning_unlock_pattern;
                }
                return R.string.screen_pinning_unlock_none;
            case 131072:
            case 196608:
                return R.string.screen_pinning_unlock_pin;
            case 262144:
            case 327680:
            case 393216:
                return R.string.screen_pinning_unlock_password;
        }
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        setLockToAppEnabled(isChecked);
        updateDisplay();
    }

    public void updateDisplay() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        if (isLockToAppEnabled(getActivity())) {
            addPreferencesFromResource(R.xml.screen_pinning_settings);
            this.mUseScreenLock = (SwitchPreference) getPreferenceScreen().findPreference(KEY_USE_SCREEN_LOCK);
            this.mUseScreenLock.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    return ScreenPinningSettings.this.setScreenLockUsed(((Boolean) newValue).booleanValue());
                }
            });
            this.mUseScreenLock.setChecked(isScreenLockUsed());
            this.mUseScreenLock.setTitle(getCurrentSecurityTitle());
        }
    }
}
