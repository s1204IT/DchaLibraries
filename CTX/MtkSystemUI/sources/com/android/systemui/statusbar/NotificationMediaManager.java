package com.android.systemui.statusbar;

import android.R;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import com.android.systemui.Dumpable;
import com.android.systemui.statusbar.NotificationData;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public class NotificationMediaManager implements Dumpable {
    private final Context mContext;
    protected NotificationEntryManager mEntryManager;
    private MediaController mMediaController;
    private final MediaController.Callback mMediaListener = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState playbackState) {
            super.onPlaybackStateChanged(playbackState);
            if (playbackState != null && !NotificationMediaManager.this.isPlaybackActive(playbackState.getState())) {
                NotificationMediaManager.this.clearCurrentMediaNotification();
                NotificationMediaManager.this.mPresenter.updateMediaMetaData(true, true);
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadata mediaMetadata) {
            super.onMetadataChanged(mediaMetadata);
            NotificationMediaManager.this.mMediaMetadata = mediaMetadata;
            NotificationMediaManager.this.mPresenter.updateMediaMetaData(true, true);
        }
    };
    private MediaMetadata mMediaMetadata;
    private String mMediaNotificationKey;
    private final MediaSessionManager mMediaSessionManager;
    protected NotificationPresenter mPresenter;

    public NotificationMediaManager(Context context) {
        this.mContext = context;
        this.mMediaSessionManager = (MediaSessionManager) this.mContext.getSystemService("media_session");
    }

    public void setUpWithPresenter(NotificationPresenter notificationPresenter, NotificationEntryManager notificationEntryManager) {
        this.mPresenter = notificationPresenter;
        this.mEntryManager = notificationEntryManager;
    }

    public void onNotificationRemoved(String str) {
        if (str.equals(this.mMediaNotificationKey)) {
            clearCurrentMediaNotification();
            this.mPresenter.updateMediaMetaData(true, true);
        }
    }

    public String getMediaNotificationKey() {
        return this.mMediaNotificationKey;
    }

    public MediaMetadata getMediaMetadata() {
        return this.mMediaMetadata;
    }

    public void findAndUpdateMediaNotifications() {
        boolean z;
        NotificationData.Entry entry;
        MediaController mediaController;
        MediaSession.Token token;
        synchronized (this.mEntryManager.getNotificationData()) {
            ArrayList<NotificationData.Entry> activeNotifications = this.mEntryManager.getNotificationData().getActiveNotifications();
            int size = activeNotifications.size();
            z = false;
            int i = 0;
            while (true) {
                if (i < size) {
                    entry = activeNotifications.get(i);
                    if (isMediaNotification(entry) && (token = (MediaSession.Token) entry.notification.getNotification().extras.getParcelable("android.mediaSession")) != null) {
                        mediaController = new MediaController(this.mContext, token);
                        if (3 == getMediaControllerPlaybackState(mediaController)) {
                            break;
                        }
                    }
                    i++;
                } else {
                    entry = null;
                    mediaController = null;
                    break;
                }
            }
            if (entry == null && this.mMediaSessionManager != null) {
                for (MediaController mediaController2 : this.mMediaSessionManager.getActiveSessionsForUser(null, -1)) {
                    if (3 == getMediaControllerPlaybackState(mediaController2)) {
                        String packageName = mediaController2.getPackageName();
                        int i2 = 0;
                        while (true) {
                            if (i2 < size) {
                                NotificationData.Entry entry2 = activeNotifications.get(i2);
                                if (!entry2.notification.getPackageName().equals(packageName)) {
                                    i2++;
                                } else {
                                    mediaController = mediaController2;
                                    entry = entry2;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if (mediaController != null && !sameSessions(this.mMediaController, mediaController)) {
                clearCurrentMediaNotificationSession();
                this.mMediaController = mediaController;
                this.mMediaController.registerCallback(this.mMediaListener);
                this.mMediaMetadata = this.mMediaController.getMetadata();
                z = true;
            }
            if (entry != null && !entry.notification.getKey().equals(this.mMediaNotificationKey)) {
                this.mMediaNotificationKey = entry.notification.getKey();
            }
        }
        if (z) {
            this.mEntryManager.updateNotifications();
        }
        this.mPresenter.updateMediaMetaData(z, true);
    }

    public void clearCurrentMediaNotification() {
        this.mMediaNotificationKey = null;
        clearCurrentMediaNotificationSession();
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.print("    mMediaSessionManager=");
        printWriter.println(this.mMediaSessionManager);
        printWriter.print("    mMediaNotificationKey=");
        printWriter.println(this.mMediaNotificationKey);
        printWriter.print("    mMediaController=");
        printWriter.print(this.mMediaController);
        if (this.mMediaController != null) {
            printWriter.print(" state=" + this.mMediaController.getPlaybackState());
        }
        printWriter.println();
        printWriter.print("    mMediaMetadata=");
        printWriter.print(this.mMediaMetadata);
        if (this.mMediaMetadata != null) {
            printWriter.print(" title=" + ((Object) this.mMediaMetadata.getText("android.media.metadata.TITLE")));
        }
        printWriter.println();
    }

    public boolean isPlaybackActive(int i) {
        return (i == 1 || i == 7 || i == 0) ? false : true;
    }

    private boolean sameSessions(MediaController mediaController, MediaController mediaController2) {
        if (mediaController == mediaController2) {
            return true;
        }
        if (mediaController == null) {
            return false;
        }
        return mediaController.controlsSameSession(mediaController2);
    }

    private int getMediaControllerPlaybackState(MediaController mediaController) {
        PlaybackState playbackState;
        if (mediaController != null && (playbackState = mediaController.getPlaybackState()) != null) {
            return playbackState.getState();
        }
        return 0;
    }

    private boolean isMediaNotification(NotificationData.Entry entry) {
        return (entry.getExpandedContentView() == null || entry.getExpandedContentView().findViewById(R.id.fade_in) == null) ? false : true;
    }

    private void clearCurrentMediaNotificationSession() {
        this.mMediaMetadata = null;
        if (this.mMediaController != null) {
            this.mMediaController.unregisterCallback(this.mMediaListener);
        }
        this.mMediaController = null;
    }
}
