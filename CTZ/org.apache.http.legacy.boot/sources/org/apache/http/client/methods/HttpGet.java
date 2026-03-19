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

    public HttpGet(String str) {
        setURI(URI.create(str));
        try {
            String str2 = (String) Class.forName("android.os.Build").getDeclaredField("TYPE").get(null);
            if ("eng".equals(str2) || "userdebug".equals(str2)) {
                System.out.println("httpget:" + str);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getMethod() {
        return METHOD_NAME;
    }
}
