package com.mediatek.camera.ui.shutter;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.animation.DecelerateInterpolator;
import android.widget.RelativeLayout;
import android.widget.Scroller;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.ui.shutter.ShutterView;

class ShutterRootLayout extends RelativeLayout implements ShutterView.OnShutterTextClicked {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ShutterRootLayout.class.getSimpleName());
    private int mCurrentIndex;
    private OnShutterChangeListener mListener;
    private boolean mResumed;
    private int mScrollDistance;
    private Scroller mScroller;

    public interface OnShutterChangeListener {
        void onShutterChangedEnd();

        void onShutterChangedStart(String str);
    }

    public void setOnShutterChangedListener(OnShutterChangeListener onShutterChangeListener) {
        this.mListener = onShutterChangeListener;
    }

    public IAppUiListener.OnGestureListener getGestureListener() {
        return new GestureListenerImpl();
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        return true;
    }

    @Override
    protected void onScrollChanged(int i, int i2, int i3, int i4) {
        super.onScrollChanged(i, i2, i3, i4);
        for (int i5 = 0; i5 < getChildCount(); i5++) {
            ((ShutterView) getChildAt(i5)).onScrolled(i, (getWidth() + 1) / 2, this.mScrollDistance);
        }
    }

    @Override
    public void computeScroll() {
        if (this.mScroller.computeScrollOffset()) {
            scrollTo(this.mScroller.getCurrX(), this.mScroller.getCurrY());
            postInvalidate();
            if (this.mScroller.isFinished() && this.mListener != null) {
                this.mListener.onShutterChangedEnd();
            }
        }
    }

    public ShutterRootLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mCurrentIndex = 0;
        this.mScrollDistance = 0;
        this.mResumed = false;
        this.mScroller = new Scroller(context, new DecelerateInterpolator());
    }

    @Override
    public void onShutterTextClicked(int i) {
        LogHelper.d(TAG, "onShutterTextClicked index = " + i);
        if (this.mScroller.isFinished() && isEnabled() && this.mResumed) {
            snapTOShutter(i, 1000);
        }
    }

    public void updateCurrentShutterIndex(int i) {
        doShutterAnimation(i, 0);
    }

    public void onResume() {
        this.mResumed = true;
    }

    public void onPause() {
        this.mResumed = false;
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        LogHelper.d(TAG, "onLayout() = " + z + "l = " + i + "  t = " + i2 + " r = " + i3 + " b = " + i4);
        super.onLayout(z, i, i2, i3, i4);
        updateCurrentShutterIndex(this.mCurrentIndex);
    }

    private void doShutterAnimation(int i, int i2) {
        int measuredWidth;
        this.mCurrentIndex = i;
        if (i > getChildCount() - 1) {
            this.mCurrentIndex = getChildCount() - 1;
        }
        if (this.mCurrentIndex == 0) {
            measuredWidth = -getScrollX();
        } else {
            measuredWidth = (((getChildAt(0).getMeasuredWidth() + getChildAt(1).getMeasuredWidth()) + 1) / 2) - getScrollX();
        }
        this.mScroller.startScroll(getScrollX(), 0, measuredWidth, 0, i2);
        this.mScrollDistance = Math.abs(measuredWidth);
        invalidate();
    }

    private void snapTOShutter(int i, int i2) {
        if (i == this.mCurrentIndex) {
            return;
        }
        doShutterAnimation(i, i2);
        if (this.mListener != null) {
            this.mListener.onShutterChangedStart(((ShutterView) getChildAt(this.mCurrentIndex)).getType());
        }
    }

    private class GestureListenerImpl implements IAppUiListener.OnGestureListener {
        private boolean mIsScale;
        private float mTransitionX;
        private float mTransitionY;

        private GestureListenerImpl() {
        }

        @Override
        public boolean onDown(MotionEvent motionEvent) {
            this.mTransitionX = 0.0f;
            this.mTransitionY = 0.0f;
            return false;
        }

        @Override
        public boolean onUp(MotionEvent motionEvent) {
            this.mTransitionX = 0.0f;
            this.mTransitionY = 0.0f;
            return false;
        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
            if (motionEvent2.getPointerCount() > 1 || ShutterRootLayout.this.getChildCount() < 2 || this.mIsScale) {
                return false;
            }
            if (!ShutterRootLayout.this.mScroller.isFinished() || !ShutterRootLayout.this.isEnabled() || !ShutterRootLayout.this.mResumed) {
                return true;
            }
            this.mTransitionX += f;
            this.mTransitionY += f2;
            Configuration configuration = ShutterRootLayout.this.getResources().getConfiguration();
            if (configuration.orientation == 1) {
                if (Math.abs(this.mTransitionX) > 100.0f && Math.abs(this.mTransitionY) < Math.abs(this.mTransitionX)) {
                    if (this.mTransitionX <= 0.0f || ShutterRootLayout.this.mCurrentIndex >= ShutterRootLayout.this.getChildCount() - 1) {
                        if (this.mTransitionX < 0.0f && ShutterRootLayout.this.mCurrentIndex > 0) {
                            if (ShutterRootLayout.this.getVisibility() != 0 || ShutterRootLayout.this.getChildAt(ShutterRootLayout.this.mCurrentIndex - 1).getVisibility() != 0) {
                                return false;
                            }
                            ShutterRootLayout.this.snapTOShutter(ShutterRootLayout.this.mCurrentIndex - 1, 1000);
                        }
                    } else {
                        if (ShutterRootLayout.this.getVisibility() != 0 || ShutterRootLayout.this.getChildAt(ShutterRootLayout.this.mCurrentIndex + 1).getVisibility() != 0) {
                            return false;
                        }
                        ShutterRootLayout.this.snapTOShutter(ShutterRootLayout.this.mCurrentIndex + 1, 1000);
                    }
                    return true;
                }
            } else if (configuration.orientation == 2 && Math.abs(this.mTransitionY) > 100.0f && Math.abs(this.mTransitionX) < Math.abs(this.mTransitionY)) {
                if (this.mTransitionY >= 0.0f || ShutterRootLayout.this.mCurrentIndex >= ShutterRootLayout.this.getChildCount() - 1) {
                    if (this.mTransitionY <= 0.0f || ShutterRootLayout.this.mCurrentIndex <= 0 || ShutterRootLayout.this.getChildAt(ShutterRootLayout.this.mCurrentIndex - 1).getVisibility() != 0) {
                        return false;
                    }
                    ShutterRootLayout.this.snapTOShutter(ShutterRootLayout.this.mCurrentIndex - 1, 1000);
                } else {
                    if (ShutterRootLayout.this.getChildAt(ShutterRootLayout.this.mCurrentIndex + 1).getVisibility() != 0) {
                        return false;
                    }
                    ShutterRootLayout.this.snapTOShutter(ShutterRootLayout.this.mCurrentIndex + 1, 1000);
                }
            }
            return false;
        }

        @Override
        public boolean onSingleTapUp(float f, float f2) {
            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(float f, float f2) {
            return false;
        }

        @Override
        public boolean onDoubleTap(float f, float f2) {
            return false;
        }

        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            this.mIsScale = true;
            return false;
        }

        @Override
        public boolean onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
            this.mIsScale = false;
            return false;
        }

        @Override
        public boolean onLongPress(float f, float f2) {
            return false;
        }
    }

    public IApp.KeyEventListener getKeyEventListener() {
        return new KeyEventListenerImpl();
    }

    private class KeyEventListenerImpl implements IApp.KeyEventListener {
        private KeyEventListenerImpl() {
        }

        @Override
        public boolean onKeyDown(int i, KeyEvent keyEvent) {
            if ((i != 29 && i != 30) || !CameraUtil.isSpecialKeyCodeEnabled()) {
                return false;
            }
            return true;
        }

        @Override
        public boolean onKeyUp(int i, KeyEvent keyEvent) {
            if (!CameraUtil.isSpecialKeyCodeEnabled()) {
                return false;
            }
            if (i != 29 && i != 30) {
                return false;
            }
            if (ShutterRootLayout.this.getChildCount() < 2) {
                LogHelper.w(ShutterRootLayout.TAG, "onKeyUp no need to slide betwwen photo mode and video mode,one mode olny");
                return false;
            }
            if (i == 29 && ShutterRootLayout.this.getChildCount() == 2 && ShutterRootLayout.this.getChildAt(0).getVisibility() == 0 && ShutterRootLayout.this.getChildAt(1).getVisibility() == 0) {
                ShutterRootLayout.this.onShutterTextClicked(0);
            } else if (i == 30 && ShutterRootLayout.this.getChildCount() == 2 && ShutterRootLayout.this.getChildAt(0).getVisibility() == 0 && ShutterRootLayout.this.getChildAt(1).getVisibility() == 0) {
                ShutterRootLayout.this.onShutterTextClicked(1);
            }
            return true;
        }
    }
}
