package android.telecom;

import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.telecom.InCallService;
import android.telecom.VideoProfile;
import android.view.Surface;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.SomeArgs;
import com.android.internal.telecom.IVideoCallback;
import com.android.internal.telecom.IVideoProvider;

public class VideoCallImpl extends InCallService.VideoCall {
    private InCallService.VideoCall.Callback mCallback;
    private final String mCallingPackageName;
    private Handler mHandler;
    private int mTargetSdkVersion;
    private final IVideoProvider mVideoProvider;
    private int mVideoQuality = 0;
    private int mVideoState = 0;
    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            VideoCallImpl.this.mVideoProvider.asBinder().unlinkToDeath(this, 0);
        }
    };
    private final VideoCallListenerBinder mBinder = new VideoCallListenerBinder();

    private final class VideoCallListenerBinder extends IVideoCallback.Stub {
        private VideoCallListenerBinder() {
        }

        @Override
        public void receiveSessionModifyRequest(VideoProfile videoProfile) {
            if (VideoCallImpl.this.mHandler == null) {
                return;
            }
            VideoCallImpl.this.mHandler.obtainMessage(1, videoProfile).sendToTarget();
        }

        @Override
        public void receiveSessionModifyResponse(int i, VideoProfile videoProfile, VideoProfile videoProfile2) {
            if (VideoCallImpl.this.mHandler == null) {
                return;
            }
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = Integer.valueOf(i);
            someArgsObtain.arg2 = videoProfile;
            someArgsObtain.arg3 = videoProfile2;
            VideoCallImpl.this.mHandler.obtainMessage(2, someArgsObtain).sendToTarget();
        }

        @Override
        public void handleCallSessionEvent(int i) {
            if (VideoCallImpl.this.mHandler == null) {
                return;
            }
            VideoCallImpl.this.mHandler.obtainMessage(3, Integer.valueOf(i)).sendToTarget();
        }

        @Override
        public void changePeerDimensions(int i, int i2) {
            if (VideoCallImpl.this.mHandler == null) {
                return;
            }
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = Integer.valueOf(i);
            someArgsObtain.arg2 = Integer.valueOf(i2);
            VideoCallImpl.this.mHandler.obtainMessage(4, someArgsObtain).sendToTarget();
        }

        @Override
        public void changeVideoQuality(int i) {
            if (VideoCallImpl.this.mHandler == null) {
                return;
            }
            VideoCallImpl.this.mHandler.obtainMessage(7, i, 0).sendToTarget();
        }

        @Override
        public void changeCallDataUsage(long j) {
            if (VideoCallImpl.this.mHandler == null) {
                return;
            }
            VideoCallImpl.this.mHandler.obtainMessage(5, Long.valueOf(j)).sendToTarget();
        }

        @Override
        public void changeCameraCapabilities(VideoProfile.CameraCapabilities cameraCapabilities) {
            if (VideoCallImpl.this.mHandler == null) {
                return;
            }
            VideoCallImpl.this.mHandler.obtainMessage(6, cameraCapabilities).sendToTarget();
        }
    }

    private final class MessageHandler extends Handler {
        private static final int MSG_CHANGE_CALL_DATA_USAGE = 5;
        private static final int MSG_CHANGE_CAMERA_CAPABILITIES = 6;
        private static final int MSG_CHANGE_PEER_DIMENSIONS = 4;
        private static final int MSG_CHANGE_VIDEO_QUALITY = 7;
        private static final int MSG_HANDLE_CALL_SESSION_EVENT = 3;
        private static final int MSG_RECEIVE_SESSION_MODIFY_REQUEST = 1;
        private static final int MSG_RECEIVE_SESSION_MODIFY_RESPONSE = 2;

        public MessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            SomeArgs someArgs;
            if (VideoCallImpl.this.mCallback != null) {
                switch (message.what) {
                    case 1:
                        VideoCallImpl.this.mCallback.onSessionModifyRequestReceived((VideoProfile) message.obj);
                        return;
                    case 2:
                        someArgs = (SomeArgs) message.obj;
                        try {
                            VideoCallImpl.this.mCallback.onSessionModifyResponseReceived(((Integer) someArgs.arg1).intValue(), (VideoProfile) someArgs.arg2, (VideoProfile) someArgs.arg3);
                            return;
                        } finally {
                        }
                    case 3:
                        VideoCallImpl.this.mCallback.onCallSessionEvent(((Integer) message.obj).intValue());
                        return;
                    case 4:
                        someArgs = (SomeArgs) message.obj;
                        try {
                            VideoCallImpl.this.mCallback.onPeerDimensionsChanged(((Integer) someArgs.arg1).intValue(), ((Integer) someArgs.arg2).intValue());
                            return;
                        } finally {
                        }
                    case 5:
                        VideoCallImpl.this.mCallback.onCallDataUsageChanged(((Long) message.obj).longValue());
                        return;
                    case 6:
                        VideoCallImpl.this.mCallback.onCameraCapabilitiesChanged((VideoProfile.CameraCapabilities) message.obj);
                        return;
                    case 7:
                        VideoCallImpl.this.mVideoQuality = message.arg1;
                        VideoCallImpl.this.mCallback.onVideoQualityChanged(message.arg1);
                        return;
                    default:
                        return;
                }
            }
        }
    }

    VideoCallImpl(IVideoProvider iVideoProvider, String str, int i) throws RemoteException {
        this.mVideoProvider = iVideoProvider;
        this.mVideoProvider.addVideoCallback(this.mBinder);
        this.mCallingPackageName = str;
        setTargetSdkVersion(i);
    }

    @VisibleForTesting
    public void setTargetSdkVersion(int i) {
        this.mTargetSdkVersion = i;
    }

    @Override
    public void destroy() {
        unregisterCallback(this.mCallback);
    }

    @Override
    public void registerCallback(InCallService.VideoCall.Callback callback) {
        registerCallback(callback, null);
    }

    @Override
    public void registerCallback(InCallService.VideoCall.Callback callback, Handler handler) {
        this.mCallback = callback;
        if (handler == null) {
            this.mHandler = new MessageHandler(Looper.getMainLooper());
        } else {
            this.mHandler = new MessageHandler(handler.getLooper());
        }
    }

    @Override
    public void unregisterCallback(InCallService.VideoCall.Callback callback) {
        if (callback != this.mCallback) {
            return;
        }
        this.mCallback = null;
        try {
            this.mVideoProvider.removeVideoCallback(this.mBinder);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void setCamera(String str) {
        try {
            Log.w(this, "setCamera: cameraId=%s, calling=%s", str, this.mCallingPackageName);
            this.mVideoProvider.setCamera(str, this.mCallingPackageName, this.mTargetSdkVersion);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void setPreviewSurface(Surface surface) {
        try {
            this.mVideoProvider.setPreviewSurface(surface);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void setDisplaySurface(Surface surface) {
        try {
            this.mVideoProvider.setDisplaySurface(surface);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void setDeviceOrientation(int i) {
        try {
            this.mVideoProvider.setDeviceOrientation(i);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void setZoom(float f) {
        try {
            this.mVideoProvider.setZoom(f);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void sendSessionModifyRequest(VideoProfile videoProfile) {
        try {
            this.mVideoProvider.sendSessionModifyRequest(new VideoProfile(this.mVideoState, this.mVideoQuality), videoProfile);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void sendSessionModifyResponse(VideoProfile videoProfile) {
        try {
            this.mVideoProvider.sendSessionModifyResponse(videoProfile);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void requestCameraCapabilities() {
        try {
            this.mVideoProvider.requestCameraCapabilities();
        } catch (RemoteException e) {
        }
    }

    @Override
    public void requestCallDataUsage() {
        try {
            this.mVideoProvider.requestCallDataUsage();
        } catch (RemoteException e) {
        }
    }

    @Override
    public void setPauseImage(Uri uri) {
        try {
            this.mVideoProvider.setPauseImage(uri);
        } catch (RemoteException e) {
        }
    }

    public void setVideoState(int i) {
        this.mVideoState = i;
    }
}
