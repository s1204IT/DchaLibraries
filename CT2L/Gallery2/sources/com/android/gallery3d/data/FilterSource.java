package com.android.gallery3d.data;

import com.android.gallery3d.app.GalleryApp;

public class FilterSource extends MediaSource {
    private GalleryApp mApplication;
    private MediaItem mCameraShortcutItem;
    private MediaItem mEmptyItem;
    private PathMatcher mMatcher;

    public FilterSource(GalleryApp application) {
        super("filter");
        this.mApplication = application;
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
        int matchType = this.mMatcher.match(path);
        DataManager dataManager = this.mApplication.getDataManager();
        switch (matchType) {
            case 0:
                int mediaType = this.mMatcher.getIntVar(0);
                String setsName = this.mMatcher.getVar(1);
                MediaSet[] sets = dataManager.getMediaSetsFromString(setsName);
                return new FilterTypeSet(path, dataManager, sets[0], mediaType);
            case 1:
                String setsName2 = this.mMatcher.getVar(0);
                MediaSet[] sets2 = dataManager.getMediaSetsFromString(setsName2);
                return new FilterDeleteSet(path, sets2[0]);
            case 2:
                String setsName3 = this.mMatcher.getVar(0);
                MediaSet[] sets3 = dataManager.getMediaSetsFromString(setsName3);
                return new FilterEmptyPromptSet(path, sets3[0], this.mEmptyItem);
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
