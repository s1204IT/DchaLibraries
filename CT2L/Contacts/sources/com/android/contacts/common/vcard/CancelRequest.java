package com.android.contacts.common.vcard;

public class CancelRequest {
    public final String displayName;
    public final int jobId;

    public CancelRequest(int jobId, String displayName) {
        this.jobId = jobId;
        this.displayName = displayName;
    }
}
