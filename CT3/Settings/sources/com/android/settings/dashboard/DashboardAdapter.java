package com.android.settings.dashboard;

import android.R;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.ArrayUtils;
import com.android.settings.SettingsActivity;
import com.android.settings.dashboard.conditional.Condition;
import com.android.settings.dashboard.conditional.ConditionAdapterUtils;
import com.android.settingslib.SuggestionParser;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;
import java.util.ArrayList;
import java.util.List;

public class DashboardAdapter extends RecyclerView.Adapter<DashboardItemHolder> implements View.OnClickListener {
    private final IconCache mCache;
    private List<DashboardCategory> mCategories;
    private List<Condition> mConditions;
    private final Context mContext;
    private int mId;
    private boolean mIsShowingAll;
    private SuggestionParser mSuggestionParser;
    private List<Tile> mSuggestions;
    private static int SUGGESTION_MODE_DEFAULT = 0;
    private static int SUGGESTION_MODE_COLLAPSED = 1;
    private static int SUGGESTION_MODE_EXPANDED = 2;
    private final List<Object> mItems = new ArrayList();
    private final List<Integer> mTypes = new ArrayList();
    private final List<Integer> mIds = new ArrayList();
    private int mSuggestionMode = SUGGESTION_MODE_DEFAULT;
    private Condition mExpandedCondition = null;

    public DashboardAdapter(Context context, SuggestionParser parser) {
        this.mContext = context;
        this.mCache = new IconCache(context);
        this.mSuggestionParser = parser;
        setHasStableIds(true);
        setShowingAll(true);
    }

    public List<Tile> getSuggestions() {
        return this.mSuggestions;
    }

    public void setSuggestions(List<Tile> suggestions) {
        this.mSuggestions = suggestions;
        recountItems();
    }

    public Tile getTile(ComponentName component) {
        for (int i = 0; i < this.mCategories.size(); i++) {
            for (int j = 0; j < this.mCategories.get(i).tiles.size(); j++) {
                Tile tile = this.mCategories.get(i).tiles.get(j);
                if (component.equals(tile.intent.getComponent())) {
                    return tile;
                }
            }
        }
        return null;
    }

    public void setCategories(List<DashboardCategory> categories) {
        this.mCategories = categories;
        TypedValue tintColor = new TypedValue();
        this.mContext.getTheme().resolveAttribute(R.attr.colorAccent, tintColor, true);
        for (int i = 0; i < categories.size(); i++) {
            for (int j = 0; j < categories.get(i).tiles.size(); j++) {
                Tile tile = categories.get(i).tiles.get(j);
                if (!this.mContext.getPackageName().equals(tile.intent.getComponent().getPackageName())) {
                    tile.icon.setTint(tintColor.data);
                }
            }
        }
        recountItems();
    }

    public void setConditions(List<Condition> conditions) {
        this.mConditions = conditions;
        recountItems();
    }

    public void notifyChanged(Tile tile) {
        notifyDataSetChanged();
    }

    public void setShowingAll(boolean showingAll) {
        this.mIsShowingAll = showingAll;
        recountItems();
    }

    public void recountItems() {
        reset();
        boolean hasConditions = false;
        for (int i = 0; this.mConditions != null && i < this.mConditions.size(); i++) {
            boolean shouldShow = this.mConditions.get(i).shouldShow();
            hasConditions |= shouldShow;
            countItem(this.mConditions.get(i), com.android.settings.R.layout.condition_card, shouldShow, 3000);
        }
        boolean hasSuggestions = (this.mSuggestions == null || this.mSuggestions.size() == 0) ? false : true;
        if (!hasConditions) {
            hasSuggestions = false;
        }
        countItem(null, com.android.settings.R.layout.dashboard_spacer, hasSuggestions, 0);
        resetCount();
        countItem(null, com.android.settings.R.layout.dashboard_spacer, true, 0);
        resetCount();
        for (int i2 = 0; this.mCategories != null && i2 < this.mCategories.size(); i2++) {
            DashboardCategory category = this.mCategories.get(i2);
            countItem(category, com.android.settings.R.layout.dashboard_category, this.mIsShowingAll, 2000);
            for (int j = 0; j < category.tiles.size(); j++) {
                Tile tile = category.tiles.get(j);
                countItem(tile, com.android.settings.R.layout.dashboard_tile, !this.mIsShowingAll ? ArrayUtils.contains(DashboardSummary.INITIAL_ITEMS, tile.intent.getComponent().getClassName()) : true, 2000);
            }
        }
        notifyDataSetChanged();
    }

