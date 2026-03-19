package org.apache.http.conn.routing;

import java.net.InetAddress;
import org.apache.http.HttpHost;

@Deprecated
public interface RouteInfo {
    int getHopCount();

    HttpHost getHopTarget(int i);

    LayerType getLayerType();

    InetAddress getLocalAddress();

    HttpHost getProxyHost();

    HttpHost getTargetHost();

    TunnelType getTunnelType();

    boolean isLayered();

    boolean isSecure();

    boolean isTunnelled();

    public enum TunnelType {
        PLAIN,
        TUNNELLED;

        public static TunnelType[] valuesCustom() {
            return values();
        }
    }

    public enum LayerType {
        PLAIN,
        LAYERED;

        public static LayerType[] valuesCustom() {
            return values();
        }
    }
}
