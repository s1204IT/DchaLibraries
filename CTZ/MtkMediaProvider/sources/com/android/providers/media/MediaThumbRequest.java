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
            public int compare(MediaThumbRequest mediaThumbRequest, MediaThumbRequest mediaThumbRequest2) {
                if (mediaThumbRequest.mPriority != mediaThumbRequest2.mPriority) {
                    return mediaThumbRequest.mPriority < mediaThumbRequest2.mPriority ? -1 : 1;
                }
                if (mediaThumbRequest.mRequestTime == mediaThumbRequest2.mRequestTime) {
                    return 0;
                }
                return mediaThumbRequest.mRequestTime < mediaThumbRequest2.mRequestTime ? -1 : 1;
            }
        };
    }

    MediaThumbRequest(ContentResolver contentResolver, String str, Uri uri, int i, long j) {
        Uri uri2;
        String str2;
        this.mCr = contentResolver;
        this.mPath = str;
        this.mPriority = i;
        this.mMagic = j;
        this.mUri = uri;
        this.mIsVideo = "video".equals(uri.getPathSegments().get(1));
        this.mOrigId = ContentUris.parseId(uri);
        if (this.mIsVideo) {
            uri2 = MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI;
        } else {
            uri2 = MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI;
        }
        this.mThumbUri = uri2;
        if (this.mIsVideo) {
            str2 = "video_id";
        } else {
            str2 = "image_id";
        }
        this.mOrigColumnName = str2;
        String queryParameter = uri.getQueryParameter("group_id");
        if (queryParameter != null) {
            this.mGroupId = Long.parseLong(queryParameter);
        }
    }

    Uri updateDatabase(Bitmap bitmap) {
        Cursor cursorQuery = this.mCr.query(this.mThumbUri, THUMB_PROJECTION, this.mOrigColumnName + " = " + this.mOrigId, null, null);
        if (cursorQuery == null) {
            return null;
        }
        try {
            if (cursorQuery.moveToFirst()) {
                return ContentUris.withAppendedId(this.mThumbUri, cursorQuery.getLong(0));
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            ContentValues contentValues = new ContentValues(4);
            contentValues.put("kind", (Integer) 1);
            contentValues.put(this.mOrigColumnName, Long.valueOf(this.mOrigId));
            contentValues.put("width", Integer.valueOf(bitmap.getWidth()));
            contentValues.put("height", Integer.valueOf(bitmap.getHeight()));
            try {
                return this.mCr.insert(this.mThumbUri, contentValues);
            } catch (Exception e) {
                Log.w("MediaThumbRequest", e);
                return null;
            }
        } finally {
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        }
    }

    void execute() throws Throwable {
        Bitmap bitmapCreateVideoThumbnail;
        byte[] byteArray;
        long jNextLong;
        Cursor cursorQuery;
        MiniThumbFile miniThumbFileInstance = MiniThumbFile.instance(this.mUri);
        long j = this.mMagic;
        Cursor cursor = null;
        if (j != 0 && miniThumbFileInstance.getMagic(this.mOrigId) == j) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                cursorQuery = this.mCr.query(this.mThumbUri, THUMB_PROJECTION, this.mOrigColumnName + " = " + this.mOrigId, null, null);
                if (cursorQuery != null) {
                    try {
                        ParcelFileDescriptor parcelFileDescriptorOpenFileDescriptor = cursorQuery.moveToFirst() ? this.mCr.openFileDescriptor(this.mThumbUri.buildUpon().appendPath(cursorQuery.getString(0)).build(), "r") : null;
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        if (parcelFileDescriptorOpenFileDescriptor != null) {
                            parcelFileDescriptorOpenFileDescriptor.close();
                            return;
                        }
                    } catch (IOException e) {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                    } catch (Throwable th) {
                        th = th;
                        cursor = cursorQuery;
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                        if (cursor != null) {
                            cursor.close();
                        }
                        throw th;
                    }
                }
            } catch (IOException e2) {
                cursorQuery = null;
            } catch (Throwable th2) {
                th = th2;
            }
        }
        if (this.mPath != null) {
            bitmapCreateVideoThumbnail = this.mIsVideo ? ThumbnailUtils.createVideoThumbnail(this.mPath, 1) : ThumbnailUtils.createImageThumbnail(this.mPath, 1);
            if (bitmapCreateVideoThumbnail == null) {
                Log.w("MediaThumbRequest", "Can't create mini thumbnail for " + this.mPath);
                return;
            }
            Uri uriUpdateDatabase = updateDatabase(bitmapCreateVideoThumbnail);
            if (uriUpdateDatabase != null) {
                OutputStream outputStreamOpenOutputStream = this.mCr.openOutputStream(uriUpdateDatabase);
                bitmapCreateVideoThumbnail.compress(Bitmap.CompressFormat.JPEG, 85, outputStreamOpenOutputStream);
                outputStreamOpenOutputStream.close();
            }
        } else {
            bitmapCreateVideoThumbnail = null;
        }
        Bitmap bitmapExtractThumbnail = ThumbnailUtils.extractThumbnail(bitmapCreateVideoThumbnail, 96, 96, 2);
        if (bitmapExtractThumbnail != null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmapExtractThumbnail.compress(Bitmap.CompressFormat.JPEG, 75, byteArrayOutputStream);
            bitmapExtractThumbnail.recycle();
            try {
                byteArrayOutputStream.close();
                byteArray = byteArrayOutputStream.toByteArray();
            } catch (IOException e3) {
                Log.e("MediaThumbRequest", "got exception ex " + e3);
                byteArray = null;
            }
            if (byteArray != null) {
                do {
                    jNextLong = sRandom.nextLong();
                } while (jNextLong == 0);
                miniThumbFileInstance.saveMiniThumbToFile(byteArray, this.mOrigId, jNextLong);
                ContentValues contentValues = new ContentValues();
                contentValues.put("mini_thumb_magic", Long.valueOf(jNextLong));
                try {
                    this.mCr.update(this.mUri, contentValues, null, null);
                    this.mMagic = jNextLong;
                } catch (IllegalStateException e4) {
                    Log.e("MediaThumbRequest", "got exception while updating database " + e4);
                }
            }
        } else {
            Log.w("MediaThumbRequest", "can't create bitmap for thumbnail.");
        }
        miniThumbFileInstance.deactivate();
    }
}
