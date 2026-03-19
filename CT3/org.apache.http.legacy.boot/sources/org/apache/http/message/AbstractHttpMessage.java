package org.apache.http.message;

import java.util.Iterator;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpMessage;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

@Deprecated
public abstract class AbstractHttpMessage implements HttpMessage {
    protected HeaderGroup headergroup;
    protected HttpParams params;

    protected AbstractHttpMessage(HttpParams params) {
        this.headergroup = new HeaderGroup();
        this.params = params;
    }

    protected AbstractHttpMessage() {
        this(null);
    }

    @Override
    public boolean containsHeader(String name) {
        return this.headergroup.containsHeader(name);
    }

    @Override
    public Header[] getHeaders(String name) {
        return this.headergroup.getHeaders(name);
    }

    @Override
    public Header getFirstHeader(String name) {
        return this.headergroup.getFirstHeader(name);
    }

    @Override
    public Header getLastHeader(String name) {
        return this.headergroup.getLastHeader(name);
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
    public void addHeader(String name, String value) {
        if (name == null) {
            throw new IllegalArgumentException("Header name may not be null");
        }
        this.headergroup.addHeader(new BasicHeader(name, value));
    }

    @Override
    public void setHeader(Header header) {
        this.headergroup.updateHeader(header);
    }

    @Override
    public void setHeader(String name, String value) {
        if (name == null) {
            throw new IllegalArgumentException("Header name may not be null");
        }
        this.headergroup.updateHeader(new BasicHeader(name, value));
    }

    @Override
    public void setHeaders(Header[] headers) {
        this.headergroup.setHeaders(headers);
    }

    @Override
    public void removeHeader(Header header) {
        this.headergroup.removeHeader(header);
    }

    @Override
    public void removeHeaders(String name) {
        if (name == null) {
            return;
        }
        Iterator i = this.headergroup.iterator();
        while (i.hasNext()) {
            Header header = (Header) i.next();
            if (name.equalsIgnoreCase(header.getName())) {
                i.remove();
            }
        }
    }

    @Override
    public HeaderIterator headerIterator() {
        return this.headergroup.iterator();
    }

    @Override
    public HeaderIterator headerIterator(String name) {
        return this.headergroup.iterator(name);
    }

    @Override
    public HttpParams getParams() {
        if (this.params == null) {
            this.params = new BasicHttpParams();
        }
        return this.params;
    }

    @Override
    public void setParams(HttpParams params) {
        if (params == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        this.params = params;
    }
}
