package com.android.launcher3.dragndrop;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.FloatArrayEvaluator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.pm.LauncherActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Handler;
import android.os.Looper;
import android.support.animation.FloatPropertyCompat;
import android.support.animation.SpringAnimation;
import android.support.animation.SpringForce;
import android.view.View;
import com.android.launcher3.FastBitmapDrawable;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.ItemInfoWithIcon;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAnimUtils;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.ShortcutConfigActivityInfo;
import com.android.launcher3.graphics.LauncherIcons;
import com.android.launcher3.shortcuts.DeepShortcutManager;
import com.android.launcher3.shortcuts.ShortcutInfoCompat;
import com.android.launcher3.shortcuts.ShortcutKey;
import com.android.launcher3.util.Themes;
import com.android.launcher3.views.BaseDragLayer;
import com.android.launcher3.widget.PendingAddShortcutInfo;
import java.util.Arrays;
import java.util.List;

public class DragView extends View {
    public static final int COLOR_CHANGE_DURATION = 120;
    public static final int VIEW_ZOOM_DURATION = 150;
    ValueAnimator mAnim;
    private int mAnimatedShiftX;
    private int mAnimatedShiftY;
    private boolean mAnimationCancelled;
    private Drawable mBadge;
    private ColorMatrixColorFilter mBaseFilter;
    private Drawable mBgSpringDrawable;
    private Bitmap mBitmap;
    private final int mBlurSizeOutline;
    private Bitmap mCrossFadeBitmap;
    float mCrossFadeProgress;
    float[] mCurrentFilter;
    final DragController mDragController;
    private final DragLayer mDragLayer;
    private Rect mDragRegion;
    private Point mDragVisualizeOffset;
    private boolean mDrawBitmap;
    private Drawable mFgSpringDrawable;
    private ValueAnimator mFilterAnimator;
    private boolean mHasDrawn;
    private final float mInitialScale;
    private float mIntrinsicIconScale;
    private int mLastTouchX;
    private int mLastTouchY;
    private final Launcher mLauncher;
    Paint mPaint;
    private final int mRegistrationX;
    private final int mRegistrationY;
    private final float mScaleOnDrop;
    private Path mScaledMaskPath;
    private final int[] mTempLoc;
    private SpringFloatValue mTranslateX;
    private SpringFloatValue mTranslateY;
    private static final ColorMatrix sTempMatrix1 = new ColorMatrix();
    private static final ColorMatrix sTempMatrix2 = new ColorMatrix();
    static float sDragAlpha = 1.0f;

