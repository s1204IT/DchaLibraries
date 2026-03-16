package org.apache.http.impl.cookie;

import gov.nist.core.Separators;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.NameValuePair;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieAttributeHandler;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.cookie.SM;
import org.apache.http.message.BufferedHeader;
import org.apache.http.util.CharArrayBuffer;

@Deprecated
public class RFC2965Spec extends RFC2109Spec {
    public RFC2965Spec() {
        this(null, false);
    }

    public RFC2965Spec(String[] datepatterns, boolean oneHeader) {
        super(datepatterns, oneHeader);
        registerAttribHandler("domain", new RFC2965DomainAttributeHandler());
        registerAttribHandler(ClientCookie.PORT_ATTR, new RFC2965PortAttributeHandler());
        registerAttribHandler(ClientCookie.COMMENTURL_ATTR, new RFC2965CommentUrlAttributeHandler());
        registerAttribHandler(ClientCookie.DISCARD_ATTR, new RFC2965DiscardAttributeHandler());
        registerAttribHandler("version", new RFC2965VersionAttributeHandler());
    }

    private BasicClientCookie createCookie(String name, String value, CookieOrigin origin) {
        BasicClientCookie cookie = new BasicClientCookie(name, value);
        cookie.setPath(getDefaultPath(origin));
        cookie.setDomain(getDefaultDomain(origin));
        return cookie;
    }

    private BasicClientCookie createCookie2(String name, String value, CookieOrigin origin) {
        BasicClientCookie2 cookie = new BasicClientCookie2(name, value);
        cookie.setPath(getDefaultPath(origin));
        cookie.setDomain(getDefaultDomain(origin));
        cookie.setPorts(new int[]{origin.getPort()});
        return cookie;
    }

    @Override
    public List<Cookie> parse(Header header, CookieOrigin origin) throws MalformedCookieException {
        BasicClientCookie cookie;
        if (header == null) {
            throw new IllegalArgumentException("Header may not be null");
        }
        if (origin == null) {
            throw new IllegalArgumentException("Cookie origin may not be null");
        }
        CookieOrigin origin2 = adjustEffectiveHost(origin);
        HeaderElement[] elems = header.getElements();
        List<Cookie> cookies = new ArrayList<>(elems.length);
        int len$ = elems.length;
        int i$ = 0;
        while (true) {
            int i$2 = i$;
            if (i$2 < len$) {
                HeaderElement headerelement = elems[i$2];
                String name = headerelement.getName();
                String value = headerelement.getValue();
                if (name == null || name.length() == 0) {
                    break;
                }
                if (header.getName().equals(SM.SET_COOKIE2)) {
                    cookie = createCookie2(name, value, origin2);
                } else {
                    cookie = createCookie(name, value, origin2);
                }
                NameValuePair[] attribs = headerelement.getParameters();
                Map<String, NameValuePair> attribmap = new HashMap<>(attribs.length);
                for (int j = attribs.length - 1; j >= 0; j--) {
                    NameValuePair param = attribs[j];
                    attribmap.put(param.getName().toLowerCase(Locale.ENGLISH), param);
                }
                for (Map.Entry<String, NameValuePair> entry : attribmap.entrySet()) {
                    NameValuePair attrib = entry.getValue();
                    String s = attrib.getName().toLowerCase(Locale.ENGLISH);
                    cookie.setAttribute(s, attrib.getValue());
                    CookieAttributeHandler handler = findAttribHandler(s);
                    if (handler != null) {
                        handler.parse(cookie, attrib.getValue());
                    }
                }
                cookies.add(cookie);
                i$ = i$2 + 1;
            } else {
                return cookies;
            }
        }
        throw new MalformedCookieException("Cookie name may not be empty");
    }

    @Override
    public void validate(Cookie cookie, CookieOrigin origin) throws MalformedCookieException {
        if (cookie == null) {
            throw new IllegalArgumentException("Cookie may not be null");
        }
        if (origin == null) {
            throw new IllegalArgumentException("Cookie origin may not be null");
        }
        super.validate(cookie, adjustEffectiveHost(origin));
    }

    @Override
    public boolean match(Cookie cookie, CookieOrigin origin) {
        if (cookie == null) {
            throw new IllegalArgumentException("Cookie may not be null");
        }
        if (origin == null) {
            throw new IllegalArgumentException("Cookie origin may not be null");
        }
        return super.match(cookie, adjustEffectiveHost(origin));
    }

    @Override
    protected void formatCookieAsVer(CharArrayBuffer buffer, Cookie cookie, int version) {
        String s;
        int[] ports;
        super.formatCookieAsVer(buffer, cookie, version);
        if ((cookie instanceof ClientCookie) && (s = ((ClientCookie) cookie).getAttribute(ClientCookie.PORT_ATTR)) != null) {
            buffer.append("; $Port");
            buffer.append("=\"");
            if (s.trim().length() > 0 && (ports = cookie.getPorts()) != null) {
                int len = ports.length;
                for (int i = 0; i < len; i++) {
                    if (i > 0) {
                        buffer.append(Separators.COMMA);
                    }
                    buffer.append(Integer.toString(ports[i]));
                }
            }
            buffer.append(Separators.DOUBLE_QUOTE);
        }
    }

    private static CookieOrigin adjustEffectiveHost(CookieOrigin origin) {
        String host = origin.getHost();
        boolean isLocalHost = true;
        for (int i = 0; i < host.length(); i++) {
            char ch = host.charAt(i);
            if (ch == '.' || ch == ':') {
                isLocalHost = false;
                break;
            }
        }
        if (isLocalHost) {
            return new CookieOrigin(host + ".local", origin.getPort(), origin.getPath(), origin.isSecure());
        }
        return origin;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public Header getVersionHeader() {
        CharArrayBuffer buffer = new CharArrayBuffer(40);
        buffer.append(SM.COOKIE2);
        buffer.append(": ");
        buffer.append("$Version=");
        buffer.append(Integer.toString(getVersion()));
        return new BufferedHeader(buffer);
    }
}
