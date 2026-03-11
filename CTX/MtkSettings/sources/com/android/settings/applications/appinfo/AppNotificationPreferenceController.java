package com.android.settings.applications.appinfo;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.notification.AppNotificationSettings;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.applications.ApplicationsState;

public class AppNotificationPreferenceController extends AppInfoPreferenceControllerBase {
    private final NotificationBackend mBackend;
    private String mChannelId;

    public AppNotificationPreferenceController(Context context, String str) {
        super(context, str);
        this.mChannelId = null;
        this.mBackend = new NotificationBackend();
    }

    @Override
    public void setParentFragment(AppInfoDashboardFragment appInfoDashboardFragment) {
        super.setParentFragment(appInfoDashboardFragment);
        if (appInfoDashboardFragment != null && appInfoDashboardFragment.getActivity() != null && appInfoDashboardFragment.getActivity().getIntent() != null) {
            this.mChannelId = appInfoDashboardFragment.getActivity().getIntent().getStringExtra(":settings:fragment_args_key");
        }
    }

    @Override
    public void updateState(Preference preference) {
        preference.setSummary(getNotificationSummary(this.mParent.getAppEntry(), this.mContext, this.mBackend));
    }

    @Override
    protected Class<? extends SettingsPreferenceFragment> getDetailFragmentClass() {
        return AppNotificationSettings.class;
    }

    @Override
    protected Bundle getArguments() {
        if (this.mChannelId != null) {
            Bundle bundle = new Bundle();
            bundle.putString(":settings:fragment_args_key", this.mChannelId);
            return bundle;
        }
        return null;
    }

    private CharSequence getNotificationSummary(ApplicationsState.AppEntry appEntry, Context context, NotificationBackend notificationBackend) {
        return getNotificationSummary(notificationBackend.loadAppRow(context, context.getPackageManager(), appEntry.info), context);
    }

    public static CharSequence getNotificationSummary(NotificationBackend.AppRow appRow, Context context) {
        if (appRow == null) {
            return "";
        }
        if (appRow.banned) {
            return context.getText(R.string.notifications_disabled);
        }
        if (appRow.channelCount == 0) {
            return context.getText(R.string.notifications_enabled);
        }
        if (appRow.channelCount == appRow.blockedChannelCount) {
            return context.getText(R.string.notifications_disabled);
        }
        if (appRow.blockedChannelCount == 0) {
            return context.getText(R.string.notifications_enabled);
        }
        return context.getString(R.string.notifications_enabled_with_info, context.getResources().getQuantityString(R.plurals.notifications_categories_off, appRow.blockedChannelCount, Integer.valueOf(appRow.blockedChannelCount)));
    }
}
