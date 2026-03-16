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
        public void onSyncDone(MediaSet mediaSet, int resultCode) {
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
        String path = this.mDataManager.getTopSetPath(3);
        this.mMediaSet = this.mDataManager.getMediaSet(path);
    }

    public MediaItemsLoader(Context context, String parentPath) {
        super(context);
        this.mSyncTask = null;
        this.mObserver = new ContentListener() {
            @Override
            public void onContentDirty() {
                MediaItemsLoader.this.onContentChanged();
            }
        };
        this.mDataManager = DataManager.from(getContext());
        this.mMediaSet = this.mDataManager.getMediaSet(parentPath);
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
        final MatrixCursor cursor = new MatrixCursor(PhotoSetLoader.PROJECTION);
        final Object[] row = new Object[PhotoSetLoader.PROJECTION.length];
        final SparseArray<MediaItem> mediaItems = new SparseArray<>();
        this.mMediaSet.enumerateTotalMediaItems(new MediaSet.ItemConsumer() {
            @Override
            public void consume(int index, MediaItem item) {
                row[0] = Integer.valueOf(index);
                row[1] = item.getContentUri().toString();
                row[4] = Long.valueOf(item.getDateInMs());
                row[3] = Integer.valueOf(item.getHeight());
                row[2] = Integer.valueOf(item.getWidth());
                row[2] = Integer.valueOf(item.getWidth());
                int rawMediaType = item.getMediaType();
                int mappedMediaType = 0;
                if (rawMediaType == 2) {
                    mappedMediaType = 1;
                } else if (rawMediaType == 4) {
                    mappedMediaType = 3;
                }
                row[5] = Integer.valueOf(mappedMediaType);
                row[6] = Integer.valueOf(item.getSupportedOperations());
                cursor.addRow(row);
                mediaItems.append(index, item);
            }
        });
        synchronized (this.mMediaSet) {
            this.mMediaItems = mediaItems;
        }
        return cursor;
    }

    @Override
    public Drawable drawableForItem(Cursor item, Drawable recycle) {
        BitmapJobDrawable drawable;
        if (recycle == null || !(recycle instanceof BitmapJobDrawable)) {
            drawable = new BitmapJobDrawable();
        } else {
            drawable = (BitmapJobDrawable) recycle;
        }
        int index = item.getInt(0);
        drawable.setMediaItem(this.mMediaItems.get(index));
        return drawable;
    }

    public static int getThumbnailSize() {
        return MediaItem.getTargetSize(2);
    }

    @Override
    public Uri uriForItem(Cursor item) {
        int index = item.getInt(0);
        MediaItem mi = this.mMediaItems.get(index);
        if (mi == null) {
            return null;
        }
        return mi.getContentUri();
    }

    @Override
    public ArrayList<Uri> urisForSubItems(Cursor item) {
        return null;
    }

    @Override
    public void deleteItemWithPath(Object path) {
        MediaObject o = this.mDataManager.getMediaObject((Path) path);
        if (o != null) {
            o.delete();
        }
    }

    @Override
    public Object getPathForItem(Cursor item) {
        int index = item.getInt(0);
        MediaItem mi = this.mMediaItems.get(index);
        if (mi != null) {
            return mi.getPath();
        }
        return null;
    }
}
