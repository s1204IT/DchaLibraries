package org.apache.http.impl.cookie;

import java.util.Locale;
import java.util.StringTokenizer;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.MalformedCookieException;

@Deprecated
public class NetscapeDomainHandler extends BasicDomainHandler {
    @Override
    public void validate(Cookie cookie, CookieOrigin cookieOrigin) throws MalformedCookieException {
        super.validate(cookie, cookieOrigin);
        String host = cookieOrigin.getHost();
        String domain = cookie.getDomain();
        if (host.contains(".")) {
            int iCountTokens = new StringTokenizer(domain, ".").countTokens();
            if (isSpecialDomain(domain)) {
                if (iCountTokens < 2) {
                    throw new MalformedCookieException("Domain attribute \"" + domain + "\" violates the Netscape cookie specification for special domains");
                }
                return;
            }
            if (iCountTokens < 3) {
                throw new MalformedCookieException("Domain attribute \"" + domain + "\" violates the Netscape cookie specification");
            }
        }
    }

    private static boolean isSpecialDomain(String str) {
        String upperCase = str.toUpperCase(Locale.ENGLISH);
        return upperCase.endsWith(".COM") || upperCase.endsWith(".EDU") || upperCase.endsWith(".NET") || upperCase.endsWith(".GOV") || upperCase.endsWith(".MIL") || upperCase.endsWith(".ORG") || upperCase.endsWith(".INT");
    }

    @Override
    public boolean match(Cookie cookie, CookieOrigin cookieOrigin) {
        if (cookie == null) {
            throw new IllegalArgumentException("Cookie may not be null");
        }
        if (cookieOrigin == null) {
            throw new IllegalArgumentException("Cookie origin may not be null");
        }
        String host = cookieOrigin.getHost();
        String domain = cookie.getDomain();
        if (domain == null) {
            return false;
        }
        return host.endsWith(domain);
    }
}
