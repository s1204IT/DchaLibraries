package com.android.gallery3d.data;

import com.android.gallery3d.app.GalleryApp;

public class FilterSource extends MediaSource {
    private GalleryApp mApplication;
    private MediaItem mCameraShortcutItem;
    private MediaItem mEmptyItem;
    private PathMatcher mMatcher;

    public FilterSource(GalleryApp galleryApp) {
        super("filter");
        this.mApplication = galleryApp;
        this.mMatcher = new PathMatcher();
        this.mMatcher.add("/filter/mediatype/*/*", 0);
        this.mMatcher.add("/filter/delete/*", 1);
        this.mMatcher.add("/filter/empty/*", 2);
        this.mMatcher.add("/filter/empty_prompt", 3);
        this.mMatcher.add("/filter/camera_shortcut", 4);
        this.mMatcher.add("/filter/camera_shortcut_item", 5);
        this.mEmptyItem = new EmptyAlbumImage(Path.fromString("/filter/empty_prompt"), this.mApplication);
        this.mCameraShortcutItem = new CameraShortcutImage(Path.fromString("/filter/camera_shortcut_item"), this.mApplication);
    }

    @Override
    public MediaObject createMediaObject(Path path) {
        int iMatch = this.mMatcher.match(path);
        DataManager dataManager = this.mApplication.getDataManager();
        switch (iMatch) {
            case 0:
                return new FilterTypeSet(path, dataManager, dataManager.getMediaSetsFromString(this.mMatcher.getVar(1))[0], this.mMatcher.getIntVar(0));
            case 1:
                return new FilterDeleteSet(path, dataManager.getMediaSetsFromString(this.mMatcher.getVar(0))[0]);
            case 2:
                return new FilterEmptyPromptSet(path, dataManager.getMediaSetsFromString(this.mMatcher.getVar(0))[0], this.mEmptyItem);
            case 3:
                return this.mEmptyItem;
            case 4:
                return new SingleItemAlbum(path, this.mCameraShortcutItem);
            case 5:
                return this.mCameraShortcutItem;
            default:
                throw new RuntimeException("bad path: " + path);
        }
    }
}
