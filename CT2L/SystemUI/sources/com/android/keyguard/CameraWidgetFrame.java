package com.android.keyguard;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.android.keyguard.KeyguardActivityLauncher;

public class CameraWidgetFrame extends KeyguardWidgetFrame implements View.OnClickListener {
    private static final String TAG = CameraWidgetFrame.class.getSimpleName();
    private boolean mActive;
    private final KeyguardActivityLauncher mActivityLauncher;
    private final KeyguardUpdateMonitorCallback mCallback;
    private final Callbacks mCallbacks;
    private boolean mDown;
    private View mFakeNavBar;
    private View mFullscreenPreview;
    private final Handler mHandler;
    private final Rect mInsets;
    private long mLaunchCameraStart;
    private final Runnable mPostTransitionToCameraEndAction;
    private FixedSizeFrameLayout mPreview;
    private final Runnable mRecoverRunnable;
    private final Runnable mRenderRunnable;
    private final Point mRenderedSize;
    private final Runnable mSecureCameraActivityStartedRunnable;
    private final int[] mTmpLoc;
    private final Runnable mTransitionToCameraEndAction;
    private final Runnable mTransitionToCameraRunnable;
    private boolean mTransitioning;
    private boolean mUseFastTransition;
    private final KeyguardActivityLauncher.CameraWidgetInfo mWidgetInfo;
    private final WindowManager mWindowManager;

    interface Callbacks {
        void onCameraLaunchedSuccessfully();

        void onCameraLaunchedUnsuccessfully();

        void onLaunchingCamera();
    }

    private static final class FixedSizeFrameLayout extends FrameLayout {
        int height;
        int width;

        FixedSizeFrameLayout(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            measureChildren(View.MeasureSpec.makeMeasureSpec(this.width, 1073741824), View.MeasureSpec.makeMeasureSpec(this.height, 1073741824));
            setMeasuredDimension(this.width, this.height);
        }
    }

    private CameraWidgetFrame(Context context, Callbacks callbacks, KeyguardActivityLauncher activityLauncher, KeyguardActivityLauncher.CameraWidgetInfo widgetInfo, View previewWidget) {
        super(context);
        this.mHandler = new Handler();
        this.mRenderedSize = new Point();
        this.mTmpLoc = new int[2];
        this.mInsets = new Rect();
        this.mTransitionToCameraRunnable = new Runnable() {
            @Override
            public void run() {
                CameraWidgetFrame.this.transitionToCamera();
            }
        };
        this.mTransitionToCameraEndAction = new Runnable() {
            @Override
            public void run() {
                if (CameraWidgetFrame.this.mTransitioning) {
                    Handler worker = CameraWidgetFrame.this.getWorkerHandler() != null ? CameraWidgetFrame.this.getWorkerHandler() : CameraWidgetFrame.this.mHandler;
                    CameraWidgetFrame.this.mLaunchCameraStart = SystemClock.uptimeMillis();
                    CameraWidgetFrame.this.mActivityLauncher.launchCamera(worker, CameraWidgetFrame.this.mSecureCameraActivityStartedRunnable);
                }
            }
        };
        this.mPostTransitionToCameraEndAction = new Runnable() {
            @Override
            public void run() {
                CameraWidgetFrame.this.mHandler.post(CameraWidgetFrame.this.mTransitionToCameraEndAction);
            }
        };
        this.mRecoverRunnable = new Runnable() {
            @Override
            public void run() {
                CameraWidgetFrame.this.recover();
            }
        };
        this.mRenderRunnable = new Runnable() {
            @Override
            public void run() {
                CameraWidgetFrame.this.render();
            }
        };
        this.mSecureCameraActivityStartedRunnable = new Runnable() {
            @Override
            public void run() {
                CameraWidgetFrame.this.onSecureCameraActivityStarted();
            }
        };
        this.mCallback = new KeyguardUpdateMonitorCallback() {
            private boolean mShowing;

            @Override
            public void onKeyguardVisibilityChanged(boolean showing) {
                if (this.mShowing != showing) {
                    this.mShowing = showing;
                    CameraWidgetFrame.this.onKeyguardVisibilityChanged(this.mShowing);
                }
            }
        };
        this.mCallbacks = callbacks;
        this.mActivityLauncher = activityLauncher;
        this.mWidgetInfo = widgetInfo;
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        KeyguardUpdateMonitor.getInstance(context).registerCallback(this.mCallback);
        this.mPreview = new FixedSizeFrameLayout(context);
        this.mPreview.addView(previewWidget);
        addView(this.mPreview);
        View clickBlocker = new View(context);
        clickBlocker.setBackgroundColor(0);
        clickBlocker.setOnClickListener(this);
        addView(clickBlocker);
        setContentDescription(context.getString(R.string.keyguard_accessibility_camera));
    }

