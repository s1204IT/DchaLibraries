package com.android.ex.chips.recipientchip;

import android.text.TextUtils;
import com.android.ex.chips.RecipientEntry;

class SimpleRecipientChip implements BaseRecipientChip {
    private final long mContactId;
    private final long mDataId;
    private final Long mDirectoryId;
    private final CharSequence mDisplay;
    private final RecipientEntry mEntry;
    private final String mLookupKey;
    private CharSequence mOriginalText;
    private boolean mSelected = false;
    private final CharSequence mValue;

    public SimpleRecipientChip(RecipientEntry entry) {
        this.mDisplay = entry.getDisplayName();
        this.mValue = entry.getDestination().trim();
        this.mContactId = entry.getContactId();
        this.mDirectoryId = entry.getDirectoryId();
        this.mLookupKey = entry.getLookupKey();
        this.mDataId = entry.getDataId();
        this.mEntry = entry;
    }

    @Override
    public void setSelected(boolean selected) {
        this.mSelected = selected;
    }

    @Override
    public boolean isSelected() {
        return this.mSelected;
    }

    @Override
    public CharSequence getValue() {
        return this.mValue;
    }

    @Override
    public long getContactId() {
        return this.mContactId;
    }

    @Override
    public Long getDirectoryId() {
        return this.mDirectoryId;
    }

    @Override
    public String getLookupKey() {
        return this.mLookupKey;
    }

    @Override
    public long getDataId() {
        return this.mDataId;
    }

    @Override
    public RecipientEntry getEntry() {
        return this.mEntry;
    }

    @Override
    public void setOriginalText(String text) {
        if (TextUtils.isEmpty(text)) {
            this.mOriginalText = text;
        } else {
            this.mOriginalText = text.trim();
        }
    }

    @Override
    public CharSequence getOriginalText() {
        return !TextUtils.isEmpty(this.mOriginalText) ? this.mOriginalText : this.mEntry.getDestination();
    }

    public String toString() {
        return ((Object) this.mDisplay) + " <" + ((Object) this.mValue) + ">";
    }
}
