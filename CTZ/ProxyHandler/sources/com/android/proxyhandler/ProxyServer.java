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
import java.util.ArrayList;
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

        private ProxyConnection(Socket socket) {
            this.connection = socket;
        }

        @Override
        public void run() {
            URI uri;
            String str;
            int i;
            List<Proxy> listSelect;
            Socket socket;
            Iterator<Proxy> it;
            Proxy proxy;
            Socket socket2;
            int i2;
            try {
                String line = getLine(this.connection.getInputStream());
                String[] strArrSplit = line.split(" ");
                if (strArrSplit.length < 3) {
                    this.connection.close();
                    return;
                }
                String str2 = strArrSplit[0];
                String str3 = strArrSplit[1];
                String str4 = strArrSplit[2];
                Socket socket3 = null;
                if (str2.equals("CONNECT")) {
                    String[] strArrSplit2 = str3.split(":");
                    String str5 = strArrSplit2[0];
                    if (strArrSplit2.length < 2) {
                        i2 = 443;
                    } else {
                        try {
                            i2 = Integer.parseInt(strArrSplit2[1]);
                        } catch (NumberFormatException e) {
                            this.connection.close();
                            return;
                        }
                    }
                    str3 = "Https://" + str5 + ":" + i2;
                    i = i2;
                    str = str5;
                    uri = null;
                } else {
                    try {
                        URI uri2 = new URI(str3);
                        String host = uri2.getHost();
                        int port = uri2.getPort();
                        if (port < 0) {
                            port = 80;
                        }
                        uri = uri2;
                        str = host;
                        i = port;
                    } catch (URISyntaxException e2) {
                        this.connection.close();
                        return;
                    }
                }
                ArrayList arrayListNewArrayList = Lists.newArrayList();
                try {
                    listSelect = ProxySelector.getDefault().select(new URI(str3));
                } catch (URISyntaxException e3) {
                    e3.printStackTrace();
                    listSelect = arrayListNewArrayList;
                }
                for (Iterator<Proxy> it2 = listSelect.iterator(); it2.hasNext(); it2 = it) {
                    Proxy next = it2.next();
                    try {
                        if (!next.equals(Proxy.NO_PROXY)) {
                            InetSocketAddress inetSocketAddress = (InetSocketAddress) next.address();
                            Socket socket4 = new Socket(inetSocketAddress.getHostName(), inetSocketAddress.getPort());
                            try {
                                sendLine(socket4, line);
                                socket2 = socket4;
                                it = it2;
                                socket3 = socket2;
                            } catch (IOException e4) {
                                e = e4;
                                socket3 = socket4;
                                proxy = next;
                                it = it2;
                                if (Log.isLoggable("ProxyServer", 2)) {
                                }
                            }
                        } else {
                            Socket socket5 = new Socket(str, i);
                            try {
                                if (str2.equals("CONNECT")) {
                                    try {
                                        skipToRequestBody(this.connection);
                                        sendLine(this.connection, "HTTP/1.1 200 OK\n");
                                        socket2 = socket5;
                                        it = it2;
                                        socket3 = socket2;
                                    } catch (IOException e5) {
                                        e = e5;
                                        socket3 = socket5;
                                        proxy = next;
                                        it = it2;
                                        if (Log.isLoggable("ProxyServer", 2)) {
                                        }
                                    }
                                } else {
                                    socket2 = socket5;
                                    proxy = next;
                                    it = it2;
                                    try {
                                        sendAugmentedRequestToHost(this.connection, socket5, str2, uri, str4);
                                        socket3 = socket2;
                                    } catch (IOException e6) {
                                        e = e6;
                                        socket3 = socket2;
                                        if (Log.isLoggable("ProxyServer", 2)) {
                                            Log.v("ProxyServer", "Unable to connect to proxy " + proxy, e);
                                        }
                                    }
                                }
                            } catch (IOException e7) {
                                e = e7;
                                socket2 = socket5;
                                proxy = next;
                                it = it2;
                            }
                        }
                    } catch (IOException e8) {
                        e = e8;
                    }
                    if (socket3 != null) {
                        break;
                    }
                }
                if (listSelect.isEmpty()) {
                    socket = new Socket(str, i);
                    if (str2.equals("CONNECT")) {
                        skipToRequestBody(this.connection);
                        sendLine(this.connection, "HTTP/1.1 200 OK\n");
                    } else {
                        sendAugmentedRequestToHost(this.connection, socket, str2, uri, str4);
                    }
                } else {
                    socket = socket3;
                }
                if (socket != null) {
                    SocketConnect.connect(this.connection, socket);
                }
            } catch (Exception e9) {
                Log.d("ProxyServer", "Problem Proxying", e9);
            }
            try {
                this.connection.close();
            } catch (IOException e10) {
            }
        }

        private void sendRequestLineWithPath(Socket socket, String str, URI uri, String str2) throws IOException {
            sendLine(socket, String.format("%s %s %s", str, getAbsolutePathFromAbsoluteURI(uri), str2));
        }

        private String getAbsolutePathFromAbsoluteURI(URI uri) {
            String rawPath = uri.getRawPath();
            String rawQuery = uri.getRawQuery();
            String rawFragment = uri.getRawFragment();
            StringBuilder sb = new StringBuilder();
            if (rawPath != null) {
                sb.append(rawPath);
            } else {
                sb.append("/");
            }
            if (rawQuery != null) {
                sb.append("?");
                sb.append(rawQuery);
            }
            if (rawFragment != null) {
                sb.append("#");
                sb.append(rawFragment);
            }
            return sb.toString();
        }

        private String getLine(InputStream inputStream) throws IOException {
            StringBuilder sb = new StringBuilder();
            int i = inputStream.read();
            if (i < 0) {
                return "";
            }
            do {
                if (i != 13) {
                    sb.append((char) i);
                }
                i = inputStream.read();
                if (i == 10) {
                    break;
                }
            } while (i >= 0);
            return sb.toString();
        }

        private void sendLine(Socket socket, String str) throws IOException {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(str.getBytes());
            outputStream.write(13);
            outputStream.write(10);
            outputStream.flush();
        }

        private void skipToRequestBody(Socket socket) throws IOException {
            while (getLine(socket.getInputStream()).length() != 0) {
            }
        }

        private void sendAugmentedRequestToHost(Socket socket, Socket socket2, String str, URI uri, String str2) throws IOException {
            sendRequestLineWithPath(socket2, str, uri, str2);
            filterAndForwardRequestHeaders(socket, socket2);
            sendLine(socket2, "Connection: close");
            sendLine(socket2, "");
        }

        private void filterAndForwardRequestHeaders(Socket socket, Socket socket2) throws IOException {
            String line;
            do {
                line = getLine(socket.getInputStream());
                if (line.length() > 0 && !shouldRemoveHeaderLine(line)) {
                    sendLine(socket2, line);
                }
            } while (line.length() > 0);
        }

        private boolean shouldRemoveHeaderLine(String str) {
            int iIndexOf = str.indexOf(":");
            if (iIndexOf != -1) {
                String strTrim = str.substring(0, iIndexOf).trim();
                if (strTrim.regionMatches(true, 0, "connection", 0, "connection".length()) || strTrim.regionMatches(true, 0, "proxy-connection", 0, "proxy-connection".length())) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public void run() {
        try {
            try {
                this.serverSocket = new ServerSocket(0);
                setPort(this.serverSocket.getLocalPort());
                while (this.mIsRunning) {
                    try {
                        Socket socketAccept = this.serverSocket.accept();
                        if (socketAccept.getInetAddress().isLoopbackAddress()) {
                            this.threadExecutor.execute(new ProxyConnection(socketAccept));
                        } else {
                            socketAccept.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e2) {
                Log.e("ProxyServer", "Failed to start proxy server", e2);
            }
        } catch (SocketException e3) {
            Log.e("ProxyServer", "Failed to start proxy server", e3);
        }
        this.mIsRunning = false;
    }

    public synchronized void setPort(int i) {
        if (this.mCallback != null) {
            try {
                this.mCallback.setProxyPort(i);
            } catch (RemoteException e) {
                Log.w("ProxyServer", "Proxy failed to report port to PacManager", e);
            }
            this.mPort = i;
        } else {
            this.mPort = i;
        }
    }

    public synchronized void setCallback(IProxyPortListener iProxyPortListener) {
        if (this.mPort != -1) {
            try {
                iProxyPortListener.setProxyPort(this.mPort);
            } catch (RemoteException e) {
                Log.w("ProxyServer", "Proxy failed to report port to PacManager", e);
            }
            this.mCallback = iProxyPortListener;
        } else {
            this.mCallback = iProxyPortListener;
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
