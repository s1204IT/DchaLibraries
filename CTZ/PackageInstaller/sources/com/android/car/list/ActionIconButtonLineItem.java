package com.android.car.list;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.car.list.TypedPagedListAdapter;

public abstract class ActionIconButtonLineItem extends TypedPagedListAdapter.LineItem<ViewHolder> {

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final Button mActionButton1;
        final Button mActionButton2;
        final TextView mDescView;
        final ImageView mEndIconView;
        final TextView mTitleView;
        final View mView;

        public ViewHolder(View view) {
            super(view);
            this.mView = view;
            this.mEndIconView = (ImageView) this.mView.findViewById(R.id.end_icon);
            this.mTitleView = (TextView) this.mView.findViewById(R.id.title);
            this.mDescView = (TextView) this.mView.findViewById(R.id.desc);
            this.mActionButton1 = (Button) this.mView.findViewById(R.id.action_button_1);
            this.mActionButton2 = (Button) this.mView.findViewById(R.id.action_button_2);
        }
    }

    public static RecyclerView.ViewHolder createViewHolder(ViewGroup viewGroup) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.action_icon_button_line_item, viewGroup, false));
    }
}
