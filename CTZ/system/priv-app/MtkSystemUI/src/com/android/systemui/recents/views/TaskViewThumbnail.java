package com.android.systemui.recents.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewDebug;
import com.android.systemui.R;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.ui.TaskSnapshotChangedEvent;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.model.ThumbnailData;
import com.android.systemui.shared.recents.utilities.Utilities;
import java.io.PrintWriter;

/* loaded from: classes.dex */
public class TaskViewThumbnail extends View {
    protected Paint mBgFillPaint;
    protected BitmapShader mBitmapShader;
    protected int mCornerRadius;

    @ViewDebug.ExportedProperty(category = "recents")
    private float mDimAlpha;

    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mDisabledInSafeMode;
    private int mDisplayOrientation;
    private Rect mDisplayRect;
    private Paint mDrawPaint;
    private float mFullscreenThumbnailScale;

    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mInvisible;
    private LightingColorFilter mLightingColorFilter;
    protected Paint mLockedPaint;
    private Matrix mMatrix;
    private boolean mOverlayHeaderOnThumbnailActionBar;
    private boolean mSizeToFit;
    private Task mTask;
    private View mTaskBar;

    @ViewDebug.ExportedProperty(category = "recents")
    protected Rect mTaskViewRect;
    private ThumbnailData mThumbnailData;

    @ViewDebug.ExportedProperty(category = "recents")
    protected Rect mThumbnailRect;

    @ViewDebug.ExportedProperty(category = "recents")
    protected float mThumbnailScale;
    private int mTitleBarHeight;
    protected boolean mUserLocked;
    private static final ColorMatrix TMP_FILTER_COLOR_MATRIX = new ColorMatrix();
    private static final ColorMatrix TMP_BRIGHTNESS_COLOR_MATRIX = new ColorMatrix();

    public TaskViewThumbnail(Context context) {
        this(context, null);
    }