    private void resetCount() {
        this.mId = 0;
    }

    private void reset() {
        this.mItems.clear();
        this.mTypes.clear();
        this.mIds.clear();
        this.mId = 0;
    }

    private void countItem(Object object, int type, boolean add, int nameSpace) {
        if (add) {
            this.mItems.add(object);
            this.mTypes.add(Integer.valueOf(type));
            this.mIds.add(Integer.valueOf(this.mId + nameSpace));
        }
        this.mId++;
    }

    @Override
    public DashboardItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DashboardItemHolder(LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false));
    }

    @Override
    public void onBindViewHolder(DashboardItemHolder holder, int position) {
        switch (this.mTypes.get(position).intValue()) {
            case com.android.settings.R.layout.condition_card:
                ConditionAdapterUtils.bindViews((Condition) this.mItems.get(position), holder, this.mItems.get(position) == this.mExpandedCondition, this, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DashboardAdapter.this.onExpandClick(v);
                    }
                });
                break;
            case com.android.settings.R.layout.dashboard_category:
                onBindCategory(holder, (DashboardCategory) this.mItems.get(position));
                break;
            case com.android.settings.R.layout.dashboard_tile:
                Tile tile = (Tile) this.mItems.get(position);
                onBindTile(holder, tile);
                holder.itemView.setTag(tile);
                holder.itemView.setOnClickListener(this);
                break;
            case com.android.settings.R.layout.see_all:
                onBindSeeAll(holder);
                break;
            case com.android.settings.R.layout.suggestion_header:
                onBindSuggestionHeader(holder);
                break;
            case com.android.settings.R.layout.suggestion_tile:
                final Tile suggestion = (Tile) this.mItems.get(position);
                onBindTile(holder, suggestion);
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MetricsLogger.action(DashboardAdapter.this.mContext, 386, DashboardAdapter.getSuggestionIdentifier(DashboardAdapter.this.mContext, suggestion));
                        ((SettingsActivity) DashboardAdapter.this.mContext).startSuggestion(suggestion.intent);
                    }
                });
                holder.itemView.findViewById(com.android.settings.R.id.overflow).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DashboardAdapter.this.showRemoveOption(v, suggestion);
                    }
                });
                break;
        }
    }

    public void showRemoveOption(View v, final Tile suggestion) {
        PopupMenu popup = new PopupMenu(new ContextThemeWrapper(this.mContext, com.android.settings.R.style.Theme_AppCompat_DayNight), v);
        popup.getMenu().add(com.android.settings.R.string.suggestion_remove).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                MetricsLogger.action(DashboardAdapter.this.mContext, 387, DashboardAdapter.getSuggestionIdentifier(DashboardAdapter.this.mContext, suggestion));
                DashboardAdapter.this.disableSuggestion(suggestion);
                DashboardAdapter.this.mSuggestions.remove(suggestion);
                DashboardAdapter.this.recountItems();
                return true;
            }
        });
        popup.show();
    }

    public void disableSuggestion(Tile suggestion) {
        if (this.mSuggestionParser == null || !this.mSuggestionParser.dismissSuggestion(suggestion)) {
            return;
        }
        this.mContext.getPackageManager().setComponentEnabledSetting(suggestion.intent.getComponent(), 2, 1);
        this.mSuggestionParser.markCategoryDone(suggestion.category);
    }

    private void onBindSuggestionHeader(DashboardItemHolder holder) {
        holder.icon.setImageResource(hasMoreSuggestions() ? com.android.settings.R.drawable.ic_expand_more : com.android.settings.R.drawable.ic_expand_less);
        holder.title.setText(this.mContext.getString(com.android.settings.R.string.suggestions_title, Integer.valueOf(this.mSuggestions.size())));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (DashboardAdapter.this.hasMoreSuggestions()) {
                    DashboardAdapter.this.mSuggestionMode = DashboardAdapter.SUGGESTION_MODE_EXPANDED;
                } else {
                    DashboardAdapter.this.mSuggestionMode = DashboardAdapter.SUGGESTION_MODE_COLLAPSED;
                }
                DashboardAdapter.this.recountItems();
            }
        });
    }

    public boolean hasMoreSuggestions() {
        if (this.mSuggestionMode != SUGGESTION_MODE_COLLAPSED) {
            return this.mSuggestionMode == SUGGESTION_MODE_DEFAULT && this.mSuggestions.size() > 2;
        }
        return true;
    }

    private void onBindTile(DashboardItemHolder holder, Tile tile) {
        holder.icon.setImageDrawable(this.mCache.getIcon(tile.icon));
        holder.title.setText(tile.title);
        if (!TextUtils.isEmpty(tile.summary)) {
            holder.summary.setText(tile.summary);
            holder.summary.setVisibility(0);
        } else {
            holder.summary.setVisibility(8);
        }
    }

    private void onBindCategory(DashboardItemHolder holder, DashboardCategory category) {
        holder.title.setText(category.title);
    }

    private void onBindSeeAll(DashboardItemHolder holder) {
        holder.title.setText(this.mIsShowingAll ? com.android.settings.R.string.see_less : com.android.settings.R.string.see_all);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DashboardAdapter.this.setShowingAll(!DashboardAdapter.this.mIsShowingAll);
            }
        });
    }

    @Override
    public long getItemId(int position) {
        return this.mIds.get(position).intValue();
    }

    @Override
    public int getItemViewType(int position) {
        return this.mTypes.get(position).intValue();
    }

    @Override
    public int getItemCount() {
        return this.mIds.size();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == com.android.settings.R.id.dashboard_tile) {
            ((SettingsActivity) this.mContext).openTile((Tile) v.getTag());
            return;
        }
        if (v.getTag() == this.mExpandedCondition) {
            MetricsLogger.action(this.mContext, 375, this.mExpandedCondition.getMetricsConstant());
            this.mExpandedCondition.onPrimaryClick();
        } else {
            this.mExpandedCondition = (Condition) v.getTag();
            MetricsLogger.action(this.mContext, 373, this.mExpandedCondition.getMetricsConstant());
            notifyDataSetChanged();
        }
    }

    public void onExpandClick(View v) {
        if (v.getTag() == this.mExpandedCondition) {
            MetricsLogger.action(this.mContext, 374, this.mExpandedCondition.getMetricsConstant());
            this.mExpandedCondition = null;
        } else {
            this.mExpandedCondition = (Condition) v.getTag();
            MetricsLogger.action(this.mContext, 373, this.mExpandedCondition.getMetricsConstant());
        }
        notifyDataSetChanged();
    }

    public Object getItem(long itemId) {
        for (int i = 0; i < this.mIds.size(); i++) {
            if (this.mIds.get(i).intValue() == itemId) {
                return this.mItems.get(i);
            }
        }
        return null;
    }

    public static String getSuggestionIdentifier(Context context, Tile suggestion) {
        String packageName = suggestion.intent.getComponent().getPackageName();
        if (packageName.equals(context.getPackageName())) {
            return suggestion.intent.getComponent().getClassName();
        }
        return packageName;
    }

    private static class IconCache {
        private final Context mContext;
        private final ArrayMap<Icon, Drawable> mMap = new ArrayMap<>();

        public IconCache(Context context) {
            this.mContext = context;
        }

        public Drawable getIcon(Icon icon) {
            Drawable drawable = this.mMap.get(icon);
            if (drawable == null) {
                Drawable drawable2 = icon.loadDrawable(this.mContext);
                this.mMap.put(icon, drawable2);
                return drawable2;
            }
            return drawable;
        }
    }

    public static class DashboardItemHolder extends RecyclerView.ViewHolder {
        public final ImageView icon;
        public final TextView summary;
        public final TextView title;

        public DashboardItemHolder(View itemView) {
            super(itemView);
            this.icon = (ImageView) itemView.findViewById(R.id.icon);
            this.title = (TextView) itemView.findViewById(R.id.title);
            this.summary = (TextView) itemView.findViewById(R.id.summary);
        }
    }
}
