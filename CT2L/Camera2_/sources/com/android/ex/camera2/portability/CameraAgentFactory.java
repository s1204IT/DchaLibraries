package com.android.ex.camera2.portability;

import android.content.Context;
import android.os.Build;
import com.android.ex.camera2.portability.debug.Log;
import com.android.ex.camera2.portability.util.SystemProperties;

public class CameraAgentFactory {
    private static final String API_LEVEL_OVERRIDE_API1 = "1";
    private static final String API_LEVEL_OVERRIDE_API2 = "2";
    private static final int FIRST_SDK_WITH_API_2 = 21;
    private static CameraAgent sAndroidCamera2Agent;
    private static int sAndroidCamera2AgentClientCount;
    private static CameraAgent sAndroidCameraAgent;
    private static int sAndroidCameraAgentClientCount;
    private static final Log.Tag TAG = new Log.Tag("CamAgntFact");
    private static final String API_LEVEL_OVERRIDE_KEY = "camera2.portability.force_api";
    private static final String API_LEVEL_OVERRIDE_DEFAULT = "0";
    private static final String API_LEVEL_OVERRIDE_VALUE = SystemProperties.get(API_LEVEL_OVERRIDE_KEY, API_LEVEL_OVERRIDE_DEFAULT);

    public enum CameraApi {
        AUTO,
        API_1,
        API_2
    }

    private static CameraApi highestSupportedApi() {
        return (Build.VERSION.SDK_INT >= FIRST_SDK_WITH_API_2 || Build.VERSION.CODENAME.equals("L")) ? CameraApi.API_2 : CameraApi.API_1;
    }

    private static CameraApi validateApiChoice(CameraApi choice) {
        if (API_LEVEL_OVERRIDE_VALUE.equals(API_LEVEL_OVERRIDE_API1)) {
            Log.d(TAG, "API level overridden by system property: forced to 1");
            return CameraApi.API_1;
        }
        if (API_LEVEL_OVERRIDE_VALUE.equals("2")) {
            Log.d(TAG, "API level overridden by system property: forced to 2");
            return CameraApi.API_2;
        }
        if (choice == null) {
            Log.w(TAG, "null API level request, so assuming AUTO");
            choice = CameraApi.AUTO;
        }
        if (choice == CameraApi.AUTO) {
            return highestSupportedApi();
        }
        return choice;
    }

    public static synchronized CameraAgent getAndroidCameraAgent(Context context, CameraApi api) {
        CameraAgent cameraAgent;
        if (validateApiChoice(api) == CameraApi.API_1) {
            if (sAndroidCameraAgent == null) {
                sAndroidCameraAgent = new AndroidCameraAgentImpl();
                sAndroidCameraAgentClientCount = 1;
            } else {
                sAndroidCameraAgentClientCount++;
            }
            cameraAgent = sAndroidCameraAgent;
        } else {
            if (highestSupportedApi() == CameraApi.API_1) {
                throw new UnsupportedOperationException("Camera API_2 unavailable on this device");
            }
            if (sAndroidCamera2Agent == null) {
                sAndroidCamera2Agent = new AndroidCamera2AgentImpl(context);
                sAndroidCamera2AgentClientCount = 1;
            } else {
                sAndroidCamera2AgentClientCount++;
            }
            cameraAgent = sAndroidCamera2Agent;
        }
        return cameraAgent;
    }

    public static synchronized void recycle(CameraApi api) {
        if (validateApiChoice(api) == CameraApi.API_1) {
            int i = sAndroidCameraAgentClientCount - 1;
            sAndroidCameraAgentClientCount = i;
            if (i == 0 && sAndroidCameraAgent != null) {
                sAndroidCameraAgent.recycle();
                sAndroidCameraAgent = null;
            }
        } else {
            if (highestSupportedApi() == CameraApi.API_1) {
                throw new UnsupportedOperationException("Camera API_2 unavailable on this device");
            }
            int i2 = sAndroidCamera2AgentClientCount - 1;
            sAndroidCamera2AgentClientCount = i2;
            if (i2 == 0 && sAndroidCamera2Agent != null) {
                sAndroidCamera2Agent.recycle();
                sAndroidCamera2Agent = null;
            }
        }
    }
}
