package com.android.providers.settings;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FullBackupDataOutput;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.SettingsValidators;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.BackupUtils;
import android.util.Log;
import com.android.internal.widget.LockPatternUtils;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.DateTimeException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.zip.CRC32;

public class SettingsBackupAgent extends BackupAgentHelper {
    private int mRestoredFromSdkInt;
    private SettingsHelper mSettingsHelper;
    private WifiManager mWifiManager;
    private static final byte[] NULL_VALUE = new byte[0];
    private static final int[] STATE_SIZES = {0, 4, 5, 6, 7, 8, 9, 10};
    private static final byte[] EMPTY_DATA = new byte[0];
    private static final String[] PROJECTION = {"name", "value"};
    private static final ArraySet<String> RESTORE_FROM_HIGHER_SDK_INT_SUPPORTED_KEYS = new ArraySet<>(Arrays.asList("network_policies", "wifi_new_config", "system", "secure", "global"));

    @Override
    public void onCreate() {
        this.mSettingsHelper = new SettingsHelper(this);
        this.mWifiManager = (WifiManager) getSystemService("wifi");
        super.onCreate();
    }

    @Override
    public void onBackup(ParcelFileDescriptor parcelFileDescriptor, BackupDataOutput backupDataOutput, ParcelFileDescriptor parcelFileDescriptor2) throws IOException {
        byte[] systemSettings = getSystemSettings();
        byte[] secureSettings = getSecureSettings();
        byte[] globalSettings = getGlobalSettings();
        byte[] lockSettings = getLockSettings(UserHandle.myUserId());
        byte[] localeData = this.mSettingsHelper.getLocaleData();
        byte[] softAPConfiguration = getSoftAPConfiguration();
        byte[] networkPolicies = getNetworkPolicies();
        byte[] newWifiConfigData = getNewWifiConfigData();
        long[] oldChecksums = readOldChecksums(parcelFileDescriptor);
        oldChecksums[0] = writeIfChanged(oldChecksums[0], "system", systemSettings, backupDataOutput);
        oldChecksums[1] = writeIfChanged(oldChecksums[1], "secure", secureSettings, backupDataOutput);
        oldChecksums[5] = writeIfChanged(oldChecksums[5], "global", globalSettings, backupDataOutput);
        oldChecksums[2] = writeIfChanged(oldChecksums[2], "locale", localeData, backupDataOutput);
        oldChecksums[3] = 0;
        oldChecksums[4] = 0;
        oldChecksums[6] = writeIfChanged(oldChecksums[6], "lock_settings", lockSettings, backupDataOutput);
        oldChecksums[7] = writeIfChanged(oldChecksums[7], "softap_config", softAPConfiguration, backupDataOutput);
        oldChecksums[8] = writeIfChanged(oldChecksums[8], "network_policies", networkPolicies, backupDataOutput);
        oldChecksums[9] = writeIfChanged(oldChecksums[9], "wifi_new_config", newWifiConfigData, backupDataOutput);
        writeNewChecksums(oldChecksums, parcelFileDescriptor2);
    }

