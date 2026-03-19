package com.mediatek.gallery3d.util;

import android.content.Context;
import com.mediatek.gallery3d.ext.IGalleryPickerExt;
import com.mediatek.gallery3d.ext.OperatorPlugin;

public class GalleryPluginUtils {
    private static volatile Context sContext = null;
    private static volatile IGalleryPickerExt sGalleryPicker;

    public static void initialize(Context context) {
        sContext = context;
    }

    public static IGalleryPickerExt getGalleryPickerPlugin() {
        if (sGalleryPicker == null) {
            synchronized (GalleryPluginUtils.class) {
                if (sGalleryPicker == null) {
                    sGalleryPicker = OperatorPlugin.getOpGalleryFactory(sContext).makeGalleryPickerExt(sContext);
                    Log.d("Gallery2/GalleryPluginUtils", "<getGalleryPickerPlugin> sGalleryPicker = " + sGalleryPicker);
                }
            }
        }
        return sGalleryPicker;
    }
}
