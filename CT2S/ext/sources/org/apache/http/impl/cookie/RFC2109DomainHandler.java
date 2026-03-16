package org.apache.http.impl.cookie;

import gov.nist.core.Separators;
import java.util.Locale;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieAttributeHandler;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.cookie.SetCookie;

@Deprecated
public class RFC2109DomainHandler implements CookieAttributeHandler {
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
        if (!domain.equals(host)) {
            if (domain.indexOf(46) == -1) {
                throw new MalformedCookieException("Domain attribute \"" + domain + "\" does not match the host \"" + host + Separators.DOUBLE_QUOTE);
            }
            if (!domain.startsWith(Separators.DOT)) {
                throw new MalformedCookieException("Domain attribute \"" + domain + "\" violates RFC 2109: domain must start with a dot");
            }
            int dotIndex = domain.indexOf(46, 1);
            if (dotIndex < 0 || dotIndex == domain.length() - 1) {
                throw new MalformedCookieException("Domain attribute \"" + domain + "\" violates RFC 2109: domain must contain an embedded dot");
            }
            String host2 = host.toLowerCase(Locale.ENGLISH);
            if (!host2.endsWith(domain)) {
                throw new MalformedCookieException("Illegal domain attribute \"" + domain + "\". Domain of origin: \"" + host2 + Separators.DOUBLE_QUOTE);
            }
            String hostWithoutDomain = host2.substring(0, host2.length() - domain.length());
            if (hostWithoutDomain.indexOf(46) != -1) {
                throw new MalformedCookieException("Domain attribute \"" + domain + "\" violates RFC 2109: host minus domain may not contain any dots");
            }
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
        return host.equals(domain) || (domain.startsWith(Separators.DOT) && host.endsWith(domain));
    }
}
