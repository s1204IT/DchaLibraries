package com.android.gallery3d.data;

import com.android.gallery3d.app.GalleryApp;

public class SnailSource extends MediaSource {
    private static int sNextId;
    private GalleryApp mApplication;
    private PathMatcher mMatcher;

    public SnailSource(GalleryApp application) {
        super("snail");
        this.mApplication = application;
        this.mMatcher = new PathMatcher();
        this.mMatcher.add("/snail/set/*", 0);
        this.mMatcher.add("/snail/item/*", 1);
    }

    @Override
    public MediaObject createMediaObject(Path path) {
        DataManager dataManager = this.mApplication.getDataManager();
        switch (this.mMatcher.match(path)) {
            case 0:
                String itemPath = "/snail/item/" + this.mMatcher.getVar(0);
                SnailItem item = (SnailItem) dataManager.getMediaObject(itemPath);
                return new SnailAlbum(path, item);
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

    public static Path getSetPath(int id) {
        return Path.fromString("/snail/set").getChild(id);
    }

    public static Path getItemPath(int id) {
        return Path.fromString("/snail/item").getChild(id);
    }
}
