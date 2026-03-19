package org.apache.http.impl.cookie;

import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.CookieSpecFactory;
import org.apache.http.cookie.params.CookieSpecPNames;
import org.apache.http.params.HttpParams;

@Deprecated
public class RFC2965SpecFactory implements CookieSpecFactory {
    @Override
    public CookieSpec newInstance(HttpParams httpParams) {
        if (httpParams != null) {
            return new RFC2965Spec((String[]) httpParams.getParameter(CookieSpecPNames.DATE_PATTERNS), httpParams.getBooleanParameter(CookieSpecPNames.SINGLE_COOKIE_HEADER, false));
        }
        return new RFC2965Spec();
    }
}
