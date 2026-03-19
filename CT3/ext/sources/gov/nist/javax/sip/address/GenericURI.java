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

    public GenericURI(String uriString) throws ParseException {
        try {
            this.uriString = uriString;
            int i = uriString.indexOf(Separators.COLON);
            this.scheme = uriString.substring(0, i);
        } catch (Exception e) {
            throw new ParseException("GenericURI, Bad URI format", 0);
        }
    }

    @Override
    public String encode() {
        return this.uriString;
    }

    @Override
    public StringBuffer encode(StringBuffer buffer) {
        return buffer.append(this.uriString);
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
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that instanceof URI) {
            URI o = (URI) that;
            return toString().equalsIgnoreCase(o.toString());
        }
        return false;
    }

    public int hashCode() {
        return toString().hashCode();
    }
}
