package com.android.gallery3d.data;

import com.android.gallery3d.app.GalleryApp;

public class SnailSource extends MediaSource {
    private static int sNextId;
    private GalleryApp mApplication;
    private PathMatcher mMatcher;

    public SnailSource(GalleryApp galleryApp) {
        super("snail");
        this.mApplication = galleryApp;
        this.mMatcher = new PathMatcher();
        this.mMatcher.add("/snail/set/*", 0);
        this.mMatcher.add("/snail/item/*", 1);
    }

    @Override
    public MediaObject createMediaObject(Path path) {
        DataManager dataManager = this.mApplication.getDataManager();
        switch (this.mMatcher.match(path)) {
            case 0:
                return new SnailAlbum(path, (SnailItem) dataManager.getMediaObject("/snail/item/" + this.mMatcher.getVar(0)));
            case 1:
                this.mMatcher.getIntVar(0);
                return new SnailItem(path);
            default:
                return null;
        }
    }

    public static synchronized int newId() {
        int i;
        i = sNextId;
        sNextId = i + 1;
        return i;
    }

    public static Path getSetPath(int i) {
        return Path.fromString("/snail/set").getChild(i);
    }

    public static Path getItemPath(int i) {
        return Path.fromString("/snail/item").getChild(i);
    }
}
