package org.apache.http.impl.auth;

import gov.nist.core.Separators;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.auth.params.AuthParams;
import org.apache.http.message.BasicHeaderValueFormatter;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.message.BufferedHeader;
import org.apache.http.util.CharArrayBuffer;
import org.apache.http.util.EncodingUtils;
import org.ccil.cowan.tagsoup.HTMLModels;

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
        boolean unsupportedQop = false;
        String qop = getParameter("qop");
        if (qop != null) {
            StringTokenizer tok = new StringTokenizer(qop, Separators.COMMA);
            while (true) {
                if (!tok.hasMoreTokens()) {
                    break;
                }
                String variant = tok.nextToken().trim();
                if (variant.equals("auth")) {
                    this.qopVariant = 2;
                    break;
                } else if (variant.equals("auth-int")) {
                    this.qopVariant = 1;
                } else {
                    unsupportedQop = true;
                }
            }
        }
        if (unsupportedQop && this.qopVariant == 0) {
            throw new MalformedChallengeException("None of the qop methods is supported");
        }
        this.cnonce = null;
        this.complete = true;
    }

    @Override
    public boolean isComplete() {
        String s = getParameter("stale");
        if ("true".equalsIgnoreCase(s)) {
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

    public void overrideParamter(String name, String value) {
        getParameters().put(name, value);
    }

    private String getCnonce() {
        if (this.cnonce == null) {
            this.cnonce = createCnonce();
        }
        return this.cnonce;
    }

    @Override
    public Header authenticate(Credentials credentials, HttpRequest request) throws AuthenticationException {
        if (credentials == null) {
            throw new IllegalArgumentException("Credentials may not be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("HTTP request may not be null");
        }
        getParameters().put("methodname", request.getRequestLine().getMethod());
        getParameters().put("uri", request.getRequestLine().getUri());
        String charset = getParameter("charset");
        if (charset == null) {
            String charset2 = AuthParams.getCredentialCharset(request.getParams());
            getParameters().put("charset", charset2);
        }
        String digest = createDigest(credentials);
        return createDigestHeader(credentials, digest);
    }

    private static MessageDigest createMessageDigest(String digAlg) throws UnsupportedDigestAlgorithmException {
        try {
            return MessageDigest.getInstance(digAlg);
        } catch (Exception e) {
            throw new UnsupportedDigestAlgorithmException("Unsupported algorithm in HTTP Digest authentication: " + digAlg);
        }
    }

    private String createDigest(Credentials credentials) throws AuthenticationException {
        String serverDigestValue;
        String uri = getParameter("uri");
        String realm = getParameter("realm");
        String nonce = getParameter("nonce");
        String method = getParameter("methodname");
        String algorithm = getParameter("algorithm");
        if (uri == null) {
            throw new IllegalStateException("URI may not be null");
        }
        if (realm == null) {
            throw new IllegalStateException("Realm may not be null");
        }
        if (nonce == null) {
            throw new IllegalStateException("Nonce may not be null");
        }
        if (algorithm == null) {
            algorithm = "MD5";
        }
        String charset = getParameter("charset");
        if (charset == null) {
            charset = "ISO-8859-1";
        }
        if (this.qopVariant == 1) {
            throw new AuthenticationException("Unsupported qop in HTTP Digest authentication");
        }
        MessageDigest md5Helper = createMessageDigest("MD5");
        String uname = credentials.getUserPrincipal().getName();
        String pwd = credentials.getPassword();
        StringBuilder tmp = new StringBuilder(uname.length() + realm.length() + pwd.length() + 2);
        tmp.append(uname);
        tmp.append(':');
        tmp.append(realm);
        tmp.append(':');
        tmp.append(pwd);
        String a1 = tmp.toString();
        if (algorithm.equalsIgnoreCase("MD5-sess")) {
            String cnonce = getCnonce();
            String tmp2 = encode(md5Helper.digest(EncodingUtils.getBytes(a1, charset)));
            StringBuilder tmp3 = new StringBuilder(tmp2.length() + nonce.length() + cnonce.length() + 2);
            tmp3.append(tmp2);
            tmp3.append(':');
            tmp3.append(nonce);
            tmp3.append(':');
            tmp3.append(cnonce);
            a1 = tmp3.toString();
        } else if (!algorithm.equalsIgnoreCase("MD5")) {
            throw new AuthenticationException("Unhandled algorithm " + algorithm + " requested");
        }
        String md5a1 = encode(md5Helper.digest(EncodingUtils.getBytes(a1, charset)));
        String a2 = null;
        if (this.qopVariant != 1) {
            a2 = method + ':' + uri;
        }
        String md5a2 = encode(md5Helper.digest(EncodingUtils.getAsciiBytes(a2)));
        if (this.qopVariant == 0) {
            StringBuilder tmp22 = new StringBuilder(md5a1.length() + nonce.length() + md5a2.length());
            tmp22.append(md5a1);
            tmp22.append(':');
            tmp22.append(nonce);
            tmp22.append(':');
            tmp22.append(md5a2);
            serverDigestValue = tmp22.toString();
        } else {
            String qopOption = getQopVariantString();
            String cnonce2 = getCnonce();
            StringBuilder tmp23 = new StringBuilder(md5a1.length() + nonce.length() + NC.length() + cnonce2.length() + qopOption.length() + md5a2.length() + 5);
            tmp23.append(md5a1);
            tmp23.append(':');
            tmp23.append(nonce);
            tmp23.append(':');
            tmp23.append(NC);
            tmp23.append(':');
            tmp23.append(cnonce2);
            tmp23.append(':');
            tmp23.append(qopOption);
            tmp23.append(':');
            tmp23.append(md5a2);
            serverDigestValue = tmp23.toString();
        }
        String serverDigest = encode(md5Helper.digest(EncodingUtils.getAsciiBytes(serverDigestValue)));
        return serverDigest;
    }

    private Header createDigestHeader(Credentials credentials, String digest) throws AuthenticationException {
        CharArrayBuffer buffer = new CharArrayBuffer(HTMLModels.M_DEF);
        if (isProxy()) {
            buffer.append("Proxy-Authorization");
        } else {
            buffer.append("Authorization");
        }
        buffer.append(": Digest ");
        String uri = getParameter("uri");
        String realm = getParameter("realm");
        String nonce = getParameter("nonce");
        String opaque = getParameter("opaque");
        String algorithm = getParameter("algorithm");
        String uname = credentials.getUserPrincipal().getName();
        List<BasicNameValuePair> params = new ArrayList<>(20);
        params.add(new BasicNameValuePair("username", uname));
        params.add(new BasicNameValuePair("realm", realm));
        params.add(new BasicNameValuePair("nonce", nonce));
        params.add(new BasicNameValuePair("uri", uri));
        params.add(new BasicNameValuePair("response", digest));
        if (this.qopVariant != 0) {
            params.add(new BasicNameValuePair("qop", getQopVariantString()));
            params.add(new BasicNameValuePair("nc", NC));
            params.add(new BasicNameValuePair("cnonce", getCnonce()));
        }
        if (algorithm != null) {
            params.add(new BasicNameValuePair("algorithm", algorithm));
        }
        if (opaque != null) {
            params.add(new BasicNameValuePair("opaque", opaque));
        }
        for (int i = 0; i < params.size(); i++) {
            BasicNameValuePair param = params.get(i);
            if (i > 0) {
                buffer.append(", ");
            }
            boolean noQuotes = "nc".equals(param.getName()) || "qop".equals(param.getName());
            BasicHeaderValueFormatter.DEFAULT.formatNameValuePair(buffer, param, !noQuotes);
        }
        return new BufferedHeader(buffer);
    }

    private String getQopVariantString() {
        if (this.qopVariant == 1) {
            return "auth-int";
        }
        return "auth";
    }

    private static String encode(byte[] binaryData) {
        if (binaryData.length != 16) {
            return null;
        }
        char[] buffer = new char[32];
        for (int i = 0; i < 16; i++) {
            int low = binaryData[i] & 15;
            int high = (binaryData[i] & 240) >> 4;
            buffer[i * 2] = HEXADECIMAL[high];
            buffer[(i * 2) + 1] = HEXADECIMAL[low];
        }
        return new String(buffer);
    }

    public static String createCnonce() {
        MessageDigest md5Helper = createMessageDigest("MD5");
        String cnonce = Long.toString(System.currentTimeMillis());
        return encode(md5Helper.digest(EncodingUtils.getAsciiBytes(cnonce)));
    }
}
