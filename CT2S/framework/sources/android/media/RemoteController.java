package android.media;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.IRemoteControlDisplay;
import android.media.session.MediaController;
import android.media.session.MediaSessionLegacyHelper;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.net.ProxyInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import java.lang.ref.WeakReference;
import java.util.List;

@Deprecated
public final class RemoteController {
    private static final boolean DEBUG = false;
    private static final int MAX_BITMAP_DIMENSION = 512;
    private static final int MSG_CLIENT_CHANGE = 4;
    private static final int MSG_DISPLAY_ENABLE = 5;
    private static final int MSG_NEW_MEDIA_METADATA = 7;
    private static final int MSG_NEW_METADATA = 3;
    private static final int MSG_NEW_PENDING_INTENT = 0;
    private static final int MSG_NEW_PLAYBACK_INFO = 1;
    private static final int MSG_NEW_PLAYBACK_STATE = 6;
    private static final int MSG_NEW_TRANSPORT_INFO = 2;
    public static final int POSITION_SYNCHRONIZATION_CHECK = 1;
    public static final int POSITION_SYNCHRONIZATION_NONE = 0;
    private static final int SENDMSG_NOOP = 1;
    private static final int SENDMSG_QUEUE = 2;
    private static final int SENDMSG_REPLACE = 0;
    private static final String TAG = "RemoteController";
    private static final int TRANSPORT_UNKNOWN = 0;
    private static final boolean USE_SESSIONS = true;
    private static final Object mGenLock = new Object();
    private static final Object mInfoLock = new Object();
    private int mArtworkHeight;
    private int mArtworkWidth;
    private final AudioManager mAudioManager;
    private int mClientGenerationIdCurrent;
    private PendingIntent mClientPendingIntentCurrent;
    private final Context mContext;
    private MediaController mCurrentSession;
    private boolean mEnabled;
    private final EventHandler mEventHandler;
    private boolean mIsRegistered;
    private PlaybackInfo mLastPlaybackInfo;
    private final int mMaxBitmapDimension;
    private MetadataEditor mMetadataEditor;
    private OnClientUpdateListener mOnClientUpdateListener;
    private final RcDisplay mRcd;
    private MediaController.Callback mSessionCb;
    private MediaSessionManager.OnActiveSessionsChangedListener mSessionListener;
    private MediaSessionManager mSessionManager;

    public interface OnClientUpdateListener {
        void onClientChange(boolean z);

        void onClientMetadataUpdate(MetadataEditor metadataEditor);

        void onClientPlaybackStateUpdate(int i);

        void onClientPlaybackStateUpdate(int i, long j, long j2, float f);

        void onClientTransportControlUpdate(int i);
    }

    public RemoteController(Context context, OnClientUpdateListener updateListener) throws IllegalArgumentException {
        this(context, updateListener, null);
    }

    public RemoteController(Context context, OnClientUpdateListener updateListener, Looper looper) throws IllegalArgumentException {
        this.mSessionCb = new MediaControllerCallback();
        this.mClientGenerationIdCurrent = 0;
        this.mIsRegistered = false;
        this.mArtworkWidth = -1;
        this.mArtworkHeight = -1;
        this.mEnabled = true;
        if (context == null) {
            throw new IllegalArgumentException("Invalid null Context");
        }
        if (updateListener == null) {
            throw new IllegalArgumentException("Invalid null OnClientUpdateListener");
        }
        if (looper != null) {
            this.mEventHandler = new EventHandler(this, looper);
        } else {
            Looper l = Looper.myLooper();
            if (l != null) {
                this.mEventHandler = new EventHandler(this, l);
            } else {
                throw new IllegalArgumentException("Calling thread not associated with a looper");
            }
        }
        this.mOnClientUpdateListener = updateListener;
        this.mContext = context;
        this.mRcd = new RcDisplay(this);
        this.mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.mSessionManager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
        this.mSessionListener = new TopTransportSessionListener();
        if (ActivityManager.isLowRamDeviceStatic()) {
            this.mMaxBitmapDimension = 512;
        } else {
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            this.mMaxBitmapDimension = Math.max(dm.widthPixels, dm.heightPixels);
        }
    }

