package com.android.packageinstaller.wear;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.tukaani.xz.LZMAInputStream;
import org.tukaani.xz.XZInputStream;

public class WearPackageUtil {
    public static File getTemporaryFile(Context context, String str) {
        try {
            File file = new File(context.getFilesDir(), "tmp");
            file.mkdirs();
            Os.chmod(file.getAbsolutePath(), 505);
            return new File(file, str + ".apk");
        } catch (ErrnoException e) {
            Log.e("WearablePkgInstaller", "Failed to open.", e);
            return null;
        }
    }

    public static File getIconFile(Context context, String str) {
        try {
            File file = new File(context.getFilesDir(), "images/icons");
            file.mkdirs();
            Os.chmod(file.getAbsolutePath(), 505);
            return new File(file, str + ".icon");
        } catch (ErrnoException e) {
            Log.e("WearablePkgInstaller", "Failed to open.", e);
            return null;
        }
    }

    public static File getFileFromFd(Context context, ParcelFileDescriptor parcelFileDescriptor, String str, String str2) {
        File temporaryFile = getTemporaryFile(context, str);
        if (parcelFileDescriptor == null || parcelFileDescriptor.getFileDescriptor() == null) {
            return null;
        }
        ParcelFileDescriptor.AutoCloseInputStream autoCloseInputStream = new ParcelFileDescriptor.AutoCloseInputStream(parcelFileDescriptor);
        try {
            InputStream xZInputStream = TextUtils.equals(str2, "xz") ? new XZInputStream(autoCloseInputStream) : TextUtils.equals(str2, "lzma") ? new LZMAInputStream(autoCloseInputStream) : autoCloseInputStream;
            byte[] bArr = new byte[1024];
            try {
                try {
                    try {
                        FileOutputStream fileOutputStream = new FileOutputStream(temporaryFile);
                        while (true) {
                            int i = xZInputStream.read(bArr, 0, bArr.length);
                            if (i == -1) {
                                break;
                            }
                            fileOutputStream.write(bArr, 0, i);
                        }
                        fileOutputStream.flush();
                        fileOutputStream.close();
                        Os.chmod(temporaryFile.getAbsolutePath(), 420);
                        return temporaryFile;
                    } catch (ErrnoException e) {
                        Log.e("WearablePkgInstaller", "Could not set permissions on file ", e);
                        try {
                            xZInputStream.close();
                        } catch (IOException e2) {
                            Log.e("WearablePkgInstaller", "Failed to close the file from FD ", e2);
                        }
                        return null;
                    }
                } catch (IOException e3) {
                    Log.e("WearablePkgInstaller", "Reading from Asset FD or writing to temp file failed ", e3);
                    try {
                        xZInputStream.close();
                    } catch (IOException e4) {
                        Log.e("WearablePkgInstaller", "Failed to close the file from FD ", e4);
                    }
                    return null;
                }
            } finally {
                try {
                    xZInputStream.close();
                } catch (IOException e5) {
                    Log.e("WearablePkgInstaller", "Failed to close the file from FD ", e5);
                }
            }
        } catch (IOException e6) {
            Log.e("WearablePkgInstaller", "Compression was set to " + str2 + ", but could not decode ", e6);
            return null;
        }
    }

    public static String getSanitizedPackageName(Uri uri) {
        String encodedSchemeSpecificPart = uri.getEncodedSchemeSpecificPart();
        if (encodedSchemeSpecificPart != null) {
            return encodedSchemeSpecificPart.replaceAll("^/+", "");
        }
        return encodedSchemeSpecificPart;
    }
}
