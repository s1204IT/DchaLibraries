package com.android.contacts.logging;

import com.google.common.base.MoreObjects;

public final class ListEvent {
    public int actionType;
    public int clickedIndex = -1;
    public int count;
    public int listType;
    public int numSelected;

    public String toString() {
        return MoreObjects.toStringHelper(this).add("actionType", this.actionType).add("listType", this.listType).add("count", this.count).add("clickedIndex", this.clickedIndex).add("numSelected", this.numSelected).toString();
    }
}
