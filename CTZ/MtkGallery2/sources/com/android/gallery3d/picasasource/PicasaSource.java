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

    public PicasaSource(GalleryApp galleryApp) {
        super("picasa");
        this.mApplication = galleryApp;
        this.mMatcher = new PathMatcher();
        this.mMatcher.add("/picasa/all", 0);
        this.mMatcher.add("/picasa/image", 0);
        this.mMatcher.add("/picasa/video", 0);
    }

    private static class EmptyAlbumSet extends MediaSet {
        public EmptyAlbumSet(Path path, long j) {
            super(path, j);
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
        if (this.mMatcher.match(path) == 0) {
            return new EmptyAlbumSet(path, MediaObject.nextVersionNumber());
        }
        throw new RuntimeException("bad path: " + path);
    }

    public static MediaItem getFaceItem(Context context, MediaItem mediaItem, int i) {
        throw new UnsupportedOperationException();
    }

    public static boolean isPicasaImage(MediaObject mediaObject) {
        return false;
    }

    public static String getImageTitle(MediaObject mediaObject) {
        throw new UnsupportedOperationException();
    }

    public static int getImageSize(MediaObject mediaObject) {
        throw new UnsupportedOperationException();
    }

    public static String getContentType(MediaObject mediaObject) {
        throw new UnsupportedOperationException();
    }

    public static long getDateTaken(MediaObject mediaObject) {
        throw new UnsupportedOperationException();
    }

    public static double getLatitude(MediaObject mediaObject) {
        throw new UnsupportedOperationException();
    }

    public static double getLongitude(MediaObject mediaObject) {
        throw new UnsupportedOperationException();
    }

    public static int getRotation(MediaObject mediaObject) {
        throw new UnsupportedOperationException();
    }

    public static long getPicasaId(MediaObject mediaObject) {
        throw new UnsupportedOperationException();
    }

    public static String getUserAccount(Context context, MediaObject mediaObject) {
        throw new UnsupportedOperationException();
    }

    public static ParcelFileDescriptor openFile(Context context, MediaObject mediaObject, String str) throws FileNotFoundException {
        throw new UnsupportedOperationException();
    }

    public static void initialize(Context context) {
    }

    public static void showSignInReminder(Activity activity) {
    }

    public static void onPackageAdded(Context context, String str) {
    }

    public static void onPackageRemoved(Context context, String str) {
    }

    public static void onPackageChanged(Context context, String str) {
    }

    public static Dialog getVersionCheckDialog(Activity activity) {
        return null;
    }
}
