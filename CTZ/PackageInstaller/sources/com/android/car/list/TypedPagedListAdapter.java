package com.android.car.list;

import android.support.v4.app.DialogFragment;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import androidx.car.widget.PagedListView;
import java.util.ArrayList;

public class TypedPagedListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements PagedListView.ItemCap {
    private ArrayList<LineItem> mContentList;

    public TypedPagedListAdapter() {
        this(new ArrayList());
    }

    public TypedPagedListAdapter(ArrayList<LineItem> arrayList) {
        this.mContentList = arrayList;
    }

    public static abstract class LineItem<VH extends RecyclerView.ViewHolder> {
        public abstract CharSequence getDesc();

        abstract int getType();

        public abstract boolean isExpandable();

        void bindViewHolder(VH vh) {
            vh.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view) {
                    this.f$0.onClick(view);
                }
            });
        }

        public boolean isEnabled() {
            return true;
        }

        public boolean isClickable() {
            return isExpandable();
        }

        public void onClick(View view) {
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        switch (i) {
            case DialogFragment.STYLE_NO_TITLE:
                return TextLineItem.createViewHolder(viewGroup);
            case DialogFragment.STYLE_NO_FRAME:
                return ToggleLineItem.createViewHolder(viewGroup);
            case DialogFragment.STYLE_NO_INPUT:
                return IconTextLineItem.createViewHolder(viewGroup);
            case 4:
                return SeekbarLineItem.createViewHolder(viewGroup);
            case 5:
                return IconToggleLineItem.createViewHolder(viewGroup);
            case 6:
                return CheckBoxLineItem.createViewHolder(viewGroup);
            case 7:
                return EditTextLineItem.createViewHolder(viewGroup);
            case 8:
                return SingleTextLineItem.createViewHolder(viewGroup);
            case 9:
                return SpinnerLineItem.createViewHolder(viewGroup);
            case 10:
                return PasswordLineItem.createViewHolder(viewGroup);
            case 11:
                return ActionIconButtonLineItem.createViewHolder(viewGroup);
            default:
                throw new IllegalStateException("ViewType not supported: " + i);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        this.mContentList.get(i).bindViewHolder(viewHolder);
    }

    @Override
    public int getItemViewType(int i) {
        return this.mContentList.get(i).getType();
    }

    @Override
    public int getItemCount() {
        return this.mContentList.size();
    }

    @Override
    public void setMaxItems(int i) {
    }
}
