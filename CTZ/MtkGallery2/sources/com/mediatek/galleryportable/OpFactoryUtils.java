package com.mediatek.galleryportable;

import android.content.Context;
import com.mediatek.common.util.OperatorCustomizationFactoryLoader;
import java.util.ArrayList;
import java.util.List;

public class OpFactoryUtils {
    private static boolean sIsOpFactoryLoaderExist = false;
    private static boolean sHasChecked = false;
    private static final List<OperatorCustomizationFactoryLoader.OperatorFactoryInfo> sOpGalleryFactoryInfoList = new ArrayList();
    private static final List<OperatorCustomizationFactoryLoader.OperatorFactoryInfo> sOpVpFactoryInfoList = new ArrayList();

    static {
        if (isOpFactoryLoaderExist()) {
            sOpGalleryFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP01Gallery.apk", "com.mediatek.gallery.op01.Op01GalleryCustomizationFactory", "com.mediatek.gallery.op01", "OP01"));
            sOpVpFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP01Gallery.apk", "com.mediatek.gallery.op01.Op01VideoCustomizationFactory", "com.mediatek.gallery.op01", "OP01"));
            sOpVpFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP02Gallery.apk", "com.mediatek.gallery.op02.Op02VideoCustomizationFactory", "com.mediatek.gallery.op02", "OP02"));
        }
    }

    public static Object getOpGalleryFactory(Context context) {
        Object opGalleryFactory = null;
        if (isOpFactoryLoaderExist()) {
            opGalleryFactory = OperatorCustomizationFactoryLoader.loadFactory(context, sOpGalleryFactoryInfoList);
        }
        Log.d("Gallery2/OpFactoryUtils", "<getOpGalleryFactory> factory = " + opGalleryFactory);
        return opGalleryFactory;
    }

    private static boolean isOpFactoryLoaderExist() {
        if (!sHasChecked) {
            try {
                Class<?> clazz = OpFactoryUtils.class.getClassLoader().loadClass("com.mediatek.common.util.OperatorCustomizationFactoryLoader");
                sIsOpFactoryLoaderExist = clazz != null;
                sHasChecked = true;
            } catch (ClassNotFoundException e) {
                sIsOpFactoryLoaderExist = false;
                sHasChecked = true;
            }
        }
        return sIsOpFactoryLoaderExist;
    }
}
