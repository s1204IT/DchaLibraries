package com.android.documentsui.dirlist;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Rect;
import android.text.format.Formatter;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.documentsui.R;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.Shared;

final class GridDocumentHolder extends DocumentHolder {
    static final boolean $assertionsDisabled = false;
    final TextView mDate;
    private final int mDefaultBgColor;
    final TextView mDetails;
    private final int mDisabledBgColor;
    private final DocumentInfo mDoc;
    final ImageView mIconCheck;
    final ImageView mIconDrm;
    final IconHelper mIconHelper;
    final ImageView mIconMimeLg;
    final ImageView mIconMimeSm;
    final ImageView mIconThumb;
    final TextView mTitle;

    public GridDocumentHolder(Context context, ViewGroup viewGroup, IconHelper iconHelper) {
        super(context, viewGroup, R.layout.item_doc_grid);
        this.mDoc = new DocumentInfo();
        this.mDisabledBgColor = context.getColor(R.color.item_doc_background_disabled);
        this.mDefaultBgColor = context.getColor(R.color.item_doc_background);
        this.mTitle = (TextView) this.itemView.findViewById(android.R.id.title);
        this.mDate = (TextView) this.itemView.findViewById(R.id.date);
        this.mDetails = (TextView) this.itemView.findViewById(R.id.details);
        this.mIconMimeLg = (ImageView) this.itemView.findViewById(R.id.icon_mime_lg);
        this.mIconMimeSm = (ImageView) this.itemView.findViewById(R.id.icon_mime_sm);
        this.mIconThumb = (ImageView) this.itemView.findViewById(R.id.icon_thumb);
        this.mIconCheck = (ImageView) this.itemView.findViewById(R.id.icon_check);
        this.mIconDrm = (ImageView) this.itemView.findViewById(R.id.icon_drm);
        this.mIconHelper = iconHelper;
    }

    @Override
    public void setSelected(boolean z, boolean z2) {
        float f;
        if (!z) {
            f = 0.0f;
        } else {
            f = 1.0f;
        }
        if (z2) {
            fade(this.mIconMimeSm, f).start();
            fade(this.mIconCheck, f).start();
        } else {
            this.mIconCheck.setAlpha(f);
        }
        if (!this.itemView.isEnabled()) {
            return;
        }
        super.setSelected(z, z2);
        if (z2) {
            fade(this.mIconMimeSm, 1.0f - f).start();
        } else {
            this.mIconMimeSm.setAlpha(1.0f - f);
        }
    }

    @Override
    public void setEnabled(boolean z) {
        super.setEnabled(z);
        this.itemView.setBackgroundColor(z ? this.mDefaultBgColor : this.mDisabledBgColor);
        float f = z ? 1.0f : 0.3f;
        this.mIconMimeLg.setAlpha(f);
        this.mIconMimeSm.setAlpha(f);
        this.mIconThumb.setAlpha(f);
    }

    @Override
    public boolean inDragRegion(MotionEvent motionEvent) {
        return true;
    }

    @Override
    public boolean inSelectRegion(MotionEvent motionEvent) {
        Rect rect = new Rect();
        this.mIconMimeSm.getGlobalVisibleRect(rect);
        return rect.contains((int) motionEvent.getRawX(), (int) motionEvent.getRawY());
    }

    @Override
    public void bind(Cursor cursor, String str) {
        this.mModelId = str;
        this.mDoc.updateFromCursor(cursor, DocumentInfo.getCursorString(cursor, "android:authority"));
        this.mIconHelper.stopLoading(this.mIconThumb);
        this.mIconMimeLg.animate().cancel();
        this.mIconMimeLg.setAlpha(1.0f);
        this.mIconThumb.animate().cancel();
        this.mIconThumb.setAlpha(0.0f);
        this.mIconHelper.load(this.mDoc, this.mIconThumb, this.mIconMimeLg, this.mIconMimeSm, cursor, this.mIconDrm);
        this.mTitle.setText(this.mDoc.displayName, TextView.BufferType.SPANNABLE);
        this.mTitle.setVisibility(0);
        if (this.mDoc.isPartial()) {
            String cursorString = DocumentInfo.getCursorString(cursor, "summary");
            this.mDetails.setVisibility(0);
            this.mDate.setText((CharSequence) null);
            this.mDetails.setText(cursorString);
            return;
        }
        if (this.mDoc.lastModified == -1) {
            this.mDate.setText((CharSequence) null);
        } else {
            this.mDate.setText(Shared.formatTime(this.mContext, this.mDoc.lastModified));
        }
        long cursorLong = DocumentInfo.getCursorLong(cursor, "_size");
        if (this.mDoc.isDirectory() || cursorLong == -1) {
            this.mDetails.setVisibility(8);
        } else {
            this.mDetails.setVisibility(0);
            this.mDetails.setText(Formatter.formatFileSize(this.mContext, cursorLong));
        }
    }
}
