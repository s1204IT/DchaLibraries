package com.mediatek.gallery3d.util;

import mf.org.apache.xerces.impl.xs.SchemaSymbols;

public class FeatureConfig {
    public static volatile boolean sIsLowRamDevice;
    public static final boolean SUPPORT_EMULATOR = com.mediatek.galleryportable.SystemPropertyUtils.get("ro.kernel.qemu").equals(SchemaSymbols.ATTVAL_TRUE_1);
    public static final boolean IS_TABLET = com.mediatek.galleryportable.SystemPropertyUtils.get("ro.build.characteristics").equals("tablet");
    public static final boolean IS_GMO_RAM_OPTIMIZE = com.mediatek.galleryportable.SystemPropertyUtils.get("ro.vendor.mtk_gmo_ram_optimize").equals(SchemaSymbols.ATTVAL_TRUE_1);

    static {
        android.util.Log.d("MtkGallery2/FeatureConfig", "SUPPORT_FANCY_HOMEPAGE = true");
        android.util.Log.d("MtkGallery2/FeatureConfig", "SUPPORT_EMULATOR = " + SUPPORT_EMULATOR);
        android.util.Log.d("MtkGallery2/FeatureConfig", "IS_GMO_RAM_OPTIMIZE = " + IS_GMO_RAM_OPTIMIZE);
        android.util.Log.d("MtkGallery2/FeatureConfig", "sIsLowRamDevice = " + sIsLowRamDevice);
    }
}
