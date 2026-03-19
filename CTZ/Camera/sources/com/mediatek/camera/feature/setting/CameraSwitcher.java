package com.mediatek.camera.feature.setting;

import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import com.mediatek.camera.R;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingBase;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.portability.SystemProperties;
import java.util.ArrayList;
import java.util.List;

public class CameraSwitcher extends SettingBase {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(CameraSwitcher.class.getSimpleName());
    private String mFacing;
    private KeyEventListenerImpl mKeyEventListener;
    private View mSwitcherView;
    private String mLastRequestCameraId = "0";
    private String mPreBackCamera = "0";
    private String[] mIdList = null;

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, ISettingManager.SettingController settingController) {
        super.init(iApp, iCameraContext, settingController);
        this.mFacing = this.mDataStore.getValue("key_camera_switcher", "back", getStoreScope());
        int numberOfCameras = Camera.getNumberOfCameras();
        if (numberOfCameras > 1) {
            List<String> camerasFacing = getCamerasFacing(numberOfCameras);
            if (camerasFacing.size() == 0) {
                return;
            }
            if (camerasFacing.size() == 1) {
                this.mFacing = camerasFacing.get(0);
                setValue(this.mFacing);
                return;
            } else {
                setSupportedPlatformValues(camerasFacing);
                setSupportedEntryValues(camerasFacing);
                setEntryValues(camerasFacing);
                this.mSwitcherView = initView();
                this.mAppUi.addToQuickSwitcher(this.mSwitcherView, 0);
            }
        } else if (numberOfCameras == 1) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(0, cameraInfo);
            if (cameraInfo.facing == 0) {
                this.mFacing = "back";
            } else if (cameraInfo.facing == 1) {
                this.mFacing = "front";
            }
        }
        setValue(this.mFacing);
        if (SystemProperties.getInt("vendor.debug.camera.single_main2", 0) == 1) {
            try {
                this.mIdList = ((CameraManager) this.mActivity.getApplicationContext().getSystemService("camera")).getCameraIdList();
                if (this.mIdList != null) {
                    for (String str : this.mIdList) {
                        LogHelper.d(TAG, "<getCameraIdList> id is " + str);
                    }
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        this.mKeyEventListener = new KeyEventListenerImpl();
        this.mApp.registerKeyEventListener(this.mKeyEventListener, Integer.MAX_VALUE);
    }

    @Override
    public void unInit() {
        if (this.mSwitcherView != null) {
            this.mAppUi.removeFromQuickSwitcher(this.mSwitcherView);
        }
        if (this.mKeyEventListener != null) {
            this.mApp.unRegisterKeyEventListener(this.mKeyEventListener);
        }
        if (SystemProperties.getInt("mtk.camera.switch.camera.debug", 0) == 1) {
            this.mLastRequestCameraId = "0";
        }
    }

    @Override
    public void postRestrictionAfterInitialized() {
    }

    @Override
    public void refreshViewEntry() {
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (CameraSwitcher.this.mSwitcherView != null) {
                    if (CameraSwitcher.this.getEntryValues().size() <= 1) {
                        CameraSwitcher.this.mSwitcherView.setVisibility(8);
                    } else {
                        CameraSwitcher.this.mSwitcherView.setVisibility(0);
                    }
                }
            }
        });
    }

    @Override
    public ICameraSetting.SettingType getSettingType() {
        return ICameraSetting.SettingType.PHOTO_AND_VIDEO;
    }

    @Override
    public String getKey() {
        return "key_camera_switcher";
    }

    @Override
    public ICameraSetting.IParametersConfigure getParametersConfigure() {
        return null;
    }

    @Override
    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure() {
        return null;
    }

    @Override
    public String getStoreScope() {
        return this.mDataStore.getGlobalScope();
    }

    private List<String> getCamerasFacing(int i) {
        ArrayList arrayList = new ArrayList();
        for (int i2 = 0; i2 < i; i2++) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(i2, cameraInfo);
            String str = null;
            if (cameraInfo.facing == 0) {
                str = "back";
            } else if (cameraInfo.facing == 1) {
                str = "front";
            }
            if (SystemProperties.getInt("mtk.camera.switch.camera.debug", 0) == 1) {
                if (str != null) {
                    arrayList.add(str);
                }
            } else if (!arrayList.contains(str)) {
                arrayList.add(str);
            }
        }
        return arrayList;
    }

    private View initView() {
        View viewInflate = this.mApp.getActivity().getLayoutInflater().inflate(R.layout.camera_switcher, (ViewGroup) null);
        viewInflate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (SystemProperties.getInt("mtk.camera.switch.camera.debug", 0) == 1) {
                    LogHelper.d(CameraSwitcher.TAG, "[onClick], enter debug mode.");
                    CameraSwitcher.this.switchCameraInDebugMode();
                } else if (SystemProperties.getInt("vendor.debug.camera.single_main2", 0) == 1) {
                    LogHelper.d(CameraSwitcher.TAG, "[onClick], enter main2 debug mode.");
                    CameraSwitcher.this.switchCameraInDebugMain2();
                } else {
                    LogHelper.d(CameraSwitcher.TAG, "[onClick], enter camera normal mode.");
                    CameraSwitcher.this.switchCameraInNormal();
                }
            }
        });
        viewInflate.setContentDescription(this.mFacing);
        return viewInflate;
    }

    private void switchCameraInDebugMain2() {
        int i;
        if (this.mIdList == null || this.mIdList.length == 0) {
            return;
        }
        String str = this.mFacing.equals("back") ? "front" : "back";
        String value = this.mDataStore.getValue("key_stereo_main2", "0", getStoreScope());
        LogHelper.d(TAG, "[switchCameraInDebugMain2] last cameraId = " + value);
        int i2 = 0;
        while (true) {
            if (i2 < this.mIdList.length) {
                if (!this.mIdList[i2].equals(value)) {
                    i2++;
                } else {
                    i = i2 + 1;
                    break;
                }
            } else {
                i = 0;
                break;
            }
        }
        if (i > this.mIdList.length - 1 || i < 0) {
            i = 0;
        }
        String str2 = this.mIdList[i];
        LogHelper.d(TAG, "[switchCameraInDebugMain2] current cameraId = " + str2);
        if (this.mApp.notifyCameraSelected(str2)) {
            LogHelper.d(TAG, "[switchCameraInDebugMain2] switch to " + str2 + " success");
            this.mDataStore.setValue("key_stereo_main2", str2, getStoreScope(), true);
            this.mFacing = str;
            this.mDataStore.setValue("key_camera_switcher", this.mFacing, getStoreScope(), true);
        }
        this.mSwitcherView.setContentDescription(this.mFacing);
    }

    private void switchCameraInNormal() {
        String str;
        String str2 = this.mFacing.equals("back") ? "front" : "back";
        LogHelper.d(TAG, "[switchCameraInNormal], switch camera to " + str2);
        if (this.mFacing.equals("back")) {
            str = CameraUtil.getCamIdsByFacing(false, this.mApp.getActivity()).get(0);
        } else {
            str = CameraUtil.getCamIdsByFacing(true, this.mApp.getActivity()).get(0);
        }
        if (this.mApp.notifyCameraSelected(str)) {
            LogHelper.d(TAG, "[switchCameraInNormal], switch camera success.");
            this.mFacing = str2;
            this.mDataStore.setValue("key_camera_switcher", this.mFacing, getStoreScope(), true);
        }
        this.mSwitcherView.setContentDescription(this.mFacing);
    }

    private void switchCameraInDebugMode() {
        LogHelper.d(TAG, "[switchCameraInDebugMode]");
        String string = SystemProperties.getString("mtk.camera.switch.id.debug", "back-0");
        String str = "0";
        List<String> camIdsByFacing = CameraUtil.getCamIdsByFacing(true, this.mApp.getActivity());
        List<String> camIdsByFacing2 = CameraUtil.getCamIdsByFacing(false, this.mApp.getActivity());
        int i = Integer.parseInt(string.substring(string.indexOf("-") + 1));
        if (string.contains("back") && camIdsByFacing == null) {
            LogHelper.e(TAG, "[switchCameraInDebugMode] backIds is null");
            return;
        }
        if (string.contains("front") && camIdsByFacing2 == null) {
            LogHelper.e(TAG, "[switchCameraInDebugMode] frontIds is null");
            return;
        }
        if (string.contains("back")) {
            if (i < camIdsByFacing.size()) {
                str = camIdsByFacing.get(i);
            } else {
                LogHelper.e(TAG, "[switchCameraInDebugMode] invalid back camera index " + i);
                return;
            }
        } else if (string.contains("front")) {
            if (i < camIdsByFacing2.size()) {
                str = camIdsByFacing2.get(i);
            } else {
                LogHelper.e(TAG, "[switchCameraInDebugMode] invalid front camera index " + i);
                return;
            }
        }
        LogHelper.i(TAG, "[switchCameraInDebugMode] requestCamera " + string + ",resultCameraId " + str + ",mLastRequestCameraId " + this.mLastRequestCameraId);
        if (str.equals(this.mLastRequestCameraId)) {
            return;
        }
        this.mLastRequestCameraId = str;
        this.mApp.notifyCameraSelected(str);
        this.mSwitcherView.setContentDescription(string);
        this.mFacing = string.substring(0, string.indexOf("-"));
        this.mDataStore.setValue("key_camera_switcher", this.mFacing, getStoreScope(), true);
    }

    private class KeyEventListenerImpl implements IApp.KeyEventListener {
        private KeyEventListenerImpl() {
        }

        @Override
        public boolean onKeyDown(int i, KeyEvent keyEvent) {
            if (i != 31 || !CameraUtil.isSpecialKeyCodeEnabled()) {
                return false;
            }
            return true;
        }

        @Override
        public boolean onKeyUp(int i, KeyEvent keyEvent) {
            if (i == 31 && CameraUtil.isSpecialKeyCodeEnabled()) {
                if (CameraSwitcher.this.mSwitcherView != null && CameraSwitcher.this.mSwitcherView.getVisibility() == 0 && CameraSwitcher.this.mSwitcherView.isEnabled()) {
                    CameraSwitcher.this.mSwitcherView.performClick();
                    return true;
                }
                return true;
            }
            return false;
        }
    }
}