    public String getRemoteControlClientPackageName() {
        String packageName;
        synchronized (mInfoLock) {
            packageName = this.mCurrentSession != null ? this.mCurrentSession.getPackageName() : null;
        }
        return packageName;
    }

    public long getEstimatedMediaPosition() {
        PlaybackState state;
        synchronized (mInfoLock) {
            if (this.mCurrentSession != null && (state = this.mCurrentSession.getPlaybackState()) != null) {
                return state.getPosition();
            }
            return -1L;
        }
    }

    public boolean sendMediaKeyEvent(KeyEvent keyEvent) throws IllegalArgumentException {
        boolean zDispatchMediaButtonEvent;
        if (!KeyEvent.isMediaKey(keyEvent.getKeyCode())) {
            throw new IllegalArgumentException("not a media key event");
        }
        synchronized (mInfoLock) {
            zDispatchMediaButtonEvent = this.mCurrentSession != null ? this.mCurrentSession.dispatchMediaButtonEvent(keyEvent) : false;
        }
        return zDispatchMediaButtonEvent;
    }

    public boolean seekTo(long timeMs) throws IllegalArgumentException {
        if (!this.mEnabled) {
            Log.e(TAG, "Cannot use seekTo() from a disabled RemoteController");
            return false;
        }
        if (timeMs < 0) {
            throw new IllegalArgumentException("illegal negative time value");
        }
        synchronized (mInfoLock) {
            if (this.mCurrentSession != null) {
                this.mCurrentSession.getTransportControls().seekTo(timeMs);
            }
        }
        return true;
    }

    public boolean setArtworkConfiguration(boolean wantBitmap, int width, int height) throws IllegalArgumentException {
        synchronized (mInfoLock) {
            if (wantBitmap) {
                if (width > 0 && height > 0) {
                    if (width > this.mMaxBitmapDimension) {
                        width = this.mMaxBitmapDimension;
                    }
                    if (height > this.mMaxBitmapDimension) {
                        height = this.mMaxBitmapDimension;
                    }
                    this.mArtworkWidth = width;
                    this.mArtworkHeight = height;
                } else {
                    throw new IllegalArgumentException("Invalid dimensions");
                }
            } else {
                this.mArtworkWidth = -1;
                this.mArtworkHeight = -1;
            }
        }
        return true;
    }

    public boolean setArtworkConfiguration(int width, int height) throws IllegalArgumentException {
        return setArtworkConfiguration(true, width, height);
    }

    public boolean clearArtworkConfiguration() {
        return setArtworkConfiguration(false, -1, -1);
    }

    public boolean setSynchronizationMode(int sync) throws IllegalArgumentException {
        if (sync != 0 && sync != 1) {
            throw new IllegalArgumentException("Unknown synchronization mode " + sync);
        }
        if (!this.mIsRegistered) {
            Log.e(TAG, "Cannot set synchronization mode on an unregistered RemoteController");
            return false;
        }
        this.mAudioManager.remoteControlDisplayWantsPlaybackPositionSync(this.mRcd, 1 == sync);
        return true;
    }

    public MetadataEditor editMetadata() {
        MetadataEditor editor = new MetadataEditor();
        editor.mEditorMetadata = new Bundle();
        editor.mEditorArtwork = null;
        editor.mMetadataChanged = true;
        editor.mArtworkChanged = true;
        editor.mEditableKeys = 0L;
        return editor;
    }

    public class MetadataEditor extends MediaMetadataEditor {
        protected MetadataEditor() {
        }

        protected MetadataEditor(Bundle metadata, long editableKeys) {
            this.mEditorMetadata = metadata;
            this.mEditableKeys = editableKeys;
            this.mEditorArtwork = (Bitmap) metadata.getParcelable(String.valueOf(100));
            if (this.mEditorArtwork != null) {
                cleanupBitmapFromBundle(100);
            }
            this.mMetadataChanged = true;
            this.mArtworkChanged = true;
            this.mApplied = false;
        }

