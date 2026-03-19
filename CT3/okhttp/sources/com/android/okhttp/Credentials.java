package com.android.okhttp;

import com.android.okhttp.internal.http.MessageDigestAlgorithm;
import com.android.okhttp.okio.ByteString;
import java.io.UnsupportedEncodingException;

public final class Credentials {
    private Credentials() {
    }

    public static String basic(String userName, String password) {
        try {
            String usernameAndPassword = userName + ":" + password;
            byte[] bytes = usernameAndPassword.getBytes("ISO-8859-1");
            String encoded = ByteString.of(bytes).base64();
            return "Basic " + encoded;
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError();
        }
    }

    public static String digest(String userName, String password, Challenge challenge, String method, String url) {
        try {
            String response = MessageDigestAlgorithm.calculateResponse("MD5", userName, challenge.getRealm(), password, challenge.getNonce(), "00000001", "ce908499f83f6c9d", method, System.getProperty("http.urlpath"), System.getProperty("http.req.body"), challenge.getQop());
            System.setProperty("http.req.body", "");
            String opaque = challenge.getOpaque();
            String header = " username=\"" + userName + "\", realm=\"" + challenge.getRealm() + "\", nonce=\"" + challenge.getNonce() + "\", uri=\"" + System.getProperty("http.urlpath") + "\", response=\"" + response + (opaque != null ? "\", opaque=\"" + opaque : "") + "\", qop=" + challenge.getQop() + ", nc=00000001, cnonce=\"ce908499f83f6c9d\", algorithm=MD5";
            return "Digest " + header;
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError();
        }
    }
}
