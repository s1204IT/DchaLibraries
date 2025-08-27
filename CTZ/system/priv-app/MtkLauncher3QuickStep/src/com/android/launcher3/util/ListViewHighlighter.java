package com.android.launcher3.util;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.ColorUtils;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import com.android.launcher3.R;

/* loaded from: classes.dex */
public class ListViewHighlighter implements AbsListView.OnScrollListener, AbsListView.RecyclerListener, View.OnLayoutChangeListener {
    private boolean mColorAnimated = false;
    private final ListView mListView;
    private int mPosHighlight;

    public ListViewHighlighter(ListView listView, int i) {
        this.mListView = listView;
        this.mPosHighlight = i;
        this.mListView.setOnScrollListener(this);
        this.mListView.setRecyclerListener(this);
        this.mListView.addOnLayoutChangeListener(this);
        this.mListView.post(new $$Lambda$ListViewHighlighter$LHpR61dAsd_XsBT1HxobUwT3h4(this));
    }

    @Override // android.view.View.OnLayoutChangeListener
    public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        this.mListView.post(new $$Lambda$ListViewHighlighter$LHpR61dAsd_XsBT1HxobUwT3h4(this));
    }

    private void tryHighlight() {
        if (this.mPosHighlight >= 0 && this.mListView.getChildCount() != 0 && !highlightIfVisible(this.mListView.getFirstVisiblePosition(), this.mListView.getLastVisiblePosition())) {
            this.mListView.smoothScrollToPosition(this.mPosHighlight);
        }
    }

    @Override // android.widget.AbsListView.OnScrollListener
    public void onScrollStateChanged(AbsListView absListView, int i) {
    }

    @Override // android.widget.AbsListView.OnScrollListener
    public void onScroll(AbsListView absListView, int i, int i2, int i3) {
        highlightIfVisible(i, i2 + i);
    }

    private boolean highlightIfVisible(int i, int i2) {
        if (this.mPosHighlight < 0 || this.mListView.getChildCount() == 0 || i > this.mPosHighlight || this.mPosHighlight > i2) {
            return false;
        }
        highlightView(this.mListView.getChildAt(this.mPosHighlight - i));
        this.mListView.setOnScrollListener(null);
        this.mListView.removeOnLayoutChangeListener(this);
        this.mPosHighlight = -1;
        return true;
    }

    @Override // android.widget.AbsListView.RecyclerListener
    public void onMovedToScrapHeap(View view) {
        unhighlightView(view);
    }

    private void highlightView(final View view) {
        if (!Boolean.TRUE.equals(view.getTag(R.id.view_highlighted))) {
            view.setTag(R.id.view_highlighted, true);
            view.setTag(R.id.view_unhighlight_background, view.getBackground());
            view.setBackground(getHighlightBackground());
            view.postDelayed(new Runnable() { // from class: com.android.launcher3.util.-$$Lambda$ListViewHighlighter$Z_XONGZw1Qjdi0BQdkUZpYybff4
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.unhighlightView(view);
                }
            }, 15000L);
        }
    }

    private void unhighlightView(View view) {
        if (Boolean.TRUE.equals(view.getTag(R.id.view_highlighted))) {
            Object tag = view.getTag(R.id.view_unhighlight_background);
            if (tag instanceof Drawable) {
                view.setBackground((Drawable) tag);
            }
            view.setTag(R.id.view_unhighlight_background, null);
            view.setTag(R.id.view_highlighted, false);
        }
    }

    private ColorDrawable getHighlightBackground() {
        int alphaComponent = ColorUtils.setAlphaComponent(Themes.getColorAccent(this.mListView.getContext()), 26);
        if (this.mColorAnimated) {
            return new ColorDrawable(alphaComponent);
        }
        this.mColorAnimated = true;
        ColorDrawable colorDrawable = new ColorDrawable(-1);
        ObjectAnimator objectAnimatorOfInt = ObjectAnimator.ofInt(colorDrawable, "color", -1, alphaComponent);
        objectAnimatorOfInt.setEvaluator(new ArgbEvaluator());
        objectAnimatorOfInt.setDuration(200L);
        objectAnimatorOfInt.setRepeatMode(2);
        objectAnimatorOfInt.setRepeatCount(4);
        objectAnimatorOfInt.start();
        return colorDrawable;
    }
}
