package com.mediatek.phone.ext;

import android.content.Context;
import android.util.Log;
import com.mediatek.common.util.OperatorCustomizationFactoryLoader;
import java.util.ArrayList;
import java.util.List;

public class OpPhoneCustomizationUtils {
    private static final String LOG_TAG = "OpPhoneCustomizationUtils";
    private static final List<OperatorCustomizationFactoryLoader.OperatorFactoryInfo> sOperatorFactoryInfoList = new ArrayList();
    static OpPhoneCustomizationFactoryBase sFactory = null;

    static {
        sOperatorFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP01TeleService.apk", "com.mediatek.op01.phone.plugin.Op01PhoneCustomizationFactory", "com.mediatek.op01.phone.plugin", "OP01"));
        sOperatorFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP02TeleService.apk", "com.mediatek.phone.op02.plugin.Op02PhoneCustomizationFactory", "com.mediatek.phone.op02.plugin", "OP02"));
        sOperatorFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP03TeleService.apk", "com.mediatek.op03.phone.Op03PhoneCustomizationFactory", "com.mediatek.op03.phone", "OP03"));
        sOperatorFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP06TeleService.apk", "com.mediatek.op06.phone.Op06PhoneCustomizationFactory", "com.mediatek.op06.phone", "OP06"));
        sOperatorFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP07TeleService.apk", "com.mediatek.op07.phone.OP07PhoneCustomizationFactory", "com.mediatek.op07.phone", "OP07"));
        sOperatorFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP08TeleService.apk", "com.mediatek.op08.phone.Op08PhoneCustomizationFactory", "com.mediatek.op08.phone", "OP08"));
        sOperatorFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP09ClibTeleService.apk", "com.mediatek.phone.op09Clib.plugin.Op09ClibPhoneCustomizationFactory", "com.mediatek.phone.op09Clib.plugin", "OP09", "SEGC"));
        sOperatorFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP12TeleService.apk", "com.mediatek.op12.phone.Op12PhoneCustomizationFactory", "com.mediatek.op12.phone", "OP12"));
        sOperatorFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP16TeleService.apk", "com.mediatek.op16.phone.Op16PhoneCustomizationFactory", "com.mediatek.op16.phone", "OP16"));
        sOperatorFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP18TeleService.apk", "com.mediatek.op18.phone.Op18PhoneCustomizationFactory", "com.mediatek.op18.phone", "OP18"));
        sOperatorFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP20TeleService.apk", "com.mediatek.op20.phone.Op20PhoneCustomizationFactory", "com.mediatek.op20.phone", "OP20"));
        sOperatorFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP112TeleService.apk", "com.mediatek.op112.phone.Op112PhoneCustomizationFactory", "com.mediatek.op112.phone", "OP112"));
    }

    private static void log(String str) {
        Log.d(LOG_TAG, str);
    }

    public static synchronized OpPhoneCustomizationFactoryBase getOpFactory(Context context) {
        if (sFactory == null) {
            sFactory = (OpPhoneCustomizationFactoryBase) OperatorCustomizationFactoryLoader.loadFactory(context, sOperatorFactoryInfoList);
            if (sFactory == null) {
                sFactory = new OpPhoneCustomizationFactoryBase();
            }
        }
        return sFactory;
    }

    public static synchronized void resetOpFactory(Context context) {
        log("resetOpFactory");
        sFactory = null;
    }
}
