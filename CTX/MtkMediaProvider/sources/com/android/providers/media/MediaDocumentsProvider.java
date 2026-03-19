package com.android.providers.media;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.provider.MediaStore;
import android.provider.MetadataReader;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import libcore.io.IoUtils;

public class MediaDocumentsProvider extends DocumentsProvider {
    static final boolean $assertionsDisabled = false;
    private static final String[] DEFAULT_ROOT_PROJECTION = {"root_id", "flags", "icon", "title", "document_id", "mime_types"};
    private static final String[] DEFAULT_DOCUMENT_PROJECTION = {"document_id", "mime_type", "_display_name", "last_modified", "flags", "_size", "_data", "is_drm", "drm_method"};
    private static final String IMAGE_MIME_TYPES = joinNewline("image/*");
    private static final String VIDEO_MIME_TYPES = joinNewline("video/*");
    private static final String AUDIO_MIME_TYPES = joinNewline("audio/*", "application/ogg", "application/x-flac");
    private static boolean sReturnedImagesEmpty = false;
    private static boolean sReturnedVideosEmpty = false;
    private static boolean sReturnedAudioEmpty = false;
    private static final Map<String, String> IMAGE_COLUMN_MAP = new HashMap();
    private static final Map<String, String> VIDEO_COLUMN_MAP = new HashMap();
    private static final Map<String, String> AUDIO_COLUMN_MAP = new HashMap();

    private interface AlbumQuery {
        public static final String[] PROJECTION = {"_id", "album"};
    }

    private interface ArtistQuery {
        public static final String[] PROJECTION = {"_id", "artist"};
    }

    private interface ImageOrientationQuery {
        public static final String[] PROJECTION = {"orientation"};
    }

    private interface ImageQuery {
        public static final String[] PROJECTION = {"_id", "_display_name", "mime_type", "_size", "date_modified", "_data", "is_drm", "drm_method"};
    }

    private interface ImageThumbnailQuery {
        public static final String[] PROJECTION = {"_data"};
    }

    private interface ImagesBucketQuery {
        public static final String[] PROJECTION = {"bucket_id", "bucket_display_name", "date_modified"};
    }

    private interface ImagesBucketThumbnailQuery {
        public static final String[] PROJECTION = {"_id", "bucket_id", "date_modified", "is_drm", "drm_method"};
    }

    private interface SongQuery {
        public static final String[] PROJECTION = {"_id", "title", "mime_type", "_size", "date_modified", "_data", "is_drm", "drm_method"};
    }

    private interface VideoQuery {
        public static final String[] PROJECTION = {"_id", "_display_name", "mime_type", "_size", "date_modified", "_data", "is_drm", "drm_method"};
    }

    private interface VideoThumbnailQuery {
        public static final String[] PROJECTION = {"_data"};
    }

    private interface VideosBucketQuery {
        public static final String[] PROJECTION = {"bucket_id", "bucket_display_name", "date_modified"};
    }

    private interface VideosBucketThumbnailQuery {
        public static final String[] PROJECTION = {"_id", "bucket_id", "date_modified", "is_drm", "drm_method"};
    }

    static {
        IMAGE_COLUMN_MAP.put("width", "ImageWidth");
        IMAGE_COLUMN_MAP.put("height", "ImageLength");
        IMAGE_COLUMN_MAP.put("datetaken", "DateTime");
        IMAGE_COLUMN_MAP.put("latitude", "GPSLatitude");
        IMAGE_COLUMN_MAP.put("longitude", "GPSLongitude");
        VIDEO_COLUMN_MAP.put("duration", "android.media.metadata.DURATION");
        VIDEO_COLUMN_MAP.put("height", "ImageLength");
        VIDEO_COLUMN_MAP.put("width", "ImageWidth");
        VIDEO_COLUMN_MAP.put("latitude", "android.media.metadata.video:latitude");
        VIDEO_COLUMN_MAP.put("longitude", "android.media.metadata.video:longitude");
        VIDEO_COLUMN_MAP.put("datetaken", "android.media.metadata.DATE");
        AUDIO_COLUMN_MAP.put("artist", "android.media.metadata.ARTIST");
        AUDIO_COLUMN_MAP.put("composer", "android.media.metadata.COMPOSER");
        AUDIO_COLUMN_MAP.put("album", "android.media.metadata.ALBUM");
        AUDIO_COLUMN_MAP.put("year", "android.media.metadata.YEAR");
        AUDIO_COLUMN_MAP.put("duration", "android.media.metadata.DURATION");
    }

    private static String joinNewline(String... strArr) {
        return TextUtils.join("\n", strArr);
    }

