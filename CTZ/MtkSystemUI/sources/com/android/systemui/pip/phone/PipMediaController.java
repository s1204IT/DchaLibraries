package com.android.systemui.pip.phone;

import android.app.IActivityManager;
import android.app.PendingIntent;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.pip.phone.PipMediaController;
import com.android.systemui.statusbar.policy.UserInfoController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class PipMediaController {
    private final IActivityManager mActivityManager;
    private final Context mContext;
    private MediaController mMediaController;
    private final MediaSessionManager mMediaSessionManager;
    private RemoteAction mNextAction;
    private RemoteAction mPauseAction;
    private RemoteAction mPlayAction;
    private RemoteAction mPrevAction;
    private BroadcastReceiver mPlayPauseActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("com.android.systemui.pip.phone.PLAY")) {
                PipMediaController.this.mMediaController.getTransportControls().play();
                return;
            }
            if (action.equals("com.android.systemui.pip.phone.PAUSE")) {
                PipMediaController.this.mMediaController.getTransportControls().pause();
            } else if (action.equals("com.android.systemui.pip.phone.NEXT")) {
                PipMediaController.this.mMediaController.getTransportControls().skipToNext();
            } else if (action.equals("com.android.systemui.pip.phone.PREV")) {
                PipMediaController.this.mMediaController.getTransportControls().skipToPrevious();
            }
        }
    };
    private final MediaController.Callback mPlaybackChangedListener = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState playbackState) {
            PipMediaController.this.notifyActionsChanged();
        }
    };
    private final MediaSessionManager.OnActiveSessionsChangedListener mSessionsChangedListener = new MediaSessionManager.OnActiveSessionsChangedListener() {
        @Override
        public void onActiveSessionsChanged(List<MediaController> list) {
            PipMediaController.this.resolveActiveMediaController(list);
        }
    };
    private ArrayList<ActionListener> mListeners = new ArrayList<>();

    public interface ActionListener {
        void onMediaActionsChanged(List<RemoteAction> list);
    }

    public PipMediaController(Context context, IActivityManager iActivityManager) {
        this.mContext = context;
        this.mActivityManager = iActivityManager;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.android.systemui.pip.phone.PLAY");
        intentFilter.addAction("com.android.systemui.pip.phone.PAUSE");
        intentFilter.addAction("com.android.systemui.pip.phone.NEXT");
        intentFilter.addAction("com.android.systemui.pip.phone.PREV");
        this.mContext.registerReceiver(this.mPlayPauseActionReceiver, intentFilter);
        createMediaActions();
        this.mMediaSessionManager = (MediaSessionManager) context.getSystemService("media_session");
        ((UserInfoController) Dependency.get(UserInfoController.class)).addCallback(new UserInfoController.OnUserInfoChangedListener() {
            @Override
            public final void onUserInfoChanged(String str, Drawable drawable, String str2) {
                this.f$0.registerSessionListenerForCurrentUser();
            }
        });
    }

    public void onActivityPinned() {
        resolveActiveMediaController(this.mMediaSessionManager.getActiveSessionsForUser(null, -2));
    }

    public void addListener(ActionListener actionListener) {
        if (!this.mListeners.contains(actionListener)) {
            this.mListeners.add(actionListener);
            actionListener.onMediaActionsChanged(getMediaActions());
        }
    }

    public void removeListener(ActionListener actionListener) {
        actionListener.onMediaActionsChanged(Collections.EMPTY_LIST);
        this.mListeners.remove(actionListener);
    }

    private List<RemoteAction> getMediaActions() {
        if (this.mMediaController == null || this.mMediaController.getPlaybackState() == null) {
            return Collections.EMPTY_LIST;
        }
        ArrayList arrayList = new ArrayList();
        boolean zIsActiveState = MediaSession.isActiveState(this.mMediaController.getPlaybackState().getState());
        long actions = this.mMediaController.getPlaybackState().getActions();
        this.mPrevAction.setEnabled((16 & actions) != 0);
        arrayList.add(this.mPrevAction);
        if (!zIsActiveState && (4 & actions) != 0) {
            arrayList.add(this.mPlayAction);
        } else if (zIsActiveState && (2 & actions) != 0) {
            arrayList.add(this.mPauseAction);
        }
        this.mNextAction.setEnabled((actions & 32) != 0);
        arrayList.add(this.mNextAction);
        return arrayList;
    }

    private void createMediaActions() {
        String string = this.mContext.getString(R.string.pip_pause);
        this.mPauseAction = new RemoteAction(Icon.createWithResource(this.mContext, R.drawable.ic_pause_white), string, string, PendingIntent.getBroadcast(this.mContext, 0, new Intent("com.android.systemui.pip.phone.PAUSE"), 134217728));
        String string2 = this.mContext.getString(R.string.pip_play);
        this.mPlayAction = new RemoteAction(Icon.createWithResource(this.mContext, R.drawable.ic_play_arrow_white), string2, string2, PendingIntent.getBroadcast(this.mContext, 0, new Intent("com.android.systemui.pip.phone.PLAY"), 134217728));
        String string3 = this.mContext.getString(R.string.pip_skip_to_next);
        this.mNextAction = new RemoteAction(Icon.createWithResource(this.mContext, R.drawable.ic_skip_next_white), string3, string3, PendingIntent.getBroadcast(this.mContext, 0, new Intent("com.android.systemui.pip.phone.NEXT"), 134217728));
        String string4 = this.mContext.getString(R.string.pip_skip_to_prev);
        this.mPrevAction = new RemoteAction(Icon.createWithResource(this.mContext, R.drawable.ic_skip_previous_white), string4, string4, PendingIntent.getBroadcast(this.mContext, 0, new Intent("com.android.systemui.pip.phone.PREV"), 134217728));
    }

    public void registerSessionListenerForCurrentUser() {
        this.mMediaSessionManager.removeOnActiveSessionsChangedListener(this.mSessionsChangedListener);
        this.mMediaSessionManager.addOnActiveSessionsChangedListener(this.mSessionsChangedListener, null, -2, null);
    }

    public void resolveActiveMediaController(List<MediaController> list) {
        ComponentName componentName;
        if (list != null && (componentName = (ComponentName) PipUtils.getTopPinnedActivity(this.mContext, this.mActivityManager).first) != null) {
            for (int i = 0; i < list.size(); i++) {
                MediaController mediaController = list.get(i);
                if (mediaController.getPackageName().equals(componentName.getPackageName())) {
                    setActiveMediaController(mediaController);
                    return;
                }
            }
        }
        setActiveMediaController(null);
    }

    private void setActiveMediaController(MediaController mediaController) {
        if (mediaController != this.mMediaController) {
            if (this.mMediaController != null) {
                this.mMediaController.unregisterCallback(this.mPlaybackChangedListener);
            }
            this.mMediaController = mediaController;
            if (mediaController != null) {
                mediaController.registerCallback(this.mPlaybackChangedListener);
            }
            notifyActionsChanged();
        }
    }

    public void notifyActionsChanged() {
        if (!this.mListeners.isEmpty()) {
            final List<RemoteAction> mediaActions = getMediaActions();
            this.mListeners.forEach(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    ((PipMediaController.ActionListener) obj).onMediaActionsChanged(mediaActions);
                }
            });
        }
    }
}
