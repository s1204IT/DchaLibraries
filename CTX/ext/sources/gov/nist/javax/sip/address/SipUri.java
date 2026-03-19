package gov.nist.javax.sip.address;

import gov.nist.core.Debug;
import gov.nist.core.GenericObject;
import gov.nist.core.Host;
import gov.nist.core.HostPort;
import gov.nist.core.NameValue;
import gov.nist.core.NameValueList;
import gov.nist.core.Separators;
import java.text.ParseException;
import java.util.Iterator;
import javax.sip.ListeningPoint;
import javax.sip.PeerUnavailableException;
import javax.sip.SipFactory;
import javax.sip.address.SipURI;
import javax.sip.header.HeaderFactory;
import org.ccil.cowan.tagsoup.XMLWriter;

public class SipUri extends GenericURI implements SipURI, SipURIExt {
    private static final long serialVersionUID = 7749781076218987044L;
    protected Authority authority;
    protected NameValueList qheaders;
    protected TelephoneNumber telephoneSubscriber;
    protected NameValueList uriParms;

    public SipUri() {
        this.scheme = "sip";
        this.uriParms = new NameValueList();
        this.qheaders = new NameValueList();
        this.qheaders.setSeparator(Separators.AND);
    }

    public void setScheme(String str) {
        if (str.compareToIgnoreCase("sip") != 0 && str.compareToIgnoreCase("sips") != 0) {
            throw new IllegalArgumentException("bad scheme " + str);
        }
        this.scheme = str.toLowerCase();
    }

    @Override
    public String getScheme() {
        return this.scheme;
    }

    public void clearUriParms() {
        this.uriParms = new NameValueList();
    }

    public void clearPassword() {
        UserInfo userInfo;
        if (this.authority != null && (userInfo = this.authority.getUserInfo()) != null) {
            userInfo.clearPassword();
        }
    }

    public Authority getAuthority() {
        return this.authority;
    }

