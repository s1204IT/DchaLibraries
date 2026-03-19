package com.android.documentsui.dirlist;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Rect;
import android.text.format.Formatter;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.documentsui.R;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.Lookup;
import com.android.documentsui.base.Shared;

final class ListDocumentHolder extends DocumentHolder {
    static final boolean $assertionsDisabled = false;
    private final TextView mDate;
    private final int mDefaultBgColor;
    private final LinearLayout mDetails;
    private final int mDisabledBgColor;
    private final DocumentInfo mDoc;
    private final Lookup<String, String> mFileTypeLookup;
    private final ImageView mIconCheck;
    final ImageView mIconDrm;
    private final IconHelper mIconHelper;
    private final View mIconLayout;
    private final ImageView mIconMime;
    private final ImageView mIconThumb;
    private final TextView mSize;
    private final TextView mSummary;
    private final TextView mTitle;
    private final TextView mType;

    public ListDocumentHolder(Context context, ViewGroup viewGroup, IconHelper iconHelper, Lookup<String, String> lookup) {
        super(context, viewGroup, R.layout.item_doc_list);
        this.mDisabledBgColor = context.getColor(R.color.item_doc_background_disabled);
        this.mDefaultBgColor = context.getColor(R.color.item_doc_background);
        this.mIconLayout = this.itemView.findViewById(android.R.id.icon);
        this.mIconMime = (ImageView) this.itemView.findViewById(R.id.icon_mime);
        this.mIconThumb = (ImageView) this.itemView.findViewById(R.id.icon_thumb);
        this.mIconCheck = (ImageView) this.itemView.findViewById(R.id.icon_check);
        this.mTitle = (TextView) this.itemView.findViewById(android.R.id.title);
        this.mSummary = (TextView) this.itemView.findViewById(android.R.id.summary);
        this.mSize = (TextView) this.itemView.findViewById(R.id.size);
        this.mDate = (TextView) this.itemView.findViewById(R.id.date);
        this.mType = (TextView) this.itemView.findViewById(R.id.file_type);
        this.mDetails = (LinearLayout) this.itemView.findViewById(R.id.line2);
        this.mIconDrm = (ImageView) this.itemView.findViewById(R.id.icon_drm);
        this.mIconHelper = iconHelper;
        this.mFileTypeLookup = lookup;
        this.mDoc = new DocumentInfo();
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
            fade(this.mIconCheck, f).start();
        } else {
            this.mIconCheck.setAlpha(f);
        }
        if (!this.itemView.isEnabled()) {
            return;
        }
        super.setSelected(z, z2);
        if (z2) {
            float f2 = 1.0f - f;
            fade(this.mIconMime, f2).start();
            fade(this.mIconThumb, f2).start();
        } else {
            float f3 = 1.0f - f;
            this.mIconMime.setAlpha(f3);
            this.mIconThumb.setAlpha(f3);
        }
    }

    @Override
    public void setEnabled(boolean z) {
        super.setEnabled(z);
        this.itemView.setBackgroundColor(z ? this.mDefaultBgColor : this.mDisabledBgColor);
        float f = z ? 1.0f : 0.3f;
        this.mIconMime.setAlpha(f);
        this.mIconThumb.setAlpha(f);
    }

    @Override
    public boolean inDragRegion(MotionEvent motionEvent) {
        if (this.itemView.isActivated()) {
            return true;
        }
        int[] iArr = new int[2];
        this.mIconLayout.getLocationOnScreen(iArr);
        Rect rect = new Rect();
        this.mTitle.getPaint().getTextBounds(this.mTitle.getText().toString(), 0, this.mTitle.getText().length(), rect);
        return new Rect(iArr[0], iArr[1], iArr[0] + this.mIconLayout.getWidth() + rect.width(), iArr[1] + Math.max(this.mIconLayout.getHeight(), rect.height())).contains((int) motionEvent.getRawX(), (int) motionEvent.getRawY());
    }

    @Override
    public boolean inSelectRegion(MotionEvent motionEvent) {
        Rect rect = new Rect();
        this.mIconLayout.getGlobalVisibleRect(rect);
        return rect.contains((int) motionEvent.getRawX(), (int) motionEvent.getRawY());
    }

    @Override
    public void bind(Cursor cursor, String str) {
        boolean z;
        this.mModelId = str;
        this.mDoc.updateFromCursor(cursor, DocumentInfo.getCursorString(cursor, "android:authority"));
        this.mIconHelper.stopLoading(this.mIconThumb);
        this.mIconMime.animate().cancel();
        this.mIconMime.setAlpha(1.0f);
        this.mIconThumb.animate().cancel();
        this.mIconThumb.setAlpha(0.0f);
        this.mIconHelper.load(this.mDoc, this.mIconThumb, this.mIconMime, null, cursor, this.mIconDrm);
        this.mTitle.setText(this.mDoc.displayName, TextView.BufferType.SPANNABLE);
        this.mTitle.setVisibility(0);
        if (!this.mDoc.isDirectory()) {
            if (this.mDoc.isPartial() && this.mDoc.summary != null) {
                this.mSummary.setText(this.mDoc.summary);
                this.mSummary.setVisibility(0);
                z = true;
            } else {
                this.mSummary.setVisibility(4);
                z = false;
            }
            if (this.mDoc.lastModified > 0) {
                this.mDate.setText(Shared.formatTime(this.mContext, this.mDoc.lastModified));
                z = true;
            } else {
                this.mDate.setText((CharSequence) null);
            }
            if (this.mDoc.size > -1) {
                this.mSize.setVisibility(0);
                this.mSize.setText(Formatter.formatFileSize(this.mContext, this.mDoc.size));
                z = true;
            } else {
                this.mSize.setVisibility(4);
            }
            this.mType.setText(this.mFileTypeLookup.lookup(this.mDoc.mimeType));
        } else {
            z = false;
        }
        if (this.mDetails != null) {
            this.mDetails.setVisibility(z ? 0 : 8);
        }
    }
}
