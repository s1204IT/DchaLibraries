package com.android.wallpaperbackup;

import android.app.AppGlobals;
import android.app.WallpaperManager;
import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FullBackupDataOutput;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Environment;
import android.os.FileUtils;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Slog;
import android.util.Xml;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;

public class WallpaperBackupAgent extends BackupAgent {
    private File mLockWallpaperFile;
    private boolean mQuotaExceeded;
    private File mQuotaFile;
    private File mWallpaperFile;
    private File mWallpaperInfo;
    private WallpaperManager mWm;

    @Override
    public void onCreate() {
        File userSystemDirectory = Environment.getUserSystemDirectory(0);
        this.mWallpaperInfo = new File(userSystemDirectory, "wallpaper_info.xml");
        this.mWallpaperFile = new File(userSystemDirectory, "wallpaper_orig");
        this.mLockWallpaperFile = new File(userSystemDirectory, "wallpaper_lock_orig");
        this.mWm = (WallpaperManager) getSystemService("wallpaper");
        this.mQuotaFile = new File(getFilesDir(), "quota");
        this.mQuotaExceeded = this.mQuotaFile.exists();
    }

    @Override
    public void onFullBackup(FullBackupDataOutput fullBackupDataOutput) throws IOException {
        File filesDir = getFilesDir();
        File file = new File(filesDir, "wallpaper-info-stage");
        File file2 = new File(filesDir, "wallpaper-stage");
        File file3 = new File(filesDir, "wallpaper-lock-stage");
        File file4 = new File(filesDir, "empty");
        try {
            try {
                new FileOutputStream(file4).close();
                fullBackupFile(file4, fullBackupDataOutput);
                SharedPreferences sharedPreferences = getSharedPreferences("wbprefs.xml", 0);
                int i = sharedPreferences.getInt("system_gen", -1);
                int i2 = sharedPreferences.getInt("lock_gen", -1);
                int wallpaperIdForUser = this.mWm.getWallpaperIdForUser(1, 0);
                int wallpaperIdForUser2 = this.mWm.getWallpaperIdForUser(2, 0);
                boolean z = wallpaperIdForUser != i;
                boolean z2 = wallpaperIdForUser2 != i2;
                boolean zIsWallpaperBackupEligible = this.mWm.isWallpaperBackupEligible(1);
                boolean zIsWallpaperBackupEligible2 = this.mWm.isWallpaperBackupEligible(2);
                ParcelFileDescriptor wallpaperFile = this.mWm.getWallpaperFile(2, 0);
                boolean z3 = wallpaperFile != null;
                IoUtils.closeQuietly(wallpaperFile);
                if (this.mWallpaperInfo.exists()) {
                    if (z || z2 || !file.exists()) {
                        FileUtils.copyFileOrThrow(this.mWallpaperInfo, file);
                    }
                    fullBackupFile(file, fullBackupDataOutput);
                }
                if (zIsWallpaperBackupEligible && this.mWallpaperFile.exists()) {
                    if (z || !file2.exists()) {
                        FileUtils.copyFileOrThrow(this.mWallpaperFile, file2);
                    }
                    fullBackupFile(file2, fullBackupDataOutput);
                    sharedPreferences.edit().putInt("system_gen", wallpaperIdForUser).apply();
                }
                if (zIsWallpaperBackupEligible2 && z3 && this.mLockWallpaperFile.exists() && !this.mQuotaExceeded) {
                    if (z2 || !file3.exists()) {
                        FileUtils.copyFileOrThrow(this.mLockWallpaperFile, file3);
                    }
                    fullBackupFile(file3, fullBackupDataOutput);
                    sharedPreferences.edit().putInt("lock_gen", wallpaperIdForUser2).apply();
                }
            } catch (Exception e) {
                Slog.e("WallpaperBackup", "Unable to back up wallpaper", e);
            }
        } finally {
            this.mQuotaFile.delete();
        }
    }