    public void clearQheaders() {
        this.qheaders = new NameValueList();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof SipURI)) {
            return false;
        }
        SipURI sipURI = (SipURI) obj;
        if (isSecure() ^ sipURI.isSecure()) {
            return false;
        }
        if ((getUser() == null) ^ (sipURI.getUser() == null)) {
            return false;
        }
        if ((getUserPassword() == null) ^ (sipURI.getUserPassword() == null)) {
            return false;
        }
        if (getUser() != null && !RFC2396UrlDecoder.decode(getUser()).equals(RFC2396UrlDecoder.decode(sipURI.getUser()))) {
            return false;
        }
        if (getUserPassword() != null && !RFC2396UrlDecoder.decode(getUserPassword()).equals(RFC2396UrlDecoder.decode(sipURI.getUserPassword()))) {
            return false;
        }
        if ((getHost() == null) ^ (sipURI.getHost() == null)) {
            return false;
        }
        if ((getHost() != null && !getHost().equalsIgnoreCase(sipURI.getHost())) || getPort() != sipURI.getPort()) {
            return false;
        }
        Iterator parameterNames = getParameterNames();
        while (parameterNames.hasNext()) {
            String str = (String) parameterNames.next();
            String parameter = getParameter(str);
            String parameter2 = sipURI.getParameter(str);
            if (parameter != null && parameter2 != null && !RFC2396UrlDecoder.decode(parameter).equalsIgnoreCase(RFC2396UrlDecoder.decode(parameter2))) {
                return false;
            }
        }
        if ((getTransportParam() == null) ^ (sipURI.getTransportParam() == null)) {
            return false;
        }
        if ((getUserParam() == null) ^ (sipURI.getUserParam() == null)) {
            return false;
        }
        if ((getTTLParam() == -1) ^ (sipURI.getTTLParam() == -1)) {
            return false;
        }
        if ((getMethodParam() == null) ^ (sipURI.getMethodParam() == null)) {
            return false;
        }
        if ((getMAddrParam() == null) ^ (sipURI.getMAddrParam() == null)) {
            return false;
        }
        if (getHeaderNames().hasNext() && !sipURI.getHeaderNames().hasNext()) {
            return false;
        }
        if (!getHeaderNames().hasNext() && sipURI.getHeaderNames().hasNext()) {
            return false;
        }
        if (getHeaderNames().hasNext() && sipURI.getHeaderNames().hasNext()) {
            try {
                HeaderFactory headerFactoryCreateHeaderFactory = SipFactory.getInstance().createHeaderFactory();
                Iterator headerNames = getHeaderNames();
                while (headerNames.hasNext()) {
                    String str2 = (String) headerNames.next();
                    String header = getHeader(str2);
                    String header2 = sipURI.getHeader(str2);
                    if (header == null && header2 != null) {
                        return false;
                    }
                    if (header2 == null && header != null) {
                        return false;
                    }
                    if (header != null || header2 != null) {
                        try {
                            if (!headerFactoryCreateHeaderFactory.createHeader(str2, RFC2396UrlDecoder.decode(header)).equals(headerFactoryCreateHeaderFactory.createHeader(str2, RFC2396UrlDecoder.decode(header2)))) {
                                return false;
                            }
                        } catch (ParseException e) {
                            Debug.logError("Cannot parse one of the header of the sip uris to compare " + this + Separators.SP + sipURI, e);
                            return false;
                        }
                    }
                }
            } catch (PeerUnavailableException e2) {
                Debug.logError("Cannot get the header factory to parse the header of the sip uris to compare", e2);
                return false;
            }
        }
        return true;
    }

    @Override
    public String encode() {
        return encode(new StringBuffer()).toString();
    }

    @Override
    public StringBuffer encode(StringBuffer stringBuffer) {
        stringBuffer.append(this.scheme);
        stringBuffer.append(Separators.COLON);
        if (this.authority != null) {
            this.authority.encode(stringBuffer);
        }
        if (!this.uriParms.isEmpty()) {
            stringBuffer.append(Separators.SEMICOLON);
            this.uriParms.encode(stringBuffer);
        }
        if (!this.qheaders.isEmpty()) {
            stringBuffer.append(Separators.QUESTION);
            this.qheaders.encode(stringBuffer);
        }
        return stringBuffer;
    }

    @Override
    public String toString() {
        return encode();
    }

    @Override
    public String getUserAtHost() {
        StringBuffer stringBuffer;
        String user = "";
        if (this.authority.getUserInfo() != null) {
            user = this.authority.getUserInfo().getUser();
        }
        String strEncode = this.authority.getHost().encode();
        if (user.equals("")) {
            stringBuffer = new StringBuffer();
        } else {
            StringBuffer stringBuffer2 = new StringBuffer(user);
            stringBuffer2.append(Separators.AT);
            stringBuffer = stringBuffer2;
        }
        stringBuffer.append(strEncode);
        return stringBuffer.toString();
    }

    @Override
    public String getUserAtHostPort() {
        StringBuffer stringBuffer;
        String user = "";
        if (this.authority.getUserInfo() != null) {
            user = this.authority.getUserInfo().getUser();
        }
        String strEncode = this.authority.getHost().encode();
        int port = this.authority.getPort();
        if (user.equals("")) {
            stringBuffer = new StringBuffer();
        } else {
            StringBuffer stringBuffer2 = new StringBuffer(user);
            stringBuffer2.append(Separators.AT);
            stringBuffer = stringBuffer2;
        }
        if (port != -1) {
            stringBuffer.append(strEncode);
            stringBuffer.append(Separators.COLON);
            stringBuffer.append(port);
            return stringBuffer.toString();
        }
        stringBuffer.append(strEncode);
        return stringBuffer.toString();
    }

    public Object getParm(String str) {
        return this.uriParms.getValue(str);
    }

    public String getMethod() {
        return (String) getParm(XMLWriter.METHOD);
    }

    public NameValueList getParameters() {
        return this.uriParms;
    }

    public void removeParameters() {
        this.uriParms = new NameValueList();
    }

    public NameValueList getQheaders() {
        return this.qheaders;
    }

    @Override
    public String getUserType() {
        return (String) this.uriParms.getValue("user");
    }

    @Override
    public String getUserPassword() {
        if (this.authority == null) {
            return null;
        }
        return this.authority.getPassword();
    }

    @Override
    public void setUserPassword(String str) {
        if (this.authority == null) {
            this.authority = new Authority();
        }
        this.authority.setPassword(str);
    }

    public TelephoneNumber getTelephoneSubscriber() {
        if (this.telephoneSubscriber == null) {
            this.telephoneSubscriber = new TelephoneNumber();
        }
        return this.telephoneSubscriber;
    }

    public HostPort getHostPort() {
        if (this.authority == null || this.authority.getHost() == null) {
            return null;
        }
        return this.authority.getHostPort();
    }

    @Override
    public int getPort() {
        HostPort hostPort = getHostPort();
        if (hostPort == null) {
            return -1;
        }
        return hostPort.getPort();
    }

    @Override
    public String getHost() {
        if (this.authority == null || this.authority.getHost() == null) {
            return null;
        }
        return this.authority.getHost().encode();
    }

    public boolean isUserTelephoneSubscriber() {
        String str = (String) this.uriParms.getValue("user");
        if (str == null) {
            return false;
        }
        return str.equalsIgnoreCase("phone");
    }

    public void removeTTL() {
        if (this.uriParms != null) {
            this.uriParms.delete("ttl");
        }
    }

    public void removeMAddr() {
        if (this.uriParms != null) {
            this.uriParms.delete("maddr");
        }
    }

    public void removeTransport() {
        if (this.uriParms != null) {
            this.uriParms.delete(gov.nist.javax.sip.header.ParameterNames.TRANSPORT);
        }
    }

    @Override
    public void removeHeader(String str) {
        if (this.qheaders != null) {
            this.qheaders.delete(str);
        }
    }

    @Override
    public void removeHeaders() {
        this.qheaders = new NameValueList();
    }

    @Override
    public void removeUserType() {
        if (this.uriParms != null) {
            this.uriParms.delete("user");
        }
    }

    public void removePort() {
        this.authority.removePort();
    }

    public void removeMethod() {
        if (this.uriParms != null) {
            this.uriParms.delete(XMLWriter.METHOD);
        }
    }

    @Override
    public void setUser(String str) {
        if (this.authority == null) {
            this.authority = new Authority();
        }
        this.authority.setUser(str);
    }

    public void removeUser() {
        this.authority.removeUserInfo();
    }

    public void setDefaultParm(String str, Object obj) {
        if (this.uriParms.getValue(str) == null) {
            this.uriParms.set(new NameValue(str, obj));
        }
    }

    public void setAuthority(Authority authority) {
        this.authority = authority;
    }

    public void setHost(Host host) {
        if (this.authority == null) {
            this.authority = new Authority();
        }
        this.authority.setHost(host);
    }

    public void setUriParms(NameValueList nameValueList) {
        this.uriParms = nameValueList;
    }

    public void setUriParm(String str, Object obj) {
        this.uriParms.set(new NameValue(str, obj));
    }

    public void setQheaders(NameValueList nameValueList) {
        this.qheaders = nameValueList;
    }

    public void setMAddr(String str) {
        NameValue nameValue = this.uriParms.getNameValue("maddr");
        Host host = new Host();
        host.setAddress(str);
        if (nameValue != null) {
            nameValue.setValueAsObject(host);
        } else {
            this.uriParms.set(new NameValue("maddr", host));
        }
    }

    @Override
    public void setUserParam(String str) {
        this.uriParms.set("user", str);
    }

    public void setMethod(String str) {
        this.uriParms.set(XMLWriter.METHOD, str);
    }

    public void setIsdnSubAddress(String str) {
        if (this.telephoneSubscriber == null) {
            this.telephoneSubscriber = new TelephoneNumber();
        }
        this.telephoneSubscriber.setIsdnSubaddress(str);
    }

    public void setTelephoneSubscriber(TelephoneNumber telephoneNumber) {
        this.telephoneSubscriber = telephoneNumber;
    }

    @Override
    public void setPort(int i) {
        if (this.authority == null) {
            this.authority = new Authority();
        }
        this.authority.setPort(i);
    }

    public boolean hasParameter(String str) {
        return this.uriParms.getValue(str) != null;
    }

    public void setQHeader(NameValue nameValue) {
        this.qheaders.set(nameValue);
    }

    public void setUriParameter(NameValue nameValue) {
        this.uriParms.set(nameValue);
    }

    @Override
    public boolean hasTransport() {
        return hasParameter(gov.nist.javax.sip.header.ParameterNames.TRANSPORT);
    }

    @Override
    public void removeParameter(String str) {
        this.uriParms.delete(str);
    }

    public void setHostPort(HostPort hostPort) {
        if (this.authority == null) {
            this.authority = new Authority();
        }
        this.authority.setHostPort(hostPort);
    }

    @Override
    public Object clone() {
        SipUri sipUri = (SipUri) super.clone();
        if (this.authority != null) {
            sipUri.authority = (Authority) this.authority.clone();
        }
        if (this.uriParms != null) {
            sipUri.uriParms = (NameValueList) this.uriParms.clone();
        }
        if (this.qheaders != null) {
            sipUri.qheaders = (NameValueList) this.qheaders.clone();
        }
        if (this.telephoneSubscriber != null) {
            sipUri.telephoneSubscriber = (TelephoneNumber) this.telephoneSubscriber.clone();
        }
        return sipUri;
    }

    @Override
    public String getHeader(String str) {
        if (this.qheaders.getValue(str) != null) {
            return this.qheaders.getValue(str).toString();
        }
        return null;
    }

    @Override
    public Iterator<String> getHeaderNames() {
        return this.qheaders.getNames();
    }

    @Override
    public String getLrParam() {
        if (hasParameter("lr")) {
            return "true";
        }
        return null;
    }

    @Override
    public String getMAddrParam() {
        NameValue nameValue = this.uriParms.getNameValue("maddr");
        if (nameValue == null) {
            return null;
        }
        return (String) nameValue.getValueAsObject();
    }

    @Override
    public String getMethodParam() {
        return getParameter(XMLWriter.METHOD);
    }

    @Override
    public String getParameter(String str) {
        Object value = this.uriParms.getValue(str);
        if (value == null) {
            return null;
        }
        if (value instanceof GenericObject) {
            return ((GenericObject) value).encode();
        }
        return value.toString();
    }

    @Override
    public Iterator<String> getParameterNames() {
        return this.uriParms.getNames();
    }

    @Override
    public int getTTLParam() {
        Integer num = (Integer) this.uriParms.getValue("ttl");
        if (num != null) {
            return num.intValue();
        }
        return -1;
    }

    @Override
    public String getTransportParam() {
        if (this.uriParms != null) {
            return (String) this.uriParms.getValue(gov.nist.javax.sip.header.ParameterNames.TRANSPORT);
        }
        return null;
    }

    @Override
    public String getUser() {
        return this.authority.getUser();
    }

    @Override
    public boolean isSecure() {
        return getScheme().equalsIgnoreCase("sips");
    }

    @Override
    public boolean isSipURI() {
        return true;
    }

    @Override
    public void setHeader(String str, String str2) {
        this.qheaders.set(new NameValue(str, str2));
    }

    @Override
    public void setHost(String str) throws ParseException {
        setHost(new Host(str));
    }

    @Override
    public void setLrParam() {
        this.uriParms.set("lr", null);
    }

    @Override
    public void setMAddrParam(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("bad maddr");
        }
        setParameter("maddr", str);
    }

    @Override
    public void setMethodParam(String str) throws ParseException {
        setParameter(XMLWriter.METHOD, str);
    }

    @Override
    public void setParameter(String str, String str2) throws ParseException {
        if (str.equalsIgnoreCase("ttl")) {
            try {
                Integer.parseInt(str2);
            } catch (NumberFormatException e) {
                throw new ParseException("bad parameter " + str2, 0);
            }
        }
        this.uriParms.set(str, str2);
    }

    @Override
    public void setSecure(boolean z) {
        if (z) {
            this.scheme = "sips";
        } else {
            this.scheme = "sip";
        }
    }

    @Override
    public void setTTLParam(int i) {
        if (i <= 0) {
            throw new IllegalArgumentException("Bad ttl value");
        }
        if (this.uriParms != null) {
            this.uriParms.set(new NameValue("ttl", Integer.valueOf(i)));
        }
    }

    @Override
    public void setTransportParam(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("null arg");
        }
        if (str.compareToIgnoreCase(ListeningPoint.UDP) == 0 || str.compareToIgnoreCase(ListeningPoint.TLS) == 0 || str.compareToIgnoreCase(ListeningPoint.TCP) == 0 || str.compareToIgnoreCase(ListeningPoint.SCTP) == 0) {
            this.uriParms.set(new NameValue(gov.nist.javax.sip.header.ParameterNames.TRANSPORT, str.toLowerCase()));
        } else {
            throw new ParseException("bad transport " + str, 0);
        }
    }

    @Override
    public String getUserParam() {
        return getParameter("user");
    }

    @Override
    public boolean hasLrParam() {
        return this.uriParms.getNameValue("lr") != null;
    }

    @Override
    public boolean hasGrParam() {
        return this.uriParms.getNameValue("gr") != null;
    }

    @Override
    public void setGrParam(String str) {
        this.uriParms.set("gr", str);
    }

    public String getGrParam() {
        return (String) this.uriParms.getValue("gr");
    }
}
