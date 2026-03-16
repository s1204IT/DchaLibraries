package com.android.gallery3d.data;

import android.support.v4.app.NotificationCompat;
import com.android.gallery3d.app.GalleryApp;

class ClusterSource extends MediaSource {
    GalleryApp mApplication;
    PathMatcher mMatcher;

    public ClusterSource(GalleryApp application) {
        super("cluster");
        this.mApplication = application;
        this.mMatcher = new PathMatcher();
        this.mMatcher.add("/cluster/*/time", 0);
        this.mMatcher.add("/cluster/*/location", 1);
        this.mMatcher.add("/cluster/*/tag", 2);
        this.mMatcher.add("/cluster/*/size", 3);
        this.mMatcher.add("/cluster/*/face", 4);
        this.mMatcher.add("/cluster/*/time/*", NotificationCompat.FLAG_LOCAL_ONLY);
        this.mMatcher.add("/cluster/*/location/*", 257);
        this.mMatcher.add("/cluster/*/tag/*", 258);
        this.mMatcher.add("/cluster/*/size/*", 259);
        this.mMatcher.add("/cluster/*/face/*", 260);
    }

    @Override
    public MediaObject createMediaObject(Path path) {
        int matchType = this.mMatcher.match(path);
        String setsName = this.mMatcher.getVar(0);
        DataManager dataManager = this.mApplication.getDataManager();
        MediaSet[] sets = dataManager.getMediaSetsFromString(setsName);
        switch (matchType) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                return new ClusterAlbumSet(path, this.mApplication, sets[0], matchType);
            case NotificationCompat.FLAG_LOCAL_ONLY:
            case 257:
            case 258:
            case 259:
            case 260:
                MediaSet parent = dataManager.getMediaSet(path.getParent());
                return new ClusterAlbum(path, dataManager, parent);
            default:
                throw new RuntimeException("bad path: " + path);
        }
    }
}
