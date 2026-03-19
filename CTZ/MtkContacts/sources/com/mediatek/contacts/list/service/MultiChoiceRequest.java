package com.mediatek.contacts.list.service;

public class MultiChoiceRequest {
    public int mContactId;
    public String mContactName;
    public int mIndicator;
    public int mSimIndex;

    public MultiChoiceRequest(int i, int i2, int i3, String str) {
        this.mIndicator = i;
        this.mSimIndex = i2;
        this.mContactId = i3;
        this.mContactName = str;
    }
}
