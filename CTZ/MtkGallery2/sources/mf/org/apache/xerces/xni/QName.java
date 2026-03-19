package mf.org.apache.xerces.xni;

public class QName implements Cloneable {
    public String localpart;
    public String prefix;
    public String rawname;
    public String uri;

    public QName() {
        clear();
    }

    public QName(String prefix, String localpart, String rawname, String uri) {
        setValues(prefix, localpart, rawname, uri);
    }

    public QName(QName qname) {
        setValues(qname);
    }

    public void setValues(QName qname) {
        this.prefix = qname.prefix;
        this.localpart = qname.localpart;
        this.rawname = qname.rawname;
        this.uri = qname.uri;
    }

    public void setValues(String prefix, String localpart, String rawname, String uri) {
        this.prefix = prefix;
        this.localpart = localpart;
        this.rawname = rawname;
        this.uri = uri;
    }

    public void clear() {
        this.prefix = null;
        this.localpart = null;
        this.rawname = null;
        this.uri = null;
    }

    public Object clone() {
        return new QName(this);
    }

    public int hashCode() {
        if (this.uri != null) {
            return this.uri.hashCode() + (this.localpart != null ? this.localpart.hashCode() : 0);
        }
        if (this.rawname != null) {
            return this.rawname.hashCode();
        }
        return 0;
    }

    public boolean equals(Object object) {
        if (object instanceof QName) {
            QName qname = (QName) object;
            return qname.uri != null ? this.uri == qname.uri && this.localpart == qname.localpart : this.uri == null && this.rawname == qname.rawname;
        }
        return false;
    }

    public String toString() {
        StringBuffer str = new StringBuffer();
        boolean comma = false;
        if (this.prefix != null) {
            str.append("prefix=\"");
            str.append(this.prefix);
            str.append('\"');
            comma = true;
        }
        if (this.localpart != null) {
            if (comma) {
                str.append(',');
            }
            str.append("localpart=\"");
            str.append(this.localpart);
            str.append('\"');
            comma = true;
        }
        if (this.rawname != null) {
            if (comma) {
                str.append(',');
            }
            str.append("rawname=\"");
            str.append(this.rawname);
            str.append('\"');
            comma = true;
        }
        if (this.uri != null) {
            if (comma) {
                str.append(',');
            }
            str.append("uri=\"");
            str.append(this.uri);
            str.append('\"');
        }
        return str.toString();
    }
}
