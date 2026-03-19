package com.android.org.conscrypt.ct;

public final class VerifiedSCT {
    public final SignedCertificateTimestamp sct;
    public final Status status;

    public enum Status {
        VALID,
        INVALID_SIGNATURE,
        UNKNOWN_LOG,
        INVALID_SCT;

        public static Status[] valuesCustom() {
            return values();
        }
    }

    public VerifiedSCT(SignedCertificateTimestamp sct, Status status) {
        this.sct = sct;
        this.status = status;
    }
}