        private void cleanupBitmapFromBundle(int key) {
            if (METADATA_KEYS_TYPE.get(key, -1) == 2) {
                this.mEditorMetadata.remove(String.valueOf(key));
            }
        }

        @Override
        public synchronized void apply() {
            Rating rating;
            if (this.mMetadataChanged) {
                synchronized (RemoteController.mInfoLock) {
                    if (RemoteController.this.mCurrentSession != null && this.mEditorMetadata.containsKey(String.valueOf(MediaMetadataEditor.RATING_KEY_BY_USER)) && (rating = (Rating) getObject(MediaMetadataEditor.RATING_KEY_BY_USER, null)) != null) {
                        RemoteController.this.mCurrentSession.getTransportControls().setRating(rating);
                    }
                }
                this.mApplied = false;
            }
        }
    }

    private static class RcDisplay extends IRemoteControlDisplay.Stub {
        private final WeakReference<RemoteController> mController;

        RcDisplay(RemoteController rc) {
            this.mController = new WeakReference<>(rc);
        }

        @Override
        public void setCurrentClientId(int genId, PendingIntent clientMediaIntent, boolean clearing) {
            RemoteController rc = this.mController.get();
            if (rc != null) {
                boolean isNew = false;
                synchronized (RemoteController.mGenLock) {
                    if (rc.mClientGenerationIdCurrent != genId) {
                        rc.mClientGenerationIdCurrent = genId;
                        isNew = true;
                    }
                }
                if (clientMediaIntent != null) {
                    RemoteController.sendMsg(rc.mEventHandler, 0, 0, genId, 0, clientMediaIntent, 0);
                }
                if (isNew || clearing) {
                    RemoteController.sendMsg(rc.mEventHandler, 4, 0, genId, clearing ? 1 : 0, null, 0);
                }
            }
        }

        @Override
        public void setEnabled(boolean enabled) {
            RemoteController rc = this.mController.get();
            if (rc != null) {
                RemoteController.sendMsg(rc.mEventHandler, 5, 0, enabled ? 1 : 0, 0, null, 0);
            }
        }

        @Override
        public void setPlaybackState(int genId, int state, long stateChangeTimeMs, long currentPosMs, float speed) {
            RemoteController rc = this.mController.get();
            if (rc != null) {
                synchronized (RemoteController.mGenLock) {
                    if (rc.mClientGenerationIdCurrent == genId) {
                        PlaybackInfo playbackInfo = new PlaybackInfo(state, stateChangeTimeMs, currentPosMs, speed);
                        RemoteController.sendMsg(rc.mEventHandler, 1, 0, genId, 0, playbackInfo, 0);
                    }
                }
            }
        }

        @Override
        public void setTransportControlInfo(int genId, int transportControlFlags, int posCapabilities) {
            RemoteController rc = this.mController.get();
            if (rc != null) {
                synchronized (RemoteController.mGenLock) {
                    if (rc.mClientGenerationIdCurrent == genId) {
                        RemoteController.sendMsg(rc.mEventHandler, 2, 0, genId, transportControlFlags, null, 0);
                    }
                }
            }
        }

        @Override
        public void setMetadata(int genId, Bundle metadata) {
            RemoteController rc = this.mController.get();
            if (rc != null && metadata != null) {
                synchronized (RemoteController.mGenLock) {
                    if (rc.mClientGenerationIdCurrent == genId) {
                        RemoteController.sendMsg(rc.mEventHandler, 3, 2, genId, 0, metadata, 0);
                    }
                }
            }
        }

        @Override
        public void setArtwork(int genId, Bitmap artwork) {
            RemoteController rc = this.mController.get();
            if (rc != null) {
                synchronized (RemoteController.mGenLock) {
                    if (rc.mClientGenerationIdCurrent == genId) {
                        Bundle metadata = new Bundle(1);
                        metadata.putParcelable(String.valueOf(100), artwork);
                        RemoteController.sendMsg(rc.mEventHandler, 3, 2, genId, 0, metadata, 0);
                    }
                }
            }
        }

