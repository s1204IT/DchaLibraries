package com.android.okhttp.internal.http;

import com.android.okhttp.Authenticator;
import com.android.okhttp.Challenge;
import com.android.okhttp.Credentials;
import com.android.okhttp.HttpUrl;
import com.android.okhttp.Request;
import com.android.okhttp.Response;
import java.io.IOException;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.List;

public final class AuthenticatorAdapter implements Authenticator {
    public static final Authenticator INSTANCE = new AuthenticatorAdapter();

    @Override
    public Request authenticate(Proxy proxy, Response response) throws IOException {
        List<Challenge> listChallenges = response.challenges();
        Request request = response.request();
        HttpUrl url = request.httpUrl();
        int size = listChallenges.size();
        for (int i = 0; i < size; i++) {
            Challenge challenge = listChallenges.get(i);
            if (!"Basic".equalsIgnoreCase(challenge.getScheme())) {
                if ("true".equals(System.getProperty("http.digest.support", "false")) && "Digest".equalsIgnoreCase(challenge.getScheme())) {
                    String method = System.getProperty("http.method", "GET");
                    System.out.println("Digest:" + method + ":" + url.url().getPath());
                    PasswordAuthentication auth = java.net.Authenticator.requestPasswordAuthentication(url.host(), null, url.port(), url.scheme(), challenge.getRealm(), challenge.getScheme(), url.url(), Authenticator.RequestorType.SERVER);
                    if (auth != null) {
                        String credential = Credentials.digest(auth.getUserName(), new String(auth.getPassword()), challenge, method, url.url().getPath());
                        return request.newBuilder().header("Authorization", credential).build();
                    }
                    if ("403".equals(System.getProperty("gba.auth"))) {
                        throw new IOException();
                    }
                    return null;
                }
            } else {
                PasswordAuthentication auth2 = java.net.Authenticator.requestPasswordAuthentication(url.rfc2732host(), getConnectToInetAddress(proxy, url), url.port(), url.scheme(), challenge.getRealm(), challenge.getScheme(), url.url(), Authenticator.RequestorType.SERVER);
                if (auth2 != null) {
                    String credential2 = Credentials.basic(auth2.getUserName(), new String(auth2.getPassword()));
                    return request.newBuilder().header("Authorization", credential2).build();
                }
            }
        }
        return null;
    }

    @Override
    public Request authenticateProxy(Proxy proxy, Response response) throws IOException {
        List<Challenge> listChallenges = response.challenges();
        Request request = response.request();
        HttpUrl url = request.httpUrl();
        int size = listChallenges.size();
        for (int i = 0; i < size; i++) {
            Challenge challenge = listChallenges.get(i);
            if ("Basic".equalsIgnoreCase(challenge.getScheme())) {
                InetSocketAddress proxyAddress = (InetSocketAddress) proxy.address();
                PasswordAuthentication auth = java.net.Authenticator.requestPasswordAuthentication(proxyAddress.getHostName(), getConnectToInetAddress(proxy, url), proxyAddress.getPort(), url.scheme(), challenge.getRealm(), challenge.getScheme(), url.url(), Authenticator.RequestorType.PROXY);
                if (auth != null) {
                    String credential = Credentials.basic(auth.getUserName(), new String(auth.getPassword()));
                    return request.newBuilder().header("Proxy-Authorization", credential).build();
                }
            }
        }
        return null;
    }

    private InetAddress getConnectToInetAddress(Proxy proxy, HttpUrl url) throws IOException {
        if (proxy != null && proxy.type() != Proxy.Type.DIRECT) {
            return ((InetSocketAddress) proxy.address()).getAddress();
        }
        return InetAddress.getByName(url.host());
    }
}