    private void copyNotificationUri(MatrixCursor matrixCursor, Cursor cursor) {
        matrixCursor.setNotificationUri(getContext().getContentResolver(), cursor.getNotificationUri());
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    private void enforceShellRestrictions() {
        if (UserHandle.getCallingAppId() == 2000 && ((UserManager) getContext().getSystemService(UserManager.class)).hasUserRestriction("no_usb_file_transfer")) {
            throw new SecurityException("Shell user cannot access files for user " + UserHandle.myUserId());
        }
    }

    protected int enforceReadPermissionInner(Uri uri, String str, IBinder iBinder) throws SecurityException {
        enforceShellRestrictions();
        return super.enforceReadPermissionInner(uri, str, iBinder);
    }

    protected int enforceWritePermissionInner(Uri uri, String str, IBinder iBinder) throws SecurityException {
        enforceShellRestrictions();
        return super.enforceWritePermissionInner(uri, str, iBinder);
    }

    private static void notifyRootsChanged(Context context) {
        context.getContentResolver().notifyChange(DocumentsContract.buildRootsUri("com.android.providers.media.documents"), (ContentObserver) null, false);
    }

    static void onMediaStoreInsert(Context context, String str, int i, long j) {
        if ("external".equals(str)) {
            if (i == 1 && sReturnedImagesEmpty) {
                sReturnedImagesEmpty = false;
                notifyRootsChanged(context);
            } else if (i == 3 && sReturnedVideosEmpty) {
                sReturnedVideosEmpty = false;
                notifyRootsChanged(context);
            } else if (i == 2 && sReturnedAudioEmpty) {
                sReturnedAudioEmpty = false;
                notifyRootsChanged(context);
            }
        }
    }

    static void onMediaStoreDelete(Context context, String str, int i, long j) {
        if ("external".equals(str)) {
            if (i == 1) {
                context.revokeUriPermission(DocumentsContract.buildDocumentUri("com.android.providers.media.documents", getDocIdForIdent("image", j)), -1);
            } else if (i == 3) {
                context.revokeUriPermission(DocumentsContract.buildDocumentUri("com.android.providers.media.documents", getDocIdForIdent("video", j)), -1);
            } else if (i == 2) {
                context.revokeUriPermission(DocumentsContract.buildDocumentUri("com.android.providers.media.documents", getDocIdForIdent("audio", j)), -1);
            }
        }
    }

    private static class Ident {
        public long id;
        public String type;

        private Ident() {
        }
    }

    private static Ident getIdentForDocId(String str) {
        Ident ident = new Ident();
        int iIndexOf = str.indexOf(58);
        if (iIndexOf == -1) {
            ident.type = str;
            ident.id = -1L;
        } else {
            ident.type = str.substring(0, iIndexOf);
            ident.id = Long.parseLong(str.substring(iIndexOf + 1));
        }
        return ident;
    }

    private static String getDocIdForIdent(String str, long j) {
        return str + ":" + j;
    }

    private static String[] resolveRootProjection(String[] strArr) {
        return strArr != null ? strArr : DEFAULT_ROOT_PROJECTION;
    }

    private static String[] resolveDocumentProjection(String[] strArr) {
        return strArr != null ? strArr : DEFAULT_DOCUMENT_PROJECTION;
    }

    private Uri getUriForDocumentId(String str) {
        Ident identForDocId = getIdentForDocId(str);
        if ("image".equals(identForDocId.type) && identForDocId.id != -1) {
            return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, identForDocId.id);
        }
        if ("video".equals(identForDocId.type) && identForDocId.id != -1) {
            return ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, identForDocId.id);
        }
        if ("audio".equals(identForDocId.type) && identForDocId.id != -1) {
            return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, identForDocId.id);
        }
        throw new UnsupportedOperationException("Unsupported document " + str);
    }

    @Override
    public void deleteDocument(String str) throws FileNotFoundException {
        Uri uriForDocumentId = getUriForDocumentId(str);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            getContext().getContentResolver().delete(uriForDocumentId, null, null);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @Override
    public Bundle getDocumentMetadata(String str) throws FileNotFoundException {
        String documentType = getDocumentType(str);
        if (MetadataReader.isSupportedMimeType(documentType)) {
            return getDocumentMetadataFromStream(str, documentType);
        }
        return getDocumentMetadataFromIndex(str);
    }

    private Bundle getDocumentMetadataFromStream(String str, String str2) throws Throwable {
        ParcelFileDescriptor.AutoCloseInputStream autoCloseInputStream;
        ParcelFileDescriptor.AutoCloseInputStream autoCloseInputStream2 = null;
        try {
            autoCloseInputStream = new ParcelFileDescriptor.AutoCloseInputStream(openDocument(str, "r", null));
        } catch (IOException e) {
            autoCloseInputStream = null;
        } catch (Throwable th) {
            th = th;
        }
        try {
            Bundle bundle = new Bundle();
            MetadataReader.getMetadata(bundle, autoCloseInputStream, str2, (String[]) null);
            IoUtils.closeQuietly(autoCloseInputStream);
            return bundle;
        } catch (IOException e2) {
            IoUtils.closeQuietly(autoCloseInputStream);
            return null;
        } catch (Throwable th2) {
            th = th2;
            autoCloseInputStream2 = autoCloseInputStream;
            IoUtils.closeQuietly(autoCloseInputStream2);
            throw th;
        }
    }

    public Bundle getDocumentMetadataFromIndex(String str) throws Throwable {
        byte b;
        Map<String, String> map;
        String str2;
        Uri uri;
        Cursor cursorQuery;
        Ident identForDocId = getIdentForDocId(str);
        String str3 = identForDocId.type;
        int iHashCode = str3.hashCode();
        if (iHashCode != 93166550) {
            if (iHashCode != 100313435) {
                b = (iHashCode == 112202875 && str3.equals("video")) ? (byte) 1 : (byte) -1;
            } else if (str3.equals("image")) {
                b = 0;
            }
        } else if (str3.equals("audio")) {
            b = 2;
        }
        switch (b) {
            case 0:
                map = IMAGE_COLUMN_MAP;
                str2 = "android:documentExif";
                uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                break;
            case 1:
                map = VIDEO_COLUMN_MAP;
                str2 = "android.media.metadata.video";
                uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                break;
            case 2:
                map = AUDIO_COLUMN_MAP;
                str2 = "android.media.metadata.audio";
                uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                break;
            default:
                throw new FileNotFoundException("Metadata request for unsupported file type: " + identForDocId.type);
        }
        Uri uri2 = uri;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        ContentResolver contentResolver = getContext().getContentResolver();
        Set<String> setKeySet = map.keySet();
        try {
            cursorQuery = contentResolver.query(uri2, (String[]) setKeySet.toArray(new String[setKeySet.size()]), "_id=?", new String[]{Long.toString(identForDocId.id)}, null);
            try {
                if (!cursorQuery.moveToFirst()) {
                    throw new FileNotFoundException("Can't find document id: " + str);
                }
                Bundle bundleExtractMetadataFromCursor = extractMetadataFromCursor(cursorQuery, map);
                Bundle bundle = new Bundle();
                bundle.putBundle(str2, bundleExtractMetadataFromCursor);
                bundle.putStringArray("android:documentMetadataType", new String[]{str2});
                IoUtils.closeQuietly(cursorQuery);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                return bundle;
            } catch (Throwable th) {
                th = th;
                IoUtils.closeQuietly(cursorQuery);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
        }
    }

    private static Bundle extractMetadataFromCursor(Cursor cursor, Map<String, String> map) {
        Bundle bundle = new Bundle();
        for (String str : map.keySet()) {
            int columnIndex = cursor.getColumnIndex(str);
            String str2 = map.get(str);
            if ("DateTime".equals(str2)) {
                bundle.putString(str2, DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMM dd, yyyy, hh:mm"), cursor.getLong(columnIndex)).toString());
            } else {
                switch (cursor.getType(columnIndex)) {
                    case 0:
                        Log.d("MediaDocumentsProvider", "Unsupported type, null, for col: " + str2);
                        break;
                    case 1:
                        bundle.putInt(str2, cursor.getInt(columnIndex));
                        break;
                    case 2:
                        bundle.putFloat(str2, cursor.getFloat(columnIndex));
                        break;
                    case 3:
                        bundle.putString(str2, cursor.getString(columnIndex));
                        break;
                    case 4:
                        Log.d("MediaDocumentsProvider", "Unsupported type, blob, for col: " + str2);
                        break;
                    default:
                        throw new RuntimeException("Data type not supported");
                }
            }
        }
        return bundle;
    }

    @Override
    public Cursor queryRoots(String[] strArr) throws FileNotFoundException {
        MatrixCursor matrixCursor = new MatrixCursor(resolveRootProjection(strArr));
        includeImagesRoot(matrixCursor);
        includeVideosRoot(matrixCursor);
        includeAudioRoot(matrixCursor);
        return matrixCursor;
    }

    @Override
    public Cursor queryDocument(String str, String[] strArr) throws Throwable {
        ContentResolver contentResolver = getContext().getContentResolver();
        MatrixCursor matrixCursor = new MatrixCursor(resolveDocumentProjection(strArr));
        Ident identForDocId = getIdentForDocId(str);
        String[] strArr2 = {Long.toString(identForDocId.id)};
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        ?? r9 = 0;
        try {
            if (!"images_root".equals(identForDocId.type)) {
                try {
                    if ("images_bucket".equals(identForDocId.type)) {
                        Cursor cursorQuery = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ImagesBucketQuery.PROJECTION, "bucket_id=?", strArr2, "bucket_id, date_modified DESC");
                        copyNotificationUri(matrixCursor, cursorQuery);
                        boolean zMoveToFirst = cursorQuery.moveToFirst();
                        str = cursorQuery;
                        if (zMoveToFirst) {
                            includeImagesBucket(matrixCursor, cursorQuery);
                            str = cursorQuery;
                        }
                    } else if ("image".equals(identForDocId.type)) {
                        Cursor cursorQuery2 = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ImageQuery.PROJECTION, "_id=?", strArr2, null);
                        copyNotificationUri(matrixCursor, cursorQuery2);
                        boolean zMoveToFirst2 = cursorQuery2.moveToFirst();
                        str = cursorQuery2;
                        if (zMoveToFirst2) {
                            includeImage(matrixCursor, cursorQuery2);
                            str = cursorQuery2;
                        }
                    } else if ("videos_root".equals(identForDocId.type)) {
                        includeVideosRootDocument(matrixCursor);
                    } else if ("videos_bucket".equals(identForDocId.type)) {
                        Cursor cursorQuery3 = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, VideosBucketQuery.PROJECTION, "bucket_id=?", strArr2, "bucket_id, date_modified DESC");
                        copyNotificationUri(matrixCursor, cursorQuery3);
                        boolean zMoveToFirst3 = cursorQuery3.moveToFirst();
                        str = cursorQuery3;
                        if (zMoveToFirst3) {
                            includeVideosBucket(matrixCursor, cursorQuery3);
                            str = cursorQuery3;
                        }
                    } else if ("video".equals(identForDocId.type)) {
                        Cursor cursorQuery4 = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, VideoQuery.PROJECTION, "_id=?", strArr2, null);
                        copyNotificationUri(matrixCursor, cursorQuery4);
                        boolean zMoveToFirst4 = cursorQuery4.moveToFirst();
                        str = cursorQuery4;
                        if (zMoveToFirst4) {
                            includeVideo(matrixCursor, cursorQuery4);
                            str = cursorQuery4;
                        }
                    } else if ("audio_root".equals(identForDocId.type)) {
                        includeAudioRootDocument(matrixCursor);
                    } else if ("artist".equals(identForDocId.type)) {
                        Cursor cursorQuery5 = contentResolver.query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, ArtistQuery.PROJECTION, "_id=?", strArr2, null);
                        copyNotificationUri(matrixCursor, cursorQuery5);
                        boolean zMoveToFirst5 = cursorQuery5.moveToFirst();
                        str = cursorQuery5;
                        if (zMoveToFirst5) {
                            includeArtist(matrixCursor, cursorQuery5);
                            str = cursorQuery5;
                        }
                    } else if ("album".equals(identForDocId.type)) {
                        Cursor cursorQuery6 = contentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, AlbumQuery.PROJECTION, "_id=?", strArr2, null);
                        copyNotificationUri(matrixCursor, cursorQuery6);
                        boolean zMoveToFirst6 = cursorQuery6.moveToFirst();
                        str = cursorQuery6;
                        if (zMoveToFirst6) {
                            includeAlbum(matrixCursor, cursorQuery6);
                            str = cursorQuery6;
                        }
                    } else {
                        if (!"audio".equals(identForDocId.type)) {
                            throw new UnsupportedOperationException("Unsupported document " + ((String) str));
                        }
                        Cursor cursorQuery7 = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, SongQuery.PROJECTION, "_id=?", strArr2, null);
                        copyNotificationUri(matrixCursor, cursorQuery7);
                        boolean zMoveToFirst7 = cursorQuery7.moveToFirst();
                        str = cursorQuery7;
                        if (zMoveToFirst7) {
                            includeAudio(matrixCursor, cursorQuery7);
                            str = cursorQuery7;
                        }
                    }
                    IoUtils.closeQuietly((AutoCloseable) str);
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    return matrixCursor;
                } catch (Throwable th) {
                    r9 = str;
                    th = th;
                    IoUtils.closeQuietly((AutoCloseable) r9);
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    throw th;
                }
            }
            includeImagesRootDocument(matrixCursor);
            str = 0;
            IoUtils.closeQuietly((AutoCloseable) str);
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            return matrixCursor;
        } catch (Throwable th2) {
            th = th2;
        }
    }

    @Override
    public Cursor queryChildDocuments(String str, String[] strArr, String str2) throws Throwable {
        Cursor cursorQuery;
        ContentResolver contentResolver = getContext().getContentResolver();
        MatrixCursor matrixCursor = new MatrixCursor(resolveDocumentProjection(strArr));
        Ident identForDocId = getIdentForDocId(str);
        String[] strArr2 = {Long.toString(identForDocId.id)};
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        ?? r9 = 0;
        try {
            long j = Long.MIN_VALUE;
            try {
                if ("images_root".equals(identForDocId.type)) {
                    cursorQuery = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ImagesBucketQuery.PROJECTION, null, null, "bucket_id, date_modified DESC");
                    copyNotificationUri(matrixCursor, cursorQuery);
                    while (cursorQuery.moveToNext()) {
                        long j2 = cursorQuery.getLong(0);
                        if (j != j2) {
                            includeImagesBucket(matrixCursor, cursorQuery);
                            j = j2;
                        }
                    }
                } else if ("images_bucket".equals(identForDocId.type)) {
                    cursorQuery = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ImageQuery.PROJECTION, "bucket_id=?", strArr2, null);
                    copyNotificationUri(matrixCursor, cursorQuery);
                    while (cursorQuery.moveToNext()) {
                        includeImage(matrixCursor, cursorQuery);
                    }
                } else if ("videos_root".equals(identForDocId.type)) {
                    cursorQuery = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, VideosBucketQuery.PROJECTION, null, null, "bucket_id, date_modified DESC");
                    copyNotificationUri(matrixCursor, cursorQuery);
                    while (cursorQuery.moveToNext()) {
                        long j3 = cursorQuery.getLong(0);
                        if (j != j3) {
                            includeVideosBucket(matrixCursor, cursorQuery);
                            j = j3;
                        }
                    }
                } else if ("videos_bucket".equals(identForDocId.type)) {
                    cursorQuery = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, VideoQuery.PROJECTION, "bucket_id=?", strArr2, null);
                    copyNotificationUri(matrixCursor, cursorQuery);
                    while (cursorQuery.moveToNext()) {
                        includeVideo(matrixCursor, cursorQuery);
                    }
                } else if ("audio_root".equals(identForDocId.type)) {
                    cursorQuery = contentResolver.query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, ArtistQuery.PROJECTION, null, null, null);
                    copyNotificationUri(matrixCursor, cursorQuery);
                    while (cursorQuery.moveToNext()) {
                        includeArtist(matrixCursor, cursorQuery);
                    }
                } else if ("artist".equals(identForDocId.type)) {
                    cursorQuery = contentResolver.query(MediaStore.Audio.Artists.Albums.getContentUri("external", identForDocId.id), AlbumQuery.PROJECTION, null, null, null);
                    copyNotificationUri(matrixCursor, cursorQuery);
                    while (cursorQuery.moveToNext()) {
                        includeAlbum(matrixCursor, cursorQuery);
                    }
                } else if ("album".equals(identForDocId.type)) {
                    cursorQuery = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, SongQuery.PROJECTION, "album_id=?", strArr2, null);
                    copyNotificationUri(matrixCursor, cursorQuery);
                    while (cursorQuery.moveToNext()) {
                        includeAudio(matrixCursor, cursorQuery);
                    }
                } else {
                    throw new UnsupportedOperationException("Unsupported document " + str);
                }
                IoUtils.closeQuietly(cursorQuery);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                return matrixCursor;
            } catch (Throwable th) {
                th = th;
                r9 = str;
                IoUtils.closeQuietly((AutoCloseable) r9);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    @Override
    public Cursor queryRecentDocuments(String str, String[] strArr) throws Throwable {
        Cursor cursorQuery;
        ContentResolver contentResolver = getContext().getContentResolver();
        MatrixCursor matrixCursor = new MatrixCursor(resolveDocumentProjection(strArr));
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        ?? r13 = 0;
        try {
            try {
                if ("images_root".equals(str)) {
                    cursorQuery = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ImageQuery.PROJECTION, null, null, "date_modified DESC");
                    copyNotificationUri(matrixCursor, cursorQuery);
                    while (cursorQuery.moveToNext() && matrixCursor.getCount() < 64) {
                        includeImage(matrixCursor, cursorQuery);
                    }
                } else if ("videos_root".equals(str)) {
                    cursorQuery = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, VideoQuery.PROJECTION, null, null, "date_modified DESC");
                    copyNotificationUri(matrixCursor, cursorQuery);
                    while (cursorQuery.moveToNext() && matrixCursor.getCount() < 64) {
                        includeVideo(matrixCursor, cursorQuery);
                    }
                } else {
                    throw new UnsupportedOperationException("Unsupported root " + str);
                }
                IoUtils.closeQuietly(cursorQuery);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                return matrixCursor;
            } catch (Throwable th) {
                r13 = str;
                th = th;
                IoUtils.closeQuietly((AutoCloseable) r13);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    @Override
    public Cursor querySearchDocuments(String str, String str2, String[] strArr) throws Throwable {
        Cursor cursorQuery;
        ContentResolver contentResolver = getContext().getContentResolver();
        MatrixCursor matrixCursor = new MatrixCursor(resolveDocumentProjection(strArr));
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        String[] strArr2 = {"%" + str2 + "%"};
        ?? r12 = 0;
        try {
            try {
                if ("images_root".equals(str)) {
                    cursorQuery = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ImageQuery.PROJECTION, "_display_name LIKE ?", strArr2, "date_modified DESC");
                    copyNotificationUri(matrixCursor, cursorQuery);
                    while (cursorQuery.moveToNext()) {
                        includeImage(matrixCursor, cursorQuery);
                    }
                } else if ("videos_root".equals(str)) {
                    cursorQuery = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, VideoQuery.PROJECTION, "_display_name LIKE ?", strArr2, "date_modified DESC");
                    copyNotificationUri(matrixCursor, cursorQuery);
                    while (cursorQuery.moveToNext()) {
                        includeVideo(matrixCursor, cursorQuery);
                    }
                } else if ("audio_root".equals(str)) {
                    cursorQuery = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, SongQuery.PROJECTION, "title LIKE ?", strArr2, "date_modified DESC");
                    copyNotificationUri(matrixCursor, cursorQuery);
                    while (cursorQuery.moveToNext()) {
                        includeAudio(matrixCursor, cursorQuery);
                    }
                } else {
                    throw new UnsupportedOperationException("Unsupported root " + str);
                }
                IoUtils.closeQuietly(cursorQuery);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                return matrixCursor;
            } catch (Throwable th) {
                r12 = str;
                th = th;
                IoUtils.closeQuietly((AutoCloseable) r12);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    @Override
    public ParcelFileDescriptor openDocument(String str, String str2, CancellationSignal cancellationSignal) throws FileNotFoundException {
        Uri uriForDocumentId = getUriForDocumentId(str);
        if (!"r".equals(str2)) {
            throw new IllegalArgumentException("Media is read-only");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return getContext().getContentResolver().openFileDescriptor(uriForDocumentId, str2);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String str, Point point, CancellationSignal cancellationSignal) throws FileNotFoundException {
        Ident identForDocId = getIdentForDocId(str);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if ("images_bucket".equals(identForDocId.type)) {
                return openOrCreateImageThumbnailCleared(getImageForBucketCleared(identForDocId.id), cancellationSignal);
            }
            if ("image".equals(identForDocId.type)) {
                return openOrCreateImageThumbnailCleared(identForDocId.id, cancellationSignal);
            }
            if ("videos_bucket".equals(identForDocId.type)) {
                return openOrCreateVideoThumbnailCleared(getVideoForBucketCleared(identForDocId.id), cancellationSignal);
            }
            if ("video".equals(identForDocId.type)) {
                return openOrCreateVideoThumbnailCleared(identForDocId.id, cancellationSignal);
            }
            throw new UnsupportedOperationException("Unsupported document " + str);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private boolean isEmpty(Uri uri) throws Throwable {
        boolean z;
        ContentResolver contentResolver = getContext().getContentResolver();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        Cursor cursor = null;
        try {
            Cursor cursorQuery = contentResolver.query(uri, new String[]{"_id"}, null, null, null);
            if (cursorQuery != null) {
                try {
                    z = cursorQuery.getCount() == 0;
                } catch (Throwable th) {
                    cursor = cursorQuery;
                    th = th;
                    IoUtils.closeQuietly(cursor);
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    throw th;
                }
            }
            IoUtils.closeQuietly(cursorQuery);
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            return z;
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private void includeImagesRoot(MatrixCursor matrixCursor) {
        int i;
        if (isEmpty(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)) {
            i = 65550;
            sReturnedImagesEmpty = true;
        } else {
            i = 14;
        }
        MatrixCursor.RowBuilder rowBuilderNewRow = matrixCursor.newRow();
        rowBuilderNewRow.add("root_id", "images_root");
        rowBuilderNewRow.add("flags", Integer.valueOf(i));
        rowBuilderNewRow.add("title", getContext().getString(R.string.root_images));
        rowBuilderNewRow.add("document_id", "images_root");
        rowBuilderNewRow.add("mime_types", IMAGE_MIME_TYPES);
    }

    private void includeVideosRoot(MatrixCursor matrixCursor) {
        int i;
        if (isEmpty(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)) {
            i = 65550;
            sReturnedVideosEmpty = true;
        } else {
            i = 14;
        }
        MatrixCursor.RowBuilder rowBuilderNewRow = matrixCursor.newRow();
        rowBuilderNewRow.add("root_id", "videos_root");
        rowBuilderNewRow.add("flags", Integer.valueOf(i));
        rowBuilderNewRow.add("title", getContext().getString(R.string.root_videos));
        rowBuilderNewRow.add("document_id", "videos_root");
        rowBuilderNewRow.add("mime_types", VIDEO_MIME_TYPES);
    }

    private void includeAudioRoot(MatrixCursor matrixCursor) {
        int i;
        if (isEmpty(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)) {
            i = 65546;
            sReturnedAudioEmpty = true;
        } else {
            i = 10;
        }
        MatrixCursor.RowBuilder rowBuilderNewRow = matrixCursor.newRow();
        rowBuilderNewRow.add("root_id", "audio_root");
        rowBuilderNewRow.add("flags", Integer.valueOf(i));
        rowBuilderNewRow.add("title", getContext().getString(R.string.root_audio));
        rowBuilderNewRow.add("document_id", "audio_root");
        rowBuilderNewRow.add("mime_types", AUDIO_MIME_TYPES);
    }

    private void includeImagesRootDocument(MatrixCursor matrixCursor) {
        MatrixCursor.RowBuilder rowBuilderNewRow = matrixCursor.newRow();
        rowBuilderNewRow.add("document_id", "images_root");
        rowBuilderNewRow.add("_display_name", getContext().getString(R.string.root_images));
        rowBuilderNewRow.add("flags", 48);
        rowBuilderNewRow.add("mime_type", "vnd.android.document/directory");
    }

    private void includeVideosRootDocument(MatrixCursor matrixCursor) {
        MatrixCursor.RowBuilder rowBuilderNewRow = matrixCursor.newRow();
        rowBuilderNewRow.add("document_id", "videos_root");
        rowBuilderNewRow.add("_display_name", getContext().getString(R.string.root_videos));
        rowBuilderNewRow.add("flags", 48);
        rowBuilderNewRow.add("mime_type", "vnd.android.document/directory");
    }

    private void includeAudioRootDocument(MatrixCursor matrixCursor) {
        MatrixCursor.RowBuilder rowBuilderNewRow = matrixCursor.newRow();
        rowBuilderNewRow.add("document_id", "audio_root");
        rowBuilderNewRow.add("_display_name", getContext().getString(R.string.root_audio));
        rowBuilderNewRow.add("mime_type", "vnd.android.document/directory");
    }

    private void includeImagesBucket(MatrixCursor matrixCursor, Cursor cursor) {
        String docIdForIdent = getDocIdForIdent("images_bucket", cursor.getLong(0));
        MatrixCursor.RowBuilder rowBuilderNewRow = matrixCursor.newRow();
        rowBuilderNewRow.add("document_id", docIdForIdent);
        rowBuilderNewRow.add("_display_name", cursor.getString(1));
        rowBuilderNewRow.add("mime_type", "vnd.android.document/directory");
        rowBuilderNewRow.add("last_modified", Long.valueOf(cursor.getLong(2) * 1000));
        rowBuilderNewRow.add("flags", 49);
    }

    private void includeImage(MatrixCursor matrixCursor, Cursor cursor) {
        String docIdForIdent = getDocIdForIdent("image", cursor.getLong(0));
        MatrixCursor.RowBuilder rowBuilderNewRow = matrixCursor.newRow();
        rowBuilderNewRow.add("document_id", docIdForIdent);
        rowBuilderNewRow.add("_display_name", cursor.getString(1));
        rowBuilderNewRow.add("_size", Long.valueOf(cursor.getLong(3)));
        rowBuilderNewRow.add("mime_type", cursor.getString(2));
        rowBuilderNewRow.add("last_modified", Long.valueOf(cursor.getLong(4) * 1000));
        rowBuilderNewRow.add("_data", cursor.getString(5));
        rowBuilderNewRow.add("is_drm", Integer.valueOf(cursor.getInt(6)));
        rowBuilderNewRow.add("drm_method", Integer.valueOf(cursor.getInt(7)));
        rowBuilderNewRow.add("flags", 131077);
    }

    private void includeVideosBucket(MatrixCursor matrixCursor, Cursor cursor) {
        String docIdForIdent = getDocIdForIdent("videos_bucket", cursor.getLong(0));
        MatrixCursor.RowBuilder rowBuilderNewRow = matrixCursor.newRow();
        rowBuilderNewRow.add("document_id", docIdForIdent);
        rowBuilderNewRow.add("_display_name", cursor.getString(1));
        rowBuilderNewRow.add("mime_type", "vnd.android.document/directory");
        rowBuilderNewRow.add("last_modified", Long.valueOf(cursor.getLong(2) * 1000));
        rowBuilderNewRow.add("flags", 49);
    }

    private void includeVideo(MatrixCursor matrixCursor, Cursor cursor) {
        String docIdForIdent = getDocIdForIdent("video", cursor.getLong(0));
        MatrixCursor.RowBuilder rowBuilderNewRow = matrixCursor.newRow();
        rowBuilderNewRow.add("document_id", docIdForIdent);
        rowBuilderNewRow.add("_display_name", cursor.getString(1));
        rowBuilderNewRow.add("_size", Long.valueOf(cursor.getLong(3)));
        rowBuilderNewRow.add("mime_type", cursor.getString(2));
        rowBuilderNewRow.add("last_modified", Long.valueOf(cursor.getLong(4) * 1000));
        rowBuilderNewRow.add("_data", cursor.getString(5));
        rowBuilderNewRow.add("is_drm", Integer.valueOf(cursor.getInt(6)));
        rowBuilderNewRow.add("drm_method", Integer.valueOf(cursor.getInt(7)));
        rowBuilderNewRow.add("flags", 131077);
    }

    private void includeArtist(MatrixCursor matrixCursor, Cursor cursor) {
        String docIdForIdent = getDocIdForIdent("artist", cursor.getLong(0));
        MatrixCursor.RowBuilder rowBuilderNewRow = matrixCursor.newRow();
        rowBuilderNewRow.add("document_id", docIdForIdent);
        rowBuilderNewRow.add("_display_name", cleanUpMediaDisplayName(cursor.getString(1)));
        rowBuilderNewRow.add("mime_type", "vnd.android.document/directory");
    }

    private void includeAlbum(MatrixCursor matrixCursor, Cursor cursor) {
        String docIdForIdent = getDocIdForIdent("album", cursor.getLong(0));
        MatrixCursor.RowBuilder rowBuilderNewRow = matrixCursor.newRow();
        rowBuilderNewRow.add("document_id", docIdForIdent);
        rowBuilderNewRow.add("_display_name", cleanUpMediaDisplayName(cursor.getString(1)));
        rowBuilderNewRow.add("mime_type", "vnd.android.document/directory");
    }

    private void includeAudio(MatrixCursor matrixCursor, Cursor cursor) {
        String docIdForIdent = getDocIdForIdent("audio", cursor.getLong(0));
        MatrixCursor.RowBuilder rowBuilderNewRow = matrixCursor.newRow();
        rowBuilderNewRow.add("document_id", docIdForIdent);
        Log.d("MediaDocumentsProvider", "[includeAudio] add to COLUMN_DISPLAY_NAME: " + cursor.getString(1));
        rowBuilderNewRow.add("_display_name", cursor.getString(1));
        rowBuilderNewRow.add("_size", Long.valueOf(cursor.getLong(3)));
        rowBuilderNewRow.add("mime_type", cursor.getString(2));
        rowBuilderNewRow.add("last_modified", Long.valueOf(cursor.getLong(4) * 1000));
        rowBuilderNewRow.add("_data", cursor.getString(5));
        rowBuilderNewRow.add("is_drm", Integer.valueOf(cursor.getInt(6)));
        rowBuilderNewRow.add("drm_method", Integer.valueOf(cursor.getInt(7)));
        rowBuilderNewRow.add("flags", 131076);
    }

    private long getImageForBucketCleared(long j) throws Throwable {
        ContentResolver contentResolver = getContext().getContentResolver();
        Cursor cursor = null;
        try {
            Cursor cursorQuery = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ImagesBucketThumbnailQuery.PROJECTION, "bucket_id=" + j, null, "date_modified DESC");
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.getCount() > 0) {
                        cursorQuery.moveToPosition(-1);
                        while (cursorQuery.moveToNext()) {
                            boolean z = cursorQuery.getInt(3) > 0;
                            if (z && (!z || cursorQuery.getInt(4) != 4)) {
                            }
                            long j2 = cursorQuery.getLong(0);
                            IoUtils.closeQuietly(cursorQuery);
                            return j2;
                        }
                    }
                } catch (Throwable th) {
                    th = th;
                    cursor = cursorQuery;
                    IoUtils.closeQuietly(cursor);
                    throw th;
                }
            }
            IoUtils.closeQuietly(cursorQuery);
            throw new FileNotFoundException("No video found for bucket");
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private ParcelFileDescriptor openImageThumbnailCleared(long j, CancellationSignal cancellationSignal) throws Throwable {
        ContentResolver contentResolver = getContext().getContentResolver();
        Cursor cursor = null;
        try {
            Cursor cursorQuery = contentResolver.query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, ImageThumbnailQuery.PROJECTION, "image_id=" + j, null, null, cancellationSignal);
            try {
                if (cursorQuery.moveToFirst()) {
                    ParcelFileDescriptor parcelFileDescriptorOpen = ParcelFileDescriptor.open(new File(cursorQuery.getString(0)), 268435456);
                    IoUtils.closeQuietly(cursorQuery);
                    return parcelFileDescriptorOpen;
                }
                IoUtils.closeQuietly(cursorQuery);
                return null;
            } catch (Throwable th) {
                th = th;
                cursor = cursorQuery;
                IoUtils.closeQuietly(cursor);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private AssetFileDescriptor openOrCreateImageThumbnailCleared(long j, CancellationSignal cancellationSignal) throws Throwable {
        ParcelFileDescriptor parcelFileDescriptorOpenFileDescriptor;
        ContentResolver contentResolver = getContext().getContentResolver();
        Bundle bundle = null;
        try {
            parcelFileDescriptorOpenFileDescriptor = openImageThumbnailCleared(j, cancellationSignal);
        } catch (FileNotFoundException e) {
            Log.d("MediaDocumentsProvider", "image thumbnail file may be delete, so generate new one.");
            parcelFileDescriptorOpenFileDescriptor = null;
        }
        if (parcelFileDescriptorOpenFileDescriptor == null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            MediaStore.Images.Thumbnails.getThumbnail(contentResolver, j, 1, options);
            parcelFileDescriptorOpenFileDescriptor = openImageThumbnailCleared(j, cancellationSignal);
        }
        if (parcelFileDescriptorOpenFileDescriptor == null) {
            parcelFileDescriptorOpenFileDescriptor = contentResolver.openFileDescriptor(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, j), "r", cancellationSignal);
        }
        ParcelFileDescriptor parcelFileDescriptor = parcelFileDescriptorOpenFileDescriptor;
        int iQueryOrientationForImage = queryOrientationForImage(j, cancellationSignal);
        if (iQueryOrientationForImage != 0) {
            bundle = new Bundle(1);
            bundle.putInt("android.provider.extra.ORIENTATION", iQueryOrientationForImage);
        }
        return new AssetFileDescriptor(parcelFileDescriptor, 0L, -1L, bundle);
    }

    private long getVideoForBucketCleared(long j) throws Throwable {
        ContentResolver contentResolver = getContext().getContentResolver();
        Cursor cursor = null;
        try {
            Cursor cursorQuery = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, VideosBucketThumbnailQuery.PROJECTION, "bucket_id=" + j, null, "date_modified DESC");
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.getCount() > 0) {
                        cursorQuery.moveToPosition(-1);
                        while (cursorQuery.moveToNext()) {
                            boolean z = cursorQuery.getInt(3) > 0;
                            if (z && (!z || cursorQuery.getInt(4) != 4)) {
                            }
                            long j2 = cursorQuery.getLong(0);
                            IoUtils.closeQuietly(cursorQuery);
                            return j2;
                        }
                    }
                } catch (Throwable th) {
                    th = th;
                    cursor = cursorQuery;
                    IoUtils.closeQuietly(cursor);
                    throw th;
                }
            }
            IoUtils.closeQuietly(cursorQuery);
            throw new FileNotFoundException("No video found for bucket");
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private AssetFileDescriptor openVideoThumbnailCleared(long j, CancellationSignal cancellationSignal) throws Throwable {
        Cursor cursorQuery;
        ContentResolver contentResolver = getContext().getContentResolver();
        Cursor cursor = null;
        try {
            cursorQuery = contentResolver.query(MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI, VideoThumbnailQuery.PROJECTION, "video_id=" + j, null, null, cancellationSignal);
        } catch (Throwable th) {
            th = th;
        }
        try {
            if (cursorQuery.moveToFirst()) {
                AssetFileDescriptor assetFileDescriptor = new AssetFileDescriptor(ParcelFileDescriptor.open(new File(cursorQuery.getString(0)), 268435456), 0L, -1L);
                IoUtils.closeQuietly(cursorQuery);
                return assetFileDescriptor;
            }
            IoUtils.closeQuietly(cursorQuery);
            return null;
        } catch (Throwable th2) {
            th = th2;
            cursor = cursorQuery;
            IoUtils.closeQuietly(cursor);
            throw th;
        }
    }

    private AssetFileDescriptor openOrCreateVideoThumbnailCleared(long j, CancellationSignal cancellationSignal) throws Throwable {
        AssetFileDescriptor assetFileDescriptorOpenVideoThumbnailCleared;
        ContentResolver contentResolver = getContext().getContentResolver();
        try {
            assetFileDescriptorOpenVideoThumbnailCleared = openVideoThumbnailCleared(j, cancellationSignal);
        } catch (FileNotFoundException e) {
            Log.d("MediaDocumentsProvider", "video thumbnail file may be delete, so generate new one.");
            assetFileDescriptorOpenVideoThumbnailCleared = null;
        }
        if (assetFileDescriptorOpenVideoThumbnailCleared == null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            MediaStore.Video.Thumbnails.getThumbnail(contentResolver, j, 1, options);
            return openVideoThumbnailCleared(j, cancellationSignal);
        }
        return assetFileDescriptorOpenVideoThumbnailCleared;
    }

    private int queryOrientationForImage(long j, CancellationSignal cancellationSignal) throws Throwable {
        Cursor cursorQuery;
        ContentResolver contentResolver = getContext().getContentResolver();
        try {
            cursorQuery = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ImageOrientationQuery.PROJECTION, "_id=" + j, null, null, cancellationSignal);
            try {
                if (cursorQuery.moveToFirst()) {
                    int i = cursorQuery.getInt(0);
                    IoUtils.closeQuietly(cursorQuery);
                    return i;
                }
                Log.w("MediaDocumentsProvider", "Missing orientation data for " + j);
                IoUtils.closeQuietly(cursorQuery);
                return 0;
            } catch (Throwable th) {
                th = th;
                IoUtils.closeQuietly(cursorQuery);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
        }
    }

    private String cleanUpMediaDisplayName(String str) {
        if (!"<unknown>".equals(str)) {
            return str;
        }
        return getContext().getResources().getString(android.R.string.unknownName);
    }
}
