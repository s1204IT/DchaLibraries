package sun.security.x509;

public class X509AttributeName {
    private static final char SEPARATOR = '.';
    private String prefix;
    private String suffix;

    public X509AttributeName(String str) {
        this.prefix = null;
        this.suffix = null;
        int iIndexOf = str.indexOf(46);
        if (iIndexOf < 0) {
            this.prefix = str;
        } else {
            this.prefix = str.substring(0, iIndexOf);
            this.suffix = str.substring(iIndexOf + 1);
        }
    }

    public String getPrefix() {
        return this.prefix;
    }

    public String getSuffix() {
        return this.suffix;
    }
}
