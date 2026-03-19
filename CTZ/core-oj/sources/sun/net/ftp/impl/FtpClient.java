package sun.net.ftp.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectStreamConstants;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import sun.net.TelnetInputStream;
import sun.net.TelnetOutputStream;
import sun.net.ftp.FtpClient;
import sun.net.ftp.FtpDirEntry;
import sun.net.ftp.FtpDirParser;
import sun.net.ftp.FtpProtocolException;
import sun.net.ftp.FtpReplyCode;
import sun.security.util.DerValue;
import sun.util.logging.PlatformLogger;

public class FtpClient extends sun.net.ftp.FtpClient {
    private static String[] MDTMformats;
    private static SimpleDateFormat[] dateFormats;
    private static int defaultConnectTimeout;
    private static int defaultSoTimeout;
    private static String encoding;
    private static Pattern epsvPat;
    private static Pattern pasvPat;
    private static Pattern[] patterns;
    private static Pattern transPat;
    private InputStream in;
    private String lastFileName;
    private FtpDirParser mlsxParser;
    private Socket oldSocket;
    private PrintStream out;
    private FtpDirParser parser;
    private Proxy proxy;
    private Socket server;
    private InetSocketAddress serverAddr;
    private SSLSocketFactory sslFact;
    private String welcomeMsg;
    private static final PlatformLogger logger = PlatformLogger.getLogger("sun.net.ftp.FtpClient");
    private static String[] patStrings = {"([\\-ld](?:[r\\-][w\\-][x\\-]){3})\\s*\\d+ (\\w+)\\s*(\\w+)\\s*(\\d+)\\s*([A-Z][a-z][a-z]\\s*\\d+)\\s*(\\d\\d:\\d\\d)\\s*(\\p{Print}*)", "([\\-ld](?:[r\\-][w\\-][x\\-]){3})\\s*\\d+ (\\w+)\\s*(\\w+)\\s*(\\d+)\\s*([A-Z][a-z][a-z]\\s*\\d+)\\s*(\\d{4})\\s*(\\p{Print}*)", "(\\d{2}/\\d{2}/\\d{4})\\s*(\\d{2}:\\d{2}[ap])\\s*((?:[0-9,]+)|(?:<DIR>))\\s*(\\p{Graph}*)", "(\\d{2}-\\d{2}-\\d{2})\\s*(\\d{2}:\\d{2}[AP]M)\\s*((?:[0-9,]+)|(?:<DIR>))\\s*(\\p{Graph}*)"};
    private static int[][] patternGroups = {new int[]{7, 4, 5, 6, 0, 1, 2, 3}, new int[]{7, 4, 5, 0, 6, 1, 2, 3}, new int[]{4, 3, 1, 2, 0, 0, 0, 0}, new int[]{4, 3, 1, 2, 0, 0, 0, 0}};
    private static Pattern linkp = Pattern.compile("(\\p{Print}+) \\-\\> (\\p{Print}+)$");
    private int readTimeout = -1;
    private int connectTimeout = -1;
    private boolean replyPending = false;
    private boolean loggedIn = false;
    private boolean useCrypto = false;
    private Vector<String> serverResponse = new Vector<>(1);
    private FtpReplyCode lastReplyCode = null;
    private final boolean passiveMode = true;
    private FtpClient.TransferType type = FtpClient.TransferType.BINARY;
    private long restartOffset = 0;
    private long lastTransSize = -1;
    private DateFormat df = DateFormat.getDateInstance(2, Locale.US);

