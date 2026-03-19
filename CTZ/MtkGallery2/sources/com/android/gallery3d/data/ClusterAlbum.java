package com.android.gallery3d.data;

import com.android.gallery3d.data.MediaSet;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashSet;

public class ClusterAlbum extends MediaSet implements ContentListener {
    private MediaSet mClusterAlbumSet;
    private MediaItem mCover;
    private MediaItem mCoverBackUp;
    private DataManager mDataManager;
    private String mName;
    private int mNumberOfDeletedImage;
    private final HashSet<Path> mPathSet;
    private ArrayList<Path> mPaths;
    private boolean mStopReload;

    static int access$008(ClusterAlbum clusterAlbum) {
        int i = clusterAlbum.mNumberOfDeletedImage;
        clusterAlbum.mNumberOfDeletedImage = i + 1;
        return i;
    }

    public ClusterAlbum(Path path, DataManager dataManager, MediaSet mediaSet) {
        super(path, nextVersionNumber());
        this.mPaths = new ArrayList<>();
        this.mName = "";
        this.mPathSet = new HashSet<>();
        this.mStopReload = false;
        this.mDataManager = dataManager;
        this.mClusterAlbumSet = mediaSet;
        this.mClusterAlbumSet.addContentListener(this);
    }

    public void setCoverMediaItem(MediaItem mediaItem) {
        this.mCover = mediaItem;
    }

    void setMediaItems(ArrayList<Path> arrayList) {
        this.mPaths = arrayList;
    }

    void addMediaItems(Path path, int i) {
        if (path != null) {
            this.mPaths.add(i, path);
            nextVersion();
        }
    }

    ArrayList<Path> getMediaItems() {
        return this.mPaths;
    }

    public void setName(String str) {
        this.mName = str;
    }

    @Override
    public String getName() {
        return this.mName;
    }

    @Override
    public int getMediaItemCount() {
        return this.mPaths.size();
    }

    @Override
    public ArrayList<MediaItem> getMediaItem(int i, int i2) {
        return getMediaItemFromPath(this.mPaths, i, i2, this.mDataManager);
    }

    public static ArrayList<MediaItem> getMediaItemFromPath(ArrayList<Path> arrayList, int i, int i2, DataManager dataManager) {
        if (i >= arrayList.size()) {
            return new ArrayList<>();
        }
        int iMin = Math.min(i2 + i, arrayList.size());
        ArrayList<Path> arrayList2 = new ArrayList<>(arrayList.subList(i, iMin));
        int i3 = iMin - i;
        final MediaItem[] mediaItemArr = new MediaItem[i3];
        dataManager.mapMediaItems(arrayList2, new MediaSet.ItemConsumer() {
            @Override
            public void consume(int i4, MediaItem mediaItem) {
                mediaItemArr[i4] = mediaItem;
            }

            @Override
            public boolean stopConsume() {
                return false;
            }
        }, 0);
        ArrayList<MediaItem> arrayList3 = new ArrayList<>(i3);
        for (int i4 = 0; i4 < mediaItemArr.length; i4++) {
            if (mediaItemArr[i4] != null) {
                arrayList3.add(mediaItemArr[i4]);
            }
        }
        return arrayList3;
    }

    @Override
    protected int enumerateMediaItems(MediaSet.ItemConsumer itemConsumer, int i) {
        this.mDataManager.mapMediaItems(this.mPaths, itemConsumer, i);
        return this.mPaths.size();
    }

    @Override
    public int getTotalMediaItemCount() {
        return this.mPaths.size() - this.mNumberOfDeletedImage;
    }

    @Override
    public void onContentDirty() {
        notifyContentChanged();
    }

    @Override
    public int getSupportedOperations() {
        return 1029;
    }

    @Override
    public void delete() {
        this.mDataManager.mapMediaItems(this.mPaths, new MediaSet.ItemConsumer() {
            @Override
            public void consume(int i, MediaItem mediaItem) {
                if ((mediaItem.getSupportedOperations() & 1) != 0) {
                    mediaItem.delete();
                    ClusterAlbum.access$008(ClusterAlbum.this);
                }
            }

            @Override
            public boolean stopConsume() {
                return false;
            }
        }, 0);
    }

    @Override
    public boolean isLeafAlbum() {
        return true;
    }

    @Override
    public synchronized long reload() {
        Log.d("Gallery2/ClusterAlbum", "<reload>");
        this.mClusterAlbumSet.mCurrentClusterAlbum = this;
        this.mClusterAlbumSet.offsetInStack = this.offsetInStack + 1;
        this.mStopReload = false;
        if (this.mClusterAlbumSet.reload() > this.mDataVersion) {
            if (!this.mStopReload) {
                this.mDataVersion = nextVersionNumber();
            }
            Log.d("Gallery2/ClusterAlbum", " <reload>mClusterAlbumSet.synchronizedAlbumData() > mDataVersion");
        }
        this.offsetInStack = 0;
        return this.mDataVersion;
    }

    public long nextVersion() {
        this.mDataVersion = nextVersionNumber();
        return this.mDataVersion;
    }

    @Override
    public MediaItem getCoverMediaItem() {
        if (this.mCover != null) {
            return this.mCover;
        }
        try {
            this.mCoverBackUp = super.getCoverMediaItem();
        } catch (ConcurrentModificationException e) {
            e.printStackTrace();
        }
        return this.mCoverBackUp;
    }

    public void setNumberOfDeletedImage(int i) {
        this.mNumberOfDeletedImage = i;
    }

    public int getNumberOfDeletedImage() {
        return this.mNumberOfDeletedImage;
    }

    public void pathSet() {
        if (this.mPathSet == null) {
            return;
        }
        this.mPathSet.clear();
        this.mPathSet.addAll(this.mPaths);
    }

    public HashSet<Path> getPathSet() {
        return this.mPathSet;
    }

    @Override
    public void stopReload() {
        Log.d("Gallery2/ClusterAlbum", "<stopReload> ......");
        this.mStopReload = true;
        if (this.mClusterAlbumSet != null) {
            this.mClusterAlbumSet.stopReload();
        }
    }
}
