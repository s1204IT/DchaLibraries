package com.android.photos.shims;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.SparseArray;
import com.android.gallery3d.data.ContentListener;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.util.Future;
import com.android.photos.data.PhotoSetLoader;
import java.util.ArrayList;

public class MediaItemsLoader extends AsyncTaskLoader<Cursor> implements LoaderCompatShim<Cursor> {
    private static final MediaSet.SyncListener sNullListener = new MediaSet.SyncListener() {
        @Override
        public void onSyncDone(MediaSet mediaSet, int i) {
        }
    };
    private final DataManager mDataManager;
    private SparseArray<MediaItem> mMediaItems;
    private final MediaSet mMediaSet;
    private ContentListener mObserver;
    private Future<Integer> mSyncTask;

    public MediaItemsLoader(Context context) {
        super(context);
        this.mSyncTask = null;
        this.mObserver = new ContentListener() {
            @Override
            public void onContentDirty() {
                MediaItemsLoader.this.onContentChanged();
            }
        };
        this.mDataManager = DataManager.from(context);
        this.mMediaSet = this.mDataManager.getMediaSet(this.mDataManager.getTopSetPath(3));
    }

    public MediaItemsLoader(Context context, String str) {
        super(context);
        this.mSyncTask = null;
        this.mObserver = new ContentListener() {
            @Override
            public void onContentDirty() {
                MediaItemsLoader.this.onContentChanged();
            }
        };
        this.mDataManager = DataManager.from(getContext());
        this.mMediaSet = this.mDataManager.getMediaSet(str);
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        this.mMediaSet.addContentListener(this.mObserver);
        this.mSyncTask = this.mMediaSet.requestSync(sNullListener);
        forceLoad();
    }

    @Override
    protected boolean onCancelLoad() {
        if (this.mSyncTask != null) {
            this.mSyncTask.cancel();
            this.mSyncTask = null;
        }
        return super.onCancelLoad();
    }

    @Override
    protected void onStopLoading() {
        super.onStopLoading();
        cancelLoad();
        this.mMediaSet.removeContentListener(this.mObserver);
    }

    @Override
    protected void onReset() {
        super.onReset();
        onStopLoading();
    }

    @Override
    public Cursor loadInBackground() {
        this.mMediaSet.reload();
        final MatrixCursor matrixCursor = new MatrixCursor(PhotoSetLoader.PROJECTION);
        final Object[] objArr = new Object[PhotoSetLoader.PROJECTION.length];
        final SparseArray<MediaItem> sparseArray = new SparseArray<>();
        this.mMediaSet.enumerateTotalMediaItems(new MediaSet.ItemConsumer() {
            @Override
            public void consume(int i, MediaItem mediaItem) {
                int i2 = 0;
                objArr[0] = Integer.valueOf(i);
                objArr[1] = mediaItem.getContentUri().toString();
                objArr[4] = Long.valueOf(mediaItem.getDateInMs());
                objArr[3] = Integer.valueOf(mediaItem.getHeight());
                objArr[2] = Integer.valueOf(mediaItem.getWidth());
                objArr[2] = Integer.valueOf(mediaItem.getWidth());
                int mediaType = mediaItem.getMediaType();
                if (mediaType != 2) {
                    if (mediaType == 4) {
                        i2 = 3;
                    }
                } else {
                    i2 = 1;
                }
                objArr[5] = Integer.valueOf(i2);
                objArr[6] = Integer.valueOf(mediaItem.getSupportedOperations());
                matrixCursor.addRow(objArr);
                sparseArray.append(i, mediaItem);
            }

            @Override
            public boolean stopConsume() {
                return false;
            }
        });
        synchronized (this.mMediaSet) {
            this.mMediaItems = sparseArray;
        }
        return matrixCursor;
    }

    @Override
    public Drawable drawableForItem(Cursor cursor, Drawable drawable) {
        BitmapJobDrawable bitmapJobDrawable;
        if (drawable != null) {
            boolean z = drawable instanceof BitmapJobDrawable;
            bitmapJobDrawable = drawable;
            if (!z) {
                bitmapJobDrawable = new BitmapJobDrawable();
            }
        }
        bitmapJobDrawable.setMediaItem(this.mMediaItems.get(cursor.getInt(0)));
        return bitmapJobDrawable;
    }

    public static int getThumbnailSize() {
        return MediaItem.getTargetSize(2);
    }

    @Override
    public Uri uriForItem(Cursor cursor) {
        MediaItem mediaItem = this.mMediaItems.get(cursor.getInt(0));
        if (mediaItem == null) {
            return null;
        }
        return mediaItem.getContentUri();
    }

    @Override
    public ArrayList<Uri> urisForSubItems(Cursor cursor) {
        return null;
    }

    @Override
    public void deleteItemWithPath(Object obj) {
        MediaObject mediaObject = this.mDataManager.getMediaObject((Path) obj);
        if (mediaObject != null) {
            mediaObject.delete();
        }
    }

    @Override
    public Object getPathForItem(Cursor cursor) {
        MediaItem mediaItem = this.mMediaItems.get(cursor.getInt(0));
        if (mediaItem != null) {
            return mediaItem.getPath();
        }
        return null;
    }
}
