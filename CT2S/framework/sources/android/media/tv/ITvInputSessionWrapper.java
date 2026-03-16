package android.media.tv;

import android.content.Context;
import android.graphics.Rect;
import android.media.tv.ITvInputSession;
import android.media.tv.TvInputService;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
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
    private static final int DO_REQUEST_UNBLOCK_CONTENT = 13;
    private static final int DO_SELECT_TRACK = 8;
    private static final int DO_SET_CAPTION_ENABLED = 7;
    private static final int DO_SET_MAIN = 2;
    private static final int DO_SET_STREAM_VOLUME = 5;
    private static final int DO_SET_SURFACE = 3;
    private static final int DO_TUNE = 6;
    private static final int MESSAGE_HANDLING_DURATION_THRESHOLD_MILLIS = 50;
    private static final int MESSAGE_TUNE_DURATION_THRESHOLD_MILLIS = 2000;
    private static final String TAG = "TvInputSessionWrapper";
    private final HandlerCaller mCaller;
    private InputChannel mChannel;
    private TvInputEventReceiver mReceiver;
    private TvInputService.Session mTvInputSessionImpl;

    public ITvInputSessionWrapper(Context context, TvInputService.Session sessionImpl, InputChannel channel) {
        this.mCaller = new HandlerCaller(context, null, this, true);
        this.mTvInputSessionImpl = sessionImpl;
        this.mChannel = channel;
        if (channel != null) {
            this.mReceiver = new TvInputEventReceiver(channel, context.getMainLooper());
        }
    }

    @Override
    public void executeMessage(Message msg) {
        if (this.mTvInputSessionImpl != null) {
            long startTime = System.currentTimeMillis();
            switch (msg.what) {
                case 1:
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
                    break;
                case 2:
                    this.mTvInputSessionImpl.setMain(((Boolean) msg.obj).booleanValue());
                    break;
                case 3:
                    this.mTvInputSessionImpl.setSurface((Surface) msg.obj);
                    break;
                case 4:
                    SomeArgs args = (SomeArgs) msg.obj;
                    this.mTvInputSessionImpl.dispatchSurfaceChanged(args.argi1, args.argi2, args.argi3);
                    args.recycle();
                    break;
                case 5:
                    this.mTvInputSessionImpl.setStreamVolume(((Float) msg.obj).floatValue());
                    break;
                case 6:
                    SomeArgs args2 = (SomeArgs) msg.obj;
                    this.mTvInputSessionImpl.tune((Uri) args2.arg1, (Bundle) args2.arg2);
                    args2.recycle();
                    break;
                case 7:
                    this.mTvInputSessionImpl.setCaptionEnabled(((Boolean) msg.obj).booleanValue());
                    break;
                case 8:
                    SomeArgs args3 = (SomeArgs) msg.obj;
                    this.mTvInputSessionImpl.selectTrack(((Integer) args3.arg1).intValue(), (String) args3.arg2);
                    args3.recycle();
                    break;
                case 9:
                    SomeArgs args4 = (SomeArgs) msg.obj;
                    this.mTvInputSessionImpl.appPrivateCommand((String) args4.arg1, (Bundle) args4.arg2);
                    args4.recycle();
                    break;
                case 10:
                    SomeArgs args5 = (SomeArgs) msg.obj;
                    this.mTvInputSessionImpl.createOverlayView((IBinder) args5.arg1, (Rect) args5.arg2);
                    args5.recycle();
                    break;
                case 11:
                    this.mTvInputSessionImpl.relayoutOverlayView((Rect) msg.obj);
                    break;
                case 12:
                    this.mTvInputSessionImpl.removeOverlayView(true);
                    break;
                case 13:
                    this.mTvInputSessionImpl.unblockContent((String) msg.obj);
                    break;
                default:
                    Log.w(TAG, "Unhandled message code: " + msg.what);
                    break;
            }
            long duration = System.currentTimeMillis() - startTime;
            if (duration > 50) {
                Log.w(TAG, "Handling message (" + msg.what + ") took too long time (duration=" + duration + "ms)");
                if (msg.what == 6 && duration > 2000) {
                    throw new RuntimeException("Too much time to handle tune request. (" + duration + "ms > 2000ms) Consider handling the tune request in a separate thread.");
                }
            }
        }
    }

    @Override
    public void release() {
        this.mTvInputSessionImpl.scheduleOverlayViewCleanup();
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessage(1));
    }

    @Override
    public void setMain(boolean isMain) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageO(2, Boolean.valueOf(isMain)));
    }

    @Override
    public void setSurface(Surface surface) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageO(3, surface));
    }

    @Override
    public void dispatchSurfaceChanged(int format, int width, int height) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageIIII(4, format, width, height, 0));
    }

    @Override
    public final void setVolume(float volume) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageO(5, Float.valueOf(volume)));
    }

    @Override
    public void tune(Uri channelUri, Bundle params) {
        this.mCaller.removeMessages(6);
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageOO(6, channelUri, params));
    }

    @Override
    public void setCaptionEnabled(boolean enabled) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageO(7, Boolean.valueOf(enabled)));
    }

    @Override
    public void selectTrack(int type, String trackId) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageOO(8, Integer.valueOf(type), trackId));
    }

    @Override
    public void appPrivateCommand(String action, Bundle data) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageOO(9, action, data));
    }

    @Override
    public void createOverlayView(IBinder windowToken, Rect frame) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageOO(10, windowToken, frame));
    }

    @Override
    public void relayoutOverlayView(Rect frame) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageO(11, frame));
    }

    @Override
    public void removeOverlayView() {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessage(12));
    }

    @Override
    public void requestUnblockContent(String unblockedRating) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageO(13, unblockedRating));
    }

    private final class TvInputEventReceiver extends InputEventReceiver {
        public TvInputEventReceiver(InputChannel inputChannel, Looper looper) {
            super(inputChannel, looper);
        }

        @Override
        public void onInputEvent(InputEvent event) {
            if (ITvInputSessionWrapper.this.mTvInputSessionImpl != null) {
                int handled = ITvInputSessionWrapper.this.mTvInputSessionImpl.dispatchInputEvent(event, this);
                if (handled != -1) {
                    finishInputEvent(event, handled == 1);
                    return;
                }
                return;
            }
            finishInputEvent(event, false);
        }
    }
}
