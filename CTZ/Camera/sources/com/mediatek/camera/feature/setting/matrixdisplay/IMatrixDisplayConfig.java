package com.mediatek.camera.feature.setting.matrixdisplay;

import com.mediatek.camera.common.setting.ICameraSetting;

interface IMatrixDisplayConfig extends ICameraSetting.ISettingChangeRequester {
    void setDisplayStatus(boolean z);

    void setPreviewSize(int i, int i2);

    void setSelectedEffect(String str);
}
