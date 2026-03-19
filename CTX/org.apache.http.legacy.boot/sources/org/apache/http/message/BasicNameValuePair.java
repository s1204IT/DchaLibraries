package org.apache.http.message;

import org.apache.http.NameValuePair;
import org.apache.http.util.CharArrayBuffer;
import org.apache.http.util.LangUtils;

@Deprecated
public class BasicNameValuePair implements NameValuePair, Cloneable {
    private final String name;
    private final String value;

    public BasicNameValuePair(String str, String str2) {
        if (str == null) {
            throw new IllegalArgumentException("Name may not be null");
        }
        this.name = str;
        this.value = str2;
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
        int length = this.name.length();
        if (this.value != null) {
            length += 1 + this.value.length();
        }
        CharArrayBuffer charArrayBuffer = new CharArrayBuffer(length);
        charArrayBuffer.append(this.name);
        if (this.value != null) {
            charArrayBuffer.append("=");
            charArrayBuffer.append(this.value);
        }
        return charArrayBuffer.toString();
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof NameValuePair)) {
            return false;
        }
        BasicNameValuePair basicNameValuePair = (BasicNameValuePair) obj;
        if (!this.name.equals(basicNameValuePair.name) || !LangUtils.equals(this.value, basicNameValuePair.value)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return LangUtils.hashCode(LangUtils.hashCode(17, this.name), this.value);
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
