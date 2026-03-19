package com.android.settings.dashboard;

import android.graphics.drawable.Drawable;
import android.service.settings.suggestions.Suggestion;
import android.support.v7.util.DiffUtil;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.dashboard.conditional.Condition;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DashboardData {
    static final int STABLE_ID_CONDITION_CONTAINER = 4;
    static final int STABLE_ID_CONDITION_FOOTER = 3;
    static final int STABLE_ID_CONDITION_HEADER = 2;
    static final int STABLE_ID_SUGGESTION_CONTAINER = 0;
    private final DashboardCategory mCategory;
    private final boolean mConditionExpanded;
    private final List<Condition> mConditions;
    private final List<Item> mItems;
    private final List<Suggestion> mSuggestions;

    private DashboardData(Builder builder) {
        this.mCategory = builder.mCategory;
        this.mConditions = builder.mConditions;
        this.mSuggestions = builder.mSuggestions;
        this.mConditionExpanded = builder.mConditionExpanded;
        this.mItems = new ArrayList();
        buildItemsData();
    }

    public int getItemIdByPosition(int i) {
        return this.mItems.get(i).id;
    }

    public int getItemTypeByPosition(int i) {
        return this.mItems.get(i).type;
    }

    public Object getItemEntityByPosition(int i) {
        return this.mItems.get(i).entity;
    }

    public List<Item> getItemList() {
        return this.mItems;
    }

    public int size() {
        return this.mItems.size();
    }

    public DashboardCategory getCategory() {
        return this.mCategory;
    }

    public List<Suggestion> getSuggestions() {
        return this.mSuggestions;
    }

    public boolean hasSuggestion() {
        return sizeOf(this.mSuggestions) > 0;
    }

    public boolean isConditionExpanded() {
        return this.mConditionExpanded;
    }

    public int getPositionByTile(Tile tile) {
        int size = this.mItems.size();
        for (int i = 0; i < size; i++) {
            Object obj = this.mItems.get(i).entity;
            if (obj == tile) {
                return i;
            }
            if ((obj instanceof Tile) && tile.title.equals(((Tile) obj).title)) {
                return i;
            }
        }
        return -1;
    }

    private void addToItemList(Object obj, int i, int i2, boolean z) {
        if (z) {
            this.mItems.add(new Item(obj, i, i2));
        }
    }

    private void buildItemsData() {
        List<Condition> conditionsToShow = getConditionsToShow(this.mConditions);
        boolean z = sizeOf(conditionsToShow) > 0;
        List<Suggestion> suggestionsToShow = getSuggestionsToShow(this.mSuggestions);
        boolean z2 = sizeOf(suggestionsToShow) > 0;
        addToItemList(suggestionsToShow, R.layout.suggestion_container, 0, z2);
        addToItemList(null, R.layout.horizontal_divider, 1, z2 && z);
        addToItemList(new ConditionHeaderData(conditionsToShow), R.layout.condition_header, 2, z && !this.mConditionExpanded);
        addToItemList(conditionsToShow, R.layout.condition_container, 4, z && this.mConditionExpanded);
        addToItemList(null, R.layout.condition_footer, 3, z && this.mConditionExpanded);
        if (this.mCategory != null) {
            List<Tile> tiles = this.mCategory.getTiles();
            for (int i = 0; i < tiles.size(); i++) {
                Tile tile = tiles.get(i);
                addToItemList(tile, R.layout.dashboard_tile, Objects.hash(tile.title), true);
            }
        }
    }

    private static int sizeOf(List<?> list) {
        if (list == null) {
            return 0;
        }
        return list.size();
    }

    private List<Condition> getConditionsToShow(List<Condition> list) {
        int size;
        if (list == null) {
            return null;
        }
        ArrayList arrayList = new ArrayList();
        if (list != null) {
            size = list.size();
        } else {
            size = 0;
        }
        for (int i = 0; i < size; i++) {
            Condition condition = list.get(i);
            if (condition.shouldShow()) {
                arrayList.add(condition);
            }
        }
        return arrayList;
    }

    private List<Suggestion> getSuggestionsToShow(List<Suggestion> list) {
        if (list == null) {
            return null;
        }
        if (list.size() <= 2) {
            return list;
        }
        ArrayList arrayList = new ArrayList(2);
        for (int i = 0; i < 2; i++) {
            arrayList.add(list.get(i));
        }
        return arrayList;
    }

    public static class Builder {
        private DashboardCategory mCategory;
        private boolean mConditionExpanded;
        private List<Condition> mConditions;
        private List<Suggestion> mSuggestions;

        public Builder() {
        }

        public Builder(DashboardData dashboardData) {
            this.mCategory = dashboardData.mCategory;
            this.mConditions = dashboardData.mConditions;
            this.mSuggestions = dashboardData.mSuggestions;
            this.mConditionExpanded = dashboardData.mConditionExpanded;
        }

        public Builder setCategory(DashboardCategory dashboardCategory) {
            this.mCategory = dashboardCategory;
            return this;
        }

        public Builder setConditions(List<Condition> list) {
            this.mConditions = list;
            return this;
        }

        public Builder setSuggestions(List<Suggestion> list) {
            this.mSuggestions = list;
            return this;
        }

        public Builder setConditionExpanded(boolean z) {
            this.mConditionExpanded = z;
            return this;
        }

        public DashboardData build() {
            return new DashboardData(this);
        }
    }

    public static class ItemsDataDiffCallback extends DiffUtil.Callback {
        private final List<Item> mNewItems;
        private final List<Item> mOldItems;

        public ItemsDataDiffCallback(List<Item> list, List<Item> list2) {
            this.mOldItems = list;
            this.mNewItems = list2;
        }

        @Override
        public int getOldListSize() {
            return this.mOldItems.size();
        }

        @Override
        public int getNewListSize() {
            return this.mNewItems.size();
        }

        @Override
        public boolean areItemsTheSame(int i, int i2) {
            return this.mOldItems.get(i).id == this.mNewItems.get(i2).id;
        }

        @Override
        public boolean areContentsTheSame(int i, int i2) {
            return this.mOldItems.get(i).equals(this.mNewItems.get(i2));
        }
    }

    static class Item {
        public final Object entity;
        public final int id;
        public final int type;

        public Item(Object obj, int i, int i2) {
            this.entity = obj;
            this.type = i;
            this.id = i2;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Item)) {
                return false;
            }
            Item item = (Item) obj;
            if (this.type != item.type || this.id != item.id) {
                return false;
            }
            int i = this.type;
            if (i == R.layout.condition_container) {
                List list = (List) this.entity;
                if (!list.isEmpty()) {
                    Object obj2 = list.get(0);
                    if ((obj2 instanceof Tile) && ((Tile) obj2).remoteViews != null) {
                        return false;
                    }
                }
            } else {
                if (i == R.layout.dashboard_tile) {
                    Tile tile = (Tile) this.entity;
                    Tile tile2 = (Tile) item.entity;
                    return TextUtils.equals(tile.title, tile2.title) && TextUtils.equals(tile.summary, tile2.summary);
                }
                if (i == R.layout.suggestion_container) {
                }
            }
            if (this.entity == null) {
                return item.entity == null;
            }
            return this.entity.equals(item.entity);
        }
    }

    public static class ConditionHeaderData {
        public final int conditionCount;
        public final List<Drawable> conditionIcons;
        public final CharSequence title;

        public ConditionHeaderData(List<Condition> list) {
            this.conditionCount = DashboardData.sizeOf(list);
            this.title = this.conditionCount > 0 ? list.get(0).getTitle() : null;
            this.conditionIcons = new ArrayList();
            for (int i = 0; list != null && i < list.size(); i++) {
                this.conditionIcons.add(list.get(i).getIcon());
            }
        }
    }
}
