package com.android.gallery3d.data;

import com.android.gallery3d.app.GalleryApp;

class ComboSource extends MediaSource {
    private GalleryApp mApplication;
    private PathMatcher mMatcher;

    public ComboSource(GalleryApp galleryApp) {
        super("combo");
        this.mApplication = galleryApp;
        this.mMatcher = new PathMatcher();
        this.mMatcher.add("/combo/*", 0);
        this.mMatcher.add("/combo/*/*", 1);
    }

    @Override
    public MediaObject createMediaObject(Path path) {
        String[] strArrSplit = path.split();
        if (strArrSplit.length < 2) {
            throw new RuntimeException("bad path: " + path);
        }
        DataManager dataManager = this.mApplication.getDataManager();
        switch (this.mMatcher.match(path)) {
            case 0:
                return new ComboAlbumSet(path, this.mApplication, dataManager.getMediaSetsFromString(strArrSplit[1]));
            case 1:
                return new ComboAlbum(path, dataManager.getMediaSetsFromString(strArrSplit[2]), strArrSplit[1]);
            default:
                return null;
        }
    }
}
