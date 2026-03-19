package jdk.net;

import java.security.BasicPermission;

public final class NetworkPermission extends BasicPermission {
    public NetworkPermission(String str) {
        super("");
    }

    public NetworkPermission(String str, String str2) {
        super("", "");
    }
}
