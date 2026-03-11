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
import com.android.settings.shortcut.CreateShortcut;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
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

        public CustomViewHolder(LocaleDragCell localeDragCell) {
            super(localeDragCell);
            this.mLocaleDragCell = localeDragCell;
            this.mLocaleDragCell.getDragHandle().setOnTouchListener(this);
        }

        public LocaleDragCell getLocaleDragCell() {
            return this.mLocaleDragCell;
        }

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (LocaleDragAndDropAdapter.this.mDragEnabled && MotionEventCompat.getActionMasked(motionEvent) == 0) {
                LocaleDragAndDropAdapter.this.mItemTouchHelper.startDrag(this);
                return false;
            }
            return false;
        }
    }

    public LocaleDragAndDropAdapter(Context context, List<LocaleStore.LocaleInfo> list) {
        this.mFeedItemList = list;
        this.mContext = context;
        final float fApplyDimension = TypedValue.applyDimension(1, 8.0f, context.getResources().getDisplayMetrics());
        this.mItemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(3, 0) {
            private int mSelectionStatus = -1;

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder viewHolder2) {
                LocaleDragAndDropAdapter.this.onItemMove(viewHolder.getAdapterPosition(), viewHolder2.getAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {
            }

            @Override
            public void onChildDraw(Canvas canvas, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float f, float f2, int i, boolean z) {
                super.onChildDraw(canvas, recyclerView, viewHolder, f, f2, i, z);
                if (this.mSelectionStatus != -1) {
                    viewHolder.itemView.setElevation(this.mSelectionStatus == 1 ? fApplyDimension : 0.0f);
                    this.mSelectionStatus = -1;
                }
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int i) {
                super.onSelectedChanged(viewHolder, i);
                if (i == 2) {
                    this.mSelectionStatus = 1;
                } else if (i == 0) {
                    this.mSelectionStatus = 0;
                }
            }
        });
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        this.mParentView = recyclerView;
        this.mItemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public CustomViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new CustomViewHolder((LocaleDragCell) LayoutInflater.from(this.mContext).inflate(R.layout.locale_drag_cell, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(CustomViewHolder customViewHolder, int i) {
        LocaleStore.LocaleInfo localeInfo = this.mFeedItemList.get(i);
        final LocaleDragCell localeDragCell = customViewHolder.getLocaleDragCell();
        localeDragCell.setLabelAndDescription(localeInfo.getFullNameNative(), localeInfo.getFullNameInUiLanguage());
        localeDragCell.setLocalized(localeInfo.isTranslated());
        localeDragCell.setMiniLabel(this.mNumberFormatter.format(i + 1));
        localeDragCell.setShowCheckbox(this.mRemoveMode);
        localeDragCell.setShowMiniLabel(!this.mRemoveMode);
        localeDragCell.setShowHandle(!this.mRemoveMode && this.mDragEnabled);
        localeDragCell.setChecked(this.mRemoveMode ? localeInfo.getChecked() : false);
        localeDragCell.setTag(localeInfo);
        localeDragCell.getCheckbox().setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                ((LocaleStore.LocaleInfo) localeDragCell.getTag()).setChecked(z);
            }
        });
    }

    @Override
    public int getItemCount() {
        int size = this.mFeedItemList != null ? this.mFeedItemList.size() : 0;
        if (size < 2 || this.mRemoveMode) {
            setDragEnabled(false);
        } else {
            setDragEnabled(true);
        }
        return size;
    }

    void onItemMove(int i, int i2) {
        if (i >= 0 && i2 >= 0) {
            LocaleStore.LocaleInfo localeInfo = this.mFeedItemList.get(i);
            this.mFeedItemList.remove(i);
            this.mFeedItemList.add(i2, localeInfo);
        } else {
            Log.e("LocaleDragAndDropAdapter", String.format(Locale.US, "Negative position in onItemMove %d -> %d", Integer.valueOf(i), Integer.valueOf(i2)));
        }
        notifyItemChanged(i);
        notifyItemChanged(i2);
        notifyItemMoved(i, i2);
    }

    void setRemoveMode(boolean z) {
        this.mRemoveMode = z;
        int size = this.mFeedItemList.size();
        for (int i = 0; i < size; i++) {
            this.mFeedItemList.get(i).setChecked(false);
            notifyItemChanged(i);
        }
    }

    boolean isRemoveMode() {
        return this.mRemoveMode;
    }

    void removeItem(int i) {
        int size = this.mFeedItemList.size();
        if (size <= 1 || i < 0 || i >= size) {
            return;
        }
        this.mFeedItemList.remove(i);
        notifyDataSetChanged();
    }

    void removeChecked() {
        for (int size = this.mFeedItemList.size() - 1; size >= 0; size--) {
            if (this.mFeedItemList.get(size).getChecked()) {
                this.mFeedItemList.remove(size);
            }
        }
        notifyDataSetChanged();
        doTheUpdate();
    }

    int getCheckedCount() {
        Iterator<LocaleStore.LocaleInfo> it = this.mFeedItemList.iterator();
        int i = 0;
        while (it.hasNext()) {
            if (it.next().getChecked()) {
                i++;
            }
        }
        return i;
    }

    void addLocale(LocaleStore.LocaleInfo localeInfo) {
        this.mFeedItemList.add(localeInfo);
        notifyItemInserted(this.mFeedItemList.size() - 1);
        doTheUpdate();
    }

    public void doTheUpdate() {
        int size = this.mFeedItemList.size();
        Locale[] localeArr = new Locale[size];
        for (int i = 0; i < size; i++) {
            localeArr[i] = this.mFeedItemList.get(i).getLocale();
        }
        updateLocalesWhenAnimationStops(new LocaleList(localeArr));
    }

    public void updateLocalesWhenAnimationStops(LocaleList localeList) {
        if (localeList.equals(this.mLocalesToSetNext)) {
            return;
        }
        LocaleList.setDefault(localeList);
        this.mLocalesToSetNext = localeList;
        this.mParentView.getItemAnimator().isRunning(new RecyclerView.ItemAnimator.ItemAnimatorFinishedListener() {
            @Override
            public void onAnimationsFinished() {
                if (LocaleDragAndDropAdapter.this.mLocalesToSetNext != null && !LocaleDragAndDropAdapter.this.mLocalesToSetNext.equals(LocaleDragAndDropAdapter.this.mLocalesSetLast)) {
                    LocalePicker.updateLocales(LocaleDragAndDropAdapter.this.mLocalesToSetNext);
                    LocaleDragAndDropAdapter.this.mLocalesSetLast = LocaleDragAndDropAdapter.this.mLocalesToSetNext;
                    new CreateShortcut.ShortcutsUpdateTask(LocaleDragAndDropAdapter.this.mContext).execute(new Void[0]);
                    LocaleDragAndDropAdapter.this.mLocalesToSetNext = null;
                    LocaleDragAndDropAdapter.this.mNumberFormatter = NumberFormat.getNumberInstance(Locale.getDefault());
                }
            }
        });
    }

    private void setDragEnabled(boolean z) {
        this.mDragEnabled = z;
    }

    public void saveState(Bundle bundle) {
        if (bundle != null) {
            ArrayList<String> arrayList = new ArrayList<>();
            for (LocaleStore.LocaleInfo localeInfo : this.mFeedItemList) {
                if (localeInfo.getChecked()) {
                    arrayList.add(localeInfo.getId());
                }
            }
            bundle.putStringArrayList("selectedLocales", arrayList);
        }
    }

    public void restoreState(Bundle bundle) {
        ArrayList<String> stringArrayList;
        if (bundle == null || !this.mRemoveMode || (stringArrayList = bundle.getStringArrayList("selectedLocales")) == null || stringArrayList.isEmpty()) {
            return;
        }
        for (LocaleStore.LocaleInfo localeInfo : this.mFeedItemList) {
            localeInfo.setChecked(stringArrayList.contains(localeInfo.getId()));
        }
        notifyItemRangeChanged(0, this.mFeedItemList.size());
    }
}
