package com.mediatek.galleryportable;

import android.media.MediaPlayer;
import android.media.Metadata;
import android.os.SystemClock;

public class VideoMetadataUtils {
    private static boolean sIsMetadataSupported = false;
    private static boolean sHasChecked = false;

    private static boolean isMetadataSupported() {
        if (!sHasChecked) {
            try {
                VideoMetadataUtils.class.getClassLoader().loadClass("android.media.Metadata");
                sIsMetadataSupported = true;
            } catch (ClassNotFoundException e) {
                sIsMetadataSupported = false;
            }
            sHasChecked = true;
            Log.d("VP_VideoMetadataUtils", "isMetadataSupported = " + sIsMetadataSupported);
        }
        return sIsMetadataSupported;
    }

    public static boolean canPause(MediaPlayer mp) {
        boolean canPause = true;
        if (isMetadataSupported()) {
            long checkStart = SystemClock.elapsedRealtime();
            boolean z = false;
            Metadata data = mp.getMetadata(false, false);
            if (data != null) {
                if (!data.has(1) || data.getBoolean(1)) {
                    z = true;
                }
                canPause = z;
                Log.d("VP_VideoMetadataUtils", "canPause, data.has(Metadata.PAUSE_AVAILABLE) = " + data.has(1) + ", data.getBoolean(Metadata.PAUSE_AVAILABLE) = " + data.getBoolean(1));
            } else {
                canPause = true;
            }
            Log.d("VP_VideoMetadataUtils", "canPause, getMetadata elapsed time = " + (SystemClock.elapsedRealtime() - checkStart));
        }
        return canPause;
    }

    public static boolean canSeekBack(MediaPlayer mp) {
        boolean canSeekBack = true;
        if (isMetadataSupported()) {
            long checkStart = SystemClock.elapsedRealtime();
            Metadata data = mp.getMetadata(false, false);
            if (data != null) {
                canSeekBack = !data.has(2) || data.getBoolean(2);
                Log.d("VP_VideoMetadataUtils", "canSeekBack, data.has(Metadata.SEEK_BACKWARD_AVAILABLE) = " + data.has(2) + ", data.getBoolean(Metadata.SEEK_BACKWARD_AVAILABLE) = " + data.getBoolean(2));
            } else {
                canSeekBack = true;
            }
            Log.d("VP_VideoMetadataUtils", "canSeekBack, getMetadata elapsed time = " + (SystemClock.elapsedRealtime() - checkStart));
        }
        return canSeekBack;
    }

    public static boolean canSeekForward(MediaPlayer mp) {
        boolean canSeekForward = true;
        if (isMetadataSupported()) {
            long checkStart = SystemClock.elapsedRealtime();
            Metadata data = mp.getMetadata(false, false);
            if (data != null) {
                canSeekForward = !data.has(3) || data.getBoolean(3);
                Log.d("VP_VideoMetadataUtils", "canSeekBack, data.has(Metadata.SEEK_FORWARD_AVAILABLE) = " + data.has(3) + ", data.getBoolean(Metadata.SEEK_FORWARD_AVAILABLE) = " + data.getBoolean(3));
            } else {
                canSeekForward = true;
            }
            Log.d("VP_VideoMetadataUtils", "canSeekForward, getMetadata elapsed time = " + (SystemClock.elapsedRealtime() - checkStart));
        }
        return canSeekForward;
    }

    public static byte[] getAlbumArt(MediaPlayer mp) {
        byte[] albumArt = null;
        if (isMetadataSupported()) {
            long checkStart = SystemClock.elapsedRealtime();
            Metadata data = mp.getMetadata(false, false);
            if (data != null && data.has(18)) {
                albumArt = data.getByteArray(18);
            }
            Log.d("VP_VideoMetadataUtils", "getAlbumArt, getMetadata elapsed time = " + (SystemClock.elapsedRealtime() - checkStart));
        }
        return albumArt;
    }
}
