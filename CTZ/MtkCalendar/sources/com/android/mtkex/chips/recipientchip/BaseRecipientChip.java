package com.android.mtkex.chips.recipientchip;

import com.android.mtkex.chips.RecipientEntry;

interface BaseRecipientChip {
    long getContactId();

    long getDataId();

    RecipientEntry getEntry();

    CharSequence getOriginalText();

    CharSequence getValue();

    boolean isSelected();

    void setOriginalText(String str);

    void setSelected(boolean z);
}
