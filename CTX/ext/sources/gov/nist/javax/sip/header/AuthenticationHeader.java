package gov.nist.javax.sip.header;

import gov.nist.core.NameValue;
import gov.nist.core.Separators;
import java.text.ParseException;
import javax.sip.address.URI;

public abstract class AuthenticationHeader extends ParametersHeader {
    public static final String ALGORITHM = "algorithm";
    public static final String CK = "ck";
    public static final String CNONCE = "cnonce";
    public static final String DOMAIN = "domain";
    public static final String IK = "ik";
    public static final String INTEGRITY_PROTECTED = "integrity-protected";
    public static final String NC = "nc";
    public static final String NONCE = "nonce";
    public static final String OPAQUE = "opaque";
    public static final String QOP = "qop";
    public static final String REALM = "realm";
    public static final String RESPONSE = "response";
    public static final String SIGNATURE = "signature";
    public static final String SIGNED_BY = "signed-by";
    public static final String STALE = "stale";
    public static final String URI = "uri";
    public static final String USERNAME = "username";
    protected String scheme;

    public AuthenticationHeader(String str) {
        super(str);
        this.parameters.setSeparator(Separators.COMMA);
        this.scheme = ParameterNames.DIGEST;
    }

    public AuthenticationHeader() {
        this.parameters.setSeparator(Separators.COMMA);
    }

    @Override
    public void setParameter(String str, String str2) throws ParseException {
        NameValue nameValue = this.parameters.getNameValue(str.toLowerCase());
        if (nameValue == null) {
            NameValue nameValue2 = new NameValue(str, str2);
            if (str.equalsIgnoreCase("qop") || str.equalsIgnoreCase("realm") || str.equalsIgnoreCase("cnonce") || str.equalsIgnoreCase("nonce") || str.equalsIgnoreCase("username") || str.equalsIgnoreCase("domain") || str.equalsIgnoreCase("opaque") || str.equalsIgnoreCase(ParameterNames.NEXT_NONCE) || str.equalsIgnoreCase("uri") || str.equalsIgnoreCase("response") || str.equalsIgnoreCase("ik") || str.equalsIgnoreCase("ck") || str.equalsIgnoreCase("integrity-protected")) {
                if ((!(this instanceof Authorization) && !(this instanceof ProxyAuthorization)) || !str.equalsIgnoreCase("qop")) {
                    nameValue2.setQuotedValue();
                }
                if (str2 == null) {
                    throw new NullPointerException("null value");
                }
                if (str2.startsWith(Separators.DOUBLE_QUOTE)) {
                    throw new ParseException(str2 + " : Unexpected DOUBLE_QUOTE", 0);
                }
            }
            super.setParameter(nameValue2);
            return;
        }
        nameValue.setValueAsObject(str2);
    }

    public void setChallenge(Challenge challenge) {
        this.scheme = challenge.scheme;
        this.parameters = challenge.authParams;
    }

    @Override
    public String encodeBody() {
        this.parameters.setSeparator(Separators.COMMA);
        return this.scheme + Separators.SP + this.parameters.encode();
    }

    public void setScheme(String str) {
        this.scheme = str;
    }

    public String getScheme() {
        return this.scheme;
    }

    public void setRealm(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception,  AuthenticationHeader, setRealm(), The realm parameter is null");
        }
        setParameter("realm", str);
    }

    public String getRealm() {
        return getParameter("realm");
    }

    public void setNonce(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception,  AuthenticationHeader, setNonce(), The nonce parameter is null");
        }
        setParameter("nonce", str);
    }

    public String getNonce() {
        return getParameter("nonce");
    }

    public void setURI(URI uri) {
        if (uri != null) {
            NameValue nameValue = new NameValue("uri", uri);
            nameValue.setQuotedValue();
            this.parameters.set(nameValue);
            return;
        }
        throw new NullPointerException("Null URI");
    }

    public URI getURI() {
        return getParameterAsURI("uri");
    }

    public void setAlgorithm(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("null arg");
        }
        setParameter("algorithm", str);
    }

    public String getAlgorithm() {
        return getParameter("algorithm");
    }

    public void setQop(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("null arg");
        }
        setParameter("qop", str);
    }

    public String getQop() {
        return getParameter("qop");
    }

    public void setOpaque(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("null arg");
        }
        setParameter("opaque", str);
    }

    public String getOpaque() {
        return getParameter("opaque");
    }

    public void setDomain(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("null arg");
        }
        setParameter("domain", str);
    }

    public String getDomain() {
        return getParameter("domain");
    }

    public void setStale(boolean z) {
        setParameter(new NameValue("stale", Boolean.valueOf(z)));
    }

    public boolean isStale() {
        return getParameterAsBoolean("stale");
    }

    public void setCNonce(String str) throws ParseException {
        setParameter("cnonce", str);
    }

    public String getCNonce() {
        return getParameter("cnonce");
    }

    public int getNonceCount() {
        return getParameterAsHexInt("nc");
    }

    public void setNonceCount(int i) throws ParseException {
        if (i < 0) {
            throw new ParseException("bad value", 0);
        }
        String hexString = Integer.toHexString(i);
        setParameter("nc", "00000000".substring(0, 8 - hexString.length()) + hexString);
    }

    public String getResponse() {
        return (String) getParameterValue("response");
    }

    public void setResponse(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("Null parameter");
        }
        setParameter("response", str);
    }

    public String getUsername() {
        return getParameter("username");
    }

    public void setUsername(String str) throws ParseException {
        setParameter("username", str);
    }

    public void setIK(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception,  AuthenticationHeader, setIk(), The auth-param IK parameter is null");
        }
        setParameter("ik", str);
    }

    public String getIK() {
        return getParameter("ik");
    }

    public void setCK(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception,  AuthenticationHeader, setCk(), The auth-param CK parameter is null");
        }
        setParameter("ck", str);
    }

    public String getCK() {
        return getParameter("ck");
    }

    public void setIntegrityProtected(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception,  AuthenticationHeader, setIntegrityProtected(), The integrity-protected parameter is null");
        }
        setParameter("integrity-protected", str);
    }

    public String getIntegrityProtected() {
        return getParameter("integrity-protected");
    }
}
