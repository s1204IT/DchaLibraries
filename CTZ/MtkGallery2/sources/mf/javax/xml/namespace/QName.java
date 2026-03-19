package mf.javax.xml.namespace;

import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class QName implements Serializable {
    private static final long serialVersionUID;
    private static boolean useDefaultSerialVersionUID;
    private final String localPart;
    private final String namespaceURI;
    private final String prefix;

    static {
        useDefaultSerialVersionUID = true;
        try {
            String valueUseCompatibleSerialVersionUID = (String) AccessController.doPrivileged(new PrivilegedAction() {
                @Override
                public Object run() {
                    return System.getProperty("com.sun.xml.namespace.QName.useCompatibleSerialVersionUID");
                }
            });
            useDefaultSerialVersionUID = valueUseCompatibleSerialVersionUID == null || !valueUseCompatibleSerialVersionUID.equals("1.0");
        } catch (Exception e) {
            useDefaultSerialVersionUID = true;
        }
        if (useDefaultSerialVersionUID) {
            serialVersionUID = -9120448754896609940L;
        } else {
            serialVersionUID = 4418622981026545151L;
        }
    }

    public QName(String namespaceURI, String localPart) {
        this(namespaceURI, localPart, "");
    }

    public QName(String namespaceURI, String localPart, String prefix) {
        if (namespaceURI == null) {
            this.namespaceURI = "";
        } else {
            this.namespaceURI = namespaceURI;
        }
        if (localPart == null) {
            throw new IllegalArgumentException("local part cannot be \"null\" when creating a QName");
        }
        this.localPart = localPart;
        if (prefix == null) {
            throw new IllegalArgumentException("prefix cannot be \"null\" when creating a QName");
        }
        this.prefix = prefix;
    }

    public QName(String localPart) {
        this("", localPart, "");
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
        if (obj != 0 && (obj instanceof QName) && this.localPart.equals(obj.localPart) && this.namespaceURI.equals(obj.namespaceURI)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return this.namespaceURI.hashCode() ^ this.localPart.hashCode();
    }

    public String toString() {
        if (this.namespaceURI.equals("")) {
            return this.localPart;
        }
        return "{" + this.namespaceURI + "}" + this.localPart;
    }
}
