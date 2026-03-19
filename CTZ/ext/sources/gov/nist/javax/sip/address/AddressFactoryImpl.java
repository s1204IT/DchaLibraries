package gov.nist.javax.sip.address;

import gov.nist.core.Separators;
import gov.nist.javax.sip.parser.StringMsgParser;
import gov.nist.javax.sip.parser.URLParser;
import java.text.ParseException;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.address.TelURL;
import javax.sip.address.URI;

public class AddressFactoryImpl implements AddressFactory {
    @Override
    public Address createAddress() {
        return new AddressImpl();
    }

    @Override
    public Address createAddress(String str, URI uri) {
        if (uri == null) {
            throw new NullPointerException("null  URI");
        }
        AddressImpl addressImpl = new AddressImpl();
        if (str != null) {
            addressImpl.setDisplayName(str);
        }
        addressImpl.setURI(uri);
        return addressImpl;
    }

    @Override
    public SipURI createSipURI(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("null URI");
        }
        try {
            return new StringMsgParser().parseSIPUrl(str);
        } catch (ParseException e) {
            throw new ParseException(e.getMessage(), 0);
        }
    }

    @Override
    public SipURI createSipURI(String str, String str2) throws ParseException {
        if (str2 == null) {
            throw new NullPointerException("null host");
        }
        StringBuffer stringBuffer = new StringBuffer("sip:");
        if (str != null) {
            stringBuffer.append(str);
            stringBuffer.append(Separators.AT);
        }
        if (str2.indexOf(58) != str2.lastIndexOf(58) && str2.trim().charAt(0) != '[') {
            str2 = '[' + str2 + ']';
        }
        stringBuffer.append(str2);
        try {
            return new StringMsgParser().parseSIPUrl(stringBuffer.toString());
        } catch (ParseException e) {
            throw new ParseException(e.getMessage(), 0);
        }
    }

    @Override
    public TelURL createTelURL(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("null url");
        }
        try {
            return (TelURLImpl) new StringMsgParser().parseUrl("tel:" + str);
        } catch (ParseException e) {
            throw new ParseException(e.getMessage(), 0);
        }
    }

    @Override
    public Address createAddress(URI uri) {
        if (uri == null) {
            throw new NullPointerException("null address");
        }
        AddressImpl addressImpl = new AddressImpl();
        addressImpl.setURI(uri);
        return addressImpl;
    }

    @Override
    public Address createAddress(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("null address");
        }
        if (str.equals(Separators.STAR)) {
            AddressImpl addressImpl = new AddressImpl();
            addressImpl.setAddressType(3);
            SipUri sipUri = new SipUri();
            sipUri.setUser(Separators.STAR);
            addressImpl.setURI(sipUri);
            return addressImpl;
        }
        return new StringMsgParser().parseAddress(str);
    }

    @Override
    public URI createURI(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("null arg");
        }
        try {
            URLParser uRLParser = new URLParser(str);
            String strPeekScheme = uRLParser.peekScheme();
            if (strPeekScheme == null) {
                throw new ParseException("bad scheme", 0);
            }
            if (strPeekScheme.equalsIgnoreCase("sip")) {
                return uRLParser.sipURL(true);
            }
            if (strPeekScheme.equalsIgnoreCase("sips")) {
                return uRLParser.sipURL(true);
            }
            if (strPeekScheme.equalsIgnoreCase("tel")) {
                return uRLParser.telURL(true);
            }
            return new GenericURI(str);
        } catch (ParseException e) {
            throw new ParseException(e.getMessage(), 0);
        }
    }
}
