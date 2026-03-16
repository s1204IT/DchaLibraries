package libcore.net.url;

import dalvik.bytecode.Opcodes;
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketPermission;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FtpURLConnection extends URLConnection {
    private static final int FTP_DATAOPEN = 125;
    private static final int FTP_FILEOK = 250;
    private static final int FTP_LOGGEDIN = 230;
    private static final int FTP_NOTFOUND = 550;
    private static final int FTP_OK = 200;
    private static final int FTP_OPENDATA = 150;
    private static final int FTP_PASWD = 331;
    private static final int FTP_PORT = 21;
    private static final int FTP_TRANSFEROK = 226;
    private static final int FTP_USERREADY = 220;
    private ServerSocket acceptSocket;
    private Socket controlSocket;
    private InputStream ctrlInput;
    private OutputStream ctrlOutput;
    private Proxy currentProxy;
    private int dataPort;
    private Socket dataSocket;
    private String hostName;
    private InputStream inputStream;
    private String password;
    private Proxy proxy;
    private String replyCode;
    private URI uri;
    private String username;

    protected FtpURLConnection(URL url) {
        super(url);
        this.username = "anonymous";
        this.password = "";
        this.hostName = url.getHost();
        String parse = url.getUserInfo();
        if (parse != null) {
            int split = parse.indexOf(58);
            if (split >= 0) {
                this.username = parse.substring(0, split);
                this.password = parse.substring(split + 1);
            } else {
                this.username = parse;
            }
        }
        this.uri = null;
        try {
            this.uri = url.toURI();
        } catch (URISyntaxException e) {
        }
    }

    protected FtpURLConnection(URL url, Proxy proxy) {
        this(url);
        this.proxy = proxy;
    }

    private void cd() throws IOException {
        int idx = this.url.getFile().lastIndexOf(47);
        if (idx > 0) {
            String dir = this.url.getFile().substring(0, idx);
            write("CWD " + dir + "\r\n");
            int reply = getReply();
            if (reply != 250 && dir.length() > 0 && dir.charAt(0) == '/') {
                write("CWD " + dir.substring(1) + "\r\n");
                reply = getReply();
            }
            if (reply != 250) {
                throw new IOException("Unable to change directories");
            }
        }
    }

    @Override
    public void connect() throws IOException {
        List<Proxy> proxyList = null;
        if (this.proxy != null) {
            proxyList = new ArrayList<>(1);
            proxyList.add(this.proxy);
        } else {
            ProxySelector selector = ProxySelector.getDefault();
            if (selector != null) {
                proxyList = selector.select(this.uri);
            }
        }
        if (proxyList == null) {
            this.currentProxy = null;
            connectInternal();
            return;
        }
        ProxySelector selector2 = ProxySelector.getDefault();
        Iterator<Proxy> iter = proxyList.iterator();
        boolean connectOK = false;
        String failureReason = "";
        while (iter.hasNext() && !connectOK) {
            this.currentProxy = iter.next();
            try {
                connectInternal();
                connectOK = true;
            } catch (IOException ioe) {
                failureReason = ioe.getLocalizedMessage();
                if (selector2 != null && Proxy.NO_PROXY != this.currentProxy) {
                    selector2.connectFailed(this.uri, this.currentProxy.address(), ioe);
                }
            }
        }
        if (!connectOK) {
            throw new IOException("Unable to connect to server: " + failureReason);
        }
    }

    private void connectInternal() throws IOException {
        int port = this.url.getPort();
        int connectTimeout = getConnectTimeout();
        if (port <= 0) {
            port = 21;
        }
        if (this.currentProxy == null || Proxy.Type.HTTP == this.currentProxy.type()) {
            this.controlSocket = new Socket();
        } else {
            this.controlSocket = new Socket(this.currentProxy);
        }
        InetSocketAddress addr = new InetSocketAddress(this.hostName, port);
        this.controlSocket.connect(addr, connectTimeout);
        this.connected = true;
        this.ctrlOutput = this.controlSocket.getOutputStream();
        this.ctrlInput = this.controlSocket.getInputStream();
        login();
        setType();
        if (!getDoInput()) {
            cd();
        }
        try {
            this.acceptSocket = new ServerSocket(0);
            this.dataPort = this.acceptSocket.getLocalPort();
            port();
            if (connectTimeout == 0) {
            }
            this.acceptSocket.setSoTimeout(getConnectTimeout());
            if (getDoInput()) {
                getFile();
            } else {
                sendFile();
            }
            this.dataSocket = this.acceptSocket.accept();
            this.dataSocket.setSoTimeout(getReadTimeout());
            this.acceptSocket.close();
            if (getDoInput()) {
                this.inputStream = new FtpURLInputStream(new BufferedInputStream(this.dataSocket.getInputStream()), this.controlSocket);
            }
        } catch (InterruptedIOException e) {
            throw new IOException("Could not establish data connection");
        }
    }

    @Override
    public String getContentType() {
        String result = guessContentTypeFromName(this.url.getFile());
        if (result == null) {
            return "content/unknown";
        }
        return result;
    }

    private void getFile() throws IOException {
        String file = this.url.getFile();
        write("RETR " + file + "\r\n");
        int reply = getReply();
        if (reply == FTP_NOTFOUND && file.length() > 0 && file.charAt(0) == '/') {
            write("RETR " + file.substring(1) + "\r\n");
            reply = getReply();
        }
        if (reply != 150 && reply != 226) {
            throw new FileNotFoundException("Unable to retrieve file: " + reply);
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (!this.connected) {
            connect();
        }
        return this.inputStream;
    }

    @Override
    public Permission getPermission() throws IOException {
        int port = this.url.getPort();
        if (port <= 0) {
            port = 21;
        }
        return new SocketPermission(this.hostName + ":" + port, "connect, resolve");
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (!this.connected) {
            connect();
        }
        return this.dataSocket.getOutputStream();
    }

    private int getReply() throws IOException {
        byte[] code = new byte[3];
        for (int i = 0; i < code.length; i++) {
            int tmp = this.ctrlInput.read();
            if (tmp == -1) {
                throw new EOFException();
            }
            code[i] = (byte) tmp;
        }
        this.replyCode = new String(code, 0, code.length, StandardCharsets.ISO_8859_1);
        boolean multiline = false;
        if (this.ctrlInput.read() == 45) {
            multiline = true;
        }
        readLine();
        if (multiline) {
            while (readMultiLine()) {
            }
        }
        try {
            return Integer.parseInt(this.replyCode);
        } catch (NumberFormatException e) {
            throw ((IOException) new IOException("reply code is invalid").initCause(e));
        }
    }

    private void login() throws IOException {
        if (getReply() != 220) {
            throw new IOException("Unable to connect to server: " + this.url.getHost());
        }
        write("USER " + this.username + "\r\n");
        int reply = getReply();
        if (reply != FTP_PASWD && reply != FTP_LOGGEDIN) {
            throw new IOException("Unable to log in to server (USER): " + this.url.getHost());
        }
        if (reply == FTP_PASWD) {
            write("PASS " + this.password + "\r\n");
            int reply2 = getReply();
            if (reply2 != 200 && reply2 != 220 && reply2 != FTP_LOGGEDIN) {
                throw new IOException("Unable to log in to server (PASS): " + this.url.getHost());
            }
        }
    }

    private void port() throws IOException {
        write("PORT " + this.controlSocket.getLocalAddress().getHostAddress().replace('.', ',') + ',' + (this.dataPort >> 8) + ',' + (this.dataPort & Opcodes.OP_CONST_CLASS_JUMBO) + "\r\n");
        if (getReply() != 200) {
            throw new IOException("Unable to configure data port");
        }
    }

    private String readLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            int c = this.ctrlInput.read();
            if (c != 10) {
                sb.append((char) c);
            } else {
                return sb.toString();
            }
        }
    }

    private boolean readMultiLine() throws IOException {
        String line = readLine();
        return (line.length() >= 4 && line.substring(0, 3).equals(this.replyCode) && line.charAt(3) == ' ') ? false : true;
    }

    private void sendFile() throws IOException {
        write("STOR " + this.url.getFile().substring(this.url.getFile().lastIndexOf(47) + 1, this.url.getFile().length()) + "\r\n");
        int reply = getReply();
        if (reply != 150 && reply != 200 && reply != 125) {
            throw new IOException("Unable to store file");
        }
    }

    @Override
    public void setDoInput(boolean newValue) {
        if (this.connected) {
            throw new IllegalAccessError();
        }
        this.doInput = newValue;
        this.doOutput = !newValue;
    }

    @Override
    public void setDoOutput(boolean newValue) {
        if (this.connected) {
            throw new IllegalAccessError();
        }
        this.doOutput = newValue;
        this.doInput = !newValue;
    }

    private void setType() throws IOException {
        write("TYPE I\r\n");
        if (getReply() != 200) {
            throw new IOException("Unable to set transfer type");
        }
    }

    public static void validateCommand(String command) throws IOException {
        int terminatorIdx = command.length() - 2;
        if (terminatorIdx < 0 || command.charAt(terminatorIdx) != '\r' || command.charAt(terminatorIdx + 1) != '\n') {
            throw new IOException("Command terminated incorrectly");
        }
        if (command.indexOf(13) < terminatorIdx || command.indexOf(10) < terminatorIdx) {
            throw new IOException("Invalid character in command");
        }
    }

    private void write(String command) throws IOException {
        validateCommand(command);
        this.ctrlOutput.write(command.getBytes(StandardCharsets.ISO_8859_1));
    }
}
