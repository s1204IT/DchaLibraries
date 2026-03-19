package com.mediatek.camera.feature.setting.scenemode;

import android.app.Activity;
import android.content.Context;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Surface;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.loader.DeviceDescription;
import com.mediatek.camera.common.mode.CameraApiHelper;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SceneModeCaptureRequestConfig implements ICameraSetting.ICaptureRequestConfigure {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(SceneModeCaptureRequestConfig.class.getSimpleName());
    private CameraCharacteristics.Key<int[]> mAsdAvailableModesKey = null;
    private CaptureRequest.Key<int[]> mAsdRequestModeKey = null;
    private CaptureResult.Key<int[]> mAsdResultModeKey = null;
    private CameraCaptureSession.CaptureCallback mCaptureCallback;
    private Context mContext;
    private String mDetectedSceneMode;
    private ISettingManager.SettingDevice2Requester mDevice2Requester;
    private Handler mMainHandler;
    private SceneMode mSceneMode;

    enum ModeEnum {
        OFF(0),
        FACE_PORTRAIT(1),
        ACTION(2),
        PORTRAIT(3),
        LANDSCAPE(4),
        NIGHT(5),
        NIGHT_PORTRAIT(6),
        THEATRE(7),
        BEACH(8),
        SNOW(9),
        SUNSET(10),
        STEADYPHOTO(11),
        FIREWORKS(12),
        SPORTS(13),
        PARTY(14),
        CANDLELIGHT(15),
        BARCODE(16),
        HIGH_SPEED_VIDEO(17),
        HDR(18),
        BACKLIGHT_PORTRAIT(32);

        private int mValue;

        ModeEnum(int i) {
            this.mValue = 0;
            this.mValue = i;
        }

        public int getValue() {
            return this.mValue;
        }

        public String getName() {
            return toString();
        }
    }

    public SceneModeCaptureRequestConfig(Activity activity, SceneMode sceneMode, ISettingManager.SettingDevice2Requester settingDevice2Requester, Context context) {
        this.mSceneMode = sceneMode;
        this.mDevice2Requester = settingDevice2Requester;
        this.mMainHandler = new MainHandler(activity.getMainLooper());
        this.mContext = context;
    }

    @Override
    public void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics) {
        List<String> listConvertEnumToString = convertEnumToString((int[]) cameraCharacteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES));
        if (!listConvertEnumToString.contains("off")) {
            listConvertEnumToString.add("off");
        }
        boolean z = true;
        DeviceDescription deviceDescription = CameraApiHelper.getDeviceSpec(this.mContext).getDeviceDescriptionMap().get(String.valueOf(this.mSceneMode.getCameraId()));
        if (deviceDescription != null) {
            this.mAsdAvailableModesKey = deviceDescription.getKeyAsdAvailableModes();
        }
        if (this.mAsdAvailableModesKey == null) {
            LogHelper.d(TAG, "available asd modes key isn't existed");
            z = false;
        }
        if (deviceDescription != null) {
            this.mAsdRequestModeKey = deviceDescription.getKeyAsdRequestMode();
        }
        if (this.mAsdRequestModeKey == null) {
            LogHelper.d(TAG, "asd request key isn't existed");
            z = false;
        }
        if (deviceDescription != null) {
            this.mAsdResultModeKey = deviceDescription.getKeyAsdResult();
        }
        if (this.mAsdResultModeKey == null) {
            LogHelper.d(TAG, "asd result key isn't existed");
            z = false;
        }
        if (z) {
            listConvertEnumToString.add("auto-scene-detection");
        }
        this.mSceneMode.initializeValue(listConvertEnumToString, "off");
    }

    @Override
    public void configCaptureRequest(CaptureRequest.Builder builder) {
        if (builder == null) {
            LogHelper.d(TAG, "[configCaptureRequest] captureBuilder is null");
            return;
        }
        String value = this.mSceneMode.getValue();
        LogHelper.d(TAG, "[configCaptureRequest], scene mode:" + value + ", mDetectedSceneMode:" + this.mDetectedSceneMode);
        if ("auto-scene-detection".equals(value)) {
            builder.set(this.mAsdRequestModeKey, new int[]{1});
            if (ModeEnum.HDR.getName().toLowerCase(Locale.ENGLISH).equals(this.mDetectedSceneMode) || ModeEnum.BACKLIGHT_PORTRAIT.getName().replace('_', '-').toLowerCase(Locale.ENGLISH).equals(this.mDetectedSceneMode) || ModeEnum.OFF.getName().toLowerCase(Locale.ENGLISH).equals(this.mDetectedSceneMode)) {
                LogHelper.d(TAG, "Special detected scene mode, actually set is: " + ModeEnum.OFF.getValue());
                builder.set(CaptureRequest.CONTROL_SCENE_MODE, Integer.valueOf(ModeEnum.OFF.getValue()));
                builder.set(CaptureRequest.CONTROL_MODE, 1);
                return;
            }
            builder.set(CaptureRequest.CONTROL_SCENE_MODE, Integer.valueOf(convertStringToEnum(this.mDetectedSceneMode)));
            builder.set(CaptureRequest.CONTROL_MODE, 2);
            return;
        }
        if (value != null && !ModeEnum.OFF.toString().equalsIgnoreCase(value)) {
            builder.set(CaptureRequest.CONTROL_SCENE_MODE, Integer.valueOf(convertStringToEnum(value)));
            builder.set(CaptureRequest.CONTROL_MODE, 2);
        }
    }

    @Override
    public void configSessionSurface(List<Surface> list) {
    }

    @Override
    public Surface configRawSurface() {
        return null;
    }

    @Override
    public CameraCaptureSession.CaptureCallback getRepeatingCaptureCallback() {
        if (!"auto-scene-detection".equals(this.mSceneMode.getValue())) {
            return null;
        }
        if (this.mCaptureCallback == null) {
            this.mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult) {
                    int[] iArr = (int[]) totalCaptureResult.get(SceneModeCaptureRequestConfig.this.mAsdResultModeKey);
                    if (iArr == null) {
                        return;
                    }
                    SceneModeCaptureRequestConfig.this.mDetectedSceneMode = SceneModeCaptureRequestConfig.this.convertEnumToString(iArr[0]);
                    SceneModeCaptureRequestConfig.this.mMainHandler.removeMessages(0);
                    SceneModeCaptureRequestConfig.this.mMainHandler.obtainMessage(0, SceneModeCaptureRequestConfig.this.mDetectedSceneMode).sendToTarget();
                }
            };
        }
        return this.mCaptureCallback;
    }

    @Override
    public void sendSettingChangeRequest() {
        this.mDevice2Requester.createAndChangeRepeatingRequest();
    }

    private List<String> convertEnumToString(int[] iArr) {
        ModeEnum[] modeEnumArrValues = ModeEnum.values();
        ArrayList arrayList = new ArrayList(iArr.length);
        for (int i : iArr) {
            int length = modeEnumArrValues.length;
            int i2 = 0;
            while (true) {
                if (i2 < length) {
                    ModeEnum modeEnum = modeEnumArrValues[i2];
                    if (modeEnum.getValue() != i) {
                        i2++;
                    } else {
                        arrayList.add(modeEnum.getName().replace('_', '-').toLowerCase(Locale.ENGLISH));
                        break;
                    }
                }
            }
        }
        return arrayList;
    }

    private String convertEnumToString(int i) {
        for (ModeEnum modeEnum : ModeEnum.values()) {
            if (modeEnum.getValue() == i) {
                return modeEnum.getName().replace('_', '-').toLowerCase(Locale.ENGLISH);
            }
        }
        return null;
    }

    private int convertStringToEnum(String str) {
        for (ModeEnum modeEnum : ModeEnum.values()) {
            if (modeEnum.getName().replace('_', '-').toLowerCase(Locale.ENGLISH).equalsIgnoreCase(str)) {
                return modeEnum.getValue();
            }
        }
        return 0;
    }

    private class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            String str = (String) message.obj;
            if (message.what == 0) {
                if (ModeEnum.HDR.getName().toLowerCase(Locale.ENGLISH).equals(str)) {
                    SceneModeCaptureRequestConfig.this.mSceneMode.onSceneDetected("hdr-detection");
                } else {
                    SceneModeCaptureRequestConfig.this.mSceneMode.onSceneDetected(str);
                }
            }
        }
    }
}
