package android.security.net.config;

import java.util.Locale;

public final class Domain {
    public final String hostname;
    public final boolean subdomainsIncluded;

    public Domain(String hostname, boolean subdomainsIncluded) {
        if (hostname == null) {
            throw new NullPointerException("Hostname must not be null");
        }
        this.hostname = hostname.toLowerCase(Locale.US);
        this.subdomainsIncluded = subdomainsIncluded;
    }

    public int hashCode() {
        return (this.subdomainsIncluded ? 1231 : 1237) ^ this.hostname.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if ((obj instanceof Domain) && obj.subdomainsIncluded == this.subdomainsIncluded) {
            return obj.hostname.equals(this.hostname);
        }
        return false;
    }
}
