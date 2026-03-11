package com.android.settings.localepicker;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.LocaleList;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import com.android.internal.app.LocalePicker;
import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class LocaleDragAndDropAdapter extends RecyclerView.Adapter<CustomViewHolder> {
    private final Context mContext;
    private final List<LocaleStore.LocaleInfo> mFeedItemList;
    private final ItemTouchHelper mItemTouchHelper;
    private RecyclerView mParentView = null;
    private boolean mRemoveMode = false;
    private boolean mDragEnabled = true;
    private NumberFormat mNumberFormatter = NumberFormat.getNumberInstance();
    private LocaleList mLocalesToSetNext = null;
    private LocaleList mLocalesSetLast = null;

    class CustomViewHolder extends RecyclerView.ViewHolder implements View.OnTouchListener {
        private final LocaleDragCell mLocaleDragCell;

        public CustomViewHolder(LocaleDragCell view) {
            super(view);
            this.mLocaleDragCell = view;
            this.mLocaleDragCell.getDragHandle().setOnTouchListener(this);
        }

        public LocaleDragCell getLocaleDragCell() {
            return this.mLocaleDragCell;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (LocaleDragAndDropAdapter.this.mDragEnabled) {
                switch (MotionEventCompat.getActionMasked(event)) {
                    case DefaultWfcSettingsExt.RESUME:
                        LocaleDragAndDropAdapter.this.mItemTouchHelper.startDrag(this);
                        break;
                }
                return false;
            }
            return false;
        }
    }

    public LocaleDragAndDropAdapter(Context context, List<LocaleStore.LocaleInfo> feedItemList) {
        this.mFeedItemList = feedItemList;
        this.mContext = context;
        final float dragElevation = TypedValue.applyDimension(1, 8.0f, context.getResources().getDisplayMetrics());
        this.mItemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(3, 0) {
            private int mSelectionStatus = -1;

            @Override
            public boolean onMove(RecyclerView view, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
                LocaleDragAndDropAdapter.this.onItemMove(source.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                if (this.mSelectionStatus == -1) {
                    return;
                }
                viewHolder.itemView.setElevation(this.mSelectionStatus == 1 ? dragElevation : 0.0f);
                this.mSelectionStatus = -1;
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == 2) {
                    this.mSelectionStatus = 1;
                } else {
                    if (actionState != 0) {
                        return;
                    }
                    this.mSelectionStatus = 0;
                }
            }
        });
    }

    public void setRecyclerView(RecyclerView rv) {
        this.mParentView = rv;
        this.mItemTouchHelper.attachToRecyclerView(rv);
    }

    @Override
    public CustomViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        LocaleDragCell item = (LocaleDragCell) LayoutInflater.from(this.mContext).inflate(R.layout.locale_drag_cell, viewGroup, false);
        return new CustomViewHolder(item);
    }

    @Override
    public void onBindViewHolder(CustomViewHolder holder, int i) {
        LocaleStore.LocaleInfo feedItem = this.mFeedItemList.get(i);
        final LocaleDragCell dragCell = holder.getLocaleDragCell();
        String label = feedItem.getFullNameNative();
        String description = feedItem.getFullNameInUiLanguage();
        dragCell.setLabelAndDescription(label, description);
        dragCell.setLocalized(feedItem.isTranslated());
        dragCell.setMiniLabel(this.mNumberFormatter.format(i + 1));
        dragCell.setShowCheckbox(this.mRemoveMode);
        dragCell.setShowMiniLabel(!this.mRemoveMode);
        dragCell.setShowHandle(!this.mRemoveMode ? this.mDragEnabled : false);
        dragCell.setChecked(this.mRemoveMode ? feedItem.getChecked() : false);
        dragCell.setTag(feedItem);
        dragCell.getCheckbox().setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LocaleStore.LocaleInfo feedItem2 = (LocaleStore.LocaleInfo) dragCell.getTag();
                feedItem2.setChecked(isChecked);
            }
        });
    }

    @Override
    public int getItemCount() {
        int itemCount = this.mFeedItemList != null ? this.mFeedItemList.size() : 0;
        if (itemCount < 2 || this.mRemoveMode) {
            setDragEnabled(false);
        } else {
            setDragEnabled(true);
        }
        return itemCount;
    }

    void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition >= 0 && toPosition >= 0) {
            LocaleStore.LocaleInfo saved = this.mFeedItemList.get(fromPosition);
            this.mFeedItemList.remove(fromPosition);
            this.mFeedItemList.add(toPosition, saved);
        } else {
            Log.e("LocaleDragAndDropAdapter", String.format(Locale.US, "Negative position in onItemMove %d -> %d", Integer.valueOf(fromPosition), Integer.valueOf(toPosition)));
        }
        notifyItemChanged(fromPosition);
        notifyItemChanged(toPosition);
        notifyItemMoved(fromPosition, toPosition);
    }

    void setRemoveMode(boolean removeMode) {
        this.mRemoveMode = removeMode;
        int itemCount = this.mFeedItemList.size();
        for (int i = 0; i < itemCount; i++) {
            this.mFeedItemList.get(i).setChecked(false);
            notifyItemChanged(i);
        }
    }

    boolean isRemoveMode() {
        return this.mRemoveMode;
    }

    void removeItem(int position) {
        int itemCount = this.mFeedItemList.size();
        if (itemCount <= 1 || position < 0 || position >= itemCount) {
            return;
        }
        this.mFeedItemList.remove(position);
        notifyDataSetChanged();
    }

    void removeChecked() {
        int itemCount = this.mFeedItemList.size();
        for (int i = itemCount - 1; i >= 0; i--) {
            if (this.mFeedItemList.get(i).getChecked()) {
                this.mFeedItemList.remove(i);
            }
        }
        notifyDataSetChanged();
        doTheUpdate();
    }

    int getCheckedCount() {
        int result = 0;
        for (LocaleStore.LocaleInfo li : this.mFeedItemList) {
            if (li.getChecked()) {
                result++;
            }
        }
        return result;
    }

    void addLocale(LocaleStore.LocaleInfo li) {
        this.mFeedItemList.add(li);
        notifyItemInserted(this.mFeedItemList.size() - 1);
        doTheUpdate();
    }

    public void doTheUpdate() {
        int count = this.mFeedItemList.size();
        Locale[] newList = new Locale[count];
        for (int i = 0; i < count; i++) {
            LocaleStore.LocaleInfo li = this.mFeedItemList.get(i);
            newList[i] = li.getLocale();
        }
        LocaleList ll = new LocaleList(newList);
        updateLocalesWhenAnimationStops(ll);
    }

    public void updateLocalesWhenAnimationStops(LocaleList localeList) {
        if (localeList.equals(this.mLocalesToSetNext)) {
            return;
        }
        LocaleList.setDefault(localeList);
        this.mLocalesToSetNext = localeList;
        RecyclerView.ItemAnimator itemAnimator = this.mParentView.getItemAnimator();
        itemAnimator.isRunning(new RecyclerView.ItemAnimator.ItemAnimatorFinishedListener() {
            @Override
            public void onAnimationsFinished() {
                if (LocaleDragAndDropAdapter.this.mLocalesToSetNext == null || LocaleDragAndDropAdapter.this.mLocalesToSetNext.equals(LocaleDragAndDropAdapter.this.mLocalesSetLast)) {
                    return;
                }
                LocalePicker.updateLocales(LocaleDragAndDropAdapter.this.mLocalesToSetNext);
                LocaleDragAndDropAdapter.this.mLocalesSetLast = LocaleDragAndDropAdapter.this.mLocalesToSetNext;
                LocaleDragAndDropAdapter.this.mLocalesToSetNext = null;
                LocaleDragAndDropAdapter.this.mNumberFormatter = NumberFormat.getNumberInstance(Locale.getDefault());
            }
        });
    }

    private void setDragEnabled(boolean enabled) {
        this.mDragEnabled = enabled;
    }

    public void saveState(Bundle outInstanceState) {
        if (outInstanceState == null) {
            return;
        }
        ArrayList<String> selectedLocales = new ArrayList<>();
        for (LocaleStore.LocaleInfo li : this.mFeedItemList) {
            if (li.getChecked()) {
                selectedLocales.add(li.getId());
            }
        }
        outInstanceState.putStringArrayList("selectedLocales", selectedLocales);
    }

    public void restoreState(Bundle savedInstanceState) {
        ArrayList<String> selectedLocales;
        if (savedInstanceState == null || !this.mRemoveMode || (selectedLocales = savedInstanceState.getStringArrayList("selectedLocales")) == null || selectedLocales.isEmpty()) {
            return;
        }
        for (LocaleStore.LocaleInfo li : this.mFeedItemList) {
            li.setChecked(selectedLocales.contains(li.getId()));
        }
        notifyItemRangeChanged(0, this.mFeedItemList.size());
    }
}
