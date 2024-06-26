package com.android.settings.dashboard;

import android.util.ArrayMap;
import com.android.settings.DisplaySettings;
import com.android.settings.accounts.AccountDashboardFragment;
import com.android.settings.accounts.AccountDetailDashboardFragment;
import com.android.settings.applications.AppAndNotificationDashboardFragment;
import com.android.settings.applications.DefaultAppSettings;
import com.android.settings.connecteddevice.AdvancedConnectedDeviceDashboardFragment;
import com.android.settings.connecteddevice.ConnectedDeviceDashboardFragment;
import com.android.settings.development.DevelopmentSettingsDashboardFragment;
import com.android.settings.deviceinfo.StorageDashboardFragment;
import com.android.settings.display.NightDisplaySettings;
import com.android.settings.fuelgauge.PowerUsageSummary;
import com.android.settings.gestures.GestureSettings;
import com.android.settings.language.LanguageAndInputSettings;
import com.android.settings.network.NetworkDashboardFragment;
import com.android.settings.notification.ConfigureNotificationSettings;
import com.android.settings.notification.SoundSettings;
import com.android.settings.notification.ZenModeSettings;
import com.android.settings.security.LockscreenDashboardFragment;
import com.android.settings.security.SecuritySettings;
import com.android.settings.system.SystemDashboardFragment;
import java.util.Map;
/* loaded from: classes.dex */
public class DashboardFragmentRegistry {
    public static final Map<String, String> CATEGORY_KEY_TO_PARENT_MAP;
    public static final Map<String, String> PARENT_TO_CATEGORY_KEY_MAP = new ArrayMap();

    static {
        PARENT_TO_CATEGORY_KEY_MAP.put(NetworkDashboardFragment.class.getName(), "com.android.settings.category.ia.wireless");
        PARENT_TO_CATEGORY_KEY_MAP.put(ConnectedDeviceDashboardFragment.class.getName(), "com.android.settings.category.ia.connect");
        PARENT_TO_CATEGORY_KEY_MAP.put(AdvancedConnectedDeviceDashboardFragment.class.getName(), "com.android.settings.category.ia.device");
        PARENT_TO_CATEGORY_KEY_MAP.put(AppAndNotificationDashboardFragment.class.getName(), "com.android.settings.category.ia.apps");
        PARENT_TO_CATEGORY_KEY_MAP.put(PowerUsageSummary.class.getName(), "com.android.settings.category.ia.battery");
        PARENT_TO_CATEGORY_KEY_MAP.put(DefaultAppSettings.class.getName(), "com.android.settings.category.ia.apps.default");
        PARENT_TO_CATEGORY_KEY_MAP.put(DisplaySettings.class.getName(), "com.android.settings.category.ia.display");
        PARENT_TO_CATEGORY_KEY_MAP.put(SoundSettings.class.getName(), "com.android.settings.category.ia.sound");
        PARENT_TO_CATEGORY_KEY_MAP.put(StorageDashboardFragment.class.getName(), "com.android.settings.category.ia.storage");
        PARENT_TO_CATEGORY_KEY_MAP.put(SecuritySettings.class.getName(), "com.android.settings.category.ia.security");
        PARENT_TO_CATEGORY_KEY_MAP.put(AccountDetailDashboardFragment.class.getName(), "com.android.settings.category.ia.account_detail");
        PARENT_TO_CATEGORY_KEY_MAP.put(AccountDashboardFragment.class.getName(), "com.android.settings.category.ia.accounts");
        PARENT_TO_CATEGORY_KEY_MAP.put(SystemDashboardFragment.class.getName(), "com.android.settings.category.ia.system");
        PARENT_TO_CATEGORY_KEY_MAP.put(LanguageAndInputSettings.class.getName(), "com.android.settings.category.ia.language");
        PARENT_TO_CATEGORY_KEY_MAP.put(DevelopmentSettingsDashboardFragment.class.getName(), "com.android.settings.category.ia.development");
        PARENT_TO_CATEGORY_KEY_MAP.put(ConfigureNotificationSettings.class.getName(), "com.android.settings.category.ia.notifications");
        PARENT_TO_CATEGORY_KEY_MAP.put(LockscreenDashboardFragment.class.getName(), "com.android.settings.category.ia.lockscreen");
        PARENT_TO_CATEGORY_KEY_MAP.put(ZenModeSettings.class.getName(), "com.android.settings.category.ia.dnd");
        PARENT_TO_CATEGORY_KEY_MAP.put(GestureSettings.class.getName(), "com.android.settings.category.ia.gestures");
        PARENT_TO_CATEGORY_KEY_MAP.put(NightDisplaySettings.class.getName(), "com.android.settings.category.ia.night_display");
        CATEGORY_KEY_TO_PARENT_MAP = new ArrayMap(PARENT_TO_CATEGORY_KEY_MAP.size());
        for (Map.Entry<String, String> entry : PARENT_TO_CATEGORY_KEY_MAP.entrySet()) {
            CATEGORY_KEY_TO_PARENT_MAP.put(entry.getValue(), entry.getKey());
        }
    }
}
