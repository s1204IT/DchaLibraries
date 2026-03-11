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
import android.os.SystemClock;
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
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.view.FloatingActionMode;
import com.android.internal.widget.FloatingToolbar;
import com.android.systemui.R;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.statusbar.DragDownHelper;
import com.android.systemui.statusbar.phone.DoubleTapHelper;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class StatusBarWindowView extends FrameLayout {
    public static final boolean DEBUG = StatusBar.DEBUG;
    private View mBrightnessMirror;
    private DoubleTapHelper mDoubleTapHelper;
    private DragDownHelper mDragDownHelper;
    private boolean mExpandAnimationPending;
    private boolean mExpandAnimationRunning;
    private Window mFakeWindow;
    private FalsingManager mFalsingManager;
    private ActionMode mFloatingActionMode;
    private View mFloatingActionModeOriginatingView;
    private FloatingToolbar mFloatingToolbar;
    private ViewTreeObserver.OnPreDrawListener mFloatingToolbarPreDrawListener;
    private int mLeftInset;
    private NotificationPanelView mNotificationPanel;
    private int mRightInset;
    private StatusBar mService;
    private NotificationStackScrollLayout mStackScrollLayout;
    private boolean mTouchActive;
    private boolean mTouchCancelled;
    private final Paint mTransparentSrcPaint;

    public StatusBarWindowView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mRightInset = 0;
        this.mLeftInset = 0;
        this.mTransparentSrcPaint = new Paint();
        this.mFakeWindow = new Window(this.mContext) {
            @Override
            public void takeSurface(SurfaceHolder.Callback2 callback2) {
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
            public void setContentView(int i) {
            }

            @Override
            public void setContentView(View view) {
            }

            @Override
            public void setContentView(View view, ViewGroup.LayoutParams layoutParams) {
            }

            @Override
            public void addContentView(View view, ViewGroup.LayoutParams layoutParams) {
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
            public void setTitle(CharSequence charSequence) {
            }

            @Override
            public void setTitleColor(int i) {
            }

            @Override
            public void openPanel(int i, KeyEvent keyEvent) {
            }

            @Override
            public void closePanel(int i) {
            }

            @Override
            public void togglePanel(int i, KeyEvent keyEvent) {
            }

            @Override
            public void invalidatePanelMenu(int i) {
            }

            @Override
            public boolean performPanelShortcut(int i, int i2, KeyEvent keyEvent, int i3) {
                return false;
            }

            @Override
            public boolean performPanelIdentifierAction(int i, int i2, int i3) {
                return false;
            }

            @Override
            public void closeAllPanels() {
            }

            @Override
            public boolean performContextMenuIdentifierAction(int i, int i2) {
                return false;
            }

            @Override
            public void onConfigurationChanged(Configuration configuration) {
            }

            @Override
            public void setBackgroundDrawable(Drawable drawable) {
            }

            @Override
            public void setFeatureDrawableResource(int i, int i2) {
            }

            @Override
            public void setFeatureDrawableUri(int i, Uri uri) {
            }

            @Override
            public void setFeatureDrawable(int i, Drawable drawable) {
            }

            @Override
            public void setFeatureDrawableAlpha(int i, int i2) {
            }

            @Override
            public void setFeatureInt(int i, int i2) {
            }

            @Override
            public void takeKeyEvents(boolean z) {
            }

            @Override
            public boolean superDispatchKeyEvent(KeyEvent keyEvent) {
                return false;
            }

            @Override
            public boolean superDispatchKeyShortcutEvent(KeyEvent keyEvent) {
                return false;
            }

            @Override
            public boolean superDispatchTouchEvent(MotionEvent motionEvent) {
                return false;
            }

            @Override
            public boolean superDispatchTrackballEvent(MotionEvent motionEvent) {
                return false;
            }

            @Override
            public boolean superDispatchGenericMotionEvent(MotionEvent motionEvent) {
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
            public void restoreHierarchyState(Bundle bundle) {
            }

            @Override
            protected void onActive() {
            }

            @Override
            public void setChildDrawable(int i, Drawable drawable) {
            }

            @Override
            public void setChildInt(int i, int i2) {
            }

            @Override
            public boolean isShortcutKey(int i, KeyEvent keyEvent) {
                return false;
            }

            @Override
            public void setVolumeControlStream(int i) {
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
            public void setStatusBarColor(int i) {
            }

            @Override
            public int getNavigationBarColor() {
                return 0;
            }

            @Override
            public void setNavigationBarColor(int i) {
            }

            @Override
            public void setDecorCaptionShade(int i) {
            }

            @Override
            public void setResizingCaptionDrawable(Drawable drawable) {
            }

            public void onMultiWindowModeChanged() {
            }

            public void onPictureInPictureModeChanged(boolean z) {
            }

            public void reportActivityRelaunched() {
            }
        };
        setMotionEventSplittingEnabled(false);
        this.mTransparentSrcPaint.setColor(0);
        this.mTransparentSrcPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        this.mFalsingManager = FalsingManager.getInstance(context);
        this.mDoubleTapHelper = new DoubleTapHelper(this, new DoubleTapHelper.ActivationListener() {
            @Override
            public final void onActiveChanged(boolean z) {
                StatusBarWindowView.lambda$new$0(z);
            }
        }, new DoubleTapHelper.DoubleTapListener() {
            @Override
            public final boolean onDoubleTap() {
                return StatusBarWindowView.lambda$new$1(this.f$0);
            }
        }, null, null);
    }

    static void lambda$new$0(boolean z) {
    }

    public static boolean lambda$new$1(StatusBarWindowView statusBarWindowView) {
        statusBarWindowView.mService.wakeUpIfDozing(SystemClock.uptimeMillis(), statusBarWindowView);
        return true;
    }

    @Override
    protected boolean fitSystemWindows(Rect rect) {
        boolean z = true;
        if (getFitsSystemWindows()) {
            if (rect.top == getPaddingTop() && rect.bottom == getPaddingBottom()) {
                z = false;
            }
            if (rect.right != this.mRightInset || rect.left != this.mLeftInset) {
                this.mRightInset = rect.right;
                this.mLeftInset = rect.left;
                applyMargins();
            }
            if (z) {
                setPadding(0, 0, 0, 0);
            }
            rect.left = 0;
            rect.top = 0;
            rect.right = 0;
        } else {
            if (this.mRightInset != 0 || this.mLeftInset != 0) {
                this.mRightInset = 0;
                this.mLeftInset = 0;
                applyMargins();
            }
            if (getPaddingLeft() == 0 && getPaddingRight() == 0 && getPaddingTop() == 0 && getPaddingBottom() == 0) {
                z = false;
            }
            if (z) {
                setPadding(0, 0, 0, 0);
            }
            rect.top = 0;
        }
        return false;
    }

    private void applyMargins() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (childAt.getLayoutParams() instanceof LayoutParams) {
                LayoutParams layoutParams = (LayoutParams) childAt.getLayoutParams();
                if (!layoutParams.ignoreRightInset && (layoutParams.rightMargin != this.mRightInset || layoutParams.leftMargin != this.mLeftInset)) {
                    layoutParams.rightMargin = this.mRightInset;
                    layoutParams.leftMargin = this.mLeftInset;
                    childAt.requestLayout();
                }
            }
        }
    }

    @Override
    public FrameLayout.LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new LayoutParams(getContext(), attributeSet);
    }

    @Override
    public FrameLayout.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-1, -1);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mStackScrollLayout = (NotificationStackScrollLayout) findViewById(R.id.notification_stack_scroller);
        this.mNotificationPanel = (NotificationPanelView) findViewById(R.id.notification_panel);
        this.mBrightnessMirror = findViewById(R.id.brightness_mirror);
    }

    @Override
    public void onViewAdded(View view) {
        super.onViewAdded(view);
        if (view.getId() == R.id.brightness_mirror) {
            this.mBrightnessMirror = view;
        }
    }

    public void setService(StatusBar statusBar) {
        this.mService = statusBar;
        setDragDownHelper(new DragDownHelper(getContext(), this, this.mStackScrollLayout, this.mService));
    }

    @VisibleForTesting
    void setDragDownHelper(DragDownHelper dragDownHelper) {
        this.mDragDownHelper = dragDownHelper;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.mService.isScrimSrcModeEnabled()) {
            IBinder windowToken = getWindowToken();
            WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) getLayoutParams();
            layoutParams.token = windowToken;
            setLayoutParams(layoutParams);
            WindowManagerGlobal.getInstance().changeCanvasOpacity(windowToken, true);
            setWillNotDraw(false);
            return;
        }
        setWillNotDraw(!DEBUG);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        if (this.mService.interceptMediaKey(keyEvent) || super.dispatchKeyEvent(keyEvent)) {
            return true;
        }
        boolean z = keyEvent.getAction() == 0;
        int keyCode = keyEvent.getKeyCode();
        if (keyCode == 4) {
            if (!z) {
                this.mService.onBackPressed();
            }
            return true;
        }
        if (keyCode == 62) {
            if (!z) {
                return this.mService.onSpacePressed();
            }
        } else if (keyCode == 82) {
            if (!z) {
                return this.mService.onMenuPressed();
            }
            if (!z) {
            }
        } else {
            switch (keyCode) {
                case 24:
                case 25:
                    if (this.mService.isDozing()) {
                        MediaSessionLegacyHelper.getHelper(this.mContext).sendVolumeKeyEvent(keyEvent, Integer.MIN_VALUE, true);
                        return true;
                    }
                default:
                    return false;
            }
        }
        return false;
    }

    public void setTouchActive(boolean z) {
        this.mTouchActive = z;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        boolean z = motionEvent.getActionMasked() == 0;
        if (!(motionEvent.getActionMasked() == 3) && this.mService.shouldIgnoreTouch()) {
            return false;
        }
        if (z && this.mNotificationPanel.isFullyCollapsed()) {
            this.mNotificationPanel.startExpandLatencyTracking();
        }
        if (z) {
            setTouchActive(true);
            this.mTouchCancelled = false;
        } else if (motionEvent.getActionMasked() == 1 || motionEvent.getActionMasked() == 3) {
            setTouchActive(false);
        }
        if (this.mTouchCancelled || this.mExpandAnimationRunning || this.mExpandAnimationPending) {
            return false;
        }
        this.mFalsingManager.onTouchEvent(motionEvent, getWidth(), getHeight());
        if (this.mBrightnessMirror != null && this.mBrightnessMirror.getVisibility() == 0 && motionEvent.getActionMasked() == 5) {
            return false;
        }
        if (z) {
            this.mStackScrollLayout.closeControlsIfOutsideTouch(motionEvent);
        }
        if (this.mService.isDozing()) {
            this.mService.mDozeScrimController.extendPulse();
        }
        return super.dispatchTouchEvent(motionEvent);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (this.mService.isDozing() && !this.mStackScrollLayout.hasPulsingNotifications()) {
            return true;
        }
        boolean zOnInterceptTouchEvent = false;
        if (this.mNotificationPanel.isFullyExpanded() && this.mStackScrollLayout.getVisibility() == 0 && this.mService.getBarState() == 1 && !this.mService.isBouncerShowing() && !this.mService.isDozing()) {
            zOnInterceptTouchEvent = this.mDragDownHelper.onInterceptTouchEvent(motionEvent);
        }
        if (!zOnInterceptTouchEvent) {
            super.onInterceptTouchEvent(motionEvent);
        }
        if (zOnInterceptTouchEvent) {
            MotionEvent motionEventObtain = MotionEvent.obtain(motionEvent);
            motionEventObtain.setAction(3);
            this.mStackScrollLayout.onInterceptTouchEvent(motionEventObtain);
            this.mNotificationPanel.onInterceptTouchEvent(motionEventObtain);
            motionEventObtain.recycle();
        }
        return zOnInterceptTouchEvent;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        boolean zOnTouchEvent;
        if (this.mService.isDozing()) {
            this.mDoubleTapHelper.onTouchEvent(motionEvent);
            zOnTouchEvent = true;
        } else {
            zOnTouchEvent = false;
        }
        if ((this.mService.getBarState() == 1 && !zOnTouchEvent) || this.mDragDownHelper.isDraggingDown()) {
            zOnTouchEvent = this.mDragDownHelper.onTouchEvent(motionEvent);
        }
        if (!zOnTouchEvent) {
            zOnTouchEvent = super.onTouchEvent(motionEvent);
        }
        int action = motionEvent.getAction();
        if (!zOnTouchEvent && (action == 1 || action == 3)) {
            this.mService.setInteracting(1, false);
        }
        return zOnTouchEvent;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mService.isScrimSrcModeEnabled()) {
            int height = getHeight() - getPaddingBottom();
            int width = getWidth() - getPaddingRight();
            if (getPaddingTop() != 0) {
                canvas.drawRect(0.0f, 0.0f, getWidth(), getPaddingTop(), this.mTransparentSrcPaint);
            }
            if (getPaddingBottom() != 0) {
                canvas.drawRect(0.0f, height, getWidth(), getHeight(), this.mTransparentSrcPaint);
            }
            if (getPaddingLeft() != 0) {
                canvas.drawRect(0.0f, getPaddingTop(), getPaddingLeft(), height, this.mTransparentSrcPaint);
            }
            if (getPaddingRight() != 0) {
                canvas.drawRect(width, getPaddingTop(), getWidth(), height, this.mTransparentSrcPaint);
            }
        }
        if (DEBUG) {
            Paint paint = new Paint();
            paint.setColor(-2130706688);
            paint.setStrokeWidth(12.0f);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(0.0f, 0.0f, canvas.getWidth(), canvas.getHeight(), paint);
        }
    }

    public void cancelExpandHelper() {
        if (this.mStackScrollLayout != null) {
            this.mStackScrollLayout.cancelExpandHelper();
        }
    }

    public void cancelCurrentTouch() {
        if (this.mTouchActive) {
            long jUptimeMillis = SystemClock.uptimeMillis();
            MotionEvent motionEventObtain = MotionEvent.obtain(jUptimeMillis, jUptimeMillis, 3, 0.0f, 0.0f, 0);
            motionEventObtain.setSource(4098);
            dispatchTouchEvent(motionEventObtain);
            motionEventObtain.recycle();
            this.mTouchCancelled = true;
        }
    }

    public void setExpandAnimationRunning(boolean z) {
        this.mExpandAnimationRunning = z;
    }

    public void setExpandAnimationPending(boolean z) {
        this.mExpandAnimationPending = z;
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.print("  mExpandAnimationPending=");
        printWriter.println(this.mExpandAnimationPending);
        printWriter.print("  mExpandAnimationRunning=");
        printWriter.println(this.mExpandAnimationRunning);
        printWriter.print("  mTouchCancelled=");
        printWriter.println(this.mTouchCancelled);
        printWriter.print("  mTouchActive=");
        printWriter.println(this.mTouchActive);
    }

    public class LayoutParams extends FrameLayout.LayoutParams {
        public boolean ignoreRightInset;

        public LayoutParams(int i, int i2) {
            super(i, i2);
        }

        public LayoutParams(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.StatusBarWindowView_Layout);
            this.ignoreRightInset = typedArrayObtainStyledAttributes.getBoolean(0, false);
            typedArrayObtainStyledAttributes.recycle();
        }
    }

    @Override
    public ActionMode startActionModeForChild(View view, ActionMode.Callback callback, int i) {
        if (i == 1) {
            return startActionMode(view, callback, i);
        }
        return super.startActionModeForChild(view, callback, i);
    }

    private ActionMode createFloatingActionMode(View view, ActionMode.Callback2 callback2) {
        if (this.mFloatingActionMode != null) {
            this.mFloatingActionMode.finish();
        }
        cleanupFloatingActionModeViews();
        this.mFloatingToolbar = new FloatingToolbar(this.mFakeWindow);
        final FloatingActionMode floatingActionMode = new FloatingActionMode(this.mContext, callback2, view, this.mFloatingToolbar);
        this.mFloatingActionModeOriginatingView = view;
        this.mFloatingToolbarPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                floatingActionMode.updateViewLocationInWindow();
                return true;
            }
        };
        return floatingActionMode;
    }

    private void setHandledFloatingActionMode(ActionMode actionMode) {
        this.mFloatingActionMode = actionMode;
        this.mFloatingActionMode.invalidate();
        this.mFloatingActionModeOriginatingView.getViewTreeObserver().addOnPreDrawListener(this.mFloatingToolbarPreDrawListener);
    }

    public void cleanupFloatingActionModeViews() {
        if (this.mFloatingToolbar != null) {
            this.mFloatingToolbar.dismiss();
            this.mFloatingToolbar = null;
        }
        if (this.mFloatingActionModeOriginatingView != null) {
            if (this.mFloatingToolbarPreDrawListener != null) {
                this.mFloatingActionModeOriginatingView.getViewTreeObserver().removeOnPreDrawListener(this.mFloatingToolbarPreDrawListener);
                this.mFloatingToolbarPreDrawListener = null;
            }
            this.mFloatingActionModeOriginatingView = null;
        }
    }

    private ActionMode startActionMode(View view, ActionMode.Callback callback, int i) {
        ActionModeCallback2Wrapper actionModeCallback2Wrapper = new ActionModeCallback2Wrapper(callback);
        ActionMode actionModeCreateFloatingActionMode = createFloatingActionMode(view, actionModeCallback2Wrapper);
        if (actionModeCreateFloatingActionMode != null && actionModeCallback2Wrapper.onCreateActionMode(actionModeCreateFloatingActionMode, actionModeCreateFloatingActionMode.getMenu())) {
            setHandledFloatingActionMode(actionModeCreateFloatingActionMode);
            return actionModeCreateFloatingActionMode;
        }
        return null;
    }

    private class ActionModeCallback2Wrapper extends ActionMode.Callback2 {
        private final ActionMode.Callback mWrapped;

        public ActionModeCallback2Wrapper(ActionMode.Callback callback) {
            this.mWrapped = callback;
        }

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            return this.mWrapped.onCreateActionMode(actionMode, menu);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            StatusBarWindowView.this.requestFitSystemWindows();
            return this.mWrapped.onPrepareActionMode(actionMode, menu);
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            return this.mWrapped.onActionItemClicked(actionMode, menuItem);
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            this.mWrapped.onDestroyActionMode(actionMode);
            if (actionMode == StatusBarWindowView.this.mFloatingActionMode) {
                StatusBarWindowView.this.cleanupFloatingActionModeViews();
                StatusBarWindowView.this.mFloatingActionMode = null;
            }
            StatusBarWindowView.this.requestFitSystemWindows();
        }

        @Override
        public void onGetContentRect(ActionMode actionMode, View view, Rect rect) {
            if (this.mWrapped instanceof ActionMode.Callback2) {
                ((ActionMode.Callback2) this.mWrapped).onGetContentRect(actionMode, view, rect);
            } else {
                super.onGetContentRect(actionMode, view, rect);
            }
        }
    }
}
