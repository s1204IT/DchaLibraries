package org.apache.http.impl.cookie;

import java.util.Locale;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieAttributeHandler;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.cookie.SetCookie;

@Deprecated
public class RFC2965DomainAttributeHandler implements CookieAttributeHandler {
    @Override
    public void parse(SetCookie setCookie, String str) throws MalformedCookieException {
        if (setCookie == null) {
            throw new IllegalArgumentException("Cookie may not be null");
        }
        if (str == null) {
            throw new MalformedCookieException("Missing value for domain attribute");
        }
        if (str.trim().length() == 0) {
            throw new MalformedCookieException("Blank value for domain attribute");
        }
        String lowerCase = str.toLowerCase(Locale.ENGLISH);
        if (!lowerCase.startsWith(".")) {
            lowerCase = '.' + lowerCase;
        }
        setCookie.setDomain(lowerCase);
    }

    public boolean domainMatch(String str, String str2) {
        return str.equals(str2) || (str2.startsWith(".") && str.endsWith(str2));
    }

    @Override
    public void validate(Cookie cookie, CookieOrigin cookieOrigin) throws MalformedCookieException {
        if (cookie == null) {
            throw new IllegalArgumentException("Cookie may not be null");
        }
        if (cookieOrigin == null) {
            throw new IllegalArgumentException("Cookie origin may not be null");
        }
        String lowerCase = cookieOrigin.getHost().toLowerCase(Locale.ENGLISH);
        if (cookie.getDomain() == null) {
            throw new MalformedCookieException("Invalid cookie state: domain not specified");
        }
        String lowerCase2 = cookie.getDomain().toLowerCase(Locale.ENGLISH);
        if ((cookie instanceof ClientCookie) && ((ClientCookie) cookie).containsAttribute(ClientCookie.DOMAIN_ATTR)) {
            if (!lowerCase2.startsWith(".")) {
                throw new MalformedCookieException("Domain attribute \"" + cookie.getDomain() + "\" violates RFC 2109: domain must start with a dot");
            }
            int iIndexOf = lowerCase2.indexOf(46, 1);
            if ((iIndexOf < 0 || iIndexOf == lowerCase2.length() - 1) && !lowerCase2.equals(".local")) {
                throw new MalformedCookieException("Domain attribute \"" + cookie.getDomain() + "\" violates RFC 2965: the value contains no embedded dots and the value is not .local");
            }
            if (!domainMatch(lowerCase, lowerCase2)) {
                throw new MalformedCookieException("Domain attribute \"" + cookie.getDomain() + "\" violates RFC 2965: effective host name does not domain-match domain attribute.");
            }
            if (lowerCase.substring(0, lowerCase.length() - lowerCase2.length()).indexOf(46) != -1) {
                throw new MalformedCookieException("Domain attribute \"" + cookie.getDomain() + "\" violates RFC 2965: effective host minus domain may not contain any dots");
            }
            return;
        }
        if (!cookie.getDomain().equals(lowerCase)) {
            throw new MalformedCookieException("Illegal domain attribute: \"" + cookie.getDomain() + "\".Domain of origin: \"" + lowerCase + "\"");
        }
    }

    @Override
    public boolean match(Cookie cookie, CookieOrigin cookieOrigin) {
        if (cookie == null) {
            throw new IllegalArgumentException("Cookie may not be null");
        }
        if (cookieOrigin == null) {
            throw new IllegalArgumentException("Cookie origin may not be null");
        }
        String lowerCase = cookieOrigin.getHost().toLowerCase(Locale.ENGLISH);
        String domain = cookie.getDomain();
        return domainMatch(lowerCase, domain) && lowerCase.substring(0, lowerCase.length() - domain.length()).indexOf(46) == -1;
    }
}
