package com.android.systemui.qs.customize;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSTileHost;
import com.android.systemui.qs.customize.TileQueryHelper;
import com.android.systemui.qs.external.CustomTile;
import com.android.systemui.qs.tileimpl.QSIconViewImpl;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import java.util.ArrayList;
import java.util.List;

public class TileAdapter extends RecyclerView.Adapter<Holder> implements TileQueryHelper.TileStateListener {
    private int mAccessibilityFromIndex;
    private final AccessibilityManager mAccessibilityManager;
    private List<TileQueryHelper.TileInfo> mAllTiles;
    private final Context mContext;
    private Holder mCurrentDrag;
    private List<String> mCurrentSpecs;
    private final RecyclerView.ItemDecoration mDecoration;
    private int mEditIndex;
    private QSTileHost mHost;
    private boolean mNeedsFocus;
    private List<TileQueryHelper.TileInfo> mOtherTiles;
    private int mTileDividerIndex;
    private final Handler mHandler = new Handler();
    private final List<TileQueryHelper.TileInfo> mTiles = new ArrayList();
    private int mAccessibilityAction = 0;
    private final GridLayoutManager.SpanSizeLookup mSizeLookup = new GridLayoutManager.SpanSizeLookup() {
        @Override
        public int getSpanSize(int i) {
            int itemViewType = TileAdapter.this.getItemViewType(i);
            return (itemViewType == 1 || itemViewType == 4) ? 3 : 1;
        }
    };
    private final ItemTouchHelper.Callback mCallbacks = new ItemTouchHelper.Callback() {
        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return false;
        }

        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int i) {
            super.onSelectedChanged(viewHolder, i);
            if (i != 2) {
                viewHolder = null;
            }
            if (viewHolder == TileAdapter.this.mCurrentDrag) {
                return;
            }
            if (TileAdapter.this.mCurrentDrag != null) {
                int adapterPosition = TileAdapter.this.mCurrentDrag.getAdapterPosition();
                TileAdapter.this.mCurrentDrag.mTileView.setShowAppLabel(adapterPosition > TileAdapter.this.mEditIndex && !((TileQueryHelper.TileInfo) TileAdapter.this.mTiles.get(adapterPosition)).isSystem);
                TileAdapter.this.mCurrentDrag.stopDrag();
                TileAdapter.this.mCurrentDrag = null;
            }
            if (viewHolder != null) {
                TileAdapter.this.mCurrentDrag = (Holder) viewHolder;
                TileAdapter.this.mCurrentDrag.startDrag();
            }
            TileAdapter.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    TileAdapter.this.notifyItemChanged(TileAdapter.this.mEditIndex);
                }
            });
        }

        @Override
        public boolean canDropOver(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder viewHolder2) {
            return (TileAdapter.this.canRemoveTiles() || viewHolder.getAdapterPosition() >= TileAdapter.this.mEditIndex) ? viewHolder2.getAdapterPosition() <= TileAdapter.this.mEditIndex + 1 : viewHolder2.getAdapterPosition() < TileAdapter.this.mEditIndex;
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            if (viewHolder.getItemViewType() == 1 || viewHolder.getItemViewType() == 4) {
                return makeMovementFlags(0, 0);
            }
            return makeMovementFlags(15, 0);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder viewHolder2) {
            return TileAdapter.this.move(viewHolder.getAdapterPosition(), viewHolder2.getAdapterPosition(), viewHolder2.itemView);
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {
        }
    };
    private final ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(this.mCallbacks);

    public TileAdapter(Context context) {
        this.mContext = context;
        this.mAccessibilityManager = (AccessibilityManager) context.getSystemService(AccessibilityManager.class);
        this.mDecoration = new TileItemDecoration(context);
    }

    public void setHost(QSTileHost qSTileHost) {
        this.mHost = qSTileHost;
    }

    public ItemTouchHelper getItemTouchHelper() {
        return this.mItemTouchHelper;
    }

    public RecyclerView.ItemDecoration getItemDecoration() {
        return this.mDecoration;
    }

    public void saveSpecs(QSTileHost qSTileHost) {
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < this.mTiles.size() && this.mTiles.get(i) != null; i++) {
            arrayList.add(this.mTiles.get(i).spec);
        }
        qSTileHost.changeTiles(this.mCurrentSpecs, arrayList);
        this.mCurrentSpecs = arrayList;
    }

    public void resetTileSpecs(QSTileHost qSTileHost, List<String> list) {
        qSTileHost.changeTiles(this.mCurrentSpecs, list);
        setTileSpecs(list);
    }

    public void setTileSpecs(List<String> list) {
        if (list.equals(this.mCurrentSpecs)) {
            return;
        }
        this.mCurrentSpecs = list;
        recalcSpecs();
    }

    @Override
    public void onTilesChanged(List<TileQueryHelper.TileInfo> list) {
        this.mAllTiles = list;
        recalcSpecs();
    }

    private void recalcSpecs() {
        if (this.mCurrentSpecs == null || this.mAllTiles == null) {
            return;
        }
        this.mOtherTiles = new ArrayList(this.mAllTiles);
        this.mTiles.clear();
        int i = 0;
        for (int i2 = 0; i2 < this.mCurrentSpecs.size(); i2++) {
            TileQueryHelper.TileInfo andRemoveOther = getAndRemoveOther(this.mCurrentSpecs.get(i2));
            if (andRemoveOther != null) {
                this.mTiles.add(andRemoveOther);
            }
        }
        this.mTiles.add(null);
        while (i < this.mOtherTiles.size()) {
            TileQueryHelper.TileInfo tileInfo = this.mOtherTiles.get(i);
            if (tileInfo.isSystem) {
                this.mOtherTiles.remove(i);
                this.mTiles.add(tileInfo);
                i--;
            }
            i++;
        }
        this.mTileDividerIndex = this.mTiles.size();
        this.mTiles.add(null);
        this.mTiles.addAll(this.mOtherTiles);
        updateDividerLocations();
        notifyDataSetChanged();
    }

    private TileQueryHelper.TileInfo getAndRemoveOther(String str) {
        for (int i = 0; i < this.mOtherTiles.size(); i++) {
            if (this.mOtherTiles.get(i).spec.equals(str)) {
                return this.mOtherTiles.remove(i);
            }
        }
        return null;
    }

    @Override
    public int getItemViewType(int i) {
        if (this.mAccessibilityAction == 1 && i == this.mEditIndex - 1) {
            return 2;
        }
        if (i == this.mTileDividerIndex) {
            return 4;
        }
        return this.mTiles.get(i) == null ? 1 : 0;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup viewGroup, int i) {
        Context context = viewGroup.getContext();
        LayoutInflater layoutInflaterFrom = LayoutInflater.from(context);
        if (i == 4) {
            return new Holder(layoutInflaterFrom.inflate(R.layout.qs_customize_tile_divider, viewGroup, false));
        }
        if (i != 1) {
            FrameLayout frameLayout = (FrameLayout) layoutInflaterFrom.inflate(R.layout.qs_customize_tile_frame, viewGroup, false);
            frameLayout.addView(new CustomizeTileView(context, new QSIconViewImpl(context)));
            return new Holder(frameLayout);
        }
        return new Holder(layoutInflaterFrom.inflate(R.layout.qs_customize_divider, viewGroup, false));
    }

    @Override
    public int getItemCount() {
        return this.mTiles.size();
    }

    @Override
    public boolean onFailedToRecycleView(Holder holder) {
        holder.clearDrag();
        return true;
    }

    @Override
    public void onBindViewHolder(final Holder holder, int i) {
        int i2;
        if (holder.getItemViewType() == 4) {
            holder.itemView.setVisibility(this.mTileDividerIndex < this.mTiles.size() - 1 ? 0 : 4);
            return;
        }
        if (holder.getItemViewType() == 1) {
            if (this.mCurrentDrag == null) {
                i2 = R.string.drag_to_add_tiles;
            } else if (!canRemoveTiles() && this.mCurrentDrag.getAdapterPosition() < this.mEditIndex) {
                i2 = R.string.drag_to_remove_disabled;
            } else {
                i2 = R.string.drag_to_remove_tiles;
            }
            ((TextView) holder.itemView.findViewById(android.R.id.title)).setText(i2);
            return;
        }
        if (holder.getItemViewType() != 2) {
            TileQueryHelper.TileInfo tileInfo = this.mTiles.get(i);
            if (i > this.mEditIndex) {
                tileInfo.state.contentDescription = this.mContext.getString(R.string.accessibility_qs_edit_add_tile_label, tileInfo.state.label);
            } else if (this.mAccessibilityAction != 0) {
                tileInfo.state.contentDescription = this.mContext.getString(R.string.accessibility_qs_edit_position_label, Integer.valueOf(i + 1));
            } else {
                tileInfo.state.contentDescription = this.mContext.getString(R.string.accessibility_qs_edit_tile_label, Integer.valueOf(i + 1), tileInfo.state.label);
            }
            holder.mTileView.handleStateChanged(tileInfo.state);
            holder.mTileView.setShowAppLabel(i > this.mEditIndex && !tileInfo.isSystem);
            if (this.mAccessibilityManager.isTouchExplorationEnabled()) {
                boolean z = this.mAccessibilityAction == 0 || i < this.mEditIndex;
                holder.mTileView.setClickable(z);
                holder.mTileView.setFocusable(z);
                holder.mTileView.setImportantForAccessibility(z ? 1 : 4);
                if (!z) {
                    return;
                }
                holder.mTileView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int adapterPosition = holder.getAdapterPosition();
                        if (TileAdapter.this.mAccessibilityAction != 0) {
                            TileAdapter.this.selectPosition(adapterPosition, view);
                        } else if (adapterPosition >= TileAdapter.this.mEditIndex || !TileAdapter.this.canRemoveTiles()) {
                            TileAdapter.this.startAccessibleAdd(adapterPosition);
                        } else {
                            TileAdapter.this.showAccessibilityDialog(adapterPosition, view);
                        }
                    }
                });
                return;
            }
            return;
        }
        holder.mTileView.setClickable(true);
        holder.mTileView.setFocusable(true);
        holder.mTileView.setFocusableInTouchMode(true);
        holder.mTileView.setVisibility(0);
        holder.mTileView.setImportantForAccessibility(1);
        holder.mTileView.setContentDescription(this.mContext.getString(R.string.accessibility_qs_edit_position_label, Integer.valueOf(i + 1)));
        holder.mTileView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TileAdapter.this.selectPosition(holder.getAdapterPosition(), view);
            }
        });
        if (!this.mNeedsFocus) {
            return;
        }
        holder.mTileView.requestLayout();
        holder.mTileView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10) {
                holder.mTileView.removeOnLayoutChangeListener(this);
                holder.mTileView.requestFocus();
            }
        });
        this.mNeedsFocus = false;
    }

    private boolean canRemoveTiles() {
        return this.mCurrentSpecs.size() > 6;
    }

    private void selectPosition(int i, View view) {
        if (this.mAccessibilityAction == 1) {
            List<TileQueryHelper.TileInfo> list = this.mTiles;
            int i2 = this.mEditIndex;
            this.mEditIndex = i2 - 1;
            list.remove(i2);
            notifyItemRemoved(this.mEditIndex);
            if (i == this.mEditIndex - 1) {
                i--;
            }
        }
        this.mAccessibilityAction = 0;
        move(this.mAccessibilityFromIndex, i, view);
        notifyDataSetChanged();
    }

    private void showAccessibilityDialog(final int i, final View view) {
        final TileQueryHelper.TileInfo tileInfo = this.mTiles.get(i);
        AlertDialog alertDialogCreate = new AlertDialog.Builder(this.mContext).setItems(new CharSequence[]{this.mContext.getString(R.string.accessibility_qs_edit_move_tile, tileInfo.state.label), this.mContext.getString(R.string.accessibility_qs_edit_remove_tile, tileInfo.state.label)}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i2) {
                if (i2 == 0) {
                    TileAdapter.this.startAccessibleMove(i);
                    return;
                }
                TileAdapter.this.move(i, tileInfo.isSystem ? TileAdapter.this.mEditIndex : TileAdapter.this.mTileDividerIndex, view);
                TileAdapter.this.notifyItemChanged(TileAdapter.this.mTileDividerIndex);
                TileAdapter.this.notifyDataSetChanged();
            }
        }).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).create();
        SystemUIDialog.setShowForAllUsers(alertDialogCreate, true);
        SystemUIDialog.applyFlags(alertDialogCreate);
        alertDialogCreate.show();
    }

    private void startAccessibleAdd(int i) {
        this.mAccessibilityFromIndex = i;
        this.mAccessibilityAction = 1;
        List<TileQueryHelper.TileInfo> list = this.mTiles;
        int i2 = this.mEditIndex;
        this.mEditIndex = i2 + 1;
        list.add(i2, null);
        this.mNeedsFocus = true;
        notifyDataSetChanged();
    }

    private void startAccessibleMove(int i) {
        this.mAccessibilityFromIndex = i;
        this.mAccessibilityAction = 2;
        notifyDataSetChanged();
    }

    public GridLayoutManager.SpanSizeLookup getSizeLookup() {
        return this.mSizeLookup;
    }

    private boolean move(int i, int i2, View view) {
        if (i2 == i) {
            return true;
        }
        CharSequence charSequence = this.mTiles.get(i).state.label;
        move(i, i2, this.mTiles);
        updateDividerLocations();
        if (i2 >= this.mEditIndex) {
            MetricsLogger.action(this.mContext, 360, strip(this.mTiles.get(i2)));
            MetricsLogger.action(this.mContext, 361, i);
        } else if (i >= this.mEditIndex) {
            MetricsLogger.action(this.mContext, 362, strip(this.mTiles.get(i2)));
            MetricsLogger.action(this.mContext, 363, i2);
            view.announceForAccessibility(this.mContext.getString(R.string.accessibility_qs_edit_tile_added, charSequence, Integer.valueOf(i2 + 1)));
        } else {
            MetricsLogger.action(this.mContext, 364, strip(this.mTiles.get(i2)));
            MetricsLogger.action(this.mContext, 365, i2);
            view.announceForAccessibility(this.mContext.getString(R.string.accessibility_qs_edit_tile_moved, charSequence, Integer.valueOf(i2 + 1)));
        }
        saveSpecs(this.mHost);
        return true;
    }

    private void updateDividerLocations() {
        this.mEditIndex = -1;
        this.mTileDividerIndex = this.mTiles.size();
        for (int i = 0; i < this.mTiles.size(); i++) {
            if (this.mTiles.get(i) == null) {
                if (this.mEditIndex == -1) {
                    this.mEditIndex = i;
                } else {
                    this.mTileDividerIndex = i;
                }
            }
        }
        if (this.mTiles.size() - 1 == this.mTileDividerIndex) {
            notifyItemChanged(this.mTileDividerIndex);
        }
    }

    private static String strip(TileQueryHelper.TileInfo tileInfo) {
        String str = tileInfo.spec;
        if (str.startsWith("custom(")) {
            return CustomTile.getComponentFromSpec(str).getPackageName();
        }
        return str;
    }

    private <T> void move(int i, int i2, List<T> list) {
        list.add(i2, list.remove(i));
        notifyItemMoved(i, i2);
    }

    public class Holder extends RecyclerView.ViewHolder {
        private CustomizeTileView mTileView;

        public Holder(View view) {
            super(view);
            if (view instanceof FrameLayout) {
                this.mTileView = (CustomizeTileView) ((FrameLayout) view).getChildAt(0);
                this.mTileView.setBackground(null);
                this.mTileView.getIcon().disableAnimation();
            }
        }

        public void clearDrag() {
            this.itemView.clearAnimation();
            this.mTileView.findViewById(R.id.tile_label).clearAnimation();
            this.mTileView.findViewById(R.id.tile_label).setAlpha(1.0f);
            this.mTileView.getAppLabel().clearAnimation();
            this.mTileView.getAppLabel().setAlpha(0.6f);
        }

        public void startDrag() {
            this.itemView.animate().setDuration(100L).scaleX(1.2f).scaleY(1.2f);
            this.mTileView.findViewById(R.id.tile_label).animate().setDuration(100L).alpha(0.0f);
            this.mTileView.getAppLabel().animate().setDuration(100L).alpha(0.0f);
        }

        public void stopDrag() {
            this.itemView.animate().setDuration(100L).scaleX(1.0f).scaleY(1.0f);
            this.mTileView.findViewById(R.id.tile_label).animate().setDuration(100L).alpha(1.0f);
            this.mTileView.getAppLabel().animate().setDuration(100L).alpha(0.6f);
        }
    }

    private class TileItemDecoration extends RecyclerView.ItemDecoration {
        private final ColorDrawable mDrawable;

        private TileItemDecoration(Context context) {
            TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(new int[]{android.R.attr.colorSecondary});
            this.mDrawable = new ColorDrawable(typedArrayObtainStyledAttributes.getColor(0, 0));
            typedArrayObtainStyledAttributes.recycle();
        }

        @Override
        public void onDraw(Canvas canvas, RecyclerView recyclerView, RecyclerView.State state) {
            super.onDraw(canvas, recyclerView, state);
            int childCount = recyclerView.getChildCount();
            int width = recyclerView.getWidth();
            int bottom = recyclerView.getBottom();
            for (int i = 0; i < childCount; i++) {
                View childAt = recyclerView.getChildAt(i);
                if (recyclerView.getChildViewHolder(childAt).getAdapterPosition() >= TileAdapter.this.mEditIndex || (childAt instanceof TextView)) {
                    this.mDrawable.setBounds(0, childAt.getTop() + ((RecyclerView.LayoutParams) childAt.getLayoutParams()).topMargin + Math.round(ViewCompat.getTranslationY(childAt)), width, bottom);
                    this.mDrawable.draw(canvas);
                    return;
                }
            }
        }
    }
}
