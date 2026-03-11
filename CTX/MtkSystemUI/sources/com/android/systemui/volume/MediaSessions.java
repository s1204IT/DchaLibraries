package com.android.systemui.volume;

import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.IRemoteVolumeController;
import android.media.MediaMetadata;
import android.media.session.ISessionController;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MediaSessions {
    private static final String TAG = Util.logTag(MediaSessions.class);
    private final Callbacks mCallbacks;
    private final Context mContext;
    private final H mHandler;
    private boolean mInit;
    private final MediaSessionManager mMgr;
    private final Map<MediaSession.Token, MediaControllerRecord> mRecords = new HashMap();
    private final MediaSessionManager.OnActiveSessionsChangedListener mSessionsListener = new MediaSessionManager.OnActiveSessionsChangedListener() {
        @Override
        public void onActiveSessionsChanged(List<MediaController> list) {
            MediaSessions.this.onActiveSessionsUpdatedH(list);
        }
    };
    private final IRemoteVolumeController mRvc = new IRemoteVolumeController.Stub() {
        public void remoteVolumeChanged(ISessionController iSessionController, int i) throws RemoteException {
            MediaSessions.this.mHandler.obtainMessage(2, i, 0, iSessionController).sendToTarget();
        }

        public void updateRemoteController(ISessionController iSessionController) throws RemoteException {
            MediaSessions.this.mHandler.obtainMessage(3, iSessionController).sendToTarget();
        }
    };

    public interface Callbacks {
        void onRemoteRemoved(MediaSession.Token token);

        void onRemoteUpdate(MediaSession.Token token, String str, MediaController.PlaybackInfo playbackInfo);

        void onRemoteVolumeChanged(MediaSession.Token token, int i);
    }

    public MediaSessions(Context context, Looper looper, Callbacks callbacks) {
        this.mContext = context;
        this.mHandler = new H(looper);
        this.mMgr = (MediaSessionManager) context.getSystemService("media_session");
        this.mCallbacks = callbacks;
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println(getClass().getSimpleName() + " state:");
        printWriter.print("  mInit: ");
        printWriter.println(this.mInit);
        printWriter.print("  mRecords.size: ");
        printWriter.println(this.mRecords.size());
        Iterator<MediaControllerRecord> it = this.mRecords.values().iterator();
        int i = 0;
        while (it.hasNext()) {
            i++;
            dump(i, printWriter, it.next().controller);
        }
    }

    public void init() {
        if (D.BUG) {
            Log.d(TAG, "init");
        }
        this.mMgr.addOnActiveSessionsChangedListener(this.mSessionsListener, null, this.mHandler);
        this.mInit = true;
        postUpdateSessions();
        this.mMgr.setRemoteVolumeController(this.mRvc);
    }

    protected void postUpdateSessions() {
        if (this.mInit) {
            this.mHandler.sendEmptyMessage(1);
        }
    }

    public void setVolume(MediaSession.Token token, int i) {
        MediaControllerRecord mediaControllerRecord = this.mRecords.get(token);
        if (mediaControllerRecord == null) {
            Log.w(TAG, "setVolume: No record found for token " + token);
            return;
        }
        if (D.BUG) {
            Log.d(TAG, "Setting level to " + i);
        }
        mediaControllerRecord.controller.setVolumeTo(i, 0);
    }

    public void onRemoteVolumeChangedH(ISessionController iSessionController, int i) {
        MediaController mediaController = new MediaController(this.mContext, iSessionController);
        if (D.BUG) {
            Log.d(TAG, "remoteVolumeChangedH " + mediaController.getPackageName() + " " + Util.audioManagerFlagsToString(i));
        }
        this.mCallbacks.onRemoteVolumeChanged(mediaController.getSessionToken(), i);
    }

    public void onUpdateRemoteControllerH(ISessionController iSessionController) {
        MediaController mediaController = iSessionController != null ? new MediaController(this.mContext, iSessionController) : null;
        String packageName = mediaController != null ? mediaController.getPackageName() : null;
        if (D.BUG) {
            Log.d(TAG, "updateRemoteControllerH " + packageName);
        }
        postUpdateSessions();
    }

    protected void onActiveSessionsUpdatedH(List<MediaController> list) {
        if (D.BUG) {
            Log.d(TAG, "onActiveSessionsUpdatedH n=" + list.size());
        }
        HashSet<MediaSession.Token> hashSet = new HashSet(this.mRecords.keySet());
        for (MediaController mediaController : list) {
            MediaSession.Token sessionToken = mediaController.getSessionToken();
            MediaController.PlaybackInfo playbackInfo = mediaController.getPlaybackInfo();
            hashSet.remove(sessionToken);
            if (!this.mRecords.containsKey(sessionToken)) {
                MediaControllerRecord mediaControllerRecord = new MediaControllerRecord(mediaController);
                mediaControllerRecord.name = getControllerName(mediaController);
                this.mRecords.put(sessionToken, mediaControllerRecord);
                mediaController.registerCallback(mediaControllerRecord, this.mHandler);
            }
            MediaControllerRecord mediaControllerRecord2 = this.mRecords.get(sessionToken);
            if (isRemote(playbackInfo)) {
                updateRemoteH(sessionToken, mediaControllerRecord2.name, playbackInfo);
                mediaControllerRecord2.sentRemote = true;
            }
        }
        for (MediaSession.Token token : hashSet) {
            MediaControllerRecord mediaControllerRecord3 = this.mRecords.get(token);
            mediaControllerRecord3.controller.unregisterCallback(mediaControllerRecord3);
            this.mRecords.remove(token);
            if (D.BUG) {
                Log.d(TAG, "Removing " + mediaControllerRecord3.name + " sentRemote=" + mediaControllerRecord3.sentRemote);
            }
            if (mediaControllerRecord3.sentRemote) {
                this.mCallbacks.onRemoteRemoved(token);
                mediaControllerRecord3.sentRemote = false;
            }
        }
    }

    public static boolean isRemote(MediaController.PlaybackInfo playbackInfo) {
        return playbackInfo != null && playbackInfo.getPlaybackType() == 2;
    }

    protected String getControllerName(MediaController mediaController) {
        String strTrim;
        PackageManager packageManager = this.mContext.getPackageManager();
        String packageName = mediaController.getPackageName();
        try {
            strTrim = Objects.toString(packageManager.getApplicationInfo(packageName, 0).loadLabel(packageManager), "").trim();
        } catch (PackageManager.NameNotFoundException e) {
        }
        if (strTrim.length() > 0) {
            return strTrim;
        }
        return packageName;
    }

    public void updateRemoteH(MediaSession.Token token, String str, MediaController.PlaybackInfo playbackInfo) {
        if (this.mCallbacks != null) {
            this.mCallbacks.onRemoteUpdate(token, str, playbackInfo);
        }
    }

    private static void dump(int i, PrintWriter printWriter, MediaController mediaController) {
        printWriter.println("  Controller " + i + ": " + mediaController.getPackageName());
        Bundle extras = mediaController.getExtras();
        long flags = mediaController.getFlags();
        MediaMetadata metadata = mediaController.getMetadata();
        MediaController.PlaybackInfo playbackInfo = mediaController.getPlaybackInfo();
        PlaybackState playbackState = mediaController.getPlaybackState();
        List<MediaSession.QueueItem> queue = mediaController.getQueue();
        CharSequence queueTitle = mediaController.getQueueTitle();
        int ratingType = mediaController.getRatingType();
        PendingIntent sessionActivity = mediaController.getSessionActivity();
        printWriter.println("    PlaybackState: " + Util.playbackStateToString(playbackState));
        printWriter.println("    PlaybackInfo: " + Util.playbackInfoToString(playbackInfo));
        if (metadata != null) {
            printWriter.println("  MediaMetadata.desc=" + metadata.getDescription());
        }
        printWriter.println("    RatingType: " + ratingType);
        printWriter.println("    Flags: " + flags);
        if (extras != null) {
            printWriter.println("    Extras:");
            for (String str : extras.keySet()) {
                printWriter.println("      " + str + "=" + extras.get(str));
            }
        }
        if (queueTitle != null) {
            printWriter.println("    QueueTitle: " + ((Object) queueTitle));
        }
        if (queue != null && !queue.isEmpty()) {
            printWriter.println("    Queue:");
            Iterator<MediaSession.QueueItem> it = queue.iterator();
            while (it.hasNext()) {
                printWriter.println("      " + it.next());
            }
        }
        if (playbackInfo != null) {
            printWriter.println("    sessionActivity: " + sessionActivity);
        }
    }

    private final class MediaControllerRecord extends MediaController.Callback {
        private final MediaController controller;
        private String name;
        private boolean sentRemote;

        private MediaControllerRecord(MediaController mediaController) {
            this.controller = mediaController;
        }

        private String cb(String str) {
            return str + " " + this.controller.getPackageName() + " ";
        }

        @Override
        public void onAudioInfoChanged(MediaController.PlaybackInfo playbackInfo) {
            if (D.BUG) {
                Log.d(MediaSessions.TAG, cb("onAudioInfoChanged") + Util.playbackInfoToString(playbackInfo) + " sentRemote=" + this.sentRemote);
            }
            boolean zIsRemote = MediaSessions.isRemote(playbackInfo);
            if (!zIsRemote && this.sentRemote) {
                MediaSessions.this.mCallbacks.onRemoteRemoved(this.controller.getSessionToken());
                this.sentRemote = false;
            } else if (zIsRemote) {
                MediaSessions.this.updateRemoteH(this.controller.getSessionToken(), this.name, playbackInfo);
                this.sentRemote = true;
            }
        }

        @Override
        public void onExtrasChanged(Bundle bundle) {
            if (D.BUG) {
                Log.d(MediaSessions.TAG, cb("onExtrasChanged") + bundle);
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadata mediaMetadata) {
            if (D.BUG) {
                Log.d(MediaSessions.TAG, cb("onMetadataChanged") + Util.mediaMetadataToString(mediaMetadata));
            }
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState playbackState) {
            if (D.BUG) {
                Log.d(MediaSessions.TAG, cb("onPlaybackStateChanged") + Util.playbackStateToString(playbackState));
            }
        }

        @Override
        public void onQueueChanged(List<MediaSession.QueueItem> list) {
            if (D.BUG) {
                Log.d(MediaSessions.TAG, cb("onQueueChanged") + list);
            }
        }

        @Override
        public void onQueueTitleChanged(CharSequence charSequence) {
            if (D.BUG) {
                Log.d(MediaSessions.TAG, cb("onQueueTitleChanged") + ((Object) charSequence));
            }
        }

        @Override
        public void onSessionDestroyed() {
            if (D.BUG) {
                Log.d(MediaSessions.TAG, cb("onSessionDestroyed"));
            }
        }

        @Override
        public void onSessionEvent(String str, Bundle bundle) {
            if (D.BUG) {
                Log.d(MediaSessions.TAG, cb("onSessionEvent") + "event=" + str + " extras=" + bundle);
            }
        }
    }

    private final class H extends Handler {
        private H(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    MediaSessions.this.onActiveSessionsUpdatedH(MediaSessions.this.mMgr.getActiveSessions(null));
                    break;
                case 2:
                    MediaSessions.this.onRemoteVolumeChangedH((ISessionController) message.obj, message.arg1);
                    break;
                case 3:
                    MediaSessions.this.onUpdateRemoteControllerH((ISessionController) message.obj);
                    break;
            }
        }
    }
}
