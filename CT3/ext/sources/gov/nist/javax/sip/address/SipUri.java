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
import javax.sip.header.Header;
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

    public void setScheme(String scheme) {
        if (scheme.compareToIgnoreCase("sip") != 0 && scheme.compareToIgnoreCase("sips") != 0) {
            throw new IllegalArgumentException("bad scheme " + scheme);
        }
        this.scheme = scheme.toLowerCase();
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
        if (this.authority == null || (userInfo = this.authority.getUserInfo()) == null) {
            return;
        }
        userInfo.clearPassword();
    }

    public Authority getAuthority() {
        return this.authority;
    }

    public void clearQheaders() {
        this.qheaders = new NameValueList();
    }

    @Override
    public boolean equals(Object that) {
        if (that == this) {
            return true;
        }
        if (that instanceof SipURI) {
            SipURI b = (SipURI) that;
            if (isSecure() ^ b.isSecure()) {
                return false;
            }
            if ((getUser() == null) ^ (b.getUser() == null)) {
                return false;
            }
            if ((getUserPassword() == null) ^ (b.getUserPassword() == null)) {
                return false;
            }
            if (getUser() != null && !RFC2396UrlDecoder.decode(getUser()).equals(RFC2396UrlDecoder.decode(b.getUser()))) {
                return false;
            }
            if (getUserPassword() != null && !RFC2396UrlDecoder.decode(getUserPassword()).equals(RFC2396UrlDecoder.decode(b.getUserPassword()))) {
                return false;
            }
            if ((getHost() == null) ^ (b.getHost() == null)) {
                return false;
            }
            if ((getHost() != null && !getHost().equalsIgnoreCase(b.getHost())) || getPort() != b.getPort()) {
                return false;
            }
            Iterator i = getParameterNames();
            while (i.hasNext()) {
                String pname = (String) i.next();
                String p1 = getParameter(pname);
                String p2 = b.getParameter(pname);
                if (p1 != null && p2 != null && !RFC2396UrlDecoder.decode(p1).equalsIgnoreCase(RFC2396UrlDecoder.decode(p2))) {
                    return false;
                }
            }
            if ((getTransportParam() == null) ^ (b.getTransportParam() == null)) {
                return false;
            }
            if ((getUserParam() == null) ^ (b.getUserParam() == null)) {
                return false;
            }
            if ((getTTLParam() == -1) ^ (b.getTTLParam() == -1)) {
                return false;
            }
            if ((getMethodParam() == null) ^ (b.getMethodParam() == null)) {
                return false;
            }
            if ((getMAddrParam() == null) ^ (b.getMAddrParam() == null)) {
                return false;
            }
            if (getHeaderNames().hasNext() && !b.getHeaderNames().hasNext()) {
                return false;
            }
            if (!getHeaderNames().hasNext() && b.getHeaderNames().hasNext()) {
                return false;
            }
            if (getHeaderNames().hasNext() && b.getHeaderNames().hasNext()) {
                try {
                    HeaderFactory headerFactory = SipFactory.getInstance().createHeaderFactory();
                    Iterator i2 = getHeaderNames();
                    while (i2.hasNext()) {
                        String hname = (String) i2.next();
                        String h1 = getHeader(hname);
                        String h2 = b.getHeader(hname);
                        if (h1 == null && h2 != null) {
                            return false;
                        }
                        if (h2 == null && h1 != null) {
                            return false;
                        }
                        if (h1 != null || h2 != null) {
                            try {
                                Header header1 = headerFactory.createHeader(hname, RFC2396UrlDecoder.decode(h1));
                                Header header2 = headerFactory.createHeader(hname, RFC2396UrlDecoder.decode(h2));
                                if (!header1.equals(header2)) {
                                    return false;
                                }
                            } catch (ParseException e) {
                                Debug.logError("Cannot parse one of the header of the sip uris to compare " + this + Separators.SP + b, e);
                                return false;
                            }
                        }
                    }
                    return true;
                } catch (PeerUnavailableException e2) {
                    Debug.logError("Cannot get the header factory to parse the header of the sip uris to compare", e2);
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public String encode() {
        return encode(new StringBuffer()).toString();
    }

    @Override
    public StringBuffer encode(StringBuffer buffer) {
        buffer.append(this.scheme).append(Separators.COLON);
        if (this.authority != null) {
            this.authority.encode(buffer);
        }
        if (!this.uriParms.isEmpty()) {
            buffer.append(Separators.SEMICOLON);
            this.uriParms.encode(buffer);
        }
        if (!this.qheaders.isEmpty()) {
            buffer.append(Separators.QUESTION);
            this.qheaders.encode(buffer);
        }
        return buffer;
    }

    @Override
    public String toString() {
        return encode();
    }

    @Override
    public String getUserAtHost() {
        StringBuffer s;
        String user = "";
        if (this.authority.getUserInfo() != null) {
            user = this.authority.getUserInfo().getUser();
        }
        String host = this.authority.getHost().encode();
        if (user.equals("")) {
            s = new StringBuffer();
        } else {
            s = new StringBuffer(user).append(Separators.AT);
        }
        return s.append(host).toString();
    }

    @Override
    public String getUserAtHostPort() {
        StringBuffer s;
        String user = "";
        if (this.authority.getUserInfo() != null) {
            user = this.authority.getUserInfo().getUser();
        }
        String host = this.authority.getHost().encode();
        int port = this.authority.getPort();
        if (user.equals("")) {
            s = new StringBuffer();
        } else {
            s = new StringBuffer(user).append(Separators.AT);
        }
        if (port != -1) {
            return s.append(host).append(Separators.COLON).append(port).toString();
        }
        return s.append(host).toString();
    }

    public Object getParm(String parmname) {
        Object obj = this.uriParms.getValue(parmname);
        return obj;
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
    public void setUserPassword(String password) {
        if (this.authority == null) {
            this.authority = new Authority();
        }
        this.authority.setPassword(password);
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
        HostPort hp = getHostPort();
        if (hp == null) {
            return -1;
        }
        return hp.getPort();
    }

    @Override
    public String getHost() {
        if (this.authority == null || this.authority.getHost() == null) {
            return null;
        }
        return this.authority.getHost().encode();
    }

    public boolean isUserTelephoneSubscriber() {
        String usrtype = (String) this.uriParms.getValue("user");
        if (usrtype == null) {
            return false;
        }
        return usrtype.equalsIgnoreCase("phone");
    }

    public void removeTTL() {
        if (this.uriParms == null) {
            return;
        }
        this.uriParms.delete("ttl");
    }

    public void removeMAddr() {
        if (this.uriParms == null) {
            return;
        }
        this.uriParms.delete("maddr");
    }

    public void removeTransport() {
        if (this.uriParms == null) {
            return;
        }
        this.uriParms.delete(gov.nist.javax.sip.header.ParameterNames.TRANSPORT);
    }

    @Override
    public void removeHeader(String name) {
        if (this.qheaders == null) {
            return;
        }
        this.qheaders.delete(name);
    }

    @Override
    public void removeHeaders() {
        this.qheaders = new NameValueList();
    }

    @Override
    public void removeUserType() {
        if (this.uriParms == null) {
            return;
        }
        this.uriParms.delete("user");
    }

    public void removePort() {
        this.authority.removePort();
    }

    public void removeMethod() {
        if (this.uriParms == null) {
            return;
        }
        this.uriParms.delete(XMLWriter.METHOD);
    }

    @Override
    public void setUser(String uname) {
        if (this.authority == null) {
            this.authority = new Authority();
        }
        this.authority.setUser(uname);
    }

    public void removeUser() {
        this.authority.removeUserInfo();
    }

    public void setDefaultParm(String name, Object value) {
        if (this.uriParms.getValue(name) != null) {
            return;
        }
        NameValue nv = new NameValue(name, value);
        this.uriParms.set(nv);
    }

    public void setAuthority(Authority authority) {
        this.authority = authority;
    }

    public void setHost(Host h) {
        if (this.authority == null) {
            this.authority = new Authority();
        }
        this.authority.setHost(h);
    }

    public void setUriParms(NameValueList parms) {
        this.uriParms = parms;
    }

    public void setUriParm(String name, Object value) {
        NameValue nv = new NameValue(name, value);
        this.uriParms.set(nv);
    }

    public void setQheaders(NameValueList parms) {
        this.qheaders = parms;
    }

    public void setMAddr(String mAddr) {
        NameValue nameValue = this.uriParms.getNameValue("maddr");
        Host host = new Host();
        host.setAddress(mAddr);
        if (nameValue != null) {
            nameValue.setValueAsObject(host);
        } else {
            this.uriParms.set(new NameValue("maddr", host));
        }
    }

    @Override
    public void setUserParam(String usertype) {
        this.uriParms.set("user", usertype);
    }

    public void setMethod(String method) {
        this.uriParms.set(XMLWriter.METHOD, method);
    }

    public void setIsdnSubAddress(String isdnSubAddress) {
        if (this.telephoneSubscriber == null) {
            this.telephoneSubscriber = new TelephoneNumber();
        }
        this.telephoneSubscriber.setIsdnSubaddress(isdnSubAddress);
    }

    public void setTelephoneSubscriber(TelephoneNumber tel) {
        this.telephoneSubscriber = tel;
    }

    @Override
    public void setPort(int p) {
        if (this.authority == null) {
            this.authority = new Authority();
        }
        this.authority.setPort(p);
    }

    public boolean hasParameter(String name) {
        return this.uriParms.getValue(name) != null;
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
    public void removeParameter(String name) {
        this.uriParms.delete(name);
    }

    public void setHostPort(HostPort hostPort) {
        if (this.authority == null) {
            this.authority = new Authority();
        }
        this.authority.setHostPort(hostPort);
    }

    @Override
    public Object clone() {
        SipUri retval = (SipUri) super.clone();
        if (this.authority != null) {
            retval.authority = (Authority) this.authority.clone();
        }
        if (this.uriParms != null) {
            retval.uriParms = (NameValueList) this.uriParms.clone();
        }
        if (this.qheaders != null) {
            retval.qheaders = (NameValueList) this.qheaders.clone();
        }
        if (this.telephoneSubscriber != null) {
            retval.telephoneSubscriber = (TelephoneNumber) this.telephoneSubscriber.clone();
        }
        return retval;
    }

    @Override
    public String getHeader(String name) {
        if (this.qheaders.getValue(name) != null) {
            return this.qheaders.getValue(name).toString();
        }
        return null;
    }

    @Override
    public Iterator<String> getHeaderNames() {
        return this.qheaders.getNames();
    }

    @Override
    public String getLrParam() {
        boolean haslr = hasParameter("lr");
        if (haslr) {
            return "true";
        }
        return null;
    }

    @Override
    public String getMAddrParam() {
        NameValue maddr = this.uriParms.getNameValue("maddr");
        if (maddr == null) {
            return null;
        }
        String host = (String) maddr.getValueAsObject();
        return host;
    }

    @Override
    public String getMethodParam() {
        return getParameter(XMLWriter.METHOD);
    }

    @Override
    public String getParameter(String name) {
        ?? value = this.uriParms.getValue(name);
        if (value == 0) {
            return null;
        }
        return value instanceof GenericObject ? value.encode() : value.toString();
    }

    @Override
    public Iterator<String> getParameterNames() {
        return this.uriParms.getNames();
    }

    @Override
    public int getTTLParam() {
        Integer ttl = (Integer) this.uriParms.getValue("ttl");
        if (ttl != null) {
            return ttl.intValue();
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
    public void setHeader(String name, String value) {
        NameValue nv = new NameValue(name, value);
        this.qheaders.set(nv);
    }

    @Override
    public void setHost(String host) throws ParseException {
        Host h = new Host(host);
        setHost(h);
    }

    @Override
    public void setLrParam() {
        this.uriParms.set("lr", null);
    }

    @Override
    public void setMAddrParam(String maddr) throws ParseException {
        if (maddr == null) {
            throw new NullPointerException("bad maddr");
        }
        setParameter("maddr", maddr);
    }

    @Override
    public void setMethodParam(String method) throws ParseException {
        setParameter(XMLWriter.METHOD, method);
    }

    @Override
    public void setParameter(String name, String value) throws ParseException {
        if (name.equalsIgnoreCase("ttl")) {
            try {
                Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new ParseException("bad parameter " + value, 0);
            }
        }
        this.uriParms.set(name, value);
    }

    @Override
    public void setSecure(boolean secure) {
        if (secure) {
            this.scheme = "sips";
        } else {
            this.scheme = "sip";
        }
    }

    @Override
    public void setTTLParam(int ttl) {
        if (ttl <= 0) {
            throw new IllegalArgumentException("Bad ttl value");
        }
        if (this.uriParms == null) {
            return;
        }
        NameValue nv = new NameValue("ttl", Integer.valueOf(ttl));
        this.uriParms.set(nv);
    }

    @Override
    public void setTransportParam(String transport) throws ParseException {
        if (transport == null) {
            throw new NullPointerException("null arg");
        }
        if (transport.compareToIgnoreCase(ListeningPoint.UDP) == 0 || transport.compareToIgnoreCase(ListeningPoint.TLS) == 0 || transport.compareToIgnoreCase(ListeningPoint.TCP) == 0 || transport.compareToIgnoreCase(ListeningPoint.SCTP) == 0) {
            NameValue nv = new NameValue(gov.nist.javax.sip.header.ParameterNames.TRANSPORT, transport.toLowerCase());
            this.uriParms.set(nv);
            return;
        }
        throw new ParseException("bad transport " + transport, 0);
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
    public void setGrParam(String value) {
        this.uriParms.set("gr", value);
    }

    public String getGrParam() {
        return (String) this.uriParms.getValue("gr");
    }
}
