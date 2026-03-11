package androidx.car.widget;

import androidx.car.widget.ListItem;
import androidx.car.widget.ListItem.ViewHolder;
import java.util.List;

public abstract class ListItemProvider<VH extends ListItem.ViewHolder> {
    public abstract ListItem<VH> get(int i);

    public abstract int size();

    public static class ListProvider<VH extends ListItem.ViewHolder> extends ListItemProvider {
        private final List<ListItem<VH>> mItems;

        public ListProvider(List<ListItem<VH>> items) {
            this.mItems = items;
        }

        @Override
        public ListItem<VH> get(int position) {
            return this.mItems.get(position);
        }

        @Override
        public int size() {
            return this.mItems.size();
        }
    }
}
