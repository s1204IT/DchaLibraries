package com.android.ims.internal;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.telecom.CameraCapabilities;
import android.telecom.VideoProfile;
import android.view.Surface;
import com.android.ims.internal.IImsVideoCallProvider;

public abstract class ImsVideoCallProvider {
    private static final int MSG_REQUEST_CALL_DATA_USAGE = 10;
    private static final int MSG_REQUEST_CAMERA_CAPABILITIES = 9;
    private static final int MSG_SEND_SESSION_MODIFY_REQUEST = 7;
    private static final int MSG_SEND_SESSION_MODIFY_RESPONSE = 8;
    private static final int MSG_SET_CALLBACK = 1;
    private static final int MSG_SET_CAMERA = 2;
    private static final int MSG_SET_DEVICE_ORIENTATION = 5;
    private static final int MSG_SET_DISPLAY_SURFACE = 4;
    private static final int MSG_SET_PAUSE_IMAGE = 11;
    private static final int MSG_SET_PREVIEW_SURFACE = 3;
    private static final int MSG_SET_ZOOM = 6;
    private IImsVideoCallCallback mCallback;
    private final Handler mProviderHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    ImsVideoCallProvider.this.mCallback = (IImsVideoCallCallback) msg.obj;
                    break;
                case 2:
                    ImsVideoCallProvider.this.onSetCamera((String) msg.obj);
                    break;
                case 3:
                    ImsVideoCallProvider.this.onSetPreviewSurface((Surface) msg.obj);
                    break;
                case 4:
                    ImsVideoCallProvider.this.onSetDisplaySurface((Surface) msg.obj);
                    break;
                case 5:
                    ImsVideoCallProvider.this.onSetDeviceOrientation(msg.arg1);
                    break;
                case 6:
                    ImsVideoCallProvider.this.onSetZoom(((Float) msg.obj).floatValue());
                    break;
                case 7:
                    ImsVideoCallProvider.this.onSendSessionModifyRequest((VideoProfile) msg.obj);
                    break;
                case 8:
                    ImsVideoCallProvider.this.onSendSessionModifyResponse((VideoProfile) msg.obj);
                    break;
                case 9:
                    ImsVideoCallProvider.this.onRequestCameraCapabilities();
                    break;
                case 10:
                    ImsVideoCallProvider.this.onRequestCallDataUsage();
                    break;
                case 11:
                    ImsVideoCallProvider.this.onSetPauseImage((String) msg.obj);
                    break;
            }
        }
    };
    private final ImsVideoCallProviderBinder mBinder = new ImsVideoCallProviderBinder();

    public abstract void onRequestCallDataUsage();

    public abstract void onRequestCameraCapabilities();

    public abstract void onSendSessionModifyRequest(VideoProfile videoProfile);

    public abstract void onSendSessionModifyResponse(VideoProfile videoProfile);

    public abstract void onSetCamera(String str);

    public abstract void onSetDeviceOrientation(int i);

    public abstract void onSetDisplaySurface(Surface surface);

    public abstract void onSetPauseImage(String str);

    public abstract void onSetPreviewSurface(Surface surface);

    public abstract void onSetZoom(float f);

    private final class ImsVideoCallProviderBinder extends IImsVideoCallProvider.Stub {
        private ImsVideoCallProviderBinder() {
        }

        public void setCallback(IImsVideoCallCallback callback) {
            ImsVideoCallProvider.this.mProviderHandler.obtainMessage(1, callback).sendToTarget();
        }

        public void setCamera(String cameraId) {
            ImsVideoCallProvider.this.mProviderHandler.obtainMessage(2, cameraId).sendToTarget();
        }

        public void setPreviewSurface(Surface surface) {
            ImsVideoCallProvider.this.mProviderHandler.obtainMessage(3, surface).sendToTarget();
        }

        public void setDisplaySurface(Surface surface) {
            ImsVideoCallProvider.this.mProviderHandler.obtainMessage(4, surface).sendToTarget();
        }

        public void setDeviceOrientation(int rotation) {
            ImsVideoCallProvider.this.mProviderHandler.obtainMessage(5, Integer.valueOf(rotation)).sendToTarget();
        }

        public void setZoom(float value) {
            ImsVideoCallProvider.this.mProviderHandler.obtainMessage(6, Float.valueOf(value)).sendToTarget();
        }

        public void sendSessionModifyRequest(VideoProfile requestProfile) {
            ImsVideoCallProvider.this.mProviderHandler.obtainMessage(7, requestProfile).sendToTarget();
        }

        public void sendSessionModifyResponse(VideoProfile responseProfile) {
            ImsVideoCallProvider.this.mProviderHandler.obtainMessage(8, responseProfile).sendToTarget();
        }

        public void requestCameraCapabilities() {
            ImsVideoCallProvider.this.mProviderHandler.obtainMessage(9).sendToTarget();
        }

        public void requestCallDataUsage() {
            ImsVideoCallProvider.this.mProviderHandler.obtainMessage(10).sendToTarget();
        }

        public void setPauseImage(String uri) {
            ImsVideoCallProvider.this.mProviderHandler.obtainMessage(11, uri).sendToTarget();
        }
    }

    public final IImsVideoCallProvider getInterface() {
        return this.mBinder;
    }

    public void receiveSessionModifyRequest(VideoProfile VideoProfile) {
        if (this.mCallback != null) {
            try {
                this.mCallback.receiveSessionModifyRequest(VideoProfile);
            } catch (RemoteException e) {
            }
        }
    }

    public void receiveSessionModifyResponse(int status, VideoProfile requestedProfile, VideoProfile responseProfile) {
        if (this.mCallback != null) {
            try {
                this.mCallback.receiveSessionModifyResponse(status, requestedProfile, responseProfile);
            } catch (RemoteException e) {
            }
        }
    }

    public void handleCallSessionEvent(int event) {
        if (this.mCallback != null) {
            try {
                this.mCallback.handleCallSessionEvent(event);
            } catch (RemoteException e) {
            }
        }
    }

    public void changePeerDimensions(int width, int height) {
        if (this.mCallback != null) {
            try {
                this.mCallback.changePeerDimensions(width, height);
            } catch (RemoteException e) {
            }
        }
    }

    public void changeCallDataUsage(int dataUsage) {
        if (this.mCallback != null) {
            try {
                this.mCallback.changeCallDataUsage(dataUsage);
            } catch (RemoteException e) {
            }
        }
    }

    public void changeCameraCapabilities(CameraCapabilities CameraCapabilities) {
        if (this.mCallback != null) {
            try {
                this.mCallback.changeCameraCapabilities(CameraCapabilities);
            } catch (RemoteException e) {
            }
        }
    }
}
