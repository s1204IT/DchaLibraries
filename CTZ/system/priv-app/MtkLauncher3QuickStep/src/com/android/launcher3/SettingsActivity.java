package com.android.launcher3;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.android.launcher3.graphics.IconShapeOverride;
import com.android.launcher3.notification.NotificationListener;
import com.android.launcher3.states.RotationHelper;
import com.android.launcher3.util.ListViewHighlighter;
import com.android.launcher3.util.SettingsObserver;
import com.android.launcher3.views.ButtonPreference;
import java.util.Objects;

/* loaded from: classes.dex */
public class SettingsActivity extends Activity {
    private static final int DELAY_HIGHLIGHT_DURATION_MILLIS = 600;
    private static final String EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key";
    private static final String EXTRA_SHOW_FRAGMENT_ARGS = ":settings:show_fragment_args";
    private static final String ICON_BADGING_PREFERENCE_KEY = "pref_icon_badging";
    public static final String NOTIFICATION_BADGING = "notification_badging";
    private static final String NOTIFICATION_ENABLED_LISTENERS = "enabled_notification_listeners";
    private static final String SAVE_HIGHLIGHTED_KEY = "android:preference_highlighted";

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (bundle == null) {
            getFragmentManager().beginTransaction().replace(android.R.id.content, getNewFragment()).commit();
        }
    }

    protected PreferenceFragment getNewFragment() {
        return new LauncherSettingsFragment();
    }

    public static class LauncherSettingsFragment extends PreferenceFragment {
        private IconBadgingObserver mIconBadgingObserver;
        private boolean mPreferenceHighlighted = false;
        private String mPreferenceKey;

        @Override // android.preference.PreferenceFragment, android.app.Fragment
        public void onCreate(Bundle bundle) {
            super.onCreate(bundle);
            if (bundle != null) {
                this.mPreferenceHighlighted = bundle.getBoolean(SettingsActivity.SAVE_HIGHLIGHTED_KEY);
            }
            getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
            addPreferencesFromResource(R.xml.launcher_preferences);
            ContentResolver contentResolver = getActivity().getContentResolver();
            ButtonPreference buttonPreference = (ButtonPreference) findPreference(SettingsActivity.ICON_BADGING_PREFERENCE_KEY);
            if (!Utilities.ATLEAST_OREO) {
                getPreferenceScreen().removePreference(findPreference(SessionCommitReceiver.ADD_ICON_PREFERENCE_KEY));
                getPreferenceScreen().removePreference(buttonPreference);
            } else if (!getResources().getBoolean(R.bool.notification_badging_enabled)) {
                getPreferenceScreen().removePreference(buttonPreference);
            } else {
                this.mIconBadgingObserver = new IconBadgingObserver(buttonPreference, contentResolver, getFragmentManager());
                this.mIconBadgingObserver.register(SettingsActivity.NOTIFICATION_BADGING, SettingsActivity.NOTIFICATION_ENABLED_LISTENERS);
            }
            Preference preferenceFindPreference = findPreference(IconShapeOverride.KEY_PREFERENCE);
            if (preferenceFindPreference != null) {
                if (IconShapeOverride.isSupported(getActivity())) {
                    IconShapeOverride.handlePreferenceUi((ListPreference) preferenceFindPreference);
                } else {
                    getPreferenceScreen().removePreference(preferenceFindPreference);
                }
            }
            Preference preferenceFindPreference2 = findPreference(RotationHelper.ALLOW_ROTATION_PREFERENCE_KEY);
            if (getResources().getBoolean(R.bool.allow_rotation)) {
                getPreferenceScreen().removePreference(preferenceFindPreference2);
            } else {
                preferenceFindPreference2.setDefaultValue(Boolean.valueOf(RotationHelper.getAllowRotationDefaultValue()));
            }
        }

        @Override // android.preference.PreferenceFragment, android.app.Fragment
        public void onSaveInstanceState(Bundle bundle) {
            super.onSaveInstanceState(bundle);
            bundle.putBoolean(SettingsActivity.SAVE_HIGHLIGHTED_KEY, this.mPreferenceHighlighted);
        }

        @Override // android.app.Fragment
        public void onResume() {
            super.onResume();
            this.mPreferenceKey = getActivity().getIntent().getStringExtra(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY);
            if (isAdded() && !this.mPreferenceHighlighted && !TextUtils.isEmpty(this.mPreferenceKey)) {
                getView().postDelayed(new Runnable() { // from class: com.android.launcher3.-$$Lambda$SettingsActivity$LauncherSettingsFragment$ZPeeMXt8knkkS8xr0AY99mJgiqM
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.highlightPreference();
                    }
                }, 600L);
            }
        }

        private void highlightPreference() {
            Preference preferenceFindPreference = findPreference(this.mPreferenceKey);
            if (preferenceFindPreference == null || getPreferenceScreen() == null) {
                return;
            }
            PreferenceScreen preferenceScreen = getPreferenceScreen();
            if (Utilities.ATLEAST_OREO) {
                preferenceScreen = selectPreferenceRecursive(preferenceFindPreference, preferenceScreen);
            }
            if (preferenceScreen == null) {
                return;
            }
            ListView listView = (ListView) (preferenceScreen.getDialog() != null ? preferenceScreen.getDialog().getWindow().getDecorView() : getView()).findViewById(android.R.id.list);
            if (listView == null || listView.getAdapter() == null) {
                return;
            }
            ListAdapter adapter = listView.getAdapter();
            int count = adapter.getCount() - 1;
            while (true) {
                if (count >= 0) {
                    if (preferenceFindPreference == adapter.getItem(count)) {
                        break;
                    } else {
                        count--;
                    }
                } else {
                    count = -1;
                    break;
                }
            }
            new ListViewHighlighter(listView, count);
            this.mPreferenceHighlighted = true;
        }

        @Override // android.preference.PreferenceFragment, android.app.Fragment
        public void onDestroy() {
            if (this.mIconBadgingObserver != null) {
                this.mIconBadgingObserver.unregister();
                this.mIconBadgingObserver = null;
            }
            super.onDestroy();
        }

        @TargetApi(26)
        private PreferenceScreen selectPreferenceRecursive(Preference preference, PreferenceScreen preferenceScreen) {
            if (!(preference.getParent() instanceof PreferenceScreen)) {
                return null;
            }
            PreferenceScreen preferenceScreen2 = (PreferenceScreen) preference.getParent();
            if (Objects.equals(preferenceScreen2.getKey(), preferenceScreen.getKey())) {
                return preferenceScreen2;
            }
            if (selectPreferenceRecursive(preferenceScreen2, preferenceScreen) == null) {
                return null;
            }
            ((PreferenceScreen) preferenceScreen2.getParent()).onItemClick(null, null, preferenceScreen2.getOrder(), 0L);
            return preferenceScreen2;
        }
    }

    private static class IconBadgingObserver extends SettingsObserver.Secure implements Preference.OnPreferenceClickListener {
        private final ButtonPreference mBadgingPref;
        private final FragmentManager mFragmentManager;
        private final ContentResolver mResolver;

        public IconBadgingObserver(ButtonPreference buttonPreference, ContentResolver contentResolver, FragmentManager fragmentManager) {
            super(contentResolver);
            this.mBadgingPref = buttonPreference;
            this.mResolver = contentResolver;
            this.mFragmentManager = fragmentManager;
        }

        @Override // com.android.launcher3.util.SettingsObserver
        public void onSettingChanged(boolean z) {
            int i = z ? R.string.icon_badging_desc_on : R.string.icon_badging_desc_off;
            boolean z2 = true;
            if (z) {
                String string = Settings.Secure.getString(this.mResolver, SettingsActivity.NOTIFICATION_ENABLED_LISTENERS);
                ComponentName componentName = new ComponentName(this.mBadgingPref.getContext(), (Class<?>) NotificationListener.class);
                if (string == null || (!string.contains(componentName.flattenToString()) && !string.contains(componentName.flattenToShortString()))) {
                    z2 = false;
                }
                if (!z2) {
                    i = R.string.title_missing_notification_access;
                }
            }
            this.mBadgingPref.setWidgetFrameVisible(!z2);
            this.mBadgingPref.setOnPreferenceClickListener(z2 ? null : this);
            this.mBadgingPref.setSummary(i);
        }

        @Override // android.preference.Preference.OnPreferenceClickListener
        public boolean onPreferenceClick(Preference preference) {
            new NotificationAccessConfirmation().show(this.mFragmentManager, "notification_access");
            return true;
        }
    }

    public static class NotificationAccessConfirmation extends DialogFragment implements DialogInterface.OnClickListener {
        @Override // android.app.DialogFragment
        public Dialog onCreateDialog(Bundle bundle) {
            Activity activity = getActivity();
            return new AlertDialog.Builder(activity).setTitle(R.string.title_missing_notification_access).setMessage(activity.getString(R.string.msg_missing_notification_access, activity.getString(R.string.derived_app_name))).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.title_change_settings, this).create();
        }

        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialogInterface, int i) {
            if (BenesseExtension.getDchaState() != 0) {
                return;
            }
            ComponentName componentName = new ComponentName(getActivity(), (Class<?>) NotificationListener.class);
            Bundle bundle = new Bundle();
            bundle.putString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY, componentName.flattenToString());
            getActivity().startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").addFlags(268435456).putExtra(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY, componentName.flattenToString()).putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGS, bundle));
        }
    }
}
