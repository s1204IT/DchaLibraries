package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import gov.nist.javax.sip.SIPConstants;

public final class StatusLine extends SIPObject implements SipStatusLine {
    private static final long serialVersionUID = -4738092215519950414L;
    protected boolean matchStatusClass;
    protected String reasonPhrase = null;
    protected String sipVersion = SIPConstants.SIP_VERSION_STRING;
    protected int statusCode;

    @Override
    public boolean match(Object obj) {
        if (!(obj instanceof StatusLine)) {
            return false;
        }
        StatusLine statusLine = (StatusLine) obj;
        if (statusLine.matchExpression != null) {
            return statusLine.matchExpression.match(encode());
        }
        if (statusLine.sipVersion != null && !statusLine.sipVersion.equals(this.sipVersion)) {
            return false;
        }
        if (statusLine.statusCode != 0) {
            if (this.matchStatusClass) {
                int i = statusLine.statusCode;
                if (Integer.toString(statusLine.statusCode).charAt(0) != Integer.toString(this.statusCode).charAt(0)) {
                    return false;
                }
            } else if (this.statusCode != statusLine.statusCode) {
                return false;
            }
        }
        if (statusLine.reasonPhrase == null || this.reasonPhrase == statusLine.reasonPhrase) {
            return true;
        }
        return this.reasonPhrase.equals(statusLine.reasonPhrase);
    }

    public void setMatchStatusClass(boolean z) {
        this.matchStatusClass = z;
    }

    @Override
    public String encode() {
        String str = "SIP/2.0 " + this.statusCode;
        if (this.reasonPhrase != null) {
            str = str + Separators.SP + this.reasonPhrase;
        }
        return str + Separators.NEWLINE;
    }

    @Override
    public String getSipVersion() {
        return this.sipVersion;
    }

    @Override
    public int getStatusCode() {
        return this.statusCode;
    }

    @Override
    public String getReasonPhrase() {
        return this.reasonPhrase;
    }

    @Override
    public void setSipVersion(String str) {
        this.sipVersion = str;
    }

    @Override
    public void setStatusCode(int i) {
        this.statusCode = i;
    }

    @Override
    public void setReasonPhrase(String str) {
        this.reasonPhrase = str;
    }

    @Override
    public String getVersionMajor() {
        if (this.sipVersion == null) {
            return null;
        }
        boolean z = false;
        String str = null;
        for (int i = 0; i < this.sipVersion.length(); i++) {
            if (this.sipVersion.charAt(i) == '.') {
                z = false;
            }
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
}
