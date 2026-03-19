package android.hardware.camera2.legacy;

import android.hardware.camera2.impl.CameraMetadataNative;
import android.os.Handler;
import android.util.Log;
import com.android.internal.telephony.IccCardConstants;

public class CameraDeviceState {
    public static final int NO_CAPTURE_ERROR = -1;
    private static final int STATE_CAPTURING = 4;
    private static final int STATE_CONFIGURING = 2;
    private static final int STATE_ERROR = 0;
    private static final int STATE_IDLE = 3;
    private static final int STATE_UNCONFIGURED = 1;
    private static final String TAG = "CameraDeviceState";
    private static final boolean DEBUG = ParameterUtils.DEBUG;
    private static final String[] sStateNames = {"ERROR", "UNCONFIGURED", "CONFIGURING", "IDLE", "CAPTURING"};
    private int mCurrentState = 1;
    private int mCurrentError = -1;
    private RequestHolder mCurrentRequest = null;
    private Handler mCurrentHandler = null;
    private CameraDeviceStateListener mCurrentListener = null;

    public interface CameraDeviceStateListener {
        void onBusy();

        void onCaptureResult(CameraMetadataNative cameraMetadataNative, RequestHolder requestHolder);

        void onCaptureStarted(RequestHolder requestHolder, long j);

        void onConfiguring();

        void onError(int i, Object obj, RequestHolder requestHolder);

        void onIdle();

        void onRepeatingRequestError(long j, int i);

        void onRequestQueueEmpty();
    }

    public synchronized void setError(int i) {
        this.mCurrentError = i;
        doStateTransition(0);
    }

    public synchronized boolean setConfiguring() {
        doStateTransition(2);
        return this.mCurrentError == -1;
    }

    public synchronized boolean setIdle() {
        doStateTransition(3);
        return this.mCurrentError == -1;
    }

    public synchronized boolean setCaptureStart(RequestHolder requestHolder, long j, int i) {
        this.mCurrentRequest = requestHolder;
        doStateTransition(4, j, i);
        return this.mCurrentError == -1;
    }

    public synchronized boolean setCaptureResult(final RequestHolder requestHolder, final CameraMetadataNative cameraMetadataNative, final int i, final Object obj) {
        if (this.mCurrentState != 4) {
            Log.e(TAG, "Cannot receive result while in state: " + this.mCurrentState);
            this.mCurrentError = 1;
            doStateTransition(0);
            return this.mCurrentError == -1;
        }
        if (this.mCurrentHandler != null && this.mCurrentListener != null) {
            if (i != -1) {
                this.mCurrentHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        CameraDeviceState.this.mCurrentListener.onError(i, obj, requestHolder);
                    }
                });
            } else {
                this.mCurrentHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        CameraDeviceState.this.mCurrentListener.onCaptureResult(cameraMetadataNative, requestHolder);
                    }
                });
            }
        }
        return this.mCurrentError == -1;
    }

    public synchronized boolean setCaptureResult(RequestHolder requestHolder, CameraMetadataNative cameraMetadataNative) {
        return setCaptureResult(requestHolder, cameraMetadataNative, -1, null);
    }

    public synchronized void setRepeatingRequestError(final long j, final int i) {
        this.mCurrentHandler.post(new Runnable() {
            @Override
            public void run() {
                CameraDeviceState.this.mCurrentListener.onRepeatingRequestError(j, i);
            }
        });
    }

    public synchronized void setRequestQueueEmpty() {
        this.mCurrentHandler.post(new Runnable() {
            @Override
            public void run() {
                CameraDeviceState.this.mCurrentListener.onRequestQueueEmpty();
            }
        });
    }

    public synchronized void setCameraDeviceCallbacks(Handler handler, CameraDeviceStateListener cameraDeviceStateListener) {
        this.mCurrentHandler = handler;
        this.mCurrentListener = cameraDeviceStateListener;
    }

    private void doStateTransition(int i) {
        doStateTransition(i, 0L, -1);
    }

    protected synchronized int getCurrentState() {
        return this.mCurrentState;
    }

    private void doStateTransition(int i, final long j, final int i2) {
        if (i != this.mCurrentState) {
            String str = IccCardConstants.INTENT_VALUE_ICC_UNKNOWN;
            if (i >= 0 && i < sStateNames.length) {
                str = sStateNames[i];
            }
            Log.i(TAG, "Legacy camera service transitioning to state " + str);
        }
        if (i != 0 && i != 3 && this.mCurrentState != i && this.mCurrentHandler != null && this.mCurrentListener != null) {
            this.mCurrentHandler.post(new Runnable() {
                @Override
                public void run() {
                    CameraDeviceState.this.mCurrentListener.onBusy();
                }
            });
        }
        if (i == 0) {
            if (this.mCurrentState != 0 && this.mCurrentHandler != null && this.mCurrentListener != null) {
                this.mCurrentHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        CameraDeviceState.this.mCurrentListener.onError(CameraDeviceState.this.mCurrentError, null, CameraDeviceState.this.mCurrentRequest);
                    }
                });
            }
            this.mCurrentState = 0;
            return;
        }
        switch (i) {
            case 2:
                if (this.mCurrentState != 1 && this.mCurrentState != 3) {
                    Log.e(TAG, "Cannot call configure while in state: " + this.mCurrentState);
                    this.mCurrentError = 1;
                    doStateTransition(0);
                    return;
                }
                if (this.mCurrentState != 2 && this.mCurrentHandler != null && this.mCurrentListener != null) {
                    this.mCurrentHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            CameraDeviceState.this.mCurrentListener.onConfiguring();
                        }
                    });
                }
                this.mCurrentState = 2;
                return;
            case 3:
                if (this.mCurrentState == 3) {
                    if (this.mCurrentHandler != null && this.mCurrentListener != null) {
                        this.mCurrentHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                CameraDeviceState.this.mCurrentListener.onIdle();
                            }
                        });
                        return;
                    }
                    return;
                }
                if (this.mCurrentState == 2 || this.mCurrentState == 4) {
                    if (this.mCurrentState != 3 && this.mCurrentHandler != null && this.mCurrentListener != null) {
                        this.mCurrentHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                CameraDeviceState.this.mCurrentListener.onIdle();
                            }
                        });
                    }
                    this.mCurrentState = 3;
                    return;
                }
                Log.e(TAG, "Cannot call idle while in state: " + this.mCurrentState);
                this.mCurrentError = 1;
                doStateTransition(0);
                return;
            case 4:
                if (this.mCurrentState != 3 && this.mCurrentState != 4) {
                    Log.e(TAG, "Cannot call capture while in state: " + this.mCurrentState);
                    this.mCurrentError = 1;
                    doStateTransition(0);
                    return;
                }
                if (this.mCurrentHandler != null && this.mCurrentListener != null) {
                    if (i2 != -1) {
                        this.mCurrentHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                CameraDeviceState.this.mCurrentListener.onError(i2, null, CameraDeviceState.this.mCurrentRequest);
                            }
                        });
                    } else {
                        this.mCurrentHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                CameraDeviceState.this.mCurrentListener.onCaptureStarted(CameraDeviceState.this.mCurrentRequest, j);
                            }
                        });
                    }
                }
                this.mCurrentState = 4;
                return;
            default:
                throw new IllegalStateException("Transition to unknown state: " + i);
        }
    }
}
