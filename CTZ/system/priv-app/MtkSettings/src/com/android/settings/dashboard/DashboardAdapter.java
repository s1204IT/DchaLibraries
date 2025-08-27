package com.android.settings.dashboard;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.service.settings.suggestions.Suggestion;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardData;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.dashboard.conditional.Condition;
import com.android.settings.dashboard.conditional.ConditionAdapter;
import com.android.settings.dashboard.suggestions.SuggestionAdapter;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnSaveInstanceState;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;
import com.android.settingslib.suggestions.SuggestionControllerMixin;
import com.android.settingslib.utils.IconCache;
import java.util.List;

/* loaded from: classes.dex */
public class DashboardAdapter extends RecyclerView.Adapter<DashboardItemHolder> implements SummaryLoader.SummaryConsumer, SuggestionAdapter.Callback, LifecycleObserver, OnSaveInstanceState {
    static final String STATE_CONDITION_EXPANDED = "condition_expanded";
    private final IconCache mCache;
    private final Context mContext;
    DashboardData mDashboardData;
    private final DashboardFeatureProvider mDashboardFeatureProvider;
    private boolean mFirstFrameDrawn;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private RecyclerView mRecyclerView;
    private SuggestionAdapter mSuggestionAdapter;
    private View.OnClickListener mTileClickListener = new View.OnClickListener() { // from class: com.android.settings.dashboard.DashboardAdapter.1
        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            DashboardAdapter.this.mDashboardFeatureProvider.openTileIntent((Activity) DashboardAdapter.this.mContext, (Tile) view.getTag());
        }
    };

    public DashboardAdapter(Context context, Bundle bundle, List<Condition> list, SuggestionControllerMixin suggestionControllerMixin, Lifecycle lifecycle) {
        DashboardCategory dashboardCategory;
        this.mContext = context;
        FeatureFactory factory = FeatureFactory.getFactory(context);
        this.mMetricsFeatureProvider = factory.getMetricsFeatureProvider();
        this.mDashboardFeatureProvider = factory.getDashboardFeatureProvider(context);
        this.mCache = new IconCache(context);
        this.mSuggestionAdapter = new SuggestionAdapter(this.mContext, suggestionControllerMixin, bundle, this, lifecycle);
        setHasStableIds(true);
        boolean z = false;
        if (bundle != null) {
            dashboardCategory = (DashboardCategory) bundle.getParcelable("category_list");
            z = bundle.getBoolean(STATE_CONDITION_EXPANDED, false);
        } else {
            dashboardCategory = null;
        }
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
        this.mDashboardData = new DashboardData.Builder().setConditions(list).setSuggestions(this.mSuggestionAdapter.getSuggestions()).setCategory(dashboardCategory).setConditionExpanded(z).build();
    }

    public void setSuggestions(List<Suggestion> list) {
        DashboardData dashboardData = this.mDashboardData;
        this.mDashboardData = new DashboardData.Builder(dashboardData).setSuggestions(list).build();
        notifyDashboardDataChanged(dashboardData);
    }

    public void setCategory(DashboardCategory dashboardCategory) {
        DashboardData dashboardData = this.mDashboardData;
        Log.d("DashboardAdapter", "adapter setCategory called");
        this.mDashboardData = new DashboardData.Builder(dashboardData).setCategory(dashboardCategory).build();
        notifyDashboardDataChanged(dashboardData);
    }

    public void setConditions(List<Condition> list) {
        DashboardData dashboardData = this.mDashboardData;
        Log.d("DashboardAdapter", "adapter setConditions called");
        this.mDashboardData = new DashboardData.Builder(dashboardData).setConditions(list).build();
        notifyDashboardDataChanged(dashboardData);
    }

    @Override // com.android.settings.dashboard.suggestions.SuggestionAdapter.Callback
    public void onSuggestionClosed(Suggestion suggestion) {
        List<Suggestion> suggestions = this.mDashboardData.getSuggestions();
        if (suggestions == null || suggestions.size() == 0) {
            return;
        }
        if (suggestions.size() == 1) {
            setSuggestions(null);
        } else {
            suggestions.remove(suggestion);
            setSuggestions(suggestions);
        }
    }

    @Override // com.android.settings.dashboard.SummaryLoader.SummaryConsumer
    public void notifySummaryChanged(Tile tile) {
        int positionByTile = this.mDashboardData.getPositionByTile(tile);
        if (positionByTile != -1) {
            notifyItemChanged(positionByTile, Integer.valueOf(this.mDashboardData.getItemTypeByPosition(positionByTile)));
        }
    }

    /* JADX DEBUG: Method merged with bridge method: onCreateViewHolder(Landroid/view/ViewGroup;I)Landroid/support/v7/widget/RecyclerView$ViewHolder; */
    @Override // android.support.v7.widget.RecyclerView.Adapter
    public DashboardItemHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View viewInflate = LayoutInflater.from(viewGroup.getContext()).inflate(i, viewGroup, false);
        if (i == R.layout.condition_header) {
            return new ConditionHeaderHolder(viewInflate);
        }
        if (i == R.layout.condition_container) {
            return new ConditionContainerHolder(viewInflate);
        }
        if (i == R.layout.suggestion_container) {
            return new SuggestionContainerHolder(viewInflate);
        }
        return new DashboardItemHolder(viewInflate);
    }

    /* JADX DEBUG: Method merged with bridge method: onBindViewHolder(Landroid/support/v7/widget/RecyclerView$ViewHolder;I)V */
    @Override // android.support.v7.widget.RecyclerView.Adapter
    public void onBindViewHolder(DashboardItemHolder dashboardItemHolder, int i) {
        int itemTypeByPosition = this.mDashboardData.getItemTypeByPosition(i);
        if (itemTypeByPosition == R.layout.dashboard_tile) {
            Tile tile = (Tile) this.mDashboardData.getItemEntityByPosition(i);
            onBindTile(dashboardItemHolder, tile);
            dashboardItemHolder.itemView.setTag(tile);
            dashboardItemHolder.itemView.setOnClickListener(this.mTileClickListener);
        }
        if (itemTypeByPosition != R.layout.suggestion_container) {
            switch (itemTypeByPosition) {
                case R.layout.condition_container /* 2131558480 */:
                    onBindCondition((ConditionContainerHolder) dashboardItemHolder, i);
                    break;
                case R.layout.condition_footer /* 2131558481 */:
                    dashboardItemHolder.itemView.setOnClickListener(new View.OnClickListener() { // from class: com.android.settings.dashboard.-$$Lambda$DashboardAdapter$EPKC8_9XatJQPDMMI1s2dy8ZY8c
                        @Override // android.view.View.OnClickListener
                        public final void onClick(View view) {
                            DashboardAdapter.lambda$onBindViewHolder$0(this.f$0, view);
                        }
                    });
                    break;
                case R.layout.condition_header /* 2131558482 */:
                    onBindConditionHeader((ConditionHeaderHolder) dashboardItemHolder, (DashboardData.ConditionHeaderData) this.mDashboardData.getItemEntityByPosition(i));
                    break;
            }
        }
    }

    public static /* synthetic */ void lambda$onBindViewHolder$0(DashboardAdapter dashboardAdapter, View view) {
        dashboardAdapter.mMetricsFeatureProvider.action(dashboardAdapter.mContext, 373, false);
        DashboardData dashboardData = dashboardAdapter.mDashboardData;
        dashboardAdapter.mDashboardData = new DashboardData.Builder(dashboardData).setConditionExpanded(false).build();
        dashboardAdapter.notifyDashboardDataChanged(dashboardData);
        dashboardAdapter.scrollToTopOfConditions();
    }

    @Override // android.support.v7.widget.RecyclerView.Adapter
    public long getItemId(int i) {
        return this.mDashboardData.getItemIdByPosition(i);
    }

    @Override // android.support.v7.widget.RecyclerView.Adapter
    public int getItemViewType(int i) {
        return this.mDashboardData.getItemTypeByPosition(i);
    }

    @Override // android.support.v7.widget.RecyclerView.Adapter
    public int getItemCount() {
        return this.mDashboardData.size();
    }

    @Override // android.support.v7.widget.RecyclerView.Adapter
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.mRecyclerView = recyclerView;
    }

    void notifyDashboardDataChanged(DashboardData dashboardData) {
        if (this.mFirstFrameDrawn && dashboardData != null) {
            DiffUtil.calculateDiff(new DashboardData.ItemsDataDiffCallback(dashboardData.getItemList(), this.mDashboardData.getItemList())).dispatchUpdatesTo(this);
        } else {
            this.mFirstFrameDrawn = true;
            notifyDataSetChanged();
        }
    }

    void onBindConditionHeader(ConditionHeaderHolder conditionHeaderHolder, DashboardData.ConditionHeaderData conditionHeaderData) {
        conditionHeaderHolder.icon.setImageDrawable(conditionHeaderData.conditionIcons.get(0));
        if (conditionHeaderData.conditionCount == 1) {
            conditionHeaderHolder.title.setText(conditionHeaderData.title);
            conditionHeaderHolder.summary.setText((CharSequence) null);
            conditionHeaderHolder.icons.setVisibility(4);
        } else {
            conditionHeaderHolder.title.setText((CharSequence) null);
            conditionHeaderHolder.summary.setText(this.mContext.getString(R.string.condition_summary, Integer.valueOf(conditionHeaderData.conditionCount)));
            updateConditionIcons(conditionHeaderData.conditionIcons, conditionHeaderHolder.icons);
            conditionHeaderHolder.icons.setVisibility(0);
        }
        conditionHeaderHolder.itemView.setOnClickListener(new View.OnClickListener() { // from class: com.android.settings.dashboard.-$$Lambda$DashboardAdapter$rjSGP6Cnddhw6FvR740SjWmziQ4
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                DashboardAdapter.lambda$onBindConditionHeader$1(this.f$0, view);
            }
        });
    }

    public static /* synthetic */ void lambda$onBindConditionHeader$1(DashboardAdapter dashboardAdapter, View view) {
        dashboardAdapter.mMetricsFeatureProvider.action(dashboardAdapter.mContext, 373, true);
        DashboardData dashboardData = dashboardAdapter.mDashboardData;
        dashboardAdapter.mDashboardData = new DashboardData.Builder(dashboardData).setConditionExpanded(true).build();
        dashboardAdapter.notifyDashboardDataChanged(dashboardData);
        dashboardAdapter.scrollToTopOfConditions();
    }

    void onBindCondition(ConditionContainerHolder conditionContainerHolder, int i) {
        ConditionAdapter conditionAdapter = new ConditionAdapter(this.mContext, (List) this.mDashboardData.getItemEntityByPosition(i), this.mDashboardData.isConditionExpanded());
        conditionAdapter.addDismissHandling(conditionContainerHolder.data);
        conditionContainerHolder.data.setAdapter(conditionAdapter);
        conditionContainerHolder.data.setLayoutManager(new LinearLayoutManager(this.mContext));
    }

    void onBindSuggestion(SuggestionContainerHolder suggestionContainerHolder, int i) {
        List<Suggestion> list = (List) this.mDashboardData.getItemEntityByPosition(i);
        if (list != null && list.size() > 0) {
            this.mSuggestionAdapter.setSuggestions(list);
            suggestionContainerHolder.data.setAdapter(this.mSuggestionAdapter);
        }
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this.mContext);
        linearLayoutManager.setOrientation(0);
        suggestionContainerHolder.data.setLayoutManager(linearLayoutManager);
    }

    void onBindTile(DashboardItemHolder dashboardItemHolder, Tile tile) {
        int i;
        Drawable icon = this.mCache.getIcon(tile.icon);
        if (!TextUtils.equals(tile.icon.getResPackage(), this.mContext.getPackageName()) && !(icon instanceof RoundedHomepageIcon)) {
            RoundedHomepageIcon roundedHomepageIcon = new RoundedHomepageIcon(this.mContext, icon);
            try {
                if (tile.metaData != null && (i = tile.metaData.getInt("com.android.settings.bg.hint", 0)) != 0) {
                    roundedHomepageIcon.setBackgroundColor(this.mContext.getPackageManager().getResourcesForApplication(tile.icon.getResPackage()).getColor(i, null));
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("DashboardAdapter", "Failed to set background color for " + tile.intent.getPackage());
            }
            this.mCache.updateIcon(tile.icon, roundedHomepageIcon);
            icon = roundedHomepageIcon;
        }
        dashboardItemHolder.icon.setImageDrawable(icon);
        dashboardItemHolder.title.setText(tile.title);
        if (!TextUtils.isEmpty(tile.summary)) {
            dashboardItemHolder.summary.setText(tile.summary);
            dashboardItemHolder.summary.setVisibility(0);
        } else {
            dashboardItemHolder.summary.setVisibility(8);
        }
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnSaveInstanceState
    public void onSaveInstanceState(Bundle bundle) {
        DashboardCategory category = this.mDashboardData.getCategory();
        if (category != null) {
            bundle.putParcelable("category_list", category);
        }
        bundle.putBoolean(STATE_CONDITION_EXPANDED, this.mDashboardData.isConditionExpanded());
    }

    private void updateConditionIcons(List<Drawable> list, ViewGroup viewGroup) {
        if (list == null || list.size() < 2) {
            viewGroup.setVisibility(4);
            return;
        }
        LayoutInflater layoutInflaterFrom = LayoutInflater.from(viewGroup.getContext());
        viewGroup.removeAllViews();
        int size = list.size();
        for (int i = 1; i < size; i++) {
            ImageView imageView = (ImageView) layoutInflaterFrom.inflate(R.layout.condition_header_icon, viewGroup, false);
            imageView.setImageDrawable(list.get(i));
            viewGroup.addView(imageView);
        }
        viewGroup.setVisibility(0);
    }

    private void scrollToTopOfConditions() {
        this.mRecyclerView.scrollToPosition(this.mDashboardData.hasSuggestion() ? 1 : 0);
    }

    public static class DashboardItemHolder extends RecyclerView.ViewHolder {
        public final ImageView icon;
        public final TextView summary;
        public final TextView title;

        public DashboardItemHolder(View view) {
            super(view);
            this.icon = (ImageView) view.findViewById(android.R.id.icon);
            this.title = (TextView) view.findViewById(android.R.id.title);
            this.summary = (TextView) view.findViewById(android.R.id.summary);
        }
    }

    public static class ConditionHeaderHolder extends DashboardItemHolder {
        public final ImageView expandIndicator;
        public final LinearLayout icons;

        public ConditionHeaderHolder(View view) {
            super(view);
            this.icons = (LinearLayout) view.findViewById(R.id.additional_icons);
            this.expandIndicator = (ImageView) view.findViewById(R.id.expand_indicator);
        }
    }

    public static class ConditionContainerHolder extends DashboardItemHolder {
        public final RecyclerView data;

        public ConditionContainerHolder(View view) {
            super(view);
            this.data = (RecyclerView) view.findViewById(R.id.data);
        }
    }

    public static class SuggestionContainerHolder extends DashboardItemHolder {
        public final RecyclerView data;

        public SuggestionContainerHolder(View view) {
            super(view);
            this.data = (RecyclerView) view.findViewById(R.id.suggestion_list);
        }
    }
}
