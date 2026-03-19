package com.android.packageinstaller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import java.io.File;
import java.io.IOException;

public class TemporaryFileManager extends BroadcastReceiver {
    private static final String LOG_TAG = TemporaryFileManager.class.getSimpleName();

    public static File getStagedFile(Context context) throws IOException {
        return File.createTempFile("package", ".apk", context.getNoBackupFilesDir());
    }

    public static File getInstallStateFile(Context context) {
        return new File(context.getNoBackupFilesDir(), "install_results.xml");
    }

    public static File getUninstallStateFile(Context context) {
        return new File(context.getNoBackupFilesDir(), "uninstall_results.xml");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        long jCurrentTimeMillis = System.currentTimeMillis() - SystemClock.elapsedRealtime();
        File[] fileArrListFiles = context.getNoBackupFilesDir().listFiles();
        if (fileArrListFiles == null) {
            return;
        }
        for (File file : fileArrListFiles) {
            if (jCurrentTimeMillis > file.lastModified()) {
                if (!file.delete()) {
                    Log.w(LOG_TAG, "Could not delete " + file.getName() + " onBoot");
                }
            } else {
                Log.w(LOG_TAG, file.getName() + " was created before onBoot broadcast was received");
            }
        }
    }
}
