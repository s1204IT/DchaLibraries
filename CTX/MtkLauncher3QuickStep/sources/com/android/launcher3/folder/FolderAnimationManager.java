package com.android.launcher3.folder;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.graphics.ColorUtils;
import android.util.Property;
import android.view.View;
import android.view.animation.AnimationUtils;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAnimUtils;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutAndWidgetContainer;
import com.android.launcher3.Utilities;
import com.android.launcher3.anim.PropertyResetListener;
import com.android.launcher3.anim.RoundedRectRevealOutlineProvider;
import com.android.launcher3.util.Themes;
import com.android.launcher3.views.BaseDragLayer;
import java.util.Iterator;
import java.util.List;

public class FolderAnimationManager {
    private FolderPagedView mContent;
    private Context mContext;
    private final int mDelay;
    private final int mDuration;
    private Folder mFolder;
    private GradientDrawable mFolderBackground;
    private FolderIcon mFolderIcon;
    private final TimeInterpolator mFolderInterpolator;
    private final boolean mIsOpening;
    private final TimeInterpolator mLargeFolderPreviewItemCloseInterpolator;
    private final TimeInterpolator mLargeFolderPreviewItemOpenInterpolator;
    private Launcher mLauncher;
    private PreviewBackground mPreviewBackground;
    private final PreviewItemDrawingParams mTmpParams = new PreviewItemDrawingParams(0.0f, 0.0f, 0.0f, 0.0f);

    public FolderAnimationManager(Folder folder, boolean z) {
        this.mFolder = folder;
        this.mContent = folder.mContent;
        this.mFolderBackground = (GradientDrawable) this.mFolder.getBackground();
        this.mFolderIcon = folder.mFolderIcon;
        this.mPreviewBackground = this.mFolderIcon.mBackground;
        this.mContext = folder.getContext();
        this.mLauncher = folder.mLauncher;
        this.mIsOpening = z;
        Resources resources = this.mContent.getResources();
        this.mDuration = resources.getInteger(R.integer.config_materialFolderExpandDuration);
        this.mDelay = resources.getInteger(R.integer.config_folderDelay);
        this.mFolderInterpolator = AnimationUtils.loadInterpolator(this.mContext, R.interpolator.folder_interpolator);
        this.mLargeFolderPreviewItemOpenInterpolator = AnimationUtils.loadInterpolator(this.mContext, R.interpolator.large_folder_preview_item_open_interpolator);
        this.mLargeFolderPreviewItemCloseInterpolator = AnimationUtils.loadInterpolator(this.mContext, R.interpolator.large_folder_preview_item_close_interpolator);
    }

