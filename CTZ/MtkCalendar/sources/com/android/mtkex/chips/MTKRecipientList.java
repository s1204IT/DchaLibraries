package com.android.mtkex.chips;

import java.util.ArrayList;

public class MTKRecipientList {
    private ArrayList<MTKRecipient> mRecipientList;

    public MTKRecipientList() {
        this.mRecipientList = null;
        if (this.mRecipientList == null) {
            this.mRecipientList = new ArrayList<>();
        }
    }

    public void addRecipient(String str, String str2) {
        this.mRecipientList.add(new MTKRecipient(str, str2));
    }

    public MTKRecipient getRecipient(int i) {
        return this.mRecipientList.get(i);
    }

    public int getRecipientCount() {
        return this.mRecipientList.size();
    }
}
