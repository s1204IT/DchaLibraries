package org.apache.http.impl.cookie;

import gov.nist.core.Separators;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieAttributeHandler;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.cookie.SetCookie;

@Deprecated
public class BasicDomainHandler implements CookieAttributeHandler {
    @Override
    public void parse(SetCookie cookie, String value) throws MalformedCookieException {
        if (cookie == null) {
            throw new IllegalArgumentException("Cookie may not be null");
        }
        if (value == null) {
            throw new MalformedCookieException("Missing value for domain attribute");
        }
        if (value.trim().length() == 0) {
            throw new MalformedCookieException("Blank value for domain attribute");
        }
        cookie.setDomain(value);
    }

    @Override
    public void validate(Cookie cookie, CookieOrigin origin) throws MalformedCookieException {
        if (cookie == null) {
            throw new IllegalArgumentException("Cookie may not be null");
        }
        if (origin == null) {
            throw new IllegalArgumentException("Cookie origin may not be null");
        }
        String host = origin.getHost();
        String domain = cookie.getDomain();
        if (domain == null) {
            throw new MalformedCookieException("Cookie domain may not be null");
        }
        if (host.contains(Separators.DOT)) {
            if (!host.endsWith(domain)) {
                if (domain.startsWith(Separators.DOT)) {
                    domain = domain.substring(1, domain.length());
                }
                if (!host.equals(domain)) {
                    throw new MalformedCookieException("Illegal domain attribute \"" + domain + "\". Domain of origin: \"" + host + Separators.DOUBLE_QUOTE);
                }
                return;
            }
            return;
        }
        if (!host.equals(domain)) {
            throw new MalformedCookieException("Illegal domain attribute \"" + domain + "\". Domain of origin: \"" + host + Separators.DOUBLE_QUOTE);
        }
    }

    @Override
    public boolean match(Cookie cookie, CookieOrigin origin) {
        if (cookie == null) {
            throw new IllegalArgumentException("Cookie may not be null");
        }
        if (origin == null) {
            throw new IllegalArgumentException("Cookie origin may not be null");
        }
        String host = origin.getHost();
        String domain = cookie.getDomain();
        if (domain == null) {
            return false;
        }
        if (host.equals(domain)) {
            return true;
        }
        if (!domain.startsWith(Separators.DOT)) {
            domain = '.' + domain;
        }
        return host.endsWith(domain) || host.equals(domain.substring(1));
    }
}
