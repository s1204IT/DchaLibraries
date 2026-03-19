package android.media;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.ProxyInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;
import java.io.IOException;
import java.util.ArrayList;

public class Ringtone {
    private static final boolean LOGD = true;
    private static final String MEDIA_SELECTION = "mime_type LIKE 'audio/%' OR mime_type IN ('application/ogg', 'application/x-flac')";
    private static final String TAG = "Ringtone";
    private final boolean mAllowRemote;
    private final AudioManager mAudioManager;
    private final Context mContext;
    private MediaPlayer mLocalPlayer;
    private final IRingtonePlayer mRemotePlayer;
    private final Binder mRemoteToken;
    private String mTitle;
    private Uri mUri;
    private static final String[] MEDIA_COLUMNS = {"_id", "_data", "title"};
    private static final ArrayList<Ringtone> sActiveRingtones = new ArrayList<>();
    private static final String[] DRM_COLUMNS = {"_id", "_data", "title"};
    private final MyOnCompletionListener mCompletionListener = new MyOnCompletionListener();
    private AudioAttributes mAudioAttributes = new AudioAttributes.Builder().setUsage(6).setContentType(4).build();
    private boolean mIsLooping = false;
    private float mVolume = 1.0f;
    private final Object mPlaybackSettingsLock = new Object();

    public Ringtone(Context context, boolean allowRemote) {
        this.mContext = context;
        this.mAudioManager = (AudioManager) this.mContext.getSystemService(Context.AUDIO_SERVICE);
        this.mAllowRemote = allowRemote;
        this.mRemotePlayer = allowRemote ? this.mAudioManager.getRingtonePlayer() : null;
        this.mRemoteToken = allowRemote ? new Binder() : null;
    }

    @Deprecated
    public void setStreamType(int streamType) {
        setAudioAttributes(new AudioAttributes.Builder().setInternalLegacyStreamType(streamType).build());
    }

    @Deprecated
    public int getStreamType() {
        return AudioAttributes.toLegacyStreamType(this.mAudioAttributes);
    }

    public void setAudioAttributes(AudioAttributes attributes) throws IllegalArgumentException {
        if (attributes == null) {
            throw new IllegalArgumentException("Invalid null AudioAttributes for Ringtone");
        }
        this.mAudioAttributes = attributes;
        setUri(this.mUri);
    }

    public AudioAttributes getAudioAttributes() {
        return this.mAudioAttributes;
    }

    public void setLooping(boolean looping) {
        synchronized (this.mPlaybackSettingsLock) {
            this.mIsLooping = looping;
            applyPlaybackProperties_sync();
        }
    }

    public void setVolume(float volume) {
        synchronized (this.mPlaybackSettingsLock) {
            if (volume < 0.0f) {
                volume = 0.0f;
            }
            if (volume > 1.0f) {
                volume = 1.0f;
            }
            this.mVolume = volume;
            applyPlaybackProperties_sync();
        }
    }

    private void applyPlaybackProperties_sync() {
        if (this.mLocalPlayer != null) {
            this.mLocalPlayer.setVolume(this.mVolume);
            this.mLocalPlayer.setLooping(this.mIsLooping);
        } else {
            if (this.mAllowRemote && this.mRemotePlayer != null) {
                try {
                    this.mRemotePlayer.setPlaybackProperties(this.mRemoteToken, this.mVolume, this.mIsLooping);
                    return;
                } catch (RemoteException e) {
                    Log.w(TAG, "Problem setting playback properties: ", e);
                    return;
                }
            }
            Log.w(TAG, "Neither local nor remote player available when applying playback properties");
        }
    }

    public String getTitle(Context context) {
        if (this.mTitle != null) {
            return this.mTitle;
        }
        String title = getTitle(context, this.mUri, true, this.mAllowRemote);
        this.mTitle = title;
        return title;
    }

