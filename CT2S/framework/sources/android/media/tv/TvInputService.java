package android.media.tv;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.hardware.hdmi.HdmiDeviceInfo;
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
import com.android.internal.os.SomeArgs;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class TvInputService extends Service {
    private static final boolean DEBUG = false;
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
            public void registerCallback(ITvInputServiceCallback cb) {
                if (cb != null) {
                    TvInputService.this.mCallbacks.register(cb);
                }
            }

            @Override
            public void unregisterCallback(ITvInputServiceCallback cb) {
                if (cb != null) {
                    TvInputService.this.mCallbacks.unregister(cb);
                }
            }

            @Override
            public void createSession(InputChannel channel, ITvInputSessionCallback cb, String inputId) {
                if (channel == null) {
                    Log.w(TvInputService.TAG, "Creating session without input channel");
                }
                if (cb != null) {
                    SomeArgs args = SomeArgs.obtain();
                    args.arg1 = channel;
                    args.arg2 = cb;
                    args.arg3 = inputId;
                    TvInputService.this.mServiceHandler.obtainMessage(1, args).sendToTarget();
                }
            }

            @Override
            public void notifyHardwareAdded(TvInputHardwareInfo hardwareInfo) {
                TvInputService.this.mServiceHandler.obtainMessage(3, hardwareInfo).sendToTarget();
            }

            @Override
            public void notifyHardwareRemoved(TvInputHardwareInfo hardwareInfo) {
                TvInputService.this.mServiceHandler.obtainMessage(4, hardwareInfo).sendToTarget();
            }

            @Override
            public void notifyHdmiDeviceAdded(HdmiDeviceInfo deviceInfo) {
                TvInputService.this.mServiceHandler.obtainMessage(5, deviceInfo).sendToTarget();
            }

            @Override
            public void notifyHdmiDeviceRemoved(HdmiDeviceInfo deviceInfo) {
                TvInputService.this.mServiceHandler.obtainMessage(6, deviceInfo).sendToTarget();
            }
        };
    }

    public TvInputInfo onHardwareAdded(TvInputHardwareInfo hardwareInfo) {
        return null;
    }

    public String onHardwareRemoved(TvInputHardwareInfo hardwareInfo) {
        return null;
    }

    public TvInputInfo onHdmiDeviceAdded(HdmiDeviceInfo deviceInfo) {
        return null;
    }

    public String onHdmiDeviceRemoved(HdmiDeviceInfo deviceInfo) {
        return null;
    }

    private boolean isPassthroughInput(String inputId) {
        if (this.mTvInputManager == null) {
            this.mTvInputManager = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
        }
        TvInputInfo info = this.mTvInputManager.getTvInputInfo(inputId);
        return info != null && info.isPassthroughInput();
    }

    public static abstract class Session implements KeyEvent.Callback {
        private static final int DETACH_OVERLAY_VIEW_TIMEOUT = 5000;
        private Context mContext;
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
        private Object mLock = new Object();
        private List<Runnable> mPendingActions = new ArrayList();

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

        public void setOverlayViewEnabled(final boolean enable) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (enable != Session.this.mOverlayViewEnabled) {
                        Session.this.mOverlayViewEnabled = enable;
                        if (enable) {
                            if (Session.this.mWindowToken != null) {
                                Session.this.createOverlayView(Session.this.mWindowToken, Session.this.mOverlayFrame);
                                return;
                            }
                            return;
                        }
                        Session.this.removeOverlayView(false);
                    }
                }
            });
        }

        public void notifySessionEvent(final String eventType, final Bundle eventArgs) {
            if (eventType == null) {
                throw new IllegalArgumentException("eventType should not be null.");
            }
            executeOrPostRunnable(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onSessionEvent(eventType, eventArgs);
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInputService.TAG, "error in sending event (event=" + eventType + ")");
                    }
                }
            });
        }

        public void notifyChannelRetuned(final Uri channelUri) {
            executeOrPostRunnable(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onChannelRetuned(channelUri);
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInputService.TAG, "error in notifyChannelRetuned");
                    }
                }
            });
        }

        public void notifyTracksChanged(final List<TvTrackInfo> tracks) {
            Set<String> trackIdSet = new HashSet<>();
            for (TvTrackInfo track : tracks) {
                String trackId = track.getId();
                if (trackIdSet.contains(trackId)) {
                    throw new IllegalArgumentException("redundant track ID: " + trackId);
                }
                trackIdSet.add(trackId);
            }
            trackIdSet.clear();
            executeOrPostRunnable(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onTracksChanged(tracks);
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInputService.TAG, "error in notifyTracksChanged");
                    }
                }
            });
        }

        public void notifyTrackSelected(final int type, final String trackId) {
            executeOrPostRunnable(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onTrackSelected(type, trackId);
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInputService.TAG, "error in notifyTrackSelected");
                    }
                }
            });
        }

        public void notifyVideoAvailable() {
            executeOrPostRunnable(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onVideoAvailable();
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInputService.TAG, "error in notifyVideoAvailable");
                    }
                }
            });
        }

        public void notifyVideoUnavailable(final int reason) {
            if (reason < 0 || reason > 3) {
                throw new IllegalArgumentException("Unknown reason: " + reason);
            }
            executeOrPostRunnable(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onVideoUnavailable(reason);
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInputService.TAG, "error in notifyVideoUnavailable");
                    }
                }
            });
        }

        public void notifyContentAllowed() {
            executeOrPostRunnable(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onContentAllowed();
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInputService.TAG, "error in notifyContentAllowed");
                    }
                }
            });
        }

        public void notifyContentBlocked(final TvContentRating rating) {
            executeOrPostRunnable(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onContentBlocked(rating.flattenToString());
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInputService.TAG, "error in notifyContentBlocked");
                    }
                }
            });
        }

        public void layoutSurface(final int left, final int top, final int right, final int bottom) {
            if (left > right || top > bottom) {
                throw new IllegalArgumentException("Invalid parameter");
            }
            executeOrPostRunnable(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (Session.this.mSessionCallback != null) {
                            Session.this.mSessionCallback.onLayoutSurface(left, top, right, bottom);
                        }
                    } catch (RemoteException e) {
                        Log.w(TvInputService.TAG, "error in layoutSurface");
                    }
                }
            });
        }

        public void onSetMain(boolean isMain) {
        }

        public void onSurfaceChanged(int format, int width, int height) {
        }

        public void onOverlayViewSizeChanged(int width, int height) {
        }

        public boolean onTune(Uri channelUri, Bundle params) {
            return onTune(channelUri);
        }

        public void onUnblockContent(TvContentRating unblockedRating) {
        }

        public boolean onSelectTrack(int type, String trackId) {
            return false;
        }

        public void onAppPrivateCommand(String action, Bundle data) {
        }

        public View onCreateOverlayView() {
            return null;
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            return false;
        }

        @Override
        public boolean onKeyLongPress(int keyCode, KeyEvent event) {
            return false;
        }

        @Override
        public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
            return false;
        }

        @Override
        public boolean onKeyUp(int keyCode, KeyEvent event) {
            return false;
        }

        public boolean onTouchEvent(MotionEvent event) {
            return false;
        }

        public boolean onTrackballEvent(MotionEvent event) {
            return false;
        }

        public boolean onGenericMotionEvent(MotionEvent event) {
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
        }

        void setMain(boolean isMain) {
            onSetMain(isMain);
        }

        void setSurface(Surface surface) {
            onSetSurface(surface);
            if (this.mSurface != null) {
                this.mSurface.release();
            }
            this.mSurface = surface;
        }

        void dispatchSurfaceChanged(int format, int width, int height) {
            onSurfaceChanged(format, width, height);
        }

        void setStreamVolume(float volume) {
            onSetStreamVolume(volume);
        }

        void tune(Uri channelUri, Bundle params) {
            onTune(channelUri, params);
        }

        void setCaptionEnabled(boolean enabled) {
            onSetCaptionEnabled(enabled);
        }

        void selectTrack(int type, String trackId) {
            onSelectTrack(type, trackId);
        }

        void unblockContent(String unblockedRating) {
            onUnblockContent(TvContentRating.unflattenFromString(unblockedRating));
        }

        void appPrivateCommand(String action, Bundle data) {
            onAppPrivateCommand(action, data);
        }

        void createOverlayView(IBinder windowToken, Rect frame) {
            if (this.mOverlayViewContainer != null) {
                removeOverlayView(false);
            }
            this.mWindowToken = windowToken;
            this.mOverlayFrame = frame;
            onOverlayViewSizeChanged(frame.right - frame.left, frame.bottom - frame.top);
            if (this.mOverlayViewEnabled) {
                this.mOverlayView = onCreateOverlayView();
                if (this.mOverlayView != null) {
                    if (this.mOverlayViewCleanUpTask != null) {
                        this.mOverlayViewCleanUpTask.cancel(true);
                        this.mOverlayViewCleanUpTask = null;
                    }
                    this.mOverlayViewContainer = new FrameLayout(this.mContext);
                    this.mOverlayViewContainer.addView(this.mOverlayView);
                    this.mWindowParams = new WindowManager.LayoutParams(frame.right - frame.left, frame.bottom - frame.top, frame.left, frame.top, 1004, 536, -2);
                    this.mWindowParams.privateFlags |= 64;
                    this.mWindowParams.gravity = 8388659;
                    this.mWindowParams.token = windowToken;
                    this.mWindowManager.addView(this.mOverlayViewContainer, this.mWindowParams);
                }
            }
        }

        void relayoutOverlayView(Rect frame) {
            if (this.mOverlayFrame == null || this.mOverlayFrame.width() != frame.width() || this.mOverlayFrame.height() != frame.height()) {
                onOverlayViewSizeChanged(frame.right - frame.left, frame.bottom - frame.top);
            }
            this.mOverlayFrame = frame;
            if (this.mOverlayViewEnabled && this.mOverlayViewContainer != null) {
                this.mWindowParams.x = frame.left;
                this.mWindowParams.y = frame.top;
                this.mWindowParams.width = frame.right - frame.left;
                this.mWindowParams.height = frame.bottom - frame.top;
                this.mWindowManager.updateViewLayout(this.mOverlayViewContainer, this.mWindowParams);
            }
        }

        void removeOverlayView(boolean clearWindowToken) {
            if (clearWindowToken) {
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

        void scheduleOverlayViewCleanup() {
            View overlayViewParent = this.mOverlayViewContainer;
            if (overlayViewParent != null) {
                this.mOverlayViewCleanUpTask = new OverlayViewCleanUpTask();
                this.mOverlayViewCleanUpTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, overlayViewParent);
            }
        }

        int dispatchInputEvent(InputEvent event, InputEventReceiver receiver) {
            boolean isNavigationKey = false;
            if (event instanceof KeyEvent) {
                KeyEvent keyEvent = (KeyEvent) event;
                isNavigationKey = TvInputService.isNavigationKey(keyEvent.getKeyCode());
                if (keyEvent.dispatch(this, this.mDispatcherState, this)) {
                    return 1;
                }
            } else if (event instanceof MotionEvent) {
                MotionEvent motionEvent = (MotionEvent) event;
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
            if (this.mOverlayViewContainer == null || !this.mOverlayViewContainer.isAttachedToWindow()) {
                return 0;
            }
            if (!this.mOverlayViewContainer.hasWindowFocus()) {
                this.mOverlayViewContainer.getViewRootImpl().windowFocusChanged(true, true);
            }
            if (isNavigationKey && this.mOverlayViewContainer.hasFocusable()) {
                this.mOverlayViewContainer.getViewRootImpl().dispatchInputEvent(event);
                return 1;
            }
            this.mOverlayViewContainer.getViewRootImpl().dispatchInputEvent(event, receiver);
            return -1;
        }

        private void initialize(ITvInputSessionCallback callback) {
            synchronized (this.mLock) {
                this.mSessionCallback = callback;
                for (Runnable runnable : this.mPendingActions) {
                    runnable.run();
                }
                this.mPendingActions.clear();
            }
        }

        private final void executeOrPostRunnable(Runnable action) {
            synchronized (this.mLock) {
                if (this.mSessionCallback == null) {
                    this.mPendingActions.add(action);
                } else if (this.mHandler.getLooper().isCurrentThread()) {
                    action.run();
                } else {
                    this.mHandler.post(action);
                }
            }
        }

        private final class OverlayViewCleanUpTask extends AsyncTask<View, Void, Void> {
            private OverlayViewCleanUpTask() {
            }

            @Override
            protected Void doInBackground(View... views) {
                View overlayViewParent = views[0];
                try {
                    Thread.sleep(TimedRemoteCaller.DEFAULT_CALL_TIMEOUT_MILLIS);
                    if (!isCancelled() && overlayViewParent.isAttachedToWindow()) {
                        Log.e(TvInputService.TAG, "Time out on releasing overlay view. Killing " + overlayViewParent.getContext().getPackageName());
                        Process.killProcess(Process.myPid());
                    }
                } catch (InterruptedException e) {
                }
                return null;
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
                    SomeArgs args = SomeArgs.obtain();
                    if (session != null) {
                        args.arg1 = HardwareSession.this;
                        args.arg2 = HardwareSession.this.mProxySession;
                        args.arg3 = HardwareSession.this.mProxySessionCallback;
                        args.arg4 = session.getToken();
                    } else {
                        args.arg1 = null;
                        args.arg2 = null;
                        args.arg3 = HardwareSession.this.mProxySessionCallback;
                        args.arg4 = null;
                        HardwareSession.this.onRelease();
                    }
                    HardwareSession.this.mServiceHandler.obtainMessage(2, args).sendToTarget();
                    session.tune(TvContract.buildChannelUriForPassthroughInput(HardwareSession.this.getHardwareInputId()));
                }

                @Override
                public void onVideoAvailable(TvInputManager.Session session) {
                    if (HardwareSession.this.mHardwareSession == session) {
                        HardwareSession.this.onHardwareVideoAvailable();
                    }
                }

                @Override
                public void onVideoUnavailable(TvInputManager.Session session, int reason) {
                    if (HardwareSession.this.mHardwareSession == session) {
                        HardwareSession.this.onHardwareVideoUnavailable(reason);
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

        public void onHardwareVideoUnavailable(int reason) {
        }
    }

    public static boolean isNavigationKey(int keyCode) {
        switch (keyCode) {
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
        private static final int DO_ADD_HARDWARE_TV_INPUT = 3;
        private static final int DO_ADD_HDMI_TV_INPUT = 5;
        private static final int DO_CREATE_SESSION = 1;
        private static final int DO_NOTIFY_SESSION_CREATED = 2;
        private static final int DO_REMOVE_HARDWARE_TV_INPUT = 4;
        private static final int DO_REMOVE_HDMI_TV_INPUT = 6;

        private ServiceHandler() {
        }

        private void broadcastAddHardwareTvInput(int deviceId, TvInputInfo inputInfo) {
            int n = TvInputService.this.mCallbacks.beginBroadcast();
            for (int i = 0; i < n; i++) {
                try {
                    ((ITvInputServiceCallback) TvInputService.this.mCallbacks.getBroadcastItem(i)).addHardwareTvInput(deviceId, inputInfo);
                } catch (RemoteException e) {
                    Log.e(TvInputService.TAG, "Error while broadcasting.", e);
                }
            }
            TvInputService.this.mCallbacks.finishBroadcast();
        }

        private void broadcastAddHdmiTvInput(int id, TvInputInfo inputInfo) {
            int n = TvInputService.this.mCallbacks.beginBroadcast();
            for (int i = 0; i < n; i++) {
                try {
                    ((ITvInputServiceCallback) TvInputService.this.mCallbacks.getBroadcastItem(i)).addHdmiTvInput(id, inputInfo);
                } catch (RemoteException e) {
                    Log.e(TvInputService.TAG, "Error while broadcasting.", e);
                }
            }
            TvInputService.this.mCallbacks.finishBroadcast();
        }

        private void broadcastRemoveTvInput(String inputId) {
            int n = TvInputService.this.mCallbacks.beginBroadcast();
            for (int i = 0; i < n; i++) {
                try {
                    ((ITvInputServiceCallback) TvInputService.this.mCallbacks.getBroadcastItem(i)).removeTvInput(inputId);
                } catch (RemoteException e) {
                    Log.e(TvInputService.TAG, "Error while broadcasting.", e);
                }
            }
            TvInputService.this.mCallbacks.finishBroadcast();
        }

        @Override
        public final void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    SomeArgs args = (SomeArgs) msg.obj;
                    InputChannel channel = (InputChannel) args.arg1;
                    ITvInputSessionCallback cb = (ITvInputSessionCallback) args.arg2;
                    String inputId = (String) args.arg3;
                    args.recycle();
                    Session sessionImpl = TvInputService.this.onCreateSession(inputId);
                    if (sessionImpl == null) {
                        try {
                            cb.onSessionCreated(null, null);
                        } catch (RemoteException e) {
                            Log.e(TvInputService.TAG, "error in onSessionCreated");
                            return;
                        }
                    } else {
                        ITvInputSession stub = new ITvInputSessionWrapper(TvInputService.this, sessionImpl, channel);
                        if (sessionImpl instanceof HardwareSession) {
                            HardwareSession proxySession = (HardwareSession) sessionImpl;
                            String harewareInputId = proxySession.getHardwareInputId();
                            if (!TextUtils.isEmpty(harewareInputId) && TvInputService.this.isPassthroughInput(harewareInputId)) {
                                proxySession.mProxySession = stub;
                                proxySession.mProxySessionCallback = cb;
                                proxySession.mServiceHandler = TvInputService.this.mServiceHandler;
                                TvInputManager manager = (TvInputManager) TvInputService.this.getSystemService(Context.TV_INPUT_SERVICE);
                                manager.createSession(harewareInputId, proxySession.mHardwareSessionCallback, TvInputService.this.mServiceHandler);
                            } else {
                                if (TextUtils.isEmpty(harewareInputId)) {
                                    Log.w(TvInputService.TAG, "Hardware input id is not setup yet.");
                                } else {
                                    Log.w(TvInputService.TAG, "Invalid hardware input id : " + harewareInputId);
                                }
                                sessionImpl.onRelease();
                                try {
                                    cb.onSessionCreated(null, null);
                                } catch (RemoteException e2) {
                                    Log.e(TvInputService.TAG, "error in onSessionCreated");
                                    return;
                                }
                            }
                        } else {
                            SomeArgs someArgs = SomeArgs.obtain();
                            someArgs.arg1 = sessionImpl;
                            someArgs.arg2 = stub;
                            someArgs.arg3 = cb;
                            someArgs.arg4 = null;
                            TvInputService.this.mServiceHandler.obtainMessage(2, someArgs).sendToTarget();
                        }
                    }
                    break;
                case 2:
                    SomeArgs args2 = (SomeArgs) msg.obj;
                    Session sessionImpl2 = (Session) args2.arg1;
                    ITvInputSession stub2 = (ITvInputSession) args2.arg2;
                    ITvInputSessionCallback cb2 = (ITvInputSessionCallback) args2.arg3;
                    IBinder hardwareSessionToken = (IBinder) args2.arg4;
                    try {
                        cb2.onSessionCreated(stub2, hardwareSessionToken);
                    } catch (RemoteException e3) {
                        Log.e(TvInputService.TAG, "error in onSessionCreated");
                    }
                    if (sessionImpl2 != null) {
                        sessionImpl2.initialize(cb2);
                    }
                    args2.recycle();
                    break;
                case 3:
                    TvInputHardwareInfo hardwareInfo = (TvInputHardwareInfo) msg.obj;
                    TvInputInfo inputInfo = TvInputService.this.onHardwareAdded(hardwareInfo);
                    if (inputInfo != null) {
                        broadcastAddHardwareTvInput(hardwareInfo.getDeviceId(), inputInfo);
                    }
                    break;
                case 4:
                    String inputId2 = TvInputService.this.onHardwareRemoved((TvInputHardwareInfo) msg.obj);
                    if (inputId2 != null) {
                        broadcastRemoveTvInput(inputId2);
                    }
                    break;
                case 5:
                    HdmiDeviceInfo deviceInfo = (HdmiDeviceInfo) msg.obj;
                    TvInputInfo inputInfo2 = TvInputService.this.onHdmiDeviceAdded(deviceInfo);
                    if (inputInfo2 != null) {
                        broadcastAddHdmiTvInput(deviceInfo.getId(), inputInfo2);
                    }
                    break;
                case 6:
                    String inputId3 = TvInputService.this.onHdmiDeviceRemoved((HdmiDeviceInfo) msg.obj);
                    if (inputId3 != null) {
                        broadcastRemoveTvInput(inputId3);
                    }
                    break;
                default:
                    Log.w(TvInputService.TAG, "Unhandled message code: " + msg.what);
                    break;
            }
        }
    }
}
