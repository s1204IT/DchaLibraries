package sun.security.x509;

import java.io.IOException;
import java.util.Locale;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

public class RFC822Name implements GeneralNameInterface {
    private String name;

    public RFC822Name(DerValue derValue) throws IOException {
        this.name = derValue.getIA5String();
        parseName(this.name);
    }

    public RFC822Name(String str) throws IOException {
        parseName(str);
        this.name = str;
    }

    public void parseName(String str) throws IOException {
        if (str == null || str.length() == 0) {
            throw new IOException("RFC822Name may not be null or empty");
        }
        String strSubstring = str.substring(str.indexOf(64) + 1);
        if (strSubstring.length() == 0) {
            throw new IOException("RFC822Name may not end with @");
        }
        if (strSubstring.startsWith(".") && strSubstring.length() == 1) {
            throw new IOException("RFC822Name domain may not be just .");
        }
    }

    @Override
    public int getType() {
        return 1;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public void encode(DerOutputStream derOutputStream) throws IOException {
        derOutputStream.putIA5String(this.name);
    }

    public String toString() {
        return "RFC822Name: " + this.name;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RFC822Name)) {
            return false;
        }
        return this.name.equalsIgnoreCase(((RFC822Name) obj).name);
    }

    public int hashCode() {
        return this.name.toUpperCase(Locale.ENGLISH).hashCode();
    }

    @Override
    public int constrains(GeneralNameInterface generalNameInterface) throws UnsupportedOperationException {
        if (generalNameInterface == null || generalNameInterface.getType() != 1) {
            return -1;
        }
        String lowerCase = ((RFC822Name) generalNameInterface).getName().toLowerCase(Locale.ENGLISH);
        String lowerCase2 = this.name.toLowerCase(Locale.ENGLISH);
        if (lowerCase.equals(lowerCase2)) {
            return 0;
        }
        if (lowerCase2.endsWith(lowerCase)) {
            if (lowerCase.indexOf(64) == -1) {
                return (lowerCase.startsWith(".") || lowerCase2.charAt(lowerCase2.lastIndexOf(lowerCase) - 1) == '@') ? 2 : 3;
            }
        } else if (lowerCase.endsWith(lowerCase2) && lowerCase2.indexOf(64) == -1) {
            if (!lowerCase2.startsWith(".") && lowerCase.charAt(lowerCase.lastIndexOf(lowerCase2) - 1) != '@') {
                return 3;
            }
            return 1;
        }
        return 3;
    }

    @Override
    public int subtreeDepth() throws UnsupportedOperationException {
        String strSubstring = this.name;
        int iLastIndexOf = strSubstring.lastIndexOf(64);
        int i = 1;
        if (iLastIndexOf >= 0) {
            strSubstring = strSubstring.substring(iLastIndexOf + 1);
            i = 2;
        }
        while (strSubstring.lastIndexOf(46) >= 0) {
            strSubstring = strSubstring.substring(0, strSubstring.lastIndexOf(46));
            i++;
        }
        return i;
    }
}
