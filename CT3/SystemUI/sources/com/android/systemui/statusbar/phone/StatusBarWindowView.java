package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.session.MediaSessionLegacyHelper;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.InputQueue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.widget.FrameLayout;
import com.android.internal.view.FloatingActionMode;
import com.android.internal.widget.FloatingToolbar;
import com.android.systemui.R;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.statusbar.DragDownHelper;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;

public class StatusBarWindowView extends FrameLayout {
    private View mBrightnessMirror;
    private DragDownHelper mDragDownHelper;
    private Window mFakeWindow;
    private FalsingManager mFalsingManager;
    private ActionMode mFloatingActionMode;
    private View mFloatingActionModeOriginatingView;
    private FloatingToolbar mFloatingToolbar;
    private ViewTreeObserver.OnPreDrawListener mFloatingToolbarPreDrawListener;
    private NotificationPanelView mNotificationPanel;
    private int mRightInset;
    private PhoneStatusBar mService;
    private NotificationStackScrollLayout mStackScrollLayout;
    private final Paint mTransparentSrcPaint;

    public StatusBarWindowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mRightInset = 0;
        this.mTransparentSrcPaint = new Paint();
        this.mFakeWindow = new Window(this.mContext) {
            @Override
            public void takeSurface(SurfaceHolder.Callback2 callback) {
            }

            @Override
            public void takeInputQueue(InputQueue.Callback callback) {
            }

            @Override
            public boolean isFloating() {
                return false;
            }

            public void alwaysReadCloseOnTouchAttr() {
            }

            @Override
            public void setContentView(int layoutResID) {
            }

            @Override
            public void setContentView(View view) {
            }

            @Override
            public void setContentView(View view, ViewGroup.LayoutParams params) {
            }

            @Override
            public void addContentView(View view, ViewGroup.LayoutParams params) {
            }

            public void clearContentView() {
            }

            @Override
            public View getCurrentFocus() {
                return null;
            }

            @Override
            public LayoutInflater getLayoutInflater() {
                return null;
            }

            @Override
            public void setTitle(CharSequence title) {
            }

            @Override
            public void setTitleColor(int textColor) {
            }

            @Override
            public void openPanel(int featureId, KeyEvent event) {
            }

            @Override
            public void closePanel(int featureId) {
            }

            @Override
            public void togglePanel(int featureId, KeyEvent event) {
            }

            @Override
            public void invalidatePanelMenu(int featureId) {
            }

            @Override
            public boolean performPanelShortcut(int featureId, int keyCode, KeyEvent event, int flags) {
                return false;
            }

            @Override
            public boolean performPanelIdentifierAction(int featureId, int id, int flags) {
                return false;
            }

            @Override
            public void closeAllPanels() {
            }

            @Override
            public boolean performContextMenuIdentifierAction(int id, int flags) {
                return false;
            }

            @Override
            public void onConfigurationChanged(Configuration newConfig) {
            }

            @Override
            public void setBackgroundDrawable(Drawable drawable) {
            }

            @Override
            public void setFeatureDrawableResource(int featureId, int resId) {
            }

            @Override
            public void setFeatureDrawableUri(int featureId, Uri uri) {
            }

            @Override
            public void setFeatureDrawable(int featureId, Drawable drawable) {
            }

            @Override
            public void setFeatureDrawableAlpha(int featureId, int alpha) {
            }

            @Override
            public void setFeatureInt(int featureId, int value) {
            }

            @Override
            public void takeKeyEvents(boolean get) {
            }

            @Override
            public boolean superDispatchKeyEvent(KeyEvent event) {
                return false;
            }

            @Override
            public boolean superDispatchKeyShortcutEvent(KeyEvent event) {
                return false;
            }

            @Override
            public boolean superDispatchTouchEvent(MotionEvent event) {
                return false;
            }

            @Override
            public boolean superDispatchTrackballEvent(MotionEvent event) {
                return false;
            }

            @Override
            public boolean superDispatchGenericMotionEvent(MotionEvent event) {
                return false;
            }

            @Override
            public View getDecorView() {
                return StatusBarWindowView.this;
            }

            @Override
            public View peekDecorView() {
                return null;
            }

            @Override
            public Bundle saveHierarchyState() {
                return null;
            }

            @Override
            public void restoreHierarchyState(Bundle savedInstanceState) {
            }

            @Override
            protected void onActive() {
            }

            @Override
            public void setChildDrawable(int featureId, Drawable drawable) {
            }

            @Override
            public void setChildInt(int featureId, int value) {
            }

            @Override
            public boolean isShortcutKey(int keyCode, KeyEvent event) {
                return false;
            }

            @Override
            public void setVolumeControlStream(int streamType) {
            }

            @Override
            public int getVolumeControlStream() {
                return 0;
            }

            @Override
            public int getStatusBarColor() {
                return 0;
            }

            @Override
            public void setStatusBarColor(int color) {
            }

            @Override
            public int getNavigationBarColor() {
                return 0;
            }

            @Override
            public void setNavigationBarColor(int color) {
            }

            @Override
            public void setDecorCaptionShade(int decorCaptionShade) {
            }

            @Override
            public void setResizingCaptionDrawable(Drawable drawable) {
            }

            public void onMultiWindowModeChanged() {
            }

            public void reportActivityRelaunched() {
            }
        };
        setMotionEventSplittingEnabled(false);
        this.mTransparentSrcPaint.setColor(0);
        this.mTransparentSrcPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        this.mFalsingManager = FalsingManager.getInstance(context);
    }

    @Override
    protected boolean fitSystemWindows(Rect insets) {
        boolean changed = true;
        if (getFitsSystemWindows()) {
            boolean paddingChanged = (insets.left == getPaddingLeft() && insets.top == getPaddingTop() && insets.bottom == getPaddingBottom()) ? false : true;
            if (insets.right != this.mRightInset) {
                this.mRightInset = insets.right;
                applyMargins();
            }
            if (paddingChanged) {
                setPadding(insets.left, 0, 0, 0);
            }
            insets.left = 0;
            insets.top = 0;
            insets.right = 0;
        } else {
            if (this.mRightInset != 0) {
                this.mRightInset = 0;
                applyMargins();
            }
            if (getPaddingLeft() == 0 && getPaddingRight() == 0 && getPaddingTop() == 0 && getPaddingBottom() == 0) {
                changed = false;
            }
            if (changed) {
                setPadding(0, 0, 0, 0);
            }
            insets.top = 0;
        }
        return false;
    }

    private void applyMargins() {
        int N = getChildCount();
        for (int i = 0; i < N; i++) {
            View child = getChildAt(i);
            if (child.getLayoutParams() instanceof LayoutParams) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (!lp.ignoreRightInset && lp.rightMargin != this.mRightInset) {
                    lp.rightMargin = this.mRightInset;
                    child.requestLayout();
                }
            }
        }
    }

    @Override
    public FrameLayout.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected FrameLayout.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-1, -1);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mStackScrollLayout = (NotificationStackScrollLayout) findViewById(R.id.notification_stack_scroller);
        this.mNotificationPanel = (NotificationPanelView) findViewById(R.id.notification_panel);
        this.mBrightnessMirror = findViewById(R.id.brightness_mirror);
    }

    public void setService(PhoneStatusBar service) {
        this.mService = service;
        this.mDragDownHelper = new DragDownHelper(getContext(), this, this.mStackScrollLayout, this.mService);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.mService.isScrimSrcModeEnabled()) {
            IBinder windowToken = getWindowToken();
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
            lp.token = windowToken;
            setLayoutParams(lp);
            WindowManagerGlobal.getInstance().changeCanvasOpacity(windowToken, true);
            setWillNotDraw(false);
            return;
        }
        setWillNotDraw(true);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean down = event.getAction() == 0;
        switch (event.getKeyCode()) {
            case 4:
                if (!down) {
                    this.mService.onBackPressed();
                }
                break;
            case 24:
            case 25:
                if (this.mService.isDozing()) {
                    MediaSessionLegacyHelper.getHelper(this.mContext).sendVolumeKeyEvent(event, true);
                }
                if (this.mService.interceptMediaKey(event)) {
                    break;
                }
                break;
            case 62:
                if (!down) {
                }
                if (this.mService.interceptMediaKey(event)) {
                }
                break;
            case 82:
                if (!down) {
                }
                if (!down) {
                }
                if (this.mService.interceptMediaKey(event)) {
                }
                break;
            default:
                if (this.mService.interceptMediaKey(event)) {
                }
                break;
        }
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        this.mFalsingManager.onTouchEvent(ev, getWidth(), getHeight());
        if (this.mBrightnessMirror != null && this.mBrightnessMirror.getVisibility() == 0 && ev.getActionMasked() == 5) {
            return false;
        }
        if (ev.getActionMasked() == 0) {
            this.mStackScrollLayout.closeControlsIfOutsideTouch(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean intercept = false;
        if (this.mNotificationPanel.isFullyExpanded() && this.mStackScrollLayout.getVisibility() == 0 && this.mService.getBarState() == 1 && !this.mService.isBouncerShowing()) {
            intercept = this.mDragDownHelper.onInterceptTouchEvent(ev);
            if (ev.getActionMasked() == 0) {
                this.mService.wakeUpIfDozing(ev.getEventTime(), ev);
            }
        }
        if (!intercept) {
            super.onInterceptTouchEvent(ev);
        }
        if (intercept) {
            MotionEvent cancellation = MotionEvent.obtain(ev);
            cancellation.setAction(3);
            this.mStackScrollLayout.onInterceptTouchEvent(cancellation);
            this.mNotificationPanel.onInterceptTouchEvent(cancellation);
            cancellation.recycle();
        }
        return intercept;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean handled = false;
        if (this.mService.getBarState() == 1) {
            handled = this.mDragDownHelper.onTouchEvent(ev);
        }
        if (!handled) {
            handled = super.onTouchEvent(ev);
        }
        int action = ev.getAction();
        if (!handled && (action == 1 || action == 3)) {
            this.mService.setInteracting(1, false);
        }
        return handled;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!this.mService.isScrimSrcModeEnabled()) {
            return;
        }
        int paddedBottom = getHeight() - getPaddingBottom();
        int paddedRight = getWidth() - getPaddingRight();
        if (getPaddingTop() != 0) {
            canvas.drawRect(0.0f, 0.0f, getWidth(), getPaddingTop(), this.mTransparentSrcPaint);
        }
        if (getPaddingBottom() != 0) {
            canvas.drawRect(0.0f, paddedBottom, getWidth(), getHeight(), this.mTransparentSrcPaint);
        }
        if (getPaddingLeft() != 0) {
            canvas.drawRect(0.0f, getPaddingTop(), getPaddingLeft(), paddedBottom, this.mTransparentSrcPaint);
        }
        if (getPaddingRight() == 0) {
            return;
        }
        canvas.drawRect(paddedRight, getPaddingTop(), getWidth(), paddedBottom, this.mTransparentSrcPaint);
    }

    public void cancelExpandHelper() {
        if (this.mStackScrollLayout == null) {
            return;
        }
        this.mStackScrollLayout.cancelExpandHelper();
    }

    public class LayoutParams extends FrameLayout.LayoutParams {
        public boolean ignoreRightInset;

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.StatusBarWindowView_Layout);
            this.ignoreRightInset = a.getBoolean(0, false);
            a.recycle();
        }
    }

    @Override
    public ActionMode startActionModeForChild(View originalView, ActionMode.Callback callback, int type) {
        if (type == 1) {
            return startActionMode(originalView, callback, type);
        }
        return super.startActionModeForChild(originalView, callback, type);
    }

    private ActionMode createFloatingActionMode(View originatingView, ActionMode.Callback2 callback) {
        if (this.mFloatingActionMode != null) {
            this.mFloatingActionMode.finish();
        }
        cleanupFloatingActionModeViews();
        final FloatingActionMode mode = new FloatingActionMode(this.mContext, callback, originatingView);
        this.mFloatingActionModeOriginatingView = originatingView;
        this.mFloatingToolbarPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mode.updateViewLocationInWindow();
                return true;
            }
        };
        return mode;
    }

    private void setHandledFloatingActionMode(ActionMode mode) {
        this.mFloatingActionMode = mode;
        this.mFloatingToolbar = new FloatingToolbar(this.mContext, this.mFakeWindow);
        this.mFloatingActionMode.setFloatingToolbar(this.mFloatingToolbar);
        this.mFloatingActionMode.invalidate();
        this.mFloatingActionModeOriginatingView.getViewTreeObserver().addOnPreDrawListener(this.mFloatingToolbarPreDrawListener);
    }

    public void cleanupFloatingActionModeViews() {
        if (this.mFloatingToolbar != null) {
            this.mFloatingToolbar.dismiss();
            this.mFloatingToolbar = null;
        }
        if (this.mFloatingActionModeOriginatingView == null) {
            return;
        }
        if (this.mFloatingToolbarPreDrawListener != null) {
            this.mFloatingActionModeOriginatingView.getViewTreeObserver().removeOnPreDrawListener(this.mFloatingToolbarPreDrawListener);
            this.mFloatingToolbarPreDrawListener = null;
        }
        this.mFloatingActionModeOriginatingView = null;
    }

    private ActionMode startActionMode(View originatingView, ActionMode.Callback callback, int type) {
        ActionMode.Callback2 wrappedCallback = new ActionModeCallback2Wrapper(callback);
        ActionMode mode = createFloatingActionMode(originatingView, wrappedCallback);
        if (mode != null && wrappedCallback.onCreateActionMode(mode, mode.getMenu())) {
            setHandledFloatingActionMode(mode);
            return mode;
        }
        return null;
    }

    private class ActionModeCallback2Wrapper extends ActionMode.Callback2 {
        private final ActionMode.Callback mWrapped;

        public ActionModeCallback2Wrapper(ActionMode.Callback wrapped) {
            this.mWrapped = wrapped;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return this.mWrapped.onCreateActionMode(mode, menu);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            StatusBarWindowView.this.requestFitSystemWindows();
            return this.mWrapped.onPrepareActionMode(mode, menu);
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return this.mWrapped.onActionItemClicked(mode, item);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            this.mWrapped.onDestroyActionMode(mode);
            if (mode == StatusBarWindowView.this.mFloatingActionMode) {
                StatusBarWindowView.this.cleanupFloatingActionModeViews();
                StatusBarWindowView.this.mFloatingActionMode = null;
            }
            StatusBarWindowView.this.requestFitSystemWindows();
        }

        @Override
        public void onGetContentRect(ActionMode mode, View view, Rect outRect) {
            if (this.mWrapped instanceof ActionMode.Callback2) {
                ((ActionMode.Callback2) this.mWrapped).onGetContentRect(mode, view, outRect);
            } else {
                super.onGetContentRect(mode, view, outRect);
            }
        }
    }
}
