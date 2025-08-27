package com.android.settings.dashboard.suggestions;

import android.R;
import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.service.settings.suggestions.Suggestion;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import com.android.settings.dashboard.DashboardAdapter;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.Utils;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnSaveInstanceState;
import com.android.settingslib.suggestions.SuggestionControllerMixin;
import com.android.settingslib.utils.IconCache;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/* loaded from: classes.dex */
public class SuggestionAdapter extends RecyclerView.Adapter<DashboardAdapter.DashboardItemHolder> implements LifecycleObserver, OnSaveInstanceState {
    private final IconCache mCache;
    private final Callback mCallback;
    private final CardConfig mConfig;
    private final Context mContext;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final SuggestionControllerMixin mSuggestionControllerMixin;
    private final SuggestionFeatureProvider mSuggestionFeatureProvider;
    private List<Suggestion> mSuggestions;
    private final ArrayList<String> mSuggestionsShownLogged;

    public interface Callback {
        void onSuggestionClosed(Suggestion suggestion);
    }

    public SuggestionAdapter(Context context, SuggestionControllerMixin suggestionControllerMixin, Bundle bundle, Callback callback, Lifecycle lifecycle) {
        this.mContext = context;
        this.mSuggestionControllerMixin = suggestionControllerMixin;
        this.mCache = new IconCache(context);
        FeatureFactory factory = FeatureFactory.getFactory(context);
        this.mMetricsFeatureProvider = factory.getMetricsFeatureProvider();
        this.mSuggestionFeatureProvider = factory.getSuggestionFeatureProvider(context);
        this.mCallback = callback;
        if (bundle != null) {
            this.mSuggestions = bundle.getParcelableArrayList("suggestion_list");
            this.mSuggestionsShownLogged = bundle.getStringArrayList("suggestions_shown_logged");
        } else {
            this.mSuggestionsShownLogged = new ArrayList<>();
        }
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
        this.mConfig = CardConfig.get(context);
        setHasStableIds(true);
    }

