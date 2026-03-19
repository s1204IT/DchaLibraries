package org.apache.http.message;

import java.util.Locale;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.ReasonPhraseCatalog;
import org.apache.http.StatusLine;

@Deprecated
public class BasicHttpResponse extends AbstractHttpMessage implements HttpResponse {
    private HttpEntity entity;
    private Locale locale;
    private ReasonPhraseCatalog reasonCatalog;
    private StatusLine statusline;

    public BasicHttpResponse(StatusLine statusLine, ReasonPhraseCatalog reasonPhraseCatalog, Locale locale) {
        if (statusLine == null) {
            throw new IllegalArgumentException("Status line may not be null.");
        }
        this.statusline = statusLine;
        this.reasonCatalog = reasonPhraseCatalog;
        this.locale = locale == null ? Locale.getDefault() : locale;
    }

    public BasicHttpResponse(StatusLine statusLine) {
        this(statusLine, (ReasonPhraseCatalog) null, (Locale) null);
    }

    public BasicHttpResponse(ProtocolVersion protocolVersion, int i, String str) {
        this(new BasicStatusLine(protocolVersion, i, str), (ReasonPhraseCatalog) null, (Locale) null);
    }

    @Override
    public ProtocolVersion getProtocolVersion() {
        return this.statusline.getProtocolVersion();
    }

    @Override
    public StatusLine getStatusLine() {
        return this.statusline;
    }

    @Override
    public HttpEntity getEntity() {
        return this.entity;
    }

    @Override
    public Locale getLocale() {
        return this.locale;
    }

    @Override
    public void setStatusLine(StatusLine statusLine) {
        if (statusLine == null) {
            throw new IllegalArgumentException("Status line may not be null");
        }
        this.statusline = statusLine;
    }

    @Override
    public void setStatusLine(ProtocolVersion protocolVersion, int i) {
        this.statusline = new BasicStatusLine(protocolVersion, i, getReason(i));
    }

    @Override
    public void setStatusLine(ProtocolVersion protocolVersion, int i, String str) {
        this.statusline = new BasicStatusLine(protocolVersion, i, str);
    }

    @Override
    public void setStatusCode(int i) {
        this.statusline = new BasicStatusLine(this.statusline.getProtocolVersion(), i, getReason(i));
    }

    @Override
    public void setReasonPhrase(String str) {
        if (str != null && (str.indexOf(10) >= 0 || str.indexOf(13) >= 0)) {
            throw new IllegalArgumentException("Line break in reason phrase.");
        }
        this.statusline = new BasicStatusLine(this.statusline.getProtocolVersion(), this.statusline.getStatusCode(), str);
    }

    @Override
    public void setEntity(HttpEntity httpEntity) {
        this.entity = httpEntity;
    }

    @Override
    public void setLocale(Locale locale) {
        if (locale == null) {
            throw new IllegalArgumentException("Locale may not be null.");
        }
        this.locale = locale;
        int statusCode = this.statusline.getStatusCode();
        this.statusline = new BasicStatusLine(this.statusline.getProtocolVersion(), statusCode, getReason(statusCode));
    }

    protected String getReason(int i) {
        if (this.reasonCatalog == null) {
            return null;
        }
        return this.reasonCatalog.getReason(i, this.locale);
    }
}
