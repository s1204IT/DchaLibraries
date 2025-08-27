package com.android.launcher3.folder;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.Utilities;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class PreviewItemManager {
    private static final int FINAL_ITEM_ANIMATION_DURATION = 200;
    static final int INITIAL_ITEM_ANIMATION_DURATION = 350;
    private static final int ITEM_SLIDE_IN_OUT_DISTANCE_PX = 200;
    private static final int SLIDE_IN_FIRST_PAGE_ANIMATION_DURATION = 300;
    private static final int SLIDE_IN_FIRST_PAGE_ANIMATION_DURATION_DELAY = 100;
    private FolderIcon mIcon;
    private boolean mShouldSlideInFirstPage;
    private float mIntrinsicIconSize = -1.0f;
    private int mTotalWidth = -1;
    private int mPrevTopPadding = -1;
    private Drawable mReferenceDrawable = null;
    private ArrayList<PreviewItemDrawingParams> mFirstPageParams = new ArrayList<>();
    private ArrayList<PreviewItemDrawingParams> mCurrentPageParams = new ArrayList<>();
    private float mCurrentPageItemsTransX = 0.0f;

    public PreviewItemManager(FolderIcon folderIcon) {
        this.mIcon = folderIcon;
    }

    public FolderPreviewItemAnim createFirstItemAnimation(boolean z, Runnable runnable) {
        if (z) {
            return new FolderPreviewItemAnim(this, this.mFirstPageParams.get(0), 0, 2, -1, -1, 200, runnable);
        }
        return new FolderPreviewItemAnim(this, this.mFirstPageParams.get(0), -1, -1, 0, 2, INITIAL_ITEM_ANIMATION_DURATION, runnable);
    }

    Drawable prepareCreateAnimation(View view) {
        Drawable drawable = ((TextView) view).getCompoundDrawables()[1];
        computePreviewDrawingParams(drawable.getIntrinsicWidth(), view.getMeasuredWidth());
        this.mReferenceDrawable = drawable;
        return drawable;
    }

    public void recomputePreviewDrawingParams() {
        if (this.mReferenceDrawable != null) {
            computePreviewDrawingParams(this.mReferenceDrawable.getIntrinsicWidth(), this.mIcon.getMeasuredWidth());
        }
    }

    private void computePreviewDrawingParams(int i, int i2) {
        float f = i;
        if (this.mIntrinsicIconSize != f || this.mTotalWidth != i2 || this.mPrevTopPadding != this.mIcon.getPaddingTop()) {
            this.mIntrinsicIconSize = f;
            this.mTotalWidth = i2;
            this.mPrevTopPadding = this.mIcon.getPaddingTop();
            this.mIcon.mBackground.setup(this.mIcon.mLauncher, this.mIcon, this.mTotalWidth, this.mIcon.getPaddingTop());
            this.mIcon.mPreviewLayoutRule.init(this.mIcon.mBackground.previewSize, this.mIntrinsicIconSize, Utilities.isRtl(this.mIcon.getResources()));
            updatePreviewItems(false);
        }
    }

    PreviewItemDrawingParams computePreviewItemDrawingParams(int i, int i2, PreviewItemDrawingParams previewItemDrawingParams) {
        if (i == -1) {
            return getFinalIconParams(previewItemDrawingParams);
        }
        return this.mIcon.mPreviewLayoutRule.computePreviewItemDrawingParams(i, i2, previewItemDrawingParams);
    }

    private PreviewItemDrawingParams getFinalIconParams(PreviewItemDrawingParams previewItemDrawingParams) {
        float f = this.mIcon.mLauncher.getDeviceProfile().iconSizePx;
        float f2 = (this.mIcon.mBackground.previewSize - f) / 2.0f;
        previewItemDrawingParams.update(f2, f2, f / this.mReferenceDrawable.getIntrinsicWidth());
        return previewItemDrawingParams;
    }

    public void drawParams(Canvas canvas, ArrayList<PreviewItemDrawingParams> arrayList, float f) {
        canvas.translate(f, 0.0f);
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            PreviewItemDrawingParams previewItemDrawingParams = arrayList.get(size);
            if (!previewItemDrawingParams.hidden) {
                drawPreviewItem(canvas, previewItemDrawingParams);
            }
        }
        canvas.translate(-f, 0.0f);
    }

    public void draw(Canvas canvas) {
        float f;
        PreviewBackground folderBackground = this.mIcon.getFolderBackground();
        canvas.translate(folderBackground.basePreviewOffsetX, folderBackground.basePreviewOffsetY);
        if (this.mShouldSlideInFirstPage) {
            drawParams(canvas, this.mCurrentPageParams, this.mCurrentPageItemsTransX);
            f = (-200.0f) + this.mCurrentPageItemsTransX;
        } else {
            f = 0.0f;
        }
        drawParams(canvas, this.mFirstPageParams, f);
        canvas.translate(-folderBackground.basePreviewOffsetX, -folderBackground.basePreviewOffsetY);
    }

    public void onParamsChanged() {
        this.mIcon.invalidate();
    }

    private void drawPreviewItem(Canvas canvas, PreviewItemDrawingParams previewItemDrawingParams) {
        canvas.save();
        canvas.translate(previewItemDrawingParams.transX, previewItemDrawingParams.transY);
        canvas.scale(previewItemDrawingParams.scale, previewItemDrawingParams.scale);
        Drawable drawable = previewItemDrawingParams.drawable;
        if (drawable != null) {
            Rect bounds = drawable.getBounds();
            canvas.save();
            canvas.translate(-bounds.left, -bounds.top);
            canvas.scale(this.mIntrinsicIconSize / bounds.width(), this.mIntrinsicIconSize / bounds.height());
            drawable.draw(canvas);
            canvas.restore();
        }
        canvas.restore();
    }

    public void hidePreviewItem(int i, boolean z) {
        int iMax = i + Math.max(this.mFirstPageParams.size() - 4, 0);
        PreviewItemDrawingParams previewItemDrawingParams = iMax < this.mFirstPageParams.size() ? this.mFirstPageParams.get(iMax) : null;
        if (previewItemDrawingParams != null) {
            previewItemDrawingParams.hidden = z;
        }
    }

    void buildParamsForPage(int i, ArrayList<PreviewItemDrawingParams> arrayList, boolean z) {
        char c;
        PreviewItemManager previewItemManager = this;
        List<BubbleTextView> previewItemsOnPage = previewItemManager.mIcon.getPreviewItemsOnPage(i);
        int size = arrayList.size();
        while (true) {
            c = 1;
            if (previewItemsOnPage.size() >= arrayList.size()) {
                break;
            } else {
                arrayList.remove(arrayList.size() - 1);
            }
        }
        while (previewItemsOnPage.size() > arrayList.size()) {
            arrayList.add(new PreviewItemDrawingParams(0.0f, 0.0f, 0.0f, 0.0f));
        }
        int size2 = i == 0 ? previewItemsOnPage.size() : 4;
        int i2 = 0;
        while (i2 < arrayList.size()) {
            PreviewItemDrawingParams previewItemDrawingParams = arrayList.get(i2);
            previewItemDrawingParams.drawable = previewItemsOnPage.get(i2).getCompoundDrawables()[c];
            if (previewItemDrawingParams.drawable != null && !previewItemManager.mIcon.mFolder.isOpen()) {
                previewItemDrawingParams.drawable.setCallback(previewItemManager.mIcon);
            }
            if (!z) {
                previewItemManager.computePreviewItemDrawingParams(i2, size2, previewItemDrawingParams);
                if (previewItemManager.mReferenceDrawable == null) {
                    previewItemManager.mReferenceDrawable = previewItemDrawingParams.drawable;
                }
            } else {
                FolderPreviewItemAnim folderPreviewItemAnim = new FolderPreviewItemAnim(previewItemManager, previewItemDrawingParams, i2, size, i2, size2, 400, null);
                if (previewItemDrawingParams.anim != null) {
                    if (!previewItemDrawingParams.anim.hasEqualFinalState(folderPreviewItemAnim)) {
                        previewItemDrawingParams.anim.cancel();
                        previewItemDrawingParams.anim = folderPreviewItemAnim;
                        previewItemDrawingParams.anim.start();
                    }
                } else {
                    previewItemDrawingParams.anim = folderPreviewItemAnim;
                    previewItemDrawingParams.anim.start();
                }
            }
            i2++;
            previewItemManager = this;
            c = 1;
        }
    }

    void onFolderClose(int i) {
        this.mShouldSlideInFirstPage = i != 0;
        if (this.mShouldSlideInFirstPage) {
            this.mCurrentPageItemsTransX = 0.0f;
            buildParamsForPage(i, this.mCurrentPageParams, false);
            onParamsChanged();
            ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 200.0f);
            valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.launcher3.folder.PreviewItemManager.1
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    PreviewItemManager.this.mCurrentPageItemsTransX = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                    PreviewItemManager.this.onParamsChanged();
                }
            });
            valueAnimatorOfFloat.addListener(new AnimatorListenerAdapter() { // from class: com.android.launcher3.folder.PreviewItemManager.2
                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animator) {
                    PreviewItemManager.this.mCurrentPageParams.clear();
                }
            });
            valueAnimatorOfFloat.setStartDelay(100L);
            valueAnimatorOfFloat.setDuration(300L);
            valueAnimatorOfFloat.start();
        }
    }

    void updatePreviewItems(boolean z) {
        buildParamsForPage(0, this.mFirstPageParams, z);
    }

    boolean verifyDrawable(@NonNull Drawable drawable) {
        for (int i = 0; i < this.mFirstPageParams.size(); i++) {
            if (this.mFirstPageParams.get(i).drawable == drawable) {
                return true;
            }
        }
        return false;
    }

    float getIntrinsicIconSize() {
        return this.mIntrinsicIconSize;
    }

    public void onDrop(List<BubbleTextView> list, List<BubbleTextView> list2, ShortcutInfo shortcutInfo) {
        int size = list2.size();
        ArrayList<PreviewItemDrawingParams> arrayList = this.mFirstPageParams;
        buildParamsForPage(0, arrayList, false);
        ArrayList arrayList2 = new ArrayList();
        for (BubbleTextView bubbleTextView : list2) {
            if (!list.contains(bubbleTextView) && !bubbleTextView.getTag().equals(shortcutInfo)) {
                arrayList2.add(bubbleTextView);
            }
        }
        for (int i = 0; i < arrayList2.size(); i++) {
            int iIndexOf = list2.indexOf(arrayList2.get(i));
            PreviewItemDrawingParams previewItemDrawingParams = arrayList.get(iIndexOf);
            computePreviewItemDrawingParams(iIndexOf, size, previewItemDrawingParams);
            updateTransitionParam(previewItemDrawingParams, (BubbleTextView) arrayList2.get(i), -3, list2.indexOf(arrayList2.get(i)), size);
        }
        for (int i2 = 0; i2 < list2.size(); i2++) {
            int iIndexOf2 = list.indexOf(list2.get(i2));
            if (iIndexOf2 >= 0 && i2 != iIndexOf2) {
                updateTransitionParam(arrayList.get(i2), list2.get(i2), iIndexOf2, i2, size);
            }
        }
        ArrayList arrayList3 = new ArrayList(list);
        arrayList3.removeAll(list2);
        for (int i3 = 0; i3 < arrayList3.size(); i3++) {
            BubbleTextView bubbleTextView2 = (BubbleTextView) arrayList3.get(i3);
            int iIndexOf3 = list.indexOf(bubbleTextView2);
            PreviewItemDrawingParams previewItemDrawingParamsComputePreviewItemDrawingParams = computePreviewItemDrawingParams(iIndexOf3, size, null);
            updateTransitionParam(previewItemDrawingParamsComputePreviewItemDrawingParams, bubbleTextView2, iIndexOf3, -2, size);
            arrayList.add(0, previewItemDrawingParamsComputePreviewItemDrawingParams);
        }
        for (int i4 = 0; i4 < arrayList.size(); i4++) {
            if (arrayList.get(i4).anim != null) {
                arrayList.get(i4).anim.start();
            }
        }
    }

    private void updateTransitionParam(PreviewItemDrawingParams previewItemDrawingParams, BubbleTextView bubbleTextView, int i, int i2, int i3) {
        previewItemDrawingParams.drawable = bubbleTextView.getCompoundDrawables()[1];
        if (!this.mIcon.mFolder.isOpen()) {
            previewItemDrawingParams.drawable.setCallback(this.mIcon);
        }
        FolderPreviewItemAnim folderPreviewItemAnim = new FolderPreviewItemAnim(this, previewItemDrawingParams, i, i3, i2, i3, 400, null);
        if (previewItemDrawingParams.anim != null && !previewItemDrawingParams.anim.hasEqualFinalState(folderPreviewItemAnim)) {
            previewItemDrawingParams.anim.cancel();
        }
        previewItemDrawingParams.anim = folderPreviewItemAnim;
    }
}
