package com.android.gallery3d.data;

import com.android.gallery3d.app.GalleryApp;

class ClusterSource extends MediaSource {
    GalleryApp mApplication;
    PathMatcher mMatcher;

    public ClusterSource(GalleryApp galleryApp) {
        super("cluster");
        this.mApplication = galleryApp;
        this.mMatcher = new PathMatcher();
        this.mMatcher.add("/cluster/*/time", 0);
        this.mMatcher.add("/cluster/*/location", 1);
        this.mMatcher.add("/cluster/*/tag", 2);
        this.mMatcher.add("/cluster/*/size", 3);
        this.mMatcher.add("/cluster/*/face", 4);
        this.mMatcher.add("/cluster/*/time/*", 256);
        this.mMatcher.add("/cluster/*/location/*", 257);
        this.mMatcher.add("/cluster/*/tag/*", 258);
        this.mMatcher.add("/cluster/*/size/*", 259);
        this.mMatcher.add("/cluster/*/face/*", 260);
    }

    @Override
    public MediaObject createMediaObject(Path path) {
        int iMatch = this.mMatcher.match(path);
        String var = this.mMatcher.getVar(0);
        DataManager dataManager = this.mApplication.getDataManager();
        MediaSet[] mediaSetsFromString = dataManager.getMediaSetsFromString(var);
        switch (iMatch) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                return new ClusterAlbumSet(path, this.mApplication, mediaSetsFromString[0], iMatch);
            default:
                switch (iMatch) {
                    case 256:
                    case 257:
                    case 258:
                    case 259:
                    case 260:
                        return new ClusterAlbum(path, dataManager, dataManager.getMediaSet(path.getParent()));
                    default:
                        throw new RuntimeException("bad path: " + path);
                }
        }
    }
}
