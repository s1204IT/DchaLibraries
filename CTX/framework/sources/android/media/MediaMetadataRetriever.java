package android.media;

import android.app.backup.FullBackup;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.IBinder;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;

public class MediaMetadataRetriever {
    private static final int EMBEDDED_PICTURE_TYPE_ANY = 65535;
    public static final int METADATA_KEY_ALBUM = 1;
    public static final int METADATA_KEY_ALBUMARTIST = 13;
    public static final int METADATA_KEY_ARTIST = 2;
    public static final int METADATA_KEY_AUTHOR = 3;
    public static final int METADATA_KEY_BITRATE = 20;
    public static final int METADATA_KEY_CAPTURE_FRAMERATE = 25;
    public static final int METADATA_KEY_CD_TRACK_NUMBER = 0;
    public static final int METADATA_KEY_COMPILATION = 15;
    public static final int METADATA_KEY_COMPOSER = 4;
    public static final int METADATA_KEY_DATE = 5;
    public static final int METADATA_KEY_DISC_NUMBER = 14;
    public static final int METADATA_KEY_DURATION = 9;
    public static final int METADATA_KEY_EXIF_LENGTH = 34;
    public static final int METADATA_KEY_EXIF_OFFSET = 33;
    public static final int METADATA_KEY_GENRE = 6;
    public static final int METADATA_KEY_HAS_AUDIO = 16;
    public static final int METADATA_KEY_HAS_IMAGE = 26;
    public static final int METADATA_KEY_HAS_VIDEO = 17;
    public static final int METADATA_KEY_IMAGE_COUNT = 27;
    public static final int METADATA_KEY_IMAGE_HEIGHT = 30;
    public static final int METADATA_KEY_IMAGE_PRIMARY = 28;
    public static final int METADATA_KEY_IMAGE_ROTATION = 31;
    public static final int METADATA_KEY_IMAGE_WIDTH = 29;
    public static final int METADATA_KEY_IS_DRM = 22;
    public static final int METADATA_KEY_LOCATION = 23;
    public static final int METADATA_KEY_MIMETYPE = 12;
    public static final int METADATA_KEY_NUM_TRACKS = 10;
    public static final int METADATA_KEY_TIMED_TEXT_LANGUAGES = 21;
    public static final int METADATA_KEY_TITLE = 7;
    public static final int METADATA_KEY_VIDEO_FRAME_COUNT = 32;
    public static final int METADATA_KEY_VIDEO_HEIGHT = 19;
    public static final int METADATA_KEY_VIDEO_ROTATION = 24;
    public static final int METADATA_KEY_VIDEO_WIDTH = 18;
    public static final int METADATA_KEY_WRITER = 11;
    public static final int METADATA_KEY_YEAR = 8;
    public static final int OPTION_CLOSEST = 3;
    public static final int OPTION_CLOSEST_SYNC = 2;
    public static final int OPTION_NEXT_SYNC = 1;
    public static final int OPTION_PREVIOUS_SYNC = 0;
    private long mNativeContext;

    @Retention(RetentionPolicy.SOURCE)
    public @interface Option {
    }

    private native List<Bitmap> _getFrameAtIndex(int i, int i2, BitmapParams bitmapParams);

    private native Bitmap _getFrameAtTime(long j, int i, int i2, int i3);

    private native Bitmap _getImageAtIndex(int i, BitmapParams bitmapParams);

    private native void _setDataSource(MediaDataSource mediaDataSource) throws IllegalArgumentException;

    private native void _setDataSource(IBinder iBinder, String str, String[] strArr, String[] strArr2) throws IllegalArgumentException;

    private native byte[] getEmbeddedPicture(int i);

    private final native void native_finalize();

    private static native void native_init();

    private native void native_setup();

    public native String extractMetadata(int i);

    public native Bitmap getThumbnailImageAtIndex(int i, BitmapParams bitmapParams, int i2, int i3);

    public native void release();

    public native void setDataSource(FileDescriptor fileDescriptor, long j, long j2) throws IllegalArgumentException;

