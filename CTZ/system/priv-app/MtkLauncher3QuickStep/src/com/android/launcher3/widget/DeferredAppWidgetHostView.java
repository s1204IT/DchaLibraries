package com.android.launcher3.widget;

import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.graphics.Canvas;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.TypedValue;
import android.widget.RemoteViews;
import com.android.launcher3.R;

/* loaded from: classes.dex */
public class DeferredAppWidgetHostView extends LauncherAppWidgetHostView {
    private final TextPaint mPaint;
    private Layout mSetupTextLayout;

    public DeferredAppWidgetHostView(Context context) {
        super(context);
        setWillNotDraw(false);
        this.mPaint = new TextPaint();
        this.mPaint.setColor(-1);
        this.mPaint.setTextSize(TypedValue.applyDimension(0, this.mLauncher.getDeviceProfile().getFullScreenProfile().iconTextSizePx, getResources().getDisplayMetrics()));
        setBackgroundResource(R.drawable.bg_deferred_app_widget);
    }

    @Override // com.android.launcher3.widget.LauncherAppWidgetHostView, android.appwidget.AppWidgetHostView
    public void updateAppWidget(RemoteViews remoteViews) {
    }

    @Override // android.widget.FrameLayout, android.view.View
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        AppWidgetProviderInfo appWidgetInfo = getAppWidgetInfo();
        if (appWidgetInfo == null || TextUtils.isEmpty(appWidgetInfo.label)) {
            return;
        }
        int measuredWidth = getMeasuredWidth() - (2 * (getPaddingLeft() + getPaddingRight()));
        if (this.mSetupTextLayout != null && this.mSetupTextLayout.getText().equals(appWidgetInfo.label) && this.mSetupTextLayout.getWidth() == measuredWidth) {
            return;
        }
        this.mSetupTextLayout = new StaticLayout(appWidgetInfo.label, this.mPaint, measuredWidth, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, true);
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        if (this.mSetupTextLayout != null) {
            canvas.translate(getPaddingLeft() * 2, (getHeight() - this.mSetupTextLayout.getHeight()) / 2);
            this.mSetupTextLayout.draw(canvas);
        }
    }
}
