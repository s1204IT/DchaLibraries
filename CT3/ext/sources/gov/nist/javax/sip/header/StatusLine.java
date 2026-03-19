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
        if (obj.matchExpression != null) {
            return obj.matchExpression.match(encode());
        }
        if (obj.sipVersion != null && !obj.sipVersion.equals(this.sipVersion)) {
            return false;
        }
        if (obj.statusCode != 0) {
            if (this.matchStatusClass) {
                int i = obj.statusCode;
                String codeString = Integer.toString(obj.statusCode);
                String mycode = Integer.toString(this.statusCode);
                if (codeString.charAt(0) != mycode.charAt(0)) {
                    return false;
                }
            } else if (this.statusCode != obj.statusCode) {
                return false;
            }
        }
        if (obj.reasonPhrase == null || this.reasonPhrase == obj.reasonPhrase) {
            return true;
        }
        return this.reasonPhrase.equals(obj.reasonPhrase);
    }

    public void setMatchStatusClass(boolean flag) {
        this.matchStatusClass = flag;
    }

    @Override
    public String encode() {
        String encoding = "SIP/2.0 " + this.statusCode;
        if (this.reasonPhrase != null) {
            encoding = encoding + Separators.SP + this.reasonPhrase;
        }
        return encoding + Separators.NEWLINE;
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
    public void setSipVersion(String s) {
        this.sipVersion = s;
    }

    @Override
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public void setReasonPhrase(String reasonPhrase) {
        this.reasonPhrase = reasonPhrase;
    }

    @Override
    public String getVersionMajor() {
        if (this.sipVersion == null) {
            return null;
        }
        String major = null;
        boolean slash = false;
        for (int i = 0; i < this.sipVersion.length(); i++) {
            if (this.sipVersion.charAt(i) == '.') {
                slash = false;
            }
            if (slash) {
                if (major == null) {
                    major = "" + this.sipVersion.charAt(i);
                } else {
                    major = major + this.sipVersion.charAt(i);
                }
            }
            if (this.sipVersion.charAt(i) == '/') {
                slash = true;
            }
        }
        return major;
    }

    @Override
    public String getVersionMinor() {
        if (this.sipVersion == null) {
            return null;
        }
        String minor = null;
        boolean dot = false;
        for (int i = 0; i < this.sipVersion.length(); i++) {
            if (dot) {
                if (minor == null) {
                    minor = "" + this.sipVersion.charAt(i);
                } else {
                    minor = minor + this.sipVersion.charAt(i);
                }
            }
            if (this.sipVersion.charAt(i) == '.') {
                dot = true;
            }
        }
        return minor;
    }
}
