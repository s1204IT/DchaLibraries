package com.mediatek.ims.internal.ext;

import android.content.Context;
import com.mediatek.common.util.OperatorCustomizationFactoryLoader;
import java.util.ArrayList;
import java.util.List;

public class OpImsCustomizationUtils {
    private static final List<OperatorCustomizationFactoryLoader.OperatorFactoryInfo> sOperatorFactoryInfoList = new ArrayList();
    static OpImsCustomizationFactoryBase sFactory = null;

    static {
        sOperatorFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP18Ims.jar", "com.mediatek.op18.ims.Op18ImsCustomizationFactory", (String) null, "OP18"));
        sOperatorFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP15Ims.jar", "com.mediatek.op15.ims.Op15ImsCustomizationFactory", (String) null, "OP15"));
    }

    public static synchronized OpImsCustomizationFactoryBase getOpFactory(Context context) {
        if (sFactory == null) {
            sFactory = (OpImsCustomizationFactoryBase) OperatorCustomizationFactoryLoader.loadFactory(context, sOperatorFactoryInfoList);
            if (sFactory == null) {
                sFactory = new OpImsCustomizationFactoryBase();
            }
        }
        return sFactory;
    }
}
