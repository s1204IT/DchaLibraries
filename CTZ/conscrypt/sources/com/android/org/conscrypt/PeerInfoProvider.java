package com.android.org.conscrypt;

abstract class PeerInfoProvider {
    private static final PeerInfoProvider NULL_PEER_INFO_PROVIDER = new PeerInfoProvider() {
        @Override
        String getHostname() {
            return null;
        }

        @Override
        public String getHostnameOrIP() {
            return null;
        }

        @Override
        public int getPort() {
            return -1;
        }
    };

    abstract String getHostname();

    abstract String getHostnameOrIP();

    abstract int getPort();

    PeerInfoProvider() {
    }

    static PeerInfoProvider nullProvider() {
        return NULL_PEER_INFO_PROVIDER;
    }

    static PeerInfoProvider forHostAndPort(final String str, final int i) {
        return new PeerInfoProvider() {
            @Override
            String getHostname() {
                return str;
            }

            @Override
            public String getHostnameOrIP() {
                return str;
            }

            @Override
            public int getPort() {
                return i;
            }
        };
    }
}