    public AnimatorSet getAnimator() {
        BaseDragLayer.LayoutParams layoutParams = (BaseDragLayer.LayoutParams) this.mFolder.getLayoutParams();
        ClippedFolderIconLayoutRule layoutRule = this.mFolderIcon.getLayoutRule();
        List<BubbleTextView> previewItems = this.mFolderIcon.getPreviewItems();
        Rect rect = new Rect();
        float descendantRectRelativeToSelf = this.mLauncher.getDragLayer().getDescendantRectRelativeToSelf(this.mFolderIcon, rect);
        int scaledRadius = this.mPreviewBackground.getScaledRadius();
        float f = scaledRadius * 2 * descendantRectRelativeToSelf;
        float iconSize = layoutRule.getIconSize() * layoutRule.scaleForItem(previewItems.size());
        float iconSize2 = (iconSize / previewItems.get(0).getIconSize()) * descendantRectRelativeToSelf;
        float f2 = this.mIsOpening ? iconSize2 : 1.0f;
        this.mFolder.setScaleX(f2);
        this.mFolder.setScaleY(f2);
        this.mFolder.setPivotX(0.0f);
        this.mFolder.setPivotY(0.0f);
        int i = (int) (iconSize / 2.0f);
        if (Utilities.isRtl(this.mContext.getResources())) {
            i = (int) (((layoutParams.width * iconSize2) - f) - i);
        }
        int i2 = i;
        int paddingLeft = (int) ((this.mFolder.getPaddingLeft() + this.mContent.getPaddingLeft()) * iconSize2);
        int paddingTop = (int) ((this.mFolder.getPaddingTop() + this.mContent.getPaddingTop()) * iconSize2);
        int offsetX = ((rect.left + this.mPreviewBackground.getOffsetX()) - paddingLeft) - i2;
        int offsetY = (rect.top + this.mPreviewBackground.getOffsetY()) - paddingTop;
        float f3 = offsetX - layoutParams.x;
        float f4 = offsetY - layoutParams.y;
        int attrColor = Themes.getAttrColor(this.mContext, android.R.attr.colorPrimary);
        int alphaComponent = ColorUtils.setAlphaComponent(attrColor, this.mPreviewBackground.getBackgroundAlpha());
        this.mFolderBackground.setColor(this.mIsOpening ? alphaComponent : attrColor);
        float f5 = paddingLeft + i2;
        float f6 = paddingTop;
        Rect rect2 = new Rect(Math.round(f5 / iconSize2), Math.round(f6 / iconSize2), Math.round((f5 + f) / iconSize2), Math.round((f6 + f) / iconSize2));
        Rect rect3 = new Rect(0, 0, layoutParams.width, layoutParams.height);
        float f7 = (f / iconSize2) / 2.0f;
        float fPxFromDp = Utilities.pxFromDp(2.0f, this.mContext.getResources().getDisplayMetrics());
        AnimatorSet animatorSetCreateAnimatorSet = LauncherAnimUtils.createAnimatorSet();
        PropertyResetListener propertyResetListener = new PropertyResetListener(BubbleTextView.TEXT_ALPHA_PROPERTY, Float.valueOf(1.0f));
        for (BubbleTextView bubbleTextView : this.mFolder.getItemsOnPage(this.mFolder.mContent.getCurrentPage())) {
            if (this.mIsOpening) {
                bubbleTextView.setTextVisibility(false);
            }
            ObjectAnimator objectAnimatorCreateTextAlphaAnimator = bubbleTextView.createTextAlphaAnimator(this.mIsOpening);
            objectAnimatorCreateTextAlphaAnimator.addListener(propertyResetListener);
            play(animatorSetCreateAnimatorSet, objectAnimatorCreateTextAlphaAnimator);
        }
        play(animatorSetCreateAnimatorSet, getAnimator(this.mFolder, View.TRANSLATION_X, f3, 0.0f));
        play(animatorSetCreateAnimatorSet, getAnimator(this.mFolder, View.TRANSLATION_Y, f4, 0.0f));
        play(animatorSetCreateAnimatorSet, getAnimator(this.mFolder, LauncherAnimUtils.SCALE_PROPERTY, iconSize2, 1.0f));
        play(animatorSetCreateAnimatorSet, getAnimator(this.mFolderBackground, "color", alphaComponent, attrColor));
        play(animatorSetCreateAnimatorSet, this.mFolderIcon.mFolderName.createTextAlphaAnimator(!this.mIsOpening));
        play(animatorSetCreateAnimatorSet, new RoundedRectRevealOutlineProvider(f7, fPxFromDp, rect2, rect3) {
            @Override
            public boolean shouldRemoveElevationDuringAnimation() {
                return true;
            }
        }.createRevealAnimator(this.mFolder, !this.mIsOpening));
        int i3 = this.mDuration / 2;
        play(animatorSetCreateAnimatorSet, getAnimator(this.mFolder, View.TRANSLATION_Z, -this.mFolder.getElevation(), 0.0f), this.mIsOpening ? i3 : 0L, i3);
        animatorSetCreateAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                super.onAnimationEnd(animator);
                FolderAnimationManager.this.mFolder.setTranslationX(0.0f);
                FolderAnimationManager.this.mFolder.setTranslationY(0.0f);
                FolderAnimationManager.this.mFolder.setTranslationZ(0.0f);
                FolderAnimationManager.this.mFolder.setScaleX(1.0f);
                FolderAnimationManager.this.mFolder.setScaleY(1.0f);
            }
        });
        Iterator<Animator> it = animatorSetCreateAnimatorSet.getChildAnimations().iterator();
        while (it.hasNext()) {
            it.next().setInterpolator(this.mFolderInterpolator);
        }
        int radius = scaledRadius - this.mPreviewBackground.getRadius();
        addPreviewItemAnimators(animatorSetCreateAnimatorSet, iconSize2 / descendantRectRelativeToSelf, i2 + radius, radius);
        return animatorSetCreateAnimatorSet;
    }

    private void addPreviewItemAnimators(AnimatorSet animatorSet, float f, int i, int i2) {
        List<BubbleTextView> previewItemsOnPage;
        List<BubbleTextView> list;
        int i3;
        ClippedFolderIconLayoutRule layoutRule = this.mFolderIcon.getLayoutRule();
        boolean z = true;
        boolean z2 = this.mFolder.mContent.getCurrentPage() == 0;
        if (z2) {
            previewItemsOnPage = this.mFolderIcon.getPreviewItems();
        } else {
            previewItemsOnPage = this.mFolderIcon.getPreviewItemsOnPage(this.mFolder.mContent.getCurrentPage());
        }
        List<BubbleTextView> list2 = previewItemsOnPage;
        int size = list2.size();
        int i4 = z2 ? size : 4;
        TimeInterpolator previewItemInterpolator = getPreviewItemInterpolator();
        ShortcutAndWidgetContainer shortcutsAndWidgets = this.mContent.getPageAt(0).getShortcutsAndWidgets();
        int i5 = 0;
        while (i5 < size) {
            final BubbleTextView bubbleTextView = list2.get(i5);
            CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) bubbleTextView.getLayoutParams();
            layoutParams.isLockedToGrid = z;
            shortcutsAndWidgets.setupLp(bubbleTextView);
            final float iconSize = ((layoutRule.getIconSize() * layoutRule.scaleForItem(i4)) / list2.get(i5).getIconSize()) / f;
            float f2 = this.mIsOpening ? iconSize : 1.0f;
            bubbleTextView.setScaleX(f2);
            bubbleTextView.setScaleY(f2);
            layoutRule.computePreviewItemDrawingParams(i5, i4, this.mTmpParams);
            int iconSize2 = (int) (((this.mTmpParams.transX - (((int) ((layoutParams.width - bubbleTextView.getIconSize()) * r3)) / 2)) + i) / f);
            ClippedFolderIconLayoutRule clippedFolderIconLayoutRule = layoutRule;
            int i6 = (int) ((this.mTmpParams.transY + i2) / f);
            final float f3 = iconSize2 - layoutParams.x;
            final float f4 = i6 - layoutParams.y;
            Animator animator = getAnimator(bubbleTextView, View.TRANSLATION_X, f3, 0.0f);
            animator.setInterpolator(previewItemInterpolator);
            play(animatorSet, animator);
            int i7 = i5;
            Animator animator2 = getAnimator(bubbleTextView, View.TRANSLATION_Y, f4, 0.0f);
            animator2.setInterpolator(previewItemInterpolator);
            play(animatorSet, animator2);
            ShortcutAndWidgetContainer shortcutAndWidgetContainer = shortcutsAndWidgets;
            Animator animator3 = getAnimator(bubbleTextView, LauncherAnimUtils.SCALE_PROPERTY, iconSize, 1.0f);
            animator3.setInterpolator(previewItemInterpolator);
            play(animatorSet, animator3);
            if (this.mFolder.getItemCount() > 4) {
                int i8 = this.mIsOpening ? this.mDelay : this.mDelay * 2;
                if (this.mIsOpening) {
                    long j = i8;
                    animator.setStartDelay(j);
                    animator2.setStartDelay(j);
                    animator3.setStartDelay(j);
                }
                list = list2;
                i3 = size;
                long j2 = i8;
                animator.setDuration(animator.getDuration() - j2);
                animator2.setDuration(animator2.getDuration() - j2);
                animator3.setDuration(animator3.getDuration() - j2);
            } else {
                list = list2;
                i3 = size;
            }
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animator4) {
                    super.onAnimationStart(animator4);
                    if (FolderAnimationManager.this.mIsOpening) {
                        bubbleTextView.setTranslationX(f3);
                        bubbleTextView.setTranslationY(f4);
                        bubbleTextView.setScaleX(iconSize);
                        bubbleTextView.setScaleY(iconSize);
                    }
                }

                @Override
                public void onAnimationEnd(Animator animator4) {
                    super.onAnimationEnd(animator4);
                    bubbleTextView.setTranslationX(0.0f);
                    bubbleTextView.setTranslationY(0.0f);
                    bubbleTextView.setScaleX(1.0f);
                    bubbleTextView.setScaleY(1.0f);
                }
            });
            i5 = i7 + 1;
            shortcutsAndWidgets = shortcutAndWidgetContainer;
            layoutRule = clippedFolderIconLayoutRule;
            list2 = list;
            size = i3;
            z = true;
        }
    }

    private void play(AnimatorSet animatorSet, Animator animator) {
        play(animatorSet, animator, animator.getStartDelay(), this.mDuration);
    }

    private void play(AnimatorSet animatorSet, Animator animator, long j, int i) {
        animator.setStartDelay(j);
        animator.setDuration(i);
        animatorSet.play(animator);
    }

    private TimeInterpolator getPreviewItemInterpolator() {
        if (this.mFolder.getItemCount() > 4) {
            if (this.mIsOpening) {
                return this.mLargeFolderPreviewItemOpenInterpolator;
            }
            return this.mLargeFolderPreviewItemCloseInterpolator;
        }
        return this.mFolderInterpolator;
    }

    private Animator getAnimator(View view, Property property, float f, float f2) {
        return this.mIsOpening ? ObjectAnimator.ofFloat(view, (Property<View, Float>) property, f, f2) : ObjectAnimator.ofFloat(view, (Property<View, Float>) property, f2, f);
    }

    private Animator getAnimator(GradientDrawable gradientDrawable, String str, int i, int i2) {
        return this.mIsOpening ? ObjectAnimator.ofArgb(gradientDrawable, str, i, i2) : ObjectAnimator.ofArgb(gradientDrawable, str, i2, i);
    }
}
