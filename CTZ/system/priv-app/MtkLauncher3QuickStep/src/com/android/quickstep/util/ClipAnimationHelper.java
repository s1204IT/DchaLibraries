package com.android.quickstep.util;

import android.annotation.TargetApi;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.view.animation.Interpolator;
import com.android.launcher3.BaseDraggingActivity;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.views.BaseDragLayer;
import com.android.quickstep.RecentsModel;
import com.android.quickstep.views.RecentsView;
import com.android.quickstep.views.TaskThumbnailView;
import com.android.systemui.shared.recents.ISystemUiProxy;
import com.android.systemui.shared.recents.utilities.RectFEvaluator;
import com.android.systemui.shared.system.RemoteAnimationTargetCompat;
import com.android.systemui.shared.system.TransactionCompat;
import com.android.systemui.shared.system.WindowManagerWrapper;
import java.util.function.BiConsumer;

@TargetApi(28)
/* loaded from: classes.dex */
public class ClipAnimationHelper {
    private final Rect mSourceStackBounds = new Rect();
    private final Rect mSourceInsets = new Rect();
    private final RectF mSourceRect = new RectF();
    private final RectF mTargetRect = new RectF();
    private final PointF mTargetOffset = new PointF();
    private final RectF mSourceWindowClipInsets = new RectF();
    public final Rect mHomeStackBounds = new Rect();
    private final Rect mClipRect = new Rect();
    private final RectFEvaluator mRectFEvaluator = new RectFEvaluator();
    private final Matrix mTmpMatrix = new Matrix();
    private final RectF mTmpRectF = new RectF();
    private float mTargetScale = 1.0f;
    private float mOffsetScale = 1.0f;
    private Interpolator mInterpolator = Interpolators.LINEAR;
    private Interpolator mOffsetYInterpolator = Interpolators.LINEAR;
    private int mBoostModeTargetLayers = -1;
    private boolean mIsFirstFrame = true;
    private BiConsumer<TransactionCompat, RemoteAnimationTargetCompat> mTaskTransformCallback = new BiConsumer() { // from class: com.android.quickstep.util.-$$Lambda$ClipAnimationHelper$xyiLO_7N0FwvSxoaYNlHX38Rbm0
        @Override // java.util.function.BiConsumer
        public final void accept(Object obj, Object obj2) {
            ClipAnimationHelper.lambda$new$0((TransactionCompat) obj, (RemoteAnimationTargetCompat) obj2);
        }
    };

    static /* synthetic */ void lambda$new$0(TransactionCompat transactionCompat, RemoteAnimationTargetCompat remoteAnimationTargetCompat) {
    }

    private void updateSourceStack(RemoteAnimationTargetCompat remoteAnimationTargetCompat) {
        this.mSourceInsets.set(remoteAnimationTargetCompat.contentInsets);
        this.mSourceStackBounds.set(remoteAnimationTargetCompat.sourceContainerBounds);
        this.mSourceStackBounds.offsetTo(remoteAnimationTargetCompat.position.x, remoteAnimationTargetCompat.position.y);
    }

    public void updateSource(Rect rect, RemoteAnimationTargetCompat remoteAnimationTargetCompat) {
        this.mHomeStackBounds.set(rect);
        updateSourceStack(remoteAnimationTargetCompat);
    }

    public void updateTargetRect(TransformedRect transformedRect) {
        this.mOffsetScale = transformedRect.scale;
        this.mSourceRect.set(this.mSourceInsets.left, this.mSourceInsets.top, this.mSourceStackBounds.width() - this.mSourceInsets.right, this.mSourceStackBounds.height() - this.mSourceInsets.bottom);
        this.mTargetRect.set(transformedRect.rect);
        Utilities.scaleRectFAboutCenter(this.mTargetRect, transformedRect.scale);
        this.mTargetRect.offset(this.mHomeStackBounds.left - this.mSourceStackBounds.left, this.mHomeStackBounds.top - this.mSourceStackBounds.top);
        RectF rectF = new RectF(this.mTargetRect);
        Utilities.scaleRectFAboutCenter(rectF, this.mSourceRect.width() / this.mTargetRect.width());
        rectF.offsetTo(this.mSourceRect.left, this.mSourceRect.top);
        this.mSourceWindowClipInsets.set(Math.max(rectF.left, 0.0f), Math.max(rectF.top, 0.0f), Math.max(this.mSourceStackBounds.width() - rectF.right, 0.0f), Math.max(this.mSourceStackBounds.height() - rectF.bottom, 0.0f));
        this.mSourceRect.set(rectF);
    }

