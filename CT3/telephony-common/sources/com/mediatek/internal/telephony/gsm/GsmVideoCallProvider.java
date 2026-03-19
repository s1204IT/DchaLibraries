package com.mediatek.internal.telephony.gsm;

import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.RemoteException;
import android.telecom.VideoProfile;
import android.view.Surface;
import com.android.internal.os.SomeArgs;
import com.mediatek.internal.telephony.gsm.IGsmVideoCallProvider;

public abstract class GsmVideoCallProvider {
    private static final int MSG_MTK_BASE = 100;
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
    private static final int MSG_SET_UI_MODE = 100;
    private static final int MSG_SET_ZOOM = 6;
    private final GsmVideoCallProviderBinder mBinder;
    private IGsmVideoCallCallback mCallback;
    private final Handler mProviderHandler;
    private HandlerThread mProviderHandlerThread = new HandlerThread("ProviderHandlerThread");

    public abstract void onRequestCallDataUsage();

    public abstract void onRequestCameraCapabilities();

    public abstract void onSendSessionModifyRequest(VideoProfile videoProfile, VideoProfile videoProfile2);

    public abstract void onSendSessionModifyResponse(VideoProfile videoProfile);

    public abstract void onSetCamera(String str);

    public abstract void onSetDeviceOrientation(int i);

    public abstract void onSetDisplaySurface(Surface surface);

    public abstract void onSetPauseImage(Uri uri);

    public abstract void onSetPreviewSurface(Surface surface);

    public abstract void onSetUIMode(int i);

    public abstract void onSetZoom(float f);

    private final class GsmVideoCallProviderBinder extends IGsmVideoCallProvider.Stub {
        GsmVideoCallProviderBinder(GsmVideoCallProvider this$0, GsmVideoCallProviderBinder gsmVideoCallProviderBinder) {
            this();
        }

        private GsmVideoCallProviderBinder() {
        }

        public void setCallback(IGsmVideoCallCallback callback) {
            GsmVideoCallProvider.this.mProviderHandler.obtainMessage(1, callback).sendToTarget();
        }

        public void setCamera(String cameraId) {
            GsmVideoCallProvider.this.mProviderHandler.obtainMessage(2, cameraId).sendToTarget();
        }

        public void setPreviewSurface(Surface surface) {
            GsmVideoCallProvider.this.mProviderHandler.obtainMessage(3, surface).sendToTarget();
        }

        public void setDisplaySurface(Surface surface) {
            GsmVideoCallProvider.this.mProviderHandler.obtainMessage(4, surface).sendToTarget();
        }

        public void setDeviceOrientation(int rotation) {
            GsmVideoCallProvider.this.mProviderHandler.obtainMessage(5, rotation, 0).sendToTarget();
        }

        public void setZoom(float value) {
            GsmVideoCallProvider.this.mProviderHandler.obtainMessage(6, Float.valueOf(value)).sendToTarget();
        }

        public void sendSessionModifyRequest(VideoProfile fromProfile, VideoProfile toProfile) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = fromProfile;
            args.arg2 = toProfile;
            GsmVideoCallProvider.this.mProviderHandler.obtainMessage(7, args).sendToTarget();
        }

        public void sendSessionModifyResponse(VideoProfile responseProfile) {
            GsmVideoCallProvider.this.mProviderHandler.obtainMessage(8, responseProfile).sendToTarget();
        }

        public void requestCameraCapabilities() {
            GsmVideoCallProvider.this.mProviderHandler.obtainMessage(9).sendToTarget();
        }

        public void requestCallDataUsage() {
            GsmVideoCallProvider.this.mProviderHandler.obtainMessage(10).sendToTarget();
        }

        public void setPauseImage(Uri uri) {
            GsmVideoCallProvider.this.mProviderHandler.obtainMessage(11, uri).sendToTarget();
        }

