package android.media.tv;

import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.hardware.hdmi.HdmiDeviceInfo;
import android.media.PlaybackParams;
import android.media.tv.ITvInputService;
import android.media.tv.TvInputManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.util.TimedRemoteCaller;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class TvInputService extends Service {
    private static final boolean DEBUG = false;
    private static final int DETACH_OVERLAY_VIEW_TIMEOUT_MS = 5000;
    public static final String SERVICE_INTERFACE = "android.media.tv.TvInputService";
    public static final String SERVICE_META_DATA = "android.media.tv.input";
    private static final String TAG = "TvInputService";
    private TvInputManager mTvInputManager;
    private final Handler mServiceHandler = new ServiceHandler();
    private final RemoteCallbackList<ITvInputServiceCallback> mCallbacks = new RemoteCallbackList<>();

    public abstract Session onCreateSession(String str);

    @Override
    public final IBinder onBind(Intent intent) {
        return new ITvInputService.Stub() {
            @Override
            public void registerCallback(ITvInputServiceCallback iTvInputServiceCallback) {
                if (iTvInputServiceCallback != null) {
                    TvInputService.this.mCallbacks.register(iTvInputServiceCallback);
                }
            }

            @Override
            public void unregisterCallback(ITvInputServiceCallback iTvInputServiceCallback) {
                if (iTvInputServiceCallback != null) {
                    TvInputService.this.mCallbacks.unregister(iTvInputServiceCallback);
                }
            }

            @Override
            public void createSession(InputChannel inputChannel, ITvInputSessionCallback iTvInputSessionCallback, String str) {
                if (inputChannel == null) {
                    Log.w(TvInputService.TAG, "Creating session without input channel");
                }
                if (iTvInputSessionCallback == null) {
                    return;
                }
                SomeArgs someArgsObtain = SomeArgs.obtain();
                someArgsObtain.arg1 = inputChannel;
                someArgsObtain.arg2 = iTvInputSessionCallback;
                someArgsObtain.arg3 = str;
                TvInputService.this.mServiceHandler.obtainMessage(1, someArgsObtain).sendToTarget();
            }

            @Override
            public void createRecordingSession(ITvInputSessionCallback iTvInputSessionCallback, String str) {
                if (iTvInputSessionCallback == null) {
                    return;
                }
                SomeArgs someArgsObtain = SomeArgs.obtain();
                someArgsObtain.arg1 = iTvInputSessionCallback;
                someArgsObtain.arg2 = str;
                TvInputService.this.mServiceHandler.obtainMessage(3, someArgsObtain).sendToTarget();
            }

            @Override
            public void notifyHardwareAdded(TvInputHardwareInfo tvInputHardwareInfo) {
                TvInputService.this.mServiceHandler.obtainMessage(4, tvInputHardwareInfo).sendToTarget();
            }

            @Override
            public void notifyHardwareRemoved(TvInputHardwareInfo tvInputHardwareInfo) {
                TvInputService.this.mServiceHandler.obtainMessage(5, tvInputHardwareInfo).sendToTarget();
            }

            @Override
            public void notifyHdmiDeviceAdded(HdmiDeviceInfo hdmiDeviceInfo) {
                TvInputService.this.mServiceHandler.obtainMessage(6, hdmiDeviceInfo).sendToTarget();
            }

            @Override
            public void notifyHdmiDeviceRemoved(HdmiDeviceInfo hdmiDeviceInfo) {
                TvInputService.this.mServiceHandler.obtainMessage(7, hdmiDeviceInfo).sendToTarget();
            }
        };
    }

    public RecordingSession onCreateRecordingSession(String str) {
        return null;
    }

    @SystemApi
    public TvInputInfo onHardwareAdded(TvInputHardwareInfo tvInputHardwareInfo) {
        return null;
    }

    @SystemApi
    public String onHardwareRemoved(TvInputHardwareInfo tvInputHardwareInfo) {
        return null;
    }

    @SystemApi
    public TvInputInfo onHdmiDeviceAdded(HdmiDeviceInfo hdmiDeviceInfo) {
        return null;
    }

    @SystemApi
    public String onHdmiDeviceRemoved(HdmiDeviceInfo hdmiDeviceInfo) {
        return null;
    }

    private boolean isPassthroughInput(String str) {
        if (this.mTvInputManager == null) {
            this.mTvInputManager = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
        }
        TvInputInfo tvInputInfo = this.mTvInputManager.getTvInputInfo(str);
        return tvInputInfo != null && tvInputInfo.isPassthroughInput();
    }

    public static abstract class Session implements KeyEvent.Callback {
        private static final int POSITION_UPDATE_INTERVAL_MS = 1000;
        private final Context mContext;
        final Handler mHandler;
        private Rect mOverlayFrame;
        private View mOverlayView;
        private OverlayViewCleanUpTask mOverlayViewCleanUpTask;
        private FrameLayout mOverlayViewContainer;
        private boolean mOverlayViewEnabled;
        private ITvInputSessionCallback mSessionCallback;
        private Surface mSurface;
        private final WindowManager mWindowManager;
        private WindowManager.LayoutParams mWindowParams;
        private IBinder mWindowToken;
        private final KeyEvent.DispatcherState mDispatcherState = new KeyEvent.DispatcherState();
        private long mStartPositionMs = Long.MIN_VALUE;
        private long mCurrentPositionMs = Long.MIN_VALUE;
        private final TimeShiftPositionTrackingRunnable mTimeShiftPositionTrackingRunnable = new TimeShiftPositionTrackingRunnable();
        private final Object mLock = new Object();
        private final List<Runnable> mPendingActions = new ArrayList();

        public abstract void onRelease();

        public abstract void onSetCaptionEnabled(boolean z);

        public abstract void onSetStreamVolume(float f);

        public abstract boolean onSetSurface(Surface surface);

        public abstract boolean onTune(Uri uri);

        public Session(Context context) {
            this.mContext = context;
            this.mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            this.mHandler = new Handler(context.getMainLooper());
        }

        public void setOverlayViewEnabled(final boolean z) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (z == Session.this.mOverlayViewEnabled) {
                        return;
                    }
                    Session.this.mOverlayViewEnabled = z;
                    if (z) {
                        if (Session.this.mWindowToken != null) {
                            Session.this.createOverlayView(Session.this.mWindowToken, Session.this.mOverlayFrame);
                            return;
                        }
                        return;
                    }
                    Session.this.removeOverlayView(false);
                }
            });
        }

        @SystemApi
        public void notifySessionEvent(final String str, final Bundle bundle) {
            Preconditions.checkNotNull(str);
            executeOrPostRunnableOnMainThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onSessionEvent(str, bundle);
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInputService.TAG, "error in sending event (event=" + str + ")", e);
                    }
                }
            });
        }

        public void notifyChannelRetuned(final Uri uri) {
            executeOrPostRunnableOnMainThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onChannelRetuned(uri);
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInputService.TAG, "error in notifyChannelRetuned", e);
                    }
                }
            });
        }

        public void notifyTracksChanged(List<TvTrackInfo> list) {
            final ArrayList arrayList = new ArrayList(list);
            executeOrPostRunnableOnMainThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onTracksChanged(arrayList);
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInputService.TAG, "error in notifyTracksChanged", e);
                    }
                }
            });
        }

        public void notifyTrackSelected(final int i, final String str) {
            executeOrPostRunnableOnMainThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onTrackSelected(i, str);
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInputService.TAG, "error in notifyTrackSelected", e);
                    }
                }
            });
        }

        public void notifyVideoAvailable() {
            executeOrPostRunnableOnMainThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onVideoAvailable();
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInputService.TAG, "error in notifyVideoAvailable", e);
                    }
                }
            });
        }

        public void notifyVideoUnavailable(final int i) {
            if (i < 0 || i > 4) {
                Log.e(TvInputService.TAG, "notifyVideoUnavailable - unknown reason: " + i);
            }
            executeOrPostRunnableOnMainThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onVideoUnavailable(i);
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInputService.TAG, "error in notifyVideoUnavailable", e);
                    }
                }
            });
        }

        public void notifyContentAllowed() {
            executeOrPostRunnableOnMainThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onContentAllowed();
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInputService.TAG, "error in notifyContentAllowed", e);
                    }
                }
            });
        }

        public void notifyContentBlocked(final TvContentRating tvContentRating) {
            Preconditions.checkNotNull(tvContentRating);
            executeOrPostRunnableOnMainThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onContentBlocked(tvContentRating.flattenToString());
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInputService.TAG, "error in notifyContentBlocked", e);
                    }
                }
            });
        }

        public void notifyTimeShiftStatusChanged(final int i) {
            executeOrPostRunnableOnMainThread(new Runnable() {
                @Override
                public void run() {
                    Session.this.timeShiftEnablePositionTracking(i == 3);
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onTimeShiftStatusChanged(i);
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInputService.TAG, "error in notifyTimeShiftStatusChanged", e);
                    }
                }
            });
        }

        private void notifyTimeShiftStartPositionChanged(final long j) {
            executeOrPostRunnableOnMainThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onTimeShiftStartPositionChanged(j);
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInputService.TAG, "error in notifyTimeShiftStartPositionChanged", e);
                    }
                }
            });
        }

        private void notifyTimeShiftCurrentPositionChanged(final long j) {
            executeOrPostRunnableOnMainThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onTimeShiftCurrentPositionChanged(j);
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInputService.TAG, "error in notifyTimeShiftCurrentPositionChanged", e);
                    }
                }
            });
        }

        public void layoutSurface(final int i, final int i2, final int i3, final int i4) {
            if (i > i3 || i2 > i4) {
                throw new IllegalArgumentException("Invalid parameter");
            }
            executeOrPostRunnableOnMainThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onLayoutSurface(i, i2, i3, i4);
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInputService.TAG, "error in layoutSurface", e);
                    }
                }
            });
        }

        @SystemApi
        public void onSetMain(boolean z) {
        }

        public void onSurfaceChanged(int i, int i2, int i3) {
        }

        public void onOverlayViewSizeChanged(int i, int i2) {
        }

        public boolean onTune(Uri uri, Bundle bundle) {
            return onTune(uri);
        }

        public void onUnblockContent(TvContentRating tvContentRating) {
        }

        public boolean onSelectTrack(int i, String str) {
            return false;
        }

        public void onAppPrivateCommand(String str, Bundle bundle) {
        }

        public View onCreateOverlayView() {
            return null;
        }

        public void onTimeShiftPlay(Uri uri) {
        }

        public void onTimeShiftPause() {
        }

        public void onTimeShiftResume() {
        }

        public void onTimeShiftSeekTo(long j) {
        }

        public void onTimeShiftSetPlaybackParams(PlaybackParams playbackParams) {
        }

        public long onTimeShiftGetStartPosition() {
            return Long.MIN_VALUE;
        }

        public long onTimeShiftGetCurrentPosition() {
            return Long.MIN_VALUE;
        }

        @Override
        public boolean onKeyDown(int i, KeyEvent keyEvent) {
            return false;
        }

        @Override
        public boolean onKeyLongPress(int i, KeyEvent keyEvent) {
            return false;
        }

        @Override
        public boolean onKeyMultiple(int i, int i2, KeyEvent keyEvent) {
            return false;
        }

        @Override
        public boolean onKeyUp(int i, KeyEvent keyEvent) {
            return false;
        }

        public boolean onTouchEvent(MotionEvent motionEvent) {
            return false;
        }

        public boolean onTrackballEvent(MotionEvent motionEvent) {
            return false;
        }

        public boolean onGenericMotionEvent(MotionEvent motionEvent) {
            return false;
        }

        void release() {
            onRelease();
            if (this.mSurface != null) {
                this.mSurface.release();
                this.mSurface = null;
            }
            synchronized (this.mLock) {
                this.mSessionCallback = null;
                this.mPendingActions.clear();
            }
            removeOverlayView(true);
            this.mHandler.removeCallbacks(this.mTimeShiftPositionTrackingRunnable);
        }

        void setMain(boolean z) {
            onSetMain(z);
        }

        void setSurface(Surface surface) {
            onSetSurface(surface);
            if (this.mSurface != null) {
                this.mSurface.release();
            }
            this.mSurface = surface;
        }

        void dispatchSurfaceChanged(int i, int i2, int i3) {
            onSurfaceChanged(i, i2, i3);
        }

        void setStreamVolume(float f) {
            onSetStreamVolume(f);
        }

        void tune(Uri uri, Bundle bundle) {
            this.mCurrentPositionMs = Long.MIN_VALUE;
            onTune(uri, bundle);
        }

        void setCaptionEnabled(boolean z) {
            onSetCaptionEnabled(z);
        }

        void selectTrack(int i, String str) {
            onSelectTrack(i, str);
        }

        void unblockContent(String str) {
            onUnblockContent(TvContentRating.unflattenFromString(str));
        }

        void appPrivateCommand(String str, Bundle bundle) {
            onAppPrivateCommand(str, bundle);
        }

        void createOverlayView(IBinder iBinder, Rect rect) {
            if (this.mOverlayViewContainer != null) {
                removeOverlayView(false);
            }
            this.mWindowToken = iBinder;
            this.mOverlayFrame = rect;
            onOverlayViewSizeChanged(rect.right - rect.left, rect.bottom - rect.top);
            if (!this.mOverlayViewEnabled) {
                return;
            }
            this.mOverlayView = onCreateOverlayView();
            if (this.mOverlayView == null) {
                return;
            }
            if (this.mOverlayViewCleanUpTask != null) {
                this.mOverlayViewCleanUpTask.cancel(true);
                this.mOverlayViewCleanUpTask = null;
            }
            this.mOverlayViewContainer = new FrameLayout(this.mContext.getApplicationContext());
            this.mOverlayViewContainer.addView(this.mOverlayView);
            int i = MetricsProto.MetricsEvent.DIALOG_RUNNIGN_SERVICE;
            if (ActivityManager.isHighEndGfx()) {
                i = 16777752;
            }
            this.mWindowParams = new WindowManager.LayoutParams(rect.right - rect.left, rect.bottom - rect.top, rect.left, rect.top, 1004, i, -2);
            this.mWindowParams.privateFlags |= 64;
            this.mWindowParams.gravity = 8388659;
            this.mWindowParams.token = iBinder;
            this.mWindowManager.addView(this.mOverlayViewContainer, this.mWindowParams);
        }

        void relayoutOverlayView(Rect rect) {
            if (this.mOverlayFrame == null || this.mOverlayFrame.width() != rect.width() || this.mOverlayFrame.height() != rect.height()) {
                onOverlayViewSizeChanged(rect.right - rect.left, rect.bottom - rect.top);
            }
            this.mOverlayFrame = rect;
            if (!this.mOverlayViewEnabled || this.mOverlayViewContainer == null) {
                return;
            }
            this.mWindowParams.x = rect.left;
            this.mWindowParams.y = rect.top;
            this.mWindowParams.width = rect.right - rect.left;
            this.mWindowParams.height = rect.bottom - rect.top;
            this.mWindowManager.updateViewLayout(this.mOverlayViewContainer, this.mWindowParams);
        }

        void removeOverlayView(boolean z) {
            if (z) {
                this.mWindowToken = null;
                this.mOverlayFrame = null;
            }
            if (this.mOverlayViewContainer != null) {
                this.mOverlayViewContainer.removeView(this.mOverlayView);
                this.mOverlayView = null;
                this.mWindowManager.removeView(this.mOverlayViewContainer);
                this.mOverlayViewContainer = null;
                this.mWindowParams = null;
            }
        }

        void timeShiftPlay(Uri uri) {
            this.mCurrentPositionMs = 0L;
            onTimeShiftPlay(uri);
        }

        void timeShiftPause() {
            onTimeShiftPause();
        }

        void timeShiftResume() {
            onTimeShiftResume();
        }

        void timeShiftSeekTo(long j) {
            onTimeShiftSeekTo(j);
        }

        void timeShiftSetPlaybackParams(PlaybackParams playbackParams) {
            onTimeShiftSetPlaybackParams(playbackParams);
        }

        void timeShiftEnablePositionTracking(boolean z) {
            if (z) {
                this.mHandler.post(this.mTimeShiftPositionTrackingRunnable);
                return;
            }
            this.mHandler.removeCallbacks(this.mTimeShiftPositionTrackingRunnable);
            this.mStartPositionMs = Long.MIN_VALUE;
            this.mCurrentPositionMs = Long.MIN_VALUE;
        }

        void scheduleOverlayViewCleanup() {
            FrameLayout frameLayout = this.mOverlayViewContainer;
            if (frameLayout != null) {
                this.mOverlayViewCleanUpTask = new OverlayViewCleanUpTask();
                this.mOverlayViewCleanUpTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, frameLayout);
            }
        }

        int dispatchInputEvent(InputEvent inputEvent, InputEventReceiver inputEventReceiver) {
            boolean z;
            boolean zIsNavigationKey;
            if (inputEvent instanceof KeyEvent) {
                KeyEvent keyEvent = (KeyEvent) inputEvent;
                if (keyEvent.dispatch(this, this.mDispatcherState, this)) {
                    return 1;
                }
                zIsNavigationKey = TvInputService.isNavigationKey(keyEvent.getKeyCode());
                z = KeyEvent.isMediaKey(keyEvent.getKeyCode()) || keyEvent.getKeyCode() == 222;
            } else {
                if (inputEvent instanceof MotionEvent) {
                    MotionEvent motionEvent = (MotionEvent) inputEvent;
                    int source = motionEvent.getSource();
                    if (motionEvent.isTouchEvent()) {
                        if (onTouchEvent(motionEvent)) {
                            return 1;
                        }
                    } else if ((source & 4) != 0) {
                        if (onTrackballEvent(motionEvent)) {
                            return 1;
                        }
                    } else if (onGenericMotionEvent(motionEvent)) {
                        return 1;
                    }
                }
                z = false;
                zIsNavigationKey = false;
            }
            if (this.mOverlayViewContainer == null || !this.mOverlayViewContainer.isAttachedToWindow() || z) {
                return 0;
            }
            if (!this.mOverlayViewContainer.hasWindowFocus()) {
                this.mOverlayViewContainer.getViewRootImpl().windowFocusChanged(true, true);
            }
            if (zIsNavigationKey && this.mOverlayViewContainer.hasFocusable()) {
                this.mOverlayViewContainer.getViewRootImpl().dispatchInputEvent(inputEvent);
                return 1;
            }
            this.mOverlayViewContainer.getViewRootImpl().dispatchInputEvent(inputEvent, inputEventReceiver);
            return -1;
        }

        private void initialize(ITvInputSessionCallback iTvInputSessionCallback) {
            synchronized (this.mLock) {
                this.mSessionCallback = iTvInputSessionCallback;
                Iterator<Runnable> it = this.mPendingActions.iterator();
                while (it.hasNext()) {
                    it.next().run();
                }
                this.mPendingActions.clear();
            }
        }

        private void executeOrPostRunnableOnMainThread(Runnable runnable) {
            synchronized (this.mLock) {
                if (this.mSessionCallback == null) {
                    this.mPendingActions.add(runnable);
                } else if (this.mHandler.getLooper().isCurrentThread()) {
                    runnable.run();
                } else {
                    this.mHandler.post(runnable);
                }
            }
        }

        private final class TimeShiftPositionTrackingRunnable implements Runnable {
            private TimeShiftPositionTrackingRunnable() {
            }

            @Override
            public void run() {
                long jOnTimeShiftGetStartPosition = Session.this.onTimeShiftGetStartPosition();
                if (Session.this.mStartPositionMs == Long.MIN_VALUE || Session.this.mStartPositionMs != jOnTimeShiftGetStartPosition) {
                    Session.this.mStartPositionMs = jOnTimeShiftGetStartPosition;
                    Session.this.notifyTimeShiftStartPositionChanged(jOnTimeShiftGetStartPosition);
                }
                long jOnTimeShiftGetCurrentPosition = Session.this.onTimeShiftGetCurrentPosition();
                if (jOnTimeShiftGetCurrentPosition < Session.this.mStartPositionMs) {
                    Log.w(TvInputService.TAG, "Current position (" + jOnTimeShiftGetCurrentPosition + ") cannot be earlier than start position (" + Session.this.mStartPositionMs + "). Reset to the start position.");
                    jOnTimeShiftGetCurrentPosition = Session.this.mStartPositionMs;
                }
                if (Session.this.mCurrentPositionMs == Long.MIN_VALUE || Session.this.mCurrentPositionMs != jOnTimeShiftGetCurrentPosition) {
                    Session.this.mCurrentPositionMs = jOnTimeShiftGetCurrentPosition;
                    Session.this.notifyTimeShiftCurrentPositionChanged(jOnTimeShiftGetCurrentPosition);
                }
                Session.this.mHandler.removeCallbacks(Session.this.mTimeShiftPositionTrackingRunnable);
                Session.this.mHandler.postDelayed(Session.this.mTimeShiftPositionTrackingRunnable, 1000L);
            }
        }
    }

    private static final class OverlayViewCleanUpTask extends AsyncTask<View, Void, Void> {
        private OverlayViewCleanUpTask() {
        }

        @Override
        protected Void doInBackground(View... viewArr) {
            View view = viewArr[0];
            try {
                Thread.sleep(TimedRemoteCaller.DEFAULT_CALL_TIMEOUT_MILLIS);
                if (!isCancelled() && view.isAttachedToWindow()) {
                    Log.e(TvInputService.TAG, "Time out on releasing overlay view. Killing " + view.getContext().getPackageName());
                    Process.killProcess(Process.myPid());
                }
                return null;
            } catch (InterruptedException e) {
                return null;
            }
        }
    }

    public static abstract class RecordingSession {
        final Handler mHandler;
        private final Object mLock = new Object();
        private final List<Runnable> mPendingActions = new ArrayList();
        private ITvInputSessionCallback mSessionCallback;

        public abstract void onRelease();

        public abstract void onStartRecording(Uri uri);

        public abstract void onStopRecording();

        public abstract void onTune(Uri uri);

        public RecordingSession(Context context) {
            this.mHandler = new Handler(context.getMainLooper());
        }

        public void notifyTuned(final Uri uri) {
            executeOrPostRunnableOnMainThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (RecordingSession.this.mSessionCallback != null) {
                            RecordingSession.this.mSessionCallback.onTuned(uri);
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInputService.TAG, "error in notifyTuned", e);
                    }
                }
            });
        }

        public void notifyRecordingStopped(final Uri uri) {
            executeOrPostRunnableOnMainThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (RecordingSession.this.mSessionCallback != null) {
                            RecordingSession.this.mSessionCallback.onRecordingStopped(uri);
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInputService.TAG, "error in notifyRecordingStopped", e);
                    }
                }
            });
        }

        public void notifyError(final int i) {
            if (i < 0 || i > 2) {
                Log.w(TvInputService.TAG, "notifyError - invalid error code (" + i + ") is changed to RECORDING_ERROR_UNKNOWN.");
                i = 0;
            }
            executeOrPostRunnableOnMainThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (RecordingSession.this.mSessionCallback != null) {
                            RecordingSession.this.mSessionCallback.onError(i);
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInputService.TAG, "error in notifyError", e);
                    }
                }
            });
        }

        @SystemApi
        public void notifySessionEvent(final String str, final Bundle bundle) {
            Preconditions.checkNotNull(str);
            executeOrPostRunnableOnMainThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (RecordingSession.this.mSessionCallback != null) {
                            RecordingSession.this.mSessionCallback.onSessionEvent(str, bundle);
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInputService.TAG, "error in sending event (event=" + str + ")", e);
                    }
                }
            });
        }

        public void onTune(Uri uri, Bundle bundle) {
            onTune(uri);
        }

        public void onAppPrivateCommand(String str, Bundle bundle) {
        }

        void tune(Uri uri, Bundle bundle) {
            onTune(uri, bundle);
        }

        void release() {
            onRelease();
        }

        void startRecording(Uri uri) {
            onStartRecording(uri);
        }

        void stopRecording() {
            onStopRecording();
        }

        void appPrivateCommand(String str, Bundle bundle) {
            onAppPrivateCommand(str, bundle);
        }

        private void initialize(ITvInputSessionCallback iTvInputSessionCallback) {
            synchronized (this.mLock) {
                this.mSessionCallback = iTvInputSessionCallback;
                Iterator<Runnable> it = this.mPendingActions.iterator();
                while (it.hasNext()) {
                    it.next().run();
                }
                this.mPendingActions.clear();
            }
        }

        private void executeOrPostRunnableOnMainThread(Runnable runnable) {
            synchronized (this.mLock) {
                if (this.mSessionCallback == null) {
                    this.mPendingActions.add(runnable);
                } else if (this.mHandler.getLooper().isCurrentThread()) {
                    runnable.run();
                } else {
                    this.mHandler.post(runnable);
                }
            }
        }
    }

    public static abstract class HardwareSession extends Session {
        private TvInputManager.Session mHardwareSession;
        private final TvInputManager.SessionCallback mHardwareSessionCallback;
        private ITvInputSession mProxySession;
        private ITvInputSessionCallback mProxySessionCallback;
        private Handler mServiceHandler;

        public abstract String getHardwareInputId();

        public HardwareSession(Context context) {
            super(context);
            this.mHardwareSessionCallback = new TvInputManager.SessionCallback() {
                @Override
                public void onSessionCreated(TvInputManager.Session session) {
                    HardwareSession.this.mHardwareSession = session;
                    SomeArgs someArgsObtain = SomeArgs.obtain();
                    if (session != null) {
                        someArgsObtain.arg1 = HardwareSession.this;
                        someArgsObtain.arg2 = HardwareSession.this.mProxySession;
                        someArgsObtain.arg3 = HardwareSession.this.mProxySessionCallback;
                        someArgsObtain.arg4 = session.getToken();
                        session.tune(TvContract.buildChannelUriForPassthroughInput(HardwareSession.this.getHardwareInputId()));
                    } else {
                        someArgsObtain.arg1 = null;
                        someArgsObtain.arg2 = null;
                        someArgsObtain.arg3 = HardwareSession.this.mProxySessionCallback;
                        someArgsObtain.arg4 = null;
                        HardwareSession.this.onRelease();
                    }
                    HardwareSession.this.mServiceHandler.obtainMessage(2, someArgsObtain).sendToTarget();
                }

                @Override
                public void onVideoAvailable(TvInputManager.Session session) {
                    if (HardwareSession.this.mHardwareSession == session) {
                        HardwareSession.this.onHardwareVideoAvailable();
                    }
                }

                @Override
                public void onVideoUnavailable(TvInputManager.Session session, int i) {
                    if (HardwareSession.this.mHardwareSession == session) {
                        HardwareSession.this.onHardwareVideoUnavailable(i);
                    }
                }
            };
        }

        @Override
        public final boolean onSetSurface(Surface surface) {
            Log.e(TvInputService.TAG, "onSetSurface() should not be called in HardwareProxySession.");
            return false;
        }

        public void onHardwareVideoAvailable() {
        }

        public void onHardwareVideoUnavailable(int i) {
        }

        @Override
        void release() {
            if (this.mHardwareSession != null) {
                this.mHardwareSession.release();
                this.mHardwareSession = null;
            }
            super.release();
        }
    }

    public static boolean isNavigationKey(int i) {
        switch (i) {
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
            case 61:
            case 62:
            case 66:
            case 92:
            case 93:
            case 122:
            case 123:
                return true;
            default:
                return false;
        }
    }

    @SuppressLint({"HandlerLeak"})
    private final class ServiceHandler extends Handler {
        private static final int DO_ADD_HARDWARE_INPUT = 4;
        private static final int DO_ADD_HDMI_INPUT = 6;
        private static final int DO_CREATE_RECORDING_SESSION = 3;
        private static final int DO_CREATE_SESSION = 1;
        private static final int DO_NOTIFY_SESSION_CREATED = 2;
        private static final int DO_REMOVE_HARDWARE_INPUT = 5;
        private static final int DO_REMOVE_HDMI_INPUT = 7;

        private ServiceHandler() {
        }

        private void broadcastAddHardwareInput(int i, TvInputInfo tvInputInfo) {
            int iBeginBroadcast = TvInputService.this.mCallbacks.beginBroadcast();
            for (int i2 = 0; i2 < iBeginBroadcast; i2++) {
                try {
                    ((ITvInputServiceCallback) TvInputService.this.mCallbacks.getBroadcastItem(i2)).addHardwareInput(i, tvInputInfo);
                } catch (RemoteException e) {
                    Log.e(TvInputService.TAG, "error in broadcastAddHardwareInput", e);
                }
            }
            TvInputService.this.mCallbacks.finishBroadcast();
        }

        private void broadcastAddHdmiInput(int i, TvInputInfo tvInputInfo) {
            int iBeginBroadcast = TvInputService.this.mCallbacks.beginBroadcast();
            for (int i2 = 0; i2 < iBeginBroadcast; i2++) {
                try {
                    ((ITvInputServiceCallback) TvInputService.this.mCallbacks.getBroadcastItem(i2)).addHdmiInput(i, tvInputInfo);
                } catch (RemoteException e) {
                    Log.e(TvInputService.TAG, "error in broadcastAddHdmiInput", e);
                }
            }
            TvInputService.this.mCallbacks.finishBroadcast();
        }

        private void broadcastRemoveHardwareInput(String str) {
            int iBeginBroadcast = TvInputService.this.mCallbacks.beginBroadcast();
            for (int i = 0; i < iBeginBroadcast; i++) {
                try {
                    ((ITvInputServiceCallback) TvInputService.this.mCallbacks.getBroadcastItem(i)).removeHardwareInput(str);
                } catch (RemoteException e) {
                    Log.e(TvInputService.TAG, "error in broadcastRemoveHardwareInput", e);
                }
            }
            TvInputService.this.mCallbacks.finishBroadcast();
        }

        @Override
        public final void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    SomeArgs someArgs = (SomeArgs) message.obj;
                    InputChannel inputChannel = (InputChannel) someArgs.arg1;
                    ITvInputSessionCallback iTvInputSessionCallback = (ITvInputSessionCallback) someArgs.arg2;
                    String str = (String) someArgs.arg3;
                    someArgs.recycle();
                    Session sessionOnCreateSession = TvInputService.this.onCreateSession(str);
                    if (sessionOnCreateSession == null) {
                        try {
                            iTvInputSessionCallback.onSessionCreated(null, null);
                        } catch (RemoteException e) {
                            Log.e(TvInputService.TAG, "error in onSessionCreated", e);
                            return;
                        }
                    } else {
                        ITvInputSessionWrapper iTvInputSessionWrapper = new ITvInputSessionWrapper(TvInputService.this, sessionOnCreateSession, inputChannel);
                        if (sessionOnCreateSession instanceof HardwareSession) {
                            HardwareSession hardwareSession = (HardwareSession) sessionOnCreateSession;
                            String hardwareInputId = hardwareSession.getHardwareInputId();
                            if (!TextUtils.isEmpty(hardwareInputId) && TvInputService.this.isPassthroughInput(hardwareInputId)) {
                                hardwareSession.mProxySession = iTvInputSessionWrapper;
                                hardwareSession.mProxySessionCallback = iTvInputSessionCallback;
                                hardwareSession.mServiceHandler = TvInputService.this.mServiceHandler;
                                ((TvInputManager) TvInputService.this.getSystemService(Context.TV_INPUT_SERVICE)).createSession(hardwareInputId, hardwareSession.mHardwareSessionCallback, TvInputService.this.mServiceHandler);
                            } else {
                                if (TextUtils.isEmpty(hardwareInputId)) {
                                    Log.w(TvInputService.TAG, "Hardware input id is not setup yet.");
                                } else {
                                    Log.w(TvInputService.TAG, "Invalid hardware input id : " + hardwareInputId);
                                }
                                sessionOnCreateSession.onRelease();
                                try {
                                    iTvInputSessionCallback.onSessionCreated(null, null);
                                } catch (RemoteException e2) {
                                    Log.e(TvInputService.TAG, "error in onSessionCreated", e2);
                                    return;
                                }
                            }
                        } else {
                            SomeArgs someArgsObtain = SomeArgs.obtain();
                            someArgsObtain.arg1 = sessionOnCreateSession;
                            someArgsObtain.arg2 = iTvInputSessionWrapper;
                            someArgsObtain.arg3 = iTvInputSessionCallback;
                            someArgsObtain.arg4 = null;
                            TvInputService.this.mServiceHandler.obtainMessage(2, someArgsObtain).sendToTarget();
                        }
                    }
                    break;
                case 2:
                    SomeArgs someArgs2 = (SomeArgs) message.obj;
                    Session session = (Session) someArgs2.arg1;
                    ITvInputSession iTvInputSession = (ITvInputSession) someArgs2.arg2;
                    ITvInputSessionCallback iTvInputSessionCallback2 = (ITvInputSessionCallback) someArgs2.arg3;
                    try {
                        iTvInputSessionCallback2.onSessionCreated(iTvInputSession, (IBinder) someArgs2.arg4);
                    } catch (RemoteException e3) {
                        Log.e(TvInputService.TAG, "error in onSessionCreated", e3);
                    }
                    if (session != null) {
                        session.initialize(iTvInputSessionCallback2);
                    }
                    someArgs2.recycle();
                    break;
                case 3:
                    SomeArgs someArgs3 = (SomeArgs) message.obj;
                    ITvInputSessionCallback iTvInputSessionCallback3 = (ITvInputSessionCallback) someArgs3.arg1;
                    String str2 = (String) someArgs3.arg2;
                    someArgs3.recycle();
                    RecordingSession recordingSessionOnCreateRecordingSession = TvInputService.this.onCreateRecordingSession(str2);
                    if (recordingSessionOnCreateRecordingSession == null) {
                        try {
                            iTvInputSessionCallback3.onSessionCreated(null, null);
                        } catch (RemoteException e4) {
                            Log.e(TvInputService.TAG, "error in onSessionCreated", e4);
                            return;
                        }
                    } else {
                        try {
                            iTvInputSessionCallback3.onSessionCreated(new ITvInputSessionWrapper(TvInputService.this, recordingSessionOnCreateRecordingSession), null);
                        } catch (RemoteException e5) {
                            Log.e(TvInputService.TAG, "error in onSessionCreated", e5);
                        }
                        recordingSessionOnCreateRecordingSession.initialize(iTvInputSessionCallback3);
                    }
                    break;
                case 4:
                    TvInputHardwareInfo tvInputHardwareInfo = (TvInputHardwareInfo) message.obj;
                    TvInputInfo tvInputInfoOnHardwareAdded = TvInputService.this.onHardwareAdded(tvInputHardwareInfo);
                    if (tvInputInfoOnHardwareAdded != null) {
                        broadcastAddHardwareInput(tvInputHardwareInfo.getDeviceId(), tvInputInfoOnHardwareAdded);
                    }
                    break;
                case 5:
                    String strOnHardwareRemoved = TvInputService.this.onHardwareRemoved((TvInputHardwareInfo) message.obj);
                    if (strOnHardwareRemoved != null) {
                        broadcastRemoveHardwareInput(strOnHardwareRemoved);
                    }
                    break;
                case 6:
                    HdmiDeviceInfo hdmiDeviceInfo = (HdmiDeviceInfo) message.obj;
                    TvInputInfo tvInputInfoOnHdmiDeviceAdded = TvInputService.this.onHdmiDeviceAdded(hdmiDeviceInfo);
                    if (tvInputInfoOnHdmiDeviceAdded != null) {
                        broadcastAddHdmiInput(hdmiDeviceInfo.getId(), tvInputInfoOnHdmiDeviceAdded);
                    }
                    break;
                case 7:
                    String strOnHdmiDeviceRemoved = TvInputService.this.onHdmiDeviceRemoved((HdmiDeviceInfo) message.obj);
                    if (strOnHdmiDeviceRemoved != null) {
                        broadcastRemoveHardwareInput(strOnHdmiDeviceRemoved);
                    }
                    break;
                default:
                    Log.w(TvInputService.TAG, "Unhandled message code: " + message.what);
                    break;
            }
        }
    }
}
