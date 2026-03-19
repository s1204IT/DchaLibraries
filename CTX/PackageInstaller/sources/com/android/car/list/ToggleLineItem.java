package com.android.car.list;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import com.android.car.list.TypedPagedListAdapter;

public abstract class ToggleLineItem extends TypedPagedListAdapter.LineItem<ToggleLineItemViewHolder> {

    public static class ToggleLineItemViewHolder extends RecyclerView.ViewHolder {
        public final TextView descView;
        public final TextView titleView;
        public final Switch toggle;

        public ToggleLineItemViewHolder(View view) {
            super(view);
            this.titleView = (TextView) view.findViewById(R.id.title);
            this.descView = (TextView) view.findViewById(R.id.desc);
            this.toggle = (Switch) view.findViewById(R.id.toggle_switch);
        }
    }

    public static RecyclerView.ViewHolder createViewHolder(ViewGroup viewGroup) {
        return new ToggleLineItemViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.toggle_line_item, viewGroup, false));
    }
}
