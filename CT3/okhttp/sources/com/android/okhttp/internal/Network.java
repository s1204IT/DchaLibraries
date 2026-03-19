package com.android.okhttp.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;

public interface Network {
    public static final Network DEFAULT = new Network() {
        @Override
        public InetAddress[] resolveInetAddresses(String host) throws UnknownHostException {
            if (host == null) {
                throw new UnknownHostException("host == null");
            }
            return InetAddress.getAllByName(host);
        }
    };

    InetAddress[] resolveInetAddresses(String str) throws UnknownHostException;
}