    @Override
    public void onRestore(BackupDataInput backupDataInput, int i, ParcelFileDescriptor parcelFileDescriptor) throws BackupUtils.BadVersionException, IOException {
        boolean z = Settings.Global.getInt(getContentResolver(), "override_settings_provider_restore_any_version", 0) == 1;
        if (i > Build.VERSION.SDK_INT && z) {
            Log.w("SettingsBackupAgent", "Ignoring restore from API" + i + " to API" + Build.VERSION.SDK_INT + " due to settings flag override.");
            return;
        }
        this.mRestoredFromSdkInt = i;
        HashSet<String> hashSet = new HashSet<>();
        Settings.System.getMovedToGlobalSettings(hashSet);
        Settings.Secure.getMovedToGlobalSettings(hashSet);
        byte[] bArr = null;
        byte[] bArr2 = null;
        while (backupDataInput.readNextHeader()) {
            String key = backupDataInput.getKey();
            int dataSize = backupDataInput.getDataSize();
            if (i > Build.VERSION.SDK_INT && !RESTORE_FROM_HIGHER_SDK_INT_SUPPORTED_KEYS.contains(key)) {
                Log.w("SettingsBackupAgent", "Not restoring unrecognized key '" + key + "' from future version " + i);
                backupDataInput.skipEntityData();
            } else {
                switch (key) {
                    case "system":
                        restoreSettings(backupDataInput, Settings.System.CONTENT_URI, hashSet);
                        this.mSettingsHelper.applyAudioSettings();
                        break;
                    case "secure":
                        restoreSettings(backupDataInput, Settings.Secure.CONTENT_URI, hashSet);
                        break;
                    case "global":
                        restoreSettings(backupDataInput, Settings.Global.CONTENT_URI, null);
                        break;
                    case "￭WIFI":
                        bArr = new byte[dataSize];
                        backupDataInput.readEntityData(bArr, 0, dataSize);
                        break;
                    case "locale":
                        byte[] bArr3 = new byte[dataSize];
                        backupDataInput.readEntityData(bArr3, 0, dataSize);
                        this.mSettingsHelper.setLocaleData(bArr3, dataSize);
                        break;
                    case "￭CONFIG_WIFI":
                        bArr2 = new byte[dataSize];
                        backupDataInput.readEntityData(bArr2, 0, dataSize);
                        break;
                    case "lock_settings":
                        restoreLockSettings(UserHandle.myUserId(), backupDataInput);
                        break;
                    case "softap_config":
                        byte[] bArr4 = new byte[dataSize];
                        backupDataInput.readEntityData(bArr4, 0, dataSize);
                        restoreSoftApConfiguration(bArr4);
                        break;
                    case "network_policies":
                        byte[] bArr5 = new byte[dataSize];
                        backupDataInput.readEntityData(bArr5, 0, dataSize);
                        restoreNetworkPolicies(bArr5);
                        break;
                    case "wifi_new_config":
                        byte[] bArr6 = new byte[dataSize];
                        backupDataInput.readEntityData(bArr6, 0, dataSize);
                        restoreNewWifiConfigData(bArr6);
                        break;
                    default:
                        backupDataInput.skipEntityData();
                        break;
                }
            }
        }
        if (bArr != null) {
            restoreSupplicantWifiConfigData(bArr, bArr2);
        }
    }

    @Override
    public void onFullBackup(FullBackupDataOutput fullBackupDataOutput) throws IOException {
    }

