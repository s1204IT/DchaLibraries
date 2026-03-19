package sun.net.www.protocol.ftp;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketPermission;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.StringTokenizer;
import libcore.net.NetworkSecurityPolicy;
import sun.net.ProgressMonitor;
import sun.net.ProgressSource;
import sun.net.ftp.FtpClient;
import sun.net.ftp.FtpLoginException;
import sun.net.ftp.FtpProtocolException;
import sun.net.www.MessageHeader;
import sun.net.www.MeteredStream;
import sun.net.www.ParseUtil;
import sun.net.www.URLConnection;
import sun.security.action.GetPropertyAction;
import sun.security.util.SecurityConstants;

public class FtpURLConnection extends URLConnection {
    static final int ASCII = 1;
    static final int BIN = 2;
    static final int DIR = 3;
    static final int NONE = 0;
    private int connectTimeout;
    String filename;
    FtpClient ftp;
    String fullpath;
    String host;
    private Proxy instProxy;
    InputStream is;
    OutputStream os;
    String password;
    String pathname;
    Permission permission;
    int port;
    private int readTimeout;
    int type;
    String user;

    protected class FtpInputStream extends FilterInputStream {
        FtpClient ftp;

        FtpInputStream(FtpClient ftpClient, InputStream inputStream) {
            super(new BufferedInputStream(inputStream));
            this.ftp = ftpClient;
        }

        @Override
        public void close() throws IOException {
            super.close();
            if (this.ftp != null) {
                this.ftp.close();
            }
        }
    }

    protected class FtpOutputStream extends FilterOutputStream {
        FtpClient ftp;

        FtpOutputStream(FtpClient ftpClient, OutputStream outputStream) {
            super(outputStream);
            this.ftp = ftpClient;
        }

        @Override
        public void close() throws Throwable {
            super.close();
            if (this.ftp != null) {
                this.ftp.close();
            }
        }
    }

    public FtpURLConnection(URL url) throws IOException {
        this(url, null);
    }

    FtpURLConnection(URL url, Proxy proxy) throws IOException {
        String str;
        super(url);
        this.is = null;
        this.os = null;
        this.ftp = null;
        this.type = 0;
        this.connectTimeout = -1;
        this.readTimeout = -1;
        this.instProxy = proxy;
        this.host = url.getHost();
        this.port = url.getPort();
        String userInfo = url.getUserInfo();
        if (!NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Cleartext traffic not permitted: ");
            sb.append(url.getProtocol());
            sb.append("://");
            sb.append(this.host);
            if (url.getPort() >= 0) {
                str = ":" + url.getPort();
            } else {
                str = "";
            }
            sb.append(str);
            throw new IOException(sb.toString());
        }
        if (userInfo != null) {
            int iIndexOf = userInfo.indexOf(58);
            if (iIndexOf == -1) {
                this.user = ParseUtil.decode(userInfo);
                this.password = null;
            } else {
                this.user = ParseUtil.decode(userInfo.substring(0, iIndexOf));
                this.password = ParseUtil.decode(userInfo.substring(iIndexOf + 1));
            }
        }
    }

    private void setTimeouts() {
        if (this.ftp != null) {
            if (this.connectTimeout >= 0) {
                this.ftp.setConnectTimeout(this.connectTimeout);
            }
            if (this.readTimeout >= 0) {
                this.ftp.setReadTimeout(this.readTimeout);
            }
        }
    }

