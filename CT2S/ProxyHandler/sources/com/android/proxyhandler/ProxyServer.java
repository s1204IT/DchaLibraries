package com.android.proxyhandler;

import android.os.RemoteException;
import android.util.Log;
import com.android.net.IProxyPortListener;
import com.google.android.collect.Lists;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProxyServer extends Thread {
    private ServerSocket serverSocket;
    public boolean mIsRunning = false;
    private ExecutorService threadExecutor = Executors.newCachedThreadPool();
    private int mPort = -1;
    private IProxyPortListener mCallback = null;

    private class ProxyConnection implements Runnable {
        private Socket connection;

        private ProxyConnection(Socket connection) {
            this.connection = connection;
        }

        @Override
        public void run() {
            String host;
            int port;
            try {
                String requestLine = getLine(this.connection.getInputStream());
                if (requestLine == null) {
                    this.connection.close();
                    return;
                }
                String[] splitLine = requestLine.split(" ");
                if (splitLine.length < 3) {
                    this.connection.close();
                    return;
                }
                String requestType = splitLine[0];
                String urlString = splitLine[1];
                if (requestType.equals("CONNECT")) {
                    String[] hostPortSplit = urlString.split(":");
                    host = hostPortSplit[0];
                    try {
                        port = Integer.parseInt(hostPortSplit[1]);
                    } catch (NumberFormatException e) {
                        port = 443;
                    }
                    urlString = "Https://" + host + ":" + port;
                } else {
                    try {
                        URI url = new URI(urlString);
                        host = url.getHost();
                        port = url.getPort();
                        if (port < 0) {
                            port = 80;
                        }
                    } catch (URISyntaxException e2) {
                        this.connection.close();
                        return;
                    }
                }
                List<Proxy> list = Lists.newArrayList();
                try {
                    list = ProxySelector.getDefault().select(new URI(urlString));
                } catch (URISyntaxException e3) {
                    e3.printStackTrace();
                }
                Socket server = null;
                Iterator<Proxy> it = list.iterator();
                while (true) {
                    Socket server2 = server;
                    if (!it.hasNext()) {
                        server = server2;
                        break;
                    }
                    Proxy proxy = it.next();
                    try {
                        if (!proxy.equals(Proxy.NO_PROXY)) {
                            InetSocketAddress inetSocketAddress = (InetSocketAddress) proxy.address();
                            server = new Socket(inetSocketAddress.getHostName(), inetSocketAddress.getPort());
                            try {
                                sendLine(server, requestLine);
                            } catch (IOException e4) {
                            }
                        } else {
                            server = new Socket(host, port);
                            if (requestType.equals("CONNECT")) {
                                while (getLine(this.connection.getInputStream()).length() != 0) {
                                }
                                sendLine(this.connection, "HTTP/1.1 200 OK\n");
                            } else {
                                sendLine(server, requestLine);
                            }
                        }
                    } catch (IOException e5) {
                        server = server2;
                    }
                    if (server != null) {
                        break;
                    }
                }
                if (server == null) {
                    server = new Socket(host, port);
                    if (requestType.equals("CONNECT")) {
                        while (getLine(this.connection.getInputStream()).length() != 0) {
                        }
                        sendLine(this.connection, "HTTP/1.1 200 OK\n");
                    } else {
                        sendLine(server, requestLine);
                    }
                }
                SocketConnect.connect(this.connection, server);
            } catch (IOException e6) {
                Log.d("ProxyServer", "Problem Proxying", e6);
            }
            try {
                this.connection.close();
            } catch (IOException e7) {
            }
        }

        private String getLine(InputStream inputStream) throws IOException {
            StringBuffer buffer = new StringBuffer();
            int byteBuffer = inputStream.read();
            if (byteBuffer < 0) {
                return "";
            }
            do {
                if (byteBuffer != 13) {
                    buffer.append((char) byteBuffer);
                }
                byteBuffer = inputStream.read();
                if (byteBuffer == 10) {
                    break;
                }
            } while (byteBuffer >= 0);
            return buffer.toString();
        }

        private void sendLine(Socket socket, String line) throws IOException {
            OutputStream os = socket.getOutputStream();
            os.write(line.getBytes());
            os.write(13);
            os.write(10);
            os.flush();
        }
    }

    @Override
    public void run() {
        try {
            try {
                this.serverSocket = new ServerSocket(0);
                if (this.serverSocket != null) {
                    setPort(this.serverSocket.getLocalPort());
                    while (this.mIsRunning) {
                        try {
                            Socket socket = this.serverSocket.accept();
                            if (socket.getInetAddress().isLoopbackAddress()) {
                                ProxyConnection parser = new ProxyConnection(socket);
                                this.threadExecutor.execute(parser);
                            } else {
                                socket.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e1) {
                Log.e("ProxyServer", "Failed to start proxy server", e1);
            }
        } catch (SocketException e2) {
            Log.e("ProxyServer", "Failed to start proxy server", e2);
        }
        this.mIsRunning = false;
    }

    public synchronized void setPort(int port) {
        if (this.mCallback != null) {
            try {
                this.mCallback.setProxyPort(port);
            } catch (RemoteException e) {
                Log.w("ProxyServer", "Proxy failed to report port to PacManager", e);
            }
            this.mPort = port;
        } else {
            this.mPort = port;
        }
    }

    public synchronized void setCallback(IProxyPortListener callback) {
        if (this.mPort != -1) {
            try {
                callback.setProxyPort(this.mPort);
            } catch (RemoteException e) {
                Log.w("ProxyServer", "Proxy failed to report port to PacManager", e);
            }
            this.mCallback = callback;
        } else {
            this.mCallback = callback;
        }
    }

    public synchronized void startServer() {
        this.mIsRunning = true;
        start();
    }

    public synchronized void stopServer() {
        this.mIsRunning = false;
        if (this.serverSocket != null) {
            try {
                this.serverSocket.close();
                this.serverSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
