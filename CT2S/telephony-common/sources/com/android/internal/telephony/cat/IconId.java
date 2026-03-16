package com.android.internal.telephony.cat;

class IconId extends ValueObject {
    int recordNumber;
    boolean selfExplanatory;

    IconId() {
    }

    @Override
    ComprehensionTlvTag getTag() {
        return ComprehensionTlvTag.ICON_ID;
    }
}
