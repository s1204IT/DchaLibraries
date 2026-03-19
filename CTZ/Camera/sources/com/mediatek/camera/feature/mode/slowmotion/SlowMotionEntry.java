package com.mediatek.camera.feature.mode.slowmotion;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.os.Build;
import android.util.Range;
import android.util.Size;
import com.mediatek.camera.R;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.loader.DeviceDescription;
import com.mediatek.camera.common.loader.FeatureEntryBase;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.portability.CamcorderProfileEx;
import com.mediatek.camera.portability.SystemProperties;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class SlowMotionEntry extends FeatureEntryBase {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(SlowMotionEntry.class.getSimpleName());
    private static final int[] sMtkSlowQualities = {2220, 2222};
    private static final int[] sSlowQualities = {2002, 2003, 2004, 2005};
    private String[] mStringSupportedIds;
    private List<String> mSupportedIdList;

    public SlowMotionEntry(Context context, Resources resources) {
        super(context, resources);
        this.mSupportedIdList = new ArrayList();
    }

    @Override
    public boolean isSupport(CameraDeviceManagerFactory.CameraApi cameraApi, Activity activity) {
        return isSlowMotionSupported(activity);
    }

    public String getFeatureEntryName() {
        return SlowMotionEntry.class.getName();
    }

    @Override
    public Class getType() {
        return ICameraMode.class;
    }

    @Override
    public Object createInstance() {
        return new SlowMotionMode();
    }

    @Override
    public IAppUi.ModeItem getModeItem() {
        IAppUi.ModeItem modeItem = new IAppUi.ModeItem();
        modeItem.mModeUnselectedIcon = this.mResources.getDrawable(R.drawable.ic_slow_motion_mode_off);
        modeItem.mModeSelectedIcon = this.mResources.getDrawable(R.drawable.ic_slow_motion_mode_on);
        modeItem.mShutterIcon = this.mResources.getDrawable(R.drawable.ic_slow_motion_shutter);
        modeItem.mType = "Video";
        modeItem.mPriority = 70;
        modeItem.mClassName = getFeatureEntryName();
        modeItem.mModeName = String.valueOf(this.mResources.getString(R.string.slow_motion_title));
        modeItem.mSupportedCameraIds = this.mStringSupportedIds;
        return modeItem;
    }

    private boolean isSlowMotionSupported(Activity activity) {
        boolean z = !isThirdPartyIntent(activity) && isFeatureOptionSupported() && isPlatFormSupported();
        LogHelper.i(TAG, "[isSlowMotionSupported] isSupported = " + z);
        return z;
    }

    private boolean isFeatureOptionSupported() {
        boolean z = SystemProperties.getInt("ro.vendor.mtk_slow_motion_support", 0) == 1;
        LogHelper.d(TAG, "[isFeatureOptionSupported]  slow motion enable = " + z);
        return z;
    }

    private boolean isPlatFormSupported() {
        initPlatformSupportedState();
        boolean z = false;
        z = false;
        if (this.mSupportedIdList != null && this.mSupportedIdList.size() >= 1) {
            this.mStringSupportedIds = new String[this.mSupportedIdList.size()];
            for (int i = 0; i < this.mSupportedIdList.size(); i++) {
                this.mStringSupportedIds[i] = this.mSupportedIdList.get(i);
                LogHelper.d(TAG, "supported slow motion id = " + this.mSupportedIdList.get(i));
            }
            z = true;
        }
        this.mSupportedIdList.clear();
        LogHelper.d(TAG, "[isPlatFormSupported] isSupported = " + z);
        return z;
    }

    private void initPlatformSupportedState() {
        if (Build.VERSION.SDK_INT < 23) {
            LogHelper.e(TAG, "[initPlatformSupportedState] sdk version is smaller than 23");
            return;
        }
        ConcurrentHashMap<String, DeviceDescription> deviceDescriptionMap = this.mDeviceSpec.getDeviceDescriptionMap();
        int size = deviceDescriptionMap.size();
        for (int i = 0; i < size; i++) {
            CameraCharacteristics cameraCharacteristics = deviceDescriptionMap.get(String.valueOf(i)).getCameraCharacteristics();
            if (cameraCharacteristics != null) {
                boolean zContains = Arrays.asList((Integer[]) convertPrimitiveArrayToObjectArray((int[]) cameraCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES), Integer.class)).contains(9);
                LogHelper.d(TAG, "CAPABILITIES contain HIGH_SPEED_VIDEO = " + zContains + "  camera id = " + i);
                if (zContains && checkerProfile(i, cameraCharacteristics) != null) {
                    this.mSupportedIdList.add(String.valueOf(i));
                }
            }
        }
    }

    private static <T> T[] convertPrimitiveArrayToObjectArray(Object obj, Class<T> cls) {
        int length = Array.getLength(obj);
        if (length == 0) {
            throw new IllegalArgumentException("Input array shouldn't be empty");
        }
        T[] tArr = (T[]) ((Object[]) Array.newInstance((Class<?>) cls, length));
        for (int i = 0; i < length; i++) {
            Array.set(tArr, i, Array.get(obj, i));
        }
        return tArr;
    }

    private CamcorderProfile checkerProfile(int i, CameraCharacteristics cameraCharacteristics) {
        CamcorderProfile camcorderProfileFindProfileForRange = findProfileForRange(i, cameraCharacteristics, sMtkSlowQualities);
        if (camcorderProfileFindProfileForRange == null) {
            camcorderProfileFindProfileForRange = findProfileForRange(i, cameraCharacteristics, sSlowQualities);
        }
        LogHelper.d(TAG, "[checkerProfile] cameraId =  " + i + "profile = " + camcorderProfileFindProfileForRange);
        return camcorderProfileFindProfileForRange;
    }

    private Range<Integer> getHighSpeedFixedFpsRangeForSize(Size size, CameraCharacteristics cameraCharacteristics) {
        try {
            for (Range<Integer> range : ((StreamConfigurationMap) cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)).getHighSpeedVideoFpsRangesFor(size)) {
                if (((Integer) range.getLower()).equals(range.getUpper())) {
                    LogHelper.d(TAG, "[getHighSpeedFpsRangeForSize] range = " + range.toString());
                    return range;
                }
            }
            return null;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    private CamcorderProfile findProfileForRange(int i, CameraCharacteristics cameraCharacteristics, int[] iArr) {
        for (int i2 = 0; i2 < iArr.length; i2++) {
            if (CamcorderProfile.hasProfile(i, iArr[i2])) {
                CamcorderProfile profile = CamcorderProfileEx.getProfile(i, iArr[i2]);
                Range<Integer> highSpeedFixedFpsRangeForSize = getHighSpeedFixedFpsRangeForSize(new Size(profile.videoFrameWidth, profile.videoFrameHeight), cameraCharacteristics);
                if (highSpeedFixedFpsRangeForSize != null && ((Integer) highSpeedFixedFpsRangeForSize.getLower()).intValue() == profile.videoFrameRate) {
                    LogHelper.d(TAG, "find slow motion FrameRate is " + profile.videoFrameRate + "Camera id = " + i);
                    return profile;
                }
            }
        }
        return null;
    }
}
