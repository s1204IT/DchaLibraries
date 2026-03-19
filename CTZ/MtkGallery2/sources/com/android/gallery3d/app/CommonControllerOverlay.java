package com.android.gallery3d.app;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.gallery3d.R;
import com.android.gallery3d.app.ControllerOverlay;
import com.android.gallery3d.app.TimeBar;
import com.mediatek.gallery3d.video.MediaPlayerWrapper;

public abstract class CommonControllerOverlay extends FrameLayout implements View.OnClickListener, ControllerOverlay, TimeBar.Listener {
    protected final ImageView mAudioOnlyView;
    protected final View mBackground;
    protected boolean mCanReplay;
    protected final TextView mErrorView;
    protected ControllerOverlay.Listener mListener;
    protected final LinearLayout mLoadingView;
    protected View mMainView;
    protected MediaPlayerWrapper mMediaPlayerWrapper;
    protected final ImageView mPlayPauseReplayView;
    protected State mState;
    protected TimeBar mTimeBar;
    protected final Rect mWindowInsets;

    public enum State {
        PLAYING,
        PAUSED,
        ENDED,
        ERROR,
        LOADING,
        BUFFERING,
        RETRY_CONNECTING,
        RETRY_CONNECTING_ERROR
    }

    protected abstract void createTimeBar(Context context);

    public void setMediaPlayerWrapper(MediaPlayerWrapper mediaPlayerWrapper) {
        this.mMediaPlayerWrapper = mediaPlayerWrapper;
        onPlayerWrapperChanged();
    }

    protected void onPlayerWrapperChanged() {
    }

