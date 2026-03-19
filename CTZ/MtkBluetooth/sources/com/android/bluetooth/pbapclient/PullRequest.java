package com.android.bluetooth.pbapclient;

import com.android.vcard.VCardEntry;
import java.util.List;

public abstract class PullRequest {
    protected List<VCardEntry> mEntries;
    public String path;

    public abstract void onPullComplete();

    public String toString() {
        return "PullRequest: { path=" + this.path + " }";
    }

    public void setResults(List<VCardEntry> list) {
        this.mEntries = list;
    }
}
