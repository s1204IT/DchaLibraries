package com.android.server.backup.utils;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Slog;
import android.util.StringBuilderPrinter;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.backup.BackupManagerService;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FullBackupUtils {
    public static void routeSocketDataToOutput(ParcelFileDescriptor parcelFileDescriptor, OutputStream outputStream) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(new FileInputStream(parcelFileDescriptor.getFileDescriptor()));
        byte[] bArr = new byte[32768];
        while (true) {
            int i = dataInputStream.readInt();
            if (i > 0) {
                while (i > 0) {
                    int i2 = dataInputStream.read(bArr, 0, i > bArr.length ? bArr.length : i);
                    if (i2 < 0) {
                        Slog.e(BackupManagerService.TAG, "Unexpectedly reached end of file while reading data");
                        throw new EOFException();
                    }
                    outputStream.write(bArr, 0, i2);
                    i -= i2;
                }
            } else {
                return;
            }
        }
    }

    public static void writeAppManifest(PackageInfo packageInfo, PackageManager packageManager, File file, boolean z, boolean z2) throws IOException {
        StringBuilder sb = new StringBuilder(4096);
        StringBuilderPrinter stringBuilderPrinter = new StringBuilderPrinter(sb);
        stringBuilderPrinter.println(Integer.toString(1));
        stringBuilderPrinter.println(packageInfo.packageName);
        stringBuilderPrinter.println(Long.toString(packageInfo.getLongVersionCode()));
        stringBuilderPrinter.println(Integer.toString(Build.VERSION.SDK_INT));
        String installerPackageName = packageManager.getInstallerPackageName(packageInfo.packageName);
        if (installerPackageName == null) {
            installerPackageName = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        stringBuilderPrinter.println(installerPackageName);
        stringBuilderPrinter.println(z ? "1" : "0");
        SigningInfo signingInfo = packageInfo.signingInfo;
        if (signingInfo == null) {
            stringBuilderPrinter.println("0");
        } else {
            Signature[] apkContentsSigners = signingInfo.getApkContentsSigners();
            stringBuilderPrinter.println(Integer.toString(apkContentsSigners.length));
            for (Signature signature : apkContentsSigners) {
                stringBuilderPrinter.println(signature.toCharsString());
            }
        }
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(sb.toString().getBytes());
        fileOutputStream.close();
        file.setLastModified(0L);
    }
}
