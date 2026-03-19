package android.media;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;
import com.android.internal.R;
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
    private final MyOnCompletionListener mCompletionListener = new MyOnCompletionListener();
    private AudioAttributes mAudioAttributes = new AudioAttributes.Builder().setUsage(6).setContentType(4).build();
    private boolean mIsLooping = false;
    private float mVolume = 1.0f;
    private final Object mPlaybackSettingsLock = new Object();

    public Ringtone(Context context, boolean z) {
        this.mContext = context;
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        this.mAllowRemote = z;
        this.mRemotePlayer = z ? this.mAudioManager.getRingtonePlayer() : null;
        this.mRemoteToken = z ? new Binder() : null;
    }

    @Deprecated
    public void setStreamType(int i) {
        PlayerBase.deprecateStreamTypeForPlayback(i, TAG, "setStreamType()");
        setAudioAttributes(new AudioAttributes.Builder().setInternalLegacyStreamType(i).build());
    }

    @Deprecated
    public int getStreamType() {
        return AudioAttributes.toLegacyStreamType(this.mAudioAttributes);
    }

    public void setAudioAttributes(AudioAttributes audioAttributes) throws IllegalArgumentException {
        if (audioAttributes == null) {
            throw new IllegalArgumentException("Invalid null AudioAttributes for Ringtone");
        }
        this.mAudioAttributes = audioAttributes;
        setUri(this.mUri);
    }

    public AudioAttributes getAudioAttributes() {
        return this.mAudioAttributes;
    }

    public void setLooping(boolean z) {
        synchronized (this.mPlaybackSettingsLock) {
            this.mIsLooping = z;
            applyPlaybackProperties_sync();
        }
    }

    public boolean isLooping() {
        boolean z;
        synchronized (this.mPlaybackSettingsLock) {
            z = this.mIsLooping;
        }
        return z;
    }

    public void setVolume(float f) {
        synchronized (this.mPlaybackSettingsLock) {
            if (f < 0.0f) {
                f = 0.0f;
            }
            if (f > 1.0f) {
                f = 1.0f;
            }
            this.mVolume = f;
            applyPlaybackProperties_sync();
        }
    }

    public float getVolume() {
        float f;
        synchronized (this.mPlaybackSettingsLock) {
            f = this.mVolume;
        }
        return f;
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

    public String getTitle(Context context) throws Throwable {
        if (this.mTitle != null) {
            return this.mTitle;
        }
        String title = getTitle(context, this.mUri, true, this.mAllowRemote);
        this.mTitle = title;
        return title;
    }

    public static String getTitle(Context context, Uri uri, boolean z, boolean z2) throws Throwable {
        String string;
        Cursor cursorQuery;
        String title;
        ContentResolver contentResolver = context.getContentResolver();
        if (uri != null) {
            String authorityWithoutUserId = ContentProvider.getAuthorityWithoutUserId(uri.getAuthority());
            string = null;
            try {
                if (!"settings".equals(authorityWithoutUserId)) {
                    try {
                        if (MediaStore.AUTHORITY.equals(authorityWithoutUserId)) {
                            cursorQuery = contentResolver.query(uri, MEDIA_COLUMNS, z2 ? null : MEDIA_SELECTION, null, null);
                            if (cursorQuery != null) {
                                try {
                                    if (cursorQuery.getCount() == 1) {
                                        cursorQuery.moveToFirst();
                                        String string2 = cursorQuery.getString(2);
                                        if (cursorQuery != null) {
                                            cursorQuery.close();
                                        }
                                        return string2;
                                    }
                                } catch (SecurityException e) {
                                    IRingtonePlayer ringtonePlayer = z2 ? ((AudioManager) context.getSystemService("audio")).getRingtonePlayer() : null;
                                    if (ringtonePlayer != null) {
                                        try {
                                            title = ringtonePlayer.getTitle(uri);
                                        } catch (RemoteException e2) {
                                            title = null;
                                            if (cursorQuery != null) {
                                            }
                                            string = title;
                                            if (string == null) {
                                            }
                                            if (string != null) {
                                            }
                                        }
                                        if (cursorQuery != null) {
                                            cursorQuery.close();
                                        }
                                        string = title;
                                    } else {
                                        title = null;
                                        if (cursorQuery != null) {
                                        }
                                        string = title;
                                    }
                                    if (string != null) {
                                    }
                                }
                            }
                        } else {
                            cursorQuery = null;
                        }
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                    } catch (SecurityException e3) {
                        cursorQuery = null;
                    } catch (Throwable th) {
                        th = th;
                        z = 0;
                        if (z != 0) {
                            z.close();
                        }
                        throw th;
                    }
                    if (string == null) {
                        string = uri.getLastPathSegment();
                    }
                } else if (z != 0) {
                    string = context.getString(R.string.ringtone_default_with_actual, getTitle(context, RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.getDefaultType(uri)), false, z2));
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } else {
            string = context.getString(R.string.ringtone_silent);
        }
        if (string != null) {
            return string;
        }
        String string3 = context.getString(R.string.ringtone_unknown);
        return string3 == null ? "" : string3;
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
        boolean z;
        float f;
        if (this.mLocalPlayer != null) {
            if (this.mAudioManager.getStreamVolume(AudioAttributes.toLegacyStreamType(this.mAudioAttributes)) != 0) {
                startLocalPlayer();
                return;
            }
            return;
        }
        if (this.mAllowRemote && this.mRemotePlayer != null) {
            Uri canonicalUri = this.mUri == null ? null : this.mUri.getCanonicalUri();
            synchronized (this.mPlaybackSettingsLock) {
                z = this.mIsLooping;
                f = this.mVolume;
            }
            try {
                this.mRemotePlayer.play(this.mRemoteToken, canonicalUri, this.mAudioAttributes, f, z);
                return;
            } catch (RemoteException e) {
                if (!playFallbackRingtone()) {
                    Log.w(TAG, "Problem playing ringtone: " + e);
                    return;
                }
                return;
            }
        }
        if (!playFallbackRingtone()) {
            Log.w(TAG, "Neither local nor remote playback available");
        }
    }

    public void stop() {
        if (this.mLocalPlayer != null) {
            destroyLocalPlayer();
            return;
        }
        if (this.mAllowRemote && this.mRemotePlayer != null) {
            try {
                this.mRemotePlayer.stop(this.mRemoteToken);
            } catch (RemoteException e) {
                Log.w(TAG, "Problem stopping ringtone: " + e);
            }
        }
    }

    private void destroyLocalPlayer() {
        if (this.mLocalPlayer != null) {
            this.mLocalPlayer.setOnCompletionListener(null);
            this.mLocalPlayer.reset();
            this.mLocalPlayer.release();
            this.mLocalPlayer = null;
            synchronized (sActiveRingtones) {
                sActiveRingtones.remove(this);
            }
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
            int defaultType = RingtoneManager.getDefaultType(this.mUri);
            if (defaultType == -1 || RingtoneManager.getActualDefaultRingtoneUri(this.mContext, defaultType) != null) {
                try {
                    AssetFileDescriptor assetFileDescriptorOpenRawResourceFd = this.mContext.getResources().openRawResourceFd(R.raw.fallbackring);
                    if (assetFileDescriptorOpenRawResourceFd != null) {
                        this.mLocalPlayer = new MediaPlayer();
                        if (assetFileDescriptorOpenRawResourceFd.getDeclaredLength() < 0) {
                            this.mLocalPlayer.setDataSource(assetFileDescriptorOpenRawResourceFd.getFileDescriptor());
                        } else {
                            this.mLocalPlayer.setDataSource(assetFileDescriptorOpenRawResourceFd.getFileDescriptor(), assetFileDescriptorOpenRawResourceFd.getStartOffset(), assetFileDescriptorOpenRawResourceFd.getDeclaredLength());
                        }
                        this.mLocalPlayer.setAudioAttributes(this.mAudioAttributes);
                        synchronized (this.mPlaybackSettingsLock) {
                            applyPlaybackProperties_sync();
                        }
                        this.mLocalPlayer.prepare();
                        startLocalPlayer();
                        assetFileDescriptorOpenRawResourceFd.close();
                        return true;
                    }
                    Log.e(TAG, "Could not load fallback ringtone");
                    return false;
                } catch (Resources.NotFoundException e) {
                    Log.e(TAG, "Fallback ringtone does not exist");
                    return false;
                } catch (IOException e2) {
                    destroyLocalPlayer();
                    Log.e(TAG, "Failed to open fallback ringtone");
                    return false;
                }
            }
            Log.w(TAG, "not playing fallback for " + this.mUri);
            return false;
        }
        return false;
    }

    void setTitle(String str) {
        this.mTitle = str;
    }

    protected void finalize() {
        if (this.mLocalPlayer != null) {
            this.mLocalPlayer.release();
        }
    }

    class MyOnCompletionListener implements MediaPlayer.OnCompletionListener {
        MyOnCompletionListener() {
        }

        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            synchronized (Ringtone.sActiveRingtones) {
                Ringtone.sActiveRingtones.remove(Ringtone.this);
            }
            mediaPlayer.setOnCompletionListener(null);
        }
    }
}
