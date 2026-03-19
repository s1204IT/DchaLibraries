package org.apache.http.impl.auth;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.StringTokenizer;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.auth.params.AuthParams;
import org.apache.http.message.BasicHeaderValueFormatter;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.message.BufferedHeader;
import org.apache.http.util.CharArrayBuffer;
import org.apache.http.util.EncodingUtils;

@Deprecated
public class DigestScheme extends RFC2617Scheme {
    private static final char[] HEXADECIMAL = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static final String NC = "00000001";
    private static final int QOP_AUTH = 2;
    private static final int QOP_AUTH_INT = 1;
    private static final int QOP_MISSING = 0;
    private String cnonce;
    private int qopVariant = 0;
    private boolean complete = false;

    @Override
    public void processChallenge(Header header) throws MalformedChallengeException {
        super.processChallenge(header);
        if (getParameter("realm") == null) {
            throw new MalformedChallengeException("missing realm in challange");
        }
        if (getParameter("nonce") == null) {
            throw new MalformedChallengeException("missing nonce in challange");
        }
        boolean z = false;
        String parameter = getParameter("qop");
        if (parameter != null) {
            StringTokenizer stringTokenizer = new StringTokenizer(parameter, ",");
            while (true) {
                if (!stringTokenizer.hasMoreTokens()) {
                    break;
                }
                String strTrim = stringTokenizer.nextToken().trim();
                if (strTrim.equals("auth")) {
                    this.qopVariant = 2;
                    break;
                } else if (strTrim.equals("auth-int")) {
                    this.qopVariant = 1;
                } else {
                    z = true;
                }
            }
        }
        if (z && this.qopVariant == 0) {
            throw new MalformedChallengeException("None of the qop methods is supported");
        }
        this.cnonce = null;
        this.complete = true;
    }

    @Override
    public boolean isComplete() {
        if ("true".equalsIgnoreCase(getParameter("stale"))) {
            return false;
        }
        return this.complete;
    }

    @Override
    public String getSchemeName() {
        return "digest";
    }

    @Override
    public boolean isConnectionBased() {
        return false;
    }

    public void overrideParamter(String str, String str2) {
        getParameters().put(str, str2);
    }

    private String getCnonce() {
        if (this.cnonce == null) {
            this.cnonce = createCnonce();
        }
        return this.cnonce;
    }

    @Override
    public Header authenticate(Credentials credentials, HttpRequest httpRequest) throws AuthenticationException {
        if (credentials == null) {
            throw new IllegalArgumentException("Credentials may not be null");
        }
        if (httpRequest == null) {
            throw new IllegalArgumentException("HTTP request may not be null");
        }
        getParameters().put("methodname", httpRequest.getRequestLine().getMethod());
        getParameters().put("uri", httpRequest.getRequestLine().getUri());
        if (getParameter("charset") == null) {
            getParameters().put("charset", AuthParams.getCredentialCharset(httpRequest.getParams()));
        }
        return createDigestHeader(credentials, createDigest(credentials));
    }

    private static MessageDigest createMessageDigest(String str) throws UnsupportedDigestAlgorithmException {
        try {
            return MessageDigest.getInstance(str);
        } catch (Exception e) {
            throw new UnsupportedDigestAlgorithmException("Unsupported algorithm in HTTP Digest authentication: " + str);
        }
    }

