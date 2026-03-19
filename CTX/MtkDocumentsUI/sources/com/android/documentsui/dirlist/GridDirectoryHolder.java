package com.android.documentsui.dirlist;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.documentsui.R;
import com.android.documentsui.base.DocumentInfo;

final class GridDirectoryHolder extends DocumentHolder {
    static final boolean $assertionsDisabled = false;
    private final ImageView mIconCheck;
    private final ImageView mIconMime;
    final TextView mTitle;

    public GridDirectoryHolder(Context context, ViewGroup viewGroup) {
        super(context, viewGroup, R.layout.item_dir_grid);
        this.mTitle = (TextView) this.itemView.findViewById(android.R.id.title);
        this.mIconMime = (ImageView) this.itemView.findViewById(R.id.icon_mime_sm);
        this.mIconCheck = (ImageView) this.itemView.findViewById(R.id.icon_check);
    }

    @Override
    public void setSelected(boolean z, boolean z2) {
        float f;
        super.setSelected(z, z2);
        if (!z) {
            f = 0.0f;
        } else {
            f = 1.0f;
        }
        if (z2) {
            fade(this.mIconCheck, f).start();
            fade(this.mIconMime, 1.0f - f).start();
        } else {
            this.mIconCheck.setAlpha(f);
            this.mIconMime.setAlpha(1.0f - f);
        }
    }

    @Override
    public boolean inDragRegion(MotionEvent motionEvent) {
        return true;
    }

    @Override
    public boolean inSelectRegion(MotionEvent motionEvent) {
        Rect rect = new Rect();
        this.mIconMime.getGlobalVisibleRect(rect);
        return rect.contains((int) motionEvent.getRawX(), (int) motionEvent.getRawY());
    }

    @Override
    public void bind(Cursor cursor, String str) {
        this.mModelId = str;
        this.mTitle.setText(DocumentInfo.getCursorString(cursor, "_display_name"), TextView.BufferType.SPANNABLE);
    }
}
