package com.android.mtkex.chips.recipientchip;

import android.text.TextUtils;
import com.android.mtkex.chips.RecipientEntry;

class SimpleRecipientChip implements BaseRecipientChip {
    private final long mContactId;
    private final long mDataId;
    private final CharSequence mDisplay;
    private final RecipientEntry mEntry;
    private CharSequence mOriginalText;
    private boolean mSelected = false;
    private final CharSequence mValue;

    public SimpleRecipientChip(RecipientEntry recipientEntry) {
        this.mDisplay = recipientEntry.getDisplayName();
        this.mValue = recipientEntry.getDestination().trim();
        this.mContactId = recipientEntry.getContactId();
        this.mDataId = recipientEntry.getDataId();
        this.mEntry = recipientEntry;
    }

    @Override
    public void setSelected(boolean z) {
        this.mSelected = z;
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
    public long getDataId() {
        return this.mDataId;
    }

    @Override
    public RecipientEntry getEntry() {
        return this.mEntry;
    }

    @Override
    public void setOriginalText(String str) {
        if (TextUtils.isEmpty(str)) {
            this.mOriginalText = str;
        } else {
            this.mOriginalText = str.trim();
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
