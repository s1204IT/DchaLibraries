package com.mediatek.camera.feature.setting.facedetection;

import com.mediatek.camera.common.utils.Size;
import java.util.List;

public interface IFaceConfig {

    public interface OnDetectedFaceUpdateListener {
        void onDetectedFaceUpdate(Face[] faceArr);
    }

    public interface OnFaceValueUpdateListener {
        Size onFacePreviewSizeUpdate();

        void onFaceSettingValueUpdate(boolean z, List<String> list);

        int onUpdateImageOrientation();
    }

    void resetFaceDetectionState();

    void setFaceDetectionUpdateListener(OnDetectedFaceUpdateListener onDetectedFaceUpdateListener);

    void updateImageOrientation();
}
