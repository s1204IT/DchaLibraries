package com.android.car.list;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.car.list.TypedPagedListAdapter;

public abstract class TextLineItem extends TypedPagedListAdapter.LineItem<ViewHolder> {
    protected final CharSequence mTitle;

    @Override
    public int getType() {
        return 1;
    }

    @Override
    public void bindViewHolder(ViewHolder viewHolder) {
        super.bindViewHolder(viewHolder);
        viewHolder.titleView.setText(this.mTitle);
        viewHolder.descView.setText(getDesc());
        viewHolder.itemView.setEnabled(isEnabled());
        viewHolder.titleView.setEnabled(isEnabled());
        viewHolder.descView.setEnabled(isEnabled());
        viewHolder.rightArrow.setEnabled(isEnabled());
        int i = 4;
        viewHolder.rightArrow.setVisibility(isExpandable() ? 0 : 4);
        View view = viewHolder.dividerLine;
        if (isClickable() && isEnabled()) {
            i = 0;
        }
        view.setVisibility(i);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView descView;
        public final View dividerLine;
        final ImageView rightArrow;
        public final TextView titleView;

        public ViewHolder(View view) {
            super(view);
            this.titleView = (TextView) view.findViewById(R.id.title);
            this.descView = (TextView) view.findViewById(R.id.desc);
            this.rightArrow = (ImageView) view.findViewById(R.id.right_chevron);
            this.dividerLine = view.findViewById(R.id.line_item_divider);
        }
    }

    public static RecyclerView.ViewHolder createViewHolder(ViewGroup viewGroup) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.text_line_item, viewGroup, false));
    }
}
