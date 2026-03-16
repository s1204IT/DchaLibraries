package libcore.net.url;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class FileHandler extends URLStreamHandler {
    @Override
    public URLConnection openConnection(URL url) throws IOException {
        return openConnection(url, null);
    }

    @Override
    public URLConnection openConnection(URL url, Proxy proxy) throws IOException {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        String host = url.getHost();
        if (host == null || host.isEmpty() || host.equalsIgnoreCase("localhost")) {
            return new FileURLConnection(url);
        }
        URL ftpURL = new URL("ftp", host, url.getFile());
        return proxy == null ? ftpURL.openConnection() : ftpURL.openConnection(proxy);
    }

    @Override
    protected void parseURL(URL url, String spec, int start, int end) {
        if (end >= start) {
            String parseString = "";
            if (start < end) {
                parseString = spec.substring(start, end).replace('\\', '/');
            }
            super.parseURL(url, parseString, 0, parseString.length());
        }
    }
}
