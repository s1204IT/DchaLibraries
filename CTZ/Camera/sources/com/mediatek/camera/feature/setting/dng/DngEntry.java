package com.mediatek.camera.feature.setting.dng;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Size;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.loader.DeviceDescription;
import com.mediatek.camera.common.loader.FeatureEntryBase;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.portability.ReflectUtil;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class DngEntry extends FeatureEntryBase {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(DngEntry.class.getSimpleName());

    public DngEntry(Context context, Resources resources) {
        super(context, resources);
    }

    @Override
    public boolean isSupport(CameraDeviceManagerFactory.CameraApi cameraApi, Activity activity) {
        if (isThirdPartyIntent(activity)) {
            return false;
        }
        if (CameraDeviceManagerFactory.CameraApi.API1.equals(cameraApi)) {
            return isAPi1SupportDng("0");
        }
        if (CameraDeviceManagerFactory.CameraApi.API2.equals(cameraApi)) {
            return isAPi2SupportDng("0");
        }
        return false;
    }

    @Override
    public int getStage() {
        return 1;
    }

    @Override
    public Class getType() {
        return ICameraSetting.class;
    }

    @Override
    public Object createInstance() {
        return new Dng();
    }

    private boolean isAPi1SupportDng(String str) {
        Camera.Parameters parameters;
        ConcurrentHashMap<String, DeviceDescription> deviceDescriptionMap = this.mDeviceSpec.getDeviceDescriptionMap();
        if (str == null || deviceDescriptionMap == null || deviceDescriptionMap.size() <= 0) {
            return false;
        }
        if (ReflectUtil.getMethod(Camera.class, "enableRaw16", Boolean.TYPE) == null) {
            LogHelper.d(TAG, "no dng interface !");
            return false;
        }
        if (!deviceDescriptionMap.containsKey(str) || (parameters = deviceDescriptionMap.get(str).getParameters()) == null) {
            return false;
        }
        return "true".equals(parameters.get("dng-supported"));
    }

    @TargetApi(21)
    private boolean isAPi2SupportDng(String str) {
        CameraCharacteristics cameraCharacteristics;
        int[] iArr;
        ConcurrentHashMap<String, DeviceDescription> deviceDescriptionMap = this.mDeviceSpec.getDeviceDescriptionMap();
        if (str == null || deviceDescriptionMap == null || deviceDescriptionMap.size() <= 0 || !deviceDescriptionMap.containsKey(str) || (cameraCharacteristics = deviceDescriptionMap.get(str).getCameraCharacteristics()) == null || (iArr = (int[]) cameraCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)) == null) {
            return false;
        }
        ArrayList arrayList = new ArrayList(iArr.length);
        for (int i : iArr) {
            arrayList.add(Integer.valueOf(i));
        }
        if (!arrayList.contains(3)) {
            return false;
        }
        return isRawSizeValid(cameraCharacteristics);
    }

    @TargetApi(21)
    private boolean isRawSizeValid(CameraCharacteristics cameraCharacteristics) {
        Size[] outputSizes = ((StreamConfigurationMap) cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)).getOutputSizes(32);
        if (outputSizes == null) {
            LogHelper.e(TAG, "[isDngSupported] No capture sizes available for raw format");
            return false;
        }
        Rect rect = (Rect) cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        if (rect == null) {
            LogHelper.e(TAG, "[isDngSupported] Active array is null");
            return false;
        }
        Size size = new Size(rect.width(), rect.height());
        for (Size size2 : outputSizes) {
            if (size2.getWidth() == size.getWidth() && size2.getHeight() == size.getHeight()) {
                return true;
            }
        }
        return false;
    }
}
