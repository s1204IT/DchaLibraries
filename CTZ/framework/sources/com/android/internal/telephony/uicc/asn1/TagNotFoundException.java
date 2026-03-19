package com.android.internal.telephony.uicc.asn1;

public class TagNotFoundException extends Exception {
    private final int mTag;

    public TagNotFoundException(int i) {
        this.mTag = i;
    }

    public int getTag() {
        return this.mTag;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + " (tag=" + this.mTag + ")";
    }
}
