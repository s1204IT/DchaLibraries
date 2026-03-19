package mf.org.apache.xml.serialize;

import java.security.AccessController;
import java.security.PrivilegedAction;

final class SecuritySupport {
    static String getSystemProperty(final String propName) {
        return (String) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                return System.getProperty(propName);
            }
        });
    }
}
