package com.android.settings.localepicker;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.android.settings.R;

public class LocaleLinearLayoutManager extends LinearLayoutManager {
    private final AccessibilityNodeInfoCompat.AccessibilityActionCompat mActionMoveBottom;
    private final AccessibilityNodeInfoCompat.AccessibilityActionCompat mActionMoveDown;
    private final AccessibilityNodeInfoCompat.AccessibilityActionCompat mActionMoveTop;
    private final AccessibilityNodeInfoCompat.AccessibilityActionCompat mActionMoveUp;
    private final AccessibilityNodeInfoCompat.AccessibilityActionCompat mActionRemove;
    private final LocaleDragAndDropAdapter mAdapter;
    private final Context mContext;

    public LocaleLinearLayoutManager(Context context, LocaleDragAndDropAdapter localeDragAndDropAdapter) {
        super(context);
        this.mContext = context;
        this.mAdapter = localeDragAndDropAdapter;
        this.mActionMoveUp = new AccessibilityNodeInfoCompat.AccessibilityActionCompat(R.id.action_drag_move_up, this.mContext.getString(R.string.action_drag_label_move_up));
        this.mActionMoveDown = new AccessibilityNodeInfoCompat.AccessibilityActionCompat(R.id.action_drag_move_down, this.mContext.getString(R.string.action_drag_label_move_down));
        this.mActionMoveTop = new AccessibilityNodeInfoCompat.AccessibilityActionCompat(R.id.action_drag_move_top, this.mContext.getString(R.string.action_drag_label_move_top));
        this.mActionMoveBottom = new AccessibilityNodeInfoCompat.AccessibilityActionCompat(R.id.action_drag_move_bottom, this.mContext.getString(R.string.action_drag_label_move_bottom));
        this.mActionRemove = new AccessibilityNodeInfoCompat.AccessibilityActionCompat(R.id.action_drag_remove, this.mContext.getString(R.string.action_drag_label_remove));
    }

    @Override
    public void onInitializeAccessibilityNodeInfoForItem(RecyclerView.Recycler recycler, RecyclerView.State state, View view, AccessibilityNodeInfoCompat accessibilityNodeInfoCompat) {
        super.onInitializeAccessibilityNodeInfoForItem(recycler, state, view, accessibilityNodeInfoCompat);
        int itemCount = getItemCount();
        int position = getPosition(view);
        StringBuilder sb = new StringBuilder();
        int i = position + 1;
        sb.append(i);
        sb.append(", ");
        sb.append((Object) ((LocaleDragCell) view).getCheckbox().getContentDescription());
        accessibilityNodeInfoCompat.setContentDescription(sb.toString());
        if (this.mAdapter.isRemoveMode()) {
            return;
        }
        if (position > 0) {
            accessibilityNodeInfoCompat.addAction(this.mActionMoveUp);
            accessibilityNodeInfoCompat.addAction(this.mActionMoveTop);
        }
        if (i < itemCount) {
            accessibilityNodeInfoCompat.addAction(this.mActionMoveDown);
            accessibilityNodeInfoCompat.addAction(this.mActionMoveBottom);
        }
        if (itemCount > 1) {
            accessibilityNodeInfoCompat.addAction(this.mActionRemove);
        }
    }

    @Override
    public boolean performAccessibilityActionForItem(RecyclerView.Recycler recycler, RecyclerView.State state, View view, int i, Bundle bundle) {
        int itemCount = getItemCount();
        int position = getPosition(view);
        boolean z = true;
        switch (i) {
            case R.id.action_drag_move_bottom:
                int i2 = itemCount - 1;
                if (position != i2) {
                    this.mAdapter.onItemMove(position, i2);
                } else {
                    z = false;
                }
                if (z) {
                    this.mAdapter.doTheUpdate();
                }
                return z;
            case R.id.action_drag_move_down:
                int i3 = position + 1;
                if (i3 < itemCount) {
                    this.mAdapter.onItemMove(position, i3);
                }
                if (z) {
                }
                return z;
            case R.id.action_drag_move_top:
                if (position != 0) {
                    this.mAdapter.onItemMove(position, 0);
                }
                if (z) {
                }
                return z;
            case R.id.action_drag_move_up:
                if (position > 0) {
                    this.mAdapter.onItemMove(position, position - 1);
                }
                if (z) {
                }
                return z;
            case R.id.action_drag_remove:
                if (itemCount > 1) {
                    this.mAdapter.removeItem(position);
                }
                if (z) {
                }
                return z;
            default:
                return super.performAccessibilityActionForItem(recycler, state, view, i, bundle);
        }
    }
}
