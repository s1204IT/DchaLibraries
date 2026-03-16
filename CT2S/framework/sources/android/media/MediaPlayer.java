package android.media;

import android.app.ActivityThread;
import android.app.backup.FullBackup;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaTimeProvider;
import android.media.SubtitleController;
import android.media.SubtitleTrack;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.util.Log;
import android.util.TimeUtils;
import android.view.Surface;
import android.view.SurfaceHolder;
import com.android.internal.app.IAppOpsService;
import com.android.internal.telephony.IccCardConstants;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;
import libcore.io.IoBridge;
import libcore.io.Libcore;

public class MediaPlayer implements SubtitleController.Listener {
    public static final boolean APPLY_METADATA_FILTER = true;
    public static final boolean BYPASS_METADATA_FILTER = false;
    private static final String IMEDIA_PLAYER = "android.media.IMediaPlayer";
    private static final int INVOKE_ID_ADD_EXTERNAL_SOURCE = 2;
    private static final int INVOKE_ID_ADD_EXTERNAL_SOURCE_FD = 3;
    private static final int INVOKE_ID_DESELECT_TRACK = 5;
    private static final int INVOKE_ID_GET_SELECTED_TRACK = 7;
    private static final int INVOKE_ID_GET_TRACK_INFO = 1;
    private static final int INVOKE_ID_SELECT_TRACK = 4;
    private static final int INVOKE_ID_SET_VIDEO_SCALE_MODE = 6;
    private static final int KEY_PARAMETER_AUDIO_ATTRIBUTES = 1400;
    private static final int MEDIA_BUFFERING_UPDATE = 3;
    private static final int MEDIA_ERROR = 100;
    public static final int MEDIA_ERROR_IO = -1004;
    public static final int MEDIA_ERROR_MALFORMED = -1007;
    public static final int MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK = 200;
    public static final int MEDIA_ERROR_SERVER_DIED = 100;
    public static final int MEDIA_ERROR_TIMED_OUT = -110;
    public static final int MEDIA_ERROR_UNKNOWN = 1;
    public static final int MEDIA_ERROR_UNSUPPORTED = -1010;
    private static final int MEDIA_INFO = 200;
    public static final int MEDIA_INFO_BAD_INTERLEAVING = 800;
    public static final int MEDIA_INFO_BUFFERING_END = 702;
    public static final int MEDIA_INFO_BUFFERING_START = 701;
    public static final int MEDIA_INFO_EXTERNAL_METADATA_UPDATE = 803;
    public static final int MEDIA_INFO_METADATA_UPDATE = 802;
    public static final int MEDIA_INFO_NOT_SEEKABLE = 801;
    public static final int MEDIA_INFO_STARTED_AS_NEXT = 2;
    public static final int MEDIA_INFO_SUBTITLE_TIMED_OUT = 902;
    public static final int MEDIA_INFO_TIMED_TEXT_ERROR = 900;
    public static final int MEDIA_INFO_UNKNOWN = 1;
    public static final int MEDIA_INFO_UNSUPPORTED_SUBTITLE = 901;
    public static final int MEDIA_INFO_VIDEO_RENDERING_START = 3;
    public static final int MEDIA_INFO_VIDEO_TRACK_LAGGING = 700;
    public static final String MEDIA_MIMETYPE_TEXT_CEA_608 = "text/cea-608";
    public static final String MEDIA_MIMETYPE_TEXT_SUBRIP = "application/x-subrip";
    public static final String MEDIA_MIMETYPE_TEXT_VTT = "text/vtt";
    private static final int MEDIA_NOP = 0;
    private static final int MEDIA_PAUSED = 7;
    private static final int MEDIA_PLAYBACK_COMPLETE = 2;
    private static final int MEDIA_PREPARED = 1;
    private static final int MEDIA_SEEK_COMPLETE = 4;
    private static final int MEDIA_SET_VIDEO_SIZE = 5;
    private static final int MEDIA_SKIPPED = 9;
    private static final int MEDIA_STARTED = 6;
    private static final int MEDIA_STOPPED = 8;
    private static final int MEDIA_SUBTITLE_DATA = 201;
    private static final int MEDIA_TIMED_TEXT = 99;
    public static final boolean METADATA_ALL = false;
    public static final boolean METADATA_UPDATE_ONLY = true;
    private static final String TAG = "MediaPlayer";
    public static final int VIDEO_SCALING_MODE_SCALE_TO_FIT = 1;
    public static final int VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING = 2;
    private final IAppOpsService mAppOps;
    private EventHandler mEventHandler;
    private SubtitleTrack[] mInbandSubtitleTracks;
    private int mListenerContext;
    private long mNativeContext;
    private long mNativeSurfaceTexture;
    private OnBufferingUpdateListener mOnBufferingUpdateListener;
    private OnCompletionListener mOnCompletionListener;
    private OnErrorListener mOnErrorListener;
    private OnInfoListener mOnInfoListener;
    private OnPreparedListener mOnPreparedListener;
    private OnSeekCompleteListener mOnSeekCompleteListener;
    private OnSubtitleDataListener mOnSubtitleDataListener;
    private OnTimedTextListener mOnTimedTextListener;
    private OnVideoSizeChangedListener mOnVideoSizeChangedListener;
    private Vector<InputStream> mOpenSubtitleSources;
    private Vector<SubtitleTrack> mOutOfBandSubtitleTracks;
    private boolean mScreenOnWhilePlaying;
    private boolean mStayAwake;
    private SubtitleController mSubtitleController;
    private SurfaceHolder mSurfaceHolder;
    private TimeProvider mTimeProvider;
    private PowerManager.WakeLock mWakeLock = null;
    private int mStreamType = Integer.MIN_VALUE;
    private int mUsage = -1;
    private final Object mInbandSubtitleLock = new Object();
    private int mSelectedSubtitleTrackIndex = -1;
    private OnSubtitleDataListener mSubtitleDataListener = new OnSubtitleDataListener() {
        @Override
        public void onSubtitleData(MediaPlayer mp, SubtitleData data) {
            SubtitleTrack track;
            int index = data.getTrackIndex();
            if (index < MediaPlayer.this.mInbandSubtitleTracks.length && (track = MediaPlayer.this.mInbandSubtitleTracks[index]) != null) {
                track.onData(data);
            }
        }
    };

    public interface OnBufferingUpdateListener {
        void onBufferingUpdate(MediaPlayer mediaPlayer, int i);
    }

    public interface OnCompletionListener {
        void onCompletion(MediaPlayer mediaPlayer);
    }

    public interface OnErrorListener {
        boolean onError(MediaPlayer mediaPlayer, int i, int i2);
    }

    public interface OnInfoListener {
        boolean onInfo(MediaPlayer mediaPlayer, int i, int i2);
    }

    public interface OnPreparedListener {
        void onPrepared(MediaPlayer mediaPlayer);
    }

    public interface OnSeekCompleteListener {
        void onSeekComplete(MediaPlayer mediaPlayer);
    }

    public interface OnSubtitleDataListener {
        void onSubtitleData(MediaPlayer mediaPlayer, SubtitleData subtitleData);
    }

    public interface OnTimedTextListener {
        void onTimedText(MediaPlayer mediaPlayer, TimedText timedText);
    }

    public interface OnVideoSizeChangedListener {
        void onVideoSizeChanged(MediaPlayer mediaPlayer, int i, int i2);
    }

    private native int _getAudioStreamType() throws IllegalStateException;

    private native void _pause() throws IllegalStateException;

