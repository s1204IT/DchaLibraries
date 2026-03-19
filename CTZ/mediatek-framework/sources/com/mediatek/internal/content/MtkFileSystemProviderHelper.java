package com.mediatek.internal.content;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.drm.DrmManagerClient;
import android.media.MediaFile;
import android.net.Uri;
import android.os.SystemProperties;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;
import com.mediatek.media.MtkMediaStore;
import java.io.File;

public class MtkFileSystemProviderHelper {
    private static final boolean DEBUG = false;
    private static final boolean LOG_INOTIFY = false;
    private static final String MIMETYPE_JPEG = "image/jpeg";
    private static final String MIMETYPE_JPG = "image/jpg";
    private static final String MIMETYPE_OCTET_STREAM = "application/octet-stream";
    private static final String TAG = "FileSystemProvider";
    private Context mContext;
    private String[] mDefaultProjection;
    private static final Uri BASE_URI = new Uri.Builder().scheme("content").authority("com.android.externalstorage.documents").build();
    private static final String[] DEFAULT_DOCUMENT_PROJECTION = {"document_id", "mime_type", "_display_name", "last_modified", "flags", "_size", "_data", "is_drm", MtkMediaStore.MediaColumns.DRM_METHOD};

    public MtkFileSystemProviderHelper(Context context) {
        this.mContext = null;
        this.mContext = context;
    }

    public static boolean isMtkDrmApp() {
        return SystemProperties.getBoolean("ro.vendor.mtk_oma_drm_support", false);
    }

    public void supportDRM(File file, MatrixCursor.RowBuilder rowBuilder, String str, String str2, File file2) throws Throwable {
        Cursor cursorQuery;
        String name = file.getName();
        if (!isMtkDrmApp() || file.isDirectory()) {
            file2 = file;
        } else {
            int iLastIndexOf = name.lastIndexOf(46);
            String lowerCase = iLastIndexOf >= 0 ? name.substring(iLastIndexOf + 1).toLowerCase() : null;
            if (lowerCase != null && lowerCase.equalsIgnoreCase("dcf")) {
                Uri contentUri = MediaStore.Files.getContentUri("external");
                String[] strArr = {"is_drm", MtkMediaStore.MediaColumns.DRM_METHOD, "mime_type"};
                try {
                    if (file2 != null) {
                        cursorQuery = this.mContext.getContentResolver().query(contentUri, strArr, "_data = ?", new String[]{file2.getAbsolutePath()}, null);
                        if (cursorQuery != null) {
                            try {
                                try {
                                    if (cursorQuery.moveToFirst()) {
                                        int i = cursorQuery.getInt(cursorQuery.getColumnIndex("is_drm"));
                                        int i2 = cursorQuery.getInt(cursorQuery.getColumnIndex(MtkMediaStore.MediaColumns.DRM_METHOD));
                                        String string = cursorQuery.getString(cursorQuery.getColumnIndex("mime_type"));
                                        try {
                                            rowBuilder.add("is_drm", Integer.valueOf(i));
                                            rowBuilder.add(MtkMediaStore.MediaColumns.DRM_METHOD, Integer.valueOf(i2));
                                            str2 = string;
                                        } catch (IllegalStateException e) {
                                            str2 = string;
                                            if (cursorQuery != null) {
                                            }
                                            rowBuilder.add("mime_type", str2);
                                            rowBuilder.add("_data", file2.getAbsolutePath());
                                        }
                                    }
                                } catch (Throwable th) {
                                    th = th;
                                    if (cursorQuery != null) {
                                        cursorQuery.close();
                                    }
                                    throw th;
                                }
                            } catch (IllegalStateException e2) {
                            }
                        }
                    } else {
                        Log.d(TAG, "VisibleFile is null");
                        cursorQuery = null;
                    }
                } catch (IllegalStateException e3) {
                    cursorQuery = null;
                } catch (Throwable th2) {
                    th = th2;
                    cursorQuery = null;
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
        }
        rowBuilder.add("mime_type", str2);
        rowBuilder.add("_data", file2.getAbsolutePath());
    }

    public String getTypeForNameMtk(File file, String str) {
        int iLastIndexOf = str.lastIndexOf(46);
        if (iLastIndexOf >= 0) {
            String lowerCase = str.substring(iLastIndexOf + 1).toLowerCase();
            if (lowerCase.equalsIgnoreCase("dcf")) {
                return getTypeForDrmFile(file);
            }
            String mimeTypeFromExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(lowerCase);
            if (mimeTypeFromExtension != null) {
                return mimeTypeFromExtension;
            }
        }
        String mimeTypeForFile = MediaFile.getMimeTypeForFile(str);
        if (mimeTypeForFile != null) {
            return mimeTypeForFile;
        }
        return MIMETYPE_OCTET_STREAM;
    }

    private String getTypeForDrmFile(File file) {
        DrmManagerClient drmManagerClient = new DrmManagerClient(this.mContext);
        String string = file.toString();
        if (drmManagerClient.canHandle(string, (String) null)) {
            return drmManagerClient.getOriginalMimeType(string);
        }
        return MIMETYPE_OCTET_STREAM;
    }

    public String[] getDefaultProjection() {
        return DEFAULT_DOCUMENT_PROJECTION;
    }
}
