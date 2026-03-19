package com.mediatek.gallerybasic.util;

import android.graphics.Bitmap;
import android.os.Environment;
import com.mediatek.galleryportable.SystemPropertyUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class DebugUtils {
    private static final String BITAMP_DUMP_FOLDER = "/.GalleryIssue/";
    public static final String BITMAP_DUMP_PATH;
    public static final boolean DEBUG_HIGH_QUALITY_SCREENAIL;
    public static final boolean DEBUG_PLAY_ENGINE;
    public static final boolean DEBUG_PLAY_RENDER;
    public static final boolean DEBUG_POSITION_CONTROLLER;
    public static final boolean DEBUG_RENDER;
    public static final boolean DEBUG_THUMBNAIL_PLAY_ENGINE;
    private static final int DEFAULT_COMPRESS_QUALITY = 100;
    public static final boolean DUMP;
    private static final String TAG = "MtkGallery2/DebugUtils";
    public static final boolean TILE;

    static {
        DUMP = SystemPropertyUtils.getInt("Gallery_DUMP", 0) == 1;
        TILE = SystemPropertyUtils.getInt("Gallery_TILE", 0) == 1;
        DEBUG_PLAY_ENGINE = SystemPropertyUtils.getInt("Gallery_DEBUG_PLAY_ENGINE", 0) == 1;
        DEBUG_THUMBNAIL_PLAY_ENGINE = SystemPropertyUtils.getInt("Gallery_DEBUG_ConstrainedEngine", 0) == 1;
        DEBUG_PLAY_RENDER = SystemPropertyUtils.getInt("Gallery_DEBUG_PLAY_RENDER", 0) == 1;
        DEBUG_POSITION_CONTROLLER = SystemPropertyUtils.getInt("Gallery_DEBUG_PC", 0) == 1;
        DEBUG_HIGH_QUALITY_SCREENAIL = SystemPropertyUtils.getInt("Gallery_DEBUG_HQS", 0) == 1;
        DEBUG_RENDER = SystemPropertyUtils.getInt("Gallery_DEBUG_RENDER", 0) == 1;
        BITMAP_DUMP_PATH = Environment.getExternalStorageDirectory().toString() + BITAMP_DUMP_FOLDER;
    }

    public static void dumpBitmap(Bitmap bitmap, String str) throws Throwable {
        FileOutputStream fileOutputStream;
        String str2 = str + ".png";
        File file = new File(BITMAP_DUMP_PATH);
        if (!file.exists()) {
            Log.d(TAG, "<dumpBitmap> create  galleryIssueFilePath");
            file.mkdir();
        }
        File file2 = new File(BITMAP_DUMP_PATH, str2);
        FileOutputStream fileOutputStream2 = null;
        try {
            try {
                try {
                    fileOutputStream = new FileOutputStream(file2);
                } catch (IOException e) {
                    e = e;
                }
            } catch (Throwable th) {
                th = th;
            }
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, DEFAULT_COMPRESS_QUALITY, fileOutputStream);
                fileOutputStream.close();
            } catch (IOException e2) {
                e = e2;
                fileOutputStream2 = fileOutputStream;
                e.printStackTrace();
                Log.d(TAG, "<dumpBitmap> IOException", e.getCause());
                if (fileOutputStream2 != null) {
                    fileOutputStream2.close();
                }
            } catch (Throwable th2) {
                th = th2;
                fileOutputStream2 = fileOutputStream;
                if (fileOutputStream2 != null) {
                    try {
                        fileOutputStream2.close();
                    } catch (IOException e3) {
                        e3.printStackTrace();
                        Log.d(TAG, "<dumpBitmap> close FileOutputStream", e3.getCause());
                    }
                }
                throw th;
            }
        } catch (IOException e4) {
            e4.printStackTrace();
            Log.d(TAG, "<dumpBitmap> close FileOutputStream", e4.getCause());
        }
    }
}