    private native void _prepare() throws IllegalStateException, IOException;

    private native void _release();

    private native void _reset();

    private native void _setAudioStreamType(int i);

    private native void _setAuxEffectSendLevel(float f);

    private native void _setDataSource(FileDescriptor fileDescriptor, long j, long j2) throws IllegalStateException, IOException, IllegalArgumentException;

    private native void _setVideoSurface(Surface surface);

    private native void _setVolume(float f, float f2);

    private native void _start() throws IllegalStateException;

    private native void _stop() throws IllegalStateException;

    private native void nativeSetDataSource(IBinder iBinder, String str, String[] strArr, String[] strArr2) throws IllegalStateException, IOException, SecurityException, IllegalArgumentException;

    private final native void native_finalize();

    private final native boolean native_getMetadata(boolean z, boolean z2, Parcel parcel);

    private static final native void native_init();

    private final native int native_invoke(Parcel parcel, Parcel parcel2);

    public static native int native_pullBatteryData(Parcel parcel);

    private final native int native_setMetadataFilter(Parcel parcel);

    private final native int native_setRetransmitEndpoint(String str, int i);

    private final native void native_setup(Object obj);

    private native boolean setParameter(int i, Parcel parcel);

    public native void attachAuxEffect(int i);

    public native int getAudioSessionId();

    public native int getCurrentPosition();

    public native int getDuration();

    public native int getVideoHeight();

    public native int getVideoWidth();

    public native boolean isLooping();

    public native boolean isPlaying();

    public native void prepareAsync() throws IllegalStateException;

    public native void seekTo(int i) throws IllegalStateException;

    public native void setAudioSessionId(int i) throws IllegalStateException, IllegalArgumentException;

    public native void setLooping(boolean z);

    public native void setNextMediaPlayer(MediaPlayer mediaPlayer);

    static {
        System.loadLibrary("media_jni");
        native_init();
    }

    public MediaPlayer() {
        Looper looper = Looper.myLooper();
        if (looper != null) {
            this.mEventHandler = new EventHandler(this, looper);
        } else {
            Looper looper2 = Looper.getMainLooper();
            if (looper2 != null) {
                this.mEventHandler = new EventHandler(this, looper2);
            } else {
                this.mEventHandler = null;
            }
        }
        this.mTimeProvider = new TimeProvider(this);
        this.mOutOfBandSubtitleTracks = new Vector<>();
        this.mOpenSubtitleSources = new Vector<>();
        this.mInbandSubtitleTracks = new SubtitleTrack[0];
        IBinder b = ServiceManager.getService(Context.APP_OPS_SERVICE);
        this.mAppOps = IAppOpsService.Stub.asInterface(b);
        native_setup(new WeakReference(this));
    }

    public Parcel newRequest() {
        Parcel parcel = Parcel.obtain();
        parcel.writeInterfaceToken(IMEDIA_PLAYER);
        return parcel;
    }

    public void invoke(Parcel request, Parcel reply) {
        int retcode = native_invoke(request, reply);
        reply.setDataPosition(0);
        if (retcode != 0) {
            throw new RuntimeException("failure code: " + retcode);
        }
    }

    public void setDisplay(SurfaceHolder sh) {
        Surface surface;
        this.mSurfaceHolder = sh;
        if (sh != null) {
            surface = sh.getSurface();
        } else {
            surface = null;
        }
        _setVideoSurface(surface);
        updateSurfaceScreenOn();
    }

    public void setSurface(Surface surface) {
        if (this.mScreenOnWhilePlaying && surface != null) {
            Log.w(TAG, "setScreenOnWhilePlaying(true) is ineffective for Surface");
        }
        this.mSurfaceHolder = null;
        _setVideoSurface(surface);
        updateSurfaceScreenOn();
    }

