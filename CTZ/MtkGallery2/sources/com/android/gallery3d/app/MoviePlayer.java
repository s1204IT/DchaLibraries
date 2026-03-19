package com.android.gallery3d.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.Virtualizer;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationManagerCompat;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.Toast;
import com.android.gallery3d.R;
import com.android.gallery3d.app.ControllerOverlay;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.util.GalleryUtils;
import com.mediatek.gallery3d.video.Bookmarker;
import com.mediatek.gallery3d.video.DefaultMovieListLoader;
import com.mediatek.gallery3d.video.ErrorDialogFragment;
import com.mediatek.gallery3d.video.ExtensionHelper;
import com.mediatek.gallery3d.video.IContrllerOverlayExt;
import com.mediatek.gallery3d.video.IMovieDrmExtension;
import com.mediatek.gallery3d.video.IMovieItem;
import com.mediatek.gallery3d.video.IMovieList;
import com.mediatek.gallery3d.video.IMovieListLoader;
import com.mediatek.gallery3d.video.IMoviePlayer;
import com.mediatek.gallery3d.video.MediaPlayerWrapper;
import com.mediatek.gallery3d.video.MovieUtils;
import com.mediatek.gallery3d.video.MovieView;
import com.mediatek.gallery3d.video.PowerSavingManager;
import com.mediatek.gallery3d.video.RemoteConnection;
import com.mediatek.gallery3d.video.VideoGestureController;
import com.mediatek.galleryportable.VideoConstantUtils;
import com.mediatek.galleryportable.VideoMetadataUtils;
import java.util.HashMap;

