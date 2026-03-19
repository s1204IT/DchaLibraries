package org.apache.http.client.methods;

import java.net.URI;

@Deprecated
public class HttpPost extends HttpEntityEnclosingRequestBase {
    public static final String METHOD_NAME = "POST";

    public HttpPost() {
    }

    public HttpPost(URI uri) {
        setURI(uri);
    }

    public HttpPost(String str) {
        try {
            String str2 = (String) Class.forName("android.os.Build").getDeclaredField("TYPE").get(null);
            if ("eng".equals(str2) || "userdebug".equals(str2)) {
                System.out.println("httppost:" + str);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        setURI(URI.create(str));
    }

    @Override
    public String getMethod() {
        return METHOD_NAME;
    }
}
