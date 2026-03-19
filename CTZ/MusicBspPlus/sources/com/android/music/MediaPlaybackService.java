package com.android.music;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.drm.DrmManagerClient;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.os.storage.StorageManager;
import android.provider.MediaStore;
import android.widget.RemoteViews;
import android.widget.Toast;
import com.android.music.IMediaPlaybackService;
import com.mediatek.bluetooth.avrcp.IBTAvrcpMusic;
import com.mediatek.bluetooth.avrcp.ServiceAvrcpStub;
import com.mediatek.omadrm.OmaDrmUtils;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.SoftReference;
import java.util.Locale;
import java.util.Random;
import java.util.Vector;

public class MediaPlaybackService extends Service {
    private static boolean mPermissionGranted = true;
    static boolean mTrackCompleted = false;
    private AudioManager mAudioManager;
    private Cursor mCursor;
    private String mFileToPlay;
    private MultiPlayer mPlayer;
    private SharedPreferences mPreferences;
    private RemoteControlClient mRemoteControlClient;
    private Toast mToast;
    private DrmManagerClient mDrmClient = null;
    private int mShuffleMode = 0;
    private int mRepeatMode = 0;
    private int mMediaMountedCount = 0;
    private long[] mAutoShuffleList = null;
    private long[] mPlayList = null;
    private int mPlayListLen = 0;
    private Vector<Integer> mHistory = new Vector<>(100);
    private int mPlayPos = -1;
    private int mNumErr = 0;
    private int mNextPlayPos = -1;
    private final Shuffler mRand = new Shuffler();
    private int mOpenFailedCounter = 0;
    String[] mCursorCols = {"audio._id AS _id", "artist", "album", "title", "_data", "mime_type", "album_id", "artist_id", "is_podcast", "bookmark", "duration"};
    private BroadcastReceiver mUnmountReceiver = null;
    private int mServiceStartId = -1;
    private boolean mServiceInUse = false;
    private boolean mIsSupposedToBePlaying = false;
    private boolean mQuietMode = false;
    private boolean mQueueIsSaveable = true;
    private boolean mPausedByTransientLossOfFocus = false;
    private MediaAppWidgetProvider mAppWidgetProvider = MediaAppWidgetProvider.getInstance();
    private boolean mIsPlayerReady = false;
    private boolean mDoSeekWhenPrepared = false;
    private boolean mMediaSeekable = true;
    private boolean mNextMediaSeekable = true;
    private boolean mIsPlaylistCompleted = false;
    private boolean mReceiverUnregistered = false;
    private boolean mIsPrev = false;
    private boolean mIsReloadSuccess = false;
    private long mPreAudioId = -1;
    private long mDurationOverride = -1;
    private long mSeekPositionForAnotherSong = 0;
    private int mAuxEffectId = 0;
    private boolean mWhetherAttachWhenPause = false;
    private boolean mLossesBTEnable = false;
    private boolean mNeedSeekCauseLossBT = false;
    public boolean mdurationReady = false;
    private long mPositonCausedByLossBT = 0;
    private StorageManager mStorageManager = null;
    private AlbumArtWorker mAsyncAlbumArtWorker = null;
    private Bitmap mAlbumArt = null;
    private String mEjectingCardPath = null;
    private float mCurrentVolume = 1.0f;
    private boolean mTaskRemoved = false;
    private boolean misAVRCPseeking = false;
    private boolean mPlayBeforeseek = false;
    HandlerThread mContentObserverThread = null;
    private Handler mContentObserverThreadHandler = null;
    private Handler mMediaplayerHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            MusicUtils.debugLog("mMediaplayerHandler.handleMessage " + message.what);
            int i = message.what;
            if (i == 1) {
                MusicLogUtils.v("MusicService", "track ended");
                if (MediaPlaybackService.this.mRepeatMode == 1 && MediaPlaybackService.this.mShuffleMode == 0) {
                    MediaPlaybackService.mTrackCompleted = false;
                    MediaPlaybackService.this.seek(0L);
                    if (MediaPlaybackService.this.isPlaying()) {
                        MediaPlaybackService.this.mPlayer.start();
                    }
                } else {
                    MediaPlaybackService.this.gotoNext(false);
                }
                MediaPlaybackService.this.notifyChange("com.android.music.playbackcomplete");
                return;
            }
            if (i != 101) {
                switch (i) {
                    case 3:
                        MusicLogUtils.v("MusicService", "SERVER_DIED: mPlayPos = " + MediaPlaybackService.this.mPlayPos);
                        MediaPlaybackService.this.sendSessionIdToAudioEffect(false);
                        if (!MediaPlaybackService.this.mIsSupposedToBePlaying) {
                            if (MediaPlaybackService.this.mPlayPos >= 0) {
                                MusicLogUtils.v("MusicService", "SERVER_DIED: -> openCurrentAndNext");
                                boolean z = MediaPlaybackService.this.mDoSeekWhenPrepared;
                                MediaPlaybackService.this.mQuietMode = true;
                                MediaPlaybackService.this.openCurrentAndNext();
                                MediaPlaybackService.this.mDoSeekWhenPrepared = z;
                                MusicLogUtils.v("MusicService", "SERVER_DIED: doseek restored to:" + MediaPlaybackService.this.mDoSeekWhenPrepared);
                                MusicLogUtils.v("MusicService", "SERVER_DIED: <- openCurrentAndNext");
                                return;
                            }
                            return;
                        }
                        MediaPlaybackService.this.gotoNext(true);
                        return;
                    case 4:
                        MusicLogUtils.v("MusicService", "AudioFocus: " + message.arg1);
                        int i2 = message.arg1;
                        if (i2 != 1) {
                            switch (i2) {
                                case -3:
                                    MediaPlaybackService.this.mMediaplayerHandler.removeMessages(6);
                                    MediaPlaybackService.this.mMediaplayerHandler.sendEmptyMessage(5);
                                    return;
                                case -2:
                                    if (MediaPlaybackService.this.isPlaying()) {
                                        MediaPlaybackService.this.mPausedByTransientLossOfFocus = true;
                                    }
                                    MediaPlaybackService.this.pause();
                                    if (MediaPlaybackService.this.mPausedByTransientLossOfFocus && !MediaPlaybackService.this.mTaskRemoved) {
                                        Notification notificationBuild = new Notification.Builder(MediaPlaybackService.this, "music_notification_channel").build();
                                        notificationBuild.icon = 0;
                                        notificationBuild.when = System.currentTimeMillis();
                                        notificationBuild.flags |= 2;
                                        MediaPlaybackService.this.startForeground(1, notificationBuild);
                                        return;
                                    }
                                    return;
                                case -1:
                                    if (MediaPlaybackService.this.isPlaying()) {
                                        MediaPlaybackService.this.mPausedByTransientLossOfFocus = false;
                                    }
                                    MediaPlaybackService.this.pause();
                                    return;
                                default:
                                    MusicLogUtils.v("MusicService", "Unknown audio focus change code");
                                    return;
                            }
                        }
                        MusicLogUtils.v("MusicService", "AudioFocus: received AUDIOFOCUS_GAIN");
                        if (MediaPlaybackService.this.isPlaying() || !MediaPlaybackService.this.mPausedByTransientLossOfFocus) {
                            MediaPlaybackService.this.mMediaplayerHandler.removeMessages(5);
                            MediaPlaybackService.this.mMediaplayerHandler.sendEmptyMessage(6);
                            return;
                        } else {
                            MediaPlaybackService.this.mPausedByTransientLossOfFocus = false;
                            MediaPlaybackService.this.mCurrentVolume = 0.0f;
                            MediaPlaybackService.this.mPlayer.setVolume(MediaPlaybackService.this.mCurrentVolume);
                            MediaPlaybackService.this.play();
                            return;
                        }
                    case 5:
                        MediaPlaybackService.access$124(MediaPlaybackService.this, 0.05f);
                        if (MediaPlaybackService.this.mCurrentVolume > 0.2f) {
                            MediaPlaybackService.this.mMediaplayerHandler.sendEmptyMessageDelayed(5, 10L);
                        } else {
                            MediaPlaybackService.this.mCurrentVolume = 0.2f;
                        }
                        MediaPlaybackService.this.mPlayer.setVolume(MediaPlaybackService.this.mCurrentVolume);
                        return;
                    case 6:
                        MediaPlaybackService.access$116(MediaPlaybackService.this, 0.05f);
                        if (MediaPlaybackService.this.mCurrentVolume < 1.0f) {
                            MediaPlaybackService.this.mMediaplayerHandler.sendEmptyMessageDelayed(6, 50L);
                        } else {
                            MediaPlaybackService.this.mCurrentVolume = 1.0f;
                        }
                        MediaPlaybackService.this.mPlayer.setVolume(MediaPlaybackService.this.mCurrentVolume);
                        return;
                    case 7:
                        MediaPlaybackService.this.mPlayPos = message.arg1;
                        synchronized (MediaPlaybackService.this) {
                            if (MediaPlaybackService.this.mCursor != null) {
                                MediaPlaybackService.this.mCursor.close();
                                MediaPlaybackService.this.mCursor = null;
                            }
                            break;
                        }
                        if (MediaPlaybackService.this.mPlayPos >= 0 && MediaPlaybackService.this.mPlayListLen > MediaPlaybackService.this.mPlayPos) {
                            MediaPlaybackService.this.mCursor = MediaPlaybackService.this.getCursorForId(MediaPlaybackService.this.mPlayList[MediaPlaybackService.this.mPlayPos]);
                            if (MediaPlaybackService.this.mCursor == null) {
                                MusicLogUtils.v("MusicService", "switch to next song fail because mCursor is null");
                                return;
                            }
                            MusicLogUtils.v("MusicService", "switch to next song");
                            MediaPlaybackService.this.mMediaSeekable = MediaPlaybackService.this.mNextMediaSeekable;
                            MediaPlaybackService.this.mNextMediaSeekable = true;
                            MediaPlaybackService.this.notifyChange("com.android.music.metachanged");
                            MediaPlaybackService.this.updateNotification(MediaPlaybackService.this, null);
                            if (MediaPlaybackService.this.mPreAudioId != MediaPlaybackService.this.getAudioId()) {
                                MediaPlaybackService.this.mAsyncAlbumArtWorker = new AlbumArtWorker();
                                MediaPlaybackService.this.mAsyncAlbumArtWorker.execute(Long.valueOf(MediaPlaybackService.this.getAlbumId()));
                            }
                            MediaPlaybackService.this.setNextTrack();
                            return;
                        }
                        return;
                    case 8:
                        MediaPlaybackService.this.showToast(MediaPlaybackService.this.getString(R.string.playback_failed));
                        MediaPlaybackService.this.notifyChange("com.android.music.quitplayback");
                        return;
                    default:
                        switch (i) {
                            case 10:
                                int i3 = message.arg1;
                                MusicLogUtils.v("MusicService", "received messsage : REMOVE_TRACK, songId = " + i3);
                                MediaPlaybackService.this.removeTrack((long) i3);
                                return;
                            case 11:
                                MediaPlaybackService.this.stopForeground(true);
                                MediaPlaybackService.this.mTaskRemoved = false;
                                return;
                            default:
                                return;
                        }
                }
            }
            MediaPlaybackService.this.handleSettingModeChange(message.arg1, message.arg2);
        }
    };
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (MediaPlaybackService.this.mReceiverUnregistered) {
                return;
            }
            String action = intent.getAction();
            String stringExtra = intent.getStringExtra("command");
            MusicUtils.debugLog("mIntentReceiver.onReceive " + action + " / " + stringExtra);
            MusicLogUtils.v("MusicService", "mIntentReceiver.onReceive: " + action + "/" + stringExtra);
            if ("android.intent.action.ACTION_SHUTDOWN".equals(action) || "android.intent.action.ACTION_SHUTDOWN_IPO".equals(action)) {
                MediaPlaybackService.this.getApplicationContext().getContentResolver().unregisterContentObserver(MediaPlaybackService.this.mContentObserver);
                if (MediaPlaybackService.this.mUnmountReceiver != null) {
                    MediaPlaybackService.this.unregisterReceiver(MediaPlaybackService.this.mUnmountReceiver);
                    MediaPlaybackService.this.mUnmountReceiver = null;
                }
                MediaPlaybackService.this.saveQueue(true);
                MediaPlaybackService.this.pause();
                if (MediaPlaybackService.this.mPlayer.isInitialized()) {
                    MediaPlaybackService.this.mPlayer.setNextDataSource(null);
                }
                MediaPlaybackService.this.stop();
                MediaPlaybackService.this.stopSelf(MediaPlaybackService.this.mServiceStartId);
                return;
            }
            if ("next".equals(stringExtra) || "com.android.music.musicservicecommand.next".equals(action)) {
                boolean zHasMountedSDcard = MusicUtils.hasMountedSDcard(MediaPlaybackService.this.getApplicationContext());
                MusicLogUtils.v("MusicService", "mIntentReceiver.onReceive hasCard = " + zHasMountedSDcard);
                if (!zHasMountedSDcard) {
                    MediaPlaybackService.this.notifyChange("com.android.music.quitplayback");
                    return;
                } else {
                    MediaPlaybackService.this.gotoNext(true);
                    return;
                }
            }
            if ("previous".equals(stringExtra) || "com.android.music.musicservicecommand.previous".equals(action)) {
                MediaPlaybackService.this.prev();
                return;
            }
            if ("togglepause".equals(stringExtra) || "com.android.music.musicservicecommand.togglepause".equals(action)) {
                if (MediaPlaybackService.this.isPlaying()) {
                    MediaPlaybackService.this.pause();
                    MediaPlaybackService.this.mPausedByTransientLossOfFocus = false;
                    return;
                } else {
                    MediaPlaybackService.this.play();
                    return;
                }
            }
            if ("pause".equals(stringExtra) || "com.android.music.musicservicecommand.pause".equals(action) || "android.media.AUDIO_BECOMING_NOISY".equals(action)) {
                MediaPlaybackService.this.pause();
                MediaPlaybackService.this.mPausedByTransientLossOfFocus = false;
                return;
            }
            if ("play".equals(stringExtra)) {
                MediaPlaybackService.this.play();
                return;
            }
            if ("stop".equals(stringExtra)) {
                MediaPlaybackService.this.pause();
                MediaPlaybackService.this.mPausedByTransientLossOfFocus = false;
                MediaPlaybackService.this.seek(0L);
                return;
            }
            if ("appwidgetupdate".equals(stringExtra)) {
                MediaPlaybackService.this.mAppWidgetProvider.performUpdate(MediaPlaybackService.this, intent.getIntArrayExtra("appWidgetIds"));
                return;
            }
            if ("com.android.music.attachauxaudioeffect".equals(action)) {
                MediaPlaybackService.this.mAuxEffectId = intent.getIntExtra("auxaudioeffectid", 0);
                MusicLogUtils.v("MusicService", "ATTACH_AUX_AUDIO_EFFECT with EffectId = " + MediaPlaybackService.this.mAuxEffectId);
                if (MediaPlaybackService.this.mPlayer != null && MediaPlaybackService.this.mPlayer.isInitialized() && MediaPlaybackService.this.mAuxEffectId > 0) {
                    if (MediaPlaybackService.this.mPlayer.isPlaying()) {
                        MediaPlaybackService.this.mPlayer.attachAuxEffect(MediaPlaybackService.this.mAuxEffectId);
                        MediaPlaybackService.this.mPlayer.setAuxEffectSendLevel(1.0f);
                        MediaPlaybackService.this.mWhetherAttachWhenPause = false;
                        return;
                    } else {
                        MediaPlaybackService.this.mWhetherAttachWhenPause = true;
                        MusicLogUtils.v("MusicService", "Need attach reverb effect when play music again!");
                        return;
                    }
                }
                return;
            }
            if ("com.android.music.detachauxaudioeffect".equals(action)) {
                int intExtra = intent.getIntExtra("auxaudioeffectid", 0);
                MusicLogUtils.v("MusicService", "DETACH_AUX_AUDIO_EFFECT with EffectId = " + intExtra);
                if (MediaPlaybackService.this.mAuxEffectId == intExtra) {
                    MediaPlaybackService.this.mAuxEffectId = 0;
                    if (MediaPlaybackService.this.mPlayer != null && MediaPlaybackService.this.mPlayer.isInitialized()) {
                        if (MediaPlaybackService.this.mPlayer.isPlaying()) {
                            MediaPlaybackService.this.mPlayer.attachAuxEffect(0);
                            MediaPlaybackService.this.mWhetherAttachWhenPause = false;
                            return;
                        } else {
                            MediaPlaybackService.this.mWhetherAttachWhenPause = true;
                            MusicLogUtils.v("MusicService", "Need detach reverb effect when play music again!");
                            return;
                        }
                    }
                    return;
                }
                return;
            }
            if ("com.android.lossessbt.enable".equals(action)) {
                MusicLogUtils.v("MusicService", "ACTION_LOSSESSBT_ENABLE!");
                MediaPlaybackService.this.mLossesBTEnable = true;
                if (MediaPlaybackService.this.mPlayer != null && MediaPlaybackService.this.mPlayer.isInitialized()) {
                    MediaPlaybackService.this.mPlayer.setNextDataSource(null);
                }
                MediaPlaybackService.this.mNeedSeekCauseLossBT = true;
                if (MediaPlaybackService.this.position() != -1) {
                    MediaPlaybackService.this.mPositonCausedByLossBT = MediaPlaybackService.this.position();
                }
                MediaPlaybackService.this.mPositonCausedByLossBT = MediaPlaybackService.this.position();
                if (!MediaPlaybackService.this.isPlaying()) {
                    MediaPlaybackService.this.mQuietMode = true;
                }
                MediaPlaybackService.this.openCurrentAndNext();
                return;
            }
            if ("com.android.lossessbt.disable".equals(action)) {
                MusicLogUtils.v("MusicService", "ACTION_LOSSESSBT_DISABLE!");
                MediaPlaybackService.this.mLossesBTEnable = false;
                SystemClock.sleep(100L);
                MusicLogUtils.v("MusicService", "ACTION_LOSSESSBT_DISABLE,sleep 100");
                MediaPlaybackService.this.mNeedSeekCauseLossBT = true;
                if (MediaPlaybackService.this.position() != -1) {
                    MediaPlaybackService.this.mPositonCausedByLossBT = MediaPlaybackService.this.position();
                }
                if (!MediaPlaybackService.this.isPlaying()) {
                    MediaPlaybackService.this.mQuietMode = true;
                }
                MediaPlaybackService.this.openCurrentAndNext();
            }
        }
    };
    private AudioManager.OnAudioFocusChangeListener mAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int i) {
            MediaPlaybackService.this.mMediaplayerHandler.obtainMessage(4, i, 0).sendToTarget();
        }
    };
    private final ContentObserver mContentObserver = new ContentObserver(this.mContentObserverThreadHandler) {
        @Override
        public void onChange(boolean z) {
            MusicLogUtils.v("MusicService", "onChange");
            super.onChange(z);
            if (MediaPlaybackService.this.mPlayPos >= 0 && MediaPlaybackService.this.mPlayPos <= MediaPlaybackService.this.mPlayList.length - 1) {
                long j = MediaPlaybackService.this.mPlayList[MediaPlaybackService.this.mPlayPos];
                Cursor cursorQuery = MusicUtils.query(MediaPlaybackService.this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{"_id"}, "_id=" + j, null, null);
                if (cursorQuery == null) {
                    MusicLogUtils.v("MusicService", "mContentObserver: cursor is null, db not ready!");
                    return;
                }
                MusicLogUtils.v("MusicService", "mContentObserver: cursor count is " + cursorQuery.getCount());
                if (cursorQuery.getCount() == 0) {
                    Message messageObtainMessage = MediaPlaybackService.this.mMediaplayerHandler.obtainMessage(10);
                    messageObtainMessage.arg1 = (int) j;
                    MediaPlaybackService.this.mMediaplayerHandler.sendMessage(messageObtainMessage);
                }
                cursorQuery.close();
            }
        }
    };
    private final Handler msHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            if (message.what == 2) {
                MediaPlaybackService.this.misAVRCPseeking = false;
                MediaPlaybackService.this.endscan();
            }
        }
    };
    private final char[] hexdigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private Handler mDelayedStopHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            if (!MediaPlaybackService.this.isPlaying() && !MediaPlaybackService.this.mPausedByTransientLossOfFocus && !MediaPlaybackService.this.mServiceInUse && !MusicUtils.hasBoundClient() && !MediaPlaybackService.this.mMediaplayerHandler.hasMessages(1)) {
                MediaPlaybackService.this.saveQueue(true);
                MediaPlaybackService.this.stopSelf(MediaPlaybackService.this.mServiceStartId);
            }
        }
    };
    private final IBinder mBinder = new ServiceStub(this);
    private final ServiceAvrcpStub mBinderAvrcp = new ServiceAvrcpStub(this);

    static float access$116(MediaPlaybackService mediaPlaybackService, float f) {
        float f2 = mediaPlaybackService.mCurrentVolume + f;
        mediaPlaybackService.mCurrentVolume = f2;
        return f2;
    }

    static float access$124(MediaPlaybackService mediaPlaybackService, float f) {
        float f2 = mediaPlaybackService.mCurrentVolume - f;
        mediaPlaybackService.mCurrentVolume = f2;
        return f2;
    }

    static int access$4908(MediaPlaybackService mediaPlaybackService) {
        int i = mediaPlaybackService.mMediaMountedCount;
        mediaPlaybackService.mMediaMountedCount = i + 1;
        return i;
    }

    static int access$4910(MediaPlaybackService mediaPlaybackService) {
        int i = mediaPlaybackService.mMediaMountedCount;
        mediaPlaybackService.mMediaMountedCount = i - 1;
        return i;
    }

    static int access$5908(MediaPlaybackService mediaPlaybackService) {
        int i = mediaPlaybackService.mNumErr;
        mediaPlaybackService.mNumErr = i + 1;
        return i;
    }

    static int access$6108(MediaPlaybackService mediaPlaybackService) {
        int i = mediaPlaybackService.mOpenFailedCounter;
        mediaPlaybackService.mOpenFailedCounter = i + 1;
        return i;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        MusicLogUtils.v("MusicService", ">> onCreate");
        if (getApplicationContext().checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") != 0) {
            if (this.mAsyncAlbumArtWorker != null) {
                this.mAsyncAlbumArtWorker.cancel(true);
            }
            mPermissionGranted = false;
            stopSelf();
            if (MediaAppWidgetProvider.isAppWidget) {
                MediaAppWidgetProvider.isAppWidget = false;
                showToast(getString(R.string.music_storage_permission_deny));
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Process.killProcess(Process.myPid());
                    }
                }, 2000L);
                return;
            }
            Process.killProcess(Process.myPid());
            return;
        }
        stopForeground(true);
        this.mAudioManager = (AudioManager) getSystemService("audio");
        ComponentName componentName = new ComponentName(getPackageName(), MediaButtonIntentReceiver.class.getName());
        this.mAudioManager.registerMediaButtonEventReceiver(componentName);
        if (OmaDrmUtils.isOmaDrmEnabled()) {
            this.mDrmClient = new DrmManagerClient(this);
        }
        Intent intent = new Intent("android.intent.action.MEDIA_BUTTON");
        intent.setComponent(componentName);
        this.mRemoteControlClient = new RemoteControlClient(PendingIntent.getBroadcast(this, 0, intent, 0));
        this.mAudioManager.registerRemoteControlClient(this.mRemoteControlClient);
        this.mRemoteControlClient.setTransportControlFlags(445);
        this.mRemoteControlClient.setPlaybackPositionUpdateListener(new RemoteControlClient.OnPlaybackPositionUpdateListener() {
            @Override
            public void onPlaybackPositionUpdate(long j) {
                if (!MediaPlaybackService.this.misAVRCPseeking) {
                    MediaPlaybackService.this.mPlayBeforeseek = MediaPlaybackService.this.isPlaying();
                }
                MediaPlaybackService.this.misAVRCPseeking = true;
                MusicLogUtils.v("MusicService", "seek true");
                long jPosition = MediaPlaybackService.this.mPlayer.position();
                MusicLogUtils.v("MusicService", "cur_pos" + jPosition + " newpos  " + j);
                if (MediaPlaybackService.this.mPlayer.isInitialized() && MediaPlaybackService.this.mIsPlayerReady) {
                    if (j != 0 && (!MediaPlaybackService.this.mMediaSeekable || !MediaPlaybackService.this.mediaCanSeek())) {
                        MusicLogUtils.v("MusicService", "seek, sorry, seek is not supported");
                        return;
                    }
                    long jDuration = MediaPlaybackService.this.duration();
                    MusicLogUtils.v("MusicService", "duration: " + jDuration);
                    long j2 = jDuration / 8;
                    if (jPosition - j > j2) {
                        j = jPosition - j2;
                    }
                    if (j <= 0) {
                        MediaPlaybackService.this.prev();
                        long jDuration2 = MediaPlaybackService.this.duration();
                        if (jDuration2 > 2500) {
                            j = jDuration2 - 2500;
                            jPosition = 500 + j;
                        } else {
                            j = 0;
                        }
                        if (j > 0) {
                            MediaPlaybackService.this.mDoSeekWhenPrepared = true;
                            MediaPlaybackService.this.mSeekPositionForAnotherSong = j;
                        }
                    }
                    MusicLogUtils.v("MusicService", "seek curr(" + jPosition + ")");
                    long jDuration3 = MediaPlaybackService.this.duration();
                    if (j < jDuration3) {
                        MediaPlaybackService.this.mIsPlaylistCompleted = false;
                    } else {
                        MediaPlaybackService.this.gotoNext(true);
                        j = jDuration3;
                    }
                    if (j <= jDuration3) {
                        MediaPlaybackService.this.mPlayer.seek(j);
                        if (j <= jDuration3) {
                            MediaPlaybackService.this.mRemoteControlClient.setPlaybackState(jPosition <= j ? 4 : 5, j, 1.0f);
                        }
                        if (j >= jDuration3) {
                            boolean unused = MediaPlaybackService.this.mPlayBeforeseek;
                        }
                    }
                }
                Message messageObtainMessage = MediaPlaybackService.this.msHandler.obtainMessage(2);
                MediaPlaybackService.this.msHandler.removeMessages(2);
                MediaPlaybackService.this.msHandler.sendMessageDelayed(messageObtainMessage, 1000L);
            }
        });
        this.mPreferences = getSharedPreferences("Music", 0);
        this.mStorageManager = (StorageManager) getSystemService("storage");
        MusicLogUtils.v("MusicService", "onCreate: hasCard = " + MusicUtils.hasMountedSDcard(getApplicationContext()));
        registerExternalStorageListener();
        this.mPlayer = new MultiPlayer();
        this.mPlayer.setHandler(this.mMediaplayerHandler);
        sendSessionIdToAudioEffect(false);
        reloadQueue();
        notifyChange("com.android.music.queuechanged");
        notifyChange("com.android.music.metachanged");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.android.music.musicservicecommand");
        intentFilter.addAction("com.android.music.musicservicecommand.togglepause");
        intentFilter.addAction("com.android.music.musicservicecommand.pause");
        intentFilter.addAction("com.android.music.musicservicecommand.next");
        intentFilter.addAction("com.android.music.musicservicecommand.previous");
        intentFilter.addAction("android.intent.action.ACTION_SHUTDOWN");
        intentFilter.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
        intentFilter.addAction("com.android.music.attachauxaudioeffect");
        intentFilter.addAction("com.android.music.detachauxaudioeffect");
        intentFilter.addAction("android.media.AUDIO_BECOMING_NOISY");
        intentFilter.addAction("com.android.lossessbt.enable");
        intentFilter.addAction("com.android.lossessbt.disable");
        registerReceiver(this.mIntentReceiver, intentFilter);
        this.mContentObserverThread = new HandlerThread("ListenToContentObserver");
        this.mContentObserverThread.start();
        this.mContentObserverThreadHandler = new Handler(this.mContentObserverThread.getLooper()) {
        };
        getContentResolver().registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, false, this.mContentObserver);
        this.mDelayedStopHandler.sendMessageDelayed(this.mDelayedStopHandler.obtainMessage(), 60000L);
        String parameters = this.mAudioManager.getParameters("LosslessBT_Working");
        MusicLogUtils.v("MusicService", "onCreate,btStatus:" + parameters);
        if (parameters.equals("yes")) {
            this.mLossesBTEnable = true;
        }
        MusicLogUtils.v("MusicService", "<< onCreate");
    }

    @Override
    public void onDestroy() {
        MusicLogUtils.v("MusicService", ">> onDestroy");
        if (mPermissionGranted) {
            if (isPlaying()) {
                MusicLogUtils.v("MusicService", "Service being destroyed while still playing.");
                pause();
            }
            this.mDurationOverride = -1L;
            if (this.mAsyncAlbumArtWorker != null) {
                this.mAsyncAlbumArtWorker.cancel(true);
            }
            MusicUtils.resetStaticService();
            Intent intent = new Intent("android.media.action.CLOSE_AUDIO_EFFECT_CONTROL_SESSION");
            intent.putExtra("android.media.extra.AUDIO_SESSION", getAudioSessionId());
            intent.putExtra("android.media.extra.PACKAGE_NAME", getPackageName());
            sendBroadcast(intent);
            this.mPlayer.release();
            this.mPlayer = null;
            this.mAudioManager.abandonAudioFocus(this.mAudioFocusListener);
            this.mAudioManager.unregisterRemoteControlClient(this.mRemoteControlClient);
            this.mDelayedStopHandler.removeCallbacksAndMessages(null);
            this.mMediaplayerHandler.removeCallbacksAndMessages(null);
            synchronized (this) {
                if (this.mCursor != null) {
                    this.mCursor.close();
                    this.mCursor = null;
                }
            }
            unregisterReceiver(this.mIntentReceiver);
            if (this.mUnmountReceiver != null) {
                unregisterReceiver(this.mUnmountReceiver);
                this.mUnmountReceiver = null;
            }
            this.mReceiverUnregistered = true;
            getContentResolver().unregisterContentObserver(this.mContentObserver);
            quitContentObserverThread();
        }
        MusicLogUtils.v("MusicService", "<< onDestroy");
        if (this.mDrmClient != null) {
            this.mDrmClient.release();
            this.mDrmClient = null;
        }
        super.onDestroy();
    }

    private void saveQueue(boolean z) {
        MusicLogUtils.v("MusicService", "saveQueue(" + z + ")");
        if (!this.mQueueIsSaveable) {
            MusicLogUtils.v("MusicService", "saveQueue: queue NOT savable!!");
            return;
        }
        SharedPreferences.Editor editorEdit = this.mPreferences.edit();
        if (z) {
            StringBuilder sb = new StringBuilder();
            int i = this.mPlayListLen;
            for (int i2 = 0; i2 < i; i2++) {
                long j = this.mPlayList[i2];
                if (j >= 0) {
                    if (j == 0) {
                        sb.append("0;");
                    } else {
                        while (j != 0) {
                            int i3 = (int) (15 & j);
                            j >>>= 4;
                            sb.append(this.hexdigits[i3]);
                        }
                        sb.append(";");
                    }
                }
            }
            editorEdit.putString("queue", sb.toString());
            MusicLogUtils.v("MusicService", "saveQueue: queue=" + sb.toString());
            if (this.mShuffleMode != 0) {
                int size = this.mHistory.size();
                sb.setLength(0);
                for (int i4 = 0; i4 < size; i4++) {
                    int iIntValue = this.mHistory.get(i4).intValue();
                    if (iIntValue == 0) {
                        sb.append("0;");
                    } else {
                        while (iIntValue != 0) {
                            int i5 = iIntValue & 15;
                            iIntValue >>>= 4;
                            sb.append(this.hexdigits[i5]);
                        }
                        sb.append(";");
                    }
                }
                editorEdit.putString("history", sb.toString());
            }
        }
        editorEdit.putInt("curpos", this.mPlayPos);
        MusicLogUtils.v("MusicService", "saveQueue: mPlayPos=" + this.mPlayPos + ", mPlayListLen=" + this.mPlayListLen);
        if (this.mPlayer.isInitialized() && this.mIsPlayerReady) {
            editorEdit.putLong("seekpos", this.mPlayer.position());
        }
        editorEdit.putInt("repeatmode", this.mRepeatMode);
        editorEdit.putInt("shufflemode", this.mShuffleMode);
        SharedPreferencesCompat.apply(editorEdit);
    }

    private void reloadQueue() {
        MusicLogUtils.v("MusicService", "reloadQueue");
        boolean zHasMountedSDcard = MusicUtils.hasMountedSDcard(getApplicationContext());
        MusicLogUtils.v("MusicService", "reloadQueue: hasCard = " + zHasMountedSDcard);
        if (!zHasMountedSDcard) {
            MusicLogUtils.v("MusicService", "reloadQueue: no sd card!");
            return;
        }
        String string = this.mPreferences.getString("queue", "");
        int length = string != null ? string.length() : 0;
        if (length > 1) {
            int i = 0;
            int i2 = 0;
            int i3 = 0;
            for (int i4 = 0; i4 < length; i4++) {
                char cCharAt = string.charAt(i4);
                if (cCharAt == ';') {
                    int i5 = i + 1;
                    ensurePlayListCapacity(i5);
                    this.mPlayList[i] = i2;
                    i2 = 0;
                    i = i5;
                    i3 = 0;
                } else {
                    if (cCharAt >= '0' && cCharAt <= '9') {
                        i2 += (cCharAt - '0') << i3;
                    } else if (cCharAt >= 'a' && cCharAt <= 'f') {
                        i2 += (('\n' + cCharAt) - 97) << i3;
                    } else {
                        i = 0;
                        break;
                    }
                    i3 += 4;
                }
            }
            this.mPlayListLen = i;
            int i6 = this.mPreferences.getInt("curpos", 0);
            MusicLogUtils.v("MusicService", "reloadQueue: mPlayListLen=" + this.mPlayListLen + ", curpos=" + i6);
            if (i6 < 0 || i6 >= this.mPlayListLen) {
                this.mPlayListLen = 0;
                return;
            }
            this.mPlayPos = i6;
            MusicLogUtils.v("MusicService", "reloadQueue: mPlayPos=" + i6);
            this.mOpenFailedCounter = 20;
            Cursor cursorForId = getCursorForId(this.mPlayList[this.mPlayPos]);
            if (cursorForId != null) {
                if (OmaDrmUtils.isOmaDrmEnabled()) {
                    checkDrmWhenOpenTrack(cursorForId);
                } else {
                    this.mQuietMode = true;
                    openCurrentAndNext();
                    this.mIsReloadSuccess = true;
                }
                cursorForId.close();
            }
            if (!this.mPlayer.isInitialized()) {
                MusicLogUtils.v("MusicService", "reloadQueue: open failed! not inited!");
                this.mPlayListLen = 0;
                this.mPlayPos = -1;
                return;
            }
            this.mDoSeekWhenPrepared = true;
            int i7 = this.mPreferences.getInt("repeatmode", 0);
            if (i7 != 2 && i7 != 1) {
                i7 = 0;
            }
            this.mRepeatMode = i7;
            int i8 = this.mPreferences.getInt("shufflemode", 0);
            if (i8 != 2 && i8 != 1) {
                i8 = 0;
            }
            if (i8 != 0) {
                String string2 = this.mPreferences.getString("history", "");
                int length2 = string2 != null ? string2.length() : 0;
                if (length2 > 1) {
                    this.mHistory.clear();
                    int i9 = 0;
                    int i10 = 0;
                    int i11 = 0;
                    while (true) {
                        if (i9 >= length2) {
                            break;
                        }
                        char cCharAt2 = string2.charAt(i9);
                        if (cCharAt2 == ';') {
                            if (i10 >= this.mPlayListLen) {
                                this.mHistory.clear();
                                break;
                            }
                            this.mHistory.add(Integer.valueOf(i10));
                            i10 = 0;
                            i11 = 0;
                            i9++;
                        } else {
                            if (cCharAt2 >= '0' && cCharAt2 <= '9') {
                                i10 += (cCharAt2 - '0') << i11;
                            } else if (cCharAt2 < 'a' || cCharAt2 > 'f') {
                                break;
                            } else {
                                i10 += ((cCharAt2 + '\n') - 97) << i11;
                            }
                            i11 += 4;
                            i9++;
                        }
                    }
                }
            }
            if (i8 == 2 && !makeAutoShuffleList()) {
                i8 = 0;
            }
            this.mShuffleMode = i8;
            return;
        }
        this.mPlayListLen = 0;
    }

    @Override
    public IBinder onBind(Intent intent) {
        MusicLogUtils.v("MediaPlaybackService", String.format("intent %s stubname:%s", intent.getAction(), ServiceAvrcpStub.class.getName()));
        if (IBTAvrcpMusic.class.getName().equals(intent.getAction())) {
            MusicLogUtils.v("MISC_AVRCP", "MediaPlayer returns IBTAvrcpMusic");
            return this.mBinderAvrcp;
        }
        if ("com.android.music.IMediaPlaybackService".equals(intent.getAction())) {
            MusicLogUtils.v("MISC_AVRCP", "MediaPlayer returns ServiceAvrcp inetrface");
            return this.mBinderAvrcp;
        }
        this.mDelayedStopHandler.removeCallbacksAndMessages(null);
        this.mServiceInUse = true;
        return this.mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        this.mDelayedStopHandler.removeCallbacksAndMessages(null);
        this.mServiceInUse = true;
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        this.mServiceStartId = i2;
        this.mDelayedStopHandler.removeCallbacksAndMessages(null);
        if (intent != null && !isEventFromMonkey()) {
            String action = intent.getAction();
            String stringExtra = intent.getStringExtra("command");
            MusicUtils.debugLog("onStartCommand " + action + " / " + stringExtra);
            MusicLogUtils.v("MusicService", "onStartCommand: " + action + "/" + stringExtra);
            if ("next".equals(stringExtra) || "com.android.music.musicservicecommand.next".equals(action)) {
                boolean zHasMountedSDcard = MusicUtils.hasMountedSDcard(getApplicationContext());
                MusicLogUtils.v("MusicService", "onStartCommand hasCard = " + zHasMountedSDcard);
                if (zHasMountedSDcard) {
                    this.mQuietMode = false;
                    gotoNext(true);
                } else {
                    notifyChange("com.android.music.quitplayback");
                }
            } else if ("previous".equals(stringExtra) || "com.android.music.musicservicecommand.previous".equals(action)) {
                this.mQuietMode = false;
                prev();
            } else if ("togglepause".equals(stringExtra) || "com.android.music.musicservicecommand.togglepause".equals(action)) {
                if (!this.misAVRCPseeking) {
                    if (isPlaying()) {
                        pause();
                        this.mPausedByTransientLossOfFocus = false;
                    } else {
                        play();
                        if (!isPlaying()) {
                            this.mQuietMode = false;
                        }
                    }
                }
            } else if ("pause".equals(stringExtra) || "com.android.music.musicservicecommand.pause".equals(action)) {
                pause();
                this.mPausedByTransientLossOfFocus = false;
            } else if ("play".equals(stringExtra)) {
                if (!this.misAVRCPseeking) {
                    play();
                }
            } else if ("stop".equals(stringExtra)) {
                pause();
                this.mPausedByTransientLossOfFocus = false;
                seek(0L);
            } else if ("forward".equals(stringExtra)) {
                scanForward(intent.getLongExtra("deltatime", 0L));
            } else if ("rewind".equals(stringExtra)) {
                scanBackward(intent.getLongExtra("deltatime", 0L));
            } else if ("appwidgetupdate".equals(stringExtra)) {
                this.mAppWidgetProvider.performUpdate(this, intent.getIntArrayExtra("appWidgetIds"));
            } else if ("endscan".equals(stringExtra)) {
                endscan();
            }
        }
        this.mDelayedStopHandler.removeCallbacksAndMessages(null);
        this.mDelayedStopHandler.sendMessageDelayed(this.mDelayedStopHandler.obtainMessage(), 60000L);
        return 1;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        this.mServiceInUse = false;
        saveQueue(true);
        if (isPlaying() || this.mPausedByTransientLossOfFocus || MusicUtils.hasBoundClient()) {
            return true;
        }
        if (this.mPlayListLen > 0 || this.mMediaplayerHandler.hasMessages(1)) {
            this.mDelayedStopHandler.sendMessageDelayed(this.mDelayedStopHandler.obtainMessage(), 60000L);
            return true;
        }
        stopSelf(this.mServiceStartId);
        return true;
    }

    public void closeExternalStorageFiles(String str) {
        StringBuilder sb = new StringBuilder();
        sb.append("closeExternalStorageFiles() storagePath = ");
        if (str == null) {
            str = "";
        }
        sb.append(str);
        MusicLogUtils.v("MusicService", sb.toString());
        stop(true);
        notifyChange("com.android.music.queuechanged");
        notifyChange("com.android.music.metachanged");
        notifyChange("com.android.music.playstatechanged");
    }

    public void registerExternalStorageListener() {
        if (this.mUnmountReceiver == null) {
            this.mUnmountReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String string;
                    if (MediaPlaybackService.this.mReceiverUnregistered) {
                        return;
                    }
                    String action = intent.getAction();
                    if (action.equals("android.intent.action.MEDIA_EJECT")) {
                        MusicLogUtils.v("MusicService", "MEDIA_EJECT");
                        MediaPlaybackService.this.mEjectingCardPath = intent.getData().getPath();
                        MediaPlaybackService.access$4910(MediaPlaybackService.this);
                        MusicLogUtils.v("MusicService", "ejected card path=" + MediaPlaybackService.this.mEjectingCardPath);
                        if (MediaPlaybackService.this.mPlayer.isInitialized()) {
                            MediaPlaybackService.this.mPlayer.setNextDataSource(null);
                        }
                        if (MediaPlaybackService.this.mEjectingCardPath.equals(Environment.getExternalStorageDirectory().getPath())) {
                            MediaPlaybackService.this.saveQueue(true);
                            MediaPlaybackService.this.mQueueIsSaveable = false;
                            MusicLogUtils.v("MusicService", "card eject");
                        }
                        if (MediaPlaybackService.this.mCursor != null && !MediaPlaybackService.this.mCursor.isAfterLast() && (string = MediaPlaybackService.this.mCursor.getString(MediaPlaybackService.this.mCursor.getColumnIndexOrThrow("_data"))) != null && string.contains(MediaPlaybackService.this.mEjectingCardPath)) {
                            MusicLogUtils.v("MusicService", "MEDIA_EJECT: current track on an unmounting external card");
                            MediaPlaybackService.this.closeExternalStorageFiles(MediaPlaybackService.this.mEjectingCardPath);
                        }
                        MediaPlaybackService.this.mEjectingCardPath = null;
                        return;
                    }
                    if (!action.equals("android.intent.action.MEDIA_MOUNTED") || MediaPlaybackService.this.mMediaMountedCount >= 0) {
                        if (action.equals("android.intent.action.MEDIA_SCANNER_FINISHED")) {
                            MediaPlaybackService.this.reloadQueueAfterScan();
                            return;
                        }
                        return;
                    }
                    MusicLogUtils.v("MusicService", "MEDIA_MOUNTED");
                    MediaPlaybackService.access$4908(MediaPlaybackService.this);
                    String path = intent.getData().getPath();
                    MusicLogUtils.v("MusicService", "mounted card path=" + path);
                    if (path.equals(Environment.getExternalStorageDirectory().getPath())) {
                        MusicLogUtils.v("MusicService", "card mounted");
                        MediaPlaybackService.this.reloadQueue();
                        MediaPlaybackService.this.mQueueIsSaveable = true;
                        MediaPlaybackService.this.notifyChange("com.android.music.queuechanged");
                        MediaPlaybackService.this.notifyChange("com.android.music.metachanged");
                    }
                }
            };
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.MEDIA_EJECT");
            intentFilter.addAction("android.intent.action.MEDIA_MOUNTED");
            intentFilter.addAction("android.intent.action.MEDIA_SCANNER_FINISHED");
            intentFilter.addDataScheme("file");
            registerReceiver(this.mUnmountReceiver, intentFilter);
        }
    }

    private void notifyChange(String str) {
        MusicLogUtils.v("MusicService", "notifyChange(" + str + ")");
        Intent intent = new Intent(str);
        intent.putExtra("id", Long.valueOf(getAudioId()));
        intent.putExtra("artist", getArtistName());
        intent.putExtra("album", getAlbumName());
        intent.putExtra("track", getTrackName());
        intent.putExtra("playing", isPlaying());
        if ("com.android.music.quitplayback".equals(str)) {
            this.mPlayPos = -1;
            this.mPlayListLen = 0;
            this.mShuffleMode = 0;
            this.mRepeatMode = 0;
            sendBroadcast(intent);
        } else {
            sendStickyBroadcast(intent);
            if (!this.misAVRCPseeking) {
                if (str.equals("com.android.music.playstatechanged")) {
                    this.mRemoteControlClient.setPlaybackState(isPlaying() ? 3 : 2, position(), 1.0f);
                    MusicLogUtils.v("MusicService", "set remote PLAYING" + isPlaying());
                } else if (str.equals("com.android.music.metachanged")) {
                    this.mRemoteControlClient.setPlaybackState(isPlaying() ? 3 : 2, position(), 1.0f);
                    MusicLogUtils.v("MusicService", "set remote PLAYING" + isPlaying());
                }
            } else {
                this.mRemoteControlClient.setPlaybackState(position() + 1000 <= duration() ? 4 : 5, position(), 1.0f);
            }
        }
        if (str.equals("com.android.music.queuechanged")) {
            saveQueue(true);
        } else {
            saveQueue(false);
        }
        this.mAppWidgetProvider.notifyChange(this, str);
        notifyBTAvrcp(str);
    }

    private void ensurePlayListCapacity(int i) {
        if (this.mPlayList == null || i > this.mPlayList.length) {
            long[] jArr = new long[i * 2];
            int length = this.mPlayList != null ? this.mPlayList.length : this.mPlayListLen;
            for (int i2 = 0; i2 < length; i2++) {
                jArr[i2] = this.mPlayList[i2];
            }
            this.mPlayList = jArr;
        }
    }

    private void addToPlayList(long[] jArr, int i) {
        int length = jArr.length;
        if (i < 0) {
            this.mPlayListLen = 0;
            i = 0;
        }
        ensurePlayListCapacity(this.mPlayListLen + length);
        if (i > this.mPlayListLen) {
            i = this.mPlayListLen;
        }
        for (int i2 = this.mPlayListLen - i; i2 > 0; i2--) {
            int i3 = i + i2;
            this.mPlayList[i3] = this.mPlayList[i3 - length];
        }
        for (int i4 = 0; i4 < length; i4++) {
            this.mPlayList[i + i4] = jArr[i4];
        }
        this.mPlayListLen += length;
        if (this.mPlayListLen == 0) {
            this.mHistory.clear();
            synchronized (this) {
                if (this.mCursor != null) {
                    this.mCursor.close();
                    this.mCursor = null;
                }
            }
            notifyChange("com.android.music.metachanged");
        }
    }

    public void enqueue(long[] jArr, int i) {
        synchronized (this) {
            StringBuilder sb = new StringBuilder();
            sb.append("enqueue() list = ");
            sb.append(jArr != null ? jArr : "null");
            sb.append(",action = ");
            sb.append(i);
            sb.append(",mPlayListLen = ");
            sb.append(this.mPlayListLen);
            MusicLogUtils.v("MusicService", sb.toString());
            if (i == 2 && this.mPlayPos + 1 < this.mPlayListLen) {
                addToPlayList(jArr, this.mPlayPos + 1);
                notifyChange("com.android.music.queuechanged");
            } else {
                if (this.mPlayer.isInitialized() && getRepeatMode() == 2 && this.mPlayPos == this.mPlayListLen - 1) {
                    this.mPlayer.setNextDataSource(null);
                }
                addToPlayList(jArr, Integer.MAX_VALUE);
                notifyChange("com.android.music.queuechanged");
                if (i == 1) {
                    this.mPlayPos = this.mPlayListLen - jArr.length;
                    openCurrentAndNext();
                    play();
                    notifyChange("com.android.music.metachanged");
                    return;
                }
            }
            if (this.mPlayPos < 0) {
                this.mPlayPos = 0;
                openCurrentAndNext();
                play();
                notifyChange("com.android.music.metachanged");
            }
        }
    }

    public void open(long[] jArr, int i) {
        synchronized (this) {
            try {
                if (jArr == null) {
                    return;
                }
                Handler handler = this.mPlayer.getHandler();
                if (handler != null && handler.hasMessages(7)) {
                    MusicLogUtils.v("MusicService", "removeMessages(TRACK_WENT_TO_NEXT)");
                    handler.removeMessages(7);
                }
                boolean z = true;
                if (this.mShuffleMode == 2) {
                    this.mShuffleMode = 1;
                }
                long audioId = getAudioId();
                int length = jArr.length;
                if (this.mPlayListLen == length) {
                    int i2 = 0;
                    while (true) {
                        if (i2 >= length) {
                            z = false;
                            break;
                        } else if (jArr[i2] != this.mPlayList[i2]) {
                            break;
                        } else {
                            i2++;
                        }
                    }
                }
                StringBuilder sb = new StringBuilder();
                sb.append("open() list = ");
                sb.append(jArr != null ? jArr : "null");
                sb.append(",position = ");
                sb.append(i);
                sb.append(",newlist = ");
                sb.append(z);
                MusicLogUtils.v("MusicService", sb.toString());
                if (z) {
                    addToPlayList(jArr, -1);
                    notifyChange("com.android.music.queuechanged");
                }
                int i3 = this.mPlayPos;
                if (i >= 0) {
                    this.mPlayPos = i;
                } else {
                    this.mPlayPos = this.mRand.nextInt(this.mPlayListLen);
                }
                this.mHistory.clear();
                saveBookmarkIfNeeded();
                openCurrentAndNext();
                if (audioId != getAudioId()) {
                    notifyChange("com.android.music.metachanged");
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public void moveQueueItem(int i, int i2) {
        MusicLogUtils.v("MusicService", "moveQueueItem() index1 = " + i + ",index2 = " + i2);
        synchronized (this) {
            if (this.mPlayer.isInitialized()) {
                this.mPlayer.setNextDataSource(null);
            }
            if (i >= this.mPlayListLen) {
                i = this.mPlayListLen - 1;
            }
            if (i2 >= this.mPlayListLen) {
                i2 = this.mPlayListLen - 1;
            }
            if (i < i2) {
                long j = this.mPlayList[i];
                int i3 = i;
                while (i3 < i2) {
                    int i4 = i3 + 1;
                    this.mPlayList[i3] = this.mPlayList[i4];
                    i3 = i4;
                }
                this.mPlayList[i2] = j;
                if (this.mPlayPos == i) {
                    this.mPlayPos = i2;
                } else if (this.mPlayPos >= i && this.mPlayPos <= i2) {
                    this.mPlayPos--;
                }
            } else if (i2 < i) {
                long j2 = this.mPlayList[i];
                for (int i5 = i; i5 > i2; i5--) {
                    this.mPlayList[i5] = this.mPlayList[i5 - 1];
                }
                this.mPlayList[i2] = j2;
                if (this.mPlayPos == i) {
                    this.mPlayPos = i2;
                } else if (this.mPlayPos >= i2 && this.mPlayPos <= i) {
                    this.mPlayPos++;
                }
            }
            notifyChange("com.android.music.queuechanged");
        }
    }

    public long[] getQueue() {
        long[] jArr;
        synchronized (this) {
            int i = this.mPlayListLen;
            jArr = new long[i];
            for (int i2 = 0; i2 < i; i2++) {
                jArr[i2] = this.mPlayList[i2];
            }
        }
        return jArr;
    }

    private Cursor getCursorForId(long j) {
        String strValueOf = String.valueOf(j);
        Cursor cursor = null;
        try {
            Cursor cursorQuery = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, this.mCursorCols, "_id=" + strValueOf, null, null);
            if (cursorQuery != null) {
                try {
                    if (!cursorQuery.moveToFirst()) {
                        cursorQuery.close();
                    } else {
                        cursor = cursorQuery;
                    }
                    MusicLogUtils.v("MusicService", "getCursorForId is " + cursor);
                } catch (IllegalStateException e) {
                    cursor = cursorQuery;
                    MusicLogUtils.v("MusicService", "getCursorForId()-IllegalStateException");
                }
            }
        } catch (IllegalStateException e2) {
        }
        return cursor;
    }

    private void openCurrentAndNext() {
        synchronized (this) {
            if (this.mLossesBTEnable) {
                MusicLogUtils.v("MusicService", "openCurrentAndNext(),lossBTEnable,sleep 100");
                SystemClock.sleep(100L);
            }
            if (this.mCursor != null) {
                MusicLogUtils.v("MusicService", "cursor null");
                this.mCursor.close();
                this.mCursor = null;
            }
            this.mDurationOverride = -1L;
            if (this.mPlayListLen == 0) {
                this.mHistory.clear();
                return;
            }
            stop(false);
            this.mCursor = getCursorForId(this.mPlayList[this.mPlayPos]);
            while (true) {
                if (this.mCursor != null) {
                    if (open(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/" + this.mCursor.getLong(0))) {
                        break;
                    }
                    if (this.mCursor != null) {
                        this.mCursor.close();
                        this.mCursor = null;
                    }
                    int i = this.mOpenFailedCounter;
                    this.mOpenFailedCounter = i + 1;
                    if (i >= 2 || this.mPlayListLen <= 1) {
                        break;
                    }
                    if (this.mIsPrev) {
                        prev();
                        break;
                    }
                    int nextPosition = getNextPosition(false);
                    if (nextPosition < 0) {
                        gotoIdleState();
                        if (this.mIsSupposedToBePlaying) {
                            this.mIsSupposedToBePlaying = false;
                            notifyChange("com.android.music.playstatechanged");
                        }
                        return;
                    }
                    this.mPlayPos = nextPosition;
                    stop(false);
                    this.mPlayPos = nextPosition;
                    this.mCursor = getCursorForId(this.mPlayList[this.mPlayPos]);
                }
            }
        }
    }

    private void setNextTrack() {
        if (!this.mPlayer.isInitialized()) {
            MusicLogUtils.v("MusicService", "setNextTrack with player not initialized!");
            return;
        }
        if (this.mPlayListLen == 0) {
            MusicLogUtils.v("MusicService", "setNextTrack with an empty playlist!");
            return;
        }
        if (this.mShuffleMode == 1 && this.mPlayListLen == 1) {
            MusicLogUtils.v("MusicService", "playlist's length is 1 with shuffle nomal mode,need not set nextplayer.");
            return;
        }
        int size = this.mHistory.size();
        addPlayedTrackToHistory();
        this.mNextPlayPos = getNextPosition(false);
        int size2 = this.mHistory.size();
        MusicLogUtils.v("MusicService", "afterSize = " + size2 + ", preSize = " + size);
        if (size2 > 0) {
            this.mHistory.remove(size2 - 1);
        }
        if (this.mPlayList != null && this.mNextPlayPos >= 0 && this.mRepeatMode != 1) {
            long j = this.mPlayList[this.mNextPlayPos];
            this.mPlayer.setNextDataSource(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/" + j);
            return;
        }
        this.mPlayer.setNextDataSource(null);
    }

    public boolean open(java.lang.String r11) {
        synchronized (r10) {
            ;
            r1 = new java.lang.StringBuilder();
            r1.append("open(");
            r1.append(r11);
            r1.append(")");
            com.android.music.MusicLogUtils.v("MusicService", r1.toString());
            if (r11 == null) {
                return false;
            } else {
                if (r10.mCursor == null) {
                    r3 = getContentResolver();
                    if (r11.startsWith("content://media/")) {
                        r4 = android.net.Uri.parse(r11);
                        r6 = null;
                        r7 = null;
                    } else {
                        r6 = "_data=?";
                        r7 = new java.lang.String[]{r11};
                        r4 = android.provider.MediaStore.Audio.Media.getContentUriForPath(r11);
                    }
                    r10.mCursor = r3.query(r4, r10.mCursorCols, r6, r7, null);
                    if (r10.mCursor != null) {
                        if (r10.mCursor.getCount() == 0) {
                            r10.mCursor.close();
                            r10.mCursor = null;
                        } else {
                            r10.mCursor.moveToNext();
                            ensurePlayListCapacity(1);
                            r10.mPlayListLen = 1;
                            r10.mPlayList[0] = r10.mCursor.getLong(0);
                            r10.mPlayPos = 0;
                        }
                    }
                    while (true) {
                    }
                    if (!r10.mPlayer.isInitialized()) {
                        r10.mOpenFailedCounter = 0;
                        return true;
                    } else {
                        stop(true);
                        return false;
                    }
                }
                r10.mFileToPlay = r11;
                r10.mIsPlayerReady = false;
                r10.mMediaSeekable = true;
                r10.mIsPlaylistCompleted = false;
                com.android.music.MediaPlaybackService.mTrackCompleted = false;
                r10.mPlayer.setDataSourceAsync(r10.mFileToPlay);
                if (!r10.mPlayer.isInitialized()) {
                }
            }
        }
    }

    public void play() {
        synchronized (this) {
            MusicLogUtils.d("MusicService", ">> play: init=" + this.mPlayer.isInitialized() + ", ready=" + this.mIsPlayerReady + ", listlen=" + this.mPlayListLen);
            if (this.mAudioManager.requestAudioFocus(this.mAudioFocusListener, 3, 1) == 0) {
                showToast(getString(R.string.audiofocus_request_failed_message));
                MusicLogUtils.d("MusicService", "<< play: phone call is ongoing, can not play music!");
                return;
            }
            this.mAudioManager.registerMediaButtonEventReceiver(new ComponentName(getPackageName(), MediaButtonIntentReceiver.class.getName()));
            this.mQuietMode = false;
            if (this.mPlayer.isInitialized() && this.mIsPlayerReady) {
                this.mIsPlaylistCompleted = false;
                if (canGoToNext(this.mPlayer.duration(), 10000L)) {
                    gotoNext(true);
                    notifyChange("com.android.music.playstatechanged");
                    MusicLogUtils.v("MusicService", "<< play: go to next song first");
                    return;
                }
                this.mPlayer.start();
                MusicLogUtils.v("MusicService", "MediaPlayer start done.");
                this.mMediaplayerHandler.removeMessages(5);
                this.mMediaplayerHandler.sendEmptyMessage(6);
                updateNotification(this, null);
                if (!this.mIsSupposedToBePlaying) {
                    this.mIsSupposedToBePlaying = true;
                    notifyChange("com.android.music.playstatechanged");
                }
                if (this.mWhetherAttachWhenPause) {
                    if (this.mAuxEffectId <= 0) {
                        this.mPlayer.attachAuxEffect(0);
                    } else {
                        this.mPlayer.attachAuxEffect(this.mAuxEffectId);
                        this.mPlayer.setAuxEffectSendLevel(1.0f);
                    }
                    this.mWhetherAttachWhenPause = false;
                    MusicLogUtils.v("MusicService", "Attach reverb when user start play again with mAuxEffectId = " + this.mAuxEffectId);
                }
                if (this.mPreAudioId != getAudioId()) {
                    this.mAsyncAlbumArtWorker = new AlbumArtWorker();
                    this.mAsyncAlbumArtWorker.execute(Long.valueOf(getAlbumId()));
                }
            } else if (this.mPlayListLen <= 0 && !this.mPlayer.isInitialized() && !this.mIsPlayerReady) {
                setShuffleMode(2);
            }
            this.mPreAudioId = -1L;
            MusicLogUtils.v("MusicService", "<< play");
        }
    }

    private void updateNotification(Context context, Bitmap bitmap) {
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.newstatusbar);
        String trackName = getTrackName();
        String artistName = getArtistName();
        if (artistName == null || artistName.equals("<unknown>")) {
            artistName = getString(R.string.unknown_artist_name);
        }
        remoteViews.setTextViewText(R.id.txt_trackinfo, trackName + " - " + artistName);
        Intent intent = new Intent("com.android.music.PLAYBACK_VIEWER");
        intent.putExtra("collapse_statusbar", true);
        remoteViews.setOnClickPendingIntent(R.id.iv_cover, PendingIntent.getActivity(context, 0, intent, 0));
        Intent intent2 = new Intent("com.android.music.musicservicecommand.previous");
        intent2.setClass(context, MediaPlaybackService.class);
        remoteViews.setOnClickPendingIntent(R.id.btn_prev, PendingIntent.getService(context, 0, intent2, 0));
        Intent intent3 = new Intent("com.android.music.musicservicecommand.pause");
        intent3.setClass(context, MediaPlaybackService.class);
        remoteViews.setOnClickPendingIntent(R.id.btn_pause, PendingIntent.getService(context, 0, intent3, 0));
        Intent intent4 = new Intent("com.android.music.musicservicecommand.next");
        intent4.setClass(context, MediaPlaybackService.class);
        remoteViews.setOnClickPendingIntent(R.id.btn_next, PendingIntent.getService(context, 0, intent4, 0));
        remoteViews.setOnClickPendingIntent(R.id.rl_newstatus, PendingIntent.getService(context, 0, new Intent("my.nullaction"), 0));
        if (bitmap != null) {
            remoteViews.setImageViewBitmap(R.id.iv_cover, bitmap);
            this.mAlbumArt = bitmap;
        }
        Intent intent5 = new Intent("com.android.music.PLAYBACK_VIEWER");
        Notification notificationBuild = new Notification.Builder(context, "music_notification_channel").build();
        notificationBuild.contentView = remoteViews;
        notificationBuild.flags |= 2;
        notificationBuild.icon = R.drawable.stat_notify_musicplayer;
        notificationBuild.contentIntent = PendingIntent.getActivity(context, 0, intent5, 0);
        this.mMediaplayerHandler.removeMessages(11);
        startForeground(1, notificationBuild);
    }

    private void stop(boolean z) {
        synchronized (this) {
            MusicLogUtils.v("MusicService", "stop(" + z + ")");
            if (this.mPlayer != null && this.mPlayer.isInitialized()) {
                this.mPlayer.stop();
            }
            this.mIsPlayerReady = false;
            this.mDoSeekWhenPrepared = false;
            this.mMediaSeekable = true;
            this.mFileToPlay = null;
            if (this.mCursor != null) {
                this.mCursor.close();
                this.mCursor = null;
            }
            this.mDurationOverride = -1L;
            if (z) {
                gotoIdleState();
            }
            if (z) {
                this.mIsSupposedToBePlaying = false;
            }
        }
    }

    public void stop() {
        stop(true);
    }

    public void pause() {
        synchronized (this) {
            MusicLogUtils.v("MusicService", "pause");
            this.mMediaplayerHandler.removeMessages(6);
            this.mPlayer.setVolume(1.0f);
            if (isPlaying() && this.mPlayer.isInitialized()) {
                if (this.mPlayer.isPlaying()) {
                    this.mPlayer.pause();
                }
                if (duration() - position() < 2000) {
                    this.mPlayer.setNextDataSource(null);
                }
                gotoIdleState();
                this.mIsSupposedToBePlaying = false;
                notifyChange("com.android.music.playstatechanged");
                saveBookmarkIfNeeded();
            }
        }
    }

    public boolean isPlaying() {
        return this.mIsSupposedToBePlaying;
    }

    public void prev() {
        boolean z;
        synchronized (this) {
            MusicLogUtils.v("MusicService", "prev");
            if (this.mShuffleMode == 1) {
                int size = this.mHistory.size();
                if (size == 0) {
                    return;
                }
                int i = size - 1;
                while (true) {
                    if (i >= 0) {
                        Integer numRemove = this.mHistory.remove(i);
                        if (numRemove.intValue() >= this.mPlayListLen) {
                            i--;
                        } else {
                            this.mPlayPos = numRemove.intValue();
                            z = true;
                            break;
                        }
                    } else {
                        z = false;
                        break;
                    }
                }
                MusicLogUtils.v("MusicService", "prev: mPlayPos = " + this.mPlayPos + ", mHistory = " + this.mHistory + ", hasValidValue = " + z);
                if (!z) {
                    return;
                }
            } else if (this.mPlayPos > 0) {
                this.mPlayPos--;
            } else {
                this.mPlayPos = this.mPlayListLen - 1;
            }
            this.mIsPrev = true;
            saveBookmarkIfNeeded();
            stop(false);
            openCurrentAndNext();
            notifyChange("com.android.music.metachanged");
            this.mIsPrev = false;
        }
    }

    private int getNextPosition(boolean z) {
        MusicLogUtils.v("MusicService", "getNextPosition(" + z + ")");
        int i = -1;
        if (this.mShuffleMode == 1) {
            int i2 = this.mPlayListLen;
            int size = this.mHistory.size();
            int i3 = 0;
            int[] iArr = new int[i2];
            int i4 = 0;
            while (true) {
                if (i4 >= size) {
                    break;
                }
                int iIntValue = this.mHistory.get(i4).intValue();
                if (iIntValue >= iArr.length) {
                    this.mHistory.clear();
                    size = 0;
                    i3 = 0;
                    break;
                }
                if (iArr[iIntValue] == -1) {
                    iArr = new int[i2];
                    i3 = i4;
                } else {
                    iArr[iIntValue] = -1;
                }
                i4++;
            }
            for (int i5 = size % i2; i5 < i2; i5++) {
                iArr[i5] = i5;
            }
            int i6 = i2;
            while (i3 < size) {
                int iIntValue2 = this.mHistory.get(i3).intValue();
                if (iIntValue2 < i2) {
                    i6--;
                    iArr[iIntValue2] = -1;
                }
                i3++;
            }
            if (i6 <= 0) {
                if (this.mRepeatMode != 2 && !z) {
                    return -1;
                }
                for (int i7 = 0; i7 < i2; i7++) {
                    iArr[i7] = i7;
                }
                i6 = i2;
            }
            int iNextInt = this.mRand.nextInt(i6);
            while (true) {
                if (i < i2 - 1) {
                    i++;
                    if (iArr[i] < 0) {
                        continue;
                    }
                }
                iNextInt--;
                if (iNextInt < 0) {
                    return i;
                }
            }
        } else {
            if (this.mShuffleMode == 2) {
                doAutoShuffleUpdate();
                return this.mPlayPos + 1;
            }
            if (this.mPlayPos < 0) {
                return -1;
            }
            if (this.mPlayPos >= this.mPlayListLen - 1) {
                this.mdurationReady = true;
                if (this.mRepeatMode == 0 && !z) {
                    return -1;
                }
                if (this.mRepeatMode != 2 && !z) {
                    return -1;
                }
                MusicLogUtils.v("MusicService", "mNumErr =" + this.mNumErr + ", mplaylistlen = " + this.mPlayListLen);
                if (this.mNumErr < this.mPlayListLen || z) {
                    return 0;
                }
                this.mNumErr = 0;
                return -1;
            }
            return this.mPlayPos + 1;
        }
    }

    public void gotoNext(boolean z) {
        synchronized (this) {
            MusicLogUtils.v("MusicService", ">> gotoNext(" + z + ") mPlayListLen = " + this.mPlayListLen);
            if (this.mPlayListLen <= 0) {
                return;
            }
            if (z && !mTrackCompleted) {
                addPlayedTrackToHistory();
            }
            int nextPosition = getNextPosition(z);
            if (nextPosition < 0) {
                gotoIdleState();
                if (this.mIsSupposedToBePlaying) {
                    this.mIsSupposedToBePlaying = false;
                    notifyChange("com.android.music.playstatechanged");
                }
                if (!this.mIsPlaylistCompleted) {
                    this.mIsPlaylistCompleted = true;
                    notifyChange("com.android.music.playbackcomplete");
                }
                this.mMediaplayerHandler.removeMessages(6);
                this.mPlayer.setVolume(1.0f);
                return;
            }
            this.mPlayPos = nextPosition;
            saveBookmarkIfNeeded();
            stop(false);
            this.mPlayPos = nextPosition;
            openCurrentAndNext();
            notifyChange("com.android.music.metachanged");
            MusicLogUtils.v("MusicService", "<< gotoNext(" + z + ")");
        }
    }

    private void gotoIdleState() {
        MusicLogUtils.v("MusicService", "gotoIdleState");
        this.mAlbumArt = null;
        this.mNumErr = 0;
        this.mDelayedStopHandler.removeCallbacksAndMessages(null);
        this.mDelayedStopHandler.sendMessageDelayed(this.mDelayedStopHandler.obtainMessage(), 60000L);
        if (this.mTaskRemoved) {
            this.mMediaplayerHandler.sendEmptyMessageDelayed(11, 1000L);
        } else {
            stopForeground(true);
        }
    }

    private void saveBookmarkIfNeeded() {
        try {
            if (isPodcast()) {
                long jPosition = position();
                long bookmark = getBookmark();
                long jDuration = duration();
                if (jPosition >= bookmark || jPosition + 10000 <= bookmark) {
                    if (jPosition > bookmark && jPosition - 10000 < bookmark) {
                        return;
                    }
                    if (jPosition < 15000 || 10000 + jPosition > jDuration) {
                        jPosition = 0;
                    }
                    if (this.mCursor != null) {
                        ContentValues contentValues = new ContentValues();
                        contentValues.put("bookmark", Long.valueOf(jPosition));
                        getContentResolver().update(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, this.mCursor.getLong(0)), contentValues, null, null);
                        return;
                    }
                    MusicLogUtils.v("MusicService", "saveBookmarkIfNeeded() fail caused by null mCursor");
                }
            }
        } catch (SQLiteException e) {
        }
    }

    private void doAutoShuffleUpdate() {
        boolean z;
        int iNextInt;
        if (this.mPlayPos > 10) {
            removeTracks(0, this.mPlayPos - 9);
            z = true;
        } else {
            z = false;
        }
        int i = 7 - (this.mPlayListLen - (this.mPlayPos < 0 ? -1 : this.mPlayPos));
        boolean z2 = z;
        int i2 = 0;
        while (i2 < i) {
            int size = this.mHistory.size();
            while (true) {
                iNextInt = this.mRand.nextInt(this.mAutoShuffleList.length);
                if (!wasRecentlyUsed(iNextInt, size)) {
                    break;
                } else {
                    size /= 2;
                }
            }
            this.mHistory.add(Integer.valueOf(iNextInt));
            if (this.mHistory.size() > 100) {
                this.mHistory.remove(0);
            }
            ensurePlayListCapacity(this.mPlayListLen + 1);
            long[] jArr = this.mPlayList;
            int i3 = this.mPlayListLen;
            this.mPlayListLen = i3 + 1;
            jArr[i3] = this.mAutoShuffleList[iNextInt];
            i2++;
            z2 = true;
        }
        MusicLogUtils.v("MusicService", "doAutoShuffleUpdate() notify = " + z2);
        if (z2) {
            notifyChange("com.android.music.queuechanged");
        }
    }

    private boolean wasRecentlyUsed(int i, int i2) {
        if (i2 == 0) {
            return false;
        }
        int size = this.mHistory.size();
        if (size < i2) {
            MusicLogUtils.v("MusicService", "lookback too big");
            i2 = size;
        }
        int i3 = size - 1;
        for (int i4 = 0; i4 < i2; i4++) {
            if (this.mHistory.get(i3 - i4).intValue() == i) {
                return true;
            }
        }
        return false;
    }

    private static class Shuffler {
        private int mPrevious;
        private Random mRandom;

        private Shuffler() {
            this.mRandom = new Random();
        }

        public int nextInt(int i) {
            int iNextInt;
            do {
                iNextInt = this.mRandom.nextInt(i);
                if (iNextInt != this.mPrevious) {
                    break;
                }
            } while (i > 1);
            this.mPrevious = iNextInt;
            return iNextInt;
        }
    }

    private boolean makeAutoShuffleList() throws Throwable {
        Cursor cursorQuery;
        try {
            cursorQuery = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{"_id"}, "is_music=1", null, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.getCount() != 0) {
                        int count = cursorQuery.getCount();
                        long[] jArr = new long[count];
                        for (int i = 0; i < count; i++) {
                            cursorQuery.moveToNext();
                            jArr[i] = cursorQuery.getLong(0);
                        }
                        this.mAutoShuffleList = jArr;
                        if (cursorQuery == null) {
                            return true;
                        }
                        cursorQuery.close();
                        return true;
                    }
                } catch (RuntimeException e) {
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    return false;
                } catch (Throwable th) {
                    th = th;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    throw th;
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return false;
        } catch (RuntimeException e2) {
            cursorQuery = null;
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
        }
    }

    public int removeTracks(int i, int i2) {
        MusicLogUtils.v("MusicService", "removeTracks()-first = " + i + ",last = " + i2);
        if (this.mPlayer.isInitialized()) {
            this.mPlayer.setNextDataSource(null);
        }
        int iRemoveTracksInternal = removeTracksInternal(i, i2);
        if (iRemoveTracksInternal > 0) {
            notifyChange("com.android.music.queuechanged");
        }
        return iRemoveTracksInternal;
    }

    private int removeTracksInternal(int i, int i2) {
        boolean z;
        synchronized (this) {
            try {
                if (i2 < i) {
                    return 0;
                }
                if (i < 0) {
                    i = 0;
                }
                if (i2 >= this.mPlayListLen) {
                    i2 = this.mPlayListLen - 1;
                }
                if (i <= this.mPlayPos && this.mPlayPos <= i2) {
                    this.mPlayPos = i;
                    z = true;
                } else {
                    if (this.mPlayPos > i2) {
                        this.mPlayPos -= (i2 - i) + 1;
                    }
                    z = false;
                }
                int i3 = (this.mPlayListLen - i2) - 1;
                for (int i4 = 0; i4 < i3; i4++) {
                    this.mPlayList[i + i4] = this.mPlayList[i2 + 1 + i4];
                }
                removeInShuffleMode(i, i2);
                int i5 = (i2 - i) + 1;
                this.mPlayListLen -= i5;
                MusicLogUtils.v("MusicService", "removeTracksInternal need goto gotoNext(" + z + ")");
                if (z) {
                    if (this.mPlayListLen == 0) {
                        this.mHistory.clear();
                        stop(true);
                        this.mPlayPos = -1;
                        if (this.mCursor != null) {
                            this.mCursor.close();
                            this.mCursor = null;
                        }
                        notifyChange("com.android.music.quitplayback");
                    } else {
                        if (this.mPlayPos >= this.mPlayListLen) {
                            this.mPlayPos = 0;
                        }
                        this.mQuietMode = !isPlaying();
                        stop(false);
                        openCurrentAndNext();
                        notifyChange("com.android.music.metachanged");
                        if (this.mQuietMode && this.mPlayPos == -1 && this.mPlayListLen == 0) {
                            MusicLogUtils.v("MusicService", "removeTracksInternal with no audio in playlist, set mQuietMode to false");
                            this.mQuietMode = false;
                        }
                    }
                }
                MusicLogUtils.v("MusicService", "removeTracksInternal end: mPlayListLen = " + this.mPlayListLen + ", mPlayPos = " + this.mPlayPos);
                return i5;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private void removeInShuffleMode(int i, int i2) {
        MusicLogUtils.v("MusicService", "removeInShuffleMode f = " + i + ", l = " + i2);
        while (i2 >= i) {
            for (int size = this.mHistory.size() - 1; size >= 0; size--) {
                Integer num = this.mHistory.get(size);
                if (num.intValue() > i2) {
                    this.mHistory.set(size, Integer.valueOf(num.intValue() - 1));
                } else if (num.intValue() == i2) {
                    this.mHistory.remove(size);
                }
            }
            i2--;
        }
    }

    public int removeTrack(long j) {
        int iRemoveTracksInternal;
        MusicLogUtils.v("MusicService", "removeTrack>>>: id = " + j);
        if (this.mPlayer.isInitialized()) {
            this.mPlayer.setNextDataSource(null);
        }
        synchronized (this) {
            int iRemoveTracksInternal2 = 0;
            for (int i = this.mPlayListLen - 1; i > this.mPlayPos; i--) {
                if (this.mPlayList[i] == j) {
                    MusicLogUtils.v("MusicService", "remove rewind(" + i + "),Play position is " + this.mPlayPos + ",removed num = " + iRemoveTracksInternal2);
                    iRemoveTracksInternal2 += removeTracksInternal(i, i);
                }
            }
            int i2 = this.mPlayPos < this.mPlayListLen ? this.mPlayPos + 1 : this.mPlayListLen;
            iRemoveTracksInternal = iRemoveTracksInternal2;
            int i3 = 0;
            for (int i4 = 0; i4 < i2; i4++) {
                if (this.mPlayList[i3] == j) {
                    MusicLogUtils.v("MusicService", "remove forward(" + i3 + "),Play position is " + this.mPlayPos + ",removed num = " + iRemoveTracksInternal);
                    iRemoveTracksInternal += removeTracksInternal(i3, i3);
                } else {
                    i3++;
                }
            }
        }
        if (iRemoveTracksInternal > 0) {
            notifyChange("com.android.music.queuechanged");
        }
        MusicLogUtils.v("MusicService", "removeTrack<<<: removed num = " + iRemoveTracksInternal);
        return iRemoveTracksInternal;
    }

    public void setShuffleMode(int i) {
        synchronized (this) {
            MusicLogUtils.v("MusicService", "setShuffleMode(" + i + ")");
            if (this.mShuffleMode != i || this.mPlayListLen <= 0) {
                if (this.mPlayer.isInitialized()) {
                    this.mPlayer.setNextDataSource(null);
                }
                this.mShuffleMode = i;
                if (this.mShuffleMode == 0) {
                    this.mHistory.clear();
                }
                if (this.mRepeatMode == 1 && this.mShuffleMode != 0) {
                    this.mRepeatMode = 2;
                }
                if (this.mShuffleMode == 2) {
                    if (makeAutoShuffleList()) {
                        this.mPlayListLen = 0;
                        doAutoShuffleUpdate();
                        this.mPlayPos = 0;
                        openCurrentAndNext();
                        notifyChange("com.android.music.metachanged");
                        return;
                    }
                    this.mShuffleMode = 0;
                }
                saveQueue(false);
            }
        }
    }

    public int getShuffleMode() {
        return this.mShuffleMode;
    }

    public void setRepeatMode(int i) {
        synchronized (this) {
            MusicLogUtils.v("MusicService", "setRepeatMode(" + i + ")");
            this.mRepeatMode = i;
            if (this.mRepeatMode == 0 && this.mShuffleMode == 1) {
                this.mHistory.clear();
            }
            setNextTrack();
            saveQueue(false);
        }
    }

    public int getRepeatMode() {
        return this.mRepeatMode;
    }

    public int getMediaMountedCount() {
        return this.mMediaMountedCount;
    }

    public String getPath() {
        return this.mFileToPlay;
    }

    public long getAudioId() {
        synchronized (this) {
            if (this.mPlayPos >= 0 && this.mPlayer != null && this.mPlayer.isInitialized()) {
                return this.mPlayList[this.mPlayPos];
            }
            return -1L;
        }
    }

    public int getQueuePosition() {
        int i;
        synchronized (this) {
            i = this.mPlayPos;
        }
        return i;
    }

    public void setQueuePosition(int i) {
        synchronized (this) {
            stop(false);
            this.mPlayPos = i;
            openCurrentAndNext();
            play();
            notifyChange("com.android.music.metachanged");
            if (this.mShuffleMode == 2) {
                doAutoShuffleUpdate();
            }
        }
    }

    public String getArtistName() {
        synchronized (this) {
            if (this.mCursor == null) {
                return null;
            }
            return this.mCursor.getString(this.mCursor.getColumnIndexOrThrow("artist"));
        }
    }

    public long getArtistId() {
        synchronized (this) {
            if (this.mCursor == null) {
                return -1L;
            }
            return this.mCursor.getLong(this.mCursor.getColumnIndexOrThrow("artist_id"));
        }
    }

    public String getAlbumName() {
        synchronized (this) {
            if (this.mCursor == null) {
                return null;
            }
            return this.mCursor.getString(this.mCursor.getColumnIndexOrThrow("album"));
        }
    }

    public long getAlbumId() {
        synchronized (this) {
            if (this.mCursor == null) {
                return -1L;
            }
            return this.mCursor.getLong(this.mCursor.getColumnIndexOrThrow("album_id"));
        }
    }

    public String getTrackName() {
        synchronized (this) {
            if (this.mCursor == null) {
                return null;
            }
            return this.mCursor.getString(this.mCursor.getColumnIndexOrThrow("title"));
        }
    }

    private boolean isPodcast() {
        synchronized (this) {
            if (this.mCursor == null) {
                return false;
            }
            return this.mCursor.getInt(8) > 0;
        }
    }

    private long getBookmark() {
        synchronized (this) {
            if (this.mCursor == null) {
                return 0L;
            }
            return this.mCursor.getLong(9);
        }
    }

    public long duration() {
        this.mdurationReady = true;
        if (this.mDurationOverride != -1) {
            MusicLogUtils.v("MusicService", "duration from override is " + this.mDurationOverride);
            return this.mDurationOverride;
        }
        synchronized (this) {
            if (this.mCursor != null) {
                int columnIndexOrThrow = this.mCursor.getColumnIndexOrThrow("duration");
                if (!this.mCursor.isNull(columnIndexOrThrow)) {
                    MusicLogUtils.v("MusicService", "duration from database is " + this.mCursor.getLong(columnIndexOrThrow));
                    return this.mCursor.getLong(columnIndexOrThrow);
                }
            }
            if (this.mPlayer.isInitialized() && this.mIsPlayerReady) {
                this.mDurationOverride = this.mPlayer.duration();
                MusicLogUtils.v("MusicService", "duration from MediaPlayer is " + this.mDurationOverride);
                return this.mDurationOverride;
            }
            return 0L;
        }
    }

    public long position() {
        if (this.mPlayer.isInitialized() && this.mIsPlayerReady) {
            long jPosition = this.mPlayer.position();
            if (!this.mdurationReady) {
                jPosition = 0;
            }
            MusicLogUtils.v("MusicService", "Position=" + jPosition);
            return jPosition;
        }
        return -1L;
    }

    public long seek(long j) {
        MusicLogUtils.v("MusicService", "seek(" + j + ")");
        if (!this.mPlayer.isInitialized() || !this.mIsPlayerReady) {
            return -1L;
        }
        if (j != 0 && (!this.mMediaSeekable || !mediaCanSeek())) {
            MusicLogUtils.v("MusicService", "seek, sorry, seek is not supported");
            return -1L;
        }
        long jPosition = this.mPlayer.position();
        if (j < 0) {
            j = 0;
        }
        MusicLogUtils.v("MusicService", "seek curr(" + jPosition + ")");
        long jDuration = this.mPlayer.duration();
        if (j < jDuration) {
            this.mIsPlaylistCompleted = false;
        } else {
            j = jDuration;
        }
        long jSeek = this.mPlayer.seek(j);
        this.mRemoteControlClient.setPlaybackState(isPlaying() ? 3 : 2, position(), 1.0f);
        return jSeek;
    }

    public void endscan() {
        MusicLogUtils.v("MusicService", "end scan");
        this.mRemoteControlClient.setPlaybackState(isPlaying() ? 3 : 2, position(), 1.0f);
        MusicLogUtils.v("MusicService", "set remote PLAYING" + isPlaying());
    }

    public int getAudioSessionId() {
        int audioSessionId;
        synchronized (this) {
            audioSessionId = this.mPlayer.getAudioSessionId();
        }
        return audioSessionId;
    }

    private class MultiPlayer {
        private Handler mHandler;
        private CompatMediaPlayer mNextMediaPlayer;
        private CompatMediaPlayer mCurrentMediaPlayer = new CompatMediaPlayer();
        private boolean mIsInitialized = false;
        MediaPlayer.OnCompletionListener listener = new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                if (!MediaPlaybackService.this.misAVRCPseeking || MediaPlaybackService.this.mPlayBeforeseek) {
                    MediaPlaybackService.this.addPlayedTrackToHistory();
                    MediaPlaybackService.this.mdurationReady = false;
                    if (mediaPlayer != MultiPlayer.this.mCurrentMediaPlayer || MultiPlayer.this.mNextMediaPlayer == null || !MultiPlayer.this.mNextMediaPlayer.gettPlayerReadyFlag()) {
                        if (mediaPlayer == MultiPlayer.this.mCurrentMediaPlayer && MultiPlayer.this.mNextMediaPlayer != null && !MultiPlayer.this.mNextMediaPlayer.gettPlayerReadyFlag()) {
                            MultiPlayer.this.mNextMediaPlayer.release();
                            MultiPlayer.this.mNextMediaPlayer = null;
                        }
                        MusicLogUtils.v("MusicService", "onCompletion: send Track End");
                        MediaPlaybackService.mTrackCompleted = true;
                        if (MediaPlaybackService.this.mPlayPos < MediaPlaybackService.this.mPlayListLen - 1) {
                            MediaPlaybackService.this.mPreAudioId = MediaPlaybackService.this.getAudioId();
                        }
                        MusicLogUtils.d("MusicService", "mIsSupposedToBePlaying:" + MediaPlaybackService.this.mIsSupposedToBePlaying);
                        if (MediaPlaybackService.this.mIsSupposedToBePlaying) {
                            MultiPlayer.this.mHandler.sendEmptyMessage(1);
                            return;
                        }
                        return;
                    }
                    MusicLogUtils.v("MusicService", "onCompletion: Auto switch to Next MediaPlayer and add played track to history.");
                    MultiPlayer.this.mCurrentMediaPlayer.release();
                    MultiPlayer.this.mCurrentMediaPlayer = MultiPlayer.this.mNextMediaPlayer;
                    MultiPlayer.this.mNextMediaPlayer = null;
                    if (!MultiPlayer.this.isPlaying()) {
                        MusicLogUtils.v("MusicService", "isPlaying()==false");
                        MultiPlayer.this.mCurrentMediaPlayer.start();
                    }
                    if (MediaPlaybackService.this.mNextPlayPos != -1) {
                        MusicLogUtils.v("MusicService", "onCompletion: mNextPlayPos is " + MediaPlaybackService.this.mNextPlayPos);
                        MultiPlayer.this.mHandler.sendMessage(MultiPlayer.this.mHandler.obtainMessage(7, MediaPlaybackService.this.mNextPlayPos, -1));
                        return;
                    }
                    return;
                }
                MusicLogUtils.v("MusicService", "onCompletion: Blocked by AVRCP seek");
            }
        };
        MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
                if (i == -22) {
                    MusicLogUtils.d("MusicService", "onError: MEDIA_ERROR_MUSICFX_DIED, extra = " + i2 + ", mPlayPos = " + MediaPlaybackService.this.mPlayPos);
                    MediaPlaybackService.this.mAuxEffectId = 0;
                    MediaPlaybackService.this.sendSessionIdToAudioEffect(true);
                    if (MediaPlaybackService.this.mPlayPos >= 0) {
                        MediaPlaybackService.this.openCurrentAndNext();
                    }
                    return true;
                }
                if (i == 100) {
                    MusicLogUtils.v("MusicService", "onError: MEDIA_ERROR_SERVER_DIED");
                    MediaPlaybackService.this.sendSessionIdToAudioEffect(false);
                    MultiPlayer.this.mIsInitialized = false;
                    MultiPlayer.this.mCurrentMediaPlayer.release();
                    MultiPlayer.this.mCurrentMediaPlayer = new CompatMediaPlayer();
                    MultiPlayer.this.mCurrentMediaPlayer.setWakeMode(MediaPlaybackService.this, 1);
                    MultiPlayer.this.mHandler.sendMessageDelayed(MultiPlayer.this.mHandler.obtainMessage(3), 2000L);
                    return true;
                }
                MusicLogUtils.v("MusicService", "onError: what=" + i + ", extra=" + i2);
                if (i != -38) {
                    MediaPlaybackService.this.mAuxEffectId = 0;
                    MediaPlaybackService.access$5908(MediaPlaybackService.this);
                    MediaPlaybackService.this.sendSessionIdToAudioEffect(true);
                    MultiPlayer.this.handlePlaySongFail(mediaPlayer);
                }
                return true;
            }
        };
        MediaPlayer.OnInfoListener infoListener = new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mediaPlayer, int i, int i2) {
                if (mediaPlayer == null) {
                    return false;
                }
                MusicLogUtils.v("MusicService", "onInfo, what " + i);
                if (i == 801) {
                    if (mediaPlayer.equals(MultiPlayer.this.mCurrentMediaPlayer)) {
                        MediaPlaybackService.this.mMediaSeekable = false;
                    } else {
                        MediaPlaybackService.this.mNextMediaSeekable = false;
                    }
                    return true;
                }
                if (i == 804) {
                    if (mediaPlayer.equals(MultiPlayer.this.mCurrentMediaPlayer)) {
                        MediaPlaybackService.this.mMediaSeekable = true;
                    } else {
                        MediaPlaybackService.this.mNextMediaSeekable = true;
                    }
                    return true;
                }
                return false;
            }
        };
        MediaPlayer.OnPreparedListener preparedlistener = new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                synchronized (MediaPlaybackService.this) {
                    if (mediaPlayer.equals(MultiPlayer.this.mCurrentMediaPlayer) || !MultiPlayer.this.mIsInitialized) {
                        MusicLogUtils.v("MusicService", ">> onPrepared: doseek=" + MediaPlaybackService.this.mDoSeekWhenPrepared + ", mediaseekable=" + MediaPlaybackService.this.mMediaSeekable + ", quietmode=" + MediaPlaybackService.this.mQuietMode);
                        MediaPlaybackService.this.mIsPlayerReady = true;
                        long j = 0;
                        if (MultiPlayer.this.duration() != 0) {
                            if (!MediaPlaybackService.this.mDoSeekWhenPrepared || !MediaPlaybackService.this.mMediaSeekable) {
                                if (!MediaPlaybackService.this.mMediaSeekable) {
                                    MusicLogUtils.v("MusicService", "onPrepared: media NOT seekable, so skip seek!");
                                    MediaPlaybackService.this.mDoSeekWhenPrepared = false;
                                }
                            } else {
                                long j2 = MediaPlaybackService.this.mPreferences.getLong("seekpos", 0L);
                                if (MediaPlaybackService.this.mSeekPositionForAnotherSong != 0) {
                                    j2 = MediaPlaybackService.this.mSeekPositionForAnotherSong;
                                    MediaPlaybackService.this.mSeekPositionForAnotherSong = 0L;
                                }
                                MusicLogUtils.v("MusicService", "seekpos=" + j2);
                                MultiPlayer multiPlayer = MultiPlayer.this;
                                if (j2 >= 0 && j2 <= MultiPlayer.this.duration()) {
                                    j = j2;
                                }
                                multiPlayer.seek(j);
                                MusicLogUtils.v("MusicService", "restored queue, currently at position " + MultiPlayer.this.position() + "/" + MultiPlayer.this.duration() + " (requested " + j2 + ")");
                                MediaPlaybackService.this.mDoSeekWhenPrepared = false;
                            }
                            if (MediaPlaybackService.this.isPodcast()) {
                                long bookmark = MediaPlaybackService.this.getBookmark();
                                if (bookmark > 5000) {
                                    MultiPlayer.this.seek(bookmark - 5000);
                                }
                                MusicLogUtils.v("MusicService", "onPrepared: seek to bookmark: " + bookmark);
                            }
                            MusicLogUtils.v("MusicService", "mQuietMode: " + MediaPlaybackService.this.mQuietMode + ",mIsSupposedToBePlaying:" + MediaPlaybackService.this.mIsSupposedToBePlaying);
                            if (!MediaPlaybackService.this.mQuietMode) {
                                if (MediaPlaybackService.this.mNeedSeekCauseLossBT) {
                                    MusicLogUtils.v("MusicService", "onPrepared: mNeedSeekCauseLossBT: " + MediaPlaybackService.this.mNeedSeekCauseLossBT);
                                    MultiPlayer.this.seek(MediaPlaybackService.this.mPositonCausedByLossBT);
                                }
                                MediaPlaybackService.this.play();
                                MediaPlaybackService.this.mNeedSeekCauseLossBT = false;
                                MediaPlaybackService.this.notifyChange("com.android.music.metachanged");
                            } else {
                                if (MediaPlaybackService.this.mNeedSeekCauseLossBT) {
                                    MusicLogUtils.v("MusicService", "mQuietMode==true,onPrepared: mNeedSeekCauseLossBT: " + MediaPlaybackService.this.mNeedSeekCauseLossBT);
                                    MusicLogUtils.v("MusicService", "<<seek:" + MediaPlaybackService.this.mPositonCausedByLossBT);
                                    MultiPlayer.this.seek(MediaPlaybackService.this.mPositonCausedByLossBT);
                                    MediaPlaybackService.this.notifyChange("com.android.music.metachanged");
                                    MusicLogUtils.v("MusicService", ">>seek:" + MediaPlaybackService.this.mPositonCausedByLossBT);
                                }
                                MediaPlaybackService.this.mNeedSeekCauseLossBT = false;
                                MediaPlaybackService.this.mQuietMode = false;
                            }
                            MusicLogUtils.v("MusicService", "mLossesBTEnablee = " + MediaPlaybackService.this.mLossesBTEnable);
                            if (!MediaPlaybackService.this.mLossesBTEnable) {
                                MediaPlaybackService.this.setNextTrack();
                            }
                            MusicLogUtils.d("MusicService", "<< onPrepared: mQuietMode = " + MediaPlaybackService.this.mQuietMode);
                            return;
                        }
                        MusicLogUtils.v("MusicService", "onPrepared, bad media: duration is 0");
                        boolean z = MediaPlaybackService.this.mQuietMode;
                        if (MediaPlaybackService.this.mShuffleMode == 0 && MediaPlaybackService.this.mRepeatMode != 2 && !MediaPlaybackService.this.mDoSeekWhenPrepared && MediaPlaybackService.this.mPlayPos >= MediaPlaybackService.this.mPlayListLen - 1) {
                            Toast.makeText(MediaPlaybackService.this, R.string.fail_to_start_stream, 0).show();
                        }
                        MediaPlaybackService.this.mQuietMode = true;
                        MultiPlayer.this.errorListener.onError(mediaPlayer, 0, 0);
                        MediaPlaybackService.this.mQuietMode = z;
                        MusicLogUtils.v("MusicService", "<< onPrepared, bad media..");
                        return;
                    }
                    MusicLogUtils.v("MusicService", "preparedlistener finish for next player!");
                    MultiPlayer.this.mCurrentMediaPlayer.setNextMediaPlayer(MultiPlayer.this.mNextMediaPlayer);
                    MultiPlayer.this.mNextMediaPlayer.setPlayerReadyFlag(true);
                    if (Math.abs(MediaPlaybackService.this.mCurrentVolume - 0.2f) < 0.001d) {
                        MultiPlayer.this.mNextMediaPlayer.setVolume(MediaPlaybackService.this.mCurrentVolume, MediaPlaybackService.this.mCurrentVolume);
                        MusicLogUtils.v("MusicService", "set next player volume to " + MediaPlaybackService.this.mCurrentVolume);
                    }
                }
            }
        };

        public MultiPlayer() {
            this.mCurrentMediaPlayer.setWakeMode(MediaPlaybackService.this, 1);
        }

        private boolean setDataSourceImpl(MediaPlayer mediaPlayer, String str, boolean z) {
            MusicLogUtils.v("MusicService", "setDataSourceImpl(" + str + ");async = " + z);
            try {
                mediaPlayer.reset();
                if (z) {
                    mediaPlayer.setOnPreparedListener(this.preparedlistener);
                } else {
                    mediaPlayer.setOnPreparedListener(null);
                }
                if (str.startsWith("content://")) {
                    mediaPlayer.setDataSource(MediaPlaybackService.this, Uri.parse(str));
                } else {
                    mediaPlayer.setDataSource(str);
                }
                if (MediaPlaybackService.this.mAuxEffectId > 0) {
                    mediaPlayer.attachAuxEffect(MediaPlaybackService.this.mAuxEffectId);
                    mediaPlayer.setAuxEffectSendLevel(1.0f);
                    MediaPlaybackService.this.mWhetherAttachWhenPause = false;
                    MusicLogUtils.v("MusicService", "setDataSourceImpl: attachAuxEffect mAuxEffectId = " + MediaPlaybackService.this.mAuxEffectId);
                }
                mediaPlayer.setAudioStreamType(3);
                if (z) {
                    mediaPlayer.prepareAsync();
                } else {
                    mediaPlayer.prepare();
                }
                mediaPlayer.setOnCompletionListener(this.listener);
                mediaPlayer.setOnErrorListener(this.errorListener);
                mediaPlayer.setOnInfoListener(this.infoListener);
                MediaPlaybackService.this.sendSessionIdToAudioEffect(false);
                return true;
            } catch (IOException e) {
                MusicLogUtils.v("MusicService", "setDataSourceImpl: " + e);
                return false;
            } catch (IllegalArgumentException e2) {
                MusicLogUtils.v("MusicService", "setDataSourceImpl: " + e2);
                return false;
            } catch (IllegalStateException e3) {
                MusicLogUtils.v("MusicService", "setDataSourceImpl: " + e3);
                return false;
            }
        }

        public void setNextDataSource(String str) {
            Cursor cursorForId;
            MusicLogUtils.v("MusicService", "setNextDataSource: path = " + str + ", mNextPlayPos = " + MediaPlaybackService.this.mNextPlayPos);
            this.mCurrentMediaPlayer.setNextMediaPlayer(null);
            if (this.mNextMediaPlayer != null) {
                this.mNextMediaPlayer.release();
                this.mNextMediaPlayer = null;
            }
            if (str == null || (cursorForId = MediaPlaybackService.this.getCursorForId(MediaPlaybackService.this.mPlayList[MediaPlaybackService.this.mPlayPos])) == null) {
                return;
            }
            long j = cursorForId.getLong(cursorForId.getColumnIndexOrThrow("duration"));
            MusicLogUtils.v("MusicService", "setNextDataSource: database duration = " + j);
            cursorForId.close();
            if (j >= 1000) {
                Cursor cursorForId2 = MediaPlaybackService.this.getCursorForId(MediaPlaybackService.this.mPlayList[MediaPlaybackService.this.mNextPlayPos]);
                if (cursorForId2 == null) {
                    return;
                }
                long j2 = cursorForId2.getLong(cursorForId2.getColumnIndexOrThrow("duration"));
                String string = cursorForId2.getString(cursorForId2.getColumnIndexOrThrow("_data"));
                MusicLogUtils.d("MusicService", "setNextDataSource: database duration = " + j2 + ", data = " + string + ", isDrm = 0");
                cursorForId2.close();
                if (j2 < 5000) {
                    MusicLogUtils.v("MusicService", "Discard setNextDataSource because the audio is so short.");
                    return;
                }
                if (string != null && string.endsWith(".amr") && j2 > 3600000) {
                    MusicLogUtils.v("MusicService", "Discard setNextDataSource because the amr file is too long.");
                    return;
                }
                this.mNextMediaPlayer = new CompatMediaPlayer();
                this.mNextMediaPlayer.setWakeMode(MediaPlaybackService.this, 1);
                this.mNextMediaPlayer.setAudioSessionId(getAudioSessionId());
                if (!setDataSourceImpl(this.mNextMediaPlayer, str, true)) {
                    this.mNextMediaPlayer.release();
                    this.mNextMediaPlayer = null;
                    return;
                }
                return;
            }
            MusicLogUtils.v("MusicService", "Discard setNextDataSource because the current audio is so short.");
        }

        public boolean isInitialized() {
            return this.mIsInitialized;
        }

        public void start() {
            MusicUtils.debugLog(new Exception("MultiPlayer.start called"));
            this.mCurrentMediaPlayer.start();
        }

        public void stop() {
            this.mCurrentMediaPlayer.reset();
            this.mIsInitialized = false;
        }

        public void release() {
            stop();
            this.mCurrentMediaPlayer.release();
            if (this.mNextMediaPlayer != null) {
                this.mNextMediaPlayer.release();
                this.mNextMediaPlayer = null;
            }
        }

        public void pause() {
            this.mCurrentMediaPlayer.pause();
        }

        public void setHandler(Handler handler) {
            this.mHandler = handler;
        }

        public Handler getHandler() {
            return this.mHandler;
        }

        public long duration() {
            return this.mCurrentMediaPlayer.getDuration();
        }

        public long position() {
            return this.mCurrentMediaPlayer.getCurrentPosition();
        }

        public long seek(long j) {
            this.mCurrentMediaPlayer.seekTo((int) j);
            return j;
        }

        public void setVolume(float f) {
            this.mCurrentMediaPlayer.setVolume(f, f);
            if (this.mNextMediaPlayer != null) {
                this.mNextMediaPlayer.setVolume(f, f);
            }
        }

        public int getAudioSessionId() {
            return this.mCurrentMediaPlayer.getAudioSessionId();
        }

        public void setDataSourceAsync(String str) {
            MusicLogUtils.v("MusicService", "setDataSourceAsync(" + str + ")");
            this.mIsInitialized = setDataSourceImpl(this.mCurrentMediaPlayer, str, true);
            if (this.mIsInitialized) {
                setNextDataSource(null);
            }
        }

        public boolean isPlaying() {
            return this.mCurrentMediaPlayer.isPlaying();
        }

        public void attachAuxEffect(int i) {
            this.mCurrentMediaPlayer.attachAuxEffect(i);
        }

        public void setAuxEffectSendLevel(float f) {
            this.mCurrentMediaPlayer.setAuxEffectSendLevel(f);
        }

        private void handlePlaySongFail(MediaPlayer mediaPlayer) {
            MediaPlaybackService.this.addPlayedTrackToHistory();
            if (this.mNextMediaPlayer == null || !this.mNextMediaPlayer.equals(mediaPlayer) || !MediaPlaybackService.this.mPlayer.isInitialized()) {
                if (MediaPlaybackService.this.mPlayPos >= 0) {
                    if (MediaPlaybackService.this.mShuffleMode == 1 && MediaPlaybackService.access$6108(MediaPlaybackService.this) < 2 && MediaPlaybackService.this.mPlayListLen > 1 && MediaPlaybackService.this.mHistory.size() >= MediaPlaybackService.this.mPlayListLen) {
                        MusicLogUtils.v("MusicService", "the last song play failed at shuffle model");
                        Toast.makeText(MediaPlaybackService.this, R.string.fail_to_start_stream, 0).show();
                        MediaPlaybackService.this.stop(true);
                        MediaPlaybackService.this.notifyChange("com.android.music.quitplayback");
                    } else if (MediaPlaybackService.access$6108(MediaPlaybackService.this) >= 2 || MediaPlaybackService.this.mPlayListLen <= 1 || (MediaPlaybackService.this.mPlayPos >= MediaPlaybackService.this.mPlayListLen - 1 && MediaPlaybackService.this.mRepeatMode != 2)) {
                        if (MediaPlaybackService.this.mPlayPos >= MediaPlaybackService.this.mPlayListLen - 1) {
                            MediaPlaybackService.this.stop(true);
                            MediaPlaybackService.this.notifyChange("com.android.music.quitplayback");
                        } else {
                            MediaPlaybackService.this.stop(true);
                        }
                    } else {
                        MediaPlaybackService.this.gotoNext(false);
                    }
                    if (MediaPlaybackService.this.mOpenFailedCounter == 2) {
                        MediaPlaybackService.this.mOpenFailedCounter = 0;
                        if (!MediaPlaybackService.this.mQuietMode) {
                            MediaPlaybackService.this.showToast(MediaPlaybackService.this.getString(R.string.fail_to_start_stream));
                        }
                        MediaPlaybackService.this.mQuietMode = false;
                        return;
                    }
                    return;
                }
                MusicLogUtils.v("MusicService", "handlePlaySongFail with mPlayPos is -1, return");
                return;
            }
            MediaPlaybackService.this.mPlayer.setNextDataSource(null);
            MusicLogUtils.v("MusicService", "handlePlaySongFail: set next player onError, clear next player and return!");
        }
    }

    static class CompatMediaPlayer extends MediaPlayer implements MediaPlayer.OnCompletionListener {
        private boolean mCompatMode;
        private MediaPlayer.OnCompletionListener mCompletion;
        private boolean mIsPlayerReady = false;
        private MediaPlayer mNextPlayer;

        public CompatMediaPlayer() {
            this.mCompatMode = true;
            try {
                MediaPlayer.class.getMethod("setNextMediaPlayer", MediaPlayer.class);
                this.mCompatMode = false;
            } catch (NoSuchMethodException e) {
                this.mCompatMode = true;
                super.setOnCompletionListener(this);
            }
        }

        @Override
        public void setNextMediaPlayer(MediaPlayer mediaPlayer) {
            if (this.mCompatMode) {
                this.mNextPlayer = mediaPlayer;
            } else {
                super.setNextMediaPlayer(mediaPlayer);
            }
        }

        @Override
        public void setOnCompletionListener(MediaPlayer.OnCompletionListener onCompletionListener) {
            if (this.mCompatMode) {
                this.mCompletion = onCompletionListener;
            } else {
                super.setOnCompletionListener(onCompletionListener);
            }
        }

        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            if (this.mNextPlayer != null) {
                SystemClock.sleep(50L);
                this.mNextPlayer.start();
            }
            this.mCompletion.onCompletion(this);
        }

        public void setPlayerReadyFlag(boolean z) {
            this.mIsPlayerReady = z;
        }

        public boolean gettPlayerReadyFlag() {
            return this.mIsPlayerReady;
        }
    }

    static class ServiceStub extends IMediaPlaybackService.Stub {
        SoftReference<MediaPlaybackService> mService;

        ServiceStub(MediaPlaybackService mediaPlaybackService) {
            this.mService = new SoftReference<>(mediaPlaybackService);
        }

        @Override
        public void openFile(String str) {
            this.mService.get().open(str);
        }

        @Override
        public void open(long[] jArr, int i) {
            this.mService.get().open(jArr, i);
        }

        @Override
        public int getQueuePosition() {
            return this.mService.get().getQueuePosition();
        }

        @Override
        public void setQueuePosition(int i) {
            this.mService.get().setQueuePosition(i);
        }

        @Override
        public boolean isPlaying() {
            return this.mService.get().isPlaying();
        }

        @Override
        public void stop() {
            this.mService.get().stop();
        }

        @Override
        public void pause() {
            this.mService.get().pause();
        }

        @Override
        public void play() {
            this.mService.get().play();
        }

        @Override
        public void prev() {
            this.mService.get().prev();
        }

        @Override
        public void next() {
            this.mService.get().gotoNext(true);
        }

        @Override
        public String getTrackName() {
            return this.mService.get().getTrackName();
        }

        @Override
        public String getAlbumName() {
            return this.mService.get().getAlbumName();
        }

        @Override
        public long getAlbumId() {
            return this.mService.get().getAlbumId();
        }

        @Override
        public String getArtistName() {
            return this.mService.get().getArtistName();
        }

        @Override
        public long getArtistId() {
            return this.mService.get().getArtistId();
        }

        @Override
        public void enqueue(long[] jArr, int i) {
            this.mService.get().enqueue(jArr, i);
        }

        @Override
        public long[] getQueue() {
            return this.mService.get().getQueue();
        }

        @Override
        public void moveQueueItem(int i, int i2) {
            this.mService.get().moveQueueItem(i, i2);
        }

        @Override
        public String getPath() {
            return this.mService.get().getPath();
        }

        @Override
        public long getAudioId() {
            return this.mService.get().getAudioId();
        }

        @Override
        public long position() {
            return this.mService.get().position();
        }

        @Override
        public long duration() {
            return this.mService.get().duration();
        }

        @Override
        public long seek(long j) {
            return this.mService.get().seek(j);
        }

        @Override
        public void setShuffleMode(int i) {
            this.mService.get().setShuffleMode(i);
        }

        @Override
        public int getShuffleMode() {
            return this.mService.get().getShuffleMode();
        }

        @Override
        public int removeTracks(int i, int i2) {
            return this.mService.get().removeTracks(i, i2);
        }

        @Override
        public int removeTrack(long j) {
            return this.mService.get().removeTrack(j);
        }

        @Override
        public void setRepeatMode(int i) {
            this.mService.get().setRepeatMode(i);
        }

        @Override
        public int getRepeatMode() {
            return this.mService.get().getRepeatMode();
        }

        @Override
        public int getMediaMountedCount() {
            return this.mService.get().getMediaMountedCount();
        }

        @Override
        public int getAudioSessionId() {
            return this.mService.get().getAudioSessionId();
        }

        @Override
        public String getMIMEType() {
            return this.mService.get().getMIMEType();
        }

        @Override
        public boolean canUseAsRingtone() {
            return this.mService.get().canUseAsRingtone();
        }
    }

    @Override
    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("" + this.mPlayListLen + " items in queue, currently at index " + this.mPlayPos);
        printWriter.println("Currently loaded:");
        printWriter.println(getArtistName());
        printWriter.println(getAlbumName());
        printWriter.println(getTrackName());
        printWriter.println(getPath());
        printWriter.println("playing: " + this.mIsSupposedToBePlaying);
        printWriter.println("actual: " + this.mPlayer.mCurrentMediaPlayer.isPlaying());
        printWriter.println("shuffle mode: " + this.mShuffleMode);
        MusicUtils.debugDump(printWriter);
    }

    public boolean isCursorNull() {
        return this.mCursor == null;
    }

    public void notifyBTAvrcp(String str) {
        this.mBinderAvrcp.notifyBTAvrcp(str);
    }

    private class AlbumArtWorker extends AsyncTask<Long, Void, Bitmap> {
        private AlbumArtWorker() {
        }

        @Override
        protected Bitmap doInBackground(Long... lArr) {
            long jLongValue = lArr[0].longValue();
            try {
                Bitmap artwork = MusicUtils.getArtwork(MediaPlaybackService.this, -1L, jLongValue, true);
                if (artwork == null) {
                    artwork = MusicUtils.getDefaultArtwork(MediaPlaybackService.this);
                }
                MusicLogUtils.v("MusicService", "AlbumArtWorker: getArtwork returns " + artwork + " for album " + jLongValue);
                return artwork;
            } catch (IllegalArgumentException e) {
                MusicLogUtils.v("MusicService", "AlbumArtWorker called with wrong parameters");
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            MusicLogUtils.v("MusicService", "AlbumArtWorker.onPostExecute: " + MediaPlaybackService.this.mIsSupposedToBePlaying);
            if (MediaPlaybackService.this.mIsSupposedToBePlaying) {
                RemoteControlClient.MetadataEditor metadataEditorEditMetadata = MediaPlaybackService.this.mRemoteControlClient.editMetadata(true);
                metadataEditorEditMetadata.putString(7, MediaPlaybackService.this.getTrackName());
                metadataEditorEditMetadata.putString(1, MediaPlaybackService.this.getAlbumName());
                metadataEditorEditMetadata.putString(2, MediaPlaybackService.this.getArtistName());
                metadataEditorEditMetadata.putString(13, MediaPlaybackService.this.getArtistName());
                metadataEditorEditMetadata.putLong(9, MediaPlaybackService.this.duration());
                if (bitmap != null) {
                    metadataEditorEditMetadata.putBitmap(100, bitmap);
                }
                metadataEditorEditMetadata.apply();
                MediaPlaybackService.this.updateNotification(MediaPlaybackService.this, bitmap);
            }
        }
    }

    private void scanBackward(long j) {
        long j2;
        long jPosition = position();
        MusicLogUtils.v("MusicService", "startSeekPos: " + jPosition);
        if (j >= 5000) {
            j2 = ((j - 5000) * 40) + 5000;
        } else {
            j2 = j * 16;
        }
        long j3 = jPosition - j2;
        if (j3 < 0) {
            prev();
            long jDuration = duration();
            MusicLogUtils.v("MusicService", "duration: " + jDuration);
            j3 += jDuration;
            if (j3 > 0) {
                this.mDoSeekWhenPrepared = true;
                this.mSeekPositionForAnotherSong = j3;
            }
        } else {
            seek(j3);
        }
        this.mRemoteControlClient.setPlaybackState(5, j3, 1.0f);
        this.mRemoteControlClient.setPlaybackState(5, position(), -2.0f);
    }

    private void scanForward(long j) {
        long j2;
        long jPosition = position();
        if (j >= 5000) {
            j2 = ((j - 5000) * 40) + 5000;
        } else {
            j2 = j * 16;
        }
        long j3 = jPosition + j2;
        long jDuration = duration();
        if (j3 >= jDuration) {
            gotoNext(true);
            j3 -= jDuration;
            long jDuration2 = duration();
            if (j3 > jDuration2) {
                j3 = jDuration2;
            }
            this.mDoSeekWhenPrepared = true;
            this.mSeekPositionForAnotherSong = j3;
        } else {
            seek(j3);
        }
        this.mRemoteControlClient.setPlaybackState(4, j3, 1.0f);
        this.mRemoteControlClient.setPlaybackState(4, position(), 2.0f);
    }

    private boolean mediaCanSeek() {
        synchronized (this) {
            boolean z = true;
            if (this.mCursor == null) {
                return true;
            }
            String string = this.mCursor.getString(this.mCursor.getColumnIndexOrThrow("_data"));
            if (string == null || (string.toLowerCase(Locale.ENGLISH).endsWith(".imy") && duration() == 2147483647L)) {
                z = false;
            }
            return z;
        }
    }

    private void sendSessionIdToAudioEffect(boolean z) {
        Intent intent = new Intent("android.media.action.OPEN_AUDIO_EFFECT_CONTROL_SESSION");
        intent.putExtra("android.media.extra.AUDIO_SESSION", getAudioSessionId());
        intent.putExtra("android.media.extra.PACKAGE_NAME", getPackageName());
        intent.putExtra("reset_reverb", z);
        sendBroadcast(intent);
    }

    public String getMIMEType() {
        synchronized (this) {
            if (this.mCursor == null) {
                return null;
            }
            return this.mCursor.getString(this.mCursor.getColumnIndexOrThrow("mime_type"));
        }
    }

    public boolean canUseAsRingtone() {
        boolean zIsDrmCanPlay;
        synchronized (this) {
            zIsDrmCanPlay = isDrmCanPlay(this.mCursor);
        }
        return zIsDrmCanPlay;
    }

    private void checkDrmWhenOpenTrack(Cursor cursor) {
    }

    private void reloadQueueAfterScan() {
        boolean zHasMountedSDcard = MusicUtils.hasMountedSDcard(getApplicationContext());
        MusicLogUtils.v("MusicService", "reloadQueueAfterScan: hasCard = " + zHasMountedSDcard + ", isReloadSuccess = " + this.mIsReloadSuccess);
        if (!zHasMountedSDcard || this.mIsReloadSuccess) {
            return;
        }
        if (this.mPlayList == null || this.mPlayList.length == 0 || this.mPlayListLen == 0) {
            MusicLogUtils.v("MusicService", "reloadQueueAfterScan()-success");
            reloadQueue();
            notifyChange("com.android.music.queuechanged");
            notifyChange("com.android.music.metachanged");
        }
    }

    private void handleSettingModeChange(int i, int i2) {
        MusicLogUtils.v("MusicService", String.format("[AVRCP] CHANGE_SETTING_MODE setting:%d newMode:%d", Integer.valueOf(i), Integer.valueOf(i2)));
        switch (i) {
            case 2:
                if (getShuffleMode() != i2) {
                    setShuffleMode(i2);
                }
                break;
            case 3:
                if (getRepeatMode() != i2) {
                    setRepeatMode(i2);
                }
                break;
            default:
                MusicLogUtils.v("MusicService", "Unsupport AVRCP setting mode!");
                break;
        }
    }

    private boolean isEventFromMonkey() {
        boolean zIsUserAMonkey = ActivityManager.isUserAMonkey();
        MusicLogUtils.v("MusicService", "isEventFromMonkey " + zIsUserAMonkey);
        return zIsUserAMonkey;
    }

    private void addPlayedTrackToHistory() {
        if (this.mShuffleMode == 1) {
            if (this.mPlayPos >= 0) {
                this.mHistory.add(Integer.valueOf(this.mPlayPos));
                MusicLogUtils.v("MusicService", "addPlayedTrackToHistory: mPlayPos = " + this.mPlayPos + ", mHistory = " + this.mHistory);
            }
            if (this.mHistory.size() > 100) {
                this.mHistory.removeElementAt(0);
            }
        }
    }

    private void showToast(CharSequence charSequence) {
        if (this.mToast == null) {
            this.mToast = Toast.makeText(getApplicationContext(), charSequence, 0);
        }
        this.mToast.setText(charSequence);
        this.mToast.show();
    }

    private boolean isDrmCanPlay(Cursor cursor) {
        if (cursor == null) {
            MusicLogUtils.v("MusicService", "isDrmCanPlay to Check given drm with null cursor.");
            return false;
        }
        if (this.mPlayPos >= 0 && this.mPlayPos <= this.mPlayList.length - 1) {
            Cursor cursorQuery = MusicUtils.query(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{"_id"}, "_id=" + this.mPlayList[this.mPlayPos], null, null);
            if (cursorQuery == null || cursorQuery.getCount() == 0) {
                return false;
            }
            cursorQuery.close();
        }
        MusicLogUtils.v("MusicService", "isDrmCanPlay false");
        return false;
    }

    private boolean canGoToNext(long j, long j2) {
        if (this.mRepeatMode != 1) {
            if (this.mIsPlaylistCompleted || j == 0) {
                return true;
            }
            if (j > 0 && j <= 1000 && this.mPlayer.position() > 0) {
                return true;
            }
            if (j > 1000 && j <= j2 && j - position() < j / 10) {
                return true;
            }
            if (j > j2 && j - position() <= 1000) {
                return true;
            }
        }
        return false;
    }

    void quitContentObserverThread() {
        boolean zQuit;
        if (this.mContentObserverThread != null) {
            zQuit = this.mContentObserverThread.quit();
        } else {
            zQuit = false;
        }
        MusicLogUtils.v("MusicService", "Quit ContentObserverThread when service will be destroy: isQuitSuccuss = " + zQuit);
    }

    @Override
    public void onTaskRemoved(Intent intent) {
        super.onTaskRemoved(intent);
        this.mTaskRemoved = true;
    }
}
