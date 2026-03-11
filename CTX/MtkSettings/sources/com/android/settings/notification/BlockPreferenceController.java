package com.android.settings.notification;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.widget.Switch;
import com.android.settings.R;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.notification.NotificationSettingsBase;
import com.android.settings.widget.SwitchBar;

public class BlockPreferenceController extends NotificationPreferenceController implements PreferenceControllerMixin, SwitchBar.OnSwitchChangeListener {
    private NotificationSettingsBase.ImportanceListener mImportanceListener;

    public BlockPreferenceController(Context context, NotificationSettingsBase.ImportanceListener importanceListener, NotificationBackend notificationBackend) {
        super(context, notificationBackend);
        this.mImportanceListener = importanceListener;
    }

    @Override
    public String getPreferenceKey() {
        return "block";
    }

    @Override
    public boolean isAvailable() {
        if (this.mAppRow == null) {
            return false;
        }
        if (this.mChannel != null) {
            return isChannelBlockable();
        }
        if (this.mChannelGroup != null) {
            return isChannelGroupBlockable();
        }
        return !this.mAppRow.systemApp || (this.mAppRow.systemApp && this.mAppRow.banned);
    }

    @Override
    public void updateState(Preference preference) {
        SwitchBar switchBar = (SwitchBar) ((LayoutPreference) preference).findViewById(R.id.switch_bar);
        if (switchBar != null) {
            switchBar.setSwitchBarText(R.string.notification_switch_label, R.string.notification_switch_label);
            switchBar.show();
            try {
                switchBar.addOnSwitchChangeListener(this);
            } catch (IllegalStateException e) {
            }
            switchBar.setDisabledByAdmin(this.mAdmin);
            boolean z = false;
            if (this.mChannel != null) {
                if (!this.mAppRow.banned && this.mChannel.getImportance() != 0) {
                    z = true;
                }
                switchBar.setChecked(z);
                return;
            }
            if (this.mChannelGroup != null) {
                if (!this.mAppRow.banned && !this.mChannelGroup.isBlocked()) {
                    z = true;
                }
                switchBar.setChecked(z);
                return;
            }
            switchBar.setChecked(!this.mAppRow.banned);
        }
    }

    @Override
    public void onSwitchChanged(Switch r5, boolean z) {
        int i;
        boolean z2 = !z;
        if (this.mChannel != null) {
            int importance = this.mChannel.getImportance();
            if (z2 || importance == 0) {
                if (!z2) {
                    i = isDefaultChannel() ? -1000 : 3;
                } else {
                    i = 0;
                }
                this.mChannel.setImportance(i);
                saveChannel();
            }
            if (this.mBackend.onlyHasDefaultChannel(this.mAppRow.pkg, this.mAppRow.uid) && this.mAppRow.banned != z2) {
                this.mAppRow.banned = z2;
                this.mBackend.setNotificationsEnabledForPackage(this.mAppRow.pkg, this.mAppRow.uid, z2 ? false : true);
            }
        } else if (this.mChannelGroup != null) {
            this.mChannelGroup.setBlocked(z2);
            this.mBackend.updateChannelGroup(this.mAppRow.pkg, this.mAppRow.uid, this.mChannelGroup);
        } else if (this.mAppRow != null) {
            this.mAppRow.banned = z2;
            this.mBackend.setNotificationsEnabledForPackage(this.mAppRow.pkg, this.mAppRow.uid, z2 ? false : true);
        }
        this.mImportanceListener.onImportanceChanged();
    }
}
