package com.android.gallery3d.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.android.gallery3d.R;
import com.android.gallery3d.app.CommonControllerOverlay;
import com.mediatek.gallery3d.video.IContrllerOverlayExt;
import com.mediatek.gallery3d.video.IMovieItem;
import com.mediatek.gallery3d.video.MediaPlayerWrapper;
import com.mediatek.gallery3d.video.MovieUtils;

public class MovieControllerOverlay extends CommonControllerOverlay implements Animation.AnimationListener {
    private Context mContext;
    private final Handler mHandler;
    private boolean mHidden;
    private final Animation mHideAnimation;
    private ImageView mLogoView;
    private LogoViewExt mLogoViewExt;
    private IMovieItem mMovieItem;
    private OverlayExtension mOverlayExt;
    private final Runnable mStartHidingRunnable;

    class AnonymousClass1 implements Runnable {
        final MovieControllerOverlay this$0;

        @Override
        public void run() {
            this.this$0.startHiding();
        }
    }

    public MovieControllerOverlay(Context context, MediaPlayerWrapper mediaPlayerWrapper, IMovieItem iMovieItem) {
        super(context);
        this.mLogoViewExt = new LogoViewExt();
        this.mContext = context;
        setMediaPlayerWrapper(mediaPlayerWrapper);
        this.mMovieItem = iMovieItem;
        this.mHandler = new Handler();
        this.mStartHidingRunnable = new Runnable() {
            @Override
            public void run() {
                if (MovieControllerOverlay.this.mListener == null || !MovieControllerOverlay.this.mListener.powerSavingNeedShowController()) {
                    MovieControllerOverlay.this.startHiding();
                } else {
                    MovieControllerOverlay.this.hide();
                }
            }
        };
        this.mHideAnimation = AnimationUtils.loadAnimation(context, R.anim.player_out);
        this.mHideAnimation.setAnimationListener(this);
        this.mOverlayExt = new OverlayExtension(this, null);
        this.mLogoViewExt.init(context);
        hide();
    }

    public Animation getHideAnimation() {
        return this.mHideAnimation;
    }

    public boolean isTimeBarEnabled() {
        return this.mTimeBar.getScrubbing();
    }

    @Override
    public void showPlaying() {
        if (!this.mOverlayExt.handleShowPlaying()) {
            this.mState = CommonControllerOverlay.State.PLAYING;
            showMainView(this.mPlayPauseReplayView);
        }
        com.mediatek.gallery3d.util.Log.v("VP_MovieController", "showPlaying() state=" + this.mState);
    }

    @Override
    public void showPaused() {
        if (!this.mOverlayExt.handleShowPaused()) {
            this.mState = CommonControllerOverlay.State.PAUSED;
            showMainView(this.mPlayPauseReplayView);
        }
        com.mediatek.gallery3d.util.Log.v("VP_MovieController", "showPaused() state=" + this.mState);
    }

    @Override
    public void showEnded() {
        this.mOverlayExt.onShowEnded();
        this.mState = CommonControllerOverlay.State.ENDED;
        showMainView(this.mPlayPauseReplayView);
        com.mediatek.gallery3d.util.Log.v("VP_MovieController", "showEnded() state=" + this.mState);
    }

    public void showLoading(boolean z) {
        this.mOverlayExt.onShowLoading(z);
        this.mState = CommonControllerOverlay.State.LOADING;
        showMainView(this.mLoadingView);
        com.mediatek.gallery3d.util.Log.v("VP_MovieController", "showLoading() state=" + this.mState);
    }

    @Override
    public void showErrorMessage(String str) {
        this.mOverlayExt.onShowErrorMessage(str);
        this.mState = CommonControllerOverlay.State.ERROR;
        int measuredWidth = (int) (getMeasuredWidth() * 0.16666667f);
        this.mErrorView.setPadding(measuredWidth, this.mErrorView.getPaddingTop(), measuredWidth, this.mErrorView.getPaddingBottom());
        this.mErrorView.setText(str);
        showMainView(this.mErrorView);
    }

    @Override
    protected void createTimeBar(Context context) {
        this.mTimeBar = new TimeBar(context, this);
        this.mTimeBar.setId(8);
    }

    @Override
    public void setTimes(int i, int i2, int i3, int i4) {
        this.mTimeBar.setTime(i, i2, i3, i4);
    }

