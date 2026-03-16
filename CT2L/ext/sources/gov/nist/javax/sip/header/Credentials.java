package gov.nist.javax.sip.header;

import gov.nist.core.NameValueList;
import gov.nist.core.Separators;

public class Credentials extends SIPObject {
    private static final long serialVersionUID = -6335592791505451524L;
    protected NameValueList parameters = new NameValueList();
    protected String scheme;
    private static String DOMAIN = "domain";
    private static String REALM = "realm";
    private static String OPAQUE = "opaque";
    private static String RESPONSE = "response";
    private static String URI = "uri";
    private static String NONCE = "nonce";
    private static String CNONCE = "cnonce";
    private static String USERNAME = "username";

    public Credentials() {
        this.parameters.setSeparator(Separators.COMMA);
    }

    public NameValueList getCredentials() {
        return this.parameters;
    }

    public String getScheme() {
        return this.scheme;
    }

    public void setScheme(String s) {
        this.scheme = s;
    }

    public void setCredentials(NameValueList c) {
        this.parameters = c;
    }

    @Override
    public String encode() {
        String retval = this.scheme;
        if (!this.parameters.isEmpty()) {
            return retval + Separators.SP + this.parameters.encode();
        }
        return retval;
    }

    @Override
    public Object clone() {
        Credentials retval = (Credentials) super.clone();
        if (this.parameters != null) {
            retval.parameters = (NameValueList) this.parameters.clone();
        }
        return retval;
    }
}
