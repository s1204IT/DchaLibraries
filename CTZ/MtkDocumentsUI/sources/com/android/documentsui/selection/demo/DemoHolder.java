package com.android.documentsui.selection.demo;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.documentsui.R;
import com.android.documentsui.selection.ItemDetailsLookup;

public final class DemoHolder extends RecyclerView.ViewHolder {
    private final LinearLayout mContainer;
    private final Details mDetails;
    private DemoItem mItem;
    public final TextView mLabel;
    public final TextView mSelector;

    DemoHolder(LinearLayout linearLayout) {
        super(linearLayout);
        this.mContainer = (LinearLayout) linearLayout.findViewById(R.id.container);
        this.mSelector = (TextView) linearLayout.findViewById(R.id.selector);
        this.mLabel = (TextView) linearLayout.findViewById(R.id.label);
        this.mDetails = new Details(this);
    }

    public void update(DemoItem demoItem) {
        this.mItem = demoItem;
        this.mLabel.setText(this.mItem.getName());
    }

    void setSelected(boolean z) {
        this.mContainer.setActivated(z);
        this.mSelector.setActivated(z);
    }

    public String getStableId() {
        if (this.mItem != null) {
            return this.mItem.getId();
        }
        return null;
    }

    public boolean inDragRegion(MotionEvent motionEvent) {
        if (this.mContainer.isActivated() || inSelectRegion(motionEvent)) {
            return true;
        }
        Rect rect = new Rect();
        this.mLabel.getPaint().getTextBounds(this.mLabel.getText().toString(), 0, this.mLabel.getText().length(), rect);
        return rect.contains((int) motionEvent.getRawX(), (int) motionEvent.getRawY());
    }

    public boolean inSelectRegion(MotionEvent motionEvent) {
        Rect rect = new Rect();
        this.mSelector.getGlobalVisibleRect(rect);
        return rect.contains((int) motionEvent.getRawX(), (int) motionEvent.getRawY());
    }

    Details getItemDetails() {
        return this.mDetails;
    }

    private static final class Details extends ItemDetailsLookup.ItemDetails {
        private final DemoHolder mHolder;

        Details(DemoHolder demoHolder) {
            this.mHolder = demoHolder;
        }

        @Override
        public int getPosition() {
            return this.mHolder.getAdapterPosition();
        }

        @Override
        public String getStableId() {
            return this.mHolder.getStableId();
        }

        @Override
        public int getItemViewType() {
            return this.mHolder.getItemViewType();
        }

        @Override
        public boolean inDragRegion(MotionEvent motionEvent) {
            return this.mHolder.inDragRegion(motionEvent);
        }

        @Override
        public boolean inSelectionHotspot(MotionEvent motionEvent) {
            return this.mHolder.inSelectRegion(motionEvent);
        }
    }
}
