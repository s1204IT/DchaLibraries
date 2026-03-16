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

public abstract class CommonControllerOverlay extends FrameLayout implements View.OnClickListener, ControllerOverlay, TimeBar.Listener {
    protected final View mBackground;
    protected boolean mCanReplay;
    protected final TextView mErrorView;
    protected ControllerOverlay.Listener mListener;
    protected final LinearLayout mLoadingView;
    protected View mMainView;
    protected final ImageView mPlayPauseReplayView;
    protected State mState;
    protected TimeBar mTimeBar;
    private final Rect mWindowInsets;

    protected enum State {
        PLAYING,
        PAUSED,
        ENDED,
        ERROR,
        LOADING
    }

    protected abstract void createTimeBar(Context context);

    public void setSeekable(boolean canSeek) {
        this.mTimeBar.setSeekable(canSeek);
    }

    public CommonControllerOverlay(Context context) {
        super(context);
        this.mCanReplay = true;
        this.mWindowInsets = new Rect();
        this.mState = State.LOADING;
        FrameLayout.LayoutParams wrapContent = new FrameLayout.LayoutParams(-2, -2);
        FrameLayout.LayoutParams matchParent = new FrameLayout.LayoutParams(-1, -1);
        this.mBackground = new View(context);
        this.mBackground.setBackgroundColor(context.getResources().getColor(R.color.darker_transparent));
        addView(this.mBackground, matchParent);
        createTimeBar(context);
        addView(this.mTimeBar, wrapContent);
        this.mTimeBar.setContentDescription(context.getResources().getString(R.string.accessibility_time_bar));
        this.mLoadingView = new LinearLayout(context);
        this.mLoadingView.setOrientation(1);
        this.mLoadingView.setGravity(1);
        ProgressBar spinner = new ProgressBar(context);
        spinner.setIndeterminate(true);
        this.mLoadingView.addView(spinner, wrapContent);
        TextView loadingText = createOverlayTextView(context);
        loadingText.setText(R.string.loading_video);
        this.mLoadingView.addView(loadingText, wrapContent);
        addView(this.mLoadingView, wrapContent);
        this.mPlayPauseReplayView = new ImageView(context);
        this.mPlayPauseReplayView.setImageResource(R.drawable.ic_vidcontrol_play);
        this.mPlayPauseReplayView.setContentDescription(context.getResources().getString(R.string.accessibility_play_video));
        this.mPlayPauseReplayView.setBackgroundResource(R.drawable.bg_vidcontrol);
        this.mPlayPauseReplayView.setScaleType(ImageView.ScaleType.CENTER);
        this.mPlayPauseReplayView.setFocusable(true);
        this.mPlayPauseReplayView.setClickable(true);
        this.mPlayPauseReplayView.setOnClickListener(this);
        addView(this.mPlayPauseReplayView, wrapContent);
        this.mErrorView = createOverlayTextView(context);
        addView(this.mErrorView, matchParent);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(-1, -1);
        setLayoutParams(params);
        hide();
    }

    private TextView createOverlayTextView(Context context) {
        TextView view = new TextView(context);
        view.setGravity(17);
        view.setTextColor(-1);
        view.setPadding(0, 15, 0, 15);
        return view;
    }

    public void setListener(ControllerOverlay.Listener listener) {
        this.mListener = listener;
    }

    public void setCanReplay(boolean canReplay) {
        this.mCanReplay = canReplay;
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

    public void showLoading() {
        this.mState = State.LOADING;
        showMainView(this.mLoadingView);
    }

    public void showErrorMessage(String message) {
        this.mState = State.ERROR;
        int padding = (int) (getMeasuredWidth() * 0.16666667f);
        this.mErrorView.setPadding(padding, this.mErrorView.getPaddingTop(), padding, this.mErrorView.getPaddingBottom());
        this.mErrorView.setText(message);
        showMainView(this.mErrorView);
    }

    public void setTimes(int currentTime, int totalTime, int trimStartTime, int trimEndTime) {
        this.mTimeBar.setTime(currentTime, totalTime, trimStartTime, trimEndTime);
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
        this.mPlayPauseReplayView.setVisibility(this.mMainView != this.mPlayPauseReplayView ? 4 : 0);
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
            if (this.mState == State.ENDED) {
                if (this.mCanReplay) {
                    this.mListener.onReplay();
                }
            } else if (this.mState == State.PAUSED || this.mState == State.PLAYING) {
                this.mListener.onPlayPause();
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    @Override
    protected boolean fitSystemWindows(Rect insets) {
        this.mWindowInsets.set(insets);
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        Rect insets = this.mWindowInsets;
        int pl = insets.left;
        int pr = insets.right;
        int i = insets.top;
        int pb = insets.bottom;
        int h = bottom - top;
        int w = right - left;
        if (this.mErrorView.getVisibility() == 0) {
        }
        int y = h - pb;
        this.mBackground.layout(0, y - this.mTimeBar.getBarHeight(), w, y);
        this.mTimeBar.layout(pl, y - this.mTimeBar.getPreferredHeight(), w - pr, y);
        layoutCenteredView(this.mPlayPauseReplayView, 0, 0, w, h);
        if (this.mMainView != null) {
            layoutCenteredView(this.mMainView, 0, 0, w, h);
        }
    }

    private void layoutCenteredView(View view, int l, int t, int r, int b) {
        int cw = view.getMeasuredWidth();
        int ch = view.getMeasuredHeight();
        int cl = ((r - l) - cw) / 2;
        int ct = ((b - t) - ch) / 2;
        view.layout(cl, ct, cl + cw, ct + ch);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureChildren(widthMeasureSpec, heightMeasureSpec);
    }

    protected void updateViews() {
        int i = 0;
        this.mBackground.setVisibility(0);
        this.mTimeBar.setVisibility(0);
        Resources resources = getContext().getResources();
        int imageResource = R.drawable.ic_vidcontrol_reload;
        String contentDescription = resources.getString(R.string.accessibility_reload_video);
        if (this.mState == State.PAUSED) {
            imageResource = R.drawable.ic_vidcontrol_play;
            contentDescription = resources.getString(R.string.accessibility_play_video);
        } else if (this.mState == State.PLAYING) {
            imageResource = R.drawable.ic_vidcontrol_pause;
            contentDescription = resources.getString(R.string.accessibility_pause_video);
        }
        this.mPlayPauseReplayView.setImageResource(imageResource);
        this.mPlayPauseReplayView.setContentDescription(contentDescription);
        ImageView imageView = this.mPlayPauseReplayView;
        if (this.mState == State.LOADING || this.mState == State.ERROR || (this.mState == State.ENDED && !this.mCanReplay)) {
            i = 8;
        }
        imageView.setVisibility(i);
        requestLayout();
    }

    @Override
    public void onScrubbingStart() {
        this.mListener.onSeekStart();
    }

    @Override
    public void onScrubbingMove(int time) {
        this.mListener.onSeekMove(time);
    }

    @Override
    public void onScrubbingEnd(int time, int trimStartTime, int trimEndTime) {
        this.mListener.onSeekEnd(time, trimStartTime, trimEndTime);
    }
}
