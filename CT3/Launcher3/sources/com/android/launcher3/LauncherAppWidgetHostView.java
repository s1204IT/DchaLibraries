package com.android.launcher3;

import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import com.android.launcher3.DragLayer;
import com.android.launcher3.compat.PackageInstallerCompat;
import com.mediatek.launcher3.LauncherLog;
import java.util.ArrayList;

public class LauncherAppWidgetHostView extends AppWidgetHostView implements DragLayer.TouchCompleteListener {
    private boolean mChildrenFocused;
    private Context mContext;
    private DragLayer mDragLayer;
    LayoutInflater mInflater;
    private CheckLongPressHelper mLongPressHelper;
    private int mPreviousOrientation;
    private float mSlop;
    private StylusEventHelper mStylusEventHelper;

    public LauncherAppWidgetHostView(Context context) {
        super(context);
        this.mContext = context;
        this.mLongPressHelper = new CheckLongPressHelper(this);
        this.mStylusEventHelper = new StylusEventHelper(this);
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mDragLayer = ((Launcher) context).getDragLayer();
        setAccessibilityDelegate(LauncherAppState.getInstance().getAccessibilityDelegate());
        setBackgroundResource(R.drawable.widget_internal_focus_bg);
    }

    @Override
    protected View getErrorView() {
        return this.mInflater.inflate(R.layout.appwidget_error, (ViewGroup) this, false);
    }

    public void updateLastInflationOrientation() {
        this.mPreviousOrientation = this.mContext.getResources().getConfiguration().orientation;
    }

    @Override
    public void updateAppWidget(RemoteViews remoteViews) {
        updateLastInflationOrientation();
        super.updateAppWidget(remoteViews);
    }

    public boolean isReinflateRequired() {
        int orientation = this.mContext.getResources().getConfiguration().orientation;
        if (this.mPreviousOrientation != orientation) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (LauncherLog.DEBUG_MOTION) {
            LauncherLog.d("LauncherAppWidgetHostView", "onInterceptTouchEvent: ev = " + ev);
        }
        if (ev.getAction() == 0) {
            this.mLongPressHelper.cancelLongPress();
        }
        if (this.mLongPressHelper.hasPerformedLongPress()) {
            this.mLongPressHelper.cancelLongPress();
            return true;
        }
        if (this.mStylusEventHelper.checkAndPerformStylusEvent(ev)) {
            this.mLongPressHelper.cancelLongPress();
            return true;
        }
        switch (ev.getAction()) {
            case PackageInstallerCompat.STATUS_INSTALLED:
                if (!this.mStylusEventHelper.inStylusButtonPressed()) {
                    this.mLongPressHelper.postCheckForLongPress();
                }
                this.mDragLayer.setTouchCompleteListener(this);
                return false;
            case PackageInstallerCompat.STATUS_INSTALLING:
            case 3:
                this.mLongPressHelper.cancelLongPress();
                return false;
            case PackageInstallerCompat.STATUS_FAILED:
                if (!Utilities.pointInView(this, ev.getX(), ev.getY(), this.mSlop)) {
                    this.mLongPressHelper.cancelLongPress();
                }
                return false;
            default:
                return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (LauncherLog.DEBUG_MOTION) {
            LauncherLog.d("LauncherAppWidgetHostView", "onTouchEvent: ev = " + ev);
        }
        switch (ev.getAction()) {
            case PackageInstallerCompat.STATUS_INSTALLING:
            case 3:
                this.mLongPressHelper.cancelLongPress();
                break;
            case PackageInstallerCompat.STATUS_FAILED:
                if (!Utilities.pointInView(this, ev.getX(), ev.getY(), this.mSlop)) {
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
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        this.mLongPressHelper.cancelLongPress();
    }

    @Override
    public AppWidgetProviderInfo getAppWidgetInfo() {
        AppWidgetProviderInfo info = super.getAppWidgetInfo();
        if (info != null && !(info instanceof LauncherAppWidgetProviderInfo)) {
            throw new IllegalStateException("Launcher widget must have LauncherAppWidgetProviderInfo");
        }
        return info;
    }

    @Override
    public void onTouchComplete() {
        if (this.mLongPressHelper.hasPerformedLongPress()) {
            return;
        }
        this.mLongPressHelper.cancelLongPress();
    }

    @Override
    public int getDescendantFocusability() {
        return this.mChildrenFocused ? 131072 : 393216;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (this.mChildrenFocused && event.getKeyCode() == 111 && event.getAction() == 1) {
            this.mChildrenFocused = false;
            requestFocus();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!this.mChildrenFocused && keyCode == 66) {
            event.startTracking();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (event.isTracking() && !this.mChildrenFocused && keyCode == 66) {
            this.mChildrenFocused = true;
            ArrayList<View> focusableChildren = getFocusables(2);
            focusableChildren.remove(this);
            int childrenCount = focusableChildren.size();
            switch (childrenCount) {
                case PackageInstallerCompat.STATUS_INSTALLED:
                    this.mChildrenFocused = false;
                    break;
                case PackageInstallerCompat.STATUS_INSTALLING:
                    if (getTag() instanceof ItemInfo) {
                        ItemInfo item = (ItemInfo) getTag();
                        if (item.spanX == 1 && item.spanY == 1) {
                            focusableChildren.get(0).performClick();
                            this.mChildrenFocused = false;
                            return true;
                        }
                    }
                default:
                    focusableChildren.get(0).requestFocus();
                    return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        if (gainFocus) {
            this.mChildrenFocused = false;
            dispatchChildFocus(false);
        }
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        dispatchChildFocus(this.mChildrenFocused && focused != null);
        if (focused == null) {
            return;
        }
        focused.setFocusableInTouchMode(false);
    }

    @Override
    public void clearChildFocus(View child) {
        super.clearChildFocus(child);
        dispatchChildFocus(false);
    }

    @Override
    public boolean dispatchUnhandledMove(View focused, int direction) {
        return this.mChildrenFocused;
    }

    private void dispatchChildFocus(boolean childIsFocused) {
        setSelected(childIsFocused);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        try {
            super.onLayout(changed, left, top, right, bottom);
        } catch (RuntimeException e) {
            post(new Runnable() {
                @Override
                public void run() {
                    LauncherAppWidgetHostView.this.updateAppWidget(new RemoteViews(LauncherAppWidgetHostView.this.getAppWidgetInfo().provider.getPackageName(), 0));
                }
            });
        }
    }
}
