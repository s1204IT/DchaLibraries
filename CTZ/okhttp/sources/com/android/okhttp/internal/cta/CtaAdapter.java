package com.android.okhttp.internal.cta;

import com.android.okhttp.Connection;
import com.android.okhttp.Request;
import com.android.okhttp.Response;
import dalvik.system.PathClassLoader;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Iterator;

public final class CtaAdapter {
    private static Method ctaCheckPduIsMmsAndEmailSendingPermitted;
    private static Method ctaCheckRequestIsMmsAndEmailSendingPermitted;
    private static Method ctaGetBadHttpResponse;

    public static boolean isSendingPermitted(byte[] bArr) {
        try {
            if (ctaCheckPduIsMmsAndEmailSendingPermitted == null) {
                ctaCheckPduIsMmsAndEmailSendingPermitted = Class.forName("com.mediatek.cta.CtaHttp", false, new PathClassLoader("system/framework/mediatek-cta.jar", ClassLoader.getSystemClassLoader())).getMethod("isMmsAndEmailSendingPermitted", Byte[].class);
            }
            return ((Boolean) ctaCheckPduIsMmsAndEmailSendingPermitted.invoke(null, bArr)).booleanValue();
        } catch (ReflectiveOperationException e) {
            if (e.getCause() instanceof SecurityException) {
                throw new SecurityException(e.getCause());
            }
            boolean z = e.getCause() instanceof ClassNotFoundException;
            return true;
        } catch (Throwable th) {
            if (th instanceof NoClassDefFoundError) {
                System.out.println("ee:" + th);
            }
            return true;
        }
    }

    public static boolean isSendingPermitted(Request request) {
        try {
            if (ctaCheckRequestIsMmsAndEmailSendingPermitted == null) {
                ctaCheckRequestIsMmsAndEmailSendingPermitted = Class.forName("com.mediatek.cta.CtaHttp", false, new PathClassLoader("system/framework/mediatek-cta.jar", ClassLoader.getSystemClassLoader())).getMethod("isMmsAndEmailSendingPermitted", Request.class);
            }
            return ((Boolean) ctaCheckRequestIsMmsAndEmailSendingPermitted.invoke(null, request)).booleanValue();
        } catch (ReflectiveOperationException e) {
            System.out.println("e:" + e);
            if (e.getCause() instanceof SecurityException) {
                throw new SecurityException(e.getCause());
            }
            boolean z = e.getCause() instanceof ClassNotFoundException;
            return true;
        } catch (Throwable th) {
            if (th instanceof NoClassDefFoundError) {
                System.out.println("ee:" + th);
            }
            return true;
        }
    }

    public static Response getBadHttpResponse() {
        try {
            if (ctaGetBadHttpResponse == null) {
                ctaGetBadHttpResponse = Class.forName("com.mediatek.cta.CtaHttp", false, new PathClassLoader("system/framework/mediatek-cta.jar", ClassLoader.getSystemClassLoader())).getMethod("getBadResponse", new Class[0]);
            }
            return (Response) ctaGetBadHttpResponse.invoke(null, new Object[0]);
        } catch (ReflectiveOperationException e) {
            if (e.getCause() instanceof SecurityException) {
                throw new SecurityException(e.getCause());
            }
            boolean z = e.getCause() instanceof ClassNotFoundException;
            return null;
        } catch (Throwable th) {
            if (th instanceof NoClassDefFoundError) {
                System.out.println("ee:" + th);
            }
            return null;
        }
    }

    public static void updateMmsBufferSize(Request request, Connection connection) {
        int iHasSocketBufferSize = hasSocketBufferSize();
        if (iHasSocketBufferSize > 0) {
            try {
                Socket socket = connection.getSocket();
                if (socket != null) {
                    System.out.println("Configure MMS buffer size:" + iHasSocketBufferSize);
                    socket.setSendBufferSize(iHasSocketBufferSize);
                    socket.setReceiveBufferSize(iHasSocketBufferSize * 2);
                }
            } catch (Exception e) {
                System.out.println("Socket Buffer size:" + e);
            }
        }
    }

    public static int hasSocketBufferSize() {
        String property = System.getProperty("socket.buffer.size", "");
        if (property.length() > 0) {
            try {
                return Integer.parseInt(property);
            } catch (Exception e) {
                System.out.println("hasMmsBufferSize:" + e);
                return 0;
            }
        }
        return 0;
    }

    public static boolean isMoMMS(Request request) {
        if ("POST".equals(request.method())) {
            String strHeader = request.header("User-Agent");
            if (strHeader != null && strHeader.indexOf("MMS") != -1) {
                return true;
            }
            String strHeader2 = request.header("Content-Type");
            if (strHeader2 != null && strHeader2.indexOf("application/vnd.wap.mms-message") != -1) {
                return true;
            }
            String strHeader3 = request.header("Accept");
            if (strHeader3 != null && strHeader3.indexOf("application/vnd.wap.mms-message") != -1) {
                return true;
            }
            Iterator<String> it = request.headers().values("Content-Type").iterator();
            while (it.hasNext()) {
                if (it.next().indexOf("application/vnd.wap.mms-message") != -1) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }
}
