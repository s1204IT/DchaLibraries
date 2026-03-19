package com.android.gallery3d.data;

import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.BucketHelper;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.MediaSetUtils;
import com.android.gallery3d.util.ThreadPool;
import com.mediatek.galleryportable.TraceHelper;
import java.util.ArrayList;
import java.util.Iterator;

public class LocalAlbumSet extends MediaSet implements FutureListener<ArrayList<MediaSet>> {
    public static final Path PATH_ALL = Path.fromString("/local/all");
    public static final Path PATH_IMAGE = Path.fromString("/local/image");
    public static final Path PATH_VIDEO = Path.fromString("/local/video");
    private static final Uri[] mWatchUris = {MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.EXTERNAL_CONTENT_URI};
    private ArrayList<MediaSet> mAlbums;
    private final GalleryApp mApplication;
    private final Handler mHandler;
    private boolean mIsLoading;
    private ArrayList<MediaSet> mLoadBuffer;
    private Future<ArrayList<MediaSet>> mLoadTask;
    private final String mName;
    private final ChangeNotifier mNotifier;
    private final int mType;

    public LocalAlbumSet(Path path, GalleryApp galleryApp) {
        super(path, nextVersionNumber());
        this.mAlbums = new ArrayList<>();
        this.mApplication = galleryApp;
        this.mHandler = new Handler(galleryApp.getMainLooper());
        this.mType = getTypeFromPath(path);
        this.mNotifier = new ChangeNotifier(this, mWatchUris, galleryApp);
        this.mName = galleryApp.getResources().getString(R.string.set_label_local_albums);
    }

    private static int getTypeFromPath(Path path) {
        String[] strArrSplit = path.split();
        if (strArrSplit.length < 2) {
            throw new IllegalArgumentException(path.toString());
        }
        return getTypeFromString(strArrSplit[1]);
    }

    @Override
    public MediaSet getSubMediaSet(int i) {
        return this.mAlbums.get(i);
    }

    @Override
    public int getSubMediaSetCount() {
        return this.mAlbums.size();
    }

    @Override
    public String getName() {
        return this.mName;
    }

    private static int findBucket(BucketHelper.BucketEntry[] bucketEntryArr, int i) {
        int length = bucketEntryArr.length;
        for (int i2 = 0; i2 < length; i2++) {
            if (bucketEntryArr[i2].bucketId == i) {
                return i2;
            }
        }
        return -1;
    }

    private class AlbumsLoader implements ThreadPool.Job<ArrayList<MediaSet>> {
        private AlbumsLoader() {
        }

        @Override
        public ArrayList<MediaSet> run(ThreadPool.JobContext jobContext) {
            int i;
            TraceHelper.beginSection(">>>>LocalAlbumSet-AlbumsLoader");
            BucketHelper.BucketEntry[] bucketEntryArrLoadBucketEntries = BucketHelper.loadBucketEntries(jobContext, LocalAlbumSet.this.mApplication.getContentResolver(), LocalAlbumSet.this.mType);
            if (!jobContext.isCancelled()) {
                int iFindBucket = LocalAlbumSet.findBucket(bucketEntryArrLoadBucketEntries, MediaSetUtils.CAMERA_BUCKET_ID);
                if (iFindBucket != -1) {
                    LocalAlbumSet.circularShiftRight(bucketEntryArrLoadBucketEntries, 0, iFindBucket);
                    i = 1;
                } else {
                    i = 0;
                }
                int iFindBucket2 = LocalAlbumSet.findBucket(bucketEntryArrLoadBucketEntries, MediaSetUtils.DOWNLOAD_BUCKET_ID);
                if (iFindBucket2 != -1) {
                    LocalAlbumSet.circularShiftRight(bucketEntryArrLoadBucketEntries, i, iFindBucket2);
                }
                ArrayList<MediaSet> arrayList = new ArrayList<>();
                DataManager dataManager = LocalAlbumSet.this.mApplication.getDataManager();
                for (BucketHelper.BucketEntry bucketEntry : bucketEntryArrLoadBucketEntries) {
                    arrayList.add(LocalAlbumSet.this.getLocalAlbum(dataManager, LocalAlbumSet.this.mType, LocalAlbumSet.this.mPath, bucketEntry.bucketId, bucketEntry.bucketName));
                }
                TraceHelper.endSection();
                return arrayList;
            }
            TraceHelper.endSection();
            return null;
        }
    }

