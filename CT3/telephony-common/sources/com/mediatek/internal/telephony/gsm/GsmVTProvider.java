package com.mediatek.internal.telephony.gsm;

import android.net.Uri;
import android.telecom.VideoProfile;
import android.util.Log;
import android.view.Surface;
import com.mediatek.internal.telephony.gsm.GsmVTProviderUtil;
import java.util.List;

public class GsmVTProvider extends GsmVideoCallProvider {
    public static final int SESSION_EVENT_BAD_DATA_BITRATE = 4008;
    public static final int SESSION_EVENT_CALL_ABNORMAL_END = 1009;
    public static final int SESSION_EVENT_CALL_END = 1008;
    public static final int SESSION_EVENT_CAM_CAP_CHANGED = 4007;
    public static final int SESSION_EVENT_DATA_BITRATE_RECOVER = 4009;
    public static final int SESSION_EVENT_DATA_USAGE_CHANGED = 4006;
    public static final int SESSION_EVENT_ERROR_CAMERA_CRASHED = 8003;
    public static final int SESSION_EVENT_ERROR_CAMERA_SET_IGNORED = 8006;
    public static final int SESSION_EVENT_ERROR_CODEC = 8004;
    public static final int SESSION_EVENT_ERROR_REC = 8005;
    public static final int SESSION_EVENT_ERROR_SERVER_DIED = 8002;
    public static final int SESSION_EVENT_ERROR_SERVICE = 8001;
    public static final int SESSION_EVENT_HANDLE_CALL_SESSION_EVT = 4003;
    public static final int SESSION_EVENT_LOCAL_SIZE_CHANGED = 4005;
    public static final int SESSION_EVENT_PEER_CAMERA_CLOSE = 1012;
    public static final int SESSION_EVENT_PEER_CAMERA_OPEN = 1011;
    public static final int SESSION_EVENT_PEER_SIZE_CHANGED = 4004;
    public static final int SESSION_EVENT_RECEIVE_FIRSTFRAME = 1001;
    public static final int SESSION_EVENT_RECORDER_EVENT_INFO_COMPLETE = 1007;
    public static final int SESSION_EVENT_RECORDER_EVENT_INFO_NO_I_FRAME = 1006;
    public static final int SESSION_EVENT_RECORDER_EVENT_INFO_REACH_MAX_DURATION = 1004;
    public static final int SESSION_EVENT_RECORDER_EVENT_INFO_REACH_MAX_FILESIZE = 1005;
    public static final int SESSION_EVENT_RECORDER_EVENT_INFO_UNKNOWN = 1003;
    public static final int SESSION_EVENT_RECV_SESSION_CONFIG_REQ = 4001;
    public static final int SESSION_EVENT_RECV_SESSION_CONFIG_RSP = 4002;
    public static final int SESSION_EVENT_SNAPSHOT_DONE = 1002;
    public static final int SESSION_EVENT_START_COUNTER = 1010;
    public static final int SESSION_EVENT_WARNING_SERVICE_NOT_READY = 9001;
    static final String TAG = "GsmVTProvider";
    public static final int VT_PROVIDER_INVALIDE_ID = -10000;
    private static int mDefaultId;
    private int mId;
    private boolean mInitComplete;
    private GsmVTProviderUtil mUtil;

    public static native int nFinalization(int i);

    public static native String nGetCameraParameters(int i);

    public static native int nGetCameraSensorCount(int i);

    public static native int nInitialization(int i);

    public static native int nRequestCallDataUsage(int i);

    public static native int nRequestCameraCapabilities(int i);

    public static native int nRequestPeerConfig(int i, String str);

    public static native int nResponseLocalConfig(int i, String str);

    public static native int nSetCamera(int i, int i2);

    public static native int nSetCameraParameters(int i, String str);

    public static native int nSetDeviceOrientation(int i, int i2);

    public static native int nSetDisplaySurface(int i, Surface surface);

    public static native int nSetPreviewSurface(int i, Surface surface);

    public static native int nSetUIMode(int i, int i2);

    public static native int nSnapshot(int i, int i2, String str);

    public static native int nStartRecording(int i, int i2, String str, long j);

    public static native int nStopRecording(int i);

    static {
        System.loadLibrary("mtk_vt_wrapper");
        mDefaultId = VT_PROVIDER_INVALIDE_ID;
    }

