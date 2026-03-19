package com.android.car.list;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.android.car.list.TypedPagedListAdapter;

public abstract class SeekbarLineItem extends TypedPagedListAdapter.LineItem<ViewHolder> {

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView iconView;
        final SeekBar seekBar;
        final TextView titleView;

        public ViewHolder(View view) {
            super(view);
            this.titleView = (TextView) view.findViewById(R.id.title);
            this.seekBar = (SeekBar) view.findViewById(R.id.seekbar);
            this.iconView = (ImageView) view.findViewById(R.id.icon);
        }
    }

    public static RecyclerView.ViewHolder createViewHolder(ViewGroup viewGroup) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.seekbar_line_item, viewGroup, false));
    }
}
