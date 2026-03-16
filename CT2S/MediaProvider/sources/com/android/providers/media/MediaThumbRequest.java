package com.android.providers.media;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MiniThumbFile;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.Random;

class MediaThumbRequest {
    private static final String[] THUMB_PROJECTION = {"_id"};
    private static final Random sRandom = new Random();
    ContentResolver mCr;
    long mGroupId;
    boolean mIsVideo;
    long mMagic;
    String mOrigColumnName;
    long mOrigId;
    String mPath;
    int mPriority;
    Uri mThumbUri;
    Uri mUri;
    long mRequestTime = System.currentTimeMillis();
    int mCallingPid = Binder.getCallingPid();
    State mState = State.WAIT;

    enum State {
        WAIT,
        DONE,
        CANCEL
    }

    static Comparator<MediaThumbRequest> getComparator() {
        return new Comparator<MediaThumbRequest>() {
            @Override
            public int compare(MediaThumbRequest r1, MediaThumbRequest r2) {
                if (r1.mPriority != r2.mPriority) {
                    return r1.mPriority < r2.mPriority ? -1 : 1;
                }
                if (r1.mRequestTime == r2.mRequestTime) {
                    return 0;
                }
                return r1.mRequestTime >= r2.mRequestTime ? 1 : -1;
            }
        };
    }

    MediaThumbRequest(ContentResolver cr, String path, Uri uri, int priority, long magic) {
        this.mCr = cr;
        this.mPath = path;
        this.mPriority = priority;
        this.mMagic = magic;
        this.mUri = uri;
        this.mIsVideo = "video".equals(uri.getPathSegments().get(1));
        this.mOrigId = ContentUris.parseId(uri);
        this.mThumbUri = this.mIsVideo ? MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI : MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI;
        this.mOrigColumnName = this.mIsVideo ? "video_id" : "image_id";
        String groupIdParam = uri.getQueryParameter("group_id");
        if (groupIdParam != null) {
            this.mGroupId = Long.parseLong(groupIdParam);
        }
    }

    Uri updateDatabase(Bitmap thumbnail) {
        Cursor c = this.mCr.query(this.mThumbUri, THUMB_PROJECTION, this.mOrigColumnName + " = " + this.mOrigId, null, null);
        if (c == null) {
            return null;
        }
        try {
            if (c.moveToFirst()) {
                Uri uriWithAppendedId = ContentUris.withAppendedId(this.mThumbUri, c.getLong(0));
            }
            if (c != null) {
                c.close();
            }
            ContentValues values = new ContentValues(4);
            values.put("kind", (Integer) 1);
            values.put(this.mOrigColumnName, Long.valueOf(this.mOrigId));
            values.put("width", Integer.valueOf(thumbnail.getWidth()));
            values.put("height", Integer.valueOf(thumbnail.getHeight()));
            try {
                return this.mCr.insert(this.mThumbUri, values);
            } catch (Exception ex) {
                Log.w("MediaThumbRequest", ex);
                return null;
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    void execute() throws IOException {
        long magic;
        MiniThumbFile miniThumbFile = MiniThumbFile.instance(this.mUri);
        long magic2 = this.mMagic;
        if (magic2 != 0) {
            long fileMagic = miniThumbFile.getMagic(this.mOrigId);
            if (fileMagic == magic2) {
                Cursor c = null;
                ParcelFileDescriptor pfd = null;
                try {
                    c = this.mCr.query(this.mThumbUri, THUMB_PROJECTION, this.mOrigColumnName + " = " + this.mOrigId, null, null);
                    if (c != null && c.moveToFirst()) {
                        pfd = this.mCr.openFileDescriptor(this.mThumbUri.buildUpon().appendPath(c.getString(0)).build(), "r");
                    }
                    if (c != null) {
                        c.close();
                    }
                    if (pfd != null) {
                        pfd.close();
                        return;
                    }
                } catch (IOException e) {
                    if (c != null) {
                        c.close();
                    }
                    if (0 != 0) {
                        pfd.close();
                        return;
                    }
                } catch (Throwable th) {
                    if (c != null) {
                        c.close();
                    }
                    if (0 == 0) {
                        throw th;
                    }
                    pfd.close();
                    return;
                }
            }
        }
        Bitmap bitmap = null;
        if (this.mPath != null) {
            if (this.mIsVideo) {
                bitmap = ThumbnailUtils.createVideoThumbnail(this.mPath, 1);
            } else {
                bitmap = ThumbnailUtils.createImageThumbnail(this.mPath, 1);
            }
            if (bitmap == null) {
                Log.w("MediaThumbRequest", "Can't create mini thumbnail for " + this.mPath);
                return;
            }
            Uri uri = updateDatabase(bitmap);
            if (uri != null) {
                OutputStream thumbOut = this.mCr.openOutputStream(uri);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, thumbOut);
                thumbOut.close();
            }
        }
        Bitmap bitmap2 = ThumbnailUtils.extractThumbnail(bitmap, 96, 96, 2);
        if (bitmap2 != null) {
            ByteArrayOutputStream miniOutStream = new ByteArrayOutputStream();
            bitmap2.compress(Bitmap.CompressFormat.JPEG, 75, miniOutStream);
            bitmap2.recycle();
            byte[] data = null;
            try {
                miniOutStream.close();
                data = miniOutStream.toByteArray();
            } catch (IOException ex) {
                Log.e("MediaThumbRequest", "got exception ex " + ex);
            }
            if (data != null) {
                do {
                    magic = sRandom.nextLong();
                } while (magic == 0);
                miniThumbFile.saveMiniThumbToFile(data, this.mOrigId, magic);
                ContentValues values = new ContentValues();
                values.put("mini_thumb_magic", Long.valueOf(magic));
                try {
                    this.mCr.update(this.mUri, values, null, null);
                    this.mMagic = magic;
                } catch (IllegalStateException ex2) {
                    Log.e("MediaThumbRequest", "got exception while updating database " + ex2);
                }
            }
            miniThumbFile.deactivate();
        }
        Log.w("MediaThumbRequest", "can't create bitmap for thumbnail.");
        miniThumbFile.deactivate();
    }
}
