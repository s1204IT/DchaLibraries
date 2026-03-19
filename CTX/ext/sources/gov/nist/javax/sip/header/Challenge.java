package gov.nist.javax.sip.header;

import gov.nist.core.NameValue;
import gov.nist.core.NameValueList;
import gov.nist.core.Separators;

public class Challenge extends SIPObject {
    private static final long serialVersionUID = 5944455875924336L;
    protected NameValueList authParams = new NameValueList();
    protected String scheme;
    private static String DOMAIN = "domain";
    private static String REALM = "realm";
    private static String OPAQUE = "opaque";
    private static String ALGORITHM = "algorithm";
    private static String QOP = "qop";
    private static String STALE = "stale";
    private static String SIGNATURE = "signature";
    private static String RESPONSE = "response";
    private static String SIGNED_BY = "signed-by";
    private static String URI = "uri";

    public Challenge() {
        this.authParams.setSeparator(Separators.COMMA);
    }

    @Override
    public String encode() {
        StringBuffer stringBuffer = new StringBuffer(this.scheme);
        stringBuffer.append(Separators.SP);
        stringBuffer.append(this.authParams.encode());
        return stringBuffer.toString();
    }

    public String getScheme() {
        return this.scheme;
    }

    public NameValueList getAuthParams() {
        return this.authParams;
    }

    public String getDomain() {
        return (String) this.authParams.getValue(DOMAIN);
    }

    public String getURI() {
        return (String) this.authParams.getValue(URI);
    }

    public String getOpaque() {
        return (String) this.authParams.getValue(OPAQUE);
    }

    public String getQOP() {
        return (String) this.authParams.getValue(QOP);
    }

    public String getAlgorithm() {
        return (String) this.authParams.getValue(ALGORITHM);
    }

    public String getStale() {
        return (String) this.authParams.getValue(STALE);
    }

    public String getSignature() {
        return (String) this.authParams.getValue(SIGNATURE);
    }

    public String getSignedBy() {
        return (String) this.authParams.getValue(SIGNED_BY);
    }

    public String getResponse() {
        return (String) this.authParams.getValue(RESPONSE);
    }

    public String getRealm() {
        return (String) this.authParams.getValue(REALM);
    }

    public String getParameter(String str) {
        return (String) this.authParams.getValue(str);
    }

    public boolean hasParameter(String str) {
        return this.authParams.getNameValue(str) != null;
    }

    public boolean hasParameters() {
        return this.authParams.size() != 0;
    }

    public boolean removeParameter(String str) {
        return this.authParams.delete(str);
    }

    public void removeParameters() {
        this.authParams = new NameValueList();
    }

    public void setParameter(NameValue nameValue) {
        this.authParams.set(nameValue);
    }

    public void setScheme(String str) {
        this.scheme = str;
    }

    public void setAuthParams(NameValueList nameValueList) {
        this.authParams = nameValueList;
    }

    @Override
    public Object clone() {
        Challenge challenge = (Challenge) super.clone();
        if (this.authParams != null) {
            challenge.authParams = (NameValueList) this.authParams.clone();
        }
        return challenge;
    }
}