    @Override
    public void hide() {
        boolean z = this.mHidden;
        this.mHidden = true;
        if (this.mListener == null || (this.mListener != null && !this.mListener.powerSavingNeedShowController())) {
            this.mPlayPauseReplayView.setVisibility(4);
            this.mLoadingView.setVisibility(4);
            if (!this.mOverlayExt.handleHide()) {
                setVisibility(4);
            }
            this.mBackground.setVisibility(4);
            this.mTimeBar.setVisibility(4);
        }
        setFocusable(true);
        requestFocus();
        if (this.mListener != null && z != this.mHidden) {
            this.mListener.onHidden();
        }
        com.mediatek.gallery3d.util.Log.v("VP_MovieController", "hide() wasHidden=" + z + ", hidden=" + this.mHidden);
    }

    private void showMainView(View view) {
        this.mMainView = view;
        this.mErrorView.setVisibility(this.mMainView == this.mErrorView ? 0 : 4);
        this.mLoadingView.setVisibility(this.mMainView == this.mLoadingView ? 0 : 4);
        this.mPlayPauseReplayView.setVisibility(this.mMainView == this.mPlayPauseReplayView ? 0 : 4);
        this.mOverlayExt.onShowMainView();
        show();
    }

    @Override
    public void show() {
        if (this.mListener != null) {
            boolean z = this.mHidden;
            this.mHidden = false;
            updateViews();
            setVisibility(0);
            setFocusable(false);
            if (this.mListener != null && z != this.mHidden) {
                this.mListener.onShown();
            }
            maybeStartHiding();
            com.mediatek.gallery3d.util.Log.v("VP_MovieController", "show() wasHidden=" + z + ", hidden=" + this.mHidden + ", listener=" + this.mListener);
        }
    }

    private void maybeStartHiding() {
        cancelHiding();
        if (this.mState == CommonControllerOverlay.State.PLAYING) {
            this.mHandler.postDelayed(this.mStartHidingRunnable, 3000L);
        }
        com.mediatek.gallery3d.util.Log.v("VP_MovieController", "maybeStartHiding() state=" + this.mState);
    }

    private void startHiding() {
        startHideAnimation(this.mBackground);
        startHideAnimation(this.mTimeBar);
        startHideAnimation(this.mPlayPauseReplayView);
    }

    private void startHideAnimation(View view) {
        if (view.getVisibility() == 0) {
            view.startAnimation(this.mHideAnimation);
        }
    }

    private void cancelHiding() {
        this.mHandler.removeCallbacks(this.mStartHidingRunnable);
        this.mBackground.setAnimation(null);
        this.mTimeBar.setAnimation(null);
        this.mPlayPauseReplayView.setAnimation(null);
    }

    @Override
    public void onAnimationStart(Animation animation) {
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        hide();
    }

    @Override
    public void onClick(View view) {
        com.mediatek.gallery3d.util.Log.v("VP_MovieController", "onClick(" + view + ") listener=" + this.mListener + ", state=" + this.mState + ", canReplay=" + this.mCanReplay);
        if (this.mListener != null && view == this.mPlayPauseReplayView) {
            if (this.mState == CommonControllerOverlay.State.ENDED || this.mState == CommonControllerOverlay.State.RETRY_CONNECTING_ERROR) {
                this.mListener.onReplay();
            } else if (this.mState == CommonControllerOverlay.State.PAUSED || this.mState == CommonControllerOverlay.State.PLAYING) {
                this.mListener.onPlayPause();
            }
        }
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (this.mHidden) {
            show();
        }
        return super.onKeyDown(i, keyEvent);
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        ((Activity) this.mContext).getWindowManager().getDefaultDisplay().getWidth();
        Rect rect = this.mWindowInsets;
        int i5 = rect.left;
        int i6 = rect.right;
        int i7 = rect.top;
        int i8 = i4 - i2;
        int i9 = i3 - i;
        int i10 = i8 - rect.bottom;
        this.mBackground.layout(0, i10 - this.mTimeBar.getPreferredHeight(), i9, i10);
        this.mTimeBar.layout(i5, i10 - this.mTimeBar.getPreferredHeight(), i9 - i6, i10);
        layoutCenteredView(this.mPlayPauseReplayView, 0, 0, i9, i8);
        layoutCenteredView(this.mAudioOnlyView, 0, 0, i9, i8);
        if (this.mMainView != null) {
            layoutCenteredView(this.mMainView, 0, 0, i9, i8);
        }
    }

