package android.net;

import android.os.Parcel;
import android.os.Parcelable;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.proto.ProtoOutputStream;
import com.android.okhttp.internalandroidapi.Dns;
import com.android.okhttp.internalandroidapi.HttpURLConnectionFactory;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.net.SocketFactory;
import libcore.io.IoUtils;

public class Network implements Parcelable {
    public static final Parcelable.Creator<Network> CREATOR;
    private static final long HANDLE_MAGIC = 3405697037L;
    private static final int HANDLE_MAGIC_SIZE = 32;
    private static final boolean httpKeepAlive = Boolean.parseBoolean(System.getProperty("http.keepAlive", "true"));
    private static final long httpKeepAliveDurationMs;
    private static final int httpMaxConnections;
    private volatile HttpURLConnectionFactory mUrlConnectionFactory;
    public final int netId;
    private volatile NetworkBoundSocketFactory mNetworkBoundSocketFactory = null;
    private final Object mLock = new Object();
    private boolean mPrivateDnsBypass = false;

    static {
        httpMaxConnections = httpKeepAlive ? Integer.parseInt(System.getProperty("http.maxConnections", "5")) : 0;
        httpKeepAliveDurationMs = Long.parseLong(System.getProperty("http.keepAliveDuration", "300000"));
        CREATOR = new Parcelable.Creator<Network>() {
            @Override
            public Network createFromParcel(Parcel parcel) {
                return new Network(parcel.readInt());
            }

            @Override
            public Network[] newArray(int i) {
                return new Network[i];
            }
        };
    }

    public Network(int i) {
        this.netId = i;
    }

    public Network(Network network) {
        this.netId = network.netId;
    }

    public InetAddress[] getAllByName(String str) throws UnknownHostException {
        return InetAddress.getAllByNameOnNet(str, getNetIdForResolv());
    }

    public InetAddress getByName(String str) throws UnknownHostException {
        return InetAddress.getByNameOnNet(str, getNetIdForResolv());
    }

    public void setPrivateDnsBypass(boolean z) {
        this.mPrivateDnsBypass = z;
    }

    public int getNetIdForResolv() {
        if (this.mPrivateDnsBypass) {
            return (int) (2147483648L | ((long) this.netId));
        }
        return this.netId;
    }

    private class NetworkBoundSocketFactory extends SocketFactory {
        private final int mNetId;

        public NetworkBoundSocketFactory(int i) {
            this.mNetId = i;
        }

        private Socket connectToHost(String str, int i, SocketAddress socketAddress) throws IOException {
            InetAddress[] allByName = Network.this.getAllByName(str);
            for (int i2 = 0; i2 < allByName.length; i2++) {
                try {
                    Socket socketCreateSocket = createSocket();
                    if (socketAddress != null) {
                        try {
                            socketCreateSocket.bind(socketAddress);
                        } catch (Throwable th) {
                            IoUtils.closeQuietly(socketCreateSocket);
                            throw th;
                        }
                    }
                    socketCreateSocket.connect(new InetSocketAddress(allByName[i2], i));
                    return socketCreateSocket;
                } catch (IOException e) {
                    if (i2 == allByName.length - 1) {
                        throw e;
                    }
                }
            }
            throw new UnknownHostException(str);
        }

        @Override
        public Socket createSocket(String str, int i, InetAddress inetAddress, int i2) throws IOException {
            return connectToHost(str, i, new InetSocketAddress(inetAddress, i2));
        }

        @Override
        public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress2, int i2) throws IOException {
            Socket socketCreateSocket = createSocket();
            try {
                socketCreateSocket.bind(new InetSocketAddress(inetAddress2, i2));
                socketCreateSocket.connect(new InetSocketAddress(inetAddress, i));
                return socketCreateSocket;
            } catch (Throwable th) {
                IoUtils.closeQuietly(socketCreateSocket);
                throw th;
            }
        }

