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
        PasswordAuthentication passwordAuthenticationRequestPasswordAuthentication;
        List<Challenge> listChallenges = response.challenges();
        Request request = response.request();
        HttpUrl httpUrl = request.httpUrl();
        int size = listChallenges.size();
        for (int i = 0; i < size; i++) {
            Challenge challenge = listChallenges.get(i);
            if ("Basic".equalsIgnoreCase(challenge.getScheme()) && (passwordAuthenticationRequestPasswordAuthentication = java.net.Authenticator.requestPasswordAuthentication(httpUrl.host(), getConnectToInetAddress(proxy, httpUrl), httpUrl.port(), httpUrl.scheme(), challenge.getRealm(), challenge.getScheme(), httpUrl.url(), Authenticator.RequestorType.SERVER)) != null) {
                return request.newBuilder().header("Authorization", Credentials.basic(passwordAuthenticationRequestPasswordAuthentication.getUserName(), new String(passwordAuthenticationRequestPasswordAuthentication.getPassword()))).build();
            }
        }
        return null;
    }

    @Override
    public Request authenticateProxy(Proxy proxy, Response response) throws IOException {
        List<Challenge> listChallenges = response.challenges();
        Request request = response.request();
        HttpUrl httpUrl = request.httpUrl();
        int size = listChallenges.size();
        for (int i = 0; i < size; i++) {
            Challenge challenge = listChallenges.get(i);
            if ("Basic".equalsIgnoreCase(challenge.getScheme())) {
                InetSocketAddress inetSocketAddress = (InetSocketAddress) proxy.address();
                PasswordAuthentication passwordAuthenticationRequestPasswordAuthentication = java.net.Authenticator.requestPasswordAuthentication(inetSocketAddress.getHostName(), getConnectToInetAddress(proxy, httpUrl), inetSocketAddress.getPort(), httpUrl.scheme(), challenge.getRealm(), challenge.getScheme(), httpUrl.url(), Authenticator.RequestorType.PROXY);
                if (passwordAuthenticationRequestPasswordAuthentication != null) {
                    return request.newBuilder().header("Proxy-Authorization", Credentials.basic(passwordAuthenticationRequestPasswordAuthentication.getUserName(), new String(passwordAuthenticationRequestPasswordAuthentication.getPassword()))).build();
                }
            }
        }
        return null;
    }

    private InetAddress getConnectToInetAddress(Proxy proxy, HttpUrl httpUrl) throws IOException {
        if (proxy != null && proxy.type() != Proxy.Type.DIRECT) {
            return ((InetSocketAddress) proxy.address()).getAddress();
        }
        return InetAddress.getByName(httpUrl.host());
    }
}
