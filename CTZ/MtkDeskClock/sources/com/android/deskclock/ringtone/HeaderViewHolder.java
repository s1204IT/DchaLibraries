package com.android.deskclock.ringtone;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.deskclock.ItemAdapter;
import com.android.deskclock.R;

final class HeaderViewHolder extends ItemAdapter.ItemViewHolder<HeaderHolder> {
    static final int VIEW_TYPE_ITEM_HEADER = 2131558499;
    private final TextView mItemHeader;

    private HeaderViewHolder(View view) {
        super(view);
        this.mItemHeader = (TextView) view.findViewById(R.id.ringtone_item_header);
    }

    @Override
    protected void onBindItemView(HeaderHolder headerHolder) {
        this.mItemHeader.setText(headerHolder.getTextResId());
    }

    public static class Factory implements ItemAdapter.ItemViewHolder.Factory {
        private final LayoutInflater mInflater;

        Factory(LayoutInflater layoutInflater) {
            this.mInflater = layoutInflater;
        }

        @Override
        public ItemAdapter.ItemViewHolder<?> createViewHolder(ViewGroup viewGroup, int i) {
            return new HeaderViewHolder(this.mInflater.inflate(i, viewGroup, false));
        }
    }
}
