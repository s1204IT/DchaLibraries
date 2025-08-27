package com.android.systemui.tuner;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.view.MenuItem;
import com.android.settingslib.drawer.SettingsDrawerActivity;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.fragments.FragmentService;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class TunerActivity extends SettingsDrawerActivity implements PreferenceFragment.OnPreferenceStartFragmentCallback, PreferenceFragment.OnPreferenceStartScreenCallback {
    @Override // com.android.settingslib.drawer.SettingsDrawerActivity, android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Dependency.initDependencies(this);
        if (getFragmentManager().findFragmentByTag("tuner") == null) {
            String action = getIntent().getAction();
            getFragmentManager().beginTransaction().replace(R.id.content_frame, action != null && action.equals("com.android.settings.action.DEMO_MODE") ? new DemoModeFragment() : new TunerFragment(), "tuner").commit();
        }
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        super.onDestroy();
        Dependency.destroy(FragmentService.class, new Consumer() { // from class: com.android.systemui.tuner.-$$Lambda$TunerActivity$RI23eCWQLUIRemsdYo0hJRYd5ug
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((FragmentService) obj).destroyAll();
            }
        });
        Dependency.clearDependencies();
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public boolean onMenuItemSelected(int i, MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            onBackPressed();
            return true;
        }
        return super.onMenuItemSelected(i, menuItem);
    }

    @Override // android.app.Activity
    public void onBackPressed() {
        if (!getFragmentManager().popBackStackImmediate()) {
            super.onBackPressed();
        }
    }

    @Override // android.support.v14.preference.PreferenceFragment.OnPreferenceStartFragmentCallback
    public boolean onPreferenceStartFragment(PreferenceFragment preferenceFragment, Preference preference) {
        try {
            Fragment fragment = (Fragment) Class.forName(preference.getFragment()).newInstance();
            Bundle bundle = new Bundle(1);
            bundle.putString("android.support.v7.preference.PreferenceFragmentCompat.PREFERENCE_ROOT", preference.getKey());
            fragment.setArguments(bundle);
            FragmentTransaction fragmentTransactionBeginTransaction = getFragmentManager().beginTransaction();
            setTitle(preference.getTitle());
            fragmentTransactionBeginTransaction.replace(R.id.content_frame, fragment);
            fragmentTransactionBeginTransaction.addToBackStack("PreferenceFragment");
            fragmentTransactionBeginTransaction.commit();
            return true;
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            Log.d("TunerActivity", "Problem launching fragment", e);
            return false;
        }
    }

    @Override // android.support.v14.preference.PreferenceFragment.OnPreferenceStartScreenCallback
    public boolean onPreferenceStartScreen(PreferenceFragment preferenceFragment, PreferenceScreen preferenceScreen) {
        FragmentTransaction fragmentTransactionBeginTransaction = getFragmentManager().beginTransaction();
        SubSettingsFragment subSettingsFragment = new SubSettingsFragment();
        Bundle bundle = new Bundle(1);
        bundle.putString("android.support.v7.preference.PreferenceFragmentCompat.PREFERENCE_ROOT", preferenceScreen.getKey());
        subSettingsFragment.setArguments(bundle);
        subSettingsFragment.setTargetFragment(preferenceFragment, 0);
        fragmentTransactionBeginTransaction.replace(R.id.content_frame, subSettingsFragment);
        fragmentTransactionBeginTransaction.addToBackStack("PreferenceFragment");
        fragmentTransactionBeginTransaction.commit();
        return true;
    }

    public static class SubSettingsFragment extends PreferenceFragment {
        private PreferenceScreen mParentScreen;

        @Override // android.support.v14.preference.PreferenceFragment
        public void onCreatePreferences(Bundle bundle, String str) {
            this.mParentScreen = (PreferenceScreen) ((PreferenceFragment) getTargetFragment()).getPreferenceScreen().findPreference(str);
            PreferenceScreen preferenceScreenCreatePreferenceScreen = getPreferenceManager().createPreferenceScreen(getPreferenceManager().getContext());
            setPreferenceScreen(preferenceScreenCreatePreferenceScreen);
            while (this.mParentScreen.getPreferenceCount() > 0) {
                Preference preference = this.mParentScreen.getPreference(0);
                this.mParentScreen.removePreference(preference);
                preferenceScreenCreatePreferenceScreen.addPreference(preference);
            }
        }

        @Override // android.app.Fragment
        public void onDestroy() {
            super.onDestroy();
            PreferenceScreen preferenceScreen = getPreferenceScreen();
            while (preferenceScreen.getPreferenceCount() > 0) {
                Preference preference = preferenceScreen.getPreference(0);
                preferenceScreen.removePreference(preference);
                this.mParentScreen.addPreference(preference);
            }
        }
    }
}
