package com.android.settings.notification;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.widget.MasterCheckBoxPreference;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class AppNotificationSettings extends NotificationSettingsBase {
    private Comparator<NotificationChannelGroup> mChannelGroupComparator = new Comparator<NotificationChannelGroup>() {
        @Override
        public int compare(NotificationChannelGroup notificationChannelGroup, NotificationChannelGroup notificationChannelGroup2) {
            if (notificationChannelGroup.getId() == null && notificationChannelGroup2.getId() != null) {
                return 1;
            }
            if (notificationChannelGroup2.getId() == null && notificationChannelGroup.getId() != null) {
                return -1;
            }
            return notificationChannelGroup.getId().compareTo(notificationChannelGroup2.getId());
        }
    };
    private List<NotificationChannelGroup> mChannelGroupList;
    private static final boolean DEBUG = Log.isLoggable("AppNotificationSettings", 3);
    private static String KEY_GENERAL_CATEGORY = "categories";
    private static String KEY_ADVANCED_CATEGORY = "app_advanced";
    private static String KEY_BADGE = "badge";
    private static String KEY_APP_LINK = "app_link";

    @Override
    public int getMetricsCategory() {
        return 72;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (this.mShowLegacyChannelConfig && preferenceScreen != null) {
            Preference preferenceFindPreference = findPreference(KEY_BADGE);
            Preference preferenceFindPreference2 = findPreference(KEY_APP_LINK);
            removePreference(KEY_ADVANCED_CATEGORY);
            if (preferenceFindPreference != null) {
                preferenceScreen.addPreference(preferenceFindPreference);
            }
            if (preferenceFindPreference2 != null) {
                preferenceScreen.addPreference(preferenceFindPreference2);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().getWindow().addPrivateFlags(524288);
        EventLog.writeEvent(1397638484, "119115683", -1, "");
        if (this.mUid < 0 || TextUtils.isEmpty(this.mPkg) || this.mPkgInfo == null) {
            Log.w("AppNotificationSettings", "Missing package or uid or packageinfo");
            finish();
            return;
        }
        if (!this.mShowLegacyChannelConfig) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voidArr) {
                    AppNotificationSettings.this.mChannelGroupList = AppNotificationSettings.this.mBackend.getGroups(AppNotificationSettings.this.mPkg, AppNotificationSettings.this.mUid).getList();
                    Collections.sort(AppNotificationSettings.this.mChannelGroupList, AppNotificationSettings.this.mChannelGroupComparator);
                    return null;
                }

                @Override
                protected void onPostExecute(Void r1) {
                    if (AppNotificationSettings.this.getHost() != null) {
                        AppNotificationSettings.this.populateList();
                    }
                }
            }.execute(new Void[0]);
        }
        for (NotificationPreferenceController notificationPreferenceController : this.mControllers) {
            notificationPreferenceController.onResume(this.mAppRow, this.mChannel, this.mChannelGroup, this.mSuspendedAppsAdmin);
            notificationPreferenceController.displayPreference(getPreferenceScreen());
        }
        updatePreferenceStates();
    }

    @Override
    public void onPause() {
        super.onPause();
        Window window = getActivity().getWindow();
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.privateFlags &= -524289;
        window.setAttributes(attributes);
    }

    @Override
    protected String getLogTag() {
        return "AppNotificationSettings";
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.app_notification_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        this.mControllers = new ArrayList();
        this.mControllers.add(new HeaderPreferenceController(context, this));
        this.mControllers.add(new BlockPreferenceController(context, this.mImportanceListener, this.mBackend));
        this.mControllers.add(new BadgePreferenceController(context, this.mBackend));
        this.mControllers.add(new AllowSoundPreferenceController(context, this.mImportanceListener, this.mBackend));
        this.mControllers.add(new ImportancePreferenceController(context, this.mImportanceListener, this.mBackend));
        this.mControllers.add(new SoundPreferenceController(context, this, this.mImportanceListener, this.mBackend));
        this.mControllers.add(new LightsPreferenceController(context, this.mBackend));
        this.mControllers.add(new VibrationPreferenceController(context, this.mBackend));
        this.mControllers.add(new VisibilityPreferenceController(context, new LockPatternUtils(context), this.mBackend));
        this.mControllers.add(new DndPreferenceController(context, this.mBackend));
        this.mControllers.add(new AppLinkPreferenceController(context));
        this.mControllers.add(new DescriptionPreferenceController(context));
        this.mControllers.add(new NotificationsOffPreferenceController(context));
        this.mControllers.add(new DeletedChannelsPreferenceController(context, this.mBackend));
        return new ArrayList(this.mControllers);
    }

    private void populateList() {
        if (!this.mDynamicPreferences.isEmpty()) {
            Iterator<Preference> it = this.mDynamicPreferences.iterator();
            while (it.hasNext()) {
                getPreferenceScreen().removePreference(it.next());
            }
            this.mDynamicPreferences.clear();
        }
        if (this.mChannelGroupList.isEmpty()) {
            PreferenceCategory preferenceCategory = new PreferenceCategory(getPrefContext());
            preferenceCategory.setTitle(R.string.notification_channels);
            preferenceCategory.setKey(KEY_GENERAL_CATEGORY);
            getPreferenceScreen().addPreference(preferenceCategory);
            this.mDynamicPreferences.add(preferenceCategory);
            Preference preference = new Preference(getPrefContext());
            preference.setTitle(R.string.no_channels);
            preference.setEnabled(false);
            preferenceCategory.addPreference(preference);
            return;
        }
        populateGroupList();
        this.mImportanceListener.onImportanceChanged();
    }

    private void populateGroupList() {
        for (NotificationChannelGroup notificationChannelGroup : this.mChannelGroupList) {
            PreferenceCategory preferenceCategory = new PreferenceCategory(getPrefContext());
            preferenceCategory.setOrderingAsAdded(true);
            getPreferenceScreen().addPreference(preferenceCategory);
            this.mDynamicPreferences.add(preferenceCategory);
            if (notificationChannelGroup.getId() == null) {
                if (this.mChannelGroupList.size() > 1) {
                    preferenceCategory.setTitle(R.string.notification_channels_other);
                }
                preferenceCategory.setKey(KEY_GENERAL_CATEGORY);
            } else {
                preferenceCategory.setTitle(notificationChannelGroup.getName());
                preferenceCategory.setKey(notificationChannelGroup.getId());
                populateGroupToggle(preferenceCategory, notificationChannelGroup);
            }
            if (!notificationChannelGroup.isBlocked()) {
                List<NotificationChannel> channels = notificationChannelGroup.getChannels();
                Collections.sort(channels, this.mChannelComparator);
                int size = channels.size();
                for (int i = 0; i < size; i++) {
                    populateSingleChannelPrefs(preferenceCategory, channels.get(i), notificationChannelGroup.isBlocked());
                }
            }
        }
    }

    protected void populateGroupToggle(PreferenceGroup preferenceGroup, final NotificationChannelGroup notificationChannelGroup) {
        boolean z;
        RestrictedSwitchPreference restrictedSwitchPreference = new RestrictedSwitchPreference(getPrefContext());
        restrictedSwitchPreference.setTitle(R.string.notification_switch_label);
        if (this.mSuspendedAppsAdmin == null && isChannelGroupBlockable(notificationChannelGroup)) {
            z = true;
        } else {
            z = false;
        }
        restrictedSwitchPreference.setEnabled(z);
        restrictedSwitchPreference.setChecked(!notificationChannelGroup.isBlocked());
        restrictedSwitchPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public final boolean onPreferenceClick(Preference preference) {
                return AppNotificationSettings.lambda$populateGroupToggle$0(this.f$0, notificationChannelGroup, preference);
            }
        });
        preferenceGroup.addPreference(restrictedSwitchPreference);
    }

    public static boolean lambda$populateGroupToggle$0(AppNotificationSettings appNotificationSettings, NotificationChannelGroup notificationChannelGroup, Preference preference) {
        notificationChannelGroup.setBlocked(!((SwitchPreference) preference).isChecked());
        appNotificationSettings.mBackend.updateChannelGroup(appNotificationSettings.mAppRow.pkg, appNotificationSettings.mAppRow.uid, notificationChannelGroup);
        appNotificationSettings.onGroupBlockStateChanged(notificationChannelGroup);
        return true;
    }

    protected void onGroupBlockStateChanged(NotificationChannelGroup notificationChannelGroup) {
        PreferenceGroup preferenceGroup;
        if (notificationChannelGroup != null && (preferenceGroup = (PreferenceGroup) getPreferenceScreen().findPreference(notificationChannelGroup.getId())) != null) {
            int i = 0;
            if (notificationChannelGroup.isBlocked()) {
                ArrayList arrayList = new ArrayList();
                int preferenceCount = preferenceGroup.getPreferenceCount();
                while (i < preferenceCount) {
                    Preference preference = preferenceGroup.getPreference(i);
                    if (preference instanceof MasterCheckBoxPreference) {
                        arrayList.add(preference);
                    }
                    i++;
                }
                Iterator it = arrayList.iterator();
                while (it.hasNext()) {
                    preferenceGroup.removePreference((Preference) it.next());
                }
                return;
            }
            List<NotificationChannel> channels = notificationChannelGroup.getChannels();
            Collections.sort(channels, this.mChannelComparator);
            int size = channels.size();
            while (i < size) {
                populateSingleChannelPrefs(preferenceGroup, channels.get(i), notificationChannelGroup.isBlocked());
                i++;
            }
        }
    }
}
