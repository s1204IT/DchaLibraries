package com.android.gallery3d.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import com.android.gallery3d.filtershow.tools.SaveImage;
import com.mediatek.gallery3d.video.BookmarkEnhance;
import com.mediatek.gallerybasic.util.ExtFieldsUtils;
import com.mediatek.galleryportable.VideoConstantUtils;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

public class SaveVideoFileUtils {
    public static SaveVideoFileInfo getDstMp4FileInfo(String str, ContentResolver contentResolver, Uri uri, File file, boolean z, String str2) {
        SaveVideoFileInfo saveVideoFileInfo = new SaveVideoFileInfo();
        if (z) {
            saveVideoFileInfo.mDirectory = file;
        } else {
            saveVideoFileInfo.mDirectory = getSaveDirectory(contentResolver, uri);
        }
        if (saveVideoFileInfo.mDirectory == null || !saveVideoFileInfo.mDirectory.canWrite()) {
            saveVideoFileInfo.mDirectory = new File(Environment.getExternalStorageDirectory(), "download");
            saveVideoFileInfo.mFolderName = str2;
        } else {
            saveVideoFileInfo.mFolderName = saveVideoFileInfo.mDirectory.getName();
        }
        saveVideoFileInfo.mFileName = new SimpleDateFormat(str).format(new Date(System.currentTimeMillis()));
        saveVideoFileInfo.mFile = new File(saveVideoFileInfo.mDirectory, saveVideoFileInfo.mFileName + ".mp4");
        return saveVideoFileInfo;
    }

    private static void querySource(ContentResolver contentResolver, Uri uri, String[] strArr, SaveImage.ContentResolverQueryCallback contentResolverQueryCallback) throws Throwable {
        Cursor cursorQuery;
        Cursor cursorQuery2 = null;
        try {
            cursorQuery = contentResolver.query(uri, strArr, null, null, null);
            if (cursorQuery == null) {
                try {
                    String strDecode = Uri.decode(uri.toString());
                    if (strDecode == null) {
                        if (cursorQuery != null) {
                            cursorQuery.close();
                            return;
                        }
                        return;
                    } else {
                        cursorQuery2 = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, strArr, "_data LIKE '%" + strDecode.replaceAll("'", "''").replaceFirst("file:///", "") + "'", null, null);
                    }
                } catch (Exception e) {
                    if (cursorQuery != null) {
                        cursorQuery.close();
                        return;
                    }
                    return;
                } catch (Throwable th) {
                    th = th;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    throw th;
                }
            } else {
                cursorQuery2 = cursorQuery;
            }
            if (cursorQuery2 != null && cursorQuery2.moveToNext()) {
                contentResolverQueryCallback.onCursorResult(cursorQuery2);
            }
            if (cursorQuery2 != null) {
                cursorQuery2.close();
            }
        } catch (Exception e2) {
            cursorQuery = cursorQuery2;
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = cursorQuery2;
        }
    }

    private static File getSaveDirectory(ContentResolver contentResolver, Uri uri) throws Throwable {
        final File[] fileArr = new File[1];
        querySource(contentResolver, uri, new String[]{BookmarkEnhance.COLUMN_DATA}, new SaveImage.ContentResolverQueryCallback() {
            @Override
            public void onCursorResult(Cursor cursor) {
                fileArr[0] = new File(cursor.getString(0)).getParentFile();
            }
        });
        return fileArr[0];
    }

    public static Uri insertContent(SaveVideoFileInfo saveVideoFileInfo, ContentResolver contentResolver, Uri uri) throws Throwable {
        long jCurrentTimeMillis = System.currentTimeMillis();
        long j = jCurrentTimeMillis / 1000;
        final ContentValues contentValues = new ContentValues(13);
        contentValues.put("title", saveVideoFileInfo.mFileName);
        contentValues.put(BookmarkEnhance.COLUMN_TITLE, saveVideoFileInfo.mFile.getName());
        contentValues.put(BookmarkEnhance.COLUMN_MEDIA_TYPE, "video/mp4");
        contentValues.put("datetaken", Long.valueOf(jCurrentTimeMillis));
        contentValues.put("date_modified", Long.valueOf(j));
        contentValues.put(BookmarkEnhance.COLUMN_ADD_DATE, Long.valueOf(j));
        contentValues.put(BookmarkEnhance.COLUMN_DATA, saveVideoFileInfo.mFile.getAbsolutePath());
        contentValues.put("_size", Long.valueOf(saveVideoFileInfo.mFile.length()));
        contentValues.put(SchemaSymbols.ATTVAL_DURATION, Integer.valueOf(retriveVideoDurationMs(saveVideoFileInfo.mFile.getPath())));
        querySource(contentResolver, uri, new String[]{"datetaken", "latitude", "longitude", "resolution", VideoConstantUtils.get(ExtFieldsUtils.VIDEO_ROTATION_FIELD)}, new SaveImage.ContentResolverQueryCallback() {
            @Override
            public void onCursorResult(Cursor cursor) {
                long j2 = cursor.getLong(0);
                if (j2 > 0) {
                    contentValues.put("datetaken", Long.valueOf(j2));
                }
                double d = cursor.getDouble(1);
                double d2 = cursor.getDouble(2);
                if (d != 0.0d || d2 != 0.0d) {
                    contentValues.put("latitude", Double.valueOf(d));
                    contentValues.put("longitude", Double.valueOf(d2));
                }
                contentValues.put("resolution", cursor.getString(3));
                contentValues.put(VideoConstantUtils.get(ExtFieldsUtils.VIDEO_ROTATION_FIELD), cursor.getString(4));
            }
        });
        return contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
    }

    public static int retriveVideoDurationMs(String str) throws IOException {
        int i;
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        try {
            mediaMetadataRetriever.setDataSource(str);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String strExtractMetadata = mediaMetadataRetriever.extractMetadata(9);
        if (strExtractMetadata != null) {
            i = Integer.parseInt(strExtractMetadata);
        } else {
            i = 0;
        }
        mediaMetadataRetriever.release();
        return i;
    }
}
