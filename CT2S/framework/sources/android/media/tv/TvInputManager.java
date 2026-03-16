package android.media.tv;

import android.graphics.Rect;
import android.media.tv.ITvInputClient;
import android.media.tv.ITvInputHardwareCallback;
import android.media.tv.ITvInputManagerCallback;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pools;
import android.util.SparseArray;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventSender;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class TvInputManager {
    public static final String ACTION_BLOCKED_RATINGS_CHANGED = "android.media.tv.action.BLOCKED_RATINGS_CHANGED";
    public static final String ACTION_PARENTAL_CONTROLS_ENABLED_CHANGED = "android.media.tv.action.PARENTAL_CONTROLS_ENABLED_CHANGED";
    public static final String ACTION_QUERY_CONTENT_RATING_SYSTEMS = "android.media.tv.action.QUERY_CONTENT_RATING_SYSTEMS";
    public static final int INPUT_STATE_CONNECTED = 0;
    public static final int INPUT_STATE_CONNECTED_STANDBY = 1;
    public static final int INPUT_STATE_DISCONNECTED = 2;
    public static final int INPUT_STATE_UNKNOWN = -1;
    public static final String META_DATA_CONTENT_RATING_SYSTEMS = "android.media.tv.metadata.CONTENT_RATING_SYSTEMS";
    private static final String TAG = "TvInputManager";
    public static final int VIDEO_UNAVAILABLE_REASON_BUFFERING = 3;
    static final int VIDEO_UNAVAILABLE_REASON_END = 3;
    static final int VIDEO_UNAVAILABLE_REASON_START = 0;
    public static final int VIDEO_UNAVAILABLE_REASON_TUNING = 1;
    public static final int VIDEO_UNAVAILABLE_REASON_UNKNOWN = 0;
    public static final int VIDEO_UNAVAILABLE_REASON_WEAK_SIGNAL = 2;
    private int mNextSeq;
    private final ITvInputManager mService;
    private final int mUserId;
    private final Object mLock = new Object();
    private final List<TvInputCallbackRecord> mCallbackRecords = new LinkedList();
    private final Map<String, Integer> mStateMap = new ArrayMap();
    private final SparseArray<SessionCallbackRecord> mSessionCallbackRecordMap = new SparseArray<>();
    private final ITvInputClient mClient = new ITvInputClient.Stub() {
        @Override
        public void onSessionCreated(String inputId, IBinder token, InputChannel channel, int seq) {
            synchronized (TvInputManager.this.mSessionCallbackRecordMap) {
                SessionCallbackRecord record = (SessionCallbackRecord) TvInputManager.this.mSessionCallbackRecordMap.get(seq);
                if (record == null) {
                    Log.e(TvInputManager.TAG, "Callback not found for " + token);
                    return;
                }
                Session session = null;
                if (token != null) {
                    session = new Session(token, channel, TvInputManager.this.mService, TvInputManager.this.mUserId, seq, TvInputManager.this.mSessionCallbackRecordMap);
                }
                record.postSessionCreated(session);
            }
        }

        @Override
        public void onSessionReleased(int seq) {
            synchronized (TvInputManager.this.mSessionCallbackRecordMap) {
                SessionCallbackRecord record = (SessionCallbackRecord) TvInputManager.this.mSessionCallbackRecordMap.get(seq);
                TvInputManager.this.mSessionCallbackRecordMap.delete(seq);
                if (record == null) {
                    Log.e(TvInputManager.TAG, "Callback not found for seq:" + seq);
                } else {
                    record.mSession.releaseInternal();
                    record.postSessionReleased();
                }
            }
        }

        @Override
        public void onChannelRetuned(Uri channelUri, int seq) {
            synchronized (TvInputManager.this.mSessionCallbackRecordMap) {
                SessionCallbackRecord record = (SessionCallbackRecord) TvInputManager.this.mSessionCallbackRecordMap.get(seq);
                if (record == null) {
                    Log.e(TvInputManager.TAG, "Callback not found for seq " + seq);
                } else {
                    record.postChannelRetuned(channelUri);
                }
            }
        }

        @Override
        public void onTracksChanged(List<TvTrackInfo> tracks, int seq) {
            synchronized (TvInputManager.this.mSessionCallbackRecordMap) {
                SessionCallbackRecord record = (SessionCallbackRecord) TvInputManager.this.mSessionCallbackRecordMap.get(seq);
                if (record != null) {
                    if (record.mSession.updateTracks(tracks)) {
                        record.postTracksChanged(tracks);
                        postVideoSizeChangedIfNeededLocked(record);
                    }
                    return;
                }
                Log.e(TvInputManager.TAG, "Callback not found for seq " + seq);
            }
        }

        @Override
        public void onTrackSelected(int type, String trackId, int seq) {
            synchronized (TvInputManager.this.mSessionCallbackRecordMap) {
                SessionCallbackRecord record = (SessionCallbackRecord) TvInputManager.this.mSessionCallbackRecordMap.get(seq);
                if (record != null) {
                    if (record.mSession.updateTrackSelection(type, trackId)) {
                        record.postTrackSelected(type, trackId);
                        postVideoSizeChangedIfNeededLocked(record);
                    }
                    return;
                }
                Log.e(TvInputManager.TAG, "Callback not found for seq " + seq);
            }
        }

        private void postVideoSizeChangedIfNeededLocked(SessionCallbackRecord record) {
            TvTrackInfo track = record.mSession.getVideoTrackToNotify();
            if (track != null) {
                record.postVideoSizeChanged(track.getVideoWidth(), track.getVideoHeight());
            }
        }

        @Override
        public void onVideoAvailable(int seq) {
            synchronized (TvInputManager.this.mSessionCallbackRecordMap) {
                SessionCallbackRecord record = (SessionCallbackRecord) TvInputManager.this.mSessionCallbackRecordMap.get(seq);
                if (record == null) {
                    Log.e(TvInputManager.TAG, "Callback not found for seq " + seq);
                } else {
                    record.postVideoAvailable();
                }
            }
        }

        @Override
        public void onVideoUnavailable(int reason, int seq) {
            synchronized (TvInputManager.this.mSessionCallbackRecordMap) {
                SessionCallbackRecord record = (SessionCallbackRecord) TvInputManager.this.mSessionCallbackRecordMap.get(seq);
                if (record == null) {
                    Log.e(TvInputManager.TAG, "Callback not found for seq " + seq);
                } else {
                    record.postVideoUnavailable(reason);
                }
            }
        }

        @Override
        public void onContentAllowed(int seq) {
            synchronized (TvInputManager.this.mSessionCallbackRecordMap) {
                SessionCallbackRecord record = (SessionCallbackRecord) TvInputManager.this.mSessionCallbackRecordMap.get(seq);
                if (record == null) {
                    Log.e(TvInputManager.TAG, "Callback not found for seq " + seq);
                } else {
                    record.postContentAllowed();
                }
            }
        }

        @Override
        public void onContentBlocked(String rating, int seq) {
            synchronized (TvInputManager.this.mSessionCallbackRecordMap) {
                SessionCallbackRecord record = (SessionCallbackRecord) TvInputManager.this.mSessionCallbackRecordMap.get(seq);
                if (record == null) {
                    Log.e(TvInputManager.TAG, "Callback not found for seq " + seq);
                } else {
                    record.postContentBlocked(TvContentRating.unflattenFromString(rating));
                }
            }
        }

        @Override
        public void onLayoutSurface(int left, int top, int right, int bottom, int seq) {
            synchronized (TvInputManager.this.mSessionCallbackRecordMap) {
                SessionCallbackRecord record = (SessionCallbackRecord) TvInputManager.this.mSessionCallbackRecordMap.get(seq);
                if (record == null) {
                    Log.e(TvInputManager.TAG, "Callback not found for seq " + seq);
                } else {
                    record.postLayoutSurface(left, top, right, bottom);
                }
            }
        }

        @Override
        public void onSessionEvent(String eventType, Bundle eventArgs, int seq) {
            synchronized (TvInputManager.this.mSessionCallbackRecordMap) {
                SessionCallbackRecord record = (SessionCallbackRecord) TvInputManager.this.mSessionCallbackRecordMap.get(seq);
                if (record == null) {
                    Log.e(TvInputManager.TAG, "Callback not found for seq " + seq);
                } else {
                    record.postSessionEvent(eventType, eventArgs);
                }
            }
        }
    };
    private final ITvInputManagerCallback mManagerCallback = new ITvInputManagerCallback.Stub() {
        @Override
        public void onInputStateChanged(String inputId, int state) {
            synchronized (TvInputManager.this.mLock) {
                TvInputManager.this.mStateMap.put(inputId, Integer.valueOf(state));
                for (TvInputCallbackRecord record : TvInputManager.this.mCallbackRecords) {
                    record.postInputStateChanged(inputId, state);
                }
            }
        }

        @Override
        public void onInputAdded(String inputId) {
            synchronized (TvInputManager.this.mLock) {
                TvInputManager.this.mStateMap.put(inputId, 0);
                for (TvInputCallbackRecord record : TvInputManager.this.mCallbackRecords) {
                    record.postInputAdded(inputId);
                }
            }
        }

        @Override
        public void onInputRemoved(String inputId) {
            synchronized (TvInputManager.this.mLock) {
                TvInputManager.this.mStateMap.remove(inputId);
                for (TvInputCallbackRecord record : TvInputManager.this.mCallbackRecords) {
                    record.postInputRemoved(inputId);
                }
            }
        }

        @Override
        public void onInputUpdated(String inputId) {
            synchronized (TvInputManager.this.mLock) {
                for (TvInputCallbackRecord record : TvInputManager.this.mCallbackRecords) {
                    record.postInputUpdated(inputId);
                }
            }
        }
    };

    public static abstract class HardwareCallback {
        public abstract void onReleased();

        public abstract void onStreamConfigChanged(TvStreamConfig[] tvStreamConfigArr);
    }

    public static abstract class SessionCallback {
        public void onSessionCreated(Session session) {
        }

        public void onSessionReleased(Session session) {
        }

        public void onChannelRetuned(Session session, Uri channelUri) {
        }

        public void onTracksChanged(Session session, List<TvTrackInfo> tracks) {
        }

        public void onTrackSelected(Session session, int type, String trackId) {
        }

        public void onVideoSizeChanged(Session session, int width, int height) {
        }

        public void onVideoAvailable(Session session) {
        }

        public void onVideoUnavailable(Session session, int reason) {
        }

        public void onContentAllowed(Session session) {
        }

        public void onContentBlocked(Session session, TvContentRating rating) {
        }

        public void onLayoutSurface(Session session, int left, int top, int right, int bottom) {
        }

        public void onSessionEvent(Session session, String eventType, Bundle eventArgs) {
        }
    }

    private static final class SessionCallbackRecord {
        private final Handler mHandler;
        private Session mSession;
        private final SessionCallback mSessionCallback;

        SessionCallbackRecord(SessionCallback sessionCallback, Handler handler) {
            this.mSessionCallback = sessionCallback;
            this.mHandler = handler;
        }

        void postSessionCreated(final Session session) {
            this.mSession = session;
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    SessionCallbackRecord.this.mSessionCallback.onSessionCreated(session);
                }
            });
        }

        void postSessionReleased() {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    SessionCallbackRecord.this.mSessionCallback.onSessionReleased(SessionCallbackRecord.this.mSession);
                }
            });
        }

        void postChannelRetuned(final Uri channelUri) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    SessionCallbackRecord.this.mSessionCallback.onChannelRetuned(SessionCallbackRecord.this.mSession, channelUri);
                }
            });
        }

        void postTracksChanged(final List<TvTrackInfo> tracks) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    SessionCallbackRecord.this.mSessionCallback.onTracksChanged(SessionCallbackRecord.this.mSession, tracks);
                }
            });
        }

        void postTrackSelected(final int type, final String trackId) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    SessionCallbackRecord.this.mSessionCallback.onTrackSelected(SessionCallbackRecord.this.mSession, type, trackId);
                }
            });
        }

        void postVideoSizeChanged(final int width, final int height) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    SessionCallbackRecord.this.mSessionCallback.onVideoSizeChanged(SessionCallbackRecord.this.mSession, width, height);
                }
            });
        }

        void postVideoAvailable() {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    SessionCallbackRecord.this.mSessionCallback.onVideoAvailable(SessionCallbackRecord.this.mSession);
                }
            });
        }

        void postVideoUnavailable(final int reason) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    SessionCallbackRecord.this.mSessionCallback.onVideoUnavailable(SessionCallbackRecord.this.mSession, reason);
                }
            });
        }

        void postContentAllowed() {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    SessionCallbackRecord.this.mSessionCallback.onContentAllowed(SessionCallbackRecord.this.mSession);
                }
            });
        }

        void postContentBlocked(final TvContentRating rating) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    SessionCallbackRecord.this.mSessionCallback.onContentBlocked(SessionCallbackRecord.this.mSession, rating);
                }
            });
        }

        void postLayoutSurface(final int left, final int top, final int right, final int bottom) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    SessionCallbackRecord.this.mSessionCallback.onLayoutSurface(SessionCallbackRecord.this.mSession, left, top, right, bottom);
                }
            });
        }

        void postSessionEvent(final String eventType, final Bundle eventArgs) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    SessionCallbackRecord.this.mSessionCallback.onSessionEvent(SessionCallbackRecord.this.mSession, eventType, eventArgs);
                }
            });
        }
    }

    public static abstract class TvInputCallback {
        public void onInputStateChanged(String inputId, int state) {
        }

        public void onInputAdded(String inputId) {
        }

        public void onInputRemoved(String inputId) {
        }

        public void onInputUpdated(String inputId) {
        }
    }

    private static final class TvInputCallbackRecord {
        private final TvInputCallback mCallback;
        private final Handler mHandler;

        public TvInputCallbackRecord(TvInputCallback callback, Handler handler) {
            this.mCallback = callback;
            this.mHandler = handler;
        }

        public TvInputCallback getCallback() {
            return this.mCallback;
        }

        public void postInputStateChanged(final String inputId, final int state) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    TvInputCallbackRecord.this.mCallback.onInputStateChanged(inputId, state);
                }
            });
        }

        public void postInputAdded(final String inputId) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    TvInputCallbackRecord.this.mCallback.onInputAdded(inputId);
                }
            });
        }

        public void postInputRemoved(final String inputId) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    TvInputCallbackRecord.this.mCallback.onInputRemoved(inputId);
                }
            });
        }

        public void postInputUpdated(final String inputId) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    TvInputCallbackRecord.this.mCallback.onInputUpdated(inputId);
                }
            });
        }
    }

    public TvInputManager(ITvInputManager service, int userId) {
        this.mService = service;
        this.mUserId = userId;
        try {
            if (this.mService != null) {
                this.mService.registerCallback(this.mManagerCallback, this.mUserId);
                List<TvInputInfo> infos = this.mService.getTvInputList(this.mUserId);
                synchronized (this.mLock) {
                    for (TvInputInfo info : infos) {
                        String inputId = info.getId();
                        int state = this.mService.getTvInputState(inputId, this.mUserId);
                        if (state != -1) {
                            this.mStateMap.put(inputId, Integer.valueOf(state));
                        }
                    }
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "TvInputManager initialization failed: " + e);
        }
    }

    public List<TvInputInfo> getTvInputList() {
        try {
            return this.mService.getTvInputList(this.mUserId);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public TvInputInfo getTvInputInfo(String inputId) {
        if (inputId == null) {
            throw new IllegalArgumentException("inputId cannot be null");
        }
        try {
            return this.mService.getTvInputInfo(inputId, this.mUserId);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public int getInputState(String inputId) {
        int iIntValue;
        if (inputId == null) {
            throw new IllegalArgumentException("inputId cannot be null");
        }
        synchronized (this.mLock) {
            Integer state = this.mStateMap.get(inputId);
            if (state == null) {
                throw new IllegalArgumentException("Unrecognized input ID: " + inputId);
            }
            iIntValue = state.intValue();
        }
        return iIntValue;
    }

    public void registerCallback(TvInputCallback callback, Handler handler) {
        if (callback == null) {
            throw new IllegalArgumentException("callback cannot be null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler cannot be null");
        }
        synchronized (this.mLock) {
            this.mCallbackRecords.add(new TvInputCallbackRecord(callback, handler));
        }
    }

    public void unregisterCallback(TvInputCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback cannot be null");
        }
        synchronized (this.mLock) {
            Iterator<TvInputCallbackRecord> it = this.mCallbackRecords.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                TvInputCallbackRecord record = it.next();
                if (record.getCallback() == callback) {
                    it.remove();
                    break;
                }
            }
        }
    }

    public boolean isParentalControlsEnabled() {
        try {
            return this.mService.isParentalControlsEnabled(this.mUserId);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void setParentalControlsEnabled(boolean enabled) {
        try {
            this.mService.setParentalControlsEnabled(enabled, this.mUserId);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isRatingBlocked(TvContentRating rating) {
        if (rating == null) {
            throw new IllegalArgumentException("rating cannot be null");
        }
        try {
            return this.mService.isRatingBlocked(rating.flattenToString(), this.mUserId);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public List<TvContentRating> getBlockedRatings() {
        try {
            List<TvContentRating> ratings = new ArrayList<>();
            for (String rating : this.mService.getBlockedRatings(this.mUserId)) {
                ratings.add(TvContentRating.unflattenFromString(rating));
            }
            return ratings;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void addBlockedRating(TvContentRating rating) {
        if (rating == null) {
            throw new IllegalArgumentException("rating cannot be null");
        }
        try {
            this.mService.addBlockedRating(rating.flattenToString(), this.mUserId);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeBlockedRating(TvContentRating rating) {
        if (rating == null) {
            throw new IllegalArgumentException("rating cannot be null");
        }
        try {
            this.mService.removeBlockedRating(rating.flattenToString(), this.mUserId);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public List<TvContentRatingSystemInfo> getTvContentRatingSystemList() {
        try {
            return this.mService.getTvContentRatingSystemList(this.mUserId);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void createSession(String inputId, SessionCallback callback, Handler handler) {
        if (inputId == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback cannot be null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler cannot be null");
        }
        SessionCallbackRecord record = new SessionCallbackRecord(callback, handler);
        synchronized (this.mSessionCallbackRecordMap) {
            int seq = this.mNextSeq;
            this.mNextSeq = seq + 1;
            this.mSessionCallbackRecordMap.put(seq, record);
            try {
                this.mService.createSession(this.mClient, inputId, seq, this.mUserId);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public List<TvStreamConfig> getAvailableTvStreamConfigList(String inputId) {
        try {
            return this.mService.getAvailableTvStreamConfigList(inputId, this.mUserId);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean captureFrame(String inputId, Surface surface, TvStreamConfig config) {
        try {
            return this.mService.captureFrame(inputId, surface, config, this.mUserId);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isSingleSessionActive() {
        try {
            return this.mService.isSingleSessionActive(this.mUserId);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public List<TvInputHardwareInfo> getHardwareList() {
        try {
            return this.mService.getHardwareList();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public Hardware acquireTvInputHardware(int deviceId, final HardwareCallback callback, TvInputInfo info) {
        try {
            return new Hardware(this.mService.acquireTvInputHardware(deviceId, new ITvInputHardwareCallback.Stub() {
                @Override
                public void onReleased() {
                    callback.onReleased();
                }

                @Override
                public void onStreamConfigChanged(TvStreamConfig[] configs) {
                    callback.onStreamConfigChanged(configs);
                }
            }, info, this.mUserId));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void releaseTvInputHardware(int deviceId, Hardware hardware) {
        try {
            this.mService.releaseTvInputHardware(deviceId, hardware.getInterface(), this.mUserId);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public static final class Session {
        static final int DISPATCH_HANDLED = 1;
        static final int DISPATCH_IN_PROGRESS = -1;
        static final int DISPATCH_NOT_HANDLED = 0;
        private static final long INPUT_SESSION_NOT_RESPONDING_TIMEOUT = 2500;
        private final List<TvTrackInfo> mAudioTracks;
        private InputChannel mChannel;
        private final InputEventHandler mHandler;
        private final Pools.Pool<PendingEvent> mPendingEventPool;
        private final SparseArray<PendingEvent> mPendingEvents;
        private String mSelectedAudioTrackId;
        private String mSelectedSubtitleTrackId;
        private String mSelectedVideoTrackId;
        private TvInputEventSender mSender;
        private final int mSeq;
        private final ITvInputManager mService;
        private final SparseArray<SessionCallbackRecord> mSessionCallbackRecordMap;
        private final List<TvTrackInfo> mSubtitleTracks;
        private IBinder mToken;
        private final Object mTrackLock;
        private final int mUserId;
        private int mVideoHeight;
        private final List<TvTrackInfo> mVideoTracks;
        private int mVideoWidth;

        public interface FinishedInputEventCallback {
            void onFinishedInputEvent(Object obj, boolean z);
        }

        private Session(IBinder token, InputChannel channel, ITvInputManager service, int userId, int seq, SparseArray<SessionCallbackRecord> sessionCallbackRecordMap) {
            this.mHandler = new InputEventHandler(Looper.getMainLooper());
            this.mPendingEventPool = new Pools.SimplePool(20);
            this.mPendingEvents = new SparseArray<>(20);
            this.mTrackLock = new Object();
            this.mAudioTracks = new ArrayList();
            this.mVideoTracks = new ArrayList();
            this.mSubtitleTracks = new ArrayList();
            this.mToken = token;
            this.mChannel = channel;
            this.mService = service;
            this.mUserId = userId;
            this.mSeq = seq;
            this.mSessionCallbackRecordMap = sessionCallbackRecordMap;
        }

        public void release() {
            if (this.mToken == null) {
                Log.w(TvInputManager.TAG, "The session has been already released");
                return;
            }
            try {
                this.mService.releaseSession(this.mToken, this.mUserId);
                releaseInternal();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }

        void setMain() {
            if (this.mToken == null) {
                Log.w(TvInputManager.TAG, "The session has been already released");
                return;
            }
            try {
                this.mService.setMainSession(this.mToken, this.mUserId);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }

        public void setSurface(Surface surface) {
            if (this.mToken == null) {
                Log.w(TvInputManager.TAG, "The session has been already released");
                return;
            }
            try {
                this.mService.setSurface(this.mToken, surface, this.mUserId);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }

        public void dispatchSurfaceChanged(int format, int width, int height) {
            if (this.mToken == null) {
                Log.w(TvInputManager.TAG, "The session has been already released");
                return;
            }
            try {
                this.mService.dispatchSurfaceChanged(this.mToken, format, width, height, this.mUserId);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }

        public void setStreamVolume(float volume) {
            if (this.mToken == null) {
                Log.w(TvInputManager.TAG, "The session has been already released");
                return;
            }
            try {
                if (volume < 0.0f || volume > 1.0f) {
                    throw new IllegalArgumentException("volume should be between 0.0f and 1.0f");
                }
                this.mService.setVolume(this.mToken, volume, this.mUserId);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }

        public void tune(Uri channelUri) {
            tune(channelUri, null);
        }

        public void tune(Uri channelUri, Bundle params) {
            if (channelUri == null) {
                throw new IllegalArgumentException("channelUri cannot be null");
            }
            if (this.mToken == null) {
                Log.w(TvInputManager.TAG, "The session has been already released");
                return;
            }
            synchronized (this.mTrackLock) {
                this.mAudioTracks.clear();
                this.mVideoTracks.clear();
                this.mSubtitleTracks.clear();
                this.mSelectedAudioTrackId = null;
                this.mSelectedVideoTrackId = null;
                this.mSelectedSubtitleTrackId = null;
                this.mVideoWidth = 0;
                this.mVideoHeight = 0;
            }
            try {
                this.mService.tune(this.mToken, channelUri, params, this.mUserId);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }

        public void setCaptionEnabled(boolean enabled) {
            if (this.mToken == null) {
                Log.w(TvInputManager.TAG, "The session has been already released");
                return;
            }
            try {
                this.mService.setCaptionEnabled(this.mToken, enabled, this.mUserId);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }

        public void selectTrack(int type, String trackId) {
            synchronized (this.mTrackLock) {
                if (type == 0) {
                    if (trackId != null && !containsTrack(this.mAudioTracks, trackId)) {
                        Log.w(TvInputManager.TAG, "Invalid audio trackId: " + trackId);
                        return;
                    }
                } else if (type == 1) {
                    if (trackId != null && !containsTrack(this.mVideoTracks, trackId)) {
                        Log.w(TvInputManager.TAG, "Invalid video trackId: " + trackId);
                        return;
                    }
                } else if (type == 2) {
                    if (trackId != null && !containsTrack(this.mSubtitleTracks, trackId)) {
                        Log.w(TvInputManager.TAG, "Invalid subtitle trackId: " + trackId);
                        return;
                    }
                } else {
                    throw new IllegalArgumentException("invalid type: " + type);
                }
                if (this.mToken == null) {
                    Log.w(TvInputManager.TAG, "The session has been already released");
                    return;
                }
                try {
                    this.mService.selectTrack(this.mToken, type, trackId, this.mUserId);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private boolean containsTrack(List<TvTrackInfo> tracks, String trackId) {
            for (TvTrackInfo track : tracks) {
                if (track.getId().equals(trackId)) {
                    return true;
                }
            }
            return false;
        }

        public List<TvTrackInfo> getTracks(int type) {
            ArrayList arrayList = null;
            synchronized (this.mTrackLock) {
                if (type == 0) {
                    if (this.mAudioTracks != null) {
                        arrayList = new ArrayList(this.mAudioTracks);
                    }
                } else if (type == 1) {
                    if (this.mVideoTracks != null) {
                        arrayList = new ArrayList(this.mVideoTracks);
                    }
                } else if (type == 2) {
                    if (this.mSubtitleTracks != null) {
                        arrayList = new ArrayList(this.mSubtitleTracks);
                    }
                } else {
                    throw new IllegalArgumentException("invalid type: " + type);
                }
                return arrayList;
            }
        }

        public String getSelectedTrack(int type) {
            String str;
            synchronized (this.mTrackLock) {
                if (type == 0) {
                    str = this.mSelectedAudioTrackId;
                } else if (type == 1) {
                    str = this.mSelectedVideoTrackId;
                } else if (type == 2) {
                    str = this.mSelectedSubtitleTrackId;
                } else {
                    throw new IllegalArgumentException("invalid type: " + type);
                }
                return str;
            }
        }

        boolean updateTracks(List<TvTrackInfo> tracks) {
            boolean z = true;
            synchronized (this.mTrackLock) {
                this.mAudioTracks.clear();
                this.mVideoTracks.clear();
                this.mSubtitleTracks.clear();
                for (TvTrackInfo track : tracks) {
                    if (track.getType() == 0) {
                        this.mAudioTracks.add(track);
                    } else if (track.getType() == 1) {
                        this.mVideoTracks.add(track);
                    } else if (track.getType() == 2) {
                        this.mSubtitleTracks.add(track);
                    }
                }
                if (this.mAudioTracks.isEmpty() && this.mVideoTracks.isEmpty() && this.mSubtitleTracks.isEmpty()) {
                    z = false;
                }
            }
            return z;
        }

        boolean updateTrackSelection(int type, String trackId) {
            boolean z = true;
            synchronized (this.mTrackLock) {
                if (type == 0) {
                    if (trackId != this.mSelectedAudioTrackId) {
                        this.mSelectedAudioTrackId = trackId;
                    } else if (type == 1 && trackId != this.mSelectedVideoTrackId) {
                        this.mSelectedVideoTrackId = trackId;
                    } else if (type == 2 && trackId != this.mSelectedSubtitleTrackId) {
                        this.mSelectedSubtitleTrackId = trackId;
                    } else {
                        z = false;
                    }
                }
            }
            return z;
        }

        TvTrackInfo getVideoTrackToNotify() {
            synchronized (this.mTrackLock) {
                if (!this.mVideoTracks.isEmpty() && this.mSelectedVideoTrackId != null) {
                    for (TvTrackInfo track : this.mVideoTracks) {
                        if (track.getId().equals(this.mSelectedVideoTrackId)) {
                            int videoWidth = track.getVideoWidth();
                            int videoHeight = track.getVideoHeight();
                            if (this.mVideoWidth != videoWidth || this.mVideoHeight != videoHeight) {
                                this.mVideoWidth = videoWidth;
                                this.mVideoHeight = videoHeight;
                                return track;
                            }
                        }
                    }
                }
                return null;
            }
        }

        public void sendAppPrivateCommand(String action, Bundle data) {
            if (this.mToken == null) {
                Log.w(TvInputManager.TAG, "The session has been already released");
                return;
            }
            try {
                this.mService.sendAppPrivateCommand(this.mToken, action, data, this.mUserId);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }

        void createOverlayView(View view, Rect frame) {
            if (view == null) {
                throw new IllegalArgumentException("view cannot be null");
            }
            if (frame == null) {
                throw new IllegalArgumentException("frame cannot be null");
            }
            if (view.getWindowToken() == null) {
                throw new IllegalStateException("view must be attached to a window");
            }
            if (this.mToken == null) {
                Log.w(TvInputManager.TAG, "The session has been already released");
                return;
            }
            try {
                this.mService.createOverlayView(this.mToken, view.getWindowToken(), frame, this.mUserId);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }

        void relayoutOverlayView(Rect frame) {
            if (frame == null) {
                throw new IllegalArgumentException("frame cannot be null");
            }
            if (this.mToken == null) {
                Log.w(TvInputManager.TAG, "The session has been already released");
                return;
            }
            try {
                this.mService.relayoutOverlayView(this.mToken, frame, this.mUserId);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }

        void removeOverlayView() {
            if (this.mToken == null) {
                Log.w(TvInputManager.TAG, "The session has been already released");
                return;
            }
            try {
                this.mService.removeOverlayView(this.mToken, this.mUserId);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }

        void requestUnblockContent(TvContentRating unblockedRating) {
            if (this.mToken == null) {
                Log.w(TvInputManager.TAG, "The session has been already released");
            } else {
                if (unblockedRating == null) {
                    throw new IllegalArgumentException("unblockedRating cannot be null");
                }
                try {
                    this.mService.requestUnblockContent(this.mToken, unblockedRating.flattenToString(), this.mUserId);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public int dispatchInputEvent(InputEvent event, Object token, FinishedInputEventCallback callback, Handler handler) {
            int iSendInputEventOnMainLooperLocked;
            if (event == null) {
                throw new IllegalArgumentException("event cannot be null");
            }
            if (callback != null && handler == null) {
                throw new IllegalArgumentException("handler cannot be null");
            }
            synchronized (this.mHandler) {
                if (this.mChannel == null) {
                    iSendInputEventOnMainLooperLocked = 0;
                } else {
                    PendingEvent p = obtainPendingEventLocked(event, token, callback, handler);
                    if (Looper.myLooper() == Looper.getMainLooper()) {
                        iSendInputEventOnMainLooperLocked = sendInputEventOnMainLooperLocked(p);
                    } else {
                        Message msg = this.mHandler.obtainMessage(1, p);
                        msg.setAsynchronous(true);
                        this.mHandler.sendMessage(msg);
                        iSendInputEventOnMainLooperLocked = -1;
                    }
                }
            }
            return iSendInputEventOnMainLooperLocked;
        }

        private void sendInputEventAndReportResultOnMainLooper(PendingEvent p) {
            synchronized (this.mHandler) {
                int result = sendInputEventOnMainLooperLocked(p);
                if (result != -1) {
                    invokeFinishedInputEventCallback(p, false);
                }
            }
        }

        private int sendInputEventOnMainLooperLocked(PendingEvent p) {
            if (this.mChannel != null) {
                if (this.mSender == null) {
                    this.mSender = new TvInputEventSender(this.mChannel, this.mHandler.getLooper());
                }
                InputEvent event = p.mEvent;
                int seq = event.getSequenceNumber();
                if (this.mSender.sendInputEvent(seq, event)) {
                    this.mPendingEvents.put(seq, p);
                    Message msg = this.mHandler.obtainMessage(2, p);
                    msg.setAsynchronous(true);
                    this.mHandler.sendMessageDelayed(msg, INPUT_SESSION_NOT_RESPONDING_TIMEOUT);
                    return -1;
                }
                Log.w(TvInputManager.TAG, "Unable to send input event to session: " + this.mToken + " dropping:" + event);
            }
            return 0;
        }

        void finishedInputEvent(int seq, boolean handled, boolean timeout) {
            synchronized (this.mHandler) {
                int index = this.mPendingEvents.indexOfKey(seq);
                if (index >= 0) {
                    PendingEvent p = this.mPendingEvents.valueAt(index);
                    this.mPendingEvents.removeAt(index);
                    if (timeout) {
                        Log.w(TvInputManager.TAG, "Timeout waiting for seesion to handle input event after 2500 ms: " + this.mToken);
                    } else {
                        this.mHandler.removeMessages(2, p);
                    }
                    invokeFinishedInputEventCallback(p, handled);
                }
            }
        }

        void invokeFinishedInputEventCallback(PendingEvent p, boolean handled) {
            p.mHandled = handled;
            if (p.mEventHandler.getLooper().isCurrentThread()) {
                p.run();
                return;
            }
            Message msg = Message.obtain(p.mEventHandler, p);
            msg.setAsynchronous(true);
            msg.sendToTarget();
        }

        private void flushPendingEventsLocked() {
            this.mHandler.removeMessages(3);
            int count = this.mPendingEvents.size();
            for (int i = 0; i < count; i++) {
                int seq = this.mPendingEvents.keyAt(i);
                Message msg = this.mHandler.obtainMessage(3, seq, 0);
                msg.setAsynchronous(true);
                msg.sendToTarget();
            }
        }

        private PendingEvent obtainPendingEventLocked(InputEvent event, Object token, FinishedInputEventCallback callback, Handler handler) {
            PendingEvent p = this.mPendingEventPool.acquire();
            if (p == null) {
                p = new PendingEvent();
            }
            p.mEvent = event;
            p.mEventToken = token;
            p.mCallback = callback;
            p.mEventHandler = handler;
            return p;
        }

        private void recyclePendingEventLocked(PendingEvent p) {
            p.recycle();
            this.mPendingEventPool.release(p);
        }

        IBinder getToken() {
            return this.mToken;
        }

        private void releaseInternal() {
            this.mToken = null;
            synchronized (this.mHandler) {
                if (this.mChannel != null) {
                    if (this.mSender != null) {
                        flushPendingEventsLocked();
                        this.mSender.dispose();
                        this.mSender = null;
                    }
                    this.mChannel.dispose();
                    this.mChannel = null;
                }
            }
            synchronized (this.mSessionCallbackRecordMap) {
                this.mSessionCallbackRecordMap.remove(this.mSeq);
            }
        }

        private final class InputEventHandler extends Handler {
            public static final int MSG_FLUSH_INPUT_EVENT = 3;
            public static final int MSG_SEND_INPUT_EVENT = 1;
            public static final int MSG_TIMEOUT_INPUT_EVENT = 2;

            InputEventHandler(Looper looper) {
                super(looper, null, true);
            }

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        Session.this.sendInputEventAndReportResultOnMainLooper((PendingEvent) msg.obj);
                        break;
                    case 2:
                        Session.this.finishedInputEvent(msg.arg1, false, true);
                        break;
                    case 3:
                        Session.this.finishedInputEvent(msg.arg1, false, false);
                        break;
                }
            }
        }

        private final class TvInputEventSender extends InputEventSender {
            public TvInputEventSender(InputChannel inputChannel, Looper looper) {
                super(inputChannel, looper);
            }

            @Override
            public void onInputEventFinished(int seq, boolean handled) {
                Session.this.finishedInputEvent(seq, handled, false);
            }
        }

        private final class PendingEvent implements Runnable {
            public FinishedInputEventCallback mCallback;
            public InputEvent mEvent;
            public Handler mEventHandler;
            public Object mEventToken;
            public boolean mHandled;

            private PendingEvent() {
            }

            public void recycle() {
                this.mEvent = null;
                this.mEventToken = null;
                this.mCallback = null;
                this.mEventHandler = null;
                this.mHandled = false;
            }

            @Override
            public void run() {
                this.mCallback.onFinishedInputEvent(this.mEventToken, this.mHandled);
                synchronized (this.mEventHandler) {
                    Session.this.recyclePendingEventLocked(this);
                }
            }
        }
    }

    public static final class Hardware {
        private final ITvInputHardware mInterface;

        private Hardware(ITvInputHardware hardwareInterface) {
            this.mInterface = hardwareInterface;
        }

        private ITvInputHardware getInterface() {
            return this.mInterface;
        }

        public boolean setSurface(Surface surface, TvStreamConfig config) {
            try {
                return this.mInterface.setSurface(surface, config);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }

        public void setStreamVolume(float volume) {
            try {
                this.mInterface.setStreamVolume(volume);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }

        public boolean dispatchKeyEventToHdmi(KeyEvent event) {
            try {
                return this.mInterface.dispatchKeyEventToHdmi(event);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }

        public void overrideAudioSink(int audioType, String audioAddress, int samplingRate, int channelMask, int format) {
            try {
                this.mInterface.overrideAudioSink(audioType, audioAddress, samplingRate, channelMask, format);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
