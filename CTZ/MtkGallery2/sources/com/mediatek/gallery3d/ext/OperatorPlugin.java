package com.mediatek.gallery3d.ext;

import android.content.Context;
import com.mediatek.gallery3d.util.Log;
import com.mediatek.galleryportable.OpFactoryUtils;

public class OperatorPlugin {
    private static final String TAG = "Gallery2/OperatorPlugin";
    private static volatile OpGalleryCustomizationFactoryBase sOpGalleryFactory = null;

    public static OpGalleryCustomizationFactoryBase getOpGalleryFactory(Context context) {
        if (sOpGalleryFactory == null) {
            synchronized (OpFactoryUtils.class) {
                if (sOpGalleryFactory == null) {
                    Object opGalleryFactory = OpFactoryUtils.getOpGalleryFactory(context);
                    if (opGalleryFactory == null) {
                        opGalleryFactory = new OpGalleryCustomizationFactoryBase();
                    }
                    sOpGalleryFactory = (OpGalleryCustomizationFactoryBase) opGalleryFactory;
                    Log.d(TAG, "<getOpGalleryFactory> factory = " + sOpGalleryFactory);
                }
            }
        }
        return sOpGalleryFactory;
    }
}
