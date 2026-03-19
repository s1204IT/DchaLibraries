package com.android.server.pm;

import android.content.pm.PackageParser;
import android.content.pm.Signature;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class Policy {
    private final Set<Signature> mCerts;
    private final Map<String, String> mPkgMap;
    private final String mSeinfo;

    Policy(PolicyBuilder builder, Policy policy) {
        this(builder);
    }

    private Policy(PolicyBuilder builder) {
        this.mSeinfo = builder.mSeinfo;
        this.mCerts = Collections.unmodifiableSet(builder.mCerts);
        this.mPkgMap = Collections.unmodifiableMap(builder.mPkgMap);
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
        for (Signature cert : this.mCerts) {
            sb.append("cert=").append(cert.toCharsString().substring(0, 11)).append("... ");
        }
        if (this.mSeinfo != null) {
            sb.append("seinfo=").append(this.mSeinfo);
        }
        for (String name : this.mPkgMap.keySet()) {
            sb.append(" ").append(name).append("=").append(this.mPkgMap.get(name));
        }
        return sb.toString();
    }

    public String getMatchedSeinfo(PackageParser.Package pkg) {
        Signature[] certs = (Signature[]) this.mCerts.toArray(new Signature[0]);
        if (!Signature.areExactMatch(certs, pkg.mSignatures)) {
            return null;
        }
        String seinfoValue = this.mPkgMap.get(pkg.packageName);
        if (seinfoValue != null) {
            return seinfoValue;
        }
        return this.mSeinfo;
    }

    public static final class PolicyBuilder {
        private final Set<Signature> mCerts = new HashSet(2);
        private final Map<String, String> mPkgMap = new HashMap(2);
        private String mSeinfo;

        public PolicyBuilder addSignature(String cert) {
            if (cert == null) {
                String err = "Invalid signature value " + cert;
                throw new IllegalArgumentException(err);
            }
            this.mCerts.add(new Signature(cert));
            return this;
        }

        public PolicyBuilder setGlobalSeinfoOrThrow(String seinfo) {
            if (!validateValue(seinfo)) {
                String err = "Invalid seinfo value " + seinfo;
                throw new IllegalArgumentException(err);
            }
            if (this.mSeinfo != null && !this.mSeinfo.equals(seinfo)) {
                throw new IllegalStateException("Duplicate seinfo tag found");
            }
            this.mSeinfo = seinfo;
            return this;
        }

        public PolicyBuilder addInnerPackageMapOrThrow(String pkgName, String seinfo) {
            if (!validateValue(pkgName)) {
                String err = "Invalid package name " + pkgName;
                throw new IllegalArgumentException(err);
            }
            if (!validateValue(seinfo)) {
                String err2 = "Invalid seinfo value " + seinfo;
                throw new IllegalArgumentException(err2);
            }
            String pkgValue = this.mPkgMap.get(pkgName);
            if (pkgValue != null && !pkgValue.equals(seinfo)) {
                throw new IllegalStateException("Conflicting seinfo value found");
            }
            this.mPkgMap.put(pkgName, seinfo);
            return this;
        }

        private boolean validateValue(String name) {
            return name != null && name.matches("\\A[\\.\\w]+\\z");
        }

        public Policy build() {
            Policy p = new Policy(this, null);
            if (p.mCerts.isEmpty()) {
                throw new IllegalStateException("Missing certs with signer tag. Expecting at least one.");
            }
            if (!((p.mSeinfo == null) ^ p.mPkgMap.isEmpty())) {
                throw new IllegalStateException("Only seinfo tag XOR package tags are allowed within a signer stanza.");
            }
            return p;
        }
    }
}
