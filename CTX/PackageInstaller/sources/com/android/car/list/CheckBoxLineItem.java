package com.android.car.list;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import com.android.car.list.TypedPagedListAdapter;

public abstract class CheckBoxLineItem extends TypedPagedListAdapter.LineItem<CheckboxLineItemViewHolder> {

    public static class CheckboxLineItemViewHolder extends RecyclerView.ViewHolder {
        public final CheckBox checkbox;
        final TextView titleView;

        public CheckboxLineItemViewHolder(View view) {
            super(view);
            this.titleView = (TextView) view.findViewById(R.id.title);
            this.checkbox = (CheckBox) view.findViewById(R.id.checkbox);
        }
    }

    public static RecyclerView.ViewHolder createViewHolder(ViewGroup viewGroup) {
        return new CheckboxLineItemViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.checkbox_line_item, viewGroup, false));
    }
}
