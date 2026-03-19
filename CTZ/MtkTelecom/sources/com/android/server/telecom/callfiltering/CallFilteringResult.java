package com.android.server.telecom.callfiltering;

public class CallFilteringResult {
    public boolean shouldAddToCallLog;
    public boolean shouldAllowCall;
    public boolean shouldReject;
    public boolean shouldShowNotification;

    public CallFilteringResult(boolean z, boolean z2, boolean z3, boolean z4) {
        this.shouldAllowCall = z;
        this.shouldReject = z2;
        this.shouldAddToCallLog = z3;
        this.shouldShowNotification = z4;
    }

    public CallFilteringResult combine(CallFilteringResult callFilteringResult) {
        if (callFilteringResult == null) {
            return this;
        }
        boolean z = false;
        boolean z2 = this.shouldAllowCall && callFilteringResult.shouldAllowCall;
        boolean z3 = this.shouldReject || callFilteringResult.shouldReject;
        boolean z4 = this.shouldAddToCallLog && callFilteringResult.shouldAddToCallLog;
        if (this.shouldShowNotification && callFilteringResult.shouldShowNotification) {
            z = true;
        }
        return new CallFilteringResult(z2, z3, z4, z);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CallFilteringResult callFilteringResult = (CallFilteringResult) obj;
        if (this.shouldAllowCall == callFilteringResult.shouldAllowCall && this.shouldReject == callFilteringResult.shouldReject && this.shouldAddToCallLog == callFilteringResult.shouldAddToCallLog && this.shouldShowNotification == callFilteringResult.shouldShowNotification) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * (((((this.shouldAllowCall ? 1 : 0) * 31) + (this.shouldReject ? 1 : 0)) * 31) + (this.shouldAddToCallLog ? 1 : 0))) + (this.shouldShowNotification ? 1 : 0);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        if (this.shouldAllowCall) {
            sb.append("Allow");
        } else if (this.shouldReject) {
            sb.append("Reject");
        } else {
            sb.append("Ignore");
        }
        if (this.shouldAddToCallLog) {
            sb.append(", logged");
        }
        if (this.shouldShowNotification) {
            sb.append(", notified");
        }
        sb.append("]");
        return sb.toString();
    }
}
