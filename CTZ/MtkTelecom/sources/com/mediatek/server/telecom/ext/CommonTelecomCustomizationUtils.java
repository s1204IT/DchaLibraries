package com.mediatek.server.telecom.ext;

import android.content.Context;
import android.util.Log;
import com.mediatek.common.util.OperatorCustomizationFactoryLoader;
import java.util.ArrayList;
import java.util.List;

public class CommonTelecomCustomizationUtils {
    private static CommonTelecomCustomizationFactoryBase sFactory;
    private static final List<OperatorCustomizationFactoryLoader.OperatorFactoryInfo> sOperatorFactoryInfoList = new ArrayList();

    static {
        if (isOpFactoryLoaderAvailable()) {
            sOperatorFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OPTelecomCommon.apk", "com.mediatek.op.telecom.CommonTelecomCustomizationFactory", "com.mediatek.op.telecom", (String) null));
        }
    }

    public static synchronized CommonTelecomCustomizationFactoryBase getOpFactory(Context context) {
        if (sFactory == null && isOpFactoryLoaderAvailable()) {
            sFactory = (CommonTelecomCustomizationFactoryBase) OperatorCustomizationFactoryLoader.loadFactory(context, sOperatorFactoryInfoList);
        }
        if (sFactory == null) {
            Log.i("CommonTelecomCustomizationUtils", "return default CommonTelecomCustomizationFactoryBase");
            sFactory = new CommonTelecomCustomizationFactoryBase();
        }
        return sFactory;
    }

    private static boolean isOpFactoryLoaderAvailable() {
        try {
            Class.forName("com.mediatek.common.util.OperatorCustomizationFactoryLoader");
            return true;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }
}
