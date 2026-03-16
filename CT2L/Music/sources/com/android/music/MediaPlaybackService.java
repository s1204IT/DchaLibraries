package com.android.music;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import com.android.music.IMediaPlaybackService;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.Random;
import java.util.Vector;

public class MediaPlaybackService extends Service {
    private AudioManager mAudioManager;
    private int mCardId;
    private Cursor mCursor;
    private String mFileToPlay;
    private RemoteControlClient.OnGetPlaybackPositionListener mGListener;
    private MultiPlayer mPlayer;
    private SharedPreferences mPreferences;
    private final Shuffler mRand;
    private RemoteControlClient mRemoteControlClient;
    private RemoteControlClient.OnPlaybackPositionUpdateListener mUListener;
    private PowerManager.WakeLock mWakeLock;
    private int mShuffleMode = 0;
    private int mRepeatMode = 0;
    private int mMediaMountedCount = 0;
    private long[] mAutoShuffleList = null;
    private long[] mPlayList = null;
    private int mPlayListLen = 0;
    private Vector<Integer> mHistory = new Vector<>(100);
    private int mPlayPos = -1;
    private int mNextPlayPos = -1;
    private int mOpenFailedCounter = 0;
    String[] mCursorCols = {"audio._id AS _id", "artist", "album", "title", "_data", "mime_type", "album_id", "artist_id", "is_podcast", "bookmark"};
    private BroadcastReceiver mUnmountReceiver = null;
    private int mServiceStartId = -1;
    private boolean mServiceInUse = false;
    private boolean mIsSupposedToBePlaying = false;
    private boolean mQuietMode = false;
    private boolean mQueueIsSaveable = true;
    private boolean mPausedByTransientLossOfFocus = false;
    private MediaAppWidgetProvider mAppWidgetProvider = MediaAppWidgetProvider.getInstance();
    private Handler mMediaplayerHandler = new Handler() {
        float mCurrentVolume = 1.0f;

        @Override
        public void handleMessage(Message msg) {
            MusicUtils.debugLog("mMediaplayerHandler.handleMessage " + msg.what);
            switch (msg.what) {
                case 1:
                    if (MediaPlaybackService.this.mRepeatMode == 1) {
                        MediaPlaybackService.this.seek(0L);
                        MediaPlaybackService.this.play();
                    } else {
                        MediaPlaybackService.this.gotoNext(false);
                    }
                    break;
                case 2:
                    MediaPlaybackService.this.mWakeLock.release();
                    break;
                case 3:
                    if (!MediaPlaybackService.this.mIsSupposedToBePlaying) {
                        MediaPlaybackService.this.openCurrentAndNext();
                    } else {
                        MediaPlaybackService.this.gotoNext(true);
                    }
                    break;
                case 4:
                    switch (msg.arg1) {
                        case -3:
                            MediaPlaybackService.this.mMediaplayerHandler.removeMessages(6);
                            MediaPlaybackService.this.mMediaplayerHandler.sendEmptyMessage(5);
                            break;
                        case -2:
                            Log.v("MediaPlaybackService", "AudioFocus: received AUDIOFOCUS_LOSS_TRANSIENT");
                            if (MediaPlaybackService.this.isPlaying()) {
                                MediaPlaybackService.this.mPausedByTransientLossOfFocus = true;
                            }
                            MediaPlaybackService.this.pause();
                            break;
                        case -1:
                            Log.v("MediaPlaybackService", "AudioFocus: received AUDIOFOCUS_LOSS");
                            if (MediaPlaybackService.this.isPlaying()) {
                                MediaPlaybackService.this.mPausedByTransientLossOfFocus = false;
                            }
                            MediaPlaybackService.this.pause();
                            break;
                        case 0:
                        default:
                            Log.e("MediaPlaybackService", "Unknown audio focus change code");
                            break;
                        case 1:
                            Log.v("MediaPlaybackService", "AudioFocus: received AUDIOFOCUS_GAIN");
                            if (MediaPlaybackService.this.isPlaying() || !MediaPlaybackService.this.mPausedByTransientLossOfFocus) {
                                MediaPlaybackService.this.mMediaplayerHandler.removeMessages(5);
                                MediaPlaybackService.this.mMediaplayerHandler.sendEmptyMessage(6);
                            } else {
                                MediaPlaybackService.this.mPausedByTransientLossOfFocus = false;
                                this.mCurrentVolume = 0.0f;
                                MediaPlaybackService.this.mPlayer.setVolume(this.mCurrentVolume);
                                MediaPlaybackService.this.play();
                            }
                            break;
                    }
                    break;
                case 5:
                    this.mCurrentVolume -= 0.05f;
                    if (this.mCurrentVolume > 0.2f) {
                        MediaPlaybackService.this.mMediaplayerHandler.sendEmptyMessageDelayed(5, 10L);
                    } else {
                        this.mCurrentVolume = 0.2f;
                    }
                    MediaPlaybackService.this.mPlayer.setVolume(this.mCurrentVolume);
                    break;
                case 6:
                    this.mCurrentVolume += 0.01f;
                    if (this.mCurrentVolume < 1.0f) {
                        MediaPlaybackService.this.mMediaplayerHandler.sendEmptyMessageDelayed(6, 10L);
                    } else {
                        this.mCurrentVolume = 1.0f;
                    }
                    MediaPlaybackService.this.mPlayer.setVolume(this.mCurrentVolume);
                    break;
                case 7:
                    MediaPlaybackService.this.mPlayPos = MediaPlaybackService.this.mNextPlayPos;
                    if (MediaPlaybackService.this.mCursor != null) {
                        MediaPlaybackService.this.mCursor.close();
                        MediaPlaybackService.this.mCursor = null;
                    }
                    if (MediaPlaybackService.this.mPlayPos >= 0 && MediaPlaybackService.this.mPlayPos < MediaPlaybackService.this.mPlayList.length) {
                        MediaPlaybackService.this.mCursor = MediaPlaybackService.this.getCursorForId(MediaPlaybackService.this.mPlayList[MediaPlaybackService.this.mPlayPos]);
                    }
                    MediaPlaybackService.this.notifyChange("com.android.music.metachanged");
                    MediaPlaybackService.this.updateNotification();
                    MediaPlaybackService.this.setNextTrack();
                    break;
            }
        }
    };
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("command");
            MusicUtils.debugLog("mIntentReceiver.onReceive " + action + " / " + cmd);
            if ("next".equals(cmd) || "com.android.music.musicservicecommand.next".equals(action)) {
                MediaPlaybackService.this.gotoNext(true);
                return;
            }
            if ("previous".equals(cmd) || "com.android.music.musicservicecommand.previous".equals(action)) {
                MediaPlaybackService.this.prev();
                return;
            }
            if ("togglepause".equals(cmd) || "com.android.music.musicservicecommand.togglepause".equals(action)) {
                if (MediaPlaybackService.this.isPlaying()) {
                    MediaPlaybackService.this.pause();
                    MediaPlaybackService.this.mPausedByTransientLossOfFocus = false;
                    return;
                } else {
                    MediaPlaybackService.this.play();
                    return;
                }
            }
            if ("pause".equals(cmd) || "com.android.music.musicservicecommand.pause".equals(action)) {
                MediaPlaybackService.this.pause();
                MediaPlaybackService.this.mPausedByTransientLossOfFocus = false;
                return;
            }
            if ("play".equals(cmd)) {
                MediaPlaybackService.this.play();
                return;
            }
            if ("stop".equals(cmd)) {
                MediaPlaybackService.this.pause();
                MediaPlaybackService.this.mPausedByTransientLossOfFocus = false;
                MediaPlaybackService.this.seek(0L);
            } else if ("appwidgetupdate".equals(cmd)) {
                int[] appWidgetIds = intent.getIntArrayExtra("appWidgetIds");
                MediaPlaybackService.this.mAppWidgetProvider.performUpdate(MediaPlaybackService.this, appWidgetIds);
            }
        }
    };
    private AudioManager.OnAudioFocusChangeListener mAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            MediaPlaybackService.this.mMediaplayerHandler.obtainMessage(4, focusChange, 0).sendToTarget();
        }
    };
    private final char[] hexdigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private Handler mDelayedStopHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (!MediaPlaybackService.this.isPlaying() && !MediaPlaybackService.this.mPausedByTransientLossOfFocus && !MediaPlaybackService.this.mServiceInUse && !MediaPlaybackService.this.mMediaplayerHandler.hasMessages(1)) {
                MediaPlaybackService.this.saveQueue(true);
                MediaPlaybackService.this.stopSelf(MediaPlaybackService.this.mServiceStartId);
            }
        }
    };
    private final IBinder mBinder = new ServiceStub(this);

    static int access$2308(MediaPlaybackService x0) {
        int i = x0.mMediaMountedCount;
        x0.mMediaMountedCount = i + 1;
        return i;
    }

    public MediaPlaybackService() {
        this.mRand = new Shuffler();
        this.mGListener = new MockOnGetPlaybackPositionListener();
        this.mUListener = new MockOnPlaybackPositionUpdateListener();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mAudioManager = (AudioManager) getSystemService("audio");
        ComponentName rec = new ComponentName(getPackageName(), MediaButtonIntentReceiver.class.getName());
        this.mAudioManager.registerMediaButtonEventReceiver(rec);
        Intent i = new Intent("android.intent.action.MEDIA_BUTTON");
        i.setComponent(rec);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
        this.mRemoteControlClient = new RemoteControlClient(pi);
        this.mAudioManager.registerRemoteControlClient(this.mRemoteControlClient);
        this.mRemoteControlClient.setTransportControlFlags(189);
        this.mPreferences = getSharedPreferences("Music", 3);
        this.mCardId = MusicUtils.getCardId(this);
        registerExternalStorageListener();
        this.mPlayer = new MultiPlayer();
        this.mPlayer.setHandler(this.mMediaplayerHandler);
        reloadQueue();
        notifyChange("com.android.music.queuechanged");
        notifyChange("com.android.music.metachanged");
        IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction("com.android.music.musicservicecommand");
        commandFilter.addAction("com.android.music.musicservicecommand.togglepause");
        commandFilter.addAction("com.android.music.musicservicecommand.pause");
        commandFilter.addAction("com.android.music.musicservicecommand.next");
        commandFilter.addAction("com.android.music.musicservicecommand.previous");
        commandFilter.addAction("com.android.music.musicservicecommand.rewind");
        registerReceiver(this.mIntentReceiver, commandFilter);
        this.mRemoteControlClient.setOnGetPlaybackPositionListener(this.mGListener);
        this.mRemoteControlClient.setPlaybackPositionUpdateListener(this.mUListener);
        PowerManager pm = (PowerManager) getSystemService("power");
        this.mWakeLock = pm.newWakeLock(1, getClass().getName());
        this.mWakeLock.setReferenceCounted(false);
        Message msg = this.mDelayedStopHandler.obtainMessage();
        this.mDelayedStopHandler.sendMessageDelayed(msg, 60000L);
    }

    @Override
    public void onDestroy() {
        if (isPlaying()) {
            Log.e("MediaPlaybackService", "Service being destroyed while still playing.");
        }
        Intent i = new Intent("android.media.action.CLOSE_AUDIO_EFFECT_CONTROL_SESSION");
        i.putExtra("android.media.extra.AUDIO_SESSION", getAudioSessionId());
        i.putExtra("android.media.extra.PACKAGE_NAME", getPackageName());
        sendBroadcast(i);
        this.mPlayer.release();
        this.mPlayer = null;
        this.mAudioManager.abandonAudioFocus(this.mAudioFocusListener);
        this.mAudioManager.unregisterRemoteControlClient(this.mRemoteControlClient);
        this.mDelayedStopHandler.removeCallbacksAndMessages(null);
        this.mMediaplayerHandler.removeCallbacksAndMessages(null);
        if (this.mCursor != null) {
            this.mCursor.close();
            this.mCursor = null;
        }
        unregisterReceiver(this.mIntentReceiver);
        if (this.mUnmountReceiver != null) {
            unregisterReceiver(this.mUnmountReceiver);
            this.mUnmountReceiver = null;
        }
        this.mWakeLock.release();
        super.onDestroy();
    }

    private class MockOnGetPlaybackPositionListener implements RemoteControlClient.OnGetPlaybackPositionListener {
        private MockOnGetPlaybackPositionListener() {
        }

        @Override
        public long onGetPlaybackPosition() {
            return MediaPlaybackService.this.position();
        }
    }

    private class MockOnPlaybackPositionUpdateListener implements RemoteControlClient.OnPlaybackPositionUpdateListener {
        private MockOnPlaybackPositionUpdateListener() {
        }

        @Override
        public void onPlaybackPositionUpdate(long newPositionMs) {
            MediaPlaybackService.this.seek(newPositionMs);
        }
    }

    private void saveQueue(boolean full) {
        if (this.mQueueIsSaveable) {
            SharedPreferences.Editor ed = this.mPreferences.edit();
            if (full) {
                StringBuilder q = new StringBuilder();
                int len = this.mPlayListLen;
                for (int i = 0; i < len; i++) {
                    long n = this.mPlayList[i];
                    if (n >= 0) {
                        if (n == 0) {
                            q.append("0;");
                        } else {
                            while (n != 0) {
                                int digit = (int) (15 & n);
                                n >>>= 4;
                                q.append(this.hexdigits[digit]);
                            }
                            q.append(";");
                        }
                    }
                }
                ed.putString("queue", q.toString());
                ed.putInt("cardid", this.mCardId);
                if (this.mShuffleMode != 0) {
                    int len2 = this.mHistory.size();
                    q.setLength(0);
                    for (int i2 = 0; i2 < len2; i2++) {
                        int n2 = this.mHistory.get(i2).intValue();
                        if (n2 == 0) {
                            q.append("0;");
                        } else {
                            while (n2 != 0) {
                                int digit2 = n2 & 15;
                                n2 >>>= 4;
                                q.append(this.hexdigits[digit2]);
                            }
                            q.append(";");
                        }
                    }
                    ed.putString("history", q.toString());
                }
            }
            ed.putInt("curpos", this.mPlayPos);
            if (this.mPlayer != null && this.mPlayer.isInitialized()) {
                ed.putLong("seekpos", this.mPlayer.position());
            }
            ed.putInt("repeatmode", this.mRepeatMode);
            ed.putInt("shufflemode", this.mShuffleMode);
            SharedPreferencesCompat.apply(ed);
        }
    }

    private void reloadQueue() {
        String q = null;
        int id = this.mCardId;
        if (this.mPreferences.contains("cardid")) {
            id = this.mPreferences.getInt("cardid", this.mCardId ^ (-1));
        }
        if (id == this.mCardId) {
            q = this.mPreferences.getString("queue", "");
        }
        int qlen = q != null ? q.length() : 0;
        if (qlen > 1) {
            int plen = 0;
            int n = 0;
            int shift = 0;
            for (int i = 0; i < qlen; i++) {
                char c = q.charAt(i);
                if (c == ';') {
                    ensurePlayListCapacity(plen + 1);
                    this.mPlayList[plen] = n;
                    plen++;
                    n = 0;
                    shift = 0;
                } else {
                    if (c >= '0' && c <= '9') {
                        n += (c - '0') << shift;
                    } else if (c >= 'a' && c <= 'f') {
                        n += ((c + '\n') - 97) << shift;
                    } else {
                        plen = 0;
                        break;
                    }
                    shift += 4;
                }
            }
            this.mPlayListLen = plen;
            int pos = this.mPreferences.getInt("curpos", 0);
            if (pos < 0 || pos >= this.mPlayListLen) {
                this.mPlayListLen = 0;
                return;
            }
            this.mPlayPos = pos;
            int repmode = this.mPreferences.getInt("repeatmode", 0);
            if (repmode != 2 && repmode != 1) {
                repmode = 0;
            }
            this.mRepeatMode = repmode;
            Cursor crsr = MusicUtils.query(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{"_id"}, "_id=" + this.mPlayList[this.mPlayPos], null, null);
            if (crsr == null || crsr.getCount() == 0) {
                SystemClock.sleep(3000L);
                crsr = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, this.mCursorCols, "_id=" + this.mPlayList[this.mPlayPos], null, null);
            }
            if (crsr != null) {
                crsr.close();
            }
            this.mOpenFailedCounter = 20;
            this.mQuietMode = true;
            openCurrentAndNext();
            if (this.mIsSupposedToBePlaying) {
                this.mIsSupposedToBePlaying = false;
                notifyChange("com.android.music.playstatechanged");
            }
            this.mQuietMode = false;
            if (this.mPlayer == null || !this.mPlayer.isInitialized()) {
                this.mPlayListLen = 0;
                return;
            }
            long seekpos = this.mPreferences.getLong("seekpos", 0L);
            seek((seekpos < 0 || seekpos >= duration()) ? 0L : seekpos);
            Log.d("MediaPlaybackService", "restored queue, currently at position " + position() + "/" + duration() + " (requested " + seekpos + ")");
            int shufmode = this.mPreferences.getInt("shufflemode", 0);
            if (shufmode != 2 && shufmode != 1) {
                shufmode = 0;
            }
            if (shufmode != 0) {
                String q2 = this.mPreferences.getString("history", "");
                int qlen2 = q2 != null ? q2.length() : 0;
                if (qlen2 > 1) {
                    int n2 = 0;
                    int shift2 = 0;
                    this.mHistory.clear();
                    int i2 = 0;
                    while (true) {
                        if (i2 >= qlen2) {
                            break;
                        }
                        char c2 = q2.charAt(i2);
                        if (c2 == ';') {
                            if (n2 >= this.mPlayListLen) {
                                this.mHistory.clear();
                                break;
                            }
                            this.mHistory.add(Integer.valueOf(n2));
                            n2 = 0;
                            shift2 = 0;
                            i2++;
                        } else {
                            if (c2 >= '0' && c2 <= '9') {
                                n2 += (c2 - '0') << shift2;
                            } else if (c2 < 'a' || c2 > 'f') {
                                break;
                            } else {
                                n2 += ((c2 + '\n') - 97) << shift2;
                            }
                            shift2 += 4;
                            i2++;
                        }
                    }
                }
            }
            if (shufmode == 2 && !makeAutoShuffleList()) {
                shufmode = 0;
            }
            this.mShuffleMode = shufmode;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.mServiceStartId = startId;
        this.mDelayedStopHandler.removeCallbacksAndMessages(null);
        if (intent != null) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("command");
            MusicUtils.debugLog("onStartCommand " + action + " / " + cmd);
            if ("next".equals(cmd) || "com.android.music.musicservicecommand.next".equals(action)) {
                gotoNext(true);
            } else if ("previous".equals(cmd) || "com.android.music.musicservicecommand.previous".equals(action)) {
                if (position() < 2000) {
                    prev();
                } else {
                    seek(0L);
                    play();
                }
            } else if ("togglepause".equals(cmd) || "com.android.music.musicservicecommand.togglepause".equals(action)) {
                if (isPlaying()) {
                    pause();
                    this.mPausedByTransientLossOfFocus = false;
                } else {
                    play();
                }
            } else if ("pause".equals(cmd) || "com.android.music.musicservicecommand.pause".equals(action)) {
                pause();
                this.mPausedByTransientLossOfFocus = false;
            } else if ("play".equals(cmd)) {
                play();
            } else if ("stop".equals(cmd)) {
                pause();
                this.mPausedByTransientLossOfFocus = false;
                seek(0L);
            }
        }
        this.mDelayedStopHandler.removeCallbacksAndMessages(null);
        Message msg = this.mDelayedStopHandler.obtainMessage();
        this.mDelayedStopHandler.sendMessageDelayed(msg, 60000L);
        return 1;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        this.mServiceInUse = false;
        saveQueue(true);
        if (!isPlaying() && !this.mPausedByTransientLossOfFocus) {
            if (this.mPlayListLen > 0 || this.mMediaplayerHandler.hasMessages(1)) {
                Message msg = this.mDelayedStopHandler.obtainMessage();
                this.mDelayedStopHandler.sendMessageDelayed(msg, 60000L);
            } else {
                stopSelf(this.mServiceStartId);
            }
        }
        return true;
    }

    public void closeExternalStorageFiles(String storagePath) {
        stop(true);
        notifyChange("com.android.music.queuechanged");
        notifyChange("com.android.music.metachanged");
    }

    public void registerExternalStorageListener() {
        if (this.mUnmountReceiver == null) {
            this.mUnmountReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action.equals("android.intent.action.MEDIA_EJECT")) {
                        MediaPlaybackService.this.saveQueue(true);
                        MediaPlaybackService.this.mQueueIsSaveable = false;
                        MediaPlaybackService.this.closeExternalStorageFiles(intent.getData().getPath());
                    } else if (action.equals("android.intent.action.MEDIA_MOUNTED")) {
                        MediaPlaybackService.access$2308(MediaPlaybackService.this);
                        MediaPlaybackService.this.mCardId = MusicUtils.getCardId(MediaPlaybackService.this);
                        MediaPlaybackService.this.reloadQueue();
                        MediaPlaybackService.this.mQueueIsSaveable = true;
                        MediaPlaybackService.this.notifyChange("com.android.music.queuechanged");
                        MediaPlaybackService.this.notifyChange("com.android.music.metachanged");
                    }
                }
            };
            IntentFilter iFilter = new IntentFilter();
            iFilter.addAction("android.intent.action.MEDIA_EJECT");
            iFilter.addAction("android.intent.action.MEDIA_MOUNTED");
            iFilter.addDataScheme("file");
            registerReceiver(this.mUnmountReceiver, iFilter);
        }
    }

    private void notifyChange(String what) {
        Intent i = new Intent(what);
        i.putExtra("id", Long.valueOf(getAudioId()));
        i.putExtra("artist", getArtistName());
        i.putExtra("album", getAlbumName());
        i.putExtra("track", getTrackName());
        i.putExtra("playing", isPlaying());
        sendStickyBroadcast(i);
        if (what.equals("com.android.music.playstatechanged")) {
            this.mRemoteControlClient.setPlaybackState(isPlaying() ? 3 : 2, position(), 1.0f);
        } else if (what.equals("com.android.music.metachanged")) {
            RemoteControlClient.MetadataEditor ed = this.mRemoteControlClient.editMetadata(true);
            ed.putString(7, getTrackName());
            ed.putString(1, getAlbumName());
            ed.putString(2, getArtistName());
            ed.putLong(9, duration());
            Bitmap b = MusicUtils.getArtwork(this, getAudioId(), getAlbumId(), false);
            if (b != null) {
                ed.putBitmap(100, b);
            } else {
                Bitmap b2 = MusicUtils.getArtwork(this, getAudioId(), -1L);
                if (b2 != null) {
                    ed.putBitmap(100, b2);
                } else {
                    Log.e("MediaPlaybackService", "Artwork not found");
                }
            }
            ed.apply();
        }
        if (what.equals("com.android.music.queuechanged")) {
            saveQueue(true);
        } else {
            saveQueue(false);
        }
        this.mAppWidgetProvider.notifyChange(this, what);
    }

    private void ensurePlayListCapacity(int size) {
        if (this.mPlayList == null || size > this.mPlayList.length) {
            long[] newlist = new long[size * 2];
            int len = this.mPlayList != null ? this.mPlayList.length : this.mPlayListLen;
            for (int i = 0; i < len; i++) {
                newlist[i] = this.mPlayList[i];
            }
            this.mPlayList = newlist;
        }
    }

    private void addToPlayList(long[] list, int position) {
        int addlen = list.length;
        if (position < 0) {
            this.mPlayListLen = 0;
            position = 0;
        }
        ensurePlayListCapacity(this.mPlayListLen + addlen);
        if (position > this.mPlayListLen) {
            position = this.mPlayListLen;
        }
        int tailsize = this.mPlayListLen - position;
        for (int i = tailsize; i > 0; i--) {
            this.mPlayList[position + i] = this.mPlayList[(position + i) - addlen];
        }
        for (int i2 = 0; i2 < addlen; i2++) {
            this.mPlayList[position + i2] = list[i2];
        }
        this.mPlayListLen += addlen;
        if (this.mPlayListLen == 0) {
            this.mCursor.close();
            this.mCursor = null;
            notifyChange("com.android.music.metachanged");
        }
    }

    public void enqueue(long[] list, int action) {
        synchronized (this) {
            if (action == 2) {
                if (this.mPlayPos + 1 < this.mPlayListLen) {
                    addToPlayList(list, this.mPlayPos + 1);
                    notifyChange("com.android.music.queuechanged");
                } else {
                    addToPlayList(list, Integer.MAX_VALUE);
                    notifyChange("com.android.music.queuechanged");
                    if (action == 1) {
                        this.mPlayPos = this.mPlayListLen - list.length;
                        openCurrentAndNext();
                        play();
                        notifyChange("com.android.music.metachanged");
                        return;
                    }
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

    public void open(long[] list, int position) {
        synchronized (this) {
            if (this.mShuffleMode == 2) {
                this.mShuffleMode = 1;
            }
            long oldId = getAudioId();
            int listlength = list.length;
            boolean newlist = true;
            if (this.mPlayListLen == listlength) {
                newlist = false;
                int i = 0;
                while (true) {
                    if (i >= listlength) {
                        break;
                    }
                    if (list[i] == this.mPlayList[i]) {
                        i++;
                    } else {
                        newlist = true;
                        break;
                    }
                }
            }
            if (newlist) {
                addToPlayList(list, -1);
                notifyChange("com.android.music.queuechanged");
            }
            int i2 = this.mPlayPos;
            if (position >= 0) {
                this.mPlayPos = position;
            } else {
                this.mPlayPos = this.mRand.nextInt(this.mPlayListLen);
            }
            this.mHistory.clear();
            saveBookmarkIfNeeded();
            openCurrentAndNext();
            if (oldId != getAudioId()) {
                notifyChange("com.android.music.metachanged");
            }
        }
    }

    public void moveQueueItem(int index1, int index2) {
        synchronized (this) {
            if (index1 >= this.mPlayListLen) {
                index1 = this.mPlayListLen - 1;
            }
            if (index2 >= this.mPlayListLen) {
                index2 = this.mPlayListLen - 1;
            }
            if (index1 < index2) {
                long tmp = this.mPlayList[index1];
                for (int i = index1; i < index2; i++) {
                    this.mPlayList[i] = this.mPlayList[i + 1];
                }
                this.mPlayList[index2] = tmp;
                if (this.mPlayPos == index1) {
                    this.mPlayPos = index2;
                } else if (this.mPlayPos >= index1 && this.mPlayPos <= index2) {
                    this.mPlayPos--;
                }
            } else if (index2 < index1) {
                long tmp2 = this.mPlayList[index1];
                for (int i2 = index1; i2 > index2; i2--) {
                    this.mPlayList[i2] = this.mPlayList[i2 - 1];
                }
                this.mPlayList[index2] = tmp2;
                if (this.mPlayPos == index1) {
                    this.mPlayPos = index2;
                } else if (this.mPlayPos >= index2 && this.mPlayPos <= index1) {
                    this.mPlayPos++;
                }
            }
            notifyChange("com.android.music.queuechanged");
        }
    }

    public long[] getQueue() {
        long[] list;
        synchronized (this) {
            int len = this.mPlayListLen;
            list = new long[len];
            for (int i = 0; i < len; i++) {
                list[i] = this.mPlayList[i];
            }
        }
        return list;
    }

    private Cursor getCursorForId(long lid) {
        String id = String.valueOf(lid);
        Cursor c = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, this.mCursorCols, "_id=" + id, null, null);
        if (c != null) {
            c.moveToFirst();
        }
        return c;
    }

    private void openCurrentAndNext() {
        synchronized (this) {
            if (this.mCursor != null) {
                this.mCursor.close();
                this.mCursor = null;
            }
            if (this.mPlayListLen != 0) {
                stop(false);
                this.mCursor = getCursorForId(this.mPlayList[this.mPlayPos]);
                while (true) {
                    if (this.mCursor == null || this.mCursor.getCount() == 0 || !open(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/" + this.mCursor.getLong(0))) {
                        if (this.mCursor != null) {
                            this.mCursor.close();
                            this.mCursor = null;
                        }
                        int i = this.mOpenFailedCounter;
                        this.mOpenFailedCounter = i + 1;
                        if (i >= 10 || this.mPlayListLen <= 1) {
                            break;
                        }
                        int pos = getNextPosition(false);
                        if (pos < 0) {
                            gotoIdleState();
                            if (this.mIsSupposedToBePlaying) {
                                this.mIsSupposedToBePlaying = false;
                                notifyChange("com.android.music.playstatechanged");
                            }
                            return;
                        }
                        this.mPlayPos = pos;
                        stop(false);
                        this.mPlayPos = pos;
                        this.mCursor = getCursorForId(this.mPlayList[this.mPlayPos]);
                    } else {
                        if (isPodcast()) {
                            long bookmark = getBookmark();
                            seek(bookmark - 5000);
                        }
                        setNextTrack();
                        return;
                    }
                }
                this.mOpenFailedCounter = 0;
                if (!this.mQuietMode) {
                    Toast.makeText(this, R.string.playback_failed, 0).show();
                }
                Log.d("MediaPlaybackService", "Failed to open file for playback");
                gotoIdleState();
                if (this.mIsSupposedToBePlaying) {
                    this.mIsSupposedToBePlaying = false;
                    notifyChange("com.android.music.playstatechanged");
                }
            }
        }
    }

    private void setNextTrack() {
        this.mNextPlayPos = getNextPosition(false);
        if (this.mNextPlayPos >= 0) {
            long id = this.mPlayList[this.mNextPlayPos];
            if (this.mPlayer.isInitialized()) {
                this.mPlayer.setNextDataSource(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/" + id);
                return;
            } else {
                this.mPlayer.setDataSource(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/" + id);
                Log.d("MediaPlaybackService", "Player not initialized, first initialize it.");
                return;
            }
        }
        this.mPlayer.setNextDataSource(null);
    }

    public boolean open(java.lang.String r11) {
        synchronized (r10) {
            ;
            if (r11 == null) {
                return false;
            } else {
                if (r10.mCursor == null) {
                    r0 = getContentResolver();
                    if (r11.startsWith("content://media/")) {
                        r1 = android.net.Uri.parse(r11);
                        r3 = null;
                        r4 = null;
                    } else {
                        r1 = android.provider.MediaStore.Audio.Media.getContentUriForPath(r11);
                        r3 = "_data=?";
                        r4 = new java.lang.String[]{r11};
                    }
                    r10.mCursor = r0.query(r1, r10.mCursorCols, r3, r4, null);
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
                    if (r10.mPlayer != null) {
                        r10.mFileToPlay = r11;
                        r10.mPlayer.setDataSource(r10.mFileToPlay);
                        if (r10.mPlayer.isInitialized()) {
                            r10.mOpenFailedCounter = 0;
                            return true;
                        }
                    }
                    stop(false);
                    return false;
                }
                if (r10.mPlayer != null) {
                }
                stop(false);
                return false;
            }
        }
    }

    public void play() {
        int status = this.mAudioManager.requestAudioFocus(this.mAudioFocusListener, 3, 1);
        if (status != 0) {
            this.mAudioManager.registerMediaButtonEventReceiver(new ComponentName(getPackageName(), MediaButtonIntentReceiver.class.getName()));
            if (this.mPlayer != null && this.mPlayer.isInitialized()) {
                long duration = this.mPlayer.duration();
                if (this.mRepeatMode != 1 && duration > 2000 && this.mPlayer.position() >= duration - 2000) {
                    gotoNext(true);
                }
                this.mPlayer.start();
                this.mMediaplayerHandler.removeMessages(5);
                this.mMediaplayerHandler.sendEmptyMessage(6);
                updateNotification();
                if (!this.mIsSupposedToBePlaying) {
                    this.mIsSupposedToBePlaying = true;
                    notifyChange("com.android.music.playstatechanged");
                    return;
                }
                return;
            }
            if (this.mPlayListLen <= 0) {
                setShuffleMode(2);
            }
        }
    }

    private void updateNotification() {
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.statusbar);
        views.setImageViewResource(R.id.icon, R.drawable.stat_notify_musicplayer);
        if (getAudioId() < 0) {
            views.setTextViewText(R.id.trackname, getPath());
            views.setTextViewText(R.id.artistalbum, null);
        } else {
            String artist = getArtistName();
            views.setTextViewText(R.id.trackname, getTrackName());
            if (artist == null || artist.equals("<unknown>")) {
                artist = getString(R.string.unknown_artist_name);
            }
            String album = getAlbumName();
            if (album == null || album.equals("<unknown>")) {
                album = getString(R.string.unknown_album_name);
            }
            views.setTextViewText(R.id.artistalbum, getString(R.string.notification_artist_album, new Object[]{artist, album}));
        }
        Notification status = new Notification();
        status.contentView = views;
        status.flags |= 2;
        status.icon = R.drawable.stat_notify_musicplayer;
        status.contentIntent = PendingIntent.getActivity(this, 0, new Intent("com.android.music.PLAYBACK_VIEWER").addFlags(268435456), 0);
        startForeground(1, status);
    }

    private void stop(boolean remove_status_icon) {
        if (this.mPlayer != null && this.mPlayer.isInitialized()) {
            this.mPlayer.stop();
        }
        this.mFileToPlay = null;
        if (this.mCursor != null) {
            this.mCursor.close();
            this.mCursor = null;
        }
        if (remove_status_icon) {
            gotoIdleState();
        } else {
            stopForeground(false);
        }
        if (remove_status_icon) {
            this.mIsSupposedToBePlaying = false;
        }
    }

    public void stop() {
        stop(true);
    }

    public void pause() {
        synchronized (this) {
            this.mMediaplayerHandler.removeMessages(6);
            if (isPlaying()) {
                this.mPlayer.pause();
                gotoIdleState();
                this.mIsSupposedToBePlaying = false;
                notifyChange("com.android.music.playstatechanged");
                saveBookmarkIfNeeded();
            }
        }
    }

    public boolean isPlaying() {
        return this.mPlayer.isPlaying();
    }

    public void prev() {
        synchronized (this) {
            if (this.mShuffleMode == 1) {
                int histsize = this.mHistory.size();
                if (histsize > 1) {
                    this.mHistory.remove(histsize - 1);
                    this.mPlayPos = this.mHistory.get(this.mHistory.size() - 1).intValue();
                } else {
                    return;
                }
            } else if (this.mPlayPos > 0) {
                this.mPlayPos--;
            } else {
                this.mPlayPos = this.mPlayListLen - 1;
            }
            saveBookmarkIfNeeded();
            stop(false);
            openCurrentAndNext();
            play();
            notifyChange("com.android.music.metachanged");
        }
    }

    private int getNextPosition(boolean force) {
        if (this.mRepeatMode == 1 && !force) {
            if (this.mPlayPos < 0) {
                return 0;
            }
            return this.mPlayPos;
        }
        if (this.mShuffleMode == 1) {
            if (this.mPlayPos >= 0 && (this.mHistory.size() == 0 || this.mHistory.get(this.mHistory.size() - 1).intValue() != this.mPlayPos)) {
                this.mHistory.add(Integer.valueOf(this.mPlayPos));
            }
            if (this.mHistory.size() > 100) {
                this.mHistory.removeElementAt(0);
            }
            int numTracks = this.mPlayListLen;
            int[] tracks = new int[numTracks];
            for (int i = 0; i < numTracks; i++) {
                tracks[i] = i;
            }
            int numHistory = this.mHistory.size();
            int numUnplayed = numTracks;
            for (int i2 = 0; i2 < numHistory; i2++) {
                int idx = this.mHistory.get(i2).intValue();
                if (idx < numTracks && tracks[idx] >= 0) {
                    numUnplayed--;
                    tracks[idx] = -1;
                }
            }
            if (numUnplayed <= 0) {
                if (this.mRepeatMode != 2 && !force) {
                    return -1;
                }
                numUnplayed = numTracks;
                for (int i3 = 0; i3 < numTracks; i3++) {
                    tracks[i3] = i3;
                }
            }
            int skip = this.mRand.nextInt(numUnplayed);
            int cnt = -1;
            while (true) {
                cnt++;
                if (tracks[cnt] >= 0 && skip - 1 < 0) {
                    return cnt;
                }
            }
        } else {
            if (this.mShuffleMode == 2) {
                doAutoShuffleUpdate();
                return this.mPlayPos + 1;
            }
            if (this.mPlayPos >= this.mPlayListLen - 1) {
                if (this.mRepeatMode != 0 || force) {
                    return (this.mRepeatMode == 2 || force) ? 0 : -1;
                }
                return -1;
            }
            return this.mPlayPos + 1;
        }
    }

    public void gotoNext(boolean force) {
        synchronized (this) {
            if (this.mPlayListLen <= 0) {
                Log.d("MediaPlaybackService", "No play queue");
                return;
            }
            int pos = getNextPosition(force);
            if (pos < 0) {
                gotoIdleState();
                if (this.mIsSupposedToBePlaying) {
                    this.mIsSupposedToBePlaying = false;
                    notifyChange("com.android.music.playstatechanged");
                }
                return;
            }
            this.mPlayPos = pos;
            saveBookmarkIfNeeded();
            stop(false);
            this.mPlayPos = pos;
            openCurrentAndNext();
            play();
            notifyChange("com.android.music.metachanged");
        }
    }

    private void gotoIdleState() {
        this.mDelayedStopHandler.removeCallbacksAndMessages(null);
        Message msg = this.mDelayedStopHandler.obtainMessage();
        this.mDelayedStopHandler.sendMessageDelayed(msg, 60000L);
        stopForeground(true);
    }

    private void saveBookmarkIfNeeded() {
        try {
            if (isPodcast()) {
                long pos = position();
                long bookmark = getBookmark();
                long duration = duration();
                if (pos >= bookmark || pos + 10000 <= bookmark) {
                    if (pos <= bookmark || pos - 10000 >= bookmark) {
                        if (pos < 15000 || pos + 10000 > duration) {
                            pos = 0;
                        }
                        ContentValues values = new ContentValues();
                        values.put("bookmark", Long.valueOf(pos));
                        Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, this.mCursor.getLong(0));
                        getContentResolver().update(uri, values, null, null);
                    }
                }
            }
        } catch (SQLiteException e) {
        }
    }

    private void doAutoShuffleUpdate() {
        int idx;
        boolean notify = false;
        if (this.mPlayPos > 10) {
            removeTracks(0, this.mPlayPos - 9);
            notify = true;
        }
        int to_add = 7 - (this.mPlayListLen - (this.mPlayPos < 0 ? -1 : this.mPlayPos));
        for (int i = 0; i < to_add; i++) {
            int lookback = this.mHistory.size();
            while (true) {
                idx = this.mRand.nextInt(this.mAutoShuffleList.length);
                if (!wasRecentlyUsed(idx, lookback)) {
                    break;
                } else {
                    lookback /= 2;
                }
            }
            this.mHistory.add(Integer.valueOf(idx));
            if (this.mHistory.size() > 100) {
                this.mHistory.remove(0);
            }
            ensurePlayListCapacity(this.mPlayListLen + 1);
            long[] jArr = this.mPlayList;
            int i2 = this.mPlayListLen;
            this.mPlayListLen = i2 + 1;
            jArr[i2] = this.mAutoShuffleList[idx];
            notify = true;
        }
        if (notify) {
            notifyChange("com.android.music.queuechanged");
        }
    }

    private boolean wasRecentlyUsed(int idx, int lookbacksize) {
        if (lookbacksize == 0) {
            return false;
        }
        int histsize = this.mHistory.size();
        if (histsize < lookbacksize) {
            Log.d("MediaPlaybackService", "lookback too big");
            lookbacksize = histsize;
        }
        int maxidx = histsize - 1;
        for (int i = 0; i < lookbacksize; i++) {
            long entry = this.mHistory.get(maxidx - i).intValue();
            if (entry == idx) {
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

        public int nextInt(int interval) {
            int ret;
            do {
                ret = this.mRandom.nextInt(interval);
                if (ret != this.mPrevious) {
                    break;
                }
            } while (interval > 1);
            this.mPrevious = ret;
            return ret;
        }
    }

    private boolean makeAutoShuffleList() {
        ContentResolver res = getContentResolver();
        Cursor c = null;
        try {
            c = res.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{"_id"}, "is_music=1", null, null);
            if (c == null || c.getCount() == 0) {
                if (c != null) {
                    c.close();
                }
                return false;
            }
            int len = c.getCount();
            long[] list = new long[len];
            for (int i = 0; i < len; i++) {
                c.moveToNext();
                list[i] = c.getLong(0);
            }
            this.mAutoShuffleList = list;
            if (c != null) {
                c.close();
            }
            return true;
        } catch (RuntimeException e) {
            if (c != null) {
                c.close();
            }
            return false;
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
            throw th;
        }
    }

    public int removeTracks(int first, int last) {
        int numremoved = removeTracksInternal(first, last);
        if (numremoved > 0) {
            notifyChange("com.android.music.queuechanged");
        }
        return numremoved;
    }

    private int removeTracksInternal(int first, int last) {
        int i = 0;
        synchronized (this) {
            if (last >= first) {
                if (first < 0) {
                    first = 0;
                }
                if (last >= this.mPlayListLen) {
                    last = this.mPlayListLen - 1;
                }
                boolean gotonext = false;
                if (first <= this.mPlayPos && this.mPlayPos <= last) {
                    this.mPlayPos = first;
                    gotonext = true;
                } else if (this.mPlayPos > last) {
                    this.mPlayPos -= (last - first) + 1;
                }
                int num = (this.mPlayListLen - last) - 1;
                for (int i2 = 0; i2 < num; i2++) {
                    this.mPlayList[first + i2] = this.mPlayList[last + 1 + i2];
                }
                this.mPlayListLen -= (last - first) + 1;
                if (gotonext) {
                    if (this.mPlayListLen == 0) {
                        stop(true);
                        this.mPlayPos = -1;
                        if (this.mCursor != null) {
                            this.mCursor.close();
                            this.mCursor = null;
                        }
                    } else {
                        if (this.mPlayPos >= this.mPlayListLen) {
                            this.mPlayPos = 0;
                        }
                        boolean wasPlaying = isPlaying();
                        stop(false);
                        openCurrentAndNext();
                        if (wasPlaying) {
                            play();
                        }
                    }
                    notifyChange("com.android.music.metachanged");
                }
                i = (last - first) + 1;
            }
        }
        return i;
    }

    public int removeTrack(long id) {
        int numremoved = 0;
        synchronized (this) {
            int i = 0;
            while (i < this.mPlayListLen) {
                if (this.mPlayList[i] == id) {
                    numremoved += removeTracksInternal(i, i);
                    i--;
                }
                i++;
            }
        }
        if (numremoved > 0) {
            notifyChange("com.android.music.queuechanged");
        }
        return numremoved;
    }

    public void setShuffleMode(int shufflemode) {
        synchronized (this) {
            if (this.mShuffleMode != shufflemode || this.mPlayListLen <= 0) {
                this.mShuffleMode = shufflemode;
                if (this.mShuffleMode == 2) {
                    if (makeAutoShuffleList()) {
                        this.mPlayListLen = 0;
                        doAutoShuffleUpdate();
                        this.mPlayPos = 0;
                        openCurrentAndNext();
                        play();
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

    public void setRepeatMode(int repeatmode) {
        synchronized (this) {
            this.mRepeatMode = repeatmode;
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

    public void setQueuePosition(int pos) {
        synchronized (this) {
            stop(false);
            this.mPlayPos = pos;
            openCurrentAndNext();
            play();
            notifyChange("com.android.music.metachanged");
            if (this.mShuffleMode == 2) {
                doAutoShuffleUpdate();
            }
        }
    }

    public String getArtistName() {
        String string;
        synchronized (this) {
            string = (this.mCursor == null || this.mCursor.isBeforeFirst() || this.mCursor.isAfterLast()) ? null : this.mCursor.getString(this.mCursor.getColumnIndexOrThrow("artist"));
        }
        return string;
    }

    public long getArtistId() {
        long j;
        synchronized (this) {
            j = (this.mCursor == null || this.mCursor.isBeforeFirst() || this.mCursor.isAfterLast()) ? -1L : this.mCursor.getLong(this.mCursor.getColumnIndexOrThrow("artist_id"));
        }
        return j;
    }

    public String getAlbumName() {
        String string;
        synchronized (this) {
            string = (this.mCursor == null || this.mCursor.isBeforeFirst() || this.mCursor.isAfterLast()) ? null : this.mCursor.getString(this.mCursor.getColumnIndexOrThrow("album"));
        }
        return string;
    }

    public long getAlbumId() {
        long j;
        synchronized (this) {
            j = (this.mCursor == null || this.mCursor.isBeforeFirst() || this.mCursor.isAfterLast()) ? -1L : this.mCursor.getLong(this.mCursor.getColumnIndexOrThrow("album_id"));
        }
        return j;
    }

    public String getTrackName() {
        String string;
        synchronized (this) {
            string = (this.mCursor == null || this.mCursor.isBeforeFirst() || this.mCursor.isAfterLast()) ? null : this.mCursor.getString(this.mCursor.getColumnIndexOrThrow("title"));
        }
        return string;
    }

    private boolean isPodcast() {
        synchronized (this) {
            if (this.mCursor != null && !this.mCursor.isBeforeFirst() && !this.mCursor.isAfterLast()) {
                z = this.mCursor.getInt(8) > 0;
            }
        }
        return z;
    }

    private long getBookmark() {
        long j;
        synchronized (this) {
            j = (this.mCursor == null || this.mCursor.isBeforeFirst() || this.mCursor.isAfterLast()) ? 0L : this.mCursor.getLong(9);
        }
        return j;
    }

    public long duration() {
        if (this.mPlayer == null || !this.mPlayer.isInitialized()) {
            return -1L;
        }
        return this.mPlayer.duration();
    }

    public long position() {
        if (this.mPlayer == null || !this.mPlayer.isInitialized()) {
            return -1L;
        }
        return this.mPlayer.position();
    }

    public long seek(long pos) {
        if (this.mPlayer != null && this.mPlayer.isInitialized()) {
            if (pos < 0) {
                pos = 0;
            }
            if (pos > this.mPlayer.duration()) {
                pos = this.mPlayer.duration();
            }
            long ret = this.mPlayer.seek(pos);
            this.mRemoteControlClient.setPlaybackState(isPlaying() ? 3 : 2, position(), 1.0f);
            return ret;
        }
        return -1L;
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
            public void onCompletion(MediaPlayer mp) {
                if (mp != MultiPlayer.this.mCurrentMediaPlayer || MultiPlayer.this.mNextMediaPlayer == null) {
                    MediaPlaybackService.this.mWakeLock.acquire(30000L);
                    MultiPlayer.this.mHandler.sendEmptyMessage(1);
                    MultiPlayer.this.mHandler.sendEmptyMessage(2);
                } else {
                    MultiPlayer.this.mCurrentMediaPlayer.release();
                    MultiPlayer.this.mCurrentMediaPlayer = MultiPlayer.this.mNextMediaPlayer;
                    MultiPlayer.this.mNextMediaPlayer = null;
                    MultiPlayer.this.mHandler.sendEmptyMessage(7);
                }
            }
        };
        MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                switch (what) {
                    case 100:
                        MultiPlayer.this.mIsInitialized = false;
                        MultiPlayer.this.mCurrentMediaPlayer.release();
                        MultiPlayer.this.mCurrentMediaPlayer = new CompatMediaPlayer();
                        MultiPlayer.this.mCurrentMediaPlayer.setWakeMode(MediaPlaybackService.this, 1);
                        MultiPlayer.this.mHandler.sendMessageDelayed(MultiPlayer.this.mHandler.obtainMessage(3), 2000L);
                        return true;
                    default:
                        Log.d("MultiPlayer", "Error: " + what + "," + extra);
                        return false;
                }
            }
        };

        public MultiPlayer() {
            this.mCurrentMediaPlayer.setWakeMode(MediaPlaybackService.this, 1);
        }

        public void setDataSource(String path) {
            this.mIsInitialized = setDataSourceImpl(this.mCurrentMediaPlayer, path);
            if (this.mIsInitialized) {
                setNextDataSource(null);
            }
        }

        private boolean setDataSourceImpl(MediaPlayer player, String path) {
            try {
                player.reset();
                player.setOnPreparedListener(null);
                if (path.startsWith("content://")) {
                    player.setDataSource(MediaPlaybackService.this, Uri.parse(path));
                } else {
                    player.setDataSource(path);
                }
                player.setAudioStreamType(3);
                player.prepare();
                player.setOnCompletionListener(this.listener);
                player.setOnErrorListener(this.errorListener);
                Intent i = new Intent("android.media.action.OPEN_AUDIO_EFFECT_CONTROL_SESSION");
                i.putExtra("android.media.extra.AUDIO_SESSION", getAudioSessionId());
                i.putExtra("android.media.extra.PACKAGE_NAME", MediaPlaybackService.this.getPackageName());
                MediaPlaybackService.this.sendBroadcast(i);
                return true;
            } catch (IOException e) {
                return false;
            } catch (IllegalArgumentException e2) {
                return false;
            }
        }

        public void setNextDataSource(String path) {
            this.mCurrentMediaPlayer.setNextMediaPlayer(null);
            if (this.mNextMediaPlayer != null) {
                this.mNextMediaPlayer.release();
                this.mNextMediaPlayer = null;
            }
            if (path != null) {
                this.mNextMediaPlayer = new CompatMediaPlayer();
                this.mNextMediaPlayer.setWakeMode(MediaPlaybackService.this, 1);
                this.mNextMediaPlayer.setAudioSessionId(getAudioSessionId());
                if (setDataSourceImpl(this.mNextMediaPlayer, path)) {
                    this.mCurrentMediaPlayer.setNextMediaPlayer(this.mNextMediaPlayer);
                } else {
                    this.mNextMediaPlayer.release();
                    this.mNextMediaPlayer = null;
                }
            }
        }

        public boolean isInitialized() {
            return this.mIsInitialized;
        }

        public void start() {
            MusicUtils.debugLog(new Exception("MultiPlayer.start called"));
            setVolume(0.0f);
            this.mCurrentMediaPlayer.start();
            SystemClock.sleep(20L);
            setVolume(1.0f);
        }

        public void stop() {
            setVolume(0.0f);
            SystemClock.sleep(20L);
            this.mCurrentMediaPlayer.reset();
            this.mIsInitialized = false;
        }

        public boolean isPlaying() {
            return this.mCurrentMediaPlayer.isPlaying();
        }

        public void release() {
            stop();
            this.mCurrentMediaPlayer.release();
        }

        public void pause() {
            this.mCurrentMediaPlayer.pause();
            this.mCurrentMediaPlayer.setNextMediaPlayer(null);
            if (this.mNextMediaPlayer != null) {
                this.mNextMediaPlayer.release();
                this.mNextMediaPlayer = null;
            }
        }

        public void setHandler(Handler handler) {
            this.mHandler = handler;
        }

        public long duration() {
            return this.mCurrentMediaPlayer.getDuration();
        }

        public long position() {
            return this.mCurrentMediaPlayer.getCurrentPosition();
        }

        public long seek(long whereto) {
            setVolume(0.0f);
            SystemClock.sleep(20L);
            this.mCurrentMediaPlayer.seekTo((int) whereto);
            SystemClock.sleep(20L);
            setVolume(1.0f);
            return whereto;
        }

        public void setVolume(float vol) {
            this.mCurrentMediaPlayer.setVolume(vol, vol);
        }

        public int getAudioSessionId() {
            return this.mCurrentMediaPlayer.getAudioSessionId();
        }
    }

    static class CompatMediaPlayer extends MediaPlayer implements MediaPlayer.OnCompletionListener {
        private boolean mCompatMode;
        private MediaPlayer.OnCompletionListener mCompletion;
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
        public void setNextMediaPlayer(MediaPlayer next) {
            if (this.mCompatMode) {
                this.mNextPlayer = next;
            } else {
                super.setNextMediaPlayer(next);
            }
        }

        @Override
        public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener) {
            if (this.mCompatMode) {
                this.mCompletion = listener;
            } else {
                super.setOnCompletionListener(listener);
            }
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            if (this.mNextPlayer != null) {
                SystemClock.sleep(50L);
                this.mNextPlayer.start();
            }
            this.mCompletion.onCompletion(this);
        }
    }

    static class ServiceStub extends IMediaPlaybackService.Stub {
        WeakReference<MediaPlaybackService> mService;

        ServiceStub(MediaPlaybackService service) {
            this.mService = new WeakReference<>(service);
        }

        @Override
        public void openFile(String path) {
            this.mService.get().open(path);
        }

        @Override
        public void open(long[] list, int position) {
            this.mService.get().open(list, position);
        }

        @Override
        public int getQueuePosition() {
            return this.mService.get().getQueuePosition();
        }

        @Override
        public void setQueuePosition(int index) {
            this.mService.get().setQueuePosition(index);
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
        public void enqueue(long[] list, int action) {
            this.mService.get().enqueue(list, action);
        }

        @Override
        public long[] getQueue() {
            return this.mService.get().getQueue();
        }

        @Override
        public void moveQueueItem(int from, int to) {
            this.mService.get().moveQueueItem(from, to);
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
        public long seek(long pos) {
            return this.mService.get().seek(pos);
        }

        @Override
        public void setShuffleMode(int shufflemode) {
            this.mService.get().setShuffleMode(shufflemode);
        }

        @Override
        public int getShuffleMode() {
            return this.mService.get().getShuffleMode();
        }

        @Override
        public int removeTracks(int first, int last) {
            return this.mService.get().removeTracks(first, last);
        }

        @Override
        public int removeTrack(long id) {
            return this.mService.get().removeTrack(id);
        }

        @Override
        public void setRepeatMode(int repeatmode) {
            this.mService.get().setRepeatMode(repeatmode);
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
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        writer.println("" + this.mPlayListLen + " items in queue, currently at index " + this.mPlayPos);
        writer.println("Currently loaded:");
        writer.println(getArtistName());
        writer.println(getAlbumName());
        writer.println(getTrackName());
        writer.println(getPath());
        writer.println("playing: " + this.mIsSupposedToBePlaying);
        writer.println("actual: " + this.mPlayer.mCurrentMediaPlayer.isPlaying());
        writer.println("shuffle mode: " + this.mShuffleMode);
        MusicUtils.debugDump(writer);
    }
}
