package javax.crypto;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

final class extObjectInputStream extends ObjectInputStream {
    private static ClassLoader systemClassLoader = null;

    extObjectInputStream(InputStream inputStream) throws IOException {
        super(inputStream);
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass objectStreamClass) throws ClassNotFoundException, IOException {
        try {
            return super.resolveClass(objectStreamClass);
        } catch (ClassNotFoundException e) {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (contextClassLoader == null) {
                if (systemClassLoader == null) {
                    systemClassLoader = ClassLoader.getSystemClassLoader();
                }
                contextClassLoader = systemClassLoader;
                if (contextClassLoader == null) {
                    throw new ClassNotFoundException(objectStreamClass.getName());
                }
            }
            return Class.forName(objectStreamClass.getName(), false, contextClassLoader);
        }
    }
}
