package com.android.gallery3d.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import com.android.gallery3d.filtershow.tools.SaveImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SaveVideoFileUtils {
    public static SaveVideoFileInfo getDstMp4FileInfo(String fileNameFormat, ContentResolver contentResolver, Uri uri, String defaultFolderName) {
        SaveVideoFileInfo dstFileInfo = new SaveVideoFileInfo();
        dstFileInfo.mDirectory = getSaveDirectory(contentResolver, uri);
        if (dstFileInfo.mDirectory == null || !dstFileInfo.mDirectory.canWrite()) {
            dstFileInfo.mDirectory = new File(Environment.getExternalStorageDirectory(), "download");
            dstFileInfo.mFolderName = defaultFolderName;
        } else {
            dstFileInfo.mFolderName = dstFileInfo.mDirectory.getName();
        }
        dstFileInfo.mFileName = new SimpleDateFormat(fileNameFormat).format((Date) new java.sql.Date(System.currentTimeMillis()));
        dstFileInfo.mFile = new File(dstFileInfo.mDirectory, dstFileInfo.mFileName + ".mp4");
        return dstFileInfo;
    }

    private static void querySource(ContentResolver contentResolver, Uri uri, String[] projection, SaveImage.ContentResolverQueryCallback callback) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToNext()) {
                callback.onCursorResult(cursor);
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            if (cursor != null) {
                cursor.close();
            }
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
            throw th;
        }
    }

    private static File getSaveDirectory(ContentResolver contentResolver, Uri uri) {
        final File[] dir = new File[1];
        querySource(contentResolver, uri, new String[]{"_data"}, new SaveImage.ContentResolverQueryCallback() {
            @Override
            public void onCursorResult(Cursor cursor) {
                dir[0] = new File(cursor.getString(0)).getParentFile();
            }
        });
        return dir[0];
    }

    public static Uri insertContent(SaveVideoFileInfo mDstFileInfo, ContentResolver contentResolver, Uri uri) {
        long nowInMs = System.currentTimeMillis();
        long nowInSec = nowInMs / 1000;
        final ContentValues values = new ContentValues(13);
        values.put("title", mDstFileInfo.mFileName);
        values.put("_display_name", mDstFileInfo.mFile.getName());
        values.put("mime_type", "video/mp4");
        values.put("datetaken", Long.valueOf(nowInMs));
        values.put("date_modified", Long.valueOf(nowInSec));
        values.put("date_added", Long.valueOf(nowInSec));
        values.put("_data", mDstFileInfo.mFile.getAbsolutePath());
        values.put("_size", Long.valueOf(mDstFileInfo.mFile.length()));
        int durationMs = retriveVideoDurationMs(mDstFileInfo.mFile.getPath());
        values.put("duration", Integer.valueOf(durationMs));
        String[] projection = {"datetaken", "latitude", "longitude", "resolution"};
        querySource(contentResolver, uri, projection, new SaveImage.ContentResolverQueryCallback() {
            @Override
            public void onCursorResult(Cursor cursor) {
                long timeTaken = cursor.getLong(0);
                if (timeTaken > 0) {
                    values.put("datetaken", Long.valueOf(timeTaken));
                }
                double latitude = cursor.getDouble(1);
                double longitude = cursor.getDouble(2);
                if (latitude != 0.0d || longitude != 0.0d) {
                    values.put("latitude", Double.valueOf(latitude));
                    values.put("longitude", Double.valueOf(longitude));
                }
                values.put("resolution", cursor.getString(3));
            }
        });
        return contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
    }

    public static int retriveVideoDurationMs(String path) throws IOException {
        int durationMs = 0;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(path);
        String duration = retriever.extractMetadata(9);
        if (duration != null) {
            durationMs = Integer.parseInt(duration);
        }
        retriever.release();
        return durationMs;
    }
}
