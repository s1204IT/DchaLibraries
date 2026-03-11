package com.android.setupwizardlib.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.v7.preference.Preference;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import com.android.setupwizardlib.DividerItemDecoration;
import com.android.setupwizardlib.R;

public class HeaderRecyclerView extends RecyclerView {
    private View mHeader;
    private int mHeaderRes;

    private static class HeaderViewHolder extends RecyclerView.ViewHolder implements DividerItemDecoration.DividedViewHolder {
        HeaderViewHolder(View view) {
            super(view);
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

    public static class HeaderAdapter<CVH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private RecyclerView.Adapter<CVH> mAdapter;
        private View mHeader;
        private final RecyclerView.AdapterDataObserver mObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                HeaderAdapter.this.notifyDataSetChanged();
            }

            @Override
            public void onItemRangeChanged(int i, int i2) {
                if (HeaderAdapter.this.mHeader != null) {
                    i++;
                }
                HeaderAdapter.this.notifyItemRangeChanged(i, i2);
            }

            @Override
            public void onItemRangeInserted(int i, int i2) {
                if (HeaderAdapter.this.mHeader != null) {
                    i++;
                }
                HeaderAdapter.this.notifyItemRangeInserted(i, i2);
            }

            @Override
            public void onItemRangeMoved(int i, int i2, int i3) {
                if (HeaderAdapter.this.mHeader != null) {
                    i++;
                    i2++;
                }
                for (int i4 = 0; i4 < i3; i4++) {
                    HeaderAdapter.this.notifyItemMoved(i + i4, i2 + i4);
                }
            }

            @Override
            public void onItemRangeRemoved(int i, int i2) {
                if (HeaderAdapter.this.mHeader != null) {
                    i++;
                }
                HeaderAdapter.this.notifyItemRangeRemoved(i, i2);
            }
        };

        public HeaderAdapter(RecyclerView.Adapter<CVH> adapter) {
            this.mAdapter = adapter;
            this.mAdapter.registerAdapterDataObserver(this.mObserver);
            setHasStableIds(this.mAdapter.hasStableIds());
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            if (i == Integer.MAX_VALUE) {
                FrameLayout frameLayout = new FrameLayout(viewGroup.getContext());
                frameLayout.setLayoutParams(new FrameLayout.LayoutParams(-1, -2));
                return new HeaderViewHolder(frameLayout);
            }
            return this.mAdapter.onCreateViewHolder(viewGroup, i);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
            if (this.mHeader != null) {
                i--;
            }
            if (viewHolder instanceof HeaderViewHolder) {
                if (this.mHeader == null) {
                    throw new IllegalStateException("HeaderViewHolder cannot find mHeader");
                }
                if (this.mHeader.getParent() != null) {
                    ((ViewGroup) this.mHeader.getParent()).removeView(this.mHeader);
                }
                ((FrameLayout) viewHolder.itemView).addView(this.mHeader);
                return;
            }
            this.mAdapter.onBindViewHolder(viewHolder, i);
        }

        @Override
        public int getItemViewType(int i) {
            if (this.mHeader != null) {
                i--;
            }
            if (i < 0) {
                return Preference.DEFAULT_ORDER;
            }
            return this.mAdapter.getItemViewType(i);
        }

        @Override
        public int getItemCount() {
            int itemCount = this.mAdapter.getItemCount();
            if (this.mHeader != null) {
                return itemCount + 1;
            }
            return itemCount;
        }

        @Override
        public long getItemId(int i) {
            if (this.mHeader != null) {
                i--;
            }
            if (i < 0) {
                return Long.MAX_VALUE;
            }
            return this.mAdapter.getItemId(i);
        }

        public void setHeader(View view) {
            this.mHeader = view;
        }

        public RecyclerView.Adapter<CVH> getWrappedAdapter() {
            return this.mAdapter;
        }
    }

    public HeaderRecyclerView(Context context) {
        super(context);
        init(null, 0);
    }

    public HeaderRecyclerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(attributeSet, 0);
    }

    public HeaderRecyclerView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init(attributeSet, i);
    }

    private void init(AttributeSet attributeSet, int i) {
        TypedArray typedArrayObtainStyledAttributes = getContext().obtainStyledAttributes(attributeSet, R.styleable.SuwHeaderRecyclerView, i, 0);
        this.mHeaderRes = typedArrayObtainStyledAttributes.getResourceId(R.styleable.SuwHeaderRecyclerView_suwHeader, 0);
        typedArrayObtainStyledAttributes.recycle();
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEvent(accessibilityEvent);
        int i = this.mHeader != null ? 1 : 0;
        accessibilityEvent.setItemCount(accessibilityEvent.getItemCount() - i);
        accessibilityEvent.setFromIndex(Math.max(accessibilityEvent.getFromIndex() - i, 0));
        if (Build.VERSION.SDK_INT >= 14) {
            accessibilityEvent.setToIndex(Math.max(accessibilityEvent.getToIndex() - i, 0));
        }
    }

    public View getHeader() {
        return this.mHeader;
    }

    public void setHeader(View view) {
        this.mHeader = view;
    }

    @Override
    public void setLayoutManager(RecyclerView.LayoutManager layoutManager) {
        super.setLayoutManager(layoutManager);
        if (layoutManager != null && this.mHeader == null && this.mHeaderRes != 0) {
            this.mHeader = LayoutInflater.from(getContext()).inflate(this.mHeaderRes, (ViewGroup) this, false);
        }
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