    public void prepareAnimation(boolean z) {
        this.mIsFirstFrame = true;
        this.mBoostModeTargetLayers = !z ? 1 : 0;
    }

    public RectF applyTransform(RemoteAnimationTargetSet remoteAnimationTargetSet, float f) {
        this.mTmpRectF.set(this.mTargetRect);
        Utilities.scaleRectFAboutCenter(this.mTmpRectF, this.mTargetScale);
        float interpolation = this.mOffsetYInterpolator.getInterpolation(f);
        float interpolation2 = this.mInterpolator.getInterpolation(f);
        RectF rectFEvaluate = this.mRectFEvaluator.evaluate(interpolation2, this.mSourceRect, this.mTmpRectF);
        synchronized (this.mTargetOffset) {
            rectFEvaluate.offset(this.mTargetOffset.x * this.mOffsetScale * interpolation2, this.mTargetOffset.y * interpolation);
        }
        this.mClipRect.left = (int) (this.mSourceWindowClipInsets.left * interpolation2);
        this.mClipRect.top = (int) (this.mSourceWindowClipInsets.top * interpolation2);
        this.mClipRect.right = (int) (this.mSourceStackBounds.width() - (this.mSourceWindowClipInsets.right * interpolation2));
        this.mClipRect.bottom = (int) (this.mSourceStackBounds.height() - (this.mSourceWindowClipInsets.bottom * interpolation2));
        TransactionCompat transactionCompat = new TransactionCompat();
        if (this.mIsFirstFrame) {
            RemoteAnimationProvider.prepareTargetsForFirstFrame(remoteAnimationTargetSet.unfilteredApps, transactionCompat, this.mBoostModeTargetLayers);
            this.mIsFirstFrame = false;
        }
        for (RemoteAnimationTargetCompat remoteAnimationTargetCompat : remoteAnimationTargetSet.apps) {
            if (remoteAnimationTargetCompat.activityType != 2) {
                this.mTmpMatrix.setRectToRect(this.mSourceRect, rectFEvaluate, Matrix.ScaleToFit.FILL);
                this.mTmpMatrix.postTranslate(remoteAnimationTargetCompat.position.x, remoteAnimationTargetCompat.position.y);
                transactionCompat.setMatrix(remoteAnimationTargetCompat.leash, this.mTmpMatrix).setWindowCrop(remoteAnimationTargetCompat.leash, this.mClipRect);
            }
            if (remoteAnimationTargetCompat.isNotInRecents || remoteAnimationTargetCompat.activityType == 2) {
                transactionCompat.setAlpha(remoteAnimationTargetCompat.leash, 1.0f - interpolation2);
            }
            this.mTaskTransformCallback.accept(transactionCompat, remoteAnimationTargetCompat);
        }
        transactionCompat.setEarlyWakeup();
        transactionCompat.apply();
        return rectFEvaluate;
    }

    public void setTaskTransformCallback(BiConsumer<TransactionCompat, RemoteAnimationTargetCompat> biConsumer) {
        this.mTaskTransformCallback = biConsumer;
    }

    public void offsetTarget(float f, float f2, float f3, Interpolator interpolator) {
        synchronized (this.mTargetOffset) {
            this.mTargetOffset.set(f2, f3);
        }
        this.mTargetScale = f;
        this.mInterpolator = interpolator;
        this.mOffsetYInterpolator = Interpolators.clampToProgress(this.mInterpolator, 0.0f, 0.8333333f);
    }

    public void fromTaskThumbnailView(TaskThumbnailView taskThumbnailView, RecentsView recentsView) {
        fromTaskThumbnailView(taskThumbnailView, recentsView, null);
    }

