package com.android.car.list;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.car.list.TypedPagedListAdapter;

public abstract class IconTextLineItem extends TypedPagedListAdapter.LineItem<ViewHolder> {
    private final CharSequence mTitle;

    public abstract void setIcon(ImageView imageView);

    @Override
    public int getType() {
        return 3;
    }

    @Override
    public void bindViewHolder(ViewHolder viewHolder) {
        super.bindViewHolder(viewHolder);
        viewHolder.titleView.setText(this.mTitle);
        setIcon(viewHolder.iconView);
        CharSequence desc = getDesc();
        if (TextUtils.isEmpty(desc)) {
            viewHolder.descView.setVisibility(8);
        } else {
            viewHolder.descView.setVisibility(0);
            viewHolder.descView.setText(desc);
        }
        viewHolder.rightArrow.setVisibility(isExpandable() ? 0 : 4);
        viewHolder.dividerLine.setVisibility((isClickable() && isEnabled()) ? 0 : 4);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView descView;
        public final View dividerLine;
        final ImageView iconView;
        final ImageView rightArrow;
        public final TextView titleView;

        public ViewHolder(View view) {
            super(view);
            this.iconView = (ImageView) view.findViewById(R.id.icon);
            this.titleView = (TextView) view.findViewById(R.id.title);
            this.descView = (TextView) view.findViewById(R.id.desc);
            this.rightArrow = (ImageView) view.findViewById(R.id.right_chevron);
            this.dividerLine = view.findViewById(R.id.line_item_divider);
        }
    }

    public static RecyclerView.ViewHolder createViewHolder(ViewGroup viewGroup) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.icon_text_line_item, viewGroup, false));
    }
}
