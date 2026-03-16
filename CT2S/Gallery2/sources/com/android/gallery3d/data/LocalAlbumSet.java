package com.android.gallery3d.data;

import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.FragmentManagerImpl;
import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.BucketHelper;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.MediaSetUtils;
import com.android.gallery3d.util.ThreadPool;
import java.util.ArrayList;
import java.util.Comparator;

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

    public LocalAlbumSet(Path path, GalleryApp application) {
        super(path, nextVersionNumber());
        this.mAlbums = new ArrayList<>();
        this.mApplication = application;
        this.mHandler = new Handler(application.getMainLooper());
        this.mType = getTypeFromPath(path);
        this.mNotifier = new ChangeNotifier(this, mWatchUris, application);
        this.mName = application.getResources().getString(R.string.set_label_local_albums);
    }

    private static int getTypeFromPath(Path path) {
        String[] name = path.split();
        if (name.length < 2) {
            throw new IllegalArgumentException(path.toString());
        }
        return getTypeFromString(name[1]);
    }

    @Override
    public MediaSet getSubMediaSet(int index) {
        return this.mAlbums.get(index);
    }

    @Override
    public int getSubMediaSetCount() {
        return this.mAlbums.size();
    }

    @Override
    public String getName() {
        return this.mName;
    }

    private static int findBucket(BucketHelper.BucketEntry[] entries, int bucketId) {
        int n = entries.length;
        for (int i = 0; i < n; i++) {
            if (entries[i].bucketId == bucketId) {
                return i;
            }
        }
        return -1;
    }

    private class AlbumsLoader implements ThreadPool.Job<ArrayList<MediaSet>> {
        private AlbumsLoader() {
        }

        @Override
        public ArrayList<MediaSet> run(ThreadPool.JobContext jc) {
            BucketHelper.BucketEntry[] entries = BucketHelper.loadBucketEntries(jc, LocalAlbumSet.this.mApplication.getContentResolver(), LocalAlbumSet.this.mType);
            if (jc.isCancelled()) {
                return null;
            }
            int offset = 0;
            int index = LocalAlbumSet.findBucket(entries, MediaSetUtils.CAMERA_BUCKET_ID);
            if (index != -1) {
                int offset2 = 0 + 1;
                LocalAlbumSet.circularShiftRight(entries, 0, index);
                offset = offset2;
            }
            int index2 = LocalAlbumSet.findBucket(entries, MediaSetUtils.DOWNLOAD_BUCKET_ID);
            if (index2 != -1) {
                int i = offset + 1;
                LocalAlbumSet.circularShiftRight(entries, offset, index2);
            }
            ArrayList<MediaSet> albums = new ArrayList<>();
            DataManager dataManager = LocalAlbumSet.this.mApplication.getDataManager();
            for (BucketHelper.BucketEntry entry : entries) {
                MediaSet album = LocalAlbumSet.this.getLocalAlbum(dataManager, LocalAlbumSet.this.mType, LocalAlbumSet.this.mPath, entry.bucketId, entry.bucketName);
                albums.add(album);
            }
            return albums;
        }
    }

    private MediaSet getLocalAlbum(DataManager manager, int type, Path parent, int id, String name) {
        synchronized (DataManager.LOCK) {
            Path path = parent.getChild(id);
            MediaObject object = manager.peekMediaObject(path);
            if (object != null) {
                return (MediaSet) object;
            }
            switch (type) {
                case 2:
                    return new LocalAlbum(path, this.mApplication, id, true, name);
                case 3:
                case 5:
                default:
                    throw new IllegalArgumentException(String.valueOf(type));
                case 4:
                    return new LocalAlbum(path, this.mApplication, id, false, name);
                case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT:
                    Comparator<MediaItem> comp = DataManager.sDateTakenComparator;
                    return new LocalMergeAlbum(path, comp, new MediaSet[]{getLocalAlbum(manager, 2, PATH_IMAGE, id, name), getLocalAlbum(manager, 4, PATH_VIDEO, id, name)}, id);
            }
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
            for (MediaSet album : this.mAlbums) {
                album.reload();
            }
            this.mDataVersion = nextVersionNumber();
        }
        return this.mDataVersion;
    }

    @Override
    public synchronized void onFutureDone(Future<ArrayList<MediaSet>> future) {
        if (this.mLoadTask == future) {
            this.mLoadBuffer = future.get();
            this.mIsLoading = false;
            if (this.mLoadBuffer == null) {
                this.mLoadBuffer = new ArrayList<>();
            }
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    LocalAlbumSet.this.notifyContentChanged();
                }
            });
        }
    }

    private static <T> void circularShiftRight(T[] array, int i, int j) {
        T temp = array[j];
        for (int k = j; k > i; k--) {
            array[k] = array[k - 1];
        }
        array[i] = temp;
    }
}
