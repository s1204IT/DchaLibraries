package com.android.documentsui.dirlist;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import com.android.documentsui.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class DirectoryItemAnimator extends DefaultItemAnimator {
    private final Integer mDefaultColor;
    private final List<ColorAnimation> mPendingAnimations = new ArrayList();
    private final Map<RecyclerView.ViewHolder, ColorAnimation> mRunningAnimations = new ArrayMap();
    private final Integer mSelectedColor;

    public DirectoryItemAnimator(Context context) {
        this.mDefaultColor = Integer.valueOf(context.getResources().getColor(R.color.item_doc_background));
        this.mSelectedColor = Integer.valueOf(context.getResources().getColor(R.color.item_doc_background_selected));
    }

    @Override
    public void runPendingAnimations() {
        super.runPendingAnimations();
        for (ColorAnimation colorAnimation : this.mPendingAnimations) {
            colorAnimation.start();
            this.mRunningAnimations.put(colorAnimation.viewHolder, colorAnimation);
        }
        this.mPendingAnimations.clear();
    }

    @Override
    public void endAnimation(RecyclerView.ViewHolder viewHolder) {
        super.endAnimation(viewHolder);
        for (int size = this.mPendingAnimations.size() - 1; size >= 0; size--) {
            ColorAnimation colorAnimation = this.mPendingAnimations.get(size);
            if (colorAnimation.viewHolder == viewHolder) {
                this.mPendingAnimations.remove(size);
                colorAnimation.end();
            }
        }
        ColorAnimation colorAnimation2 = this.mRunningAnimations.get(viewHolder);
        if (colorAnimation2 != null) {
            colorAnimation2.cancel();
        }
    }

    @Override
    public RecyclerView.ItemAnimator.ItemHolderInfo recordPreLayoutInformation(RecyclerView.State state, RecyclerView.ViewHolder viewHolder, int i, List<Object> list) {
        ItemInfo itemInfo = (ItemInfo) super.recordPreLayoutInformation(state, viewHolder, i, list);
        itemInfo.isActivated = viewHolder.itemView.isActivated();
        return itemInfo;
    }

    @Override
    public RecyclerView.ItemAnimator.ItemHolderInfo recordPostLayoutInformation(RecyclerView.State state, RecyclerView.ViewHolder viewHolder) {
        ItemInfo itemInfo = (ItemInfo) super.recordPostLayoutInformation(state, viewHolder);
        itemInfo.isActivated = viewHolder.itemView.isActivated();
        return itemInfo;
    }

    @Override
    public RecyclerView.ItemAnimator.ItemHolderInfo obtainHolderInfo() {
        return new ItemInfo();
    }

    @Override
    public boolean canReuseUpdatedViewHolder(RecyclerView.ViewHolder viewHolder) {
        return true;
    }

    class ItemInfo extends RecyclerView.ItemAnimator.ItemHolderInfo {
        boolean isActivated;

        ItemInfo() {
        }
    }

    class ColorAnimation implements Animator.AnimatorListener, ValueAnimator.AnimatorUpdateListener {
        int mEndColor;
        ValueAnimator mValueAnimator;
        final DirectoryItemAnimator this$0;
        final RecyclerView.ViewHolder viewHolder;

        public void start() {
            this.mValueAnimator.start();
        }

        public void cancel() {
            this.mValueAnimator.cancel();
        }

        public void end() {
            this.mValueAnimator.end();
        }

        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            this.viewHolder.itemView.setBackgroundColor(((Integer) valueAnimator.getAnimatedValue()).intValue());
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            this.viewHolder.itemView.setBackgroundColor(this.mEndColor);
            this.this$0.mRunningAnimations.remove(this.viewHolder);
            this.this$0.dispatchAnimationFinished(this.viewHolder);
        }

        @Override
        public void onAnimationStart(Animator animator) {
            this.this$0.dispatchAnimationStarted(this.viewHolder);
        }

        @Override
        public void onAnimationCancel(Animator animator) {
        }

        @Override
        public void onAnimationRepeat(Animator animator) {
        }
    }
}