        @Override
        public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
            Socket socketCreateSocket = createSocket();
            try {
                socketCreateSocket.connect(new InetSocketAddress(inetAddress, i));
                return socketCreateSocket;
            } catch (Throwable th) {
                IoUtils.closeQuietly(socketCreateSocket);
                throw th;
            }
        }

        @Override
        public Socket createSocket(String str, int i) throws IOException {
            return connectToHost(str, i, null);
        }

        @Override
        public Socket createSocket() throws IOException {
            Socket socket = new Socket();
            try {
                Network.this.bindSocket(socket);
                return socket;
            } catch (Throwable th) {
                IoUtils.closeQuietly(socket);
                throw th;
            }
        }
    }

    public SocketFactory getSocketFactory() {
        if (this.mNetworkBoundSocketFactory == null) {
            synchronized (this.mLock) {
                if (this.mNetworkBoundSocketFactory == null) {
                    this.mNetworkBoundSocketFactory = new NetworkBoundSocketFactory(this.netId);
                }
            }
        }
        return this.mNetworkBoundSocketFactory;
    }

    private void maybeInitUrlConnectionFactory() {
        synchronized (this.mLock) {
            if (this.mUrlConnectionFactory == null) {
                Dns dns = new Dns() {
                    public final List lookup(String str) {
                        return Arrays.asList(this.f$0.getAllByName(str));
                    }
                };
                HttpURLConnectionFactory httpURLConnectionFactory = new HttpURLConnectionFactory();
                httpURLConnectionFactory.setDns(dns);
                httpURLConnectionFactory.setNewConnectionPool(httpMaxConnections, httpKeepAliveDurationMs, TimeUnit.MILLISECONDS);
                this.mUrlConnectionFactory = httpURLConnectionFactory;
            }
        }
    }

    public URLConnection openConnection(URL url) throws IOException {
        java.net.Proxy proxyMakeProxy;
        ConnectivityManager instanceOrNull = ConnectivityManager.getInstanceOrNull();
        if (instanceOrNull == null) {
            throw new IOException("No ConnectivityManager yet constructed, please construct one");
        }
        ProxyInfo proxyForNetwork = instanceOrNull.getProxyForNetwork(this);
        if (proxyForNetwork != null) {
            proxyMakeProxy = proxyForNetwork.makeProxy();
        } else {
            proxyMakeProxy = java.net.Proxy.NO_PROXY;
        }
        return openConnection(url, proxyMakeProxy);
    }

    public URLConnection openConnection(URL url, java.net.Proxy proxy) throws IOException {
        if (proxy == null) {
            throw new IllegalArgumentException("proxy is null");
        }
        maybeInitUrlConnectionFactory();
        return this.mUrlConnectionFactory.openConnection(url, getSocketFactory(), proxy);
    }

    public void bindSocket(DatagramSocket datagramSocket) throws IOException {
        datagramSocket.getReuseAddress();
        bindSocket(datagramSocket.getFileDescriptor$());
    }

    public void bindSocket(Socket socket) throws IOException {
        socket.getReuseAddress();
        bindSocket(socket.getFileDescriptor$());
    }

    public void bindSocket(FileDescriptor fileDescriptor) throws IOException {
        try {
            if (!((InetSocketAddress) Os.getpeername(fileDescriptor)).getAddress().isAnyLocalAddress()) {
                throw new SocketException("Socket is connected");
            }
        } catch (ErrnoException e) {
            if (e.errno != OsConstants.ENOTCONN) {
                throw e.rethrowAsSocketException();
            }
        } catch (ClassCastException e2) {
            throw new SocketException("Only AF_INET/AF_INET6 sockets supported");
        }
        int iBindSocketToNetwork = NetworkUtils.bindSocketToNetwork(fileDescriptor.getInt$(), this.netId);
        if (iBindSocketToNetwork != 0) {
            throw new ErrnoException("Binding socket to network " + this.netId, -iBindSocketToNetwork).rethrowAsSocketException();
        }
    }

    public static Network fromNetworkHandle(long j) {
        if (j == 0) {
            throw new IllegalArgumentException("Network.fromNetworkHandle refusing to instantiate NETID_UNSET Network.");
        }
        if ((4294967295L & j) != HANDLE_MAGIC || j < 0) {
            throw new IllegalArgumentException("Value passed to fromNetworkHandle() is not a network handle.");
        }
        return new Network((int) (j >> 32));
    }

    public long getNetworkHandle() {
        if (this.netId == 0) {
            return 0L;
        }
        return (((long) this.netId) << 32) | HANDLE_MAGIC;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.netId);
    }

    public boolean equals(Object obj) {
        return (obj instanceof Network) && this.netId == ((Network) obj).netId;
    }

    public int hashCode() {
        return this.netId * 11;
    }

    public String toString() {
        return Integer.toString(this.netId);
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1120986464257L, this.netId);
        protoOutputStream.end(jStart);
    }
}
