package android.media.tv;

import android.Manifest;
import android.annotation.SystemApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.media.PlaybackParams;
import android.media.tv.TvInputManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewRootImpl;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

public class TvView extends ViewGroup {
    private static final boolean DEBUG = false;
    private static final String TAG = "TvView";
    private static final int ZORDER_MEDIA = 0;
    private static final int ZORDER_MEDIA_OVERLAY = 1;
    private static final int ZORDER_ON_TOP = 2;
    private final AttributeSet mAttrs;
    private TvInputCallback mCallback;
    private Boolean mCaptionEnabled;
    private final int mDefStyleAttr;
    private final TvInputManager.Session.FinishedInputEventCallback mFinishedInputEventCallback;
    private final Handler mHandler;
    private OnUnhandledInputEventListener mOnUnhandledInputEventListener;
    private boolean mOverlayViewCreated;
    private Rect mOverlayViewFrame;
    private final Queue<Pair<String, Bundle>> mPendingAppPrivateCommands;
    private TvInputManager.Session mSession;
    private MySessionCallback mSessionCallback;
    private Float mStreamVolume;
    private Surface mSurface;
    private boolean mSurfaceChanged;
    private int mSurfaceFormat;
    private int mSurfaceHeight;
    private final SurfaceHolder.Callback mSurfaceHolderCallback;
    private SurfaceView mSurfaceView;
    private int mSurfaceViewBottom;
    private int mSurfaceViewLeft;
    private int mSurfaceViewRight;
    private int mSurfaceViewTop;
    private int mSurfaceWidth;
    private TimeShiftPositionCallback mTimeShiftPositionCallback;
    private final TvInputManager mTvInputManager;
    private boolean mUseRequestedSurfaceLayout;
    private int mWindowZOrder;
    private static final WeakReference<TvView> NULL_TV_VIEW = new WeakReference<>(null);
    private static final Object sMainTvViewLock = new Object();
    private static WeakReference<TvView> sMainTvView = NULL_TV_VIEW;

    public interface OnUnhandledInputEventListener {
        boolean onUnhandledInputEvent(InputEvent inputEvent);
    }

    public TvView(Context context) {
        this(context, null, 0);
    }

