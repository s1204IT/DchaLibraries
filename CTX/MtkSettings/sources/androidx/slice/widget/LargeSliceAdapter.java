package androidx.slice.widget;

import android.content.Context;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.slice.SliceItem;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceQuery;
import androidx.slice.view.R;
import androidx.slice.widget.SliceView;
import java.util.ArrayList;
import java.util.List;

public class LargeSliceAdapter extends RecyclerView.Adapter<SliceViewHolder> {
    private AttributeSet mAttrs;
    private int mColor;
    private final Context mContext;
    private int mDefStyleAttr;
    private int mDefStyleRes;
    private long mLastUpdated;
    private SliceView mParent;
    private boolean mShowLastUpdated;
    private List<SliceAction> mSliceActions;
    private SliceView.OnSliceActionListener mSliceObserver;
    private LargeTemplateView mTemplateView;
    private final IdGenerator mIdGen = new IdGenerator();
    private List<SliceWrapper> mSlices = new ArrayList();

    public LargeSliceAdapter(Context context) {
        this.mContext = context;
        setHasStableIds(true);
    }

    public void setParents(SliceView parent, LargeTemplateView templateView) {
        this.mParent = parent;
        this.mTemplateView = templateView;
    }

    public void setSliceObserver(SliceView.OnSliceActionListener observer) {
        this.mSliceObserver = observer;
    }

    public void setSliceActions(List<SliceAction> actions) {
        this.mSliceActions = actions;
        notifyHeaderChanged();
    }

    public void setSliceItems(List<SliceItem> slices, int color, int mode) {
        if (slices == null) {
            this.mSlices.clear();
        } else {
            this.mIdGen.resetUsage();
            this.mSlices = new ArrayList(slices.size());
            for (SliceItem s : slices) {
                this.mSlices.add(new SliceWrapper(s, this.mIdGen, mode));
            }
        }
        this.mColor = color;
        notifyDataSetChanged();
    }

    public void setStyle(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this.mAttrs = attrs;
        this.mDefStyleAttr = defStyleAttr;
        this.mDefStyleRes = defStyleRes;
        notifyDataSetChanged();
    }

    public void setShowLastUpdated(boolean showLastUpdated) {
        this.mShowLastUpdated = showLastUpdated;
        notifyHeaderChanged();
    }

    public void setLastUpdated(long lastUpdated) {
        this.mLastUpdated = lastUpdated;
        notifyHeaderChanged();
    }

