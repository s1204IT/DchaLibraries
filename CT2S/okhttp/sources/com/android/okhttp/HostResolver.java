package com.android.okhttp;

import java.net.InetAddress;
import java.net.UnknownHostException;

public interface HostResolver {
    public static final HostResolver DEFAULT = new HostResolver() {
        @Override
        public InetAddress[] getAllByName(String host) throws UnknownHostException {
            if (host == null) {
                throw new UnknownHostException("host == null");
            }
            return InetAddress.getAllByName(host);
        }
    };

    InetAddress[] getAllByName(String str) throws UnknownHostException;
}
