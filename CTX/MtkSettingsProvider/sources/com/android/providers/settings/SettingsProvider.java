package com.android.providers.settings;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.backup.BackupManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.hardware.camera2.utils.ArrayUtils;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.DropBoxManager;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManagerInternal;
import android.provider.Settings;
import android.provider.SettingsValidators;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.ByteStringUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.providers.settings.SettingsState;
import com.android.server.LocalServices;
import com.android.server.SystemConfig;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class SettingsProvider extends ContentProvider {
    private static final String[] ALL_COLUMNS;
    private static final Set<String> CRITICAL_GLOBAL_SETTINGS;
    private static final Set<String> CRITICAL_SECURE_SETTINGS;
    private static final Bundle NULL_SETTING_BUNDLE;
    private static final Set<String> OVERLAY_ALLOWED_GLOBAL_INSTANT_APP_SETTINGS;
    private static final Set<String> OVERLAY_ALLOWED_SECURE_INSTANT_APP_SETTINGS;
    private static final Set<String> OVERLAY_ALLOWED_SYSTEM_INSTANT_APP_SETTINGS;
    private static final Set<String> REMOVED_LEGACY_TABLES = new ArraySet();
    static final Set<String> sGlobalMovedToSecureSettings;
    private static final Set<String> sSecureCloneToManagedSettings;
    static final Set<String> sSecureMovedToGlobalSettings;
    public static final Map<String, String> sSystemCloneFromParentOnDependency;
    private static final Set<String> sSystemCloneToManagedSettings;
    static final Set<String> sSystemMovedToGlobalSettings;
    static final Set<String> sSystemMovedToSecureSettings;

    @GuardedBy("mLock")
    private Handler mHandler;

    @GuardedBy("mLock")
    private HandlerThread mHandlerThread;
    private final Object mLock = new Object();
    private volatile IPackageManager mPackageManager;

    @GuardedBy("mLock")
    private SettingsRegistry mSettingsRegistry;
    private volatile UserManager mUserManager;
    private UserManagerInternal mUserManagerInternal;

    static {
        REMOVED_LEGACY_TABLES.add("favorites");
        REMOVED_LEGACY_TABLES.add("old_favorites");
        REMOVED_LEGACY_TABLES.add("bluetooth_devices");
        REMOVED_LEGACY_TABLES.add("bookmarks");
        REMOVED_LEGACY_TABLES.add("android_metadata");
        ALL_COLUMNS = new String[]{"_id", "name", "value"};
        NULL_SETTING_BUNDLE = Bundle.forPair("value", null);
        OVERLAY_ALLOWED_GLOBAL_INSTANT_APP_SETTINGS = new ArraySet();
        OVERLAY_ALLOWED_SYSTEM_INSTANT_APP_SETTINGS = new ArraySet();
        OVERLAY_ALLOWED_SECURE_INSTANT_APP_SETTINGS = new ArraySet();
        for (String str : Resources.getSystem().getStringArray(android.R.array.cloneable_apps)) {
            OVERLAY_ALLOWED_GLOBAL_INSTANT_APP_SETTINGS.add(str);
        }
        for (String str2 : Resources.getSystem().getStringArray(android.R.array.config_allowedSecureInstantAppSettings)) {
            OVERLAY_ALLOWED_SYSTEM_INSTANT_APP_SETTINGS.add(str2);
        }
        for (String str3 : Resources.getSystem().getStringArray(android.R.array.config_allowedGlobalInstantAppSettings)) {
            OVERLAY_ALLOWED_SECURE_INSTANT_APP_SETTINGS.add(str3);
        }
        CRITICAL_GLOBAL_SETTINGS = new ArraySet();
        CRITICAL_GLOBAL_SETTINGS.add("device_provisioned");
        CRITICAL_SECURE_SETTINGS = new ArraySet();
        CRITICAL_SECURE_SETTINGS.add("user_setup_complete");
        sSecureMovedToGlobalSettings = new ArraySet();
        Settings.Secure.getMovedToGlobalSettings(sSecureMovedToGlobalSettings);
        sSystemMovedToGlobalSettings = new ArraySet();
        Settings.System.getMovedToGlobalSettings(sSystemMovedToGlobalSettings);
        sSystemMovedToSecureSettings = new ArraySet();
        Settings.System.getMovedToSecureSettings(sSystemMovedToSecureSettings);
        sGlobalMovedToSecureSettings = new ArraySet();
        Settings.Global.getMovedToSecureSettings(sGlobalMovedToSecureSettings);
        sSecureCloneToManagedSettings = new ArraySet();
        Settings.Secure.getCloneToManagedProfileSettings(sSecureCloneToManagedSettings);
        sSystemCloneToManagedSettings = new ArraySet();
        Settings.System.getCloneToManagedProfileSettings(sSystemCloneToManagedSettings);
        sSystemCloneFromParentOnDependency = new ArrayMap();
        Settings.System.getCloneFromParentOnValueSettings(sSystemCloneFromParentOnDependency);
    }

    public static int makeKey(int i, int i2) {
        return SettingsState.makeKey(i, i2);
    }

    public static int getTypeFromKey(int i) {
        return SettingsState.getTypeFromKey(i);
    }

    public static int getUserIdFromKey(int i) {
        return SettingsState.getUserIdFromKey(i);
    }

    @Override
    public boolean onCreate() {
        Settings.setInSystemServer();
        ensureAllBackedUpSystemSettingsHaveValidators();
        ensureAllBackedUpGlobalSettingsHaveValidators();
        ensureAllBackedUpSecureSettingsHaveValidators();
        synchronized (this.mLock) {
            this.mUserManager = UserManager.get(getContext());
            this.mUserManagerInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
            this.mPackageManager = AppGlobals.getPackageManager();
            this.mHandlerThread = new HandlerThread("SettingsProvider", 10);
            this.mHandlerThread.start();
            this.mHandler = new Handler(this.mHandlerThread.getLooper());
            this.mSettingsRegistry = new SettingsRegistry();
        }
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                SettingsProvider.lambda$onCreate$0(this.f$0);
            }
        });
        ServiceManager.addService("settings", new SettingsService(this));
        return true;
    }

    public static void lambda$onCreate$0(SettingsProvider settingsProvider) {
        settingsProvider.registerBroadcastReceivers();
        settingsProvider.startWatchingUserRestrictionChanges();
    }

    private void ensureAllBackedUpSystemSettingsHaveValidators() {
        failToBootIfOffendersPresent(getOffenders(concat(Settings.System.SETTINGS_TO_BACKUP, Settings.System.LEGACY_RESTORE_SETTINGS), Settings.System.VALIDATORS), "Settings.System");
    }

    private void ensureAllBackedUpGlobalSettingsHaveValidators() {
        failToBootIfOffendersPresent(getOffenders(concat(Settings.Global.SETTINGS_TO_BACKUP, Settings.Global.LEGACY_RESTORE_SETTINGS), Settings.Global.VALIDATORS), "Settings.Global");
    }

    private void ensureAllBackedUpSecureSettingsHaveValidators() {
        failToBootIfOffendersPresent(getOffenders(concat(Settings.Secure.SETTINGS_TO_BACKUP, Settings.Secure.LEGACY_RESTORE_SETTINGS), Settings.Secure.VALIDATORS), "Settings.Secure");
    }

    private void failToBootIfOffendersPresent(String str, String str2) {
        if (str.length() > 0) {
            throw new RuntimeException("All " + str2 + " settings that are backed up have to have a non-null validator, but those don't: " + str);
        }
    }

    private String getOffenders(String[] strArr, Map<String, SettingsValidators.Validator> map) {
        StringBuilder sb = new StringBuilder();
        for (String str : strArr) {
            if (map.get(str) == null) {
                sb.append(str);
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    private final String[] concat(String[] strArr, String[] strArr2) {
        if (strArr2 == null || strArr2.length == 0) {
            return strArr;
        }
        int length = strArr.length;
        int length2 = strArr2.length;
        String[] strArr3 = new String[length + length2];
        System.arraycopy(strArr, 0, strArr3, 0, length);
        System.arraycopy(strArr2, 0, strArr3, length, length2);
        return strArr3;
    }

    @Override
    public Bundle call(String str, String str2, Bundle bundle) {
        int requestingUserId;
        requestingUserId = getRequestingUserId(bundle);
        switch (str) {
            case "GET_global":
                return packageValueForCallResult(getGlobalSetting(str2), isTrackingGeneration(bundle));
            case "GET_secure":
                return packageValueForCallResult(getSecureSetting(str2, requestingUserId, true), isTrackingGeneration(bundle));
            case "GET_system":
                return packageValueForCallResult(getSystemSetting(str2, requestingUserId), isTrackingGeneration(bundle));
            case "PUT_global":
                insertGlobalSetting(str2, getSettingValue(bundle), getSettingTag(bundle), getSettingMakeDefault(bundle), requestingUserId, false);
                return null;
            case "PUT_secure":
                insertSecureSetting(str2, getSettingValue(bundle), getSettingTag(bundle), getSettingMakeDefault(bundle), requestingUserId, false);
                return null;
            case "PUT_system":
                insertSystemSetting(str2, getSettingValue(bundle), requestingUserId);
                return null;
            case "RESET_global":
                resetGlobalSetting(requestingUserId, getResetModeEnforcingPermission(bundle), getSettingTag(bundle));
                return null;
            case "RESET_secure":
                resetSecureSetting(requestingUserId, getResetModeEnforcingPermission(bundle), getSettingTag(bundle));
                return null;
            default:
                Slog.w("SettingsProvider", "call() with invalid method: " + str);
                return null;
        }
    }

    @Override
    public String getType(Uri uri) {
        Arguments arguments = new Arguments(uri, null, null, true);
        if (TextUtils.isEmpty(arguments.name)) {
            return "vnd.android.cursor.dir/" + arguments.table;
        }
        return "vnd.android.cursor.item/" + arguments.table;
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        byte b = 1;
        Arguments arguments = new Arguments(uri, str, strArr2, true);
        String[] strArrNormalizeProjection = normalizeProjection(strArr);
        if (REMOVED_LEGACY_TABLES.contains(arguments.table)) {
            return new MatrixCursor(strArrNormalizeProjection, 0);
        }
        String str3 = arguments.table;
        int iHashCode = str3.hashCode();
        if (iHashCode != -1243020381) {
            if (iHashCode != -906273929) {
                b = (iHashCode == -887328209 && str3.equals("system")) ? (byte) 2 : (byte) -1;
            } else if (!str3.equals("secure")) {
            }
        } else if (str3.equals("global")) {
            b = 0;
        }
        switch (b) {
            case 0:
                if (arguments.name != null) {
                    return packageSettingForQuery(getGlobalSetting(arguments.name), strArrNormalizeProjection);
                }
                return getAllGlobalSettings(strArr);
            case 1:
                int callingUserId = UserHandle.getCallingUserId();
                if (arguments.name != null) {
                    return packageSettingForQuery(getSecureSetting(arguments.name, callingUserId), strArrNormalizeProjection);
                }
                return getAllSecureSettings(callingUserId, strArr);
            case 2:
                int callingUserId2 = UserHandle.getCallingUserId();
                if (arguments.name != null) {
                    return packageSettingForQuery(getSystemSetting(arguments.name, callingUserId2), strArrNormalizeProjection);
                }
                return getAllSystemSettings(callingUserId2, strArr);
            default:
                throw new IllegalArgumentException("Invalid Uri path:" + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        String validTableOrThrow = getValidTableOrThrow(uri);
        if (REMOVED_LEGACY_TABLES.contains(validTableOrThrow)) {
            return null;
        }
        String asString = contentValues.getAsString("name");
        if (!isKeyValid(asString)) {
            return null;
        }
        String asString2 = contentValues.getAsString("value");
        byte b = -1;
        int iHashCode = validTableOrThrow.hashCode();
        if (iHashCode != -1243020381) {
            if (iHashCode != -906273929) {
                if (iHashCode == -887328209 && validTableOrThrow.equals("system")) {
                    b = 2;
                }
            } else if (validTableOrThrow.equals("secure")) {
                b = 1;
            }
        } else if (validTableOrThrow.equals("global")) {
            b = 0;
        }
        switch (b) {
            case 0:
                if (insertGlobalSetting(asString, asString2, null, false, UserHandle.getCallingUserId(), false)) {
                    return Uri.withAppendedPath(Settings.Global.CONTENT_URI, asString);
                }
                return null;
            case 1:
                if (insertSecureSetting(asString, asString2, null, false, UserHandle.getCallingUserId(), false)) {
                    return Uri.withAppendedPath(Settings.Secure.CONTENT_URI, asString);
                }
                return null;
            case 2:
                if (insertSystemSetting(asString, asString2, UserHandle.getCallingUserId())) {
                    return Uri.withAppendedPath(Settings.System.CONTENT_URI, asString);
                }
                return null;
            default:
                throw new IllegalArgumentException("Bad Uri path:" + uri);
        }
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] contentValuesArr) {
        int i = 0;
        for (ContentValues contentValues : contentValuesArr) {
            if (insert(uri, contentValues) != null) {
                i++;
            }
        }
        return i;
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        byte b;
        Arguments arguments = new Arguments(uri, str, strArr, false);
        if (REMOVED_LEGACY_TABLES.contains(arguments.table) || !isKeyValid(arguments.name)) {
            return 0;
        }
        String str2 = arguments.table;
        int iHashCode = str2.hashCode();
        if (iHashCode != -1243020381) {
            if (iHashCode != -906273929) {
                b = (iHashCode == -887328209 && str2.equals("system")) ? (byte) 2 : (byte) -1;
            } else if (str2.equals("secure")) {
                b = 1;
            }
        } else if (str2.equals("global")) {
            b = 0;
        }
        switch (b) {
            case 0:
                return deleteGlobalSetting(arguments.name, UserHandle.getCallingUserId(), false) ? 1 : 0;
            case 1:
                return deleteSecureSetting(arguments.name, UserHandle.getCallingUserId(), false) ? 1 : 0;
            case 2:
                return deleteSystemSetting(arguments.name, UserHandle.getCallingUserId()) ? 1 : 0;
            default:
                throw new IllegalArgumentException("Bad Uri path:" + uri);
        }
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        byte b;
        Arguments arguments = new Arguments(uri, str, strArr, false);
        if (REMOVED_LEGACY_TABLES.contains(arguments.table) || !isKeyValid(contentValues.getAsString("name"))) {
            return 0;
        }
        String asString = contentValues.getAsString("value");
        String str2 = arguments.table;
        int iHashCode = str2.hashCode();
        if (iHashCode != -1243020381) {
            if (iHashCode != -906273929) {
                b = (iHashCode == -887328209 && str2.equals("system")) ? (byte) 2 : (byte) -1;
            } else if (str2.equals("secure")) {
                b = 1;
            }
        } else if (str2.equals("global")) {
            b = 0;
        }
        switch (b) {
            case 0:
                return updateGlobalSetting(arguments.name, asString, null, false, UserHandle.getCallingUserId(), false) ? 1 : 0;
            case 1:
                return updateSecureSetting(arguments.name, asString, null, false, UserHandle.getCallingUserId(), false) ? 1 : 0;
            case 2:
                return updateSystemSetting(arguments.name, asString, UserHandle.getCallingUserId()) ? 1 : 0;
            default:
                throw new IllegalArgumentException("Invalid Uri path:" + uri);
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String str) throws FileNotFoundException {
        String str2;
        String str3;
        int iResolveOwningUserIdForSystemSettingLocked;
        int userIdFromUri = getUserIdFromUri(uri, UserHandle.getCallingUserId());
        if (userIdFromUri != UserHandle.getCallingUserId()) {
            getContext().enforceCallingPermission("android.permission.INTERACT_ACROSS_USERS", "Access files from the settings of another user");
        }
        Uri uriWithoutUserId = ContentProvider.getUriWithoutUserId(uri);
        if (Settings.System.RINGTONE_CACHE_URI.equals(uriWithoutUserId)) {
            str2 = "ringtone";
            str3 = "ringtone_cache";
        } else if (Settings.System.NOTIFICATION_SOUND_CACHE_URI.equals(uriWithoutUserId)) {
            str2 = "notification_sound";
            str3 = "notification_sound_cache";
        } else if (Settings.System.ALARM_ALERT_CACHE_URI.equals(uriWithoutUserId)) {
            str2 = "alarm_alert";
            str3 = "alarm_alert_cache";
        } else {
            throw new FileNotFoundException("Direct file access no longer supported; ringtone playback is available through android.media.Ringtone");
        }
        synchronized (this.mLock) {
            iResolveOwningUserIdForSystemSettingLocked = resolveOwningUserIdForSystemSettingLocked(userIdFromUri, str2);
        }
        return ParcelFileDescriptor.open(new File(getRingtoneCacheDir(iResolveOwningUserIdForSystemSettingLocked), str3), ParcelFileDescriptor.parseMode(str));
    }

    private File getRingtoneCacheDir(int i) {
        File file = new File(Environment.getDataSystemDeDirectory(i), "ringtones");
        file.mkdir();
        SELinux.restorecon(file);
        return file;
    }

    void dumpProto(FileDescriptor fileDescriptor) {
        ProtoOutputStream protoOutputStream = new ProtoOutputStream(fileDescriptor);
        synchronized (this.mLock) {
            SettingsProtoDumpUtil.dumpProtoLocked(this.mSettingsRegistry, protoOutputStream);
        }
        protoOutputStream.flush();
    }

    public void dumpInternal(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        synchronized (this.mLock) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                SparseBooleanArray knownUsersLocked = this.mSettingsRegistry.getKnownUsersLocked();
                int size = knownUsersLocked.size();
                for (int i = 0; i < size; i++) {
                    dumpForUserLocked(knownUsersLocked.keyAt(i), printWriter);
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    private void dumpForUserLocked(int i, PrintWriter printWriter) {
        if (i == 0) {
            printWriter.println("GLOBAL SETTINGS (user " + i + ")");
            SettingsState settingsLocked = this.mSettingsRegistry.getSettingsLocked(0, 0);
            if (settingsLocked != null) {
                dumpSettingsLocked(settingsLocked, printWriter);
                printWriter.println();
                settingsLocked.dumpHistoricalOperations(printWriter);
            }
        }
        printWriter.println("SECURE SETTINGS (user " + i + ")");
        SettingsState settingsLocked2 = this.mSettingsRegistry.getSettingsLocked(2, i);
        if (settingsLocked2 != null) {
            dumpSettingsLocked(settingsLocked2, printWriter);
            printWriter.println();
            settingsLocked2.dumpHistoricalOperations(printWriter);
        }
        printWriter.println("SYSTEM SETTINGS (user " + i + ")");
        SettingsState settingsLocked3 = this.mSettingsRegistry.getSettingsLocked(1, i);
        if (settingsLocked3 != null) {
            dumpSettingsLocked(settingsLocked3, printWriter);
            printWriter.println();
            settingsLocked3.dumpHistoricalOperations(printWriter);
        }
    }

    private void dumpSettingsLocked(SettingsState settingsState, PrintWriter printWriter) {
        List<String> settingNamesLocked = settingsState.getSettingNamesLocked();
        int size = settingNamesLocked.size();
        for (int i = 0; i < size; i++) {
            String str = settingNamesLocked.get(i);
            SettingsState.Setting settingLocked = settingsState.getSettingLocked(str);
            printWriter.print("_id:");
            printWriter.print(toDumpString(settingLocked.getId()));
            printWriter.print(" name:");
            printWriter.print(toDumpString(str));
            if (settingLocked.getPackageName() != null) {
                printWriter.print(" pkg:");
                printWriter.print(settingLocked.getPackageName());
            }
            printWriter.print(" value:");
            printWriter.print(toDumpString(settingLocked.getValue()));
            if (settingLocked.getDefaultValue() != null) {
                printWriter.print(" default:");
                printWriter.print(settingLocked.getDefaultValue());
                printWriter.print(" defaultSystemSet:");
                printWriter.print(settingLocked.isDefaultFromSystem());
            }
            if (settingLocked.getTag() != null) {
                printWriter.print(" tag:");
                printWriter.print(settingLocked.getTag());
            }
            printWriter.println();
        }
    }

    private static String toDumpString(String str) {
        if (str != null) {
            return str;
        }
        return "{null}";
    }

    private void registerBroadcastReceivers() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        intentFilter.addAction("android.intent.action.USER_STOPPED");
        getContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                byte b;
                int intExtra = intent.getIntExtra("android.intent.extra.user_handle", 0);
                String action = intent.getAction();
                int iHashCode = action.hashCode();
                if (iHashCode != -2061058799) {
                    b = (iHashCode == -742246786 && action.equals("android.intent.action.USER_STOPPED")) ? (byte) 1 : (byte) -1;
                } else if (action.equals("android.intent.action.USER_REMOVED")) {
                    b = 0;
                }
                switch (b) {
                    case 0:
                        synchronized (SettingsProvider.this.mLock) {
                            SettingsProvider.this.mSettingsRegistry.removeUserStateLocked(intExtra, true);
                            break;
                        }
                        return;
                    case 1:
                        synchronized (SettingsProvider.this.mLock) {
                            SettingsProvider.this.mSettingsRegistry.removeUserStateLocked(intExtra, false);
                            break;
                        }
                        return;
                    default:
                        return;
                }
            }
        }, intentFilter);
        new PackageMonitor() {
            public void onPackageRemoved(String str, int i) {
                synchronized (SettingsProvider.this.mLock) {
                    SettingsProvider.this.mSettingsRegistry.onPackageRemovedLocked(str, UserHandle.getUserId(i));
                }
            }

            public void onUidRemoved(int i) {
                synchronized (SettingsProvider.this.mLock) {
                    SettingsProvider.this.mSettingsRegistry.onUidRemovedLocked(i);
                }
            }
        }.register(getContext(), BackgroundThread.getHandler().getLooper(), UserHandle.ALL, true);
    }

    private void startWatchingUserRestrictionChanges() {
        ((UserManagerInternal) LocalServices.getService(UserManagerInternal.class)).addUserRestrictionsListener(new UserManagerInternal.UserRestrictionsListener() {
            public final void onUserRestrictionsChanged(int i, Bundle bundle, Bundle bundle2) {
                SettingsProvider.lambda$startWatchingUserRestrictionChanges$1(this.f$0, i, bundle, bundle2);
            }
        });
    }

    public static void lambda$startWatchingUserRestrictionChanges$1(SettingsProvider settingsProvider, int i, Bundle bundle, Bundle bundle2) {
        long jClearCallingIdentity;
        if (bundle.getBoolean("no_share_location") != bundle2.getBoolean("no_share_location")) {
            jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (settingsProvider.mLock) {
                    SettingsState.Setting secureSetting = settingsProvider.getSecureSetting("location_providers_allowed", i);
                    settingsProvider.updateSecureSetting("location_providers_allowed", secureSetting != null ? secureSetting.getValue() : null, null, true, i, true);
                }
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            } finally {
            }
        }
        if (bundle.getBoolean("no_install_unknown_sources") != bundle2.getBoolean("no_install_unknown_sources")) {
            jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (settingsProvider.mLock) {
                    SettingsState.Setting globalSetting = settingsProvider.getGlobalSetting("install_non_market_apps");
                    settingsProvider.updateGlobalSetting("install_non_market_apps", globalSetting != null ? globalSetting.getValue() : null, null, true, i, true);
                }
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            } finally {
            }
        }
        if (bundle.getBoolean("no_debugging_features") != bundle2.getBoolean("no_debugging_features")) {
            jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (settingsProvider.mLock) {
                    SettingsState.Setting globalSetting2 = settingsProvider.getGlobalSetting("adb_enabled");
                    settingsProvider.updateGlobalSetting("adb_enabled", globalSetting2 != null ? globalSetting2.getValue() : null, null, true, i, true);
                }
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            } finally {
            }
        }
        if (bundle.getBoolean("ensure_verify_apps") != bundle2.getBoolean("ensure_verify_apps")) {
            jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (settingsProvider.mLock) {
                    SettingsState.Setting globalSetting3 = settingsProvider.getGlobalSetting("package_verifier_enable");
                    settingsProvider.updateGlobalSetting("package_verifier_enable", globalSetting3 != null ? globalSetting3.getValue() : null, null, true, i, true);
                    SettingsState.Setting globalSetting4 = settingsProvider.getGlobalSetting("verifier_verify_adb_installs");
                    settingsProvider.updateGlobalSetting("verifier_verify_adb_installs", globalSetting4 != null ? globalSetting4.getValue() : null, null, true, i, true);
                }
            } finally {
            }
        }
        if (bundle.getBoolean("no_config_mobile_networks") != bundle2.getBoolean("no_config_mobile_networks")) {
            jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (settingsProvider.mLock) {
                    SettingsState.Setting globalSetting5 = settingsProvider.getGlobalSetting("preferred_network_mode");
                    settingsProvider.updateGlobalSetting("preferred_network_mode", globalSetting5 != null ? globalSetting5.getValue() : null, null, true, i, true);
                }
            } finally {
            }
        }
    }

    private Cursor getAllGlobalSettings(String[] strArr) {
        MatrixCursor matrixCursor;
        synchronized (this.mLock) {
            SettingsState settingsLocked = this.mSettingsRegistry.getSettingsLocked(0, 0);
            List<String> settingsNamesLocked = getSettingsNamesLocked(0, 0);
            int size = settingsNamesLocked.size();
            matrixCursor = new MatrixCursor(normalizeProjection(strArr), size);
            for (int i = 0; i < size; i++) {
                appendSettingToCursor(matrixCursor, settingsLocked.getSettingLocked(settingsNamesLocked.get(i)));
            }
        }
        return matrixCursor;
    }

    private SettingsState.Setting getGlobalSetting(String str) {
        SettingsState.Setting settingLocked;
        enforceSettingReadable(str, 0, UserHandle.getCallingUserId());
        synchronized (this.mLock) {
            settingLocked = this.mSettingsRegistry.getSettingLocked(0, 0, str);
        }
        return settingLocked;
    }

    private boolean updateGlobalSetting(String str, String str2, String str3, boolean z, int i, boolean z2) {
        return mutateGlobalSetting(str, str2, str3, z, i, 3, z2, 0);
    }

    private boolean insertGlobalSetting(String str, String str2, String str3, boolean z, int i, boolean z2) {
        return mutateGlobalSetting(str, str2, str3, z, i, 1, z2, 0);
    }

    private boolean deleteGlobalSetting(String str, int i, boolean z) {
        return mutateGlobalSetting(str, null, null, false, i, 2, z, 0);
    }

    private void resetGlobalSetting(int i, int i2, String str) {
        mutateGlobalSetting(null, null, str, false, i, 4, false, i2);
    }

    private boolean mutateGlobalSetting(String str, String str2, String str3, boolean z, int i, int i2, boolean z2, int i3) {
        String str4;
        enforceWritePermission("android.permission.WRITE_SECURE_SETTINGS");
        int iResolveCallingUserIdEnforcingPermissionsLocked = resolveCallingUserIdEnforcingPermissionsLocked(i);
        if (str != null) {
            str4 = str2;
            if (this.mUserManagerInternal.isSettingRestrictedForUser(str, iResolveCallingUserIdEnforcingPermissionsLocked, str4, Binder.getCallingUid())) {
                return false;
            }
        } else {
            str4 = str2;
        }
        synchronized (this.mLock) {
            switch (i2) {
                case 1:
                    return this.mSettingsRegistry.insertSettingLocked(0, 0, str, str4, str3, z, getCallingPackage(), z2, CRITICAL_GLOBAL_SETTINGS);
                case 2:
                    return this.mSettingsRegistry.deleteSettingLocked(0, 0, str, z2, CRITICAL_GLOBAL_SETTINGS);
                case 3:
                    return this.mSettingsRegistry.updateSettingLocked(0, 0, str, str4, str3, z, getCallingPackage(), z2, CRITICAL_GLOBAL_SETTINGS);
                case 4:
                    this.mSettingsRegistry.resetSettingsLocked(0, 0, getCallingPackage(), i3, str3);
                    return true;
                default:
                    return false;
            }
        }
    }

    private PackageInfo getCallingPackageInfo(int i) {
        try {
            return this.mPackageManager.getPackageInfo(getCallingPackage(), 64, i);
        } catch (RemoteException e) {
            throw new IllegalStateException("Package " + getCallingPackage() + " doesn't exist");
        }
    }

    private Cursor getAllSecureSettings(int i, String[] strArr) {
        MatrixCursor matrixCursor;
        SettingsState.Setting settingLocked;
        int iResolveCallingUserIdEnforcingPermissionsLocked = resolveCallingUserIdEnforcingPermissionsLocked(i);
        PackageInfo callingPackageInfo = getCallingPackageInfo(resolveOwningUserIdForSecureSettingLocked(iResolveCallingUserIdEnforcingPermissionsLocked, "android_id"));
        synchronized (this.mLock) {
            List<String> settingsNamesLocked = getSettingsNamesLocked(2, iResolveCallingUserIdEnforcingPermissionsLocked);
            int size = settingsNamesLocked.size();
            matrixCursor = new MatrixCursor(normalizeProjection(strArr), size);
            for (int i2 = 0; i2 < size; i2++) {
                String str = settingsNamesLocked.get(i2);
                int iResolveOwningUserIdForSecureSettingLocked = resolveOwningUserIdForSecureSettingLocked(iResolveCallingUserIdEnforcingPermissionsLocked, str);
                if (isSecureSettingAccessible(str, iResolveCallingUserIdEnforcingPermissionsLocked, iResolveOwningUserIdForSecureSettingLocked)) {
                    if (!isNewSsaidSetting(str)) {
                        settingLocked = this.mSettingsRegistry.getSettingLocked(2, iResolveOwningUserIdForSecureSettingLocked, str);
                    } else {
                        settingLocked = getSsaidSettingLocked(callingPackageInfo, iResolveOwningUserIdForSecureSettingLocked);
                    }
                    appendSettingToCursor(matrixCursor, settingLocked);
                }
            }
        }
        return matrixCursor;
    }

    private SettingsState.Setting getSecureSetting(String str, int i) {
        return getSecureSetting(str, i, false);
    }

    private SettingsState.Setting getSecureSetting(String str, int i, boolean z) {
        SettingsState.Setting settingLocked;
        SettingsState.Setting locationProvidersAllowedSetting;
        SettingsState.Setting ssaidSettingLocked;
        int iResolveCallingUserIdEnforcingPermissionsLocked = resolveCallingUserIdEnforcingPermissionsLocked(i);
        enforceSettingReadable(str, 2, UserHandle.getCallingUserId());
        int iResolveOwningUserIdForSecureSettingLocked = resolveOwningUserIdForSecureSettingLocked(iResolveCallingUserIdEnforcingPermissionsLocked, str);
        if (!isSecureSettingAccessible(str, iResolveCallingUserIdEnforcingPermissionsLocked, iResolveOwningUserIdForSecureSettingLocked)) {
            SettingsState settingsLocked = this.mSettingsRegistry.getSettingsLocked(2, iResolveOwningUserIdForSecureSettingLocked);
            if (settingsLocked != null) {
                return settingsLocked.getNullSetting();
            }
            return null;
        }
        if (isNewSsaidSetting(str)) {
            PackageInfo callingPackageInfo = getCallingPackageInfo(iResolveOwningUserIdForSecureSettingLocked);
            synchronized (this.mLock) {
                ssaidSettingLocked = getSsaidSettingLocked(callingPackageInfo, iResolveOwningUserIdForSecureSettingLocked);
            }
            return ssaidSettingLocked;
        }
        if (z && "location_providers_allowed".equals(str) && (locationProvidersAllowedSetting = getLocationProvidersAllowedSetting(iResolveOwningUserIdForSecureSettingLocked)) != null) {
            return locationProvidersAllowedSetting;
        }
        synchronized (this.mLock) {
            settingLocked = this.mSettingsRegistry.getSettingLocked(2, iResolveOwningUserIdForSecureSettingLocked, str);
        }
        return settingLocked;
    }

    private boolean isNewSsaidSetting(String str) {
        return "android_id".equals(str) && UserHandle.getAppId(Binder.getCallingUid()) >= 10000;
    }

    private SettingsState.Setting getSsaidSettingLocked(PackageInfo packageInfo, int i) {
        String string = Integer.toString(UserHandle.getUid(i, UserHandle.getAppId(Binder.getCallingUid())));
        SettingsState.Setting settingLocked = this.mSettingsRegistry.getSettingLocked(3, i, string);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            try {
                String instantAppAndroidId = this.mPackageManager.getInstantAppAndroidId(packageInfo.packageName, i);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                SettingsState settingsLocked = this.mSettingsRegistry.getSettingsLocked(3, i);
                if (instantAppAndroidId == null) {
                    return (settingLocked == null || settingLocked.isNull() || settingLocked.getValue() == null) ? mascaradeSsaidSetting(settingsLocked, this.mSettingsRegistry.generateSsaidLocked(packageInfo, i)) : mascaradeSsaidSetting(settingsLocked, settingLocked);
                }
                if (settingLocked != null && instantAppAndroidId.equals(settingLocked.getValue())) {
                    return mascaradeSsaidSetting(settingsLocked, settingLocked);
                }
                if (settingsLocked.insertSettingLocked(string, instantAppAndroidId, null, true, packageInfo.packageName)) {
                    return mascaradeSsaidSetting(settingsLocked, this.mSettingsRegistry.getSettingLocked(3, i, string));
                }
                throw new IllegalStateException("Failed to update instant app android id");
            } catch (RemoteException e) {
                Slog.e("SettingsProvider", "Failed to get Instant App Android ID", e);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                return null;
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    private SettingsState.Setting mascaradeSsaidSetting(SettingsState settingsState, SettingsState.Setting setting) {
        if (setting != null) {
            Objects.requireNonNull(settingsState);
            return new SettingsState.Setting(settingsState, setting) {
                {
                    super(setting);
                    Objects.requireNonNull(settingsState);
                }

                @Override
                public int getKey() {
                    return SettingsProvider.makeKey(2, SettingsProvider.getUserIdFromKey(super.getKey()));
                }

                @Override
                public String getName() {
                    return "android_id";
                }
            };
        }
        return null;
    }

    private SettingsState.Setting getLocationProvidersAllowedSetting(int i) {
        synchronized (this.mLock) {
            if (!"1".equals(getGlobalSetting("location_global_kill_switch").getValue())) {
                return null;
            }
            SettingsState settingsLocked = this.mSettingsRegistry.getSettingsLocked(2, i);
            Objects.requireNonNull(settingsLocked);
            return new SettingsState.Setting(settingsLocked, "location_providers_allowed", "", "", "", "", false, "0") {
                {
                    super(str, str, str, str, str, z, str);
                    Objects.requireNonNull(settingsLocked);
                }

                @Override
                public boolean update(String str, boolean z, String str2, String str3, boolean z2) {
                    Slog.wtf("SettingsProvider", "update shoudln't be called on this instance.");
                    return false;
                }
            };
        }
    }

    private boolean insertSecureSetting(String str, String str2, String str3, boolean z, int i, boolean z2) {
        return mutateSecureSetting(str, str2, str3, z, i, 1, z2, 0);
    }

    private boolean deleteSecureSetting(String str, int i, boolean z) {
        return mutateSecureSetting(str, null, null, false, i, 2, z, 0);
    }

    private boolean updateSecureSetting(String str, String str2, String str3, boolean z, int i, boolean z2) {
        return mutateSecureSetting(str, str2, str3, z, i, 3, z2, 0);
    }

    private void resetSecureSetting(int i, int i2, String str) {
        mutateSecureSetting(null, null, str, false, i, 4, false, i2);
    }

    private boolean mutateSecureSetting(String str, String str2, String str3, boolean z, int i, int i2, boolean z2, int i3) {
        String str4;
        enforceWritePermission("android.permission.WRITE_SECURE_SETTINGS");
        int iResolveCallingUserIdEnforcingPermissionsLocked = resolveCallingUserIdEnforcingPermissionsLocked(i);
        if (str != null) {
            str4 = str2;
            if (this.mUserManagerInternal.isSettingRestrictedForUser(str, iResolveCallingUserIdEnforcingPermissionsLocked, str4, Binder.getCallingUid())) {
                return false;
            }
        } else {
            str4 = str2;
        }
        int iResolveOwningUserIdForSecureSettingLocked = resolveOwningUserIdForSecureSettingLocked(iResolveCallingUserIdEnforcingPermissionsLocked, str);
        if (iResolveOwningUserIdForSecureSettingLocked != iResolveCallingUserIdEnforcingPermissionsLocked) {
            return false;
        }
        if ("location_providers_allowed".equals(str)) {
            return updateLocationProvidersAllowedLocked(str4, str3, iResolveOwningUserIdForSecureSettingLocked, z, z2);
        }
        synchronized (this.mLock) {
            switch (i2) {
                case 1:
                    return this.mSettingsRegistry.insertSettingLocked(2, iResolveOwningUserIdForSecureSettingLocked, str, str4, str3, z, getCallingPackage(), z2, CRITICAL_SECURE_SETTINGS);
                case 2:
                    return this.mSettingsRegistry.deleteSettingLocked(2, iResolveOwningUserIdForSecureSettingLocked, str, z2, CRITICAL_SECURE_SETTINGS);
                case 3:
                    return this.mSettingsRegistry.updateSettingLocked(2, iResolveOwningUserIdForSecureSettingLocked, str, str4, str3, z, getCallingPackage(), z2, CRITICAL_SECURE_SETTINGS);
                case 4:
                    this.mSettingsRegistry.resetSettingsLocked(2, 0, getCallingPackage(), i3, str3);
                    return true;
                default:
                    return false;
            }
        }
    }

    private Cursor getAllSystemSettings(int i, String[] strArr) {
        MatrixCursor matrixCursor;
        int iResolveCallingUserIdEnforcingPermissionsLocked = resolveCallingUserIdEnforcingPermissionsLocked(i);
        synchronized (this.mLock) {
            List<String> settingsNamesLocked = getSettingsNamesLocked(1, iResolveCallingUserIdEnforcingPermissionsLocked);
            int size = settingsNamesLocked.size();
            matrixCursor = new MatrixCursor(normalizeProjection(strArr), size);
            for (int i2 = 0; i2 < size; i2++) {
                String str = settingsNamesLocked.get(i2);
                appendSettingToCursor(matrixCursor, this.mSettingsRegistry.getSettingLocked(1, resolveOwningUserIdForSystemSettingLocked(iResolveCallingUserIdEnforcingPermissionsLocked, str), str));
            }
        }
        return matrixCursor;
    }

    private SettingsState.Setting getSystemSetting(String str, int i) {
        SettingsState.Setting settingLocked;
        int iResolveCallingUserIdEnforcingPermissionsLocked = resolveCallingUserIdEnforcingPermissionsLocked(i);
        enforceSettingReadable(str, 1, UserHandle.getCallingUserId());
        int iResolveOwningUserIdForSystemSettingLocked = resolveOwningUserIdForSystemSettingLocked(iResolveCallingUserIdEnforcingPermissionsLocked, str);
        synchronized (this.mLock) {
            settingLocked = this.mSettingsRegistry.getSettingLocked(1, iResolveOwningUserIdForSystemSettingLocked, str);
        }
        return settingLocked;
    }

    private boolean insertSystemSetting(String str, String str2, int i) {
        return mutateSystemSetting(str, str2, i, 1);
    }

    private boolean deleteSystemSetting(String str, int i) {
        return mutateSystemSetting(str, null, i, 2);
    }

    private boolean updateSystemSetting(String str, String str2, int i) {
        return mutateSystemSetting(str, str2, i, 3);
    }

    private boolean mutateSystemSetting(String str, String str2, int i, int i2) {
        String str3;
        if (!hasWriteSecureSettingsPermission() && !Settings.checkAndNoteWriteSettingsOperation(getContext(), Binder.getCallingUid(), getCallingPackage(), true)) {
            return false;
        }
        int iResolveCallingUserIdEnforcingPermissionsLocked = resolveCallingUserIdEnforcingPermissionsLocked(i);
        if (str != null) {
            str3 = str2;
            if (this.mUserManagerInternal.isSettingRestrictedForUser(str, iResolveCallingUserIdEnforcingPermissionsLocked, str3, Binder.getCallingUid())) {
                return false;
            }
        } else {
            str3 = str2;
        }
        enforceRestrictedSystemSettingsMutationForCallingPackage(i2, str, iResolveCallingUserIdEnforcingPermissionsLocked);
        int iResolveOwningUserIdForSystemSettingLocked = resolveOwningUserIdForSystemSettingLocked(iResolveCallingUserIdEnforcingPermissionsLocked, str);
        if (iResolveOwningUserIdForSystemSettingLocked != iResolveCallingUserIdEnforcingPermissionsLocked) {
            return false;
        }
        String str4 = null;
        if ("ringtone".equals(str)) {
            str4 = "ringtone_cache";
        } else if ("notification_sound".equals(str)) {
            str4 = "notification_sound_cache";
        } else if ("alarm_alert".equals(str)) {
            str4 = "alarm_alert_cache";
        }
        if (str4 != null) {
            new File(getRingtoneCacheDir(iResolveOwningUserIdForSystemSettingLocked), str4).delete();
        }
        synchronized (this.mLock) {
            switch (i2) {
                case 1:
                    validateSystemSettingValue(str, str3);
                    return this.mSettingsRegistry.insertSettingLocked(1, iResolveOwningUserIdForSystemSettingLocked, str, str3, null, false, getCallingPackage(), false, null);
                case 2:
                    return this.mSettingsRegistry.deleteSettingLocked(1, iResolveOwningUserIdForSystemSettingLocked, str, false, null);
                case 3:
                    validateSystemSettingValue(str, str3);
                    return this.mSettingsRegistry.updateSettingLocked(1, iResolveOwningUserIdForSystemSettingLocked, str, str3, null, false, getCallingPackage(), false, null);
                default:
                    return false;
            }
        }
    }

    private boolean hasWriteSecureSettingsPermission() {
        if (getContext().checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == 0) {
            return true;
        }
        return false;
    }

    private void validateSystemSettingValue(String str, String str2) {
        SettingsValidators.Validator validator = (SettingsValidators.Validator) Settings.System.VALIDATORS.get(str);
        if (validator != null && !validator.validate(str2)) {
            throw new IllegalArgumentException("Invalid value: " + str2 + " for setting: " + str);
        }
    }

    private boolean isSecureSettingAccessible(String str, int i, int i2) {
        if (isLocationProvidersAllowedRestricted(str, i, i2)) {
            return false;
        }
        byte b = -1;
        if (str.hashCode() == 1713600355 && str.equals("bluetooth_address")) {
            b = 0;
        }
        return b != 0 || getContext().checkCallingOrSelfPermission("android.permission.LOCAL_MAC_ADDRESS") == 0;
    }

    private boolean isLocationProvidersAllowedRestricted(String str, int i, int i2) {
        if (i == i2 || !"location_providers_allowed".equals(str) || !this.mUserManager.hasUserRestriction("no_share_location", new UserHandle(i))) {
            return false;
        }
        return true;
    }

    private int resolveOwningUserIdForSecureSettingLocked(int i, String str) {
        return resolveOwningUserIdLocked(i, sSecureCloneToManagedSettings, str);
    }

    private int resolveOwningUserIdForSystemSettingLocked(int i, String str) {
        int groupParentLocked;
        if (sSystemCloneFromParentOnDependency.containsKey(str) && (groupParentLocked = getGroupParentLocked(i)) != i) {
            String str2 = sSystemCloneFromParentOnDependency.get(str);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                SettingsState.Setting secureSetting = getSecureSetting(str2, i);
                if (secureSetting != null) {
                    if (secureSetting.getValue().equals("1")) {
                        return groupParentLocked;
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
        return resolveOwningUserIdLocked(i, sSystemCloneToManagedSettings, str);
    }

    private int resolveOwningUserIdLocked(int i, Set<String> set, String str) {
        int groupParentLocked = getGroupParentLocked(i);
        if (groupParentLocked != i && set.contains(str)) {
            return groupParentLocked;
        }
        return i;
    }

    private void enforceRestrictedSystemSettingsMutationForCallingPackage(int i, String str, int i2) {
        int appId = UserHandle.getAppId(Binder.getCallingUid());
        Slog.v("SettingsProvider", "name : " + str + " appId : " + appId);
        if (appId == 1000 || appId == 2000 || appId == 0) {
            return;
        }
        switch (i) {
            case 1:
            case 3:
                if (Settings.System.PUBLIC_SETTINGS.contains(str)) {
                    return;
                }
                PackageInfo callingPackageInfoOrThrow = getCallingPackageInfoOrThrow(i2);
                Slog.v("SettingsProvider", "Package name : " + callingPackageInfoOrThrow.packageName + " privateFlags : " + callingPackageInfoOrThrow.applicationInfo.privateFlags);
                if ((callingPackageInfoOrThrow.applicationInfo.privateFlags & 8) != 0) {
                    return;
                }
                warnOrThrowForUndesiredSecureSettingsMutationForTargetSdk(callingPackageInfoOrThrow.applicationInfo.targetSdkVersion, str);
                return;
            case 2:
                if (Settings.System.PUBLIC_SETTINGS.contains(str) || Settings.System.PRIVATE_SETTINGS.contains(str)) {
                    throw new IllegalArgumentException("You cannot delete system defined secure settings.");
                }
                PackageInfo callingPackageInfoOrThrow2 = getCallingPackageInfoOrThrow(i2);
                if ((callingPackageInfoOrThrow2.applicationInfo.privateFlags & 8) != 0) {
                    return;
                }
                warnOrThrowForUndesiredSecureSettingsMutationForTargetSdk(callingPackageInfoOrThrow2.applicationInfo.targetSdkVersion, str);
                return;
            default:
                return;
        }
    }

    private Set<String> getInstantAppAccessibleSettings(int i) {
        switch (i) {
            case 0:
                return Settings.Global.INSTANT_APP_SETTINGS;
            case 1:
                return Settings.System.INSTANT_APP_SETTINGS;
            case 2:
                return Settings.Secure.INSTANT_APP_SETTINGS;
            default:
                throw new IllegalArgumentException("Invalid settings type: " + i);
        }
    }

    private Set<String> getOverlayInstantAppAccessibleSettings(int i) {
        switch (i) {
            case 0:
                return OVERLAY_ALLOWED_GLOBAL_INSTANT_APP_SETTINGS;
            case 1:
                return OVERLAY_ALLOWED_SYSTEM_INSTANT_APP_SETTINGS;
            case 2:
                return OVERLAY_ALLOWED_SECURE_INSTANT_APP_SETTINGS;
            default:
                throw new IllegalArgumentException("Invalid settings type: " + i);
        }
    }

    private List<String> getSettingsNamesLocked(int i, int i2) {
        return this.mSettingsRegistry.getSettingsNamesLocked(i, i2);
    }

    private void enforceSettingReadable(String str, int i, int i2) {
        if (UserHandle.getAppId(Binder.getCallingUid()) < 10000) {
            return;
        }
        ApplicationInfo callingApplicationInfoOrThrow = getCallingApplicationInfoOrThrow();
        if (callingApplicationInfoOrThrow.isInstantApp() && !getInstantAppAccessibleSettings(i).contains(str) && !getOverlayInstantAppAccessibleSettings(i).contains(str)) {
            Slog.w("SettingsProvider", "Instant App " + callingApplicationInfoOrThrow.packageName + " trying to access unexposed setting, this will be an error in the future.");
        }
    }

    private ApplicationInfo getCallingApplicationInfoOrThrow() {
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = this.mPackageManager.getApplicationInfo(getCallingPackage(), 0, UserHandle.getCallingUserId());
        } catch (RemoteException e) {
            applicationInfo = null;
        }
        if (applicationInfo == null) {
            throw new IllegalStateException("Failed to lookup info for package " + getCallingPackage());
        }
        return applicationInfo;
    }

    private PackageInfo getCallingPackageInfoOrThrow(int i) {
        try {
            PackageInfo packageInfo = this.mPackageManager.getPackageInfo(getCallingPackage(), 0, i);
            if (packageInfo != null) {
                return packageInfo;
            }
        } catch (RemoteException e) {
        }
        throw new IllegalStateException("Calling package doesn't exist");
    }

    private int getGroupParentLocked(int i) {
        if (i == 0) {
            return i;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            UserInfo profileParent = this.mUserManager.getProfileParent(i);
            if (profileParent != null) {
                i = profileParent.id;
            }
            return i;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void enforceWritePermission(String str) {
        if (getContext().checkCallingOrSelfPermission(str) != 0) {
            throw new SecurityException("Permission denial: writing to settings requires:" + str);
        }
    }

    private boolean updateLocationProvidersAllowedLocked(String str, String str2, int i, boolean z, boolean z2) {
        SettingsState.Setting secureSetting;
        boolean z3;
        char cCharAt;
        if (TextUtils.isEmpty(str) || (secureSetting = getSecureSetting("location_providers_allowed", i)) == null) {
            return false;
        }
        String value = secureSetting.getValue();
        ArrayList arrayList = TextUtils.isEmpty(value) ? new ArrayList() : new ArrayList(Arrays.asList(value.split(",")));
        ArraySet arraySet = new ArraySet();
        arraySet.addAll(arrayList);
        for (String str3 : str.split(",")) {
            if (!TextUtils.isEmpty(str3) && ((cCharAt = str3.charAt(0)) == '+' || cCharAt == '-')) {
                String strSubstring = str3.substring(1);
                if (cCharAt == '+') {
                    arraySet.add(strSubstring);
                } else if (cCharAt == '-') {
                    arraySet.remove(strSubstring);
                }
            } else {
                z3 = true;
                break;
            }
        }
        z3 = false;
        String strJoin = TextUtils.join(",", arraySet.toArray());
        if (z3 || strJoin.equals(value)) {
            if (z2) {
                this.mSettingsRegistry.notifyForSettingsChange(makeKey(2, i), "location_providers_allowed");
            }
            return false;
        }
        return this.mSettingsRegistry.insertSettingLocked(2, i, "location_providers_allowed", strJoin, str2, z, getCallingPackage(), z2, CRITICAL_SECURE_SETTINGS);
    }

    private static void warnOrThrowForUndesiredSecureSettingsMutationForTargetSdk(int i, String str) {
        if (i <= 22) {
            if (Settings.System.PRIVATE_SETTINGS.contains(str)) {
                Slog.w("SettingsProvider", "You shouldn't not change private system settings. This will soon become an error.");
                return;
            } else {
                Slog.w("SettingsProvider", "You shouldn't keep your settings in the secure settings. This will soon become an error.");
                return;
            }
        }
        if (Settings.System.PRIVATE_SETTINGS.contains(str)) {
            throw new IllegalArgumentException("You cannot change private secure settings.");
        }
        throw new IllegalArgumentException("You cannot keep your settings in the secure settings.");
    }

    private static int resolveCallingUserIdEnforcingPermissionsLocked(int i) {
        if (i == UserHandle.getCallingUserId()) {
            return i;
        }
        return ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), i, false, true, "get/set setting for user", null);
    }

    private Bundle packageValueForCallResult(SettingsState.Setting setting, boolean z) {
        if (!z) {
            if (setting == null || setting.isNull()) {
                return NULL_SETTING_BUNDLE;
            }
            return Bundle.forPair("value", setting.getValue());
        }
        Bundle bundle = new Bundle();
        bundle.putString("value", !setting.isNull() ? setting.getValue() : null);
        this.mSettingsRegistry.mGenerationRegistry.addGenerationData(bundle, setting.getKey());
        Slog.v("SettingsProvider", "packageValueForCallResult, name = " + setting.getName() + ", value : " + bundle);
        return bundle;
    }

    private static int getRequestingUserId(Bundle bundle) {
        int callingUserId = UserHandle.getCallingUserId();
        return bundle != null ? bundle.getInt("_user", callingUserId) : callingUserId;
    }

    private boolean isTrackingGeneration(Bundle bundle) {
        return bundle != null && bundle.containsKey("_track_generation");
    }

    private static String getSettingValue(Bundle bundle) {
        if (bundle != null) {
            return bundle.getString("value");
        }
        return null;
    }

    private static String getSettingTag(Bundle bundle) {
        if (bundle != null) {
            return bundle.getString("_tag");
        }
        return null;
    }

    private static boolean getSettingMakeDefault(Bundle bundle) {
        return bundle != null && bundle.getBoolean("_make_default");
    }

    private static int getResetModeEnforcingPermission(Bundle bundle) {
        int i = bundle != null ? bundle.getInt("_reset_mode") : 0;
        switch (i) {
            case 1:
                return i;
            case 2:
                if (!isCallerSystemOrShellOrRootOnDebuggableBuild()) {
                    throw new SecurityException("Only system, shell/root on a debuggable build can reset to untrusted defaults");
                }
                return i;
            case 3:
                if (!isCallerSystemOrShellOrRootOnDebuggableBuild()) {
                    throw new SecurityException("Only system, shell/root on a debuggable build can reset untrusted changes");
                }
                return i;
            case 4:
                if (!isCallerSystemOrShellOrRootOnDebuggableBuild()) {
                    throw new SecurityException("Only system, shell/root on a debuggable build can reset to trusted defaults");
                }
                return i;
            default:
                throw new IllegalArgumentException("Invalid reset mode: " + i);
        }
    }

    private static boolean isCallerSystemOrShellOrRootOnDebuggableBuild() {
        int appId = UserHandle.getAppId(Binder.getCallingUid());
        return appId == 1000 || (Build.IS_DEBUGGABLE && (appId == 2000 || appId == 0));
    }

    private static String getValidTableOrThrow(Uri uri) {
        if (uri.getPathSegments().size() > 0) {
            String str = uri.getPathSegments().get(0);
            if (DatabaseHelper.isValidTable(str)) {
                return str;
            }
            throw new IllegalArgumentException("Bad root path: " + str);
        }
        throw new IllegalArgumentException("Invalid URI:" + uri);
    }

    private static MatrixCursor packageSettingForQuery(SettingsState.Setting setting, String[] strArr) {
        if (setting.isNull()) {
            return new MatrixCursor(strArr, 0);
        }
        MatrixCursor matrixCursor = new MatrixCursor(strArr, 1);
        appendSettingToCursor(matrixCursor, setting);
        return matrixCursor;
    }

    private static String[] normalizeProjection(String[] strArr) {
        if (strArr == null) {
            return ALL_COLUMNS;
        }
        for (String str : strArr) {
            if (!ArrayUtils.contains(ALL_COLUMNS, str)) {
                throw new IllegalArgumentException("Invalid column: " + str);
            }
        }
        return strArr;
    }

    private static void appendSettingToCursor(MatrixCursor matrixCursor, SettingsState.Setting setting) {
        byte b;
        if (setting == null || setting.isNull()) {
            return;
        }
        int columnCount = matrixCursor.getColumnCount();
        String[] strArr = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            String columnName = matrixCursor.getColumnName(i);
            int iHashCode = columnName.hashCode();
            if (iHashCode != 94650) {
                if (iHashCode != 3373707) {
                    b = (iHashCode == 111972721 && columnName.equals("value")) ? (byte) 2 : (byte) -1;
                } else if (columnName.equals("name")) {
                    b = 1;
                }
            } else if (columnName.equals("_id")) {
                b = 0;
            }
            switch (b) {
                case 0:
                    strArr[i] = setting.getId();
                    break;
                case 1:
                    strArr[i] = setting.getName();
                    break;
                case 2:
                    strArr[i] = setting.getValue();
                    break;
            }
        }
        matrixCursor.addRow(strArr);
    }

    private static boolean isKeyValid(String str) {
        return (TextUtils.isEmpty(str) || SettingsState.isBinary(str)) ? false : true;
    }

    private static final class Arguments {
        public final String name;
        public final String table;
        private static final Pattern WHERE_PATTERN_WITH_PARAM_NO_BRACKETS = Pattern.compile("[\\s]*name[\\s]*=[\\s]*\\?[\\s]*");
        private static final Pattern WHERE_PATTERN_WITH_PARAM_IN_BRACKETS = Pattern.compile("[\\s]*\\([\\s]*name[\\s]*=[\\s]*\\?[\\s]*\\)[\\s]*");
        private static final Pattern WHERE_PATTERN_NO_PARAM_IN_BRACKETS = Pattern.compile("[\\s]*\\([\\s]*name[\\s]*=[\\s]*['\"].*['\"][\\s]*\\)[\\s]*");
        private static final Pattern WHERE_PATTERN_NO_PARAM_NO_BRACKETS = Pattern.compile("[\\s]*name[\\s]*=[\\s]*['\"].*['\"][\\s]*");

        public Arguments(Uri uri, String str, String[] strArr, boolean z) {
            switch (uri.getPathSegments().size()) {
                case 1:
                    if (str != null && ((WHERE_PATTERN_WITH_PARAM_NO_BRACKETS.matcher(str).matches() || WHERE_PATTERN_WITH_PARAM_IN_BRACKETS.matcher(str).matches()) && strArr.length == 1)) {
                        this.name = strArr[0];
                        this.table = computeTableForSetting(uri, this.name);
                        return;
                    }
                    if (str != null && (WHERE_PATTERN_NO_PARAM_NO_BRACKETS.matcher(str).matches() || WHERE_PATTERN_NO_PARAM_IN_BRACKETS.matcher(str).matches())) {
                        this.name = str.substring(Math.max(str.indexOf("'"), str.indexOf("\"")) + 1, Math.max(str.lastIndexOf("'"), str.lastIndexOf("\"")));
                        this.table = computeTableForSetting(uri, this.name);
                        return;
                    } else if (z && str == null && strArr == null) {
                        this.name = null;
                        this.table = computeTableForSetting(uri, null);
                        return;
                    }
                    break;
                case 2:
                    if (str == null && strArr == null) {
                        this.name = uri.getPathSegments().get(1);
                        this.table = computeTableForSetting(uri, this.name);
                        return;
                    }
                    break;
            }
            EventLogTags.writeUnsupportedSettingsQuery(uri.toSafeString(), str, Arrays.toString(strArr));
            throw new IllegalArgumentException(String.format("Supported SQL:\n  uri content://some_table/some_property with null where and where args\n  uri content://some_table with query name=? and single name as arg\n  uri content://some_table with query name=some_name and null args\n  but got - uri:%1s, where:%2s whereArgs:%3s", uri, str, Arrays.toString(strArr)));
        }

        private static String computeTableForSetting(Uri uri, String str) {
            String validTableOrThrow = SettingsProvider.getValidTableOrThrow(uri);
            if (str != null) {
                if (SettingsProvider.sSystemMovedToSecureSettings.contains(str)) {
                    validTableOrThrow = "secure";
                }
                if (SettingsProvider.sSystemMovedToGlobalSettings.contains(str)) {
                    validTableOrThrow = "global";
                }
                if (SettingsProvider.sSecureMovedToGlobalSettings.contains(str)) {
                    validTableOrThrow = "global";
                }
                if (SettingsProvider.sGlobalMovedToSecureSettings.contains(str)) {
                    return "secure";
                }
                return validTableOrThrow;
            }
            return validTableOrThrow;
        }
    }

    final class SettingsRegistry {
        private final BackupManager mBackupManager;
        private GenerationRegistry mGenerationRegistry;
        private final Handler mHandler;
        private String mSettingsCreationBuildId;
        private final SparseArray<SettingsState> mSettingsStates = new SparseArray<>();

        public SettingsRegistry() {
            this.mHandler = new MyHandler(SettingsProvider.this.getContext().getMainLooper());
            this.mGenerationRegistry = new GenerationRegistry(SettingsProvider.this.mLock);
            this.mBackupManager = new BackupManager(SettingsProvider.this.getContext());
            migrateAllLegacySettingsIfNeeded();
            syncSsaidTableOnStart();
        }

        private void generateUserKeyLocked(int i) {
            byte[] bArr = new byte[32];
            new SecureRandom().nextBytes(bArr);
            if (!getSettingsLocked(3, i).insertSettingLocked("userkey", ByteStringUtils.toHexString(bArr), null, true, "android")) {
                throw new IllegalStateException("Ssaid settings not accessible");
            }
        }

        private byte[] getLengthPrefix(byte[] bArr) {
            return ByteBuffer.allocate(4).putInt(bArr.length).array();
        }

        public SettingsState.Setting generateSsaidLocked(PackageInfo packageInfo, int i) {
            SettingsState.Setting settingLocked = getSettingLocked(3, i, "userkey");
            if (settingLocked == null || settingLocked.isNull() || settingLocked.getValue() == null) {
                generateUserKeyLocked(i);
                settingLocked = getSettingLocked(3, i, "userkey");
                if (settingLocked == null || settingLocked.isNull() || settingLocked.getValue() == null) {
                    throw new IllegalStateException("User key not accessible");
                }
            }
            byte[] bArrFromHexToByteArray = ByteStringUtils.fromHexToByteArray(settingLocked.getValue());
            if (bArrFromHexToByteArray == null || (bArrFromHexToByteArray.length != 16 && bArrFromHexToByteArray.length != 32)) {
                throw new IllegalStateException("User key invalid");
            }
            try {
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(bArrFromHexToByteArray, mac.getAlgorithm()));
                for (int i2 = 0; i2 < packageInfo.signatures.length; i2++) {
                    byte[] byteArray = packageInfo.signatures[i2].toByteArray();
                    mac.update(getLengthPrefix(byteArray), 0, 4);
                    mac.update(byteArray);
                }
                String lowerCase = ByteStringUtils.toHexString(mac.doFinal()).substring(0, 16).toLowerCase(Locale.US);
                String string = Integer.toString(packageInfo.applicationInfo.uid);
                if (!getSettingsLocked(3, i).insertSettingLocked(string, lowerCase, null, true, packageInfo.packageName)) {
                    throw new IllegalStateException("Ssaid settings not accessible");
                }
                return getSettingLocked(3, i, string);
            } catch (InvalidKeyException e) {
                throw new IllegalStateException("Key is corrupted", e);
            } catch (NoSuchAlgorithmException e2) {
                throw new IllegalStateException("HmacSHA256 is not available", e2);
            }
        }

        public void syncSsaidTableOnStart() {
            synchronized (SettingsProvider.this.mLock) {
                for (UserInfo userInfo : SettingsProvider.this.mUserManager.getUsers(true)) {
                    try {
                        List list = SettingsProvider.this.mPackageManager.getInstalledPackages(8192, userInfo.id).getList();
                        HashSet hashSet = new HashSet();
                        Iterator it = list.iterator();
                        while (it.hasNext()) {
                            hashSet.add(Integer.toString(((PackageInfo) it.next()).applicationInfo.uid));
                        }
                        HashSet hashSet2 = new HashSet(getSettingsNamesLocked(3, userInfo.id));
                        hashSet2.remove("userkey");
                        hashSet2.removeAll(hashSet);
                        SettingsState settingsLocked = getSettingsLocked(3, userInfo.id);
                        Iterator it2 = hashSet2.iterator();
                        while (it2.hasNext()) {
                            settingsLocked.deleteSettingLocked((String) it2.next());
                        }
                    } catch (RemoteException e) {
                        throw new IllegalStateException("Package manager not available");
                    }
                }
            }
        }

        public List<String> getSettingsNamesLocked(int i, int i2) {
            SettingsState settingsStatePeekSettingsStateLocked = peekSettingsStateLocked(SettingsProvider.makeKey(i, i2));
            if (settingsStatePeekSettingsStateLocked == null) {
                return new ArrayList();
            }
            return settingsStatePeekSettingsStateLocked.getSettingNamesLocked();
        }

        public SparseBooleanArray getKnownUsersLocked() {
            SparseBooleanArray sparseBooleanArray = new SparseBooleanArray();
            for (int size = this.mSettingsStates.size() - 1; size >= 0; size--) {
                sparseBooleanArray.put(SettingsProvider.getUserIdFromKey(this.mSettingsStates.keyAt(size)), true);
            }
            return sparseBooleanArray;
        }

        public SettingsState getSettingsLocked(int i, int i2) {
            return peekSettingsStateLocked(SettingsProvider.makeKey(i, i2));
        }

        public boolean ensureSettingsForUserLocked(int i) throws Throwable {
            if (SettingsProvider.this.mUserManager.getUserInfo(i) == null) {
                Slog.wtf("SettingsProvider", "Requested user " + i + " does not exist");
                return false;
            }
            migrateLegacySettingsForUserIfNeededLocked(i);
            if (i == 0) {
                ensureSettingsStateLocked(SettingsProvider.makeKey(0, 0));
            }
            ensureSettingsStateLocked(SettingsProvider.makeKey(2, i));
            ensureSecureSettingAndroidIdSetLocked(getSettingsLocked(2, i));
            ensureSettingsStateLocked(SettingsProvider.makeKey(1, i));
            ensureSettingsStateLocked(SettingsProvider.makeKey(3, i));
            new UpgradeController(i).upgradeIfNeededLocked();
            return true;
        }

        private void ensureSettingsStateLocked(int i) {
            if (this.mSettingsStates.get(i) == null) {
                this.mSettingsStates.put(i, new SettingsState(SettingsProvider.this.getContext(), SettingsProvider.this.mLock, getSettingsFile(i), i, getMaxBytesPerPackageForType(SettingsProvider.getTypeFromKey(i)), SettingsProvider.this.mHandlerThread.getLooper()));
            }
        }

        public void removeUserStateLocked(int i, boolean z) {
            final int iMakeKey = SettingsProvider.makeKey(1, i);
            SettingsState settingsState = this.mSettingsStates.get(iMakeKey);
            if (settingsState != null) {
                if (z) {
                    this.mSettingsStates.remove(iMakeKey);
                    settingsState.destroyLocked(null);
                } else {
                    settingsState.destroyLocked(new Runnable() {
                        @Override
                        public void run() {
                            SettingsRegistry.this.mSettingsStates.remove(iMakeKey);
                        }
                    });
                }
            }
            final int iMakeKey2 = SettingsProvider.makeKey(2, i);
            SettingsState settingsState2 = this.mSettingsStates.get(iMakeKey2);
            if (settingsState2 != null) {
                if (z) {
                    this.mSettingsStates.remove(iMakeKey2);
                    settingsState2.destroyLocked(null);
                } else {
                    settingsState2.destroyLocked(new Runnable() {
                        @Override
                        public void run() {
                            SettingsRegistry.this.mSettingsStates.remove(iMakeKey2);
                        }
                    });
                }
            }
            final int iMakeKey3 = SettingsProvider.makeKey(3, i);
            SettingsState settingsState3 = this.mSettingsStates.get(iMakeKey3);
            if (settingsState3 != null) {
                if (z) {
                    this.mSettingsStates.remove(iMakeKey3);
                    settingsState3.destroyLocked(null);
                } else {
                    settingsState3.destroyLocked(new Runnable() {
                        @Override
                        public void run() {
                            SettingsRegistry.this.mSettingsStates.remove(iMakeKey3);
                        }
                    });
                }
            }
            this.mGenerationRegistry.onUserRemoved(i);
        }

        public boolean insertSettingLocked(int i, int i2, String str, String str2, String str3, boolean z, String str4, boolean z2, Set<String> set) {
            boolean zInsertSettingLocked;
            int iMakeKey = SettingsProvider.makeKey(i, i2);
            SettingsState settingsStatePeekSettingsStateLocked = peekSettingsStateLocked(iMakeKey);
            if (settingsStatePeekSettingsStateLocked != null) {
                zInsertSettingLocked = settingsStatePeekSettingsStateLocked.insertSettingLocked(str, str2, str3, z, str4);
            } else {
                zInsertSettingLocked = false;
            }
            if (zInsertSettingLocked && set != null && set.contains(str)) {
                settingsStatePeekSettingsStateLocked.persistSyncLocked();
            }
            if (z2 || zInsertSettingLocked) {
                notifyForSettingsChange(iMakeKey, str);
            }
            return zInsertSettingLocked;
        }

        public boolean deleteSettingLocked(int i, int i2, String str, boolean z, Set<String> set) {
            boolean zDeleteSettingLocked;
            int iMakeKey = SettingsProvider.makeKey(i, i2);
            SettingsState settingsStatePeekSettingsStateLocked = peekSettingsStateLocked(iMakeKey);
            if (settingsStatePeekSettingsStateLocked != null) {
                zDeleteSettingLocked = settingsStatePeekSettingsStateLocked.deleteSettingLocked(str);
            } else {
                zDeleteSettingLocked = false;
            }
            if (zDeleteSettingLocked && set != null && set.contains(str)) {
                settingsStatePeekSettingsStateLocked.persistSyncLocked();
            }
            if (z || zDeleteSettingLocked) {
                notifyForSettingsChange(iMakeKey, str);
            }
            return zDeleteSettingLocked;
        }

        public boolean updateSettingLocked(int i, int i2, String str, String str2, String str3, boolean z, String str4, boolean z2, Set<String> set) {
            boolean zUpdateSettingLocked;
            int iMakeKey = SettingsProvider.makeKey(i, i2);
            SettingsState settingsStatePeekSettingsStateLocked = peekSettingsStateLocked(iMakeKey);
            if (settingsStatePeekSettingsStateLocked != null) {
                zUpdateSettingLocked = settingsStatePeekSettingsStateLocked.updateSettingLocked(str, str2, str3, z, str4);
            } else {
                zUpdateSettingLocked = false;
            }
            if (zUpdateSettingLocked && set != null && set.contains(str)) {
                settingsStatePeekSettingsStateLocked.persistSyncLocked();
            }
            if (z2 || zUpdateSettingLocked) {
                notifyForSettingsChange(iMakeKey, str);
            }
            return zUpdateSettingLocked;
        }

        public SettingsState.Setting getSettingLocked(int i, int i2, String str) {
            SettingsState settingsStatePeekSettingsStateLocked = peekSettingsStateLocked(SettingsProvider.makeKey(i, i2));
            if (settingsStatePeekSettingsStateLocked == null) {
                return null;
            }
            return settingsStatePeekSettingsStateLocked.getSettingLocked(str);
        }

        public void resetSettingsLocked(int i, int i2, String str, int i3, String str2) {
            boolean z;
            boolean z2;
            boolean z3;
            boolean z4;
            int iMakeKey = SettingsProvider.makeKey(i, i2);
            SettingsState settingsStatePeekSettingsStateLocked = peekSettingsStateLocked(iMakeKey);
            if (settingsStatePeekSettingsStateLocked == null) {
            }
            switch (i3) {
                case 1:
                    for (String str3 : settingsStatePeekSettingsStateLocked.getSettingNamesLocked()) {
                        SettingsState.Setting settingLocked = settingsStatePeekSettingsStateLocked.getSettingLocked(str3);
                        if (str.equals(settingLocked.getPackageName())) {
                            if (str2 == null || str2.equals(settingLocked.getTag())) {
                                if (settingsStatePeekSettingsStateLocked.resetSettingLocked(str3)) {
                                    notifyForSettingsChange(iMakeKey, str3);
                                    z = true;
                                }
                                if (!z) {
                                    settingsStatePeekSettingsStateLocked.persistSyncLocked();
                                }
                            }
                        }
                        z = false;
                        if (!z) {
                        }
                    }
                    break;
                case 2:
                    for (String str4 : settingsStatePeekSettingsStateLocked.getSettingNamesLocked()) {
                        if (SettingsState.isSystemPackage(SettingsProvider.this.getContext(), settingsStatePeekSettingsStateLocked.getSettingLocked(str4).getPackageName()) || !settingsStatePeekSettingsStateLocked.resetSettingLocked(str4)) {
                            z2 = false;
                        } else {
                            notifyForSettingsChange(iMakeKey, str4);
                            z2 = true;
                        }
                        if (z2) {
                            settingsStatePeekSettingsStateLocked.persistSyncLocked();
                        }
                    }
                    break;
                case 3:
                    for (String str5 : settingsStatePeekSettingsStateLocked.getSettingNamesLocked()) {
                        SettingsState.Setting settingLocked2 = settingsStatePeekSettingsStateLocked.getSettingLocked(str5);
                        if (!SettingsState.isSystemPackage(SettingsProvider.this.getContext(), settingLocked2.getPackageName())) {
                            if (settingLocked2.isDefaultFromSystem()) {
                                if (settingsStatePeekSettingsStateLocked.resetSettingLocked(str5)) {
                                    notifyForSettingsChange(iMakeKey, str5);
                                    z3 = true;
                                }
                                z3 = false;
                            } else {
                                if (settingsStatePeekSettingsStateLocked.deleteSettingLocked(str5)) {
                                    notifyForSettingsChange(iMakeKey, str5);
                                    z3 = true;
                                }
                                z3 = false;
                            }
                        } else {
                            z3 = false;
                        }
                        if (z3) {
                            settingsStatePeekSettingsStateLocked.persistSyncLocked();
                        }
                    }
                    break;
                case 4:
                    for (String str6 : settingsStatePeekSettingsStateLocked.getSettingNamesLocked()) {
                        if (settingsStatePeekSettingsStateLocked.getSettingLocked(str6).isDefaultFromSystem()) {
                            if (settingsStatePeekSettingsStateLocked.resetSettingLocked(str6)) {
                                notifyForSettingsChange(iMakeKey, str6);
                                z4 = true;
                            }
                            z4 = false;
                        } else {
                            if (settingsStatePeekSettingsStateLocked.deleteSettingLocked(str6)) {
                                notifyForSettingsChange(iMakeKey, str6);
                                z4 = true;
                            }
                            z4 = false;
                        }
                        if (z4) {
                            settingsStatePeekSettingsStateLocked.persistSyncLocked();
                        }
                    }
                    break;
            }
        }

        public void onPackageRemovedLocked(String str, int i) {
            SettingsState settingsState = this.mSettingsStates.get(SettingsProvider.makeKey(1, i));
            if (settingsState != null) {
                settingsState.onPackageRemovedLocked(str);
            }
        }

        public void onUidRemovedLocked(int i) {
            SettingsState settingsLocked = getSettingsLocked(3, UserHandle.getUserId(i));
            if (settingsLocked != null) {
                settingsLocked.deleteSettingLocked(Integer.toString(i));
            }
        }

        private SettingsState peekSettingsStateLocked(int i) {
            SettingsState settingsState = this.mSettingsStates.get(i);
            if (settingsState != null) {
                return settingsState;
            }
            if (!ensureSettingsForUserLocked(SettingsProvider.getUserIdFromKey(i))) {
                return null;
            }
            return this.mSettingsStates.get(i);
        }

        private void migrateAllLegacySettingsIfNeeded() {
            synchronized (SettingsProvider.this.mLock) {
                if (SettingsState.stateFileExists(getSettingsFile(SettingsProvider.makeKey(0, 0)))) {
                    return;
                }
                this.mSettingsCreationBuildId = Build.ID;
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    List users = SettingsProvider.this.mUserManager.getUsers(true);
                    int size = users.size();
                    for (int i = 0; i < size; i++) {
                        int i2 = ((UserInfo) users.get(i)).id;
                        DatabaseHelper databaseHelper = new DatabaseHelper(SettingsProvider.this.getContext(), i2);
                        migrateLegacySettingsForUserLocked(databaseHelper, databaseHelper.getWritableDatabase(), i2);
                        new UpgradeController(i2).upgradeIfNeededLocked();
                        if (!SettingsProvider.this.mUserManager.isUserRunning(new UserHandle(i2))) {
                            removeUserStateLocked(i2, false);
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        private void migrateLegacySettingsForUserIfNeededLocked(int i) {
            if (SettingsState.stateFileExists(getSettingsFile(SettingsProvider.makeKey(2, i)))) {
                return;
            }
            DatabaseHelper databaseHelper = new DatabaseHelper(SettingsProvider.this.getContext(), i);
            migrateLegacySettingsForUserLocked(databaseHelper, databaseHelper.getWritableDatabase(), i);
        }

        private void migrateLegacySettingsForUserLocked(DatabaseHelper databaseHelper, SQLiteDatabase sQLiteDatabase, int i) {
            int iMakeKey = SettingsProvider.makeKey(1, i);
            ensureSettingsStateLocked(iMakeKey);
            SettingsState settingsState = this.mSettingsStates.get(iMakeKey);
            migrateLegacySettingsLocked(settingsState, sQLiteDatabase, "system");
            settingsState.persistSyncLocked();
            int iMakeKey2 = SettingsProvider.makeKey(2, i);
            ensureSettingsStateLocked(iMakeKey2);
            SettingsState settingsState2 = this.mSettingsStates.get(iMakeKey2);
            migrateLegacySettingsLocked(settingsState2, sQLiteDatabase, "secure");
            ensureSecureSettingAndroidIdSetLocked(settingsState2);
            settingsState2.persistSyncLocked();
            if (i == 0) {
                int iMakeKey3 = SettingsProvider.makeKey(0, i);
                ensureSettingsStateLocked(iMakeKey3);
                SettingsState settingsState3 = this.mSettingsStates.get(iMakeKey3);
                migrateLegacySettingsLocked(settingsState3, sQLiteDatabase, "global");
                if (this.mSettingsCreationBuildId != null) {
                    settingsState3.insertSettingLocked("database_creation_buildid", this.mSettingsCreationBuildId, null, true, "android");
                }
                settingsState3.persistSyncLocked();
            }
            databaseHelper.dropDatabase();
        }

        private void migrateLegacySettingsLocked(SettingsState settingsState, SQLiteDatabase sQLiteDatabase, String str) {
            SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
            sQLiteQueryBuilder.setTables(str);
            Cursor cursorQuery = sQLiteQueryBuilder.query(sQLiteDatabase, SettingsProvider.ALL_COLUMNS, null, null, null, null, null);
            if (cursorQuery == null) {
                return;
            }
            try {
                if (!cursorQuery.moveToFirst()) {
                    return;
                }
                int columnIndex = cursorQuery.getColumnIndex("name");
                int columnIndex2 = cursorQuery.getColumnIndex("value");
                settingsState.setVersionLocked(sQLiteDatabase.getVersion());
                while (!cursorQuery.isAfterLast()) {
                    settingsState.insertSettingLocked(cursorQuery.getString(columnIndex), cursorQuery.getString(columnIndex2), null, true, "android");
                    cursorQuery.moveToNext();
                }
            } finally {
                cursorQuery.close();
            }
        }

        private void ensureSecureSettingAndroidIdSetLocked(SettingsState settingsState) {
            DropBoxManager dropBoxManager;
            if (!settingsState.getSettingLocked("android_id").isNull()) {
                return;
            }
            int userIdFromKey = SettingsProvider.getUserIdFromKey(settingsState.mKey);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                UserInfo userInfo = SettingsProvider.this.mUserManager.getUserInfo(userIdFromKey);
                if (userInfo == null) {
                    return;
                }
                String hexString = Long.toHexString(new SecureRandom().nextLong());
                settingsState.insertSettingLocked("android_id", hexString, null, true, "android");
                Slog.d("SettingsProvider", "Generated and saved new ANDROID_ID [" + hexString + "] for user " + userIdFromKey);
                if (userInfo.isRestricted() && (dropBoxManager = (DropBoxManager) SettingsProvider.this.getContext().getSystemService("dropbox")) != null && dropBoxManager.isTagEnabled("restricted_profile_ssaid")) {
                    dropBoxManager.addText("restricted_profile_ssaid", System.currentTimeMillis() + ",restricted_profile_ssaid," + hexString + "\n");
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        private void notifyForSettingsChange(int i, String str) {
            this.mGenerationRegistry.incrementGeneration(i);
            if (isGlobalSettingsKey(i)) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    if ("location_global_kill_switch".equals(str)) {
                        notifyLocationChangeForRunningUsers();
                    }
                    notifyGlobalSettingChangeForRunningUsers(i, str);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            } else {
                int userIdFromKey = SettingsProvider.getUserIdFromKey(i);
                Uri notificationUriFor = getNotificationUriFor(i, str);
                this.mHandler.obtainMessage(1, userIdFromKey, 0, notificationUriFor).sendToTarget();
                if (isSecureSettingsKey(i)) {
                    maybeNotifyProfiles(SettingsProvider.getTypeFromKey(i), userIdFromKey, notificationUriFor, str, SettingsProvider.sSecureCloneToManagedSettings);
                    maybeNotifyProfiles(1, userIdFromKey, notificationUriFor, str, SettingsProvider.sSystemCloneFromParentOnDependency.values());
                } else if (isSystemSettingsKey(i)) {
                    maybeNotifyProfiles(SettingsProvider.getTypeFromKey(i), userIdFromKey, notificationUriFor, str, SettingsProvider.sSystemCloneToManagedSettings);
                }
            }
            this.mHandler.obtainMessage(2).sendToTarget();
        }

        private void maybeNotifyProfiles(int i, int i2, Uri uri, String str, Collection<String> collection) {
            if (collection.contains(str)) {
                for (int i3 : SettingsProvider.this.mUserManager.getProfileIdsWithDisabled(i2)) {
                    if (i3 != i2) {
                        this.mGenerationRegistry.incrementGeneration(SettingsProvider.makeKey(i, i3));
                        this.mHandler.obtainMessage(1, i3, 0, uri).sendToTarget();
                    }
                }
            }
        }

        private void notifyGlobalSettingChangeForRunningUsers(int i, String str) {
            Uri notificationUriFor = getNotificationUriFor(i, str);
            List users = SettingsProvider.this.mUserManager.getUsers(true);
            for (int i2 = 0; i2 < users.size(); i2++) {
                int i3 = ((UserInfo) users.get(i2)).id;
                if (SettingsProvider.this.mUserManager.isUserRunning(UserHandle.of(i3))) {
                    this.mHandler.obtainMessage(1, i3, 0, notificationUriFor).sendToTarget();
                }
            }
        }

        private void notifyLocationChangeForRunningUsers() {
            List users = SettingsProvider.this.mUserManager.getUsers(true);
            for (int i = 0; i < users.size(); i++) {
                int i2 = ((UserInfo) users.get(i)).id;
                if (SettingsProvider.this.mUserManager.isUserRunning(UserHandle.of(i2))) {
                    int iMakeKey = SettingsProvider.makeKey(2, i2);
                    this.mGenerationRegistry.incrementGeneration(iMakeKey);
                    this.mHandler.obtainMessage(1, i2, 0, getNotificationUriFor(iMakeKey, "location_providers_allowed")).sendToTarget();
                }
            }
        }

        private boolean isGlobalSettingsKey(int i) {
            return SettingsProvider.getTypeFromKey(i) == 0;
        }

        private boolean isSystemSettingsKey(int i) {
            return SettingsProvider.getTypeFromKey(i) == 1;
        }

        private boolean isSecureSettingsKey(int i) {
            return SettingsProvider.getTypeFromKey(i) == 2;
        }

        private boolean isSsaidSettingsKey(int i) {
            return SettingsProvider.getTypeFromKey(i) == 3;
        }

        private File getSettingsFile(int i) {
            if (isGlobalSettingsKey(i)) {
                return new File(Environment.getUserSystemDirectory(SettingsProvider.getUserIdFromKey(i)), "settings_global.xml");
            }
            if (isSystemSettingsKey(i)) {
                return new File(Environment.getUserSystemDirectory(SettingsProvider.getUserIdFromKey(i)), "settings_system.xml");
            }
            if (isSecureSettingsKey(i)) {
                return new File(Environment.getUserSystemDirectory(SettingsProvider.getUserIdFromKey(i)), "settings_secure.xml");
            }
            if (isSsaidSettingsKey(i)) {
                return new File(Environment.getUserSystemDirectory(SettingsProvider.getUserIdFromKey(i)), "settings_ssaid.xml");
            }
            throw new IllegalArgumentException("Invalid settings key:" + i);
        }

        private Uri getNotificationUriFor(int i, String str) {
            if (isGlobalSettingsKey(i)) {
                return str != null ? Uri.withAppendedPath(Settings.Global.CONTENT_URI, str) : Settings.Global.CONTENT_URI;
            }
            if (isSecureSettingsKey(i)) {
                return str != null ? Uri.withAppendedPath(Settings.Secure.CONTENT_URI, str) : Settings.Secure.CONTENT_URI;
            }
            if (isSystemSettingsKey(i)) {
                return str != null ? Uri.withAppendedPath(Settings.System.CONTENT_URI, str) : Settings.System.CONTENT_URI;
            }
            throw new IllegalArgumentException("Invalid settings key:" + i);
        }

        private int getMaxBytesPerPackageForType(int i) {
            if (i != 0) {
                switch (i) {
                    case 2:
                    case 3:
                        return -1;
                    default:
                        return 20000;
                }
            }
            return -1;
        }

        private final class MyHandler extends Handler {
            public MyHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        int i = message.arg1;
                        Uri uri = (Uri) message.obj;
                        try {
                            SettingsProvider.this.getContext().getContentResolver().notifyChange(uri, null, true, i);
                        } catch (SecurityException e) {
                            Slog.w("SettingsProvider", "Failed to notify for " + i + ": " + uri, e);
                        }
                        Slog.v("SettingsProvider", "Notifying for " + i + ": " + uri);
                        break;
                    case 2:
                        SettingsRegistry.this.mBackupManager.dataChanged();
                        break;
                }
            }
        }

        private final class UpgradeController {
            private final int mUserId;

            public UpgradeController(int i) {
                this.mUserId = i;
            }

            public void upgradeIfNeededLocked() throws Throwable {
                SettingsState settingsLocked = SettingsRegistry.this.getSettingsLocked(2, this.mUserId);
                int versionLocked = settingsLocked.getVersionLocked();
                if (versionLocked != 169) {
                    int iOnUpgradeLocked = onUpgradeLocked(this.mUserId, versionLocked, 169);
                    if (iOnUpgradeLocked != 169) {
                        SettingsRegistry.this.removeUserStateLocked(this.mUserId, true);
                        DatabaseHelper databaseHelper = new DatabaseHelper(SettingsProvider.this.getContext(), this.mUserId);
                        SQLiteDatabase writableDatabase = databaseHelper.getWritableDatabase();
                        databaseHelper.recreateDatabase(writableDatabase, 169, iOnUpgradeLocked, versionLocked);
                        SettingsRegistry.this.migrateLegacySettingsForUserLocked(databaseHelper, writableDatabase, this.mUserId);
                        onUpgradeLocked(this.mUserId, versionLocked, 169);
                        getGlobalSettingsLocked().insertSettingLocked("database_downgrade_reason", "Settings rebuilt! Current version: " + iOnUpgradeLocked + " while expected: 169", null, true, "android");
                    }
                    if (this.mUserId == 0) {
                        SettingsRegistry.this.getSettingsLocked(0, this.mUserId).setVersionLocked(169);
                    }
                    settingsLocked.setVersionLocked(169);
                    SettingsRegistry.this.getSettingsLocked(1, this.mUserId).setVersionLocked(169);
                }
            }

            private SettingsState getGlobalSettingsLocked() {
                return SettingsRegistry.this.getSettingsLocked(0, 0);
            }

            private SettingsState getSecureSettingsLocked(int i) {
                return SettingsRegistry.this.getSettingsLocked(2, i);
            }

            private SettingsState getSsaidSettingsLocked(int i) {
                return SettingsRegistry.this.getSettingsLocked(3, i);
            }

            private SettingsState getSystemSettingsLocked(int i) {
                return SettingsRegistry.this.getSettingsLocked(1, i);
            }

            private int onUpgradeLocked(int i, int i2, int i3) {
                String string;
                ArraySet<ComponentName> defaultVrComponents;
                Slog.w("SettingsProvider", "Upgrading settings for user: " + i + " from version: " + i2 + " to version: " + i3);
                if (i2 == 118) {
                    if (i == 0) {
                        SettingsState globalSettingsLocked = getGlobalSettingsLocked();
                        globalSettingsLocked.updateSettingLocked("zen_mode", Integer.toString(0), null, true, "android");
                        globalSettingsLocked.updateSettingLocked("mode_ringer", Integer.toString(2), null, true, "android");
                    }
                    i2 = 119;
                }
                if (i2 == 119) {
                    getSecureSettingsLocked(i).insertSettingLocked("double_tap_to_wake", SettingsProvider.this.getContext().getResources().getBoolean(R.bool.def_double_tap_to_wake) ? "1" : "0", null, true, "android");
                    i2 = 120;
                }
                if (i2 == 120) {
                    i2 = 121;
                }
                if (i2 == 121) {
                    SettingsState secureSettingsLocked = getSecureSettingsLocked(i);
                    String string2 = SettingsProvider.this.getContext().getResources().getString(R.string.def_nfc_payment_component);
                    SettingsState.Setting settingLocked = secureSettingsLocked.getSettingLocked("nfc_payment_default_component");
                    if (string2 != null && !string2.isEmpty() && settingLocked.isNull()) {
                        secureSettingsLocked.insertSettingLocked("nfc_payment_default_component", string2, null, true, "android");
                    }
                    i2 = 122;
                }
                if (i2 == 122) {
                    if (i == 0) {
                        SettingsState globalSettingsLocked2 = getGlobalSettingsLocked();
                        if (globalSettingsLocked2.getSettingLocked("add_users_when_locked").isNull()) {
                            globalSettingsLocked2.insertSettingLocked("add_users_when_locked", SettingsProvider.this.getContext().getResources().getBoolean(R.bool.def_add_users_from_lockscreen) ? "1" : "0", null, true, "android");
                        }
                    }
                    i2 = 123;
                }
                if (i2 == 123) {
                    getGlobalSettingsLocked().insertSettingLocked("bluetooth_disabled_profiles", SettingsProvider.this.getContext().getResources().getString(R.string.def_bluetooth_disabled_profiles), null, true, "android");
                    i2 = 124;
                }
                if (i2 == 124) {
                    SettingsState secureSettingsLocked2 = getSecureSettingsLocked(i);
                    if (secureSettingsLocked2.getSettingLocked("show_ime_with_hard_keyboard").isNull()) {
                        secureSettingsLocked2.insertSettingLocked("show_ime_with_hard_keyboard", SettingsProvider.this.getContext().getResources().getBoolean(R.bool.def_show_ime_with_hard_keyboard) ? "1" : "0", null, true, "android");
                    }
                    i2 = 125;
                }
                if (i2 == 125) {
                    SettingsState secureSettingsLocked3 = getSecureSettingsLocked(i);
                    if (secureSettingsLocked3.getSettingLocked("enabled_vr_listeners").isNull() && (defaultVrComponents = SystemConfig.getInstance().getDefaultVrComponents()) != null && !defaultVrComponents.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        boolean z = true;
                        for (ComponentName componentName : defaultVrComponents) {
                            if (!z) {
                                sb.append(':');
                            }
                            sb.append(componentName.flattenToString());
                            z = false;
                        }
                        secureSettingsLocked3.insertSettingLocked("enabled_vr_listeners", sb.toString(), null, true, "android");
                    }
                    i2 = 126;
                }
                if (i2 == 126) {
                    if (SettingsProvider.this.mUserManager.isManagedProfile(i)) {
                        SettingsState secureSettingsLocked4 = getSecureSettingsLocked(0);
                        SettingsState.Setting settingLocked2 = secureSettingsLocked4.getSettingLocked("lock_screen_show_notifications");
                        if (!settingLocked2.isNull()) {
                            getSecureSettingsLocked(i).insertSettingLocked("lock_screen_show_notifications", settingLocked2.getValue(), null, true, "android");
                        }
                        SettingsState.Setting settingLocked3 = secureSettingsLocked4.getSettingLocked("lock_screen_allow_private_notifications");
                        if (!settingLocked3.isNull()) {
                            getSecureSettingsLocked(i).insertSettingLocked("lock_screen_allow_private_notifications", settingLocked3.getValue(), null, true, "android");
                        }
                    }
                    i2 = 127;
                }
                if (i2 == 127) {
                    i2 = 128;
                }
                if (i2 == 128) {
                    i2 = 129;
                }
                if (i2 == 129) {
                    SettingsState secureSettingsLocked5 = getSecureSettingsLocked(i);
                    if (TextUtils.equals("500", secureSettingsLocked5.getSettingLocked("long_press_timeout").getValue())) {
                        secureSettingsLocked5.insertSettingLocked("long_press_timeout", String.valueOf(SettingsProvider.this.getContext().getResources().getInteger(R.integer.def_long_press_timeout_millis)), null, true, "android");
                    }
                    i2 = 130;
                }
                if (i2 == 130) {
                    SettingsState secureSettingsLocked6 = getSecureSettingsLocked(i);
                    if ("0".equals(secureSettingsLocked6.getSettingLocked("doze_enabled").getValue())) {
                        secureSettingsLocked6.insertSettingLocked("doze_pulse_on_pick_up", "0", null, true, "android");
                        secureSettingsLocked6.insertSettingLocked("doze_pulse_on_double_tap", "0", null, true, "android");
                    }
                    i2 = 131;
                }
                if (i2 == 131) {
                    SettingsState secureSettingsLocked7 = getSecureSettingsLocked(i);
                    if (TextUtils.equals(null, secureSettingsLocked7.getSettingLocked("multi_press_timeout").getValue())) {
                        secureSettingsLocked7.insertSettingLocked("multi_press_timeout", String.valueOf(SettingsProvider.this.getContext().getResources().getInteger(R.integer.def_multi_press_timeout_millis)), null, true, "android");
                    }
                    i2 = 132;
                }
                if (i2 == 132) {
                    getSecureSettingsLocked(i).insertSettingLocked("sync_parent_sounds", SettingsProvider.this.getContext().getResources().getBoolean(R.bool.def_sync_parent_sounds) ? "1" : "0", null, true, "android");
                    i2 = 133;
                }
                if (i2 == 133) {
                    SettingsState systemSettingsLocked = getSystemSettingsLocked(i);
                    if (systemSettingsLocked.getSettingLocked("end_button_behavior") == null) {
                        systemSettingsLocked.insertSettingLocked("end_button_behavior", Integer.toString(SettingsProvider.this.getContext().getResources().getInteger(R.integer.def_end_button_behavior)), null, true, "android");
                    }
                    i2 = 134;
                }
                if (i2 == 134) {
                    getSecureSettingsLocked(i).deleteSettingLocked("accessibility_display_magnification_auto_update");
                    i2 = 135;
                }
                if (i2 == 135) {
                    i2 = 136;
                }
                if (i2 == 136) {
                    try {
                        if (SettingsProvider.this.mPackageManager.isUpgrade()) {
                            SettingsState.Setting settingLocked4 = SettingsRegistry.this.getSettingLocked(2, i, "android_id");
                            if (settingLocked4 == null || settingLocked4.isNull() || settingLocked4.getValue() == null) {
                                throw new IllegalStateException("Legacy ssaid not accessible");
                            }
                            String value = settingLocked4.getValue();
                            try {
                                List<PackageInfo> list = SettingsProvider.this.mPackageManager.getInstalledPackages(8192, i).getList();
                                SettingsState ssaidSettingsLocked = getSsaidSettingsLocked(i);
                                for (PackageInfo packageInfo : list) {
                                    String string3 = Integer.toString(packageInfo.applicationInfo.uid);
                                    SettingsState.Setting settingLocked5 = ssaidSettingsLocked.getSettingLocked(string3);
                                    if (settingLocked5.isNull() || settingLocked5.getValue() == null) {
                                        ssaidSettingsLocked.insertSettingLocked(string3, value, null, true, packageInfo.packageName);
                                    }
                                }
                            } catch (RemoteException e) {
                                throw new IllegalStateException("Package manager not available");
                            }
                        }
                        i2 = 137;
                    } catch (RemoteException e2) {
                        throw new IllegalStateException("Package manager not available");
                    }
                }
                if (i2 == 137) {
                    SettingsState secureSettingsLocked8 = getSecureSettingsLocked(i);
                    if (!SettingsProvider.this.mUserManager.hasUserRestriction("no_install_unknown_sources", UserHandle.of(i)) && secureSettingsLocked8.getSettingLocked("install_non_market_apps").getValue().equals("0")) {
                        secureSettingsLocked8.insertSettingLocked("install_non_market_apps", "1", null, true, "android");
                        secureSettingsLocked8.insertSettingLocked("unknown_sources_default_reversed", "1", null, true, "android");
                    }
                    i2 = 138;
                }
                if (i2 == 138) {
                    i2 = 139;
                }
                if (i2 == 139) {
                    getSecureSettingsLocked(i).updateSettingLocked("speak_password", "1", null, true, "android");
                    i2 = 140;
                }
                if (i2 == 140) {
                    i2 = 141;
                }
                if (i2 == 141) {
                    i2 = 142;
                }
                if (i2 == 142) {
                    if (i == 0) {
                        SettingsState globalSettingsLocked3 = getGlobalSettingsLocked();
                        if (globalSettingsLocked3.getSettingLocked("wifi_wakeup_enabled").isNull()) {
                            globalSettingsLocked3.insertSettingLocked("wifi_wakeup_enabled", SettingsProvider.this.getContext().getResources().getBoolean(R.bool.def_wifi_wakeup_enabled) ? "1" : "0", null, true, "android");
                        }
                    }
                    i2 = 143;
                }
                if (i2 == 143) {
                    SettingsState secureSettingsLocked9 = getSecureSettingsLocked(i);
                    if (secureSettingsLocked9.getSettingLocked("autofill_service").isNull() && (string = SettingsProvider.this.getContext().getResources().getString(android.R.string.action_bar_home_description)) != null) {
                        Slog.d("SettingsProvider", "Setting [" + string + "] as Autofill Service for user " + i);
                        secureSettingsLocked9.insertSettingLocked("autofill_service", string, null, true, "android");
                    }
                    i2 = 144;
                }
                if (i2 == 144) {
                    i2 = 145;
                }
                if (i2 == 145) {
                    if (i == 0) {
                        SettingsState globalSettingsLocked4 = getGlobalSettingsLocked();
                        SettingsRegistry.this.ensureLegacyDefaultValueAndSystemSetUpdatedLocked(globalSettingsLocked4, i);
                        globalSettingsLocked4.persistSyncLocked();
                    }
                    SettingsState secureSettingsLocked10 = getSecureSettingsLocked(this.mUserId);
                    SettingsRegistry.this.ensureLegacyDefaultValueAndSystemSetUpdatedLocked(secureSettingsLocked10, i);
                    secureSettingsLocked10.persistSyncLocked();
                    SettingsState systemSettingsLocked2 = getSystemSettingsLocked(this.mUserId);
                    SettingsRegistry.this.ensureLegacyDefaultValueAndSystemSetUpdatedLocked(systemSettingsLocked2, i);
                    systemSettingsLocked2.persistSyncLocked();
                    i2 = 146;
                }
                if (i2 == 146) {
                    i2 = 147;
                }
                if (i2 == 147) {
                    if (i == 0) {
                        SettingsState globalSettingsLocked5 = getGlobalSettingsLocked();
                        if (globalSettingsLocked5.getSettingLocked("default_restrict_background_data").isNull()) {
                            globalSettingsLocked5.insertSettingLocked("default_restrict_background_data", SettingsProvider.this.getContext().getResources().getBoolean(R.bool.def_restrict_background_data) ? "1" : "0", null, true, "android");
                        }
                    }
                    i2 = 148;
                }
                if (i2 == 148) {
                    SettingsState secureSettingsLocked11 = getSecureSettingsLocked(i);
                    if (TextUtils.equals(null, secureSettingsLocked11.getSettingLocked("backup_manager_constants").getValue())) {
                        String string4 = SettingsProvider.this.getContext().getResources().getString(R.string.def_backup_manager_constants);
                        if (!TextUtils.isEmpty(string4)) {
                            secureSettingsLocked11.insertSettingLocked("backup_manager_constants", string4, null, true, "android");
                        }
                    }
                    i2 = 149;
                }
                if (i2 == 149) {
                    SettingsState globalSettingsLocked6 = getGlobalSettingsLocked();
                    if (globalSettingsLocked6.getSettingLocked("mobile_data_always_on").isNull()) {
                        globalSettingsLocked6.insertSettingLocked("mobile_data_always_on", SettingsProvider.this.getContext().getResources().getBoolean(R.bool.def_mobile_data_always_on) ? "1" : "0", null, true, "android");
                    }
                    i2 = 150;
                }
                if (i2 == 150) {
                    i2 = 151;
                }
                if (i2 == 151) {
                    i2 = 152;
                }
                if (i2 == 152) {
                    getGlobalSettingsLocked().deleteSettingLocked("wifi_wakeup_available");
                    i2 = 153;
                }
                if (i2 == 153) {
                    SettingsState secureSettingsLocked12 = getSecureSettingsLocked(i);
                    if (secureSettingsLocked12.getSettingLocked("notification_badging").isNull()) {
                        secureSettingsLocked12.insertSettingLocked("notification_badging", SettingsProvider.this.getContext().getResources().getBoolean(android.R.^attr-private.listItemLayout) ? "1" : "0", null, true, "android");
                    }
                    i2 = 154;
                }
                if (i2 == 154) {
                    SettingsState secureSettingsLocked13 = getSecureSettingsLocked(i);
                    if (TextUtils.equals(null, secureSettingsLocked13.getSettingLocked("backup_local_transport_parameters").getValue())) {
                        String string5 = SettingsProvider.this.getContext().getResources().getString(R.string.def_backup_local_transport_parameters);
                        if (!TextUtils.isEmpty(string5)) {
                            secureSettingsLocked13.insertSettingLocked("backup_local_transport_parameters", string5, null, true, "android");
                        }
                    }
                    i2 = 155;
                }
                if (i2 == 155) {
                    SettingsState globalSettingsLocked7 = getGlobalSettingsLocked();
                    String value2 = globalSettingsLocked7.getSettingLocked("wireless_charging_started_sound").getValue();
                    String string6 = SettingsProvider.this.getContext().getResources().getString(R.string.def_wireless_charging_started_sound);
                    if (TextUtils.equals(null, value2) || TextUtils.equals(value2, string6)) {
                        String string7 = SettingsProvider.this.getContext().getResources().getString(R.string.def_charging_started_sound);
                        if (!TextUtils.isEmpty(string7)) {
                            globalSettingsLocked7.insertSettingLocked("wireless_charging_started_sound", string7, null, true, "android");
                        }
                    }
                    i2 = 156;
                }
                if (i2 == 156) {
                    SettingsState globalSettingsLocked8 = getGlobalSettingsLocked();
                    if (globalSettingsLocked8.getSettingLocked("zen_duration").isNull()) {
                        globalSettingsLocked8.insertSettingLocked("zen_duration", Integer.toString(SettingsProvider.this.getContext().getResources().getInteger(R.integer.def_zen_duration)), null, true, "android");
                    }
                    i2 = 157;
                }
                if (i2 == 157) {
                    SettingsState globalSettingsLocked9 = getGlobalSettingsLocked();
                    if (TextUtils.equals(null, globalSettingsLocked9.getSettingLocked("backup_agent_timeout_parameters").getValue())) {
                        String string8 = SettingsProvider.this.getContext().getResources().getString(R.string.def_backup_agent_timeout_parameters);
                        if (!TextUtils.isEmpty(string8)) {
                            globalSettingsLocked9.insertSettingLocked("backup_agent_timeout_parameters", string8, null, true, "android");
                        }
                    }
                    i2 = 158;
                }
                if (i2 == 158) {
                    getGlobalSettingsLocked().deleteSettingLocked("wifi_scan_background_throttle_interval_ms");
                    getGlobalSettingsLocked().deleteSettingLocked("wifi_scan_background_throttle_package_whitelist");
                    i2 = 159;
                }
                if (i2 == 159) {
                    if (SettingsProvider.this.mUserManager.isManagedProfile(i)) {
                        SettingsState secureSettingsLocked14 = getSecureSettingsLocked(i);
                        if ("0".equals(secureSettingsLocked14.getSettingLocked("lock_screen_show_notifications").getValue())) {
                            secureSettingsLocked14.insertSettingLocked("lock_screen_allow_private_notifications", "0", null, false, "android");
                        }
                        secureSettingsLocked14.deleteSettingLocked("lock_screen_show_notifications");
                    }
                    i2 = 160;
                }
                if (i2 == 160) {
                    SettingsState globalSettingsLocked10 = getGlobalSettingsLocked();
                    if (TextUtils.equals(null, globalSettingsLocked10.getSettingLocked("max_sound_trigger_detection_service_ops_per_day").getValue())) {
                        globalSettingsLocked10.insertSettingLocked("max_sound_trigger_detection_service_ops_per_day", Integer.toString(SettingsProvider.this.getContext().getResources().getInteger(R.integer.def_max_sound_trigger_detection_service_ops_per_day)), null, true, "android");
                    }
                    if (TextUtils.equals(null, globalSettingsLocked10.getSettingLocked("sound_trigger_detection_service_op_timeout").getValue())) {
                        globalSettingsLocked10.insertSettingLocked("sound_trigger_detection_service_op_timeout", Integer.toString(SettingsProvider.this.getContext().getResources().getInteger(R.integer.def_sound_trigger_detection_service_op_timeout)), null, true, "android");
                    }
                    i2 = 161;
                }
                if (i2 == 161) {
                    SettingsState secureSettingsLocked15 = getSecureSettingsLocked(i);
                    if (secureSettingsLocked15.getSettingLocked("volume_hush_gesture").isNull()) {
                        secureSettingsLocked15.insertSettingLocked("volume_hush_gesture", Integer.toString(1), null, true, "android");
                    }
                    i2 = 162;
                }
                if (i2 == 162) {
                    i2 = 163;
                }
                if (i2 == 163) {
                    SettingsState globalSettingsLocked11 = getGlobalSettingsLocked();
                    if (globalSettingsLocked11.getSettingLocked("max_sound_trigger_detection_service_ops_per_day").isDefaultFromSystem()) {
                        globalSettingsLocked11.insertSettingLocked("max_sound_trigger_detection_service_ops_per_day", Integer.toString(SettingsProvider.this.getContext().getResources().getInteger(R.integer.def_max_sound_trigger_detection_service_ops_per_day)), null, true, "android");
                    }
                    i2 = 164;
                }
                if (i2 == 164) {
                    SettingsState globalSettingsLocked12 = getGlobalSettingsLocked();
                    SettingsState.Setting settingLocked6 = globalSettingsLocked12.getSettingLocked("show_zen_upgrade_notification");
                    if (!settingLocked6.isNull() && TextUtils.equals("0", settingLocked6.getValue())) {
                        globalSettingsLocked12.insertSettingLocked("show_zen_upgrade_notification", "1", null, true, "android");
                    }
                    i2 = 165;
                }
                if (i2 == 165) {
                    SettingsState globalSettingsLocked13 = getGlobalSettingsLocked();
                    if (globalSettingsLocked13.getSettingLocked("show_zen_settings_suggestion").isNull()) {
                        globalSettingsLocked13.insertSettingLocked("show_zen_settings_suggestion", "1", null, true, "android");
                    }
                    if (globalSettingsLocked13.getSettingLocked("zen_settings_updated").isNull()) {
                        globalSettingsLocked13.insertSettingLocked("zen_settings_updated", "0", null, true, "android");
                    }
                    if (globalSettingsLocked13.getSettingLocked("zen_settings_suggestion_viewed").isNull()) {
                        globalSettingsLocked13.insertSettingLocked("zen_settings_suggestion_viewed", "0", null, true, "android");
                    }
                    i2 = 166;
                }
                if (i2 == 166) {
                    SettingsState secureSettingsLocked16 = getSecureSettingsLocked(i);
                    if (secureSettingsLocked16.getSettingLocked("hush_gesture_used").isNull()) {
                        secureSettingsLocked16.insertSettingLocked("hush_gesture_used", "0", null, true, "android");
                    }
                    if (secureSettingsLocked16.getSettingLocked("manual_ringer_toggle_count").isNull()) {
                        secureSettingsLocked16.insertSettingLocked("manual_ringer_toggle_count", "0", null, true, "android");
                    }
                    i2 = 167;
                }
                if (i2 == 167) {
                    SettingsState globalSettingsLocked14 = getGlobalSettingsLocked();
                    if (globalSettingsLocked14.getSettingLocked("charging_vibration_enabled").isNull()) {
                        globalSettingsLocked14.insertSettingLocked("charging_vibration_enabled", "1", null, true, "android");
                    }
                    i2 = 168;
                }
                if (i2 == 168) {
                    SettingsState systemSettingsLocked3 = getSystemSettingsLocked(i);
                    if (systemSettingsLocked3.getSettingLocked("vibrate_when_ringing").isNull()) {
                        systemSettingsLocked3.insertSettingLocked("vibrate_when_ringing", SettingsProvider.this.getContext().getResources().getBoolean(R.bool.def_vibrate_when_ringing) ? "1" : "0", null, true, "android");
                    }
                    i2 = 169;
                }
                if (i2 != i3) {
                    Slog.wtf("SettingsProvider", "warning: upgrading settings database to version " + i3 + " left it at " + i2 + " instead; this is probably a bug. Did you update SETTINGS_VERSION?", new Throwable());
                }
                return i2;
            }
        }

        private void ensureLegacyDefaultValueAndSystemSetUpdatedLocked(SettingsState settingsState, int i) {
            List<String> settingNamesLocked = settingsState.getSettingNamesLocked();
            int size = settingNamesLocked.size();
            for (int i2 = 0; i2 < size; i2++) {
                String str = settingNamesLocked.get(i2);
                SettingsState.Setting settingLocked = settingsState.getSettingLocked(str);
                int packageUid = -1;
                try {
                    packageUid = SettingsProvider.this.mPackageManager.getPackageUid(settingLocked.getPackageName(), 0, i);
                } catch (RemoteException e) {
                }
                if (packageUid < 0) {
                    Slog.e("SettingsProvider", "Unknown package: " + settingLocked.getPackageName());
                } else {
                    try {
                        if (SettingsState.isSystemPackage(SettingsProvider.this.getContext(), settingLocked.getPackageName(), packageUid)) {
                            settingsState.insertSettingLocked(str, settingLocked.getValue(), settingLocked.getTag(), true, settingLocked.getPackageName());
                        } else if (settingLocked.getDefaultValue() != null && settingLocked.isDefaultFromSystem()) {
                            settingsState.resetSettingDefaultValueLocked(str);
                        }
                    } catch (IllegalStateException e2) {
                        Slog.e("SettingsProvider", "Error upgrading setting: " + settingLocked.getName(), e2);
                    }
                }
            }
        }
    }
}
