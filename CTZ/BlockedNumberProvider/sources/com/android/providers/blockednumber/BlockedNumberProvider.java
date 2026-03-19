package com.android.providers.blockednumber;

import android.app.AppOpsManager;
import android.app.backup.BackupManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.UserManager;
import android.provider.BlockedNumberContract;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.common.content.ProjectionMap;
import com.android.internal.annotations.VisibleForTesting;

public class BlockedNumberProvider extends ContentProvider {
    private static final ProjectionMap sBlockedNumberColumns;

    @VisibleForTesting
    protected BackupManager mBackupManager;

    @VisibleForTesting
    protected BlockedNumberDatabaseHelper mDbHelper;

    @VisibleForTesting
    static boolean ALLOW_SELF_CALL = true;
    private static final UriMatcher sUriMatcher = new UriMatcher(0);

    static {
        sUriMatcher.addURI("com.android.blockednumber", "blocked", 1000);
        sUriMatcher.addURI("com.android.blockednumber", "blocked/#", 1001);
        sBlockedNumberColumns = ProjectionMap.builder().add("_id").add("original_number").add("e164_number").build();
    }

    @Override
    public boolean onCreate() {
        this.mDbHelper = BlockedNumberDatabaseHelper.getInstance(getContext());
        this.mBackupManager = new BackupManager(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case 1000:
                return "vnd.android.cursor.dir/blocked_number";
            case 1001:
                return "vnd.android.cursor.item/blocked_number";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        enforceWritePermissionAndPrimaryUser();
        if (sUriMatcher.match(uri) == 1000) {
            Uri uriInsertBlockedNumber = insertBlockedNumber(contentValues);
            getContext().getContentResolver().notifyChange(uriInsertBlockedNumber, null);
            this.mBackupManager.dataChanged();
            return uriInsertBlockedNumber;
        }
        throw new IllegalArgumentException("Unsupported URI: " + uri);
    }

    private Uri insertBlockedNumber(ContentValues contentValues) {
        throwIfSpecified(contentValues, "_id");
        String asString = contentValues.getAsString("original_number");
        if (TextUtils.isEmpty(asString)) {
            throw new IllegalArgumentException("Missing a required column original_number");
        }
        contentValues.put("e164_number", Utils.getE164Number(getContext(), asString, contentValues.getAsString("e164_number")));
        return ContentUris.withAppendedId(BlockedNumberContract.BlockedNumbers.CONTENT_URI, this.mDbHelper.getWritableDatabase().insertWithOnConflict("blocked", null, contentValues, 5));
    }

    private static void throwIfSpecified(ContentValues contentValues, String str) {
        if (contentValues.containsKey(str)) {
            throw new IllegalArgumentException("Column " + str + " must not be specified");
        }
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        enforceWritePermissionAndPrimaryUser();
        throw new UnsupportedOperationException("Update is not supported.  Use delete + insert instead");
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        int iDeleteBlockedNumber;
        enforceWritePermissionAndPrimaryUser();
        switch (sUriMatcher.match(uri)) {
            case 1000:
                iDeleteBlockedNumber = deleteBlockedNumber(str, strArr);
                break;
            case 1001:
                iDeleteBlockedNumber = deleteBlockedNumberWithId(ContentUris.parseId(uri), str);
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        this.mBackupManager.dataChanged();
        return iDeleteBlockedNumber;
    }

    private int deleteBlockedNumberWithId(long j, String str) {
        throwForNonEmptySelection(str);
        return deleteBlockedNumber("_id=?", new String[]{Long.toString(j)});
    }

    private int deleteBlockedNumber(String str, String[] strArr) {
        SQLiteDatabase writableDatabase = this.mDbHelper.getWritableDatabase();
        if (!TextUtils.isEmpty(str)) {
            writableDatabase.validateSql("select 1 FROM blocked WHERE " + Utils.wrapSelectionWithParens(str), null);
        }
        return writableDatabase.delete("blocked", str, strArr);
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        enforceReadPermissionAndPrimaryUser();
        return query(uri, strArr, str, strArr2, str2, null);
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2, CancellationSignal cancellationSignal) {
        Cursor cursorQueryBlockedList;
        enforceReadPermissionAndPrimaryUser();
        switch (sUriMatcher.match(uri)) {
            case 1000:
                cursorQueryBlockedList = queryBlockedList(strArr, str, strArr2, str2, cancellationSignal);
                break;
            case 1001:
                cursorQueryBlockedList = queryBlockedListWithId(ContentUris.parseId(uri), strArr, str, cancellationSignal);
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        cursorQueryBlockedList.setNotificationUri(getContext().getContentResolver(), uri);
        return cursorQueryBlockedList;
    }

    private Cursor queryBlockedListWithId(long j, String[] strArr, String str, CancellationSignal cancellationSignal) {
        throwForNonEmptySelection(str);
        return queryBlockedList(strArr, "_id=?", new String[]{Long.toString(j)}, null, cancellationSignal);
    }

    private Cursor queryBlockedList(String[] strArr, String str, String[] strArr2, String str2, CancellationSignal cancellationSignal) {
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        sQLiteQueryBuilder.setStrict(true);
        sQLiteQueryBuilder.setTables("blocked");
        sQLiteQueryBuilder.setProjectionMap(sBlockedNumberColumns);
        return sQLiteQueryBuilder.query(this.mDbHelper.getReadableDatabase(), strArr, str, strArr2, null, null, str2, null, cancellationSignal);
    }

    private void throwForNonEmptySelection(String str) {
        if (!TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("When ID is specified in URI, selection must be null");
        }
    }

    @Override
    public Bundle call(String str, String str2, Bundle bundle) {
        Bundle bundle2;
        bundle2 = new Bundle();
        switch (str) {
            case "is_blocked":
                enforceReadPermissionAndPrimaryUser();
                bundle2.putBoolean("blocked", isBlocked(str2));
                return bundle2;
            case "can_current_user_block_numbers":
                bundle2.putBoolean("can_block", canCurrentUserBlockUsers());
                return bundle2;
            case "unblock":
                enforceWritePermissionAndPrimaryUser();
                bundle2.putInt("num_deleted", unblock(str2));
                return bundle2;
            case "notify_emergency_contact":
                enforceSystemWritePermissionAndPrimaryUser();
                notifyEmergencyContact();
                return bundle2;
            case "end_block_suppression":
                enforceSystemWritePermissionAndPrimaryUser();
                endBlockSuppression();
                return bundle2;
            case "get_block_suppression_status":
                enforceSystemReadPermissionAndPrimaryUser();
                BlockedNumberContract.SystemContract.BlockSuppressionStatus blockSuppressionStatus = getBlockSuppressionStatus();
                bundle2.putBoolean("blocking_suppressed", blockSuppressionStatus.isSuppressed);
                bundle2.putLong("blocking_suppressed_until_timestamp", blockSuppressionStatus.untilTimestampMillis);
                return bundle2;
            case "should_system_block_number":
                enforceSystemReadPermissionAndPrimaryUser();
                bundle2.putBoolean("blocked", shouldSystemBlockNumber(str2, bundle));
                return bundle2;
            case "should_show_emergency_call_notification":
                enforceSystemReadPermissionAndPrimaryUser();
                bundle2.putBoolean("show_emergency_call_notification", shouldShowEmergencyCallNotification());
                return bundle2;
            case "get_enhanced_block_setting":
                enforceSystemReadPermissionAndPrimaryUser();
                if (bundle != null) {
                    bundle2.putBoolean("enhanced_setting_enabled", getEnhancedBlockSetting(bundle.getString("extra_enhanced_setting_key")));
                }
                return bundle2;
            case "set_enhanced_block_setting":
                enforceSystemWritePermissionAndPrimaryUser();
                if (bundle != null) {
                    setEnhancedBlockSetting(bundle.getString("extra_enhanced_setting_key"), bundle.getBoolean("extra_enhanced_setting_value", false));
                }
                return bundle2;
            default:
                enforceReadPermissionAndPrimaryUser();
                throw new IllegalArgumentException("Unsupported method " + str);
        }
    }

    private int unblock(String str) {
        if (TextUtils.isEmpty(str)) {
            return 0;
        }
        StringBuilder sb = new StringBuilder("original_number=?");
        String[] strArr = {str};
        String e164Number = Utils.getE164Number(getContext(), str, null);
        if (!TextUtils.isEmpty(e164Number)) {
            sb.append(" or e164_number=?");
            strArr = new String[]{str, e164Number};
        }
        return deleteBlockedNumber(sb.toString(), strArr);
    }

    private boolean isEmergencyNumber(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        return PhoneNumberUtils.isEmergencyNumber(str) || PhoneNumberUtils.isEmergencyNumber(Utils.getE164Number(getContext(), str, null));
    }

    private boolean isBlocked(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        Cursor cursorRawQuery = this.mDbHelper.getReadableDatabase().rawQuery("SELECT original_number,e164_number FROM blocked WHERE original_number=?1 OR (?2 != '' AND e164_number=?2)", new String[]{str, Utils.getE164Number(getContext(), str, null)});
        try {
            return cursorRawQuery.moveToNext();
        } finally {
            cursorRawQuery.close();
        }
    }

    private boolean canCurrentUserBlockUsers() {
        return ((UserManager) getContext().getSystemService(UserManager.class)).isPrimaryUser();
    }

    private void notifyEmergencyContact() {
        long jCurrentTimeMillis;
        long blockSuppressSecondsFromCarrierConfig = getBlockSuppressSecondsFromCarrierConfig();
        if (blockSuppressSecondsFromCarrierConfig < 0) {
            jCurrentTimeMillis = -1;
        } else {
            jCurrentTimeMillis = (blockSuppressSecondsFromCarrierConfig * 1000) + System.currentTimeMillis();
        }
        writeBlockSuppressionExpiryTimePref(jCurrentTimeMillis);
        writeEmergencyCallNotificationPref(true);
        notifyBlockSuppressionStateChange();
    }

    private void endBlockSuppression() {
        if (getBlockSuppressionStatus().isSuppressed) {
            writeBlockSuppressionExpiryTimePref(0L);
            writeEmergencyCallNotificationPref(false);
            notifyBlockSuppressionStateChange();
        }
    }

    private BlockedNumberContract.SystemContract.BlockSuppressionStatus getBlockSuppressionStatus() {
        long j = getContext().getSharedPreferences("block_number_provider_prefs", 0).getLong("block_suppression_expiry_time_pref", 0L);
        return new BlockedNumberContract.SystemContract.BlockSuppressionStatus(j == -1 || System.currentTimeMillis() < j, j);
    }

    private boolean shouldSystemBlockNumber(String str, Bundle bundle) {
        boolean enhancedBlockSetting;
        if (getBlockSuppressionStatus().isSuppressed) {
            return false;
        }
        if (bundle != null && !bundle.isEmpty()) {
            boolean z = bundle.getBoolean("extra_contact_exist");
            switch (bundle.getInt("extra_call_presentation")) {
                case 1:
                    enhancedBlockSetting = getEnhancedBlockSetting("block_numbers_not_in_contacts_setting") && !z;
                    break;
                case 2:
                    enhancedBlockSetting = getEnhancedBlockSetting("block_private_number_calls_setting");
                    break;
                case 3:
                    enhancedBlockSetting = getEnhancedBlockSetting("block_unknown_calls_setting");
                    break;
                case 4:
                    enhancedBlockSetting = getEnhancedBlockSetting("block_payphone_calls_setting");
                    break;
                default:
                    enhancedBlockSetting = false;
                    break;
            }
        } else {
            enhancedBlockSetting = false;
        }
        return (enhancedBlockSetting || isBlocked(str)) && !isEmergencyNumber(str);
    }

    private boolean shouldShowEmergencyCallNotification() {
        return isEnhancedCallBlockingEnabledByPlatform() && isAnyEnhancedBlockingSettingEnabled() && getBlockSuppressionStatus().isSuppressed && getEnhancedBlockSetting("show_emergency_call_notification");
    }

    private boolean isEnhancedCallBlockingEnabledByPlatform() {
        PersistableBundle config = ((CarrierConfigManager) getContext().getSystemService("carrier_config")).getConfig();
        if (config == null) {
            config = CarrierConfigManager.getDefaultConfig();
        }
        return config.getBoolean("support_enhanced_call_blocking_bool");
    }

    private boolean isAnyEnhancedBlockingSettingEnabled() {
        return getEnhancedBlockSetting("block_numbers_not_in_contacts_setting") || getEnhancedBlockSetting("block_private_number_calls_setting") || getEnhancedBlockSetting("block_payphone_calls_setting") || getEnhancedBlockSetting("block_unknown_calls_setting");
    }

    private boolean getEnhancedBlockSetting(String str) {
        return getContext().getSharedPreferences("block_number_provider_prefs", 0).getBoolean(str, false);
    }

    private void setEnhancedBlockSetting(String str, boolean z) {
        SharedPreferences.Editor editorEdit = getContext().getSharedPreferences("block_number_provider_prefs", 0).edit();
        editorEdit.putBoolean(str, z);
        editorEdit.apply();
    }

    private void writeEmergencyCallNotificationPref(boolean z) {
        if (!isEnhancedCallBlockingEnabledByPlatform()) {
            return;
        }
        setEnhancedBlockSetting("show_emergency_call_notification", z);
    }

    private void writeBlockSuppressionExpiryTimePref(long j) {
        SharedPreferences.Editor editorEdit = getContext().getSharedPreferences("block_number_provider_prefs", 0).edit();
        editorEdit.putLong("block_suppression_expiry_time_pref", j);
        editorEdit.apply();
    }

    private long getBlockSuppressSecondsFromCarrierConfig() {
        int i = ((CarrierConfigManager) getContext().getSystemService(CarrierConfigManager.class)).getConfig().getInt("duration_blocking_disabled_after_emergency_int");
        if (!(i <= 604800)) {
            i = CarrierConfigManager.getDefaultConfig().getInt("duration_blocking_disabled_after_emergency_int");
        }
        return i;
    }

    private boolean checkForPrivilegedApplications() {
        if (Binder.getCallingUid() == 0) {
            return true;
        }
        String callingPackage = getCallingPackage();
        if (TextUtils.isEmpty(callingPackage)) {
            Log.w("BlockedNumbers", "callingPackage not accessible");
            return false;
        }
        TelecomManager telecomManager = (TelecomManager) getContext().getSystemService(TelecomManager.class);
        return callingPackage.equals(telecomManager.getDefaultDialerPackage()) || callingPackage.equals(telecomManager.getSystemDialerPackage()) || ((AppOpsManager) getContext().getSystemService(AppOpsManager.class)).noteOp(15, Binder.getCallingUid(), callingPackage) == 0 || ((TelephonyManager) getContext().getSystemService(TelephonyManager.class)).checkCarrierPrivilegesForPackage(callingPackage) == 1;
    }

    private void notifyBlockSuppressionStateChange() {
        getContext().sendBroadcast(new Intent("android.provider.action.BLOCK_SUPPRESSION_STATE_CHANGED"), "android.permission.READ_BLOCKED_NUMBERS");
    }

    private void enforceReadPermissionAndPrimaryUser() {
        checkForPermissionAndPrimaryUser("android.permission.READ_BLOCKED_NUMBERS");
    }

    private void enforceWritePermissionAndPrimaryUser() {
        checkForPermissionAndPrimaryUser("android.permission.WRITE_BLOCKED_NUMBERS");
    }

    private void checkForPermissionAndPrimaryUser(String str) {
        checkForPermission(str);
        if (!canCurrentUserBlockUsers()) {
            throwCurrentUserNotPermittedSecurityException();
        }
    }

    private void checkForPermission(String str) {
        if (!(passesSystemPermissionCheck(str) || checkForPrivilegedApplications() || isSelf())) {
            throwSecurityException();
        }
    }

    private void enforceSystemReadPermissionAndPrimaryUser() {
        enforceSystemPermissionAndUser("android.permission.READ_BLOCKED_NUMBERS");
    }

    private void enforceSystemWritePermissionAndPrimaryUser() {
        enforceSystemPermissionAndUser("android.permission.WRITE_BLOCKED_NUMBERS");
    }

    private void enforceSystemPermissionAndUser(String str) {
        if (!canCurrentUserBlockUsers()) {
            throwCurrentUserNotPermittedSecurityException();
        }
        if (!passesSystemPermissionCheck(str)) {
            throwSecurityException();
        }
    }

    private boolean passesSystemPermissionCheck(String str) {
        return getContext().checkCallingPermission(str) == 0;
    }

    private boolean isSelf() {
        return ALLOW_SELF_CALL && Binder.getCallingPid() == Process.myPid();
    }

    private void throwSecurityException() {
        throw new SecurityException("Caller must be system, default dialer or default SMS app");
    }

    private void throwCurrentUserNotPermittedSecurityException() {
        throw new SecurityException("The current user cannot perform this operation");
    }
}