    public void onRestoreFile(ParcelFileDescriptor parcelFileDescriptor, long j, int i, String str, String str2, long j2, long j3) throws BackupUtils.BadVersionException, IOException {
        DataInputStream dataInputStream = new DataInputStream(new FileInputStream(parcelFileDescriptor.getFileDescriptor()));
        int i2 = dataInputStream.readInt();
        if (i2 <= 6) {
            HashSet<String> hashSet = new HashSet<>();
            Settings.System.getMovedToGlobalSettings(hashSet);
            Settings.Secure.getMovedToGlobalSettings(hashSet);
            int i3 = dataInputStream.readInt();
            byte[] bArr = new byte[i3];
            dataInputStream.readFully(bArr, 0, i3);
            restoreSettings(bArr, i3, Settings.System.CONTENT_URI, hashSet);
            int i4 = dataInputStream.readInt();
            if (i4 > bArr.length) {
                bArr = new byte[i4];
            }
            dataInputStream.readFully(bArr, 0, i4);
            restoreSettings(bArr, i4, Settings.Secure.CONTENT_URI, hashSet);
            if (i2 >= 2) {
                int i5 = dataInputStream.readInt();
                if (i5 > bArr.length) {
                    bArr = new byte[i5];
                }
                dataInputStream.readFully(bArr, 0, i5);
                hashSet.clear();
                restoreSettings(bArr, i5, Settings.Global.CONTENT_URI, hashSet);
            }
            int i6 = dataInputStream.readInt();
            if (i6 > bArr.length) {
                bArr = new byte[i6];
            }
            dataInputStream.readFully(bArr, 0, i6);
            this.mSettingsHelper.setLocaleData(bArr, i6);
            if (i2 < 6) {
                int i7 = dataInputStream.readInt();
                byte[] bArr2 = new byte[i7];
                dataInputStream.readFully(bArr2, 0, i7);
                byte[] bArr3 = new byte[dataInputStream.readInt()];
                dataInputStream.readFully(bArr3, 0, i6);
                restoreSupplicantWifiConfigData(bArr2, bArr3);
            }
            if (i2 >= 3) {
                int i8 = dataInputStream.readInt();
                if (i8 > bArr.length) {
                    bArr = new byte[i8];
                }
                if (i8 > 0) {
                    dataInputStream.readFully(bArr, 0, i8);
                    restoreLockSettings(UserHandle.myUserId(), bArr, i8);
                }
            }
            if (i2 >= 4) {
                int i9 = dataInputStream.readInt();
                if (i9 > bArr.length) {
                    bArr = new byte[i9];
                }
                if (i9 > 0) {
                    dataInputStream.readFully(bArr, 0, i9);
                    restoreSoftApConfiguration(bArr);
                }
            }
            if (i2 >= 5) {
                int i10 = dataInputStream.readInt();
                if (i10 > bArr.length) {
                    bArr = new byte[i10];
                }
                if (i10 > 0) {
                    dataInputStream.readFully(bArr, 0, i10);
                    restoreNetworkPolicies(bArr);
                }
            }
            if (i2 >= 6) {
                int i11 = dataInputStream.readInt();
                if (i11 > bArr.length) {
                    bArr = new byte[i11];
                }
                dataInputStream.readFully(bArr, 0, i11);
                restoreNewWifiConfigData(bArr);
                return;
            }
            return;
        }
        parcelFileDescriptor.close();
        throw new IOException("Invalid file schema");
    }

    private long[] readOldChecksums(ParcelFileDescriptor parcelFileDescriptor) throws IOException {
        long[] jArr = new long[10];
        DataInputStream dataInputStream = new DataInputStream(new FileInputStream(parcelFileDescriptor.getFileDescriptor()));
        try {
            int i = dataInputStream.readInt();
            if (i > 7) {
                i = 7;
            }
            for (int i2 = 0; i2 < STATE_SIZES[i]; i2++) {
                jArr[i2] = dataInputStream.readLong();
            }
        } catch (EOFException e) {
        }
        dataInputStream.close();
        return jArr;
    }

