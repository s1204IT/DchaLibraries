package java.net.cta;

import dalvik.system.PathClassLoader;
import java.lang.reflect.Method;

public class CtaAdapter {
    private static Method enforceCheckPermissionMethod;

    public static boolean isSendingPermitted(int i) {
        try {
            if (enforceCheckPermissionMethod == null) {
                enforceCheckPermissionMethod = Class.forName("com.mediatek.cta.CtaHttp", false, new PathClassLoader("system/framework/mediatek-cta.jar", ClassLoader.getSystemClassLoader())).getMethod("isSendingPermitted", Integer.TYPE);
            }
            return ((Boolean) enforceCheckPermissionMethod.invoke(null, Integer.valueOf(i))).booleanValue();
        } catch (ReflectiveOperationException e) {
            System.out.println("e:" + ((Object) e));
            if (e.getCause() instanceof SecurityException) {
                throw new SecurityException(e.getCause());
            }
            return true;
        } catch (Throwable th) {
            if (th instanceof NoClassDefFoundError) {
                System.out.println("ee:" + ((Object) th));
            }
            return true;
        }
    }
}
