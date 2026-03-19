package android.media.tv;

import android.content.Context;
import android.graphics.Rect;
import android.media.PlaybackParams;
import android.media.tv.ITvInputSession;
import android.media.tv.TvInputService;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.TimeUtils;
import android.util.TimedRemoteCaller;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.Surface;
import com.android.internal.os.HandlerCaller;
import com.android.internal.os.SomeArgs;

public class ITvInputSessionWrapper extends ITvInputSession.Stub implements HandlerCaller.Callback {
    private static final int DO_APP_PRIVATE_COMMAND = 9;
    private static final int DO_CREATE_OVERLAY_VIEW = 10;
    private static final int DO_DISPATCH_SURFACE_CHANGED = 4;
    private static final int DO_RELAYOUT_OVERLAY_VIEW = 11;
    private static final int DO_RELEASE = 1;
    private static final int DO_REMOVE_OVERLAY_VIEW = 12;
    private static final int DO_SELECT_TRACK = 8;
    private static final int DO_SET_CAPTION_ENABLED = 7;
    private static final int DO_SET_MAIN = 2;
    private static final int DO_SET_STREAM_VOLUME = 5;
    private static final int DO_SET_SURFACE = 3;
    private static final int DO_START_RECORDING = 20;
    private static final int DO_STOP_RECORDING = 21;
    private static final int DO_TIME_SHIFT_ENABLE_POSITION_TRACKING = 19;
    private static final int DO_TIME_SHIFT_PAUSE = 15;
    private static final int DO_TIME_SHIFT_PLAY = 14;
    private static final int DO_TIME_SHIFT_RESUME = 16;
    private static final int DO_TIME_SHIFT_SEEK_TO = 17;
    private static final int DO_TIME_SHIFT_SET_PLAYBACK_PARAMS = 18;
    private static final int DO_TUNE = 6;
    private static final int DO_UNBLOCK_CONTENT = 13;
    private static final int EXECUTE_MESSAGE_TIMEOUT_LONG_MILLIS = 5000;
    private static final int EXECUTE_MESSAGE_TIMEOUT_SHORT_MILLIS = 50;
    private static final int EXECUTE_MESSAGE_TUNE_TIMEOUT_MILLIS = 2000;
    private static final String TAG = "TvInputSessionWrapper";
    private final HandlerCaller mCaller;
    private InputChannel mChannel;
    private final boolean mIsRecordingSession = true;
    private TvInputEventReceiver mReceiver;
    private TvInputService.RecordingSession mTvInputRecordingSessionImpl;
    private TvInputService.Session mTvInputSessionImpl;

    public ITvInputSessionWrapper(Context context, TvInputService.Session session, InputChannel inputChannel) {
        this.mCaller = new HandlerCaller(context, null, this, true);
        this.mTvInputSessionImpl = session;
        this.mChannel = inputChannel;
        if (inputChannel != null) {
            this.mReceiver = new TvInputEventReceiver(inputChannel, context.getMainLooper());
        }
    }

    public ITvInputSessionWrapper(Context context, TvInputService.RecordingSession recordingSession) {
        this.mCaller = new HandlerCaller(context, null, this, true);
        this.mTvInputRecordingSessionImpl = recordingSession;
    }