    @Override
    public synchronized void connect() throws IOException {
        Proxy next;
        if (this.connected) {
            return;
        }
        char[] charArray = null;
        if (this.instProxy == null) {
            ProxySelector proxySelector = (ProxySelector) AccessController.doPrivileged(new PrivilegedAction<ProxySelector>() {
                @Override
                public ProxySelector run() {
                    return ProxySelector.getDefault();
                }
            });
            if (proxySelector != null) {
                URI uri = ParseUtil.toURI(this.url);
                Iterator<Proxy> it = proxySelector.select(uri).iterator();
                next = null;
                while (it.hasNext() && (next = it.next()) != null && next != Proxy.NO_PROXY && next.type() != Proxy.Type.SOCKS) {
                    if (next.type() != Proxy.Type.HTTP || !(next.address() instanceof InetSocketAddress)) {
                        proxySelector.connectFailed(uri, next.address(), new IOException("Wrong proxy type"));
                    } else {
                        proxySelector.connectFailed(uri, next.address(), new IOException("FTP connections over HTTP proxy not supported"));
                    }
                }
            } else {
                next = null;
            }
        } else {
            next = this.instProxy;
        }
        if (this.user == null) {
            this.user = "anonymous";
            this.password = (String) AccessController.doPrivileged(new GetPropertyAction("ftp.protocol.user", "Java" + ((String) AccessController.doPrivileged(new GetPropertyAction("java.version"))) + "@"));
        }
        try {
            try {
                this.ftp = FtpClient.create();
                if (next != null) {
                    this.ftp.setProxy(next);
                }
                setTimeouts();
                if (this.port != -1) {
                    this.ftp.connect(new InetSocketAddress(this.host, this.port));
                } else {
                    this.ftp.connect(new InetSocketAddress(this.host, FtpClient.defaultPort()));
                }
                try {
                    FtpClient ftpClient = this.ftp;
                    String str = this.user;
                    if (this.password != null) {
                        charArray = this.password.toCharArray();
                    }
                    ftpClient.login(str, charArray);
                    this.connected = true;
                } catch (FtpProtocolException e) {
                    this.ftp.close();
                    throw new FtpLoginException("Invalid username/password");
                }
            } catch (UnknownHostException e2) {
                throw e2;
            }
        } catch (FtpProtocolException e3) {
            throw new IOException(e3);
        }
    }

    private void decodePath(String str) {
        int iIndexOf = str.indexOf(";type=");
        if (iIndexOf >= 0) {
            String strSubstring = str.substring(iIndexOf + 6, str.length());
            if ("i".equalsIgnoreCase(strSubstring)) {
                this.type = 2;
            }
            if ("a".equalsIgnoreCase(strSubstring)) {
                this.type = 1;
            }
            if ("d".equalsIgnoreCase(strSubstring)) {
                this.type = 3;
            }
            str = str.substring(0, iIndexOf);
        }
        if (str != null && str.length() > 1 && str.charAt(0) == '/') {
            str = str.substring(1);
        }
        if (str == null || str.length() == 0) {
            str = "./";
        }
        if (str.endsWith("/")) {
            this.pathname = str.substring(0, str.length() - 1);
            this.filename = null;
        } else {
            int iLastIndexOf = str.lastIndexOf(47);
            if (iLastIndexOf > 0) {
                this.filename = str.substring(iLastIndexOf + 1, str.length());
                this.filename = ParseUtil.decode(this.filename);
                this.pathname = str.substring(0, iLastIndexOf);
            } else {
                this.filename = ParseUtil.decode(str);
                this.pathname = null;
            }
        }
        if (this.pathname != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(this.pathname);
            sb.append("/");
            sb.append(this.filename != null ? this.filename : "");
            this.fullpath = sb.toString();
            return;
        }
        this.fullpath = this.filename;
    }

