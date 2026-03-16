package libcore.net.url;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class JarHandler extends URLStreamHandler {
    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        return new JarURLConnectionImpl(u);
    }

    @Override
    protected void parseURL(URL url, String spec, int start, int limit) {
        String spec2;
        String file;
        String file2 = url.getFile();
        if (file2 == null) {
            file2 = "";
        }
        if (limit > start) {
            spec2 = spec.substring(start, limit);
        } else {
            spec2 = "";
        }
        if (spec2.indexOf("!/") == -1 && file2.indexOf("!/") == -1) {
            throw new NullPointerException("Cannot find \"!/\"");
        }
        if (file2.isEmpty()) {
            file = spec2;
        } else if (spec2.charAt(0) == '/') {
            file = file2.substring(0, file2.indexOf(33) + 1) + spec2;
        } else {
            int idx = file2.indexOf(33);
            String tmpFile = file2.substring(idx + 1, file2.lastIndexOf(47) + 1) + spec2;
            file = file2.substring(0, idx + 1) + UrlUtils.canonicalizePath(tmpFile, true);
        }
        try {
            new URL(file);
            setURL(url, "jar", "", -1, null, null, file, null, null);
        } catch (MalformedURLException e) {
            throw new NullPointerException(e.toString());
        }
    }

    @Override
    protected String toExternalForm(URL url) {
        StringBuilder sb = new StringBuilder();
        sb.append("jar:");
        sb.append(url.getFile());
        String ref = url.getRef();
        if (ref != null) {
            sb.append(ref);
        }
        return sb.toString();
    }
}
