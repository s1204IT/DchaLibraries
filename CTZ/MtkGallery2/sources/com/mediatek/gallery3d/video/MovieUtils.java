package com.mediatek.gallery3d.video;

import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import com.mediatek.gallery3d.util.Log;
import java.util.Locale;

public class MovieUtils {
    private static final String EXTRA_CAN_SHARE = "CanShare";
    private static final String HTTP_LIVE_SUFFIX = ".m3u8";
    private static final boolean LOG = true;
    private static final String TAG = "VP_MovieUtils";
    public static final int VIDEO_TYPE_HTTP = 1;
    public static final int VIDEO_TYPE_LIVE = 3;
    public static final int VIDEO_TYPE_LOCAL = 0;
    public static final int VIDEO_TYPE_RTSP = 2;

    private MovieUtils() {
    }

    public static int judgeVideoType(Uri uri, String str) {
        int i;
        Log.v(TAG, "judgeStreamingType entry with uri is: " + uri + " and mimeType is: " + str);
        if (uri == null) {
            return -1;
        }
        if (isRtspStreaming(uri, str)) {
            i = 2;
        } else if (isHttpStreaming(uri, str) || isHttpLiveStreaming(uri, str)) {
            i = 1;
        } else {
            i = 0;
        }
        Log.v(TAG, "videoType is " + i);
        return i;
    }

    public static boolean isRtspStreaming(Uri uri, String str) {
        if (uri != null && ("application/sdp".equals(str) || uri.toString().toLowerCase(Locale.ENGLISH).endsWith(".sdp") || "rtsp".equalsIgnoreCase(uri.getScheme()))) {
            return LOG;
        }
        return false;
    }

    public static boolean isHttpStreaming(Uri uri, String str) {
        if (uri != null && (("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) && !uri.toString().toLowerCase(Locale.ENGLISH).contains(HTTP_LIVE_SUFFIX) && !uri.toString().toLowerCase(Locale.ENGLISH).contains(".sdp") && !uri.toString().toLowerCase(Locale.ENGLISH).contains(".smil"))) {
            return LOG;
        }
        return false;
    }

    public static boolean isHttpLiveStreaming(Uri uri, String str) {
        if (uri != null && (("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) && uri.toString().toLowerCase(Locale.ENGLISH).contains(HTTP_LIVE_SUFFIX))) {
            return LOG;
        }
        return false;
    }

    public static boolean isLocalFile(Uri uri, String str) {
        boolean z = (isRtspStreaming(uri, str) || isHttpStreaming(uri, str) || isHttpLiveStreaming(uri, str)) ? false : LOG;
        Log.v(TAG, "isLocalFile(" + uri + ", " + str + ") return " + z);
        return z;
    }

    public static boolean isLocalFile(int i) {
        if (i == 0) {
            Log.v(TAG, "isLocalFile() is local");
            return LOG;
        }
        Log.v(TAG, "isLocalFile() is not local video type: " + i);
        return false;
    }

    public static boolean isRTSP(int i) {
        if (i == 2) {
            Log.v(TAG, "isRTSP() is RTSP");
            return LOG;
        }
        Log.v(TAG, "isRTSP() is not RTSP videoType: " + i);
        return false;
    }

    public static boolean isHTTP(int i) {
        if (i == 1) {
            Log.v(TAG, "isHTTP() is HTTP");
            return LOG;
        }
        Log.v(TAG, "isHTTP() is not HTTP videoType: " + i);
        return false;
    }

    @Deprecated
    public static boolean isRtspOrSdp(int i) {
        return isRTSP(i);
    }

    public static boolean isLiveStreaming(int i) {
        if (i == 3) {
            Log.v(TAG, "isLiveStreaming() is live streaming");
            return LOG;
        }
        Log.v(TAG, "isLiveStreaming() is not live video type: " + i);
        return false;
    }

    public static boolean canShare(Bundle bundle) {
        boolean z = LOG;
        if (bundle != null) {
            z = bundle.getBoolean(EXTRA_CAN_SHARE, LOG);
        }
        Log.v(TAG, "canShare(" + bundle + ") return " + z);
        return z;
    }

    public static Drawable bytesToDrawable(byte[] bArr) {
        BitmapDrawable bitmapDrawable;
        int length = bArr.length;
        if (length != 0) {
            bitmapDrawable = new BitmapDrawable(BitmapFactory.decodeByteArray(bArr, 0, length));
        } else {
            bitmapDrawable = null;
        }
        Log.v(TAG, "bytesToDrawable() exit with the drawable is " + bitmapDrawable);
        return bitmapDrawable;
    }
}