    private String createDigest(Credentials credentials) throws AuthenticationException {
        String string;
        String parameter = getParameter("uri");
        String parameter2 = getParameter("realm");
        String parameter3 = getParameter("nonce");
        String parameter4 = getParameter("methodname");
        String parameter5 = getParameter("algorithm");
        if (parameter == null) {
            throw new IllegalStateException("URI may not be null");
        }
        if (parameter2 == null) {
            throw new IllegalStateException("Realm may not be null");
        }
        if (parameter3 == null) {
            throw new IllegalStateException("Nonce may not be null");
        }
        if (parameter5 == null) {
            parameter5 = "MD5";
        }
        String parameter6 = getParameter("charset");
        if (parameter6 == null) {
            parameter6 = "ISO-8859-1";
        }
        if (this.qopVariant == 1) {
            throw new AuthenticationException("Unsupported qop in HTTP Digest authentication");
        }
        MessageDigest messageDigestCreateMessageDigest = createMessageDigest("MD5");
        String name = credentials.getUserPrincipal().getName();
        String password = credentials.getPassword();
        StringBuilder sb = new StringBuilder(name.length() + parameter2.length() + password.length() + 2);
        sb.append(name);
        sb.append(':');
        sb.append(parameter2);
        sb.append(':');
        sb.append(password);
        String string2 = sb.toString();
        if (parameter5.equalsIgnoreCase("MD5-sess")) {
            String cnonce = getCnonce();
            String strEncode = encode(messageDigestCreateMessageDigest.digest(EncodingUtils.getBytes(string2, parameter6)));
            StringBuilder sb2 = new StringBuilder(strEncode.length() + parameter3.length() + cnonce.length() + 2);
            sb2.append(strEncode);
            sb2.append(':');
            sb2.append(parameter3);
            sb2.append(':');
            sb2.append(cnonce);
            string2 = sb2.toString();
        } else if (!parameter5.equalsIgnoreCase("MD5")) {
            throw new AuthenticationException("Unhandled algorithm " + parameter5 + " requested");
        }
        String strEncode2 = encode(messageDigestCreateMessageDigest.digest(EncodingUtils.getBytes(string2, parameter6)));
        String str = null;
        if (this.qopVariant != 1) {
            str = parameter4 + ':' + parameter;
        }
        String strEncode3 = encode(messageDigestCreateMessageDigest.digest(EncodingUtils.getAsciiBytes(str)));
        if (this.qopVariant == 0) {
            StringBuilder sb3 = new StringBuilder(strEncode2.length() + parameter3.length() + strEncode3.length());
            sb3.append(strEncode2);
            sb3.append(':');
            sb3.append(parameter3);
            sb3.append(':');
            sb3.append(strEncode3);
            string = sb3.toString();
        } else {
            String qopVariantString = getQopVariantString();
            String cnonce2 = getCnonce();
            StringBuilder sb4 = new StringBuilder(strEncode2.length() + parameter3.length() + NC.length() + cnonce2.length() + qopVariantString.length() + strEncode3.length() + 5);
            sb4.append(strEncode2);
            sb4.append(':');
            sb4.append(parameter3);
            sb4.append(':');
            sb4.append(NC);
            sb4.append(':');
            sb4.append(cnonce2);
            sb4.append(':');
            sb4.append(qopVariantString);
            sb4.append(':');
            sb4.append(strEncode3);
            string = sb4.toString();
        }
        return encode(messageDigestCreateMessageDigest.digest(EncodingUtils.getAsciiBytes(string)));
    }

    private Header createDigestHeader(Credentials credentials, String str) throws AuthenticationException {
        CharArrayBuffer charArrayBuffer = new CharArrayBuffer(128);
        if (isProxy()) {
            charArrayBuffer.append(AUTH.PROXY_AUTH_RESP);
        } else {
            charArrayBuffer.append(AUTH.WWW_AUTH_RESP);
        }
        charArrayBuffer.append(": Digest ");
        String parameter = getParameter("uri");
        String parameter2 = getParameter("realm");
        String parameter3 = getParameter("nonce");
        String parameter4 = getParameter("opaque");
        String parameter5 = getParameter("algorithm");
        String name = credentials.getUserPrincipal().getName();
        ArrayList arrayList = new ArrayList(20);
        arrayList.add(new BasicNameValuePair("username", name));
        arrayList.add(new BasicNameValuePair("realm", parameter2));
        arrayList.add(new BasicNameValuePair("nonce", parameter3));
        arrayList.add(new BasicNameValuePair("uri", parameter));
        arrayList.add(new BasicNameValuePair("response", str));
        if (this.qopVariant != 0) {
            arrayList.add(new BasicNameValuePair("qop", getQopVariantString()));
            arrayList.add(new BasicNameValuePair("nc", NC));
            arrayList.add(new BasicNameValuePair("cnonce", getCnonce()));
        }
        if (parameter5 != null) {
            arrayList.add(new BasicNameValuePair("algorithm", parameter5));
        }
        if (parameter4 != null) {
            arrayList.add(new BasicNameValuePair("opaque", parameter4));
        }
        for (int i = 0; i < arrayList.size(); i++) {
            BasicNameValuePair basicNameValuePair = (BasicNameValuePair) arrayList.get(i);
            if (i > 0) {
                charArrayBuffer.append(", ");
            }
            BasicHeaderValueFormatter.DEFAULT.formatNameValuePair(charArrayBuffer, basicNameValuePair, !("nc".equals(basicNameValuePair.getName()) || "qop".equals(basicNameValuePair.getName())));
        }
        return new BufferedHeader(charArrayBuffer);
    }

    private String getQopVariantString() {
        if (this.qopVariant == 1) {
            return "auth-int";
        }
        return "auth";
    }

    private static String encode(byte[] bArr) {
        if (bArr.length != 16) {
            return null;
        }
        char[] cArr = new char[32];
        for (int i = 0; i < 16; i++) {
            int i2 = bArr[i] & 15;
            int i3 = i * 2;
            cArr[i3] = HEXADECIMAL[(bArr[i] & 240) >> 4];
            cArr[i3 + 1] = HEXADECIMAL[i2];
        }
        return new String(cArr);
    }

    public static String createCnonce() {
        return encode(createMessageDigest("MD5").digest(EncodingUtils.getAsciiBytes(Long.toString(System.currentTimeMillis()))));
    }
}
