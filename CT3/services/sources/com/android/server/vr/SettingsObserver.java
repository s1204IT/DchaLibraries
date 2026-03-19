package com.android.server.vr;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.ArraySet;
import java.util.Objects;
import java.util.Set;

public class SettingsObserver {
    private final ContentObserver mContentObserver;
    private final String mSecureSettingName;
    private final BroadcastReceiver mSettingRestoreReceiver;
    private final Set<SettingChangeListener> mSettingsListeners = new ArraySet();

    public interface SettingChangeListener {
        void onSettingChanged();

        void onSettingRestored(String str, String str2, int i);
    }

    private SettingsObserver(Context context, Handler handler, final Uri settingUri, final String secureSettingName) {
        this.mSecureSettingName = secureSettingName;
        this.mSettingRestoreReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (!"android.os.action.SETTING_RESTORED".equals(intent.getAction())) {
                    return;
                }
                String element = intent.getStringExtra("setting_name");
                if (!Objects.equals(element, secureSettingName)) {
                    return;
                }
                String prevValue = intent.getStringExtra("previous_value");
                String newValue = intent.getStringExtra("new_value");
                SettingsObserver.this.sendSettingRestored(prevValue, newValue, getSendingUserId());
            }
        };
        this.mContentObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && !settingUri.equals(uri)) {
                    return;
                }
                SettingsObserver.this.sendSettingChanged();
            }
        };
        ContentResolver resolver = context.getContentResolver();
        resolver.registerContentObserver(settingUri, false, this.mContentObserver, -1);
    }

    public static SettingsObserver build(Context context, Handler handler, String settingName) {
        Uri settingUri = Settings.Secure.getUriFor(settingName);
        return new SettingsObserver(context, handler, settingUri, settingName);
    }

    public void addListener(SettingChangeListener listener) {
        this.mSettingsListeners.add(listener);
    }

    public void removeListener(SettingChangeListener listener) {
        this.mSettingsListeners.remove(listener);
    }

    private void sendSettingChanged() {
        for (SettingChangeListener l : this.mSettingsListeners) {
            l.onSettingChanged();
        }
    }

    private void sendSettingRestored(String prevValue, String newValue, int userId) {
        for (SettingChangeListener l : this.mSettingsListeners) {
            l.onSettingRestored(prevValue, newValue, userId);
        }
    }
}
