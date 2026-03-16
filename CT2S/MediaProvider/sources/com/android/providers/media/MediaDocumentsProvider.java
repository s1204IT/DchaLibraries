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
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.io.FileNotFoundException;
import libcore.io.IoUtils;

public class MediaDocumentsProvider extends DocumentsProvider {
    private static final String[] DEFAULT_ROOT_PROJECTION = {"root_id", "flags", "icon", "title", "document_id", "mime_types"};
    private static final String[] DEFAULT_DOCUMENT_PROJECTION = {"document_id", "mime_type", "_display_name", "last_modified", "flags", "_size"};
    private static final String IMAGE_MIME_TYPES = joinNewline("image/*");
    private static final String VIDEO_MIME_TYPES = joinNewline("video/*");
    private static final String AUDIO_MIME_TYPES = joinNewline("audio/*", "application/ogg", "application/x-flac");
    private static boolean sReturnedImagesEmpty = false;
    private static boolean sReturnedVideosEmpty = false;
    private static boolean sReturnedAudioEmpty = false;

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
        public static final String[] PROJECTION = {"_id", "_display_name", "mime_type", "_size", "date_modified"};
    }

    private interface ImageThumbnailQuery {
        public static final String[] PROJECTION = {"_data"};
    }

    private interface ImagesBucketQuery {
        public static final String[] PROJECTION = {"bucket_id", "bucket_display_name", "date_modified"};
    }

    private interface ImagesBucketThumbnailQuery {
        public static final String[] PROJECTION = {"_id", "bucket_id", "date_modified"};
    }

    private interface SongQuery {
        public static final String[] PROJECTION = {"_id", "title", "mime_type", "_size", "date_modified"};
    }

    private interface VideoQuery {
        public static final String[] PROJECTION = {"_id", "_display_name", "mime_type", "_size", "date_modified"};
    }

    private interface VideoThumbnailQuery {
        public static final String[] PROJECTION = {"_data"};
    }

    private interface VideosBucketQuery {
        public static final String[] PROJECTION = {"bucket_id", "bucket_display_name", "date_modified"};
    }

    private interface VideosBucketThumbnailQuery {
        public static final String[] PROJECTION = {"_id", "bucket_id", "date_modified"};
    }

    private static String joinNewline(String... args) {
        return TextUtils.join("\n", args);
    }

    private void copyNotificationUri(MatrixCursor result, Cursor cursor) {
        result.setNotificationUri(getContext().getContentResolver(), cursor.getNotificationUri());
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    private static void notifyRootsChanged(Context context) {
        context.getContentResolver().notifyChange(DocumentsContract.buildRootsUri("com.android.providers.media.documents"), (ContentObserver) null, false);
    }

    static void onMediaStoreInsert(Context context, String volumeName, int type, long id) {
        if ("external".equals(volumeName)) {
            if (type == 1 && sReturnedImagesEmpty) {
                sReturnedImagesEmpty = false;
                notifyRootsChanged(context);
            } else if (type == 3 && sReturnedVideosEmpty) {
                sReturnedVideosEmpty = false;
                notifyRootsChanged(context);
            } else if (type == 2 && sReturnedAudioEmpty) {
                sReturnedAudioEmpty = false;
                notifyRootsChanged(context);
            }
        }
    }

    static void onMediaStoreDelete(Context context, String volumeName, int type, long id) {
        if ("external".equals(volumeName)) {
            if (type == 1) {
                Uri uri = DocumentsContract.buildDocumentUri("com.android.providers.media.documents", getDocIdForIdent("image", id));
                context.revokeUriPermission(uri, -1);
            } else if (type == 3) {
                Uri uri2 = DocumentsContract.buildDocumentUri("com.android.providers.media.documents", getDocIdForIdent("video", id));
                context.revokeUriPermission(uri2, -1);
            } else if (type == 2) {
                Uri uri3 = DocumentsContract.buildDocumentUri("com.android.providers.media.documents", getDocIdForIdent("audio", id));
                context.revokeUriPermission(uri3, -1);
            }
        }
    }

    private static class Ident {
        public long id;
        public String type;

        private Ident() {
        }
    }

    private static Ident getIdentForDocId(String docId) {
        Ident ident = new Ident();
        int split = docId.indexOf(58);
        if (split == -1) {
            ident.type = docId;
            ident.id = -1L;
        } else {
            ident.type = docId.substring(0, split);
            ident.id = Long.parseLong(docId.substring(split + 1));
        }
        return ident;
    }

    private static String getDocIdForIdent(String type, long id) {
        return type + ":" + id;
    }

    private static String[] resolveRootProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_ROOT_PROJECTION;
    }

    private static String[] resolveDocumentProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION;
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        MatrixCursor result = new MatrixCursor(resolveRootProjection(projection));
        includeImagesRoot(result);
        includeVideosRoot(result);
        includeAudioRoot(result);
        return result;
    }

    @Override
    public Cursor queryDocument(String docId, String[] projection) throws FileNotFoundException {
        ContentResolver resolver = getContext().getContentResolver();
        MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        Ident ident = getIdentForDocId(docId);
        long token = Binder.clearCallingIdentity();
        Cursor cursor = null;
        try {
            if ("images_root".equals(ident.type)) {
                includeImagesRootDocument(result);
            } else if ("images_bucket".equals(ident.type)) {
                cursor = resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ImagesBucketQuery.PROJECTION, "bucket_id=" + ident.id, null, "bucket_id, date_modified DESC");
                copyNotificationUri(result, cursor);
                if (cursor.moveToFirst()) {
                    includeImagesBucket(result, cursor);
                }
            } else if ("image".equals(ident.type)) {
                cursor = resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ImageQuery.PROJECTION, "_id=" + ident.id, null, null);
                copyNotificationUri(result, cursor);
                if (cursor.moveToFirst()) {
                    includeImage(result, cursor);
                }
            } else if ("videos_root".equals(ident.type)) {
                includeVideosRootDocument(result);
            } else if ("videos_bucket".equals(ident.type)) {
                cursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, VideosBucketQuery.PROJECTION, "bucket_id=" + ident.id, null, "bucket_id, date_modified DESC");
                copyNotificationUri(result, cursor);
                if (cursor.moveToFirst()) {
                    includeVideosBucket(result, cursor);
                }
            } else if ("video".equals(ident.type)) {
                cursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, VideoQuery.PROJECTION, "_id=" + ident.id, null, null);
                copyNotificationUri(result, cursor);
                if (cursor.moveToFirst()) {
                    includeVideo(result, cursor);
                }
            } else if ("audio_root".equals(ident.type)) {
                includeAudioRootDocument(result);
            } else if ("artist".equals(ident.type)) {
                cursor = resolver.query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, ArtistQuery.PROJECTION, "_id=" + ident.id, null, null);
                copyNotificationUri(result, cursor);
                if (cursor.moveToFirst()) {
                    includeArtist(result, cursor);
                }
            } else if ("album".equals(ident.type)) {
                cursor = resolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, AlbumQuery.PROJECTION, "_id=" + ident.id, null, null);
                copyNotificationUri(result, cursor);
                if (cursor.moveToFirst()) {
                    includeAlbum(result, cursor);
                }
            } else if ("audio".equals(ident.type)) {
                cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, SongQuery.PROJECTION, "_id=" + ident.id, null, null);
                copyNotificationUri(result, cursor);
                if (cursor.moveToFirst()) {
                    includeAudio(result, cursor);
                }
            } else {
                throw new UnsupportedOperationException("Unsupported document " + docId);
            }
            return result;
        } finally {
            IoUtils.closeQuietly((AutoCloseable) null);
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public Cursor queryChildDocuments(String docId, String[] projection, String sortOrder) throws FileNotFoundException {
        Cursor cursor;
        ContentResolver resolver = getContext().getContentResolver();
        MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        Ident ident = getIdentForDocId(docId);
        long token = Binder.clearCallingIdentity();
        Cursor cursor2 = null;
        try {
            if ("images_root".equals(ident.type)) {
                cursor = resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ImagesBucketQuery.PROJECTION, null, null, "bucket_id, date_modified DESC");
                copyNotificationUri(result, cursor);
                long lastId = Long.MIN_VALUE;
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    if (lastId != id) {
                        includeImagesBucket(result, cursor);
                        lastId = id;
                    }
                }
            } else if ("images_bucket".equals(ident.type)) {
                cursor = resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ImageQuery.PROJECTION, "bucket_id=" + ident.id, null, null);
                copyNotificationUri(result, cursor);
                while (cursor.moveToNext()) {
                    includeImage(result, cursor);
                }
            } else if ("videos_root".equals(ident.type)) {
                cursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, VideosBucketQuery.PROJECTION, null, null, "bucket_id, date_modified DESC");
                copyNotificationUri(result, cursor);
                long lastId2 = Long.MIN_VALUE;
                while (cursor.moveToNext()) {
                    long id2 = cursor.getLong(0);
                    if (lastId2 != id2) {
                        includeVideosBucket(result, cursor);
                        lastId2 = id2;
                    }
                }
            } else if ("videos_bucket".equals(ident.type)) {
                cursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, VideoQuery.PROJECTION, "bucket_id=" + ident.id, null, null);
                copyNotificationUri(result, cursor);
                while (cursor.moveToNext()) {
                    includeVideo(result, cursor);
                }
            } else if ("audio_root".equals(ident.type)) {
                cursor = resolver.query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, ArtistQuery.PROJECTION, null, null, null);
                copyNotificationUri(result, cursor);
                while (cursor.moveToNext()) {
                    includeArtist(result, cursor);
                }
            } else if ("artist".equals(ident.type)) {
                cursor = resolver.query(MediaStore.Audio.Artists.Albums.getContentUri("external", ident.id), AlbumQuery.PROJECTION, null, null, null);
                copyNotificationUri(result, cursor);
                while (cursor.moveToNext()) {
                    includeAlbum(result, cursor);
                }
            } else if ("album".equals(ident.type)) {
                cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, SongQuery.PROJECTION, "album_id=" + ident.id, null, null);
                copyNotificationUri(result, cursor);
                while (cursor.moveToNext()) {
                    includeAudio(result, cursor);
                }
            } else {
                throw new UnsupportedOperationException("Unsupported document " + docId);
            }
            return result;
        } finally {
            IoUtils.closeQuietly(cursor2);
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public Cursor queryRecentDocuments(String rootId, String[] projection) throws FileNotFoundException {
        Cursor cursor;
        ContentResolver resolver = getContext().getContentResolver();
        MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        long token = Binder.clearCallingIdentity();
        Cursor cursor2 = null;
        try {
            if ("images_root".equals(rootId)) {
                cursor = resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ImageQuery.PROJECTION, null, null, "date_modified DESC");
                copyNotificationUri(result, cursor);
                while (cursor.moveToNext() && result.getCount() < 64) {
                    includeImage(result, cursor);
                }
            } else if ("videos_root".equals(rootId)) {
                cursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, VideoQuery.PROJECTION, null, null, "date_modified DESC");
                copyNotificationUri(result, cursor);
                while (cursor.moveToNext() && result.getCount() < 64) {
                    includeVideo(result, cursor);
                }
            } else {
                throw new UnsupportedOperationException("Unsupported root " + rootId);
            }
            return result;
        } finally {
            IoUtils.closeQuietly(cursor2);
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public ParcelFileDescriptor openDocument(String docId, String mode, CancellationSignal signal) throws FileNotFoundException {
        Uri target;
        Ident ident = getIdentForDocId(docId);
        if (!"r".equals(mode)) {
            throw new IllegalArgumentException("Media is read-only");
        }
        if ("image".equals(ident.type) && ident.id != -1) {
            target = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ident.id);
        } else if ("video".equals(ident.type) && ident.id != -1) {
            target = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, ident.id);
        } else if ("audio".equals(ident.type) && ident.id != -1) {
            target = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ident.id);
        } else {
            throw new UnsupportedOperationException("Unsupported document " + docId);
        }
        long token = Binder.clearCallingIdentity();
        try {
            return getContext().getContentResolver().openFileDescriptor(target, mode);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String docId, Point sizeHint, CancellationSignal signal) throws FileNotFoundException {
        AssetFileDescriptor assetFileDescriptorOpenOrCreateVideoThumbnailCleared;
        getContext().getContentResolver();
        Ident ident = getIdentForDocId(docId);
        long token = Binder.clearCallingIdentity();
        try {
            if ("images_bucket".equals(ident.type)) {
                long id = getImageForBucketCleared(ident.id);
                assetFileDescriptorOpenOrCreateVideoThumbnailCleared = openOrCreateImageThumbnailCleared(id, signal);
            } else if ("image".equals(ident.type)) {
                assetFileDescriptorOpenOrCreateVideoThumbnailCleared = openOrCreateImageThumbnailCleared(ident.id, signal);
            } else if ("videos_bucket".equals(ident.type)) {
                long id2 = getVideoForBucketCleared(ident.id);
                assetFileDescriptorOpenOrCreateVideoThumbnailCleared = openOrCreateVideoThumbnailCleared(id2, signal);
            } else {
                if (!"video".equals(ident.type)) {
                    throw new UnsupportedOperationException("Unsupported document " + docId);
                }
                assetFileDescriptorOpenOrCreateVideoThumbnailCleared = openOrCreateVideoThumbnailCleared(ident.id, signal);
            }
            return assetFileDescriptorOpenOrCreateVideoThumbnailCleared;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private boolean isEmpty(Uri uri) {
        boolean z;
        ContentResolver resolver = getContext().getContentResolver();
        long token = Binder.clearCallingIdentity();
        Cursor cursor = null;
        try {
            cursor = resolver.query(uri, new String[]{"_id"}, null, null, null);
            if (cursor != null) {
                z = cursor.getCount() == 0;
            }
            return z;
        } finally {
            IoUtils.closeQuietly(cursor);
            Binder.restoreCallingIdentity(token);
        }
    }

    private void includeImagesRoot(MatrixCursor result) {
        int flags = 6;
        if (isEmpty(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)) {
            flags = 6 | 65536;
            sReturnedImagesEmpty = true;
        }
        MatrixCursor.RowBuilder row = result.newRow();
        row.add("root_id", "images_root");
        row.add("flags", Integer.valueOf(flags));
        row.add("title", getContext().getString(R.string.root_images));
        row.add("document_id", "images_root");
        row.add("mime_types", IMAGE_MIME_TYPES);
    }

    private void includeVideosRoot(MatrixCursor result) {
        int flags = 6;
        if (isEmpty(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)) {
            flags = 6 | 65536;
            sReturnedVideosEmpty = true;
        }
        MatrixCursor.RowBuilder row = result.newRow();
        row.add("root_id", "videos_root");
        row.add("flags", Integer.valueOf(flags));
        row.add("title", getContext().getString(R.string.root_videos));
        row.add("document_id", "videos_root");
        row.add("mime_types", VIDEO_MIME_TYPES);
    }

    private void includeAudioRoot(MatrixCursor result) {
        int flags = 2;
        if (isEmpty(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)) {
            flags = 2 | 65536;
            sReturnedAudioEmpty = true;
        }
        MatrixCursor.RowBuilder row = result.newRow();
        row.add("root_id", "audio_root");
        row.add("flags", Integer.valueOf(flags));
        row.add("title", getContext().getString(R.string.root_audio));
        row.add("document_id", "audio_root");
        row.add("mime_types", AUDIO_MIME_TYPES);
    }

    private void includeImagesRootDocument(MatrixCursor result) {
        MatrixCursor.RowBuilder row = result.newRow();
        row.add("document_id", "images_root");
        row.add("_display_name", getContext().getString(R.string.root_images));
        row.add("flags", 48);
        row.add("mime_type", "vnd.android.document/directory");
    }

    private void includeVideosRootDocument(MatrixCursor result) {
        MatrixCursor.RowBuilder row = result.newRow();
        row.add("document_id", "videos_root");
        row.add("_display_name", getContext().getString(R.string.root_videos));
        row.add("flags", 48);
        row.add("mime_type", "vnd.android.document/directory");
    }

    private void includeAudioRootDocument(MatrixCursor result) {
        MatrixCursor.RowBuilder row = result.newRow();
        row.add("document_id", "audio_root");
        row.add("_display_name", getContext().getString(R.string.root_audio));
        row.add("mime_type", "vnd.android.document/directory");
    }

    private void includeImagesBucket(MatrixCursor result, Cursor cursor) {
        long id = cursor.getLong(0);
        String docId = getDocIdForIdent("images_bucket", id);
        MatrixCursor.RowBuilder row = result.newRow();
        row.add("document_id", docId);
        row.add("_display_name", cursor.getString(1));
        row.add("mime_type", "vnd.android.document/directory");
        row.add("last_modified", Long.valueOf(cursor.getLong(2) * 1000));
        row.add("flags", 65585);
    }

    private void includeImage(MatrixCursor result, Cursor cursor) {
        long id = cursor.getLong(0);
        String docId = getDocIdForIdent("image", id);
        MatrixCursor.RowBuilder row = result.newRow();
        row.add("document_id", docId);
        row.add("_display_name", cursor.getString(1));
        row.add("_size", Long.valueOf(cursor.getLong(3)));
        row.add("mime_type", cursor.getString(2));
        row.add("last_modified", Long.valueOf(cursor.getLong(4) * 1000));
        row.add("flags", 1);
    }

    private void includeVideosBucket(MatrixCursor result, Cursor cursor) {
        long id = cursor.getLong(0);
        String docId = getDocIdForIdent("videos_bucket", id);
        MatrixCursor.RowBuilder row = result.newRow();
        row.add("document_id", docId);
        row.add("_display_name", cursor.getString(1));
        row.add("mime_type", "vnd.android.document/directory");
        row.add("last_modified", Long.valueOf(cursor.getLong(2) * 1000));
        row.add("flags", 65585);
    }

    private void includeVideo(MatrixCursor result, Cursor cursor) {
        long id = cursor.getLong(0);
        String docId = getDocIdForIdent("video", id);
        MatrixCursor.RowBuilder row = result.newRow();
        row.add("document_id", docId);
        row.add("_display_name", cursor.getString(1));
        row.add("_size", Long.valueOf(cursor.getLong(3)));
        row.add("mime_type", cursor.getString(2));
        row.add("last_modified", Long.valueOf(cursor.getLong(4) * 1000));
        row.add("flags", 1);
    }

    private void includeArtist(MatrixCursor result, Cursor cursor) {
        long id = cursor.getLong(0);
        String docId = getDocIdForIdent("artist", id);
        MatrixCursor.RowBuilder row = result.newRow();
        row.add("document_id", docId);
        row.add("_display_name", cursor.getString(1));
        row.add("mime_type", "vnd.android.document/directory");
    }

    private void includeAlbum(MatrixCursor result, Cursor cursor) {
        long id = cursor.getLong(0);
        String docId = getDocIdForIdent("album", id);
        MatrixCursor.RowBuilder row = result.newRow();
        row.add("document_id", docId);
        row.add("_display_name", cursor.getString(1));
        row.add("mime_type", "vnd.android.document/directory");
    }

    private void includeAudio(MatrixCursor result, Cursor cursor) {
        long id = cursor.getLong(0);
        String docId = getDocIdForIdent("audio", id);
        MatrixCursor.RowBuilder row = result.newRow();
        row.add("document_id", docId);
        row.add("_display_name", cursor.getString(1));
        row.add("_size", Long.valueOf(cursor.getLong(3)));
        row.add("mime_type", cursor.getString(2));
        row.add("last_modified", Long.valueOf(cursor.getLong(4) * 1000));
    }

    private long getImageForBucketCleared(long bucketId) throws FileNotFoundException {
        ContentResolver resolver = getContext().getContentResolver();
        Cursor cursor = null;
        try {
            cursor = resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ImagesBucketThumbnailQuery.PROJECTION, "bucket_id=" + bucketId, null, "date_modified DESC");
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
            IoUtils.closeQuietly(cursor);
            throw new FileNotFoundException("No video found for bucket");
        } finally {
            IoUtils.closeQuietly(cursor);
        }
    }

    private ParcelFileDescriptor openImageThumbnailCleared(long id, CancellationSignal signal) throws FileNotFoundException {
        ContentResolver resolver = getContext().getContentResolver();
        Cursor cursor = null;
        try {
            cursor = resolver.query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, ImageThumbnailQuery.PROJECTION, "image_id=" + id, null, null, signal);
            if (!cursor.moveToFirst()) {
                return null;
            }
            String data = cursor.getString(0);
            return ParcelFileDescriptor.open(new File(data), 268435456);
        } finally {
            IoUtils.closeQuietly(cursor);
        }
    }

    private AssetFileDescriptor openOrCreateImageThumbnailCleared(long id, CancellationSignal signal) throws FileNotFoundException {
        Bundle extras;
        ContentResolver resolver = getContext().getContentResolver();
        ParcelFileDescriptor pfd = openImageThumbnailCleared(id, signal);
        if (pfd == null) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            MediaStore.Images.Thumbnails.getThumbnail(resolver, id, 1, opts);
            pfd = openImageThumbnailCleared(id, signal);
        }
        if (pfd == null) {
            Uri fullUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
            pfd = resolver.openFileDescriptor(fullUri, "r", signal);
        }
        int orientation = queryOrientationForImage(id, signal);
        if (orientation != 0) {
            extras = new Bundle(1);
            extras.putInt("android.content.extra.ORIENTATION", orientation);
        } else {
            extras = null;
        }
        return new AssetFileDescriptor(pfd, 0L, -1L, extras);
    }

    private long getVideoForBucketCleared(long bucketId) throws FileNotFoundException {
        ContentResolver resolver = getContext().getContentResolver();
        Cursor cursor = null;
        try {
            cursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, VideosBucketThumbnailQuery.PROJECTION, "bucket_id=" + bucketId, null, "date_modified DESC");
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
            IoUtils.closeQuietly(cursor);
            throw new FileNotFoundException("No video found for bucket");
        } finally {
            IoUtils.closeQuietly(cursor);
        }
    }

    private AssetFileDescriptor openVideoThumbnailCleared(long id, CancellationSignal signal) throws FileNotFoundException {
        ContentResolver resolver = getContext().getContentResolver();
        Cursor cursor = null;
        try {
            cursor = resolver.query(MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI, VideoThumbnailQuery.PROJECTION, "video_id=" + id, null, null, signal);
            if (!cursor.moveToFirst()) {
                return null;
            }
            String data = cursor.getString(0);
            return new AssetFileDescriptor(ParcelFileDescriptor.open(new File(data), 268435456), 0L, -1L);
        } finally {
            IoUtils.closeQuietly(cursor);
        }
    }

    private AssetFileDescriptor openOrCreateVideoThumbnailCleared(long id, CancellationSignal signal) throws FileNotFoundException {
        ContentResolver resolver = getContext().getContentResolver();
        AssetFileDescriptor afd = openVideoThumbnailCleared(id, signal);
        if (afd == null) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            MediaStore.Video.Thumbnails.getThumbnail(resolver, id, 1, opts);
            return openVideoThumbnailCleared(id, signal);
        }
        return afd;
    }

    private int queryOrientationForImage(long id, CancellationSignal signal) {
        ContentResolver resolver = getContext().getContentResolver();
        Cursor cursor = null;
        try {
            cursor = resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ImageOrientationQuery.PROJECTION, "_id=" + id, null, null, signal);
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
            Log.w("MediaDocumentsProvider", "Missing orientation data for " + id);
            return 0;
        } finally {
            IoUtils.closeQuietly(cursor);
        }
    }
}
