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
    private final Camera mCamera;
    private static String TAG = "LegacyFocusStateMapper";
    private static final boolean DEBUG = ParameterUtils.DEBUG;
    private int mAfStatePrevious = 0;
    private String mAfModePrevious = null;
    private final Object mLock = new Object();
    private int mAfRun = 0;
    private int mAfState = 0;

    public LegacyFocusStateMapper(Camera camera) {
        this.mCamera = (Camera) Preconditions.checkNotNull(camera, "camera must not be null");
    }

    public void processRequestTriggers(CaptureRequest captureRequest, Camera.Parameters parameters) {
        final int i;
        byte b;
        final int i2;
        int i3;
        Preconditions.checkNotNull(captureRequest, "captureRequest must not be null");
        int i4 = 0;
        int iIntValue = ((Integer) ParamsUtils.getOrDefault(captureRequest, CaptureRequest.CONTROL_AF_TRIGGER, 0)).intValue();
        final String focusMode = parameters.getFocusMode();
        if (!Objects.equals(this.mAfModePrevious, focusMode)) {
            if (DEBUG) {
                Log.v(TAG, "processRequestTriggers - AF mode switched from " + this.mAfModePrevious + " to " + focusMode);
            }
            synchronized (this.mLock) {
                this.mAfRun++;
                this.mAfState = 0;
            }
            this.mCamera.cancelAutoFocus();
        }
        this.mAfModePrevious = focusMode;
        synchronized (this.mLock) {
            i = this.mAfRun;
        }
        Camera.AutoFocusMoveCallback autoFocusMoveCallback = new Camera.AutoFocusMoveCallback() {
            @Override
            public void onAutoFocusMoving(boolean z, Camera camera) {
                int i5;
                synchronized (LegacyFocusStateMapper.this.mLock) {
                    int i6 = LegacyFocusStateMapper.this.mAfRun;
                    if (LegacyFocusStateMapper.DEBUG) {
                        Log.v(LegacyFocusStateMapper.TAG, "onAutoFocusMoving - start " + z + " latest AF run " + i6 + ", last AF run " + i);
                    }
                    if (i != i6) {
                        Log.d(LegacyFocusStateMapper.TAG, "onAutoFocusMoving - ignoring move callbacks from old af run" + i);
                        return;
                    }
                    byte b2 = 1;
                    if (!z) {
                        i5 = 2;
                    } else {
                        i5 = 1;
                    }
                    String str = focusMode;
                    int iHashCode = str.hashCode();
                    if (iHashCode != -194628547) {
                        b2 = (iHashCode == 910005312 && str.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) ? (byte) 0 : (byte) -1;
                    } else if (str.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                    }
                    switch (b2) {
                        case 0:
                        case 1:
                            break;
                        default:
                            Log.w(LegacyFocusStateMapper.TAG, "onAutoFocus - got unexpected onAutoFocus in mode " + focusMode);
                            break;
                    }
                    LegacyFocusStateMapper.this.mAfState = i5;
                }
            }
        };
        int iHashCode = focusMode.hashCode();
        byte b2 = 2;
        if (iHashCode != -194628547) {
            if (iHashCode != 3005871) {
                if (iHashCode != 103652300) {
                    b = (iHashCode == 910005312 && focusMode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) ? (byte) 2 : (byte) -1;
                } else if (focusMode.equals(Camera.Parameters.FOCUS_MODE_MACRO)) {
                    b = 1;
                }
            } else if (focusMode.equals("auto")) {
                b = 0;
            }
        } else if (focusMode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            b = 3;
        }
        switch (b) {
            case 0:
            case 1:
            case 2:
            case 3:
                this.mCamera.setAutoFocusMoveCallback(autoFocusMoveCallback);
                break;
        }
        switch (iIntValue) {
            case 0:
                return;
            case 1:
                int iHashCode2 = focusMode.hashCode();
                if (iHashCode2 != -194628547) {
                    if (iHashCode2 != 3005871) {
                        if (iHashCode2 != 103652300) {
                            if (iHashCode2 != 910005312 || !focusMode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                                b2 = -1;
                            }
                        } else if (focusMode.equals(Camera.Parameters.FOCUS_MODE_MACRO)) {
                            b2 = 1;
                        }
                    } else if (focusMode.equals("auto")) {
                        b2 = 0;
                    }
                } else if (focusMode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                    b2 = 3;
                }
                switch (b2) {
                    case 0:
                    case 1:
                        i4 = 3;
                        break;
                    case 2:
                    case 3:
                        i4 = 1;
                        break;
                }
                synchronized (this.mLock) {
                    i2 = this.mAfRun + 1;
                    this.mAfRun = i2;
                    this.mAfState = i4;
                    break;
                }
                if (DEBUG) {
                    Log.v(TAG, "processRequestTriggers - got AF_TRIGGER_START, new AF run is " + i2);
                }
                if (i4 != 0) {
                    this.mCamera.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean z, Camera camera) {
                            int i5;
                            synchronized (LegacyFocusStateMapper.this.mLock) {
                                int i6 = LegacyFocusStateMapper.this.mAfRun;
                                if (LegacyFocusStateMapper.DEBUG) {
                                    Log.v(LegacyFocusStateMapper.TAG, "onAutoFocus - success " + z + " latest AF run " + i6 + ", last AF run " + i2);
                                }
                                byte b3 = 1;
                                if (i6 != i2) {
                                    Log.d(LegacyFocusStateMapper.TAG, String.format("onAutoFocus - ignoring AF callback (old run %d, new run %d)", Integer.valueOf(i2), Integer.valueOf(i6)));
                                    return;
                                }
                                if (z) {
                                    i5 = 4;
                                } else {
                                    i5 = 5;
                                }
                                String str = focusMode;
                                int iHashCode3 = str.hashCode();
                                if (iHashCode3 != -194628547) {
                                    if (iHashCode3 != 3005871) {
                                        if (iHashCode3 != 103652300) {
                                            if (iHashCode3 != 910005312 || !str.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                                                b3 = -1;
                                            }
                                        } else if (str.equals(Camera.Parameters.FOCUS_MODE_MACRO)) {
                                            b3 = 3;
                                        }
                                    } else if (str.equals("auto")) {
                                        b3 = 0;
                                    }
                                } else if (str.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                                    b3 = 2;
                                }
                                switch (b3) {
                                    case 0:
                                    case 1:
                                    case 2:
                                    case 3:
                                        break;
                                    default:
                                        Log.w(LegacyFocusStateMapper.TAG, "onAutoFocus - got unexpected onAutoFocus in mode " + focusMode);
                                        break;
                                }
                                LegacyFocusStateMapper.this.mAfState = i5;
                            }
                        }
                    });
                    return;
                }
                return;
            case 2:
                synchronized (this.mLock) {
                    synchronized (this.mLock) {
                        i3 = this.mAfRun + 1;
                        this.mAfRun = i3;
                        this.mAfState = 0;
                        break;
                    }
                    this.mCamera.cancelAutoFocus();
                    if (DEBUG) {
                        Log.v(TAG, "processRequestTriggers - got AF_TRIGGER_CANCEL, new AF run is " + i3);
                    }
                }
                return;
            default:
                Log.w(TAG, "processRequestTriggers - ignoring unknown control.afTrigger = " + iIntValue);
                return;
        }
    }

    public void mapResultTriggers(CameraMetadataNative cameraMetadataNative) {
        int i;
        Preconditions.checkNotNull(cameraMetadataNative, "result must not be null");
        synchronized (this.mLock) {
            i = this.mAfState;
        }
        if (DEBUG && i != this.mAfStatePrevious) {
            Log.v(TAG, String.format("mapResultTriggers - afState changed from %s to %s", afStateToString(this.mAfStatePrevious), afStateToString(i)));
        }
        cameraMetadataNative.set(CaptureResult.CONTROL_AF_STATE, Integer.valueOf(i));
        this.mAfStatePrevious = i;
    }

    private static String afStateToString(int i) {
        switch (i) {
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
                return "UNKNOWN(" + i + ")";
        }
    }
}
