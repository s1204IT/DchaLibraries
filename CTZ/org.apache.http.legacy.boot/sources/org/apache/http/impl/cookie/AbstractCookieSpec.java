package org.apache.http.impl.cookie;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.cookie.CookieAttributeHandler;
import org.apache.http.cookie.CookieSpec;

@Deprecated
public abstract class AbstractCookieSpec implements CookieSpec {
    private final Map<String, CookieAttributeHandler> attribHandlerMap = new HashMap(10);

    public void registerAttribHandler(String str, CookieAttributeHandler cookieAttributeHandler) {
        if (str == null) {
            throw new IllegalArgumentException("Attribute name may not be null");
        }
        if (cookieAttributeHandler == null) {
            throw new IllegalArgumentException("Attribute handler may not be null");
        }
        this.attribHandlerMap.put(str, cookieAttributeHandler);
    }

    protected CookieAttributeHandler findAttribHandler(String str) {
        return this.attribHandlerMap.get(str);
    }

    protected CookieAttributeHandler getAttribHandler(String str) {
        CookieAttributeHandler cookieAttributeHandlerFindAttribHandler = findAttribHandler(str);
        if (cookieAttributeHandlerFindAttribHandler == null) {
            throw new IllegalStateException("Handler not registered for " + str + " attribute.");
        }
        return cookieAttributeHandlerFindAttribHandler;
    }

    protected Collection<CookieAttributeHandler> getAttribHandlers() {
        return this.attribHandlerMap.values();
    }
}
