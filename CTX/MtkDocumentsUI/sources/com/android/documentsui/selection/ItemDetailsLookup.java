package com.android.documentsui.selection;

import android.view.MotionEvent;

public abstract class ItemDetailsLookup {
    public abstract ItemDetails getItemDetails(MotionEvent motionEvent);

    public abstract boolean inItemDragRegion(MotionEvent motionEvent);

    public abstract boolean overItem(MotionEvent motionEvent);

    public abstract boolean overStableItem(MotionEvent motionEvent);

    public static abstract class ItemDetails {
        public abstract int getItemViewType();

        public abstract int getPosition();

        public abstract String getStableId();

        public boolean hasPosition() {
            return getPosition() > -1;
        }

        public boolean hasStableId() {
            return getStableId() != null;
        }

        public boolean inSelectionHotspot(MotionEvent motionEvent) {
            return false;
        }

        public boolean inDragRegion(MotionEvent motionEvent) {
            return false;
        }

        public boolean equals(Object obj) {
            if (obj instanceof ItemDetails) {
                return equals((ItemDetails) obj);
            }
            return false;
        }

        private boolean equals(ItemDetails itemDetails) {
            return getPosition() == itemDetails.getPosition() && getStableId() == itemDetails.getStableId();
        }

        public int hashCode() {
            return getPosition() >>> 8;
        }
    }
}
