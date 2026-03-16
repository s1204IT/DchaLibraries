package android.webkit;

import android.webkit.CacheManager;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

@Deprecated
public final class UrlInterceptRegistry {
    private static final String LOGTAG = "intercept";
    private static boolean mDisabled = false;
    private static LinkedList mHandlerList;

    private static synchronized LinkedList getHandlers() {
        if (mHandlerList == null) {
            mHandlerList = new LinkedList();
        }
        return mHandlerList;
    }

    @Deprecated
    public static synchronized void setUrlInterceptDisabled(boolean disabled) {
        mDisabled = disabled;
    }

    @Deprecated
    public static synchronized boolean urlInterceptDisabled() {
        return mDisabled;
    }

    @Deprecated
    public static synchronized boolean registerHandler(UrlInterceptHandler handler) {
        boolean z;
        if (getHandlers().contains(handler)) {
            z = false;
        } else {
            getHandlers().addFirst(handler);
            z = true;
        }
        return z;
    }

    @Deprecated
    public static synchronized boolean unregisterHandler(UrlInterceptHandler handler) {
        return getHandlers().remove(handler);
    }

    @Deprecated
    public static synchronized CacheManager.CacheResult getSurrogate(String url, Map<String, String> headers) {
        CacheManager.CacheResult result;
        if (!urlInterceptDisabled()) {
            Iterator iter = getHandlers().listIterator();
            while (true) {
                if (!iter.hasNext()) {
                    result = null;
                    break;
                }
                UrlInterceptHandler handler = (UrlInterceptHandler) iter.next();
                result = handler.service(url, headers);
                if (result != null) {
                    break;
                }
            }
        } else {
            result = null;
        }
        return result;
    }

    @Deprecated
    public static synchronized PluginData getPluginData(String url, Map<String, String> headers) {
        PluginData data;
        if (!urlInterceptDisabled()) {
            Iterator iter = getHandlers().listIterator();
            while (true) {
                if (!iter.hasNext()) {
                    data = null;
                    break;
                }
                UrlInterceptHandler handler = (UrlInterceptHandler) iter.next();
                data = handler.getPluginData(url, headers);
                if (data != null) {
                    break;
                }
            }
        } else {
            data = null;
        }
        return data;
    }
}
