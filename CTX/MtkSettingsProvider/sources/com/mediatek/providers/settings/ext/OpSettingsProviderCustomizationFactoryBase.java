package com.mediatek.providers.settings.ext;

import android.content.Context;
import com.mediatek.common.util.OperatorCustomizationFactoryLoader;
import java.util.ArrayList;
import java.util.List;

public class OpSettingsProviderCustomizationFactoryBase {
    static OpSettingsProviderCustomizationFactoryBase sFactory;
    private static final List<OperatorCustomizationFactoryLoader.OperatorFactoryInfo> sOpFactoryInfoList = new ArrayList();

    public IDatabaseHelperExt makeDatabaseHelp(Context context) {
        return new DefaultDatabaseHelperExt(context);
    }

    static {
        sOpFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP01SettingsProvider.apk", "com.mediatek.providers.settings.op01.Op01SettingsProviderCustomizationFactory", "com.mediatek.providers.settings.op01", "OP01"));
        sOpFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP03SettingsProvider.apk", "com.mediatek.op03.settingsProvider.OP03SettingsProviderCustomizationFactory", "com.mediatek.op03.settingsProvider", "OP03", "SEGDEFAULT"));
        sOpFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP18SettingsProvider.jar", "com.mediatek.op18.settingsProvider.OP18SettingsProviderCustomizationFactory", (String) null, "OP18", "SEGDEFAULT"));
        sFactory = null;
    }

    public static synchronized OpSettingsProviderCustomizationFactoryBase getOpFactory(Context context) {
        if (sFactory == null) {
            sFactory = (OpSettingsProviderCustomizationFactoryBase) OperatorCustomizationFactoryLoader.loadFactory(context, sOpFactoryInfoList);
            if (sFactory == null) {
                sFactory = new OpSettingsProviderCustomizationFactoryBase();
            }
        }
        return sFactory;
    }
}
