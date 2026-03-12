package com.android.keyguard;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.RemoteController;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.transition.ChangeBounds;
import android.transition.ChangeText;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.android.keyguard.KeyguardHostView;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class KeyguardTransportControlView extends FrameLayout {
    private AudioManager mAudioManager;
    private ImageView mBadge;
    private ImageView mBtnNext;
    private ImageView mBtnPlay;
    private ImageView mBtnPrev;
    private int mCurrentPlayState;
    private DateFormat mFormat;
    private final FutureSeekRunnable mFutureSeekRunnable;
    private ViewGroup mInfoContainer;
    private Metadata mMetadata;
    private final TransitionSet mMetadataChangeTransition;
    private ViewGroup mMetadataContainer;
    private final SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener;
    private RemoteController.MetadataEditor mPopulateMetadataWhenAttached;
    private RemoteController.OnClientUpdateListener mRCClientUpdateListener;
    private RemoteController mRemoteController;
    private final Runnable mResetToMetadata;
    private boolean mSeekEnabled;
    private Date mTempDate;
    private TextView mTrackArtistAlbum;
    private TextView mTrackTitle;
    private View mTransientSeek;
    private SeekBar mTransientSeekBar;
    private TextView mTransientSeekTimeElapsed;
    private TextView mTransientSeekTimeTotal;
    private final View.OnClickListener mTransportCommandListener;
    KeyguardHostView.TransportControlCallback mTransportControlCallback;
    private int mTransportControlFlags;
    private final View.OnLongClickListener mTransportShowSeekBarListener;
    private final KeyguardUpdateMonitorCallback mUpdateMonitor;
    private final UpdateSeekBarRunnable mUpdateSeekBars;

    private class UpdateSeekBarRunnable implements Runnable {
        private UpdateSeekBarRunnable() {
        }

        @Override
        public void run() {
            boolean seekAble = updateOnce();
            if (seekAble) {
                KeyguardTransportControlView.this.removeCallbacks(this);
                KeyguardTransportControlView.this.postDelayed(this, 1000L);
            }
        }

        public boolean updateOnce() {
            return KeyguardTransportControlView.this.updateSeekBars();
        }
    }

    class FutureSeekRunnable implements Runnable {
        private boolean mPending;
        private int mProgress;

        FutureSeekRunnable() {
        }

        @Override
        public void run() {
            KeyguardTransportControlView.this.scrubTo(this.mProgress);
            this.mPending = false;
        }

        void setProgress(int progress) {
            this.mProgress = progress;
            if (!this.mPending) {
                this.mPending = true;
                KeyguardTransportControlView.this.postDelayed(this, 30L);
            }
        }
    }

    public static final boolean playbackPositionShouldMove(int playstate) {
        switch (playstate) {
            case 1:
            case 2:
            case 6:
            case 7:
            case 8:
            case 9:
                return false;
            case 3:
            case 4:
            case 5:
            default:
                return true;
        }
    }

    public KeyguardTransportControlView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mMetadata = new Metadata();
        this.mTempDate = new Date();
        this.mPopulateMetadataWhenAttached = null;
        this.mRCClientUpdateListener = new RemoteController.OnClientUpdateListener() {
            @Override
            public void onClientChange(boolean clearing) {
                if (clearing) {
                    KeyguardTransportControlView.this.clearMetadata();
                }
            }

            @Override
            public void onClientPlaybackStateUpdate(int state) {
                KeyguardTransportControlView.this.updatePlayPauseState(state);
            }

            @Override
            public void onClientPlaybackStateUpdate(int state, long stateChangeTimeMs, long currentPosMs, float speed) {
                KeyguardTransportControlView.this.updatePlayPauseState(state);
                KeyguardTransportControlView.this.removeCallbacks(KeyguardTransportControlView.this.mUpdateSeekBars);
                if (KeyguardTransportControlView.this.mTransientSeek.getVisibility() == 0 && KeyguardTransportControlView.playbackPositionShouldMove(KeyguardTransportControlView.this.mCurrentPlayState)) {
                    KeyguardTransportControlView.this.postDelayed(KeyguardTransportControlView.this.mUpdateSeekBars, 1000L);
                }
            }

            @Override
            public void onClientTransportControlUpdate(int transportControlFlags) {
                KeyguardTransportControlView.this.updateTransportControls(transportControlFlags);
            }

            @Override
            public void onClientMetadataUpdate(RemoteController.MetadataEditor metadataEditor) {
                KeyguardTransportControlView.this.updateMetadata(metadataEditor);
            }
        };
        this.mUpdateSeekBars = new UpdateSeekBarRunnable();
        this.mResetToMetadata = new Runnable() {
            @Override
            public void run() {
                KeyguardTransportControlView.this.resetToMetadata();
            }
        };
        this.mTransportCommandListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int keyCode = -1;
                if (v != KeyguardTransportControlView.this.mBtnPrev) {
                    if (v != KeyguardTransportControlView.this.mBtnNext) {
                        if (v == KeyguardTransportControlView.this.mBtnPlay) {
                            keyCode = 85;
                        }
                    } else {
                        keyCode = 87;
                    }
                } else {
                    keyCode = 88;
                }
                if (keyCode != -1) {
                    KeyguardTransportControlView.this.sendMediaButtonClick(keyCode);
                    KeyguardTransportControlView.this.delayResetToMetadata();
                }
            }
        };
        this.mTransportShowSeekBarListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (KeyguardTransportControlView.this.mSeekEnabled) {
                    return KeyguardTransportControlView.this.tryToggleSeekBar();
                }
                return false;
            }
        };
        this.mFutureSeekRunnable = new FutureSeekRunnable();
        this.mOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    KeyguardTransportControlView.this.mFutureSeekRunnable.setProgress(progress);
                    KeyguardTransportControlView.this.delayResetToMetadata();
                    KeyguardTransportControlView.this.mTempDate.setTime(progress);
                    KeyguardTransportControlView.this.mTransientSeekTimeElapsed.setText(KeyguardTransportControlView.this.mFormat.format(KeyguardTransportControlView.this.mTempDate));
                    return;
                }
                KeyguardTransportControlView.this.updateSeekDisplay();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                KeyguardTransportControlView.this.delayResetToMetadata();
                KeyguardTransportControlView.this.removeCallbacks(KeyguardTransportControlView.this.mUpdateSeekBars);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
        this.mUpdateMonitor = new KeyguardUpdateMonitorCallback() {
            @Override
            public void onScreenTurnedOff(int why) {
                KeyguardTransportControlView.this.setEnableMarquee(false);
            }

            @Override
            public void onScreenTurnedOn() {
                KeyguardTransportControlView.this.setEnableMarquee(true);
            }
        };
        this.mAudioManager = new AudioManager(this.mContext);
        this.mCurrentPlayState = 0;
        this.mRemoteController = new RemoteController(context, this.mRCClientUpdateListener);
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int dim = Math.max(dm.widthPixels, dm.heightPixels);
        this.mRemoteController.setArtworkConfiguration(true, dim, dim);
        Transition changeText = new ChangeText();
        changeText.setChangeBehavior(3);
        TransitionSet inner = new TransitionSet();
        inner.addTransition(changeText).addTransition(new ChangeBounds());
        TransitionSet tg = new TransitionSet();
        tg.addTransition(new Fade(2)).addTransition(inner).addTransition(new Fade(1));
        tg.setOrdering(1);
        tg.setDuration(200L);
        this.mMetadataChangeTransition = tg;
    }

    public void updateTransportControls(int transportControlFlags) {
        this.mTransportControlFlags = transportControlFlags;
        setSeekBarsEnabled((transportControlFlags & 256) != 0);
    }

    void setSeekBarsEnabled(boolean enabled) {
        if (enabled != this.mSeekEnabled) {
            this.mSeekEnabled = enabled;
            if (this.mTransientSeek.getVisibility() == 0 && !enabled) {
                this.mTransientSeek.setVisibility(4);
                this.mMetadataContainer.setVisibility(0);
                cancelResetToMetadata();
            }
        }
    }

    public void setTransportControlCallback(KeyguardHostView.TransportControlCallback transportControlCallback) {
        this.mTransportControlCallback = transportControlCallback;
    }

    public void setEnableMarquee(boolean enabled) {
        if (this.mTrackTitle != null) {
            this.mTrackTitle.setSelected(enabled);
        }
        if (this.mTrackArtistAlbum != null) {
            this.mTrackTitle.setSelected(enabled);
        }
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mInfoContainer = (ViewGroup) findViewById(R.id.info_container);
        this.mMetadataContainer = (ViewGroup) findViewById(R.id.metadata_container);
        this.mBadge = (ImageView) findViewById(R.id.badge);
        this.mTrackTitle = (TextView) findViewById(R.id.title);
        this.mTrackArtistAlbum = (TextView) findViewById(R.id.artist_album);
        this.mTransientSeek = findViewById(R.id.transient_seek);
        this.mTransientSeekBar = (SeekBar) findViewById(R.id.transient_seek_bar);
        this.mTransientSeekBar.setOnSeekBarChangeListener(this.mOnSeekBarChangeListener);
        this.mTransientSeekTimeElapsed = (TextView) findViewById(R.id.transient_seek_time_elapsed);
        this.mTransientSeekTimeTotal = (TextView) findViewById(R.id.transient_seek_time_remaining);
        this.mBtnPrev = (ImageView) findViewById(R.id.btn_prev);
        this.mBtnPlay = (ImageView) findViewById(R.id.btn_play);
        this.mBtnNext = (ImageView) findViewById(R.id.btn_next);
        View[] buttons = {this.mBtnPrev, this.mBtnPlay, this.mBtnNext};
        for (View view : buttons) {
            view.setOnClickListener(this.mTransportCommandListener);
            view.setOnLongClickListener(this.mTransportShowSeekBarListener);
        }
        boolean screenOn = KeyguardUpdateMonitor.getInstance(this.mContext).isScreenOn();
        setEnableMarquee(screenOn);
        setOnLongClickListener(this.mTransportShowSeekBarListener);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.mPopulateMetadataWhenAttached != null) {
            updateMetadata(this.mPopulateMetadataWhenAttached);
            this.mPopulateMetadataWhenAttached = null;
        }
        this.mMetadata.clear();
        this.mAudioManager.registerRemoteController(this.mRemoteController);
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mUpdateMonitor);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
        int dim = Math.max(dm.widthPixels, dm.heightPixels);
        this.mRemoteController.setArtworkConfiguration(true, dim, dim);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mAudioManager.unregisterRemoteController(this.mRemoteController);
        KeyguardUpdateMonitor.getInstance(this.mContext).removeCallback(this.mUpdateMonitor);
        this.mMetadata.clear();
        removeCallbacks(this.mUpdateSeekBars);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState ss = new SavedState(super.onSaveInstanceState());
        ss.artist = this.mMetadata.artist;
        ss.trackTitle = this.mMetadata.trackTitle;
        ss.albumTitle = this.mMetadata.albumTitle;
        ss.duration = this.mMetadata.duration;
        ss.bitmap = this.mMetadata.bitmap;
        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        this.mMetadata.artist = ss.artist;
        this.mMetadata.trackTitle = ss.trackTitle;
        this.mMetadata.albumTitle = ss.albumTitle;
        this.mMetadata.duration = ss.duration;
        this.mMetadata.bitmap = ss.bitmap;
        populateMetadata();
    }

    void setBadgeIcon(Drawable bmp) {
        this.mBadge.setImageDrawable(bmp);
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0.0f);
        this.mBadge.setColorFilter(new ColorMatrixColorFilter(cm));
        this.mBadge.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
        this.mBadge.setImageAlpha(239);
    }

    class Metadata {
        private String albumTitle;
        private String artist;
        private Bitmap bitmap;
        private long duration;
        private String trackTitle;

        Metadata() {
        }

        public void clear() {
            this.artist = null;
            this.trackTitle = null;
            this.albumTitle = null;
            this.bitmap = null;
            this.duration = -1L;
        }

        public String toString() {
            return "Metadata[artist=" + this.artist + " trackTitle=" + this.trackTitle + " albumTitle=" + this.albumTitle + " duration=" + this.duration + "]";
        }
    }

    void clearMetadata() {
        this.mPopulateMetadataWhenAttached = null;
        this.mMetadata.clear();
        populateMetadata();
    }

    void updateMetadata(RemoteController.MetadataEditor data) {
        if (isAttachedToWindow()) {
            this.mMetadata.artist = data.getString(13, this.mMetadata.artist);
            this.mMetadata.trackTitle = data.getString(7, this.mMetadata.trackTitle);
            this.mMetadata.albumTitle = data.getString(1, this.mMetadata.albumTitle);
            this.mMetadata.duration = data.getLong(9, -1L);
            this.mMetadata.bitmap = data.getBitmap(100, this.mMetadata.bitmap);
            populateMetadata();
            return;
        }
        this.mPopulateMetadataWhenAttached = data;
    }

    private void populateMetadata() {
        String skeleton;
        if (isLaidOut() && this.mMetadataContainer.getVisibility() == 0) {
            TransitionManager.beginDelayedTransition(this.mMetadataContainer, this.mMetadataChangeTransition);
        }
        String remoteClientPackage = this.mRemoteController.getRemoteControlClientPackageName();
        Drawable badgeIcon = null;
        try {
            badgeIcon = getContext().getPackageManager().getApplicationIcon(remoteClientPackage);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("TransportControlView", "Couldn't get remote control client package icon", e);
        }
        setBadgeIcon(badgeIcon);
        this.mTrackTitle.setText(!TextUtils.isEmpty(this.mMetadata.trackTitle) ? this.mMetadata.trackTitle : null);
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(this.mMetadata.artist)) {
            if (sb.length() != 0) {
                sb.append(" - ");
            }
            sb.append(this.mMetadata.artist);
        }
        if (!TextUtils.isEmpty(this.mMetadata.albumTitle)) {
            if (sb.length() != 0) {
                sb.append(" - ");
            }
            sb.append(this.mMetadata.albumTitle);
        }
        String trackArtistAlbum = sb.toString();
        TextView textView = this.mTrackArtistAlbum;
        if (TextUtils.isEmpty(trackArtistAlbum)) {
            trackArtistAlbum = null;
        }
        textView.setText(trackArtistAlbum);
        if (this.mMetadata.duration >= 0) {
            setSeekBarsEnabled(true);
            setSeekBarDuration(this.mMetadata.duration);
            if (this.mMetadata.duration >= 86400000) {
                skeleton = "DDD kk mm ss";
            } else if (this.mMetadata.duration >= 3600000) {
                skeleton = "kk mm ss";
            } else {
                skeleton = "mm ss";
            }
            this.mFormat = new SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(getContext().getResources().getConfiguration().locale, skeleton));
            this.mFormat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        } else {
            setSeekBarsEnabled(false);
        }
        KeyguardUpdateMonitor.getInstance(getContext()).dispatchSetBackground(this.mMetadata.bitmap);
        int flags = this.mTransportControlFlags;
        setVisibilityBasedOnFlag(this.mBtnPrev, flags, 1);
        setVisibilityBasedOnFlag(this.mBtnNext, flags, 128);
        setVisibilityBasedOnFlag(this.mBtnPlay, flags, 60);
        updatePlayPauseState(this.mCurrentPlayState);
    }

    void updateSeekDisplay() {
        if (this.mMetadata != null && this.mRemoteController != null && this.mFormat != null) {
            this.mTempDate.setTime(this.mRemoteController.getEstimatedMediaPosition());
            this.mTransientSeekTimeElapsed.setText(this.mFormat.format(this.mTempDate));
            this.mTempDate.setTime(this.mMetadata.duration);
            this.mTransientSeekTimeTotal.setText(this.mFormat.format(this.mTempDate));
        }
    }

    boolean tryToggleSeekBar() {
        TransitionManager.beginDelayedTransition(this.mInfoContainer);
        if (this.mTransientSeek.getVisibility() == 0) {
            this.mTransientSeek.setVisibility(4);
            this.mMetadataContainer.setVisibility(0);
            cancelResetToMetadata();
            removeCallbacks(this.mUpdateSeekBars);
        } else {
            this.mTransientSeek.setVisibility(0);
            this.mMetadataContainer.setVisibility(4);
            delayResetToMetadata();
            if (playbackPositionShouldMove(this.mCurrentPlayState)) {
                this.mUpdateSeekBars.run();
            } else {
                this.mUpdateSeekBars.updateOnce();
            }
        }
        this.mTransportControlCallback.userActivity();
        return true;
    }

    void resetToMetadata() {
        TransitionManager.beginDelayedTransition(this.mInfoContainer);
        if (this.mTransientSeek.getVisibility() == 0) {
            this.mTransientSeek.setVisibility(4);
            this.mMetadataContainer.setVisibility(0);
        }
    }

    void delayResetToMetadata() {
        removeCallbacks(this.mResetToMetadata);
        postDelayed(this.mResetToMetadata, 5000L);
    }

    void cancelResetToMetadata() {
        removeCallbacks(this.mResetToMetadata);
    }

    void setSeekBarDuration(long duration) {
        this.mTransientSeekBar.setMax((int) duration);
    }

    void scrubTo(int progress) {
        this.mRemoteController.seekTo(progress);
        this.mTransportControlCallback.userActivity();
    }

    private static void setVisibilityBasedOnFlag(View view, int flags, int flag) {
        if ((flags & flag) != 0) {
            view.setVisibility(0);
        } else {
            view.setVisibility(4);
        }
    }

    public void updatePlayPauseState(int state) {
        int imageResId;
        int imageDescId;
        if (state != this.mCurrentPlayState) {
            switch (state) {
                case 3:
                    imageResId = R.drawable.ic_media_pause;
                    imageDescId = R.string.keyguard_transport_pause_description;
                    break;
                case 8:
                    imageResId = R.drawable.ic_media_stop;
                    imageDescId = R.string.keyguard_transport_stop_description;
                    break;
                case 9:
                    imageResId = R.drawable.stat_sys_warning;
                    imageDescId = R.string.keyguard_transport_play_description;
                    break;
                default:
                    imageResId = R.drawable.ic_media_play;
                    imageDescId = R.string.keyguard_transport_play_description;
                    break;
            }
            boolean clientSupportsSeek = this.mMetadata != null && this.mMetadata.duration > 0;
            setSeekBarsEnabled(clientSupportsSeek);
            this.mBtnPlay.setImageResource(imageResId);
            this.mBtnPlay.setContentDescription(getResources().getString(imageDescId));
            this.mCurrentPlayState = state;
        }
    }

    boolean updateSeekBars() {
        int position = (int) this.mRemoteController.getEstimatedMediaPosition();
        if (position >= 0) {
            this.mTransientSeekBar.setProgress(position);
            return true;
        }
        Log.w("TransportControlView", "Updating seek bars; received invalid estimated media position (" + position + "). Disabling seek.");
        setSeekBarsEnabled(false);
        return false;
    }

    static class SavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        String albumTitle;
        String artist;
        Bitmap bitmap;
        boolean clientPresent;
        long duration;
        String trackTitle;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.clientPresent = in.readInt() != 0;
            this.artist = in.readString();
            this.trackTitle = in.readString();
            this.albumTitle = in.readString();
            this.duration = in.readLong();
            this.bitmap = (Bitmap) Bitmap.CREATOR.createFromParcel(in);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.clientPresent ? 1 : 0);
            out.writeString(this.artist);
            out.writeString(this.trackTitle);
            out.writeString(this.albumTitle);
            out.writeLong(this.duration);
            this.bitmap.writeToParcel(out, flags);
        }
    }

    public void sendMediaButtonClick(int keyCode) {
        this.mRemoteController.sendMediaKeyEvent(new KeyEvent(0, keyCode));
        this.mRemoteController.sendMediaKeyEvent(new KeyEvent(1, keyCode));
        this.mTransportControlCallback.userActivity();
    }
}
