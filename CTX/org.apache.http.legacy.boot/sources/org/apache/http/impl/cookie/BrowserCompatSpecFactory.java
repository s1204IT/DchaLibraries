package org.apache.http.impl.cookie;

import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.CookieSpecFactory;
import org.apache.http.cookie.params.CookieSpecPNames;
import org.apache.http.params.HttpParams;

@Deprecated
public class BrowserCompatSpecFactory implements CookieSpecFactory {
    @Override
    public CookieSpec newInstance(HttpParams httpParams) {
        if (httpParams != null) {
            return new BrowserCompatSpec((String[]) httpParams.getParameter(CookieSpecPNames.DATE_PATTERNS));
        }
        return new BrowserCompatSpec();
    }
}
