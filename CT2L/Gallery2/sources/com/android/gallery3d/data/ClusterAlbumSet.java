package com.android.gallery3d.data;

import android.content.Context;
import android.net.Uri;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.MediaSet;
import java.util.ArrayList;
import java.util.HashSet;

public class ClusterAlbumSet extends MediaSet implements ContentListener {
    private ArrayList<ClusterAlbum> mAlbums;
    private GalleryApp mApplication;
    private MediaSet mBaseSet;
    private boolean mFirstReloadDone;
    private int mKind;

    public ClusterAlbumSet(Path path, GalleryApp application, MediaSet baseSet, int kind) {
        super(path, -1L);
        this.mAlbums = new ArrayList<>();
        this.mApplication = application;
        this.mBaseSet = baseSet;
        this.mKind = kind;
        baseSet.addContentListener(this);
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
        return this.mBaseSet.getName();
    }

    @Override
    public long reload() {
        if (this.mBaseSet.reload() > this.mDataVersion) {
            if (this.mFirstReloadDone) {
                updateClustersContents();
            } else {
                updateClusters();
                this.mFirstReloadDone = true;
            }
            this.mDataVersion = nextVersionNumber();
        }
        return this.mDataVersion;
    }

    @Override
    public void onContentDirty() {
        notifyContentChanged();
    }

    private void updateClusters() {
        Clustering clustering;
        Path childPath;
        ClusterAlbum album;
        this.mAlbums.clear();
        Context context = this.mApplication.getAndroidContext();
        switch (this.mKind) {
            case 0:
                clustering = new TimeClustering(context);
                break;
            case 1:
                clustering = new LocationClustering(context);
                break;
            case 2:
                clustering = new TagClustering(context);
                break;
            case 3:
            default:
                clustering = new SizeClustering(context);
                break;
            case 4:
                clustering = new FaceClustering(context);
                break;
        }
        clustering.run(this.mBaseSet);
        int n = clustering.getNumberOfClusters();
        DataManager dataManager = this.mApplication.getDataManager();
        for (int i = 0; i < n; i++) {
            String childName = clustering.getClusterName(i);
            if (this.mKind == 2) {
                childPath = this.mPath.getChild(Uri.encode(childName));
            } else if (this.mKind == 3) {
                long minSize = ((SizeClustering) clustering).getMinSize(i);
                childPath = this.mPath.getChild(minSize);
            } else {
                childPath = this.mPath.getChild(i);
            }
            synchronized (DataManager.LOCK) {
                album = (ClusterAlbum) dataManager.peekMediaObject(childPath);
                if (album == null) {
                    album = new ClusterAlbum(childPath, dataManager, this);
                }
            }
            album.setMediaItems(clustering.getCluster(i));
            album.setName(childName);
            album.setCoverMediaItem(clustering.getClusterCover(i));
            this.mAlbums.add(album);
        }
    }

    private void updateClustersContents() {
        final HashSet<Path> existing = new HashSet<>();
        this.mBaseSet.enumerateTotalMediaItems(new MediaSet.ItemConsumer() {
            @Override
            public void consume(int index, MediaItem item) {
                existing.add(item.getPath());
            }
        });
        int n = this.mAlbums.size();
        for (int i = n - 1; i >= 0; i--) {
            ArrayList<Path> oldPaths = this.mAlbums.get(i).getMediaItems();
            ArrayList<Path> newPaths = new ArrayList<>();
            int m = oldPaths.size();
            for (int j = 0; j < m; j++) {
                Path p = oldPaths.get(j);
                if (existing.contains(p)) {
                    newPaths.add(p);
                }
            }
            this.mAlbums.get(i).setMediaItems(newPaths);
            if (newPaths.isEmpty()) {
                this.mAlbums.remove(i);
            }
        }
    }
}
