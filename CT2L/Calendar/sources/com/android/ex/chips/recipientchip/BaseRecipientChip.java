package com.android.ex.chips.recipientchip;

import com.android.ex.chips.RecipientEntry;

interface BaseRecipientChip {
    long getContactId();

    long getDataId();

    Long getDirectoryId();

    RecipientEntry getEntry();

    String getLookupKey();

    CharSequence getOriginalText();

    CharSequence getValue();

    boolean isSelected();

    void setOriginalText(String str);

    void setSelected(boolean z);
}
