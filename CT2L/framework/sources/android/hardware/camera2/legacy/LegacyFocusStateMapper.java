package android.hardware.camera2.legacy;

import android.hardware.Camera;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.impl.CameraMetadataNative;
import android.hardware.camera2.utils.ParamsUtils;
import android.util.Log;
import com.android.internal.util.Preconditions;
import java.util.Objects;

public class LegacyFocusStateMapper {
    private static String TAG = "LegacyFocusStateMapper";
    private static final boolean VERBOSE = Log.isLoggable(TAG, 2);
    private final Camera mCamera;
    private int mAfStatePrevious = 0;
    private String mAfModePrevious = null;
    private final Object mLock = new Object();
    private int mAfRun = 0;
    private int mAfState = 0;

    public LegacyFocusStateMapper(Camera camera) {
        this.mCamera = (Camera) Preconditions.checkNotNull(camera, "camera must not be null");
    }

    public void processRequestTriggers(CaptureRequest captureRequest, Camera.Parameters parameters) {
        final int currentAfRun;
        Camera.AutoFocusMoveCallback afMoveCallback;
        int updatedAfRun;
        int afStateAfterStart;
        final int currentAfRun2;
        Preconditions.checkNotNull(captureRequest, "captureRequest must not be null");
        int afTrigger = ((Integer) ParamsUtils.getOrDefault(captureRequest, CaptureRequest.CONTROL_AF_TRIGGER, 0)).intValue();
        final String afMode = parameters.getFocusMode();
        if (!Objects.equals(this.mAfModePrevious, afMode)) {
            if (VERBOSE) {
                Log.v(TAG, "processRequestTriggers - AF mode switched from " + this.mAfModePrevious + " to " + afMode);
            }
            synchronized (this.mLock) {
                this.mAfRun++;
                this.mAfState = 0;
            }
            if (!this.mCamera.getParameters().getFocusMode().equals(Camera.Parameters.FOCUS_MODE_MANUAL)) {
                this.mCamera.cancelAutoFocus();
                Log.v(TAG, "cancelAutoFocus is not supported in FOCUS_MODE_MANUAL");
            }
        }
        this.mAfModePrevious = afMode;
        synchronized (this.mLock) {
            currentAfRun = this.mAfRun;
        }
        afMoveCallback = new Camera.AutoFocusMoveCallback() {
            @Override
            public void onAutoFocusMoving(boolean start, Camera camera) {
                synchronized (LegacyFocusStateMapper.this.mLock) {
                    int latestAfRun = LegacyFocusStateMapper.this.mAfRun;
                    if (LegacyFocusStateMapper.VERBOSE) {
                        Log.v(LegacyFocusStateMapper.TAG, "onAutoFocusMoving - start " + start + " latest AF run " + latestAfRun + ", last AF run " + currentAfRun);
                    }
                    if (currentAfRun != latestAfRun) {
                        Log.d(LegacyFocusStateMapper.TAG, "onAutoFocusMoving - ignoring move callbacks from old af run" + currentAfRun);
                        return;
                    }
                    int newAfState = start ? 1 : 2;
                    switch (afMode) {
                        case "continuous-picture":
                        case "continuous-video":
                            break;
                        default:
                            Log.w(LegacyFocusStateMapper.TAG, "onAutoFocus - got unexpected onAutoFocus in mode " + afMode);
                            break;
                    }
                    LegacyFocusStateMapper.this.mAfState = newAfState;
                }
            }
        };
        switch (afMode) {
            case "auto":
            case "macro":
            case "continuous-picture":
            case "continuous-video":
                this.mCamera.setAutoFocusMoveCallback(afMoveCallback);
                break;
        }
        switch (afTrigger) {
            case 0:
                return;
            case 1:
                switch (afMode) {
                    case "auto":
                    case "macro":
                        afStateAfterStart = 3;
                        break;
                    case "continuous-picture":
                    case "continuous-video":
                        afStateAfterStart = 1;
                        break;
                    default:
                        afStateAfterStart = 0;
                        break;
                }
                synchronized (this.mLock) {
                    currentAfRun2 = this.mAfRun + 1;
                    this.mAfRun = currentAfRun2;
                    this.mAfState = afStateAfterStart;
                    break;
                }
                if (VERBOSE) {
                    Log.v(TAG, "processRequestTriggers - got AF_TRIGGER_START, new AF run is " + currentAfRun2);
                }
                if (afStateAfterStart != 0) {
                    this.mCamera.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            byte b = 0;
                            synchronized (LegacyFocusStateMapper.this.mLock) {
                                int latestAfRun = LegacyFocusStateMapper.this.mAfRun;
                                if (LegacyFocusStateMapper.VERBOSE) {
                                    Log.v(LegacyFocusStateMapper.TAG, "onAutoFocus - success " + success + " latest AF run " + latestAfRun + ", last AF run " + currentAfRun2);
                                }
                                if (latestAfRun != currentAfRun2) {
                                    Log.d(LegacyFocusStateMapper.TAG, String.format("onAutoFocus - ignoring AF callback (old run %d, new run %d)", Integer.valueOf(currentAfRun2), Integer.valueOf(latestAfRun)));
                                    return;
                                }
                                int newAfState = success ? 4 : 5;
                                String str = afMode;
                                switch (str.hashCode()) {
                                    case -194628547:
                                        b = !str.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) ? (byte) -1 : (byte) 2;
                                        break;
                                    case 3005871:
                                        if (!str.equals("auto")) {
                                            b = -1;
                                        }
                                        break;
                                    case 103652300:
                                        b = !str.equals("macro") ? (byte) -1 : (byte) 3;
                                        break;
                                    case 910005312:
                                        b = !str.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) ? (byte) -1 : (byte) 1;
                                        break;
                                    default:
                                        b = -1;
                                        break;
                                }
                                switch (b) {
                                    case 0:
                                    case 1:
                                    case 2:
                                    case 3:
                                        break;
                                    default:
                                        Log.w(LegacyFocusStateMapper.TAG, "onAutoFocus - got unexpected onAutoFocus in mode " + afMode);
                                        break;
                                }
                                LegacyFocusStateMapper.this.mAfState = newAfState;
                            }
                        }
                    });
                    return;
                }
                return;
            case 2:
                synchronized (this.mLock) {
                    synchronized (this.mLock) {
                        updatedAfRun = this.mAfRun + 1;
                        this.mAfRun = updatedAfRun;
                        this.mAfState = 0;
                        break;
                    }
                    if (!this.mCamera.getParameters().getFocusMode().equals(Camera.Parameters.FOCUS_MODE_MANUAL)) {
                        this.mCamera.cancelAutoFocus();
                        Log.v(TAG, "cancelAutoFocus is not supported in FOCUS_MODE_MANUAL");
                    }
                    if (VERBOSE) {
                        Log.v(TAG, "processRequestTriggers - got AF_TRIGGER_CANCEL, new AF run is " + updatedAfRun);
                    }
                }
                return;
            default:
                Log.w(TAG, "processRequestTriggers - ignoring unknown control.afTrigger = " + afTrigger);
                return;
        }
    }

    public void mapResultTriggers(CameraMetadataNative result) {
        int newAfState;
        Preconditions.checkNotNull(result, "result must not be null");
        synchronized (this.mLock) {
            newAfState = this.mAfState;
        }
        if (VERBOSE && newAfState != this.mAfStatePrevious) {
            Log.v(TAG, String.format("mapResultTriggers - afState changed from %s to %s", afStateToString(this.mAfStatePrevious), afStateToString(newAfState)));
        }
        result.set(CaptureResult.CONTROL_AF_STATE, Integer.valueOf(newAfState));
        this.mAfStatePrevious = newAfState;
    }

    private static String afStateToString(int afState) {
        switch (afState) {
            case 0:
                return "INACTIVE";
            case 1:
                return "PASSIVE_SCAN";
            case 2:
                return "PASSIVE_FOCUSED";
            case 3:
                return "ACTIVE_SCAN";
            case 4:
                return "FOCUSED_LOCKED";
            case 5:
                return "NOT_FOCUSED_LOCKED";
            case 6:
                return "PASSIVE_UNFOCUSED";
            default:
                return "UNKNOWN(" + afState + ")";
        }
    }
}
