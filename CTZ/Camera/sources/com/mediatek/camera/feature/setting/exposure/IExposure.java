package com.mediatek.camera.feature.setting.exposure;

import java.util.List;

public interface IExposure {

    public enum FlashFlow {
        FLASH_FLOW_NO_FLASH,
        FLASH_FLOW_NORMAL,
        FLASH_FLOW_PANEL_STANDARD,
        FLASH_FLOW_PANEL_CUSTOMIZATION
    }

    public interface Listener {
        boolean checkTodoCapturAfterAeConverted();

        boolean getAeLock();

        boolean needConsiderAePretrigger();

        void overrideExposureValue(String str, List<String> list);

        void setAeLock(boolean z);

        void updateEv(int i);
    }
}
