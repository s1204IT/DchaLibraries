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
import android.media.AudioSystem;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.RILConstants;
import com.android.internal.util.XmlUtils;
import com.android.internal.widget.LockPatternUtils;
import com.mediatek.providers.utils.ProvidersUtils;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.xmlpull.v1.XmlPullParserException;

@Deprecated
class DatabaseHelper extends SQLiteOpenHelper {
    private static final HashSet<String> mValidTables = new HashSet<>();
    private Context mContext;
    private int mUserHandle;
    private ProvidersUtils mUtils;

    static {
        mValidTables.add("system");
        mValidTables.add("secure");
        mValidTables.add("global");
        mValidTables.add("bluetooth_devices");
        mValidTables.add("bookmarks");
        mValidTables.add("favorites");
        mValidTables.add("old_favorites");
        mValidTables.add("android_metadata");
    }

    static String dbNameForUser(int i) {
        if (i == 0) {
            return "settings.db";
        }
        File file = new File(Environment.getUserSystemDirectory(i), "settings.db");
        if (!file.exists()) {
            Log.i("SettingsProvider", "No previous database file exists - running in in-memory mode");
            return null;
        }
        return file.getPath();
    }

    public DatabaseHelper(Context context, int i) {
        super(context, dbNameForUser(i), (SQLiteDatabase.CursorFactory) null, 118);
        this.mContext = context;
        this.mUserHandle = i;
        if (this.mUtils == null) {
            this.mUtils = new ProvidersUtils(this.mContext);
        }
    }

    public static boolean isValidTable(String str) {
        return mValidTables.contains(str);
    }

    private boolean isInMemory() {
        return getDatabaseName() == null;
    }

    public void dropDatabase() {
        close();
        if (isInMemory()) {
            return;
        }
        File databasePath = this.mContext.getDatabasePath(getDatabaseName());
        if (databasePath.exists()) {
            SQLiteDatabase.deleteDatabase(databasePath);
            Log.w("SettingsProvider", "dropDatabase, name = " + getDatabaseName());
        }
    }

    private void createSecureTable(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE secure (_id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT UNIQUE ON CONFLICT REPLACE,value TEXT);");
        sQLiteDatabase.execSQL("CREATE INDEX secureIndex1 ON secure (name);");
    }

    private void createGlobalTable(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE global (_id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT UNIQUE ON CONFLICT REPLACE,value TEXT);");
        sQLiteDatabase.execSQL("CREATE INDEX globalIndex1 ON global (name);");
    }

