package com.android.org.conscrypt.ct;

import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Iterator;

public class CTPolicyImpl implements CTPolicy {
    private final CTLogStore logStore;
    private final int minimumLogCount;

    public CTPolicyImpl(CTLogStore cTLogStore, int i) {
        this.logStore = cTLogStore;
        this.minimumLogCount = i;
    }

    @Override
    public boolean doesResultConformToPolicy(CTVerificationResult cTVerificationResult, String str, X509Certificate[] x509CertificateArr) {
        HashSet hashSet = new HashSet();
        Iterator<VerifiedSCT> it = cTVerificationResult.getValidSCTs().iterator();
        while (it.hasNext()) {
            CTLogInfo knownLog = this.logStore.getKnownLog(it.next().sct.getLogID());
            if (knownLog != null) {
                hashSet.add(knownLog);
            }
        }
        return hashSet.size() >= this.minimumLogCount;
    }
}
