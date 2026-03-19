package com.android.gallery3d.util;

import android.content.Context;
import android.net.Uri;

public class LightCycleHelper {
    public static final PanoramaMetadata NOT_PANORAMA = new PanoramaMetadata(false, false);

    public static class PanoramaMetadata {
        public final boolean mIsPanorama360;
        public final boolean mUsePanoramaViewer;

        public PanoramaMetadata(boolean z, boolean z2) {
            this.mUsePanoramaViewer = z;
            this.mIsPanorama360 = z2;
        }
    }

    public static PanoramaMetadata getPanoramaMetadata(Context context, Uri uri) {
        return NOT_PANORAMA;
    }
}
