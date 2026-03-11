package com.android.setupwizardlib.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import com.android.setupwizardlib.DividerItemDecoration;
import com.android.setupwizardlib.R$styleable;

public class HeaderRecyclerView extends RecyclerView {
    private View mHeader;
    private int mHeaderRes;

    private static class HeaderViewHolder extends RecyclerView.ViewHolder implements DividerItemDecoration.DividedViewHolder {
        public HeaderViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public boolean isDividerAllowedAbove() {
            return false;
        }

        @Override
        public boolean isDividerAllowedBelow() {
            return false;
        }
    }

    public static class HeaderAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private RecyclerView.Adapter mAdapter;
        private View mHeader;
        private final RecyclerView.AdapterDataObserver mObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                HeaderAdapter.this.notifyDataSetChanged();
            }

            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                HeaderAdapter.this.notifyItemRangeChanged(positionStart, itemCount);
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                HeaderAdapter.this.notifyItemRangeInserted(positionStart, itemCount);
            }

            @Override
            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                HeaderAdapter.this.notifyDataSetChanged();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                HeaderAdapter.this.notifyItemRangeRemoved(positionStart, itemCount);
            }
        };

        public HeaderAdapter(RecyclerView.Adapter adapter) {
            this.mAdapter = adapter;
            this.mAdapter.registerAdapterDataObserver(this.mObserver);
            setHasStableIds(this.mAdapter.hasStableIds());
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == Integer.MAX_VALUE) {
                return new HeaderViewHolder(this.mHeader);
            }
            return this.mAdapter.onCreateViewHolder(parent, viewType);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (this.mHeader != null) {
                position--;
            }
            if (position < 0) {
                return;
            }
            this.mAdapter.onBindViewHolder(holder, position);
        }

        @Override
        public int getItemViewType(int position) {
            if (this.mHeader != null) {
                position--;
            }
            if (position < 0) {
                return Integer.MAX_VALUE;
            }
            return this.mAdapter.getItemViewType(position);
        }

        @Override
        public int getItemCount() {
            int count = this.mAdapter.getItemCount();
            if (this.mHeader != null) {
                return count + 1;
            }
            return count;
        }

        @Override
        public long getItemId(int position) {
            if (this.mHeader != null) {
                position--;
            }
            if (position < 0) {
                return Long.MAX_VALUE;
            }
            return this.mAdapter.getItemId(position);
        }

        public void setHeader(View header) {
            this.mHeader = header;
        }

        public RecyclerView.Adapter getWrappedAdapter() {
            return this.mAdapter;
        }
    }

    public HeaderRecyclerView(Context context) {
        super(context);
        init(null, 0);
    }

    public HeaderRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public HeaderRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    private void init(AttributeSet attrs, int defStyleAttr) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R$styleable.SuwHeaderRecyclerView, defStyleAttr, 0);
        this.mHeaderRes = a.getResourceId(R$styleable.SuwHeaderRecyclerView_suwHeader, 0);
        a.recycle();
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        int numberOfHeaders = this.mHeader != null ? 1 : 0;
        event.setItemCount(event.getItemCount() - numberOfHeaders);
        event.setFromIndex(Math.max(event.getFromIndex() - numberOfHeaders, 0));
        if (Build.VERSION.SDK_INT < 14) {
            return;
        }
        event.setToIndex(Math.max(event.getToIndex() - numberOfHeaders, 0));
    }

    public View getHeader() {
        return this.mHeader;
    }

    @Override
    public void setLayoutManager(RecyclerView.LayoutManager layout) {
        super.setLayoutManager(layout);
        if (layout == null || this.mHeader != null || this.mHeaderRes == 0) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(getContext());
        this.mHeader = inflater.inflate(this.mHeaderRes, (ViewGroup) this, false);
    }

    @Override
    public void setAdapter(RecyclerView.Adapter adapter) {
        if (this.mHeader != null && adapter != null) {
            HeaderAdapter headerAdapter = new HeaderAdapter(adapter);
            headerAdapter.setHeader(this.mHeader);
            adapter = headerAdapter;
        }
        super.setAdapter(adapter);
    }
}
