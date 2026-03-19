package com.mediatek.server.telecom.ext;

import android.content.Context;
import android.telecom.Log;
import com.mediatek.common.util.OperatorCustomizationFactoryLoader;
import java.util.ArrayList;
import java.util.List;

public class OpTelecomCustomizationUtils {
    static OpTelecomCustomizationFactoryBase sFactory;
    private static final List<OperatorCustomizationFactoryLoader.OperatorFactoryInfo> sOperatorFactoryInfoList = new ArrayList();

    static {
        if (isOpFactoryLoaderAvailable()) {
            sOperatorFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP01Telecom.apk", "com.mediatek.op01.telecom.Op01TelecomCustomizationFactory", "com.mediatek.op01.telecom", "OP01"));
            sOperatorFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP08Telecom.apk", "com.mediatek.op08.telecom.Op08TelecomCustomizationFactory", "com.mediatek.op08.telecom", "OP08"));
            sOperatorFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP09ClibTelecom.apk", "com.mediatek.op09clib.telecom.Op09ClibTelecomCustomizationFactory", "com.mediatek.op09clib.telecom", "OP09", "SEGC"));
            sOperatorFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP12Telecomm.apk", "com.mediatek.op12.telecom.Op12TelecomCustomizationFactory", "com.mediatek.op12.telecom", "OP12"));
            sOperatorFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP18Telecomm.apk", "com.mediatek.op18.telecom.OP18TelecomCustomizationFactory", "com.mediatek.op18.telecom", "OP18"));
        }
        sFactory = null;
    }

    public static synchronized OpTelecomCustomizationFactoryBase getOpFactory(Context context) {
        if (sFactory == null && isOpFactoryLoaderAvailable()) {
            sFactory = (OpTelecomCustomizationFactoryBase) OperatorCustomizationFactoryLoader.loadFactory(context, sOperatorFactoryInfoList);
        }
        if (sFactory == null) {
            Log.i("OpTelecomCustomizationFactoryBase", "return default OpTelecomCustomizationFactoryBase", new Object[0]);
            sFactory = new OpTelecomCustomizationFactoryBase();
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
