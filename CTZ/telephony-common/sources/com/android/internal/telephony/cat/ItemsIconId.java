package com.android.internal.telephony.cat;

public class ItemsIconId extends ValueObject {
    public int[] recordNumbers;
    public boolean selfExplanatory;

    @Override
    ComprehensionTlvTag getTag() {
        return ComprehensionTlvTag.ITEM_ICON_ID_LIST;
    }
}
