package com.mediatek.gallery3d.video;

import android.content.Context;
import com.mediatek.gallery3d.ext.IActivityHooker;
import com.mediatek.gallery3d.util.Log;

public class ExtensionHelper {
    private static final boolean LOG = true;
    private static final String TAG = "VP_ExtensionHelper";
    private static String sContextString;
    private static IMovieDrmExtension sMovieDrmExtension;

    public static IActivityHooker getHooker(Context context) {
        ActivityHookerGroup activityHookerGroup = new ActivityHookerGroup();
        activityHookerGroup.addHooker(new StopVideoHooker());
        activityHookerGroup.addHooker(new LoopVideoHooker());
        activityHookerGroup.addHooker(new TrimVideoHooker());
        activityHookerGroup.addHooker(new NfcHooker());
        activityHookerGroup.addHooker(new VideoTitleHooker());
        if (MtkVideoFeature.isClearMotionMenuEnabled(context)) {
            activityHookerGroup.addHooker(new ClearMotionHooker());
        }
        if (MtkVideoFeature.isSupportCTA()) {
            activityHookerGroup.addHooker(new CTAHooker());
        }
        int size = activityHookerGroup.size();
        for (int i = 0; i < size; i++) {
            Log.v(TAG, "getHooker() [" + i + "]=" + activityHookerGroup.getHooker(i));
        }
        sContextString = context.toString();
        return activityHookerGroup;
    }

    public static IMovieDrmExtension getMovieDrmExtension(Context context) {
        if (sMovieDrmExtension == null) {
            if (MtkVideoFeature.isOmaDrmSupported()) {
                sMovieDrmExtension = new MovieDrmExtensionImpl();
            } else {
                sMovieDrmExtension = new DefaultMovieDrmExtension();
            }
        }
        return sMovieDrmExtension;
    }
}
