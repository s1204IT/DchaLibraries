package com.android.providers.settings;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.media.AudioService;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.RILConstants;
import com.android.internal.util.XmlUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final HashSet<String> mValidTables = new HashSet<>();
    private Context mContext;
    private int mUserHandle;

    static {
        mValidTables.add("system");
        mValidTables.add("secure");
        mValidTables.add("global");
        mValidTables.add("bluetooth_devices");
        mValidTables.add("bookmarks");
        mValidTables.add("favorites");
        mValidTables.add("gservices");
        mValidTables.add("old_favorites");
    }

    static String dbNameForUser(int userHandle) {
        if (userHandle == 0) {
            return "settings.db";
        }
        File databaseFile = new File(Environment.getUserSystemDirectory(userHandle), "settings.db");
        return databaseFile.getPath();
    }

    public DatabaseHelper(Context context, int userHandle) {
        super(context, dbNameForUser(userHandle), (SQLiteDatabase.CursorFactory) null, 118);
        this.mContext = context;
        this.mUserHandle = userHandle;
    }

    public static boolean isValidTable(String name) {
        return mValidTables.contains(name);
    }

    private void createSecureTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE secure (_id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT UNIQUE ON CONFLICT REPLACE,value TEXT);");
        db.execSQL("CREATE INDEX secureIndex1 ON secure (name);");
    }

    private void createGlobalTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE global (_id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT UNIQUE ON CONFLICT REPLACE,value TEXT);");
        db.execSQL("CREATE INDEX globalIndex1 ON global (name);");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE system (_id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT UNIQUE ON CONFLICT REPLACE,value TEXT);");
        db.execSQL("CREATE INDEX systemIndex1 ON system (name);");
        createSecureTable(db);
        if (this.mUserHandle == 0) {
            createGlobalTable(db);
        }
        db.execSQL("CREATE TABLE bluetooth_devices (_id INTEGER PRIMARY KEY,name TEXT,addr TEXT,channel INTEGER,type INTEGER);");
        db.execSQL("CREATE TABLE bookmarks (_id INTEGER PRIMARY KEY,title TEXT,folder TEXT,intent TEXT,shortcut INTEGER,ordering INTEGER);");
        db.execSQL("CREATE INDEX bookmarksIndex1 ON bookmarks (folder);");
        db.execSQL("CREATE INDEX bookmarksIndex2 ON bookmarks (shortcut);");
        boolean onlyCore = false;
        try {
            onlyCore = IPackageManager.Stub.asInterface(ServiceManager.getService("package")).isOnlyCoreApps();
        } catch (RemoteException e) {
        }
        if (!onlyCore) {
            loadBookmarks(db);
        }
        loadVolumeLevels(db);
        loadSettings(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
        int oldShow;
        SQLiteStatement stmt;
        Log.w("SettingsProvider", "Upgrading settings database from version " + oldVersion + " to " + currentVersion);
        int upgradeVersion = oldVersion;
        if (upgradeVersion == 20) {
            loadVibrateSetting(db, true);
            upgradeVersion = 21;
        }
        if (upgradeVersion < 22) {
            upgradeVersion = 22;
            upgradeLockPatternLocation(db);
        }
        if (upgradeVersion < 23) {
            db.execSQL("UPDATE favorites SET iconResource=0 WHERE iconType=0");
            upgradeVersion = 23;
        }
        if (upgradeVersion == 23) {
            db.beginTransaction();
            try {
                db.execSQL("ALTER TABLE favorites ADD spanX INTEGER");
                db.execSQL("ALTER TABLE favorites ADD spanY INTEGER");
                db.execSQL("UPDATE favorites SET spanX=1, spanY=1 WHERE itemType<=0");
                db.execSQL("UPDATE favorites SET spanX=2, spanY=2 WHERE itemType=1000 or itemType=1002");
                db.execSQL("UPDATE favorites SET spanX=4, spanY=1 WHERE itemType=1001");
                db.setTransactionSuccessful();
                db.endTransaction();
                upgradeVersion = 24;
            } finally {
            }
        }
        if (upgradeVersion == 24) {
            db.beginTransaction();
            try {
                db.execSQL("DELETE FROM system WHERE name='network_preference'");
                db.execSQL("INSERT INTO system ('name', 'value') values ('network_preference', '1')");
                db.setTransactionSuccessful();
                db.endTransaction();
                upgradeVersion = 25;
            } finally {
            }
        }
        if (upgradeVersion == 25) {
            db.beginTransaction();
            try {
                db.execSQL("ALTER TABLE favorites ADD uri TEXT");
                db.execSQL("ALTER TABLE favorites ADD displayMode INTEGER");
                db.setTransactionSuccessful();
                db.endTransaction();
                upgradeVersion = 26;
            } finally {
            }
        }
        if (upgradeVersion == 26) {
            db.beginTransaction();
            try {
                createSecureTable(db);
                db.setTransactionSuccessful();
                db.endTransaction();
                upgradeVersion = 27;
            } finally {
            }
        }
        if (upgradeVersion == 27) {
            String[] settingsToMove = {"adb_enabled", "android_id", "bluetooth_on", "data_roaming", "device_provisioned", "http_proxy", "install_non_market_apps", "location_providers_allowed", "logging_id", "network_preference", "parental_control_enabled", "parental_control_last_update", "parental_control_redirect_url", "settings_classname", "usb_mass_storage_enabled", "use_google_mail", "wifi_networks_available_notification_on", "wifi_networks_available_repeat_delay", "wifi_num_open_networks_kept", "wifi_on", "wifi_watchdog_acceptable_packet_loss_percentage", "wifi_watchdog_ap_count", "wifi_watchdog_background_check_delay_ms", "wifi_watchdog_background_check_enabled", "wifi_watchdog_background_check_timeout_ms", "wifi_watchdog_initial_ignored_ping_count", "wifi_watchdog_max_ap_checks", "wifi_watchdog_on", "wifi_watchdog_ping_count", "wifi_watchdog_ping_delay_ms", "wifi_watchdog_ping_timeout_ms"};
            moveSettingsToNewTable(db, "system", "secure", settingsToMove, false);
            upgradeVersion = 28;
        }
        if (upgradeVersion == 28 || upgradeVersion == 29) {
            db.beginTransaction();
            try {
                db.execSQL("DELETE FROM system WHERE name='mode_ringer_streams_affected'");
                db.execSQL("INSERT INTO system ('name', 'value') values ('mode_ringer_streams_affected', '" + String.valueOf(38) + "')");
                db.setTransactionSuccessful();
                db.endTransaction();
                upgradeVersion = 30;
            } finally {
            }
        }
        if (upgradeVersion == 30) {
            db.beginTransaction();
            try {
                db.execSQL("UPDATE bookmarks SET folder = '@quicklaunch'");
                db.execSQL("UPDATE bookmarks SET title = ''");
                db.setTransactionSuccessful();
                db.endTransaction();
                upgradeVersion = 31;
            } finally {
            }
        }
        if (upgradeVersion == 31) {
            db.beginTransaction();
            SQLiteStatement stmt2 = null;
            try {
                db.execSQL("DELETE FROM system WHERE name='window_animation_scale'");
                db.execSQL("DELETE FROM system WHERE name='transition_animation_scale'");
                stmt = db.compileStatement("INSERT INTO system(name,value) VALUES(?,?);");
                loadDefaultAnimationSettings(stmt);
                db.setTransactionSuccessful();
                upgradeVersion = 32;
            } finally {
            }
        }
        if (upgradeVersion == 32) {
            String wifiWatchList = SystemProperties.get("ro.com.android.wifi-watchlist");
            if (!TextUtils.isEmpty(wifiWatchList)) {
                db.beginTransaction();
                try {
                    db.execSQL("INSERT OR IGNORE INTO secure(name,value) values('wifi_watchdog_watch_list','" + wifiWatchList + "');");
                    db.setTransactionSuccessful();
                } finally {
                }
            }
            upgradeVersion = 33;
        }
        if (upgradeVersion == 33) {
            db.beginTransaction();
            try {
                db.execSQL("INSERT INTO system(name,value) values('zoom','2');");
                db.setTransactionSuccessful();
                db.endTransaction();
                upgradeVersion = 34;
            } finally {
            }
        }
        if (upgradeVersion == 34) {
            db.beginTransaction();
            SQLiteStatement stmt3 = null;
            try {
                stmt3 = db.compileStatement("INSERT OR IGNORE INTO secure(name,value) VALUES(?,?);");
                loadSecure35Settings(stmt3);
                db.setTransactionSuccessful();
                if (stmt3 != null) {
                    stmt3.close();
                }
                upgradeVersion = 35;
            } finally {
                if (stmt3 != null) {
                    stmt3.close();
                }
            }
        }
        if (upgradeVersion == 35) {
            upgradeVersion = 36;
        }
        if (upgradeVersion == 36) {
            db.beginTransaction();
            try {
                db.execSQL("DELETE FROM system WHERE name='mode_ringer_streams_affected'");
                db.execSQL("INSERT INTO system ('name', 'value') values ('mode_ringer_streams_affected', '" + String.valueOf(166) + "')");
                db.setTransactionSuccessful();
                db.endTransaction();
                upgradeVersion = 37;
            } finally {
            }
        }
        if (upgradeVersion == 37) {
            db.beginTransaction();
            SQLiteStatement stmt4 = null;
            try {
                stmt4 = db.compileStatement("INSERT OR IGNORE INTO system(name,value) VALUES(?,?);");
                loadStringSetting(stmt4, "airplane_mode_toggleable_radios", R.string.airplane_mode_toggleable_radios);
                db.setTransactionSuccessful();
                if (stmt4 != null) {
                    stmt4.close();
                }
                upgradeVersion = 38;
            } finally {
                if (stmt4 != null) {
                    stmt4.close();
                }
            }
        }
        if (upgradeVersion == 38) {
            db.beginTransaction();
            try {
                String value = this.mContext.getResources().getBoolean(R.bool.assisted_gps_enabled) ? "1" : "0";
                db.execSQL("INSERT OR IGNORE INTO secure(name,value) values('assisted_gps_enabled','" + value + "');");
                db.setTransactionSuccessful();
                db.endTransaction();
                upgradeVersion = 39;
            } finally {
            }
        }
        if (upgradeVersion == 39) {
            upgradeAutoBrightness(db);
            upgradeVersion = 40;
        }
        if (upgradeVersion == 40) {
            db.beginTransaction();
            SQLiteStatement stmt5 = null;
            try {
                db.execSQL("DELETE FROM system WHERE name='window_animation_scale'");
                db.execSQL("DELETE FROM system WHERE name='transition_animation_scale'");
                stmt5 = db.compileStatement("INSERT INTO system(name,value) VALUES(?,?);");
                loadDefaultAnimationSettings(stmt5);
                db.setTransactionSuccessful();
                if (stmt5 != null) {
                    stmt5.close();
                }
                upgradeVersion = 41;
            } finally {
                if (stmt5 != null) {
                    stmt5.close();
                }
            }
        }
        if (upgradeVersion == 41) {
            db.beginTransaction();
            SQLiteStatement stmt6 = null;
            try {
                db.execSQL("DELETE FROM system WHERE name='haptic_feedback_enabled'");
                stmt6 = db.compileStatement("INSERT INTO system(name,value) VALUES(?,?);");
                loadDefaultHapticSettings(stmt6);
                db.setTransactionSuccessful();
                if (stmt6 != null) {
                    stmt6.close();
                }
                upgradeVersion = 42;
            } finally {
                if (stmt6 != null) {
                    stmt6.close();
                }
            }
        }
        if (upgradeVersion == 42) {
            db.beginTransaction();
            SQLiteStatement stmt7 = null;
            try {
                stmt7 = db.compileStatement("INSERT INTO system(name,value) VALUES(?,?);");
                loadBooleanSetting(stmt7, "notification_light_pulse", R.bool.def_notification_pulse);
                db.setTransactionSuccessful();
                if (stmt7 != null) {
                    stmt7.close();
                }
                upgradeVersion = 43;
            } finally {
                if (stmt7 != null) {
                    stmt7.close();
                }
            }
        }
        if (upgradeVersion == 43) {
            db.beginTransaction();
            SQLiteStatement stmt8 = null;
            try {
                stmt8 = db.compileStatement("INSERT OR IGNORE INTO system(name,value) VALUES(?,?);");
                loadSetting(stmt8, "volume_bluetooth_sco", Integer.valueOf(AudioService.getDefaultStreamVolume(6)));
                db.setTransactionSuccessful();
                if (stmt8 != null) {
                    stmt8.close();
                }
                upgradeVersion = 44;
            } finally {
                if (stmt8 != null) {
                    stmt8.close();
                }
            }
        }
        if (upgradeVersion == 44) {
            db.execSQL("DROP TABLE IF EXISTS gservices");
            db.execSQL("DROP INDEX IF EXISTS gservicesIndex1");
            upgradeVersion = 45;
        }
        if (upgradeVersion == 45) {
            db.beginTransaction();
            try {
                db.execSQL("INSERT INTO secure(name,value) values('mount_play_not_snd','1');");
                db.execSQL("INSERT INTO secure(name,value) values('mount_ums_autostart','0');");
                db.execSQL("INSERT INTO secure(name,value) values('mount_ums_prompt','1');");
                db.execSQL("INSERT INTO secure(name,value) values('mount_ums_notify_enabled','1');");
                db.setTransactionSuccessful();
                db.endTransaction();
                upgradeVersion = 46;
            } finally {
            }
        }
        if (upgradeVersion == 46) {
            db.beginTransaction();
            try {
                db.execSQL("DELETE FROM system WHERE name='lockscreen.password_type';");
                db.setTransactionSuccessful();
                db.endTransaction();
                upgradeVersion = 47;
            } finally {
            }
        }
        if (upgradeVersion == 47) {
            db.beginTransaction();
            try {
                db.execSQL("DELETE FROM system WHERE name='lockscreen.password_type';");
                db.setTransactionSuccessful();
                db.endTransaction();
                upgradeVersion = 48;
            } finally {
            }
        }
        if (upgradeVersion == 48) {
            upgradeVersion = 49;
        }
        if (upgradeVersion == 49) {
            db.beginTransaction();
            SQLiteStatement stmt9 = null;
            try {
                stmt9 = db.compileStatement("INSERT INTO system(name,value) VALUES(?,?);");
                loadUISoundEffectsSettings(stmt9);
                db.setTransactionSuccessful();
                if (stmt9 != null) {
                    stmt9.close();
                }
                upgradeVersion = 50;
            } finally {
                if (stmt9 != null) {
                    stmt9.close();
                }
            }
        }
        if (upgradeVersion == 50) {
            upgradeVersion = 51;
        }
        if (upgradeVersion == 51) {
            String[] settingsToMove2 = {"lock_pattern_autolock", "lock_pattern_visible_pattern", "lock_pattern_tactile_feedback_enabled", "lockscreen.password_type", "lockscreen.lockoutattemptdeadline", "lockscreen.patterneverchosen", "lock_pattern_autolock", "lockscreen.lockedoutpermanently", "lockscreen.password_salt"};
            moveSettingsToNewTable(db, "system", "secure", settingsToMove2, false);
            upgradeVersion = 52;
        }
        if (upgradeVersion == 52) {
            db.beginTransaction();
            SQLiteStatement stmt10 = null;
            try {
                stmt10 = db.compileStatement("INSERT INTO system(name,value) VALUES(?,?);");
                loadBooleanSetting(stmt10, "vibrate_in_silent", R.bool.def_vibrate_in_silent);
                db.setTransactionSuccessful();
                if (stmt10 != null) {
                    stmt10.close();
                }
                upgradeVersion = 53;
            } finally {
                if (stmt10 != null) {
                    stmt10.close();
                }
            }
        }
        if (upgradeVersion == 53) {
            upgradeVersion = 54;
        }
        if (upgradeVersion == 54) {
            db.beginTransaction();
            try {
                upgradeScreenTimeoutFromNever(db);
                db.setTransactionSuccessful();
                db.endTransaction();
                upgradeVersion = 55;
            } finally {
            }
        }
        if (upgradeVersion == 55) {
            String[] settingsToMove3 = {"set_install_location", "default_install_location"};
            moveSettingsToNewTable(db, "system", "secure", settingsToMove3, false);
            db.beginTransaction();
            SQLiteStatement stmt11 = null;
            try {
                stmt11 = db.compileStatement("INSERT INTO system(name,value) VALUES(?,?);");
                loadSetting(stmt11, "set_install_location", 0);
                loadSetting(stmt11, "default_install_location", 0);
                db.setTransactionSuccessful();
                if (stmt11 != null) {
                    stmt11.close();
                }
                upgradeVersion = 56;
            } finally {
                if (stmt11 != null) {
                    stmt11.close();
                }
            }
        }
        if (upgradeVersion == 56) {
            db.beginTransaction();
            SQLiteStatement stmt12 = null;
            try {
                db.execSQL("DELETE FROM system WHERE name='airplane_mode_toggleable_radios'");
                stmt12 = db.compileStatement("INSERT OR IGNORE INTO system(name,value) VALUES(?,?);");
                loadStringSetting(stmt12, "airplane_mode_toggleable_radios", R.string.airplane_mode_toggleable_radios);
                db.setTransactionSuccessful();
                if (stmt12 != null) {
                    stmt12.close();
                }
                upgradeVersion = 57;
            } finally {
                if (stmt12 != null) {
                    stmt12.close();
                }
            }
        }
        if (upgradeVersion == 57) {
            db.beginTransaction();
            SQLiteStatement stmt13 = null;
            try {
                SQLiteStatement stmt14 = db.compileStatement("INSERT INTO secure(name,value) VALUES(?,?);");
                loadBooleanSetting(stmt14, "accessibility_script_injection", R.bool.def_accessibility_script_injection);
                stmt14.close();
                stmt13 = db.compileStatement("INSERT INTO secure(name,value) VALUES(?,?);");
                loadStringSetting(stmt13, "accessibility_web_content_key_bindings", R.string.def_accessibility_web_content_key_bindings);
                db.setTransactionSuccessful();
                if (stmt13 != null) {
                    stmt13.close();
                }
                upgradeVersion = 58;
            } finally {
                if (stmt13 != null) {
                    stmt13.close();
                }
            }
        }
        if (upgradeVersion == 58) {
            int autoTimeValue = getIntValueFromSystem(db, "auto_time", 0);
            db.beginTransaction();
            SQLiteStatement stmt15 = null;
            try {
                stmt15 = db.compileStatement("INSERT INTO system(name,value) VALUES(?,?);");
                loadSetting(stmt15, "auto_time_zone", Integer.valueOf(autoTimeValue));
                db.setTransactionSuccessful();
                if (stmt15 != null) {
                    stmt15.close();
                }
                upgradeVersion = 59;
            } finally {
                if (stmt15 != null) {
                    stmt15.close();
                }
            }
        }
        if (upgradeVersion == 59) {
            db.beginTransaction();
            SQLiteStatement stmt16 = null;
            try {
                stmt16 = db.compileStatement("INSERT INTO system(name,value) VALUES(?,?);");
                loadBooleanSetting(stmt16, "user_rotation", R.integer.def_user_rotation);
                db.setTransactionSuccessful();
                if (stmt16 != null) {
                    stmt16.close();
                }
                upgradeVersion = 60;
            } finally {
                if (stmt16 != null) {
                    stmt16.close();
                }
            }
        }
        if (upgradeVersion == 60) {
            upgradeVersion = 61;
        }
        if (upgradeVersion == 61) {
            upgradeVersion = 62;
        }
        if (upgradeVersion == 62) {
            upgradeVersion = 63;
        }
        if (upgradeVersion == 63) {
            db.beginTransaction();
            try {
                db.execSQL("DELETE FROM system WHERE name='mode_ringer_streams_affected'");
                db.execSQL("INSERT INTO system ('name', 'value') values ('mode_ringer_streams_affected', '" + String.valueOf(174) + "')");
                db.setTransactionSuccessful();
                db.endTransaction();
                upgradeVersion = 64;
            } finally {
            }
        }
        if (upgradeVersion == 64) {
            db.beginTransaction();
            SQLiteStatement stmt17 = null;
            try {
                stmt17 = db.compileStatement("INSERT INTO secure(name,value) VALUES(?,?);");
                loadIntegerSetting(stmt17, "long_press_timeout", R.integer.def_long_press_timeout_millis);
                stmt17.close();
                db.setTransactionSuccessful();
                if (stmt17 != null) {
                    stmt17.close();
                }
                upgradeVersion = 65;
            } finally {
                if (stmt17 != null) {
                    stmt17.close();
                }
            }
        }
        if (upgradeVersion == 65) {
            db.beginTransaction();
            SQLiteStatement stmt18 = null;
            try {
                db.execSQL("DELETE FROM system WHERE name='window_animation_scale'");
                db.execSQL("DELETE FROM system WHERE name='transition_animation_scale'");
                stmt18 = db.compileStatement("INSERT INTO system(name,value) VALUES(?,?);");
                loadDefaultAnimationSettings(stmt18);
                db.setTransactionSuccessful();
                if (stmt18 != null) {
                    stmt18.close();
                }
                upgradeVersion = 66;
            } finally {
                if (stmt18 != null) {
                    stmt18.close();
                }
            }
        }
        if (upgradeVersion == 66) {
            db.beginTransaction();
            int ringerModeAffectedStreams = 166;
            try {
                if (!this.mContext.getResources().getBoolean(android.R.^attr-private.externalRouteEnabledDrawable)) {
                    ringerModeAffectedStreams = 166 | 8;
                }
                db.execSQL("DELETE FROM system WHERE name='mode_ringer_streams_affected'");
                db.execSQL("INSERT INTO system ('name', 'value') values ('mode_ringer_streams_affected', '" + String.valueOf(ringerModeAffectedStreams) + "')");
                db.setTransactionSuccessful();
                db.endTransaction();
                upgradeVersion = 67;
            } finally {
            }
        }
        if (upgradeVersion == 67) {
            db.beginTransaction();
            SQLiteStatement stmt19 = null;
            try {
                stmt19 = db.compileStatement("INSERT INTO secure(name,value) VALUES(?,?);");
                loadBooleanSetting(stmt19, "touch_exploration_enabled", R.bool.def_touch_exploration_enabled);
                stmt19.close();
                db.setTransactionSuccessful();
                if (stmt19 != null) {
                    stmt19.close();
                }
                upgradeVersion = 68;
            } finally {
                if (stmt19 != null) {
                    stmt19.close();
                }
            }
        }
        if (upgradeVersion == 68) {
            db.beginTransaction();
            try {
                db.execSQL("DELETE FROM system WHERE name='notifications_use_ring_volume'");
                db.setTransactionSuccessful();
                db.endTransaction();
                upgradeVersion = 69;
            } finally {
            }
        }
        if (upgradeVersion == 69) {
            String airplaneRadios = this.mContext.getResources().getString(R.string.def_airplane_mode_radios);
            String toggleableRadios = this.mContext.getResources().getString(R.string.airplane_mode_toggleable_radios);
            db.beginTransaction();
            try {
                db.execSQL("UPDATE system SET value='" + airplaneRadios + "' WHERE name='airplane_mode_radios'");
                db.execSQL("UPDATE system SET value='" + toggleableRadios + "' WHERE name='airplane_mode_toggleable_radios'");
                db.setTransactionSuccessful();
                db.endTransaction();
                upgradeVersion = 70;
            } finally {
            }
        }
        if (upgradeVersion == 70) {
            loadBookmarks(db);
            upgradeVersion = 71;
        }
        if (upgradeVersion == 71) {
            db.beginTransaction();
            SQLiteStatement stmt20 = null;
            try {
                stmt20 = db.compileStatement("INSERT INTO secure(name,value) VALUES(?,?);");
                loadBooleanSetting(stmt20, "speak_password", R.bool.def_accessibility_speak_password);
                db.setTransactionSuccessful();
                if (stmt20 != null) {
                    stmt20.close();
                }
                upgradeVersion = 72;
            } finally {
                if (stmt20 != null) {
                    stmt20.close();
                }
            }
        }
        if (upgradeVersion == 72) {
            db.beginTransaction();
            SQLiteStatement stmt21 = null;
            try {
                stmt21 = db.compileStatement("INSERT OR REPLACE INTO system(name,value) VALUES(?,?);");
                loadBooleanSetting(stmt21, "vibrate_in_silent", R.bool.def_vibrate_in_silent);
                db.setTransactionSuccessful();
                if (stmt21 != null) {
                    stmt21.close();
                }
                upgradeVersion = 73;
            } finally {
                if (stmt21 != null) {
                    stmt21.close();
                }
            }
        }
        if (upgradeVersion == 73) {
            upgradeVibrateSettingFromNone(db);
            upgradeVersion = 74;
        }
        if (upgradeVersion == 74) {
            db.beginTransaction();
            stmt = null;
            try {
                stmt = db.compileStatement("INSERT INTO secure(name,value) VALUES(?,?);");
                loadStringSetting(stmt, "accessibility_script_injection_url", R.string.def_accessibility_screen_reader_url);
                db.setTransactionSuccessful();
                if (stmt != null) {
                    stmt.close();
                }
                upgradeVersion = 75;
            } finally {
                if (stmt != null) {
                    stmt.close();
                }
            }
        }
        if (upgradeVersion == 75) {
            db.beginTransaction();
            SQLiteStatement stmt22 = null;
            Cursor c = null;
            try {
                c = db.query("secure", new String[]{"_id", "value"}, "name='lockscreen.disabled'", null, null, null, null);
                if (c == null || c.getCount() == 0) {
                    stmt22 = db.compileStatement("INSERT INTO system(name,value) VALUES(?,?);");
                    loadBooleanSetting(stmt22, "lockscreen.disabled", R.bool.def_lockscreen_disabled);
                }
                db.setTransactionSuccessful();
                upgradeVersion = 76;
            } finally {
                if (c != null) {
                    c.close();
                }
                if (stmt22 != null) {
                    stmt22.close();
                }
            }
        }
        if (upgradeVersion == 76) {
            db.beginTransaction();
            try {
                db.execSQL("DELETE FROM system WHERE name='vibrate_in_silent'");
                db.setTransactionSuccessful();
                db.endTransaction();
                upgradeVersion = 77;
            } finally {
            }
        }
        if (upgradeVersion == 77) {
            loadVibrateWhenRingingSetting(db);
            upgradeVersion = 78;
        }
        if (upgradeVersion == 78) {
            db.beginTransaction();
            SQLiteStatement stmt23 = null;
            try {
                stmt23 = db.compileStatement("INSERT OR REPLACE INTO secure(name,value) VALUES(?,?);");
                loadStringSetting(stmt23, "accessibility_script_injection_url", R.string.def_accessibility_screen_reader_url);
                db.setTransactionSuccessful();
                if (stmt23 != null) {
                    stmt23.close();
                }
                upgradeVersion = 79;
            } finally {
                if (stmt23 != null) {
                    stmt23.close();
                }
            }
        }
        if (upgradeVersion == 79) {
            boolean accessibilityEnabled = getIntValueFromTable(db, "secure", "accessibility_enabled", 0) == 1;
            boolean touchExplorationEnabled = getIntValueFromTable(db, "secure", "touch_exploration_enabled", 0) == 1;
            if (accessibilityEnabled && touchExplorationEnabled) {
                String enabledServices = getStringValueFromTable(db, "secure", "enabled_accessibility_services", "");
                String touchExplorationGrantedServices = getStringValueFromTable(db, "secure", "touch_exploration_granted_accessibility_services", "");
                if (TextUtils.isEmpty(touchExplorationGrantedServices) && !TextUtils.isEmpty(enabledServices)) {
                    SQLiteStatement stmt24 = null;
                    try {
                        db.beginTransaction();
                        stmt24 = db.compileStatement("INSERT OR REPLACE INTO secure(name,value) VALUES(?,?);");
                        loadSetting(stmt24, "touch_exploration_granted_accessibility_services", enabledServices);
                        db.setTransactionSuccessful();
                        if (stmt24 != null) {
                            stmt24.close();
                        }
                    } finally {
                        if (stmt24 != null) {
                            stmt24.close();
                        }
                    }
                }
            }
            upgradeVersion = 80;
        }
        if (upgradeVersion == 80) {
            db.beginTransaction();
            SQLiteStatement stmt25 = null;
            try {
                stmt25 = db.compileStatement("INSERT OR REPLACE INTO secure(name,value) VALUES(?,?);");
                loadBooleanSetting(stmt25, "screensaver_enabled", android.R.^attr-private.glowDot);
                loadBooleanSetting(stmt25, "screensaver_activate_on_dock", android.R.^attr-private.glyphDrawable);
                loadBooleanSetting(stmt25, "screensaver_activate_on_sleep", android.R.^attr-private.glyphMap);
                loadStringSetting(stmt25, "screensaver_components", android.R.string.config_systemSupervision);
                loadStringSetting(stmt25, "screensaver_default_component", android.R.string.config_systemSupervision);
                db.setTransactionSuccessful();
                if (stmt25 != null) {
                    stmt25.close();
                }
                upgradeVersion = 81;
            } finally {
                if (stmt25 != null) {
                    stmt25.close();
                }
            }
        }
        if (upgradeVersion == 81) {
            db.beginTransaction();
            SQLiteStatement stmt26 = null;
            try {
                stmt26 = db.compileStatement("INSERT OR REPLACE INTO secure(name,value) VALUES(?,?);");
                loadBooleanSetting(stmt26, "package_verifier_enable", R.bool.def_package_verifier_enable);
                db.setTransactionSuccessful();
                if (stmt26 != null) {
                    stmt26.close();
                }
                upgradeVersion = 82;
            } finally {
                if (stmt26 != null) {
                    stmt26.close();
                }
            }
        }
        if (upgradeVersion == 82) {
            if (this.mUserHandle == 0) {
                db.beginTransaction();
                SQLiteStatement stmt27 = null;
                try {
                    createGlobalTable(db);
                    moveSettingsToNewTable(db, "system", "global", hashsetToStringArray(SettingsProvider.sSystemGlobalKeys), false);
                    moveSettingsToNewTable(db, "secure", "global", hashsetToStringArray(SettingsProvider.sSecureGlobalKeys), false);
                    db.setTransactionSuccessful();
                    if (stmt27 != null) {
                        stmt27.close();
                    }
                } finally {
                    if (stmt27 != null) {
                        stmt27.close();
                    }
                }
            }
            upgradeVersion = 83;
        }
        if (upgradeVersion == 83) {
            db.beginTransaction();
            SQLiteStatement stmt28 = null;
            try {
                SQLiteStatement stmt29 = db.compileStatement("INSERT INTO secure(name,value) VALUES(?,?);");
                loadBooleanSetting(stmt29, "accessibility_display_magnification_enabled", R.bool.def_accessibility_display_magnification_enabled);
                stmt29.close();
                SQLiteStatement stmt30 = db.compileStatement("INSERT INTO secure(name,value) VALUES(?,?);");
                loadFractionSetting(stmt30, "accessibility_display_magnification_scale", R.fraction.def_accessibility_display_magnification_scale, 1);
                stmt30.close();
                stmt28 = db.compileStatement("INSERT INTO secure(name,value) VALUES(?,?);");
                loadBooleanSetting(stmt28, "accessibility_display_magnification_auto_update", R.bool.def_accessibility_display_magnification_auto_update);
                db.setTransactionSuccessful();
                if (stmt28 != null) {
                    stmt28.close();
                }
                upgradeVersion = 84;
            } finally {
                if (stmt28 != null) {
                    stmt28.close();
                }
            }
        }
        if (upgradeVersion == 84) {
            if (this.mUserHandle == 0) {
                db.beginTransaction();
                SQLiteStatement stmt31 = null;
                try {
                    String[] settingsToMove4 = {"adb_enabled", "bluetooth_on", "data_roaming", "device_provisioned", "install_non_market_apps", "usb_mass_storage_enabled"};
                    moveSettingsToNewTable(db, "secure", "global", settingsToMove4, true);
                    db.setTransactionSuccessful();
                    db.endTransaction();
                    if (stmt31 != null) {
                        stmt31.close();
                    }
                } finally {
                    if (stmt31 != null) {
                        stmt31.close();
                    }
                }
            }
            upgradeVersion = 85;
        }
        if (upgradeVersion == 85) {
            if (this.mUserHandle == 0) {
                db.beginTransaction();
                try {
                    String[] settingsToMove5 = {"stay_on_while_plugged_in"};
                    moveSettingsToNewTable(db, "system", "global", settingsToMove5, true);
                    db.setTransactionSuccessful();
                    db.endTransaction();
                } finally {
                }
            }
            upgradeVersion = 86;
        }
        if (upgradeVersion == 86) {
            if (this.mUserHandle == 0) {
                db.beginTransaction();
                try {
                    String[] settingsToMove6 = {"package_verifier_enable", "verifier_timeout", "verifier_default_response"};
                    moveSettingsToNewTable(db, "secure", "global", settingsToMove6, true);
                    db.setTransactionSuccessful();
                    db.endTransaction();
                } finally {
                }
            }
            upgradeVersion = 87;
        }
        if (upgradeVersion == 87) {
            if (this.mUserHandle == 0) {
                db.beginTransaction();
                try {
                    String[] settingsToMove7 = {"data_stall_alarm_non_aggressive_delay_in_ms", "data_stall_alarm_aggressive_delay_in_ms", "gprs_register_check_period_ms"};
                    moveSettingsToNewTable(db, "secure", "global", settingsToMove7, true);
                    db.setTransactionSuccessful();
                    db.endTransaction();
                } finally {
                }
            }
            upgradeVersion = 88;
        }
        if (upgradeVersion == 88) {
            if (this.mUserHandle == 0) {
                db.beginTransaction();
                try {
                    String[] settingsToMove8 = {"battery_discharge_duration_threshold", "battery_discharge_threshold", "send_action_app_error", "dropbox_age_seconds", "dropbox_max_files", "dropbox_quota_kb", "dropbox_quota_percent", "dropbox_reserve_percent", "dropbox:", "logcat_for_", "sys_free_storage_log_interval", "disk_free_change_reporting_threshold", "sys_storage_threshold_percentage", "sys_storage_threshold_max_bytes", "sys_storage_full_threshold_bytes", "sync_max_retry_delay_in_seconds", "connectivity_change_delay", "captive_portal_detection_enabled", "captive_portal_server", "nsd_on", "set_install_location", "default_install_location", "inet_condition_debounce_up_delay", "inet_condition_debounce_down_delay", "read_external_storage_enforced_default", "http_proxy", "global_http_proxy_host", "global_http_proxy_port", "global_http_proxy_exclusion_list", "set_global_http_proxy", "default_dns_server"};
                    moveSettingsToNewTable(db, "secure", "global", settingsToMove8, true);
                    db.setTransactionSuccessful();
                    db.endTransaction();
                } finally {
                }
            }
            upgradeVersion = 89;
        }
        if (upgradeVersion == 89) {
            if (this.mUserHandle == 0) {
                db.beginTransaction();
                try {
                    String[] prefixesToMove = {"bluetooth_headset_priority_", "bluetooth_a2dp_sink_priority_", "bluetooth_input_device_priority_"};
                    movePrefixedSettingsToNewTable(db, "secure", "global", prefixesToMove);
                    db.setTransactionSuccessful();
                    db.endTransaction();
                } finally {
                }
            }
            upgradeVersion = 90;
        }
        if (upgradeVersion == 90) {
            if (this.mUserHandle == 0) {
                db.beginTransaction();
                try {
                    String[] systemToGlobal = {"window_animation_scale", "transition_animation_scale", "animator_duration_scale", "fancy_ime_animations", "compatibility_mode", "emergency_tone", "call_auto_retry", "debug_app", "wait_for_debugger", "show_processes", "always_finish_activities"};
                    String[] secureToGlobal = {"preferred_network_mode", "subscription_mode"};
                    moveSettingsToNewTable(db, "system", "global", systemToGlobal, true);
                    moveSettingsToNewTable(db, "secure", "global", secureToGlobal, true);
                    db.setTransactionSuccessful();
                } finally {
                }
            }
            upgradeVersion = 91;
        }
        if (upgradeVersion == 91) {
            if (this.mUserHandle == 0) {
                db.beginTransaction();
                try {
                    String[] settingsToMove9 = {"mode_ringer"};
                    moveSettingsToNewTable(db, "system", "global", settingsToMove9, true);
                    db.setTransactionSuccessful();
                } finally {
                }
            }
            upgradeVersion = 92;
        }
        if (upgradeVersion == 92) {
            SQLiteStatement stmt32 = null;
            try {
                SQLiteStatement stmt33 = db.compileStatement("INSERT OR IGNORE INTO secure(name,value) VALUES(?,?);");
                if (this.mUserHandle == 0) {
                    int deviceProvisioned = getIntValueFromTable(db, "global", "device_provisioned", 0);
                    loadSetting(stmt33, "user_setup_complete", Integer.valueOf(deviceProvisioned));
                } else {
                    loadBooleanSetting(stmt33, "user_setup_complete", R.bool.def_user_setup_complete);
                }
                if (stmt33 != null) {
                    stmt33.close();
                }
                upgradeVersion = 93;
            } finally {
                if (stmt32 != null) {
                    stmt32.close();
                }
            }
        }
        if (upgradeVersion == 93) {
            if (this.mUserHandle == 0) {
                db.beginTransaction();
                try {
                    moveSettingsToNewTable(db, "system", "global", hashsetToStringArray(SettingsProvider.sSystemGlobalKeys), true);
                    moveSettingsToNewTable(db, "secure", "global", hashsetToStringArray(SettingsProvider.sSecureGlobalKeys), true);
                    db.setTransactionSuccessful();
                } finally {
                }
            }
            upgradeVersion = 94;
        }
        if (upgradeVersion == 94) {
            if (this.mUserHandle == 0) {
                db.beginTransaction();
                SQLiteStatement stmt34 = null;
                try {
                    stmt34 = db.compileStatement("INSERT OR REPLACE INTO global(name,value) VALUES(?,?);");
                    loadStringSetting(stmt34, "wireless_charging_started_sound", R.string.def_wireless_charging_started_sound);
                    db.setTransactionSuccessful();
                    if (stmt34 != null) {
                        stmt34.close();
                    }
                } finally {
                    if (stmt34 != null) {
                        stmt34.close();
                    }
                }
            }
            upgradeVersion = 95;
        }
        if (upgradeVersion == 95) {
            if (this.mUserHandle == 0) {
                db.beginTransaction();
                try {
                    String[] settingsToMove10 = {"bugreport_in_power_menu"};
                    moveSettingsToNewTable(db, "secure", "global", settingsToMove10, true);
                    db.setTransactionSuccessful();
                } finally {
                }
            }
            upgradeVersion = 96;
        }
        if (upgradeVersion == 96) {
            upgradeVersion = 97;
        }
        if (upgradeVersion == 97) {
            if (this.mUserHandle == 0) {
                db.beginTransaction();
                SQLiteStatement stmt35 = null;
                try {
                    stmt35 = db.compileStatement("INSERT OR REPLACE INTO global(name,value) VALUES(?,?);");
                    loadIntegerSetting(stmt35, "low_battery_sound_timeout", R.integer.def_low_battery_sound_timeout);
                    db.setTransactionSuccessful();
                    if (stmt35 != null) {
                        stmt35.close();
                    }
                } finally {
                    if (stmt35 != null) {
                        stmt35.close();
                    }
                }
            }
            upgradeVersion = 98;
        }
        if (upgradeVersion == 98) {
            upgradeVersion = 99;
        }
        if (upgradeVersion == 99) {
            upgradeVersion = 100;
        }
        if (upgradeVersion == 100) {
            if (this.mUserHandle == 0) {
                db.beginTransaction();
                SQLiteStatement stmt36 = null;
                try {
                    stmt36 = db.compileStatement("INSERT OR REPLACE INTO global(name,value) VALUES(?,?);");
                    loadIntegerSetting(stmt36, "heads_up_notifications_enabled", R.integer.def_heads_up_enabled);
                    db.setTransactionSuccessful();
                    if (stmt36 != null) {
                        stmt36.close();
                    }
                } finally {
                    if (stmt36 != null) {
                        stmt36.close();
                    }
                }
            }
            upgradeVersion = 101;
        }
        if (upgradeVersion == 101) {
            if (this.mUserHandle == 0) {
                db.beginTransaction();
                SQLiteStatement stmt37 = null;
                try {
                    stmt37 = db.compileStatement("INSERT OR IGNORE INTO global(name,value) VALUES(?,?);");
                    loadSetting(stmt37, "device_name", getDefaultDeviceName());
                    db.setTransactionSuccessful();
                    if (stmt37 != null) {
                        stmt37.close();
                    }
                } finally {
                    if (stmt37 != null) {
                        stmt37.close();
                    }
                }
            }
            upgradeVersion = 102;
        }
        if (upgradeVersion == 102) {
            db.beginTransaction();
            SQLiteStatement stmt38 = null;
            try {
                if (this.mUserHandle == 0) {
                    String[] globalToSecure = {"install_non_market_apps"};
                    moveSettingsToNewTable(db, "global", "secure", globalToSecure, true);
                } else {
                    stmt38 = db.compileStatement("INSERT OR IGNORE INTO secure(name,value) VALUES(?,?);");
                    loadBooleanSetting(stmt38, "install_non_market_apps", R.bool.def_install_non_market_apps);
                }
                db.setTransactionSuccessful();
                if (stmt38 != null) {
                    stmt38.close();
                }
                upgradeVersion = 103;
            } finally {
                if (stmt38 != null) {
                    stmt38.close();
                }
            }
        }
        if (upgradeVersion == 103) {
            db.beginTransaction();
            SQLiteStatement stmt39 = null;
            try {
                stmt39 = db.compileStatement("INSERT OR REPLACE INTO secure(name,value) VALUES(?,?);");
                loadBooleanSetting(stmt39, "wake_gesture_enabled", R.bool.def_wake_gesture_enabled);
                db.setTransactionSuccessful();
                if (stmt39 != null) {
                    stmt39.close();
                }
                upgradeVersion = 104;
            } finally {
                if (stmt39 != null) {
                    stmt39.close();
                }
            }
        }
        if (upgradeVersion < 105) {
            if (this.mUserHandle == 0) {
                db.beginTransaction();
                SQLiteStatement stmt40 = null;
                try {
                    stmt40 = db.compileStatement("INSERT OR IGNORE INTO global(name,value) VALUES(?,?);");
                    loadBooleanSetting(stmt40, "guest_user_enabled", R.bool.def_guest_user_enabled);
                    db.setTransactionSuccessful();
                    if (stmt40 != null) {
                        stmt40.close();
                    }
                } finally {
                    if (stmt40 != null) {
                        stmt40.close();
                    }
                }
            }
            upgradeVersion = 105;
        }
        if (upgradeVersion < 106) {
            db.beginTransaction();
            SQLiteStatement stmt41 = null;
            try {
                stmt41 = db.compileStatement("INSERT OR IGNORE INTO secure(name,value) VALUES(?,?);");
                loadIntegerSetting(stmt41, "lock_screen_show_notifications", R.integer.def_lock_screen_show_notifications);
                if (this.mUserHandle == 0 && (oldShow = getIntValueFromTable(db, "global", "lock_screen_show_notifications", -1)) >= 0) {
                    loadSetting(stmt41, "lock_screen_show_notifications", Integer.valueOf(oldShow));
                    SQLiteStatement deleteStmt = db.compileStatement("DELETE FROM global WHERE name=?");
                    deleteStmt.bindString(1, "lock_screen_show_notifications");
                    deleteStmt.execute();
                }
                db.setTransactionSuccessful();
                if (stmt41 != null) {
                    stmt41.close();
                }
                upgradeVersion = 106;
            } finally {
                if (stmt41 != null) {
                    stmt41.close();
                }
            }
        }
        if (upgradeVersion < 107) {
            if (this.mUserHandle == 0) {
                db.beginTransaction();
                SQLiteStatement stmt42 = null;
                try {
                    stmt42 = db.compileStatement("INSERT OR REPLACE INTO global(name,value) VALUES(?,?);");
                    loadStringSetting(stmt42, "trusted_sound", R.string.def_trusted_sound);
                    db.setTransactionSuccessful();
                    if (stmt42 != null) {
                        stmt42.close();
                    }
                } finally {
                    if (stmt42 != null) {
                        stmt42.close();
                    }
                }
            }
            upgradeVersion = 107;
        }
        if (upgradeVersion < 108) {
            db.beginTransaction();
            SQLiteStatement stmt43 = null;
            try {
                stmt43 = db.compileStatement("INSERT OR REPLACE INTO system(name,value) VALUES(?,?);");
                loadBooleanSetting(stmt43, "screen_brightness_mode", R.bool.def_screen_brightness_automatic_mode);
                db.setTransactionSuccessful();
                if (stmt43 != null) {
                    stmt43.close();
                }
                upgradeVersion = 108;
            } finally {
                if (stmt43 != null) {
                    stmt43.close();
                }
            }
        }
        if (upgradeVersion < 109) {
            db.beginTransaction();
            SQLiteStatement stmt44 = null;
            try {
                stmt44 = db.compileStatement("INSERT OR IGNORE INTO secure(name,value) VALUES(?,?);");
                loadBooleanSetting(stmt44, "lock_screen_allow_private_notifications", R.bool.def_lock_screen_allow_private_notifications);
                db.setTransactionSuccessful();
                if (stmt44 != null) {
                    stmt44.close();
                }
                upgradeVersion = 109;
            } finally {
                if (stmt44 != null) {
                    stmt44.close();
                }
            }
        }
        if (upgradeVersion < 110) {
            db.beginTransaction();
            SQLiteStatement stmt45 = null;
            try {
                stmt45 = db.compileStatement("UPDATE system SET value = ? WHERE name = ? AND value = ?;");
                stmt45.bindString(1, "SIP_ADDRESS_ONLY");
                stmt45.bindString(2, "sip_call_options");
                stmt45.bindString(3, "SIP_ASK_ME_EACH_TIME");
                stmt45.execute();
                db.setTransactionSuccessful();
                if (stmt45 != null) {
                    stmt45.close();
                }
                upgradeVersion = 110;
            } finally {
                if (stmt45 != null) {
                    stmt45.close();
                }
            }
        }
        if (upgradeVersion < 111) {
            if (this.mUserHandle == 0) {
                db.beginTransaction();
                SQLiteStatement stmt46 = null;
                try {
                    stmt46 = db.compileStatement("INSERT OR REPLACE INTO global(name,value) VALUES(?,?);");
                    loadSetting(stmt46, "mode_ringer", 2);
                    db.setTransactionSuccessful();
                    if (stmt46 != null) {
                        stmt46.close();
                    }
                } finally {
                    if (stmt46 != null) {
                        stmt46.close();
                    }
                }
            }
            upgradeVersion = 111;
        }
        if (upgradeVersion < 112) {
            if (this.mUserHandle == 0) {
                db.beginTransaction();
                SQLiteStatement stmt47 = null;
                try {
                    stmt47 = db.compileStatement("UPDATE global SET value = ?  WHERE name = ? AND value = ?");
                    stmt47.bindString(1, getDefaultDeviceName());
                    stmt47.bindString(2, "device_name");
                    stmt47.bindString(3, getOldDefaultDeviceName());
                    stmt47.execute();
                    db.setTransactionSuccessful();
                    if (stmt47 != null) {
                        stmt47.close();
                    }
                } finally {
                    if (stmt47 != null) {
                        stmt47.close();
                    }
                }
            }
            upgradeVersion = 112;
        }
        if (upgradeVersion < 113) {
            db.beginTransaction();
            SQLiteStatement stmt48 = null;
            try {
                stmt48 = db.compileStatement("INSERT OR IGNORE INTO secure(name,value) VALUES(?,?);");
                loadIntegerSetting(stmt48, "sleep_timeout", R.integer.def_sleep_timeout);
                db.setTransactionSuccessful();
                if (stmt48 != null) {
                    stmt48.close();
                }
                upgradeVersion = 113;
            } finally {
                if (stmt48 != null) {
                    stmt48.close();
                }
            }
        }
        if (upgradeVersion < 115) {
            if (this.mUserHandle == 0) {
                db.beginTransaction();
                SQLiteStatement stmt49 = null;
                try {
                    stmt49 = db.compileStatement("INSERT OR IGNORE INTO global(name,value) VALUES(?,?);");
                    loadBooleanSetting(stmt49, "theater_mode_on", R.bool.def_theater_mode_on);
                    db.setTransactionSuccessful();
                    if (stmt49 != null) {
                        stmt49.close();
                    }
                } finally {
                    if (stmt49 != null) {
                        stmt49.close();
                    }
                }
            }
            upgradeVersion = 115;
        }
        if (upgradeVersion < 116) {
            if (this.mUserHandle == 0) {
                db.beginTransaction();
                SQLiteStatement stmt50 = null;
                try {
                    stmt50 = db.compileStatement("INSERT OR IGNORE INTO global(name,value) VALUES(?,?);");
                    loadSetting(stmt50, "volte_vt_enabled", 1);
                    db.setTransactionSuccessful();
                    if (stmt50 != null) {
                        stmt50.close();
                    }
                } finally {
                    if (stmt50 != null) {
                        stmt50.close();
                    }
                }
            }
            upgradeVersion = 116;
        }
        if (upgradeVersion < 117) {
            db.beginTransaction();
            try {
                String[] systemToSecure = {"lock_to_app_exit_locked"};
                moveSettingsToNewTable(db, "system", "secure", systemToSecure, true);
                db.setTransactionSuccessful();
                db.endTransaction();
                upgradeVersion = 117;
            } finally {
            }
        }
        if (upgradeVersion < 118) {
            db.beginTransaction();
            SQLiteStatement stmt51 = null;
            try {
                stmt51 = db.compileStatement("INSERT OR REPLACE INTO system(name,value) VALUES(?,?);");
                loadSetting(stmt51, "hide_rotation_lock_toggle_for_accessibility", 0);
                db.setTransactionSuccessful();
                if (stmt51 != null) {
                    stmt51.close();
                }
                upgradeVersion = 118;
            } finally {
                if (stmt51 != null) {
                    stmt51.close();
                }
            }
        }
        if (upgradeVersion != currentVersion) {
            Log.w("SettingsProvider", "Got stuck trying to upgrade from version " + upgradeVersion + ", must wipe the settings provider");
            db.execSQL("DROP TABLE IF EXISTS global");
            db.execSQL("DROP TABLE IF EXISTS globalIndex1");
            db.execSQL("DROP TABLE IF EXISTS system");
            db.execSQL("DROP INDEX IF EXISTS systemIndex1");
            db.execSQL("DROP TABLE IF EXISTS secure");
            db.execSQL("DROP INDEX IF EXISTS secureIndex1");
            db.execSQL("DROP TABLE IF EXISTS gservices");
            db.execSQL("DROP INDEX IF EXISTS gservicesIndex1");
            db.execSQL("DROP TABLE IF EXISTS bluetooth_devices");
            db.execSQL("DROP TABLE IF EXISTS bookmarks");
            db.execSQL("DROP INDEX IF EXISTS bookmarksIndex1");
            db.execSQL("DROP INDEX IF EXISTS bookmarksIndex2");
            db.execSQL("DROP TABLE IF EXISTS favorites");
            onCreate(db);
            String wipeReason = oldVersion + "/" + upgradeVersion + "/" + currentVersion;
            db.execSQL("INSERT INTO secure(name,value) values('wiped_db_reason','" + wipeReason + "');");
        }
    }

    private String[] hashsetToStringArray(HashSet<String> set) {
        String[] array = new String[set.size()];
        return (String[]) set.toArray(array);
    }

    private void moveSettingsToNewTable(SQLiteDatabase db, String sourceTable, String destTable, String[] settingsToMove, boolean doIgnore) {
        SQLiteStatement insertStmt = null;
        SQLiteStatement deleteStmt = null;
        db.beginTransaction();
        try {
            insertStmt = db.compileStatement("INSERT " + (doIgnore ? " OR IGNORE " : "") + " INTO " + destTable + " (name,value) SELECT name,value FROM " + sourceTable + " WHERE name=?");
            deleteStmt = db.compileStatement("DELETE FROM " + sourceTable + " WHERE name=?");
            for (String setting : settingsToMove) {
                insertStmt.bindString(1, setting);
                insertStmt.execute();
                deleteStmt.bindString(1, setting);
                deleteStmt.execute();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            if (insertStmt != null) {
                insertStmt.close();
            }
            if (deleteStmt != null) {
                deleteStmt.close();
            }
        }
    }

    private void movePrefixedSettingsToNewTable(SQLiteDatabase db, String sourceTable, String destTable, String[] prefixesToMove) {
        SQLiteStatement insertStmt = null;
        SQLiteStatement deleteStmt = null;
        db.beginTransaction();
        try {
            insertStmt = db.compileStatement("INSERT INTO " + destTable + " (name,value) SELECT name,value FROM " + sourceTable + " WHERE substr(name,0,?)=?");
            deleteStmt = db.compileStatement("DELETE FROM " + sourceTable + " WHERE substr(name,0,?)=?");
            for (String prefix : prefixesToMove) {
                insertStmt.bindLong(1, prefix.length() + 1);
                insertStmt.bindString(2, prefix);
                insertStmt.execute();
                deleteStmt.bindLong(1, prefix.length() + 1);
                deleteStmt.bindString(2, prefix);
                deleteStmt.execute();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            if (insertStmt != null) {
                insertStmt.close();
            }
            if (deleteStmt != null) {
                deleteStmt.close();
            }
        }
    }

    private void upgradeLockPatternLocation(SQLiteDatabase db) {
        Cursor c = db.query("system", new String[]{"_id", "value"}, "name='lock_pattern'", null, null, null, null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            String lockPattern = c.getString(1);
            if (!TextUtils.isEmpty(lockPattern)) {
                try {
                    LockPatternUtils lpu = new LockPatternUtils(this.mContext);
                    List<LockPatternView.Cell> cellPattern = LockPatternUtils.stringToPattern(lockPattern);
                    lpu.saveLockPattern(cellPattern);
                } catch (IllegalArgumentException e) {
                }
            }
            c.close();
            db.delete("system", "name='lock_pattern'", null);
            return;
        }
        c.close();
    }

    private void upgradeScreenTimeoutFromNever(SQLiteDatabase db) {
        Cursor c = db.query("system", new String[]{"_id", "value"}, "name=? AND value=?", new String[]{"screen_off_timeout", "-1"}, null, null, null);
        SQLiteStatement stmt = null;
        if (c.getCount() > 0) {
            c.close();
            try {
                stmt = db.compileStatement("INSERT OR REPLACE INTO system(name,value) VALUES(?,?);");
                loadSetting(stmt, "screen_off_timeout", Integer.toString(1800000));
                if (stmt != null) {
                    return;
                } else {
                    return;
                }
            } finally {
                if (stmt != null) {
                    stmt.close();
                }
            }
        }
        c.close();
    }

    private void upgradeVibrateSettingFromNone(SQLiteDatabase db) {
        int vibrateSetting = getIntValueFromSystem(db, "vibrate_on", 0);
        if ((vibrateSetting & 3) == 0) {
            vibrateSetting = AudioService.getValueForVibrateSetting(0, 0, 2);
        }
        int vibrateSetting2 = AudioService.getValueForVibrateSetting(vibrateSetting, 1, vibrateSetting);
        SQLiteStatement stmt = null;
        try {
            stmt = db.compileStatement("INSERT OR REPLACE INTO system(name,value) VALUES(?,?);");
            loadSetting(stmt, "vibrate_on", Integer.valueOf(vibrateSetting2));
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    private void upgradeAutoBrightness(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            String value = this.mContext.getResources().getBoolean(R.bool.def_screen_brightness_automatic_mode) ? "1" : "0";
            db.execSQL("INSERT OR REPLACE INTO system(name,value) values('screen_brightness_mode','" + value + "');");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void loadBookmarks(SQLiteDatabase db) {
        String title;
        Intent intent;
        ActivityInfo info;
        ContentValues values = new ContentValues();
        PackageManager packageManager = this.mContext.getPackageManager();
        try {
            XmlResourceParser parser = this.mContext.getResources().getXml(R.xml.bookmarks);
            XmlUtils.beginDocument(parser, "bookmarks");
            int depth = parser.getDepth();
            while (true) {
                int type = parser.next();
                if ((type != 3 || parser.getDepth() > depth) && type != 1) {
                    if (type == 2) {
                        String name = parser.getName();
                        if ("bookmark".equals(name)) {
                            String pkg = parser.getAttributeValue(null, "package");
                            String cls = parser.getAttributeValue(null, "class");
                            String shortcutStr = parser.getAttributeValue(null, "shortcut");
                            String category = parser.getAttributeValue(null, "category");
                            int shortcutValue = shortcutStr.charAt(0);
                            if (TextUtils.isEmpty(shortcutStr)) {
                                Log.w("SettingsProvider", "Unable to get shortcut for: " + pkg + "/" + cls);
                            } else if (pkg != null && cls != null) {
                                ComponentName cn = new ComponentName(pkg, cls);
                                try {
                                    info = packageManager.getActivityInfo(cn, 0);
                                } catch (PackageManager.NameNotFoundException e) {
                                    String[] packages = packageManager.canonicalToCurrentPackageNames(new String[]{pkg});
                                    cn = new ComponentName(packages[0], cls);
                                    try {
                                        info = packageManager.getActivityInfo(cn, 0);
                                    } catch (PackageManager.NameNotFoundException e2) {
                                        Log.w("SettingsProvider", "Unable to add bookmark: " + pkg + "/" + cls, e);
                                    }
                                }
                                intent = new Intent("android.intent.action.MAIN", (Uri) null);
                                intent.addCategory("android.intent.category.LAUNCHER");
                                intent.setComponent(cn);
                                title = info.loadLabel(packageManager).toString();
                                intent.setFlags(268435456);
                                values.put("intent", intent.toUri(0));
                                values.put("title", title);
                                values.put("shortcut", Integer.valueOf(shortcutValue));
                                db.delete("bookmarks", "shortcut = ?", new String[]{Integer.toString(shortcutValue)});
                                db.insert("bookmarks", null, values);
                            } else if (category != null) {
                                intent = Intent.makeMainSelectorActivity("android.intent.action.MAIN", category);
                                title = "";
                                intent.setFlags(268435456);
                                values.put("intent", intent.toUri(0));
                                values.put("title", title);
                                values.put("shortcut", Integer.valueOf(shortcutValue));
                                db.delete("bookmarks", "shortcut = ?", new String[]{Integer.toString(shortcutValue)});
                                db.insert("bookmarks", null, values);
                            } else {
                                Log.w("SettingsProvider", "Unable to add bookmark for shortcut " + shortcutStr + ": missing package/class or category attributes");
                            }
                        } else {
                            return;
                        }
                    }
                } else {
                    return;
                }
            }
        } catch (IOException e3) {
            Log.w("SettingsProvider", "Got execption parsing bookmarks.", e3);
        } catch (XmlPullParserException e4) {
            Log.w("SettingsProvider", "Got execption parsing bookmarks.", e4);
        }
    }

    private void loadVolumeLevels(SQLiteDatabase db) {
        SQLiteStatement stmt = null;
        try {
            stmt = db.compileStatement("INSERT OR IGNORE INTO system(name,value) VALUES(?,?);");
            loadSetting(stmt, "volume_music", Integer.valueOf(AudioService.getDefaultStreamVolume(3)));
            loadSetting(stmt, "volume_ring", Integer.valueOf(AudioService.getDefaultStreamVolume(2)));
            loadSetting(stmt, "volume_system", Integer.valueOf(AudioService.getDefaultStreamVolume(1)));
            loadSetting(stmt, "volume_voice", Integer.valueOf(AudioService.getDefaultStreamVolume(0)));
            loadSetting(stmt, "volume_alarm", Integer.valueOf(AudioService.getDefaultStreamVolume(4)));
            loadSetting(stmt, "volume_notification", Integer.valueOf(AudioService.getDefaultStreamVolume(5)));
            loadSetting(stmt, "volume_bluetooth_sco", Integer.valueOf(AudioService.getDefaultStreamVolume(6)));
            int ringerModeAffectedStreams = 166;
            if (!this.mContext.getResources().getBoolean(android.R.^attr-private.externalRouteEnabledDrawable)) {
                ringerModeAffectedStreams = 166 | 8;
            }
            loadSetting(stmt, "mode_ringer_streams_affected", Integer.valueOf(ringerModeAffectedStreams));
            loadSetting(stmt, "mute_streams_affected", 46);
            loadVibrateWhenRingingSetting(db);
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    private void loadVibrateSetting(SQLiteDatabase db, boolean deleteOld) {
        if (deleteOld) {
            db.execSQL("DELETE FROM system WHERE name='vibrate_on'");
        }
        SQLiteStatement stmt = null;
        try {
            stmt = db.compileStatement("INSERT OR IGNORE INTO system(name,value) VALUES(?,?);");
            int vibrate = AudioService.getValueForVibrateSetting(0, 1, 2);
            loadSetting(stmt, "vibrate_on", Integer.valueOf(vibrate | AudioService.getValueForVibrateSetting(vibrate, 0, 2)));
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    private void loadVibrateWhenRingingSetting(SQLiteDatabase db) {
        int vibrateSetting = getIntValueFromSystem(db, "vibrate_on", 0);
        boolean vibrateWhenRinging = (vibrateSetting & 3) == 1;
        SQLiteStatement stmt = null;
        try {
            stmt = db.compileStatement("INSERT OR IGNORE INTO system(name,value) VALUES(?,?);");
            loadSetting(stmt, "vibrate_when_ringing", Integer.valueOf(vibrateWhenRinging ? 1 : 0));
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    private void loadSettings(SQLiteDatabase db) {
        loadSystemSettings(db);
        loadSecureSettings(db);
        if (this.mUserHandle == 0) {
            loadGlobalSettings(db);
        }
    }

    private void loadSystemSettings(SQLiteDatabase db) {
        SQLiteStatement stmt = null;
        try {
            stmt = db.compileStatement("INSERT OR IGNORE INTO system(name,value) VALUES(?,?);");
            loadBooleanSetting(stmt, "dim_screen", R.bool.def_dim_screen);
            loadIntegerSetting(stmt, "screen_off_timeout", R.integer.def_screen_off_timeout);
            loadIntegerSetting(stmt, "screen_dim_timeout", R.integer.def_screen_dim_timeout);
            loadSetting(stmt, "dtmf_tone_type", 0);
            loadSetting(stmt, "hearing_aid", 0);
            loadSetting(stmt, "tty_mode", 0);
            loadIntegerSetting(stmt, "screen_brightness", R.integer.def_screen_brightness);
            loadBooleanSetting(stmt, "screen_brightness_mode", R.bool.def_screen_brightness_automatic_mode);
            loadDefaultAnimationSettings(stmt);
            loadBooleanSetting(stmt, "accelerometer_rotation", R.bool.def_accelerometer_rotation);
            loadDefaultHapticSettings(stmt);
            loadBooleanSetting(stmt, "notification_light_pulse", R.bool.def_notification_pulse);
            loadUISoundEffectsSettings(stmt);
            loadIntegerSetting(stmt, "pointer_speed", R.integer.def_pointer_speed);
            loadIntegerSetting(stmt, "pen_mode", R.integer.def_pen_mode);
            loadBooleanSetting(stmt, "screen_capture_on", R.bool.def_screen_capture_on);
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    private void loadUISoundEffectsSettings(SQLiteStatement stmt) {
        loadBooleanSetting(stmt, "dtmf_tone", R.bool.def_dtmf_tones_enabled);
        loadBooleanSetting(stmt, "sound_effects_enabled", R.bool.def_sound_effects_enabled);
        loadBooleanSetting(stmt, "haptic_feedback_enabled", R.bool.def_haptic_feedback);
        loadIntegerSetting(stmt, "lockscreen_sounds_enabled", R.integer.def_lockscreen_sounds_enabled);
    }

    private void loadDefaultAnimationSettings(SQLiteStatement stmt) {
        loadFractionSetting(stmt, "window_animation_scale", R.fraction.def_window_animation_scale, 1);
        loadFractionSetting(stmt, "transition_animation_scale", R.fraction.def_window_transition_scale, 1);
    }

    private void loadDefaultHapticSettings(SQLiteStatement stmt) {
        loadBooleanSetting(stmt, "haptic_feedback_enabled", R.bool.def_haptic_feedback);
    }

    private void loadSecureSettings(SQLiteDatabase db) {
        SQLiteStatement stmt = null;
        try {
            stmt = db.compileStatement("INSERT OR IGNORE INTO secure(name,value) VALUES(?,?);");
            loadStringSetting(stmt, "location_providers_allowed", R.string.def_location_providers_allowed);
            String wifiWatchList = SystemProperties.get("ro.com.android.wifi-watchlist");
            if (!TextUtils.isEmpty(wifiWatchList)) {
                loadSetting(stmt, "wifi_watchdog_watch_list", wifiWatchList);
            }
            loadSetting(stmt, "mock_location", Integer.valueOf("1".equals(SystemProperties.get("ro.allow.mock.location")) ? 1 : 0));
            loadSecure35Settings(stmt);
            loadBooleanSetting(stmt, "mount_play_not_snd", R.bool.def_mount_play_notification_snd);
            loadBooleanSetting(stmt, "mount_ums_autostart", R.bool.def_mount_ums_autostart);
            loadBooleanSetting(stmt, "mount_ums_prompt", R.bool.def_mount_ums_prompt);
            loadBooleanSetting(stmt, "mount_ums_notify_enabled", R.bool.def_mount_ums_notify_enabled);
            loadBooleanSetting(stmt, "accessibility_script_injection", R.bool.def_accessibility_script_injection);
            loadStringSetting(stmt, "accessibility_web_content_key_bindings", R.string.def_accessibility_web_content_key_bindings);
            loadIntegerSetting(stmt, "long_press_timeout", R.integer.def_long_press_timeout_millis);
            loadBooleanSetting(stmt, "touch_exploration_enabled", R.bool.def_touch_exploration_enabled);
            loadBooleanSetting(stmt, "speak_password", R.bool.def_accessibility_speak_password);
            loadStringSetting(stmt, "accessibility_script_injection_url", R.string.def_accessibility_screen_reader_url);
            if (SystemProperties.getBoolean("ro.lockscreen.disable.default", false)) {
                loadSetting(stmt, "lockscreen.disabled", "1");
            } else {
                loadBooleanSetting(stmt, "lockscreen.disabled", R.bool.def_lockscreen_disabled);
            }
            loadBooleanSetting(stmt, "screensaver_enabled", android.R.^attr-private.glowDot);
            loadBooleanSetting(stmt, "screensaver_activate_on_dock", android.R.^attr-private.glyphDrawable);
            loadBooleanSetting(stmt, "screensaver_activate_on_sleep", android.R.^attr-private.glyphMap);
            loadStringSetting(stmt, "screensaver_components", android.R.string.config_systemSupervision);
            loadStringSetting(stmt, "screensaver_default_component", android.R.string.config_systemSupervision);
            loadBooleanSetting(stmt, "accessibility_display_magnification_enabled", R.bool.def_accessibility_display_magnification_enabled);
            loadFractionSetting(stmt, "accessibility_display_magnification_scale", R.fraction.def_accessibility_display_magnification_scale, 1);
            loadBooleanSetting(stmt, "accessibility_display_magnification_auto_update", R.bool.def_accessibility_display_magnification_auto_update);
            loadBooleanSetting(stmt, "user_setup_complete", R.bool.def_user_setup_complete);
            loadStringSetting(stmt, "immersive_mode_confirmations", R.string.def_immersive_mode_confirmations);
            loadBooleanSetting(stmt, "install_non_market_apps", R.bool.def_install_non_market_apps);
            loadBooleanSetting(stmt, "wake_gesture_enabled", R.bool.def_wake_gesture_enabled);
            loadIntegerSetting(stmt, "lock_screen_show_notifications", R.integer.def_lock_screen_show_notifications);
            loadBooleanSetting(stmt, "lock_screen_allow_private_notifications", R.bool.def_lock_screen_allow_private_notifications);
            loadIntegerSetting(stmt, "sleep_timeout", R.integer.def_sleep_timeout);
            loadBooleanSetting(stmt, "user_setup_device_owner", R.bool.def_user_setup_device_owner);
            loadStringSetting(stmt, "default_input_method", R.string.config_default_input_method);
            loadStringSetting(stmt, "enabled_input_methods", R.string.config_enable_input_method);
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    private void loadSecure35Settings(SQLiteStatement stmt) {
        loadBooleanSetting(stmt, "backup_enabled", R.bool.def_backup_enabled);
        loadStringSetting(stmt, "backup_transport", R.string.def_backup_transport);
    }

    private void loadGlobalSettings(SQLiteDatabase db) {
        SQLiteStatement stmt = null;
        try {
            stmt = db.compileStatement("INSERT OR IGNORE INTO global(name,value) VALUES(?,?);");
            loadBooleanSetting(stmt, "airplane_mode_on", R.bool.def_airplane_mode_on);
            loadSetting(stmt, "enable_sim1", "1");
            loadSetting(stmt, "enable_sim2", "1");
            loadBooleanSetting(stmt, "theater_mode_on", R.bool.def_theater_mode_on);
            loadStringSetting(stmt, "airplane_mode_radios", R.string.def_airplane_mode_radios);
            loadStringSetting(stmt, "airplane_mode_toggleable_radios", R.string.airplane_mode_toggleable_radios);
            loadBooleanSetting(stmt, "assisted_gps_enabled", R.bool.assisted_gps_enabled);
            loadBooleanSetting(stmt, "auto_time", R.bool.def_auto_time);
            loadBooleanSetting(stmt, "auto_time_zone", R.bool.def_auto_time_zone);
            loadSetting(stmt, "stay_on_while_plugged_in", Integer.valueOf(("1".equals(SystemProperties.get("ro.kernel.qemu")) || this.mContext.getResources().getBoolean(R.bool.def_stay_on_while_plugged_in)) ? 1 : 0));
            loadIntegerSetting(stmt, "wifi_sleep_policy", R.integer.def_wifi_sleep_policy);
            loadSetting(stmt, "mode_ringer", 2);
            loadBooleanSetting(stmt, "package_verifier_enable", R.bool.def_package_verifier_enable);
            loadBooleanSetting(stmt, "wifi_on", R.bool.def_wifi_on);
            loadBooleanSetting(stmt, "wifi_networks_available_notification_on", R.bool.def_networks_available_notification_on);
            loadBooleanSetting(stmt, "bluetooth_on", R.bool.def_bluetooth_on);
            loadSetting(stmt, "cdma_cell_broadcast_sms", 1);
            loadSetting(stmt, "data_roaming", Integer.valueOf("true".equalsIgnoreCase(SystemProperties.get("ro.com.android.dataroaming", "false")) ? 1 : 0));
            loadBooleanSetting(stmt, "device_provisioned", R.bool.def_device_provisioned);
            int maxBytes = this.mContext.getResources().getInteger(R.integer.def_download_manager_max_bytes_over_mobile);
            if (maxBytes > 0) {
                loadSetting(stmt, "download_manager_max_bytes_over_mobile", Integer.toString(maxBytes));
            }
            int recommendedMaxBytes = this.mContext.getResources().getInteger(R.integer.def_download_manager_recommended_max_bytes_over_mobile);
            if (recommendedMaxBytes > 0) {
                loadSetting(stmt, "download_manager_recommended_max_bytes_over_mobile", Integer.toString(recommendedMaxBytes));
            }
            loadSetting(stmt, "mobile_data", Integer.valueOf("true".equalsIgnoreCase(SystemProperties.get("ro.com.android.mobiledata", "true")) ? 1 : 0));
            loadBooleanSetting(stmt, "netstats_enabled", R.bool.def_netstats_enabled);
            loadBooleanSetting(stmt, "usb_mass_storage_enabled", R.bool.def_usb_mass_storage_enabled);
            loadIntegerSetting(stmt, "wifi_max_dhcp_retry_count", R.integer.def_max_dhcp_retries);
            loadBooleanSetting(stmt, "wifi_display_on", R.bool.def_wifi_display_on);
            loadStringSetting(stmt, "lock_sound", R.string.def_lock_sound);
            loadStringSetting(stmt, "unlock_sound", R.string.def_unlock_sound);
            loadStringSetting(stmt, "trusted_sound", R.string.def_trusted_sound);
            loadIntegerSetting(stmt, "power_sounds_enabled", R.integer.def_power_sounds_enabled);
            loadStringSetting(stmt, "low_battery_sound", R.string.def_low_battery_sound);
            loadIntegerSetting(stmt, "dock_sounds_enabled", R.integer.def_dock_sounds_enabled);
            loadStringSetting(stmt, "desk_dock_sound", R.string.def_desk_dock_sound);
            loadStringSetting(stmt, "desk_undock_sound", R.string.def_desk_undock_sound);
            loadStringSetting(stmt, "car_dock_sound", R.string.def_car_dock_sound);
            loadStringSetting(stmt, "car_undock_sound", R.string.def_car_undock_sound);
            loadStringSetting(stmt, "wireless_charging_started_sound", R.string.def_wireless_charging_started_sound);
            loadIntegerSetting(stmt, "dock_audio_media_enabled", R.integer.def_dock_audio_media_enabled);
            loadSetting(stmt, "set_install_location", 0);
            loadSetting(stmt, "default_install_location", 0);
            loadSetting(stmt, "emergency_tone", 0);
            loadSetting(stmt, "call_auto_retry", 0);
            loadSetting(stmt, "hide_carrier_network_settings", 0);
            int type = RILConstants.PREFERRED_NETWORK_MODE;
            loadSetting(stmt, "preferred_network_mode", Integer.valueOf(type));
            int type2 = SystemProperties.getInt("ro.telephony.default_cdma_sub", 1);
            loadSetting(stmt, "subscription_mode", Integer.valueOf(type2));
            loadIntegerSetting(stmt, "low_battery_sound_timeout", R.integer.def_low_battery_sound_timeout);
            loadIntegerSetting(stmt, "wifi_scan_always_enabled", R.integer.def_wifi_scan_always_available);
            loadIntegerSetting(stmt, "heads_up_notifications_enabled", R.integer.def_heads_up_enabled);
            loadSetting(stmt, "device_name", getDefaultDeviceName());
            loadBooleanSetting(stmt, "guest_user_enabled", R.bool.def_guest_user_enabled);
            loadSetting(stmt, "volte_vt_enabled", 1);
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    private void loadSetting(SQLiteStatement stmt, String key, Object value) {
        stmt.bindString(1, key);
        stmt.bindString(2, value.toString());
        stmt.execute();
    }

    private void loadStringSetting(SQLiteStatement stmt, String key, int resid) {
        loadSetting(stmt, key, this.mContext.getResources().getString(resid));
    }

    private void loadBooleanSetting(SQLiteStatement stmt, String key, int resid) {
        loadSetting(stmt, key, this.mContext.getResources().getBoolean(resid) ? "1" : "0");
    }

    private void loadIntegerSetting(SQLiteStatement stmt, String key, int resid) {
        loadSetting(stmt, key, Integer.toString(this.mContext.getResources().getInteger(resid)));
    }

    private void loadFractionSetting(SQLiteStatement stmt, String key, int resid, int base) {
        loadSetting(stmt, key, Float.toString(this.mContext.getResources().getFraction(resid, base, base)));
    }

    private int getIntValueFromSystem(SQLiteDatabase db, String name, int defaultValue) {
        return getIntValueFromTable(db, "system", name, defaultValue);
    }

    private int getIntValueFromTable(SQLiteDatabase db, String table, String name, int defaultValue) {
        String value = getStringValueFromTable(db, table, name, null);
        if (value == null) {
            return defaultValue;
        }
        int defaultValue2 = Integer.parseInt(value);
        return defaultValue2;
    }

    private String getStringValueFromTable(SQLiteDatabase db, String table, String name, String defaultValue) {
        Cursor c = null;
        try {
            c = db.query(table, new String[]{"value"}, "name='" + name + "'", null, null, null, null);
            if (c != null && c.moveToFirst()) {
                String val = c.getString(0);
                if (val != null) {
                    defaultValue = val;
                }
            } else if (c != null) {
                c.close();
            }
            return defaultValue;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private String getOldDefaultDeviceName() {
        return this.mContext.getResources().getString(R.string.def_device_name, Build.MANUFACTURER, Build.MODEL);
    }

    private String getDefaultDeviceName() {
        return this.mContext.getResources().getString(R.string.def_device_name_simple, Build.MODEL);
    }
}