public class MoviePlayer implements AudioManager.OnAudioFocusChangeListener, ControllerOverlay.Listener, IMovieListLoader.LoaderListener, MediaPlayerWrapper.Listener, MediaPlayerWrapper.MultiWindowListener {
    private AudioBecomingNoisyReceiver mAudioBecomingNoisyReceiver;
    private Bookmarker mBookmarker;
    private boolean mCanReplay;
    private Context mContext;
    private MovieControllerOverlay mController;
    private boolean mDragging;
    private boolean mFirstBePlayed;
    private FragmentManager mFragmentManager;
    private Handler mHandler;
    private boolean mHasPaused;
    private boolean mIsShowResumingDialog;
    private MediaPlayerWrapper mMediaPlayerWrapper;
    private IMovieItem mMovieItem;
    public IMovieList mMovieList;
    private IMovieListLoader mMovieLoader;
    private MovieView mMovieView;
    private IContrllerOverlayExt mOverlayExt;
    private View mPlaceHolder;
    private int mPowerSavingPosition;
    private long mResumeableTime;
    private View mRootView;
    private int mSeekMovePosition;
    private MediaSession mSession;
    private boolean mShowing;
    private TState mTState;
    private VideoGestureController mVideoGestureController;
    private int mVideoLastDuration;
    private int mVideoPosition;
    private Virtualizer mVirtualizer;
    private static int MEDIA_ERROR_BASE = NotificationManagerCompat.IMPORTANCE_UNSPECIFIED;
    private static int ERROR_BUFFER_DEQUEUE_FAIL = (MEDIA_ERROR_BASE - 100) - 6;
    private String mCookie = null;
    private int mLastSystemUiVis = 0;
    private boolean mVideoCanPause = false;
    private boolean mVideoCanSeek = false;
    private boolean mError = false;
    private int mAudiofocusState = 0;
    private RetryExtension mRetryExt = new RetryExtension();
    public MoviePlayerExtension mPlayerExt = new MoviePlayerExtension();
    private boolean mIsBuffering = false;
    private boolean mIsDialogShow = false;
    private boolean mIsOnlyAudio = false;
    private PowerSavingManager mPowerSavingManager = null;
    PowerSavingEvent mPowerSavingEvent = PowerSavingEvent.EVENT_NONE;
    private final String TAG_ERROR_DIALOG = "ERROR_DIALOG_TAG";
    private final Runnable mPlayingChecker = new Runnable() {
        @Override
        public void run() {
            if (!MoviePlayer.this.mMediaPlayerWrapper.isPlaying() || MoviePlayer.this.mIsBuffering) {
                MoviePlayer.this.mHandler.postDelayed(MoviePlayer.this.mPlayingChecker, 250L);
                return;
            }
            if (MoviePlayer.this.mIsDialogShow && !MovieUtils.isLiveStreaming(MoviePlayer.this.mMovieItem.getVideoType())) {
                com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "mPlayingChecker.run() pauseIfNeed");
                MoviePlayer.this.mPlayerExt.pauseIfNeed();
            } else {
                com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "mPlayingChecker.run() showPlaying");
                MoviePlayer.this.mController.showPlaying();
            }
        }
    };
    private final Runnable mProgressChecker = new Runnable() {
        @Override
        public void run() {
            MoviePlayer.this.mHandler.postDelayed(MoviePlayer.this.mProgressChecker, 1000 - (MoviePlayer.this.setProgress() % 1000));
        }
    };
    private final MediaSession.Callback mSessionCallback = new MediaSession.Callback() {
        @Override
        public boolean onMediaButtonEvent(Intent intent) {
            if (MoviePlayer.this.mSession == null || intent == null) {
                com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "SessionCallback mSession= " + MoviePlayer.this.mSession + "mediaIntent= " + intent);
                return false;
            }
            if ("android.intent.action.MEDIA_BUTTON".equals(intent.getAction())) {
                KeyEvent keyEvent = (KeyEvent) intent.getParcelableExtra("android.intent.extra.KEY_EVENT");
                com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "SessionCallback event= " + keyEvent);
                if (keyEvent != null && keyEvent.getAction() == 0) {
                    MoviePlayer.this.onKeyDown(keyEvent.getKeyCode(), keyEvent);
                    return true;
                }
            }
            return false;
        }
    };
    private RemoteConnection.ConnectionEventListener mEventListener = new RemoteConnection.ConnectionEventListener() {
        @Override
        public void onEvent(int i) {
            com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "onEvent() what= " + i);
            if (i == 1 || i == 2) {
                MoviePlayer.this.mPowerSavingPosition = MoviePlayer.this.mMediaPlayerWrapper.getCurrentPosition();
                MoviePlayer.this.mPowerSavingEvent = PowerSavingEvent.EVENT_NEED_RESTORE;
                return;
            }
            if (i == 3) {
                if (!((MovieActivity) MoviePlayer.this.mActivityContext).isMultiWindowMode()) {
                    MoviePlayer.this.mActivityContext.finish();
                    return;
                } else {
                    MoviePlayer.this.mPlayerExt.stopVideo();
                    return;
                }
            }
            MoviePlayer.this.mPowerSavingEvent = PowerSavingEvent.EVENT_NONE;
        }
    };
    private boolean mConsumedDrmRight = false;
    private Activity mActivityContext;
    private IMovieDrmExtension mDrmExt = ExtensionHelper.getMovieDrmExtension(this.mActivityContext);
    private Runnable mDelayVideoRunnable = new Runnable() {
        @Override
        public void run() {
            com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "mDelayVideoRunnable.run(), set MovieView visible");
            MoviePlayer.this.mMovieView.setVisibility(0);
        }
    };

    private enum PowerSavingEvent {
        EVENT_NEED_RESTORE,
        EVENT_NONE
    }

    private enum TState {
        PLAYING,
        PAUSED,
        STOPED,
        COMPELTED,
        RETRY_ERROR
    }

    public void dismissAllowingStateLoss() {
        if (this.mFragmentManager == null) {
            this.mFragmentManager = this.mActivityContext.getFragmentManager();
        }
        DialogFragment dialogFragment = (DialogFragment) this.mFragmentManager.findFragmentByTag("ERROR_DIALOG_TAG");
        if (dialogFragment != null) {
            dialogFragment.dismissAllowingStateLoss();
        }
    }

    public MoviePlayer(View view, MovieActivity movieActivity, IMovieItem iMovieItem, Bundle bundle, boolean z, String str) {
        this.mResumeableTime = Long.MAX_VALUE;
        this.mVideoPosition = 0;
        this.mHasPaused = false;
        this.mFirstBePlayed = false;
        this.mTState = TState.PLAYING;
        com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "new MoviePlayer, rootView = " + view + ", movieActivity = " + movieActivity + ", movieItem = " + iMovieItem + ", savedInstance = " + bundle + ", canReplay = " + z + ", cookie = " + str);
        initialize(view, movieActivity, iMovieItem, z, str);
        if (bundle != null) {
            this.mVideoPosition = bundle.getInt("video-position", 0);
            this.mResumeableTime = bundle.getLong("resumeable-timeout", Long.MAX_VALUE);
            this.mHasPaused = true;
            onRestoreInstanceState(bundle);
            return;
        }
        this.mTState = TState.PLAYING;
        this.mFirstBePlayed = true;
        Bookmarker.BookmarkerInfo bookmark = this.mBookmarker.getBookmark(iMovieItem.getUri());
        if (bookmark != null) {
            showResumeDialog(movieActivity, bookmark);
        } else {
            doStartVideoCareDrm(false, 0, 0);
        }
    }

    private void initialize(View view, MovieActivity movieActivity, IMovieItem iMovieItem, boolean z, String str) {
        this.mRootView = view;
        this.mMovieView = (MovieView) this.mRootView.findViewById(R.id.movie_view);
        this.mPlaceHolder = this.mRootView.findViewById(R.id.place_holder);
        this.mActivityContext = movieActivity;
        this.mContext = movieActivity.getApplicationContext();
        this.mMovieItem = iMovieItem;
        this.mCanReplay = z;
        this.mCookie = str;
        this.mMediaPlayerWrapper = new MediaPlayerWrapper(this.mContext, this.mMovieView);
        this.mMediaPlayerWrapper.setListener(this);
        this.mMediaPlayerWrapper.setMultiWindowListener(this);
        this.mHandler = new Handler();
        this.mBookmarker = new Bookmarker(movieActivity);
        this.mController = new MovieControllerOverlay(movieActivity, this.mMediaPlayerWrapper, this.mMovieItem);
        ((ViewGroup) view).addView(this.mController.getView());
        this.mController.setListener(this);
        this.mController.setCanReplay(z);
        movieActivity.setMovieHookerParameter(null, this.mPlayerExt);
        this.mOverlayExt = this.mController.getOverlayExt();
        if (movieActivity.getIntent().getBooleanExtra("virtualize", false)) {
            int audioSessionId = this.mMediaPlayerWrapper.getAudioSessionId();
            if (audioSessionId != 0) {
                this.mVirtualizer = new Virtualizer(0, audioSessionId);
                this.mVirtualizer.setEnabled(true);
            } else {
                com.mediatek.gallery3d.util.Log.w("VP_MoviePlayer", "no audio session to virtualize");
            }
        }
        setOnSystemUiVisibilityChangeListener();
        showSystemUi(false);
        this.mAudioBecomingNoisyReceiver = new AudioBecomingNoisyReceiver();
        this.mAudioBecomingNoisyReceiver.register();
        enablePowerSavingIfNeed();
        this.mVideoGestureController = new VideoGestureController(this.mContext, this.mRootView, this.mController);
        initMovieList();
    }

    private void createMediaSession() {
        com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "createMediaSession() mSession= " + this.mSession);
        if (this.mSession == null) {
            this.mSession = new MediaSession(this.mContext, MoviePlayer.class.getSimpleName());
            this.mSession.setCallback(this.mSessionCallback);
            this.mSession.setFlags(1);
            this.mSession.setActive(true);
        }
    }

    private void releaseMediaSession() {
        com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "releaseMediaSession() mSession=" + this.mSession);
        if (this.mSession != null) {
            this.mSession.setCallback(null);
            this.mSession.setActive(false);
            this.mSession.release();
            this.mSession = null;
        }
    }

    private void initMovieList() {
        this.mMovieLoader = new DefaultMovieListLoader();
        this.mMovieLoader.fillVideoList(this.mContext, this.mActivityContext.getIntent(), this, this.mMovieItem);
    }

    @Override
    public void onListLoaded(IMovieList iMovieList) {
        this.mMovieList = iMovieList;
        StringBuilder sb = new StringBuilder();
        sb.append("onListLoaded() ");
        sb.append(this.mMovieList != null ? Integer.valueOf(this.mMovieList.size()) : "null");
        com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", sb.toString());
    }

    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "onKeyDown keyCode = " + i);
        if (keyEvent.getRepeatCount() > 0) {
            return isMediaKey(i);
        }
        if (!this.mController.isTimeBarEnabled()) {
            com.mediatek.gallery3d.util.Log.w("VP_MoviePlayer", "onKeyDown, can not play or pause");
            return isMediaKey(i);
        }
        switch (i) {
            case 79:
            case 85:
                if (this.mMediaPlayerWrapper.isPlaying() && this.mMediaPlayerWrapper.canPause()) {
                    pauseVideo();
                } else {
                    playVideo();
                }
                return true;
            case 87:
                break;
            case 88:
                if (this.mMovieList != null) {
                    this.mPlayerExt.startNextVideo(this.mMovieList.getPrevious(this.mMovieItem));
                    return true;
                }
                break;
            case 126:
                if (!this.mMediaPlayerWrapper.isPlaying()) {
                    playVideo();
                }
                return true;
            case 127:
                if (this.mMediaPlayerWrapper.isPlaying() && this.mMediaPlayerWrapper.canPause()) {
                    pauseVideo();
                }
                return true;
            default:
                return false;
        }
        if (this.mMovieList != null) {
            this.mPlayerExt.startNextVideo(this.mMovieList.getNext(this.mMovieItem));
            return true;
        }
        return false;
    }

    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        return isMediaKey(i);
    }

    private static boolean isMediaKey(int i) {
        return i == 79 || i == 88 || i == 87 || i == 85 || i == 126 || i == 127;
    }

    private void enablePowerSavingIfNeed() {
        if (this.mPowerSavingManager == null) {
            this.mPowerSavingManager = new PowerSavingManager(this.mActivityContext, this.mRootView, this.mEventListener);
        } else {
            this.mPowerSavingManager.refreshRemoteDisplay();
        }
    }

    private void playbackControlforPowerSaving() {
        com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "playbackControlforPowerSaving() mPowerSavingPosition= " + this.mPowerSavingPosition + " mVideoPosition= " + this.mVideoPosition + " mTState= " + this.mTState);
        if (this.mPowerSavingPosition == 0) {
            this.mPowerSavingPosition = this.mVideoPosition;
        }
        if (this.mTState == TState.PLAYING) {
            this.mMediaPlayerWrapper.seekTo(this.mPowerSavingPosition);
            playVideo();
        } else if (this.mTState == TState.PAUSED) {
            this.mMediaPlayerWrapper.seekTo(this.mPowerSavingPosition);
            pauseVideo();
        }
        this.mPowerSavingEvent = PowerSavingEvent.EVENT_NONE;
        this.mPowerSavingPosition = 0;
    }

    private void disablePowerSavingIfNeed() {
        this.mPowerSavingManager.release();
    }

    @TargetApi(16)
    private void clearOnSystemUiVisibilityChangeListener() {
        if (!ApiHelper.HAS_VIEW_SYSTEM_UI_FLAG_HIDE_NAVIGATION) {
            return;
        }
        this.mRootView.setOnSystemUiVisibilityChangeListener(null);
    }

    @TargetApi(16)
    private void setOnSystemUiVisibilityChangeListener() {
        if (!ApiHelper.HAS_VIEW_SYSTEM_UI_FLAG_HIDE_NAVIGATION) {
            return;
        }
        this.mRootView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int i) {
                boolean zIsFinishing;
                if (MoviePlayer.this.mActivityContext != null) {
                    zIsFinishing = MoviePlayer.this.mActivityContext.isFinishing();
                } else {
                    zIsFinishing = true;
                }
                int i2 = MoviePlayer.this.mLastSystemUiVis ^ i;
                MoviePlayer.this.mLastSystemUiVis = i;
                if (((i2 & 2) != 0 && (i & 2) == 0) || ((1 & i2) != 0 && (i & 1) == 0)) {
                    MoviePlayer.this.mController.show();
                }
                com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "onSystemUiVisibilityChange(" + i + ") finishing()=" + zIsFinishing);
            }
        });
    }

    @TargetApi(16)
    private void showSystemUi(boolean z) {
        com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "showSystemUi() visible " + z);
        if (!ApiHelper.HAS_VIEW_SYSTEM_UI_FLAG_LAYOUT_STABLE) {
            return;
        }
        int i = 1792;
        if (!z) {
            i = 1799;
            this.mActivityContext.closeOptionsMenu();
        }
        this.mRootView.setSystemUiVisibility(i);
    }

    public void onSaveInstanceState(Bundle bundle) {
        if (!this.mHasPaused) {
            recordVideoPosition();
        }
        bundle.putInt("video-position", this.mVideoPosition);
        bundle.putLong("resumeable-timeout", this.mResumeableTime);
        onSaveInstanceStateMore(bundle);
    }

    private void showResumeDialog(Context context, final Bookmarker.BookmarkerInfo bookmarkerInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.resume_playing_title);
        builder.setMessage(String.format(context.getString(R.string.resume_playing_message), GalleryUtils.formatDuration(context, bookmarkerInfo.mBookmark / 1000)));
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                MoviePlayer.this.onCompletion();
                MoviePlayer.this.mIsShowResumingDialog = false;
            }
        });
        builder.setPositiveButton(R.string.resume_playing_resume, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                MoviePlayer.this.mVideoCanSeek = true;
                MoviePlayer.this.doStartVideoCareDrm(true, bookmarkerInfo.mBookmark, bookmarkerInfo.mDuration);
                MoviePlayer.this.mVideoPosition = bookmarkerInfo.mBookmark;
                MoviePlayer.this.mIsShowResumingDialog = false;
                MoviePlayer.this.mHandler.removeCallbacks(MoviePlayer.this.mProgressChecker);
                MoviePlayer.this.mHandler.post(MoviePlayer.this.mProgressChecker);
            }
        });
        builder.setNegativeButton(R.string.resume_playing_restart, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                MoviePlayer.this.doStartVideoCareDrm(true, 0, bookmarkerInfo.mDuration);
                MoviePlayer.this.mIsShowResumingDialog = false;
                MoviePlayer.this.mHandler.removeCallbacks(MoviePlayer.this.mProgressChecker);
                MoviePlayer.this.mHandler.post(MoviePlayer.this.mProgressChecker);
            }
        });
        builder.show();
        this.mIsShowResumingDialog = true;
    }

    public void onPause() {
        com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "onPause()");
        recordVideoPosition();
        if (!((MovieActivity) this.mActivityContext).isMultiWindowMode()) {
            doOnPause();
        }
    }

    public void onStop() {
        com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "onStop()");
        doOnPause();
    }

    private void recordVideoPosition() {
        int currentPosition = this.mMediaPlayerWrapper.getCurrentPosition();
        if (currentPosition < 0) {
            currentPosition = this.mVideoPosition;
        }
        this.mVideoPosition = currentPosition;
        int duration = this.mMediaPlayerWrapper.getDuration();
        if (duration <= 0) {
            duration = this.mVideoLastDuration;
        }
        this.mVideoLastDuration = duration;
        this.mResumeableTime = System.currentTimeMillis() + 180000;
        com.mediatek.gallery3d.util.Log.d("VP_MoviePlayer", "recordVideoPosition()  mVideoPosition= " + this.mVideoPosition + " mVideoLastDuration= " + this.mVideoLastDuration + " mResumeableTime= " + this.mResumeableTime);
    }

    private void doOnPause() {
        com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "doOnPause() mHasPaused= " + this.mHasPaused);
        if (this.mHasPaused) {
            return;
        }
        this.mHasPaused = true;
        abandonAudiofocus();
        releaseMediaSession();
        this.mHandler.removeCallbacksAndMessages(null);
        this.mOverlayExt.onCancelHiding();
        clearOnSystemUiVisibilityChangeListener();
        this.mBookmarker.setBookmark(this.mMovieItem.getUri(), this.mVideoPosition, this.mVideoLastDuration);
        this.mMediaPlayerWrapper.stop();
        this.mIsBuffering = false;
        this.mMediaPlayerWrapper.setResumed(false);
        this.mOverlayExt.setBottomPanel(false, false);
        this.mOverlayExt.clearBuffering();
        disablePowerSavingIfNeed();
    }

    public void onResume() {
        dump();
        if (this.mHasPaused) {
            this.mDragging = false;
            this.mFirstBePlayed = true;
            this.mHasPaused = false;
            setOnSystemUiVisibilityChangeListener();
            this.mMovieView.removeCallbacks(this.mDelayVideoRunnable);
            this.mMovieView.postDelayed(this.mDelayVideoRunnable, 500L);
            enablePowerSavingIfNeed();
            if (this.mIsShowResumingDialog) {
            }
            switch (AnonymousClass11.$SwitchMap$com$android$gallery3d$app$MoviePlayer$TState[this.mTState.ordinal()]) {
                case 1:
                    this.mRetryExt.showRetry();
                    break;
                case 2:
                case 3:
                    this.mController.showEnded();
                    break;
                case 4:
                    doStartVideo(true, this.mVideoPosition, this.mVideoLastDuration);
                    pauseVideo();
                    break;
                default:
                    if (this.mConsumedDrmRight) {
                        doStartVideo(true, this.mVideoPosition, this.mVideoLastDuration);
                    } else {
                        doStartVideoCareDrm(true, this.mVideoPosition, this.mVideoLastDuration);
                    }
                    pauseVideoMoreThanThreeMinutes();
                    break;
            }
        }
    }

    static class AnonymousClass11 {
        static final int[] $SwitchMap$com$android$gallery3d$app$MoviePlayer$TState = new int[TState.values().length];

        static {
            try {
                $SwitchMap$com$android$gallery3d$app$MoviePlayer$TState[TState.RETRY_ERROR.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$gallery3d$app$MoviePlayer$TState[TState.STOPED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$gallery3d$app$MoviePlayer$TState[TState.COMPELTED.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$gallery3d$app$MoviePlayer$TState[TState.PAUSED.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    private void pauseVideoMoreThanThreeMinutes() {
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (jCurrentTimeMillis > this.mResumeableTime && !MovieUtils.isLiveStreaming(this.mMovieItem.getVideoType())) {
            if (this.mVideoCanPause || this.mMediaPlayerWrapper.canPause()) {
                com.mediatek.gallery3d.util.Log.d("VP_MoviePlayer", "pauseVideoMoreThanThreeMinutes() now=" + jCurrentTimeMillis);
                pauseVideo();
            }
        }
    }

    public void onDestroy() {
        com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "onDestroy");
        if (this.mVirtualizer != null) {
            this.mVirtualizer.release();
            this.mVirtualizer = null;
        }
        this.mAudioBecomingNoisyReceiver.unregister();
    }

    private int setProgress() {
        if (this.mDragging) {
            return 0;
        }
        int currentPosition = this.mMediaPlayerWrapper.getCurrentPosition();
        this.mController.setTimes(currentPosition, this.mMediaPlayerWrapper.getDuration(), 0, 0);
        return currentPosition;
    }

    private void doStartVideo(boolean z, int i, int i2) {
        com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "doStartVideo(" + z + ", " + i + ", " + i2);
        requestAudioFocus();
        createMediaSession();
        dismissAllowingStateLoss();
        Uri uri = this.mMovieItem.getUri();
        if (!MovieUtils.isLocalFile(this.mMovieItem.getVideoType())) {
            HashMap map = new HashMap(2);
            this.mController.showLoading(false);
            this.mOverlayExt.setPlayingInfo(MovieUtils.isLiveStreaming(this.mMovieItem.getVideoType()));
            this.mHandler.removeCallbacks(this.mPlayingChecker);
            this.mHandler.postDelayed(this.mPlayingChecker, 250L);
            com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "doStartVideo() mCookie is " + this.mCookie);
            if (MovieUtils.isRTSP(this.mMovieItem.getVideoType())) {
                if (this.mCookie != null) {
                    map.put("Cookie", this.mCookie);
                }
                this.mMediaPlayerWrapper.setVideoURI(uri, map);
            } else if (this.mCookie != null) {
                map.put("Cookie", this.mCookie);
                this.mMediaPlayerWrapper.setVideoURI(uri, map);
            } else {
                this.mMediaPlayerWrapper.setVideoURI(uri, null);
            }
        } else {
            this.mController.showPlaying();
            this.mController.hide();
            this.mMediaPlayerWrapper.setVideoURI(uri, null);
        }
        this.mMediaPlayerWrapper.start();
        if (i > 0 && (this.mVideoCanSeek || this.mMediaPlayerWrapper.canSeekForward())) {
            this.mMediaPlayerWrapper.seekTo(i);
        }
        if (z) {
            this.mMediaPlayerWrapper.setDuration(i2);
        }
        this.mHandler.removeCallbacks(this.mProgressChecker);
        this.mHandler.post(this.mProgressChecker);
    }

    private void doStartVideoCareDrm(final boolean z, final int i, final int i2) {
        com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "doStartVideoCareDrm(" + z + ", " + i + ", " + i2 + ")");
        this.mTState = TState.PLAYING;
        if (!this.mDrmExt.handleDrmFile(this.mActivityContext, this.mMovieItem, new IMovieDrmExtension.IMovieDrmCallback() {
            @Override
            public void onContinue() {
                MoviePlayer.this.doStartVideo(z, i, i2);
                MoviePlayer.this.mConsumedDrmRight = true;
            }

            @Override
            public void onStop() {
                MoviePlayer.this.mPlayerExt.setLoop(false);
                MoviePlayer.this.onCompletion(null);
            }
        })) {
            doStartVideo(z, i, i2);
        }
    }

    private void playVideo() {
        com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "playVideo()");
        if (!hasAudiofocus() && !requestAudioFocus()) {
            Toast.makeText(this.mContext.getApplicationContext(), this.mContext.getString(R.string.m_audiofocus_request_failed_message), 0).show();
            return;
        }
        this.mPlayerExt.mPauseBuffering = false;
        this.mTState = TState.PLAYING;
        this.mMediaPlayerWrapper.start();
        this.mController.showPlaying();
        setProgress();
        this.mHandler.removeCallbacks(this.mProgressChecker);
        this.mHandler.post(this.mProgressChecker);
    }

    private void pauseVideo() {
        com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "pauseVideo()");
        this.mTState = TState.PAUSED;
        this.mMediaPlayerWrapper.pause();
        this.mController.showPaused();
        setProgress();
        this.mHandler.removeCallbacks(this.mProgressChecker);
    }

    public void onCompletion() {
    }

    public boolean isPlaying() {
        return this.mMediaPlayerWrapper.isPlaying();
    }

    private class AudioBecomingNoisyReceiver extends BroadcastReceiver {
        private AudioBecomingNoisyReceiver() {
        }

        public void register() {
            MoviePlayer.this.mContext.registerReceiver(this, new IntentFilter("android.media.AUDIO_BECOMING_NOISY"));
        }

        public void unregister() {
            MoviePlayer.this.mContext.unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "AudioBecomingNoisyReceiver onReceive");
            if (MoviePlayer.this.mController.isTimeBarEnabled()) {
                if (MoviePlayer.this.mMediaPlayerWrapper.isPlaying() && MoviePlayer.this.mMediaPlayerWrapper.canPause()) {
                    MoviePlayer.this.pauseVideo();
                    return;
                }
                return;
            }
            com.mediatek.gallery3d.util.Log.w("VP_MoviePlayer", "AudioBecomingNoisyReceiver, can not play or pause");
        }
    }

    public SurfaceView getVideoSurface() {
        return this.mMovieView;
    }

    public MediaPlayerWrapper getPlayerWrapper() {
        return this.mMediaPlayerWrapper;
    }

    private void onSaveInstanceStateMore(Bundle bundle) {
        bundle.putInt("video_last_duration", this.mVideoLastDuration);
        bundle.putBoolean("video_can_pause", this.mMediaPlayerWrapper.canPause());
        if (this.mVideoCanSeek || this.mMediaPlayerWrapper.canSeekForward()) {
            bundle.putBoolean("video_can_seek", true);
        } else {
            bundle.putBoolean("video_can_seek", false);
        }
        bundle.putBoolean("consumed_drm_right", this.mConsumedDrmRight);
        bundle.putString("video_state", String.valueOf(this.mTState));
        bundle.putString("video_current_uri", this.mMovieItem.getUri().toString());
        this.mRetryExt.onSaveInstanceState(bundle);
        this.mPlayerExt.onSaveInstanceState(bundle);
        com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "onSaveInstanceState(" + bundle + ")");
    }

    private void onRestoreInstanceState(Bundle bundle) {
        this.mVideoLastDuration = bundle.getInt("video_last_duration");
        this.mVideoCanPause = bundle.getBoolean("video_can_pause");
        this.mVideoCanSeek = bundle.getBoolean("video_can_seek");
        this.mConsumedDrmRight = bundle.getBoolean("consumed_drm_right");
        this.mTState = TState.valueOf(bundle.getString("video_state"));
        this.mMovieItem.setUri(Uri.parse(bundle.getString("video_current_uri")));
        this.mRetryExt.onRestoreInstanceState(bundle);
        this.mPlayerExt.onRestoreInstanceState(bundle);
        com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "onRestoreInstanceState(" + bundle + ")");
    }

    private void clearVideoInfo() {
        this.mVideoPosition = 0;
        this.mVideoLastDuration = 0;
        this.mIsOnlyAudio = false;
        this.mConsumedDrmRight = false;
        this.mIsBuffering = false;
        if (this.mRetryExt != null) {
            this.mRetryExt.removeRetryRunnable();
        }
    }

    private void getVideoInfo(MediaPlayer mediaPlayer) {
        Uri uri = this.mMovieItem.getUri();
        String mimeType = this.mMovieItem.getMimeType();
        if (!MovieUtils.isLocalFile(this.mMovieItem.getVideoType())) {
            int duration = mediaPlayer.getDuration();
            if (duration <= 0 && !MovieUtils.isHttpStreaming(uri, mimeType)) {
                com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "getVideoInfo(), correct type as live streaming");
                this.mMovieItem.setVideoType(3);
            }
            com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "getVideoInfo() duration =" + duration);
        }
    }

    private void checkPlayStatus(int i, int i2) {
        int i3;
        if (this.mFirstBePlayed) {
            if (i2 == -1003 || i2 == -1010 || i2 == -1100) {
                i3 = R.string.VideoView_info_text_network_interrupt;
            } else if (i == VideoConstantUtils.get(860)) {
                i3 = R.string.VideoView_info_text_video_not_supported;
            } else if (i == VideoConstantUtils.get(862)) {
                i3 = R.string.audio_not_supported;
            } else {
                i3 = 0;
            }
            if (i3 != 0) {
                String string = this.mActivityContext.getString(i3);
                Toast.makeText(this.mActivityContext, string, 0).show();
                this.mFirstBePlayed = false;
                com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "checkPlayStatus: " + string);
            }
        }
    }

    private void handleMetadataUpdate(MediaPlayer mediaPlayer, int i, int i2) {
        com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "handleMetadataUpdate entry");
        byte[] albumArt = VideoMetadataUtils.getAlbumArt(mediaPlayer);
        if (albumArt != null) {
            this.mOverlayExt.setLogoPic(albumArt);
            com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "handleMetadataUpdate album size is " + albumArt.length);
            return;
        }
        this.mOverlayExt.setBottomPanel(true, true);
        com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "handleMetadataUpdate album is null");
    }

    private void handleBuffering(int i, int i2) {
        com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "handleBuffering what is " + i + " mIsDialogShow is " + this.mIsDialogShow);
        if (i == 701) {
            this.mIsBuffering = true;
            if (MovieUtils.isHTTP(this.mMovieItem.getVideoType())) {
                this.mController.showLoading(true);
                return;
            }
            return;
        }
        if (i == 702) {
            com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "handleBuffering mTState is " + this.mTState);
            this.mIsBuffering = false;
            if (this.mIsDialogShow || !hasAudiofocus()) {
                this.mPlayerExt.pauseIfNeed();
            }
            if (MovieUtils.isHTTP(this.mMovieItem.getVideoType())) {
                if (this.mTState == TState.PAUSED) {
                    this.mController.showPaused();
                } else {
                    this.mController.showPlaying();
                }
            }
        }
    }

    private void dump() {
        com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "dump() mHasPaused = " + this.mHasPaused + ", mVideoPosition=" + this.mVideoPosition + ", mResumeableTime=" + this.mResumeableTime + ", mVideoLastDuration=" + this.mVideoLastDuration + ", mDragging=" + this.mDragging + ", mConsumedDrmRight=" + this.mConsumedDrmRight + ", mVideoCanSeek=" + this.mVideoCanSeek + ", mVideoCanPause=" + this.mVideoCanPause + ", mTState=" + this.mTState + ", mIsShowResumingDialog=" + this.mIsShowResumingDialog);
    }

    @Override
    public void onPlayPause() {
        if (this.mMediaPlayerWrapper.isPlaying()) {
            if (this.mMediaPlayerWrapper.canPause()) {
                pauseVideo();
                return;
            }
            return;
        }
        playVideo();
    }

    @Override
    public void onSeekStart() {
        com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "onSeekStart() mDragging=" + this.mDragging);
        this.mSeekMovePosition = -1;
        this.mDragging = true;
    }

    @Override
    public void onSeekMove(int i) {
        com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "onSeekMove(" + i + ") mDragging=" + this.mDragging);
        if (MovieUtils.isLocalFile(this.mMovieItem.getVideoType())) {
            this.mMediaPlayerWrapper.seekTo(i);
            this.mSeekMovePosition = i;
        }
    }

    @Override
    public void onSeekEnd(int i, int i2, int i3) {
        com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "onSeekEnd(" + i + ") mDragging=" + this.mDragging + ", mSeekMovePosition=" + this.mSeekMovePosition);
        this.mDragging = false;
        if (this.mSeekMovePosition != i) {
            this.mMediaPlayerWrapper.seekTo(i);
        }
    }

    @Override
    public void onShown() {
        com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "onShown");
        this.mPowerSavingManager.endPowerSaving();
        this.mShowing = true;
        setProgress();
        showSystemUi(true);
    }

    @Override
    public void onHidden() {
        com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "onHidden");
        this.mShowing = false;
        this.mPowerSavingManager.startPowerSaving();
        showSystemUi(false);
    }

    @Override
    public void onReplay() {
        com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "onReplay()");
        this.mFirstBePlayed = true;
        if (this.mRetryExt.handleOnReplay()) {
            return;
        }
        doStartVideoCareDrm(false, 0, 0);
    }

    @Override
    public boolean powerSavingNeedShowController() {
        return this.mPowerSavingManager.isInExtensionDisplay();
    }

    private boolean requestAudioFocus() {
        this.mAudiofocusState = ((AudioManager) this.mContext.getSystemService("audio")).requestAudioFocus(this, 3, 1);
        com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "requestAudioFocus mAudiofocusState= " + this.mAudiofocusState);
        return hasAudiofocus();
    }

    private void abandonAudiofocus() {
        ((AudioManager) this.mContext.getSystemService("audio")).abandonAudioFocus(this);
        this.mAudiofocusState = 0;
    }

    private boolean hasAudiofocus() {
        return this.mAudiofocusState == 1;
    }

    @Override
    public void onAudioFocusChange(int i) {
        com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "AudioFocusChange state is " + i);
        if (i == 1) {
            this.mAudiofocusState = 1;
        } else if (i == -1 || i == -2) {
            this.mAudiofocusState = 0;
            this.mPlayerExt.pauseIfNeed();
        }
    }

    public class MoviePlayerExtension implements IMoviePlayer {
        private boolean mIsLoop;
        private boolean mLastCanPaused;
        private boolean mLastPlaying;
        private boolean mPauseBuffering;
        private boolean mResumeNeed = false;

        public MoviePlayerExtension() {
        }

        @Override
        public void stopVideo() {
            com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "stopVideo()");
            MoviePlayer.this.mTState = TState.STOPED;
            MoviePlayer.this.mMediaPlayerWrapper.clearSeek();
            MoviePlayer.this.mMediaPlayerWrapper.clearDuration();
            MoviePlayer.this.mMediaPlayerWrapper.stop();
            MoviePlayer.this.mMediaPlayerWrapper.setResumed(false);
            if (MoviePlayer.this.mPlaceHolder != null) {
                MoviePlayer.this.mPlaceHolder.setVisibility(0);
            }
            MoviePlayer.this.clearVideoInfo();
            MoviePlayer.this.mFirstBePlayed = false;
            MoviePlayer.this.mController.setCanReplay(true);
            MoviePlayer.this.mController.showEnded();
            MoviePlayer.this.setProgress();
            MoviePlayer.this.mHandler.removeCallbacks(MoviePlayer.this.mProgressChecker);
        }

        @Override
        public boolean canStop() {
            boolean zIsPlayingEnd;
            if (MoviePlayer.this.mController != null && MoviePlayer.this.mOverlayExt != null) {
                zIsPlayingEnd = MoviePlayer.this.mOverlayExt.isPlayingEnd();
            } else {
                zIsPlayingEnd = false;
            }
            com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "canStop() stopped=" + zIsPlayingEnd);
            return !zIsPlayingEnd;
        }

        @Override
        public boolean getLoop() {
            com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "getLoop() return " + this.mIsLoop);
            return this.mIsLoop;
        }

        @Override
        public void setLoop(boolean z) {
            com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "setLoop(" + z + ") mIsLoop=" + this.mIsLoop);
            if (MovieUtils.isLocalFile(MoviePlayer.this.mMovieItem.getVideoType())) {
                this.mIsLoop = z;
                if (MoviePlayer.this.mTState != TState.STOPED) {
                    MoviePlayer.this.mActivityContext.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MoviePlayer.this.mController.setCanReplay(MoviePlayerExtension.this.mIsLoop);
                        }
                    });
                }
            }
        }

        @Override
        public void startNextVideo(IMovieItem iMovieItem) {
            if (iMovieItem != null && iMovieItem != MoviePlayer.this.mMovieItem) {
                MoviePlayer.this.mBookmarker.setBookmark(MoviePlayer.this.mMovieItem.getUri(), MoviePlayer.this.mMediaPlayerWrapper.getCurrentPosition(), MoviePlayer.this.mMediaPlayerWrapper.getDuration());
                MoviePlayer.this.mMediaPlayerWrapper.stop();
                MoviePlayer.this.mMovieView.setVisibility(4);
                MoviePlayer.this.clearVideoInfo();
                MoviePlayer.this.mMovieItem = iMovieItem;
                ((MovieActivity) MoviePlayer.this.mActivityContext).refreshMovieInfo(MoviePlayer.this.mMovieItem);
                MoviePlayer.this.mFirstBePlayed = true;
                MoviePlayer.this.doStartVideoCareDrm(false, 0, 0);
                MoviePlayer.this.mMovieView.setVisibility(0);
            } else {
                com.mediatek.gallery3d.util.Log.e("VP_MoviePlayer", "Cannot play the next video! " + iMovieItem);
            }
            MoviePlayer.this.mActivityContext.closeOptionsMenu();
        }

        public void onRestoreInstanceState(Bundle bundle) {
            this.mIsLoop = bundle.getBoolean("video_is_loop", false);
            if (this.mIsLoop) {
                MoviePlayer.this.mController.setCanReplay(true);
            }
        }

        public void onSaveInstanceState(Bundle bundle) {
            bundle.putBoolean("video_is_loop", this.mIsLoop);
        }

        private void pauseIfNeed() {
            this.mLastCanPaused = canStop() && MoviePlayer.this.mMediaPlayerWrapper.canPause();
            if (this.mLastCanPaused) {
                com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "pauseIfNeed mTState= " + MoviePlayer.this.mTState);
                this.mLastPlaying = MoviePlayer.this.mTState == TState.PLAYING;
                if (!MovieUtils.isLiveStreaming(MoviePlayer.this.mMovieItem.getVideoType()) && MoviePlayer.this.isPlaying() && !MoviePlayer.this.mIsBuffering) {
                    this.mPauseBuffering = true;
                    MoviePlayer.this.mOverlayExt.clearBuffering();
                    MoviePlayer.this.pauseVideo();
                }
            }
            com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "pauseIfNeed() mLastPlaying=" + this.mLastPlaying + ", mLastCanPaused=" + this.mLastCanPaused + ", mPauseBuffering= " + this.mPauseBuffering + " mTState=" + MoviePlayer.this.mTState);
        }

        public boolean pauseBuffering() {
            return this.mPauseBuffering;
        }

        @Override
        public int getVideoType() {
            return MoviePlayer.this.mMovieItem.getVideoType();
        }

        @Override
        public int getVideoPosition() {
            return MoviePlayer.this.mVideoPosition;
        }

        @Override
        public int getVideoLastDuration() {
            return MoviePlayer.this.mVideoLastDuration;
        }

        @Override
        public void startVideo(boolean z, int i, int i2) {
            MoviePlayer.this.doStartVideoCareDrm(z, i, i2);
        }

        @Override
        public void notifyCompletion() {
            MoviePlayer.this.onCompletion();
        }

        @Override
        public SurfaceView getVideoSurface() {
            return getVideoSurface();
        }

        @Override
        public boolean canSeekForward() {
            return MoviePlayer.this.mMediaPlayerWrapper.canSeekForward();
        }

        @Override
        public boolean canSeekBackward() {
            return MoviePlayer.this.mMediaPlayerWrapper.canSeekBackward();
        }

        @Override
        public boolean isVideoCanSeek() {
            return MoviePlayer.this.mVideoCanSeek;
        }

        @Override
        public void seekTo(int i) {
            MoviePlayer.this.mMediaPlayerWrapper.seekTo(i);
        }

        @Override
        public void setDuration(int i) {
            MoviePlayer.this.mMediaPlayerWrapper.setDuration(i);
        }

        @Override
        public int getCurrentPosition() {
            return MoviePlayer.this.mMediaPlayerWrapper.getCurrentPosition();
        }

        @Override
        public int getDuration() {
            return MoviePlayer.this.mMediaPlayerWrapper.getDuration();
        }

        @Override
        public Animation getHideAnimation() {
            return MoviePlayer.this.mController.getHideAnimation();
        }

        @Override
        public boolean isTimeBarEnabled() {
            return MoviePlayer.this.mController.isTimeBarEnabled();
        }

        @Override
        public void updateProgressBar() {
            MoviePlayer.this.mHandler.post(MoviePlayer.this.mProgressChecker);
        }

        @Override
        public void showEnded() {
            MoviePlayer.this.mController.showEnded();
        }

        @Override
        public void showMovieController() {
            MoviePlayer.this.mController.show();
        }

        @Override
        public void showSubtitleViewSetDialog() {
        }
    }

    private class RetryExtension implements MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener {
        private int mRetryCount;
        private int mRetryDuration;
        private int mRetryPosition;
        private final Runnable mRetryRunnable;

        private RetryExtension() {
            this.mRetryRunnable = new Runnable() {
                @Override
                public void run() {
                    com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "mRetryRunnable.run()");
                    RetryExtension.this.retry();
                }
            };
        }

        public void removeRetryRunnable() {
            MoviePlayer.this.mHandler.removeCallbacks(this.mRetryRunnable);
        }

        public void retry() {
            MoviePlayer.this.doStartVideoCareDrm(true, this.mRetryPosition, this.mRetryDuration);
            com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "retry() mRetryCount=" + this.mRetryCount + ", mRetryPosition=" + this.mRetryPosition);
        }

        public void clearRetry() {
            com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "clearRetry() mRetryCount=" + this.mRetryCount);
            this.mRetryCount = 0;
        }

        public boolean reachRetryCount() {
            com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "reachRetryCount() mRetryCount=" + this.mRetryCount);
            if (this.mRetryCount > 3) {
                return true;
            }
            return false;
        }

        public boolean isRetrying() {
            boolean z;
            if (this.mRetryCount > 0) {
                z = true;
            } else {
                z = false;
            }
            com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "isRetrying() mRetryCount=" + this.mRetryCount);
            return z;
        }

        public void onRestoreInstanceState(Bundle bundle) {
            this.mRetryCount = bundle.getInt("video_retry_count");
        }

        public void onSaveInstanceState(Bundle bundle) {
            bundle.putInt("video_retry_count", this.mRetryCount);
        }

        @Override
        public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
            if (i == 261) {
                this.mRetryPosition = MoviePlayer.this.mMediaPlayerWrapper.getCurrentPosition();
                this.mRetryDuration = MoviePlayer.this.mMediaPlayerWrapper.getDuration();
                this.mRetryCount++;
                MoviePlayer.this.mTState = TState.RETRY_ERROR;
                if (reachRetryCount()) {
                    MoviePlayer.this.mOverlayExt.showReconnectingError();
                    MoviePlayer.this.mController.setCanReplay(true);
                    if (MoviePlayer.this.mMediaPlayerWrapper.canPause()) {
                        MoviePlayer.this.mOverlayExt.setCanScrubbing(false);
                    }
                } else {
                    MoviePlayer.this.mOverlayExt.showReconnecting(this.mRetryCount);
                    MoviePlayer.this.mHandler.postDelayed(this.mRetryRunnable, 1500L);
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean onInfo(MediaPlayer mediaPlayer, int i, int i2) {
            if (i == 702) {
                clearRetry();
                return true;
            }
            return false;
        }

        public boolean handleOnReplay() {
            if (!isRetrying()) {
                return false;
            }
            clearRetry();
            int currentPosition = MoviePlayer.this.mMediaPlayerWrapper.getCurrentPosition();
            int duration = MoviePlayer.this.mMediaPlayerWrapper.getDuration();
            MoviePlayer.this.doStartVideoCareDrm(currentPosition > 0, currentPosition, duration);
            com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "onReplay() errorPosition=" + currentPosition + ", errorDuration=" + duration);
            return true;
        }

        public void showRetry() {
            MoviePlayer.this.mOverlayExt.showReconnectingError();
            if (MoviePlayer.this.mVideoCanSeek || MoviePlayer.this.mMediaPlayerWrapper.canSeekForward()) {
                MoviePlayer.this.mMediaPlayerWrapper.seekTo(MoviePlayer.this.mVideoPosition);
            }
            MoviePlayer.this.mMediaPlayerWrapper.setDuration(MoviePlayer.this.mVideoLastDuration);
            this.mRetryPosition = MoviePlayer.this.mVideoPosition;
            this.mRetryDuration = MoviePlayer.this.mVideoLastDuration;
        }
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
        setProgress();
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mediaPlayer, int i, int i2) {
        if (i == 0 && i2 == 0) {
            this.mIsOnlyAudio = true;
        } else {
            this.mIsOnlyAudio = false;
        }
        if (this.mOverlayExt != null) {
            this.mOverlayExt.setBottomPanel(this.mIsOnlyAudio, true);
        }
        com.mediatek.gallery3d.util.Log.d("VP_MoviePlayer", "onVideoSizeChanged(" + i + ", " + i2 + ", mIsAudioOnly = " + this.mIsOnlyAudio);
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i2) {
        this.mRetryExt.onInfo(mediaPlayer, i, i2);
        checkPlayStatus(i, i2);
        if (i == 3) {
            long jCurrentTimeMillis = System.currentTimeMillis();
            android.util.Log.d("Gallery2PerformanceTestCase2", "[Performance Auto Test][VideoPlayback] The duration of open a video end [" + jCurrentTimeMillis + "]");
            com.mediatek.gallery3d.util.Log.d("VP_MoviePlayer", "[CMCC Performance test][Gallery2][Video Playback] open mp4 file end [" + jCurrentTimeMillis + "]");
            if (this.mPlaceHolder != null) {
                this.mPlaceHolder.setVisibility(8);
            }
            if (!hasAudiofocus()) {
                this.mPlayerExt.pauseIfNeed();
            }
        }
        handleBuffering(i, i2);
        if (i == 802 && this.mIsOnlyAudio) {
            handleMetadataUpdate(mediaPlayer, i, i2);
            return false;
        }
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        getVideoInfo(mediaPlayer);
        boolean zCanPause = this.mMediaPlayerWrapper.canPause();
        boolean z = this.mMediaPlayerWrapper.canSeekForward() && this.mMediaPlayerWrapper.canSeekBackward();
        if (this.mPowerSavingEvent != PowerSavingEvent.EVENT_NONE) {
            playbackControlforPowerSaving();
        }
        if (!MovieUtils.isLocalFile(this.mMovieItem.getVideoType())) {
            this.mOverlayExt.setPlayingInfo(MovieUtils.isLiveStreaming(this.mMovieItem.getVideoType()));
        }
        this.mOverlayExt.setCanPause(zCanPause);
        this.mOverlayExt.setCanScrubbing(z);
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
        com.mediatek.gallery3d.util.Log.d("VP_MoviePlayer", "onBufferingUpdate, pauseBuffering = " + this.mPlayerExt.pauseBuffering());
        if (!this.mPlayerExt.pauseBuffering()) {
            this.mOverlayExt.showBuffering((MovieUtils.isRTSP(this.mMovieItem.getVideoType()) || MovieUtils.isLiveStreaming(this.mMovieItem.getVideoType())) ? false : true, i);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        setProgress();
        if (this.mError) {
            com.mediatek.gallery3d.util.Log.e("VP_MoviePlayer", "error occured, exit the video player");
            this.mActivityContext.finish();
        } else {
            if (this.mPlayerExt.getLoop()) {
                onReplay();
                return;
            }
            this.mTState = TState.COMPELTED;
            if (this.mCanReplay || ((MovieActivity) this.mActivityContext).isMultiWindowMode()) {
                this.mPlayerExt.stopVideo();
            } else {
                onCompletion();
            }
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
        int i3;
        this.mError = true;
        if (this.mConsumedDrmRight) {
            com.mediatek.gallery3d.util.Log.v("VP_MoviePlayer", "onError, clear mConsumedDrmRight flag");
            this.mConsumedDrmRight = false;
        }
        if (this.mRetryExt.onError(mediaPlayer, i, i2)) {
            return true;
        }
        this.mHandler.removeCallbacksAndMessages(null);
        this.mHandler.post(this.mProgressChecker);
        this.mController.showErrorMessage("");
        if (this.mMovieView.getWindowToken() != null) {
            if (i == 260) {
                if (i2 == ERROR_BUFFER_DEQUEUE_FAIL) {
                    return true;
                }
                i3 = R.string.VideoView_error_text_bad_file;
            } else if (i == 261) {
                i3 = R.string.VideoView_error_text_cannot_connect_to_server;
            } else if (i == 262) {
                i3 = R.string.VideoView_error_text_type_not_supported;
            } else if (i == 263) {
                i3 = R.string.VideoView_error_text_drm_not_supported;
            } else if (i == 264) {
                i3 = R.string.VideoView_error_text_invalid_connection;
            } else if (i == 200) {
                i3 = R.string.VideoView_error_text_invalid_progressive_playback;
            } else {
                i3 = R.string.VideoView_error_text_unknown;
            }
            dismissAllowingStateLoss();
            if (!this.mActivityContext.isFinishing()) {
                ErrorDialogFragment.newInstance(i3).show(this.mFragmentManager, "ERROR_DIALOG_TAG");
                this.mFragmentManager.executePendingTransactions();
            }
        }
        return true;
    }

    @Override
    public void onSurfaceDestroyed() {
        if (((MovieActivity) this.mActivityContext).isMultiWindowMode() && !this.mPowerSavingManager.isInExtensionDisplay()) {
            com.mediatek.gallery3d.util.Log.d("VP_MoviePlayer", "MultiWindowListener.onSurfaceDestroyed()");
            doOnPause();
        }
    }
}
