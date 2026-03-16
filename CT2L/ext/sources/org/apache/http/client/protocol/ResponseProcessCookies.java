package org.apache.http.client.protocol;

import java.io.IOException;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.cookie.SM;
import org.apache.http.protocol.HttpContext;

@Deprecated
public class ResponseProcessCookies implements HttpResponseInterceptor {
    private final Log log = LogFactory.getLog(getClass());

    @Override
    public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
        if (response == null) {
            throw new IllegalArgumentException("HTTP request may not be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("HTTP context may not be null");
        }
        CookieStore cookieStore = (CookieStore) context.getAttribute(ClientContext.COOKIE_STORE);
        if (cookieStore == null) {
            this.log.info("Cookie store not available in HTTP context");
            return;
        }
        CookieSpec cookieSpec = (CookieSpec) context.getAttribute(ClientContext.COOKIE_SPEC);
        if (cookieSpec == null) {
            this.log.info("CookieSpec not available in HTTP context");
            return;
        }
        CookieOrigin cookieOrigin = (CookieOrigin) context.getAttribute(ClientContext.COOKIE_ORIGIN);
        if (cookieOrigin == null) {
            this.log.info("CookieOrigin not available in HTTP context");
            return;
        }
        HeaderIterator it = response.headerIterator(SM.SET_COOKIE);
        processCookies(it, cookieSpec, cookieOrigin, cookieStore);
        if (cookieSpec.getVersion() > 0) {
            HeaderIterator it2 = response.headerIterator(SM.SET_COOKIE2);
            processCookies(it2, cookieSpec, cookieOrigin, cookieStore);
        }
    }

    private void processCookies(HeaderIterator iterator, CookieSpec cookieSpec, CookieOrigin cookieOrigin, CookieStore cookieStore) {
        while (iterator.hasNext()) {
            Header header = iterator.nextHeader();
            try {
                List<Cookie> cookies = cookieSpec.parse(header, cookieOrigin);
                for (Cookie cookie : cookies) {
                    try {
                        cookieSpec.validate(cookie, cookieOrigin);
                        cookieStore.addCookie(cookie);
                        if (this.log.isDebugEnabled()) {
                            this.log.debug("Cookie accepted: \"" + cookieToString(cookie) + "\". ");
                        }
                    } catch (MalformedCookieException ex) {
                        if (this.log.isWarnEnabled()) {
                            this.log.warn("Cookie rejected: \"" + cookieToString(cookie) + "\". " + ex.getMessage());
                        }
                    }
                }
            } catch (MalformedCookieException ex2) {
                if (this.log.isWarnEnabled()) {
                    this.log.warn("Invalid cookie header: \"" + header + "\". " + ex2.getMessage());
                }
            }
        }
    }

    private String cookieToString(Cookie cookie) {
        return cookie.getClass().getSimpleName() + "[version=" + cookie.getVersion() + ",name=" + cookie.getName() + ",domain=" + cookie.getDomain() + ",path=" + cookie.getPath() + ",expiry=" + cookie.getExpiryDate() + "]";
    }
}
