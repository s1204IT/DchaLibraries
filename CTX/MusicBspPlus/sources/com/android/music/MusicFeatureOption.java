package com.android.music;

import android.os.SystemProperties;

public final class MusicFeatureOption {
    static final boolean IS_SUPPORT_DRM = SystemProperties.getBoolean("ro.vendor.mtk_oma_drm_support", false);
    static final boolean IS_SUPPORT_FM_TX = SystemProperties.getBoolean("ro.vendor.mtk_fm_tx_support", false);
}
