package com.android.car.list;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import com.android.car.list.TypedPagedListAdapter;

public class SpinnerLineItem<T> extends TypedPagedListAdapter.LineItem<ViewHolder> {
    private final ArrayAdapter<T> mArrayAdapter;
    private final AdapterView.OnItemSelectedListener mOnItemSelectedListener;
    private final int mSelectedPosition;
    private final CharSequence mTitle;

    @Override
    public int getType() {
        return 9;
    }

    @Override
    public void bindViewHolder(ViewHolder viewHolder) {
        super.bindViewHolder(viewHolder);
        viewHolder.spinner.setAdapter((SpinnerAdapter) this.mArrayAdapter);
        viewHolder.spinner.setSelection(this.mSelectedPosition);
        viewHolder.spinner.setOnItemSelectedListener(this.mOnItemSelectedListener);
        viewHolder.titleView.setText(this.mTitle);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final Spinner spinner;
        public final TextView titleView;

        public ViewHolder(View view) {
            super(view);
            this.spinner = (Spinner) view.findViewById(R.id.spinner);
            this.titleView = (TextView) view.findViewById(R.id.title);
        }
    }

    public static RecyclerView.ViewHolder createViewHolder(ViewGroup viewGroup) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.spinner_line_item, viewGroup, false));
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
