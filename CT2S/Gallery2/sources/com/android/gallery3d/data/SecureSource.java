package com.android.gallery3d.data;

import com.android.gallery3d.app.GalleryApp;

public class SecureSource extends MediaSource {
    private static PathMatcher mMatcher = new PathMatcher();
    private GalleryApp mApplication;

    static {
        mMatcher.add("/secure/all/*", 0);
        mMatcher.add("/secure/unlock", 1);
    }

    public SecureSource(GalleryApp context) {
        super("secure");
        this.mApplication = context;
    }

    public static boolean isSecurePath(String path) {
        return mMatcher.match(Path.fromString(path)) == 0;
    }

    @Override
    public MediaObject createMediaObject(Path path) {
        switch (mMatcher.match(path)) {
            case 0:
                DataManager dataManager = this.mApplication.getDataManager();
                MediaItem unlock = (MediaItem) dataManager.getMediaObject("/secure/unlock");
                return new SecureAlbum(path, this.mApplication, unlock);
            case 1:
                return new UnlockImage(path, this.mApplication);
            default:
                throw new RuntimeException("bad path: " + path);
        }
    }
}
