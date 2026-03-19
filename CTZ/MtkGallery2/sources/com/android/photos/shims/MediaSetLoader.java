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
        public void onSyncDone(MediaSet mediaSet, int i) {
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
        this.mMediaSet = this.mDataManager.getMediaSet(this.mDataManager.getTopSetPath(3));
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
        MatrixCursor matrixCursor = new MatrixCursor(AlbumSetLoader.PROJECTION);
        Object[] objArr = new Object[AlbumSetLoader.PROJECTION.length];
        int subMediaSetCount = this.mMediaSet.getSubMediaSetCount();
        ArrayList<MediaItem> arrayList = new ArrayList<>(subMediaSetCount);
        for (int i = 0; i < subMediaSetCount; i++) {
            MediaSet subMediaSet = this.mMediaSet.getSubMediaSet(i);
            subMediaSet.reload();
            objArr[0] = Integer.valueOf(i);
            objArr[1] = subMediaSet.getName();
            objArr[7] = Integer.valueOf(subMediaSet.getMediaItemCount());
            objArr[8] = Integer.valueOf(subMediaSet.getSupportedOperations());
            MediaItem coverMediaItem = subMediaSet.getCoverMediaItem();
            if (coverMediaItem != null) {
                objArr[2] = Long.valueOf(coverMediaItem.getDateInMs());
            }
            arrayList.add(coverMediaItem);
            matrixCursor.addRow(objArr);
        }
        synchronized (this.mMediaSet) {
            this.mCoverItems = arrayList;
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
        bitmapJobDrawable.setMediaItem(this.mCoverItems.get(cursor.getInt(0)));
        return bitmapJobDrawable;
    }

    @Override
    public Uri uriForItem(Cursor cursor) {
        MediaSet subMediaSet = this.mMediaSet.getSubMediaSet(cursor.getInt(0));
        if (subMediaSet == null) {
            return null;
        }
        return subMediaSet.getContentUri();
    }

    @Override
    public ArrayList<Uri> urisForSubItems(Cursor cursor) {
        MediaSet subMediaSet = this.mMediaSet.getSubMediaSet(cursor.getInt(0));
        if (subMediaSet == null) {
            return null;
        }
        final ArrayList<Uri> arrayList = new ArrayList<>();
        subMediaSet.enumerateMediaItems(new MediaSet.ItemConsumer() {
            @Override
            public void consume(int i, MediaItem mediaItem) {
                if (mediaItem != null) {
                    arrayList.add(mediaItem.getContentUri());
                }
            }

            @Override
            public boolean stopConsume() {
                return false;
            }
        });
        return arrayList;
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
        MediaSet subMediaSet = this.mMediaSet.getSubMediaSet(cursor.getInt(0));
        if (subMediaSet != null) {
            return subMediaSet.getPath();
        }
        return null;
    }
}
