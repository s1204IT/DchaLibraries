package com.android.server.pm;

import android.content.pm.PackageParser;
import android.content.pm.Signature;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

final class Policy {
    private final Set<Signature> mCerts;
    private final Map<String, String> mPkgMap;
    private final String mSeinfo;

    private Policy(PolicyBuilder policyBuilder) {
        this.mSeinfo = policyBuilder.mSeinfo;
        this.mCerts = Collections.unmodifiableSet(policyBuilder.mCerts);
        this.mPkgMap = Collections.unmodifiableMap(policyBuilder.mPkgMap);
    }

    public Set<Signature> getSignatures() {
        return this.mCerts;
    }

    public boolean hasInnerPackages() {
        return !this.mPkgMap.isEmpty();
    }

    public Map<String, String> getInnerPackages() {
        return this.mPkgMap;
    }

    public boolean hasGlobalSeinfo() {
        return this.mSeinfo != null;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        Iterator<Signature> it = this.mCerts.iterator();
        while (it.hasNext()) {
            sb.append("cert=" + it.next().toCharsString().substring(0, 11) + "... ");
        }
        if (this.mSeinfo != null) {
            sb.append("seinfo=" + this.mSeinfo);
        }
        for (String str : this.mPkgMap.keySet()) {
            sb.append(" " + str + "=" + this.mPkgMap.get(str));
        }
        return sb.toString();
    }

    public String getMatchedSeInfo(PackageParser.Package r5) {
        Signature[] signatureArr = (Signature[]) this.mCerts.toArray(new Signature[0]);
        if (r5.mSigningDetails != PackageParser.SigningDetails.UNKNOWN && !Signature.areExactMatch(signatureArr, r5.mSigningDetails.signatures) && (signatureArr.length > 1 || !r5.mSigningDetails.hasCertificate(signatureArr[0]))) {
            return null;
        }
        String str = this.mPkgMap.get(r5.packageName);
        if (str != null) {
            return str;
        }
        return this.mSeinfo;
    }

    public static final class PolicyBuilder {
        private final Set<Signature> mCerts = new HashSet(2);
        private final Map<String, String> mPkgMap = new HashMap(2);
        private String mSeinfo;

        public PolicyBuilder addSignature(String str) {
            if (str == null) {
                throw new IllegalArgumentException("Invalid signature value " + str);
            }
            this.mCerts.add(new Signature(str));
            return this;
        }

        public PolicyBuilder setGlobalSeinfoOrThrow(String str) {
            if (!validateValue(str)) {
                throw new IllegalArgumentException("Invalid seinfo value " + str);
            }
            if (this.mSeinfo != null && !this.mSeinfo.equals(str)) {
                throw new IllegalStateException("Duplicate seinfo tag found");
            }
            this.mSeinfo = str;
            return this;
        }

        public PolicyBuilder addInnerPackageMapOrThrow(String str, String str2) {
            if (!validateValue(str)) {
                throw new IllegalArgumentException("Invalid package name " + str);
            }
            if (!validateValue(str2)) {
                throw new IllegalArgumentException("Invalid seinfo value " + str2);
            }
            String str3 = this.mPkgMap.get(str);
            if (str3 != null && !str3.equals(str2)) {
                throw new IllegalStateException("Conflicting seinfo value found");
            }
            this.mPkgMap.put(str, str2);
            return this;
        }

        private boolean validateValue(String str) {
            if (str == null || !str.matches("\\A[\\.\\w]+\\z")) {
                return false;
            }
            return true;
        }

        public Policy build() {
            Policy policy = new Policy(this);
            if (!policy.mCerts.isEmpty()) {
                if (!((policy.mSeinfo == null) ^ policy.mPkgMap.isEmpty())) {
                    throw new IllegalStateException("Only seinfo tag XOR package tags are allowed within a signer stanza.");
                }
                return policy;
            }
            throw new IllegalStateException("Missing certs with signer tag. Expecting at least one.");
        }
    }
}
