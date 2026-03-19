package com.android.ims.internal;

import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.telecom.Connection;
import android.telecom.Log;
import android.telecom.VideoProfile;
import android.view.Surface;
import com.android.ims.internal.IImsVideoCallCallback;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.SomeArgs;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ImsVideoCallProviderWrapper extends Connection.VideoProvider {
    private static final int MSG_CHANGE_CALL_DATA_USAGE = 5;
    private static final int MSG_CHANGE_CAMERA_CAPABILITIES = 6;
    private static final int MSG_CHANGE_PEER_DIMENSIONS = 4;
    private static final int MSG_CHANGE_VIDEO_QUALITY = 7;
    private static final int MSG_HANDLE_CALL_SESSION_EVENT = 3;
    private static final int MSG_RECEIVE_SESSION_MODIFY_REQUEST = 1;
    private static final int MSG_RECEIVE_SESSION_MODIFY_RESPONSE = 2;
    private final ImsVideoCallCallback mBinder;
    private final Set<ImsVideoProviderWrapperCallback> mCallbacks;
    private int mCurrentVideoState;
    private RegistrantList mDataUsageUpdateRegistrants;
    private IBinder.DeathRecipient mDeathRecipient;
    private final Handler mHandler;
    private boolean mIsVideoEnabled;
    private boolean mUseVideoPauseWorkaround;
    private final IImsVideoCallProvider mVideoCallProvider;
    private VideoPauseTracker mVideoPauseTracker;

    public interface ImsVideoProviderWrapperCallback {
        void onReceiveSessionModifyResponse(int i, VideoProfile videoProfile, VideoProfile videoProfile2);
    }

    private final class ImsVideoCallCallback extends IImsVideoCallCallback.Stub {
        private ImsVideoCallCallback() {
        }

        public void receiveSessionModifyRequest(VideoProfile videoProfile) {
            ImsVideoCallProviderWrapper.this.mHandler.obtainMessage(1, videoProfile).sendToTarget();
        }

        public void receiveSessionModifyResponse(int i, VideoProfile videoProfile, VideoProfile videoProfile2) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = Integer.valueOf(i);
            someArgsObtain.arg2 = videoProfile;
            someArgsObtain.arg3 = videoProfile2;
            ImsVideoCallProviderWrapper.this.mHandler.obtainMessage(2, someArgsObtain).sendToTarget();
        }

        public void handleCallSessionEvent(int i) {
            ImsVideoCallProviderWrapper.this.mHandler.obtainMessage(ImsVideoCallProviderWrapper.MSG_HANDLE_CALL_SESSION_EVENT, Integer.valueOf(i)).sendToTarget();
        }

        public void changePeerDimensions(int i, int i2) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = Integer.valueOf(i);
            someArgsObtain.arg2 = Integer.valueOf(i2);
            ImsVideoCallProviderWrapper.this.mHandler.obtainMessage(ImsVideoCallProviderWrapper.MSG_CHANGE_PEER_DIMENSIONS, someArgsObtain).sendToTarget();
        }

        public void changeVideoQuality(int i) {
            ImsVideoCallProviderWrapper.this.mHandler.obtainMessage(ImsVideoCallProviderWrapper.MSG_CHANGE_VIDEO_QUALITY, i, 0).sendToTarget();
        }

        public void changeCallDataUsage(long j) {
            ImsVideoCallProviderWrapper.this.mHandler.obtainMessage(ImsVideoCallProviderWrapper.MSG_CHANGE_CALL_DATA_USAGE, Long.valueOf(j)).sendToTarget();
        }

        public void changeCameraCapabilities(VideoProfile.CameraCapabilities cameraCapabilities) {
            ImsVideoCallProviderWrapper.this.mHandler.obtainMessage(ImsVideoCallProviderWrapper.MSG_CHANGE_CAMERA_CAPABILITIES, cameraCapabilities).sendToTarget();
        }
    }

    public void registerForDataUsageUpdate(Handler handler, int i, Object obj) {
        this.mDataUsageUpdateRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForDataUsageUpdate(Handler handler) {
        this.mDataUsageUpdateRegistrants.remove(handler);
    }

    public void addImsVideoProviderCallback(ImsVideoProviderWrapperCallback imsVideoProviderWrapperCallback) {
        this.mCallbacks.add(imsVideoProviderWrapperCallback);
    }

    public void removeImsVideoProviderCallback(ImsVideoProviderWrapperCallback imsVideoProviderWrapperCallback) {
        this.mCallbacks.remove(imsVideoProviderWrapperCallback);
    }

    public ImsVideoCallProviderWrapper(IImsVideoCallProvider iImsVideoCallProvider) throws RemoteException {
        this.mDataUsageUpdateRegistrants = new RegistrantList();
        this.mCallbacks = Collections.newSetFromMap(new ConcurrentHashMap(8, 0.9f, 1));
        this.mVideoPauseTracker = new VideoPauseTracker();
        this.mUseVideoPauseWorkaround = false;
        this.mIsVideoEnabled = true;
        this.mDeathRecipient = new IBinder.DeathRecipient() {
            @Override
            public void binderDied() {
                ImsVideoCallProviderWrapper.this.mVideoCallProvider.asBinder().unlinkToDeath(this, 0);
            }
        };
        this.mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                SomeArgs someArgs;
                switch (message.what) {
                    case 1:
                        VideoProfile videoProfile = (VideoProfile) message.obj;
                        if (!VideoProfile.isVideo(ImsVideoCallProviderWrapper.this.mCurrentVideoState) && VideoProfile.isVideo(videoProfile.getVideoState()) && !ImsVideoCallProviderWrapper.this.mIsVideoEnabled) {
                            Log.i(ImsVideoCallProviderWrapper.this, "receiveSessionModifyRequest: requestedVideoState=%s; rejecting as video is disabled.", new Object[]{Integer.valueOf(videoProfile.getVideoState())});
                            try {
                                ImsVideoCallProviderWrapper.this.mVideoCallProvider.sendSessionModifyResponse(new VideoProfile(0));
                                return;
                            } catch (RemoteException e) {
                                return;
                            }
                        }
                        ImsVideoCallProviderWrapper.this.receiveSessionModifyRequest(videoProfile);
                        return;
                    case 2:
                        someArgs = (SomeArgs) message.obj;
                        try {
                            int iIntValue = ((Integer) someArgs.arg1).intValue();
                            VideoProfile videoProfile2 = (VideoProfile) someArgs.arg2;
                            VideoProfile videoProfile3 = (VideoProfile) someArgs.arg3;
                            ImsVideoCallProviderWrapper.this.receiveSessionModifyResponse(iIntValue, videoProfile2, videoProfile3);
                            for (ImsVideoProviderWrapperCallback imsVideoProviderWrapperCallback : ImsVideoCallProviderWrapper.this.mCallbacks) {
                                if (imsVideoProviderWrapperCallback != null) {
                                    imsVideoProviderWrapperCallback.onReceiveSessionModifyResponse(iIntValue, videoProfile2, videoProfile3);
                                }
                                break;
                            }
                            return;
                        } finally {
                        }
                    case ImsVideoCallProviderWrapper.MSG_HANDLE_CALL_SESSION_EVENT:
                        ImsVideoCallProviderWrapper.this.handleCallSessionEvent(((Integer) message.obj).intValue());
                        return;
                    case ImsVideoCallProviderWrapper.MSG_CHANGE_PEER_DIMENSIONS:
                        someArgs = (SomeArgs) message.obj;
                        try {
                            ImsVideoCallProviderWrapper.this.changePeerDimensions(((Integer) someArgs.arg1).intValue(), ((Integer) someArgs.arg2).intValue());
                            return;
                        } finally {
                        }
                    case ImsVideoCallProviderWrapper.MSG_CHANGE_CALL_DATA_USAGE:
                        ImsVideoCallProviderWrapper.this.setCallDataUsage(((Long) message.obj).longValue());
                        ImsVideoCallProviderWrapper.this.mDataUsageUpdateRegistrants.notifyResult(message.obj);
                        return;
                    case ImsVideoCallProviderWrapper.MSG_CHANGE_CAMERA_CAPABILITIES:
                        ImsVideoCallProviderWrapper.this.changeCameraCapabilities((VideoProfile.CameraCapabilities) message.obj);
                        return;
                    case ImsVideoCallProviderWrapper.MSG_CHANGE_VIDEO_QUALITY:
                        ImsVideoCallProviderWrapper.this.changeVideoQuality(message.arg1);
                        return;
                    default:
                        return;
                }
            }
        };
        this.mVideoCallProvider = iImsVideoCallProvider;
        if (iImsVideoCallProvider != null) {
            this.mVideoCallProvider.asBinder().linkToDeath(this.mDeathRecipient, 0);
            this.mBinder = new ImsVideoCallCallback();
            this.mVideoCallProvider.setCallback(this.mBinder);
            return;
        }
        this.mBinder = null;
    }

    @VisibleForTesting
    public ImsVideoCallProviderWrapper(IImsVideoCallProvider iImsVideoCallProvider, VideoPauseTracker videoPauseTracker) throws RemoteException {
        this(iImsVideoCallProvider);
        this.mVideoPauseTracker = videoPauseTracker;
    }

    @Override
    public void onSetCamera(String str) {
        try {
            this.mVideoCallProvider.setCamera(str, Binder.getCallingUid());
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
    public void onSetDeviceOrientation(int i) {
        try {
            this.mVideoCallProvider.setDeviceOrientation(i);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void onSetZoom(float f) {
        try {
            this.mVideoCallProvider.setZoom(f);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void onSendSessionModifyRequest(VideoProfile videoProfile, VideoProfile videoProfile2) {
        if (videoProfile == null || videoProfile2 == null) {
            Log.w(this, "onSendSessionModifyRequest: null profile in request.", new Object[0]);
            return;
        }
        try {
            if (isResumeRequest(videoProfile.getVideoState(), videoProfile2.getVideoState()) && !VideoProfile.isPaused(this.mCurrentVideoState)) {
                Log.i(this, "onSendSessionModifyRequest: fromVideoState=%s, toVideoState=%s; skipping resume request - already resumed.", new Object[]{VideoProfile.videoStateToString(videoProfile.getVideoState()), VideoProfile.videoStateToString(videoProfile2.getVideoState())});
                return;
            }
            VideoProfile videoProfileMaybeFilterPauseResume = maybeFilterPauseResume(videoProfile, videoProfile2, 1);
            videoProfile.getVideoState();
            videoProfileMaybeFilterPauseResume.getVideoState();
            Log.i(this, "onSendSessionModifyRequest: fromVideoState=%s, toVideoState=%s; ", new Object[]{VideoProfile.videoStateToString(videoProfile.getVideoState()), VideoProfile.videoStateToString(videoProfileMaybeFilterPauseResume.getVideoState())});
            this.mVideoCallProvider.sendSessionModifyRequest(videoProfile, videoProfileMaybeFilterPauseResume);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void onSendSessionModifyResponse(VideoProfile videoProfile) {
        try {
            this.mVideoCallProvider.sendSessionModifyResponse(videoProfile);
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

    @Override
    public void onSetPauseImage(Uri uri) {
        try {
            this.mVideoCallProvider.setPauseImage(uri);
        } catch (RemoteException e) {
        }
    }

    @VisibleForTesting
    public static boolean isPauseRequest(int i, int i2) {
        return !VideoProfile.isPaused(i) && VideoProfile.isPaused(i2);
    }

    @VisibleForTesting
    public static boolean isResumeRequest(int i, int i2) {
        return VideoProfile.isPaused(i) && !VideoProfile.isPaused(i2);
    }

    @VisibleForTesting
    public static boolean isTurnOffCameraRequest(int i, int i2) {
        return VideoProfile.isTransmissionEnabled(i) && !VideoProfile.isTransmissionEnabled(i2);
    }

    @VisibleForTesting
    public static boolean isTurnOnCameraRequest(int i, int i2) {
        return !VideoProfile.isTransmissionEnabled(i) && VideoProfile.isTransmissionEnabled(i2);
    }

    @VisibleForTesting
    public VideoProfile maybeFilterPauseResume(VideoProfile videoProfile, VideoProfile videoProfile2, int i) {
        int videoState = videoProfile.getVideoState();
        int videoState2 = videoProfile2.getVideoState();
        boolean z = i == 1 && VideoProfile.isPaused(videoState) && VideoProfile.isPaused(videoState2);
        boolean z2 = isPauseRequest(videoState, videoState2) || z;
        boolean zIsResumeRequest = isResumeRequest(videoState, videoState2);
        if (z2) {
            Log.i(this, "maybeFilterPauseResume: isPauseRequest (from=%s, to=%s)", new Object[]{VideoProfile.videoStateToString(videoState), VideoProfile.videoStateToString(videoState2)});
            if (!this.mVideoPauseTracker.shouldPauseVideoFor(i) && !z) {
                return new VideoProfile(videoState2 & (-5), videoProfile2.getQuality());
            }
        } else if (zIsResumeRequest) {
            boolean zIsTurnOffCameraRequest = isTurnOffCameraRequest(videoState, videoState2);
            boolean zIsTurnOnCameraRequest = isTurnOnCameraRequest(videoState, videoState2);
            if (this.mUseVideoPauseWorkaround && (zIsTurnOffCameraRequest || zIsTurnOnCameraRequest)) {
                Log.i(this, "maybeFilterPauseResume: isResumeRequest, but camera turning on/off so skipping (from=%s, to=%s)", new Object[]{VideoProfile.videoStateToString(videoState), VideoProfile.videoStateToString(videoState2)});
                return videoProfile2;
            }
            Log.i(this, "maybeFilterPauseResume: isResumeRequest (from=%s, to=%s)", new Object[]{VideoProfile.videoStateToString(videoState), VideoProfile.videoStateToString(videoState2)});
            if (!this.mVideoPauseTracker.shouldResumeVideoFor(i)) {
                return new VideoProfile(videoState2 | MSG_CHANGE_PEER_DIMENSIONS, videoProfile2.getQuality());
            }
        }
        return videoProfile2;
    }

    public void pauseVideo(int i, int i2) {
        if (this.mVideoPauseTracker.shouldPauseVideoFor(i2)) {
            VideoProfile videoProfile = new VideoProfile(i);
            VideoProfile videoProfile2 = new VideoProfile(i | MSG_CHANGE_PEER_DIMENSIONS);
            try {
                Log.i(this, "pauseVideo: fromVideoState=%s, toVideoState=%s", new Object[]{VideoProfile.videoStateToString(videoProfile.getVideoState()), VideoProfile.videoStateToString(videoProfile2.getVideoState())});
                this.mVideoCallProvider.sendSessionModifyRequest(videoProfile, videoProfile2);
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        Log.i(this, "pauseVideo: video already paused", new Object[0]);
    }

    public void resumeVideo(int i, int i2) {
        if (this.mVideoPauseTracker.shouldResumeVideoFor(i2)) {
            VideoProfile videoProfile = new VideoProfile(i);
            VideoProfile videoProfile2 = new VideoProfile(i & (-5));
            try {
                Log.i(this, "resumeVideo: fromVideoState=%s, toVideoState=%s", new Object[]{VideoProfile.videoStateToString(videoProfile.getVideoState()), VideoProfile.videoStateToString(videoProfile2.getVideoState())});
                this.mVideoCallProvider.sendSessionModifyRequest(videoProfile, videoProfile2);
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        Log.i(this, "resumeVideo: remaining paused (paused from other sources)", new Object[0]);
    }

    public boolean wasVideoPausedFromSource(int i) {
        return this.mVideoPauseTracker.wasVideoPausedFromSource(i);
    }

    public void setUseVideoPauseWorkaround(boolean z) {
        this.mUseVideoPauseWorkaround = z;
    }

    public void onVideoStateChanged(int i) {
        if (VideoProfile.isPaused(this.mCurrentVideoState) && !VideoProfile.isPaused(i)) {
            Log.i(this, "onVideoStateChanged: currentVideoState=%s, newVideoState=%s, clearing pending pause requests.", new Object[]{VideoProfile.videoStateToString(this.mCurrentVideoState), VideoProfile.videoStateToString(i)});
            this.mVideoPauseTracker.clearPauseRequests();
        } else {
            Log.d(this, "onVideoStateChanged: currentVideoState=%s, newVideoState=%s", new Object[]{VideoProfile.videoStateToString(this.mCurrentVideoState), VideoProfile.videoStateToString(i)});
        }
        this.mCurrentVideoState = i;
    }

    public void setIsVideoEnabled(boolean z) {
        this.mIsVideoEnabled = z;
    }
}
