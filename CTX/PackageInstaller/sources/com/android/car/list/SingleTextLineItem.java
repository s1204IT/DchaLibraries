package com.android.car.list;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.car.list.TypedPagedListAdapter;

public class SingleTextLineItem extends TypedPagedListAdapter.LineItem<ViewHolder> {
    private final CharSequence mTitle;

    @Override
    public int getType() {
        return 8;
    }

    @Override
    public void bindViewHolder(ViewHolder viewHolder) {
        super.bindViewHolder(viewHolder);
        viewHolder.titleView.setText(this.mTitle);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView titleView;

        public ViewHolder(View view) {
            super(view);
            this.titleView = (TextView) view.findViewById(R.id.title);
        }
    }

    public static RecyclerView.ViewHolder createViewHolder(ViewGroup viewGroup) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.single_text_line_item, viewGroup, false));
    }

    @Override
    public CharSequence getDesc() {
        return null;
    }

    @Override
    public boolean isExpandable() {
        return false;
    }

    @Override
    public boolean isClickable() {
        return true;
    }
}
