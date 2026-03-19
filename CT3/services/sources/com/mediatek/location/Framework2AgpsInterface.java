package com.mediatek.location;

import com.mediatek.socket.base.SocketUtils;
import com.mediatek.socket.base.UdpClient;

public class Framework2AgpsInterface {
    public static final int DNS_QUERY_RESULT = 0;
    public static final int MAX_BUFF_SIZE = 35;
    public static final int PROTOCOL_TYPE = 302;

    public static class Framework2AgpsInterfaceSender {
        public boolean DnsQueryResult(UdpClient client, boolean isSuccess, boolean hasIpv4, int ipv4, boolean hasIpv6, byte[] ipv6) {
            if (!client.connect()) {
                return false;
            }
            SocketUtils.BaseBuffer buff = client.getBuff();
            buff.putInt(Framework2AgpsInterface.PROTOCOL_TYPE);
            buff.putInt(0);
            buff.putBool(isSuccess);
            buff.putBool(hasIpv4);
            buff.putInt(ipv4);
            buff.putBool(hasIpv6);
            SocketUtils.assertSize(ipv6, 16, 0);
            buff.putArrayByte(ipv6);
            boolean _ret = client.write();
            client.close();
            return _ret;
        }
    }
}
