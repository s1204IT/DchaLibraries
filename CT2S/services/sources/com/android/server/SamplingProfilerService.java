package com.android.server;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Binder;
import android.os.DropBoxManager;
import android.os.FileObserver;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Slog;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;

public class SamplingProfilerService extends Binder {
    private static final boolean LOCAL_LOGV = false;
    public static final String SNAPSHOT_DIR = "/data/snapshots";
    private static final String TAG = "SamplingProfilerService";
    private final Context mContext;
    private FileObserver snapshotObserver;

    public SamplingProfilerService(Context context) {
        this.mContext = context;
        registerSettingObserver(context);
        startWorking(context);
    }

    private void startWorking(Context context) {
        final DropBoxManager dropbox = (DropBoxManager) context.getSystemService("dropbox");
        File[] snapshotFiles = new File(SNAPSHOT_DIR).listFiles();
        for (int i = 0; snapshotFiles != null && i < snapshotFiles.length; i++) {
            handleSnapshotFile(snapshotFiles[i], dropbox);
        }
        this.snapshotObserver = new FileObserver(SNAPSHOT_DIR, 4) {
            @Override
            public void onEvent(int event, String path) {
                SamplingProfilerService.this.handleSnapshotFile(new File(SamplingProfilerService.SNAPSHOT_DIR, path), dropbox);
            }
        };
        this.snapshotObserver.startWatching();
    }

    private void handleSnapshotFile(File file, DropBoxManager dropbox) {
        try {
            dropbox.addFile(TAG, file, 0);
        } catch (IOException e) {
            Slog.e(TAG, "Can't add " + file.getPath() + " to dropbox", e);
        } finally {
            file.delete();
        }
    }

    private void registerSettingObserver(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        contentResolver.registerContentObserver(Settings.Global.getUriFor("sampling_profiler_ms"), LOCAL_LOGV, new SamplingProfilerSettingsObserver(contentResolver));
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", TAG);
        pw.println("SamplingProfilerService:");
        pw.println("Watching directory: /data/snapshots");
    }

    private class SamplingProfilerSettingsObserver extends ContentObserver {
        private ContentResolver mContentResolver;

        public SamplingProfilerSettingsObserver(ContentResolver contentResolver) {
            super(null);
            this.mContentResolver = contentResolver;
            onChange(SamplingProfilerService.LOCAL_LOGV);
        }

        @Override
        public void onChange(boolean selfChange) {
            Integer samplingProfilerMs = Integer.valueOf(Settings.Global.getInt(this.mContentResolver, "sampling_profiler_ms", 0));
            SystemProperties.set("persist.sys.profiler_ms", samplingProfilerMs.toString());
        }
    }
}
