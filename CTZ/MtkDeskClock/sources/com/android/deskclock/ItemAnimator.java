package com.android.deskclock;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.Property;
import android.view.View;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ItemAnimator extends SimpleItemAnimator {
    private final List<Animator> mAddAnimatorsList = new ArrayList();
    private final List<Animator> mRemoveAnimatorsList = new ArrayList();
    private final List<Animator> mChangeAnimatorsList = new ArrayList();
    private final List<Animator> mMoveAnimatorsList = new ArrayList();
    private final Map<RecyclerView.ViewHolder, Animator> mAnimators = new ArrayMap();

    public interface OnAnimateChangeListener {
        Animator onAnimateChange(RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder viewHolder2, long j);

        Animator onAnimateChange(List<Object> list, int i, int i2, int i3, int i4, long j);
    }

    @Override
    public boolean animateRemove(final RecyclerView.ViewHolder viewHolder) {
        endAnimation(viewHolder);
        final float alpha = viewHolder.itemView.getAlpha();
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(viewHolder.itemView, (Property<View, Float>) View.ALPHA, 0.0f);
        objectAnimatorOfFloat.setDuration(getRemoveDuration());
        objectAnimatorOfFloat.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                ItemAnimator.this.dispatchRemoveStarting(viewHolder);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                animator.removeAllListeners();
                ItemAnimator.this.mAnimators.remove(viewHolder);
                viewHolder.itemView.setAlpha(alpha);
                ItemAnimator.this.dispatchRemoveFinished(viewHolder);
            }
        });
        this.mRemoveAnimatorsList.add(objectAnimatorOfFloat);
        this.mAnimators.put(viewHolder, objectAnimatorOfFloat);
        return true;
    }

    @Override
    public boolean animateAdd(final RecyclerView.ViewHolder viewHolder) {
        endAnimation(viewHolder);
        final float alpha = viewHolder.itemView.getAlpha();
        viewHolder.itemView.setAlpha(0.0f);
        ObjectAnimator duration = ObjectAnimator.ofFloat(viewHolder.itemView, (Property<View, Float>) View.ALPHA, 1.0f).setDuration(getAddDuration());
        duration.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                ItemAnimator.this.dispatchAddStarting(viewHolder);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                animator.removeAllListeners();
                ItemAnimator.this.mAnimators.remove(viewHolder);
                viewHolder.itemView.setAlpha(alpha);
                ItemAnimator.this.dispatchAddFinished(viewHolder);
            }
        });
        this.mAddAnimatorsList.add(duration);
        this.mAnimators.put(viewHolder, duration);
        return true;
    }

    @Override
    public boolean animateMove(final RecyclerView.ViewHolder viewHolder, int i, int i2, int i3, int i4) {
        ObjectAnimator objectAnimatorOfPropertyValuesHolder;
        endAnimation(viewHolder);
        int i5 = i3 - i;
        int i6 = i4 - i2;
        long moveDuration = getMoveDuration();
        if (i5 == 0 && i6 == 0) {
            dispatchMoveFinished(viewHolder);
            return false;
        }
        final View view = viewHolder.itemView;
        final float translationX = view.getTranslationX();
        final float translationY = view.getTranslationY();
        view.setTranslationX(-i5);
        view.setTranslationY(-i6);
        if (i5 != 0 && i6 != 0) {
            objectAnimatorOfPropertyValuesHolder = ObjectAnimator.ofPropertyValuesHolder(viewHolder.itemView, PropertyValuesHolder.ofFloat((Property<?, Float>) View.TRANSLATION_X, 0.0f), PropertyValuesHolder.ofFloat((Property<?, Float>) View.TRANSLATION_Y, 0.0f));
        } else if (i5 != 0) {
            objectAnimatorOfPropertyValuesHolder = ObjectAnimator.ofPropertyValuesHolder(viewHolder.itemView, PropertyValuesHolder.ofFloat((Property<?, Float>) View.TRANSLATION_X, 0.0f));
        } else {
            objectAnimatorOfPropertyValuesHolder = ObjectAnimator.ofPropertyValuesHolder(viewHolder.itemView, PropertyValuesHolder.ofFloat((Property<?, Float>) View.TRANSLATION_Y, 0.0f));
        }
        objectAnimatorOfPropertyValuesHolder.setDuration(moveDuration);
        objectAnimatorOfPropertyValuesHolder.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);
        objectAnimatorOfPropertyValuesHolder.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                ItemAnimator.this.dispatchMoveStarting(viewHolder);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                animator.removeAllListeners();
                ItemAnimator.this.mAnimators.remove(viewHolder);
                view.setTranslationX(translationX);
                view.setTranslationY(translationY);
                ItemAnimator.this.dispatchMoveFinished(viewHolder);
            }
        });
        this.mMoveAnimatorsList.add(objectAnimatorOfPropertyValuesHolder);
        this.mAnimators.put(viewHolder, objectAnimatorOfPropertyValuesHolder);
        return true;
    }

    @Override
    public boolean animateChange(@NonNull final RecyclerView.ViewHolder viewHolder, @NonNull final RecyclerView.ViewHolder viewHolder2, @NonNull RecyclerView.ItemAnimator.ItemHolderInfo itemHolderInfo, @NonNull RecyclerView.ItemAnimator.ItemHolderInfo itemHolderInfo2) {
        endAnimation(viewHolder);
        endAnimation(viewHolder2);
        long changeDuration = getChangeDuration();
        List<Object> payloads = itemHolderInfo instanceof PayloadItemHolderInfo ? ((PayloadItemHolderInfo) itemHolderInfo).getPayloads() : null;
        if (viewHolder == viewHolder2) {
            Animator animatorOnAnimateChange = ((OnAnimateChangeListener) viewHolder2).onAnimateChange(payloads, itemHolderInfo.left, itemHolderInfo.top, itemHolderInfo.right, itemHolderInfo.bottom, changeDuration);
            if (animatorOnAnimateChange == null) {
                dispatchChangeFinished(viewHolder2, false);
                return false;
            }
            animatorOnAnimateChange.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animator) {
                    ItemAnimator.this.dispatchChangeStarting(viewHolder2, false);
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    animator.removeAllListeners();
                    ItemAnimator.this.mAnimators.remove(viewHolder2);
                    ItemAnimator.this.dispatchChangeFinished(viewHolder2, false);
                }
            });
            this.mChangeAnimatorsList.add(animatorOnAnimateChange);
            this.mAnimators.put(viewHolder2, animatorOnAnimateChange);
            return true;
        }
        if (!(viewHolder instanceof OnAnimateChangeListener) || !(viewHolder2 instanceof OnAnimateChangeListener)) {
            dispatchChangeFinished(viewHolder, true);
            dispatchChangeFinished(viewHolder2, true);
            return false;
        }
        Animator animatorOnAnimateChange2 = ((OnAnimateChangeListener) viewHolder).onAnimateChange(viewHolder, viewHolder2, changeDuration);
        if (animatorOnAnimateChange2 != null) {
            animatorOnAnimateChange2.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animator) {
                    ItemAnimator.this.dispatchChangeStarting(viewHolder, true);
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    animator.removeAllListeners();
                    ItemAnimator.this.mAnimators.remove(viewHolder);
                    ItemAnimator.this.dispatchChangeFinished(viewHolder, true);
                }
            });
            this.mAnimators.put(viewHolder, animatorOnAnimateChange2);
            this.mChangeAnimatorsList.add(animatorOnAnimateChange2);
        } else {
            dispatchChangeFinished(viewHolder, true);
        }
        Animator animatorOnAnimateChange3 = ((OnAnimateChangeListener) viewHolder2).onAnimateChange(viewHolder, viewHolder2, changeDuration);
        if (animatorOnAnimateChange3 != null) {
            animatorOnAnimateChange3.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animator) {
                    ItemAnimator.this.dispatchChangeStarting(viewHolder2, false);
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    animator.removeAllListeners();
                    ItemAnimator.this.mAnimators.remove(viewHolder2);
                    ItemAnimator.this.dispatchChangeFinished(viewHolder2, false);
                }
            });
            this.mAnimators.put(viewHolder2, animatorOnAnimateChange3);
            this.mChangeAnimatorsList.add(animatorOnAnimateChange3);
        } else {
            dispatchChangeFinished(viewHolder2, false);
        }
        return true;
    }

    @Override
    public boolean animateChange(RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder viewHolder2, int i, int i2, int i3, int i4) {
        throw new IllegalStateException("This method should not be used");
    }

    @Override
    public void runPendingAnimations() {
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(this.mRemoveAnimatorsList);
        this.mRemoveAnimatorsList.clear();
        AnimatorSet animatorSet2 = new AnimatorSet();
        animatorSet2.playTogether(this.mAddAnimatorsList);
        this.mAddAnimatorsList.clear();
        AnimatorSet animatorSet3 = new AnimatorSet();
        animatorSet3.playTogether(this.mChangeAnimatorsList);
        this.mChangeAnimatorsList.clear();
        AnimatorSet animatorSet4 = new AnimatorSet();
        animatorSet4.playTogether(this.mMoveAnimatorsList);
        this.mMoveAnimatorsList.clear();
        AnimatorSet animatorSet5 = new AnimatorSet();
        animatorSet5.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                animator.removeAllListeners();
                ItemAnimator.this.dispatchFinishedWhenDone();
            }
        });
        animatorSet5.play(animatorSet).before(animatorSet3);
        animatorSet5.play(animatorSet).before(animatorSet4);
        animatorSet5.play(animatorSet3).with(animatorSet4);
        animatorSet5.play(animatorSet2).after(animatorSet3);
        animatorSet5.play(animatorSet2).after(animatorSet4);
        animatorSet5.start();
    }

    @Override
    public void endAnimation(RecyclerView.ViewHolder viewHolder) {
        Animator animator = this.mAnimators.get(viewHolder);
        this.mAnimators.remove(viewHolder);
        this.mAddAnimatorsList.remove(animator);
        this.mRemoveAnimatorsList.remove(animator);
        this.mChangeAnimatorsList.remove(animator);
        this.mMoveAnimatorsList.remove(animator);
        if (animator != null) {
            animator.end();
        }
        dispatchFinishedWhenDone();
    }

    @Override
    public void endAnimations() {
        Iterator it = new ArrayList(this.mAnimators.values()).iterator();
        while (it.hasNext()) {
            ((Animator) it.next()).end();
        }
        dispatchFinishedWhenDone();
    }

    @Override
    public boolean isRunning() {
        return !this.mAnimators.isEmpty();
    }

    private void dispatchFinishedWhenDone() {
        if (!isRunning()) {
            dispatchAnimationsFinished();
        }
    }

    @Override
    @NonNull
    public RecyclerView.ItemAnimator.ItemHolderInfo recordPreLayoutInformation(@NonNull RecyclerView.State state, @NonNull RecyclerView.ViewHolder viewHolder, int i, @NonNull List<Object> list) {
        RecyclerView.ItemAnimator.ItemHolderInfo itemHolderInfoRecordPreLayoutInformation = super.recordPreLayoutInformation(state, viewHolder, i, list);
        if (itemHolderInfoRecordPreLayoutInformation instanceof PayloadItemHolderInfo) {
            ((PayloadItemHolderInfo) itemHolderInfoRecordPreLayoutInformation).setPayloads(list);
        }
        return itemHolderInfoRecordPreLayoutInformation;
    }

    @Override
    public RecyclerView.ItemAnimator.ItemHolderInfo obtainHolderInfo() {
        return new PayloadItemHolderInfo();
    }

    @Override
    public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, @NonNull List<Object> list) {
        return !list.isEmpty() || super.canReuseUpdatedViewHolder(viewHolder, list);
    }

    private static final class PayloadItemHolderInfo extends RecyclerView.ItemAnimator.ItemHolderInfo {
        private final List<Object> mPayloads;

        private PayloadItemHolderInfo() {
            this.mPayloads = new ArrayList();
        }

        void setPayloads(List<Object> list) {
            this.mPayloads.clear();
            this.mPayloads.addAll(list);
        }

        List<Object> getPayloads() {
            return this.mPayloads;
        }
    }
}
