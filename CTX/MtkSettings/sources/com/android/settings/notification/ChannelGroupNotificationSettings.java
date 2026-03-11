package com.android.settings.notification;

import android.app.NotificationChannel;
import android.content.Context;
import android.support.v7.preference.Preference;
import android.util.Log;
import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ChannelGroupNotificationSettings extends NotificationSettingsBase {
    @Override
    public int getMetricsCategory() {
        return 1218;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.mAppRow == null || this.mChannelGroup == null) {
            Log.w("ChannelGroupSettings", "Missing package or uid or packageinfo or group");
            finish();
            return;
        }
        populateChannelList();
        for (NotificationPreferenceController notificationPreferenceController : this.mControllers) {
            notificationPreferenceController.onResume(this.mAppRow, this.mChannel, this.mChannelGroup, this.mSuspendedAppsAdmin);
            notificationPreferenceController.displayPreference(getPreferenceScreen());
        }
        updatePreferenceStates();
    }

    @Override
    protected String getLogTag() {
        return "ChannelGroupSettings";
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.notification_group_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        this.mControllers = new ArrayList();
        this.mControllers.add(new HeaderPreferenceController(context, this));
        this.mControllers.add(new BlockPreferenceController(context, this.mImportanceListener, this.mBackend));
        this.mControllers.add(new AppLinkPreferenceController(context));
        this.mControllers.add(new NotificationsOffPreferenceController(context));
        this.mControllers.add(new DescriptionPreferenceController(context));
        return new ArrayList(this.mControllers);
    }

    private void populateChannelList() {
        if (!this.mDynamicPreferences.isEmpty()) {
            Log.w("ChannelGroupSettings", "Notification channel group posted twice to settings - old size " + this.mDynamicPreferences.size() + ", new size " + this.mDynamicPreferences.size());
            Iterator<Preference> it = this.mDynamicPreferences.iterator();
            while (it.hasNext()) {
                getPreferenceScreen().removePreference(it.next());
            }
        }
        if (this.mChannelGroup.getChannels().isEmpty()) {
            Preference preference = new Preference(getPrefContext());
            preference.setTitle(R.string.no_channels);
            preference.setEnabled(false);
            getPreferenceScreen().addPreference(preference);
            this.mDynamicPreferences.add(preference);
            return;
        }
        List<NotificationChannel> channels = this.mChannelGroup.getChannels();
        Collections.sort(channels, this.mChannelComparator);
        Iterator<NotificationChannel> it2 = channels.iterator();
        while (it2.hasNext()) {
            this.mDynamicPreferences.add(populateSingleChannelPrefs(getPreferenceScreen(), it2.next(), this.mChannelGroup.isBlocked()));
        }
    }
}
