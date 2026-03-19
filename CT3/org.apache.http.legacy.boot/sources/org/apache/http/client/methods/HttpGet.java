package org.apache.http.client.methods;

import java.net.URI;

@Deprecated
public class HttpGet extends HttpRequestBase {
    public static final String METHOD_NAME = "GET";

    public HttpGet() {
    }

    public HttpGet(URI uri) {
        setURI(uri);
    }

    public HttpGet(String uri) {
        String encodeUri = uri.trim();
        String encodeUri2 = encodeUri.replaceAll(" ", "%20");
        try {
            Class<?> classType = Class.forName("android.os.Build");
            String value = (String) classType.getDeclaredField("TYPE").get(null);
            if ("eng".equals(value)) {
                System.out.println("httpget:" + uri);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e2) {
            e2.printStackTrace();
        } catch (NoSuchFieldException e3) {
            e3.printStackTrace();
        }
        setURI(URI.create(encodeUri2));
    }

    @Override
    public String getMethod() {
        return METHOD_NAME;
    }
}
