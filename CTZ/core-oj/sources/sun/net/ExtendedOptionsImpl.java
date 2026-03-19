package sun.net;

import java.io.FileDescriptor;
import java.net.SocketOption;
import jdk.net.NetworkPermission;
import jdk.net.SocketFlow;

public class ExtendedOptionsImpl {
    private ExtendedOptionsImpl() {
    }

    public static void checkSetOptionPermission(SocketOption<?> socketOption) {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager == null) {
            return;
        }
        securityManager.checkPermission(new NetworkPermission("setOption." + socketOption.name()));
    }

    public static void checkGetOptionPermission(SocketOption<?> socketOption) {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager == null) {
            return;
        }
        securityManager.checkPermission(new NetworkPermission("getOption." + socketOption.name()));
    }

    public static void checkValueType(Object obj, Class<?> cls) {
        if (!cls.isAssignableFrom(obj.getClass())) {
            throw new IllegalArgumentException("Found: " + obj.getClass().toString() + " Expected: " + cls.toString());
        }
    }

    public static void setFlowOption(FileDescriptor fileDescriptor, SocketFlow socketFlow) {
        throw new UnsupportedOperationException("unsupported socket option");
    }

    public static void getFlowOption(FileDescriptor fileDescriptor, SocketFlow socketFlow) {
        throw new UnsupportedOperationException("unsupported socket option");
    }

    public static boolean flowSupported() {
        return false;
    }
}
