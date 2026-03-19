package javax.xml.namespace;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

public class QName implements Serializable {
    private static final long compatibilitySerialVersionUID = 4418622981026545151L;
    private static final long defaultSerialVersionUID = -9120448754896609940L;
    private static final long serialVersionUID;
    private final String localPart;
    private final String namespaceURI;
    private String prefix;
    private transient String qNameAsString;

    static {
        serialVersionUID = !"1.0".equals(System.getProperty("org.apache.xml.namespace.QName.useCompatibleSerialVersionUID")) ? defaultSerialVersionUID : compatibilitySerialVersionUID;
    }

    public QName(String str, String str2) {
        this(str, str2, "");
    }

    public QName(String str, String str2, String str3) {
        if (str == null) {
            this.namespaceURI = "";
        } else {
            this.namespaceURI = str;
        }
        if (str2 == null) {
            throw new IllegalArgumentException("local part cannot be \"null\" when creating a QName");
        }
        this.localPart = str2;
        if (str3 == null) {
            throw new IllegalArgumentException("prefix cannot be \"null\" when creating a QName");
        }
        this.prefix = str3;
    }

    public QName(String str) {
        this("", str, "");
    }

    public String getNamespaceURI() {
        return this.namespaceURI;
    }

    public String getLocalPart() {
        return this.localPart;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof QName)) {
            return false;
        }
        QName qName = (QName) obj;
        return this.localPart.equals(qName.localPart) && this.namespaceURI.equals(qName.namespaceURI);
    }

    public final int hashCode() {
        return this.namespaceURI.hashCode() ^ this.localPart.hashCode();
    }

    public String toString() {
        String string = this.qNameAsString;
        if (string == null) {
            int length = this.namespaceURI.length();
            if (length == 0) {
                string = this.localPart;
            } else {
                StringBuilder sb = new StringBuilder(length + this.localPart.length() + 2);
                sb.append('{');
                sb.append(this.namespaceURI);
                sb.append('}');
                sb.append(this.localPart);
                string = sb.toString();
            }
            this.qNameAsString = string;
        }
        return string;
    }

    public static QName valueOf(String str) {
        if (str == null) {
            throw new IllegalArgumentException("cannot create QName from \"null\" or \"\" String");
        }
        if (str.length() == 0) {
            return new QName("", str, "");
        }
        if (str.charAt(0) != '{') {
            return new QName("", str, "");
        }
        if (str.startsWith("{}")) {
            throw new IllegalArgumentException("Namespace URI .equals(XMLConstants.NULL_NS_URI), .equals(\"\"), only the local part, \"" + str.substring(2 + "".length()) + "\", should be provided.");
        }
        int iIndexOf = str.indexOf(125);
        if (iIndexOf == -1) {
            throw new IllegalArgumentException("cannot create QName from \"" + str + "\", missing closing \"}\"");
        }
        return new QName(str.substring(1, iIndexOf), str.substring(iIndexOf + 1), "");
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        if (this.prefix == null) {
            this.prefix = "";
        }
    }
}