        @Override
        public void setAllMetadata(int genId, Bundle metadata, Bitmap artwork) {
            RemoteController rc = this.mController.get();
            if (rc != null) {
                if (metadata != null || artwork != null) {
                    synchronized (RemoteController.mGenLock) {
                        if (rc.mClientGenerationIdCurrent == genId) {
                            if (metadata == null) {
                                metadata = new Bundle(1);
                            }
                            if (artwork != null) {
                                metadata.putParcelable(String.valueOf(100), artwork);
                            }
                            RemoteController.sendMsg(rc.mEventHandler, 3, 2, genId, 0, metadata, 0);
                        }
                    }
                }
            }
        }
    }

    private class MediaControllerCallback extends MediaController.Callback {
        private MediaControllerCallback() {
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            RemoteController.this.onNewPlaybackState(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            RemoteController.this.onNewMediaMetadata(metadata);
        }
    }

    private class TopTransportSessionListener implements MediaSessionManager.OnActiveSessionsChangedListener {
        private TopTransportSessionListener() {
        }

        @Override
        public void onActiveSessionsChanged(List<MediaController> controllers) {
            int size = controllers.size();
            for (int i = 0; i < size; i++) {
                MediaController controller = controllers.get(i);
                long flags = controller.getFlags();
                if ((2 & flags) != 0) {
                    RemoteController.this.updateController(controller);
                    return;
                }
            }
            RemoteController.this.updateController(null);
        }
    }

    private class EventHandler extends Handler {
        public EventHandler(RemoteController rc, Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    RemoteController.this.onNewPendingIntent(msg.arg1, (PendingIntent) msg.obj);
                    break;
                case 1:
                    RemoteController.this.onNewPlaybackInfo(msg.arg1, (PlaybackInfo) msg.obj);
                    break;
                case 2:
                    RemoteController.this.onNewTransportInfo(msg.arg1, msg.arg2);
                    break;
                case 3:
                    RemoteController.this.onNewMetadata(msg.arg1, (Bundle) msg.obj);
                    break;
                case 4:
                    RemoteController.this.onClientChange(msg.arg1, msg.arg2 == 1);
                    break;
                case 5:
                    RemoteController.this.onDisplayEnable(msg.arg1 == 1);
                    break;
                case 6:
                    RemoteController.this.onNewPlaybackState((PlaybackState) msg.obj);
                    break;
                case 7:
                    RemoteController.this.onNewMediaMetadata((MediaMetadata) msg.obj);
                    break;
                default:
                    Log.e(RemoteController.TAG, "unknown event " + msg.what);
                    break;
            }
        }
    }

    void startListeningToSessions() {
        ComponentName listenerComponent = new ComponentName(this.mContext, this.mOnClientUpdateListener.getClass());
        Handler handler = null;
        if (Looper.myLooper() == null) {
            handler = new Handler(Looper.getMainLooper());
        }
        this.mSessionManager.addOnActiveSessionsChangedListener(this.mSessionListener, listenerComponent, UserHandle.myUserId(), handler);
        this.mSessionListener.onActiveSessionsChanged(this.mSessionManager.getActiveSessions(listenerComponent));
    }

    void stopListeningToSessions() {
        this.mSessionManager.removeOnActiveSessionsChangedListener(this.mSessionListener);
    }

    private static void sendMsg(Handler handler, int msg, int existingMsgPolicy, int arg1, int arg2, Object obj, int delayMs) {
        if (handler == null) {
            Log.e(TAG, "null event handler, will not deliver message " + msg);
            return;
        }
        if (existingMsgPolicy == 0) {
            handler.removeMessages(msg);
        } else if (existingMsgPolicy == 1 && handler.hasMessages(msg)) {
            return;
        }
        handler.sendMessageDelayed(handler.obtainMessage(msg, arg1, arg2, obj), delayMs);
    }