    @Override
    protected void updateViews() {
        int i;
        if (this.mHidden) {
            return;
        }
        int i2 = 0;
        this.mBackground.setVisibility(0);
        this.mTimeBar.setVisibility(0);
        ImageView imageView = this.mPlayPauseReplayView;
        if (this.mState == CommonControllerOverlay.State.PAUSED) {
            i = R.drawable.videoplayer_play;
        } else {
            i = this.mState == CommonControllerOverlay.State.PLAYING ? R.drawable.videoplayer_pause : R.drawable.videoplayer_reload;
        }
        imageView.setImageResource(i);
        if (!this.mOverlayExt.handleUpdateViews()) {
            ImageView imageView2 = this.mPlayPauseReplayView;
            if (this.mState == CommonControllerOverlay.State.LOADING || this.mState == CommonControllerOverlay.State.ERROR || (this.mState == CommonControllerOverlay.State.ENDED && !this.mCanReplay)) {
                i2 = 8;
            }
            imageView2.setVisibility(i2);
        }
        requestLayout();
        com.mediatek.gallery3d.util.Log.v("VP_MovieController", "updateViews() state=" + this.mState + ", canReplay=" + this.mCanReplay);
    }

    @Override
    public void onScrubbingStart() {
        cancelHiding();
        super.onScrubbingStart();
    }

    @Override
    public void onScrubbingMove(int i) {
        cancelHiding();
        super.onScrubbingMove(i);
    }

    @Override
    public void onScrubbingEnd(int i, int i2, int i3) {
        maybeStartHiding();
        super.onScrubbingEnd(i, i2, i3);
    }

    public IContrllerOverlayExt getOverlayExt() {
        return this.mOverlayExt;
    }

    private class OverlayExtension implements IContrllerOverlayExt {
        private boolean mAlwaysShowBottom;
        private boolean mCanPause;
        private boolean mEnableScrubbing;
        private CommonControllerOverlay.State mLastState;
        private Drawable mLogoPic;
        private String mPlayingInfo;

        private OverlayExtension() {
            this.mLastState = CommonControllerOverlay.State.PLAYING;
            this.mCanPause = true;
            this.mEnableScrubbing = false;
        }

        OverlayExtension(MovieControllerOverlay movieControllerOverlay, AnonymousClass1 anonymousClass1) {
            this();
        }

        @Override
        public void showBuffering(boolean z, int i) {
            com.mediatek.gallery3d.util.Log.v("VP_MovieController", "showBuffering(" + z + ", " + i + ") lastState=" + this.mLastState + ", state=" + MovieControllerOverlay.this.mState);
            if (z) {
                MovieControllerOverlay.this.mTimeBar.setSecondaryProgress(i);
                return;
            }
            if (MovieControllerOverlay.this.mState == CommonControllerOverlay.State.PAUSED || MovieControllerOverlay.this.mState == CommonControllerOverlay.State.PLAYING) {
                this.mLastState = MovieControllerOverlay.this.mState;
            }
            if (i < 0 || i >= 100) {
                if (i == 100) {
                    MovieControllerOverlay.this.mState = this.mLastState;
                    MovieControllerOverlay.this.mTimeBar.setInfo(null);
                    MovieControllerOverlay.this.showMainView(MovieControllerOverlay.this.mPlayPauseReplayView);
                    return;
                } else {
                    MovieControllerOverlay.this.mState = this.mLastState;
                    MovieControllerOverlay.this.mTimeBar.setInfo(null);
                    return;
                }
            }
            MovieControllerOverlay.this.mState = CommonControllerOverlay.State.BUFFERING;
            MovieControllerOverlay.this.mTimeBar.setInfo(String.format(MovieControllerOverlay.this.getResources().getString(R.string.media_controller_buffering), Integer.valueOf(i)));
            MovieControllerOverlay.this.showMainView(MovieControllerOverlay.this.mLoadingView);
        }

        @Override
        public void clearBuffering() {
            com.mediatek.gallery3d.util.Log.v("VP_MovieController", "clearBuffering()");
            MovieControllerOverlay.this.mTimeBar.setSecondaryProgress(-1);
            showBuffering(false, -1);
        }

        @Override
        public void onCancelHiding() {
            MovieControllerOverlay.this.cancelHiding();
        }