    @Override
    public void executeMessage(Message message) {
        if (this.mIsRecordingSession && this.mTvInputRecordingSessionImpl == null) {
            return;
        }
        if (!this.mIsRecordingSession && this.mTvInputSessionImpl == null) {
            return;
        }
        long jNanoTime = System.nanoTime();
        switch (message.what) {
            case 1:
                if (this.mIsRecordingSession) {
                    this.mTvInputRecordingSessionImpl.release();
                    this.mTvInputRecordingSessionImpl = null;
                } else {
                    this.mTvInputSessionImpl.release();
                    this.mTvInputSessionImpl = null;
                    if (this.mReceiver != null) {
                        this.mReceiver.dispose();
                        this.mReceiver = null;
                    }
                    if (this.mChannel != null) {
                        this.mChannel.dispose();
                        this.mChannel = null;
                    }
                }
                break;
            case 2:
                this.mTvInputSessionImpl.setMain(((Boolean) message.obj).booleanValue());
                break;
            case 3:
                this.mTvInputSessionImpl.setSurface((Surface) message.obj);
                break;
            case 4:
                SomeArgs someArgs = (SomeArgs) message.obj;
                this.mTvInputSessionImpl.dispatchSurfaceChanged(someArgs.argi1, someArgs.argi2, someArgs.argi3);
                someArgs.recycle();
                break;
            case 5:
                this.mTvInputSessionImpl.setStreamVolume(((Float) message.obj).floatValue());
                break;
            case 6:
                SomeArgs someArgs2 = (SomeArgs) message.obj;
                if (this.mIsRecordingSession) {
                    this.mTvInputRecordingSessionImpl.tune((Uri) someArgs2.arg1, (Bundle) someArgs2.arg2);
                } else {
                    this.mTvInputSessionImpl.tune((Uri) someArgs2.arg1, (Bundle) someArgs2.arg2);
                }
                someArgs2.recycle();
                break;
            case 7:
                this.mTvInputSessionImpl.setCaptionEnabled(((Boolean) message.obj).booleanValue());
                break;
            case 8:
                SomeArgs someArgs3 = (SomeArgs) message.obj;
                this.mTvInputSessionImpl.selectTrack(((Integer) someArgs3.arg1).intValue(), (String) someArgs3.arg2);
                someArgs3.recycle();
                break;
            case 9:
                SomeArgs someArgs4 = (SomeArgs) message.obj;
                if (this.mIsRecordingSession) {
                    this.mTvInputRecordingSessionImpl.appPrivateCommand((String) someArgs4.arg1, (Bundle) someArgs4.arg2);
                } else {
                    this.mTvInputSessionImpl.appPrivateCommand((String) someArgs4.arg1, (Bundle) someArgs4.arg2);
                }
                someArgs4.recycle();
                break;
            case 10:
                SomeArgs someArgs5 = (SomeArgs) message.obj;
                this.mTvInputSessionImpl.createOverlayView((IBinder) someArgs5.arg1, (Rect) someArgs5.arg2);
                someArgs5.recycle();
                break;
            case 11:
                this.mTvInputSessionImpl.relayoutOverlayView((Rect) message.obj);
                break;
            case 12:
                this.mTvInputSessionImpl.removeOverlayView(true);
                break;
            case 13:
                this.mTvInputSessionImpl.unblockContent((String) message.obj);
                break;
            case 14:
                this.mTvInputSessionImpl.timeShiftPlay((Uri) message.obj);
                break;
            case 15:
                this.mTvInputSessionImpl.timeShiftPause();
                break;
            case 16:
                this.mTvInputSessionImpl.timeShiftResume();
                break;
            case 17:
                this.mTvInputSessionImpl.timeShiftSeekTo(((Long) message.obj).longValue());
                break;
            case 18:
                this.mTvInputSessionImpl.timeShiftSetPlaybackParams((PlaybackParams) message.obj);
                break;
            case 19:
                this.mTvInputSessionImpl.timeShiftEnablePositionTracking(((Boolean) message.obj).booleanValue());
                break;
            case 20:
                this.mTvInputRecordingSessionImpl.startRecording((Uri) message.obj);
                break;
            case 21:
                this.mTvInputRecordingSessionImpl.stopRecording();
                break;
            default:
                Log.w(TAG, "Unhandled message code: " + message.what);
                break;
        }
        long jNanoTime2 = (System.nanoTime() - jNanoTime) / TimeUtils.NANOS_PER_MS;
        if (jNanoTime2 > 50) {
            Log.w(TAG, "Handling message (" + message.what + ") took too long time (duration=" + jNanoTime2 + "ms)");
            if (message.what == 6 && jNanoTime2 > 2000) {
                throw new RuntimeException("Too much time to handle tune request. (" + jNanoTime2 + "ms > 2000ms) Consider handling the tune request in a separate thread.");
            }
            if (jNanoTime2 > TimedRemoteCaller.DEFAULT_CALL_TIMEOUT_MILLIS) {
                throw new RuntimeException("Too much time to handle a request. (type=" + message.what + ", " + jNanoTime2 + "ms > 5000ms).");
            }
        }
    }

    @Override
    public void release() {
        if (!this.mIsRecordingSession) {
            this.mTvInputSessionImpl.scheduleOverlayViewCleanup();
        }
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessage(1));
    }

    @Override
    public void setMain(boolean z) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageO(2, Boolean.valueOf(z)));
    }

    @Override
    public void setSurface(Surface surface) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageO(3, surface));
    }

    @Override
    public void dispatchSurfaceChanged(int i, int i2, int i3) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageIIII(4, i, i2, i3, 0));
    }

    @Override
    public final void setVolume(float f) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageO(5, Float.valueOf(f)));
    }

    @Override
    public void tune(Uri uri, Bundle bundle) {
        this.mCaller.removeMessages(6);
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageOO(6, uri, bundle));
    }

    @Override
    public void setCaptionEnabled(boolean z) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageO(7, Boolean.valueOf(z)));
    }

    @Override
    public void selectTrack(int i, String str) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageOO(8, Integer.valueOf(i), str));
    }

    @Override
    public void appPrivateCommand(String str, Bundle bundle) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageOO(9, str, bundle));
    }

    @Override
    public void createOverlayView(IBinder iBinder, Rect rect) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageOO(10, iBinder, rect));
    }

    @Override
    public void relayoutOverlayView(Rect rect) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageO(11, rect));
    }

    @Override
    public void removeOverlayView() {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessage(12));
    }

    @Override
    public void unblockContent(String str) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageO(13, str));
    }

    @Override
    public void timeShiftPlay(Uri uri) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageO(14, uri));
    }

    @Override
    public void timeShiftPause() {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessage(15));
    }

    @Override
    public void timeShiftResume() {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessage(16));
    }

    @Override
    public void timeShiftSeekTo(long j) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageO(17, Long.valueOf(j)));
    }

    @Override
    public void timeShiftSetPlaybackParams(PlaybackParams playbackParams) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageO(18, playbackParams));
    }

    @Override
    public void timeShiftEnablePositionTracking(boolean z) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageO(19, Boolean.valueOf(z)));
    }

    @Override
    public void startRecording(Uri uri) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageO(20, uri));
    }

    @Override
    public void stopRecording() {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessage(21));
    }

    private final class TvInputEventReceiver extends InputEventReceiver {
        public TvInputEventReceiver(InputChannel inputChannel, Looper looper) {
            super(inputChannel, looper);
        }

        @Override
        public void onInputEvent(InputEvent inputEvent, int i) {
            if (ITvInputSessionWrapper.this.mTvInputSessionImpl != null) {
                int iDispatchInputEvent = ITvInputSessionWrapper.this.mTvInputSessionImpl.dispatchInputEvent(inputEvent, this);
                if (iDispatchInputEvent != -1) {
                    finishInputEvent(inputEvent, iDispatchInputEvent == 1);
                    return;
                }
                return;
            }
            finishInputEvent(inputEvent, false);
        }
    }
}