    public static CameraWidgetFrame create(Context context, Callbacks callbacks, KeyguardActivityLauncher launcher) {
        KeyguardActivityLauncher.CameraWidgetInfo widgetInfo;
        View previewWidget;
        if (context == null || callbacks == null || launcher == null || (widgetInfo = launcher.getCameraWidgetInfo()) == null || (previewWidget = getPreviewWidget(context, widgetInfo)) == null) {
            return null;
        }
        return new CameraWidgetFrame(context, callbacks, launcher, widgetInfo, previewWidget);
    }

    private static View getPreviewWidget(Context context, KeyguardActivityLauncher.CameraWidgetInfo widgetInfo) {
        return widgetInfo.layoutId > 0 ? inflateWidgetView(context, widgetInfo) : inflateGenericWidgetView(context);
    }

    private static View inflateWidgetView(Context context, KeyguardActivityLauncher.CameraWidgetInfo widgetInfo) {
        View widgetView = null;
        Exception exception = null;
        try {
            Context cameraContext = context.createPackageContext(widgetInfo.contextPackage, 4);
            LayoutInflater cameraInflater = (LayoutInflater) cameraContext.getSystemService("layout_inflater");
            widgetView = cameraInflater.cloneInContext(cameraContext).inflate(widgetInfo.layoutId, (ViewGroup) null, false);
        } catch (PackageManager.NameNotFoundException e) {
            exception = e;
        } catch (RuntimeException e2) {
            exception = e2;
        }
        if (exception != null) {
            Log.w(TAG, "Error creating camera widget view", exception);
        }
        return widgetView;
    }

    private static View inflateGenericWidgetView(Context context) {
        ImageView iv = new ImageView(context);
        iv.setImageResource(R.drawable.ic_lockscreen_camera);
        iv.setScaleType(ImageView.ScaleType.CENTER);
        iv.setBackgroundColor(Color.argb(127, 0, 0, 0));
        return iv;
    }

    private void render() {
        View root = getRootView();
        int width = root.getWidth() - this.mInsets.right;
        int height = root.getHeight() - this.mInsets.bottom;
        if ((this.mRenderedSize.x != width || this.mRenderedSize.y != height) && width != 0 && height != 0) {
            this.mPreview.width = width;
            this.mPreview.height = height;
            this.mPreview.requestLayout();
            int thisWidth = (getWidth() - getPaddingLeft()) - getPaddingRight();
            int thisHeight = (getHeight() - getPaddingTop()) - getPaddingBottom();
            float pvScaleX = thisWidth / width;
            float pvScaleY = thisHeight / height;
            float pvScale = Math.min(pvScaleX, pvScaleY);
            int pvWidth = (int) (width * pvScale);
            int pvHeight = (int) (height * pvScale);
            float pvTransX = pvWidth < thisWidth ? (thisWidth - pvWidth) / 2 : 0.0f;
            float pvTransY = pvHeight < thisHeight ? (thisHeight - pvHeight) / 2 : 0.0f;
            boolean isRtl = this.mPreview.getLayoutDirection() == 1;
            this.mPreview.setPivotX(isRtl ? this.mPreview.width : 0.0f);
            this.mPreview.setPivotY(0.0f);
            this.mPreview.setScaleX(pvScale);
            this.mPreview.setScaleY(pvScale);
            this.mPreview.setTranslationX((isRtl ? -1 : 1) * pvTransX);
            this.mPreview.setTranslationY(pvTransY);
            this.mRenderedSize.set(width, height);
        }
    }

