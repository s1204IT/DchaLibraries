package com.android.launcher3.widget;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherAppWidgetProviderInfo;
import com.android.launcher3.R;
import com.android.launcher3.StylusEventHelper;
import com.android.launcher3.WidgetPreviewLoader;
import com.android.launcher3.compat.AppWidgetManagerCompat;

public class WidgetCell extends LinearLayout implements View.OnLayoutChangeListener {
    int cellSize;
    private WidgetPreviewLoader.PreviewLoadRequest mActiveRequest;
    private String mDimensionsFormatString;
    private Object mInfo;
    private Launcher mLauncher;
    private int mPresetPreviewSize;
    private StylusEventHelper mStylusEventHelper;
    private TextView mWidgetDims;
    private WidgetImageView mWidgetImage;
    private TextView mWidgetName;
    private WidgetPreviewLoader mWidgetPreviewLoader;

    public WidgetCell(Context context) {
        this(context, null);
    }

    public WidgetCell(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WidgetCell(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Resources r = context.getResources();
        this.mLauncher = (Launcher) context;
        this.mStylusEventHelper = new StylusEventHelper(this);
        this.mDimensionsFormatString = r.getString(R.string.widget_dims_format);
        setContainerWidth();
        setWillNotDraw(false);
        setClipToPadding(false);
        setAccessibilityDelegate(LauncherAppState.getInstance().getAccessibilityDelegate());
    }

    private void setContainerWidth() {
        DeviceProfile profile = this.mLauncher.getDeviceProfile();
        this.cellSize = (int) (profile.cellWidthPx * 2.6f);
        this.mPresetPreviewSize = (int) (this.cellSize * 0.8f);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mWidgetImage = (WidgetImageView) findViewById(R.id.widget_preview);
        this.mWidgetName = (TextView) findViewById(R.id.widget_name);
        this.mWidgetDims = (TextView) findViewById(R.id.widget_dims);
    }

    public void clear() {
        this.mWidgetImage.animate().cancel();
        this.mWidgetImage.setBitmap(null);
        this.mWidgetName.setText((CharSequence) null);
        this.mWidgetDims.setText((CharSequence) null);
        if (this.mActiveRequest == null) {
            return;
        }
        this.mActiveRequest.cleanup();
        this.mActiveRequest = null;
    }

    public void applyFromAppWidgetProviderInfo(LauncherAppWidgetProviderInfo info, WidgetPreviewLoader loader) {
        InvariantDeviceProfile profile = LauncherAppState.getInstance().getInvariantDeviceProfile();
        this.mInfo = info;
        this.mWidgetName.setText(AppWidgetManagerCompat.getInstance(getContext()).loadLabel(info));
        int hSpan = Math.min(info.spanX, profile.numColumns);
        int vSpan = Math.min(info.spanY, profile.numRows);
        this.mWidgetDims.setText(String.format(this.mDimensionsFormatString, Integer.valueOf(hSpan), Integer.valueOf(vSpan)));
        this.mWidgetPreviewLoader = loader;
    }

    public void applyFromResolveInfo(PackageManager pm, ResolveInfo info, WidgetPreviewLoader loader) {
        this.mInfo = info;
        CharSequence label = info.loadLabel(pm);
        this.mWidgetName.setText(label);
        this.mWidgetDims.setText(String.format(this.mDimensionsFormatString, 1, 1));
        this.mWidgetPreviewLoader = loader;
    }

    public int[] getPreviewSize() {
        int[] maxSize = {this.mPresetPreviewSize, this.mPresetPreviewSize};
        return maxSize;
    }

    public void applyPreview(Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        this.mWidgetImage.setBitmap(bitmap);
        this.mWidgetImage.setAlpha(0.0f);
        ViewPropertyAnimator anim = this.mWidgetImage.animate();
        anim.alpha(1.0f).setDuration(90L);
    }

    public void ensurePreview() {
        if (this.mActiveRequest != null) {
            return;
        }
        int[] size = getPreviewSize();
        this.mActiveRequest = this.mWidgetPreviewLoader.getPreview(this.mInfo, size[0], size[1], this);
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        removeOnLayoutChangeListener(this);
        ensurePreview();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean handled = super.onTouchEvent(ev);
        if (this.mStylusEventHelper.checkAndPerformStylusEvent(ev)) {
            return true;
        }
        return handled;
    }
}