    public void fromTaskThumbnailView(TaskThumbnailView taskThumbnailView, RecentsView recentsView, @Nullable RemoteAnimationTargetCompat remoteAnimationTargetCompat) {
        BaseDraggingActivity baseDraggingActivityFromContext = BaseDraggingActivity.fromContext(taskThumbnailView.getContext());
        BaseDragLayer dragLayer = baseDraggingActivityFromContext.getDragLayer();
        int[] iArr = new int[2];
        dragLayer.getLocationOnScreen(iArr);
        this.mHomeStackBounds.set(0, 0, dragLayer.getWidth(), dragLayer.getHeight());
        this.mHomeStackBounds.offset(iArr[0], iArr[1]);
        if (remoteAnimationTargetCompat != null) {
            updateSourceStack(remoteAnimationTargetCompat);
        } else if (recentsView.shouldUseMultiWindowTaskSizeStrategy()) {
            updateStackBoundsToMultiWindowTaskSize(baseDraggingActivityFromContext);
        } else {
            this.mSourceStackBounds.set(this.mHomeStackBounds);
            this.mSourceInsets.set(baseDraggingActivityFromContext.getDeviceProfile().getInsets());
        }
        TransformedRect transformedRect = new TransformedRect();
        dragLayer.getDescendantRectRelativeToSelf(taskThumbnailView, transformedRect.rect);
        updateTargetRect(transformedRect);
        if (remoteAnimationTargetCompat == null) {
            float fWidth = this.mTargetRect.width() / this.mSourceRect.width();
            this.mSourceWindowClipInsets.left *= fWidth;
            this.mSourceWindowClipInsets.top *= fWidth;
            this.mSourceWindowClipInsets.right *= fWidth;
            this.mSourceWindowClipInsets.bottom *= fWidth;
        }
    }

    private void updateStackBoundsToMultiWindowTaskSize(BaseDraggingActivity baseDraggingActivity) {
        ISystemUiProxy systemUiProxy = RecentsModel.getInstance(baseDraggingActivity).getSystemUiProxy();
        if (systemUiProxy != null) {
            try {
                this.mSourceStackBounds.set(systemUiProxy.getNonMinimizedSplitScreenSecondaryBounds());
                return;
            } catch (RemoteException e) {
            }
        }
        DeviceProfile fullScreenProfile = baseDraggingActivity.getDeviceProfile().getFullScreenProfile();
        int i = fullScreenProfile.availableWidthPx;
        int i2 = fullScreenProfile.availableHeightPx;
        int dimensionPixelSize = baseDraggingActivity.getResources().getDimensionPixelSize(R.dimen.multi_window_task_divider_size) / 2;
        Rect rect = new Rect();
        WindowManagerWrapper.getInstance().getStableInsets(rect);
        if (fullScreenProfile.isLandscape) {
            i = (i / 2) - dimensionPixelSize;
        } else {
            i2 = (i2 / 2) - dimensionPixelSize;
        }
        int i3 = baseDraggingActivity.getDeviceProfile().isSeascape() ? rect.left : (rect.left + fullScreenProfile.availableWidthPx) - i;
        this.mSourceStackBounds.set(0, 0, i, i2);
        this.mSourceStackBounds.offset(i3, (rect.top + fullScreenProfile.availableHeightPx) - i2);
    }

    public void drawForProgress(TaskThumbnailView taskThumbnailView, Canvas canvas, float f) {
        RectF rectFEvaluate = this.mRectFEvaluator.evaluate(f, this.mSourceRect, this.mTargetRect);
        canvas.translate(this.mSourceStackBounds.left - this.mHomeStackBounds.left, this.mSourceStackBounds.top - this.mHomeStackBounds.top);
        this.mTmpMatrix.setRectToRect(this.mTargetRect, rectFEvaluate, Matrix.ScaleToFit.FILL);
        canvas.concat(this.mTmpMatrix);
        canvas.translate(this.mTargetRect.left, this.mTargetRect.top);
        float f2 = 1.0f - f;
        taskThumbnailView.drawOnCanvas(canvas, (-this.mSourceWindowClipInsets.left) * f2, (-this.mSourceWindowClipInsets.top) * f2, taskThumbnailView.getMeasuredWidth() + (this.mSourceWindowClipInsets.right * f2), taskThumbnailView.getMeasuredHeight() + (this.mSourceWindowClipInsets.bottom * f2), taskThumbnailView.getCornerRadius() * f);
    }

    public RectF getTargetRect() {
        return this.mTargetRect;
    }

    public RectF getSourceRect() {
        return this.mSourceRect;
    }
}
