package com.android.gallery3d.filtershow.state;

import com.android.gallery3d.filtershow.filters.FilterFxRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;

public class State {
    private FilterRepresentation mFilterRepresentation;
    private String mText;
    private int mType;

    public State(String text) {
        this(text, StateView.DEFAULT);
    }

    public State(String text, int type) {
        this.mText = text;
        this.mType = type;
    }

    public boolean equals(State state) {
        if (this.mFilterRepresentation.getFilterClass() != state.mFilterRepresentation.getFilterClass()) {
            return false;
        }
        if (this.mFilterRepresentation instanceof FilterFxRepresentation) {
            return this.mFilterRepresentation.equals(state.getFilterRepresentation());
        }
        return true;
    }

    String getText() {
        return this.mText;
    }

    int getType() {
        return this.mType;
    }

    public FilterRepresentation getFilterRepresentation() {
        return this.mFilterRepresentation;
    }

    public void setFilterRepresentation(FilterRepresentation filterRepresentation) {
        this.mFilterRepresentation = filterRepresentation;
    }
}
