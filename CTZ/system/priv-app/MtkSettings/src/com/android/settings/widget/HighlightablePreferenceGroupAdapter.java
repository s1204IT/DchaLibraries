package com.android.settings.widget;

import android.R;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceGroupAdapter;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import com.android.settings.SettingsPreferenceFragment;

/* loaded from: classes.dex */
public class HighlightablePreferenceGroupAdapter extends PreferenceGroupAdapter {
    static final long DELAY_HIGHLIGHT_DURATION_MILLIS = 600;
    boolean mFadeInAnimated;
    final int mHighlightColor;
    private final String mHighlightKey;
    private int mHighlightPosition;
    private boolean mHighlightRequested;
    private final int mNormalBackgroundRes;

    public static void adjustInitialExpandedChildCount(SettingsPreferenceFragment settingsPreferenceFragment) {
        PreferenceScreen preferenceScreen;
        if (settingsPreferenceFragment == null || (preferenceScreen = settingsPreferenceFragment.getPreferenceScreen()) == null) {
            return;
        }
        Bundle arguments = settingsPreferenceFragment.getArguments();
        if (arguments != null && !TextUtils.isEmpty(arguments.getString(":settings:fragment_args_key"))) {
            preferenceScreen.setInitialExpandedChildrenCount(Preference.DEFAULT_ORDER);
            return;
        }
        int initialExpandedChildCount = settingsPreferenceFragment.getInitialExpandedChildCount();
        if (initialExpandedChildCount <= 0) {
            return;
        }
        preferenceScreen.setInitialExpandedChildrenCount(initialExpandedChildCount);
    }

