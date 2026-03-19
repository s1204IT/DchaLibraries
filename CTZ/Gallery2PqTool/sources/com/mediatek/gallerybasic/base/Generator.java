package com.mediatek.gallerybasic.base;

import android.os.StatFs;
import com.mediatek.gallerybasic.util.Log;
import java.io.File;

public abstract class Generator {
    protected static final int DEFAULT_THUMBNAIL_SIZE = 224;
    private static final String[] DYNAMIC_CACHE_FILE_POSTFIX = {".dthumb", ".mp4", ".gif"};
    protected static final int GENERATE_CANCEL = 1;
    protected static final int GENERATE_ERROR = 2;
    protected static final int GENERATE_OK = 0;
    private static final int MIN_STORAGE_SPACE = 3145728;
    public static final int STATE_GENERATED = 2;
    public static final int STATE_GENERATED_FAIL = 3;
    public static final int STATE_GENERATING = 1;
    public static final int STATE_NEED_GENERATE = 0;
    private static final String SUFFIX_TMP = ".tmp";
    private static final String TAG = "MtkGallery2/AbstractVideoGenerator";
    public static final int VTYPE_SHARE = 1;
    public static final int VTYPE_SHARE_GIF = 2;
    public static final int VTYPE_THUMB = 0;
    public int[] videoState = {0, 0, 0};
    public String[] videoPath = {null, null, null};

    protected abstract int generate(MediaData mediaData, int i, String str);

    public abstract void onCancelRequested(MediaData mediaData, int i);

    protected int generateAsync(MediaData mediaData, int i) {
        if (i != 0) {
            throw new UnsupportedOperationException("now only support syncGenerate for thumbnail play");
        }
        GeneratorCoordinator.requestThumbnail(this, mediaData);
        return 0;
    }

    protected boolean shouldCancel() {
        return Thread.currentThread().isInterrupted();
    }

    protected boolean needGenerating(MediaData mediaData, int i) {
        this.videoPath[i] = getVideoThumbnailPathFromOriginalFilePath(mediaData, i);
        if (i != 0 || !new File(this.videoPath[i]).exists()) {
            return true;
        }
        return false;
    }

    public String generateVideo(MediaData mediaData, int i) {
        if (mediaData.filePath == null || mediaData.width == 0 || mediaData.height == 0) {
            return null;
        }
        if (!needGenerating(mediaData, i)) {
            return this.videoPath[i];
        }
        if (i == 0) {
            generateAsync(mediaData, 0);
            return null;
        }
        if (generateAndWait(mediaData, i) == 0) {
            return this.videoPath[i];
        }
        return null;
    }

    public void prepareToRegenerate(MediaData mediaData) {
        this.videoState[0] = 0;
        this.videoState[1] = 0;
        this.videoState[2] = 0;
    }

    public int generateAndWait(MediaData mediaData, int i) {
        boolean zRenameTo;
        String strSubstring = mediaData.filePath.substring(0, mediaData.filePath.lastIndexOf(47));
        if (!new File(strSubstring).exists()) {
            Log.e(TAG, "media file folder: " + strSubstring + " is not invalid! folder deleted or sdcard unmounted?");
            return 2;
        }
        if (!isStorageSafeForGenerating(strSubstring)) {
            return 2;
        }
        String str = this.videoPath[i];
        File file = new File(str.substring(0, str.lastIndexOf(47)));
        if (!file.exists() && !file.mkdir()) {
            Log.e(TAG, "exception when creating cache container!");
            return 2;
        }
        File file2 = new File(this.videoPath[i] + SUFFIX_TMP);
        int iGenerate = generate(mediaData, i, this.videoPath[i] + SUFFIX_TMP);
        Log.v(TAG, "generate result: " + iGenerate);
        if (iGenerate == 1) {
            return 1;
        }
        if (iGenerate == 0 && file2.exists()) {
            zRenameTo = file2.renameTo(new File(this.videoPath[i]));
        } else {
            zRenameTo = false;
        }
        Log.v(TAG, "recrified generate result: " + iGenerate);
        if (zRenameTo) {
            return 0;
        }
        if (file2.exists()) {
            file2.delete();
        }
        return 2;
    }

    public static boolean isStorageSafeForGenerating(String str) {
        try {
            StatFs statFs = new StatFs(str);
            long availableBlocks = ((long) statFs.getAvailableBlocks()) * ((long) statFs.getBlockSize());
            Log.v(TAG, "storage available in this volume is: " + availableBlocks);
            if (availableBlocks < 3145728) {
                return false;
            }
            return true;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            Log.d(TAG, "may sdcard unmounted (or switched) for this moment");
            return false;
        }
    }

    private static String getVideoThumbnailPathFromOriginalFilePath(MediaData mediaData, int i) {
        StringBuilder sb;
        String str = mediaData.filePath;
        int iLastIndexOf = str.lastIndexOf("/");
        if (iLastIndexOf == -1) {
            sb = new StringBuilder(".dthumb/");
            sb.append(str.substring(iLastIndexOf + 1).hashCode());
            sb.append(mediaData.dateModifiedInSec);
            sb.append(DYNAMIC_CACHE_FILE_POSTFIX[i]);
        } else {
            int i2 = iLastIndexOf + 1;
            sb = new StringBuilder(str.substring(0, i2));
            sb.append(".dthumb/");
            sb.append(str.substring(i2).hashCode());
            sb.append(mediaData.dateModifiedInSec);
            sb.append(DYNAMIC_CACHE_FILE_POSTFIX[i]);
        }
        return sb.toString();
    }
}
