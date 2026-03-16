package com.android.camera.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import com.android.camera.debug.Log;
import java.util.List;

public class IntentHelper {
    private static final Log.Tag TAG = new Log.Tag("IntentHelper");

    public static Intent getGalleryIntent(Context context) {
        Intent intent = new Intent("android.intent.action.MAIN");
        GalleryHelper.setGalleryIntentClassName(intent);
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 65536);
        if (resolveInfos.size() == 0) {
            return null;
        }
        return intent;
    }

    public static Drawable getGalleryIcon(Context context, Intent galleryIntent) {
        return GalleryHelper.getGalleryIcon(context, galleryIntent);
    }

    public static CharSequence getGalleryAppName(Context context, Intent galleryIntent) {
        return GalleryHelper.getGalleryAppName(context, galleryIntent);
    }

    public static Intent getVideoPlayerIntent(Uri uri) {
        return new Intent("android.intent.action.VIEW").setDataAndType(uri, "video/*");
    }
}
