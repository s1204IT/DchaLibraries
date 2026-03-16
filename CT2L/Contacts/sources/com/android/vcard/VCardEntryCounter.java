package com.android.vcard;

public class VCardEntryCounter implements VCardInterpreter {
    private int mCount;

    public int getCount() {
        return this.mCount;
    }

    @Override
    public void onVCardStarted() {
    }

    @Override
    public void onVCardEnded() {
    }

    @Override
    public void onEntryStarted() {
    }

    @Override
    public void onEntryEnded() {
        this.mCount++;
    }

    @Override
    public void onPropertyCreated(VCardProperty property) {
    }
}
