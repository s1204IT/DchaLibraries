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
    public Address createAddress(String displayName, URI uri) {
        if (uri == null) {
            throw new NullPointerException("null  URI");
        }
        AddressImpl addressImpl = new AddressImpl();
        if (displayName != null) {
            addressImpl.setDisplayName(displayName);
        }
        addressImpl.setURI(uri);
        return addressImpl;
    }

    @Override
    public SipURI createSipURI(String uri) throws ParseException {
        if (uri == null) {
            throw new NullPointerException("null URI");
        }
        try {
            StringMsgParser smp = new StringMsgParser();
            SipUri sipUri = smp.parseSIPUrl(uri);
            return sipUri;
        } catch (ParseException ex) {
            throw new ParseException(ex.getMessage(), 0);
        }
    }

    @Override
    public SipURI createSipURI(String user, String host) throws ParseException {
        if (host == null) {
            throw new NullPointerException("null host");
        }
        StringBuffer uriString = new StringBuffer("sip:");
        if (user != null) {
            uriString.append(user);
            uriString.append(Separators.AT);
        }
        if (host.indexOf(58) != host.lastIndexOf(58) && host.trim().charAt(0) != '[') {
            host = '[' + host + ']';
        }
        uriString.append(host);
        StringMsgParser smp = new StringMsgParser();
        try {
            SipUri sipUri = smp.parseSIPUrl(uriString.toString());
            return sipUri;
        } catch (ParseException ex) {
            throw new ParseException(ex.getMessage(), 0);
        }
    }

    @Override
    public TelURL createTelURL(String uri) throws ParseException {
        if (uri == null) {
            throw new NullPointerException("null url");
        }
        String telUrl = "tel:" + uri;
        try {
            StringMsgParser smp = new StringMsgParser();
            TelURLImpl timp = (TelURLImpl) smp.parseUrl(telUrl);
            return timp;
        } catch (ParseException ex) {
            throw new ParseException(ex.getMessage(), 0);
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
    public Address createAddress(String address) throws ParseException {
        if (address == null) {
            throw new NullPointerException("null address");
        }
        if (address.equals(Separators.STAR)) {
            AddressImpl addressImpl = new AddressImpl();
            addressImpl.setAddressType(3);
            SipURI uri = new SipUri();
            uri.setUser(Separators.STAR);
            addressImpl.setURI(uri);
            return addressImpl;
        }
        StringMsgParser smp = new StringMsgParser();
        return smp.parseAddress(address);
    }

    @Override
    public URI createURI(String uri) throws ParseException {
        if (uri == null) {
            throw new NullPointerException("null arg");
        }
        try {
            URLParser urlParser = new URLParser(uri);
            String scheme = urlParser.peekScheme();
            if (scheme == null) {
                throw new ParseException("bad scheme", 0);
            }
            if (scheme.equalsIgnoreCase("sip")) {
                return urlParser.sipURL(true);
            }
            if (scheme.equalsIgnoreCase("sips")) {
                return urlParser.sipURL(true);
            }
            if (scheme.equalsIgnoreCase("tel")) {
                return urlParser.telURL(true);
            }
            return new GenericURI(uri);
        } catch (ParseException ex) {
            throw new ParseException(ex.getMessage(), 0);
        }
    }
}
