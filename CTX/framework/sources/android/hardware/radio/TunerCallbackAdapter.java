package android.hardware.radio;

import android.hardware.radio.ITunerCallback;
import android.hardware.radio.ProgramList;
import android.hardware.radio.RadioManager;
import android.hardware.radio.RadioTuner;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class TunerCallbackAdapter extends ITunerCallback.Stub {
    private static final String TAG = "BroadcastRadio.TunerCallbackAdapter";
    private final RadioTuner.Callback mCallback;
    RadioManager.ProgramInfo mCurrentProgramInfo;
    private final Handler mHandler;
    List<RadioManager.ProgramInfo> mLastCompleteList;
    ProgramList mProgramList;
    private final Object mLock = new Object();
    boolean mIsAntennaConnected = true;
    private boolean mDelayedCompleteCallback = false;

    TunerCallbackAdapter(RadioTuner.Callback callback, Handler handler) {
        this.mCallback = callback;
        if (handler == null) {
            this.mHandler = new Handler(Looper.getMainLooper());
        } else {
            this.mHandler = handler;
        }
    }

    void close() {
        synchronized (this.mLock) {
            if (this.mProgramList != null) {
                this.mProgramList.close();
            }
        }
    }

    void setProgramListObserver(final ProgramList programList, final ProgramList.OnCloseListener onCloseListener) {
        Objects.requireNonNull(onCloseListener);
        synchronized (this.mLock) {
            if (this.mProgramList != null) {
                Log.w(TAG, "Previous program list observer wasn't properly closed, closing it...");
                this.mProgramList.close();
            }
            this.mProgramList = programList;
            if (programList == null) {
                return;
            }
            programList.setOnCloseListener(new ProgramList.OnCloseListener() {
                @Override
                public final void onClose() {
                    TunerCallbackAdapter.lambda$setProgramListObserver$0(this.f$0, programList, onCloseListener);
                }
            });
            programList.addOnCompleteListener(new ProgramList.OnCompleteListener() {
                @Override
                public final void onComplete() {
                    TunerCallbackAdapter.lambda$setProgramListObserver$1(this.f$0, programList);
                }
            });
        }
    }

    public static void lambda$setProgramListObserver$0(TunerCallbackAdapter tunerCallbackAdapter, ProgramList programList, ProgramList.OnCloseListener onCloseListener) {
        synchronized (tunerCallbackAdapter.mLock) {
            if (tunerCallbackAdapter.mProgramList != programList) {
                return;
            }
            tunerCallbackAdapter.mProgramList = null;
            tunerCallbackAdapter.mLastCompleteList = null;
            onCloseListener.onClose();
        }
    }

    public static void lambda$setProgramListObserver$1(TunerCallbackAdapter tunerCallbackAdapter, ProgramList programList) {
        synchronized (tunerCallbackAdapter.mLock) {
            if (tunerCallbackAdapter.mProgramList != programList) {
                return;
            }
            tunerCallbackAdapter.mLastCompleteList = programList.toList();
            if (tunerCallbackAdapter.mDelayedCompleteCallback) {
                Log.d(TAG, "Sending delayed onBackgroundScanComplete callback");
                tunerCallbackAdapter.sendBackgroundScanCompleteLocked();
            }
        }
    }

    List<RadioManager.ProgramInfo> getLastCompleteList() {
        List<RadioManager.ProgramInfo> list;
        synchronized (this.mLock) {
            list = this.mLastCompleteList;
        }
        return list;
    }

    void clearLastCompleteList() {
        synchronized (this.mLock) {
            this.mLastCompleteList = null;
        }
    }

    RadioManager.ProgramInfo getCurrentProgramInformation() {
        RadioManager.ProgramInfo programInfo;
        synchronized (this.mLock) {
            programInfo = this.mCurrentProgramInfo;
        }
        return programInfo;
    }

    boolean isAntennaConnected() {
        return this.mIsAntennaConnected;
    }

    @Override
    public void onError(final int i) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mCallback.onError(i);
            }
        });
    }

    @Override
    public void onTuneFailed(final int i, final ProgramSelector programSelector) {
        final int i2;
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mCallback.onTuneFailed(i, programSelector);
            }
        });
        if (i != Integer.MIN_VALUE && i != -38) {
            if (i != -32) {
                if (i != -22 && i != -19) {
                    if (i != -1) {
                    }
                }
                i2 = 3;
            }
            i2 = 1;
        } else {
            Log.i(TAG, "Got an error with no mapping to the legacy API (" + i + "), doing a best-effort conversion to ERROR_SCAN_TIMEOUT");
            i2 = 3;
        }
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mCallback.onError(i2);
            }
        });
    }

    @Override
    public void onConfigurationChanged(final RadioManager.BandConfig bandConfig) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mCallback.onConfigurationChanged(bandConfig);
            }
        });
    }

    @Override
    public void onCurrentProgramInfoChanged(final RadioManager.ProgramInfo programInfo) {
        if (programInfo == null) {
            Log.e(TAG, "ProgramInfo must not be null");
            return;
        }
        synchronized (this.mLock) {
            this.mCurrentProgramInfo = programInfo;
        }
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                TunerCallbackAdapter.lambda$onCurrentProgramInfoChanged$6(this.f$0, programInfo);
            }
        });
    }

    public static void lambda$onCurrentProgramInfoChanged$6(TunerCallbackAdapter tunerCallbackAdapter, RadioManager.ProgramInfo programInfo) {
        tunerCallbackAdapter.mCallback.onProgramInfoChanged(programInfo);
        RadioMetadata metadata = programInfo.getMetadata();
        if (metadata != null) {
            tunerCallbackAdapter.mCallback.onMetadataChanged(metadata);
        }
    }

    @Override
    public void onTrafficAnnouncement(final boolean z) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mCallback.onTrafficAnnouncement(z);
            }
        });
    }

    @Override
    public void onEmergencyAnnouncement(final boolean z) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mCallback.onEmergencyAnnouncement(z);
            }
        });
    }

    @Override
    public void onAntennaState(final boolean z) {
        this.mIsAntennaConnected = z;
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mCallback.onAntennaState(z);
            }
        });
    }

    @Override
    public void onBackgroundScanAvailabilityChange(final boolean z) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mCallback.onBackgroundScanAvailabilityChange(z);
            }
        });
    }

    private void sendBackgroundScanCompleteLocked() {
        this.mDelayedCompleteCallback = false;
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mCallback.onBackgroundScanComplete();
            }
        });
    }

    @Override
    public void onBackgroundScanComplete() {
        synchronized (this.mLock) {
            if (this.mLastCompleteList == null) {
                Log.i(TAG, "Got onBackgroundScanComplete callback, but the program list didn't get through yet. Delaying it...");
                this.mDelayedCompleteCallback = true;
            } else {
                sendBackgroundScanCompleteLocked();
            }
        }
    }

    @Override
    public void onProgramListChanged() {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mCallback.onProgramListChanged();
            }
        });
    }

    @Override
    public void onProgramListUpdated(ProgramList.Chunk chunk) {
        synchronized (this.mLock) {
            if (this.mProgramList == null) {
                return;
            }
            this.mProgramList.apply((ProgramList.Chunk) Objects.requireNonNull(chunk));
        }
    }

    @Override
    public void onParametersUpdated(final Map map) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mCallback.onParametersUpdated(map);
            }
        });
    }
}