    static {
        encoding = "ISO8859_1";
        final int[] iArr = {0, 0};
        final String[] strArr = {null};
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                iArr[0] = Integer.getInteger("sun.net.client.defaultReadTimeout", 0).intValue();
                iArr[1] = Integer.getInteger("sun.net.client.defaultConnectTimeout", 0).intValue();
                strArr[0] = System.getProperty("file.encoding", "ISO8859_1");
                return null;
            }
        });
        if (iArr[0] == 0) {
            defaultSoTimeout = -1;
        } else {
            defaultSoTimeout = iArr[0];
        }
        if (iArr[1] == 0) {
            defaultConnectTimeout = -1;
        } else {
            defaultConnectTimeout = iArr[1];
        }
        encoding = strArr[0];
        try {
            if (!isASCIISuperset(encoding)) {
                encoding = "ISO8859_1";
            }
        } catch (Exception e) {
            encoding = "ISO8859_1";
        }
        patterns = new Pattern[patStrings.length];
        for (int i = 0; i < patStrings.length; i++) {
            patterns[i] = Pattern.compile(patStrings[i]);
        }
        transPat = null;
        epsvPat = null;
        pasvPat = null;
        MDTMformats = new String[]{"yyyyMMddHHmmss.SSS", "yyyyMMddHHmmss"};
        dateFormats = new SimpleDateFormat[MDTMformats.length];
        for (int i2 = 0; i2 < MDTMformats.length; i2++) {
            dateFormats[i2] = new SimpleDateFormat(MDTMformats[i2]);
            dateFormats[i2].setTimeZone(TimeZone.getTimeZone("GMT"));
        }
    }

    private static boolean isASCIISuperset(String str) throws Exception {
        return Arrays.equals("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_.!~*'();/?:@&=+$,".getBytes(str), new byte[]{48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, ObjectStreamConstants.TC_REFERENCE, ObjectStreamConstants.TC_CLASSDESC, ObjectStreamConstants.TC_OBJECT, ObjectStreamConstants.TC_STRING, ObjectStreamConstants.TC_ARRAY, ObjectStreamConstants.TC_CLASS, ObjectStreamConstants.TC_BLOCKDATA, ObjectStreamConstants.TC_ENDBLOCKDATA, ObjectStreamConstants.TC_RESET, ObjectStreamConstants.TC_BLOCKDATALONG, 45, 95, 46, 33, 126, 42, 39, 40, 41, 59, 47, 63, 58, DerValue.TAG_APPLICATION, 38, 61, 43, 36, 44});
    }

    private class DefaultParser implements FtpDirParser {
        private DefaultParser() {
        }

        @Override
        public FtpDirEntry parseLine(String str) {
            int i;
            Date time;
            boolean z;
            FtpDirEntry.Type type;
            boolean zStartsWith;
            Calendar calendar = Calendar.getInstance();
            char c = 1;
            int i2 = calendar.get(1);
            int i3 = 0;
            boolean z2 = false;
            String strGroup = null;
            String str2 = null;
            String strGroup2 = null;
            String str3 = null;
            String strGroup3 = null;
            String strGroup4 = null;
            String strGroup5 = null;
            while (true) {
                i = 3;
                if (i3 >= FtpClient.patterns.length) {
                    break;
                }
                Matcher matcher = FtpClient.patterns[i3].matcher(str);
                if (matcher.find()) {
                    strGroup = matcher.group(FtpClient.patternGroups[i3][0]);
                    String strGroup6 = matcher.group(FtpClient.patternGroups[i3][c]);
                    String strGroup7 = matcher.group(FtpClient.patternGroups[i3][2]);
                    if (FtpClient.patternGroups[i3][4] <= 0) {
                        if (FtpClient.patternGroups[i3][3] > 0) {
                            strGroup7 = strGroup7 + ", " + String.valueOf(i2);
                        }
                    } else {
                        strGroup7 = strGroup7 + ", " + matcher.group(FtpClient.patternGroups[i3][4]);
                    }
                    if (FtpClient.patternGroups[i3][3] > 0) {
                        strGroup2 = matcher.group(FtpClient.patternGroups[i3][3]);
                    }
                    if (FtpClient.patternGroups[i3][5] > 0) {
                        strGroup5 = matcher.group(FtpClient.patternGroups[i3][5]);
                        zStartsWith = strGroup5.startsWith("d");
                    } else {
                        zStartsWith = z2;
                    }
                    if (FtpClient.patternGroups[i3][6] > 0) {
                        strGroup3 = matcher.group(FtpClient.patternGroups[i3][6]);
                    }
                    if (FtpClient.patternGroups[i3][7] > 0) {
                        strGroup4 = matcher.group(FtpClient.patternGroups[i3][7]);
                    }
                    if ("<DIR>".equals(strGroup6)) {
                        str2 = strGroup7;
                        str3 = null;
                        z2 = true;
                    } else {
                        z2 = zStartsWith;
                        String str4 = strGroup7;
                        str3 = strGroup6;
                        str2 = str4;
                    }
                }
                i3++;
                c = 1;
            }
            if (strGroup != null) {
                try {
                    time = FtpClient.this.df.parse(str2);
                } catch (Exception e) {
                    time = null;
                }
                if (time != null && strGroup2 != null) {
                    int iIndexOf = strGroup2.indexOf(":");
                    calendar.setTime(time);
                    calendar.set(10, Integer.parseInt(strGroup2.substring(0, iIndexOf)));
                    calendar.set(12, Integer.parseInt(strGroup2.substring(iIndexOf + 1)));
                    time = calendar.getTime();
                }
                Matcher matcher2 = FtpClient.linkp.matcher(strGroup);
                if (matcher2.find()) {
                    z = true;
                    strGroup = matcher2.group(1);
                } else {
                    z = true;
                }
                boolean[][] zArr = (boolean[][]) Array.newInstance((Class<?>) boolean.class, 3, 3);
                int i4 = 0;
                while (i4 < i) {
                    int i5 = 0;
                    while (i5 < i) {
                        zArr[i4][i5] = strGroup5.charAt((i4 * 3) + i5) != '-' ? z : false;
                        i5++;
                        i = 3;
                    }
                    i4++;
                    i = 3;
                }
                FtpDirEntry ftpDirEntry = new FtpDirEntry(strGroup);
                ftpDirEntry.setUser(strGroup3).setGroup(strGroup4);
                ftpDirEntry.setSize(Long.parseLong(str3)).setLastModified(time);
                ftpDirEntry.setPermissions(zArr);
                if (z2) {
                    type = FtpDirEntry.Type.DIR;
                } else {
                    type = str.charAt(0) == 'l' ? FtpDirEntry.Type.LINK : FtpDirEntry.Type.FILE;
                }
                ftpDirEntry.setType(type);
                return ftpDirEntry;
            }
            return null;
        }
    }

    private class MLSxParser implements FtpDirParser {
        private SimpleDateFormat df;

        private MLSxParser() {
            this.df = new SimpleDateFormat("yyyyMMddhhmmss");
        }

        @Override
        public FtpDirEntry parseLine(String str) {
            String strTrim;
            String strSubstring;
            Date date;
            Date date2;
            String strSubstring2;
            int iLastIndexOf = str.lastIndexOf(";");
            if (iLastIndexOf > 0) {
                strTrim = str.substring(iLastIndexOf + 1).trim();
                strSubstring = str.substring(0, iLastIndexOf);
            } else {
                strTrim = str.trim();
                strSubstring = "";
            }
            FtpDirEntry ftpDirEntry = new FtpDirEntry(strTrim);
            while (!strSubstring.isEmpty()) {
                int iIndexOf = strSubstring.indexOf(";");
                if (iIndexOf > 0) {
                    String strSubstring3 = strSubstring.substring(0, iIndexOf);
                    strSubstring2 = strSubstring.substring(iIndexOf + 1);
                    strSubstring = strSubstring3;
                } else {
                    strSubstring2 = "";
                }
                int iIndexOf2 = strSubstring.indexOf("=");
                if (iIndexOf2 > 0) {
                    ftpDirEntry.addFact(strSubstring.substring(0, iIndexOf2), strSubstring.substring(iIndexOf2 + 1));
                }
                strSubstring = strSubstring2;
            }
            String fact = ftpDirEntry.getFact("Size");
            if (fact != null) {
                ftpDirEntry.setSize(Long.parseLong(fact));
            }
            String fact2 = ftpDirEntry.getFact("Modify");
            if (fact2 != null) {
                try {
                    date = this.df.parse(fact2);
                } catch (ParseException e) {
                    date = null;
                }
                if (date != null) {
                    ftpDirEntry.setLastModified(date);
                }
            }
            String fact3 = ftpDirEntry.getFact("Create");
            if (fact3 != null) {
                try {
                    date2 = this.df.parse(fact3);
                } catch (ParseException e2) {
                    date2 = null;
                }
                if (date2 != null) {
                    ftpDirEntry.setCreated(date2);
                }
            }
            String fact4 = ftpDirEntry.getFact("Type");
            if (fact4 != null) {
                if (fact4.equalsIgnoreCase("file")) {
                    ftpDirEntry.setType(FtpDirEntry.Type.FILE);
                }
                if (fact4.equalsIgnoreCase("dir")) {
                    ftpDirEntry.setType(FtpDirEntry.Type.DIR);
                }
                if (fact4.equalsIgnoreCase("cdir")) {
                    ftpDirEntry.setType(FtpDirEntry.Type.CDIR);
                }
                if (fact4.equalsIgnoreCase("pdir")) {
                    ftpDirEntry.setType(FtpDirEntry.Type.PDIR);
                }
            }
            return ftpDirEntry;
        }
    }

    private void getTransferSize() {
        this.lastTransSize = -1L;
        String lastResponseString = getLastResponseString();
        if (transPat == null) {
            transPat = Pattern.compile("150 Opening .*\\((\\d+) bytes\\).");
        }
        Matcher matcher = transPat.matcher(lastResponseString);
        if (matcher.find()) {
            this.lastTransSize = Long.parseLong(matcher.group(1));
        }
    }

    private void getTransferName() {
        this.lastFileName = null;
        String lastResponseString = getLastResponseString();
        int iIndexOf = lastResponseString.indexOf("unique file name:");
        int iLastIndexOf = lastResponseString.lastIndexOf(41);
        if (iIndexOf >= 0) {
            this.lastFileName = lastResponseString.substring(iIndexOf + 17, iLastIndexOf);
        }
    }

    private int readServerResponse() throws IOException {
        int i;
        StringBuffer stringBuffer = new StringBuffer(32);
        this.serverResponse.setSize(0);
        int i2 = -1;
        while (true) {
            int i3 = this.in.read();
            if (i3 != -1) {
                if (i3 == 13 && (i3 = this.in.read()) != 10) {
                    stringBuffer.append('\r');
                }
                stringBuffer.append((char) i3);
                if (i3 != 10) {
                    continue;
                }
            }
            String string = stringBuffer.toString();
            stringBuffer.setLength(0);
            if (logger.isLoggable(PlatformLogger.Level.FINEST)) {
                logger.finest("Server [" + ((Object) this.serverAddr) + "] --> " + string);
            }
            if (string.length() != 0) {
                try {
                    i = Integer.parseInt(string.substring(0, 3));
                } catch (NumberFormatException e) {
                    i = -1;
                } catch (StringIndexOutOfBoundsException e2) {
                }
                this.serverResponse.addElement(string);
                if (i2 == -1) {
                    if (i == i2 && (string.length() < 4 || string.charAt(3) != '-')) {
                        break;
                    }
                } else {
                    if (string.length() < 4 || string.charAt(3) != '-') {
                        break;
                    }
                    i2 = i;
                }
            }
            i = -1;
            this.serverResponse.addElement(string);
            if (i2 == -1) {
            }
        }
        return i;
    }

    private void sendServer(String str) {
        this.out.print(str);
        if (logger.isLoggable(PlatformLogger.Level.FINEST)) {
            logger.finest("Server [" + ((Object) this.serverAddr) + "] <-- " + str);
        }
    }

    private String getResponseString() {
        return this.serverResponse.elementAt(0);
    }

    private Vector<String> getResponseStrings() {
        return this.serverResponse;
    }

    private boolean readReply() throws IOException {
        this.lastReplyCode = FtpReplyCode.find(readServerResponse());
        if (this.lastReplyCode.isPositivePreliminary()) {
            this.replyPending = true;
            return true;
        }
        if (this.lastReplyCode.isPositiveCompletion() || this.lastReplyCode.isPositiveIntermediate()) {
            if (this.lastReplyCode == FtpReplyCode.CLOSING_DATA_CONNECTION) {
                getTransferName();
            }
            return true;
        }
        return false;
    }

    private boolean issueCommand(String str) throws FtpProtocolException, IOException {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected");
        }
        if (this.replyPending) {
            try {
                completePending();
            } catch (FtpProtocolException e) {
            }
        }
        if (str.indexOf(10) != -1) {
            FtpProtocolException ftpProtocolException = new FtpProtocolException("Illegal FTP command");
            ftpProtocolException.initCause(new IllegalArgumentException("Illegal carriage return"));
            throw ftpProtocolException;
        }
        sendServer(str + "\r\n");
        return readReply();
    }

    private void issueCommandCheck(String str) throws FtpProtocolException, IOException {
        if (!issueCommand(str)) {
            throw new FtpProtocolException(str + ":" + getResponseString(), getLastReplyCode());
        }
    }

    private Socket openPassiveDataConnection(String str) throws FtpProtocolException, IOException {
        InetSocketAddress inetSocketAddress;
        Socket socket;
        if (issueCommand("EPSV ALL")) {
            issueCommandCheck("EPSV");
            String responseString = getResponseString();
            if (epsvPat == null) {
                epsvPat = Pattern.compile("^229 .* \\(\\|\\|\\|(\\d+)\\|\\)");
            }
            Matcher matcher = epsvPat.matcher(responseString);
            if (!matcher.find()) {
                throw new FtpProtocolException("EPSV failed : " + responseString);
            }
            int i = Integer.parseInt(matcher.group(1));
            InetAddress inetAddress = this.server.getInetAddress();
            if (inetAddress != null) {
                inetSocketAddress = new InetSocketAddress(inetAddress, i);
            } else {
                inetSocketAddress = InetSocketAddress.createUnresolved(this.serverAddr.getHostName(), i);
            }
        } else {
            issueCommandCheck("PASV");
            String responseString2 = getResponseString();
            if (pasvPat == null) {
                pasvPat = Pattern.compile("227 .* \\(?(\\d{1,3},\\d{1,3},\\d{1,3},\\d{1,3}),(\\d{1,3}),(\\d{1,3})\\)?");
            }
            Matcher matcher2 = pasvPat.matcher(responseString2);
            if (!matcher2.find()) {
                throw new FtpProtocolException("PASV failed : " + responseString2);
            }
            inetSocketAddress = new InetSocketAddress(matcher2.group(1).replace(',', '.'), Integer.parseInt(matcher2.group(3)) + (Integer.parseInt(matcher2.group(2)) << 8));
        }
        if (this.proxy != null) {
            if (this.proxy.type() == Proxy.Type.SOCKS) {
                socket = (Socket) AccessController.doPrivileged(new PrivilegedAction<Socket>() {
                    @Override
                    public Socket run() {
                        return new Socket(FtpClient.this.proxy);
                    }
                });
            } else {
                socket = new Socket(Proxy.NO_PROXY);
            }
        } else {
            socket = new Socket();
        }
        socket.bind(new InetSocketAddress((InetAddress) AccessController.doPrivileged(new PrivilegedAction<InetAddress>() {
            @Override
            public InetAddress run() {
                return FtpClient.this.server.getLocalAddress();
            }
        }), 0));
        if (this.connectTimeout >= 0) {
            socket.connect(inetSocketAddress, this.connectTimeout);
        } else if (defaultConnectTimeout > 0) {
            socket.connect(inetSocketAddress, defaultConnectTimeout);
        } else {
            socket.connect(inetSocketAddress);
        }
        if (this.readTimeout >= 0) {
            socket.setSoTimeout(this.readTimeout);
        } else if (defaultSoTimeout > 0) {
            socket.setSoTimeout(defaultSoTimeout);
        }
        if (this.useCrypto) {
            try {
                socket = this.sslFact.createSocket(socket, inetSocketAddress.getHostName(), inetSocketAddress.getPort(), true);
            } catch (Exception e) {
                throw new FtpProtocolException("Can't open secure data channel: " + ((Object) e));
            }
        }
        if (!issueCommand(str)) {
            socket.close();
            if (getLastReplyCode() == FtpReplyCode.FILE_UNAVAILABLE) {
                throw new FileNotFoundException(str);
            }
            throw new FtpProtocolException(str + ":" + getResponseString(), getLastReplyCode());
        }
        return socket;
    }

    private Socket openDataConnection(String str) throws FtpProtocolException, IOException {
        try {
            return openPassiveDataConnection(str);
        } catch (FtpProtocolException e) {
            String message = e.getMessage();
            if (!message.startsWith("PASV") && !message.startsWith("EPSV")) {
                throw e;
            }
            if (this.proxy != null && this.proxy.type() == Proxy.Type.SOCKS) {
                throw new FtpProtocolException("Passive mode failed");
            }
            ServerSocket serverSocket = new ServerSocket(0, 1, this.server.getLocalAddress());
            try {
                InetAddress inetAddress = serverSocket.getInetAddress();
                if (inetAddress.isAnyLocalAddress()) {
                    inetAddress = this.server.getLocalAddress();
                }
                StringBuilder sb = new StringBuilder();
                sb.append("EPRT |");
                sb.append(inetAddress instanceof Inet6Address ? "2" : "1");
                sb.append("|");
                sb.append(inetAddress.getHostAddress());
                sb.append("|");
                sb.append(serverSocket.getLocalPort());
                sb.append("|");
                if (!issueCommand(sb.toString()) || !issueCommand(str)) {
                    String str2 = "PORT ";
                    for (byte b : inetAddress.getAddress()) {
                        str2 = str2 + (b & Character.DIRECTIONALITY_UNDEFINED) + ",";
                    }
                    issueCommandCheck(str2 + ((serverSocket.getLocalPort() >>> 8) & 255) + "," + (serverSocket.getLocalPort() & 255));
                    issueCommandCheck(str);
                }
                if (this.connectTimeout >= 0) {
                    serverSocket.setSoTimeout(this.connectTimeout);
                } else if (defaultConnectTimeout > 0) {
                    serverSocket.setSoTimeout(defaultConnectTimeout);
                }
                Socket socketAccept = serverSocket.accept();
                if (this.readTimeout >= 0) {
                    socketAccept.setSoTimeout(this.readTimeout);
                } else if (defaultSoTimeout > 0) {
                    socketAccept.setSoTimeout(defaultSoTimeout);
                }
                serverSocket.close();
                if (this.useCrypto) {
                    try {
                        return this.sslFact.createSocket(socketAccept, this.serverAddr.getHostName(), this.serverAddr.getPort(), true);
                    } catch (Exception e2) {
                        throw new IOException(e2.getLocalizedMessage());
                    }
                }
                return socketAccept;
            } catch (Throwable th) {
                serverSocket.close();
                throw th;
            }
        }
    }

    private InputStream createInputStream(InputStream inputStream) {
        if (this.type == FtpClient.TransferType.ASCII) {
            return new TelnetInputStream(inputStream, false);
        }
        return inputStream;
    }

    private OutputStream createOutputStream(OutputStream outputStream) {
        if (this.type == FtpClient.TransferType.ASCII) {
            return new TelnetOutputStream(outputStream, false);
        }
        return outputStream;
    }

    protected FtpClient() {
        this.parser = new DefaultParser();
        this.mlsxParser = new MLSxParser();
    }

    public static sun.net.ftp.FtpClient create() {
        return new FtpClient();
    }

    @Override
    public sun.net.ftp.FtpClient enablePassiveMode(boolean z) {
        return this;
    }

    @Override
    public boolean isPassiveModeEnabled() {
        return true;
    }

    @Override
    public sun.net.ftp.FtpClient setConnectTimeout(int i) {
        this.connectTimeout = i;
        return this;
    }

    @Override
    public int getConnectTimeout() {
        return this.connectTimeout;
    }

    @Override
    public sun.net.ftp.FtpClient setReadTimeout(int i) {
        this.readTimeout = i;
        return this;
    }

    @Override
    public int getReadTimeout() {
        return this.readTimeout;
    }

    @Override
    public sun.net.ftp.FtpClient setProxy(Proxy proxy) {
        this.proxy = proxy;
        return this;
    }

    @Override
    public Proxy getProxy() {
        return this.proxy;
    }

    private void tryConnect(InetSocketAddress inetSocketAddress, int i) throws IOException {
        if (isConnected()) {
            disconnect();
        }
        this.server = doConnect(inetSocketAddress, i);
        try {
            this.out = new PrintStream((OutputStream) new BufferedOutputStream(this.server.getOutputStream()), true, encoding);
            this.in = new BufferedInputStream(this.server.getInputStream());
        } catch (UnsupportedEncodingException e) {
            throw new InternalError(encoding + "encoding not found", e);
        }
    }

    private Socket doConnect(InetSocketAddress inetSocketAddress, int i) throws IOException {
        Socket socket;
        if (this.proxy != null) {
            if (this.proxy.type() == Proxy.Type.SOCKS) {
                socket = (Socket) AccessController.doPrivileged(new PrivilegedAction<Socket>() {
                    @Override
                    public Socket run() {
                        return new Socket(FtpClient.this.proxy);
                    }
                });
            } else {
                socket = new Socket(Proxy.NO_PROXY);
            }
        } else {
            socket = new Socket();
        }
        if (i >= 0) {
            socket.connect(inetSocketAddress, i);
        } else if (this.connectTimeout >= 0) {
            socket.connect(inetSocketAddress, this.connectTimeout);
        } else if (defaultConnectTimeout > 0) {
            socket.connect(inetSocketAddress, defaultConnectTimeout);
        } else {
            socket.connect(inetSocketAddress);
        }
        if (this.readTimeout >= 0) {
            socket.setSoTimeout(this.readTimeout);
        } else if (defaultSoTimeout > 0) {
            socket.setSoTimeout(defaultSoTimeout);
        }
        return socket;
    }

    private void disconnect() throws IOException {
        if (isConnected()) {
            this.server.close();
        }
        this.server = null;
        this.in = null;
        this.out = null;
        this.lastTransSize = -1L;
        this.lastFileName = null;
        this.restartOffset = 0L;
        this.welcomeMsg = null;
        this.lastReplyCode = null;
        this.serverResponse.setSize(0);
    }

    @Override
    public boolean isConnected() {
        return this.server != null;
    }

    @Override
    public SocketAddress getServerAddress() {
        if (this.server == null) {
            return null;
        }
        return this.server.getRemoteSocketAddress();
    }

    @Override
    public sun.net.ftp.FtpClient connect(SocketAddress socketAddress) throws FtpProtocolException, IOException {
        return connect(socketAddress, -1);
    }

    @Override
    public sun.net.ftp.FtpClient connect(SocketAddress socketAddress, int i) throws FtpProtocolException, IOException {
        if (!(socketAddress instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Wrong address type");
        }
        this.serverAddr = (InetSocketAddress) socketAddress;
        tryConnect(this.serverAddr, i);
        if (!readReply()) {
            throw new FtpProtocolException("Welcome message: " + getResponseString(), this.lastReplyCode);
        }
        this.welcomeMsg = getResponseString().substring(4);
        return this;
    }

    private void tryLogin(String str, char[] cArr) throws FtpProtocolException, IOException {
        issueCommandCheck("USER " + str);
        if (this.lastReplyCode == FtpReplyCode.NEED_PASSWORD && cArr != null && cArr.length > 0) {
            issueCommandCheck("PASS " + String.valueOf(cArr));
        }
    }

    @Override
    public sun.net.ftp.FtpClient login(String str, char[] cArr) throws FtpProtocolException, IOException {
        if (!isConnected()) {
            throw new FtpProtocolException("Not connected yet", FtpReplyCode.BAD_SEQUENCE);
        }
        if (str == null || str.length() == 0) {
            throw new IllegalArgumentException("User name can't be null or empty");
        }
        tryLogin(str, cArr);
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < this.serverResponse.size(); i++) {
            String strElementAt = this.serverResponse.elementAt(i);
            if (strElementAt != null) {
                if (strElementAt.length() >= 4 && strElementAt.startsWith("230")) {
                    strElementAt = strElementAt.substring(4);
                }
                stringBuffer.append(strElementAt);
            }
        }
        this.welcomeMsg = stringBuffer.toString();
        this.loggedIn = true;
        return this;
    }

    @Override
    public sun.net.ftp.FtpClient login(String str, char[] cArr, String str2) throws FtpProtocolException, IOException {
        if (!isConnected()) {
            throw new FtpProtocolException("Not connected yet", FtpReplyCode.BAD_SEQUENCE);
        }
        if (str == null || str.length() == 0) {
            throw new IllegalArgumentException("User name can't be null or empty");
        }
        tryLogin(str, cArr);
        if (this.lastReplyCode == FtpReplyCode.NEED_ACCOUNT) {
            issueCommandCheck("ACCT " + str2);
        }
        StringBuffer stringBuffer = new StringBuffer();
        if (this.serverResponse != null) {
            for (String strSubstring : this.serverResponse) {
                if (strSubstring != null) {
                    if (strSubstring.length() >= 4 && strSubstring.startsWith("230")) {
                        strSubstring = strSubstring.substring(4);
                    }
                    stringBuffer.append(strSubstring);
                }
            }
        }
        this.welcomeMsg = stringBuffer.toString();
        this.loggedIn = true;
        return this;
    }

    @Override
    public void close() throws IOException {
        if (isConnected()) {
            try {
                issueCommand("QUIT");
            } catch (FtpProtocolException e) {
            }
            this.loggedIn = false;
        }
        disconnect();
    }

    @Override
    public boolean isLoggedIn() {
        return this.loggedIn;
    }

    @Override
    public sun.net.ftp.FtpClient changeDirectory(String str) throws FtpProtocolException, IOException {
        if (str == null || "".equals(str)) {
            throw new IllegalArgumentException("directory can't be null or empty");
        }
        issueCommandCheck("CWD " + str);
        return this;
    }

    @Override
    public sun.net.ftp.FtpClient changeToParentDirectory() throws FtpProtocolException, IOException {
        issueCommandCheck("CDUP");
        return this;
    }

    @Override
    public String getWorkingDirectory() throws FtpProtocolException, IOException {
        issueCommandCheck("PWD");
        String responseString = getResponseString();
        if (!responseString.startsWith("257")) {
            return null;
        }
        return responseString.substring(5, responseString.lastIndexOf(34));
    }

    @Override
    public sun.net.ftp.FtpClient setRestartOffset(long j) {
        if (j < 0) {
            throw new IllegalArgumentException("offset can't be negative");
        }
        this.restartOffset = j;
        return this;
    }

    @Override
    public sun.net.ftp.FtpClient getFile(String str, OutputStream outputStream) throws FtpProtocolException, IOException {
        if (this.restartOffset > 0) {
            try {
                Socket socketOpenDataConnection = openDataConnection("REST " + this.restartOffset);
                this.restartOffset = 0L;
                issueCommandCheck("RETR " + str);
                getTransferSize();
                InputStream inputStreamCreateInputStream = createInputStream(socketOpenDataConnection.getInputStream());
                byte[] bArr = new byte[15000];
                while (true) {
                    int i = inputStreamCreateInputStream.read(bArr);
                    if (i < 0) {
                        break;
                    }
                    if (i > 0) {
                        outputStream.write(bArr, 0, i);
                    }
                }
                inputStreamCreateInputStream.close();
            } catch (Throwable th) {
                this.restartOffset = 0L;
                throw th;
            }
        } else {
            Socket socketOpenDataConnection2 = openDataConnection("RETR " + str);
            getTransferSize();
            InputStream inputStreamCreateInputStream2 = createInputStream(socketOpenDataConnection2.getInputStream());
            byte[] bArr2 = new byte[15000];
            while (true) {
                int i2 = inputStreamCreateInputStream2.read(bArr2);
                if (i2 < 0) {
                    break;
                }
                if (i2 > 0) {
                    outputStream.write(bArr2, 0, i2);
                }
            }
            inputStreamCreateInputStream2.close();
        }
        return completePending();
    }

    @Override
    public InputStream getFileStream(String str) throws FtpProtocolException, IOException {
        if (this.restartOffset > 0) {
            try {
                Socket socketOpenDataConnection = openDataConnection("REST " + this.restartOffset);
                if (socketOpenDataConnection == null) {
                    return null;
                }
                issueCommandCheck("RETR " + str);
                getTransferSize();
                return createInputStream(socketOpenDataConnection.getInputStream());
            } finally {
                this.restartOffset = 0L;
            }
        }
        Socket socketOpenDataConnection2 = openDataConnection("RETR " + str);
        if (socketOpenDataConnection2 == null) {
            return null;
        }
        getTransferSize();
        return createInputStream(socketOpenDataConnection2.getInputStream());
    }

    @Override
    public OutputStream putFileStream(String str, boolean z) throws FtpProtocolException, IOException {
        Socket socketOpenDataConnection = openDataConnection((z ? "STOU " : "STOR ") + str);
        if (socketOpenDataConnection == null) {
            return null;
        }
        return new TelnetOutputStream(socketOpenDataConnection.getOutputStream(), this.type == FtpClient.TransferType.BINARY);
    }

    @Override
    public sun.net.ftp.FtpClient putFile(String str, InputStream inputStream, boolean z) throws FtpProtocolException, IOException {
        String str2 = z ? "STOU " : "STOR ";
        if (this.type == FtpClient.TransferType.BINARY) {
            OutputStream outputStreamCreateOutputStream = createOutputStream(openDataConnection(str2 + str).getOutputStream());
            byte[] bArr = new byte[15000];
            while (true) {
                int i = inputStream.read(bArr);
                if (i < 0) {
                    break;
                }
                if (i > 0) {
                    outputStreamCreateOutputStream.write(bArr, 0, i);
                }
            }
            outputStreamCreateOutputStream.close();
        }
        return completePending();
    }

    @Override
    public sun.net.ftp.FtpClient appendFile(String str, InputStream inputStream) throws FtpProtocolException, IOException {
        OutputStream outputStreamCreateOutputStream = createOutputStream(openDataConnection("APPE " + str).getOutputStream());
        byte[] bArr = new byte[15000];
        while (true) {
            int i = inputStream.read(bArr);
            if (i >= 0) {
                if (i > 0) {
                    outputStreamCreateOutputStream.write(bArr, 0, i);
                }
            } else {
                outputStreamCreateOutputStream.close();
                return completePending();
            }
        }
    }

    @Override
    public sun.net.ftp.FtpClient rename(String str, String str2) throws FtpProtocolException, IOException {
        issueCommandCheck("RNFR " + str);
        issueCommandCheck("RNTO " + str2);
        return this;
    }

    @Override
    public sun.net.ftp.FtpClient deleteFile(String str) throws FtpProtocolException, IOException {
        issueCommandCheck("DELE " + str);
        return this;
    }

    @Override
    public sun.net.ftp.FtpClient makeDirectory(String str) throws FtpProtocolException, IOException {
        issueCommandCheck("MKD " + str);
        return this;
    }

    @Override
    public sun.net.ftp.FtpClient removeDirectory(String str) throws FtpProtocolException, IOException {
        issueCommandCheck("RMD " + str);
        return this;
    }

    @Override
    public sun.net.ftp.FtpClient noop() throws FtpProtocolException, IOException {
        issueCommandCheck("NOOP");
        return this;
    }

    @Override
    public String getStatus(String str) throws FtpProtocolException, IOException {
        issueCommandCheck(str == null ? "STAT" : "STAT " + str);
        Vector<String> responseStrings = getResponseStrings();
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 1; i < responseStrings.size() - 1; i++) {
            stringBuffer.append(responseStrings.get(i));
        }
        return stringBuffer.toString();
    }

    @Override
    public List<String> getFeatures() throws FtpProtocolException, IOException {
        ArrayList arrayList = new ArrayList();
        issueCommandCheck("FEAT");
        Vector<String> responseStrings = getResponseStrings();
        for (int i = 1; i < responseStrings.size() - 1; i++) {
            String str = responseStrings.get(i);
            arrayList.add(str.substring(1, str.length() - 1));
        }
        return arrayList;
    }

    @Override
    public sun.net.ftp.FtpClient abort() throws FtpProtocolException, IOException {
        issueCommandCheck("ABOR");
        return this;
    }

    @Override
    public sun.net.ftp.FtpClient completePending() throws FtpProtocolException, IOException {
        while (this.replyPending) {
            this.replyPending = false;
            if (!readReply()) {
                throw new FtpProtocolException(getLastResponseString(), this.lastReplyCode);
            }
        }
        return this;
    }

    @Override
    public sun.net.ftp.FtpClient reInit() throws FtpProtocolException, IOException {
        issueCommandCheck("REIN");
        this.loggedIn = false;
        if (this.useCrypto && (this.server instanceof SSLSocket)) {
            ((SSLSocket) this.server).getSession().invalidate();
            this.server = this.oldSocket;
            this.oldSocket = null;
            try {
                this.out = new PrintStream((OutputStream) new BufferedOutputStream(this.server.getOutputStream()), true, encoding);
                this.in = new BufferedInputStream(this.server.getInputStream());
            } catch (UnsupportedEncodingException e) {
                throw new InternalError(encoding + "encoding not found", e);
            }
        }
        this.useCrypto = false;
        return this;
    }

    @Override
    public sun.net.ftp.FtpClient setType(FtpClient.TransferType transferType) throws FtpProtocolException, IOException {
        String str = "NOOP";
        this.type = transferType;
        if (transferType == FtpClient.TransferType.ASCII) {
            str = "TYPE A";
        }
        if (transferType == FtpClient.TransferType.BINARY) {
            str = "TYPE I";
        }
        if (transferType == FtpClient.TransferType.EBCDIC) {
            str = "TYPE E";
        }
        issueCommandCheck(str);
        return this;
    }

    @Override
    public InputStream list(String str) throws FtpProtocolException, IOException {
        String str2;
        if (str == null) {
            str2 = "LIST";
        } else {
            str2 = "LIST " + str;
        }
        Socket socketOpenDataConnection = openDataConnection(str2);
        if (socketOpenDataConnection != null) {
            return createInputStream(socketOpenDataConnection.getInputStream());
        }
        return null;
    }

    @Override
    public InputStream nameList(String str) throws FtpProtocolException, IOException {
        String str2;
        if (str == null) {
            str2 = "NLST";
        } else {
            str2 = "NLST " + str;
        }
        Socket socketOpenDataConnection = openDataConnection(str2);
        if (socketOpenDataConnection != null) {
            return createInputStream(socketOpenDataConnection.getInputStream());
        }
        return null;
    }

    @Override
    public long getSize(String str) throws FtpProtocolException, IOException {
        if (str == null || str.length() == 0) {
            throw new IllegalArgumentException("path can't be null or empty");
        }
        issueCommandCheck("SIZE " + str);
        if (this.lastReplyCode == FtpReplyCode.FILE_STATUS) {
            return Long.parseLong(getResponseString().substring(4, r3.length() - 1));
        }
        return -1L;
    }

    @Override
    public Date getLastModified(String str) throws FtpProtocolException, IOException {
        issueCommandCheck("MDTM " + str);
        if (this.lastReplyCode == FtpReplyCode.FILE_STATUS) {
            String strSubstring = getResponseString().substring(4);
            Date date = null;
            for (SimpleDateFormat simpleDateFormat : dateFormats) {
                try {
                    date = simpleDateFormat.parse(strSubstring);
                } catch (ParseException e) {
                }
                if (date != null) {
                    return date;
                }
            }
        }
        return null;
    }

    @Override
    public sun.net.ftp.FtpClient setDirParser(FtpDirParser ftpDirParser) {
        this.parser = ftpDirParser;
        return this;
    }

    private class FtpFileIterator implements Iterator<FtpDirEntry>, Closeable {
        private FtpDirParser fparser;
        private BufferedReader in;
        private FtpDirEntry nextFile = null;
        private boolean eof = false;

        public FtpFileIterator(FtpDirParser ftpDirParser, BufferedReader bufferedReader) {
            this.in = null;
            this.fparser = null;
            this.in = bufferedReader;
            this.fparser = ftpDirParser;
            readNext();
        }

        private void readNext() {
            String line;
            this.nextFile = null;
            if (this.eof) {
                return;
            }
            do {
                try {
                    line = this.in.readLine();
                    if (line != null) {
                        this.nextFile = this.fparser.parseLine(line);
                        if (this.nextFile != null) {
                            return;
                        }
                    }
                } catch (IOException e) {
                }
            } while (line != null);
            this.in.close();
            this.eof = true;
        }

        @Override
        public boolean hasNext() {
            return this.nextFile != null;
        }

        @Override
        public FtpDirEntry next() {
            FtpDirEntry ftpDirEntry = this.nextFile;
            readNext();
            return ftpDirEntry;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void close() throws IOException {
            if (this.in != null && !this.eof) {
                this.in.close();
            }
            this.eof = true;
            this.nextFile = null;
        }
    }

    @Override
    public Iterator<FtpDirEntry> listFiles(String str) throws FtpProtocolException, IOException {
        String str2;
        Socket socketOpenDataConnection;
        String str3;
        if (str == null) {
            str2 = "MLSD";
        } else {
            try {
                str2 = "MLSD " + str;
            } catch (FtpProtocolException e) {
                socketOpenDataConnection = null;
            }
        }
        socketOpenDataConnection = openDataConnection(str2);
        if (socketOpenDataConnection != null) {
            return new FtpFileIterator(this.mlsxParser, new BufferedReader(new InputStreamReader(socketOpenDataConnection.getInputStream())));
        }
        if (str == null) {
            str3 = "LIST";
        } else {
            str3 = "LIST " + str;
        }
        Socket socketOpenDataConnection2 = openDataConnection(str3);
        if (socketOpenDataConnection2 == null) {
            return null;
        }
        return new FtpFileIterator(this.parser, new BufferedReader(new InputStreamReader(socketOpenDataConnection2.getInputStream())));
    }

    private boolean sendSecurityData(byte[] bArr) throws FtpProtocolException, IOException {
        return issueCommand("ADAT " + new BASE64Encoder().encode(bArr));
    }

    private byte[] getSecurityData() {
        String lastResponseString = getLastResponseString();
        if (lastResponseString.substring(4, 9).equalsIgnoreCase("ADAT=")) {
            try {
                return new BASE64Decoder().decodeBuffer(lastResponseString.substring(9, lastResponseString.length() - 1));
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public sun.net.ftp.FtpClient useKerberos() throws FtpProtocolException, IOException {
        return this;
    }

    @Override
    public String getWelcomeMsg() {
        return this.welcomeMsg;
    }

    @Override
    public FtpReplyCode getLastReplyCode() {
        return this.lastReplyCode;
    }

    @Override
    public String getLastResponseString() {
        StringBuffer stringBuffer = new StringBuffer();
        if (this.serverResponse != null) {
            for (String str : this.serverResponse) {
                if (str != null) {
                    stringBuffer.append(str);
                }
            }
        }
        return stringBuffer.toString();
    }

    @Override
    public long getLastTransferSize() {
        return this.lastTransSize;
    }

    @Override
    public String getLastFileName() {
        return this.lastFileName;
    }

    @Override
    public sun.net.ftp.FtpClient startSecureSession() throws FtpProtocolException, IOException {
        if (!isConnected()) {
            throw new FtpProtocolException("Not connected yet", FtpReplyCode.BAD_SEQUENCE);
        }
        if (this.sslFact == null) {
            try {
                this.sslFact = (SSLSocketFactory) SSLSocketFactory.getDefault();
            } catch (Exception e) {
                throw new IOException(e.getLocalizedMessage());
            }
        }
        issueCommandCheck("AUTH TLS");
        try {
            Socket socketCreateSocket = this.sslFact.createSocket(this.server, this.serverAddr.getHostName(), this.serverAddr.getPort(), true);
            this.oldSocket = this.server;
            this.server = socketCreateSocket;
            try {
                this.out = new PrintStream((OutputStream) new BufferedOutputStream(this.server.getOutputStream()), true, encoding);
                this.in = new BufferedInputStream(this.server.getInputStream());
                issueCommandCheck("PBSZ 0");
                issueCommandCheck("PROT P");
                this.useCrypto = true;
                return this;
            } catch (UnsupportedEncodingException e2) {
                throw new InternalError(encoding + "encoding not found", e2);
            }
        } catch (SSLException e3) {
            try {
                disconnect();
            } catch (Exception e4) {
            }
            throw e3;
        }
    }

    @Override
    public sun.net.ftp.FtpClient endSecureSession() throws FtpProtocolException, IOException {
        if (!this.useCrypto) {
            return this;
        }
        issueCommandCheck("CCC");
        issueCommandCheck("PROT C");
        this.useCrypto = false;
        this.server = this.oldSocket;
        this.oldSocket = null;
        try {
            this.out = new PrintStream((OutputStream) new BufferedOutputStream(this.server.getOutputStream()), true, encoding);
            this.in = new BufferedInputStream(this.server.getInputStream());
            return this;
        } catch (UnsupportedEncodingException e) {
            throw new InternalError(encoding + "encoding not found", e);
        }
    }

    @Override
    public sun.net.ftp.FtpClient allocate(long j) throws FtpProtocolException, IOException {
        issueCommandCheck("ALLO " + j);
        return this;
    }

    @Override
    public sun.net.ftp.FtpClient structureMount(String str) throws FtpProtocolException, IOException {
        issueCommandCheck("SMNT " + str);
        return this;
    }

    @Override
    public String getSystem() throws FtpProtocolException, IOException {
        issueCommandCheck("SYST");
        return getResponseString().substring(4);
    }

    @Override
    public String getHelp(String str) throws FtpProtocolException, IOException {
        issueCommandCheck("HELP " + str);
        Vector<String> responseStrings = getResponseStrings();
        if (responseStrings.size() == 1) {
            return responseStrings.get(0).substring(4);
        }
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 1; i < responseStrings.size() - 1; i++) {
            stringBuffer.append(responseStrings.get(i).substring(3));
        }
        return stringBuffer.toString();
    }

    @Override
    public sun.net.ftp.FtpClient siteCmd(String str) throws FtpProtocolException, IOException {
        issueCommandCheck("SITE " + str);
        return this;
    }
}
