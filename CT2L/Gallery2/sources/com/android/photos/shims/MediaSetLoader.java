package com.android.photos.shims;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import com.android.gallery3d.data.ContentListener;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.util.Future;
import com.android.photos.data.AlbumSetLoader;
import java.util.ArrayList;

public class MediaSetLoader extends AsyncTaskLoader<Cursor> implements LoaderCompatShim<Cursor> {
    private static final MediaSet.SyncListener sNullListener = new MediaSet.SyncListener() {
        @Override
        public void onSyncDone(MediaSet mediaSet, int resultCode) {
        }
    };
    private ArrayList<MediaItem> mCoverItems;
    private final DataManager mDataManager;
    private final MediaSet mMediaSet;
    private ContentListener mObserver;
    private Future<Integer> mSyncTask;

    public MediaSetLoader(Context context) {
        super(context);
        this.mSyncTask = null;
        this.mObserver = new ContentListener() {
            @Override
            public void onContentDirty() {
                MediaSetLoader.this.onContentChanged();
            }
        };
        this.mDataManager = DataManager.from(context);
        String path = this.mDataManager.getTopSetPath(3);
        this.mMediaSet = this.mDataManager.getMediaSet(path);
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
        MatrixCursor cursor = new MatrixCursor(AlbumSetLoader.PROJECTION);
        Object[] row = new Object[AlbumSetLoader.PROJECTION.length];
        int count = this.mMediaSet.getSubMediaSetCount();
        ArrayList<MediaItem> coverItems = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            MediaSet m = this.mMediaSet.getSubMediaSet(i);
            m.reload();
            row[0] = Integer.valueOf(i);
            row[1] = m.getName();
            row[7] = Integer.valueOf(m.getMediaItemCount());
            row[8] = Integer.valueOf(m.getSupportedOperations());
            MediaItem coverItem = m.getCoverMediaItem();
            if (coverItem != null) {
                row[2] = Long.valueOf(coverItem.getDateInMs());
            }
            coverItems.add(coverItem);
            cursor.addRow(row);
        }
        synchronized (this.mMediaSet) {
            this.mCoverItems = coverItems;
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
        drawable.setMediaItem(this.mCoverItems.get(index));
        return drawable;
    }

    @Override
    public Uri uriForItem(Cursor item) {
        int index = item.getInt(0);
        MediaSet ms = this.mMediaSet.getSubMediaSet(index);
        if (ms == null) {
            return null;
        }
        return ms.getContentUri();
    }

    @Override
    public ArrayList<Uri> urisForSubItems(Cursor item) {
        int index = item.getInt(0);
        MediaSet ms = this.mMediaSet.getSubMediaSet(index);
        if (ms == null) {
            return null;
        }
        final ArrayList<Uri> result = new ArrayList<>();
        ms.enumerateMediaItems(new MediaSet.ItemConsumer() {
            @Override
            public void consume(int index2, MediaItem item2) {
                if (item2 != null) {
                    result.add(item2.getContentUri());
                }
            }
        });
        return result;
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
        MediaSet ms = this.mMediaSet.getSubMediaSet(index);
        if (ms != null) {
            return ms.getPath();
        }
        return null;
    }
}
