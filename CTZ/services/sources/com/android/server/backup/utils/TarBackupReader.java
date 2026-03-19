package com.android.server.backup.utils;

import android.app.backup.IBackupManagerMonitor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.Signature;
import android.os.Bundle;
import android.util.Slog;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.FileMetadata;
import com.android.server.backup.restore.RestorePolicy;
import com.android.server.pm.DumpState;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class TarBackupReader {
    private static final int TAR_HEADER_LENGTH_FILESIZE = 12;
    private static final int TAR_HEADER_LENGTH_MODE = 8;
    private static final int TAR_HEADER_LENGTH_MODTIME = 12;
    private static final int TAR_HEADER_LENGTH_PATH = 100;
    private static final int TAR_HEADER_LENGTH_PATH_PREFIX = 155;
    private static final int TAR_HEADER_LONG_RADIX = 8;
    private static final int TAR_HEADER_OFFSET_FILESIZE = 124;
    private static final int TAR_HEADER_OFFSET_MODE = 100;
    private static final int TAR_HEADER_OFFSET_MODTIME = 136;
    private static final int TAR_HEADER_OFFSET_PATH = 0;
    private static final int TAR_HEADER_OFFSET_PATH_PREFIX = 345;
    private static final int TAR_HEADER_OFFSET_TYPE_CHAR = 156;
    private final BytesReadListener mBytesReadListener;
    private final InputStream mInputStream;
    private IBackupManagerMonitor mMonitor;
    private byte[] mWidgetData = null;

    public TarBackupReader(InputStream inputStream, BytesReadListener bytesReadListener, IBackupManagerMonitor iBackupManagerMonitor) {
        this.mInputStream = inputStream;
        this.mBytesReadListener = bytesReadListener;
        this.mMonitor = iBackupManagerMonitor;
    }

    public FileMetadata readTarHeaders() throws IOException {
        byte[] bArr = new byte[512];
        if (!readTarHeader(bArr)) {
            return null;
        }
        try {
            FileMetadata fileMetadata = new FileMetadata();
            fileMetadata.size = extractRadix(bArr, TAR_HEADER_OFFSET_FILESIZE, 12, 8);
            fileMetadata.mtime = extractRadix(bArr, 136, 12, 8);
            fileMetadata.mode = extractRadix(bArr, 100, 8, 8);
            fileMetadata.path = extractString(bArr, TAR_HEADER_OFFSET_PATH_PREFIX, TAR_HEADER_LENGTH_PATH_PREFIX);
            String strExtractString = extractString(bArr, 0, 100);
            if (strExtractString.length() > 0) {
                if (fileMetadata.path.length() > 0) {
                    fileMetadata.path += '/';
                }
                fileMetadata.path += strExtractString;
            }
            byte b = bArr[TAR_HEADER_OFFSET_TYPE_CHAR];
            if (b == 120) {
                boolean paxExtendedHeader = readPaxExtendedHeader(fileMetadata);
                if (paxExtendedHeader) {
                    paxExtendedHeader = readTarHeader(bArr);
                }
                if (!paxExtendedHeader) {
                    throw new IOException("Bad or missing pax header");
                }
                b = bArr[TAR_HEADER_OFFSET_TYPE_CHAR];
            }
            if (b == 0) {
                return null;
            }
            if (b == 48) {
                fileMetadata.type = 1;
            } else if (b == 53) {
                fileMetadata.type = 2;
                if (fileMetadata.size != 0) {
                    Slog.w(BackupManagerService.TAG, "Directory entry with nonzero size in header");
                    fileMetadata.size = 0L;
                }
            } else {
                Slog.e(BackupManagerService.TAG, "Unknown tar entity type: " + ((int) b));
                throw new IOException("Unknown entity type " + ((int) b));
            }
            if ("shared/".regionMatches(0, fileMetadata.path, 0, "shared/".length())) {
                fileMetadata.path = fileMetadata.path.substring("shared/".length());
                fileMetadata.packageName = BackupManagerService.SHARED_BACKUP_AGENT_PACKAGE;
                fileMetadata.domain = "shared";
                Slog.i(BackupManagerService.TAG, "File in shared storage: " + fileMetadata.path);
                return fileMetadata;
            }
            if (!"apps/".regionMatches(0, fileMetadata.path, 0, "apps/".length())) {
                return fileMetadata;
            }
            fileMetadata.path = fileMetadata.path.substring("apps/".length());
            int iIndexOf = fileMetadata.path.indexOf(47);
            if (iIndexOf >= 0) {
                fileMetadata.packageName = fileMetadata.path.substring(0, iIndexOf);
                fileMetadata.path = fileMetadata.path.substring(iIndexOf + 1);
                if (fileMetadata.path.equals(BackupManagerService.BACKUP_MANIFEST_FILENAME) || fileMetadata.path.equals(BackupManagerService.BACKUP_METADATA_FILENAME)) {
                    return fileMetadata;
                }
                int iIndexOf2 = fileMetadata.path.indexOf(47);
                if (iIndexOf2 >= 0) {
                    fileMetadata.domain = fileMetadata.path.substring(0, iIndexOf2);
                    fileMetadata.path = fileMetadata.path.substring(iIndexOf2 + 1);
                    return fileMetadata;
                }
                throw new IOException("Illegal semantic path in non-manifest " + fileMetadata.path);
            }
            throw new IOException("Illegal semantic path in " + fileMetadata.path);
        } catch (IOException e) {
            Slog.e(BackupManagerService.TAG, "Parse error in header: " + e.getMessage());
            throw e;
        }
    }

    private static int readExactly(InputStream inputStream, byte[] bArr, int i, int i2) throws IOException {
        if (i2 <= 0) {
            throw new IllegalArgumentException("size must be > 0");
        }
        int i3 = 0;
        while (i3 < i2) {
            int i4 = inputStream.read(bArr, i + i3, i2 - i3);
            if (i4 <= 0) {
                break;
            }
            i3 += i4;
        }
        return i3;
    }

    public Signature[] readAppManifestAndReturnSignatures(FileMetadata fileMetadata) throws IOException {
        if (fileMetadata.size > 65536) {
            throw new IOException("Restore manifest too big; corrupt? size=" + fileMetadata.size);
        }
        byte[] bArr = new byte[(int) fileMetadata.size];
        if (readExactly(this.mInputStream, bArr, 0, (int) fileMetadata.size) == fileMetadata.size) {
            this.mBytesReadListener.onBytesRead(fileMetadata.size);
            String[] strArr = new String[1];
            try {
                int iExtractLine = extractLine(bArr, 0, strArr);
                int i = Integer.parseInt(strArr[0]);
                if (i == 1) {
                    int iExtractLine2 = extractLine(bArr, iExtractLine, strArr);
                    String str = strArr[0];
                    if (str.equals(fileMetadata.packageName)) {
                        int iExtractLine3 = extractLine(bArr, iExtractLine2, strArr);
                        fileMetadata.version = Integer.parseInt(strArr[0]);
                        int iExtractLine4 = extractLine(bArr, iExtractLine3, strArr);
                        Integer.parseInt(strArr[0]);
                        int iExtractLine5 = extractLine(bArr, iExtractLine4, strArr);
                        fileMetadata.installerPackageName = strArr[0].length() > 0 ? strArr[0] : null;
                        int iExtractLine6 = extractLine(bArr, iExtractLine5, strArr);
                        fileMetadata.hasApk = strArr[0].equals("1");
                        int iExtractLine7 = extractLine(bArr, iExtractLine6, strArr);
                        int i2 = Integer.parseInt(strArr[0]);
                        if (i2 > 0) {
                            Signature[] signatureArr = new Signature[i2];
                            int iExtractLine8 = iExtractLine7;
                            for (int i3 = 0; i3 < i2; i3++) {
                                iExtractLine8 = extractLine(bArr, iExtractLine8, strArr);
                                signatureArr[i3] = new Signature(strArr[0]);
                            }
                            return signatureArr;
                        }
                        Slog.i(BackupManagerService.TAG, "Missing signature on backed-up package " + fileMetadata.packageName);
                        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 42, null, 3, BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", fileMetadata.packageName));
                    } else {
                        Slog.i(BackupManagerService.TAG, "Expected package " + fileMetadata.packageName + " but restore manifest claims " + str);
                        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 43, null, 3, BackupManagerMonitorUtils.putMonitoringExtra(BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", fileMetadata.packageName), "android.app.backup.extra.LOG_MANIFEST_PACKAGE_NAME", str));
                    }
                } else {
                    Slog.i(BackupManagerService.TAG, "Unknown restore manifest version " + i + " for package " + fileMetadata.packageName);
                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 44, null, 3, BackupManagerMonitorUtils.putMonitoringExtra(BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", fileMetadata.packageName), "android.app.backup.extra.LOG_EVENT_PACKAGE_VERSION", (long) i));
                }
            } catch (NumberFormatException e) {
                Slog.w(BackupManagerService.TAG, "Corrupt restore manifest for package " + fileMetadata.packageName);
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 46, null, 3, BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", fileMetadata.packageName));
            } catch (IllegalArgumentException e2) {
                Slog.w(BackupManagerService.TAG, e2.getMessage());
            }
            return null;
        }
        throw new IOException("Unexpected EOF in manifest");
    }

    public RestorePolicy chooseRestorePolicy(PackageManager packageManager, boolean z, FileMetadata fileMetadata, Signature[] signatureArr, PackageManagerInternal packageManagerInternal) {
        RestorePolicy restorePolicy;
        if (signatureArr == null) {
            return RestorePolicy.IGNORE;
        }
        RestorePolicy restorePolicy2 = RestorePolicy.IGNORE;
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(fileMetadata.packageName, 134217728);
            if ((packageInfo.applicationInfo.flags & 32768) != 0) {
                if (packageInfo.applicationInfo.uid >= 10000 || packageInfo.applicationInfo.backupAgentName != null) {
                    if (AppBackupUtils.signaturesMatch(signatureArr, packageInfo, packageManagerInternal)) {
                        if ((packageInfo.applicationInfo.flags & DumpState.DUMP_INTENT_FILTER_VERIFIERS) != 0) {
                            Slog.i(BackupManagerService.TAG, "Package has restoreAnyVersion; taking data");
                            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 34, packageInfo, 3, null);
                            restorePolicy2 = RestorePolicy.ACCEPT;
                        } else if (packageInfo.getLongVersionCode() >= fileMetadata.version) {
                            Slog.i(BackupManagerService.TAG, "Sig + version match; taking data");
                            restorePolicy2 = RestorePolicy.ACCEPT;
                            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 35, packageInfo, 3, null);
                        } else if (z) {
                            Slog.i(BackupManagerService.TAG, "Data version " + fileMetadata.version + " is newer than installed version " + packageInfo.getLongVersionCode() + " - requiring apk");
                            restorePolicy2 = RestorePolicy.ACCEPT_IF_APK;
                        } else {
                            Slog.i(BackupManagerService.TAG, "Data requires newer version " + fileMetadata.version + "; ignoring");
                            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 36, packageInfo, 3, BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_OLD_VERSION", fileMetadata.version));
                            restorePolicy2 = RestorePolicy.IGNORE;
                        }
                    } else {
                        Slog.w(BackupManagerService.TAG, "Restore manifest signatures do not match installed application for " + fileMetadata.packageName);
                        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 37, packageInfo, 3, null);
                    }
                } else {
                    Slog.w(BackupManagerService.TAG, "Package " + fileMetadata.packageName + " is system level with no agent");
                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 38, packageInfo, 2, null);
                }
            } else {
                Slog.i(BackupManagerService.TAG, "Restore manifest from " + fileMetadata.packageName + " but allowBackup=false");
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 39, packageInfo, 3, null);
            }
        } catch (PackageManager.NameNotFoundException e) {
            if (z) {
                Slog.i(BackupManagerService.TAG, "Package " + fileMetadata.packageName + " not installed; requiring apk in dataset");
                restorePolicy = RestorePolicy.ACCEPT_IF_APK;
            } else {
                restorePolicy = RestorePolicy.IGNORE;
            }
            restorePolicy2 = restorePolicy;
            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 40, null, 3, BackupManagerMonitorUtils.putMonitoringExtra(BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", fileMetadata.packageName), "android.app.backup.extra.LOG_POLICY_ALLOW_APKS", z));
        }
        if (restorePolicy2 == RestorePolicy.ACCEPT_IF_APK && !fileMetadata.hasApk) {
            Slog.i(BackupManagerService.TAG, "Cannot restore package " + fileMetadata.packageName + " without the matching .apk");
            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 41, null, 3, BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", fileMetadata.packageName));
        }
        return restorePolicy2;
    }

    public void skipTarPadding(long j) throws IOException {
        long j2 = (j + 512) % 512;
        if (j2 > 0) {
            int i = 512 - ((int) j2);
            if (readExactly(this.mInputStream, new byte[i], 0, i) == i) {
                this.mBytesReadListener.onBytesRead(i);
                return;
            }
            throw new IOException("Unexpected EOF in padding");
        }
    }

    public void readMetadata(FileMetadata fileMetadata) throws IOException {
        if (fileMetadata.size > 65536) {
            throw new IOException("Metadata too big; corrupt? size=" + fileMetadata.size);
        }
        byte[] bArr = new byte[(int) fileMetadata.size];
        if (readExactly(this.mInputStream, bArr, 0, (int) fileMetadata.size) == fileMetadata.size) {
            this.mBytesReadListener.onBytesRead(fileMetadata.size);
            String[] strArr = new String[1];
            int iExtractLine = extractLine(bArr, 0, strArr);
            int i = Integer.parseInt(strArr[0]);
            if (i == 1) {
                int iExtractLine2 = extractLine(bArr, iExtractLine, strArr);
                String str = strArr[0];
                if (fileMetadata.packageName.equals(str)) {
                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bArr, iExtractLine2, bArr.length - iExtractLine2);
                    DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
                    while (byteArrayInputStream.available() > 0) {
                        int i2 = dataInputStream.readInt();
                        int i3 = dataInputStream.readInt();
                        if (i3 > 65536) {
                            throw new IOException("Datum " + Integer.toHexString(i2) + " too big; corrupt? size=" + fileMetadata.size);
                        }
                        if (i2 == 33549569) {
                            this.mWidgetData = new byte[i3];
                            dataInputStream.read(this.mWidgetData);
                        } else {
                            Slog.i(BackupManagerService.TAG, "Ignoring metadata blob " + Integer.toHexString(i2) + " for " + fileMetadata.packageName);
                            dataInputStream.skipBytes(i3);
                        }
                    }
                    return;
                }
                Slog.w(BackupManagerService.TAG, "Metadata mismatch: package " + fileMetadata.packageName + " but widget data for " + str);
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 47, null, 3, BackupManagerMonitorUtils.putMonitoringExtra(BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", fileMetadata.packageName), "android.app.backup.extra.LOG_WIDGET_PACKAGE_NAME", str));
                return;
            }
            Slog.w(BackupManagerService.TAG, "Unsupported metadata version " + i);
            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 48, null, 3, BackupManagerMonitorUtils.putMonitoringExtra(BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", fileMetadata.packageName), "android.app.backup.extra.LOG_EVENT_PACKAGE_VERSION", (long) i));
            return;
        }
        throw new IOException("Unexpected EOF in widget data");
    }

    private static int extractLine(byte[] bArr, int i, String[] strArr) throws IOException {
        int length = bArr.length;
        if (i >= length) {
            throw new IOException("Incomplete data");
        }
        int i2 = i;
        while (i2 < length && bArr[i2] != 10) {
            i2++;
        }
        strArr[0] = new String(bArr, i, i2 - i);
        return i2 + 1;
    }

    private boolean readTarHeader(byte[] bArr) throws IOException {
        int exactly = readExactly(this.mInputStream, bArr, 0, 512);
        if (exactly == 0) {
            return false;
        }
        if (exactly < 512) {
            throw new IOException("Unable to read full block header");
        }
        this.mBytesReadListener.onBytesRead(512L);
        return true;
    }

    private boolean readPaxExtendedHeader(FileMetadata fileMetadata) throws IOException {
        if (fileMetadata.size > 32768) {
            Slog.w(BackupManagerService.TAG, "Suspiciously large pax header size " + fileMetadata.size + " - aborting");
            throw new IOException("Sanity failure: pax header size " + fileMetadata.size);
        }
        byte[] bArr = new byte[((int) ((fileMetadata.size + 511) >> 9)) * 512];
        int i = 0;
        if (readExactly(this.mInputStream, bArr, 0, bArr.length) < bArr.length) {
            throw new IOException("Unable to read full pax header");
        }
        this.mBytesReadListener.onBytesRead(bArr.length);
        int i2 = (int) fileMetadata.size;
        do {
            int i3 = i + 1;
            while (i3 < i2 && bArr[i3] != 32) {
                i3++;
            }
            if (i3 >= i2) {
                throw new IOException("Invalid pax data");
            }
            int iExtractRadix = (int) extractRadix(bArr, i, i3 - i, 10);
            int i4 = i3 + 1;
            i += iExtractRadix;
            int i5 = i - 1;
            int i6 = i4 + 1;
            while (bArr[i6] != 61 && i6 <= i5) {
                i6++;
            }
            if (i6 > i5) {
                throw new IOException("Invalid pax declaration");
            }
            String str = new String(bArr, i4, i6 - i4, "UTF-8");
            String str2 = new String(bArr, i6 + 1, (i5 - i6) - 1, "UTF-8");
            if ("path".equals(str)) {
                fileMetadata.path = str2;
            } else if ("size".equals(str)) {
                fileMetadata.size = Long.parseLong(str2);
            } else {
                Slog.i(BackupManagerService.TAG, "Unhandled pax key: " + i4);
            }
        } while (i < i2);
        return true;
    }

    private static long extractRadix(byte[] bArr, int i, int i2, int i3) throws IOException {
        int i4 = i2 + i;
        long j = 0;
        while (i < i4) {
            int i5 = bArr[i];
            if (i5 == 0 || i5 == 32) {
                break;
            }
            if (i5 < 48 || i5 > (48 + i3) - 1) {
                throw new IOException("Invalid number in header: '" + ((char) i5) + "' for radix " + i3);
            }
            j = ((long) (i5 - 48)) + (((long) i3) * j);
            i++;
        }
        return j;
    }

    private static String extractString(byte[] bArr, int i, int i2) throws IOException {
        int i3 = i2 + i;
        int i4 = i;
        while (i4 < i3 && bArr[i4] != 0) {
            i4++;
        }
        return new String(bArr, i, i4 - i, "US-ASCII");
    }

    private static void hexLog(byte[] bArr) {
        int length = bArr.length;
        StringBuilder sb = new StringBuilder(64);
        int i = 0;
        while (length > 0) {
            sb.append(String.format("%04x   ", Integer.valueOf(i)));
            int i2 = 16;
            if (length <= 16) {
                i2 = length;
            }
            for (int i3 = 0; i3 < i2; i3++) {
                sb.append(String.format("%02x ", Byte.valueOf(bArr[i + i3])));
            }
            Slog.i("hexdump", sb.toString());
            sb.setLength(0);
            length -= i2;
            i += i2;
        }
    }

    public IBackupManagerMonitor getMonitor() {
        return this.mMonitor;
    }

    public byte[] getWidgetData() {
        return this.mWidgetData;
    }
}