    private void transitionToCamera() {
        if (!this.mTransitioning && !this.mDown) {
            this.mTransitioning = true;
            enableWindowExitAnimation(false);
            int navHeight = this.mInsets.bottom;
            int navWidth = this.mInsets.right;
            this.mPreview.getLocationInWindow(this.mTmpLoc);
            float pvHeight = this.mPreview.getHeight() * this.mPreview.getScaleY();
            float pvCenter = this.mTmpLoc[1] + (pvHeight / 2.0f);
            ViewGroup root = (ViewGroup) getRootView();
            if (this.mFullscreenPreview == null) {
                this.mFullscreenPreview = getPreviewWidget(this.mContext, this.mWidgetInfo);
                this.mFullscreenPreview.setClickable(false);
                root.addView(this.mFullscreenPreview, new FrameLayout.LayoutParams(root.getWidth() - navWidth, root.getHeight() - navHeight));
            }
            float fsHeight = root.getHeight() - navHeight;
            float fsCenter = root.getTop() + (fsHeight / 2.0f);
            float fsScaleY = this.mPreview.getScaleY();
            float fsTransY = pvCenter - fsCenter;
            this.mPreview.setVisibility(8);
            this.mFullscreenPreview.setVisibility(0);
            this.mFullscreenPreview.setTranslationY(fsTransY);
            this.mFullscreenPreview.setScaleX(fsScaleY);
            this.mFullscreenPreview.setScaleY(fsScaleY);
            this.mFullscreenPreview.animate().scaleX(1.0f).scaleY(1.0f).translationX(0.0f).translationY(0.0f).setDuration(250L).withEndAction(this.mPostTransitionToCameraEndAction).start();
            if (navHeight > 0 || navWidth > 0) {
                boolean atBottom = navHeight > 0;
                if (this.mFakeNavBar == null) {
                    this.mFakeNavBar = new View(this.mContext);
                    this.mFakeNavBar.setBackgroundColor(-16777216);
                    root.addView(this.mFakeNavBar, new FrameLayout.LayoutParams(atBottom ? -1 : navWidth, atBottom ? navHeight : -1, atBottom ? 87 : 117));
                    this.mFakeNavBar.setPivotY(navHeight);
                    this.mFakeNavBar.setPivotX(navWidth);
                }
                this.mFakeNavBar.setAlpha(0.0f);
                if (atBottom) {
                    this.mFakeNavBar.setScaleY(0.5f);
                } else {
                    this.mFakeNavBar.setScaleX(0.5f);
                }
                this.mFakeNavBar.setVisibility(0);
                this.mFakeNavBar.animate().alpha(1.0f).scaleY(1.0f).scaleY(1.0f).setDuration(250L).start();
            }
            this.mCallbacks.onLaunchingCamera();
        }
    }

    private void recover() {
        this.mCallbacks.onCameraLaunchedUnsuccessfully();
        reset();
    }

    @Override
    public void setOnLongClickListener(View.OnLongClickListener l) {
    }