    public GsmVTProvider(int id) {
        this.mId = 1;
        this.mInitComplete = false;
        Log.d(TAG, "New GsmVTProvider id = " + id);
        this.mInitComplete = false;
        int wait_time = 0;
        Log.d(TAG, "New GsmVTProvider check if exist the same id");
        while (true) {
            if (GsmVTProviderUtil.recordGet(id) == null) {
                break;
            }
            Log.d(TAG, "New GsmVTProvider the same id exist, wait ...");
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
            }
            wait_time++;
            if (wait_time > 10) {
                Log.d(TAG, "New GsmVTProvider the same id exist, break!");
                break;
            }
        }
        this.mId = id;
        this.mUtil = new GsmVTProviderUtil();
        GsmVTProviderUtil.recordAdd(this.mId, this);
        nInitialization(this.mId);
        if (mDefaultId == -10000) {
            mDefaultId = this.mId;
        }
        this.mInitComplete = true;
    }

    public GsmVTProvider() {
        this.mId = 1;
        this.mInitComplete = false;
        Log.d(TAG, "New GsmVTProvider without id");
        this.mId = VT_PROVIDER_INVALIDE_ID;
        this.mInitComplete = false;
    }

    public void setId(int id) {
        Log.d(TAG, "setId id = " + id);
        Log.d(TAG, "setId mId = " + this.mId);
        if (this.mId != -10000) {
            return;
        }
        int wait_time = 0;
        Log.d(TAG, "setId check if exist the same id");
        while (true) {
            if (GsmVTProviderUtil.recordGet(id) == null) {
                break;
            }
            Log.d(TAG, "setId the same id exist, wait ...");
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
            }
            wait_time++;
            if (wait_time > 10) {
                Log.d(TAG, "setId the same id exist, break!");
                break;
            }
        }
        this.mId = id;
        this.mUtil = new GsmVTProviderUtil();
        GsmVTProviderUtil.recordAdd(this.mId, this);
        nInitialization(this.mId);
        if (mDefaultId == -10000) {
            mDefaultId = this.mId;
        }
        this.mInitComplete = true;
    }

    public int getId() {
        return this.mId;
    }

    public void waitInitComplete() {
        while (!this.mInitComplete) {
            try {
                Log.w(TAG, "Wait for initialization complete!");
                Thread.sleep(500L);
            } catch (InterruptedException e) {
            }
        }
    }

    private static void updateDefaultId() {
        if (!GsmVTProviderUtil.recordContain(mDefaultId)) {
            if (GsmVTProviderUtil.recordSize() != 0) {
                mDefaultId = GsmVTProviderUtil.recordPopId();
            } else {
                mDefaultId = VT_PROVIDER_INVALIDE_ID;
            }
        }
    }

    @Override
    public void onSetCamera(String cameraId) {
        if (this.mId == -10000) {
            handleCallSessionEvent(SESSION_EVENT_WARNING_SERVICE_NOT_READY);
            return;
        }
        waitInitComplete();
        if (cameraId != null) {
            nSetCamera(this.mId, Integer.valueOf(cameraId).intValue());
        } else {
            nSetCamera(this.mId, -1);
        }
    }

    @Override
    public void onSetPreviewSurface(Surface surface) {
        GsmVTProvider vp;
        waitInitComplete();
        nSetPreviewSurface(this.mId, surface);
        if (surface == null) {
            GsmVTProviderUtil.surfaceSet(this.mId, true, false);
        } else {
            GsmVTProviderUtil.surfaceSet(this.mId, true, true);
        }
        if (GsmVTProviderUtil.surfaceGet(this.mId) != 0 || (vp = GsmVTProviderUtil.recordGet(this.mId)) == null) {
            return;
        }
        vp.handleCallSessionEvent(1008);
    }

    @Override
    public void onSetDisplaySurface(Surface surface) {
        GsmVTProvider vp;
        waitInitComplete();
        nSetDisplaySurface(this.mId, surface);
        if (surface == null) {
            GsmVTProviderUtil.surfaceSet(this.mId, false, false);
        } else {
            GsmVTProviderUtil.surfaceSet(this.mId, false, true);
        }
        if (GsmVTProviderUtil.surfaceGet(this.mId) != 0 || (vp = GsmVTProviderUtil.recordGet(this.mId)) == null) {
            return;
        }
        vp.handleCallSessionEvent(1008);
    }

    @Override
    public void onSetDeviceOrientation(int rotation) {
        nSetDeviceOrientation(this.mId, rotation);
    }

    @Override
    public void onSetZoom(float value) {
        GsmVTProviderUtil gsmVTProviderUtil = this.mUtil;
        GsmVTProviderUtil.getSetting().set(GsmVTProviderUtil.ParameterSet.KEY_ZOOM, (int) value);
        GsmVTProviderUtil gsmVTProviderUtil2 = this.mUtil;
        String currentSeeting = GsmVTProviderUtil.getSetting().flatten();
        nSetCameraParameters(this.mId, currentSeeting);
    }

    @Override
    public void onSendSessionModifyRequest(VideoProfile fromProfile, VideoProfile toProfile) {
        nRequestPeerConfig(this.mId, GsmVTProviderUtil.packFromVdoProfile(toProfile));
    }

    @Override
    public void onSendSessionModifyResponse(VideoProfile responseProfile) {
        nResponseLocalConfig(this.mId, GsmVTProviderUtil.packFromVdoProfile(responseProfile));
    }

    @Override
    public void onRequestCameraCapabilities() {
        nRequestCameraCapabilities(this.mId);
    }

    @Override
    public void onRequestCallDataUsage() {
        nRequestCallDataUsage(this.mId);
    }

    @Override
    public void onSetPauseImage(Uri uri) {
    }

    @Override
    public void onSetUIMode(int mode) {
        if (mode == 65536) {
            nFinalization(this.mId);
        } else {
            nSetUIMode(this.mId, mode);
        }
    }

    public static void postEventFromNative(int msg, int id, int arg1, int arg2, int arg3, Object obj1, Object obj2, Object obj3) {
        GsmVTProvider vp = GsmVTProviderUtil.recordGet(id);
        if (vp == null) {
            Log.e(TAG, "Error: post event to Call is already release or has happen error before!");
            if (msg == 8002) {
                while (true) {
                    int callId = GsmVTProviderUtil.recordPopId();
                    if (callId == -10000) {
                        break;
                    }
                    GsmVTProvider vp2 = GsmVTProviderUtil.recordGet(callId);
                    if (vp2 != null) {
                        vp2.handleCallSessionEvent(msg);
                    }
                    GsmVTProviderUtil.recordRemove(callId);
                }
            }
            mDefaultId = VT_PROVIDER_INVALIDE_ID;
        }
        Log.i(TAG, "postEventFromNative [" + msg + "]");
        switch (msg) {
            case 1001:
                Log.d(TAG, "postEventFromNative : msg = SESSION_EVENT_RECEIVE_FIRSTFRAME");
                vp.handleCallSessionEvent(msg);
                break;
            case 1002:
                Log.d(TAG, "postEventFromNative : msg = SESSION_EVENT_SNAPSHOT_DONE");
                vp.handleCallSessionEvent(msg);
                break;
            case 1003:
                Log.d(TAG, "postEventFromNative : msg = SESSION_EVENT_RECORDER_EVENT_INFO_UNKNOWN");
                vp.handleCallSessionEvent(msg);
                break;
            case 1004:
                Log.d(TAG, "postEventFromNative : msg = SESSION_EVENT_RECORDER_EVENT_INFO_REACH_MAX_DURATION");
                vp.handleCallSessionEvent(msg);
                break;
            case 1005:
                Log.d(TAG, "postEventFromNative : msg = SESSION_EVENT_RECORDER_EVENT_INFO_REACH_MAX_FILESIZE");
                vp.handleCallSessionEvent(msg);
                break;
            case 1006:
                Log.d(TAG, "postEventFromNative : msg = SESSION_EVENT_RECORDER_EVENT_INFO_NO_I_FRAME");
                vp.handleCallSessionEvent(msg);
                break;
            case 1007:
                Log.d(TAG, "postEventFromNative : msg = SESSION_EVENT_RECORDER_EVENT_INFO_COMPLETE");
                vp.handleCallSessionEvent(msg);
                break;
            case 1008:
            case 1009:
                Log.d(TAG, "postEventFromNative : msg = SESSION_EVENT_CALL_END / SESSION_EVENT_CALL_ABNORMAL_END");
                GsmVTProviderUtil.recordRemove(id);
                updateDefaultId();
                vp.handleCallSessionEvent(msg);
                break;
            case SESSION_EVENT_START_COUNTER:
                Log.d(TAG, "postEventFromNative : msg = MSG_START_COUNTER");
                vp.handleCallSessionEvent(msg);
                break;
            case 1011:
                Log.d(TAG, "postEventFromNative : msg = MSG_PEER_CAMERA_OPEN");
                vp.handleCallSessionEvent(msg);
                break;
            case 1012:
                Log.d(TAG, "postEventFromNative : msg = MSG_PEER_CAMERA_CLOSE");
                vp.handleCallSessionEvent(msg);
                break;
            case SESSION_EVENT_RECV_SESSION_CONFIG_REQ:
                Log.d(TAG, "postEventFromNative : msg = SESSION_EVENT_RECV_SESSION_CONFIG_REQ");
                vp.receiveSessionModifyRequest(GsmVTProviderUtil.unPackToVdoProfile((String) obj1));
                break;
            case SESSION_EVENT_RECV_SESSION_CONFIG_RSP:
                Log.d(TAG, "postEventFromNative : msg = SESSION_EVENT_RECV_SESSION_CONFIG_RSP");
                vp.receiveSessionModifyResponse(arg1, GsmVTProviderUtil.unPackToVdoProfile((String) obj1), GsmVTProviderUtil.unPackToVdoProfile((String) obj2));
                break;
            case SESSION_EVENT_HANDLE_CALL_SESSION_EVT:
                Log.d(TAG, "postEventFromNative : msg = SESSION_EVENT_HANDLE_CALL_SESSION_EVT");
                vp.handleCallSessionEvent(msg);
                break;
            case SESSION_EVENT_PEER_SIZE_CHANGED:
                Log.d(TAG, "postEventFromNative : msg = SESSION_EVENT_PEER_SIZE_CHANGED");
                vp.changePeerDimensionsWithAngle(arg1, arg2, arg3);
                break;
            case SESSION_EVENT_LOCAL_SIZE_CHANGED:
                Log.d(TAG, "postEventFromNative : msg = SESSION_EVENT_LOCAL_SIZE_CHANGED");
                break;
            case SESSION_EVENT_DATA_USAGE_CHANGED:
                Log.d(TAG, "postEventFromNative : msg = SESSION_EVENT_DATA_USAGE_CHANGED");
                vp.changeCallDataUsage(arg1);
                break;
            case SESSION_EVENT_CAM_CAP_CHANGED:
                Log.d(TAG, "postEventFromNative : msg = SESSION_EVENT_CAM_CAP_CHANGED");
                Log.d(TAG, (String) obj1);
                GsmVTProviderUtil.getSetting().unflatten((String) obj1);
                GsmVTProviderUtil.ParameterSet set = GsmVTProviderUtil.getSetting();
                int zoom_max = set.getInt(GsmVTProviderUtil.ParameterSet.KEY_MAX_ZOOM, 0);
                boolean zoom_support = "true".equals(set.get(GsmVTProviderUtil.ParameterSet.KEY_ZOOM_SUPPORTED));
                List<GsmVTProviderUtil.Size> size = set.getSizeList(GsmVTProviderUtil.ParameterSet.KEY_PREVIEW_SIZE);
                int width = 176;
                int height = 144;
                if (size != null) {
                    width = size.get(0).width;
                    height = size.get(0).height;
                }
                int temp = width;
                int width2 = height;
                VideoProfile.CameraCapabilities camCap = new VideoProfile.CameraCapabilities(width2, temp, zoom_support, zoom_max);
                vp.changeCameraCapabilities(camCap);
                break;
            case SESSION_EVENT_BAD_DATA_BITRATE:
                Log.d(TAG, "postEventFromNative : msg = SESSION_EVENT_BAD_DATA_BITRATE");
                vp.handleCallSessionEvent(msg);
                break;
            case SESSION_EVENT_ERROR_SERVICE:
                Log.d(TAG, "postEventFromNative : msg = MSG_ERROR_SERVICE");
                GsmVTProviderUtil.recordRemove(id);
                updateDefaultId();
                vp.handleCallSessionEvent(msg);
                break;
            case SESSION_EVENT_ERROR_SERVER_DIED:
                Log.d(TAG, "postEventFromNative : msg = MSG_ERROR_SERVER_DIED");
                GsmVTProviderUtil.recordRemove(id);
                updateDefaultId();
                if (vp != null) {
                    vp.handleCallSessionEvent(msg);
                }
                break;
            case SESSION_EVENT_ERROR_CAMERA_CRASHED:
                Log.d(TAG, "postEventFromNative : msg = MSG_ERROR_CAMERA_CRASHED");
                vp.handleCallSessionEvent(msg);
                break;
            case SESSION_EVENT_ERROR_CODEC:
                Log.d(TAG, "postEventFromNative : msg = MSG_ERROR_CODEC");
                vp.handleCallSessionEvent(msg);
                break;
            case SESSION_EVENT_ERROR_REC:
                Log.d(TAG, "postEventFromNative : msg = MSG_ERROR_REC");
                vp.handleCallSessionEvent(msg);
                break;
            case SESSION_EVENT_ERROR_CAMERA_SET_IGNORED:
                Log.d(TAG, "postEventFromNative : msg = MSG_ERROR_CAMERA_SET_IGNORED");
                vp.handleCallSessionEvent(msg);
                break;
            default:
                Log.d(TAG, "postEventFromNative : msg = UNKNOWB");
                break;
        }
    }
}
