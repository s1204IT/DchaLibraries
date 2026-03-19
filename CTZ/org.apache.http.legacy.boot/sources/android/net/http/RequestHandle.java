package android.net.http;

import android.net.compatibility.WebAddress;
import android.webkit.CookieManager;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.SM;
import org.apache.http.protocol.HTTP;

public class RequestHandle {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    public static final int MAX_REDIRECT_COUNT = 16;
    private static final String PROXY_AUTHORIZATION_HEADER = "Proxy-Authorization";
    private int mBodyLength;
    private InputStream mBodyProvider;
    private Connection mConnection;
    private Map<String, String> mHeaders;
    private String mMethod;
    private int mRedirectCount;
    private Request mRequest;
    private RequestQueue mRequestQueue;
    private WebAddress mUri;
    private String mUrl;

    public RequestHandle(RequestQueue requestQueue, String str, WebAddress webAddress, String str2, Map<String, String> map, InputStream inputStream, int i, Request request) {
        this.mRedirectCount = 0;
        this.mHeaders = map == null ? new HashMap<>() : map;
        this.mBodyProvider = inputStream;
        this.mBodyLength = i;
        this.mMethod = str2 == null ? HttpGet.METHOD_NAME : str2;
        this.mUrl = str;
        this.mUri = webAddress;
        this.mRequestQueue = requestQueue;
        this.mRequest = request;
    }

    public RequestHandle(RequestQueue requestQueue, String str, WebAddress webAddress, String str2, Map<String, String> map, InputStream inputStream, int i, Request request, Connection connection) {
        this(requestQueue, str, webAddress, str2, map, inputStream, i, request);
        this.mConnection = connection;
    }

    public void cancel() {
        if (this.mRequest != null) {
            this.mRequest.cancel();
        }
    }

    public void pauseRequest(boolean z) {
        if (this.mRequest != null) {
            this.mRequest.setLoadingPaused(z);
        }
    }

    public void handleSslErrorResponse(boolean z) {
        if (this.mRequest != null) {
            this.mRequest.handleSslErrorResponse(z);
        }
    }

    public boolean isRedirectMax() {
        return this.mRedirectCount >= 16;
    }

    public int getRedirectCount() {
        return this.mRedirectCount;
    }

    public void setRedirectCount(int i) {
        this.mRedirectCount = i;
    }

