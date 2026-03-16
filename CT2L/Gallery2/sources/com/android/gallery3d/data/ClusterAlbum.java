package com.android.gallery3d.data;

import com.android.gallery3d.data.MediaSet;
import java.util.ArrayList;

public class ClusterAlbum extends MediaSet implements ContentListener {
    private MediaSet mClusterAlbumSet;
    private MediaItem mCover;
    private DataManager mDataManager;
    private String mName;
    private ArrayList<Path> mPaths;

    public ClusterAlbum(Path path, DataManager dataManager, MediaSet clusterAlbumSet) {
        super(path, nextVersionNumber());
        this.mPaths = new ArrayList<>();
        this.mName = "";
        this.mDataManager = dataManager;
        this.mClusterAlbumSet = clusterAlbumSet;
        this.mClusterAlbumSet.addContentListener(this);
    }

    public void setCoverMediaItem(MediaItem cover) {
        this.mCover = cover;
    }

    @Override
    public MediaItem getCoverMediaItem() {
        return this.mCover != null ? this.mCover : super.getCoverMediaItem();
    }

    void setMediaItems(ArrayList<Path> paths) {
        this.mPaths = paths;
    }

    ArrayList<Path> getMediaItems() {
        return this.mPaths;
    }

    public void setName(String name) {
        this.mName = name;
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
    public ArrayList<MediaItem> getMediaItem(int start, int count) {
        return getMediaItemFromPath(this.mPaths, start, count, this.mDataManager);
    }

    public static ArrayList<MediaItem> getMediaItemFromPath(ArrayList<Path> paths, int start, int count, DataManager dataManager) {
        if (start >= paths.size()) {
            return new ArrayList<>();
        }
        int end = Math.min(start + count, paths.size());
        ArrayList<Path> subset = new ArrayList<>(paths.subList(start, end));
        final MediaItem[] buf = new MediaItem[end - start];
        MediaSet.ItemConsumer consumer = new MediaSet.ItemConsumer() {
            @Override
            public void consume(int index, MediaItem item) {
                buf[index] = item;
            }
        };
        dataManager.mapMediaItems(subset, consumer, 0);
        ArrayList<MediaItem> result = new ArrayList<>(end - start);
        for (MediaItem mediaItem : buf) {
            result.add(mediaItem);
        }
        return result;
    }

    @Override
    protected int enumerateMediaItems(MediaSet.ItemConsumer consumer, int startIndex) {
        this.mDataManager.mapMediaItems(this.mPaths, consumer, startIndex);
        return this.mPaths.size();
    }

    @Override
    public int getTotalMediaItemCount() {
        return this.mPaths.size();
    }

    @Override
    public long reload() {
        if (this.mClusterAlbumSet.reload() > this.mDataVersion) {
            this.mDataVersion = nextVersionNumber();
        }
        return this.mDataVersion;
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
        MediaSet.ItemConsumer consumer = new MediaSet.ItemConsumer() {
            @Override
            public void consume(int index, MediaItem item) {
                if ((item.getSupportedOperations() & 1) != 0) {
                    item.delete();
                }
            }
        };
        this.mDataManager.mapMediaItems(this.mPaths, consumer, 0);
    }

    @Override
    public boolean isLeafAlbum() {
        return true;
    }
}