    static {
        System.loadLibrary("media_jni");
        native_init();
    }

    public MediaMetadataRetriever() {
        native_setup();
    }

    public void setDataSource(String str) throws IllegalArgumentException {
        if (str == null) {
            throw new IllegalArgumentException();
        }
        try {
            FileInputStream fileInputStream = new FileInputStream(str);
            Throwable th = null;
            try {
                setDataSource(fileInputStream.getFD(), 0L, DataSourceDesc.LONG_MAX);
                fileInputStream.close();
            } catch (Throwable th2) {
                if (th != null) {
                    try {
                        fileInputStream.close();
                    } catch (Throwable th3) {
                        th.addSuppressed(th3);
                    }
                } else {
                    fileInputStream.close();
                }
                throw th2;
            }
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException();
        } catch (IOException e2) {
            throw new IllegalArgumentException();
        }
    }

    public void setDataSource(String str, Map<String, String> map) throws IllegalArgumentException {
        String[] strArr = new String[map.size()];
        String[] strArr2 = new String[map.size()];
        int i = 0;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            strArr[i] = entry.getKey();
            strArr2[i] = entry.getValue();
            i++;
        }
        _setDataSource(MediaHTTPService.createHttpServiceBinderIfNecessary(str), str, strArr, strArr2);
    }

    public void setDataSource(FileDescriptor fileDescriptor) throws IllegalArgumentException {
        setDataSource(fileDescriptor, 0L, DataSourceDesc.LONG_MAX);
    }

    public void setDataSource(Context context, Uri uri) throws Throwable {
        AssetFileDescriptor assetFileDescriptorOpenAssetFileDescriptor;
        if (uri == null) {
            throw new IllegalArgumentException();
        }
        String scheme = uri.getScheme();
        if (scheme == null || scheme.equals(ContentResolver.SCHEME_FILE)) {
            setDataSource(uri.getPath());
            return;
        }
        try {
            try {
                assetFileDescriptorOpenAssetFileDescriptor = context.getContentResolver().openAssetFileDescriptor(uri, FullBackup.ROOT_TREE_TOKEN);
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException();
            }
        } catch (SecurityException e2) {
            assetFileDescriptorOpenAssetFileDescriptor = null;
        } catch (Throwable th) {
            th = th;
            assetFileDescriptorOpenAssetFileDescriptor = null;
        }
        try {
            if (assetFileDescriptorOpenAssetFileDescriptor == null) {
                throw new IllegalArgumentException();
            }
            FileDescriptor fileDescriptor = assetFileDescriptorOpenAssetFileDescriptor.getFileDescriptor();
            if (!fileDescriptor.valid()) {
                throw new IllegalArgumentException();
            }
            if (assetFileDescriptorOpenAssetFileDescriptor.getDeclaredLength() < 0) {
                setDataSource(fileDescriptor);
            } else {
                setDataSource(fileDescriptor, assetFileDescriptorOpenAssetFileDescriptor.getStartOffset(), assetFileDescriptorOpenAssetFileDescriptor.getDeclaredLength());
            }
            if (assetFileDescriptorOpenAssetFileDescriptor == null) {
                return;
            }
            try {
                assetFileDescriptorOpenAssetFileDescriptor.close();
            } catch (IOException e3) {
            }
        } catch (SecurityException e4) {
            if (assetFileDescriptorOpenAssetFileDescriptor != null) {
                try {
                    assetFileDescriptorOpenAssetFileDescriptor.close();
                } catch (IOException e5) {
                }
            }
            setDataSource(uri.toString());
        } catch (Throwable th2) {
            th = th2;
            if (assetFileDescriptorOpenAssetFileDescriptor != null) {
                try {
                    assetFileDescriptorOpenAssetFileDescriptor.close();
                } catch (IOException e6) {
                }
            }
            throw th;
        }
    }

    public void setDataSource(MediaDataSource mediaDataSource) throws IllegalArgumentException {
        _setDataSource(mediaDataSource);
    }

    public Bitmap getFrameAtTime(long j, int i) {
        if (i < 0 || i > 3) {
            throw new IllegalArgumentException("Unsupported option: " + i);
        }
        return _getFrameAtTime(j, i, -1, -1);
    }

    public Bitmap getScaledFrameAtTime(long j, int i, int i2, int i3) {
        if (i < 0 || i > 3) {
            throw new IllegalArgumentException("Unsupported option: " + i);
        }
        if (i2 <= 0) {
            throw new IllegalArgumentException("Invalid width: " + i2);
        }
        if (i3 <= 0) {
            throw new IllegalArgumentException("Invalid height: " + i3);
        }
        return _getFrameAtTime(j, i, i2, i3);
    }

    public Bitmap getFrameAtTime(long j) {
        return getFrameAtTime(j, 2);
    }

    public Bitmap getFrameAtTime() {
        return _getFrameAtTime(-1L, 2, -1, -1);
    }

    public static final class BitmapParams {
        private Bitmap.Config inPreferredConfig = Bitmap.Config.ARGB_8888;
        private Bitmap.Config outActualConfig = Bitmap.Config.ARGB_8888;

        public void setPreferredConfig(Bitmap.Config config) {
            if (config == null) {
                throw new IllegalArgumentException("preferred config can't be null");
            }
            this.inPreferredConfig = config;
        }

        public Bitmap.Config getPreferredConfig() {
            return this.inPreferredConfig;
        }

        public Bitmap.Config getActualConfig() {
            return this.outActualConfig;
        }
    }

    public Bitmap getFrameAtIndex(int i, BitmapParams bitmapParams) {
        return getFramesAtIndex(i, 1, bitmapParams).get(0);
    }

    public Bitmap getFrameAtIndex(int i) {
        return getFramesAtIndex(i, 1).get(0);
    }

    public List<Bitmap> getFramesAtIndex(int i, int i2, BitmapParams bitmapParams) {
        return getFramesAtIndexInternal(i, i2, bitmapParams);
    }

    public List<Bitmap> getFramesAtIndex(int i, int i2) {
        return getFramesAtIndexInternal(i, i2, null);
    }

    private List<Bitmap> getFramesAtIndexInternal(int i, int i2, BitmapParams bitmapParams) {
        if (!"yes".equals(extractMetadata(17))) {
            throw new IllegalStateException("Does not contail video or image sequences");
        }
        int i3 = Integer.parseInt(extractMetadata(32));
        if (i < 0 || i2 < 1 || i >= i3 || i > i3 - i2) {
            throw new IllegalArgumentException("Invalid frameIndex or numFrames: " + i + ", " + i2);
        }
        return _getFrameAtIndex(i, i2, bitmapParams);
    }

    public Bitmap getImageAtIndex(int i, BitmapParams bitmapParams) {
        return getImageAtIndexInternal(i, bitmapParams);
    }

    public Bitmap getImageAtIndex(int i) {
        return getImageAtIndexInternal(i, null);
    }

    public Bitmap getPrimaryImage(BitmapParams bitmapParams) {
        return getImageAtIndexInternal(-1, bitmapParams);
    }

    public Bitmap getPrimaryImage() {
        return getImageAtIndexInternal(-1, null);
    }

    private Bitmap getImageAtIndexInternal(int i, BitmapParams bitmapParams) {
        if (!"yes".equals(extractMetadata(26))) {
            throw new IllegalStateException("Does not contail still images");
        }
        String strExtractMetadata = extractMetadata(27);
        if (i >= Integer.parseInt(strExtractMetadata)) {
            throw new IllegalArgumentException("Invalid image index: " + strExtractMetadata);
        }
        return _getImageAtIndex(i, bitmapParams);
    }

    public byte[] getEmbeddedPicture() {
        return getEmbeddedPicture(65535);
    }

    protected void finalize() throws Throwable {
        try {
            native_finalize();
        } finally {
            super.finalize();
        }
    }
}