    public CommonControllerOverlay(Context context) {
        super(context);
        this.mCanReplay = true;
        this.mWindowInsets = new Rect();
        this.mState = State.LOADING;
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-2, -2);
        FrameLayout.LayoutParams layoutParams2 = new FrameLayout.LayoutParams(-1, -1);
        this.mAudioOnlyView = new ImageView(context);
        this.mAudioOnlyView.setImageResource(R.drawable.ic_media_audio_only_video);
        this.mAudioOnlyView.setScaleType(ImageView.ScaleType.CENTER);
        addView(this.mAudioOnlyView, layoutParams);
        this.mAudioOnlyView.setVisibility(8);
        this.mBackground = new View(context);
        this.mBackground.setBackgroundColor(context.getResources().getColor(R.color.darker_transparent));
        addView(this.mBackground, layoutParams2);
        createTimeBar(context);
        addView(this.mTimeBar, layoutParams);
        this.mTimeBar.setContentDescription(context.getResources().getString(R.string.accessibility_time_bar));
        this.mLoadingView = new LinearLayout(context);
        this.mLoadingView.setOrientation(1);
        this.mLoadingView.setGravity(1);
        ProgressBar progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true);
        this.mLoadingView.addView(progressBar, layoutParams);
        addView(this.mLoadingView, layoutParams);
        this.mPlayPauseReplayView = new ImageView(context);
        this.mPlayPauseReplayView.setContentDescription(context.getResources().getString(R.string.accessibility_play_video));
        this.mPlayPauseReplayView.setBackgroundResource(R.drawable.bg_vidcontrol);
        this.mPlayPauseReplayView.setScaleType(ImageView.ScaleType.CENTER);
        this.mPlayPauseReplayView.setFocusable(true);
        this.mPlayPauseReplayView.setClickable(true);
        this.mPlayPauseReplayView.setOnClickListener(this);
        addView(this.mPlayPauseReplayView, layoutParams);
        this.mErrorView = createOverlayTextView(context);
        addView(this.mErrorView, layoutParams2);
        setLayoutParams(new RelativeLayout.LayoutParams(-1, -1));
    }

    private TextView createOverlayTextView(Context context) {
        TextView textView = new TextView(context);
        textView.setGravity(17);
        textView.setTextColor(-1);
        textView.setPadding(0, 15, 0, 15);
        return textView;
    }

    public void setListener(ControllerOverlay.Listener listener) {
        this.mListener = listener;
    }

    public void setCanReplay(boolean z) {
        this.mCanReplay = z;
    }

    public View getView() {
        return this;
    }

    public void showPlaying() {
        this.mState = State.PLAYING;
        showMainView(this.mPlayPauseReplayView);
    }

    public void showPaused() {
        this.mState = State.PAUSED;
        showMainView(this.mPlayPauseReplayView);
    }

    public void showEnded() {
        this.mState = State.ENDED;
        if (this.mCanReplay) {
            showMainView(this.mPlayPauseReplayView);
        }
    }

    public void showErrorMessage(String str) {
        this.mState = State.ERROR;
        int measuredWidth = (int) (getMeasuredWidth() * 0.16666667f);
        this.mErrorView.setPadding(measuredWidth, this.mErrorView.getPaddingTop(), measuredWidth, this.mErrorView.getPaddingBottom());
        this.mErrorView.setText(str);
        showMainView(this.mErrorView);
    }

    public void setTimes(int i, int i2, int i3, int i4) {
        this.mTimeBar.setTime(i, i2, i3, i4);
    }

    public void hide() {
        this.mPlayPauseReplayView.setVisibility(4);
        this.mLoadingView.setVisibility(4);
        this.mBackground.setVisibility(4);
        this.mTimeBar.setVisibility(4);
        setVisibility(4);
        setFocusable(true);
        requestFocus();
    }

    private void showMainView(View view) {
        this.mMainView = view;
        this.mErrorView.setVisibility(this.mMainView == this.mErrorView ? 0 : 4);
        this.mLoadingView.setVisibility(this.mMainView == this.mLoadingView ? 0 : 4);
        this.mPlayPauseReplayView.setVisibility(this.mMainView == this.mPlayPauseReplayView ? 0 : 4);
        show();
    }

    public void show() {
        updateViews();
        setVisibility(0);
        setFocusable(false);
    }

    @Override
    public void onClick(View view) {
        if (this.mListener != null && view == this.mPlayPauseReplayView) {
            if (this.mState == State.ENDED || this.mState == State.RETRY_CONNECTING_ERROR) {
                if (this.mCanReplay) {
                    this.mListener.onReplay();
                }
            } else if (this.mState == State.PAUSED || this.mState == State.PLAYING) {
                this.mListener.onPlayPause();
            }
        }
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        return super.onKeyDown(i, keyEvent);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (super.onTouchEvent(motionEvent)) {
            return true;
        }
        return false;
    }

    @Override
    protected boolean fitSystemWindows(Rect rect) {
        this.mWindowInsets.set(rect);
        return true;
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        Rect rect = this.mWindowInsets;
        int i5 = rect.left;
        int i6 = rect.right;
        int i7 = rect.top;
        int i8 = i4 - i2;
        int i9 = i3 - i;
        int i10 = i8 - rect.bottom;
        this.mBackground.layout(0, i10 - this.mTimeBar.getBarHeight(), i9, i10);
        this.mTimeBar.layout(i5, i10 - this.mTimeBar.getPreferredHeight(), i9 - i6, i10);
        layoutCenteredView(this.mPlayPauseReplayView, 0, 0, i9, i8);
        if (this.mMainView != null) {
            layoutCenteredView(this.mMainView, 0, 0, i9, i8);
        }
    }

    protected void layoutCenteredView(View view, int i, int i2, int i3, int i4) {
        int measuredWidth = view.getMeasuredWidth();
        int measuredHeight = view.getMeasuredHeight();
        int i5 = ((i3 - i) - measuredWidth) / 2;
        int i6 = ((i4 - i2) - measuredHeight) / 2;
        view.layout(i5, i6, measuredWidth + i5, measuredHeight + i6);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        measureChildren(i, i2);
    }

    protected void updateViews() {
        int i;
        String string;
        int i2 = 0;
        this.mBackground.setVisibility(0);
        this.mTimeBar.setVisibility(0);
        Resources resources = getContext().getResources();
        String string2 = resources.getString(R.string.accessibility_reload_video);
        if (this.mState == State.PAUSED) {
            i = R.drawable.ic_vidcontrol_play;
            string = resources.getString(R.string.accessibility_play_video);
        } else if (this.mState == State.PLAYING) {
            i = R.drawable.ic_vidcontrol_pause;
            string = resources.getString(R.string.accessibility_pause_video);
        } else {
            i = R.drawable.ic_vidcontrol_reload;
            string = string2;
        }
        this.mPlayPauseReplayView.setImageResource(i);
        this.mPlayPauseReplayView.setContentDescription(string);
        ImageView imageView = this.mPlayPauseReplayView;
        if (this.mState == State.LOADING || this.mState == State.ERROR || (this.mState == State.ENDED && !this.mCanReplay)) {
            i2 = 8;
        }
        imageView.setVisibility(i2);
        requestLayout();
    }

    @Override
    public void onScrubbingStart() {
        if (this.mListener != null) {
            this.mListener.onSeekStart();
        }
    }

    @Override
    public void onScrubbingMove(int i) {
        if (this.mListener != null) {
            this.mListener.onSeekMove(i);
        }
    }

    @Override
    public void onScrubbingEnd(int i, int i2, int i3) {
        if (this.mListener != null) {
            this.mListener.onSeekEnd(i, i2, i3);
        }
    }
}