    private MediaSet getLocalAlbum(DataManager dataManager, int i, Path path, int i2, String str) {
        synchronized (DataManager.LOCK) {
            Path child = path.getChild(i2);
            MediaObject mediaObjectPeekMediaObject = dataManager.peekMediaObject(child);
            if (mediaObjectPeekMediaObject != null) {
                return (MediaSet) mediaObjectPeekMediaObject;
            }
            if (i == 2) {
                return new LocalAlbum(child, this.mApplication, i2, true, str);
            }
            if (i == 4) {
                return new LocalAlbum(child, this.mApplication, i2, false, str);
            }
            if (i == 6) {
                return new LocalMergeAlbum(child, DataManager.sDateTakenComparator, new MediaSet[]{getLocalAlbum(dataManager, 2, PATH_IMAGE, i2, str), getLocalAlbum(dataManager, 4, PATH_VIDEO, i2, str)}, i2);
            }
            throw new IllegalArgumentException(String.valueOf(i));
        }
    }

    @Override
    public synchronized boolean isLoading() {
        return this.mIsLoading;
    }

    @Override
    public synchronized long reload() {
        if (this.mNotifier.isDirty()) {
            if (this.mLoadTask != null) {
                this.mLoadTask.cancel();
            }
            this.mIsLoading = true;
            this.mLoadTask = this.mApplication.getThreadPool().submit(new AlbumsLoader(), this);
        }
        if (this.mLoadBuffer != null) {
            this.mAlbums = this.mLoadBuffer;
            this.mLoadBuffer = null;
            Iterator<MediaSet> it = this.mAlbums.iterator();
            while (it.hasNext()) {
                it.next().reload();
            }
            this.mDataVersion = nextVersionNumber();
        }
        return this.mDataVersion;
    }

    @Override
    public synchronized void onFutureDone(Future<ArrayList<MediaSet>> future) {
        if (this.mLoadTask != future) {
            return;
        }
        this.mLoadBuffer = future.get();
        if (this.mLoadBuffer == null) {
            this.mLoadBuffer = new ArrayList<>();
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                LocalAlbumSet.this.notifyContentChanged();
                LocalAlbumSet.this.mIsLoading = false;
            }
        });
    }

    private static <T> void circularShiftRight(T[] tArr, int i, int i2) {
        T t = tArr[i2];
        while (i2 > i) {
            tArr[i2] = tArr[i2 - 1];
            i2--;
        }
        tArr[i] = t;
    }

    @Override
    public synchronized long reloadForSlideShow() {
        if (this.mNotifier.isDirty()) {
            if (this.mLoadTask != null) {
                this.mLoadTask.cancel();
            }
            this.mIsLoading = true;
            this.mLoadTask = this.mApplication.getThreadPool().submit(new AlbumsLoader(), null);
            this.mLoadBuffer = this.mLoadTask.get();
            this.mIsLoading = false;
        }
        if (this.mLoadBuffer != null) {
            this.mAlbums = this.mLoadBuffer;
            this.mLoadBuffer = null;
            Iterator<MediaSet> it = this.mAlbums.iterator();
            while (it.hasNext()) {
                it.next().reloadForSlideShow();
            }
            this.mDataVersion = nextVersionNumber();
        }
        return this.mDataVersion;
    }

    @Override
    public synchronized long synchronizedAlbumData() {
        if (this.mNotifier.isDirty()) {
            if (this.mLoadTask != null) {
                this.mLoadTask.cancel();
            }
            this.mIsLoading = true;
            this.mLoadTask = this.mApplication.getThreadPool().submit(new AlbumsLoader(), null);
            this.mLoadBuffer = this.mLoadTask.get();
            this.mIsLoading = false;
        }
        if (this.mLoadBuffer != null) {
            this.mAlbums = this.mLoadBuffer;
            this.mLoadBuffer = null;
            Iterator<MediaSet> it = this.mAlbums.iterator();
            while (it.hasNext()) {
                it.next().reload();
            }
            this.mDataVersion = nextVersionNumber();
        }
        return this.mDataVersion;
    }
}
