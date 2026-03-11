package com.android.launcher3.widget;

import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.AdapterView;
import android.widget.Advanceable;
import android.widget.RemoteViews;
import com.android.launcher3.CheckLongPressHelper;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppWidgetInfo;
import com.android.launcher3.LauncherAppWidgetProviderInfo;
import com.android.launcher3.R;
import com.android.launcher3.SimpleOnStylusPressListener;
import com.android.launcher3.StylusEventHelper;
import com.android.launcher3.Utilities;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.views.BaseDragLayer;
import java.util.ArrayList;

public class LauncherAppWidgetHostView extends AppWidgetHostView implements BaseDragLayer.TouchCompleteListener, View.OnLongClickListener {
    private static final long ADVANCE_INTERVAL = 20000;
    private static final long ADVANCE_STAGGER = 250;
    private static final SparseBooleanArray sAutoAdvanceWidgetIds = new SparseBooleanArray();
    private Runnable mAutoAdvanceRunnable;

    @ViewDebug.ExportedProperty(category = "launcher")
    private boolean mChildrenFocused;
    protected final LayoutInflater mInflater;
    private boolean mIsAttachedToWindow;
    private boolean mIsAutoAdvanceRegistered;
    private boolean mIsScrollable;
    protected final Launcher mLauncher;
    private final CheckLongPressHelper mLongPressHelper;

    @ViewDebug.ExportedProperty(category = "launcher")
    private boolean mReinflateOnConfigChange;
    private float mScaleToFit;
    private float mSlop;
    private final StylusEventHelper mStylusEventHelper;
    private final PointF mTranslationForCentering;

