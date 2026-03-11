package com.android.settingslib.drawer;

import android.R;
import android.graphics.drawable.Icon;
import android.os.BenesseExtension;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settingslib.R$drawable;
import com.android.settingslib.R$id;
import com.android.settingslib.R$layout;
import com.android.settingslib.R$string;
import java.util.ArrayList;
import java.util.List;

public class SettingsDrawerAdapter extends BaseAdapter {
    private final SettingsDrawerActivity mActivity;
    private final ArrayList<Item> mItems = new ArrayList<>();

    public SettingsDrawerAdapter(SettingsDrawerActivity activity) {
        this.mActivity = activity;
    }

    void updateCategories() {
        Item item = null;
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        List<DashboardCategory> categories = this.mActivity.getDashboardCategories();
        this.mItems.clear();
        this.mItems.add(null);
        Item tile = new Item(item);
        tile.label = this.mActivity.getString(R$string.home);
        tile.icon = Icon.createWithResource(this.mActivity, R$drawable.home);
        this.mItems.add(tile);
        for (int i = 0; i < categories.size(); i++) {
            Item category = new Item(item);
            category.icon = null;
            DashboardCategory dashboardCategory = categories.get(i);
            category.label = dashboardCategory.title;
            this.mItems.add(category);
            for (int j = 0; j < dashboardCategory.tiles.size(); j++) {
                Item tile2 = new Item(item);
                Tile dashboardTile = dashboardCategory.tiles.get(j);
                tile2.label = dashboardTile.title;
                tile2.icon = dashboardTile.icon;
                tile2.tile = dashboardTile;
                this.mItems.add(tile2);
            }
        }
        notifyDataSetChanged();
    }

    public Tile getTile(int position) {
        if (this.mItems.get(position) != null) {
            return this.mItems.get(position).tile;
        }
        return null;
    }

    @Override
    public int getCount() {
        return this.mItems.size();
    }

    @Override
    public Object getItem(int position) {
        return this.mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean isEnabled(int position) {
        return (this.mItems.get(position) == null || this.mItems.get(position).icon == null) ? false : true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Item item = this.mItems.get(position);
        if (item == null) {
            if (convertView == null || convertView.getId() != R$id.spacer) {
                return LayoutInflater.from(this.mActivity).inflate(R$layout.drawer_spacer, parent, false);
            }
            return convertView;
        }
        if (convertView != null && convertView.getId() == R$id.spacer) {
            convertView = null;
        }
        boolean isTile = item.icon != null;
        if (convertView == null) {
            convertView = LayoutInflater.from(this.mActivity).inflate(isTile ? R$layout.drawer_item : R$layout.drawer_category, parent, false);
        } else {
            if (isTile != (convertView.getId() == R$id.tile_item)) {
            }
        }
        if (isTile) {
            ((ImageView) convertView.findViewById(R.id.icon)).setImageIcon(item.icon);
        }
        ((TextView) convertView.findViewById(R.id.title)).setText(item.label);
        return convertView;
    }

    private static class Item {
        public Icon icon;
        public CharSequence label;
        public Tile tile;

        Item(Item item) {
            this();
        }

        private Item() {
        }
    }
}