    @Override
    public void onCreate(SQLiteDatabase sQLiteDatabase) throws Throwable {
        sQLiteDatabase.execSQL("CREATE TABLE system (_id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT UNIQUE ON CONFLICT REPLACE,value TEXT);");
        sQLiteDatabase.execSQL("CREATE INDEX systemIndex1 ON system (name);");
        createSecureTable(sQLiteDatabase);
        if (this.mUserHandle == 0) {
            createGlobalTable(sQLiteDatabase);
        }
        sQLiteDatabase.execSQL("CREATE TABLE bluetooth_devices (_id INTEGER PRIMARY KEY,name TEXT,addr TEXT,channel INTEGER,type INTEGER);");
        sQLiteDatabase.execSQL("CREATE TABLE bookmarks (_id INTEGER PRIMARY KEY,title TEXT,folder TEXT,intent TEXT,shortcut INTEGER,ordering INTEGER);");
        sQLiteDatabase.execSQL("CREATE INDEX bookmarksIndex1 ON bookmarks (folder);");
        sQLiteDatabase.execSQL("CREATE INDEX bookmarksIndex2 ON bookmarks (shortcut);");
        boolean zIsOnlyCoreApps = false;
        try {
            zIsOnlyCoreApps = IPackageManager.Stub.asInterface(ServiceManager.getService("package")).isOnlyCoreApps();
        } catch (RemoteException e) {
        }
        if (!zIsOnlyCoreApps) {
            loadBookmarks(sQLiteDatabase);
        }
        loadVolumeLevels(sQLiteDatabase);
        loadSettings(sQLiteDatabase);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) throws Throwable {
        int i3;
        SQLiteStatement sQLiteStatementCompileStatement;
        SQLiteStatement sQLiteStatementCompileStatement2;
        SQLiteStatement sQLiteStatementCompileStatement3;
        SQLiteStatement sQLiteStatementCompileStatement4;
        SQLiteStatement sQLiteStatementCompileStatement5;
        SQLiteStatement sQLiteStatementCompileStatement6;
        SQLiteStatement sQLiteStatementCompileStatement7;
        SQLiteStatement sQLiteStatementCompileStatement8;
        SQLiteStatement sQLiteStatementCompileStatement9;
        SQLiteStatement sQLiteStatementCompileStatement10;
        SQLiteStatement sQLiteStatementCompileStatement11;
        int intValueFromTable;
        SQLiteStatement sQLiteStatementCompileStatement12;
        SQLiteStatement sQLiteStatementCompileStatement13;
        SQLiteStatement sQLiteStatementCompileStatement14;
        SQLiteStatement sQLiteStatementCompileStatement15;
        SQLiteStatement sQLiteStatementCompileStatement16;
        SQLiteStatement sQLiteStatementCompileStatement17;
        SQLiteStatement sQLiteStatementCompileStatement18;
        SQLiteStatement sQLiteStatementCompileStatement19;
        SQLiteStatement sQLiteStatementCompileStatement20;
        SQLiteStatement sQLiteStatementCompileStatement21;
        SQLiteStatement sQLiteStatementCompileStatement22;
        SQLiteStatement sQLiteStatementCompileStatement23;
        SQLiteStatement sQLiteStatementCompileStatement24;
        SQLiteStatement sQLiteStatementCompileStatement25;
        SQLiteStatement sQLiteStatementCompileStatement26;
        SQLiteStatement sQLiteStatementCompileStatement27;
        SQLiteStatement sQLiteStatementCompileStatement28;
        SQLiteStatement sQLiteStatementCompileStatement29;
        SQLiteStatement sQLiteStatementCompileStatement30;
        SQLiteStatement sQLiteStatementCompileStatement31;
        SQLiteStatement sQLiteStatementCompileStatement32;
        Log.w("SettingsProvider", "Upgrading settings database from version " + i + " to " + i2);
        if (i == 20) {
            loadVibrateSetting(sQLiteDatabase, true);
            i3 = 21;
        } else {
            i3 = i;
        }
        if (i3 < 22) {
            upgradeLockPatternLocation(sQLiteDatabase);
            i3 = 22;
        }
        if (i3 < 23) {
            sQLiteDatabase.execSQL("UPDATE favorites SET iconResource=0 WHERE iconType=0");
            i3 = 23;
        }
        if (i3 == 23) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteDatabase.execSQL("ALTER TABLE favorites ADD spanX INTEGER");
                sQLiteDatabase.execSQL("ALTER TABLE favorites ADD spanY INTEGER");
                sQLiteDatabase.execSQL("UPDATE favorites SET spanX=1, spanY=1 WHERE itemType<=0");
                sQLiteDatabase.execSQL("UPDATE favorites SET spanX=2, spanY=2 WHERE itemType=1000 or itemType=1002");
                sQLiteDatabase.execSQL("UPDATE favorites SET spanX=4, spanY=1 WHERE itemType=1001");
                sQLiteDatabase.setTransactionSuccessful();
                sQLiteDatabase.endTransaction();
                i3 = 24;
            } finally {
            }
        }
        if (i3 == 24) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteDatabase.execSQL("DELETE FROM system WHERE name='network_preference'");
                sQLiteDatabase.execSQL("INSERT INTO system ('name', 'value') values ('network_preference', '1')");
                sQLiteDatabase.setTransactionSuccessful();
                sQLiteDatabase.endTransaction();
                i3 = 25;
            } finally {
            }
        }
        if (i3 == 25) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteDatabase.execSQL("ALTER TABLE favorites ADD uri TEXT");
                sQLiteDatabase.execSQL("ALTER TABLE favorites ADD displayMode INTEGER");
                sQLiteDatabase.setTransactionSuccessful();
                sQLiteDatabase.endTransaction();
                i3 = 26;
            } finally {
            }
        }
        if (i3 == 26) {
            sQLiteDatabase.beginTransaction();
            try {
                createSecureTable(sQLiteDatabase);
                sQLiteDatabase.setTransactionSuccessful();
                sQLiteDatabase.endTransaction();
                i3 = 27;
            } finally {
            }
        }
        if (i3 == 27) {
            moveSettingsToNewTable(sQLiteDatabase, "system", "secure", new String[]{"adb_enabled", "android_id", "bluetooth_on", "data_roaming", "device_provisioned", "http_proxy", "install_non_market_apps", "location_providers_allowed", "logging_id", "network_preference", "parental_control_enabled", "parental_control_last_update", "parental_control_redirect_url", "settings_classname", "usb_mass_storage_enabled", "use_google_mail", "wifi_networks_available_notification_on", "wifi_networks_available_repeat_delay", "wifi_num_open_networks_kept", "wifi_on", "wifi_watchdog_acceptable_packet_loss_percentage", "wifi_watchdog_ap_count", "wifi_watchdog_background_check_delay_ms", "wifi_watchdog_background_check_enabled", "wifi_watchdog_background_check_timeout_ms", "wifi_watchdog_initial_ignored_ping_count", "wifi_watchdog_max_ap_checks", "wifi_watchdog_on", "wifi_watchdog_ping_count", "wifi_watchdog_ping_delay_ms", "wifi_watchdog_ping_timeout_ms"}, false);
            i3 = 28;
        }
        if (i3 == 28 || i3 == 29) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteDatabase.execSQL("DELETE FROM system WHERE name='mode_ringer_streams_affected'");
                sQLiteDatabase.execSQL("INSERT INTO system ('name', 'value') values ('mode_ringer_streams_affected', '" + String.valueOf(38) + "')");
                sQLiteDatabase.setTransactionSuccessful();
                sQLiteDatabase.endTransaction();
                i3 = 30;
            } finally {
            }
        }
        if (i3 == 30) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteDatabase.execSQL("UPDATE bookmarks SET folder = '@quicklaunch'");
                sQLiteDatabase.execSQL("UPDATE bookmarks SET title = ''");
                sQLiteDatabase.setTransactionSuccessful();
                sQLiteDatabase.endTransaction();
                i3 = 31;
            } finally {
            }
        }
        SQLiteStatement sQLiteStatement = null;
        SQLiteStatement sQLiteStatement2 = null;
        SQLiteStatement sQLiteStatement3 = null;
        SQLiteStatement sQLiteStatement4 = null;
        SQLiteStatement sQLiteStatement5 = null;
        SQLiteStatement sQLiteStatement6 = null;
        SQLiteStatement sQLiteStatement7 = null;
        SQLiteStatement sQLiteStatement8 = null;
        Cursor cursor = null;
        SQLiteStatement sQLiteStatement9 = null;
        if (i3 == 31) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteDatabase.execSQL("DELETE FROM system WHERE name='window_animation_scale'");
                sQLiteDatabase.execSQL("DELETE FROM system WHERE name='transition_animation_scale'");
                SQLiteStatement sQLiteStatementCompileStatement33 = sQLiteDatabase.compileStatement("INSERT INTO system(name,value) VALUES(?,?);");
                try {
                    loadDefaultAnimationSettings(sQLiteStatementCompileStatement33);
                    sQLiteDatabase.setTransactionSuccessful();
                    if (sQLiteStatementCompileStatement33 != null) {
                        sQLiteStatementCompileStatement33.close();
                    }
                    i3 = 32;
                } catch (Throwable th) {
                    th = th;
                    sQLiteStatement2 = sQLiteStatementCompileStatement33;
                    if (sQLiteStatement2 != null) {
                        sQLiteStatement2.close();
                    }
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
        if (i3 == 32) {
            i3 = 33;
        }
        if (i3 == 33) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteDatabase.execSQL("INSERT INTO system(name,value) values('zoom','2');");
                sQLiteDatabase.setTransactionSuccessful();
                sQLiteDatabase.endTransaction();
                i3 = 34;
            } finally {
            }
        }
        if (i3 == 34) {
            sQLiteDatabase.beginTransaction();
            try {
                SQLiteStatement sQLiteStatementCompileStatement34 = sQLiteDatabase.compileStatement("INSERT OR IGNORE INTO secure(name,value) VALUES(?,?);");
                try {
                    loadSecure35Settings(sQLiteStatementCompileStatement34);
                    sQLiteDatabase.setTransactionSuccessful();
                    if (sQLiteStatementCompileStatement34 != null) {
                        sQLiteStatementCompileStatement34.close();
                    }
                    i3 = 35;
                } catch (Throwable th3) {
                    th = th3;
                    sQLiteStatement3 = sQLiteStatementCompileStatement34;
                    if (sQLiteStatement3 != null) {
                        sQLiteStatement3.close();
                    }
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
            }
        }
        if (i3 == 35) {
            i3 = 36;
        }
        if (i3 == 36) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteDatabase.execSQL("DELETE FROM system WHERE name='mode_ringer_streams_affected'");
                sQLiteDatabase.execSQL("INSERT INTO system ('name', 'value') values ('mode_ringer_streams_affected', '" + String.valueOf(166) + "')");
                sQLiteDatabase.setTransactionSuccessful();
                sQLiteDatabase.endTransaction();
                i3 = 37;
            } finally {
            }
        }
        if (i3 == 37) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteStatementCompileStatement32 = sQLiteDatabase.compileStatement("INSERT OR IGNORE INTO system(name,value) VALUES(?,?);");
                try {
                    loadStringSetting(sQLiteStatementCompileStatement32, "airplane_mode_toggleable_radios", R.string.airplane_mode_toggleable_radios);
                    sQLiteDatabase.setTransactionSuccessful();
                    if (sQLiteStatementCompileStatement32 != null) {
                        sQLiteStatementCompileStatement32.close();
                    }
                    i3 = 38;
                } catch (Throwable th5) {
                    th = th5;
                    if (sQLiteStatementCompileStatement32 != null) {
                        sQLiteStatementCompileStatement32.close();
                    }
                    throw th;
                }
            } catch (Throwable th6) {
                th = th6;
                sQLiteStatementCompileStatement32 = null;
            }
        }
        if (i3 == 38) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteDatabase.execSQL("INSERT OR IGNORE INTO secure(name,value) values('assisted_gps_enabled','" + (this.mContext.getResources().getBoolean(R.bool.assisted_gps_enabled) ? "1" : "0") + "');");
                sQLiteDatabase.setTransactionSuccessful();
                sQLiteDatabase.endTransaction();
                i3 = 39;
            } finally {
            }
        }
        if (i3 == 39) {
            upgradeAutoBrightness(sQLiteDatabase);
            i3 = 40;
        }
        if (i3 == 40) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteDatabase.execSQL("DELETE FROM system WHERE name='window_animation_scale'");
                sQLiteDatabase.execSQL("DELETE FROM system WHERE name='transition_animation_scale'");
                SQLiteStatement sQLiteStatementCompileStatement35 = sQLiteDatabase.compileStatement("INSERT INTO system(name,value) VALUES(?,?);");
                try {
                    loadDefaultAnimationSettings(sQLiteStatementCompileStatement35);
                    sQLiteDatabase.setTransactionSuccessful();
                    if (sQLiteStatementCompileStatement35 != null) {
                        sQLiteStatementCompileStatement35.close();
                    }
                    i3 = 41;
                } catch (Throwable th7) {
                    th = th7;
                    sQLiteStatement4 = sQLiteStatementCompileStatement35;
                    if (sQLiteStatement4 != null) {
                        sQLiteStatement4.close();
                    }
                    throw th;
                }
            } catch (Throwable th8) {
                th = th8;
            }
        }
        if (i3 == 41) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteDatabase.execSQL("DELETE FROM system WHERE name='haptic_feedback_enabled'");
                SQLiteStatement sQLiteStatementCompileStatement36 = sQLiteDatabase.compileStatement("INSERT INTO system(name,value) VALUES(?,?);");
                try {
                    loadDefaultHapticSettings(sQLiteStatementCompileStatement36);
                    sQLiteDatabase.setTransactionSuccessful();
                    if (sQLiteStatementCompileStatement36 != null) {
                        sQLiteStatementCompileStatement36.close();
                    }
                    i3 = 42;
                } catch (Throwable th9) {
                    th = th9;
                    sQLiteStatement5 = sQLiteStatementCompileStatement36;
                    if (sQLiteStatement5 != null) {
                        sQLiteStatement5.close();
                    }
                    throw th;
                }
            } catch (Throwable th10) {
                th = th10;
            }
        }
        if (i3 == 42) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteStatementCompileStatement31 = sQLiteDatabase.compileStatement("INSERT INTO system(name,value) VALUES(?,?);");
                try {
                    loadBooleanSetting(sQLiteStatementCompileStatement31, "notification_light_pulse", R.bool.def_notification_pulse);
                    sQLiteDatabase.setTransactionSuccessful();
                    if (sQLiteStatementCompileStatement31 != null) {
                        sQLiteStatementCompileStatement31.close();
                    }
                    i3 = 43;
                } catch (Throwable th11) {
                    th = th11;
                    if (sQLiteStatementCompileStatement31 != null) {
                        sQLiteStatementCompileStatement31.close();
                    }
                    throw th;
                }
            } catch (Throwable th12) {
                th = th12;
                sQLiteStatementCompileStatement31 = null;
            }
        }
        if (i3 == 43) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteStatementCompileStatement30 = sQLiteDatabase.compileStatement("INSERT OR IGNORE INTO system(name,value) VALUES(?,?);");
                try {
                    loadSetting(sQLiteStatementCompileStatement30, "volume_bluetooth_sco", Integer.valueOf(AudioSystem.getDefaultStreamVolume(6)));
                    sQLiteDatabase.setTransactionSuccessful();
                    if (sQLiteStatementCompileStatement30 != null) {
                        sQLiteStatementCompileStatement30.close();
                    }
                    i3 = 44;
                } catch (Throwable th13) {
                    th = th13;
                    if (sQLiteStatementCompileStatement30 != null) {
                        sQLiteStatementCompileStatement30.close();
                    }
                    throw th;
                }
            } catch (Throwable th14) {
                th = th14;
                sQLiteStatementCompileStatement30 = null;
            }
        }
        if (i3 == 44) {
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS gservices");
            sQLiteDatabase.execSQL("DROP INDEX IF EXISTS gservicesIndex1");
            i3 = 45;
        }
        if (i3 == 45) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteDatabase.execSQL("INSERT INTO secure(name,value) values('mount_play_not_snd','1');");
                sQLiteDatabase.execSQL("INSERT INTO secure(name,value) values('mount_ums_autostart','0');");
                sQLiteDatabase.execSQL("INSERT INTO secure(name,value) values('mount_ums_prompt','1');");
                sQLiteDatabase.execSQL("INSERT INTO secure(name,value) values('mount_ums_notify_enabled','1');");
                sQLiteDatabase.setTransactionSuccessful();
                sQLiteDatabase.endTransaction();
                i3 = 46;
            } finally {
            }
        }
        if (i3 == 46) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteDatabase.execSQL("DELETE FROM system WHERE name='lockscreen.password_type';");
                sQLiteDatabase.setTransactionSuccessful();
                sQLiteDatabase.endTransaction();
                i3 = 47;
            } finally {
            }
        }
        if (i3 == 47) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteDatabase.execSQL("DELETE FROM system WHERE name='lockscreen.password_type';");
                sQLiteDatabase.setTransactionSuccessful();
                sQLiteDatabase.endTransaction();
                i3 = 48;
            } finally {
            }
        }
        if (i3 == 48) {
            i3 = 49;
        }
        if (i3 == 49) {
            sQLiteDatabase.beginTransaction();
            try {
                SQLiteStatement sQLiteStatementCompileStatement37 = sQLiteDatabase.compileStatement("INSERT INTO system(name,value) VALUES(?,?);");
                try {
                    loadUISoundEffectsSettings(sQLiteStatementCompileStatement37);
                    sQLiteDatabase.setTransactionSuccessful();
                    if (sQLiteStatementCompileStatement37 != null) {
                        sQLiteStatementCompileStatement37.close();
                    }
                    i3 = 50;
                } catch (Throwable th15) {
                    th = th15;
                    sQLiteStatement6 = sQLiteStatementCompileStatement37;
                    if (sQLiteStatement6 != null) {
                        sQLiteStatement6.close();
                    }
                    throw th;
                }
            } catch (Throwable th16) {
                th = th16;
            }
        }
        if (i3 == 50) {
            i3 = 51;
        }
        if (i3 == 51) {
            moveSettingsToNewTable(sQLiteDatabase, "system", "secure", new String[]{"lock_pattern_autolock", "lock_pattern_visible_pattern", "lock_pattern_tactile_feedback_enabled", "lockscreen.password_type", "lockscreen.lockoutattemptdeadline", "lockscreen.patterneverchosen", "lock_pattern_autolock", "lockscreen.lockedoutpermanently", "lockscreen.password_salt"}, false);
            i3 = 52;
        }
        if (i3 == 52) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteStatementCompileStatement29 = sQLiteDatabase.compileStatement("INSERT INTO system(name,value) VALUES(?,?);");
                try {
                    loadBooleanSetting(sQLiteStatementCompileStatement29, "vibrate_in_silent", R.bool.def_vibrate_in_silent);
                    sQLiteDatabase.setTransactionSuccessful();
                    if (sQLiteStatementCompileStatement29 != null) {
                        sQLiteStatementCompileStatement29.close();
                    }
                    i3 = 53;
                } catch (Throwable th17) {
                    th = th17;
                    if (sQLiteStatementCompileStatement29 != null) {
                        sQLiteStatementCompileStatement29.close();
                    }
                    throw th;
                }
            } catch (Throwable th18) {
                th = th18;
                sQLiteStatementCompileStatement29 = null;
            }
        }
        if (i3 == 53) {
            i3 = 54;
        }
        if (i3 == 54) {
            sQLiteDatabase.beginTransaction();
            try {
                upgradeScreenTimeoutFromNever(sQLiteDatabase);
                sQLiteDatabase.setTransactionSuccessful();
                sQLiteDatabase.endTransaction();
                i3 = 55;
            } finally {
            }
        }
        if (i3 == 55) {
            moveSettingsToNewTable(sQLiteDatabase, "system", "secure", new String[]{"set_install_location", "default_install_location"}, false);
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteStatementCompileStatement28 = sQLiteDatabase.compileStatement("INSERT INTO system(name,value) VALUES(?,?);");
                try {
                    loadSetting(sQLiteStatementCompileStatement28, "set_install_location", 0);
                    loadSetting(sQLiteStatementCompileStatement28, "default_install_location", 0);
                    sQLiteDatabase.setTransactionSuccessful();
                    if (sQLiteStatementCompileStatement28 != null) {
                        sQLiteStatementCompileStatement28.close();
                    }
                    i3 = 56;
                } catch (Throwable th19) {
                    th = th19;
                    if (sQLiteStatementCompileStatement28 != null) {
                        sQLiteStatementCompileStatement28.close();
                    }
                    throw th;
                }
            } catch (Throwable th20) {
                th = th20;
                sQLiteStatementCompileStatement28 = null;
            }
        }
        if (i3 == 56) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteDatabase.execSQL("DELETE FROM system WHERE name='airplane_mode_toggleable_radios'");
                SQLiteStatement sQLiteStatementCompileStatement38 = sQLiteDatabase.compileStatement("INSERT OR IGNORE INTO system(name,value) VALUES(?,?);");
                try {
                    loadStringSetting(sQLiteStatementCompileStatement38, "airplane_mode_toggleable_radios", R.string.airplane_mode_toggleable_radios);
                    sQLiteDatabase.setTransactionSuccessful();
                    if (sQLiteStatementCompileStatement38 != null) {
                        sQLiteStatementCompileStatement38.close();
                    }
                    i3 = 57;
                } catch (Throwable th21) {
                    th = th21;
                    sQLiteStatement7 = sQLiteStatementCompileStatement38;
                    if (sQLiteStatement7 != null) {
                        sQLiteStatement7.close();
                    }
                    throw th;
                }
            } catch (Throwable th22) {
                th = th22;
            }
        }
        if (i3 == 57) {
            i3 = 58;
        }
        if (i3 == 58) {
            int intValueFromSystem = getIntValueFromSystem(sQLiteDatabase, "auto_time", 0);
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteStatementCompileStatement27 = sQLiteDatabase.compileStatement("INSERT INTO system(name,value) VALUES(?,?);");
                try {
                    loadSetting(sQLiteStatementCompileStatement27, "auto_time_zone", Integer.valueOf(intValueFromSystem));
                    sQLiteDatabase.setTransactionSuccessful();
                    if (sQLiteStatementCompileStatement27 != null) {
                        sQLiteStatementCompileStatement27.close();
                    }
                    i3 = 59;
                } catch (Throwable th23) {
                    th = th23;
                    if (sQLiteStatementCompileStatement27 != null) {
                        sQLiteStatementCompileStatement27.close();
                    }
                    throw th;
                }
            } catch (Throwable th24) {
                th = th24;
                sQLiteStatementCompileStatement27 = null;
            }
        }
        if (i3 == 59) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteStatementCompileStatement26 = sQLiteDatabase.compileStatement("INSERT INTO system(name,value) VALUES(?,?);");
                try {
                    loadBooleanSetting(sQLiteStatementCompileStatement26, "user_rotation", R.integer.def_user_rotation);
                    sQLiteDatabase.setTransactionSuccessful();
                    if (sQLiteStatementCompileStatement26 != null) {
                        sQLiteStatementCompileStatement26.close();
                    }
                    i3 = 60;
                } catch (Throwable th25) {
                    th = th25;
                    if (sQLiteStatementCompileStatement26 != null) {
                        sQLiteStatementCompileStatement26.close();
                    }
                    throw th;
                }
            } catch (Throwable th26) {
                th = th26;
                sQLiteStatementCompileStatement26 = null;
            }
        }
        if (i3 == 60) {
            i3 = 61;
        }
        if (i3 == 61) {
            i3 = 62;
        }
        if (i3 == 62) {
            i3 = 63;
        }
        if (i3 == 63) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteDatabase.execSQL("DELETE FROM system WHERE name='mode_ringer_streams_affected'");
                sQLiteDatabase.execSQL("INSERT INTO system ('name', 'value') values ('mode_ringer_streams_affected', '" + String.valueOf(174) + "')");
                sQLiteDatabase.setTransactionSuccessful();
                sQLiteDatabase.endTransaction();
                i3 = 64;
            } finally {
            }
        }
        if (i3 == 64) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteStatementCompileStatement25 = sQLiteDatabase.compileStatement("INSERT INTO secure(name,value) VALUES(?,?);");
                try {
                    loadIntegerSetting(sQLiteStatementCompileStatement25, "long_press_timeout", R.integer.def_long_press_timeout_millis);
                    sQLiteStatementCompileStatement25.close();
                    sQLiteDatabase.setTransactionSuccessful();
                    if (sQLiteStatementCompileStatement25 != null) {
                        sQLiteStatementCompileStatement25.close();
                    }
                    i3 = 65;
                } catch (Throwable th27) {
                    th = th27;
                    if (sQLiteStatementCompileStatement25 != null) {
                        sQLiteStatementCompileStatement25.close();
                    }
                    throw th;
                }
            } catch (Throwable th28) {
                th = th28;
                sQLiteStatementCompileStatement25 = null;
            }
        }
        if (i3 == 65) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteDatabase.execSQL("DELETE FROM system WHERE name='window_animation_scale'");
                sQLiteDatabase.execSQL("DELETE FROM system WHERE name='transition_animation_scale'");
                SQLiteStatement sQLiteStatementCompileStatement39 = sQLiteDatabase.compileStatement("INSERT INTO system(name,value) VALUES(?,?);");
                try {
                    loadDefaultAnimationSettings(sQLiteStatementCompileStatement39);
                    sQLiteDatabase.setTransactionSuccessful();
                    if (sQLiteStatementCompileStatement39 != null) {
                        sQLiteStatementCompileStatement39.close();
                    }
                    i3 = 66;
                } catch (Throwable th29) {
                    th = th29;
                    sQLiteStatement8 = sQLiteStatementCompileStatement39;
                    if (sQLiteStatement8 != null) {
                        sQLiteStatement8.close();
                    }
                    throw th;
                }
            } catch (Throwable th30) {
                th = th30;
            }
        }
        if (i3 == 66) {
            sQLiteDatabase.beginTransaction();
            int i4 = 166;
            try {
                if (!this.mContext.getResources().getBoolean(android.R.^attr-private.popupPromptView)) {
                    i4 = 174;
                }
                sQLiteDatabase.execSQL("DELETE FROM system WHERE name='mode_ringer_streams_affected'");
                sQLiteDatabase.execSQL("INSERT INTO system ('name', 'value') values ('mode_ringer_streams_affected', '" + String.valueOf(i4) + "')");
                sQLiteDatabase.setTransactionSuccessful();
                sQLiteDatabase.endTransaction();
                i3 = 67;
            } finally {
            }
        }
        if (i3 == 67) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteStatementCompileStatement24 = sQLiteDatabase.compileStatement("INSERT INTO secure(name,value) VALUES(?,?);");
                try {
                    loadBooleanSetting(sQLiteStatementCompileStatement24, "touch_exploration_enabled", R.bool.def_touch_exploration_enabled);
                    sQLiteStatementCompileStatement24.close();
                    sQLiteDatabase.setTransactionSuccessful();
                    if (sQLiteStatementCompileStatement24 != null) {
                        sQLiteStatementCompileStatement24.close();
                    }
                    i3 = 68;
                } catch (Throwable th31) {
                    th = th31;
                    if (sQLiteStatementCompileStatement24 != null) {
                        sQLiteStatementCompileStatement24.close();
                    }
                    throw th;
                }
            } catch (Throwable th32) {
                th = th32;
                sQLiteStatementCompileStatement24 = null;
            }
        }
        if (i3 == 68) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteDatabase.execSQL("DELETE FROM system WHERE name='notifications_use_ring_volume'");
                sQLiteDatabase.setTransactionSuccessful();
                sQLiteDatabase.endTransaction();
                i3 = 69;
            } finally {
            }
        }
        if (i3 == 69) {
            String string = this.mContext.getResources().getString(R.string.def_airplane_mode_radios);
            String string2 = this.mContext.getResources().getString(R.string.airplane_mode_toggleable_radios);
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteDatabase.execSQL("UPDATE system SET value='" + string + "' WHERE name='airplane_mode_radios'");
                sQLiteDatabase.execSQL("UPDATE system SET value='" + string2 + "' WHERE name='airplane_mode_toggleable_radios'");
                sQLiteDatabase.setTransactionSuccessful();
                sQLiteDatabase.endTransaction();
                i3 = 70;
            } finally {
            }
        }
        if (i3 == 70) {
            loadBookmarks(sQLiteDatabase);
            i3 = 71;
        }
        if (i3 == 71) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteStatementCompileStatement23 = sQLiteDatabase.compileStatement("INSERT INTO secure(name,value) VALUES(?,?);");
                try {
                    loadBooleanSetting(sQLiteStatementCompileStatement23, "speak_password", R.bool.def_accessibility_speak_password);
                    sQLiteDatabase.setTransactionSuccessful();
                    if (sQLiteStatementCompileStatement23 != null) {
                        sQLiteStatementCompileStatement23.close();
                    }
                    i3 = 72;
                } catch (Throwable th33) {
                    th = th33;
                    if (sQLiteStatementCompileStatement23 != null) {
                        sQLiteStatementCompileStatement23.close();
                    }
                    throw th;
                }
            } catch (Throwable th34) {
                th = th34;
                sQLiteStatementCompileStatement23 = null;
            }
        }
        if (i3 == 72) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteStatementCompileStatement22 = sQLiteDatabase.compileStatement("INSERT OR REPLACE INTO system(name,value) VALUES(?,?);");
                try {
                    loadBooleanSetting(sQLiteStatementCompileStatement22, "vibrate_in_silent", R.bool.def_vibrate_in_silent);
                    sQLiteDatabase.setTransactionSuccessful();
                    if (sQLiteStatementCompileStatement22 != null) {
                        sQLiteStatementCompileStatement22.close();
                    }
                    i3 = 73;
                } catch (Throwable th35) {
                    th = th35;
                    if (sQLiteStatementCompileStatement22 != null) {
                        sQLiteStatementCompileStatement22.close();
                    }
                    throw th;
                }
            } catch (Throwable th36) {
                th = th36;
                sQLiteStatementCompileStatement22 = null;
            }
        }
        if (i3 == 73) {
            upgradeVibrateSettingFromNone(sQLiteDatabase);
            i3 = 74;
        }
        if (i3 == 74) {
            i3 = 75;
        }
        if (i3 == 75) {
            sQLiteDatabase.beginTransaction();
            try {
                Cursor cursorQuery = sQLiteDatabase.query("secure", new String[]{"_id", "value"}, "name='lockscreen.disabled'", null, null, null, null);
                if (cursorQuery == null) {
                    sQLiteStatementCompileStatement21 = sQLiteDatabase.compileStatement("INSERT INTO system(name,value) VALUES(?,?);");
                    loadBooleanSetting(sQLiteStatementCompileStatement21, "lockscreen.disabled", R.bool.def_lockscreen_disabled);
                    sQLiteDatabase.setTransactionSuccessful();
                    if (cursorQuery != null) {
                    }
                    if (sQLiteStatementCompileStatement21 != null) {
                    }
                    i3 = 76;
                } else {
                    try {
                        if (cursorQuery.getCount() == 0) {
                            sQLiteStatementCompileStatement21 = sQLiteDatabase.compileStatement("INSERT INTO system(name,value) VALUES(?,?);");
                            try {
                                loadBooleanSetting(sQLiteStatementCompileStatement21, "lockscreen.disabled", R.bool.def_lockscreen_disabled);
                                sQLiteDatabase.setTransactionSuccessful();
                                if (cursorQuery != null) {
                                    cursorQuery.close();
                                }
                                if (sQLiteStatementCompileStatement21 != null) {
                                    sQLiteStatementCompileStatement21.close();
                                }
                                i3 = 76;
                            } catch (Throwable th37) {
                                th = th37;
                                cursor = cursorQuery;
                                if (cursor != null) {
                                }
                                if (sQLiteStatementCompileStatement21 != null) {
                                }
                                throw th;
                            }
                        } else {
                            sQLiteStatementCompileStatement21 = null;
                            sQLiteDatabase.setTransactionSuccessful();
                            if (cursorQuery != null) {
                            }
                            if (sQLiteStatementCompileStatement21 != null) {
                            }
                            i3 = 76;
                        }
                    } catch (Throwable th38) {
                        th = th38;
                        sQLiteStatementCompileStatement21 = null;
                        cursor = cursorQuery;
                        if (cursor != null) {
                            cursor.close();
                        }
                        if (sQLiteStatementCompileStatement21 != null) {
                            sQLiteStatementCompileStatement21.close();
                        }
                        throw th;
                    }
                }
            } catch (Throwable th39) {
                th = th39;
                sQLiteStatementCompileStatement21 = null;
            }
        }
        if (i3 == 76) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteDatabase.execSQL("DELETE FROM system WHERE name='vibrate_in_silent'");
                sQLiteDatabase.setTransactionSuccessful();
                sQLiteDatabase.endTransaction();
                i3 = 77;
            } finally {
            }
        }
        if (i3 == 77) {
            loadVibrateWhenRingingSetting(sQLiteDatabase);
            i3 = 78;
        }
        if (i3 == 78) {
            i3 = 79;
        }
        if (i3 == 79) {
            Object[] objArr = getIntValueFromTable(sQLiteDatabase, "secure", "accessibility_enabled", 0) == 1;
            Object[] objArr2 = getIntValueFromTable(sQLiteDatabase, "secure", "touch_exploration_enabled", 0) == 1;
            if (objArr != false && objArr2 != false) {
                String stringValueFromTable = getStringValueFromTable(sQLiteDatabase, "secure", "enabled_accessibility_services", "");
                if (TextUtils.isEmpty(getStringValueFromTable(sQLiteDatabase, "secure", "touch_exploration_granted_accessibility_services", "")) && !TextUtils.isEmpty(stringValueFromTable)) {
                    try {
                        sQLiteDatabase.beginTransaction();
                        SQLiteStatement sQLiteStatementCompileStatement40 = sQLiteDatabase.compileStatement("INSERT OR REPLACE INTO secure(name,value) VALUES(?,?);");
                        try {
                            loadSetting(sQLiteStatementCompileStatement40, "touch_exploration_granted_accessibility_services", stringValueFromTable);
                            sQLiteDatabase.setTransactionSuccessful();
                            if (sQLiteStatementCompileStatement40 != null) {
                                sQLiteStatementCompileStatement40.close();
                            }
                        } catch (Throwable th40) {
                            th = th40;
                            sQLiteStatement9 = sQLiteStatementCompileStatement40;
                            if (sQLiteStatement9 != null) {
                                sQLiteStatement9.close();
                            }
                            throw th;
                        }
                    } catch (Throwable th41) {
                        th = th41;
                    }
                }
            }
            i3 = 80;
        }
        if (i3 == 80) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteStatementCompileStatement20 = sQLiteDatabase.compileStatement("INSERT OR REPLACE INTO secure(name,value) VALUES(?,?);");
                try {
                    loadBooleanSetting(sQLiteStatementCompileStatement20, "screensaver_enabled", android.R.^attr-private.expandActivityOverflowButtonDrawable);
                    loadBooleanSetting(sQLiteStatementCompileStatement20, "screensaver_activate_on_dock", android.R.^attr-private.errorMessageAboveBackground);
                    loadBooleanSetting(sQLiteStatementCompileStatement20, "screensaver_activate_on_sleep", android.R.^attr-private.errorMessageBackground);
                    loadStringSetting(sQLiteStatementCompileStatement20, "screensaver_components", android.R.string.adbwifi_active_notification_message);
                    loadStringSetting(sQLiteStatementCompileStatement20, "screensaver_default_component", android.R.string.adbwifi_active_notification_message);
                    sQLiteDatabase.setTransactionSuccessful();
                    if (sQLiteStatementCompileStatement20 != null) {
                        sQLiteStatementCompileStatement20.close();
                    }
                    i3 = 81;
                } catch (Throwable th42) {
                    th = th42;
                    if (sQLiteStatementCompileStatement20 != null) {
                        sQLiteStatementCompileStatement20.close();
                    }
                    throw th;
                }
            } catch (Throwable th43) {
                th = th43;
                sQLiteStatementCompileStatement20 = null;
            }
        }
        if (i3 == 81) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteStatementCompileStatement19 = sQLiteDatabase.compileStatement("INSERT OR REPLACE INTO secure(name,value) VALUES(?,?);");
                try {
                    loadBooleanSetting(sQLiteStatementCompileStatement19, "package_verifier_enable", R.bool.def_package_verifier_enable);
                    sQLiteDatabase.setTransactionSuccessful();
                    sQLiteDatabase.endTransaction();
                    if (sQLiteStatementCompileStatement19 != null) {
                        sQLiteStatementCompileStatement19.close();
                    }
                    i3 = 82;
                } catch (Throwable th44) {
                    th = th44;
                    if (sQLiteStatementCompileStatement19 != null) {
                        sQLiteStatementCompileStatement19.close();
                    }
                    throw th;
                }
            } catch (Throwable th45) {
                th = th45;
                sQLiteStatementCompileStatement19 = null;
            }
        }
        if (i3 == 82) {
            if (this.mUserHandle == 0) {
                sQLiteDatabase.beginTransaction();
                try {
                    createGlobalTable(sQLiteDatabase);
                    moveSettingsToNewTable(sQLiteDatabase, "system", "global", setToStringArray(SettingsProvider.sSystemMovedToGlobalSettings), false);
                    moveSettingsToNewTable(sQLiteDatabase, "secure", "global", setToStringArray(SettingsProvider.sSecureMovedToGlobalSettings), false);
                    sQLiteDatabase.setTransactionSuccessful();
                } finally {
                }
            }
            i3 = 83;
        }
        if (i3 == 83) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteStatementCompileStatement18 = sQLiteDatabase.compileStatement("INSERT INTO secure(name,value) VALUES(?,?);");
                try {
                    loadBooleanSetting(sQLiteStatementCompileStatement18, "accessibility_display_magnification_enabled", R.bool.def_accessibility_display_magnification_enabled);
                    sQLiteStatementCompileStatement18.close();
                    SQLiteStatement sQLiteStatementCompileStatement41 = sQLiteDatabase.compileStatement("INSERT INTO secure(name,value) VALUES(?,?);");
                    try {
                        loadFractionSetting(sQLiteStatementCompileStatement41, "accessibility_display_magnification_scale", R.fraction.def_accessibility_display_magnification_scale, 1);
                        sQLiteStatementCompileStatement41.close();
                        sQLiteDatabase.setTransactionSuccessful();
                        sQLiteDatabase.endTransaction();
                        if (sQLiteStatementCompileStatement41 != null) {
                            sQLiteStatementCompileStatement41.close();
                        }
                        i3 = 84;
                    } catch (Throwable th46) {
                        th = th46;
                        sQLiteStatementCompileStatement18 = sQLiteStatementCompileStatement41;
                        if (sQLiteStatementCompileStatement18 != null) {
                            sQLiteStatementCompileStatement18.close();
                        }
                        throw th;
                    }
                } catch (Throwable th47) {
                    th = th47;
                }
            } catch (Throwable th48) {
                th = th48;
                sQLiteStatementCompileStatement18 = null;
            }
        }
        if (i3 == 84) {
            if (this.mUserHandle == 0) {
                sQLiteDatabase.beginTransaction();
                try {
                    moveSettingsToNewTable(sQLiteDatabase, "secure", "global", new String[]{"adb_enabled", "bluetooth_on", "data_roaming", "device_provisioned", "install_non_market_apps", "usb_mass_storage_enabled"}, true);
                    sQLiteDatabase.setTransactionSuccessful();
                    sQLiteDatabase.endTransaction();
                } finally {
                }
            }
            i3 = 85;
        }
        if (i3 == 85) {
            if (this.mUserHandle == 0) {
                sQLiteDatabase.beginTransaction();
                try {
                    moveSettingsToNewTable(sQLiteDatabase, "system", "global", new String[]{"stay_on_while_plugged_in"}, true);
                    sQLiteDatabase.setTransactionSuccessful();
                    sQLiteDatabase.endTransaction();
                } finally {
                }
            }
            i3 = 86;
        }
        if (i3 == 86) {
            if (this.mUserHandle == 0) {
                sQLiteDatabase.beginTransaction();
                try {
                    moveSettingsToNewTable(sQLiteDatabase, "secure", "global", new String[]{"package_verifier_enable", "verifier_timeout", "verifier_default_response"}, true);
                    sQLiteDatabase.setTransactionSuccessful();
                    sQLiteDatabase.endTransaction();
                } finally {
                }
            }
            i3 = 87;
        }
        if (i3 == 87) {
            if (this.mUserHandle == 0) {
                sQLiteDatabase.beginTransaction();
                try {
                    moveSettingsToNewTable(sQLiteDatabase, "secure", "global", new String[]{"data_stall_alarm_non_aggressive_delay_in_ms", "data_stall_alarm_aggressive_delay_in_ms", "gprs_register_check_period_ms"}, true);
                    sQLiteDatabase.setTransactionSuccessful();
                    sQLiteDatabase.endTransaction();
                } finally {
                }
            }
            i3 = 88;
        }
        if (i3 == 88) {
            if (this.mUserHandle == 0) {
                sQLiteDatabase.beginTransaction();
                try {
                    moveSettingsToNewTable(sQLiteDatabase, "secure", "global", new String[]{"battery_discharge_duration_threshold", "battery_discharge_threshold", "send_action_app_error", "dropbox_age_seconds", "dropbox_max_files", "dropbox_quota_kb", "dropbox_quota_percent", "dropbox_reserve_percent", "dropbox:", "logcat_for_", "sys_free_storage_log_interval", "disk_free_change_reporting_threshold", "sys_storage_threshold_percentage", "sys_storage_threshold_max_bytes", "sys_storage_full_threshold_bytes", "sync_max_retry_delay_in_seconds", "connectivity_change_delay", "captive_portal_detection_enabled", "captive_portal_server", "nsd_on", "set_install_location", "default_install_location", "inet_condition_debounce_up_delay", "inet_condition_debounce_down_delay", "read_external_storage_enforced_default", "http_proxy", "global_http_proxy_host", "global_http_proxy_port", "global_http_proxy_exclusion_list", "set_global_http_proxy", "default_dns_server"}, true);
                    sQLiteDatabase.setTransactionSuccessful();
                    sQLiteDatabase.endTransaction();
                } finally {
                }
            }
            i3 = 89;
        }
        if (i3 == 89) {
            if (this.mUserHandle == 0) {
                sQLiteDatabase.beginTransaction();
                try {
                    movePrefixedSettingsToNewTable(sQLiteDatabase, "secure", "global", new String[]{"bluetooth_headset_priority_", "bluetooth_a2dp_sink_priority_", "bluetooth_input_device_priority_"});
                    sQLiteDatabase.setTransactionSuccessful();
                    sQLiteDatabase.endTransaction();
                } finally {
                }
            }
            i3 = 90;
        }
        if (i3 == 90) {
            if (this.mUserHandle == 0) {
                sQLiteDatabase.beginTransaction();
                try {
                    moveSettingsToNewTable(sQLiteDatabase, "system", "global", new String[]{"window_animation_scale", "transition_animation_scale", "animator_duration_scale", "fancy_ime_animations", "compatibility_mode", "emergency_tone", "call_auto_retry", "debug_app", "wait_for_debugger", "always_finish_activities"}, true);
                    moveSettingsToNewTable(sQLiteDatabase, "secure", "global", new String[]{"preferred_network_mode", "subscription_mode"}, true);
                    sQLiteDatabase.setTransactionSuccessful();
                } finally {
                }
            }
            i3 = 91;
        }
        if (i3 == 91) {
            if (this.mUserHandle == 0) {
                sQLiteDatabase.beginTransaction();
                try {
                    moveSettingsToNewTable(sQLiteDatabase, "system", "global", new String[]{"mode_ringer"}, true);
                    sQLiteDatabase.setTransactionSuccessful();
                } finally {
                }
            }
            i3 = 92;
        }
        if (i3 == 92) {
            try {
                sQLiteStatementCompileStatement = sQLiteDatabase.compileStatement("INSERT OR IGNORE INTO secure(name,value) VALUES(?,?);");
                try {
                    if (this.mUserHandle == 0) {
                        loadSetting(sQLiteStatementCompileStatement, "user_setup_complete", Integer.valueOf(getIntValueFromTable(sQLiteDatabase, "global", "device_provisioned", 0)));
                    } else {
                        loadBooleanSetting(sQLiteStatementCompileStatement, "user_setup_complete", R.bool.def_user_setup_complete);
                    }
                    if (sQLiteStatementCompileStatement != null) {
                        sQLiteStatementCompileStatement.close();
                    }
                    i3 = 93;
                } catch (Throwable th49) {
                    th = th49;
                    if (sQLiteStatementCompileStatement != null) {
                        sQLiteStatementCompileStatement.close();
                    }
                    throw th;
                }
            } catch (Throwable th50) {
                th = th50;
                sQLiteStatementCompileStatement = null;
            }
        }
        if (i3 == 93) {
            if (this.mUserHandle == 0) {
                sQLiteDatabase.beginTransaction();
                try {
                    moveSettingsToNewTable(sQLiteDatabase, "system", "global", setToStringArray(SettingsProvider.sSystemMovedToGlobalSettings), true);
                    moveSettingsToNewTable(sQLiteDatabase, "secure", "global", setToStringArray(SettingsProvider.sSecureMovedToGlobalSettings), true);
                    sQLiteDatabase.setTransactionSuccessful();
                } finally {
                }
            }
            i3 = 94;
        }
        if (i3 == 94) {
            if (this.mUserHandle == 0) {
                sQLiteDatabase.beginTransaction();
                try {
                    sQLiteStatementCompileStatement17 = sQLiteDatabase.compileStatement("INSERT OR REPLACE INTO global(name,value) VALUES(?,?);");
                    try {
                        loadStringSetting(sQLiteStatementCompileStatement17, "wireless_charging_started_sound", R.string.def_wireless_charging_started_sound);
                        sQLiteDatabase.setTransactionSuccessful();
                        if (sQLiteStatementCompileStatement17 != null) {
                            sQLiteStatementCompileStatement17.close();
                        }
                    } catch (Throwable th51) {
                        th = th51;
                        if (sQLiteStatementCompileStatement17 != null) {
                            sQLiteStatementCompileStatement17.close();
                        }
                        throw th;
                    }
                } catch (Throwable th52) {
                    th = th52;
                    sQLiteStatementCompileStatement17 = null;
                }
            }
            i3 = 95;
        }
        if (i3 == 95) {
            if (this.mUserHandle == 0) {
                sQLiteDatabase.beginTransaction();
                try {
                    moveSettingsToNewTable(sQLiteDatabase, "secure", "global", new String[]{"bugreport_in_power_menu"}, true);
                    sQLiteDatabase.setTransactionSuccessful();
                } finally {
                }
            }
            i3 = 96;
        }
        if (i3 == 96) {
            i3 = 97;
        }
        if (i3 == 97) {
            if (this.mUserHandle == 0) {
                sQLiteDatabase.beginTransaction();
                try {
                    sQLiteStatementCompileStatement16 = sQLiteDatabase.compileStatement("INSERT OR REPLACE INTO global(name,value) VALUES(?,?);");
                    try {
                        loadIntegerSetting(sQLiteStatementCompileStatement16, "low_battery_sound_timeout", R.integer.def_low_battery_sound_timeout);
                        sQLiteDatabase.setTransactionSuccessful();
                        if (sQLiteStatementCompileStatement16 != null) {
                            sQLiteStatementCompileStatement16.close();
                        }
                    } catch (Throwable th53) {
                        th = th53;
                        if (sQLiteStatementCompileStatement16 != null) {
                            sQLiteStatementCompileStatement16.close();
                        }
                        throw th;
                    }
                } catch (Throwable th54) {
                    th = th54;
                    sQLiteStatementCompileStatement16 = null;
                }
            }
            i3 = 98;
        }
        if (i3 == 98) {
            i3 = 99;
        }
        if (i3 == 99) {
            i3 = 100;
        }
        if (i3 == 100) {
            if (this.mUserHandle == 0) {
                sQLiteDatabase.beginTransaction();
                try {
                    sQLiteStatementCompileStatement15 = sQLiteDatabase.compileStatement("INSERT OR REPLACE INTO global(name,value) VALUES(?,?);");
                    try {
                        loadIntegerSetting(sQLiteStatementCompileStatement15, "heads_up_notifications_enabled", R.integer.def_heads_up_enabled);
                        sQLiteDatabase.setTransactionSuccessful();
                        if (sQLiteStatementCompileStatement15 != null) {
                            sQLiteStatementCompileStatement15.close();
                        }
                    } catch (Throwable th55) {
                        th = th55;
                        if (sQLiteStatementCompileStatement15 != null) {
                            sQLiteStatementCompileStatement15.close();
                        }
                        throw th;
                    }
                } catch (Throwable th56) {
                    th = th56;
                    sQLiteStatementCompileStatement15 = null;
                }
            }
            i3 = 101;
        }
        if (i3 == 101) {
            if (this.mUserHandle == 0) {
                sQLiteDatabase.beginTransaction();
                try {
                    sQLiteStatementCompileStatement14 = sQLiteDatabase.compileStatement("INSERT OR IGNORE INTO global(name,value) VALUES(?,?);");
                    try {
                        loadSetting(sQLiteStatementCompileStatement14, "device_name", getDefaultDeviceName());
                        sQLiteDatabase.setTransactionSuccessful();
                        if (sQLiteStatementCompileStatement14 != null) {
                            sQLiteStatementCompileStatement14.close();
                        }
                    } catch (Throwable th57) {
                        th = th57;
                        if (sQLiteStatementCompileStatement14 != null) {
                            sQLiteStatementCompileStatement14.close();
                        }
                        throw th;
                    }
                } catch (Throwable th58) {
                    th = th58;
                    sQLiteStatementCompileStatement14 = null;
                }
            }
            i3 = 102;
        }
        if (i3 == 102) {
            sQLiteDatabase.beginTransaction();
            try {
                if (this.mUserHandle == 0) {
                    moveSettingsToNewTable(sQLiteDatabase, "global", "secure", new String[]{"install_non_market_apps"}, true);
                    sQLiteStatementCompileStatement13 = null;
                } else {
                    sQLiteStatementCompileStatement13 = sQLiteDatabase.compileStatement("INSERT OR IGNORE INTO secure(name,value) VALUES(?,?);");
                    try {
                        loadBooleanSetting(sQLiteStatementCompileStatement13, "install_non_market_apps", R.bool.def_install_non_market_apps);
                    } catch (Throwable th59) {
                        th = th59;
                        sQLiteStatement = sQLiteStatementCompileStatement13;
                        if (sQLiteStatement != null) {
                            sQLiteStatement.close();
                        }
                        throw th;
                    }
                }
                sQLiteDatabase.setTransactionSuccessful();
                if (sQLiteStatementCompileStatement13 != null) {
                    sQLiteStatementCompileStatement13.close();
                }
                i3 = 103;
            } catch (Throwable th60) {
                th = th60;
            }
        }
        if (i3 == 103) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteStatementCompileStatement12 = sQLiteDatabase.compileStatement("INSERT OR REPLACE INTO secure(name,value) VALUES(?,?);");
                try {
                    loadBooleanSetting(sQLiteStatementCompileStatement12, "wake_gesture_enabled", R.bool.def_wake_gesture_enabled);
                    sQLiteDatabase.setTransactionSuccessful();
                    if (sQLiteStatementCompileStatement12 != null) {
                        sQLiteStatementCompileStatement12.close();
                    }
                    i3 = 104;
                } catch (Throwable th61) {
                    th = th61;
                    if (sQLiteStatementCompileStatement12 != null) {
                        sQLiteStatementCompileStatement12.close();
                    }
                    throw th;
                }
            } catch (Throwable th62) {
                th = th62;
                sQLiteStatementCompileStatement12 = null;
            }
        }
        if (i3 < 105) {
            i3 = 105;
        }
        if (i3 < 106) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteStatementCompileStatement11 = sQLiteDatabase.compileStatement("INSERT OR IGNORE INTO secure(name,value) VALUES(?,?);");
                try {
                    loadIntegerSetting(sQLiteStatementCompileStatement11, "lock_screen_show_notifications", R.integer.def_lock_screen_show_notifications);
                    if (this.mUserHandle == 0 && (intValueFromTable = getIntValueFromTable(sQLiteDatabase, "global", "lock_screen_show_notifications", -1)) >= 0) {
                        loadSetting(sQLiteStatementCompileStatement11, "lock_screen_show_notifications", Integer.valueOf(intValueFromTable));
                        SQLiteStatement sQLiteStatementCompileStatement42 = sQLiteDatabase.compileStatement("DELETE FROM global WHERE name=?");
                        sQLiteStatementCompileStatement42.bindString(1, "lock_screen_show_notifications");
                        sQLiteStatementCompileStatement42.execute();
                    }
                    sQLiteDatabase.setTransactionSuccessful();
                    if (sQLiteStatementCompileStatement11 != null) {
                        sQLiteStatementCompileStatement11.close();
                    }
                    i3 = 106;
                } catch (Throwable th63) {
                    th = th63;
                    if (sQLiteStatementCompileStatement11 != null) {
                        sQLiteStatementCompileStatement11.close();
                    }
                    throw th;
                }
            } catch (Throwable th64) {
                th = th64;
                sQLiteStatementCompileStatement11 = null;
            }
        }
        if (i3 < 107) {
            if (this.mUserHandle == 0) {
                sQLiteDatabase.beginTransaction();
                try {
                    sQLiteStatementCompileStatement10 = sQLiteDatabase.compileStatement("INSERT OR REPLACE INTO global(name,value) VALUES(?,?);");
                    try {
                        loadStringSetting(sQLiteStatementCompileStatement10, "trusted_sound", R.string.def_trusted_sound);
                        sQLiteDatabase.setTransactionSuccessful();
                        if (sQLiteStatementCompileStatement10 != null) {
                            sQLiteStatementCompileStatement10.close();
                        }
                    } catch (Throwable th65) {
                        th = th65;
                        if (sQLiteStatementCompileStatement10 != null) {
                            sQLiteStatementCompileStatement10.close();
                        }
                        throw th;
                    }
                } catch (Throwable th66) {
                    th = th66;
                    sQLiteStatementCompileStatement10 = null;
                }
            }
            i3 = 107;
        }
        if (i3 < 108) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteStatementCompileStatement9 = sQLiteDatabase.compileStatement("INSERT OR REPLACE INTO system(name,value) VALUES(?,?);");
                try {
                    loadBooleanSetting(sQLiteStatementCompileStatement9, "screen_brightness_mode", R.bool.def_screen_brightness_automatic_mode);
                    sQLiteDatabase.setTransactionSuccessful();
                    if (sQLiteStatementCompileStatement9 != null) {
                        sQLiteStatementCompileStatement9.close();
                    }
                    i3 = 108;
                } catch (Throwable th67) {
                    th = th67;
                    if (sQLiteStatementCompileStatement9 != null) {
                        sQLiteStatementCompileStatement9.close();
                    }
                    throw th;
                }
            } catch (Throwable th68) {
                th = th68;
                sQLiteStatementCompileStatement9 = null;
            }
        }
        if (i3 < 109) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteStatementCompileStatement8 = sQLiteDatabase.compileStatement("INSERT OR IGNORE INTO secure(name,value) VALUES(?,?);");
                try {
                    loadBooleanSetting(sQLiteStatementCompileStatement8, "lock_screen_allow_private_notifications", R.bool.def_lock_screen_allow_private_notifications);
                    sQLiteDatabase.setTransactionSuccessful();
                    if (sQLiteStatementCompileStatement8 != null) {
                        sQLiteStatementCompileStatement8.close();
                    }
                    i3 = 109;
                } catch (Throwable th69) {
                    th = th69;
                    if (sQLiteStatementCompileStatement8 != null) {
                        sQLiteStatementCompileStatement8.close();
                    }
                    throw th;
                }
            } catch (Throwable th70) {
                th = th70;
                sQLiteStatementCompileStatement8 = null;
            }
        }
        if (i3 < 110) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteStatementCompileStatement7 = sQLiteDatabase.compileStatement("UPDATE system SET value = ? WHERE name = ? AND value = ?;");
                try {
                    sQLiteStatementCompileStatement7.bindString(1, "SIP_ADDRESS_ONLY");
                    sQLiteStatementCompileStatement7.bindString(2, "sip_call_options");
                    sQLiteStatementCompileStatement7.bindString(3, "SIP_ASK_ME_EACH_TIME");
                    sQLiteStatementCompileStatement7.execute();
                    sQLiteDatabase.setTransactionSuccessful();
                    if (sQLiteStatementCompileStatement7 != null) {
                        sQLiteStatementCompileStatement7.close();
                    }
                    i3 = 110;
                } catch (Throwable th71) {
                    th = th71;
                    if (sQLiteStatementCompileStatement7 != null) {
                        sQLiteStatementCompileStatement7.close();
                    }
                    throw th;
                }
            } catch (Throwable th72) {
                th = th72;
                sQLiteStatementCompileStatement7 = null;
            }
        }
        if (i3 < 111) {
            if (this.mUserHandle == 0) {
                sQLiteDatabase.beginTransaction();
                try {
                    sQLiteStatementCompileStatement6 = sQLiteDatabase.compileStatement("INSERT OR REPLACE INTO global(name,value) VALUES(?,?);");
                    try {
                        loadSetting(sQLiteStatementCompileStatement6, "mode_ringer", 2);
                        sQLiteDatabase.setTransactionSuccessful();
                        if (sQLiteStatementCompileStatement6 != null) {
                            sQLiteStatementCompileStatement6.close();
                        }
                    } catch (Throwable th73) {
                        th = th73;
                        if (sQLiteStatementCompileStatement6 != null) {
                            sQLiteStatementCompileStatement6.close();
                        }
                        throw th;
                    }
                } catch (Throwable th74) {
                    th = th74;
                    sQLiteStatementCompileStatement6 = null;
                }
            }
            i3 = 111;
        }
        if (i3 < 112) {
            if (this.mUserHandle == 0) {
                sQLiteDatabase.beginTransaction();
                try {
                    sQLiteStatementCompileStatement5 = sQLiteDatabase.compileStatement("UPDATE global SET value = ?  WHERE name = ? AND value = ?");
                    try {
                        sQLiteStatementCompileStatement5.bindString(1, getDefaultDeviceName());
                        sQLiteStatementCompileStatement5.bindString(2, "device_name");
                        sQLiteStatementCompileStatement5.bindString(3, getOldDefaultDeviceName());
                        sQLiteStatementCompileStatement5.execute();
                        sQLiteDatabase.setTransactionSuccessful();
                        if (sQLiteStatementCompileStatement5 != null) {
                            sQLiteStatementCompileStatement5.close();
                        }
                    } catch (Throwable th75) {
                        th = th75;
                        if (sQLiteStatementCompileStatement5 != null) {
                            sQLiteStatementCompileStatement5.close();
                        }
                        throw th;
                    }
                } catch (Throwable th76) {
                    th = th76;
                    sQLiteStatementCompileStatement5 = null;
                }
            }
            i3 = 112;
        }
        if (i3 < 113) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteStatementCompileStatement4 = sQLiteDatabase.compileStatement("INSERT OR IGNORE INTO secure(name,value) VALUES(?,?);");
                try {
                    loadIntegerSetting(sQLiteStatementCompileStatement4, "sleep_timeout", R.integer.def_sleep_timeout);
                    sQLiteDatabase.setTransactionSuccessful();
                    if (sQLiteStatementCompileStatement4 != null) {
                        sQLiteStatementCompileStatement4.close();
                    }
                    i3 = 113;
                } catch (Throwable th77) {
                    th = th77;
                    if (sQLiteStatementCompileStatement4 != null) {
                        sQLiteStatementCompileStatement4.close();
                    }
                    throw th;
                }
            } catch (Throwable th78) {
                th = th78;
                sQLiteStatementCompileStatement4 = null;
            }
        }
        if (i3 < 115) {
            if (this.mUserHandle == 0) {
                sQLiteDatabase.beginTransaction();
                try {
                    sQLiteStatementCompileStatement3 = sQLiteDatabase.compileStatement("INSERT OR IGNORE INTO global(name,value) VALUES(?,?);");
                    try {
                        loadBooleanSetting(sQLiteStatementCompileStatement3, "theater_mode_on", R.bool.def_theater_mode_on);
                        sQLiteDatabase.setTransactionSuccessful();
                        if (sQLiteStatementCompileStatement3 != null) {
                            sQLiteStatementCompileStatement3.close();
                        }
                    } catch (Throwable th79) {
                        th = th79;
                        if (sQLiteStatementCompileStatement3 != null) {
                            sQLiteStatementCompileStatement3.close();
                        }
                        throw th;
                    }
                } catch (Throwable th80) {
                    th = th80;
                    sQLiteStatementCompileStatement3 = null;
                }
            }
            i3 = 115;
        }
        if (i3 < 116) {
            i3 = 116;
        }
        if (i3 < 117) {
            sQLiteDatabase.beginTransaction();
            try {
                moveSettingsToNewTable(sQLiteDatabase, "system", "secure", new String[]{"lock_to_app_exit_locked"}, true);
                sQLiteDatabase.setTransactionSuccessful();
                sQLiteDatabase.endTransaction();
                i3 = 117;
            } finally {
            }
        }
        if (i3 < 118) {
            sQLiteDatabase.beginTransaction();
            try {
                sQLiteStatementCompileStatement2 = sQLiteDatabase.compileStatement("INSERT OR REPLACE INTO system(name,value) VALUES(?,?);");
                try {
                    loadSetting(sQLiteStatementCompileStatement2, "hide_rotation_lock_toggle_for_accessibility", 0);
                    sQLiteDatabase.setTransactionSuccessful();
                    if (sQLiteStatementCompileStatement2 != null) {
                        sQLiteStatementCompileStatement2.close();
                    }
                    i3 = 118;
                } catch (Throwable th81) {
                    th = th81;
                    if (sQLiteStatementCompileStatement2 != null) {
                        sQLiteStatementCompileStatement2.close();
                    }
                    throw th;
                }
            } catch (Throwable th82) {
                th = th82;
                sQLiteStatementCompileStatement2 = null;
            }
        }
        if (i3 != i2) {
            recreateDatabase(sQLiteDatabase, i, i3, i2);
        }
    }

    public void recreateDatabase(SQLiteDatabase sQLiteDatabase, int i, int i2, int i3) throws Throwable {
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS global");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS globalIndex1");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS system");
        sQLiteDatabase.execSQL("DROP INDEX IF EXISTS systemIndex1");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS secure");
        sQLiteDatabase.execSQL("DROP INDEX IF EXISTS secureIndex1");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS gservices");
        sQLiteDatabase.execSQL("DROP INDEX IF EXISTS gservicesIndex1");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS bluetooth_devices");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS bookmarks");
        sQLiteDatabase.execSQL("DROP INDEX IF EXISTS bookmarksIndex1");
        sQLiteDatabase.execSQL("DROP INDEX IF EXISTS bookmarksIndex2");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS favorites");
        onCreate(sQLiteDatabase);
        sQLiteDatabase.execSQL("INSERT INTO secure(name,value) values('wiped_db_reason','" + (i + "/" + i2 + "/" + i3) + "');");
    }

    private String[] setToStringArray(Set<String> set) {
        return (String[]) set.toArray(new String[set.size()]);
    }

    private void moveSettingsToNewTable(SQLiteDatabase sQLiteDatabase, String str, String str2, String[] strArr, boolean z) throws Throwable {
        SQLiteStatement sQLiteStatement;
        sQLiteDatabase.beginTransaction();
        SQLiteStatement sQLiteStatement2 = null;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("INSERT ");
            sb.append(z ? " OR IGNORE " : "");
            sb.append(" INTO ");
            sb.append(str2);
            sb.append(" (name,value) SELECT name,value FROM ");
            sb.append(str);
            sb.append(" WHERE name=?");
            SQLiteStatement sQLiteStatementCompileStatement = sQLiteDatabase.compileStatement(sb.toString());
            try {
                SQLiteStatement sQLiteStatementCompileStatement2 = sQLiteDatabase.compileStatement("DELETE FROM " + str + " WHERE name=?");
                try {
                    for (String str3 : strArr) {
                        sQLiteStatementCompileStatement.bindString(1, str3);
                        sQLiteStatementCompileStatement.execute();
                        sQLiteStatementCompileStatement2.bindString(1, str3);
                        sQLiteStatementCompileStatement2.execute();
                    }
                    sQLiteDatabase.setTransactionSuccessful();
                    sQLiteDatabase.endTransaction();
                    if (sQLiteStatementCompileStatement != null) {
                        sQLiteStatementCompileStatement.close();
                    }
                    if (sQLiteStatementCompileStatement2 != null) {
                        sQLiteStatementCompileStatement2.close();
                    }
                } catch (Throwable th) {
                    sQLiteStatement2 = sQLiteStatementCompileStatement;
                    sQLiteStatement = sQLiteStatementCompileStatement2;
                    th = th;
                    sQLiteDatabase.endTransaction();
                    if (sQLiteStatement2 != null) {
                        sQLiteStatement2.close();
                    }
                    if (sQLiteStatement != null) {
                        sQLiteStatement.close();
                    }
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                sQLiteStatement2 = sQLiteStatementCompileStatement;
                sQLiteStatement = null;
            }
        } catch (Throwable th3) {
            th = th3;
            sQLiteStatement = null;
        }
    }

    private void movePrefixedSettingsToNewTable(SQLiteDatabase sQLiteDatabase, String str, String str2, String[] strArr) throws Throwable {
        SQLiteStatement sQLiteStatementCompileStatement;
        sQLiteDatabase.beginTransaction();
        SQLiteStatement sQLiteStatement = null;
        try {
            sQLiteStatementCompileStatement = sQLiteDatabase.compileStatement("INSERT INTO " + str2 + " (name,value) SELECT name,value FROM " + str + " WHERE substr(name,0,?)=?");
            try {
                SQLiteStatement sQLiteStatementCompileStatement2 = sQLiteDatabase.compileStatement("DELETE FROM " + str + " WHERE substr(name,0,?)=?");
                try {
                    for (String str3 : strArr) {
                        sQLiteStatementCompileStatement.bindLong(1, str3.length() + 1);
                        sQLiteStatementCompileStatement.bindString(2, str3);
                        sQLiteStatementCompileStatement.execute();
                        sQLiteStatementCompileStatement2.bindLong(1, str3.length() + 1);
                        sQLiteStatementCompileStatement2.bindString(2, str3);
                        sQLiteStatementCompileStatement2.execute();
                    }
                    sQLiteDatabase.setTransactionSuccessful();
                    sQLiteDatabase.endTransaction();
                    if (sQLiteStatementCompileStatement != null) {
                        sQLiteStatementCompileStatement.close();
                    }
                    if (sQLiteStatementCompileStatement2 != null) {
                        sQLiteStatementCompileStatement2.close();
                    }
                } catch (Throwable th) {
                    th = th;
                    sQLiteStatement = sQLiteStatementCompileStatement2;
                    sQLiteDatabase.endTransaction();
                    if (sQLiteStatementCompileStatement != null) {
                        sQLiteStatementCompileStatement.close();
                    }
                    if (sQLiteStatement != null) {
                        sQLiteStatement.close();
                    }
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (Throwable th3) {
            th = th3;
            sQLiteStatementCompileStatement = null;
        }
    }

    private void upgradeLockPatternLocation(SQLiteDatabase sQLiteDatabase) {
        Cursor cursorQuery = sQLiteDatabase.query("system", new String[]{"_id", "value"}, "name='lock_pattern'", null, null, null, null);
        if (cursorQuery.getCount() > 0) {
            cursorQuery.moveToFirst();
            String string = cursorQuery.getString(1);
            if (!TextUtils.isEmpty(string)) {
                try {
                    new LockPatternUtils(this.mContext).saveLockPattern(LockPatternUtils.stringToPattern(string), (String) null, 0);
                } catch (IllegalArgumentException e) {
                }
            }
            cursorQuery.close();
            sQLiteDatabase.delete("system", "name='lock_pattern'", null);
            return;
        }
        cursorQuery.close();
    }

    private void upgradeScreenTimeoutFromNever(SQLiteDatabase sQLiteDatabase) throws Throwable {
        Throwable th;
        SQLiteStatement sQLiteStatementCompileStatement;
        Cursor cursorQuery = sQLiteDatabase.query("system", new String[]{"_id", "value"}, "name=? AND value=?", new String[]{"screen_off_timeout", "-1"}, null, null, null);
        if (cursorQuery.getCount() > 0) {
            cursorQuery.close();
            try {
                sQLiteStatementCompileStatement = sQLiteDatabase.compileStatement("INSERT OR REPLACE INTO system(name,value) VALUES(?,?);");
            } catch (Throwable th2) {
                th = th2;
                sQLiteStatementCompileStatement = null;
            }
            try {
                loadSetting(sQLiteStatementCompileStatement, "screen_off_timeout", Integer.toString(1800000));
                if (sQLiteStatementCompileStatement != null) {
                    sQLiteStatementCompileStatement.close();
                    return;
                }
                return;
            } catch (Throwable th3) {
                th = th3;
                if (sQLiteStatementCompileStatement != null) {
                    sQLiteStatementCompileStatement.close();
                }
                throw th;
            }
        }
        cursorQuery.close();
    }

    private void upgradeVibrateSettingFromNone(SQLiteDatabase sQLiteDatabase) throws Throwable {
        SQLiteStatement sQLiteStatementCompileStatement;
        int intValueFromSystem = getIntValueFromSystem(sQLiteDatabase, "vibrate_on", 0);
        if ((intValueFromSystem & 3) == 0) {
            intValueFromSystem = AudioSystem.getValueForVibrateSetting(0, 0, 2);
        }
        int valueForVibrateSetting = AudioSystem.getValueForVibrateSetting(intValueFromSystem, 1, intValueFromSystem);
        try {
            sQLiteStatementCompileStatement = sQLiteDatabase.compileStatement("INSERT OR REPLACE INTO system(name,value) VALUES(?,?);");
            try {
                loadSetting(sQLiteStatementCompileStatement, "vibrate_on", Integer.valueOf(valueForVibrateSetting));
                if (sQLiteStatementCompileStatement != null) {
                    sQLiteStatementCompileStatement.close();
                }
            } catch (Throwable th) {
                th = th;
                if (sQLiteStatementCompileStatement != null) {
                    sQLiteStatementCompileStatement.close();
                }
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            sQLiteStatementCompileStatement = null;
        }
    }

    private void upgradeAutoBrightness(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.beginTransaction();
        try {
            sQLiteDatabase.execSQL("INSERT OR REPLACE INTO system(name,value) values('screen_brightness_mode','" + (this.mContext.getResources().getBoolean(R.bool.def_screen_brightness_automatic_mode) ? "1" : "0") + "');");
            sQLiteDatabase.setTransactionSuccessful();
        } finally {
            sQLiteDatabase.endTransaction();
        }
    }

    private void loadBookmarks(SQLiteDatabase sQLiteDatabase) {
        String string;
        Intent intentMakeMainSelectorActivity;
        ComponentName componentName;
        ActivityInfo activityInfo;
        ContentValues contentValues = new ContentValues();
        PackageManager packageManager = this.mContext.getPackageManager();
        try {
            XmlResourceParser xml = this.mContext.getResources().getXml(R.xml.bookmarks);
            XmlUtils.beginDocument(xml, "bookmarks");
            int depth = xml.getDepth();
            while (true) {
                int next = xml.next();
                if ((next != 3 || xml.getDepth() > depth) && next != 1) {
                    if (next == 2) {
                        if ("bookmark".equals(xml.getName())) {
                            String attributeValue = xml.getAttributeValue(null, "package");
                            String attributeValue2 = xml.getAttributeValue(null, "class");
                            String attributeValue3 = xml.getAttributeValue(null, "shortcut");
                            String attributeValue4 = xml.getAttributeValue(null, "category");
                            char cCharAt = attributeValue3.charAt(0);
                            if (TextUtils.isEmpty(attributeValue3)) {
                                Log.w("SettingsProvider", "Unable to get shortcut for: " + attributeValue + "/" + attributeValue2);
                            } else if (attributeValue != null && attributeValue2 != null) {
                                ComponentName componentName2 = new ComponentName(attributeValue, attributeValue2);
                                try {
                                    activityInfo = packageManager.getActivityInfo(componentName2, 0);
                                    componentName = componentName2;
                                } catch (PackageManager.NameNotFoundException e) {
                                    componentName = new ComponentName(packageManager.canonicalToCurrentPackageNames(new String[]{attributeValue})[0], attributeValue2);
                                    try {
                                        activityInfo = packageManager.getActivityInfo(componentName, 0);
                                    } catch (PackageManager.NameNotFoundException e2) {
                                        Log.w("SettingsProvider", "Unable to add bookmark: " + attributeValue + "/" + attributeValue2, e);
                                    }
                                }
                                intentMakeMainSelectorActivity = new Intent("android.intent.action.MAIN", (Uri) null);
                                intentMakeMainSelectorActivity.addCategory("android.intent.category.LAUNCHER");
                                intentMakeMainSelectorActivity.setComponent(componentName);
                                string = activityInfo.loadLabel(packageManager).toString();
                                intentMakeMainSelectorActivity.setFlags(268435456);
                                contentValues.put("intent", intentMakeMainSelectorActivity.toUri(0));
                                contentValues.put("title", string);
                                contentValues.put("shortcut", Integer.valueOf(cCharAt));
                                sQLiteDatabase.delete("bookmarks", "shortcut = ?", new String[]{Integer.toString(cCharAt)});
                                sQLiteDatabase.insert("bookmarks", null, contentValues);
                            } else if (attributeValue4 != null) {
                                intentMakeMainSelectorActivity = Intent.makeMainSelectorActivity("android.intent.action.MAIN", attributeValue4);
                                string = "";
                                intentMakeMainSelectorActivity.setFlags(268435456);
                                contentValues.put("intent", intentMakeMainSelectorActivity.toUri(0));
                                contentValues.put("title", string);
                                contentValues.put("shortcut", Integer.valueOf(cCharAt));
                                sQLiteDatabase.delete("bookmarks", "shortcut = ?", new String[]{Integer.toString(cCharAt)});
                                sQLiteDatabase.insert("bookmarks", null, contentValues);
                            } else {
                                Log.w("SettingsProvider", "Unable to add bookmark for shortcut " + attributeValue3 + ": missing package/class or category attributes");
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

    private void loadVolumeLevels(SQLiteDatabase sQLiteDatabase) throws Throwable {
        SQLiteStatement sQLiteStatementCompileStatement;
        try {
            sQLiteStatementCompileStatement = sQLiteDatabase.compileStatement("INSERT OR IGNORE INTO system(name,value) VALUES(?,?);");
            try {
                loadSetting(sQLiteStatementCompileStatement, "volume_music", Integer.valueOf(AudioSystem.getDefaultStreamVolume(3)));
                loadSetting(sQLiteStatementCompileStatement, "volume_ring", Integer.valueOf(AudioSystem.getDefaultStreamVolume(2)));
                loadSetting(sQLiteStatementCompileStatement, "volume_system", Integer.valueOf(AudioSystem.getDefaultStreamVolume(1)));
                loadSetting(sQLiteStatementCompileStatement, "volume_voice", Integer.valueOf(AudioSystem.getDefaultStreamVolume(0)));
                loadSetting(sQLiteStatementCompileStatement, "volume_alarm", Integer.valueOf(AudioSystem.getDefaultStreamVolume(4)));
                loadSetting(sQLiteStatementCompileStatement, "volume_notification", Integer.valueOf(AudioSystem.getDefaultStreamVolume(5)));
                loadSetting(sQLiteStatementCompileStatement, "volume_bluetooth_sco", Integer.valueOf(AudioSystem.getDefaultStreamVolume(6)));
                int i = 166;
                if (!this.mContext.getResources().getBoolean(android.R.^attr-private.popupPromptView)) {
                    i = 174;
                }
                loadSetting(sQLiteStatementCompileStatement, "mode_ringer_streams_affected", Integer.valueOf(i));
                loadSetting(sQLiteStatementCompileStatement, "mute_streams_affected", 47);
                if (sQLiteStatementCompileStatement != null) {
                    sQLiteStatementCompileStatement.close();
                }
                loadVibrateWhenRingingSetting(sQLiteDatabase);
            } catch (Throwable th) {
                th = th;
                if (sQLiteStatementCompileStatement != null) {
                    sQLiteStatementCompileStatement.close();
                }
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            sQLiteStatementCompileStatement = null;
        }
    }

    private void loadVibrateSetting(SQLiteDatabase sQLiteDatabase, boolean z) throws Throwable {
        Throwable th;
        SQLiteStatement sQLiteStatementCompileStatement;
        if (z) {
            sQLiteDatabase.execSQL("DELETE FROM system WHERE name='vibrate_on'");
        }
        try {
            sQLiteStatementCompileStatement = sQLiteDatabase.compileStatement("INSERT OR IGNORE INTO system(name,value) VALUES(?,?);");
            try {
                int valueForVibrateSetting = AudioSystem.getValueForVibrateSetting(0, 1, 2);
                loadSetting(sQLiteStatementCompileStatement, "vibrate_on", Integer.valueOf(valueForVibrateSetting | AudioSystem.getValueForVibrateSetting(valueForVibrateSetting, 0, 2)));
                if (sQLiteStatementCompileStatement != null) {
                    sQLiteStatementCompileStatement.close();
                }
            } catch (Throwable th2) {
                th = th2;
                if (sQLiteStatementCompileStatement != null) {
                    sQLiteStatementCompileStatement.close();
                }
                throw th;
            }
        } catch (Throwable th3) {
            th = th3;
            sQLiteStatementCompileStatement = null;
        }
    }

    private void loadVibrateWhenRingingSetting(SQLiteDatabase sQLiteDatabase) throws Throwable {
        Throwable th;
        SQLiteStatement sQLiteStatementCompileStatement;
        int i = (getIntValueFromSystem(sQLiteDatabase, "vibrate_on", 0) & 3) == 1 ? 1 : 0;
        try {
            sQLiteStatementCompileStatement = sQLiteDatabase.compileStatement("INSERT OR IGNORE INTO system(name,value) VALUES(?,?);");
            try {
                loadSetting(sQLiteStatementCompileStatement, "vibrate_when_ringing", Integer.valueOf(i));
                if (sQLiteStatementCompileStatement != null) {
                    sQLiteStatementCompileStatement.close();
                }
            } catch (Throwable th2) {
                th = th2;
                if (sQLiteStatementCompileStatement != null) {
                    sQLiteStatementCompileStatement.close();
                }
                throw th;
            }
        } catch (Throwable th3) {
            th = th3;
            sQLiteStatementCompileStatement = null;
        }
    }

    private void loadSettings(SQLiteDatabase sQLiteDatabase) throws Throwable {
        loadSystemSettings(sQLiteDatabase);
        loadSecureSettings(sQLiteDatabase);
        if (this.mUserHandle == 0) {
            loadGlobalSettings(sQLiteDatabase);
        }
    }

    private void loadSystemSettings(SQLiteDatabase sQLiteDatabase) throws Throwable {
        Throwable th;
        SQLiteStatement sQLiteStatementCompileStatement;
        try {
            sQLiteStatementCompileStatement = sQLiteDatabase.compileStatement("INSERT OR IGNORE INTO system(name,value) VALUES(?,?);");
            try {
                loadBooleanSetting(sQLiteStatementCompileStatement, "dim_screen", R.bool.def_dim_screen);
                loadIntegerSetting(sQLiteStatementCompileStatement, "screen_off_timeout", R.integer.def_screen_off_timeout);
                loadSetting(sQLiteStatementCompileStatement, "dtmf_tone_type", 0);
                loadSetting(sQLiteStatementCompileStatement, "hearing_aid", 0);
                loadSetting(sQLiteStatementCompileStatement, "tty_mode", 0);
                loadIntegerSetting(sQLiteStatementCompileStatement, "screen_brightness", R.integer.def_screen_brightness);
                loadIntegerSetting(sQLiteStatementCompileStatement, "screen_brightness_for_vr", android.R.integer.config_doubleTapPowerGestureMultiTargetDefaultAction);
                loadBooleanSetting(sQLiteStatementCompileStatement, "screen_brightness_mode", R.bool.def_screen_brightness_automatic_mode);
                loadBooleanSetting(sQLiteStatementCompileStatement, "accelerometer_rotation", R.bool.def_accelerometer_rotation);
                loadDefaultHapticSettings(sQLiteStatementCompileStatement);
                loadBooleanSetting(sQLiteStatementCompileStatement, "notification_light_pulse", R.bool.def_notification_pulse);
                loadUISoundEffectsSettings(sQLiteStatementCompileStatement);
                loadIntegerSetting(sQLiteStatementCompileStatement, "pointer_speed", R.integer.def_pointer_speed);
                this.mUtils.loadCustomSystemSettings(sQLiteStatementCompileStatement);
                loadSetting(sQLiteStatementCompileStatement, "time_12_24", "24");
                if (sQLiteStatementCompileStatement != null) {
                    sQLiteStatementCompileStatement.close();
                }
            } catch (Throwable th2) {
                th = th2;
                if (sQLiteStatementCompileStatement != null) {
                    sQLiteStatementCompileStatement.close();
                }
                throw th;
            }
        } catch (Throwable th3) {
            th = th3;
            sQLiteStatementCompileStatement = null;
        }
    }

    private void loadUISoundEffectsSettings(SQLiteStatement sQLiteStatement) {
        loadBooleanSetting(sQLiteStatement, "dtmf_tone", R.bool.def_dtmf_tones_enabled);
        loadBooleanSetting(sQLiteStatement, "sound_effects_enabled", R.bool.def_sound_effects_enabled);
        loadSetting(sQLiteStatement, "haptic_feedback_enabled", this.mUtils.getBooleanValue("haptic_feedback_enabled", R.bool.def_haptic_feedback));
        loadIntegerSetting(sQLiteStatement, "lockscreen_sounds_enabled", R.integer.def_lockscreen_sounds_enabled);
    }

    private void loadDefaultAnimationSettings(SQLiteStatement sQLiteStatement) {
        loadFractionSetting(sQLiteStatement, "window_animation_scale", R.fraction.def_window_animation_scale, 1);
        loadFractionSetting(sQLiteStatement, "transition_animation_scale", R.fraction.def_window_transition_scale, 1);
    }

    private void loadDefaultHapticSettings(SQLiteStatement sQLiteStatement) {
        loadSetting(sQLiteStatement, "haptic_feedback_enabled", this.mUtils.getBooleanValue("haptic_feedback_enabled", R.bool.def_haptic_feedback));
    }

    private void loadSecureSettings(SQLiteDatabase sQLiteDatabase) throws Throwable {
        Throwable th;
        SQLiteStatement sQLiteStatementCompileStatement;
        try {
            sQLiteStatementCompileStatement = sQLiteDatabase.compileStatement("INSERT OR IGNORE INTO secure(name,value) VALUES(?,?);");
            try {
                loadSetting(sQLiteStatementCompileStatement, "location_providers_allowed", this.mUtils.getStringValue("location_providers_allowed", R.string.def_location_providers_allowed));
                loadSetting(sQLiteStatementCompileStatement, "mock_location", Integer.valueOf("1".equals(SystemProperties.get("ro.allow.mock.location")) ? 1 : 0));
                loadSecure35Settings(sQLiteStatementCompileStatement);
                loadBooleanSetting(sQLiteStatementCompileStatement, "mount_play_not_snd", R.bool.def_mount_play_notification_snd);
                loadBooleanSetting(sQLiteStatementCompileStatement, "mount_ums_autostart", R.bool.def_mount_ums_autostart);
                loadBooleanSetting(sQLiteStatementCompileStatement, "mount_ums_prompt", R.bool.def_mount_ums_prompt);
                loadBooleanSetting(sQLiteStatementCompileStatement, "mount_ums_notify_enabled", R.bool.def_mount_ums_notify_enabled);
                loadIntegerSetting(sQLiteStatementCompileStatement, "long_press_timeout", R.integer.def_long_press_timeout_millis);
                loadBooleanSetting(sQLiteStatementCompileStatement, "touch_exploration_enabled", R.bool.def_touch_exploration_enabled);
                loadBooleanSetting(sQLiteStatementCompileStatement, "speak_password", R.bool.def_accessibility_speak_password);
                if (SystemProperties.getBoolean("ro.lockscreen.disable.default", false)) {
                    loadSetting(sQLiteStatementCompileStatement, "lockscreen.disabled", "1");
                } else {
                    loadBooleanSetting(sQLiteStatementCompileStatement, "lockscreen.disabled", R.bool.def_lockscreen_disabled);
                }
                loadBooleanSetting(sQLiteStatementCompileStatement, "screensaver_enabled", android.R.^attr-private.expandActivityOverflowButtonDrawable);
                loadBooleanSetting(sQLiteStatementCompileStatement, "screensaver_activate_on_dock", android.R.^attr-private.errorMessageAboveBackground);
                loadBooleanSetting(sQLiteStatementCompileStatement, "screensaver_activate_on_sleep", android.R.^attr-private.errorMessageBackground);
                loadStringSetting(sQLiteStatementCompileStatement, "screensaver_components", android.R.string.adbwifi_active_notification_message);
                loadStringSetting(sQLiteStatementCompileStatement, "screensaver_default_component", android.R.string.adbwifi_active_notification_message);
                loadBooleanSetting(sQLiteStatementCompileStatement, "accessibility_display_magnification_enabled", R.bool.def_accessibility_display_magnification_enabled);
                loadFractionSetting(sQLiteStatementCompileStatement, "accessibility_display_magnification_scale", R.fraction.def_accessibility_display_magnification_scale, 1);
                loadBooleanSetting(sQLiteStatementCompileStatement, "user_setup_complete", R.bool.def_user_setup_complete);
                loadStringSetting(sQLiteStatementCompileStatement, "immersive_mode_confirmations", R.string.def_immersive_mode_confirmations);
                loadBooleanSetting(sQLiteStatementCompileStatement, "install_non_market_apps", R.bool.def_install_non_market_apps);
                loadBooleanSetting(sQLiteStatementCompileStatement, "wake_gesture_enabled", R.bool.def_wake_gesture_enabled);
                loadIntegerSetting(sQLiteStatementCompileStatement, "lock_screen_show_notifications", R.integer.def_lock_screen_show_notifications);
                loadBooleanSetting(sQLiteStatementCompileStatement, "lock_screen_allow_private_notifications", R.bool.def_lock_screen_allow_private_notifications);
                loadIntegerSetting(sQLiteStatementCompileStatement, "sleep_timeout", R.integer.def_sleep_timeout);
                loadIntegerSetting(sQLiteStatementCompileStatement, "theme_mode", R.integer.def_theme_mode);
                this.mUtils.loadCustomSecureSettings(sQLiteStatementCompileStatement);
                if (sQLiteStatementCompileStatement != null) {
                    sQLiteStatementCompileStatement.close();
                }
            } catch (Throwable th2) {
                th = th2;
                if (sQLiteStatementCompileStatement != null) {
                    sQLiteStatementCompileStatement.close();
                }
                throw th;
            }
        } catch (Throwable th3) {
            th = th3;
            sQLiteStatementCompileStatement = null;
        }
    }

    private void loadSecure35Settings(SQLiteStatement sQLiteStatement) {
        loadBooleanSetting(sQLiteStatement, "backup_enabled", R.bool.def_backup_enabled);
        loadStringSetting(sQLiteStatement, "backup_transport", R.string.def_backup_transport);
    }

    private void loadGlobalSettings(SQLiteDatabase sQLiteDatabase) throws Throwable {
        Throwable th;
        SQLiteStatement sQLiteStatementCompileStatement;
        try {
            sQLiteStatementCompileStatement = sQLiteDatabase.compileStatement("INSERT OR IGNORE INTO global(name,value) VALUES(?,?);");
            try {
                loadBooleanSetting(sQLiteStatementCompileStatement, "airplane_mode_on", R.bool.def_airplane_mode_on);
                loadBooleanSetting(sQLiteStatementCompileStatement, "theater_mode_on", R.bool.def_theater_mode_on);
                loadStringSetting(sQLiteStatementCompileStatement, "airplane_mode_radios", R.string.def_airplane_mode_radios);
                loadStringSetting(sQLiteStatementCompileStatement, "airplane_mode_toggleable_radios", R.string.airplane_mode_toggleable_radios);
                loadBooleanSetting(sQLiteStatementCompileStatement, "assisted_gps_enabled", R.bool.assisted_gps_enabled);
                loadSetting(sQLiteStatementCompileStatement, "auto_time", this.mUtils.getBooleanValue("auto_time", R.bool.def_auto_time));
                loadSetting(sQLiteStatementCompileStatement, "auto_time_zone", this.mUtils.getBooleanValue("auto_time_zone", R.bool.def_auto_time_zone));
                loadSetting(sQLiteStatementCompileStatement, "stay_on_while_plugged_in", Integer.valueOf(("1".equals(SystemProperties.get("ro.kernel.qemu")) || this.mContext.getResources().getBoolean(R.bool.def_stay_on_while_plugged_in)) ? 1 : 0));
                loadIntegerSetting(sQLiteStatementCompileStatement, "wifi_sleep_policy", R.integer.def_wifi_sleep_policy);
                loadSetting(sQLiteStatementCompileStatement, "mode_ringer", 2);
                loadDefaultAnimationSettings(sQLiteStatementCompileStatement);
                loadBooleanSetting(sQLiteStatementCompileStatement, "package_verifier_enable", R.bool.def_package_verifier_enable);
                loadBooleanSetting(sQLiteStatementCompileStatement, "wifi_on", R.bool.def_wifi_on);
                loadBooleanSetting(sQLiteStatementCompileStatement, "wifi_networks_available_notification_on", R.bool.def_networks_available_notification_on);
                loadBooleanSetting(sQLiteStatementCompileStatement, "bluetooth_on", R.bool.def_bluetooth_on);
                loadSetting(sQLiteStatementCompileStatement, "cdma_cell_broadcast_sms", 1);
                loadSetting(sQLiteStatementCompileStatement, "data_roaming", Integer.valueOf("true".equalsIgnoreCase(SystemProperties.get("ro.com.android.dataroaming", "false")) ? 1 : 0));
                loadBooleanSetting(sQLiteStatementCompileStatement, "device_provisioned", R.bool.def_device_provisioned);
                int integer = this.mContext.getResources().getInteger(R.integer.def_download_manager_max_bytes_over_mobile);
                if (integer > 0) {
                    loadSetting(sQLiteStatementCompileStatement, "download_manager_max_bytes_over_mobile", Integer.toString(integer));
                }
                int integer2 = this.mContext.getResources().getInteger(R.integer.def_download_manager_recommended_max_bytes_over_mobile);
                if (integer2 > 0) {
                    loadSetting(sQLiteStatementCompileStatement, "download_manager_recommended_max_bytes_over_mobile", Integer.toString(integer2));
                }
                loadSetting(sQLiteStatementCompileStatement, "mobile_data", Integer.valueOf("true".equalsIgnoreCase(SystemProperties.get("ro.com.android.mobiledata", "true")) ? 1 : 0));
                loadBooleanSetting(sQLiteStatementCompileStatement, "netstats_enabled", R.bool.def_netstats_enabled);
                loadBooleanSetting(sQLiteStatementCompileStatement, "usb_mass_storage_enabled", R.bool.def_usb_mass_storage_enabled);
                loadIntegerSetting(sQLiteStatementCompileStatement, "wifi_max_dhcp_retry_count", R.integer.def_max_dhcp_retries);
                loadBooleanSetting(sQLiteStatementCompileStatement, "wifi_display_on", R.bool.def_wifi_display_on);
                loadStringSetting(sQLiteStatementCompileStatement, "lock_sound", R.string.def_lock_sound);
                loadStringSetting(sQLiteStatementCompileStatement, "unlock_sound", R.string.def_unlock_sound);
                loadStringSetting(sQLiteStatementCompileStatement, "trusted_sound", R.string.def_trusted_sound);
                loadIntegerSetting(sQLiteStatementCompileStatement, "power_sounds_enabled", R.integer.def_power_sounds_enabled);
                loadStringSetting(sQLiteStatementCompileStatement, "low_battery_sound", R.string.def_low_battery_sound);
                loadIntegerSetting(sQLiteStatementCompileStatement, "dock_sounds_enabled", R.integer.def_dock_sounds_enabled);
                loadIntegerSetting(sQLiteStatementCompileStatement, "dock_sounds_enabled_when_accessbility", R.integer.def_dock_sounds_enabled_when_accessibility);
                loadStringSetting(sQLiteStatementCompileStatement, "desk_dock_sound", R.string.def_desk_dock_sound);
                loadStringSetting(sQLiteStatementCompileStatement, "desk_undock_sound", R.string.def_desk_undock_sound);
                loadStringSetting(sQLiteStatementCompileStatement, "car_dock_sound", R.string.def_car_dock_sound);
                loadStringSetting(sQLiteStatementCompileStatement, "car_undock_sound", R.string.def_car_undock_sound);
                loadStringSetting(sQLiteStatementCompileStatement, "wireless_charging_started_sound", R.string.def_wireless_charging_started_sound);
                loadIntegerSetting(sQLiteStatementCompileStatement, "dock_audio_media_enabled", R.integer.def_dock_audio_media_enabled);
                loadSetting(sQLiteStatementCompileStatement, "set_install_location", 0);
                loadSetting(sQLiteStatementCompileStatement, "default_install_location", 0);
                loadSetting(sQLiteStatementCompileStatement, "emergency_tone", 0);
                loadSetting(sQLiteStatementCompileStatement, "call_auto_retry", 0);
                loadSetting(sQLiteStatementCompileStatement, "ota_disable_automatic_update", 1);
                loadSetting(sQLiteStatementCompileStatement, "tether_offload_disabled", 1);
                loadSetting(sQLiteStatementCompileStatement, "show_notification_channel_warnings", 0);
                String str = "";
                for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
                    String telephonyProperty = TelephonyManager.getTelephonyProperty(i, "ro.telephony.default_network", Integer.toString(RILConstants.PREFERRED_NETWORK_MODE));
                    str = i == 0 ? telephonyProperty : str + "," + telephonyProperty;
                }
                loadSetting(sQLiteStatementCompileStatement, "preferred_network_mode", str);
                loadSetting(sQLiteStatementCompileStatement, "subscription_mode", Integer.valueOf(SystemProperties.getInt("ro.telephony.default_cdma_sub", 0)));
                loadIntegerSetting(sQLiteStatementCompileStatement, "low_battery_sound_timeout", R.integer.def_low_battery_sound_timeout);
                loadIntegerSetting(sQLiteStatementCompileStatement, "wifi_scan_always_enabled", R.integer.def_wifi_scan_always_available);
                loadIntegerSetting(sQLiteStatementCompileStatement, "heads_up_notifications_enabled", R.integer.def_heads_up_enabled);
                loadSetting(sQLiteStatementCompileStatement, "device_name", getDefaultDeviceName());
                this.mUtils.loadCustomGlobalSettings(sQLiteStatementCompileStatement);
                loadSetting(sQLiteStatementCompileStatement, "charging_sounds_enabled", 0);
                if (sQLiteStatementCompileStatement != null) {
                    sQLiteStatementCompileStatement.close();
                }
            } catch (Throwable th2) {
                th = th2;
                if (sQLiteStatementCompileStatement != null) {
                    sQLiteStatementCompileStatement.close();
                }
                throw th;
            }
        } catch (Throwable th3) {
            th = th3;
            sQLiteStatementCompileStatement = null;
        }
    }

    private void loadSetting(SQLiteStatement sQLiteStatement, String str, Object obj) {
        sQLiteStatement.bindString(1, str);
        sQLiteStatement.bindString(2, obj.toString());
        sQLiteStatement.execute();
    }

    private void loadStringSetting(SQLiteStatement sQLiteStatement, String str, int i) {
        loadSetting(sQLiteStatement, str, this.mContext.getResources().getString(i));
    }

    private void loadBooleanSetting(SQLiteStatement sQLiteStatement, String str, int i) {
        loadSetting(sQLiteStatement, str, this.mContext.getResources().getBoolean(i) ? "1" : "0");
    }

    private void loadIntegerSetting(SQLiteStatement sQLiteStatement, String str, int i) {
        loadSetting(sQLiteStatement, str, Integer.toString(this.mContext.getResources().getInteger(i)));
    }

    private void loadFractionSetting(SQLiteStatement sQLiteStatement, String str, int i, int i2) {
        loadSetting(sQLiteStatement, str, Float.toString(this.mContext.getResources().getFraction(i, i2, i2)));
    }

    private int getIntValueFromSystem(SQLiteDatabase sQLiteDatabase, String str, int i) {
        return getIntValueFromTable(sQLiteDatabase, "system", str, i);
    }

    private int getIntValueFromTable(SQLiteDatabase sQLiteDatabase, String str, String str2, int i) throws Throwable {
        String stringValueFromTable = getStringValueFromTable(sQLiteDatabase, str, str2, null);
        return stringValueFromTable != null ? Integer.parseInt(stringValueFromTable) : i;
    }

    private String getStringValueFromTable(SQLiteDatabase sQLiteDatabase, String str, String str2, String str3) throws Throwable {
        Cursor cursor = null;
        try {
            Cursor cursorQuery = sQLiteDatabase.query(str, new String[]{"value"}, "name='" + str2 + "'", null, null, null, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst()) {
                        String string = cursorQuery.getString(0);
                        if (string == null) {
                            string = str3;
                        }
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        return string;
                    }
                } catch (Throwable th) {
                    th = th;
                    cursor = cursorQuery;
                    if (cursor != null) {
                        cursor.close();
                    }
                    throw th;
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return str3;
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private String getOldDefaultDeviceName() {
        return this.mContext.getResources().getString(R.string.def_device_name, Build.MANUFACTURER, Build.MODEL);
    }

    private String getDefaultDeviceName() {
        return this.mContext.getResources().getString(R.string.def_device_name_simple, Build.MODEL);
    }
}