    public static String getTitle(Context context, Uri uri, boolean followSettingsUri, boolean allowRemote) {
        ContentResolver res = context.getContentResolver();
        String title = null;
        if (uri != null) {
            String authority = uri.getAuthority();
            if (!"settings".equals(authority)) {
                Cursor cursor = null;
                try {
                    try {
                        if (MediaStore.AUTHORITY.equals(authority)) {
                            cursor = res.query(uri, MEDIA_COLUMNS, allowRemote ? null : MEDIA_SELECTION, null, null);
                            if (cursor != null && cursor.getCount() == 1) {
                                cursor.moveToFirst();
                                return cursor.getString(2);
                            }
                        }
                        if (cursor != null) {
                            cursor.close();
                        }
                    } catch (SecurityException e) {
                        IRingtonePlayer mRemotePlayer = null;
                        if (allowRemote) {
                            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                            mRemotePlayer = audioManager.getRingtonePlayer();
                        }
                        if (mRemotePlayer != null) {
                            try {
                                title = mRemotePlayer.getTitle(uri);
                            } catch (RemoteException e2) {
                            }
                        }
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                    if (title == null) {
                        title = uri.getLastPathSegment();
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            } else if (followSettingsUri) {
                Uri actualUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.getDefaultType(uri));
                String actualTitle = getTitle(context, actualUri, false, allowRemote);
                title = context.getString(17040334, actualTitle);
            }
        }
        if (title != null) {
            return title;
        }
        String title2 = context.getString(17040337);
        return title2 == null ? ProxyInfo.LOCAL_EXCL_LIST : title2;
    }

    public void setUri(Uri uri) {
        destroyLocalPlayer();
        this.mUri = uri;
        if (this.mUri == null) {
            return;
        }
        this.mLocalPlayer = new MediaPlayer();
        try {
            this.mLocalPlayer.setDataSource(this.mContext, this.mUri);
            this.mLocalPlayer.setAudioAttributes(this.mAudioAttributes);
            synchronized (this.mPlaybackSettingsLock) {
                applyPlaybackProperties_sync();
            }
            this.mLocalPlayer.prepare();
        } catch (IOException | SecurityException e) {
            destroyLocalPlayer();
            if (!this.mAllowRemote) {
                Log.w(TAG, "Remote playback not allowed: " + e);
            }
        }
        if (this.mLocalPlayer != null) {
            Log.d(TAG, "Successfully created local player");
        } else {
            Log.d(TAG, "Problem opening; delegating to remote player");
        }
    }

    public Uri getUri() {
        return this.mUri;
    }

    public void play() {
        boolean looping;
        float volume;
        if (this.mLocalPlayer != null) {
            if (this.mAudioManager.getStreamVolume(AudioAttributes.toLegacyStreamType(this.mAudioAttributes)) == 0) {
                return;
            }
            startLocalPlayer();
            return;
        }
        if (this.mAllowRemote && this.mRemotePlayer != null) {
            Uri canonicalUri = this.mUri == null ? null : this.mUri.getCanonicalUri();
            synchronized (this.mPlaybackSettingsLock) {
                looping = this.mIsLooping;
                volume = this.mVolume;
            }
            try {
                this.mRemotePlayer.play(this.mRemoteToken, canonicalUri, this.mAudioAttributes, volume, looping);
                return;
            } catch (RemoteException e) {
                if (playFallbackRingtone()) {
                    return;
                }
                Log.w(TAG, "Problem playing ringtone: " + e);
                return;
            }
        }
        if (playFallbackRingtone()) {
            return;
        }
        Log.w(TAG, "Neither local nor remote playback available");
    }

    public void stop() {
        if (this.mLocalPlayer != null) {
            destroyLocalPlayer();
            return;
        }
        if (!this.mAllowRemote || this.mRemotePlayer == null) {
            return;
        }
        try {
            this.mRemotePlayer.stop(this.mRemoteToken);
        } catch (RemoteException e) {
            Log.w(TAG, "Problem stopping ringtone: " + e);
        }
    }

    private void destroyLocalPlayer() {
        if (this.mLocalPlayer == null) {
            return;
        }
        this.mLocalPlayer.reset();
        this.mLocalPlayer.release();
        this.mLocalPlayer = null;
        synchronized (sActiveRingtones) {
            sActiveRingtones.remove(this);
        }
    }

    private void startLocalPlayer() {
        if (this.mLocalPlayer == null) {
            return;
        }
        synchronized (sActiveRingtones) {
            sActiveRingtones.add(this);
        }
        this.mLocalPlayer.setOnCompletionListener(this.mCompletionListener);
        this.mLocalPlayer.start();
    }

    public boolean isPlaying() {
        if (this.mLocalPlayer != null) {
            return this.mLocalPlayer.isPlaying();
        }
        if (this.mAllowRemote && this.mRemotePlayer != null) {
            try {
                return this.mRemotePlayer.isPlaying(this.mRemoteToken);
            } catch (RemoteException e) {
                Log.w(TAG, "Problem checking ringtone: " + e);
                return false;
            }
        }
        Log.w(TAG, "Neither local nor remote playback available");
        return false;
    }

    private boolean playFallbackRingtone() {
        if (this.mAudioManager.getStreamVolume(AudioAttributes.toLegacyStreamType(this.mAudioAttributes)) != 0) {
            int ringtoneType = RingtoneManager.getDefaultType(this.mUri);
            if (RingtoneManager.getActualDefaultRingtoneUri(this.mContext, ringtoneType) != null) {
                try {
                    AssetFileDescriptor afd = this.mContext.getResources().openRawResourceFd(17825797);
                    if (afd != null) {
                        this.mLocalPlayer = new MediaPlayer();
                        if (afd.getDeclaredLength() < 0) {
                            this.mLocalPlayer.setDataSource(afd.getFileDescriptor());
                        } else {
                            this.mLocalPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getDeclaredLength());
                        }
                        this.mLocalPlayer.setAudioAttributes(this.mAudioAttributes);
                        synchronized (this.mPlaybackSettingsLock) {
                            applyPlaybackProperties_sync();
                        }
                        this.mLocalPlayer.prepare();
                        startLocalPlayer();
                        afd.close();
                        return true;
                    }
                    Log.e(TAG, "Could not load fallback ringtone");
                } catch (Resources.NotFoundException e) {
                    Log.e(TAG, "Fallback ringtone does not exist");
                } catch (IOException e2) {
                    destroyLocalPlayer();
                    Log.e(TAG, "Failed to open fallback ringtone");
                }
            } else {
                Log.w(TAG, "not playing fallback for " + this.mUri);
            }
        }
        return false;
    }

    void setTitle(String title) {
        this.mTitle = title;
    }

    protected void finalize() {
        if (this.mLocalPlayer == null) {
            return;
        }
        this.mLocalPlayer.release();
    }

    class MyOnCompletionListener implements MediaPlayer.OnCompletionListener {
        MyOnCompletionListener() {
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            synchronized (Ringtone.sActiveRingtones) {
                Ringtone.sActiveRingtones.remove(Ringtone.this);
            }
        }
    }
}
