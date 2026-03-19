package com.android.server.pm;

class IntentFilterVerificationKey {
    public String className;
    public String domains;
    public String packageName;

    public IntentFilterVerificationKey(String[] strArr, String str, String str2) {
        StringBuilder sb = new StringBuilder();
        for (String str3 : strArr) {
            sb.append(str3);
        }
        this.domains = sb.toString();
        this.packageName = str;
        this.className = str2;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        IntentFilterVerificationKey intentFilterVerificationKey = (IntentFilterVerificationKey) obj;
        if (this.domains == null ? intentFilterVerificationKey.domains != null : !this.domains.equals(intentFilterVerificationKey.domains)) {
            return false;
        }
        if (this.className == null ? intentFilterVerificationKey.className != null : !this.className.equals(intentFilterVerificationKey.className)) {
            return false;
        }
        if (this.packageName == null ? intentFilterVerificationKey.packageName == null : this.packageName.equals(intentFilterVerificationKey.packageName)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * (((this.domains != null ? this.domains.hashCode() : 0) * 31) + (this.packageName != null ? this.packageName.hashCode() : 0))) + (this.className != null ? this.className.hashCode() : 0);
    }
}