    public boolean setupRedirect(String str, int i, Map<String, String> map) throws Throwable {
        String cookie;
        this.mHeaders.remove("Authorization");
        this.mHeaders.remove("Proxy-Authorization");
        int i2 = this.mRedirectCount + 1;
        this.mRedirectCount = i2;
        if (i2 == 16) {
            this.mRequest.error(-9, "The page contains too many server redirects.");
            return false;
        }
        if (this.mUrl.startsWith("https:") && str.startsWith("http:")) {
            this.mHeaders.remove("Referer");
        }
        this.mUrl = str;
        try {
            this.mUri = new WebAddress(this.mUrl);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        this.mHeaders.remove(SM.COOKIE);
        if (this.mUri != null) {
            cookie = CookieManager.getInstance().getCookie(this.mUri.toString());
        } else {
            cookie = null;
        }
        if (cookie != null && cookie.length() > 0) {
            this.mHeaders.put(SM.COOKIE, cookie);
        }
        if ((i == 302 || i == 303) && this.mMethod.equals(HttpPost.METHOD_NAME)) {
            this.mMethod = HttpGet.METHOD_NAME;
        }
        if (i == 307) {
            try {
                if (this.mBodyProvider != null) {
                    this.mBodyProvider.reset();
                }
            } catch (IOException e2) {
                return false;
            }
        } else {
            this.mHeaders.remove(HTTP.CONTENT_TYPE);
            this.mBodyProvider = null;
        }
        this.mHeaders.putAll(map);
        createAndQueueNewRequest();
        return true;
    }

    public void setupBasicAuthResponse(boolean z, String str, String str2) throws Throwable {
        String strComputeBasicAuthResponse = computeBasicAuthResponse(str, str2);
        this.mHeaders.put(authorizationHeader(z), "Basic " + strComputeBasicAuthResponse);
        setupAuthResponse();
    }

    public void setupDigestAuthResponse(boolean z, String str, String str2, String str3, String str4, String str5, String str6, String str7) throws Throwable {
        String strComputeDigestAuthResponse = computeDigestAuthResponse(str, str2, str3, str4, str5, str6, str7);
        this.mHeaders.put(authorizationHeader(z), "Digest " + strComputeDigestAuthResponse);
        setupAuthResponse();
    }

    private void setupAuthResponse() throws Throwable {
        try {
            if (this.mBodyProvider != null) {
                this.mBodyProvider.reset();
            }
        } catch (IOException e) {
        }
        createAndQueueNewRequest();
    }

    public String getMethod() {
        return this.mMethod;
    }

    public static String computeBasicAuthResponse(String str, String str2) {
        if (str == null) {
            throw new NullPointerException("username == null");
        }
        if (str2 == null) {
            throw new NullPointerException("password == null");
        }
        return new String(Base64.encodeBase64((str + ':' + str2).getBytes()));
    }

    public void waitUntilComplete() {
        this.mRequest.waitUntilComplete();
    }

    public void processRequest() throws Throwable {
        if (this.mConnection != null) {
            this.mConnection.processRequests(this.mRequest);
        }
    }

    private String computeDigestAuthResponse(String str, String str2, String str3, String str4, String str5, String str6, String str7) {
        if (str == null) {
            throw new NullPointerException("username == null");
        }
        if (str2 == null) {
            throw new NullPointerException("password == null");
        }
        if (str3 == null) {
            throw new NullPointerException("realm == null");
        }
        String str8 = this.mMethod + ":" + this.mUrl;
        String strComputeCnonce = computeCnonce();
        String strComputeDigest = computeDigest(str + ":" + str3 + ":" + str2, str8, str4, str5, "00000001", strComputeCnonce);
        String str9 = (((("username=" + doubleQuote(str) + ", ") + "realm=" + doubleQuote(str3) + ", ") + "nonce=" + doubleQuote(str4) + ", ") + "uri=" + doubleQuote(this.mUrl) + ", ") + "response=" + doubleQuote(strComputeDigest);
        if (str7 != null) {
            str9 = str9 + ", opaque=" + doubleQuote(str7);
        }
        if (str6 != null) {
            str9 = str9 + ", algorithm=" + str6;
        }
        if (str5 != null) {
            return str9 + ", qop=" + str5 + ", nc=00000001, cnonce=" + doubleQuote(strComputeCnonce);
        }
        return str9;
    }

    public static String authorizationHeader(boolean z) {
        if (!z) {
            return "Authorization";
        }
        return "Proxy-Authorization";
    }

    private String computeDigest(String str, String str2, String str3, String str4, String str5, String str6) {
        if (str4 == null) {
            return KD(H(str), str3 + ":" + H(str2));
        }
        if (str4.equalsIgnoreCase("auth")) {
            return KD(H(str), str3 + ":" + str5 + ":" + str6 + ":" + str4 + ":" + H(str2));
        }
        return null;
    }

    private String KD(String str, String str2) {
        return H(str + ":" + str2);
    }

    private String H(String str) {
        if (str != null) {
            try {
                byte[] bArrDigest = MessageDigest.getInstance("MD5").digest(str.getBytes());
                if (bArrDigest != null) {
                    return bufferToHex(bArrDigest);
                }
                return null;
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private String bufferToHex(byte[] bArr) {
        char[] cArr = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        if (bArr != null) {
            int length = bArr.length;
            if (length > 0) {
                StringBuilder sb = new StringBuilder(2 * length);
                for (int i = 0; i < length; i++) {
                    byte b = (byte) (bArr[i] & 15);
                    sb.append(cArr[(byte) ((bArr[i] & 240) >> 4)]);
                    sb.append(cArr[b]);
                }
                return sb.toString();
            }
            return "";
        }
        return null;
    }

    private String computeCnonce() {
        int iNextInt = new Random().nextInt();
        return Integer.toString(iNextInt == Integer.MIN_VALUE ? Integer.MAX_VALUE : Math.abs(iNextInt), 16);
    }

    private String doubleQuote(String str) {
        if (str != null) {
            return "\"" + str + "\"";
        }
        return null;
    }

    private void createAndQueueNewRequest() throws Throwable {
        if (this.mConnection != null) {
            RequestHandle requestHandleQueueSynchronousRequest = this.mRequestQueue.queueSynchronousRequest(this.mUrl, this.mUri, this.mMethod, this.mHeaders, this.mRequest.mEventHandler, this.mBodyProvider, this.mBodyLength);
            this.mRequest = requestHandleQueueSynchronousRequest.mRequest;
            this.mConnection = requestHandleQueueSynchronousRequest.mConnection;
            requestHandleQueueSynchronousRequest.processRequest();
            return;
        }
        this.mRequest = this.mRequestQueue.queueRequest(this.mUrl, this.mUri, this.mMethod, this.mHeaders, this.mRequest.mEventHandler, this.mBodyProvider, this.mBodyLength).mRequest;
    }
}
