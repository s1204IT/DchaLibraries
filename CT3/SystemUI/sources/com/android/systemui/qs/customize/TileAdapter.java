package com.android.systemui.qs.customize;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
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
import com.android.systemui.qs.QSIconView;
import com.android.systemui.qs.customize.TileQueryHelper;
import com.android.systemui.qs.external.CustomTile;
import com.android.systemui.statusbar.phone.QSTileHost;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import java.util.ArrayList;
import java.util.List;

public class TileAdapter extends RecyclerView.Adapter<Holder> implements TileQueryHelper.TileStateListener {
    private int mAccessibilityFromIndex;
    private final AccessibilityManager mAccessibilityManager;
    private boolean mAccessibilityMoving;
    private List<TileQueryHelper.TileInfo> mAllTiles;
    private final Context mContext;
    private Holder mCurrentDrag;
    private List<String> mCurrentSpecs;
    private int mEditIndex;
    private QSTileHost mHost;
    private boolean mNeedsFocus;
    private List<TileQueryHelper.TileInfo> mOtherTiles;
    private int mTileDividerIndex;
    private final Handler mHandler = new Handler();
    private final List<TileQueryHelper.TileInfo> mTiles = new ArrayList();
    private final GridLayoutManager.SpanSizeLookup mSizeLookup = new GridLayoutManager.SpanSizeLookup() {
        @Override
        public int getSpanSize(int position) {
            int type = TileAdapter.this.getItemViewType(position);
            return (type == 1 || type == 4) ? 3 : 1;
        }
    };
    private final RecyclerView.ItemDecoration mDecoration = new RecyclerView.ItemDecoration() {
        private final ColorDrawable mDrawable = new ColorDrawable(-13090232);

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            super.onDraw(c, parent, state);
            int childCount = parent.getChildCount();
            int width = parent.getWidth();
            int bottom = parent.getBottom();
            for (int i = 0; i < childCount; i++) {
                View child = parent.getChildAt(i);
                RecyclerView.ViewHolder holder = parent.getChildViewHolder(child);
                if (holder.getAdapterPosition() >= TileAdapter.this.mEditIndex || (child instanceof TextView)) {
                    RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
                    int top = child.getTop() + params.topMargin + Math.round(ViewCompat.getTranslationY(child));
                    this.mDrawable.setBounds(0, top, width, bottom);
                    this.mDrawable.draw(c);
                    return;
                }
            }
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
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            boolean z = false;
            super.onSelectedChanged(viewHolder, actionState);
            if (actionState != 2) {
                viewHolder = null;
            }
            if (viewHolder == TileAdapter.this.mCurrentDrag) {
                return;
            }
            if (TileAdapter.this.mCurrentDrag != null) {
                int position = TileAdapter.this.mCurrentDrag.getAdapterPosition();
                TileQueryHelper.TileInfo info = (TileQueryHelper.TileInfo) TileAdapter.this.mTiles.get(position);
                CustomizeTileView customizeTileView = TileAdapter.this.mCurrentDrag.mTileView;
                if (position > TileAdapter.this.mEditIndex && !info.isSystem) {
                    z = true;
                }
                customizeTileView.setShowAppLabel(z);
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
        public boolean canDropOver(RecyclerView recyclerView, RecyclerView.ViewHolder current, RecyclerView.ViewHolder target) {
            return target.getAdapterPosition() <= TileAdapter.this.mEditIndex + 1;
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            if (viewHolder.getItemViewType() == 1) {
                return makeMovementFlags(0, 0);
            }
            return makeMovementFlags(15, 0);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            int from = viewHolder.getAdapterPosition();
            int to = target.getAdapterPosition();
            return TileAdapter.this.move(from, to, target.itemView);
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        }
    };
    private final ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(this.mCallbacks);

    public TileAdapter(Context context) {
        this.mContext = context;
        this.mAccessibilityManager = (AccessibilityManager) context.getSystemService(AccessibilityManager.class);
    }

    public void setHost(QSTileHost host) {
        this.mHost = host;
    }

    public ItemTouchHelper getItemTouchHelper() {
        return this.mItemTouchHelper;
    }

    public RecyclerView.ItemDecoration getItemDecoration() {
        return this.mDecoration;
    }

    public void saveSpecs(QSTileHost host) {
        List<String> newSpecs = new ArrayList<>();
        for (int i = 0; i < this.mTiles.size() && this.mTiles.get(i) != null; i++) {
            newSpecs.add(this.mTiles.get(i).spec);
        }
        host.changeTiles(this.mCurrentSpecs, newSpecs);
        this.mCurrentSpecs = newSpecs;
    }

    public void setTileSpecs(List<String> currentSpecs) {
        if (currentSpecs.equals(this.mCurrentSpecs)) {
            return;
        }
        this.mCurrentSpecs = currentSpecs;
        recalcSpecs();
    }

    @Override
    public void onTilesChanged(List<TileQueryHelper.TileInfo> tiles) {
        this.mAllTiles = tiles;
        recalcSpecs();
    }

    private void recalcSpecs() {
        if (this.mCurrentSpecs == null || this.mAllTiles == null) {
            return;
        }
        this.mOtherTiles = new ArrayList(this.mAllTiles);
        this.mTiles.clear();
        for (int i = 0; i < this.mCurrentSpecs.size(); i++) {
            TileQueryHelper.TileInfo tile = getAndRemoveOther(this.mCurrentSpecs.get(i));
            if (tile != null) {
                this.mTiles.add(tile);
            }
        }
        this.mTiles.add(null);
        int i2 = 0;
        while (i2 < this.mOtherTiles.size()) {
            TileQueryHelper.TileInfo tile2 = this.mOtherTiles.get(i2);
            if (tile2.isSystem) {
                this.mOtherTiles.remove(i2);
                this.mTiles.add(tile2);
                i2--;
            }
            i2++;
        }
        this.mTileDividerIndex = this.mTiles.size();
        this.mTiles.add(null);
        this.mTiles.addAll(this.mOtherTiles);
        updateDividerLocations();
        notifyDataSetChanged();
    }

    private TileQueryHelper.TileInfo getAndRemoveOther(String s) {
        for (int i = 0; i < this.mOtherTiles.size(); i++) {
            if (this.mOtherTiles.get(i).spec.equals(s)) {
                return this.mOtherTiles.remove(i);
            }
        }
        return null;
    }

    @Override
    public int getItemViewType(int position) {
        if (this.mAccessibilityMoving && position == this.mEditIndex - 1) {
            return 2;
        }
        if (position == this.mTileDividerIndex) {
            return 4;
        }
        if (this.mTiles.get(position) == null) {
            return 1;
        }
        return 0;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == 4) {
            return new Holder(inflater.inflate(R.layout.qs_customize_tile_divider, parent, false));
        }
        if (viewType == 1) {
            return new Holder(inflater.inflate(R.layout.qs_customize_divider, parent, false));
        }
        FrameLayout frame = (FrameLayout) inflater.inflate(R.layout.qs_customize_tile_frame, parent, false);
        frame.addView(new CustomizeTileView(context, new QSIconView(context)));
        return new Holder(frame);
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
        boolean z = false;
        z = false;
        if (holder.getItemViewType() == 4) {
            holder.itemView.setVisibility(this.mTileDividerIndex >= this.mTiles.size() + (-1) ? 4 : 0);
            return;
        }
        if (holder.getItemViewType() == 1) {
            ((TextView) holder.itemView.findViewById(android.R.id.title)).setText(this.mCurrentDrag != null ? R.string.drag_to_remove_tiles : R.string.drag_to_add_tiles);
            return;
        }
        if (holder.getItemViewType() == 2) {
            holder.mTileView.setClickable(true);
            holder.mTileView.setFocusable(true);
            holder.mTileView.setFocusableInTouchMode(true);
            holder.mTileView.setVisibility(0);
            holder.mTileView.setImportantForAccessibility(1);
            holder.mTileView.setContentDescription(this.mContext.getString(R.string.accessibility_qs_edit_position_label, Integer.valueOf(i + 1)));
            holder.mTileView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TileAdapter.this.selectPosition(holder.getAdapterPosition(), v);
                }
            });
            if (this.mNeedsFocus) {
                holder.mTileView.requestLayout();
                holder.mTileView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                        holder.mTileView.removeOnLayoutChangeListener(this);
                        holder.mTileView.requestFocus();
                    }
                });
                this.mNeedsFocus = false;
                return;
            }
            return;
        }
        TileQueryHelper.TileInfo tileInfo = this.mTiles.get(i);
        if (i > this.mEditIndex) {
            tileInfo.state.contentDescription = this.mContext.getString(R.string.accessibility_qs_edit_add_tile_label, tileInfo.state.label);
        } else if (this.mAccessibilityMoving) {
            tileInfo.state.contentDescription = this.mContext.getString(R.string.accessibility_qs_edit_position_label, Integer.valueOf(i + 1));
        } else {
            tileInfo.state.contentDescription = this.mContext.getString(R.string.accessibility_qs_edit_tile_label, Integer.valueOf(i + 1), tileInfo.state.label);
        }
        holder.mTileView.onStateChanged(tileInfo.state);
        holder.mTileView.setAppLabel(tileInfo.appLabel);
        CustomizeTileView customizeTileView = holder.mTileView;
        if (i > this.mEditIndex && !tileInfo.isSystem) {
            z = true;
        }
        customizeTileView.setShowAppLabel(z);
        if (!this.mAccessibilityManager.isTouchExplorationEnabled()) {
            return;
        }
        boolean z2 = !this.mAccessibilityMoving || i < this.mEditIndex;
        holder.mTileView.setClickable(z2);
        holder.mTileView.setFocusable(z2);
        holder.mTileView.setImportantForAccessibility(z2 ? 1 : 4);
        if (!z2) {
            return;
        }
        holder.mTileView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = holder.getAdapterPosition();
                if (TileAdapter.this.mAccessibilityMoving) {
                    TileAdapter.this.selectPosition(position, v);
                } else if (position < TileAdapter.this.mEditIndex) {
                    TileAdapter.this.showAccessibilityDialog(position, v);
                } else {
                    TileAdapter.this.startAccessibleDrag(position);
                }
            }
        });
    }

    public void selectPosition(int position, View v) {
        this.mAccessibilityMoving = false;
        List<TileQueryHelper.TileInfo> list = this.mTiles;
        int i = this.mEditIndex;
        this.mEditIndex = i - 1;
        list.remove(i);
        notifyItemRemoved(this.mEditIndex - 1);
        move(this.mAccessibilityFromIndex, position, v);
        notifyDataSetChanged();
    }

    public void showAccessibilityDialog(final int position, final View v) {
        final TileQueryHelper.TileInfo info = this.mTiles.get(position);
        CharSequence[] options = {this.mContext.getString(R.string.accessibility_qs_edit_move_tile, info.state.label), this.mContext.getString(R.string.accessibility_qs_edit_remove_tile, info.state.label)};
        AlertDialog dialog = new AlertDialog.Builder(this.mContext).setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog2, int which) {
                if (which == 0) {
                    TileAdapter.this.startAccessibleDrag(position);
                    return;
                }
                TileAdapter.this.move(position, info.isSystem ? TileAdapter.this.mEditIndex : TileAdapter.this.mTileDividerIndex, v);
                TileAdapter.this.notifyItemChanged(TileAdapter.this.mTileDividerIndex);
                TileAdapter.this.notifyDataSetChanged();
            }
        }).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).create();
        SystemUIDialog.setShowForAllUsers(dialog, true);
        SystemUIDialog.applyFlags(dialog);
        dialog.show();
    }

    public void startAccessibleDrag(int position) {
        this.mAccessibilityMoving = true;
        this.mNeedsFocus = true;
        this.mAccessibilityFromIndex = position;
        List<TileQueryHelper.TileInfo> list = this.mTiles;
        int i = this.mEditIndex;
        this.mEditIndex = i + 1;
        list.add(i, null);
        notifyDataSetChanged();
    }

    public GridLayoutManager.SpanSizeLookup getSizeLookup() {
        return this.mSizeLookup;
    }

    public boolean move(int from, int to, View v) {
        CharSequence announcement;
        if (to == from) {
            return true;
        }
        CharSequence fromLabel = this.mTiles.get(from).state.label;
        move(from, to, this.mTiles);
        updateDividerLocations();
        if (to >= this.mEditIndex) {
            MetricsLogger.action(this.mContext, 360, strip(this.mTiles.get(to)));
            MetricsLogger.action(this.mContext, 361, from);
            announcement = this.mContext.getString(R.string.accessibility_qs_edit_tile_removed, fromLabel);
        } else if (from >= this.mEditIndex) {
            MetricsLogger.action(this.mContext, 362, strip(this.mTiles.get(to)));
            MetricsLogger.action(this.mContext, 363, to);
            announcement = this.mContext.getString(R.string.accessibility_qs_edit_tile_added, fromLabel, Integer.valueOf(to + 1));
        } else {
            MetricsLogger.action(this.mContext, 364, strip(this.mTiles.get(to)));
            MetricsLogger.action(this.mContext, 365, to);
            announcement = this.mContext.getString(R.string.accessibility_qs_edit_tile_moved, fromLabel, Integer.valueOf(to + 1));
        }
        v.announceForAccessibility(announcement);
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
        if (this.mTiles.size() - 1 != this.mTileDividerIndex) {
            return;
        }
        notifyItemChanged(this.mTileDividerIndex);
    }

    private static String strip(TileQueryHelper.TileInfo tileInfo) {
        String spec = tileInfo.spec;
        if (spec.startsWith("custom(")) {
            ComponentName component = CustomTile.getComponentFromSpec(spec);
            return component.getPackageName();
        }
        return spec;
    }

    private <T> void move(int from, int to, List<T> list) {
        list.add(to, list.remove(from));
        notifyItemMoved(from, to);
    }

    public class Holder extends RecyclerView.ViewHolder {
        private CustomizeTileView mTileView;

        public Holder(View itemView) {
            super(itemView);
            if (!(itemView instanceof FrameLayout)) {
                return;
            }
            this.mTileView = (CustomizeTileView) ((FrameLayout) itemView).getChildAt(0);
            this.mTileView.setBackground(null);
            this.mTileView.getIcon().disableAnimation();
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
}
