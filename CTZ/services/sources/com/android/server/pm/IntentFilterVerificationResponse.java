package com.android.server.pm;

import java.util.List;

class IntentFilterVerificationResponse {
    public final int callerUid;
    public final int code;
    public final List<String> failedDomains;

    public IntentFilterVerificationResponse(int i, int i2, List<String> list) {
        this.callerUid = i;
        this.code = i2;
        this.failedDomains = list;
    }

    public String getFailedDomainsString() {
        StringBuilder sb = new StringBuilder();
        for (String str : this.failedDomains) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(str);
        }
        return sb.toString();
    }
}
