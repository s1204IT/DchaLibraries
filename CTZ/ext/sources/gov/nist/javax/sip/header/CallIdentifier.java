package gov.nist.javax.sip.header;

import gov.nist.core.Separators;

public final class CallIdentifier extends SIPObject {
    private static final long serialVersionUID = 7314773655675451377L;
    protected String host;
    protected String localId;

    public CallIdentifier() {
    }

    public CallIdentifier(String str, String str2) {
        this.localId = str;
        this.host = str2;
    }

    public CallIdentifier(String str) throws IllegalArgumentException {
        setCallID(str);
    }

    @Override
    public String encode() {
        return encode(new StringBuffer()).toString();
    }

    @Override
    public StringBuffer encode(StringBuffer stringBuffer) {
        stringBuffer.append(this.localId);
        if (this.host != null) {
            stringBuffer.append(Separators.AT);
            stringBuffer.append(this.host);
        }
        return stringBuffer;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !obj.getClass().equals(getClass())) {
            return false;
        }
        CallIdentifier callIdentifier = (CallIdentifier) obj;
        if (this.localId.compareTo(callIdentifier.localId) != 0) {
            return false;
        }
        if (this.host == callIdentifier.host) {
            return true;
        }
        return (this.host != null || callIdentifier.host == null) && (this.host == null || callIdentifier.host != null) && this.host.compareToIgnoreCase(callIdentifier.host) == 0;
    }

    public int hashCode() {
        if (this.localId == null) {
            throw new UnsupportedOperationException("Hash code called before id is set");
        }
        return this.localId.hashCode();
    }

    public String getLocalId() {
        return this.localId;
    }

    public String getHost() {
        return this.host;
    }

    public void setLocalId(String str) {
        this.localId = str;
    }

    public void setCallID(String str) throws IllegalArgumentException {
        if (str == null) {
            throw new IllegalArgumentException("NULL!");
        }
        int iIndexOf = str.indexOf(64);
        if (iIndexOf == -1) {
            this.localId = str;
            this.host = null;
            return;
        }
        this.localId = str.substring(0, iIndexOf);
        this.host = str.substring(iIndexOf + 1, str.length());
        if (this.localId == null || this.host == null) {
            throw new IllegalArgumentException("CallID  must be token@token or token");
        }
    }

    public void setHost(String str) {
        this.host = str;
    }
}
