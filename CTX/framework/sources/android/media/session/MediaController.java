package android.media.session;

import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.media.AudioAttributes;
import android.media.MediaMetadata;
import android.media.Rating;
import android.media.session.ISessionControllerCallback;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public final class MediaController {
    private static final int MSG_DESTROYED = 8;
    private static final int MSG_EVENT = 1;
    private static final int MSG_UPDATE_EXTRAS = 7;
    private static final int MSG_UPDATE_METADATA = 3;
    private static final int MSG_UPDATE_PLAYBACK_STATE = 2;
    private static final int MSG_UPDATE_QUEUE = 5;
    private static final int MSG_UPDATE_QUEUE_TITLE = 6;
    private static final int MSG_UPDATE_VOLUME = 4;
    private static final String TAG = "MediaController";
    private final ArrayList<MessageHandler> mCallbacks;
    private boolean mCbRegistered;
    private final CallbackStub mCbStub;
    private final Context mContext;
    private final Object mLock;
    private String mPackageName;
    private final ISessionController mSessionBinder;
    private String mTag;
    private final MediaSession.Token mToken;
    private final TransportControls mTransportControls;

    public MediaController(Context context, ISessionController iSessionController) {
        this.mCbStub = new CallbackStub(this);
        this.mCallbacks = new ArrayList<>();
        this.mLock = new Object();
        this.mCbRegistered = false;
        if (iSessionController == null) {
            throw new IllegalArgumentException("Session token cannot be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        this.mSessionBinder = iSessionController;
        this.mTransportControls = new TransportControls();
        this.mToken = new MediaSession.Token(iSessionController);
        this.mContext = context;
    }

    public MediaController(Context context, MediaSession.Token token) {
        this(context, token.getBinder());
    }

    public TransportControls getTransportControls() {
        return this.mTransportControls;
    }

    public boolean dispatchMediaButtonEvent(KeyEvent keyEvent) {
        return dispatchMediaButtonEventInternal(false, keyEvent);
    }

    public boolean dispatchMediaButtonEventAsSystemService(KeyEvent keyEvent) {
        return dispatchMediaButtonEventInternal(true, keyEvent);
    }

    private boolean dispatchMediaButtonEventInternal(boolean z, KeyEvent keyEvent) {
        if (keyEvent == null) {
            throw new IllegalArgumentException("KeyEvent may not be null");
        }
        if (!KeyEvent.isMediaKey(keyEvent.getKeyCode())) {
            return false;
        }
        try {
            return this.mSessionBinder.sendMediaButton(this.mContext.getPackageName(), this.mCbStub, z, keyEvent);
        } catch (RemoteException e) {
            return false;
        }
    }

    public void dispatchVolumeButtonEventAsSystemService(KeyEvent keyEvent) {
        switch (keyEvent.getAction()) {
            case 0:
                int i = 0;
                int keyCode = keyEvent.getKeyCode();
                if (keyCode != 164) {
                    switch (keyCode) {
                        case 24:
                            i = 1;
                            break;
                        case 25:
                            i = -1;
                            break;
                    }
                } else {
                    i = 101;
                }
                try {
                    this.mSessionBinder.adjustVolume(this.mContext.getPackageName(), this.mCbStub, true, i, 1);
                } catch (RemoteException e) {
                    Log.wtf(TAG, "Error calling adjustVolumeBy", e);
                }
                break;
            case 1:
                break;
            default:
                return;
        }
        try {
            this.mSessionBinder.adjustVolume(this.mContext.getPackageName(), this.mCbStub, true, 0, 4116);
        } catch (RemoteException e2) {
            Log.wtf(TAG, "Error calling adjustVolumeBy", e2);
        }
    }

    public PlaybackState getPlaybackState() {
        try {
            return this.mSessionBinder.getPlaybackState();
        } catch (RemoteException e) {
            Log.wtf(TAG, "Error calling getPlaybackState.", e);
            return null;
        }
    }

    public MediaMetadata getMetadata() {
        try {
            return this.mSessionBinder.getMetadata();
        } catch (RemoteException e) {
            Log.wtf(TAG, "Error calling getMetadata.", e);
            return null;
        }
    }

    public List<MediaSession.QueueItem> getQueue() {
        try {
            ParceledListSlice queue = this.mSessionBinder.getQueue();
            if (queue != null) {
                return queue.getList();
            }
            return null;
        } catch (RemoteException e) {
            Log.wtf(TAG, "Error calling getQueue.", e);
            return null;
        }
    }

    public CharSequence getQueueTitle() {
        try {
            return this.mSessionBinder.getQueueTitle();
        } catch (RemoteException e) {
            Log.wtf(TAG, "Error calling getQueueTitle", e);
            return null;
        }
    }

    public Bundle getExtras() {
        try {
            return this.mSessionBinder.getExtras();
        } catch (RemoteException e) {
            Log.wtf(TAG, "Error calling getExtras", e);
            return null;
        }
    }

    public int getRatingType() {
        try {
            return this.mSessionBinder.getRatingType();
        } catch (RemoteException e) {
            Log.wtf(TAG, "Error calling getRatingType.", e);
            return 0;
        }
    }

    public long getFlags() {
        try {
            return this.mSessionBinder.getFlags();
        } catch (RemoteException e) {
            Log.wtf(TAG, "Error calling getFlags.", e);
            return 0L;
        }
    }

    public PlaybackInfo getPlaybackInfo() {
        try {
            ParcelableVolumeInfo volumeAttributes = this.mSessionBinder.getVolumeAttributes();
            return new PlaybackInfo(volumeAttributes.volumeType, volumeAttributes.audioAttrs, volumeAttributes.controlType, volumeAttributes.maxVolume, volumeAttributes.currentVolume);
        } catch (RemoteException e) {
            Log.wtf(TAG, "Error calling getAudioInfo.", e);
            return null;
        }
    }

    public PendingIntent getSessionActivity() {
        try {
            return this.mSessionBinder.getLaunchPendingIntent();
        } catch (RemoteException e) {
            Log.wtf(TAG, "Error calling getPendingIntent.", e);
            return null;
        }
    }

    public MediaSession.Token getSessionToken() {
        return this.mToken;
    }

    public void setVolumeTo(int i, int i2) {
        try {
            this.mSessionBinder.setVolumeTo(this.mContext.getPackageName(), this.mCbStub, i, i2);
        } catch (RemoteException e) {
            Log.wtf(TAG, "Error calling setVolumeTo.", e);
        }
    }

    public void adjustVolume(int i, int i2) {
        try {
            this.mSessionBinder.adjustVolume(this.mContext.getPackageName(), this.mCbStub, false, i, i2);
        } catch (RemoteException e) {
            Log.wtf(TAG, "Error calling adjustVolumeBy.", e);
        }
    }

    public void registerCallback(Callback callback) {
        registerCallback(callback, null);
    }

    public void registerCallback(Callback callback, Handler handler) {
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        if (handler == null) {
            handler = new Handler();
        }
        synchronized (this.mLock) {
            addCallbackLocked(callback, handler);
        }
    }

    public void unregisterCallback(Callback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        synchronized (this.mLock) {
            removeCallbackLocked(callback);
        }
    }

    public void sendCommand(String str, Bundle bundle, ResultReceiver resultReceiver) {
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("command cannot be null or empty");
        }
        try {
            this.mSessionBinder.sendCommand(this.mContext.getPackageName(), this.mCbStub, str, bundle, resultReceiver);
        } catch (RemoteException e) {
            Log.d(TAG, "Dead object in sendCommand.", e);
        }
    }

    public String getPackageName() {
        if (this.mPackageName == null) {
            try {
                this.mPackageName = this.mSessionBinder.getPackageName();
            } catch (RemoteException e) {
                Log.d(TAG, "Dead object in getPackageName.", e);
            }
        }
        return this.mPackageName;
    }

    public String getTag() {
        if (this.mTag == null) {
            try {
                this.mTag = this.mSessionBinder.getTag();
            } catch (RemoteException e) {
                Log.d(TAG, "Dead object in getTag.", e);
            }
        }
        return this.mTag;
    }

    ISessionController getSessionBinder() {
        return this.mSessionBinder;
    }

    public boolean controlsSameSession(MediaController mediaController) {
        return mediaController != null && this.mSessionBinder.asBinder() == mediaController.getSessionBinder().asBinder();
    }

    private void addCallbackLocked(Callback callback, Handler handler) {
        if (getHandlerForCallbackLocked(callback) != null) {
            Log.w(TAG, "Callback is already added, ignoring");
            return;
        }
        MessageHandler messageHandler = new MessageHandler(handler.getLooper(), callback);
        this.mCallbacks.add(messageHandler);
        messageHandler.mRegistered = true;
        if (!this.mCbRegistered) {
            try {
                this.mSessionBinder.registerCallbackListener(this.mContext.getPackageName(), this.mCbStub);
                this.mCbRegistered = true;
            } catch (RemoteException e) {
                Log.e(TAG, "Dead object in registerCallback", e);
            }
        }
    }

    private boolean removeCallbackLocked(Callback callback) {
        boolean z = false;
        for (int size = this.mCallbacks.size() - 1; size >= 0; size--) {
            MessageHandler messageHandler = this.mCallbacks.get(size);
            if (callback == messageHandler.mCallback) {
                this.mCallbacks.remove(size);
                messageHandler.mRegistered = false;
                z = true;
            }
        }
        if (this.mCbRegistered && this.mCallbacks.size() == 0) {
            try {
                this.mSessionBinder.unregisterCallbackListener(this.mCbStub);
            } catch (RemoteException e) {
                Log.e(TAG, "Dead object in removeCallbackLocked");
            }
            this.mCbRegistered = false;
        }
        return z;
    }

    private MessageHandler getHandlerForCallbackLocked(Callback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Callback cannot be null");
        }
        for (int size = this.mCallbacks.size() - 1; size >= 0; size--) {
            MessageHandler messageHandler = this.mCallbacks.get(size);
            if (callback == messageHandler.mCallback) {
                return messageHandler;
            }
        }
        return null;
    }

    private final void postMessage(int i, Object obj, Bundle bundle) {
        synchronized (this.mLock) {
            for (int size = this.mCallbacks.size() - 1; size >= 0; size--) {
                this.mCallbacks.get(size).post(i, obj, bundle);
            }
        }
    }

    public static abstract class Callback {
        public void onSessionDestroyed() {
        }

        public void onSessionEvent(String str, Bundle bundle) {
        }

        public void onPlaybackStateChanged(PlaybackState playbackState) {
        }

        public void onMetadataChanged(MediaMetadata mediaMetadata) {
        }

        public void onQueueChanged(List<MediaSession.QueueItem> list) {
        }

        public void onQueueTitleChanged(CharSequence charSequence) {
        }

        public void onExtrasChanged(Bundle bundle) {
        }

        public void onAudioInfoChanged(PlaybackInfo playbackInfo) {
        }
    }

    public final class TransportControls {
        private static final String TAG = "TransportController";

        private TransportControls() {
        }

        public void prepare() {
            try {
                MediaController.this.mSessionBinder.prepare(MediaController.this.mContext.getPackageName(), MediaController.this.mCbStub);
            } catch (RemoteException e) {
                Log.wtf(TAG, "Error calling prepare.", e);
            }
        }

        public void prepareFromMediaId(String str, Bundle bundle) {
            if (!TextUtils.isEmpty(str)) {
                try {
                    MediaController.this.mSessionBinder.prepareFromMediaId(MediaController.this.mContext.getPackageName(), MediaController.this.mCbStub, str, bundle);
                    return;
                } catch (RemoteException e) {
                    Log.wtf(TAG, "Error calling prepare(" + str + ").", e);
                    return;
                }
            }
            throw new IllegalArgumentException("You must specify a non-empty String for prepareFromMediaId.");
        }

        public void prepareFromSearch(String str, Bundle bundle) {
            if (str == null) {
                str = "";
            }
            try {
                MediaController.this.mSessionBinder.prepareFromSearch(MediaController.this.mContext.getPackageName(), MediaController.this.mCbStub, str, bundle);
            } catch (RemoteException e) {
                Log.wtf(TAG, "Error calling prepare(" + str + ").", e);
            }
        }

        public void prepareFromUri(Uri uri, Bundle bundle) {
            if (uri != null && !Uri.EMPTY.equals(uri)) {
                try {
                    MediaController.this.mSessionBinder.prepareFromUri(MediaController.this.mContext.getPackageName(), MediaController.this.mCbStub, uri, bundle);
                    return;
                } catch (RemoteException e) {
                    Log.wtf(TAG, "Error calling prepare(" + uri + ").", e);
                    return;
                }
            }
            throw new IllegalArgumentException("You must specify a non-empty Uri for prepareFromUri.");
        }

        public void play() {
            try {
                MediaController.this.mSessionBinder.play(MediaController.this.mContext.getPackageName(), MediaController.this.mCbStub);
            } catch (RemoteException e) {
                Log.wtf(TAG, "Error calling play.", e);
            }
        }

        public void playFromMediaId(String str, Bundle bundle) {
            if (!TextUtils.isEmpty(str)) {
                try {
                    MediaController.this.mSessionBinder.playFromMediaId(MediaController.this.mContext.getPackageName(), MediaController.this.mCbStub, str, bundle);
                    return;
                } catch (RemoteException e) {
                    Log.wtf(TAG, "Error calling play(" + str + ").", e);
                    return;
                }
            }
            throw new IllegalArgumentException("You must specify a non-empty String for playFromMediaId.");
        }

        public void playFromSearch(String str, Bundle bundle) {
            if (str == null) {
                str = "";
            }
            try {
                MediaController.this.mSessionBinder.playFromSearch(MediaController.this.mContext.getPackageName(), MediaController.this.mCbStub, str, bundle);
            } catch (RemoteException e) {
                Log.wtf(TAG, "Error calling play(" + str + ").", e);
            }
        }

        public void playFromUri(Uri uri, Bundle bundle) {
            if (uri != null && !Uri.EMPTY.equals(uri)) {
                try {
                    MediaController.this.mSessionBinder.playFromUri(MediaController.this.mContext.getPackageName(), MediaController.this.mCbStub, uri, bundle);
                    return;
                } catch (RemoteException e) {
                    Log.wtf(TAG, "Error calling play(" + uri + ").", e);
                    return;
                }
            }
            throw new IllegalArgumentException("You must specify a non-empty Uri for playFromUri.");
        }

        public void skipToQueueItem(long j) {
            try {
                MediaController.this.mSessionBinder.skipToQueueItem(MediaController.this.mContext.getPackageName(), MediaController.this.mCbStub, j);
            } catch (RemoteException e) {
                Log.wtf(TAG, "Error calling skipToItem(" + j + ").", e);
            }
        }

        public void pause() {
            try {
                MediaController.this.mSessionBinder.pause(MediaController.this.mContext.getPackageName(), MediaController.this.mCbStub);
            } catch (RemoteException e) {
                Log.wtf(TAG, "Error calling pause.", e);
            }
        }

        public void stop() {
            try {
                MediaController.this.mSessionBinder.stop(MediaController.this.mContext.getPackageName(), MediaController.this.mCbStub);
            } catch (RemoteException e) {
                Log.wtf(TAG, "Error calling stop.", e);
            }
        }

        public void seekTo(long j) {
            try {
                MediaController.this.mSessionBinder.seekTo(MediaController.this.mContext.getPackageName(), MediaController.this.mCbStub, j);
            } catch (RemoteException e) {
                Log.wtf(TAG, "Error calling seekTo.", e);
            }
        }

        public void fastForward() {
            try {
                MediaController.this.mSessionBinder.fastForward(MediaController.this.mContext.getPackageName(), MediaController.this.mCbStub);
            } catch (RemoteException e) {
                Log.wtf(TAG, "Error calling fastForward.", e);
            }
        }

        public void skipToNext() {
            try {
                MediaController.this.mSessionBinder.next(MediaController.this.mContext.getPackageName(), MediaController.this.mCbStub);
            } catch (RemoteException e) {
                Log.wtf(TAG, "Error calling next.", e);
            }
        }

        public void rewind() {
            try {
                MediaController.this.mSessionBinder.rewind(MediaController.this.mContext.getPackageName(), MediaController.this.mCbStub);
            } catch (RemoteException e) {
                Log.wtf(TAG, "Error calling rewind.", e);
            }
        }

        public void skipToPrevious() {
            try {
                MediaController.this.mSessionBinder.previous(MediaController.this.mContext.getPackageName(), MediaController.this.mCbStub);
            } catch (RemoteException e) {
                Log.wtf(TAG, "Error calling previous.", e);
            }
        }

        public void setRating(Rating rating) {
            try {
                MediaController.this.mSessionBinder.rate(MediaController.this.mContext.getPackageName(), MediaController.this.mCbStub, rating);
            } catch (RemoteException e) {
                Log.wtf(TAG, "Error calling rate.", e);
            }
        }

        public void sendCustomAction(PlaybackState.CustomAction customAction, Bundle bundle) {
            if (customAction == null) {
                throw new IllegalArgumentException("CustomAction cannot be null.");
            }
            sendCustomAction(customAction.getAction(), bundle);
        }

        public void sendCustomAction(String str, Bundle bundle) {
            if (!TextUtils.isEmpty(str)) {
                try {
                    MediaController.this.mSessionBinder.sendCustomAction(MediaController.this.mContext.getPackageName(), MediaController.this.mCbStub, str, bundle);
                    return;
                } catch (RemoteException e) {
                    Log.d(TAG, "Dead object in sendCustomAction.", e);
                    return;
                }
            }
            throw new IllegalArgumentException("CustomAction cannot be null.");
        }
    }

    public static final class PlaybackInfo {
        public static final int PLAYBACK_TYPE_LOCAL = 1;
        public static final int PLAYBACK_TYPE_REMOTE = 2;
        private final AudioAttributes mAudioAttrs;
        private final int mCurrentVolume;
        private final int mMaxVolume;
        private final int mVolumeControl;
        private final int mVolumeType;

        public PlaybackInfo(int i, AudioAttributes audioAttributes, int i2, int i3, int i4) {
            this.mVolumeType = i;
            this.mAudioAttrs = audioAttributes;
            this.mVolumeControl = i2;
            this.mMaxVolume = i3;
            this.mCurrentVolume = i4;
        }

        public int getPlaybackType() {
            return this.mVolumeType;
        }

        public AudioAttributes getAudioAttributes() {
            return this.mAudioAttrs;
        }

        public int getVolumeControl() {
            return this.mVolumeControl;
        }

        public int getMaxVolume() {
            return this.mMaxVolume;
        }

        public int getCurrentVolume() {
            return this.mCurrentVolume;
        }
    }

    private static final class CallbackStub extends ISessionControllerCallback.Stub {
        private final WeakReference<MediaController> mController;

        public CallbackStub(MediaController mediaController) {
            this.mController = new WeakReference<>(mediaController);
        }

        @Override
        public void onSessionDestroyed() {
            MediaController mediaController = this.mController.get();
            if (mediaController != null) {
                mediaController.postMessage(8, null, null);
            }
        }

        @Override
        public void onEvent(String str, Bundle bundle) {
            MediaController mediaController = this.mController.get();
            if (mediaController != null) {
                mediaController.postMessage(1, str, bundle);
            }
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState playbackState) {
            MediaController mediaController = this.mController.get();
            if (mediaController != null) {
                mediaController.postMessage(2, playbackState, null);
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadata mediaMetadata) {
            MediaController mediaController = this.mController.get();
            if (mediaController != null) {
                mediaController.postMessage(3, mediaMetadata, null);
            }
        }

        @Override
        public void onQueueChanged(ParceledListSlice parceledListSlice) {
            List list;
            if (parceledListSlice != null) {
                list = parceledListSlice.getList();
            } else {
                list = null;
            }
            MediaController mediaController = this.mController.get();
            if (mediaController != null) {
                mediaController.postMessage(5, list, null);
            }
        }

        @Override
        public void onQueueTitleChanged(CharSequence charSequence) {
            MediaController mediaController = this.mController.get();
            if (mediaController != null) {
                mediaController.postMessage(6, charSequence, null);
            }
        }

        @Override
        public void onExtrasChanged(Bundle bundle) {
            MediaController mediaController = this.mController.get();
            if (mediaController != null) {
                mediaController.postMessage(7, bundle, null);
            }
        }

        @Override
        public void onVolumeInfoChanged(ParcelableVolumeInfo parcelableVolumeInfo) {
            MediaController mediaController = this.mController.get();
            if (mediaController != null) {
                mediaController.postMessage(4, new PlaybackInfo(parcelableVolumeInfo.volumeType, parcelableVolumeInfo.audioAttrs, parcelableVolumeInfo.controlType, parcelableVolumeInfo.maxVolume, parcelableVolumeInfo.currentVolume), null);
            }
        }
    }

    private static final class MessageHandler extends Handler {
        private final Callback mCallback;
        private boolean mRegistered;

        public MessageHandler(Looper looper, Callback callback) {
            super(looper, null, true);
            this.mRegistered = false;
            this.mCallback = callback;
        }

        @Override
        public void handleMessage(Message message) {
            if (!this.mRegistered) {
            }
            switch (message.what) {
                case 1:
                    this.mCallback.onSessionEvent((String) message.obj, message.getData());
                    break;
                case 2:
                    this.mCallback.onPlaybackStateChanged((PlaybackState) message.obj);
                    break;
                case 3:
                    this.mCallback.onMetadataChanged((MediaMetadata) message.obj);
                    break;
                case 4:
                    this.mCallback.onAudioInfoChanged((PlaybackInfo) message.obj);
                    break;
                case 5:
                    this.mCallback.onQueueChanged((List) message.obj);
                    break;
                case 6:
                    this.mCallback.onQueueTitleChanged((CharSequence) message.obj);
                    break;
                case 7:
                    this.mCallback.onExtrasChanged((Bundle) message.obj);
                    break;
                case 8:
                    this.mCallback.onSessionDestroyed();
                    break;
            }
        }

        public void post(int i, Object obj, Bundle bundle) {
            Message messageObtainMessage = obtainMessage(i, obj);
            messageObtainMessage.setData(bundle);
            messageObtainMessage.sendToTarget();
        }
    }
}