    public HighlightablePreferenceGroupAdapter(PreferenceGroup preferenceGroup, String str, boolean z) {
        super(preferenceGroup);
        this.mHighlightPosition = -1;
        this.mHighlightKey = str;
        this.mHighlightRequested = z;
        Context context = preferenceGroup.getContext();
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.selectableItemBackground, typedValue, true);
        this.mNormalBackgroundRes = typedValue.resourceId;
        this.mHighlightColor = context.getColor(com.android.settings.R.color.preference_highligh_color);
    }

    /* JADX DEBUG: Method merged with bridge method: onBindViewHolder(Landroid/support/v7/widget/RecyclerView$ViewHolder;I)V */
    @Override // android.support.v7.preference.PreferenceGroupAdapter, android.support.v7.widget.RecyclerView.Adapter
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder, int i) {
        super.onBindViewHolder(preferenceViewHolder, i);
        updateBackground(preferenceViewHolder, i);
    }

    void updateBackground(PreferenceViewHolder preferenceViewHolder, int i) {
        View view = preferenceViewHolder.itemView;
        if (i == this.mHighlightPosition) {
            addHighlightBackground(view, !this.mFadeInAnimated);
        } else if (Boolean.TRUE.equals(view.getTag(com.android.settings.R.id.preference_highlighted))) {
            removeHighlightBackground(view, false);
        }
    }

    public void requestHighlight(View view, final RecyclerView recyclerView) {
        if (this.mHighlightRequested || recyclerView == null || TextUtils.isEmpty(this.mHighlightKey)) {
            return;
        }
        view.postDelayed(new Runnable() { // from class: com.android.settings.widget.-$$Lambda$HighlightablePreferenceGroupAdapter$Xc5BA2nCks8YuSzn7LsPZS7EmPA
            @Override // java.lang.Runnable
            public final void run() {
                HighlightablePreferenceGroupAdapter.lambda$requestHighlight$0(this.f$0, recyclerView);
            }
        }, DELAY_HIGHLIGHT_DURATION_MILLIS);
    }

    public static /* synthetic */ void lambda$requestHighlight$0(HighlightablePreferenceGroupAdapter highlightablePreferenceGroupAdapter, RecyclerView recyclerView) {
        int preferenceAdapterPosition = highlightablePreferenceGroupAdapter.getPreferenceAdapterPosition(highlightablePreferenceGroupAdapter.mHighlightKey);
        if (preferenceAdapterPosition < 0) {
            return;
        }
        highlightablePreferenceGroupAdapter.mHighlightRequested = true;
        recyclerView.smoothScrollToPosition(preferenceAdapterPosition);
        highlightablePreferenceGroupAdapter.mHighlightPosition = preferenceAdapterPosition;
        highlightablePreferenceGroupAdapter.notifyItemChanged(preferenceAdapterPosition);
    }

    public boolean isHighlightRequested() {
        return this.mHighlightRequested;
    }

    void requestRemoveHighlightDelayed(final View view) {
        view.postDelayed(new Runnable() { // from class: com.android.settings.widget.-$$Lambda$HighlightablePreferenceGroupAdapter$CKVsKq8Jy7vb9RpitMwq8ps1ehE
            @Override // java.lang.Runnable
            public final void run() {
                HighlightablePreferenceGroupAdapter.lambda$requestRemoveHighlightDelayed$1(this.f$0, view);
            }
        }, 15000L);
    }

    public static /* synthetic */ void lambda$requestRemoveHighlightDelayed$1(HighlightablePreferenceGroupAdapter highlightablePreferenceGroupAdapter, View view) {
        highlightablePreferenceGroupAdapter.mHighlightPosition = -1;
        highlightablePreferenceGroupAdapter.removeHighlightBackground(view, true);
    }

    private void addHighlightBackground(final View view, boolean z) {
        view.setTag(com.android.settings.R.id.preference_highlighted, true);
        if (!z) {
            view.setBackgroundColor(this.mHighlightColor);
            Log.d("HighlightableAdapter", "AddHighlight: Not animation requested - setting highlight background");
            requestRemoveHighlightDelayed(view);
            return;
        }
        this.mFadeInAnimated = true;
        ValueAnimator valueAnimatorOfObject = ValueAnimator.ofObject(new ArgbEvaluator(), -1, Integer.valueOf(this.mHighlightColor));
        valueAnimatorOfObject.setDuration(200L);
        valueAnimatorOfObject.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.settings.widget.-$$Lambda$HighlightablePreferenceGroupAdapter$piymLpeUf2m74Yz5ep7jpdxw2ho
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                view.setBackgroundColor(((Integer) valueAnimator.getAnimatedValue()).intValue());
            }
        });
        valueAnimatorOfObject.setRepeatMode(2);
        valueAnimatorOfObject.setRepeatCount(4);
        valueAnimatorOfObject.start();
        Log.d("HighlightableAdapter", "AddHighlight: starting fade in animation");
        requestRemoveHighlightDelayed(view);
    }

    private void removeHighlightBackground(final View view, boolean z) {
        if (!z) {
            view.setTag(com.android.settings.R.id.preference_highlighted, false);
            view.setBackgroundResource(this.mNormalBackgroundRes);
            Log.d("HighlightableAdapter", "RemoveHighlight: No animation requested - setting normal background");
        } else {
            if (!Boolean.TRUE.equals(view.getTag(com.android.settings.R.id.preference_highlighted))) {
                Log.d("HighlightableAdapter", "RemoveHighlight: Not highlighted - skipping");
                return;
            }
            int i = this.mHighlightColor;
            view.setTag(com.android.settings.R.id.preference_highlighted, false);
            ValueAnimator valueAnimatorOfObject = ValueAnimator.ofObject(new ArgbEvaluator(), Integer.valueOf(i), -1);
            valueAnimatorOfObject.setDuration(500L);
            valueAnimatorOfObject.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.settings.widget.-$$Lambda$HighlightablePreferenceGroupAdapter$HMY634RMu5R2WoggcFMdrEe8uA0
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    view.setBackgroundColor(((Integer) valueAnimator.getAnimatedValue()).intValue());
                }
            });
            valueAnimatorOfObject.addListener(new AnimatorListenerAdapter() { // from class: com.android.settings.widget.HighlightablePreferenceGroupAdapter.1
                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animator) {
                    view.setBackgroundResource(HighlightablePreferenceGroupAdapter.this.mNormalBackgroundRes);
                }
            });
            valueAnimatorOfObject.start();
            Log.d("HighlightableAdapter", "Starting fade out animation");
        }
    }
}