        @Override
        public void showReconnecting(int i) {
            clearBuffering();
            MovieControllerOverlay.this.mState = CommonControllerOverlay.State.RETRY_CONNECTING;
            MovieControllerOverlay.this.mTimeBar.setInfo(MovieControllerOverlay.this.getResources().getString(R.string.VideoView_error_text_cannot_connect_retry, Integer.valueOf(i)));
            MovieControllerOverlay.this.showMainView(MovieControllerOverlay.this.mLoadingView);
            com.mediatek.gallery3d.util.Log.v("VP_MovieController", "showReconnecting(" + i + ")");
        }

        @Override
        public void showReconnectingError() {
            clearBuffering();
            MovieControllerOverlay.this.mState = CommonControllerOverlay.State.RETRY_CONNECTING_ERROR;
            MovieControllerOverlay.this.mTimeBar.setInfo(MovieControllerOverlay.this.getResources().getString(R.string.VideoView_error_text_cannot_connect_to_server));
            MovieControllerOverlay.this.showMainView(MovieControllerOverlay.this.mPlayPauseReplayView);
            com.mediatek.gallery3d.util.Log.v("VP_MovieController", "showReconnectingError()");
        }

        @Override
        public void setPlayingInfo(boolean z) {
            int i;
            if (z) {
                i = R.string.media_controller_live;
            } else {
                i = R.string.media_controller_playing;
            }
            this.mPlayingInfo = MovieControllerOverlay.this.getResources().getString(i);
            com.mediatek.gallery3d.util.Log.v("VP_MovieController", "setPlayingInfo(" + z + ") playingInfo=" + this.mPlayingInfo);
        }

        @Override
        public void setCanPause(boolean z) {
            this.mCanPause = z;
            com.mediatek.gallery3d.util.Log.v("VP_MovieController", "setCanPause(" + z + ")");
        }

        @Override
        public void setCanScrubbing(boolean z) {
            this.mEnableScrubbing = z;
            MovieControllerOverlay.this.mTimeBar.setScrubbing(z);
            com.mediatek.gallery3d.util.Log.v("VP_MovieController", "setCanScrubbing(" + z + ")");
        }

        @Override
        public void setBottomPanel(boolean z, boolean z2) {
            this.mAlwaysShowBottom = z;
            if (!z) {
                MovieControllerOverlay.this.mAudioOnlyView.setVisibility(4);
                MovieControllerOverlay.this.setBackgroundColor(0);
                if (this.mLogoPic != null) {
                    com.mediatek.gallery3d.util.Log.v("VP_MovieController", "setBottomPanel() dissmiss orange logo picuture");
                    this.mLogoPic = null;
                    MovieControllerOverlay.this.mLogoView.setImageDrawable(null);
                    MovieControllerOverlay.this.mLogoView.setBackgroundColor(0);
                    MovieControllerOverlay.this.mLogoView.setVisibility(8);
                }
            } else {
                if (this.mLogoPic != null) {
                    MovieControllerOverlay.this.mAudioOnlyView.setVisibility(4);
                    MovieControllerOverlay.this.mLogoView.setImageDrawable(this.mLogoPic);
                } else {
                    MovieControllerOverlay.this.setBackgroundColor(-16777216);
                    MovieControllerOverlay.this.mAudioOnlyView.setVisibility(0);
                }
                if (z2) {
                    MovieControllerOverlay.this.setVisibility(0);
                }
            }
            com.mediatek.gallery3d.util.Log.v("VP_MovieController", "setBottomPanel(" + z + ", " + z2 + ")");
        }

        public boolean handleHide() {
            com.mediatek.gallery3d.util.Log.v("VP_MovieController", "handleHide() mAlwaysShowBottom" + this.mAlwaysShowBottom);
            return this.mAlwaysShowBottom;
        }

        @Override
        public void setLogoPic(byte[] bArr) {
            Drawable drawableBytesToDrawable = MovieUtils.bytesToDrawable(bArr);
            MovieControllerOverlay.this.setBackgroundDrawable(null);
            MovieControllerOverlay.this.mLogoView.setBackgroundColor(-16777216);
            MovieControllerOverlay.this.mLogoView.setImageDrawable(drawableBytesToDrawable);
            MovieControllerOverlay.this.mLogoView.setVisibility(0);
            this.mLogoPic = drawableBytesToDrawable;
        }