    public TaskViewThumbnail(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public TaskViewThumbnail(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public TaskViewThumbnail(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mDisplayOrientation = 0;
        this.mDisplayRect = new Rect();
        this.mTaskViewRect = new Rect();
        this.mThumbnailRect = new Rect();
        this.mFullscreenThumbnailScale = 1.0f;
        this.mSizeToFit = false;
        this.mOverlayHeaderOnThumbnailActionBar = true;
        this.mMatrix = new Matrix();
        this.mDrawPaint = new Paint();
        this.mLockedPaint = new Paint();
        this.mBgFillPaint = new Paint();
        this.mUserLocked = false;
        this.mLightingColorFilter = new LightingColorFilter(-1, 0);
        this.mDrawPaint.setColorFilter(this.mLightingColorFilter);
        this.mDrawPaint.setFilterBitmap(true);
        this.mDrawPaint.setAntiAlias(true);
        Resources resources = getResources();
        this.mCornerRadius = resources.getDimensionPixelSize(R.dimen.recents_task_view_rounded_corners_radius);
        this.mBgFillPaint.setColor(-1);
        this.mLockedPaint.setColor(-1);
        this.mTitleBarHeight = resources.getDimensionPixelSize(R.dimen.recents_grid_task_view_header_height);
    }

    public void onTaskViewSizeChanged(int i, int i2) {
        if (this.mTaskViewRect.width() == i && this.mTaskViewRect.height() == i2) {
            return;
        }
        this.mTaskViewRect.set(0, 0, i, i2);
        setLeftTopRightBottom(0, 0, i, i2);
        updateThumbnailMatrix();
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        int height;
        if (this.mInvisible) {
            return;
        }
        int iWidth = this.mTaskViewRect.width();
        int iHeight = this.mTaskViewRect.height();
        int iMin = Math.min(iWidth, (int) (this.mThumbnailRect.width() * this.mThumbnailScale));
        int iMin2 = Math.min(iHeight, (int) (this.mThumbnailRect.height() * this.mThumbnailScale));
        if (this.mUserLocked) {
            canvas.drawRoundRect(0.0f, 0.0f, iWidth, iHeight, this.mCornerRadius, this.mCornerRadius, this.mLockedPaint);
            return;
        }
        if (this.mBitmapShader != null && iMin > 0 && iMin2 > 0) {
            if (this.mTaskBar != null && this.mOverlayHeaderOnThumbnailActionBar) {
                height = this.mTaskBar.getHeight() - this.mCornerRadius;
            } else {
                height = 0;
            }
            if (iMin < iWidth) {
                canvas.drawRoundRect(Math.max(0, iMin - this.mCornerRadius), height, iWidth, iHeight, this.mCornerRadius, this.mCornerRadius, this.mBgFillPaint);
            }
            if (iMin2 < iHeight) {
                canvas.drawRoundRect(0.0f, Math.max(height, iMin2 - this.mCornerRadius), iWidth, iHeight, this.mCornerRadius, this.mCornerRadius, this.mBgFillPaint);
            }
            canvas.drawRoundRect(0.0f, height, iMin, iMin2, this.mCornerRadius, this.mCornerRadius, this.mDrawPaint);
            return;
        }
        canvas.drawRoundRect(0.0f, 0.0f, iWidth, iHeight, this.mCornerRadius, this.mCornerRadius, this.mBgFillPaint);
    }

    void setThumbnail(ThumbnailData thumbnailData) {
        if (thumbnailData != null && thumbnailData.thumbnail != null) {
            Bitmap bitmap = thumbnailData.thumbnail;
            bitmap.prepareToDraw();
            this.mFullscreenThumbnailScale = thumbnailData.scale;
            this.mBitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            this.mDrawPaint.setShader(this.mBitmapShader);
            this.mThumbnailRect.set(0, 0, (bitmap.getWidth() - thumbnailData.insets.left) - thumbnailData.insets.right, (bitmap.getHeight() - thumbnailData.insets.top) - thumbnailData.insets.bottom);
            this.mThumbnailData = thumbnailData;
            updateThumbnailMatrix();
            updateThumbnailPaintFilter();
            return;
        }
        this.mBitmapShader = null;
        this.mDrawPaint.setShader(null);
        this.mThumbnailRect.setEmpty();
        this.mThumbnailData = null;
    }

    void updateThumbnailPaintFilter() {
        if (this.mInvisible) {
            return;
        }
        int i = (int) ((1.0f - this.mDimAlpha) * 255.0f);
        if (this.mBitmapShader != null) {
            if (this.mDisabledInSafeMode) {
                TMP_FILTER_COLOR_MATRIX.setSaturation(0.0f);
                float f = 1.0f - this.mDimAlpha;
                float[] array = TMP_BRIGHTNESS_COLOR_MATRIX.getArray();
                array[0] = f;
                array[6] = f;
                array[12] = f;
                array[4] = this.mDimAlpha * 255.0f;
                array[9] = this.mDimAlpha * 255.0f;
                array[14] = this.mDimAlpha * 255.0f;
                TMP_FILTER_COLOR_MATRIX.preConcat(TMP_BRIGHTNESS_COLOR_MATRIX);
                ColorMatrixColorFilter colorMatrixColorFilter = new ColorMatrixColorFilter(TMP_FILTER_COLOR_MATRIX);
                this.mDrawPaint.setColorFilter(colorMatrixColorFilter);
                this.mBgFillPaint.setColorFilter(colorMatrixColorFilter);
                this.mLockedPaint.setColorFilter(colorMatrixColorFilter);
            } else {
                this.mLightingColorFilter.setColorMultiply(Color.argb(255, i, i, i));
                this.mDrawPaint.setColorFilter(this.mLightingColorFilter);
                this.mDrawPaint.setColor(-1);
                this.mBgFillPaint.setColorFilter(this.mLightingColorFilter);
                this.mLockedPaint.setColorFilter(this.mLightingColorFilter);
            }
        } else {
            this.mDrawPaint.setColorFilter(null);
            this.mDrawPaint.setColor(Color.argb(255, i, i, i));
        }
        if (!this.mInvisible) {
            invalidate();
        }
    }

    public void updateThumbnailMatrix() {
        this.mThumbnailScale = 1.0f;
        if (this.mBitmapShader != null && this.mThumbnailData != null) {
            if (this.mTaskViewRect.isEmpty()) {
                this.mThumbnailScale = 0.0f;
            } else if (!this.mSizeToFit) {
                float f = 1.0f / this.mFullscreenThumbnailScale;
                if (this.mDisplayOrientation == 1) {
                    if (this.mThumbnailData.orientation == 1) {
                        this.mThumbnailScale = this.mTaskViewRect.width() / this.mThumbnailRect.width();
                    } else {
                        this.mThumbnailScale = f * (this.mTaskViewRect.width() / this.mDisplayRect.width());
                    }
                } else {
                    this.mThumbnailScale = f;
                }
            } else if (this.mTaskViewRect.width() / (this.mTaskViewRect.height() - this.mTitleBarHeight) > this.mThumbnailRect.width() / this.mThumbnailRect.height()) {
                this.mThumbnailScale = this.mTaskViewRect.width() / this.mThumbnailRect.width();
            } else {
                this.mThumbnailScale = (this.mTaskViewRect.height() - this.mTitleBarHeight) / this.mThumbnailRect.height();
            }
            this.mMatrix.setTranslate((-this.mThumbnailData.insets.left) * this.mFullscreenThumbnailScale, (-this.mThumbnailData.insets.top) * this.mFullscreenThumbnailScale);
            this.mMatrix.postScale(this.mThumbnailScale, this.mThumbnailScale);
            this.mBitmapShader.setLocalMatrix(this.mMatrix);
        }
        if (!this.mInvisible) {
            invalidate();
        }
    }

    public void setSizeToFit(boolean z) {
        this.mSizeToFit = z;
    }

    public void setOverlayHeaderOnThumbnailActionBar(boolean z) {
        this.mOverlayHeaderOnThumbnailActionBar = z;
    }

    void updateClipToTaskBar(View view) {
        this.mTaskBar = view;
        invalidate();
    }

    void updateThumbnailVisibility(int i) {
        boolean z = this.mTaskBar != null && getHeight() - i <= this.mTaskBar.getHeight();
        if (z != this.mInvisible) {
            this.mInvisible = z;
            if (!this.mInvisible) {
                updateThumbnailPaintFilter();
            }
        }
    }

    public void setDimAlpha(float f) {
        this.mDimAlpha = f;
        updateThumbnailPaintFilter();
    }

    protected Paint getDrawPaint() {
        if (this.mUserLocked) {
            return this.mLockedPaint;
        }
        return this.mDrawPaint;
    }

    void bindToTask(Task task, boolean z, int i, Rect rect) throws NoSuchMethodException, SecurityException {
        this.mTask = task;
        this.mDisabledInSafeMode = z;
        this.mDisplayOrientation = i;
        this.mDisplayRect.set(rect);
        if (task.colorBackground != 0) {
            this.mBgFillPaint.setColor(task.colorBackground);
        }
        if (task.colorPrimary != 0) {
            this.mLockedPaint.setColor(task.colorPrimary);
        }
        this.mUserLocked = task.isLocked;
        EventBus.getDefault().register(this);
    }

    void onTaskDataLoaded(ThumbnailData thumbnailData) {
        setThumbnail(thumbnailData);
    }

    void unbindFromTask() {
        this.mTask = null;
        setThumbnail(null);
        EventBus.getDefault().unregister(this);
    }

    public final void onBusEvent(TaskSnapshotChangedEvent taskSnapshotChangedEvent) {
        if (this.mTask == null || taskSnapshotChangedEvent.taskId != this.mTask.key.id || taskSnapshotChangedEvent.thumbnailData == null || taskSnapshotChangedEvent.thumbnailData.thumbnail == null) {
            return;
        }
        setThumbnail(taskSnapshotChangedEvent.thumbnailData);
    }

    public void dump(String str, PrintWriter printWriter) {
        printWriter.print(str);
        printWriter.print("TaskViewThumbnail");
        printWriter.print(" mTaskViewRect=");
        printWriter.print(Utilities.dumpRect(this.mTaskViewRect));
        printWriter.print(" mThumbnailRect=");
        printWriter.print(Utilities.dumpRect(this.mThumbnailRect));
        printWriter.print(" mThumbnailScale=");
        printWriter.print(this.mThumbnailScale);
        printWriter.print(" mDimAlpha=");
        printWriter.print(this.mDimAlpha);
        printWriter.println();
    }
}
