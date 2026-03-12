package android.net.dns;

import android.net.Network;
import android.system.GaiException;
import android.system.OsConstants;
import android.system.StructAddrinfo;
import java.net.InetAddress;
import java.net.UnknownHostException;
import libcore.io.Libcore;

public class ResolvUtil {
    public static InetAddress[] blockingResolveAllLocally(Network network, String str) throws UnknownHostException {
        return blockingResolveAllLocally(network, str, OsConstants.AI_ADDRCONFIG);
    }

    public static InetAddress[] blockingResolveAllLocally(Network network, String str, int i) throws UnknownHostException {
        StructAddrinfo structAddrinfo = new StructAddrinfo();
        structAddrinfo.ai_flags = i;
        structAddrinfo.ai_family = OsConstants.AF_UNSPEC;
        structAddrinfo.ai_socktype = OsConstants.SOCK_STREAM;
        try {
            return Libcore.os.android_getaddrinfo(str, structAddrinfo, getNetworkWithUseLocalNameserversFlag(network).netId);
        } catch (GaiException e) {
            e.rethrowAsUnknownHostException(str + ": TLS-bypass resolution failed");
            return null;
        }
    }

    public static Network getNetworkWithUseLocalNameserversFlag(Network network) {
        return new Network((int) (((long) network.netId) | 2147483648L));
    }

    public static Network makeNetworkWithPrivateDnsBypass(final Network network) {
        return new Network(network) {
            @Override
            public InetAddress[] getAllByName(String str) throws UnknownHostException {
                return ResolvUtil.blockingResolveAllLocally(network, str);
            }
        };
    }
}
