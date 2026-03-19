package com.android.server.wm;

import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.GraphicBuffer;
import android.graphics.Rect;
import android.util.Slog;
import com.android.server.wm.nano.WindowManagerProtos;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

class TaskSnapshotLoader {
    private static final String TAG = "WindowManager";
    private final TaskSnapshotPersister mPersister;

    TaskSnapshotLoader(TaskSnapshotPersister taskSnapshotPersister) {
        this.mPersister = taskSnapshotPersister;
    }

    ActivityManager.TaskSnapshot loadTask(int i, int i2, boolean z) {
        File bitmapFile;
        File protoFile = this.mPersister.getProtoFile(i, i2);
        if (z) {
            bitmapFile = this.mPersister.getReducedResolutionBitmapFile(i, i2);
        } else {
            bitmapFile = this.mPersister.getBitmapFile(i, i2);
        }
        if (bitmapFile == null || !protoFile.exists() || !bitmapFile.exists()) {
            return null;
        }
        try {
            WindowManagerProtos.TaskSnapshotProto from = WindowManagerProtos.TaskSnapshotProto.parseFrom(Files.readAllBytes(protoFile.toPath()));
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.HARDWARE;
            Bitmap bitmapDecodeFile = BitmapFactory.decodeFile(bitmapFile.getPath(), options);
            if (bitmapDecodeFile == null) {
                Slog.w("WindowManager", "Failed to load bitmap: " + bitmapFile.getPath());
                return null;
            }
            GraphicBuffer graphicBufferCreateGraphicBufferHandle = bitmapDecodeFile.createGraphicBufferHandle();
            if (graphicBufferCreateGraphicBufferHandle == null) {
                Slog.w("WindowManager", "Failed to retrieve gralloc buffer for bitmap: " + bitmapFile.getPath());
                return null;
            }
            return new ActivityManager.TaskSnapshot(graphicBufferCreateGraphicBufferHandle, from.orientation, new Rect(from.insetLeft, from.insetTop, from.insetRight, from.insetBottom), z, z ? TaskSnapshotPersister.REDUCED_SCALE : 1.0f, from.isRealSnapshot, from.windowingMode, from.systemUiVisibility, from.isTranslucent);
        } catch (IOException e) {
            Slog.w("WindowManager", "Unable to load task snapshot data for taskId=" + i);
            return null;
        }
    }
}
