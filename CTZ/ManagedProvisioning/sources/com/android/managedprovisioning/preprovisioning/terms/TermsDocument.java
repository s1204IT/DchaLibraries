package com.android.managedprovisioning.preprovisioning.terms;

import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.common.ProvisionLogger;

public final class TermsDocument {
    private final String mContent;
    private final String mHeading;

    public static TermsDocument createInstance(String str, String str2) {
        try {
            return new TermsDocument(str, str2);
        } catch (IllegalArgumentException e) {
            ProvisionLogger.loge("Failed to parse a disclaimer.", e);
            return null;
        }
    }

    private TermsDocument(String str, String str2) {
        this.mHeading = (String) Preconditions.checkStringNotEmpty(str);
        this.mContent = (String) Preconditions.checkStringNotEmpty(str2);
    }

    public String getHeading() {
        return this.mHeading;
    }

    public String getContent() {
        return this.mContent;
    }
}
