package com.android.launcher2;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import com.android.launcher.R;

public class LauncherAppWidgetHostView extends AppWidgetHostView {
    private Context mContext;
    private LayoutInflater mInflater;
    private CheckLongPressHelper mLongPressHelper;
    private int mPreviousOrientation;

    public LauncherAppWidgetHostView(Context context) {
        super(context);
        this.mContext = context;
        this.mLongPressHelper = new CheckLongPressHelper(this);
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
    }

    @Override
    protected View getErrorView() {
        return this.mInflater.inflate(R.layout.appwidget_error, (ViewGroup) this, false);
    }

    @Override
    public void updateAppWidget(RemoteViews remoteViews) {
        this.mPreviousOrientation = this.mContext.getResources().getConfiguration().orientation;
        super.updateAppWidget(remoteViews);
    }

    public boolean orientationChangedSincedInflation() {
        int orientation = this.mContext.getResources().getConfiguration().orientation;
        return this.mPreviousOrientation != orientation;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (this.mLongPressHelper.hasPerformedLongPress()) {
            this.mLongPressHelper.cancelLongPress();
            return true;
        }
        switch (ev.getAction()) {
            case 0:
                this.mLongPressHelper.postCheckForLongPress();
                return false;
            case 1:
            case 3:
                this.mLongPressHelper.cancelLongPress();
                return false;
            case 2:
            default:
                return false;
        }
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        this.mLongPressHelper.cancelLongPress();
    }

    @Override
    public int getDescendantFocusability() {
        return 393216;
    }
}
