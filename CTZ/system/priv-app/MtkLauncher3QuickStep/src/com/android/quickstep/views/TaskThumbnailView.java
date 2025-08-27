package com.android.quickstep.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.util.Property;
import android.view.View;
import com.android.launcher3.BaseActivity;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.Themes;
import com.android.quickstep.TaskOverlayFactory;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.model.ThumbnailData;

/* loaded from: classes.dex */
public class TaskThumbnailView extends View {
    private final BaseActivity mActivity;
    private final Paint mBackgroundPaint;
    protected BitmapShader mBitmapShader;
    private float mClipBottom;
    private final float mCornerRadius;
    private float mDimAlpha;
    private float mDimAlphaMultiplier;
    private final boolean mIsDarkTextTheme;
    private final Matrix mMatrix;
    private final TaskOverlayFactory.TaskOverlay mOverlay;
    private final Paint mPaint;
    private Task mTask;
    private ThumbnailData mThumbnailData;
    private static final LightingColorFilter[] sDimFilterCache = new LightingColorFilter[256];
    private static final LightingColorFilter[] sHighlightFilterCache = new LightingColorFilter[256];
    public static final Property<TaskThumbnailView, Float> DIM_ALPHA_MULTIPLIER = new FloatProperty<TaskThumbnailView>("dimAlphaMultiplier") { // from class: com.android.quickstep.views.TaskThumbnailView.1
        /* JADX DEBUG: Method merged with bridge method: setValue(Ljava/lang/Object;F)V */
        @Override // android.util.FloatProperty
        public void setValue(TaskThumbnailView taskThumbnailView, float f) {
            taskThumbnailView.setDimAlphaMultipler(f);
        }

        /* JADX DEBUG: Method merged with bridge method: get(Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.util.Property
        public Float get(TaskThumbnailView taskThumbnailView) {
            return Float.valueOf(taskThumbnailView.mDimAlphaMultiplier);
        }
    };

    public TaskThumbnailView(Context context) {
        this(context, null);
    }

    public TaskThumbnailView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public TaskThumbnailView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mPaint = new Paint();
        this.mBackgroundPaint = new Paint();
        this.mMatrix = new Matrix();
        this.mClipBottom = -1.0f;
        this.mDimAlpha = 1.0f;
        this.mDimAlphaMultiplier = 1.0f;
        this.mCornerRadius = getResources().getDimension(R.dimen.task_corner_radius);
        this.mOverlay = TaskOverlayFactory.get(context).createOverlay(this);
        this.mPaint.setFilterBitmap(true);
        this.mBackgroundPaint.setColor(-1);
        this.mActivity = BaseActivity.fromContext(context);
        this.mIsDarkTextTheme = Themes.getAttrBoolean(this.mActivity, R.attr.isWorkspaceDarkText);
    }

    public void bind() {
        this.mOverlay.reset();
    }

    public void setThumbnail(Task task, ThumbnailData thumbnailData) {
        this.mTask = task;
        int i = ViewCompat.MEASURED_STATE_MASK;
        if (task != null) {
            i = (-16777216) | task.colorBackground;
        }
        this.mPaint.setColor(i);
        this.mBackgroundPaint.setColor(i);
        if (thumbnailData != null && thumbnailData.thumbnail != null) {
            Bitmap bitmap = thumbnailData.thumbnail;
            bitmap.prepareToDraw();
            this.mBitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            this.mPaint.setShader(this.mBitmapShader);
            this.mThumbnailData = thumbnailData;
            updateThumbnailMatrix();
        } else {
            this.mBitmapShader = null;
            this.mThumbnailData = null;
            this.mPaint.setShader(null);
            this.mOverlay.reset();
        }
        updateThumbnailPaintFilter();
    }

    public void setDimAlphaMultipler(float f) {
        this.mDimAlphaMultiplier = f;
        setDimAlpha(this.mDimAlpha);
    }

    public void setDimAlpha(float f) {
        this.mDimAlpha = f;
        updateThumbnailPaintFilter();
    }

    public Rect getInsets() {
        if (this.mThumbnailData != null) {
            return this.mThumbnailData.insets;
        }
        return new Rect();
    }

    public int getSysUiStatusNavFlags() {
        int i;
        int i2;
        if (this.mThumbnailData == null) {
            return 0;
        }
        if ((this.mThumbnailData.systemUiVisibility & 8192) != 0) {
            i = 4;
        } else {
            i = 8;
        }
        int i3 = i | 0;
        if ((this.mThumbnailData.systemUiVisibility & 16) != 0) {
            i2 = 1;
        } else {
            i2 = 2;
        }
        return i3 | i2;
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        drawOnCanvas(canvas, 0.0f, 0.0f, getMeasuredWidth(), getMeasuredHeight(), this.mCornerRadius);
    }

    public float getCornerRadius() {
        return this.mCornerRadius;
    }

    public void drawOnCanvas(Canvas canvas, float f, float f2, float f3, float f4, float f5) {
        boolean z = this.mTask == null || this.mTask.isLocked || this.mBitmapShader == null || this.mThumbnailData == null;
        if (z || this.mClipBottom > 0.0f || this.mThumbnailData.isTranslucent) {
            canvas.drawRoundRect(f, f2, f3, f4, f5, f5, this.mBackgroundPaint);
            if (z) {
                return;
            }
        }
        if (this.mClipBottom > 0.0f) {
            canvas.save();
            canvas.clipRect(f, f2, f3, this.mClipBottom);
            canvas.drawRoundRect(f, f2, f3, f4, f5, f5, this.mPaint);
            canvas.restore();
            return;
        }
        canvas.drawRoundRect(f, f2, f3, f4, f5, f5, this.mPaint);
    }

    private void updateThumbnailPaintFilter() {
        int i = (int) ((1.0f - (this.mDimAlpha * this.mDimAlphaMultiplier)) * 255.0f);
        if (this.mBitmapShader != null) {
            LightingColorFilter dimmingColorFilter = getDimmingColorFilter(i, this.mIsDarkTextTheme);
            this.mPaint.setColorFilter(dimmingColorFilter);
            this.mBackgroundPaint.setColorFilter(dimmingColorFilter);
        } else {
            this.mPaint.setColorFilter(null);
            this.mPaint.setColor(Color.argb(255, i, i, i));
        }
        invalidate();
    }

    private void updateThumbnailMatrix() {
        float measuredWidth;
        this.mClipBottom = -1.0f;
        boolean z = false;
        if (this.mBitmapShader != null && this.mThumbnailData != null) {
            float f = this.mThumbnailData.scale;
            Rect rect = this.mThumbnailData.insets;
            float width = this.mThumbnailData.thumbnail.getWidth() - ((rect.left + rect.right) * f);
            float height = this.mThumbnailData.thumbnail.getHeight() - ((rect.top + rect.bottom) * f);
            DeviceProfile deviceProfile = this.mActivity.getDeviceProfile();
            if (getMeasuredWidth() != 0) {
                if (getContext().getResources().getConfiguration().orientation != this.mThumbnailData.orientation && !this.mActivity.isInMultiWindowModeCompat() && this.mThumbnailData.windowingMode == 1) {
                    z = true;
                }
                if (z) {
                    measuredWidth = getMeasuredWidth() / height;
                } else {
                    measuredWidth = getMeasuredWidth() / width;
                }
            } else {
                measuredWidth = 0.0f;
            }
            if (z) {
                int i = (!deviceProfile.isVerticalBarLayout() || deviceProfile.isSeascape()) ? 1 : -1;
                this.mMatrix.setRotate(90 * i);
                this.mMatrix.postTranslate((-(i == 1 ? rect.bottom : rect.top)) * f, (-(i == 1 ? rect.left : rect.right)) * f);
                if (i == -1) {
                    this.mMatrix.postTranslate(0.0f, -((width * measuredWidth) - getMeasuredHeight()));
                }
                if (i == 1) {
                    this.mMatrix.postTranslate(this.mThumbnailData.thumbnail.getHeight(), 0.0f);
                } else {
                    this.mMatrix.postTranslate(0.0f, this.mThumbnailData.thumbnail.getWidth());
                }
            } else {
                this.mMatrix.setTranslate((-this.mThumbnailData.insets.left) * f, (-this.mThumbnailData.insets.top) * f);
            }
            this.mMatrix.postScale(measuredWidth, measuredWidth);
            this.mBitmapShader.setLocalMatrix(this.mMatrix);
            if (!z) {
                width = height;
            }
            float fMax = Math.max(width * measuredWidth, 0.0f);
            if (Math.round(fMax) < getMeasuredHeight()) {
                this.mClipBottom = fMax;
            }
            this.mPaint.setShader(this.mBitmapShader);
        }
        if (z) {
            this.mOverlay.reset();
        } else {
            this.mOverlay.setTaskInfo(this.mTask, this.mThumbnailData, this.mMatrix);
        }
        invalidate();
    }

    @Override // android.view.View
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        updateThumbnailMatrix();
    }

    private static LightingColorFilter getDimmingColorFilter(int i, boolean z) {
        int iBoundToRange = Utilities.boundToRange(i, 0, 255);
        if (iBoundToRange == 255) {
            return null;
        }
        if (z) {
            if (sHighlightFilterCache[iBoundToRange] == null) {
                int i2 = 255 - iBoundToRange;
                sHighlightFilterCache[iBoundToRange] = new LightingColorFilter(Color.argb(255, iBoundToRange, iBoundToRange, iBoundToRange), Color.argb(255, i2, i2, i2));
            }
            return sHighlightFilterCache[iBoundToRange];
        }
        if (sDimFilterCache[iBoundToRange] == null) {
            sDimFilterCache[iBoundToRange] = new LightingColorFilter(Color.argb(255, iBoundToRange, iBoundToRange, iBoundToRange), 0);
        }
        return sDimFilterCache[iBoundToRange];
    }
}
