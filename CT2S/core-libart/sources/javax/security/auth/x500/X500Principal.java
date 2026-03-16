package javax.security.auth.x500;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.Principal;
import java.util.Map;
import org.apache.harmony.security.x501.Name;

public final class X500Principal implements Serializable, Principal {
    public static final String CANONICAL = "CANONICAL";
    public static final String RFC1779 = "RFC1779";
    public static final String RFC2253 = "RFC2253";
    private static final long serialVersionUID = -500463348111345721L;
    private transient String canonicalName;
    private transient Name dn;

    public X500Principal(byte[] name) {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        try {
            this.dn = (Name) Name.ASN1.decode(name);
        } catch (IOException e) {
            throw incorrectInputEncoding(e);
        }
    }

    public X500Principal(InputStream in) {
        if (in == null) {
            throw new NullPointerException("in == null");
        }
        try {
            this.dn = (Name) Name.ASN1.decode(in);
        } catch (IOException e) {
            throw incorrectInputEncoding(e);
        }
    }

    private IllegalArgumentException incorrectInputEncoding(IOException e) {
        IllegalArgumentException iae = new IllegalArgumentException("Incorrect input encoding");
        iae.initCause(e);
        throw iae;
    }

    public X500Principal(String name) {
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        try {
            this.dn = new Name(name);
        } catch (IOException e) {
            throw incorrectInputName(e, name);
        }
    }

    public X500Principal(String name, Map<String, String> keywordMap) {
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        try {
            this.dn = new Name(substituteNameFromMap(name, keywordMap));
        } catch (IOException e) {
            throw incorrectInputName(e, name);
        }
    }

    private IllegalArgumentException incorrectInputName(IOException e, String name) {
        IllegalArgumentException iae = new IllegalArgumentException("Incorrect input name:" + name);
        iae.initCause(e);
        throw iae;
    }

    private synchronized String getCanonicalName() {
        if (this.canonicalName == null) {
            this.canonicalName = this.dn.getName(CANONICAL);
        }
        return this.canonicalName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        X500Principal principal = (X500Principal) o;
        return getCanonicalName().equals(principal.getCanonicalName());
    }

    public byte[] getEncoded() {
        byte[] src = this.dn.getEncoded();
        byte[] dst = new byte[src.length];
        System.arraycopy(src, 0, dst, 0, dst.length);
        return dst;
    }

    @Override
    public String getName() {
        return this.dn.getName(RFC2253);
    }

    public String getName(String format) {
        return CANONICAL.equals(format) ? getCanonicalName() : this.dn.getName(format);
    }

    public String getName(String format, Map<String, String> oidMap) {
        String rfc1779Name = this.dn.getName(RFC1779);
        String rfc2253Name = this.dn.getName(RFC2253);
        if (format.equalsIgnoreCase(RFC1779)) {
            StringBuilder resultName = new StringBuilder(rfc1779Name);
            int fromIndex = resultName.length();
            while (true) {
                int equalIndex = resultName.lastIndexOf("=", fromIndex);
                if (-1 != equalIndex) {
                    int commaIndex = resultName.lastIndexOf(",", equalIndex);
                    String subName = resultName.substring(commaIndex + 1, equalIndex).trim();
                    if (subName.length() > 4 && subName.substring(0, 4).equals("OID.")) {
                        String subSubName = subName.substring(4);
                        if (oidMap.containsKey(subSubName)) {
                            String replaceName = oidMap.get(subSubName);
                            if (commaIndex > 0) {
                                replaceName = " " + replaceName;
                            }
                            resultName.replace(commaIndex + 1, equalIndex, replaceName);
                        }
                    }
                    fromIndex = commaIndex;
                } else {
                    return resultName.toString();
                }
            }
        } else if (format.equalsIgnoreCase(RFC2253)) {
            StringBuilder resultName2 = new StringBuilder(rfc2253Name);
            StringBuilder subsidyName = new StringBuilder(rfc1779Name);
            int fromIndex2 = resultName2.length();
            int subsidyFromIndex = subsidyName.length();
            while (true) {
                int equalIndex2 = resultName2.lastIndexOf("=", fromIndex2);
                if (-1 != equalIndex2) {
                    int subsidyEqualIndex = subsidyName.lastIndexOf("=", subsidyFromIndex);
                    int commaIndex2 = resultName2.lastIndexOf(",", equalIndex2);
                    String subName2 = resultName2.substring(commaIndex2 + 1, equalIndex2).trim();
                    if (oidMap.containsKey(subName2)) {
                        int subOrignalEndIndex = resultName2.indexOf(",", equalIndex2);
                        if (subOrignalEndIndex == -1) {
                            subOrignalEndIndex = resultName2.length();
                        }
                        int subGoalEndIndex = subsidyName.indexOf(",", subsidyEqualIndex);
                        if (subGoalEndIndex == -1) {
                            subGoalEndIndex = subsidyName.length();
                        }
                        resultName2.replace(equalIndex2 + 1, subOrignalEndIndex, subsidyName.substring(subsidyEqualIndex + 1, subGoalEndIndex));
                        resultName2.replace(commaIndex2 + 1, equalIndex2, oidMap.get(subName2));
                    }
                    fromIndex2 = commaIndex2;
                    subsidyFromIndex = subsidyEqualIndex - 1;
                } else {
                    return resultName2.toString();
                }
            }
        } else {
            throw new IllegalArgumentException("invalid format specified: " + format);
        }
    }

    @Override
    public int hashCode() {
        return getCanonicalName().hashCode();
    }

    @Override
    public String toString() {
        return this.dn.getName(RFC1779);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(this.dn.getEncoded());
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        this.dn = (Name) Name.ASN1.decode((byte[]) in.readObject());
    }

    private String substituteNameFromMap(String name, Map<String, String> keywordMap) {
        StringBuilder sbName = new StringBuilder(name);
        int fromIndex = sbName.length();
        while (true) {
            int equalIndex = sbName.lastIndexOf("=", fromIndex);
            if (-1 != equalIndex) {
                int commaIndex = sbName.lastIndexOf(",", equalIndex);
                String subName = sbName.substring(commaIndex + 1, equalIndex).trim();
                if (keywordMap.containsKey(subName)) {
                    sbName.replace(commaIndex + 1, equalIndex, keywordMap.get(subName));
                }
                fromIndex = commaIndex;
            } else {
                return sbName.toString();
            }
        }
    }
}
