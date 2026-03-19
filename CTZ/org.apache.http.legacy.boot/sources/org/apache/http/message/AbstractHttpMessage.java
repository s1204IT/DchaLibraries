package org.apache.http.message;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpMessage;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

@Deprecated
public abstract class AbstractHttpMessage implements HttpMessage {
    protected HeaderGroup headergroup;
    protected HttpParams params;

    protected AbstractHttpMessage(HttpParams httpParams) {
        this.headergroup = new HeaderGroup();
        this.params = httpParams;
    }

    protected AbstractHttpMessage() {
        this(null);
    }

    @Override
    public boolean containsHeader(String str) {
        return this.headergroup.containsHeader(str);
    }

    @Override
    public Header[] getHeaders(String str) {
        return this.headergroup.getHeaders(str);
    }

    @Override
    public Header getFirstHeader(String str) {
        return this.headergroup.getFirstHeader(str);
    }

    @Override
    public Header getLastHeader(String str) {
        return this.headergroup.getLastHeader(str);
    }

    @Override
    public Header[] getAllHeaders() {
        return this.headergroup.getAllHeaders();
    }

    @Override
    public void addHeader(Header header) {
        this.headergroup.addHeader(header);
    }

    @Override
    public void addHeader(String str, String str2) {
        if (str == null) {
            throw new IllegalArgumentException("Header name may not be null");
        }
        this.headergroup.addHeader(new BasicHeader(str, str2));
    }

    @Override
    public void setHeader(Header header) {
        this.headergroup.updateHeader(header);
    }

    @Override
    public void setHeader(String str, String str2) {
        if (str == null) {
            throw new IllegalArgumentException("Header name may not be null");
        }
        this.headergroup.updateHeader(new BasicHeader(str, str2));
    }

    @Override
    public void setHeaders(Header[] headerArr) {
        this.headergroup.setHeaders(headerArr);
    }

    @Override
    public void removeHeader(Header header) {
        this.headergroup.removeHeader(header);
    }

    @Override
    public void removeHeaders(String str) {
        if (str == null) {
            return;
        }
        HeaderIterator it = this.headergroup.iterator();
        while (it.hasNext()) {
            if (str.equalsIgnoreCase(((Header) it.next()).getName())) {
                it.remove();
            }
        }
    }

    @Override
    public HeaderIterator headerIterator() {
        return this.headergroup.iterator();
    }

    @Override
    public HeaderIterator headerIterator(String str) {
        return this.headergroup.iterator(str);
    }

    @Override
    public HttpParams getParams() {
        if (this.params == null) {
            this.params = new BasicHttpParams();
        }
        return this.params;
    }

    @Override
    public void setParams(HttpParams httpParams) {
        if (httpParams == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        this.params = httpParams;
    }
}
