package com.mediatek.camera.feature.mode.slowmotion;

import android.content.ContentValues;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.mode.video.VideoMode;
import com.mediatek.camera.common.mode.video.recorder.IRecorder;
import com.mediatek.camera.common.relation.Relation;
import java.util.List;

public class SlowMotionMode extends VideoMode {
    private static final SlowMotionGestureImpl mGestureListener = new SlowMotionGestureImpl();

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, boolean z) {
        super.init(iApp, iCameraContext, z);
        this.mCameraApi = CameraDeviceManagerFactory.CameraApi.API2;
        this.mAppUi.registerGestureListener(mGestureListener, 0);
    }

    @Override
    public void unInit() {
        super.unInit();
        this.mAppUi.unregisterGestureListener(mGestureListener);
    }

    @Override
    public String getModeKey() {
        return getClass().getName();
    }

    @Override
    protected IRecorder.RecorderSpec modifyRecorderSpec(IRecorder.RecorderSpec recorderSpec, boolean z) {
        recorderSpec.captureRate = getProfile().videoFrameRate;
        recorderSpec.profile = getProfile();
        recorderSpec.videoFrameRate = 30;
        return recorderSpec;
    }

    @Override
    protected void initCameraDevice(CameraDeviceManagerFactory.CameraApi cameraApi) {
        this.mCameraDevice = new SlowMotionDevice(this.mApp.getActivity(), this.mCameraContext);
    }

    @Override
    protected ContentValues modifyContentValues(ContentValues contentValues) {
        return contentValues;
    }

    @Override
    protected Relation getPreviewedRestriction() {
        Relation relation = SlowMotionRestriction.getPreviewRelation().getRelation("preview", true);
        String strQueryValue = this.mSettingManager.getSettingController().queryValue("key_flash");
        if (!"on".equals(strQueryValue)) {
            strQueryValue = "off";
        }
        relation.addBody("key_flash", strQueryValue, "on,off");
        String strQueryValue2 = this.mSettingManager.getSettingController().queryValue("key_video_quality");
        relation.addBody("key_video_quality", strQueryValue2, strQueryValue2);
        return relation;
    }

    @Override
    protected List<Relation> getRecordedRestriction(boolean z) {
        return null;
    }

    @Override
    protected void updateModeDefinedCameraApi() {
        this.mCameraApi = CameraDeviceManagerFactory.CameraApi.API2;
    }
}
