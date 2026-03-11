package com.mediatek.systemui.ext;

public class DefaultMobileIconExt implements IMobileIconExt {
    @Override
    public int customizeWifiNetCondition(int netCondition) {
        return netCondition;
    }

    @Override
    public int customizeMobileNetCondition(int netCondition) {
        return netCondition;
    }
}
