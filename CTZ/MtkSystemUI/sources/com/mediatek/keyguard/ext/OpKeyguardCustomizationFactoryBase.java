package com.mediatek.keyguard.ext;

import android.content.Context;
import com.mediatek.common.util.OperatorCustomizationFactoryLoader;
import java.util.ArrayList;
import java.util.List;

public class OpKeyguardCustomizationFactoryBase {
    static OpKeyguardCustomizationFactoryBase sFactory;
    private static final List<OperatorCustomizationFactoryLoader.OperatorFactoryInfo> sOpFactoryInfoList = new ArrayList();

    public ICarrierTextExt makeCarrierText() {
        return new DefaultCarrierTextExt();
    }

    public IEmergencyButtonExt makeEmergencyButton() {
        return new DefaultEmergencyButtonExt();
    }

    public IKeyguardUtilExt makeKeyguardUtil() {
        return new DefaultKeyguardUtilExt();
    }

    public IOperatorSIMString makeOperatorSIMString() {
        return new DefaultOperatorSIMString();
    }

    static {
        sOpFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP01SystemUI.apk", "com.mediatek.keyguard.op01.Op01KeyguardCustomizationFactory", "com.mediatek.systemui.op01", "OP01"));
        sOpFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP02SystemUI.apk", "com.mediatek.keyguard.op02.Op02KeyguardCustomizationFactory", "com.mediatek.systemui.op02", "OP02"));
        sOpFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP09SystemUI.apk", "com.mediatek.keyguard.op09.Op09KeyguardCustomizationFactory", "com.mediatek.systemui.op09", "OP09", "SEGDEFAULT"));
        sOpFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP09ClipSystemUI.apk", "com.mediatek.keyguard.op09clip.Op09KeyguardCustomizationFactory", "com.mediatek.systemui.op09clip", "OP09", "SEGC"));
        sFactory = null;
    }

    public static synchronized OpKeyguardCustomizationFactoryBase getOpFactory(Context context) {
        if (sFactory == null) {
            sFactory = (OpKeyguardCustomizationFactoryBase) OperatorCustomizationFactoryLoader.loadFactory(context, sOpFactoryInfoList);
            if (sFactory == null) {
                sFactory = new OpKeyguardCustomizationFactoryBase();
            }
        }
        return sFactory;
    }
}
