package com.android.camera.session;

import android.content.Context;
import com.android.camera.debug.Log;
import com.android.camera.util.FileUtil;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

public class SessionStorageManagerImpl implements SessionStorageManager {
    private static final int MAX_SESSION_AGE_MILLIS = 86400000;
    private static final Log.Tag TAG = new Log.Tag("SesnStorageMgrImpl");
    private final File mBaseDirectory;
    private final File mDeprecatedBaseDirectory;

    public static SessionStorageManager create(Context context) {
        return new SessionStorageManagerImpl(context.getExternalCacheDir(), context.getExternalFilesDir(null));
    }

    SessionStorageManagerImpl(File baseDirectory, File deprecatedBaseDirectory) {
        this.mBaseDirectory = baseDirectory;
        this.mDeprecatedBaseDirectory = deprecatedBaseDirectory;
    }

    @Override
    public File getSessionDirectory(String subDirectory) throws IOException {
        File sessionDirectory = new File(this.mBaseDirectory, subDirectory);
        if (!sessionDirectory.exists() && !sessionDirectory.mkdirs()) {
            throw new IOException("Could not create session directory: " + sessionDirectory);
        }
        if (!sessionDirectory.isDirectory()) {
            throw new IOException("Session directory is not a directory: " + sessionDirectory);
        }
        cleanUpExpiredSessions(sessionDirectory);
        File deprecatedSessionDirectory = new File(this.mDeprecatedBaseDirectory, subDirectory);
        cleanUpExpiredSessions(deprecatedSessionDirectory);
        return sessionDirectory;
    }

    private void cleanUpExpiredSessions(File baseDirectory) {
        File[] sessionDirs = baseDirectory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        if (sessionDirs != null) {
            long nowInMillis = System.currentTimeMillis();
            for (File sessionDir : sessionDirs) {
                if (sessionDir.lastModified() < nowInMillis - 86400000 && !FileUtil.deleteDirectoryRecursively(sessionDir)) {
                    Log.w(TAG, "Could not clean up " + sessionDir.getAbsolutePath());
                }
            }
        }
    }
}
