package com.android.settings.search;

import com.android.settings.ChooseLockGeneric;
import com.android.settings.DateTimeSettings;
import com.android.settings.DevelopmentSettings;
import com.android.settings.DeviceInfoSettings;
import com.android.settings.DisplaySettings;
import com.android.settings.LegalSettings;
import com.android.settings.PrivacySettings;
import com.android.settings.ScreenPinningSettings;
import com.android.settings.SecuritySettings;
import com.android.settings.WallpaperTypeSettings;
import com.android.settings.WirelessSettings;
import com.android.settings.accessibility.AccessibilitySettings;
import com.android.settings.accounts.AccountSettings;
import com.android.settings.applications.AdvancedAppSettings;
import com.android.settings.applications.SpecialAccessSettings;
import com.android.settings.bluetooth.BluetoothSettings;
import com.android.settings.datausage.DataUsageMeteredSettings;
import com.android.settings.datausage.DataUsageSummary;
import com.android.settings.deviceinfo.StorageSettings;
import com.android.settings.display.ScreenZoomSettings;
import com.android.settings.fuelgauge.BatterySaverSettings;
import com.android.settings.fuelgauge.PowerUsageSummary;
import com.android.settings.inputmethod.InputMethodAndLanguageSettings;
import com.android.settings.location.LocationSettings;
import com.android.settings.location.ScanningSettings;
import com.android.settings.notification.ConfigureNotificationSettings;
import com.android.settings.notification.OtherSoundSettings;
import com.android.settings.notification.SoundSettings;
import com.android.settings.notification.ZenModeAutomationSettings;
import com.android.settings.notification.ZenModePrioritySettings;
import com.android.settings.notification.ZenModeSettings;
import com.android.settings.notification.ZenModeVisualInterruptionSettings;
import com.android.settings.print.PrintSettingsFragment;
import com.android.settings.sim.SimSettings;
import com.android.settings.users.UserSettings;
import com.android.settings.wifi.AdvancedWifiSettings;
import com.android.settings.wifi.SavedAccessPointsWifiSettings;
import com.android.settings.wifi.WifiSettings;
import java.util.HashMap;

public final class Ranking {
    public static int sCurrentBaseRank = 2048;
    private static HashMap<String, Integer> sRankMap = new HashMap<>();
    private static HashMap<String, Integer> sBaseRankMap = new HashMap<>();

    static {
        sRankMap.put(WifiSettings.class.getName(), 1);
        sRankMap.put(AdvancedWifiSettings.class.getName(), 1);
        sRankMap.put(SavedAccessPointsWifiSettings.class.getName(), 1);
        sRankMap.put(BluetoothSettings.class.getName(), 2);
        sRankMap.put(SimSettings.class.getName(), 3);
        sRankMap.put(DataUsageSummary.class.getName(), 4);
        sRankMap.put(DataUsageMeteredSettings.class.getName(), 4);
        sRankMap.put(WirelessSettings.class.getName(), 5);
        sRankMap.put(DisplaySettings.class.getName(), 6);
        sRankMap.put(ScreenZoomSettings.class.getName(), 1);
        sRankMap.put(WallpaperTypeSettings.class.getName(), 7);
        sRankMap.put(SoundSettings.class.getName(), 9);
        sRankMap.put(ConfigureNotificationSettings.class.getName(), 8);
        sRankMap.put(OtherSoundSettings.class.getName(), 8);
        sRankMap.put(ZenModeSettings.class.getName(), 8);
        sRankMap.put(ZenModePrioritySettings.class.getName(), 8);
        sRankMap.put(ZenModeAutomationSettings.class.getName(), 8);
        sRankMap.put(ZenModeVisualInterruptionSettings.class.getName(), 8);
        sRankMap.put(StorageSettings.class.getName(), 11);
        sRankMap.put(PowerUsageSummary.class.getName(), 12);
        sRankMap.put(BatterySaverSettings.class.getName(), 12);
        sRankMap.put(AdvancedAppSettings.class.getName(), 10);
        sRankMap.put(SpecialAccessSettings.class.getName(), 10);
        sRankMap.put(UserSettings.class.getName(), 13);
        sRankMap.put(LocationSettings.class.getName(), 14);
        sRankMap.put(ScanningSettings.class.getName(), 14);
        sRankMap.put(SecuritySettings.class.getName(), 15);
        sRankMap.put(ChooseLockGeneric.ChooseLockGenericFragment.class.getName(), 15);
        sRankMap.put(ScreenPinningSettings.class.getName(), 15);
        sRankMap.put(AccountSettings.class.getName(), 16);
        sRankMap.put(InputMethodAndLanguageSettings.class.getName(), 17);
        sRankMap.put(PrivacySettings.class.getName(), 18);
        sRankMap.put(DateTimeSettings.class.getName(), 19);
        sRankMap.put(AccessibilitySettings.class.getName(), 20);
        sRankMap.put(PrintSettingsFragment.class.getName(), 21);
        sRankMap.put(DevelopmentSettings.class.getName(), 22);
        sRankMap.put(DeviceInfoSettings.class.getName(), 23);
        sRankMap.put(LegalSettings.class.getName(), 23);
        sBaseRankMap.put("com.android.settings", 0);
    }

    public static int getRankForClassName(String className) {
        Integer rank = sRankMap.get(className);
        if (rank != null) {
            return rank.intValue();
        }
        return 1024;
    }

    public static int getBaseRankForAuthority(String authority) {
        synchronized (sBaseRankMap) {
            Integer base = sBaseRankMap.get(authority);
            if (base != null) {
                return base.intValue();
            }
            sCurrentBaseRank++;
            sBaseRankMap.put(authority, Integer.valueOf(sCurrentBaseRank));
            return sCurrentBaseRank;
        }
    }
}