        @Override
        public boolean isPlayingEnd() {
            com.mediatek.gallery3d.util.Log.v("VP_MovieController", "isPlayingEnd() state=" + MovieControllerOverlay.this.mState);
            if (CommonControllerOverlay.State.ENDED == MovieControllerOverlay.this.mState || CommonControllerOverlay.State.ERROR == MovieControllerOverlay.this.mState || CommonControllerOverlay.State.RETRY_CONNECTING_ERROR == MovieControllerOverlay.this.mState) {
                return true;
            }
            return false;
        }

        public boolean handleShowPlaying() {
            if (MovieControllerOverlay.this.mState == CommonControllerOverlay.State.BUFFERING) {
                this.mLastState = CommonControllerOverlay.State.PLAYING;
                return true;
            }
            return false;
        }

        public boolean handleShowPaused() {
            MovieControllerOverlay.this.mTimeBar.setInfo(null);
            if (MovieControllerOverlay.this.mState == CommonControllerOverlay.State.BUFFERING) {
                this.mLastState = CommonControllerOverlay.State.PAUSED;
                return true;
            }
            return false;
        }

        public void onShowLoading(boolean z) {
            int i;
            if (z) {
                i = R.string.VideoView_info_buffering;
            } else {
                i = R.string.media_controller_connecting;
            }
            MovieControllerOverlay.this.mTimeBar.setInfo(MovieControllerOverlay.this.getResources().getString(i));
        }

        public void onShowEnded() {
            clearBuffering();
            MovieControllerOverlay.this.mTimeBar.setInfo(null);
        }

        public void onShowErrorMessage(String str) {
            clearBuffering();
        }

        public boolean handleUpdateViews() {
            int i;
            ImageView imageView = MovieControllerOverlay.this.mPlayPauseReplayView;
            if (MovieControllerOverlay.this.mState != CommonControllerOverlay.State.LOADING && MovieControllerOverlay.this.mState != CommonControllerOverlay.State.ERROR && MovieControllerOverlay.this.mState != CommonControllerOverlay.State.BUFFERING && MovieControllerOverlay.this.mState != CommonControllerOverlay.State.RETRY_CONNECTING && (MovieControllerOverlay.this.mState == CommonControllerOverlay.State.ENDED || MovieControllerOverlay.this.mState == CommonControllerOverlay.State.RETRY_CONNECTING_ERROR || this.mCanPause)) {
                i = 0;
            } else {
                i = 8;
            }
            imageView.setVisibility(i);
            if (this.mPlayingInfo != null && MovieControllerOverlay.this.mState == CommonControllerOverlay.State.PLAYING) {
                MovieControllerOverlay.this.mTimeBar.setInfo(this.mPlayingInfo);
                return true;
            }
            return true;
        }

        public void onShowMainView() {
            com.mediatek.gallery3d.util.Log.v("VP_MovieController", "onShowMainView() enableScrubbing=" + this.mEnableScrubbing + ", state=" + MovieControllerOverlay.this.mState);
            if (this.mEnableScrubbing && (MovieControllerOverlay.this.mState == CommonControllerOverlay.State.PAUSED || MovieControllerOverlay.this.mState == CommonControllerOverlay.State.PLAYING)) {
                MovieControllerOverlay.this.mTimeBar.setScrubbing(true);
            } else {
                MovieControllerOverlay.this.mTimeBar.setScrubbing(false);
            }
        }
    }

    class LogoViewExt {
        LogoViewExt() {
        }

        private void init(Context context) {
            if (context instanceof MovieActivity) {
                RelativeLayout relativeLayout = (RelativeLayout) ((MovieActivity) MovieControllerOverlay.this.mContext).findViewById(R.id.movie_view_root);
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-1, -1, 17);
                MovieControllerOverlay.this.mLogoView = new ImageView(MovieControllerOverlay.this.mContext);
                MovieControllerOverlay.this.mLogoView.setAdjustViewBounds(true);
                MovieControllerOverlay.this.mLogoView.setMaxWidth(((MovieActivity) MovieControllerOverlay.this.mContext).getWindowManager().getDefaultDisplay().getWidth());
                MovieControllerOverlay.this.mLogoView.setMaxHeight(((MovieActivity) MovieControllerOverlay.this.mContext).getWindowManager().getDefaultDisplay().getHeight());
                relativeLayout.addView(MovieControllerOverlay.this.mLogoView, layoutParams);
                MovieControllerOverlay.this.mLogoView.setVisibility(8);
            }
        }
    }
}
