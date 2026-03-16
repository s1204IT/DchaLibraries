package android.telecom;

import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.telecom.InCallService;
import android.view.Surface;
import com.android.internal.os.SomeArgs;
import com.android.internal.telecom.IVideoCallback;
import com.android.internal.telecom.IVideoProvider;

public class VideoCallImpl extends InCallService.VideoCall {
    private static final int MSG_CHANGE_CALL_DATA_USAGE = 5;
    private static final int MSG_CHANGE_CAMERA_CAPABILITIES = 6;
    private static final int MSG_CHANGE_PEER_DIMENSIONS = 4;
    private static final int MSG_HANDLE_CALL_SESSION_EVENT = 3;
    private static final int MSG_RECEIVE_SESSION_MODIFY_REQUEST = 1;
    private static final int MSG_RECEIVE_SESSION_MODIFY_RESPONSE = 2;
    private final VideoCallListenerBinder mBinder;
    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            VideoCallImpl.this.mVideoProvider.asBinder().unlinkToDeath(this, 0);
        }
    };
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            SomeArgs args;
            if (VideoCallImpl.this.mVideoCallListener != null) {
                switch (msg.what) {
                    case 1:
                        VideoCallImpl.this.mVideoCallListener.onSessionModifyRequestReceived((VideoProfile) msg.obj);
                        return;
                    case 2:
                        args = (SomeArgs) msg.obj;
                        try {
                            int status = ((Integer) args.arg1).intValue();
                            VideoProfile requestProfile = (VideoProfile) args.arg2;
                            VideoProfile responseProfile = (VideoProfile) args.arg3;
                            VideoCallImpl.this.mVideoCallListener.onSessionModifyResponseReceived(status, requestProfile, responseProfile);
                            return;
                        } finally {
                        }
                    case 3:
                        VideoCallImpl.this.mVideoCallListener.onCallSessionEvent(((Integer) msg.obj).intValue());
                        return;
                    case 4:
                        args = (SomeArgs) msg.obj;
                        try {
                            int width = ((Integer) args.arg1).intValue();
                            int height = ((Integer) args.arg2).intValue();
                            VideoCallImpl.this.mVideoCallListener.onPeerDimensionsChanged(width, height);
                            return;
                        } finally {
                        }
                    case 5:
                        VideoCallImpl.this.mVideoCallListener.onCallDataUsageChanged(msg.arg1);
                        return;
                    case 6:
                        VideoCallImpl.this.mVideoCallListener.onCameraCapabilitiesChanged((CameraCapabilities) msg.obj);
                        return;
                    default:
                        return;
                }
            }
        }
    };
    private InCallService.VideoCall.Listener mVideoCallListener;
    private final IVideoProvider mVideoProvider;

    private final class VideoCallListenerBinder extends IVideoCallback.Stub {
        private VideoCallListenerBinder() {
        }

        @Override
        public void receiveSessionModifyRequest(VideoProfile videoProfile) {
            VideoCallImpl.this.mHandler.obtainMessage(1, videoProfile).sendToTarget();
        }

        @Override
        public void receiveSessionModifyResponse(int status, VideoProfile requestProfile, VideoProfile responseProfile) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = Integer.valueOf(status);
            args.arg2 = requestProfile;
            args.arg3 = responseProfile;
            VideoCallImpl.this.mHandler.obtainMessage(2, args).sendToTarget();
        }

        @Override
        public void handleCallSessionEvent(int event) {
            VideoCallImpl.this.mHandler.obtainMessage(3, Integer.valueOf(event)).sendToTarget();
        }

        @Override
        public void changePeerDimensions(int width, int height) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = Integer.valueOf(width);
            args.arg2 = Integer.valueOf(height);
            VideoCallImpl.this.mHandler.obtainMessage(4, args).sendToTarget();
        }

        @Override
        public void changeCallDataUsage(int dataUsage) {
            VideoCallImpl.this.mHandler.obtainMessage(5, Integer.valueOf(dataUsage)).sendToTarget();
        }

        @Override
        public void changeCameraCapabilities(CameraCapabilities cameraCapabilities) {
            VideoCallImpl.this.mHandler.obtainMessage(6, cameraCapabilities).sendToTarget();
        }
    }

    VideoCallImpl(IVideoProvider videoProvider) throws RemoteException {
        this.mVideoProvider = videoProvider;
        this.mVideoProvider.asBinder().linkToDeath(this.mDeathRecipient, 0);
        this.mBinder = new VideoCallListenerBinder();
        this.mVideoProvider.setVideoCallback(this.mBinder);
    }

    @Override
    public void setVideoCallListener(InCallService.VideoCall.Listener videoCallListener) {
        this.mVideoCallListener = videoCallListener;
    }

    @Override
    public void setCamera(String cameraId) {
        try {
            this.mVideoProvider.setCamera(cameraId);
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
    public void setDeviceOrientation(int rotation) {
        try {
            this.mVideoProvider.setDeviceOrientation(rotation);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void setZoom(float value) {
        try {
            this.mVideoProvider.setZoom(value);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void sendSessionModifyRequest(VideoProfile requestProfile) {
        try {
            this.mVideoProvider.sendSessionModifyRequest(requestProfile);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void sendSessionModifyResponse(VideoProfile responseProfile) {
        try {
            this.mVideoProvider.sendSessionModifyResponse(responseProfile);
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
    public void setPauseImage(String uri) {
        try {
            this.mVideoProvider.setPauseImage(uri);
        } catch (RemoteException e) {
        }
    }
}
