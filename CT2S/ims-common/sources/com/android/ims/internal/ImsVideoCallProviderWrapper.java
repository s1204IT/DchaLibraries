package com.android.ims.internal;

import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.telecom.CameraCapabilities;
import android.telecom.Connection;
import android.telecom.VideoProfile;
import android.view.Surface;
import com.android.ims.internal.IImsVideoCallCallback;
import com.android.internal.os.SomeArgs;

public class ImsVideoCallProviderWrapper extends Connection.VideoProvider {
    private static final int MSG_CHANGE_CALL_DATA_USAGE = 5;
    private static final int MSG_CHANGE_CAMERA_CAPABILITIES = 6;
    private static final int MSG_CHANGE_PEER_DIMENSIONS = 4;
    private static final int MSG_HANDLE_CALL_SESSION_EVENT = 3;
    private static final int MSG_RECEIVE_SESSION_MODIFY_REQUEST = 1;
    private static final int MSG_RECEIVE_SESSION_MODIFY_RESPONSE = 2;
    private final ImsVideoCallCallback mBinder;
    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            ImsVideoCallProviderWrapper.this.mVideoCallProvider.asBinder().unlinkToDeath(this, 0);
        }
    };
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            SomeArgs args;
            switch (msg.what) {
                case 1:
                    ImsVideoCallProviderWrapper.this.receiveSessionModifyRequest((VideoProfile) msg.obj);
                    return;
                case 2:
                    args = (SomeArgs) msg.obj;
                    try {
                        int status = ((Integer) args.arg1).intValue();
                        VideoProfile requestProfile = (VideoProfile) args.arg2;
                        VideoProfile responseProfile = (VideoProfile) args.arg3;
                        ImsVideoCallProviderWrapper.this.receiveSessionModifyResponse(status, requestProfile, responseProfile);
                        return;
                    } finally {
                    }
                case 3:
                    ImsVideoCallProviderWrapper.this.handleCallSessionEvent(((Integer) msg.obj).intValue());
                    return;
                case 4:
                    args = (SomeArgs) msg.obj;
                    try {
                        int width = ((Integer) args.arg1).intValue();
                        int height = ((Integer) args.arg2).intValue();
                        ImsVideoCallProviderWrapper.this.changePeerDimensions(width, height);
                        return;
                    } finally {
                    }
                case 5:
                    ImsVideoCallProviderWrapper.this.changeCallDataUsage(msg.arg1);
                    return;
                case 6:
                    ImsVideoCallProviderWrapper.this.changeCameraCapabilities((CameraCapabilities) msg.obj);
                    return;
                default:
                    return;
            }
        }
    };
    private final IImsVideoCallProvider mVideoCallProvider;

    private final class ImsVideoCallCallback extends IImsVideoCallCallback.Stub {
        private ImsVideoCallCallback() {
        }

        public void receiveSessionModifyRequest(VideoProfile VideoProfile) {
            ImsVideoCallProviderWrapper.this.mHandler.obtainMessage(1, VideoProfile).sendToTarget();
        }

        public void receiveSessionModifyResponse(int status, VideoProfile requestProfile, VideoProfile responseProfile) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = Integer.valueOf(status);
            args.arg2 = requestProfile;
            args.arg3 = responseProfile;
            ImsVideoCallProviderWrapper.this.mHandler.obtainMessage(2, args).sendToTarget();
        }

        public void handleCallSessionEvent(int event) {
            ImsVideoCallProviderWrapper.this.mHandler.obtainMessage(3, Integer.valueOf(event)).sendToTarget();
        }

        public void changePeerDimensions(int width, int height) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = Integer.valueOf(width);
            args.arg2 = Integer.valueOf(height);
            ImsVideoCallProviderWrapper.this.mHandler.obtainMessage(4, args).sendToTarget();
        }

        public void changeCallDataUsage(int dataUsage) {
            ImsVideoCallProviderWrapper.this.mHandler.obtainMessage(5, Integer.valueOf(dataUsage)).sendToTarget();
        }

        public void changeCameraCapabilities(CameraCapabilities cameraCapabilities) {
            ImsVideoCallProviderWrapper.this.mHandler.obtainMessage(6, cameraCapabilities).sendToTarget();
        }
    }

    public ImsVideoCallProviderWrapper(IImsVideoCallProvider VideoProvider) throws RemoteException {
        this.mVideoCallProvider = VideoProvider;
        this.mVideoCallProvider.asBinder().linkToDeath(this.mDeathRecipient, 0);
        this.mBinder = new ImsVideoCallCallback();
        this.mVideoCallProvider.setCallback(this.mBinder);
    }

    @Override
    public void onSetCamera(String cameraId) {
        try {
            this.mVideoCallProvider.setCamera(cameraId);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void onSetPreviewSurface(Surface surface) {
        try {
            this.mVideoCallProvider.setPreviewSurface(surface);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void onSetDisplaySurface(Surface surface) {
        try {
            this.mVideoCallProvider.setDisplaySurface(surface);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void onSetDeviceOrientation(int rotation) {
        try {
            this.mVideoCallProvider.setDeviceOrientation(rotation);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void onSetZoom(float value) {
        try {
            this.mVideoCallProvider.setZoom(value);
        } catch (RemoteException e) {
        }
    }

    public void onSendSessionModifyRequest(VideoProfile requestProfile) {
        try {
            this.mVideoCallProvider.sendSessionModifyRequest(requestProfile);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void onSendSessionModifyResponse(VideoProfile responseProfile) {
        try {
            this.mVideoCallProvider.sendSessionModifyResponse(responseProfile);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void onRequestCameraCapabilities() {
        try {
            this.mVideoCallProvider.requestCameraCapabilities();
        } catch (RemoteException e) {
        }
    }

    @Override
    public void onRequestConnectionDataUsage() {
        try {
            this.mVideoCallProvider.requestCallDataUsage();
        } catch (RemoteException e) {
        }
    }

    public void onSetPauseImage(String uri) {
        try {
            this.mVideoCallProvider.setPauseImage(uri);
        } catch (RemoteException e) {
        }
    }
}
