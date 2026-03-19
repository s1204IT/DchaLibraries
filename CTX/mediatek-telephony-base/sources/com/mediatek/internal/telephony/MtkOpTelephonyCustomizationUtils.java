package com.mediatek.internal.telephony;

import android.content.Context;
import android.telephony.Rlog;
import com.mediatek.common.util.OperatorCustomizationFactoryLoader;
import java.util.ArrayList;
import java.util.List;

public class MtkOpTelephonyCustomizationUtils {
    private static final String DBG_TAG = "MtkOpTelephonyCustomizationUtils";
    private static MtkOpCommonTelephonyCustFactoryBase sInstance;
    private static final List<OperatorCustomizationFactoryLoader.OperatorFactoryInfo> sOperatorFactoryInfoList = new ArrayList();
    static MtkOpTelephonyCustomizationFactoryBase sFactory = null;

    static {
        sOperatorFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP09CTelephony.jar", "com.mediatek.op09c.telephony.MtkOp09CTelephonyCustomizationFactory", (String) null, "OP09", "SEGC"));
        sOperatorFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP01Telephony.jar", "com.mediatek.op01.telephony.MtkOp01TelephonyCustomizationFactory", (String) null, "OP01"));
        sOperatorFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP02Telephony.jar", "com.mediatek.op02.telephony.MtkOp02TelephonyCustomizationFactory", (String) null, "OP02"));
    }

    public static synchronized MtkOpTelephonyCustomizationFactoryBase getOpFactory(Context context) {
        if (sFactory == null) {
            sFactory = (MtkOpTelephonyCustomizationFactoryBase) OperatorCustomizationFactoryLoader.loadFactory(context, sOperatorFactoryInfoList);
            if (sFactory == null) {
                sFactory = new MtkOpTelephonyCustomizationFactoryBase();
            }
        }
        return sFactory;
    }

    public static MtkOpCommonTelephonyCustFactoryBase getOpCommonInstance() {
        if (sInstance == null) {
            synchronized (MtkOpCommonTelephonyCustFactoryBase.class) {
                try {
                    sInstance = (MtkOpCommonTelephonyCustFactoryBase) Class.forName("com.mediatek.op.telephony.MtkOpCommonTelephonyCustFactory").getConstructor(new Class[0]).newInstance(new Object[0]);
                    Rlog.d(DBG_TAG, "Success to get MtkOpCommonTelephonyCustFactoryBase class");
                } catch (Exception e) {
                    sInstance = new MtkOpCommonTelephonyCustFactoryBase();
                    Rlog.e(DBG_TAG, "Fail to get MtkOpCommonTelephonyCustFactoryBase class");
                }
            }
        }
        return sInstance;
    }
}
