package gov.nist.javax.sip.header;

import gov.nist.core.NameValue;
import gov.nist.core.Separators;
import java.text.ParseException;
import javax.sip.header.AuthenticationInfoHeader;

public final class AuthenticationInfo extends ParametersHeader implements AuthenticationInfoHeader {
    private static final long serialVersionUID = -4371927900917127057L;

    public AuthenticationInfo() {
        super("Authentication-Info");
        this.parameters.setSeparator(Separators.COMMA);
    }

    public void add(NameValue nameValue) {
        this.parameters.set(nameValue);
    }

    @Override
    protected String encodeBody() {
        return this.parameters.encode();
    }

    public NameValue getAuthInfo(String str) {
        return this.parameters.getNameValue(str);
    }

    public String getAuthenticationInfo() {
        return encodeBody();
    }

    @Override
    public String getCNonce() {
        return getParameter("cnonce");
    }

    @Override
    public String getNextNonce() {
        return getParameter(ParameterNames.NEXT_NONCE);
    }

    @Override
    public int getNonceCount() {
        return getParameterAsInt("nc");
    }

    @Override
    public String getQop() {
        return getParameter("qop");
    }

    @Override
    public String getResponse() {
        return getParameter(ParameterNames.RESPONSE_AUTH);
    }

    @Override
    public void setCNonce(String str) throws ParseException {
        setParameter("cnonce", str);
    }

    @Override
    public void setNextNonce(String str) throws ParseException {
        setParameter(ParameterNames.NEXT_NONCE, str);
    }

    @Override
    public void setNonceCount(int i) throws ParseException {
        if (i < 0) {
            throw new ParseException("bad value", 0);
        }
        String hexString = Integer.toHexString(i);
        setParameter("nc", "00000000".substring(0, 8 - hexString.length()) + hexString);
    }

    @Override
    public void setQop(String str) throws ParseException {
        setParameter("qop", str);
    }

    @Override
    public void setResponse(String str) throws ParseException {
        setParameter(ParameterNames.RESPONSE_AUTH, str);
    }

    @Override
    public void setParameter(String str, String str2) throws ParseException {
        if (str == null) {
            throw new NullPointerException("null name");
        }
        NameValue nameValue = this.parameters.getNameValue(str.toLowerCase());
        if (nameValue == null) {
            NameValue nameValue2 = new NameValue(str, str2);
            if (str.equalsIgnoreCase("qop") || str.equalsIgnoreCase(ParameterNames.NEXT_NONCE) || str.equalsIgnoreCase("realm") || str.equalsIgnoreCase("cnonce") || str.equalsIgnoreCase("nonce") || str.equalsIgnoreCase("opaque") || str.equalsIgnoreCase("username") || str.equalsIgnoreCase("domain") || str.equalsIgnoreCase(ParameterNames.NEXT_NONCE) || str.equalsIgnoreCase(ParameterNames.RESPONSE_AUTH)) {
                if (str2 == null) {
                    throw new NullPointerException("null value");
                }
                if (str2.startsWith(Separators.DOUBLE_QUOTE)) {
                    throw new ParseException(str2 + " : Unexpected DOUBLE_QUOTE", 0);
                }
                nameValue2.setQuotedValue();
            }
            super.setParameter(nameValue2);
            return;
        }
        nameValue.setValueAsObject(str2);
    }
}
