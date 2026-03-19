package com.android.documentsui;

import com.mediatek.omadrm.OmaDrmUtils;

public final class DocumentsFeatureOption {
    public static final boolean IS_SUPPORT_DRM = OmaDrmUtils.isOmaDrmEnabled();
}
