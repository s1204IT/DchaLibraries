package sun.net.www;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class URLConnection extends java.net.URLConnection {
    private static HashMap<String, Void> proxiedHosts = new HashMap<>();
    private int contentLength;
    private String contentType;
    protected MessageHeader properties;

    public URLConnection(URL url) {
        super(url);
        this.contentLength = -1;
        this.properties = new MessageHeader();
    }

    public MessageHeader getProperties() {
        return this.properties;
    }

    public void setProperties(MessageHeader messageHeader) {
        this.properties = messageHeader;
    }

    @Override
    public void setRequestProperty(String str, String str2) {
        if (this.connected) {
            throw new IllegalAccessError("Already connected");
        }
        if (str == null) {
            throw new NullPointerException("key cannot be null");
        }
        this.properties.set(str, str2);
    }

    @Override
    public void addRequestProperty(String str, String str2) {
        if (this.connected) {
            throw new IllegalStateException("Already connected");
        }
        if (str == null) {
            throw new NullPointerException("key is null");
        }
    }

    @Override
    public String getRequestProperty(String str) {
        if (this.connected) {
            throw new IllegalStateException("Already connected");
        }
        return null;
    }

    @Override
    public Map<String, List<String>> getRequestProperties() {
        if (this.connected) {
            throw new IllegalStateException("Already connected");
        }
        return Collections.emptyMap();
    }

    @Override
    public String getHeaderField(String str) {
        try {
            getInputStream();
            if (this.properties == null) {
                return null;
            }
            return this.properties.findValue(str);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getHeaderFieldKey(int i) {
        try {
            getInputStream();
            MessageHeader messageHeader = this.properties;
            if (messageHeader == null) {
                return null;
            }
            return messageHeader.getKey(i);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getHeaderField(int i) {
        try {
            getInputStream();
            MessageHeader messageHeader = this.properties;
            if (messageHeader == null) {
                return null;
            }
            return messageHeader.getValue(i);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getContentType() {
        if (this.contentType == null) {
            this.contentType = getHeaderField("content-type");
        }
        if (this.contentType == null) {
            String strFindValue = null;
            try {
                strFindValue = guessContentTypeFromStream(getInputStream());
            } catch (IOException e) {
            }
            String strFindValue2 = this.properties.findValue("content-encoding");
            if (strFindValue == null && (strFindValue = this.properties.findValue("content-type")) == null) {
                if (this.url.getFile().endsWith("/")) {
                    strFindValue = "text/html";
                } else {
                    strFindValue = guessContentTypeFromName(this.url.getFile());
                }
            }
            if (strFindValue == null || (strFindValue2 != null && !strFindValue2.equalsIgnoreCase("7bit") && !strFindValue2.equalsIgnoreCase("8bit") && !strFindValue2.equalsIgnoreCase("binary"))) {
                strFindValue = "content/unknown";
            }
            setContentType(strFindValue);
        }
        return this.contentType;
    }

    public void setContentType(String str) {
        this.contentType = str;
        this.properties.set("content-type", str);
    }

    @Override
    public int getContentLength() {
        try {
            getInputStream();
            int i = this.contentLength;
            if (i < 0) {
                try {
                    int i2 = Integer.parseInt(this.properties.findValue("content-length"));
                    try {
                        setContentLength(i2);
                        return i2;
                    } catch (Exception e) {
                        return i2;
                    }
                } catch (Exception e2) {
                    return i;
                }
            }
            return i;
        } catch (Exception e3) {
            return -1;
        }
    }

    protected void setContentLength(int i) {
        this.contentLength = i;
        this.properties.set("content-length", String.valueOf(i));
    }

    public boolean canCache() {
        return this.url.getFile().indexOf(63) < 0;
    }

    public void close() {
        this.url = null;
    }

    public static synchronized void setProxiedHost(String str) {
        proxiedHosts.put(str.toLowerCase(), null);
    }

    public static synchronized boolean isProxiedHost(String str) {
        return proxiedHosts.containsKey(str.toLowerCase());
    }
}
