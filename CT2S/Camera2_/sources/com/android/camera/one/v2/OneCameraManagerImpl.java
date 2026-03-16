package com.android.camera.one.v2;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.util.DisplayMetrics;
import com.android.camera.SoundPlayer;
import com.android.camera.debug.Log;
import com.android.camera.one.OneCamera;
import com.android.camera.one.OneCameraManager;
import com.android.camera.util.Size;

public class OneCameraManagerImpl extends OneCameraManager {
    private static final Log.Tag TAG = new Log.Tag("OneCameraMgrImpl2");
    private final CameraManager mCameraManager;
    private final Context mContext;
    private final DisplayMetrics mDisplayMetrics;
    private final int mMaxMemoryMB;
    private final SoundPlayer mSoundPlayer;

    public OneCameraManagerImpl(Context context, CameraManager cameraManager, int maxMemoryMB, DisplayMetrics displayMetrics, SoundPlayer soundPlayer) {
        this.mContext = context;
        this.mCameraManager = cameraManager;
        this.mMaxMemoryMB = maxMemoryMB;
        this.mDisplayMetrics = displayMetrics;
        this.mSoundPlayer = soundPlayer;
    }

    @Override
    public void open(OneCamera.Facing facing, final boolean useHdr, final Size pictureSize, final OneCamera.OpenCallback openCallback, Handler handler) {
        try {
            String cameraId = getCameraId(facing);
            Log.i(TAG, "Opening Camera ID " + cameraId);
            this.mCameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                private boolean isFirstCallback = true;

                @Override
                public void onDisconnected(CameraDevice device) {
                    if (this.isFirstCallback) {
                        this.isFirstCallback = false;
                        device.close();
                        openCallback.onCameraClosed();
                    }
                }

                @Override
                public void onClosed(CameraDevice device) {
                    if (this.isFirstCallback) {
                        this.isFirstCallback = false;
                        openCallback.onCameraClosed();
                    }
                }

                @Override
                public void onError(CameraDevice device, int error) {
                    if (this.isFirstCallback) {
                        this.isFirstCallback = false;
                        device.close();
                        openCallback.onFailure();
                    }
                }

                @Override
                public void onOpened(CameraDevice device) {
                    if (this.isFirstCallback) {
                        this.isFirstCallback = false;
                        try {
                            CameraCharacteristics characteristics = OneCameraManagerImpl.this.mCameraManager.getCameraCharacteristics(device.getId());
                            OneCamera oneCamera = OneCameraCreator.create(OneCameraManagerImpl.this.mContext, useHdr, device, characteristics, pictureSize, OneCameraManagerImpl.this.mMaxMemoryMB, OneCameraManagerImpl.this.mDisplayMetrics, OneCameraManagerImpl.this.mSoundPlayer);
                            openCallback.onCameraOpened(oneCamera);
                        } catch (CameraAccessException e) {
                            Log.d(OneCameraManagerImpl.TAG, "Could not get camera characteristics");
                            openCallback.onFailure();
                        }
                    }
                }
            }, handler);
        } catch (CameraAccessException ex) {
            Log.e(TAG, "Could not open camera. " + ex.getMessage());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    openCallback.onFailure();
                }
            });
        } catch (UnsupportedOperationException ex2) {
            Log.e(TAG, "Could not open camera. " + ex2.getMessage());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    openCallback.onFailure();
                }
            });
        }
    }

    @Override
    public boolean hasCameraFacing(OneCamera.Facing facing) {
        return getFirstCameraFacing(facing == OneCamera.Facing.FRONT ? 0 : 1) != null;
    }

    private String getCameraId(OneCamera.Facing facing) {
        return facing == OneCamera.Facing.FRONT ? getFirstFrontCameraId() : getFirstBackCameraId();
    }

    public String getFirstBackCameraId() {
        Log.d(TAG, "Getting First BACK Camera");
        String cameraId = getFirstCameraFacing(1);
        if (cameraId == null) {
            throw new RuntimeException("No back-facing camera found.");
        }
        return cameraId;
    }

    public String getFirstFrontCameraId() {
        Log.d(TAG, "Getting First FRONT Camera");
        String cameraId = getFirstCameraFacing(0);
        if (cameraId == null) {
            throw new RuntimeException("No front-facing camera found.");
        }
        return cameraId;
    }

    private String getFirstCameraFacing(int facing) {
        try {
            String[] cameraIds = this.mCameraManager.getCameraIdList();
            for (String cameraId : cameraIds) {
                CameraCharacteristics characteristics = this.mCameraManager.getCameraCharacteristics(cameraId);
                if (((Integer) characteristics.get(CameraCharacteristics.LENS_FACING)).intValue() == facing) {
                    return cameraId;
                }
            }
            return null;
        } catch (CameraAccessException ex) {
            throw new RuntimeException("Unable to get camera ID", ex);
        }
    }
}
