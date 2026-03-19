package com.mediatek.phone.ext;

import android.content.Context;
import com.mediatek.common.util.OperatorCustomizationFactoryLoader;
import java.util.ArrayList;
import java.util.List;

public class CommonPhoneCustomizationUtils {
    private static CommonPhoneCustomizationFactoryBase sFactory;
    private static final List<OperatorCustomizationFactoryLoader.OperatorFactoryInfo> sOperatorFactoryInfoList = new ArrayList<OperatorCustomizationFactoryLoader.OperatorFactoryInfo>() {
        {
            add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OPTeleServiceCommon.apk", "com.mediatek.op.phone.plugin.CommonPhoneCustomizationFactory", "com.mediatek.op.phone.plugin", (String) null));
        }
    };

    public static synchronized CommonPhoneCustomizationFactoryBase getOpFactory(Context context) {
        if (sFactory == null) {
            sFactory = (CommonPhoneCustomizationFactoryBase) OperatorCustomizationFactoryLoader.loadFactory(context, sOperatorFactoryInfoList);
            if (sFactory == null) {
                sFactory = new CommonPhoneCustomizationFactoryBase();
            }
        }
        return sFactory;
    }
}