    private void onNewPendingIntent(int genId, PendingIntent pi) {
        synchronized (mGenLock) {
            if (this.mClientGenerationIdCurrent == genId) {
                synchronized (mInfoLock) {
                    this.mClientPendingIntentCurrent = pi;
                }
            }
        }
    }

    private void onNewPlaybackInfo(int genId, PlaybackInfo pi) {
        OnClientUpdateListener l;
        synchronized (mGenLock) {
            if (this.mClientGenerationIdCurrent == genId) {
                synchronized (mInfoLock) {
                    l = this.mOnClientUpdateListener;
                    this.mLastPlaybackInfo = pi;
                }
                if (l != null) {
                    if (pi.mCurrentPosMs == RemoteControlClient.PLAYBACK_POSITION_ALWAYS_UNKNOWN) {
                        l.onClientPlaybackStateUpdate(pi.mState);
                    } else {
                        l.onClientPlaybackStateUpdate(pi.mState, pi.mStateChangeTimeMs, pi.mCurrentPosMs, pi.mSpeed);
                    }
                }
            }
        }
    }

    private void onNewTransportInfo(int genId, int transportControlFlags) {
        OnClientUpdateListener l;
        synchronized (mGenLock) {
            if (this.mClientGenerationIdCurrent == genId) {
                synchronized (mInfoLock) {
                    l = this.mOnClientUpdateListener;
                }
                if (l != null) {
                    l.onClientTransportControlUpdate(transportControlFlags);
                }
            }
        }
    }

    private void onNewMetadata(int genId, Bundle metadata) {
        OnClientUpdateListener l;
        MetadataEditor metadataEditor;
        synchronized (mGenLock) {
            if (this.mClientGenerationIdCurrent == genId) {
                long editableKeys = metadata.getLong(String.valueOf(MediaMetadataEditor.KEY_EDITABLE_MASK), 0L);
                if (editableKeys != 0) {
                    metadata.remove(String.valueOf(MediaMetadataEditor.KEY_EDITABLE_MASK));
                }
                synchronized (mInfoLock) {
                    l = this.mOnClientUpdateListener;
                    if (this.mMetadataEditor != null && this.mMetadataEditor.mEditorMetadata != null) {
                        if (this.mMetadataEditor.mEditorMetadata != metadata) {
                            this.mMetadataEditor.mEditorMetadata.putAll(metadata);
                        }
                        this.mMetadataEditor.putBitmap(100, (Bitmap) metadata.getParcelable(String.valueOf(100)));
                        this.mMetadataEditor.cleanupBitmapFromBundle(100);
                    } else {
                        this.mMetadataEditor = new MetadataEditor(metadata, editableKeys);
                    }
                    metadataEditor = this.mMetadataEditor;
                }
                if (l != null) {
                    l.onClientMetadataUpdate(metadataEditor);
                }
            }
        }
    }

    private void onClientChange(int genId, boolean clearing) {
        OnClientUpdateListener l;
        synchronized (mGenLock) {
            if (this.mClientGenerationIdCurrent == genId) {
                synchronized (mInfoLock) {
                    l = this.mOnClientUpdateListener;
                    this.mMetadataEditor = null;
                }
                if (l != null) {
                    l.onClientChange(clearing);
                }
            }
        }
    }

    private void onDisplayEnable(boolean enabled) {
        int genId;
        synchronized (mInfoLock) {
            this.mEnabled = enabled;
            OnClientUpdateListener onClientUpdateListener = this.mOnClientUpdateListener;
        }
        if (!enabled) {
            synchronized (mGenLock) {
                genId = this.mClientGenerationIdCurrent;
            }
            PlaybackInfo pi = new PlaybackInfo(1, SystemClock.elapsedRealtime(), 0L, 0.0f);
            sendMsg(this.mEventHandler, 1, 0, genId, 0, pi, 0);
            sendMsg(this.mEventHandler, 2, 0, genId, 0, null, 0);
            Bundle metadata = new Bundle(3);
            metadata.putString(String.valueOf(7), ProxyInfo.LOCAL_EXCL_LIST);
            metadata.putString(String.valueOf(2), ProxyInfo.LOCAL_EXCL_LIST);
            metadata.putLong(String.valueOf(9), 0L);
            sendMsg(this.mEventHandler, 3, 2, genId, 0, metadata, 0);
        }
    }

