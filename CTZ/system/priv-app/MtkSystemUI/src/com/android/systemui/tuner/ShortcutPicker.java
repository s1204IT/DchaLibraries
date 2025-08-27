package com.android.systemui.tuner;

import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Process;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.tuner.ShortcutParser;
import com.android.systemui.tuner.TunerService;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class ShortcutPicker extends PreferenceFragment implements TunerService.Tunable {
    private String mKey;
    private SelectablePreference mNonePreference;
    private final ArrayList<SelectablePreference> mSelectablePreferences = new ArrayList<>();
    private TunerService mTunerService;

    @Override // android.support.v14.preference.PreferenceFragment
    public void onCreatePreferences(Bundle bundle, String str) {
        final Context context = getPreferenceManager().getContext();
        final PreferenceScreen preferenceScreenCreatePreferenceScreen = getPreferenceManager().createPreferenceScreen(context);
        preferenceScreenCreatePreferenceScreen.setOrderingAsAdded(true);
        final PreferenceCategory preferenceCategory = new PreferenceCategory(context);
        preferenceCategory.setTitle(R.string.tuner_other_apps);
        this.mNonePreference = new SelectablePreference(context);
        this.mSelectablePreferences.add(this.mNonePreference);
        this.mNonePreference.setTitle(R.string.lockscreen_none);
        this.mNonePreference.setIcon(R.drawable.ic_remove_circle);
        preferenceScreenCreatePreferenceScreen.addPreference(this.mNonePreference);
        List<LauncherActivityInfo> activityList = ((LauncherApps) getContext().getSystemService(LauncherApps.class)).getActivityList(null, Process.myUserHandle());
        preferenceScreenCreatePreferenceScreen.addPreference(preferenceCategory);
        activityList.forEach(new Consumer() { // from class: com.android.systemui.tuner.-$$Lambda$ShortcutPicker$kjubRY0RERFi5q4FUGuCDMvPtEc
            @Override // java.util.function.Consumer
            public final void accept(Object obj) throws Resources.NotFoundException {
                ShortcutPicker.lambda$onCreatePreferences$1(this.f$0, context, preferenceScreenCreatePreferenceScreen, preferenceCategory, (LauncherActivityInfo) obj);
            }
        });
        preferenceScreenCreatePreferenceScreen.removePreference(preferenceCategory);
        for (int i = 0; i < preferenceCategory.getPreferenceCount(); i++) {
            Preference preference = preferenceCategory.getPreference(0);
            preferenceCategory.removePreference(preference);
            preference.setOrder(Integer.MAX_VALUE);
            preferenceScreenCreatePreferenceScreen.addPreference(preference);
        }
        setPreferenceScreen(preferenceScreenCreatePreferenceScreen);
        this.mKey = getArguments().getString("android.support.v7.preference.PreferenceFragmentCompat.PREFERENCE_ROOT");
        this.mTunerService = (TunerService) Dependency.get(TunerService.class);
        this.mTunerService.addTunable(this, this.mKey);
    }

    public static /* synthetic */ void lambda$onCreatePreferences$1(final ShortcutPicker shortcutPicker, final Context context, final PreferenceScreen preferenceScreen, PreferenceCategory preferenceCategory, final LauncherActivityInfo launcherActivityInfo) throws Resources.NotFoundException {
        try {
            List<ShortcutParser.Shortcut> shortcuts = new ShortcutParser(shortcutPicker.getContext(), launcherActivityInfo.getComponentName()).getShortcuts();
            AppPreference appPreference = new AppPreference(context, launcherActivityInfo);
            shortcutPicker.mSelectablePreferences.add(appPreference);
            if (shortcuts.size() != 0) {
                preferenceScreen.addPreference(appPreference);
                shortcuts.forEach(new Consumer() { // from class: com.android.systemui.tuner.-$$Lambda$ShortcutPicker$KhrCRM8tSZs7Fj3ZW16pUQ2_D54
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ShortcutPicker.lambda$onCreatePreferences$0(this.f$0, context, launcherActivityInfo, preferenceScreen, (ShortcutParser.Shortcut) obj);
                    }
                });
            } else {
                preferenceCategory.addPreference(appPreference);
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
    }

    public static /* synthetic */ void lambda$onCreatePreferences$0(ShortcutPicker shortcutPicker, Context context, LauncherActivityInfo launcherActivityInfo, PreferenceScreen preferenceScreen, ShortcutParser.Shortcut shortcut) {
        ShortcutPreference shortcutPreference = new ShortcutPreference(context, shortcut, launcherActivityInfo.getLabel());
        shortcutPicker.mSelectablePreferences.add(shortcutPreference);
        preferenceScreen.addPreference(shortcutPreference);
    }

    @Override // android.support.v14.preference.PreferenceFragment, android.support.v7.preference.PreferenceManager.OnPreferenceTreeClickListener
    public boolean onPreferenceTreeClick(Preference preference) {
        this.mTunerService.setValue(this.mKey, preference.toString());
        getActivity().onBackPressed();
        return true;
    }

    @Override // android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        if ("sysui_keyguard_left".equals(this.mKey)) {
            getActivity().setTitle(R.string.lockscreen_shortcut_left);
        } else {
            getActivity().setTitle(R.string.lockscreen_shortcut_right);
        }
    }

    @Override // android.app.Fragment
    public void onDestroy() {
        super.onDestroy();
        this.mTunerService.removeTunable(this);
    }

    @Override // com.android.systemui.tuner.TunerService.Tunable
    public void onTuningChanged(String str, final String str2) {
        if (str2 == null) {
            str2 = "";
        }
        this.mSelectablePreferences.forEach(new Consumer() { // from class: com.android.systemui.tuner.-$$Lambda$ShortcutPicker$i1fIZ726bN-ySXwulncRN12T1Qg
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                SelectablePreference selectablePreference = (SelectablePreference) obj;
                selectablePreference.setChecked(str2.equals(selectablePreference.toString()));
            }
        });
    }

    private static class AppPreference extends SelectablePreference {
        private boolean mBinding;
        private final LauncherActivityInfo mInfo;

        public AppPreference(Context context, LauncherActivityInfo launcherActivityInfo) {
            super(context);
            this.mInfo = launcherActivityInfo;
            setTitle(context.getString(R.string.tuner_launch_app, launcherActivityInfo.getLabel()));
            setSummary(context.getString(R.string.tuner_app, launcherActivityInfo.getLabel()));
        }

        @Override // android.support.v7.preference.CheckBoxPreference, android.support.v7.preference.Preference
        public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
            this.mBinding = true;
            if (getIcon() == null) {
                setIcon(this.mInfo.getBadgedIcon(getContext().getResources().getConfiguration().densityDpi));
            }
            this.mBinding = false;
            super.onBindViewHolder(preferenceViewHolder);
        }

        @Override // android.support.v7.preference.Preference
        protected void notifyChanged() {
            if (this.mBinding) {
                return;
            }
            super.notifyChanged();
        }

        @Override // com.android.systemui.tuner.SelectablePreference, android.support.v7.preference.Preference
        public String toString() {
            return this.mInfo.getComponentName().flattenToString();
        }
    }

    private static class ShortcutPreference extends SelectablePreference {
        private boolean mBinding;
        private final ShortcutParser.Shortcut mShortcut;

        public ShortcutPreference(Context context, ShortcutParser.Shortcut shortcut, CharSequence charSequence) {
            super(context);
            this.mShortcut = shortcut;
            setTitle(shortcut.label);
            setSummary(context.getString(R.string.tuner_app, charSequence));
        }

        @Override // android.support.v7.preference.CheckBoxPreference, android.support.v7.preference.Preference
        public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
            this.mBinding = true;
            if (getIcon() == null) {
                setIcon(this.mShortcut.icon.loadDrawable(getContext()));
            }
            this.mBinding = false;
            super.onBindViewHolder(preferenceViewHolder);
        }

        @Override // android.support.v7.preference.Preference
        protected void notifyChanged() {
            if (this.mBinding) {
                return;
            }
            super.notifyChanged();
        }

        @Override // com.android.systemui.tuner.SelectablePreference, android.support.v7.preference.Preference
        public String toString() {
            return this.mShortcut.toString();
        }
    }
}
