package com.android.gallery3d.data;

import com.android.gallery3d.app.GalleryApp;

public class SecureSource extends MediaSource {
    private static PathMatcher mMatcher = new PathMatcher();
    private GalleryApp mApplication;

    static {
        mMatcher.add("/secure/all/*", 0);
        mMatcher.add("/secure/unlock", 1);
    }

    public SecureSource(GalleryApp galleryApp) {
        super("secure");
        this.mApplication = galleryApp;
    }

    public static boolean isSecurePath(String str) {
        return mMatcher.match(Path.fromString(str)) == 0;
    }

    @Override
    public MediaObject createMediaObject(Path path) {
        switch (mMatcher.match(path)) {
            case 0:
                return new SecureAlbum(path, this.mApplication, (MediaItem) this.mApplication.getDataManager().getMediaObject("/secure/unlock"));
            case 1:
                return new UnlockImage(path, this.mApplication);
            default:
                throw new RuntimeException("bad path: " + path);
        }
    }
}
