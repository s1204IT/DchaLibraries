package com.mediatek.camera.common.setting;

import android.hardware.camera2.CaptureRequest;
import android.view.Surface;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2CaptureSessionProxy;
import com.mediatek.camera.common.mode.photo.device.CaptureSurface;
import com.mediatek.camera.common.setting.ISettingManager;

public class SettingDevice2RequesterProxy implements ISettingManager.SettingDevice2Requester {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(SettingDevice2RequesterProxy.class.getSimpleName());
    private ISettingManager.SettingDevice2Requester mModeDevice2RequesterImpl;

    @Override
    public void createAndChangeRepeatingRequest() {
        synchronized (this) {
            if (this.mModeDevice2RequesterImpl != null) {
                this.mModeDevice2RequesterImpl.createAndChangeRepeatingRequest();
            }
        }
    }

    @Override
    public CaptureRequest.Builder createAndConfigRequest(int i) {
        synchronized (this) {
            if (this.mModeDevice2RequesterImpl != null) {
                return this.mModeDevice2RequesterImpl.createAndConfigRequest(i);
            }
            return null;
        }
    }

    @Override
    public Camera2CaptureSessionProxy getCurrentCaptureSession() {
        synchronized (this) {
            if (this.mModeDevice2RequesterImpl != null) {
                return this.mModeDevice2RequesterImpl.getCurrentCaptureSession();
            }
            return null;
        }
    }

    @Override
    public void requestRestartSession() {
        synchronized (this) {
            if (this.mModeDevice2RequesterImpl != null) {
                this.mModeDevice2RequesterImpl.requestRestartSession();
            }
        }
    }

    @Override
    public int getRepeatingTemplateType() {
        synchronized (this) {
            if (this.mModeDevice2RequesterImpl != null) {
                return this.mModeDevice2RequesterImpl.getRepeatingTemplateType();
            }
            return -1;
        }
    }

    public void updateModeDevice2Requester(ISettingManager.SettingDevice2Requester settingDevice2Requester) {
        synchronized (this) {
            this.mModeDevice2RequesterImpl = settingDevice2Requester;
        }
    }

    @Override
    public CaptureSurface getModeSharedCaptureSurface() throws IllegalStateException {
        return this.mModeDevice2RequesterImpl.getModeSharedCaptureSurface();
    }

    @Override
    public Surface getModeSharedPreviewSurface() throws IllegalStateException {
        return this.mModeDevice2RequesterImpl.getModeSharedPreviewSurface();
    }

    @Override
    public Surface getModeSharedThumbnailSurface() throws IllegalStateException {
        return this.mModeDevice2RequesterImpl.getModeSharedThumbnailSurface();
    }
}
