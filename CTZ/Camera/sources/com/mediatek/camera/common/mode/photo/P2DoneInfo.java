package com.mediatek.camera.common.mode.photo;

import android.content.Context;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.loader.DeviceDescription;
import com.mediatek.camera.common.mode.CameraApiHelper;

public class P2DoneInfo {
    private static CaptureRequest.Key<int[]> mKeyP2NotificationRequest;
    private static CaptureResult.Key<int[]> mKeyP2NotificationResult;
    private static final LogUtil.Tag TAG = new LogUtil.Tag(P2DoneInfo.class.getSimpleName());
    private static final int[] P2DONE_SUPPORT = {1};
    private static boolean mIsSupport = false;

    public static void setCameraCharacteristics(Context context, int i) {
        DeviceDescription deviceDescription = CameraApiHelper.getDeviceSpec(context).getDeviceDescriptionMap().get(String.valueOf(i));
        if (deviceDescription != null) {
            mIsSupport = deviceDescription.isSpeedUpSupport().booleanValue();
        }
        LogHelper.d(TAG, "[setCameraCharacteristics], mIsSupport = " + mIsSupport);
        if (mIsSupport && deviceDescription != null) {
            mKeyP2NotificationRequest = deviceDescription.getKeyP2NotificationRequestMode();
            mKeyP2NotificationResult = deviceDescription.getKeyP2NotificationResult();
        }
    }

    public static boolean enableP2Done(CaptureRequest.Builder builder) {
        if (mIsSupport) {
            builder.set(mKeyP2NotificationRequest, P2DONE_SUPPORT);
            return true;
        }
        return false;
    }

    public static boolean checkP2DoneResult(CaptureResult captureResult) {
        int[] iArr;
        return mIsSupport && (iArr = (int[]) captureResult.get(mKeyP2NotificationResult)) != null && iArr[0] == P2DONE_SUPPORT[0];
    }
}