    public void setVideoScalingMode(int mode) {
        if (!isVideoScalingModeSupported(mode)) {
            String msg = "Scaling mode " + mode + " is not supported";
            throw new IllegalArgumentException(msg);
        }
        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            request.writeInterfaceToken(IMEDIA_PLAYER);
            request.writeInt(6);
            request.writeInt(mode);
            invoke(request, reply);
        } finally {
            request.recycle();
            reply.recycle();
        }
    }

    public static MediaPlayer create(Context context, Uri uri) {
        return create(context, uri, null);
    }

    public static MediaPlayer create(Context context, Uri uri, SurfaceHolder holder) {
        int s = AudioSystem.newAudioSessionId();
        if (s <= 0) {
            s = 0;
        }
        return create(context, uri, holder, null, s);
    }

    public static MediaPlayer create(Context context, Uri uri, SurfaceHolder holder, AudioAttributes audioAttributes, int audioSessionId) {
        try {
            MediaPlayer mp = new MediaPlayer();
            AudioAttributes aa = audioAttributes != null ? audioAttributes : new AudioAttributes.Builder().build();
            mp.setAudioAttributes(aa);
            mp.setAudioSessionId(audioSessionId);
            mp.setDataSource(context, uri);
            if (holder != null) {
                mp.setDisplay(holder);
            }
            mp.prepare();
            return mp;
        } catch (IOException ex) {
            Log.d(TAG, "create failed:", ex);
            return null;
        } catch (IllegalArgumentException ex2) {
            Log.d(TAG, "create failed:", ex2);
            return null;
        } catch (SecurityException ex3) {
            Log.d(TAG, "create failed:", ex3);
            return null;
        }
    }

    public static MediaPlayer create(Context context, int resid) {
        int s = AudioSystem.newAudioSessionId();
        if (s <= 0) {
            s = 0;
        }
        return create(context, resid, null, s);
    }

    public static MediaPlayer create(Context context, int resid, AudioAttributes audioAttributes, int audioSessionId) {
        try {
            AssetFileDescriptor afd = context.getResources().openRawResourceFd(resid);
            if (afd == null) {
                return null;
            }
            MediaPlayer mp = new MediaPlayer();
            AudioAttributes aa = audioAttributes != null ? audioAttributes : new AudioAttributes.Builder().build();
            mp.setAudioAttributes(aa);
            mp.setAudioSessionId(audioSessionId);
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp.prepare();
            return mp;
        } catch (IOException ex) {
            Log.d(TAG, "create failed:", ex);
            return null;
        } catch (IllegalArgumentException ex2) {
            Log.d(TAG, "create failed:", ex2);
            return null;
        } catch (SecurityException ex3) {
            Log.d(TAG, "create failed:", ex3);
            return null;
        }
    }

    public void setDataSource(Context context, Uri uri) throws IllegalStateException, IOException, SecurityException, IllegalArgumentException {
        setDataSource(context, uri, (Map<String, String>) null);
    }

    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IllegalStateException, IOException, SecurityException, IllegalArgumentException {
        String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            setDataSource(uri.getPath());
            return;
        }
        if ("content".equals(scheme) && "settings".equals(uri.getAuthority()) && (uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.getDefaultType(uri))) == null) {
            throw new FileNotFoundException("Failed to resolve default ringtone");
        }
        AssetFileDescriptor fd = null;
        try {
            ContentResolver resolver = context.getContentResolver();
            AssetFileDescriptor fd2 = resolver.openAssetFileDescriptor(uri, FullBackup.ROOT_TREE_TOKEN);
            if (fd2 == null) {
                if (fd2 != null) {
                    fd2.close();
                }
            } else {
                if (fd2.getDeclaredLength() < 0) {
                    setDataSource(fd2.getFileDescriptor());
                } else {
                    setDataSource(fd2.getFileDescriptor(), fd2.getStartOffset(), fd2.getDeclaredLength());
                }
                if (fd2 != null) {
                    fd2.close();
                }
            }
        } catch (IOException e) {
            if (0 != 0) {
                fd.close();
            }
            Log.d(TAG, "Couldn't open file on client side, trying server side");
            setDataSource(uri.toString(), headers);
        } catch (SecurityException e2) {
            if (0 != 0) {
                fd.close();
            }
            Log.d(TAG, "Couldn't open file on client side, trying server side");
            setDataSource(uri.toString(), headers);
        } catch (Throwable th) {
            if (0 != 0) {
                fd.close();
            }
            throw th;
        }
    }

    public void setDataSource(String path) throws IllegalStateException, IOException, SecurityException, IllegalArgumentException {
        setDataSource(path, (String[]) null, (String[]) null);
    }

    public void setDataSource(String path, Map<String, String> headers) throws IllegalStateException, IOException, SecurityException, IllegalArgumentException {
        String[] keys = null;
        String[] values = null;
        if (headers != null) {
            keys = new String[headers.size()];
            values = new String[headers.size()];
            int i = 0;
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                keys[i] = entry.getKey();
                values[i] = entry.getValue();
                i++;
            }
        }
        setDataSource(path, keys, values);
    }

    private void setDataSource(String path, String[] keys, String[] values) throws IllegalStateException, IOException, SecurityException, IllegalArgumentException {
        Uri uri = Uri.parse(path);
        String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            path = uri.getPath();
        } else if (scheme != null) {
            nativeSetDataSource(MediaHTTPService.createHttpServiceBinderIfNecessary(path), path, keys, values);
            return;
        }
        File file = new File(path);
        if (file.exists()) {
            FileInputStream is = new FileInputStream(file);
            FileDescriptor fd = is.getFD();
            setDataSource(fd);
            is.close();
            return;
        }
        throw new IOException("setDataSource failed.");
    }

    public void setDataSource(FileDescriptor fd) throws IllegalStateException, IOException, IllegalArgumentException {
        setDataSource(fd, 0L, 576460752303423487L);
    }

    public void setDataSource(FileDescriptor fd, long offset, long length) throws IllegalStateException, IOException, IllegalArgumentException {
        _setDataSource(fd, offset, length);
    }

    public void prepare() throws IllegalStateException, IOException {
        _prepare();
        scanInternalSubtitleTracks();
    }

    public void start() throws IllegalStateException {
        if (isRestricted()) {
            _setVolume(0.0f, 0.0f);
        }
        stayAwake(true);
        _start();
    }

    private boolean isRestricted() {
        try {
            int usage = this.mUsage != -1 ? this.mUsage : AudioAttributes.usageForLegacyStreamType(getAudioStreamType());
            int mode = this.mAppOps.checkAudioOperation(28, usage, Process.myUid(), ActivityThread.currentPackageName());
            return mode != 0;
        } catch (RemoteException e) {
            return false;
        }
    }

    private int getAudioStreamType() {
        if (this.mStreamType == Integer.MIN_VALUE) {
            this.mStreamType = _getAudioStreamType();
        }
        return this.mStreamType;
    }

    public void stop() throws IllegalStateException {
        stayAwake(false);
        _stop();
    }

    public void pause() throws IllegalStateException {
        stayAwake(false);
        _pause();
    }

    public void setWakeMode(Context context, int mode) {
        boolean washeld = false;
        if (this.mWakeLock != null) {
            if (this.mWakeLock.isHeld()) {
                washeld = true;
                this.mWakeLock.release();
            }
            this.mWakeLock = null;
        }
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(536870912 | mode, MediaPlayer.class.getName());
        this.mWakeLock.setReferenceCounted(false);
        if (washeld) {
            this.mWakeLock.acquire();
        }
    }

    public void setScreenOnWhilePlaying(boolean screenOn) {
        if (this.mScreenOnWhilePlaying != screenOn) {
            if (screenOn && this.mSurfaceHolder == null) {
                Log.w(TAG, "setScreenOnWhilePlaying(true) is ineffective without a SurfaceHolder");
            }
            this.mScreenOnWhilePlaying = screenOn;
            updateSurfaceScreenOn();
        }
    }

    private void stayAwake(boolean awake) {
        if (this.mWakeLock != null) {
            if (awake && !this.mWakeLock.isHeld()) {
                this.mWakeLock.acquire();
            } else if (!awake && this.mWakeLock.isHeld()) {
                this.mWakeLock.release();
            }
        }
        this.mStayAwake = awake;
        updateSurfaceScreenOn();
    }

    private void updateSurfaceScreenOn() {
        if (this.mSurfaceHolder != null) {
            this.mSurfaceHolder.setKeepScreenOn(this.mScreenOnWhilePlaying && this.mStayAwake);
        }
    }

    public Metadata getMetadata(boolean update_only, boolean apply_filter) {
        Parcel reply = Parcel.obtain();
        Metadata data = new Metadata();
        if (!native_getMetadata(update_only, apply_filter, reply)) {
            reply.recycle();
            return null;
        }
        if (!data.parse(reply)) {
            reply.recycle();
            return null;
        }
        return data;
    }

    public int setMetadataFilter(Set<Integer> allow, Set<Integer> block) {
        Parcel request = newRequest();
        int capacity = request.dataSize() + ((allow.size() + 1 + 1 + block.size()) * 4);
        if (request.dataCapacity() < capacity) {
            request.setDataCapacity(capacity);
        }
        request.writeInt(allow.size());
        for (Integer t : allow) {
            request.writeInt(t.intValue());
        }
        request.writeInt(block.size());
        for (Integer t2 : block) {
            request.writeInt(t2.intValue());
        }
        return native_setMetadataFilter(request);
    }

    public void release() {
        stayAwake(false);
        updateSurfaceScreenOn();
        this.mOnPreparedListener = null;
        this.mOnBufferingUpdateListener = null;
        this.mOnCompletionListener = null;
        this.mOnSeekCompleteListener = null;
        this.mOnErrorListener = null;
        this.mOnInfoListener = null;
        this.mOnVideoSizeChangedListener = null;
        this.mOnTimedTextListener = null;
        if (this.mTimeProvider != null) {
            this.mTimeProvider.close();
            this.mTimeProvider = null;
        }
        this.mOnSubtitleDataListener = null;
        _release();
    }

    public void reset() {
        this.mSelectedSubtitleTrackIndex = -1;
        synchronized (this.mOpenSubtitleSources) {
            for (InputStream is : this.mOpenSubtitleSources) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
            this.mOpenSubtitleSources.clear();
        }
        this.mOutOfBandSubtitleTracks.clear();
        this.mInbandSubtitleTracks = new SubtitleTrack[0];
        if (this.mSubtitleController != null) {
            this.mSubtitleController.reset();
        }
        if (this.mTimeProvider != null) {
            this.mTimeProvider.close();
            this.mTimeProvider = null;
        }
        stayAwake(false);
        _reset();
        if (this.mEventHandler != null) {
            this.mEventHandler.removeCallbacksAndMessages(null);
        }
    }

    public void setAudioStreamType(int streamtype) {
        _setAudioStreamType(streamtype);
        this.mStreamType = streamtype;
    }

    public void setAudioAttributes(AudioAttributes attributes) throws IllegalArgumentException {
        if (attributes == null) {
            throw new IllegalArgumentException("Cannot set AudioAttributes to null");
        }
        this.mUsage = attributes.getUsage();
        Parcel pattributes = Parcel.obtain();
        attributes.writeToParcel(pattributes, 1);
        setParameter(KEY_PARAMETER_AUDIO_ATTRIBUTES, pattributes);
        pattributes.recycle();
    }

    public void setVolume(float leftVolume, float rightVolume) {
        if (!isRestricted()) {
            _setVolume(leftVolume, rightVolume);
        }
    }

    public void setVolume(float volume) {
        setVolume(volume, volume);
    }

    public void setAuxEffectSendLevel(float level) {
        if (!isRestricted()) {
            _setAuxEffectSendLevel(level);
        }
    }

    public static class TrackInfo implements Parcelable {
        static final Parcelable.Creator<TrackInfo> CREATOR = new Parcelable.Creator<TrackInfo>() {
            @Override
            public TrackInfo createFromParcel(Parcel in) {
                return new TrackInfo(in);
            }

            @Override
            public TrackInfo[] newArray(int size) {
                return new TrackInfo[size];
            }
        };
        public static final int MEDIA_TRACK_TYPE_AUDIO = 2;
        public static final int MEDIA_TRACK_TYPE_SUBTITLE = 4;
        public static final int MEDIA_TRACK_TYPE_TIMEDTEXT = 3;
        public static final int MEDIA_TRACK_TYPE_UNKNOWN = 0;
        public static final int MEDIA_TRACK_TYPE_VIDEO = 1;
        final MediaFormat mFormat;
        final int mTrackType;

        public int getTrackType() {
            return this.mTrackType;
        }

        public String getLanguage() {
            String language = this.mFormat.getString("language");
            return language == null ? "und" : language;
        }

        public MediaFormat getFormat() {
            if (this.mTrackType == 3 || this.mTrackType == 4) {
                return this.mFormat;
            }
            return null;
        }

        TrackInfo(Parcel in) {
            this.mTrackType = in.readInt();
            String language = in.readString();
            if (this.mTrackType == 3) {
                this.mFormat = MediaFormat.createSubtitleFormat(MediaPlayer.MEDIA_MIMETYPE_TEXT_SUBRIP, language);
                return;
            }
            if (this.mTrackType == 4) {
                String mime = in.readString();
                this.mFormat = MediaFormat.createSubtitleFormat(mime, language);
                this.mFormat.setInteger(MediaFormat.KEY_IS_AUTOSELECT, in.readInt());
                this.mFormat.setInteger(MediaFormat.KEY_IS_DEFAULT, in.readInt());
                this.mFormat.setInteger(MediaFormat.KEY_IS_FORCED_SUBTITLE, in.readInt());
                return;
            }
            this.mFormat = new MediaFormat();
            this.mFormat.setString("language", language);
        }

        TrackInfo(int type, MediaFormat format) {
            this.mTrackType = type;
            this.mFormat = format;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.mTrackType);
            dest.writeString(getLanguage());
            if (this.mTrackType == 4) {
                dest.writeString(this.mFormat.getString(MediaFormat.KEY_MIME));
                dest.writeInt(this.mFormat.getInteger(MediaFormat.KEY_IS_AUTOSELECT));
                dest.writeInt(this.mFormat.getInteger(MediaFormat.KEY_IS_DEFAULT));
                dest.writeInt(this.mFormat.getInteger(MediaFormat.KEY_IS_FORCED_SUBTITLE));
            }
        }

        public String toString() {
            StringBuilder out = new StringBuilder(128);
            out.append(getClass().getName());
            out.append('{');
            switch (this.mTrackType) {
                case 1:
                    out.append("VIDEO");
                    break;
                case 2:
                    out.append("AUDIO");
                    break;
                case 3:
                    out.append("TIMEDTEXT");
                    break;
                case 4:
                    out.append("SUBTITLE");
                    break;
                default:
                    out.append(IccCardConstants.INTENT_VALUE_ICC_UNKNOWN);
                    break;
            }
            out.append(", " + this.mFormat.toString());
            out.append("}");
            return out.toString();
        }
    }

    public TrackInfo[] getTrackInfo() throws IllegalStateException {
        TrackInfo[] trackInfo = getInbandTrackInfo();
        TrackInfo[] allTrackInfo = new TrackInfo[trackInfo.length + this.mOutOfBandSubtitleTracks.size()];
        System.arraycopy(trackInfo, 0, allTrackInfo, 0, trackInfo.length);
        int i = trackInfo.length;
        for (SubtitleTrack track : this.mOutOfBandSubtitleTracks) {
            int type = track.isTimedText() ? 3 : 4;
            allTrackInfo[i] = new TrackInfo(type, track.getFormat());
            i++;
        }
        return allTrackInfo;
    }

    private TrackInfo[] getInbandTrackInfo() throws IllegalStateException {
        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            request.writeInterfaceToken(IMEDIA_PLAYER);
            request.writeInt(1);
            invoke(request, reply);
            TrackInfo[] trackInfo = (TrackInfo[]) reply.createTypedArray(TrackInfo.CREATOR);
            return trackInfo;
        } finally {
            request.recycle();
            reply.recycle();
        }
    }

    private static boolean availableMimeTypeForExternalSource(String mimeType) {
        return MEDIA_MIMETYPE_TEXT_SUBRIP.equals(mimeType);
    }

    public void setSubtitleAnchor(SubtitleController controller, SubtitleController.Anchor anchor) {
        this.mSubtitleController = controller;
        this.mSubtitleController.setAnchor(anchor);
    }

    @Override
    public void onSubtitleTrackSelected(SubtitleTrack track) {
        if (this.mSelectedSubtitleTrackIndex >= 0) {
            try {
                selectOrDeselectInbandTrack(this.mSelectedSubtitleTrackIndex, false);
            } catch (IllegalStateException e) {
            }
            this.mSelectedSubtitleTrackIndex = -1;
        }
        setOnSubtitleDataListener(null);
        if (track != null) {
            for (int i = 0; i < this.mInbandSubtitleTracks.length; i++) {
                if (this.mInbandSubtitleTracks[i] == track) {
                    Log.v(TAG, "Selecting subtitle track " + i);
                    this.mSelectedSubtitleTrackIndex = i;
                    try {
                        selectOrDeselectInbandTrack(this.mSelectedSubtitleTrackIndex, true);
                    } catch (IllegalStateException e2) {
                    }
                    setOnSubtitleDataListener(this.mSubtitleDataListener);
                    return;
                }
            }
        }
    }

    public void addSubtitleSource(final InputStream is, final MediaFormat format) throws IllegalStateException {
        synchronized (this.mOpenSubtitleSources) {
            this.mOpenSubtitleSources.add(is);
        }
        final HandlerThread thread = new HandlerThread("SubtitleReadThread", 9);
        thread.start();
        Handler handler = new Handler(thread.getLooper());
        handler.post(new Runnable() {
            private int addTrack() {
                SubtitleTrack track;
                if (is == null || MediaPlayer.this.mSubtitleController == null || (track = MediaPlayer.this.mSubtitleController.addTrack(format)) == null) {
                    return 901;
                }
                Scanner scanner = new Scanner(is, "UTF-8");
                String contents = scanner.useDelimiter("\\A").next();
                synchronized (MediaPlayer.this.mOpenSubtitleSources) {
                    MediaPlayer.this.mOpenSubtitleSources.remove(is);
                }
                scanner.close();
                MediaPlayer.this.mOutOfBandSubtitleTracks.add(track);
                track.onData(contents.getBytes(), true, -1L);
                return 803;
            }

            @Override
            public void run() {
                int res = addTrack();
                if (MediaPlayer.this.mEventHandler != null) {
                    Message m = MediaPlayer.this.mEventHandler.obtainMessage(200, res, 0, null);
                    MediaPlayer.this.mEventHandler.sendMessage(m);
                }
                thread.getLooper().quitSafely();
            }
        });
    }

    private void scanInternalSubtitleTracks() {
        if (this.mSubtitleController == null) {
            Log.e(TAG, "Should have subtitle controller already set");
            return;
        }
        TrackInfo[] tracks = getInbandTrackInfo();
        synchronized (this.mInbandSubtitleLock) {
            SubtitleTrack[] inbandTracks = new SubtitleTrack[tracks.length];
            for (int i = 0; i < tracks.length; i++) {
                if (tracks[i].getTrackType() == 4) {
                    if (i < this.mInbandSubtitleTracks.length) {
                        inbandTracks[i] = this.mInbandSubtitleTracks[i];
                    } else {
                        SubtitleTrack track = this.mSubtitleController.addTrack(tracks[i].getFormat());
                        inbandTracks[i] = track;
                    }
                }
            }
            this.mInbandSubtitleTracks = inbandTracks;
        }
        this.mSubtitleController.selectDefaultTrack();
    }

    public void addTimedTextSource(String path, String mimeType) throws IllegalStateException, IOException, IllegalArgumentException {
        if (!availableMimeTypeForExternalSource(mimeType)) {
            String msg = "Illegal mimeType for timed text source: " + mimeType;
            throw new IllegalArgumentException(msg);
        }
        File file = new File(path);
        if (file.exists()) {
            FileInputStream is = new FileInputStream(file);
            FileDescriptor fd = is.getFD();
            addTimedTextSource(fd, mimeType);
            is.close();
            return;
        }
        throw new IOException(path);
    }

    public void addTimedTextSource(Context context, Uri uri, String mimeType) throws IllegalStateException, IOException, IllegalArgumentException {
        String scheme = uri.getScheme();
        if (scheme == null || scheme.equals(ContentResolver.SCHEME_FILE)) {
            addTimedTextSource(uri.getPath(), mimeType);
            return;
        }
        AssetFileDescriptor fd = null;
        try {
            ContentResolver resolver = context.getContentResolver();
            AssetFileDescriptor fd2 = resolver.openAssetFileDescriptor(uri, FullBackup.ROOT_TREE_TOKEN);
            if (fd2 == null) {
                if (fd2 != null) {
                    fd2.close();
                }
            } else {
                addTimedTextSource(fd2.getFileDescriptor(), mimeType);
                if (fd2 != null) {
                    fd2.close();
                }
            }
        } catch (IOException e) {
            if (0 != 0) {
                fd.close();
            }
        } catch (SecurityException e2) {
            if (0 != 0) {
                fd.close();
            }
        } catch (Throwable th) {
            if (0 != 0) {
                fd.close();
            }
            throw th;
        }
    }

    public void addTimedTextSource(FileDescriptor fd, String mimeType) throws IllegalStateException, IllegalArgumentException {
        addTimedTextSource(fd, 0L, 576460752303423487L, mimeType);
    }

    public void addTimedTextSource(FileDescriptor fd, final long offset, final long length, String mime) throws IllegalStateException, IllegalArgumentException {
        if (!availableMimeTypeForExternalSource(mime)) {
            throw new IllegalArgumentException("Illegal mimeType for timed text source: " + mime);
        }
        try {
            final FileDescriptor fd2 = Libcore.os.dup(fd);
            MediaFormat fFormat = new MediaFormat();
            fFormat.setString(MediaFormat.KEY_MIME, mime);
            fFormat.setInteger(MediaFormat.KEY_IS_TIMED_TEXT, 1);
            Context context = ActivityThread.currentApplication();
            if (this.mSubtitleController == null) {
                this.mSubtitleController = new SubtitleController(context, this.mTimeProvider, this);
                this.mSubtitleController.setAnchor(new SubtitleController.Anchor() {
                    @Override
                    public void setSubtitleWidget(SubtitleTrack.RenderingWidget subtitleWidget) {
                    }

                    @Override
                    public Looper getSubtitleLooper() {
                        return Looper.getMainLooper();
                    }
                });
            }
            if (!this.mSubtitleController.hasRendererFor(fFormat)) {
                this.mSubtitleController.registerRenderer(new SRTRenderer(context, this.mEventHandler));
            }
            final SubtitleTrack track = this.mSubtitleController.addTrack(fFormat);
            this.mOutOfBandSubtitleTracks.add(track);
            final HandlerThread thread = new HandlerThread("TimedTextReadThread", 9);
            thread.start();
            Handler handler = new Handler(thread.getLooper());
            handler.post(new Runnable() {
                private int addTrack() {
                    int i;
                    InputStream is = null;
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    try {
                        try {
                            Libcore.os.lseek(fd2, offset, OsConstants.SEEK_SET);
                            byte[] buffer = new byte[4096];
                            long total = 0;
                            while (total < length) {
                                int bytesToRead = (int) Math.min(buffer.length, length - total);
                                int bytes = IoBridge.read(fd2, buffer, 0, bytesToRead);
                                if (bytes < 0) {
                                    break;
                                }
                                bos.write(buffer, 0, bytes);
                                total += (long) bytes;
                            }
                            track.onData(bos.toByteArray(), true, -1L);
                            i = 803;
                            if (0 != 0) {
                                try {
                                    is.close();
                                } catch (IOException e) {
                                    Log.e(MediaPlayer.TAG, e.getMessage(), e);
                                }
                            }
                        } catch (Exception e2) {
                            Log.e(MediaPlayer.TAG, e2.getMessage(), e2);
                            i = MediaPlayer.MEDIA_INFO_TIMED_TEXT_ERROR;
                            if (0 != 0) {
                                try {
                                    is.close();
                                } catch (IOException e3) {
                                    Log.e(MediaPlayer.TAG, e3.getMessage(), e3);
                                }
                            }
                        }
                        return i;
                    } catch (Throwable th) {
                        if (0 != 0) {
                            try {
                                is.close();
                            } catch (IOException e4) {
                                Log.e(MediaPlayer.TAG, e4.getMessage(), e4);
                            }
                        }
                        throw th;
                    }
                }

                @Override
                public void run() {
                    int res = addTrack();
                    if (MediaPlayer.this.mEventHandler != null) {
                        Message m = MediaPlayer.this.mEventHandler.obtainMessage(200, res, 0, null);
                        MediaPlayer.this.mEventHandler.sendMessage(m);
                    }
                    thread.getLooper().quitSafely();
                }
            });
        } catch (ErrnoException ex) {
            Log.e(TAG, ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    public int getSelectedTrack(int trackType) throws IllegalStateException {
        SubtitleTrack subtitleTrack;
        int index;
        if (trackType == 4 && this.mSubtitleController != null && (subtitleTrack = this.mSubtitleController.getSelectedTrack()) != null && (index = this.mOutOfBandSubtitleTracks.indexOf(subtitleTrack)) >= 0) {
            return this.mInbandSubtitleTracks.length + index;
        }
        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            request.writeInterfaceToken(IMEDIA_PLAYER);
            request.writeInt(7);
            request.writeInt(trackType);
            invoke(request, reply);
            return reply.readInt();
        } finally {
            request.recycle();
            reply.recycle();
        }
    }

    public void selectTrack(int index) throws IllegalStateException {
        selectOrDeselectTrack(index, true);
    }

    public void deselectTrack(int index) throws IllegalStateException {
        selectOrDeselectTrack(index, false);
    }

    private void selectOrDeselectTrack(int index, boolean select) throws IllegalStateException {
        int ttIndex;
        SubtitleTrack track = null;
        synchronized (this.mInbandSubtitleLock) {
            if (this.mInbandSubtitleTracks.length == 0) {
                TrackInfo[] tracks = getInbandTrackInfo();
                this.mInbandSubtitleTracks = new SubtitleTrack[tracks.length];
                for (int i = 0; i < tracks.length; i++) {
                    if (tracks[i].getTrackType() == 4) {
                        this.mInbandSubtitleTracks[i] = this.mSubtitleController.addTrack(tracks[i].getFormat());
                    }
                }
            }
        }
        if (index < this.mInbandSubtitleTracks.length) {
            track = this.mInbandSubtitleTracks[index];
        } else if (index < this.mInbandSubtitleTracks.length + this.mOutOfBandSubtitleTracks.size()) {
            SubtitleTrack track2 = this.mOutOfBandSubtitleTracks.get(index - this.mInbandSubtitleTracks.length);
            track = track2;
        }
        if (this.mSubtitleController != null && track != null) {
            if (select) {
                if (track.isTimedText() && (ttIndex = getSelectedTrack(3)) >= 0 && ttIndex < this.mInbandSubtitleTracks.length) {
                    selectOrDeselectInbandTrack(ttIndex, false);
                }
                this.mSubtitleController.selectTrack(track);
                return;
            }
            if (this.mSubtitleController.getSelectedTrack() == track) {
                this.mSubtitleController.selectTrack(null);
                return;
            } else {
                Log.w(TAG, "trying to deselect track that was not selected");
                return;
            }
        }
        selectOrDeselectInbandTrack(index, select);
    }

    private void selectOrDeselectInbandTrack(int index, boolean select) throws IllegalStateException {
        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            request.writeInterfaceToken(IMEDIA_PLAYER);
            request.writeInt(select ? 4 : 5);
            request.writeInt(index);
            invoke(request, reply);
        } finally {
            request.recycle();
            reply.recycle();
        }
    }

    public void setRetransmitEndpoint(InetSocketAddress endpoint) throws IllegalStateException, IllegalArgumentException {
        String addrString = null;
        int port = 0;
        if (endpoint != null) {
            addrString = endpoint.getAddress().getHostAddress();
            port = endpoint.getPort();
        }
        int ret = native_setRetransmitEndpoint(addrString, port);
        if (ret != 0) {
            throw new IllegalArgumentException("Illegal re-transmit endpoint; native ret " + ret);
        }
    }

    protected void finalize() {
        native_finalize();
    }

    public MediaTimeProvider getMediaTimeProvider() {
        if (this.mTimeProvider == null) {
            this.mTimeProvider = new TimeProvider(this);
        }
        return this.mTimeProvider;
    }

    private class EventHandler extends Handler {
        private MediaPlayer mMediaPlayer;

        public EventHandler(MediaPlayer mp, Looper looper) {
            super(looper);
            this.mMediaPlayer = mp;
        }

        @Override
        public void handleMessage(Message msg) {
            if (this.mMediaPlayer.mNativeContext == 0) {
                Log.w(MediaPlayer.TAG, "mediaplayer went away with unhandled events");
                return;
            }
            switch (msg.what) {
                case 0:
                    return;
                case 1:
                    MediaPlayer.this.scanInternalSubtitleTracks();
                    if (MediaPlayer.this.mOnPreparedListener != null) {
                        MediaPlayer.this.mOnPreparedListener.onPrepared(this.mMediaPlayer);
                        return;
                    }
                    return;
                case 2:
                    if (MediaPlayer.this.mOnCompletionListener != null) {
                        MediaPlayer.this.mOnCompletionListener.onCompletion(this.mMediaPlayer);
                    }
                    MediaPlayer.this.stayAwake(false);
                    return;
                case 3:
                    if (MediaPlayer.this.mOnBufferingUpdateListener != null) {
                        MediaPlayer.this.mOnBufferingUpdateListener.onBufferingUpdate(this.mMediaPlayer, msg.arg1);
                        return;
                    }
                    return;
                case 4:
                    if (MediaPlayer.this.mOnSeekCompleteListener != null) {
                        MediaPlayer.this.mOnSeekCompleteListener.onSeekComplete(this.mMediaPlayer);
                    }
                    break;
                case 5:
                    if (MediaPlayer.this.mOnVideoSizeChangedListener != null) {
                        MediaPlayer.this.mOnVideoSizeChangedListener.onVideoSizeChanged(this.mMediaPlayer, msg.arg1, msg.arg2);
                        return;
                    }
                    return;
                case 6:
                case 7:
                    if (MediaPlayer.this.mTimeProvider != null) {
                        MediaPlayer.this.mTimeProvider.onPaused(msg.what == 7);
                        return;
                    }
                    return;
                case 8:
                    if (MediaPlayer.this.mTimeProvider != null) {
                        MediaPlayer.this.mTimeProvider.onStopped();
                        return;
                    }
                    return;
                case 9:
                    break;
                case 99:
                    if (MediaPlayer.this.mOnTimedTextListener != null) {
                        if (msg.obj == null) {
                            MediaPlayer.this.mOnTimedTextListener.onTimedText(this.mMediaPlayer, null);
                            return;
                        } else {
                            if (msg.obj instanceof Parcel) {
                                Parcel parcel = (Parcel) msg.obj;
                                TimedText text = new TimedText(parcel);
                                parcel.recycle();
                                MediaPlayer.this.mOnTimedTextListener.onTimedText(this.mMediaPlayer, text);
                                return;
                            }
                            return;
                        }
                    }
                    return;
                case 100:
                    Log.e(MediaPlayer.TAG, "Error (" + msg.arg1 + "," + msg.arg2 + ")");
                    boolean error_was_handled = false;
                    if (MediaPlayer.this.mOnErrorListener != null) {
                        error_was_handled = MediaPlayer.this.mOnErrorListener.onError(this.mMediaPlayer, msg.arg1, msg.arg2);
                    }
                    if (MediaPlayer.this.mOnCompletionListener != null && !error_was_handled) {
                        MediaPlayer.this.mOnCompletionListener.onCompletion(this.mMediaPlayer);
                    }
                    MediaPlayer.this.stayAwake(false);
                    return;
                case 200:
                    switch (msg.arg1) {
                        case 700:
                            Log.i(MediaPlayer.TAG, "Info (" + msg.arg1 + "," + msg.arg2 + ")");
                            break;
                        case 802:
                            MediaPlayer.this.scanInternalSubtitleTracks();
                        case 803:
                            msg.arg1 = 802;
                            if (MediaPlayer.this.mSubtitleController != null) {
                                MediaPlayer.this.mSubtitleController.selectDefaultTrack();
                            }
                            break;
                    }
                    if (MediaPlayer.this.mOnInfoListener != null) {
                        MediaPlayer.this.mOnInfoListener.onInfo(this.mMediaPlayer, msg.arg1, msg.arg2);
                        return;
                    }
                    return;
                case 201:
                    if (MediaPlayer.this.mOnSubtitleDataListener != null && (msg.obj instanceof Parcel)) {
                        Parcel parcel2 = (Parcel) msg.obj;
                        SubtitleData data = new SubtitleData(parcel2);
                        parcel2.recycle();
                        MediaPlayer.this.mOnSubtitleDataListener.onSubtitleData(this.mMediaPlayer, data);
                        return;
                    }
                    return;
                default:
                    Log.e(MediaPlayer.TAG, "Unknown message type " + msg.what);
                    return;
            }
            if (MediaPlayer.this.mTimeProvider != null) {
                MediaPlayer.this.mTimeProvider.onSeekComplete(this.mMediaPlayer);
            }
        }
    }

    private static void postEventFromNative(Object mediaplayer_ref, int what, int arg1, int arg2, Object obj) {
        MediaPlayer mp = (MediaPlayer) ((WeakReference) mediaplayer_ref).get();
        if (mp != null) {
            if (what == 200 && arg1 == 2) {
                mp.start();
            }
            if (mp.mEventHandler != null) {
                Message m = mp.mEventHandler.obtainMessage(what, arg1, arg2, obj);
                mp.mEventHandler.sendMessage(m);
            }
        }
    }

    public void setOnPreparedListener(OnPreparedListener listener) {
        this.mOnPreparedListener = listener;
    }

    public void setOnCompletionListener(OnCompletionListener listener) {
        this.mOnCompletionListener = listener;
    }

    public void setOnBufferingUpdateListener(OnBufferingUpdateListener listener) {
        this.mOnBufferingUpdateListener = listener;
    }

    public void setOnSeekCompleteListener(OnSeekCompleteListener listener) {
        this.mOnSeekCompleteListener = listener;
    }

    public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener listener) {
        this.mOnVideoSizeChangedListener = listener;
    }

    public void setOnTimedTextListener(OnTimedTextListener listener) {
        this.mOnTimedTextListener = listener;
    }

    public void setOnSubtitleDataListener(OnSubtitleDataListener listener) {
        this.mOnSubtitleDataListener = listener;
    }

    public void setOnErrorListener(OnErrorListener listener) {
        this.mOnErrorListener = listener;
    }

    public void setOnInfoListener(OnInfoListener listener) {
        this.mOnInfoListener = listener;
    }

    private boolean isVideoScalingModeSupported(int mode) {
        return mode == 1 || mode == 2;
    }

    static class TimeProvider implements OnSeekCompleteListener, MediaTimeProvider {
        private static final long MAX_EARLY_CALLBACK_US = 1000;
        private static final long MAX_NS_WITHOUT_POSITION_CHECK = 5000000000L;
        private static final int NOTIFY = 1;
        private static final int NOTIFY_SEEK = 3;
        private static final int NOTIFY_STOP = 2;
        private static final int NOTIFY_TIME = 0;
        private static final int REFRESH_AND_NOTIFY_TIME = 1;
        private static final String TAG = "MTP";
        private static final long TIME_ADJUSTMENT_RATE = 2;
        private Handler mEventHandler;
        private HandlerThread mHandlerThread;
        private long mLastNanoTime;
        private long mLastReportedTime;
        private long mLastTimeUs;
        private MediaTimeProvider.OnMediaTimeListener[] mListeners;
        private MediaPlayer mPlayer;
        private boolean mRefresh;
        private long mTimeAdjustment;
        private long[] mTimes;
        private boolean mPaused = true;
        private boolean mStopped = true;
        private boolean mPausing = false;
        private boolean mSeeking = false;
        public boolean DEBUG = false;

        public TimeProvider(MediaPlayer mp) {
            this.mLastTimeUs = 0L;
            this.mRefresh = false;
            this.mPlayer = mp;
            try {
                getCurrentTimeUs(true, false);
            } catch (IllegalStateException e) {
                this.mRefresh = true;
            }
            Looper looper = Looper.myLooper();
            if (looper == null && (looper = Looper.getMainLooper()) == null) {
                this.mHandlerThread = new HandlerThread("MediaPlayerMTPEventThread", -2);
                this.mHandlerThread.start();
                looper = this.mHandlerThread.getLooper();
            }
            this.mEventHandler = new EventHandler(looper);
            this.mListeners = new MediaTimeProvider.OnMediaTimeListener[0];
            this.mTimes = new long[0];
            this.mLastTimeUs = 0L;
            this.mTimeAdjustment = 0L;
        }

        private void scheduleNotification(int type, long delayUs) {
            if (!this.mSeeking || (type != 0 && type != 1)) {
                if (this.DEBUG) {
                    Log.v(TAG, "scheduleNotification " + type + " in " + delayUs);
                }
                this.mEventHandler.removeMessages(1);
                Message msg = this.mEventHandler.obtainMessage(1, type, 0);
                this.mEventHandler.sendMessageDelayed(msg, (int) (delayUs / 1000));
            }
        }

        public void close() {
            this.mEventHandler.removeMessages(1);
            if (this.mHandlerThread != null) {
                this.mHandlerThread.quitSafely();
                this.mHandlerThread = null;
            }
        }

        protected void finalize() {
            if (this.mHandlerThread != null) {
                this.mHandlerThread.quitSafely();
            }
        }

        public void onPaused(boolean paused) {
            synchronized (this) {
                if (this.DEBUG) {
                    Log.d(TAG, "onPaused: " + paused);
                }
                if (this.mStopped) {
                    this.mStopped = false;
                    this.mSeeking = true;
                    scheduleNotification(3, 0L);
                } else {
                    this.mPausing = paused;
                    this.mSeeking = false;
                    scheduleNotification(1, 0L);
                }
            }
        }

        public void onStopped() {
            synchronized (this) {
                if (this.DEBUG) {
                    Log.d(TAG, "onStopped");
                }
                this.mPaused = true;
                this.mStopped = true;
                this.mSeeking = false;
                scheduleNotification(2, 0L);
            }
        }

        @Override
        public void onSeekComplete(MediaPlayer mp) {
            synchronized (this) {
                this.mStopped = false;
                this.mSeeking = true;
                scheduleNotification(3, 0L);
            }
        }

        public void onNewPlayer() {
            if (this.mRefresh) {
                synchronized (this) {
                    this.mStopped = false;
                    this.mSeeking = true;
                    scheduleNotification(3, 0L);
                }
            }
        }

        private synchronized void notifySeek() {
            this.mSeeking = false;
            try {
                long timeUs = getCurrentTimeUs(true, false);
                if (this.DEBUG) {
                    Log.d(TAG, "onSeekComplete at " + timeUs);
                }
                MediaTimeProvider.OnMediaTimeListener[] arr$ = this.mListeners;
                for (MediaTimeProvider.OnMediaTimeListener listener : arr$) {
                    if (listener == null) {
                        break;
                    }
                    listener.onSeek(timeUs);
                }
            } catch (IllegalStateException e) {
                if (this.DEBUG) {
                    Log.d(TAG, "onSeekComplete but no player");
                }
                this.mPausing = true;
                notifyTimedEvent(false);
            }
        }

        private synchronized void notifyStop() {
            MediaTimeProvider.OnMediaTimeListener[] arr$ = this.mListeners;
            for (MediaTimeProvider.OnMediaTimeListener listener : arr$) {
                if (listener == null) {
                    break;
                }
                listener.onStop();
            }
        }

        private int registerListener(MediaTimeProvider.OnMediaTimeListener listener) {
            int i = 0;
            while (i < this.mListeners.length && this.mListeners[i] != listener && this.mListeners[i] != null) {
                i++;
            }
            if (i >= this.mListeners.length) {
                MediaTimeProvider.OnMediaTimeListener[] newListeners = new MediaTimeProvider.OnMediaTimeListener[i + 1];
                long[] newTimes = new long[i + 1];
                System.arraycopy(this.mListeners, 0, newListeners, 0, this.mListeners.length);
                System.arraycopy(this.mTimes, 0, newTimes, 0, this.mTimes.length);
                this.mListeners = newListeners;
                this.mTimes = newTimes;
            }
            if (this.mListeners[i] == null) {
                this.mListeners[i] = listener;
                this.mTimes[i] = -1;
            }
            return i;
        }

        @Override
        public void notifyAt(long timeUs, MediaTimeProvider.OnMediaTimeListener listener) {
            synchronized (this) {
                if (this.DEBUG) {
                    Log.d(TAG, "notifyAt " + timeUs);
                }
                this.mTimes[registerListener(listener)] = timeUs;
                scheduleNotification(0, 0L);
            }
        }

        @Override
        public void scheduleUpdate(MediaTimeProvider.OnMediaTimeListener listener) {
            synchronized (this) {
                if (this.DEBUG) {
                    Log.d(TAG, "scheduleUpdate");
                }
                int i = registerListener(listener);
                if (!this.mStopped) {
                    this.mTimes[i] = 0;
                    scheduleNotification(0, 0L);
                }
            }
        }

        @Override
        public void cancelNotifications(MediaTimeProvider.OnMediaTimeListener listener) {
            synchronized (this) {
                int i = 0;
                while (true) {
                    if (i < this.mListeners.length) {
                        if (this.mListeners[i] == listener) {
                            System.arraycopy(this.mListeners, i + 1, this.mListeners, i, (this.mListeners.length - i) - 1);
                            System.arraycopy(this.mTimes, i + 1, this.mTimes, i, (this.mTimes.length - i) - 1);
                            this.mListeners[this.mListeners.length - 1] = null;
                            this.mTimes[this.mTimes.length - 1] = -1;
                            break;
                        }
                        if (this.mListeners[i] == null) {
                            break;
                        } else {
                            i++;
                        }
                    } else {
                        break;
                    }
                }
                scheduleNotification(0, 0L);
            }
        }

        private synchronized void notifyTimedEvent(boolean refreshTime) {
            long nowUs;
            try {
                nowUs = getCurrentTimeUs(refreshTime, true);
            } catch (IllegalStateException e) {
                this.mRefresh = true;
                this.mPausing = true;
                nowUs = getCurrentTimeUs(refreshTime, true);
            }
            long nextTimeUs = nowUs;
            if (!this.mSeeking) {
                if (this.DEBUG) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("notifyTimedEvent(").append(this.mLastTimeUs).append(" -> ").append(nowUs).append(") from {");
                    boolean first = true;
                    long[] arr$ = this.mTimes;
                    for (long time : arr$) {
                        if (time != -1) {
                            if (!first) {
                                sb.append(", ");
                            }
                            sb.append(time);
                            first = false;
                        }
                    }
                    sb.append("}");
                    Log.d(TAG, sb.toString());
                }
                Vector<MediaTimeProvider.OnMediaTimeListener> activatedListeners = new Vector<>();
                for (int ix = 0; ix < this.mTimes.length && this.mListeners[ix] != null; ix++) {
                    if (this.mTimes[ix] > -1) {
                        if (this.mTimes[ix] <= 1000 + nowUs) {
                            activatedListeners.add(this.mListeners[ix]);
                            if (this.DEBUG) {
                                Log.d(TAG, Environment.MEDIA_REMOVED);
                            }
                            this.mTimes[ix] = -1;
                        } else if (nextTimeUs == nowUs || this.mTimes[ix] < nextTimeUs) {
                            nextTimeUs = this.mTimes[ix];
                        }
                    }
                }
                if (nextTimeUs > nowUs && !this.mPaused) {
                    if (this.DEBUG) {
                        Log.d(TAG, "scheduling for " + nextTimeUs + " and " + nowUs);
                    }
                    scheduleNotification(0, nextTimeUs - nowUs);
                } else {
                    this.mEventHandler.removeMessages(1);
                }
                for (MediaTimeProvider.OnMediaTimeListener listener : activatedListeners) {
                    listener.onTimedEvent(nowUs);
                }
            }
        }

        private long getEstimatedTime(long nanoTime, boolean monotonic) {
            if (this.mPaused) {
                this.mLastReportedTime = this.mLastTimeUs + this.mTimeAdjustment;
            } else {
                long timeSinceRead = (nanoTime - this.mLastNanoTime) / 1000;
                this.mLastReportedTime = this.mLastTimeUs + timeSinceRead;
                if (this.mTimeAdjustment > 0) {
                    long adjustment = this.mTimeAdjustment - (timeSinceRead / 2);
                    if (adjustment <= 0) {
                        this.mTimeAdjustment = 0L;
                    } else {
                        this.mLastReportedTime += adjustment;
                    }
                }
            }
            return this.mLastReportedTime;
        }

        @Override
        public long getCurrentTimeUs(boolean refreshTime, boolean monotonic) throws IllegalStateException {
            long estimatedTime;
            synchronized (this) {
                if (this.mPaused && !refreshTime) {
                    estimatedTime = this.mLastReportedTime;
                } else {
                    long nanoTime = System.nanoTime();
                    if (refreshTime || nanoTime >= this.mLastNanoTime + MAX_NS_WITHOUT_POSITION_CHECK) {
                        try {
                            this.mLastTimeUs = ((long) this.mPlayer.getCurrentPosition()) * 1000;
                            this.mPaused = this.mPlayer.isPlaying() ? false : true;
                            if (this.DEBUG) {
                                Log.v(TAG, (this.mPaused ? "paused" : "playing") + " at " + this.mLastTimeUs);
                            }
                            this.mLastNanoTime = nanoTime;
                            if (monotonic && this.mLastTimeUs < this.mLastReportedTime) {
                                this.mTimeAdjustment = this.mLastReportedTime - this.mLastTimeUs;
                                if (this.mTimeAdjustment > TimeUtils.NANOS_PER_MS) {
                                    this.mStopped = false;
                                    this.mSeeking = true;
                                    scheduleNotification(3, 0L);
                                }
                            } else {
                                this.mTimeAdjustment = 0L;
                            }
                            estimatedTime = getEstimatedTime(nanoTime, monotonic);
                        } catch (IllegalStateException e) {
                            if (this.mPausing) {
                                this.mPausing = false;
                                getEstimatedTime(nanoTime, monotonic);
                                this.mPaused = true;
                                if (this.DEBUG) {
                                    Log.d(TAG, "illegal state, but pausing: estimating at " + this.mLastReportedTime);
                                }
                                estimatedTime = this.mLastReportedTime;
                            } else {
                                throw e;
                            }
                        }
                    } else {
                        estimatedTime = getEstimatedTime(nanoTime, monotonic);
                    }
                }
            }
            return estimatedTime;
        }

        private class EventHandler extends Handler {
            public EventHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    switch (msg.arg1) {
                        case 0:
                            TimeProvider.this.notifyTimedEvent(false);
                            break;
                        case 1:
                            TimeProvider.this.notifyTimedEvent(true);
                            break;
                        case 2:
                            TimeProvider.this.notifyStop();
                            break;
                        case 3:
                            TimeProvider.this.notifySeek();
                            break;
                    }
                }
            }
        }
    }
}