    @Override
    public void onClick(View v) {
        if (!this.mTransitioning && this.mActive) {
            cancelTransitionToCamera();
            transitionToCamera();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(this.mContext).removeCallback(this.mCallback);
        cancelTransitionToCamera();
        this.mHandler.removeCallbacks(this.mRecoverRunnable);
    }

    @Override
    public void onActive(boolean isActive) {
        this.mActive = isActive;
        if (this.mActive) {
            rescheduleTransitionToCamera();
        } else {
            reset();
        }
    }

    @Override
    public boolean onUserInteraction(MotionEvent event) {
        boolean z = true;
        if (this.mTransitioning) {
            return true;
        }
        getLocationOnScreen(this.mTmpLoc);
        int rawBottom = this.mTmpLoc[1] + getHeight();
        if (event.getRawY() > rawBottom) {
            return true;
        }
        int action = event.getAction();
        if (action != 0 && action != 2) {
            z = false;
        }
        this.mDown = z;
        if (this.mActive) {
            rescheduleTransitionToCamera();
        }
        return false;
    }

    protected void onFocusLost() {
        cancelTransitionToCamera();
        super.onFocusLost();
    }

    public void onScreenTurnedOff() {
        reset();
    }

    private void rescheduleTransitionToCamera() {
        this.mHandler.removeCallbacks(this.mTransitionToCameraRunnable);
        long duration = this.mUseFastTransition ? 0L : 400L;
        this.mHandler.postDelayed(this.mTransitionToCameraRunnable, duration);
    }

    private void cancelTransitionToCamera() {
        this.mHandler.removeCallbacks(this.mTransitionToCameraRunnable);
    }

    private void onCameraLaunched() {
        this.mCallbacks.onCameraLaunchedSuccessfully();
        reset();
    }

    private void reset() {
        this.mLaunchCameraStart = 0L;
        this.mTransitioning = false;
        this.mDown = false;
        cancelTransitionToCamera();
        this.mHandler.removeCallbacks(this.mRecoverRunnable);
        this.mPreview.setVisibility(0);
        if (this.mFullscreenPreview != null) {
            this.mFullscreenPreview.animate().cancel();
            this.mFullscreenPreview.setVisibility(8);
        }
        if (this.mFakeNavBar != null) {
            this.mFakeNavBar.animate().cancel();
            this.mFakeNavBar.setVisibility(8);
        }
        enableWindowExitAnimation(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if ((w != oldw && oldw > 0) || (h != oldh && oldh > 0)) {
            Point point = this.mRenderedSize;
            this.mRenderedSize.y = -1;
            point.x = -1;
        }
        this.mHandler.post(this.mRenderRunnable);
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    public void onBouncerShowing(boolean showing) {
        if (showing) {
            this.mTransitioning = false;
            this.mHandler.post(this.mRecoverRunnable);
        }
    }

    private void enableWindowExitAnimation(boolean isEnabled) {
        View root = getRootView();
        ViewGroup.LayoutParams lp = root.getLayoutParams();
        if (lp instanceof WindowManager.LayoutParams) {
            WindowManager.LayoutParams wlp = (WindowManager.LayoutParams) lp;
            int newWindowAnimations = isEnabled ? R.style.Animation_LockScreen : 0;
            if (newWindowAnimations != wlp.windowAnimations) {
                wlp.windowAnimations = newWindowAnimations;
                this.mWindowManager.updateViewLayout(root, wlp);
            }
        }
    }

    private void onKeyguardVisibilityChanged(boolean showing) {
        if (this.mTransitioning && !showing) {
            this.mTransitioning = false;
            this.mHandler.removeCallbacks(this.mRecoverRunnable);
            if (this.mLaunchCameraStart > 0) {
                long jUptimeMillis = SystemClock.uptimeMillis() - this.mLaunchCameraStart;
                this.mLaunchCameraStart = 0L;
                onCameraLaunched();
            }
        }
    }

    private void onSecureCameraActivityStarted() {
        this.mHandler.postDelayed(this.mRecoverRunnable, 1000L);
    }

    public void setInsets(Rect insets) {
        this.mInsets.set(insets);
    }

    public void setUseFastTransition(boolean useFastTransition) {
        this.mUseFastTransition = useFastTransition;
    }
}
