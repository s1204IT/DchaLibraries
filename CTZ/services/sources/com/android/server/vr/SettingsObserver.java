package com.android.server.vr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.ArraySet;
import java.util.Iterator;
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

    private SettingsObserver(Context context, Handler handler, final Uri uri, final String str) {
        this.mSecureSettingName = str;
        this.mSettingRestoreReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if ("android.os.action.SETTING_RESTORED".equals(intent.getAction()) && Objects.equals(intent.getStringExtra("setting_name"), str)) {
                    SettingsObserver.this.sendSettingRestored(intent.getStringExtra("previous_value"), intent.getStringExtra("new_value"), getSendingUserId());
                }
            }
        };
        this.mContentObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean z, Uri uri2) {
                if (uri2 == null || uri.equals(uri2)) {
                    SettingsObserver.this.sendSettingChanged();
                }
            }
        };
        context.getContentResolver().registerContentObserver(uri, false, this.mContentObserver, -1);
    }

    public static SettingsObserver build(Context context, Handler handler, String str) {
        return new SettingsObserver(context, handler, Settings.Secure.getUriFor(str), str);
    }

    public void addListener(SettingChangeListener settingChangeListener) {
        this.mSettingsListeners.add(settingChangeListener);
    }

    public void removeListener(SettingChangeListener settingChangeListener) {
        this.mSettingsListeners.remove(settingChangeListener);
    }

    private void sendSettingChanged() {
        Iterator<SettingChangeListener> it = this.mSettingsListeners.iterator();
        while (it.hasNext()) {
            it.next().onSettingChanged();
        }
    }

    private void sendSettingRestored(String str, String str2, int i) {
        Iterator<SettingChangeListener> it = this.mSettingsListeners.iterator();
        while (it.hasNext()) {
            it.next().onSettingRestored(str, str2, i);
        }
    }
}
