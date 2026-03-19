package com.mediatek.camera.feature.setting.dng;

import android.util.Size;
import java.util.List;

public interface IDngConfig {

    public interface OnDngValueUpdateListener {
        int onDisplayOrientationUpdate();

        void onDngCreatorStateUpdate(boolean z);

        void onDngValueUpdate(List<String> list, boolean z);

        void onSaveDngImage(byte[] bArr, Size size);
    }

    void notifyOverrideValue(boolean z);

    void onModeClosed();

    void requestChangeOverrideValues();

    void setDngStatus(boolean z, boolean z2);
}