    public DragView(Launcher launcher, Bitmap bitmap, int i, int i2, final float f, float f2, float f3) {
        super(launcher);
        this.mDrawBitmap = true;
        this.mTempLoc = new int[2];
        this.mDragVisualizeOffset = null;
        this.mDragRegion = null;
        this.mHasDrawn = false;
        this.mCrossFadeProgress = 0.0f;
        this.mAnimationCancelled = false;
        this.mIntrinsicIconScale = 1.0f;
        this.mLauncher = launcher;
        this.mDragLayer = launcher.getDragLayer();
        this.mDragController = launcher.getDragController();
        final float width = (bitmap.getWidth() + f3) / bitmap.getWidth();
        setScaleX(f);
        setScaleY(f);
        this.mAnim = LauncherAnimUtils.ofFloat(0.0f, 1.0f);
        this.mAnim.setDuration(150L);
        this.mAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float fFloatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                DragView.this.setScaleX(f + ((width - f) * fFloatValue));
                DragView.this.setScaleY(f + ((width - f) * fFloatValue));
                if (DragView.sDragAlpha != 1.0f) {
                    DragView.this.setAlpha((DragView.sDragAlpha * fFloatValue) + (1.0f - fFloatValue));
                }
                if (DragView.this.getParent() == null) {
                    valueAnimator.cancel();
                }
            }
        });
        this.mAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                if (!DragView.this.mAnimationCancelled) {
                    DragView.this.mDragController.onDragViewAnimationEnd();
                }
            }
        });
        this.mBitmap = bitmap;
        setDragRegion(new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()));
        this.mRegistrationX = i;
        this.mRegistrationY = i2;
        this.mInitialScale = f;
        this.mScaleOnDrop = f2;
        int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
        measure(iMakeMeasureSpec, iMakeMeasureSpec);
        this.mPaint = new Paint(2);
        this.mBlurSizeOutline = getResources().getDimensionPixelSize(R.dimen.blur_size_medium_outline);
        setElevation(getResources().getDimension(R.dimen.drag_elevation));
    }

    @TargetApi(26)
    public void setItemInfo(final ItemInfo itemInfo) {
        if (!Utilities.ATLEAST_OREO) {
            return;
        }
        if (itemInfo.itemType != 0 && itemInfo.itemType != 6 && itemInfo.itemType != 2) {
            return;
        }
        new Handler(LauncherModel.getWorkerLooper()).postAtFrontOfQueue(new Runnable() {
            @Override
            public void run() {
                LauncherAppState launcherAppState = LauncherAppState.getInstance(DragView.this.mLauncher);
                Object[] objArr = new Object[1];
                final Drawable fullDrawable = DragView.this.getFullDrawable(itemInfo, launcherAppState, objArr);
                if (fullDrawable instanceof AdaptiveIconDrawable) {
                    int width = DragView.this.mBitmap.getWidth();
                    int height = DragView.this.mBitmap.getHeight();
                    int dimension = ((int) DragView.this.mLauncher.getResources().getDimension(R.dimen.blur_size_medium_outline)) / 2;
                    Rect rect = new Rect(0, 0, width, height);
                    rect.inset(dimension, dimension);
                    Rect rect2 = new Rect(rect);
                    DragView.this.mBadge = DragView.this.getBadge(itemInfo, launcherAppState, objArr[0]);
                    DragView.this.mBadge.setBounds(rect2);
                    LauncherIcons launcherIconsObtain = LauncherIcons.obtain(DragView.this.mLauncher);
                    Utilities.scaleRectAboutCenter(rect, launcherIconsObtain.getNormalizer().getScale(fullDrawable, null, null, null));
                    launcherIconsObtain.recycle();
                    AdaptiveIconDrawable adaptiveIconDrawable = (AdaptiveIconDrawable) fullDrawable;
                    Rect rect3 = new Rect(rect);
                    Utilities.scaleRectAboutCenter(rect3, 0.98f);
                    adaptiveIconDrawable.setBounds(rect3);
                    final Path iconMask = adaptiveIconDrawable.getIconMask();
                    DragView.this.mTranslateX = new SpringFloatValue(DragView.this, width * AdaptiveIconDrawable.getExtraInsetFraction());
                    DragView.this.mTranslateY = new SpringFloatValue(DragView.this, height * AdaptiveIconDrawable.getExtraInsetFraction());
                    rect.inset((int) ((-rect.width()) * AdaptiveIconDrawable.getExtraInsetFraction()), (int) ((-rect.height()) * AdaptiveIconDrawable.getExtraInsetFraction()));
                    DragView.this.mBgSpringDrawable = adaptiveIconDrawable.getBackground();
                    if (DragView.this.mBgSpringDrawable == null) {
                        DragView.this.mBgSpringDrawable = new ColorDrawable(0);
                    }
                    DragView.this.mBgSpringDrawable.setBounds(rect);
                    DragView.this.mFgSpringDrawable = adaptiveIconDrawable.getForeground();
                    if (DragView.this.mFgSpringDrawable == null) {
                        DragView.this.mFgSpringDrawable = new ColorDrawable(0);
                    }
                    DragView.this.mFgSpringDrawable.setBounds(rect);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            DragView.this.mScaledMaskPath = iconMask;
                            DragView.this.mDrawBitmap = !(fullDrawable instanceof FolderAdaptiveIcon);
                            if (itemInfo.isDisabled()) {
                                FastBitmapDrawable fastBitmapDrawable = new FastBitmapDrawable((Bitmap) null);
                                fastBitmapDrawable.setIsDisabled(true);
                                DragView.this.mBaseFilter = (ColorMatrixColorFilter) fastBitmapDrawable.getColorFilter();
                            }
                            DragView.this.updateColorFilter();
                        }
                    });
                }
            }
        });
    }

    @TargetApi(26)
    private void updateColorFilter() {
        if (this.mCurrentFilter == null) {
            this.mPaint.setColorFilter(null);
            if (this.mScaledMaskPath != null) {
                this.mBgSpringDrawable.setColorFilter(this.mBaseFilter);
                this.mFgSpringDrawable.setColorFilter(this.mBaseFilter);
                this.mBadge.setColorFilter(this.mBaseFilter);
            }
        } else {
            ColorMatrixColorFilter colorMatrixColorFilter = new ColorMatrixColorFilter(this.mCurrentFilter);
            this.mPaint.setColorFilter(colorMatrixColorFilter);
            if (this.mScaledMaskPath != null) {
                if (this.mBaseFilter != null) {
                    this.mBaseFilter.getColorMatrix(sTempMatrix1);
                    sTempMatrix2.set(this.mCurrentFilter);
                    sTempMatrix1.postConcat(sTempMatrix2);
                    colorMatrixColorFilter = new ColorMatrixColorFilter(sTempMatrix1);
                }
                this.mBgSpringDrawable.setColorFilter(colorMatrixColorFilter);
                this.mFgSpringDrawable.setColorFilter(colorMatrixColorFilter);
                this.mBadge.setColorFilter(colorMatrixColorFilter);
            }
        }
        invalidate();
    }

    private Drawable getFullDrawable(ItemInfo itemInfo, LauncherAppState launcherAppState, Object[] objArr) {
        FolderAdaptiveIcon folderAdaptiveIconCreateFolderAdaptiveIcon;
        if (itemInfo.itemType == 0) {
            LauncherActivityInfo launcherActivityInfoResolveActivity = LauncherAppsCompat.getInstance(this.mLauncher).resolveActivity(itemInfo.getIntent(), itemInfo.user);
            objArr[0] = launcherActivityInfoResolveActivity;
            if (launcherActivityInfoResolveActivity != null) {
                return launcherAppState.getIconCache().getFullResIcon(launcherActivityInfoResolveActivity, false);
            }
            return null;
        }
        if (itemInfo.itemType == 6) {
            if (itemInfo instanceof PendingAddShortcutInfo) {
                ShortcutConfigActivityInfo shortcutConfigActivityInfo = ((PendingAddShortcutInfo) itemInfo).activityInfo;
                objArr[0] = shortcutConfigActivityInfo;
                return shortcutConfigActivityInfo.getFullResIcon(launcherAppState.getIconCache());
            }
            ShortcutKey shortcutKeyFromItemInfo = ShortcutKey.fromItemInfo(itemInfo);
            DeepShortcutManager deepShortcutManager = DeepShortcutManager.getInstance(this.mLauncher);
            List<ShortcutInfoCompat> listQueryForFullDetails = deepShortcutManager.queryForFullDetails(shortcutKeyFromItemInfo.componentName.getPackageName(), Arrays.asList(shortcutKeyFromItemInfo.getId()), shortcutKeyFromItemInfo.user);
            if (listQueryForFullDetails.isEmpty()) {
                return null;
            }
            objArr[0] = listQueryForFullDetails.get(0);
            return deepShortcutManager.getShortcutIconDrawable(listQueryForFullDetails.get(0), launcherAppState.getInvariantDeviceProfile().fillResIconDpi);
        }
        if (itemInfo.itemType != 2 || (folderAdaptiveIconCreateFolderAdaptiveIcon = FolderAdaptiveIcon.createFolderAdaptiveIcon(this.mLauncher, itemInfo.id, new Point(this.mBitmap.getWidth(), this.mBitmap.getHeight()))) == null) {
            return null;
        }
        objArr[0] = folderAdaptiveIconCreateFolderAdaptiveIcon;
        return folderAdaptiveIconCreateFolderAdaptiveIcon;
    }

    @TargetApi(26)
    private Drawable getBadge(ItemInfo itemInfo, LauncherAppState launcherAppState, Object obj) {
        int i = launcherAppState.getInvariantDeviceProfile().iconBitmapSize;
        if (itemInfo.itemType == 6) {
            boolean z = (itemInfo instanceof ItemInfoWithIcon) && (((ItemInfoWithIcon) itemInfo).runtimeStatusFlags & 512) > 0;
            if ((itemInfo.id == -1 && !z) || !(obj instanceof ShortcutInfoCompat)) {
                return new FixedSizeEmptyDrawable(i);
            }
            LauncherIcons launcherIconsObtain = LauncherIcons.obtain(launcherAppState.getContext());
            Bitmap bitmap = launcherIconsObtain.getShortcutInfoBadge((ShortcutInfoCompat) obj, launcherAppState.getIconCache()).iconBitmap;
            launcherIconsObtain.recycle();
            float f = i;
            float dimension = (f - this.mLauncher.getResources().getDimension(R.dimen.profile_badge_size)) / f;
            return new InsetDrawable(new FastBitmapDrawable(bitmap), dimension, dimension, 0.0f, 0.0f);
        }
        if (itemInfo.itemType == 2) {
            return ((FolderAdaptiveIcon) obj).getBadge();
        }
        return this.mLauncher.getPackageManager().getUserBadgedIcon(new FixedSizeEmptyDrawable(i), itemInfo.user);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        setMeasuredDimension(this.mBitmap.getWidth(), this.mBitmap.getHeight());
    }

    public void setIntrinsicIconScaleFactor(float f) {
        this.mIntrinsicIconScale = f;
    }

    public float getIntrinsicIconScaleFactor() {
        return this.mIntrinsicIconScale;
    }

    public int getDragRegionLeft() {
        return this.mDragRegion.left;
    }

    public int getDragRegionTop() {
        return this.mDragRegion.top;
    }

    public int getDragRegionWidth() {
        return this.mDragRegion.width();
    }

    public int getDragRegionHeight() {
        return this.mDragRegion.height();
    }

    public void setDragVisualizeOffset(Point point) {
        this.mDragVisualizeOffset = point;
    }

    public Point getDragVisualizeOffset() {
        return this.mDragVisualizeOffset;
    }

    public void setDragRegion(Rect rect) {
        this.mDragRegion = rect;
    }

    public Rect getDragRegion() {
        return this.mDragRegion;
    }

    public Bitmap getPreviewBitmap() {
        return this.mBitmap;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        this.mHasDrawn = true;
        if (this.mDrawBitmap) {
            boolean z = this.mCrossFadeProgress > 0.0f && this.mCrossFadeBitmap != null;
            if (z) {
                this.mPaint.setAlpha(z ? (int) ((1.0f - this.mCrossFadeProgress) * 255.0f) : 255);
            }
            canvas.drawBitmap(this.mBitmap, 0.0f, 0.0f, this.mPaint);
            if (z) {
                this.mPaint.setAlpha((int) (255.0f * this.mCrossFadeProgress));
                int iSave = canvas.save();
                canvas.scale((this.mBitmap.getWidth() * 1.0f) / this.mCrossFadeBitmap.getWidth(), (this.mBitmap.getHeight() * 1.0f) / this.mCrossFadeBitmap.getHeight());
                canvas.drawBitmap(this.mCrossFadeBitmap, 0.0f, 0.0f, this.mPaint);
                canvas.restoreToCount(iSave);
            }
        }
        if (this.mScaledMaskPath != null) {
            int iSave2 = canvas.save();
            canvas.clipPath(this.mScaledMaskPath);
            this.mBgSpringDrawable.draw(canvas);
            canvas.translate(this.mTranslateX.mValue, this.mTranslateY.mValue);
            this.mFgSpringDrawable.draw(canvas);
            canvas.restoreToCount(iSave2);
            this.mBadge.draw(canvas);
        }
    }

    public void setCrossFadeBitmap(Bitmap bitmap) {
        this.mCrossFadeBitmap = bitmap;
    }

    public void crossFade(int i) {
        ValueAnimator valueAnimatorOfFloat = LauncherAnimUtils.ofFloat(0.0f, 1.0f);
        valueAnimatorOfFloat.setDuration(i);
        valueAnimatorOfFloat.setInterpolator(Interpolators.DEACCEL_1_5);
        valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                DragView.this.mCrossFadeProgress = valueAnimator.getAnimatedFraction();
                DragView.this.invalidate();
            }
        });
        valueAnimatorOfFloat.start();
    }

    public void setColor(int i) {
        if (this.mPaint == null) {
            this.mPaint = new Paint(2);
        }
        if (i != 0) {
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0.0f);
            ColorMatrix colorMatrix2 = new ColorMatrix();
            Themes.setColorScaleOnMatrix(i, colorMatrix2);
            colorMatrix.postConcat(colorMatrix2);
            animateFilterTo(colorMatrix.getArray());
            return;
        }
        if (this.mCurrentFilter == null) {
            updateColorFilter();
        } else {
            animateFilterTo(new ColorMatrix().getArray());
        }
    }

    private void animateFilterTo(float[] fArr) {
        float[] array = this.mCurrentFilter == null ? new ColorMatrix().getArray() : this.mCurrentFilter;
        this.mCurrentFilter = Arrays.copyOf(array, array.length);
        if (this.mFilterAnimator != null) {
            this.mFilterAnimator.cancel();
        }
        this.mFilterAnimator = ValueAnimator.ofObject(new FloatArrayEvaluator(this.mCurrentFilter), array, fArr);
        this.mFilterAnimator.setDuration(120L);
        this.mFilterAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                DragView.this.updateColorFilter();
            }
        });
        this.mFilterAnimator.start();
    }

    public boolean hasDrawn() {
        return this.mHasDrawn;
    }

    @Override
    public void setAlpha(float f) {
        super.setAlpha(f);
        this.mPaint.setAlpha((int) (255.0f * f));
        invalidate();
    }

    public void show(int i, int i2) {
        this.mDragLayer.addView(this);
        BaseDragLayer.LayoutParams layoutParams = new BaseDragLayer.LayoutParams(0, 0);
        layoutParams.width = this.mBitmap.getWidth();
        layoutParams.height = this.mBitmap.getHeight();
        layoutParams.customPosition = true;
        setLayoutParams(layoutParams);
        move(i, i2);
        post(new Runnable() {
            @Override
            public void run() {
                DragView.this.mAnim.start();
            }
        });
    }

    public void cancelAnimation() {
        this.mAnimationCancelled = true;
        if (this.mAnim != null && this.mAnim.isRunning()) {
            this.mAnim.cancel();
        }
    }

    public void move(int i, int i2) {
        if (i > 0 && i2 > 0 && this.mLastTouchX > 0 && this.mLastTouchY > 0 && this.mScaledMaskPath != null) {
            this.mTranslateX.animateToPos(this.mLastTouchX - i);
            this.mTranslateY.animateToPos(this.mLastTouchY - i2);
        }
        this.mLastTouchX = i;
        this.mLastTouchY = i2;
        applyTranslation();
    }

    public void animateTo(int i, int i2, Runnable runnable, int i3) {
        this.mTempLoc[0] = i - this.mRegistrationX;
        this.mTempLoc[1] = i2 - this.mRegistrationY;
        this.mDragLayer.animateViewIntoPosition(this, this.mTempLoc, 1.0f, this.mScaleOnDrop, this.mScaleOnDrop, 0, runnable, i3);
    }

    public void animateShift(final int i, final int i2) {
        if (this.mAnim.isStarted()) {
            return;
        }
        this.mAnimatedShiftX = i;
        this.mAnimatedShiftY = i2;
        applyTranslation();
        this.mAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float animatedFraction = 1.0f - valueAnimator.getAnimatedFraction();
                DragView.this.mAnimatedShiftX = (int) (i * animatedFraction);
                DragView.this.mAnimatedShiftY = (int) (animatedFraction * i2);
                DragView.this.applyTranslation();
            }
        });
    }

    private void applyTranslation() {
        setTranslationX((this.mLastTouchX - this.mRegistrationX) + this.mAnimatedShiftX);
        setTranslationY((this.mLastTouchY - this.mRegistrationY) + this.mAnimatedShiftY);
    }

    public void remove() {
        if (getParent() != null) {
            this.mDragLayer.removeView(this);
        }
    }

    public int getBlurSizeOutline() {
        return this.mBlurSizeOutline;
    }

    public float getInitialScale() {
        return this.mInitialScale;
    }

    private static class SpringFloatValue {
        private static final float DAMPENING_RATIO = 1.0f;
        private static final int PARALLAX_MAX_IN_DP = 8;
        private static final int STIFFNESS = 4000;
        private static final FloatPropertyCompat<SpringFloatValue> VALUE = new FloatPropertyCompat<SpringFloatValue>(LauncherSettings.Settings.EXTRA_VALUE) {
            @Override
            public float getValue(SpringFloatValue springFloatValue) {
                return springFloatValue.mValue;
            }

            @Override
            public void setValue(SpringFloatValue springFloatValue, float f) {
                springFloatValue.mValue = f;
                springFloatValue.mView.invalidate();
            }
        };
        private final float mDelta;
        private final SpringAnimation mSpring;
        private float mValue;
        private final View mView;

        public SpringFloatValue(View view, float f) {
            this.mView = view;
            this.mSpring = new SpringAnimation(this, VALUE, 0.0f).setMinValue(-f).setMaxValue(f).setSpring(new SpringForce(0.0f).setDampingRatio(1.0f).setStiffness(4000.0f));
            this.mDelta = view.getResources().getDisplayMetrics().density * 8.0f;
        }

        public void animateToPos(float f) {
            this.mSpring.animateToFinalPosition(Utilities.boundToRange(f, -this.mDelta, this.mDelta));
        }
    }

    private static class FixedSizeEmptyDrawable extends ColorDrawable {
        private final int mSize;

        public FixedSizeEmptyDrawable(int i) {
            super(0);
            this.mSize = i;
        }

        @Override
        public int getIntrinsicHeight() {
            return this.mSize;
        }

        @Override
        public int getIntrinsicWidth() {
            return this.mSize;
        }
    }
}