    public LauncherAppWidgetHostView(Context context) {
        super(context);
        this.mScaleToFit = 1.0f;
        this.mTranslationForCentering = new PointF(0.0f, 0.0f);
        this.mLauncher = Launcher.getLauncher(context);
        this.mLongPressHelper = new CheckLongPressHelper(this, this);
        this.mStylusEventHelper = new StylusEventHelper(new SimpleOnStylusPressListener(this), this);
        this.mInflater = LayoutInflater.from(context);
        setAccessibilityDelegate(this.mLauncher.getAccessibilityDelegate());
        setBackgroundResource(R.drawable.widget_internal_focus_bg);
        if (Utilities.ATLEAST_OREO) {
            setExecutor(Utilities.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if (this.mIsScrollable) {
            Launcher.getLauncher(getContext()).getDragLayer().requestDisallowInterceptTouchEvent(false);
        }
        view.performLongClick();
        return true;
    }

    @Override
    protected View getErrorView() {
        return this.mInflater.inflate(R.layout.appwidget_error, (ViewGroup) this, false);
    }

    @Override
    public void updateAppWidget(RemoteViews remoteViews) {
        super.updateAppWidget(remoteViews);
        checkIfAutoAdvance();
        this.mReinflateOnConfigChange = !isSameOrientation();
    }

    private boolean isSameOrientation() {
        return this.mLauncher.getResources().getConfiguration().orientation == this.mLauncher.getOrientation();
    }

    private boolean checkScrollableRecursively(ViewGroup viewGroup) {
        if (viewGroup instanceof AdapterView) {
            return true;
        }
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View childAt = viewGroup.getChildAt(i);
            if ((childAt instanceof ViewGroup) && checkScrollableRecursively((ViewGroup) childAt)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == 0) {
            this.mLongPressHelper.cancelLongPress();
        }
        if (this.mLongPressHelper.hasPerformedLongPress()) {
            this.mLongPressHelper.cancelLongPress();
            return true;
        }
        if (this.mStylusEventHelper.onMotionEvent(motionEvent)) {
            this.mLongPressHelper.cancelLongPress();
            return true;
        }
        switch (motionEvent.getAction()) {
            case 0:
                DragLayer dragLayer = Launcher.getLauncher(getContext()).getDragLayer();
                if (this.mIsScrollable) {
                    dragLayer.requestDisallowInterceptTouchEvent(true);
                }
                if (!this.mStylusEventHelper.inStylusButtonPressed()) {
                    this.mLongPressHelper.postCheckForLongPress();
                }
                dragLayer.setTouchCompleteListener(this);
                break;
            case 1:
            case 3:
                this.mLongPressHelper.cancelLongPress();
                break;
            case 2:
                if (!Utilities.pointInView(this, motionEvent.getX(), motionEvent.getY(), this.mSlop)) {
                    this.mLongPressHelper.cancelLongPress();
                }
                break;
        }
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case 1:
            case 3:
                this.mLongPressHelper.cancelLongPress();
                break;
            case 2:
                if (!Utilities.pointInView(this, motionEvent.getX(), motionEvent.getY(), this.mSlop)) {
                    this.mLongPressHelper.cancelLongPress();
                }
                break;
        }
        return false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        this.mIsAttachedToWindow = true;
        checkIfAutoAdvance();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mIsAttachedToWindow = false;
        checkIfAutoAdvance();
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        this.mLongPressHelper.cancelLongPress();
    }

    @Override
    public AppWidgetProviderInfo getAppWidgetInfo() {
        AppWidgetProviderInfo appWidgetInfo = super.getAppWidgetInfo();
        if (appWidgetInfo != null && !(appWidgetInfo instanceof LauncherAppWidgetProviderInfo)) {
            throw new IllegalStateException("Launcher widget must have LauncherAppWidgetProviderInfo");
        }
        return appWidgetInfo;
    }

    @Override
    public void onTouchComplete() {
        if (!this.mLongPressHelper.hasPerformedLongPress()) {
            this.mLongPressHelper.cancelLongPress();
        }
    }

    @Override
    public int getDescendantFocusability() {
        return this.mChildrenFocused ? 131072 : 393216;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        if (this.mChildrenFocused && keyEvent.getKeyCode() == 111 && keyEvent.getAction() == 1) {
            this.mChildrenFocused = false;
            requestFocus();
            return true;
        }
        return super.dispatchKeyEvent(keyEvent);
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (!this.mChildrenFocused && i == 66) {
            keyEvent.startTracking();
            return true;
        }
        return super.onKeyDown(i, keyEvent);
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        if (keyEvent.isTracking() && !this.mChildrenFocused && i == 66) {
            this.mChildrenFocused = true;
            ArrayList focusables = getFocusables(2);
            focusables.remove(this);
            switch (focusables.size()) {
                case 0:
                    this.mChildrenFocused = false;
                    break;
                case 1:
                    if (getTag() instanceof ItemInfo) {
                        ItemInfo itemInfo = (ItemInfo) getTag();
                        if (itemInfo.spanX == 1 && itemInfo.spanY == 1) {
                            ((View) focusables.get(0)).performClick();
                            this.mChildrenFocused = false;
                        }
                        break;
                    }
                default:
                    ((View) focusables.get(0)).requestFocus();
                    break;
            }
            return true;
        }
        return super.onKeyUp(i, keyEvent);
    }

    @Override
    protected void onFocusChanged(boolean z, int i, Rect rect) {
        if (z) {
            this.mChildrenFocused = false;
            dispatchChildFocus(false);
        }
        super.onFocusChanged(z, i, rect);
    }

    @Override
    public void requestChildFocus(View view, View view2) {
        super.requestChildFocus(view, view2);
        dispatchChildFocus(this.mChildrenFocused && view2 != null);
        if (view2 != null) {
            view2.setFocusableInTouchMode(false);
        }
    }

    @Override
    public void clearChildFocus(View view) {
        super.clearChildFocus(view);
        dispatchChildFocus(false);
    }

    @Override
    public boolean dispatchUnhandledMove(View view, int i) {
        return this.mChildrenFocused;
    }

    private void dispatchChildFocus(boolean z) {
        setSelected(z);
    }

    public void switchToErrorView() {
        updateAppWidget(new RemoteViews(getAppWidgetInfo().provider.getPackageName(), 0));
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        try {
            super.onLayout(z, i, i2, i3, i4);
        } catch (RuntimeException e) {
            post(new Runnable() {
                @Override
                public void run() {
                    LauncherAppWidgetHostView.this.switchToErrorView();
                }
            });
        }
        this.mIsScrollable = checkScrollableRecursively(this);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        accessibilityNodeInfo.setClassName(getClass().getName());
    }

    @Override
    protected void onWindowVisibilityChanged(int i) {
        super.onWindowVisibilityChanged(i);
        maybeRegisterAutoAdvance();
    }

    private void checkIfAutoAdvance() {
        boolean z;
        Advanceable advanceable = getAdvanceable();
        boolean z2 = false;
        if (advanceable == null) {
            z = false;
        } else {
            advanceable.fyiWillBeAdvancedByHostKThx();
            z = true;
        }
        if (sAutoAdvanceWidgetIds.indexOfKey(getAppWidgetId()) >= 0) {
            z2 = true;
        }
        if (z != z2) {
            if (z) {
                sAutoAdvanceWidgetIds.put(getAppWidgetId(), true);
            } else {
                sAutoAdvanceWidgetIds.delete(getAppWidgetId());
            }
            maybeRegisterAutoAdvance();
        }
    }

    private Advanceable getAdvanceable() {
        AppWidgetProviderInfo appWidgetInfo = getAppWidgetInfo();
        if (appWidgetInfo == null || appWidgetInfo.autoAdvanceViewId == -1 || !this.mIsAttachedToWindow) {
            return null;
        }
        KeyEvent.Callback callbackFindViewById = findViewById(appWidgetInfo.autoAdvanceViewId);
        if (callbackFindViewById instanceof Advanceable) {
            return (Advanceable) callbackFindViewById;
        }
        return null;
    }

    private void maybeRegisterAutoAdvance() {
        Handler handler = getHandler();
        boolean z = getWindowVisibility() == 0 && handler != null && sAutoAdvanceWidgetIds.indexOfKey(getAppWidgetId()) >= 0;
        if (z != this.mIsAutoAdvanceRegistered) {
            this.mIsAutoAdvanceRegistered = z;
            if (this.mAutoAdvanceRunnable == null) {
                this.mAutoAdvanceRunnable = new Runnable() {
                    @Override
                    public void run() {
                        LauncherAppWidgetHostView.this.runAutoAdvance();
                    }
                };
            }
            handler.removeCallbacks(this.mAutoAdvanceRunnable);
            scheduleNextAdvance();
        }
    }

    private void scheduleNextAdvance() {
        if (!this.mIsAutoAdvanceRegistered) {
            return;
        }
        long jUptimeMillis = SystemClock.uptimeMillis();
        long jIndexOfKey = jUptimeMillis + (ADVANCE_INTERVAL - (jUptimeMillis % ADVANCE_INTERVAL)) + (ADVANCE_STAGGER * ((long) sAutoAdvanceWidgetIds.indexOfKey(getAppWidgetId())));
        Handler handler = getHandler();
        if (handler != null) {
            handler.postAtTime(this.mAutoAdvanceRunnable, jIndexOfKey);
        }
    }

    public void runAutoAdvance() {
        Advanceable advanceable = getAdvanceable();
        if (advanceable != null) {
            advanceable.advance();
        }
        scheduleNextAdvance();
    }

    public void setScaleToFit(float f) {
        this.mScaleToFit = f;
        setScaleX(f);
        setScaleY(f);
    }

    public float getScaleToFit() {
        return this.mScaleToFit;
    }

    public void setTranslationForCentering(float f, float f2) {
        this.mTranslationForCentering.set(f, f2);
        setTranslationX(f);
        setTranslationY(f2);
    }

    public PointF getTranslationForCentering() {
        return this.mTranslationForCentering;
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (this.mReinflateOnConfigChange && isSameOrientation()) {
            this.mReinflateOnConfigChange = false;
            reInflate();
        }
    }

    public void reInflate() {
        if (!isAttachedToWindow()) {
            return;
        }
        LauncherAppWidgetInfo launcherAppWidgetInfo = (LauncherAppWidgetInfo) getTag();
        this.mLauncher.removeItem(this, launcherAppWidgetInfo, false);
        this.mLauncher.bindAppWidget(launcherAppWidgetInfo);
    }
}
