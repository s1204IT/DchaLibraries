package com.android.gallery3d.picasasource;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.ParcelFileDescriptor;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.MediaSource;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.data.PathMatcher;
import java.io.FileNotFoundException;

public class PicasaSource extends MediaSource {
    public static final Path ALBUM_PATH = Path.fromString("/picasa/all");
    private GalleryApp mApplication;
    private PathMatcher mMatcher;

    public PicasaSource(GalleryApp application) {
        super("picasa");
        this.mApplication = application;
        this.mMatcher = new PathMatcher();
        this.mMatcher.add("/picasa/all", 0);
        this.mMatcher.add("/picasa/image", 0);
        this.mMatcher.add("/picasa/video", 0);
    }

    private static class EmptyAlbumSet extends MediaSet {
        public EmptyAlbumSet(Path path, long version) {
            super(path, version);
        }

        @Override
        public String getName() {
            return "picasa";
        }

        @Override
        public long reload() {
            return this.mDataVersion;
        }
    }

    @Override
    public MediaObject createMediaObject(Path path) {
        switch (this.mMatcher.match(path)) {
            case 0:
                return new EmptyAlbumSet(path, MediaObject.nextVersionNumber());
            default:
                throw new RuntimeException("bad path: " + path);
        }
    }

    public static MediaItem getFaceItem(Context context, MediaItem item, int faceIndex) {
        throw new UnsupportedOperationException();
    }

    public static boolean isPicasaImage(MediaObject object) {
        return false;
    }

    public static String getImageTitle(MediaObject image) {
        throw new UnsupportedOperationException();
    }

    public static int getImageSize(MediaObject image) {
        throw new UnsupportedOperationException();
    }

    public static String getContentType(MediaObject image) {
        throw new UnsupportedOperationException();
    }

    public static long getDateTaken(MediaObject image) {
        throw new UnsupportedOperationException();
    }

    public static double getLatitude(MediaObject image) {
        throw new UnsupportedOperationException();
    }

    public static double getLongitude(MediaObject image) {
        throw new UnsupportedOperationException();
    }

    public static int getRotation(MediaObject image) {
        throw new UnsupportedOperationException();
    }

    public static long getPicasaId(MediaObject image) {
        throw new UnsupportedOperationException();
    }

    public static String getUserAccount(Context context, MediaObject image) {
        throw new UnsupportedOperationException();
    }

    public static ParcelFileDescriptor openFile(Context context, MediaObject image, String mode) throws FileNotFoundException {
        throw new UnsupportedOperationException();
    }

    public static void initialize(Context context) {
    }

    public static void requestSync(Context context) {
    }

    public static void showSignInReminder(Activity context) {
    }

    public static void onPackageAdded(Context context, String packageName) {
    }

    public static void onPackageRemoved(Context context, String packageName) {
    }

    public static void onPackageChanged(Context context, String packageName) {
    }

    public static Dialog getVersionCheckDialog(Activity activity) {
        return null;
    }
}