        public void setUIMode(int mode) {
            GsmVideoCallProvider.this.mProviderHandler.obtainMessage(100, Integer.valueOf(mode)).sendToTarget();
        }
    }

    public GsmVideoCallProvider() {
        this.mProviderHandlerThread.start();
        this.mProviderHandler = new Handler(this.mProviderHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        GsmVideoCallProvider.this.mCallback = (IGsmVideoCallCallback) msg.obj;
                        return;
                    case 2:
                        GsmVideoCallProvider.this.onSetCamera((String) msg.obj);
                        return;
                    case 3:
                        GsmVideoCallProvider.this.onSetPreviewSurface((Surface) msg.obj);
                        return;
                    case 4:
                        GsmVideoCallProvider.this.onSetDisplaySurface((Surface) msg.obj);
                        return;
                    case 5:
                        GsmVideoCallProvider.this.onSetDeviceOrientation(msg.arg1);
                        return;
                    case 6:
                        GsmVideoCallProvider.this.onSetZoom(((Float) msg.obj).floatValue());
                        return;
                    case 7:
                        SomeArgs args = (SomeArgs) msg.obj;
                        try {
                            VideoProfile fromProfile = (VideoProfile) args.arg1;
                            VideoProfile toProfile = (VideoProfile) args.arg2;
                            GsmVideoCallProvider.this.onSendSessionModifyRequest(fromProfile, toProfile);
                            return;
                        } finally {
                            args.recycle();
                        }
                    case 8:
                        GsmVideoCallProvider.this.onSendSessionModifyResponse((VideoProfile) msg.obj);
                        return;
                    case 9:
                        GsmVideoCallProvider.this.onRequestCameraCapabilities();
                        return;
                    case 10:
                        GsmVideoCallProvider.this.onRequestCallDataUsage();
                        return;
                    case 11:
                        GsmVideoCallProvider.this.onSetPauseImage((Uri) msg.obj);
                        return;
                    case 100:
                        GsmVideoCallProvider.this.onSetUIMode(((Integer) msg.obj).intValue());
                        return;
                    default:
                        return;
                }
            }
        };
        this.mBinder = new GsmVideoCallProviderBinder(this, null);
    }

    public final IGsmVideoCallProvider getInterface() {
        return this.mBinder;
    }

    public void receiveSessionModifyRequest(VideoProfile VideoProfile) {
        if (this.mCallback == null) {
            return;
        }
        try {
            this.mCallback.receiveSessionModifyRequest(VideoProfile);
        } catch (RemoteException e) {
        }
    }

    public void receiveSessionModifyResponse(int status, VideoProfile requestedProfile, VideoProfile responseProfile) {
        if (this.mCallback == null) {
            return;
        }
        try {
            this.mCallback.receiveSessionModifyResponse(status, requestedProfile, responseProfile);
        } catch (RemoteException e) {
        }
    }

    public void handleCallSessionEvent(int event) {
        if (this.mCallback == null) {
            return;
        }
        try {
            this.mCallback.handleCallSessionEvent(event);
        } catch (RemoteException e) {
        }
    }

    public void changePeerDimensions(int width, int height) {
        if (this.mCallback == null) {
            return;
        }
        try {
            this.mCallback.changePeerDimensions(width, height);
        } catch (RemoteException e) {
        }
    }

    public void changePeerDimensionsWithAngle(int width, int height, int rotation) {
        if (this.mCallback == null) {
            return;
        }
        try {
            this.mCallback.changePeerDimensionsWithAngle(width, height, rotation);
        } catch (RemoteException e) {
        }
    }

    public void changeCallDataUsage(long dataUsage) {
        if (this.mCallback == null) {
            return;
        }
        try {
            this.mCallback.changeCallDataUsage(dataUsage);
        } catch (RemoteException e) {
        }
    }

    public void changeCameraCapabilities(VideoProfile.CameraCapabilities CameraCapabilities) {
        if (this.mCallback == null) {
            return;
        }
        try {
            this.mCallback.changeCameraCapabilities(CameraCapabilities);
        } catch (RemoteException e) {
        }
    }

    public void changeVideoQuality(int videoQuality) {
        if (this.mCallback == null) {
            return;
        }
        try {
            this.mCallback.changeVideoQuality(videoQuality);
        } catch (RemoteException e) {
        }
    }
}
