package com.android.server.am;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.server.backup.BackupManagerConstants;

class GlobalSettingsToPropertiesMapper {
    private static final String TAG = "GlobalSettingsToPropertiesMapper";
    private static final String[][] sGlobalSettingsMapping = {new String[]{"sys_vdso", "sys.vdso"}, new String[]{"fps_divisor", "debug.hwui.fps_divisor"}, new String[]{"display_panel_lpm", "sys.display_panel_lpm"}, new String[]{"sys_uidcpupower", "sys.uidcpupower"}, new String[]{"sys_traced", "sys.traced.enable_override"}};
    private final ContentResolver mContentResolver;
    private final String[][] mGlobalSettingsMapping;

    @VisibleForTesting
    GlobalSettingsToPropertiesMapper(ContentResolver contentResolver, String[][] strArr) {
        this.mContentResolver = contentResolver;
        this.mGlobalSettingsMapping = strArr;
    }

    void updatePropertiesFromGlobalSettings() {
        for (String[] strArr : this.mGlobalSettingsMapping) {
            final String str = strArr[0];
            final String str2 = strArr[1];
            Uri uriFor = Settings.Global.getUriFor(str);
            Preconditions.checkNotNull(uriFor, "Setting " + str + " not found");
            ContentObserver contentObserver = new ContentObserver(null) {
                @Override
                public void onChange(boolean z) {
                    GlobalSettingsToPropertiesMapper.this.updatePropertyFromSetting(str, str2);
                }
            };
            updatePropertyFromSetting(str, str2);
            this.mContentResolver.registerContentObserver(uriFor, false, contentObserver);
        }
    }

    public static void start(ContentResolver contentResolver) {
        new GlobalSettingsToPropertiesMapper(contentResolver, sGlobalSettingsMapping).updatePropertiesFromGlobalSettings();
    }

    private String getGlobalSetting(String str) {
        return Settings.Global.getString(this.mContentResolver, str);
    }

    private void setProperty(String str, String str2) {
        if (str2 == null) {
            if (TextUtils.isEmpty(systemPropertiesGet(str))) {
                return;
            } else {
                str2 = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            }
        }
        try {
            systemPropertiesSet(str, str2);
        } catch (Exception e) {
            if (Build.IS_DEBUGGABLE) {
                Slog.wtf(TAG, "Unable to set property " + str + " value '" + str2 + "'", e);
                return;
            }
            Slog.e(TAG, "Unable to set property " + str + " value '" + str2 + "'", e);
        }
    }

    @VisibleForTesting
    protected String systemPropertiesGet(String str) {
        return SystemProperties.get(str);
    }

    @VisibleForTesting
    protected void systemPropertiesSet(String str, String str2) {
        SystemProperties.set(str, str2);
    }

    @VisibleForTesting
    void updatePropertyFromSetting(String str, String str2) {
        setProperty(str2, getGlobalSetting(str));
    }
}
