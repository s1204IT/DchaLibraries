package com.mediatek.common.wallpaper;

import android.content.Context;
import com.mediatek.common.util.OperatorCustomizationFactoryLoader;
import java.util.ArrayList;
import java.util.List;

public class OpWallpaperCustomizationFactoryBase {
    static OpWallpaperCustomizationFactoryBase sFactory;
    private static final List<OperatorCustomizationFactoryLoader.OperatorFactoryInfo> sOpFactoryInfoList = new ArrayList();

    public IWallpaperPlugin makeWallpaperPlugin(Context context) {
        return new DefaultWallpaperPlugin(context);
    }

    static {
        sOpFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP03Wallpaper.apk", "com.mediatek.op03.wallpaper.Op03WallpaperCustomizationFactory", "com.mediatek.op03.wallpaper", "OP03", "SEGDEFAULT"));
        sFactory = null;
    }

    public static synchronized OpWallpaperCustomizationFactoryBase getOpFactory(Context context) {
        if (sFactory == null) {
            sFactory = (OpWallpaperCustomizationFactoryBase) OperatorCustomizationFactoryLoader.loadFactory(context, sOpFactoryInfoList);
            if (sFactory == null) {
                sFactory = new OpWallpaperCustomizationFactoryBase();
            }
        }
        return sFactory;
    }
}