    @Override
    public SliceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = inflateForType(viewType);
        v.setLayoutParams(new ViewGroup.LayoutParams(-1, -2));
        return new SliceViewHolder(v);
    }

    @Override
    public int getItemViewType(int position) {
        return this.mSlices.get(position).mType;
    }

    @Override
    public long getItemId(int position) {
        return this.mSlices.get(position).mId;
    }

    @Override
    public int getItemCount() {
        return this.mSlices.size();
    }

    @Override
    public void onBindViewHolder(SliceViewHolder holder, int position) {
        SliceWrapper slice = this.mSlices.get(position);
        holder.bind(slice.mItem, position);
    }

    private void notifyHeaderChanged() {
        if (getItemCount() > 0) {
            notifyItemChanged(0);
        }
    }

    private View inflateForType(int viewType) {
        View v = new RowView(this.mContext);
        switch (viewType) {
            case 3:
                View v2 = LayoutInflater.from(this.mContext).inflate(R.layout.abc_slice_grid, (ViewGroup) null);
                return v2;
            case 4:
                View v3 = LayoutInflater.from(this.mContext).inflate(R.layout.abc_slice_message, (ViewGroup) null);
                return v3;
            case 5:
                View v4 = LayoutInflater.from(this.mContext).inflate(R.layout.abc_slice_message_local, (ViewGroup) null);
                return v4;
            default:
                return v;
        }
    }

    protected static class SliceWrapper {
        private final long mId;
        private final SliceItem mItem;
        private final int mType;

        public SliceWrapper(SliceItem item, IdGenerator idGen, int mode) {
            this.mItem = item;
            this.mType = getFormat(item);
            this.mId = idGen.getId(item, mode);
        }

        public static int getFormat(SliceItem item) {
            if ("message".equals(item.getSubType())) {
                if (SliceQuery.findSubtype(item, (String) null, "source") != null) {
                    return 4;
                }
                return 5;
            }
            if (item.hasHint("horizontal")) {
                return 3;
            }
            if (!item.hasHint("list_item")) {
                return 2;
            }
            return 1;
        }
    }

    public class SliceViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnTouchListener {
        public final SliceChildView mSliceChildView;

        public SliceViewHolder(View itemView) {
            super(itemView);
            this.mSliceChildView = itemView instanceof SliceChildView ? (SliceChildView) itemView : null;
        }

        void bind(SliceItem item, int position) {
            if (this.mSliceChildView == null || item == null) {
                return;
            }
            this.mSliceChildView.setOnClickListener(this);
            this.mSliceChildView.setOnTouchListener(this);
            boolean isHeader = position == 0;
            int mode = LargeSliceAdapter.this.mParent != null ? LargeSliceAdapter.this.mParent.getMode() : 2;
            this.mSliceChildView.setMode(mode);
            this.mSliceChildView.setTint(LargeSliceAdapter.this.mColor);
            this.mSliceChildView.setStyle(LargeSliceAdapter.this.mAttrs, LargeSliceAdapter.this.mDefStyleAttr, LargeSliceAdapter.this.mDefStyleRes);
            this.mSliceChildView.setSliceItem(item, isHeader, position, LargeSliceAdapter.this.getItemCount(), LargeSliceAdapter.this.mSliceObserver);
            this.mSliceChildView.setSliceActions(isHeader ? LargeSliceAdapter.this.mSliceActions : null);
            this.mSliceChildView.setLastUpdated(isHeader ? LargeSliceAdapter.this.mLastUpdated : -1L);
            this.mSliceChildView.setShowLastUpdated(isHeader && LargeSliceAdapter.this.mShowLastUpdated);
            if (this.mSliceChildView instanceof RowView) {
                ((RowView) this.mSliceChildView).setSingleItem(LargeSliceAdapter.this.getItemCount() == 1);
            }
            int[] info = {ListContent.getRowType(LargeSliceAdapter.this.mContext, item, isHeader, LargeSliceAdapter.this.mSliceActions), position};
            this.mSliceChildView.setTag(info);
        }

        @Override
        public void onClick(View v) {
            if (LargeSliceAdapter.this.mParent != null) {
                LargeSliceAdapter.this.mParent.setClickInfo((int[]) v.getTag());
                LargeSliceAdapter.this.mParent.performClick();
            }
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (LargeSliceAdapter.this.mTemplateView != null) {
                LargeSliceAdapter.this.mTemplateView.onForegroundActivated(event);
                return false;
            }
            return false;
        }
    }

    private static class IdGenerator {
        private final ArrayMap<String, Long> mCurrentIds;
        private long mNextLong;
        private final ArrayMap<String, Integer> mUsedIds;

        private IdGenerator() {
            this.mNextLong = 0L;
            this.mCurrentIds = new ArrayMap<>();
            this.mUsedIds = new ArrayMap<>();
        }

        public long getId(SliceItem item, int mode) {
            String str = genString(item);
            SliceItem summary = SliceQuery.find(item, (String) null, "summary", (String) null);
            if (summary != null) {
                str = str + mode;
            }
            if (!this.mCurrentIds.containsKey(str)) {
                ArrayMap<String, Long> arrayMap = this.mCurrentIds;
                long j = this.mNextLong;
                this.mNextLong = 1 + j;
                arrayMap.put(str, Long.valueOf(j));
            }
            long id = this.mCurrentIds.get(str).longValue();
            Integer usedIdIndex = this.mUsedIds.get(str);
            int index = usedIdIndex != null ? usedIdIndex.intValue() : 0;
            this.mUsedIds.put(str, Integer.valueOf(index + 1));
            return ((long) (index * 10000)) + id;
        }

        private String genString(SliceItem item) {
            if ("slice".equals(item.getFormat()) || "action".equals(item.getFormat())) {
                return String.valueOf(item.getSlice().getItems().size());
            }
            return item.toString();
        }

        public void resetUsage() {
            this.mUsedIds.clear();
        }
    }
}