    private void writeNewChecksums(long[] jArr, ParcelFileDescriptor parcelFileDescriptor) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(parcelFileDescriptor.getFileDescriptor())));
        dataOutputStream.writeInt(7);
        for (int i = 0; i < 10; i++) {
            dataOutputStream.writeLong(jArr[i]);
        }
        dataOutputStream.close();
    }

    private long writeIfChanged(long j, String str, byte[] bArr, BackupDataOutput backupDataOutput) {
        CRC32 crc32 = new CRC32();
        crc32.update(bArr);
        long value = crc32.getValue();
        if (j == value) {
            return j;
        }
        try {
            backupDataOutput.writeEntityHeader(str, bArr.length);
            backupDataOutput.writeEntityData(bArr, bArr.length);
        } catch (IOException e) {
        }
        return value;
    }

    private byte[] getSystemSettings() {
        Cursor cursorQuery = getContentResolver().query(Settings.System.CONTENT_URI, PROJECTION, null, null, null);
        try {
            return extractRelevantValues(cursorQuery, Settings.System.SETTINGS_TO_BACKUP);
        } finally {
            cursorQuery.close();
        }
    }

    private byte[] getSecureSettings() {
        Cursor cursorQuery = getContentResolver().query(Settings.Secure.CONTENT_URI, PROJECTION, null, null, null);
        try {
            return extractRelevantValues(cursorQuery, Settings.Secure.SETTINGS_TO_BACKUP);
        } finally {
            cursorQuery.close();
        }
    }

    private byte[] getGlobalSettings() {
        Cursor cursorQuery = getContentResolver().query(Settings.Global.CONTENT_URI, PROJECTION, null, null, null);
        try {
            return extractRelevantValues(cursorQuery, Settings.Global.SETTINGS_TO_BACKUP);
        } finally {
            cursorQuery.close();
        }
    }

    private byte[] getLockSettings(int i) {
        LockPatternUtils lockPatternUtils = new LockPatternUtils(this);
        boolean zIsOwnerInfoEnabled = lockPatternUtils.isOwnerInfoEnabled(i);
        String ownerInfo = lockPatternUtils.getOwnerInfo(i);
        lockPatternUtils.isLockPatternEnabled(i);
        boolean zIsVisiblePatternEnabled = lockPatternUtils.isVisiblePatternEnabled(i);
        boolean powerButtonInstantlyLocks = lockPatternUtils.getPowerButtonInstantlyLocks(i);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try {
            dataOutputStream.writeUTF("owner_info_enabled");
            dataOutputStream.writeUTF(zIsOwnerInfoEnabled ? "1" : "0");
            if (ownerInfo != null) {
                dataOutputStream.writeUTF("owner_info");
                if (ownerInfo == null) {
                    ownerInfo = "";
                }
                dataOutputStream.writeUTF(ownerInfo);
            }
            if (lockPatternUtils.isVisiblePatternEverChosen(i)) {
                dataOutputStream.writeUTF("visible_pattern_enabled");
                dataOutputStream.writeUTF(zIsVisiblePatternEnabled ? "1" : "0");
            }
            if (lockPatternUtils.isPowerButtonInstantlyLocksEverChosen(i)) {
                dataOutputStream.writeUTF("power_button_instantly_locks");
                dataOutputStream.writeUTF(powerButtonInstantlyLocks ? "1" : "0");
            }
            dataOutputStream.writeUTF("");
            dataOutputStream.flush();
        } catch (IOException e) {
        }
        return byteArrayOutputStream.toByteArray();
    }

    private void restoreSettings(BackupDataInput backupDataInput, Uri uri, HashSet<String> hashSet) {
        byte[] bArr = new byte[backupDataInput.getDataSize()];
        try {
            backupDataInput.readEntityData(bArr, 0, bArr.length);
            restoreSettings(bArr, bArr.length, uri, hashSet);
        } catch (IOException e) {
            Log.e("SettingsBackupAgent", "Couldn't read entity data");
        }
    }

    private void restoreSettings(byte[] bArr, int i, Uri uri, HashSet<String> hashSet) {
        String[] strArrConcat;
        Map<String, SettingsValidators.Validator> map;
        int i2;
        boolean z;
        String str;
        String str2;
        Uri uri2;
        int i3;
        int i4;
        SettingsHelper settingsHelper;
        if (uri.equals(Settings.Secure.CONTENT_URI)) {
            strArrConcat = concat(Settings.Secure.SETTINGS_TO_BACKUP, Settings.Secure.LEGACY_RESTORE_SETTINGS);
            map = Settings.Secure.VALIDATORS;
        } else if (uri.equals(Settings.System.CONTENT_URI)) {
            strArrConcat = concat(Settings.System.SETTINGS_TO_BACKUP, Settings.System.LEGACY_RESTORE_SETTINGS);
            map = Settings.System.VALIDATORS;
        } else if (uri.equals(Settings.Global.CONTENT_URI)) {
            strArrConcat = concat(Settings.Global.SETTINGS_TO_BACKUP, Settings.Global.LEGACY_RESTORE_SETTINGS);
            map = Settings.Global.VALIDATORS;
        } else {
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        String[] strArr = strArrConcat;
        Map<String, SettingsValidators.Validator> map2 = map;
        ArrayMap arrayMap = new ArrayMap();
        ContentValues contentValues = new ContentValues(2);
        SettingsHelper settingsHelper2 = this.mSettingsHelper;
        ContentResolver contentResolver = getContentResolver();
        int length = strArr.length;
        int i5 = 0;
        int i6 = 0;
        while (i6 < length) {
            String str3 = strArr[i6];
            if (arrayMap.indexOfKey(str3) >= 0) {
                str2 = (String) arrayMap.remove(str3);
            } else {
                while (i5 < i) {
                    int i7 = readInt(bArr, i5);
                    int i8 = i5 + 4;
                    String str4 = i7 >= 0 ? new String(bArr, i8, i7) : null;
                    int i9 = i8 + i7;
                    int i10 = readInt(bArr, i9);
                    i5 = i9 + 4;
                    if (i10 >= 0) {
                        str2 = new String(bArr, i5, i10);
                        i5 += i10;
                    } else {
                        str2 = null;
                    }
                    if (!str3.equals(str4)) {
                        arrayMap.put(str4, str2);
                    }
                }
                i2 = i5;
                z = false;
                str = null;
                if (!z) {
                    i3 = i6;
                    i4 = length;
                    settingsHelper = settingsHelper2;
                } else if (!isValidSettingValue(str3, str, map2)) {
                    Log.w("SettingsBackupAgent", "Attempted restore of " + str3 + " setting, but its value didn't pass validation, value: " + str);
                    i3 = i6;
                    i4 = length;
                    settingsHelper = settingsHelper2;
                } else {
                    if (hashSet != null && hashSet.contains(str3)) {
                        uri2 = Settings.Global.CONTENT_URI;
                    } else {
                        uri2 = uri;
                    }
                    Uri uri3 = uri2;
                    i3 = i6;
                    i4 = length;
                    settingsHelper = settingsHelper2;
                    settingsHelper2.restoreValue(this, contentResolver, contentValues, uri3, str3, str, this.mRestoredFromSdkInt);
                }
                i6 = i3 + 1;
                i5 = i2;
                settingsHelper2 = settingsHelper;
                length = i4;
            }
            i2 = i5;
            str = str2;
            z = true;
            if (!z) {
            }
            i6 = i3 + 1;
            i5 = i2;
            settingsHelper2 = settingsHelper;
            length = i4;
        }
    }

    private boolean isValidSettingValue(String str, String str2, Map<String, SettingsValidators.Validator> map) {
        SettingsValidators.Validator validator;
        return (str == null || map == null || (validator = map.get(str)) == null || !validator.validate(str2)) ? false : true;
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

    private void restoreLockSettings(int i, byte[] bArr, int i2) {
        byte b;
        LockPatternUtils lockPatternUtils = new LockPatternUtils(this);
        DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bArr, 0, i2));
        while (true) {
            try {
                String utf = dataInputStream.readUTF();
                if (utf.length() > 0) {
                    String utf2 = dataInputStream.readUTF();
                    int iHashCode = utf.hashCode();
                    if (iHashCode != -1849966140) {
                        if (iHashCode != 412331973) {
                            if (iHashCode != 529054396) {
                                b = (iHashCode == 556982970 && utf.equals("owner_info")) ? (byte) 1 : (byte) -1;
                            } else if (utf.equals("owner_info_enabled")) {
                                b = 0;
                            }
                        } else if (utf.equals("visible_pattern_enabled")) {
                            b = 2;
                        }
                    } else if (utf.equals("power_button_instantly_locks")) {
                        b = 3;
                    }
                    switch (b) {
                        case 0:
                            lockPatternUtils.setOwnerInfoEnabled("1".equals(utf2), i);
                            break;
                        case 1:
                            lockPatternUtils.setOwnerInfo(utf2, i);
                            break;
                        case 2:
                            lockPatternUtils.reportPatternWasChosen(i);
                            lockPatternUtils.setVisiblePatternEnabled("1".equals(utf2), i);
                            break;
                        case 3:
                            lockPatternUtils.setPowerButtonInstantlyLocks("1".equals(utf2), i);
                            break;
                    }
                } else {
                    dataInputStream.close();
                    return;
                }
            } catch (IOException e) {
                return;
            }
        }
    }

    private void restoreLockSettings(int i, BackupDataInput backupDataInput) {
        byte[] bArr = new byte[backupDataInput.getDataSize()];
        try {
            backupDataInput.readEntityData(bArr, 0, bArr.length);
            restoreLockSettings(i, bArr, bArr.length);
        } catch (IOException e) {
            Log.e("SettingsBackupAgent", "Couldn't read entity data");
        }
    }

    private byte[] extractRelevantValues(Cursor cursor, String[] strArr) {
        if (!cursor.moveToFirst()) {
            Log.e("SettingsBackupAgent", "Couldn't read from the cursor");
            return new byte[0];
        }
        int columnIndex = cursor.getColumnIndex("name");
        int columnIndex2 = cursor.getColumnIndex("value");
        byte[][] bArr = new byte[strArr.length * 2][];
        ArrayMap arrayMap = new ArrayMap();
        int length = 0;
        int i = 0;
        for (String str : strArr) {
            String str2 = null;
            boolean z = true;
            if (arrayMap.indexOfKey(str) < 0) {
                while (true) {
                    if (!cursor.isAfterLast()) {
                        String string = cursor.getString(columnIndex);
                        String string2 = cursor.getString(columnIndex2);
                        cursor.moveToNext();
                        if (!str.equals(string)) {
                            arrayMap.put(string, string2);
                        } else {
                            str2 = string2;
                            break;
                        }
                    } else {
                        z = false;
                        break;
                    }
                }
            } else {
                str2 = (String) arrayMap.remove(str);
            }
            if (z) {
                String strOnBackupValue = this.mSettingsHelper.onBackupValue(str, str2);
                byte[] bytes = str.getBytes();
                int length2 = length + bytes.length + 4;
                int i2 = i * 2;
                bArr[i2] = bytes;
                byte[] bytes2 = strOnBackupValue != null ? strOnBackupValue.getBytes() : NULL_VALUE;
                length = length2 + 4 + bytes2.length;
                bArr[i2 + 1] = bytes2;
                i++;
            }
        }
        byte[] bArr2 = new byte[length];
        int i3 = i * 2;
        int iWriteInt = 0;
        for (int i4 = 0; i4 < i3; i4++) {
            byte[] bArr3 = bArr[i4];
            if (bArr3 != NULL_VALUE) {
                iWriteInt = writeBytes(bArr2, writeInt(bArr2, iWriteInt, bArr3.length), bArr3);
            } else {
                iWriteInt = writeInt(bArr2, iWriteInt, -1);
            }
        }
        return bArr2;
    }

    private void restoreSupplicantWifiConfigData(byte[] bArr, byte[] bArr2) {
        this.mWifiManager.restoreSupplicantBackupData(bArr, bArr2);
    }

    private byte[] getSoftAPConfiguration() {
        try {
            return this.mWifiManager.getWifiApConfiguration().getBytesForBackup();
        } catch (IOException e) {
            Log.e("SettingsBackupAgent", "Failed to marshal SoftAPConfiguration" + e.getMessage());
            return new byte[0];
        }
    }

    private void restoreSoftApConfiguration(byte[] bArr) {
        try {
            this.mWifiManager.setWifiApConfiguration(WifiConfiguration.getWifiConfigFromBackup(new DataInputStream(new ByteArrayInputStream(bArr))));
        } catch (IOException | BackupUtils.BadVersionException e) {
            Log.e("SettingsBackupAgent", "Failed to unMarshal SoftAPConfiguration " + e.getMessage());
        }
    }

    private byte[] getNetworkPolicies() {
        NetworkPolicy[] networkPolicies = ((NetworkPolicyManager) getSystemService("netpolicy")).getNetworkPolicies();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        if (networkPolicies != null && networkPolicies.length != 0) {
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            try {
                dataOutputStream.writeInt(1);
                dataOutputStream.writeInt(networkPolicies.length);
                for (NetworkPolicy networkPolicy : networkPolicies) {
                    if (networkPolicy != null && !networkPolicy.inferred) {
                        byte[] bytesForBackup = networkPolicy.getBytesForBackup();
                        dataOutputStream.writeByte(1);
                        dataOutputStream.writeInt(bytesForBackup.length);
                        dataOutputStream.write(bytesForBackup);
                    } else {
                        dataOutputStream.writeByte(0);
                    }
                }
            } catch (IOException e) {
                Log.e("SettingsBackupAgent", "Failed to convert NetworkPolicies to byte array " + e.getMessage());
                byteArrayOutputStream.reset();
            }
        }
        return byteArrayOutputStream.toByteArray();
    }

    private byte[] getNewWifiConfigData() {
        return this.mWifiManager.retrieveBackupData();
    }

    private void restoreNewWifiConfigData(byte[] bArr) {
        this.mWifiManager.restoreBackupData(bArr);
    }

    private void restoreNetworkPolicies(byte[] bArr) throws BackupUtils.BadVersionException {
        NetworkPolicyManager networkPolicyManager = (NetworkPolicyManager) getSystemService("netpolicy");
        if (bArr != null && bArr.length != 0) {
            DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bArr));
            try {
                int i = dataInputStream.readInt();
                if (i < 1 || i > 1) {
                    throw new BackupUtils.BadVersionException("Unknown Backup Serialization Version");
                }
                int i2 = dataInputStream.readInt();
                NetworkPolicy[] networkPolicyArr = new NetworkPolicy[i2];
                for (int i3 = 0; i3 < i2; i3++) {
                    if (dataInputStream.readByte() != 0) {
                        int i4 = dataInputStream.readInt();
                        byte[] bArr2 = new byte[i4];
                        dataInputStream.read(bArr2, 0, i4);
                        networkPolicyArr[i3] = NetworkPolicy.getNetworkPolicyFromBackup(new DataInputStream(new ByteArrayInputStream(bArr2)));
                    }
                }
                networkPolicyManager.setNetworkPolicies(networkPolicyArr);
            } catch (IOException | NullPointerException | BackupUtils.BadVersionException | DateTimeException e) {
                Log.e("SettingsBackupAgent", "Failed to convert byte array to NetworkPolicies " + e.getMessage());
            }
        }
    }

    private int writeInt(byte[] bArr, int i, int i2) {
        bArr[i + 0] = (byte) ((i2 >> 24) & 255);
        bArr[i + 1] = (byte) ((i2 >> 16) & 255);
        bArr[i + 2] = (byte) ((i2 >> 8) & 255);
        bArr[i + 3] = (byte) ((i2 >> 0) & 255);
        return i + 4;
    }

    private int writeBytes(byte[] bArr, int i, byte[] bArr2) {
        System.arraycopy(bArr2, 0, bArr, i, bArr2.length);
        return i + bArr2.length;
    }

    private int readInt(byte[] bArr, int i) {
        return ((bArr[i + 3] & 255) << 0) | ((bArr[i] & 255) << 24) | ((bArr[i + 1] & 255) << 16) | ((bArr[i + 2] & 255) << 8);
    }
}