    private void cd(String str) throws FtpProtocolException, IOException {
        if (str == null || str.isEmpty()) {
            return;
        }
        if (str.indexOf(47) == -1) {
            this.ftp.changeDirectory(ParseUtil.decode(str));
            return;
        }
        StringTokenizer stringTokenizer = new StringTokenizer(str, "/");
        while (stringTokenizer.hasMoreTokens()) {
            this.ftp.changeDirectory(ParseUtil.decode(stringTokenizer.nextToken()));
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        ProgressSource progressSource;
        if (!this.connected) {
            connect();
        }
        if (this.os != null) {
            throw new IOException("Already opened for output");
        }
        if (this.is != null) {
            return this.is;
        }
        MessageHeader messageHeader = new MessageHeader();
        try {
            decodePath(this.url.getPath());
            if (this.filename == null || this.type == 3) {
                this.ftp.setAsciiType();
                cd(this.pathname);
                if (this.filename == null) {
                    this.is = new FtpInputStream(this.ftp, this.ftp.list(null));
                } else {
                    this.is = new FtpInputStream(this.ftp, this.ftp.nameList(this.filename));
                }
            } else {
                if (this.type == 1) {
                    this.ftp.setAsciiType();
                } else {
                    this.ftp.setBinaryType();
                }
                cd(this.pathname);
                this.is = new FtpInputStream(this.ftp, this.ftp.getFileStream(this.filename));
            }
            try {
                long lastTransferSize = this.ftp.getLastTransferSize();
                messageHeader.add("content-length", Long.toString(lastTransferSize));
                if (lastTransferSize > 0) {
                    if (ProgressMonitor.getDefault().shouldMeterInput(this.url, "GET")) {
                        progressSource = new ProgressSource(this.url, "GET", lastTransferSize);
                        progressSource.beginTracking();
                    } else {
                        progressSource = null;
                    }
                    this.is = new MeteredStream(this.is, progressSource, lastTransferSize);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            messageHeader.add("access-type", "file");
            String strGuessContentTypeFromName = guessContentTypeFromName(this.fullpath);
            if (strGuessContentTypeFromName == null && this.is.markSupported()) {
                strGuessContentTypeFromName = guessContentTypeFromStream(this.is);
            }
            if (strGuessContentTypeFromName != null) {
                messageHeader.add("content-type", strGuessContentTypeFromName);
            }
        } catch (FileNotFoundException e2) {
            try {
                cd(this.fullpath);
                this.ftp.setAsciiType();
                this.is = new FtpInputStream(this.ftp, this.ftp.list(null));
                messageHeader.add("content-type", "text/plain");
                messageHeader.add("access-type", "directory");
            } catch (IOException e3) {
                throw new FileNotFoundException(this.fullpath);
            } catch (FtpProtocolException e4) {
                throw new FileNotFoundException(this.fullpath);
            }
        } catch (FtpProtocolException e5) {
            throw new IOException(e5);
        }
        setProperties(messageHeader);
        return this.is;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (!this.connected) {
            connect();
        }
        if (this.is != null) {
            throw new IOException("Already opened for input");
        }
        if (this.os != null) {
            return this.os;
        }
        decodePath(this.url.getPath());
        if (this.filename == null || this.filename.length() == 0) {
            throw new IOException("illegal filename for a PUT");
        }
        try {
            if (this.pathname != null) {
                cd(this.pathname);
            }
            if (this.type == 1) {
                this.ftp.setAsciiType();
            } else {
                this.ftp.setBinaryType();
            }
            this.os = new FtpOutputStream(this.ftp, this.ftp.putFileStream(this.filename, false));
            return this.os;
        } catch (FtpProtocolException e) {
            throw new IOException(e);
        }
    }

    String guessContentTypeFromFilename(String str) {
        return guessContentTypeFromName(str);
    }

    @Override
    public Permission getPermission() {
        if (this.permission == null) {
            int port = this.url.getPort();
            if (port < 0) {
                port = FtpClient.defaultPort();
            }
            this.permission = new SocketPermission(this.host + ":" + port, SecurityConstants.SOCKET_CONNECT_ACTION);
        }
        return this.permission;
    }

    @Override
    public void setRequestProperty(String str, String str2) {
        super.setRequestProperty(str, str2);
        if ("type".equals(str)) {
            if ("i".equalsIgnoreCase(str2)) {
                this.type = 2;
                return;
            }
            if ("a".equalsIgnoreCase(str2)) {
                this.type = 1;
                return;
            }
            if ("d".equalsIgnoreCase(str2)) {
                this.type = 3;
                return;
            }
            throw new IllegalArgumentException("Value of '" + str + "' request property was '" + str2 + "' when it must be either 'i', 'a' or 'd'");
        }
    }

    @Override
    public String getRequestProperty(String str) {
        String str2;
        String requestProperty = super.getRequestProperty(str);
        if (requestProperty == null && "type".equals(str)) {
            if (this.type == 1) {
                str2 = "a";
            } else {
                str2 = this.type == 3 ? "d" : "i";
            }
            return str2;
        }
        return requestProperty;
    }

    @Override
    public void setConnectTimeout(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("timeouts can't be negative");
        }
        this.connectTimeout = i;
    }

    @Override
    public int getConnectTimeout() {
        if (this.connectTimeout < 0) {
            return 0;
        }
        return this.connectTimeout;
    }

    @Override
    public void setReadTimeout(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("timeouts can't be negative");
        }
        this.readTimeout = i;
    }

    @Override
    public int getReadTimeout() {
        if (this.readTimeout < 0) {
            return 0;
        }
        return this.readTimeout;
    }
}
