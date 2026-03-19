package sun.security.x509;

import java.io.IOException;
import java.util.Locale;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

public class DNSName implements GeneralNameInterface {
    private static final String alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String alphaDigitsAndHyphen = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-";
    private static final String digitsAndHyphen = "0123456789-";
    private String name;

    public DNSName(DerValue derValue) throws IOException {
        this.name = derValue.getIA5String();
    }

    public DNSName(String str) throws IOException {
        if (str == null || str.length() == 0) {
            throw new IOException("DNS name must not be null");
        }
        if (str.indexOf(32) != -1) {
            throw new IOException("DNS names or NameConstraints with blank components are not permitted");
        }
        int i = 0;
        if (str.charAt(0) == '.' || str.charAt(str.length() - 1) == '.') {
            throw new IOException("DNS names or NameConstraints may not begin or end with a .");
        }
        while (i < str.length()) {
            int iIndexOf = str.indexOf(46, i);
            iIndexOf = iIndexOf < 0 ? str.length() : iIndexOf;
            if (iIndexOf - i < 1) {
                throw new IOException("DNSName SubjectAltNames with empty components are not permitted");
            }
            if (alpha.indexOf(str.charAt(i)) < 0) {
                throw new IOException("DNSName components must begin with a letter");
            }
            do {
                i++;
                if (i < iIndexOf) {
                }
            } while (alphaDigitsAndHyphen.indexOf(str.charAt(i)) >= 0);
            throw new IOException("DNSName components must consist of letters, digits, and hyphens");
        }
        this.name = str;
    }

    @Override
    public int getType() {
        return 2;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public void encode(DerOutputStream derOutputStream) throws IOException {
        derOutputStream.putIA5String(this.name);
    }

    public String toString() {
        return "DNSName: " + this.name;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DNSName)) {
            return false;
        }
        return this.name.equalsIgnoreCase(((DNSName) obj).name);
    }

    public int hashCode() {
        return this.name.toUpperCase(Locale.ENGLISH).hashCode();
    }

    @Override
    public int constrains(GeneralNameInterface generalNameInterface) throws UnsupportedOperationException {
        if (generalNameInterface == null || generalNameInterface.getType() != 2) {
            return -1;
        }
        String lowerCase = ((DNSName) generalNameInterface).getName().toLowerCase(Locale.ENGLISH);
        String lowerCase2 = this.name.toLowerCase(Locale.ENGLISH);
        if (lowerCase.equals(lowerCase2)) {
            return 0;
        }
        if (lowerCase2.endsWith(lowerCase)) {
            return lowerCase2.charAt(lowerCase2.lastIndexOf(lowerCase) - 1) == '.' ? 2 : 3;
        }
        if (lowerCase.endsWith(lowerCase2) && lowerCase.charAt(lowerCase.lastIndexOf(lowerCase2) - 1) == '.') {
            return 1;
        }
        return 3;
    }

    @Override
    public int subtreeDepth() throws UnsupportedOperationException {
        int iIndexOf = this.name.indexOf(46);
        int i = 1;
        while (iIndexOf >= 0) {
            i++;
            iIndexOf = this.name.indexOf(46, iIndexOf + 1);
        }
        return i;
    }
}
