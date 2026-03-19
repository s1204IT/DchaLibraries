package org.apache.http.conn.params;

import java.util.HashMap;
import java.util.Map;
import org.apache.http.conn.routing.HttpRoute;

@Deprecated
public final class ConnPerRouteBean implements ConnPerRoute {
    public static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = 2;
    private int defaultMax;
    private final Map<HttpRoute, Integer> maxPerHostMap;

    public ConnPerRouteBean(int i) {
        this.maxPerHostMap = new HashMap();
        setDefaultMaxPerRoute(i);
    }

    public ConnPerRouteBean() {
        this(2);
    }

    public int getDefaultMax() {
        return this.defaultMax;
    }

    public void setDefaultMaxPerRoute(int i) {
        if (i < 1) {
            throw new IllegalArgumentException("The maximum must be greater than 0.");
        }
        this.defaultMax = i;
    }

    public void setMaxForRoute(HttpRoute httpRoute, int i) {
        if (httpRoute == null) {
            throw new IllegalArgumentException("HTTP route may not be null.");
        }
        if (i < 1) {
            throw new IllegalArgumentException("The maximum must be greater than 0.");
        }
        this.maxPerHostMap.put(httpRoute, Integer.valueOf(i));
    }

    @Override
    public int getMaxForRoute(HttpRoute httpRoute) {
        if (httpRoute == null) {
            throw new IllegalArgumentException("HTTP route may not be null.");
        }
        Integer num = this.maxPerHostMap.get(httpRoute);
        if (num != null) {
            return num.intValue();
        }
        return this.defaultMax;
    }

    public void setMaxForRoutes(Map<HttpRoute, Integer> map) {
        if (map == null) {
            return;
        }
        this.maxPerHostMap.clear();
        this.maxPerHostMap.putAll(map);
    }
}