    /* JADX DEBUG: Method merged with bridge method: onCreateViewHolder(Landroid/view/ViewGroup;I)Landroid/support/v7/widget/RecyclerView$ViewHolder; */
    @Override // android.support.v7.widget.RecyclerView.Adapter
    public DashboardAdapter.DashboardItemHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new DashboardAdapter.DashboardItemHolder(LayoutInflater.from(viewGroup.getContext()).inflate(i, viewGroup, false));
    }

    /* JADX DEBUG: Method merged with bridge method: onBindViewHolder(Landroid/support/v7/widget/RecyclerView$ViewHolder;I)V */
    @Override // android.support.v7.widget.RecyclerView.Adapter
    public void onBindViewHolder(DashboardAdapter.DashboardItemHolder dashboardItemHolder, int i) {
        final Suggestion suggestion = this.mSuggestions.get(i);
        final String id = suggestion.getId();
        int size = this.mSuggestions.size();
        if (!this.mSuggestionsShownLogged.contains(id)) {
            this.mMetricsFeatureProvider.action(this.mContext, 384, id, new Pair[0]);
            this.mSuggestionsShownLogged.add(id);
        }
        Drawable icon = this.mCache.getIcon(suggestion.getIcon());
        if (icon != null && (suggestion.getFlags() & 2) != 0) {
            icon.setTint(Utils.getColorAccent(this.mContext));
        }
        dashboardItemHolder.icon.setImageDrawable(icon);
        dashboardItemHolder.title.setText(suggestion.getTitle());
        dashboardItemHolder.title.setTypeface(Typeface.create(this.mContext.getString(R.string.android_system_label), 0));
        if (size == 1) {
            CharSequence summary = suggestion.getSummary();
            if (!TextUtils.isEmpty(summary)) {
                dashboardItemHolder.summary.setText(summary);
                dashboardItemHolder.summary.setVisibility(0);
            } else {
                dashboardItemHolder.summary.setVisibility(8);
            }
        } else {
            this.mConfig.setCardLayout(dashboardItemHolder, i);
        }
        View viewFindViewById = dashboardItemHolder.itemView.findViewById(com.android.settings.R.id.close_button);
        if (viewFindViewById != null) {
            viewFindViewById.setOnClickListener(new View.OnClickListener() { // from class: com.android.settings.dashboard.suggestions.-$$Lambda$SuggestionAdapter$3YCJShAgHMZGvTmpJ4rD8V_2WkA
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    SuggestionAdapter.lambda$onBindViewHolder$0(this.f$0, suggestion, view);
                }
            });
        }
        View view = dashboardItemHolder.itemView;
        View viewFindViewById2 = dashboardItemHolder.itemView.findViewById(R.id.primary);
        if (viewFindViewById2 == null) {
            viewFindViewById2 = view;
        }
        viewFindViewById2.setOnClickListener(new View.OnClickListener() { // from class: com.android.settings.dashboard.suggestions.-$$Lambda$SuggestionAdapter$o_nlX1JhE-RQCl3p5ch8A_R_uN0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) throws PendingIntent.CanceledException {
                SuggestionAdapter.lambda$onBindViewHolder$1(this.f$0, id, suggestion, view2);
            }
        });
    }

    public static /* synthetic */ void lambda$onBindViewHolder$0(SuggestionAdapter suggestionAdapter, Suggestion suggestion, View view) {
        suggestionAdapter.mSuggestionFeatureProvider.dismissSuggestion(suggestionAdapter.mContext, suggestionAdapter.mSuggestionControllerMixin, suggestion);
        if (suggestionAdapter.mCallback != null) {
            suggestionAdapter.mCallback.onSuggestionClosed(suggestion);
        }
    }

    public static /* synthetic */ void lambda$onBindViewHolder$1(SuggestionAdapter suggestionAdapter, String str, Suggestion suggestion, View view) throws PendingIntent.CanceledException {
        suggestionAdapter.mMetricsFeatureProvider.action(suggestionAdapter.mContext, 386, str, new Pair[0]);
        try {
            suggestion.getPendingIntent().send();
            suggestionAdapter.mSuggestionControllerMixin.launchSuggestion(suggestion);
        } catch (PendingIntent.CanceledException e) {
            Log.w("SuggestionAdapter", "Failed to start suggestion " + ((Object) suggestion.getTitle()));
        }
    }

    @Override // android.support.v7.widget.RecyclerView.Adapter
    public long getItemId(int i) {
        return Objects.hash(this.mSuggestions.get(i).getId());
    }

    @Override // android.support.v7.widget.RecyclerView.Adapter
    public int getItemViewType(int i) {
        if ((getSuggestion(i).getFlags() & 1) != 0) {
            return com.android.settings.R.layout.suggestion_tile_with_button;
        }
        if (getItemCount() == 1) {
            return com.android.settings.R.layout.suggestion_tile;
        }
        return com.android.settings.R.layout.suggestion_tile_two_cards;
    }

    @Override // android.support.v7.widget.RecyclerView.Adapter
    public int getItemCount() {
        return this.mSuggestions.size();
    }

    public Suggestion getSuggestion(int i) {
        long itemId = getItemId(i);
        if (this.mSuggestions == null) {
            return null;
        }
        for (Suggestion suggestion : this.mSuggestions) {
            if (Objects.hash(suggestion.getId()) == itemId) {
                return suggestion;
            }
        }
        return null;
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnSaveInstanceState
    public void onSaveInstanceState(Bundle bundle) {
        if (this.mSuggestions != null) {
            bundle.putParcelableArrayList("suggestion_list", new ArrayList<>(this.mSuggestions));
        }
        bundle.putStringArrayList("suggestions_shown_logged", this.mSuggestionsShownLogged);
    }

    public void setSuggestions(List<Suggestion> list) {
        this.mSuggestions = list;
    }

    public List<Suggestion> getSuggestions() {
        return this.mSuggestions;
    }

    static class CardConfig {
        private static CardConfig sConfig;
        private final int mMarginInner;
        private final int mMarginOuter;
        private final WindowManager mWindowManager;

        private CardConfig(Context context) {
            this.mWindowManager = (WindowManager) context.getSystemService("window");
            Resources resources = context.getResources();
            this.mMarginInner = resources.getDimensionPixelOffset(com.android.settings.R.dimen.suggestion_card_inner_margin);
            this.mMarginOuter = resources.getDimensionPixelOffset(com.android.settings.R.dimen.suggestion_card_outer_margin);
        }

        public static CardConfig get(Context context) {
            if (sConfig == null) {
                sConfig = new CardConfig(context);
            }
            return sConfig;
        }

        void setCardLayout(DashboardAdapter.DashboardItemHolder dashboardItemHolder, int i) {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(getWidthForTwoCrads(), -2);
            layoutParams.setMarginStart(i == 0 ? this.mMarginOuter : this.mMarginInner);
            layoutParams.setMarginEnd(i != 0 ? this.mMarginOuter : 0);
            dashboardItemHolder.itemView.setLayoutParams(layoutParams);
        }

        private int getWidthForTwoCrads() {
            return ((getScreenWidth() - this.mMarginInner) - (this.mMarginOuter * 2)) / 2;
        }

        int getScreenWidth() {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            this.mWindowManager.getDefaultDisplay().getMetrics(displayMetrics);
            return displayMetrics.widthPixels;
        }
    }
}
