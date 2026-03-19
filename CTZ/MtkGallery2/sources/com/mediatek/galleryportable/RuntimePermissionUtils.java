package com.mediatek.galleryportable;

import android.content.Context;
import com.mediatek.internal.R;
import java.lang.reflect.Field;

public class RuntimePermissionUtils {
    private static String sDeniedPermissionString = "";
    private static boolean sHasChecked = false;

    public static String getDeniedPermissionString(Context context) {
        if (!sHasChecked) {
            try {
                Field field = R.string.class.getField("denied_required_permission");
                int id = field.getInt(null);
                sDeniedPermissionString = context.getResources().getString(id);
            } catch (Exception e) {
                sDeniedPermissionString = "Permissions denied. You can change them in Settings -> Apps.";
            }
            sHasChecked = true;
        }
        return sDeniedPermissionString;
    }
}
