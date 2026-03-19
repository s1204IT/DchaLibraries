package org.apache.http.impl.cookie;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.cookie.SetCookie;

@Deprecated
public class BasicClientCookie implements SetCookie, ClientCookie, Cloneable {
    private Map<String, String> attribs;
    private String cookieComment;
    private String cookieDomain;
    private Date cookieExpiryDate;
    private String cookiePath;
    private int cookieVersion;
    private boolean isSecure;
    private final String name;
    private String value;

    public BasicClientCookie(String name, String value) {
        if (name == null) {
            throw new IllegalArgumentException("Name may not be null");
        }
        this.name = name;
        this.attribs = new HashMap();
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

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getComment() {
        return this.cookieComment;
    }

    @Override
    public void setComment(String comment) {
        this.cookieComment = comment;
    }

    @Override
    public String getCommentURL() {
        return null;
    }

    @Override
    public Date getExpiryDate() {
        return this.cookieExpiryDate;
    }

    @Override
    public void setExpiryDate(Date expiryDate) {
        this.cookieExpiryDate = expiryDate;
    }

    @Override
    public boolean isPersistent() {
        return this.cookieExpiryDate != null;
    }

    @Override
    public String getDomain() {
        return this.cookieDomain;
    }

    @Override
    public void setDomain(String domain) {
        if (domain != null) {
            this.cookieDomain = domain.toLowerCase(Locale.ENGLISH);
        } else {
            this.cookieDomain = null;
        }
    }

    @Override
    public String getPath() {
        return this.cookiePath;
    }

    @Override
    public void setPath(String path) {
        this.cookiePath = path;
    }

    @Override
    public boolean isSecure() {
        return this.isSecure;
    }

    @Override
    public void setSecure(boolean secure) {
        this.isSecure = secure;
    }

    @Override
    public int[] getPorts() {
        return null;
    }

    @Override
    public int getVersion() {
        return this.cookieVersion;
    }

    @Override
    public void setVersion(int version) {
        this.cookieVersion = version;
    }

    @Override
    public boolean isExpired(Date date) {
        if (date == null) {
            throw new IllegalArgumentException("Date may not be null");
        }
        return this.cookieExpiryDate != null && this.cookieExpiryDate.getTime() <= date.getTime();
    }

    public void setAttribute(String name, String value) {
        this.attribs.put(name, value);
    }

    @Override
    public String getAttribute(String name) {
        return this.attribs.get(name);
    }

    @Override
    public boolean containsAttribute(String name) {
        return this.attribs.get(name) != null;
    }

    public Object clone() throws CloneNotSupportedException {
        BasicClientCookie clone = (BasicClientCookie) super.clone();
        clone.attribs = new HashMap(this.attribs);
        return clone;
    }

    public String toString() {
        return "[version: " + Integer.toString(this.cookieVersion) + "][name: " + this.name + "][value: " + this.value + "][domain: " + this.cookieDomain + "][path: " + this.cookiePath + "][expiry: " + this.cookieExpiryDate + "]";
    }
}
