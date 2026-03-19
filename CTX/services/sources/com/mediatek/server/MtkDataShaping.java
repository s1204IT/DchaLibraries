package com.mediatek.server;

import android.os.IBinder;
import android.os.ServiceManager;
import android.util.Log;
import com.android.server.AlarmManagerService;
import dalvik.system.PathClassLoader;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class MtkDataShaping {
    private static final String DATASHPAING_SERVICE_CLASS = "com.mediatek.datashaping.DataShapingServiceImpl";
    private static final String DATASHPAING_SERVICE_NAME = "data_shaping";
    private static final String TAG = "MtkDataShapingUtils";

    public static void openLteGateByDataShaping(ArrayList<AlarmManagerService.Alarm> arrayList) {
        try {
            IBinder service = ServiceManager.getService(DATASHPAING_SERVICE_NAME);
            if (service == null) {
                Log.d(TAG, "failed to get service : data_shaping");
                return;
            }
            PathClassLoader pathClassLoader = MtkSystemServer.sClassLoader;
            if (pathClassLoader == null) {
                Log.d(TAG, "MtkSystemServer.sClassLoader is null.");
                return;
            }
            Class<?> cls = Class.forName(DATASHPAING_SERVICE_CLASS, false, pathClassLoader);
            if (cls == null) {
                Log.d(TAG, "failed to get class for name : com.mediatek.datashaping.DataShapingServiceImpl");
                return;
            }
            Method declaredMethod = cls.getDeclaredMethod("openLteGateByDataShaping", ArrayList.class);
            if (declaredMethod == null) {
                Log.d(TAG, "failed to get method : openLteGateByDataShaping");
            } else {
                declaredMethod.setAccessible(true);
                declaredMethod.invoke(service, arrayList);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception openLteGateByDataShaping  in " + e);
            e.printStackTrace();
        }
    }

    public static void setDeviceIdleMode(boolean z) {
        try {
            IBinder service = ServiceManager.getService(DATASHPAING_SERVICE_NAME);
            Method declaredMethod = Class.forName(DATASHPAING_SERVICE_CLASS, false, MtkSystemServer.sClassLoader).getDeclaredMethod("setDeviceIdleMode", Boolean.TYPE);
            declaredMethod.setAccessible(true);
            declaredMethod.invoke(service, Boolean.valueOf(z));
        } catch (Exception e) {
            Log.e(TAG, "Exception setDeviceIdleMode  in " + e);
            e.printStackTrace();
        }
    }
}
