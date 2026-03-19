package com.mediatek.server.wifi;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.os.Message;
import android.util.Log;
import com.android.internal.util.State;
import com.android.server.wifi.ScanDetail;
import dalvik.system.PathClassLoader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class MtkWifiServiceAdapter {
    private static Method sHandleScanResultsMethod;
    private static IMtkWifiService sMWS;
    private static Method sPostProcessMessageMethod;
    private static Method sPreProcessMessageMethod;
    private static Method sUpdateRSSIMethod;
    private static final String TAG = "MtkWifiServiceAdapter";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);

    public interface IMtkWifiService {
        List<ScanResult> getLatestScanResults();

        void handleScanResults(List<ScanDetail> list, List<ScanDetail> list2);

        void initialize();

        boolean postProcessMessage(State state, Message message, Object... objArr);

        boolean preProcessMessage(State state, Message message);

        void updateRSSI(Integer num, int i, int i2);
    }

    public static void initialize(Context context) {
        log("[initialize]: " + context);
        try {
            sMWS = (IMtkWifiService) Class.forName("com.mediatek.server.wifi.MtkWifiService", false, new PathClassLoader("/system/framework/mtk-wifi-service.jar", MtkWifiServiceAdapter.class.getClassLoader())).getConstructor(Context.class).newInstance(context);
            sMWS.initialize();
        } catch (ClassNotFoundException e) {
            log("No extension found");
            e.printStackTrace();
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e2) {
            throw new Error(e2);
        }
    }

    public static void log(String str) {
        if (DEBUG) {
            Log.d(TAG, str);
        }
    }

    public static void handleScanResults(List<ScanDetail> list, List<ScanDetail> list2) {
        if (sMWS != null) {
            sMWS.handleScanResults(list, list2);
        }
    }

    public static void updateRSSI(Integer num, int i, int i2) {
        if (sMWS != null) {
            sMWS.updateRSSI(num, i, i2);
        }
    }

    public static boolean preProcessMessage(State state, Message message) {
        if (sMWS != null) {
            return sMWS.preProcessMessage(state, message);
        }
        return false;
    }

    public static boolean postProcessMessage(State state, Message message, Object... objArr) {
        if (sMWS != null) {
            return sMWS.postProcessMessage(state, message, objArr);
        }
        return false;
    }
}
