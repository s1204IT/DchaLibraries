package com.android.okhttp.internal.http;

import com.android.okhttp.Headers;
import com.android.okhttp.OkAuthenticator;
import com.android.okhttp.Request;
import com.android.okhttp.Response;
import java.io.IOException;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public final class HttpAuthenticator {
    public static final OkAuthenticator SYSTEM_DEFAULT = new OkAuthenticator() {
        @Override
        public OkAuthenticator.Credential authenticate(Proxy proxy, URL url, List<OkAuthenticator.Challenge> challenges) throws IOException {
            PasswordAuthentication auth;
            int size = challenges.size();
            for (int i = 0; i < size; i++) {
                OkAuthenticator.Challenge challenge = challenges.get(i);
                if ("Basic".equalsIgnoreCase(challenge.getScheme()) && (auth = Authenticator.requestPasswordAuthentication(url.getHost(), getConnectToInetAddress(proxy, url), url.getPort(), url.getProtocol(), challenge.getRealm(), challenge.getScheme(), url, Authenticator.RequestorType.SERVER)) != null) {
                    return OkAuthenticator.Credential.basic(auth.getUserName(), new String(auth.getPassword()));
                }
            }
            return null;
        }

        @Override
        public OkAuthenticator.Credential authenticateProxy(Proxy proxy, URL url, List<OkAuthenticator.Challenge> challenges) throws IOException {
            int size = challenges.size();
            for (int i = 0; i < size; i++) {
                OkAuthenticator.Challenge challenge = challenges.get(i);
                if ("Basic".equalsIgnoreCase(challenge.getScheme())) {
                    InetSocketAddress proxyAddress = (InetSocketAddress) proxy.address();
                    PasswordAuthentication auth = Authenticator.requestPasswordAuthentication(proxyAddress.getHostName(), getConnectToInetAddress(proxy, url), proxyAddress.getPort(), url.getProtocol(), challenge.getRealm(), challenge.getScheme(), url, Authenticator.RequestorType.PROXY);
                    if (auth != null) {
                        return OkAuthenticator.Credential.basic(auth.getUserName(), new String(auth.getPassword()));
                    }
                }
            }
            return null;
        }

        private InetAddress getConnectToInetAddress(Proxy proxy, URL url) throws IOException {
            return (proxy == null || proxy.type() == Proxy.Type.DIRECT) ? InetAddress.getByName(url.getHost()) : ((InetSocketAddress) proxy.address()).getAddress();
        }
    };

    private HttpAuthenticator() {
    }

    public static Request processAuthHeader(OkAuthenticator authenticator, Response response, Proxy proxy) throws IOException {
        String responseField;
        String requestField;
        if (response.code() == 401) {
            responseField = "WWW-Authenticate";
            requestField = "Authorization";
        } else if (response.code() == 407) {
            responseField = "Proxy-Authenticate";
            requestField = "Proxy-Authorization";
        } else {
            throw new IllegalArgumentException();
        }
        List<OkAuthenticator.Challenge> challenges = parseChallenges(response.headers(), responseField);
        if (challenges.isEmpty()) {
            return null;
        }
        Request request = response.request();
        OkAuthenticator.Credential credential = response.code() == 407 ? authenticator.authenticateProxy(proxy, request.url(), challenges) : authenticator.authenticate(proxy, request.url(), challenges);
        if (credential != null) {
            return request.newBuilder().header(requestField, credential.getHeaderValue()).build();
        }
        return null;
    }

    private static List<OkAuthenticator.Challenge> parseChallenges(Headers responseHeaders, String challengeHeader) {
        List<OkAuthenticator.Challenge> result = new ArrayList<>();
        for (int h = 0; h < responseHeaders.size(); h++) {
            if (challengeHeader.equalsIgnoreCase(responseHeaders.name(h))) {
                String value = responseHeaders.value(h);
                int pos = 0;
                while (pos < value.length()) {
                    int tokenStart = pos;
                    int pos2 = HeaderParser.skipUntil(value, pos, " ");
                    String scheme = value.substring(tokenStart, pos2).trim();
                    int pos3 = HeaderParser.skipWhitespace(value, pos2);
                    if (value.regionMatches(true, pos3, "realm=\"", 0, "realm=\"".length())) {
                        int pos4 = pos3 + "realm=\"".length();
                        int pos5 = HeaderParser.skipUntil(value, pos4, "\"");
                        String realm = value.substring(pos4, pos5);
                        pos = HeaderParser.skipWhitespace(value, HeaderParser.skipUntil(value, pos5 + 1, ",") + 1);
                        result.add(new OkAuthenticator.Challenge(scheme, realm));
                    }
                }
            }
        }
        return result;
    }
}
