package com.android.systemui.recents.views;

import android.app.ActivityManager;
import android.content.Context;
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
import com.android.systemui.recents.model.Task;
import com.mediatek.systemui.statusbar.util.FeatureOptions;

public class TaskViewThumbnail extends View {
    private Paint mBgFillPaint;
    private BitmapShader mBitmapShader;
    private int mCornerRadius;

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
    private Matrix mScaleMatrix;
    private Task mTask;
    private View mTaskBar;

    @ViewDebug.ExportedProperty(category = "recents")
    private Rect mTaskViewRect;
    private ActivityManager.TaskThumbnailInfo mThumbnailInfo;

    @ViewDebug.ExportedProperty(category = "recents")
    private Rect mThumbnailRect;

    @ViewDebug.ExportedProperty(category = "recents")
    private float mThumbnailScale;
    private static final ColorMatrix TMP_FILTER_COLOR_MATRIX = new ColorMatrix();
    private static final ColorMatrix TMP_BRIGHTNESS_COLOR_MATRIX = new ColorMatrix();

    public TaskViewThumbnail(Context context) {
        this(context, null);
    }

    public TaskViewThumbnail(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TaskViewThumbnail(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TaskViewThumbnail(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mDisplayOrientation = 0;
        this.mDisplayRect = new Rect();
        this.mTaskViewRect = new Rect();
        this.mThumbnailRect = new Rect();
        this.mScaleMatrix = new Matrix();
        this.mDrawPaint = new Paint();
        this.mBgFillPaint = new Paint();
        this.mLightingColorFilter = new LightingColorFilter(-1, 0);
        this.mDrawPaint.setColorFilter(this.mLightingColorFilter);
        this.mDrawPaint.setFilterBitmap(true);
        this.mDrawPaint.setAntiAlias(true);
        this.mCornerRadius = getResources().getDimensionPixelSize(R.dimen.recents_task_view_rounded_corners_radius);
        this.mBgFillPaint.setColor(-1);
        this.mFullscreenThumbnailScale = context.getResources().getFraction(android.R.fraction.config_prescaleAbsoluteVolume_index1, 1, 1);
    }

    public void onTaskViewSizeChanged(int width, int height) {
        if (this.mTaskViewRect.width() == width && this.mTaskViewRect.height() == height) {
            return;
        }
        this.mTaskViewRect.set(0, 0, width, height);
        setLeftTopRightBottom(0, 0, width, height);
        updateThumbnailScale();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int topOffset;
        if (this.mInvisible) {
            return;
        }
        int viewWidth = this.mTaskViewRect.width();
        int viewHeight = this.mTaskViewRect.height();
        int thumbnailWidth = Math.min(viewWidth, (int) (this.mThumbnailRect.width() * this.mThumbnailScale));
        int thumbnailHeight = Math.min(viewHeight, (int) (this.mThumbnailRect.height() * this.mThumbnailScale));
        if (this.mBitmapShader != null && thumbnailWidth > 0 && thumbnailHeight > 0) {
            if (this.mTaskBar != null) {
                topOffset = this.mTaskBar.getHeight() - this.mCornerRadius;
            } else {
                topOffset = 0;
            }
            if (thumbnailWidth < viewWidth) {
                canvas.drawRoundRect(Math.max(0, thumbnailWidth - this.mCornerRadius), topOffset, viewWidth, viewHeight, this.mCornerRadius, this.mCornerRadius, this.mBgFillPaint);
            }
            if (thumbnailHeight < viewHeight) {
                canvas.drawRoundRect(0.0f, Math.max(topOffset, thumbnailHeight - this.mCornerRadius), viewWidth, viewHeight, this.mCornerRadius, this.mCornerRadius, this.mBgFillPaint);
            }
            canvas.drawRoundRect(0.0f, topOffset, thumbnailWidth, thumbnailHeight, this.mCornerRadius, this.mCornerRadius, this.mDrawPaint);
            return;
        }
        canvas.drawRoundRect(0.0f, 0.0f, viewWidth, viewHeight, this.mCornerRadius, this.mCornerRadius, this.mBgFillPaint);
    }

    void setThumbnail(Bitmap bm, ActivityManager.TaskThumbnailInfo thumbnailInfo) {
        if (bm != null) {
            this.mBitmapShader = new BitmapShader(bm, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            this.mDrawPaint.setShader(this.mBitmapShader);
            this.mThumbnailRect.set(0, 0, bm.getWidth(), bm.getHeight());
            this.mThumbnailInfo = thumbnailInfo;
            updateThumbnailScale();
            return;
        }
        this.mBitmapShader = null;
        this.mDrawPaint.setShader(null);
        this.mThumbnailRect.setEmpty();
        this.mThumbnailInfo = null;
    }

    void updateThumbnailPaintFilter() {
        if (this.mInvisible) {
            return;
        }
        int mul = (int) ((1.0f - this.mDimAlpha) * 255.0f);
        if (this.mBitmapShader != null) {
            if (this.mDisabledInSafeMode) {
                TMP_FILTER_COLOR_MATRIX.setSaturation(0.0f);
                float scale = 1.0f - this.mDimAlpha;
                float[] mat = TMP_BRIGHTNESS_COLOR_MATRIX.getArray();
                mat[0] = scale;
                mat[6] = scale;
                mat[12] = scale;
                mat[4] = this.mDimAlpha * 255.0f;
                mat[9] = this.mDimAlpha * 255.0f;
                mat[14] = this.mDimAlpha * 255.0f;
                TMP_FILTER_COLOR_MATRIX.preConcat(TMP_BRIGHTNESS_COLOR_MATRIX);
                ColorMatrixColorFilter filter = new ColorMatrixColorFilter(TMP_FILTER_COLOR_MATRIX);
                this.mDrawPaint.setColorFilter(filter);
                this.mBgFillPaint.setColorFilter(filter);
            } else {
                this.mLightingColorFilter.setColorMultiply(Color.argb(255, mul, mul, mul));
                this.mDrawPaint.setColorFilter(this.mLightingColorFilter);
                this.mDrawPaint.setColor(-1);
                this.mBgFillPaint.setColorFilter(this.mLightingColorFilter);
            }
        } else {
            this.mDrawPaint.setColorFilter(null);
            this.mDrawPaint.setColor(Color.argb(255, mul, mul, mul));
        }
        if (this.mInvisible) {
            return;
        }
        invalidate();
    }

    public void updateThumbnailScale() {
        this.mThumbnailScale = 1.0f;
        if (this.mBitmapShader != null) {
            boolean isStackTask = !this.mTask.isFreeformTask() || this.mTask.bounds == null;
            if (this.mTaskViewRect.isEmpty() || this.mThumbnailInfo == null || this.mThumbnailInfo.taskWidth == 0 || this.mThumbnailInfo.taskHeight == 0) {
                this.mThumbnailScale = 0.0f;
            } else if (isStackTask) {
                float invThumbnailScale = 1.0f / this.mFullscreenThumbnailScale;
                if (FeatureOptions.LOW_RAM_SUPPORT) {
                    invThumbnailScale *= 2.0f;
                }
                if (this.mDisplayOrientation == 1) {
                    if (this.mThumbnailInfo.screenOrientation == 1) {
                        this.mThumbnailScale = this.mTaskViewRect.width() / this.mThumbnailRect.width();
                    } else {
                        this.mThumbnailScale = (this.mTaskViewRect.width() / this.mDisplayRect.width()) * invThumbnailScale;
                    }
                } else {
                    this.mThumbnailScale = invThumbnailScale;
                }
            } else {
                this.mThumbnailScale = Math.min(this.mTaskViewRect.width() / this.mThumbnailRect.width(), this.mTaskViewRect.height() / this.mThumbnailRect.height());
            }
            this.mScaleMatrix.setScale(this.mThumbnailScale, this.mThumbnailScale);
            this.mBitmapShader.setLocalMatrix(this.mScaleMatrix);
        }
        if (this.mInvisible) {
            return;
        }
        invalidate();
    }

    void updateClipToTaskBar(View taskBar) {
        this.mTaskBar = taskBar;
        invalidate();
    }

    void updateThumbnailVisibility(int clipBottom) {
        boolean invisible = this.mTaskBar != null && getHeight() - clipBottom <= this.mTaskBar.getHeight();
        if (invisible == this.mInvisible) {
            return;
        }
        this.mInvisible = invisible;
        if (this.mInvisible) {
            return;
        }
        updateThumbnailPaintFilter();
    }

    public void setDimAlpha(float dimAlpha) {
        this.mDimAlpha = dimAlpha;
        updateThumbnailPaintFilter();
    }

    void bindToTask(Task t, boolean disabledInSafeMode, int displayOrientation, Rect displayRect) {
        this.mTask = t;
        this.mDisabledInSafeMode = disabledInSafeMode;
        this.mDisplayOrientation = displayOrientation;
        this.mDisplayRect.set(displayRect);
        if (t.colorBackground == 0) {
            return;
        }
        this.mBgFillPaint.setColor(t.colorBackground);
    }

    void onTaskDataLoaded(ActivityManager.TaskThumbnailInfo thumbnailInfo) {
        if (this.mTask.thumbnail != null) {
            setThumbnail(this.mTask.thumbnail, thumbnailInfo);
        } else {
            setThumbnail(null, null);
        }
    }

    void unbindFromTask() {
        this.mTask = null;
        setThumbnail(null, null);
    }
}
