package com.mediatek.contacts.util;

import android.content.ContentValues;
import android.content.Context;
import android.drm.DrmManagerClient;
import android.net.Uri;
import com.mediatek.contacts.ContactsSystemProperties;

public class DrmUtils {
    public static boolean isDrmImage(Context context, Uri uri) {
        ContentValues metadata;
        boolean z = false;
        if (!ContactsSystemProperties.MTK_DRM_SUPPORT) {
            Log.w("DrmUtils", "[isDrmImage] not support drm...");
            return false;
        }
        DrmManagerClient drmManagerClient = new DrmManagerClient(context);
        try {
            metadata = drmManagerClient.getMetadata(uri);
        } catch (IllegalArgumentException e) {
            Log.e("DrmUtils", "isDrmImage: getMetadata fail with uri " + uri, e);
            metadata = null;
        }
        Integer asInteger = metadata != null ? metadata.getAsInteger("isdrm") : null;
        if (asInteger != null && asInteger.intValue() > 0) {
            z = true;
        }
        drmManagerClient.release();
        Log.i("DrmUtils", "[isDrmImage] isDrm:" + z + ",uri:" + uri);
        return z;
    }
}
