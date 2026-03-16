package com.android.camera.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import com.android.camera.CameraActivity;
import com.android.camera.app.CameraAppUI;
import com.android.camera.debug.Log;
import com.android.camera.util.CameraUtil;
import com.android.camera.widget.FilmstripLayout;
import com.android.camera2.R;

public class MainActivityLayout extends FrameLayout {
    private static final int SWIPE_TIME_OUT = 500;
    private final Log.Tag TAG;
    private boolean mCheckToIntercept;
    private MotionEvent mDown;
    private FilmstripLayout mFilmstripLayout;
    private final boolean mIsCaptureIntent;
    private ModeListView mModeList;
    private CameraAppUI.NonDecorWindowSizeChangedListener mNonDecorWindowSizeChangedListener;
    private boolean mRequestToInterceptTouchEvents;
    private final int mSlop;

    @Deprecated
    private boolean mSwipeEnabled;
    private View mTouchReceiver;

    public MainActivityLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.TAG = new Log.Tag("MainActivityLayout");
        this.mRequestToInterceptTouchEvents = false;
        this.mTouchReceiver = null;
        this.mNonDecorWindowSizeChangedListener = null;
        this.mSwipeEnabled = true;
        this.mSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        Activity activity = (Activity) context;
        Intent intent = activity.getIntent();
        String action = intent.getAction();
        this.mIsCaptureIntent = "android.media.action.IMAGE_CAPTURE".equals(action) || CameraActivity.ACTION_IMAGE_CAPTURE_SECURE.equals(action) || "android.media.action.VIDEO_CAPTURE".equals(action);
    }

    @Deprecated
    public void setSwipeEnabled(boolean enabled) {
        this.mSwipeEnabled = enabled;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == 0) {
            this.mCheckToIntercept = true;
            this.mDown = MotionEvent.obtain(ev);
            this.mTouchReceiver = null;
            this.mRequestToInterceptTouchEvents = false;
            return false;
        }
        if (this.mRequestToInterceptTouchEvents) {
            this.mRequestToInterceptTouchEvents = false;
            onTouchEvent(this.mDown);
            return true;
        }
        if (ev.getActionMasked() == 5) {
            this.mCheckToIntercept = false;
            return false;
        }
        if (!this.mCheckToIntercept || ev.getEventTime() - ev.getDownTime() > 500 || this.mIsCaptureIntent || !this.mSwipeEnabled) {
            return false;
        }
        int deltaX = (int) (ev.getX() - this.mDown.getX());
        int deltaY = (int) (ev.getY() - this.mDown.getY());
        if (ev.getActionMasked() != 2 || Math.abs(deltaX) <= this.mSlop) {
            return false;
        }
        if (deltaX >= Math.abs(deltaY) * 2) {
            this.mTouchReceiver = this.mModeList;
            onTouchEvent(this.mDown);
            return true;
        }
        if (deltaX >= (-Math.abs(deltaY)) * 2) {
            return false;
        }
        this.mTouchReceiver = this.mFilmstripLayout;
        onTouchEvent(this.mDown);
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (this.mTouchReceiver == null) {
            return false;
        }
        this.mTouchReceiver.setVisibility(0);
        return this.mTouchReceiver.dispatchTouchEvent(ev);
    }

    @Override
    public void onFinishInflate() {
        this.mModeList = (ModeListView) findViewById(R.id.mode_list_layout);
        this.mFilmstripLayout = (FilmstripLayout) findViewById(R.id.filmstrip_layout);
    }

    public void redirectTouchEventsTo(View touchReceiver) {
        if (touchReceiver == null) {
            Log.e(this.TAG, "Cannot redirect touch to a null receiver.");
        } else {
            this.mTouchReceiver = touchReceiver;
            this.mRequestToInterceptTouchEvents = true;
        }
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.mNonDecorWindowSizeChangedListener != null) {
            this.mNonDecorWindowSizeChangedListener.onNonDecorWindowSizeChanged(View.MeasureSpec.getSize(widthMeasureSpec), View.MeasureSpec.getSize(heightMeasureSpec), CameraUtil.getDisplayRotation(getContext()));
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setNonDecorWindowSizeChangedListener(CameraAppUI.NonDecorWindowSizeChangedListener listener) {
        this.mNonDecorWindowSizeChangedListener = listener;
        if (this.mNonDecorWindowSizeChangedListener != null) {
            this.mNonDecorWindowSizeChangedListener.onNonDecorWindowSizeChanged(getMeasuredWidth(), getMeasuredHeight(), CameraUtil.getDisplayRotation(getContext()));
        }
    }
}
