package com.bumptech.glide.load.data;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import com.bumptech.glide.Priority;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class MediaStoreThumbFetcher implements DataFetcher<InputStream> {
    private static final ThumbnailStreamOpenerFactory DEFAULT_FACTORY = new ThumbnailStreamOpenerFactory();
    private static final int MINI_HEIGHT = 384;
    private static final int MINI_WIDTH = 512;
    private final Context context;
    private final long dateModified;
    private final DataFetcher<InputStream> defaultFetcher;
    private final ThumbnailStreamOpenerFactory factory;
    private final int height;
    private InputStream inputStream;
    private final Uri mediaStoreUri;
    private String mimeType;
    private final int orientation;
    private final int width;

    interface ThumbnailQuery {
        Cursor query(Context context, Uri uri);
    }

    public MediaStoreThumbFetcher(Context context, Uri mediaStoreUri, DataFetcher<InputStream> defaultFetcher, int width, int height, String mimeType, long dateModified, int orientation) {
        this(context, mediaStoreUri, defaultFetcher, width, height, mimeType, dateModified, orientation, DEFAULT_FACTORY);
    }

    MediaStoreThumbFetcher(Context context, Uri mediaStoreUri, DataFetcher<InputStream> defaultFetcher, int width, int height, String mimeType, long dateModified, int orientation, ThumbnailStreamOpenerFactory factory) {
        this.context = context;
        this.mediaStoreUri = mediaStoreUri;
        this.defaultFetcher = defaultFetcher;
        this.width = width;
        this.height = height;
        this.mimeType = mimeType;
        this.dateModified = dateModified;
        this.orientation = orientation;
        this.factory = factory;
    }

    @Override
    public InputStream loadData(Priority priority) throws Exception {
        ThumbnailStreamOpener fetcher = this.factory.build(this.mediaStoreUri, this.width, this.height);
        if (fetcher != null) {
            this.inputStream = fetcher.open(this.context, this.mediaStoreUri);
        }
        return this.inputStream != null ? this.inputStream : this.defaultFetcher.loadData(priority);
    }

    @Override
    public void cleanup() {
        if (this.inputStream != null) {
            try {
                this.inputStream.close();
            } catch (IOException e) {
            }
        }
        this.defaultFetcher.cleanup();
    }

    @Override
    public String getId() {
        return this.mediaStoreUri + this.mimeType + String.valueOf(this.dateModified) + String.valueOf(this.orientation);
    }

    @Override
    public void cancel() {
    }

    private static boolean isMediaStoreUri(Uri uri) {
        return uri != null && "content".equals(uri.getScheme()) && "media".equals(uri.getAuthority());
    }

    private static boolean isMediaStoreVideo(Uri uri) {
        return isMediaStoreUri(uri) && uri.getPathSegments().contains("video");
    }

    static class FileService {
        FileService() {
        }

        public boolean exists(File file) {
            return file.exists();
        }

        public long length(File file) {
            return file.length();
        }

        public File get(String path) {
            return new File(path);
        }
    }

    static class ThumbnailStreamOpener {
        private static final FileService DEFAULT_SERVICE = new FileService();
        private ThumbnailQuery query;
        private final FileService service;

        public ThumbnailStreamOpener(ThumbnailQuery query) {
            this(DEFAULT_SERVICE, query);
        }

        public ThumbnailStreamOpener(FileService service, ThumbnailQuery query) {
            this.service = service;
            this.query = query;
        }

        public InputStream open(Context context, Uri uri) throws FileNotFoundException {
            Uri thumbnailUri = null;
            Cursor cursor = this.query.query(context, uri);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        String path = cursor.getString(0);
                        if (!TextUtils.isEmpty(path)) {
                            File file = this.service.get(path);
                            if (this.service.exists(file) && this.service.length(file) > 0) {
                                thumbnailUri = Uri.fromFile(file);
                            }
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
            if (thumbnailUri == null) {
                return null;
            }
            InputStream inputStream = context.getContentResolver().openInputStream(thumbnailUri);
            return inputStream;
        }
    }

    static class ImageThumbnailQuery implements ThumbnailQuery {
        ImageThumbnailQuery() {
        }

        @Override
        public Cursor query(Context context, Uri uri) {
            String id = uri.getLastPathSegment();
            return context.getContentResolver().query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, new String[]{"_data"}, "image_id = ? AND kind = ?", new String[]{id, String.valueOf(1)}, null);
        }
    }

    static class VideoThumbnailQuery implements ThumbnailQuery {
        VideoThumbnailQuery() {
        }

        @Override
        public Cursor query(Context context, Uri uri) {
            String id = uri.getLastPathSegment();
            return context.getContentResolver().query(MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI, new String[]{"_data"}, "video_id = ? AND kind = ?", new String[]{id, String.valueOf(1)}, null);
        }
    }

    static class ThumbnailStreamOpenerFactory {
        ThumbnailStreamOpenerFactory() {
        }

        public ThumbnailStreamOpener build(Uri uri, int width, int height) {
            if (MediaStoreThumbFetcher.isMediaStoreUri(uri) && width <= 512 && height <= MediaStoreThumbFetcher.MINI_HEIGHT) {
                if (MediaStoreThumbFetcher.isMediaStoreVideo(uri)) {
                    return new ThumbnailStreamOpener(new VideoThumbnailQuery());
                }
                return new ThumbnailStreamOpener(new ImageThumbnailQuery());
            }
            return null;
        }
    }
}
