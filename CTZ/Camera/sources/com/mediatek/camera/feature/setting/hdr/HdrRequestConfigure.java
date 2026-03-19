package com.mediatek.camera.feature.setting.hdr;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.view.Surface;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.loader.DeviceDescription;
import com.mediatek.camera.common.mode.CameraApiHelper;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.feature.setting.hdr.IHdr;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import junit.framework.Assert;

@TargetApi(21)
public class HdrRequestConfigure implements ICameraSetting.ICaptureRequestConfigure, IHdr.Listener {
    private Context mContext;
    private Hdr mHdr;
    private CameraCharacteristics.Key<int[]> mKeyHdrAvailablePhotoModes;
    private CameraCharacteristics.Key<int[]> mKeyHdrAvailableVideoModes;
    private CaptureResult.Key<int[]> mKeyHdrDetectionResult;
    private CaptureRequest.Key<int[]> mKeyHdrRequestMode;
    private CaptureRequest.Key<int[]> mKeyHdrRequsetSessionMode;
    private ISettingManager.SettingDevice2Requester mSettingDevice2Requester;
    private static final LogUtil.Tag TAG = new LogUtil.Tag(HdrRequestConfigure.class.getSimpleName());
    private static final int[] CAM_HDR_FEATURE_HDR_MODE_OFF = {0};
    private static final int[] CAM_HDR_FEATURE_HDR_MODE_ON = {1};
    private static final int[] CAM_HDR_FEATURE_HDR_MODE_AUTO = {2};
    private static final int[] CAM_HDR_FEATURE_HDR_MODE_VIDEO_ON = {3};
    private static final int[] CAM_HDR_FEATURE_HDR_MODE_VIDEO_AUTO = {4};
    private static final int[] CAM_HDR_AUTO_DETECTION_ON = {1};
    private int mCameraId = -1;
    private int mLastHdrDetectionValue = -1;
    private boolean mIsHdrSupported = false;
    private boolean mIsVendorHdrSupported = false;
    private boolean mIsSensorDetectionHdrOnSupported = false;
    private boolean mIsSensorDetectionHdrAutoSupported = false;
    private String mHdrSessionValue = "off";
    private CameraCaptureSession.CaptureCallback mPreviewCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult) {
            super.onCaptureCompleted(cameraCaptureSession, captureRequest, totalCaptureResult);
            Assert.assertNotNull(totalCaptureResult);
            if (!"auto".equals(HdrRequestConfigure.this.mHdr.getValue())) {
                return;
            }
            Iterator<CaptureResult.Key<?>> it = totalCaptureResult.getKeys().iterator();
            while (it.hasNext()) {
                if (it.next().getName().equals("com.mediatek.hdrfeature.hdrDetectionResult")) {
                    int[] iArr = (int[]) totalCaptureResult.get(HdrRequestConfigure.this.mKeyHdrDetectionResult);
                    if (iArr[0] != HdrRequestConfigure.this.mLastHdrDetectionValue) {
                        LogHelper.d(HdrRequestConfigure.TAG, "onCaptureCompleted, value: " + iArr[0]);
                        if (iArr[0] == HdrRequestConfigure.CAM_HDR_AUTO_DETECTION_ON[0]) {
                            HdrRequestConfigure.this.mHdr.onAutoDetectionResult(true);
                        } else {
                            HdrRequestConfigure.this.mHdr.onAutoDetectionResult(false);
                        }
                        HdrRequestConfigure.this.mLastHdrDetectionValue = iArr[0];
                        return;
                    }
                    return;
                }
            }
        }
    };

    public HdrRequestConfigure(Hdr hdr, ISettingManager.SettingDevice2Requester settingDevice2Requester, Context context) {
        this.mContext = context;
        this.mHdr = hdr;
        this.mSettingDevice2Requester = settingDevice2Requester;
    }

    @Override
    public void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics) {
        this.mIsHdrSupported = false;
        this.mIsVendorHdrSupported = false;
        initHdrVendorKey(cameraCharacteristics);
        if (this.mHdr.getCurrentModeType() == ICameraMode.ModeType.VIDEO) {
            initHdrVideoSettingValues(cameraCharacteristics);
        } else {
            initHdrPhotoSettingValues(cameraCharacteristics);
        }
        if (this.mHdr.getEntryValues().size() > 1 && !this.mHdr.getEntryValues().contains(this.mHdr.getValue())) {
            this.mHdr.setValue("off");
        } else if (this.mHdr.getEntryValues().size() <= 1) {
            this.mHdr.resetRestriction();
        }
        this.mHdrSessionValue = this.mHdr.getValue();
        LogHelper.d(TAG, "[setCameraCharacteristics], mIsHdrSupported = " + this.mIsHdrSupported);
    }

    @Override
    public void configCaptureRequest(CaptureRequest.Builder builder) {
        if (builder == null) {
            LogHelper.d(TAG, "[configCaptureRequest] captureBuilder is null");
            return;
        }
        if (this.mHdr.getEntryValues().size() <= 1) {
            return;
        }
        LogHelper.d(TAG, "[configCaptureRequest], value = " + this.mHdr.getValue());
        if ("on".equals(this.mHdr.getValue()) || "auto".equals(this.mHdr.getValue())) {
            builder.set(CaptureRequest.CONTROL_MODE, 2);
            builder.set(CaptureRequest.CONTROL_SCENE_MODE, 18);
        }
        if (!this.mIsVendorHdrSupported) {
            return;
        }
        if (this.mHdr.getCurrentModeType() == ICameraMode.ModeType.VIDEO) {
            configVideoCaptureRequest(builder);
        } else {
            configPhotoCaptureRequest(builder);
        }
        configHdrSessionRequest(builder);
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
        return this.mPreviewCallback;
    }

    @Override
    public void sendSettingChangeRequest() {
        this.mSettingDevice2Requester.createAndChangeRepeatingRequest();
    }

    @Override
    public void onPreviewStateChanged(boolean z) {
    }

    @Override
    public void onHdrValueChanged() {
        this.mSettingDevice2Requester.createAndChangeRepeatingRequest();
    }

    @Override
    public void updateModeDeviceState(String str) {
    }

    @Override
    public void setCameraId(int i) {
        this.mCameraId = i;
    }

    @Override
    public boolean isZsdHdrSupported() {
        return false;
    }

    private void configVideoCaptureRequest(CaptureRequest.Builder builder) {
        Assert.assertNotNull(this.mKeyHdrRequestMode);
        if ("on".equals(this.mHdr.getValue())) {
            builder.set(this.mKeyHdrRequestMode, CAM_HDR_FEATURE_HDR_MODE_VIDEO_ON);
        } else if ("auto".equals(this.mHdr.getValue())) {
            builder.set(this.mKeyHdrRequestMode, CAM_HDR_FEATURE_HDR_MODE_VIDEO_AUTO);
        } else {
            builder.set(this.mKeyHdrRequestMode, CAM_HDR_FEATURE_HDR_MODE_OFF);
        }
    }

    private void configPhotoCaptureRequest(CaptureRequest.Builder builder) {
        Assert.assertNotNull(this.mKeyHdrRequestMode);
        if ("on".equals(this.mHdr.getValue())) {
            if (this.mIsSensorDetectionHdrOnSupported) {
                builder.set(this.mKeyHdrRequestMode, CAM_HDR_FEATURE_HDR_MODE_VIDEO_ON);
                return;
            } else {
                builder.set(this.mKeyHdrRequestMode, CAM_HDR_FEATURE_HDR_MODE_ON);
                return;
            }
        }
        if ("auto".equals(this.mHdr.getValue())) {
            if (this.mIsSensorDetectionHdrAutoSupported) {
                builder.set(this.mKeyHdrRequestMode, CAM_HDR_FEATURE_HDR_MODE_VIDEO_AUTO);
                return;
            } else {
                builder.set(this.mKeyHdrRequestMode, CAM_HDR_FEATURE_HDR_MODE_AUTO);
                return;
            }
        }
        builder.set(this.mKeyHdrRequestMode, CAM_HDR_FEATURE_HDR_MODE_OFF);
    }

    private void configHdrSessionRequest(CaptureRequest.Builder builder) {
        if (this.mKeyHdrRequsetSessionMode == null) {
            LogHelper.w(TAG, "[configHdrSessionRequest] mKeyHdrRequsetSessionMode is null");
            return;
        }
        LogHelper.d(TAG, "[configHdrSessionRequest] currrent mode " + this.mHdr.getCurrentModeType() + ",mHdrSessionValue " + this.mHdrSessionValue);
        if (this.mHdr.getCurrentModeType() == ICameraMode.ModeType.VIDEO) {
            if ("on".equals(this.mHdrSessionValue)) {
                builder.set(this.mKeyHdrRequsetSessionMode, CAM_HDR_FEATURE_HDR_MODE_VIDEO_ON);
                return;
            } else if ("auto".equals(this.mHdrSessionValue)) {
                builder.set(this.mKeyHdrRequsetSessionMode, CAM_HDR_FEATURE_HDR_MODE_VIDEO_AUTO);
                return;
            } else {
                builder.set(this.mKeyHdrRequsetSessionMode, CAM_HDR_FEATURE_HDR_MODE_OFF);
                return;
            }
        }
        if ("on".equals(this.mHdrSessionValue)) {
            if (this.mIsSensorDetectionHdrOnSupported) {
                builder.set(this.mKeyHdrRequsetSessionMode, CAM_HDR_FEATURE_HDR_MODE_VIDEO_ON);
                return;
            } else {
                builder.set(this.mKeyHdrRequsetSessionMode, CAM_HDR_FEATURE_HDR_MODE_ON);
                return;
            }
        }
        if ("auto".equals(this.mHdrSessionValue)) {
            if (this.mIsSensorDetectionHdrAutoSupported) {
                builder.set(this.mKeyHdrRequsetSessionMode, CAM_HDR_FEATURE_HDR_MODE_VIDEO_AUTO);
                return;
            } else {
                builder.set(this.mKeyHdrRequsetSessionMode, CAM_HDR_FEATURE_HDR_MODE_AUTO);
                return;
            }
        }
        builder.set(this.mKeyHdrRequsetSessionMode, CAM_HDR_FEATURE_HDR_MODE_OFF);
    }

    private void initHdrVendorKey(CameraCharacteristics cameraCharacteristics) {
        DeviceDescription deviceDescription = CameraApiHelper.getDeviceSpec(this.mContext).getDeviceDescriptionMap().get(String.valueOf(this.mCameraId));
        if (deviceDescription != null) {
            this.mKeyHdrAvailablePhotoModes = deviceDescription.getKeyHdrAvailablePhotoModes();
            this.mKeyHdrAvailableVideoModes = deviceDescription.getKeyHdrAvailableVideoModes();
            this.mKeyHdrDetectionResult = deviceDescription.getKeyHdrDetectionResult();
            this.mKeyHdrRequestMode = deviceDescription.getKeyHdrRequestMode();
            this.mKeyHdrRequsetSessionMode = deviceDescription.getKeyHdrRequsetSessionMode();
            LogHelper.i(TAG, "initHdrVendorKey init vendor key from device spec mCameraId: " + this.mCameraId);
            return;
        }
        Iterator<CameraCharacteristics.Key<?>> it = cameraCharacteristics.getKeys().iterator();
        while (it.hasNext()) {
            CameraCharacteristics.Key<int[]> key = (CameraCharacteristics.Key) it.next();
            if (key.getName().equals("com.mediatek.hdrfeature.availableHdrModesPhoto")) {
                this.mKeyHdrAvailablePhotoModes = key;
            } else if (key.getName().equals("com.mediatek.hdrfeature.availableHdrModesVideo")) {
                this.mKeyHdrAvailableVideoModes = key;
            }
        }
        Iterator<CaptureResult.Key<?>> it2 = cameraCharacteristics.getAvailableCaptureResultKeys().iterator();
        while (it2.hasNext()) {
            CaptureResult.Key<int[]> key2 = (CaptureResult.Key) it2.next();
            if (key2.getName().equals("com.mediatek.hdrfeature.hdrDetectionResult")) {
                this.mKeyHdrDetectionResult = key2;
            }
        }
        Iterator<CaptureRequest.Key<?>> it3 = cameraCharacteristics.getAvailableCaptureRequestKeys().iterator();
        while (it3.hasNext()) {
            CaptureRequest.Key<int[]> key3 = (CaptureRequest.Key) it3.next();
            if (key3.getName().equals("com.mediatek.hdrfeature.hdrMode")) {
                this.mKeyHdrRequestMode = key3;
            } else if (key3.getName().equals("com.mediatek.hdrfeature.SessionParamhdrMode")) {
                this.mKeyHdrRequsetSessionMode = key3;
            }
        }
    }

    private void initHdrPhotoSettingValues(CameraCharacteristics cameraCharacteristics) {
        ArrayList arrayList = new ArrayList();
        this.mIsSensorDetectionHdrOnSupported = false;
        this.mIsSensorDetectionHdrAutoSupported = false;
        arrayList.add("off");
        int[] iArr = (int[]) cameraCharacteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES);
        int length = iArr.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            if (iArr[i] != 18) {
                i++;
            } else {
                arrayList.add("on");
                this.mIsHdrSupported = true;
                break;
            }
        }
        if (!this.mIsHdrSupported) {
            return;
        }
        LogHelper.d(TAG, "initHdrPhotoSettingValues ");
        int[] iArr2 = null;
        if (this.mKeyHdrAvailablePhotoModes != null) {
            iArr2 = (int[]) cameraCharacteristics.get(this.mKeyHdrAvailablePhotoModes);
        }
        if (iArr2 != null) {
            for (int i2 : iArr2) {
                LogHelper.d(TAG, "photo support value: " + i2);
                if (i2 != CAM_HDR_FEATURE_HDR_MODE_ON[0]) {
                    if (i2 != CAM_HDR_FEATURE_HDR_MODE_AUTO[0]) {
                        if (i2 != CAM_HDR_FEATURE_HDR_MODE_VIDEO_ON[0]) {
                            if (i2 == CAM_HDR_FEATURE_HDR_MODE_VIDEO_AUTO[0]) {
                                this.mIsSensorDetectionHdrAutoSupported = true;
                            }
                        } else {
                            this.mIsSensorDetectionHdrOnSupported = true;
                        }
                    } else {
                        arrayList.add("auto");
                    }
                } else {
                    this.mIsVendorHdrSupported = true;
                }
            }
        }
        this.mHdr.setSupportedPlatformValues(arrayList);
        this.mHdr.setSupportedEntryValues(arrayList);
        this.mHdr.setEntryValues(arrayList);
    }

    private void initHdrVideoSettingValues(CameraCharacteristics cameraCharacteristics) {
        int[] iArr;
        LogHelper.d(TAG, "initHdrVideoSettingValues ");
        ArrayList arrayList = new ArrayList();
        arrayList.add("off");
        if (this.mKeyHdrAvailableVideoModes != null) {
            iArr = (int[]) cameraCharacteristics.get(this.mKeyHdrAvailableVideoModes);
        } else {
            iArr = null;
        }
        if (iArr != null) {
            for (int i : iArr) {
                LogHelper.d(TAG, "video support value: " + i);
                if (i == CAM_HDR_FEATURE_HDR_MODE_VIDEO_ON[0]) {
                    arrayList.add("on");
                    this.mIsVendorHdrSupported = true;
                } else if (i == CAM_HDR_FEATURE_HDR_MODE_VIDEO_AUTO[0]) {
                    arrayList.add("auto");
                }
            }
        }
        this.mHdr.setSupportedPlatformValues(arrayList);
        this.mHdr.setSupportedEntryValues(arrayList);
        this.mHdr.setEntryValues(arrayList);
    }
}
