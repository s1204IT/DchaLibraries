package sun.net.www.protocol.file;

import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import sun.net.www.ParseUtil;

public class Handler extends URLStreamHandler {
    private String getHost(URL url) {
        String host = url.getHost();
        if (host == null) {
            return "";
        }
        return host;
    }

    @Override
    protected void parseURL(URL url, String str, int i, int i2) {
        super.parseURL(url, str.replace(File.separatorChar, '/'), i, i2);
    }

    @Override
    public synchronized URLConnection openConnection(URL url) throws IOException {
        return openConnection(url, null);
    }

    @Override
    public synchronized URLConnection openConnection(URL url, Proxy proxy) throws IOException {
        URLConnection uRLConnectionOpenConnection;
        String str;
        String host = url.getHost();
        if (host == null || host.equals("") || host.equals("~") || host.equalsIgnoreCase("localhost")) {
            return createFileURLConnection(url, new File(ParseUtil.decode(url.getPath())));
        }
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(url.getFile());
            if (url.getRef() == null) {
                str = "";
            } else {
                str = "#" + url.getRef();
            }
            sb.append(str);
            URL url2 = new URL("ftp", host, sb.toString());
            if (proxy != null) {
                uRLConnectionOpenConnection = url2.openConnection(proxy);
            } else {
                uRLConnectionOpenConnection = url2.openConnection();
            }
        } catch (IOException e) {
            uRLConnectionOpenConnection = null;
        }
        if (uRLConnectionOpenConnection != null) {
            return uRLConnectionOpenConnection;
        }
        throw new IOException("Unable to connect to: " + url.toExternalForm());
    }

    protected URLConnection createFileURLConnection(URL url, File file) {
        return new FileURLConnection(url, file);
    }

    @Override
    protected boolean hostsEqual(URL url, URL url2) {
        String host = url.getHost();
        String host2 = url2.getHost();
        if ("localhost".equalsIgnoreCase(host) && (host2 == null || "".equals(host2))) {
            return true;
        }
        if ("localhost".equalsIgnoreCase(host2) && (host == null || "".equals(host))) {
            return true;
        }
        return super.hostsEqual(url, url2);
    }
}