    private void updateController(MediaController controller) {
        synchronized (mInfoLock) {
            if (controller == null) {
                if (this.mCurrentSession != null) {
                    this.mCurrentSession.unregisterCallback(this.mSessionCb);
                    this.mCurrentSession = null;
                    sendMsg(this.mEventHandler, 4, 0, 0, 1, null, 0);
                }
            } else if (this.mCurrentSession == null || !controller.getSessionToken().equals(this.mCurrentSession.getSessionToken())) {
                if (this.mCurrentSession != null) {
                    this.mCurrentSession.unregisterCallback(this.mSessionCb);
                }
                sendMsg(this.mEventHandler, 4, 0, 0, 0, null, 0);
                this.mCurrentSession = controller;
                this.mCurrentSession.registerCallback(this.mSessionCb, this.mEventHandler);
                PlaybackState state = controller.getPlaybackState();
                sendMsg(this.mEventHandler, 6, 0, 0, 0, state, 0);
                MediaMetadata metadata = controller.getMetadata();
                sendMsg(this.mEventHandler, 7, 0, 0, 0, metadata, 0);
            }
        }
    }

    private void onNewPlaybackState(PlaybackState state) {
        OnClientUpdateListener l;
        synchronized (mInfoLock) {
            l = this.mOnClientUpdateListener;
        }
        if (l != null) {
            int playstate = state == null ? 0 : PlaybackState.getRccStateFromState(state.getState());
            if (state == null || state.getPosition() == -1) {
                l.onClientPlaybackStateUpdate(playstate);
            } else {
                l.onClientPlaybackStateUpdate(playstate, state.getLastPositionUpdateTime(), state.getPosition(), state.getPlaybackSpeed());
            }
            if (state != null) {
                l.onClientTransportControlUpdate(PlaybackState.getRccControlFlagsFromActions(state.getActions()));
            }
        }
    }

    private void onNewMediaMetadata(MediaMetadata metadata) {
        OnClientUpdateListener l;
        MetadataEditor metadataEditor;
        if (metadata != null) {
            synchronized (mInfoLock) {
                l = this.mOnClientUpdateListener;
                boolean canRate = (this.mCurrentSession == null || this.mCurrentSession.getRatingType() == 0) ? false : true;
                long editableKeys = canRate ? 268435457L : 0L;
                Bundle legacyMetadata = MediaSessionLegacyHelper.getOldMetadata(metadata, this.mArtworkWidth, this.mArtworkHeight);
                this.mMetadataEditor = new MetadataEditor(legacyMetadata, editableKeys);
                metadataEditor = this.mMetadataEditor;
            }
            if (l != null) {
                l.onClientMetadataUpdate(metadataEditor);
            }
        }
    }

    private static class PlaybackInfo {
        long mCurrentPosMs;
        float mSpeed;
        int mState;
        long mStateChangeTimeMs;

        PlaybackInfo(int state, long stateChangeTimeMs, long currentPosMs, float speed) {
            this.mState = state;
            this.mStateChangeTimeMs = stateChangeTimeMs;
            this.mCurrentPosMs = currentPosMs;
            this.mSpeed = speed;
        }
    }

    void setIsRegistered(boolean registered) {
        synchronized (mInfoLock) {
            this.mIsRegistered = registered;
        }
    }

    RcDisplay getRcDisplay() {
        return this.mRcd;
    }

    int[] getArtworkSize() {
        int[] size;
        synchronized (mInfoLock) {
            size = new int[]{this.mArtworkWidth, this.mArtworkHeight};
        }
        return size;
    }

    OnClientUpdateListener getUpdateListener() {
        return this.mOnClientUpdateListener;
    }
}
