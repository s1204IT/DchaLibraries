package com.android.contacts;

public class SimPhonebookService2 extends SimPhonebookService {
    protected int mSlotId = 1;

    @Override
    protected int getSlotId() {
        return this.mSlotId;
    }
}
