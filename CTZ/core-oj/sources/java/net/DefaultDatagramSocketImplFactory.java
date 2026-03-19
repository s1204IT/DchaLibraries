package java.net;

import java.security.AccessController;
import sun.security.action.GetPropertyAction;

class DefaultDatagramSocketImplFactory {
    static Class<?> prefixImplClass;

    DefaultDatagramSocketImplFactory() {
    }

    static {
        String str;
        prefixImplClass = null;
        try {
            str = (String) AccessController.doPrivileged(new GetPropertyAction("impl.prefix", null));
            if (str != null) {
                try {
                    prefixImplClass = Class.forName("java.net." + str + "DatagramSocketImpl");
                } catch (Exception e) {
                    System.err.println("Can't find class: java.net." + str + "DatagramSocketImpl: check impl.prefix property");
                }
            }
        } catch (Exception e2) {
            str = null;
        }
    }

    static DatagramSocketImpl createDatagramSocketImpl(boolean z) throws SocketException {
        if (prefixImplClass != null) {
            try {
                return (DatagramSocketImpl) prefixImplClass.newInstance();
            } catch (Exception e) {
                throw new SocketException("can't instantiate DatagramSocketImpl");
            }
        }
        return new PlainDatagramSocketImpl();
    }
}
