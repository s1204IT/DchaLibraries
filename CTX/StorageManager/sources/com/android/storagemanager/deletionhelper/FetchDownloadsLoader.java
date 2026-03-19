package com.android.storagemanager.deletionhelper;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.SystemProperties;
import com.android.storagemanager.utils.AsyncLoader;
import com.android.storagemanager.utils.IconProvider;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class FetchDownloadsLoader extends AsyncLoader<DownloadsResult> {
    private File mDirectory;

    public FetchDownloadsLoader(Context context, File file) {
        super(context);
        this.mDirectory = file;
    }

    @Override
    protected void onDiscardResult(DownloadsResult downloadsResult) {
    }

    @Override
    public DownloadsResult loadInBackground() {
        return collectFiles(this.mDirectory);
    }

    static DownloadsResult collectFiles(File file) {
        return collectFiles(file, new DownloadsResult());
    }

    private static DownloadsResult collectFiles(File file, DownloadsResult downloadsResult) {
        long jCurrentTimeMillis = System.currentTimeMillis() - (((long) SystemProperties.getInt("debug.asm.file_age_limit", 0)) * 86400000);
        File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles != null && fileArrListFiles.length > 0) {
            for (File file2 : fileArrListFiles) {
                if (file2.isDirectory()) {
                    collectFiles(file2, downloadsResult);
                } else if (jCurrentTimeMillis >= file2.lastModified()) {
                    if (file2.lastModified() < downloadsResult.youngestLastModified) {
                        downloadsResult.youngestLastModified = file2.lastModified();
                    }
                    downloadsResult.files.add(file2);
                    downloadsResult.totalSize += file2.length();
                    if (IconProvider.isImageType(file2)) {
                        downloadsResult.thumbnails.put(file2, ThumbnailUtils.createImageThumbnail(file2.getAbsolutePath(), 1));
                    }
                }
            }
        }
        return downloadsResult;
    }

    public static class DownloadsResult {
        public ArrayList<File> files;
        public HashMap<File, Bitmap> thumbnails;
        public long totalSize;
        public long youngestLastModified;

        public DownloadsResult() {
            this(0L, Long.MAX_VALUE, new ArrayList(), new HashMap());
        }

        public DownloadsResult(long j, long j2, ArrayList<File> arrayList, HashMap<File, Bitmap> map) {
            this.totalSize = j;
            this.youngestLastModified = j2;
            this.files = arrayList;
            this.thumbnails = map;
        }
    }
}
