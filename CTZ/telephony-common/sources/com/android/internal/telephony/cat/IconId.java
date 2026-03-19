package com.android.internal.telephony.cat;

public class IconId extends ValueObject {
    public int recordNumber;
    public boolean selfExplanatory;

    @Override
    ComprehensionTlvTag getTag() {
        return ComprehensionTlvTag.ICON_ID;
    }
}
