package org.apache.http.impl.cookie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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

    public RFC2965Spec(String[] strArr, boolean z) {
        super(strArr, z);
        registerAttribHandler(ClientCookie.DOMAIN_ATTR, new RFC2965DomainAttributeHandler());
        registerAttribHandler(ClientCookie.PORT_ATTR, new RFC2965PortAttributeHandler());
        registerAttribHandler(ClientCookie.COMMENTURL_ATTR, new RFC2965CommentUrlAttributeHandler());
        registerAttribHandler(ClientCookie.DISCARD_ATTR, new RFC2965DiscardAttributeHandler());
        registerAttribHandler(ClientCookie.VERSION_ATTR, new RFC2965VersionAttributeHandler());
    }

    private BasicClientCookie createCookie(String str, String str2, CookieOrigin cookieOrigin) {
        BasicClientCookie basicClientCookie = new BasicClientCookie(str, str2);
        basicClientCookie.setPath(getDefaultPath(cookieOrigin));
        basicClientCookie.setDomain(getDefaultDomain(cookieOrigin));
        return basicClientCookie;
    }

    private BasicClientCookie createCookie2(String str, String str2, CookieOrigin cookieOrigin) {
        BasicClientCookie2 basicClientCookie2 = new BasicClientCookie2(str, str2);
        basicClientCookie2.setPath(getDefaultPath(cookieOrigin));
        basicClientCookie2.setDomain(getDefaultDomain(cookieOrigin));
        basicClientCookie2.setPorts(new int[]{cookieOrigin.getPort()});
        return basicClientCookie2;
    }

    @Override
    public List<Cookie> parse(Header header, CookieOrigin cookieOrigin) throws MalformedCookieException {
        BasicClientCookie basicClientCookieCreateCookie;
        if (header == null) {
            throw new IllegalArgumentException("Header may not be null");
        }
        if (cookieOrigin == null) {
            throw new IllegalArgumentException("Cookie origin may not be null");
        }
        CookieOrigin cookieOriginAdjustEffectiveHost = adjustEffectiveHost(cookieOrigin);
        HeaderElement[] elements = header.getElements();
        ArrayList arrayList = new ArrayList(elements.length);
        for (HeaderElement headerElement : elements) {
            String name = headerElement.getName();
            String value = headerElement.getValue();
            if (name == null || name.length() == 0) {
                throw new MalformedCookieException("Cookie name may not be empty");
            }
            if (header.getName().equals(SM.SET_COOKIE2)) {
                basicClientCookieCreateCookie = createCookie2(name, value, cookieOriginAdjustEffectiveHost);
            } else {
                basicClientCookieCreateCookie = createCookie(name, value, cookieOriginAdjustEffectiveHost);
            }
            NameValuePair[] parameters = headerElement.getParameters();
            HashMap map = new HashMap(parameters.length);
            for (int length = parameters.length - 1; length >= 0; length--) {
                NameValuePair nameValuePair = parameters[length];
                map.put(nameValuePair.getName().toLowerCase(Locale.ENGLISH), nameValuePair);
            }
            Iterator it = map.entrySet().iterator();
            while (it.hasNext()) {
                NameValuePair nameValuePair2 = (NameValuePair) ((Map.Entry) it.next()).getValue();
                String lowerCase = nameValuePair2.getName().toLowerCase(Locale.ENGLISH);
                basicClientCookieCreateCookie.setAttribute(lowerCase, nameValuePair2.getValue());
                CookieAttributeHandler cookieAttributeHandlerFindAttribHandler = findAttribHandler(lowerCase);
                if (cookieAttributeHandlerFindAttribHandler != null) {
                    cookieAttributeHandlerFindAttribHandler.parse(basicClientCookieCreateCookie, nameValuePair2.getValue());
                }
            }
            arrayList.add(basicClientCookieCreateCookie);
        }
        return arrayList;
    }

    @Override
    public void validate(Cookie cookie, CookieOrigin cookieOrigin) throws MalformedCookieException {
        if (cookie == null) {
            throw new IllegalArgumentException("Cookie may not be null");
        }
        if (cookieOrigin == null) {
            throw new IllegalArgumentException("Cookie origin may not be null");
        }
        super.validate(cookie, adjustEffectiveHost(cookieOrigin));
    }

    @Override
    public boolean match(Cookie cookie, CookieOrigin cookieOrigin) {
        if (cookie == null) {
            throw new IllegalArgumentException("Cookie may not be null");
        }
        if (cookieOrigin == null) {
            throw new IllegalArgumentException("Cookie origin may not be null");
        }
        return super.match(cookie, adjustEffectiveHost(cookieOrigin));
    }

    @Override
    protected void formatCookieAsVer(CharArrayBuffer charArrayBuffer, Cookie cookie, int i) {
        String attribute;
        int[] ports;
        super.formatCookieAsVer(charArrayBuffer, cookie, i);
        if ((cookie instanceof ClientCookie) && (attribute = ((ClientCookie) cookie).getAttribute(ClientCookie.PORT_ATTR)) != null) {
            charArrayBuffer.append("; $Port");
            charArrayBuffer.append("=\"");
            if (attribute.trim().length() > 0 && (ports = cookie.getPorts()) != null) {
                int length = ports.length;
                for (int i2 = 0; i2 < length; i2++) {
                    if (i2 > 0) {
                        charArrayBuffer.append(",");
                    }
                    charArrayBuffer.append(Integer.toString(ports[i2]));
                }
            }
            charArrayBuffer.append("\"");
        }
    }

    private static CookieOrigin adjustEffectiveHost(CookieOrigin cookieOrigin) {
        String host = cookieOrigin.getHost();
        boolean z = false;
        int i = 0;
        while (true) {
            if (i < host.length()) {
                char cCharAt = host.charAt(i);
                if (cCharAt == '.' || cCharAt == ':') {
                    break;
                }
                i++;
            } else {
                z = true;
                break;
            }
        }
        if (z) {
            return new CookieOrigin(host + ".local", cookieOrigin.getPort(), cookieOrigin.getPath(), cookieOrigin.isSecure());
        }
        return cookieOrigin;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public Header getVersionHeader() {
        CharArrayBuffer charArrayBuffer = new CharArrayBuffer(40);
        charArrayBuffer.append(SM.COOKIE2);
        charArrayBuffer.append(": ");
        charArrayBuffer.append("$Version=");
        charArrayBuffer.append(Integer.toString(getVersion()));
        return new BufferedHeader(charArrayBuffer);
    }
}