    public TvView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public TvView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mHandler = new Handler();
        this.mPendingAppPrivateCommands = new ArrayDeque();
        this.mSurfaceHolderCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i2, int i3, int i4) {
                TvView.this.mSurfaceFormat = i2;
                TvView.this.mSurfaceWidth = i3;
                TvView.this.mSurfaceHeight = i4;
                TvView.this.mSurfaceChanged = true;
                TvView.this.dispatchSurfaceChanged(TvView.this.mSurfaceFormat, TvView.this.mSurfaceWidth, TvView.this.mSurfaceHeight);
            }

            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                TvView.this.mSurface = surfaceHolder.getSurface();
                TvView.this.setSessionSurface(TvView.this.mSurface);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                TvView.this.mSurface = null;
                TvView.this.mSurfaceChanged = false;
                TvView.this.setSessionSurface(null);
            }
        };
        this.mFinishedInputEventCallback = new TvInputManager.Session.FinishedInputEventCallback() {
            @Override
            public void onFinishedInputEvent(Object obj, boolean z) {
                ViewRootImpl viewRootImpl;
                if (z) {
                    return;
                }
                InputEvent inputEvent = (InputEvent) obj;
                if (!TvView.this.dispatchUnhandledInputEvent(inputEvent) && (viewRootImpl = TvView.this.getViewRootImpl()) != null) {
                    viewRootImpl.dispatchUnhandledInputEvent(inputEvent);
                }
            }
        };
        this.mAttrs = attributeSet;
        this.mDefStyleAttr = i;
        resetSurfaceView();
        this.mTvInputManager = (TvInputManager) getContext().getSystemService(Context.TV_INPUT_SERVICE);
    }

    public void setCallback(TvInputCallback tvInputCallback) {
        this.mCallback = tvInputCallback;
    }

    @SystemApi
    public void setMain() {
        synchronized (sMainTvViewLock) {
            sMainTvView = new WeakReference<>(this);
            if (hasWindowFocus() && this.mSession != null) {
                this.mSession.setMain();
            }
        }
    }

    public void setZOrderMediaOverlay(boolean z) {
        if (z) {
            this.mWindowZOrder = 1;
            removeSessionOverlayView();
        } else {
            this.mWindowZOrder = 0;
            createSessionOverlayView();
        }
        if (this.mSurfaceView != null) {
            this.mSurfaceView.setZOrderOnTop(false);
            this.mSurfaceView.setZOrderMediaOverlay(z);
        }
    }

    public void setZOrderOnTop(boolean z) {
        if (z) {
            this.mWindowZOrder = 2;
            removeSessionOverlayView();
        } else {
            this.mWindowZOrder = 0;
            createSessionOverlayView();
        }
        if (this.mSurfaceView != null) {
            this.mSurfaceView.setZOrderMediaOverlay(false);
            this.mSurfaceView.setZOrderOnTop(z);
        }
    }

    public void setStreamVolume(float f) {
        this.mStreamVolume = Float.valueOf(f);
        if (this.mSession == null) {
            return;
        }
        this.mSession.setStreamVolume(f);
    }

    public void tune(String str, Uri uri) {
        tune(str, uri, null);
    }

    public void tune(String str, Uri uri, Bundle bundle) {
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("inputId cannot be null or an empty string");
        }
        synchronized (sMainTvViewLock) {
            if (sMainTvView.get() == null) {
                sMainTvView = new WeakReference<>(this);
            }
        }
        if (this.mSessionCallback != null && TextUtils.equals(this.mSessionCallback.mInputId, str)) {
            if (this.mSession != null) {
                this.mSession.tune(uri, bundle);
                return;
            } else {
                this.mSessionCallback.mChannelUri = uri;
                this.mSessionCallback.mTuneParams = bundle;
                return;
            }
        }
        resetInternal();
        this.mSessionCallback = new MySessionCallback(str, uri, bundle);
        if (this.mTvInputManager != null) {
            this.mTvInputManager.createSession(str, this.mSessionCallback, this.mHandler);
        }
    }

    public void reset() {
        synchronized (sMainTvViewLock) {
            if (this == sMainTvView.get()) {
                sMainTvView = NULL_TV_VIEW;
            }
        }
        resetInternal();
    }

    private void resetInternal() {
        this.mSessionCallback = null;
        this.mPendingAppPrivateCommands.clear();
        if (this.mSession != null) {
            setSessionSurface(null);
            removeSessionOverlayView();
            this.mUseRequestedSurfaceLayout = false;
            this.mSession.release();
            this.mSession = null;
            resetSurfaceView();
        }
    }

    public void requestUnblockContent(TvContentRating tvContentRating) {
        unblockContent(tvContentRating);
    }

    @SystemApi
    public void unblockContent(TvContentRating tvContentRating) {
        if (this.mSession != null) {
            this.mSession.unblockContent(tvContentRating);
        }
    }

    public void setCaptionEnabled(boolean z) {
        this.mCaptionEnabled = Boolean.valueOf(z);
        if (this.mSession != null) {
            this.mSession.setCaptionEnabled(z);
        }
    }

    public void selectTrack(int i, String str) {
        if (this.mSession != null) {
            this.mSession.selectTrack(i, str);
        }
    }

    public List<TvTrackInfo> getTracks(int i) {
        if (this.mSession == null) {
            return null;
        }
        return this.mSession.getTracks(i);
    }

    public String getSelectedTrack(int i) {
        if (this.mSession == null) {
            return null;
        }
        return this.mSession.getSelectedTrack(i);
    }

    public void timeShiftPlay(String str, Uri uri) {
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("inputId cannot be null or an empty string");
        }
        synchronized (sMainTvViewLock) {
            if (sMainTvView.get() == null) {
                sMainTvView = new WeakReference<>(this);
            }
        }
        if (this.mSessionCallback != null && TextUtils.equals(this.mSessionCallback.mInputId, str)) {
            if (this.mSession != null) {
                this.mSession.timeShiftPlay(uri);
                return;
            } else {
                this.mSessionCallback.mRecordedProgramUri = uri;
                return;
            }
        }
        resetInternal();
        this.mSessionCallback = new MySessionCallback(str, uri);
        if (this.mTvInputManager != null) {
            this.mTvInputManager.createSession(str, this.mSessionCallback, this.mHandler);
        }
    }

    public void timeShiftPause() {
        if (this.mSession != null) {
            this.mSession.timeShiftPause();
        }
    }

    public void timeShiftResume() {
        if (this.mSession != null) {
            this.mSession.timeShiftResume();
        }
    }

    public void timeShiftSeekTo(long j) {
        if (this.mSession != null) {
            this.mSession.timeShiftSeekTo(j);
        }
    }

    public void timeShiftSetPlaybackParams(PlaybackParams playbackParams) {
        if (this.mSession != null) {
            this.mSession.timeShiftSetPlaybackParams(playbackParams);
        }
    }

    public void setTimeShiftPositionCallback(TimeShiftPositionCallback timeShiftPositionCallback) {
        this.mTimeShiftPositionCallback = timeShiftPositionCallback;
        ensurePositionTracking();
    }

    private void ensurePositionTracking() {
        if (this.mSession == null) {
            return;
        }
        this.mSession.timeShiftEnablePositionTracking(this.mTimeShiftPositionCallback != null);
    }

    public void sendAppPrivateCommand(String str, Bundle bundle) {
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("action cannot be null or an empty string");
        }
        if (this.mSession != null) {
            this.mSession.sendAppPrivateCommand(str, bundle);
            return;
        }
        Log.w(TAG, "sendAppPrivateCommand - session not yet created (action \"" + str + "\" pending)");
        this.mPendingAppPrivateCommands.add(Pair.create(str, bundle));
    }

    public boolean dispatchUnhandledInputEvent(InputEvent inputEvent) {
        if (this.mOnUnhandledInputEventListener != null && this.mOnUnhandledInputEventListener.onUnhandledInputEvent(inputEvent)) {
            return true;
        }
        return onUnhandledInputEvent(inputEvent);
    }

    public boolean onUnhandledInputEvent(InputEvent inputEvent) {
        return false;
    }

    public void setOnUnhandledInputEventListener(OnUnhandledInputEventListener onUnhandledInputEventListener) {
        this.mOnUnhandledInputEventListener = onUnhandledInputEventListener;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        if (super.dispatchKeyEvent(keyEvent)) {
            return true;
        }
        if (this.mSession == null) {
            return false;
        }
        KeyEvent keyEventCopy = keyEvent.copy();
        return this.mSession.dispatchInputEvent(keyEventCopy, keyEventCopy, this.mFinishedInputEventCallback, this.mHandler) != 0;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        if (super.dispatchTouchEvent(motionEvent)) {
            return true;
        }
        if (this.mSession == null) {
            return false;
        }
        MotionEvent motionEventCopy = motionEvent.copy();
        return this.mSession.dispatchInputEvent(motionEventCopy, motionEventCopy, this.mFinishedInputEventCallback, this.mHandler) != 0;
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent motionEvent) {
        if (super.dispatchTrackballEvent(motionEvent)) {
            return true;
        }
        if (this.mSession == null) {
            return false;
        }
        MotionEvent motionEventCopy = motionEvent.copy();
        return this.mSession.dispatchInputEvent(motionEventCopy, motionEventCopy, this.mFinishedInputEventCallback, this.mHandler) != 0;
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent motionEvent) {
        if (super.dispatchGenericMotionEvent(motionEvent)) {
            return true;
        }
        if (this.mSession == null) {
            return false;
        }
        MotionEvent motionEventCopy = motionEvent.copy();
        return this.mSession.dispatchInputEvent(motionEventCopy, motionEventCopy, this.mFinishedInputEventCallback, this.mHandler) != 0;
    }

    @Override
    public void dispatchWindowFocusChanged(boolean z) {
        super.dispatchWindowFocusChanged(z);
        synchronized (sMainTvViewLock) {
            if (z) {
                try {
                    if (this == sMainTvView.get() && this.mSession != null && checkChangeHdmiCecActiveSourcePermission()) {
                        this.mSession.setMain();
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        createSessionOverlayView();
    }

    @Override
    protected void onDetachedFromWindow() {
        removeSessionOverlayView();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        if (this.mUseRequestedSurfaceLayout) {
            this.mSurfaceView.layout(this.mSurfaceViewLeft, this.mSurfaceViewTop, this.mSurfaceViewRight, this.mSurfaceViewBottom);
        } else {
            this.mSurfaceView.layout(0, 0, i3 - i, i4 - i2);
        }
    }

    @Override
    protected void onMeasure(int i, int i2) {
        this.mSurfaceView.measure(i, i2);
        int measuredWidth = this.mSurfaceView.getMeasuredWidth();
        int measuredHeight = this.mSurfaceView.getMeasuredHeight();
        int measuredState = this.mSurfaceView.getMeasuredState();
        setMeasuredDimension(resolveSizeAndState(measuredWidth, i, measuredState), resolveSizeAndState(measuredHeight, i2, measuredState << 16));
    }

    @Override
    public boolean gatherTransparentRegion(Region region) {
        if (this.mWindowZOrder != 2 && region != null) {
            int width = getWidth();
            int height = getHeight();
            if (width > 0 && height > 0) {
                int[] iArr = new int[2];
                getLocationInWindow(iArr);
                int i = iArr[0];
                int i2 = iArr[1];
                region.op(i, i2, i + width, i2 + height, Region.Op.UNION);
            }
        }
        return super.gatherTransparentRegion(region);
    }

    @Override
    public void draw(Canvas canvas) {
        if (this.mWindowZOrder != 2) {
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        }
        super.draw(canvas);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (this.mWindowZOrder != 2) {
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        }
        super.dispatchDraw(canvas);
    }

    @Override
    protected void onVisibilityChanged(View view, int i) {
        super.onVisibilityChanged(view, i);
        this.mSurfaceView.setVisibility(i);
        if (i == 0) {
            createSessionOverlayView();
        } else {
            removeSessionOverlayView();
        }
    }

    private void resetSurfaceView() {
        if (this.mSurfaceView != null) {
            this.mSurfaceView.getHolder().removeCallback(this.mSurfaceHolderCallback);
            removeView(this.mSurfaceView);
        }
        this.mSurface = null;
        this.mSurfaceView = new SurfaceView(getContext(), this.mAttrs, this.mDefStyleAttr) {
            @Override
            protected void updateSurface() {
                super.updateSurface();
                TvView.this.relayoutSessionOverlayView();
            }
        };
        this.mSurfaceView.setSecure(true);
        this.mSurfaceView.getHolder().addCallback(this.mSurfaceHolderCallback);
        if (this.mWindowZOrder == 1) {
            this.mSurfaceView.setZOrderMediaOverlay(true);
        } else if (this.mWindowZOrder == 2) {
            this.mSurfaceView.setZOrderOnTop(true);
        }
        addView(this.mSurfaceView);
    }

    private void setSessionSurface(Surface surface) {
        if (this.mSession == null) {
            return;
        }
        this.mSession.setSurface(surface);
    }

    private void dispatchSurfaceChanged(int i, int i2, int i3) {
        if (this.mSession == null) {
            return;
        }
        this.mSession.dispatchSurfaceChanged(i, i2, i3);
    }

    private void createSessionOverlayView() {
        if (this.mSession == null || !isAttachedToWindow() || this.mOverlayViewCreated || this.mWindowZOrder != 0) {
            return;
        }
        this.mOverlayViewFrame = getViewFrameOnScreen();
        this.mSession.createOverlayView(this, this.mOverlayViewFrame);
        this.mOverlayViewCreated = true;
    }

    private void removeSessionOverlayView() {
        if (this.mSession == null || !this.mOverlayViewCreated) {
            return;
        }
        this.mSession.removeOverlayView();
        this.mOverlayViewCreated = false;
        this.mOverlayViewFrame = null;
    }

    private void relayoutSessionOverlayView() {
        if (this.mSession == null || !isAttachedToWindow() || !this.mOverlayViewCreated || this.mWindowZOrder != 0) {
            return;
        }
        Rect viewFrameOnScreen = getViewFrameOnScreen();
        if (viewFrameOnScreen.equals(this.mOverlayViewFrame)) {
            return;
        }
        this.mSession.relayoutOverlayView(viewFrameOnScreen);
        this.mOverlayViewFrame = viewFrameOnScreen;
    }

    private Rect getViewFrameOnScreen() {
        Rect rect = new Rect();
        getGlobalVisibleRect(rect);
        RectF rectF = new RectF(rect);
        getMatrix().mapRect(rectF);
        rectF.round(rect);
        return rect;
    }

    private boolean checkChangeHdmiCecActiveSourcePermission() {
        return getContext().checkSelfPermission(Manifest.permission.CHANGE_HDMI_CEC_ACTIVE_SOURCE) == 0;
    }

    public static abstract class TimeShiftPositionCallback {
        public void onTimeShiftStartPositionChanged(String str, long j) {
        }

        public void onTimeShiftCurrentPositionChanged(String str, long j) {
        }
    }

    public static abstract class TvInputCallback {
        public void onConnectionFailed(String str) {
        }

        public void onDisconnected(String str) {
        }

        public void onChannelRetuned(String str, Uri uri) {
        }

        public void onTracksChanged(String str, List<TvTrackInfo> list) {
        }

        public void onTrackSelected(String str, int i, String str2) {
        }

        public void onVideoSizeChanged(String str, int i, int i2) {
        }

        public void onVideoAvailable(String str) {
        }

        public void onVideoUnavailable(String str, int i) {
        }

        public void onContentAllowed(String str) {
        }

        public void onContentBlocked(String str, TvContentRating tvContentRating) {
        }

        @SystemApi
        public void onEvent(String str, String str2, Bundle bundle) {
        }

        public void onTimeShiftStatusChanged(String str, int i) {
        }
    }

    private class MySessionCallback extends TvInputManager.SessionCallback {
        Uri mChannelUri;
        final String mInputId;
        Uri mRecordedProgramUri;
        Bundle mTuneParams;

        MySessionCallback(String str, Uri uri, Bundle bundle) {
            this.mInputId = str;
            this.mChannelUri = uri;
            this.mTuneParams = bundle;
        }

        MySessionCallback(String str, Uri uri) {
            this.mInputId = str;
            this.mRecordedProgramUri = uri;
        }

        @Override
        public void onSessionCreated(TvInputManager.Session session) {
            if (this == TvView.this.mSessionCallback) {
                TvView.this.mSession = session;
                if (session != null) {
                    for (Pair pair : TvView.this.mPendingAppPrivateCommands) {
                        TvView.this.mSession.sendAppPrivateCommand((String) pair.first, (Bundle) pair.second);
                    }
                    TvView.this.mPendingAppPrivateCommands.clear();
                    synchronized (TvView.sMainTvViewLock) {
                        if (TvView.this.hasWindowFocus() && TvView.this == TvView.sMainTvView.get() && TvView.this.checkChangeHdmiCecActiveSourcePermission()) {
                            TvView.this.mSession.setMain();
                        }
                    }
                    if (TvView.this.mSurface != null) {
                        TvView.this.setSessionSurface(TvView.this.mSurface);
                        if (TvView.this.mSurfaceChanged) {
                            TvView.this.dispatchSurfaceChanged(TvView.this.mSurfaceFormat, TvView.this.mSurfaceWidth, TvView.this.mSurfaceHeight);
                        }
                    }
                    TvView.this.createSessionOverlayView();
                    if (TvView.this.mStreamVolume != null) {
                        TvView.this.mSession.setStreamVolume(TvView.this.mStreamVolume.floatValue());
                    }
                    if (TvView.this.mCaptionEnabled != null) {
                        TvView.this.mSession.setCaptionEnabled(TvView.this.mCaptionEnabled.booleanValue());
                    }
                    if (this.mChannelUri != null) {
                        TvView.this.mSession.tune(this.mChannelUri, this.mTuneParams);
                    } else {
                        TvView.this.mSession.timeShiftPlay(this.mRecordedProgramUri);
                    }
                    TvView.this.ensurePositionTracking();
                    return;
                }
                TvView.this.mSessionCallback = null;
                if (TvView.this.mCallback != null) {
                    TvView.this.mCallback.onConnectionFailed(this.mInputId);
                    return;
                }
                return;
            }
            Log.w(TvView.TAG, "onSessionCreated - session already created");
            if (session != null) {
                session.release();
            }
        }

        @Override
        public void onSessionReleased(TvInputManager.Session session) {
            if (this == TvView.this.mSessionCallback) {
                TvView.this.mOverlayViewCreated = false;
                TvView.this.mOverlayViewFrame = null;
                TvView.this.mSessionCallback = null;
                TvView.this.mSession = null;
                if (TvView.this.mCallback != null) {
                    TvView.this.mCallback.onDisconnected(this.mInputId);
                    return;
                }
                return;
            }
            Log.w(TvView.TAG, "onSessionReleased - session not created");
        }

        @Override
        public void onChannelRetuned(TvInputManager.Session session, Uri uri) {
            if (this == TvView.this.mSessionCallback) {
                if (TvView.this.mCallback != null) {
                    TvView.this.mCallback.onChannelRetuned(this.mInputId, uri);
                    return;
                }
                return;
            }
            Log.w(TvView.TAG, "onChannelRetuned - session not created");
        }

        @Override
        public void onTracksChanged(TvInputManager.Session session, List<TvTrackInfo> list) {
            if (this == TvView.this.mSessionCallback) {
                if (TvView.this.mCallback != null) {
                    TvView.this.mCallback.onTracksChanged(this.mInputId, list);
                    return;
                }
                return;
            }
            Log.w(TvView.TAG, "onTracksChanged - session not created");
        }

        @Override
        public void onTrackSelected(TvInputManager.Session session, int i, String str) {
            if (this == TvView.this.mSessionCallback) {
                if (TvView.this.mCallback != null) {
                    TvView.this.mCallback.onTrackSelected(this.mInputId, i, str);
                    return;
                }
                return;
            }
            Log.w(TvView.TAG, "onTrackSelected - session not created");
        }

        @Override
        public void onVideoSizeChanged(TvInputManager.Session session, int i, int i2) {
            if (this == TvView.this.mSessionCallback) {
                if (TvView.this.mCallback != null) {
                    TvView.this.mCallback.onVideoSizeChanged(this.mInputId, i, i2);
                    return;
                }
                return;
            }
            Log.w(TvView.TAG, "onVideoSizeChanged - session not created");
        }

        @Override
        public void onVideoAvailable(TvInputManager.Session session) {
            if (this == TvView.this.mSessionCallback) {
                if (TvView.this.mCallback != null) {
                    TvView.this.mCallback.onVideoAvailable(this.mInputId);
                    return;
                }
                return;
            }
            Log.w(TvView.TAG, "onVideoAvailable - session not created");
        }

        @Override
        public void onVideoUnavailable(TvInputManager.Session session, int i) {
            if (this == TvView.this.mSessionCallback) {
                if (TvView.this.mCallback != null) {
                    TvView.this.mCallback.onVideoUnavailable(this.mInputId, i);
                    return;
                }
                return;
            }
            Log.w(TvView.TAG, "onVideoUnavailable - session not created");
        }

        @Override
        public void onContentAllowed(TvInputManager.Session session) {
            if (this == TvView.this.mSessionCallback) {
                if (TvView.this.mCallback != null) {
                    TvView.this.mCallback.onContentAllowed(this.mInputId);
                    return;
                }
                return;
            }
            Log.w(TvView.TAG, "onContentAllowed - session not created");
        }

        @Override
        public void onContentBlocked(TvInputManager.Session session, TvContentRating tvContentRating) {
            if (this == TvView.this.mSessionCallback) {
                if (TvView.this.mCallback != null) {
                    TvView.this.mCallback.onContentBlocked(this.mInputId, tvContentRating);
                    return;
                }
                return;
            }
            Log.w(TvView.TAG, "onContentBlocked - session not created");
        }

        @Override
        public void onLayoutSurface(TvInputManager.Session session, int i, int i2, int i3, int i4) {
            if (this == TvView.this.mSessionCallback) {
                TvView.this.mSurfaceViewLeft = i;
                TvView.this.mSurfaceViewTop = i2;
                TvView.this.mSurfaceViewRight = i3;
                TvView.this.mSurfaceViewBottom = i4;
                TvView.this.mUseRequestedSurfaceLayout = true;
                TvView.this.requestLayout();
                return;
            }
            Log.w(TvView.TAG, "onLayoutSurface - session not created");
        }

        @Override
        public void onSessionEvent(TvInputManager.Session session, String str, Bundle bundle) {
            if (this == TvView.this.mSessionCallback) {
                if (TvView.this.mCallback != null) {
                    TvView.this.mCallback.onEvent(this.mInputId, str, bundle);
                    return;
                }
                return;
            }
            Log.w(TvView.TAG, "onSessionEvent - session not created");
        }

        @Override
        public void onTimeShiftStatusChanged(TvInputManager.Session session, int i) {
            if (this == TvView.this.mSessionCallback) {
                if (TvView.this.mCallback != null) {
                    TvView.this.mCallback.onTimeShiftStatusChanged(this.mInputId, i);
                    return;
                }
                return;
            }
            Log.w(TvView.TAG, "onTimeShiftStatusChanged - session not created");
        }

        @Override
        public void onTimeShiftStartPositionChanged(TvInputManager.Session session, long j) {
            if (this == TvView.this.mSessionCallback) {
                if (TvView.this.mTimeShiftPositionCallback != null) {
                    TvView.this.mTimeShiftPositionCallback.onTimeShiftStartPositionChanged(this.mInputId, j);
                    return;
                }
                return;
            }
            Log.w(TvView.TAG, "onTimeShiftStartPositionChanged - session not created");
        }

        @Override
        public void onTimeShiftCurrentPositionChanged(TvInputManager.Session session, long j) {
            if (this == TvView.this.mSessionCallback) {
                if (TvView.this.mTimeShiftPositionCallback != null) {
                    TvView.this.mTimeShiftPositionCallback.onTimeShiftCurrentPositionChanged(this.mInputId, j);
                    return;
                }
                return;
            }
            Log.w(TvView.TAG, "onTimeShiftCurrentPositionChanged - session not created");
        }
    }
}
