package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import gov.nist.javax.sip.SIPConstants;
import gov.nist.javax.sip.address.GenericURI;
import javax.sip.address.URI;

public class RequestLine extends SIPObject implements SipRequestLine {
    private static final long serialVersionUID = -3286426172326043129L;
    protected String method;
    protected String sipVersion = SIPConstants.SIP_VERSION_STRING;
    protected GenericURI uri;

    public RequestLine() {
    }

    @Override
    public String encode() {
        return encode(new StringBuffer()).toString();
    }

    @Override
    public StringBuffer encode(StringBuffer stringBuffer) {
        if (this.method != null) {
            stringBuffer.append(this.method);
            stringBuffer.append(Separators.SP);
        }
        if (this.uri != null) {
            this.uri.encode(stringBuffer);
            stringBuffer.append(Separators.SP);
        }
        stringBuffer.append(this.sipVersion);
        stringBuffer.append(Separators.NEWLINE);
        return stringBuffer;
    }

    @Override
    public GenericURI getUri() {
        return this.uri;
    }

    public RequestLine(GenericURI genericURI, String str) {
        this.uri = genericURI;
        this.method = str;
    }

    @Override
    public String getMethod() {
        return this.method;
    }

    @Override
    public String getSipVersion() {
        return this.sipVersion;
    }

    @Override
    public void setUri(URI uri) {
        this.uri = (GenericURI) uri;
    }

    @Override
    public void setMethod(String str) {
        this.method = str;
    }

    @Override
    public void setSipVersion(String str) {
        this.sipVersion = str;
    }

    @Override
    public String getVersionMajor() {
        String str = null;
        if (this.sipVersion == null) {
            return null;
        }
        boolean z = false;
        for (int i = 0; i < this.sipVersion.length() && this.sipVersion.charAt(i) != '.'; i++) {
            if (z) {
                str = str == null ? "" + this.sipVersion.charAt(i) : str + this.sipVersion.charAt(i);
            }
            if (this.sipVersion.charAt(i) == '/') {
                z = true;
            }
        }
        return str;
    }

    @Override
    public String getVersionMinor() {
        if (this.sipVersion == null) {
            return null;
        }
        String str = null;
        boolean z = false;
        for (int i = 0; i < this.sipVersion.length(); i++) {
            if (z) {
                str = str == null ? "" + this.sipVersion.charAt(i) : str + this.sipVersion.charAt(i);
            }
            if (this.sipVersion.charAt(i) == '.') {
                z = true;
            }
        }
        return str;
    }

    @Override
    public boolean equals(Object obj) {
        if (!obj.getClass().equals(getClass())) {
            return false;
        }
        RequestLine requestLine = (RequestLine) obj;
        try {
            if (this.method.equals(requestLine.method) && this.uri.equals(requestLine.uri)) {
                return this.sipVersion.equals(requestLine.sipVersion);
            }
            return false;
        } catch (NullPointerException e) {
            return false;
        }
    }

    @Override
    public Object clone() {
        RequestLine requestLine = (RequestLine) super.clone();
        if (this.uri != null) {
            requestLine.uri = (GenericURI) this.uri.clone();
        }
        return requestLine;
    }
}
