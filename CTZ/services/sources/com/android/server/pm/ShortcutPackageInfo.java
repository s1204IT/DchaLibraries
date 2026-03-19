package com.android.server.pm;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManagerInternal;
import android.content.pm.SigningInfo;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.LocalServices;
import com.android.server.backup.BackupUtils;
import com.android.server.hdmi.HdmiCecKeycode;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Base64;
import libcore.util.HexEncoding;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class ShortcutPackageInfo {
    private static final String ATTR_BACKUP_ALLOWED = "allow-backup";
    private static final String ATTR_BACKUP_ALLOWED_INITIALIZED = "allow-backup-initialized";
    private static final String ATTR_BACKUP_SOURCE_BACKUP_ALLOWED = "bk_src_backup-allowed";
    private static final String ATTR_BACKUP_SOURCE_VERSION = "bk_src_version";
    private static final String ATTR_LAST_UPDATE_TIME = "last_udpate_time";
    private static final String ATTR_SHADOW = "shadow";
    private static final String ATTR_SIGNATURE_HASH = "hash";
    private static final String ATTR_VERSION = "version";
    private static final String TAG = "ShortcutService";
    static final String TAG_ROOT = "package-info";
    private static final String TAG_SIGNATURE = "signature";
    private boolean mBackupAllowedInitialized;
    private boolean mIsShadow;
    private long mLastUpdateTime;
    private ArrayList<byte[]> mSigHashes;
    private long mVersionCode;
    private long mBackupSourceVersionCode = -1;
    private boolean mBackupAllowed = false;
    private boolean mBackupSourceBackupAllowed = false;

    private ShortcutPackageInfo(long j, long j2, ArrayList<byte[]> arrayList, boolean z) {
        this.mVersionCode = -1L;
        this.mVersionCode = j;
        this.mLastUpdateTime = j2;
        this.mIsShadow = z;
        this.mSigHashes = arrayList;
    }

    public static ShortcutPackageInfo newEmpty() {
        return new ShortcutPackageInfo(-1L, 0L, new ArrayList(0), false);
    }

    public boolean isShadow() {
        return this.mIsShadow;
    }

    public void setShadow(boolean z) {
        this.mIsShadow = z;
    }

    public long getVersionCode() {
        return this.mVersionCode;
    }

    public long getBackupSourceVersionCode() {
        return this.mBackupSourceVersionCode;
    }

    @VisibleForTesting
    public boolean isBackupSourceBackupAllowed() {
        return this.mBackupSourceBackupAllowed;
    }

    public long getLastUpdateTime() {
        return this.mLastUpdateTime;
    }

    public boolean isBackupAllowed() {
        return this.mBackupAllowed;
    }

    public void updateFromPackageInfo(PackageInfo packageInfo) {
        if (packageInfo != null) {
            this.mVersionCode = packageInfo.getLongVersionCode();
            this.mLastUpdateTime = packageInfo.lastUpdateTime;
            this.mBackupAllowed = ShortcutService.shouldBackupApp(packageInfo);
            this.mBackupAllowedInitialized = true;
        }
    }

    public boolean hasSignatures() {
        return this.mSigHashes.size() > 0;
    }

    public int canRestoreTo(ShortcutService shortcutService, PackageInfo packageInfo, boolean z) {
        if (!BackupUtils.signaturesMatch(this.mSigHashes, packageInfo, (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class))) {
            Slog.w(TAG, "Can't restore: Package signature mismatch");
            return HdmiCecKeycode.CEC_KEYCODE_RESTORE_VOLUME_FUNCTION;
        }
        if (!ShortcutService.shouldBackupApp(packageInfo) || !this.mBackupSourceBackupAllowed) {
            Slog.w(TAG, "Can't restore: package didn't or doesn't allow backup");
            return 101;
        }
        if (z || packageInfo.getLongVersionCode() >= this.mBackupSourceVersionCode) {
            return 0;
        }
        Slog.w(TAG, String.format("Can't restore: package current version %d < backed up version %d", Long.valueOf(packageInfo.getLongVersionCode()), Long.valueOf(this.mBackupSourceVersionCode)));
        return 100;
    }

    @VisibleForTesting
    public static ShortcutPackageInfo generateForInstalledPackageForTest(ShortcutService shortcutService, String str, int i) {
        PackageInfo packageInfoWithSignatures = shortcutService.getPackageInfoWithSignatures(str, i);
        SigningInfo signingInfo = packageInfoWithSignatures.signingInfo;
        if (signingInfo == null) {
            Slog.e(TAG, "Can't get signatures: package=" + str);
            return null;
        }
        ShortcutPackageInfo shortcutPackageInfo = new ShortcutPackageInfo(packageInfoWithSignatures.getLongVersionCode(), packageInfoWithSignatures.lastUpdateTime, BackupUtils.hashSignatureArray(signingInfo.getApkContentsSigners()), false);
        shortcutPackageInfo.mBackupSourceBackupAllowed = ShortcutService.shouldBackupApp(packageInfoWithSignatures);
        shortcutPackageInfo.mBackupSourceVersionCode = packageInfoWithSignatures.getLongVersionCode();
        return shortcutPackageInfo;
    }

    public void refreshSignature(ShortcutService shortcutService, ShortcutPackageItem shortcutPackageItem) {
        if (this.mIsShadow) {
            shortcutService.wtf("Attempted to refresh package info for shadow package " + shortcutPackageItem.getPackageName() + ", user=" + shortcutPackageItem.getOwnerUserId());
            return;
        }
        PackageInfo packageInfoWithSignatures = shortcutService.getPackageInfoWithSignatures(shortcutPackageItem.getPackageName(), shortcutPackageItem.getPackageUserId());
        if (packageInfoWithSignatures == null) {
            Slog.w(TAG, "Package not found: " + shortcutPackageItem.getPackageName());
            return;
        }
        SigningInfo signingInfo = packageInfoWithSignatures.signingInfo;
        if (signingInfo == null) {
            Slog.w(TAG, "Not refreshing signature for " + shortcutPackageItem.getPackageName() + " since it appears to have no signing info.");
            return;
        }
        this.mSigHashes = BackupUtils.hashSignatureArray(signingInfo.getApkContentsSigners());
    }

    public void saveToXml(ShortcutService shortcutService, XmlSerializer xmlSerializer, boolean z) throws IOException {
        if (z && !this.mBackupAllowedInitialized) {
            shortcutService.wtf("Backup happened before mBackupAllowed is initialized.");
        }
        xmlSerializer.startTag(null, TAG_ROOT);
        ShortcutService.writeAttr(xmlSerializer, ATTR_VERSION, this.mVersionCode);
        ShortcutService.writeAttr(xmlSerializer, ATTR_LAST_UPDATE_TIME, this.mLastUpdateTime);
        ShortcutService.writeAttr(xmlSerializer, ATTR_SHADOW, this.mIsShadow);
        ShortcutService.writeAttr(xmlSerializer, ATTR_BACKUP_ALLOWED, this.mBackupAllowed);
        ShortcutService.writeAttr(xmlSerializer, ATTR_BACKUP_ALLOWED_INITIALIZED, this.mBackupAllowedInitialized);
        ShortcutService.writeAttr(xmlSerializer, ATTR_BACKUP_SOURCE_VERSION, this.mBackupSourceVersionCode);
        ShortcutService.writeAttr(xmlSerializer, ATTR_BACKUP_SOURCE_BACKUP_ALLOWED, this.mBackupSourceBackupAllowed);
        for (int i = 0; i < this.mSigHashes.size(); i++) {
            xmlSerializer.startTag(null, TAG_SIGNATURE);
            ShortcutService.writeAttr(xmlSerializer, ATTR_SIGNATURE_HASH, Base64.getEncoder().encodeToString(this.mSigHashes.get(i)));
            xmlSerializer.endTag(null, TAG_SIGNATURE);
        }
        xmlSerializer.endTag(null, TAG_ROOT);
    }

    public void loadFromXml(XmlPullParser xmlPullParser, boolean z) throws XmlPullParserException, IOException {
        int i;
        long longAttribute = ShortcutService.parseLongAttribute(xmlPullParser, ATTR_VERSION, -1L);
        long longAttribute2 = ShortcutService.parseLongAttribute(xmlPullParser, ATTR_LAST_UPDATE_TIME);
        int i2 = 1;
        boolean z2 = z || ShortcutService.parseBooleanAttribute(xmlPullParser, ATTR_SHADOW);
        long longAttribute3 = ShortcutService.parseLongAttribute(xmlPullParser, ATTR_BACKUP_SOURCE_VERSION, -1L);
        boolean booleanAttribute = ShortcutService.parseBooleanAttribute(xmlPullParser, ATTR_BACKUP_ALLOWED, true);
        boolean booleanAttribute2 = ShortcutService.parseBooleanAttribute(xmlPullParser, ATTR_BACKUP_SOURCE_BACKUP_ALLOWED, true);
        ArrayList<byte[]> arrayList = new ArrayList<>();
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next == i2 || (next == 3 && xmlPullParser.getDepth() <= depth)) {
                break;
            }
            if (next == 2) {
                int depth2 = xmlPullParser.getDepth();
                String name = xmlPullParser.getName();
                boolean z3 = z2;
                if (depth2 == depth + 1) {
                    i = depth;
                    if (((name.hashCode() == 1073584312 && name.equals(TAG_SIGNATURE)) ? (byte) 0 : (byte) -1) == 0) {
                        arrayList.add(Base64.getDecoder().decode(ShortcutService.parseStringAttribute(xmlPullParser, ATTR_SIGNATURE_HASH)));
                    }
                    z2 = z3;
                    depth = i;
                } else {
                    i = depth;
                }
                ShortcutService.warnForInvalidTag(depth2, name);
                z2 = z3;
                depth = i;
            }
            i2 = 1;
        }
        boolean z4 = z2;
        if (z) {
            this.mVersionCode = -1L;
            this.mBackupSourceVersionCode = longAttribute;
            this.mBackupSourceBackupAllowed = booleanAttribute;
        } else {
            this.mVersionCode = longAttribute;
            this.mBackupSourceVersionCode = longAttribute3;
            this.mBackupSourceBackupAllowed = booleanAttribute2;
        }
        this.mLastUpdateTime = longAttribute2;
        this.mIsShadow = z4;
        this.mSigHashes = arrayList;
        this.mBackupAllowed = false;
        this.mBackupAllowedInitialized = false;
    }

    public void dump(PrintWriter printWriter, String str) {
        printWriter.println();
        printWriter.print(str);
        printWriter.println("PackageInfo:");
        printWriter.print(str);
        printWriter.print("  IsShadow: ");
        printWriter.print(this.mIsShadow);
        printWriter.print(this.mIsShadow ? " (not installed)" : " (installed)");
        printWriter.println();
        printWriter.print(str);
        printWriter.print("  Version: ");
        printWriter.print(this.mVersionCode);
        printWriter.println();
        if (this.mBackupAllowedInitialized) {
            printWriter.print(str);
            printWriter.print("  Backup Allowed: ");
            printWriter.print(this.mBackupAllowed);
            printWriter.println();
        }
        if (this.mBackupSourceVersionCode != -1) {
            printWriter.print(str);
            printWriter.print("  Backup source version: ");
            printWriter.print(this.mBackupSourceVersionCode);
            printWriter.println();
            printWriter.print(str);
            printWriter.print("  Backup source backup allowed: ");
            printWriter.print(this.mBackupSourceBackupAllowed);
            printWriter.println();
        }
        printWriter.print(str);
        printWriter.print("  Last package update time: ");
        printWriter.print(this.mLastUpdateTime);
        printWriter.println();
        for (int i = 0; i < this.mSigHashes.size(); i++) {
            printWriter.print(str);
            printWriter.print("    ");
            printWriter.print("SigHash: ");
            printWriter.println(HexEncoding.encode(this.mSigHashes.get(i)));
        }
    }
}
