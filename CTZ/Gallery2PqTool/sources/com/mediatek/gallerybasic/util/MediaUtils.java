package com.mediatek.gallerybasic.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import com.mediatek.galleryportable.TraceHelper;
import java.util.Arrays;
import java.util.List;

public class MediaUtils {
    private static final String TAG = "MtkGallery2/MediaUtils";
    private static List<String> sImageColumns;
    private static List<String> sVideoColumns;

    public static List<String> getImageColumns(Context context) {
        if (sImageColumns == null) {
            TraceHelper.beginSection(">>>>Gallery2-getColumns-image");
            sImageColumns = getColumns(context, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
            TraceHelper.endSection();
        }
        return sImageColumns;
    }

    public static List<String> getVideoColumns(Context context) {
        if (sVideoColumns == null) {
            TraceHelper.beginSection(">>>>Gallery2-getColumns-video");
            sVideoColumns = getColumns(context, MediaStore.Video.Media.INTERNAL_CONTENT_URI);
            TraceHelper.endSection();
        }
        return sVideoColumns;
    }

    private static List<String> getColumns(Context context, Uri uri) {
        Log.d(TAG, "<getColumns> baseUri = " + uri);
        Cursor cursorQuery = context.getContentResolver().query(uri.buildUpon().appendQueryParameter("limit", "0,1").build(), null, null, null, null);
        List<String> listAsList = null;
        if (cursorQuery != null) {
            String[] columnNames = cursorQuery.getColumnNames();
            if (columnNames != null) {
                listAsList = Arrays.asList(columnNames);
            }
            cursorQuery.close();
        }
        return listAsList;
    }
}
