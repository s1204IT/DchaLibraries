package org.apache.http.message;

import gov.nist.core.Separators;
import org.apache.http.NameValuePair;
import org.apache.http.util.CharArrayBuffer;
import org.apache.http.util.LangUtils;

@Deprecated
public class BasicNameValuePair implements NameValuePair, Cloneable {
    private final String name;
    private final String value;

    public BasicNameValuePair(String name, String value) {
        if (name == null) {
            throw new IllegalArgumentException("Name may not be null");
        }
        this.name = name;
        this.value = value;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getValue() {
        return this.value;
    }

    public String toString() {
        int len = this.name.length();
        if (this.value != null) {
            len += this.value.length() + 1;
        }
        CharArrayBuffer buffer = new CharArrayBuffer(len);
        buffer.append(this.name);
        if (this.value != null) {
            buffer.append(Separators.EQUALS);
            buffer.append(this.value);
        }
        return buffer.toString();
    }

    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (this == object) {
            return true;
        }
        if (!(object instanceof NameValuePair)) {
            return false;
        }
        BasicNameValuePair that = (BasicNameValuePair) object;
        return this.name.equals(that.name) && LangUtils.equals(this.value, that.value);
    }

    public int hashCode() {
        int hash = LangUtils.hashCode(17, this.name);
        return LangUtils.hashCode(hash, this.value);
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
