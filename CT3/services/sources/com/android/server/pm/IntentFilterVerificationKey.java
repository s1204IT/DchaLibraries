package com.android.server.pm;

class IntentFilterVerificationKey {
    public String className;
    public String domains;
    public String packageName;

    public IntentFilterVerificationKey(String[] domains, String packageName, String className) {
        StringBuilder sb = new StringBuilder();
        for (String host : domains) {
            sb.append(host);
        }
        this.domains = sb.toString();
        this.packageName = packageName;
        this.className = className;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IntentFilterVerificationKey that = (IntentFilterVerificationKey) o;
        if (this.domains == null ? that.domains != null : !this.domains.equals(that.domains)) {
            return false;
        }
        if (this.className == null ? that.className == null : this.className.equals(that.className)) {
            return this.packageName == null ? that.packageName == null : this.packageName.equals(that.packageName);
        }
        return false;
    }

    public int hashCode() {
        int result = this.domains != null ? this.domains.hashCode() : 0;
        return (((result * 31) + (this.packageName != null ? this.packageName.hashCode() : 0)) * 31) + (this.className != null ? this.className.hashCode() : 0);
    }
}