    @Override
    public void onQuotaExceeded(long j, long j2) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(this.mQuotaFile);
            try {
                fileOutputStream.write(0);
            } finally {
                $closeResource(null, fileOutputStream);
            }
        } catch (Exception e) {
            Slog.w("WallpaperBackup", "Unable to record quota-exceeded: " + e.getMessage());
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    @Override
    public void onRestoreFinished() {
        File filesDir = getFilesDir();
        File file = new File(filesDir, "wallpaper-info-stage");
        File file2 = new File(filesDir, "wallpaper-stage");
        File file3 = new File(filesDir, "wallpaper-lock-stage");
        try {
            try {
                restoreFromStage(file2, file, "wp", (file3.exists() ? 0 : 2) | 1);
                restoreFromStage(file3, file, "kwp", 2);
                ComponentName wallpaperComponent = parseWallpaperComponent(file, "wp");
                if (servicePackageExists(wallpaperComponent)) {
                    this.mWm.setWallpaperComponent(wallpaperComponent, 0);
                    if (!file3.exists()) {
                        this.mWm.clear(2);
                    }
                }
            } catch (Exception e) {
                Slog.e("WallpaperBackup", "Unable to restore wallpaper: " + e.getMessage());
            }
        } finally {
            file.delete();
            file2.delete();
            file3.delete();
            getSharedPreferences("wbprefs.xml", 0).edit().putInt("system_gen", -1).putInt("lock_gen", -1).commit();
        }
    }

    private void restoreFromStage(File file, File file2, String str, int i) throws Exception {
        Rect cropHint;
        if (file.exists() && (cropHint = parseCropHint(file2, str)) != null) {
            Slog.i("WallpaperBackup", "Got restored wallpaper; applying which=" + i);
            FileInputStream fileInputStream = new FileInputStream(file);
            Throwable th = null;
            try {
                WallpaperManager wallpaperManager = this.mWm;
                if (cropHint.isEmpty()) {
                    cropHint = null;
                }
                wallpaperManager.setStream(fileInputStream, cropHint, true, i);
            } finally {
                $closeResource(th, fileInputStream);
            }
        }
    }

    private Rect parseCropHint(File file, String str) throws Throwable {
        Throwable th;
        Throwable th2;
        int next;
        Rect rect = new Rect();
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            try {
                XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                xmlPullParserNewPullParser.setInput(fileInputStream, StandardCharsets.UTF_8.name());
                do {
                    next = xmlPullParserNewPullParser.next();
                    if (next == 2 && str.equals(xmlPullParserNewPullParser.getName())) {
                        rect.left = getAttributeInt(xmlPullParserNewPullParser, "cropLeft", 0);
                        rect.top = getAttributeInt(xmlPullParserNewPullParser, "cropTop", 0);
                        rect.right = getAttributeInt(xmlPullParserNewPullParser, "cropRight", 0);
                        rect.bottom = getAttributeInt(xmlPullParserNewPullParser, "cropBottom", 0);
                    }
                } while (next != 1);
                $closeResource(null, fileInputStream);
                return rect;
            } catch (Throwable th3) {
                try {
                    throw th3;
                } catch (Throwable th4) {
                    th = th3;
                    th2 = th4;
                    $closeResource(th, fileInputStream);
                    throw th2;
                }
            }
        } catch (Exception e) {
            Slog.w("WallpaperBackup", "Failed to parse restored crop: " + e.getMessage());
            return null;
        }
    }

    private ComponentName parseWallpaperComponent(File file, String str) throws Throwable {
        Throwable th;
        ComponentName componentNameUnflattenFromString;
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            try {
                XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                xmlPullParserNewPullParser.setInput(fileInputStream, StandardCharsets.UTF_8.name());
                while (true) {
                    int next = xmlPullParserNewPullParser.next();
                    if (next == 2 && str.equals(xmlPullParserNewPullParser.getName())) {
                        break;
                    }
                    if (next == 1) {
                        componentNameUnflattenFromString = null;
                        break;
                    }
                }
                $closeResource(null, fileInputStream);
                return componentNameUnflattenFromString;
            } catch (Throwable th2) {
                th = th2;
                th = null;
                $closeResource(th, fileInputStream);
                throw th;
            }
        } catch (Exception e) {
            Slog.w("WallpaperBackup", "Failed to parse restored component: " + e.getMessage());
            return null;
        }
    }

    private int getAttributeInt(XmlPullParser xmlPullParser, String str, int i) {
        String attributeValue = xmlPullParser.getAttributeValue(null, str);
        return attributeValue == null ? i : Integer.parseInt(attributeValue);
    }

    private boolean servicePackageExists(ComponentName componentName) {
        if (componentName != null) {
            try {
                return AppGlobals.getPackageManager().getPackageInfo(componentName.getPackageName(), 0, 0) != null;
            } catch (RemoteException e) {
                Slog.e("WallpaperBackup", "Unable to contact package manager");
            }
        }
        return false;
    }

    @Override
    public void onBackup(ParcelFileDescriptor parcelFileDescriptor, BackupDataOutput backupDataOutput, ParcelFileDescriptor parcelFileDescriptor2) throws IOException {
    }

    @Override
    public void onRestore(BackupDataInput backupDataInput, int i, ParcelFileDescriptor parcelFileDescriptor) throws IOException {
    }
}
