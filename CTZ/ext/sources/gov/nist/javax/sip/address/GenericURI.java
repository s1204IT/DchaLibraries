package gov.nist.javax.sip.address;

import gov.nist.core.Separators;
import java.text.ParseException;
import javax.sip.address.URI;

public class GenericURI extends NetObject implements URI {
    public static final String ISUB = "isub";
    public static final String PHONE_CONTEXT_TAG = "context-tag";
    public static final String POSTDIAL = "postdial";
    public static final String PROVIDER_TAG = "provider-tag";
    public static final String SIP = "sip";
    public static final String SIPS = "sips";
    public static final String TEL = "tel";
    private static final long serialVersionUID = 3237685256878068790L;
    protected String scheme;
    protected String uriString;

    protected GenericURI() {
    }

    public GenericURI(String str) throws ParseException {
        try {
            this.uriString = str;
            this.scheme = str.substring(0, str.indexOf(Separators.COLON));
        } catch (Exception e) {
            throw new ParseException("GenericURI, Bad URI format", 0);
        }
    }

    @Override
    public String encode() {
        return this.uriString;
    }

    @Override
    public StringBuffer encode(StringBuffer stringBuffer) {
        stringBuffer.append(this.uriString);
        return stringBuffer;
    }

    @Override
    public String toString() {
        return encode();
    }

    @Override
    public String getScheme() {
        return this.scheme;
    }

    @Override
    public boolean isSipURI() {
        return this instanceof SipUri;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof URI) {
            return toString().equalsIgnoreCase(((URI) obj).toString());
        }
        return false;
    }

    public int hashCode() {
        return toString().hashCode();
    }
}
