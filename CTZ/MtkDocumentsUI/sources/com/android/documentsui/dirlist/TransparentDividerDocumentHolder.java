package com.android.documentsui.dirlist;

import android.content.Context;
import android.database.Cursor;
import android.widget.Space;
import com.android.documentsui.R;
import com.android.documentsui.base.State;

final class TransparentDividerDocumentHolder extends MessageHolder {
    private State mState;
    private final int mVisibleHeight;

    public TransparentDividerDocumentHolder(Context context) {
        super(context, new Space(context));
        this.mVisibleHeight = context.getResources().getDimensionPixelSize(R.dimen.grid_section_separator_height);
    }

    public void bind(State state) {
        this.mState = state;
        bind(null, null);
    }

    @Override
    public void bind(Cursor cursor, String str) {
        if (this.mState.derivedMode == 2) {
            this.itemView.setMinimumHeight(this.mVisibleHeight);
        } else {
            this.itemView.setMinimumHeight(0);
        }
    }
}
